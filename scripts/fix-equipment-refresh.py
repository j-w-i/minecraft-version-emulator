"""Force the player's attributes to be recomputed once the version is known.

The measured chain: LivingEntity.collectEquipmentChanges is what applies a held item's ATTACK_DAMAGE to the
player's attribute map, and it runs when equipment CHANGES. The session that decides whether the era's damage
values apply is set later, on player join. So the map is built with modern damage and is never rebuilt while
the player keeps holding the same sword - the gate reports true, the hook is correct, and nothing changes.
That is exactly what the owner described: gates true, swords and axes at their modern values.

The fix is to re-run the equipment pass after the session is set. collectEquipmentChanges is private, so it is
exposed with a Mixin accessor rather than by reaching into the class.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
mixin_dir = root / 'src/main/java/net/historicalexperience/mixin'
gameplay = root / 'src/main/java/net/historicalexperience/gameplay'

# 1. The accessor. An interface, so the generated method is added to LivingEntity and can be called on any
#    living entity without a cast to a class the mixin does not extend.
(mixin_dir / 'LivingEntityEquipmentInvoker.java').write_text('''package net.historicalexperience.mixin;

import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code LivingEntity.collectEquipmentChanges}, which is private, so the equipment pass can be re-run
 * on demand.
 *
 * <p>Why it is needed: that private method is what applies a held item's ATTACK_DAMAGE to the player's
 * attribute map, and it only runs when equipment changes. The historical version is not known until the
 * player joins, so the map is built first and then never rebuilt - the era's damage values were gate-true and
 * completely inert until the owner switched items.
 *
 * <p>Declared as an abstract instance method on an interface, which is the form Mixin generates a body for.
 * The static form is rejected, and the failure is fatal at startup rather than a warning.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityEquipmentInvoker {
    @Invoker("collectEquipmentChanges")
    Map<EquipmentSlot, ItemStack> invokeCollectEquipmentChanges();
}
''', encoding='utf-8')
print('wrote LivingEntityEquipmentInvoker')

# 2. A place to call it from, so no caller has to know about the mixin.
(gameplay / 'HistoricalEquipmentRefresh.java').write_text('''package net.historicalexperience.gameplay;

import net.minecraft.world.entity.LivingEntity;

/**
 * Re-runs a living entity's equipment pass, so version-dependent attribute modifiers take effect.
 *
 * <p>Called once the world's historical version is known - on the server when a player joins, and on the
 * client when the server's payload arrives. Without it, an item already in hand keeps the modern attribute
 * map until the player happens to switch items, which is a change nobody would think to make.
 */
public final class HistoricalEquipmentRefresh {
    private HistoricalEquipmentRefresh() {
    }

    /** Recomputes the entity's equipment-derived attributes, if it is one this build can refresh. */
    public static void refresh(LivingEntity entity) {
        if (entity instanceof net.historicalexperience.mixin.LivingEntityEquipmentInvoker invoker) {
            try {
                invoker.invokeCollectEquipmentChanges();
            } catch (RuntimeException e) {
                // A refresh failure must not break a login: the worst case is the modern attribute map, which
                // is exactly the state this call was trying to improve on.
                net.historicalexperience.HistoricalExperienceMod.LOGGER.warn(
                    "could not refresh equipment attributes after the version was set", e);
            }
        }
    }
}
''', encoding='utf-8')
print('wrote HistoricalEquipmentRefresh')

# 3. Register the accessor.
config = root / 'src/main/resources/historical_experience.mixins.json'
import json
document = json.loads(config.read_text(encoding='utf-8'))
if 'LivingEntityEquipmentInvoker' not in document['mixins']:
    document['mixins'].append('LivingEntityEquipmentInvoker')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered the accessor; mixins =', len(document['mixins']))

# 4. Refresh on the server once the session is set, and on the client when the payload arrives.
sync = root / 'src/main/java/net/historicalexperience/server/HistoricalVersionSync.java'
text = sync.read_text(encoding='utf-8')
old = '''            HistoricalActiveSession.set(versionId);'''
new = '''            HistoricalActiveSession.set(versionId);
            // Recompute the attributes NOW that the version is known. The equipment pass that applies a held
            // item's attack damage already ran, before this, with the modern values; without this the era's
            // damage never takes effect until the player happens to switch items.
            net.historicalexperience.gameplay.HistoricalEquipmentRefresh.refresh(handler.player);'''
assert old in text, 'sync anchor not found'
sync.write_text(text.replace(old, new, 1), encoding='utf-8')
print('server refreshes equipment after setting the session')

client = root / 'src/main/java/net/historicalexperience/client/HistoricalExperienceClient.java'
text = client.read_text(encoding='utf-8')
old = '''                HistoricalClientRules.applySyncedVersion(payload.versionId());'''
new = '''                HistoricalClientRules.applySyncedVersion(payload.versionId());
                // And on this side too: the client predicts its own attack damage and draws the attack
                // indicator from its own attribute map.
                net.historicalexperience.gameplay.HistoricalEquipmentRefresh.refresh(context.player());'''
assert old in text, 'client anchor not found'
client.write_text(text.replace(old, new, 1), encoding='utf-8')
print('client refreshes equipment on receiving the version')

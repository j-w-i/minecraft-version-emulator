"""Write the loot substitution hook, register the sheep accessor, and add the helper's gate.

Nothing was applied by the previous attempt: PowerShell parses a whole script before running it, so the inline
python syntax error meant the mixin file was never written either. This is a file for that reason.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalLootRules;
import net.historicalexperience.gameplay.HistoricalMobLootHelper;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives a mob the drops its era had, and withholds experience from versions that awarded none.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_drops/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers the {@code OLD_*_DROPS} family.
 *
 * <h2>Substitution, not replacement</h2>
 *
 * <p>Upstream modifies the {@code getLootTable} call inside {@code dropFromLootTable} and returns a generated
 * historical table, so the vanilla death path still rolls it. My first version cancelled
 * {@code dropFromLootTable} outright and spawned the items itself. The visible drop looked right - a Beta 1.7.3
 * zombie dropped feathers either way - while two vanilla mechanics were skipped underneath it: a Looting sword
 * changed nothing, and a mob that died on fire did not drop the cooked form. Calling downstream code is not the
 * same thing as calling the code that downstream calls.
 *
 * <h2>The experience half</h2>
 *
 * <p>The {@code dropExperience} hook is kept, and is a separate rule from upstream's orb suppression: it removes
 * experience in versions whose own registry had no orbs, which is a fact about the version rather than about how
 * an orb is created. The owner confirmed its effect in play.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalLootMixin {
    /** Upstream: {@code nt_mob_drops$modifyDropLootTable}. */
    @ModifyExpressionValue(
        method = "dropFromLootTable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable("
                + "Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;"))
    private LootTable historicalExperience$replaceLootTable(LootTable vanilla) {
        LivingEntity self = (LivingEntity) (Object) this;
        LootTable chosen = HistoricalMobLootHelper.getTable(self, vanilla);

        if (chosen != vanilla) {
            String entityTypeId =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).toString();
            HistoricalLootRules.recordReplacedDeath(entityTypeId);
        }

        return chosen;
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$suppressExperience(Entity killer, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String profileId = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        if (profileId != null && HistoricalLootRules.suppressesExperience(profileId)) {
            HistoricalLootRules.recordSuppressedExperience();
            callback.cancel();
        }
    }
}
'''

mixin = root / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalLootMixin.java'
mixin.write_text(MIXIN, encoding='utf-8')
print('wrote the substitution hook')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'SheepAccess' not in document['mixins']:
    document['mixins'].append('SheepAccess')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered SheepAccess; shared mixins =', len(document['mixins']))

rules = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalLootRules.java'
text = rules.read_text(encoding='utf-8')
if 'import net.minecraft.world.level.Level;' not in text:
    text = text.replace('import net.minecraft.world.entity.Entity;',
                        'import net.minecraft.server.level.ServerLevel;\n'
                        'import net.minecraft.world.entity.Entity;\n'
                        'import net.minecraft.world.level.Level;', 1)

METHOD = '''
    /**
     * Whether the active version has an old-drop rule for this entity.
     *
     * <p>This is the gate the loot helper asks. Upstream has fourteen separate per-entity toggles; this project
     * asks the version instead, and reuses the boundary that is already tested here rather than introducing a
     * second one that could disagree with it.
     */
    public static boolean hasOldDrops(Level level, String entityTypeId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        String profileId = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        return profileId != null && forEntity(profileId, entityTypeId).isPresent();
    }
'''
index = text.rstrip().rfind('}')
text = text[:index] + METHOD + text[index:]
rules.write_text(text, encoding='utf-8')
print('added hasOldDrops')

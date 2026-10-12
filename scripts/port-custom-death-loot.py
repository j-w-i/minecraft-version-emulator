"""Port Mob.dropCustomDeathLoot refusal for skeletons, dated by the research.

Evidence, from research/feature-timeline.json: "Skeletons dropped arrows from Indev 20100219; 0-2 bones were
added in Beta 1.2. The bow drop is a separate, much later behaviour (1.2.1 12w06a, 8.5%)." So through Release
1.1 a skeleton dropped no bow, and the bow drop begins at 1.2.1 - the same boundary as the avoidSun gate, whose
declared proxy is RELEASE_1_2_5.

Upstream: common/.../mixin/tweak/gameplay/mob_drops/MobMixin.java, a @WrapWithCondition on the spawnAtLocation
call inside dropCustomDeathLoot, with one flag per mob. Only the skeleton half is ported: "stray" does not exist
in any declared profile, and the research says nothing about a zombie pigman refusing its held item.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a skeleton dropping its bow, in versions where it had none to drop.
 *
 * <p>DIRECT PORT of the skeleton half of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_drops/MobMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.OLD_SKELETON_DROPS}. The injection
 * point - a wrap on the {@code spawnAtLocation} call inside {@code dropCustomDeathLoot} - is upstream's, and so
 * is the idea: the bow is CUSTOM death loot, a separate drop from the table, so the loot-table substitution
 * cannot reach it and it needs this hook.
 *
 * <h2>The boundary, from the research rather than from guesswork</h2>
 *
 * <p>feature-timeline.json: "Skeletons dropped arrows from Indev 20100219; 0-2 bones were added in Beta 1.2.
 * The bow drop is a separate, much later behaviour (1.2.1 12w06a, 8.5%)". So the bow is refused through Release
 * 1.1 and allowed from Release 1.2.1 - the same boundary and the same declared proxy (RELEASE_1_2_5) as the
 * avoid-sun gate, which is why the gate table carries both.
 *
 * <p>Two of upstream's three flags are deliberately not ported. {@code OLD_STYLE_STRAY_DROPS} is unreachable -
 * strays are 1.10 and no declared profile has them - and the research makes no claim about a zombie pigman
 * refusing its held item, so {@code OLD_ZOMBIE_PIGMEN_DROPS} would be a guess. A default-off upstream flag is
 * NOT a reason to skip either one; the missing evidence is.
 */
@Mixin(Mob.class)
public abstract class MobHistoricalCustomLootMixin {
    /** Upstream: {@code nt_mob_drops$shouldDropCustomDeathLoot}, skeleton branch only. */
    @WrapWithCondition(
        method = "dropCustomDeathLoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation("
                + "Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private boolean historicalExperience$shouldDropCustomDeathLoot(Mob mob, ItemStack itemStack) {
        if (mob.getType() == EntityType.SKELETON
            && HistoricalGameplayTweaks.appliesTo("customDeathLoot", HistoricalActiveSession.current())) {
            return false;
        }
        return true;
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/MobHistoricalCustomLootMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the custom death loot mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'MobHistoricalCustomLootMixin' not in document['mixins']:
    document['mixins'].append('MobHistoricalCustomLootMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')

old = '            case "avoidSun" -> isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_2_5);'
assert old in text, 'avoidSun case anchor not found'
text = text.replace(old, old + '\n'
    '            // Same boundary as avoidSun and the same declared proxy: the skeleton bow drop is 1.2.1\n'
    '            // (12w06a, 8.5%) per the timeline, so through Release 1.1 there is no bow to drop.\n'
    '            case "customDeathLoot" ->\n'
    '                isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_2_5);', 1)

old = '"itemPickup", "monsterEnchant"'
assert old in text, 'gateNames anchor not found'
text = text.replace(old, '"customDeathLoot", "itemPickup", "monsterEnchant"', 1)

old = '        Map.entry("avoidSun",'
assert old in text, 'evidence anchor not found'
text = text.replace(old, '''        Map.entry("customDeathLoot",
            "A skeleton dropping its bow is Release 1.2.1 (12w06a, 8.5%): the timeline records arrows from "
                + "Indev 20100219 and bones from Beta 1.2, with the bow as a separate later behaviour. "
                + "Upstream tweak: OLD_SKELETON_DROPS."),
        Map.entry("avoidSun",''', 1)
tweaks.write_text(text, encoding='utf-8')
print('gate, gateNames and evidence updated')

test = root / 'src/test/java/net/historicalexperience/gameplay/HistoricalGameplayTweaksTest.java'
text = test.read_text(encoding='utf-8')
old = 'List.of("avoidSun");'
assert old in text, 'test list anchor not found'
test.write_text(text.replace(old, 'List.of("avoidSun", "customDeathLoot");', 1), encoding='utf-8')
print('test boundary list updated')

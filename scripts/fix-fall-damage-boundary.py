"""Give the sword-block fall damage reduction its OWN boundary, not the blocking mechanic's.

The bug this fixes, found by the owner asking whether the reduction was limited to before the Beta 1.9 prerelease.
It was not. The gate was HistoricalSwordBlocking.isBlocking, whose boundary is sword blocking itself: Beta 1.8
Pre-release through 1.9. But the research row says the two sub-behaviours were removed EARLIER:

    "Blocking also used to reduce fall damage and to let the player rise faster underwater; both were removed in
     Beta 1.9 Prerelease, so those two sub-behaviours have a different, later removal boundary (Beta 1.9
     Prerelease) than the mechanic itself (1.9)."

So the window is Beta 1.8 Pre-release <= version < Beta 1.9 Prerelease. Of the ten declared profiles exactly ONE
falls inside it: beta_1_8_1. beta_1_9_pre_3 is Beta 1.9 Prerelease 3, which is on the far side of the removal, and
it was wrongly getting the reduction.

No profile is declared between Beta 1.8.1 and Beta 1.9 Prerelease 3, so beta_1_9_pre_3 is the nearest declared
version on the correct side and serves as the proxy, exactly as the registry's other proxied boundaries do.

The gate is a method rather than a name in the appliesTo table because that table is one-sided (it answers "older
than X") and this window has two edges. Sword blocking itself is expressed the same way, for the same reason.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')

old = '''    public static boolean swordBlockingEnabled(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId);
    }'''
new = '''    public static boolean swordBlockingEnabled(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId);
    }

    /**
     * Whether the old block's FALL DAMAGE reduction applies, which is a narrower window than blocking itself.
     *
     * <p>The timeline records that blocking reduced fall damage and let the player rise faster underwater, and that
     * <b>both were removed in Beta 1.9 Prerelease</b> - earlier than blocking itself, which lasted until 1.9. So a
     * version can have the stance and not the reduction, and Beta 1.9 Prerelease 3 is exactly that version.
     *
     * <p>Of the ten declared profiles only Beta 1.8.1 is inside the window, and Beta 1.9 Prerelease 3 is the
     * nearest declared version past the removal, so it is the proxy - the same technique the registry uses
     * wherever a real boundary falls between two declared versions.
     *
     * <p>This was wrong in the first version of the port: the reduction used the blocking gate and so applied in
     * Beta 1.9 Prerelease 3, which is after the prerelease that removed it.
     */
    public static boolean blockFallDamageReductionEnabled(String versionId) {
        return swordBlockingEnabled(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
    }'''
assert old in text, 'swordBlockingEnabled anchor not found'
text = text.replace(old, new, 1)
tweaks.write_text(text, encoding='utf-8')
print('added the two-sided fall damage window')

mixin = root / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalSwordBlockingMixin.java'
text = mixin.read_text(encoding='utf-8')
old = '''    private float historicalExperience$reduceBlockedFallDamage(float multiplier) {
        if ((Object) this instanceof Player player && HistoricalSwordBlocking.isBlocking(player)) {
            return multiplier * HistoricalSwordBlocking.damageMultiplier();
        }
        return multiplier;
    }'''
new = '''    private float historicalExperience$reduceBlockedFallDamage(float multiplier) {
        // The NARROWER window, not isBlocking: the reduction was removed in Beta 1.9 Prerelease, before blocking
        // itself went in 1.9, so Beta 1.9 Prerelease 3 has the stance without the reduction.
        if ((Object) this instanceof Player player
            && HistoricalSwordBlocking.isBlocking(player)
            && HistoricalGameplayTweaks.blockFallDamageReductionEnabled(HistoricalActiveSession.current())) {
            return multiplier * HistoricalSwordBlocking.damageMultiplier();
        }
        return multiplier;
    }'''
assert old in text, 'fall damage handler anchor not found'
text = text.replace(old, new, 1)
if 'import net.historicalexperience.gameplay.HistoricalActiveSession;' not in text:
    text = text.replace('import net.historicalexperience.gameplay.HistoricalSwordBlocking;',
                        'import net.historicalexperience.gameplay.HistoricalActiveSession;\n'
                        'import net.historicalexperience.gameplay.HistoricalGameplayTweaks;\n'
                        'import net.historicalexperience.gameplay.HistoricalSwordBlocking;', 1)
mixin.write_text(text, encoding='utf-8')
print('the fall damage hook now uses the narrow window')

TEST = '''package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the boundary the owner caught: the sword block's fall damage reduction is a NARROWER window than blocking.
 *
 * <p>Blocking is Beta 1.8 Pre-release through 1.9. The fall damage reduction was removed earlier, in Beta 1.9
 * Prerelease, so a version can hold the stance without the reduction - and Beta 1.9 Prerelease 3 is exactly that
 * version. The first version of the port used the blocking gate and wrongly reduced fall damage there.
 */
class BlockFallDamageBoundaryTest {
    private static final List<String> DECLARED = List.of(
        "historical_experience:infdev_420",
        "historical_experience:infdev_611",
        "historical_experience:alpha",
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_6_4");

    @Test
    void onlyBeta181FallsInsideTheWindow() {
        List<String> inside = DECLARED.stream()
            .filter(HistoricalGameplayTweaks::blockFallDamageReductionEnabled)
            .toList();

        assertTrue(inside.equals(List.of("historical_experience:beta_1_8_1")),
            "the fall damage reduction belongs to Beta 1.8.1 alone among the declared profiles, but got: "
                + inside);
    }

    @Test
    void theStanceOutlivesTheReduction() {
        String preReleaseThree = "historical_experience:beta_1_9_pre_3";

        assertTrue(HistoricalGameplayTweaks.swordBlockingEnabled(preReleaseThree),
            "Beta 1.9 Prerelease 3 still has the sword block stance");
        assertFalse(HistoricalGameplayTweaks.blockFallDamageReductionEnabled(preReleaseThree),
            "but not the fall damage reduction, which was removed in Beta 1.9 Prerelease");
    }

    @Test
    void ordinaryVersionsGetNeither() {
        assertFalse(HistoricalGameplayTweaks.blockFallDamageReductionEnabled(null));
        assertFalse(HistoricalGameplayTweaks.blockFallDamageReductionEnabled("minecraft:overworld"));
    }
}
'''
(root / 'src/test/java/net/historicalexperience/gameplay/BlockFallDamageBoundaryTest.java').write_text(
    TEST, encoding='utf-8')
print('wrote the boundary test')

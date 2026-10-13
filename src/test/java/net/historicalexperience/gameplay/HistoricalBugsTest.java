package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two single-build bugs the owner named: Pre3's blowable End portal frames and Pre1's experience-on-jump.
 *
 * <h2>Why these are asserted as EXACT builds rather than as boundaries</h2>
 *
 * <p>Almost every gate in this project is a boundary, and the tempting way to write both of these is
 * {@code olderThan(version, ...)}, which is wrong in a way that would take a player to notice: "before Beta 1.9
 * Pre4" also includes Pre1, Pre2 and Pre3, so one build's bug would leak into three builds. Both were removed by
 * the very next prerelease, so the assertion walks every declared version and requires the gate to be true for
 * exactly one of them.
 *
 * <p>Evidence: minecraft.wiki, End Portal Frame history — Pre3 <i>"It can drop as an item due to explosions"</i>,
 * Pre4 <i>"The End portal can no longer be broken by explosions"</i>; and for the jump bug
 * {@code research/minecraft-version-history.md} states it in items 6, 11 and 13 and
 * {@code research/historical-features.json} records it independently.
 */
class HistoricalBugsTest {
    private static final String PRE_3 = "historical_experience:beta_1_9_pre_3";
    private static final String PRE_1 = "historical_experience:beta_1_9_pre_1";

    @Test
    @DisplayName("End portal frames can be blown up in Beta 1.9 Pre3 and in no other declared build")
    void endPortalFramesExplodeOnlyInPreThree() {
        int applying = 0;
        for (var version : HistoricalVersions.all()) {
            if (HistoricalGameplayTweaks.appliesTo("endPortalFrameExplodes", version.id())) {
                assertEquals(PRE_3, version.id(),
                    "only Beta 1.9 Pre3 allowed this; Pre4 fixed it, so " + version.id() + " must not");
                applying++;
            }
        }
        assertEquals(1, applying,
            "exactly one declared build had blast-destroyable End portal frames. If this is 0 the bug is "
                + "unreachable; if it is more than 1 then a boundary expression has leaked it into neighbouring "
                + "prereleases, which is the mistake this test exists to catch");

        assertTrue(HistoricalGameplayTweaks.appliesTo("endPortalFrameExplodes", PRE_3));
        assertFalse(HistoricalGameplayTweaks.appliesTo("endPortalFrameExplodes", PRE_1));
        assertFalse(HistoricalGameplayTweaks.appliesTo("endPortalFrameExplodes", "historical_experience:beta_1_9_pre_4"));
        assertFalse(HistoricalGameplayTweaks.appliesTo("endPortalFrameExplodes", "historical_experience:release_1_0_0"));
    }

    @Test
    @DisplayName("jumping grants 1-100 experience in Beta 1.9 Pre1 and nothing in any other declared build")
    void jumpExperienceOnlyInPreOne() {
        int applying = 0;
        for (var version : HistoricalVersions.all()) {
            if (HistoricalGameplayTweaks.appliesTo("jumpExperienceBug", version.id())) {
                assertEquals(PRE_1, version.id(),
                    "only Beta 1.9 Pre1 had the jump-experience bug; Pre2 removed it, so " + version.id()
                        + " must not");
                applying++;
            }
        }
        assertEquals(1, applying, "exactly one declared build had the jump-experience bug, saw " + applying);
    }

    @Test
    @DisplayName("the jump bug grants between 1 and 100 inclusive, and 0 where it does not apply")
    void jumpExperienceRange() {
        // The research says 1-100 per jump and that the amount is random per jump, so both ends of the range must
        // be reachable and neither must escape it. A fixed value would be a fabrication; a range of 0-99 would be
        // an off-by-one that a player would feel as "never a full hundred".
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(20261013L);
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            seen.add(HistoricalGameplayTweaks.jumpExperienceBug(random));
        }
        // The session version is unset in a unit test, so the gate is false and every draw is 0. That is the safe
        // direction and is asserted here rather than assumed: an unknown version must not hand out experience.
        assertEquals(java.util.Set.of(0), seen,
            "with no session version established the gate must be false and grant nothing, so a unit test can "
                + "never accidentally award experience");

        // The range itself is asserted against the METHOD the hook calls, for the single build it applies to,
        // rather than against a copy of its arithmetic. The first version of this test re-implemented
        // `1 + nextInt(100)` inline and compared it to itself, which passes whether or not the real gate is
        // correct - a check that cannot fail. The version-keyed overload exists so this can be pinned offline.
        net.minecraft.util.RandomSource bounded = net.minecraft.util.RandomSource.create(7L);
        int low = 101;
        int high = 0;
        for (int i = 0; i < 20_000; i++) {
            int value = HistoricalGameplayTweaks.jumpExperienceBug(PRE_1, bounded);
            low = Math.min(low, value);
            high = Math.max(high, value);
            assertTrue(value >= 1 && value <= 100, "a jump in Pre1 must pay 1..100, saw " + value);
        }
        assertEquals(1, low, "a hundred draws should reach the bottom of the range");
        assertEquals(100, high, "and the top of it");

        // And the same method draws nothing at all for the build that fixed it, or for any other declared build.
        net.minecraft.util.RandomSource other = net.minecraft.util.RandomSource.create(11L);
        for (int i = 0; i < 1_000; i++) {
            assertEquals(0,
                HistoricalGameplayTweaks.jumpExperienceBug("historical_experience:beta_1_9_pre_2", other),
                "Pre2 removed the debug experience, so it must draw none");
            assertEquals(0,
                HistoricalGameplayTweaks.jumpExperienceBug("historical_experience:release_1_6_4", other),
                "and no later build ever had it");
        }
    }
}

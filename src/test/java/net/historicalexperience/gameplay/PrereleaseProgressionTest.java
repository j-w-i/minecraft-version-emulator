/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards the two prerelease boundaries that arrive around the same builds and are easy to merge.
 *
 * <p>From {@code research/version-history-transitions}: brewing becomes FUNCTIONAL in Beta 1.9 Prerelease 3 -
 * before that, Prerelease 2's brewing is a cauldron prototype with no ordinary block ID and Beta 1.8 has none at
 * all - while the End portal stays inert through Prerelease 3 and is activated in Prerelease 4.
 *
 * <p>So the two boundaries sit on adjacent builds and must not be collapsed: at Prerelease 3, brewing works and
 * the portal does not. That single row is what these tests pin down.
 */
class PrereleaseProgressionTest {
    private static final String BETA_1_8_1 = "historical_experience:beta_1_8_1";
    private static final String BETA_1_9_PRE_3 = "historical_experience:beta_1_9_pre_3";

    @Test
    void brewingWorksFromPrereleaseThreeButNotInBetaOnePointEight() {
        assertFalse(HistoricalGameplayTweaks.functionalBrewing(BETA_1_8_1),
            "Beta 1.8 has no brewing at all; a stand that brews there is an anachronism");
        assertTrue(HistoricalGameplayTweaks.functionalBrewing(BETA_1_9_PRE_3),
            "Prerelease 3 is the first functional brewing: stand-based, thirty seconds, 25 potions");

        for (String preHunger : new String[] {
            "historical_experience:alpha", "historical_experience:beta_1_7_3"}) {
            assertFalse(HistoricalGameplayTweaks.functionalBrewing(preHunger),
                preHunger + " predates potions entirely");
        }
    }

    @Test
    void theEndPortalWorksFromPrereleaseFourButNotAtPrereleaseThree() {
        assertFalse(HistoricalGameplayTweaks.endPortalFunctional(BETA_1_9_PRE_3),
            "Prerelease 3 adds the portal room, the eye and the frames, and the portal block does nothing");
        assertFalse(HistoricalGameplayTweaks.endPortalFunctional(BETA_1_8_1),
            "Beta 1.8 strongholds have no portal room at all");

        for (String release : new String[] {
            "historical_experience:release_1_0_0",
            "historical_experience:release_1_1",
            "historical_experience:release_1_2_5",
            "historical_experience:release_1_6_4"}) {
            assertTrue(HistoricalGameplayTweaks.endPortalFunctional(release),
                release + " is past Prerelease 4, where the portal activates");
        }
    }

    @Test
    void theTwoBoundariesAreNotTheSameBoundary() {
        // This is the whole point of the pair: one build where brewing is real and the portal is not.
        assertTrue(HistoricalGameplayTweaks.functionalBrewing(BETA_1_9_PRE_3)
                && !HistoricalGameplayTweaks.endPortalFunctional(BETA_1_9_PRE_3),
            "Prerelease 3 must be the build where brewing works and the End portal does not. Collapsing these two "
                + "gates would either give Prerelease 3 a working End or take Prerelease 3's brewing away");
    }

    @Test
    void unresolvedVersionsGetNothing() {
        assertFalse(HistoricalGameplayTweaks.functionalBrewing(null));
        assertFalse(HistoricalGameplayTweaks.endPortalFunctional(null));
        assertFalse(HistoricalGameplayTweaks.functionalBrewing("historical_experience:no_such_version"));
        assertFalse(HistoricalGameplayTweaks.endPortalFunctional("historical_experience:no_such_version"));
    }
}

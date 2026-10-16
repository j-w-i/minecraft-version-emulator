/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoricalEarlyMenuRulesTest {
    @Test void bothAuditedMenusAreBlockedForAllThreeEarlyProfiles() {
        for (String profile : EarlyGameplayRecipes.profiles()) for (String menu : List.of("anvil", "grindstone")) {
            assertTrue(HistoricalEarlyMenuRules.blocksResult(profile, menu));
        }
    }
    @Test void ordinaryUnknownAndLaterProfilesAreNotModified() {
        for (String menu : List.of("anvil", "grindstone")) {
            assertFalse(HistoricalEarlyMenuRules.blocksResult(null, menu));
            for (String profile : List.of("", "unknown:profile", "historical_experience:alpha_1_0_1", "historical_experience:beta_1_7_3", "historical_experience:release_1_4")) {
                assertFalse(HistoricalEarlyMenuRules.blocksResult(profile, menu));
            }
        }
    }
    @Test void thisNarrowRuleDoesNotPretendToJudgeOtherMenus() {
        for (String profile : EarlyGameplayRecipes.profiles()) {
            assertFalse(HistoricalEarlyMenuRules.blocksResult(profile, null));
            for (String menu : List.of("crafting_table", "furnace", "chest", "loom", "cartography_table", "stonecutter", "smithing")) {
                assertFalse(HistoricalEarlyMenuRules.blocksResult(profile, menu));
            }
        }
    }
}

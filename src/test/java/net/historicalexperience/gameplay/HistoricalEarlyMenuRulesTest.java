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

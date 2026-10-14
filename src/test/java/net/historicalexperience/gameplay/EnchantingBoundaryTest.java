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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the three historical states of enchanting, which are easy to collapse into one.
 *
 * <p>{@code research/minecraft-version-history.md} Part IV separates them precisely: Beta 1.8 has an experience bar
 * that cannot be spent and no enchanting table; Beta 1.9 Prerelease 3 has the table, its interface and its glint
 * but applies no enchantments and spends nothing; Prerelease 4 onward is real enchanting. The middle state is the
 * trap, because a table that opens looks like a working table.
 */
class EnchantingBoundaryTest {
    private static final String BETA_1_9_PRE_3 = "historical_experience:beta_1_9_pre_3";

    @Test
    void enchantingWorksOnlyFromBetaOnePointNinePrereleaseFour() {
        for (String version : List.of(
            "historical_experience:infdev_420",
            "historical_experience:alpha",
            "historical_experience:beta_1_7_3",
            "historical_experience:beta_1_8_1",
            BETA_1_9_PRE_3)) {
            assertFalse(HistoricalGameplayTweaks.functionalEnchanting(version),
                version + " predates functional enchanting; Beta 1.9 Prerelease 3 has the table's interface and "
                    + "no enchantments, which is exactly the state this asserts");
        }

        for (String version : List.of(
            "historical_experience:release_1_0_0",
            "historical_experience:release_1_1",
            "historical_experience:release_1_2_5",
            "historical_experience:release_1_6_4")) {
            assertTrue(HistoricalGameplayTweaks.functionalEnchanting(version),
                version + " is Prerelease 4 or later, where enchanting applies enchantments and spends levels");
        }
    }

    @Test
    void theTableExistsFromPrereleaseThreeButNotBefore() {
        assertFalse(HistoricalGameplayTweaks.enchantingTableExists("historical_experience:beta_1_8_1"),
            "Beta 1.8 has no enchanting table, only an unspendable experience bar");
        assertTrue(HistoricalGameplayTweaks.enchantingTableExists(BETA_1_9_PRE_3),
            "Prerelease 3 is where the table and its interface arrive");

        // The two gates are deliberately not the same gate: Prerelease 3 has the block without the function.
        assertTrue(HistoricalGameplayTweaks.enchantingTableExists(BETA_1_9_PRE_3)
                && !HistoricalGameplayTweaks.functionalEnchanting(BETA_1_9_PRE_3),
            "Prerelease 3 must be the version where the table exists and does nothing");
    }

    @Test
    void theNumericBoundaryFollowsTheRegistrarsOwnNumbering() {
        int prereleaseThree = HistoricalVersions.byId(BETA_1_9_PRE_3).orElseThrow().chronology();

        assertEquals(prereleaseThree + 1, HistoricalGameplayTweaks.functionalEnchantingBoundaryChronology(),
            "the boundary is written as a literal because Beta 1.9 Prerelease 4 is not declared yet. This test ties "
                + "it to the registry's numbering scheme, so that a literal which drifts silently - era offset, "
                + "major, minor or patch - fails here instead of quietly gating the wrong build");
    }

    @Test
    void undeclaredAndNullVersionsAreNeverTreatedAsEnchanting() {
        assertFalse(HistoricalGameplayTweaks.functionalEnchanting(null),
            "a world whose version could not be resolved must not be handed modern enchanting");
        assertFalse(HistoricalGameplayTweaks.enchantingTableExists(null));
        assertFalse(HistoricalGameplayTweaks.functionalEnchanting("historical_experience:no_such_version"));
    }
}

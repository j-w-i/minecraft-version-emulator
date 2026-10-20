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

import net.historicalexperience.version.ActiveVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Guards that the food a version heals by is the food THAT version had.
 *
 * <p>The direct-heal path read one fixed table - Beta 1.7.3's - for every version, which made each version's own
 * researched table dead data. The Alpha profile already carried {@code ALPHA_1_1_2_HEAL_POINTS} and it was never
 * consulted, so raw fish, cooked fish, cookies and cake healed in Alpha worlds: fish arrives in Alpha 1.2.0, and
 * cookies and cake are Beta additions.
 *
 * <p>The tests set the active session, because the question "what does this food heal" has no answer without a
 * version - which is the whole point of the fix.
 */
class PerVersionFoodHealTest {
    private static final String ALPHA = "historical_experience:alpha";
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";

    @AfterEach
    void clearSession() {
        ActiveVersion.clear();
    }

    @Test
    void alphaDoesNotHealFoodThatDidNotExistYet() {
        ActiveVersion.set(ALPHA);

        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:cod"),
            "raw fish arrives in Alpha 1.2.0, so an Alpha v1.1.2_01 world must not heal by its figure");
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:cooked_cod"),
            "cooked fish likewise");
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:cookie"),
            "cookies arrive in Beta 1.4");
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:cake"),
            "cake arrives in Beta 1.2");
    }

    @Test
    void alphaStillHealsTheFoodItDidHave() {
        ActiveVersion.set(ALPHA);

        assertEquals(4, HistoricalFoodHelper.researchedHeal("minecraft:apple"));
        assertEquals(5, HistoricalFoodHelper.researchedHeal("minecraft:bread"));
        assertEquals(10, HistoricalFoodHelper.researchedHeal("minecraft:mushroom_stew"));
        assertEquals(3, HistoricalFoodHelper.researchedHeal("minecraft:porkchop"));
        assertEquals(8, HistoricalFoodHelper.researchedHeal("minecraft:cooked_porkchop"));
    }

    @Test
    void betaOnePointSevenPointThreeStillHealsItsOwnFullTable() {
        ActiveVersion.set(BETA_1_7_3);

        assertEquals(2, HistoricalFoodHelper.researchedHeal("minecraft:cod"),
            "Beta 1.7.3 does have fish, and its figure comes from the client jar");
        assertEquals(5, HistoricalFoodHelper.researchedHeal("minecraft:cooked_cod"));
        assertEquals(1, HistoricalFoodHelper.researchedHeal("minecraft:cookie"),
            "cookies exist by Beta 1.7.3");
        assertEquals(42, HistoricalFoodHelper.researchedHeal("minecraft:golden_apple"));
    }

    @Test
    void anUnresearchedVersionFallsBackRatherThanAnsweringWithModernNutrition() {
        // Infdev declares no researched heals at all. The fallback keeps its behaviour as it was rather than
        // silently becoming modern, which would be an equally unevidenced answer.
        ActiveVersion.set("historical_experience:infdev_420");

        assertEquals(4, HistoricalFoodHelper.researchedHeal("minecraft:apple"),
            "the pre-1.8 table is the documented fallback for a version whose heals were never researched");
    }
}

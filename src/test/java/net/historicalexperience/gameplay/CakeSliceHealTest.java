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

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Cake in the pre-hunger era. research/minecraft-version-history.md: "Beta 1.2 cake (3 health per slice,
 * 18 for the whole cake)". Cake is the only pre-hunger food whose heal came from a BLOCK interaction, so the item-food
 * table could not cover it and a Beta 1.2-1.7.3 cake restored nothing.
 */
class CakeSliceHealTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the window is Beta 1.2 through Beta 1.7.3")
    void window() {
        for (String has : new String[] {"beta_1_2", "beta_1_3", "beta_1_4", "beta_1_5", "beta_1_6", "beta_1_7_3"}) {
            assertTrue(Features.active(Feature.CAKE_SLICE_HEALS, P + has), has);
            assertEquals(3.0F, HistoricalFoodRules.cakeSliceHeal(P + has), has + " heals three per slice");
        }
        for (String not : new String[] {"beta_1_0", "beta_1_8_1", "release_1_0_0"}) {
            assertFalse(Features.active(Feature.CAKE_SLICE_HEALS, P + not), not);
            assertEquals(0.0F, HistoricalFoodRules.cakeSliceHeal(P + not), not);
        }
        assertFalse(Features.active(Feature.CAKE_SLICE_HEALS, (String) null));
        assertEquals(0.0F, HistoricalFoodRules.cakeSliceHeal(null));
    }
}

/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
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
 * Level: unit. minecraft.wiki, Cake history: Beta 1.8 Pre-release "Cake now restores 12 hunger and its slice restores 2";
 * 14w27a "Cake slices available when eating cakes have been increased from 6 to 7, restoring 14 in total." 14w27a is a
 * Release 1.8 snapshot, above the newest declared build, so every declared version has six slices.
 */
class CakeSlicesTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("six slices in every declared version, the modern seven in an ordinary world")
    void slices() {
        for (String version : new String[] {"beta_1_2", "beta_1_7_3", "beta_1_8_1", "release_1_0_0", "release_1_6_4"}) {
            assertTrue(Features.active(Feature.CAKE_SIX_SLICES, P + version), version);
            assertEquals(5, HistoricalFoodRules.cakeBitesMax(P + version), version + " stops at five bites");
        }
        assertFalse(Features.active(Feature.CAKE_SIX_SLICES, (String) null));
        assertEquals(6, HistoricalFoodRules.cakeBitesMax(null), "an ordinary world keeps the modern seventh bite");
        assertEquals(6, HistoricalFoodRules.cakeBitesMax("moderner_beta:beta"));
    }

    @Test
    @DisplayName("the two heal paths total the documented whole-cake amounts")
    void totals() {
        // Before hunger: 3 health per slice, six slices. From Beta 1.8: 2 hunger per slice, six slices.
        assertEquals(3.0F, HistoricalFoodRules.cakeSliceHeal(P + "beta_1_7_3"), "3 health per slice pre-hunger");
        assertEquals(0.0F, HistoricalFoodRules.cakeSliceHeal(P + "beta_1_8_1"), "hunger era does not heal directly");
        // 6 * 3 = 18 health; 6 * 2 = 12 hunger - the numbers the wiki rows state.
        assertEquals(18.0F, 3.0F * (HistoricalFoodRules.cakeBitesMax(P + "beta_1_7_3") + 1));
        assertEquals(12, 2 * (HistoricalFoodRules.cakeBitesMax(P + "release_1_6_4") + 1));
    }
}

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
 * Level: unit. minecraft.wiki, Pressure Plate history, Beta 1.3: "Pressure plates now require only two wood planks/stone
 * to craft; previous versions required three (this recipe now creates slabs)."
 *
 * <p>The OUTPUT count before Beta 1.3 is not stated by the row, so the supplied shapes yield one and that is recorded
 * as an assumption in {@link HistoricalPressurePlates}.
 */
class PressurePlateTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the three-block shapes apply before Beta 1.3 and the modern two-block shape from it")
    void window() {
        for (String early : new String[] {"alpha_1_0_1", "alpha_1_2_0", "beta_1_0", "beta_1_2"}) {
            assertTrue(Features.active(Feature.PRESSURE_PLATE_THREE_BLOCKS, P + early), early);
            assertTrue(RemovedRecipes.replacesModernRecipe(P + early, "minecraft:oak_pressure_plate"), early);
            assertTrue(RemovedRecipes.replacesModernRecipe(P + early, "minecraft:stone_pressure_plate"), early);
        }
        for (String later : new String[] {"beta_1_3", "beta_1_7_3", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.PRESSURE_PLATE_THREE_BLOCKS, P + later), later);
            assertFalse(RemovedRecipes.replacesModernRecipe(P + later, "minecraft:oak_pressure_plate"), later);
        }
    }

    @Test
    @DisplayName("both shapes are three matching blocks in a row yielding one plate")
    void shapes() {
        var recipes = HistoricalPressurePlates.recipes();
        assertEquals(2, recipes.size());
        for (var recipe : recipes) {
            assertEquals(3, recipe.ingredients().size(), recipe.result());
            assertEquals(1, recipe.count(), recipe.result() + " assumes the modern output");
            assertTrue(recipe.ingredients().stream().distinct().count() == 1,
                recipe.result() + " must be three of one block");
        }
    }
}

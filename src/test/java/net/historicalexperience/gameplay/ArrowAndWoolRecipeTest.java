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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The Alpha-era arrow and wool recipes (audit A-12 and A-13), and the Beta-era gate that has to refuse
 * modern Minecraft's replacements.
 *
 * <p>minecraft.wiki, Arrow history: Alpha v1.0.14 "The tip in crafting is now made from flint rather than iron
 * ingots". minecraft.wiki, Wool history: "1.6.6 The crafting recipe of wool has been changed, reducing the amount of
 * string required from 9 to 4 pieces". Before each boundary the modern recipe has to be refused, or the player simply
 * uses whichever one the registry happens to contain.
 */
class ArrowAndWoolRecipeTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("arrows: iron tip before Alpha v1.0.14, flint after")
    void arrowRecipe() {
        assertTrue(RemovedRecipes.replacesModernRecipe(P + "alpha_1_0_1", "minecraft:arrow"));
        assertTrue(RemovedRecipes.replacesModernRecipe(P + "alpha_1_0_11", "minecraft:arrow"));
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "alpha_1_0_14", "minecraft:arrow"),
            "v1.0.14 switches the tip to flint, which is the modern recipe");
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "beta_1_7_3", "minecraft:arrow"));
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "indev_latest", "minecraft:arrow"),
            "Indev uses the extracted per-build recipe table, not this path");

        var arrow = RemovedRecipes.ARROW_IRON_TIP;
        assertEquals(1, arrow.width());
        assertEquals(3, arrow.height());
        assertEquals("minecraft:iron_ingot", arrow.ingredients().get(0));
        assertEquals("minecraft:stick", arrow.ingredients().get(1));
        assertEquals("minecraft:feather", arrow.ingredients().get(2));
        assertEquals("minecraft:arrow", arrow.result());
        assertEquals(4, arrow.count(), "the recipe yields four arrows");
    }

    @Test
    @DisplayName("wool: nine string until Beta 1.6.6, four after")
    void woolRecipe() {
        assertTrue(RemovedRecipes.replacesModernRecipe(P + "alpha_1_0_1", "minecraft:white_wool"));
        assertTrue(RemovedRecipes.replacesModernRecipe(P + "beta_1_6", "minecraft:white_wool"));
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "beta_1_7_3", "minecraft:white_wool"));
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "release_1_6_4", "minecraft:white_wool"));

        var wool = RemovedRecipes.WOOL_FROM_NINE_STRING;
        assertEquals(3, wool.width());
        assertEquals(3, wool.height());
        assertEquals(9, wool.ingredients().stream().filter("minecraft:string"::equals).count());
        assertEquals("minecraft:white_wool", wool.result());
    }

    @Test
    @DisplayName("the gate refuses the modern shape exactly in the window where the historical one applies")
    void gateFollowsTheWindow() {
        assertTrue(HistoricalCraftingEra.isRefused(P + "alpha_1_0_1", "minecraft:arrow", null));
        assertTrue(HistoricalCraftingEra.isRefused(P + "beta_1_0", "minecraft:white_wool", null));
        assertFalse(HistoricalCraftingEra.isRefused(P + "alpha_1_0_14", "minecraft:arrow", null));
        assertFalse(HistoricalCraftingEra.isRefused(P + "beta_1_7_3", "minecraft:white_wool", null));
        assertFalse(HistoricalCraftingEra.isRefused(null, "minecraft:arrow", null));
    }
}

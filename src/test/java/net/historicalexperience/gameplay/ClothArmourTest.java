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
 * Level: unit. Audit A-14: in Alpha v1.0.1, v1.0.4 and v1.0.6 the only armour recipe was the modern leather one, but
 * leather has no legitimate source there - cows arrive in Alpha v1.0.8 ("The crafting recipe of cloth/leather armor
 * has been changed from cloth to leather", minecraft.wiki, Alpha v1.0.8) - so those builds had no craftable armour at
 * all. The era's armour was cloth, i.e. wool, in the same four shapes.
 */
class ClothArmourTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the four armour pieces are supplied from cloth in v1.0.1-v1.0.6, and not after")
    void window() {
        for (String early : new String[] {"alpha_1_0_1", "alpha_1_0_4", "alpha_1_0_6"}) {
            for (String piece : new String[] {"helmet", "chestplate", "leggings", "boots"}) {
                assertTrue(RemovedRecipes.replacesModernRecipe(P + early, "minecraft:leather_" + piece),
                    early + " crafts the " + piece + " from cloth");
            }
        }
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "alpha_1_0_11", "minecraft:leather_helmet"),
            "v1.0.8 switched the recipe to leather");
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "beta_1_7_3", "minecraft:leather_helmet"));
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "indev_latest", "minecraft:leather_helmet"),
            "Indev uses the extracted recipe table");
        assertFalse(RemovedRecipes.replacesModernRecipe(null, "minecraft:leather_helmet"));
    }

    @Test
    @DisplayName("the shapes are the vanilla armour patterns, made of cloth")
    void shapes() {
        assertEquals(4, RemovedRecipes.CLOTH_ARMOUR.size());
        for (var recipe : RemovedRecipes.CLOTH_ARMOUR) {
            assertTrue(recipe.result().startsWith("minecraft:leather_"), recipe.result());
            assertTrue(recipe.ingredients().stream().filter(i -> !i.isEmpty()).allMatch("minecraft:white_wool"::equals),
                recipe.id() + " must be made of cloth only");
        }
        assertEquals(5, wool(RemovedRecipes.CLOTH_ARMOUR.get(0)), "helmet");
        assertEquals(8, wool(RemovedRecipes.CLOTH_ARMOUR.get(1)), "chestplate");
        assertEquals(7, wool(RemovedRecipes.CLOTH_ARMOUR.get(2)), "leggings");
        assertEquals(4, wool(RemovedRecipes.CLOTH_ARMOUR.get(3)), "boots");
    }

    @Test
    @DisplayName("the gate refuses the leather shape only in the cloth window")
    void gate() {
        assertTrue(HistoricalCraftingEra.isRefused(P + "alpha_1_0_1", "minecraft:leather_helmet", null));
        assertFalse(HistoricalCraftingEra.isRefused(P + "alpha_1_0_11", "minecraft:leather_helmet", null));
    }

    private static long wool(RemovedRecipes.Removed recipe) {
        return recipe.ingredients().stream().filter("minecraft:white_wool"::equals).count();
    }
}

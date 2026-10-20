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
 * Level: unit. minecraft.wiki, Java Edition 1.2.1: "Crafting recipe for slabs now yields 6 instead of 3"; "Ladders: ...
 * Crafting recipe now yields 3 ladders instead of 2". This project's Release 1.2.5 profile is the 1.2.1 baseline, so
 * every declared build before it crafts three slabs and two ladders.
 */
class SlabLadderCountsTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the smaller counts apply before Release 1.2.5 and the modern counts from it")
    void window() {
        for (String early : new String[] {"beta_1_3", "beta_1_7_3", "release_1_0_0", "release_1_1"}) {
            assertTrue(Features.active(Feature.LEGACY_SLAB_LADDER_COUNTS, P + early), early);
            assertTrue(RemovedRecipes.replacesModernRecipe(P + early, "minecraft:oak_slab"), early);
            assertTrue(RemovedRecipes.replacesModernRecipe(P + early, "minecraft:ladder"), early);
        }
        for (String later : new String[] {"release_1_2_5", "release_1_3", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.LEGACY_SLAB_LADDER_COUNTS, P + later), later);
            assertFalse(RemovedRecipes.replacesModernRecipe(P + later, "minecraft:oak_slab"), later);
            assertFalse(RemovedRecipes.replacesModernRecipe(P + later, "minecraft:ladder"), later);
        }
    }

    @Test
    @DisplayName("each supplied shape has the era's count and ingredients")
    void shapes() {
        // oak, cobblestone, sandstone and smooth-stone slabs, plus the ladder: the stone slab joined this list
        // when audit A-11 established that the era's stone slab is today's smooth_stone_slab.
        assertEquals(5, RemovedRecipes.LEGACY_COUNTS.size());
        for (var recipe : RemovedRecipes.LEGACY_COUNTS) {
            if (recipe.result().equals("minecraft:ladder")) {
                assertEquals(2, recipe.count(), "two ladders");
                assertEquals(7, recipe.ingredients().stream().filter("minecraft:stick"::equals).count());
            } else {
                assertEquals(3, recipe.count(), recipe.result() + " yields three");
                assertTrue(recipe.result().endsWith("_slab"), recipe.result());
            }
        }
    }
}

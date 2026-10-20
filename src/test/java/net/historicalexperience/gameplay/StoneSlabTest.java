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

import net.historicalexperience.generation.HistoricalBlockAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audit A-11. minecraft.wiki, Slab history: "0.26 SURVIVAL TEST Added stone (now smooth stone) slabs";
 * 20100130 "Slabs have now received a crafting recipe: 3 slabs from 3 cobblestone"; Beta 1.3 added a DIFFERENT slab,
 * "stone" (now cobblestone). So the era's stone slab is today's `smooth_stone_slab`, and the modern `stone_slab` is the
 * 1.14 block that did not exist in any declared version.
 *
 * <p>The old implementation refused the era block as "Beta 1.3" while allowing the 1.14 block with the modern recipe.
 */
class StoneSlabTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the era stone slab is present from the earliest declared build")
    void eraSlabIsAvailable() {
        for (String version : new String[] {"indev_latest", "infdev_420", "alpha_1_0_1", "beta_1_0", "beta_1_2"}) {
            assertEquals(HistoricalBlockAvailability.Verdict.ALLOWED,
                HistoricalBlockAvailability.judge(P + version, "minecraft:smooth_stone_slab"),
                version + " had stone slabs");
        }
    }

    @Test
    @DisplayName("the 1.14 stone_slab is refused everywhere, and the era slab's old recipe is supplied")
    void modernBlockIsRefused() {
        for (String version : new String[] {"alpha_1_0_1", "beta_1_7_3", "release_1_6_4"}) {
            assertTrue(HistoricalCraftingEra.isRefused(P + version, "minecraft:stone_slab", "minecraft:stone_slab"),
                version + " must not craft the 1.14 stone slab");
        }
        assertTrue(RemovedRecipes.replacesModernRecipe(P + "beta_1_7_3", "minecraft:smooth_stone_slab"),
            "before Release 1.2.1 the era slab is supplied");
        assertFalse(RemovedRecipes.replacesModernRecipe(P + "release_1_2_5", "minecraft:smooth_stone_slab"),
            "from Release 1.2.1 the modern six-slab recipe is the era's recipe");
    }

    @Test
    @DisplayName("the supplied shape is 3 cobblestone in a row yielding three slabs")
    void shape() {
        var recipe = RemovedRecipes.LEGACY_COUNTS.stream()
            .filter(r -> r.result().equals("minecraft:smooth_stone_slab")).findFirst().orElseThrow();
        assertEquals(3, recipe.count());
        assertEquals(3, recipe.ingredients().size());
        assertTrue(recipe.ingredients().stream().allMatch("minecraft:cobblestone"::equals));
    }
}

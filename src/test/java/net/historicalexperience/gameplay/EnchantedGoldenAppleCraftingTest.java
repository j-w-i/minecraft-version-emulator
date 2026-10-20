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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The pre-1.9 enchanted golden apple, reported by the owner as uncraftable in a Release 1.3 world.
 *
 * <h2>The history this asserts</h2>
 *
 * <p>minecraft.wiki, Enchanted Golden Apple, Java Edition history:
 *
 * <ul>
 *   <li><b>1.3.1 (12w21a)</b> - "Added a second tier of the golden apple ... It can be crafted with 8 blocks of gold
 *       and one apple." Effects: Resistance 5:00, Fire Resistance 5:00, Regeneration IV 0:30, 4 hunger.</li>
 *   <li><b>1.6.1 (13w24b)</b> - "now give the player Absorption I as well, and Regeneration IV has been updated to
 *       Regeneration V."</li>
 *   <li><b>1.9 (15w44a)</b> - "Enchanted golden apples are now uncraftable ... have been added to monster room, desert
 *       pyramid, and mineshaft chests."</li>
 * </ul>
 *
 * <p>So a Release 1.3 through 1.8 world must be able to craft it, with the gold-block recipe, and the item must not be
 * refused by the crafting gate. The two things that were missing were the recipe (modern Minecraft removed it in 1.9,
 * so no vanilla recipe exists to permit) and the allow-list entry.
 */
class EnchantedGoldenAppleCraftingTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the gold-block recipe is supplied exactly where the item was craftable")
    void recipeAppliesToTheRightVersions() {
        assertTrue(RemovedRecipes.appliesTo(P + "release_1_3"), "12w21a is Release 1.3.1");
        assertTrue(RemovedRecipes.appliesTo(P + "release_1_4"));
        assertTrue(RemovedRecipes.appliesTo(P + "release_1_5"));
        assertTrue(RemovedRecipes.appliesTo(P + "release_1_6_4"));

        assertFalse(RemovedRecipes.appliesTo(P + "release_1_2_5"), "before 12w21a there is only one apple tier");
        assertFalse(RemovedRecipes.appliesTo(P + "beta_1_9_pre_3"));
        assertFalse(RemovedRecipes.appliesTo(P + "beta_1_8_1"));
        assertFalse(RemovedRecipes.appliesTo(P + "infdev_420"));
        assertFalse(RemovedRecipes.appliesTo(null), "an ordinary world gets no historical recipe");
        assertFalse(RemovedRecipes.appliesTo("moderner_beta:beta"));
        assertFalse(RemovedRecipes.appliesTo("example:not_a_version"));
    }

    @Test
    @DisplayName("the recipe is eight gold blocks around an apple, producing one enchanted golden apple")
    void recipeShapeIsTheWitchRow() {
        RemovedRecipes.Removed recipe = RemovedRecipes.ENCHANTED_GOLDEN_APPLE;
        assertEquals(3, recipe.width());
        assertEquals(3, recipe.height());
        assertEquals("minecraft:enchanted_golden_apple", recipe.result());
        assertEquals(1, recipe.count());

        List<String> slots = recipe.ingredients();
        assertEquals("minecraft:apple", slots.get(4), "the apple is the centre of the grid");
        for (int slot = 0; slot < slots.size(); slot++) {
            if (slot == 4) {
                continue;
            }
            assertEquals("minecraft:gold_block", slots.get(slot), "slot " + slot + " must be a block of gold");
        }
    }

    @Test
    @DisplayName("the crafting gate allows the item where the recipe exists, and refuses it earlier")
    void gateAgreesWithTheRecipe() {
        for (String later : List.of("release_1_3", "release_1_4", "release_1_5", "release_1_6_4")) {
            assertFalse(HistoricalCraftingEra.isRefused(P + later, "minecraft:enchanted_golden_apple", null),
                later + " had the gold-block recipe, so the gate must not refuse its result");
        }
        for (String earlier : List.of("release_1_2_5", "release_1_1", "release_1_0_0", "beta_1_8_1")) {
            assertTrue(HistoricalCraftingEra.isRefused(P + earlier, "minecraft:enchanted_golden_apple", null),
                earlier + " predates 12w21a, so the item must not be craftable");
        }
        assertFalse(HistoricalCraftingEra.isRefused(null, "minecraft:enchanted_golden_apple", null),
            "an ordinary world is never gated");
    }

    @Test
    @DisplayName("the effects are 1.3.1's, with 13w24b's Absorption and Regeneration V from Release 1.6.1")
    void effectValuesFollowTheHistory() {
        // The values the food hook assembles the effect list from. They are plain numbers so this test needs no registry.
        assertEquals(600, HistoricalFoodHelper.enchantedGoldenAppleRegenerationTicks(), "30 seconds of Regeneration");
        assertEquals(3, HistoricalFoodHelper.enchantedGoldenAppleRegenerationAmplifier(false), "Regeneration IV");
        assertEquals(4, HistoricalFoodHelper.enchantedGoldenAppleRegenerationAmplifier(true), "Regeneration V");
        assertEquals(6000, HistoricalFoodHelper.enchantedGoldenAppleResistanceTicks(), "Resistance 5:00");
        assertEquals(6000, HistoricalFoodHelper.enchantedGoldenAppleFireResistanceTicks(), "Fire Resistance 5:00");
        assertEquals(2400, HistoricalFoodHelper.enchantedGoldenAppleAbsorptionTicks(),
            "Absorption I 2:00 - the duration is an assumption, see HistoricalFoodHelper");
    }

    @Test
    @DisplayName("the two windows meet at Release 1.6.1, which is not declared, so Release 1.6.4 is the first build with it")
    void absorptionBoundarySitsBetweenDeclaredBuilds() {
        HistoricalVersion release15 = HistoricalVersions.byId(P + "release_1_5").orElseThrow();
        HistoricalVersion release164 = HistoricalVersions.byId(P + "release_1_6_4").orElseThrow();

        assertTrue(Features.active(Feature.LEGACY_ENCHANTED_GOLDEN_APPLE, release15),
            "Release 1.5 still has the second tier");
        assertFalse(Features.active(Feature.ENCHANTED_GOLDEN_APPLE_ABSORPTION, release15),
            "Absorption arrives in 13w24b, after Release 1.5");
        assertTrue(Features.active(Feature.ENCHANTED_GOLDEN_APPLE_ABSORPTION, release164),
            "Release 1.6.4 is the first declared build at or after 13w24b");
        assertNotNull(HistoricalVersions.byId(P + "release_1_6_4").orElse(null));
    }

    @Test
    @DisplayName("every removed recipe names a real item id, so building it cannot fail at lookup time")
    void recipeItemsAreWellFormed() {
        for (RemovedRecipes.Removed recipe : RemovedRecipes.all()) {
            assertTrue(recipe.result().contains(":"), recipe.id() + " result must be namespaced");
            for (String slot : recipe.ingredients()) {
                if (!slot.isEmpty()) {
                    assertTrue(slot.contains(":"), recipe.id() + " ingredient must be namespaced: " + slot);
                }
            }
        }
    }
}

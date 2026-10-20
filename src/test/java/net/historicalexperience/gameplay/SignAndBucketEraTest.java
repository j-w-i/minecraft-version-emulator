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

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.minecraft.SharedConstants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. minecraft.wiki, Java Edition 1.3.1: "Signs: Stack up to 16 now. Crafting now gives 3 signs instead of
 * 1." and "Empty buckets: Stack up to 16 now." So Release 1.0.0, 1.1 and 1.2.5 craft one sign per recipe and cannot
 * stack buckets.
 */
class SignAndBucketEraTest {
    private static final String P = "historical_experience:";

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @AfterEach
    void clear() {
        ActiveVersion.clear();
    }

    @Test
    @DisplayName("the one-sign recipe applies before Release 1.3.1 and not from it")
    void signWindow() {
        for (String early : new String[] {"release_1_0_0", "release_1_1", "release_1_2_5"}) {
            assertTrue(Features.active(Feature.SIGN_RECIPE_ONE, P + early), early);
            assertTrue(RemovedRecipes.replacesModernRecipe(P + early, "minecraft:oak_sign"), early);
        }
        for (String later : new String[] {"release_1_3", "release_1_5", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.SIGN_RECIPE_ONE, P + later), later);
            assertFalse(RemovedRecipes.replacesModernRecipe(P + later, "minecraft:oak_sign"), later);
        }
        var sign = RemovedRecipes.SIGN_ONE;
        assertEquals(1, sign.count(), "one sign, not three");
        assertEquals(6, sign.ingredients().stream().filter("minecraft:oak_planks"::equals).count());
        assertEquals(1, sign.ingredients().stream().filter("minecraft:stick"::equals).count());
        assertEquals("minecraft:oak_sign", sign.result());
    }

    @Test
    @DisplayName("empty buckets do not stack before Release 1.3.1")
    void bucketStack() {
        ActiveVersion.set(P + "release_1_2_5");
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.BUCKET)));
        assertTrue(HistoricalFoodStackLimit.foodDoesNotStack(new ItemStack(Items.BUCKET)));

        ActiveVersion.set(P + "release_1_3");
        assertFalse(HistoricalFoodStackLimit.foodDoesNotStack(new ItemStack(Items.BUCKET)),
            "buckets stack from Release 1.3.1");

        // The food rule is unaffected: bread is still limited before Beta 1.8.
        ActiveVersion.set(P + "beta_1_7_3");
        assertTrue(HistoricalFoodStackLimit.foodDoesNotStack(new ItemStack(Items.BREAD)));
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.BREAD)));

        ActiveVersion.clear();
        assertFalse(HistoricalFoodStackLimit.foodDoesNotStack(new ItemStack(Items.BUCKET)),
            "an ordinary world is never limited");
    }
}

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audit item "cookies cannot stack": before Beta 1.8 food does not stack, but the cookie is the documented
 * exception - a Beta 1.7.3 cookie IS the same item in a dedicated subclass whose only purpose is a stack size of 8
 * ({@code research/beta173-food-healing.json}: "Beta cookie saved in the {@code qk} subclass purely to raise stack
 * size to 8"; {@code research/infdev-registries.json}: "(101, 1, false, 8)", the last argument the stack size).
 */
class CookieStackLimitTest {
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
    @DisplayName("the cookie window is Beta 1.4 through Beta 1.7.3")
    void window() {
        for (String has : new String[] {"beta_1_4", "beta_1_5", "beta_1_6", "beta_1_7_3"}) {
            assertTrue(Features.active(Feature.COOKIE_STACKS_EIGHT, P + has), has + " has stackable cookies");
        }
        for (String not : new String[] {"beta_1_3", "beta_1_8_1", "release_1_0_0"}) {
            assertFalse(Features.active(Feature.COOKIE_STACKS_EIGHT, P + not), not);
        }
        assertFalse(Features.active(Feature.COOKIE_STACKS_EIGHT, (String) null));
    }

    @Test
    @DisplayName("a cookie gets 8 in that window and 1 outside it; other food stays 1")
    void limits() {
        ActiveVersion.set(P + "beta_1_7_3");
        assertEquals(8, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.COOKIE)));
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.BREAD)),
            "only the cookie is the exception");
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(ItemStack.EMPTY));

        ActiveVersion.set(P + "beta_1_3");
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.COOKIE)),
            "cookies do not stack in Beta 1.3, before they existed as a stackable item");

        ActiveVersion.set(P + "beta_1_8_1");
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.COOKIE)),
            "all food stacks from Beta 1.8, so this path is not used there");

        ActiveVersion.clear();
        assertEquals(1, HistoricalFoodStackLimit.historicalStackLimit(new ItemStack(Items.COOKIE)),
            "an ordinary world is never affected");
        assertTrue(BuiltInRegistries.ITEM.getKey(Items.COOKIE).toString().contains("cookie"));
    }
}

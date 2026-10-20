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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EarlyLeafDropsTest {
    @Test void everySourceQuantityBucketMatchesAllThreeArtifacts() {
        for (String profile : EarlyGameplayRecipes.profiles()) {
            var row = EarlyGameplayRecipes.leafDrop(profile, "minecraft:oak_leaves").orElseThrow();
            assertEquals("minecraft:oak_sapling", row.result()); assertEquals(1, row.count());
            assertEquals(10, row.chanceDenominator()); assertEquals(0, row.successRoll());
            for (int roll = 0; roll < 10; roll++) assertEquals(roll == 0 ? 1 : 0, row.countForRoll(roll));
        }
    }
    @Test void unknownProfilesAndOtherBlocksNeverInheritThisRule() {
        for (String profile : new String[] {null, "", "unknown:era", "historical_experience:alpha_1_0_1", "historical_experience:beta_1_7_3"}) {
            assertTrue(EarlyGameplayRecipes.leafDrop(profile, "minecraft:oak_leaves").isEmpty());
        }
        for (String profile : EarlyGameplayRecipes.profiles()) for (String block : new String[] {null, "", "minecraft:birch_leaves", "minecraft:oak_log", "minecraft:stone"}) {
            assertTrue(EarlyGameplayRecipes.leafDrop(profile, block).isEmpty());
        }
    }
    @Test void malformedDropRowsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> row(null, "minecraft:oak_sapling", 1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> row("oak_leaves", "minecraft:oak_sapling", 1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> row("minecraft:oak_leaves", null, 1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> row("minecraft:oak_leaves", "", 1, 10, 0));
        for (int count : new int[] {-1, 0, 65}) assertThrows(IllegalArgumentException.class,
            () -> row("minecraft:oak_leaves", "minecraft:oak_sapling", count, 10, 0));
        for (int chance : new int[] {-1, 0, 65537}) assertThrows(IllegalArgumentException.class,
            () -> row("minecraft:oak_leaves", "minecraft:oak_sapling", 1, chance, 0));
        for (int roll : new int[] {-1, 10}) assertThrows(IllegalArgumentException.class,
            () -> row("minecraft:oak_leaves", "minecraft:oak_sapling", 1, 10, roll));
    }
    @Test void impossibleRollsAreNotSilentlyAccepted() {
        var row = EarlyGameplayRecipes.leafDrop("historical_experience:infdev_611", "minecraft:oak_leaves").orElseThrow();
        for (int roll : new int[] {-1, 10, Integer.MAX_VALUE}) assertThrows(IllegalArgumentException.class, () -> row.countForRoll(roll));
    }
    private static EarlyGameplayRecipes.LeafDrop row(String block, String result, int count, int chance, int roll) {
        return new EarlyGameplayRecipes.LeafDrop(block, result, count, chance, roll);
    }
}

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
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.gameplay.HistoricalCraftingEra;
import net.historicalexperience.generation.HistoricalBlockAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audit G11 — the Prerelease 2 / Prerelease 3 boundary was shifted by one prerelease in both directions.
 * The expected side is {@code research/version-transitions.json}, whose per-build "introduced" lists are the project's
 * own sourced record: Pre1 adds nether bricks/stairs/fences and gold nuggets; Pre2 adds the potion system and its
 * ingredients (glass bottles, spider eyes, blaze powder, fermented spider eye, magma cream) and animal breeding; Pre3
 * adds the brewing stand, enchanting table, cauldron block, eye of ender, and the nether wart / end stone blocks
 * move with their dimensions.
 */
class Pre2Pre3BoundaryTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("breeding and tempting are Prerelease 2, so Pre2 and Pre3 both have them")
    void breedingIsPrereleaseTwo() {
        for (String before : new String[] {"beta_1_8_1", "beta_1_9_pre_1"}) {
            assertTrue(Features.active(Feature.NO_ANIMAL_BREEDING, P + before), before + " predates breeding");
            assertTrue(Features.active(Feature.NO_ANIMAL_TEMPTING, P + before), before + " predates tempting");
        }
        for (String has : new String[] {"beta_1_9_pre_2", "beta_1_9_pre_3", "release_1_0_0"}) {
            assertFalse(Features.active(Feature.NO_ANIMAL_BREEDING, P + has), has + " has breeding");
            assertFalse(Features.active(Feature.NO_ANIMAL_TEMPTING, P + has), has + " has tempting");
        }
    }

    @Test
    @DisplayName("Prerelease 1's own additions are craftable in Prerelease 1")
    void prereleaseOneContent() {
        for (String item : new String[] {"minecraft:nether_bricks", "minecraft:nether_brick_stairs",
                "minecraft:nether_brick_fence", "minecraft:gold_nugget"}) {
            assertFalse(HistoricalCraftingEra.isRefused(P + "beta_1_9_pre_1", item, null),
                "Prerelease 1 added " + item);
            assertTrue(HistoricalCraftingEra.isRefused(P + "beta_1_8_1", item, null),
                "Beta 1.8.1 predates " + item);
        }
    }

    @Test
    @DisplayName("Prerelease 2's potion ingredients are craftable in Prerelease 2")
    void prereleaseTwoContent() {
        for (String item : new String[] {"minecraft:glass_bottle",
                "minecraft:blaze_powder", "minecraft:fermented_spider_eye", "minecraft:magma_cream"}) {
            assertFalse(HistoricalCraftingEra.isRefused(P + "beta_1_9_pre_2", item, null),
                "Prerelease 2 has " + item);
            assertTrue(HistoricalCraftingEra.isRefused(P + "beta_1_9_pre_1", item, null),
                "Prerelease 1 predates " + item);
        }
    }

    @Test
    @DisplayName("Prerelease 3 keeps its own additions")
    void prereleaseThreeContent() {
        for (String item : new String[] {"minecraft:brewing_stand", "minecraft:enchanting_table",
                "minecraft:cauldron", "minecraft:ender_eye"}) {
            assertFalse(HistoricalCraftingEra.isRefused(P + "beta_1_9_pre_3", item, null),
                "Prerelease 3 added " + item);
            assertTrue(HistoricalCraftingEra.isRefused(P + "beta_1_9_pre_2", item, null),
                "Prerelease 2 predates " + item);
        }
    }

    @Test
    @DisplayName("nether wart is Pre1 content and end stone is Pre4 content, not Release 1.0")
    void blockDates() {
        assertEquals(HistoricalBlockAvailability.Verdict.ALLOWED,
            HistoricalBlockAvailability.judge(P + "beta_1_9_pre_1", "minecraft:nether_wart"));
        assertEquals(HistoricalBlockAvailability.Verdict.ANACHRONISTIC,
            HistoricalBlockAvailability.judge(P + "beta_1_8_1", "minecraft:nether_wart"));
        assertEquals(HistoricalBlockAvailability.Verdict.ALLOWED,
            HistoricalBlockAvailability.judge(P + "beta_1_9_pre_4", "minecraft:end_stone"));
        assertEquals(HistoricalBlockAvailability.Verdict.ANACHRONISTIC,
            HistoricalBlockAvailability.judge(P + "beta_1_9_pre_3", "minecraft:end_stone"));
    }
}

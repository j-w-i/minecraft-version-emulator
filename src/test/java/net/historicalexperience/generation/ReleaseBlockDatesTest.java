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
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import net.historicalexperience.gameplay.HistoricalCraftingEra;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audit G3 — the block table dated genuine Release 1.2.1/1.3.1/1.4.2/1.5 content to Release 1.6.4,
 * because the 1.6.4 extraction was the first PALETTE to register those names; the project has no extracted
 * 1.3/1.4/1.5 pages, so palette evidence could not tell those releases apart. The crafting gate trusts a timeline
 * boundary, so the consequence was visible: an ender chest could not be crafted in a 1.3 world, an anvil or beacon
 * in 1.4, or any Redstone Update block in 1.5.
 *
 * <p>Expected dates are the additions recorded in {@code research/version-transitions.json} for each release, which
 * is the project's own sourced record and the input the generator now uses.
 */
class ReleaseBlockDatesTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("Release 1.3.1 content is dated to Release 1.3, not 1.6.4")
    void release13Content() {
        assertAllowed(P + "release_1_3", "minecraft:ender_chest");
        assertAllowed(P + "release_1_3", "minecraft:tripwire");
        // ...and earlier releases still cannot have it.
        assertAnachronistic(P + "release_1_2_5", "minecraft:ender_chest");
    }

    @Test
    @DisplayName("Release 1.4.2 content is dated to Release 1.4")
    void release14Content() {
        for (String block : new String[] {"minecraft:anvil", "minecraft:beacon", "minecraft:cobblestone_wall",
                "minecraft:flower_pot", "minecraft:carrots", "minecraft:potatoes", "minecraft:command_block"}) {
            assertAllowed(P + "release_1_4", block);
            assertAnachronistic(P + "release_1_3", block);
        }
    }

    @Test
    @DisplayName("the Redstone Update is dated to Release 1.5")
    void release15Content() {
        for (String block : new String[] {"minecraft:hopper", "minecraft:dropper", "minecraft:comparator",
                "minecraft:daylight_detector", "minecraft:activator_rail", "minecraft:quartz_block"}) {
            assertAllowed(P + "release_1_5", block);
            assertAnachronistic(P + "release_1_4", block);
        }
    }

    @Test
    @DisplayName("the crafting gate now admits each release's own blocks")
    void craftingGateFollowsTheDate() {
        assertFalse(HistoricalCraftingEra.isRefused(P + "release_1_3", "minecraft:ender_chest",
            "minecraft:ender_chest"), "a 1.3 world must be able to craft the block 1.3.1 added");
        assertFalse(HistoricalCraftingEra.isRefused(P + "release_1_4", "minecraft:anvil", "minecraft:anvil"));
        assertFalse(HistoricalCraftingEra.isRefused(P + "release_1_5", "minecraft:hopper", "minecraft:hopper"));
    }

    @Test
    @DisplayName("Release 1.6 content stays dated to Release 1.6")
    void release16Content() {
        assertAllowed(P + "release_1_6_4", "minecraft:hay_block");
        assertAnachronistic(P + "release_1_5", "minecraft:hay_block");
    }

    private static void assertAllowed(String version, String block) {
        assertEquals(HistoricalBlockAvailability.Verdict.ALLOWED,
            HistoricalBlockAvailability.judge(version, block), version + " should have " + block);
    }

    private static void assertAnachronistic(String version, String block) {
        assertEquals(HistoricalBlockAvailability.Verdict.ANACHRONISTIC,
            HistoricalBlockAvailability.judge(version, block), version + " should not have " + block);
    }
}

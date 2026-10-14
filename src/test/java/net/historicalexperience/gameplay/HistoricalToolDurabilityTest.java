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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tool durability before Beta 1.2, asserted against the values the reviewed research states.
 *
 * <p>Level: unit. {@link HistoricalToolDurability#durabilityFor} reads a fixed table plus the version registry, so
 * it needs no bootstrapped item registry - which is why the table takes an item id rather than an {@code ItemStack}.
 */
class HistoricalToolDurabilityTest {
    private static final String ALPHA_1_0_1 = "historical_experience:alpha_1_0_1";
    private static final String BETA_1_0 = "historical_experience:beta_1_0";
    private static final String BETA_1_2 = "historical_experience:beta_1_2";
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";

    @Test
    @DisplayName("the four stated materials get their pre-Beta-1.2 values on every tool shape")
    void statedMaterialsUseTheOldValues() {
        // version-transitions.json, Beta 1.2 changed list: "tool durability: wood 33 -> 59, stone 65 -> 131,
        // iron 129 -> 250, diamond 1025 -> 1561". The source states a figure per MATERIAL, so every shape of a
        // material shares it.
        assertEquals(33, HistoricalToolDurability.durabilityFor(ALPHA_1_0_1, "minecraft:wooden_sword"));
        assertEquals(33, HistoricalToolDurability.durabilityFor(ALPHA_1_0_1, "minecraft:wooden_pickaxe"));
        assertEquals(65, HistoricalToolDurability.durabilityFor(BETA_1_0, "minecraft:stone_pickaxe"));
        assertEquals(129, HistoricalToolDurability.durabilityFor(BETA_1_0, "minecraft:iron_axe"));
        assertEquals(1025, HistoricalToolDurability.durabilityFor(BETA_1_0, "minecraft:diamond_shovel"));

        // Every named item answers for an old version, so the table cannot have a hole that silently leaves one
        // tool on its modern value while its siblings changed.
        for (String item : HistoricalToolDurability.namedItems()) {
            assertEquals(Integer.valueOf(1025 * 0 + HistoricalToolDurability.durabilityFor(ALPHA_1_0_1, item)),
                HistoricalToolDurability.durabilityFor(BETA_1_0, item),
                item + " must answer the same for both old versions");
        }
    }

    @Test
    @DisplayName("the boundary is Beta 1.2, the build that raised them")
    void theBoundaryIsBetaOneTwo() {
        // Beta 1.2 itself HAS the modern values - it is the build that raised them - so it gets no override.
        for (String atOrAfter : new String[] {BETA_1_2, BETA_1_7_3, RELEASE_1_6_4}) {
            for (String item : HistoricalToolDurability.namedItems()) {
                assertNull(HistoricalToolDurability.durabilityFor(atOrAfter, item),
                    atOrAfter + " must keep the modern durability of " + item);
            }
        }

        // And every declared version older than it is covered, derived from the registry rather than listed.
        int covered = 0;
        for (HistoricalVersion version : HistoricalVersions.all()) {
            boolean old = version.chronology() < HistoricalVersions.byId(BETA_1_2).orElseThrow().chronology();
            Integer value = HistoricalToolDurability.durabilityFor(version.id(), "minecraft:iron_pickaxe");
            if (old) {
                covered++;
                assertEquals(129, value, version.id() + " predates Beta 1.2 and must use the old durability");
            } else {
                assertNull(value, version.id() + " is at or after Beta 1.2 and must not be overridden");
            }
        }
        assertTrue(covered >= 10,
            "only " + covered + " declared versions are older than Beta 1.2, which cannot be right");
    }

    @Test
    @DisplayName("gold is deliberately not claimed, and an unnamed item falls through")
    void goldAndUnnamedItemsMakeNoClaim() {
        // The same source sentence says gold tools gained faster mining "while retaining old durability", so there
        // is no old number. Naming one would be inventing a value.
        assertNull(HistoricalToolDurability.durabilityFor(ALPHA_1_0_1, "minecraft:golden_pickaxe"),
            "gold kept its durability in Beta 1.2, so the research gives no old value to apply");

        // Items the sentence does not name, and an undeclared version, both mean "no claim".
        for (String unnamed : new String[] {"minecraft:shears", "minecraft:flint_and_steel",
                "minecraft:fishing_rod", "minecraft:bow", "minecraft:shield", "minecraft:elytra"}) {
            assertNull(HistoricalToolDurability.durabilityFor(ALPHA_1_0_1, unnamed), unnamed + " is not stated");
        }
        assertNull(HistoricalToolDurability.durabilityFor(null, "minecraft:iron_pickaxe"));
        assertNull(HistoricalToolDurability.durabilityFor("moderner_beta:beta", "minecraft:iron_pickaxe"));
    }
}

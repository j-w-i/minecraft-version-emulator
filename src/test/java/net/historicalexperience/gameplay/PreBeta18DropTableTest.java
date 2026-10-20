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

import java.util.Map;
import java.util.Set;

import net.historicalexperience.version.DropRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The pre-Beta-1.8 drop table (audit G1) and the sheep death-wool window (audit A-02).
 *
 * <p>Every expected value below is a VERIFIED row of {@code research/feature-timeline.json}, whose own entry
 * carries the wiki quote and URL; the table's javadoc quotes them. The point of this test is that the era table
 * is the pre-1.8 one and that the sheep branch starts at Beta 1.7, not that the numbers were re-derived here.
 */
class PreBeta18DropTableTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the pre-1.8 table gives each mob the item its build dropped, never the modern one")
    void tableIsTheEraTable() {
        Map<String, DropRule> byEntity = HistoricalLootRules.PRE_BETA_1_8_BY_ENTITY;

        assertEquals("minecraft:feather", byEntity.get("minecraft:zombie").itemId(),
            "zombies drop feathers until the Beta 1.8 Pre-release");
        assertEquals("minecraft:arrow", byEntity.get("minecraft:skeleton").itemId(),
            "bones arrive in Beta 1.2 and this model holds one item per mob, so arrows stand");
        assertEquals("minecraft:gunpowder", byEntity.get("minecraft:creeper").itemId());
        assertEquals("minecraft:string", byEntity.get("minecraft:spider").itemId(),
            "spider eyes are Beta 1.8");
        assertEquals("minecraft:porkchop", byEntity.get("minecraft:pig").itemId(),
            "cooked-on-fire is Beta 1.5");
        assertEquals("minecraft:leather", byEntity.get("minecraft:cow").itemId(), "beef is Beta 1.8");
        assertEquals("minecraft:feather", byEntity.get("minecraft:chicken").itemId(), "raw chicken is Beta 1.8");
        assertEquals("minecraft:cooked_porkchop", byEntity.get("minecraft:zombified_piglin").itemId());
        assertEquals(2, byEntity.get("minecraft:zombified_piglin").maxExclusiveCount(), "0-1, so nextInt(2)");
    }

    @Test
    @DisplayName("no modern-only drop item appears in the era table")
    void noModernItems() {
        Set<String> modernOnly = Set.of("minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:beef",
            "minecraft:chicken", "minecraft:bone", "minecraft:ghast_tear");
        for (DropRule rule : HistoricalLootRules.PRE_BETA_1_8) {
            assertFalse(modernOnly.contains(rule.itemId()),
                rule.entityTypeId() + " would drop modern-only " + rule.itemId());
        }
    }

    @Test
    @DisplayName("sheep death wool starts at Beta 1.7 and stops at Release 1.8, not before Beta 1.7")
    void sheepDeathWoolWindow() {
        // Before Beta 1.7 wool came from punching the sheep; the death drop is the Beta 1.7 change.
        assertFalse(HistoricalLootRules.usesLegacySheepDrops(P + "alpha_1_0_1"), "Alpha must not drop wool on death");
        assertFalse(HistoricalLootRules.usesLegacySheepDrops(P + "alpha_1_2_2a"));
        assertFalse(HistoricalLootRules.usesLegacySheepDrops(P + "beta_1_0"));
        assertFalse(HistoricalLootRules.usesLegacySheepDrops(P + "beta_1_6"));
        assertTrue(HistoricalLootRules.usesLegacySheepDrops(P + "beta_1_7_3"), "Beta 1.7 added the death wool");
        assertTrue(HistoricalLootRules.usesLegacySheepDrops(P + "beta_1_8_1"));
        assertTrue(HistoricalLootRules.usesLegacySheepDrops(P + "release_1_6_4"), "mutton is Release 1.8");
        assertFalse(HistoricalLootRules.usesLegacySheepDrops(null), "an ordinary world is never gated");
    }
}

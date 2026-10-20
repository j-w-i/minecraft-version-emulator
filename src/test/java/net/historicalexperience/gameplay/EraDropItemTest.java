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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The owner-reported defect: chickens dropped no raw chicken and cows no beef in a Beta 1.8.1 world.
 *
 * <p>Cause: {@code HistoricalMobLootHelper} cached one global table per entity type, built from Beta 1.7.3's items,
 * and looked it up by entity type alone. So a 1.8.1 chicken resolved to the 1.7.3 feather table and a 1.8.1 cow to
 * the 1.7.3 leather table, no matter what the version's own rule said. The helper now builds from the version's rule;
 * this test pins the version-dependent item choice so the cache can never silently serve a neighbouring era again.
 */
class EraDropItemTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("Beta 1.8.1's Adventure Update drops raw chicken and raw beef")
    void beta181DropsMeat() {
        assertEquals("minecraft:chicken", HistoricalMobLootHelper.eraItemId(P + "beta_1_8_1", "minecraft:chicken"),
            "raw chicken arrives with the Beta 1.8 Pre-release");
        assertEquals("minecraft:beef", HistoricalMobLootHelper.eraItemId(P + "beta_1_8_1", "minecraft:cow"),
            "raw beef arrives with the Beta 1.8 Pre-release");
        assertEquals("minecraft:rotten_flesh",
            HistoricalMobLootHelper.eraItemId(P + "beta_1_8_1", "minecraft:zombie"),
            "rotten flesh replaces feathers in Beta 1.8");
    }

    @Test
    @DisplayName("Beta 1.7.3 and Alpha keep the era items, so the fix is version-dependent, not a global swap")
    void pre18KeepsEraItems() {
        assertEquals("minecraft:feather", HistoricalMobLootHelper.eraItemId(P + "beta_1_7_3", "minecraft:chicken"));
        assertEquals("minecraft:leather", HistoricalMobLootHelper.eraItemId(P + "beta_1_7_3", "minecraft:cow"));
        assertEquals("minecraft:feather", HistoricalMobLootHelper.eraItemId(P + "alpha_1_0_1", "minecraft:zombie"));
        assertEquals("minecraft:feather", HistoricalMobLootHelper.eraItemId(P + "alpha_1_0_14", "minecraft:chicken"));
    }

    @Test
    @DisplayName("Beta 1.8.1 drops TWO items where the wiki says two, not one")
    void beta181DropsBothItems() {
        // Artifact-verified in research/historical-features.json: the b1.8 client's mob classes reference both fields.
        assertEquals(List.of("minecraft:feather"), extraItems("beta_1_8_1", "minecraft:chicken"));
        assertEquals(List.of("minecraft:leather"), extraItems("beta_1_8_1", "minecraft:cow"));
        assertEquals(List.of("minecraft:spider_eye"), extraItems("beta_1_8_1", "minecraft:spider"));
        assertEquals(List.of("minecraft:bone"), extraItems("beta_1_8_1", "minecraft:skeleton"));
        assertEquals(List.of("minecraft:rotten_flesh"), extraItems("beta_1_8_1", "minecraft:zombified_piglin"));
        // Mobs with only one drop in 1.8 have no second pool.
        assertEquals(List.of(), extraItems("beta_1_8_1", "minecraft:zombie"));
        assertEquals(List.of(), extraItems("beta_1_8_1", "minecraft:pig"));
        assertEquals(List.of(), extraItems("beta_1_8_1", "minecraft:creeper"));
    }

    @Test
    @DisplayName("pre-1.8 versions keep one item per mob: no second pools are borrowed")
    void pre18HasNoSecondPools() {
        assertEquals(List.of(), extraItems("beta_1_7_3", "minecraft:chicken"));
        assertEquals(List.of(), extraItems("alpha_1_0_1", "minecraft:cow"));
        assertEquals(List.of(), extraItems("release_1_6_4", "minecraft:cow"));
        assertEquals(Set.of("historical_experience:beta_1_8_1"), HistoricalLootRules.versionsWithExtraDrops());
    }

    private static List<String> extraItems(String version, String entity) {
        return HistoricalLootRules.extraFor(P + version, entity).stream()
            .map(HistoricalLootRules.ExtraDrop::itemId).toList();
    }

    @Test
    @DisplayName("Release profiles keep the modern tables, so no rule is invented for them")
    void releaseHasNoRule() {
        assertNull(HistoricalMobLootHelper.eraItemId(P + "release_1_6_4", "minecraft:chicken"));
        assertNull(HistoricalMobLootHelper.eraItemId(P + "release_1_6_4", "minecraft:cow"));
        assertNull(HistoricalMobLootHelper.eraItemId(null, "minecraft:cow"));
    }
}

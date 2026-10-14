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

import static org.junit.jupiter.api.Assertions.*;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Vanilla dimension generators have no historical marker of their own. */
class NetherEnforcementScopeTest {
    @AfterEach void clearScope() { HistoricalGenerationScope.clear(); }

    @Test void unmarkedDimensionUsesOwningWorldRecipe() {
        for (var version : HistoricalVersions.all()) {
            var revisions = version.revisions();
            String alias = revisions.get(revisions.size() - 1).alias();
            HistoricalGenerationScope.set(alias);
            assertEquals(alias, HistoricalFeatureExclusions.activeAlias(null));
            var exclusions = HistoricalFeatureExclusions.runtimeExclusionsForAlias(
                HistoricalFeatureExclusions.activeAlias(null));
            for (String id : java.util.List.of("minecraft:ore_gold_nether", "minecraft:ore_magma",
                    "minecraft:ore_ancient_debris_large", "minecraft:ore_debris_small",
                    "minecraft:ore_blackstone", "minecraft:patch_soul_fire")) {
                assertTrue(exclusions.stream().anyMatch(e -> e.featureId().equals(id)), version.id() + ": " + id);
            }
        }
    }

    @Test void ordinaryWorldAndStoppedServerHaveNoFallback() {
        HistoricalGenerationScope.clear();
        assertNull(HistoricalFeatureExclusions.activeAlias(null));
        assertTrue(HistoricalFeatureExclusions.runtimeExclusionsForAlias(null).isEmpty());
        HistoricalGenerationScope.set(HistoricalVersions.all().iterator().next().revisions().get(0).alias());
        HistoricalGenerationScope.clear();
        assertNull(HistoricalFeatureExclusions.activeAlias(null));
    }
}

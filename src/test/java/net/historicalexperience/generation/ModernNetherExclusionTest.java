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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Guards the 1.16 Nether content that reached historical worlds through vanilla's own biome.
 *
 * <p>The owner looked at an Alpha 1.2.0 Nether and asked whether it was correct. It largely was - one biome, no
 * crimson or warped forest, no fortresses, no bastion - but reading vanilla's {@code nether_wastes} feature list
 * showed five things no declared version had:
 *
 * <pre>
 *   ore_blackstone, ore_gold_nether, ore_ancient_debris_large, ore_debris_small, patch_soul_fire
 * </pre>
 *
 * <p>Ancient debris in a Beta world is the one a player would notice first. None of these were in the block
 * availability table, so the block veto could not catch them, and the table cannot even express "later than every
 * version this project declares" - which is why they are feature exclusions.
 *
 * <p>This is a unit test rather than a probe because exclusions are ids, and ids can be checked without a
 * bootstrapped registry. That is the opposite of the village style check, which needed one.
 */
class ModernNetherExclusionTest {
    private static Set<String> excludedIds(java.util.List<HistoricalFeatureExclusions.Exclusion> exclusions) {
        return exclusions.stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(Collectors.toSet());
    }

    @Test
    void everyModernNetherFeatureIsExcluded() {
        Set<String> excluded = excludedIds(HistoricalFeatureExclusions.MODERN_NETHER_CONTENT);

        for (String feature : new String[] {
            "minecraft:ore_ancient_debris_large",
            "minecraft:ore_debris_small",
            "minecraft:ore_blackstone",
            "minecraft:ore_gold_nether",
            "minecraft:patch_soul_fire"}) {
            assertTrue(excluded.contains(feature),
                feature + " is 1.16 Nether content and must be excluded from every declared version");
        }
    }

    @Test
    void netherQuartzIsDeliberatelyNotExcluded() {
        // Quartz is Release 1.5 content, and it is gated per version by which Nether biome a world preset names
        // rather than by a flat exclusion. Excluding it here would remove it from Release 1.5 and later too.
        assertFalse(excludedIds(HistoricalFeatureExclusions.MODERN_NETHER_CONTENT)
                .contains("minecraft:ore_quartz_nether"),
            "nether quartz is Release 1.5 content and is gated by the biome variant, not by this list");
    }

    @Test
    void theExclusionsReachEveryHistoricalVersionThroughTheSharedRoot() {
        // BETA_1_7_3_RESTRICTIONS is the root of every other list in the class: Beta 1.0 through 1.7.3 use it
        // directly, and Alpha and Infdev reach it through BEFORE_GRASS_EXISTED. So one prepend covers all 29
        // declared versions, and this asserts that the chain still carries them rather than trusting the comment.
        Set<String> fromRoot = excludedIds(HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS);
        Set<String> fromAlphaChain = excludedIds(HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED);

        for (String feature : excludedIds(HistoricalFeatureExclusions.MODERN_NETHER_CONTENT)) {
            assertTrue(fromRoot.contains(feature),
                feature + " must be in the root list, or Beta 1.0 through 1.7.3 would still place it");
            assertTrue(fromAlphaChain.contains(feature),
                feature + " must reach the Alpha chain too, or an Alpha 1.2.0 Nether would still place it - which "
                    + "is the build the owner was looking at");
        }
    }
}

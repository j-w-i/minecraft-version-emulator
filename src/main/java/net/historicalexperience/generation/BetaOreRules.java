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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;


/**
 * Which ores a historical generation revision places, with the parameters and height ranges that
 * Beta 1.7.3 used.
 *
 * <p>Every number here is already independently verified: the per-call-site parameters in
 * {@code research/beta173-ore-parameters-parent-reviewed.json} and the shape in
 * {@code research/beta173-ore-vein-geometry.json}. The height sampling is not always a single uniform
 * draw — lapis sums two {@code nextInt(16)} calls — so a rule carries two bounds and zero means "not
 * drawn", which keeps the RNG consumption order identical to the call site.
 *
 * <h2>Why a new revision</h2>
 *
 * <p>Ore placement is generated content: it changes what a stored chunk contains, so unlike the spawn
 * rule it is bound to a generation revision and revisions 1 to 4 select none. A world generated
 * before this revision therefore keeps exactly the ore placement its recipe recorded.
 *
 * <h2>Emerald is excluded, on evidence</h2>
 *
 * <p>Moderner Beta deliberately adds {@code moderner_beta:ore_emerald_y95} to its Beta biomes, and the
 * question of whether that matched Beta 1.7.3 was left open. It is now answered from the client: the
 * block registry {@code uu.class} contains exactly six ores — {@code oreCoal, oreDiamond, oreGold,
 * oreIron, oreLapis, oreRedstone} — and no emerald ore block, and no {@code oreEmerald} or
 * {@code emeraldOre} string exists anywhere in the jar. An unused <em>item</em> named {@code emerald}
 * does exist in the item name table, which is exactly the kind of leftover that makes this worth
 * checking rather than assuming: the item exists, the ore block does not, so Beta 1.7.3 generated no
 * emerald ore.
 */
public final class BetaOreRules {
    /**
     * One ore population.
     *
     * @param name             the ore's short name, for diagnostics
     * @param blockId          the block the vein writes
     * @param size             the vein size argument
     * @param attempts         how many veins are attempted per chunk
     * @param originBoundA     first height draw, exclusive; zero when not drawn
     * @param originBoundB     second height draw, exclusive; zero when not drawn. Lapis is the only
     *                         rule that uses it, summing two {@code nextInt(16)} calls.
     */
    public record OreRule(
        String name,
        String blockId,
        int size,
        int attempts,
        int originBoundA,
        int originBoundB
    ) {
        /** The largest origin height this rule can draw. */
        public int maxOriginY() {
            return Math.max(0, originBoundA - 1) + Math.max(0, originBoundB - 1);
        }

        /**
         * The largest y a vein from this rule can reach.
         *
         * <p>Derived from the verified algorithm rather than measured: the centre can sit up to two
         * blocks above the origin ({@code nextInt(3) - 2}), and the radius never exceeds
         * {@code (2 * size / 16) + 1}, so this is a generous but evidence-based ceiling.
         */
        public int maxReachableY() {
            return maxOriginY() + 2 + (int) Math.ceil(2.0 * size / 16.0) + 1;
        }
    }

    /** The Beta 1.7.3 population, in the order the client attempted it. */
    public static final List<OreRule> BETA_1_7_3 = List.of(
        new OreRule("dirt", "minecraft:dirt", 32, 20, 128, 0),
        new OreRule("gravel", "minecraft:gravel", 32, 10, 128, 0),
        new OreRule("coal", "minecraft:coal_ore", 16, 20, 128, 0),
        new OreRule("iron", "minecraft:iron_ore", 8, 20, 64, 0),
        new OreRule("gold", "minecraft:gold_ore", 8, 2, 32, 0),
        new OreRule("redstone", "minecraft:redstone_ore", 7, 8, 16, 0),
        new OreRule("diamond", "minecraft:diamond_ore", 7, 1, 16, 0),
        new OreRule("lapis", "minecraft:lapis_ore", 6, 1, 16, 16)
    );

    private static final LongAdder VEINS_ATTEMPTED = new LongAdder();
    private static final LongAdder BLOCKS_PLACED = new LongAdder();
    private static final Map<String, Integer> PLACED_BY_ORE = new ConcurrentHashMap<>();

    private BetaOreRules() {
    }

    /**
     * The ore rules a persisted recipe alias selects.
     *
     * <p>Resolved through the version registry: the alias identifies the version, and that version
     * declares whether it places its own ores. Revisions created before ore placement existed declare
     * an empty population, which is what keeps their recorded recipe meaning what it meant.
     */
    public static List<OreRule> forAlias(String rawAlias) {
        return net.historicalexperience.version.HistoricalVersions.byRevisionAlias(rawAlias)
            .flatMap(version -> version.revisions().stream()
                .filter(revision -> revision.alias().equals(rawAlias))
                .findFirst())
            .map(net.historicalexperience.version.HistoricalVersion.GenerationRevision::orePopulation)
            .orElse(List.of());
    }

    /** Every revision that currently selects the Beta ore population. */
    public static Set<String> oreRevisions() {
        return net.historicalexperience.version.HistoricalVersions.all().stream()
            .flatMap(version -> version.revisions().stream())
            .filter(net.historicalexperience.version.HistoricalVersion.GenerationRevision
                ::placesOwnOres)
            .map(net.historicalexperience.version.HistoricalVersion.GenerationRevision::alias)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** The identifiers of every block this population writes, for statistics and tests. */
    public static Set<String> writtenBlockIds() {
        return BETA_1_7_3.stream().map(OreRule::blockId)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** Records one attempted vein. */
    public static void recordVeinAttempt() {
        VEINS_ATTEMPTED.increment();
    }

    /** Records one vein block actually written into the world. */
    public static void recordBlockPlaced(String oreName) {
        BLOCKS_PLACED.increment();
        PLACED_BY_ORE.merge(oreName, 1, Integer::sum);
    }

    public static long veinsAttempted() {
        return VEINS_ATTEMPTED.sum();
    }

    public static long blocksPlaced() {
        return BLOCKS_PLACED.sum();
    }

    public static Map<String, Integer> placedByOre() {
        return Map.copyOf(PLACED_BY_ORE);
    }

    /** Diagnostics as an ordered map, for verdict reporting. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("oreRevisions", oreRevisions().toString());
        report.put("rules", Integer.toString(BETA_1_7_3.size()));
        report.put("veinsAttempted", Long.toString(VEINS_ATTEMPTED.sum()));
        report.put("blocksPlaced", Long.toString(BLOCKS_PLACED.sum()));
        report.put("placedByOre", PLACED_BY_ORE.toString());
        return report;
    }
}

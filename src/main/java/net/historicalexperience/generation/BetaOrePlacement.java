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

import net.historicalexperience.version.OreRule;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;

/**
 * Places a historical revision's ore population into one chunk.
 *
 * <p>This is the population step Beta 1.7.3 ran per chunk, expressed with the verified parameters and
 * the verified vein shape: for each rule, {@code attempts} times, draw x from the chunk's 16-block
 * span, draw y from the rule's heights, draw z from the chunk's span, then generate a vein of the
 * rule's size that replaces <b>stone and nothing else</b>.
 *
 * <p>Runs after vanilla's biome decoration rather than through it, because the ores must be placed only
 * in historical worlds and a biome's feature list is global. The revision's own exclusion list removes
 * the vanilla ore features from those same worlds, so the two never double up.
 *
 * <p><b>What is reproduced and what is not.</b> The chunk-local random is seeded the way vanilla seeds
 * decoration ({@code setDecorationSeed(levelSeed, chunkX, chunkZ)}), which is deterministic per chunk
 * and seed, but it is not Beta's own population random: Beta derived its stream from a different
 * sequence over the whole population pass. Individual vein positions therefore differ from a real Beta
 * 1.7.3 world with the same seed, while the attempted counts, sizes, height ranges and vein shape —
 * which is what a distribution is made of — are the verified ones.
 */
public final class BetaOrePlacement {
    private BetaOrePlacement() {
    }

    /**
     * The ore rules this persisted revision alias places.
     *
     * <p>A revision's own declared population always wins. Only when it declares none - which is every
     * revision created before ore placement existed - does the version's era population apply. This is a
     * code-level default rather than an edit to those revisions' stored data: revisions are append-only,
     * so the fix for an old world has to live in the fallback, not in the recipe it recorded.
     *
     * <p>The era population is {@link net.historicalexperience.version.HistoricalVersions#eraOrePopulation},
     * the same derivation the newest {@code _era_ores} revision stores, so an old revision and a current
     * one cannot disagree about which ores an era had.
     *
     * @param rawAlias the generator's persisted historical preset alias
     * @return the declared population, or the version's era population, or an empty list for an alias no
     *         declared version owns
     */
    public static List<OreRule> populationForAlias(String rawAlias) {
        List<OreRule> declared = BetaOreRules.forAlias(rawAlias);
        if (!declared.isEmpty()) {
            return declared;
        }
        return net.historicalexperience.version.HistoricalVersions.byRevisionAlias(rawAlias)
            .map(net.historicalexperience.version.HistoricalVersions::eraOrePopulation)
            .orElse(List.of());
    }

    /**
     * Places the ores selected by this generator's revision into one chunk, if any.
     *
     * @return the number of vein blocks written
     */
    public static int placeForChunk(ChunkGenerator generator, WorldGenLevel level, ChunkAccess chunk) {
        String alias = HistoricalFeatureExclusions.historicalAlias(generator);
        if (alias == null) {
            return 0;
        }
        var rules = populationForAlias(alias);
        if (rules.isEmpty()) {
            return 0;
        }

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        WorldgenRandom random = new WorldgenRandom(
            new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(level.getSeed(), chunkMinX, chunkMinZ);

        int placed = 0;
        for (OreRule rule : rules) {
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(rule.blockId()))
                .orElse(null);
            if (block == null) {
                continue;
            }
            BlockState oreState = block.defaultBlockState();
            for (int attempt = 0; attempt < rule.attempts(); attempt++) {
                int x = chunkMinX + random.nextInt(16);
                int y = sampleOriginY(random, rule);
                int z = chunkMinZ + random.nextInt(16);
                BetaOreRules.recordVeinAttempt();
                placed += placeVein(level, random, rule, oreState, x, y, z);
            }
        }
        return placed;
    }

    /** Beta's height draw, including the two-draw sum that lapis uses. */
    static int sampleOriginY(RandomSource random, OreRule rule) {
        int y = 0;
        if (rule.originBoundA() > 0) {
            y += random.nextInt(rule.originBoundA());
        }
        if (rule.originBoundB() > 0) {
            y += random.nextInt(rule.originBoundB());
        }
        return y;
    }

    private static int placeVein(
        WorldGenLevel level,
        RandomSource random,
        OreRule rule,
        BlockState oreState,
        int x,
        int y,
        int z
    ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] placed = {0};
        BetaOreVein.generate(random, x, y, z, rule.size(), (bx, by, bz) -> {
            pos.set(bx, by, bz);
            // Beta replaced stone and nothing else: no dirt, no gravel, no deepslate, and no
            // discard-on-air-exposure. Writing through the world region also means a vein stops at
            // the chunk border instead of reaching into unloaded space.
            if (!level.getBlockState(pos).is(Blocks.STONE)) {
                return false;
            }
            level.setBlock(pos, oreState, 2);
            placed[0]++;
            BetaOreRules.recordBlockPlaced(rule.name());
            return true;
        });
        return placed[0];
    }
}

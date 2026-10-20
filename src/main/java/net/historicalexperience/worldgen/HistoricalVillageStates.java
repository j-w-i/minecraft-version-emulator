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
/*
 * Ported from the NeoForge mod "OldVillages" (upstream file
 * src/main/java/cz/maxtechnik/old_villages/worldgen/OldVillagePieces.java, which held these block
 * states as fields on the piece class).
 * Upstream repository: https://github.com/MaxTechnik3796/old_villages (pinned revision
 * 7578013ec061303982539fac7606df67577ffb26).
 * Upstream declares "mod_license=MIT" in its gradle.properties. The reference checkout
 * ships no LICENSE file, so that gradle.properties declaration is the whole licence
 * statement available; it is recorded here rather than a licence file being claimed.
 * Licence: MIT.
 */
package net.historicalexperience.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns {@link HistoricalVillageBlocks}' ids into block states.
 *
 * <h2>Why the split</h2>
 *
 * <p>This is the only class in the port that needs Minecraft at all for the block decision, and it holds no
 * decision: it maps ids to states and fails loudly on an id this runtime does not have. The table it reads
 * is plain data, so the palette test can run in a plain JUnit environment, and a typo in a block id is a
 * startup failure rather than a piece that places nothing.
 */
public final class HistoricalVillageStates {
    /** The block states this runtime registers for the ids the village table names. */
    private static final Map<String, Block> RESOLVED = build();

    private HistoricalVillageStates() {
    }

    private static Map<String, Block> build() {
        Map<String, Block> resolved = new LinkedHashMap<>();
        // Every id the village table can produce, plus the version-independent states.
        Set<String> ids = new java.util.LinkedHashSet<>(HistoricalVillageBlocks.placedBlockIds());
        ids.addAll(HistoricalVillageBlocks.versionIndependentBlockIds());
        ids.addAll(HistoricalVillageBlocks.forbiddenBlocks().keySet());
        for (String id : ids) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            Block block = location == null ? null : BuiltInRegistries.BLOCK.get(location);
            if (block == null || block == Blocks.AIR && !"minecraft:air".equals(id)) {
                // A missing block means this port named something the runtime does not have. Failing here,
                // once, is far better than a village that quietly leaves a wall out.
                throw new IllegalStateException(
                    "the historical village names the block " + id + ", which this runtime does not "
                        + "register; the village tables and this Minecraft version disagree");
            }
            resolved.put(id, block);
        }
        return Map.copyOf(resolved);
    }

    /** The block for an id, or a failure naming the id. */
    public static Block block(String id) {
        Block block = RESOLVED.get(id);
        if (block == null) {
            throw new IllegalArgumentException(id + " is not one of the village's declared blocks; add it "
                + "to HistoricalVillageBlocks before using it here");
        }
        return block;
    }

    /** The default state for an id. */
    public static BlockState state(String id) {
        return block(id).defaultBlockState();
    }

}

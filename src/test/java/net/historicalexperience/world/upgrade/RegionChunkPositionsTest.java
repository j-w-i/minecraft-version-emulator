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
package net.historicalexperience.world.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reading which chunks a region file holds, on real region files.
 *
 * <p>This exists because the count came out zero in a live run while the conversion itself worked: the driver recorded
 * "0 pre-existing chunk(s)", so Moderner Beta had no chunk set to defer to and the deferral did nothing. A unit test
 * over an actual region file separates "the reader is wrong" from "the driver never asked".
 */
class RegionChunkPositionsTest {
    /** A region file from this project's own test worlds, skipped when the tree has no worlds checked out. */
    private static Path aRealRegionFile() {
        Path saves = Paths.get("run/client/saves");
        if (!Files.isDirectory(saves)) {
            return null;
        }
        try (var worlds = Files.list(saves)) {
            for (Path world : worlds.filter(Files::isDirectory).toList()) {
                Path region = world.resolve("region");
                if (!Files.isDirectory(region)) {
                    continue;
                }
                try (var files = Files.list(region)) {
                    var first = files.filter(path -> path.getFileName().toString().endsWith(".mca")).findFirst();
                    if (first.isPresent()) {
                        return first.get();
                    }
                }
            }
        } catch (java.io.IOException unreadable) {
            return null;
        }
        return null;
    }

    @Test
    @DisplayName("a real region file reports the chunks it holds, and they are not zero")
    void readsRealRegionFile() throws Exception {
        Path region = aRealRegionFile();
        if (region == null) {
            // No test world in this checkout; the assertions below need one.
            return;
        }
        var positions = HistoricalRegionBiomes.chunkPositions(region);
        assertFalse(positions.isEmpty(),
            "a region file that the biome census reads thousands of chunks from must report positions here too: "
                + region);

        // The positions must be the ones the file's own name implies, not arbitrary values.
        String[] parts = region.getFileName().toString().split("\\.");
        int regionX = Integer.parseInt(parts[1]);
        int regionZ = Integer.parseInt(parts[2]);
        for (long packed : positions) {
            int chunkX = net.minecraft.world.level.ChunkPos.getX(packed);
            int chunkZ = net.minecraft.world.level.ChunkPos.getZ(packed);
            assertTrue(chunkX >= regionX * 32 && chunkX < regionX * 32 + 32,
                "chunk X " + chunkX + " is outside region " + regionX);
            assertTrue(chunkZ >= regionZ * 32 && chunkZ < regionZ * 32 + 32,
                "chunk Z " + chunkZ + " is outside region " + regionZ);
        }
    }

    @Test
    @DisplayName("a world's region files together yield every chunk, which is what the record stores")
    void readsAWholeWorld() throws Exception {
        Path region = aRealRegionFile();
        if (region == null) {
            return;
        }
        Path world = region.getParent().getParent();
        var regions = HistoricalBiomeConversionDriver.regionFilesIn(world);
        assertFalse(regions.isEmpty(), "the driver found no region files in " + world);
        Set<Long> chunks = new java.util.HashSet<>();
        for (Path file : regions) {
            chunks.addAll(HistoricalRegionBiomes.chunkPositions(file));
        }
        assertFalse(chunks.isEmpty(),
            "the driver's own region list and reader together must find chunks, or Moderner Beta has nothing to defer to");
    }

    @Test
    @DisplayName("a region file that is not one, or is unreadable, reports nothing rather than throwing")
    void unreadableRegionIsQuiet(@TempDir Path directory) throws Exception {
        Path notARegion = directory.resolve("level.dat");
        Files.writeString(notARegion, "not a region file");
        assertEquals(0, HistoricalRegionBiomes.chunkPositions(notARegion).size());

        Path missing = directory.resolve("r.0.0.mca");
        assertEquals(0, HistoricalRegionBiomes.chunkPositions(missing).size());
    }
}

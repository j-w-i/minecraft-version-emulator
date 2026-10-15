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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The rule that a conversion runs once per boundary, not once per world load.
 *
 * <p>Its absence was destructive rather than merely wasteful: the first implementation decided by scanning the chunks,
 * so once a converted world had generated chunks of its own - which for every build from Alpha v1.2.0 onward hold a
 * biome distribution - the next load saw those natural biomes, called the world unconverted, and flattened them.
 *
 * <p>The record is also deliberately NOT part of the metadata document. Writing it there broke a real world: an upgrade
 * journal describes the metadata revision it expects, and a conversion landing between the staging and the generator
 * advance left the metadata newer than the journal, which recovery refuses. These tests hold the record out of the
 * metadata file.
 */
class BiomeConversionRecordTest {
    private static final String BETA_PLAINS = "moderner_beta:beta_plains";
    private static final String LATE_BETA_PLAINS = "moderner_beta:late_beta_plains";

    @Test
    @DisplayName("a world that has never been converted owes one, and only one, conversion")
    void firstConversionIsOwedOnce(@TempDir Path world) {
        assertNull(BiomeConversionRecord.lastConvertedTarget(world), "nothing is recorded yet");
        assertTrue(BiomeConversionRecord.conversionOwed(world, BETA_PLAINS), "the first conversion is owed");

        assertEquals(BETA_PLAINS, BiomeConversionRecord.record(world, BETA_PLAINS, 42L));
        assertFalse(BiomeConversionRecord.conversionOwed(world, BETA_PLAINS),
            "once recorded, the same target is never owed again - this is what stops a later load flattening the "
                + "biomes the world has generated since");
        assertEquals(BETA_PLAINS, BiomeConversionRecord.lastConvertedTarget(world));
    }

    @Test
    @DisplayName("each boundary converts once, and going back to an earlier target does not convert again")
    void eachTargetIsConvertedExactlyOnce(@TempDir Path world) {
        BiomeConversionRecord.record(world, BETA_PLAINS, 1L);
        assertTrue(BiomeConversionRecord.conversionOwed(world, LATE_BETA_PLAINS),
            "a boundary upgrade owes its own conversion");
        BiomeConversionRecord.record(world, LATE_BETA_PLAINS, 2L);
        assertFalse(BiomeConversionRecord.conversionOwed(world, LATE_BETA_PLAINS));
        assertFalse(BiomeConversionRecord.conversionOwed(world, BETA_PLAINS),
            "and the earlier target is not owed again: it has already been done once, and doing it again would "
                + "flatten everything generated since");
        assertEquals(List.of(BETA_PLAINS, LATE_BETA_PLAINS), BiomeConversionRecord.convertedTargets(world),
            "both conversions are remembered, in order");
    }

    @Test
    @DisplayName("recording writes its own file and leaves the metadata document byte-identical")
    void neverTouchesTheMetadataDocument(@TempDir Path world) throws Exception {
        // This is the check that would have caught the world refusal. The metadata document is owned by the
        // version-upgrade transaction alone; a second writer appending to it mid-transaction makes a pending journal
        // inconsistent and the world refuses to load.
        Path metadata = world.resolve("historical_experience_metadata.json");
        String original = "{\"schema\":1,\"profileId\":\"historical_experience:alpha_1_0_1\","
            + "\"generationPreset\":\"historical_experience:alpha_1_0_1_no_modern_content\","
            + "\"revision\":3,\"history\":[]}";
        Files.writeString(metadata, original);

        BiomeConversionRecord.record(world, BETA_PLAINS, 7L);

        assertEquals(original, Files.readString(metadata),
            "the metadata document must be byte-identical after a conversion record");
        assertTrue(Files.isRegularFile(BiomeConversionRecord.fileFor(world)),
            "the record lives in its own file beside it");
    }

    @Test
    @DisplayName("a missing or corrupt record means convert, never skip")
    void unreadableRecordConvertsRatherThanSkips(@TempDir Path world) throws Exception {
        // Missing metadata must not be read as "already converted": that would silently skip a world that needs it.
        assertNull(BiomeConversionRecord.lastConvertedTarget(world));
        assertTrue(BiomeConversionRecord.conversionOwed(world, BETA_PLAINS));

        // A corrupt record is treated the same way, and does not throw.
        Files.writeString(BiomeConversionRecord.fileFor(world), "{ this is not json");
        assertEquals(List.of(), BiomeConversionRecord.convertedTargets(world));
        assertTrue(BiomeConversionRecord.conversionOwed(world, BETA_PLAINS));

        // And no target means no conversion, which is how every build before Alpha v1.2.0 is excluded.
        assertFalse(BiomeConversionRecord.conversionOwed(world, null));
    }

    @Test
    @DisplayName("the chunks that existed beforehand survive a write and read, including negative coordinates")
    void preExistingChunksRoundTrip(@TempDir Path world) {
        java.util.Set<Long> chunks = new java.util.HashSet<>();
        chunks.add(net.minecraft.world.level.ChunkPos.asLong(0, 0));
        chunks.add(net.minecraft.world.level.ChunkPos.asLong(31, 31));
        chunks.add(net.minecraft.world.level.ChunkPos.asLong(-1, -1));
        chunks.add(net.minecraft.world.level.ChunkPos.asLong(-33, 40));
        BiomeConversionRecord.record(world, BETA_PLAINS, 1L, chunks);

        assertEquals(chunks, BiomeConversionRecord.convertedChunks(world),
            "exactly the chunks that were recorded come back, and no neighbour of them");
        assertFalse(BiomeConversionRecord.convertedChunks(world)
            .contains(net.minecraft.world.level.ChunkPos.asLong(1, 0)), "a chunk generated afterwards is not listed");
    }

    @Test
    @DisplayName("a conversion label names its single biome only where one exists")
    void labelNamesSingleBiome() {
        String prefix = BiomeCarryForward.recordLabel();
        assertEquals("moderner_beta:alpha", BiomeCarryForward.singleBiomeForLabel(prefix + ":alpha_only"));
        assertEquals(BETA_PLAINS, BiomeCarryForward.singleBiomeForLabel(prefix + ":beta_plains_only"));
        assertNull(BiomeCarryForward.singleBiomeForLabel(prefix + ":late_beta"));
        assertNull(BiomeCarryForward.singleBiomeForLabel(null));
    }

    @Test
    @DisplayName("recording the same target twice does not duplicate the entry")
    void recordingIsIdempotent(@TempDir Path world) {
        BiomeConversionRecord.record(world, BETA_PLAINS, 1L);
        BiomeConversionRecord.record(world, BETA_PLAINS, 2L);
        assertEquals(List.of(BETA_PLAINS), BiomeConversionRecord.convertedTargets(world));
    }
}

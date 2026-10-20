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

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataSchema;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FiniteEdgeRepairTest {
    @TempDir Path world;
    private static final String APRIL = FiniteTransitionIdentityTest.APRIL;
    private static final Set<Long> POSITIONS = Set.of(ChunkPos.asLong(0, 0), ChunkPos.asLong(8, 0), ChunkPos.asLong(9, 0));

    private String fixture(boolean backup) throws Exception {
        FiniteTransitionIdentityTest.fixture(world, 123, 100);
        var edited = FiniteApronReleaseTest.apron();
        edited.getList("sections", Tag.TAG_COMPOUND).getCompound(0).getCompound("block_states")
            .getList("palette", Tag.TAG_COMPOUND).getCompound(0).putString("Name", "minecraft:diamond_block");
        Files.createDirectories(world.resolve("region"));
        Files.write(world.resolve("region/r.0.0.mca"), FiniteApronReleaseTest.region(Map.of(
            0, FiniteApronReleaseTest.apron(), 8, FiniteApronReleaseTest.apron(), 9, edited)));
        String directory = backup ? WorldBackup.create(world,
            List.of(MetadataSchema.FILE_NAME, WorldGenSettingsAdvance.FILE_NAME), 1, 2, 200) : null;
        var store = HistoricalMetadataStore.forWorldDirectory(world);
        store.write(store.read().advanceVersion(HistoricalIdentifier.parse("historical_experience:infdev_420"),
            HistoricalIdentifier.parse(APRIL), "test", "version upgrade to historical_experience:infdev_420 on generation recipe " + APRIL, 200));
        WorldGenSettingsAdvance.advance(world, FiniteTransitionIdentityTest.INDEV, APRIL);
        BiomeConversionRecord.record(world, "infdev", 210, POSITIONS, 2);
        return directory;
    }
    private byte[] region() throws IOException { return Files.readAllBytes(world.resolve("region/r.0.0.mca")); }

    @Test void stagingIsIntentOnlyAndClosedRepairPreservesEditsAndUnrelatedRecords() throws Exception {
        fixture(true);
        var original = new FiniteRegionSnapshot(world.resolve("region/r.0.0.mca"));
        byte[] blocks = region(), biome = Files.readAllBytes(BiomeConversionRecord.fileFor(world));
        byte[] metadata = Files.readAllBytes(HistoricalMetadataStore.forWorldDirectory(world).file());
        byte[] level = Files.readAllBytes(world.resolve("level.dat"));
        String message = FiniteEdgeRepair.stage(world, "test", 300);
        assertTrue(message.contains("staged")); assertTrue(FiniteEdgeRepair.pending(world));
        assertArrayEquals(blocks, region()); assertArrayEquals(biome, Files.readAllBytes(BiomeConversionRecord.fileFor(world)));
        assertFalse(Files.exists(world.resolve(FiniteApronRelease.FILE_NAME)));
        assertTrue(FiniteEdgeRepair.recover(world)); assertFalse(FiniteEdgeRepair.pending(world));
        var plan = FiniteApronRelease.read(world);
        assertEquals(Set.of(ChunkPos.asLong(8, 0)), plan.releasedPositions());
        var after = new FiniteRegionSnapshot(world.resolve("region/r.0.0.mca"));
        assertEquals(original.chunks.stream().filter(chunk -> chunk.slot() != 8).toList(), after.chunks);
        assertEquals(Set.of(ChunkPos.asLong(0, 0), ChunkPos.asLong(9, 0)), BiomeConversionRecord.convertedChunks(world));
        assertEquals(2, BiomeConversionRecord.completedUpgradeRevision(world));
        assertEquals(210, BiomeConversionRecord.lastConvertedAt(world));
        assertArrayEquals(metadata, Files.readAllBytes(HistoricalMetadataStore.forWorldDirectory(world).file()));
        assertArrayEquals(level, Files.readAllBytes(world.resolve("level.dat")));
        Path before = world.resolve("historical_experience_finite_transition_backup/biome-conversion."
            + WorldBackup.sha256(biome) + ".before.json");
        assertArrayEquals(biome, Files.readAllBytes(before));
        byte[] completeRegion = region(), completeRecord = Files.readAllBytes(BiomeConversionRecord.fileFor(world));
        assertFalse(FiniteEdgeRepair.recover(world));
        assertTrue(FiniteEdgeRepair.stage(world, "test", 400).contains("already completed"));
        assertArrayEquals(completeRegion, region()); assertArrayEquals(completeRecord, Files.readAllBytes(BiomeConversionRecord.fileFor(world)));
    }

    @Test void missingOrCorruptSourceBackupNeverAuthorizesRepair() throws Exception {
        fixture(false); byte[] blocks = region();
        assertThrows(IOException.class, () -> FiniteEdgeRepair.stage(world, "test", 300));
        assertArrayEquals(blocks, region()); assertFalse(Files.exists(world.resolve(FiniteEdgeRepair.FILE_NAME)));
    }

    @Test void corruptSourceBackupFailsBeforeStaging() throws Exception {
        String directory = fixture(true); byte[] blocks = region();
        Files.writeString(world.resolve(directory).resolve("level.dat"), "corrupt");
        assertThrows(IOException.class, () -> FiniteEdgeRepair.stage(world, "test", 300));
        assertArrayEquals(blocks, region()); assertFalse(Files.exists(world.resolve(FiniteEdgeRepair.FILE_NAME)));
    }

    @Test void provenanceIsReverifiedAtExecutionRatherThanTrustingStageTime() throws Exception {
        String directory = fixture(true);
        FiniteEdgeRepair.stage(world, "test", 300); byte[] blocks = region();
        Files.writeString(world.resolve(directory).resolve("level.dat"), "corrupt after stage");
        assertThrows(IOException.class, () -> FiniteEdgeRepair.recover(world));
        assertArrayEquals(blocks, region()); assertTrue(FiniteEdgeRepair.pending(world));
        assertFalse(Files.exists(world.resolve(FiniteApronRelease.FILE_NAME)));
    }

    @Test void verifiedSameSeedForeignBackupIsStillNotThisWorldsProvenance() throws Exception {
        String directory = fixture(true); byte[] blocks = region();
        Path foreign = world.resolve("foreign-source");
        FiniteTransitionIdentityTest.fixture(foreign, 123, 101);
        String foreignBackup = WorldBackup.create(foreign,
            List.of(MetadataSchema.FILE_NAME, WorldGenSettingsAdvance.FILE_NAME), 1, 2, 200);
        try (var files = Files.list(foreign.resolve(foreignBackup))) {
            for (Path file : files.toList()) Files.copy(file, world.resolve(directory).resolve(file.getFileName()),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        WorldBackup.verify(world, directory); // Hash/size alone pass; origin/history binding must reject it.
        assertThrows(IOException.class, () -> FiniteEdgeRepair.stage(world, "test", 300));
        assertArrayEquals(blocks, region()); assertFalse(Files.exists(world.resolve(FiniteEdgeRepair.FILE_NAME)));
    }

    @Test void duplicateMatchingBackupsAreRefusedAsAmbiguous() throws Exception {
        String directory = fixture(true); byte[] blocks = region();
        Path copy = world.resolve(UpgradeJournal.BACKUP_DIRECTORY_NAME).resolve("201-r1-to-r2"); Files.createDirectories(copy);
        try (var files = Files.list(world.resolve(directory))) {
            for (Path file : files.toList()) Files.copy(file, copy.resolve(file.getFileName()));
        }
        assertThrows(IOException.class, () -> FiniteEdgeRepair.stage(world, "test", 300));
        assertArrayEquals(blocks, region());
    }

    @Test void interruptedBetweenRegionAndBiomeWritesResumesWithoutReapplyingRegionDeltas() throws Exception {
        fixture(true); FiniteEdgeRepair.stage(world, "test", 300);
        FiniteApronRelease.release(world, FiniteEdgeRepair.read(world).identity());
        byte[] changedRegion = region(), biome = Files.readAllBytes(BiomeConversionRecord.fileFor(world));
        Files.writeString(BiomeConversionRecord.fileFor(world), "{broken");
        assertThrows(IOException.class, () -> FiniteEdgeRepair.recover(world));
        assertTrue(FiniteEdgeRepair.pending(world)); assertArrayEquals(changedRegion, region());
        Files.write(BiomeConversionRecord.fileFor(world), biome);
        assertTrue(FiniteEdgeRepair.recover(world)); assertArrayEquals(changedRegion, region());
        assertFalse(BiomeConversionRecord.convertedChunks(world).contains(ChunkPos.asLong(8, 0)));
    }

    private void exteriorBiomes(String id, long lastUpdate) throws Exception {
        var original = new FiniteRegionSnapshot(world.resolve("region/r.0.0.mca"));
        var tags = new java.util.TreeMap<Integer, net.minecraft.nbt.CompoundTag>();
        for (var chunk : original.chunks) tags.put(chunk.slot(), chunk.tag().copy());
        FiniteChunkEvidenceTest.biomes(tags.get(8), id); tags.get(8).putLong("LastUpdate", lastUpdate);
        Files.write(world.resolve("region/r.0.0.mca"), FiniteApronReleaseTest.region(tags));
    }

    @Test void exactApronReplicaInDestinationNativeBiomesIsPreserved() throws Exception {
        fixture(true); exteriorBiomes("moderner_beta:infdev_420", 0);
        byte[] before = region(); FiniteEdgeRepair.stage(world, "test", 300); FiniteEdgeRepair.recover(world);
        assertEquals(0, FiniteApronRelease.read(world).releasedChunks()); assertArrayEquals(before, region());
    }

    @Test void convertedSourceBiomesAndOldSavedTimestampCanProveOriginalChunk() throws Exception {
        fixture(true); exteriorBiomes("moderner_beta:alpha", 99);
        FiniteEdgeRepair.stage(world, "test", 300); FiniteEdgeRepair.recover(world);
        assertEquals(Set.of(ChunkPos.asLong(8, 0)), FiniteApronRelease.read(world).releasedPositions());
    }

    @Test void convertedLookingReplicaSavedAfterVerifiedSourceTimeIsNotDeleted() throws Exception {
        fixture(true); exteriorBiomes("moderner_beta:alpha", 101);
        byte[] before = region(); FiniteEdgeRepair.stage(world, "test", 300); FiniteEdgeRepair.recover(world);
        assertEquals(0, FiniteApronRelease.read(world).releasedChunks()); assertArrayEquals(before, region());
    }

    @Test void oldPreparedPlanWithValidChecksumsStillNeedsSourceEvidenceBeforeAnyRegionWrite() throws Exception {
        fixture(true); exteriorBiomes("moderner_beta:infdev_420", 0); FiniteEdgeRepair.stage(world, "test", 300);
        var before = new FiniteRegionSnapshot(world.resolve("region/r.0.0.mca"));
        byte[] after = before.without(List.of(8));
        var change = new FiniteApronRelease.Change("r.0.0.mca", WorldBackup.sha256(before.bytes), WorldBackup.sha256(after), List.of(8));
        var plan = new FiniteApronRelease.Plan(3, false, java.util.stream.IntStream.range(0, 1024).map(i -> -1).toArray(),
            1, 1, List.of(change), Set.of(0L), FiniteEdgeRepair.read(world).identity(), false);
        Path backup = world.resolve("historical_experience_finite_transition_backup"); Files.createDirectories(backup);
        Files.write(backup.resolve("r.0.0.mca.before"), before.bytes); Files.write(backup.resolve("r.0.0.mca.after"), after);
        Files.writeString(world.resolve(FiniteApronRelease.FILE_NAME), new com.google.gson.Gson().toJson(plan));
        assertThrows(IOException.class, () -> FiniteEdgeRepair.recover(world));
        assertArrayEquals(before.bytes, region()); assertTrue(FiniteEdgeRepair.pending(world));
    }

    @Test void versionUpgradeCannotInterleaveWithPendingRepair() throws Exception {
        fixture(true); FiniteEdgeRepair.stage(world, "test", 300);
        var current = HistoricalMetadataStore.forWorldDirectory(world).read();
        var next = current.advanceVersion(HistoricalIdentifier.parse("historical_experience:infdev_611"),
            HistoricalIdentifier.parse("historical_experience:infdev_611_with_ores"), "test", "next", 400);
        assertThrows(UpgradeException.class, () -> GenerationUpgrade.stage(world, next, false, 400));
        assertThrows(UpgradeException.class, () -> GenerationUpgrade.perform(world, next, false, 400, GenerationUpgrade.Phase.COMPLETE));
        assertFalse(UpgradeJournal.existsIn(world));
    }

    @Test void infiniteProfileCannotDisguiseAStillFiniteStoredRecipe() throws Exception {
        fixture(true); byte[] blocks = region();
        Path metadata = HistoricalMetadataStore.forWorldDirectory(world).file();
        var json = com.google.gson.JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
        json.addProperty("generationPreset", FiniteTransitionIdentityTest.INDEV); Files.writeString(metadata, json.toString());
        assertEquals(FiniteTransitionIdentityTest.INDEV, HistoricalMetadataStore.forWorldDirectory(world).read().generationPreset().value());
        var root = FiniteTransitionIdentityTest.level(world);
        var gen = FiniteTransitionIdentityTest.generator(root);
        gen.getCompound("provider_settings").putString("moderner_beta:preset", FiniteTransitionIdentityTest.INDEV);
        gen.getCompound("biome_source").getCompound("provider_settings").putString("moderner_beta:preset", FiniteTransitionIdentityTest.INDEV);
        gen.getCompound("biome_source").getCompound("cave_provider_settings").putString("moderner_beta:preset", FiniteTransitionIdentityTest.INDEV);
        net.minecraft.nbt.NbtIo.writeCompressed(root, world.resolve("level.dat"));
        assertThrows(IOException.class, () -> FiniteEdgeRepair.stage(world, "test", 300));
        assertArrayEquals(blocks, region()); assertFalse(Files.exists(world.resolve(FiniteEdgeRepair.FILE_NAME)));
    }

    @Test void changedCurrentMetadataKeepsRequestPendingAndAllChunksUnchanged() throws Exception {
        fixture(true); FiniteEdgeRepair.stage(world, "test", 300); byte[] blocks = region();
        Path file = HistoricalMetadataStore.forWorldDirectory(world).file();
        Files.writeString(file, Files.readString(file) + "\n");
        assertThrows(IOException.class, () -> FiniteEdgeRepair.recover(world));
        assertArrayEquals(blocks, region()); assertTrue(FiniteEdgeRepair.pending(world));
    }

    @Test void malformedRequestAndTraversalNeverBecomeAnUpgradeBypass() throws Exception {
        fixture(true); byte[] blocks = region();
        Files.writeString(world.resolve(FiniteEdgeRepair.FILE_NAME), "{}");
        assertTrue(FiniteEdgeRepair.pending(world));
        assertThrows(IOException.class, () -> FiniteEdgeRepair.recover(world));
        assertArrayEquals(blocks, region());
        assertThrows(IllegalArgumentException.class, () -> new FiniteEdgeRepair.Request(1, false, "../foreign", "0".repeat(64),
            "0".repeat(64), FiniteTransitionIdentityTest.dummy(), "test", 1));
    }
}

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BiomeConversionTriggerTest {
    private HistoricalVersion version(String name) {
        return HistoricalVersions.byId("historical_experience:" + name).orElseThrow();
    }

    private HistoricalMetadata initial(Path world, HistoricalVersion version) throws Exception {
        var metadata = HistoricalMetadata.initial(HistoricalIdentifier.parse(version.id()),
            HistoricalIdentifier.parse(version.latestRevision().alias()), "test", 1);
        HistoricalMetadataStore.forWorldDirectory(world).write(metadata);
        return metadata;
    }

    @Test
    void openingAnyUnchangedVersionNeverRequestsAnUpgrade(@TempDir Path world) throws Exception {
        for (var version : HistoricalVersions.all()) {
            initial(world, version);
            assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, version), version.id());
            assertFalse(HistoricalBiomeConversionDriver.start(world, version), version.id());
        }
    }

    @Test
    void infdevPaletteAndOldRecordCannotTriggerAlphaConversion(@TempDir Path world) throws Exception {
        var infdev = HistoricalVersions.all().stream()
            .filter(v -> v.id().contains("infdev")).findFirst().orElseThrow();
        initial(world, infdev);
        Path region = Files.createDirectories(world.resolve("region")).resolve("r.0.0.mca");
        Files.writeString(region, "deliberately unreadable region must not even be scanned on reload");
        BiomeConversionRecord.record(world, "older_mapping_label", 2);
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, infdev));
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, version("alpha_1_0_1")));
        assertEquals("deliberately unreadable region must not even be scanned on reload", Files.readString(region));
    }

    @Test
    void explicitUpgradeIsCompletedOnDiskAndDoesNotRepeatAfterStateReset(@TempDir Path world) throws Exception {
        var alpha = version("alpha_1_0_1");
        var destination = version("alpha_1_2_0");
        var target = GenerationUpgrade.plan(initial(world, alpha), destination.id(), "test", 2);
        HistoricalMetadataStore.forWorldDirectory(world).write(target);
        assertTrue(HistoricalBiomeConversionDriver.needsConversion(world, destination));
        String before = Files.readString(HistoricalMetadataStore.forWorldDirectory(world).file());
        Set<Long> chunks = Set.of(net.minecraft.world.level.ChunkPos.asLong(-3, 7));
        assertNotNull(BiomeConversionRecord.record(world, BiomeCarryForward.recordLabelFor(destination), 3,
            chunks, target.revision()));

        HistoricalBiomeConversionDriver.resetReport();
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(Path.of(world.toString()), destination));
        assertEquals(target.revision(), BiomeConversionRecord.completedUpgradeRevision(world));
        assertEquals(chunks, BiomeConversionRecord.convertedChunks(world));
        assertEquals(before, Files.readString(HistoricalMetadataStore.forWorldDirectory(world).file()));
    }

    @Test
    void anotherExplicitUpgradeWithinSameBiomeGenerationIsNotMistakenForCompleted(@TempDir Path world) throws Exception {
        var start = initial(world, version("alpha_1_0_1"));
        var destination = version("alpha_1_2_0");
        var target = GenerationUpgrade.plan(start, destination.id(), "test", 2);
        HistoricalMetadataStore.forWorldDirectory(world).write(target);
        BiomeConversionRecord.record(world, BiomeCarryForward.recordLabelFor(destination), 3, Set.of(), target.revision());
        var next = version("beta_1_7_3");
        var nextTarget = GenerationUpgrade.plan(target, next.id(), "test", 4);
        HistoricalMetadataStore.forWorldDirectory(world).write(nextTarget);
        assertTrue(HistoricalBiomeConversionDriver.needsConversion(world, next));
        Set<Long> newChunks = Set.of(123L);
        BiomeConversionRecord.record(world, BiomeCarryForward.recordLabelFor(next), 5, newChunks, nextTarget.revision());
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, next));
        assertEquals(newChunks, BiomeConversionRecord.convertedChunks(world));
        assertEquals(nextTarget.revision(), BiomeConversionRecord.completedUpgradeRevision(world));
    }

    @Test
    void recipeOnlyAdvanceNeverRequestsBiomeConversion(@TempDir Path world) throws Exception {
        var destination = version("alpha_1_2_0");
        var target = initial(world, destination).advance(null, "test", "generation revision upgrade to recipe", 2);
        HistoricalMetadataStore.forWorldDirectory(world).write(target);
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, destination));
    }

    private void awaitConversion() throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (HistoricalBiomeConversionDriver.running() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertFalse(HistoricalBiomeConversionDriver.running(), "conversion worker did not terminate");
        assertTrue(HistoricalBiomeConversionDriver.finished());
    }

    @Test
    void workerPersistsEvenAnEmptyWorldUpgrade(@TempDir Path world) throws Exception {
        var destination = version("alpha_1_2_0");
        var target = GenerationUpgrade.plan(initial(world, version("alpha_1_0_1")), destination.id(), "test", 2);
        HistoricalMetadataStore.forWorldDirectory(world).write(target);
        assertTrue(HistoricalBiomeConversionDriver.start(world, destination));
        awaitConversion();
        assertFalse(HistoricalBiomeConversionDriver.failed(), HistoricalBiomeConversionDriver.detail());
        assertEquals(target.revision(), BiomeConversionRecord.completedUpgradeRevision(world));
        HistoricalBiomeConversionDriver.resetReport();
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, destination));
    }

    @Test
    void recordWriteFailureIsNotReportedAsCompletion(@TempDir Path world) throws Exception {
        var destination = version("alpha_1_2_0");
        var target = GenerationUpgrade.plan(initial(world, version("alpha_1_0_1")), destination.id(), "test", 2);
        HistoricalMetadataStore.forWorldDirectory(world).write(target);
        Files.createDirectory(world.resolve(BiomeConversionRecord.FILE_NAME + ".tmp"));
        assertTrue(HistoricalBiomeConversionDriver.start(world, destination));
        awaitConversion();
        assertTrue(HistoricalBiomeConversionDriver.failed());
        assertEquals(0, BiomeConversionRecord.completedUpgradeRevision(world));
        assertTrue(HistoricalBiomeConversionDriver.needsConversion(world, destination));
    }

    @Test
    void legacyCompletionIsHonouredWithoutRewritingIt(@TempDir Path world) throws Exception {
        var destination = version("alpha_1_2_0");
        var target = GenerationUpgrade.plan(initial(world, version("alpha_1_0_1")), destination.id(), "test", 2);
        HistoricalMetadataStore.forWorldDirectory(world).write(target);
        BiomeConversionRecord.record(world, BiomeCarryForward.recordLabelFor(destination), 3);
        String before = Files.readString(BiomeConversionRecord.fileFor(world));
        assertFalse(HistoricalBiomeConversionDriver.needsConversion(world, destination));
        assertEquals(before, Files.readString(BiomeConversionRecord.fileFor(world)));
    }
}

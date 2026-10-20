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
package net.historicalexperience.server;

import java.nio.file.Files;
import java.nio.file.Path;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.metadata.*;
import net.historicalexperience.world.upgrade.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotUpgradeTest {
    private static final String A = "historical_experience:snapshot_11w49a", B = "historical_experience:snapshot_12w49a";
    private static HistoricalMetadata fixture(Path world, String profile) throws Exception {
        Files.createDirectories(world);
        var version = HistoricalVersions.byId(profile).orElseThrow();
        String alias = version.revisions().get(0).alias();
        var metadata = HistoricalMetadata.initial(HistoricalIdentifier.parse(profile), HistoricalIdentifier.parse(alias), "fixture", 1);
        HistoricalMetadataStore.forWorldDirectory(world).write(metadata);
        var root = new CompoundTag(); var data = new CompoundTag(); var settings = new CompoundTag();
        var dimensions = new CompoundTag(); var overworld = new CompoundTag(); var generator = new CompoundTag();
        var reference = new CompoundTag(); reference.putString("moderner_beta:preset", alias);
        generator.putString("type", "moderner_beta:moderner_beta"); generator.put("provider_settings", reference.copy());
        var biome = new CompoundTag(); biome.putString("type", "moderner_beta:moderner_beta");
        biome.put("provider_settings", reference.copy()); biome.put("cave_provider_settings", reference.copy());
        generator.put("biome_source", biome); overworld.put("generator", generator);
        dimensions.put("minecraft:overworld", overworld); settings.put("dimensions", dimensions);
        data.put("WorldGenSettings", settings); data.putString("unrelatedPlayerSentinel", "name/damage/custom-data-preserved");
        root.put("Data", data); NbtIo.writeCompressed(root, world.resolve("level.dat"));
        Files.write(world.resolve("inventory-preservation-sentinel.bin"), new byte[]{2,7,11,29});
        return metadata;
    }
    @Test void sameParentPatchAndClearRetainExactRecipeAndLevelBytes(@TempDir Path world) throws Exception {
        var old = fixture(world, "historical_experience:release_1_1");
        byte[] level = Files.readAllBytes(world.resolve("level.dat"));
        assertTrue(HistoricalUpgradeService.upgradeUnloadedWorld(world, A, "test", 2).ok());
        var selected = HistoricalMetadataStore.forWorldDirectory(world).read();
        assertEquals(A, selected.selectionId()); assertEquals(old.profileId(), selected.profileId());
        assertEquals(old.generationPreset(), selected.generationPreset()); assertEquals(2, selected.revision());
        assertArrayEquals(level, Files.readAllBytes(world.resolve("level.dat")));
        var state = HistoricalUpgradeService.inspect(world);
        assertEquals(old.profileId().value(), state.profileId()); assertEquals(A, state.selectionId());
        assertTrue(state.displayName().contains("11w49a")); assertTrue(state.pendingUpgrade());
        var journal = UpgradeJournal.readFrom(world); WorldBackup.verify(world, journal.backupDirectory());
        assertEquals(selected, journal.targetMetadata());
        assertFalse(HistoricalUpgradeService.upgradeUnloadedWorld(world, A, "test", 3).ok());
        GenerationUpgrade.applyPendingAdvance(world).orElseThrow();
        assertFalse(UpgradeJournal.existsIn(world)); assertArrayEquals(level, Files.readAllBytes(world.resolve("level.dat")));
        assertTrue(HistoricalUpgradeService.upgradeUnloadedWorld(world, "release_1_1", "test", 4).ok());
        var cleared = HistoricalMetadataStore.forWorldDirectory(world).read();
        assertEquals("", cleared.snapshotPatchId()); assertEquals(old.generationPreset(), cleared.generationPreset());
        GenerationUpgrade.applyPendingAdvance(world).orElseThrow();
        assertArrayEquals(level, Files.readAllBytes(world.resolve("level.dat")));
        assertArrayEquals(new byte[]{2,7,11,29}, Files.readAllBytes(world.resolve("inventory-preservation-sentinel.bin")));
    }
    @Test void crossParentSelectionUsesNormalTransactionAndOneNewRevision(@TempDir Path world) throws Exception {
        var old = fixture(world, "historical_experience:release_1_1");
        assertTrue(HistoricalUpgradeService.upgradeUnloadedWorld(world, B, "test", 2).ok());
        var target = HistoricalMetadataStore.forWorldDirectory(world).read();
        assertEquals(B, target.selectionId()); assertEquals("historical_experience:release_1_4", target.profileId().value());
        assertEquals(HistoricalVersions.byId(target.profileId().value()).orElseThrow().latestRevision().alias(), target.generationPreset().value());
        assertEquals(2, target.revision()); assertEquals(old.history(), target.history().subList(0, old.history().size()));
        assertTrue(target.history().get(1).note().contains("12w49a"));
        assertEquals(old.generationPreset().value(), WorldGenSettingsAdvance.read(world).orElseThrow().settingsPreset());
        GenerationUpgrade.applyPendingAdvance(world).orElseThrow();
        assertEquals(target.generationPreset().value(), WorldGenSettingsAdvance.read(world).orElseThrow().settingsPreset());
        assertEquals("name/damage/custom-data-preserved", NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap()).getCompound("Data").getString("unrelatedPlayerSentinel"));
        assertArrayEquals(new byte[]{2,7,11,29}, Files.readAllBytes(world.resolve("inventory-preservation-sentinel.bin")));
    }
    @Test void snapshotIdentitySurvivesJournalBeforeMetadataRecovery(@TempDir Path world) throws Exception {
        var old = fixture(world, "historical_experience:release_1_1");
        var target = GenerationUpgrade.plan(old, A, "test", 2);
        GenerationUpgrade.perform(world, target, false, 2, GenerationUpgrade.Phase.JOURNAL_WRITTEN);
        assertEquals(old, HistoricalMetadataStore.forWorldDirectory(world).read());
        assertEquals(A, UpgradeJournal.readFrom(world).targetMetadata().snapshotPatchId());
        GenerationUpgrade.recover(world).orElseThrow();
        assertEquals(target, HistoricalMetadataStore.forWorldDirectory(world).read());
        GenerationUpgrade.applyPendingAdvance(world);
        assertFalse(UpgradeJournal.existsIn(world));
    }
    @Test void ordinaryUnknownNoopAndOpenWorldRefusalsDoNotRetagContent(@TempDir Path root) throws Exception {
        var ordinary = root.resolve("ordinary"); Files.createDirectories(ordinary);
        assertFalse(HistoricalUpgradeService.upgradeUnloadedWorld(ordinary, A, "test", 2).ok());
        assertFalse(HistoricalMetadataStore.forWorldDirectory(ordinary).hasEntry());
        var world = root.resolve("historical"); var old = fixture(world, "historical_experience:release_1_1");
        assertFalse(HistoricalUpgradeService.upgrade(world, "snapshot_unknown", "test", 2).ok());
        assertEquals(old, HistoricalMetadataStore.forWorldDirectory(world).read());
        var lock = world.resolve("session.lock"); Files.write(lock, new byte[]{1});
        try (var channel = java.nio.channels.FileChannel.open(lock, java.nio.file.StandardOpenOption.WRITE); var held = channel.lock()) {
            assertFalse(HistoricalUpgradeService.upgradeUnloadedWorld(world, A, "test", 3).ok());
            assertEquals(old, HistoricalMetadataStore.forWorldDirectory(world).read()); assertFalse(UpgradeJournal.existsIn(world));
        }
        assertTrue(HistoricalUpgradeService.upgradeUnloadedWorld(world, A, "test", 4).ok());
        GenerationUpgrade.applyPendingAdvance(world);
        var snapshot = HistoricalMetadataStore.forWorldDirectory(world).read();
        assertFalse(HistoricalUpgradeService.upgradeUnloadedWorld(world, A, "test", 5).ok());
        assertEquals(snapshot, HistoricalMetadataStore.forWorldDirectory(world).read());
    }
}

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

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.upgrade.GenerationUpgrade;
import net.historicalexperience.world.upgrade.UpgradeJournal;
import net.historicalexperience.world.upgrade.WorldGenSettingsAdvance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoricalUpgradeService1211Test {
    private static final String OLD = "historical_experience:beta_1_7_3_content_preview_4";
    private static final String NEW = "historical_experience:beta_1_7_3_content_preview_10";

    private static HistoricalMetadata fixture(Path world) throws Exception {
        Files.createDirectories(world);
        HistoricalMetadata before = HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"), HistoricalIdentifier.parse(OLD),
            "fixture", 1);
        HistoricalMetadataStore.forWorldDirectory(world).write(before);
        CompoundTag root = new CompoundTag(), data = new CompoundTag(), settings = new CompoundTag();
        CompoundTag dimensions = new CompoundTag(), overworld = new CompoundTag(), generator = new CompoundTag();
        CompoundTag reference = new CompoundTag();
        reference.putString("moderner_beta:preset", OLD);
        generator.putString("type", "moderner_beta:moderner_beta");
        generator.put("provider_settings", reference.copy());
        CompoundTag biome = new CompoundTag();
        biome.putString("type", "moderner_beta:moderner_beta");
        biome.put("provider_settings", reference.copy());
        biome.put("cave_provider_settings", reference.copy());
        generator.put("biome_source", biome);
        overworld.put("generator", generator);
        dimensions.put("minecraft:overworld", overworld);
        settings.put("dimensions", dimensions);
        data.put("WorldGenSettings", settings);
        data.putString("unrelatedPlayerSentinel", "preserved");
        root.put("Data", data);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        return before;
    }

    @Test void acceptsBareNamespacedAndQuotedNames(@TempDir Path temp) throws Exception {
        assertEquals("historical_experience:beta_1_7_3", HistoricalUpgradeService.qualify(" beta_1_7_3 "));
        assertEquals("historical_experience:beta_1_7_3", HistoricalUpgradeService.qualify("historical_experience:beta_1_7_3"));
        assertEquals("historical_experience:beta_1_7_3", HistoricalUpgradeService.qualify("\"historical_experience:beta_1_7_3\""));
        fixture(temp);
        assertTrue(HistoricalUpgradeService.upgrade(temp, "historical_experience:beta_1_7_3", "test", 2).ok());
        assertEquals(OLD, WorldGenSettingsAdvance.read(temp).orElseThrow().settingsPreset());
        assertTrue(UpgradeJournal.existsIn(temp));
        assertEquals(NEW, HistoricalMetadataStore.forWorldDirectory(temp).read().generationPreset().value());
        GenerationUpgrade.applyPendingAdvance(temp);
        assertEquals(NEW, WorldGenSettingsAdvance.read(temp).orElseThrow().settingsPreset());
        assertFalse(UpgradeJournal.existsIn(temp));
    }

    @Test void refusesStoredMarkerMismatchBeforePublishingAnything(@TempDir Path world) throws Exception {
        fixture(world);
        WorldGenSettingsAdvance.advance(world, OLD, "historical_experience:beta_1_7_3");
        byte[] saved = Files.readAllBytes(world.resolve("level.dat"));
        var before = HistoricalMetadataStore.forWorldDirectory(world).read();
        assertFalse(HistoricalUpgradeService.upgrade(world, "beta_1_7_3", "test", 2).ok());
        assertEquals(before, HistoricalMetadataStore.forWorldDirectory(world).read());
        assertArrayEquals(saved, Files.readAllBytes(world.resolve("level.dat")));
        assertFalse(UpgradeJournal.existsIn(world));
        assertFalse(Files.exists(world.resolve("historical_experience_backups")));
    }

    @Test void refusesOrdinaryUnknownAndNoopWithoutMutations(@TempDir Path temp) throws Exception {
        Path ordinary = temp.resolve("ordinary");
        Files.createDirectories(ordinary);
        assertFalse(HistoricalUpgradeService.upgrade(ordinary, "beta_1_7_3", "test", 2).ok());
        assertFalse(HistoricalMetadataStore.forWorldDirectory(ordinary).hasEntry());
        Path world = temp.resolve("historical");
        fixture(world);
        byte[] before = Files.readAllBytes(world.resolve("level.dat"));
        assertFalse(HistoricalUpgradeService.upgrade(world, "unknown_version", "test", 2).ok());
        assertArrayEquals(before, Files.readAllBytes(world.resolve("level.dat")));
        assertFalse(UpgradeJournal.existsIn(world));
        assertTrue(HistoricalUpgradeService.upgrade(world, "beta_1_7_3", "test", 3).ok());
        assertFalse(HistoricalUpgradeService.upgrade(world, "beta_1_7_3", "test", 4).ok());
        GenerationUpgrade.applyPendingAdvance(world);
        assertFalse(HistoricalUpgradeService.upgrade(world, "beta_1_7_3", "test", 5).ok());
    }
}

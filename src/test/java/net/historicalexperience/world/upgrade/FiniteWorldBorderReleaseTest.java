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
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * An Indev world taken to an infinite era must stop being 256 blocks wide.
 *
 * <p>Owner: <i>"remember the biome upgrader also itll upgrade from a finite to infinite world so account for
 * that"</i>. The border is not part of the generator block this project rewrites: Moderner Beta's {@code world_border}
 * component sets the level's border at creation and the game stores it in {@code level.dat}, so advancing the preset
 * alone would leave a world that generates infinitely while the player is still fenced inside a 256-block square.
 *
 * <p>Every case here writes a real {@code level.dat} through the game's own NBT writer, so the test exercises the
 * same file shape an upgrade meets rather than a stub.
 */
class FiniteWorldBorderReleaseTest {
    private static final String INDEV_PRESET = "historical_experience:indev_latest";
    private static final String INFDEV_PRESET = "historical_experience:infdev_420";
    private static final double INDEC_BORDER = 256.0D;

    /** A world whose generator names {@code preset} and whose border is {@code borderSize}. */
    private static void fixture(Path world, String preset, double borderSize) throws Exception {
        Files.createDirectories(world);
        CompoundTag reference = new CompoundTag();
        reference.putString("moderner_beta:preset", preset);
        CompoundTag biome = new CompoundTag();
        biome.putString("type", "moderner_beta:moderner_beta");
        biome.put("provider_settings", reference.copy());
        biome.put("cave_provider_settings", reference.copy());
        CompoundTag generator = new CompoundTag();
        generator.putString("type", "moderner_beta:moderner_beta");
        generator.put("provider_settings", reference.copy());
        generator.put("biome_source", biome);
        CompoundTag overworld = new CompoundTag();
        overworld.put("generator", generator);
        CompoundTag dimensions = new CompoundTag();
        dimensions.put("minecraft:overworld", overworld);
        CompoundTag settings = new CompoundTag();
        settings.put("dimensions", dimensions);

        // 1.21.1 flattens the border into Data: BorderSize, BorderSizeLerpTarget and the warning/centre keys sit
        // beside the generator rather than inside a WorldBorder compound. The first version of this fixture wrote a
        // compound, which the release code then could not find - so the fixture now mirrors the real file.
        CompoundTag data = new CompoundTag();
        data.put("WorldGenSettings", settings);
        data.putDouble("BorderSize", borderSize);
        data.putDouble("BorderSizeLerpTarget", borderSize);
        data.putDouble("BorderCenterX", 0.0D);
        data.putDouble("BorderCenterZ", 0.0D);
        data.putInt("BorderWarningTime", 15);
        data.putString("unrelatedPlayerSentinel", "preserved");
        CompoundTag root = new CompoundTag();
        root.put("Data", data);
        NbtIo.writeCompressed(root, world.resolve(WorldGenSettingsAdvance.FILE_NAME));
    }

    @Test
    @DisplayName("a finite-to-infinite upgrade releases the 256-block border, in the same write as the preset")
    void releasingTheBorder(@TempDir Path temp) throws Exception {
        Path world = temp.resolve("indev");
        fixture(world, INDEV_PRESET, INDEC_BORDER);
        assertEquals(INDEC_BORDER, WorldGenSettingsAdvance.readWorldBorderSize(world).orElseThrow(),
            "the fixture must start finite, or this test proves nothing");

        assertTrue(HistoricalVersions.isFinitePreset(INDEV_PRESET), "Indev is the finite era");
        assertFalse(HistoricalVersions.isFinitePreset(INFDEV_PRESET), "Infdev is not finite");

        WorldGenSettingsAdvance.advance(world, INDEV_PRESET, INFDEV_PRESET, true);

        assertEquals(WorldGenSettingsAdvance.DEFAULT_WORLD_BORDER_SIZE,
            WorldGenSettingsAdvance.readWorldBorderSize(world).orElseThrow(),
            "an upgraded world must use the ordinary border, not the finite level's");
        assertEquals(INFDEV_PRESET, WorldGenSettingsAdvance.read(world).orElseThrow().settingsPreset(),
            "and the generator must have advanced in the same write");

        // The rest of the border is the player's, and the rest of the file is nobody's to touch.
        CompoundTag root = NbtIo.readCompressed(world.resolve(WorldGenSettingsAdvance.FILE_NAME),
            net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        CompoundTag data = root.getCompound("Data");
        assertEquals(0.0D, data.getDouble("BorderCenterX"));
        assertEquals(15, data.getInt("BorderWarningTime"),
            "warning time is a player setting, not this project's to overwrite");
        assertEquals("preserved", data.getString("unrelatedPlayerSentinel"));
    }

    @Test
    @DisplayName("without the flag the border is left exactly as stored")
    void notReleasingTheBorder(@TempDir Path temp) throws Exception {
        Path world = temp.resolve("indev");
        fixture(world, INDEV_PRESET, INDEC_BORDER);

        WorldGenSettingsAdvance.advance(world, INDEV_PRESET, INFDEV_PRESET, false);

        assertEquals(INDEC_BORDER, WorldGenSettingsAdvance.readWorldBorderSize(world).orElseThrow(),
            "the release is opt-in, so every other upgrade leaves the border alone");
    }

    @Test
    @DisplayName("a level that stores no border is left alone rather than given one")
    void noStoredBorderStaysAbsent(@TempDir Path temp) throws Exception {
        Path world = temp.resolve("infinite");
        fixture(world, INFDEV_PRESET, 0.0D);
        // Remove the border the fixture wrote, which is how a world that never had one looks.
        CompoundTag root = NbtIo.readCompressed(world.resolve(WorldGenSettingsAdvance.FILE_NAME),
            net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        root.getCompound("Data").remove("BorderSize");
        root.getCompound("Data").remove("BorderSizeLerpTarget");
        NbtIo.writeCompressed(root, world.resolve(WorldGenSettingsAdvance.FILE_NAME));

        assertTrue(WorldGenSettingsAdvance.readWorldBorderSize(world).isEmpty(),
            "an absent border reads as absent, not as a default");
        WorldGenSettingsAdvance.advance(world, INFDEV_PRESET, "historical_experience:infdev_611", true);
        assertTrue(WorldGenSettingsAdvance.readWorldBorderSize(world).isEmpty(),
            "writing a border into a level that had none would be inventing state");
    }
}

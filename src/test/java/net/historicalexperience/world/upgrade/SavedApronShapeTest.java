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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Developer measurement, skipped unless armed: what do SAVED finite worlds actually contain outside their island?
 *
 * <p>The apron release recognises the synthetic shell by its exact blocks, and its own constants are the default
 * Indev map's (bedrock 0-53, dirt 54, water 55-63, air above). Whether an Inland map, a Hell map or a smaller map has
 * the same shell cannot be read out of this project's code - the generator decides it - so it is measured from the
 * saved region files a real run left behind. Reading the region files rather than a live chunk is the point: the
 * shell is what the provider PRE-FILLS while generating, and a chunk this project requests later on demand comes back
 * empty regardless.
 *
 * <p>Armed with {@code -PapronFixture=<saves directory>} (and optionally {@code -PapronFixtureMax=N}). It asserts
 * nothing about the values it prints - it is a measurement, and its output is recorded in the handoff notes.
 */
class SavedApronShapeTest {
    @Test
    void printSavedFiniteWorldShapesWhenArmed() throws Exception {
        String armed = System.getProperty("historical_experience.apronFixture");
        Assumptions.assumeTrue(armed != null && !armed.isBlank(),
            "measurement only; arm with -PapronFixture=<saves directory>");
        int max = Integer.getInteger("historical_experience.apronFixture.max", 6);
        Path saves = Path.of(armed);
        Assumptions.assumeTrue(Files.isDirectory(saves), "no saves directory at " + saves);

        for (Path world : newest(saves, max)) {
            System.out.println("[apron] ==== " + world.getFileName());
            String stored = providerSettings(world);
            System.out.println("[apron] stored generator provider_settings: " + stored);
            if (!stored.contains("moderner_beta")) {
                System.out.println("[apron] not a Moderner Beta world; skipped");
                continue;
            }
            Path regionDir = world.resolve("region");
            if (!Files.isDirectory(regionDir)) {
                System.out.println("[apron] no overworld region directory");
                continue;
            }
            List<Path> regions = new ArrayList<>();
            try (var stream = Files.list(regionDir)) {
                stream.filter(path -> path.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca"))
                    .sorted().forEach(regions::add);
            }
            int inside = 0, outside = 0, outsideShell = 0, outsideEmpty = 0, unreadable = 0;
            String sampleInside = null, sampleOutside = null;
            for (Path region : regions) {
                for (var chunk : new FiniteRegionSnapshot(region).chunks) {
                    boolean in = FiniteApronRelease.inside(chunk.x(), chunk.z());
                    boolean shell = false, empty = false;
                    try {
                        shell = FiniteRegionSnapshot.pristineApron(chunk.tag());
                        empty = isEmpty(chunk.tag());
                    } catch (Exception notFinite) {
                        // A vanilla-shaped chunk is not a finite shell; counted, not fatal, so one ordinary world in
                        // the same saves directory cannot abort the measurement of the others.
                        unreadable++;
                    }
                    if (in) {
                        inside++;
                        if (sampleInside == null) {
                            try {
                                sampleInside = signature(region.getFileName() + " " + chunk.x() + "," + chunk.z(), chunk.tag());
                            } catch (Exception notFinite) {
                                sampleInside = "<not finite>";
                            }
                        }
                    } else {
                        outside++;
                        if (shell) outsideShell++;
                        if (empty) outsideEmpty++;
                        if (sampleOutside == null) {
                            try {
                                sampleOutside = signature(region.getFileName() + " " + chunk.x() + "," + chunk.z(), chunk.tag());
                            } catch (Exception notFinite) {
                                sampleOutside = "<not finite>";
                            }
                        }
                    }
                }
            }
            System.out.println("[apron] chunks inside[-8,8)=" + inside + " outside=" + outside
                + " of which known-default-shell=" + outsideShell + " all-air=" + outsideEmpty
                + " unreadable(non-finite)=" + unreadable);
            System.out.println("[apron] sample OUTSIDE column: " + sampleOutside);
            System.out.println("[apron] sample inside  column: " + sampleInside);
        }
    }

    private static List<Path> newest(Path saves, int max) throws Exception {
        List<Path> worlds = new ArrayList<>();
        try (var stream = Files.list(saves)) {
            stream.filter(Files::isDirectory).forEach(worlds::add);
        }
        worlds.sort(Comparator.comparingLong(SavedApronShapeTest::modified).reversed());
        return worlds.subList(0, Math.min(max, worlds.size()));
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception unreadable) {
            return 0L;
        }
    }

    /** The raw stored provider settings, so a world's own finite geometry is visible next to its shell. */
    private static String providerSettings(Path world) {
        try {
            CompoundTag root = NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap());
            CompoundTag dimensions = root.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions");
            CompoundTag generator = dimensions.getCompound("minecraft:overworld").getCompound("generator");
            return generator.getCompound("provider_settings").toString();
        } catch (Exception unreadable) {
            return "<unreadable: " + unreadable + ">";
        }
    }

    /** Whether the chunk holds nothing but air, which is the other thing an exterior chunk can legitimately be. */
    private static boolean isEmpty(CompoundTag chunk) throws Exception {
        String[] blocks = FiniteRegionSnapshot.blocks(chunk);
        for (String block : blocks) {
            if (!block.equals("minecraft:air")) return false;
        }
        return true;
    }

    /** One column (the chunk's centre) as contiguous y-runs, which is what a shell looks like at a glance. */
    private static String signature(String label, CompoundTag chunk) throws Exception {
        String[] blocks = FiniteRegionSnapshot.blocks(chunk);
        StringBuilder runs = new StringBuilder(label).append(" = ");
        String previous = null;
        int start = 0;
        for (int y = 0; y < 128; y++) {
            String name = blocks[y * 256 + 8 * 16 + 8];
            if (!name.equals(previous)) {
                if (previous != null) runs.append(start).append('-').append(y - 1).append(' ').append(previous).append(" | ");
                previous = name;
                start = y;
            }
        }
        return runs.append(start).append("-127 ").append(previous).toString();
    }
}

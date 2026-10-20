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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FiniteApronReleaseTest {
    @TempDir Path world;

    static CompoundTag apron() {
        var chunk = new CompoundTag();
        chunk.putString("Status", "minecraft:full");
        var sections = new ListTag();
        for (int sy = 0; sy < 8; sy++) {
            var section = new CompoundTag();
            section.putByte("Y", (byte) sy);
            var state = new CompoundTag();
            var palette = new ListTag();
            for (String name : List.of("bedrock", "dirt", "water", "air")) {
                var entry = new CompoundTag();
                entry.putString("Name", "minecraft:" + name);
                if (name.equals("water")) {
                    var properties = new CompoundTag(); properties.putString("level", "0"); entry.put("Properties", properties);
                }
                palette.add(entry);
            }
            state.put("palette", palette);
            long[] packed = new long[256];
            for (int i = 0; i < 4096; i++) {
                int y = sy * 16 + (i >> 8);
                int value = y < 54 ? 0 : y == 54 ? 1 : y < 64 ? 2 : 3;
                packed[i / 16] |= (long) value << (i % 16 * 4);
            }
            state.putLongArray("data", packed);
            section.put("block_states", state);
            var biomes = new CompoundTag();
            var biomePalette = new ListTag(); biomePalette.add(net.minecraft.nbt.StringTag.valueOf("moderner_beta:indev_normal"));
            biomes.put("palette", biomePalette); section.put("biomes", biomes);
            sections.add(section);
        }
        chunk.put("sections", sections);
        return chunk;
    }

    static byte[] region(Map<Integer, CompoundTag> chunks) throws Exception {
        var blobs = new LinkedHashMap<Integer, byte[]>();
        int sectors = 2;
        for (var entry : chunks.entrySet()) {
            var bytes = new ByteArrayOutputStream();
            var tag = entry.getValue().copy();
            tag.putInt("xPos", entry.getKey() % 32); tag.putInt("zPos", entry.getKey() / 32);
            try (var out = new DataOutputStream(new DeflaterOutputStream(bytes))) { NbtIo.write(tag, out); }
            byte[] blob = bytes.toByteArray(); blobs.put(entry.getKey(), blob);
            sectors += (blob.length + 5 + 4095) / 4096;
        }
        byte[] result = new byte[sectors * 4096];
        int sector = 2;
        for (var entry : blobs.entrySet()) {
            int size = (entry.getValue().length + 5 + 4095) / 4096;
            ByteBuffer.wrap(result).putInt(entry.getKey() * 4, sector << 8 | size)
                .putInt(4096 + entry.getKey() * 4, 1234).putInt(sector * 4096, entry.getValue().length + 1);
            result[sector * 4096 + 4] = 2;
            System.arraycopy(entry.getValue(), 0, result, sector * 4096 + 5, entry.getValue().length);
            sector += size;
        }
        return result;
    }

    private Path fixture() throws Exception {
        FiniteTransitionIdentityTest.fixture(world, 123, 100);
        Path path = world.resolve("region/r.0.0.mca"); Files.createDirectories(path.getParent());
        CompoundTag edited = apron();
        var container = new CompoundTag(); container.putString("id", "minecraft:chest");
        var containers = new ListTag(); containers.add(container); edited.put("block_entities", containers);
        Files.write(path, region(Map.of(7, apron(), 8, apron(), 9, edited, 10, apron())));
        Path entities = world.resolve("entities/r.0.0.mca"); Files.createDirectories(entities.getParent());
        var entityChunk = new CompoundTag(); var list = new ListTag();
        var entity = new CompoundTag(); entity.putString("id", "minecraft:item"); list.add(entity);
        entityChunk.put("Entities", list); Files.write(entities, region(Map.of(10, entityChunk)));
        return path;
    }

    @Test void onlyProvenPristineExteriorIsReleasedAndBackedUp() throws Exception {
        Path path = fixture(); byte[] before = Files.readAllBytes(path);
        byte[] entityBefore = Files.readAllBytes(world.resolve("entities/r.0.0.mca"));
        var plan = FiniteApronRelease.release(world);
        assertTrue(plan.complete()); assertEquals(1, plan.releasedChunks()); assertEquals(2, plan.preservedExteriorChunks());
        assertEquals(0, plan.knownEdgeColumns(),
            "an upgrade is a HARD CUT: it samples no island edge and therefore blends nothing, by the owner's "
                + "decision - the collar was pulling the coast into long ridges");
        assertTrue(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(7, 0)));
        assertTrue(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(9, 0)));
        assertTrue(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(10, 0)));
        assertFalse(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(8, 0)));
        var snapshot = new FiniteRegionSnapshot(path);
        assertEquals(List.of(7, 9, 10), snapshot.chunks.stream().map(FiniteRegionSnapshot.Chunk::slot).toList());
        assertArrayEquals(entityBefore, Files.readAllBytes(world.resolve("entities/r.0.0.mca")));
        assertArrayEquals(before, Files.readAllBytes(world.resolve(
            "historical_experience_finite_transition_backup/r.0.0.mca.before")));
        byte[] after = Files.readAllBytes(path);
        // Only location and timestamp for slot 8 may change. Every retained compressed blob is identical.
        Arrays.fill(before, 8 * 4, 8 * 4 + 4, (byte) 0);
        Arrays.fill(before, 4096 + 8 * 4, 4096 + 8 * 4 + 4, (byte) 0);
        assertArrayEquals(before, after);
        FiniteApronRelease.release(world);
        assertArrayEquals(after, Files.readAllBytes(path), "retry must not rewrite an already-completed region");
    }

    /** A chunk whose every block is air: the shape a custom-settings map has outside its island. */
    static CompoundTag emptyChunk() {
        var chunk = new CompoundTag();
        chunk.putString("Status", "minecraft:full");
        var sections = new ListTag();
        for (int sy = 0; sy < 8; sy++) {
            var section = new CompoundTag();
            section.putByte("Y", (byte) sy);
            var state = new CompoundTag();
            var palette = new ListTag();
            var entry = new CompoundTag();
            entry.putString("Name", "minecraft:air");
            palette.add(entry);
            state.put("palette", palette);
            section.put("block_states", state);
            var biomes = new CompoundTag();
            var biomePalette = new ListTag();
            biomePalette.add(net.minecraft.nbt.StringTag.valueOf("moderner_beta:indev_normal"));
            biomes.put("palette", biomePalette);
            section.put("biomes", biomes);
            sections.add(section);
        }
        chunk.put("sections", sections);
        return chunk;
    }

    @Test
    void provablyEmptyExteriorIsReleasedWhileAnythingHoldingContentIsNot() throws Exception {
        // Same fixture shape as the shell test, except the exterior chunk that CAN be released is empty rather than
        // shell-shaped - so this fails on the old rule, which released only bedrock/dirt/water chunks. Measured
        // against real worlds, that rule left ~95% of even a default world's exterior chunks behind and every one of
        // a custom-settings world's, because a custom map has no shell at all.
        FiniteTransitionIdentityTest.fixture(world, 123, 100);
        Path path = world.resolve("region/r.0.0.mca");
        Files.createDirectories(path.getParent());
        CompoundTag occupied = emptyChunk();
        var container = new CompoundTag();
        container.putString("id", "minecraft:chest");
        var containers = new ListTag();
        containers.add(container);
        occupied.put("block_entities", containers);
        // 7 is inside the island; 8 is empty; 9 is empty but holds a chest; 10 is the shell and has an entity record.
        Files.write(path, region(Map.of(7, apron(), 8, emptyChunk(), 9, occupied, 10, apron())));
        Path entities = world.resolve("entities/r.0.0.mca");
        Files.createDirectories(entities.getParent());
        var entityChunk = new CompoundTag();
        var list = new ListTag();
        var entity = new CompoundTag();
        entity.putString("id", "minecraft:item");
        list.add(entity);
        entityChunk.put("Entities", list);
        Files.write(entities, region(Map.of(10, entityChunk)));

        var plan = FiniteApronRelease.release(world);
        assertEquals(1, plan.releasedChunks(), "only the provably empty exterior chunk may be released");
        assertEquals(2, plan.preservedExteriorChunks(), "the chest chunk and the entity-record chunk stay");
        var snapshot = new FiniteRegionSnapshot(path);
        assertEquals(List.of(7, 9, 10), snapshot.chunks.stream().map(FiniteRegionSnapshot.Chunk::slot).toList(),
            "slot 8 is gone, and only slot 8");
        assertFalse(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(8, 0)));
        assertTrue(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(9, 0)));
    }

    @Test
    void aRecordedCustomFootprintDecidesWhatIsIslandAndWhatIsExterior() throws Exception {
        // The end-to-end consequence of reading the world's own geometry. Chunk -5 is INSIDE the shipped default
        // footprint [-8,8) and OUTSIDE a 64-block map's [-2,2): under the hardcoded footprint it was island and kept
        // forever, which is the ring of void the owner is left with. With the geometry read from the world it is
        // exterior, and being provably empty it is released. The same world also proves the shell path still works.
        FiniteTransitionIdentityTest.fixture(world, 123, 100);
        var root = FiniteTransitionIdentityTest.level(world);
        var properties = new CompoundTag();
        properties.putString("type", "inland");
        properties.putString("theme", "normal");
        properties.putInt("width", 64);
        properties.putInt("length", 64);
        properties.putInt("height", 64);
        FiniteTransitionIdentityTest.generator(root).getCompound("provider_settings")
            .put("moderner_beta:finite_level_properties", properties);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));

        Path path = world.resolve("region/r.0.0.mca");
        Files.createDirectories(path.getParent());
        // Region r.0.0 gives chunk x = slot % 32, z = 0. With a 64-block recorded map the island is x in [-2,2), so:
        // slot 1 is island, slot 2 is the RING (outside the recorded footprint, inside the shipped default's [-8,8)),
        // and slot 9 is beyond both. Slot 2 is empty, slot 9 is shell-shaped.
        Files.write(path, region(Map.of(1, apron(), 2, emptyChunk(), 9, apron())));

        var plan = FiniteApronRelease.release(world);
        assertEquals(new net.historicalexperience.world.upgrade.FiniteGeometry(64, 64, 64, "inland", "normal", true),
            plan.identity().geometry());
        assertEquals(2, plan.releasedChunks(),
            "the empty ring chunk the hardcoded footprint called island, and the shell chunk beyond both footprints");
        assertEquals(0, plan.preservedExteriorChunks(),
            "the kept chunk is island under the recorded geometry, so it is protected terrain rather than kept exterior");
        assertTrue(plan.protectedTerrainChunks().contains(net.minecraft.world.level.ChunkPos.asLong(1, 0)),
            "the island chunk is recorded as terrain to preserve");
        var snapshot = new FiniteRegionSnapshot(path);
        assertEquals(List.of(1), snapshot.chunks.stream().map(FiniteRegionSnapshot.Chunk::slot).toList(),
            "the island chunk survives and the two exterior ones are gone");
    }

    /** Exterior terrain of the kind an Inland map generates: solid, flat, neither the water shell nor empty. */
    static CompoundTag plainChunk() {
        var chunk = emptyChunk();
        var sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        var state = sections.getCompound(0).getCompound("block_states");
        var palette = new ListTag();
        for (String name : List.of("minecraft:air", "minecraft:grass_block", "minecraft:dirt", "minecraft:stone")) {
            var entry = new CompoundTag();
            entry.putString("Name", name);
            palette.add(entry);
        }
        state.put("palette", palette);
        // Four bits per block: y=0 stone, y=1..3 dirt, y=4 grass, above that air - a plain surface, which is all this
        // needs: the point is that it is neither the known shell nor empty.
        long[] packed = new long[256];
        for (int i = 0; i < 4096; i++) {
            int y = i >> 8;
            int value = y == 0 ? 3 : y < 4 ? 2 : y == 4 ? 1 : 0;
            packed[i / 16] |= (long) value << (i % 16 * 4);
        }
        state.putLongArray("data", packed);
        return chunk;
    }

    @Test
    void theUpgradePathCutsOffPlainExteriorTerrainThatTheNarrowRuleWouldKeep() throws Exception {
        // The owner's case, exactly: an Inland map's outside is generated flat land, which is neither the water shell
        // nor empty, so the narrow rule kept every such chunk and the finite plain survived the upgrade. The upgrade
        // path has no proven source and does not consult shape, so it cuts them; the repair path keeps its own rule,
        // which the other tests here and in FiniteEdgeRepairTest pin.
        FiniteTransitionIdentityTest.fixture(world, 123, 100);
        Path path = world.resolve("region/r.0.0.mca");
        Files.createDirectories(path.getParent());
        Files.write(path, region(Map.of(1, apron(), 9, plainChunk())));

        var upgrade = FiniteApronRelease.release(world);
        assertTrue(upgrade.blanketExterior(), "an explicit upgrade records that it used the cut-everything rule");
        assertEquals(1, upgrade.releasedChunks(), "the plain land outside the island is cut off");
        assertEquals(List.of(1), new FiniteRegionSnapshot(path).chunks.stream()
            .map(FiniteRegionSnapshot.Chunk::slot).toList(), "the island chunk stays, the plain one is gone");
    }

    @Test void recoveryIsIdempotentAndCorruptBackupRefusesBeforeWriting() throws Exception {
        Path path = fixture(); byte[] original = Files.readAllBytes(path);
        var completed = FiniteApronRelease.release(world);
        byte[] after = Files.readAllBytes(path);
        var pending = new FiniteApronRelease.Plan(3, false, completed.edgeHeights(), completed.releasedChunks(),
            completed.preservedExteriorChunks(), completed.changes(), completed.protectedTerrainChunks(), completed.identity(), completed.blanketExterior());
        Files.write(path, original); // Crash before the region write.
        FiniteApronRelease.apply(world, pending); assertArrayEquals(after, Files.readAllBytes(path));
        FiniteApronRelease.apply(world, pending); assertArrayEquals(after, Files.readAllBytes(path));
        Files.write(path, original);
        Files.write(world.resolve("historical_experience_finite_transition_backup/r.0.0.mca.before"), new byte[] {1});
        assertThrows(java.io.IOException.class, () -> FiniteApronRelease.apply(world, pending));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test void copiedPlanCannotReleaseOrAttachToAnotherWorld() throws Exception {
        Path path = fixture();
        var plan = FiniteApronRelease.release(world);
        byte[] before = Files.readAllBytes(path);
        Path other = world.resolve("other"); FiniteTransitionIdentityTest.fixture(other, 124, 100);
        Files.copy(world.resolve(FiniteApronRelease.FILE_NAME), other.resolve(FiniteApronRelease.FILE_NAME));
        assertThrows(java.io.IOException.class, () -> FiniteApronRelease.read(other));
        assertThrows(java.io.IOException.class, () -> FiniteApronRelease.apply(other, plan));
        assertArrayEquals(before, Files.readAllBytes(path));
        // Even the same seed is insufficient if the immutable historical origin differs.
        FiniteTransitionIdentityTest.fixture(other, 123, 101);
        assertThrows(java.io.IOException.class, () -> FiniteApronRelease.read(other));
    }

    @Test void malformedFiniteMapSettingsRefuseBeforeCreatingBackupsOrWritingRegions() throws Exception {
        // What still refuses: map settings that cannot be read (here, a width that is not an int). A well-formed
        // customization is accepted now - that is the owner's decision - but an unprovable geometry must never be
        // handed to the release, and the refusal must land before any backup or region write exists.
        Path path = fixture(); byte[] before = Files.readAllBytes(path);
        var root = FiniteTransitionIdentityTest.level(world);
        var properties = new CompoundTag();
        properties.putString("type", "island");
        properties.putString("theme", "normal");
        properties.putString("width", "512"); // a map setting with the wrong type, not a foreign component
        properties.putInt("length", 256);
        properties.putInt("height", 128);
        FiniteTransitionIdentityTest.generator(root).getCompound("biome_source").getCompound("provider_settings")
            .put("moderner_beta:finite_level_properties", properties);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        assertThrows(java.io.IOException.class, () -> FiniteApronRelease.release(world));
        assertArrayEquals(before, Files.readAllBytes(path));
        assertFalse(Files.exists(world.resolve(FiniteApronRelease.FILE_NAME)));
        assertFalse(Files.exists(world.resolve("historical_experience_finite_transition_backup")));
    }

    @Test void fullWidthNegativeSeedSurvivesJsonPlanRoundTrip() throws Exception {
        fixture();
        long seed = Long.MIN_VALUE + 17;
        var root = FiniteTransitionIdentityTest.level(world);
        root.getCompound("Data").getCompound("WorldGenSettings").putLong("seed", seed);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        FiniteApronRelease.release(world);
        assertEquals(seed, FiniteApronRelease.read(world).identity().worldSeed());
    }

    @Test void unboundDevelopmentPlanIsNeverSilentlyAccepted() throws Exception {
        Path path = fixture(); FiniteApronRelease.release(world);
        byte[] before = Files.readAllBytes(path);
        Path manifest = world.resolve(FiniteApronRelease.FILE_NAME);
        Files.writeString(manifest, Files.readString(manifest).replace("\"schema\": 3", "\"schema\": 2"));
        assertThrows(java.io.IOException.class, () -> FiniteApronRelease.read(world));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test void emptyVanillaEntityRegionsAreNotCorruption() throws Exception {
        Path path = world.resolve("r.0.0.mca");
        Files.write(path, new byte[0]);
        assertTrue(new FiniteRegionSnapshot(path).chunks.isEmpty());
        Files.write(path, new byte[4096]);
        assertTrue(new FiniteRegionSnapshot(path).chunks.isEmpty());
        Files.write(path, new byte[] {1});
        assertThrows(java.io.IOException.class, () -> new FiniteRegionSnapshot(path));
    }

    @Test void oneChangedBlockOrFlowingWaterIsNotAnApron() throws Exception {
        var chunk = apron(); assertTrue(FiniteRegionSnapshot.pristineApron(chunk));
        var state = chunk.getList("sections", Tag.TAG_COMPOUND).getCompound(4).getCompound("block_states");
        long[] data = state.getLongArray("data"); data[0] &= ~15L; state.putLongArray("data", data);
        assertFalse(FiniteRegionSnapshot.pristineApron(chunk));
        chunk = apron();
        chunk.getList("sections", Tag.TAG_COMPOUND).getCompound(3).getCompound("block_states")
            .getList("palette", Tag.TAG_COMPOUND).getCompound(2).getCompound("Properties").putString("level", "1");
        assertFalse(FiniteRegionSnapshot.pristineApron(chunk));
        assertFalse(FiniteRegionSnapshot.pristineApron(new CompoundTag()));
    }

    @Test void collarMatchesEachEdgeAndReturnsExactlyToUpstream() {
        int[] heights = new int[1024]; Arrays.fill(heights, 54);
        var plan = new FiniteApronRelease.Plan(3, true, heights, 1, 0, List.of(), java.util.Set.of(), FiniteTransitionIdentityTest.dummy(), true);
        for (int[] pos : new int[][] {{128, 0}, {-129, 0}, {0, 128}, {0, -129}, {128, 128}, {-129, -129}}) {
            assertTrue(plan.blend(-1000, pos[0], 54, pos[1]) > 0);
            assertTrue(plan.blend(1000, pos[0], 55, pos[1]) < 0);
        }
        assertEquals(-123, plan.blend(-123, 191, 54, 0));
        assertEquals(456, plan.blend(456, -192, 54, 0));
        assertEquals(789, plan.blend(789, 0, 54, 0), "never modify the finite island footprint");
        heights[768 + 128] = -1;
        var unknown = new FiniteApronRelease.Plan(3, true, heights, 0, 0, List.of(), java.util.Set.of(), FiniteTransitionIdentityTest.dummy(), true);
        assertEquals(123, unknown.blend(123, 128, 54, 0), "unknown edges never fabricate heights");
    }
}

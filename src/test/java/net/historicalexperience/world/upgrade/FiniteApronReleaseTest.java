/* SPDX-License-Identifier: LGPL-3.0-or-later */
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
        assertEquals(16, plan.knownEdgeColumns());
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

    @Test void recoveryIsIdempotentAndCorruptBackupRefusesBeforeWriting() throws Exception {
        Path path = fixture(); byte[] original = Files.readAllBytes(path);
        var completed = FiniteApronRelease.release(world);
        byte[] after = Files.readAllBytes(path);
        var pending = new FiniteApronRelease.Plan(3, false, completed.edgeHeights(), completed.releasedChunks(),
            completed.preservedExteriorChunks(), completed.changes(), completed.protectedTerrainChunks(), completed.identity());
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

    @Test void customFiniteSourceRefusesBeforeCreatingBackupsOrWritingRegions() throws Exception {
        Path path = fixture(); byte[] before = Files.readAllBytes(path);
        var root = FiniteTransitionIdentityTest.level(world);
        FiniteTransitionIdentityTest.generator(root).getCompound("biome_source").getCompound("provider_settings")
            .putInt("moderner_beta:finite_width", 512);
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
        var plan = new FiniteApronRelease.Plan(3, true, heights, 1, 0, List.of(), java.util.Set.of(), FiniteTransitionIdentityTest.dummy());
        for (int[] pos : new int[][] {{128, 0}, {-129, 0}, {0, 128}, {0, -129}, {128, 128}, {-129, -129}}) {
            assertTrue(plan.blend(-1000, pos[0], 54, pos[1]) > 0);
            assertTrue(plan.blend(1000, pos[0], 55, pos[1]) < 0);
        }
        assertEquals(-123, plan.blend(-123, 191, 54, 0));
        assertEquals(456, plan.blend(456, -192, 54, 0));
        assertEquals(789, plan.blend(789, 0, 54, 0), "never modify the finite island footprint");
        heights[768 + 128] = -1;
        var unknown = new FiniteApronRelease.Plan(3, true, heights, 0, 0, List.of(), java.util.Set.of(), FiniteTransitionIdentityTest.dummy());
        assertEquals(123, unknown.blend(123, 128, 54, 0), "unknown edges never fabricate heights");
    }
}

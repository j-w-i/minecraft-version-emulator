/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/** Strict read-only Anvil view. Removing slots changes headers only, retaining all other blobs byte-for-byte. */
final class FiniteRegionSnapshot {
    record Chunk(int slot, int x, int z, CompoundTag tag) { }
    final Path path;
    final byte[] bytes;
    final List<Chunk> chunks;

    FiniteRegionSnapshot(Path path) throws IOException { this(path, path.getFileName().toString()); }

    /** Immutable journal images have a .before suffix, but retain the original region's coordinate identity. */
    FiniteRegionSnapshot(Path path, String regionName) throws IOException {
        this.path = path;
        this.bytes = Files.readAllBytes(path);
        // Vanilla creates an empty region handle before its first chunk write; entity regions can therefore be 0 bytes.
        if (bytes.length == 0 || (bytes.length == 4096 && java.util.Arrays.equals(bytes, new byte[4096]))) {
            this.chunks = List.of();
            return;
        }
        if (bytes.length < 8192) throw new IOException("Truncated region " + path);
        String[] name = regionName.split("\\.");
        if (name.length != 4 || !name[0].equals("r") || !name[3].equals("mca")) {
            throw new IOException("Invalid region filename " + path);
        }
        int rx = Integer.parseInt(name[1]), rz = Integer.parseInt(name[2]);
        var result = new ArrayList<Chunk>();
        for (int slot = 0; slot < 1024; slot++) {
            int location = ByteBuffer.wrap(bytes, slot * 4, 4).getInt();
            if (location == 0) continue;
            int offset = (location >>> 8) * 4096, sectors = location & 255;
            if (offset < 8192 || sectors == 0 || (long) offset + sectors * 4096L > bytes.length) {
                throw new IOException("Invalid chunk location in " + path + " slot " + slot);
            }
            int size = ByteBuffer.wrap(bytes, offset, 4).getInt();
            if (size < 1 || size > sectors * 4096 - 4) throw new IOException("Invalid chunk size in " + path);
            int compression = bytes[offset + 4] & 255;
            var input = new ByteArrayInputStream(bytes, offset + 5, size - 1);
            var decoded = switch (compression) {
                case 1 -> new GZIPInputStream(input);
                case 2 -> new InflaterInputStream(input);
                case 3 -> input;
                default -> throw new IOException("Unsupported/external chunk compression in " + path);
            };
            CompoundTag tag;
            try (var data = new DataInputStream(decoded)) {
                tag = NbtIo.read(data, NbtAccounter.create(64L * 1024 * 1024));
            }
            result.add(new Chunk(slot, rx * 32 + slot % 32, rz * 32 + slot / 32, tag));
        }
        this.chunks = List.copyOf(result);
    }

    byte[] without(List<Integer> slots) {
        byte[] result = bytes.clone();
        for (int slot : slots) {
            if (slot < 0 || slot >= 1024) throw new IllegalArgumentException("Invalid region slot");
            ByteBuffer.wrap(result).putInt(slot * 4, 0).putInt(4096 + slot * 4, 0);
        }
        return result;
    }

    /** Names and water level are retained for exact synthetic-apron comparison; no registry bootstrap required. */
    static String[] blocks(CompoundTag chunk) throws IOException {
        String[] result = new String[128 * 256];
        java.util.Arrays.fill(result, "minecraft:air");
        var sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.getCompound(i);
            int sy = section.getByte("Y");
            if (!section.contains("block_states", Tag.TAG_COMPOUND)) continue;
            var state = section.getCompound("block_states");
            var palette = state.getList("palette", Tag.TAG_COMPOUND);
            if (palette.isEmpty()) throw new IOException("Missing block palette");
            String[] names = new String[palette.size()];
            for (int p = 0; p < names.length; p++) {
                var entry = palette.getCompound(p);
                names[p] = entry.getString("Name");
                if (names[p].isEmpty()) throw new IOException("Missing block name");
                var properties = entry.getCompound("Properties");
                if (names[p].equals("minecraft:water")) {
                    if ((!properties.isEmpty() && !properties.getAllKeys().equals(java.util.Set.of("level")))
                            || (properties.contains("level") && !properties.getString("level").equals("0"))) names[p] += ":nondefault";
                } else if (!properties.isEmpty()) names[p] += ":nondefault";
                // Any extra block state outside the finite envelope is not the known synthetic apron.
            }
            int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(names.length - 1));
            int perLong = 64 / bits;
            long[] data = state.getLongArray("data");
            if (names.length > 1 && data.length < (4096 + perLong - 1) / perLong) {
                throw new IOException("Truncated block-state data");
            }
            for (int index = 0; index < 4096; index++) {
                int value = names.length == 1 ? 0 : (int) ((data[index / perLong]
                    >>> ((index % perLong) * bits)) & ((1L << bits) - 1));
                if (value >= names.length) throw new IOException("Invalid palette index");
                int y = sy * 16 + (index >> 8);
                String name = names[value];
                if (y >= 0 && y < 128) result[y * 256 + (index & 255)] = name;
                else if (!name.equals("minecraft:air")) throw new IOException("Nonempty terrain outside finite height");
            }
        }
        return result;
    }

    static boolean pristineApron(CompoundTag chunk) throws IOException {
        if (!chunk.getList("block_entities", Tag.TAG_COMPOUND).isEmpty()
                || !chunk.getList("entities", Tag.TAG_COMPOUND).isEmpty()) return false;
        String[] blocks = blocks(chunk);
        for (int y = 0; y < 128; y++) {
            String expected = y < 54 ? "minecraft:bedrock" : y == 54 ? "minecraft:dirt"
                : y < 64 ? "minecraft:water" : "minecraft:air";
            for (int column = 0; column < 256; column++) {
                if (!expected.equals(blocks[y * 256 + column])) return false;
            }
        }
        return true;
    }
}

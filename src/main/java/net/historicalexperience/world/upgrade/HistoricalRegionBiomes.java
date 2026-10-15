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

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.historicalexperience.version.HistoricalVersion;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Rewrites the biome stored in each chunk of an Anvil region file.
 *
 * <h2>Why this is needed at all</h2>
 *
 * <p>Moderner Beta's biome source answers every position from its provider, so a settings preset decides what F3
 * reports - but a chunk also STORES a biome per section, and the APIs backed by the chunk read that instead. After a
 * preset change the two disagree until the stored values are rewritten. Measured on a real Release 1.6.4 world: the
 * provider said {@code moderner_beta:late_beta_plains} while {@code Level.getBiome} still said
 * {@code moderner_beta:beta_rainforest}, because the chunks had been written under the old preset.
 *
 * <h2>Why it edits region files rather than chunks</h2>
 *
 * <p>The conversion has to finish before the world is opened, so there is no loaded chunk to work through. Region
 * files are also the only place the values live before the server reads them.
 *
 * <p>The rewrite replaces every entry of every biome palette, so a section holding several biomes becomes the target
 * biome without re-packing any index. Sizes change when the target name has a different length, so the location table
 * is recomputed; timestamps are preserved; and the write is atomic, through a temporary file, so an interrupted run
 * leaves the original region intact.
 */
public final class HistoricalRegionBiomes {
    /** Anvil files address chunks in 4 KiB sectors. */
    private static final int SECTOR = 4096;

    /** A region file holds 32x32 chunks. */
    private static final int SLOTS = 1024;

    /** The location table and the timestamp table, 4 KiB each. */
    private static final int HEADER = 8192;

    private HistoricalRegionBiomes() {
    }

    /** What one region's pass did. */
    public record RegionResult(int chunksRead, int chunksRewritten, int paletteEntriesChanged) {
    }

    /**
     * Reports whether any chunk in a region file names a biome other than {@code target}, decompressing as little as
     * possible. Used to decide whether a world needs the conversion at all, so an already-converted world costs one
     * scan of its biome palettes and nothing else.
     */
    public static boolean needsConversion(Path regionFile, HistoricalVersion destination) throws IOException {
        byte[] raw = readHeaderAndBody(regionFile);
        if (raw == null) {
            return false;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            byte[] blob = blobAt(raw, slot);
            if (blob == null) {
                continue;
            }
            CompoundTag chunk;
            try {
                chunk = decode(blob);
            } catch (IOException notThisChunk) {
                // A chunk that cannot be decoded is left exactly as it is, and it is not evidence of a needed
                // conversion either way; the rewrite pass reports its own failure count.
                continue;
            }
            if (holdsCarriedBiome(chunk, destination)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rewrites every biome palette in one region file, carrying each stored biome to its counterpart. Returns what it
     * did; a region with nothing to change is left byte-identical on disk, which is what makes a second run free.
     */
    public static RegionResult convert(Path regionFile, HistoricalVersion destination) throws IOException {
        byte[] raw = readHeaderAndBody(regionFile);
        if (raw == null) {
            return new RegionResult(0, 0, 0);
        }

        int chunksRead = 0;
        int chunksRewritten = 0;
        int entriesChanged = 0;
        // The rebuilt body, in slot order, with the location table recomputed as we go.
        ByteBuffer header = ByteBuffer.allocate(HEADER);
        header.position(HEADER / 2);
        header.put(raw, HEADER / 2, HEADER / 2);          // timestamps are preserved verbatim
        header.position(0);

        ByteArrayOutputStream body = new ByteArrayOutputStream(raw.length);
        int sector = 2;
        for (int slot = 0; slot < SLOTS; slot++) {
            byte[] blob = blobAt(raw, slot);
            if (blob == null) {
                continue;
            }
            chunksRead++;
            byte compression = blob[4];
            CompoundTag chunk;
            int changed;
            try {
                chunk = decode(blob);
                changed = rewriteBiomes(chunk, destination);
            } catch (IOException undecodable) {
                changed = 0;
                chunk = null;
            }
            byte[] out = blob;
            if (changed > 0 && chunk != null) {
                byte[] encoded = encode(chunk, compression);
                out = new byte[5 + encoded.length];
                writeInt(out, 0, encoded.length + 1);
                out[4] = compression;
                System.arraycopy(encoded, 0, out, 5, encoded.length);
                chunksRewritten++;
                entriesChanged += changed;
            }
            int sectors = (out.length + SECTOR - 1) / SECTOR;
            header.put(slot * 4, (byte) (sector >>> 16));
            header.put(slot * 4 + 1, (byte) (sector >>> 8));
            header.put(slot * 4 + 2, (byte) sector);
            header.put(slot * 4 + 3, (byte) sectors);
            body.write(out, 0, out.length);
            int padding = sectors * SECTOR - out.length;
            for (int i = 0; i < padding; i++) {
                body.write(0);
            }
            sector += sectors;
        }

        if (chunksRewritten == 0) {
            return new RegionResult(chunksRead, 0, 0);
        }
        Path temp = regionFile.resolveSibling(regionFile.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(header.array()));
            channel.write(ByteBuffer.wrap(body.toByteArray()));
        }
        Files.move(temp, regionFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return new RegionResult(chunksRead, chunksRewritten, entriesChanged);
    }

    /** The chunk blobs a region holds, as packed chunk positions, so the caller can report progress per chunk. */
    public static LongList chunkPositions(Path regionFile) throws IOException {
        LongList positions = new LongArrayList();
        byte[] raw = readHeaderAndBody(regionFile);
        if (raw == null) {
            return positions;
        }
        int regionX = regionCoordinate(regionFile, 1);
        int regionZ = regionCoordinate(regionFile, 2);
        for (int slot = 0; slot < SLOTS; slot++) {
            if (blobAt(raw, slot) == null) {
                continue;
            }
            int chunkX = regionX * 32 + (slot % 32);
            int chunkZ = regionZ * 32 + (slot / 32);
            positions.add(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ));
        }
        return positions;
    }

    /**
     * The chunks of a region that were really generated beforehand, as packed chunk positions.
     *
     * <p>{@link #chunkPositions} lists every blob in the file, and that is too many. A region also holds the ring of
     * placeholder chunks the game writes around generated terrain just to look up structures: status
     * {@code structure_starts} (or earlier), with no terrain and no biome of their own yet. Recording those as
     * "generated beforehand" made Moderner Beta pin them to the converted biome when they were eventually generated
     * for real - a third of the recorded set in a measured world - which is exactly the generation the owner said must
     * stay untouched.
     */
    public static LongList generatedChunkPositions(Path regionFile) throws IOException {
        LongList positions = new LongArrayList();
        byte[] raw = readHeaderAndBody(regionFile);
        if (raw == null) {
            return positions;
        }
        int regionX = regionCoordinate(regionFile, 1);
        int regionZ = regionCoordinate(regionFile, 2);
        for (int slot = 0; slot < SLOTS; slot++) {
            byte[] blob = blobAt(raw, slot);
            if (blob == null) {
                continue;
            }
            try {
                if (!hasGeneratedBiomes(decode(blob))) {
                    continue;
                }
            } catch (IOException undecodable) {
                // A chunk that cannot be read is left alone by the rewrite as well, so it is not claimed here.
                continue;
            }
            int chunkX = regionX * 32 + (slot % 32);
            int chunkZ = regionZ * 32 + (slot / 32);
            positions.add(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ));
        }
        return positions;
    }

    /**
     * Whether a chunk's status means its biomes were really computed. The game fills biomes in at the
     * {@code biomes} step, so the three statuses before it hold none, and a missing status is an older format that did.
     */
    static boolean hasGeneratedBiomes(CompoundTag chunk) {
        if (!chunk.contains("Status", Tag.TAG_STRING)) {
            return true;
        }
        String status = chunk.getString("Status");
        return !(status.equals("minecraft:empty") || status.equals("empty")
            || status.equals("minecraft:structure_starts") || status.equals("structure_starts")
            || status.equals("minecraft:structure_references") || status.equals("structure_references"));
    }

    /** {@code r.<x>.<z>.mca} -> the requested coordinate, or 0 when the name is not a region file. */
    private static int regionCoordinate(Path regionFile, int index) {
        String[] parts = regionFile.getFileName().toString().split("\\.");
        if (parts.length <= index) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException notARegion) {
            return 0;
        }
    }

    /**
     * Carry every biome palette entry in a chunk to its counterpart. Returns how many entries changed.
     *
     * <p>A name with no counterpart is left alone, which is the point: an ocean stays an ocean and a modern biome stays
     * as it is. Only the names the carry-forward table knows about move.
     */
    static int rewriteBiomes(CompoundTag chunk, HistoricalVersion destination) {
        ListTag sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        int changed = 0;
        for (int index = 0; index < sections.size(); index++) {
            CompoundTag section = sections.getCompound(index);
            CompoundTag biomes = section.getCompound("biomes");
            ListTag palette = biomes.getList("palette", Tag.TAG_STRING);
            for (int entry = 0; entry < palette.size(); entry++) {
                String carried = BiomeCarryForward.destinationFor(palette.getString(entry), destination);
                if (carried != null) {
                    palette.set(entry, StringTag.valueOf(carried));
                    changed++;
                }
            }
        }
        return changed;
    }

    /** Whether a chunk holds any biome that would be carried forward. */
    static boolean holdsCarriedBiome(CompoundTag chunk, HistoricalVersion destination) {
        ListTag sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        for (int index = 0; index < sections.size(); index++) {
            ListTag palette = sections.getCompound(index).getCompound("biomes")
                .getList("palette", Tag.TAG_STRING);
            for (int entry = 0; entry < palette.size(); entry++) {
                if (BiomeCarryForward.destinationFor(palette.getString(entry), destination) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The location table plus the chunk bodies, or null when the file is too small to be a region. */
    private static byte[] readHeaderAndBody(Path regionFile) throws IOException {
        if (!Files.isRegularFile(regionFile)) {
            return null;
        }
        long size = Files.size(regionFile);
        if (size < HEADER) {
            return null;
        }
        byte[] raw = new byte[(int) Math.min(size, Integer.MAX_VALUE)];
        try (FileChannel channel = FileChannel.open(regionFile, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            while (buffer.hasRemaining() && channel.read(buffer) > 0) {
                // keep reading
            }
        }
        return raw;
    }

    /** The chunk blob in a slot, including its 4-byte length and 1-byte compression, or null when the slot is empty. */
    private static byte[] blobAt(byte[] raw, int slot) {
        int offset = ((raw[slot * 4] & 0xFF) << 16) | ((raw[slot * 4 + 1] & 0xFF) << 8) | (raw[slot * 4 + 2] & 0xFF);
        int sectors = raw[slot * 4 + 3] & 0xFF;
        if (offset == 0 || sectors == 0) {
            return null;
        }
        int start = offset * SECTOR;
        if (start + 5 > raw.length) {
            return null;
        }
        int length = readInt(raw, start);
        if (length <= 1 || start + 4 + length > raw.length) {
            return null;
        }
        byte[] blob = new byte[4 + length];
        System.arraycopy(raw, start, blob, 0, blob.length);
        return blob;
    }

    /** Decode a chunk blob with the compression scheme its own header byte names. */
    private static CompoundTag decode(byte[] blob) throws IOException {
        byte compression = blob[4];
        byte[] payload = new byte[blob.length - 5];
        System.arraycopy(blob, 5, payload, 0, payload.length);
        try (DataInputStream in = new DataInputStream(switch (compression) {
            case 1 -> new GZIPInputStream(new ByteArrayInputStream(payload));
            case 2 -> new InflaterInputStream(new ByteArrayInputStream(payload));
            default -> throw new IOException("unsupported chunk compression " + compression);
        })) {
            return NbtIo.read(in, NbtAccounter.unlimitedHeap());
        }
    }

    /** Re-encode a chunk with the scheme it arrived in, so nothing else about the file changes. */
    private static byte[] encode(CompoundTag chunk, byte compression) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var out = compression == 1
                ? new GZIPOutputStream(bytes)
                : new DeflaterOutputStream(bytes)) {
            NbtIo.write(chunk, new java.io.DataOutputStream(out));
        }
        return bytes.toByteArray();
    }

    private static int readInt(byte[] raw, int at) {
        return ((raw[at] & 0xFF) << 24) | ((raw[at + 1] & 0xFF) << 16) | ((raw[at + 2] & 0xFF) << 8)
            | (raw[at + 3] & 0xFF);
    }

    private static void writeInt(byte[] target, int at, int value) {
        target[at] = (byte) (value >>> 24);
        target[at + 1] = (byte) (value >>> 16);
        target[at + 2] = (byte) (value >>> 8);
        target[at + 3] = (byte) value;
    }
}

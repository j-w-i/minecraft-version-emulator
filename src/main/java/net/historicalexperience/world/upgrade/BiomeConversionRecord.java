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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Remembers which biome conversions a world has had, in a file of its own.
 *
 * <h2>Why this is a separate file and not the metadata document</h2>
 *
 * <p>It was first written into {@code historical_experience_metadata.json}, appending a history entry like the version
 * upgrades do. That was wrong, and it broke a world. {@code UpgradeRecovery} deliberately refuses to run when the
 * metadata revision is NEWER than the revision an interrupted upgrade's journal describes - it will not overwrite newer
 * metadata with an older document:
 *
 * <pre>recovery found revision 4 in the world but the journal only describes revision 3;
 * refusing to overwrite newer metadata</pre>
 *
 * <p>So a biome conversion landing between an upgrade being staged and its generator advance made the world refuse to
 * load at all, with the journal retained. The metadata document is owned by ONE writer, the version-upgrade
 * transaction; a second writer appending to it mid-transaction is a corruption however well-meaning.
 *
 * <p>This file is therefore independent: it carries no revision number, is never read by the upgrade machinery, and
 * cannot make a pending upgrade inconsistent. Revision-scoped completion survives app restarts and distinguishes
 * separate explicit version changes even when they share a biome generation. Target labels remain for legacy readers.
 */
public final class BiomeConversionRecord {
    /** The record file's name, beside the metadata document. */
    public static final String FILE_NAME = "historical_experience_biome_conversion.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The most entries this file will ever carry; two targets exist, so anything beyond this is a corrupt file. */
    private static final int MAX_ENTRIES = 16;

    private BiomeConversionRecord() {
    }

    /** The record file for a world directory. */
    public static Path fileFor(Path worldDirectory) {
        return worldDirectory.resolve(FILE_NAME);
    }

    /**
     * Every biome this world has had a conversion to, in the order the conversions happened.
     *
     * <p>An unreadable or corrupt file returns an empty list, which means the caller converts. That direction is
     * deliberate: a missing record must never be mistaken for "already converted", because the cost of converting
     * twice is wasted work while the cost of not converting is a world whose stored biomes disagree with its generator.
     */
    public static List<String> convertedTargets(Path worldDirectory) {
        Path file = fileFor(worldDirectory);
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("convertedTargets");
            if (array == null) {
                return List.of();
            }
            List<String> targets = new ArrayList<>();
            for (JsonElement element : array) {
                String target = element.getAsString();
                if (!target.isBlank() && !targets.contains(target)) {
                    targets.add(target);
                }
            }
            return List.copyOf(targets);
        } catch (IOException | RuntimeException unreadable) {
            return List.of();
        }
    }

    /** Completion time from legacy records, used only to avoid replaying conversions predating revision tracking. */
    public static long lastConvertedAt(Path worldDirectory) {
        Path file = fileFor(worldDirectory);
        if (!Files.isRegularFile(file)) {
            return -1;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.has("lastConvertedAt") ? root.get("lastConvertedAt").getAsLong() : -1;
        } catch (IOException | RuntimeException unreadable) {
            return -1;
        }
    }

    /** Persisted identity of the last completed version-change action; zero denotes a legacy record. */
    public static long completedUpgradeRevision(Path worldDirectory) {
        Path file = fileFor(worldDirectory);
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.has("completedUpgradeRevision") ? root.get("completedUpgradeRevision").getAsLong() : 0;
        } catch (IOException | RuntimeException unreadable) {
            return 0;
        }
    }

    /** The most recent biome this world was converted to, or null when it has never been converted. */
    public static String lastConvertedTarget(Path worldDirectory) {
        List<String> targets = convertedTargets(worldDirectory);
        return targets.isEmpty() ? null : targets.get(targets.size() - 1);
    }

    /**
     * Whether a conversion to {@code target} is still owed: true when this world has never been converted to exactly
     * that biome.
     *
     * <p>Written as "has this target ever been done" rather than "is it the most recent one", because a world can move
     * between profiles in either direction, and a last-target record would consider an earlier one owed again on the
     * way back - converting a world that had already had it and flattening whatever it generated in between.
     */
    public static boolean conversionOwed(Path worldDirectory, String target) {
        if (target == null) {
            return false;
        }
        return !convertedTargets(worldDirectory).contains(target);
    }

    /**
     * Records a completed conversion. Written atomically through a temporary file, so an interrupted write cannot leave
     * a half-written record that reads as a list of targets it does not actually contain.
     *
     * @return the recorded target, or null when the record could not be written
     */
    public static String record(Path worldDirectory, String target, long timestamp) {
        return record(worldDirectory, target, timestamp, java.util.Set.of());
    }

    /**
     * Records a completed conversion, together with the chunks it converted.
     *
     * <p>The chunk set is the reason this file exists rather than a note in the metadata. Moderner Beta's biome source
     * holds only registries and settings - no level and no chunk access - so it cannot ask whether a position belongs to
     * a chunk that already existed. The owner's rule is that it should report the converted biome for chunks generated
     * beforehand and generate normally for everything after, so it has to be TOLD which chunks those are.
     */
    public static String record(Path worldDirectory, String target, long timestamp, java.util.Set<Long> chunks) {
        return record(worldDirectory, target, timestamp, chunks, 0);
    }

    /** Records completion of a specific upgrade even if this biome generation was visited before. */
    public static String record(Path worldDirectory, String target, long timestamp, java.util.Set<Long> chunks,
            long upgradeRevision) {
        if (target == null) {
            return null;
        }
        org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger("historical_experience/biome-conversion");
        List<String> existing = new ArrayList<>(convertedTargets(worldDirectory));
        if (existing.contains(target) && upgradeRevision == 0) {
            return target;
        }
        // The last label describes the current converted chunk set, including when returning to a previous version.
        existing.remove(target);
        existing.add(target);
        if (existing.size() > MAX_ENTRIES) {
            // Only two targets exist, so this file has been corrupted by something else. Refusing to grow it is safer
            // than writing an unbounded document into the world.
            logger.error("Refusing to record a conversion: the record already lists {} targets", existing.size());
            return null;
        }

        JsonObject root = new JsonObject();
        root.addProperty("schema", 3);
        root.addProperty("completedUpgradeRevision", upgradeRevision);
        JsonArray array = new JsonArray();
        existing.forEach(array::add);
        root.add("convertedTargets", array);
        root.addProperty("lastConvertedAt", timestamp);
        // Which chunks existed before the conversion, per region as a bitmask over its 1024 slots. A bitmask keeps a
        // large world's record small: one 1024-character string per region rather than a list of chunk positions.
        if (!chunks.isEmpty()) {
            JsonArray regions = new JsonArray();
            encodeChunks(chunks).forEach((regionKey, mask) -> {
                JsonObject region = new JsonObject();
                region.addProperty("region", regionKey);
                region.addProperty("chunks", mask);
                regions.add(region);
            });
            root.add("convertedChunks", regions);
        }

        Path file = fileFor(worldDirectory);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            logger.info("Recorded biome conversion to {} for {} ({} pre-existing chunk(s))",
                target, worldDirectory, chunks.size());
            return target;
        } catch (IOException failure) {
            return null;
        }
    }

    /**
     * One 1024-character bitmask per region, keyed by the region's own coordinates packed into a long.
     *
     * <p>A region covers 32x32 chunks, so slot {@code (chunkX & 31) + (chunkZ & 31) * 32} is that region's own slot for
     * a chunk - the same layout an Anvil location table uses.
     */
    private static java.util.Map<Long, String> encodeChunks(java.util.Set<Long> chunks) {
        java.util.Map<Long, char[]> masks = new java.util.HashMap<>();
        for (long packed : chunks) {
            int chunkX = net.minecraft.world.level.ChunkPos.getX(packed);
            int chunkZ = net.minecraft.world.level.ChunkPos.getZ(packed);
            int regionX = chunkX >> 5;
            int regionZ = chunkZ >> 5;
            int slot = (chunkX & 31) + (chunkZ & 31) * 32;
            long regionKey = ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);
            char[] mask = masks.computeIfAbsent(regionKey, key -> {
                char[] blank = new char[1024];
                java.util.Arrays.fill(blank, '0');
                return blank;
            });
            mask[slot] = '1';
        }
        java.util.Map<Long, String> encoded = new java.util.TreeMap<>();
        masks.forEach((key, mask) -> encoded.put(key, new String(mask)));
        return encoded;
    }

    /**
     * The chunks that existed before the conversion, as packed chunk positions.
     *
     * <p>Empty when there is no record, or when the record predates the chunk set. Empty is the safe answer: it makes
     * Moderner Beta generate normally everywhere, which is what a world with no conversion does anyway.
     */
    public static java.util.Set<Long> convertedChunks(Path worldDirectory) {
        Path file = fileFor(worldDirectory);
        if (!Files.isRegularFile(file)) {
            return java.util.Set.of();
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray regions = root.getAsJsonArray("convertedChunks");
            if (regions == null) {
                return java.util.Set.of();
            }
            java.util.Set<Long> chunks = new java.util.HashSet<>();
            for (JsonElement element : regions) {
                JsonObject region = element.getAsJsonObject();
                long regionKey = region.get("region").getAsLong();
                int regionX = (int) (regionKey >> 32);
                int regionZ = (int) regionKey;
                String mask = region.get("chunks").getAsString();
                for (int slot = 0; slot < Math.min(1024, mask.length()); slot++) {
                    if (mask.charAt(slot) != '0') {
                        int chunkX = regionX * 32 + (slot % 32);
                        int chunkZ = regionZ * 32 + (slot / 32);
                        chunks.add(net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ));
                    }
                }
            }
            return java.util.Set.copyOf(chunks);
        } catch (IOException | RuntimeException unreadable) {
            return java.util.Set.of();
        }
    }

    /** Withdraw only explicitly released synthetic chunks. Preserve every other field/bit and retain a hash-verified backup.
     * Unlike the read-only convenience queries, a corrupt record FAILS this transaction rather than becoming empty.
     * The caller retains its repair request until this atomic, idempotent write succeeds.
     */
    public static int forgetReleasedChunks(Path worldDirectory, java.util.Set<Long> released) throws IOException {
        Path file = fileFor(worldDirectory);
        if (!Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS) || released.isEmpty()) return 0;
        if (Files.isSymbolicLink(file)) throw new IOException("Biome conversion record must not be a symlink during repair");
        byte[] original = Files.readAllBytes(file);
        JsonObject root;
        int removed = 0;
        try {
            root = JsonParser.parseString(new String(original, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("schema") || !java.util.Set.of("1", "2", "3").contains(root.get("schema").getAsString())) {
                throw new IllegalArgumentException("Unsupported biome record schema");
            }
            if (!root.has("convertedChunks")) return 0;
            var seen = new java.util.HashSet<Long>();
            for (JsonElement entry : root.getAsJsonArray("convertedChunks")) {
                JsonObject region = entry.getAsJsonObject();
                String encoded = region.get("region").getAsString();
                if (!encoded.matches("-?\\d+")) throw new IllegalArgumentException("Invalid biome region key");
                long key = Long.parseLong(encoded);
                if (!seen.add(key)) throw new IllegalArgumentException("Duplicate biome region key");
                String mask = region.get("chunks").getAsString();
                if (!mask.matches("[01]{1024}")) throw new IllegalArgumentException("Invalid biome region mask");
                char[] next = mask.toCharArray();
                int rx = (int) (key >> 32), rz = (int) key;
                for (int slot = 0; slot < 1024; slot++) {
                    long position = net.minecraft.world.level.ChunkPos.asLong(rx * 32 + slot % 32, rz * 32 + slot / 32);
                    if (next[slot] == '1' && released.contains(position)) { next[slot] = '0'; removed++; }
                }
                region.addProperty("chunks", new String(next));
            }
        } catch (RuntimeException corrupt) {
            throw new IOException("Cannot safely prune the biome conversion record; original retained", corrupt);
        }
        if (removed == 0) return 0;
        String hash = WorldBackup.sha256(original);
        Path directory = worldDirectory.resolve("historical_experience_finite_transition_backup");
        Files.createDirectories(directory);
        Path backup = directory.resolve("biome-conversion." + hash + ".before.json");
        if (!Files.exists(backup)) forcedWrite(backup, original, true);
        if (!WorldBackup.sha256(Files.readAllBytes(backup)).equals(hash)) throw new IOException("Biome record backup mismatch");
        Path temporary = file.resolveSibling(file.getFileName() + ".finite-repair.tmp");
        forcedWrite(temporary, (GSON.toJson(root) + "\n").getBytes(StandardCharsets.UTF_8), false);
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return removed;
    }

    private static void forcedWrite(Path file, byte[] bytes, boolean createNew) throws IOException {
        var options = createNew
            ? java.util.Set.of(java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)
            : java.util.Set.of(java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE);
        try (var channel = java.nio.channels.FileChannel.open(file, options)) {
            var buffer = java.nio.ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    /** Whether a chunk existed before the conversion, by packed position. */
    public static boolean wasConverted(java.util.Set<Long> convertedChunks, long packedChunkPos) {
        return convertedChunks.contains(packedChunkPos);
    }
}

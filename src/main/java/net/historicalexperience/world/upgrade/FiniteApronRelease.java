/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

/** Explicit finite-to-infinite compatibility migration; never a general chunk-regeneration tool.
 * Only exterior chunks equal to the pinned default Indev ocean apron are released. Island chunks,
 * edited exterior chunks, containers, entity-bearing chunks and POIs are never released.
 * A separate verified before/after journal precedes every region write and makes retries idempotent.
 */
public final class FiniteApronRelease {
    public static final String FILE_NAME = "historical_experience_finite_transition.json";
    private static final String BACKUP = "historical_experience_finite_transition_backup";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public record Change(String region, String beforeHash, String afterHash, List<Integer> slots) { }
    public record Plan(int schema, boolean complete, int[] edgeHeights, int releasedChunks,
            int preservedExteriorChunks, List<Change> changes, Set<Long> protectedTerrainChunks, FiniteTransitionIdentity identity) {
        public Plan {
            if (schema != 3 || identity == null || edgeHeights == null || edgeHeights.length != 1024 || changes == null || protectedTerrainChunks == null
                    || releasedChunks < 0 || preservedExteriorChunks < 0) throw new IllegalArgumentException("Invalid finite plan");
            edgeHeights = edgeHeights.clone();
            for (int h : edgeHeights) if (h < -1 || h > 127) throw new IllegalArgumentException("Invalid edge height");
            changes = List.copyOf(changes);
            protectedTerrainChunks = Set.copyOf(protectedTerrainChunks);
            for (Change change : changes) {
                if (!change.region.matches("r\\.-?\\d+\\.-?\\d+\\.mca")
                        || !change.beforeHash.matches("[0-9a-f]{64}") || !change.afterHash.matches("[0-9a-f]{64}")) {
                    throw new IllegalArgumentException("Invalid finite backup entry");
                }
                for (int slot : change.slots) if (slot < 0 || slot >= 1024) throw new IllegalArgumentException("Invalid slot");
            }
        }
        @Override public int[] edgeHeights() { return edgeHeights.clone(); }
        public int knownEdgeColumns() { return (int) Arrays.stream(edgeHeights).filter(h -> h >= 0).count(); }
        public Set<Long> releasedPositions() {
            Set<Long> positions = new HashSet<>();
            for (Change change : changes) {
                String[] parts = change.region.split("\\.");
                int rx = Integer.parseInt(parts[1]), rz = Integer.parseInt(parts[2]);
                for (int slot : change.slots) positions.add(ChunkPos.asLong(rx * 32 + slot % 32, rz * 32 + slot / 32));
            }
            return Set.copyOf(positions);
        }

        /** Smooth coast collar, applied to upstream density BEFORE block placement, only outside the finite footprint. */
        public double blend(double upstream, int x, int y, int z) {
            int dx = Math.max(-128 - x, x - 127), dz = Math.max(-128 - z, z - 127);
            int distance = Math.max(dx, dz);
            if (distance <= 0 || distance >= 64) return upstream;
            int h;
            if (dx >= dz) h = edgeHeights[(x < -128 ? 512 : 768) + Math.clamp(z + 128, 0, 255)];
            else h = edgeHeights[(z < -128 ? 0 : 256) + Math.clamp(x + 128, 0, 255)];
            if (h < 0) return upstream; // Unknown edge is not invented terrain data.
            double t = (distance - 1) / 63.0;
            t = t * t * (3 - 2 * t);
            double edge = (h + 0.5 - y) * 8;
            return edge * (1 - t) + upstream * t;
        }
    }

    private FiniteApronRelease() { }

    public static Plan read(Path world) throws IOException {
        Path file = world.resolve(FILE_NAME);
        if (!Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return null;
        if (Files.isSymbolicLink(file)) throw new IOException("Finite transition record must not be a symlink");
        try {
            Plan plan = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Plan.class);
            if (plan == null) throw new IOException("Empty finite transition record");
            plan.identity.verifyWorld(world);
            return plan;
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid finite transition record; refusing to guess", invalid);
        }
    }

    /** Caller owns the world lock and must prove the stored source preset is finite, destination infinite. */
    public static Plan release(Path world) throws IOException { return release(world, null); }

    /** Explicit repair may prove the original default source through a verified historical backup instead of current level.dat. */
    static Plan release(Path world, FiniteTransitionIdentity provenSource) throws IOException {
        return release(world, provenSource, FiniteChunkEvidence.nativeOnly());
    }

    static Plan release(Path world, FiniteTransitionIdentity provenSource, FiniteChunkEvidence evidence) throws IOException {
        Plan plan = read(world);
        if (plan != null && provenSource != null && !plan.identity.equals(provenSource)) {
            throw new IOException("Finite repair source differs from the prepared transition identity");
        }
        if (plan != null && plan.complete) return plan;
        if (plan == null) {
            List<Path> paths;
            if (!Files.isDirectory(world.resolve("region"))) return null;
            try (var stream = Files.list(world.resolve("region"))) {
                paths = stream.filter(path -> path.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca"))
                    .sorted().toList();
            }
            if (paths.isEmpty()) return null;
            var identity = provenSource == null ? FiniteTransitionIdentity.fromSource(world) : provenSource;
            identity.verifyWorld(world);
            Set<Long> protectedChunks = protectedChunks(world);
            Set<Long> terrainToPreserve = new HashSet<>();
            int[] heights = new int[1024];
            Arrays.fill(heights, -1);
            List<Change> changes = new ArrayList<>();
            int released = 0, kept = 0, uncertain = 0;
            for (Path path : paths) {
                FiniteRegionSnapshot region = new FiniteRegionSnapshot(path);
                List<Integer> slots = new ArrayList<>();
                for (var chunk : region.chunks) {
                    if (inside(chunk.x(), chunk.z())) {
                        if (hasTerrain(chunk.tag())) terrainToPreserve.add(ChunkPos.asLong(chunk.x(), chunk.z()));
                        if (chunk.x() == -8 || chunk.x() == 7 || chunk.z() == -8 || chunk.z() == 7) {
                            captureEdge(heights, chunk);
                        }
                    } else {
                        boolean pristine;
                        try { pristine = FiniteRegionSnapshot.pristineApron(chunk.tag()); }
                        catch (IOException unsupported) { pristine = false; }
                        boolean proven = pristine && evidence.allows(chunk);
                        if (pristine && !proven) uncertain++;
                        if (!protectedChunks.contains(ChunkPos.asLong(chunk.x(), chunk.z())) && proven) slots.add(chunk.slot());
                        else {
                            kept++;
                            if (hasTerrain(chunk.tag())) terrainToPreserve.add(ChunkPos.asLong(chunk.x(), chunk.z()));
                        }
                    }
                }
                if (!slots.isEmpty()) {
                    byte[] after = region.without(slots);
                    String name = path.getFileName().toString();
                    Path backup = world.resolve(BACKUP);
                    Files.createDirectories(backup);
                    verifiedStage(backup.resolve(name + ".before"), region.bytes);
                    verifiedStage(backup.resolve(name + ".after"), after);
                    changes.add(new Change(name, WorldBackup.sha256(region.bytes), WorldBackup.sha256(after), List.copyOf(slots)));
                    released += slots.size();
                }
            }
            if (uncertain > 0) net.historicalexperience.HistoricalExperienceMod.LOGGER.warn(
                "FINITE_TRANSITION retained {} block-matching apron chunks with unsupported or uncertain source evidence; their walls are intentionally preserved", uncertain);
            plan = new Plan(3, false, heights, released, kept, changes, terrainToPreserve, identity);
            writePlan(world, plan); // Commit intent only after every staged image has been forced and verified.
        }
        apply(world, plan, evidence);
        Plan complete = new Plan(3, true, plan.edgeHeights, plan.releasedChunks, plan.preservedExteriorChunks,
            plan.changes, plan.protectedTerrainChunks, plan.identity);
        writePlan(world, complete);
        net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
            "FINITE_TRANSITION released={} preservedExterior={} knownEdgeColumns={}/1024 backup={}",
            complete.releasedChunks, complete.preservedExteriorChunks, complete.knownEdgeColumns(), world.resolve(BACKUP));
        return complete;
    }

    static boolean inside(int x, int z) { return x >= -8 && x < 8 && z >= -8 && z < 8; }

    static void apply(Path world, Plan plan) throws IOException { apply(world, plan, FiniteChunkEvidence.nativeOnly()); }

    private static void apply(Path world, Plan plan, FiniteChunkEvidence evidence) throws IOException {
        plan.identity.verifyWorld(world);
        // Validate ALL files first, including already-written ones. A changed/corrupt image is never overwritten.
        Set<Long> occupied = protectedChunks(world);
        for (var change : plan.changes) {
            Path before = world.resolve(BACKUP).resolve(change.region + ".before");
            Path after = world.resolve(BACKUP).resolve(change.region + ".after");
            requireHash(before, change.beforeHash);
            requireHash(after, change.afterHash);
            // Revalidate prepared OLD development plans too; checksums are not proof of a safe source chunk.
            var original = new FiniteRegionSnapshot(before, change.region);
            for (int slot : change.slots) {
                var saved = original.chunks.stream().filter(chunk -> chunk.slot() == slot).findFirst()
                    .orElseThrow(() -> new IOException("Prepared finite slot is missing from its source image"));
                if (inside(saved.x(), saved.z()) || occupied.contains(ChunkPos.asLong(saved.x(), saved.z()))
                        || !FiniteRegionSnapshot.pristineApron(saved.tag()) || !evidence.allows(saved)) {
                    throw new IOException("Prepared finite slot lacks pristine default source evidence: " + change.region + " slot " + slot);
                }
            }
            String current = WorldBackup.sha256(Files.readAllBytes(world.resolve("region").resolve(change.region)));
            if (!current.equals(change.beforeHash) && !current.equals(change.afterHash)) {
                throw new IOException("Region changed during finite transition: " + change.region);
            }
        }
        for (var change : plan.changes) {
            Path target = world.resolve("region").resolve(change.region);
            if (WorldBackup.sha256(Files.readAllBytes(target)).equals(change.afterHash)) continue;
            atomicWrite(target, Files.readAllBytes(world.resolve(BACKUP).resolve(change.region + ".after")));
        }
    }

    private static boolean hasTerrain(net.minecraft.nbt.CompoundTag chunk) {
        return Set.of("minecraft:noise", "minecraft:surface", "minecraft:carvers", "minecraft:features",
            "minecraft:initialize_light", "minecraft:light", "minecraft:spawn", "minecraft:full").contains(chunk.getString("Status"));
    }

    private static void captureEdge(int[] heights, FiniteRegionSnapshot.Chunk chunk) throws IOException {
        if (!hasTerrain(chunk.tag())) return;
        String[] blocks = FiniteRegionSnapshot.blocks(chunk.tag());
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int wx = chunk.x() * 16 + x, wz = chunk.z() * 16 + z;
            if (wx != -128 && wx != 127 && wz != -128 && wz != 127) continue;
            int h = 0;
            for (int y = 127; y >= 0; y--) {
                String name = blocks[y * 256 + z * 16 + x];
                if (!name.equals("minecraft:air") && !name.startsWith("minecraft:water")
                        && !name.equals("minecraft:oak_leaves") && !name.equals("minecraft:oak_log")) { h = y; break; }
            }
            if (wz == -128) heights[wx + 128] = h;
            if (wz == 127) heights[256 + wx + 128] = h;
            if (wx == -128) heights[512 + wz + 128] = h;
            if (wx == 127) heights[768 + wz + 128] = h;
        }
    }

    private static Set<Long> protectedChunks(Path world) throws IOException {
        Set<Long> result = new HashSet<>();
        for (String folder : List.of("entities", "poi")) {
            if (!Files.isDirectory(world.resolve(folder))) continue;
            try (var stream = Files.list(world.resolve(folder))) {
                for (Path path : stream.filter(p -> p.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca")).toList()) {
                    for (var chunk : new FiniteRegionSnapshot(path).chunks) {
                        boolean occupied = !chunk.tag().getList("Entities", Tag.TAG_COMPOUND).isEmpty();
                        var sections = chunk.tag().getCompound("Sections");
                        for (String key : sections.getAllKeys()) {
                            occupied |= !sections.getCompound(key).getList("Records", Tag.TAG_COMPOUND).isEmpty();
                        }
                        if (occupied) result.add(ChunkPos.asLong(chunk.x(), chunk.z()));
                    }
                }
            }
        }
        return result;
    }

    private static void requireHash(Path path, String expected) throws IOException {
        if (!WorldBackup.sha256(Files.readAllBytes(path)).equals(expected)) throw new IOException("Finite backup mismatch: " + path);
    }
    private static void verifiedStage(Path path, byte[] bytes) throws IOException {
        if (!Files.exists(path)) {
            try (var channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
        }
        requireHash(path, WorldBackup.sha256(bytes));
    }
    private static void writePlan(Path world, Plan plan) throws IOException {
        atomicWrite(world.resolve(FILE_NAME), (GSON.toJson(plan) + "\n").getBytes(StandardCharsets.UTF_8));
    }
    private static void atomicWrite(Path path, byte[] bytes) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            var buffer = java.nio.ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}

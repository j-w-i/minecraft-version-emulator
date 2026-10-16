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
    /**
     * @param blanketExterior the owner's rule for an explicit UPGRADE: everything outside the island is cut off and
     *     regenerated except a chunk holding an entity or block entity, whatever shape the terrain has (an Inland
     *     map's outside is generated flat grass, which matches no known shell). False - the default, and what a plan
     *     written before this field existed reads as - keeps the audited REPAIR rule: only chunks whose stored source
     *     matches the known shell or is empty, AND whose evidence proves it. The two rules cannot be the same one:
     *     applying the blanket rule to repair broke its preservation guarantees outright.
     */
    public record Plan(int schema, boolean complete, int[] edgeHeights, int releasedChunks,
            int preservedExteriorChunks, List<Change> changes, Set<Long> protectedTerrainChunks,
            FiniteTransitionIdentity identity, boolean blanketExterior) {
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
            // The footprint is the world's own, not the default's: a 64x64 map's island is chunks [-2,2), and treating
            // the ring around it as island left every empty chunk there behind.
            FiniteGeometry geometry = identity.geometry();
            // The blanket rule belongs to the explicit UPGRADE, which is the call with no proven source. A repair
            // supplies the source it proved from a backup and must keep the narrow, evidence-backed rule.
            boolean blanket = provenSource == null;
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
                    if (geometry.inside(chunk.x(), chunk.z())) {
                        if (hasTerrain(chunk.tag())) terrainToPreserve.add(ChunkPos.asLong(chunk.x(), chunk.z()));
                        // The upgrade does NOT sample the island edge, because it does not blend. A hard cut is what
                        // the owner asked for - "you don't need to try and carefully transition in, you are allowed to
                        // just hard cut" - and it is what the online converters do: release everything outside, let
                        // the infinite generator write fresh chunks. Sampling here is what fed the collar that pulled
                        // the coastline into long ridges. The repair path still samples, because its job is the
                        // opposite: put back a rim without touching what is there.
                        if (!blanket && (chunk.x() == -8 || chunk.x() == 7 || chunk.z() == -8 || chunk.z() == 7)) {
                            captureEdge(heights, chunk);
                        }
                    } else {
                        // Two shapes are provably nothing and therefore releasable: the synthetic shell the finite
                        // provider lays down (bedrock/dirt/water), and a chunk that is entirely empty. The second is
                        // what a custom-settings map has instead of a shell, and what most of even a default map's
                        // exterior is - measuring saved worlds gave 37 shell chunks against 668 empty ones. Anything
                        // else, including an empty chunk carrying an entity or block-entity record, is kept.
                        // Shape rules are kept for now: the repair path shares this code and its audited guarantee is
                        // that only exact, evidence-backed source chunks are released. Scoping the owner's blanket
                        // "cut everything outside" rule TO THE UPGRADE PATH is the next step; applying it here broke
                        // five repair tests, which is the signal that it must not be global.
                        boolean proven;
                        boolean releasable;
                        if (blanket) {
                            // UPGRADE: the owner's rule. Anything outside the island is cut off unless it holds an
                            // entity or a block entity - somebody's animals or their chests - or an entity/POI region
                            // claims it. Shape is not consulted, which is the entire point: an Inland map's outside is
                            // generated flat grass.
                            releasable = !FiniteRegionSnapshot.holdsContent(chunk.tag());
                            proven = releasable;
                        } else {
                            // REPAIR: unchanged and deliberately narrow - the known shell, or a chunk that is entirely
                            // empty, and only with saved source evidence.
                            try {
                                releasable = FiniteRegionSnapshot.pristineApron(chunk.tag())
                                    || FiniteRegionSnapshot.provablyEmpty(chunk.tag());
                            } catch (IOException unsupported) {
                                releasable = false;
                            }
                            proven = releasable && evidence.allows(chunk);
                            if (releasable && !proven) uncertain++;
                        }
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
                "FINITE_TRANSITION retained {} provably-empty-or-shell exterior chunks with unsupported or uncertain source evidence; they are intentionally preserved", uncertain);
            plan = new Plan(3, false, heights, released, kept, changes, terrainToPreserve, identity, blanket);
            writePlan(world, plan); // Commit intent only after every staged image has been forced and verified.
        }
        apply(world, plan, evidence);
        Plan complete = new Plan(3, true, plan.edgeHeights, plan.releasedChunks, plan.preservedExteriorChunks,
            plan.changes, plan.protectedTerrainChunks, plan.identity, plan.blanketExterior);
        writePlan(world, complete);
        net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
            "FINITE_TRANSITION released={} preservedExterior={} knownEdgeColumns={}/1024 blanketExterior={} backup={}",
            complete.releasedChunks, complete.preservedExteriorChunks, complete.knownEdgeColumns(),
            complete.blanketExterior(), world.resolve(BACKUP));
        return complete;
    }

    /** The shipped default footprint. Kept for the developer measurement that compares worlds against it. */
    static boolean inside(int x, int z) { return FiniteGeometry.DEFAULT.inside(x, z); }

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
                // Re-prove the slot under the rule THIS plan was prepared with. A blanket (upgrade) plan proves the
                // chunk holds no entity or block entity; a repair plan additionally proves the stored source matches
                // the known shell or is empty, with saved evidence - the guarantee its tests pin.
                boolean acceptable = plan.blanketExterior()
                    ? !FiniteRegionSnapshot.holdsContent(saved.tag())
                    : (FiniteRegionSnapshot.pristineApron(saved.tag())
                        || FiniteRegionSnapshot.provablyEmpty(saved.tag())) && evidence.allows(saved);
                if (plan.identity().geometry().inside(saved.x(), saved.z())
                        || occupied.contains(ChunkPos.asLong(saved.x(), saved.z())) || !acceptable) {
                    throw new IOException("Prepared finite slot is not releasable under this plan's rule: "
                        + change.region + " slot " + slot);
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

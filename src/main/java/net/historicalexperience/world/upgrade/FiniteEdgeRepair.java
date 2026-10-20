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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;
import net.historicalexperience.world.metadata.MetadataSchema;

/** Explicit operator intent for OLD already-upgraded worlds. Never scans/deletes live chunks or auto-adopts a world.
 * A verified finite backup must prove the same origin, seed, source geometry and append-only upgrade history.
 * Request stays pending across crashes until BOTH the region transaction and biome bookkeeping finish.
 */
public final class FiniteEdgeRepair {
    public static final String FILE_NAME = "historical_experience_finite_edge_repair.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public record Request(int schema, boolean complete, String backupDirectory, String backupManifestHash,
            String metadataHash, FiniteTransitionIdentity identity, String actor, long timestamp) {
        public Request {
            if (schema != 1 || identity == null || actor == null || actor.isBlank() || timestamp < 0
                    || backupDirectory == null || !backupDirectory.matches("historical_experience_backups/\\d+-r\\d+-to-r\\d+")
                    || backupManifestHash == null || !backupManifestHash.matches("[0-9a-f]{64}")
                    || metadataHash == null || !metadataHash.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid finite-edge repair request");
            }
        }
    }
    private record Source(String backupDirectory, String manifestHash, FiniteTransitionIdentity identity, FiniteChunkEvidence evidence) { }
    private FiniteEdgeRepair() { }

    /** Invalid requests count as pending so neither loading nor another upgrade can bypass a corrupt intent file. */
    public static boolean pending(Path world) {
        if (!Files.exists(world.resolve(FILE_NAME), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return false;
        try { return !read(world).complete; }
        catch (IOException failure) { return true; }
    }

    /** Server-thread command path: stage intent ONLY. Players may keep playing until a normal shutdown. */
    public static String stage(Path world, String actor, long timestamp) throws IOException {
        if (Files.exists(world.resolve(FILE_NAME), java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            Request existing = read(world);
            if (!existing.complete) return "Finite-edge repair is already staged; stop and restart normally to finish it.";
            var plan = FiniteApronRelease.read(world);
            if (plan == null || !plan.complete()) throw new IOException("Completed repair has no completed transition plan");
            return "Finite-edge repair already completed; no chunks changed.";
        }
        if (UpgradeJournal.existsIn(world)) throw new IOException("Finish the pending version upgrade before staging an edge repair");
        if (Files.exists(world.resolve(FiniteApronRelease.FILE_NAME))) {
            var plan = FiniteApronRelease.read(world);
            if (plan != null && plan.complete()) return "This world already has a completed finite transition; no repair staged.";
            throw new IOException("A finite transition is still prepared; finish its owning upgrade first");
        }
        HistoricalMetadata current = currentInfiniteWorld(world);
        List<Source> sources = new ArrayList<>();
        Path backups = world.resolve(UpgradeJournal.BACKUP_DIRECTORY_NAME);
        if (Files.isDirectory(backups)) {
            try (var stream = Files.list(backups)) {
                for (Path directory : stream.filter(Files::isDirectory).sorted().toList()) {
                    String relative = UpgradeJournal.BACKUP_DIRECTORY_NAME + "/" + directory.getFileName();
                    if (!relative.matches("historical_experience_backups/\\d+-r\\d+-to-r\\d+")) continue;
                    try { sources.add(proveSource(world, relative, current)); }
                    catch (IOException invalidOrUnrelated) { /* No unverified backup becomes repair authority. */ }
                }
            }
        }
        if (sources.size() != 1) throw new IOException("Repair requires exactly one verified matching finite-to-infinite backup; found "
            + sources.size() + ". Missing, corrupt, foreign and ambiguous provenance is never guessed.");
        Source source = sources.getFirst();
        Request request = new Request(1, false, source.backupDirectory, source.manifestHash,
            metadataHash(world), source.identity, actor, timestamp);
        write(world, request);
        return "Finite-edge repair staged. Stop and restart normally. Only exact pristine apron chunks with saved source evidence are eligible; edited or uncertain chunks stay, even if that retains part of the wall.";
    }

    /** Caller holds the storage lock BEFORE world decode, or runs AFTER generation drained and all region handles closed. */
    public static boolean recover(Path world) throws IOException {
        if (!Files.exists(world.resolve(FILE_NAME), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return false;
        Request request = read(world);
        if (request.complete) return false;
        if (UpgradeJournal.existsIn(world)) throw new IOException("Version upgrade and finite repair are both pending; refusing concurrent transactions");
        if (!metadataHash(world).equals(request.metadataHash)) throw new IOException("Historical metadata changed after repair staging; request retained");
        Source source = proveSource(world, request.backupDirectory, currentInfiniteWorld(world));
        if (!source.manifestHash.equals(request.backupManifestHash) || !source.identity.equals(request.identity)) {
            throw new IOException("Finite repair provenance changed after staging; request retained");
        }
        request.identity.verifyWorld(world);
        var plan = FiniteApronRelease.release(world, request.identity, source.evidence);
        if (plan == null) throw new IOException("No region data available for the staged finite-edge repair");
        int pruned = BiomeConversionRecord.forgetReleasedChunks(world, plan.releasedPositions());
        write(world, new Request(1, true, request.backupDirectory, request.backupManifestHash,
            request.metadataHash, request.identity, request.actor, request.timestamp));
        net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
            "FINITE_EDGE_REPAIR completed: released={} preservedExterior={} biomePositionsWithdrawn={} source={}",
            plan.releasedChunks(), plan.preservedExteriorChunks(), pruned, request.backupDirectory);
        return true;
    }

    private static HistoricalMetadata currentInfiniteWorld(Path world) throws IOException {
        HistoricalMetadata metadata = metadata(world);
        if (HistoricalVersions.byId(metadata.profileId().value()).isEmpty()
                || HistoricalVersions.isFiniteWorld(metadata.profileId().value())
                || HistoricalVersions.isFinitePreset(metadata.generationPreset().value())) {
            throw new IOException("Repair requires an already-upgraded historical infinite world");
        }
        var recipeOwner = HistoricalVersions.byRevisionAlias(metadata.generationPreset().value());
        if (recipeOwner.isEmpty() || !recipeOwner.get().id().equals(metadata.profileId().value())) {
            throw new IOException("Current recipe does not belong to the recorded historical profile");
        }
        try {
            var state = WorldGenSettingsAdvance.read(world).orElseThrow(() -> new IOException("No historical stored generator"));
            if (!state.alreadyIs(metadata.generationPreset().value())) throw new IOException("Stored generator and metadata do not agree");
        } catch (UpgradeException failure) { throw new IOException("Cannot verify current generator", failure); }
        return metadata;
    }

    private static Source proveSource(Path world, String relative, HistoricalMetadata current) throws IOException {
        Path backup = world.resolve(relative);
        if (Files.isSymbolicLink(backup)) throw new IOException("Symlink backup is not repair provenance");
        try {
            WorldBackup.verify(world, relative);
            var manifest = WorldBackup.readManifest(backup);
            Set<String> entries = manifest.entries().stream().map(WorldBackup.Entry::relativePath).collect(java.util.stream.Collectors.toSet());
            if (!entries.containsAll(Set.of(MetadataSchema.FILE_NAME, WorldGenSettingsAdvance.FILE_NAME))) {
                throw new IOException("Backup does not verify both historical metadata and level.dat");
            }
            HistoricalMetadata source = metadata(backup);
            var identity = FiniteTransitionIdentity.fromSource(backup);
            identity.verifyWorld(world);
            if (!HistoricalVersions.isFiniteWorld(source.profileId().value())
                    || !source.generationPreset().value().equals(identity.sourcePreset())
                    || source.revision() != manifest.fromRevision() || manifest.toRevision() != manifest.fromRevision() + 1
                    || current.history().size() <= source.history().size()
                    || !current.history().subList(0, source.history().size()).equals(source.history())) {
                throw new IOException("Backup is not a prefix of this world's finite-to-infinite history");
            }
            var action = current.history().get(source.history().size());
            String prefix = "version upgrade to ", separator = " on generation recipe ";
            int split = action.note().indexOf(separator);
            if (action.revision() != manifest.toRevision() || !action.note().startsWith(prefix) || split < prefix.length()) {
                throw new IOException("Backup does not precede an explicit version upgrade");
            }
            String destination = action.note().substring(prefix.length(), split);
            if (HistoricalVersions.byId(destination).isEmpty() || HistoricalVersions.isFiniteWorld(destination)) {
                throw new IOException("Backup does not precede an infinite destination");
            }
            var time = WorldGenSettingsAdvance.readGameTime(backup);
            var converted = new java.util.HashSet<String>();
            var targetVersion = HistoricalVersions.byId(current.profileId().value()).orElseThrow();
            for (String nativeBiome : Set.of("moderner_beta:indev_normal", "minecraft:the_void")) {
                String mapped = BiomeCarryForward.destinationFor(nativeBiome, targetVersion);
                converted.add(mapped == null ? nativeBiome : mapped);
            }
            var evidence = new FiniteChunkEvidence(time.isPresent() ? time.getAsLong() : null, converted);
            return new Source(relative, WorldBackup.sha256(Files.readAllBytes(backup.resolve(WorldBackup.MANIFEST_FILE_NAME))), identity, evidence);
        } catch (UpgradeException failure) { throw new IOException("Backup verification failed", failure); }
    }

    private static HistoricalMetadata metadata(Path world) throws IOException {
        try { return HistoricalMetadataStore.forWorldDirectory(world).read(); }
        catch (MetadataException failure) { throw new IOException("Historical metadata is not usable repair provenance", failure); }
    }
    private static String metadataHash(Path world) throws IOException {
        return WorldBackup.sha256(Files.readAllBytes(HistoricalMetadataStore.forWorldDirectory(world).file()));
    }
    static Request read(Path world) throws IOException {
        try {
            Request request = GSON.fromJson(Files.readString(world.resolve(FILE_NAME), StandardCharsets.UTF_8), Request.class);
            if (request == null) throw new IOException("Empty finite-edge repair request");
            return request;
        } catch (RuntimeException invalid) { throw new IOException("Invalid finite-edge repair request; retained without changes", invalid); }
    }
    private static void write(Path world, Request request) throws IOException {
        Path file = world.resolve(FILE_NAME), temporary = file.resolveSibling(FILE_NAME + ".tmp");
        byte[] bytes = (GSON.toJson(request) + "\n").getBytes(StandardCharsets.UTF_8);
        try (var channel = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.WRITE)) {
            var buffer = java.nio.ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}

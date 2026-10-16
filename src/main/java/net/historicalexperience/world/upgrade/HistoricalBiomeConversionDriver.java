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

import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Converts a world's stored chunk biomes before the world is opened, with progress the player can watch.
 *
 * <h2>Where this sits</h2>
 *
 * <p>It is driven by the world-open hook, which refuses to open the storage while a conversion is outstanding. That
 * ordering is the whole point: a running server holds the region files and would write them back on its next save, so
 * the conversion has to finish first.
 *
 * <h2>Performance</h2>
 *
 * <p>One task per region file across a pool sized to the available processors, so the work is parallel; and each task
 * rewrites its own file atomically, so a failure in one region cannot corrupt another. Worlds without an outstanding
 * version-change action skip the work without scanning any region files. Completion belongs to the world on disk,
 * not to the lifetime of this driver or the progress screen.
 *
 * <h2>State</h2>
 *
 * <p>Exactly one conversion runs at a time. The counters are atomics because the pool updates them from worker
 * threads and the screen reads them from the client thread.
 */
public final class HistoricalBiomeConversionDriver {
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final AtomicInteger CHUNKS_DONE = new AtomicInteger();
    private static final AtomicInteger CHUNKS_TOTAL = new AtomicInteger();
    private static final AtomicLong ENTRIES_CHANGED = new AtomicLong();
    private static final AtomicInteger REGIONS_DONE = new AtomicInteger();
    private static final AtomicInteger REGIONS_TOTAL = new AtomicInteger();
    private static volatile String currentFile = "";
    private static volatile String detail = "";
    private static volatile String runTarget = "";
    private static volatile boolean finished;
    private static volatile boolean failed;

    private HistoricalBiomeConversionDriver() {
    }

    /**
     * The label a completed conversion is recorded under, for a given destination.
     *
     * <p>The label carries the destination's biome GENERATION, so moving a world on to a new generation converts again
     * while reopening the same world does not. Two earlier designs are recorded here because both were reported as bugs.
     * The first chose ONE destination biome from the world's version, which flattened a Beta 1.8.1 world's whole biome
     * map. The second carried each biome to its own counterpart but with no gate, so an Infdev world's placeholder went
     * straight to a Late Beta name instead of to Beta plains. The destination now decides where a biome lands; see
     * {@link BiomeCarryForward}.
     */
    public static String recordLabelFor(HistoricalVersion destination) {
        return BiomeCarryForward.recordLabelFor(destination);
    }

    /**
     * Only an explicit, persisted version change can request conversion. Opening a world, seeing an unfamiliar
     * palette, or changing the mapping implementation is not an upgrade action.
     */
    public static boolean needsConversion(Path worldDirectory, HistoricalVersion destination) {
        var action = requestedUpgrade(worldDirectory, destination);
        if (action == null) {
            return false;
        }
        long completed = BiomeConversionRecord.completedUpgradeRevision(worldDirectory);
        if (completed >= action.revision()) {
            return false;
        }
        // Preserve successful records from builds predating revision-scoped completion. Do not replay their conversion
        // merely because the app restarted or this build added a new bookkeeping field.
        return completed != 0 || BiomeConversionRecord.lastConvertedAt(worldDirectory) < action.timestamp();
    }

    static long requestedUpgradeRevision(Path worldDirectory, HistoricalVersion destination) {
        var action = requestedUpgrade(worldDirectory, destination);
        return action == null ? 0 : action.revision();
    }

    /** The most recent explicit version-change action for this destination, not a recipe-only update. */
    private static net.historicalexperience.world.metadata.MetadataRevision requestedUpgrade(
            Path worldDirectory, HistoricalVersion destination) {
        if (destination == null) {
            return null;
        }
        var state = net.historicalexperience.world.HistoricalWorldState.loadAt(worldDirectory);
        if (!(state instanceof net.historicalexperience.world.HistoricalWorldState.Historical historical)
                || !historical.metadata().profileId().value().equals(destination.id())) {
            return null;
        }
        // Existing metadata already persists this canonical action in its append-only history. Reading it here keeps
        // old saves compatible without inventing an upgrade for worlds that only have an establishment entry.
        var history = historical.metadata().history();
        for (int i = history.size() - 1; i >= 0; i--) {
            var entry = history.get(i);
            if (entry.note().startsWith("version upgrade to ")) {
                return entry.note().startsWith("version upgrade to " + destination.id() + " on generation recipe ")
                    ? entry : null;
            }
        }
        return null;
    }

    /** Whether a conversion is in flight right now. */
    public static boolean running() {
        return RUNNING.get();
    }

    /** Chunks processed so far. */
    public static int chunksDone() {
        return CHUNKS_DONE.get();
    }

    /** Chunks to process in total, known after the scan. */
    public static int chunksTotal() {
        return CHUNKS_TOTAL.get();
    }

    /** Region files processed so far. */
    public static int regionsDone() {
        return REGIONS_DONE.get();
    }

    /** Region files in the whole job. */
    public static int regionsTotal() {
        return REGIONS_TOTAL.get();
    }

    /** Biome palette entries rewritten so far. */
    public static long entriesChanged() {
        return ENTRIES_CHANGED.get();
    }

    /** The region file being worked on, for the progress screen. */
    public static String currentFile() {
        return currentFile;
    }

    /** A one-line summary of the outcome, or empty while running. */
    public static String detail() {
        return detail;
    }

    /** The biome this run converts to, so the screen names the right one instead of assuming. */
    public static String runTarget() {
        return runTarget;
    }

    /** Whether the job has finished, successfully or not. */
    public static boolean finished() {
        return finished;
    }

    /** Whether the job failed. Some regions may have completed; completion stays owed so a retry can finish. */
    public static boolean failed() {
        return failed;
    }

    /**
     * Starts the conversion on a worker pool. Returns false when one is already running or the world needs no work, so
     * the caller can fall straight through to opening the world.
     */
    public static boolean start(Path worldDirectory, HistoricalVersion destination) {
        String target = recordLabelFor(destination);
        if (target == null || !needsConversion(worldDirectory, destination)) {
            return false;
        }
        long upgradeRevision = requestedUpgradeRevision(worldDirectory, destination);
        if (!RUNNING.compareAndSet(false, true)) {
            return false;
        }
        CHUNKS_DONE.set(0);
        ENTRIES_CHANGED.set(0);
        REGIONS_DONE.set(0);
        finished = false;
        failed = false;
        detail = "";
        currentFile = "";
        runTarget = target;
        // The screen names the job, not a single biome, because each stored biome is carried to its own counterpart.

        List<Path> regions = regionFilesIn(worldDirectory);
        REGIONS_TOTAL.set(regions.size());
        CHUNKS_TOTAL.set(0);

        Thread worker = new Thread(() -> run(regions, destination, worldDirectory, upgradeRevision),
            "historical-biome-conversion");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    private static void run(List<Path> regions, HistoricalVersion destination, Path worldDirectory, long upgradeRevision) {
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads, task -> {
            Thread thread = new Thread(task, "historical-biome-conversion-worker");
            thread.setDaemon(true);
            return thread;
        });
        try (var worldLock = net.minecraft.util.DirectoryLock.create(worldDirectory)) {
            // Release only a proven finite source during this explicit upgrade, BEFORE remembering old chunk positions.
            var source = WorldGenSettingsAdvance.read(worldDirectory).orElse(null);
            if (source != null && source.consistent() && HistoricalVersions.isFinitePreset(source.settingsPreset())
                    && !HistoricalVersions.isFiniteWorld(destination.id())) {
                FiniteApronRelease.release(worldDirectory);
            }
            // Total first, so the bar is honest from the start rather than growing as work is discovered.
            int total = 0;
            java.util.Set<Long> preExistingChunks = new java.util.HashSet<>();
            for (Path region : regions) {
                // Only the overworld feeds Moderner Beta's biome source; a Nether or End chunk at the same coordinates
                // is a different chunk and must not make an overworld position look pre-existing.
                Path regionDirectory = region.getParent();
                boolean overworld = regionDirectory != null && worldDirectory.equals(regionDirectory.getParent());
                try {
                    total += HistoricalRegionBiomes.chunkPositions(region).size();
                    // Only chunks that were really generated count as "beforehand": the placeholder chunks around them
                    // have no biome yet and must stay free to generate normally.
                    if (overworld) {
                        LongList generated = HistoricalRegionBiomes.generatedChunkPositions(region);
                        preExistingChunks.addAll(generated);
                    }
                } catch (IOException unreadable) {
                    // A region that cannot be scanned contributes nothing to the total; its task will report the
                    // failure itself rather than the whole job dying here.
                    continue;
                }
            }
            CHUNKS_TOTAL.set(total);
            // Reported because a silent zero here is invisible: the conversion still rewrites the chunks, but Moderner
            // Beta is told nothing and so has no chunk set to defer to. That exact combination happened once.
            org.slf4j.LoggerFactory.getLogger("historical_experience/biome-conversion").info(
                "Biome conversion scope for {}: {} region file(s), {} pre-existing chunk(s)",
                worldDirectory, regions.size(), preExistingChunks.size());

            List<Future<?>> futures = new ArrayList<>(regions.size());
            for (Path region : regions) {
                futures.add(pool.submit(() -> {
                    currentFile = region.getFileName().toString();
                    try {
                        HistoricalRegionBiomes.RegionResult result =
                            HistoricalRegionBiomes.convert(region, destination);
                        CHUNKS_DONE.addAndGet(result.chunksRewritten());
                        ENTRIES_CHANGED.addAndGet(result.paletteEntriesChanged());
                    } catch (IOException failure) {
                        failed = true;
                    } finally {
                        REGIONS_DONE.incrementAndGet();
                    }
                }));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    failed = true;
                    break;
                } catch (java.util.concurrent.ExecutionException ignored) {
                    failed = true;
                }
            }
            detail = "Converted " + ENTRIES_CHANGED.get() + " biome entries across " + CHUNKS_DONE.get()
                + " chunks in " + REGIONS_TOTAL.get() + " region files";
            // Record it, so this conversion is not repeated. Written only on success: a record claiming a conversion
            // that did not happen would leave the world permanently unconverted.
            if (!failed) {
                String recorded = BiomeConversionRecord.record(worldDirectory, BiomeCarryForward.recordLabelFor(destination),
                    System.currentTimeMillis(), preExistingChunks, upgradeRevision);
                if (recorded == null) {
                    failed = true;
                    detail = "Could not save upgrade completion; close this screen and retry opening the world.";
                }
            } else {
                detail = "Biome conversion did not finish; close this screen and retry opening the world.";
            }
        } catch (IOException | UpgradeException | RuntimeException failure) {
            failed = true;
            detail = "Conversion failed: " + failure;
        } finally {
            pool.shutdownNow();
            finished = true;
            RUNNING.set(false);
        }
    }

    /** Every region file the overworld, Nether and End of a world directory hold, in a stable order. */
    static List<Path> regionFilesIn(Path worldDirectory) {
        List<Path> regions = new ArrayList<>();
        List<Path> directories = List.of(
            worldDirectory.resolve("region"),
            worldDirectory.resolve("DIM-1").resolve("region"),
            worldDirectory.resolve("DIM1").resolve("region"));
        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(path -> path.getFileName().toString().endsWith(".mca"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(regions::add);
            } catch (IOException unlistable) {
                continue;
            }
        }
        return regions;
    }

    /** Forgets the last run's counters. Called when a screen is opened so a stale summary is never shown. */
    public static void resetReport() {
        detail = "";
        currentFile = "";
        finished = false;
        failed = false;
        CHUNKS_DONE.set(0);
        ENTRIES_CHANGED.set(0);
        REGIONS_DONE.set(0);
    }
}

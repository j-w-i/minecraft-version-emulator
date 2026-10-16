/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import net.historicalexperience.version.HistoricalSnapshotPatches;

/** A one-shot, exact-directory new-world intent. Never consulted to relabel existing metadata. */
public final class HistoricalSnapshotCreation {
    private record Pending(Path directory, String patchId) { }
    private static final AtomicReference<Pending> PENDING = new AtomicReference<>();
    private HistoricalSnapshotCreation() { }
    public static void clear() { PENDING.set(null); }
    public static void discard(Path worldDirectory) {
        var pending = PENDING.get();
        if (pending != null && worldDirectory != null && pending.directory().equals(worldDirectory.toAbsolutePath().normalize()))
            PENDING.compareAndSet(pending, null);
    }
    public static void bind(Path newWorldDirectory, String patchId) {
        var patch = HistoricalSnapshotPatches.byId(patchId).orElseThrow(() -> new IllegalArgumentException("Unknown snapshot selection " + patchId));
        PENDING.set(new Pending(newWorldDirectory.toAbsolutePath().normalize(), patch.id()));
    }
    public static String consume(Path worldDirectory, String baseProfileId) {
        var pending = PENDING.get();
        if (pending == null || worldDirectory == null || !pending.directory().equals(worldDirectory.toAbsolutePath().normalize())) return "";
        if (!PENDING.compareAndSet(pending, null)) return "";
        HistoricalSnapshotPatches.validateAttachment(baseProfileId, pending.patchId());
        return pending.patchId();
    }
}

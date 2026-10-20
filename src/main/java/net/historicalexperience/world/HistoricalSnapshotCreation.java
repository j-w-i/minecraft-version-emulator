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

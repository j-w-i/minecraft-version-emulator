/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.version.HistoricalSnapshotPatches;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Running-world snapshot scope. Staged metadata changes take effect only after the required restart. */
public final class HistoricalSnapshotRuntime {
    private static final ConcurrentMap<MinecraftServer, String> SERVER_PATCHES = new ConcurrentHashMap<>();
    // Client prediction only. Dedicated-server enforcement always reads the per-server map above.
    private static volatile String clientPatchId = "";
    private HistoricalSnapshotRuntime() { }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var metadata = HistoricalServerLifecycle.readMetadata(HistoricalServerLifecycle.metadataFileFor(server));
            if (metadata.isEmpty() || metadata.get().snapshotPatchId().isEmpty()) return;
            String base = HistoricalProfileLookup.profileIdForLevel(server.overworld());
            if (!metadata.get().profileId().value().equals(base)) {
                throw new IllegalStateException("Snapshot metadata does not match the running overworld profile");
            }
            HistoricalSnapshotPatches.validateAttachment(base, metadata.get().snapshotPatchId());
            SERVER_PATCHES.put(server, metadata.get().snapshotPatchId());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVER_PATCHES::remove);
        SnapshotCreativeRequest.register();
    }

    public static String activePatchId(ServerLevel level) {
        return level == null ? "" : activePatchId(level.getServer());
    }
    public static String activePatchId(MinecraftServer server) {
        return server == null ? "" : SERVER_PATCHES.getOrDefault(server, "");
    }
    public static String selectionId(MinecraftServer server, String baseProfileId) {
        return HistoricalSnapshotPatches.byId(activePatchId(server))
            .filter(patch -> patch.baseProfileId().equals(baseProfileId))
            .map(HistoricalSnapshotPatches.Patch::id).orElse(baseProfileId);
    }
    public static String clientPatchId() { return clientPatchId; }
    public static void setClientSelection(String selectionId) {
        clientPatchId = HistoricalSnapshotPatches.byId(selectionId).map(HistoricalSnapshotPatches.Patch::id).orElse("");
    }
}

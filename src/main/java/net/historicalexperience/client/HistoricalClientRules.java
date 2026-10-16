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
package net.historicalexperience.client;

import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.client.Minecraft;

/**
 * Which historical version a CLIENT is currently in, so client-side presentation can follow the rules.
 *
 * <h2>Why this was missing</h2>
 *
 * <p>Every rule this project implemented was enforced on the server, and the client was left alone. That
 * made the rules invisible: hunger never drained but the hunger bar still drew, sprinting was refused for
 * a {@code ServerPlayer} while the client's own player sprinted regardless, and the game looked modern
 * whatever the rules said. The complaints that followed were correct.
 *
 * <h2>Where the answer comes from</h2>
 *
 * <p>In single-player the client asks the integrated server, which is the same authority the server-side
 * enforcement uses, so the two can never disagree. On a multiplayer server the client has no world
 * metadata of its own, so the version is pushed to it when it joins - see {@link #applySyncedVersion}.
 * Before that push arrives, or on a server that does not send it, the answer is "unknown" and nothing is
 * suppressed: a client that guessed would show one thing and be told another by the server.
 */
public final class HistoricalClientRules {
    private static volatile String syncedVersionId;

    private HistoricalClientRules() {
    }

    /** Records the version the server says this client is in. Called when the server's push arrives. */
    public static void applySyncedVersion(String versionId) {
        syncedVersionId = net.historicalexperience.version.HistoricalSnapshotPatches.baseProfileForSelection(versionId).orElse(null);
        net.historicalexperience.server.HistoricalSnapshotRuntime.setClientSelection(versionId);
        // Snapshot quirks are separate: all existing gameplay tables still receive the parent's profile.
        HistoricalActiveSession.set(syncedVersionId);
    }

    /** Forgets a pushed version, so a client that leaves a server stops applying that server's rules. */
    public static void clearSyncedVersion() {
        syncedVersionId = null;
        net.historicalexperience.server.HistoricalSnapshotRuntime.setClientSelection(null);
        HistoricalActiveSession.clear();
    }

    /** Exact snapshot identity, if selected; this must never be inferred from the parent's release alone. */
    public static String activeSnapshotPatchId() {
        var minecraft = Minecraft.getInstance();
        if (minecraft == null) return "";
        var server = minecraft.getSingleplayerServer();
        return server != null ? net.historicalexperience.server.HistoricalSnapshotRuntime.activePatchId(server)
            : net.historicalexperience.server.HistoricalSnapshotRuntime.clientPatchId();
    }

    /** The parent's historical rules, or null when the server has made no historical claim. */
    public static HistoricalVersion activeVersion() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        var server = minecraft.getSingleplayerServer();
        if (server != null) {
            // Single-player: the integrated server is authoritative and is right here in this process.
            String versionId = HistoricalProfileLookup.profileIdForLevel(server.overworld());
            if (versionId != null) {
                return HistoricalVersions.byId(versionId).orElse(null);
            }
        }
        String pushed = syncedVersionId;
        return pushed == null ? null : HistoricalVersions.byId(pushed).orElse(null);
    }
}

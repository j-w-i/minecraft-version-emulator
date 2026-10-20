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
package net.historicalexperience.gameplay;

import java.util.Map;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.minecraft.world.level.storage.LevelData;

/**
 * Whether a loaded level may be in hardcore mode at all.
 *
 * <p>research/version-transitions.json, Beta 1.9 Prerelease 2: "Hardcore mode (locks difficulty to Hard, permadeath)".
 * A level whose stored flag says hardcore, loaded as an earlier build, must read as non-hardcore - the flag is
 * preserved in the file, only the answer changes.
 *
 * <h2>Why a note at server start rather than a lookup in the mixin</h2>
 *
 * <p>{@code PrimaryLevelData} has no server, no level and no generator to ask, and on a dedicated server the active
 * session is empty, so {@code Features.active(..., ActiveVersion.current())} cannot decide. The lifecycle already
 * resolves the world's profile at {@code SERVER_STARTING}; it records the answer here, and the mixin asks this map.
 * The map is keyed by the level-data instance itself, so two servers in one JVM cannot answer for each other.
 */
public final class HistoricalHardcore {
    private static final Map<LevelData, Boolean> GATED = new java.util.concurrent.ConcurrentHashMap<>();

    private HistoricalHardcore() {
    }

    /** Records whether this loaded level's build had hardcore; null or unknown profiles are not gated. */
    public static void note(LevelData data, String profileId) {
        if (data == null) {
            return;
        }
        boolean gated = Features.active(Feature.NO_HARDCORE, profileId);
        net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
            "Historical hardcore gate: profile={} gated={} stored={}", profileId, gated, data.isHardcore());
        GATED.put(data, gated);
    }

    /** True when this loaded level belongs to a build that predates hardcore. */
    public static boolean gated(LevelData data) {
        return data != null && Boolean.TRUE.equals(GATED.get(data));
    }

    /** Forgets every note; called when a server starts so a stale mapping cannot survive a world change. */
    public static void clear() {
        GATED.clear();
    }
}

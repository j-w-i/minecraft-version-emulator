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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.server.level.ServerLevel;

/**
 * Weather did not exist before Beta 1.5, so those worlds never rain, snow or thunder.
 *
 * <h2>Where the boundary comes from</h2>
 *
 * <p>{@code research/minecraft-version-history.md}'s Beta 1.5 entry states it four separate ways, which is why
 * this is implemented rather than guessed:
 * <ul>
 *   <li>item 4, World generation: <i>"Natural rain, snowfall and thunderstorms; no new base generator."</i></li>
 *   <li>item 10, Rendering/audio: <i>"Release implements rain, snow, thunderstorms and unique rain sounds."</i></li>
 *   <li>item 14, the delta from the previous build: <i>"Difference from Beta 1.4: Weather,
 *       statistics/achievements, powered/detector rails, cobweb and saplings, charged creeper, lightning
 *       transformations."</i> Weather being listed as a <b>difference from Beta 1.4</b> is the boundary itself.</li>
 *   <li>item 15, the recreation notes, which address this project directly: <i>"no modern weather
 *       defaults"</i>.</li>
 * </ul>
 *
 * <p>So every declared version older than Beta 1.5 — Alpha, both Infdev builds and Beta 1.0 through 1.4 — had
 * clear skies permanently, and that includes the lightning that Beta 1.5 ties to weather (charged creepers, pigs
 * becoming zombie pigmen, fires started by strikes).
 *
 * <h2>Why this is enforced rather than merely recorded</h2>
 *
 * <p>A version registry that lists weather as Beta 1.5 content while the world still rains is exactly the kind of
 * claim this project's coverage matrix exists to prevent: the entry would read as implemented while a player
 * standing in an Alpha world watches it rain. The registry entry being right is not the same as the behaviour
 * being right.
 */
public final class HistoricalWeather {
    private HistoricalWeather() {
    }

    /** Whether weather exists in the given version. Pure, so the boundary can be tested without a world. */
    public static boolean existsIn(HistoricalVersion version) {
        if (version == null) {
            // An unresolvable version is not a licence to change the weather; leave the world as it is.
            return true;
        }
        return !Features.active(Feature.NO_WEATHER, version);
    }

    /** Whether weather exists in the world's version, resolved from the level's own generator metadata. */
    public static boolean existsFor(ServerLevel level) {
        if (level == null) {
            return true;
        }
        String versionId = HistoricalProfileLookup.profileIdForLevel(level);
        if (versionId == null) {
            // An ordinary, non-historical world has weather. So does a historical world whose metadata could not
            // be read: refusing weather on a failed lookup would silently change worlds this project does not own.
            return true;
        }
        return HistoricalVersions.byId(versionId).map(HistoricalWeather::existsIn).orElse(true);
    }

    /** Whether weather must be prevented, which is the only state older than Beta 1.5 ever had. */
    public static boolean mustStayClear(ServerLevel level) {
        return !existsFor(level);
    }
}

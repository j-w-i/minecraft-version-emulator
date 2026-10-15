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
package net.historicalexperience.client.config;

import net.historicalexperience.client.lighting.HistoricalClientOptions;

/**
 * Whether the player has asked to see this project's developer-facing output.
 *
 * <h2>What it gates, and why those things are separate from the feature</h2>
 *
 * <p>Several surfaces answer questions only whoever maintains this mod asks: which generation recipe a version uses,
 * what a world's document revision is, where the backup was written and what its manifest hashed to, and which
 * specific build a profile was measured against. Those are the right things to show while developing and the wrong
 * things to show a player who only wanted to play Beta 1.7.3, so they are behind this switch rather than being
 * removed - the information is still one click away when it is needed.
 *
 * <p><b>Off by default.</b> A player must not have to opt out of output written for someone else, and the default is
 * also the safe direction: a switch that defaults on would leak the addresses and identifiers into every screenshot
 * and bug report taken before anyone thought about it.
 *
 * <p>The value lives in this project's own client properties file, beside the other client options, and is read
 * through the same cache - so the switch takes effect immediately in-game and needs no restart.
 */
public final class HistoricalDeveloperMode {
    /** The properties key. Deliberately plain: a player may edit this file by hand. */
    private static final String KEY = "developerMode";

    private HistoricalDeveloperMode() {
    }

    /** Whether developer-facing output should be shown. Off unless the player has turned it on. */
    public static boolean enabled() {
        return HistoricalClientOptions.get(KEY, false);
    }

    public static void setEnabled(boolean enabled) {
        HistoricalClientOptions.set(KEY, enabled);
    }
}

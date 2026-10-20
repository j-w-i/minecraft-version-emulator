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
package net.historicalexperience.client.textures;

import net.historicalexperience.client.lighting.HistoricalClientOptions;

/**
 * The persisted historical-texture settings, in the mod's own client properties file.
 *
 * <p>These live beside the mod's other client options and nowhere near {@code options.txt}: the player's resource-pack
 * selection is never read for these settings and never written by them.
 */
public final class HistoricalTextureConfig {
    /** Automatic historical textures. On by default: the point of the feature is that nothing needs configuring. */
    private static final String ENABLED_KEY = "historicalTextures";
    private static final String SETUP_KEY = "historicalTexturesSetupVersion";

    /**
     * Bumped when the setup screen's requirements change enough that a previous acknowledgement no longer covers them
     * (for example a new dependency), so existing players are asked once about the new thing and not before.
     */
    public static final int SETUP_VERSION = 1;

    private HistoricalTextureConfig() {
    }

    public static boolean enabled() {
        return HistoricalClientOptions.get(ENABLED_KEY, true);
    }

    public static void setEnabled(boolean enabled) {
        HistoricalClientOptions.set(ENABLED_KEY, enabled);
    }

    /**
     * Fetch missing packs automatically from Modrinth, at the exact pinned versions. On by default; the setup screen
     * says so before it happens, and this switch turns it off for players who would rather install the packs by hand.
     */
    private static final String AUTO_DOWNLOAD_KEY = "historicalTexturesAutoDownload";

    public static boolean autoDownload() {
        return HistoricalClientOptions.get(AUTO_DOWNLOAD_KEY, true);
    }

    public static void setAutoDownload(boolean enabled) {
        HistoricalClientOptions.set(AUTO_DOWNLOAD_KEY, enabled);
    }

    /** True once the player has been through (or skipped) the setup screen at this version or later. */
    public static boolean setupCompleted() {
        return storedSetupVersion() >= SETUP_VERSION;
    }

    public static void markSetupCompleted() {
        HistoricalClientOptions.setString(SETUP_KEY, Integer.toString(SETUP_VERSION));
    }

    private static int storedSetupVersion() {
        try {
            return Integer.parseInt(HistoricalClientOptions.getString(SETUP_KEY, "0").trim());
        } catch (NumberFormatException corrupt) {
            // A corrupt marker means "not completed": showing the screen once more is the safe direction.
            return 0;
        }
    }
}

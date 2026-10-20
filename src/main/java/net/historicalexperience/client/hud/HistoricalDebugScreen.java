/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.client.hud;

import java.util.ArrayList;
import java.util.List;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;

/**
 * The debug overlay's version-dependent lines, kept out of the mixin so the filtering is testable without a client.
 *
 * <p>minecraft.wiki, Debug screen history: "v1.2.3: The coordinates have been added to the debug screen."
 */
public final class HistoricalDebugScreen {
    private HistoricalDebugScreen() {
    }

    /** True when the overlay must not show coordinates in this version. */
    public static boolean hidesCoordinates(String versionId) {
        return Features.active(Feature.NO_DEBUG_COORDINATES, versionId);
    }

    /**
     * The same lines with the coordinate readout removed when the version predates it.
     *
     * <p>The modern line begins with the literal {@code XYZ:}; the debug overlay's labels are not translated, so this
     * is the same string a player sees. Filtering by prefix rather than by index means an unrelated added or removed
     * line cannot shift the result.
     */
    public static List<String> filter(String versionId, List<String> lines) {
        if (lines == null || !hidesCoordinates(versionId)) {
            return lines;
        }
        List<String> kept = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line != null && line.stripLeading().startsWith("XYZ:")) {
                continue;
            }
            kept.add(line);
        }
        return kept;
    }
}

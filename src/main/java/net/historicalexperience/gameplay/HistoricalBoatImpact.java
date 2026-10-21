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
package net.historicalexperience.gameplay;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;

/**
 * Whether a boat hitting land at speed should break.
 *
 * <p>minecraft.wiki, Boat history: "v1.0.6_01 Boats now only break on impact with land at high speeds." The wiki gives
 * no number, so {@link #HIGH_SPEED} is an APPROXIMATION chosen as the horizontal speed at which a boat is clearly no
 * longer merely nudging the shore; it is documented rather than presented as a quoted value, and the runtime probe
 * (summon a boat with that motion on land) is the check that the rule fires at all.
 */
public final class HistoricalBoatImpact {
    /** Blocks per tick regarded as "high speed". An approximation: the wiki states no figure. */
    public static final double HIGH_SPEED = 0.5D;

    private HistoricalBoatImpact() {
    }

    /** Whether this version breaks a boat that hits land at speed. */
    public static boolean breaksOnLandImpact(String versionId) {
        return Features.active(Feature.BOAT_LAND_IMPACT_BREAKS, versionId);
    }

    /**
     * Whether a boat moving at this horizontal speed into land must break in this version.
     *
     * <p>{@code landAhead} rather than "the boat is not in water": a boat running aground is almost always still
     * floating (its bounding box is in the water it left), so the collision target is what says "land".
     */
    public static boolean shouldBreak(String versionId, boolean landAhead, boolean horizontalCollision,
            double horizontalSpeed) {
        return landAhead
            && horizontalCollision
            && horizontalSpeed >= HIGH_SPEED
            && breaksOnLandImpact(versionId);
    }
}

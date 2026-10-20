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
package net.historicalexperience.version;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The question every historical rule asks: <em>is this feature active here?</em>
 *
 * <p>There are two forms. {@link #active(Feature)} answers for the world the game is currently in, using the
 * {@link ActiveVersion} that the server (and, on a client, the server's push) sets. {@link #active(Feature, String)}
 * and {@link #active(Feature, HistoricalVersion)} answer for a named version, which is what tests, reports and any
 * rule that already knows its world should use.
 *
 * <p>All forms answer {@code false} for a world with no declared version, for {@code null} and for an unknown id.
 * That is the ordinary-world guarantee, and it is why every {@link Feature} is a <em>deviation</em> from modern
 * behaviour.
 *
 * <h2>Cost</h2>
 *
 * <p>The per-version {@link FeatureSet} is built once and cached. {@link #active(Feature)} is one volatile read, one
 * null check and one array read, so it is safe on per-tick and per-block paths. Nothing here allocates after a version
 * has been seen once, and nothing runs during class initialisation, which is what keeps {@link Feature} free of the
 * registry's initialisation order.
 */
public final class Features {
    private static final Map<String, FeatureSet> BY_VERSION = new ConcurrentHashMap<>();

    private Features() {
    }

    /** Whether the feature is active in the world the game is currently in. */
    public static boolean active(Feature feature) {
        return ActiveVersion.features().has(feature);
    }

    /** Whether the feature is active for the declared version with this id; false for null and unknown ids. */
    public static boolean active(Feature feature, String versionId) {
        return setFor(versionId).has(feature);
    }

    /** Whether the feature is active for this version; false for null. */
    public static boolean active(Feature feature, HistoricalVersion version) {
        return version != null && setFor(version.id()).has(feature);
    }

    /**
     * The resolved features of a version id. Unknown ids resolve to {@link FeatureSet#NONE} and are not cached, so an
     * arbitrary string cannot grow the cache.
     */
    static FeatureSet setFor(String versionId) {
        if (versionId == null) {
            return FeatureSet.NONE;
        }
        FeatureSet cached = BY_VERSION.get(versionId);
        if (cached != null) {
            return cached;
        }
        return HistoricalVersions.byId(versionId)
            .map(version -> BY_VERSION.computeIfAbsent(versionId, id -> FeatureSet.resolve(version)))
            .orElse(FeatureSet.NONE);
    }
}
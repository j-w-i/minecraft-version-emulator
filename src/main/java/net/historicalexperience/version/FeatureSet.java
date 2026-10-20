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

/**
 * The features one version has, resolved once and immutable.
 *
 * <p>This is what makes the runtime check cheap: asking a version whether a feature is active is one array read,
 * with no window arithmetic, no string comparison and no allocation. The array is private and never exposed, so a
 * set cannot be changed after it is built. {@code ordinal()} indexes memory here only; it is never persisted and
 * never used to order history.
 */
public final class FeatureSet {
    /** The set of a world with no historical version: nothing is active. */
    public static final FeatureSet NONE = new FeatureSet(null, Integer.MIN_VALUE, new boolean[Feature.values().length]);

    private final String versionId;
    private final int key;
    private final boolean[] active;

    private FeatureSet(String versionId, int key, boolean[] active) {
        this.versionId = versionId;
        this.key = key;
        this.active = active;
    }

    /** Resolves every feature for a declared version. */
    static FeatureSet resolve(HistoricalVersion version) {
        Feature[] all = Feature.values();
        boolean[] active = new boolean[all.length];
        for (Feature feature : all) {
            active[feature.ordinal()] = feature.appliesTo(version.id(), version.chronology());
        }
        return new FeatureSet(version.id(), version.chronology(), active);
    }

    /** Whether the feature is active for this version. */
    public boolean has(Feature feature) {
        return this.active[feature.ordinal()];
    }

    /** True when this set was resolved for exactly this version id and key. */
    boolean isFor(HistoricalVersion version) {
        return this.key == version.chronology() && version.id().equals(this.versionId);
    }

    /** The id of the version this set was resolved for, or null for {@link #NONE}. */
    public String versionId() {
        return this.versionId;
    }
}
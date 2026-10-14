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

/**
 * A half-open range of the project's chronology key, {@code [fromInclusive, toExclusive)}.
 *
 * <p>Boundaries are expressed against {@code HistoricalVersions.chronology(...)} and never against a version name, so
 * a snapshot, prerelease or intermediate build is placed by when it is rather than by what its string looks like.
 */
public record VersionWindow(int fromInclusive, int toExclusive) {
    public static final VersionWindow ALWAYS = new VersionWindow(Integer.MIN_VALUE, Integer.MAX_VALUE);

    public VersionWindow {
        if (fromInclusive > toExclusive) {
            throw new IllegalArgumentException("empty window " + fromInclusive + ".." + toExclusive);
        }
    }

    public static VersionWindow before(int toExclusive) {
        return new VersionWindow(Integer.MIN_VALUE, toExclusive);
    }

    public static VersionWindow from(int fromInclusive) {
        return new VersionWindow(fromInclusive, Integer.MAX_VALUE);
    }

    public boolean contains(int chronology) {
        return chronology >= fromInclusive && chronology < toExclusive;
    }
}

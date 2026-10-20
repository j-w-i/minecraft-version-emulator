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
 * The historical families this project emulates, and the base of each one's position on the shared timeline.
 *
 * <p>The base is what keeps every build comparable with every other: a version's chronology key is its era's base
 * plus a build-specific number, so no Infdev build can ever sort after an Alpha build whatever it is called. The
 * numbers are the keys the project has always used, so nothing that compares them changes.
 */
public enum Era {
    INDEV(200_000),
    INFDEV(300_000),
    ALPHA(400_000),
    BETA(500_000),
    RELEASE(600_000);

    private final int base;

    Era(int base) {
        this.base = base;
    }

    /**
     * The family a chronology key belongs to: the era with the greatest base not above it. A key below every base
     * (nothing declared is) falls into {@link #INDEV}.
     */
    public static Era of(int chronology) {
        Era found = INDEV;
        for (Era era : values()) {
            if (chronology >= era.base) {
                found = era;
            }
        }
        return found;
    }

    /** The first chronology key of this era. */
    public int base() {
        return this.base;
    }

    /**
     * The key of a build whose number is an ordinary {@code major.minor.patch}: {@code base + major*1000 +
     * minor*100 + patch}. Builds that are not (date-labelled Indev and Infdev, lettered Alpha builds) give their key
     * explicitly instead; see {@link Milestone#ofKey}.
     */
    public int key(int major, int minor, int patch) {
        return this.base + major * 1000 + minor * 100 + patch;
    }
}
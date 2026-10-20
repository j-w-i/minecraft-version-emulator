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
 * A named position on the historical timeline: a stable comparison key and a human label.
 *
 * <h2>Why this is a value and not an enum of every build</h2>
 *
 * <p>Boundaries are of two kinds: builds this project declares, and builds it does not (Beta 1.8, Release 1.4.2,
 * the synthetic "just after Beta 1.8.1"). An enum of declared builds would add another edit site per new version,
 * which is the problem this model removes. A milestone is therefore just a key, and the registry is tied to the
 * keys by a test rather than by shared code.
 *
 * <h2>Comparison rules</h2>
 *
 * <ul>
 *   <li>Order is the numeric {@link #key()}. {@code ordinal()} is never used for history.</li>
 *   <li>A key is <b>not</b> an identity. Two different builds may in principle share one; behaviour that belongs to
 *       exactly one build is expressed with {@link FeatureWindow#exactly}, which compares the version id.</li>
 *   <li>Keys are the project's existing chronology numbers, including the irregular ones (Indev {@code 200223},
 *       Infdev {@code 300420}/{@code 300611}, Alpha v1.1.2_01 {@code 401102}, Alpha 1.2.2a {@code 401202}). They
 *       are compared but not persisted.</li>
 * </ul>
 *
 * @param key   the position on the timeline; larger is later
 * @param label what the position is, for reports and documentation
 */
public record Milestone(int key, String label) {
    public Milestone {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("a milestone needs a label");
        }
    }

    /** A build with an ordinary {@code major.minor.patch} number. */
    public static Milestone of(Era era, int major, int minor, int patch, String label) {
        return new Milestone(era.key(major, minor, patch), label);
    }

    /** A build whose key cannot be derived from its number, such as a date-labelled one. */
    public static Milestone ofKey(int key, String label) {
        return new Milestone(key, label);
    }

    /**
     * The position immediately after another milestone: "up to and including" a build, expressed as a boundary.
     * Used where a rule ends after a build that may later gain neighbours.
     */
    public Milestone after() {
        return new Milestone(this.key + 1, "just after " + this.label);
    }

    /** True when this position is strictly earlier than {@code other}. */
    public boolean isBefore(Milestone other) {
        return this.key < other.key;
    }
}
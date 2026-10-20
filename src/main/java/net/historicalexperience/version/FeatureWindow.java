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
 * Where on the timeline a {@link Feature} is active.
 *
 * <p>Three shapes cover every behaviour the project models, and they are deliberately not a general expression
 * language: a window is a decision about <em>one</em> version, made by key or by identity, and nothing else.
 *
 * <ul>
 *   <li>{@link Range} - the half-open interval {@code [from, until)} of keys, either end open. Most historical
 *       differences are this: "before Beta 1.8", "from Alpha 1.1.0", "from Beta 1.8 until Release 1.6.4".</li>
 *   <li>{@link Exact} - exactly one declared build, compared by <b>id</b>. For behaviour that existed in one
 *       development build and nowhere else (a prerelease bug). A key is not an identity, so a numeric interval one
 *       unit wide would be the wrong tool.</li>
 *   <li>{@link AllDeclared} - every declared version. For behaviour the modern game has and no declared version
 *       had, where the true upper boundary lies beyond the newest declared build.</li>
 * </ul>
 *
 * <p>A feature may list several windows; it is active when any contains the version (the corner version label,
 * for example, appears, disappears and reappears).
 */
public sealed interface FeatureWindow {
    /** Whether a declared version with this id and key is inside the window. */
    boolean contains(String versionId, int key);

    /** The interval {@code [from, until)}; a null end is open. */
    record Range(Milestone from, Milestone until) implements FeatureWindow {
        public Range {
            if (from != null && until != null && until.key() < from.key()) {
                throw new IllegalArgumentException("empty window: " + from.label() + " .. " + until.label());
            }
        }

        @Override
        public boolean contains(String versionId, int key) {
            return (this.from == null || key >= this.from.key())
                && (this.until == null || key < this.until.key());
        }
    }

    /** Exactly the declared build with this id. */
    record Exact(String id) implements FeatureWindow {
        public Exact {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("an exact window needs a version id");
            }
        }

        @Override
        public boolean contains(String versionId, int key) {
            return this.id.equals(versionId);
        }
    }

    /** Every declared version. */
    record AllDeclared() implements FeatureWindow {
        @Override
        public boolean contains(String versionId, int key) {
            return true;
        }
    }

    /** Every declared version older than {@code until}. */
    static FeatureWindow before(Milestone until) {
        return new Range(null, until);
    }

    /** Every declared version at or after {@code from}. */
    static FeatureWindow from(Milestone from) {
        return new Range(from, null);
    }

    /** Every declared version in {@code [from, until)}. */
    static FeatureWindow between(Milestone from, Milestone until) {
        return new Range(from, until);
    }

    /** Exactly one declared build. */
    static FeatureWindow exactly(String versionId) {
        return new Exact(versionId);
    }

    /** Every declared version. */
    static FeatureWindow allDeclared() {
        return new AllDeclared();
    }
}
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
 * Which historical version the running game is currently showing, if any.
 *
 * <h2>Why this exists rather than reading a level</h2>
 *
 * <p>Most rules that have a world to hand resolve the version from it, which is correct whenever the question is
 * about a world. Some questions are not about a world at all: an item's attack damage modifier is a property of the
 * item stack, shared by the whole session, with no level anywhere in reach. Those ask this holder, which is
 * per-world from the player's point of view (one world is loaded at a time) and never touches an ordinary world,
 * because an ordinary world never sets it.
 *
 * <h2>Ownership</h2>
 *
 * <p>Set on the server when a player joins a world whose version resolved ({@code HistoricalVersionSync}), on a client
 * when the server pushes its version ({@code HistoricalClientRules}), and cleared when the world goes away. The
 * verification harnesses set and restore it around their own checks. Nothing else writes it.
 *
 * <p>The id and its resolved {@link FeatureSet} are published together as one immutable object through a single
 * volatile, so a reader can never see the id of one version with the features of another.
 */
public final class ActiveVersion {
    private record Active(String id, FeatureSet features) {
    }

    private static final Active NONE = new Active(null, FeatureSet.NONE);

    private static volatile Active active = NONE;

    private ActiveVersion() {
    }

    /** Records the active version. A null or blank id is treated as "no historical world". */
    public static void set(String id) {
        active = (id == null || id.isBlank()) ? NONE : new Active(id, Features.setFor(id));
    }

    /** Forgets the active version. Called when leaving a world or stopping a server. */
    public static void clear() {
        active = NONE;
    }

    /** The active version id, or null when the game is not in a historical world. */
    public static String current() {
        return active.id();
    }

    /** Whether a historical version is active. */
    public static boolean isHistorical() {
        return active.id() != null;
    }

    /** The resolved features of the active version; {@link FeatureSet#NONE} for an ordinary world. */
    static FeatureSet features() {
        return active.features();
    }
}
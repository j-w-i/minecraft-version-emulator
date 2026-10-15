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
package net.historicalexperience.client.config;

import net.historicalexperience.version.HistoricalVersion;

/**
 * Where a player's option does or does not apply, for the text on its config row.
 *
 * <h2>Why an option is never hidden or greyed for this</h2>
 *
 * <p>These options are <b>preferences</b>, and the version decides only whether the thing they control is used. The
 * first design greyed a row out when the world being played did not offer the feature, and refused to store the
 * change; the owner then reported that turning old lighting and old smooth lighting on or off "does not update in the
 * game", including after reopening the world. It did not: the write was discarded whenever no suitable world was
 * open, which is the usual state when Mod Menu is opened.
 *
 * <p>So every row is clickable at all times, the setting is always stored, and the row's own text says which worlds
 * it will be used in. That is also the honest description of what the setting is: one preference that applies inside
 * the window it belongs to, not a value that happens to be remembered per world.
 *
 * <p>Pure by construction - it takes a version and a predicate answer - so the labels can be asserted without a
 * running game.
 */
public enum OptionScope {
    /** The world being played is inside the window: the setting is in use right now. */
    APPLIES,
    /** A historical world is open, but this build's version never had the feature. */
    NOT_IN_THIS_VERSION,
    /** No historical world is open, so nothing can be said about the current world. */
    NO_VERSION;

    public static OptionScope of(HistoricalVersion version, boolean availableForThatVersion) {
        if (version == null) {
            return NO_VERSION;
        }
        return availableForThatVersion ? APPLIES : NOT_IN_THIS_VERSION;
    }

    /** The language-file suffix this scope's sentence is stored under. */
    public String keySuffix() {
        return switch (this) {
            case APPLIES -> "applies";
            case NOT_IN_THIS_VERSION -> "not_in_this_version";
            case NO_VERSION -> "no_version";
        };
    }
}

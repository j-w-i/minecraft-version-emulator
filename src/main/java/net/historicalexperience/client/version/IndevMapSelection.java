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
package net.historicalexperience.client.version;

import net.historicalexperience.worldgen.IndevMapSettings;

/**
 * The Indev map choice for the world being created, remembered while the create-world screen is rebuilt.
 *
 * <p>The same reason {@link CreateWorldHistoricalVersion.Selection} exists: opening game rules, data packs or the
 * customisation screen destroys and rebuilds the create-world screen, and a choice that lived only in a widget would
 * be lost on the way back. The screen is the view; this is the state.
 */
public final class IndevMapSelection {
    private static IndevMapSettings.Choice choice = IndevMapSettings.DEFAULT;
    private static boolean chosen;

    private IndevMapSelection() {
    }

    /** The choice the next Indev world is created with. Defaults to what the project already generates. */
    public static IndevMapSettings.Choice choice() {
        return choice;
    }

    /** Whether the player has actually confirmed a choice, i.e. whether the creation context needs our modifier. */
    public static boolean chosen() {
        return chosen;
    }

    static void choose(IndevMapSettings.Choice newChoice) {
        choice = newChoice == null ? IndevMapSettings.DEFAULT : newChoice;
        chosen = true;
    }

    /** Forgets the choice, so the next world creation does not inherit it. */
    public static void clear() {
        choice = IndevMapSettings.DEFAULT;
        chosen = false;
    }
}

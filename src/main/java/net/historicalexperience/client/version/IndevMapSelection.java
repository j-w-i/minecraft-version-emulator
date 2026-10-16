/* SPDX-License-Identifier: LGPL-3.0-or-later */
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

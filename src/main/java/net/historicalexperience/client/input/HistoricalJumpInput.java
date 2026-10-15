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
package net.historicalexperience.client.input;

/**
 * Remembers that the key currently held down has already jumped, for the one build that removed held-jump repeat.
 *
 * <h2>What the state is</h2>
 *
 * <p>One flag, meaning "a jump has happened since the jump key was last released". While it is set the jump input is
 * suppressed, so the player lands and stays there until they let go and press again - which is exactly what
 * minecraft.wiki describes for Beta 1.9-pre1: <i>"Continuous jumping by holding down the jump key is no longer
 * possible."</i> See {@code HistoricalGameplayTweaks.jumpRequiresFreshPress}.
 *
 * <h2>Why it is not a tick counter or a cooldown</h2>
 *
 * <p>A cooldown would allow repeated jumps at a rate; the behaviour being reproduced allows exactly one jump per
 * press, however long the player holds the key. A latch keyed on the key state is that behaviour and nothing else.
 *
 * <h2>Why it is reset, and where</h2>
 *
 * <p>It is client input state with no owner in the world, so it is cleared when the player leaves a world - otherwise
 * a jump they made in one world could suppress the first jump in the next. It is trivially reset by
 * {@link #clear()}, which the client initialiser calls on disconnect.
 */
public final class HistoricalJumpInput {
    private static volatile boolean jumpedSincePress;

    private HistoricalJumpInput() {
    }

    /** Whether the key being held has already produced its jump. */
    public static boolean alreadyJumped() {
        return jumpedSincePress;
    }

    /** Records that the key's jump has been used; the caller suppresses further jumps until the key is released. */
    public static void markJumped() {
        jumpedSincePress = true;
    }

    /** The key is up, so the next press may jump again. */
    public static void allowNextPress() {
        jumpedSincePress = false;
    }

    /** Forgets the state entirely, for a disconnect or a world change. */
    public static void clear() {
        jumpedSincePress = false;
    }
}

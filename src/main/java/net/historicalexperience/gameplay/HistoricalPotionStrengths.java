/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.gameplay;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;

/**
 * The two potion strengths Release 1.6.1 reduced, as constants the effect mixins ask for.
 *
 * <p>minecraft.wiki, Java Edition 1.6.1: "Instant Health nerfed by 33% (heals only 4 health points/2 hearts at level I).
 * Regeneration nerfed by 50% (takes 50 ticks/2.5 seconds to restore 1 health point/.5 heart at level I)."
 *
 * <p>So before 1.6.1: Instant Health's per-level base is 6 rather than 4, and Regeneration's interval base is 25 ticks
 * rather than 50. Both values live here, pure and unit-tested, rather than as literals inside the mixins.
 */
public final class HistoricalPotionStrengths {
    /** Modern Instant Health base (4 at level I); pre-1.6.1 is 6. */
    public static final int MODERN_INSTANT_HEALTH = 4;
    public static final int PRE_1_6_1_INSTANT_HEALTH = 6;

    /** Modern Regeneration interval base (50 ticks); pre-1.6.1 is 25. */
    public static final int MODERN_REGENERATION_TICKS = 50;
    public static final int PRE_1_6_1_REGENERATION_TICKS = 25;

    private HistoricalPotionStrengths() {
    }

    private static boolean gated() {
        return Features.active(Feature.PRE_1_6_1_POTION_STRENGTHS, HistoricalServerProfile.active());
    }

    /** The Instant Health base to use: 6 before Release 1.6.1, the modern 4 after. */
    public static int instantHealthBase(int modernBase) {
        return gated() ? PRE_1_6_1_INSTANT_HEALTH : modernBase;
    }

    /** The Regeneration interval base: 25 ticks before Release 1.6.1, the modern 50 after. */
    public static int regenerationTickBase(int modernBase) {
        return gated() ? PRE_1_6_1_REGENERATION_TICKS : modernBase;
    }
}

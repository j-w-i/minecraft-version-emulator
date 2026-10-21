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

import java.util.Set;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;

/**
 * The two potions Release 1.4.2 introduced, as brew results an earlier build must not produce.
 *
 * <p>minecraft.wiki, Java Edition 1.4.2 additions: "Potion of Night Vision - Brewed by adding a golden carrot to an
 * awkward potion"; "Potion of Invisibility - Brewed by adding a fermented spider eye to a potion of Night Vision".
 */
public final class HistoricalPotionMixes {
    public static final Set<String> RELEASE_1_4_2_POTIONS = Set.of(
        "minecraft:night_vision",
        "minecraft:long_night_vision",
        "minecraft:invisibility",
        "minecraft:long_invisibility");

    private HistoricalPotionMixes() {
    }

    /** Whether brewing this potion must be refused in this version. */
    public static boolean refuseBrew(String potionId, String versionId) {
        return potionId != null
            && RELEASE_1_4_2_POTIONS.contains(potionId)
            && Features.active(Feature.NO_1_4_2_POTIONS, versionId);
    }
}

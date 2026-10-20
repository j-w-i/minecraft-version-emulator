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

import java.util.List;

import net.historicalexperience.version.Feature;

/**
 * Pressure plates before Beta 1.3 take THREE blocks, not two.
 *
 * <p>minecraft.wiki, Pressure Plate history, Beta 1.3: "Pressure plates now require only two wood planks/stone to craft;
 * previous versions required three (this recipe now creates slabs)." So Alpha v1.0.1 through Beta 1.2 use a
 * three-in-a-row shape.
 *
 * <p><b>The output count is an assumption.</b> The row states the INGREDIENT count and no row anywhere states the output
 * before the change; the modern recipe yields one plate. Rather than invent a different count, the supplied shapes yield
 * one - the modern figure - and the assumption is recorded here and in the status board, because a wrong output would
 * be invisible to a test that only checks the shape. The era's "stone" block is today's smooth stone, which is the
 * ingredient the supplied stone shape uses.
 */
public final class HistoricalPressurePlates {
    private HistoricalPressurePlates() {
    }

    /** The recipes a pre-Beta-1.3 version used instead of the modern two-block shape. */
    public static List<RemovedRecipes.Removed> recipes() {
        return List.of(
            new RemovedRecipes.Removed("oak_pressure_plate_from_three_planks", 3, 1,
                List.of("minecraft:oak_planks", "minecraft:oak_planks", "minecraft:oak_planks"),
                "minecraft:oak_pressure_plate", 1, Feature.PRESSURE_PLATE_THREE_BLOCKS, true),
            new RemovedRecipes.Removed("stone_pressure_plate_from_three_stone", 3, 1,
                List.of("minecraft:smooth_stone", "minecraft:smooth_stone", "minecraft:smooth_stone"),
                "minecraft:stone_pressure_plate", 1, Feature.PRESSURE_PLATE_THREE_BLOCKS, true));
    }
}

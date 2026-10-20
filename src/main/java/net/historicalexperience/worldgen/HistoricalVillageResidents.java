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
package net.historicalexperience.worldgen;

import java.util.List;
import java.util.Map;

/** Five piece types retain local coordinates from the incorporated MIT-declared OldVillages source,
 * 7578013ec061303982539fac7606df67577ffb26, OldVillagePieces.java lines449/450,555,611/612,658,731.
 * Small houses/shacks additionally get one resident at inspected clear interior floor positions: otherwise
 * layouts composed only of these homes are empty. This is an explicit population repair, not an upstream
 * call-site or original-artifact claim. Never arbitrary piece centres or the well. Port notices apply.
 */
public final class HistoricalVillageResidents {
    public record Spot(int x, int y, int z) { }
    private static final Map<Integer, List<Spot>> SPOTS = Map.of(
        2, List.of(new Spot(2, 2, 2)),
        3, List.of(new Spot(2, 2, 2)),
        10, List.of(new Spot(1, 2, 2)),
        11, List.of(new Spot(1, 2, 2)),
        4, List.of(new Spot(2, 2, 3), new Spot(4, 2, 3)),
        7, List.of(new Spot(4, 2, 2)),
        8, List.of(new Spot(1, 2, 7), new Spot(4, 2, 7)),
        9, List.of(new Spot(2, 2, 7)),
        12, List.of(new Spot(2, 2, 3)));
    private HistoricalVillageResidents() { }
    public static List<Spot> spots(int pieceType) { return SPOTS.getOrDefault(pieceType, List.of()); }
    public static int validMask(int pieceType) { return (1 << spots(pieceType).size()) - 1; }
}

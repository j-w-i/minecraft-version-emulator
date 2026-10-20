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
 * One ore population: the verified vein shape, size, attempt count and origin-height draw of a historical version.
 *
 * <p>A pure data carrier owned by the version model (see {@link Exclusion}). It moved here unchanged from
 * {@code OreRule}; the verified Beta 1.7.3 population and the algorithm that places it remain in
 * {@code net.historicalexperience.generation}.
 *
 * @param name         the ore's short name, for diagnostics
 * @param blockId      the block the vein writes
 * @param size         the vein size argument
 * @param attempts     how many veins are attempted per chunk
 * @param originBoundA first height draw, exclusive; zero when not drawn
 * @param originBoundB second height draw, exclusive; zero when not drawn. Lapis is the only rule that uses it,
 *                     summing two {@code nextInt(16)} calls.
 */
public record OreRule(
    String name,
    String blockId,
    int size,
    int attempts,
    int originBoundA,
    int originBoundB
) {
    /** The largest origin height this rule can draw. */
    public int maxOriginY() {
        return Math.max(0, originBoundA - 1) + Math.max(0, originBoundB - 1);
    }

    /**
     * The largest y a vein from this rule can reach.
     *
     * <p>Derived from the verified algorithm rather than measured: the centre can sit up to two
     * blocks above the origin ({@code nextInt(3) - 2}), and the radius never exceeds
     * {@code (2 * size / 16) + 1}, so this is a generous but evidence-based ceiling.
     */
    public int maxReachableY() {
        return maxOriginY() + 2 + (int) Math.ceil(2.0 * size / 16.0) + 1;
    }
}

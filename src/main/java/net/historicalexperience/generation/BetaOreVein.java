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
package net.historicalexperience.generation;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The Beta 1.7.3 ore vein shape, transcribed from the hash-verified client.
 *
 * <p>Every constant, bound and comparison here comes from {@code fl.a(World, Random, int, int, int)}
 * in {@code fl.class} (SHA1 {@code 5ed3ca39bc7be250ca1e504b3fc70d28faed654c}); see
 * {@code research/beta173-ore-vein-geometry.json} for the per-instruction record. The transcription is
 * literal on purpose, including three details a from-memory implementation would get wrong:
 *
 * <ul>
 *   <li>The loop bound is {@code i <= size}, i.e. {@code size + 1} iterations. The bytecode compares
 *       with {@code if_icmpgt}, so the end is inclusive.</li>
 *   <li>The z-axis centre offsets and the z half-radius use the <b>x</b> radius, not the y one. Only
 *       the middle loop uses the second radius. Beta computes both radii from the <em>same</em>
 *       expression, so the two values are equal and collapsing them would be behaviourally safe — but
 *       they stay separate here so each loop still maps onto the bytecode it came from.</li>
 *   <li>The axis endpoints are computed in {@code float} arithmetic and only then widened, and the
 *       radius terms are floats widened into a double multiply. Writing those in double would shift
 *       boundary blocks.</li>
 * </ul>
 *
 * <p>The caller decides what a vein block does, so this class stays a pure function of
 * {@code (random, origin, size)} and can be pinned by unit tests.
 *
 * <p><b>Known, bounded deviation.</b> Beta evaluated its sine through a {@code float}-argument lookup;
 * 26.3's equivalent takes a {@code double}. The angle is still produced by {@code float} arithmetic
 * exactly as Beta did and both are the same 65536-entry table, but widening that float to a double can
 * land on a neighbouring table entry at a boundary. That can move an individual boundary block; it
 * cannot change the attempted counts, the origin ranges, the sizes or the resulting distribution.
 */
public final class BetaOreVein {
    /** Receives each block the vein covers, in the order the algorithm visits it. */
    public interface BlockWriter {
        /**
         * Offers one block position to the vein.
         *
         * @return true when the block was replaced, which is what Beta's stone test decides
         */
        boolean offer(int x, int y, int z);
    }

    private BetaOreVein() {
    }

    /**
     * Generates one vein.
     *
     * @param random the chunk's population random, consumed in Beta's order
     * @param x      the vein's x origin, already offset by the call site's {@code nextInt(16)}
     * @param y      the vein's y origin, already sampled from the call site's height range
     * @param z      the vein's z origin, already offset by the call site's {@code nextInt(16)}
     * @param size   the vein size argument
     * @param writer the sink for the blocks the vein covers
     */
    public static void generate(
        RandomSource random,
        int x,
        int y,
        int z,
        int size,
        BlockWriter writer
    ) {
        float angle = random.nextFloat() * (float) Math.PI;
        float sinAngle = (float) Mth.sin(angle);
        float cosAngle = (float) Mth.cos(angle);

        // Beta: (float)(x + 8) + sin(f) * (float) size / 8.0f, evaluated in float, then widened.
        double xFrom = (double) ((float) (x + 8) + sinAngle * (float) size / 8.0f);
        double xTo = (double) ((float) (x + 8) - sinAngle * (float) size / 8.0f);
        double zFrom = (double) ((float) (z + 8) + cosAngle * (float) size / 8.0f);
        double zTo = (double) ((float) (z + 8) - cosAngle * (float) size / 8.0f);
        double yFrom = y + random.nextInt(3) - 2;
        double yTo = y + random.nextInt(3) - 2;

        for (int i = 0; i <= size; i++) {
            double centreX = xFrom + (xTo - xFrom) * (double) i / (double) size;
            double centreY = yFrom + (yTo - yFrom) * (double) i / (double) size;
            double centreZ = zFrom + (zTo - zFrom) * (double) i / (double) size;

            double noise = random.nextDouble() * (double) size / 16.0;
            float wave = (float) Mth.sin(((float) i * (float) Math.PI / (float) size));
            double radiusX = (double) (wave + 1.0f) * noise + 1.0;
            // The same expression as radiusX: Beta evaluates it twice into two locals.
            double radiusY = (double) (wave + 1.0f) * noise + 1.0;

            int minX = Mth.floor(centreX - radiusX / 2.0);
            int minY = Mth.floor(centreY - radiusY / 2.0);
            int minZ = Mth.floor(centreZ - radiusX / 2.0);
            int maxX = Mth.floor(centreX + radiusX / 2.0);
            int maxY = Mth.floor(centreY + radiusY / 2.0);
            int maxZ = Mth.floor(centreZ + radiusX / 2.0);

            for (int bx = minX; bx <= maxX; bx++) {
                double dx = ((double) bx + 0.5 - centreX) / (radiusX / 2.0);
                if (dx * dx >= 1.0) {
                    continue;
                }
                for (int by = minY; by <= maxY; by++) {
                    double dy = ((double) by + 0.5 - centreY) / (radiusY / 2.0);
                    if (dx * dx + dy * dy >= 1.0) {
                        continue;
                    }
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        double dz = ((double) bz + 0.5 - centreZ) / (radiusX / 2.0);
                        if (dx * dx + dy * dy + dz * dz >= 1.0) {
                            continue;
                        }
                        writer.offer(bx, by, bz);
                    }
                }
            }
        }
    }
}

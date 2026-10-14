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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Uses Minecraft's {@link RandomSource} so the RNG consumption order is the real one, but
 * no world, registry or chunk: the vein writer is a plain recorder.
 *
 * <p>These tests exist because the transcription is the risky part. They pin the shape against fixed
 * seeds, so a change to a bound, a comparison, the {@code i <= size} end condition, which radius each
 * loop uses, or the order in which the random is consumed all show up as a diff rather than as a
 * slightly different world.
 */
class BetaOreVeinTest {
    private record Point(int x, int y, int z) {
    }

    private static List<Point> generate(long seed, int x, int y, int z, int size) {
        List<Point> visited = new ArrayList<>();
        BetaOreVein.generate(RandomSource.create(seed), x, y, z, size, (bx, by, bz) -> {
            visited.add(new Point(bx, by, bz));
            return true;
        });
        return visited;
    }

    @Test
    @DisplayName("the vein is deterministic for a seed and visits a contiguous, bounded set")
    void shapeIsDeterministicAndBounded() {
        List<Point> first = generate(1L, 16, 40, 16, 8);
        List<Point> second = generate(1L, 16, 40, 16, 8);

        assertEquals(first, second, "the same seed and origin must produce the same vein");
        assertFalse(first.isEmpty(), "a size-8 vein must cover at least one block");
        assertTrue(first.size() < 8 * 11,
            "a size-8 vein must stay inside the ellipsoid, not fill its bounding box");
    }

    @Test
    @DisplayName("a bigger size reaches further from the origin")
    void largerSizeReachesFurther() {
        int smallReach = reach(generate(7L, 0, 64, 0, 6));
        int largeReach = reach(generate(7L, 0, 64, 0, 32));

        assertTrue(largeReach > smallReach,
            "size 32 must reach further than size 6 (" + largeReach + " vs " + smallReach + ")");
    }

    @Test
    @DisplayName("the origin height reaches at most two blocks up and the radius on top of that")
    void verticalReachMatchesTheDerivedBound() {
        // This pins the derived expectation the probe later checks a whole world against: the centre
        // can sit at most two above the origin, and the radius never exceeds (2 * size / 16) + 1.
        for (int size : new int[] {6, 7, 8, 16, 32}) {
            int highest = Integer.MIN_VALUE;
            for (int seed = 0; seed < 40; seed++) {
                for (Point point : generate(seed, 0, 100, 0, size)) {
                    highest = Math.max(highest, point.y());
                }
            }
            int derivedCeiling = 100 + 2 + (int) Math.ceil(2.0 * size / 16.0) + 1;
            assertTrue(highest <= derivedCeiling,
                "size " + size + " reached y=" + highest + " above the derived ceiling "
                    + derivedCeiling);
        }
    }

    @Test
    @DisplayName("the same seed offers exactly these positions, in this order")
    void pinnedOfferedPositionsForAFixedSeed() {
        // A characterisation pin, captured from this implementation. It does NOT prove the
        // transcription is right - the semantic checks above and the whole-world measurement do that.
        // What it does prove is that nothing about the shape drifts unnoticed: a bound, a comparison,
        // the inclusive loop end, which radius a loop uses, or the order the random is consumed all
        // change this sequence.
        List<Point> visited = generate(42L, 0, 30, 0, 7);

        assertEquals(List.of(
            new Point(8, 27, 7), new Point(8, 28, 7),
            new Point(8, 27, 7), new Point(8, 28, 7),
            new Point(7, 27, 8), new Point(7, 28, 8),
            new Point(7, 27, 8), new Point(7, 28, 8),
            new Point(7, 27, 8), new Point(7, 28, 8)),
            visited);
    }

    @Test
    @DisplayName("a position can be offered more than once, which is why the writer tests stone each time")
    void positionsMayRepeat() {
        // Measured, not assumed: the ellipsoid is evaluated once per iteration, so consecutive
        // iterations can cover the same block. A writer must therefore be idempotent and decide
        // replacement from the CURRENT block, which is exactly what Beta's stone test does - and it is
        // why a repeated position is harmless rather than something the caller has to de-duplicate.
        List<Point> visited = generate(42L, 0, 30, 0, 7);
        long distinct = visited.stream().distinct().count();

        assertTrue(distinct < visited.size(),
            "this seed is expected to re-offer positions; if it no longer does, the loop bound or the "
                + "ellipsoid test changed and the pinned sequence above must be re-derived");
        assertEquals(4, distinct);
    }

    @Test
    @DisplayName("a size-7 vein covers only a few blocks, not seven")
    void smallVeinsAreSmall() {
        // Diamond and redstone use size 7 in Beta 1.7.3, so this is the shape of a diamond vein. The
        // radius is (sin + 1) * nextDouble() * size / 16 + 1, which for size 7 is barely above one
        // block, so a vein yields a handful of ore rather than the size argument's worth. Recording it
        // keeps anyone from "fixing" a future low ore count by assuming the size is the yield.
        int distinctForSeven = (int) generate(42L, 0, 30, 0, 7).stream().distinct().count();
        int distinctForSixteen = (int) generate(42L, 0, 30, 0, 16).stream().distinct().count();

        assertTrue(distinctForSeven <= 8, "a size-7 vein must stay small, got " + distinctForSeven);
        assertTrue(distinctForSixteen > distinctForSeven,
            "a size-16 vein must cover more ground than a size-7 one");
    }

    @Test
    @DisplayName("a writer that replaces nothing still sees the whole candidate set")
    void writerDecidesReplacement() {
        List<Point> offered = new ArrayList<>();
        BetaOreVein.generate(RandomSource.create(5L), 0, 20, 0, 8, (bx, by, bz) -> {
            offered.add(new Point(bx, by, bz));
            return false;
        });

        assertFalse(offered.isEmpty(),
            "the vein must offer its candidate blocks; the writer, not the vein, decides");
    }

    private static int reach(List<Point> points) {
        int max = 0;
        for (Point point : points) {
            max = Math.max(max, Math.abs(point.x()) + Math.abs(point.z()));
        }
        return max;
    }
}

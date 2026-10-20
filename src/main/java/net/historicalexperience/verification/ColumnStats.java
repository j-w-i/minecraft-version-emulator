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
package net.historicalexperience.verification;

import java.util.List;
import java.util.Objects;

/**
 * Pure, runtime-free analysis of one world column.
 *
 * <p>Exists so the runtime probe's terrain claims are computed by tested code instead of by
 * ad-hoc arithmetic inside the probe, and so the numbers can be asserted on without a Minecraft
 * runtime.
 *
 * <p>Samples are ordered bottom-up: {@code samples[0]} is the world's minimum Y.
 */
public final class ColumnStats {

    /** Placeholder reported when a column has no non-air block at all. */
    public static final int NONE = Integer.MIN_VALUE;

    private ColumnStats() {
    }

    /**
     * Measurements for a single column.
     *
     * @param top            index of the highest non-air sample, or {@link #NONE}
     * @param lowestAir      index of the lowest air sample, or {@link #NONE} when the column is
     *                       solid from bottom to top
     * @param sampleCount    number of samples examined
     * @param bedrock        number of bedrock samples (expected exactly 1 in a normal column)
     * @param water          number of water samples
     * @param waterTop       index of the highest water sample, or {@link #NONE}
     * @param nonAirSamples  number of non-air samples
     * @param airAboveTop    air samples strictly above {@link #top}
     */
    public record Stats(
        int top,
        int lowestAir,
        int sampleCount,
        int bedrock,
        int bedrockTop,
        int bedrockBottom,
        int water,
        int waterTop,
        int nonAirSamples,
        int airAboveTop
    ) {
        /** True when the column has any non-air content at all. */
        public boolean hasTerrain() {
            return top != NONE;
        }

        /** The number of air samples in the column. */
        public int airSamples() {
            return sampleCount - nonAirSamples;
        }

        /** A single machine readable line for the probe log. */
        public String describe() {
            return "top=" + display(top)
                + " lowestAir=" + display(lowestAir)
                + " samples=" + sampleCount
                + " bedrock=" + bedrock
                + " water=" + water
                + " waterTop=" + display(waterTop)
                + " nonAir=" + nonAirSamples
                + " airAboveTop=" + airAboveTop;
        }

        private static String display(int value) {
            return value == NONE ? "<none>" : Integer.toString(value);
        }

        /**
         * The absolute Y of the highest non-air sample, or {@link #NONE}.
         *
         * <p>Sample indices are relative to the chunk's minimum Y; a reader comparing these numbers
         * with a height map or a debug screen needs the absolute form.
         */
        public int absoluteTopY(int chunkMinY) {
            return top == NONE ? NONE : chunkMinY + top;
        }

        /** The absolute Y of the lowest air sample, or {@link #NONE}. */
        public int absoluteLowestAirY(int chunkMinY) {
            return lowestAir == NONE ? NONE : chunkMinY + lowestAir;
        }

        /** The absolute Y of the highest bedrock sample, or {@link #NONE}. */
        public int absoluteBedrockTopY(int chunkMinY) {
            return bedrockTop == NONE ? NONE : chunkMinY + bedrockTop;
        }

        /** The absolute Y of the lowest bedrock sample, or {@link #NONE}. */
        public int absoluteBedrockBottomY(int chunkMinY) {
            return bedrockBottom == NONE ? NONE : chunkMinY + bedrockBottom;
        }

        /** The absolute Y of the highest water sample, or {@link #NONE}. */
        public int absoluteWaterTopY(int chunkMinY) {
            return waterTop == NONE ? NONE : chunkMinY + waterTop;
        }
    }

    /**
     * The absolute block x/z of a sample inside a chunk at the given chunk coordinates.
     *
     * <p>Exists so the negative-coordinate algebra is tested rather than assumed: chunk (-1,-1)
     * spans blocks -16..-1, and any formula that is not a signed shift silently yields the wrong
     * column for every negative chunk.
     */
    public static int absoluteBlockCoordinate(int chunkCoordinate, int localCoordinate) {
        return (chunkCoordinate << 4) + localCoordinate;
    }

    /**
     * Analyses a bottom-up column.
     *
     * <p>Air is recognised by its block identifier ("minecraft:air", "air") or by the vanilla
     * placeholder "void_air". Water is recognised by "minecraft:water" or "water". Empty nullable
     * samples are skipped for terrain purposes but still counted in {@code sampleCount}, so a
     * partial read cannot raise the reported top.
     *
     * @param samples block identifiers ordered from the world's minimum Y upwards
     */
    public static Stats analyze(List<String> samples) {
        Objects.requireNonNull(samples, "samples");
        int top = NONE;
        int lowestAir = NONE;
        int bedrock = 0;
        int bedrockTop = NONE;
        int bedrockBottom = NONE;
        int water = 0;
        int waterTop = NONE;
        int nonAir = 0;

        for (int index = 0; index < samples.size(); index++) {
            String sample = samples.get(index);
            if (isUnreadable(sample)) {
                // Unreadable samples are treated as air for terrain purposes so a partial read
                // cannot raise the reported surface. Counted in sampleCount but never as terrain.
                if (lowestAir == NONE) {
                    lowestAir = index;
                }
                continue;
            }
            if (isAir(sample)) {
                if (lowestAir == NONE) {
                    lowestAir = index;
                }
                continue;
            }
            nonAir++;
            top = index;
            if (isBedrock(sample)) {
                bedrock++;
                if (bedrockBottom == NONE) {
                    bedrockBottom = index;
                }
                bedrockTop = index;
            }
            if (isWater(sample)) {
                water++;
                waterTop = index;
            }
        }

        return new Stats(
            top,
            lowestAir,
            samples.size(),
            bedrock,
            bedrockTop,
            bedrockBottom,
            water,
            waterTop,
            nonAir,
            top == NONE ? 0 : samples.size() - 1 - top);
    }

    /** True when the sample could not be read at all. */
    public static boolean isUnreadable(String sample) {
        return sample == null;
    }

    /** True when the sample is air, void air, or unreadable/absent. */
    public static boolean isAir(String sample) {
        if (sample == null) {
            return true;
        }
        String path = pathOf(sample);
        return path.equals("air") || path.equals("void_air") || path.equals("cave_air");
    }

    /** True when the sample is bedrock. */
    public static boolean isBedrock(String sample) {
        return pathOf(sample).equals("bedrock");
    }

    /** True when the sample is water or a waterlogged fluid sample. */
    public static boolean isWater(String sample) {
        return pathOf(sample).equals("water");
    }

    /** True when the sample is a fluid other than water. */
    public static boolean isLava(String sample) {
        return pathOf(sample).equals("lava");
    }

    /** Strips a namespace prefix from a block identifier. */
    private static String pathOf(String sample) {
        if (sample == null) {
            return "";
        }
        int separator = sample.indexOf(':');
        return (separator < 0 ? sample : sample.substring(separator + 1)).toLowerCase(java.util.Locale.ROOT);
    }
}

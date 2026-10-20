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
package net.historicalexperience.world.upgrade;

import net.historicalexperience.version.Milestones;

import java.util.Map;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Carries a stored biome forward through the biome hierarchy, gated on the DESTINATION version.
 *
 * <h2>The hierarchy, as the owner stated it</h2>
 *
 * <blockquote>"the hierarchy is simple. infdev-alpha 1.1 going to any version prior to 1.8.1 goes to beta plains. alpha
 * 1.2-beta 1.7.3 -> 1.8.1 + goes to late beta and then early release. it should be like a gate."</blockquote>
 *
 * <p>So there are three generations of Moderner Beta biome naming, and where a stored biome lands depends on how far
 * the world is being upgraded:
 *
 * <ol>
 *   <li><b>Prior to Beta 1.8.1</b> - the destination is a single-biome world, so everything becomes
 *       {@code moderner_beta:beta_plains}. This covers the Infdev and early Alpha worlds, and it is why an Infdev world
 *       must NOT land on a Late Beta name: there is no path from Infdev straight to late beta, only through beta
 *       plains.</li>
 *   <li><b>Beta 1.8.1 and the Beta 1.9 prereleases</b> - the destination is the late beta generation, so a stored
 *       biome that exists in that generation is carried to its {@code late_beta_*} counterpart.</li>
 *   <li><b>Release 1.0 onward</b> - the owner's "and then early release": where an {@code early_release_*}
 *       counterpart exists it is selected, and otherwise the {@code late_beta_*} name is kept.</li>
 * </ol>
 *
 * <p>The earlier implementation had no gate at all: it mapped every family to one destination regardless of where the
 * world was going, which is what sent Infdev to Late Beta plains and flattened a Beta 1.8.1 world's biome map.
 */
public final class BiomeCarryForward {
    /** The single biome every destination before Beta 1.8.1 uses. */
    public static final String BETA_PLAINS = "moderner_beta:beta_plains";

    /** The plains name of the late beta generation, used when no early-release counterpart exists. */
    public static final String LATE_BETA_PLAINS = "moderner_beta:late_beta_plains";

    /** The one biome of the pre-biome builds: Infdev, Indev and Alpha before v1.2.0. */
    public static final String ALPHA_BIOME = "moderner_beta:alpha";

    /**
     * The biome families, in the order the game moved through them. Each suffix is a place that was renamed twice; the
     * set with an {@code early_release_*} member is measured from the shipped Moderner Beta jar, which ships exactly
     * extreme_hills, ice_plains, swampland and taiga under that prefix.
     */
    private static final String[] RENAMED_PLACES = {"extreme_hills", "ice_plains", "swampland", "taiga"};

    /** Which generation a destination belongs to. */
    public enum Generation {
        /** Alpha v1.0.1 through Alpha v1.1.x, and everything before: one placeholder biome, {@code moderner_beta:alpha}. */
        ALPHA_ONLY,
        /** Alpha v1.2.0 through Beta 1.7.3: one biome, Beta plains. */
        BETA_PLAINS_ONLY,
        /** Beta 1.8.1 through the Beta 1.9 prereleases: the late beta names. */
        LATE_BETA,
        /** Release 1.0 onward: the early release names where they exist. */
        EARLY_RELEASE
    }

    /** The generation a destination version belongs to, or null when this project declares no version. */
    public static Generation generationFor(HistoricalVersion destination) {
        if (destination == null) {
            return null;
        }
        if (destination.isBefore(Milestones.ALPHA_1_2_0)) {
            return Generation.ALPHA_ONLY;
        }
        if (destination.isBefore(Milestones.BETA_1_8_1)) {
            return Generation.BETA_PLAINS_ONLY;
        }
        return destination.isBefore(Milestones.RELEASE_1_0) ? Generation.LATE_BETA : Generation.EARLY_RELEASE;
    }

    /**
     * Where a stored biome is carried to when a world is upgraded to {@code destination}.
     *
     * @return the biome to store, or null when nothing needs to change
     */
    public static String destinationFor(String storedBiome, HistoricalVersion destination) {
        Generation generation = generationFor(destination);
        if (storedBiome == null || generation == null) {
            return null;
        }
        switch (generation) {
            case ALPHA_ONLY:
                // The owner's correction: an Infdev world taken to Alpha 1.0 or Alpha 1.1 becomes the Alpha biome, not
                // Beta plains. Those builds have no biome generation, so there is nothing else it could be.
                return ALPHA_BIOME.equals(storedBiome) ? null : ALPHA_BIOME;
            case BETA_PLAINS_ONLY:
                // Alpha v1.2.0 and through Beta 1.7.3: one biome, Beta plains. This is where the Infdev placeholder
                // lands when it is taken PAST Alpha 1.2.0, and it is why it must not skip to a Late Beta name.
                return BETA_PLAINS.equals(storedBiome) ? null : BETA_PLAINS;
            case LATE_BETA:
                return inLateBeta(storedBiome);
            case EARLY_RELEASE:
                return inEarlyRelease(storedBiome);
            default:
                return null;
        }
    }

    /** The late beta form of a stored biome, or null when it is already that or has no counterpart. */
    private static String inLateBeta(String storedBiome) {
        if (storedBiome.equals(LATE_BETA_PLAINS)) {
            return null;
        }
        if (storedBiome.equals(BETA_PLAINS) || isCarriedPlaceholder(storedBiome)) {
            // Every placeholder means the same thing here - a world whose generation predates biome weather - and
            // this generation's plains is what such a world becomes. ALPHA_BIOME is one of them.
            return LATE_BETA_PLAINS;
        }
        String climate = betaClimateCounterpart(storedBiome, false);
        if (climate != null) {
            return climate;
        }
        String suffix = knownSuffix(storedBiome);
        if (suffix == null) {
            return null;
        }
        String carried = "moderner_beta:late_beta_" + suffix;
        return carried.equals(storedBiome) ? null : carried;
    }

    /**
     * The early release form, which is what the owner asked for at this generation: "and then early release". Plains has
     * no early-release counterpart in the shipped jar, so it keeps its late beta name rather than being invented.
     */
    private static String inEarlyRelease(String storedBiome) {
        if (storedBiome.equals(LATE_BETA_PLAINS)) {
            // Already the name this generation keeps for plains. Returning it would count as a change on every run.
            return null;
        }
        if (storedBiome.equals(BETA_PLAINS) || isCarriedPlaceholder(storedBiome)) {
            return LATE_BETA_PLAINS;
        }
        String climate = betaClimateCounterpart(storedBiome, true);
        if (climate != null) {
            return climate;
        }
        String suffix = knownSuffix(storedBiome);
        if (suffix == null) {
            return null;
        }
        String carried = "moderner_beta:early_release_" + suffix;
        return carried.equals(storedBiome) ? null : carried;
    }

    /**
     * Where one of the Beta 1.7 climate biomes goes when a world reaches Beta 1.8.1 or Release 1.0.
     *
     * <p>A Beta 1.7.3 world stores about a dozen of these ({@code beta_oak_taiga}, {@code beta_tundra},
     * {@code beta_savanna}, {@code beta_rainforest}, the ocean variants ...), because its provider maps each climate cell
     * to one. The Beta 1.8.1 provider draws from a much smaller set - desert, forest, extreme hills, swampland, plains and
     * taiga, with oceans as plain {@code minecraft:ocean} - and Release 1.0 adds ice plains and jungle. Carrying only the
     * four renamed places left every other name in the chunks while Moderner Beta already reported the Late Beta set, so
     * the two disagreed. This table closes that: every Beta climate biome lands on the nearest biome the destination
     * actually generates.
     *
     * @return the counterpart, or null when {@code storedBiome} is not a Beta climate biome (or has no counterpart)
     */
    private static String betaClimateCounterpart(String storedBiome, boolean earlyRelease) {
        String prefix = "moderner_beta:beta_";
        if (!storedBiome.startsWith(prefix)) {
            return null;
        }
        String place = earlyRelease ? "moderner_beta:early_release_" : "moderner_beta:late_beta_";
        switch (storedBiome.substring(prefix.length())) {
            case "desert":
                return "minecraft:desert";
            case "forest":
            case "oak_forest":
            case "seasonal_forest":
                return "minecraft:forest";
            case "rainforest":
                return earlyRelease ? "minecraft:jungle" : "minecraft:forest";
            case "shrubland":
            case "savanna":
                return LATE_BETA_PLAINS;
            case "swampland":
                return place + "swampland";
            case "taiga":
            case "oak_taiga":
                return place + "taiga";
            case "tundra":
            case "ice_desert":
            case "ice_plains":
                return place + "ice_plains";
            case "extreme_hills":
                return place + "extreme_hills";
            case "ocean":
            case "cold_ocean":
            case "lukewarm_ocean":
            case "warm_ocean":
                return "minecraft:ocean";
            case "frozen_ocean":
                return earlyRelease ? "minecraft:frozen_ocean" : "minecraft:ocean";
            default:
                return null;
        }
    }

    /** The shared suffix of a recognised beta, late beta or early release biome, or null when it is not one. */
    private static String knownSuffix(String biome) {
        for (String prefix : new String[] {"moderner_beta:beta_", "moderner_beta:late_beta_",
            "moderner_beta:early_release_"}) {
            if (biome.startsWith(prefix)) {
                String suffix = biome.substring(prefix.length());
                for (String known : RENAMED_PLACES) {
                    if (known.equals(suffix)) {
                        return known;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The pre-biome placeholder names: every biome a world can store whose generation predates biome weather.
     *
     * <p>They all mean the same thing to this class, so it does not need to know them individually - but it does need
     * to RECOGNISE them, which is what this set is for. A stored one of these becomes {@link #ALPHA_BIOME} at an Alpha
     * destination, {@link #BETA_PLAINS} at a Beta-plains one, and {@link #LATE_BETA_PLAINS} at a late beta or early
     * release one. Leaving the set unused is what made an Indev world's biome survive into a Beta 1.8.1 world: the
     * Indev and Infdev names reached the suffix lookup, were not recognised as renamed places, and answered null -
     * which this class reads as "no change needed".
     */
    private static final java.util.Set<String> PLACEHOLDERS = java.util.Set.of(        "moderner_beta:infdev_227", "moderner_beta:infdev_325", "moderner_beta:infdev_415",
        "moderner_beta:infdev_420", "moderner_beta:infdev_611", ALPHA_BIOME,
        "moderner_beta:alpha_winter", "moderner_beta:indev_normal", "moderner_beta:indev_hell",
        "moderner_beta:indev_paradise", "moderner_beta:indev_woods", "moderner_beta:classic_0_30",
        "moderner_beta:classic_0_0_14a_08", "moderner_beta:pe_plains");

    /**
     * Whether a stored placeholder is one the Java-line hierarchy carries into a later generation.
     *
     * <p>Every placeholder means "a world whose generation predates biome weather", but not every placeholder is this
     * project's to rewrite. {@code moderner_beta:pe_*} are Pocket Edition provider names: they come from another
     * edition's biome list, this project has no researched hierarchy for them, and guessing one would be inventing a
     * mapping - so they are left exactly as stored, which the existing test pins.
     */
    private static boolean isCarriedPlaceholder(String storedBiome) {
        return PLACEHOLDERS.contains(storedBiome) && !storedBiome.startsWith("moderner_beta:pe_");
    }

    /** A stable label for the record, so a completed conversion is not repeated. */
    public static String recordLabel() {
        // v4: the late beta and early release tables now cover every Beta climate biome. A world converted under v3 left
        // beta_oak_taiga, beta_tundra, beta_savanna and the ocean variants in its chunks, so the label changes to make
        // those worlds owe the corrected conversion once. It is safe to run over a v3 result: converted names map to null.
        return "biome_hierarchy_v4";
    }

    /**
     * The label including the destination generation, so an upgrade that crosses a generation converts again while a
     * reload of the same world does not.
     */
    public static String recordLabelFor(HistoricalVersion destination) {
        Generation generation = generationFor(destination);
        return generation == null ? null : recordLabel() + ":" + generation.name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * The one biome a conversion label stands for, or null when its generation carries each biome to its own
     * counterpart (late beta and early release), so no single biome describes it.
     */
    public static String singleBiomeForLabel(String label) {
        if (label == null) {
            return null;
        }
        if (label.equals(recordLabel() + ":" + Generation.ALPHA_ONLY.name().toLowerCase(java.util.Locale.ROOT))) {
            return ALPHA_BIOME;
        }
        if (label.equals(recordLabel() + ":" + Generation.BETA_PLAINS_ONLY.name().toLowerCase(java.util.Locale.ROOT))) {
            return BETA_PLAINS;
        }
        return null;
    }

    /** Every placeholder name, exposed so tests can assert the whole set rather than a sample. */
    public static java.util.Set<String> placeholders() {
        return PLACEHOLDERS;
    }

    private BiomeCarryForward() {
    }
}

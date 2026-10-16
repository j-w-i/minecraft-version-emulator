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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The biome hierarchy, gated on the DESTINATION version.
 *
 * <p>The owner's statement of it: "the hierarchy is simple. infdev-alpha 1.1 going to any version prior to 1.8.1 goes to
 * beta plains. alpha 1.2-beta 1.7.3 -> 1.8.1 + goes to late beta and then early release. it should be like a gate."
 *
 * <p>Every test here corresponds to a reported defect. The first implementation wrote one biome over everything; the
 * second carried each biome to its own counterpart but with no gate, which sent an Infdev world's placeholder to a Late
 * Beta name when it should have gone to Beta plains.
 */
class BiomeCarryForwardTest {
    private static final String BETA_PLAINS = "moderner_beta:beta_plains";
    private static final String LATE_BETA_PLAINS = "moderner_beta:late_beta_plains";

    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId("historical_experience:" + id)
            .orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    /** Where a stored biome lands when the world is upgraded to {@code destinationId}. */
    private static String land(String storedBiome, String destinationId) {
        return BiomeCarryForward.destinationFor(storedBiome, at(destinationId));
    }

    @Test
    @DisplayName("Infdev going to Alpha BEFORE 1.2 goes to the Alpha biome, not Beta plains")
    void infdevToAlphaBeforeOneTwo() {
        // The owner's correction: "if i upgrade from infdev to alpha before 1.2 it should go to alpha rather than beta
        // plains". Alpha v1.0.x and v1.1.x have no biome generation at all, so their one biome is moderner_beta:alpha.
        String alpha = "moderner_beta:alpha";
        assertEquals(alpha, land("moderner_beta:infdev_611", "alpha_1_0_1"),
            "an Infdev world taken to Alpha 1.0.1 becomes the Alpha biome");
        assertEquals(alpha, land("moderner_beta:infdev_420", "alpha_1_1_0"));
        assertEquals(alpha, land("moderner_beta:infdev_611", "alpha"));
        // A world already holding the Alpha biome has nothing to do at such a destination.
        assertNull(land(alpha, "alpha_1_1_0"));
        // But taken PAST Alpha 1.2.0 it becomes Beta plains, which is the next generation along.
        assertEquals(BETA_PLAINS, land("moderner_beta:infdev_611", "alpha_1_2_0"));
    }

    @Test
    @DisplayName("from Alpha 1.2.0 to Beta 1.7.3 the destination has one biome, Beta plains")
    void everythingBecomesBetaPlainsFromAlpha12() {
        assertEquals(BETA_PLAINS, land("moderner_beta:alpha", "alpha_1_2_0"));
        assertEquals(BETA_PLAINS, land("moderner_beta:alpha", "beta_1_7_3"));
        // A stored biome from a later generation is pulled back down to the destination's single biome too.
        assertEquals(BETA_PLAINS, land("moderner_beta:beta_taiga", "beta_1_7_3"));
        assertEquals(BETA_PLAINS, land("moderner_beta:late_beta_taiga", "beta_1_7_3"));
        // And a world already on Beta plains has nothing to do.
        assertNull(land(BETA_PLAINS, "beta_1_7_3"));
    }

    @Test
    @DisplayName("Beta 1.8.1 and the Beta 1.9 prereleases carry each biome to its late beta counterpart")
    void lateBetaGeneration() {
        assertEquals("moderner_beta:late_beta_taiga", land("moderner_beta:beta_taiga", "beta_1_8_1"),
            "a taiga must stay a taiga; writing one biome over everything is the flattening that was reported");
        assertEquals("moderner_beta:late_beta_swampland", land("moderner_beta:beta_swampland", "beta_1_8_1"));
        assertEquals("moderner_beta:late_beta_extreme_hills", land("moderner_beta:beta_extreme_hills", "beta_1_8_1"));
        assertEquals("moderner_beta:late_beta_ice_plains", land("moderner_beta:beta_ice_plains", "beta_1_8_1"));
        assertEquals(LATE_BETA_PLAINS, land(BETA_PLAINS, "beta_1_8_1"));
        // The Beta 1.9 prereleases belong to the same generation.
        assertEquals("moderner_beta:late_beta_taiga", land("moderner_beta:beta_taiga", "beta_1_9_pre_1"));
        assertEquals("moderner_beta:late_beta_taiga", land("moderner_beta:beta_taiga", "beta_1_9_pre_6"));
        // Already there means nothing to do.
        assertNull(land("moderner_beta:late_beta_taiga", "beta_1_8_1"));
    }

    @Test
    @DisplayName("an Indev or Infdev world's placeholder biome is carried into the destination's generation")
    void placeholderBiomesAreCarriedEverywhere() {
        // The owner asked for this one directly: "remember the biome upgrader also itll upgrade from a finite to
        // infinite world so account for that". Before this test, BiomeCarryForward's PLACEHOLDERS set named the Indev
        // and Infdev biomes but nothing consulted it, so those stored biomes reached the renamed-place suffix lookup,
        // were not recognised, and answered null - which the driver reads as "no change needed". An Indev world taken
        // to Beta 1.8.1 would have kept moderner_beta:indev_normal in it.
        for (String indev : new String[] {"moderner_beta:indev_normal", "moderner_beta:indev_hell",
                "moderner_beta:indev_paradise", "moderner_beta:indev_woods"}) {
            assertEquals("moderner_beta:alpha", land(indev, "alpha_1_1_0"),
                indev + " reaching Alpha 1.1 becomes the Alpha placeholder");
            assertEquals(BETA_PLAINS, land(indev, "beta_1_7_3"),
                indev + " reaching Beta plains becomes Beta plains");
            assertEquals(LATE_BETA_PLAINS, land(indev, "beta_1_8_1"),
                indev + " reaching Beta 1.8.1 becomes late beta plains, the only placeholder that generation has");
            assertNotEquals(indev, land(indev, "beta_1_8_1"),
                indev + " must not survive an upgrade into the late beta generation");
            assertEquals(LATE_BETA_PLAINS, land(indev, "release_1_6_4"),
                indev + " reaching Release 1.6.4 keeps the late beta plains name, which is what plains keeps there");
        }

        // The Infdev placeholders were broken the same way, and are covered by the same set.
        assertEquals(LATE_BETA_PLAINS, land("moderner_beta:infdev_611", "beta_1_8_1"));
        assertEquals(LATE_BETA_PLAINS, land("moderner_beta:infdev_227", "release_1_6_4"));
        // Every Java-line placeholder is carried, so a future one cannot be added half-wired - and the Pocket
        // Edition provider names are NOT, because this project has no researched hierarchy for another edition.
        for (String placeholder : BiomeCarryForward.placeholders()) {
            if (placeholder.startsWith("moderner_beta:pe_")) {
                assertNull(land(placeholder, "beta_1_8_1"),
                    placeholder + " is a Pocket Edition name and must be left as stored");
                continue;
            }
            assertEquals(LATE_BETA_PLAINS, land(placeholder, "beta_1_8_1"),
                placeholder + " is a declared Java-line placeholder, so it must land on late beta plains");
        }
    }

    @Test
    @DisplayName("every Beta climate biome a 1.7.3 world stores lands on a biome Beta 1.8.1 generates")
    void betaClimateBiomesAreFullyCarriedToLateBeta() {
        // The reported defect: Late Beta applied in the Moderner Beta provider but the chunks still held the Beta 1.7
        // names, because only four of them were ever carried.
        assertEquals("moderner_beta:late_beta_taiga", land("moderner_beta:beta_oak_taiga", "beta_1_8_1"));
        assertEquals("moderner_beta:late_beta_ice_plains", land("moderner_beta:beta_tundra", "beta_1_8_1"));
        assertEquals("moderner_beta:late_beta_ice_plains", land("moderner_beta:beta_ice_desert", "beta_1_8_1"));
        assertEquals("minecraft:forest", land("moderner_beta:beta_oak_forest", "beta_1_8_1"));
        assertEquals("minecraft:forest", land("moderner_beta:beta_seasonal_forest", "beta_1_8_1"));
        assertEquals("minecraft:forest", land("moderner_beta:beta_rainforest", "beta_1_8_1"));
        assertEquals("minecraft:desert", land("moderner_beta:beta_desert", "beta_1_8_1"));
        assertEquals(LATE_BETA_PLAINS, land("moderner_beta:beta_savanna", "beta_1_8_1"));
        assertEquals(LATE_BETA_PLAINS, land("moderner_beta:beta_shrubland", "beta_1_8_1"));
        assertEquals("minecraft:ocean", land("moderner_beta:beta_ocean", "beta_1_8_1"));
        assertEquals("minecraft:ocean", land("moderner_beta:beta_frozen_ocean", "beta_1_8_1"));
        assertEquals("minecraft:ocean", land("moderner_beta:beta_warm_ocean", "beta_1_9_pre_3"));
        // Release 1.0 generates jungle and ice plains too.
        assertEquals("minecraft:jungle", land("moderner_beta:beta_rainforest", "release_1_0_0"));
        assertEquals("moderner_beta:early_release_ice_plains", land("moderner_beta:beta_tundra", "release_1_0_0"));
        assertEquals("minecraft:frozen_ocean", land("moderner_beta:beta_frozen_ocean", "release_1_6_4"));
    }

    @Test
    @DisplayName("the carry is idempotent: running it over its own output changes nothing")
    void carryIsIdempotent() {
        for (String version : new String[] {"beta_1_8_1", "release_1_0_0"}) {
            for (String stored : new String[] {"beta_oak_taiga", "beta_tundra", "beta_savanna", "beta_rainforest",
                "beta_ocean", "beta_frozen_ocean", "beta_taiga", "beta_plains", "beta_swampland"}) {
                String once = land("moderner_beta:" + stored, version);
                assertNotEquals(null, once, stored + " must be carried at " + version);
                assertNull(land(once, version), once + " is already at " + version + " and must not move again");
            }
        }
    }

    @Test
    @DisplayName("from Release 1.0 the early release names are selected where they exist")
    void earlyReleaseGeneration() {
        // The owner's "and then early release".
        assertEquals("moderner_beta:early_release_taiga", land("moderner_beta:beta_taiga", "release_1_0_0"));
        assertEquals("moderner_beta:early_release_taiga", land("moderner_beta:late_beta_taiga", "release_1_0_0"),
            "a world already carried to late beta moves on again when the destination generation changes");
        assertEquals("moderner_beta:early_release_swampland", land("moderner_beta:beta_swampland", "release_1_6_4"));
        assertEquals("moderner_beta:early_release_extreme_hills",
            land("moderner_beta:beta_extreme_hills", "release_1_2_5"));
        assertEquals("moderner_beta:early_release_ice_plains", land("moderner_beta:beta_ice_plains", "release_1_3"));
        // Plains has no early-release counterpart, which is measured from the shipped jar, so it keeps the late beta
        // name rather than having one invented for it.
        assertEquals(LATE_BETA_PLAINS, land(BETA_PLAINS, "release_1_0_0"));
        assertNull(land(LATE_BETA_PLAINS, "release_1_6_4"),
            "late beta plains is already what this generation keeps, so there is nothing to change");
        // Already at its destination means nothing to do.
        assertNull(land("moderner_beta:early_release_taiga", "release_1_0_0"));
    }

    @Test
    @DisplayName("biomes with no counterpart anywhere in the hierarchy are left alone")
    void unlistedBiomesAreUntouched() {
        for (String biome : new String[] {"minecraft:ocean", "minecraft:desert", "minecraft:forest",
            "minecraft:river", "minecraft:jungle", "minecraft:mushroom_fields", "minecraft:frozen_ocean",
            "minecraft:plains", "minecraft:taiga", "moderner_beta:pe_plains", "moderner_beta:beta_sky",
            "moderner_beta:pe_taiga"}) {
            assertNull(land(biome, "beta_1_8_1"), biome + " must not be moved at a late beta destination");
            assertNull(land(biome, "release_1_6_4"), biome + " must not be moved at an early release destination");
        }
        assertNull(BiomeCarryForward.destinationFor(null, at("beta_1_8_1")));
        assertNull(BiomeCarryForward.destinationFor(BETA_PLAINS, null),
            "an ordinary world has no version, so nothing is carried");
    }

    @Test
    @DisplayName("the record label follows the destination generation, so each crossing converts once")
    void recordLabelFollowsTheGeneration() {
        var before181 = HistoricalBiomeConversionDriver.recordLabelFor(at("beta_1_7_3"));
        var lateBeta = HistoricalBiomeConversionDriver.recordLabelFor(at("beta_1_8_1"));
        var earlyRelease = HistoricalBiomeConversionDriver.recordLabelFor(at("release_1_0_0"));
        // Three distinct labels, so moving a world across a generation converts again; and the same label within a
        // generation, so reopening a world does not.
        assertNotEquals(before181, lateBeta);
        assertNotEquals(lateBeta, earlyRelease);
        assertEquals(lateBeta, HistoricalBiomeConversionDriver.recordLabelFor(at("beta_1_9_pre_6")));
        assertEquals(earlyRelease, HistoricalBiomeConversionDriver.recordLabelFor(at("release_1_6_4")));
        assertNull(HistoricalBiomeConversionDriver.recordLabelFor(null));
    }

    @Test
    @DisplayName("the placeholder set is the single-biome names of Infdev, Indev, Classic and early Alpha")
    void placeholdersAreThePreBiomeBuilds() {
        assertTrue(BiomeCarryForward.placeholders().contains("moderner_beta:infdev_611"));
        assertTrue(BiomeCarryForward.placeholders().contains("moderner_beta:alpha"));
        assertTrue(BiomeCarryForward.placeholders().contains("moderner_beta:indev_hell"));
        // A real biome name must never be treated as a placeholder, or it would be flattened to plains.
        for (String real : new String[] {"moderner_beta:beta_taiga", "moderner_beta:beta_plains",
            "moderner_beta:late_beta_taiga", "moderner_beta:early_release_taiga", "moderner_beta:beta_ocean"}) {
            assertFalse(BiomeCarryForward.placeholders().contains(real), real + " is a real biome, not a placeholder");
        }
    }
}

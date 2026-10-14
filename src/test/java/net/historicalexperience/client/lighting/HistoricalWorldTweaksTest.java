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
package net.historicalexperience.client.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The windows for the sky, cloud, item-rendering and lighting toggles, asserted against the registry.
 *
 * <p>Each boundary here rests on a quotation recorded on {@link HistoricalWorldTweaks} or
 * {@link HistoricalLightingTweaks}. The point of the test is that the window cannot drift silently: a version moved
 * across a boundary fails here rather than changing what a world renders.
 */
class HistoricalWorldTweaksTest {
    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId(id).orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    @Test
    @DisplayName("historical cloud heights are 108 before Beta 1.8 and 128 afterward; ordinary/no-cloud values survive")
    void cloudHeight() {
        HistoricalVersion infdev = at("historical_experience:infdev_420");
        HistoricalVersion beta17 = at("historical_experience:beta_1_7_3");
        HistoricalVersion beta18 = at("historical_experience:beta_1_8_1");
        HistoricalVersion release = at("historical_experience:release_1_6_4");
        assertEquals(108.0F, HistoricalWorldTweaks.cloudHeightForVersion(192.0F, infdev));
        assertEquals(108.0F, HistoricalWorldTweaks.cloudHeightForVersion(192.0F, beta17));
        assertEquals(128.0F, HistoricalWorldTweaks.cloudHeightForVersion(192.0F, beta18));
        assertEquals(128.0F, HistoricalWorldTweaks.cloudHeightForVersion(192.0F, release));
        assertEquals(192.0F, HistoricalWorldTweaks.cloudHeightForVersion(192.0F, null), "ordinary world retains modern height");
        assertTrue(Float.isNaN(HistoricalWorldTweaks.cloudHeightForVersion(Float.NaN, beta17)),
            "no-cloud dimensions retain Minecraft's NaN sentinel");
        assertTrue(HistoricalWorldTweaks.availableForCloudHeight(infdev));
        assertFalse(HistoricalWorldTweaks.availableForCloudHeight(beta18));
    }

    @Test
    @DisplayName("2D dropped items apply through the Release 1.4 profile; the 3D model arrived in 1.4.6")
    void twoDimensionalItems() {
        // Item (entity) history dates the 3D spinning model to 12w50a, released in 1.4.6; the declared Release 1.4
        // profile begins at 1.4.2 and therefore predates it.
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:alpha_1_0_1")));
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:beta_1_7_3")));
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_3")));
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_4")));
        assertFalse(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_5")));
        assertFalse(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_6_4")));
    }

    @Test
    @DisplayName("the recipe-book button hides in each historical profile and remains alone in ordinary worlds")
    void recipeBook() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(HistoricalWorldTweaks.shouldHideRecipeBookButton(version, true), version.id());
            assertFalse(HistoricalWorldTweaks.shouldHideRecipeBookButton(version, false), version.id());
        }
        assertFalse(HistoricalWorldTweaks.shouldHideRecipeBookButton(null, true), "ordinary worlds remain untouched");
        assertFalse(HistoricalWorldTweaks.availableForRecipeBook(at("historical_experience:release_1_6_4")));
    }

    @Test
    @DisplayName("leaves and water lighting are on before Beta 1.8 and unavailable from it")
    void lightingTweaks() {
        assertTrue(HistoricalLightingTweaks.availableFor(at("historical_experience:alpha_1_0_1")));
        assertTrue(HistoricalLightingTweaks.availableFor(at("historical_experience:beta_1_7_3")));
        assertFalse(HistoricalLightingTweaks.availableFor(at("historical_experience:beta_1_8_1")),
            "Beta 1.8 replaced the lighting engine these rules belong to");
        assertFalse(HistoricalLightingTweaks.availableFor(null));
        // Upstream's water light-block figure, pinned so it cannot drift.
        org.junit.jupiter.api.Assertions.assertEquals(3, HistoricalLightingTweaks.OLD_WATER_LIGHT_BLOCK);
    }

    @Test
    @DisplayName("version-gated options remain inert with no historical version active")
    void ordinaryWorldsAreUntouched() {
        // The settings read the active version, which is unset in a unit test, so all of these answer false.
        assertFalse(HistoricalWorldTweaks.old2dItems());
        assertFalse(HistoricalWorldTweaks.recipeBookDisabled());
        assertFalse(HistoricalLightingTweaks.oldLeavesLighting());
        assertFalse(HistoricalLightingTweaks.oldWaterLighting());
    }

    @Test
    @DisplayName("the sky is the era's own: a flat Infdev/Alpha colour, the Beta temperature function, nothing later")
    void eraSkyColour() {
        // Alpha v1.0.11 carries 8961023 (0x8BBDFF) as its world sky colour; Infdev's is Moderner Beta's extracted
        // INFDEV_*_SKY_COLOR, 0x99CCFF. See HistoricalSkyColours for the jar references.
        assertEquals(Integer.valueOf(0x8BBDFF), HistoricalWorldTweaks.eraSkyColour(at("historical_experience:alpha_1_0_1")));
        assertEquals(Integer.valueOf(0x8BBDFF), HistoricalWorldTweaks.eraSkyColour(at("historical_experience:alpha_1_1_0")));
        assertEquals(Integer.valueOf(0x8BBDFF), HistoricalWorldTweaks.eraSkyColour(at("historical_experience:alpha_1_2_0")));
        assertEquals(Integer.valueOf(0x99CCFF), HistoricalWorldTweaks.eraSkyColour(at("historical_experience:infdev_420")));
        assertEquals(Integer.valueOf(0x99CCFF), HistoricalWorldTweaks.eraSkyColour(at("historical_experience:infdev_611")));

        // Beta's sky was computed from the temperature field, so there is no flat colour to assert - asserting one is
        // exactly the mistake Nostalgic Tweaks made with 0x97A3FF. Beta 1.0 through Beta 1.8.1 are the temperature
        // window; the flat value must stay null for all of them.
        for (String id : new String[] {"beta_1_0", "beta_1_2", "beta_1_3", "beta_1_4", "beta_1_5", "beta_1_6",
            "beta_1_7_3", "beta_1_8_1"}) {
            HistoricalVersion beta = at("historical_experience:" + id);
            assertEquals(HistoricalWorldTweaks.SkyEra.TEMPERATURE, HistoricalWorldTweaks.skyEra(beta), id);
            assertNull(HistoricalWorldTweaks.eraSkyColour(beta), id);
            assertTrue(HistoricalWorldTweaks.availableForSkyColour(beta), id);
        }

        // Beta 1.9 Pre-release 6 and Release 1.0 both read the biome temperature at height 64 and feed it to the same
        // HSV function modern Minecraft still uses, so from there the game's own colour is already correct.
        for (String id : new String[] {"beta_1_9_pre_1", "beta_1_9_pre_6", "release_1_0_0", "release_1_6_4"}) {
            HistoricalVersion later = at("historical_experience:" + id);
            assertEquals(HistoricalWorldTweaks.SkyEra.NONE, HistoricalWorldTweaks.skyEra(later), id);
            assertNull(HistoricalWorldTweaks.eraSkyColour(later), id);
            assertFalse(HistoricalWorldTweaks.availableForSkyColour(later), id);
        }

        assertEquals(HistoricalWorldTweaks.SkyEra.NONE, HistoricalWorldTweaks.skyEra(null), "unknown is not 'yes'");
        assertNull(HistoricalWorldTweaks.eraSkyColour(null));
        assertFalse(HistoricalWorldTweaks.skyTweakActive(null), "ordinary worlds must stay untouched");
    }

    @Test
    @DisplayName("Beta's sky colour function is the original one, rounding like java.awt.Color and not like Mth")
    void betaSkyColourFunction() {
        // Expected values are the output of the decompiled clients' own call,
        // Color.getHSBColor(0.62222224F - t * 0.05F, 0.5F + t * 0.1F, 1.0F).getRGB() with t = clamp(temp / 3).
        assertEquals(0x9C99FF, HistoricalSkyColours.temperatureSkyColour(-3.0F), "coldest reachable colour");
        assertEquals(0x80A1FF, HistoricalSkyColours.temperatureSkyColour(0.0F), "neutral temperature");
        assertEquals(0x66BDFF, HistoricalSkyColours.temperatureSkyColour(3.0F), "warmest reachable colour");
        assertEquals(0x77A9FF, HistoricalSkyColours.temperatureSkyColour(1.0F));
        assertEquals(0x73ADFF, HistoricalSkyColours.temperatureSkyColour(1.5F));
        // Out-of-range temperatures are clamped by the era's own code, not extrapolated.
        assertEquals(0x9C99FF, HistoricalSkyColours.temperatureSkyColour(-40.0F));
        assertEquals(0x66BDFF, HistoricalSkyColours.temperatureSkyColour(40.0F));
        // Modern Minecraft truncates where the original rounds; at a neutral temperature that is one unit of red.
        // If this ever stops being true the port has silently become a port of the wrong client.
        assertEquals(0x80A1FF, HistoricalSkyColours.temperatureSkyColour(0.0F));
        org.junit.jupiter.api.Assertions.assertNotEquals(0x7FA1FF, HistoricalSkyColours.temperatureSkyColour(0.0F),
            "0x7FA1FF is modern Mth.hsvToRgb's truncating result, not Beta's rounding one");
    }

    @Test
    @DisplayName("left-click activation stops before the first published Release 1.4 build, which is 1.4.2")
    void leftClickActivation() {
        assertTrue(net.historicalexperience.gameplay.HistoricalLeftClickEvents
            .availableFor(at("historical_experience:alpha_1_0_1")));
        assertTrue(net.historicalexperience.gameplay.HistoricalLeftClickEvents
            .availableFor(at("historical_experience:beta_1_7_3")));
        assertTrue(net.historicalexperience.gameplay.HistoricalLeftClickEvents
            .availableFor(at("historical_experience:release_1_0_0")));
        assertFalse(net.historicalexperience.gameplay.HistoricalLeftClickEvents
            .availableFor(at("historical_experience:release_1_4")),
            "the first published Release 1.4 build is 1.4.2, after left-click activation was removed");
        assertFalse(net.historicalexperience.gameplay.HistoricalLeftClickEvents
            .availableFor(at("historical_experience:release_1_5")),
            "Release 1.5 is after 1.4.2, so left-click activation is gone");
        assertFalse(net.historicalexperience.gameplay.HistoricalLeftClickEvents
            .availableFor(at("historical_experience:release_1_6_4")));
        assertFalse(net.historicalexperience.gameplay.HistoricalLeftClickEvents.availableFor(null));
    }
}

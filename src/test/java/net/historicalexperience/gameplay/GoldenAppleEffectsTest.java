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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The golden apple's effect history, taken from minecraft.wiki's own Golden Apple history rows (fetched
 * 2026-10-21 against the raw wikitext):
 *
 * <ul>
 *   <li>Beta 1.8 Pre-release: 10 hunger, Regeneration 30 s (already implemented).</li>
 *   <li>Release: "restore 4 hunger and give only 5 seconds of Regeneration. The Regeneration effect of golden apples
 *       has been slowed down" — so amplifier 0, not the modern II.</li>
 *   <li>13w23b-13w25a, shipping in Release 1.6.1: Regeneration II for 0:10 and Absorption for 2:00.</li>
 *   <li>1.9 15w37a reduced it to I, 15w44a returned it to II (outside every declared version).</li>
 * </ul>
 */
class GoldenAppleEffectsTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("Release 1.0-1.5 use the slowed Regeneration and no Absorption")
    void releaseOneEffects() {
        for (String version : new String[] {"release_1_0_0", "release_1_1", "release_1_2_5", "release_1_3",
                "release_1_4", "release_1_5"}) {
            assertTrue(Features.active(Feature.GOLDEN_APPLE_REGEN_ONE, P + version), version + " uses the slowed Regen");
            assertFalse(Features.active(Feature.GOLDEN_APPLE_SIX_ONE_EFFECTS, P + version),
                version + " predates 13w23b, so no Absorption and no Regeneration II");
        }
        assertEquals(100, HistoricalFoodHelper.goldenAppleReleaseRegenerationTicks(), "5 seconds");
        assertEquals(0, HistoricalFoodHelper.goldenAppleReleaseRegenerationAmplifier(), "slowed to level I");
    }

    @Test
    @DisplayName("Release 1.6.4 uses Regeneration II for 10 s and Absorption 2:00")
    void releaseSixOneEffects() {
        assertTrue(Features.active(Feature.GOLDEN_APPLE_SIX_ONE_EFFECTS, P + "release_1_6_4"));
        assertFalse(Features.active(Feature.GOLDEN_APPLE_REGEN_ONE, P + "release_1_6_4"));
        assertEquals(200, HistoricalFoodHelper.goldenAppleSixOneRegenerationTicks(), "0:10");
        assertEquals(1, HistoricalFoodHelper.goldenAppleSixOneRegenerationAmplifier(), "level II");
        assertEquals(2400, HistoricalFoodHelper.goldenAppleSixOneAbsorptionTicks(), "2:00");
    }

    @Test
    @DisplayName("the Beta 1.8 form is untouched, and ordinary worlds get neither window")
    void windowsDoNotOverlap() {
        assertTrue(Features.active(Feature.BETA_GOLDEN_APPLE, P + "beta_1_8_1"));
        assertFalse(Features.active(Feature.GOLDEN_APPLE_REGEN_ONE, P + "beta_1_8_1"));
        assertFalse(Features.active(Feature.GOLDEN_APPLE_SIX_ONE_EFFECTS, P + "beta_1_8_1"));
        for (String ordinary : new String[] {null, "moderner_beta:beta", "example:not_a_version"}) {
            assertFalse(Features.active(Feature.GOLDEN_APPLE_REGEN_ONE, ordinary));
            assertFalse(Features.active(Feature.GOLDEN_APPLE_SIX_ONE_EFFECTS, ordinary));
        }
    }
}

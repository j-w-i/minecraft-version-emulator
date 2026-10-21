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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. minecraft.wiki, Boat history: "v1.0.6_01 Boats now only break on impact with land at high speeds." Boats
 * were added in v1.0.6, so every declared build with boats has this rule. The wiki gives no speed figure;
 * {@link HistoricalBoatImpact#HIGH_SPEED} is documented as an approximation.
 *
 * <p>The hook itself is proven to run on a dedicated server (`boatImpactCalls` in `/hex status`), but RCON cannot drive a
 * boat at speed - boats need player input, and NBT-granted motion is damped away - so the break itself is left to the
 * in-game test rather than claimed from a probe.
 */
class BoatLandImpactTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the rule applies to every declared build with boats")
    void window() {
        for (String version : new String[] {"alpha_1_0_6", "beta_1_7_3", "release_1_0_0", "release_1_6_4"}) {
            assertTrue(Features.active(Feature.BOAT_LAND_IMPACT_BREAKS, P + version), version);
        }
        for (String earlier : new String[] {"alpha_1_0_1", "alpha_1_0_4"}) {
            assertFalse(Features.active(Feature.BOAT_LAND_IMPACT_BREAKS, P + earlier),
                earlier + " has no boats");
        }
        assertFalse(Features.active(Feature.BOAT_LAND_IMPACT_BREAKS, (String) null));
    }

    @Test
    @DisplayName("land ahead, a collision and high speed are all required")
    void predicate() {
        String version = P + "alpha_1_0_6";
        assertTrue(HistoricalBoatImpact.shouldBreak(version, true, true, HistoricalBoatImpact.HIGH_SPEED));
        assertTrue(HistoricalBoatImpact.shouldBreak(version, true, true, 1.2D), "above the threshold");
        assertFalse(HistoricalBoatImpact.shouldBreak(version, true, true, HistoricalBoatImpact.HIGH_SPEED - 0.001D),
            "just below the threshold is a nudge, not a crash");
        assertFalse(HistoricalBoatImpact.shouldBreak(version, false, true, 1.2D), "water ahead cannot break it");
        assertFalse(HistoricalBoatImpact.shouldBreak(version, true, false, 1.2D), "no collision, no break");
        assertFalse(HistoricalBoatImpact.shouldBreak(P + "alpha_1_0_1", true, true, 1.2D), "no boats there");
        assertFalse(HistoricalBoatImpact.shouldBreak(null, true, true, 1.2D), "ordinary worlds untouched");
    }

    @Test
    @DisplayName("the approximation is documented and positive")
    void threshold() {
        assertTrue(HistoricalBoatImpact.HIGH_SPEED > 0.0D);
        assertTrue(HistoricalBoatImpact.HIGH_SPEED < 2.0D, "a boat cannot exceed a couple of blocks per tick");
    }
}

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
 * Level: unit. Beta 1.5's damage bug. research/feature-timeline.json, Java Edition history rows (the same rows the
 * feature's evidence quotes): Beta 1.5 "All attacks from players now deal 1 more damage than intended unless the
 * player is currently moving upwards in singleplayer"; Beta 1.8 Pre-release "Player attacks no longer deal 1 more
 * damage than intended". The same builds raised unarmed damage from 1 to 2 and lowered it back, so the extra point is
 * what reproduces both statements.
 */
class BetaDamageBugTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the window is Beta 1.5 through Beta 1.7.3")
    void window() {
        for (String has : new String[] {"beta_1_5", "beta_1_6", "beta_1_7_3"}) {
            assertTrue(Features.active(Feature.BETA_DAMAGE_BUG, P + has), has);
        }
        for (String not : new String[] {"beta_1_4", "beta_1_8_1", "release_1_0_0"}) {
            assertFalse(Features.active(Feature.BETA_DAMAGE_BUG, P + not), not);
        }
        assertFalse(Features.active(Feature.BETA_DAMAGE_BUG, (String) null), "ordinary worlds untouched");
    }

    @Test
    @DisplayName("one extra point, unless the player is moving upwards")
    void bonus() {
        assertEquals(1.0F, HistoricalDamageValues.betaDamageBugBonus(P + "beta_1_7_3", false));
        assertEquals(0.0F, HistoricalDamageValues.betaDamageBugBonus(P + "beta_1_7_3", true),
            "the bug exempted a player moving upwards");
        assertEquals(0.0F, HistoricalDamageValues.betaDamageBugBonus(P + "beta_1_4", false));
        assertEquals(0.0F, HistoricalDamageValues.betaDamageBugBonus(P + "beta_1_8_1", false),
            "the Beta 1.8 Pre-release fixed it");
        assertEquals(0.0F, HistoricalDamageValues.betaDamageBugBonus(null, false));
    }
}

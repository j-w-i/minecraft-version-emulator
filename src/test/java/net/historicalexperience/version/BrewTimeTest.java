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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. research/version-transitions.json records brewing "listed at 30 seconds" for Beta 1.9 Prerelease 3;
 * minecraft.wiki, Java Edition 1.1: "Brewing - Reduced brewing time from 30 seconds to 20 seconds." The mixin
 * `BrewingStandHistoricalBrewTimeMixin` turns the 400-tick constant into 600 inside that window.
 */
class BrewTimeTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("30 seconds from Prerelease 3 through Release 1.0, 20 seconds from Release 1.1")
    void window() {
        for (String slow : new String[] {"beta_1_9_pre_3", "beta_1_9_pre_4", "beta_1_9_pre_6", "release_1_0_0"}) {
            assertTrue(Features.active(Feature.BREW_TIME_THIRTY_SECONDS, P + slow), slow + " brews in 30 s");
        }
        for (String fast : new String[] {"beta_1_9_pre_2", "release_1_1", "release_1_2_5", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.BREW_TIME_THIRTY_SECONDS, P + fast), fast + " brews in 20 s");
        }
        assertFalse(Features.active(Feature.BREW_TIME_THIRTY_SECONDS, (String) null), "ordinary worlds untouched");
    }
}

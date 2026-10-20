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
 * Level: unit. Hardcore mode arrives in Beta 1.9 Prerelease 2.
 *
 * <p>research/version-transitions.json, Prerelease 2's additions: "Hardcore mode (locks difficulty to Hard,
 * permadeath)". So Beta 1.8.1 and Prerelease 1 cannot be in hardcore, whatever a level file says; the server-side
 * enforcement is the `PrimaryLevelData.isHardcore` mixin and is probed at runtime, not here.
 */
class HardcoreGateTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("hardcore is gated before Beta 1.9 Prerelease 2 and available from it")
    void window() {
        for (String early : new String[] {"alpha_1_2_0", "beta_1_0", "beta_1_7_3", "beta_1_8_1", "beta_1_9_pre_1"}) {
            assertTrue(Features.active(Feature.NO_HARDCORE, P + early), early + " predates hardcore");
        }
        for (String later : new String[] {"beta_1_9_pre_2", "beta_1_9_pre_6", "release_1_0_0", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.NO_HARDCORE, P + later), later + " has hardcore");
        }
        assertFalse(Features.active(Feature.NO_HARDCORE, (String) null), "ordinary worlds untouched");
    }
}

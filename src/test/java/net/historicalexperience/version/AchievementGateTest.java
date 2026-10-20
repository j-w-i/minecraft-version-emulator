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
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. research/version-transitions.json, mechanic "achievements", per build:
 * <ul>
 *   <li>Alpha v1.0.1 "through Beta 1.3": absent (its matrix row is "A");</li>
 *   <li>Beta 1.4: trigger infrastructure only, "the complete achievement set/UI arrives incrementally";</li>
 *   <li>Beta 1.5: "16 achievements and statistics", with the achievement/statistics UI.</li>
 * </ul>
 * The mixin that suppresses modern advancements is `PlayerAdvancementsHistoricalMixin`; this test pins the boundary.
 */
class AchievementGateTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("no advancements before Beta 1.5, and normal behaviour from Beta 1.5")
    void window() {
        for (String early : new String[] {"alpha_1_0_1", "beta_1_0", "beta_1_3", "beta_1_4"}) {
            assertTrue(Features.active(Feature.NO_ACHIEVEMENTS, P + early), early);
        }
        for (String later : new String[] {"beta_1_5", "beta_1_7_3", "beta_1_9_pre_6", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.NO_ACHIEVEMENTS, P + later), later);
        }
        assertFalse(Features.active(Feature.NO_ACHIEVEMENTS, (String) null), "ordinary worlds untouched");
    }
}

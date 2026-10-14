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

import static org.junit.jupiter.api.Assertions.*;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.Test;

class HistoricalFogTweaksTest {
    @Test
    void upstreamAlphaRelease164DistanceBranches() {
        assertEquals(384, HistoricalFogTweaks.farPlane(12));
        assertEquals(307.2F, HistoricalFogTweaks.terrainEnd(12), 0.001F);
        assertEquals(70.4F, HistoricalFogTweaks.terrainEnd(4), 0.001F);
        assertEquals(64.0F, HistoricalFogTweaks.skyEnd(4), 0.001F);
        assertEquals(464, HistoricalFogTweaks.farPlane(29));
    }

    @Test
    void sunriseFogBoundaryAndOrdinaryIsolation() {
        assertTrue(HistoricalFogTweaks.beforeBeta18(HistoricalVersions.byId(
            "historical_experience:beta_1_7_3").orElseThrow()));
        assertFalse(HistoricalFogTweaks.beforeBeta18(HistoricalVersions.byId(
            "historical_experience:beta_1_8_1").orElseThrow()));
        assertFalse(HistoricalFogTweaks.beforeBeta18(null));
        assertFalse(HistoricalFogTweaks.worldFog());
        assertFalse(HistoricalFogTweaks.oldSunriseFog());
    }
}

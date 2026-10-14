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

class HistoricalStarsTest {
    private static HistoricalStars.Style at(String id) {
        return HistoricalStars.styleFor(HistoricalVersions.byId("historical_experience:" + id).orElseThrow());
    }

    @Test
    void starChangesAtBeta15AndRelease13() {
        assertEquals(HistoricalStars.Style.ALPHA, at("infdev_420"));
        assertEquals(HistoricalStars.Style.ALPHA, at("alpha_1_0_1"));
        assertEquals(HistoricalStars.Style.ALPHA, at("beta_1_3"));
        assertEquals(HistoricalStars.Style.BETA, at("beta_1_5"));
        assertEquals(HistoricalStars.Style.BETA, at("beta_1_7_3"));
        assertEquals(HistoricalStars.Style.BETA, at("beta_1_8_1"));
        assertEquals(HistoricalStars.Style.BETA, at("release_1_2_5"));
        assertEquals(HistoricalStars.Style.MODERN, at("release_1_3"));
        assertEquals(HistoricalStars.Style.MODERN, at("release_1_6_4"));
        assertEquals(HistoricalStars.Style.MODERN, HistoricalStars.styleFor(null));
    }

    @Test
    void upstreamSizeAndBrightnessAreNotGuessedFromNames() {
        assertEquals(0.25F, HistoricalStars.width(HistoricalStars.Style.ALPHA, 0.15F));
        assertEquals(0.25F, HistoricalStars.width(HistoricalStars.Style.BETA, 0.1F));
        assertEquals(0.15F, HistoricalStars.width(HistoricalStars.Style.MODERN, 0.15F));
        assertEquals(1.0F, HistoricalStars.colour(HistoricalStars.Style.ALPHA, 0.5F));
        assertEquals(0.5F, HistoricalStars.colour(HistoricalStars.Style.BETA, 0.5F));
    }

    @Test
    void titleTimeDefaultsToModernWithoutAnyWorld() {
        assertEquals(HistoricalStars.Style.MODERN, HistoricalStars.style());
        assertFalse(HistoricalStars.oldStars());
        assertEquals(0, HistoricalStars.rebuildCount());
    }
}

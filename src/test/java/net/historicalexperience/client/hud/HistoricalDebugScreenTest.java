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
package net.historicalexperience.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audit A-06: minecraft.wiki, Debug screen history - "v1.2.3: The coordinates have been added to the debug
 * screen." So the two Alpha profiles before that build must not show a coordinate readout.
 */
class HistoricalDebugScreenTest {
    private static final String P = "historical_experience:";
    private static final List<String> LINES = List.of(
        "Minecraft 1.21.1 (vanilla)",
        "XYZ: 12.500 / 64.00000 / -3.200",
        "Block: 12 64 -4",
        "Chunk: 0 4 -1 in minecraft:overworld",
        "facing: north");

    @AfterEach
    void clear() {
        ActiveVersion.clear();
    }

    @Test
    @DisplayName("coordinates are hidden before Alpha v1.2.3 and shown from it")
    void window() {
        for (String early : new String[] {"alpha_1_0_1", "alpha_1_1_0", "alpha_1_2_0", "alpha_1_2_2a"}) {
            assertTrue(Features.active(Feature.NO_DEBUG_COORDINATES, P + early), early);
            assertTrue(HistoricalDebugScreen.hidesCoordinates(P + early), early);
        }
        for (String later : new String[] {"beta_1_0", "beta_1_7_3", "release_1_0_0"}) {
            assertFalse(Features.active(Feature.NO_DEBUG_COORDINATES, P + later), later);
        }
        assertFalse(Features.active(Feature.NO_DEBUG_COORDINATES, (String) null));
    }

    @Test
    @DisplayName("only the XYZ line is dropped, and unrelated lines keep their order")
    void filterDropsOnlyCoordinates() {
        ActiveVersion.set(P + "alpha_1_2_0");
        List<String> filtered = HistoricalDebugScreen.filter(ActiveVersion.current(), LINES);
        assertEquals(List.of("Minecraft 1.21.1 (vanilla)", "Block: 12 64 -4",
            "Chunk: 0 4 -1 in minecraft:overworld", "facing: north"), filtered);

        ActiveVersion.set(P + "release_1_2_5");
        assertSame(LINES, HistoricalDebugScreen.filter(ActiveVersion.current(), LINES),
            "a version that shows coordinates is untouched");

        ActiveVersion.clear();
        assertSame(LINES, HistoricalDebugScreen.filter(null, LINES), "an ordinary world is untouched");
        assertNull(HistoricalDebugScreen.filter(null, null));
    }
}

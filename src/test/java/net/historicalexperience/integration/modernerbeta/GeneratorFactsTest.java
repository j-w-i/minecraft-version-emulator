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
package net.historicalexperience.integration.modernerbeta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pure diagnostic contract: no registry bootstrap, marker queries or historical fidelity claim. */
class GeneratorFactsTest {
    private static ModernerBetaAdapter.GeneratorFacts facts(int minY, int depth, int seaLevel) {
        return new ModernerBetaAdapter.GeneratorFacts(
            "diagnostic-generator", true, "diagnostic-marker",
            "chunk", "biome", "cave", minY, depth, seaLevel);
    }

    @Test
    void zeroBasedEnvelopeHasKnownMaximum() {
        var facts = facts(0, 128, 64);
        assertTrue(facts.terrainKnown());
        assertEquals(127, facts.maxY());
    }

    @Test
    void negativeBaseEnvelopeHasKnownMaximum() {
        var facts = facts(-64, 192, 64);
        assertTrue(facts.terrainKnown());
        assertEquals(127, facts.maxY());
    }

    @Test
    void unknownMinimumCannotProducePhysicalMaximum() {
        var facts = facts(ModernerBetaAdapter.TERRAIN_UNKNOWN, 128, 64);
        assertFalse(facts.terrainKnown());
        assertEquals(ModernerBetaAdapter.TERRAIN_UNKNOWN, facts.maxY());
    }

    @Test
    void unknownDepthCannotProducePhysicalMaximum() {
        var facts = facts(0, ModernerBetaAdapter.TERRAIN_UNKNOWN, 64);
        assertFalse(facts.terrainKnown());
        assertEquals(ModernerBetaAdapter.TERRAIN_UNKNOWN, facts.maxY());
    }

    @Test
    void unknownSeaLevelKeepsTerrainDiagnosticsUnknown() {
        var facts = facts(0, 128, ModernerBetaAdapter.TERRAIN_UNKNOWN);
        assertFalse(facts.terrainKnown());
        assertEquals(ModernerBetaAdapter.TERRAIN_UNKNOWN, facts.maxY());
    }

    @Test
    void nullGeneratorClassIsRejected() {
        assertThrows(NullPointerException.class, () -> new ModernerBetaAdapter.GeneratorFacts(
            null, true, "diagnostic-marker", "chunk", "biome", "cave", 0, 128, 64));
    }
}

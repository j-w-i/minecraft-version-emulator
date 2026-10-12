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

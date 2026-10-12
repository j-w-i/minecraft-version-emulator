package net.historicalexperience.integration.modernerbeta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppliedPresetEvidenceTest {
    @Test
    void allSettingsMatchRequiresEveryCompleteBlock() {
        for (int mask = 0; mask < 8; mask++) {
            var evidence = evidence((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, true);
            assertEquals(mask == 7, evidence.allSettingsMatch(), "component mask " + mask);
        }
    }

    @Test
    void providerReadinessIsIndependentFromSettingsEquality() {
        // The runtime probe must require both; this record must not mistake listener timing
        // for a change in the resolved configuration.
        assertTrue(evidence(true, true, true, false).allSettingsMatch());
    }

    private static ModernerBetaAdapter.AppliedPresetEvidence evidence(
            boolean chunk, boolean biome, boolean cave, boolean ready) {
        return new ModernerBetaAdapter.AppliedPresetEvidence(
            "moderner_beta:beta", "moderner_beta:beta", "moderner_beta:beta", "moderner_beta:beta",
            12, 6, 2, chunk, biome, cave, ready);
    }
}

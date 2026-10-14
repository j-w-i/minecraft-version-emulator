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

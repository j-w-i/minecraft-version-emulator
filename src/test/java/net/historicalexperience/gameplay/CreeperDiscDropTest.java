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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. The creeper's music-disc drop, from minecraft.wiki's Creeper history:
 * "v1.0.14 Added music discs '13' and 'cat', which are dropped by creepers shot by skeletons"; "1.1 (11w50a) Creepers
 * now drop all music discs, except for '11'"; "1.4.4 (1.4.3) Creepers now drop music discs '11' and 'wait'".
 */
class CreeperDiscDropTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("no disc before Alpha v1.0.14, and 13/cat from it")
    void earlyWindow() {
        assertFalse(Features.active(Feature.CREEPER_DISC_DROPS, P + "alpha_1_0_1"));
        assertTrue(HistoricalDiscDrops.eligible(P + "alpha_1_0_1").isEmpty());
        for (String version : new String[] {"alpha_1_0_14", "beta_1_0", "beta_1_7_3", "release_1_0_0"}) {
            assertTrue(Features.active(Feature.CREEPER_DISC_DROPS, P + version), version);
            assertEquals(List.of("minecraft:music_disc_13", "minecraft:music_disc_cat"),
                HistoricalDiscDrops.eligible(P + version), version);
        }
    }

    @Test
    @DisplayName("Release 1.1 adds every disc except 11; Release 1.4.3's 11 and wait arrive with Release 1.5")
    void laterSets() {
        var fourteen = HistoricalDiscDrops.eligible(P + "release_1_1");
        assertEquals(10, fourteen.size(), "13, cat plus the nine Prerelease-2 discs");
        assertFalse(fourteen.contains("minecraft:music_disc_11"), "11 is not in the 1.1 set");
        assertFalse(fourteen.contains("minecraft:music_disc_wait"));
        assertEquals(fourteen, HistoricalDiscDrops.eligible(P + "release_1_4"),
            "the declared Release 1.4 profile is 1.4.2, before 1.4.3");

        for (String version : new String[] {"release_1_5", "release_1_6_4"}) {
            var set = HistoricalDiscDrops.eligible(P + version);
            assertEquals(12, set.size(), version);
            assertTrue(set.contains("minecraft:music_disc_11"), version);
            assertTrue(set.contains("minecraft:music_disc_wait"), version);
        }
    }

    @Test
    @DisplayName("a pick is always an eligible disc, and an ordinary world gets none")
    void picking() {
        var random = net.minecraft.util.RandomSource.create(7L);
        for (String version : new String[] {"alpha_1_0_14", "release_1_1", "release_1_6_4"}) {
            String pick = HistoricalDiscDrops.pick(P + version, random);
            assertTrue(HistoricalDiscDrops.eligible(P + version).contains(pick), version + " -> " + pick);
        }
        assertNull(HistoricalDiscDrops.pick(P + "alpha_1_0_1", random));
        assertNull(HistoricalDiscDrops.pick(null, random));
        assertNull(HistoricalDiscDrops.pick(P + "beta_1_7_3", null));
        assertFalse(Features.active(Feature.CREEPER_DISC_DROPS, (String) null));
    }
}

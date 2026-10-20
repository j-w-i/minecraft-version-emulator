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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. minecraft.wiki, Java Edition 1.6.1: "Instant Health nerfed by 33% (heals only 4 health points/2 hearts at
 * level I). Regeneration nerfed by 50% (takes 50 ticks/2.5 seconds to restore 1 health point/.5 heart at level I)."
 * So before 1.6.1 the base values are 6 and 25.
 *
 * <p>The profile is resolved through {@link HistoricalServerProfile}, because mob-effect classes are stateless
 * singletons and a dedicated server has no session version.
 */
class PotionStrengthTest {
    private static final String P = "historical_experience:";

    @AfterEach
    void clear() {
        ActiveVersion.clear();
        HistoricalServerProfile.clear();
    }

    @Test
    @DisplayName("the stronger values apply before Release 1.6.1 and the modern values from it")
    void window() {
        for (String early : new String[] {"beta_1_9_pre_3", "release_1_0_0", "release_1_2_5", "release_1_3", "release_1_5"}) {
            assertTrue(Features.active(Feature.PRE_1_6_1_POTION_STRENGTHS, P + early), early);
        }
        for (String later : new String[] {"release_1_6_4"}) {
            assertFalse(Features.active(Feature.PRE_1_6_1_POTION_STRENGTHS, P + later), later);
        }
    }

    @Test
    @DisplayName("the two constants switch on the server profile, and every ordinary world keeps modern values")
    void constants() {
        HistoricalServerProfile.set(P + "release_1_5");
        assertEquals(6, HistoricalPotionStrengths.instantHealthBase(4), "Instant Health I heals 6 before 1.6.1");
        assertEquals(25, HistoricalPotionStrengths.regenerationTickBase(50), "Regeneration ticks 25 before 1.6.1");

        HistoricalServerProfile.set(P + "release_1_6_4");
        assertEquals(4, HistoricalPotionStrengths.instantHealthBase(4), "modern after 1.6.1");
        assertEquals(50, HistoricalPotionStrengths.regenerationTickBase(50));

        HistoricalServerProfile.clear();
        assertEquals(4, HistoricalPotionStrengths.instantHealthBase(4), "an ordinary server keeps the modern value");
        assertEquals(50, HistoricalPotionStrengths.regenerationTickBase(50));

        // The session wins over the recorded server profile, so a client joining a world predicts with that world's rules.
        HistoricalServerProfile.set(P + "release_1_5");
        ActiveVersion.set(P + "release_1_6_4");
        assertEquals(4, HistoricalPotionStrengths.instantHealthBase(4), "session overrides the server note");
        assertEquals(P + "release_1_6_4", HistoricalServerProfile.active());
        assertTrue(HistoricalServerProfile.recorded().endsWith("release_1_5"),
            "the recorded value itself is not overwritten");
    }
}

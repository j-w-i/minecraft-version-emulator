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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. minecraft.wiki, Java Edition 1.3.1: "The 1/4 second (5 game tick) delay between block breaking has been
 * removed for players in Survival." So before Release 1.3 (the 1.3.1 baseline) a survival break within five ticks of
 * the previous one is refused, and from Release 1.3 there is no delay.
 *
 * <p>The delay is applied to every earlier declared build because the wiki documents the removal and no introduction -
 * an inference recorded in the class it lives in, not presented as a dated fact.
 */
class BlockBreakDelayTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("a break within five ticks is refused before Release 1.3 and allowed from it")
    void window() {
        assertTrue(HistoricalBlockBreakDelay.shouldRefuse(P + "release_1_2_5", true, 100L, 96L),
            "4 ticks later is too soon");
        assertFalse(HistoricalBlockBreakDelay.shouldRefuse(P + "release_1_2_5", true, 100L, 95L),
            "exactly 5 ticks is allowed: the delay is 'less than five ticks'");
        assertFalse(HistoricalBlockBreakDelay.shouldRefuse(P + "release_1_3", true, 100L, 99L),
            "Release 1.3 removed the delay");
        assertTrue(HistoricalBlockBreakDelay.shouldRefuse(P + "beta_1_7_3", true, 0L, -1L) == false);
        assertTrue(HistoricalBlockBreakDelay.shouldRefuse(P + "beta_1_7_3", true, 3L, 0L),
            "Beta-era breaks are gated too");
    }

    @Test
    @DisplayName("creative players, first breaks and ordinary worlds are never refused")
    void exemptions() {
        assertFalse(HistoricalBlockBreakDelay.shouldRefuse(P + "release_1_2_5", false, 100L, 96L),
            "creative is exempt");
        assertFalse(HistoricalBlockBreakDelay.shouldRefuse(P + "release_1_2_5", true, 100L, -1L),
            "a first break has no previous tick");
        assertFalse(HistoricalBlockBreakDelay.shouldRefuse(null, true, 100L, 96L), "ordinary world");
        assertFalse(HistoricalBlockBreakDelay.shouldRefuse("moderner_beta:beta", true, 100L, 96L));
    }

    @Test
    @DisplayName("the documented delay is five ticks")
    void delayValue() {
        assertTrue(HistoricalBlockBreakDelay.DELAY_TICKS == 5);
        java.util.UUID player = java.util.UUID.randomUUID();
        assertTrue(HistoricalBlockBreakDelay.lastBreak(player) == -1L, "no history initially");
        HistoricalBlockBreakDelay.recordBreak(player, 42L);
        assertTrue(HistoricalBlockBreakDelay.lastBreak(player) == 42L);
        HistoricalBlockBreakDelay.forget(player);
        assertTrue(HistoricalBlockBreakDelay.lastBreak(player) == -1L);
    }
}

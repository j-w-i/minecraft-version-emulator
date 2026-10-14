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
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Guards what the block veto will and will not refuse, in the first Alpha build.
 *
 * <p>Written after the veto made dates consequential and a live defect appeared: redstone ore was dated to
 * {@code alpha} - which is Alpha v1.1.2_01 - while its own evidence says Alpha v1.0.1. While a date was
 * documentation that cost nothing. The moment {@code WorldGenRegionHistoricalBlockVetoMixin} started enforcing
 * dates, it meant Alpha 1.0.1 worlds refused the ore that build introduced.
 *
 * <p>So these tests assert both directions, because a wrong date is a defect whichever way it points:
 *
 * <ul>
 *   <li>content the build HAD must not be judged anachronistic - or the veto deletes it;</li>
 *   <li>content that arrived later must be - or the veto lets it through.</li>
 * </ul>
 *
 * <p>{@code scripts/audit-proxy-dates.py} finds every entry in this state by comparing each date with the evidence
 * written next to it; this test pins the ones that matter for the oldest declared build.
 */
class AlphaBlockDatingTest {
    private static final String ALPHA_1_0_1 = "historical_experience:alpha_1_0_1";

    @Test
    void whatAlphaOnePointZeroPointOneIntroducedIsNotVetoed() {
        // Straight from that build's deltas: redstone ore and dust, redstone torches, levers, buttons, pressure
        // plates and iron doors. Redstone ore is the one that was wrong.
        assertFalse(HistoricalBlockContent.isAnachronistic(ALPHA_1_0_1, "minecraft:redstone_ore"),
            "Alpha 1.0.1 INTRODUCED redstone ore, so the veto must not refuse it. It did, because the entry was "
                + "dated to alpha - Alpha v1.1.2_01 - which is later than the build it describes");

        for (String ancient : new String[] {
            "minecraft:stone", "minecraft:dirt", "minecraft:spawner", "minecraft:chest", "minecraft:oak_log"}) {
            assertFalse(HistoricalBlockContent.isAnachronistic(ALPHA_1_0_1, ancient),
                ancient + " predates Alpha 1.0.1 and must never be vetoed from it");
        }
    }

    @Test
    void whatArrivedLaterIsStillVetoed() {
        // These are the blocks the veto was observed refusing in a real Alpha 1.0.1 world, and each is dated after
        // that build by the research.
        assertTrue(HistoricalBlockContent.isAnachronistic(ALPHA_1_0_1, "minecraft:pumpkin"),
            "pumpkins arrive with the Halloween Update, Alpha 1.2.0");
        assertTrue(HistoricalBlockContent.isAnachronistic(ALPHA_1_0_1, "minecraft:vine"),
            "vines arrive in Beta 1.8");
        assertTrue(HistoricalBlockContent.isAnachronistic(ALPHA_1_0_1, "minecraft:bee_nest"),
            "bee nests arrive in 1.15");
    }

    @Test
    void theBlocksStillDatedConservativelyAreRecordedAsSuch() {
        // Cactus (Alpha 1.0.6) and clay and sugar cane (Alpha 1.0.11) are dated to "alpha" because those builds are
        // targets that are not declared yet, so "alpha" is the earliest DECLARED version that had them. That is
        // conservative and correct for the declared set: they are absent from 1.0.1 and 1.0.4, which is true.
        for (String laterAlpha : new String[] {
            "minecraft:cactus", "minecraft:clay", "minecraft:sugar_cane"}) {
            assertTrue(HistoricalBlockContent.isAnachronistic(ALPHA_1_0_1, laterAlpha),
                laterAlpha + " must be absent from Alpha 1.0.1");
            assertFalse(HistoricalBlockContent.isAnachronistic("historical_experience:alpha", laterAlpha),
                laterAlpha + " must be present by Alpha v1.1.2_01, or the dating has gone the other way");
        }
    }
}

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
 */package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every declared historical version gets the original fight, and an ordinary world keeps the modern one.
 *
 * <p>The first half is checked against the registry rather than a list, so a profile added later is covered the day it
 * appears; if one is ever declared at or after Release 1.9 this test is the thing that must be revisited.
 */
class HistoricalDragonFightTest {
    @Test
    @DisplayName("every declared version is before the Release 1.9 revamp, so all of them get the original fight")
    void everyDeclaredVersionGetsTheOriginalFight() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(HistoricalDragonFight.originalFight(version.id()),
                version.id() + " is before Release 1.9 and must not get the revamped fight");
        }
    }

    @Test
    @DisplayName("an ordinary world, an unknown id and no id keep the modern fight")
    void ordinaryWorldsAreUntouched() {
        assertFalse(HistoricalDragonFight.originalFight((String) null));
        assertFalse(HistoricalDragonFight.originalFight("minecraft:overworld"));
        assertFalse(HistoricalDragonFight.originalFight("historical_experience:not_a_version"));
    }

    @Test
    @DisplayName("the boundary is Release 1.9 itself")
    void boundary() {
        assertTrue(HistoricalDragonFight.REVAMP_CHRONOLOGY
            > HistoricalVersions.byId("historical_experience:release_1_6_4").orElseThrow().chronology());
    }

    @Test
    @DisplayName("the bar says Boss health before Release 1.4 and the dragon's own name from it")
    void bossBarCaption() {
        for (String id : new String[] {"beta_1_9_pre_6", "release_1_0_0", "release_1_1", "release_1_2_5",
            "release_1_3"}) {
            org.junit.jupiter.api.Assertions.assertEquals("Boss health",
                HistoricalDragonFight.bossBarCaption("historical_experience:" + id), id);
        }
        for (String id : new String[] {"release_1_4", "release_1_5", "release_1_6_4"}) {
            org.junit.jupiter.api.Assertions.assertNull(
                HistoricalDragonFight.bossBarCaption("historical_experience:" + id), id);
        }
        org.junit.jupiter.api.Assertions.assertNull(HistoricalDragonFight.bossBarCaption(null));
    }
    @Test
    @DisplayName("the charge aims where Mojang's 1.0 dragon did: at the feet from 48 blocks, below them when closer")
    void aimHeightIsTheOriginals() {
        // Values worked from the disassembled 1.0 class: targetY = minY + min(10, 0.4 + d/80 - 1).
        org.junit.jupiter.api.Assertions.assertEquals(-0.6, HistoricalDragonFight.aimHeightAbove(0), 1.0e-6);
        org.junit.jupiter.api.Assertions.assertEquals(0.0, HistoricalDragonFight.aimHeightAbove(48), 1.0e-6);
        org.junit.jupiter.api.Assertions.assertEquals(0.65, HistoricalDragonFight.aimHeightAbove(100), 1.0e-6);
        org.junit.jupiter.api.Assertions.assertEquals(10.0, HistoricalDragonFight.aimHeightAbove(5000), 1.0e-6,
            "the offset is capped at ten blocks");
        assertTrue(HistoricalDragonFight.aimHeightAbove(10) < 0, "close in, it aims below the player's feet");
    }

    @Test
    @DisplayName("the original's vertical gain is ten times the modern one")
    void verticalGain() {
        org.junit.jupiter.api.Assertions.assertEquals(10.0,
            HistoricalDragonFight.ORIGINAL_VERTICAL_GAIN / HistoricalDragonFight.MODERN_VERTICAL_GAIN, 1.0e-6);
    }}

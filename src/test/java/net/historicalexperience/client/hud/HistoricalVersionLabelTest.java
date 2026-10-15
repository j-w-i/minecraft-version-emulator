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
package net.historicalexperience.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which versions drew their name in the corner, checked against the profile registry rather than against a copy of
 * the rule, so a profile added later is covered the day it appears.
 *
 * <p>The expectations come from disassembling the original client jars (see {@link HistoricalVersionLabel}), not from
 * the code under test.
 */
class HistoricalVersionLabelTest {
    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId("historical_experience:" + id)
            .orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    @Test
    @DisplayName("every build the jars show drawing the label draws it")
    void alwaysDrawnBuilds() {
        for (String id : List.of("infdev_420", "infdev_611", "alpha_1_0_1", "alpha_1_0_4", "alpha_1_0_6",
            "alpha_1_0_11", "alpha_1_0_14", "alpha_1_0_17", "alpha_1_1_0", "alpha", "alpha_1_2_0", "beta_1_0",
            "beta_1_2", "beta_1_3", "beta_1_4", "beta_1_5", "beta_1_6", "beta_1_8_1")) {
            assertTrue(HistoricalVersionLabel.shownIn(at(id)), id + " drew its name on the plain screen");
        }
    }

    @Test
    @DisplayName("Beta 1.7.3 and the Release builds do not")
    void debugOnlyBuilds() {
        for (String id : List.of("beta_1_7_3", "release_1_0_0", "release_1_1", "release_1_2_5", "release_1_3",
            "release_1_4", "release_1_5", "release_1_6_4")) {
            assertFalse(HistoricalVersionLabel.shownIn(at(id)), id + " kept its name to F3 only");
        }
    }

    @Test
    @DisplayName("the Beta 1.9 prereleases follow the wiki: shown, shown, shown, hidden, shown, shown")
    void prereleaseWindow() {
        assertTrue(HistoricalVersionLabel.shownIn(at("beta_1_9_pre_1")));
        assertTrue(HistoricalVersionLabel.shownIn(at("beta_1_9_pre_2")));
        assertTrue(HistoricalVersionLabel.shownIn(at("beta_1_9_pre_3")));
        assertFalse(HistoricalVersionLabel.shownIn(at("beta_1_9_pre_4")), "Prerelease 4 removed it");
        assertTrue(HistoricalVersionLabel.shownIn(at("beta_1_9_pre_5")), "Prerelease 5 restored it");
        assertTrue(HistoricalVersionLabel.shownIn(at("beta_1_9_pre_6")));
    }

    @Test
    @DisplayName("an ordinary world has no label")
    void noVersionNoLabel() {
        assertFalse(HistoricalVersionLabel.shownIn(null));
    }

    @Test
    @DisplayName("the text is the original's: Infdev names no build, the rest name themselves")
    void text() {
        assertEquals("Minecraft Infdev", HistoricalVersionLabel.textFor(at("infdev_611")));
        assertEquals("Minecraft Infdev", HistoricalVersionLabel.textFor(at("infdev_420")));
        assertEquals("Minecraft Alpha v1.1.2_01", HistoricalVersionLabel.textFor(at("alpha")));
        assertEquals("Minecraft Beta 1.8.1", HistoricalVersionLabel.textFor(at("beta_1_8_1")));
        assertEquals("Minecraft Beta 1.6", HistoricalVersionLabel.textFor(at("beta_1_6")));
    }
}
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the sneaking boundary, which the project had nowhere and the owner supplied.
 *
 * <p>The owner reported, from playing an Alpha 1.0.1 world: "i can also still sneak way before sneaking was added",
 * and then dated it: sneaking arrives in <b>Alpha 1.1.0</b>. The research document is silent on the build - its
 * only sneak reference is Beta 1.4's "sneaking while climbing holds position", which implies sneaking existed by
 * then without dating its arrival - so the owner's correction is the best evidence available and is recorded as
 * such rather than dressed up as a citation.
 */
class SneakingBoundaryTest {
    private static final int ERA_ALPHA = 400_000;

    @Test
    void everyAlphaBuildBeforeOneOneZeroLacksSneaking() {
        for (String id : new String[] {
            "historical_experience:alpha_1_0_1",
            "historical_experience:alpha_1_0_4"}) {
            assertFalse(HistoricalGameplayTweaks.sneakingAvailable(id),
                id + " predates Alpha 1.1.0, so the crouch must do nothing there");
        }
    }

    @Test
    void theDeclaredAlphaProfileHasSneaking() {
        // Alpha v1.1.2_01 is past the boundary, which is what makes this a boundary rather than a blanket refusal
        // for the whole era.
        assertTrue(HistoricalGameplayTweaks.sneakingAvailable("historical_experience:alpha"),
            "Alpha v1.1.2_01 is after Alpha 1.1.0 and must be able to sneak");
    }

    @Test
    void theBoundaryLiteralFollowsTheRegistrarsOwnNumbering() {
        assertEquals(HistoricalVersions.chronology(ERA_ALPHA, 1, 1, 0),
            HistoricalGameplayTweaks.sneakingBoundaryChronology(),
            "the boundary is a literal because Alpha 1.1.0 is not declared yet. This ties it to the registry's "
                + "scheme so it cannot drift into gating the wrong build");

        assertTrue(HistoricalGameplayTweaks.sneakingBoundaryChronology()
                > HistoricalVersions.chronology(ERA_ALPHA, 1, 0, 17),
            "Alpha 1.0.17 must be below the sneak boundary: sneaking arrives in 1.1.0, after every 1.0.x build");
    }

    @Test
    void unresolvedVersionsNeverGetSneaking() {
        assertFalse(HistoricalGameplayTweaks.sneakingAvailable(null));
        assertFalse(HistoricalGameplayTweaks.sneakingAvailable("historical_experience:no_such_version"));
    }
}

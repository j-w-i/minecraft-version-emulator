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

import net.historicalexperience.version.Milestones;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the offhand boundary: no supported version has one.
 *
 * <p>Reported by the owner: "the offhand is available in all versions currently and should not be available at all,
 * not in the gui or via a hotkey". Dual wielding arrives in Release 1.9, and the newest version here is Release
 * 1.6.4, so the answer is the same for every version this project declares.
 *
 * <p>The tests assert that in both directions, because "no version has it" is only half the claim:
 *
 * <ul>
 *   <li>every declared version must refuse it;</li>
 *   <li>an unresolved version must NOT refuse it, or an ordinary world - one this mod is not managing - would lose
 *       an offhand it is entitled to.</li>
 * </ul>
 *
 * <p>The second matters more than it looks. A null session is what a world with no historical metadata produces,
 * and this project's rule is that ordinary worlds are left alone.
 */
class OffhandBoundaryTest {
    private static final int ERA_RELEASE = 600_000;

    @Test
    void everyDeclaredVersionRefusesTheOffhand() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(Features.active(Feature.NO_OFFHAND, version.id()),
                version.id() + " (" + version.displayName() + ") predates Release 1.9, so it must have no offhand");
        }
    }

    @Test
    void anOrdinaryWorldKeepsItsOffhand() {
        assertFalse(Features.active(Feature.NO_OFFHAND, (String) null),
            "a world with no historical metadata is an ordinary world, and removing its offhand would break "
                + "vanilla play rather than emulate a version");
        assertFalse(Features.active(Feature.NO_OFFHAND, "historical_experience:no_such_version"),
            "an unresolvable version is not a claim about the offhand");
    }

    @Test
    void theBoundaryLiteralFollowsTheRegistrarsOwnNumbering() {
        assertEquals(HistoricalVersions.chronology(ERA_RELEASE, 1, 9, 0),
            Milestones.RELEASE_1_9.key(),
            "the offhand arrives in Release 1.9, which is outside the declared matrix, so the boundary is a literal "
                + "tied to the registry's scheme. This asserts it cannot drift into gating the wrong release");

        assertTrue(Milestones.RELEASE_1_9.key()
                > HistoricalVersions.byId("historical_experience:release_1_6_4").orElseThrow().chronology(),
            "Release 1.6.4, the newest version supported, must sit below the offhand boundary - which is why every "
                + "declared version refuses it");
    }
}

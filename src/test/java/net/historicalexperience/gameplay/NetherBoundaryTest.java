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
 * Guards the Nether boundary, which the owner asked about directly.
 *
 * <p>"I assume you've already blocked nether portals until alpha 1.2" - and it was not blocked. The block veto runs
 * on the generation path, so it stops netherrack, glowstone and portal blocks from being generated, and does nothing
 * about a player placing obsidian and lighting it. In an Alpha 1.0.1 world that produced a working portal into a
 * dimension the version never had.
 *
 * <p>The Nether arrives with the Halloween Update, Alpha 1.2.0, so every declared Alpha and Infdev build must refuse
 * both creating a portal and travelling through one, and every declared Beta or Release build must allow both.
 */
class NetherBoundaryTest {
    private static final int ERA_ALPHA = 400_000;

    @Test
    void everyDeclaredBuildBeforeAlphaOnePointTwoHasNoNether() {
        for (String id : new String[] {
            "historical_experience:infdev_420",
            "historical_experience:infdev_611",
            "historical_experience:alpha_1_0_1",
            "historical_experience:alpha_1_0_4",
            "historical_experience:alpha_1_0_6"}) {
            assertFalse(!Features.active(Feature.NO_NETHER, id),
                id + " predates Alpha 1.2.0, so no portal may be lit and no travel may happen there");
        }
    }

    @Test
    void theDeclaredAlphaProfileAlsoHasNoNether() {
        // Alpha v1.1.2_01 is BELOW the boundary, which is the case a reader is most likely to get wrong: the Alpha
        // profile is the newest declared Alpha build and still predates the Halloween Update by one build.
        assertFalse(!Features.active(Feature.NO_NETHER, "historical_experience:alpha"),
            "Alpha v1.1.2_01 predates Alpha 1.2.0 and must have no Nether");
    }

    @Test
    void everyDeclaredBetaAndReleaseBuildHasTheNether() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            if (version.chronology() < 500_000) {
                continue;
            }
            assertTrue(!Features.active(Feature.NO_NETHER, version.id()),
                version.id() + " is after Alpha 1.2.0 and must have the Nether");
        }
    }

    @Test
    void theBoundaryLiteralFollowsTheRegistrarsOwnNumbering() {
        assertEquals(HistoricalVersions.chronology(ERA_ALPHA, 1, 2, 0),
            Milestones.ALPHA_1_2_0.key(),
            "the Nether arrives in Alpha 1.2.0, a target that is not declared yet, so the boundary is a literal tied "
                + "to the registry's scheme");

        assertTrue(Milestones.ALPHA_1_2_0.key()
                > HistoricalVersions.byId("historical_experience:alpha").orElseThrow().chronology(),
            "Alpha v1.1.2_01 must sit below the Nether boundary");

        // The portal mixins cancel when this is false and the session is null in an ordinary world, so the
        // answer for "no historical version" MUST be true or the gate would disable portals in every ordinary world.
        assertTrue(!Features.active(Feature.NO_NETHER, (String) null),
            "an ordinary world is not a historical version and this gate must not touch it");
        assertTrue(!Features.active(Feature.NO_NETHER, "moderner_beta:beta"));
        assertTrue(!Features.active(Feature.NO_NETHER, "example:not_a_version"));
    }
}

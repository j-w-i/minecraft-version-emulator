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

import java.util.Set;
import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersions;

/**
 * Pins the Beta mob timeline, which corrected two assumptions of mine.
 *
 * <p>I had assumed squid were ancient, present in Beta 1.0 by default. The research says otherwise, and says it
 * plainly:
 *
 * <pre>
 *   Beta 1.2 | squid (10 health, drops ink sac, no sounds, could be milked in this build)
 *   Beta 1.4 | wolves (neutral, common in taiga, 10 health, tame via bones at 1/3 chance per bone)
 * </pre>
 *
 * <p>So Beta 1.0 has neither, Beta 1.2 gains the squid, and Beta 1.4 gains the wolf and thereby reaches the
 * Beta 1.7.3 set. Deriving each set by subtraction from the 1.7.3 set rather than listing them keeps that
 * relationship visible; these tests assert it, so a wrong date cannot quietly survive.
 */
class BetaMobSetTest {
    private static final String BETA_1_0 = "historical_experience:beta_1_0";
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";

    private static Set<String> mobsOf(String versionId) {
        return HistoricalVersions.byId(versionId).orElseThrow(
            () -> new AssertionError(versionId + " is not declared")).rules().allowedNaturalSpawns();
    }

    @Test
    void betaOnePointZeroHasNeitherSquidNorWolves() {
        Set<String> mobs = mobsOf(BETA_1_0);

        assertFalse(mobs.contains("minecraft:squid"),
            "squid arrive in Beta 1.2, so Beta 1.0 must not have them - I assumed they were ancient and was wrong");
        assertFalse(mobs.contains("minecraft:wolf"),
            "wolves arrive in Beta 1.4");

        // And the mobs that DO predate it must still be there, or the subtraction has gone too far.
        for (String ancient : new String[] {
            "minecraft:creeper", "minecraft:skeleton", "minecraft:spider", "minecraft:zombie",
            "minecraft:pig", "minecraft:sheep", "minecraft:cow", "minecraft:chicken",
            "minecraft:slime", "minecraft:ghast", "minecraft:zombified_piglin"}) {
            assertTrue(mobs.contains(ancient),
                ancient + " predates Beta 1.0 and must be allowed, or a Beta world spawns almost nothing");
        }
    }

    @Test
    void theOnePointSevenPointThreeSetIsBetaOnePointZeroPlusSquidAndWolf() {
        Set<String> difference = new java.util.TreeSet<>(mobsOf(BETA_1_7_3));
        difference.removeAll(mobsOf(BETA_1_0));

        assertEquals(Set.of("minecraft:squid", "minecraft:wolf"), difference,
            "Beta 1.7.3 differs from Beta 1.0 by exactly the squid (Beta 1.2) and the wolf (Beta 1.4). If that "
                + "stops holding, one of the two dates is wrong or a mob has been added to one set only");
    }

    @Test
    void betaOnePointZeroRestrictsSpawnsAtAll() {
        // The point of declaring it through the rules-carrying helper: before this, Release and prerelease builds
        // declared with terrainOnly had NO restriction and spawned modern 1.21.1 mobs.
        assertTrue(HistoricalVersions.byId(BETA_1_0).orElseThrow().rules().restrictsSpawns(),
            "Beta 1.0 has a researched mob set and must restrict, or modern mobs spawn there");
    }
}

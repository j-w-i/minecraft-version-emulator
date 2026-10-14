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
package net.historicalexperience.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the two villager boundaries, both of which were found by the owner rather than by a test.
 *
 * <p>The first: villages exist from Beta 1.8.1 and are EMPTY until villagers arrive in Beta 1.9 Prerelease 1, and
 * this port originally placed no entities anywhere at all rather than make spawning conditional. The owner found it -
 * "villagers do not spawn in villages past 1.8 when they are supposed to, or at least they dont spawn in release 1.0".
 *
 * <p>The second: professions and trading are Release 1.3 content, so a Release 1.0 villager must be a plain villager.
 * That was correct by omission until it was gated, which is the kind of correctness that expires quietly.
 *
 * <p>The profession LIST is asserted here as ids, deliberately: it can be checked without a bootstrapped registry,
 * which is what makes this a unit test rather than another probe diagnostic.
 */
class VillagerBoundaryTest {
    @Test
    void villagesAreEmptyBeforePrereleaseOneAndInhabitedFromIt() {
        int pre1 = HistoricalVillageRules.BETA_1_9_PRE_1_CHRONOLOGY;

        // Empty villages: Beta 1.8.1 is the build villages arrive in, and it predates the villagers.
        assertTrue(HistoricalVersions.byId("historical_experience:beta_1_8_1").orElseThrow().chronology() < pre1,
            "Beta 1.8.1 villages must be EMPTY - villagers arrive later");

        // Inhabited: every prerelease from Pre1 and every Release build.
        for (String id : java.util.List.of(
            "historical_experience:beta_1_9_pre_1",
            "historical_experience:beta_1_9_pre_2",
            "historical_experience:beta_1_9_pre_3",
            "historical_experience:beta_1_9_pre_4",
            "historical_experience:beta_1_9_pre_5",
            "historical_experience:beta_1_9_pre_6",
            "historical_experience:release_1_0_0",
            "historical_experience:release_1_6_4")) {
            assertTrue(HistoricalVersions.byId(id).orElseThrow().chronology() >= pre1,
                id + " must have villagers: Pre1 is where villages became inhabited");
        }
    }

    @Test
    void professionsStartAtReleaseOnePointThree() {
        int release13 = HistoricalVillageRules.RELEASE_1_3_CHRONOLOGY;

        // Release 1.0 is the owner's report: villagers, but nothing to trade.
        assertTrue(HistoricalVersions.byId("historical_experience:release_1_0_0").orElseThrow().chronology()
                < release13,
            "Release 1.0 predates trading, so its villagers must have no profession");
        assertTrue(HistoricalVersions.byId("historical_experience:release_1_2_5").orElseThrow().chronology()
                < release13,
            "Release 1.2.5 predates trading too");
        assertTrue(HistoricalVersions.byId("historical_experience:release_1_3").orElseThrow().chronology()
                >= release13,
            "Release 1.3 is where trading arrives and villagers gain professions");
    }

    @Test
    void theProfessionListIsTheOneReleaseOnePointThreeHad() {
        List<String> professions = HistoricalVillageRules.RELEASE_1_3_PROFESSIONS;

        // The five that build had, with the single blacksmith represented by the three smiths modern Minecraft split
        // it into. Anything else - cartographer, fisherman, fletcher, leatherworker, mason, shepherd - did not exist.
        for (String absent : List.of("minecraft:cartographer", "minecraft:fisherman", "minecraft:fletcher",
                "minecraft:leatherworker", "minecraft:mason", "minecraft:shepherd", "minecraft:nitwit",
                "minecraft:none")) {
            assertFalse(professions.contains(absent),
                absent + " did not exist in Release 1.3 and must not be offered there");
        }

        for (String present : List.of("minecraft:farmer", "minecraft:librarian", "minecraft:cleric",
                "minecraft:butcher")) {
            assertTrue(professions.contains(present), present + " existed in Release 1.3");
        }

        // The blacksmith: one profession then, three now. At least one must be listed or the era's smith is missing.
        assertTrue(professions.contains("minecraft:armorer") || professions.contains("minecraft:toolsmith")
                || professions.contains("minecraft:weaponsmith"),
            "Release 1.3 had a blacksmith; modern Minecraft split it into armorer, toolsmith and weaponsmith, and the "
                + "era's smith must be represented by at least one of them");

        assertEquals(professions.size(), professions.stream().distinct().count(),
            "the profession list must not contain duplicates, or one profession is over-represented");
    }
}

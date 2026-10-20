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
 * Level: unit. "Dropped nothing" is a different fact from "has no rule".
 *
 * <p>research/version-transitions.json: Beta 1.9 Prerelease 1 is where "blaze rods, ghast tears and Nether wart are
 * obtainable but have no use yet", and its fortress row says those items arrive there. So ghasts - which arrive with
 * the Nether in Alpha v1.2.0 - dropped nothing at all until Prerelease 1, while the modern table gives them a tear and
 * gunpowder.
 */
class NoDropEntitiesTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("a ghast drops nothing before Beta 1.9 Prerelease 1")
    void ghastDropsNothingBeforePre1() {
        for (String early : new String[] {"alpha_1_2_0", "alpha_1_2_2a", "beta_1_0", "beta_1_7_3"}) {
            assertTrue(HistoricalLootRules.dropsNothing(P + early, "minecraft:ghast"), early);
        }
        for (String later : new String[] {"beta_1_9_pre_1", "beta_1_9_pre_6", "release_1_0_0", "release_1_6_4"}) {
            assertFalse(HistoricalLootRules.dropsNothing(P + later, "minecraft:ghast"),
                later + " has ghast tears");
        }
    }

    @Test
    @DisplayName("the rule only ever suppresses what it names, and ordinary worlds are untouched")
    void scope() {
        assertFalse(HistoricalLootRules.dropsNothing(P + "beta_1_7_3", "minecraft:zombie"),
            "a mob with a rule is not affected");
        assertFalse(HistoricalLootRules.dropsNothing(P + "release_1_6_4", "minecraft:chicken"));
        assertFalse(HistoricalLootRules.dropsNothing(null, "minecraft:ghast"));
        assertFalse(HistoricalLootRules.dropsNothing(P + "beta_1_7_3", null));
        assertFalse(HistoricalLootRules.dropsNothing("moderner_beta:beta", "minecraft:ghast"));
    }

    @Test
    @DisplayName("the affected profiles are exactly the ones before Prerelease 1")
    void affectedProfiles() {
        var profiles = HistoricalLootRules.profilesWithNoDrops();
        assertTrue(profiles.contains(P + "beta_1_7_3"));
        assertTrue(profiles.contains(P + "alpha_1_2_0"));
        assertFalse(profiles.contains(P + "beta_1_9_pre_1"));
        assertFalse(profiles.contains(P + "release_1_6_4"));
    }
}

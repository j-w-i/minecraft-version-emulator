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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The dates behind chest-loot filtering, checked against the registry rather than against a copy of the rule.
 *
 * <p>Every dated item must name a version this project declares: a date written against a version that does not
 * exist would throw while the class loads, and a date against the wrong one would quietly filter the wrong era.
 */
class HistoricalItemAvailabilityTest {
    @Test
    @DisplayName("every dated item names a declared version")
    void everyDateNamesADeclaredVersion() {
        for (var entry : HistoricalItemAvailability.all().entrySet()) {
            HistoricalItemAvailability.IntroducedIn introduced = entry.getValue();
            if (introduced.chronology() == Integer.MAX_VALUE) {
                assertTrue(introduced.versionId().equals("none"),
                    entry.getKey() + " is barred from every version and must say so");
                continue;
            }
            assertTrue(HistoricalVersions.byId("historical_experience:" + introduced.versionId()).isPresent(),
                entry.getKey() + " is dated to " + introduced.versionId() + ", which is not a declared version");
        }
    }

    @Test
    @DisplayName("an item does not exist before its date and does exist from it")
    void boundaryHolds() {
        assertFalse(HistoricalItemAvailability.existsIn("historical_experience:beta_1_7_3", "minecraft:emerald"),
            "emeralds arrive with villagers in Release 1.3");
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:release_1_3", "minecraft:emerald"));
        assertFalse(HistoricalItemAvailability.existsIn("historical_experience:release_1_2_5", "minecraft:carrot"));
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:release_1_4", "minecraft:carrot"));
        assertFalse(HistoricalItemAvailability.existsIn("historical_experience:release_1_5",
            "minecraft:iron_horse_armor"), "horse armour arrives in Release 1.6.1");
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:release_1_6_4",
            "minecraft:iron_horse_armor"));
    }

    @Test
    @DisplayName("an undated item is allowed, and a barred one never is")
    void unknownIsAllowedNeverIsNot() {
        // The direction matters: an undated item must not silently vanish from a chest.
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:alpha", "minecraft:apple"));
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:beta_1_7_3", "minecraft:golden_apple"));
        for (String version : new String[] {"alpha", "beta_1_7_3", "beta_1_8_1", "release_1_0_0", "release_1_6_4"}) {
            assertFalse(HistoricalItemAvailability.existsIn("historical_experience:" + version, "minecraft:bamboo"),
                "bamboo is 1.14 content and no declared version may contain it");
            assertFalse(HistoricalItemAvailability.existsIn("historical_experience:" + version,
                "minecraft:enchanted_golden_apple"));
        }
    }

    @Test
    @DisplayName("an ordinary world is never filtered")
    void unknownWorldAllows() {
        assertTrue(HistoricalItemAvailability.existsIn(null, "minecraft:bamboo"));
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:no_such_version",
            "minecraft:bamboo"));
    }

    @Test
    @DisplayName("the table is not empty")
    void tableIsPopulated() {
        assertTrue(HistoricalItemAvailability.size() >= 40,
            "the chest tables name dozens of items that no declared version had; got "
                + HistoricalItemAvailability.size());
        assertEquals(HistoricalItemAvailability.all().size(), HistoricalItemAvailability.size());
    }
}
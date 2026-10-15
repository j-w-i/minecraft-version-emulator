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
 */package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The selector's overview data, checked against the real catalogue so the panel can never be empty by accident.
 *
 * <p>An overview keyed by an id the catalogue does not use would silently show nothing for that version, and nothing
 * would fail. These tests tie the two together.
 */
class VersionOverviewTest {
    @Test
    @DisplayName("every implemented version in the catalogue has an overview")
    void everyImplementedTargetHasAnOverview() {
        for (HistoricalVersionCatalogue.Entry entry : HistoricalVersionCatalogue.entries()) {
            if (!entry.selectable() || !entry.target()) {
                // The two Infdev builds and Alpha v1.1.2_01 are outside the researched matrix and are not claimed.
                continue;
            }
            assertTrue(VersionOverview.forVersion(entry.key()).isPresent(),
                entry.displayName() + " (" + entry.key() + ") has no overview, so its panel would be blank");
        }
    }

    @Test
    @DisplayName("a version with a recorded overview shows what it introduced")
    void overviewCarriesItsLists() {
        VersionOverview.Overview alpha = VersionOverview.forVersion("historical_experience:alpha_1_0_1")
            .orElseThrow();
        assertFalse(alpha.introduced().isEmpty());
        assertTrue(alpha.introduced().stream().anyMatch(line -> line.contains("redstone")),
            "Alpha 1.0.1 is where redstone was added");
        assertFalse(alpha.worldgen().isEmpty());
    }

    @Test
    @DisplayName("an unknown or planned key has no overview rather than a made-up one")
    void unknownKeysShowNothing() {
        assertTrue(VersionOverview.forVersion("historical_experience:planned_beta_9_9_9").isEmpty());
        assertTrue(VersionOverview.forVersion("").isEmpty());
        assertTrue(VersionOverview.forVersion("historical_experience:infdev_611").isEmpty());
    }

    @Test
    @DisplayName("a malformed file is rejected without taking the parser state with it")
    void malformedInputThrowsRatherThanHalfLoading() {
        try {
            VersionOverview.parse(new java.io.StringReader("[1, 2, 3]"));
        } catch (RuntimeException expected) {
            assertEquals(IllegalStateException.class, expected.getClass(),
                "a JSON array is not the overview document");
            return;
        }
        throw new AssertionError("a JSON array must not parse as the overview document");
    }
}
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
package net.historicalexperience.world.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the persisted identifier value type.
 *
 * <p>Level: unit. No Minecraft runtime, no filesystem.
 */
class HistoricalIdentifierTest {
    @Test
    @DisplayName("valid identifiers parse into namespace and path")
    void validIdentifiersParse() {
        HistoricalIdentifier id = HistoricalIdentifier.parse("moderner_beta:beta");

        assertEquals("moderner_beta", id.namespace());
        assertEquals("beta", id.path());
        assertEquals("moderner_beta:beta", id.value());
        assertEquals("moderner_beta:beta", id.toString());
    }

    @Test
    @DisplayName("paths may contain dots, dashes, underscores, digits and slashes")
    void pathCharacterSet() {
        assertTrue(HistoricalIdentifier.isValid("historical_experience:beta_1_7_3"));
        assertTrue(HistoricalIdentifier.isValid("moderner_beta:release_1_12_2"));
        assertTrue(HistoricalIdentifier.isValid("a:b.c-d_e/1"));
    }

    @Test
    @DisplayName("invalid identifiers are rejected with an explanatory message")
    void invalidIdentifiersAreRejected() {
        assertFalse(HistoricalIdentifier.isValid(null));
        assertFalse(HistoricalIdentifier.isValid(""));
        assertFalse(HistoricalIdentifier.isValid("no_namespace"));
        assertFalse(HistoricalIdentifier.isValid(":missing_namespace"));
        assertFalse(HistoricalIdentifier.isValid("missing_path:"));
        assertFalse(HistoricalIdentifier.isValid("a:b:c"));
        assertFalse(HistoricalIdentifier.isValid("UPPER:beta"));
        assertFalse(HistoricalIdentifier.isValid("historical_experience:Beta"));
        assertFalse(HistoricalIdentifier.isValid("historical_experience:beta path"));
        assertFalse(HistoricalIdentifier.isValid("historical_experience:/beta"));
        assertFalse(HistoricalIdentifier.isValid("historical_experience:beta/"));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
            () -> HistoricalIdentifier.parse("no_namespace"));
        assertTrue(thrown.getMessage().contains("namespace:path"),
            "message should describe the expected form: " + thrown.getMessage());
    }

    @Test
    @DisplayName("equality and ordering are value based")
    void equalityAndOrdering() {
        HistoricalIdentifier a = HistoricalIdentifier.parse("moderner_beta:beta");
        HistoricalIdentifier b = HistoricalIdentifier.parse("moderner_beta:beta");
        HistoricalIdentifier c = HistoricalIdentifier.parse("moderner_beta:skylands");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.compareTo(c) < 0);
        assertFalse(a.equals(c));
    }
}

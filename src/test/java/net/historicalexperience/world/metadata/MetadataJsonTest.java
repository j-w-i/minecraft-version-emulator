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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for metadata JSON encoding and decoding.
 *
 * <p>Level: unit. No Minecraft runtime, no filesystem.
 */
class MetadataJsonTest {
    private static HistoricalMetadata beta173() {
        return HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            HistoricalIdentifier.parse("moderner_beta:beta"),
            "historical_experience",
            1_700_000_000_000L);
    }

    @Test
    @DisplayName("roundtrip: encode then decode preserves every field")
    void roundtripPreservesEveryField() throws Exception {
        HistoricalMetadata original = beta173();

        String json = MetadataJson.encode(original);
        HistoricalMetadata decoded = MetadataJson.decode(json);

        assertEquals(original, decoded);
        assertEquals(original.schema(), decoded.schema());
        assertEquals(original.profileId(), decoded.profileId());
        assertEquals(original.generationPreset(), decoded.generationPreset());
        assertEquals(original.revision(), decoded.revision());
        assertEquals(original.history(), decoded.history());
    }

    @Test
    @DisplayName("roundtrip: advances survive a second roundtrip with stable ordering")
    void roundtripIsStableAcrossRepeatedEncodes() throws Exception {
        HistoricalMetadata first = beta173();
        HistoricalMetadata second = first.advance(
            HistoricalIdentifier.parse("moderner_beta:beta"), "historical_experience",
            "test revision", 1_700_000_100_000L);
        HistoricalMetadata third = second.advance(
            null, "historical_experience", "profile unchanged, preset unchanged",
            1_700_000_200_000L);

        String encodedOnce = MetadataJson.encode(third);
        HistoricalMetadata decoded = MetadataJson.decode(encodedOnce);
        String encodedTwice = MetadataJson.encode(decoded);

        assertEquals(encodedOnce, encodedTwice);
        assertEquals(3L, decoded.revision());
        assertEquals(3, decoded.history().size());
        assertEquals(1L, decoded.history().get(0).revision());
        assertEquals(2L, decoded.history().get(1).revision());
        assertEquals(3L, decoded.history().get(2).revision());
    }

    @Test
    @DisplayName("decode rejects an unknown schema version (fail closed)")
    void decodeRejectsUnknownSchema() {
        String json = MetadataJson.encode(beta173()).replace("\"schema\":1", "\"schema\":2");

        MetadataException thrown = assertThrows(MetadataException.class,
            () -> MetadataJson.decode(json));
        assertTrue(thrown.getMessage().contains("schema"),
            "message should name the offending field: " + thrown.getMessage());
    }

    @Test
    @DisplayName("decode rejects a missing required field")
    void decodeRejectsMissingField() {
        String json = MetadataJson.encode(beta173())
            .replace("\"generationPreset\":\"moderner_beta:beta\",", "");

        MetadataException thrown = assertThrows(MetadataException.class,
            () -> MetadataJson.decode(json));
        assertTrue(thrown.getMessage().contains("generationPreset"),
            "message should name the missing field: " + thrown.getMessage());
    }

    @Test
    @DisplayName("decode rejects an unknown field rather than ignoring it")
    void decodeRejectsUnknownField() {
        String json = MetadataJson.encode(beta173())
            .replace("{\"schema\":1", "{\"schema\":1,\"surprise\":true");

        MetadataException thrown = assertThrows(MetadataException.class,
            () -> MetadataJson.decode(json));
        assertTrue(thrown.getMessage().contains("surprise"),
            "message should name the unknown field: " + thrown.getMessage());
    }

    @Test
    @DisplayName("decode rejects malformed JSON")
    void decodeRejectsMalformedJson() {
        assertThrows(MetadataException.class, () -> MetadataJson.decode("{\"schema\":"));
        assertThrows(MetadataException.class, () -> MetadataJson.decode("not json at all"));
        assertThrows(MetadataException.class, () -> MetadataJson.decode(""));
        assertThrows(MetadataException.class, () -> MetadataJson.decode("[]"));
    }

    @Test
    @DisplayName("decode rejects a field with the wrong JSON type")
    void decodeRejectsWrongTypes() {
        assertThrows(MetadataException.class,
            () -> MetadataJson.decode(MetadataJson.encode(beta173())
                .replace("\"revision\":1", "\"revision\":\"one\"")));
        assertThrows(MetadataException.class,
            () -> MetadataJson.decode(MetadataJson.encode(beta173())
                .replace("\"schema\":1", "\"schema\":\"1\"")));
        assertThrows(MetadataException.class,
            () -> MetadataJson.decode(MetadataJson.encode(beta173())
                .replace("\"history\":[", "\"history\":{")));
    }

    @Test
    @DisplayName("decode rejects an invalid identifier in a persisted field")
    void decodeRejectsInvalidIdentifier() {
        assertThrows(MetadataException.class,
            () -> MetadataJson.decode(MetadataJson.encode(beta173())
                .replace("moderner_beta:beta", "not an identifier")));
        assertThrows(MetadataException.class,
            () -> MetadataJson.decode(MetadataJson.encode(beta173())
                .replace("historical_experience:beta_1_7_3", "no-namespace")));
    }

    @Test
    @DisplayName("decode rejects history that is not strictly increasing")
    void decodeRejectsNonIncreasingHistory() {
        String json = "{\"schema\":1,\"profileId\":\"historical_experience:beta_1_7_3\","
            + "\"generationPreset\":\"moderner_beta:beta\",\"revision\":2,\"history\":["
            + "{\"revision\":2,\"timestamp\":1,\"actor\":\"a\",\"note\":\"n\"},"
            + "{\"revision\":1,\"timestamp\":2,\"actor\":\"a\",\"note\":\"n\"}]}";

        MetadataException thrown = assertThrows(MetadataException.class,
            () -> MetadataJson.decode(json));
        assertTrue(thrown.getMessage().contains("strictly increasing"),
            "message should explain ordering: " + thrown.getMessage());
    }

    @Test
    @DisplayName("decode rejects a revision that does not match the history head")
    void decodeRejectsRevisionHistoryMismatch() {
        String json = "{\"schema\":1,\"profileId\":\"historical_experience:beta_1_7_3\","
            + "\"generationPreset\":\"moderner_beta:beta\",\"revision\":9,\"history\":["
            + "{\"revision\":1,\"timestamp\":1,\"actor\":\"a\",\"note\":\"n\"}]}";

        MetadataException thrown = assertThrows(MetadataException.class,
            () -> MetadataJson.decode(json));
        assertTrue(thrown.getMessage().contains("highest history revision"),
            "message should explain the mismatch: " + thrown.getMessage());
    }

    @Test
    @DisplayName("record invariants reject an empty history")
    void recordRejectsEmptyHistory() {
        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalMetadata(1,
                HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
                HistoricalIdentifier.parse("moderner_beta:beta"),
                1,
                List.of()));
    }

    @Test
    @DisplayName("record invariants reject a blank actor or note")
    void recordRejectsBlankAttribution() {
        assertThrows(IllegalArgumentException.class, () -> new MetadataRevision(1, 1, " ", "note"));
        assertThrows(IllegalArgumentException.class, () -> new MetadataRevision(1, 1, "actor", " "));
        assertThrows(IllegalArgumentException.class, () -> new MetadataRevision(0, 1, "a", "n"));
    }
}

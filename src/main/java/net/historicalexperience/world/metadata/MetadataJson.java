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

/**
 * JSON codec for {@link HistoricalMetadata}.
 *
 * <p>Implemented directly against the Gson streaming API so that reading is strict and produces
 * actionable diagnostics: unknown top level fields are rejected rather than ignored, every field
 * is type checked, and no partial result is ever produced. A document this codec cannot fully
 * understand must be rejected by callers, never repaired.
 */
public final class MetadataJson {
    private MetadataJson() {
    }

    /**
     * Serialises metadata to a compact, stable JSON document.
     *
     * <p>Field order is fixed and history entries are emitted oldest first so that byte-for-byte
     * comparisons of successive writes are meaningful in tests and diffs.
     */
    public static String encode(HistoricalMetadata metadata) {
        java.io.StringWriter out = new java.io.StringWriter();
        com.google.gson.stream.JsonWriter writer = new com.google.gson.stream.JsonWriter(out);
        writer.setSerializeNulls(false);
        try {
            writer.beginObject();
            writer.name("schema").value(metadata.schema());
            writer.name("profileId").value(metadata.profileId().value());
            writer.name("generationPreset").value(metadata.generationPreset().value());
            writer.name("revision").value(metadata.revision());
            writer.name("history").beginArray();
            for (MetadataRevision entry : metadata.history()) {
                writer.beginObject();
                writer.name("revision").value(entry.revision());
                writer.name("timestamp").value(entry.timestamp());
                writer.name("actor").value(entry.actor());
                writer.name("note").value(entry.note());
                writer.endObject();
            }
            writer.endArray();
            // Omit an absent patch entirely: existing canonical metadata bytes and finite-origin hashes stay stable.
            if (!metadata.snapshotPatchId().isEmpty()) writer.name("snapshotPatchId").value(metadata.snapshotPatchId());
            writer.endObject();
            writer.flush();
            return out.toString();
        } catch (java.io.IOException e) {
            // Writing to a StringWriter cannot fail; wrap defensively rather than leaking.
            throw new IllegalStateException("failed to encode historical metadata", e);
        }
    }

    /**
     * Parses metadata from JSON.
     *
     * @throws MetadataException when the document is malformed, has an unknown schema, or omits
     *                           or mistypes a required field
     */
    public static HistoricalMetadata decode(String json) throws MetadataException {
        if (json == null || json.isBlank()) {
            throw new MetadataException("historical metadata document is empty");
        }

        Integer schema = null;
        String profileId = null;
        String generationPreset = null;
        String snapshotPatchId = "";
        Long revision = null;
        java.util.List<MetadataRevision> history = null;

        try (com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(
            new java.io.StringReader(json))) {
            reader.setLenient(false);

            if (reader.peek() != com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
                throw new MetadataException("historical metadata root must be a JSON object");
            }
            reader.beginObject();
            java.util.Set<String> topLevelSeen = new java.util.HashSet<>();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (!topLevelSeen.add(name)) {
                    throw new MetadataException(
                        "duplicate field '" + name + "' in historical metadata document");
                }
                switch (name) {
                    case "schema" -> schema = readInt(reader, name);
                    case "profileId" -> profileId = readString(reader, name);
                    case "generationPreset" -> generationPreset = readString(reader, name);
                    case "revision" -> revision = readLong(reader, name);
                    case "history" -> history = readHistory(reader);
                    case "snapshotPatchId" -> snapshotPatchId = readString(reader, name);
                    default -> throw new MetadataException(
                        "unknown field '" + name + "' in historical metadata document");
                }
            }
            reader.endObject();
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                throw new MetadataException(
                    "trailing content after the historical metadata document: " + reader.peek());
            }
        } catch (MetadataException e) {
            throw e;
        } catch (java.io.IOException | RuntimeException e) {
            throw new MetadataException("historical metadata is not valid JSON: "
                + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }

        if (schema == null) {
            throw new MetadataException("historical metadata is missing required field 'schema'");
        }
        if (profileId == null) {
            throw new MetadataException(
                "historical metadata is missing required field 'profileId'");
        }
        if (generationPreset == null) {
            throw new MetadataException(
                "historical metadata is missing required field 'generationPreset'");
        }
        if (revision == null) {
            throw new MetadataException(
                "historical metadata is missing required field 'revision'");
        }
        if (history == null) {
            throw new MetadataException("historical metadata is missing required field 'history'");
        }

        HistoricalIdentifier profile = parseIdentifier(profileId, "profileId");
        HistoricalIdentifier preset = parseIdentifier(generationPreset, "generationPreset");

        try {
            return new HistoricalMetadata(schema, profile, preset, revision, history, snapshotPatchId);
        } catch (IllegalArgumentException e) {
            throw new MetadataException("historical metadata failed validation: " + e.getMessage(),
                e);
        }
    }

    private static HistoricalIdentifier parseIdentifier(String raw, String field)
        throws MetadataException {
        try {
            return HistoricalIdentifier.parse(raw);
        } catch (IllegalArgumentException e) {
            throw new MetadataException(
                "field '" + field + "' is not a valid identifier: " + e.getMessage(), e);
        }
    }

    private static int readInt(com.google.gson.stream.JsonReader reader, String field)
        throws java.io.IOException, MetadataException {
        long value = readLong(reader, field);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new MetadataException("field '" + field + "' is out of integer range: " + value);
        }
        return (int) value;
    }

    private static long readLong(com.google.gson.stream.JsonReader reader, String field)
        throws java.io.IOException, MetadataException {
        if (reader.peek() != com.google.gson.stream.JsonToken.NUMBER) {
            throw new MetadataException(
                "field '" + field + "' must be a number, found " + reader.peek());
        }
        String raw = reader.nextString();
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new MetadataException("field '" + field + "' is not an integer: " + raw, e);
        }
    }

    private static String readString(com.google.gson.stream.JsonReader reader, String field)
        throws java.io.IOException, MetadataException {
        if (reader.peek() != com.google.gson.stream.JsonToken.STRING) {
            throw new MetadataException(
                "field '" + field + "' must be a string, found " + reader.peek());
        }
        return reader.nextString();
    }

    private static java.util.List<MetadataRevision> readHistory(
        com.google.gson.stream.JsonReader reader
    ) throws java.io.IOException, MetadataException {
        if (reader.peek() != com.google.gson.stream.JsonToken.BEGIN_ARRAY) {
            throw new MetadataException("field 'history' must be an array, found " + reader.peek());
        }
        java.util.List<MetadataRevision> entries = new java.util.ArrayList<>();
        reader.beginArray();
        int index = 0;
        while (reader.hasNext()) {
            entries.add(readHistoryEntry(reader, index));
            index++;
        }
        reader.endArray();
        return entries;
    }

    private static MetadataRevision readHistoryEntry(
        com.google.gson.stream.JsonReader reader,
        int index
    ) throws java.io.IOException, MetadataException {
        String where = "history[" + index + "]";
        if (reader.peek() != com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
            throw new MetadataException(where + " must be an object");
        }

        Long revision = null;
        Long timestamp = null;
        String actor = null;
        String note = null;

        reader.beginObject();
        java.util.Set<String> entrySeen = new java.util.HashSet<>();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (!entrySeen.add(name)) {
                throw new MetadataException("duplicate field '" + name + "' in " + where);
            }
            switch (name) {
                case "revision" -> revision = readLong(reader, where + ".revision");
                case "timestamp" -> timestamp = readLong(reader, where + ".timestamp");
                case "actor" -> actor = readString(reader, where + ".actor");
                case "note" -> note = readString(reader, where + ".note");
                default -> throw new MetadataException(
                    "unknown field '" + name + "' in " + where);
            }
        }
        reader.endObject();

        if (revision == null) {
            throw new MetadataException(where + " is missing required field 'revision'");
        }
        if (timestamp == null) {
            throw new MetadataException(where + " is missing required field 'timestamp'");
        }
        if (actor == null) {
            throw new MetadataException(where + " is missing required field 'actor'");
        }
        if (note == null) {
            throw new MetadataException(where + " is missing required field 'note'");
        }

        try {
            return new MetadataRevision(revision, timestamp, actor, note);
        } catch (IllegalArgumentException e) {
            throw new MetadataException(where + " failed validation: " + e.getMessage(), e);
        }
    }
}

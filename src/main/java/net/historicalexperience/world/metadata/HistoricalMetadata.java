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
 * Persisted historical metadata for a world.
 *
 * <p>Invariants enforced by the compact constructor:
 * <ul>
 *   <li>{@code schema} must be a schema this build understands
 *       (see {@link MetadataSchema#CURRENT}); unknown (usually newer) schemas fail closed so that
 *       a newer build's data is never silently rewritten by an older one.</li>
 *   <li>{@code profileId} and {@code generationPreset} are separate, independently versioned
 *       identifiers. A profile describes gameplay semantics; a generation preset describes
 *       terrain only.</li>
 *   <li>{@code revision} is the current revision and must be the highest revision present in
 *       {@code history}.</li>
 * </ul>
 *
 * @param schema           persisted schema version
 * @param profileId        historical gameplay profile identifier
 * @param generationPreset terrain generation preset identifier
 * @param revision         current revision number, {@code >= 1}
 * @param history          append-only revision history, oldest first
 * @param snapshotPatchId  optional explicit snapshot patch; empty retains the ordinary parent profile
 */
public record HistoricalMetadata(
    int schema,
    HistoricalIdentifier profileId,
    HistoricalIdentifier generationPreset,
    long revision,
    java.util.List<MetadataRevision> history,
    String snapshotPatchId
) {
    /** Existing worlds and callers have no snapshot patch unless explicitly selected. */
    public HistoricalMetadata(int schema, HistoricalIdentifier profileId, HistoricalIdentifier generationPreset,
                              long revision, java.util.List<MetadataRevision> history) {
        this(schema, profileId, generationPreset, revision, history, "");
    }

    public HistoricalMetadata {
        if (schema != MetadataSchema.CURRENT) {
            throw new IllegalArgumentException(
                "unsupported metadata schema " + schema + " (this build writes and reads schema "
                    + MetadataSchema.CURRENT + ")");
        }
        if (profileId == null) {
            throw new IllegalArgumentException("profileId must not be null");
        }
        if (generationPreset == null) {
            throw new IllegalArgumentException("generationPreset must not be null");
        }
        if (snapshotPatchId == null || (!snapshotPatchId.isEmpty() && snapshotPatchId.isBlank())) {
            throw new IllegalArgumentException("snapshotPatchId must be empty or a known patch identifier");
        }
        net.historicalexperience.version.HistoricalSnapshotPatches.validateAttachment(profileId.value(), snapshotPatchId);
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be >= 1, got " + revision);
        }
        if (history == null) {
            throw new IllegalArgumentException("history must not be null");
        }
        history = java.util.List.copyOf(history);
        long highest = 0;
        long previous = 0;
        for (MetadataRevision entry : history) {
            if (entry == null) {
                throw new IllegalArgumentException("history must not contain null entries");
            }
            if (entry.revision() <= previous) {
                throw new IllegalArgumentException(
                    "history must be strictly increasing by revision: " + previous + " -> "
                        + entry.revision());
            }
            previous = entry.revision();
            highest = entry.revision();
        }
        if (highest != revision) {
            throw new IllegalArgumentException(
                "revision " + revision + " does not match the highest history revision "
                    + highest);
        }
    }

    /** The revision history, oldest first. */
    @Override
    public java.util.List<MetadataRevision> history() {
        return history;
    }

    /**
     * Creates the initial metadata for a world that is being established as historical.
     *
     * @param profileId        gameplay profile identifier
     * @param generationPreset generation preset identifier
     * @param actor            attribution for the originating revision
     * @param timestamp        epoch milliseconds
     */
    public static HistoricalMetadata initial(
        HistoricalIdentifier profileId,
        HistoricalIdentifier generationPreset,
        String actor,
        long timestamp
    ) {
        MetadataRevision first = new MetadataRevision(
            1, timestamp, actor, "initial historical world establishment");
        return new HistoricalMetadata(
            MetadataSchema.CURRENT, profileId, generationPreset, 1, java.util.List.of(first));
    }

    /** Initial snapshot selection keeps the parent's terrain/profile and records only an explicit patch. */
    public static HistoricalMetadata initialWithSnapshot(HistoricalIdentifier profileId, HistoricalIdentifier generationPreset,
                                                         String patchId, String actor, long timestamp) {
        var initial = initial(profileId, generationPreset, actor, timestamp);
        return new HistoricalMetadata(initial.schema(), profileId, generationPreset, initial.revision(), initial.history(), patchId);
    }

    /** Selector/network identity; the ordinary historical rules continue to use profileId. */
    public String selectionId() {
        return snapshotPatchId.isEmpty() ? profileId.value() : snapshotPatchId;
    }

    /** Patch changes are metadata revisions, never an in-place generator or terrain replacement. */
    public HistoricalMetadata advanceSnapshotPatch(String patchId, String actor, String note, long timestamp) {
        if (snapshotPatchId.equals(patchId)) throw new IllegalArgumentException("snapshot patch is unchanged");
        long next = revision + 1;
        var nextHistory = new java.util.ArrayList<>(history);
        nextHistory.add(new MetadataRevision(next, timestamp, actor, note));
        return new HistoricalMetadata(schema, profileId, generationPreset, next, nextHistory, patchId);
    }

    /**
     * Returns a new metadata value with a bumped revision and appended history entry. The
     * generation preset can be advanced independently of the profile.
     *
     * <p>This is the WITHIN-VERSION advance: the world stays the version it is and moves to a newer
     * generation recipe of that same version. For a move to a DIFFERENT version use
     * {@link #advanceVersion}, which also re-points the profile - and therefore the gameplay rules.
     */
    public HistoricalMetadata advance(
        HistoricalIdentifier newGenerationPreset,
        String actor,
        String note,
        long timestamp
    ) {
        long next = revision + 1;
        java.util.List<MetadataRevision> nextHistory = new java.util.ArrayList<>(history);
        nextHistory.add(new MetadataRevision(next, timestamp, actor, note));
        return new HistoricalMetadata(
            schema,
            profileId,
            newGenerationPreset == null ? generationPreset : newGenerationPreset,
            next,
            nextHistory,
            snapshotPatchId);
    }

    /**
     * Re-points the document at a different historical version: both the gameplay profile and the
     * generation recipe move together.
     *
     * <h2>Why both, and why this method had to exist</h2>
     *
     * <p>Advancing the generation preset alone leaves a document that claims to be one version while
     * generating another. A real client hit exactly that: upgrading a Beta 1.7.3 world to Beta 1.8.1
     * stored profile {@code beta_1_7_3} with generation preset {@code beta_1_8_1_cave_fix}, and the next
     * start refused to load it - correctly, because the world would otherwise have kept Beta 1.7.3's
     * gameplay rules (mob spawns, drops, food, no sprint, no hunger) on top of Beta 1.8.1 terrain. The
     * refusal was the right behaviour; producing that state was the bug.
     *
     * @param newProfileId        the profile of the version being moved to, taken from the binding for
     *                            the new preset so the two cannot disagree
     * @param newGenerationPreset that version's newest generation recipe
     */
    public HistoricalMetadata advanceVersion(
        HistoricalIdentifier newProfileId,
        HistoricalIdentifier newGenerationPreset,
        String actor,
        String note,
        long timestamp
    ) {
        if (newProfileId == null) {
            throw new IllegalArgumentException("a version advance must name the target profile");
        }
        long next = revision + 1;
        java.util.List<MetadataRevision> nextHistory = new java.util.ArrayList<>(history);
        nextHistory.add(new MetadataRevision(next, timestamp, actor, note));
        return new HistoricalMetadata(
            schema,
            newProfileId,
            newGenerationPreset == null ? generationPreset : newGenerationPreset,
            next,
            nextHistory);
    }
}

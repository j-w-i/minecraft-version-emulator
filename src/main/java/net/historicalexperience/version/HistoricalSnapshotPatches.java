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
package net.historicalexperience.version;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Explicit gameplay patches over existing release-family profiles, not replacement terrain recipes.
 * Snapshot identities must remain separate from the base profile used by ordinary historical rules.
 * Sources and confidence are recorded in research/snapshot-patches.json.
 */
public final class HistoricalSnapshotPatches {
    public enum Feature { B_CREATIVE_SHORTCUT, BOOK_ON_ANY_SINGLE_ITEM }

    public record Patch(String id, String displayName, String parentRelease, String baseProfileId,
                        LocalDate releaseDate, Set<Feature> features, String sourceUrl) {
        public Patch {
            if (id == null || !id.startsWith("historical_experience:snapshot_")
                    || displayName == null || displayName.isBlank()
                    || parentRelease == null || parentRelease.isBlank()
                    || baseProfileId == null || !baseProfileId.startsWith("historical_experience:release_")
                    || releaseDate == null || sourceUrl == null || !sourceUrl.startsWith("https://")) {
                throw new IllegalArgumentException("Invalid snapshot patch identity");
            }
            features = Set.copyOf(features);
        }
        public boolean enables(Feature feature) { return features.contains(feature); }
    }

    private static final List<Patch> PATCHES = List.of(
        new Patch("historical_experience:snapshot_11w49a", "11w49a", "1.1",
            "historical_experience:release_1_1", LocalDate.of(2011, 12, 8),
            Set.of(Feature.B_CREATIVE_SHORTCUT), "https://minecraft.wiki/w/Java_Edition_11w49a"),
        new Patch("historical_experience:snapshot_12w49a", "12w49a", "1.4.6",
            "historical_experience:release_1_4", LocalDate.of(2012, 12, 7),
            Set.of(Feature.BOOK_ON_ANY_SINGLE_ITEM), "https://minecraft.wiki/w/Java_Edition_12w49a")
    );

    private HistoricalSnapshotPatches() { }

    public static List<Patch> all() { return PATCHES; }

    public static Optional<Patch> byId(String id) {
        return PATCHES.stream().filter(patch -> patch.id().equals(id)).findFirst();
    }

    /** A snapshot patch may only be attached to its declared base profile. Unknown patches are refused. */
    public static void validateAttachment(String baseProfileId, String patchId) {
        if (patchId == null || patchId.isBlank()) return;
        var patch = byId(patchId).orElseThrow(() -> new IllegalArgumentException("Unknown snapshot patch " + patchId));
        if (!patch.baseProfileId().equals(baseProfileId)) {
            throw new IllegalArgumentException("Snapshot " + patch.displayName() + " requires " + patch.baseProfileId());
        }
    }

    /** Resolve a selector key without registering another terrain version or changing release chronology. */
    public static Optional<String> baseProfileForSelection(String selectionId) {
        var patch = byId(selectionId);
        if (patch.isPresent()) return Optional.of(patch.get().baseProfileId());
        return HistoricalVersions.byId(selectionId).map(HistoricalVersion::id);
    }
}

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
package net.historicalexperience.world;

import java.util.Objects;
import java.util.Optional;

import net.historicalexperience.profile.HistoricalProfile;
import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataException;
import net.historicalexperience.world.metadata.MetadataSchema;

import java.io.IOException;

/**
 * The server-side historical state of one world directory.
 *
 * <p>Explicitly a closed set of three outcomes, because the difference between "not historical"
 * and "historical but unreadable" is the difference between continuing normally and refusing to
 * start:
 *
 * <ul>
 *   <li>{@link Normal} - no metadata document. This world was never declared historical. It must
 *       be left completely alone; in particular it must never be tagged automatically.</li>
 *   <li>{@link Historical} - a readable, validated metadata document. The world is historical and
 *       the loaded metadata is authoritative.</li>
 *   <li>{@link Unreadable} - a metadata document exists but cannot be trusted. This is
 *       fail-closed: the document is preserved untouched and the caller must refuse to proceed,
 *       because silently regenerating or rewriting it would destroy unsupported stored
 *       content.</li>
 * </ul>
 */
public sealed interface HistoricalWorldState {

    /** True when a metadata document is present but could not be read and validated. */
    default boolean isUnreadable() {
        return this instanceof Unreadable;
    }

    /** True when the world is explicitly historical and its metadata is valid. */
    default boolean isHistorical() {
        return this instanceof Historical;
    }

    /** The metadata, when the world is historical. */
    default java.util.Optional<HistoricalMetadata> metadataOrEmpty() {
        return this instanceof Historical historical
            ? java.util.Optional.of(historical.metadata())
            : java.util.Optional.empty();
    }

    /** The metadata file backing this state. */
    java.nio.file.Path source();

    /** A world that was never declared historical. */
    record Normal(java.nio.file.Path source) implements HistoricalWorldState {
        public Normal {
            Objects.requireNonNull(source, "source");
        }
    }

    /** A world explicitly declared historical, with valid metadata. */
    record Historical(
        java.nio.file.Path source,
        HistoricalMetadata metadata,
        HistoricalProfile profile
    ) implements HistoricalWorldState {
        public Historical {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(profile, "profile");
        }

        /**
         * The immutable generation binding this world's persisted recipe identifier resolves to.
         *
         * <p>Resolved from {@link #metadata()}'s generation preset - the owned raw settings alias -
         * rather than stored separately, so it can never disagree with the document on disk.
         * {@link HistoricalWorldState#load} only constructs a {@code Historical} after proving the
         * binding exists and that its profile id is the persisted profile id, and it passes the
         * binding's own resolved profile value in; for a value constructed directly by a caller this
         * is a plain lookup that may be empty, and callers must fail closed on empty rather than
         * assume a binding.
         */
        public Optional<HistoricalProfiles.GenerationBinding> generationBinding() {
            return HistoricalProfiles.byGenerationPreset(metadata.generationPreset());
        }

        /**
         * True when the persisted generation preset still exactly matches the recipe this world's
         * stored profile declares.
         *
         * <p>An exact identifier comparison, never a mapping: the two Beta 1.7.3 revisions declare
         * different owned settings aliases, and each must match its own persisted value. There is
         * no equivalence rule between revisions and no repair of a persisted value.
         */
        public boolean generationPresetMatchesProfile() {
            return profile.generationPreset().equals(metadata.generationPreset());
        }
    }

    /** A world with a metadata document that cannot be trusted. Never repaired in place. */
    record Unreadable(java.nio.file.Path source, String reason) implements HistoricalWorldState {
        public Unreadable {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * Loads the historical state of a world directory, for a caller that has no level.
     *
     * <p>Exists so the world-selection GUI can classify a world without a second copy of the rules above. The
     * classification is the same one the lifecycle uses to decide whether a world may start, and a GUI that
     * re-implemented it would be able to disagree with the server about what a world is - which is the one thing
     * an interface must never do.
     */
    public static HistoricalWorldState loadAt(java.nio.file.Path worldDirectory) {
        Objects.requireNonNull(worldDirectory, "worldDirectory");
        return load(HistoricalMetadataStore.forWorldDirectory(worldDirectory));
    }

    /**
     * Loads the historical state of a world directory.
     *
     * <p>This method never writes. Absence of a document yields {@link Normal}; a document that
     * cannot be parsed or validated yields {@link Unreadable}; a valid document whose generation
     * recipe this build does not declare, or whose profile id is not the profile that recipe is
     * bound to, also yields {@link Unreadable}, because this build cannot reason about that world's
     * historical semantics.
     *
     * <p>The persisted generation preset is the owned raw settings alias and selects the binding;
     * the persisted profile id must then equal that binding's profile id. The profile stored on the
     * returned {@link Historical} is the <b>binding's own resolved profile value</b>, never the
     * canonical registry value for the id: two revisions may share a profile id while declaring
     * different generation presets, and substituting the canonical value would re-point the world at
     * the wrong recipe. A cross pair - a preset bound to a different profile id - is rejected, and an
     * unknown recipe is never substituted with a known one.
     */
    static HistoricalWorldState load(HistoricalMetadataStore store) {
        Objects.requireNonNull(store, "store");
        java.nio.file.Path file = store.file();

        if (!store.hasEntry()) {
            return new Normal(file);
        }

        HistoricalMetadata metadata;
        try {
            metadata = store.read();
        } catch (MetadataException e) {
            return new Unreadable(file, e.getMessage());
        } catch (IOException e) {
            return new Unreadable(file,
                "could not read " + MetadataSchema.FILE_NAME + ": " + e.getMessage());
        }

        // The gameplay profile id is validated first and on its own terms. A document naming an era
        // this build does not declare must be reported as an unknown profile, not as a generation
        // recipe mismatch: the two failures need different operator responses, and collapsing them
        // would hide the more fundamental problem behind a binding diagnostic.
        if (net.historicalexperience.profile.HistoricalProfiles
                .byId(metadata.profileId()).isEmpty()) {
            return new Unreadable(file,
                "metadata names unknown historical profile '" + metadata.profileId() + "'");
        }

        Optional<HistoricalProfiles.GenerationBinding> binding =
            HistoricalProfiles.byGenerationPreset(metadata.generationPreset());
        if (binding.isEmpty()) {
            return new Unreadable(file,
                "persisted profile '" + metadata.profileId() + "' with generation preset '"
                    + metadata.generationPreset() + "' does not match any generation binding this "
                    + "build declares; refusing to substitute a different generation recipe or to "
                    + "fall back to the frozen legacy declaration");
        }

        // The exact cross pair check. Both identifiers may be individually known and still not form
        // a registered pair: the profile is never taken from one binding and the terrain from
        // another, and a revision of the same era is never silently accepted in place of the one
        // the document records.
        HistoricalProfile resolved = binding.get().profile();
        if (!resolved.id().equals(metadata.profileId())) {
            return new Unreadable(file,
                "persisted profile '" + metadata.profileId() + "' does not match the profile bound "
                    + "to generation preset '" + metadata.generationPreset() + "' ('"
                    + resolved.id() + "')");
        }
        if (!resolved.generationPreset().equals(metadata.generationPreset())) {
            return new Unreadable(file,
                "generation preset '" + metadata.generationPreset()
                    + "' resolved through a profile that declares '"
                    + resolved.generationPreset() + "'");
        }

        return new Historical(file, metadata, resolved);
    }
}

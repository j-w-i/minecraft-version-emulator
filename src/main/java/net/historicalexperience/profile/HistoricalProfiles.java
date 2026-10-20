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
package net.historicalexperience.profile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.historicalexperience.HistoricalExperience;
import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.world.metadata.HistoricalIdentifier;

/**
 * The declarative profile table for this slice.
 *
 * <p>Profiles are data, not behaviour: adding an era means adding an entry that names the era and
 * the Moderner Beta preset that provides its terrain. Behavioural differences belong in
 * {@code net.historicalexperience.gameplay}, which is out of scope for this slice.
 *
 * <h2>Two independent axes</h2>
 *
 * <p>A <em>gameplay profile</em> ({@link HistoricalProfile}) names an era of the game. A
 * <em>generation binding</em> ({@link GenerationBinding}) pairs one <b>resolved</b> profile value
 * with one generation-recipe revision and the actual upstream terrain foundation it inherits from.
 *
 * <p>They are deliberately separate tables. One gameplay profile can be backed by more than one
 * generation revision over time, and each revision must stay independently addressable, because
 * {@link net.historicalexperience.world.metadata.HistoricalMetadata} persists the owned recipe
 * identifier a world was created with. Overwriting a single alias instead would silently
 * reinterpret existing worlds; a new revision is therefore a <b>new</b> identifier and a <b>new</b>
 * resolved profile value, never a mutation of an old one.
 *
 * <p>Two identifiers must never be conflated: the <em>owned raw settings alias</em>
 * ({@code historical_experience:beta_1_7_3} and its preview successor) is what worlds reference,
 * report as their marker and persist; the <em>actual upstream Moderner Beta preset</em>
 * ({@code moderner_beta:beta}) is the shared terrain foundation both revisions inherit from.
 *
 * <p>Bindings are only ever added, never edited. {@link #beta173()} and {@link #byId} keep
 * resolving the frozen v1 declaration, so the canonical v1 value is never promoted to, or replaced
 * by, a newer revision.
 */
public final class HistoricalProfiles {
    /** Revision recorded for the frozen v1 declaration: not a numbered content revision. */
    public static final int LEGACY_REVISION = 0;

    /** First numbered content revision: the bounded Beta 1.7.3 content preview. */
    public static final int CONTENT_PREVIEW_REVISION_1 = 1;

    /**
     * Second numbered content revision: the same content deltas on the reduced level envelope.
     *
     * <p>A new revision rather than an edit to revision 1, because revision 1 is already persisted by
     * worlds that were generated with the vanilla {@code -64..319} envelope. Reinterpreting that
     * identifier would silently change what an existing world's recorded recipe means.
     */
    public static final int CONTENT_PREVIEW_REVISION_2 = 2;

    /**
     * Third numbered content revision: reduced level envelope plus Moderner Beta's published
     * reduced-height noise envelope.
     *
     * <p>Revision 2 measured <b>zero bedrock</b>: reducing only the dimension type moved the level
     * floor to y=0 while Moderner Beta still placed its bedrock column from a noise envelope whose
     * {@code min_y} was -64, i.e. entirely below the floor. Revision 3 adopts the noise half too, so
     * the recorded recipe and the generated world agree. Again a new identifier rather than an edit:
     * revision 2 is already persisted by a generated world.
     */
    public static final int CONTENT_PREVIEW_REVISION_3 = 3;

    /**
     * Fourth numbered content revision: the first revision that also selects generation-time
     * <b>content restrictions</b>.
     *
     * <p>Revisions 1 to 3 selected only settings components. This one additionally selects an
     * append-only restriction policy that suppresses placed features Beta 1.7.3 did not have. Those
     * features are declared by Moderner Beta's Java biome builders, so the restriction cannot be a
     * settings component; it is selected by the revision alias itself, which is why the earlier
     * aliases must keep resolving to an <em>empty</em> restriction set.
     */
    public static final int CONTENT_PREVIEW_REVISION_4 = 4;

    /**
     * Fifth numbered content revision: Beta 1.7.3's own ore placement.
     *
     * <p>Ore placement changes what a stored chunk contains, so it is a revision rather than a
     * gameplay rule: revisions 1 to 4 keep generating the vanilla ore placement their recipes recorded,
     * and only this alias selects the Beta population.
     */
    public static final int CONTENT_PREVIEW_REVISION_5 = 5;

    private static final Map<HistoricalIdentifier, HistoricalProfile> REGISTRY;
    private static final Map<HistoricalIdentifier, GenerationBinding> GENERATION_BINDINGS;

    static {
        // Both tables are derived from the version registry. That is the point of the registry: a new
        // version declares its revisions once, and the profile table and the binding table follow,
        // rather than each subsystem keeping its own list that can drift.
        Map<HistoricalIdentifier, HistoricalProfile> profiles = new LinkedHashMap<>();
        Map<HistoricalIdentifier, GenerationBinding> bindings = new LinkedHashMap<>();

        for (net.historicalexperience.version.HistoricalVersion version
            : net.historicalexperience.version.HistoricalVersions.all()) {
            HistoricalIdentifier profileId = HistoricalIdentifier.parse(version.id());

            for (net.historicalexperience.version.HistoricalVersion.GenerationRevision revision
                : version.revisions()) {
                HistoricalIdentifier recipe = HistoricalIdentifier.parse(revision.alias());
                // One resolved profile value per revision. A revision is a different generation recipe
                // for the same era, so the profile id is shared while the declared preset differs -
                // and the value is deliberately not promoted into the canonical registry.
                HistoricalProfile profile =
                    new HistoricalProfile(profileId, version.displayName(), recipe);
                registerBinding(bindings, new GenerationBinding(
                    profile,
                    HistoricalIdentifier.parse(revision.foundation()),
                    revision.revision(),
                    revision.experimental()));

                if (revision.legacy()) {
                    HistoricalProfile previous = profiles.put(profileId, profile);
                    if (previous != null) {
                        throw new IllegalStateException(
                            "duplicate canonical profile id " + profileId);
                    }
                }
            }
        }
        REGISTRY = Map.copyOf(profiles);
        GENERATION_BINDINGS = Map.copyOf(bindings);
    }

    private HistoricalProfiles() {
    }

    private static void register(
        Map<HistoricalIdentifier, HistoricalProfile> profiles,
        HistoricalProfile profile
    ) {
        HistoricalProfile previous = profiles.put(profile.id(), profile);
        if (previous != null) {
            throw new IllegalStateException("duplicate historical profile id " + profile.id());
        }
    }

    private static void registerBinding(
        Map<HistoricalIdentifier, GenerationBinding> bindings,
        GenerationBinding binding
    ) {
        // Keyed by the OWNED raw settings alias the world resource references and metadata persists -
        // the profile's own declared generation preset. The recorded terrain foundation is the real
        // upstream Moderner Beta preset those settings inherit from; it is deliberately not the key,
        // because several revisions of one version share one foundation.
        HistoricalIdentifier key = binding.profile().generationPreset();
        GenerationBinding previous = bindings.put(key, binding);
        if (previous != null) {
            throw new IllegalStateException(
                "duplicate generation binding for raw settings alias " + key);
        }
        // Every version records its OWN upstream foundation - Beta 1.7.3 inherits moderner_beta:beta
        // and Beta 1.8.1 inherits moderner_beta:beta_1_8_1 - so the invariant here is the namespace,
        // not one pinned value. Pinning a single value was correct while one version existed and would
        // have been a silent trap for the second.
        if (!binding.terrainPreset().value().startsWith("moderner_beta:")) {
            throw new IllegalStateException(
                "generation binding " + key + " records terrain foundation "
                    + binding.terrainPreset() + ", which is not a Moderner Beta preset; terrain must "
                    + "always inherit from the pinned upstream foundation");
        }
    }

    /** Looks up a profile by identifier. */
    public static Optional<HistoricalProfile> byId(HistoricalIdentifier id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    /** Looks up a profile by its raw {@code namespace:path} string. */
    public static Optional<HistoricalProfile> byId(String rawId) {
        HistoricalIdentifier id;
        try {
            id = HistoricalIdentifier.parse(rawId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return byId(id);
    }

    /** The profile implemented by this slice. */
    public static HistoricalProfile beta173() {
        return byId(HistoricalIdentifier.parse(HistoricalExperience.Profiles.BETA_1_7_3))
            .orElseThrow();
    }

    /**
     * The resolved gameplay profile of the bounded Beta 1.7.3 content-preview revision.
     *
     * <p>A second immutable resolved value with the <b>same</b> gameplay profile id as
     * {@link #beta173()} - the content preview is a generation recipe revision for Beta 1.7.3, not a
     * new gameplay era - but its own declared generation preset, the owned settings alias
     * {@code historical_experience:beta_1_7_3_content_preview_1}. The canonical v1 value returned by
     * {@link #beta173()} and {@link #byId(HistoricalIdentifier)} is never replaced by it.
     */
    public static HistoricalProfile beta173ContentPreview() {
        return byGenerationPreset(HistoricalMarker.CONTENT_PREVIEW_PRESET_ID)
            .map(GenerationBinding::profile)
            .orElseThrow();
    }

    /**
     * The resolved gameplay profile of content-preview revision 2 (reduced level envelope).
     *
     * <p>Same gameplay profile id as {@link #beta173()}, its own declared generation preset, and its
     * own revision number. Revision 1 remains resolvable and unchanged; nothing here reinterprets or
     * promotes it.
     */
    public static HistoricalProfile beta173ContentPreview2() {
        return byGenerationPreset(HistoricalMarker.CONTENT_PREVIEW_2_PRESET_ID)
            .map(GenerationBinding::profile)
            .orElseThrow();
    }

    /**
     * The resolved gameplay profile of content-preview revision 3 (complete envelope adaptation).
     *
     * <p>This is the newest revision, not an accepted one: the generation-content work is still
     * incomplete, so nothing here may be described as historical fidelity.
     */
    public static HistoricalProfile beta173ContentPreview3() {
        return byGenerationPreset(HistoricalMarker.CONTENT_PREVIEW_3_PRESET_ID)
            .map(GenerationBinding::profile)
            .orElseThrow();
    }

    /**
     * The resolved gameplay profile of content-preview revision 4 (first content restrictions).
     *
     * <p>Newest revision, still not an accepted one: ore distributions, decoration, structures,
     * spawns and the Nether/End remain unfinished.
     */
    public static HistoricalProfile beta173ContentPreview4() {
        return byGenerationPreset(HistoricalMarker.CONTENT_PREVIEW_4_PRESET_ID)
            .map(GenerationBinding::profile)
            .orElseThrow();
    }

    /**
     * The resolved gameplay profile of content-preview revision 5 (Beta ore placement).
     *
     * <p>Newest revision, still not an accepted one: decoration, building-height rules and the
     * Nether/End remain unfinished.
     */
    public static HistoricalProfile beta173ContentPreview5() {
        return byGenerationPreset(HistoricalMarker.CONTENT_PREVIEW_5_PRESET_ID)
            .map(GenerationBinding::profile)
            .orElseThrow();
    }

    /**
     * The immutable generation binding registered for a project owned raw settings alias.
     *
     * <p>Keyed by the alias a world resource references and metadata persists. This is the only
     * lookup that maps a persisted generation-recipe identifier back to the resolved profile and
     * revision it represents. Unknown and malformed identifiers resolve to empty; callers must fail
     * closed rather than default to any binding, in particular never to the frozen v1 binding.
     */
    public static Optional<GenerationBinding> byGenerationPreset(HistoricalIdentifier presetId) {
        return presetId == null
            ? Optional.empty()
            : Optional.ofNullable(GENERATION_BINDINGS.get(presetId));
    }

    /** Convenience overload of {@link #byGenerationPreset(HistoricalIdentifier)}. */
    public static Optional<GenerationBinding> byGenerationPreset(String rawPresetId) {
        HistoricalIdentifier id;
        try {
            id = HistoricalIdentifier.parse(rawPresetId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return byGenerationPreset(id);
    }

    /**
     * Resolves an exact {@code (gameplay profile id, generation preset)} pair.
     *
     * <p>The preset half selects a binding; the profile half must then equal that binding's own
     * profile id, and the <em>binding's resolved profile value</em> - not the canonical registry
     * value - is returned. That distinction is load-bearing: the two Beta 1.7.3 revisions share a
     * profile id but not a declared generation preset, so returning the canonical legacy value for a
     * preview pair would silently re-point the world at the wrong recipe.
     *
     * <p>Returns empty when the preset is unknown, or when the preset is known but bound to a
     * different profile id. In particular a cross pair assembled from two different bindings
     * resolves to empty.
     *
     * @throws NullPointerException when either half is {@code null}: a missing half is a caller
     *         defect, not an unknown world, and must never be reported as "no binding"
     */
    public static Optional<HistoricalProfile> resolve(
        HistoricalIdentifier profileId,
        HistoricalIdentifier presetId
    ) {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(presetId, "presetId");
        return resolve(byGenerationPreset(presetId), profileId);
    }

    /**
     * Convenience overload of {@link #resolve(HistoricalIdentifier, HistoricalIdentifier)}.
     *
     * @throws NullPointerException when either half is {@code null}
     */
    public static Optional<HistoricalProfile> resolve(String profileId, String presetId) {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(presetId, "presetId");
        HistoricalIdentifier profile;
        HistoricalIdentifier preset;
        try {
            profile = HistoricalIdentifier.parse(profileId);
            preset = HistoricalIdentifier.parse(presetId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return resolve(profile, preset);
    }

    private static Optional<HistoricalProfile> resolve(
        Optional<GenerationBinding> binding,
        HistoricalIdentifier profileId
    ) {
        return binding
            .filter(candidate -> candidate.profile().id().equals(profileId))
            .map(GenerationBinding::profile);
    }

    /** Every registered generation binding, keyed by raw settings alias, in declaration order. */
    public static Map<HistoricalIdentifier, GenerationBinding> generationBindings() {
        return GENERATION_BINDINGS;
    }

    /** All known profiles keyed by identifier, in declaration order. */
    public static Map<HistoricalIdentifier, HistoricalProfile> all() {
        return REGISTRY;
    }

    /**
     * An immutable binding of one resolved gameplay profile to one generation-recipe revision.
     *
     * <p>Two distinct identifiers live here and must never be conflated:
     * <ul>
     *   <li>{@link #profile()}'s {@code generationPreset()} is the <b>owned raw settings alias</b>
     *       ({@code historical_experience:beta_1_7_3} or {@code ..._content_preview_1}). It is what
     *       the world resource references, what a loaded world reports as its durable marker, what
     *       metadata persists, and what this binding is keyed by.</li>
     *   <li>{@link #terrainPreset()} is the <b>actual upstream Moderner Beta preset</b> the settings
     *       inherit their terrain from - the foundation. Both revisions share it, so it identifies
     *       no revision on its own and must never be used as a marker or a persisted key.</li>
     * </ul>
     *
     * @param profile         the resolved gameplay profile of this revision; its declared generation
     *                        preset is the raw owned alias that keys this binding
     * @param terrainPreset   the actual upstream Moderner Beta preset this revision is founded on
     * @param generationRevision {@link #LEGACY_REVISION} for the frozen v1 declaration, otherwise a
     *                        positive content revision number
     * @param experimental    {@code true} only for a bounded, explicitly non-final revision; such a
     *                        revision must never be described as historically accurate or complete
     */
    public record GenerationBinding(
        HistoricalProfile profile,
        HistoricalIdentifier terrainPreset,
        int generationRevision,
        boolean experimental
    ) {
        public GenerationBinding {
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(terrainPreset, "terrainPreset");
            if (generationRevision < LEGACY_REVISION) {
                throw new IllegalArgumentException(
                    "generationRevision must be >= " + LEGACY_REVISION + ", got "
                        + generationRevision);
            }
            boolean legacy = generationRevision == LEGACY_REVISION;
            // Same rule as GenerationRevision: a numbered revision is experimental unless it is on the reviewed
            // accepted list. Both invariants had to learn the list, or an accepted revision could not be built.
            boolean accepted = !legacy
                && net.historicalexperience.version.HistoricalVersion.ACCEPTED_REVISIONS
                    .contains(String.valueOf(profile.generationPreset()));
            boolean wronglyFlagged = legacy ? experimental : (!experimental && !accepted);
            if (wronglyFlagged) {
                throw new IllegalArgumentException(
                    "generation binding " + profile.generationPreset() + " declares revision "
                        + generationRevision + " with experimental=" + experimental
                        + "; the unnumbered frozen declaration is not experimental and a numbered "
                        + "content revision must be marked experimental until it is accepted");
            }
        }

        /** True for the frozen, unnumbered v1 declaration. */
        public boolean legacy() {
            return generationRevision == LEGACY_REVISION;
        }

        /** The owned raw settings alias this revision is persisted and referenced by. */
        public HistoricalIdentifier rawSettingsAlias() {
            return profile.generationPreset();
        }

        /** The actual upstream Moderner Beta preset this revision is founded on. */
        public HistoricalIdentifier foundationPreset() {
            return terrainPreset;
        }
    }
}

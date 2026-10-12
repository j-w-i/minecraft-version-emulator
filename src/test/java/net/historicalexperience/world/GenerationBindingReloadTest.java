package net.historicalexperience.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for resolving a persisted generation binding across a reload.
 *
 * <p>Level: unit with a real temporary filesystem. No Minecraft runtime, no Moderner Beta classes.
 *
 * <p>What these tests pin, and why it matters: the metadata document's generation preset is the
 * owned raw settings alias and the only record of which generation recipe produced a world's chunks.
 * A reload must therefore reproduce <b>exactly</b> that binding - the same revision, the same
 * experimental status, the same <em>resolved</em> profile value - or fail closed. It must never fall
 * back to the frozen v1 declaration, never substitute a different revision of the same era, never
 * swap the owned alias for the shared upstream terrain foundation, and never rewrite the document.
 */
class GenerationBindingReloadTest {
    private static final String PROFILE = "historical_experience:beta_1_7_3";
    private static final String LEGACY_PRESET = "historical_experience:beta_1_7_3";
    private static final String PREVIEW_PRESET =
        "historical_experience:beta_1_7_3_content_preview_1";
    private static final String UPSTREAM_PRESET = "moderner_beta:beta";

    private static HistoricalMetadata document(String profileId, String generationPreset) {
        return HistoricalMetadata.initial(
            HistoricalIdentifier.parse(profileId),
            HistoricalIdentifier.parse(generationPreset),
            "historical_experience",
            1_700_000_000_000L);
    }

    private static HistoricalWorldState loadAfterWrite(
        HistoricalMetadataStore store,
        HistoricalMetadata metadata
    ) throws Exception {
        store.write(metadata);
        return HistoricalWorldState.load(new HistoricalMetadataStore(store.file()));
    }

    private static HistoricalWorldState.Historical historical(HistoricalWorldState state) {
        assertTrue(state instanceof HistoricalWorldState.Historical,
            "expected a historical world, got " + state);
        return (HistoricalWorldState.Historical) state;
    }

    @Test
    @DisplayName("a content-preview world reloads onto its own revision-1 binding, not the legacy one")
    void previewWorldReloadsOntoPreviewBinding(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        HistoricalWorldState state = loadAfterWrite(store, document(PROFILE, PREVIEW_PRESET));
        byte[] before = Files.readAllBytes(store.file());

        HistoricalWorldState.Historical historical = historical(state);
        HistoricalProfiles.GenerationBinding binding = historical.generationBinding().orElseThrow();

        // The binding is keyed by the owned raw settings alias that was persisted, and it records
        // the actual upstream terrain foundation separately.
        assertEquals(HistoricalIdentifier.parse(PREVIEW_PRESET), binding.rawSettingsAlias());
        assertEquals(HistoricalIdentifier.parse(UPSTREAM_PRESET), binding.terrainPreset());
        assertEquals(HistoricalProfiles.CONTENT_PREVIEW_REVISION_1, binding.generationRevision());
        assertTrue(binding.experimental());
        assertFalse(binding.legacy());

        // The stored profile is the preview revision's own resolved value: same gameplay profile
        // ID, a different value from the canonical legacy one.
        assertEquals(HistoricalIdentifier.parse(PROFILE), historical.profile().id());
        assertEquals(HistoricalProfiles.beta173ContentPreview(), historical.profile());
        assertSame(HistoricalProfiles.beta173ContentPreview(), historical.profile());
        assertEquals(HistoricalProfiles.beta173ContentPreview(), binding.profile());
        assertNotSame(HistoricalProfiles.beta173(), historical.profile(),
            "a preview world must not be stored as the canonical legacy profile value");

        assertEquals(HistoricalIdentifier.parse(PREVIEW_PRESET),
            historical.metadata().generationPreset());
        assertTrue(historical.generationPresetMatchesProfile(),
            "the exact check must hold for the preview revision too");

        assertArrayEquals(before, Files.readAllBytes(store.file()),
            "loading must not rewrite the persisted document");
    }

    @Test
    @DisplayName("a reloading content-preview world is never downgraded to the legacy binding")
    void previewWorldIsNeverDowngraded(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        HistoricalWorldState.Historical historical =
            historical(loadAfterWrite(store, document(PROFILE, PREVIEW_PRESET)));

        // The two revisions share one gameplay profile ID, so a defect that resolved the profile by
        // id and then defaulted the terrain would still pass a profile-id-only assertion. Both the
        // alias and the profile value must be the preview revision's.
        assertEquals(HistoricalIdentifier.parse(PREVIEW_PRESET),
            historical.generationBinding().orElseThrow().rawSettingsAlias());
        assertEquals(HistoricalIdentifier.parse(PREVIEW_PRESET),
            historical.profile().generationPreset());
        assertFalse(historical.profile().generationPreset()
            .equals(HistoricalIdentifier.parse(LEGACY_PRESET)),
            "the persisted revision must be preserved, not replaced by the frozen declaration");
    }

    @Test
    @DisplayName("a frozen v1 world reloads onto the unchanged legacy binding")
    void legacyWorldReloadsOntoLegacyBinding(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        HistoricalWorldState.Historical historical =
            historical(loadAfterWrite(store, document(PROFILE, LEGACY_PRESET)));
        HistoricalProfiles.GenerationBinding binding = historical.generationBinding().orElseThrow();

        assertEquals(HistoricalIdentifier.parse(LEGACY_PRESET), binding.rawSettingsAlias());
        assertEquals(HistoricalIdentifier.parse(UPSTREAM_PRESET), binding.terrainPreset());
        assertEquals(HistoricalProfiles.LEGACY_REVISION, binding.generationRevision());
        assertFalse(binding.experimental());
        assertTrue(binding.legacy());
        assertSame(HistoricalProfiles.beta173(), historical.profile());
        assertEquals(HistoricalProfiles.beta173(), historical.profile());
        assertTrue(historical.generationPresetMatchesProfile());
    }

    @Test
    @DisplayName("both revisions of one profile survive reload with their own bytes intact")
    void bothRevisionsCoexistAcrossReloads(@TempDir Path worldDirectory) throws Exception {
        Path legacyDirectory = worldDirectory.resolve("legacy");
        Path previewDirectory = worldDirectory.resolve("preview");

        HistoricalMetadataStore legacyStore =
            HistoricalMetadataStore.forWorldDirectory(legacyDirectory);
        HistoricalMetadataStore previewStore =
            HistoricalMetadataStore.forWorldDirectory(previewDirectory);

        loadAfterWrite(legacyStore, document(PROFILE, LEGACY_PRESET));
        loadAfterWrite(previewStore, document(PROFILE, PREVIEW_PRESET));
        byte[] legacyBytes = Files.readAllBytes(legacyStore.file());
        byte[] previewBytes = Files.readAllBytes(previewStore.file());

        HistoricalProfiles.GenerationBinding reloadedLegacy =
            historical(HistoricalWorldState.load(new HistoricalMetadataStore(legacyStore.file())))
                .generationBinding().orElseThrow();
        HistoricalProfiles.GenerationBinding reloadedPreview =
            historical(HistoricalWorldState.load(new HistoricalMetadataStore(previewStore.file())))
                .generationBinding().orElseThrow();

        assertEquals(HistoricalProfiles.LEGACY_REVISION, reloadedLegacy.generationRevision());
        assertEquals(HistoricalProfiles.CONTENT_PREVIEW_REVISION_1,
            reloadedPreview.generationRevision());
        assertEquals(reloadedLegacy.profile().id(), reloadedPreview.profile().id());
        assertArrayEquals(legacyBytes, Files.readAllBytes(legacyStore.file()));
        assertArrayEquals(previewBytes, Files.readAllBytes(previewStore.file()));
    }

    @Test
    @DisplayName("an unknown persisted generation binding fails closed and the bytes survive")
    void unknownBindingFailsClosed(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(document(PROFILE, "historical_experience:beta_1_7_3_content_preview_99"));
        byte[] before = Files.readAllBytes(store.file());

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertTrue(((HistoricalWorldState.Unreadable) state).reason()
                .contains("historical_experience:beta_1_7_3_content_preview_99"),
            "the reason must name the unrecognised recipe: " + state);
        assertTrue(state.metadataOrEmpty().isEmpty());
        assertArrayEquals(before, Files.readAllBytes(store.file()),
            "an unrecognised binding must never be repaired, rewritten or deleted");
    }

    @Test
    @DisplayName("the shared upstream foundation is not a binding and fails closed")
    void upstreamFoundationIsNotABinding(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(document(PROFILE, "moderner_beta:beta"));
        byte[] before = Files.readAllBytes(store.file());

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertArrayEquals(before, Files.readAllBytes(store.file()));
    }

    @Test
    @DisplayName("an exact cross pair of profile and preset fails closed and is never repaired")
    void crossPairMismatchFailsClosed(@TempDir Path worldDirectory) throws Exception {
        // The strongest cross-pair available in a build with one gameplay profile: the persisted
        // recipe is a real registered revision, but the persisted profile is not the profile that
        // revision is bound to. The profile is never taken from one binding and the terrain from
        // another, the document is never rewritten, and nothing is reinterpreted.
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(document("historical_experience:skylands", LEGACY_PRESET));
        byte[] before = Files.readAllBytes(store.file());

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertArrayEquals(before, Files.readAllBytes(store.file()));
    }

    @Test
    @DisplayName("a profile paired with a preset bound to another profile fails closed")
    void profilePresetCrossPairFailsClosed(@TempDir Path worldDirectory) throws Exception {
        // Both identifiers are individually well formed and each belongs to a project declaration -
        // 'historical_experience:skylands' is the profile identifier the existing v1 cross-pair
        // fixture uses - but the pair is not a registered binding.
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(document("historical_experience:skylands", PREVIEW_PRESET));
        byte[] before = Files.readAllBytes(store.file());

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertArrayEquals(before, Files.readAllBytes(store.file()));
    }

    @Test
    @DisplayName("a preview preset recorded under an unknown profile fails closed")
    void previewPresetWithForeignProfileFailsClosed(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(document("historical_experience:beta_1_8_1", PREVIEW_PRESET));
        byte[] before = Files.readAllBytes(store.file());

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertArrayEquals(before, Files.readAllBytes(store.file()));
    }

    @Test
    @DisplayName("a world with no document is still Normal: the new binding adopts nothing")
    void ordinaryWorldStaysNormal(@TempDir Path worldDirectory) {
        HistoricalWorldState state = HistoricalWorldState.load(
            HistoricalMetadataStore.forWorldDirectory(worldDirectory));

        assertTrue(state instanceof HistoricalWorldState.Normal, state.toString());
        assertFalse(state.isHistorical());
        assertFalse(Files.exists(worldDirectory.resolve(
            net.historicalexperience.world.metadata.MetadataSchema.FILE_NAME)));
    }

    @Test
    @DisplayName("a solved binding always reports the persisted raw alias it was keyed by")
    void bindingIsKeyedByThePersistedPreset(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalWorldState.Historical historical =
            historical(loadAfterWrite(store, document(PROFILE, PREVIEW_PRESET)));

        HistoricalProfiles.GenerationBinding binding = historical.generationBinding().orElseThrow();

        assertEquals(historical.metadata().generationPreset(), binding.rawSettingsAlias());
        assertEquals(historical.profile().generationPreset(), binding.rawSettingsAlias());
        assertEquals(historical.metadata().profileId(), binding.profile().id());
        assertEquals(HistoricalIdentifier.parse(UPSTREAM_PRESET), binding.terrainPreset());
        assertEquals(HistoricalProfiles.byGenerationPreset(historical.metadata().generationPreset())
            .orElseThrow(), binding);
        assertEquals(1L, historical.metadata().revision(),
            "the metadata schema and revision semantics are unchanged by this slice");
    }

    @Test
    @DisplayName("a reloaded preview world never records the shared upstream foundation as its recipe")
    void previewWorldDoesNotPersistTheFoundationAsItsRecipe(@TempDir Path worldDirectory)
        throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalWorldState.Historical historical =
            historical(loadAfterWrite(store, document(PROFILE, PREVIEW_PRESET)));

        // Persisting the actual Moderner Beta preset instead of the owned alias would make a plain
        // upstream Beta world indistinguishable from a project world, and the marker check could no
        // longer be satisfied by this world.
        assertFalse(historical.metadata().generationPreset()
                .equals(HistoricalIdentifier.parse(UPSTREAM_PRESET)),
            "metadata must persist the owned raw settings alias, not the terrain foundation");
        assertEquals(HistoricalIdentifier.parse(PREVIEW_PRESET),
            historical.metadata().generationPreset());
    }
}

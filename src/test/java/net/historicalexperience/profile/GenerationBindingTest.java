package net.historicalexperience.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the immutable generation-binding table.
 *
 * <p>Level: unit. No Minecraft runtime, no Moderner Beta classes, no filesystem.
 *
 * <p>These tests pin the three identifiers that must never be conflated:
 * <ul>
 *   <li>the <b>gameplay profile id</b>, shared by both Beta 1.7.3 revisions;</li>
 *   <li>the <b>owned raw settings alias</b>, which is what worlds reference and metadata persists,
 *       and which is therefore the key of the binding table;</li>
 *   <li>the <b>actual upstream Moderner Beta preset</b> the settings inherit terrain from, which is
 *       the binding's recorded foundation and identifies no revision on its own.</li>
 * </ul>
 *
 * <p>They also pin what must <em>not</em> happen: a revision must not be registered as its own
 * gameplay era, the canonical v1 profile value must not be replaced by the preview value, and
 * unknown or cross-paired lookups must resolve to empty rather than to a default.
 */
class GenerationBindingTest {
    private static final String LEGACY_ALIAS = "historical_experience:beta_1_7_3";
    private static final String PREVIEW_ALIAS =
        "historical_experience:beta_1_7_3_content_preview_1";
    private static final String UPSTREAM_PRESET = "moderner_beta:beta";
    private static final String PROFILE = "historical_experience:beta_1_7_3";

    private static HistoricalProfiles.GenerationBinding legacy() {
        return HistoricalProfiles.byGenerationPreset(LEGACY_ALIAS).orElseThrow();
    }

    private static HistoricalProfiles.GenerationBinding preview() {
        return HistoricalProfiles.byGenerationPreset(PREVIEW_ALIAS).orElseThrow();
    }

    @Test
    @DisplayName("the frozen v1 declaration is unchanged: legacy alias, revision 0, same foundation")
    void legacyBindingIsUnchanged() {
        HistoricalProfiles.GenerationBinding binding = legacy();

        assertEquals(HistoricalIdentifier.parse(LEGACY_ALIAS), binding.rawSettingsAlias());
        assertEquals(HistoricalIdentifier.parse(LEGACY_ALIAS),
            binding.profile().generationPreset());
        assertEquals(HistoricalProfiles.LEGACY_REVISION, binding.generationRevision());
        assertEquals(0, binding.generationRevision());
        assertTrue(binding.legacy());
        assertFalse(binding.experimental(), "the frozen v1 declaration is not experimental");
        assertEquals(HistoricalProfiles.beta173(), binding.profile());
        assertEquals(UPSTREAM_PRESET, binding.terrainPreset().value());
    }

    @Test
    @DisplayName("the content preview is a new resolved profile at revision 1 on the same foundation")
    void previewBindingSharesTheGameplayProfile() {
        HistoricalProfiles.GenerationBinding binding = preview();

        assertEquals(HistoricalIdentifier.parse(PREVIEW_ALIAS), binding.rawSettingsAlias());
        assertEquals(HistoricalIdentifier.parse(PREVIEW_ALIAS),
            binding.profile().generationPreset());
        assertEquals(HistoricalProfiles.CONTENT_PREVIEW_REVISION_1, binding.generationRevision());
        assertEquals(1, binding.generationRevision());
        assertTrue(binding.experimental(),
            "the bounded content preview is not an accepted historical profile");
        assertFalse(binding.legacy());
        assertEquals(UPSTREAM_PRESET, binding.terrainPreset().value());
    }

    @Test
    @DisplayName("the recorded terrain preset is the actual upstream foundation for every revision")
    void terrainPresetIsAlwaysTheActualUpstreamFoundation() {
        assertEquals(HistoricalIdentifier.parse(UPSTREAM_PRESET), legacy().terrainPreset());
        assertEquals(HistoricalIdentifier.parse(UPSTREAM_PRESET), preview().terrainPreset());
        assertEquals(legacy().foundationPreset(), legacy().terrainPreset());
        assertEquals(preview().foundationPreset(), preview().terrainPreset());
        assertEquals(HistoricalMarker.UPSTREAM_PRESET_ID, legacy().terrainPreset().value());
    }

    @Test
    @DisplayName("the owned alias is never the recorded terrain foundation")
    void aliasAndFoundationAreDifferentIdentifiers() {
        assertNotEquals(legacy().terrainPreset(), legacy().rawSettingsAlias());
        assertNotEquals(preview().terrainPreset(), preview().rawSettingsAlias());
        assertNotEquals(HistoricalMarker.UPSTREAM_PRESET_ID, HistoricalMarker.HISTORICAL_PRESET_ID);
        assertFalse(HistoricalMarker.isHistoricalMarker(UPSTREAM_PRESET));
    }

    @Test
    @DisplayName("both revisions share one profile id but are two distinct resolved profile values")
    void bothRevisionsShareOneProfileIdButNotOneValue() {
        assertEquals(legacy().profile().id(), preview().profile().id());
        assertEquals(HistoricalIdentifier.parse(PROFILE), preview().profile().id());
        assertNotSame(legacy().profile(), preview().profile(),
            "the preview must be its own resolved profile value, not the canonical legacy object");
        assertNotEquals(legacy().profile(), preview().profile());
        assertNotEquals(legacy().profile().generationPreset(),
            preview().profile().generationPreset());
    }

    @Test
    @DisplayName("beta173ContentPreview returns the resolved preview profile, not the canonical one")
    void previewAccessorReturnsItsOwnResolvedProfile() {
        HistoricalProfile previewProfile = HistoricalProfiles.beta173ContentPreview();

        assertSame(preview().profile(), previewProfile);
        assertEquals(HistoricalIdentifier.parse(PREVIEW_ALIAS),
            previewProfile.generationPreset());
        assertEquals(HistoricalIdentifier.parse(LEGACY_ALIAS),
            HistoricalProfiles.beta173().generationPreset());
        assertNotSame(HistoricalProfiles.beta173(), previewProfile,
            "the canonical legacy value must not be replaced by the preview value");
        assertNotEquals(HistoricalProfiles.beta173(), previewProfile);
    }

    @Test
    @DisplayName("the profile table keeps exactly one canonical profile: a revision is not an era")
    void noSyntheticProfilePerRevision() {
        // One canonical profile per declared version. A generation revision must never add one, which
        // is what this asserts; the absolute count comes from the registry so a new version is a data
        // change rather than a test edit.
        assertEquals(net.historicalexperience.version.HistoricalVersions.all().size(),
            HistoricalProfiles.all().size(),
            "a terrain variant must not be registered as a gameplay profile");
        assertEquals(net.historicalexperience.version.HistoricalVersions.allRevisionAliases().size(),
            HistoricalProfiles.generationBindings().size(),
            "one binding per declared generation revision");

        for (Map.Entry<HistoricalIdentifier, HistoricalProfiles.GenerationBinding> entry
            : HistoricalProfiles.generationBindings().entrySet()) {
            assertTrue(HistoricalProfiles.byId(entry.getValue().profile().id()).isPresent(),
                "binding " + entry.getKey() + " names a profile id outside the table");
            assertEquals(entry.getKey(), entry.getValue().profile().generationPreset(),
                "bindings are keyed by the owned raw settings alias");
            assertEquals(entry.getKey(), entry.getValue().rawSettingsAlias());
            // Each version records its OWN upstream foundation - Beta 1.7.3 inherits moderner_beta:beta
            // and Beta 1.8.1 inherits moderner_beta:beta_1_8_1 - so the invariant is that the recorded
            // foundation is a Moderner Beta preset that the owning version declares, not one fixed value.
            assertTrue(entry.getValue().terrainPreset().value().startsWith("moderner_beta:"),
                "revision " + entry.getKey() + " must inherit from a Moderner Beta preset, was "
                    + entry.getValue().terrainPreset());
            var owner = net.historicalexperience.version.HistoricalVersions
                .byRevisionAlias(entry.getKey().value()).orElseThrow();
            assertTrue(owner.revisions().stream().anyMatch(revision ->
                    revision.foundation().equals(entry.getValue().terrainPreset().value())),
                "revision " + entry.getKey() + " records foundation "
                    + entry.getValue().terrainPreset() + ", which its owning version "
                    + owner.id() + " does not declare");
        }
    }

    @Test
    @DisplayName("unknown, malformed and upstream generation presets resolve to no binding")
    void unknownBindingsResolveEmpty() {
        // moderner_beta:beta is the shared foundation, not a project binding: adopting it would
        // adopt every ordinary Moderner Beta world.
        assertTrue(HistoricalProfiles.byGenerationPreset(UPSTREAM_PRESET).isEmpty());
        assertTrue(HistoricalProfiles.byGenerationPreset("historical_experience:nope").isEmpty());
        assertTrue(HistoricalProfiles.byGenerationPreset("other_mod:beta_1_7_3").isEmpty());
        assertTrue(HistoricalProfiles.byGenerationPreset("not an identifier").isEmpty());
        assertTrue(HistoricalProfiles.byGenerationPreset("").isEmpty());
        assertTrue(HistoricalProfiles.byGenerationPreset((String) null).isEmpty());
        assertTrue(HistoricalProfiles.byGenerationPreset((HistoricalIdentifier) null).isEmpty());
    }

    @Test
    @DisplayName("resolve returns each revision's own resolved profile value")
    void resolveReturnsTheBindingsOwnResolvedProfile() {
        HistoricalProfile resolvedLegacy =
            HistoricalProfiles.resolve(PROFILE, LEGACY_ALIAS).orElseThrow();
        HistoricalProfile resolvedPreview =
            HistoricalProfiles.resolve(PROFILE, PREVIEW_ALIAS).orElseThrow();

        assertSame(legacy().profile(), resolvedLegacy);
        assertSame(preview().profile(), resolvedPreview);
        assertEquals(HistoricalProfiles.beta173(), resolvedLegacy);
        assertEquals(HistoricalProfiles.beta173ContentPreview(), resolvedPreview);
        assertEquals(resolvedLegacy.id(), resolvedPreview.id());
        assertNotEquals(resolvedLegacy, resolvedPreview,
            "the preset half must select which revision's profile is returned");

        assertSame(preview().profile(), HistoricalProfiles.resolve(
            HistoricalIdentifier.parse(PROFILE),
            HistoricalIdentifier.parse(PREVIEW_ALIAS)).orElseThrow());
    }

    @Test
    @DisplayName("cross pairs, foreign profiles and malformed input resolve to empty")
    void crossPairsResolveEmpty() {
        assertTrue(HistoricalProfiles.resolve("historical_experience:beta_1_8_1", LEGACY_ALIAS)
            .isEmpty());
        assertTrue(HistoricalProfiles.resolve("historical_experience:beta_1_8_1", PREVIEW_ALIAS)
            .isEmpty());
        assertTrue(HistoricalProfiles.resolve(PROFILE, UPSTREAM_PRESET).isEmpty());
        assertTrue(HistoricalProfiles.resolve(PROFILE, "historical_experience:skylands").isEmpty());
        assertTrue(HistoricalProfiles.resolve("not an identifier", LEGACY_ALIAS).isEmpty());
        assertTrue(HistoricalProfiles.resolve(PROFILE, "not an identifier").isEmpty());
    }

    @Test
    @DisplayName("resolve rejects null halves instead of silently resolving to empty")
    void resolveRejectsNullHalves() {
        // A raw-string overload exists too, so an explicit cast keeps this unambiguous.
        assertThrows(NullPointerException.class,
            () -> HistoricalProfiles.resolve((String) null, LEGACY_ALIAS));
        assertThrows(NullPointerException.class,
            () -> HistoricalProfiles.resolve(PROFILE, (String) null));
        assertThrows(NullPointerException.class, () -> HistoricalProfiles.resolve(
            (HistoricalIdentifier) null, HistoricalIdentifier.parse(PROFILE)));
        assertThrows(NullPointerException.class, () -> HistoricalProfiles.resolve(
            HistoricalIdentifier.parse(PROFILE), (HistoricalIdentifier) null));
    }

    @Test
    @DisplayName("v1 profile lookup and enumeration stay canonical and untouched")
    void v1LookupUnchanged() {
        assertSame(HistoricalProfiles.beta173(), HistoricalProfiles.byId(PROFILE).orElseThrow());
        assertEquals(HistoricalIdentifier.parse(LEGACY_ALIAS),
            HistoricalProfiles.beta173().generationPreset());
        assertEquals("Beta 1.7.3", HistoricalProfiles.beta173().displayName());
        assertEquals(net.historicalexperience.version.HistoricalVersions.all().size(),
            HistoricalProfiles.all().size());
        assertTrue(HistoricalProfiles.byId(PREVIEW_ALIAS).isEmpty(),
            "the preview alias is a generation preset, not a gameplay profile id");
        assertEquals(HistoricalProfiles.beta173(), HistoricalProfiles.all().get(
            HistoricalIdentifier.parse(PROFILE)));
    }

    @Test
    @DisplayName("marker recognition is exact and closed over the declared revisions")
    void markerRecognitionIsExactAndClosed() {
        assertTrue(HistoricalMarker.isHistoricalMarker(LEGACY_ALIAS));
        assertTrue(HistoricalMarker.isHistoricalMarker(PREVIEW_ALIAS));

        // Pinned as the exact list, not as a count: adding a revision must be a deliberate edit to
        // this expectation, and an accidental or unreviewed extra marker must fail here.
        // Derived from the registry rather than listed, so adding a version is a data change. The
        // invariants that matter are asserted generically in HistoricalVersionsTest.
        assertEquals(net.historicalexperience.version.HistoricalVersions.allRevisionAliases(),
            HistoricalMarker.HISTORICAL_PRESET_IDS);

        assertFalse(HistoricalMarker.isHistoricalMarker(UPSTREAM_PRESET),
            "the shared foundation is never a project marker");
        assertFalse(HistoricalMarker.isHistoricalMarker("moderner_beta:beta_1_7_3"));
        assertFalse(HistoricalMarker.isHistoricalMarker("other_mod:beta_1_7_3"));
        assertFalse(HistoricalMarker.isHistoricalMarker("historical_experience:beta_1_7_3_x"));
        assertFalse(HistoricalMarker.isHistoricalMarker(
            "historical_experience:beta_1_7_3_content_preview_99"),
            "a revision that was never declared is not a marker, however plausible its name");
        assertFalse(HistoricalMarker.isHistoricalMarker(""));
        assertFalse(HistoricalMarker.isHistoricalMarker(null));
    }

    @Test
    @DisplayName("each revision is addressed by exactly the marker this build recognises")
    void bindingsAreKeyedByTheirOwnMarker() {
        assertTrue(HistoricalMarker.isHistoricalMarker(legacy().rawSettingsAlias().value()));
        assertTrue(HistoricalMarker.isHistoricalMarker(preview().rawSettingsAlias().value()));
        assertEquals(HistoricalMarker.HISTORICAL_PRESET_ID, legacy().rawSettingsAlias().value());
        assertEquals(HistoricalMarker.CONTENT_PREVIEW_PRESET_ID,
            preview().rawSettingsAlias().value());
        assertEquals(HistoricalMarker.CONTENT_PREVIEW_PRESET_ID,
            HistoricalProfiles.beta173ContentPreview().generationPreset().value());
    }

    @Test
    @DisplayName("bindings are immutable data with enforced revision invariants")
    void bindingRejectsInvalidRevisions() {
        HistoricalProfile profile = HistoricalProfiles.beta173();
        HistoricalIdentifier foundation = HistoricalIdentifier.parse(UPSTREAM_PRESET);

        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalProfiles.GenerationBinding(profile, foundation, -1, false));
        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalProfiles.GenerationBinding(profile, foundation, 1, false),
            "a numbered revision must be marked experimental until it is accepted");
        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalProfiles.GenerationBinding(profile, foundation, 0, true),
            "the frozen declaration is not experimental");
        assertThrows(NullPointerException.class,
            () -> new HistoricalProfiles.GenerationBinding(null, foundation, 1, true));
        assertThrows(NullPointerException.class,
            () -> new HistoricalProfiles.GenerationBinding(profile, null, 1, true));

        Map<HistoricalIdentifier, HistoricalProfiles.GenerationBinding> bindings =
            HistoricalProfiles.generationBindings();
        assertThrows(UnsupportedOperationException.class, () -> bindings.clear());
    }

    @Test
    @DisplayName("a binding's foundation and raw alias never swap roles")
    void bindingExposesBothIdentifiersSeparately() {
        HistoricalProfiles.GenerationBinding binding =
            new HistoricalProfiles.GenerationBinding(
                HistoricalProfiles.beta173(),
                HistoricalIdentifier.parse(UPSTREAM_PRESET),
                HistoricalProfiles.CONTENT_PREVIEW_REVISION_1,
                true);

        assertEquals(HistoricalIdentifier.parse(UPSTREAM_PRESET), binding.foundationPreset());
        assertEquals(HistoricalIdentifier.parse(LEGACY_ALIAS), binding.rawSettingsAlias());
        assertNotNull(binding.profile());
        assertNotEquals(binding.foundationPreset(), binding.rawSettingsAlias());
    }

    @Test
    @DisplayName("a resolved pair always reports the profile the binding carries")
    void resolveIsConsistentWithTheBinding() {
        Optional<HistoricalProfiles.GenerationBinding> binding =
            HistoricalProfiles.byGenerationPreset(PREVIEW_ALIAS);
        Optional<HistoricalProfile> resolved = HistoricalProfiles.resolve(
            binding.orElseThrow().profile().id(), binding.orElseThrow().rawSettingsAlias());

        assertSame(binding.orElseThrow().profile(), resolved.orElseThrow());
    }
}


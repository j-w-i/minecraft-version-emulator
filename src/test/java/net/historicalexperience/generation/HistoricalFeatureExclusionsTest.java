package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.historicalexperience.HistoricalMarker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. No Minecraft runtime, no Moderner Beta classes, no registry.
 *
 * <p>Pins the two properties that make the generation-time restriction policy safe to ship:
 * <ol>
 *   <li><b>Append-only binding.</b> A restriction is selected by a persisted recipe alias. Revisions
 *       that were generated without restrictions must keep selecting none, or an existing world
 *       would silently start generating different content than the recipe it recorded.</li>
 *   <li><b>Each restriction is justified.</b> Every entry names a real placed feature and a reason
 *       that identifies why Beta 1.7.3 could not have it, so an unreviewed addition is visible.</li>
 * </ol>
 */
class HistoricalFeatureExclusionsTest {
    private static final String REVISION_1 = "historical_experience:beta_1_7_3_content_preview_1";
    private static final String REVISION_2 = "historical_experience:beta_1_7_3_content_preview_2";
    private static final String REVISION_3 = "historical_experience:beta_1_7_3_content_preview_3";
    private static final String REVISION_4 = "historical_experience:beta_1_7_3_content_preview_4";
    private static final String REVISION_5 = "historical_experience:beta_1_7_3_content_preview_5";

    @Test
    @DisplayName("already-generated revisions select no restrictions, so their worlds do not change")
    void earlierRevisionsKeepTheirRecordedRecipe() {
        for (String alias : List.of(
            HistoricalMarker.HISTORICAL_PRESET_ID, REVISION_1, REVISION_2, REVISION_3)) {
            assertEquals(List.of(), HistoricalFeatureExclusions.forAlias(alias),
                alias + " was generated without content restrictions and must never acquire them");
        }
    }

    @Test
    @DisplayName("only a revision that declares restrictions selects them")
    void onlyRestrictedRevisionSelectsRestrictions() {
        assertFalse(HistoricalFeatureExclusions.forAlias(REVISION_4).isEmpty(),
            "revision 4 is the revision that first declares the researched restrictions");
        assertFalse(HistoricalFeatureExclusions.forAlias(REVISION_5).isEmpty(),
            "revision 5 declares them plus the vanilla ore features it replaces");
        // Derived: every revision whose own declaration excludes something, plus the check that the
        // two Beta 1.7.3 restriction revisions are among them.
        Set<String> restricted = HistoricalFeatureExclusions.restrictedRevisions();
        assertTrue(restricted.contains(REVISION_4), "revision 4 must declare restrictions");
        assertTrue(restricted.contains(REVISION_5), "revision 5 must declare restrictions");
        for (String alias : restricted) {
            assertFalse(HistoricalFeatureExclusions.forAlias(alias).isEmpty(),
                alias + " is listed as restricted but declares nothing");
        }
        assertTrue(HistoricalMarker.isHistoricalMarker(REVISION_4));
        assertTrue(HistoricalMarker.isHistoricalMarker(REVISION_5));

        // Revision 5's list is a strict superset: it must not have dropped anything revision 4 had.
        Set<String> revision4 = HistoricalFeatureExclusions.forAlias(REVISION_4).stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(Collectors.toSet());
        Set<String> revision5 = HistoricalFeatureExclusions.forAlias(REVISION_5).stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(Collectors.toSet());
        assertTrue(revision5.containsAll(revision4),
            "revision 5 must keep every revision 4 restriction, missing: "
                + revision4.stream().filter(id -> !revision5.contains(id)).toList());
    }

    @Test
    @DisplayName("an unknown or foreign alias selects no restrictions")
    void unknownAliasesSelectNothing() {
        for (String alias : List.of(
            "moderner_beta:beta",
            "historical_experience:beta_1_7_3_content_preview_99",
            "other_mod:beta_1_7_3",
            "")) {
            assertEquals(List.of(), HistoricalFeatureExclusions.forAlias(alias));
        }
    }

    @Test
    @DisplayName("every restriction names a placed feature and states why it is anachronistic")
    void everyRestrictionIsJustified() {
        List<HistoricalFeatureExclusions.Exclusion> restrictions =
            HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS;

        assertFalse(restrictions.isEmpty());
        for (HistoricalFeatureExclusions.Exclusion exclusion : restrictions) {
            assertNotNull(exclusion.featureId());
            assertTrue(exclusion.featureId().contains(":"),
                exclusion.featureId() + " must be a namespaced identifier");
            assertNotNull(exclusion.reason());
            assertFalse(exclusion.reason().isBlank(),
                exclusion.featureId() + " must state why Beta 1.7.3 cannot have it");
        }

        Set<String> ids = restrictions.stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(Collectors.toSet());
        assertEquals(restrictions.size(), ids.size(), "a feature must not be listed twice");

        // The exact researched set. Adding an entry is a deliberate edit to this expectation.
        assertEquals(Set.of(
                "minecraft:ore_copper",
                "minecraft:amethyst_geode",
                "minecraft:underwater_magma",
                "minecraft:ore_granite_lower",
                "minecraft:ore_diorite_lower",
                "minecraft:ore_andesite_lower",
                "minecraft:ore_tuff",
                "minecraft:glow_lichen"),
            ids);
    }

    @Test
    @DisplayName("ores Beta 1.7.3 actually had are never excluded for being modern-looking")
    void betaOresAreNotExcluded() {
        Set<String> ids = HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS.stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(Collectors.toSet());

        // Removing these would not make a world more historical, it would remove Beta's own ores.
        // Their DISTRIBUTIONS are a separate, still-open fidelity task.
        for (String ore : List.of(
            "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
            "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small",
            "minecraft:ore_gold", "minecraft:ore_gold_lower",
            "minecraft:ore_redstone", "minecraft:ore_redstone_lower",
            "minecraft:ore_diamond", "minecraft:ore_diamond_medium",
            "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried",
            "minecraft:ore_lapis", "minecraft:ore_lapis_buried",
            "minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_clay")) {
            assertFalse(ids.contains(ore), ore + " is Beta 1.7.3 content and must not be excluded");
        }

        // Moderner Beta's own emerald placement is a documented OPEN question, not a decision.
        assertFalse(ids.contains("moderner_beta:ore_emerald_y95"),
            "emerald is deliberately left undecided pending evidence about Moderner Beta's beta "
                + "preset intent; do not add it silently");
    }

    @Test
    @DisplayName("diagnostics start clean and expose every field the verdict needs")
    void diagnosticsAreComplete() {
        // No generation has run in this unit-test process, so nothing can have been skipped and no
        // exclusion can have failed to resolve.
        assertEquals(0L, HistoricalFeatureExclusions.skippedPlacements());
        assertEquals(Set.of(), HistoricalFeatureExclusions.unresolvedExclusions());

        var diagnostics = HistoricalFeatureExclusions.diagnostics();
        assertEquals(
            Set.of("restrictedRevisions", "skippedPlacements", "unresolvedExclusions"),
            diagnostics.keySet());
        assertEquals("0", diagnostics.get("skippedPlacements"));
    }
}

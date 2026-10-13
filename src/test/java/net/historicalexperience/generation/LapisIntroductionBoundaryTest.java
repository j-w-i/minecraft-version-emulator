package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lapis ore is excluded exactly for the versions that predate it — the fix, and the much larger defect the fix's
 * own test uncovered.
 *
 * <h2>Why both directions are asserted</h2>
 *
 * <p>Lapis was measured generating in a fresh Beta 1.0 world while Beta 1.2's registry entry says that build
 * "introduces lapis ore". The obvious fix — two lines in the shared root list — would have been silently wrong:
 * Beta 1.2 through 1.6 all take that root, so it would have deleted lapis from five versions that really had it.
 * A test naming one version would not catch that; walking the registry in both directions does.
 *
 * <h2>The defect this test found while being written</h2>
 *
 * <p>Auditing the lists showed that {@code BETA_1_7_3_WITHOUT_AQUATIC_PLANTS} is built on
 * {@code BETA_1_7_3_WITH_REPLACED_ORES}, and that list carries <b>18 ore exclusions</b> — they exist because Beta
 * 1.7.3 and Beta 1.8.1 place their <b>own</b> ore population, so the vanilla features must be switched off. But
 * the plant chain inherits them, so every list downstream of it inherits them too, including the public
 * {@code _no_modern_content} revision of every Beta 1.9 prerelease and every Release. Those versions place
 * <b>no</b> ores of their own, and the probe measures the consequence directly: a fresh
 * {@code release_1_0_0} and {@code release_1_6_4} and {@code beta_1_9_pre_1} world contain <b>zero ores</b> —
 * no coal, iron, gold, diamond, redstone or lapis — while {@code beta_1_2}, {@code beta_1_7_3} and
 * {@code beta_1_8_1} contain all six.
 *
 * <p>That is a content LOSS rather than an anachronism, which is why no sweep of anachronistic blocks could see
 * it and why it survived this long. The last test pins it so the fix cannot be forgotten and so the assertion has
 * to be updated deliberately rather than deleted quietly.
 */
class LapisIntroductionBoundaryTest {
    private static final String BETA_1_2 = "historical_experience:beta_1_2";

    private static boolean excludesLapis(String alias) {
        return HistoricalFeatureExclusions.runtimeExclusionsForAlias(alias).stream()
            .anyMatch(exclusion -> exclusion.featureId().equals("minecraft:ore_lapis")
                || exclusion.featureId().equals("minecraft:ore_lapis_buried"));
    }

    /** The ore features that only a revision placing its own ores may switch off. */
    private static java.util.List<String> excludedOreFeatures(String alias) {
        return HistoricalFeatureExclusions.runtimeExclusionsForAlias(alias).stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .filter(id -> id.equals("minecraft:ore_coal_upper") || id.equals("minecraft:ore_iron_upper")
                || id.equals("minecraft:ore_lapis") || id.equals("minecraft:ore_diamond")
                || id.equals("minecraft:ore_redstone") || id.equals("minecraft:ore_gold"))
            .sorted()
            .toList();
    }

    @Test
    @DisplayName("the public revision of every pre-Beta-1.2 version excludes lapis, and Beta 1.2 keeps it")
    void theFixHoldsInBothDirections() {
        String betaOneTwo = BETA_1_2;
        assertFalse(excludesLapis(betaOneTwo + "_historical_envelope"),
            "Beta 1.2 introduced lapis ore, so its public revision must keep the vanilla feature - this is the "
                + "control that proves the pre-1.2 fix did not over-exclude");

        int pre = 0;
        for (String id : new String[] {"historical_experience:beta_1_0", "historical_experience:alpha_1_0_1",
                "historical_experience:alpha_1_0_11", "historical_experience:alpha_1_2_0",
                "historical_experience:alpha", "historical_experience:infdev_420",
                "historical_experience:infdev_611"}) {
            HistoricalVersion version = HistoricalVersions.byId(id).orElseThrow();
            HistoricalVersion.GenerationRevision latest = version.revisions().get(version.revisions().size() - 1);
            assertTrue(excludesLapis(latest.alias()),
                id + " predates lapis ore (Beta 1.2), so its public revision " + latest.alias()
                    + " must exclude the vanilla lapis features. The matrix sweep measured lapis generating in a "
                    + "fresh Beta 1.0 world before this was fixed");
            pre++;
        }
        assertTrue(pre == 7, "expected seven pre-1.2 profiles to check, saw " + pre);
    }

    @Test
    @DisplayName("the root list stays free of lapis, which is what keeps Beta 1.2 to 1.6 safe")
    void theRootDoesNotCarryLapis() {
        assertFalse(HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS.stream()
                .anyMatch(exclusion -> exclusion.featureId().equals("minecraft:ore_lapis")),
            "the root must not exclude lapis: Beta 1.2 through 1.6 inherit it and all of them had lapis");
    }

    @Test
    @DisplayName("FIXED: the thirteen profiles keep the vanilla ore features they place nothing to replace")
    void oreLossIsFixedForEveryAffectedProfile() {
        // This test replaced a pin that asserted the opposite and failed the moment the fix landed, which is what
        // a pin is for. The defect it recorded: the ore bans in BETA_1_7_3_WITH_REPLACED_ORES propagated down the
        // plant chain into the public revision of all six Beta 1.9 prereleases and all seven Release profiles,
        // none of which place ores of their own, so they generated NOTHING - measured as {} for
        // release_1_0_0, release_1_6_4 and beta_1_9_pre_1 in the matrix sweep.
        for (String id : new String[] {"historical_experience:beta_1_9_pre_1", "historical_experience:beta_1_9_pre_2",
                "historical_experience:beta_1_9_pre_3", "historical_experience:beta_1_9_pre_4",
                "historical_experience:beta_1_9_pre_5", "historical_experience:beta_1_9_pre_6",
                "historical_experience:release_1_0_0", "historical_experience:release_1_1",
                "historical_experience:release_1_2_5", "historical_experience:release_1_3",
                "historical_experience:release_1_4", "historical_experience:release_1_5",
                "historical_experience:release_1_6_4"}) {
            HistoricalVersion version = HistoricalVersions.byId(id)
                .orElseThrow(() -> new AssertionError(id + " must still be declared"));
            HistoricalVersion.GenerationRevision latest = version.revisions().get(version.revisions().size() - 1);
            assertFalse(latest.placesOwnOres(),
                id + " places no ores of its own, which is why leaving the bans in place emptied the world");
            assertTrue(excludedOreFeatures(latest.alias()).isEmpty(),
                id + "'s public revision " + latest.alias() + " still excludes " + excludedOreFeatures(
                    latest.alias()) + ", so it generates no ores at all");
        }
    }

    @Test
    @DisplayName("the versions that DO place their own ores keep the bans, so the two cannot double up")
    void ownOreRevisionsKeepTheBans() {
        // The fix must not have lifted the bans where they are correct: Beta 1.7.3's own-ore previews and
        // Beta 1.8.1 place a full historical ore population themselves, and running the vanilla features beside
        // it would double the ore in the world.
        for (String alias : new String[] {"historical_experience:beta_1_7_3_content_preview_10",
                "historical_experience:beta_1_8_1", "historical_experience:beta_1_8_1_no_modern_content"}) {
            assertTrue(excludedOreFeatures(alias).size() >= 6,
                alias + " places its own ores, so it must still exclude the vanilla ore features");
        }
    }
}

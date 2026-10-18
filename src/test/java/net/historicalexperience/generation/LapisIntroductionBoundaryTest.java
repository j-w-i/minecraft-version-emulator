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
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("the thirteen profiles place their era's own ores and exclude every vanilla ore feature")
    void affectedProfilesPlaceTheirOwnEraOres() {
        // This assertion is the OPPOSITE of the one it replaced, and that is deliberate. The old pin
        // required these profiles to place no ores of their own and keep vanilla's ore features. That was
        // correct for the defect it recorded - they had inherited the ore bans and generated NOTHING,
        // measured as {} for release_1_0_0, release_1_6_4 and beta_1_9_pre_1 - and then it became the next
        // defect: keeping vanilla's modern features meant generating at MODERN heights. A fresh
        // release_1_2_5 world measured diamond y=1..72, redstone y=1..30, lapis y=1..63, gold y=1..30,
        // iron y=1..71 and coal y=1..80, where every declared version's historical truth is diamond y 0..15.
        //
        // So the fix is a NEW revision per version, and both halves are asserted: the era population is
        // non-empty, and every vanilla ore feature it replaces is switched off.
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
            assertTrue(latest.placesOwnOres(),
                id + "'s public revision " + latest.alias() + " must place the era's own ore population, or vanilla's "
                    + "modern placed features generate it at modern heights");
            assertEquals(8, latest.orePopulation().size(),
                id + " is at or after Beta 1.2 and before emerald, so its population is the full Beta 1.7.3 one");
            assertTrue(excludedOreFeatures(latest.alias()).size() >= 6,
                id + "'s public revision " + latest.alias() + " still leaves " + excludedOreFeatures(
                    latest.alias()) + " unexcluded, so the modern feature would double up with the era population");
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

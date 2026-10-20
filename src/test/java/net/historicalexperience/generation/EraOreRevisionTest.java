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

import net.historicalexperience.version.OreRule;

import net.historicalexperience.version.Exclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the <b>era ore</b> revision: the population each version places, the exclusions that stop the
 * modern placed features from running beside it, and the append-only property that keeps the older
 * revisions meaning what they meant.
 *
 * <h2>The defect these assertions exist for</h2>
 *
 * <p>A fresh {@code historical_experience:release_1_2_5} world (measured over 1,349 chunks) generated
 * diamond at y=1..72, redstone y=1..30, lapis y=1..63, gold y=1..30, iron y=1..71 and coal y=1..80,
 * because that revision kept vanilla's modern features and placed no era population of its own. Every
 * version this project declares predates the 1.18 ore overhaul, so diamond's historical truth is y 0..15.
 *
 * <p>The population is the verified Beta 1.7.3 one, filtered - <b>no number in it is new</b>, which the
 * "exactly the verified rules" assertion below makes impossible to drift.
 */
class EraOreRevisionTest {
    /** The 17 vanilla ore placements a replaced population must switch off, and their feature ids. */
    private static final Set<String> VANILLA_ORE_FEATURES = Set.of(
        "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
        "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small",
        "minecraft:ore_gold", "minecraft:ore_gold_lower",
        "minecraft:ore_redstone", "minecraft:ore_redstone_lower",
        "minecraft:ore_diamond", "minecraft:ore_diamond_medium", "minecraft:ore_diamond_large",
        "minecraft:ore_diamond_buried",
        "minecraft:ore_lapis", "minecraft:ore_lapis_buried",
        "minecraft:ore_dirt", "minecraft:ore_gravel");

    private static final String EMERALD_FEATURE = "moderner_beta:ore_emerald_y95";

    private static HistoricalVersion.GenerationRevision latest(String versionId) {
        HistoricalVersion version = HistoricalVersions.byId(versionId)
            .orElseThrow(() -> new AssertionError(versionId + " must be declared"));
        return version.revisions().get(version.revisions().size() - 1);
    }

    private static Set<String> excludedIds(String alias) {
        return HistoricalFeatureExclusions.forAlias(alias).stream()
            .map(Exclusion::featureId)
            .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("the two versions that already placed their own ores are untouched, so nothing doubles up")
    void versionsThatAlreadyPlaceOwnOresKeepTheirRecipe() {
        // Beta 1.7.3 and Beta 1.8.1 already place a population and already exclude vanilla's features. A
        // second era-ore revision for them would double every ore count, so they must NOT gain one.
        assertEquals("historical_experience:beta_1_7_3_content_preview_10",
            latest("historical_experience:beta_1_7_3").alias());
        assertEquals("historical_experience:beta_1_8_1_no_modern_content",
            latest("historical_experience:beta_1_8_1").alias());
        assertFalse(latest("historical_experience:beta_1_7_3").alias().endsWith("_era_ores"));
        assertFalse(latest("historical_experience:beta_1_8_1").alias().endsWith("_era_ores"));
        assertEquals(BetaOreRules.BETA_1_7_3, latest("historical_experience:beta_1_7_3").orePopulation(),
            "Beta 1.7.3's population is still the full verified set");
    }

    @Test
    @DisplayName("every other declared version's newest revision places an era population")
    void everyOtherVersionGainsTheEraOreRevision() {
        int checked = 0;
        for (HistoricalVersion version : HistoricalVersions.all()) {
            if (version.id().equals("historical_experience:beta_1_7_3")
                || version.id().equals("historical_experience:beta_1_8_1")) {
                continue;
            }
            // The era-ore revision is looked up by name, not by position: a later revision (the 256-height one for
            // the Release profiles) may sit after it, and append-only means that is allowed as long as THIS revision's
            // stored recipe is unchanged.
            String eraOreAlias = "historical_experience:"
                + version.id().substring(version.id().indexOf(':') + 1) + "_era_ores";
            HistoricalVersion.GenerationRevision newest = version.revisions().stream()
                .filter(revision -> revision.alias().equals(eraOreAlias))
                .findFirst()
                .orElseThrow(() -> new AssertionError(version.id() + " has no era-ore revision"));
            assertTrue(version.latestRevision().placesOwnOres(),
                version.id() + " newest revision must still place the era's own ores");
            assertTrue(newest.placesOwnOres(),
                version.id() + " must place the era's own ores instead of vanilla's modern placement");
            assertTrue(newest.orePopulation().size() >= 6,
                version.id() + " lost an ore its era had: " + newest.orePopulation());
            assertTrue(excludedIds(newest.alias()).containsAll(VANILLA_ORE_FEATURES),
                version.id() + " leaves modern ore features running, missing: "
                    + VANILLA_ORE_FEATURES.stream()
                        .filter(id -> !excludedIds(newest.alias()).contains(id)).toList());
            checked++;
        }
        assertEquals(32, checked, "thirty-two versions place their era's own ore population");
    }

    @Test
    @DisplayName("the population is the verified Beta 1.7.3 rules filtered, never a new size or count")
    void populationIsTheVerifiedSetFiltered() {
        // Every rule in every revision must be byte-for-byte one of the verified Beta 1.7.3 rules. This is
        // the assertion that makes "do not invent vein sizes or counts" enforced rather than remembered.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                for (OreRule rule : revision.orePopulation()) {
                    assertTrue(BetaOreRules.BETA_1_7_3.contains(rule),
                        revision.alias() + " declares " + rule + ", which is not a verified Beta 1.7.3 rule");
                }
            }
        }

        // Release 1.2.5: the full eight (dirt, gravel, coal, iron, gold, redstone, diamond, lapis).
        assertEquals(8, latest("historical_experience:release_1_2_5").orePopulation().size());
        // Beta 1.0 predates lapis (Beta 1.2), so seven - and lapis is the one that is missing.
        List<OreRule> betaOneZero = latest("historical_experience:beta_1_0").orePopulation();
        assertEquals(7, betaOneZero.size());
        assertFalse(betaOneZero.stream().anyMatch(rule -> rule.name().equals("lapis")));
        assertTrue(betaOneZero.stream().anyMatch(rule -> rule.name().equals("redstone")));
        // Alpha v1.2.0 has redstone (Alpha v1.0.1) and no lapis.
        List<OreRule> alpha = latest("historical_experience:alpha_1_2_0").orePopulation();
        assertEquals(7, alpha.size());
        assertTrue(alpha.stream().anyMatch(rule -> rule.name().equals("redstone")));
        assertFalse(alpha.stream().anyMatch(rule -> rule.name().equals("lapis")));
        // Pre-Alpha: no redstone, no lapis, but diamond (restored Infdev 20100320, before every declared
        // profile) and dirt/gravel.
        for (String id : List.of("historical_experience:indev_latest",
                "historical_experience:infdev_420", "historical_experience:infdev_611")) {
            List<OreRule> preAlpha = latest(id).orePopulation();
            assertEquals(6, preAlpha.size(), id + " places exactly its six ores: " + preAlpha);
            assertFalse(preAlpha.stream().anyMatch(rule -> rule.name().equals("redstone")), id);
            assertFalse(preAlpha.stream().anyMatch(rule -> rule.name().equals("lapis")), id);
            assertTrue(preAlpha.stream().anyMatch(rule -> rule.name().equals("diamond")), id);
        }
    }

    @Test
    @DisplayName("emerald is excluded exactly before Release 1.3.1, and kept where the era had it")
    void emeraldBoundaryIsReleaseOneThreeOne() {
        for (String id : List.of("historical_experience:release_1_0_0", "historical_experience:release_1_1",
                "historical_experience:release_1_2_5", "historical_experience:beta_1_0",
                "historical_experience:beta_1_2", "historical_experience:beta_1_7_3",
                "historical_experience:alpha_1_2_0", "historical_experience:indev_latest")) {
            assertTrue(excludedIds(latest(id).alias()).contains(EMERALD_FEATURE),
                id + " predates emerald ore (Release 1.3.1) so its newest revision must exclude the feature");
        }
        for (String id : List.of("historical_experience:release_1_3", "historical_experience:release_1_4",
                "historical_experience:release_1_5", "historical_experience:release_1_6_4")) {
            assertFalse(excludedIds(latest(id).alias()).contains(EMERALD_FEATURE),
                id + " is the Release 1.3.1 baseline or later and really had emerald ore");
        }
    }

    @Test
    @DisplayName("the older revisions' STORED data is untouched: append-only means their recorded recipe still holds")
    void earlierRevisionsKeepTheirPublishedRecipe() {
        // The measured defect lived on `_no_modern_content`, which is published and may be recorded in a
        // world. It must keep DECLARING no population and keep its vanilla-ore list; the runtime fallback
        // and catch-all are code-level defaults, so the stored recipe stays exactly what it was.
        HistoricalVersion release = HistoricalVersions.byId("historical_experience:release_1_2_5").orElseThrow();
        HistoricalVersion.GenerationRevision previous = release.revisions().stream()
            .filter(revision -> revision.alias().equals(
                "historical_experience:release_1_2_5_no_modern_content"))
            .findFirst().orElseThrow();
        assertEquals("historical_experience:release_1_2_5_no_modern_content", previous.alias());
        assertEquals(List.of(), previous.orePopulation());
        assertFalse(excludedIds(previous.alias()).contains("minecraft:ore_diamond"));
        assertFalse(excludedIds(previous.alias()).contains("minecraft:ore_coal_upper"));

        // The pre-Beta published revisions are the same kind of record: still no stored population. What
        // they generate now comes from BetaOrePlacement's fallback, which OrePopulationInvariantTest pins.
        for (String alias : List.of("historical_experience:indev_latest_with_ores",
                "historical_experience:infdev_420_with_ores", "historical_experience:infdev_611_with_ores")) {
            assertEquals(List.of(), BetaOreRules.forAlias(alias),
                alias + " is published and must keep DECLARING no population; the era fallback supplies one "
                    + "at runtime without editing the recipe");
        }
    }
}

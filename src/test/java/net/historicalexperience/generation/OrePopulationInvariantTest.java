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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PERMANENT invariant for the era ore population and the historical ore catch-all.
 *
 * <p>This replaces the temporary {@code TemporaryOreAuditTest}, which only counted revisions with neither
 * a population nor an exclusion. Counting was not enough: 64 older revisions were "unprotected", so a
 * world recorded on one of them still generated 1.21.1's modern ores (diamond up to y=72-80). The
 * invariant is now stated positively, and both halves of the fix are load-bearing:
 *
 * <ul>
 *   <li>removing the era fallback from {@link BetaOrePlacement#populationForAlias(String)} makes the old
 *       revisions resolve to an empty population, so the first test fails;</li>
 *   <li>removing the catch-all from
 *       {@link HistoricalFeatureExclusions#eraOreExclusionsForAlias(String)} makes the old revisions that
 *       record no ore exclusion stop excluding the modern placements and emerald, so the third test
 *       fails.</li>
 * </ul>
 *
 * <p>Level: unit. No Minecraft runtime; the revision data and the resolution code are pure.
 */
class OrePopulationInvariantTest {
    private static final String DIAMOND_ORE = "minecraft:diamond_ore";
    private static final String EMERALD_FEATURE = "moderner_beta:ore_emerald_y95";

    @Test
    @DisplayName("every declared revision has a non-empty effective ore population containing diamond")
    void everyRevisionHasAnEffectiveDiamondPopulation() {
        List<String> broken = new ArrayList<>();
        int revisions = 0;
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                revisions++;
                List<OreRule> effective = BetaOrePlacement.populationForAlias(revision.alias());
                if (effective.isEmpty()) {
                    broken.add(revision.alias() + ": no population, declared or fallback");
                } else if (effective.stream().noneMatch(rule -> rule.blockId().equals(DIAMOND_ORE))) {
                    broken.add(revision.alias() + ": population has no diamond: " + effective);
                }
                for (OreRule rule : effective) {
                    assertTrue(BetaOreRules.BETA_1_7_3.contains(rule),
                        revision.alias() + " resolved " + rule + ", which is not a verified Beta 1.7.3 rule");
                }
            }
        }
        // The audit that motivated this test saw 138 revisions; the assertion is a floor because adding a
        // version is allowed and losing one silently is not.
        assertTrue(revisions >= 138, "the registry declared only " + revisions + " revisions");
        assertTrue(broken.isEmpty(),
            "these revisions would generate no era diamond in a historical world: " + broken);
    }

    @Test
    @DisplayName("an old revision resolves the era fallback, and a declared population still wins")
    void oldRevisionsResolveTheEraFallbackWithoutDoublingDeclaredPopulations() {
        for (String alias : List.of(
            // The old unprotected revisions that matter: the measured defect was on this kind of recipe.
            "historical_experience:beta_1_7_3_content_preview_4",
            "historical_experience:release_1_2_5_no_modern_content",
            "historical_experience:indev_latest",
            "historical_experience:indev_latest_with_ores",
            "historical_experience:release_1_2_5")) {
            List<OreRule> effective = BetaOrePlacement.populationForAlias(alias);
            assertFalse(effective.isEmpty(), alias + " must fall back to its era population");
            assertTrue(effective.stream().anyMatch(rule -> rule.blockId().equals(DIAMOND_ORE)),
                alias + " must place diamond: " + effective);
        }

        // A revision that declares a population is returned unchanged - never merged with the fallback,
        // which is what keeps the existing ore censuses at their current counts.
        assertEquals(BetaOreRules.BETA_1_7_3,
            BetaOrePlacement.populationForAlias("historical_experience:beta_1_7_3_content_preview_10"));
        assertEquals(BetaOreRules.BETA_1_7_3,
            BetaOrePlacement.populationForAlias("historical_experience:beta_1_8_1_no_modern_content"));
        assertEquals(8, BetaOrePlacement.populationForAlias(
            "historical_experience:release_1_2_5_era_ores").size());
        assertEquals(6, BetaOrePlacement.populationForAlias(
            "historical_experience:indev_latest_era_ores").size());

        // An alias no declared version owns places nothing, so ordinary worlds are untouched by the
        // fallback even if it were ever reached.
        assertEquals(List.of(), BetaOrePlacement.populationForAlias("moderner_beta:beta"));
        assertEquals(List.of(), BetaOrePlacement.populationForAlias("minecraft:normal"));
    }

    @Test
    @DisplayName("the effective exclusion set follows the emerald boundary and always removes modern ores")
    void emeraldAndModernOresAreExcludedExactlyWhereTheyMustBe() {
        List<String> broken = new ArrayList<>();
        for (HistoricalVersion version : HistoricalVersions.all()) {
            boolean eraHasEmerald = HistoricalVersions.eraHasEmeraldOre(version);
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                Set<String> effective = new HashSet<>(excludedIds(revision));
                effective.addAll(HistoricalFeatureExclusions.eraOreExclusionsForAlias(revision.alias()));

                boolean emeraldExcluded = effective.contains(EMERALD_FEATURE);
                if (eraHasEmerald && emeraldExcluded) {
                    broken.add(revision.alias()
                        + " is at or after the emerald boundary but excludes emerald ore");
                }
                if (!eraHasEmerald && !emeraldExcluded) {
                    broken.add(revision.alias()
                        + " predates emerald ore but leaves its only placed feature running");
                }

                // The fallback population never contains an emerald rule: emerald is not part of the
                // verified Beta 1.7.3 population and no declared revision adds one.
                List<OreRule> effectivePopulation =
                    BetaOrePlacement.populationForAlias(revision.alias());
                assertTrue(effectivePopulation.stream()
                        .noneMatch(rule -> rule.name().contains("emerald")),
                    revision.alias() + " must not place an emerald rule");

                // Every historical revision must remove the modern diamond placements, which are the
                // placements the measured y=72-80 defect came from.
                for (String diamond : List.of("minecraft:ore_diamond", "minecraft:ore_diamond_medium",
                        "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried")) {
                    if (!effective.contains(diamond)) {
                        broken.add(revision.alias() + " leaves " + diamond + " running");
                    }
                }
            }
        }
        assertTrue(broken.isEmpty(), String.join("; ", broken));
    }

    /**
     * The no-double-placement guarantee, stated as data: a revision that declares its own population must
     * already exclude every feature the catch-all removes, so the catch-all adds nothing for it and its
     * generated content is exactly what it was. Only revisions with an empty declared population rely on
     * the catch-all for these ids.
     */
    @Test
    @DisplayName("a revision that declares a population already excludes the whole catch-all, so nothing doubles")
    void declaredPopulationsAlreadyCarryTheCatchAll() {
        List<String> broken = new ArrayList<>();
        int checked = 0;
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                if (!revision.placesOwnOres()) {
                    continue;
                }
                checked++;
                Set<String> declared = excludedIds(revision);
                for (String id : HistoricalFeatureExclusions.eraOreExclusionsForAlias(revision.alias())) {
                    if (!declared.contains(id)) {
                        broken.add(revision.alias() + " declares a population but does not exclude " + id);
                    }
                }
            }
        }
        // Beta 1.7.3's six population revisions, Beta 1.8.1's three, and one era-ore revision per version
        // that does not already place its own ores.
        assertTrue(checked >= 41, "only " + checked + " revisions declare their own ore population");
        assertTrue(broken.isEmpty(),
            "the catch-all would change these declared-population revisions: " + broken);
    }

    private static Set<String> excludedIds(HistoricalVersion.GenerationRevision revision) {
        return revision.excludedPlacedFeatures().stream()
            .map(Exclusion::featureId)
            .collect(Collectors.toSet());
    }
}

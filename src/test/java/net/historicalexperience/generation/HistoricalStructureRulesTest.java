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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit.
 *
 * <p>The boundary these rules encode is narrow on purpose - structures did not exist at all before Beta
 * 1.8 - and these tests pin both halves of that: the early versions are restricted, and the later ones are
 * deliberately left alone rather than filled in with structure dates nobody has verified.
 */
class HistoricalStructureRulesTest {
    @Test
    @DisplayName("versions that had no structures at all are restricted, and to nothing")
    void earlyVersionsHaveNoStructures() {
        for (String versionId : List.of("historical_experience:infdev_420",
                "historical_experience:infdev_611", "historical_experience:alpha",
                "historical_experience:beta_1_7_3")) {
            HistoricalStructureRules.StructureRule rule =
                HistoricalStructureRules.ruleForVersion(versionId).orElseThrow(
                    () -> new AssertionError(versionId + " must declare a structure rule"));
            assertTrue(rule.restricts(), versionId + " restricts structures");
            assertEquals(Set.of(), rule.allowedStructureIds(),
                versionId + " allows none: an empty allowed set that restricts means exactly that");
            assertFalse(rule.evidence().isBlank(), "the rule must state its evidence");
        }
    }

    @Test
    @DisplayName("the rule applies through a revision alias, because an alias is a recipe not a version")
    void suppressionResolvesThroughTheAlias() {
        // Worlds store a generation alias, which names a recipe of a version. The structure boundary is a
        // property of the version, so a revision alias must reach the same answer as the version id.
        assertTrue(HistoricalStructureRules.suppresses(
            "historical_experience:beta_1_7_3_content_preview_9", "minecraft:village_plains"));
        assertTrue(HistoricalStructureRules.suppresses(
            "historical_experience:beta_1_7_3", "minecraft:mineshaft"));
        assertTrue(HistoricalStructureRules.suppresses(
            "historical_experience:alpha", "minecraft:stronghold"));
        assertTrue(HistoricalStructureRules.suppresses(
            "historical_experience:infdev_611", "minecraft:ruined_portal"));
    }

    @Test
    @DisplayName("every version at or after Beta 1.8.1 now carries its researched structure rule")
    void laterVersionsAreRuledOnFromEvidence() {
        // This assertion used to be the opposite one: no later version had a rule, because their structure
        // history had not been researched, and the test pinned that gap so filling it had to be deliberate.
        // It now IS deliberate: research/feature-timeline.json records each structure's introduction with
        // the wiki's own History line, so every one of these versions has a rule and the gap is closed.
        for (String versionId : List.of("historical_experience:beta_1_8_1",
                "historical_experience:beta_1_9_pre_3", "historical_experience:release_1_0_0",
                "historical_experience:release_1_1", "historical_experience:release_1_2_5",
                "historical_experience:release_1_6_4")) {
            assertTrue(HistoricalStructureRules.ruleForVersion(versionId).isPresent(),
                versionId + " must carry a structure rule now that its history is researched");
            // These versions DO have villages - but they are this project's historical village, so the
            // modern jigsaw villages are suppressed. That distinction is the whole point: the owner
            // generated a Beta 1.8.1 world and the villages "looked the same" as a current one, because
            // the vanilla village ids were still allowed beside the historical structure.
            for (String modern : List.of("minecraft:village_plains", "minecraft:village_desert",
                    "minecraft:village_savanna", "minecraft:village_taiga", "minecraft:village_snowy")) {
                assertTrue(HistoricalStructureRules.suppresses(versionId, modern),
                    versionId + " must generate the historical village instead of " + modern);
            }
            // ... and the versions below the boundary have no village at all, ours or vanilla's.
        }
        for (String earlier : List.of("historical_experience:beta_1_7_3",
                "historical_experience:alpha", "historical_experience:infdev_420",
                "historical_experience:infdev_611")) {
            assertTrue(HistoricalStructureRules.suppresses(earlier, "minecraft:village_plains"),
                earlier + " had no villages of any kind");
            assertFalse(net.historicalexperience.worldgen.HistoricalVillageRules
                .generationEnabledForAlias(earlier), earlier + " must not generate a historical village");
        }
        // And an unknown world is still never restricted: the rule table is keyed by declared profiles.
        assertFalse(HistoricalStructureRules.suppresses("moderner_beta:beta", "minecraft:village_plains"));
        assertFalse(HistoricalStructureRules.suppresses(null, "minecraft:village_plains"));
        assertFalse(HistoricalStructureRules.suppresses("historical_experience:alpha", null));
    }

    @Test
    @DisplayName("a rule that does not restrict may not name allowed structures, and must cite evidence")
    void contradictoryRulesAreRefused() {
        assertThrows(IllegalArgumentException.class, () ->
            new HistoricalStructureRules.StructureRule(false, Set.of("minecraft:village_plains"), "x"),
            "naming allowed structures while claiming not to restrict leaves two answers");
        assertThrows(IllegalArgumentException.class, () ->
            new HistoricalStructureRules.StructureRule(true, Set.of(), "  "),
            "a rule must state how it was established");
    }

    @Test
    @DisplayName("every version with a structure rule is a declared version")
    void rulesOnlyNameDeclaredVersions() {
        for (String versionId : HistoricalStructureRules.declaredVersions()) {
            assertTrue(HistoricalVersions.byId(versionId).isPresent(),
                versionId + " carries a structure rule but is not a declared version");
        }
    }

    @Test
    @DisplayName("the registry's hasStructures flag agrees with the structure rules")
    void agreesWithTheRegistryFlag() {
        // Two mechanisms now describe the same boundary. They must not drift, because a version that says
        // hasStructures=false while carrying no rule would advertise a guarantee nothing enforces.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            boolean ruleForbidsEverything = HistoricalStructureRules.ruleForVersion(version.id())
                .map(rule -> rule.restricts() && rule.allowedStructureIds().isEmpty())
                .orElse(false);
            if (!version.rules().hasStructures()) {
                assertTrue(ruleForbidsEverything,
                    version.id() + " claims to have no structures but declares no rule enforcing it");
            }
        }
    }
}

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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.historicalexperience.HistoricalMarker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. No Minecraft runtime; the entity-type identifiers are plain strings here, which is why
 * the rule table is kept free of Minecraft types.
 *
 * <p>Pins the two properties that make the spawn restriction trustworthy:
 * <ol>
 *   <li>The allowed set is exactly the Beta 1.7.3 entity registry, so an unreviewed addition or
 *       removal is a visible edit to this expectation.</li>
 *   <li>It is keyed by the <b>gameplay profile</b>, not by a generation revision, so it applies to
 *       every revision of the era without changing what any world's generation recipe recorded.</li>
 * </ol>
 */
class HistoricalSpawnRulesTest {
    private static final String BETA_PROFILE = "historical_experience:beta_1_7_3";

    @Test
    @DisplayName("the allowed set excludes registered but non-spawning giants")
    void allowedSetMatchesTheBetaRegistry() {
        // Derived from jc.class in the hash-verified b1.7.3 client; see
        // research/beta173-entity-types.json. Pinned as an exact list, not a count.
        assertEquals(List.of(
                "minecraft:chicken",
                "minecraft:cow",
                "minecraft:creeper",
                "minecraft:ghast",
                "minecraft:pig",
                "minecraft:sheep",
                "minecraft:skeleton",
                "minecraft:slime",
                "minecraft:spider",
                "minecraft:squid",
                "minecraft:wolf",
                "minecraft:zombie",
                "minecraft:zombified_piglin"),
            HistoricalSpawnRules.beta173MobsInOrder());
    }

    @Test
    void giantsAreNotNaturalSpawnsInAnyDeclaredProfile() {
        for (var version : net.historicalexperience.version.HistoricalVersions.all()) {
            assertFalse(version.rules().allowedNaturalSpawns().contains("minecraft:giant"), version.id());
        }
    }

    @Test
    @DisplayName("entity types that did not exist in Beta 1.7.3 are not allowed")
    void laterEntityTypesAreNotAllowed() {
        HistoricalSpawnRules.SpawnRule rule =
            HistoricalSpawnRules.forProfile(BETA_PROFILE).orElseThrow();

        // The six measured in a historical world's live spawn lists, plus representative later mobs.
        for (String later : List.of(
            "minecraft:bat", "minecraft:turtle", "minecraft:enderman", "minecraft:witch",
            "minecraft:zombie_villager", "minecraft:glow_squid", "minecraft:silverfish",
            "minecraft:cave_spider", "minecraft:blaze", "minecraft:magma_cube",
            "minecraft:wither_skeleton", "minecraft:villager", "minecraft:bee",
            "minecraft:axolotl", "minecraft:goat", "minecraft:frog", "minecraft:phantom",
            "minecraft:drowned", "minecraft:warden")) {
            assertFalse(HistoricalSpawnRules.isAllowed(rule, later),
                later + " did not exist in Beta 1.7.3 and must not be allowed to spawn");
        }
    }

    @Test
    @DisplayName("every era mob Moderner Beta lists is allowed, so no Beta content is removed")
    void betaEraMobsStayAllowed() {
        HistoricalSpawnRules.SpawnRule rule =
            HistoricalSpawnRules.forProfile(BETA_PROFILE).orElseThrow();

        // Exactly the entity types Moderner Beta's Beta biome spawn lists actually contain and that
        // the Beta registry also contains. None of these may be filtered out.
        for (String beta : List.of(
            "minecraft:chicken", "minecraft:cow", "minecraft:pig", "minecraft:sheep",
            "minecraft:wolf", "minecraft:squid", "minecraft:creeper", "minecraft:skeleton",
            "minecraft:spider", "minecraft:zombie", "minecraft:slime")) {
            assertTrue(HistoricalSpawnRules.isAllowed(rule, beta),
                beta + " existed in Beta 1.7.3 and must keep spawning");
        }
    }

    @Test
    @DisplayName("the rule is keyed by gameplay profile, so every revision of the era shares it")
    void ruleAppliesToEveryRevisionOfTheEra() {
        // Derived from the registry: exactly the versions whose rules restrict spawning, which
        // currently includes Beta 1.7.3 and Beta 1.8.1. Pinning one id here would have to be edited for
        // every new version, which is the churn the registry removes.
        assertEquals(
            net.historicalexperience.version.HistoricalVersions.all().stream()
                .filter(version -> version.rules().restrictsSpawns())
                .map(net.historicalexperience.version.HistoricalVersion::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()),
            HistoricalSpawnRules.restrictedProfiles());
        assertTrue(HistoricalSpawnRules.restrictedProfiles().contains(BETA_PROFILE));

        // Every declared revision resolves to its OWN version's profile. Deriving this means a new
        // version never edits the test, and it still catches the failure that matters: an alias that
        // resolves to the wrong era, which would apply one version's rules to another's world.
        for (var version : net.historicalexperience.version.HistoricalVersions.all()) {
            for (String alias : version.revisionAliases()) {
                assertEquals(version.id(), HistoricalSpawnRules.profileIdForAlias(alias),
                    alias + " must resolve to its own version");
            }
        }
        assertEquals(BETA_PROFILE,
            HistoricalSpawnRules.profileIdForAlias("historical_experience:beta_1_7_3"));
    }

    @Test
    @DisplayName("a non-historical or unknown alias selects no spawn rule")
    void unknownAliasesSelectNoRule() {
        for (String alias : List.of(
            "moderner_beta:beta",
            "historical_experience:beta_1_7_3_content_preview_99",
            "other_mod:beta_1_7_3",
            "")) {
            assertNull(HistoricalSpawnRules.profileIdForAlias(alias));
        }
        assertTrue(HistoricalSpawnRules.forProfile(null).isEmpty());
        assertTrue(HistoricalSpawnRules.forProfile("minecraft:overworld").isEmpty());

        // An unrestricted profile allows everything, which is what keeps ordinary worlds untouched.
        assertTrue(HistoricalSpawnRules.isAllowed(null, "minecraft:enderman"));
    }

    @Test
    @DisplayName("diagnostics start clean and expose every field the verdict needs")
    void diagnosticsAreComplete() {
        assertEquals(0L, HistoricalSpawnRules.filterInvocations());
        assertEquals(0L, HistoricalSpawnRules.removedEntries());
        assertEquals(Set.of(), HistoricalSpawnRules.removedEntityTypes());

        var diagnostics = HistoricalSpawnRules.diagnostics();
        assertEquals(
            Set.of("restrictedProfiles", "allowedEntityTypes", "filterInvocations", "removedEntries",
                "removedEntityTypes"),
            diagnostics.keySet());
        assertEquals(Integer.toString(HistoricalSpawnRules.BETA_1_7_3_MOBS.size()),
            diagnostics.get("allowedEntityTypes"));
    }
}

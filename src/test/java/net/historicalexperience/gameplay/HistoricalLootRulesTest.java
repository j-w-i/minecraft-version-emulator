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

import net.historicalexperience.version.DropRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Pins the verified Beta 1.7.3 drop table and the profile keying.
 *
 * <p>This file also carries the one part of the loot rule the runtime harness cannot measure: with no
 * player in a headless server, vanilla awards a mob no experience either, so an observation of zero
 * orbs proves nothing. The rule itself is pinned here instead, and the docs say plainly that the
 * suppression hook is applied-but-unexercised rather than pretending the measurement settled it.
 */
class HistoricalLootRulesTest {
    private static final String BETA_PROFILE = "historical_experience:beta_1_7_3";

    @Test
    @DisplayName("the drop table is exactly what the client's getDropItemId returns")
    void dropTableMatchesTheClient() {
        assertEquals(List.of(
                "minecraft:zombie", "minecraft:chicken", "minecraft:spider", "minecraft:cow"),
            HistoricalLootRules.BETA_1_7_3.stream()
                .map(DropRule::entityTypeId).toList());

        assertDrop("minecraft:zombie", "minecraft:feather");
        assertDrop("minecraft:chicken", "minecraft:feather");
        assertDrop("minecraft:spider", "minecraft:string");
        assertDrop("minecraft:cow", "minecraft:leather");
    }

    @Test
    @DisplayName("every rule cites the client field it was read from")
    void everyRuleCitesItsEvidence() {
        for (DropRule rule : HistoricalLootRules.BETA_1_7_3) {
            assertFalse(rule.evidence().isBlank(),
                rule.entityTypeId() + " must say which class and item field it came from");
            // The single-letter item fields are the reason this matters: gm.J is feather and gm.j is
            // arrow, so the evidence has to name the field rather than the item alone.
            assertTrue(rule.evidence().contains("gm."),
                rule.entityTypeId() + " must name the resolved item field, was: " + rule.evidence());
        }
    }

    @Test
    @DisplayName("no modern-only drop is in the table")
    void modernDropsAreAbsent() {
        Set<String> items = HistoricalLootRules.BETA_1_7_3.stream()
            .map(DropRule::itemId)
            .collect(java.util.stream.Collectors.toSet());

        // None of these exist in the Beta 1.7.3 item registry at all.
        for (String modern : List.of(
            "minecraft:rotten_flesh", "minecraft:beef", "minecraft:chicken",
            "minecraft:spider_eye", "minecraft:iron_ingot", "minecraft:carrot",
            "minecraft:potato", "minecraft:ink_sac")) {
            assertFalse(items.contains(modern), modern + " did not exist in Beta 1.7.3");
        }
        assertEquals(Set.of("minecraft:feather", "minecraft:string", "minecraft:leather"), items);
    }

    @Test
    @DisplayName("the rule is keyed by gameplay profile, so every revision of the era shares it")
    void ruleAppliesToEveryRevisionOfTheEra() {
        // Derived from the registry, for the same reason as the spawn rule's equivalent.
        assertEquals(
            net.historicalexperience.version.HistoricalVersions.all().stream()
                .filter(version -> version.rules().replacesLoot())
                .map(net.historicalexperience.version.HistoricalVersion::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()),
            HistoricalLootRules.restrictedProfiles());
        assertTrue(HistoricalLootRules.restrictedProfiles().contains(BETA_PROFILE));
        for (var version : net.historicalexperience.version.HistoricalVersions.all()) {
            for (String alias : version.revisionAliases()) {
                assertEquals(version.id(), HistoricalProfileLookup.profileIdForAlias(alias),
                    alias + " must resolve to its own version");
            }
        }
        assertEquals(BETA_PROFILE,
            HistoricalProfileLookup.profileIdForAlias("historical_experience:beta_1_7_3"));
    }

    @Test
    @DisplayName("an ordinary world resolves to no profile and therefore no rule")
    void ordinaryWorldsAreUntouched() {
        assertEquals(null, HistoricalProfileLookup.profileIdForAlias("moderner_beta:beta"));
        // Deliberately a revision number that will never be allocated. An earlier version of this test
        // used _content_preview_6, which was plausible-but-undeclared at the time and then BECAME real
        // when the cave-biome fix added it, so the test failed for the one reason it was never meant to:
        // the example went stale rather than the invariant breaking.
        assertEquals(null, HistoricalProfileLookup.profileIdForAlias(
            "historical_experience:beta_1_7_3_content_preview_99"));
        assertTrue(HistoricalLootRules.forEntity(null, "minecraft:zombie").isEmpty());
        assertTrue(HistoricalLootRules.forEntity("minecraft:overworld", "minecraft:zombie").isEmpty());

        // A mob with no Beta-specific drop keeps its modern loot table.
        assertTrue(HistoricalLootRules.forEntity(BETA_PROFILE, "minecraft:pig").isEmpty());
        assertTrue(HistoricalLootRules.forEntity(BETA_PROFILE, "minecraft:squid").isEmpty());
    }

    @Test
    @DisplayName("experience suppression is a profile rule, pinned here because the harness cannot measure it")
    void experienceSuppressionIsRuleLevel() {
        assertTrue(HistoricalLootRules.suppressesExperience(BETA_PROFILE));
        assertFalse(HistoricalLootRules.suppressesExperience(null));
        assertFalse(HistoricalLootRules.suppressesExperience("minecraft:overworld"));

        // Beta's drop count is nextInt(3): 0, 1 or 2, expressed as per-rule data now that versions can
        // differ. A zero roll in a single trial is expected, which is why the runtime probe uses several
        // trials and refuses an all-zero result.
        assertEquals(3, HistoricalLootRules.BETA_1_7_3_BY_ENTITY.get("minecraft:zombie")
            .maxExclusiveCount());
    }

    private static void assertDrop(String entityTypeId, String itemId) {
        DropRule rule =
            HistoricalLootRules.forEntity(BETA_PROFILE, entityTypeId).orElseThrow();
        assertEquals(itemId, rule.itemId(), entityTypeId + " must drop " + itemId);
    }
}

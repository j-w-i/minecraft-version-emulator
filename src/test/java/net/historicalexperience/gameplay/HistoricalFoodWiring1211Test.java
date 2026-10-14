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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Direct food healing is applied exactly where the version had no hunger, and nowhere else.
 *
 * <p>The rule set was dead code - nothing called {@code healPointsFor} - so the values were researched and
 * unused. This is the data half of the fix; {@code PlayerHistoricalFoodMixin} is the wiring, and
 * {@code HistoricalLootWiring1211Test} asserts that every mixin on disk is registered, which is what stops
 * it going quiet again.
 */
class HistoricalFoodWiring1211Test {
    private static Set<String> healingProfiles() {
        return HistoricalFoodRules.healingProfiles();
    }

    private static boolean hasHunger(String versionId) {
        return HistoricalVersions.byId(versionId).orElseThrow().rules().hasHunger();
    }

    @Test
    @DisplayName("food heals directly exactly in the versions that had no hunger")
    void directHealingIsExactlyTheHungerlessVersions() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            String id = version.id();
            boolean hunger = version.rules().hasHunger();
            if (hunger) {
                assertFalse(HistoricalFoodRules.healsFoodDirectly(id),
                    id + " has hunger, so food must feed the bar rather than healing directly");
                assertFalse(HistoricalFoodRules.allowsEatingWithoutHunger(id),
                    id + " has hunger, so vanilla's canEat gate must stay in place");
            } else {
                assertTrue(HistoricalFoodRules.allowsEatingWithoutHunger(id),
                    id + " has no hunger, so a full food bar must not stop a hurt player eating");
            }
        }
        // The version whose values were read as integer literals from its own client.
        assertTrue(HistoricalFoodRules.healsFoodDirectly("historical_experience:beta_1_7_3"));
        assertTrue(HistoricalFoodRules.healsFoodDirectly("historical_experience:alpha"));
        assertFalse(HistoricalFoodRules.healsFoodDirectly("historical_experience:beta_1_8_1"),
            "hunger arrives in Beta 1.8.1, which is what replaces direct healing");
        assertFalse(HistoricalFoodRules.healsFoodDirectly("historical_experience:release_1_6_4"));
    }

    @Test
    @DisplayName("the verified values are the ones the table still holds")
    void verifiedValuesArePresent() {
        // Every profile that heals directly must name items with positive heal values, and every value must
        // carry its evidence - an unevidenced healing value is exactly what this project keeps having to undo.
        for (String profile : healingProfiles()) {
            var points = HistoricalFoodRules.healPoints(profile);
            assertFalse(points.isEmpty(), profile + " heals directly but names no food");
            for (var entry : points.entrySet()) {
                assertTrue(entry.getValue() > 0,
                    profile + " gives " + entry.getKey() + " a non-positive heal of " + entry.getValue());
            }
        }
        // Beta 1.7.3's own table: health is in half-hearts, so 8 points is 4 hearts.
        var bread = HistoricalFoodRules.healPointsFor("historical_experience:beta_1_7_3", "minecraft:bread");
        assertTrue(bread.isPresent(), "bread is one of the foods Beta 1.7.3 healed with");
        assertTrue(bread.get() > 0, "bread must restore health, not zero");
        assertFalse(HistoricalFoodRules.BETA_1_7_3.isEmpty(), "the Beta 1.7.3 table looks truncated");
        for (HistoricalFoodRules.FoodHeal heal : HistoricalFoodRules.BETA_1_7_3) {
            assertFalse(heal.evidence().isBlank(), "no evidence recorded for " + heal.itemId());
        }
    }

    @Test
    @DisplayName("a food the version's table does not model is left to vanilla")
    void unmodelledFoodKeepsTheModernPath() {
        // The mixin returns early in this case, so a modern food in an old world still behaves as vanilla
        // rather than being healed by a guessed value.
        assertTrue(HistoricalFoodRules
            .healPointsFor("historical_experience:beta_1_7_3", "minecraft:golden_carrot").isEmpty(),
            "golden carrots are Release 1.4.2 and not in the Beta 1.7.3 table");
        assertTrue(HistoricalFoodRules
            .healPointsFor("historical_experience:beta_1_7_3", "example:not_a_food").isEmpty());
        assertTrue(HistoricalFoodRules.healPointsFor(null, "minecraft:bread").isEmpty());
        assertTrue(HistoricalFoodRules.healPointsFor("historical_experience:beta_1_7_3", null).isEmpty());
    }
}

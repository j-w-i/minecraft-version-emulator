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
 */package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two pre-Release-1.8 table rules and the version-gated enchantment list.
 *
 * <p>The list is checked for its boundaries against the wiki record rather than for its size: Prerelease 4's seventeen,
 * Release 1.1's four, Release 1.4.2's Thorns, and the modern enchantments that no declared version may hand out.
 */
class HistoricalEnchantingTest {
    private static final String PRE_3 = "historical_experience:beta_1_9_pre_3";
    private static final String PRE_4 = "historical_experience:beta_1_9_pre_4";

    @Test
    @DisplayName("the old table rules cover every declared version, because Release 1.8 is past all of them")
    void oldRulesCoverEveryDeclaredVersion() {
        for (var version : HistoricalVersions.all()) {
            assertTrue(HistoricalEnchanting.usesOldTableRules(version.id()),
                version.id() + " is before Release 1.8, where the cost became flat and lapis arrived");
        }
        assertFalse(HistoricalEnchanting.usesOldTableRules(null), "an ordinary world keeps the modern table");
        assertTrue(HistoricalEnchanting.MODERN_ENCHANTING_FROM
            > HistoricalVersions.byId("historical_experience:release_1_6_4").orElseThrow().chronology());
    }

    @Test
    @DisplayName("Prerelease 4's seventeen enchantments are the first any declared version has")
    void prereleaseFourSet() {
        var set = HistoricalEnchanting.prereleaseFourEnchantments();
        assertEquals(17, set.size(), "the wiki names seventeen enchantments for Prerelease 4: " + set);
        assertTrue(set.contains(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING),
            "Feather Falling is one of them, which is why the placeholder reason is about boots");
        assertTrue(set.contains(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING));
        assertFalse(set.contains(net.minecraft.world.item.enchantment.Enchantments.MENDING),
            "Mending is 1.9 content");
        assertFalse(set.contains(net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER),
            "Frost Walker is 1.9 content");
    }

    @Test
    @DisplayName("Thorns waits for Release 1.4.2 and the rest arrive no later")
    void boundaries() {
        var thorns = net.minecraft.world.item.enchantment.Enchantments.THORNS;
        assertFalse(HistoricalEnchanting.available("historical_experience:release_1_4", thorns),
            "Thorns is 12w50a, inside Release 1.4.2");
        assertTrue(HistoricalEnchanting.available("historical_experience:release_1_6_4", thorns));

        var flame = net.minecraft.world.item.enchantment.Enchantments.FLAME;
        assertFalse(HistoricalEnchanting.available("historical_experience:release_1_0_0", flame),
            "Flame, Infinity, Power and Punch arrive in Release 1.1");
        assertTrue(HistoricalEnchanting.available("historical_experience:release_1_1", flame));
    }

    @Test
    @DisplayName("the modern enchantments are in no declared version")
    void modernEnchantmentsAreRefused() {
        for (String version : new String[] {"beta_1_9_pre_3", "beta_1_9_pre_4", "release_1_0_0", "release_1_1",
            "release_1_2_5", "release_1_3", "release_1_4", "release_1_5", "release_1_6_4"}) {
            for (var modern : new net.minecraft.resources.ResourceKey[] {
                net.minecraft.world.item.enchantment.Enchantments.MENDING,
                net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER,
                net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE,
                net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE,
                net.minecraft.world.item.enchantment.Enchantments.LOYALTY,
                net.minecraft.world.item.enchantment.Enchantments.RIPTIDE,
                net.minecraft.world.item.enchantment.Enchantments.CHANNELING,
                net.minecraft.world.item.enchantment.Enchantments.IMPALING,
                net.minecraft.world.item.enchantment.Enchantments.MULTISHOT,
                net.minecraft.world.item.enchantment.Enchantments.QUICK_CHARGE,
                net.minecraft.world.item.enchantment.Enchantments.PIERCING,
                net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE,
                net.minecraft.world.item.enchantment.Enchantments.SOUL_SPEED,
                net.minecraft.world.item.enchantment.Enchantments.SWIFT_SNEAK}) {
                assertFalse(HistoricalEnchanting.available("historical_experience:" + version, modern),
                    version + " must not offer " + modern.location());
            }
        }
    }

    @Test
    @DisplayName("Prerelease 3 has no enchantments at all, and an ordinary world keeps the whole list")
    void theBuildWithoutDataAndTheWorldWithoutAVersion() {
        assertFalse(HistoricalEnchanting.available(PRE_3,
            net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING),
            "Prerelease 3 applied no real enchantment");
        assertTrue(HistoricalEnchanting.available(PRE_4,
            net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING));
        assertTrue(HistoricalEnchanting.available(null,
            net.minecraft.world.item.enchantment.Enchantments.MENDING),
            "an ordinary world is left alone");
    }

    @Test
    @DisplayName("the Prerelease 3 placeholder is Feather Falling I, fixed rather than rolled")
    void placeholderIsFeatherFallingOne() {
        // The owner's rule: every item enchanted in Prerelease 3 becomes Feather Falling I at Prerelease 4, so the
        // reveal cannot depend on a roll or on a seed that moves between the two builds.
        assertEquals(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING,
            HistoricalEnchanting.placeholderEnchantment());
        assertEquals(1, HistoricalEnchanting.placeholderEnchantmentLevel());
        assertTrue(HistoricalEnchanting.prereleaseFourEnchantments().contains(
            HistoricalEnchanting.placeholderEnchantment()),
            "the placeholder must be an enchantment the destination build actually has");
        assertFalse(HistoricalEnchanting.available(PRE_3, HistoricalEnchanting.placeholderEnchantment()),
            "Prerelease 3 still offers no real enchantment of its own; the placeholder is applied by the table, "
                + "not drawn from the version's set");
    }

    @Test
    @DisplayName("an enchantment nobody dated is refused, which is the opposite direction to the item table")
    void undatedEnchantmentIsRefused() {
        // The two tables deliberately disagree; this records that it is a decision and not an oversight.
        assertTrue(HistoricalItemAvailability.existsIn("historical_experience:beta_1_7_3", "minecraft:apple"),
            "an undated ITEM stays in a chest");
        assertFalse(HistoricalEnchanting.available("historical_experience:beta_1_7_3",
            net.minecraft.world.item.enchantment.Enchantments.DENSITY),
            "an undated ENCHANTMENT is one this table has never heard of, so it is refused");
    }
}
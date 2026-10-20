/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audits G9 and A-10.
 *
 * <p>G9: geared monsters arrive in Release 1.4.2 (12w32a - armour and weapons, monster item pickup and spawn
 * enchantments together). The project's Release 1.4 profile IS the 1.4.2 stable baseline, but the gates were proxied
 * by Release 1.6.4, so Release 1.4 and 1.5 had no geared monsters and Release 1.6.4 stood in for a boundary it is not.
 *
 * <p>A-10: the crafting allow-list put the compass and clock in {@code always()}, so both were craftable from Alpha
 * v1.0.1, contradicting the project's own creative catalogue (compass Alpha v1.1.0, clock Alpha v1.2.0).
 */
class ReleaseGearedMobsAndToolsTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("geared monsters appear in Release 1.4, not only in 1.6.4")
    void gearedMobsStartAtRelease14() {
        for (Feature feature : new Feature[] {Feature.NO_MONSTER_ITEM_PICKUP, Feature.NO_MONSTER_GEAR,
                Feature.NO_MONSTER_ENCHANTS}) {
            assertTrue(Features.active(feature, P + "release_1_3"), feature + " applies before 1.4.2");
            for (String has : new String[] {"release_1_4", "release_1_5", "release_1_6_4"}) {
                assertFalse(Features.active(feature, P + has), feature + " must not apply to " + has);
            }
        }
    }

    @Test
    @DisplayName("baby zombies are 1.6.2, so the 1.6.4 proxy stays correct for them")
    void babyZombiesKeepTheProxy() {
        assertTrue(Features.active(Feature.NO_BABY_ZOMBIES, P + "release_1_5"));
        assertFalse(Features.active(Feature.NO_BABY_ZOMBIES, P + "release_1_6_4"));
    }

    @Test
    @DisplayName("the redstone lamp is Release 1.2.1 content, so Release 1.2.5 crafts it")
    void redstoneLampDate() {
        // minecraft.wiki, Redstone Lamp history: "1.2.1 (12w07a) Added redstone lamps"; Release 1.2.5 is this
        // project's profile for that line.
        assertTrue(HistoricalCraftingEra.isRefused(P + "release_1_1", "minecraft:redstone_lamp", null),
            "Release 1.1 predates the lamp");
        assertFalse(HistoricalCraftingEra.isRefused(P + "release_1_2_5", "minecraft:redstone_lamp", null),
            "Release 1.2.5 is the 1.2.1 baseline and had the lamp");
        assertFalse(HistoricalCraftingEra.isRefused(P + "release_1_3", "minecraft:redstone_lamp", null));
    }

    @Test
    @DisplayName("compass is Alpha 1.1.0 and clock is Alpha 1.2.0, as the creative catalogue already said")
    void compassAndClockDates() {
        assertTrue(HistoricalCraftingEra.isRefused(P + "alpha_1_0_1", "minecraft:compass", null));
        assertFalse(HistoricalCraftingEra.isRefused(P + "alpha_1_1_0", "minecraft:compass", null));
        assertTrue(HistoricalCraftingEra.isRefused(P + "alpha_1_0_1", "minecraft:clock", null));
        assertTrue(HistoricalCraftingEra.isRefused(P + "alpha_1_1_0", "minecraft:clock", null));
        assertFalse(HistoricalCraftingEra.isRefused(P + "alpha_1_2_0", "minecraft:clock", null));
    }
}

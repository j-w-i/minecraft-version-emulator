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
 * Level: unit. minecraft.wiki, Java Edition 1.4.2 additions: "Potion of Night Vision - Brewed by adding a golden carrot to
 * an awkward potion"; "Potion of Invisibility - Brewed by adding a fermented spider eye to a potion of Night Vision".
 *
 * <p>The gate is additionally runtime-verified: a Release 1.3 brewing stand kept its awkward potion while a Release 1.6.4
 * stand produced Night Vision from the same inputs.
 */
class PotionMixTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("only the two 1.4.2 potions are refused, and only before Release 1.4")
    void mixes() {
        for (String early : new String[] {"release_1_0_0", "release_1_2_5", "release_1_3"}) {
            assertTrue(Features.active(Feature.NO_1_4_2_POTIONS, P + early), early);
            assertTrue(HistoricalPotionMixes.refuseBrew("minecraft:night_vision", P + early), early);
            assertTrue(HistoricalPotionMixes.refuseBrew("minecraft:invisibility", P + early), early);
            assertTrue(HistoricalPotionMixes.refuseBrew("minecraft:long_invisibility", P + early), early);
            assertFalse(HistoricalPotionMixes.refuseBrew("minecraft:swiftness", P + early),
                early + " can still brew Swiftness");
            assertFalse(HistoricalPotionMixes.refuseBrew("minecraft:healing", P + early),
                early + " can still brew Healing");
        }
        for (String later : new String[] {"release_1_4", "release_1_6_4"}) {
            assertFalse(Features.active(Feature.NO_1_4_2_POTIONS, P + later), later);
            assertFalse(HistoricalPotionMixes.refuseBrew("minecraft:night_vision", P + later), later);
        }
        assertFalse(HistoricalPotionMixes.refuseBrew("minecraft:night_vision", null), "ordinary worlds");
        assertFalse(HistoricalPotionMixes.refuseBrew(null, P + "release_1_0_0"), "no potion id, no claim");
    }

    @Test
    @DisplayName("all four variants are listed")
    void variants() {
        assertTrue(HistoricalPotionMixes.RELEASE_1_4_2_POTIONS.containsAll(
            java.util.Set.of("minecraft:night_vision", "minecraft:long_night_vision",
                "minecraft:invisibility", "minecraft:long_invisibility")));
    }
}

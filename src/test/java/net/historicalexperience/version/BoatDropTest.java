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
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. minecraft.wiki, Boat history, 12w27a (Release 1.3): "Boats now drop their item instead of 3 oak planks and
 * 2 sticks if broken by a player. Instead, it needs to fall certain heights to drop 3 oak wood planks and 2 sticks."
 * 15w51a (Release 1.9) changed the drop again, above every declared build, so the window is Release 1.3 onward.
 */
class BoatDropTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the boat item drop starts at Release 1.3, not before")
    void window() {
        for (String early : new String[] {"release_1_0_0", "release_1_1", "release_1_2_5"}) {
            assertFalse(Features.active(Feature.BOAT_DROP_ITEM_ON_PLAYER_BREAK, P + early),
                early + " drops planks and sticks");
        }
        for (String later : new String[] {"release_1_3", "release_1_4", "release_1_5", "release_1_6_4"}) {
            assertTrue(Features.active(Feature.BOAT_DROP_ITEM_ON_PLAYER_BREAK, P + later),
                later + " drops the boat item when a player breaks it");
        }
        assertFalse(Features.active(Feature.BOAT_DROP_ITEM_ON_PLAYER_BREAK, (String) null));
    }
}

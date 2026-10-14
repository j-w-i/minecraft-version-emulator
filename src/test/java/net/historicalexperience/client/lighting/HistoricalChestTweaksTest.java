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
package net.historicalexperience.client.lighting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The windows for the old chest and ender chest.
 *
 * <p>The chest model was redone in the <b>Beta 1.8 Pre-release</b> (minecraft.wiki, Chest history - <i>"The chest's
 * model has been redone. Rather than a usual cube, it uses an entity model"</i>). Ender chests were <b>added in Release
 * 1.3.1</b> (12w21a - <i>"Added ender chests."</i>). This gates the NT cube aesthetic to versions where an ender chest
 * existed; it is not a claim that authentic vanilla ender chests had cube geometry.
 */
class HistoricalChestTweaksTest {
    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId(id).orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    @Test
    @DisplayName("the old cube chest is every declared build below Beta 1.8, and none at or after it")
    void oldChestWindow() {
        assertTrue(HistoricalChestTweaks.availableForChest(at("historical_experience:infdev_420")));
        assertTrue(HistoricalChestTweaks.availableForChest(at("historical_experience:alpha_1_0_1")));
        assertTrue(HistoricalChestTweaks.availableForChest(at("historical_experience:beta_1_7_3")),
            "the cube is what Beta 1.7.3 had");
        assertFalse(HistoricalChestTweaks.availableForChest(at("historical_experience:beta_1_8_1")),
            "the model was redone in the Beta 1.8 Pre-release");
        assertFalse(HistoricalChestTweaks.availableForChest(at("historical_experience:release_1_6_4")));
        assertFalse(HistoricalChestTweaks.availableForChest(null), "unknown is not 'yes'");
    }

    @Test
    @DisplayName("the ender chest starts at Release 1.3.1 and is offered for NO beta profile")
    void oldEnderChestWindow() {
        // The commonly repeated claim is that the ender chest arrived in Beta 1.8. The wiki's own history says 1.3.1,
        // so every beta profile must be excluded - giving one to a beta world would add a block the era lacked.
        assertFalse(HistoricalChestTweaks.availableForEnderChest(at("historical_experience:beta_1_7_3")),
            "the ender chest did not exist in Beta 1.7.3");
        assertFalse(HistoricalChestTweaks.availableForEnderChest(at("historical_experience:beta_1_8_1")),
            "nor in Beta 1.8, despite that being the common claim");
        assertFalse(HistoricalChestTweaks.availableForEnderChest(at("historical_experience:release_1_2_5")));
        assertTrue(HistoricalChestTweaks.availableForEnderChest(at("historical_experience:release_1_3")),
            "added 12w21a, in the Release 1.3 line");
        assertTrue(HistoricalChestTweaks.availableForEnderChest(at("historical_experience:release_1_6_4")));
        assertFalse(HistoricalChestTweaks.availableForEnderChest(null));
    }

    @Test
    @DisplayName("historical blockstate assets are always baked without changing non-chest assets")
    void chestBlockstatesAreAvailableAtTitleTime() {
        assertTrue(HistoricalChestTweaks.hasHistoricalBlockState("minecraft", "chest"));
        assertTrue(HistoricalChestTweaks.hasHistoricalBlockState("minecraft", "trapped_chest"));
        assertTrue(HistoricalChestTweaks.hasHistoricalBlockState("minecraft", "ender_chest"));
        assertFalse(HistoricalChestTweaks.hasHistoricalBlockState("other_mod", "chest"));
        assertFalse(HistoricalChestTweaks.hasHistoricalBlockState("minecraft", "barrel"));
    }

    @Test
    @DisplayName("model selection is inert with no historical version, so ordinary worlds stay vanilla")
    void ordinaryWorldsAreUntouched() {
        assertFalse(HistoricalChestTweaks.wantsOldChest(null));
        assertFalse(HistoricalChestTweaks.wantsOldEnderChest(null));
        assertFalse(HistoricalChestTweaks.oldChest());
        assertFalse(HistoricalChestTweaks.oldEnderChest());
    }
}

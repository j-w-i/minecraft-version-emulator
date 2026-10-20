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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the boundary the owner caught: the sword block's fall damage reduction is a NARROWER window than blocking.
 *
 * <p>Blocking is Beta 1.8 Pre-release through 1.9. The fall damage reduction was removed earlier, in Beta 1.9
 * Prerelease, so a version can hold the stance without the reduction - and Beta 1.9 Prerelease 3 is exactly that
 * version. The first version of the port used the blocking gate and wrongly reduced fall damage there.
 */
class BlockFallDamageBoundaryTest {
    private static final List<String> DECLARED = List.of(
        "historical_experience:infdev_420",
        "historical_experience:infdev_611",
        "historical_experience:alpha",
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_6_4");

    @Test
    void onlyBeta181FallsInsideTheWindow() {
        List<String> inside = DECLARED.stream()
            .filter(HistoricalGameplayTweaks::blockFallDamageReductionEnabled)
            .toList();

        assertTrue(inside.equals(List.of("historical_experience:beta_1_8_1")),
            "the fall damage reduction belongs to Beta 1.8.1 alone among the declared profiles, but got: "
                + inside);
    }

    @Test
    void theStanceOutlivesTheReduction() {
        String preReleaseThree = "historical_experience:beta_1_9_pre_3";

        assertTrue(Features.active(Feature.OLD_SWORD_BLOCKING, preReleaseThree),
            "Beta 1.9 Prerelease 3 still has the sword block stance");
        assertFalse(Features.active(Feature.SWORD_BLOCK_FALL_PROTECTION, preReleaseThree),
            "but not the fall damage reduction, which was removed in Beta 1.9 Prerelease");
    }

    @Test
    void ordinaryVersionsGetNeither() {
        assertFalse(Features.active(Feature.SWORD_BLOCK_FALL_PROTECTION, (String) null));
        assertFalse(Features.active(Feature.SWORD_BLOCK_FALL_PROTECTION, "minecraft:overworld"));
    }
}

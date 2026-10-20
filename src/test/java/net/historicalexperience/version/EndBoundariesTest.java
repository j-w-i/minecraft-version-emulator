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
 * Level: unit. Audit G10 — two End boundaries that were implemented in the wrong shape.
 *
 * <p>minecraft.wiki, Ender Dragon history: the dragon is added in <b>Beta 1.9 Prerelease 6</b>. The End itself arrived
 * in Prerelease 4 with the central island and the obsidian pillars, so Pre4 and Pre5 have an End with no dragon; the
 * audit found no spawn suppression at all, so both builds had a live dragon and Pre6 lost its milestone.
 *
 * <p>minecraft.wiki, End Portal Frame history: the frame and eye of ender are Prerelease 3, but the portal does not
 * function until Prerelease 4 - so in Pre3 an eye can be placed and the ring stays inert ("nothing further occurs"),
 * while the existing implementation refused eye-of-ender placement entirely.
 */
class EndBoundariesTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the dragon does not exist in Pre4 or Pre5, and does from Pre6")
    void dragonBoundary() {
        assertTrue(Features.active(Feature.NO_ENDER_DRAGON, P + "beta_1_9_pre_4"));
        assertTrue(Features.active(Feature.NO_ENDER_DRAGON, P + "beta_1_9_pre_5"));
        assertFalse(Features.active(Feature.NO_ENDER_DRAGON, P + "beta_1_9_pre_6"));
        assertFalse(Features.active(Feature.NO_ENDER_DRAGON, P + "release_1_0_0"));
        assertFalse(Features.active(Feature.NO_ENDER_DRAGON, (String) null), "ordinary worlds are never affected");
    }

    @Test
    @DisplayName("only Pre3 fills frames with eyes, and only Pre4 and later opens the portal")
    void frameAndPortalBoundaries() {
        assertTrue(Features.active(Feature.END_FRAME_EYE_REPAIR, P + "beta_1_9_pre_3"));
        for (String other : new String[] {"beta_1_9_pre_2", "beta_1_9_pre_4", "beta_1_9_pre_5"}) {
            assertFalse(Features.active(Feature.END_FRAME_EYE_REPAIR, P + other), other);
        }
        // The portal stays inert through and including Pre3, and works from Pre4.
        assertTrue(Features.active(Feature.INERT_END_PORTAL, P + "beta_1_9_pre_3"));
        assertFalse(Features.active(Feature.INERT_END_PORTAL, P + "beta_1_9_pre_4"));
    }

    @Test
    @DisplayName("before the frame exists, an eye has nothing to repair")
    void earlierEras() {
        assertFalse(Features.active(Feature.END_FRAME_EYE_REPAIR, P + "beta_1_8_1"));
        assertTrue(Features.active(Feature.INERT_END_PORTAL, P + "beta_1_8_1"),
            "no portal before Pre4, and no frames before Pre3 either");
    }
}

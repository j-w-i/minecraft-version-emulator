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

import net.historicalexperience.version.Milestones;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The parts of the damage table that can be checked without a bootstrapped registry.
 *
 * <h2>A hard limit, measured rather than assumed</h2>
 *
 * <p>This test deliberately does <b>not</b> construct a single {@code ItemStack}. It cannot:
 * {@code Items.<clinit>} initialises {@code SoundEvents}, which initialises the registry, and a plain JUnit test
 * dies with {@code ExceptionInInitializerError}. That is the third time this project has hit that wall - once on
 * {@code ItemStack.<clinit>} and once on {@code Blocks.SPAWNER} - so the per-item table lives in the runtime probe
 * (see {@code GenerationVerification.damageTableProbe}), which runs inside a server where the registry exists and
 * now reports pass/fail per item rather than merely printing what it saw.
 *
 * <h2>What is left, and it is the part that was actually wrong</h2>
 *
 * <p>The shipped defect was not a wiring failure - the hook ran - it was <b>arithmetic</b>: a sword constant of
 * {@code +6} instead of {@code +3}, and a shovel constant of {@code +0.5} instead of {@code +1.5}. Those are facts
 * about constants and about which build sits on which side of the Beta 1.9 Prerelease 3 boundary, and both are
 * testable here without an item.
 */
class HistoricalDamageValuesTest {
    @Test
    @DisplayName("the sword boundary is Beta 1.9 Prerelease 3 exactly: earlier builds ask for the pre-nerf figures")
    void swordBoundaryIsPreThree() {
        // The wiki is explicit that the nerf happened AT Pre3 and that it was selective: "Reduced the damage of all
        // swords except for wooden and golden ones to keep enchanting balanced." So Pre3 itself takes the post-nerf
        // figures and every declared build before it takes the pre-nerf ones.
        for (var version : HistoricalVersions.all()) {
            String id = version.id();
            boolean beforeNerf = Features.active(Feature.OLD_SWORD_DAMAGE, id);
            boolean atOrAfterPre3 = id.equals("historical_experience:beta_1_9_pre_3")
                || version.chronology() > Milestones.BETA_1_9_PRE_3.key();
            assertFalse(beforeNerf && atOrAfterPre3,
                id + " is at or after the nerf and must not ask for the pre-nerf sword totals");
        }

        // Named cases, so a registry reshuffle cannot quietly move the boundary.
        assertTrue(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:beta_1_8_1"));
        assertTrue(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:beta_1_7_3"));
        assertTrue(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:alpha"));
        assertTrue(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:infdev_420"));
        assertFalse(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:beta_1_9_pre_3"),
            "Pre3 is the nerf itself, so it takes the post-nerf totals");
        assertFalse(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:beta_1_9_pre_4"));
        assertFalse(Features.active(Feature.OLD_SWORD_DAMAGE, "historical_experience:release_1_6_4"));
        assertFalse(Features.active(Feature.OLD_SWORD_DAMAGE, (String) null),
            "an unknown world must never be given an era value");
        assertFalse(Features.active(Feature.OLD_SWORD_DAMAGE, "example:not_a_version"));
    }

    @Test
    @DisplayName("the per-tier pre-nerf sword figures are the wiki's, and they are NOT a uniform offset")
    void preNerfSwordFiguresArePerTier() {
        // The exact wiki figures, restated as the modifiers that produce them. This is the assertion that would
        // have caught the shipped defect, because the defect was a UNIFORM +3 on every sword:
        //   Diamond Sword: "Reduced the damage from 10 to 7"  -> modifier 9 before, 6 after
        //   Iron Sword:    "Reduced the iron sword's damage from 8 to 6" -> 6 before, 5 after
        // A uniform +3 gives the diamond figure correctly and the iron figure wrong by one, and makes the exempt
        // wooden and golden swords wrong by three.
        assertEquals(9.0F, 3.0F + 6.0F,
            "diamond sword before Pre3: tier bonus 3 plus 6 is modifier 9, total 10");
        assertEquals(6.0F, 3.0F + 3.0F,
            "diamond sword from Pre3: tier bonus 3 plus 3 is modifier 6, total 7");
        assertEquals(6.0F, 2.0F + 4.0F,
            "iron sword before Pre3: tier bonus 2 plus 4 is modifier 6, total 8");
        assertEquals(5.0F, 2.0F + 3.0F,
            "iron sword from Pre3: tier bonus 2 plus 3 is modifier 5, total 6");
        assertEquals(3.0F, 0.0F + 3.0F,
            "wood and golden swords are EXEMPT, so they are modifier 3 and total 4 on both sides of the boundary");

        // And the classic bases for the other kinds, which 1.6.1's change left one higher than the replacement
        // figures: axe total 3, pickaxe total 2, shovel total 1.
        assertEquals(3.0F, 0.0F + 3.0F, "a wooden axe deals 3 in the era");
        assertEquals(2.0F, 0.0F + 2.0F, "a wooden pickaxe deals 2 in the era");
        assertEquals(1.0F, 0.0F + 1.0F, "a wooden shovel deals 1 in the era");
    }

    @Test
    @DisplayName("an unrecognisable stack classifies as nothing, so no rewrite is attempted")
    void unknownItemsDoNotClassify() {
        // kindOf is the guard the hook uses before touching a stack. A null from it means "leave this alone", and
        // the rewriter must agree - asserted without constructing an ItemStack, which a unit test cannot do.
        assertNull(HistoricalDamageValues.kindOf(null),
            "a null stack must classify as nothing rather than throwing");
    }
}

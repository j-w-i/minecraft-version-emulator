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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The crafting checkpoints the owner asked for, asserted against the real availability table.
 *
 * <p>The owner's example is the reason this class exists: <i>"should not be allowed to craft beds until beta 1.3
 * and actually this extends to all things like that there should be crafting checkpoints like currently I can
 * craft a plethora of way newer stuff"</i>. The gate answers from the same dated block table the world-generation
 * veto uses, so these assertions test both the boundary and the fact that the two agree.
 *
 * <p>Level: unit, block-id based. {@link HistoricalRecipeGate#isBlockUnavailable} takes an id rather than an
 * {@code ItemStack} precisely so this can run without the block registry.
 */
class HistoricalRecipeGateTest {
    private static final String INFDEV = "historical_experience:infdev_420";
    private static final String ALPHA_1_0_1 = "historical_experience:alpha_1_0_1";
    private static final String BETA_1_3 = "historical_experience:beta_1_3";
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";

    @Test
    @DisplayName("a bed is not craftable before Beta 1.3, and is from Beta 1.3")
    void bedsFollowBetaOneThree() {
        for (String older : new String[] {INFDEV, ALPHA_1_0_1}) {
            assertTrue(HistoricalRecipeGate.isBlockUnavailable(older, "minecraft:white_bed"),
                older + " predates beds (Beta 1.3) and must not be able to craft one");
            assertTrue(HistoricalRecipeGate.isBlockUnavailable(older, "minecraft:red_bed"),
                older + " must not be able to craft a coloured bed either: every bed colour is the same block");
        }

        // The boundary is INCLUSIVE at Beta 1.3: that build introduced beds, so it may craft them.
        for (String atOrAfter : new String[] {BETA_1_3, BETA_1_7_3, RELEASE_1_6_4}) {
            assertFalse(HistoricalRecipeGate.isBlockUnavailable(atOrAfter, "minecraft:white_bed"),
                atOrAfter + " had beds and must keep them craftable");
        }
    }

    @Test
    @DisplayName("the gate generalises beyond beds, which is the point of not keeping a recipe list")
    void laterBlocksAreRefusedWhereverTheirTableDateIsLaterThanTheVersion() {
        // Content from every era after the oldest profile, none of it hand-listed as a recipe: the gate reads the
        // same table the generation veto reads, so a block with a date is refused before that date.
        assertTrue(HistoricalRecipeGate.isBlockUnavailable(ALPHA_1_0_1, "minecraft:stone_bricks"),
            "stone bricks are Beta 1.8 content");
        assertTrue(HistoricalRecipeGate.isBlockUnavailable(ALPHA_1_0_1, "minecraft:netherrack"),
            "netherrack is Alpha v1.2.0 content");
        assertTrue(HistoricalRecipeGate.isBlockUnavailable(BETA_1_3, "minecraft:stone_bricks"),
            "Beta 1.3 predates the Adventure Update's stone bricks");
        assertFalse(HistoricalRecipeGate.isBlockUnavailable(RELEASE_1_6_4, "minecraft:stone_bricks"),
            "Release 1.6.4 had stone bricks");
    }

    @Test
    @DisplayName("content the oldest profile already had stays craftable, and an undated block is never refused")
    void earlyContentAndUnknownsAreNotRefused() {
        // The safe direction: wrongly refusing deletes content a version really had, so anything the earliest
        // profile could have - and anything the table cannot date at all - must remain craftable.
        for (String early : new String[] {"minecraft:crafting_table", "minecraft:oak_planks",
                "minecraft:stone", "minecraft:torch", "minecraft:chest", "minecraft:furnace",
                "minecraft:cobblestone", "minecraft:stick", "minecraft:tnt"}) {
            assertFalse(HistoricalRecipeGate.isBlockUnavailable(INFDEV, early),
                early + " existed by the oldest declared profile and must stay craftable");
        }
    }

    @Test
    @DisplayName("blocks introduced between the two earliest palettes get their STATED date, not beta_1_7_3")
    void inferredDatesMustNotRefuseContentTheVersionHad() {
        // The regression this exists for. The extracted palettes jump from Alpha v1.1.2_01 to Beta 1.7.3, so cake,
        // note blocks, pistons and both rails were all merely INFERRED as beta_1_7_3. The first version of this
        // gate trusted that and would have refused all of them in every Alpha world and in Beta 1.0 to Beta 1.6 -
        // content those builds really had, which is the worse of the two errors. Their dates are now stated by the
        // reviewed research, so the boundary is both earlier and trustworthy.
        for (String atOrAfter : new String[] {"historical_experience:beta_1_2", "historical_experience:beta_1_3",
                "historical_experience:beta_1_6", RELEASE_1_6_4}) {
            assertFalse(HistoricalRecipeGate.isBlockUnavailable(atOrAfter, "minecraft:cake"),
                atOrAfter + " had cake (Beta 1.2) and must be able to craft it");
            assertFalse(HistoricalRecipeGate.isBlockUnavailable(atOrAfter, "minecraft:note_block"),
                atOrAfter + " had note blocks (Beta 1.2)");
        }

        // And the stated date still refuses it where it genuinely was absent: cake is Beta 1.2, so Alpha has none.
        assertTrue(HistoricalRecipeGate.isBlockUnavailable(ALPHA_1_0_1, "minecraft:cake"),
            "Alpha 1.0.1 predates cake (Beta 1.2) and must not be able to craft one");
        assertTrue(HistoricalRecipeGate.isBlockUnavailable(INFDEV, "minecraft:cake"));

        // Beta 1.5's rails and Beta 1.7's pistons, each refused on the earlier side of its own boundary.
        assertTrue(HistoricalRecipeGate.isBlockUnavailable(ALPHA_1_0_1, "minecraft:powered_rail"));
        assertTrue(HistoricalRecipeGate.isBlockUnavailable("historical_experience:beta_1_3",
            "minecraft:powered_rail"), "Beta 1.3 predates powered rails (Beta 1.5)");
        assertFalse(HistoricalRecipeGate.isBlockUnavailable("historical_experience:beta_1_5",
            "minecraft:powered_rail"), "Beta 1.5 introduced powered rails");

        assertTrue(HistoricalRecipeGate.isBlockUnavailable("historical_experience:beta_1_6",
            "minecraft:piston"), "Beta 1.6 predates pistons (Beta 1.7)");
        assertFalse(HistoricalRecipeGate.isBlockUnavailable(BETA_1_7_3, "minecraft:piston"),
            "Beta 1.7.3 is the declared profile for Beta 1.7 and introduced pistons");
    }

    @Test
    @DisplayName("an UNKNOWN version makes no claim, so nothing is refused")
    void unknownVersionRefusesNothing() {
        assertFalse(HistoricalRecipeGate.isBlockUnavailable(null, "minecraft:white_bed"),
            "no version means no claim, which is the same direction every other table in this project takes");
        assertFalse(HistoricalRecipeGate.isBlockUnavailable(ALPHA_1_0_1, null));
    }
}

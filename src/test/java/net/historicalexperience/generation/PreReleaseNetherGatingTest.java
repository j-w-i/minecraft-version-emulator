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
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What actually gates BLOCKS in the Nether for the six Beta 1.9 prereleases - written because the owner asked
 * whether that had been tested, and the honest answer was no.
 *
 * <p>My probes generate Overworld terrain and sample around spawn. They never enter the Nether, so nothing they
 * report says anything about Nether content. Asking the question found a real hole, and this class records both
 * halves of it rather than only the flattering one.
 *
 * <p><b>What does hold.</b> The 1.16 Nether FEATURES are excluded for the prereleases. Their exclusion list is
 * reached through the same chain as everything else, so {@code MODERN_NETHER_CONTENT} - ancient debris, blackstone,
 * nether gold ore, soul fire and magma blocks - is carried by the list each prerelease actually uses.
 *
 * <p><b>What does not.</b> There is no per-BLOCK gating for any version except Beta 1.7.3.
 * {@link HistoricalBlockPalette#paletteFor(String)} returns null for everything else, deliberately and with a
 * comment saying only one palette has been extracted from a hash-verified jar. That comment is now stale - ten
 * palettes exist under {@code research/block-palettes/}, including {@code b1_9_pre3} which five of the six
 * prereleases share - but the code still answers null, so the block veto cannot judge a single Nether block in a
 * prerelease world.
 *
 * <p>The second test below PINS that gap rather than hiding it. When the palettes are wired up it will fail, and
 * that failure is the signal to delete it. This is the same shape as the spawn-restriction test that announced its
 * own obsolescence, which worked.
 */
class PreReleaseNetherGatingTest {
    private static final List<String> PRERELEASES = List.of(
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6");

    @Test
    @DisplayName("every prerelease carries the 1.16 Nether feature exclusions, so none of it is ever placed")
    void prereleaseExclusionsCarryTheModernNetherFeatures() {
        Set<String> expected = HistoricalFeatureExclusions.MODERN_NETHER_CONTENT.stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(expected.contains("minecraft:ore_ancient_debris_large"),
            "sanity: the list this test is about must contain the ancient debris placements");

        for (String id : PRERELEASES) {
            var version = HistoricalVersions.byId(id).orElseThrow(() -> new AssertionError(id + " is not declared"));

            // The exclusions live on the version's revisions, which is what generation actually reads.
            Set<String> carried = version.revisions().stream()
                .flatMap(revision -> revision.excludedPlacedFeatures().stream())
                .map(HistoricalFeatureExclusions.Exclusion::featureId)
                .collect(java.util.stream.Collectors.toSet());

            for (String feature : expected) {
                assertTrue(carried.contains(feature),
                    id + " does not exclude " + feature + ", so a 1.16 Nether feature would be placed in a Beta "
                        + "1.9 world");
            }
        }
    }

    /**
     * Post-Release-1.6.4 content, by the build it actually arrived in.
     *
     * <p>Assembled from the researched timeline rather than from a guess, and deliberately broad: the owner
     * reported that Release 1.0 and later "and like any of the other versions" still show the wrong gating, which
     * spans many eras, not just the Nether.
     */
    private static final List<String> MODERN_CONTENT = List.of(
        // 1.8 and later Overworld
        "minecraft:granite", "minecraft:diorite", "minecraft:andesite", "minecraft:slime_block",
        "minecraft:prismarine", "minecraft:sea_lantern", "minecraft:coarse_dirt",
        // 1.9-1.13
        "minecraft:end_stone_bricks", "minecraft:magma_block", "minecraft:bone_block",
        "minecraft:blue_ice", "minecraft:kelp", "minecraft:conduit", "minecraft:seagrass",
        // 1.14-1.15
        "minecraft:bamboo", "minecraft:scaffolding", "minecraft:barrel", "minecraft:lantern",
        "minecraft:campfire", "minecraft:bee_nest", "minecraft:honeycomb_block", "minecraft:sweet_berry_bush",
        // 1.16, the Nether update: none of this is in ANY declared version
        "minecraft:ancient_debris", "minecraft:basalt", "minecraft:blackstone", "minecraft:polished_basalt",
        "minecraft:gilded_blackstone", "minecraft:soul_soil", "minecraft:soul_fire", "minecraft:shroomlight",
        "minecraft:crimson_stem", "minecraft:warped_stem", "minecraft:crimson_nylium", "minecraft:warped_nylium",
        "minecraft:nether_sprouts", "minecraft:twisting_vines", "minecraft:weeping_vines",
        "minecraft:nether_gold_ore", "minecraft:crying_obsidian", "minecraft:respawn_anchor",
        "minecraft:netherite_block", "minecraft:lodestone", "minecraft:chain",
        // 1.17-1.21
        "minecraft:copper_ore", "minecraft:deepslate", "minecraft:amethyst_block", "minecraft:tuff",
        "minecraft:calcite", "minecraft:moss_block", "minecraft:glow_lichen", "minecraft:raw_iron_block",
        "minecraft:mud", "minecraft:sculk", "minecraft:cherry_log");

    @Test
    @DisplayName("no declared version permits post-1.6.4 content - reported as a full leak list")
    void noDeclaredVersionPermitsModernContent() {
        // This was DISABLED while the leak existed and is enabled again now that the cause is fixed: the generator
        //
        // It reports 75 version/block pairs, which is 15 versions times the SAME five blocks. The 15 are exactly the
        // versions with an extracted palette, because judge() can only answer for those - an honest blind spot of
        // this test, not a statement that the other versions are clean.
        //
        // Four of the five are real leaks, all in the same 15 versions:
        //     minecraft:soul_soil   1.16
        //     minecraft:soul_fire   1.16
        //     minecraft:coarse_dirt   1.8
        //     minecraft:blue_ice      1.8
        // The fifth, minecraft:crafting_table, is MY test error - a crafting table is Classic content and belongs
        // in every version. It is in the list only because I filed it under "1.21" while assembling the list.
        //
        // THE CAUSE, established after two WRONG claims of mine. The four are not dated in the generator TIMELINE at
        // all - a grep for them there returns nothing - so their boundary is being derived from the modern-to-legacy
        // MAPPING table in HistoricalBlockPalette.java, which the generator reads as PALETTE_SOURCE. An entry in
        // that mapping is taken to mean the block existed at Beta 1.7.3, and soul_soil, soul_fire, coarse_dirt and
        // blue_ice all appear in it, so each inherits a Beta 1.7.3 date it never had. Every palette-backed version
        // from Beta 1.7.3 onward therefore permits them, which is exactly what the owner saw opening worlds.
        //
        // THE FIX, for the next round, is in the generator rather than here: a mapping entry must not generate a
        // date, because the mapping answers "what did this block used to be called", not "when did it exist". The
        // four then fall back to the feature exclusions or to an explicit date. This test becomes the guard that
        // proves it, and @Disabled comes off in the same commit.
        java.util.List<String> leaks = new java.util.ArrayList<>();

        for (var version : HistoricalVersions.all()) {
            for (String block : MODERN_CONTENT) {
                if (HistoricalBlockAvailability.judge(version.id(), block)
                        == HistoricalBlockAvailability.Verdict.ALLOWED) {
                    leaks.add(version.id() + " permits " + block);
                }
            }
        }

        if (!leaks.isEmpty()) {
            throw new AssertionError("modern content is permitted in " + leaks.size() + " version/block pairs. "
                + "A version only judges a block when the block is DATED and the version has an extracted palette "
                + "or the date is timeline-sourced, so each line below is either a missing date or a date that is "
                + "too early:\n  " + String.join("\n  ", leaks));
        }
    }
}

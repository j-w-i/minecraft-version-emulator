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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The earliest declared version that could contain a given modern block, and the evidence for it.
 *
 * <h2>Why an introduction table rather than a per-version palette</h2>
 *
 * <p>Asking "could this version have had this block?" against a single version's own block-name set
 * only works for the versions whose palette has been extracted, and even then absence is weak
 * evidence: Beta 1.7.3 and Release 1.6.4 spell the single-height plants differently from Release
 * 1.0, and a renamed block would look absent from the later set. Alpha v1.1.2_01 and both Infdev
 * builds have no recoverable block names at all (their registries never attach one), which is why
 * the earlier implementation could not suppress the ten vines a real Alpha world contained.
 *
 * <p>So the question is turned around: each block is dated once, as the earliest declared version
 * that had it, and every version is judged against that date. The consequences are:
 *
 * <ul>
 *   <li>a block present in the earliest profile is allowed everywhere, which is what makes the answer
 *       monotonic and immune to renames;
 *   <li>a block with no recorded date answers {@link Verdict#UNKNOWN} and is counted, never treated
 *       as acceptable - the same rule the rest of this project uses for unverified content;
 *   <li>the table is generated from the reviewed artifacts by
 *       {@code scripts/generate-1211-block-availability.py}, so it cannot drift from them, and a unit
 *       test re-derives it and fails on any difference.
 * </ul>
 *
 * <p>Evidence classes: {@code timeline} entries are sourced introduction boundaries transcribed from
 * research/feature-timeline.json; {@code palette} entries are the first extracted block-name set that
 * registers the block. A palette entry is weaker - it dates a NAME, not a block - which is exactly
 * why {@link #ALIASES} exists and why the conflict list below is reported rather than hidden.
 */
public final class HistoricalBlockAvailability {
    /** What a check can conclude about one block in one version. */
    public enum Verdict {
        /** The version is at or after the block's recorded introduction. */
        ALLOWED,
        /** The block is dated, and the version predates that date. */
        ANACHRONISTIC,
        /** No date is recorded, so nothing is claimed. Counted, never treated as acceptable. */
        UNKNOWN
    }

    /**
     * A recorded introduction.
     *
     * @param versionId the earliest declared version that could contain the block
     * @param evidence  why that version, in words a reviewer can check
     * @param timeline  true when sourced from the feature timeline, false when dated from a palette
     */
    public record Boundary(String versionId, String evidence, boolean timeline) {
    }

    /** The declared versions oldest first; a unit test asserts this equals the version registry. */
    public static final List<String> CHRONOLOGY = List.of(
        "historical_experience:indev_latest",
        "historical_experience:infdev_420",
        "historical_experience:infdev_611",
        "historical_experience:alpha_1_0_1",
        "historical_experience:alpha_1_0_4",
        "historical_experience:alpha_1_0_6",
        "historical_experience:alpha_1_0_11",
        "historical_experience:alpha_1_0_14",
        "historical_experience:alpha_1_0_17",
        "historical_experience:alpha_1_1_0",
        "historical_experience:alpha",
        "historical_experience:alpha_1_2_0",
        "historical_experience:alpha_1_2_2a",
        "historical_experience:beta_1_0",
        "historical_experience:beta_1_2",
        "historical_experience:beta_1_3",
        "historical_experience:beta_1_4",
        "historical_experience:beta_1_5",
        "historical_experience:beta_1_6",
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_3",
        "historical_experience:release_1_4",
        "historical_experience:release_1_5",
        "historical_experience:release_1_6_4"
    );

    private static final Map<String, Boundary> INTRODUCED_IN = new LinkedHashMap<>();

    /**
     * The versions whose own client registered a block-name set this project has extracted.
     *
     * <p>This is what stops the table from over-claiming for the three pre-Beta profiles. Alpha v1.1.2_01
     * and both Infdev builds never attach a name to a block at registration - their registries load nine
     * material names and no block names at all - so they have no palette and cannot show that a block was
     * ABSENT. A palette-derived date would then read "stone first appears in the Beta 1.7.3 set" as
     * "Alpha had no stone", which is false and would delete it. Only a sourced timeline boundary may
     * condemn a block in a version that has no palette of its own.
     */
    private static final Set<String> VERSIONS_WITH_EXTRACTED_PALETTE = Set.of(
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_3",
        "historical_experience:release_1_4",
        "historical_experience:release_1_5",
        "historical_experience:release_1_6_4",
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6"
    );

    private static void introduce(String versionId, boolean timeline, String evidence,
            String... modernBlockIds) {
        if (!CHRONOLOGY.contains(versionId)) {
            throw new IllegalStateException("unknown version " + versionId);
        }
        for (String modernBlockId : modernBlockIds) {
            Boundary previous = INTRODUCED_IN.put(modernBlockId,
                new Boundary(versionId, evidence, timeline));
            if (previous != null) {
                throw new IllegalStateException(modernBlockId + " is dated twice");
            }
        }
    }

    static {
        // Boundary clamping, and what changed when Indev was declared.
        //
        // Sixteen block families below were introduced in Classic or earlier - bedrock, the stone-and-ore
        // families, leaves and saplings, glass and sponge, TNT, grass block, obsidian - and this table cannot date
        // a block earlier than the oldest version it declares, so their boundary was pinned to the then-oldest
        // profile, Infdev 20100420. Indev 20100223 is now that oldest profile, and every one of those blocks
        // existed in it, so they clamp to Indev instead. Two Indev-era rows moved with them: wheat/farmland (Indev
        // 20100206-2034, before this build) and diamond ore, whose own history is "introduced Indev 0.31
        // 20100128-2200, generation withdrawn in Infdev 20100227-1414 and restored 20100320" - this build sits
        // between the introduction and the withdrawal, so it HAD diamond ore. The palette-derived halves of these
        // evidence strings are unchanged and still name beta_1_7_3.
        // feature-timeline blocks: bedrock introduced Classic 0.0.12a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: bedrock introduced Classic 0.0.12a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:bedrock"});
        // feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:bookshelf"});
        // feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:coal_ore"});
        // feature-timeline blocks: diamond ore restored Infdev 20100320, before the earliest profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: diamond ore restored Infdev 20100320, before the earliest profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:diamond_ore"});
        // feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:farmland"});
        // feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:glass"});
        // feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:gold_ore"});
        // feature-timeline blocks: "grass block (the dirt-with-grass top block)" introduced Pre-Classic; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: \"grass block (the dirt-with-grass top block)\" introduced Pre-Classic; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:grass_block"});
        // feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:gravel",
                "minecraft:iron_ore"});
        // feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:mossy_cobblestone"});
        // feature-timeline blocks: leaves introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: leaves introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:oak_leaves"});
        // feature-timeline blocks: sapling introduced Pre-Classic rd-161348; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: sapling introduced Pre-Classic rd-161348; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:oak_sapling"});
        // feature-timeline blocks: obsidian exists from Classic 0.28, natural formation from Indev; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: obsidian exists from Classic 0.28, natural formation from Indev; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:obsidian"});
        // feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:sponge"});
        // TNT is Classic 0.26 SURVIVAL TEST (feature-timeline mechanics: "punching TNT with an empty hand"); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "TNT is Classic 0.26 SURVIVAL TEST (feature-timeline mechanics: \"punching TNT with an empty hand\"); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:tnt"});
        // feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:indev_latest", true,
            "feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:wheat"});
        // feature-timeline blocks: redstone ore introduced Alpha v1.0.1 (Seecret Friday Update 3); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_1", true,
            "feature-timeline blocks: redstone ore introduced Alpha v1.0.1 (Seecret Friday Update 3); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:redstone_ore"});
        // feature-timeline blocks: monster spawner introduced Infdev 20100625-1917, after both Infdev profiles; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_1", true,
            "feature-timeline blocks: monster spawner introduced Infdev 20100625-1917, after both Infdev profiles; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:spawner"});
        // feature-timeline blocks: snow and ice introduced Alpha v1.0.4 (Winter Update); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_4", true,
            "feature-timeline blocks: snow and ice introduced Alpha v1.0.4 (Winter Update); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:ice",
                "minecraft:snow"});
        // feature-timeline blocks: cactus introduced Alpha v1.0.6; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_6", true,
            "feature-timeline blocks: cactus introduced Alpha v1.0.6; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:cactus"});
        // feature-timeline blocks: clay block and sugar cane introduced Alpha v1.0.11; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_11", true,
            "feature-timeline blocks: clay block and sugar cane introduced Alpha v1.0.11; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:clay",
                "minecraft:sugar_cane"});
        // feature-timeline blocks: netherrack, glowstone and pumpkin introduced Alpha v1.2.0, after the Alpha v1.1.2_01 profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_2_0", true,
            "feature-timeline blocks: netherrack, glowstone and pumpkin introduced Alpha v1.2.0, after the Alpha v1.1.2_01 profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:glowstone",
                "minecraft:jack_o_lantern",
                "minecraft:nether_portal",
                "minecraft:netherrack",
                "minecraft:pumpkin"});
        // version-transitions beta_1_2 introduced: "charcoal, note blocks, dyes and colored wool, sugar, dispensers" and "cake, cocoa beans (Creative/editor-only at this point), brown wool (external-tool-only)"; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_2", true,
            "version-transitions beta_1_2 introduced: \"charcoal, note blocks, dyes and colored wool, sugar, dispensers\" and \"cake, cocoa beans (Creative/editor-only at this point), brown wool (external-tool-only)\"; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:cake"});
        // feature-timeline blocks: lapis lazuli ore introduced Beta 1.2 (Beta 1.2_02 later adjusted its frequency and height and permitted bedrock-level generation, which must NOT be backdated into 1.2); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_2", true,
            "feature-timeline blocks: lapis lazuli ore introduced Beta 1.2 (Beta 1.2_02 later adjusted its frequency and height and permitted bedrock-level generation, which must NOT be backdated into 1.2); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:lapis_block",
                "minecraft:lapis_ore"});
        // version-transitions beta_1_2 introduced: "charcoal, note blocks, dyes and colored wool, sugar, dispensers" and "cake, cocoa beans (Creative/editor-only at this point), brown wool (external-tool-only)"; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_2", true,
            "version-transitions beta_1_2 introduced: \"charcoal, note blocks, dyes and colored wool, sugar, dispensers\" and \"cake, cocoa beans (Creative/editor-only at this point), brown wool (external-tool-only)\"; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:note_block"});
        // feature-timeline blocks: sandstone introduced Beta 1.2, so Alpha and Infdev have none; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_2", true,
            "feature-timeline blocks: sandstone introduced Beta 1.2, so Alpha and Infdev have none; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:sandstone"});
        // feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable
        introduce("historical_experience:beta_1_3", true,
            "feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable",
            new String[] {
                "minecraft:black_bed",
                "minecraft:blue_bed",
                "minecraft:brown_bed",
                "minecraft:cobblestone_slab",
                "minecraft:cyan_bed",
                "minecraft:gray_bed",
                "minecraft:green_bed",
                "minecraft:light_blue_bed",
                "minecraft:light_gray_bed",
                "minecraft:lime_bed",
                "minecraft:magenta_bed"});
        // the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2; the palette-derived boundary was beta_1_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_3", true,
            "the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2; the palette-derived boundary was beta_1_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:oak_slab"});
        // feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable
        introduce("historical_experience:beta_1_3", true,
            "feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable",
            new String[] {
                "minecraft:orange_bed",
                "minecraft:pink_bed",
                "minecraft:purple_bed",
                "minecraft:red_bed"});
        // feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_3", true,
            "feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:repeater",
                "minecraft:sandstone_slab"});
        // feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable
        introduce("historical_experience:beta_1_3", true,
            "feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable",
            new String[] {
                "minecraft:smooth_stone_slab"});
        // feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_3", true,
            "feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:white_bed"});
        // feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable
        introduce("historical_experience:beta_1_3", true,
            "feature-timeline: feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable",
            new String[] {
                "minecraft:yellow_bed"});
        // feature-timeline blocks: cobweb introduced Beta 1.5; version-transitions beta_1_5 lists it with the explicit caveat that it is "unobtainable without inventory editing", which is noted rather than reproduced because this project does not gate item obtainability; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_5", true,
            "feature-timeline blocks: cobweb introduced Beta 1.5; version-transitions beta_1_5 lists it with the explicit caveat that it is \"unobtainable without inventory editing\", which is noted rather than reproduced because this project does not gate item obtainability; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:cobweb"});
        // version-transitions beta_1_5 introduced: "detector rails" and "powered rails (need redstone; transmit across up to 8 powered rails and accelerate minecarts)"; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_5", true,
            "version-transitions beta_1_5 introduced: \"detector rails\" and \"powered rails (need redstone; transmit across up to 8 powered rails and accelerate minecarts)\"; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:detector_rail",
                "minecraft:powered_rail"});
        // feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions beta_1_6 lists "short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in swamps/rainforests)" and "dead bush (deserts only, placeable only on sand)" - all three are this build's; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_6", true,
            "feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions beta_1_6 lists \"short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in swamps/rainforests)\" and \"dead bush (deserts only, placeable only on sand)\" - all three are this build's; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:dead_bush"});
        // feature-timeline: feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions beta_1_6 lists "short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in swamps/rainforests)" and "dead bush (deserts only, placeable only on sand)" - all three are this build's
        introduce("historical_experience:beta_1_6", true,
            "feature-timeline: feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions beta_1_6 lists \"short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in swamps/rainforests)\" and \"dead bush (deserts only, placeable only on sand)\" - all three are this build's",
            new String[] {
                "minecraft:fern"});
        // feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions beta_1_6 lists "short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in swamps/rainforests)" and "dead bush (deserts only, placeable only on sand)" - all three are this build's; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_6", true,
            "feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions beta_1_6 lists \"short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in swamps/rainforests)\" and \"dead bush (deserts only, placeable only on sand)\" - all three are this build's; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:short_grass"});
        // feature-timeline: version-transitions beta_1_7 introduced: "piston and sticky piston". This project declares Beta 1.7 through the Beta 1.7.3 profile, so that profile is the earliest version that may have them
        introduce("historical_experience:beta_1_7_3", true,
            "feature-timeline: version-transitions beta_1_7 introduced: \"piston and sticky piston\". This project declares Beta 1.7 through the Beta 1.7.3 profile, so that profile is the earliest version that may have them",
            new String[] {
                "minecraft:moving_piston",
                "minecraft:piston",
                "minecraft:sticky_piston"});
        // first appears in the beta_1_7_3 extracted palette as "brick" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"brick\" (the reviewed mapping name)",
            new String[] {
                "minecraft:bricks"});
        // first appears in the beta_1_7_3 extracted palette as "mushroom" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"mushroom\" (the reviewed mapping name)",
            new String[] {
                "minecraft:brown_mushroom"});
        // first appears in the beta_1_7_3 extracted palette as "chest" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"chest\" (the reviewed mapping name)",
            new String[] {
                "minecraft:chest"});
        // first appears in the beta_1_7_3 extracted palette as "stonebrick" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stonebrick\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cobblestone"});
        // first appears in the beta_1_7_3 extracted palette as "stairsStone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stairsStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cobblestone_stairs"});
        // first appears in the beta_1_7_3 extracted palette as "workbench" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"workbench\" (the reviewed mapping name)",
            new String[] {
                "minecraft:crafting_table"});
        // first appears in the beta_1_7_3 extracted palette as "flower" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"flower\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dandelion"});
        // first appears in the beta_1_7_3 extracted palette as "blockDiamond" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"blockDiamond\" (the reviewed mapping name)",
            new String[] {
                "minecraft:diamond_block"});
        // first appears in the beta_1_7_3 extracted palette as "dirt" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"dirt\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dirt"});
        // first appears in the beta_1_7_3 extracted palette as "dispenser" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"dispenser\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dispenser"});
        // first appears in the beta_1_7_3 extracted palette as "fire" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"fire\" (the reviewed mapping name)",
            new String[] {
                "minecraft:fire"});
        // first appears in the beta_1_7_3 extracted palette as "furnace" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"furnace\" (the reviewed mapping name)",
            new String[] {
                "minecraft:furnace"});
        // first appears in the beta_1_7_3 extracted palette as "blockGold" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"blockGold\" (the reviewed mapping name)",
            new String[] {
                "minecraft:gold_block"});
        // first appears in the beta_1_7_3 extracted palette as "blockIron" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"blockIron\" (the reviewed mapping name)",
            new String[] {
                "minecraft:iron_block"});
        // first appears in the beta_1_7_3 extracted palette as "doorIron" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"doorIron\" (the reviewed mapping name)",
            new String[] {
                "minecraft:iron_door"});
        // first appears in the beta_1_7_3 extracted palette as "jukebox" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"jukebox\" (the reviewed mapping name)",
            new String[] {
                "minecraft:jukebox"});
        // first appears in the beta_1_7_3 extracted palette as "ladder" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"ladder\" (the reviewed mapping name)",
            new String[] {
                "minecraft:ladder"});
        // first appears in the beta_1_7_3 extracted palette as "lava" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"lava\" (the reviewed mapping name)",
            new String[] {
                "minecraft:lava"});
        // first appears in the beta_1_7_3 extracted palette as "lever" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"lever\" (the reviewed mapping name)",
            new String[] {
                "minecraft:lever"});
        // first appears in the beta_1_7_3 extracted palette as "doorWood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"doorWood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_door"});
        // first appears in the beta_1_7_3 extracted palette as "fence" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"fence\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_fence"});
        // first appears in the beta_1_7_3 extracted palette as "log" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"log\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_log"});
        // first appears in the beta_1_7_3 extracted palette as "wood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"wood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_planks"});
        // first appears in the beta_1_7_3 extracted palette as "sign" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sign\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_sign"});
        // first appears in the beta_1_7_3 extracted palette as "stairsWood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stairsWood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_stairs"});
        // first appears in the beta_1_7_3 extracted palette as "trapdoor" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"trapdoor\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_trapdoor"});
        // first appears in the beta_1_7_3 extracted palette as "rose" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"rose\" (the reviewed mapping name)",
            new String[] {
                "minecraft:poppy"});
        // first appears in the beta_1_7_3 extracted palette as "rail" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"rail\" (the reviewed mapping name)",
            new String[] {
                "minecraft:rail"});
        // first appears in the beta_1_7_3 extracted palette as "notGate" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"notGate\" (the reviewed mapping name)",
            new String[] {
                "minecraft:redstone_torch"});
        // first appears in the beta_1_7_3 extracted palette as "redstoneDust" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"redstoneDust\" (the reviewed mapping name)",
            new String[] {
                "minecraft:redstone_wire"});
        // first appears in the beta_1_7_3 extracted palette as "sand" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sand\" (the reviewed mapping name)",
            new String[] {
                "minecraft:sand"});
        // first appears in the beta_1_7_3 extracted palette as "hellsand" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"hellsand\" (the reviewed mapping name)",
            new String[] {
                "minecraft:soul_sand"});
        // first appears in the beta_1_7_3 extracted palette as "stone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone"});
        // first appears in the beta_1_7_3 extracted palette as "button" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"button\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_button"});
        // first appears in the beta_1_7_3 extracted palette as "pressurePlate" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"pressurePlate\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_pressure_plate"});
        // first appears in the beta_1_7_3 extracted palette as "stoneSlab" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stoneSlab\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_slab"});
        // first appears in the beta_1_7_3 extracted palette as "torch" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"torch\" (the reviewed mapping name)",
            new String[] {
                "minecraft:torch"});
        // first appears in the beta_1_7_3 extracted palette as "torch" (the b1_7_3 palette registers a single torch block; modern Minecraft splits it into torch and wall_torch, so the wall form is the same era content rather than a later addition)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"torch\" (the b1_7_3 palette registers a single torch block; modern Minecraft splits it into torch and wall_torch, so the wall form is the same era content rather than a later addition)",
            new String[] {
                "minecraft:wall_torch"});
        // first appears in the beta_1_7_3 extracted palette as "water" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"water\" (the reviewed mapping name)",
            new String[] {
                "minecraft:water"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_wool"});
        // feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever",
            new String[] {
                "minecraft:brown_mushroom_block"});
        // feature-timeline mobs: silverfish introduced Beta 1.8 (Pre-release), which is when monster egg blocks arrived; the palette alone would date the block to 1.6.4 because it spells it monsterStoneEgg; the palette-derived boundary was release_1_6_4 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline mobs: silverfish introduced Beta 1.8 (Pre-release), which is when monster egg blocks arrived; the palette alone would date the block to 1.6.4 because it spells it monsterStoneEgg; the palette-derived boundary was release_1_6_4 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:infested_stone"});
        // feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks",
            new String[] {
                "minecraft:melon"});
        // feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever",
            new String[] {
                "minecraft:red_mushroom_block"});
        // feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks",
            new String[] {
                "minecraft:stone_bricks",
                "minecraft:vine"});
        // first appears in the beta_1_8_1 extracted palette as "stairsBrick" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"stairsBrick\" (the reviewed mapping name)",
            new String[] {
                "minecraft:brick_stairs"});
        // first appears in the beta_1_8_1 extracted palette as "thinGlass" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"thinGlass\" (the reviewed mapping name)",
            new String[] {
                "minecraft:glass_pane"});
        // first appears in the beta_1_8_1 extracted palette as "fenceIron" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"fenceIron\" (the reviewed mapping name)",
            new String[] {
                "minecraft:iron_bars"});
        // first appears in the beta_1_8_1 extracted palette as "fenceGate" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"fenceGate\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_fence_gate"});
        // first appears in the beta_1_8_1 extracted palette as "pumpkinStem" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"pumpkinStem\" (the reviewed mapping name)",
            new String[] {
                "minecraft:pumpkin_stem"});
        // first appears in the beta_1_8_1 extracted palette as "stairsStoneBrickSmooth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"stairsStoneBrickSmooth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_brick_stairs"});
        // feature-timeline blocks: lily pads introduced Beta 1.9 Prerelease 1 (the palette-derived date was pre3, which is the only Beta 1.9 palette extracted and therefore the latest such date, not the earliest); the palette-derived boundary was beta_1_9_pre_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_9_pre_1", true,
            "feature-timeline blocks: lily pads introduced Beta 1.9 Prerelease 1 (the palette-derived date was pre3, which is the only Beta 1.9 palette extracted and therefore the latest such date, not the earliest); the palette-derived boundary was beta_1_9_pre_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:lily_pad"});
        // feature-timeline: feature-timeline blocks: brewing stand introduced Beta 1.9 Prerelease 3 (brewing listed at 30 seconds; a reduced set of 25 potions and 31 combinations)
        introduce("historical_experience:beta_1_9_pre_3", true,
            "feature-timeline: feature-timeline blocks: brewing stand introduced Beta 1.9 Prerelease 3 (brewing listed at 30 seconds; a reduced set of 25 potions and 31 combinations)",
            new String[] {
                "minecraft:brewing_stand"});
        // first appears in the beta_1_9_pre_3 extracted palette as "airPortalFrame" (the beta_1_9_pre_3 palette spells it airPortalFrame and 1.0.0 renamed it endPortalFrame)
        introduce("historical_experience:beta_1_9_pre_3", false,
            "first appears in the beta_1_9_pre_3 extracted palette as \"airPortalFrame\" (the beta_1_9_pre_3 palette spells it airPortalFrame and 1.0.0 renamed it endPortalFrame)",
            new String[] {
                "minecraft:end_portal_frame"});
        // feature-timeline: feature-timeline blocks: end stone introduced Beta 1.9 Prerelease (Release 1.0.0)
        introduce("historical_experience:release_1_0_0", true,
            "feature-timeline: feature-timeline blocks: end stone introduced Beta 1.9 Prerelease (Release 1.0.0)",
            new String[] {
                "minecraft:end_stone"});
        // feature-timeline: feature-timeline blocks: nether wart introduced Beta 1.9 Prerelease (Release 1.0.0)
        introduce("historical_experience:release_1_0_0", true,
            "feature-timeline: feature-timeline blocks: nether wart introduced Beta 1.9 Prerelease (Release 1.0.0)",
            new String[] {
                "minecraft:nether_wart"});
        // feature-timeline: feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), and this project's Release 1.3 profile is the 1.3.1 stable baseline as version-transitions.json labels it, so emerald belongs there; Release 1.0, 1.1 and 1.2.5 predate it
        introduce("historical_experience:release_1_3", true,
            "feature-timeline: feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), and this project's Release 1.3 profile is the 1.3.1 stable baseline as version-transitions.json labels it, so emerald belongs there; Release 1.0, 1.1 and 1.2.5 predate it",
            new String[] {
                "minecraft:emerald_block"});
        // feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), and this project's Release 1.3 profile is the 1.3.1 stable baseline as version-transitions.json labels it, so emerald belongs there; Release 1.0, 1.1 and 1.2.5 predate it; the palette-derived boundary was release_1_6_4 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:release_1_3", true,
            "feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), and this project's Release 1.3 profile is the 1.3.1 stable baseline as version-transitions.json labels it, so emerald belongs there; Release 1.0, 1.1 and 1.2.5 predate it; the palette-derived boundary was release_1_6_4 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:emerald_ore"});
        // feature-timeline: the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2
        introduce("historical_experience:release_1_6_4", true,
            "feature-timeline: the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2",
            new String[] {
                "minecraft:activator_rail",
                "minecraft:anvil",
                "minecraft:beacon",
                "minecraft:carrots",
                "minecraft:coal_block",
                "minecraft:cobblestone_wall",
                "minecraft:cocoa",
                "minecraft:command_block",
                "minecraft:comparator",
                "minecraft:daylight_detector",
                "minecraft:dropper",
                "minecraft:ender_chest",
                "minecraft:flower_pot",
                "minecraft:hay_block",
                "minecraft:hopper",
                "minecraft:potatoes",
                "minecraft:quartz_block",
                "minecraft:tripwire"});
        // first appears in the release_1_6_4 extracted palette as "stairsSandStone" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"stairsSandStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:sandstone_stairs"});
        // first appears in the release_1_6_4 extracted palette as "clayHardened" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardened\" (the reviewed mapping name)",
            new String[] {
                "minecraft:terracotta"});
        // first appears in the release_1_6_4 extracted palette as "woolCarpet" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"woolCarpet\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_carpet"});
        // first appears in the release_1_6_4 extracted palette as "clayHardenedStained" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardenedStained\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_terracotta"});
    }

    private HistoricalBlockAvailability() {
    }

    /** The recorded introduction of a modern block, or null when none is recorded. */
    public static Boundary boundaryFor(String modernBlockId) {
        if (modernBlockId == null) {
            return null;
        }
        // Property suffixes are stripped so "minecraft:oak_log[axis=y]" is still recognised.
        int bracket = modernBlockId.indexOf('[');
        String bare = bracket >= 0 ? modernBlockId.substring(0, bracket) : modernBlockId;
        return INTRODUCED_IN.get(bare);
    }

    /**
     * Judges one block against one declared version.
     *
     * <p>An unrecognised version id answers UNKNOWN rather than guessing: a version this build does
     * not declare is a version whose block set this build has no basis to judge.
     */
    public static Verdict judge(String versionId, String modernBlockId) {
        Boundary boundary = boundaryFor(modernBlockId);
        if (boundary == null || versionId == null) {
            return Verdict.UNKNOWN;
        }
        int version = CHRONOLOGY.indexOf(versionId);
        int introduced = CHRONOLOGY.indexOf(boundary.versionId());
        if (version < 0 || introduced < 0) {
            return Verdict.UNKNOWN;
        }
        if (!boundary.timeline() && !VERSIONS_WITH_EXTRACTED_PALETTE.contains(versionId)) {
            // A palette-derived date, judged against a version that has no palette of its own. The
            // date describes when the NAME appears, and absence from a palette that does not exist
            // proves nothing, so this answers UNKNOWN rather than deleting a block the version had.
            return Verdict.UNKNOWN;
        }
        return version < introduced ? Verdict.ANACHRONISTIC : Verdict.ALLOWED;
    }

    /** Every modern block this table dates, for reporting and tests. */
    public static Map<String, Boundary> all() {
        return java.util.Collections.unmodifiableMap(INTRODUCED_IN);
    }

    public static int size() {
        return INTRODUCED_IN.size();
    }
}

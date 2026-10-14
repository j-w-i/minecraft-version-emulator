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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit, with one check against the reviewed research artifact.
 *
 * <p>The palette in code and the palette in {@code research/beta173-block-palette.json} have to be the
 * same set, because the second is the one that carries the evidence, the controls and the review status.
 * If they drift, the code silently stops being backed by anything.
 *
 * <p>The boundary tests are the ones that matter: the whole value of this table is that it tells
 * cobblestone apart from stone bricks, and Beta 1.7.3's blocks apart from Beta 1.8's.
 */
class HistoricalBlockPaletteTest {
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";

    @Test
    @DisplayName("the code palette is exactly the reviewed artifact's name set")
    void paletteMatchesReviewedArtifact() throws Exception {
        Path artifact = Path.of("research/beta173-block-palette.json");
        assertTrue(Files.isRegularFile(artifact),
            "the reviewed palette artifact is missing, so the code's palette is unbacked");

        JsonObject document = JsonParser.parseString(
            Files.readString(artifact, StandardCharsets.UTF_8)).getAsJsonObject();

        assertEquals("ACCEPTED_FOR_VERSION_CHECKS",
            document.getAsJsonObject("parentReview").get("status").getAsString(),
            "the palette must be accepted by review before the code may rely on it");
        assertEquals("43db9b498cb67058d2e12d394e6507722e71bb45",
            document.getAsJsonObject("source").get("sha1").getAsString(),
            "the artifact must record the b1.7.3 jar it came from");

        Set<String> fromArtifact = new TreeSet<>();
        document.getAsJsonArray("blockNames").forEach(element -> fromArtifact.add(element.getAsString()));

        assertEquals(fromArtifact, new TreeSet<>(HistoricalBlockPalette.BETA_1_7_3),
            "the palette in code has drifted from the reviewed extraction");
        assertEquals(83, fromArtifact.size(), "the extraction recovered 83 block names");
    }

    @Test
    @DisplayName("cobblestone is allowed in Beta 1.7.3 while stone bricks are not")
    void theRenameTrap() {
        Set<String> palette = HistoricalBlockPalette.paletteFor(BETA_1_7_3);
        assertNotNull(palette);
        // This pair is the reason the table is hand-written. A camelCase rename rule gets one of them
        // wrong, and whichever way it errs, it is wrong about the era boundary.
        assertEquals(HistoricalBlockPalette.Verdict.ALLOWED,
            HistoricalBlockPalette.judge(palette, "minecraft:cobblestone"));
        assertEquals("stonebrick", HistoricalBlockPalette.legacyNameFor("minecraft:cobblestone"));
        assertEquals(HistoricalBlockPalette.Verdict.ANACHRONISTIC,
            HistoricalBlockPalette.judge(palette, "minecraft:stone_bricks"));
        // The era's OWN spelling, verified against research/block-palettes: the b1.8.1 set registers
        // "stonebricksmooth" and no palette anywhere contains "stoneBrick". An earlier draft wrote
        // stoneBrick here, which dated stone bricks to a version that never had the name.
        assertEquals("stonebricksmooth", HistoricalBlockPalette.legacyNameFor("minecraft:stone_bricks"));
    }

    @Test
    @DisplayName("blocks Beta 1.7.3 had are allowed, including the renamed ones")
    void eraBlocksAreAllowed() {
        Set<String> palette = HistoricalBlockPalette.paletteFor(BETA_1_7_3);
        for (String allowed : Set.of("minecraft:stone", "minecraft:grass_block", "minecraft:dirt",
                "minecraft:bedrock", "minecraft:oak_log", "minecraft:birch_log", "minecraft:oak_leaves",
                "minecraft:poppy", "minecraft:dandelion", "minecraft:sugar_cane",
                "minecraft:short_grass", "minecraft:spawner", "minecraft:mossy_cobblestone",
                "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:gold_ore",
                "minecraft:diamond_ore", "minecraft:redstone_ore", "minecraft:lapis_ore",
                "minecraft:obsidian", "minecraft:piston", "minecraft:sticky_piston",
                "minecraft:crafting_table", "minecraft:cobweb", "minecraft:clay", "minecraft:cactus",
                "minecraft:white_wool", "minecraft:red_bed", "minecraft:farmland", "minecraft:wheat")) {
            assertEquals(HistoricalBlockPalette.Verdict.ALLOWED,
                HistoricalBlockPalette.judge(palette, allowed),
                allowed + " existed in Beta 1.7.3 and must not be reported as an anachronism");
        }
    }

    @Test
    @DisplayName("blocks later than Beta 1.7.3 are rejected BY NAME, which is the point of the table")
    void laterBlocksAreAnachronistic() {
        Set<String> palette = HistoricalBlockPalette.paletteFor(BETA_1_7_3);
        // vine is the case the version-agnostic check structurally cannot see: vines are Release 1.6.4
        // content, so they are on no "later than everything" list, yet Beta 1.8 introduced them.
        for (String rejected : Set.of("minecraft:vine", "minecraft:leaf_litter",
                "minecraft:firefly_bush", "minecraft:melon", "minecraft:glass_pane",
                "minecraft:iron_bars", "minecraft:brick_stairs", "minecraft:stone_brick_stairs",
                "minecraft:oak_fence_gate", "minecraft:brown_mushroom_block", "minecraft:emerald_ore",
                "minecraft:kelp", "minecraft:seagrass", "minecraft:coal_block",
                "minecraft:slime_block", "minecraft:white_carpet", "minecraft:dirt_path",
                "minecraft:white_stained_glass", "minecraft:sea_lantern", "minecraft:white_banner")) {
            assertEquals(HistoricalBlockPalette.Verdict.ANACHRONISTIC,
                HistoricalBlockPalette.judge(palette, rejected),
                rejected + " did not exist in Beta 1.7.3 and must be reported");
        }
    }

    @Test
    @DisplayName("an unmapped block is UNKNOWN, never quietly acceptable")
    void unmappedIsUnknown() {
        Set<String> palette = HistoricalBlockPalette.paletteFor(BETA_1_7_3);
        // Copper is not in the mapping table because it is already caught by the version-agnostic list.
        // Reporting UNKNOWN keeps that visible rather than implying the check approved it.
        assertEquals(HistoricalBlockPalette.Verdict.UNKNOWN,
            HistoricalBlockPalette.judge(palette, "minecraft:copper_ore"));
        assertNull(HistoricalBlockPalette.legacyNameFor("minecraft:copper_ore"));
        // Block-state suffixes must not defeat the lookup.
        // Each block keeps its OWN name rather than being folded into a relative, so the only claim the
        // check makes is "this name is not in the palette". The returned name is the legacy spelling.
        assertEquals("caveVines",
            HistoricalBlockPalette.legacyNameFor("minecraft:cave_vines[berries=true]"));
        assertFalse(HistoricalBlockPalette.BETA_1_7_3.contains("caveVines"));
        assertEquals(HistoricalBlockPalette.Verdict.ANACHRONISTIC,
            HistoricalBlockPalette.judge(palette, "minecraft:vine[up=true]"));
    }

    @Test
    @DisplayName("a version with no extracted palette gets no answer, never a borrowed one")
    void noPaletteIsNotABorrowedPalette() {
        assertNull(HistoricalBlockPalette.paletteFor("historical_experience:release_1_2_5"),
            "answering for another version by borrowing Beta 1.7.3's palette is the substitution this "
                + "project keeps having to undo");
        assertNull(HistoricalBlockPalette.paletteFor("historical_experience:alpha"));
    }

    @Test
    @DisplayName("the mapping table is unambiguous")
    void mappingHasNoContradictions() {
        // The duplicate guard in the table throws during class initialisation, so reaching this point at
        // all proves no modern id is mapped to two different legacy names. The assertions below confirm
        // the table actually loaded and is not trivially empty.
        assertTrue(HistoricalBlockPalette.mappedBlockCount() > 150,
            "the mapping table looks unloaded or truncated: "
                + HistoricalBlockPalette.mappedBlockCount() + " entries");
        assertFalse(HistoricalBlockPalette.mapping().isEmpty());
        assertTrue(HistoricalBlockPalette.BETA_1_7_3.contains("cloth"),
            "wool is the block the rejected extraction omitted");
        assertFalse(HistoricalBlockPalette.BETA_1_7_3.contains("vine"),
            "vines arrived in Beta 1.8");
    }
}

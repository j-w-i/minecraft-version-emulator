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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import net.historicalexperience.generation.HistoricalBlockAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The crafting era list, checked against the game's REAL recipes.
 *
 * <p>The data files are extracted from the 1.21.1 jar ({@code data/minecraft/recipe}), not typed from memory: 887
 * crafting recipes with their result items, and the 14 special recipes that have no fixed result. A test that only
 * checked the cases its author thought of would pass for the same reason the old gate did - it would never look at the
 * 400-odd blocks nobody listed.
 */
class HistoricalCraftingEraTest {
    private static final String ALPHA = "historical_experience:alpha_1_0_1";
    private static final String BETA_1_2 = "historical_experience:beta_1_2";
    private static final String BETA_1_3 = "historical_experience:beta_1_3";
    private static final String BETA_1_7 = "historical_experience:beta_1_7_3";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";

    /** Recipe path to result item, for every crafting recipe in 1.21.1. */
    private static TreeMap<String, String> recipes() throws IOException {
        TreeMap<String, String> out = new TreeMap<>();
        try (InputStream in = HistoricalCraftingEraTest.class
                .getResourceAsStream("/crafting/crafting-recipes-1.21.1.txt")) {
            assertTrue(in != null, "the recipe data file must be on the test classpath");
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!line.isBlank()) {
                    String[] parts = line.trim().split(" ");
                    out.put(parts[0], "minecraft:" + parts[1]);
                }
            }
        }
        return out;
    }

    private static List<String> specialRecipes() throws IOException {
        try (InputStream in = HistoricalCraftingEraTest.class
                .getResourceAsStream("/crafting/special-recipes-1.21.1.txt")) {
            assertTrue(in != null, "the special-recipe data file must be on the test classpath");
            List<String> out = new ArrayList<>();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (!line.isBlank()) {
                    out.add(line.trim());
                }
            }
            return out;
        }
    }

    @Test
    @DisplayName("the recipe data really is the 1.21.1 crafting list")
    void dataIsComplete() throws IOException {
        assertEquals(887, recipes().size(), "1.21.1 has 887 crafting recipes with a result");
        assertEquals(14, specialRecipes().size());
    }

    @Test
    @DisplayName("Alpha 1.0.1 can craft only what the allow-list gives it, and nothing from the modern game")
    void alphaCannotCraftTheModernGame() throws IOException {
        TreeSet<String> craftable = new TreeSet<>();
        for (var recipe : recipes().entrySet()) {
            if (HistoricalCraftingEra.isRecipeRefused(ALPHA, recipe.getKey())) {
                continue;
            }
            // Every recipe in this test is judged exactly as the mixin judges it: id first, then result - and then
            // the result the player actually receives, which is what a substituted wood changes.
            String obtained = HistoricalCraftingEra.substitutedResult(ALPHA, recipe.getValue());
            String result = obtained != null ? obtained : recipe.getValue();
            if (!HistoricalCraftingEra.isRefused(ALPHA, recipe.getValue(), null)) {
                craftable.add(result.replace("minecraft:", ""));
            }
        }
        // The things the owner named, and the hundreds the old gate missed.
        for (String modern : List.of("crossbow", "shield", "spyglass", "netherite_ingot", "copper_ingot",
                "cherry_planks", "bamboo_planks", "mangrove_planks", "crimson_planks", "warped_planks",
                "white_concrete_powder", "red_banner", "white_stained_glass", "cut_copper", "tuff_bricks",
                "polished_andesite", "granite_wall", "smithing_table", "loom", "barrel", "lantern", "campfire",
                "observer", "honey_block", "beehive", "chain", "end_rod", "elytra_placeholder")) {
            assertFalse(craftable.contains(modern), modern + " must not be craftable in Alpha 1.0.1");
        }
        // The per-species planks are the one family a pre-Release-1.2 world substitutes rather than refuses, so what a
        // player gets must be the single planks item and never the species ones.
        for (String species : List.of("spruce_planks", "birch_planks", "jungle_planks")) {
            assertFalse(craftable.contains(species),
                species + " did not exist as an item in Alpha; the recipe yields planks instead");
        }
        // And nothing in the whole modern-wood / modern-block families got through.
        for (String id : craftable) {
            assertFalse(id.contains("cherry") || id.contains("bamboo") || id.contains("mangrove")
                    || id.contains("crimson") || id.contains("warped") || id.contains("copper")
                    || id.contains("deepslate") || id.contains("tuff") || id.contains("candle")
                    || id.contains("concrete") || id.contains("banner") || id.contains("stained_glass")
                    || id.contains("_wall") && !id.equals("cobblestone_wall"),
                "a modern id is craftable in Alpha 1.0.1: " + id);
        }
        // What Alpha genuinely had is still there.
        for (String old : List.of("oak_planks", "stick", "crafting_table", "chest", "furnace", "torch", "wooden_sword",
                "iron_pickaxe", "diamond_chestplate", "bow", "arrow", "ladder", "oak_door", "tnt", "rail", "minecart",
                "bucket", "flint_and_steel")) {
            assertTrue(craftable.contains(old), old + " must stay craftable in Alpha 1.0.1");
        }
    }

    @Test
    @DisplayName("a log makes planks up to Release 1.2, and its own planks from Release 1.2")
    void logsArePlanksBeforeRelease12() {
        // The owner's report: "Birch trees are available in beta 1.2 but I can't craft anything with the logs and they
        // dont go into planks. Until Release 1.2 they were just wooden planks but in 1.2 they start crafting into
        // birch planks." So before Release 1.2 the species planks recipes must be ALLOWED (not refused, which left
        // the log with no recipe at all) and their result rewritten to the one planks item.
        for (String version : List.of("historical_experience:alpha_1_0_1", "historical_experience:beta_1_2",
                "historical_experience:beta_1_7_3", "historical_experience:release_1_0_0",
                "historical_experience:release_1_1")) {
            for (String species : List.of("minecraft:spruce_planks", "minecraft:birch_planks",
                    "minecraft:jungle_planks")) {
                assertFalse(HistoricalCraftingEra.isRefused(version, species, species),
                    species + " must not be refused in " + version + ", or the log has no recipe left");
                assertEquals(HistoricalCraftingEra.GENERIC_PLANKS,
                    HistoricalCraftingEra.substitutedResult(version, species),
                    species + " must become the single planks item in " + version);
            }
        }

        // Release 1.2 is the boundary the owner named: from there the recipes make their own planks.
        for (String version : List.of("historical_experience:release_1_2_5", "historical_experience:release_1_3",
                "historical_experience:release_1_6_4")) {
            for (String species : List.of("minecraft:spruce_planks", "minecraft:birch_planks",
                    "minecraft:jungle_planks")) {
                assertNull(HistoricalCraftingEra.substitutedResult(version, species),
                    species + " is its own item from Release 1.2 onward, in " + version);
                assertFalse(HistoricalCraftingEra.isRefused(version, species, species), species + " in " + version);
            }
        }

        // Only that family is substituted, and only for a declared version.
        assertNull(HistoricalCraftingEra.substitutedResult("historical_experience:beta_1_7_3",
            "minecraft:oak_planks"), "the single planks item is not substituted");
        assertNull(HistoricalCraftingEra.substitutedResult("historical_experience:beta_1_7_3",
            "minecraft:cherry_planks"), "a later wood is refused, not substituted");
        assertNull(HistoricalCraftingEra.substitutedResult("historical_experience:beta_1_7_3", "minecraft:stick"));
        assertNull(HistoricalCraftingEra.substitutedResult(null, "minecraft:birch_planks"),
            "an ordinary world has no version and nothing is substituted in it");
    }

    @Test
    @DisplayName("the version gates the owner named: beds at Beta 1.3, and nothing is lost the version really had")
    void datedBoundaries() {
        assertTrue(HistoricalCraftingEra.isRefused(BETA_1_2, "minecraft:red_bed", "minecraft:red_bed"));
        assertFalse(HistoricalCraftingEra.isRefused(BETA_1_3, "minecraft:red_bed", "minecraft:red_bed"));
        assertTrue(HistoricalCraftingEra.isRefused(BETA_1_3, "minecraft:shears", null));
        assertFalse(HistoricalCraftingEra.isRefused(BETA_1_7, "minecraft:shears", null));
        assertTrue(HistoricalCraftingEra.isRefused(BETA_1_7, "minecraft:stone_bricks", "minecraft:stone_bricks"));
        assertFalse(HistoricalCraftingEra.isRefused("historical_experience:beta_1_8_1", "minecraft:stone_bricks",
            "minecraft:stone_bricks"));
        assertTrue(HistoricalCraftingEra.isRefused(BETA_1_2, "minecraft:cookie", null));
        assertFalse(HistoricalCraftingEra.isRefused("historical_experience:beta_1_4", "minecraft:cookie", null));
        // Cake is Beta 1.2: the old gate would have refused it from the palette's inferred 1.7.3 date.
        assertFalse(HistoricalCraftingEra.isRefused(BETA_1_2, "minecraft:cake", "minecraft:cake"));
        assertTrue(HistoricalCraftingEra.isRefused(ALPHA, "minecraft:cake", "minecraft:cake"));
    }

    @Test
    @DisplayName("the newest declared build still crafts everything the 1.6.4 client had, and nothing it lacked")
    void release164() throws IOException {
        for (var recipe : recipes().entrySet()) {
            boolean refused = HistoricalCraftingEra.isRecipeRefused(RELEASE_1_6_4, recipe.getKey())
                || HistoricalCraftingEra.isRefused(RELEASE_1_6_4, recipe.getValue(), null);
            boolean existed = HistoricalCraftingEra.existedBy164(recipe.getValue());
            if (!existed) {
                assertTrue(refused, recipe.getKey() + " makes " + recipe.getValue()
                    + ", which 1.6.4 lacks, so it must be refused");
            }
        }
        for (String id : List.of("minecraft:crossbow", "minecraft:shield", "minecraft:netherite_ingot",
                "minecraft:white_dye", "minecraft:cherry_planks", "minecraft:copper_ingot")) {
            assertTrue(HistoricalCraftingEra.isRefused(RELEASE_1_6_4, id, null), id + " must be refused in 1.6.4");
        }
    }

    @Test
    @DisplayName("every allow-listed id is a real result of a real recipe, so a typo cannot hide")
    void everyEntryMatchesARecipe() throws IOException {
        TreeSet<String> produced = new TreeSet<>(recipes().values());
        List<String> dead = new ArrayList<>();
        for (String id : HistoricalCraftingEra.allowListed()) {
            if (!produced.contains(id)) {
                dead.add(id);
            }
        }
        // Items that exist only as results of smelting/stonecutting/other recipe types are not in the crafting file.
        dead.removeIf(id -> List.of("minecraft:glowstone", "minecraft:clay", "minecraft:sandstone",
            "minecraft:white_wool", "minecraft:snow_block", "minecraft:coal", "minecraft:iron_ingot",
            "minecraft:gold_ingot", "minecraft:green_dye").contains(id));
        assertTrue(dead.isEmpty(), "allow-list entries that match no crafting recipe (typos?): " + dead);
    }

    @Test
    @DisplayName("alternate routes added later are refused, but the dye mixes that existed are kept")
    void fromRecipes() {
        assertTrue(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "red_dye_from_beetroot"));
        assertTrue(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "bone_meal_from_bone_block"));
        assertTrue(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "stick_from_bamboo_item"));
        assertFalse(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "red_dye_from_poppy"));
        assertFalse(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "sugar_from_sugar_cane"));
        assertFalse(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "iron_ingot_from_iron_block"));
    }

    @Test
    @DisplayName("special recipes with no fixed result are judged by id, and the never-existed ones are refused everywhere")
    void specialRecipesAreJudgedById() throws IOException {
        for (String never : List.of("tipped_arrow", "suspicious_stew", "shulker_box_coloring", "shield_decoration",
                "decorated_pot", "banner_duplicate")) {
            assertTrue(specialRecipes().contains(never), never + " is a real 1.21.1 special recipe");
            for (String version : HistoricalBlockAvailability.CHRONOLOGY) {
                assertTrue(HistoricalCraftingEra.isRecipeRefused(version, never), never + " must be refused in " + version);
            }
        }
        assertTrue(HistoricalCraftingEra.isRecipeRefused(BETA_1_7, "firework_rocket"));
        assertFalse(HistoricalCraftingEra.isRecipeRefused(RELEASE_1_6_4, "firework_rocket"));
        assertTrue(HistoricalCraftingEra.isRecipeRefused(BETA_1_2, "repair_item"));
        assertFalse(HistoricalCraftingEra.isRecipeRefused("historical_experience:beta_1_9_pre_2", "repair_item"));
    }

    @Test
    @DisplayName("an ordinary world, or an unknown version, is never refused")
    void ordinaryWorldsAreUntouched() throws IOException {
        for (String world : new String[] {null, "moderner_beta:beta", "example:nothing"}) {
            assertFalse(HistoricalCraftingEra.isRefused(world, "minecraft:crossbow", null));
            assertFalse(HistoricalCraftingEra.isRecipeRefused(world, "tipped_arrow"));
        }
    }
}

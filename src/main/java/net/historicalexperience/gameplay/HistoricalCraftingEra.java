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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.historicalexperience.generation.HistoricalBlockAvailability;

/**
 * What a declared version could actually craft, judged by an ALLOW-list rather than a deny-list.
 *
 * <h2>Why the polarity was flipped</h2>
 *
 * <p>The earlier gate asked "is this result on a list of known anachronisms?". That list cannot be finished, and the
 * owner kept finding the gap by crafting things. Measured against the game's own 887 crafting recipes: of the 608
 * blocks they produce, 421 had no record in either dating table and were therefore silently <b>allowed</b> - cherry,
 * bamboo, mangrove, crimson and warped wood, concrete powder, banners, stained glass, copper, every wall and every
 * modern slab - in Alpha 1.0.1 exactly as in Release 1.6.4. Non-block items were not judged at all, because the gate
 * only looked at {@code BlockItem}s, so a crossbow, a shield, a spyglass or netherite could be crafted in every
 * version.
 *
 * <p>The newest declared build is Release 1.6.4, so anything that does not exist in the 1.6.4 client exists in
 * <b>no</b> declared version. This class lists what DID exist by 1.6.4, with the first declared version that had it,
 * and refuses everything else in every version. A mistake in the list is then visible - a missing entry refuses
 * something real, which a test or a play session shows at once - instead of a missing entry silently allowing
 * something wrong. {@code HistoricalCraftingEraTest} also checks every entry against the real recipe list, so a typo
 * cannot hide as an entry that never matches anything.
 *
 * <h2>Where each date comes from</h2>
 *
 * <p>Three grades of evidence, and the grade decides how much to trust a date:
 * <ul>
 *   <li><b>Palette</b> - the block's name is absent from one extracted client and present in the next. Strong for
 *       blocks. Used for stone bricks, glass panes, iron bars, melons and fence gates (Beta 1.8.1), the nether brick
 *       family and the enchanting table, cauldron and brewing stand (Beta 1.9 Prerelease 3), and the 1.6.4-only
 *       blocks.</li>
 *   <li><b>Stated</b> - {@code research/minecraft-version-history.md} or {@code feature-timeline.json} says it. Used
 *       for beds and repeaters (Beta 1.3), powered and detector rails (Beta 1.5), maps and trapdoors (Beta 1.6),
 *       pistons and shears (Beta 1.7), sugar, note blocks, dispensers and dyes (Beta 1.2), paper and books (Alpha
 *       1.0.11), boats (Alpha 1.0.6) and the anvil (1.4.2).</li>
 *   <li><b>Bounded only</b> - absent from the 1.2.5 palette and present in 1.6.4, or an item with no palette at all.
 *       The earliest declared version that could have it is used, which is the safe direction: a date that is too
 *       EARLY lets a version craft something a build early, a defect that leaves the game playable, while a date
 *       that is too LATE deletes content the version really had. Items in this grade are marked below.</li>
 * </ul>
 *
 * <p>The block-level dating table ({@code HistoricalBlockAvailability}) still applies on top, so a block it dates
 * more precisely than this list is refused by that date as well.
 */
public final class HistoricalCraftingEra {
    private HistoricalCraftingEra() {
    }

    private static final String P = "historical_experience:";

    private static final List<String> COLOURS = List.of(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");

    /** Craftable ids that existed by Release 1.6.4: {@code modern id -> first declared version}, null meaning always. */
    private static final Map<String, String> EXISTED_BY_1_6_4 = new HashMap<>();

    private static void always(String... ids) {
        for (String id : ids) {
            EXISTED_BY_1_6_4.put("minecraft:" + id, null);
        }
    }

    private static void from(String version, String... ids) {
        for (String id : ids) {
            EXISTED_BY_1_6_4.put("minecraft:" + id, P + version);
        }
    }

    static {
        // ---- Present in every declared build (Classic/Indev/Infdev content) --------------------------------
        always("oak_planks", "stick", "crafting_table", "chest", "furnace", "torch", "ladder", "oak_door",
            "oak_stairs", "cobblestone_stairs", "oak_fence", "oak_sign", "tnt", "stone_slab", "bookshelf",
            "compass", "clock", "painting", "bow", "arrow", "bucket", "bowl", "mushroom_stew", "bread",
            "flint_and_steel", "coal", "diamond", "iron_ingot", "gold_ingot", "redstone", "white_wool",
            "diamond_block", "iron_block", "gold_block");
        for (String tier : List.of("wooden", "stone", "iron", "golden", "diamond")) {
            for (String tool : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) {
                always(tier + "_" + tool);
            }
        }
        for (String armour : List.of("leather", "iron", "golden", "diamond")) {
            for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                always(armour + "_" + slot);
            }
        }

        // ---- Alpha ----------------------------------------------------------------------------------------
        // Alpha 1.0.1 (Seecret Friday Update 3): redstone, levers, buttons, pressure plates, iron doors.
        from("alpha_1_0_1", "minecart", "iron_door", "lever", "stone_button", "stone_pressure_plate", "oak_pressure_plate",
            "redstone_torch", "rail");
        from("alpha_1_0_4", "snow_block");
        from("alpha_1_0_6", "oak_boat");
        from("alpha_1_0_11", "paper", "book", "clay", "bricks");
        from("alpha_1_0_14", "jukebox", "chest_minecart", "furnace_minecart");
        from("alpha_1_2_0", "fishing_rod", "glowstone", "jack_o_lantern");

        // ---- Beta 1.0 - 1.7 (stated) ----------------------------------------------------------------------
        // Beta 1.2: dyes and coloured wool, bone meal, sugar, note block, dispenser, lapis, sandstone, cake.
        from("beta_1_2", "bone_meal", "sugar", "note_block", "dispenser", "lapis_lazuli", "lapis_block", "sandstone",
            "cake");
        for (String colour : COLOURS) {
            if (!colour.equals("white")) {
                // White is bone meal itself in this era; 'white_dye' is a 1.14 item and is deliberately absent.
                from("beta_1_2", colour + "_dye", colour + "_wool");
            }
            // Beds are Beta 1.3 (the owner's example, and the block table states it).
            from("beta_1_3", colour + "_bed");
        }
        // Beta 1.3: beds, repeaters, wood/cobblestone/sandstone slabs.
        from("beta_1_3", "repeater", "oak_slab", "cobblestone_slab", "sandstone_slab");
        from("beta_1_4", "cookie");
        // Beta 1.5: powered and detector rails. Beta 1.6: maps and wooden trapdoors. Beta 1.7: pistons and shears.
        from("beta_1_5", "powered_rail", "detector_rail");
        from("beta_1_6", "map", "oak_trapdoor");
        from("beta_1_7_3", "piston", "sticky_piston", "shears");

        // ---- Beta 1.8 (palette: absent in 1.7.3, present in 1.8.1) ----------------------------------------
        from("beta_1_8_1", "stone_bricks", "chiseled_stone_bricks", "stone_brick_stairs", "stone_brick_slab",
            "brick_stairs", "brick_slab", "iron_bars", "glass_pane", "melon", "oak_fence_gate", "melon_seeds",
            "pumpkin_seeds", "pumpkin_pie");

        // ---- Beta 1.9 prereleases (palette: absent in 1.8.1, present in Pre3) -----------------------------
        // Beta 1.9 Prerelease 1: nether bricks and their stairs/fences, per research/version-transitions.json
        // ("nether bricks, nether brick stairs and fences"), which also lists gold nuggets. The audit found these
        // dated a boundary too late because the palette first sees the NAMES in Pre3's extraction.
        from("beta_1_9_pre_1", "nether_bricks", "nether_brick_stairs", "nether_brick_fence", "gold_nugget");
        // Beta 1.9 Prerelease 2: the potion system's ingredients, per the transitions entry (cauldron prototype,
        // "glass bottles, spider eyes, blaze powder, fermented spider eye, magma cream", "potion/status-effect
        // system", and animal breeding). Only the stand itself is Pre3.
        from("beta_1_9_pre_2", "glass_bottle", "blaze_powder", "fermented_spider_eye", "magma_cream");
        // Spider eyes are NOT here: they are a mob drop with no recipe, so the allow-list (which is a list of
        // recipe results) cannot carry them. Beta 1.9 drops them from spiders through the modern loot path.
        // Beta 1.9 Prerelease 3: the enchanting table, the cauldron as a real block, the brewing stand, and the
        // End-related items (eye of ender), plus the glistering melon and fire charge whose dates the audit could
        // not move to Pre2 on the evidence it had.
        from("beta_1_9_pre_3", "enchanting_table", "cauldron", "brewing_stand", "glistering_melon_slice",
            "ender_eye", "fire_charge");

        // The golden apple is Infdev content (added 20100227, after this project's Indev profile), so it is
        // available from the oldest Infdev build. It used to be dated Beta 1.8 here, which made it uncraftable
        // in every Beta world even though the era crafted it.
        from("infdev_420", "golden_apple");

        // ---- Release 1.2: the other woods' planks ---------------------------------------------------------
        from("release_1_2_5", "spruce_planks", "birch_planks", "jungle_planks");

        // ---- Release 1.3+ ---------------------------------------------------------------------------------
        // BOUNDED ONLY (absent from the 1.2.5 palette, present in 1.6.4): the earliest declared version after 1.2.5.
        // The second golden-apple tier - the item 1.13 renamed the enchanted golden apple - arrives in Release 1.3.1
        // (12w21a) and stops being craftable in 1.9 (15w44a). Its recipe no longer exists in modern Minecraft, so
        // RemovedRecipes supplies the recipe; this entry is what stops the gate refusing the result.
        from("release_1_3", "enchanted_golden_apple", "sandstone_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs", "spruce_slab",
            "birch_slab", "jungle_slab", "emerald", "emerald_block", "ender_chest", "writable_book",
            "cobblestone_wall", "mossy_cobblestone_wall", "flower_pot", "tripwire_hook", "redstone_block",
            "light_weighted_pressure_plate", "heavy_weighted_pressure_plate", "trapped_chest", "hopper", "dropper",
            "comparator", "daylight_detector", "activator_rail", "hopper_minecart", "tnt_minecart", "quartz_block",
            "chiseled_quartz_block", "quartz_pillar", "quartz_slab", "quartz_stairs", "nether_brick_slab",
            "redstone_lamp", "oak_button");
        // STATED: the anvil is 1.4.2 (12w41a); the others are 1.4.x items with no palette, dated to the Release 1.4
        // profile as the earliest that could have them.
        from("release_1_4", "anvil", "item_frame", "carrot_on_a_stick", "golden_carrot", "firework_rocket", "beacon");
        // Release 1.6: stained clay, carpets, hay bales, coal blocks, leads.
        for (String colour : COLOURS) {
            from("release_1_6_4", colour + "_terracotta", colour + "_carpet");
        }
        from("release_1_6_4", "hay_block", "coal_block", "lead");
    }

    /**
     * Recipes whose id says "made from X" were mostly added later to give a new ingredient a route to an old item.
     * They are refused everywhere EXCEPT these, which existed by 1.6.4: dyes mixed from other dyes, the block-to-ingot
     * unpackings, sugar from cane, and the two flowers that were dyes then.
     */
    private static final Set<String> FROM_RECIPES_ALLOWED = Set.of(
        "gold_ingot_from_gold_block", "iron_ingot_from_iron_block", "gold_ingot_from_nuggets",
        "sugar_from_sugar_cane", "red_dye_from_poppy", "yellow_dye_from_dandelion", "pink_dye_from_red_white_dye",
        "orange_dye_from_red_yellow", "light_gray_dye_from_black_white_dye", "light_gray_dye_from_gray_white_dye",
        "light_blue_dye_from_blue_white_dye", "magenta_dye_from_blue_red_white_dye",
        "magenta_dye_from_blue_red_pink", "magenta_dye_from_purple_and_pink");

    /**
     * Recipes with no fixed result item (the game assembles one from the inputs), so the result cannot be judged and
     * the recipe id is. Value is the first declared version; null means always.
     */
    private static final Map<String, String> SPECIAL_RECIPES = Map.of(
        "repair_item", P + "beta_1_9_pre_2",
        "armor_dye", P + "release_1_4",
        "firework_rocket", P + "release_1_4",
        "firework_star", P + "release_1_4",
        "firework_star_fade", P + "release_1_4",
        "book_cloning", P + "release_1_3",
        "map_cloning", P + "release_1_3",
        "map_extending", P + "release_1_3");

    /**
     * Special recipes that exist in no declared version: banners and shields are 1.8 and later, tipped arrows and
     * suspicious stew are 1.9 and 1.14, shulker boxes are 1.11, the decorated pot is 1.20. They are listed rather than
     * left to fall through, because a special recipe has no fixed result for the allow-list to judge.
     */
    private static final Set<String> SPECIAL_RECIPES_NEVER = Set.of(
        "tipped_arrow", "suspicious_stew", "shulker_box_coloring", "shield_decoration", "decorated_pot",
        "banner_duplicate");
    /** Every modern id the allow-list knows, for tests and the probe. */
    public static Set<String> allowListed() {
        return java.util.Collections.unmodifiableSet(EXISTED_BY_1_6_4.keySet());
    }

    /** The first declared version that had this id, or null when it is craftable throughout or not allow-listed. */
    public static String firstVersion(String itemId) {
        return EXISTED_BY_1_6_4.get(itemId);
    }

    /** Whether the id is on the allow-list at all. */
    public static boolean existedBy164(String itemId) {
        return EXISTED_BY_1_6_4.containsKey(itemId);
    }

    private static boolean declared(String versionId) {
        return versionId != null && HistoricalBlockAvailability.CHRONOLOGY.contains(versionId);
    }

    private static boolean before(String versionId, String firstVersion) {
        return HistoricalBlockAvailability.CHRONOLOGY.indexOf(versionId)
            < HistoricalBlockAvailability.CHRONOLOGY.indexOf(firstVersion);
    }

    /**
     * Whether a recipe is refused by its OWN id, independent of what it makes.
     *
     * @param recipePath the recipe's path within the {@code minecraft} namespace, e.g. {@code red_dye_from_tulip}
     */
    public static boolean isRecipeRefused(String versionId, String recipePath) {
        if (!declared(versionId) || recipePath == null) {
            return false;
        }
        if (SPECIAL_RECIPES_NEVER.contains(recipePath)) {
            return true;
        }
        if (SPECIAL_RECIPES.containsKey(recipePath)) {
            String first = SPECIAL_RECIPES.get(recipePath);
            return before(versionId, first);
        }
        if (recipePath.contains("_from_")) {
            return !FROM_RECIPES_ALLOWED.contains(recipePath);
        }
        return false;
    }

    /**
     * The item a recipe's result becomes in this version, when the result itself had not been separated out yet.
     *
     * <h2>The woods, reported by the owner</h2>
     *
     * <blockquote>"Birch trees are available in beta 1.2 but I can't craft anything with the logs and they dont go into
     * planks. Until Release 1.2 they were just wooden planks but in 1.2 they start crafting into birch planks."</blockquote>
     *
     * <p>So there was one planks item, and any log made it. Refusing the per-species planks recipes (which is what
     * {@link #EXISTED_BY_1_6_4} does on its own) therefore left a birch, spruce or jungle log with no route to planks
     * at all - the recipe was hidden and nothing replaced it. The version's answer is the generic planks, which is
     * {@code minecraft:oak_planks} here because it is the plank this project already gives those versions.
     *
     * <p>Substitution is judged by the RESULT, not by the recipe: every route to a per-species plank before Release
     * 1.2 produced the one planks item, whatever the recipe was called.
     *
     * @return the replacement item id, or null when this result stands as it is
     */
    public static String substitutedResult(String versionId, String itemId) {
        if (!declared(versionId) || itemId == null) {
            return null;
        }
        if (BED_ITEMS.contains(itemId) && !RED_BED.equals(itemId)) {
            // Coloured beds are Release 1.12 (17w15a); before that there was one bed and it was red, whatever wool
            // was used - minecraft.wiki, Bed: "The red bed was the original default bed color", and the sixteen
            // coloured beds appear in its 1.12 history row. None of these profiles is 1.12, and a test asserts that.
            //
            // Before Beta 1.3 there is no bed at all, so this returns null and the recipe stays refused: a bed must
            // not be conjured out of wool in a build that had none.
            String bedsArrive = EXISTED_BY_1_6_4.get(itemId);
            return bedsArrive != null && !before(versionId, bedsArrive) ? RED_BED : null;
        }
        String first = EXISTED_BY_1_6_4.get(itemId);
        if (first == null || !before(versionId, first)) {
            return null;
        }
        return SPECIES_PLANKS.contains(itemId) ? GENERIC_PLANKS : null;
    }

    /** The one bed those versions had. */
    public static final String RED_BED = "minecraft:red_bed";

    /**
     * The chronology of Release 1.12, where the coloured beds arrive, as a literal because no such profile is
     * declared. {@link #substitutedResult} treats every bed as red and does not consult it, so a test asserts that no
     * declared version reaches it - if one ever does, that test fails and the boundary has to be written properly.
     */
    public static int colouredBedsChronology() {
        return Milestones.RELEASE_1_12.key();
    }

    /** Every bed item in the modern game, which is the family the substitution above covers. */
    private static final Set<String> BED_ITEMS = bedItems();

    private static Set<String> bedItems() {
        Set<String> beds = new java.util.LinkedHashSet<>();
        for (String colour : COLOURS) {
            beds.add("minecraft:" + colour + "_bed");
        }
        beds.add(RED_BED);
        return Set.copyOf(beds);
    }

    /**
     * Whether a stack's item is shown under the plain name "Bed" in this version.
     *
     * <p>The owner: <i>"they were red beds for a long time no matter the wool used and they were just called
     * beds"</i>. The item a player holds is the modern {@code red_bed}, whose own name is "Red Bed", so the name has
     * to be answered by the version rather than by the item. Applies to every bed colour, because a world may
     * already hold beds of other colours.
     */
    public static boolean showsPlainBedName(String versionId, String itemId) {
        return declared(versionId) && itemId != null && BED_ITEMS.contains(itemId);
    }

    /** The three wood species whose planks are substituted before Release 1.2. */

    /** The per-species planks, which are the one family this project substitutes rather than refuses. */
    private static final Set<String> SPECIES_PLANKS =
        Set.of("minecraft:spruce_planks", "minecraft:birch_planks", "minecraft:jungle_planks");

    /** The single planks item those versions had. */
    public static final String GENERIC_PLANKS = "minecraft:oak_planks";

    /**
     * Whether crafting this result must be refused in this version.
     *
     * @param versionId a declared version; anything else (an ordinary world) is never refused
     * @param itemId    the result item's modern id
     * @param blockId   the result's block id when it is a block item, else null; the block table can date it more
     *                  precisely than this list does
     */
    public static boolean isRefused(String versionId, String itemId, String blockId) {
        if (!declared(versionId) || itemId == null) {
            return false;
        }
        if (EarlyGameplayRecipes.supports(versionId)) {
            return !EarlyGameplayRecipes.canCraftResult(versionId, itemId);
        }
        if (RemovedRecipes.replacesModernRecipe(versionId, itemId)) {
            // The version's own shape is supplied by RemovedRecipes; leaving the 1.21.1 recipe in place as well
            // would let the player craft the item from ingredients the build never used.
            return true;
        }
        if (!EXISTED_BY_1_6_4.containsKey(itemId)) {
            // Not in the 1.6.4 client, therefore in no declared version.
            return true;
        }
        if (substitutedResult(versionId, itemId) != null) {
            // Not refused: it is what this version's recipe produced, wearing the later item's name. The result is
            // rewritten instead - see CraftingRecipeHistoricalWoodMixin.
            return false;
        }
        String first = EXISTED_BY_1_6_4.get(itemId);
        if (first != null && before(versionId, first)) {
            return true;
        }
        return blockId != null && HistoricalRecipeGate.isBlockUnavailable(versionId, blockId);
    }
}

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

import java.util.*;
import net.historicalexperience.version.HistoricalSnapshotPatches;
import net.historicalexperience.version.HistoricalVersions;

/** Positive modern representations of dated historical catalog items. Never a saved-inventory filter.
 * Sources/alias and coarse-family limitations: research/creative-menu-catalog.json.
 */
public final class HistoricalCreativeCatalog {
    public record Window(int first, int endExclusive) { }
    private static final Map<String, Window> ITEMS = new LinkedHashMap<>();
    private static int date(String profile) {
        return HistoricalVersions.byId("historical_experience:" + profile).orElseThrow().chronology();
    }
    private static void from(String profile, String ids) {
        int first = date(profile);
        for (String id : ids.split("\\s+")) if (!id.isBlank()) ITEMS.put("minecraft:" + id, new Window(first, Integer.MAX_VALUE));
    }
    static {
        from("indev_latest", "stone grass_block dirt cobblestone oak_planks oak_sapling bedrock sand gravel gold_ore iron_ore coal_ore oak_log oak_leaves sponge glass white_wool dandelion poppy brown_mushroom red_mushroom gold_block iron_block smooth_stone_slab bricks tnt bookshelf mossy_cobblestone obsidian torch chest diamond_ore diamond_block crafting_table furnace wheat_seeds wheat bread stick bowl mushroom_stew flint_and_steel coal diamond iron_ingot gold_ingot bow arrow string feather gunpowder flint porkchop cooked_porkchop");
        for (String material : List.of("wooden", "stone", "iron", "golden", "diamond"))
            for (String tool : List.of("sword", "pickaxe", "axe", "shovel", "hoe")) from("indev_latest", material + "_" + tool);
        for (String material : List.of("leather", "chainmail", "iron", "golden", "diamond"))
            for (String part : List.of("helmet", "chestplate", "leggings", "boots")) from("indev_latest", material + "_" + part);
        from("infdev_420", "apple golden_apple");
        from("infdev_611", "ladder oak_door oak_sign painting");
        from("alpha_1_0_1", "oak_stairs cobblestone_stairs minecart bucket water_bucket lava_bucket iron_door lever stone_button stone_pressure_plate oak_pressure_plate redstone redstone_ore redstone_torch rail");
        from("alpha_1_0_4", "snow_block snowball ice");
        from("alpha_1_0_6", "oak_boat cactus");
        from("alpha_1_0_11", "paper book clay clay_ball brick sugar_cane leather milk_bucket");
        from("alpha_1_0_14", "jukebox music_disc_13 music_disc_cat chest_minecart furnace_minecart egg");
        from("alpha_1_0_17", "oak_fence slime_ball");
        from("alpha_1_1_0", "compass");
        from("alpha_1_2_0", "fishing_rod cod cooked_cod clock pumpkin jack_o_lantern netherrack soul_sand glowstone glowstone_dust");
        from("beta_1_2", "spruce_log birch_log lapis_ore lapis_lazuli lapis_block sandstone dispenser note_block bone bone_meal sugar cake ink_sac cocoa_beans");
        for (String color : List.of("orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            from("beta_1_2", color + "_wool");
            if (!Set.of("blue", "brown", "black").contains(color)) from("beta_1_2", color + "_dye");
        }
        from("beta_1_3", "red_bed repeater cobblestone_slab sandstone_slab petrified_oak_slab");
        ITEMS.put("minecraft:petrified_oak_slab", new Window(date("beta_1_3"), date("release_1_3")));
        from("beta_1_4", "cookie");
        from("beta_1_5", "powered_rail detector_rail spruce_sapling birch_sapling");
        from("beta_1_6", "map dead_bush oak_trapdoor");
        from("beta_1_7_3", "piston sticky_piston shears");
        from("beta_1_8_1", "stone_bricks mossy_stone_bricks cracked_stone_bricks stone_brick_stairs stone_brick_slab brick_stairs brick_slab iron_bars glass_pane melon melon_slice melon_seeds pumpkin_seeds oak_fence_gate vine beef cooked_beef chicken cooked_chicken rotten_flesh ender_pearl");
        from("beta_1_9_pre_1", "nether_bricks nether_brick_stairs nether_brick_fence nether_wart lily_pad blaze_rod ghast_tear gold_nugget spider_eye");
        from("beta_1_9_pre_3", "enchanting_table brewing_stand cauldron glass_bottle blaze_powder fermented_spider_eye glistering_melon_slice magma_cream ender_eye end_portal_frame");
        from("beta_1_9_pre_4", "end_stone");
        from("beta_1_9_pre_6", "short_grass fern spruce_leaves birch_leaves");
        from("release_1_0_0", "music_disc_blocks music_disc_chirp music_disc_far music_disc_mall music_disc_mellohi music_disc_stal music_disc_strad music_disc_ward music_disc_11");
        for (String mob : List.of("creeper", "skeleton", "spider", "zombie", "slime", "ghast", "zombified_piglin", "enderman", "cave_spider", "silverfish", "blaze", "magma_cube", "pig", "sheep", "cow", "chicken", "squid", "wolf", "mooshroom", "villager")) from("release_1_1", mob + "_spawn_egg");
        from("release_1_2_5", "jungle_log jungle_leaves jungle_sapling spruce_planks birch_planks jungle_planks chiseled_stone_bricks chiseled_sandstone cut_sandstone redstone_lamp ocelot_spawn_egg fire_charge experience_bottle");
        from("release_1_3", "oak_slab spruce_slab birch_slab jungle_slab spruce_stairs birch_stairs jungle_stairs sandstone_stairs emerald_ore emerald emerald_block ender_chest writable_book written_book tripwire_hook snow charcoal enchanted_golden_apple potion splash_potion infested_stone infested_cobblestone infested_stone_bricks");
        from("release_1_4", "anvil chipped_anvil damaged_anvil item_frame flower_pot cobblestone_wall mossy_cobblestone_wall carrot potato baked_potato poisonous_potato golden_carrot carrot_on_a_stick pumpkin_pie oak_button beacon nether_star skeleton_skull wither_skeleton_skull zombie_head player_head creeper_head music_disc_wait bat_spawn_egg witch_spawn_egg enchanted_book firework_rocket firework_star nether_brick_slab");
        from("release_1_5", "redstone_block light_weighted_pressure_plate heavy_weighted_pressure_plate trapped_chest hopper dropper comparator daylight_detector activator_rail hopper_minecart tnt_minecart quartz nether_quartz_ore quartz_block chiseled_quartz_block quartz_pillar quartz_slab quartz_stairs nether_brick");
        from("release_1_6_4", "terracotta hay_block coal_block lead name_tag saddle iron_horse_armor golden_horse_armor diamond_horse_armor horse_spawn_egg");
        for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) from("release_1_6_4", color + "_terracotta " + color + "_carpet");
        // Saddles existed long before horses; only their creative duplication is being dated here.
        from("alpha_1_0_17", "saddle");
    }
    private HistoricalCreativeCatalog() { }
    public static Map<String, Window> all() { return Collections.unmodifiableMap(ITEMS); }
    public static boolean available(String selection, String itemId) {
        var version = HistoricalSnapshotPatches.baseProfileForSelection(selection).flatMap(HistoricalVersions::byId).orElse(null);
        if (version == null) return true; // ordinary/unknown scope is never adopted
        var window = ITEMS.get(itemId);
        return window != null && version.chronology() >= window.first() && version.chronology() < window.endExclusive();
    }
    /** Modern effect names represent their old equivalents; later/custom effects are not new grants. */
    public static boolean potionAvailable(String selection, String potionId, boolean splash) {
        if (!HistoricalCreativeRules.gatesCatalog(selection)) return true;
        if (!available(selection, splash ? "minecraft:splash_potion" : "minecraft:potion")) return false;
        if (potionId == null || !potionId.startsWith("minecraft:")) return false;
        String name = potionId.substring(10);
        if (name.equals("water")) return !splash; // splash water entered the catalog in1.9
        if (name.startsWith("long_")) name = name.substring(5);
        else if (name.startsWith("strong_")) name = name.substring(7);
        if (Set.of("healing", "harming", "night_vision", "invisibility").contains(name))
            return available(selection, "minecraft:anvil"); //1.4 family,12w38a inventory/potion additions
        return Set.of("swiftness", "slowness", "strength", "weakness", "poison", "regeneration", "fire_resistance").contains(name);
    }
}

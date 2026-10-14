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
import java.util.Map;

/**
 * Blocks that did not exist in any version this project declares, with the release that introduced them.
 *
 * <h2>Why a curated list and not a full historical palette</h2>
 *
 * <p>A generated world is judged by asking "does it contain anything that could not have been there?". A
 * complete Beta 1.7.3 palette would answer that more precisely, but building one from memory is exactly
 * the failure this project keeps repeating: a plausible list that is subtly wrong looks like evidence. So
 * this list contains only blocks whose introduction is <b>unambiguously later</b> than the newest version
 * declared here (Release 1.6.4) - copper, deepslate, amethyst, the lush-cave and dripstone families, and
 * the 1.13+ ocean and Nether families. Anything on it is a defect in any historical world, with no
 * judgement call required.
 *
 * <p>The list is deliberately one-directional: it can prove a world is wrong, and it can never prove a
 * world is right. The probe reports it as {@code generation.anachronisticBlocks}, and an empty report
 * means "nothing on this list was found" - not "the world is historically correct".
 */
public final class HistoricalBlockAnachronisms {
    /** Block id -> the release that introduced it, for blocks no declared version could contain. */
    private static final Map<String, String> INTRODUCED_IN = new LinkedHashMap<>();

    private static void later(String release, String... blockIds) {
        for (String blockId : blockIds) {
            INTRODUCED_IN.put("minecraft:" + blockId, release);
        }
    }

    static {
        later("1.17", "copper_ore", "deepslate_copper_ore", "raw_copper_block", "copper_block",
            "deepslate", "cobbled_deepslate", "deepslate_bricks", "deepslate_tiles", "tuff",
            "calcite", "smooth_basalt", "amethyst_block", "budding_amethyst", "amethyst_cluster",
            "small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "tinted_glass",
            "moss_block", "moss_carpet", "azalea", "flowering_azalea", "azalea_leaves",
            "flowering_azalea_leaves", "hanging_roots", "big_dripleaf", "small_dripleaf",
            "glow_lichen", "cave_vines", "cave_vines_plant", "glow_berries", "spore_blossom",
            "raw_iron_block", "raw_gold_block", "lightning_rod", "candle", "waxed_copper_block");
        later("1.13", "kelp", "kelp_plant", "seagrass", "tall_seagrass", "sea_pickle", "dried_kelp_block",
            "conduit", "prismarine", "prismarine_bricks", "dark_prismarine", "sea_lantern",
            "blue_ice", "frosted_ice", "turtle_egg", "bubble_column", "stripped_oak_log",
            "stripped_birch_log", "stripped_spruce_log", "smooth_stone", "smooth_sandstone");
        later("1.16", "ancient_debris", "netherite_block", "crying_obsidian", "respawn_anchor",
            "nether_gold_ore", "quartz_bricks", "chiseled_nether_bricks", "cracked_nether_bricks",
            "soul_fire", "soul_soil", "soul_sand", "basalt", "polished_basalt", "blackstone",
            "gilded_blackstone", "polished_blackstone", "polished_blackstone_bricks", "shroomlight",
            "warped_stem", "crimson_stem", "nether_sprouts", "warped_roots", "crimson_roots",
            "twisting_vines", "weeping_vines", "chain", "lodestone", "target", "honey_block",
            "honeycomb_block", "bee_nest", "beehive");
        later("1.14", "bamboo", "bamboo_sapling", "scaffolding", "barrel", "smoker", "blast_furnace",
            "cartography_table", "fletching_table", "grindstone", "lectern", "loom", "stonecutter",
            "bell", "campfire", "sweet_berry_bush", "lantern", "cornflower", "lily_of_the_valley",
            "wither_rose", "smooth_quartz", "smooth_red_sandstone", "composter");
        later("1.15", "bee_nest_full", "honeycomb");
        later("1.19", "mangrove_log", "mangrove_leaves", "mangrove_roots", "muddy_mangrove_roots",
            "mud", "packed_mud", "mud_bricks", "sculk", "sculk_vein", "sculk_catalyst",
            "sculk_shrieker", "sculk_sensor", "reinforced_deepslate", "ochre_froglight",
            "verdant_froglight", "pearlescent_froglight");
        later("1.20", "cherry_log", "cherry_leaves", "cherry_planks", "bamboo_block",
            "stripped_bamboo_block", "chiseled_bookshelf", "decorated_pot", "calibrated_sculk_sensor",
            "suspicious_sand", "suspicious_gravel", "piglin_head");
        later("1.21", "crafter", "trial_spawner", "vault", "heavy_core", "copper_grate",
            "chiseled_copper", "copper_bulb", "copper_door", "copper_trapdoor", "tuff_bricks",
            "polished_tuff", "chiseled_tuff");
        // Found by censusing a real generated world rather than by reading a release list: a 289-chunk
        // Beta 1.7.3 world contained 7 firefly bushes and 1,769 leaf litter, and neither was on the
        // original list. The lesson is that a hand-written "too new" list has holes exactly where the
        // newest decorative plants are, because those are the ones nobody thinks of as content.
        later("1.21.5", "firefly_bush", "bush", "cactus_flower", "wildflowers", "short_dry_grass",
            "tall_dry_grass", "leaf_litter");
        later("1.21.4", "pale_oak_log", "pale_oak_wood", "stripped_pale_oak_log",
            "stripped_pale_oak_wood", "pale_oak_planks", "pale_oak_sapling", "pale_oak_leaves",
            "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss", "open_eyeblossom",
            "closed_eyeblossom", "creaking_heart", "resin_block", "resin_brick", "resin_clump",
            "chiseled_resin_bricks");
        // 1.8 is the boundary the project declares as its newest Release target, so anything from 1.9
        // onward is out of range for every version, including 1.8.9.
        later("1.9", "end_stone_bricks", "purpur_block", "purpur_pillar", "chorus_flower", "chorus_plant",
            "end_rod", "dragon_head", "beetroots", "grass_path", "structure_block");
        later("1.10", "magma_block", "bone_block", "nether_wart_block", "red_nether_brick",
            "husk_spawn_egg", "stray_spawn_egg");
        later("1.11", "observer", "shulker_box", "white_shulker_box");
        later("1.12", "concrete", "white_concrete", "concrete_powder", "glazed_terracotta",
            "white_glazed_terracotta", "magenta_glazed_terracotta");
        // Corrected: terracotta was listed under 1.12, which is when the block was RENAMED from hardened
        // clay, not when it arrived. It is a Release 1.6.1 block, so Release 1.6.4 legitimately contains it,
        // and claiming otherwise made the probe report a correct world as broken. It is deliberately NOT
        // listed here at all: this list only holds blocks later than EVERY declared version, and Release
        // 1.6.1 is inside the declared range. HistoricalBlockAvailability dates the terracotta family from
        // the 1.6.4 palette's own clayHardened / clayHardenedStained names instead.
        // Later-than-every-declared-profile content that the first draft of this list missed, each found by
        // asking what a Release 1.6.4 world could not have rather than by recalling a release list.
        later("1.7.2", "large_fern", "tall_grass", "packed_ice", "white_stained_glass");
        later("1.8", "white_banner", "slime_block", "prismarine", "prismarine_bricks", "dark_prismarine",
            "sea_lantern", "granite", "diorite", "andesite", "polished_granite", "polished_diorite",
            "polished_andesite", "coarse_dirt", "podzol", "iron_trapdoor", "spruce_fence_gate",
            "birch_fence_gate", "jungle_fence_gate", "dark_oak_fence_gate", "acacia_fence_gate");
        later("1.9", "dirt_path");
        later("1.20", "oak_hanging_sign", "spruce_hanging_sign", "birch_hanging_sign",
            "jungle_hanging_sign", "acacia_hanging_sign", "dark_oak_hanging_sign",
            "mangrove_hanging_sign", "cherry_hanging_sign", "bamboo_hanging_sign",
            "crimson_hanging_sign", "warped_hanging_sign", "pale_oak_hanging_sign");
        // The 1.8 blocks matter because Release 1.8.9 is a declared target: they are legitimate there and
        // anachronistic in everything older, so they are NOT listed here. The probe can compare against
        // the version under test; this list only covers what no declared version may contain.
    }

    private HistoricalBlockAnachronisms() {
    }

    /**
     * The release that introduced {@code blockId}, or null when the block is not known to be too new.
     *
     * <p>A null answer means "not on this list", never "historically correct" - see the class comment.
     */
    public static String introducedIn(String blockId) {
        if (blockId == null) {
            return null;
        }
        // Property suffixes are stripped so "minecraft:cave_vines[berries=true]" is still recognised.
        int bracket = blockId.indexOf('[');
        String bare = bracket >= 0 ? blockId.substring(0, bracket) : blockId;
        return INTRODUCED_IN.get(bare);
    }

    /** Every block this list treats as impossible in a declared historical version. */
    public static Map<String, String> all() {
        return java.util.Collections.unmodifiableMap(INTRODUCED_IN);
    }

    public static int size() {
        return INTRODUCED_IN.size();
    }
}

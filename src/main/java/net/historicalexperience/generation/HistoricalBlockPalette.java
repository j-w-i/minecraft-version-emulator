package net.historicalexperience.generation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Which blocks a version could have contained, and how a modern block id maps onto the version's own name.
 *
 * <h2>Why a mapping table and not a rename rule</h2>
 *
 * <p>The obvious implementation is to turn a modern id into camelCase and look it up. That is wrong in
 * exactly the places this project cares about, because the era boundary runs through the renames: modern
 * {@code minecraft:cobblestone} is Beta 1.7.3's {@code stonebrick}, while modern
 * {@code minecraft:stone_bricks} is Beta 1.8's {@code stoneBrick} and did not exist in 1.7.3 at all. A
 * rename rule would either accept stone bricks in Beta 1.7.3 or reject cobblestone. The same shape recurs
 * with {@code oreCoal} against {@code coal_ore}, and with {@code grass}/{@code short_grass}.
 *
 * <p>So the table is written by hand and reviewed, and every entry is a claim that can be checked against
 * the extracted palette.
 *
 * <h2>What this can and cannot say</h2>
 *
 * <ul>
 *   <li>mapped to a name in the palette - the version could have had this block;
 *   <li>mapped to a name NOT in the palette - the version could not have had it, and it is reported as a
 *       defect;
 *   <li>not in the table at all - reported as unknown and counted, <b>never</b> as acceptable. An
 *       unrecognised block is an unverified block, and the coverage count exists so that this stays
 *       visible instead of quietly passing.
 * </ul>
 *
 * <p>The palette comes from {@code research/beta173-block-palette.json}, whose parent review status is
 * {@code ACCEPTED_FOR_VERSION_CHECKS}, and a unit test holds this class to it so the two cannot drift.
 */
public final class HistoricalBlockPalette {
    /** Every block name the Beta 1.7.3 client registers, from the hash-verified jar. */
    public static final Set<String> BETA_1_7_3 = Set.of(
        "stone", "wood", "gravel", "grass", "cloth", "sand", "dirt", "stonebrick",
        "sapling", "bedrock", "water", "lava", "oreGold", "oreIron", "oreCoal", "log",
        "leaves", "sponge", "glass", "oreLapis", "blockLapis", "dispenser", "sandStone", "musicBlock",
        "bed", "goldenRail", "detectorRail", "pistonStickyBase", "web", "tallgrass", "deadbush", "pistonBase",
        "flower", "rose", "mushroom", "blockGold", "blockIron", "stoneSlab", "brick", "tnt",
        "bookshelf", "stoneMoss", "obsidian", "torch", "fire", "mobSpawner", "stairsWood", "chest",
        "redstoneDust", "oreDiamond", "blockDiamond", "workbench", "crops", "farmland", "furnace", "sign",
        "doorWood", "ladder", "rail", "stairsStone", "lever", "pressurePlate", "doorIron", "oreRedstone",
        "notGate", "button", "snow", "ice", "cactus", "clay", "reeds", "jukebox",
        "fence", "pumpkin", "hellrock", "hellsand", "lightgem", "portal", "litpumpkin", "cake",
        "diode", "lockedchest", "trapdoor");

    /** Modern block id -> the name the same block carried in this era. */
    private static final Map<String, String> MODERN_TO_LEGACY = new LinkedHashMap<>();

    private static void map(String legacyName, String... modernIds) {
        for (String modernId : modernIds) {
            String key = modernId.startsWith("minecraft:") ? modernId : "minecraft:" + modernId;
            // A duplicate would silently replace the earlier mapping, and two entries for one modern id
            // is how a correct block turns into a reported defect. Caught at class-initialisation, so it
            // cannot reach a world.
            String previous = MODERN_TO_LEGACY.put(key, legacyName);
            if (previous != null && !previous.equals(legacyName)) {
                throw new IllegalStateException("block " + key + " is mapped to both '" + previous
                    + "' and '" + legacyName + "'; one of them is wrong and it would decide whether the "
                    + "block is reported as an anachronism");
            }
        }
    }

    static {
        // Terrain.
        map("stone", "stone");
        map("grass", "grass_block");
        map("dirt", "dirt", "coarse_dirt", "rooted_dirt");
        map("bedrock", "bedrock");
        map("sand", "sand", "red_sand");
        map("sandStone", "sandstone", "smooth_sandstone", "cut_sandstone", "chiseled_sandstone");
        map("gravel", "gravel");
        map("clay", "clay");
        map("water", "water");
        map("lava", "lava");
        // THE TRAP: cobblestone was called stonebrick, and modern stone_bricks is a different, later block.
        map("stonebrick", "cobblestone");
        map("stoneMoss", "mossy_cobblestone");
        // Beta 1.7.3's cobblestone is "stonebrick" (one word, no second s) and Beta 1.8's stone bricks are
        // "stonebricksmooth". Names checked against research/block-palettes: the first is in the b1.7.3 set,
        // the second first appears in the b1.8.1 set. An earlier draft wrote "stoneBrick" here, which is in
        // NO extracted palette - it would have dated stone bricks to a version that never had the name and
        // then deleted them from worlds that did.
        map("stonebricksmooth", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks",
            "chiseled_stone_bricks");
        // Ores. b1.7.3 has exactly six, and the modern ids are in the opposite order.
        map("oreCoal", "coal_ore");
        map("oreIron", "iron_ore");
        map("oreGold", "gold_ore");
        map("oreDiamond", "diamond_ore");
        map("oreRedstone", "redstone_ore");
        map("oreLapis", "lapis_ore");
        // One log and one leaves block covered every tree species.
        map("log", "oak_log", "birch_log", "spruce_log", "jungle_log");
        map("leaves", "oak_leaves", "birch_leaves", "spruce_leaves", "jungle_leaves");
        map("sapling", "oak_sapling", "birch_sapling", "spruce_sapling", "jungle_sapling");
        map("wood", "oak_planks", "birch_planks", "spruce_planks", "jungle_planks");
        // Plants. Modern short_grass is the renamed tall grass, and poppy is the renamed rose. Beta 1.7.3
        // had ONE tall-grass block with metadata variants, so the single-height grass and the fern both
        // belong to it; the double-height forms are Beta 1.8's doublePlant and are mapped separately.
        map("tallgrass", "short_grass", "fern");
        map("flower", "dandelion");
        map("rose", "poppy");
        map("mushroom", "brown_mushroom", "red_mushroom");
        map("deadbush", "dead_bush");
        map("cactus", "cactus");
        map("reeds", "sugar_cane");
        map("pumpkin", "pumpkin");
        map("litpumpkin", "jack_o_lantern");
        // Built blocks.
        map("glass", "glass", "tinted_glass");
        map("brick", "bricks");
        map("tnt", "tnt");
        map("bookshelf", "bookshelf");
        map("obsidian", "obsidian");
        map("torch", "torch", "wall_torch");
        map("fire", "fire", "soul_fire");
        map("mobSpawner", "spawner");
        map("chest", "chest", "trapped_chest");
        map("workbench", "crafting_table");
        map("furnace", "furnace");
        map("dispenser", "dispenser");
        map("sign", "oak_sign", "oak_wall_sign");
        map("doorWood", "oak_door");
        map("doorIron", "iron_door");
        map("trapdoor", "oak_trapdoor");
        map("ladder", "ladder");
        map("rail", "rail");
        map("goldenRail", "powered_rail");
        map("detectorRail", "detector_rail");
        map("lever", "lever");
        map("button", "stone_button");
        map("pressurePlate", "stone_pressure_plate");
        map("notGate", "redstone_torch", "redstone_wall_torch");
        map("redstoneDust", "redstone_wire");
        map("diode", "repeater");
        map("stoneSlab", "stone_slab", "smooth_stone_slab");
        // Pre-1.3 there was ONE slab block whose metadata chose stone, sandstone, cobble or wood, so a
        // sandstone slab is the same registry name as a stone one. Without this entry the village's
        // sandstone slabs were UNJUDGEABLE rather than allowed, which the village test caught.
        map("stoneSlab", "sandstone_slab");
        // Sandstone stairs first appear in the 1.6.4 extraction; the name the era used is the one here.
        map("stairsSandStone", "sandstone_stairs");
        map("stairsWood", "oak_stairs");
        map("stairsStone", "cobblestone_stairs");
        map("farmland", "farmland");
        map("crops", "wheat");
        map("cake", "cake");
        map("jukebox", "jukebox");
        map("fence", "oak_fence");
        map("sponge", "sponge", "wet_sponge");
        map("cloth", "white_wool", "orange_wool", "magenta_wool", "light_blue_wool", "yellow_wool",
            "lime_wool", "pink_wool", "gray_wool", "light_gray_wool", "cyan_wool", "purple_wool",
            "blue_wool", "brown_wool", "green_wool", "red_wool", "black_wool");
        map("bed", "white_bed", "red_bed", "blue_bed", "black_bed");
        map("blockCoal", "coal_block");
        map("blockIron", "iron_block");
        map("blockGold", "gold_block");
        map("blockDiamond", "diamond_block");
        map("blockLapis", "lapis_block");
        map("musicBlock", "note_block");
        map("pistonBase", "piston", "moving_piston");
        map("pistonStickyBase", "sticky_piston");
        map("web", "cobweb");
        map("snow", "snow", "snow_block", "powder_snow");
        map("ice", "ice", "packed_ice", "blue_ice", "frosted_ice");
        map("hellrock", "netherrack");
        map("hellsand", "soul_sand", "soul_soil");
        map("lightgem", "glowstone");
        map("portal", "nether_portal");
        // NOT "lockedchest" for minecraft:chest: a chest is a chest, and mapping it to the unused locked
        // chest would have been a second mapping for the same modern id, silently replacing the first.
        // Later content, mapped so the check can say "this version could not have had it" BY NAME rather
        // than only through the version-agnostic list. The name on the right is the name the block had
        // when it was introduced, which is what makes its absence from the palette meaningful.
        map("vine", "vine");
        // The 1.7.2 flowers. Found by the coverage counter rather than by thinking of them: a census
        // reported minecraft:rose_bush as UNKNOWN, and an unknown block is an unverified block - rose
        // bushes are 1.7.2 content and therefore wrong in every version this project declares except the
        // Release 1.7.10 target. Listing them turns "we could not say" into "this version could not have
        // had it".
        map("roseBush", "rose_bush");
        map("peony", "peony");
        map("sunflower", "sunflower");
        map("lilac", "lilac");
        map("azureBluet", "azure_bluet");
        map("oxeyeDaisy", "oxeye_daisy");
        map("blueOrchid", "blue_orchid");
        map("allium", "allium");
        map("tulipRed", "red_tulip");
        map("tulipOrange", "orange_tulip");
        map("tulipWhite", "white_tulip");
        map("tulipPink", "pink_tulip");
        // Double-height plants are their own Beta 1.8 block, NOT the single tall grass that Beta 1.7.3
        // had. Mapping large_fern to tallgrass as well would have been a duplicate and would have made a
        // Beta 1.8 plant look legitimate.
        // Blocks named by the structures this project generates, so that a structure-placed block is
        // judgeable rather than merely unmapped. The village's church places a brewing stand from Beta 1.9
        // Prerelease 3, which is exactly what the b1.9-pre3 extraction registers under this name.
        // Lily pads. Found as the last UNJUDGED block by the per-profile runtime checks: Release 1.2.5 and
        // Release 1.6.4 worlds each reported five lily pads that no table could rule on. The b1.9-pre3
        // extraction registers the era name waterlily, which dates them to the Beta 1.9 Pre-release - where
        // the timeline puts them - so they are now allowed from there and refusable before it, instead of
        // merely unclassified.
        map("waterlily", "lily_pad");
        map("brewingStand", "brewing_stand");
        map("cobbleWall", "cobblestone_wall");
        map("doublePlant", "large_fern", "tall_grass");
        // Cave vines, weeping vines and twisting vines each get their OWN name rather than being folded
        // into "vine". Folding them together would claim they are the same block as vines, which is
        // false; giving each its own name keeps the claim to "this name is not in the palette", which is
        // the only thing the check is entitled to say.
        map("caveVines", "cave_vines", "cave_vines_plant", "glow_berries");
        map("weepingVines", "weeping_vines", "weeping_vines_plant");
        map("twistingVines", "twisting_vines", "twisting_vines_plant");
        map("melon", "melon", "melon_stem", "attached_melon_stem");
        map("pumpkinStem", "pumpkin_stem", "attached_pumpkin_stem");
        map("monsterStoneEgg", "infested_stone", "infested_cobblestone", "infested_stone_bricks",
            "infested_mossy_stone_bricks", "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks",
            "infested_deepslate");
        map("thinGlass", "glass_pane");
        // Same class of correction as stone bricks: these are the names the era's own registry used, taken
        // from the b1.8.1 palette, not modernised spellings that appear in no palette at all.
        map("fenceIron", "iron_bars");
        map("stairsBrick", "brick_stairs");
        map("stairsStoneBrickSmooth", "stone_brick_stairs");
        map("fenceGate", "oak_fence_gate");
        map("mushroomCapBrown", "brown_mushroom_block");
        map("mushroomCapRed", "red_mushroom_block");
        map("leaf_litter", "leaf_litter");
        map("firefly_bush", "firefly_bush");
        map("wildflowers", "wildflowers");
        // Bee nests are written by a tree decorator Moderner Beta attaches to its trees, so they need a
        // mapping or the suppression hook cannot recognise them - found by a census reporting bee_nest in
        // a Beta 1.7.3 world, where it is 1.16 content.
        map("beeNest", "bee_nest");
        map("beehive", "beehive");
        map("oreEmerald", "emerald_ore", "deepslate_emerald_ore");
        map("grassPath", "dirt_path");
        map("seaLantern", "sea_lantern", "prismarine", "prismarine_bricks", "dark_prismarine");
        map("kelp", "kelp", "kelp_plant");
        map("seagrass", "seagrass", "tall_seagrass");
        map("slimeBlock", "slime_block");
        // Hardened clay is the UNCOLOURED terracotta; stained clay is the coloured set. Both are Release
        // 1.6.1 blocks and the 1.6.4 palette registers them as clayHardened and clayHardenedStained, which
        // is why those are the names here. An earlier draft wrote hardenedClay/stainedClay, which appear in
        // no palette and would have left the whole terracotta family undated.
        map("clayHardened", "terracotta");
        map("clayHardenedStained", "white_terracotta", "orange_terracotta", "magenta_terracotta",
            "light_blue_terracotta", "yellow_terracotta", "lime_terracotta", "pink_terracotta",
            "gray_terracotta", "light_gray_terracotta", "cyan_terracotta", "purple_terracotta",
            "blue_terracotta", "brown_terracotta", "green_terracotta", "red_terracotta",
            "black_terracotta");
        map("stainedGlass", "white_stained_glass");
        // The wool carpet is Release 1.6.1 and the 1.6.4 palette spells it woolCarpet. The moss carpets are
        // NOT folded in with it: they are 1.17 and 1.21.4 content, so mapping them to an era name would make
        // them look legitimate in a Release 1.6.4 world.
        map("woolCarpet", "white_carpet");
        map("mossCarpet", "moss_carpet");
        map("paleMossCarpet", "pale_moss_carpet");
        map("banner", "white_banner");
        // NOT "standingSign" for oak_sign: b1.7.3's sign block is "sign" and it is in the palette, so
        // re-mapping the same modern id to a later name would have made every sign an anachronism.
        map("hangingSign", "oak_hanging_sign");
    }

    private HistoricalBlockPalette() {
    }

    /** The palette for a version, or null when no palette has been extracted for it yet. */
    public static Set<String> paletteFor(String versionId) {
        if ("historical_experience:beta_1_7_3".equals(versionId)) {
            return BETA_1_7_3;
        }
        // Deliberately null rather than a guess. Only Beta 1.7.3's palette has been extracted from a
        // hash-verified jar, and answering for another version by borrowing its neighbour's palette is
        // exactly the kind of plausible substitution this project keeps having to undo.
        return null;
    }

    /** What a check can conclude about one block. */
    public enum Verdict {
        /** The version's palette has this block. */
        ALLOWED,
        /** The version's palette does not have it, and the mapping says which block it is. */
        ANACHRONISTIC,
        /** Not in the mapping table, so nothing can be said. Counted, never treated as acceptable. */
        UNKNOWN
    }

    /** The legacy name this modern block carried, or null when the table does not know it. */
    public static String legacyNameFor(String modernBlockId) {
        if (modernBlockId == null) {
            return null;
        }
        int bracket = modernBlockId.indexOf('[');
        String bare = bracket >= 0 ? modernBlockId.substring(0, bracket) : modernBlockId;
        return MODERN_TO_LEGACY.get(bare);
    }

    /** Judges one block against a version's palette. */
    public static Verdict judge(Set<String> palette, String modernBlockId) {
        String legacy = legacyNameFor(modernBlockId);
        if (legacy == null) {
            return Verdict.UNKNOWN;
        }
        return palette.contains(legacy) ? Verdict.ALLOWED : Verdict.ANACHRONISTIC;
    }

    public static int mappedBlockCount() {
        return MODERN_TO_LEGACY.size();
    }

    public static Map<String, String> mapping() {
        return java.util.Collections.unmodifiableMap(MODERN_TO_LEGACY);
    }
}

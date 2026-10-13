package net.historicalexperience.generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The researched, generation-time content restrictions for a historical generation revision.
 *
 * <p>Why a hook exists at all: these features are attached to biomes by Moderner Beta's Java biome
 * builders ({@code level/biome/ModernBetaBiomeFeatures.java}), which call vanilla helpers and set
 * compile-time flags. A biome's feature list is global registry-adjacent data, so no datapack, world
 * preset or settings component can restrict it <em>for one world</em>. That is precisely the case
 * the architecture allows a narrowly scoped hook for.
 *
 * <p>What this is not: it never places modern content and then removes it. A skipped feature returns
 * "nothing was placed", and because {@code ChunkGenerator.applyBiomeDecoration} reseeds the shared
 * random per feature ({@code random.setFeatureSeed(decorationSeed, index, step)}), omitting one
 * feature does not perturb any other feature's output. The decision is derived from the generator's
 * own durable marker, so it is per-world by construction: an ordinary world, a plain Moderner Beta
 * world, or a world with no marker is never affected, and no context has to be threaded through
 * chunk generation or cleaned up on an exception path.
 *
 * <h2>Restrictions are bound to a generation revision, not to "historical" in general</h2>
 *
 * <p>{@link #BY_REVISION} is an <b>append-only</b> table keyed by the owned raw settings alias a
 * world persists. A world created as revision 1, 2 or 3 recorded a recipe that had no content
 * exclusions, and it must keep generating exactly that recipe: attaching a new restriction to an
 * existing alias would silently change what an existing world means, which is the specific failure
 * the revision scheme exists to prevent. A newly researched restriction therefore ships as a new
 * revision, and only that new alias selects it. Rows are never edited or re-pointed.
 *
 * <p>Each entry carries the exact call site that adds it. Entries are only added when the feature
 * demonstrably did not exist in Beta 1.7.3 <em>and</em> a call site actually adds it to a Beta
 * biome; merely being modern is not enough, because some modern features stand in for content Beta
 * did have.
 */
public final class HistoricalFeatureExclusions {
    private static final Logger LOGGER = Logger.getLogger("historical_experience");

    /**
     * One excluded placed feature and the evidence for excluding it.
     *
     * @param featureId the placed feature registry identifier
     * @param reason    why Beta 1.7.3 cannot have it, and which call site adds it
     */
    public record Exclusion(String featureId, String reason) {
    }

    /**
     * Moderner Beta's Beta biomes reach these through two vanilla helpers:
     * {@code BiomeDefaultFeatures.addDefaultOres} and {@code addDefaultCrystalFormations} via
     * {@code ModernBetaBiomeFeatures.addDefaultFeatures}, and
     * {@code ModernBetaBiomeFeatures.addMineables} with {@code BETA_ADD_ALTERNATE_STONES = true} and
     * {@code BETA_ADD_NEW_MINEABLES = true}.
     *
     * <p>Deliberately NOT listed, and why:
     * <ul>
     *   <li>coal, iron, gold, redstone, diamond and lapis - Beta 1.7.3 <em>did</em> have these. Their
     *       <b>distributions</b> are still vanilla's and are a separate, unimplemented fidelity task;
     *       excluding them would not make the world more historical, it would remove the ores.</li>
     *   <li>{@code moderner_beta:ore_emerald_y95} - Moderner Beta adds this deliberately, and emerald
     *       ore only exists from Release 1.3.1 onward. Whether Moderner Beta's {@code beta} preset
     *       models 1.7.3 or a later Beta-era game is unresolved, so this is recorded as an open
     *       fidelity question rather than silently decided either way.</li>
     *   <li>dungeons, dirt, gravel, clay, lakes, springs, lava and the Beta carvers - Beta 1.7.3
     *       content or Moderner Beta's own historical implementations.</li>
     * </ul>
     */
    /**
     * The Nether content that arrived in 1.16 and belongs in NO version this project declares.
     *
     * <p>Found by the owner looking at an Alpha 1.2.0 Nether and asking whether it was correct. It largely is -
     * one biome, no crimson or warped forest, no fortresses - but vanilla's own {@code nether_wastes} feature list
     * still places five things no historical version had:
     *
     * <pre>
     *   step 7: ... ore_blackstone, ore_gold_nether, ore_ancient_debris_large, ore_debris_small
     *   step 9: ... patch_soul_fire
     * </pre>
     *
     * <p>Ancient debris in a Beta world is the most obvious of these, and nether gold ore is 1.16 too. Soul fire is
     * 1.16. Blackstone and its ore are 1.16. So they are excluded for every declared version.
     *
     * <p>Nether QUARTZ is deliberately NOT here: it is Release 1.5 content and the world-preset generator already
     * gates the quartz-bearing Nether biome on that boundary, which is a per-version distinction this flat list
     * could not express.
     *
     * <p>This list is prepended to {@link #BETA_1_7_3_RESTRICTIONS}, which is the root of every other list in this
     * class - Beta 1.0 through 1.7.3 use it directly, and Alpha and Infdev reach it through
     * {@link #BEFORE_GRASS_EXISTED}. Adding it in one place therefore covers all 29 declared versions rather than
     * needing an edit per entry.
     */
    public static final List<Exclusion> MODERN_NETHER_CONTENT = List.of(
        new Exclusion("minecraft:ore_ancient_debris_large",
            "Ancient debris is 1.16 content and no declared version had it"),
        new Exclusion("minecraft:ore_debris_small",
            "Ancient debris is 1.16 content and no declared version had it"),
        new Exclusion("minecraft:ore_blackstone",
            "Blackstone is 1.16 content and no declared version had it"),
        new Exclusion("minecraft:ore_gold_nether",
            "Nether gold ore is 1.16 content; the era's Nether ores are quartz and the fortress loot"),
        new Exclusion("minecraft:patch_soul_fire",
            "Soul fire is 1.16 content; soul sand existed, soul fire did not"),
        // The last anachronism in vanilla's nether_wastes feature list, found by ENUMERATING that list rather than
        // by guessing at block names. Every other entry in it - the springs, patch_fire, the two glowstone features,
        // the nether and overworld mushrooms, and ore_gravel_nether - is Alpha 1.2.0 content or earlier, and nether
        // quartz is Release 1.5 content gated by which biome variant a world preset names.
        //
        // The rest of the 1.16 Nether - basalt, soul soil, blackstone, crimson and warped wood, shroomlight, roots,
        // vines, crying obsidian, respawn anchors, chains - is deliberately NOT listed: their features belong to the
        // crimson forest, warped forest, basalt deltas and soul sand valley BIOMES, and historical presets generate
        // the Nether as a single nether_wastes biome, so those features never run.
        //
        // The 1.16 Nether MOBS need no entry here at all: piglin, hoglin, zoglin, piglin brute and strider appear in
        // no historical spawn set, so the spawn filter already removes them. Endermen DO belong, from Beta 1.8.
        new Exclusion("minecraft:ore_magma",
            "Magma BLOCKS are 1.10 content, so no declared version placed them"));

    public static final List<Exclusion> BETA_1_7_3_RESTRICTIONS = List.copyOf(
        java.util.stream.Stream.concat(
            MODERN_NETHER_CONTENT.stream(),
            java.util.stream.Stream.of(
        new Exclusion("minecraft:ore_copper",
            "Copper ore is 1.17 content; added by BiomeDefaultFeatures.addDefaultOres"),
        new Exclusion("minecraft:amethyst_geode",
            "Amethyst geodes are 1.17 content; added by addDefaultCrystalFormations"),
        new Exclusion("minecraft:underwater_magma",
            "Magma blocks are 1.10 content; added by addDefaultOres via CavePlacements.UNDERWATER_MAGMA"),
        new Exclusion("minecraft:ore_granite_lower",
            "Granite is 1.8 content; added by addMineables' BETA_ADD_ALTERNATE_STONES"),
        new Exclusion("minecraft:ore_diorite_lower",
            "Diorite is 1.8 content; added by addMineables' BETA_ADD_ALTERNATE_STONES"),
        new Exclusion("minecraft:ore_andesite_lower",
            "Andesite is 1.8 content; added by addMineables' BETA_ADD_ALTERNATE_STONES"),
        new Exclusion("minecraft:ore_tuff",
            "Tuff is 1.17 content; added by addMineables' BETA_ADD_NEW_MINEABLES"),
        new Exclusion("minecraft:glow_lichen",
            "Glow lichen is 1.17 content; added by addMineables' BETA_ADD_NEW_MINEABLES"),
        // NETHER QUARTZ belongs HERE, at the root, and NOT at the Beta 1.9 Prerelease 3 level where I first put it.
        //
        // Quartz ore is Release 1.5 (13w01a) content, so every version before 1.5 must exclude it - while 1.5 and
        // 1.6.4 must NOT, because it is content they really had. That is a PREFIX of the version list, and this
        // chain is linear, so an exclusion added partway down covers a SUFFIX and silently misses everything above
        // it. At Prerelease 3 it therefore covered Pre3 onward and left Beta 1.7.3 and Beta 1.8.1 - the two era
        // profiles the owner actually opens - still generating quartz. They reported it twice.
        //
        // In the root it reaches every version, and RELEASE_1_5_WITHOUT_MODERN_VANILLA_FEATURES filters this exact
        // id back out for 1.5 and 1.6.4. That is the same shape the desert well already uses, and it is the only
        // arrangement a linear chain offers for expressing "before Release 1.5".
        new Exclusion("minecraft:ore_quartz_nether",
            "Nether quartz ore is Release 1.5 (13w01a) content, and vanilla's nether_wastes biome places it as a "
                + "feature, so every version before 1.5 would generate it")
        )).toList()
    );

    /**
     * Revision 5 additionally removes every vanilla ore feature, because that revision places
     * Beta 1.7.3's own ores itself ({@link BetaOreRules}).
     *
     * <p>Both halves are needed: excluding the vanilla features without placing Beta's would leave the
     * world with no ores at all, and placing Beta's without excluding vanilla's would roughly double
     * every ore count. Each entry is therefore listed with the helper that adds it, and the list covers
     * exactly the ore placements Moderner Beta's Beta biomes pull in:
     * {@code addDefaultOres}, {@code addMineables} and Moderner Beta's own emerald.
     *
     * <p>{@code moderner_beta:ore_emerald_y95} is excluded on primary evidence, not on suspicion: the
     * Beta 1.7.3 block registry contains six ores and no emerald ore block, and no {@code oreEmerald}
     * string exists anywhere in that client, even though an unused <em>item</em> named emerald does.
     */
    public static final List<Exclusion> BETA_1_7_3_WITH_REPLACED_ORES = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_RESTRICTIONS.stream(),
            java.util.stream.Stream.of(
                new Exclusion("minecraft:ore_coal_upper",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_coal_lower",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_iron_upper",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_iron_middle",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_iron_small",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_gold",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_gold_lower",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_redstone",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_redstone_lower",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_diamond",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_diamond_medium",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_diamond_large",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_diamond_buried",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_lapis",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_lapis_buried",
                    "Vanilla ore placement, replaced by Beta's own population in this revision"),
                new Exclusion("minecraft:ore_dirt",
                    "Vanilla placement, replaced by Beta's own dirt population in this revision"),
                new Exclusion("minecraft:ore_gravel",
                    "Vanilla placement, replaced by Beta's own gravel population in this revision"),
                new Exclusion("moderner_beta:ore_emerald_y95",
                    "Emerald ore did not exist in Beta 1.7.3: the client's block registry has exactly "
                        + "six ores and no emerald ore block, and no oreEmerald string exists in it")
            )).toList()
    );

    /**
     * Beta 1.7.3's restrictions plus the removal of the 1.13 aquatic plants.
     *
     * <p>Found by measurement, not by reading: judging every block a generated world contains against a
     * list of blocks no declared version can hold flagged <b>2,712</b> of them in a 169-chunk sample of a
     * Beta 1.7.3 world - kelp, kelp plants, seagrass and tall seagrass, all 1.13 content. Beta 1.7.3's
     * oceans were water, sand, gravel and clay with nothing growing in them.
     *
     * <p>This is a NEW set rather than an addition to {@link #BETA_1_7_3_WITH_REPLACED_ORES}, because
     * revisions 4, 5 and 6 already reference that one and worlds exist on them: adding to it would change
     * what those recorded recipes generate, which is the thing the append-only rule protects. The
     * exclusion mechanism itself is unchanged - these are the same generation-time placed-feature
     * exclusions, so nothing is generated and then removed.
     */
    public static final List<Exclusion> BETA_1_7_3_WITHOUT_AQUATIC_PLANTS = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITH_REPLACED_ORES.stream(),
            java.util.stream.Stream.of(
                aquatic("minecraft:kelp_cold"),
                aquatic("minecraft:kelp_warm"),
                aquatic("minecraft:seagrass_cold"),
                aquatic("minecraft:seagrass_deep"),
                aquatic("minecraft:seagrass_deep_cold"),
                aquatic("minecraft:seagrass_deep_warm"),
                aquatic("minecraft:seagrass_normal"),
                aquatic("minecraft:seagrass_river"),
                aquatic("minecraft:seagrass_swamp"),
                aquatic("minecraft:seagrass_warm")
            )).toList()
    );

    private static Exclusion aquatic(String placedFeatureId) {
        return new Exclusion(placedFeatureId,
            "Seagrass and kelp are 1.13 content. Beta 1.7.3's oceans held nothing that grew: found by a "
                + "block census of a generated world, which counted these in the thousands");
    }

    /**
     * Beta 1.7.3's restrictions, the aquatic plants, and the 1.21.5 firefly bushes.
     *
     * <p>The firefly bushes came out of the same census that found the seagrass: a 289-chunk world held
     * seven of them, and they are 1.21.5 content. Excluding them is safe because each is placed by its
     * own feature and nothing else places them.
     *
     * <p><b>Leaf litter is deliberately NOT excluded, and is left as a visible failure.</b> Its 1,769
     * blocks are placed as a decorator on the tree features themselves, so excluding it would delete the
     * birch and dark oak trees rather than the litter. That needs a replacement feature, the way the ore
     * population replaced vanilla's placement, and until then the census reports it.
     */
    public static final List<Exclusion> BETA_1_7_3_WITHOUT_MODERN_PLANTS = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITHOUT_AQUATIC_PLANTS.stream(),
            java.util.stream.Stream.of(
                new Exclusion("minecraft:patch_firefly_bush_near_water",
                    "Firefly bushes are 1.21.5 content; found by a block census of a generated world"),
                new Exclusion("minecraft:patch_firefly_bush_near_water_swamp",
                    "Firefly bushes are 1.21.5 content; found by a block census of a generated world"),
                new Exclusion("minecraft:patch_firefly_bush_swamp",
                    "Firefly bushes are 1.21.5 content; found by a block census of a generated world")
            )).toList()
    );

    /**
     * Every remaining vanilla placed feature that Moderner Beta adds to a Beta-era biome and that is too
     * new for Beta 1.7.3.
     *
     * <p>Found by reading Moderner Beta's own biome-feature code rather than by censusing worlds one at a
     * time, which is what the previous rounds had to do and why they kept finding one anachronism per run.
     * That code lists every vanilla feature it adds, so the remaining defects can be enumerated instead of
     * discovered: {@code FOREST_FLOWERS}, {@code FLOWER_PLAINS} and {@code FLOWER_SWAMP} place the 1.7.2
     * and 1.14 flowers (rose bush, peony, lilac, lily of the valley, cornflower); {@code PATCH_MELON_SPARSE}
     * places melons, which are Beta 1.8; {@code SEA_PICKLE} and {@code WARM_OCEAN_VEGETATION} are 1.13;
     * and {@code PATCH_WATERLILY} is later than Beta 1.7.3 as well.
     *
     * <p>Removing the vanilla flower features costs nothing historical, because Moderner Beta places its
     * own dandelions and poppies in the same biomes - which is exactly the era-correct content.
     *
     * <p><b>Two anachronisms are NOT here, because exclusion cannot express them.</b> Leaf litter and vines
     * are placed from <i>inside</i> the tree features: leaf litter because on 26.3 Moderner Beta
     * deliberately substitutes {@code TreePlacements.OAK_BEES_0002_LEAF_LITTER} for the pre-1.21.5 tree
     * placement, and vines because the vanilla swamp tree carries them. Excluding either feature deletes
     * the trees. They need generation-time suppression of the block, and until that exists the census
     * reports them.
     */
    public static final List<Exclusion> BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITHOUT_MODERN_PLANTS.stream(),
            java.util.stream.Stream.of(
                new Exclusion("minecraft:forest_flowers",
                    "Beta 1.7.3 had dandelions and roses, which Moderner Beta places itself; the vanilla "
                        + "forest flowers add rose bushes, peonies, lilacs and lilies of the valley, all "
                        + "1.7.2 or later"),
                new Exclusion("minecraft:flower_plains",
                    "Vanilla plains flowers include 1.7.2 and 1.14 species; Moderner Beta places its own "
                        + "dandelions and poppies for this era"),
                new Exclusion("minecraft:flower_swamp",
                    "Vanilla swamp flowers include 1.14 species such as the lily of the valley"),
                new Exclusion("minecraft:patch_melon_sparse",
                    "Melons are Beta 1.8 content and did not exist in Beta 1.7.3"),
                new Exclusion("minecraft:sea_pickle",
                    "Sea pickles are 1.13 content"),
                new Exclusion("minecraft:warm_ocean_vegetation",
                    "Warm-ocean vegetation is 1.13 content and places coral and seagrass"),
                new Exclusion("minecraft:patch_waterlily",
                    "Lily pads arrive after Beta 1.7.3 (Beta 1.9 prerelease at the earliest)")
            )).toList()
    );

    /**
     * Everything above, plus the removal of grass itself for versions that predate it.
     *
     * <p><b>Grass was added in Beta 1.6</b> (Test Build 3), which the Minecraft Wiki records as
     * {@code |1.6|dev=Test Build 3| Added grass.} on the Short Grass page, where the block's pre-1.13 id is
     * 31 with the name {@code tallgrass} - the same name this project extracted from the Beta 1.7.3 client.
     * So Alpha and Infdev, which have no grass at all, must not place it.
     *
     * <p>Moderner Beta does place it there: its Alpha and Infdev 611 biomes add
     * {@code ModernBetaVegetationPlacedFeatures.PATCH_GRASS_ALPHA_2}, whose registry id is
     * {@code moderner_beta:patch_grass_alpha_2}. That is a real anachronism in a real world, and it was
     * visible in play before it was measured.
     *
     * <p>The rest of the set is the same one Beta 1.7.3 uses, and every entry in it is later than Alpha
     * too - the 1.13 aquatic plants, the 1.7.2 and 1.14 flowers, melons, sea pickles, warm-ocean
     * vegetation, water lilies and the 1.21.5 firefly bushes.
     */
    public static final List<Exclusion> BEFORE_GRASS_EXISTED = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES.stream(),
            java.util.stream.Stream.of(
                new Exclusion("moderner_beta:patch_grass_alpha_2",
                    "Grass was added in Beta 1.6, so Alpha and Infdev had none; Moderner Beta places it "
                        + "in those versions' biomes")
            )).toList()
    );

    /**
     * {@link #BEFORE_GRASS_EXISTED} plus the desert well, which is Release 1.2.1 (12w04a).
     *
     * <p>The well is a PLACED FEATURE on 1.21.1, not a structure, so the structure gate cannot express it and
     * it belongs here.
     */
    public static final List<Exclusion> BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL = List.copyOf(
        java.util.stream.Stream.concat(
            BEFORE_GRASS_EXISTED.stream(),
            java.util.stream.Stream.of(desertWell("Alpha and both Infdev builds"))
        ).toList()
    );

    /**
     * The above, plus every block the research dates to Alpha 1.0.11, for the builds before it.
     *
     * <p>Alpha 1.0.11 introduces clay, sugar cane, paper, books, bricks and slimes. This list exists because the
     * block-level suppression path cannot reach these: that path decides from a version's extracted block palette
     * and its own documentation says "a version with no extracted palette makes no claim at all" - which is every
     * Alpha build - and vanilla PLACED FEATURES are not that path either.
     *
     * <p>Clay and sugar cane were both found by playing a real Alpha 1.0.1 world rather than by reading anything.
     * The dates were already in the availability table - one entry names them together, "clay block and sugar cane
     * introduced Alpha v1.0.11" - but a date is a claim, not a filter, and nothing was enforcing it here.
     *
     * <p>Named for the boundary rather than for one block, because it will grow: 1.0.1, 1.0.4 and 1.0.6 all predate
     * all of it, and Alpha v1.1.2_01 has all of it.
     */
    public static final List<Exclusion> ALPHA_BEFORE_1_0_11_CONTENT = List.copyOf(
        java.util.stream.Stream.concat(
            BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL.stream(),
            java.util.stream.Stream.of(
                // Clay: four placed features can produce it.
                new Exclusion("moderner_beta:ore_clay",
                    "Clay arrives in Alpha 1.0.11; observed generating in an Alpha 1.0.1 world"),
                new Exclusion("minecraft:ore_clay",
                    "Clay arrives in Alpha 1.0.11, and this build predates it"),
                new Exclusion("minecraft:disk_clay",
                    "Clay arrives in Alpha 1.0.11, and this build predates it"),
                new Exclusion("lush_caves_clay",
                    "Clay arrives in Alpha 1.0.11, and this build predates it"),
                // Sugar cane, which the same availability entry dates to the same build. Four vanilla features
                // place it, and the report was "i can still see sugar cane which wasn't added until later".
                new Exclusion("minecraft:patch_sugar_cane",
                    "Sugar cane arrives in Alpha 1.0.11; observed in an Alpha 1.0.1 world"),
                new Exclusion("minecraft:patch_sugar_cane_desert",
                    "Sugar cane arrives in Alpha 1.0.11, and this build predates it"),
                new Exclusion("minecraft:patch_sugar_cane_swamp",
                    "Sugar cane arrives in Alpha 1.0.11, and this build predates it"),
                new Exclusion("minecraft:patch_sugar_cane_badlands",
                    "Sugar cane arrives in Alpha 1.0.11, and this build predates it"))
        ).toList()
    );

    /**
     * The above, plus the monster room, for the two Infdev builds that predate it.
     *
     * <p>Found by observing a real world rather than by reading the research: an Infdev 20100420 sample
     * contained {@code minecraft:spawner} and chests, so a dungeon was generating in a build two months older
     * than Infdev 20100625-1917, which is when monster rooms arrive. The dungeon is a placed feature, so the
     * structure rules never had an opinion about it.
     */
    public static final List<Exclusion> INFDEV_WITHOUT_DUNGEONS = List.copyOf(
        java.util.stream.Stream.concat(
            BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL.stream(),
            java.util.stream.Stream.of(
                new Exclusion("minecraft:monster_room",
                    "Monster rooms arrive in Infdev 20100625-1917, which is after both Infdev builds this "
                        + "project declares; observed generating in a real 20100420 world"),
                new Exclusion("minecraft:monster_room_deep",
                    "The deepslate monster room is modern content and existed in no declared version")
            )).toList()
    );

    /** Beta 1.7.3's modern-feature set plus the desert well, which is Release 1.2.1. */
    public static final List<Exclusion> BETA_1_7_3_WITHOUT_DESERT_WELL = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES.stream(),
            java.util.stream.Stream.of(desertWell("Beta 1.7.3"))
        ).toList()
    );

    /**
     * The same modern-content exclusions, for the versions from Beta 1.8.1 onwards.
     *
     * <h2>Why this is a set of its own</h2>
     *
     * <p>Until this existed, every version after Beta 1.7.3 carried only
     * {@link #BETA_1_7_3_RESTRICTIONS} - the eight blocks that are later than every declared version - and
     * nothing else. Their generation therefore still received the 1.13 aquatic plants, the 1.7.2 and 1.14
     * flowers, sea pickles, warm-ocean vegetation, water lilies and the 1.21.5 firefly bushes. That was
     * measured rather than assumed: a Beta 1.8.1 world generated on the revision-1 recipe contained
     * <b>kelp 338, kelp plants 1,666, seagrass 1,301 and tall seagrass 1,212</b>, none of which is content
     * Beta 1.8.1 had.
     *
     * <h2>The one entry deliberately removed</h2>
     *
     * <p>{@code minecraft:patch_melon_sparse} is NOT excluded here. Melons were added in the Beta 1.8
     * Pre-release, so they are legitimate in every version this set applies to, and carrying the
     * Beta 1.7.3 exclusion forward would have deleted content the version really had. That is the reason
     * this is a derived set with an explicit filter rather than a copy.
     */
    public static final List<Exclusion> BETA_1_8_1_WITHOUT_MODERN_VANILLA_FEATURES = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES.stream()
                .filter(exclusion -> !"minecraft:patch_melon_sparse".equals(exclusion.featureId())),
            java.util.stream.Stream.of(desertWell("Beta 1.8.1"))
        ).toList()
    );

    /**
     * The Beta 1.9 Prerelease 3 set: as above, and lily pads are NOT excluded any more.
     *
     * <p>Lily pads arrive in the Beta 1.9 Pre-release, so {@code minecraft:patch_waterlily} is legitimate
     * from this version onwards and the Beta 1.8.1 exclusion is dropped rather than carried forward.
     */
    public static final List<Exclusion> BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES = List.copyOf(
        BETA_1_8_1_WITHOUT_MODERN_VANILLA_FEATURES.stream()
            .filter(exclusion -> !"minecraft:patch_waterlily".equals(exclusion.featureId()))
            .toList()
    );

    /**
     * Release 1.2.5, 1.3 and 1.4: nether quartz is still excluded, because it arrives in Release 1.5.
     *
     * <p>Declared BEFORE the 1.5 list, because that one is derived from this one and a Java static initialiser
     * cannot refer forward.
     */
    public static final List<Exclusion> RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES = List.copyOf(
        BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES.stream()
            .filter(exclusion -> !"minecraft:desert_well".equals(exclusion.featureId()))
            .toList()
    );

    /**
     * Release 1.5 and Release 1.6.4: nether quartz is content these versions really had.
     *
     * <p>Quartz ore arrives in Release 1.5 (13w01a), so excluding it from 1.5 onwards would delete content the
     * version genuinely had. The exclusion sits on the pre-1.5 list and is removed here, exactly as the desert well
     * is removed for Release 1.2.5.
     */
    public static final List<Exclusion> RELEASE_1_5_WITHOUT_MODERN_VANILLA_FEATURES = List.copyOf(
        RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES.stream()
            .filter(exclusion -> !"minecraft:ore_quartz_nether".equals(exclusion.featureId()))
            .toList()
    );

    private static Exclusion desertWell(String version) {
        return new Exclusion("minecraft:desert_well",
            "Desert wells are Release 1.2.1 (12w04a) content and this is a PLACED FEATURE on 1.21.1, not "
                + "a structure, so the structure rule cannot express it. " + version + " predates it");
    }

    private static final LongAdder SKIPPED = new LongAdder();
    private static final Set<String> UNRESOLVED = ConcurrentHashMap.newKeySet();

    private HistoricalFeatureExclusions() {
    }

    /**
     * The restrictions a persisted recipe alias selects.
     *
     * <p>Resolved through the version registry rather than a table here: the alias identifies the
     * version, and that version's rule set is the single declaration of what it excludes. An alias that
     * declares no exclusion - including revisions 1 to 3, which were generated without any - resolves
     * to an empty list.
     */
    public static List<Exclusion> forAlias(String rawAlias) {
        return net.historicalexperience.version.HistoricalVersions.byRevisionAlias(rawAlias)
            .flatMap(version -> version.revisions().stream()
                .filter(revision -> revision.alias().equals(rawAlias))
                .findFirst())
            .map(net.historicalexperience.version.HistoricalVersion.GenerationRevision
                ::excludedPlacedFeatures)
            .orElse(List.of());
    }

    /** Runtime translation is separate from the immutable version-registry recipe declaration. */
    public static List<Exclusion> runtimeExclusionsForAlias(String alias) {
        return adaptTo1211(forAlias(alias));
    }

    /** Translate the 26.3 recipe to the actual 1.21.1 feature registry, not to a new recipe. */
    private static List<Exclusion> adaptTo1211(List<Exclusion> declared) {
        java.util.ArrayList<Exclusion> result = new java.util.ArrayList<>();
        for (Exclusion exclusion : declared) {
            // These features were introduced in 1.21.5 and simply do not exist on this runtime.
            if (Set.of("minecraft:patch_firefly_bush_near_water",
                    "minecraft:patch_firefly_bush_near_water_swamp",
                    "minecraft:patch_firefly_bush_swamp").contains(exclusion.featureId())) {
                continue;
            }
            result.add(exclusion);
            // 1.21.1 has BOTH upper and lower alternate-stone placements; 26.3 only used lower.
            if (Set.of("minecraft:ore_granite_lower", "minecraft:ore_diorite_lower",
                    "minecraft:ore_andesite_lower").contains(exclusion.featureId())) {
                result.add(new Exclusion(exclusion.featureId().replace("_lower", "_upper"),
                    exclusion.reason() + "; the corresponding 1.21.1 upper placement"));
            }
        }
        return List.copyOf(result);
    }

    /** Every revision that currently selects restrictions, for diagnostics and tests. */
    public static Set<String> restrictedRevisions() {
        return net.historicalexperience.version.HistoricalVersions.all().stream()
            .flatMap(version -> version.revisions().stream())
            .filter(revision -> !revision.excludedPlacedFeatures().isEmpty())
            .map(net.historicalexperience.version.HistoricalVersion.GenerationRevision::alias)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * The revision alias this generator was created with, or {@code null} when the generator is not a
     * marker-carrying Moderner Beta generator.
     *
     * <p>Read straight from the generator's own stored settings rather than from any ambient state,
     * so the answer cannot be wrong for a concurrently generating neighbouring world and cannot leak
     * across threads or survive an exception.
     */
    public static String historicalAlias(ChunkGenerator generator) {
        if (!(generator instanceof mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator
                modernBeta)) {
            return null;
        }
        var settings = modernBeta.getChunkSettings();
        if (settings == null) {
            return null;
        }
        ResourceLocation preset =
            settings.get(mod.bluestaggo.modernerbeta.settings.SettingsComponentTypes.PRESET);
        if (preset == null) {
            return null;
        }
        String alias = preset.toString();
        if (DIAGNOSED_PRESETS.add(alias)) {
            LOGGER.log(Level.INFO, "generation hook: generator preset component is {0} "
                + "(recognised as a historical marker: {1})",
                new Object[] { alias, HistoricalMarker.isHistoricalMarker(alias) });
        }
        return HistoricalMarker.isHistoricalMarker(alias) ? alias : null;
    }

    /**
     * Whether this placed feature must not run in this world.
     *
     * <p>Identity comparison against the registry's own {@link PlacedFeature} instances: the
     * decoration loop takes its features from the same registry holders, so a resolved instance is
     * the instance being tested. Identifiers are only used to resolve, never to compare names, which
     * is what keeps a same-named entry from another addon from being silently filtered.
     *
     * <p>Resolution happens per call rather than through a cache. That is a handful of map lookups
     * per feature on restricted worlds only, which is far cheaper than the terrain work beside it, and
     * it cannot go stale when a datapack reload replaces the placed feature registry.
     */
    public static boolean shouldSkip(
        ChunkGenerator generator,
        WorldGenLevel level,
        PlacedFeature feature
    ) {
        String alias = historicalAlias(generator);
        if (alias == null) {
            return false;
        }
        List<Exclusion> restrictions = runtimeExclusionsForAlias(alias);
        if (restrictions.isEmpty()) {
            return false;
        }
        var lookup = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (Exclusion exclusion : restrictions) {
            ResourceLocation id = ResourceLocation.tryParse(exclusion.featureId());
            if (id == null) {
                recordUnresolved(exclusion.featureId());
                continue;
            }
            var holder = lookup.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, id));
            if (holder.isEmpty()) {
                recordUnresolved(exclusion.featureId());
                continue;
            }
            if (holder.get().value() == feature) {
                SKIPPED.increment();
                return true;
            }
        }
        return false;
    }

    /** How many feature placements this hook has suppressed since process start. */
    public static long skippedPlacements() {
        return SKIPPED.sum();
    }

    private static final LongAdder SUPPRESSED_BLOCKS = new LongAdder();
    /** Preset values already reported by the one-shot diagnostic below, so it logs once each. */
    private static final Set<String> DIAGNOSED_PRESETS = ConcurrentHashMap.newKeySet();
    private static final Set<String> SUPPRESSED_BLOCK_IDS = ConcurrentHashMap.newKeySet();

    /**
     * True when the active historical version could not have contained {@code state}'s block, so a tree
     * decorator must not write it.
     *
     * <p>This exists because two anachronisms are placed from <b>inside</b> tree features and cannot be
     * expressed as a placed-feature exclusion:
     *
     * <ul>
     *   <li><b>Leaf litter</b> is attached as a {@code PlaceOnGroundDecorator} on the tree, and on 26.3
     *       Moderner Beta deliberately substitutes {@code TreePlacements.OAK_BEES_0002_LEAF_LITTER} for the
     *       pre-1.21.5 placement. Excluding the feature would delete the forest's trees.</li>
     *   <li><b>Vines</b> come from the vanilla swamp tree's trunk and leaf decorators. Excluding the tree
     *       would delete the swamp's trees.</li>
     * </ul>
     *
     * <p>Both are therefore suppressed at the moment of placement, so the block is <b>never generated</b>
     * and there is no generate-then-remove step. The decision is made from the version's extracted block
     * palette, so it applies to exactly the versions that could not have had the block, and a version with
     * no extracted palette makes no claim at all.
     */
    public static boolean shouldSuppressBlock(WorldGenLevel level, BlockState state) {
        if (level == null || state == null) {
            return false;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return isAnachronisticForActiveVersion(level, blockId);
    }

    /**
     * True when the active historical version could not have had vines.
     *
     * <p>Vines need their own entry point because a decorator writes them through {@code placeVine}, which
     * builds the vine state internally rather than handing one over - so there is no {@code BlockState} to
     * inspect at the call site.
     */
    public static boolean shouldSuppressVines(WorldGenLevel level) {
        return level != null && isAnachronisticForActiveVersion(level, "minecraft:vine");
    }

    private static boolean isAnachronisticForActiveVersion(WorldGenLevel level, String blockId) {
        ChunkGenerator generator;
        try {
            generator = level.getLevel().getChunkSource().getGenerator();
        } catch (RuntimeException e) {
            // A level that cannot produce a generator is not a historical world we can judge.
            return false;
        }
        String alias = activeAlias(generator);
        if (alias == null) {
            return false;
        }
        String versionId = HistoricalVersions.byRevisionAlias(alias)
            .map(HistoricalVersion::id)
            .orElse(alias);
        if (!HistoricalBlockContent.isAnachronistic(versionId, blockId)) {
            return false;
        }
        SUPPRESSED_BLOCKS.increment();
        SUPPRESSED_BLOCK_IDS.add(blockId);
        return true;
    }

    /**
     * The historical recipe governing a generator, falling back to the running server's scope.
     *
     * <p>The fallback exists for dimensions whose level stem is an ordinary vanilla generator - the Nether
     * and the End of a historical world - and for hooks handed only a registry access. It is consulted
     * <b>only</b> when the generator carries no marker of its own, so a per-dimension answer always wins
     * over the per-server one.
     */
    public static String activeAlias(ChunkGenerator generator) {
        String alias = historicalAlias(generator);
        return alias != null ? alias : HistoricalGenerationScope.generationAlias();
    }

    /** How many individual blocks this hook has suppressed since process start. */
    public static long suppressedBlockCount() {
        return SUPPRESSED_BLOCKS.sum();
    }

    /** Which block ids have been suppressed, for the probe to report. */
    public static Set<String> suppressedBlockIds() {
        return java.util.Collections.unmodifiableSet(SUPPRESSED_BLOCK_IDS);
    }

    /**
     * Excluded identifiers that were not present in the live placed feature registry.
     *
     * <p>An unresolvable exclusion means a restriction silently did not apply, so this is reported
     * rather than swallowed: unreported, a renamed or removed vanilla feature would look exactly like
     * a correct world.
     */
    public static Set<String> unresolvedExclusions() {
        return Set.copyOf(UNRESOLVED);
    }

    /** Diagnostics as an ordered map, for verdict reporting. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("restrictedRevisions", restrictedRevisions().toString());
        report.put("skippedPlacements", Long.toString(SKIPPED.sum()));
        report.put("unresolvedExclusions", UNRESOLVED.toString());
        return report;
    }

    private static void recordUnresolved(String featureId) {
        if (UNRESOLVED.add(featureId)) {
            LOGGER.log(Level.WARNING,
                "Historical content exclusion {0} is not present in the placed feature registry, so "
                    + "that restriction did not apply; the world may contain content Beta 1.7.3 did "
                    + "not have",
                featureId);
        }
    }
}

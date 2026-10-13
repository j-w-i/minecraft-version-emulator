package net.historicalexperience.version;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.historicalexperience.HistoricalExperience;
import net.historicalexperience.generation.BetaOreRules;
import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.historicalexperience.gameplay.HistoricalLootRules;
import net.historicalexperience.gameplay.HistoricalSpawnRules;
import net.historicalexperience.version.HistoricalVersion.GenerationRevision;
import net.historicalexperience.version.HistoricalVersion.VersionRules;

/**
 * The registry of historical Minecraft versions this build knows about.
 *
 * <p><b>This is the single place a version is declared.</b> Adding a version means adding one entry
 * here, plus the two resource files its revisions need; every subsystem - markers, profile bindings,
 * generation-content exclusions, ore placement, natural spawning, mob drops, experience, sprinting and
 * hunger - reads its data through this registry instead of keeping its own table. The registry test
 * asserts that invariant, so a new version cannot quietly be declared in one subsystem only.
 *
 * <p>Version ids are plain strings on purpose. This class must not depend on the classes that consume
 * it: it is initialised from literal data, and the consumers initialise from it. Dependency in one
 * direction is what keeps static initialisation order safe.
 *
 * <p>Chronology is a sortable integer so questions like "did this behaviour exist at or after version
 * X" are answered by comparison rather than by a list of ids. The scale is documented in
 * {@link #chronology}.
 */
public final class HistoricalVersions {
    /** Era offsets for {@link #chronology}, so ordering never depends on how a version is named. */
    private static final int ERA_CLASSIC = 100_000;
    private static final int ERA_INDEV = 200_000;
    private static final int ERA_INFDEV = 300_000;
    private static final int ERA_ALPHA = 400_000;
    private static final int ERA_BETA = 500_000;
    private static final int ERA_RELEASE = 600_000;

    /**
     * Sortable chronology key: {@code eraOffset + major * 1000 + minor * 100 + patch}.
     *
     * <p>Beta 1.7.3 is 501703 and Beta 1.8.1 is 501801, so a feature introduced in Beta 1.8 is present
     * in every version whose key is at least 501800.
     */
    public static int chronology(int eraOffset, int major, int minor, int patch) {
        return eraOffset + major * 1000 + minor * 100 + patch;
    }

    private static final List<HistoricalVersion> ALL;

    static {
        List<HistoricalVersion> versions = new java.util.ArrayList<>();

        versions.add(new HistoricalVersion(
            HistoricalExperience.Profiles.BETA_1_7_3,
            "Beta 1.7.3",
            "beta",
            chronology(ERA_BETA, 1, 7, 3),
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_7_3_MOBS,
                HistoricalLootRules.BETA_1_7_3_BY_ENTITY,
                // Direct food healing, because this version has no hunger system: eating restored health
                // instead of feeding a food level. Values read as literals from the same client.
                HistoricalFoodRules.BETA_1_7_3_HEAL_POINTS,
                false,
                false,
                false,
                // Beta 1.7.3 has no structures at all: strongholds, mineshafts and villages all arrive
                // in Beta 1.8.1.
                false,
                "Entity registry, item registry and ore parameters/vein verified from the b1.7.3 client "
                    + "(sha1 43db9b498cb67058d2e12d394e6507722e71bb45); see "
                    + "research/beta173-entity-types.json, research/beta173-ore-parameters-parent-reviewed.json "
                    + "and research/beta173-ore-vein-geometry.json"),
            List.of(
                // Revisions 0 to 3 declare NO generation-content restrictions, exactly as they were
                // created. That is what keeps already-generated worlds generating what their recorded
                // recipe said, and it is why these rules live on the revision rather than the version.
                new GenerationRevision(0, HistoricalExperience.GenerationPresets.HISTORICAL_BETA_1_7_3,
                    HistoricalExperience.GenerationPresets.MODERNER_BETA_BETA_1_7_3, false,
                    "frozen original declaration: reference-only alias on the vanilla level envelope "
                        + "with vanilla ore placement",
                    java.util.List.of(), java.util.List.of()),
                new GenerationRevision(1, alias("beta_1_7_3_content_preview_1"),
                    "moderner_beta:beta", true,
                    "deepslate stone layer disabled and cave-biome injection removed",
                    java.util.List.of(), java.util.List.of()),
                new GenerationRevision(2, alias("beta_1_7_3_content_preview_2"),
                    "moderner_beta:beta", true,
                    "reduced level envelope via the dimension type (min_y 0)",
                    java.util.List.of(), java.util.List.of()),
                new GenerationRevision(3, alias("beta_1_7_3_content_preview_3"),
                    "moderner_beta:beta", true,
                    "reduced noise envelope too, which restores the bedrock column",
                    java.util.List.of(), java.util.List.of()),
                new GenerationRevision(4, alias("beta_1_7_3_content_preview_4"),
                    "moderner_beta:beta", true,
                    "first generation-time content restrictions: eight anachronistic placed features",
                    HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS,
                    java.util.List.of()),
                new GenerationRevision(5, alias("beta_1_7_3_content_preview_5"),
                    "moderner_beta:beta", true,
                    "Beta 1.7.3's own ore population, with the vanilla ore features excluded",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITH_REPLACED_ORES,
                    BetaOreRules.BETA_1_7_3),
                // Revision 6: the cave biomes. Moderner Beta's Beta preset assigns cave biomes through a
                // voronoi diagram whose points name MODERN cave biomes - the preset literally lists
                // minecraft:lush_caves and minecraft:sulfur_* - so a Beta 1.7.3 world was being carved
                // with 1.18-era cave biomes. The previous revisions set the provider to
                // `moderner_beta:none`, which is not a fix either: CaveBiomeProviderNone returns a null
                // biome. Cave biomes did not exist before 1.13, so the historical choice is
                // ONE era-appropriate biome for all caves, which is what this revision declares. It is a
                // new revision rather than an edit because revisions are append-only: worlds already
                // exist on revisions 0 to 5 and their recorded recipe has to keep meaning what it said.
                new GenerationRevision(6, alias("beta_1_7_3_content_preview_6"),
                    "moderner_beta:beta", true,
                    "cave biomes: modern voronoi cave biomes replaced by a single era-appropriate biome",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITH_REPLACED_ORES,
                    BetaOreRules.BETA_1_7_3),
                // Revision 7: the 1.13 aquatic plants. A block census of a generated world, judged
                // against a list of blocks no declared version can contain, found 2,712 of them - kelp,
                // kelp plants, seagrass and tall seagrass - in 169 chunks of a Beta 1.7.3 world. Beta
                // 1.7.3's oceans were water, sand, gravel and clay with nothing growing in them. The ten
                // placed features are excluded at generation time, which is the same mechanism the other
                // restrictions use: nothing is generated and then removed.
                new GenerationRevision(7, alias("beta_1_7_3_content_preview_7"),
                    "moderner_beta:beta", true,
                    "1.13 aquatic plants (kelp and seagrass) excluded; found by a block census",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_AQUATIC_PLANTS,
                    BetaOreRules.BETA_1_7_3),
                // Revision 8: the 1.21.5 firefly bushes, found by the same census that found the seagrass
                // (289 chunks, seven of them). Leaf litter came out of that census too and is NOT excluded,
                // because it is placed as a decorator on the tree features themselves - excluding it would
                // delete the birch and dark oak trees. It stays a reported failure until it can be
                // replaced rather than removed.
                new GenerationRevision(8, alias("beta_1_7_3_content_preview_8"),
                    "moderner_beta:beta", true,
                    "1.21.5 firefly bushes excluded; leaf litter left visible because it needs replacement",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_MODERN_PLANTS,
                    BetaOreRules.BETA_1_7_3),
                // Revision 9: every other vanilla placed feature that Moderner Beta adds to a Beta-era
                // biome and that is too new for Beta 1.7.3 - the 1.7.2/1.14 flowers (which is what the
                // earlier rounds found as rose bushes and lilies of the valley), Beta 1.8 melons, and the
                // 1.13 ocean content. Found by reading Moderner Beta's biome-feature code, which lists
                // every vanilla feature it adds, rather than by censusing worlds one defect at a time.
                // Leaf litter and vines are still absent from this set on purpose: both are placed from
                // inside tree features, so excluding them would delete the trees.
                new GenerationRevision(9, alias("beta_1_7_3_content_preview_9"),
                    "moderner_beta:beta", true,
                    "the remaining modern vanilla features Moderner Beta adds to Beta biomes are excluded; "
                        + "leaf litter and vines still need generation-time suppression",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES,
                    BetaOreRules.BETA_1_7_3),
                // Revision 10 adds the desert well. It is Release 1.2.1 (12w04a) and a placed feature, so
                // neither the structure gate nor any earlier revision excluded it.
                new GenerationRevision(10, alias("beta_1_7_3_content_preview_10"),
                    "moderner_beta:beta", true,
                    "modern content: the Release 1.2.1 desert well is excluded, which no earlier revision did",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_DESERT_WELL,
                    BetaOreRules.BETA_1_7_3)
            )));

        versions.add(new HistoricalVersion(
            HistoricalExperience.Profiles.BETA_1_8_1,
            "Beta 1.8.1",
            "beta",
            chronology(ERA_BETA, 1, 8, 1),
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_8_1_MOBS,
                HistoricalLootRules.BETA_1_8_1_BY_ENTITY,
                // Empty on purpose: this version HAS hunger, so food feeds the food level and health
                // regeneration follows from it. Declaring heal values here would apply both at once.
                java.util.Map.of(),
                true,
                true,
                true,
                // Beta 1.8.1 does have structures, so demanding generate-structures=false for it would
                // have been wrong. The requirement is a property of the version, not of the framework.
                true,
                "VERIFIED from the b1.8.1 client (sha1 6b562463ccc2c7ff12ff350a2b04a67b3adcd37b): its "
                    + "entity registry (class um, sha1 0db6b648...) holds endermen, cave spiders, "
                    + "silverfish and XP orbs, and it has a FoodStats-shaped hunger class - none of which "
                    + "exist in b1.7.3. NOT VERIFIED: sprinting. The b1.8.1 jar's sprint fingerprint "
                    + "could not be located, so 'Beta 1.8 added sprinting' is documented version history "
                    + "rather than a jar observation, and allowsSprinting=true here is an inference. The "
                    + "drop items are verified present in the b1.8.1 item registry (rottenFlesh, "
                    + "chickenRaw, beefRaw, spiderEye) but each item-to-mob association and every count "
                    + "is inferred, and the model carries one item per mob so probabilistic secondary "
                    + "drops are not expressed. See research/version-boundaries.json"),
            List.of(
                // This version's original declaration is free to carry restrictions from the start:
                // no world predates it, so nothing is being reinterpreted. The same eight anachronisms
                // still did not exist in Beta 1.8.1 - copper, amethyst, tuff and glow lichen are far
                // later, and granite/diorite/andesite arrive in Release 1.8, not Beta 1.8 - and its ores
                // are unchanged from Beta 1.7.3, so emerald ore still does not exist.
                new GenerationRevision(0, alias("beta_1_8_1"), "moderner_beta:beta_1_8_1", false,
                    "frozen original declaration for this version: reduced envelope and no cave-biome "
                        + "injection, inheriting Moderner Beta's own Beta 1.8.1 preset",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITH_REPLACED_ORES,
                    BetaOreRules.BETA_1_7_3),
                // Revision 1 applies the same cave-biome correction as Beta 1.7.3 revision 6, for the
                // same reason: this version's own upstream preset also distributes MODERN cave biomes
                // through a voronoi diagram, and setting the provider to `none` returns a null biome
                // rather than a historical one.
                new GenerationRevision(1, alias("beta_1_8_1_cave_fix"),
                    "moderner_beta:beta_1_8_1", true,
                    "cave biomes: modern voronoi cave biomes replaced by a single era-appropriate biome",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITH_REPLACED_ORES,
                    BetaOreRules.BETA_1_7_3),
                // Revision 2 adds the modern-content exclusions this version was missing. Measured first:
                // a world on revision 1 contained kelp 338, kelp plants 1,666, seagrass 1,301 and tall
                // seagrass 1,212, all of them 1.13 content, plus the 1.7.2/1.14 flowers, sea pickles,
                // warm-ocean vegetation, water lilies and the 1.21.5 firefly bushes. Melons are allowed
                // here because Beta 1.8 introduced them, which is why this list is derived with an
                // explicit filter rather than copied.
                new GenerationRevision(2, alias("beta_1_8_1_no_modern_content"),
                    "moderner_beta:beta_1_8_1", true,
                    "modern content: removes the 1.13 aquatic plants, the 1.7.2 and 1.14 flowers, sea "
                        + "pickles, warm-ocean vegetation, water lilies, the 1.21.5 firefly bushes and the "
                        + "Release 1.2.1 desert well, none of which existed in Beta 1.8.1",
                    HistoricalFeatureExclusions.BETA_1_8_1_WITHOUT_MODERN_VANILLA_FEATURES,
                    BetaOreRules.BETA_1_7_3)
            )));

        // ---- Terrain-only versions: Beta 1.9-pre-3 through Release 1.6.4 ----------------------------
        //
        // These declare generation only. Their terrain foundation is the actual upstream Moderner Beta
        // preset, and each was verified to declare moderner_beta:overworld_128 - the same reduced 0..127
        // envelope Beta 1.8.1 uses - so the project's already-verified override set applies unchanged:
        // reference the upstream preset, disable deepslate (a modern block none of them had), and remove
        // the cave-biome injection.
        //
        // Their SIMULATION rules are deliberately left unmodified rather than copied from Beta 1.7.3 or
        // Beta 1.8.1. Those rule sets were researched for their own versions; applying them here would be
        // a guess dressed as data. For Release 1.0 through 1.6.4 the modern defaults are also close to
        // right in the ways that matter most - sprinting, hunger and experience all exist by then - so
        // "unmodified" is an honest statement of the current state, not a placeholder for missing work.
        // What is NOT historical for them is stated in each evidence string: mob set, mob drops, ore
        // distribution, sea level and structure placement.
        // The Beta 1.9 prereleases differ from EACH OTHER, which is why they are declared one at a time rather than
        // as one profile with six names. Each entry below carries what its own build changed.
        //
        // Pre1 and Pre2 are declared through the rules-carrying helper rather than terrainOnly, specifically so they
        // can restrict spawns. The research found that the versions declared through terrainOnly - Pre3 and every
        // Release profile - have NO spawn restriction at all, and therefore spawn modern 1.21.1 mobs. That is a
        // content defect recorded in research/implementation-coverage.md and still open for those versions.
        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_1", "Beta 1.9 Prerelease 1", "beta",
            chronology(ERA_BETA, 1, 9, 1), "moderner_beta:beta_1_9_pre_3",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_9_PRE_1_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "The first half of the Adventure Update. Nether bricks, mycelium, lily pads, blaze rods, ghast tears, "
                    + "gold nuggets and Nether wart; Nether fortresses with two blaze spawners; the mushroom island "
                    + "and four snow biomes; the blaze, magma cube, mooshroom, snow golem and villager mobs; reduced "
                    + "landmass and ocean height 0.5 -> 0.4. Armour stops reducing fire, explosion and projectile "
                    + "damage and gains durability, and loses durability-based effectiveness decay; held-jump "
                    + "auto-repeat is removed; snowfall temporarily destroys redstone, torches, saplings and rails, a "
                    + "regression Pre5 fixes. The last three are NOT implemented: each needs a mechanism this project "
                    + "does not have yet, and the snowfall regression deliberately must not leak into Release 1.0. "
                    + "Terrain comes from Moderner Beta's beta_1_9_pre_3 preset, the only Beta 1.9 terrain it ships, "
                    + "so Pre1's own reduced landmass and ocean height are not reproduced."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));

        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_2", "Beta 1.9 Prerelease 2", "beta",
            chronology(ERA_BETA, 1, 9, 2), "moderner_beta:beta_1_9_pre_3",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_9_PRE_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Potions and status effects, brewing ingredients (glass bottles, spider eyes, blaze powder, "
                    + "fermented spider eye, magma cream), nine music discs, hardcore mode, animal breeding with "
                    + "wheat, and combining equally damaged tools to repair. The Pre1 XP-on-jump bug is removed and "
                    + "held-jump auto-repeat is restored. Adds NO mob: breeding changes how existing mobs are made, "
                    + "not which exist, so the spawn set is shared with Pre1 as a statement about the research. "
                    + "Breeding and tool repair are not implemented yet; the cauldron exists in code without an "
                    + "assigned block id in this build and is not obtainable, which is recorded rather than faked. "
                    + "Terrain comes from beta_1_9_pre_3, the only Beta 1.9 terrain Moderner Beta ships."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));

        // Prerelease 4 is where the Adventure Update's second half lands: enchanting becomes OPERATIONAL and the
        // End replaces the Sky dimension. Two gates already coded against BETA_1_9_PRE_4_CHRONOLOGY - functional
        // enchanting and the end portal - become live the moment this profile is declared, which is exactly the
        // inter-build transition this project is for, and it is why these three are declared one at a time rather
        // than as one profile with three names.
        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_4", "Beta 1.9 Prerelease 4", "beta",
            chronology(ERA_BETA, 1, 9, 4), "moderner_beta:beta_1_9_pre_3",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_9_PRE_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "End stone; splash potions; operational enchanting with 17 enchantments, table maximum raised to "
                    + "50 and bookshelf power required; the End dimension generated and reachable through an active "
                    + "portal, with obsidian pillars; numeric XP levels displayed and spendable; moon phases. "
                    + "Enchanting becomes server-side and enchantments actually apply; hoes, flint-and-steel and "
                    + "shears leave the table-eligible items; the eye of ender tracks strongholds; endermen spawn in "
                    + "the End and stop burning in daylight; spiders and cave spiders become arthropods. The Sky "
                    + "dimension is REPLACED by the End. NOT implemented: the End's own generation beyond the "
                    + "reachability gate, and the dragon, whose code exists but does not spawn in this build. Adds "
                    + "no naturally spawning mob, so the spawn set is inherited. Terrain is beta_1_9_pre_3, the only "
                    + "Beta 1.9 terrain Moderner Beta ships."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));

        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_5", "Beta 1.9 Prerelease 5", "beta",
            chronology(ERA_BETA, 1, 9, 5), "moderner_beta:beta_1_9_pre_3",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_9_PRE_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Ten achievements including brewing, the Nether, locating the End and killing the Dragon; the "
                    + "/toggledownfall and /xp commands; a clouds toggle; the dragon death animation with test "
                    + "health 1, which still does not spawn; multiplayer breeding producing babies rather than "
                    + "adults. Changes: slime spawn height raised from Y=0-16 to Y=0-40; Silk Touch no longer "
                    + "obtains spawners or lit furnaces and becomes incompatible with Fortune; downward water flow "
                    + "begins creating source blocks; and SNOWFALL NO LONGER DESTROYS REDSTONE, TORCHES, SAPLINGS "
                    + "AND RAILS - the regression Prerelease 1 introduced is fixed here, which is why that "
                    + "regression was recorded against Pre1 rather than carried forward. Adds no mob. Terrain is "
                    + "beta_1_9_pre_3."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));

        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_6", "Beta 1.9 Prerelease 6", "beta",
            chronology(ERA_BETA, 1, 9, 6), "moderner_beta:beta_1_9_pre_3",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_9_PRE_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "End crystals on the obsidian pillars, healing the dragon; the dragon egg; the boss bar; and the "
                    + "Ender Dragon ACTUALLY SPAWNING - the first build in the sequence with a spawning, killable "
                    + "dragon - with a working exit portal and reachable credits. Baby sheep inherit their dye. "
                    + "Prerelease 5's downward-water source creation is reverted to ordinary flowing water. Known "
                    + "issue in this build, recorded rather than fixed: wooden doors open only with redstone. No "
                    + "mob is added to the natural spawn set, because the dragon is a fixed End spawn rather than a "
                    + "natural one and End crystals are not mobs - which is why the spawn set is still inherited. "
                    + "Terrain is beta_1_9_pre_3."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));

        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_3", "Beta 1.9 Prerelease 3", "beta",
            chronology(ERA_BETA, 1, 9, 3), "moderner_beta:beta_1_9_pre_3",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_9_PRE_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Newborn animals became baby variants in this build, which changes breeding rather than the mob set, "
                    + "so the spawn set is inherited from Pre2. Declared through the rules-carrying helper so that "
                    + "spawns are restricted: it was terrainOnly, which sets no restriction, and therefore spawned "
                    + "modern mobs. Terrain comes from beta_1_9_pre_3 itself."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));
        versions.add(terrainOnly(
            "historical_experience:release_1_0_0", "Release 1.0", "release",
            chronology(ERA_RELEASE, 1, 0, 0), "moderner_beta:release_1_0_0",
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));
        versions.add(terrainOnly(
            "historical_experience:release_1_1", "Release 1.1", "release",
            chronology(ERA_RELEASE, 1, 1, 0), "moderner_beta:release_1_1",
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES));
        versions.add(terrainOnly(
            "historical_experience:release_1_2_5", "Release 1.2.5", "release",
            chronology(ERA_RELEASE, 1, 2, 5), "moderner_beta:release_1_2_5",
            HistoricalFeatureExclusions.RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES));
        versions.add(terrainOnly(
            "historical_experience:release_1_6_4", "Release 1.6.4", "release",
            chronology(ERA_RELEASE, 1, 6, 4), "moderner_beta:release_1_6_4",
            HistoricalFeatureExclusions.RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES));

        // ---- Pre-Beta eras: Infdev and Alpha ------------------------------------------------------
        //
        // These are the oldest versions this build offers, and their simulation rules are a different
        // kind of claim from the Release versions above. Hunger, sprinting and experience are known
        // ABSENT at Beta 1.7.3 and known PRESENT at Beta 1.8.1, both verified from those clients. A
        // feature absent at 1.7.3 and present at 1.8.1 is therefore absent at every version that
        // precedes 1.7.3, and that is a sound inference rather than a guess - but it IS an inference,
        // and each entry says so instead of borrowing the authority of the Beta 1.7.3 measurements.
        //
        // What is genuinely not researched for these eras, and is left modern rather than invented:
        // their mob sets, their mob drops, their food heal values, their ore distribution and their
        // decoration. Those are stated per entry rather than quietly inherited from Beta 1.7.3, because
        // the earlier mistake in this project was assuming Beta 1.8.1 had villagers.
        // Infdev 20100227 and 20100325 are NOT offered, and the reason is measured. Both of their
        // upstream presets declare `provider = moderner_beta:infdev_227` plus a unique
        // `moderner_beta:infdev_227_structures` component, and both hang the server during world
        // creation - "Preparing level" / "Selecting global world spawn" - producing no verdict at all.
        // The two Infdev builds whose presets use `provider = moderner_beta:noise_3d`, 20100420 and
        // 20100611, load completely and PASS, which isolates the provider as the discriminator rather
        // than this project's override:
        //
        //   20100227  provider=moderner_beta:infdev_227  HANGS (no verdict)
        //   20100325  provider=moderner_beta:infdev_227  HANGS (killed at 170s)
        //   20100420  provider=moderner_beta:noise_3d   PASS  (12/3/1 components, all matching)
        //   20100611  provider=moderner_beta:noise_3d   PASS
        //
        // The same shape as the Classic/Indev finding: a world created from Moderner Beta's own preset
        // does not load here. Their extracted data is preserved in research/infdev-registries.json - 227
        // instantiates no mob at all, 325 constructs six from a nextInt(6) switch, and neither has any
        // food - so the work is not lost and can be reinstated if the provider is fixed. Until then,
        // offering them would mean offering a world that never loads.
        versions.add(infdevTerrainOnly(
            "historical_experience:infdev_420", "Infdev 20100420", 420, "moderner_beta:infdev_420",
            HistoricalSpawnRules.INFDEV_20100420_AND_20100611_MOBS, true,
            "MOBS: creeper, skeleton, spider, giant, zombie, pig and sheep, read from entity registry "
                + "class net/minecraft/a/c/e (13 registrations: Mob and Monster bases, those seven "
                + "species, and arrow, item, painting and primed TNT). Registered, which is the same "
                + "convention the Beta 1.7.3 self-check fixes; no numeric entity id exists yet in this "
                + "registry."));
        versions.add(infdevTerrainOnly(
            "historical_experience:infdev_611", "Infdev 20100611", 611, "moderner_beta:infdev_611",
            HistoricalSpawnRules.INFDEV_20100420_AND_20100611_MOBS, true,
            "MOBS: the same seven as 20100420 - registry class net/minecraft/a/c/e with a "
                + "byte-identical <clinit> instruction sequence and the same 13 registrations."));
        versions.add(terrainOnlyWithRules(
            // Alpha 1.0.1, the first of the twenty-nine targets. Declared through this helper rather than through
            // alphaTerrainOnly, which hard-codes the v1.1.2_01 chronology and so cannot serve a second build.
            //
            // Its rules are the Alpha profile's with ONE difference, and that is the point of the owner's
            // correction: the Alpha versions do not share a gameplay ruleset. Slimes arrive in 1.0.11 and chickens
            // in 1.0.14, so this build spawns neither. It reuses the Alpha food table deliberately, because no
            // edible item was added anywhere between 1.0.1 and 1.1.0 - and that is a fact about the deltas, not an
            // assumption that the versions are alike.
            //
            // NOT implemented, and recorded rather than guessed: gold ore generating "twice as often" and diamond
            // ore twice as often in smaller blobs are relative to the PREVIOUS build, and the research gives no
            // absolute counts to double. A borrowed number would look like evidence.
            "historical_experience:alpha_1_0_1", "Alpha v1.0.1", "alpha",
            chronology(ERA_ALPHA, 1, 0, 1), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_0_1_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "Seecret Friday 3, 2 July 2010. Redstone ore and dust, redstone torches, levers, buttons, "
                    + "pressure plates and iron doors; monster spawners stop dropping as items. Mob set derived "
                    + "from the Alpha v1.1.2_01 client registry minus slimes (Alpha 1.0.11) and chickens (Alpha "
                    + "1.0.14), per research/version-transitions.json. Food table shared with Alpha v1.1.2_01 "
                    + "because no edible item was added between them. Gold and diamond ore frequency per this "
                    + "build is NOT implemented: see research/implementation-coverage.md."),
                HistoricalFeatureExclusions.ALPHA_BEFORE_1_0_11_CONTENT));

        // Alpha 1.0.4, whose distinguishing feature is winter mode: an alternate CREATION-TIME generator, which the
        // research describes as a separate world-creation choice rather than a climate-driven snow biome.
        //
        // Hand-written rather than declared through the helper, and deliberately: the helper produces exactly three
        // revisions, and this build needs a fourth. The checklist's rule against hand-writing exists to stop a
        // version taking shortcuts on its RULES, not to forbid one with a genuinely different revision set.
        //
        // Its mob set and food table are shared with Alpha 1.0.1 because no mob and no edible item is added between
        // them - a fact about the per-build deltas, not an assumption that the builds are alike.
        versions.add(new HistoricalVersion(
            "historical_experience:alpha_1_0_4", "Alpha v1.0.4", "alpha",
            chronology(ERA_ALPHA, 1, 0, 4),
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_0_1_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "Seecret Friday 4, 9 July 2010. Snow and ice blocks, selectable winter-mode worlds, and mob physics "
                    + "changed so jumps became shorter. Snow layers existed in terrain but were not obtainable as an "
                    + "item, so generated snow is not normal harvest here. Mob set and food table shared with Alpha "
                    + "1.0.1: no mob or edible item is added between the two builds."),
            List.of(
                new GenerationRevision(
                    0,
                    "historical_experience:alpha_1_0_4",
                    "moderner_beta:alpha",
                    false,
                    "frozen original declaration for this version: reduced envelope and no cave-biome injection, "
                        + "inheriting Moderner Beta's own preset",
                    HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS,
                    List.of()),
                new GenerationRevision(
                    1,
                    "historical_experience:alpha_1_0_4_no_grass",
                    "moderner_beta:alpha",
                    true,
                    "grass excluded: it was added in Beta 1.6, and Moderner Beta places it in this version's biomes",
                    HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED,
                    List.of()),
                new GenerationRevision(
                    2,
                    "historical_experience:alpha_1_0_4_no_modern_content",
                    "moderner_beta:alpha",
                    true,
                    "modern content: the Release 1.2.1 desert well, and clay, which arrives in Alpha 1.0.11",
                    HistoricalFeatureExclusions.ALPHA_BEFORE_1_0_11_CONTENT,
                    List.of()))));

        // Alpha 1.0.6, the Seecret Friday that added cactus, boats and mixed tree sizes and REMOVED leaf decay.
        //
        // Shares the mob set and food table with 1.0.1 and 1.0.4: no mob and no edible item is added between them.
        //
        // Three of its own deltas are NOT implemented, and are recorded rather than approximated because each needs
        // a mechanism this project does not have yet - not a value:
        //   * leaves no longer decay: a vanilla behaviour to disable, not a number to set;
        //   * sapling drop chance 10% -> 5%: a loot modifier;
        //   * the snow-block recipe costing four snowballs while breaking yields six: the economy quirk this build
        //     is known for, which needs recipe data rather than a gate.
        versions.add(terrainOnlyWithRules(
            "historical_experience:alpha_1_0_6", "Alpha v1.0.6", "alpha",
            chronology(ERA_ALPHA, 1, 0, 6), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_0_1_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "Seecret Friday 5, 16 July 2010. Cactus, boats, mixed tree sizes and throwable snowballs; leaf decay "
                    + "removed, sapling drop chance cut from 10% to 5%, and the snow-block recipe costs four "
                    + "snowballs while breaking yields six. Those three are recorded as not implemented. Mob set and "
                    + "food table shared with Alpha 1.0.1: no mob or edible item is added between them."),
            HistoricalFeatureExclusions.ALPHA_BEFORE_1_0_11_CONTENT));

        // Alpha 1.0.11 is the build the availability audit had been naming for many rounds: clay and sugar cane were
        // dated to "alpha" - Alpha v1.1.2_01 - because 1.0.11 did not exist and "alpha" was the earliest DECLARED
        // version that had them. Declaring this build is what let the date become correct, and the audit went quiet
        // the moment it was re-pointed. This is the payoff of recording those proxy dates instead of guessing them.
        //
        // Its revision uses the PRE-CLAY exclusion list, because this build is the one that introduces clay and
        // sugar cane, so nothing may exclude them here.
        versions.add(terrainOnlyWithRules(
            "historical_experience:alpha_1_0_11", "Alpha v1.0.11", "alpha",
            chronology(ERA_ALPHA, 1, 0, 11), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_0_11_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "Clay, sugar cane, paper, books, bricks and slimes. Cows are present because they arrive in Alpha "
                    + "v1.0.8, so this is the first declared Alpha build with cows AND the last without chickens "
                    + "(1.0.14). The slime date is inference rather than citation and the reasoning is recorded in "
                    + "HistoricalSpawnRules.ALPHA_1_0_11_MOBS. Food table shared with Alpha 1.0.1: no edible item "
                    + "is added between them."),
            HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL));

        // Alpha 1.0.14. Two of its facts interact, and that is why it is declared separately from 1.0.11:
        // chickens ARRIVE here, and slime natural spawning is DISABLED here by a bug. A single "Alpha mobs" set
        // could express neither.
        //
        // Its evidence also records what this build changed in world generation itself: dungeon generator
        // iterations went 4 -> 8, "increasing dungeon generation and shifting subsequent decorator RNG outcomes such
        // as trees/flowers". That is a real generation change this project does not reproduce - the terrafirm
        // authority is Moderner Beta's preset, which does not expose it - so it is recorded rather than faked,
        // exactly as the snowfall regression was recorded against Pre1.
        versions.add(terrainOnlyWithRules(
            "historical_experience:alpha_1_0_14", "Alpha v1.0.14", "alpha",
            chronology(ERA_ALPHA, 1, 0, 14), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_0_14_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "Chickens and eggs, jukeboxes and music discs (from dungeon chests, or when a skeleton kills a "
                    + "creeper), furnace and chest minecarts, three non-ambient music tracks, and a revised title "
                    + "logo. Obsidian blast resistance rises to 1,200 and music plays throughout the world rather "
                    + "than only as ambience. SLIME NATURAL SPAWNING IS DISABLED in this build due to a reported "
                    + "spawn-limit miscalculation - a bug, not a design, and NOT a permanent removal. Dungeon "
                    + "generator iterations increase from 4 to 8, shifting decorator RNG; recorded as not "
                    + "implemented because the terrain authority does not expose it. Food table shared with Alpha "
                    + "1.0.1: no edible item is added between them."),
            HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL));

        // WINTER MODE IS NOT DECLARED YET, and the reason is a conflict in the model rather than missing research.
        // Moderner Beta ships `moderner_beta:alpha_winter`, and this project's own settings preset
        // `alpha_1_0_4_winter` inherits it, so the terrain is available. What is missing is a place to put it:
        // revisions are append-only improvements whose NEWEST member is what a new world should use, and
        // GenerationBackport1211Test enforces exactly that. Winter mode is an ALTERNATIVE, not a successor, so
        // declaring it as revision 3 makes the public entry point at revision 2 while the newest is revision 3 -
        // which the invariant correctly rejects. It needs either a variant-version concept or a way to mark a
        // revision as selectable-but-not-newest. Recorded in research/implementation-coverage.md.

        versions.add(alphaTerrainOnly(
            "historical_experience:alpha", "Alpha v1.1.2_01", "moderner_beta:alpha"));

        // ---- The finite worlds: Classic and Indev are NOT offered ---------------------------------
        //
        // They were implemented and they do not work. A Classic world loads, its preset applies
        // completely (14 chunk components, all matching, providers initialized) and its marker resolves -
        // and then requesting chunk (0,0), the very centre, never reaches FULL status. The server
        // watchdog killed the run after 60 seconds. Measured, twice: once ring-sampling at radius 1 and
        // once at radius 0.
        //
        // These eras are FINITE in Moderner Beta, using a different chunk provider (moderner_beta:finite_2d)
        // and finite-specific components (finite_level_properties, finite_noise, finite_pools,
        // finite_beaches, finite_cave_generation, world_border, spawn_indev_house). Moderner Beta ships
        // exactly ONE dimension type, a replacement of vanilla minecraft:overworld that this project
        // copied verbatim, and one world preset. The decisive experiment was then run, and it is NOT
        // our wrapper: a world created directly from Moderner Beta's own world preset with
        // generator-settings {"preset":"moderner_beta:classic_0_30"} - no project preset, marker or
        // dimension type involved at all - applies its settings completely (14 chunk / 3 biome / 1 cave
        // components, providers initialized, resolved as an ordinary upstream world) and then hangs on
        // chunk (0,0) until the watchdog kills the server. Moderner Beta's finite path does not generate
        // in this environment, which is an upstream finding rather than a defect here, and offering these
        // versions would mean offering a world that hangs the server.
        //
        // The resources, the create-world entries and the world-preset tag entries were removed with the
        // declarations: half-offered is worse than absent.

        ALL = List.copyOf(versions);

        // Self-checks: two versions may never share a profile id or a recipe alias, because both are
        // persisted and a collision would make a world's recorded identity ambiguous.
        Map<String, HistoricalVersion> byId = new LinkedHashMap<>();
        Map<String, HistoricalVersion> byAlias = new LinkedHashMap<>();
        for (HistoricalVersion version : ALL) {
            HistoricalVersion previous = byId.put(version.id(), version);
            if (previous != null) {
                throw new IllegalStateException("duplicate version id " + version.id());
            }
            for (GenerationRevision revision : version.revisions()) {
                HistoricalVersion owner = byAlias.put(revision.alias(), version);
                if (owner != null) {
                    throw new IllegalStateException("recipe alias " + revision.alias()
                        + " is declared by both " + owner.id() + " and " + version.id());
                }
            }
        }
        BY_ID = Map.copyOf(byId);
        BY_ALIAS = Map.copyOf(byAlias);
    }

    private static final Map<String, HistoricalVersion> BY_ID;
    private static final Map<String, HistoricalVersion> BY_ALIAS;

    private HistoricalVersions() {
    }

    private static String alias(String path) {
        return HistoricalExperience.MOD_ID + ":" + path;
    }

    /**
     * The shared evidence clause for a pre-Beta era: which rules are inferred, why that is sound, and
     * what has deliberately been left alone because it has not been researched.
     */
    private static String preBetaEvidence(String upstreamPreset, String versionName) {
        return "PRE-BETA ERA. Generation inherits the real upstream " + upstreamPreset
            + " preset, with deepslate disabled and no cave-biome injection, on the project's reduced "
            + "0..127 envelope - the same override set verified for Beta 1.8.1, whose upstream preset "
            + "declares the same moderner_beta:overworld_128. Simulation rules are INFERRED, not "
            + "measured for this version: hunger, sprinting and experience are verified ABSENT at Beta "
            + "1.7.3 (its client has no food, saturation or exhaustion concept and no sprint binding) "
            + "and verified PRESENT at Beta 1.8.1, so a feature absent there and present at 1.8.1 is "
            + "absent at every version preceding 1.7.3. Sound, but an inference, and weaker evidence "
            + "than the Beta 1.7.3 rules. NOT researched for " + versionName + " and therefore still "
            + "modern: its mob set, mob drops, food heal values, ore distribution and decoration. The "
            + "newest version to have its own ore population is Beta 1.7.3. See "
            + "research/prebeta-registries.json for what the pre-Beta extraction does establish.";
    }

    /** An Infdev-era version: reduced terrain, plus that build's own verified mob situation. */
    private static HistoricalVersion infdevTerrainOnly(
        String id,
        String displayName,
        int buildNumber,
        String upstreamPreset,
        java.util.Set<String> mobs,
        boolean restrictsSpawns,
        String mobEvidence
    ) {
        // Chronology is the era offset plus the build number, so 227 < 325 < 420 < 611 orders correctly
        // and every Infdev build still sorts before Alpha, Beta and Release.
        //
        // PROVENANCE, now resolved rather than blocked. Mojang publishes none of these four builds
        // (absent from all three official manifests; the only published Infdev id is inf-20100618, a
        // different build). The project owner authorised archive-hosted jars under explicit conditions,
        // and every jar was obtained from Omniarchive Vault BETA with its SHA-1 checked against an
        // independently published per-version metadata document - not against the download itself:
        //   227 -> inf-20100227-1433.jar  sha1 78ba00379a9c55e692a55c9b9464a2c7f46281a9  509103 bytes
        //   325 -> inf-20100325-1640.jar  sha1 99da3b55b4db292faca59824e3ec76bf53a7eae6  531352 bytes
        //   420 -> inf-20100420.jar       sha1 27010a5137abd2c8d8df85e99c14f5406ec197b3  552012 bytes
        //   611 -> inf-20100611.jar       sha1 421318a554f17463a56a271d08e9597941d066d9  566417 bytes
        String evidence = preBetaEvidence(upstreamPreset, displayName)
            + " ARCHIVE PROVENANCE: retrieved from Omniarchive Vault BETA with the SHA-1 checked against "
            + "an independently published per-version metadata document, because Mojang publishes no "
            + "Infdev build. " + mobEvidence
            + " FOOD: verified ABSENT in this build - the health-restoration method exists and clamps at "
            + "20, but it has zero call sites jar-wide, there are no constant-pool hits for any food "
            + "item, and no hunger, saturation or exhaustion system exists at all. So an empty food "
            + "table here is a measured fact rather than an unfinished extraction.";
        return terrainOnlyWithRules(id, displayName, "infdev", ERA_INFDEV + buildNumber, upstreamPreset,
            new VersionRules(
                restrictsSpawns,
                mobs,
                java.util.Map.of(),
                java.util.Map.of(),
                false,
                false,
                false,
                false,
                evidence));
    }

    /**
     * The Alpha-era version: reduced terrain, plus a mob set and food values that are <b>verified</b>.
     *
     * <p>Alpha is no longer terrain-only. Its natural-spawn set and all six of its direct food heals were
     * read from its own client (SHA-1 {@code daa4b9f192d2c260837d3b98c39432324da28e86}, 897,164 bytes,
     * checked against Mojang's own version metadata first), and every food value is proved from that
     * jar's own recipes, drop methods or smelting table rather than by analogy to Beta. Its
     * sprint/hunger/experience/structure rules remain inferred by the same monotonicity argument as the
     * other pre-Beta eras, and the evidence string keeps the two kinds of claim apart.
     */
    private static HistoricalVersion alphaTerrainOnly(
        String id,
        String displayName,
        String upstreamPreset
    ) {
        String evidence = "RESEARCHED from the Alpha v1.1.2_01 client (sha1 "
            + "daa4b9f192d2c260837d3b98c39432324da28e86, 897164 bytes, verified against Mojang's own "
            + "version metadata before use): the natural-spawn set (entity registry class ew, 20 "
            + "registrations = 10 mobs + 8 non-mob entities + the base classes Mob and Monster; ghasts, "
            + "zombie pigmen, squid and wolves absent as UTF-8 constants anywhere in the jar) and all six "
            + "direct food heals (health field ge.E, heal method ge.b(int), food class oj with "
            + "bowl-returning subclass ap; each value proved from the jar's own recipe, drop or smelting "
            + "table). Still INFERRED by monotonicity between the verified Beta 1.7.3 and Beta 1.8.1 "
            + "boundaries: no hunger, no sprinting, no experience, no structures. NOT researched and "
            + "therefore still modern: mob drops, ore distribution, decoration. See "
            + "research/prebeta-registries.json.";
        return terrainOnlyWithRules(
            // Chronology on the registry's own scheme, not hand-written. It used to read ERA_ALPHA + 112, which
            // is BELOW every Alpha 1.x build the scheme produces (Alpha 1.0.1 is 401001) - so declaring Alpha 1.0.1
            // would have placed it after this profile, inverting the Alpha line. The value is derived, never
            // persisted: a world stores its profile id and preset, not a chronology.
            id, displayName, "alpha", chronology(ERA_ALPHA, 1, 1, 2), upstreamPreset,
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_1_2_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                evidence));
    }

    /**
     * The rule set every pre-Beta era shares.
     *
     * <p>Deliberately declares NO mob set and NO food heal values. An empty mob set means unrestricted
     * spawning, which is a visible historical inaccuracy, and an empty heal table means eating does
     * nothing in a version with no hunger. Both are recorded as gaps rather than filled with values
     * borrowed from a neighbouring version, because the one thing this project has learned the hard way
     * is that a plausible borrowed value is worse than a stated gap: it looks like evidence.
     */
    private static VersionRules preBetaRules(String evidence) {
        return new VersionRules(
            false,                   // spawns NOT restricted: unresearched, so modern spawning is left alone
            java.util.Set.of(),      // and therefore no allowed set may be named, which the record enforces
            java.util.Map.of(),      // drops not researched
            java.util.Map.of(),      // food heals not researched
            false,                   // no experience, inferred by monotonicity from Beta 1.7.3/1.8.1
            false,                   // no sprinting, same inference
            false,                   // no hunger, same inference
            false,                   // no structures: mineshafts, villages and strongholds arrive in Beta 1.8
            evidence);
    }

    private static HistoricalVersion terrainOnlyWithRules(
        String id,
        String displayName,
        String era,
        int chronology,
        String upstreamPreset,
        VersionRules rules
    ) {
        return terrainOnlyWithRules(id, displayName, era, chronology, upstreamPreset, rules, null);
    }

    /**
     * As above, but with the modern-content exclusion list chosen by the caller.
     *
     * <p>Added for Alpha 1.0.1, which predates clay: that exclusion cannot live in the shared pre-Beta list because
     * Alpha v1.1.2_01 has clay, and it does not need a new revision because 1.0.1 was declared this session and no
     * world exists on its recipes except this project's own probe fixture. Clay is simply modern content for that
     * build, which is what revision 2 means.
     *
     * @param modernContentExclusions the list revision 2 should carry, or null to let this method derive it
     */
    private static HistoricalVersion terrainOnlyWithRules(
        String id,
        String displayName,
        String era,
        int chronology,
        String upstreamPreset,
        VersionRules rules,
        List<HistoricalFeatureExclusions.Exclusion> modernContentExclusions
    ) {
        List<HistoricalFeatureExclusions.Exclusion> revisionTwoExclusions = modernContentExclusions != null
            ? modernContentExclusions
            : (chronology >= ERA_ALPHA
                ? HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL
                : HistoricalFeatureExclusions.INFDEV_WITHOUT_DUNGEONS);

        return new HistoricalVersion(
            id,
            displayName,
            era,
            chronology,
            rules,
            List.of(
                new GenerationRevision(
                    0,
                    id,
                    upstreamPreset,
                    false,
                    "frozen original declaration for this version: reduced envelope and no cave-biome "
                        + "injection, inheriting Moderner Beta's own preset",
                    HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS,
                    List.of()),
                // Revision 1 removes grass, which did not exist before Beta 1.6. Every caller of this
                // helper predates that - Alpha and both Infdev builds - so the correction applies to all
                // of them, and it is a new revision rather than an edit because worlds already exist on
                // revision 0 and their recorded recipe has to keep meaning what it said.
                new GenerationRevision(
                    1,
                    alias(id.substring(id.indexOf(':') + 1) + "_no_grass"),
                    upstreamPreset,
                    true,
                    "grass excluded: it was added in Beta 1.6, and Moderner Beta places it in this version's "
                        + "biomes",
                    HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED,
                    List.of()),
                // Revision 2 adds the desert well, and the monster room only where the version predates it.
                // The boundary is derived rather than passed in: the monster room arrives in Infdev
                // 20100625-1917, which falls between the 20100611 build and Alpha, and both are on the
                // registry's own chronology scale. Deriving it here means the two Infdev builds and Alpha
                // cannot drift apart from the research, and no caller has to remember which list it needs.
                new GenerationRevision(
                    2,
                    alias(id.substring(id.indexOf(':') + 1) + "_no_modern_content"),
                    upstreamPreset,
                    true,
                    "modern content: the Release 1.2.1 desert well is excluded, and the monster room as well "
                        + "for the builds older than Infdev 20100625-1917",
                    revisionTwoExclusions,
                    List.of())));
    }

    /**
     * A version that declares generation only, with its simulation rules explicitly unmodified.
     *
     * <p>The evidence string is generated from {@code upstreamPreset} on purpose: it has to name the real
     * upstream foundation the terrain inherits from, and it has to say plainly that the simulation side
     * has not been researched. A version in this state is terrain coverage, and calling it anything more
     * would overstate what has been verified.
     */
    private static HistoricalVersion terrainOnly(
        String id,
        String displayName,
        String era,
        int chronology,
        String upstreamPreset,
        List<HistoricalFeatureExclusions.Exclusion> modernContentExclusions
    ) {
        String evidence = "TERRAIN ONLY. Generation inherits the real upstream " + upstreamPreset
            + " preset, with deepslate disabled and the cave-biome injection removed, on the project's "
            + "reduced 0..127 envelope - the same override set verified for Beta 1.8.1, whose upstream "
            + "preset declares the same moderner_beta:overworld_128. NOT researched for this version, so "
            + "the modern defaults still apply: its mob set, mob drops, ore distribution, sea level and "
            + "structure placement are not historical. See research/version-boundaries.json for what the "
            + "boundary research does establish about this version, and do not read this entry as "
            + "historical gameplay fidelity.";
        return new HistoricalVersion(
            id,
            displayName,
            era,
            chronology,
            HistoricalVersion.unmodified(evidence),
            List.of(
                new GenerationRevision(
                    0,
                    id,
                    upstreamPreset,
                    false,
                    "frozen original declaration for this version: reduced envelope and no cave-biome "
                        + "injection, inheriting Moderner Beta's own preset",
                    // The same eight anachronisms: copper, amethyst, tuff and glow lichen are far later,
                    // and granite/diorite/andesite arrive in Release 1.8, after every version here. No ore
                    // population is declared, so vanilla modern ore placement still applies and the ore
                    // distribution is explicitly not historical for these versions.
                    HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS,
                    List.of()),
                // Revision 1 applies the cave-biome correction. Every version here has an upstream preset
                // that distributes MODERN cave biomes through a voronoi diagram, and the previous
                // `moderner_beta:none` provider returns a null biome rather than a historical one. It is a
                // new revision rather than an edit because revisions are append-only and worlds already
                // exist on revision 0.
                new GenerationRevision(
                    1,
                    alias(id.substring(id.indexOf(':') + 1) + "_cave_fix"),
                    upstreamPreset,
                    true,
                    "cave biomes: modern voronoi cave biomes replaced by a single era-appropriate biome",
                    HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS,
                    List.of()),
                // Revision 2 adds the modern-content exclusions. Before it, these versions carried only the
                // eight blocks that postdate every declared version, so their worlds still received the 1.13
                // aquatic plants the Beta 1.8.1 measurement exposed. Which entries apply depends on the
                // version - lily pads from Beta 1.9, the desert well from Release 1.2.1 - so the list is
                // passed in rather than shared.
                new GenerationRevision(
                    2,
                    alias(id.substring(id.indexOf(':') + 1) + "_no_modern_content"),
                    upstreamPreset,
                    true,
                    "modern content: removes the plants, flowers and features that postdate this version",
                    modernContentExclusions,
                    List.of())));
    }

    /** Every declared version, oldest first. */
    public static List<HistoricalVersion> all() {
        return ALL;
    }

    /** A version by its gameplay profile id. */
    public static Optional<HistoricalVersion> byId(String versionId) {
        return versionId == null
            ? Optional.empty()
            : Optional.ofNullable(BY_ID.get(versionId));
    }

    /** The version that owns a persisted generation-recipe alias. */
    public static Optional<HistoricalVersion> byRevisionAlias(String rawAlias) {
        return rawAlias == null
            ? Optional.empty()
            : Optional.ofNullable(BY_ALIAS.get(rawAlias));
    }

    /** Every owned recipe alias of every version, in declaration order. */
    public static List<String> allRevisionAliases() {
        return ALL.stream()
            .flatMap(version -> version.revisions().stream())
            .map(GenerationRevision::alias)
            .toList();
    }

    /** The versions at or after a chronology key, for "which eras have this" queries. */
    public static List<HistoricalVersion> atOrAfter(int chronology) {
        return ALL.stream().filter(version -> version.chronology() >= chronology).toList();
    }

    /** The versions before a chronology key. */
    public static List<HistoricalVersion> before(int chronology) {
        return ALL.stream().filter(version -> version.chronology() < chronology).toList();
    }

    /** Every era family the registry declares. */
    public static Set<String> eras() {
        return ALL.stream().map(HistoricalVersion::era)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}

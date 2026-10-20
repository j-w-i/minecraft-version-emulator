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
package net.historicalexperience.legacy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.historicalexperience.HistoricalExperience;
import net.historicalexperience.generation.BetaOreRules;
import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.historicalexperience.gameplay.HistoricalLootRules;
import net.historicalexperience.gameplay.HistoricalSpawnRules;
import net.historicalexperience.version.Era;
import net.historicalexperience.version.Exclusion;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersion.GenerationRevision;
import net.historicalexperience.version.HistoricalVersion.VersionRules;
import net.historicalexperience.version.OreRule;

/**
 * Every historical version this build declares, and the generation recipes it declares them with.
 *
 * <h2>Why this is a package of its own</h2>
 *
 * <p>A version is a piece of <b>data</b> composed from three things: a position on the timeline
 * ({@link net.historicalexperience.version.Milestones}), a set of simulation rules, and one or more immutable
 * generation recipes. Two of those come from the systems that implement them - the spawn sets from
 * {@code gameplay}, the exclusion lists and ore populations from {@code generation} - so the declarations must be
 * allowed to depend on those packages.
 *
 * <p>{@code net.historicalexperience.version} must not: it is the model every other package reads, and a model that
 * depends on its consumers cannot be reasoned about in isolation (and cannot be initialised in an arbitrary order).
 * So the data lives here and the model stays pure. The only edge from the model back to data is
 * {@code HistoricalVersions -> LegacyCatalog}, which is the composition root and nothing else.
 *
 * <h2>Adding a version</h2>
 *
 * <p>Add an entry here, add the two resource files its revisions need, and add a milestone. The registry test
 * ({@code HistoricalVersionsTest}) asserts that every declared revision resolves as a marker, as a profile binding and
 * as a resource pair, so a missing piece fails at build time rather than at world load. See ADDING_VERSIONS.md.
 */
public final class LegacyCatalog {
    private static final List<HistoricalVersion> ALL = declare();

    private LegacyCatalog() {
    }

    /** Every declared version, in declaration order (oldest family last: the registry's own order). */
    public static List<HistoricalVersion> all() {
        return ALL;
    }

    private static List<HistoricalVersion> declare() {
        List<HistoricalVersion> versions = new java.util.ArrayList<>();

        versions.add(new HistoricalVersion(
            HistoricalExperience.Profiles.BETA_1_7_3,
            "Beta 1.7.3",
            "beta",
            key(Era.BETA.base(), 1, 7, 3),
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
            key(Era.BETA.base(), 1, 8, 1),
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
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_1", "Beta 1.9 Prerelease 1", "beta",
            key(Era.BETA.base(), 1, 9, 1), "moderner_beta:beta_1_9_pre_3",
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
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));

        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_2", "Beta 1.9 Prerelease 2", "beta",
            key(Era.BETA.base(), 1, 9, 2), "moderner_beta:beta_1_9_pre_3",
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
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));

        // Prerelease 4 is where the Adventure Update's second half lands: enchanting becomes OPERATIONAL and the
        // End replaces the Sky dimension. Two gates already coded against BETA_1_9_PRE_4_CHRONOLOGY - functional
        // enchanting and the end portal - become live the moment this profile is declared, which is exactly the
        // inter-build transition this project is for, and it is why these three are declared one at a time rather
        // than as one profile with three names.
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_4", "Beta 1.9 Prerelease 4", "beta",
            key(Era.BETA.base(), 1, 9, 4), "moderner_beta:beta_1_9_pre_3",
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
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));

        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_5", "Beta 1.9 Prerelease 5", "beta",
            key(Era.BETA.base(), 1, 9, 5), "moderner_beta:beta_1_9_pre_3",
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
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));

        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_6", "Beta 1.9 Prerelease 6", "beta",
            key(Era.BETA.base(), 1, 9, 6), "moderner_beta:beta_1_9_pre_3",
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
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));

        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_9_pre_3", "Beta 1.9 Prerelease 3", "beta",
            key(Era.BETA.base(), 1, 9, 3), "moderner_beta:beta_1_9_pre_3",
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
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));
        // Release 1.0.0, and the first of the Release line to carry RULES rather than being declared terrain-only.
        //
        // That declaration is the fix for the largest content defect this project had: terrainOnly sets no spawn
        // restriction, so every Release world spawned modern 1.21.1 mobs including piglins and hoglins. Release 1.0.0
        // is the released form of the Beta 1.9 prerelease, so its mob set IS the Pre set - which is why the set is
        // the prerelease's rather than something newly written.
        //
        // It uses releaseTerrainOnlyWithRules, not terrainOnlyWithRules: the latter offers a _no_grass revision
        // because every caller it was written for predates Beta 1.6, and this build is two years past it.
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_0_0", "Release 1.0", "release",
            key(Era.RELEASE.base(), 1, 0, 0), "moderner_beta:release_1_0_0",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_0_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "The released form of the Beta 1.9 prerelease. Its own deltas are the 1.0 sound update - chests, XP "
                    + "orb pickup, bone-crack damage, eating and drinking, blaze breathing and enderman sounds - and "
                    + "add no mob, which is why the spawn set is the prerelease set rather than a new one. Experience, "
                    + "sprinting, hunger and structures all exist by this build. The End, enchanting and brewing are "
                    + "operational; the dragon exists but this project does not claim the boss fight."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_1", "Release 1.1", "release",
            key(Era.RELEASE.base(), 1, 1, 0), "moderner_beta:release_1_1",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_0_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Adds NO mob, so the spawn set is Release 1.0.0's and is inherited rather than written out. Its own "
                    + "deltas are spawn eggs, sheep regrowing wool by grazing, and the usual fixes; none of them add "
                    + "a creature that spawns naturally."),
            HistoricalFeatureExclusions.BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_2_5", "Release 1.2.5", "release",
            key(Era.RELEASE.base(), 1, 2, 5), "moderner_beta:release_1_2_5",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Adds ocelots/cats and iron golems, and zombie sieges with villager breeding and transformation. "
                    + "Only the OCELOT is added to the spawn set: cats are a separate entity type in 1.21.1 and did "
                    + "not spawn naturally until 1.14, so including them would put a modern mob in a Release 1.2 "
                    + "world. The iron golem is included because the research names it for this build."),
            HistoricalFeatureExclusions.RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_ORES));
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_6_4", "Release 1.6.4", "release",
            key(Era.RELEASE.base(), 1, 6, 4), "moderner_beta:release_1_6_4",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_6_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Adds horses and variants, donkeys and mules, and the research states that horses spawn naturally in "
                    + "plains and savanna. Horse and donkey are added to the spawn set; the MULE is not, because it "
                    + "is bred rather than spawned. Releases 1.3 and 1.5 add no mob, and 1.4 adds witch, bat, wither "
                    + "skeleton and zombie villager, which this set inherits through RELEASE_1_4_MOBS. It uses the "
                    + "1.5 exclusion list because quartz, added in 1.5, is content this build had."),
            HistoricalFeatureExclusions.RELEASE_1_5_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_EMERALD));

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
        // ---- Indev 20100223: the oldest version this build offers, and the only finite one ---------------
        //
        // Asked for by the owner. The earlier withdrawal note said Classic AND Indev do not generate; that verdict
        // was measured on CLASSIC, which hangs on chunk (0,0) even from Moderner Beta's own preset. Indev is a
        // different provider path and was re-measured as its own case - run/indevprobe.out shows it generating: spawn
        // prepared in 1.25 s, biome moderner_beta:indev_normal, border 256 centred on the origin. Classic stays
        // withdrawn. Rules start from Infdev's, as instructed, adjusted to Indev's own record.
        versions.add(terrainOnlyWithRules(
            "historical_experience:indev_latest", "Indev 20100223", "indev",
            Era.INDEV.base() + 223,
            "moderner_beta:indev",
            new VersionRules(
                true,
                HistoricalSpawnRules.INDEV_20100223_MOBS,
                HistoricalLootRules.INDEV_20100223_BY_ENTITY,
                HistoricalFoodRules.INDEV_20100223_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "FINITE ERA, and the only one this project offers. Generation inherits the real upstream "
                    + "moderner_beta:indev preset - provider moderner_beta:finite_2d, finite level 256x256x128, theme "
                    + "normal, world border 256 centred on the origin with the ocean falloff, out-of-bounds biome "
                    + "minecraft:the_void - with this project's overrides: deepslate off, cave biomes off, and the "
                    + "preset's stronghold placement override REMOVED, because a stronghold at distance 0 would put "
                    + "Release 1.0's structure in the middle of a 256-block island. The envelope is NOT taken from the "
                    + "overworld_128 override the Infdev and Alpha presets use: this provider reads its own finite_2d "
                    + "noise settings and its height comes from finite_level_properties. MOBS: the same seven as Infdev "
                    + "(creeper, skeleton, spider, giant, zombie, pig, sheep), adopted from the next measured era "
                    + "because no research row adds or removes an Indev species; mob armour and arm-swinging are "
                    + "absent because the research removes both in Indev 20100202. DROPS: Indev 20100219's tables - "
                    + "zombie 0-2 feathers, skeleton 0-2 arrows, pig 0-2 raw porkchops, spider 0-2 string; bones are "
                    + "Beta 1.2, rotten flesh Beta 1.8. FOOD: this exact jar registers bread 5, stew 10, raw porkchop "
                    + "3 and cooked porkchop 8. Neither Classic mushroom eating nor an apple registration is inherited "
                    + "into this build. Exact crafting patterns/counts and six smelts are artifact-extracted. "
                    + "SIMULATION: no hunger, no sprinting, no experience, no structures.")));
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
            key(Era.ALPHA.base(), 1, 0, 1), "moderner_beta:alpha",
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
            key(Era.ALPHA.base(), 1, 0, 4),
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
            key(Era.ALPHA.base(), 1, 0, 6), "moderner_beta:alpha",
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
            key(Era.ALPHA.base(), 1, 0, 11), "moderner_beta:alpha",
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
            HistoricalFeatureExclusions.BEFORE_LAPIS_EXISTED));

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
            key(Era.ALPHA.base(), 1, 0, 14), "moderner_beta:alpha",
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
            HistoricalFeatureExclusions.BEFORE_LAPIS_EXISTED));

        // Alpha 1.0.17 and 1.1.0 add NO mob between them, so 1.0.17, 1.1.0 and v1.1.2_01 share one spawn set. That
        // is a fact read off three consecutive sets of deltas, not a shortcut - and it is why 1.0.14's slime-less
        // set is NOT reused here: 1.0.14 removed slimes by a bug, and neither of these builds inherits that.
        versions.add(terrainOnlyWithRules(
            "historical_experience:alpha_1_0_17", "Alpha v1.0.17", "alpha",
            key(Era.ALPHA.base(), 1, 0, 17), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_1_2_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "Oak fences, the spider jockey, a working multiplayer day/night cycle, pigs, chickens and slimes "
                    + "brought to multiplayer, milk buckets that can be emptied, and chickens that no longer take "
                    + "fall damage and glide downward. Slime spawning is BACK, because 1.0.14 disabled it as a bug "
                    + "and the research says not to infer a permanent removal. This build also had a transient "
                    + "defect - broken or invisible inventory icons - which the deltas record as a short-lived "
                    + "client fault rather than a stable mechanic, so it is deliberately NOT reproduced. Food "
                    + "table shared with Alpha 1.0.1: no edible item is added between them."),
            HistoricalFeatureExclusions.BEFORE_LAPIS_EXISTED));

        versions.add(terrainOnlyWithRules(
            "historical_experience:alpha_1_1_0", "Alpha v1.1.0", "alpha",
            key(Era.ALPHA.base(), 1, 1, 0), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_1_2_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "The compass, pointing to world spawn; a shared client/server inventory; inventory persisted on a "
                    + "multiplayer server; player location persisted on disconnect; chests, signs and furnaces "
                    + "working in multiplayer; and an experimental switch that enables monsters in multiplayer. "
                    + "Monster spawners stop rendering the mob they contain. Adds no mob, so the spawn set is Alpha "
                    + "1.0.17's. The multiplayer monster switch is described as EXPERIMENTAL and is not evidence of "
                    + "parity with singleplayer AI, which is recorded rather than smoothed over. This is also the "
                    + "build that makes SNEAKING possible: this project's sneak boundary is Alpha 1.1.0, supplied "
                    + "by the owner, and it lands exactly here. Food table shared with Alpha 1.0.1."),
            HistoricalFeatureExclusions.BEFORE_LAPIS_EXISTED));

        // Alpha 1.2.0, the Halloween Update and the last Alpha build. Three things converge here:
        //
        //  * The Nether arrives, so the portal gate that has been coded against ALPHA_1_2_0_CHRONOLOGY since it was
        //    written finally has a DECLARED BUILD behind its literal. Before this round, "Alpha 1.2.0" was a number
        //    tied to the registry's scheme by a test; now it is a version.
        //  * The block dates for netherrack, glowstone, the portal, pumpkins and jack-o'-lanterns were held at
        //    beta_1_7_3 as a proxy while this build did not exist. They are re-pointed to it, which is the same
        //    payoff Alpha 1.0.11 gave for clay and sugar cane.
        //  * The selectable WINTER WORLD MODE is REMOVED, replaced by biome-based weather. That is the design
        //    decision parked in the coverage matrix: the winter variant is a world-creation CHOICE, not a newer
        //    revision, and this build ends the window in which it existed - Alpha 1.0.4 through Alpha 1.1.0.
        versions.add(terrainOnlyWithRules(
            "historical_experience:alpha_1_2_0", "Alpha v1.2.0", "alpha",
            key(Era.ALPHA.base(), 1, 2, 0), "moderner_beta:alpha",
            new VersionRules(
                true,
                HistoricalSpawnRules.ALPHA_1_2_0_MOBS,
                java.util.Map.of(),
                HistoricalFoodRules.ALPHA_1_1_2_HEAL_POINTS,
                false,
                false,
                false,
                false,
                "The Halloween Update: the Nether, reachable through a player-built obsidian portal, with netherrack, "
                    + "soul sand and glowstone; pumpkins and jack-o'-lanterns; clocks; the fishing rod and raw fish; "
                    + "ghasts and zombie pigmen, the pigmen neutral until attacked and then retaliating as a group; "
                    + "twelve listed biomes with biome-coloured terrain, of which Ice Desert is listed but unused; six "
                    + "music tracks; and an Overworld:Nether distance mapping of 8:1. Monoliths cease and the bottom "
                    + "layer becomes all bedrock. Nether water evaporates. Slime natural spawning is REMOVED from the "
                    + "mob spawning code - not described as a bug this time, so it is taken at face value - and the "
                    + "selectable winter world mode is REMOVED in favour of biome-based weather. NOT implemented: the "
                    + "biome count and biome-coloured terrain, because Moderner Beta ships NO Alpha 1.2.0 preset and "
                    + "the closest, moderner_beta:alpha, is the 1.1.2-era terrain; the depth-noise scale change from "
                    + "100 to 200, which the research itself labels low-confidence and development-preview; and the "
                    + "fishing rate, whose semantics the research records as unverified. Terrain therefore comes from "
                    + "moderner_beta:alpha. Food table shared with Alpha 1.0.1: no edible item is added between them."),
            HistoricalFeatureExclusions.BEFORE_LAPIS_EXISTED));

        // Alpha v1.2.0 gets a NEW revision whose BIOME SETTINGS come from Beta 1.1, because the owner's rule is that
        // this build's generation is the same as Beta 1.1's - their biome generators are the same.
        //
        // That matters because it is NOT a single-biome world. Moderner Beta's `beta_1_1_02` biome settings use
        // `"moderner_beta:provider": "moderner_beta:beta"`, a climate-based generator with a mapping table from climate
        // to biome (`plains` -> `beta_plains`, `taiga` -> `beta_oak_taiga`, and so on), so Alpha v1.2.0 now generates
        // plains, taiga, desert, forest, rainforest and the rest rather than one biome everywhere. An earlier revision
        // here set `single_biome` instead, which did make the BIOME READ as Beta plains but did it by replacing the
        // whole provider - flattening every chunk the world generated afterwards. The owner rejected that, twice and
        // for both halves of this range.
        //
        // The chunk settings are deliberately left alone: only the biome settings are taken from Beta 1.1, so the
        // terrain foundation stays `moderner_beta:alpha` and the height envelope does not move.
        //
        // It is a revision rather than an edit to `_no_modern_content` for the project's usual reason: that alias is
        // published, and a world created against it recorded the terrain it was given.
        versions.set(versions.size() - 1, withBetaElevenBiomes(
            versions.get(versions.size() - 1), "historical_experience:alpha_1_2_0_beta_plains"));

        // Alpha v1.2.2a, requested by the owner: "in alpha 1.2.2a you could press F4 to spawn a portal. add this as a
        // clone of 1.2.0 but add that feature". The owner then chose it as the BANNER version for the Alpha 1.2 line,
        // so it is the selectable default for this era the way beta_1_7_3 is for Beta 1.7.
        //
        // The F4 feature is VERIFIED, not taken on faith. Both clients were downloaded and SHA-1 checked against
        // Mojang's own manifest, then decompiled:
        //
        //   * Alpha 1.2.0 has F11 (87), F3 (63) and no F4 at all.
        //   * Alpha 1.2.2a has F11 (87), F4 (62) and F3 (63). Minecraft.java:829 reads
        //       if (Keyboard.getEventKey() == 62) { new nj().c(this.e, this.g); continue; }
        //     and nj.c searches 16 blocks around the player for a floor with headroom, picks an orientation from
        //     nextInt(4) and builds an obsidian (nl.ap) frame with nl.be portal blocks inside, setting cw.i = true
        //     while it does - the flag that places a portal block LIT. nj.b, the method that finds an existing portal
        //     within 128 blocks and MOVES the player to it, is not called, so F4 rebuilds a lit portal where you
        //     stand rather than teleporting.
        //
        // So the chronology descends from 1.2.0 rather than from 1.2.6: the build letter is not a number, and this
        // places 1.2.2a exactly where it belongs in the order - after 1.2.0 and before anything else. Everything
        // else is a clone of 1.2.0, INCLUDING its three generation revisions and its description, because 1.2.2a was
        // a fix release and the Halloween Update content it inherits is the point. The rules are taken from the 1.2.0
        // version object itself rather than restated, so the two cannot drift; only the identity moves.
        versions.add(renamedClone(
            versions.get(versions.size() - 1),
            "historical_experience:alpha_1_2_0",
            "historical_experience:alpha_1_2_2a",
            "Alpha v1.2.0",
            "Alpha v1.2.2a",
            key(Era.ALPHA.base(), 1, 2, 2)));

        // The Release 1.3 / 1.4 / 1.5 gap, and it needs NO approximation - which is the opposite of what I expected
        // when I flagged it as needing a decision. The research states it directly: Release 1.4 says "no major
        // terrain generator change; Nether fortresses inherited", and Release 1.5 says "no new biome, structure or
        // mob identified". The generator did not change between 1.2.5 and 1.7, so moderner_beta's release_1_2_5
        // preset IS the right terrain for these builds rather than a stand-in for one.
        //
        // What they add is CONTENT, which this project handles by rule rather than by preset: desert pyramids and
        // jungle temples in 1.3, witch huts and infested monster eggs in 1.4, nether quartz ore in 1.5.
        //
        // They use terrainOnly, matching the other Release builds, so they carry NO spawn restriction and therefore
        // inherit the known Release-line defect: their mob sets are not researched, and SpawnRestrictionSemanticsTest
        // forbids inventing one. Recorded as U in the coverage matrix rather than papered over.
        // The last three Release profiles to gain researched mob sets, which completes the fix for the largest
        // content defect this project had: every one of these was declared terrain-only, so its world spawned
        // modern 1.21.1 mobs including piglins and hoglins. Each set is inherited from the build before it plus
        // whatever the research names for the interim, and none of them is invented.
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_3", "Release 1.3", "release",
            key(Era.RELEASE.base(), 1, 3, 0), "moderner_beta:release_1_2_5",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Adds no mob: its deltas are tripwire, the ender chest, emeralds and villager trading, cocoa pods, "
                    + "the desert pyramid and jungle temple, Large Biomes, adventure mode and XP from mining ores. So "
                    + "the spawn set is Release 1.2.5's, which already carries the ocelot and iron golem."),
            HistoricalFeatureExclusions.RELEASE_1_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_EMERALD));
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_4", "Release 1.4", "release",
            key(Era.RELEASE.base(), 1, 4, 0), "moderner_beta:release_1_2_5",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_4_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Adds witch, bat, wither skeleton and zombie villager, and witch huts generating in swamps with "
                    + "witches associated to them. The WITHER is not added to the spawn set: it is summoned from soul "
                    + "sand and three wither skeleton skulls rather than spawning naturally. Also anvils, item frames, "
                    + "flower pots, walls, carrots and potatoes, the beacon and Nether Star, and infested monster eggs "
                    + "in Extreme Hills."),
            HistoricalFeatureExclusions.RELEASE_1_3_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_EMERALD));
        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:release_1_5", "Release 1.5", "release",
            key(Era.RELEASE.base(), 1, 5, 0), "moderner_beta:release_1_2_5",
            new VersionRules(
                true,
                HistoricalSpawnRules.RELEASE_1_4_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                true,
                true,
                true,
                true,
                "Adds no mob: its deltas are the comparator, hopper, dropper, trapped chest, weighted pressure "
                    + "plates, daylight sensor, redstone block, activator rail and nether quartz ore, plus a lighting "
                    + "rewrite. So the spawn set is Release 1.4's, which carries the witch, bat, wither skeleton and "
                    + "zombie villager. NETHER QUARTZ IS CONTENT THIS BUILD HAD, which is why it uses the 1.5 "
                    + "exclusion list rather than the earlier one."),
            HistoricalFeatureExclusions.RELEASE_1_5_WITHOUT_MODERN_VANILLA_FEATURES_KEEPING_EMERALD));

        // Beta 1.0, the first Beta. Its terrain foundation is NOT an approximation, and that was verified rather
        // than assumed: the research says for this build "existing climate-era generator (pre-Beta-1.8) persists",
        // for Beta 1.4 "no terrain generator change", and for Beta 1.5 "no new base generator". The generator held
        // steady from Alpha 1.2 until the Adventure Update overhauled it in Beta 1.8, so moderner_beta:beta is the
        // CORRECT preset for this and every Beta build up to 1.7.3 rather than the nearest available one.
        //
        // Declared through the rules-carrying helper so its spawns are restricted, which the research supports:
        // squid arrive in Beta 1.2 and wolves in Beta 1.4, so this build has neither.
        //
        // The _no_grass revision the helper creates IS correct here, unlike on the Beta 1.9 prereleases: grass
        // patches arrive in Beta 1.6, so a Beta 1.0 world genuinely had none. That is the same fact that makes the
        // revision wrong three years later.
        //
        // Recorded as NOT enforced: the palette-derived block dates. This build has no extracted palette and the
        // nearest is Beta 1.7.3's, whose block set includes things Beta 1.0 did not have. Borrowing it would be
        // inventing a claim, so timeline-stated dates are enforced here and palette-derived ones are not - the same
        // principled position SpawnRestrictionSemanticsTest takes for unresearched mob sets.
        versions.add(terrainOnlyWithRulesWithHistoricalEnvelope(
            "historical_experience:beta_1_0", "Beta 1.0", "beta",
            key(Era.BETA.base(), 1, 0, 0), "moderner_beta:beta",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_0_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                // Beta 1.0 predates all of these. Experience, sprinting and the hunger system all arrive with the
                // Adventure Update in Beta 1.8, and I initially set them true by reflex - three tests failed on it
                // at once, which is what those flags are for. hasStructures is TRUE because the Nether is inherited
                // and its fortress rule is non-empty; claiming no structures while declaring a rule that allows one
                // is the contradiction HistoricalStructureRulesTest exists to catch.
                false,
                false,
                false,
                true,
                "Item-name tooltips, cape and deadmau5 ear support, leaf decay REINTRODUCED with different behaviour "
                    + "from its pre-Beta form, throwable eggs at stack size 16 with a 12.5% chicken spawn chance, and "
                    + "a server-side functional inventory. Lava lakes replace neighbouring blocks with stone, the "
                    + "grassland/sand biome boundary is MISALIGNED until Beta 1.3, and the Nether is inherited with "
                    + "no strongholds, villages or mineshafts. Armour-slot outlines are removed, crafting-grid slots "
                    + "no longer hold items, and tools on the ground stop repairing themselves. No mob is added here."),
            HistoricalFeatureExclusions.BETA_1_0_WITHOUT_LAPIS));
        // Beta 1.0 generates NO sandstone, and this is a fourth revision rather than an edit to
        // `_historical_envelope` for the project's usual reason: that alias is published and a world created against
        // it recorded the terrain it was given, so rewriting its preset would move the terrain of the chunks that
        // world has not generated yet.
        //
        // The defect was measured, not inferred. A fresh Beta 1.0 world held 2,993 sandstone blocks in nine chunks
        // while every other declared version held none, because the shared `moderner_beta:beta` foundation enables
        // `surface_properties.generateSandstone` and this version used that foundation unchanged. Two independent
        // research entries date the block and the rule: version-transitions.json lists sandstone among Beta 1.2's
        // additions, and "sandstone generates under naturally generated sand" is a Beta 1.3 behaviour. Beta 1.0 had
        // neither.
        //
        // The mechanism is a supported setting and not a hook: Moderner Beta serialises its surface behaviour into
        // the settings preset, and its own `alpha` preset sets this same flag to false while `beta` sets it true -
        // read from the shipped preset JSON, not guessed. So this version's preset declares the full surface object
        // with that one flag flipped, and the key it is registered under here is what names it.
        versions.set(versions.size() - 1, withSurfaceSandstoneDisabled(
            versions.get(versions.size() - 1), "historical_experience:beta_1_0_no_sandstone"));

        // Beta 1.2. Two things converge, and both are the payoff of declaring a build rather than approximating it:
        // the SQUID arrive here, and lapis lazuli's date becomes true - it was held at beta_1_7_3 as a proxy while
        // 1.2 did not exist, while its own evidence read "lapis lazuli ore introduced Beta 1.2".
        versions.add(terrainOnlyWithRulesWithHistoricalEnvelope(
            "historical_experience:beta_1_2", "Beta 1.2", "beta",
            key(Era.BETA.base(), 1, 2, 0), "moderner_beta:beta",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_2_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                false,
                false,
                false,
                false,
                "Charcoal, note blocks, dyes and coloured wool, sugar, dispensers, bone and bone meal, sandstone, "
                    + "birch and spruce logs and leaves, cake, and lapis lazuli with its block and ore. The squid "
                    + "arrive, with 10 health and an ink sac, no sounds yet, and milkable in this build - which is "
                    + "recorded as a quirk rather than reproduced. Sugar cane is renamed from reeds and the crafting "
                    + "table from workbench; tool durability rises across the board, wood 33 to 59, stone 65 to 131, "
                    + "iron 129 to 250, diamond 1025 to 1561; skeletons start dropping bones; spiders climb walls; "
                    + "sheep can be dyed; bone meal fertilises saplings and wheat. World generation adds birch trees "
                    + "to forests, replaces oak with spruce in taiga, and introduces lapis ore - whose frequency and "
                    + "height were adjusted in Beta 1.2_02, which must NOT be backdated into this build. Cocoa beans "
                    + "and brown wool are registry content obtainable only by external tools here, so they are "
                    + "recorded rather than given a survival route. IMPLEMENTED: the tool durability table this "
                    + "entry used to record as missing is applied by HistoricalToolDurability, so wood 33, stone 65, "
                    + "iron 129 and diamond 1025 hold for every declared version older than this one and this build "
                    + "itself gets the raised values."),
            HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS));
        // Beta 1.2 HAS sandstone but must not GENERATE it under sand, and the Sandstone History table states those
        // two dates separately: `1.2` is "Added sandstone." and `1.3` is "Sandstone is now found below three blocks
        // of naturally generated sand." This version used `beta_1_2_historical_envelope`, whose foundation enables
        // the rule, so a fresh Beta 1.2 world generated 2,993 sandstone its own build could not have placed.
        //
        // A new revision rather than an edit, for the same reason Beta 1.0's was: `_historical_envelope` is
        // published. Beta 1.3 KEEPS its sandstone - the rule is genuine from that build - so this is deliberately a
        // two-version change and not a blanket one.
        versions.set(versions.size() - 1, withSurfaceSandstoneDisabled(
            versions.get(versions.size() - 1), "historical_experience:beta_1_2_no_sandstone"));

        // Beta 1.3. Beds, redstone repeaters and slabs arrive, the grassland/sand biome boundary that Beta 1.0
        // misaligned is FIXED, and slimes spawn again after being removed in Alpha 1.2.0 - so this build's mob set
        // differs from Beta 1.2's by exactly the slime, which my Beta 1.2 entry got wrong until these deltas were
        // read.
        versions.add(terrainOnlyWithRulesWithHistoricalEnvelope(
            "historical_experience:beta_1_3", "Beta 1.3", "beta",
            key(Era.BETA.base(), 1, 3, 0), "moderner_beta:beta",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_3_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                false,
                false,
                false,
                false,
                "Beds with sleeping and time skip, redstone repeaters, cobblestone, wood and sandstone slabs, a "
                    + "smooth stone slab that is present but unobtainable, the 'Skull on Fire' painting, world seed "
                    + "and name selection with multiple world slots, and a smooth-lighting engine. Sandstone now "
                    + "generates under naturally generated sand; the grassland/sand biome boundary Beta 1.0 left "
                    + "misaligned is FIXED here; stone slabs move to the stone recipe and double slabs drop two; the "
                    + "stone pressure plate recipe drops from three materials to two; gunpowder is renamed from "
                    + "sulphur; powered redstone dust darkens with distance from its source; and squid swim in 3D "
                    + "instead of sitting on the bottom, and can no longer be milked - a Beta 1.2 quirk this build "
                    + "REMOVES. SLIMES SPAWN AGAIN, having been removed from the spawning code in Alpha 1.2.0. Bed "
                    + "respawn-point reset is explicitly NOT in this build; it arrives bugged in Beta 1.4."),
            HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS));

        // Beta 1.4, 1.5 and 1.6: the last three unimplemented targets of the expansion matrix.
        //
        // The terrain question is the same one Beta 1.0-1.3 answered, and the research answers it the same way:
        // Beta 1.4 and 1.5 record "no terrain generator change", and Beta 1.6 records additions to the EXISTING
        // generator (grass patches, ferns, desert dead bushes) rather than a new one. So `moderner_beta:beta` is
        // the CORRECT foundation for all three, not an approximation - the generator was not overhauled until the
        // Beta 1.8 Adventure Update.
        //
        // Grass is the reason two different helpers are used. Short grass and ferns arrive in Beta 1.6, so a Beta
        // 1.4 or 1.5 world genuinely had none and the helper offering a _no_grass revision is right for them;
        // Beta 1.6 HAD them, so it uses the grass-keeping helper exactly as every Release profile does. Each
        // carries its own spawn set rather than sharing Beta 1.3's, because Beta 1.4 adds the wolf.
        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_4", "Beta 1.4", "beta",
            key(Era.BETA.base(), 1, 4, 0), "moderner_beta:beta",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_4_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                false,
                false,
                false,
                true,
                "Adds the WOLF - neutral, common in taiga, 10 health, tamed with bones at a 1/3 chance per bone, "
                    + "defending its owner and going angry when the owner or the owner's wolves are hit, its tail "
                    + "showing health, feedable raw pork, and DROPPING NOTHING when killed. Also cookies (healing "
                    + "1 health and stacking to 8, the first stackable food), the April Fools locked chest, bed "
                    + "spawn-position reset - added here but BUGGED, only applying on 'Leave bed' and only working "
                    + "for multiplayer - partly implemented achievement infrastructure, the E inventory key, a "
                    + "changeable GUI scale, faster wheat growth, spiders no longer trampling crops, spider sounds "
                    + "REMOVED, the zombie/spider one-block-above damage bug fixed, and ladders holding position "
                    + "while sneaking. NO terrain generator change."),
            HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS));

        versions.add(terrainOnlyWithRules(
            "historical_experience:beta_1_5", "Beta 1.5", "beta",
            key(Era.BETA.base(), 1, 5, 0), "moderner_beta:beta",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_5_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                false,
                false,
                false,
                true,
                "Adds WEATHER - natural rain, snowfall and thunderstorms with their own sounds - plus birch and "
                    + "spruce saplings, detector rails, powered rails that need redstone and accelerate minecarts, "
                    + "cobweb (present but obtainable only by inventory editing, slowing players and mobs and "
                    + "dropping string when broken with a sword), and sixteen achievements and statistics with "
                    + "their UI. Unarmed damage rises 1 -> 2 health points; saplings smelt 0.5 items each; redstone "
                    + "wire can be placed on snow; the ladder recipe yields 2; lightning creates fire and turns "
                    + "struck pigs into zombie pigmen and struck creepers into charged creepers; wolves become more "
                    + "common outside taiga and forest. NO mob is added and there is NO new base generator."),
            HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS));

        versions.add(releaseTerrainOnlyWithRules(
            "historical_experience:beta_1_6", "Beta 1.6", "beta",
            key(Era.BETA.base(), 1, 6, 0), "moderner_beta:beta",
            new VersionRules(
                true,
                HistoricalSpawnRules.BETA_1_6_MOBS,
                java.util.Map.of(),
                java.util.Map.of(),
                false,
                false,
                false,
                true,
                "Adds the map item, the wooden trapdoor, dead bush (deserts only, placeable only on sand), and "
                    + "SHORT GRASS AND FERNS as naturally generated vegetation - grass patches on most grassy-soil "
                    + "biomes, ferns only in swamps and rainforests. Also Nether support in multiplayer with the "
                    + "view-distance and allow-nether server properties, and grass/foliage colour-map files that "
                    + "exist but are initially not read. A bed placed in the Nether EXPLODES; fire spread is "
                    + "severely slowed and no longer spreads infinitely; ice regenerates without snow; mushrooms "
                    + "spread slowly; below a layer of source blocks water no longer behaves as source; the player "
                    + "dismounts from a boat or minecart entering a portal, fixing the portal-ride crash; the "
                    + "ten-tick missed-swing cooldown is REMOVED; and public Beta 1.6 fixes the ore-generation "
                    + "quadrant bias that made some ores at least 50% more common to the south-east. NO mob is "
                    + "added and there is NO new base generator. The unreleased Test Build 3's randomised "
                    + "vegetation and its clay x=z coordinate defect are NOT this build."),
            HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS));

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

        // ---- The era ore population, for every version that does not already place its own ----------
        //
        // Applied at the END, over the finished list, on purpose: a version's newest revision is whatever
        // the declarations above produced (the _no_sandstone, _beta_plains, _with_ores and
        // _no_modern_content aliases), and the era-ore revision must inherit from THAT revision rather than
        // from a guessed one. Doing it here also covers Alpha v1.2.2a, which is a renamed clone of 1.2.0:
        // both get their own era-ore revision, named for their own id, inheriting the same biome settings.
        //
        // Beta 1.7.3 and Beta 1.8.1 are skipped because they already place a population of their own; a
        // second one would double their ores. Every other declared version - the six Beta 1.9
        // prereleases, the seven Release profiles, Beta 1.0 to 1.6, the ten Alpha builds and the three
        // pre-Beta builds - gained nothing but vanilla's modern placement before this.
        for (int index = 0; index < versions.size(); index++) {
            HistoricalVersion version = versions.get(index);
            if (!version.latestRevision().placesOwnOres()) {
                versions.set(index, withEraOres(version));
            }
        }


        List<HistoricalVersion> declared = List.copyOf(versions);
        checkUnique(declared);
        return declared;
    }

    /** Two versions may never share a profile id or a recipe alias: both are persisted, so a clash is ambiguous. */
    private static void checkUnique(List<HistoricalVersion> versions) {
        Map<String, HistoricalVersion> byId = new LinkedHashMap<>();
        Map<String, HistoricalVersion> byAlias = new LinkedHashMap<>();
        for (HistoricalVersion version : versions) {
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
    }

    /** The position of a build on the shared timeline, from its era and number. */
    private static int key(int eraBase, int major, int minor, int patch) {
        return eraBase + major * 1000 + minor * 100 + patch;
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
            + " FOOD: six registered food instances, proved by this build's item constructors, stew subclass, "
            + "pig drop and six-entry smelting function; health restored directly (4/5/10/3/8/42). "
            + "The previous zero-call-site conclusion was false: 20100611 r.a(stack,world,player) calls player.b(heal). "
            + "LOOT: feather/arrow/string/gunpowder/raw pork, never modern bones, flesh, meat-on-fire or rare drops. "
            + "CRAFTING: exact per-build patterns, ingredients and counts, not the Alpha allow-list. "
            + "See research/early-gameplay-audit.md.";
        return terrainOnlyWithRules(id, displayName, "infdev", Era.INFDEV.base() + buildNumber, upstreamPreset,
            new VersionRules(
                restrictsSpawns,
                mobs,
                HistoricalLootRules.INDEV_20100223_BY_ENTITY,
                HistoricalFoodRules.INFDEV_HEAL_POINTS,
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
            id, displayName, "alpha", key(Era.ALPHA.base(), 1, 1, 2), upstreamPreset,
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
     * Beta 1.0, 1.2 and 1.3: {@link #terrainOnlyWithRules}' shape plus a revision that gives the world the
     * historical envelope.
     *
     * <p>This exists for a MEASURED defect, not a precaution. Those three builds' settings presets inherited
     * {@code moderner_beta:beta} without stating the envelope or disabling deepslate, so their worlds generated at
     * the modern -64..320 height with deepslate in them. The runtime probe measured it directly: a Beta 1.3 world
     * (alias {@code beta_1_3_no_modern_content}) reported {@code generatorMinY=-64}, {@code generatorDepth=192},
     * {@code deepslateDisabled=false} and the voronoi cave provider injecting dripstone and lush caves, and found
     * <b>8,366 deepslate and 424 emerald ore</b> in nine sampled chunks - every one of them anachronistic. Beta
     * 1.7.3's own {@code content_preview_*} presets already carried the correct envelope, which is what proved the
     * omission was in these presets rather than a Moderner Beta limitation.
     *
     * <p>It is a NEW revision rather than an edit to {@code _no_modern_content}, for the project's usual reason:
     * that alias is published, worlds may already be recorded against it, and rewriting its preset would move the
     * terrain of chunks those worlds have not generated yet. A new world selects the new revision; an existing one
     * keeps the generator it recorded.
     */
    private static HistoricalVersion terrainOnlyWithRulesWithHistoricalEnvelope(
        String id,
        String displayName,
        String era,
        int chronology,
        String upstreamPreset,
        VersionRules rules,
        List<Exclusion> modernContentExclusions
    ) {
        HistoricalVersion base =
            terrainOnlyWithRules(id, displayName, era, chronology, upstreamPreset, rules, modernContentExclusions);
        List<HistoricalVersion.GenerationRevision> revisions = new java.util.ArrayList<>(base.revisions());
        revisions.add(new HistoricalVersion.GenerationRevision(
            revisions.size(),
            alias(id.substring(id.indexOf(':') + 1) + "_historical_envelope"),
            upstreamPreset,
            true,
            "the historical 0..127 envelope with deepslate disabled and no modern cave biomes, which this version's "
                + "earlier revisions were missing: their preset inherited moderner_beta:beta without stating either, "
                + "so the probe measured deepslate and modern cave biomes generating in a Beta world",
            base.revisions().get(base.revisions().size() - 1).excludedPlacedFeatures(),
            List.of()));
        return new HistoricalVersion(id, displayName, era, chronology, rules, List.copyOf(revisions));
    }

    /**
     * Adds a revision that turns Beta sandstone generation OFF for one version, keeping everything else identical.
     *
     * <p>Written as a helper rather than inlined so the shape is visible: the new revision inherits the previous
     * revision's exclusion list by reference and adds no ore population of its own, because the only thing it
     * changes is a surface setting inside the settings preset named by {@code alias}. Two things must therefore
     * agree, and a test catches it if they do not: this alias and the preset JSON file of the same name.
     */
    private static HistoricalVersion withSurfaceSandstoneDisabled(HistoricalVersion base, String alias) {
        List<HistoricalVersion.GenerationRevision> revisions = new java.util.ArrayList<>(base.revisions());
        HistoricalVersion.GenerationRevision previous = revisions.get(revisions.size() - 1);
        revisions.add(new HistoricalVersion.GenerationRevision(
            revisions.size(),
            alias,
            previous.foundation(),
            true,
            "no sandstone: the shared foundation enables surface sandstone generation, which is Beta 1.3 "
                + "behaviour, and sandstone is itself a Beta 1.2 addition - this version had neither",
            previous.excludedPlacedFeatures(),
            previous.orePopulation()));
        return new HistoricalVersion(base.id(), base.displayName(), base.era(), base.chronology(), base.rules(),
            List.copyOf(revisions));
    }

    /**
     * A new revision whose preset takes Beta 1.1's biome settings, leaving the terrain foundation and every other
     * setting inherited unchanged.
     *
     * <p>Written as a helper for the same reason as {@link #withSurfaceSandstoneDisabled}: the shape is the point. The
     * new revision inherits the previous revision's exclusion list and ore population by reference because the only thing
     * it changes is the biome settings inside the settings preset named by {@code alias}. Two things must agree, and a
     * test catches it if they do not: this alias and the preset JSON file of the same name.
     *
     * <p>It exists for Alpha v1.2.0, whose generation the owner requires to match Beta 1.1's. That is a MULTI-biome
     * climate generator, not a single biome, which is why this is not {@code withSingleBiome}.
     */
    private static HistoricalVersion withBetaElevenBiomes(HistoricalVersion base, String alias) {
        List<HistoricalVersion.GenerationRevision> revisions = new java.util.ArrayList<>(base.revisions());
        HistoricalVersion.GenerationRevision previous = revisions.get(revisions.size() - 1);
        revisions.add(new HistoricalVersion.GenerationRevision(
            revisions.size(),
            alias,
            previous.foundation(),
            true,
            "biome settings from moderner_beta:beta_1_1_02, because Alpha v1.2.0 and Beta 1.1 share a biome generator",
            previous.excludedPlacedFeatures(),
            previous.orePopulation()));
        return new HistoricalVersion(base.id(), base.displayName(), base.era(), base.chronology(), base.rules(),
            List.copyOf(revisions));
    }

/**
     * The era population for a version: the verified Beta 1.7.3 population filtered to the ores that era
     * had.
     *
     * <p>This is the single derivation. {@link #withEraOres} stores its result in a revision, and the
     * runtime fallback used by an older revision that declares no population calls the same method - so
     * an old world and a newly created one cannot disagree about which ores an era had.
     */
    public static List<OreRule> eraOrePopulation(HistoricalVersion version) {
        return BetaOreRules.eraPopulation(
            version.isAtOrAfter(net.historicalexperience.version.Milestones.ALPHA_1_0_1),
            version.isAtOrAfter(net.historicalexperience.version.Milestones.BETA_1_2));
    }

    /**
     * Appends the <b>era ore</b> revision: the version's own ore population, with the modern vanilla ore
     * features switched off.
     *
     * <h2>The defect this fixes</h2>
     *
     * <p>A historical world whose version placed no population of its own was filled entirely by
     * vanilla 1.21.1's modern placed features, so it generated at modern heights. Measured on a fresh
     * {@code release_1_2_5} world: diamond y=1..72 (1,180 of 1,795 above y=15), redstone y=1..30, lapis
     * y=1..63, gold y=1..30, iron y=1..71 and coal y=1..80. Every version this project declares predates
     * the 1.18 ore overhaul, and the verified Beta 1.7.3 population puts diamond at y 0..15.
     *
     * <h2>Why a new revision, and why a shared derivation</h2>
     *
     * <p>Revisions are append-only. The thirteen profiles that were measured with no ores at all
     * ({@code _no_modern_content}) and the pre-Beta {@code _with_ores} revisions are published aliases,
     * so their exclusions and populations cannot be edited: a world recorded against one has to keep
     * generating what it recorded. This appends one revision per version instead, and because a new
     * world selects the newest revision, a newly created world picks it up with no further change.
     *
     * <p>The population is the verified Beta 1.7.3 one filtered to the ores the era had. It is NOT a new
     * set of numbers: ore generation did not change across this range, so the only question per version
     * is availability, and the boundaries are the block availability table's own (redstone Alpha
     * v1.0.1, lapis Beta 1.2, emerald Release 1.3.1). Diamond needs no boundary here because every
     * declared version is after its Infdev 20100320 restoration - see
     * {@link BetaOreRules#eraPopulation}.
     */
    private static HistoricalVersion withEraOres(HistoricalVersion base) {
        List<HistoricalVersion.GenerationRevision> revisions = new java.util.ArrayList<>(base.revisions());
        HistoricalVersion.GenerationRevision previous = revisions.get(revisions.size() - 1);
        boolean emerald = base.isAtOrAfter(net.historicalexperience.version.Milestones.RELEASE_1_3);
        List<OreRule> population = eraOrePopulation(base);
        revisions.add(new HistoricalVersion.GenerationRevision(
            revisions.size(),
            alias(base.id().substring(base.id().indexOf(':') + 1) + "_era_ores"),
            previous.foundation(),
            true,
            "the era's own ore population: the verified Beta 1.7.3 vein shape, sizes and attempt counts, "
                + "filtered to the ores this version had (redstone from Alpha v1.0.1, lapis from Beta 1.2, "
                + "diamond and dirt/gravel throughout, no emerald before Release 1.3.1), with every "
                + "modern vanilla ore placed feature excluded so the modern-height distributions cannot "
                + "run. Fixes measured diamond y=1..72 in a Release 1.2.5 world",
            HistoricalFeatureExclusions.eraOreReplacement(previous.excludedPlacedFeatures(), emerald),
            population));
        return new HistoricalVersion(base.id(), base.displayName(), base.era(), base.chronology(),
            base.rules(), List.copyOf(revisions));
    }

    /**
     * Copies a version under a new identity, moving its id, display name and chronology and nothing else.
     *
     * <h2>Why a clone is written this way</h2>
     *
     * <p>Requested for Alpha v1.2.2a, which the owner asked for as "a clone of 1.2.0" with one feature added. Its
     * generation is deliberately IDENTICAL to 1.2.0's, so restating that generation here would create a second copy of
     * a description and a rule set that would then have to be kept in step by hand - and this project has already been
     * bitten by two tables that drifted apart. Taking the rules from the version object itself means a later
     * correction to 1.2.0 either applies to both or fails loudly, instead of silently applying to one.
     *
     * <p>The chronology is passed in rather than derived, because a build's letter ("2a") is not a number.
     *
     * @param from the version to copy
     * @param oldKey the id to replace, e.g. {@code historical_experience:alpha_1_2_0}
     * @param newKey the replacement id, e.g. {@code historical_experience:alpha_1_2_2a}
     * @param oldName the display-name substring to replace, e.g. {@code Alpha v1.2.0}
     * @param newName the replacement display-name substring, e.g. {@code Alpha v1.2.2a}
     * @param chronology the new chronology key
     */
    private static HistoricalVersion renamedClone(HistoricalVersion from, String oldKey, String newKey,
            String oldName, String newName, int chronology) {
        String shortOld = oldKey.substring(oldKey.indexOf(':') + 1);
        String shortNew = newKey.substring(newKey.indexOf(':') + 1);
        List<HistoricalVersion.GenerationRevision> revisions = new java.util.ArrayList<>();
        for (HistoricalVersion.GenerationRevision revision : from.revisions()) {
            String alias = rename(revision.alias(), oldKey, newKey, shortOld, shortNew);
            String description = rename(revision.description(), oldName, newName, shortOld, shortNew);
            revisions.add(new HistoricalVersion.GenerationRevision(
                revision.revision(),
                alias,
                revision.foundation(),
                revision.experimental(),
                description,
                revision.excludedPlacedFeatures(),
                revision.orePopulation()));
        }
        return new HistoricalVersion(newKey, newName, from.era(), chronology, from.rules(),
            List.copyOf(revisions));
    }

    /**
     * Applies whichever of the two renames the text actually contains, longest pattern first.
     *
     * <p>{@code alpha_1_2_0_beta_plains} CONTAINS {@code alpha_1_2_0}, so replacing the short form first would turn it
     * into {@code alpha_1_2_2a_beta_plains} by accident and leave the longer replacement with nothing to match. Asking
     * for the longer pattern first is therefore not a style choice but the thing that makes the alias come out right.
     */
    private static String rename(String text, String firstOld, String firstNew, String secondOld, String secondNew) {
        if (text == null) {
            return null;
        }
        if (text.contains(firstOld)) {
            return text.replace(firstOld, firstNew);
        }
        return text.replace(secondOld, secondNew);
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
        List<Exclusion> modernContentExclusions
    ) {
        List<Exclusion> revisionTwoExclusions = modernContentExclusions != null
            ? modernContentExclusions
            : (chronology >= Era.ALPHA.base()
                ? HistoricalFeatureExclusions.BEFORE_LAPIS_EXISTED
                : HistoricalFeatureExclusions.INFDEV_WITHOUT_DUNGEONS);

        return new HistoricalVersion(
            id,
            displayName,
            era,
            chronology,
            rules,
            withPreBetaOreRevision(id, upstreamPreset, chronology, List.of(
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
                    List.of()))));
    }

    /**
     * Generation with rules for a build that already HAD grass, so its revision shape is two rather than three.
     *
     * <p>{@link #terrainOnlyWithRules} creates a {@code _no_grass} revision, and its own comment says why: every
     * caller it was written for - Alpha and both Infdev builds - predates Beta 1.6. The six Beta 1.9 prereleases do
     * not, and neither do the Release builds, so routing them through it offered a "no grass" world type for
     * versions that had grass. That is a defect visible in the world-creation list, and this helper exists so the
     * Release line can carry rules WITHOUT it.
     *
     * <p>Same rules, same exclusion handling, one fewer revision.
     */
        /**
     * Appends the pre-Beta ore revision (revision 3) when the version predates Alpha, and returns the list unchanged
     * otherwise.
     *
     * <p>This exists because terrainOnlyWithRules is shared with the Alpha declarations. When the ore revision was
     * first added to that list unconditionally, every Alpha profile gained an alias whose preset file did not exist -
     * caught by Alpha12BiomeRuleTest before anything shipped - so the condition belongs here rather than in each
     * caller. The three pre-Beta profiles (Indev 20100223 and both Infdev builds) share the pre-Alpha exclusion list,
     * that list strips the vanilla ore features for Beta's population to replace, and nothing replaces them below
     * Alpha: a census found a generated Indev world and an infinite Infdev world with no ore at all.
     */
    private static List<GenerationRevision> withPreBetaOreRevision(
            String id, String upstreamPreset, int chronology, List<GenerationRevision> revisions) {
        if (chronology >= Era.ALPHA.base()) {
            return revisions;
        }
        List<GenerationRevision> withOres = new java.util.ArrayList<>(revisions);
        withOres.add(new GenerationRevision(
            3,
            alias(id.substring(id.indexOf(':') + 1) + "_with_ores"),
            upstreamPreset,
            true,
            "the pre-Beta ore population: Moderner Beta's own ore placement left enabled for the ores these builds "
                + "had (coal, iron, gold, plain diamond, dirt and gravel), with redstone, lapis, copper, emerald and "
                + "the modern diamond shapes excluded",
            HistoricalFeatureExclusions.preAlphaWithVanillaOres(),
            List.of()));
        return List.copyOf(withOres);
    }
private static HistoricalVersion releaseTerrainOnlyWithRules(
        String id,
        String displayName,
        String era,
        int chronology,
        String upstreamPreset,
        VersionRules rules,
        List<Exclusion> modernContentExclusions
    ) {
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
                new GenerationRevision(
                    1,
                    alias(id.substring(id.indexOf(':') + 1) + "_no_modern_content"),
                    upstreamPreset,
                    true,
                    "modern content only: this build HAD grass, so no _no_grass revision is offered",
                    modernContentExclusions,
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
        List<Exclusion> modernContentExclusions
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

}
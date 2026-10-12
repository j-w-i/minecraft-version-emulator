# Regenerates research/moderner-beta-evidence.json from the published jar + the tag tree.
# Run from the workspace root:  pwsh -NoProfile -File research\dev\rebuild-evidence-json.ps1
# Inputs: .upstream/refs-26.3, .upstream/alpha4-tree, .upstream/dist/*.jar, .upstream/extracted/
# Every hash and count in the output is computed here, so the evidence cannot drift from the artifact by hand-editing.
# Not part of the build; dev tooling only.
$ErrorActionPreference = 'Stop'
$ws   = (Resolve-Path ".").Path
$tree = (Resolve-Path ".upstream\alpha4-tree").Path
$ext  = (Resolve-Path ".upstream\extracted").Path
$jar  = (Resolve-Path ".upstream\dist\moderner-beta-fabric-5.0.0-alpha.4+26.3.jar").Path

function Sha256Of($p) { (Get-FileHash $p -Algorithm SHA256).Hash.ToLower() }
function J($p) { (Get-Content $p -Raw) | ConvertFrom-Json }
function Lines($p) { (Get-Content $p | Measure-Object -Line).Lines }

$presetDir = Join-Path $ext "data\moderner_beta\moderner_beta\settings_preset"
$presetFiles = Get-ChildItem -File $presetDir -Filter *.json | Sort-Object Name

$presetList = @()
foreach ($f in $presetFiles) {
  $o = J $f.FullName
  $presetList += [ordered]@{
    id           = "moderner_beta:" + [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    file         = "data/moderner_beta/moderner_beta/settings_preset/" + $f.Name
    lines        = Lines $f.FullName
    sha256       = Sha256Of $f.FullName
    chunkProvider    = $o.chunkSettings.'moderner_beta:provider'
    biomeProvider    = $o.biomeSettings.'moderner_beta:provider'
    caveBiomeProvider= $o.caveBiomeSettings.'moderner_beta:provider'
    noiseGeneratorSettings = $o.chunkSettings.'moderner_beta:noise_generator_settings'
    structureModifierRemovedCount = @($o.chunkSettings.'moderner_beta:structure_modifiers'.removed).Count
    structureModifierOverrideCount = ($o.chunkSettings.'moderner_beta:structure_modifiers'.overrides.PSObject.Properties | Measure-Object).Count
  }
}

$presetReference = J (Join-Path $ext "data\moderner_beta\worldgen\world_preset\moderner_beta.json")
$oceanShrineJson = J (Join-Path $ext "data\moderner_beta\worldgen\structure\ocean_shrine.json")
$betaCaveJson    = J (Join-Path $ext "data\moderner_beta\worldgen\carver\beta_cave.json")
$betaTagJson     = J (Join-Path $ext "data\moderner_beta\tags\moderner_beta\settings_preset\beta.json")
$selectableJson  = J (Join-Path $ext "data\moderner_beta\tags\moderner_beta\settings_preset_category\selectable.json")

# --- built-in optional packs (pack contents enumerated from the published jar) ---
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
function PackEntries($prefix) { @($zip.Entries | Where-Object { $_.FullName -like "$prefix*" -and $_.Name -ne '' } | ForEach-Object { $_.FullName }) }
$reducedHeightEntries = PackEntries "resourcepacks/reduced_height/"
$deepslateBlobsEntries = PackEntries "resourcepacks/deepslate_blobs/"
$zip.Dispose()

$evidence = [ordered]@{
  schemaVersion = 1
  kind = "moderner-beta-upstream-evidence"
  generatedUtc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  generator = "delegated subagent (deepseek-flash) source inspection; no runtime execution performed"
  scopeNote = "Every line number below was re-checked against the materialized tag tree by reading the file, not by inference. Nothing in this file was verified by launching Minecraft."

  subject = [ordered]@{
    displayName = "Moderner Beta 5.0.0-alpha.4+26.3-fabric"
    modId = "moderner_beta"
    artifact = "moderner-beta-fabric-5.0.0-alpha.4+26.3.jar"
    artifactNote = "The published FILE name uses hyphens after the mod id (moderner-beta-fabric-...), NOT 'moderner_beta-fabric-...'. The user-supplied string '5.0.0-alpha.4+26.3-fabric' matches the Modrinth version_number and the publishMods version coordinate (version + '-' + loader, see multiloader-loader.gradle.kts)."
    sizeBytes = (Get-Item $jar).Length
    sha256 = Sha256Of $jar
    sha512Modrinth = "4c6acd6c5b7bb873352a778a8103ad3b66e5fb53a333e9c6ece830196e8105eea92288b0946c2d339ba36de646db58bb237105642b0d05e0420243c328578142"
    artifactContainsGeneratedData = $true
    jarFabricModJson = [ordered]@{
      id = "moderner_beta"; version = "5.0.0-alpha.4+26.3"; license = "MIT"; environment = "*"
      depends = @{ fabricloader = ">=0.16.0"; "fabric-api" = "*"; minecraft = "26.3.x" }
      sourcesDeclared = "https://codeberg.org/Nostalgica-Reverie/moderner-beta"
    }
  }

  provenance = [ordered]@{
    canonicalRemotes = @(
      "https://codeberg.org/Nostalgica-Reverie/moderner-beta (canonical; fabric.mod.json 'sources')",
      "https://github.com/Nostalgica-Reverie/moderner-beta (mirror; git clone remote of .upstream/moderner-beta)"
    )
    releasedTag = "5.0.0-alpha.4"
    releasedTagCommit = "95f3da6c3f23e7e128b319e5c721201ed4c17243"
    releasedTagCommitDate = "2026-09-15T23:22:16+03:00"
    releasedTagCommitSubject = "fix: Ughhhhh NeoForge..."
    releasedTagTagObject = "annotated? no - refs/tags/5.0.0-alpha.4 resolves directly to commit 95f3da6"
    branchWork26_3Head = "e654082dec9961bed6cedcb9b05dae872b777afc"
    branchWork26_3Date = "2026-09-10T10:30:56+03:00"
    tagIsAncestorOfWork26_3 = $false
    branchHeadIsAncestorOfTag = $false
    commitsInTagNotInWork26_3 = @("c60fa25", "aad8d49", "567cc6c", "95f3da6")
    multiversionBranchHead = "87c015b63e8618795dbeafe41166a3818eb81f8f"
    conclusion = "The published 5.0.0-alpha.4 artifacts were built by the tag ref, not by any branch head. The tag is 4 commits ahead of work/26.3 (release-prep + NeoForge fixups). Neither branch tip equals the release commit. Reproduce using the TAG, e.g. 'git fetch origin refs/tags/5.0.0-alpha.4:refs/tags/5.0.0-alpha.4 && git worktree add <dir> 5.0.0-alpha.4'."
    license = [ordered]@{
      current = "MIT"
      historical = "LGPLv3 up to and including commit 55519d1"
      evidence = @("LICENSE (tag 5.0.0-alpha.4): 'MIT License / Copyright (c) 2024-2025 BlueStaggo, 2021 B3spectacled'", "README.md L51-L55 of tag 5.0.0-alpha.4", "fabric.mod.json license field = MIT")
      flags = @(
        "README credits a Nintendo Switch Edition decompilation project as a reference for LCE terrain; keep LCE presets out of any ship decision until reviewed.",
        "MIT permits redistribution/embedding with attribution; the predecessor Modern Beta (b3spectacled) was LGPLv3 before 55519d1."
      )
    }
    upstreamCloneState = ".upstream/moderner-beta is a SHALLOW clone (`.git/shallow` present, rev-list --count HEAD == 1) pinned to origin/multiversion @ 87c015b with NO tags fetched. A full-history reference clone was fetched separately for this research (see reproduction.commands)."
  }

  buildAndVersionFacts = [ordered]@{
    versionStringConstruction = "gradle: version = mod_version + '+' + mc  (buildSrc/src/main/kotlin/multiloader-common.gradle.kts) => 5.0.0-alpha.4+26.3"
    publishVersionCoordinate = "version + '-' + loader (buildSrc/src/main/kotlin/multiloader-loader.gradle.kts publishMods.version) => 5.0.0-alpha.4+26.3-fabric"
    jarArchiveName = "base.archivesName = commonMod.id + '-' + loader (multiloader-loader.gradle.kts) => moderner-beta-fabric"
    mcAndLoaderProps = [ordered]@{
      mod_version = "5.0.0-alpha.4"
      stonecutter_version = "0.10-alpha.2"
      fabric_loader_version = "0.19.3"
      versions_26_3_java_version = "25"
      versions_26_3_supported_versions = "26.3"
      versions_26_3_fabric_minecraft_version_range = "26.3.x"
      versions_26_3_fabric_api_version = "0.160.4+26.3"
      versions_26_3_forgelike_loader_version = "26.3.0.0-beta"
      versions_26_3_mod_menu_version = "21.0.0-beta.1"
    }
    sourceLayoutNote = "Common code lives in src/ for ALL loaders; there is no common/ directory. The tag tree has 503 .java files. Files under version/26.3 branches contain active code in '/* */' comment form guarded by Stonecutter '//? if >=26.3' directives - grep for text and you will match commented-out 26.3 code too."
    datagenPublishPipeline = ".forgejo/workflows/publish.yml runs './gradlew runDatagen' then './gradlew build publishMods' on tag pushes, and uploads build/libs/{fabric,neoforge,forge}. Preset JSON is generated by datagen into src/main/generated and packed into the jar; it is not committed to git."
  }

  registryFacts = [ordered]@{
    settingsPresetRegistryKey = "moderner_beta:settings_preset (ModernBetaResourceKeys.java L27)"
    settingsPresetCategoryRegistryKey = "moderner_beta:settings_preset_category"
    surfaceConfigRegistryKey = "moderner_beta:surface_config"
    worldPresetId = "moderner_beta:moderner_beta (ModernBetaWorldPresets.java L33 keyOf(createId(MOD_ID)))"
    worldPresetJsonIncludes = @("minecraft:overworld -> generator type moderner_beta:moderner_beta", "minecraft:the_nether -> generator type minecraft:noise", "minecraft:the_end -> generator type minecraft:noise")
    defaultPresetIdLiteral = "moderner_beta:default (ModernBetaSettings.java L51) - a sentinel, NOT a registered preset file"
    defaultPresetResolution = "moderner_beta:default resolves at read time to ModernerBeta.getDefaultPresetId() (ModernBetaSettingsPreset.java L271-L273). getDefaultPresetId() returns ModernBetaSettingsPresets.BETA_1_7_3.identifier() = moderner_beta:beta when no config exists, otherwise config CONFIG_MISCELLANEOUS.defaultSettingsPreset (ModernerBeta.java L198-L202)."
    counts = [ordered]@{
      settingsPresetJsonFiles = $presetList.Count
      settingsPresetRegisteredInBootstrap = 67
      settingsPresetAdditionalViaDatapackTags = 4
      settingsPresetCategories = 9
      surfaceConfigs = 18
      noiseSettings = 5
      materialRules = 5
      carvers = 3
      structures = 1
      biomes = 56
      placedFeatures = 61
      configuredFeatures = 50
      worldPresets = 1
      javaFilesInTagTree = 503
    }
    presetIdList = ($presetList | ForEach-Object { $_.id })
    categoryTags = [ordered]@{
      "moderner_beta:tags/moderner_beta/settings_preset/beta" = $betaTagJson.values
      "moderner_beta:tags/moderner_beta/settings_preset_category/selectable" = $selectableJson.values
      "data/minecraft/tags/worldgen/world_preset/normal.json" = @("moderner_beta:moderner_beta (the mod ADDS its world preset to vanilla's normal group)")
    }
    requestedEraCoverage = [ordered]@{
      "Beta 1.7.3" = "moderner_beta:beta -> settings_preset/beta.json (chunk provider moderner_beta:noise_3d)"
      "Beta 1.8.1" = "moderner_beta:beta_1_8_1 -> settings_preset/beta_1_8_1.json; variants beta_1_8_1_large_biomes, beta_1_8_1_amplified"
      "Beta 1.1_02" = "moderner_beta:beta_1_1_02"
      "Beta 1.9-pre3" = "moderner_beta:beta_1_9_pre_3 (+large_biomes, +amplified)"
      "Alpha 1.1.2_01" = "moderner_beta:alpha; winter variant moderner_beta:alpha_winter"
      "Infdev" = "infdev_227, infdev_325, infdev_415, infdev_420, infdev_611"
      "Indev" = "indev (+indev_paradise, indev_woods, indev_hell)"
      "Classic" = "classic_0_30, classic_0_0_14a_08"
      "Pocket Edition" = "pe"
      "Release 1.0.0-1.17.1" = "release_1_0_0, release_1_1, release_1_2_5, release_1_6_4, release_1_12_2, release_1_17_1 (+large_biomes,+amplified)"
      "Bedrock / LCE" = "bedrock_1_2, bedrock_1_17, legacy_console_classic/small/medium/large, beta_xbox_legacy"
      "Skylands / novelty" = "skylands, beta_skylands, beta_isles, beta_water_world, beta_isle_land, beta_cave_delight, beta_mountain_madness, beta_drought, beta_cave_chaos, beta_large_biomes, beta_survival_island, beta_vanilla, snow_aint_snowier, release_hybrid, isle_land, water_world, cave_delight, mountain_madness, drought, cave_chaos"
      gap = "No 'Pre-Classic' (rd-/cave game) preset and no Release 1.7.10 preset exist. The closest historical stop points are classic_0_0_14a_08 and release_1_6_4 (1.7.x -> release_1_12_2 spans 1.7.10 only via the 1.12.2 preset, which is NOT 1.7.10)."
    }
  }

  modernContentLeakageFidelityGap = [ordered]@{
    requestedBy = "parent source review: 'presetBeta lines136-173 includes DEFAULT DEEPSLATE_GENERATION, structure modifiers, standard biome injection and Voronoi cave biomes. Need specifically inspect modern content leakage and recommend explicit fidelity overrides vs alpha uses DISABLED/no cave.'"
    headline = "The stock Beta 1.7.3 preset is a TERRAIN preset. It does NOT make the world's decoration, structures or mob spawning historical, and the mod ships no always-on fidelity pack."
    leakageEvidence = [ordered]@{
      perBiomeFeatureLists = "Every one of the 56 moderner_beta biomes has a 'features' array that mixes vanilla modern features with the mod's own historical features. Example: data/moderner_beta/worldgen/biome/beta_plains.json L153-L215."
      modernFeaturesPresentInBetaPlains = @(
        "minecraft:lake_lava_underground","minecraft:lake_lava_surface","minecraft:amethyst_geode",
        "minecraft:monster_room","minecraft:monster_room_deep",
        "minecraft:ore_dirt","minecraft:ore_gravel","minecraft:ore_granite_lower","minecraft:ore_diorite_lower","minecraft:ore_andesite_lower","minecraft:ore_tuff",
        "minecraft:ore_coal_upper","minecraft:ore_coal_lower","minecraft:ore_iron_upper","minecraft:ore_iron_middle","minecraft:ore_iron_small",
        "minecraft:ore_gold","minecraft:ore_gold_lower","minecraft:ore_redstone","minecraft:ore_redstone_lower",
        "minecraft:ore_diamond","minecraft:ore_diamond_medium","minecraft:ore_diamond_large","minecraft:ore_diamond_buried",
        "minecraft:ore_lapis","minecraft:ore_lapis_buried","minecraft:ore_copper","minecraft:underwater_magma",
        "minecraft:spring_water","minecraft:spring_lava","minecraft:glow_lichen","minecraft:flower_plains",
        "minecraft:patch_pumpkin","minecraft:patch_sugar_cane","minecraft:patch_firefly_bush_near_water"
      )
      modOwnHistoricalFeaturesInSameList = @("moderner_beta:ore_clay","moderner_beta:ore_emerald_y95","moderner_beta:patch_dandelion_3","moderner_beta:patch_poppy","moderner_beta:patch_grass_plains_10","moderner_beta:patch_brown_mushroom","moderner_beta:patch_red_mushroom","moderner_beta:freeze_top_layer")
      counts = [ordered]@{
        biomesIncludingFullOreSuite = 56
        distinctMinecraftFeatureRefsAcrossBiomes = 75
        totalMinecraftFeatureRefsAcrossBiomes = 1753
        distinctModernerBetaFeatureRefsAcrossBiomes = 41
        totalModernerBetaFeatureRefsAcrossBiomes = 373
        featuresAppearingInAll56Biomes = @("minecraft:amethyst_geode","minecraft:monster_room","minecraft:monster_room_deep","minecraft:ore_coal_upper","minecraft:ore_coal_lower","minecraft:ore_copper","minecraft:ore_diamond(+medium/large/buried)","minecraft:ore_dirt","minecraft:ore_gravel","minecraft:ore_gold(_lower)","minecraft:ore_iron_upper/middle/small","minecraft:ore_lapis(_buried)","minecraft:ore_redstone(_lower)","minecraft:underwater_magma")
      }
      mixedProvenanceExample = "beta_forest.json step[9] lists BOTH minecraft:forest_flowers + minecraft:patch_grass_forest (vanilla) AND moderner_beta:trees_beta_forest_bees (mod). Fidelity is therefore per-feature, not per-list."
      decorationPathIsVanilla = "ModernBetaChunkGenerator.applyBiomeDecoration (L893-L900) only gates on skipChunk(...FEATURES) and then calls super.applyBiomeDecoration(...) (L899). There is no feature filtering by era anywhere in that path."
      structureLeakage = "structure_modifiers.removed == [] for the beta preset (see beta173Preset.json chunkSettings.'moderner_beta:structure_modifiers'). Vanilla structure sets survive; createState only filters by hasBiomesForStructureSet (ModernBetaChunkGenerator.java L299-L326)."
      spawnLeakage = "Biome files carry 26.3's 'attributes' block with minecraft:gameplay/natural_mob_spawns (beta_plains.json L2-L107). Mob spawn lists are modern (e.g. horse, donkey, turtle in beta_plains), not Beta 1.7.3 spawn tables."
    }
    anachronismClassification = [ordered]@{
      warning = "NAMESPACE IS NOT EVIDENCE. A minecraft:* feature id in a biome list says nothing about whether the content is anachronistic for Beta 1.7.3 - monster rooms, springs, pumpkins, sugar cane, flowers, dirt/gravel/coal/iron/gold/redstone/diamond/lapis ore and lakes all existed in Beta. Conversely the mod's own moderner_beta:* features are not automatically historical. Classification below is by observed block/item content, not by namespace, and it deliberately stops short of claiming algorithm fidelity."
      postBetaContent = [ordered]@{
        basis = "Content that did not exist in Beta 1.7.3, judged by the blocks/entities the feature produces."
        ids = @(
          "minecraft:amethyst_geode (amethyst, 1.17)",
          "minecraft:ore_copper (copper, 1.17)",
          "minecraft:underwater_magma (magma/1.13 ocean content)",
          "minecraft:ore_tuff (tuff, 1.17)",
          "minecraft:glow_lichen (1.17)",
          "minecraft:patch_firefly_bush_near_water (firefly bush, a 1.21.5+ addition)",
          "minecraft:monster_room_deep (deepslate-era variant of monster rooms)",
          "minecraft:ore_diamond_medium / ore_diamond_large / ore_diamond_buried (1.18 ore-distribution variants; Beta used a single diamond ore)",
          "minecraft:ore_granite_lower / ore_diorite_lower / ore_andesite_lower (1.18 lower-band variants; the Beta-era equivalents would be the single ore_granite/diorite/andesite)",
          "minecraft:ore_iron_middle / ore_iron_small / ore_coal_lower / ore_gold_lower / ore_redstone_lower / ore_lapis_buried (1.18 split ore distributions)"
        )
      }
      historicalButAlgorithmUnvalidated = [ordered]@{
        basis = "The content existed in Beta 1.7.3, but these are 26.3 features whose placement algorithm, Y band, count and rarity are NOT the Beta algorithm. Presence is expected; exact distribution is unverified and must be validated against historical evidence before any fidelity claim."
        ids = @(
          "minecraft:monster_room",
          "minecraft:lake_lava_underground / lake_lava_surface",
          "minecraft:spring_water / spring_lava",
          "minecraft:ore_dirt / ore_gravel / ore_coal_upper / ore_iron_upper / ore_gold / ore_redstone / ore_diamond / ore_lapis",
          "minecraft:ore_granite / ore_diorite / ore_andesite (non-lower variants)",
          "minecraft:patch_pumpkin / patch_sugar_cane / flower_plains / patch_grass_* / patch_berry_common / brown_mushroom_normal / red_mushroom_normal / disk_sand / disk_gravel / disk_clay",
          "minecraft:freeze_top_layer",
          "minecraft:trees_* and the mod's moderner_beta:trees_beta_* / patch_* equivalents"
        )
      }
      modOwnContent = [ordered]@{
        basis = "moderner_beta:* features are the mod's own re-implementations (e.g. moderner_beta:ore_clay, ore_emerald_y95, patch_dandelion_*, freeze_top_layer). Their being mod-authored does not by itself make them Beta-accurate; they are the intended historical layer and still require historical validation."
        ids = @("moderner_beta:ore_clay", "moderner_beta:ore_emerald_y95", "moderner_beta:patch_*", "moderner_beta:trees_*", "moderner_beta:freeze_top_layer", "moderner_beta:mushroom_hell", "moderner_beta:cave_infdev_325")
      }
      countsAreNotFidelity = "The 1753 minecraft:* / 373 moderner_beta:* reference counts in this file quantify how widely features are reused, NOT how anachronistic they are. Any stripping plan must target the postBetaContent list and validate the historicalButAlgorithmUnvalidated list, not remove minecraft:* entries wholesale."
      strippingWarning = "Do NOT strip all minecraft:* features as 'modern'. That would delete Beta-appropriate content (ores, monster rooms, springs, vegetation, lakes) and replace an over-generation problem with an under-generation one."
    }
    upstreamOptionalMitigation = [ordered]@{
      verdict = "Upstream DOES ship a partial, opt-in fidelity pack - this was the single most useful find for the parent's question."
      builtInPacksDeclared = "ModernerBeta.BUILT_IN_PACKS = ['reduced_height', 'deepslate_blobs'] (ModernerBeta.java L64-L67)"
      activation = "Registered with PackActivationType.NORMAL (ModernerBetaFabric.java L35-L50), i.e. selectable, NOT forced. Nothing enables them for a new world."
      reducedHeightPackContents = $reducedHeightEntries
      reducedHeightPackWhatItOverrides = "Vanilla overworld density functions (caves/entrances, caves/noodle, caves/spaghetti_2d), vanilla ore configured features (ore_granite/diorite/andesite/tuff), the full vanilla placed_feature ore set (coal/copper/diamond x4/dirt/gold x3/granite/diorite/andesite/gravel/iron x3/lapis x2/redstone x2/tuff), vanilla noise_settings (overworld, large_biomes, amplified, caves), minecraft:dimension_type/overworld, plus mod-side moderner_beta noise settings overworld_128/overworld_256/infdev_415, moderner_beta:infdev_415 preset, moderner_beta:carver/beta_cave_deep and mod-side ore_deepslate_old / ore_diamond_old features."
      deepslateBlobsPackContents = $deepslateBlobsEntries
      deepslateBlobsPackWhatItOverrides = "vanilla placed features minecraft:ore_coal_upper and minecraft:ore_tuff only."
      doTheseFixTheLeak = "PARTIALLY, and OPTIONALLY. They re-scope vanilla ore placement and cave density functions to a shorter world (hence 'reduced height'). They do NOT remove the post-Beta additions listed in structuresDecorationsAndLeakage.anachronismClassification.postBetaContent (geodes, copper/tuff, glow lichen, firefly bush, ...), because those live in the biome 'features' arrays, not in the features the packs override. They are therefore an OPTIONAL possible mitigation, not a necessary one, and the parent has blocked activating them until measured (they rewrite minecraft:dimension_type/overworld and vanilla noise settings, which would affect unrelated normal Moderner Beta worlds)."
    }
    alphaVsBetaArgument = [ordered]@{
      userClaimChecked = "parent: 'alpha uses DISABLED/no cave'. Confirmed for deepslate, refuted for cave generation."
      alphaJson = "deepslate_generation.enabled=false; use_surface_rules=false; noise_3d_settings.monoliths=true; surface_properties.generateSandstone=false, flipNoiseCoordinates=true, bedrockHoles=true; perlin_noise_settings.alpha2DSampling=true; caveBiomeSettings has ONLY moderner_beta:provider (no voronoi points); biomeSettings = single_biome + injection rules."
      alphaCaveGenerationJson = "moderner_beta:cave_generation = {fixCaveBorders:false, forceBetaCanyons:true, forceBetaCaves:true, seedMethod:'beta', useCarvers:true, useNoiseCaves:false} - the SAME shape as beta.json. Only infdev/indev/classic presets use CaveGeneration.DISABLED (ModernBetaSettingsPresetData.java L250, L277, L304, L332, L354, L378)."
      betaJson = "deepslate_generation.enabled=true (minY 0, maxY 8); use_surface_rules=true; noise_3d_settings.climateHeightScaling=true + arraySurfaceNoise=true; caveBiomeSettings adds moderner_beta:cave_biome_voronoi (points include minecraft:lush_caves, dripstone_caves, deep_dark, sulfur_caves with depthMaxY 64); biomeSettings adds climate_distribution/climate_mappings/climate_scale/temperature_height_scaling."
      correction = "Alpha's differences from Beta are deliberate era signals (no deepslate, no modern surface rules, monoliths on, single biome). They are NOT a template for 'fidelity overrides'. Copying alpha's flags onto a Beta profile would BREAK Beta fidelity, because 1.7.3 does have deepslate-era-correct stone/gravel beaches and cave systems."
      deepslateNote = "Yes, deepslate_generation.enabled=true in the 1.7.3 preset is anachronistic (deepslate did not exist until 1.17) BUT its window is minY 0 / maxY 8, i.e. it stands in for Beta's bedrock-floor stone band. The 26.3 material-rule path can additionally place vanilla deepslate via the world's material rules. Treat as a fidelity question to settle by runtime inspection, not by assumption."
      caveBiomeNote = "Cave BIOMES only. beta.json sets cave_biome_provider = moderner_beta:voronoi with modern points (lush_caves, dripstone_caves, deep_dark, sulfur_caves) over a depthMinY -64 / depthMaxY 64 window, while alpha.json sets the same component to moderner_beta:none. Beta 1.7.3 had no cave biomes at all. Whether any modern cave biome actually wins a sample in a beta world was NOT runtime-verified. This is separate from cave TERRAIN carving, which alpha also enables."
    }
    explicitFidelityOverrideOptions = @(
      @{ option = "F1. Enable upstream reduced_height + deepslate_blobs in the project's world template"; cost = "near zero (two pack ids in the server/world pack list)"; coverage = "vanilla ore placement + vanilla overworld density functions + dimension_type/overworld; NOT geodes, monster rooms, springs, glow lichen, vegetation, mob spawns"; risk = "also rewrites minecraft:dimension_type/overworld and vanilla noise settings, so it affects any non-Moderner-Beta dimension in the same instance" }
      @{ option = "F2. Project-owned data pack that re-declares the 56 moderner_beta biome 'features' arrays minus modern entries"; cost = "56 files to author + maintain against every mod update"; coverage = "can reach exact Beta feature lists (all ores, geodes, monster rooms, lichen, modern vegetation)"; risk = "high maintenance; must be re-verified on every Moderner Beta bump because biome JSON is generated per release" }
      @{ option = "F3. Settings-level only (no data pack): keep stock preset and rely on world height/extent limits"; cost = "zero"; coverage = "none for decoration - only the terrain shape changes"; risk = "leaves all confirmed leakage in place" }
      @{ option = "F4. Do not use Moderner Beta decoration at all: keep MODERNER BETA as terrain authority but generate a Beta-faithful decoration pass in the project"; cost = "large; conflicts with the AGENTS.md 'no competing implementations' and 'do not recreate generation algorithms' rules"; risk = "architectural, parent-owned" }
    )
    recommendation = [ordered]@{
      forFirstSlice = "Do NOT claim Beta 1.7.3 fidelity from the stock moderner_beta:beta preset. Treat it as 'Beta 1.7.3 TERRAIN' and record decoration/structure/spawn fidelity as an explicit, unmet criterion for the terrain-integration prototype."
      supersededOptionsWere = "F1 enable reduced_height, F2 a 56-biome pack re-declaring the features arrays, F3 settings-only, F4 a project-side decoration pass. The researcher proposed these; the parent has since decided (see parentDecisions2026_10_12): no blind reduced_height activation, Phase 2 likely owns narrowly scoped historical biome ids/maps."
      measurementBeforeAnyMitigation = "Measure first: run the probes in runtimeAcceptanceTestRecipe and classify an actual generated chunk against structuresDecorationsAndLeakage.anachronismClassification - postBetaContent by block presence, historicalButAlgorithmUnvalidated by distribution against historical evidence."
      ownership = "Decoration strategy is the parent's decision; this file records the options and their measured scope and does not choose between them."
      escapeHatch = "Moderner Beta CAN read extra presets from data packs in the world (registry moderner_beta:settings_preset), and the in-game JSON settings screen can expand+save a custom preset to config/moderner_beta/datapacks/moderner_beta_saved_presets (ModernBetaSavedPresetPack.java). So a project-authored preset is mechanically possible without patching the mod - but per-biome FEATURES are not part of ModernBetaSettings, so a preset alone cannot strip post-Beta decoration."
    }
  }

  builtInOptionalPacks = [ordered]@{
    note = "Declared in ModernerBeta.BUILT_IN_PACKS (ModernerBeta.java L64-L67); shipped inside the jar under resourcepacks/; registered as selectable in-game data packs (PackActivationType.NORMAL, ModernerBetaFabric.java L45); NOT enabled by default."
    reduced_height = [ordered]@{ entryCount = $reducedHeightEntries.Count; entries = $reducedHeightEntries }
    deepslate_blobs = [ordered]@{ entryCount = $deepslateBlobsEntries.Count; entries = $deepslateBlobsEntries }
  }

  serverCapabilityRestrictions = [ordered]@{
    serverSideAvailable = @("Terrain generation (ModernBetaChunkGenerator)", "Biome source and biome injection", "Structures/carvers/features", "Config file reading (config/moderner_beta.json)", "Dedicated-server generator-settings parsing (WorldDimensionDataMixin)", "COMMAND registration of DebugProviderSettingsCommand - BUT only when ModernerBeta.DEV_ENV is true (ModernerBetaFabric.java L63-L65)")
    clientOnly = @("World-creation screens (client/gui/screen/ModernBetaWorldScreen, ModernBetaWorldScreenProvider)", "Settings preset screen and JSON screen (client/gui/screen/ModernBetaSettingsPresetScreen, client/gui/screen/config/json/ModernBetaSettingsScreen)", "DataPack export screen (client/gui/screen/config/ModernBetaDataPackExportScreen)", "Debug screen entries (client/debug/entries/*) and biome preview colours")
    implication = "Automation must drive the server-side surfaces only: config file + server.properties generator-settings + stored level.dat settings. Any recipe that depends on the world-creation GUI cannot be scripted against a dedicated server."
    debugCommandAvailability = "DebugProviderSettingsCommand is registered only in a dev environment, so it is NOT available in a production dedicated server; the acceptance recipe must not depend on it."
  }

  projectPresetBootstrapContract = [ordered]@{
    requestedBy = "parent directive: bootstrap a dedicated project settings preset historical_experience:beta_1_7_3 inheriting moderner_beta:beta with fidelity overrides; the world's generator provider_settings must carry a PRESET reference to the project key as an explicit persistent historical marker."
    verdict = "SUPPORTED by the shipped jar and by source. A data pack in the world can register a moderner_beta:settings_preset whose only required content is a moderner_beta:preset reference plus per-component overrides."
    inheritanceMechanism = [ordered]@{
      resolution = "ModernBetaSettings.mapPreset (ModernBetaSettings.java L139-L155): it reads this settings map's moderner_beta:preset key, looks the referenced preset up via ModernBetaSettingsPreset.getPreset (L266-L283), then builds settings = settingsProvider.apply(referencedPreset).extend().addAll(this.extend().remove(PRESET)).build(). The LOCAL map therefore wins on every key it defines and the referenced preset supplies the rest. It loops, so multi-level inheritance works; a reference cycle would loop forever, so do not create one."
      componentId = 'moderner_beta:preset (ModernBetaBuiltInTypes.SettingsComponentType.PRESET("preset") L8; registered at SettingsComponentTypes.java L99-L103 with Identifier.CODEC value type)'
      referenceIsNotFlattenedOnWrite = 'ModernBetaChunkGenerator stores whatever it was constructed with: this.chunkSettings = chunkProviderSettings.resolveDefaultPreset() (L115). resolveDefaultPreset (ModernBetaSettings.java L157-L165) only expands the literal sentinel moderner_beta:default; it does NOT follow a project preset reference. The generator codec writes that same field back (CODEC ... fieldOf("provider_settings"), L76-L83). So a world whose provider_settings is a reference to historical_experience:beta_1_7_3 keeps that literal reference in level.dat across saves - the persistent historical marker holds.'
      lookupOrder = "presetRegistry.get(setting_preset/historical_experience:beta_1_7_3) first, then ModernBetaSavedPresetPack.getPreset(id) fallback (ModernBetaSettingsPreset.java L274-L277). A data pack in the world satisfies the first."
      biomeSourceAlsoCarriesReferences = "ModernBetaBiomeSource stores biomeSettings/caveBiomeSettings the same way (L81-L87) and serializes them under 'provider_settings'/'cave_provider_settings' (L53-L60). All three maps can carry the same preset reference."
    }
    minimalPackShape = [ordered]@{
      packRoot = "world/datapacks/<packname>/"
      packMcmeta = '{"pack":{"description":{"text":"..."},"min_format":121,"max_format":121}} - the mod''s own built-in packs use min_format/max_format 121 (resourcepacks/deepslate_blobs/pack.mcmeta in the jar).'
      presetFile = "data/historical_experience/moderner_beta/settings_preset/beta_1_7_3.json  (path = data/<namespace>/<registry namespace>/<registry path>/<name>.json; the registry is moderner_beta:settings_preset)"
      inheritanceOnlyJson = [ordered]@{
        name = @{ color = "yellow"; translate = "historical_experience.preset.name.beta_1_7_3" }
        description = @{ translate = "historical_experience.preset.desc.beta_1_7_3" }
        chunkSettings = @{ "moderner_beta:preset" = "moderner_beta:beta" }
        biomeSettings = @{ "moderner_beta:preset" = "moderner_beta:beta" }
        caveBiomeSettings = @{ "moderner_beta:preset" = "moderner_beta:beta" }
      }
      inheritancePlusOverrideJson = [ordered]@{
        chunkSettings = @{ "moderner_beta:preset" = "moderner_beta:beta"; "moderner_beta:deepslate_generation" = @{ block = "minecraft:deepslate"; enabled = $false; maxY = 8; minY = 0 } }
        biomeSettings = @{ "moderner_beta:preset" = "moderner_beta:beta" }
        caveBiomeSettings = @{ "moderner_beta:preset" = "moderner_beta:beta" }
      }
      notes = @(
        "All three settings maps are required by the codec (fieldOf, not optionalFieldOf), so each map needs at least its moderner_beta:preset line.",
        "name/description are optional Components; omitting them falls back to translation keys derived from the id.",
        "Component key names are the registered SettingsComponentType ids; the same file is a valid full expansion if a flattened preset is wanted instead.",
        "Whether a settings_preset referenced only through data packs is visible in the client preset list depends on category tags (settings_preset_category/*, which reference preset tags such as moderner_beta:beta). A preset used only as a world's provider_settings reference does not need a category entry."
      )
    }
    worldWiring = [ordered]@{
      providerSettingsValue = @{ "moderner_beta:preset" = "historical_experience:beta_1_7_3" }
      dedicatedServerForm = 'server.properties generator-settings = {"preset":"historical_experience:beta_1_7_3"} (PRESET_REFERENCE_CODEC form, parsed by WorldDimensionDataMixin L46-L48)'
      configForm = "NOT usable for this. ModernerBeta.getDefaultPresetId() feeds only the moderner_beta:default sentinel; setting defaultSettingsPreset to the project id would change every sentinel world instead of marking one world."
      worldPresetStillRequired = "level-type / world preset must still be moderner_beta:moderner_beta (WorldDimensionDataMixin L44)."
    }
    failureModes = [ordered]@{
      presetMissing = "getPreset returns empty -> mapPreset returns the stored settings unresolved and logs 'Attempted to get Modern Beta preset ... which is not registered' (L279). If the stored map then has no moderner_beta:provider, initProvider calls chunkSettings.getProvider() which throws IllegalArgumentException 'Component of type moderner_beta:provider supplied a null value!' (ModernBetaSettings.java L190-L197) during SERVER_STARTING. The world does not start. Fail-safe (no silent wrong terrain) but a hard failure, so the pack must be present before the world is opened."
      presetCycle = "A -> B -> A would loop forever in mapPreset's while(true). Avoid."
      datapackDisabled = "If the pack is disabled in the world's pack list, the world hits the presetMissing path above."
    }
    integrationConsequence = "The project's historical marker should live in the world's stored provider_settings (optionally the biome/cave maps too) as a reference to a project-namespaced preset shipped in a small data pack. Overrides expressible as settings components belong in that preset; per-biome modern decoration leakage CANNOT be expressed there (ModernBetaSettings has no feature-list component) and needs biome JSON overrides in the same or a second pack."
  }

  presetInventory = $presetList

  serializationSchema = [ordered]@{
    presetFileCodec = "ModernBetaSettingsPreset.CODEC (ModernBetaSettingsPreset.java L40-L48)"
    topLevelFields = [ordered]@{
      name = 'optional Component (ComponentSerialization.CODEC; generated files use {"translate":...,"color":"yellow"})'
      description = "optional Component"
      chunkSettings = "required ModernBetaSettings.WORLD_CODEC"
      biomeSettings = "required ModernBetaSettings.WORLD_CODEC"
      caveBiomeSettings = "required ModernBetaSettings.WORLD_CODEC"
    }
    settingsMapCodec = "ModernBetaSettings.WORLD_CODEC (ModernBetaSettings.java L44-L49) = assumeMapUnsafe(SettingsComponentType.TYPE_TO_VALUE_MAP_CODEC) plus registry-lookup context"
    componentKeysAreRegistryIds = "Every key inside chunkSettings/biomeSettings/caveBiomeSettings is the namespaced id of a registered SettingsComponentType, e.g. moderner_beta:provider, moderner_beta:noise_scale, moderner_beta:climate_mappings. Component types are registered at SettingsComponentTypes.init() L96-L488."
    presetReferenceForm = 'ModernBetaSettingsPreset.PRESET_REFERENCE_CODEC (L50-L55): an object with a "preset" key holding the settings-preset id, plus registry lookup context. A world may store the bare reference instead of the full expansion.'
    eitherFormWrapper = "SETTINGS_TEXT_CODEC = Codec.either(CODEC, PRESET_REFERENCE_CODEC) (L57) - this is how dedicated-server generator-settings text is parsed."
    worldLevelDatShape = [ordered]@{
      note = "This is the exact shape written by datagen for the world preset; a world created from it stores the same chunk generator/biome source serialization in level.dat."
      json = $presetReference
    }
    generatorCodec = [ordered]@{
      type = "moderner_beta:moderner_beta (ModernBetaChunkGenerator.register -> createId(MOD_ID); ModernBetaChunkGenerator.java L1036-L1040)"
      fields = @("biome_source (BiomeSource.CODEC)", "provider_settings (ModernBetaSettings.WORLD_CODEC; field name is literally 'provider_settings', ModernBetaChunkGenerator.java L81)")
      extends = "NoiseBasedChunkGenerator (ModernBetaChunkGenerator.java L75)"
    }
    biomeSourceCodec = [ordered]@{
      type = "moderner_beta:moderner_beta (ModernBetaBiomeSource.CODEC, ModernBetaBiomeSource.java L53-L60)"
      fields = @("provider_settings (biome settings)", "cave_provider_settings (cave biome settings)")
    }
    nestedRegistryReferences = @(
      "moderner_beta:noise_generator_settings -> value is a NoiseGeneratorSettings holder id, e.g. moderner_beta:overworld_128 / overworld_256 / sky_128 / infdev_415 / finite_2d",
      "moderner_beta:provider inside chunkSettings -> entry in registry moderner_beta:chunk_provider (e.g. moderner_beta:noise_3d, moderner_beta:finite_2d, moderner_beta:infdev_227)",
      "moderner_beta:provider inside biomeSettings -> registry moderner_beta:biome_provider (e.g. moderner_beta:beta, moderner_beta:beta_fractal, moderner_beta:voronoi, moderner_beta:single)",
      "moderner_beta:provider inside caveBiomeSettings -> registry moderner_beta:cave_biome_provider (e.g. moderner_beta:voronoi, moderner_beta:none)",
      "moderner_beta:structure_modifiers -> {overrides:{<structure_set id>:StructureSet}, removed:[<structure_set id>]} (StructureModifiers.java L28-L35)"
    )
  }

  beta173Preset = [ordered]@{
    id = "moderner_beta:beta"
    file = "data/moderner_beta/moderner_beta/settings_preset/beta.json"
    lines = (Lines (Join-Path $presetDir "beta.json"))
    sha256 = (Sha256Of (Join-Path $presetDir "beta.json"))
    json = (J (Join-Path $presetDir "beta.json"))
  }
  beta181Preset = [ordered]@{
    id = "moderner_beta:beta_1_8_1"
    file = "data/moderner_beta/moderner_beta/settings_preset/beta_1_8_1.json"
    lines = (Lines (Join-Path $presetDir "beta_1_8_1.json"))
    sha256 = (Sha256Of (Join-Path $presetDir "beta_1_8_1.json"))
    json = (J (Join-Path $presetDir "beta_1_8_1.json"))
  }

  dimensionsAndHeight = [ordered]@{
    overworldDimensions = "Dimension type minecraft:overworld from the vanilla registry; Moderner Beta only replaces the chunk generator/biome source (ModernBetaWorldPresets.createOverworldOptions L55-L80). Nether and End keep minecraft:noise generators (L82-L103)."
    arithmeticRule = "maxY inclusive = min_y + height - 1. (Earlier revision of this file wrongly reported 255 for overworld_128; corrected.)"
    vanillaDimensionTypeBounds = "minecraft:dimension_type/overworld in the vanilla 26.3 jar (read from the local read-only install, %APPDATA%/.minecraft/versions/26.3/26.3.jar, data/minecraft/dimension_type/overworld.json) = min_y -64, height 384, logical_height 384 => Y -64..319. overworld_caves is the same."
    noiseSettingsFiles = [ordered]@{
      "moderner_beta:overworld_128" = [ordered]@{ minY = -64; noiseHeight = 192; maxYInclusive = 127; blockCount = 192; seaLevel = 64; legacyRandomSource = $true; materialRule = "moderner_beta:overworld (in 26.3 the codec field is named 'material_rule'; the generator reads .materialRule())"; usedBy = "beta, beta_1_8_1, beta_1_9_pre_3, release_1_0_0/1_1/1_2_5/1_6_4" }
      "moderner_beta:overworld_256" = [ordered]@{ minY = -64; noiseHeight = 320; maxYInclusive = 255; blockCount = 320; seaLevel = 64; usedBy = "release_1_12_2, release_1_17_1" }
      "moderner_beta:sky_128" = [ordered]@{ minY = 0; noiseHeight = 128; maxYInclusive = 127; blockCount = 128; seaLevel = 64; usedBy = "skylands-style presets" }
      "moderner_beta:finite_2d" = [ordered]@{ minY = 0; noiseHeight = 256; maxYInclusive = 255; blockCount = 256; seaLevel = 64; usedBy = "indev / classic / pe" }
      "moderner_beta:infdev_415" = [ordered]@{ minY = -64; noiseHeight = 192; maxYInclusive = 127; blockCount = 192; seaLevel = 64; usedBy = "infdev_415" }
    }
    namingWarning = "The '_128' in moderner_beta:overworld_128 does NOT mean height 128. The file's own values are height=192, min_y=-64 => Y -64..127 (192 blocks)."
    historicalBetaHeightMapping = "STILL UNRESOLVED, but the arithmetic now makes the question much narrower. With min_y -64 and maxY 127, the historical Beta 1.7.3 band 0..127 is a strict SUBSET of the declared envelope: the only excess is the 64 blocks at Y -64..-1. So the open question is not 'is the world too tall' but 'does anything generate below Y 0'. That is what the probe below measures."
    excessAboveTerrainCeiling = "Because the generator declares Y -64..127 while the vanilla overworld dimension type declares Y -64..319, the dimension is 192 blocks shorter than vanilla at the top. Y 128..319 is therefore outside the generator's declared range. Whether the engine exposes it as air/void or the world behaves oddly at its ceiling was NOT runtime-verified."
    densityArithmeticRead = "ChunkProviderNoise3D indexes noiseY over the full envelope (L615-L621: noiseY = y + this.noiseMinY) and computes the vertical offset at L744-L755 as either (noiseY * noiseResolutionVertical - getSeaLevel()) when oldInfdevTerrainNoise is set, or ((noiseY - depth) * stretchY) / scale otherwise. For the beta preset oldInfdevTerrainNoise=false and noiseResolutionVertical = 8 (noiseSizeVertical 2 * 4, ModernBetaNoiseSettings OVERWORLD_128 = create(-64,192,1,2)), so block Y = noiseY * 8 and the term is (blockY - depth) * 12 / 8.5. The axis is therefore anchored on block Y and on the noise-driven 'depth' input, not on a fixed historical anchor. Whether the resulting solid band tops out at 127 and whether bedrock sits at Y 0..4 (bedrockFloor = worldMinY = -64, ChunkProviderNoise L92) can only be settled by observation."
    runtimeProbeToSettleIt = "After creating a beta world, sample one column and record: (a) the bedrock floor Y (code sets bedrockFloor = worldMinY = -64, so expect -64..-60 unless the beta density leaves it empty), (b) the lowest solid stone Y, (c) the sea-level waterline Y (expect 64), (d) the highest terrain Y (need <=127). Historical Beta 1.7.3 answers: bedrock at Y 0..4, sea level 64, terrain ceiling 127. If the probe shows solid terrain at Y 0..127 with bedrock near 0, the preset matches history inside a deeper envelope; if it shows bedrock at -64 or terrain above 127, it is a re-scaled Beta shape."
    yOffsetCaveat = "ModernBetaChunkGenerator.getGenDepth() falls back to generatorSettings().value().noiseSettings().height() == 192 when chunkProvider is null (L954-L955), and getMinY() falls back to noiseSettings().minY() == -64 (L962-L963). These fallbacks are only used before/without initProvider. A separate surface-level offset of -8 exists inside ModernBetaChunkSurfaceLevel (HEIGHT_OFFSET = -8, L20 and L65) and only affects aquifer surface levels on 26.3, not the world's declared bounds."
    caveBiomeDepthWindow = "The beta preset's cave biome Voronoi is configured with depthMinY=-64 and depthMaxY=64 (data/moderner_beta/moderner_beta/settings_preset/beta.json L93-L95) over a world declared Y -64..127. The Voronoi window therefore reaches below the historical Beta floor; whether a modern cave biome actually wins a sample was NOT runtime-verified."
  }

  parentDecisions2026_10_12 = [ordered]@{
    note = "Final authority decisions received from the parent. Recorded here so the evidence file cannot be read as still recommending the superseded options."
    noVersionSubstitution = "Do NOT drop or substitute the requested Pre-Classic and Release 1.7.10 eras, and do NOT treat Classic or 1.6.4 as an acceptable stand-in. Exact-era mapping is deferred research: Moderner Beta's existing generators and biome layers must be researched later to map eras precisely."
    noBlindReducedHeight = "Do NOT activate the upstream reduced_height pack blindly. It overrides vanilla minecraft:dimension_type/overworld, vanilla noise_settings, vanilla density functions and vanilla ore features, so it would affect unrelated normal Moderner Beta worlds. Activation is blocked on measurement."
    phase2OwnBiomeIds = "Phase 2 will likely own narrowly scoped historical biome ids/maps (with MIT attribution if any Moderner Beta JSON is derived), so that stripping modern features/spawns does not globally change unrelated normal Moderner Beta worlds."
    caveAvoidanceCandidates = "Preset noCaveBiomes (moderner_beta:cave_biome_provider = moderner_beta:none, exactly how the alpha preset does it) plus a biome-injection override are candidate mechanisms to avoid caves. Documented here as candidates only; not validated."
    boundedHeightConfig = "Any explicit bounded generation height config must be evidence-based."
    firstSliceStatus = "The first runtime slice may remain a terrain-integration prototype with modern leakage an explicitly UNMET criterion."
  }

  eraMappingInputsForLaterResearch = [ordered]@{
    purpose = "The parent deferred exact-era mapping but asked for documented source differences wherever shared generation settings are involved. This is the concrete input for that research."
    requestedErasWithoutAPreset = @(
      "Pre-Classic: no moderner_beta:* preset represents rd-/Cave Game. The earliest Classic preset is moderner_beta:classic_0_0_14a_08; whether its generator/biome layer configuration can represent earlier builds is UNKNOWN and must be established from source before it is used as a base.",
      "Release 1.7.10: no preset. The nearest file ids are moderner_beta:release_1_6_4 and moderner_beta:release_1_12_2 (1.7.x is skipped entirely). Whether 1.7.10 shares 1.6.4's layer pipeline / noise configuration is NOT established, and the two shipped presets differ substantially (below), so neither can be assumed to cover 1.7.x."
    )
    measuredDifferences_1_6_4_vs_1_12_2 = [ordered]@{
      method = "Compared the two shipped generated JSON files field by field (chunkSettings and biomeSettings key sets and values)."
      identicalKeySets = $true
      differingComponents = @(
        "moderner_beta:cave_generation -> 1_6_4 {fixCaveBorders:true, forceBetaCanyons:true, forceBetaCaves:true, seedMethod:'early_release', useCarvers:true, useNoiseCaves:false} vs 1_12_2 {fixCaveBorders:true, forceBetaCanyons:false, forceBetaCaves:false, seedMethod:'modern', useCarvers:true, useNoiseCaves:false}",
        "moderner_beta:noise_generator_settings -> 1_6_4 'moderner_beta:overworld_128' (Y -64..127) vs 1_12_2 'moderner_beta:overworld_256' (Y -64..255)",
        "moderner_beta:noise_3d_settings -> 1_6_4 {arraySurfaceNoise:true, simplexSurfaceNoise:false} vs 1_12_2 {arraySurfaceNoise:false, simplexSurfaceNoise:true}",
        "moderner_beta:forced_biome_height -> different height-override sets: 1_6_4 has 6 entries keyed on *hills (desert/forest/jungle/early_release_*) vs 1_12_2 has ~80 entries keyed on full modern biome ids including badlands/bamboo_jungle/cherry_grove/mangrove_swamp/pale_garden/oceans",
        "moderner_beta:structure_modifiers -> 1_6_4 registers the moderner_beta:ocean_shrine override; 1_12_2 has overrides {} (no ocean shrine)",
        "moderner_beta:surface_properties -> 1_12_2 sets gravelOceanBed:true and legacyBadlandsBands:true; 1_6_4 has both false",
        "biomeSettings -> 1_12_2 adds moderner_beta:use_32bit_layer_seed (1_6_4 omits it; both otherwise share fractal_layers + provider + temperature_height_scaling)"
      )
      biomeFractalLayerSourceLines = "Both presets build their layer pipelines in ModernBetaSettingsPresetData.java: preset164 at L1396 and preset1122 at L1968; the fractal layer code they share lives under level/biome/provider/fractal/. Reading those two builders against level/biome/provider/fractal/** is the concrete starting point for deciding whether 1.7.10 is representable."
      caveBiomeSettings = "Identical key sets for both (moderner_beta:cave_biome_voronoi + moderner_beta:provider)."
    }
    alphaCaveBiomeCorrection = [ordered]@{
      parentHypothesis = "Alpha uses DISABLED / no cave BIOMES."
      verdictAfterRecheck = "CORRECT on the cave-biome point, and this file previously mis-worded it."
      evidence = "data/moderner_beta/moderner_beta/settings_preset/alpha.json caveBiomeSettings has ONLY moderner_beta:provider, and its value is moderner_beta:none (= ModernBetaSettings.noCaveBiomes(), ModernBetaSettings.java L108-L112, which is DefaultedRegistry default for the cave_biome_provider registry, ModernBetaRegistries.java L46). beta.json's value is moderner_beta:voronoi with a full modern point set."
      whatIsNotDifferent = "Cave TERRAIN generation is NOT disabled in alpha: alpha.json and beta.json carry the same moderner_beta:cave_generation shape ({forceBetaCaves:true, forceBetaCanyons:true, useNoiseCaves:false, seedMethod:'beta'}). Only infdev/indev/classic presets use CaveGeneration.DISABLED (ModernBetaSettingsPresetData.java L250, L277, L304, L332, L354, L378)."
      implication = "moderner_beta:none on the cave_biome_provider registry is a shipped, first-party way to switch cave BIOMES off without touching cave carving - which is exactly the parent's candidate cave-avoidance mechanism."
    }
  }

  structuresDecorationsAndLeakage = [ordered]@{
    structureModifiersDefault = "StructureModifiers.getDefault (StructureModifiers.java L39-L47) = overrides {moderner_beta:ocean_shrine -> RandomSpread(64,16,TRIANGULAR,357)} and removed = [] (EMPTY)."
    beta173StructureModifiersInJson = "chunkSettings.'moderner_beta:structure_modifiers' has overrides = {moderner_beta:ocean_shrine: {...spacing 64, separation 16, salt 357, triangular}} and removed = [] - see beta173Preset.json."
    consequence = "For Beta 1.7.3 the mod removes NO structure sets. All vanilla structure sets (village, pillager_outpost, ancient_city, trial_chambers, ruined_portal, mineshaft, stronghold, monument, ...) remain candidates; createState() only filters by 'hasBiomesForStructureSet' (ModernBetaChunkGenerator.java L299-L326)."
    oceanShrine = [ordered]@{ id = "moderner_beta:ocean_shrine"; file = "data/moderner_beta/worldgen/structure/ocean_shrine.json"; biomesTag = "#moderner_beta:has_structure/ocean_shrine (=#minecraft:is_ocean)"; step = "surface_structures"; terrainAdaptation = "beard_thin"; nbt = "data/moderner_beta/structure/ocean_shrine/base.nbt"; json = $oceanShrineJson }
    betaCaveCarver = [ordered]@{ file = "data/moderner_beta/worldgen/carver/beta_cave.json"; yRange = "biased_to_bottom 0..127"; json = $betaCaveJson; note = "historically bounded Y range, consistent with Beta caves; the preset enables forceBetaCaves/forceBetaCanyons and disables useNoiseCaves." }
    modernDecorationLeakage = [ordered]@{
      verdict = "CONFIRMED. moderner_beta biomes list modern vanilla features directly."
      example = "data/moderner_beta/worldgen/biome/beta_plains.json L153-L215 features array includes minecraft:amethyst_geode, minecraft:monster_room, minecraft:monster_room_deep, minecraft:ore_dirt, ore_gravel, ore_granite_lower, ore_diorite_lower, ore_andesite_lower, ore_tuff, ore_coal_upper/lower, ore_iron_*, ore_gold(_lower), ore_redstone(_lower), ore_diamond(+medium/large/buried), ore_lapis(+buried), ore_copper, underwater_magma, minecraft:spring_water, spring_lava, minecraft:glow_lichen, minecraft:flower_plains, minecraft:patch_pumpkin, minecraft:patch_sugar_cane, minecraft:patch_firefly_bush_near_water."
      modHistoricalFeatures = "The same array mixes in the mod's own re-implementations: moderner_beta:ore_clay, moderner_beta:ore_emerald_y95, moderner_beta:patch_dandelion_3, patch_poppy, patch_grass_plains_10, patch_brown_mushroom, patch_red_mushroom, moderner_beta:freeze_top_layer."
      carvers = "beta_plains carvers = [moderner_beta:beta_cave, moderner_beta:beta_cave_deep, moderner_beta:beta_canyon]."
      quantification = "Across data/moderner_beta/worldgen (176 json files) namespace references are minecraft:* = 4925 occurrences vs moderner_beta:* = 642. In placed_feature+feature only, the top minecraft refs are oak_log/oak_leaves (195 each), matching_blocks (194), biome (58), in_square (56), heightmap (44), trapezoid (42), count (41), surface_water_depth_filter (39), random_selector (38), fallen_oak_tree (34)."
      implication = "Even a 'Beta 1.7.3' world generates modern ores, geodes, monster rooms, springs, glow lichen and modern flower/vegetation patches unless a data pack or per-world settings override removes them. This must be handled by the project, not assumed away."
    }
  }

  generatorSelectionApis = [ordered]@{
    A_worldPresetRegistryId = [ordered]@{ id = "moderner_beta:moderner_beta"; ref = "ModernBetaWorldPresets.java L33"; use = "select the world type when creating a level" }
    B_serverPropertiesGeneratorSettings = [ordered]@{
      ref = "WorldDimensionDataMixin.java L37-L68 (injects at TAIL of DedicatedServerProperties$WorldDimensionData#create, only when worldPreset.is(ModernBetaWorldPresets.MODERN_BETA))"
      acceptedForms = @('a bare preset reference object, i.e. an object with a "preset" key whose value is "moderner_beta:beta"', 'a full ModernBetaSettingsPreset JSON (same schema as settings_preset/*.json)')
      parsingCodec = "ModernBetaSettingsPreset.SETTINGS_TEXT_CODEC (L46) -> Codec.either(CODEC, PRESET_REFERENCE_CODEC)"
      hardPrerequisite = "The world preset must already be moderner_beta:moderner_beta, otherwise the mixin does not trigger at all. generator-settings alone cannot switch a non-Moderner-Beta world into Moderner Beta terrain."
      noCredentialsNeeded = $true
    }
    C_modConfigFile = [ordered]@{
      path = "config/moderner_beta.json (FabricLoader configDir; ModernerBeta.loadConfig L126-L156)"
      keys = @("oldFogColorWeighting (bool, default true)", "defaultSettingsPreset (Identifier, default moderner_beta:beta; MiscConfig.java L16)")
      loadedAt = "mod init: ModernerBetaFabric.java L56 ModernerBeta.loadConfig(...) - BEFORE ServerLifecycleEvents.SERVER_STARTING (L67)"
      effect = "Sets what moderner_beta:default resolves to for every world whose stored settings contain the 'default' sentinel."
      warning = "Changing this value changes the terrain of EXISTING worlds that use the sentinel. It is a global generator switch, not a per-world one."
    }
    D_levelDatDirectEdit = [ordered]@{ shape = "see serializationSchema.worldLevelDatShape.json"; note = "Offline-writable but hostile: the mixin path (B) exists precisely because dedicated servers need a text-based route." }
    E_javaApi = [ordered]@{
      replaceGenerator = "WorldDimensions.replaceOverworldGenerator(registries, new ModernBetaChunkGenerator(new ModernBetaBiomeSource(biomeGetter, presetGetter, settingsPreset.biomeSettings(), settingsPreset.caveBiomeSettings()), presetGetter, surfaceConfigGetter, settingsPreset.chunkSettings())) (WorldDimensionDataMixin.java L53-L64)"
      resolvePreset = "ModernBetaSettingsPreset.getPreset(Identifier, HolderGetter<ModernBetaSettingsPreset>) (ModernBetaSettingsPreset.java L266-L283) - returns Optional; logs a WARN and returns empty for unknown ids"
      presetRegistry = "registries.lookupOrThrow(ModernBetaResourceKeys.SETTINGS_PRESET) (ModernBetaResourceKeys.java L27)"
      mapPresetChain = "ModernBetaSettings.mapPreset(...) (ModernBetaSettings.java L139-L155) resolves the preset reference chain; resolveDefaultPreset() (L157-L165) expands the 'default' sentinel"
      registrationForNewProviders = "ModernerBeta.setupCustomRegistryHandlers() (ModernerBeta.java L95-L108) + ModernBetaRegistries.CHUNK/BIOME/CAVE_BIOME (ModernBetaRegistries.java L43-L53, all defaulted registries)"
    }
    F_savedUserPresets = [ordered]@{ path = "config/moderner_beta/datapacks/moderner_beta_saved_presets (ModernBetaSavedPresetPack.java L28-L41)"; idScheme = "moderner_beta:default_presets/<sha256-of-canonical-json> (L78)"; note = "Saved presets are expanded (base preset flattened) before writing (L66-L76), so they are self-contained and survive a default-preset change." }
  }

  pipelineAndCacheImplicationsForRestartUpgrade = [ordered]@{
    whenProvidersAreBuilt = "ModernBetaLevelInitializer.initStarting (L17-L39) is registered on Fabric SERVER_STARTING (ModernerBetaFabric.java L67). It walks the LEVEL_STEM registry and calls ModernBetaBiomeSource.initProvider(seed) then ModernBetaChunkGenerator.initProvider(seed). Server start is the ONLY place providers are created."
    whatHoldsLiveState = @(
      "ModernBetaChunkGenerator.chunkProvider (built in initProvider L279-L296 from ModernBetaRegistries.CHUNK.getValue(chunkSettings.getProvider()).apply(this, seed))",
      "ChunkProviderHolder (L1008-L1019, volatile field, set at L292) - feeds ModernBetaChunkSurfaceLevel (L166) and the NoiseRouter's chunk_surface_level in the 26.3 path",
      "ModernBetaBiomeSource.biomeProvider / caveBiomeProvider / biomeInjectionHandler (L90-L107)",
      "CachedBiomeInjectionHandler wrapper (L104-L106) and the biome injection caches cleared by clearBiomeInjectionCaches (L55-L63)",
      "generatorSettings() holder: DefferedDirectHolder.of(lazy supplier) (ModernBetaChunkGenerator.java L130) - the NoiseGeneratorSettings object itself is built lazily from the resolved preset"
    )
    restartBehavior = "Re-opening a world re-reads the chunk generator from level.dat, which stores only the settings maps (provider_settings / cave_provider_settings). initProvider runs again at SERVER_STARTING, so a restart with an edited settings map or an edited config produces DIFFERENT terrain in never-generated chunks while already-generated chunks keep their stored blocks - a silent seam, not an error."
    unsafeSwitchingPoints = @(
      @{ point = "Editing defaultSettingsPreset in config/moderner_beta.json"; why = "moderner_beta:default is stored inside level.dat for any world created through the UI default path; getPreset() resolves it through ModernerBeta.getDefaultPresetId() at load time (ModernBetaSettingsPreset.java L271-L273). Restarting then regenerates unloaded chunks under a different preset."; evidence = "ModernBetaSettingsPreset.java L266-L283; ModernerBeta.java L198-L202; ModernerBetaFabric.java L56 vs L67" }
      @{ point = "Editing config/moderner_beta.json *while the server is running*"; why = "config is read once at mod init (L56) and also written back by setDefaultSettingsPreset (ModernerBeta.java L181-L196) on successful save; an external concurrent write is last-writer-wins and can be silently overwritten."; evidence = "ModernerBeta.java L126-L196" }
      @{ point = "Live swap of the chunk provider / settings object"; why = "There is no public setter for chunkSettings or the provider list. ChunkProviderHolder.set is private (L1016-L1018). Any live swap would desynchronize: NoiseRouter/density functions are captured from generatorSettings() (L160-L186), carvers/Aquifer hold the surface-level function, biome injection caches retain stale biomes, and RandomState/StructureState are already seeded."; evidence = "ModernBetaChunkGenerator.java L85-L120, L123-L186, L279-L296, L1008-L1019" }
      @{ point = "Editing the stored per-world settings maps in level.dat"; why = "The settings map is merged with the referenced preset (mapPreset L139-L155) and differences are re-derived against the base preset via getDifference (L205-L229). A key removed or changed silently reshapes terrain; an unknown provider id throws at initProvider (getValue on a defaulted registry) and can prevent the level from starting."; evidence = "ModernBetaSettings.java L139-L155, L205-L229; ModernBetaChunkGenerator.java L282-L289" }
      @{ point = "/reload (END_DATA_PACK_RELOAD)"; why = "Clears biome injection caches (ModernerBetaFabric.java L69-L73 -> clearBiomeInjectionCaches L55-L63) but does NOT re-run initStarting. Datapack changes to settings presets take effect for newly generated chunks with already-initialized providers still holding the old provider objects; caches and providers can therefore disagree until the next restart."; evidence = "ModernerBetaFabric.java L69-L73; ModernBetaLevelInitializer.java L41-L63" }
      @{ point = "Switching a world's world-preset away from moderner_beta:moderner_beta (or the reverse)"; why = "The generator/biome-source codecs are type-specific. A stored moderner_beta:moderner_beta generator cannot be read by a vanilla noise generator and vice versa; also the WorldDimensionDataMixin path only fires when the world preset is moderner_beta:moderner_beta (L44)."; evidence = "WorldDimensionDataMixin.java L44-L67; ModernBetaChunkGenerator.java L1025-L1028" }
      @{ point = "Relying on getGenDepth()/getMinY() before initProvider"; why = "Both fall back to the noise settings (192 / -64) when chunkProvider == null, then change to chunkProvider.getWorldHeight()/getWorldMinY() after init. Anything that caches height bounds too early observes different values across the server-start boundary."; evidence = "ModernBetaChunkGenerator.java L949-L966" }
    )
    recommendedRestartRecipe = @(
      "1. Stop the server cleanly (drain generation; normal stop saves level.dat).",
      "2. Snapshot the whole world directory AND config/moderner_beta.json.",
      "3. Change exactly one thing: config defaultSettingsPreset, or the world's stored provider_settings.",
      "4. Start the server; watch the log for 'Unable to read settings NBT! Reverting to previous settings..' (ModernBetaSettingsPreset.java L254) or a getValue failure.",
      "5. Verify the effective preset. The /modernerbeta_debug_provider_settings command exists ONLY in a dev environment (ModernerBetaFabric.java L63-L65, ModernerBeta.DEV_ENV), so a production dedicated server must be verified by other means: read back the stored generator NBT, or observe block/biome content in a freshly generated region.",
      "6. Generate chunks only in a new area; never assume the existing chunks changed."
    )
  }

  minimalExternalDependencyIntegration = [ordered]@{
    principle = "Treat the published jar as an external, opaque MOD dependency. Do not fork, vendor, or patch .upstream, and do not re-implement any terrain algorithm (project AGENTS.md constraint)."
    dependency = [ordered]@{
      coordinate = "moderner-beta-fabric-5.0.0-alpha.4+26.3.jar"
      source = "https://cdn.modrinth.com/data/xkrdwmh2/versions/Ucgo1y8G/moderner-beta-fabric-5.0.0-alpha.4%2B26.3.jar"
      sha256 = Sha256Of $jar
      modrinthProject = "xkrdwmh2"; modrinthVersionId = "Ucgo1y8G"; license = "MIT"; versionType = "alpha"
      companion = "Fabric API 0.160.4+26.3 (moderner_beta requires fabric-api '*'; the 26.3 build pins 0.160.4+26.3 in versions/26.3/gradle.properties)"
      pinningAdvice = "Pin the jar by SHA-256 and the loader by version. Multiple 5.0.0-alpha.4+<mc>-fabric jars share the same mod version string 5.0.0-alpha.4+<mc>, so version-string pinning alone is ambiguous across MC targets."
    }
    integrationSurface = @(
      "One launcher/dedicated-server instance per historical era profile; the era -> terrain binding is the settings preset id (moderner_beta:beta for Beta 1.7.3).",
      "Per-instance config/moderner_beta.json supplies defaultSettingsPreset so that no UI interaction is needed.",
      'Server-side world creation uses server.properties level-type=moderner_beta:moderner_beta plus generator-settings = an object with a "preset" key whose value is "moderner_beta:beta" (parsed by WorldDimensionDataMixin).',
      "The project's own profile/world metadata must record the preset id, the mod jar hash, and the MC/loader versions, because the mod itself stores only settings maps and the 'default' sentinel in level.dat."
    )
    explicitNonGoals = @("No custom chunk generator, biome provider, layer, feature, or carver registration.", "No datapack overrides of moderner_beta presets in the first slice.", "No live generator switching.")
    openRisk = "Modern decoration leakage (modern ores/geodes/monster rooms etc.) is present in the beta preset. Whether the project must strip it via a data pack is an architectural decision owned by the parent."
  }

  runtimeAcceptanceTestRecipe = [ordered]@{
    status = "NOT EXECUTED. Everything in this recipe is prepared from verified external facts (URLs, versions, hashes) plus the mod's own source; no Minecraft process was started by this research."
    environmentFactsVerified = [ordered]@{
      minecraftVersionExists = "26.3 is the current release in https://launchermeta.mojang.com/mc/game/version_manifest_v2.json (latest.release == 26.3)"
      javaInstalled = "java 25.0.4.1+1-LTS present on this host (java -version)"
      fabricLoaderAvailableFor26_3 = "https://meta.fabricmc.net/v2/versions/loader/26.3 returned 253 loader entries; newest listed 0.19.5, mod's own pin is 0.19.3"
      fabricApiArtifactExists = "HEAD https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.160.4+26.3/fabric-api-0.160.4+26.3.jar -> HTTP 200, 2587950 bytes"
      fabricInstaller = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.2/fabric-installer-1.1.2.jar"
    }
    recipeDedicatedServer = @(
      "0. Work in the project run/ directory only. Never touch %APPDATA%/.minecraft.",
      "1. mkdir run/era-beta173 and place moderner-beta-fabric-5.0.0-alpha.4+26.3.jar (sha256 above) + fabric-api-0.160.4+26.3.jar in run/era-beta173/mods/.",
      "2. Install the Fabric server for MC 26.3 with loader 0.19.3 via fabric-installer 1.1.2 (server -mcversion 26.3 -loader 0.19.3 -downloadMinecraft).",
      "3. In eula.txt set eula=true ONLY with explicit human authorization (project contract forbids accepting Mojang terms on the user's behalf).",
      '4. server.properties: level-type=moderner_beta:moderner_beta ; generator-settings={"preset":"moderner_beta:beta"} ; level-name=era-beta173-world ; online-mode=false for a local test.',
      '5. config/moderner_beta.json: {"moderner_beta:config_miscellaneous":{"oldFogColorWeighting":true,"defaultSettingsPreset":"moderner_beta:beta"}} - the key name must be the registered SettingsComponentType id.',
      "6. Start once, then assert: (a) the log contains no 'Unable to read settings JSON/NBT' error line from ModernBetaSettingsPreset; (b) the world's level.dat stores generator type 'moderner_beta:moderner_beta' with biome_source type 'moderner_beta:moderner_beta'; (c) /seed and a new chunk in a fresh region contain beta_* biomes only.",
      "7. Historical-fidelity spot checks (these are the acceptance probes, not proofs): sea level 64; sky/void above the Beta band rather than a 320-tall overworld; no amethyst geodes; no deepslate below Y 8 (beta preset sets deepslate enabled with minY 0 / maxY 8); Beta cave carver Y range 0..127.",
      '8. Negative-path check: set generator-settings to {"preset":"moderner_beta:does_not_exist"} and confirm the mod logs a warning/refusal rather than silently falling back (getPreset logs WARN at ModernBetaSettingsPreset.java L279; the mixin''s resultOrPartial logs parse errors at L48).',
      "9. Restart-upgrade check: stop, snapshot, change config defaultSettingsPreset to moderner_beta:beta_1_8_1, restart, generate in a NEW area, and confirm the old chunks are byte-identical while the new area differs. This is the concrete demonstration that preset switching is a restart-time, partial-world operation."
    )
    deterministicAlternativeWithoutMinecraft = @(
      "Datagen-only check, no game launch, no credentials: fetch tag 5.0.0-alpha.4 (commit 95f3da6), run './gradlew runDatagen' with JDK 25, and diff src/main/generated/data/moderner_beta/moderner_beta/settings_preset/*.json against the same paths extracted from the published jar. Expected: byte-identical for all 71 files (hashes in presetInventory)."
    )
  }

  reproduction = [ordered]@{
    commands = @(
      "git -C .upstream/moderner-beta ls-remote --tags origin   # showed 5.0.0-alpha.1..alpha.4 only; no 4.1.x tags",
      "git clone --bare https://github.com/Nostalgica-Reverie/moderner-beta.git .upstream/refs-26.3",
      "git --git-dir=.upstream/refs-26.3 fetch --depth 60 origin refs/heads/work/26.3:refs/heads/work/26.3 refs/tags/5.0.0-alpha.4:refs/tags/5.0.0-alpha.4",
      "git --git-dir=.upstream/refs-26.3 --work-tree=.upstream/alpha4-tree checkout -f 5.0.0-alpha.4",
      "Invoke-WebRequest https://cdn.modrinth.com/data/xkrdwmh2/versions/Ucgo1y8G/moderner-beta-fabric-5.0.0-alpha.4%2B26.3.jar -OutFile .upstream/dist/...jar",
      "expand the jar's data/moderner_beta/** into .upstream/extracted (this is how every preset/carver/structure/biome JSON in this file was obtained)"
    )
    sourceUrls = @(
      "https://github.com/Nostalgica-Reverie/moderner-beta",
      "https://codeberg.org/Nostalgica-Reverie/moderner-beta",
      "https://github.com/Nostalgica-Reverie/moderner-beta/releases/tag/5.0.0-alpha.4",
      "https://modrinth.com/mod/moderner-beta/version/5.0.0-alpha.4+26.3-fabric",
      "https://api.modrinth.com/v2/project/xkrdwmh2/version",
      "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json",
      "https://meta.fabricmc.net/v2/versions/loader/26.3"
    )
    inTreePaths = [ordered]@{
      tagTree = ".upstream/alpha4-tree (checked out at 5.0.0-alpha.4)"
      jar = ".upstream/dist/moderner-beta-fabric-5.0.0-alpha.4+26.3.jar"
      extractedData = ".upstream/extracted (data/moderner_beta/** and data/minecraft/** from the jar)"
      referenceClone = ".upstream/refs-26.3 (bare, holds work/26.3 + the release tag)"
      upstreamWorkingClone = ".upstream/moderner-beta (shallow, branch multiversion @ 87c015b, untouched)"
    }
  }

  outOfScopeNotAssessed = [ordered]@{
    flashback = "NOT ASSESSED. Per the updated project contract the Flashback target is the user's https://github.com/j-w-i/Flashback fork, not the upstream release. No Flashback fork revision was fetched or inspected by this research, so nothing here should be read as fork compatibility, recording, playback or seeking evidence. The only loader-viability fact established is that Moderner Beta 5.0.0-alpha.4 publishes a Fabric 26.3 artifact; whether any Flashback revision coexists with it on Fabric 26.3/Java 25 is unverified."
    modernerBetaUpstreamArtifactAsFlashbackEvidence = "The upstream published artifact is used here ONLY as loader/target viability evidence (Fabric 26.3 + Java 25 + fabric-api 0.160.4+26.3)."
  }

  verificationState = [ordered]@{
    verifiedByReadingSource = @(
      "release commit/tag identity and branch divergence",
      "MIT license text and README license history",
      "all 71 generated preset JSON files (extracted from the published jar, per-file sha256 recorded)",
      "codec field names for ModernBetaSettingsPreset, ModernBetaSettings, ModernBetaChunkGenerator, ModernBetaBiomeSource",
      "default-preset sentinel resolution chain and config key names",
      "structure modifiers default (ocean_shrine override, empty removed list)",
      "modern feature leakage across all 56 biome JSON files",
      "built-in optional pack contents (reduced_height, deepslate_blobs)",
      "provider initialization timing and the volatile ChunkProviderHolder"
    )
    verifiedByNetworkFetch = @(
      "Modrinth version metadata + file hashes for 5.0.0-alpha.4+26.3-fabric",
      "GitHub release asset names for 5.0.0-alpha.4",
      "Mojang version manifest contains 26.3",
      "Fabric meta has loaders for 26.3; Fabric API 0.160.4+26.3 jar exists"
    )
    notVerified = [ordered]@{
      runtime = "No Minecraft process was started. No world was created. Terrain values in this file are read from source/JSON, never observed."
      dataFormatGenerality = "Mojang's on-disk level.dat NBT layout for 26.3 generators is inferred from the codec, not parsed from a real level.dat."
      historicalFidelity = "No Beta 1.7.3 reference world was compared against. 'Fidelity' statements here describe what the code and data contain, not a validated match to 1.7.3."
    }
  }

  uncertainties = [ordered]@{
    high = @(
      "Beta 1.7.3 vertical mapping: the preset declares Y -64..127 (min_y -64 + height 192 - 1) with sea level 64, so the historical 0..127 band is inside the envelope and the open question is only whether anything generates below Y 0 (see dimensionsAndHeight).",
      "Whether modern structures (ancient city, trial chambers, villages, ...) actually place in a beta world - depends on hasBiomesForStructureSet against beta_* biomes; not executed.",
      "Whether modern cave biomes (lush_caves, dripstone_caves, deep_dark, sulfur_caves) ever win a Voronoi sample given depthMaxY=64 and the beta terrain envelope.",
      "Whether 'moderner_beta:use_carvers' inside cave_generation still has any effect on 26.3 (26.3 uses generateCarvers with material rules); the field is present in every preset but no 26.3 code path was found reading it."
    )
    medium = @(
      "Byte-identity of the published jar's generated JSON vs a fresh './gradlew runDatagen' at tag 5.0.0-alpha.4 (hashes recorded so it is checkable).",
      "Whether 'reduced_height' + 'deepslate_blobs' can be force-enabled on a dedicated server without a client, and whether they are safe to enable on an existing world.",
      "How the mod behaves when a world's stored settings map references a provider id removed by a datapack (expected: getValue throws in initProvider, potentially blocking world load)."
    )
    low = @(
      "Exact structure of Mojang's on-disk level.dat NBT for 26.3 chunk generators (the JSON form is proven; the NBT form is inferred from the same codec)."
    )
  }
}

$out = Join-Path $ws "research\moderner-beta-evidence.json"
$evidence | ConvertTo-Json -Depth 32 | Set-Content -Path $out -Encoding utf8
"WROTE $out  bytes=$((Get-Item $out).Length)"






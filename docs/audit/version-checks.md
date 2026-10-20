# Version-check audit (read-only inventory)

Scope: every place **outside** `version/HistoricalVersions.java` and `version/HistoricalVersion.java` where code decides behaviour by the selected historical version. Searched `src/main/java` and `src/test/java`. Method: grep of `.chronology()`, `HistoricalVersions.chronology(`, `ERA_*`, `byId`, `byRevisionAlias`, `atOrAfter/before`, `appliesTo(`, `eraHas*`, `isFinite*`, `VersionWindow`, `.era()`, `historical_experience:` literals and version-keyed maps, followed by reading each hit in context. Statements marked **(static reading)** were not run.

Paths below are relative to `src/main/java/net/historicalexperience/` (tests: `src/test/java/net/historicalexperience/`).

Chronology scale: `chronology(era, major, minor, patch) = era + major*1000 + minor*100 + patch`, with ERA_ALPHA=400000, ERA_BETA=500000, ERA_RELEASE=600000. So Beta 1.8 = 501800, Beta 1.8.1 = 501801, Release 1.0.0 = 601000 (NOT 600000; ERA_RELEASE=600000 sits below every release).

## 0. Headline numbers

* Distinct check sites in main: **154** (rows S001-S156 less S127 and S156, which are not decision points; a version-keyed table is one row).
* Distinct boundaries: **46 rows** (B01-B46: 42 real milestone chronologies, 2 era-constant bounds B43/B44, and 2 pseudo-boundaries B45 "above every declared version" and B46 "finite world").
* Test-side: 18 grouped rows in section 4 (about 40 test files with boundary logic out of 122).
* Top files by token count of version-decision constructs (`.chronology()`, `chronology(`, `*_CHRONOLOGY`, `byId`, `appliesTo`, `.era()`, `olderThan`, `VersionWindow`): HistoricalGameplayTweaks 123, HistoricalTextureRules 24, HistoricalColormaps 16, HistoricalVillageRules 14, HistoricalEnchanting 13, HistoricalVersionCatalogue 13, HistoricalVersionLabel 10, HistoricalWorldTweaks 9, HistoricalTooltips 9, HistoricalVillageBlocks 9. By version-keyed ENTRIES instead, HistoricalBlockAvailability (about 118 introduce calls), HistoricalStructureRules (34), HistoricalCreativeCatalog (about 40) and HistoricalCraftingEra (about 45) are the largest.

Form codes: `LIT` = chronology integer literal; `CHR` = `HistoricalVersions.chronology(...)` expression; `ID` = version-id string + `byId`/`olderThan`; `GATE` = `appliesTo("name", id)` string key; `SET` = set/map keyed by version id; `WIN` = `VersionWindow`; `ERA` = era string; `EXACT` = equality on one build; `REG` = reads a flag from `VersionRules` (registry-driven, already declarative); `IDX` = index in `HistoricalBlockAvailability.CHRONOLOGY` list (a second ordering scale).
Range codes: `HO` = half-open [from, until); `GE` = [from, inf); `LT` = (-inf, until); `EX` = exact build; `ALL` = every declared version; `NA`.
`D` in the DUP column = the same real boundary is also expressed elsewhere (see section 2 for the others).

## 1. Check sites in src/main/java

<!-- BEGIN:SITES (pipe table; columns: id|file:line|boundary|form|domain|range|dup) -->

| id | file:line | boundary expressed | form | domain | range | dup |
|---|---|---|---|---|---|---|
| S001 | gameplay/HistoricalGameplayTweaks.java:59,70-73 | BETA_1_8 (501800 literal, derived BEFORE_BETA_1_8 id set) | LIT+SET | combat/gameplay | LT | D |
| S002 | gameplay/HistoricalGameplayTweaks.java:244 | gates skeletonStrafing, criticalHit, panic: before Beta 1.8 | GATE | combat/mobAI | LT | D |
| S003 | gameplay/HistoricalGameplayTweaks.java:246 | gate avoidSun: Release 1.2.1 proxied by release_1_2_5 | GATE+ID | spawn/mobAI | LT | D |
| S004 | gameplay/HistoricalGameplayTweaks.java:249 | gate customDeathLoot: Release 1.2.1 proxy release_1_2_5 | GATE+ID | loot | LT | D |
| S005 | gameplay/HistoricalGameplayTweaks.java:251 | gates breeding/tempting/babyAnimal: before Pre3 (docs say breeding+tempting are Pre2) | GATE+ID | gameplay | LT | D |
| S006 | gameplay/HistoricalGameplayTweaks.java:253 | gates itemPickup/monsterGear/monsterEnchant (1.4.2) and babyZombie (1.6.2) proxied by release_1_6_4 | GATE+ID | spawn/loot/mobs | LT | D |
| S007 | gameplay/HistoricalGameplayTweaks.java:258 | gates cooldown/sweep/swim: Release 1.9 (every declared version) | GATE | combat | ALL | D |
| S008 | gameplay/HistoricalGameplayTweaks.java:260 | gate punchShear: Beta 1.7 proxied by beta_1_7_3 | GATE+ID | gameplay | LT | D |
| S009 | gameplay/HistoricalGameplayTweaks.java:264 | gate toolDurability: Beta 1.2 | GATE+ID | gameplay | LT | D |
| S010 | gameplay/HistoricalGameplayTweaks.java:268 | gate mobArms: Indev 20100202 (all declared) | GATE | rendering | ALL | - |
| S011 | gameplay/HistoricalGameplayTweaks.java:276 | gate endPortalFrameExplodes: exactly Beta 1.9 Pre3 | GATE+EXACT(id) | gameplay | EX | D |
| S012 | gameplay/HistoricalGameplayTweaks.java:281 | gate jumpExperienceBug: exactly Beta 1.9 Pre1 | GATE+EXACT(id) | gameplay/xp | EX | D |
| S013 | gameplay/HistoricalGameplayTweaks.java:287 | gate silkySpawners: exactly Beta 1.9 Pre4 | GATE+EXACT(id) | loot | EX | D |
| S014 | gameplay/HistoricalGameplayTweaks.java:292 | gate endPortalFrameLightFifteen: exactly Beta 1.9 Pre3 | GATE+EXACT(id) | lighting | EX | D |
| S015 | gameplay/HistoricalGameplayTweaks.java:301 | gate snowballSurplus: exactly Alpha 1.0.6 | GATE+EXACT(id) | loot | EX | D |
| S016 | gameplay/HistoricalGameplayTweaks.java:310 | gate noToolShaping: Release 1.9/1.13 (all declared) | GATE | gameplay | ALL | - |
| S017 | gameplay/HistoricalGameplayTweaks.java:317 | gates instantBow/arrowSpeed/invincibleBow: before Beta 1.8 | GATE+SET | combat | LT | D |
| S018 | gameplay/HistoricalGameplayTweaks.java:322 | gate instantAir: before Beta 1.8 | GATE+SET | gameplay | LT | D |
| S019 | gameplay/HistoricalGameplayTweaks.java:328 | gate instantBonemeal: Release 1.5 (13w04a) | GATE+ID | gameplay | LT | D |
| S020 | gameplay/HistoricalGameplayTweaks.java:333 | gate punchTnt: Beta 1.7 proxy beta_1_7_3 | GATE+ID | gameplay | LT | D |
| S021 | gameplay/HistoricalGameplayTweaks.java:338 | gate grassSeeds: Beta 1.6 Test Build 3 proxy beta_1_6 | GATE+ID | gameplay | LT | D |
| S022 | gameplay/HistoricalGameplayTweaks.java:342 | gate oldFishingLoot: Release 1.7.2 (all declared) | GATE | loot | ALL | D |
| S023 | gameplay/HistoricalGameplayTweaks.java:346 | gate noFishingExperience: 1.4.2 proxy release_1_4 | GATE+ID | xp/loot | LT | D |
| S024 | gameplay/HistoricalGameplayTweaks.java:478,598 | alwaysOpenChest + oreDropsAsBlocks: every declared version (isDeclaredVersion) | ID | gameplay/loot | ALL | - |
| S025 | gameplay/HistoricalGameplayTweaks.java:525 | swordBlockingEnabled: Beta 1.8 onward (no upper bound at Release 1.9) | SET | combat | GE | D |
| S026 | gameplay/HistoricalGameplayTweaks.java:617 | betaExhaustionCosts: Beta 1.8 onward | SET | food | GE | D |
| S027 | gameplay/HistoricalGameplayTweaks.java:628 | betaSprintJumpExhaustion: [Beta 1.8, release_1_6_4) (proxy for 1.3.1 and 1.7.2) | SET+ID | food | HO | D |
| S028 | gameplay/HistoricalGameplayTweaks.java:642,672 | functionalEnchanting: Beta 1.9 Pre4 literal 501_904 (null session => true) | LIT | enchanting | GE | D |
| S029 | gameplay/HistoricalGameplayTweaks.java:693 | enchantingDisabled: derived (not functional and not Pre3 window) | derived | enchanting | HO | D |
| S030 | gameplay/HistoricalGameplayTweaks.java:645,708-709 | placeholderEnchantmentTooltip: [Pre3 501_903, Pre4 501_904) literals | LIT | enchanting/UI | HO | D |
| S031 | gameplay/HistoricalGameplayTweaks.java:758,780-781 | f4SpawnsPortal: exactly Alpha 1.2.2a (chronology literal 401_202 AND id equals) | LIT+EXACT | gameplay | EX | D |
| S032 | gameplay/HistoricalGameplayTweaks.java:747,797 | netherAvailable: Alpha 1.2.0 literal 401_200 (null session => FALSE) | LIT | gameplay/dimension | GE | D |
| S033 | gameplay/HistoricalGameplayTweaks.java:812 | foodStacks: Beta 1.8 literal (null session => FALSE) | LIT | food/inventory | GE | D |
| S034 | gameplay/HistoricalGameplayTweaks.java:734,827 | offhandUnavailable: Release 1.9 literal 601_900 | LIT | combat/UI | LT | D |
| S035 | gameplay/HistoricalGameplayTweaks.java:858 | experienceLevelShown: Pre3 literal (null => true) | LIT | UI/xp | GE | D |
| S036 | gameplay/HistoricalGameplayTweaks.java:720,880 | sneakingAvailable: Alpha 1.1.0 literal 401_100 (null => true) | LIT | gameplay | GE | D |
| S037 | gameplay/HistoricalGameplayTweaks.java:899 | functionalBrewing: Pre3 literal (null => true) | LIT | gameplay | GE | D |
| S038 | gameplay/HistoricalGameplayTweaks.java:915 | endPortalFunctional: Pre4 literal (null => true) | LIT | gameplay/dimension | GE | D |
| S039 | gameplay/HistoricalGameplayTweaks.java:937 | enchantingTableExists: Pre3 literal (null => false; no caller in main) | LIT | gameplay | GE | D |
| S040 | gameplay/HistoricalGameplayTweaks.java:949 | betaGoldenApple: [Beta 1.8, release_1_1) | SET+ID | food | HO | D |
| S041 | gameplay/HistoricalGameplayTweaks.java:955 | betaFoodHungerValues: [Beta 1.8, release_1_6_4) | SET+ID | food | HO | D |
| S042 | gameplay/HistoricalGameplayTweaks.java:961 | betaRegenerationModel: Beta 1.8 onward | SET | food | GE | D |
| S043 | gameplay/HistoricalGameplayTweaks.java:965 | betaCriticalHitFormula: [Beta 1.8, release_1_6_4) | SET+ID | combat | HO | D |
| S044 | gameplay/HistoricalGameplayTweaks.java:971 | swordDamageBeforeReleaseOnePointZero: before Pre3 | ID | combat | LT | D |
| S045 | gameplay/HistoricalGameplayTweaks.java:986 | blockFallDamageReductionEnabled: [Beta 1.8, Pre3) | SET+ID | combat | HO | D |
| S046 | gameplay/HistoricalGameplayTweaks.java:996 | experienceOrbSpawnDisabled: `rules().awardsExperience` | REG | xp | NA | - |
| S047 | gameplay/HistoricalGameplayTweaks.java:362-388,550 | id constants BETA_1_6/1_7_3/1_2, RELEASE_1_1/1_4/1_5/1_2_5/1_6_4, PRE_1/3/4, ALPHA_1_0_6 + public `olderThanVersion` helper | ID | gameplay | NA | D |
| S048 | gameplay/HistoricalGameplayTweaks.java:1007-1009,353-358 | EVIDENCE map (17 keys) and gateNames() (21 names) are separate lists from the 33 `appliesTo` gates | SET | gameplay | NA | - |
| S049 | gameplay/HistoricalEnchanting.java:57,118 | RELEASE_1_8 (CHR 1.8.0) old table rules | CHR | enchanting | LT | D |
| S050 | gameplay/HistoricalEnchanting.java:61,82,107 | RELEASE_1_4.2 Thorns: CHR(1,4,2)=601402 (above the Release 1.4 profile 601400) | CHR | enchanting | GE | D |
| S051 | gameplay/HistoricalEnchanting.java:72-79,107 | 17 enchantments at Pre4 expressed as pre_3.chronology()+1 | ID+arith | enchanting | GE | D |
| S052 | gameplay/HistoricalEnchanting.java:80,107 | RELEASE_1_1 Flame/Infinity/Power/Punch | CHR | enchanting | GE | D |
| S053 | gameplay/HistoricalEnchanting.java:85,107 | RELEASE_1_7_2 Luck of the Sea / Lure (unreachable) | CHR | enchanting | GE | D |
| S054 | gameplay/HistoricalEnchanting.java:128-131 | Pre4 +1 recomputed; exact match `entry.getValue()==prereleaseFour` | ID+arith | enchanting | EX | D |
| S055 | gameplay/HistoricalDragonFight.java:40,98 | RELEASE_1_9 CHR dragon revamp | CHR | gameplay/boss | LT | D |
| S056 | gameplay/HistoricalDragonFight.java:46,89 | RELEASE_1_4 (1.4.0 CHR; real boundary 1.4.2/12w34b) boss-bar caption | CHR | UI | LT | D |
| S057 | worldgen/HistoricalVillageRules.java:93,196,203 | BETA_1_8_1 chronology: villageVersions()/noVillageVersions() via atOrAfter/before | CHR+WIN-like | worldgen | GE | D |
| S058 | worldgen/HistoricalVillageRules.java:214-218 | generationEnabledForVersion: BETA_1_8_1 | CHR | worldgen | GE | D |
| S059 | worldgen/HistoricalVillageRules.java:229-234 | generationEnabledForAlias: BETA_1_8_1 (second copy of the check) | CHR | worldgen | GE | D |
| S060 | worldgen/HistoricalVillageStructure.java:191-196 | villagers: BETA_1_9_PRE_1_CHRONOLOGY | CHR | worldgen/spawn | GE | D |
| S061 | worldgen/HistoricalVillagePieces.java:527-528 | villagers: BETA_1_9_PRE_1 (second copy) | CHR | worldgen/spawn | GE | D |
| S062 | worldgen/HistoricalVillagePieces.java:539 | professions: RELEASE_1_3_CHRONOLOGY | CHR | worldgen/spawn | GE | D |
| S063 | worldgen/HistoricalVillagePieces.java:1199-1205 | blacksmith horse armour: chronology(600_000,1,6,1) (era as raw literal) | CHR(raw) | loot | GE | D |
| S064 | worldgen/HistoricalVillageBlocks.java:238-249 | carrots/potatoes and sandstone stairs: release_1_6_4 id lookup (doc says 1.4.2 / 1.3) | ID | worldgen | GE | D |
| S065 | worldgen/HistoricalVillageBlocks.java:257 | brewing stand in church: BETA_1_9_PRE_3_CHRONOLOGY | CHR | worldgen | GE | D |
| S066 | worldgen/HistoricalVillageBlocks.java:287 | dirt path: chronology(600_000,1,9,0) (raw era literal) | CHR(raw) | worldgen | GE | D |
| S067 | gameplay/HistoricalSpawnRules.java:563,573 | NETHER_ENDERMAN: Release 1.10 literal 602_000 | LIT | spawn | GE | D |
| S068 | server/GenerationVerification.java:760-761 | NETHER_ENDERMAN again (verification, orElseThrow) | LIT | spawn | LT | D |
| S069 | gameplay/HistoricalLootRules.java:181-185 | usesLegacySheepDrops: Release 1.8 CHR | CHR | loot | LT | D |
| S070 | gameplay/HistoricalLootRules.java:175,190 + HistoricalFoodRules.java:227,283 + HistoricalPlayerRules.java:59,66,73 | per-version rule flags/tables (mobDrops, foodHealPoints, hasHunger, allowsSprinting, hasStructures) | REG | loot/food | NA | - |
| S071 | gameplay/HistoricalCreativeRules.java:21-23 | old creative menu [Beta 1.8.1, Release 1.3.0) | CHR | UI | HO | D |
| S072 | gameplay/HistoricalCreativeCatalog.java:14-70 | ~40 `from(<profile id>, "items...")` windows; first = profile chronology, end = MAX | SET(id)->WIN | UI/items | GE | D |
| S073 | gameplay/HistoricalCreativeCatalog.java:43 | petrified_oak_slab window [beta_1_3, release_1_3) | ID->WIN | UI/items | HO | D |
| S074 | gameplay/HistoricalCraftingEra.java:86-172 | ~45 `from(<profile id>, "items")` : EXISTED_BY_1_6_4 map (id -> first version id) | SET(id) | crafting | GE | D |
| S075 | gameplay/HistoricalCraftingEra.java:191,228-231 | SPECIAL_RECIPES map + `before()` by HistoricalBlockAvailability.CHRONOLOGY.indexOf | SET+IDX | crafting | LT | D |
| S076 | gameplay/HistoricalCraftingEra.java:302 | colouredBedsChronology 602_200 (Release 1.12) | LIT | crafting | NA | D |
| S077 | gameplay/HistoricalItemAvailability.java:58-131 | INTRODUCED table via `at(<id suffix>)` / `never()` ; 11 boundary ids | SET(id) | loot | GE | D |
| S078 | gameplay/HistoricalItemAvailability.java:148-154 | existsIn: chronology >= introduced.chronology() (MAX_VALUE for never) | CHR | loot | GE | - |
| S079 | generation/HistoricalBlockAvailability.java:77-112 | CHRONOLOGY list: 34 ids oldest-first (a second ordering, tested equal to the registry) | IDX | worldgen | NA | D |
| S080 | generation/HistoricalBlockAvailability.java:126-142 | VERSIONS_WITH_EXTRACTED_PALETTE set (15 ids) | SET | worldgen | NA | - |
| S081 | generation/HistoricalBlockAvailability.java:144-722 | introduce(<id>, ...) : ~118 calls over 17 distinct version ids (indev_latest, alpha_1_0_1/4/6/11, alpha_1_2_0, beta_1_2/3/5/6/7_3/8_1, beta_1_9_pre_1/3, release_1_0_0/1_3/1_6_4) | SET(id) | worldgen/blocks | GE | D |
| S082 | generation/HistoricalBlockAvailability.java:745-762 | judge(): CHRONOLOGY.indexOf comparison | IDX | worldgen | LT | - |
| S083 | generation/HistoricalBlockPalette.java:294 | paletteFor: `"historical_experience:beta_1_7_3".equals(id)` | EXACT(id) | worldgen | EX | D |
| S084 | generation/HistoricalStructureRules.java:160-296 | RULES map: exhaustive per-version entry (34 keys) with allowed structure sets | SET(id) | worldgen | NA | D |
| S085 | generation/HistoricalStructureRules.java:143-148 | BETA_1_8_STRUCTURES (mineshaft, stronghold, village) ; fortress = Pre1 ; temples = Release 1.3 ; witch hut = Release 1.4 | SET | worldgen | GE | D |
| S086 | generation/HistoricalFeatureExclusions.java:415,422,990 | eraHasEmerald(alias) via HistoricalVersions.eraHasEmeraldOre | REG(chrono) | worldgen/ore | GE | D |
| S087 | gameplay/EarlyGameplayRecipes.java:69-71,80 | recipe data keyed by indev_latest / infdev_420 / infdev_611 ids | SET(id) | crafting/food | EX | - |
| S088 | gameplay/HistoricalFoodRules.java:186,190 | foodHealing(infdev_420), foodHealing(indev_latest) | EXACT(id) | food | EX | - |
| S089 | gameplay/HistoricalToolDurability.java:77 | consumer of gate toolDurability + BEFORE_BETA_1_2 value table | GATE | gameplay | LT | D |
| S090 | gameplay/HistoricalWeather.java:54,65-67 | weather: Beta 1.5 id | ID | rendering/gameplay | GE | D |
| S091 | gameplay/HistoricalLeftClickEvents.java:42,77-78 | left-click activation ends at release_1_4 (=1.4.2) | ID | gameplay | LT | D |
| S092 | version/HistoricalSnapshotPatches.java:23,32-39 | patch->base profile ids (release_1_1, release_1_4); `startsWith("historical_experience:release_")` | SET+string | UI/persistence | EX | D |
| S093 | version/HistoricalVersionCatalogue.java:63-69,368 | Era enum bases 200000..600000 (duplicates ERA_*); order() | LIT | UI | NA | D |
| S094 | version/HistoricalVersionCatalogue.java:251-304 | display-version -> profile id table (proxy table: Beta 1.7->1.7.3, Beta 1.8->1.8.1, Release 1.2->1.2.5, Release 1.6->1.6.4) | SET(id) | UI | NA | D |
| S095 | client/lighting/HistoricalWorldTweaks.java:63,90-107 | cloud height 108 -> 128 at Beta 1.8 (id beta_1_8_1) | ID | rendering | LT | D |
| S096 | client/lighting/HistoricalWorldTweaks.java:66,112-113 | 2D dropped items end at release_1_5 | ID | rendering | LT | D |
| S097 | client/lighting/HistoricalWorldTweaks.java:132-134 | recipe book hidden (availableForRecipeBook=false, all) | ALL | UI | ALL | - |
| S098 | client/lighting/HistoricalWorldTweaks.java:172-221 | flat sky window [infdev_420, beta_1_8_1) via 3 byId lookups | ID | rendering | HO | D |
| S099 | client/lighting/HistoricalWorldTweaks.java:225-226,242-244 | era strings "infdev","alpha","beta" switch (redundant inside the window above) | ERA | rendering | NA | D |
| S100 | client/lighting/HistoricalFogTweaks.java:41-43 | beforeBeta18 with inline beta_1_8_1 lookup | ID | rendering | LT | D |
| S101 | client/lighting/HistoricalOldLighting.java:61,83-89 | old lighting ends before beta_1_8_1 | ID | lighting | LT | D |
| S102 | client/lighting/HistoricalLightingTweaks.java:24,74-77 | leaves/water lighting end before beta_1_8_1 | ID | lighting | LT | D |
| S103 | client/lighting/HistoricalSmoothLighting.java:66,69,86-96 | smooth lighting [beta_1_3, beta_1_8_1) | ID | lighting | HO | D |
| S104 | mixin/MinecraftHistoricalSmoothLightingMixin.java:64,73-75 | before beta_1_3 => AO off (duplicate of S103 start) | ID | lighting | LT | D |
| S105 | client/lighting/HistoricalChestTweaks.java:44,53-59 | cube chest before beta_1_8_1 | ID | rendering | LT | D |
| S106 | client/lighting/HistoricalChestTweaks.java:47,63-69 | ender chest from release_1_3 | ID | rendering | GE | D |
| S107 | client/lighting/HistoricalStars.java:44-49 | stars style ALPHA before beta_1_5 | ID | rendering | LT | D |
| S108 | client/lighting/HistoricalStars.java:45,52 | stars style BETA before release_1_3 | ID | rendering | HO | D |
| S109 | client/textures/HistoricalTextureRules.java:78,263 | GOLDEN_DAYS_ALPHA_END = Alpha 1.2.0 (WIN) | CHR+WIN | textures | LT | D |
| S110 | client/textures/HistoricalTextureRules.java:82,260 | GOLDEN_DAYS_BASE_END = Release 1.2.5 + 1 | CHR+arith | textures | LT | D |
| S111 | client/textures/HistoricalTextureRules.java:86,118,123 | COBBLESTONE_CHANGE = Beta 1.7.0 (501700) | CHR+WIN | textures | HO | D |
| S112 | client/textures/HistoricalTextureRules.java:90,135,145 | MOSSY_COBBLESTONE_CHANGE = Beta 1.8.0 (501800) | CHR+WIN | textures | HO | D |
| S113 | client/textures/HistoricalTextureRules.java:94,184 | END_PORTAL_FRAME_REDRAW = Pre4 | CHR+WIN | textures | GE | D |
| S114 | client/textures/HistoricalTextureRules.java:98,229 | GOLDEN_DAYS_CHEST_END = Beta 1.8.1 + 1 (501802) | CHR+arith+WIN | textures | GE | D |
| S115 | client/textures/HistoricalTextureRules.java:113,160,202 | VersionWindow.ALWAYS rules (xp bar, ender eyes, ender chest) | WIN | textures | ALL | - |
| S116 | client/textures/AssetRule.java:64 / VersionWindow.java:26-44 / TexturePlan.java | window machinery (activeAt, contains) | WIN | textures | HO | - |
| S117 | client/colour/HistoricalColormaps.java:68,115,192 | FOLIAGE_CHANGE = Beta 1.2.0 | CHR | rendering | HO | D |
| S118 | client/colour/HistoricalColormaps.java:72,101,109,189 | COLOURMAP_START = Alpha 1.2.0 | CHR | rendering | GE | D |
| S119 | client/tooltip/HistoricalTooltips.java:49,85 | TOOLTIPS_ADDED = Beta 1.0 | CHR | UI | GE | D |
| S120 | client/tooltip/HistoricalTooltips.java:53,88 | MODERN_TOOLTIPS_FROM = Beta 1.9 Pre2 | CHR | UI | GE | D |
| S121 | client/tooltip/HistoricalTooltips.java:67,120 | ATTRIBUTE_TOOLTIPS_FROM = Release 1.6.1 (601601) | CHR | UI | GE | D |
| S122 | client/hud/HistoricalVersionLabel.java:49,69 | label removed Beta 1.6.5 | CHR | UI | LT | D |
| S123 | client/hud/HistoricalVersionLabel.java:52,72 | label restored Beta 1.8.0 | CHR | UI | HO | D |
| S124 | client/hud/HistoricalVersionLabel.java:55,72 | label removed Pre4 | CHR | UI | HO | D |
| S125 | client/hud/HistoricalVersionLabel.java:58,75 | label restored Pre5, until ERA_RELEASE | CHR | UI | HO | D |
| S126 | client/hud/HistoricalVersionLabel.java:83,86 | era string "indev" ; chronology < ERA_ALPHA => "Infdev" | ERA+CHR | UI | NA | - |
| S127 | client/HistoricalClientRules.java:75-90 | lookup only (activeVersion) - not a check; listed for completeness | ID | UI | NA | - |
| S128 | world/upgrade/BiomeCarryForward.java:65,104 | ALPHA_PLAINS_FROM = Alpha 1.2.0 | CHR | persistence/worldgen | GE | D |
| S129 | world/upgrade/BiomeCarryForward.java:69,107 | LATE_BETA_FROM = Beta 1.8.1 | CHR | persistence/worldgen | GE | D |
| S130 | world/upgrade/BiomeCarryForward.java:73,110 | EARLY_RELEASE_FROM = Release 1.0.0 | CHR | persistence/worldgen | GE | D |
| S131 | version/HistoricalVersions isFiniteWorld/isFinitePreset consumers: world/upgrade/FiniteEdgeRepair.java:111,139,153 | indev_latest by id set (FINITE_PROFILES) | SET(id) | persistence | EX | - |
| S132 | world/upgrade/HistoricalBiomeConversionDriver.java:235 | isFiniteWorld | SET(id) | persistence | EX | - |
| S133 | client/indev/IndevMapApplication.java:78 ; client/version/HistoricalVersionScreen.java:334 | isFiniteWorld | SET(id) | UI | EX | - |
| S134 | world/upgrade/GenerationUpgrade.java:122 ; WorldGenSettingsAdvance.java:129,242,355 ; FiniteTransitionIdentity.java:19 | isFinitePreset (alias based) | SET(alias) | persistence | EX | - |
| S135 | client/indev/IndevMapProbe.java:42 ; server/EarlyPlayerGameplayVerification.java:146 | literal indev_latest / `endsWith(":indev_latest")` (verification code in main) | EXACT(id) | persistence/test | EX | - |
| S136 | server/VillageResidentAcceptance.java:36-37,82,127 | `endsWith("beta_1_8_1")`, `endsWith("release_1_1")` (acceptance harness) | EXACT(string) | worldgen/test | EX | D |
| S137 | server/EnchantingVerification.java:92-94,214 | literal ids pre_3, pre_4, release_1_0_0, release_1_6_4, beta_1_7_3 (harness) | SET(id) | enchanting/test | NA | - |
| S138 | client/version/SnapshotAnvilProbe.java:64,78,102 ; ScreenProbe.java:103-110 | literal ids in probes | EXACT(string) | UI/test | EX | - |
| S139 | mixin/AgeableMobHistoricalSpawnMixin.java:45 | appliesTo("babyAnimal") | GATE | spawn | LT | D |
| S140 | mixin/AbstractZombieModelHistoricalArmsMixin.java:60,75 ; AnimationUtilsHistoricalArmBobMixin.java:57 | appliesTo("mobArms") | GATE | rendering | ALL | - |
| S141 | mixin/AnimalHistoricalBreedingMixin.java:48 | appliesTo("breeding") | GATE | gameplay | LT | D |
| S142 | mixin/TemptGoalHistoricalMixin.java:47 | appliesTo("tempting") | GATE | gameplay | LT | D |
| S143 | mixin/ClientLevelHistoricalAttackSoundMixin.java:68 ; ParticleEngineHistoricalSweepMixin.java:64 ; PlayerHistoricalCombatMixin.java:92 | appliesTo("sweep") | GATE | combat | ALL | D |
| S144 | mixin/GuiHistoricalAttackIndicatorMixin.java:65 ; PlayerHistoricalCombatMixin.java:59,70 ; server/HistoricalUpgradeService.java:143,145 | appliesTo("cooldown") (+ diagnostic text) | GATE | combat | ALL | D |
| S145 | mixin/PlayerHistoricalCombatMixin.java:81 | appliesTo("criticalHit") | GATE | combat | LT | D |
| S146 | mixin/FleeSunGoalHistoricalMixin.java:39 | appliesTo("avoidSun") | GATE | spawn/mobAI | LT | D |
| S147 | mixin/LivingEntityHistoricalMovementMixin.java:55 | appliesTo("swim") | GATE | gameplay | ALL | D |
| S148 | mixin/MobHistoricalCustomLootMixin.java:59 | appliesTo("customDeathLoot") | GATE | loot | LT | D |
| S149 | mixin/MobHistoricalEquipmentMixin.java:67,84,100 | appliesTo("monsterGear"), 2x ("monsterEnchant") | GATE | spawn | LT | D |
| S150 | mixin/MobHistoricalItemPickupMixin.java:57 | appliesTo("itemPickup") | GATE | spawn | LT | D |
| S151 | mixin/PanicGoalHistoricalMixin.java:46 | appliesTo("panic") | GATE | mobAI | LT | D |
| S152 | mixin/RangedBowAttackGoalHistoricalMixin.java:47 | appliesTo("skeletonStrafing") | GATE | combat | LT | D |
| S153 | mixin/SheepHistoricalShearMixin.java:84 | appliesTo("punchShear") | GATE | gameplay | LT | D |
| S154 | mixin/ZombieHistoricalSpawnMixin.java:52 | appliesTo("babyZombie") | GATE | spawn | LT | D |
| S155 | gameplay/HistoricalGameplayTweaks.java:393-504,1021-1130 | 22 thin wrappers `appliesTo("<literal>", ...)` repeating the gate strings a 2nd time | GATE | gameplay | NA | D |
| S156 | mixin consumers of named predicates (nether x2: NetherPortalBlockHistoricalNetherMixin:50, PortalShapeHistoricalNetherMixin:54; foodStacks x5: HistoricalFoodStackLimit:59, SlotHistoricalFoodStackMixin:88; offhand x6; sneaking x4; brewing, enchanting, end portal, food, crit, exhaustion, regeneration) | consumers, not boundaries | call sites | various | NA | - |

<!-- END:SITES -->

Counting convention: a row is one decision point (a gate arm, a predicate, a constant+its consumers, or a version-keyed table). Tables with many entries (S072, S074, S077, S081, S084) are ONE row each but contain ~40, ~45, ~25, ~118 and 34 entries. S127 and S156 are not decision points and are excluded from the totals below.

## 2. Distinct boundaries

<!-- BEGIN:BOUNDARIES (columns: id|milestone|chronology|forms used|files) -->

| id | milestone | chronology | forms used | files using it |
|---|---|---|---|---|
| B01 | INDEV_20100223 (oldest) | 200223 | ID, SET | HistoricalCreativeCatalog, HistoricalBlockAvailability, EarlyGameplayRecipes, HistoricalFoodRules, HistoricalStructureRules, FiniteEdgeRepair, IndevMapApplication |
| B02 | INFDEV_20100420 | 300420 | ID | HistoricalWorldTweaks (sky lower bound), HistoricalCreativeCatalog, EarlyGameplayRecipes |
| B03 | INFDEV_20100611 | 300611 | ID | HistoricalCreativeCatalog, EarlyGameplayRecipes |
| B04 | ALPHA_1_0_1 | 401001 | ID | HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B05 | ALPHA_1_0_4 | 401004 | ID | HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B06 | ALPHA_1_0_6 (exact + introduce) | 401006 | ID, EXACT | HistoricalGameplayTweaks (snowballSurplus), HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B07 | ALPHA_1_0_11 | 401011 | ID | HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B08 | ALPHA_1_0_14 | 401014 | ID | HistoricalCreativeCatalog, HistoricalCraftingEra |
| B09 | ALPHA_1_0_17 | 401017 | ID | HistoricalCreativeCatalog |
| B10 | ALPHA_1_1_0 (sneaking) | 401100 | LIT, ID | HistoricalGameplayTweaks, HistoricalCreativeCatalog |
| B11 | ALPHA_1_2_0 (Nether, colourmaps, biomes) | 401200 | LIT, CHR, ID | HistoricalGameplayTweaks, HistoricalTextureRules, HistoricalColormaps, BiomeCarryForward, HistoricalBlockAvailability, HistoricalItemAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B12 | ALPHA_1_2_2A (exact) | 401202 | LIT+EXACT, ID | HistoricalGameplayTweaks, HistoricalStructureRules |
| B13 | BETA_1_0 | 501000 | CHR, ID | HistoricalTooltips, HistoricalWorldTweaks |
| B14 | BETA_1_2 | 501200 | ID, CHR | HistoricalGameplayTweaks (toolDurability), HistoricalColormaps, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B15 | BETA_1_3 | 501300 | ID | HistoricalSmoothLighting, MinecraftHistoricalSmoothLightingMixin, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B16 | BETA_1_4 | 501400 | ID | HistoricalCreativeCatalog, HistoricalCraftingEra |
| B17 | BETA_1_5 | 501500 | ID | HistoricalWeather, HistoricalStars, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B18 | BETA_1_6 (Test Build 3 grass) | 501600 | ID | HistoricalGameplayTweaks (grassSeeds), HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B19 | BETA_1_6_5 (label removed) | 501605 | CHR | HistoricalVersionLabel |
| B20 | BETA_1_7 (via beta_1_7_3 proxy = 501703 AND Beta 1.7.0 literal 501700) | 501700/501703 | CHR, ID | HistoricalTextureRules (501700), HistoricalGameplayTweaks (punchTnt, punchShear: 501703), HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra, HistoricalBlockPalette, HistoricalStructureRules |
| B21 | BETA_1_8 (Adventure Update) | 501800 / 501801 / 501802 | LIT 501_800, ID beta_1_8_1, CHR(1,8,0), CHR(1,8,1), CHR(1,8,1)+1 | HistoricalGameplayTweaks, HistoricalTextureRules (mossy 501800; chest end 501802), HistoricalVersionLabel (501800), HistoricalCreativeRules (501801), BiomeCarryForward (501801), HistoricalVillageRules (501801), HistoricalWorldTweaks, HistoricalFogTweaks, HistoricalOldLighting, HistoricalLightingTweaks, HistoricalSmoothLighting, HistoricalChestTweaks, HistoricalStructureRules, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra |
| B22 | BETA_1_9_PRE_1 | 501901 | CHR, ID, EXACT | HistoricalVillageRules, HistoricalVillageStructure, HistoricalVillagePieces, HistoricalGameplayTweaks (jumpExperienceBug exact), HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalStructureRules (fortress) |
| B23 | BETA_1_9_PRE_2 | 501902 | CHR, ID | HistoricalTooltips, HistoricalCraftingEra (repair_item), HistoricalGameplayTweaks (doc/javadoc only for breeding/tempting) |
| B24 | BETA_1_9_PRE_3 | 501903 | LIT, ID, CHR, EXACT | HistoricalGameplayTweaks (many), HistoricalEnchanting (+1), HistoricalVillageRules/VillageBlocks, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra, HistoricalBlockPalette(no) |
| B25 | BETA_1_9_PRE_4 | 501904 | LIT 501_904, CHR, ID, pre3+1 | HistoricalGameplayTweaks, HistoricalEnchanting, HistoricalTextureRules, HistoricalVersionLabel, HistoricalCreativeCatalog |
| B26 | BETA_1_9_PRE_5 | 501905 | CHR | HistoricalVersionLabel |
| B27 | BETA_1_9_PRE_6 | 501906 | ID | HistoricalCreativeCatalog (short_grass/fern/leaves) |
| B28 | RELEASE_1_0_0 | 601000 | CHR, ID | BiomeCarryForward, HistoricalBlockAvailability, HistoricalItemAvailability, HistoricalCreativeCatalog, HistoricalStructureRules |
| B29 | RELEASE_1_1 | 601100 | ID, CHR | HistoricalGameplayTweaks (golden apple), HistoricalEnchanting, HistoricalItemAvailability, HistoricalCreativeCatalog, HistoricalSnapshotPatches |
| B30 | RELEASE_1_2 (1.2.1 desert well, wolves/ocelot, 1.2.5 proxy) | 601205 | ID | HistoricalGameplayTweaks (avoidSun, customDeathLoot), HistoricalItemAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra, HistoricalStructureRules |
| B31 | RELEASE_1_2_5_END (textures, +1) | 601206 | CHR+arith | HistoricalTextureRules |
| B32 | RELEASE_1_3 (emerald/ender chest/villager trading) | 601300 | CHR, ID | HistoricalVillageRules, HistoricalCreativeRules, HistoricalChestTweaks, HistoricalStars, HistoricalItemAvailability, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra, HistoricalStructureRules |
| B33 | RELEASE_1_4 (1.4.0 / 1.4.2) | 601400 / 601402 | CHR, ID, mixed | HistoricalGameplayTweaks (fishing XP, release_1_4), HistoricalLeftClickEvents, HistoricalDragonFight (601400), HistoricalEnchanting Thorns (601402), HistoricalItemAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra, HistoricalSnapshotPatches |
| B34 | RELEASE_1_4_2 PROXIED BY release_1_6_4 (gear/pickup/enchant) | 601604 | ID | HistoricalGameplayTweaks |
| B35 | RELEASE_1_5 (13w04a bone meal, 3D items, quartz) | 601500 | ID | HistoricalGameplayTweaks, HistoricalWorldTweaks, HistoricalItemAvailability, HistoricalCreativeCatalog |
| B36 | RELEASE_1_6_1 (attributes, horse armour) | 601601 | CHR | HistoricalTooltips, HistoricalVillagePieces |
| B37 | RELEASE_1_6_2/1_6_4 (baby zombie; declared newest) | 601604 | ID | HistoricalGameplayTweaks, HistoricalVillageBlocks, HistoricalItemAvailability, HistoricalBlockAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra, HistoricalStructureRules |
| B38 | RELEASE_1_7_2 | 601702 | CHR | HistoricalEnchanting (unreachable), HistoricalGameplayTweaks (oldFishingLoot, ALL) |
| B39 | RELEASE_1_8 | 601800 | CHR | HistoricalEnchanting, HistoricalLootRules |
| B40 | RELEASE_1_9 | 601900 | LIT 601_900, CHR | HistoricalGameplayTweaks (offhand; also cooldown/sweep ALL), HistoricalDragonFight, HistoricalVillageBlocks |
| B41 | RELEASE_1_10 | 602000 | LIT | HistoricalSpawnRules, GenerationVerification |
| B42 | RELEASE_1_12 | 602200 | LIT | HistoricalCraftingEra |
| B43 | ERA_ALPHA_START (Infdev vs Alpha) | 400000 | CHR(era const) | HistoricalVersionLabel |
| B44 | ERA_RELEASE_START (upper bound of Beta) | 600000 | CHR(era const) | HistoricalVersionLabel |
| B45 | ABOVE_EVERY_DECLARED (modern-only feature, "all declared" gates) | n/a | GATE ALL / isDeclaredVersion | HistoricalGameplayTweaks (cooldown, sweep, swim, mobArms, noToolShaping, oldFishingLoot, alwaysOpenChest, oreDropsAsBlocks), HistoricalWorldTweaks (recipe book) |
| B46 | FINITE_WORLD (Indev id set) | n/a | SET(id) | FiniteEdgeRepair, HistoricalBiomeConversionDriver, IndevMapApplication, HistoricalVersionScreen, WorldGenSettingsAdvance |

<!-- END:BOUNDARIES -->

## 3. Inconsistencies and proxy mismatches

Ranked by bug risk. "**(static reading)**" = not run.

### 3.1 HIGH

1. **Null-session defaults are inverted for `netherAvailable` and `foodStacks` (static reading).** `HistoricalGameplayTweaks.netherAvailable(null)` and `foodStacks(null)` return `false` (lines 797, 812; the Nether test even asserts `netherAvailable(null)==false`). Their consumers cancel/limit when the answer is false: `NetherPortalBlockHistoricalNetherMixin:50`, `PortalShapeHistoricalNetherMixin:54`, and `HistoricalFoodStackLimit.foodDoesNotStack` (`!foodStacks(current) && stack has FOOD`) used by `Slot/Menu/Container/InventoryHistoricalFoodStack*Mixin`. With `HistoricalActiveSession.current()==null` (ordinary world) the portal would be refused and food capped at stack size 1. The javadocs on `functionalEnchanting`, `sneakingAvailable`, `functionalBrewing`, `endPortalFunctional`, `experienceLevelShown` say "a null session answers the modern way - the owner reported this", and those five return `true` for null; these two were missed. No `isHistorical()` guard was found in the mixin bodies. Needs a runtime check in an ordinary world. A declarative `FeatureWindow` with an explicit `ordinaryWorld` answer removes this class of defect.
2. **"1.4.2" boundary has four different proxies.** (a) `release_1_6_4` id for itemPickup/monsterGear/monsterEnchant (Tweaks :253); (b) `release_1_4` id for noFishingExperience (:346) and left-click (`HistoricalLeftClickEvents:42`, comment "Release 1.4 represents 1.4.2"); (c) `chronology(RELEASE,1,4,2)=601402` for Thorns (`HistoricalEnchanting:61`), which is ABOVE the Release 1.4 profile (601400), so the profile that "represents 1.4.2" lacks Thorns; (d) `chronology(RELEASE,1,4,0)` for the boss-bar caption (`HistoricalDragonFight:46`). The javadocs on S006 still say "Release 1.6.4 is the only declared version after 1.4.2"; Release 1.4 and Release 1.5 are declared. Result: Release 1.4 and 1.5 worlds lack monster item pickup, mob gear and mob enchants even by the project's own "1.4 = 1.4.2" convention.
3. **Breeding/tempting boundary: code says Pre3, comments say Pre2.** Tweaks :251 gates `breeding`, `tempting`, `babyAnimal` all at `BETA_1_9_PRE_3`; the EVIDENCE text and the javadocs on `animalBreedingDisabled`/`animalTemptingDisabled` say Pre2 for breeding and tempting (and Pre3 only for baby animals). `HistoricalCraftingEra.SPECIAL_RECIPES` dates tool repair to Pre2. The test hard-codes the Pre3 for all three, so it would not catch it. A Pre2 world currently has no breeding.
4. **Items/blocks dated differently by four independent tables (HistoricalBlockAvailability, HistoricalItemAvailability, HistoricalCreativeCatalog, HistoricalCraftingEra).** Verified conflicts:
   * `nether_wart`: BlockAvailability release_1_0_0; ItemAvailability alpha_1_2_0; CreativeCatalog beta_1_9_pre_1.
   * `end_stone`: BlockAvailability release_1_0_0 (S081 :662); CreativeCatalog beta_1_9_pre_4 (comment in registry says End arrives Pre4).
   * `ender_pearl`: ItemAvailability release_1_0_0 (comment itself says "End arrives in Pre4"); CreativeCatalog beta_1_8_1.
   * `sandstone_stairs`: BlockAvailability release_1_6_4; CraftingEra and CreativeCatalog release_1_3; village code uses release_1_6_4.
   * `carrots`/`potatoes`: BlockAvailability and village release_1_6_4; ItemAvailability/CreativeCatalog release_1_4.
   * `short_grass`/`fern`: BlockAvailability beta_1_6; CreativeCatalog beta_1_9_pre_6.
   * `glistering_melon_slice`, `splash_potion`, `cake`, `cod`, `cooked_cod`, `pumpkin_pie`, `fire_charge` appear in `HistoricalItemAvailability.never(...)` (S077) while CreativeCatalog/CraftingEra date them at beta_1_2 ... release_1_3 (`never` is documented as chest-loot scoped, so some of these may be deliberate; they still share one namespace).
   * `fire_charge`: CraftingEra beta_1_9_pre_3; CreativeCatalog release_1_2_5. `gold_nugget`: CraftingEra pre_3; CreativeCatalog pre_1.
5. **Raw-literal boundaries that can drift and an obsolete justification.** `BETA_1_9_PRE_4_CHRONOLOGY = 501_904` and `..._PRE_3 = 501_903` (Tweaks :642-645) are justified by "Pre4 is not declared yet" - Pre4 IS declared (registry, 34 versions). `HistoricalEnchanting` re-derives Pre4 as `pre_3.chronology()+1` twice (:72-73, :128-129) and compares equality (`entry.getValue()==prereleaseFour`, an EXACT match on a derived number). Four more duplicate literals: Beta 1.8 `501_800`, Alpha 1.1.0 `401_100`, Alpha 1.2.0 `401_200`, Release 1.9 `601_900`, Release 1.10 `602_000` (two files), Release 1.12 `602_200`; all rely on tests to stay aligned.

### 3.2 MEDIUM

6. **Beta 1.8 is written five ways**: literal `501_800` (Tweaks), id `beta_1_8_1` string redefined independently in 7 classes (`HistoricalChestTweaks:44`, `HistoricalOldLighting:61`, `HistoricalSmoothLighting:69`, `HistoricalLightingTweaks:24`, `HistoricalWorldTweaks:63,179`, `HistoricalFogTweaks:42` inline, plus the `Minecraft...SmoothLightingMixin` copy for Beta 1.3), `chronology(BETA,1,8,0)` (texture mossy, label), `chronology(BETA,1,8,1)` (`HistoricalCreativeRules:21`, `BiomeCarryForward:70`, `HistoricalVillageRules:93`), and `chronology(BETA,1,8,1)+1` (chest textures, "ends after 1.8.1"). They agree today because nothing is declared between 501800 and 501801; adding a Beta 1.8 or 1.8.1_x profile would split them.
7. **Beta 1.7 proxy differs**: textures use `chronology(BETA,1,7,0)=501700` (`HistoricalTextureRules:86`), gameplay uses profile `beta_1_7_3`=501703 for punchTnt/punchShear (`:260,:333`), BlockAvailability dates pistons to `beta_1_7_3`. Declaring any 1.7.0-1.7.2 build would put the texture and gameplay boundaries on different builds.
8. **Release 1.6.x**: `chronology(600_000,1,6,1)` (tooltips attribute block, village blacksmith horse armour - same expression typed twice, with raw 600_000 in `HistoricalVillagePieces:1203`) vs `release_1_6_4` for horse armour/leads in ItemAvailability/CreativeCatalog vs `release_1_6_4` for baby zombies (real 1.6.2). Equivalent now, different if 1.6.1/1.6.2 are added.
9. **Era arithmetic done by hand**: raw `500_000`/`600_000` passed to `HistoricalVersions.chronology` in `HistoricalVillageRules:93,102,111,134`, `HistoricalVillageBlocks:287`, `HistoricalVillagePieces:1203`, `HistoricalSpawnRules` comment; `HistoricalVersionCatalogue.Era` repeats 200000..600000 (private ERA_INDEV/INFDEV/CLASSIC in the registry are private, only ALPHA/BETA/RELEASE are public); tests re-declare `ERA_*` in `VersionChronologyTest`, `NetherBoundaryTest`, `SneakingBoundaryTest`, `OffhandBoundaryTest`.
10. **Two parallel orderings**: `HistoricalBlockAvailability.CHRONOLOGY` (34-id list, index compare) used by `HistoricalCraftingEra` (S075) versus `HistoricalVersion.chronology()` used everywhere else. A test asserts they match; adding a version in the registry without the list breaks BlockAvailability `introduce` (throws "unknown version") at class init.
11. **`HistoricalGameplayTweaks.gateNames()` lists 21 of the 33 `appliesTo` gates** (missing: punchShear, toolDurability, endPortalFrameExplodes, jumpExperienceBug, silkySpawners, endPortalFrameLightFifteen, snowballSurplus, noToolShaping, instantBow, arrowSpeed, invincibleBow, instantAir). `EVIDENCE` has 17 keys, two of which (`monsterPickup`, `gearedMobs`) do not correspond to any gate name (the gates are `itemPickup`, `monsterGear`). The whole-table test iterates `gateNames()`, so 12 gates are never walked across all versions. 22 wrapper methods then repeat the gate literals again (S155) and 24 mixins repeat them a third time (S139-S154).
12. **Different default for unknown/null across predicates**: `appliesTo` and most `beta*` predicates -> false; `functionalEnchanting`/`sneakingAvailable`/`functionalBrewing`/`endPortalFunctional`/`experienceLevelShown` -> true; `netherAvailable`/`foodStacks`/`enchantingTableExists`/`swordBlockingEnabled` -> false; `HistoricalItemAvailability.existsIn` -> true; `HistoricalEnchanting.available` -> true for unknown version, false for undated enchantment; `HistoricalVillageRules.generationEnabledForVersion` -> throws; `generationEnabledForAlias` -> false.
13. **Same village rule in two places**: `HistoricalVillageRules` (Beta 1.8.1 chronology, S057-S059) and `HistoricalStructureRules.RULES` (village only in the beta_1_8_1+ allowed sets, S084-S085). Village-resident rules appear three times (VillageStructure:196, VillagePieces:528, harness `VillageResidentAcceptance:37,127`).

### 3.3 LOW / hygiene

14. Stale counts in comments: "all ten profiles", "all 32 builds", "thirteen declared", "newest is Release 1.6.4" while the registry now declares 34 versions (test `HistoricalGameplayTweaksTest` asserts 34).
15. `BEFORE_BETA_1_8` is derived from the literal 501_800, so it silently includes any future 501800..501800 build only by chronology; the id-based `olderThan(…, beta_1_8_1)` form would give a different answer for a hypothetical Beta 1.8.0 profile.
16. `swordBlockingEnabled`, `betaRegenerationModel` have no upper bound at the real Release 1.9 boundary (the registry has none above 1.6.4, so unreachable today).
17. `HistoricalWorldTweaks.skyEra` tests both a chronology window and an era-string switch; the switch is redundant. `HistoricalVersionLabel.textFor` mixes an era string and a chronology comparison.
18. Verification/probe classes in main (S135-S138) hard-code ids with `endsWith`; they will not follow a rename.

## 4. Test-side sites (src/test/java)

Boundaries are re-expressed in tests, mostly to tie a main-side literal to the registry. ~122 test files; the following contain version-boundary logic (approximate hit counts from grep):

| id | test file | what it pins |
|---|---|---|
| T001 | gameplay/HistoricalGameplayTweaksTest.java (36) | gate table by id/chronology; hard-coded lists BEFORE_BETA_1_8 etc.; PROFILES count 34 |
| T002 | version/VersionChronologyTest.java (19) | ERA_* re-declared; Alpha ordering |
| T003 | gameplay/HistoricalBugsTest.java (18) | exact-build gates (Pre1, Pre3, Pre4, Alpha 1.0.6) via appliesTo |
| T004 | version/HistoricalVersionsTest.java (16) | atOrAfter/before; byId |
| T005 | generation/HistoricalVillageGeneration1211Test.java (12) | Beta 1.8.1 boundary 501801 |
| T006 | client/tooltip/HistoricalTooltipsTest.java (12) | Beta 1.0, Pre2, Release 1.6.1/1.9 |
| T007 | client/textures/HistoricalTextureRulesTest.java (12) | Alpha 1.2.0, Release 1.2.5+1 windows |
| T008 | gameplay/NetherSpawnFilteringTest.java (11) | Alpha 1.2.0 via literal 400_000; Release 1.10 |
| T009 | version/SpawnRestrictionSemanticsTest.java (10) | per-version spawn rules |
| T010 | version/VersionTableConsistencyTest.java (10) | cross-table consistency |
| T011 | version/CatalogueOrderingTruthTest.java + client/version/SnapshotSelectorTest.java | catalogue order == registry chronology |
| T012 | gameplay/HistoricalRecipeGateTest.java (8) | crafting dates |
| T013 | worldgen/VillagerBoundaryTest.java (7) | Pre1, Release 1.0/1.2.5/1.3 |
| T014 | gameplay/NetherBoundaryTest.java, SneakingBoundaryTest.java, OffhandBoundaryTest.java, PreBeta18BoundaryTest.java, EnchantingBoundaryTest.java, HistoricalPortalSpawnTest.java | each ties a main literal (`*BoundaryChronology()`) to registry numbering; NetherBoundaryTest:86 asserts `netherAvailable(null)==false` |
| T015 | gameplay/EarlyMobDropFactsTest.java, HistoricalEnchantingTest.java, HistoricalDragonFightTest.java, HistoricalDamageValuesTest.java, HistoricalToolDurabilityTest.java, HistoricalWeatherTest.java, HistoricalPlayerRulesWiring1211Test.java | one boundary each |
| T016 | generation/EmeraldIntroductionBoundaryTest.java, LapisIntroductionBoundaryTest.java, EraOreRevisionTest.java, OrePopulationInvariantTest.java, PreReleaseNetherGatingTest.java, HistoricalStructureRulesTest.java, Alpha12BiomeRuleTest.java, ReportedNetherContentExclusionsTest.java | era-ore / structure / exclusion tables per version |
| T017 | client/lighting/HistoricalWorldTweaksTest.java, HistoricalFogTweaksTest.java, HistoricalChestTweaksTest.java, HistoricalStarsTest.java; client/hud/HistoricalVersionLabelTest.java; client/colour/HistoricalColormapsTest.java; client/config/OptionScopeTest.java | client boundaries |
| T018 | version/Alpha122aCloneTest.java, world/upgrade/BiomeCarryForwardTest.java, HistoricalRegionBiomesTest.java, BiomeConversionTriggerTest.java | upgrade/biome per-version |

Test duplicates worth noting: private `ERA_ALPHA/ERA_RELEASE/ERA_*` copies (4 files), raw `500_000` (`NetherBoundaryTest:67`), raw `chronology(400_000/600_000,...)` (`NetherSpawnFilteringTest:77,188`), and the same boundary list typed in `HistoricalGameplayTweaksTest:53-83`.

## 5. Where a declarative form would simplify the code

Proposal: a single `FeatureWindow(id, fromBoundary, untilBoundary, ordinaryWorldDefault)` table keyed by a `Boundary` enum (name -> `chronology`), with `Boundary` resolved once from the registry (declared version where one exists, `chronology(...)` literal otherwise), plus `exact(versionId)` for single-build bugs. Candidates (all currently if/else on ids or literals):

* **All of `HistoricalGameplayTweaks`** (S001-S047): the 33 gates are `[from, until)` windows plus 5 exact builds. One table `GATES = {name -> window}` replaces the 22 case arms, 18 hand-written predicates, the id constants, the 14 literals, `gateNames()` and parts of `EVIDENCE`; the null-session default becomes a field.
* **Windows with two ends already written by hand**: Smooth lighting `[B1.3, B1.8)` (S103/S104), flat sky `[Infdev, B1.8)` (S098), old creative menu `[B1.8.1, R1.3)` (S071), label (S122-S125, four ends), BiomeCarryForward (S128-S130), placeholder enchant tooltip (S030), block-fall-damage (S045), golden apple/food/crit (S040/S041/S043), stars (S107/S108).
* **Pure `LT B1.8` client rendering rules**: cloud height, fog, old lighting, leaves/water lighting, chest, sky (S095, S098, S100-S102, S105): six classes each with its own `FIRST_NEW_LIGHTING_BUILD` string.
* **Texture `VersionWindow` rules** are already declarative; they would only need to take `Boundary` instead of raw chronology arithmetic (`+1`).
* **Item/block/crafting/creative dating** (S072-S075, S077, S081): four id-keyed tables of the same fact ("introduced at X"). One `Introduced(itemOrBlockId, Boundary, evidence, scope)` table with per-scope views would eliminate the contradictions in 3.1(4).
* **Village rules** (S057-S066): one `VillageFeature` enum with a boundary each: exists (B1.8.1), inhabited (Pre1), professions (R1.3), horse-armour loot (R1.6.1), brewing stand (Pre3), carrots/sandstone stairs (R1.4.2), dirt path (R1.9).
* **Era strings**: replace `version.era()` switches with `Boundary` windows (S099, S126).
* **`appliesTo` string keys in mixins** (S139-S154): replace with a `Gate` enum so a typo is a compile error and `gateNames()` becomes `Gate.values()`.

## 6. Exact-build special cases

| gate/rule | build | where | note |
|---|---|---|---|
| endPortalFrameExplodes | Beta 1.9 Pre3 only | Tweaks :276 | wiki History row |
| endPortalFrameLightFifteen | Beta 1.9 Pre3 only | Tweaks :292 | same row |
| jumpExperienceBug / jumpRequiresFreshPress | Beta 1.9 Pre1 only | Tweaks :281,:1154,:1179 | Pre2 fixes |
| silkySpawners | Beta 1.9 Pre4 only | Tweaks :287 | inverted: false is the modern answer |
| snowballSurplus | Alpha 1.0.6 only | Tweaks :301 | fix boundary unknown |
| f4SpawnsPortal | Alpha 1.2.2a only | Tweaks :780 | checks both chronology 401202 and id string |
| placeholderEnchantmentTooltip | Pre3 only | Tweaks :708 | [Pre3, Pre4) |
| enchantment table `Pre4 + 1` equality | Pre4 | HistoricalEnchanting :129 | exact on a derived integer |
| HistoricalBlockPalette.paletteFor | beta_1_7_3 only | :294 | only extracted palette |
| FINITE_PROFILES | indev_latest only | registry set, 5 consumers | finite-world special case |
| VillageResidentAcceptance | beta_1_8_1 / release_1_1 | :37,:82,:127 | harness |
| Tweaks `isDeclaredVersion` ALL | every declared build | S010, S016, S022, S024 | not a boundary, "above the newest" |

## 7. Methodology notes and limits

* Greps covered `src/main/java` and `src/test/java`; large hits (HistoricalBlockAvailability 118 introduce calls, CreativeCatalog/CraftingEra `from` calls) were counted by script, not read one by one.
* Mixin consumers of named predicates (S156) were located by grep of the predicate names; consumer line numbers are from that grep. The null-session finding (3.1.1) is from reading the two mixins and `HistoricalFoodStackLimit`; it was not exercised at runtime.
* Registry-internal derivations (`eraHasRedstoneOre/LapisOre/EmeraldOre`, `withEraOres`, `GenerationRevision` lists, `HistoricalSpawnRules.*_MOBS` inheritance chains such as `BETA_1_4_MOBS = BETA_1_7_3_MOBS`, `HistoricalFeatureExclusions` named lists) are defined outside the registry files but referenced by them; they are version-keyed data, not branching sites, and are not rows above.

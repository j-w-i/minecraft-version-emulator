# Mixin audit — `net.historicalexperience.mixin` + `historical_experience.mixins.json`

Read-only audit; no source file was edited. Date of audit: 2026-10-20.

## 0. Method and limits (what is fact, what is not)

* Every one of the 145 files in `src/main/java/net/historicalexperience/mixin/` was read (comments excluded from line counts). Line numbers below are **source line numbers of the annotation** in the mixin file.
* "Verified in bytecode" means `javap -p -c` against the Mojang-mapped 1.21.1 merged jar in the Loom cache (`minecraft-merged-1.21.1-loom.mappings.1_21_1.layered+hash.2198-v2.jar`). "Verified in log" means a line in `run/verification/orefix-f-normal/logs/latest.log` (a dedicated-server run, `Env=SERVER`).
* Nothing was launched. Anything marked *unverified* is code-reading only.
* Not checked: Moderner Beta's own mixins for overlaps with ours (only noted where I saw a shared method name), and Fabric API mixins.

### Legend

| Code | Meaning |
|---|---|
| **AS** | `gameplay/HistoricalActiveSession.current()` — process-global static string. Set at `server/HistoricalVersionSync.java:84` (first/each player JOIN, from `HistoricalProfileLookup.profileIdForLevel(server.overworld())`), on the client at `client/HistoricalClientRules.java:55`; cleared at `HistoricalVersionSync.java:96` (SERVER_STOPPED) and `HistoricalClientRules.java:62`. Also set by verification probes. Most `HistoricalGameplayTweaks.xxx()` no-arg gates read it internally (lines 1021–1143). |
| **PL** | `gameplay/HistoricalProfileLookup.profileIdForLevel(ServerLevel)` (`HistoricalProfileLookup.java:46`) → generator preset alias (`generation/HistoricalFeatureExclusions.historicalAlias`, `:892`) → `HistoricalProfiles.byGenerationPreset`. Per-level, durable. |
| **CR** | `client/HistoricalClientRules.activeVersion()` (`:75`): single-player → PL on the integrated server's overworld, else the server-pushed id. Used by all `client/lighting/*`, `HistoricalTooltips`, `HistoricalColormaps`, `HistoricalTextureService`, `HistoricalCreativeInventory` helpers (confirmed by scanning each helper for `HistoricalClientRules`). |
| **FE** | `HistoricalFeatureExclusions` (`activeAlias(generator)` `:1117`, `historicalAlias` `:892`) — generator-based resolution for worldgen. |
| **SC** | `server/HistoricalServerContext` (cached converted-chunk set/target; `:76`, `:85`). |
| Hot path | **T** per-tick, **B** per-block, **R** per-render/frame, **E** per-entity/per-mob-per-tick, **C** very-frequent generic call, **X** rare. |
| Logic | approximate count of lines of real logic in the handler(s) beyond delegation. |
| Verdict | **THIN** pure integration point; **FAT** contains version decisions / gameplay logic that belongs in a subsystem class; **SUSPECT** possible redundancy, dead injection, or side/registration defect. |

## 1. Facts about the config

* `src/main/resources/historical_experience.mixins.json`: `required: true`, `package: "net.historicalexperience.mixin"`, `compatibilityLevel: JAVA_21`, `injectors.defaultRequire: 1`. **No** `server` array, **no** `priority` / `plugin` / `refmap` key (Loom adds the refmap at build). Referenced from `fabric.mod.json:38-39` as a plain string (no `environment` qualifier).
* `mixins` (common) = 89 entries, `client` = 56 entries, total 145 = number of `.java` files. **No duplicates, no missing files, no unregistered files** (computed by script; also asserted by `HistoricalLootWiring1211Test.mixinRegistrationMatchesTheSources`, which reads both arrays and `server`).
* No `@Mixin(priority=…)` anywhere in the mixin directory (grep returned nothing) → all mixins use default priority 1000; ordering within a shared method is therefore config-array order, then MixinExtras handler composition.
* Explicit `require = 0` is used by exactly 3 mixins (all Moderner Beta targets): `ModernBetaBiomeSourceHistoricalChunksMixin:59`, `ModernBetaClimateSamplerHistoricalMixin:60`, `ModernBetaDebugBiomeHistoricalMixin:47`. Everything else is `require ≥ 1` (explicit or via `defaultRequire`). A wrong target signature in any of those 142 therefore fails at class-load of the target (see PROGRESS.md §6, `RecipeToast` incident).
* No subdirectories exist under `mixin/` today.

## 2. Per-mixin table (145 rows)

Columns: **File** (all `net.historicalexperience.mixin.*`) · **Target** · **Side** (`C`=listed in `mixins`, `CL`=listed in `client`; ⚠ = client imports in a `C` file or common class in `CL`) · **Injects** (kind; method; point; line) · **Hot** · **Logic** · **Delegates to** · **Version via** · **Verdict**.

| # | File | Target | Side | Injects (line) | Hot | Logic | Delegates to | Version via | Verdict |
|---|---|---|---|---|---|---|---|---|---|
| 1 | AbstractContainerScreenHistoricalTooltipMixin | AbstractContainerScreen | CL | @ModifyExpressionValue `renderTooltip` @INVOKE `Slot.hasItem` (32) | R | 1 | `HistoricalTooltips.hidden()` | CR | THIN |
| 2 | AbstractZombieModelHistoricalArmsMixin | AbstractZombieModel | CL | @ModifyArg×2 `setupAnim` → `animateZombieArms` idx 2 (52), idx 3 (67) | R,E | 2 | `HistoricalGameplayTweaks.appliesTo("mobArms")` | AS | THIN |
| 3 | AgeableMobHistoricalSpawnMixin | AgeableMob | C | @ModifyExpressionValue `finalizeSpawn` @`isShouldSpawnBaby` (39) | E (spawn) | 1 | `appliesTo("babyAnimal")` | AS | THIN |
| 4 | AnimalHistoricalBreedingMixin | Animal | C | @Redirect `mobInteract` @`isFood` (41) | E | 2 | `appliesTo("breeding")` | AS | THIN |
| 5 | AnimationUtilsHistoricalArmBobMixin | AnimationUtils | CL | @Inject `animateZombieArms` @INVOKE `bobArms` BEFORE (48) | R,E | 2 | `MobArmHelper.applyStaticArms`, `appliesTo("mobArms")` | AS | THIN |
| 6 | AnvilMenuHistoricalEarlyMixin | AnvilMenu | C | @Inject `<init>` RETURN (36); @Inject `createResult` HEAD cancellable (41); @WrapOperation `createResult` @`Enchantment.canEnchant` (58); `@Unique` level field (32) | C (per anvil change) | ~25 | `HistoricalEarlyMenuRules.blocksResult`, `HistoricalCreativeRules.rejectsStackedBookTarget/allowsBookOnAnyItem`, `HistoricalSnapshotRuntime.activePatchId/clientPatchId` | `level instanceof ServerLevel ? PL : AS` (idiom copy-pasted, see §3.6) | FAT |
| 7 | AxeAccess | AxeItem | C | @Accessor static `STRIPPABLES` (40) | X | 0 | used by `gameplay/HistoricalToolInteractionEvents` | – | THIN |
| 8 | BiomeClimateAccess | Biome | CL | @Accessor `climateSettings` (26) | X | 0 | used only by #98 | – | THIN |
| 9 | BiomeHistoricalSkyColourMixin | Biome | CL | @ModifyReturnValue `getSkyColor` (67) | R (per sky/fog sample) | ~28 + helper | `HistoricalWorldTweaks.skyTweakActive/eraSkyColour/skyEra`, `HistoricalSkyColours.temperatureSkyColour`, Moderner Beta `SkyColorSampler`/`ClimateSamplerSky` (`:110-120`), `RenderSystem.getShaderFogColor` | CR (`:73`) | FAT |
| 10 | BlockAndTintGetterHistoricalMixin | BlockAndTintGetter (interface) | CL | @ModifyReturnValue default `canSeeSky` (40) | C (client) | 4 | `HistoricalOldLighting.activeNow/combined` | CR (inside helper) | THIN |
| 11 | BlockBehaviourHistoricalDropMixin | BlockBehaviour | C | @ModifyReturnValue `getDrops` RETURN ordinal 0 (76) and ordinal 1 (103); private `hasSilkTouch` (187), `isOre` (215) | B (per drop) | ~60 | `HistoricalGameplayTweaks.oreDropsAsBlocks/endPortalFrameExplodes/silkySpawnerDrops/snowballSurplus` | AS (inside gates) | FAT |
| 12 | BlockBehaviourHistoricalShadeBrightnessMixin | BlockBehaviour | CL | @ModifyExpressionValue `getShadeBrightness` @CONSTANT 0.2F (69); @ModifyReturnValue `getShadeBrightness` (78) | B,R (per face) | 3 | `HistoricalSmoothLighting.activeNow`, `HistoricalLightingTweaks.oldLeavesLighting` | CR | THIN |
| 13 | BlockEntityRenderDispatcherHistoricalChestMixin | BlockEntityRenderDispatcher | CL | @ModifyReturnValue `getRenderer` (46) | R (per BE per frame) | 6 | `HistoricalChestTweaks.oldChest/oldEnderChest` | CR | THIN |
| 14 | BlockHistoricalEndPortalFrameMixin | Block | C | @ModifyReturnValue `getExplosionResistance` (55) — no-arg method verified in bytecode | C (all blocks) | 5 | `endPortalFrameExplodes()` | AS | THIN |
| 15 | BlockStateBaseHistoricalEndPortalFrameLightMixin | BlockStateBase | C | @ModifyReturnValue `getLightEmission` (63) | C (light updates; every block state) | 5 | `endPortalFrameLightFifteen()` | AS | THIN |
| 16 | BlockStateModelLoaderHistoricalChestMixin | BlockStateModelLoader | CL | @ModifyArg `loadBlockStateDefinitions` @`FileToIdConverter.idToFile` (34) | X (resource reload) | 4 | `HistoricalChestTweaks.hasHistoricalBlockState` | – (name filter) | THIN |
| 17 | BrewingStandHistoricalBrewingMixin | BrewingStandBlockEntity | C | @Inject static `doBrew` HEAD cancellable (53) | T (brewing) | 1 | `functionalBrewing(AS)` | AS | THIN |
| 18 | ChestBlockHistoricalRenderShapeMixin | ChestBlock (+TrappedChestBlock subclass) | CL | @ModifyReturnValue `getRenderShape` (43) | B,R | 1 | `HistoricalChestTweaks.oldChest` | CR | THIN |
| 19 | ChunkGeneratorHistoricalOreMixin | ChunkGenerator | C | @Inject `applyBiomeDecoration` RETURN (48) | per chunk | 1 | `generation/BetaOrePlacement.placeForChunk` | FE (inside) | THIN |
| 20 | ClientLevelHistoricalAttackSoundMixin | ClientLevel | CL | @Inject `playSound(DDD…)` HEAD cancellable (55); private `isModernAttackSound` (74) | C (per sound) | ~8 | `appliesTo("sweep")` | AS | FAT |
| 21 | ClientLevelHistoricalNightSkyMixin | ClientLevel | CL | @ModifyArg `getSkyColor` @`Mth.clamp(FFF)` idx 1 (31) — exactly one `clamp(FFF)F` call verified in `ClientLevel.getSkyColor` | R | 3 | `HistoricalStars.oldStars` | CR | THIN |
| 22 | CocoaBlockHistoricalBonemealMixin | CocoaBlock | C | @ModifyArg `performBonemeal` @`setBlock` idx 1 (32) | X | 3 | `instantBonemeal()` | AS | THIN |
| 23 | ContainerHistoricalFoodStackMixin | Container (interface) | C | @Inject default `getMaxStackSize(ItemStack)` HEAD (63) | C | 3 | `HistoricalFoodStackLimit.foodDoesNotStack` | AS | THIN |
| 24 | ContainerScreenAccess | AbstractContainerScreen | CL | @Accessor `hoveredSlot`,`leftPos`,`topPos`; @Invoker `slotClicked` (35–45) | X | 0 | used only by `client/textures/TexturesProbe`, `client/version/SnapshotFeatureProbe` (probe code) | – | THIN |
| 25 | CraftingRecipeHistoricalWoodMixin | ShapedRecipe, ShapelessRecipe | C | @ModifyReturnValue `assemble` (55) | C (per craft) | ~15 | `HistoricalCraftingEra.substitutedResult`; builds substitute `ItemStack` | AS | FAT |
| 26 | CraftingScreenHistoricalRecipeBookMixin | CraftingScreen | CL | @Inject `init` TAIL (27) | X | 1 | `HistoricalRecipeBookButton.setRecipeButton` | – | THIN |
| 27 | CreateWorldHistoricalFooterMixin | CreateWorldScreen | CL | @Inject `openFresh` HEAD (35), `createFromExisting` HEAD (42), `createNewWorldDirectory` RETURN (49); @Redirect `init` @`addToFooter` (55) | X | ~10 (layout) | `CreateWorldHistoricalVersion.*`, `HistoricalSnapshotCreation.clear`, `IndevMapSelection.clear` | – | THIN |
| 28 | CreativePickerHistoricalCatalogMixin | CreativeModeInventoryScreen.ItemPickerMenu | CL | @Inject `scrollTo` HEAD (21); `@Shadow items` | R (per scroll) | 3 | `HistoricalCreativeRules.gatesCatalog`, `HistoricalCreativeGrants.allowed` | selection string from `HistoricalCreativeInventory` (CR) | THIN |
| 29 | CreativeScreenProbeAccess | CreativeModeInventoryScreen | CL | @Invoker `selectTab` (12) | X | 0 | used only by `client/version/SnapshotAnvilProbe` | – | THIN |
| 30 | CreeperHistoricalEarlyDropsMixin | Creeper | C | @ModifyReturnValue `canDropMobsSkull` (21); @Inject `dropCustomDeathLoot` HEAD (28) | E (death) | 3 | `EarlyGameplayRecipes.supports` | PL | THIN |
| 31 | CropBlockHistoricalBonemealMixin | CropBlock | C | @ModifyReturnValue `getBonemealAgeIncrease` (43) | X | 1 | `instantBonemeal()` | AS | THIN |
| 32 | DataComponentHolderHistoricalDamageMixin | DataComponentHolder (interface) | C | @ModifyReturnValue default `getOrDefault` (53) — same method as #33, #34 | **C (every component read)** | 6 | `HistoricalDamageValues.isApplicable/rewrite` | AS (inside helper) | THIN (but see §3.1) |
| 33 | DataComponentHolderHistoricalFoodMixin | DataComponentHolder | C | @ModifyReturnValue default `getOrDefault` (47) | **C** | ~25 | `HistoricalGameplayTweaks.betaGoldenApple/betaFoodHungerValues`, `HistoricalFoodHelper.*`, builds `FoodProperties` and `MobEffectInstance` inline (`:61-75`) | AS | **SUSPECT** — likely dead, see §3.3 |
| 34 | DataComponentHolderHistoricalToolDurabilityMixin | DataComponentHolder | C | @ModifyReturnValue default `getOrDefault` (53) | **C** | 4 | `HistoricalToolDurability.durabilityFor` | AS | THIN (but see §3.1) |
| 35 | DimensionSpecialEffectsHistoricalCloudHeightMixin | DimensionSpecialEffects | CL | @ModifyReturnValue `getCloudHeight` (31) | R | 1 | `HistoricalWorldTweaks.cloudHeightOverride` | CR | THIN |
| 36 | DragonHoldingPatternHistoricalMixin | DragonHoldingPatternPhase | C | @Inject `strafePlayer` HEAD cancellable (52) | T (dragon) | ~10 | `HistoricalDragonFight.originalFight/aimHeightAbove` | PL (in helper) | FAT |
| 37 | EatBlockGoalHistoricalEarlyMixin | EatBlockGoal | C | @Inject `canUse` HEAD cancellable (20) | E,T | 2 | `EarlyGameplayRecipes.supports` | PL (ServerLevel only) | THIN |
| 38 | EnchantmentMenuHistoricalEnchantingMixin | EnchantmentMenu | C | @Inject `clickMenuButton` HEAD cancellable (85); @Inject `getGoldCount` HEAD cancellable (160); private `enchantTheOldWay` (105–153); implements `EnchantmentMenuInvoker` | X (menu click) | ~50 | `HistoricalEnchanting.usesOldTableRules`, `HistoricalGameplayTweaks.enchantingDisabled`; reimplements the enchant action | AS | FAT |
| 39 | EnchantmentMenuHistoricalPoolMixin | EnchantmentMenu | C | @ModifyReturnValue `getEnchantmentList` (45) | X | ~20 | `HistoricalEnchanting.available/placeholderEnchantment*`, `placeholderEnchantmentTooltip` | AS | FAT |
| 40 | EnchantmentMenuInvoker | EnchantmentMenu | C | @Invoker `getEnchantmentList` (37) | X | 0 | used by #38 | – | THIN |
| 41 | EnchantmentScreenHistoricalMixin | **EnchantmentScreen (client class)** | **C ⚠** | @Inject `init` TAIL (61); @WrapWithCondition `render` @`renderComponentTooltip` (94) | R | ~8 | `HistoricalEnchanting.usesOldTableRules` | AS | **SUSPECT** — client target in `mixins`, see §3.4 |
| 42 | EndDragonFightHistoricalMixin | EndDragonFight | C | @Inject `spawnNewGateway` HEAD (48), `tick` HEAD (61), `tryRespawn` HEAD (78) | **T** (boss-bar caption every tick: `Component.literal` + `equals` + `HistoricalProfileLookup` each tick, `:66-75`) | ~15 | `HistoricalDragonFight.originalFight/bossBarCaption` | PL | FAT |
| 43 | EnderChestBlockHistoricalRenderShapeMixin | EnderChestBlock | CL | @ModifyReturnValue `getRenderShape` (38) | B,R | 1 | `HistoricalChestTweaks.oldEnderChest` | CR | THIN |
| 44 | EnderDragonPhaseManagerHistoricalMixin | EnderDragonPhaseManager | C | @Inject `setPhase` HEAD cancellable (44) | T (dragon) | 3 | `HistoricalDragonFight.isPerchPhase/originalFight` | PL | THIN |
| 45 | EnderDragonVerticalDriveHistoricalMixin | EnderDragon | C | @ModifyExpressionValue `aiStep` @CONSTANT 0.01 (41) — one `0.01d` constant verified | T | 4 | `HistoricalDragonFight.ORIGINAL_VERTICAL_GAIN` | PL | THIN |
| 46 | EnderEyeHistoricalPortalMixin | EnderEyeItem | C | @Inject `useOn` HEAD cancellable (52) | X | 1 | `endPortalFunctional(AS)` | AS | THIN |
| 47 | EntityHistoricalSneakingMixin | Entity | C | @Inject `setShiftKeyDown` HEAD cancellable (46) | T (players) | 1 | `sneakingAvailable(AS)` | AS | THIN |
| 48 | FeaturePlacerHistoricalContentMixin | PlacedFeature | C | @Inject `placeWithBiomeCheck` and `place` HEAD cancellable (54) — both methods exist; `applyBiomeDecoration` calls `placeWithBiomeCheck` (verified) | **per feature per chunk** | 1 | `HistoricalFeatureExclusions.shouldSkip` | FE | THIN |
| 49 | FishingHookHistoricalMixin | FishingHook | C | @ModifyExpressionValue `retrieve` @`LootTable.getRandomItems` (47); @WrapWithCondition `retrieve` @`addFreshEntity` (63) | X | ~5 (builds COD stack inline `:58-60`) | `oldFishingLoot()`, `noFishingExperience()` | AS | FAT |
| 50 | FleeSunGoalHistoricalMixin | FleeSunGoal | C | @ModifyReturnValue `canUse` (37) | E,T | 2 | `appliesTo("avoidSun")` | AS | THIN |
| 51 | FogRendererHistoricalWorldMixin | FogRenderer | CL | @ModifyExpressionValue `setupColor` @`getSunriseColor` (40); @Inject `setupFog` TAIL (46) | R | ~14 | `HistoricalFogTweaks.worldFog/oldSunriseFog/terrainEnd/skyEnd`; calls RenderSystem fog setters | CR | FAT |
| 52 | FoliageColorHistoricalColormapMixin | FoliageColor | CL | @ModifyVariable `init` HEAD argsOnly (34) | X (reload) | 3 | `HistoricalColormaps.current/noteApplied` | CR | THIN |
| 53 | FoodDataHistoricalHungerMixin | FoodData | C | @Inject `eat(IF)V` HEAD (51); @Inject `tick` HEAD (60); `@Unique` counter | **T** | ~10 | `HistoricalPlayerRules.suppressesHungerTick` | AS | **SUSPECT** — `eat(IF)` is not on the normal food path, §3.3 |
| 54 | FoodDataHistoricalRegenerationMixin | FoodData | C | @ModifyConstant `tick` int 20 (50) — one `bipush 20` verified in `tick`; @ModifyArg `tick` @`addExhaustion` ordinal 1 (64) | **T** | 2 | `betaRegenerationModel(AS)` | AS | THIN |
| 55 | FurnaceScreenHistoricalRecipeBookMixin | AbstractFurnaceScreen | CL | @Inject `init` TAIL (27) | X | 1 | `HistoricalRecipeBookButton` | – | THIN |
| 56 | GrassColorHistoricalColormapMixin | GrassColor | CL | @ModifyVariable `init` HEAD (39) | X | 3 | `HistoricalColormaps` | CR | THIN |
| 57 | GrindstoneMenuHistoricalEarlyMixin | GrindstoneMenu | C | @Inject `<init>` RETURN (24); @Inject `createResult` HEAD cancellable (29) | C | ~10 | `HistoricalEarlyMenuRules.blocksResult` | `ServerLevel ? PL : AS` | FAT |
| 58 | GuiAccess | **Gui (client class)** | **C ⚠** | @Accessor static ARMOR_*/AIR_* sprites, `displayHealth` (33–58) | R | 0 | used by `client/hud/HudHelper` | – | **SUSPECT** — client target in `mixins`; produces a WARN on a dedicated server (log `:55`), §3.4 |
| 59 | GuiHistoricalAttackIndicatorMixin | Gui | CL | @ModifyExpressionValue `renderCrosshair` w/ @Slice (51) | R | 2 | `appliesTo("cooldown")` | AS | THIN |
| 60 | GuiHistoricalExperienceLevelMixin | Gui | CL | @Inject `renderExperienceLevel` HEAD cancellable (64) | R | 1 | `HudHelper.hideExperienceLevel` | AS (inside `HudHelper`) | THIN |
| 61 | GuiHistoricalHudMixin | Gui | CL | 12 handlers (49–222): `isExperienceBarVisible`, `renderHotbarAndDecorations` ×3, `renderPlayerHealth` ×8 (@Inject/@WrapWithCondition/@Local) | R | 0 (pure delegation, 227 lines) | `HudHelper.begin/apply/pop/end/hide*` | AS (+`Minecraft.getInstance`) inside `HudHelper` | THIN |
| 62 | HumanoidArmorLayerHistoricalTintMixin | HumanoidArmorLayer | CL | @Inject `renderArmorPiece` HEAD (55); @ModifyExpressionValue `renderModel` (62); @ModifyArg `renderModel` idx 3 (75); `@Unique` entity field | R,E | 2 | `ArmorHelper.getDamagedConsumer/getDamagedPackedOverlay` | – (entity-based, none) | THIN |
| 63 | HumanoidModelHistoricalBlockingMixin | HumanoidModel | CL | @Inject `setupAnim` @INVOKE `setupAttackAnimation` (48) | R,E | 3 | `HistoricalSwordBlocking.isBlocking`, `SwordBlockingRenderer.applyThirdPerson` | AS (inside `HistoricalSwordBlocking`) | THIN |
| 64 | ImageButtonHistoricalSpritesAccess | ImageButton | CL | @Accessor `sprites` (25) | X | 0 | used by `client/HistoricalRecipeBookButton` | – | THIN |
| 65 | InventoryHistoricalFoodStackMixin | Inventory | C | @Redirect `placeItemBackInInventory(ItemStack,Z)` @`getMaxStackSize` (43) | C | 3 | `HistoricalFoodStackLimit.foodDoesNotStack` | AS | THIN |
| 66 | InventoryScreenHistoricalOffhandMixin | InventoryScreen | CL | @Inject `<init>` TAIL (77); @Inject `renderBg` TAIL (86) | R | 4 | `HistoricalInventoryScreen.hideOffHandSlot/paintOverOffHandSlot`, `offhandUnavailable` | AS | THIN |
| 67 | InventoryScreenHistoricalRecipeBookMixin | InventoryScreen | CL | @Inject `init` TAIL (27) | X | 1 | `HistoricalRecipeBookButton` | – | THIN |
| 68 | InventoryShieldSlotMixin | `InventoryMenu$1` (anon slot; exists in 1.21.1, verified: extends Slot, overrides `getNoItemIcon`) | CL (imports `Minecraft`) | @ModifyReturnValue `getNoItemIcon` (51) | R | 4 | `offhandUnavailable`, `Minecraft.gameMode.hasInfiniteItems` | AS | THIN |
| 69 | ItemHistoricalEarlyFoodMixin | ItemStack | C | @Inject `use` HEAD cancellable (30) | E (every item use) | ~8 | `EarlyGameplayRecipes.supports`, `HistoricalFoodRules.healPointsFor`; constructs `InteractionResultHolder` and calls `finishUsingItem` | `ServerLevel ? PL : AS` | FAT |
| 70 | ItemInHandRendererHistoricalBlockingMixin | ItemInHandRenderer | CL | @Inject `renderArmWithItem` ×2 around `renderItem` ordinal 1 (64, 86); @ModifyExpressionValue @`isUsingItem` ordinal 1 (105) | R | 3 | `HistoricalSwordBlocking.isBlocking`, `SwordBlockingRenderer.applyFirstPerson` | AS (inside helper) | THIN |
| 71 | ItemRendererHistoricalChestMixin | ItemRenderer | CL | @ModifyReturnValue `getModel` (47) | R (per item draw) | ~15 | `HistoricalChestTweaks.oldChest/oldEnderChest`; chooses block per item (`:59-72`) | CR | FAT |
| 72 | ItemStackHistoricalAttributeTooltipMixin | ItemStack | **C ⚠** | @Inject `addAttributeTooltips(Consumer,Player)` HEAD cancellable (54) — method is `private` in 1.21.1 (verified), reachable by Mixin | R (tooltip) | 1 | `client/tooltip/HistoricalTooltips.showsAttributeTooltip()` → `HistoricalClientRules` → `net.minecraft.client.Minecraft` | CR | **SUSPECT** — client-only class chain from a common mixin, §3.4 |
| 73 | ItemStackHistoricalBedNameMixin | ItemStack | C | @ModifyReturnValue `getHoverName` (43) | R | 4 | `HistoricalCraftingEra.showsPlainBedName` | AS | THIN |
| 74 | ItemStackHistoricalEnchantedTooltipMixin | ItemStack | C | @Inject `getTooltipLines` RETURN cancellable (68) | R | ~12; owns the placeholder text `"Enchanted!!"` (`:66`) | `placeholderEnchantmentTooltip(AS)` | AS | FAT |
| 75 | KeyboardInputHistoricalJumpMixin | KeyboardInput | CL | @Inject `tick` TAIL (55) | **T** | ~10 state machine | `HistoricalJumpInput.allowNextPress/alreadyJumped/markJumped`, `jumpRequiresFreshPress(AS)` | AS | FAT |
| 76 | LayerLightSectionStorageHistoricalMixin | LayerLightSectionStorage | CL | @ModifyReturnValue `getDataLayerData` (49); `@Shadow chunkSource` | **C (light lookups; allocates `new HistoricalDataLayer` per call, `:58`)** | 6 | `HistoricalOldLighting.activeNow`, `HistoricalDataLayer` | CR | THIN |
| 77 | LevelRendererHistoricalStarsAccess | LevelRenderer | CL | @Invoker `createStars` (24) | X | 0 | used by `HistoricalStars` | – | THIN |
| 78 | LevelRendererHistoricalStarsMixin | LevelRenderer | CL | @ModifyExpressionValue `drawStars` CONSTANT 0.15F (42) and 0.1F (47); @Inject `renderSky` @`setupNoFog` (52) | R | ~12 | `HistoricalStars.width/style/colour`, `HistoricalFogTweaks.worldFog` | CR | FAT |
| 79 | LevelStorageHistoricalUpgradeRecoveryMixin | LevelStorageSource.LevelStorageAccess | C | @Inject `getDataTag(Z)` HEAD (40); `@Shadow getLevelPath/checkLock` | X (world open) | ~15 | `UpgradeJournal.existsIn`, `FiniteEdgeRepair.pending/recover`, `GenerationUpgrade.recover/applyPendingAdvance` | none (world path) | FAT |
| 80 | LightEngineHistoricalOldLightingMixin | LightEngine | CL | @ModifyReturnValue `getLightValue(BlockPos)` (53) — exists, verified | **C** | 6 | `HistoricalOldLighting.activeNow/transform` | CR | THIN |
| 81 | LightTextureHistoricalMixin | LightTexture | CL | @Inject `tick` RETURN (70); @ModifyExpressionValue `updateLightTexture` CONSTANT 16 w/ slice (77); @Inject `updateLightTexture` @`upload` (89); @ModifyReturnValue static `getBrightness` (105) | **T,R** | ~12 (`Mth.lerp(…, pow(0.8, 15-level), 1)` curve at `:110`) | `HistoricalOldLighting.activeNow`, `HistoricalLightmap.applyGrayscale` | CR | FAT |
| 82 | LivingEntityAttackTickerAccess | LivingEntity | C | @Accessor setter `attackStrengthTicker` (31) | X | 0 | used by #99 | – | THIN |
| 83 | LivingEntityEquipmentInvoker | LivingEntity | C | @Invoker `collectEquipmentChanges` (41) | X | 0 | used by `gameplay/HistoricalEquipmentRefresh` | – | THIN |
| 84 | LivingEntityHistoricalHungerEffectMixin | LivingEntity | C | @ModifyReturnValue `canBeAffected` (49) | E | 3 | `HistoricalPlayerRules.suppressesHungerTick` | AS | THIN |
| 85 | LivingEntityHistoricalInstantAirMixin | LivingEntity | C | @ModifyReturnValue `increaseAirSupply` (54) | T (players) | 3 | `instantAir()` | AS | THIN |
| 86 | LivingEntityHistoricalInstantEatMixin | LivingEntity | C | 5 handlers: @WrapWithCondition `eat` @`playSound` (92), `eat` @`addEatEffect` (119), `updateUsingItem` @`triggerItemUseEffects` (136), `completeUsingItem` @`triggerItemUseEffects` (151); @ModifyExpressionValue `startUsingItem` @`getUseDuration` (165) | T (using item) | 5 | `HistoricalFoodHelper.isInstantaneousEdible` | AS (inside helper) | THIN (shares `getUseDuration` site with #94, §3.1) |
| 87 | LivingEntityHistoricalLootMixin | LivingEntity | C | @ModifyExpressionValue `dropFromLootTable` @`getLootTable` (55); @Inject `dropExperience` HEAD cancellable (74) | E (death) | ~8 | `HistoricalMobLootHelper.getTable`, `HistoricalLootRules.*` | PL (`:80`) + AS inside `HistoricalMobLootHelper` | THIN |
| 88 | LivingEntityHistoricalMovementMixin | LivingEntity | C | @ModifyVariable `setSprinting` HEAD argsOnly (51) | T (players, sprint calls) | ~18 | `HistoricalPlayerRules.allowsSprinting`, `appliesTo("swim")` | AS | FAT |
| 89 | LivingEntityHistoricalSwordBlockingMixin | LivingEntity | C | @ModifyExpressionValue `startUsingItem` @`getUseDuration` (67); @ModifyVariable `calculateFallDamage` ordinal 1 (87); @ModifyVariable `hurt` HEAD (99) | E (every hurt) | ~8 | `HistoricalSwordBlocking.canBlock/isBlocking/damageMultiplier`, `blockFallDamageReductionEnabled` | AS | THIN (shares `getUseDuration` site with #86, §3.1) |
| 90 | LoadingOverlayHistoricalQuietMixin | LoadingOverlay | CL | @Inject `render` HEAD cancellable (55) | R | ~20 (reimplements overlay finish `:61-75`) | `HistoricalTextureService.isQuietOverlay` | – | FAT |
| 91 | LocalPlayerHistoricalSneakingMixin | LocalPlayer | CL | @Inject `isCrouching` HEAD (48), `isShiftKeyDown` HEAD (55) — both exist on `LocalPlayer` (verified) | **T (many calls/frame)** | 2 | `sneakingAvailable(AS)` | AS | THIN |
| 92 | LootTableHistoricalChestLootMixin | LootTable | C | @Inject `getRandomItems(LootContext)` RETURN (50) | per roll | ~12 | `HistoricalChestLoot.allows/recordRemoved` | PL (`:60`) | THIN |
| 93 | LootTableHistoricalEarlyLeavesMixin | LootTable | C | @Inject `getRandomItemsRaw(LootContext,Consumer)` HEAD cancellable (26) | per roll (every loot roll runs `getParamOrNull` first) | ~10 (re-implements the leaf drop roll `:34-38`) | `EarlyGameplayRecipes.leafDrop` | PL | FAT |
| 94 | MenuHistoricalCreativeCloneMixin | AbstractContainerMenu | C | @Inject `clicked` HEAD cancellable (18) | C (every click) | ~10 | `HistoricalCreativeGrants.allowed`, `HistoricalCreativeAuthority.selection/resync` | per-player selection | FAT |
| 95 | MenuHistoricalFoodStackMixin | AbstractContainerMenu | C | @Redirect `doClick` @`getMaxStackSize` (52); `canItemQuickReplace` (69); `getQuickCraftPlaceCount` (77) | C | 3×1 | `HistoricalFoodStackLimit.foodDoesNotStack` | AS | THIN |
| 96 | MinecraftHistoricalCreativeScreenMixin | Minecraft | CL | @ModifyVariable `setScreen` HEAD argsOnly (16) | X | 4 | `HistoricalCreativeRules.usesOldMenu`, `new LegacyCreativeScreen` | CR (selection) | THIN |
| 97 | MinecraftHistoricalInstantEatMixin | Minecraft | CL | @WrapWithCondition `startUseItem` @`itemUsed` **ordinal 1** w/ slice from `isItemEnabled` (53) | T | 2 | `HistoricalFoodHelper.isInstantaneousEdible` | AS | **SUSPECT** — same call site as #100, §3.1 |
| 98 | MinecraftHistoricalSmoothLightingMixin | Minecraft | CL | @ModifyReturnValue static `useAmbientOcclusion` (66) — only caller seen in `ModelBlockRenderer.tesselateBlock` (B,R) | B,R | ~10: contains its own era decision (`FIRST_SMOOTH_LIGHTING_BUILD = beta_1_3`, chronology compare `:64,73-76`) | `HistoricalOldLighting.toggle`, `HistoricalVersions.byId` | CR | FAT |
| 99 | MinecraftHistoricalSwordBlockingMixin | Minecraft | CL | @WrapWithCondition `startUseItem` @`swing` ordinal 2 (59) and @`itemUsed` **ordinal 1** (76), same slice as #97 | T | 2 | `HistoricalSwordBlocking.isBlocking` | AS | **SUSPECT** — same call site as #97, §3.1 |
| 100 | MobHistoricalCustomLootMixin | Mob | C | @WrapWithCondition `dropCustomDeathLoot` @`spawnAtLocation` (51) | E (death) | 3 | `appliesTo("customDeathLoot")` | AS | THIN |
| 101 | MobHistoricalEquipmentMixin | Mob | C | @Inject `populateDefaultEquipmentEnchantments` HEAD (58); @WrapWithCondition `enchantSpawnedWeapon` (74), `enchantSpawnedArmor` (90) | E (spawn) | ~10 (clears armor slots `:65-70`) | `appliesTo("monsterGear"/"monsterEnchant")` | AS | FAT |
| 102 | MobHistoricalItemPickupMixin | Mob | C | @WrapWithCondition `aiStep` @`pickUpItem` (49) | **E,T** | 3 | `appliesTo("itemPickup")` | AS | THIN |
| 103 | ModernBetaBiomeSourceAccess | `ModernBetaBiomeSource` (remap=false, **common/server-side class**) | **CL ⚠** | @Accessor `biomeRegistry` (37) | C | 0 | used only by #104 | – | **SUSPECT** — §3.4 |
| 104 | ModernBetaBiomeSourceHistoricalChunksMixin | `ModernBetaBiomeSource` (remap=false) | **CL ⚠** | @Inject `getNoiseBiome` HEAD cancellable, `require=0` (59) | **C (every biome sample during generation)** | ~12 | `HistoricalServerContext.convertedChunks/convertedTarget` | SC | **SUSPECT** — worldgen/server logic registered under `client`, §3.4 |
| 105 | ModernBetaClimateSamplerHistoricalMixin | `BlockColorSampler` (client class, remap=false) | CL | @Inject `sampleClime(II)` HEAD cancellable, `require=0` (56) — target exists privately in upstream tree (`BlockColorSampler.java:97`) | B,R (per-block tint) | ~25 | `HistoricalServerContext.convertedChunks` (server-side cache used from client class), `BiomeCarryForward.generationFor`, `BiomeClimateAccess` | CR + SC | FAT |
| 106 | ModernBetaDebugBiomeHistoricalMixin | `DebugEntryExtendedBiome` (client, remap=false) | CL | @Inject static `getLine` HEAD cancellable, `require=0` (47) — target exists (`DebugEntryExtendedBiome.java:41`) | R (F3) | ~12 | `HistoricalServerContext.convertedChunks` | SC | FAT |
| 107 | ModernBetaFiniteTransitionMixin | `ChunkProviderNoise` (remap=false) | C | @ModifyReturnValue `isDensityModified` (21), `modifyEdgeDensity` (26); both exist upstream (`ChunkProviderNoise.java:692,696`) | **B (per noise cell)** | 3 | `FiniteTransitionRuntime.planFor`, `plan.blend` | WeakHashMap keyed by generator (set by PL in `FiniteTransitionRuntime.register`) | THIN |
| 108 | NaturalSpawnerHistoricalSpawnMixin | NaturalSpawner | C | @Inject `mobsAt` RETURN cancellable (65); @Redirect `spawnMobsForChunkGeneration` @`MobSpawnSettings.getMobs` (74) | **C (per spawn attempt)** | 2 | `HistoricalSpawnFilter.applyForLevel` | FE (inside helper) | THIN |
| 109 | NaturalSpawnerProbeAccessor | NaturalSpawner | C | @Invoker static `mobsAt` (63) | X | 0 | used only by `server/GenerationVerification:701,746` (probe) | – | THIN |
| 110 | NetherPortalBlockHistoricalNetherMixin | NetherPortalBlock | C | @Inject `entityInside` HEAD cancellable (47) | E,T | 1 | `netherAvailable(AS)` | AS | THIN |
| 111 | PackRepositoryHistoricalTexturesMixin | PackRepository | CL | @ModifyReturnValue `openAllSelected` (43) | X (reload) | 5 | `HistoricalTextureService.isClientRepository/buildLayer` | CR (inside service) | THIN |
| 112 | PanicGoalHistoricalMixin | PanicGoal | C | @ModifyReturnValue `shouldPanic` (44) | E,T | 2 | `appliesTo("panic")` | AS | THIN |
| 113 | ParticleEngineHistoricalSweepMixin | ParticleEngine | CL | @ModifyExpressionValue `createParticle` @`makeParticle` (51) | per particle | 6 | `appliesTo("sweep")` | AS | THIN |
| 114 | PlayerHistoricalCombatMixin | Player | C | @Inject `attack` HEAD (57); @ModifyReturnValue `getAttackStrengthScale` (68); @ModifyExpressionValue `attack` @`onClimbable` (77), @`getEntitiesOfClass` (86) — one `onClimbable` and one `getEntitiesOfClass` call verified in `Player.attack` | E (per attack) | ~8 | `appliesTo("cooldown"/"criticalHit"/"sweep")`, `HistoricalCombatDiagnostics.recordSweepCancel`, `LivingEntityAttackTickerAccess` | AS | THIN |
| 115 | PlayerHistoricalCriticalHitMixin | Player | C | @ModifyConstant `attack` float 1.5F (54) — one `1.5f` ldc verified | E | 3 | `betaCriticalHitFormula(AS)` | AS | THIN |
| 116 | PlayerHistoricalExhaustionMixin | Player | C | @ModifyConstant `attack` 0.1F (48); `jumpFromGround` 0.05F (54), 0.2F (60) — each constant occurs once (verified) | E,T | 3 | `betaExhaustionCosts`, `betaSprintJumpExhaustion` | AS | THIN |
| 117 | PlayerHistoricalFoodMixin | Player | C | @Inject `eat` HEAD (60); @ModifyReturnValue `canEat` (76) | E | ~10 (`canEat` rewrite using health) | `HistoricalFoodHelper.directHealAmount`, `HistoricalPlayerRules.suppressesHungerTick` | AS | FAT |
| 118 | PlayerHistoricalJumpExperienceMixin | Player | C | @Inject `jumpFromGround` TAIL (66) | E | 4 | `jumpExperienceBug(AS, random)` | AS | THIN |
| 119 | PlayerHistoricalOffhandMixin | Player | C | @Inject `setItemSlot` HEAD cancellable (51) | C | 2 | `offhandUnavailable(AS)` | AS | THIN |
| 120 | PortalShapeHistoricalNetherMixin | PortalShape | C | @Inject static `findEmptyPortalShape` HEAD cancellable (51) | X | 1 | `netherAvailable(AS)` | AS | THIN |
| 121 | ProjectileWeaponItemHistoricalInvincibleBowMixin | ProjectileWeaponItem | C | @WrapWithCondition `shoot` @`hurtAndBreak` (57) | X | 3 | `invincibleBow()` | AS | THIN |
| 122 | RangedBowAttackGoalHistoricalMixin | RangedBowAttackGoal | C | @Redirect `tick` @`MoveControl.strafe` (42) | E,T | 3 | `appliesTo("skeletonStrafing")` | AS | THIN |
| 123 | RecipeBookComponentHistoricalMixin | RecipeBookComponent | CL | @ModifyReturnValue `isVisible` (33) | R | 1 | `HistoricalWorldTweaks.recipeBookDisabled` | CR | THIN |
| 124 | RecipeManagerHistoricalCraftingMixin | RecipeManager | C | @Inject `getRecipeFor(type,input,level,RecipeHolder)` HEAD (70) **and** @ModifyReturnValue same method RETURN (135); @Inject `getRecipesFor` HEAD (92) **and** @ModifyReturnValue `getRecipesFor` RETURN (112) — all four overloads/descriptors exist (verified) | C (every recipe lookup) | ~60 | `EarlyGameplayRecipes.*`, `EarlyRecipeLookup.find`, `HistoricalCraftingEra.isRecipeRefused/isRefused`, `HistoricalRecipeGate.recordRefusal` | `ServerLevel ? PL : AS` repeated 4× (`:79,98,116,144`) | FAT |
| 125 | RecipeToastHistoricalMixin | **RecipeToast (client class)** | **C ⚠** | @Inject `addOrUpdate` HEAD (57), `addItem` HEAD (65) | X | 4 | `HistoricalActiveSession.current() != null` | AS | **SUSPECT** — client target in `mixins` (WARN on server, log `:57`), §3.4 |
| 126 | SaplingBlockHistoricalBonemealMixin | SaplingBlock | C | @ModifyExpressionValue `advanceTree` @`getValue` (41); @ModifyReturnValue `isBonemealSuccess` (51) | X | 2 | `instantBonemeal()` | AS | THIN |
| 127 | ServerCreativeHistoricalGrantMixin | ServerGamePacketListenerImpl | C | @Inject `handleSetCreativeModeSlot` @`ensureRunningOnSameThread` AFTER (17) | X | 1 | `HistoricalCreativeAuthority.refuseWrite` | per-player | THIN |
| 128 | ServerGamePacketListenerHistoricalOffhandMixin | ServerGamePacketListenerImpl | C | @Inject `handlePlayerAction` HEAD cancellable (47) | C (per player action packet) | 3 | `offhandUnavailable(AS)` | AS | THIN |
| 129 | ServerLevelHistoricalMobDropProbeMixin | ServerLevel | C | @Inject `addEntity` RETURN (15) | **E (every entity add; `ThreadLocal.get` each time)** | 1 | `server/EarlyMobDropCapture.recordAccepted` — a **verification-probe** (active only with `-Dhistorical_experience.generationProbe`) | – | **SUSPECT** — test instrumentation in production path, §3.5 |
| 130 | ServerLevelHistoricalOrbMixin | ServerLevel | C | @WrapOperation `addEntity` @`addNewEntity` (51) | E | 5 | `experienceOrbSpawnDisabled(level)`, `HistoricalCombatDiagnostics.recordOrbRefused` | per-level (`HistoricalGameplayTweaks.versionOf(level)`) | THIN |
| 131 | ServerLevelHistoricalWeatherMixin | ServerLevel | C | @Inject `advanceWeatherCycle` HEAD (58), `setWeatherParameters` HEAD (68); private `clearAndHold` (79) | T | ~10 | `HistoricalWeather.mustStayClear` | PL (in helper) | FAT |
| 132 | SheepAccess | Sheep | C | @Accessor static `ITEM_BY_DYE` (36) | X | 0 | used by #133, `HistoricalMobLootHelper`, `GenerationVerification` | – | THIN |
| 133 | SheepHistoricalShearMixin | **LivingEntity** (name says Sheep) | C | @Inject `hurt` HEAD (77) — class is on `LivingEntity.hurt` and does `instanceof Sheep` itself (`:80`); `Sheep`/`Animal`/`Mob` do not override `hurt` (verified) so the target is correct but the file name misleads | **E (every hurt of every LivingEntity)** | ~25: spawns wool (`:114-119`) | `appliesTo("punchShear")`, `EarlyGameplayRecipes.supports`, `SheepAccess` | `ServerLevel ? PL : AS` | FAT |
| 134 | ShovelAccess | ShovelItem | C | @Accessor static `FLATTENABLES` (34) | X | 0 | used by `HistoricalToolInteractionEvents` | – | THIN |
| 135 | SlotHistoricalFoodStackMixin | Slot | C | @Inject `getMaxStackSize()` HEAD (65), `getMaxStackSize(ItemStack)` HEAD (73); private `foodDoesNotStack` (87) | C | 3 + duplicated predicate | **its own copy** of the rule in `HistoricalFoodStackLimit.foodDoesNotStack` (identical expression, `Slot…:87-92` vs `HistoricalFoodStackLimit.java:58-63`) | AS | **SUSPECT** — duplicate decision, §3.1 |
| 136 | StemBlockHistoricalBonemealMixin | StemBlock | C | @ModifyExpressionValue `performBonemeal` @`Math.min` (33) | X | 1 | `instantBonemeal()` | AS | THIN |
| 137 | StructureHistoricalSuppressionMixin | Structure | C | @Inject `generate` HEAD cancellable (71) | per structure attempt | ~8 | `HistoricalFeatureExclusions.activeAlias`, `HistoricalStructureRules.suppresses` | FE | THIN |
| 138 | TemptGoalHistoricalMixin | TemptGoal | C | @ModifyReturnValue `shouldFollow` (44) | E,T | 2 | `appliesTo("tempting")` | AS | THIN |
| 139 | TooltipRenderUtilHistoricalMixin | TooltipRenderUtil | CL | @Inject static `renderTooltipBackground` HEAD cancellable (38) | R | 3 | `HistoricalTooltips.legacyBox/LEGACY_BACKGROUND` | CR | THIN |
| 140 | TreeDecoratorContextHistoricalSuppressionMixin | TreeDecorator.Context | C | @Inject `setBlock` HEAD cancellable (71) | **B** | 3 | `HistoricalFeatureExclusions.shouldSuppressBlock` | FE | **SUSPECT** — same predicate as #146, §3.2 |
| 141 | WorldDimensionsHistoricalStabilityMixin | WorldDimensions.Complete | C | @Inject `lifecycle` HEAD cancellable (51) | X | 4 | `HistoricalWorldStability.isHistoricalWorld` (imports Moderner Beta `ModernBetaBiomeSource`) | dimension type + biome source | THIN |
| 142 | WorldGenRegionHistoricalBlockVetoMixin | WorldGenRegion | C | @Inject `ensureCanWrite` HEAD (58) **and** @Inject `setBlock(BlockPos,BlockState,II)` HEAD (66) | **B (≈110k invocations for 25 chunks, per log `blockVetoInvocations=109596`)** | 5 | `FiniteTransitionRuntime.protectsWrite` (twice) + `HistoricalFeatureExclusions.shouldSuppressBlock` | FE / generator map | **SUSPECT** — §3.2 |
| 143 | WorldListEntryAccessor | WorldSelectionList.WorldListEntry | CL | @Accessor `summary` (41) | X | 0 | used by `client/version/ManageWorldHistoricalVersion` | – | THIN |
| 144 | WorldOpenFlowsHistoricalBiomeMixin | WorldOpenFlows | CL | @Inject `openWorld` HEAD cancellable (54) | X | ~45: world-state load, repair, conversion driver, screen chaining | `HistoricalWorldState.loadAt`, `WorldGenSettingsAdvance.repairRetainedFiniteComponents`, `HistoricalBiomeConversionDriver.*`, `BiomeConversionScreen` | stored world metadata | FAT |
| 145 | ZombieHistoricalSpawnMixin | Zombie | C | @ModifyExpressionValue static `getSpawnAsBabyOdds` @`nextFloat` (46) | E (spawn) | 3 | `appliesTo("babyZombie")` | AS | THIN |

Row-number caveat: rows are alphabetical by file but a few lines above were collected in a different order from the printed list; the *file name* column is authoritative, the `#` is only an index. Cross references such as "#98" in §2 rows refer to the file name given in the same sentence of §3.

### 2.1 Totals

| Verdict | Count |
|---|---|
| THIN | **99** |
| FAT | **32** |
| SUSPECT | **14** |
| **Total** | **145** |

FAT: Anvil·Grindstone·EnchantmentMenuEnchanting·EnchantmentMenuPool·BlockBehaviourDrop·BiomeSkyColour·CraftingRecipeWood·RecipeManagerCrafting·SheepShear·FishingHook·ItemEarlyFood·LootTableEarlyLeaves·LivingEntityMovement·PlayerFood·MobEquipment·DragonHoldingPattern·EndDragonFight·ModernBetaClimateSampler·ModernBetaDebugBiome·LevelStorageUpgradeRecovery·WorldOpenFlowsBiome·LoadingOverlayQuiet·MinecraftSmoothLighting·FogRendererWorld·LevelRendererStars·LightTexture·ItemRendererChest·KeyboardInputJump·ItemStackEnchantedTooltip·ServerLevelWeather·MenuCreativeClone·ClientLevelAttackSound.

SUSPECT: DataComponentHolderFood·FoodDataHunger·EnchantmentScreen·GuiAccess·RecipeToast·ItemStackAttributeTooltip·ModernBetaBiomeSourceAccess·ModernBetaBiomeSourceChunks·MinecraftInstantEat·MinecraftSwordBlocking·SlotFoodStack·WorldGenRegionBlockVeto·TreeDecoratorContextSuppression·ServerLevelMobDropProbe.

Accessor/invoker interfaces (15, all zero-logic): Axe, BiomeClimate, ContainerScreen, CreativeScreenProbe, EnchantmentMenuInvoker, Gui, ImageButtonSprites, LevelRendererStars, LivingEntityAttackTicker, LivingEntityEquipmentInvoker, ModernBetaBiomeSource, NaturalSpawnerProbe, Sheep, Shovel, WorldListEntry. The verdict above is THIN for 13 of them and SUSPECT for `GuiAccess` and `ModernBetaBiomeSourceAccess` (side defects, not content).

## 3. Findings

### 3.1 Same class + method targeted by several mixins (conflict / redundancy review)

No `priority` is set anywhere, so all are priority 1000. Groups:

| Target method | Mixins | Assessment |
|---|---|---|
| `DataComponentHolder.getOrDefault` (default method on an interface) | `DataComponentHolderHistoricalDamageMixin`, `…FoodMixin`, `…ToolDurabilityMixin` | Three `@ModifyReturnValue` handlers on one method. Each early-returns on a different `DataComponentType` (`ATTRIBUTE_MODIFIERS`, `FOOD`, `MAX_DAMAGE`), so they commute; order is irrelevant. **Hot-path concern**: `getOrDefault` is called for every component read of every stack (`ItemStack.getMaxDamage`, `getMaxStackSize`, attribute reads — verified in bytecode), so three handlers add three type comparisons + casts to every call. A single dispatching mixin (`DataComponentHolderHistoricalMixin`) delegating to `gameplay` would halve the injected code. |
| `LivingEntity.startUsingItem` @INVOKE `ItemStack.getUseDuration` | `LivingEntityHistoricalInstantEatMixin:165`, `LivingEntityHistoricalSwordBlockingMixin:67` | Two `@ModifyExpressionValue` on the same call. Compose in MixinExtras; their inputs are disjoint (food vs sword) so results do not collide, but the interplay is order-dependent only if an item is both. |
| `Minecraft.startUseItem` @INVOKE `ItemInHandRenderer.itemUsed` **ordinal 1, same `@Slice` from `isItemEnabled`** | `MinecraftHistoricalInstantEatMixin:53`, `MinecraftHistoricalSwordBlockingMixin:76` | Two `@WrapWithCondition` on the identical call site with identical ordinal/slice — real overlap; both must return true for the call to run. Fragile if either ordinal moves. Candidate to merge into one `MinecraftHistoricalUseItemMixin` with one predicate. |
| `LivingEntity.hurt` HEAD | `SheepHistoricalShearMixin:77` (@Inject), `LivingEntityHistoricalSwordBlockingMixin:99` (@ModifyVariable) | Different injector types, different concerns, no conflict. `SheepHistoricalShearMixin` runs `instanceof Sheep` for *every* hurt of every entity (hot). |
| `Player.attack` | `PlayerHistoricalCombatMixin` (HEAD inject, `onClimbable`, `getEntitiesOfClass`), `PlayerHistoricalCriticalHitMixin` (1.5F), `PlayerHistoricalExhaustionMixin` (0.1F) | Distinct sites; the critical-hit rules are complementary (`criticalHit` gate disables crit before Beta 1.8 via `onClimbable`; `betaCriticalHitFormula` randomises 1.0–2.0 for Beta 1.8 ≤ v < Release 1.6.4). No conflict. |
| `Player.jumpFromGround` | `PlayerHistoricalExhaustionMixin` (constants 0.05F, 0.2F), `PlayerHistoricalJumpExperienceMixin` (TAIL) | Distinct sites. |
| `FoodData.tick` | `FoodDataHistoricalHungerMixin` (HEAD), `FoodDataHistoricalRegenerationMixin` (constant 20, `addExhaustion` ordinal 1) | Distinct sites. |
| `EnchantmentMenu.getEnchantmentList` | `…PoolMixin` (return filter) + `…EnchantingMixin` calls it through `EnchantmentMenuInvoker` (so its result is also filtered) | Intentional coupling. |
| `ServerLevel.addEntity` | `ServerLevelHistoricalOrbMixin` (wrap), `ServerLevelHistoricalMobDropProbeMixin` (RETURN) | No conflict; see §3.5 for the probe. |
| `ItemStack` | `use` (EarlyFood), `getHoverName` (BedName), `getTooltipLines` (EnchantedTooltip), `addAttributeTooltips` (AttributeTooltip) | All distinct methods. |
| `AbstractContainerMenu` | `clicked` (CreativeClone) vs `doClick`/`canItemQuickReplace`/`getQuickCraftPlaceCount` (FoodStack) | Distinct. |
| `RecipeManager.getRecipeFor`/`getRecipesFor` | one class, a HEAD @Inject and a RETURN @ModifyReturnValue on each of two descriptors | Not a cross-mixin conflict, but the same version-resolution block is copied 4× (`:79,98,116,144`). |
| Food-stack rule | five mixins (`Container`, `Slot`, `Inventory`, `AbstractContainerMenu`×3 redirects) all answering the same question through `HistoricalFoodStackLimit` | One rule spread over 5 files; `SlotHistoricalFoodStackMixin` duplicates the predicate instead of calling `HistoricalFoodStackLimit` (`:87-92`). `ItemStack.getMaxStackSize` is itself `getOrDefault(MAX_STACK_SIZE)` (verified), so a single `getOrDefault` hook would be a possible consolidation — *design option, not tested*. |
| Moderner Beta overlap | `BiomeHistoricalSkyColourMixin` (`Biome.getSkyColor`) and `ClientLevelHistoricalNightSkyMixin` (`ClientLevel.getSkyColor`) | Moderner Beta also has `BiomeMixin`/`BiomeAccessor` on `Biome` and a client `ClientLevelMixin` on `getSkyColor` (upstream tree, `mixin/client/ClientLevelMixin.java:23`, and `EnvironmentAttributeSystemMixin`). Not analysed for conflict; flagged only. |

### 3.2 Block-write vetoes in worldgen (redundancy)

* `WorldGenRegion.setBlock(BlockPos,BlockState,int,int)` calls `ensureCanWrite(pos)` first (verified in bytecode, first 5 instructions). `WorldGenRegionHistoricalBlockVetoMixin` injects at HEAD of **both** `ensureCanWrite` (`:58`) and `setBlock` (`:66`), and both call `FiniteTransitionRuntime.protectsWrite`. When `protectsWrite` is true the `ensureCanWrite` inject already makes `setBlock` return false, so the `protectsWrite` call in the `setBlock` inject (`:72`) is executed twice per setBlock and the second is redundant. Cost is per-block (log: 109,596 invocations for 25 sampled chunks).
* `TreeDecoratorContextHistoricalSuppressionMixin` cancels `TreeDecorator.Context.setBlock` using the same `HistoricalFeatureExclusions.shouldSuppressBlock` predicate as the `setBlock` inject. `Context.setBlock` forwards to the `decorationSetter` BiConsumer (bytecode verified); if that setter ends in `WorldGenLevel.setBlock` (expected for `TreeFeature`, **not verified**) the region veto would already catch it. The difference may be that cancelling in the Context keeps the position out of the tree's tracked set — if that is the reason it should be documented in the file. Marked SUSPECT, not dead.
* `FeaturePlacerHistoricalContentMixin` (skip features) and `ChunkGeneratorHistoricalOreMixin` (add ores after decoration) are complementary, not redundant.

### 3.3 Registered-and-applied but likely not firing

1. **`DataComponentHolderHistoricalFoodMixin`** hooks `DataComponentHolder.getOrDefault` for `DataComponents.FOOD`. In 1.21.1 bytecode the food component is read with `ItemStack.get(FOOD)` (`Item.getFoodProperties`, `Item.use`, `LivingEntity.…eat` entry — all `invokevirtual ItemStack.get`), and `DataComponentHolder.get` is a **separate** default method from `getOrDefault` (both verified). A scan of 162 classes (`world.item.*`, `LivingEntity`, `Player`, `world.food.*`, `world.item.enchantment.*`, `world.item.component.*`) found `getOrDefault` paired with `ATTRIBUTE_MODIFIERS` and `MAX_DAMAGE` (in `ItemStack`; alive) but **never with `FOOD`**. Own code also reads food with `get`/`has` only (`PlayerHistoricalFoodMixin:67`, `HistoricalFoodHelper:40`). Conclusion: the injection applies but I found no caller; the beta golden-apple / beta hunger-value logic in it is likely dead on the vanilla path. Packages outside the scanned set (e.g. client tooltip code, block/entity classes) were *not* scanned. Needs a runtime probe before removal; a unit test for the numbers exists (`GoldenAppleBetaValuesTest`) but tests the helper, not the injection.
2. **`FoodDataHistoricalHungerMixin`** `@Inject eat(IF)V HEAD` (`:51`). Verified: `Player.eat` → `FoodData.eat(FoodProperties)` → private `add(IF)`; `eat(IF)` is *not* called on that path. The only vanilla `eat(IF)` caller found is `CakeBlock`. So this handler is cake-only; normal food healing is done by `PlayerHistoricalFoodMixin:60` (`Player.eat` HEAD). No double heal. The `tick` handler in the same class is live.
3. `require = 0` mixins (§1) cannot fail loudly; I could confirm the three targets and method names exist in the upstream source tree (`BlockColorSampler.sampleClime(II)` line 97, `DebugEntryExtendedBiome.getLine` line 41, `ModernBetaBiomeSource.getNoiseBiome` line 110, `ChunkProviderNoise.isDensityModified/modifyEdgeDensity` lines 692/696) but not in the pinned binary jar (5.0.0-alpha.4+1.21.1).
4. All other target methods whose signature I could resolve in the 1.21.1 jar exist (`PlacedFeature.place/placeWithBiomeCheck`, `Gui.renderCrosshair/renderHotbarAndDecorations/renderExperienceLevel/isExperienceBarVisible`, `LocalPlayer.isCrouching/isShiftKeyDown`, `EnderEyeItem.useOn`, `RecipeManager.getRecipeFor×4/getRecipesFor`, `Block.getExplosionResistance()`, `Mob.populateDefaultEquipmentEnchantments/enchantSpawnedWeapon/enchantSpawnedArmor`, `LightEngine.getLightValue(BlockPos)`, `Biome.getSkyColor()`, `BlockAndTintGetter.canSeeSky`, `ItemStack.addAttributeTooltips`, `InventoryMenu$1.getNoItemIcon`). I did **not** exhaustively javap every one of the ~250 handlers; the dedicated-server log shows the common set applying without failure (`mixinTargets … loaded and transformed` in `GENERATION_VERIFICATION`).

### 3.4 Client/server separation defects

**A. Client-only targets listed in the common `mixins` array** (registered on a dedicated server; target class cannot load there):

| Mixin | Target | Evidence |
|---|---|---|
| `EnchantmentScreenHistoricalMixin` (`mixins`, json line 12) | `net.minecraft.client.gui.screens.inventory.EnchantmentScreen` | Server log `run/verification/orefix-f-normal/logs/latest.log:52-53`: *"Cannot load class … in environment type SERVER … @Mixin target … was not found"*. |
| `GuiAccess` (json line 45) | `net.minecraft.client.gui.Gui` | log `:54-55`; also `run/probe/logs/latest.log:53`. |
| `RecipeToastHistoricalMixin` (json line 87) | `net.minecraft.client.gui.components.toasts.RecipeToast` | log `:56-57`. |

These are WARNs, not crashes (the server ran to `success=true`), but they are config errors with `required: true` + `defaultRequire: 1` semantics and noise on every server start. Fix: move all three to the `client` array. `GenerationVerification.java:426` mentions `GuiAccess` only in a comment; real users of `GuiAccess` are in `client/hud/HudHelper`.

**B. Client classes referenced from a common-array mixin whose target is common:** `ItemStackHistoricalAttributeTooltipMixin` (json `mixins`, line 73) imports `net.historicalexperience.client.tooltip.HistoricalTooltips` (`:20`); `HistoricalTooltips` → `HistoricalClientRules` → `net.minecraft.client.Minecraft` (`HistoricalClientRules.java:24`, `Minecraft.getInstance()` at `:76`). `ItemStack.addAttributeTooltips` is normally only run by client tooltip code, so a vanilla dedicated server never trips it, but any server-side/mod call to `getTooltipLines` would throw `NoClassDefFoundError` on `Minecraft`. Fix: move to the `client` array (target `ItemStack` is common, which is fine for a client-only mixin, as #12/#18/#43 already do) — or route through a common gate.

No other `C` mixin imports `net.minecraft.client.*`, `com.mojang.blaze3d.*` or `net.historicalexperience.client.*` (computed by scan over every mixin's imports).

**C. Server/common logic registered in the `client` array** (so it does not run on a dedicated server):
* `ModernBetaBiomeSourceHistoricalChunksMixin` (json `client`, line 104) + `ModernBetaBiomeSourceAccess` (line 108) target `mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource`, the biome source that the **server** uses for generation, and use `HistoricalServerContext` (server). Registered under `client`, they are only applied in a client JVM (single-player / integrated server). On a dedicated server the "converted chunks keep their converted biome" rule would not apply. Compare `ModernBetaFiniteTransitionMixin`, which targets a common Moderner Beta class and is correctly in `mixins`. (Stated from the registration and the class's own javadoc; the dedicated-server consequence has not been run.)
* `PackRepositoryHistoricalTexturesMixin` (client) targets the common `PackRepository`; correct because the code is client-service-only.

**D. Process-global session state (design hazard, not a registration bug):** 51 mixins resolve the version from **AS**, but AS is only `set` at player JOIN (`HistoricalVersionSync.java:84`) and by client sync. By code reading there is no assignment at `SERVER_STARTED`, so AS-gated server mixins (`MobHistoricalEquipmentMixin`, `AgeableMobHistoricalSpawnMixin`, `ZombieHistoricalSpawnMixin`, `MobHistoricalItemPickupMixin`, …) see "no historical world" for anything that happens before the first player joins (e.g. chunk-generation spawns, `spawnMobsForChunkGeneration`). Only 11 mixins use per-level **PL**. *Unverified at runtime.*

### 3.5 Non-production code inside mixins

* `ServerLevelHistoricalMobDropProbeMixin` → `EarlyMobDropCapture.recordAccepted` (`server/EarlyMobDropCapture.java`, guard `Boolean.getBoolean("historical_experience.generationProbe")`, `open()` line 16–19): a verification probe hooked into `ServerLevel.addEntity` for every entity in production. Cost is a `ThreadLocal.get()` per entity add.
* Probe-only accessors in the mixin package: `NaturalSpawnerProbeAccessor` (`GenerationVerification:701,746`), `CreativeScreenProbeAccess` (`SnapshotAnvilProbe`), `ContainerScreenAccess` (`SnapshotFeatureProbe`, `TexturesProbe:322-350`).

### 3.6 Duplication worth a shared helper

* `level instanceof ServerLevel ? HistoricalProfileLookup.profileIdForLevel(server) : HistoricalActiveSession.current()` appears in `AnvilMenuHistoricalEarlyMixin:45`, `GrindstoneMenuHistoricalEarlyMixin:33`, `ItemHistoricalEarlyFoodMixin:33`, `SheepHistoricalShearMixin:105`, and 4× in `RecipeManagerHistoricalCraftingMixin` (`:79,98,116,144`). Suggested home: `HistoricalProfileLookup.profileIdForLevelOrSession(Level)`.
* `EarlyGameplayRecipes.supports(PL)` is the gate for 9 mixins (`Creeper`, `EatBlockGoal`, `ItemEarlyFood`, `LootTableEarlyLeaves`, `RecipeManager`, `SheepShear`, `Anvil`, `Grindstone` + `HistoricalEarlyMenuRules`).

### 3.7 (2) Registered-but-missing / unregistered files

* Registered with no file: **none**. Files with no registration: **none**. Duplicates: none. (Script-verified; enforced by `HistoricalLootWiring1211Test`.)
* **Stale references to non-existent mixin files elsewhere** (not in the json): `scripts/write-nt-port-audit.py:33` (`PlayerHistoricalSprintMixin.java`) and `:46` (`ItemStackHistoricalDamageMixin.java`) — neither exists; `scripts/summarize-1211-verification.py:23-25` asserts the `mixins` array equals a 6-name set, which cannot hold with 89 entries.

## 4. Recommended grouping (side × domain)

Layout (target); counts are the number of files per folder.

```
mixin/
├─ client/                                   (client-only; json "client")
│  ├─ render/world/    LevelRendererStarsMixin, LevelRendererStarsAccess, FogRendererWorldMixin, ClientLevelNightSkyMixin,
│  │                   BiomeSkyColourMixin, DimensionSpecialEffectsCloudHeightMixin, GrassColorColormapMixin, FoliageColorColormapMixin  (8)
│  ├─ render/lighting/ BlockBehaviourShadeBrightnessMixin, LightEngineOldLightingMixin, LayerLightSectionStorageMixin,
│  │                   LightTextureMixin, BlockAndTintGetterMixin, MinecraftSmoothLightingMixin  (6)
│  ├─ render/chest/    BlockEntityRenderDispatcherChestMixin, ChestBlockRenderShapeMixin, EnderChestBlockRenderShapeMixin,
│  │                   BlockStateModelLoaderChestMixin, ItemRendererChestMixin  (5)
│  ├─ render/entity/   HumanoidModelBlockingMixin, ItemInHandRendererBlockingMixin, AbstractZombieModelArmsMixin,
│  │                   AnimationUtilsArmBobMixin, HumanoidArmorLayerTintMixin, ParticleEngineSweepMixin, ClientLevelAttackSoundMixin  (7)
│  ├─ gui/hud/         GuiHudMixin, GuiExperienceLevelMixin, GuiAttackIndicatorMixin, GuiAccess  (4)
│  ├─ gui/tooltip/     AbstractContainerScreenTooltipMixin, TooltipRenderUtilMixin, ItemStackAttributeTooltipMixin(moved from common), RecipeToastMixin(moved)  (4)
│  ├─ gui/recipebook/  RecipeBookComponentMixin, CraftingScreenRecipeBookMixin, FurnaceScreenRecipeBookMixin,
│  │                   InventoryScreenRecipeBookMixin, ImageButtonSpritesAccess  (5)
│  ├─ gui/inventory/   InventoryScreenOffhandMixin, InventoryShieldSlotMixin, ContainerScreenAccess, EnchantmentScreenMixin(moved),
│  │                   MinecraftCreativeScreenMixin, CreativePickerCatalogMixin, CreativeScreenProbeAccess  (7)
│  ├─ gui/world/       CreateWorldFooterMixin, WorldOpenFlowsBiomeMixin, WorldListEntryAccessor, LoadingOverlayQuietMixin  (4)
│  ├─ input/           KeyboardInputJumpMixin, LocalPlayerSneakingMixin, MinecraftInstantEatMixin, MinecraftSwordBlockingMixin  (4)
│  ├─ resources/       PackRepositoryTexturesMixin  (1)
│  └─ compat/modernerbeta/  ModernBetaClimateSamplerMixin, ModernBetaDebugBiomeMixin, BiomeClimateAccess  (3)
│                                                                                                   client total = 58
├─ common/                                   (both sides; json "mixins")
│  ├─ gameplay/combat/    PlayerCombatMixin, PlayerCriticalHitMixin, LivingEntitySwordBlockingMixin, LivingEntityAttackTickerAccess,
│  │                      ProjectileWeaponItemInvincibleBowMixin, DataComponentHolderDamageMixin, ServerLevelOrbMixin  (7)
│  ├─ gameplay/food/      FoodDataHungerMixin, FoodDataRegenerationMixin, PlayerFoodMixin, PlayerExhaustionMixin,
│  │                      LivingEntityHungerEffectMixin, LivingEntityInstantEatMixin, DataComponentHolderFoodMixin,
│  │                      ItemEarlyFoodMixin, {Container,Slot,Inventory,Menu}FoodStackMixin  (12)
│  ├─ gameplay/mobs/      AgeableMob, Animal, Panic, Tempt, FleeSun, EatBlock, Zombie, MobEquipment, MobItemPickup, MobCustomLoot,
│  │                      CreeperEarlyDrops, RangedBowAttackGoal, SheepShear, SheepAccess, LivingEntityLoot, LootTable{ChestLoot,EarlyLeaves}  (17)
│  ├─ gameplay/player/    EntitySneaking, PlayerOffhand, ServerGamePacketListenerOffhand, LivingEntityMovement, PlayerJumpExperience,
│  │                      LivingEntityInstantAir, MenuCreativeClone, ServerCreativeGrant, LivingEntityEquipmentInvoker  (9)
│  ├─ gameplay/crafting/  AnvilMenuEarly, GrindstoneMenuEarly, EnchantmentMenu{Enchanting,Pool,Invoker}, BrewingStandBrewing,
│  │                      RecipeManagerCrafting, CraftingRecipeWood, ItemStack{BedName,EnchantedTooltip}  (10)
│  ├─ gameplay/blocks/    BlockBehaviourDrop, BlockEndPortalFrame, BlockStateBaseEndPortalFrameLight, {Crop,Stem,Cocoa,Sapling}Bonemeal,
│  │                      FishingHook, AxeAccess, ShovelAccess  (11)
│  ├─ gameplay/dimensions/ EnderEyePortal, NetherPortalBlockNether, PortalShapeNether, ServerLevelWeather,
│  │                       EndDragonFight, EnderDragonPhaseManager, EnderDragonVerticalDrive, DragonHoldingPattern  (8)
│  ├─ worldgen/           FeaturePlacerContent, ChunkGeneratorOre, TreeDecoratorContextSuppression, StructureSuppression,
│  │                      WorldGenRegionBlockVeto, NaturalSpawnerSpawn, WorldDimensionsStability  (7)
│  ├─ world/              LevelStorageUpgradeRecovery  (1)
│  ├─ compat/modernerbeta/ ModernBetaFiniteTransition, ModernBetaBiomeSourceChunks(moved), ModernBetaBiomeSourceAccess(moved)  (3)
│  └─ probe/              NaturalSpawnerProbeAccessor, ServerLevelMobDropProbeMixin (or delete/guard per §3.5)  (2)
│                                                                                                   common total = 87
```

(58 + 87 = 145. The `client` figure rises from 56 to 58 and the `common` figure falls from 89 to 87 because net moves are: `EnchantmentScreen`, `GuiAccess`, `RecipeToast`, `ItemStackAttributeTooltip` → client (+4); `ModernBetaBiomeSource{Chunks,Access}` → common (−2 from client, +2 to common)… net client 56+4−2 = 58, common 89−4+2 = 87.)

### 4.1 Exactly what a move requires in `historical_experience.mixins.json`

The mixin config resolves each array entry as `<package>.<entry>`. The Java `package` statement of the moved file must match.

1. **`"package"` can stay `"net.historicalexperience.mixin"`.** Entries become dotted sub-package paths: `"client.render.world.FogRendererHistoricalWorldMixin"` (dots, not slashes; no `.java`). This keeps every class under the already-claimed mixin package (the transformer owns `net.historicalexperience.mixin.*` — see `MixinNestedTypeGuardTest` javadoc).
   * Alternative (not needed): change `"package"` to e.g. `net.historicalexperience.mixin.client` and split into two JSON files (`…common.mixins.json`, `…client.mixins.json`) — then `fabric.mod.json:38-39` must list both, the client one as `{ "config": "…", "environment": "client" }`. That gives genuine environment scoping instead of the `client` array, but is a larger change than the folder move.
2. **Every moved file**: change its `package net.historicalexperience.mixin;` to `package net.historicalexperience.mixin.<side>.<domain>;`.
3. **Every non-mixin class that imports an accessor/invoker interface by FQN must be edited** (these are ordinary `import` / inline FQN references): `client/HistoricalRecipeBookButton.java:18` (`ImageButtonHistoricalSpritesAccess`), `client/hud/HudHelper.java:25` (`GuiAccess`), `gameplay/HistoricalToolInteractionEvents.java:19-20` (`AxeAccess`, `ShovelAccess`), `client/lighting/HistoricalStars.java:18` (`LevelRendererHistoricalStarsAccess`), `client/version/SnapshotFeatureProbe.java:13` and `client/textures/TexturesProbe.java:322,348,350` (`ContainerScreenAccess`), `client/version/SnapshotAnvilProbe.java:14` (`CreativeScreenProbeAccess`), `client/version/ManageWorldHistoricalVersion.java:27` (`WorldListEntryAccessor`), `gameplay/HistoricalMobLootHelper.java:20` and `server/GenerationVerification.java` (`SheepAccess`; `NaturalSpawnerProbeAccessor` at `:701,:746`), `gameplay/HistoricalEquipmentRefresh.java:35` (`LivingEntityEquipmentInvoker`). Intra-mixin references (`EnchantmentMenuInvoker`, `LivingEntityAttackTickerAccess`, `BiomeClimateAccess`, `ModernBetaBiomeSourceAccess`, `SheepAccess` from `SheepHistoricalShearMixin`, `NaturalSpawnerProbeAccessor` from `NaturalSpawnerHistoricalSpawnMixin`) need an import where the two classes land in different sub-packages (same-package references need none today).
4. **Tests that depend on layout**:
   * `HistoricalLootWiring1211Test.java:53,79` — `Files.list(MIXIN_DIR)` is **non-recursive** and compares bare file names to json entries; both must change (walk recursively; compare `relativePath → dotted name`).
   * `HistoricalWeatherTest.java:112` — `Class.forName("net.historicalexperience.mixin.ServerLevelHistoricalWeatherMixin")` → new FQN.
   * `MixinNestedTypeGuardTest.java:86` uses `Files.walk` (already recursive) — no change; its javadoc quotes old names (cosmetic).
   * `HistoricalNtIsolationTest.java:42`, `HistoricalPlayerRulesWiring1211Test.java:125-128`, `VersionGuiWiringTest.java:162` use `contains(...)` on the json text — still pass with dotted prefixes.
5. **Scripts/docs with file paths** (update or leave as historical records): `scripts/audit-generation-backport.py:28-30`, `scripts/apply-licence-headers.py:117-200` (`NT_FILES` list uses `main/java/net/historicalexperience/mixin/<File>.java` — must be updated for ported files or `LicenceComplianceTest` will lose coverage), `scripts/write-nt-port-audit.py:27-46`, `scripts/summarize-1211-verification.py:23-25` (exact-name set equality), `research/implementation-inventory.md:268`, javadoc in `gameplay/HistoricalFoodStackLimit.java:33-44`. `docs/PROVENANCE.md` file-by-file records should be re-checked (not opened in this audit).
6. **Nothing else**: no `refmap` entry exists to rename; `build.gradle` has no mixin-path configuration (only `-Dhistorical_experience.clientMixinCheck`, which takes *target* class names); `fabric.mod.json` only names the json.
7. Keep the rule from `HistoricalFoodStackLimit` / `MixinNestedTypeGuardTest`: **no nested types in mixin classes** and no helper classes inside `net.historicalexperience.mixin.**`.

### 4.2 Order of work suggested by the findings (not applied)

1. Move `EnchantmentScreen`, `GuiAccess`, `RecipeToast`, `ItemStackAttributeTooltip` to `client`; decide on `ModernBetaBiomeSource{Chunks,Access}` (common) — pure json edits, no folder change needed.
2. Remove duplicate `protectsWrite` in the `setBlock` inject; replace `SlotHistoricalFoodStackMixin`'s private predicate with `HistoricalFoodStackLimit`.
3. Merge `MinecraftHistoricalInstantEat/SwordBlocking`, and consider one `DataComponentHolderHistoricalMixin`.
4. Guard or remove `ServerLevelHistoricalMobDropProbeMixin`; runtime-probe `DataComponentHolderHistoricalFoodMixin` and `FoodDataHistoricalHungerMixin.eat` before deleting.
5. Extract the 32 FAT mixins' bodies into subsystem classes (largest first: `RecipeManagerHistoricalCraftingMixin`, `EnchantmentMenuHistoricalEnchantingMixin`, `BlockBehaviourHistoricalDropMixin`, `WorldOpenFlowsHistoricalBiomeMixin`, `BiomeHistoricalSkyColourMixin`).
6. Then do the folder move per §4.1 in a single commit with the test/script edits.

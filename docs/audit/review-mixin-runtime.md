# Independent review — Fabric/Mixin runtime constraints of the planned refactor

Scope: `docs/audit/target-architecture.md` §3, §6, §7 (gate → `Features.active`, mixin sub-packages, 4 client re-classifications, 22 probe classes → `verification`).
Method: read-only. Grep/read of the repo, the Mixin *source* of the version Fabric Loader 0.16.10 bundles, and the existing dedicated-server logs. **Nothing was run** (no Gradle, no game).

Version actually in use (from `run/logs/latest.log`, 2026-10-17): `SpongePowered MIXIN Subsystem Version=0.8.7 … sponge-mixin-0.15.4+mixin.0.8.7 … Service=Knot/Fabric Env=SERVER`, MixinExtras 0.4.1.

Caveat on line numbers: the working tree is dirty and being edited while this review ran (`HistoricalActiveSession.java` is already deleted, `version/ActiveVersion.java`, `Feature*.java` already exist, many mixins already modified). Line numbers are from the moment each file was read and may drift by a few lines. Where a mixin has already been migrated to `ActiveVersion`, the cited gate text is from before that edit.

Doc sources (primary):
- [MixinConfig.java @ 0.15.4+mixin.0.8.7](https://raw.githubusercontent.com/FabricMC/Mixin/0.15.4%2Bmixin.0.8.7/src/main/java/org/spongepowered/asm/mixin/transformer/MixinConfig.java) — `package`, `mixins`/`client`/`server`, `packageMatch`, `prepareMixins`, `injectors.defaultRequire`, `plugin`.
- [MixinProcessor.java (master)](https://raw.githubusercontent.com/SpongePowered/Mixin/master/src/main/java/org/spongepowered/asm/mixin/transformer/MixinProcessor.java) — `packageOwnedByConfig` / `IllegalClassLoadError`, `getInvalidClassError`.
- [MixinInfo.java (master)](https://raw.githubusercontent.com/SpongePowered/Mixin/master/src/main/java/org/spongepowered/asm/mixin/transformer/MixinInfo.java) — `isLoadable()` (only `Accessor` sub-type), `registerInvalidClass`, `getTargetClass`/`handleTargetError`.
- [PluginHandle.java @ tag](https://raw.githubusercontent.com/FabricMC/Mixin/0.15.4%2Bmixin.0.8.7/src/main/java/org/spongepowered/asm/mixin/transformer/PluginHandle.java), [MixinCoprocessorPassthrough.java @ tag](https://raw.githubusercontent.com/FabricMC/Mixin/0.15.4%2Bmixin.0.8.7/src/main/java/org/spongepowered/asm/mixin/transformer/MixinCoprocessorPassthrough.java).
- [Fabric wiki — Registering Mixins](https://wiki.fabricmc.net/tutorial:mixin_registration): "*That package and its subpackages should be dedicated to Mixins, and nothing else.*"
- Real-world precedent of a sub-package entry: [fabric-lifecycle-events-v1.mixins.json](https://raw.githubusercontent.com/FabricMC/fabric/1.21.1/fabric-lifecycle-events-v1/src/main/resources/fabric-lifecycle-events-v1.mixins.json) (`"server": ["server.WorldChunkMixin"]`).
- MixinProcessor/MixinInfo were read from `master` (the tag URLs for those two files were not fetched); the logic involved (`packageMatch`, `isLoadable`) has been stable for years, but treat "master" as a small residual uncertainty.
- The SpongePowered wiki pages themselves rendered as GitHub chrome only; the source files above are the authoritative evidence instead.

---

## Verdict on "is moving mixins into sub-packages safe?"

**Yes, conditionally.** Mixin resolves every `mixins`/`client`/`server` entry as `package + entry` (a dotted sub-path is accepted) and treats *every* class whose name starts with `"<package>."` as owned, including sub-packages. The move is therefore mechanically safe if the MUST items in §1/§6/§7 below are done in the same commit (imports of the 11 externally-referenced accessor interfaces, tests that walk the directory non-recursively, scripts that hard-code paths, and *preserving the relative json order of overlapping mixins*).

---

## 1. Sub-package names and the "owned package" rule

### 1.1 Does Mixin accept `"client.render.FooMixin"` relative to `package`? — YES

Evidence:
- `MixinConfig.prepareMixins`: `String fqMixinClass = this.mixinPackage + mixinClass;` and `new MixinInfo(service, this, mixinClass, …)` → `MixinInfo` ctor `this.className = parent.getMixinPackage() + name;`. `onSelect()` appends the trailing `.` to the package. A name containing dots is just concatenated, and the class bytes are then loaded by that fully-qualified name. Only the *simple name* is used for log text.
- `getClasses()` also returns `package + entry` for all three arrays.
- Precedent: Fabric API ships `"server.WorldChunkMixin"` under package `net.fabricmc.fabric.mixin.event.lifecycle` (link above). The Fabric wiki states the package and its sub-packages are the mixin area.
- Server/client side split: `MixinConfig.prepare` → `case CLIENT: prepareMixins("client", …)`, `case SERVER: prepareMixins("server", …)`. Entries of the *other* side's array are never read, never loaded.

Consequence for JSON: entries become `"client.render.LightTextureHistoricalMixin"`, `"common.entity.PlayerHistoricalCombatMixin"`, etc. `"package"` stays `net.historicalexperience.mixin`. The Java `package` statement of each moved file must be `net.historicalexperience.mixin.client.render` etc. (otherwise `loadMixinClass` → `ClassNotFoundException "The specified mixin … was not found"`; with `required:true` that is a **startup crash**, not a warning).

### 1.2 The "owned package" rule applies to sub-packages — YES, with `startsWith`

`MixinConfig.packageMatch(className)` = `className.startsWith(this.mixinPackage)` (package with trailing dot). `MixinProcessor.applyMixins`: if any config matches, and the class is not a loadable accessor/passthrough, it throws `IllegalClassLoadError("<name> is in a defined mixin package <pkg>* owned by <config> and cannot be referenced directly")` — exactly the message quoted in `src/main/java/net/historicalexperience/gameplay/HistoricalFoodStackLimit.java:33-34` and `src/test/java/.../mixin/MixinNestedTypeGuardTest.java:44-47`. Because it is a prefix test, `mixin.client.*` and `mixin.common.*` are owned just like the root. Also owned: **any non-mixin helper class someone places in those packages** (so "no helpers in the mixin tree" must be kept; today there are none — all 145 files under `mixin/` carry `@Mixin`).

What is exempt (`MixinInfo.isLoadable()`): only the **`ACCESSOR` sub-type** — an interface whose methods are all `@Accessor`/`@Invoker` (`MixinInfo.SubType.Accessor.isLoadable() == true`, picked up by `MixinCoprocessorPassthrough.onPrepare`). Such an interface is loadable from ordinary code, *provided its mixin was prepared on the current side*. Everything else (standard mixin classes, "interface" mixins that have `default` methods such as `ContainerHistoricalFoodStackMixin`, `DataComponentHolder*Mixin`) is registered as an invalid class and fatal to reference. Nested types in any mixin are fatal (existing guard test). Accessor interfaces whose mixin was **not** registered (wrong side, plugin-gated off) are *also* fatal to reference ("missing from … which owns package") — see §5.

### 1.3 Every reference to a mixin class from outside `mixin/` today

Grep of `net.historicalexperience.mixin.` in `src/main` + `src/test` (non-mixin packages only). All targets are accessor/invoker-only interfaces (safe class-load shape):

| Reference | Interface | Kind | Safe after move? |
| --- | --- | --- | --- |
| `gameplay/HistoricalEquipmentRefresh.java:35` | `LivingEntityEquipmentInvoker` (instanceof) | @Invoker | yes, update FQN |
| `server/GenerationVerification.java:701,746` | `NaturalSpawnerProbeAccessor.invokeMobsAt` (static) | probe | yes, but see §5 gate coupling |
| `gameplay/HistoricalToolInteractionEvents.java:19-20` | `AxeAccess`, `ShovelAccess` | @Accessor | yes |
| `gameplay/HistoricalMobLootHelper.java:20` | `SheepAccess` | @Accessor | yes |
| `client/HistoricalRecipeBookButton.java:18` | `ImageButtonHistoricalSpritesAccess` | client | yes (client list) |
| `client/hud/HudHelper.java:25` | `GuiAccess` | client target | yes **only if `GuiAccess` goes to the client list** (it already must) |
| `client/version/ManageWorldHistoricalVersion.java:27` | `WorldListEntryAccessor` | client | yes |
| `client/lighting/HistoricalStars.java:18` | `LevelRendererHistoricalStarsAccess` | client | yes |
| `client/version/SnapshotAnvilProbe.java:14,228,232` | `CreativeScreenProbeAccess` | probe | yes; probe-only |
| `client/version/SnapshotFeatureProbe.java:13`, `client/textures/TexturesProbe.java:322,348,350` | `ContainerScreenAccess` | probe-only | yes |
| `src/test/.../HistoricalWeatherTest.java:112` | `Class.forName("net.historicalexperience.mixin.ServerLevelHistoricalWeatherMixin")` | a **standard mixin** class, loaded reflectively in a unit test (no Knot, so no `IllegalClassLoadError`) | **breaks** (ClassNotFoundException) after move — update FQN |

Also referenced *between* mixins (same package today, no import, will need imports after the move): `EnchantmentMenuHistoricalEnchantingMixin implements EnchantmentMenuInvoker` (`:76`, cast `:124`), `PlayerHistoricalCombatMixin` → `LivingEntityAttackTickerAccess` (`:63`), `ModernBetaBiomeSourceHistoricalChunksMixin` → `ModernBetaBiomeSourceAccess` (`:81`), `ModernBetaClimateSamplerHistoricalMixin` → `BiomeClimateAccess` (`:82`). Compile errors will surface these; they are not a runtime hazard (accessor interfaces are public and loadable).

### 1.4 Items for §1

- **MUST** Keep the JSON `package` exactly `net.historicalexperience.mixin`; put **no** non-mixin class (helper, enum, constant holder, plugin) under `net.historicalexperience.mixin.**` — it will throw `IllegalClassLoadError` on first reference, *after* a clean compile and a clean mixin apply (the exact failure mode already recorded in `MixinNestedTypeGuardTest`).
- **MUST** Update the 11 outside imports in the table (+ the `HistoricalWeatherTest` `Class.forName`).
- **MUST** Extend `MixinNestedTypeGuardTest` (or add a sibling) with a second structural rule: *no `.java` under `mixin/**` without `@Mixin`* and *no class outside `mixin/**` imports `net.historicalexperience.mixin.**` except the accessor/invoker interfaces named in an allow-list*. The existing test already `Files.walk`s (`:86`), so the nested-type rule survives the move.
- **SHOULD** Keep the Java package == directory == the json path 1:1 and add a test that derives the json entry from the file path (`mixin/client/render/X.java` ↔ `"client.render.X"`), replacing the flat-name comparison in `HistoricalLootWiring1211Test` (see §6).
- **NICE** Document in `ARCHITECTURE.md` the three loadability classes (accessor = loadable; everything else = never reference; registered-on-wrong-side accessor = never reference).

---

## 2. Client-targeting mixins in the common (`mixins`) list

Scan: every entry of `mixins` (89) for `net.minecraft.client.*` (import or FQN in body/annotation), `Minecraft.getInstance`, `@Environment`, and `import net.historicalexperience.client.*`/`verification.*`.

| Mixin | What is client | Verdict |
| --- | --- | --- |
| `EnchantmentScreenHistoricalMixin` | target `net.minecraft.client.gui.screens.inventory.EnchantmentScreen` (`:24,53`), `GuiGraphics`/`Font` in `@WrapWithCondition` target string (`:98-101`) | **client-only target** |
| `GuiAccess` | target `net.minecraft.client.gui.Gui` (`:17,31`) | **client-only target** |
| `RecipeToastHistoricalMixin` | target `net.minecraft.client.gui.components.toasts.RecipeToast`, `ToastComponent` (`:21-22,55`) | **client-only target** |
| `ItemStackHistoricalAttributeTooltipMixin` | target `ItemStack` is common, but handler body calls `net.historicalexperience.client.tooltip.HistoricalTooltips.showsAttributeTooltip()` (`:20,62`) → `HistoricalClientRules.activeVersion()` (`HistoricalTooltips.java:115`) → `net.minecraft.client.Minecraft` (`HistoricalClientRules.java:24,76,80`) | **client class reachable from a common handler** (the 4th) |

My independent scan found **no others** (checked imports and FQN occurrences, including `net/minecraft/client` slash form, for all 89 common mixins). That matches `docs/audit/mixins.md` §3.4 A/B exactly. Reverse direction (client-list mixins that contain server/common logic) is §2.4 below.

### 2.1 What happens on a dedicated server today (facts from log + source)

`run/logs/latest.log` (2026-10-17 23:26, `Env=SERVER`):
```
Error loading class: net/minecraft/client/gui/screens/inventory/EnchantmentScreen (… in environment type SERVER)
@Mixin target net.minecraft.client.gui.screens.inventory.EnchantmentScreen was not found historical_experience.mixins.json:EnchantmentScreenHistoricalMixin
… same pair for net.minecraft.client.gui.Gui (GuiAccess) and …toasts.RecipeToast (RecipeToastHistoricalMixin)
… "Done (2.858s)! For help, type "help""
```
Mechanism: `MixinInfo.parseTargets → getTargetClass` → `ClassInfo.forName(target)` is null → `handleTargetError(…, false)`. `handleTargetError` throws `InvalidMixinException` **only** when `Option.DEBUG_TARGETS` is on (`-Dmixin.debug.strict=true`/`mixin.debug.targets`); otherwise it logs WARN and the mixin ends up with zero targets and is not added to the config. `required:true` does **not** turn this into a crash by itself (`prepareMixins` re-throws only if the exception was thrown, i.e. only in strict mode). `injectors.defaultRequire = 1` is irrelevant here: it only counts matches of injectors **that are being applied to a class**; a mixin with no resolved target never applies.

So: three WARN lines on every dedicated start; **not a failure** unless somebody launches the server with `-Dmixin.debug.strict=true` (then it is fatal because of `required:true`). The 4th (`ItemStackHistoricalAttributeTooltipMixin`) *does* apply on the server (target exists) and is safe only as long as nothing on a dedicated server calls `ItemStack.addAttributeTooltips` (vanilla never does; any server-side mod that builds tooltips would get `NoClassDefFoundError: net/minecraft/client/Minecraft`). The `GuiAccess` WARN also shows that `HudHelper` (client) is the only user.

Moving them to the `client` array: the server never reads that array (`MixinConfig.prepare`), so all four disappear from the server — safe. On the integrated server (single-player), the JVM is a client, so the `client` list applies there; behaviour is identical to today.

### 2.2 Evidence that the project has run a dedicated server

Yes, repeatedly, **in the dev environment (Loom `runServer`, Mojang names)**:
- `run/logs/latest.log` as above (boot to `Done`, with the Moderner Beta `blocksource` registry ERROR unrelated).
- `PROGRESS.md:2786` ("every row from a real dedicated server, with controls"), `:2980` ("Three dedicated-server runtime controls"), `:3159` ("server boots with no mixin failures"), `:3833`, `:4159` (client joined a dedicated server with a Beta 1.7.3 world), `:4531`.
- `scripts/verify-lifecycle-controls.ps1:18` drives `runServer -PgenerationRunDir=run/probe -PgenerationProbe=true`.

Gaps (state them, do not overclaim): (1) none of those logs is from the current dirty tree — the newest `latest.log` is 2026-10-17, the newest mixin edits are 2026-10-19; (2) no evidence of a **remapped production jar** on a dedicated server — built `build/resources/main/historical_experience.mixins.json` has no `refmap` entry, so production remap (intermediary) behaviour of every mixin is unverified by what I can read; (3) `-Dmixin.debug.strict` has never been tried.

### 2.3 Items for §2

- **MUST** Move the four mixins to the `client` array in the same commit that moves their files to `mixin/client/**`. `ItemStackHistoricalAttributeTooltipMixin` stays targeting `ItemStack`; being in `client` simply means it is not applied on a dedicated server (intended — its only input is the client's version).
- **MUST** Preserve json *relative order* when moving (see §4.3) — these four have no overlap, so order is irrelevant for them, but the regeneration of both arrays is one edit.
- **MUST** Add a unit test that fails if any mixin in the `mixins` array (or `server` array) contains `net.minecraft.client` or `net.historicalexperience.client` in its source (strip comments), and that any entry whose path begins `client.` is in the `client` array and vice-versa. This turns today's WARN into a build failure. `HistoricalNtIsolationTest` only guards one mixin (`BlockBehaviourHistoricalShadeBrightnessMixin`).
- **SHOULD** Add a dedicated-server boot with `-Dmixin.debug.strict=true` (or at least `-Dmixin.debug.verbose=true`) to S10 and grep the log for `was not found` — after the move the expected count for this project's own config is **0**.
- **SHOULD** Make the dedicated-server boot part of the S7 gate, not only S10: S7's "mixins json valid; boot verified" must mean a *server* boot **and** a *client* boot (with `ClientMixinCheck` for the client classes), because a wrong json path is a startup crash (`required:true`).
- **NICE** Run one remapped-jar server boot (`remapJar` output) once before the refactor lands and once after, to confirm the refmap is regenerated with the new class names (refmap keys are the mixin FQNs; Loom's annotation processor regenerates them, but nothing in the repo proves it).

### 2.4 Related finding — the reverse misclassification (relevant to "reclassify" work)

`ModernBetaBiomeSourceHistoricalChunksMixin` and `ModernBetaBiomeSourceAccess` are in the **client** list but target Moderner Beta's *server-side* biome source and use `HistoricalServerContext` (`docs/audit/mixins.md` §3.4 C). On a dedicated server the "converted chunks keep their converted biome" rule is not applied. When regrouping into `mixin/client/**` vs `mixin/common/**`, **do not place these in `client/`** just because json says so today; moving them to the common list is a behaviour change that must be its own commit with a dedicated-server test (it targets a Moderner Beta class named by string with `remap = false`, `require = 0` — a server without the class would just WARN). **SHOULD** flag, not silently fix.

---

## 3. Hot paths, what they call, and thread-safety

Terms: *T* per tick per entity, *B* per block-state query, *R* per render/mesh call, *W* worldgen worker thread(s).

### 3.1 Session/Features state used by mixins

- `HistoricalActiveSession` (being replaced) / `ActiveVersion`: a single `volatile` reference (`ActiveVersion.java`: `private static volatile Active active` holding an immutable record `(id, FeatureSet)`; `FeatureSet.has` = boolean-array index). `Features.active(Feature)` = volatile read + array read. **This is safe from any thread and strictly cheaper than the current string switch** — and it does not touch `HistoricalVersions` (no class-init of the 1,900-line registry from a mixin handler). `Features.active(Feature, String)` / `(Feature, HistoricalVersion)` do a `ConcurrentHashMap.get` per call (`Features.java:setFor`) — fine for cold paths, **not** for B/R paths; hot mixins should use the no-arg form only.
- Writers: `HistoricalVersionSync.java:84` (every player JOIN, server thread), `HistoricalClientRules.java:55,62` (client thread), `SERVER_STOPPED` clear (`:96`), verification harnesses. There is **no write at server start** — a real pre-existing semantic gap: anything gated on the session that runs **before the first player joins** (e.g. `spawnMobsForChunkGeneration` on W threads: `MobHistoricalEquipmentMixin`, `AgeableMobHistoricalSpawnMixin`, `ZombieHistoricalSpawnMixin`) sees "no historical world". Same finding as `docs/audit/mixins.md` §3.4 D. The refactor must keep this behaviour (it is a refactor) but should record it as a known gap; do not "fix" it by setting the session at SERVER_STARTED without a golden-diff decision.
- `HistoricalGenerationScope.generationAlias()`: `volatile String`; set only from `HistoricalServerLifecycle` (`:193`, cleared `:163,176,472`). Safe to read from W threads (a plain publish). It is *process*-global, not per-level: two server instances in one JVM (singleplayer reload) can overlap only during stop/start; the existing clear at `:472` handles it.
- `HistoricalServerContext`: four `volatile` fields written **non-atomically** (`refresh()` `:91-114`: `cachedConvertedChunks`, `cachedTarget`, then `cachedWorld` last). A W-thread reader can see the new set with the old target for a moment; benign today (set is built once per world), but `refresh()` is called **on every `convertedChunks()`/`convertedTarget()` call** and allocates (`running.getWorldPath(ROOT)` + `Path.toString()` + `equals`) — see 3.3.

### 3.2 Hot mixins and what they currently call

(*Cost today* = what the handler does after its first early-out; **bold** = candidate for harm if a lookup gets slower.)

| Mixin → target | Frequency | Current call chain |
| --- | --- | --- |
| `BlockStateBaseHistoricalEndPortalFrameLightMixin` → `BlockStateBase.getLightEmission` | B | block identity test first, then `HistoricalGameplayTweaks.endPortalFrameLightFifteen()` (string switch on session). Ordering is already correct (documented `:52-58`). |
| `BlockHistoricalEndPortalFrameMixin` → `Block.getExplosionResistance` | per explosion ray step | block identity first, then gate. OK. |
| `BlockBehaviourHistoricalDropMixin` → `BlockBehaviour.getDrops` (2 handlers) | per drop | `oreDropsAsBlocks()` = `isDeclaredVersion(session)` → `HistoricalVersions.byId` (HashMap). |
| `BlockBehaviourHistoricalShadeBrightnessMixin` → `getShadeBrightness` (client; 2 handlers) | **R, mesher threads, per face vertex** | `HistoricalSmoothLighting.activeNow()` = `availableFor(HistoricalClientRules.activeVersion()) && toggle()` (`HistoricalSmoothLighting.java:106`) → **`Minecraft.getInstance().getSingleplayerServer()` → `server.overworld()` → `HistoricalProfileLookup.profileIdForLevel` → `HistoricalFeatureExclusions.historicalAlias(generator)` (`settings.get(PRESET)`, `DIAGNOSED_PRESETS.add`, marker test) → `HistoricalProfiles.byGenerationPreset` → `HistoricalVersions.byId` ×3 + chronology compare** (`HistoricalClientRules.java:75-87`, `HistoricalSmoothLighting.java:86-95`). Same chain for `HistoricalLightingTweaks.oldLeavesLighting()` (once per leaf block). This is already a heavy per-call path and runs off the main thread (`server.overworld()` from a mesher thread). |
| `LightTextureHistoricalMixin`, `LightEngineHistoricalOldLightingMixin`, `LayerLightSectionStorageHistoricalMixin`, `BlockAndTintGetterHistoricalMixin` (client) | R / B (light queries) | `HistoricalOldLighting.activeNow()` = `volatile boolean active` — already the right design (resolved once per client tick, `HistoricalOldLighting.java:71,136`). **Do not regress this to a per-call version lookup.** |
| `GrassColorHistoricalColormapMixin`, `FoliageColorHistoricalColormapMixin`, `BiomeHistoricalSkyColourMixin` (client) | R (per block colour query) | `HistoricalClientRules.activeVersion()` per call (`HistoricalColormaps.current`, `BiomeHistoricalSkyColourMixin:73`) — same heavy chain as the shade mixin; 31 call sites of `activeVersion()` in `client/**`. |
| `DataComponentHolderHistorical{Damage,Food,ToolDurability}Mixin` → `DataComponentHolder.getOrDefault` | **every component read of every stack** (per tick in inventories, tooltips, hotbar) | three `@ModifyReturnValue`s, each begins `if (DataComponents.X != component) return`. Cheap for non-matching components. **But** `ToolDurabilityMixin` (`:61-64`) computes `BuiltInRegistries.ITEM.getKey(item).toString()` *before* the session gate (`HistoricalToolDurability.durabilityFor` checks `appliesTo("toolDurability", session)` first thing) → one `ResourceLocation.toString()` allocation per `MAX_DAMAGE` read in **every** world including ordinary ones. `DamageMixin` runs `HistoricalDamageValues.isApplicable(stack)` before knowing the session. `FoodMixin` is per `FOOD` read (and per `docs/audit/mixins.md` §3.3 the FOOD hook may be dead). |
| `Container/Slot/Inventory/AbstractContainerMenu` food-stack family (5 files) | per slot interaction | `HistoricalFoodStackLimit.foodDoesNotStack(stack)` = gate first, then `stack.has(FOOD)` — correct order (already migrated to `Features.active(Feature.UNSTACKABLE_FOOD)` in the working tree). |
| `EntityHistoricalSneakingMixin` → `Entity.setShiftKeyDown`; `LivingEntityHistorical{Movement,HungerEffect,InstantAir,InstantEat,SwordBlocking}Mixin`; `PlayerHistorical{Combat,CriticalHit,Exhaustion,Food,JumpExperience,Offhand}Mixin`; `FoodDataHistorical{Hunger,Regeneration}Mixin`; `FleeSun/Panic/Tempt` goals; `MobHistoricalItemPickupMixin` → `Mob.aiStep` | **T** per entity/player | all `HistoricalGameplayTweaks.appliesTo("<string>", session)`: string `switch` + `isDeclaredVersion` → `HistoricalVersions.byId` (HashMap) + a `Set<String>.contains` / `olderThan` (chronology). Replacing with `Features.active(Feature.X)` is a strict speed-up. `SheepHistoricalShearMixin` (`LivingEntity.hurt` HEAD) and `MobHistoricalItemPickupMixin` do the cheap `instanceof` first — keep that order. |
| `ServerLevelHistoricalMobDropProbeMixin` → `ServerLevel.addEntity` RETURN | per entity add | `ci.getReturnValueZ()` then `EarlyMobDropCapture.recordAccepted` → `ThreadLocal.get()` (`EarlyMobDropCapture.java:23-25`) — probe code on a production hot path (see §5). |

### 3.3 Mixins that execute on worldgen worker threads (W) and their shared state

| Mixin | Thread | Reads | Thread-safety / cost |
| --- | --- | --- | --- |
| `FeaturePlacerHistoricalContentMixin` → `PlacedFeature` (per feature per chunk) | W | `HistoricalFeatureExclusions.shouldSkip` (`:~920`): `activeAlias(generator)` (generator settings; fallback `HistoricalGenerationScope` volatile), then `registryAccess().registryOrThrow(PLACED_FEATURE)` + `getResourceKey(feature)` (map lookup) + **a loop over every exclusion of the revision doing `ResourceLocation.tryParse` + `getHolder`** | thread-safe (immutable tables, `LongAdder`, `ConcurrentHashMap` sets). **Cost is O(#exclusions) per placed feature; the exclusion parse/lookup is un-cached** (documented as intentional "per call" in the class comment). |
| `WorldGenRegionHistoricalBlockVetoMixin` → `ensureCanWrite` **and** `setBlock` | W, **per block write** (109,596 invocations for 25 chunks per `docs/audit/mixins.md` §3.2) | `FiniteTransitionRuntime.protectsWrite` twice per setBlock (`planFor` = `Collections.synchronizedMap(new WeakHashMap)` → **a monitor acquired by every worker thread on every block write**, `FiniteTransitionRuntime.java:18-27`), then `HistoricalFeatureExclusions.shouldSuppressBlock` (`:~1046`): `BuiltInRegistries.BLOCK.getKey(...).toString()` **before** any gate, `SEEN_BLOCK_IDS.size()`, `LongAdder.increment`, then generator lookup → `historicalAlias` (`settings.get`, `DIAGNOSED_PRESETS.add` CHM write each call) → `HistoricalVersions.byRevisionAlias` → `HistoricalBlockContent.judge`. An **ordinary vanilla** world already pays the `toString()` + `getKey` + `getChunkSource().getGenerator()` per block write. |
| `TreeDecoratorContextHistoricalSuppressionMixin` → `TreeDecorator.Context.setBlock` | W | same `shouldSuppressBlock` | see above |
| `StructureHistoricalSuppressionMixin` → `Structure.generate` | W | `getResourceKey` on the dynamic registry + alias | cheap enough (per structure attempt) |
| `ChunkGeneratorHistoricalOreMixin` → `applyBiomeDecoration` RETURN | W | `BetaOrePlacement.placeForChunk` | per chunk |
| `ModernBetaFiniteTransitionMixin` → `ChunkProviderNoise.isDensityModified` / `modifyEdgeDensity` | W, **per noise cell** | `FiniteTransitionRuntime.planFor` = the same synchronized `WeakHashMap` | **contended monitor on the density hot loop** |
| `ModernBetaBiomeSourceHistoricalChunksMixin` → `getNoiseBiome` | W, **per quart position** | `HistoricalServerContext.convertedChunks()` → `refresh()` allocation each call | (client-listed today, so only runs on integrated server) |
| `NaturalSpawnerHistoricalSpawnMixin` | W / server thread | `HistoricalSpawnFilter.applyForLevel` → generator lookup | per spawn attempt |
| `MobHistoricalEquipmentMixin`, `AgeableMobHistoricalSpawnMixin`, `ZombieHistoricalSpawnMixin` | W (chunk-gen mob spawns) + server thread | session | race-free, but see the "no write before first join" gap in 3.1 |

### 3.4 Where a slower lookup would hurt, and where the refactor helps

- **Would hurt:** `Features.active(Feature, String)`/`(Feature, HistoricalVersion)` (CHM get) if copied into B/R/W paths; any resolver that re-resolves the version through `HistoricalClientRules.activeVersion()`/`profileIdForLevel` per call (that *is* the current cost of the shade/colormap/sky mixins); anything that adds a lock/CHM write per `setBlock`. `ActiveVersion.set` allocates a fresh `Active` per player join — fine.
- **Gets better for free:** all T-class `appliesTo("string", session)` sites; the shade/colormap/sky/leaf client mixins *if* they are rewritten to `Features.active(...)` instead of `activeVersion()` (§3.5 caveat).
- **Must NOT be "converted":** the worldgen mixins (§3.3) resolve the version from the **generator** (`historicalAlias(generator)`) deliberately — "generator-first alias lookup" is listed in `target-architecture.md` §1 as *already good and to be preserved*. Rewriting them to `Features.active(Feature.X)` (session-based) would re-introduce the "no historical world before first join" gap on exactly the threads where it matters (chunk generation precedes JOIN) and would make a Nether/End vs overworld decision wrong.

### 3.5 Items for §3

- **MUST** Do not migrate `FeaturePlacerHistoricalContentMixin`, `StructureHistoricalSuppressionMixin`, `TreeDecoratorContextHistoricalSuppressionMixin`, `WorldGenRegionHistoricalBlockVetoMixin`, `ChunkGeneratorHistoricalOreMixin`, `ModernBetaFiniteTransitionMixin`, `ModernBetaBiomeSourceHistoricalChunksMixin`, `NaturalSpawnerHistoricalSpawnMixin` to `Features.active` on the session. They are generator-keyed; keep their resolution source.
- **MUST** Hot-path mixins (T/B/R/W) call only the no-arg `Features.active(Feature)`; keep the existing cheap-test-first order in `BlockStateBase…LightMixin`, `SheepHistoricalShearMixin`, `MobHistoricalItemPickupMixin`.
- **MUST** When rewriting `Features.active` over `HistoricalClientRules.activeVersion()` in client mixins (shade, smooth lighting, leaves, colormaps, sky, fog, stars…), the semantic is **not identical**: `activeVersion()` resolves live from the singleplayer server's level or the synced id; `ActiveVersion` is a snapshot set at JOIN/sync. Guarantee `ActiveVersion.set/clear` is called on every transition the client already handles (join, leave, disconnect, server stop, world switch) and add a test per transition; otherwise a stale feature set leaks into the next world.
- **SHOULD** Reorder the two cheap fixes noticed while reading (no behaviour change): (a) `DataComponentHolderHistoricalToolDurabilityMixin` — test the session/feature **before** building `itemId`; (b) `HistoricalFeatureExclusions.shouldSuppressBlock` — resolve `activeAlias(generator)` (return false fast for vanilla generators) **before** `getKey(...).toString()`/`SEEN_BLOCK_IDS`/`VETO_INVOCATIONS`. Land them as separate, golden-neutral commits, not mixed into the gate migration.
- **SHOULD** Replace `FiniteTransitionRuntime.PLANS` (`synchronizedMap(WeakHashMap)`) with a lock-free structure (e.g. keep the `Plan` on a per-generator volatile field, or a `ConcurrentHashMap` keyed by identity with weak semantics) — separate commit, with a worldgen determinism test; it is on the noise inner loop.
- **SHOULD** Make `HistoricalServerContext.refresh()` publish one immutable snapshot object (world + set + target) instead of three volatile writes, and avoid `getWorldPath().toString()` per call.
- **NICE** Add a JMH-less micro-guard (a unit test that counts allocations is overkill); instead add a code-review rule "no `String` creation / `ResourceLocation.toString()` before the gate in any B/R/W mixin".

---

## 4. Overlap / duplicate injections, priority and ordering

Cross-checked against `docs/audit/mixins.md` §3.1–3.2 (exists, 351 lines); I re-derived the method map from source rather than trusting it, and everything there held. Additions are marked **new**.

### 4.1 Priority

`grep` for `priority` across `mixin/**` and the json: **no `@Mixin(priority=…)`, no `mixinPriority`, no config `priority`** → every mixin is priority 1000. Ordering among equal-priority mixins is `MixinInfo.order` — a global counter incremented in **json iteration order** (`MixinInfo.mixinOrder++`, `compareTo`) — i.e. *the order of entries in the `mixins` array, then `client`*, and Fabric's config order across mods (the json `priority` default 1000 also). **Consequence: re-sorting/regenerating the json arrays (e.g. alphabetically by new sub-package) silently changes injection/handler ordering for overlapping mixins.**

### 4.2 Overlap groups (same target class + method)

| Target | Mixins | Notes |
| --- | --- | --- |
| `Minecraft.startUseItem` @INVOKE `ItemInHandRenderer.itemUsed`, same `@Slice` from `isItemEnabled` | `MinecraftHistoricalInstantEatMixin:53-64`, `MinecraftHistoricalSwordBlockingMixin:76-87` | two `@WrapWithCondition` on one instruction. MixinExtras composes them (both must return true). Order only matters for side-effect ordering in the conditions (none). Both client-list. |
| `LivingEntity.startUsingItem` @INVOKE `ItemStack.getUseDuration` | `LivingEntityHistoricalSwordBlockingMixin:70-73`, `LivingEntityHistoricalInstantEatMixin:165-169` (**new line refs**) | two `@ModifyExpressionValue`; chain order = registration order; inputs disjoint (sword vs food) but the first transformer's result is the second's input. |
| `LivingEntity.hurt` HEAD | `SheepHistoricalShearMixin:77` (@Inject), `LivingEntityHistoricalSwordBlockingMixin:101` (@ModifyVariable argsOnly) | independent. |
| `DataComponentHolder.getOrDefault` RETURN | Damage / Food / ToolDurability | commute (disjoint component types). Mixin interface with `default` method → **not loadable**, never reference. |
| `ServerLevel.addEntity` | `ServerLevelHistoricalOrbMixin` (wrap invoke), `ServerLevelHistoricalMobDropProbeMixin` (@Inject RETURN, `require=1`) | independent. |
| `Player.attack` | Combat (HEAD, 2 expressions), CriticalHit (const 1.5F), Exhaustion (const 0.1F) | distinct sites. |
| `BlockBehaviour.getDrops` | two `@ModifyReturnValue` ordinal 0 / ordinal 1 inside one mixin | documented intentional (class comment `:50-58`); ordinals depend on RETURN count. |
| `WorldGenRegion.setBlock` | `ensureCanWrite` HEAD inject **and** `setBlock` HEAD inject, both call `protectsWrite` | redundant (second `protectsWrite` is dead when the first already cancelled) — `docs/audit/mixins.md` §3.2. |
| `TreeDecorator.Context.setBlock` + `WorldGenRegion.setBlock` | same `shouldSuppressBlock` predicate | see mixins.md §3.2; behaviour difference (tracked set) undocumented. |
| `Gui.renderPlayerHealth` | **one** class (`GuiHistoricalHudMixin`) has ~9 injectors incl. `CONSTANT stringValue=armor/health/food` and `At.Shift` | dependent on vanilla's profiler push strings; all in one class so order is class-internal. |
| food-stack | 5 files all answering one question through `HistoricalFoodStackLimit` | one rule, five hook sites — correct today. |
| `Biome.getSkyColor` / `ClientLevel.getSkyColor` | `BiomeHistoricalSkyColourMixin` (client) and Moderner Beta's own mixins on the same targets | **foreign-mod overlap, never analysed** (flagged in mixins.md). Ordering vs Moderner Beta depends on config order across mods — **do not touch** `priority` here as part of a refactor. |

Redundant injections: only the `protectsWrite` double call and the probable-dead `DataComponentHolderHistoricalFoodMixin` / `FoodDataHistoricalHungerMixin.eat(IF)` documented in mixins.md §3.3. I found **no** redundancy *introduced* by the sub-package plan.

### 4.3 Items for §4

- **MUST** When regenerating the json arrays, preserve the **relative order** of the mixins in the two composed groups (`MinecraftHistoricalInstantEatMixin` before `MinecraftHistoricalSwordBlockingMixin` today? — keep whatever the current array order is; `LivingEntityHistoricalSwordBlockingMixin` currently precedes `LivingEntityHistoricalInstantEatMixin` in the `mixins` array) and across the `mixins`→`client` boundary. Best done mechanically: move entries to new paths **in place** (text substitution), not by regrouping the arrays by domain.
- **MUST** Add a test that asserts the relative order of the overlap groups in the json (a tiny `List.indexOf` ordering assertion for each pair above) — otherwise the ordering contract exists only by accident.
- **SHOULD** If a stable order is wanted irrespective of the json, give the overlapping pairs explicit `@Mixin(priority=…)` values in a **separate commit** — priority is part of the identity MixinInfo compares and changing it also reorders against *other mods'* mixins on the same target (`Biome`, `ClientLevel`); use only on pairs inside this project.
- **SHOULD** Remove the redundant `protectsWrite` in the `setBlock` handler (keeps worker-thread cost down) — behaviour-neutral, separate commit.
- **NICE** Merge the two `Minecraft.startUseItem` wraps into one (fragile identical slice/ordinal) after the move; not part of the refactor.

---

## 5. Probe-related mixins

| Item | What it is | Cost in production today | Who uses it |
| --- | --- | --- | --- |
| `ServerLevelHistoricalMobDropProbeMixin` | `@Inject ServerLevel.addEntity` RETURN, `require=1`; calls `EarlyMobDropCapture.recordAccepted` (ThreadLocal.get) | one injected call + `ThreadLocal.get()` per entity add in every world | `server/EarlyMobDeathVerification:150` via `EarlyMobDropCapture.open` guarded by `Boolean.getBoolean("historical_experience.generationProbe")` (`EarlyMobDropCapture.java:20`) |
| `NaturalSpawnerProbeAccessor` | static `@Invoker mobsAt` (accessor, **loadable**) | none at runtime (adds a synthetic static method to `NaturalSpawner`) | `GenerationVerification.java:701,746` (probe) |
| `CreativeScreenProbeAccess` | `@Invoker selectTab` on a client class (accessor, loadable, client list) | none | `SnapshotAnvilProbe.java:14,228,232` |
| `ContainerScreenAccess` | accessor/invoker for `AbstractContainerScreen` (client list) | none | `SnapshotFeatureProbe.java:13`, `TexturesProbe.java:322,348,350` |
| `ClientMixinCheck` (`client/ClientMixinCheck.java`) | **not a mixin**; a tick listener that `Class.forName`s the class named by `-Dhistorical_experience.clientMixinCheck` so a broken client mixin fails at load | inert without the property | `HistoricalExperienceClient.java:69` `registerIfRequested()`; `build.gradle:257-258` |

### 5.1 Can they be applied conditionally? Is `IMixinConfigPlugin` reasonable?

**Yes, technically.** `MixinConfig` accepts a `"plugin": "<FQCN>"` (`@SerializedName("plugin")`); `PluginHandle` instantiates it before `prepare()`, and `MixinInfo.readDeclaredTargets → shouldApplyMixin(ignorePlugin, targetName)` calls `plugin.shouldApplyMixin(targetClassName, mixinClassName)` per mixin/target at prepare time. Returning `false` means the mixin gets **no declared targets** and is never applied.
- Cost: one small class, one call per (mixin, target) pair (~150 calls at boot), no steady-state cost. If the plugin class fails to load, `PluginHandle` logs an error and sets `plugin = null` → **fail-open** (everything applies) — not a crash.
- Risks:
  1. The plugin runs very early (before game classes initialise): it must import no Minecraft/Fabric-API/mod classes and read only `System.getProperty`.
  2. It **must not live in `net.historicalexperience.mixin.**`** (owned package; loading it through the transformer path is the `IllegalClassLoadError` trap — it currently happens to work only because the config is not yet in the processor's `configs` list when the plugin is instantiated; do not rely on that). Put it in a neutral package (e.g. `net.historicalexperience.bootstrap`).
  3. **Gate coupling:** if a probe mixin is gated off and probe code still references its accessor interface, the reference throws `IllegalClassLoadError` ("missing from … which owns package"). The gate property must be exactly the same condition that arms the probe (one umbrella system property, e.g. `historical_experience.verification=true`, set by every `runServer`/`runClient` probe task in `build.gradle:240-290`, or a list of the existing per-probe properties). A forgotten `vmArg` in a Gradle task = deterministic failure in that probe run (loud, but easy to hit).
  3b. Another config approach with no plugin: register a *second* json (`historical_experience.verification.mixins.json`) at runtime from a `preLaunch` entrypoint when the flag is present (`Mixins.addConfiguration`). Equivalent effect, more moving parts than a plugin; I would not choose it.
  4. The plugin is unit-testable only as a pure `shouldApplyMixin(String,String)` function (no Mixin runtime needed) — keep it a pure function over a property map.
  5. `MixinCoprocessor` accessors: gating *only* the injecting mixin is enough; gating accessor-only interfaces buys nothing (zero runtime cost) and adds the coupling risk above.

### 5.2 Recommendation

| Item | Recommendation |
| --- | --- |
| `ServerLevelHistoricalMobDropProbeMixin` | **GATE** via a config plugin (the only probe with production-path cost: an injection on `ServerLevel.addEntity` plus a ThreadLocal per entity). Move the file to `mixin/common/probe/` (stays under the owned package; must stay there — cannot go to `verification`, a mixin class outside the owned package is not a mixin that the config can name). Move `EarlyMobDropCapture` to `verification` (a normal class; it is *called* by the mixin, which is fine — mixin → verification references are ordinary). |
| `NaturalSpawnerProbeAccessor`, `CreativeScreenProbeAccess`, `ContainerScreenAccess` | **KEEP** (no gating). Zero cost, loadable accessor interfaces, referenced by probes that will live in `verification`; place under `mixin/common/probe/` (resp. `mixin/client/probe/`). The probes in `verification.*` import them — allowed because they are accessor interfaces (add the three to the allow-list from §1.4). `ContainerScreenAccess` is also named "…Access" with no "Probe": rename not required. |
| `ClientMixinCheck` | **MOVE** to `verification.client` (it is a plain class; not owned by the mixin package). Keep the `HistoricalExperienceClient.registerIfRequested()` call: that entrypoint is on the allowed-importer list (§7 dependency rule). Keep it dependency-free of `verification` helpers it does not need (it only needs `Minecraft`, Fabric tick event). Its `Class.forName` of a mixin *target* class is exactly the way client mixins are verified (see its javadoc); it stays valid after sub-package moves (targets are Minecraft classes). Update the doc string in `PROGRESS.md`-style instructions only if the FQCN is documented outside historical entries. |

### 5.3 Items for §5

- **MUST** If gating is adopted: gate **key == arm key** (one shared constant, referenced by both the plugin and the probe bootstrap) and a test that every probe class which references a probe accessor is only reachable when that key is set.
- **MUST NOT** put the plugin (or any probe helper) under `net.historicalexperience.mixin.**`.
- **SHOULD** Prefer GATE for `ServerLevelHistoricalMobDropProbeMixin` only; leave the three accessors.
- **SHOULD** Add `-Dhistorical_experience.verification=true` (or equivalent) to every probe task in `build.gradle` in the same commit.
- **NICE** If the owner prefers zero new infrastructure, the fallback is "KEEP + rename + document" — the per-entity cost is one `ThreadLocal.get()`; I would still not call that acceptable permanently because it is verification code on the `ServerLevel.addEntity` path.

---

## 6. Accesswidener and mixin config vs. the planned renames

`src/main/resources/historical_experience.accesswidener` (`accessWidener v2 named`):
```
accessible class  net/minecraft/world/level/biome/Biome$ClimateSettings
accessible method net/minecraft/world/level/biome/Biome$ClimateSettings downfall ()F
accessible method net/minecraft/world/level/biome/Biome$ClimateSettings temperature ()F
mutable field     net/minecraft/world/inventory/Slot x I
mutable field     net/minecraft/world/inventory/Slot y I
```
- Every entry names a **Minecraft** class/member; none names a project class. **No AW entry is affected by renaming/moving mixins or any `net.historicalexperience.*` package.** `fabric.mod.json` references it by resource name (`"accessWidener": "historical_experience.accesswidener"`) and `build.gradle:221` by path; neither changes.
- **Do check** that no code that depends on the AW (`BiomeClimateAccess`, `EnchantmentScreenHistoricalMixin` writes `Slot.x/y`, `HistoricalInventoryScreen`) is moved to a place that no longer compiles against the widened class — it compiles anywhere in the same source set.
- `fabric.mod.json` entrypoints: `net.historicalexperience.HistoricalExperienceMod`, `net.historicalexperience.client.HistoricalExperienceClient`, `net.historicalexperience.client.config.HistoricalModMenu` — **not** mixin classes; not affected, but note `verification/` is not in the entrypoint list: the "nothing imports `verification` except the two entrypoints" rule means `HistoricalExperienceMod` (server `registerIfRequested()` calls at `:45,52-54`) and `HistoricalExperienceClient:69`.

Things the renames **do** break (all outside the accesswidener):

| Break | Where | Failure mode |
| --- | --- | --- |
| JSON `mixins`/`client` entries ≠ file paths | `historical_experience.mixins.json` | **startup crash** (`required:true`, "mixin … was not found") |
| `HistoricalLootWiring1211Test` compares *flat* simple names to `Files.list(MIXIN_DIR)` (non-recursive) (`:53,79-94`) | test | fails loudly (every mixin "missing"/"unregistered") — rewrite to `Files.walk` + path→entry mapping |
| `HistoricalClientSideGateTest.noMixinAsksAVersionGateAboutALevel` uses `Files.list(mixins)` + `.java` filter (`:75-88`) | test | **passes vacuously** after the move (lists only directories) — it silently stops guarding; change to `Files.walk` |
| `HistoricalWeatherTest:112` `Class.forName("net.historicalexperience.mixin.ServerLevelHistoricalWeatherMixin")` | test | ClassNotFoundException |
| `MixinNestedTypeGuardTest` | already `Files.walk` | ok (still guards sub-packages) |
| `VersionGuiWiringTest:159-168`, `HistoricalNtIsolationTest:39-43`, `HistoricalPlayerRulesWiring1211Test:119-128` | substring checks on json text | still pass (substring), but weaker: "client list contains X" now matches `client.render.X` anywhere in the file — consider parsing JSON properly |
| `scripts/apply-licence-headers.py` `NT_FILES` (≈77 entries `main/java/net/historicalexperience/mixin/X.java`, `:97-200`) and its `names no such source file` check (`:254-258`) | script | **exit error** until updated; `LicenceComplianceTest` counts lines in `NT_FILES` so it still passes — the two disagree silently until the script is re-run |
| `scripts/audit-generation-backport.py:28-30`, `scripts/write-nt-port-audit.py:27-46` (already references non-existent `PlayerHistoricalSprintMixin`, `ItemStackHistoricalDamageMixin`), `scripts/summarize-1211-verification.py:21-24` (asserts the json `mixins` set equals 6 old names — already stale) | scripts | already-stale tooling; fix or retire |
| `PROGRESS.md` has 32 `historicalexperience/mixin/` links | docs | historical record — **leave as is** (AGENTS.md rule) |
| `docs/PROVENANCE.md` / `THIRD_PARTY_NOTICES.md` | no mixin paths (0 matches) | fine; counts (`**N Java source files**`) are tied to `NT_FILES` count — unchanged by a pure move |
| Moving files with `git mv` | n/a | preserve history/headers; the licence header test checks file *content*, not path |

Items for §6:
- **MUST** Update `NT_FILES` paths in the same commit as the file moves (and re-run the script / `LicenceComplianceTest`), because the header tool otherwise aborts.
- **MUST** Fix `HistoricalClientSideGateTest` (`Files.list` → `Files.walk`) and `HistoricalLootWiring1211Test` before or with the move; run both once *before* the move to prove they are green, then after to prove the "vacuous pass" is gone (add a sanity assertion `count > 100`).
- **SHOULD** Parse `historical_experience.mixins.json` with Gson in the substring tests (3 files) so "is registered under `client`" is exact.
- **NICE** Fix/retire the already-stale scripts listed above.

---

## 7. Consolidated MUST / SHOULD / NICE

### MUST
1. **Sub-package entries are legal** (`"client.render.Foo"`); keep `"package": "net.historicalexperience.mixin"`; Java package statement == directory == json path. Entire `mixin.**` tree is owned: **no non-mixin class there**, no nested types, no direct references except the accessor-only interfaces (allow-list + structural test). (§1)
2. Update the **11 outside references** and the `HistoricalWeatherTest.Class.forName`; add the allow-list test. (§1.3)
3. **Reclassify the four** (`EnchantmentScreenHistoricalMixin`, `GuiAccess`, `RecipeToastHistoricalMixin`, `ItemStackHistoricalAttributeTooltipMixin`) to `client`; add the "no client class in common/server mixin" source-scan test. (§2)
4. **Preserve relative json order** of overlapping mixins; do the move as in-place path substitution, not regrouping; add an order assertion for the two composed pairs. (§4)
5. **Do not migrate generator-keyed worldgen mixins to session-based `Features.active`**. Hot mixins use only the no-arg `Features.active(Feature)`. (§3)
6. Fix the **path-dependent tests/scripts** (`Files.list` vacuous pass; `Files.list` flat-name comparison; `NT_FILES`). (§6)
7. S7 gate = a **dedicated-server boot and a client boot** on the new json (a wrong path is a startup crash). Capture the server log and require `0` occurrences of `was not found`/`Mixin apply … failed` for this project's config. (§2.3)
8. If a probe plugin is added: gate key == probe arm key; plugin outside the mixin package, no Minecraft imports. (§5)
9. Re-verify `ActiveVersion.set/clear` on every client/server transition when client mixins switch from `HistoricalClientRules.activeVersion()` (live) to `Features.active` (snapshot). (§3.5)

### SHOULD
1. Gate only `ServerLevelHistoricalMobDropProbeMixin`; keep the three probe accessors; move `EarlyMobDropCapture` + probes to `verification`. (§5.2)
2. Strict-mode boot (`-Dmixin.debug.strict=true`) in S10 once. (§2.3)
3. Land as *separate, golden-neutral* commits: `ToolDurability` gate-before-allocation; `shouldSuppressBlock` fast-exit for vanilla generators; lock-free `FiniteTransitionRuntime.planFor`; single-snapshot `HistoricalServerContext`; drop the duplicate `protectsWrite`. (§3.5, §4.3)
4. Do **not** silently relocate `ModernBetaBiomeSource*` mixins to `common` as part of regrouping; decide separately (dedicated-server behaviour change). (§2.4)
5. Parse JSON in the three substring tests. (§6)
6. Record the "no session before first join" gap in `PROGRESS.md` as an open question rather than fixing it inside the refactor. (§3.1)

### NICE
1. Explicit `@Mixin(priority)` only for intra-project overlaps, separate commit. (§4.3)
2. Merge the two `Minecraft.startUseItem` wraps. (§4.3)
3. One remapped-jar dedicated-server run before/after (refmap). (§2.3)
4. Retire stale `scripts/*audit*.py`. (§6)

---

## 8. What I did not verify (honesty list)

- No Gradle/game run; every runtime statement about Mixin is from reading the Mixin sources and the existing `run/logs/latest.log`, not from executing the post-move build.
- `MixinProcessor`/`MixinInfo` were read from `master`, not the 0.8.7 tag.
- I did not inspect Moderner Beta's own mixin configs (priority/overlap on `Biome`/`ClientLevel`).
- I did not verify the production (remapped/intermediary) behaviour; the built json has no `refmap` entry in `build/resources/main`.
- Timing/cost claims in §3 are from code reading (allocation/lock structure), not profiling; the 109,596-invocations figure is quoted from `docs/audit/mixins.md` §3.2.
- The working tree changes under my feet (the parent agent is mid-refactor); numbers like "89/56 entries" were counted from the json at read time.

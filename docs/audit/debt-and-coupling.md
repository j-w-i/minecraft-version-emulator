# Debt and coupling audit — `net.historicalexperience`

Read-only audit. Scope: `src/main/java/net/historicalexperience` (453 java files incl. tests; main = 331 files, test = 122), `build.gradle`, `scripts/`, `src/main/resources/{fabric.mod.json,historical_experience.mixins.json}`.
Evidence = file:line. Counts come from `Select-String`/`grep` over the working tree (nothing was compiled or run).
**Coverage caveat:** the final "who references the verification classes from scripts/docs" pass was only partly completed (see §2.4). Everything in §1, §3 (as marked), §4, §5, §6, §7 was verified.

Main-source package sizes (non-comment lines ≈ `Measure-Object -Line`):

| package | files | lines |
|---|---|---|
| mixin | 145 | 9 292 |
| gameplay | 34 | 5 526 |
| server | 23 | 4 527 |
| world.upgrade | 16 | 3 980 |
| generation | 11 | 3 331 |
| client.textures | 18 | 3 258 |
| worldgen | 9 | 3 093 |
| version | 5 | 2 594 |
| client.version | 10 | 2 373 |
| client.lighting | 12 | 1 637 |
| integration.modernerbeta | 2 | 1 120 |
| world.metadata + world | 9 | 1 137 |
| profile | 2 | 434 |

---

## 0. build.gradle — source sets, tests, run configs

* **Source sets:** only the default `main` + `test` (no `verification`/`probe` source set, no `splitEnvironmentSourceSets()`; client and server code share `src/main`, `fabric.mod.json:"environment": "*"`). Mixins: one config `historical_experience.mixins.json` with a common list and a `"client"` list (client list begins at `ContainerScreenAccess`).
* **Test setup:** JUnit 5 (`build.gradle:63-65`), `useJUnitPlatform`, `workingDir projectDir` (`:70-73`). 122 test files; no Minecraft-runtime tests, all pure-JVM/unit. Optional `-PapronFixture` system property (`:75-78`).
* **No `runServer`/`runClient` rename**: loom run configs `clientQuickPlay` (`:228-314`) and `server` (`:315-345`). Both are *property-gated probe launchers*, not plain run configs:
  * `clientQuickPlay` maps Gradle `-P` flags to `-Dhistorical_experience.*` JVM args: `titleStart`, `screenLayoutProbe`/`screenProbe` (`:235-241,307-313`), `snapshotProbe` (`:242`), `villageResidentsProbe` (`:246`), `snapshotAnvilProbe` (`:249`), `indevProbe` (`:253`), `clientMixinCheck` (`:257`), `ntStarsProbe` (`:260`), `ntVisualProbe` (`:263`), `texturesProbe` + ~10 sub-flags (`:266-306`).
  * `server` maps `enchantProbe` (`:321`), `dragonProbe` (`:324`), `generationProbe` + `netherProbe/probeToken/probeRadius/probeCenter/probePrepareFiniteRepairFixture/probeRepairFiniteEdge/probeUpgradeTo` (`:327-344`).
  * **No Gradle mapping exists for:** `historical_experience.villageProbe` (+`.token`,`.cells`) read at `VillageVerification.java:81,88,139`; and none for `historical_experience.probe.establish/verify/autoStop/reload` (constants at `HistoricalServerLifecycle.java:75-94`). `villageProbe` can only be passed via raw `JAVA_TOOL_OPTIONS`/manual `-D`.
* **Verification tasks:** only `verifyLicenceArtifacts` (hooked to `check`, `:121-161`) and `verifyPublishedSource` (network, manual, `:168-214`). No Gradle task runs any in-game probe; the probes are driven by `scripts/*.py|*.ps1` (see §2).
* Extra: `sourcesJar` and `jar` bundle licence files (`:90-115`). `mod_license` etc. in `gradle.properties`. `yarn_mappings` in `gradle.properties` is unused (build uses `officialMojangMappings`, `build.gradle:44`) — harmless, but stale.

---

## 1. Classes with excessive responsibility

Line counts are from `Measure-Object -Line` (blank lines excluded, so physical length is slightly larger).

| File | Lines | Distinct concerns mixed |
|---|---|---|
| `version/HistoricalVersions.java` | 1 841 | (a) **registry data** — static initialiser building ~10 `HistoricalVersion` constants with `chronology(...)` keys (e.g. `:97 BETA 1.7.3`, `:436 Release 1.0.0`, `:598 Alpha 1.0.1`, …); (b) **generation-revision builders** — `withSurfaceSandstoneDisabled :1470`, `withBetaElevenBiomes :1498`, `withEraOres :1574`, `renamedClone :1615`, `rename :1643`, `withPreBetaOreRevision :1684`, `_no_grass/_no_modern_content/_with_ores/_cave_fix` alias builders `:1700-1862`; (c) **ore derivation** — `eraHasRedstoneOre/LapisOre/EmeraldOre :1518-1531`, `eraOrePopulation :1543` (imports `generation.BetaOreRules`); (d) **pre-Beta rule synthesis** — `preBetaEvidence :1282`, `preBetaRules :1395`; (e) **query API** — `byId :1876`, `byRevisionAlias :1883`, `atOrAfter/before :1898-1903`, `isFiniteWorld/isFinitePreset/finiteProfiles :1257-1267`. Imports gameplay + generation (`:27-33`). |
| `worldgen/HistoricalVillagePieces.java` | 1 432 | Piece-type constants (`PIECE_SHACK :118`…), a `Style` class (`:181`), a `VillagePiece extends StructurePiece` (`:405`) with NBT (de)serialisation, block placement and village-specific `postProcess` (`:1492`). Structure *geometry*, *palette/style* and *serialisation* in one file. |
| `generation/HistoricalFeatureExclusions.java` | 1 098 | (a) **data** — hundreds of `Exclusion(featureId, reason)` lists per era (`:81…:826`), `aquatic(...) :466`, `lapisOres :632`, `desertWell :819`; (b) **data transforms** — `keepingVanillaOres :336`, `eraOreReplacement :374`, `adaptTo1211 :855`; (c) **runtime hooks** — `historicalAlias(ChunkGenerator) :892` (reads Moderner Beta settings), `shouldSuppressBlock :1046`, `shouldSuppressVines :1082`, `isAnachronisticForActiveVersion :1086`, ore replacement checks `:983`, End-gateway check `:1006`; (d) **mutable diagnostics** — 4 concurrent sets/`LongAdder`s (`:826,1023,1024,1073`) and `diagnostics() :1144`. |
| `gameplay/HistoricalGameplayTweaks.java` | 1 094 | ~75 public static predicates on `Level`/`versionId`: (a) the gate table `appliesTo :242`, `gateNames :353`, `evidenceFor :1007`; (b) *Level-resolving wrappers* (`skeletonStrafingDisabled :392` … `experienceOrbSpawnDisabled :996`); (c) *boundary-chronology getters* (`preBeta18BoundaryChronology :76`, `sneaking… :723`, `offhand… :737`, `nether… :750`, `f4Portal… :761`, …); (d) *per-feature predicates* (food `:810-965`, enchanting `:670-705,925-949`, bow `:1112-1129`, jump-XP `:1142-1178`); (e) version arithmetic `versionOf :203`, `olderThan :220`. Gate registry + evidence + version math + feature API in one class. |
| `integration/modernerbeta/ModernerBetaAdapter.java` | 1 086 | Preset resolution (`resolveHistoricalPreset :97`, `resolveUpstreamBeta173 :108`, `resolvePresetById :143`, `declaredFoundation :128`); marker reading (`explicitPresetMarker :212`, `markerStateOf :770`); **evidence/diff records** (`GeneratorFacts :263`, `AppliedPresetEvidence :326`, `AppliedRecipeDeltas :439`, `GeneratorDescription :574`, `DimensionFacts :608`, `ServerSettingsDescription :630`); comparison logic (`compareAppliedSettings :484`, `compareToUpstreamBeta173 :371`); server description (`:830-900`); registry-key accessors (`:1155-1160`). The "bounded adapter" of AGENTS.md is also a verification-report builder. |
| `server/GenerationVerification.java` | 967 | See §2: it is a verification harness (damage table, tool durability, crafting, mixin smoke test, ore/block drops, spawn probe, Nether probe, upgrade staging) that is *also* called by `HistoricalExperienceMod` at start-up. |
| `generation/HistoricalBlockAvailability.java` | 759 | ~150 `introduce(versionId, timeline, evidence, ids…)` registrations (`:171-…`) = pure data, plus a `Boundary` record (`:73`) and lookup logic. Data belongs in a resource (JSON) per AGENTS.md "declarative data for availability". |
| `client/textures/TexturesProbe.java` | 732 | Probe (see §2) with 20+ static state fields (`:66-69,229-234,432-433,505-508,610-611,644-645`). |
| `gameplay/HistoricalSpawnRules.java` | 575 | Profile→entity allow-lists (data `:303-495`), alias resolution (`:524`), diagnostics counters (`:578-612`). |
| `client/version/HistoricalVersionScreen.java` | 563 | One screen with list widget, narration, filtering. Acceptable (UI) but large. |
| `server/HistoricalServerLifecycle.java` | 557 (591 physical) | Marker→decision wiring, establish, fail-closed, generation-scope set/clear, probe property constants (`:75-94`) with an **empty `if (Boolean.getBoolean(PROBE_VERIFY_PROPERTY)) {}` block** (`:114-120`), upgrade advance at stop. |
| `worldgen/HistoricalVillageStructure.java` | 553 | Structure layout algorithm + height probing + static diagnostic `declinedForBiomeCount :577`. |
| `world/upgrade/WorldGenSettingsAdvance.java` | 519, `GenerationUpgrade.java` 493 | Transactional upgrade (each cohesive, but large). |

Highest-value splits: `HistoricalVersions` → `HistoricalVersions` (registry/query only) + `GenerationRevisionBuilder` (b) + move (c) to `generation`; `HistoricalFeatureExclusions` → data tables vs `…Hooks`; `HistoricalGameplayTweaks` → split by gate domain; `GenerationVerification` → move to verification package.

---

## 2. Probe / verification / diagnostic classes in production source

### 2.1 Catalogue (all in `src/main`, packaged into the shipped jar)

| Class | Lines | Trigger | Registered at |
|---|---|---|---|
| `server/GenerationVerification` | 967 | `-Dhistorical_experience.generationProbe=true` (+`.nether .token .radius .center .prepareFiniteRepairFixture .repairFiniteEdge .upgradeTo`) `:43,811-973` | `HistoricalExperienceMod.java:45` `registerIfRequested()` |
| `server/VillageVerification` | 284 | `-Dhistorical_experience.villageProbe` `:81` (+`.token .cells`) | `HistoricalExperienceMod.java:52` |
| `server/DragonFightVerification` | 127 | `-Dhistorical_experience.dragonProbe` `:50` | `HistoricalExperienceMod.java:53` |
| `server/EnchantingVerification` | 237 | `-Dhistorical_experience.enchantProbe` `:51` | `HistoricalExperienceMod.java:54` |
| `server/EarlyGameplayVerification`, `EarlyPlayerGameplayVerification`, `EarlyLeafDropVerification`, `EarlyMobDeathVerification`, `EarlyIndependentMenuVerification`, `EarlyMobDropCapture`, `FiniteRepairFixtureVerification`, `ColumnStats` | 147/237/112/161/83/30/102/212 | package-private; reached only from `GenerationVerification.run` (`:818,948-958`); each re-checks `generationProbe` and throws `"Probe only"` otherwise | transitively |
| `server/VillageResidentAcceptance` | 139 | `-Dhistorical_experience.villageResidentsProbe` `:32,111` | called from client `SnapshotAnvilProbe.java:79,85` |
| `mixin/ServerLevelHistoricalMobDropProbeMixin` | 19 | **always applied** (listed in the common `mixins` array); observer on `ServerLevel.addEntity` RETURN → `EarlyMobDropCapture.recordAccepted`, a no-op unless a capture is open (`ThreadLocal`) | `historical_experience.mixins.json` |
| `mixin/NaturalSpawnerProbeAccessor`, `mixin/CreativeScreenProbeAccess` | – | accessor interfaces used only by `GenerationVerification.java:701,746` and `SnapshotAnvilProbe.java:228,232` | mixins.json |
| `gameplay/HistoricalCombatDiagnostics` | 59 | **no gate**: always-on counters, written by `PlayerHistoricalCombatMixin:93`, `ServerLevelHistoricalOrbMixin:61`; read by `GenerationVerification:948` and `HistoricalUpgradeService:149` | – |
| `client/HistoricalClientDiagnostics` | 91 | registered unconditionally `HistoricalExperienceClient.java:56` | – |
| `client/LoadingHangWatchdog` | 108 | **always on**, tick-registered `HistoricalExperienceClient.java:92-93` | – |
| `client/ClientMixinCheck` | 66 | `-Dhistorical_experience.clientMixinCheck=<target>` `:50` | `…Client.java:69` |
| `client/NtVisualProbe` | 181 | `-Dhistorical_experience.ntVisualProbe` `:50` | `…Client.java:68` |
| `client/NtStarsProbe` | 109 | `-Dhistorical_experience.ntStarsProbe` `:46` | `…Client.java:73` |
| `client/version/ScreenProbe` | 262 | `-Dhistorical_experience.screenProbe[.layoutOnly]` `:64,87` | `…Client.java:64` |
| `client/version/SnapshotFeatureProbe` | 393 | `-Dhistorical_experience.snapshotProbe` (+`.root`) `:50-70` | `…Client.java:65` |
| `client/version/SnapshotAnvilProbe` | 273 | `-Dhistorical_experience.snapshotAnvilProbe` `:50` | `…Client.java:66` |
| `client/indev/IndevMapProbe` | 380 | `-Dhistorical_experience.indevProbe` (+`.root`) `:54,74` | `…Client.java:67` |
| `client/textures/TexturesProbe` | 732 | `-Dhistorical_experience.texturesProbe` + 10 sub-props `:75-133,191,248,673,756` | `…Client.java:88` |

Probes are therefore **reachable in production** (the code and the always-applied mixin ship in the jar). They are inert unless a system property is set, which any user/pack can do; none requires an operator permission. Three always-on ones (`HistoricalCombatDiagnostics`, `HistoricalClientDiagnostics`, `LoadingHangWatchdog`) are diagnostics rather than probes.

Cross-cutting smell: the probes call *into production* via public statics, and `ServerLevelHistoricalMobDropProbeMixin` and the two `*Probe*Access` mixins make probe code a dependency of mixin config (`required: true`, `defaultRequire: 1`) — a probe mixin failing to apply would abort the production mod.

Other production-source test hooks: public mutable `HistoricalDamageValues.LAST_BRANCH/LAST_TIER_BONUS/LAST_SWORD_PRE_NERF/LAST_TIER_NAME` (`HistoricalDamageValues.java:231-234`), written on **every** attack-damage computation (`:178-222`) and read only by `GenerationVerification.java:315-318`.

### 2.2 Should they move?

Recommendation: create `net.historicalexperience.verification` (or a Gradle `probe` source set joined into the **dev** runtime only) with sub-packages `verification.server`, `verification.client`. The verification harness must keep working, so keep these entry-points reachable by the same property names:

1. `GenerationVerification` + the 8 package-private `Early*`/`FiniteRepairFixture*`/`ColumnStats`/`EarlyMobDropCapture` helpers → `verification.server`. **Constraint:** `ColumnStatsTest` (`src/test/.../server/ColumnStatsTest.java`, ~40 refs) uses `ColumnStats` — move the test with it. `ColumnStats` has main refs = 0 from non-test, 1 from test; it is only used by tests (+ the verification package), so it is a *test-support class living in main*.
2. Client probes (`ScreenProbe`, `SnapshotFeatureProbe`, `SnapshotAnvilProbe`, `IndevMapProbe`, `NtVisualProbe`, `NtStarsProbe`, `ClientMixinCheck`, `TexturesProbe`) → `verification.client`; `SnapshotFeatureProbe` helper statics (`check/call/await/press`) are imported by `IndevMapProbe` and `SnapshotAnvilProbe` (`SnapshotAnvilProbe.java:4`, `IndevMapProbe.java:49,98,259-384`) — extract them as `verification.client.ProbeSupport`.
3. Registration: replace direct calls in `HistoricalExperienceMod:45,52-54` / `HistoricalExperienceClient:64-68,73,88` with ONE `VerificationBootstrap.registerIfRequested()` that no-ops (no class loading of the probes) unless any `historical_experience.*Probe` property is present. Even better: register through an optional second Fabric entrypoint key (`"hex-verification"` custom entrypoint) loaded only when the verification jar/source set is on the classpath.
4. Mixins: move `ServerLevelHistoricalMobDropProbeMixin`, `NaturalSpawnerProbeAccessor`, `CreativeScreenProbeAccess` to a separate `historical_experience.verification.mixins.json` (not `required`), declared in `fabric.mod.json` only in the dev source set. Note `NaturalSpawnerProbeAccessor` is referenced by javadoc of the *production* `NaturalSpawnerHistoricalSpawnMixin:60`; fine.
5. **Do not delete anything**: `scripts/verify-generation-1211.py`, `verify-lifecycle-controls.ps1`, `verify-matrix-runtime.ps1`, `prepare-probe-run.ps1`, `verify-generation-1211-matrix.py`, `summarize-1211-verification.py` and docs (`ADDING-A-VERSION.md` mentions `generationProbe`/`screenProbe`, `VILLAGE-RESIDENTS.md` mentions `villageResidentsProbe`, `SNAPSHOT-PATCHES.md` `snapshotProbe`) depend on them (grep evidence: `generationProbe` in 4 scripts + 1 doc; `screenProbe`, `snapshotProbe`, `villageResidentsProbe` in docs; the Gradle `-P` flags in `build.gradle:228-345` map to the rest).
6. `HistoricalDamageValues.LAST_*`: replace with an optional listener (`Consumer<Computation>` set by the verification package) so production does not write four volatiles per attack.

### 2.3 Disarmed / dead scaffolding inside the lifecycle
`HistoricalServerLifecycle.java:75-94` defines four public constants `PROBE_ESTABLISH_PROPERTY`, `PROBE_VERIFY_PROPERTY`, `PROBE_AUTOSTOP_PROPERTY`, `PROBE_RELOAD_PROPERTY`; only `PROBE_VERIFY_PROPERTY` is read (`:114`) and its block is empty (`:114-120`, comment says "deliberately NOT ported"). No script or test references `probe.establish|verify|autoStop|reload` (grep over `scripts/`, `docs/`, `build.gradle`, `src/test`: no hits). See DEAD list.

### 2.4 Not fully verified
* Whether `scripts/verify-*.ps1` pass `villageProbe`/`dragonProbe`/`enchantProbe` — grep of `scripts/` and `docs/` for those names returned **no** hits, i.e. only `build.gradle` (`dragonProbe`, `enchantProbe`) wires them and `villageProbe` has no launcher at all. Treat `VillageVerification` as "armed only by hand `-D`"; before moving, ask the owner whether it is still used (do not delete).
* `ntStarsProbe`, `ntVisualProbe`, `clientMixinCheck`, `indevProbe`, `texturesProbe` are wired only via `build.gradle` `-P` flags (no scripts/docs).

---

## 3. Unused code

Method used: for every `.java` in `src/main` the class simple-name was searched in all main/test/scripts/docs files (excluding its own file); mixins checked against `historical_experience.mixins.json`; for methods, every declaration with a name that occurs exactly once in `src`, `scripts`, `docs` (+ `research/`, `PROGRESS.md`) was reported. Entry-point/registered classes were then excluded.

### 3.1 Whole classes
* No class has zero references. Classes with a single (non-self) reference are all registered entrypoints/mixins/wiring (`HistoricalModMenu` ← `fabric.mod.json:entrypoints.modmenu`; every `*Mixin` ← mixins.json; `F4PortalKey`, `SnapshotCreativeKey`, `TextureFirstRun`, `LoadingHangWatchdog`, `BiomeConversionScreen`, probes ← `HistoricalExperienceClient`).
* **Every class in `mixin/` appears in the json and every json entry has a file** (checked both ways: no orphan mixins).
* `ModernerBetaIntegrationException` has one reference (a catch/throw in `ModernerBetaAdapter`) — live.
* `server/ColumnStats`: referenced only by `ColumnStatsTest` (no production caller) — **test-only; not dead** (a retained test depends on it). Candidate to move to the verification package with its test.
* `HistoricalCreativeAuthority`, `HistoricalMarkerDecision` — the last grep that would have confirmed their callers was cut off by tool termination; **not claimed dead** (both are used by lifecycle/creative code by name; `HistoricalMarkerDecision` is used at `HistoricalServerLifecycle.java:43-49,147-149`).

### 3.2 DEAD methods / fields (zero references in main, test, scripts, docs, mixins.json, fabric.mod.json — each name occurs exactly once, at its declaration)

| Symbol | Declaration | Evidence |
|---|---|---|
| `HistoricalColormaps.appliedDescription()` | `client/colour/HistoricalColormaps.java:91` | name appears 1× in whole repo |
| `IndevMapProbe.onEndTick` | `client/indev/IndevMapProbe.java:59` | 1× (registration uses a different lambda/method ref; verify before removal — may be referenced via `::onEndTick` pattern, but grep `\bonEndTick\b` found only the declaration) |
| `HistoricalChestTweaks.setOldChest`, `setOldEnderChest` | `client/lighting/HistoricalChestTweaks.java:101,109` | 1× each |
| `HistoricalLightingTweaks.setOldLeavesLighting`, `setOldWaterLighting` | `client/lighting/HistoricalLightingTweaks.java:48,66` | 1× each |
| `HistoricalSmoothLighting.resetCache` | `client/lighting/HistoricalSmoothLighting.java:155` | 1× |
| `HistoricalWorldTweaks.setOld2dItems`, `setRecipeBookDisabled`, `skyColourOverride`, `setOldSkyColour` | `client/lighting/HistoricalWorldTweaks.java:121,146,250,271` | 1× each |
| `HistoricalTextureConfig.resetSetup` | `client/textures/HistoricalTextureConfig.java:74` | 1× |
| `HistoricalTextureService.useDownloader` | `client/textures/HistoricalTextureService.java:170` | 1× |
| `PackPin.pageUrl` | `client/textures/PackPin.java:72` | 1× |
| `TextureStatusView.allDependenciesPresent` | `client/textures/TextureStatusView.java:96` | 1× |
| `TexturesProbe.modMenuStageUnused`, `modMenuTicksUnused` | `client/textures/TexturesProbe.java:364-365` | named "Unused", 1× each — abandoned experiment |
| `HistoricalChestLoot.reportedProfiles` | `gameplay/HistoricalChestLoot.java:91` | 1× |
| `HistoricalCombatDiagnostics.recordCooldownAnswer`, `recordDamageRewrite` | `gameplay/HistoricalCombatDiagnostics.java:42,50` | only mention elsewhere is `research/implementation-inventory.md` (a doc, not code) |
| `HistoricalDamageValues.isAttributeComponent` | `gameplay/HistoricalDamageValues.java:270` | 1× |
| `HistoricalFoodRules.recordHeal`, `recordEatGateOverride` | `gameplay/HistoricalFoodRules.java:240,247` | only `research/implementation-inventory.md` |
| `HistoricalPlayerRules.recordSprintSuppression`, `recordHungerTickSuppression` | `gameplay/HistoricalPlayerRules.java:83,87` | only `research/implementation-inventory.md` |
| `HistoricalLootRules.recordDrop`, `replacedEntityTypes` | `gameplay/HistoricalLootRules.java:202,224` | 1× each |
| `HistoricalRecipeGate.refusedPairs` | `gameplay/HistoricalRecipeGate.java:121` | 1× |
| `HistoricalSpawnFilter.changedAnything` (record accessor-style method) | `gameplay/HistoricalSpawnFilter.java:54` | 1× |
| `HistoricalGameplayTweaks.skeletonStrafingDisabled`, `criticalHitDisabled`, `monsterItemPickupDisabled`, `animalTemptingDisabled`, `babyAnimalSpawningDisabled`, `animalPanicDisabled`, `babyZombieSpawningDisabled`, `monsterEnchantmentSpawnDisabled`, `attackCooldownDisabled`, `sweepAttackDisabled` | `gameplay/HistoricalGameplayTweaks.java:392,397,407,427,432,444,456,467,498,503` | 0 code callers (only `research/implementation-inventory.md` and, for 3, `PROGRESS.md`) — mixins use `appliesTo(...)`/other names instead. Redundant wrappers. |
| `HistoricalGameplayTweaks.olderThanVersion`, `documentedGates` | `:550,:1182` | 1× each |
| `HistoricalChestLoot`/`HistoricalGameplayTweaks` wrappers above are `public` — a javac "unused" warning never fires, hence they accumulate. | | |
| `HistoricalFeatureExclusions.shouldSuppressVines` | `generation/HistoricalFeatureExclusions.java:1082` | only `research/implementation-inventory.md` |
| `HistoricalVersions.finiteProfiles` | `version/HistoricalVersions.java:1267` | 1× (javadoc says "so a test can assert…" but no test calls it) |
| `HistoricalProfiles.beta173ContentPreview2/3/4/5` | `profile/HistoricalProfiles.java:231,243,255,267` | 1× each (only `beta173ContentPreview()` at `:218` is referenced? — not re-verified; the 2–5 variants are definitively unreferenced) |
| `ModernerBetaAdapter.compareToBeta173ContentPreview` | `integration/modernerbeta/ModernerBetaAdapter.java:376` | 1× |
| `ModernerBetaAdapter.historicalPresetReference`, `explicitHistoricalPresetMarker`, `markerValue`, `markerStateOf`, `providersMatchUpstreamBeta173`, `settingsPresetRegistryKey`, `surfaceConfigRegistryKey` | `:86,:236,:597,:770,:1146,:1155,:1160` | In `src/` + `scripts/` each name occurs once (declaration). They do also occur in **`exports/checkpoint-2026-10-12/…`** (an archived copy, not a build input, and `research/dev/rebuild-evidence-json.ps1`/`research/moderner-beta-evidence.json` mention `settingsPresetRegistryKey`/`surfaceConfigRegistryKey` as strings). Treat `settingsPresetRegistryKey`/`surfaceConfigRegistryKey` as **NOT dead** (a retained research script mentions them); the rest are dead in the live tree. |
| `BiomeConversionRecord.wasConverted` | `world/upgrade/BiomeConversionRecord.java:357` | 1× |
| `GenerationUpgrade.backedUpFiles`, `WorldGenSettingsAdvance.touchedFiles` | `:507`, `:551` | 1× each |
| `HistoricalBiomeConversionDriver.regionsDone`, `regionsTotal` | `:151,156` | 1× (the backing `REGIONS_DONE/TOTAL` atomics are written but only read here) |
| `HistoricalVillageBlocks.alwaysPlacedIds` | `worldgen/HistoricalVillageBlocks.java:345` | 1× |
| `HistoricalVillageStates.isDeclared` | `:95` | 1× |
| `HistoricalVillageTypes.structureTypeId`, `pieceTypeId`, `biomeTagReference`, `biomeTagId` | `worldgen/HistoricalVillageTypes.java:90,95,110,115` | 1× each |
| `HistoricalVillagePieces.postProcess` | `:1492` | 1× (likely an override-style name for `StructurePiece.postProcess` — **not claimed dead**; reflective/inherited call) |
| `IndevMapSettings.PARADISE` | `worldgen/IndevMapSettings.java:70` | enum constant name appears once outside prose (enums may be used via `values()`; **not claimed dead**) |
| UI overrides flagged by the scan (`BiomeConversionScreen.isPauseScreen`, `HistoricalVersionScreen.renderWidget/updateWidgetNarration/getRowWidth`) | – | framework overrides — **not dead** |

Confidence note: "1×" means the identifier matched `\bname\b` exactly once in 1 035 text files under the repo (excluding `build/`, `.git`, `.gradle`, `.upstream`, `run`, `logs`). Reflection by name (Gradle, Mixin string targets) was not found for any of these, but `@Invoker/@Accessor` string names were not individually checked for the client-lighting setters. Remove in small commits with a compile + `./gradlew test`.

### 3.3 Abandoned experiments / obsolete workarounds / redundant wrappers
* `HistoricalServerLifecycle.java:109-120` — comment-only blocks for a removed spawning algorithm and an un-ported `RuntimeProbe`; the `if (Boolean.getBoolean(PROBE_VERIFY_PROPERTY)) { }` is a no-op, and `PROBE_ESTABLISH/AUTOSTOP/RELOAD` constants (`:75-94`) have no readers.
* `TexturesProbe.java:364-365` — `…Unused` fields.
* `HistoricalProfiles.beta173ContentPreview2..5` (`:231-271`) + the corresponding `HistoricalMarker.CONTENT_PREVIEW_{2..5}_PRESET_ID` constants: five near-identical accessors that differ only by constant. Replace with a single `byGenerationPreset(id)` (already public).
* Redundant resolver triplication — see §6.
* `exports/checkpoint-2026-10-12/` holds 30 stale copies of java files (including an older `ModernerBetaAdapter`, `RuntimeProbe`, `ModernerBetaSettingsAccess`) inside the repo root; not on the source path but they pollute grep results. Consider removing or `.gitignore`-ing (owner decision).
* `gradle.properties:yarn_mappings` unused (build uses Mojang mappings).
* `java.util.logging` used in 3 places (`HistoricalFeatureExclusions` uses `LOGGER.log(Level.INFO,…)` at `:908`) while the rest uses slf4j (`HistoricalExperienceMod.LOGGER`).

---

## 4. Static mutable state / global singletons

Thread notation: C = client thread, S = server thread (integrated server in single-player is a *different thread from C but the same JVM*), W = worldgen/worker threads, N = netty thread, M = main/init.

### 4.1 Cross-side session holders (highest risk)

| Owner | Field | Thread-safety | Writers → readers | Crosses client/server? |
|---|---|---|---|---|
| `gameplay/HistoricalActiveSession` `:33` | `static volatile String versionId` | volatile single ref | **Written** by server (`HistoricalVersionSync.java:84` on player JOIN, `:96` on SERVER_STOPPED), by client (`HistoricalClientRules.java:55,62` on payload / disconnect), and by 3 verification classes (`EnchantingVerification:95-100`, `EarlyPlayerGameplayVerification:38-41`, `GenerationVerification:879`). **Read** by 5 client, 22 gameplay, 67 mixin, 7 server call sites. | **Yes, and it is a single JVM-global.** In single-player, server JOIN writes the id and the client payload then writes it again; on stop `SERVER_STOPPED` clears it even if the client is still "connected". On a dedicated server it is the *last-joined player's* world id (fine for a one-world server). Mixed read from server-thread mixins and client mixins. Not a per-level answer. |
| `client/HistoricalClientRules` `:45` | `static volatile String syncedVersionId` | volatile | written C (payload handler `HistoricalExperienceClient.java:48` — note comment claims client thread; Fabric's `registerGlobalReceiver` handler runs on the **client thread**), read C | client only |
| `server/HistoricalSnapshotRuntime` `:14,16` | `ConcurrentMap<MinecraftServer,String> SERVER_PATCHES` (per-server, good) + `static volatile String clientPatchId` | concurrent / volatile | written S (SERVER_STARTED) / C (`setClientSelection`) | **Yes**: one class in package `server` holds the client's prediction field; `client` package imports `server` (§5). |
| `generation/HistoricalGenerationScope` `:47` | `static volatile String generationAlias` | volatile | written S (lifecycle `:163,176,193,472`), read **W** (worldgen: `HistoricalFeatureExclusions.activeAlias :1117`, structure hooks) | server↔worker threads. Documented fallback only. Process-global: two servers in one JVM (not supported by MC) would collide. |
| `server/HistoricalServerContext` `:41-44` | `volatile MinecraftServer server`; `volatile String cachedWorld`; `volatile Set<Long> cachedConvertedChunks`; `volatile String cachedTarget` | volatile fields but `refresh()` (`:91-115`) does non-atomic multi-field update (check `cachedWorld`, then write three fields, then `cachedWorld = world` last — benign race: readers may see new chunks with old target) | written S (`HistoricalServerLifecycle:354,451`), read from biome-source mixins `ModernBetaBiomeSourceHistoricalChunksMixin:63,73`, `ModernBetaClimateSamplerHistoricalMixin:62`, `ModernBetaDebugBiomeHistoricalMixin:50` — these are in the mixins.json **client** list, i.e. executed on the client thread / integrated-server threads. | Yes: holds a `MinecraftServer` reference statically; stale if a client opens world B while an integrated server for A is shutting down. |

### 4.2 Client-only unsynchronised state (all on client thread, so OK unless noted)
`ClientMixinCheck.done :44`, `F4PortalKey.held :59`, `SnapshotCreativeKey.held :12`, `LoadingHangWatchdog.joinedAWorld/overlaySinceNanos/reported :54-57`, `HistoricalStars.rebuilds :35`, `TextureFirstRun.shownThisSession :33`, `CreateWorldHistoricalVersion.key/enabled :82-83`, `IndevMapSelection.choice/chosen :14-15`, `HistoricalTextureService.ticks/reloads/missingNotified/scanCount/autoDownloadAttempted :79-81,135,159` (non-volatile, next to volatile siblings `:74-78` — inconsistent), probe state (`NtVisualProbe`, `NtStarsProbe`, `TexturesProbe`, `ScreenProbe LOG`, `SnapshotFeatureProbe started/toggleChecked`). `HistoricalWorldTweaks`/`HistoricalClientOptions` use `ConcurrentHashMap` caches (`HistoricalClientOptions.java:36-37`).
Volatile client rendering state read from render/light threads: `HistoricalColormaps.grass/foliageOld/foliageNew/loaded/applied :81-85`, `HistoricalOldLighting.active/toggle/timeSkyLight/weatherSkyLight/relightAll :71-75` + `ConcurrentLinkedDeque RELIGHT_QUEUE :77` (lighting engine threads → client), `HistoricalSmoothLighting.toggle :75`, `HistoricalStars.style :34`, `HistoricalJumpInput.jumpedSincePress :42`.

### 4.3 Server/common
* `world/upgrade/HistoricalBiomeConversionDriver` `:59-69`: Atomics + 5 volatile strings/flags, mutated by the conversion worker, read by the client `BiomeConversionScreen` (`import …world.upgrade.HistoricalBiomeConversionDriver`) — **a client screen reads a server-side worker's global progress**.
* `world/HistoricalSnapshotCreation.PENDING :11` — `AtomicReference`, written by client create-world UI (`CreateWorldHistoricalVersion:322,329`, `CreateWorldHistoricalFooterMixin:38,45`), consumed by common world code → crosses sides by design (client → integrated server).
* `worldgen/HistoricalVillageTypes.registered :63` — plain static boolean, init-time only (main thread).
* `version/VersionOverview` `:50-51`: `ConcurrentHashMap CACHE` + `volatile boolean loaded`.
* `gameplay/HistoricalDamageValues.LAST_* :231-234` — **public** mutable volatiles, written on every attack calculation (S and C) for diagnostics.
* `gameplay/HistoricalRecipeGate.refusals :61` — **plain `static long`** incremented from the server thread while `REFUSED` is a concurrent set (`:60`): unsynchronised counter (data race on read from probes).
* `Concurrent` diagnostic collections: `BetaOreRules.PLACED_BY_ORE :139`, `HistoricalChestLoot.REMOVED_BY_ITEM/REPORTED_PROFILES :55-56`, `HistoricalLootRules.DROPS_BY_ENTITY/REPLACED_ENTITIES :159-160`, `HistoricalFoodRules.HEALS_BY_ITEM/HEALED_ITEMS :195-196`, `HistoricalSpawnRules.REMOVED_ENTITY_TYPES :496`, `HistoricalStructureRules.SUPPRESSED_IDS :313`, `HistoricalFeatureExclusions.UNRESOLVED/DIAGNOSED_PRESETS/SUPPRESSED_BLOCK_IDS/SEEN_BLOCK_IDS :826,1023,1024,1073`, `EarlyRecipeLookup.CACHE :22`, `HistoricalDamageValues.REWRITTEN :95` — correct (concurrent), but each is unbounded process-lifetime diagnostics written from **worldgen worker threads** (the `FeatureExclusions`/`BetaOreRules` ones) and read only by verification. Memory growth is bounded by key space only.
* Non-concurrent static maps populated in static initialisers and read-only afterwards (safe): `HistoricalBlockAnachronisms.INTRODUCED_IN :42` (LinkedHashMap), `HistoricalBlockAvailability.INTRODUCED_IN :114`, `HistoricalCraftingEra.EXISTED_BY_1_6_4 :78` (HashMap), `HistoricalMobLootHelper.LOOT_MAP :74`, `HistoricalVillageBlocks.COMMON/FORBIDDEN :118,163`, `HistoricalItemAvailability.INTRODUCED :56`, `HistoricalEnchanting.INTRODUCED :63`, `HistoricalCreativeCatalog.ITEMS :13`, `HistoricalBlockPalette.MODERN_TO_LEGACY :69`. Not wrapped in `Collections.unmodifiableMap`/`Map.copyOf` → mutable by accident.
* `EarlyMobDropCapture.ACTIVE :15` — `ThreadLocal`, probe only.

### 4.4 Risks to flag
1. `HistoricalActiveSession` is written by both sides and cleared on `SERVER_STOPPED` even when the client state is independent (single-player stop after disconnect is ordered; but dedicated-server + any client-embedded server is not reachable). It is the root cause of the "ClientLevel returns null" workaround documented in `HistoricalExperienceClient.java:104-111`.
2. `HistoricalServerContext` mixins sit in the client mixin list; a dedicated server never applies them (fine), but singleplayer integrated + client both read the same static.
3. Static `MinecraftServer` reference (`HistoricalServerContext.server`) is cleared in `onServerStopped` — OK.

---

## 5. Cross-package dependency matrix

Counts = `import net.historicalexperience.<to>…` statements located in a file of package `<from>` (self-package imports excluded; same-package class use has no import and is not counted; fully-qualified inline references like `net.historicalexperience.server.X` in code bodies — widely used in `HistoricalExperienceMod`, `HistoricalExperienceClient`, `HistoricalClientRules`, `HistoricalServerLifecycle` — are **not** counted, so true coupling is higher). Top-level buckets; `integration.modernerbeta` treated as its own bucket, `root` = `net.historicalexperience` itself (`HistoricalExperience`, `HistoricalMarker`, `HistoricalExperienceMod`).

| from \ to | client | gameplay | generation | integ.mb | mixin | profile | root | server | version | world* | worldgen |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **client** | – | 15 | | | 6 | | 1 | **7** | 42 | 1 | 4 |
| **gameplay** | | – | 3 | | 3 | 1 | | | 16 | | |
| **generation** | | **1** | – | | | | 1 | | 3 | 1 | |
| **integration.modernerbeta** | | | | – | | | 1 | | | | |
| **mixin** | **49** | 152 | 7 | | – | | 1 | 7 | 5 | 5 | 1 |
| **profile** | | | | | | – | 2 | | | 2 | |
| **server** | | 19 | 5 | 2 | | 1 | 8 | – | 3 | 16 | 6 |
| **version** | | **4** | **4** | | | | 1 | | – | | |
| **world\*** (world, world.metadata, world.upgrade) | | | | | | 3 | | | 10 | – | |
| **worldgen** | | | 1 | | | | 2 | | 4 | | – |
| **root** | | 1 | | | | | | 3 | | | |

(tests are not included.) Direct reads of `net.minecraft.client` from non-`client`, non-`mixin` main code: **none** found (good). Direct `mod.bluestaggo` (Moderner Beta) references: client 12, generation 2, integration 11, mixin 10, server 6 → the "bounded adapter" is bypassed in 4 other packages.

### 5.1 Violations
1. **`version` → `gameplay` / `generation`** (8 imports): `HistoricalVersion.java:24-26` imports `generation.BetaOreRules`, `generation.HistoricalFeatureExclusions`, `gameplay.HistoricalLootRules`; `HistoricalVersions.java:27-31` imports `generation.BetaOreRules`, `generation.HistoricalFeatureExclusions`, `gameplay.HistoricalFoodRules/LootRules/SpawnRules`. The registry (a leaf data layer) depends on its consumers.
2. **Cycles (package level):**
   * `version ⇄ generation`: `version→generation` (4) vs `generation→version` (3: `HistoricalFeatureExclusions.java:30-31`, `HistoricalStructureRules.java:27`).
   * `version ⇄ gameplay`: `version→gameplay` (4) vs `gameplay→version` (16).
   * `generation ⇄ gameplay`: `generation→gameplay` (`FiniteTransitionRuntime.java:10` imports `gameplay.HistoricalProfileLookup`) vs `gameplay→generation` (3: `HistoricalCraftingEra.java:24`, `HistoricalRecipeGate.java:22-23`).
   * `generation → world.upgrade` (`FiniteTransitionRuntime.java:11` imports `FiniteApronRelease`) and `world.upgrade → version → generation`: `generation → world → version → generation`.
   * `gameplay ⇄ mixin` (`gameplay→mixin` 3: `HistoricalMobLootHelper:20 SheepAccess`, `HistoricalToolInteractionEvents:19-20 AxeAccess/ShovelAccess`) vs `mixin→gameplay` (152). Mixin accessor interfaces should be a leaf, not imported by gameplay (acceptable in Fabric practice, but it makes `mixin` part of the cycle).
   * `client ⇄ mixin` (`client→mixin` 6 vs `mixin→client` 49) and `client → server → …`.
   * `server ⇄ client`? No (`server→client` = 0). OK.
3. **common → client:** `mixin→client` 49 — but most are client-list mixins. Common-list mixins that import client code / `net.minecraft.client`: `ItemStackHistoricalAttributeTooltipMixin.java:20` (`client.tooltip.HistoricalTooltips`), `EnchantmentScreenHistoricalMixin:24,100` (in the common `mixins` array although it targets a client-only class — works only because Mixin skips missing targets; `defaultRequire:1` makes this fragile), `RecipeToastHistoricalMixin:21-22`, `GuiAccess:17` — all four are listed in the **common** array (`mixins.json` lines for `EnchantmentScreenHistoricalMixin`, `GuiAccess`, `RecipeToastHistoricalMixin`, `ItemStackHistoricalAttributeTooltipMixin`) yet target client classes. On a dedicated server they would be loaded and fail class resolution unless guarded. **Move to the `"client"` array.** (Also `ServerCreativeHistoricalGrantMixin` etc. are correct.)
4. **client → server:** 7 imports (`F4PortalKey:23 PortalSpawnRequest`, `SnapshotCreativeKey:7`, `HistoricalExperienceClient:23 HistoricalVersionSync`, `ManageWorldHistoricalVersion:28 HistoricalUpgradeService`, `IndevMapProbe:17`, `SnapshotAnvilProbe:15 server.*`, `SnapshotFeatureProbe:14 server.*`) plus inline `server.HistoricalSnapshotRuntime` in `HistoricalClientRules`. Network payload records and the snapshot client-selection holder live in `server/` — should be a `net` / `protocol` package.
5. **generation → server:** none (`generation→server` = 0). **generation → gameplay** (1, `FiniteTransitionRuntime`) and **generation → world.upgrade** (1) exist.
6. **client → worldgen** (4) and **client → world.upgrade** (1): `IndevMapSettings` is data in `worldgen` consumed by UI; `BiomeConversionScreen:20` reads `HistoricalBiomeConversionDriver` statics.
7. **mixin → server/world/worldgen** (7/5/1) — expected, but `ServerLevelHistoricalMobDropProbeMixin:4` imports `server.EarlyMobDropCapture` (probe, §2).
8. **root → server/gameplay**: the mod initialiser references 20+ classes by FQN (`HistoricalExperienceMod.java:44-79`).
9. **gameplay → profile** (1) and **`profile → world.metadata`** (2: `HistoricalProfile.java:22`, `HistoricalProfiles.java:27` import `HistoricalIdentifier`) while `world → profile` (3) — a mild `profile ⇄ world` cycle (identifier type lives in `world.metadata`; should be in `profile` or a `core` package).
10. `HistoricalSnapshotPatches` (in `version`) is imported by `world.HistoricalSnapshotCreation:6` and `gameplay.HistoricalCreative*` — version package is the shared kernel but also imports consumers (item 1).

Suggested layering (bottom → top): `core/ids` (`HistoricalIdentifier`, `HistoricalMarker`, `HistoricalExperience` constants) → `version` (pure data) → `profile` → `generation`/`gameplay` rule data → `world`/`world.upgrade` → `integration.modernerbeta` → `server`/`client` → `mixin`. The 8 `version→gameplay/generation` imports are the minimum to break first (invert via a `VersionRules` carrier that holds the data, built by a `version.builders` class in the consumer packages or a `RegistryBootstrap` in `server`).

---

## 6. Duplicate `profileId → HistoricalVersion/profile` helpers

### 6.1 Catalogue

| Helper | Location | Input → output | Notes |
|---|---|---|---|
| `HistoricalVersions.byId(String)` | `version/HistoricalVersions.java:1876` | version id → `Optional<HistoricalVersion>` | the registry; **70 call sites** in main |
| `HistoricalVersions.byRevisionAlias(String)` | `:1883` | persisted generation alias → version | used by `isFinitePreset` etc. |
| `HistoricalProfiles.byId(HistoricalIdentifier|String)` | `profile/HistoricalProfiles.java:188,193` | id → `HistoricalProfile` (second, parallel model of "profile") |
| `HistoricalProfiles.byGenerationPreset(String)` | `profile/HistoricalProfiles.java` (used at `HistoricalProfileLookup:55`, `HistoricalSpawnRules:525`, `HistoricalServerLifecycle:227`, 20 more sites) | **persisted raw alias → GenerationBinding** (profile+revision+foundation) | canonical *alias* resolver |
| `HistoricalProfiles.resolve(…)` ×3 | `:314,328,342` | profile id + preset id → profile | |
| `HistoricalProfileLookup.profileIdForAlias / profileIdForGenerator / profileIdForLevel(ServerLevel)` | `gameplay/HistoricalProfileLookup.java:36,46,54` | → profile **id String** | 32 call sites (client 2, gameplay 8, generation 1, mixin 15, server 7). Reads generator via `HistoricalFeatureExclusions.historicalAlias` (no scope fallback) |
| `HistoricalSpawnRules.profileIdForAlias` | `gameplay/HistoricalSpawnRules.java:524` | **byte-for-byte copy** of `HistoricalProfileLookup.profileIdForAlias` | duplicate #1 |
| `HistoricalSpawnFilter.profileIdForGenerator` | `gameplay/HistoricalSpawnFilter.java:77` | same as `HistoricalProfileLookup.profileIdForGenerator` but uses `HistoricalFeatureExclusions.activeAlias` (adds the `HistoricalGenerationScope` fallback) | duplicate #2 — **semantically different** (the only variant that works for Nether/End) |
| `HistoricalGameplayTweaks.versionOf(Level)` (private) | `gameplay/HistoricalGameplayTweaks.java:203` | `Level` → `instanceof ServerLevel` → `profileIdForLevel` ; returns null for `ClientLevel` | duplicate #3; the "client sees null" trap documented in `HistoricalExperienceClient.java:104-111` |
| `HistoricalGameplayTweaks.isDeclaredVersion / olderThan / olderThanVersion` | `:210,:220,:550` | id → `HistoricalVersions.byId` + chronology compare | |
| `HistoricalActiveSession.current()` | `gameplay/HistoricalActiveSession.java:49` | global → profile id | 5 client + 22 gameplay + 67 mixin + 7 server uses |
| `HistoricalClientRules.activeVersion()` | `client/HistoricalClientRules.java:75` | singleplayer: `server.overworld()` → `profileIdForLevel`; else pushed `syncedVersionId` → `HistoricalVersions.byId` | the client resolver; **duplicated** by `HistoricalConfigScreen.activeVersion()` `:178` (thin delegate) |
| `HistoricalClientRules.applySyncedVersion` | `:51` | selection id → `HistoricalSnapshotPatches.baseProfileForSelection` → session | |
| `HistoricalSnapshotPatches.baseProfileForSelection / byId` | `version/HistoricalSnapshotPatches.java:45,59` | selection id → base profile | |
| `HistoricalSnapshotRuntime.activePatchId / selectionId` | `server/HistoricalSnapshotRuntime.java` | server → patch id | |
| `HistoricalFeatureExclusions.historicalAlias(generator)` / `activeAlias(generator)` | `generation/HistoricalFeatureExclusions.java:892,1117` | generator → raw alias (with/without scope fallback) | alias reader |
| `ModernerBetaAdapter.explicitPresetMarker / markerStateOf / describeGenerator` | `integration/modernerbeta/ModernerBetaAdapter.java:212,770,705` | generator → marker | a **third** generator-alias reader (the adapter one is richer — returns marker *state*) |
| `HistoricalGenerationScope.generationAlias()` | `generation/HistoricalGenerationScope.java` | server-wide alias | |
| `HistoricalWorldState` / `HistoricalMetadata` | `world/HistoricalWorldState.java:103,186` | persisted metadata → profile via `byGenerationPreset` | disk-based resolver |
| `HistoricalVillageRules` / `HistoricalVillageStructure` / `BetaOreRules.java:152` / `BetaOrePlacement:80` / `GenerationUpgrade:203,229` / `FiniteEdgeRepair:115` / `GenerationVerification:727,871` | each calls `byGenerationPreset`/`byRevisionAlias` directly | | 21 direct callers |

### 6.2 Recommended canonical resolvers (single owner: a new `net.historicalexperience.profile.HistoricalContext` or keep `HistoricalProfileLookup` but move it out of `gameplay` into `profile`/`core`)

* **(a) `ServerLevel` → profile/version:** `HistoricalProfileLookup.profileIdForLevel(ServerLevel)` made to use the **scope-aware** alias (`activeAlias`, i.e. the `HistoricalSpawnFilter` variant) so Nether/End are covered, returning the `HistoricalVersion` (not a string) via `HistoricalVersions.byId`. Replace `HistoricalGameplayTweaks.versionOf`, `HistoricalSpawnFilter.profileIdForGenerator`, and delete `HistoricalSpawnRules.profileIdForAlias`. For code with a `Level` that may be a `ClientLevel`, require callers to go through (b).
* **(b) `ClientLevel` / client session:** `HistoricalClientRules.activeVersion()` (singleplayer = integrated server resolver, multiplayer = server-pushed id). Make `HistoricalActiveSession` **private to it** (side-specific holders: `ClientSession`, `ServerSession`) so mixin code that runs on both sides must call a single `HistoricalContext.versionFor(Level)` that does `level.isClientSide ? ClientRules.activeVersion() : Lookup.versionFor((ServerLevel) level)`. This removes the 67 mixin reads of the JVM-global.
* **(c) persisted generation preset alias:** `HistoricalProfiles.byGenerationPreset(String)` (returns the full `GenerationBinding`) is already the one binding table (comment at `HistoricalSpawnRules.java:521` says so). Make it the sole alias→profile function and have `HistoricalVersions.byRevisionAlias` delegate to / be derived from it, so the two registries (`version` and `profile`) cannot drift. `ModernerBetaAdapter` stays the only place that reads the alias *off* a generator; `HistoricalFeatureExclusions.historicalAlias` should call `ModernerBetaAdapter.explicitPresetMarker`.

### 6.3 Naming inconsistencies
* `profile` vs `version` vs `revision` vs `alias` vs `preset` vs `marker`: `profileId` strings are sometimes `HistoricalIdentifier`, sometimes `String` (`HistoricalProfiles.byId` has both overloads); "version id" and "profile id" are the same string value in `HistoricalClientRules.activeVersion()`.
* Two parallel models: `profile.HistoricalProfile(s)` (immutable resolved) vs `version.HistoricalVersion(s)` (registry) with overlapping id sets.
* Class prefixes: almost all classes use `Historical*`, exceptions `ColumnStats`, `EarlyMobDropCapture`, `Early*Verification`, `F4PortalKey`, `SnapshotCreativeKey`, `PortalSpawnRequest`, `SnapshotCreativeRequest`, `IndevMapSettings`, `TexturePlan`, `Pack*`, `Beta*` (`BetaOreRules`, `BetaOrePlacement`, `BetaOreVein`), `Finite*`, `Biome*`.
* `server/` contains network payload/request classes (`HistoricalVersionSync`, `PortalSpawnRequest`, `SnapshotCreativeRequest`), UI-facing service (`HistoricalUpgradeService`), command, lifecycle and 14 verification classes.
* `Verification` / `Probe` / `Acceptance` / `Check` / `Diagnostics` / `Watchdog` used interchangeably for the same role.
* Mixin naming: mixin classes follow `<Target>Historical<Feature>Mixin` except `*Access`, `*Accessor`, `*Invoker`, `InventoryShieldSlotMixin`, `GuiHistoricalExperienceLevelMixin` (feature word `ExperienceLevel` collides with "Experience" in mod name).
* Logging: slf4j (`HistoricalExperienceMod.LOGGER`) vs `java.util.logging` (3 files, e.g. `HistoricalFeatureExclusions.java:908`).
* Mod id `hex1211` (`fabric.mod.json`) vs resource namespace `historical_experience` (all resource paths/payload ids) vs jar `historical-experience-1211`; `depends` uses `moderner_beta`.

---

## 7. Patterns that are good and should be preserved

1. **Generator-sourced truth with scope only as fallback** — `HistoricalFeatureExclusions.historicalAlias` (`:885-913`) reads the alias from the generator's own stored settings ("cannot leak across threads"), and `HistoricalGenerationScope` is documented as "deliberately only a fallback" (`HistoricalGenerationScope.java` class doc). Keep this precedence.
2. **Fail-closed lifecycle with explicit decision type** — `HistoricalMarkerDecision` (pure function of `MarkerEvidence`) separated from side effects in `HistoricalServerLifecycle` (`:143-181`), with LEAVE_ORDINARY writing nothing. Strongly unit-testable (tests in `src/test/.../server`).
3. **Persisted metadata layer isolated** — `world/metadata/*` (schema, store, JSON, exception, revision) with no dependency on `gameplay`/`client`; `world.upgrade` has journal/backup/recovery split (`UpgradeJournal`, `WorldBackup`, `UpgradeRecovery`, `FiniteApronRelease`) that matches the AGENTS.md "backup/journal/crash recovery" contract, with 14 tests.
4. **Append-only generation revisions + binding table** — `HistoricalProfiles.byGenerationPreset` / `GenerationBinding` is the single immutable alias→(profile, revision, foundation) map; revisions never edited (comments `HistoricalProfiles.java:209-271`).
5. **Gate table with evidence** — `HistoricalGameplayTweaks.appliesTo(gate, version)` + `evidenceFor(gate)` + chronology-based `olderThan` (`:214-240`): boundaries are expressed against registry chronology so inserting a version cannot leave a stale boundary. Keep the pattern; split the class, not the idea.
6. **Chronology integer in the registry** — `HistoricalVersions.chronology(era, major, minor, patch)` (`:84`) as the single ordering function.
7. **Bounded integration adapter for Moderner Beta** — one package (`integration/modernerbeta`) with its own exception type; evidence records (`GeneratorFacts`, `AppliedPresetEvidence`) make integration claims testable without a running game (2 tests in `integration/modernerbeta`).
8. **Probe opt-in guard in every probe entry** — e.g. `Early*Verification` start with `if (!Boolean.getBoolean("historical_experience.generationProbe")) throw new IllegalStateException("Probe only")`; `ThreadLocal` capture is scoped and `close()` verifies nesting (`EarlyMobDropCapture.java:19-31`). Worth keeping when the probes are relocated.
9. **Client presentation follows a pushed version, forgotten on disconnect** — `HistoricalVersionSync` payload + `ClientPlayConnectionEvents.DISCONNECT` clears (`HistoricalExperienceClient.java:95-101`). Server owns enforcement.
10. **Declarative-data discipline in tests** — `GenerationBindingTest`, `HistoricalVillageGeneration1211Test`, `ProfileGenerationRules1211Test`, `FiniteApronReleaseTest` (350 lines) and `LicenceComplianceTest`/`verifyLicenceArtifacts` check artefacts, not just source.
11. **Optional Mod Menu dependency** — `modCompileOnly` + `suggests` + `modLocalRuntime` toggle `-PwithoutModMenu` (`build.gradle:55-61`).
12. **Mixin hygiene** — all 145 mixins are registered (no orphans either direction), names carry `Historical`, most gate on a single rule call; accessor/invoker interfaces are separate files.
13. **Licence provenance headers** on ported files + `LicenceComplianceTest` + `verifyLicenceArtifacts` wired into `check`.
14. **Concurrent diagnostics where worldgen threads write** (`ConcurrentHashMap.newKeySet()` in `HistoricalFeatureExclusions` etc.), and `AtomicReference` in `HistoricalSnapshotCreation`.

---

## Prioritised follow-ups
1. Move the 3 always-applied probe mixins and the 4 client-targeting mixins listed in §5.1(3) into the correct config (low risk, high safety).
2. Break `version → gameplay/generation` (§5.1) by moving revision builders out of `HistoricalVersions`.
3. Collapse the three `profileIdFor*` duplicates into one scope-aware resolver (§6.2) and replace `HistoricalActiveSession` reads in mixins with `versionFor(Level)`.
4. Relocate probes to `net.historicalexperience.verification` behind one bootstrap (§2.2), keeping property names; give `villageProbe` and `probe.*` constants a Gradle mapping or delete the unused constants.
5. Delete the DEAD list in §3.2 in small commits.
6. Split `HistoricalVersions`, `HistoricalFeatureExclusions`, `HistoricalGameplayTweaks`, `ModernerBetaAdapter`; convert `HistoricalBlockAvailability` and the exclusion lists to data resources.

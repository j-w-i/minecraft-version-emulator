# Historical compatibility: implementation inventory

Scope: the repository at `C:\Users\jwalr\Minecraft Version Emulator` (Fabric 1.21.1). This document records
what **exists today**, read from the source, the mixin configuration, the datapack resources and the test
suite. It is deliberately an inventory, not a plan: nothing here proposes a change.

Method and limits, stated up front because they bound every claim below:

- Every claim cites `file:line`.
- Version sets are computed from the registry's own chronology scale (`HistoricalVersions.chronology`,
  `version/HistoricalVersions.java:50`) evaluated against the ten declared chronology keys. They were derived
  by hand from the quoted predicates, not by running the code; where a range depends on runtime state rather
  than on the registry the text says so.
- "Counted" numbers (versions, revisions, resources, mixins, test classes) were counted directly from the
  files and from `historical_experience.mixins.json`.

---

## 1. Version registry

Single declaration point: `version/HistoricalVersions.java` (`ALL`, built at `:56-339`). The record types are
`version/HistoricalVersion.java:41-48` (`HistoricalVersion`) and `:171-181` (`VersionRules`); a generation
recipe is `HistoricalVersion.GenerationRevision` at `:106-114`.

**Declared version count: 10.**

| # | id | display name | era | chronology | revisions | ancestry of the declaration |
|---|---|---|---|---|---|---|
| 1 | `historical_experience:infdev_420` | Infdev 20100420 | `infdev` | `ERA_INFDEV + 420` = 300420 | 3 | `HistoricalVersions.java:299-306` |
| 2 | `historical_experience:infdev_611` | Infdev 20100611 | `infdev` | `ERA_INFDEV + 611` = 300611 | 3 | `:307-311` |
| 3 | `historical_experience:alpha` | Alpha v1.1.2_01 | `alpha` | `ERA_ALPHA + 112` = 400112 | 3 | `:312-313` |
| 4 | `historical_experience:beta_1_7_3` | Beta 1.7.3 | `beta` | `chronology(ERA_BETA,1,7,3)` = 501703 | 11 (0–10) | `:59-167` |
| 5 | `historical_experience:beta_1_8_1` | Beta 1.8.1 | `beta` | `chronology(ERA_BETA,1,8,1)` = 501801 | 3 (0–2) | `:169-230` |
| 6 | `historical_experience:beta_1_9_pre_3` | Beta 1.9 Prerelease 3 | `beta` | 501903 | 3 (0–2) | `:247-250` |
| 7 | `historical_experience:release_1_0_0` | Release 1.0 | `release` | `600000+1000` = 601000 | 3 (0–2) | `:251-254` |
| 8 | `historical_experience:release_1_1` | Release 1.1 | `release` | 601100 | 3 (0–2) | `:255-258` |
| 9 | `historical_experience:release_1_2_5` | Release 1.2.5 | `release` | 601205 | 3 (0–2) | `:259-262` |
| 10 | `historical_experience:release_1_6_4` | Release 1.6.4 | `release` | 601604 | 3 (0–2) | `:263-266` |

Era offsets: `ERA_CLASSIC 100_000`, `ERA_INDEV 200_000`, `ERA_INFDEV 300_000`, `ERA_ALPHA 400_000`,
`ERA_BETA 500_000`, `ERA_RELEASE 600_000` (`:37-42`). The formula is
`eraOffset + major*1000 + minor*100 + patch` (`:50-52`); Infdev entries bypass it and pass the build number
directly (`:421`).

Note on notation: `ERA_CLASSIC` (100 000) and `ERA_INDEV` (200 000) are declared but **no version uses
them**, because the finite Classic/Indev worlds were withdrawn (`:315-337`).

**Generation revisions total: 38.** Per version: 11 + 3 + 3 + 3 + 3 + 3 + 3 + 3 + 3 + 3 = 38. Each revision
declares four things that matter (`HistoricalVersion.java:106-114`): a numeric `revision`, a project-owned
`alias` (the persisted recipe identifier), a `foundation` (the real upstream Moderner Beta preset the alias
inherits terrain from), and an `experimental` flag. The constructor enforces that revision 0 is not
experimental and every numbered revision is (`:128-134`).

Three construction helpers produce the shapes:

- Beta 1.7.3 is written out literally, revisions 0–10, each with its own description
  (`HistoricalVersions.java:81-167`).
- Beta 1.8.1 is written out literally, revisions 0–2 (`:197-230`).
- The six terrain-only versions (Beta 1.9 Prerelease 3, Release 1.0/1.1/1.2.5/1.6.4) come from
  `terrainOnly(...)` (`:559-621`), which emits revision 0 (frozen), revision 1 (`_cave_fix`) and revision 2
  (`_no_modern_content`).
- The two Infdev builds and Alpha come from `terrainOnlyWithRules(...)` (`:496-549`) and
  `infdevTerrainOnly(...)` (`:392-432`) / `alphaTerrainOnly(...)` (`:444-472`), which emit revision 0,
  revision 1 (`_no_grass`) and revision 2 (`_no_modern_content`, with the monster-room exclusion chosen by
  chronology at `:545-547`).

Two invariants are enforced in the static initialiser (`:341-359`): no two versions may share a profile id,
and no two revisions may share a recipe alias.

### Simulation rule sets (`VersionRules`)

| version | restrictsSpawns | awardsExperience | allowsSprinting | hasHunger | hasStructures | foodHealPoints | mobDrops declared |
|---|---|---|---|---|---|---|---|
| infdev_420 | yes, 7 mobs | no | no | no | no | none | none |
| infdev_611 | yes, 7 mobs | no | no | no | no | none | none |
| alpha | yes, 10 mobs | no | no | no | no | 6 items | none |
| beta_1_7_3 | yes, 14 mobs | no | no | no | no | 9 items | 4 entities |
| beta_1_8_1 | yes, 17 mobs | yes | yes | yes | yes | none (deliberate) | 4 entities |
| beta_1_9_pre_3 | no | yes | yes | yes | yes | none | none |
| release_1_0_0 | no | yes | yes | yes | yes | none | none |
| release_1_1 | no | yes | yes | yes | yes | none | none |
| release_1_2_5 | no | yes | yes | yes | yes | none | none |
| release_1_6_4 | no | yes | yes | yes | yes | none | none |

Sources: Infdev rules `HistoricalVersions.java:392-432`; Alpha rules `:444-472`; Beta 1.7.3 rules `:64-80`;
Beta 1.8.1 rules `:174-196`; terrain-only versions use `HistoricalVersion.unmodified(evidence)`
(`HistoricalVersion.java:211-213`, called at `HistoricalVersions.java:580`), which is
`(false, Set.of(), Map.of(), Map.of(), true, true, true, true, evidence)`.

The `VersionRules` constructor refuses two contradictory shapes: a non-positive food heal (`:186-193`) and a
rule set that does not restrict spawning while naming allowed spawns (`:194-198`).

### Catalogue (planned matrix, separate from the registry)

`version/HistoricalVersionCatalogue.java` is a *menu* description, not the registry (`:10-36`). It declares
**4 eras** (`:39-43`) and merges implemented entries with not-yet-implemented ones.

- **29 planned targets**: 8 Alpha, 14 Beta, 7 Release (`:217-250`; asserted at
  `HistoricalVersionCatalogueTest.java:31-38`).
- **3 implemented extras** that the plan does not name: `infdev_420`, `infdev_611`, `alpha` (`:207-213`;
  asserted at `HistoricalVersionCatalogueTest.java:51-53`).
- Total rows: 32. Implemented rows resolve to registry versions; future rows are numbered
  `historical_experience:planned_…` (`:283-293`) and are never selectable (`:114-116`).

---

## 2. Behaviour gates

This is the largest section, because the project gates behaviour in four distinct ways and only one of them
is the named gate table. All four are reported, since "implemented under a different name or through a shared
abstraction" is exactly the failure mode the brief warns about.

`HistoricalActiveSession.current()` (`gameplay/HistoricalActiveSession.java:35-37`) is a process-wide volatile
version id, set on the server at player join (`server/HistoricalVersionSync.java:66`) and on the client when
the server's payload arrives (`client/HistoricalClientRules.java:34-39`). Most gate sites read it rather than
a level, because a client's level is a `ClientLevel` and a level-based question answers `null` there — a
defect that broke sprinting and sword blocking twice (`gameplay/HistoricalGameplayTweaks.java:304-314`).

### 2a. The named gate table: `HistoricalGameplayTweaks.appliesTo`

The single boundary switch is `HistoricalGameplayTweaks.appliesTo(String gate, String versionId)`
(`gameplay/HistoricalGameplayTweaks.java:141-164`). Verbatim:

```java
case "skeletonStrafing", "criticalHit", "panic" ->
    versionId != null && BEFORE_BETA_1_8.contains(versionId);
case "avoidSun" -> isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_2_5);
case "customDeathLoot" ->
    isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_2_5);
case "breeding", "tempting", "babyAnimal" ->
    isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3);
case "itemPickup", "monsterGear", "monsterEnchant", "babyZombie" ->
    isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_6_4);
case "cooldown", "sweep", "swim" -> isDeclaredVersion(versionId);
case "mobArms" -> isDeclaredVersion(versionId);
default -> throw new IllegalArgumentException(...);
```

Supporting definitions:
`BEFORE_BETA_1_8 = {infdev_420, infdev_611, alpha, beta_1_7_3}` (`:42-46`);
`RELEASE_1_2_5 = "historical_experience:release_1_2_5"` (`:173`);
`RELEASE_1_6_4 = "historical_experience:release_1_6_4"` (`:174`);
`BETA_1_9_PRE_3 = "historical_experience:beta_1_9_pre_3"` (`:175`);
`isDeclaredVersion(id) = id != null && HistoricalVersions.byId(id).isPresent()` (`:109-111`);
`olderThan(a,b) = self != null && other != null && self.chronology() < other.chronology()` (`:119-123`).

`gateNames()` (`:167-171`) is the closed list of **16 gates**: `avoidSun, babyAnimal, babyZombie, breeding,
cooldown, criticalHit, customDeathLoot, itemPickup, mobArms, monsterEnchant, monsterGear, panic,
skeletonStrafing, sweep, swim, tempting`. An unknown gate name throws rather than answering false (`:161-162`).

Resolved to the ten declared versions (using the chronology column from §1):

| gate | evaluates true for | evaluates false for | implementing site(s) |
|---|---|---|---|
| `skeletonStrafing` | infdev_420, infdev_611, alpha, beta_1_7_3 | beta_1_8_1 … release_1_6_4 | `mixin/RangedBowAttackGoalHistoricalMixin.java:28-36` |
| `criticalHit` | infdev_420, infdev_611, alpha, beta_1_7_3 | beta_1_8_1 … release_1_6_4 | `mixin/PlayerHistoricalCombatMixin.java:64-70` |
| `panic` | infdev_420, infdev_611, alpha, beta_1_7_3 | beta_1_8_1 … release_1_6_4 | `mixin/PanicGoalHistoricalMixin.java:30-37` |
| `avoidSun` | infdev_420, infdev_611, alpha, beta_1_7_3, beta_1_8_1, beta_1_9_pre_3, release_1_0_0, release_1_1 | release_1_2_5, release_1_6_4 | `mixin/FleeSunGoalHistoricalMixin.java:23-29` |
| `customDeathLoot` | same 8 as `avoidSun` | release_1_2_5, release_1_6_4 | `mixin/MobHistoricalCustomLootMixin.java:37-49` |
| `breeding` | infdev_420, infdev_611, alpha, beta_1_7_3, beta_1_8_1 | beta_1_9_pre_3 … release_1_6_4 | `mixin/AnimalHistoricalBreedingMixin.java:27-38` |
| `tempting` | same 5 as `breeding` | beta_1_9_pre_3 … release_1_6_4 | `mixin/TemptGoalHistoricalMixin.java:30-37` |
| `babyAnimal` | same 5 as `breeding` | beta_1_9_pre_3 … release_1_6_4 | `mixin/AgeableMobHistoricalSpawnMixin.java:25-33` |
| `itemPickup` | infdev_420, infdev_611, alpha, beta_1_7_3, beta_1_8_1, beta_1_9_pre_3, release_1_0_0, release_1_1, release_1_2_5 | release_1_6_4 | `mixin/MobHistoricalItemPickupMixin.java:35-46` |
| `monsterGear` | same 9 as `itemPickup` | release_1_6_4 | `mixin/MobHistoricalEquipmentMixin.java:44-57` |
| `monsterEnchant` | same 9 as `itemPickup` | release_1_6_4 | `mixin/MobHistoricalEquipmentMixin.java:60-89` |
| `babyZombie` | same 9 as `itemPickup` | release_1_6_4 | `mixin/ZombieHistoricalSpawnMixin.java:32-42` |
| `cooldown` | **all 10** | — | `mixin/PlayerHistoricalCombatMixin.java:44-61`; `mixin/GuiHistoricalAttackIndicatorMixin.java:37-55` |
| `sweep` | **all 10** | — | `mixin/PlayerHistoricalCombatMixin.java:72-84`; `mixin/ParticleEngineHistoricalSweepMixin.java:37-66`; `mixin/ClientLevelHistoricalAttackSoundMixin.java:41-57` |
| `swim` | **all 10** | — | `mixin/LivingEntityHistoricalMovementMixin.java:41,52-54` |
| `mobArms` | **all 10** | — | `mixin/AbstractZombieModelHistoricalArmsMixin.java:38-65`; `mixin/AnimationUtilsHistoricalArmBobMixin.java:34-46` |

Gates that carry a recorded reason: `EVIDENCE` (`:48-93`) has **12 entries** —
`avoidSun, babyZombie, breeding, criticalHit, customDeathLoot, gearedMobs, mobArms, monsterEnchant,
monsterPickup, panic, skeletonStrafing`. Only **11** of those keys are gate names: `gearedMobs` and
`monsterPickup` are the prose names for the `monsterGear` and `itemPickup` gates, so the map's keys and
`gateNames()` are not the same vocabulary. `cooldown`, `sweep`, `swim` and `tempting` therefore have no
`EVIDENCE` entry at all — see §8.2(j).

### 2b. Version-gated predicates *outside* the table

These are gates by any reasonable reading, but they are **not** in `gateNames()` and are not walked by
`HistoricalGameplayTweaksTest`. All quoted verbatim.

| behaviour | class:line | predicate (verbatim) | true for |
|---|---|---|---|
| sword blocking stance ("can a player raise a sword") | `HistoricalGameplayTweaks.java:311-314`, used at `gameplay/HistoricalSwordBlocking.java:50` | `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId)` | beta_1_8_1 … release_1_6_4 (6) |
| sword block's fall-damage reduction | `HistoricalGameplayTweaks.java:444-446`, used at `mixin/LivingEntityHistoricalSwordBlockingMixin.java:75-85` | `swordBlockingEnabled(versionId) && olderThan(versionId, BETA_1_9_PRE_3)` | **beta_1_8_1 only** |
| pre-1.0.0 sword damage (+3.0 on the modifier) | `HistoricalGameplayTweaks.java:440-442`, used at `gameplay/HistoricalDamageValues.java:135-140` | `isDeclaredVersion(versionId) && olderThan(versionId, BETA_1_9_PRE_3)` | infdev_420, infdev_611, alpha, beta_1_7_3, beta_1_8_1 (5) |
| Beta exhaustion costs (attack 0.3, jump 0.2) | `HistoricalGameplayTweaks.java:403-405`, used at `mixin/PlayerHistoricalExhaustionMixin.java:34-43,53-55` | `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId)` | beta_1_8_1 … release_1_6_4 (6) |
| Beta sprint-jump exhaustion (0.8) | `HistoricalGameplayTweaks.java:414-416`, used at `mixin/PlayerHistoricalExhaustionMixin.java:46-51` | `betaExhaustionCosts(versionId) && olderThan(versionId, RELEASE_1_6_4)` | beta_1_8_1, beta_1_9_pre_3, release_1_0_0, release_1_1, release_1_2_5 (5) |
| Beta golden apple (10 hunger, no Regeneration) | `HistoricalGameplayTweaks.java:418-422`, used at `mixin/DataComponentHolderHistoricalFoodMixin.java:46-49` | `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId) && olderThan(versionId, RELEASE_1_2_5)` | beta_1_8_1, beta_1_9_pre_3, release_1_0_0, release_1_1 (4) |
| pre-change cookie/stew hunger values | `HistoricalGameplayTweaks.java:424-428`, used at `mixin/DataComponentHolderHistoricalFoodMixin.java:52-65` | `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId) && olderThan(versionId, RELEASE_1_6_4)` | beta_1_8_1 … release_1_2_5 (5) |
| Beta 1.8 regeneration model (no fast branch, no exhaustion) | `HistoricalGameplayTweaks.java:430-432`, used at `mixin/FoodDataHistoricalRegenerationMixin.java:33-56` | `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId)` | beta_1_8_1 … release_1_6_4 (6) |
| Beta critical hit formula (random 1.0–2.0 × instead of flat 1.5) | `HistoricalGameplayTweaks.java:434-438`, used at `mixin/PlayerHistoricalCriticalHitMixin.java:40-49` | `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId) && olderThan(versionId, RELEASE_1_6_4)` | beta_1_8_1 … release_1_2_5 (5) |
| ores drop the ore block, not a raw metal | `HistoricalGameplayTweaks.java:384-386`, used at `mixin/BlockBehaviourHistoricalDropMixin.java:41` | `return isDeclaredVersion(HistoricalActiveSession.current());` | **all 10** (also asserted for all 10 at `OreSelfDropTest.java:35-42`) |
| experience orbs refused entry to the world | `HistoricalGameplayTweaks.java:454-462`, used at `mixin/ServerLevelHistoricalOrbMixin.java:45-49` | `HistoricalVersions.byId(versionId).map(v -> !v.rules().awardsExperience()).orElse(false)` | infdev_420, infdev_611, alpha, beta_1_7_3 (4) |
| "always open chest" (upstream `ALWAYS_OPEN_CHEST`) | `HistoricalGameplayTweaks.java:264-266` | `return isDeclaredWorld(level);` | all 10 — **but the method has no caller anywhere** (§8) |

### 2c. Registry-flag gates (a version's `VersionRules` read through a helper)

These are gates whose predicate is a *field* rather than a comparison.

| behaviour | class:line | predicate | true for |
|---|---|---|---|
| sprinting allowed | `gameplay/HistoricalPlayerRules.java:41-45` | `version.rules().allowsSprinting()`, `.orElse(true)` for unknown | false for infdev_420, infdev_611, alpha, beta_1_7_3; true for the other 6; true for unknown |
| hunger system present | `HistoricalPlayerRules.java:48-52` | `version.rules().hasHunger()`, `.orElse(true)` | false for the first 4; true for the other 6 |
| hunger tick suppressed | `HistoricalPlayerRules.java:62-64` | `!hasHunger(versionId)` | true for infdev_420, infdev_611, alpha, beta_1_7_3 |
| structures generated | `HistoricalPlayerRules.java:55-59` | `version.rules().hasStructures()`, `.orElse(true)` | false for the first 4; true for the other 6 |
| direct food healing applies | `gameplay/HistoricalFoodRules.java:163-165` | `!hasHunger(versionId) && !healPoints(versionId).isEmpty()` | beta_1_7_3, alpha only |
| eating ungated by a food level | `HistoricalFoodRules.java:173-175` | `!hasHunger(versionId)` | the first 4 |
| food eaten instantly, eating particles suppressed | `gameplay/HistoricalFoodHelper.java:95-103`, used at `mixin/LivingEntityHistoricalInstantEatMixin.java:44-82` and `mixin/MinecraftHistoricalInstantEatMixin.java:39-58` | `session != null && HistoricalPlayerRules.suppressesHungerTick(session)` then `isEdible(stack)` | the first 4 |
| natural spawn allow-list applied | `gameplay/HistoricalSpawnFilter.java:54-104` via `HistoricalSpawnRules.forProfile` (`gameplay/HistoricalSpawnRules.java:183-188`) | `version.rules().restrictsSpawns()` then membership of `allowedNaturalSpawns` | infdev_420/611 (7 mobs each), alpha (10), beta_1_7_3 (14), beta_1_8_1 (17) |
| mob death drops substituted | `gameplay/HistoricalLootRules.java:192-199` via `forEntity` (`:130-133`) | `profileId != null && forEntity(profileId, entityTypeId).isPresent()` | beta_1_7_3 (4 entities), beta_1_8_1 (4 entities) |
| mob experience withheld on death | `HistoricalLootRules.java:123-127`, used at `mixin/LivingEntityHistoricalLootMixin.java:60-71` | `version.rules().awardsExperience()` negated | the first 4 |
| mob drops replaced with era loot tables | `gameplay/HistoricalMobLootHelper.java:150-178` | `HistoricalLootRules.hasOldDrops(entity.level(), entityTypeId)` | gated to the two versions above; **the helper's own table covers 14 entity types, 6 of which exist in no declared version** (`:55-57`, `:130-143`) |
| Hunger status effect refused | `mixin/LivingEntityHistoricalHungerEffectMixin.java:35-47` | `session != null && HistoricalPlayerRules.suppressesHungerTick(session)` | the first 4 |

### 2d. Structure and block gates (generation-time, alias-keyed)

`generation/HistoricalStructureRules.suppresses(alias, structureId)` (`:221-247`) resolves the persisted alias
to its owning version and applies an **allow-list**: anything not named is suppressed
(`StructureRule.allows`, `:86-88`). Two rules run before the allow-list: modern jigsaw villages are suppressed
in favour of the project's own structure (`:232-239`), and an alias that is not a declared recipe is not
suppressed at all (`:221-224`).

| version | allowed structure ids | source |
|---|---|---|
| infdev_420 | *(none)* | `:144-145` |
| infdev_611 | *(none)* | `:146-148` |
| alpha | *(none)* | `:149-150` |
| beta_1_7_3 | *(none)* | `:151-152` |
| beta_1_8_1 | `minecraft:mineshaft`, `minecraft:stronghold`, `historical_experience:historical_village` | `:126-127`, `:153-159` |
| beta_1_9_pre_3 | the above + `minecraft:fortress` | `:160-165` |
| release_1_0_0 | the above | `:166-170` |
| release_1_1 | the above | `:171-173` |
| release_1_2_5 | the above | `:174-179` |
| release_1_6_4 | the above + `minecraft:desert_pyramid`, `minecraft:jungle_pyramid`, `minecraft:swamp_hut` | `:129-131`, `:180-188` |

`VANILLA_STRUCTURE_IDS` (`:92-104`) pins the 34 structures the pinned 1.21.1 jar registers, so an
unclassified one is a visible failure.

Block-level gate: `generation/HistoricalBlockContent.isAnachronistic(versionId, blockId)` (`:44-46`) asks the
per-version availability table first (`generation/HistoricalBlockAvailability.judge`) and falls back to the
version-agnostic list (`generation/HistoricalBlockAnachronisms.introducedIn`). Its outcomes are
`ALLOWED` / `ANACHRONISTIC` / `UNKNOWN`, and `UNKNOWN` is *never* treated as acceptable (`:30-41`). It is
consumed by `generation/HistoricalFeatureExclusions.isAnachronisticForActiveVersion` (`:582-603`), which is
what the tree-decorator suppression hook asks (`mixin/TreeDecoratorContextHistoricalSuppressionMixin.java:54-64`).

Village gates (`worldgen/HistoricalVillageRules.java`):
`generationEnabledForVersion` = `version.chronology() >= BETA_1_8_1_CHRONOLOGY` (`:154-159`), true for
beta_1_8_1 … release_1_6_4; `generationEnabledForAlias` = the same through the registry, `false` for a
non-historical alias (`:169-176`); `suppressVanillaVillagesForAlias` delegates to it (`:187-189`);
`biomeIsPermitted` is set membership in `{plains, sunflower_plains, desert}` (`:96-122`). The structure itself
re-checks both directions at `worldgen/HistoricalVillageStructure.java:101-104` and `:120-125`.

Per-version village block substitutions, from `worldgen/HistoricalVillageBlocks.java`:
`brewingStand = chronology >= BETA_1_9_PRE_3_CHRONOLOGY` (`:233`, `:241-242`);
`carrotsAndPotatoes = chronology >= release_1_6_4` (`:225-227`, `:244-247`) — release_1_6_4 only;
`sandstoneStairs = chronology >= release_1_6_4` (`:232`, `:236-238`) — release_1_6_4 only, cobblestone stairs
before it; `usesDirtPath` compares against a Release 1.9 chronology that **no declared version reaches**, so it
is false for all ten (`:258-264`).

---

## 3. Mixins by subsystem

Configuration: `src/main/resources/historical_experience.mixins.json`. Package
`net.historicalexperience.mixin`, `compatibilityLevel JAVA_21`, `required: true`,
`injectors.defaultRequire: 1`. **35 mixins in the shared `mixins` array** (`:5-41`) and **12 in the `client`
array** (`:45-58`) — 47 total. There is no `server` array. Every one of the 47 files exists in
`src/main/java/net/historicalexperience/mixin/`, and no other file in that directory exists, which
`HistoricalLootWiring1211Test.mixinRegistrationMatchesTheSources` (`:48-82`) enforces in both directions.

"Client-only" below means registered under `client`.

### Player physics / movement

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `LivingEntityHistoricalMovementMixin` | `LivingEntity` | `setSprinting` (`@ModifyVariable argsOnly`) | sets the sprint argument false in versions without sprinting, and the swim argument false in versions without 1.13 swimming; creative and spectator are exempt (`:37-62`) |

### Combat

| file | target | vanilla method hooked | what it changes | client-only |
|---|---|---|---|---|
| `PlayerHistoricalCombatMixin` | `Player` | `attack` (HEAD inject), `getAttackStrengthScale`, `attack` × 2 (`@ModifyExpressionValue` on `onClimbable` and `Level.getEntitiesOfClass`) | resets the attack-strength ticker and pins the scale to 1.0 (no 1.9 cooldown); forces the critical-hit branch on; empties the sweep target list (`:44-84`) | no |
| `PlayerHistoricalCriticalHitMixin` | `Player` | `attack` (`@ModifyConstant 1.5F`) | replaces the flat ×1.5 critical multiplier with the Beta 1.8 random 1.0–2.0 (`:40-50`) | no |
| `LivingEntityHistoricalSwordBlockingMixin` | `LivingEntity` | `startUsingItem`, `calculateFallDamage` (`@ModifyVariable ordinal 1`), `hurt` | gives a blockable sword a 72 000-tick use duration, halves blocked fall damage in the narrow window, halves incoming damage while blocking, and deliberately charges **no** durability (`:55-102`) | no |
| `DataComponentHolderHistoricalDamageMixin` | `DataComponentHolder` | `getOrDefault` (`@ModifyReturnValue`) | rewrites the `ATTRIBUTE_MODIFIERS` component to the pre-1.9 tiered-item damage (`:39-56`) | no |
| `GuiHistoricalAttackIndicatorMixin` | `Gui` | `renderCrosshair` (`@ModifyExpressionValue` on `OptionInstance.get`) | forces the crosshair attack indicator to `OFF` (`:37-55`) | **yes** |
| `ParticleEngineHistoricalSweepMixin` | `ParticleEngine` | `createParticle` (`@ModifyExpressionValue` on `makeParticle`) | returns `null` for `SWEEP_ATTACK`, `DAMAGE_INDICATOR` and `ENCHANTED_HIT`; deliberately **not** `CRIT` (`:37-66`) | **yes** |
| `ClientLevelHistoricalAttackSoundMixin` | `ClientLevel` | `playSound(DDD…J)V` (HEAD, cancellable) | cancels the six 1.9 attack sound variants (`:41-67`) | **yes** |
| `MobHistoricalCustomLootMixin` | `Mob` | `dropCustomDeathLoot` (`@WrapWithCondition` on `spawnAtLocation`) | refuses a skeleton's bow drop before Release 1.2.1 (`:37-49`) | no |

### Hunger / food

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `FoodDataHistoricalHungerMixin` | `FoodData` | `eat(IF)V` (HEAD), `tick` (HEAD) | remembers the food-level modifier and heals it directly; zeroes exhaustion and the tick timer every tick in a hungerless version (`:37-59`) |
| `FoodDataHistoricalRegenerationMixin` | `FoodData` | `tick` (`@ModifyConstant 20`), `tick` (`@ModifyArg` on `FoodData.addExhaustion` ordinal 1) | makes the fast regeneration branch unreachable and zeroes the slow branch's exhaustion charge (`:33-56`) |
| `PlayerHistoricalFoodMixin` | `Player` | `eat` (HEAD inject), `canEat` (`@ModifyReturnValue`) | heals directly from `HistoricalFoodHelper.directHealAmount`, and un-gates eating to "health below maximum" (`:46-69`) |
| `DataComponentHolderHistoricalFoodMixin` | `DataComponentHolder` | `getOrDefault` (`@ModifyReturnValue`) | reconstructs the `FOOD` component: golden apple at 10 hunger with an empty effect list, cookie/stew at their pre-change nutrition (`:34-66`) |
| `LivingEntityHistoricalInstantEatMixin` | `LivingEntity` | `startUsingItem` (`@ModifyExpressionValue` on `getUseDuration`), `updateUsingItem` and `completeUsingItem` (`@WrapWithCondition` on `triggerItemUseEffects`) | forces a 1-tick use duration and suppresses the eating particle burst (`:44-82`) |
| `PlayerHistoricalExhaustionMixin` | `Player` | `attack` (`@ModifyConstant 0.1F`), `jumpFromGround` (`@ModifyConstant 0.05F` and `0.2F`) | restores Beta 1.8's exhaustion costs and the era-dependent sprint-jump cost (`:34-55`) |
| `LivingEntityHistoricalHungerEffectMixin` | `LivingEntity` | `canBeAffected` (`@ModifyReturnValue`) | refuses the Hunger effect in a version with no hunger meter (`:35-47`) |
| `MinecraftHistoricalInstantEatMixin` | `Minecraft` | `startUseItem` (`@WrapWithCondition` on `ItemInHandRenderer.itemUsed`) | suppresses the held-item equip animation after an instant meal (`:39-58`) — **client-only** |

### Experience

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `ServerLevelHistoricalOrbMixin` | `ServerLevel` | `addEntity` (`@WrapOperation` on `PersistentEntitySectionManager.addNewEntity`) | refuses an `ExperienceOrb` at the point the level admits entities, which catches every creation path (`:37-51`) |
| `LivingEntityHistoricalLootMixin` | `LivingEntity` | `dropFromLootTable` (`@ModifyExpressionValue` on `ReloadableServerRegistries$Holder.getLootTable`), `dropExperience` (HEAD, cancellable) | substitutes the era loot table and withholds experience (`:41-71`) |

### Mobs / AI

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `RangedBowAttackGoalHistoricalMixin` | `RangedBowAttackGoal` | `tick` (`@Redirect` on `MoveControl.strafe`) | stops a skeleton sidestepping while shooting; it still aims and fires (`:28-36`) |
| `FleeSunGoalHistoricalMixin` | `FleeSunGoal` | `canUse` (`@ModifyReturnValue`) | the goal never becomes usable (`:23-29`) |
| `PanicGoalHistoricalMixin` | `PanicGoal` | `shouldPanic` (`@ModifyReturnValue`) | no panic for `Animal` instances only (`:30-37`) |
| `TemptGoalHistoricalMixin` | `TemptGoal` | `shouldFollow` (`@ModifyReturnValue`) | no following for `AgeableMob` instances only (`:29-37`) |
| `MobHistoricalItemPickupMixin` | `Mob` | `aiStep` (`@WrapWithCondition` on `pickUpItem`) | a `Monster` cannot pick items up (`:35-46`) |
| `AnimalHistoricalBreedingMixin` | `Animal` | `mobInteract` (`@Redirect` on `Animal.isFood`) | breeding food is answered "not food" only at the breeding call site (`:27-38`) |
| `MobHistoricalEquipmentMixin` | `Mob` | `populateDefaultEquipmentEnchantments` (HEAD inject + two `@WrapWithCondition` on `enchantSpawnedWeapon` / `enchantSpawnedArmor`) | clears armour slots and refuses enchantments for `Monster` only (`:44-89`) |
| `ZombieHistoricalSpawnMixin` | `Zombie` | `getSpawnAsBabyOdds` (`@ModifyExpressionValue` on `RandomSource.nextFloat`) | returns `1.0F`, so the baby test can never pass (`:32-42`) |
| `AgeableMobHistoricalSpawnMixin` | `AgeableMob` | `finalizeSpawn` (`@ModifyExpressionValue` on `AgeableMobGroupData.isShouldSpawnBaby`) | no baby animal from natural spawning (`:25-33`) |
| `AbstractZombieModelHistoricalArmsMixin` | `AbstractZombieModel` | `setupAnim` (`@ModifyArg` index 2 and 3 on `AnimationUtils.animateZombieArms`) | `isAggressive=false`, `attackTime=0` — arms stay outstretched (`:38-65`) — **client-only** |
| `AnimationUtilsHistoricalArmBobMixin` | `AnimationUtils` | `animateZombieArms` (inject before `bobArms`) | freezes the idle arm bob via `MobArmHelper.applyStaticArms` (`:34-46`) — **client-only** |

### Spawning

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `NaturalSpawnerHistoricalSpawnMixin` | `NaturalSpawner` | `mobsAt` (RETURN inject, cancellable), `spawnMobsForChunkGeneration` (`@Redirect` on `MobSpawnSettings.getMobs`) | filters the candidate spawn list in both the live and the chunk-generation path through the shared `HistoricalSpawnFilter` (`:26-43`) |

### Loot

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `LivingEntityHistoricalLootMixin` | `LivingEntity` | see §Experience — the loot-table substitution is the same hook | tables come from `HistoricalMobLootHelper`, which builds real `LootTable`s so Looting and on-fire smelting keep working (`gameplay/HistoricalMobLootHelper.java:150-178`) |
| `MobHistoricalCustomLootMixin` | `Mob` | see §Combat | skeleton bow drop |
| `SheepAccess` | `Sheep` | `@Accessor("ITEM_BY_DYE")` | exposes the dye-to-wool map the sheared-sheep case needs (`:21-25`) |

### Blocks / items

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `BlockBehaviourHistoricalDropMixin` | `BlockBehaviour` | `getDrops` (`@ModifyReturnValue`, RETURN ordinal 1) | clears the drop list and adds the block's own item for six ore blocks (`:39-61`) — see the §8 finding about ore loot tables |
| `DataComponentHolderHistoricalDamageMixin` | `DataComponentHolder` | see §Combat | |
| `DataComponentHolderHistoricalFoodMixin` | `DataComponentHolder` | see §Hunger / food | |

### World / level

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `FeaturePlacerHistoricalContentMixin` | `PlacedFeature` | `placeWithBiomeCheck` (HEAD, cancellable) | refuses an excluded placed feature before it places anything (`:35-46`) |
| `ChunkGeneratorHistoricalOreMixin` | `ChunkGenerator` | `applyBiomeDecoration` (RETURN inject) | runs the revision's own ore population after vanilla decoration (`:31-39`) |
| `TreeDecoratorContextHistoricalSuppressionMixin` | `TreeDecorator.Context` | `setBlock` (HEAD, cancellable) | refuses an anachronistic block at the decorator write (`:54-64`) |
| `StructureHistoricalSuppressionMixin` | `Structure` | `generate` (HEAD, cancellable) | returns `StructureStart.INVALID_START` for a suppressed structure (`:54-81`) |
| `LevelStorageHistoricalUpgradeRecoveryMixin` | `LevelStorageSource.LevelStorageAccess` | `getDataTag(Z)` (HEAD inject) | recovers an interrupted upgrade under the acquired world lock, **before** level.dat is decoded into loaded generators (`:23-41`) |

### Client HUD

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `GuiHistoricalHudMixin` | `Gui` | `isExperienceBarVisible`, `renderHotbarAndDecorations` × 3, `renderPlayerHealth` × 8 | hides the hunger bar (by translating it off-screen, not by cancelling the draw) and the experience bar, and moves the armour and air icons into the hunger bar's space (`:34-212`) |
| `GuiAccess` | `Gui` | `@Accessor` × 6 | exposes the vanilla armour/air sprites and `displayHealth` (`:18-45`) |

### Client rendering

| file | target | vanilla method hooked | what it changes |
|---|---|---|---|
| `HumanoidModelHistoricalBlockingMixin` | `HumanoidModel` | `setupAnim(LivingEntity;FFFFF)V` (inject at `setupAttackAnimation`) | lowers the raised arm in third person (`:34-45`) |
| `ItemInHandRendererHistoricalBlockingMixin` | `ItemInHandRenderer` | `renderArmWithItem` (two injects around `renderItem`, plus `@ModifyExpressionValue` on `isUsingItem`) | pushes/pops the block pose in first person and hides the vanilla "using item" pose (`:50-103`) |
| `MinecraftHistoricalSwordBlockingMixin` | `Minecraft` | `startUseItem` (two `@WrapWithCondition`) | no swing and no re-equip animation when raising a block (`:45-80`) |
| `HumanoidArmorLayerHistoricalTintMixin` | `HumanoidArmorLayer` | `renderArmorPiece` (HEAD), `renderModel` (`@ModifyExpressionValue` on `MultiBufferSource.getBuffer`, `@ModifyArg` index 3) | applies the red hurt tint to armour (`:41-71`) |
| `AbstractZombieModelHistoricalArmsMixin` | see §Mobs / AI | | |
| `AnimationUtilsHistoricalArmBobMixin` | see §Mobs / AI | | |
| `MinecraftHistoricalInstantEatMixin` | see §Hunger / food | | |
| `ParticleEngineHistoricalSweepMixin` | see §Combat | | |
| `ClientLevelHistoricalAttackSoundMixin` | see §Combat | | |

### Other

| file | target | vanilla method hooked | what it changes | client-only |
|---|---|---|---|---|
| `LivingEntityEquipmentInvoker` | `LivingEntity` | `@Invoker("collectEquipmentChanges")` | lets the equipment pass be re-run once the version is known (`:22-26`) | no |
| `LivingEntityAttackTickerAccess` | `LivingEntity` | `@Accessor("attackStrengthTicker")` | lets the cooldown hook reset the ticker (`:15-18`) | no |
| `WorldListEntryAccessor` | `WorldSelectionList.WorldListEntry` | `@Accessor("summary")` | exposes the `LevelSummary` so the version manager resolves the save **folder** rather than the display name (`:21-25`) | **yes** |

---

## 4. Gameplay helpers and data tables

| class | what it holds | keyed by | applies to |
|---|---|---|---|
| `gameplay/HistoricalFoodRules` | Two direct-heal tables. `BETA_1_7_3` (`:84-100`) is 10 `FoodHeal` records with per-entry evidence (client class + initialiser), of which **9 are applied** — cake is recorded and excluded (`:102-105`, `UNEXCLUDED` reason at `:112-113`). `ALPHA_1_1_2` (`:134-147`) is 6 records, each proved from that jar's own recipe/drop/smelting table. `BETA_1_7_3_HEAL_POINTS` and `ALPHA_1_1_2_HEAL_POINTS` are the id-keyed projections the registry actually uses (`:108-110`, `:150-151`). | item id, inside a version-keyed table | beta_1_7_3 (9 items) and alpha (6 items) — `healingProfiles()` derives the set at `:194-199` |
| `gameplay/HistoricalFoodHelper` | A **second, different** heal table: `DIRECT_HEAL_BY_ID` with 6 entries (`:37-43`) plus `GOLDEN_APPLE_BETA_HUNGER = 10` (`:61`). Also `BETA_HUNGER` for cookie (1) and mushroom stew (8) (`:51-53`). | item id | any hungerless version, via `isInstantaneousEdible` (`:95-103`); the heal value is read at `:87-92`. **This table disagrees with `HistoricalFoodRules`** — see §8 |
| `gameplay/HistoricalLootRules` | `BETA_1_7_3` (`:59-68`): 4 `DropRule`s (zombie→feather, chicken→feather, spider→string, cow→leather), each with `maxExclusiveCount = 3` and a citation naming the client field. `BETA_1_8_1` (`:78-93`): 4 rules, **all explicitly PROVISIONAL**. Keyed by entity type id inside a version-keyed map (`:96-103`). Also `suppressesExperience` (`:123-127`). | entity type id | beta_1_7_3 and beta_1_8_1 only |
| `gameplay/HistoricalMobLootHelper` | 14 entity→`LootTable` mappings (`:130-143`), plus the sheared-sheep special case (`:168-175`). Builds real tables with `SetItemCountFunction`, `EnchantCountIncreaseFunction` and `SmeltItemFunction` (`:73-110`). Six of the fourteen (drowned, husk, stray, bogged, mooshroom, rabbit) exist in **no declared version** (`:55-57`). | `EntityType<?>`, reached through `HistoricalLootRules.hasOldDrops` | beta_1_7_3 and beta_1_8_1 |
| `gameplay/HistoricalSpawnRules` | Five mob sets: `BETA_1_7_3_MOBS` (14, `:44-59`), `BETA_1_8_1_MOBS` (17 = 14 + enderman/cave_spider/silverfish, `:73-79`), `ALPHA_1_1_2_MOBS` (10, `:96-107`), `INFDEV_20100420_AND_20100611_MOBS` (7, `:139-147`), `INFDEV_20100325_MOBS` (6, `:121-128`) and `INFDEV_20100227_MOBS` (`Set.of()`, `:161`). | entity type id, inside a version-keyed set | beta_1_7_3 (14), beta_1_8_1 (17), alpha (10), infdev_420/611 (7). The 20100325 and 20100227 sets are **not reachable from any declared version** (`SpawnRestrictionSemanticsTest.java:139-151` records this deliberately) |
| `gameplay/HistoricalSpawnFilter` | No data; the applier. `analyse` is pure and records nothing so the verification probe measures the same code the hook runs (`:48-78`). | — | drives the allow-list in `NaturalSpawnerHistoricalSpawnMixin` |
| `gameplay/HistoricalDamageValues` | `Kind` enum with the pre-1.9 base offsets: SWORD 3.0, AXE 2.0, PICKAXE 1.0, SHOVEL 0.0, HOE 0.0 (`:62-78`). The per-tier damage is computed from the item's tier bonus plus a kind-specific constant (`:124-143`), and a sword gets +3.0 more before Beta 1.9 Prerelease 3 (`:135-140`). Only `DiggerItem` and `SwordItem` are rewritten (`:146-148`); hoes keep vanilla. | item id (cached in `REWRITTEN`, `:80`) | sword bonus window: infdev_420 … beta_1_8_1; the rest applies to all declared versions |
| `gameplay/HistoricalGameplayTweaks.EVIDENCE` | 12 entries, covering 11 gate names, each naming the upstream Nostalgic Tweaks flag (`:48-93`). | prose gate name (two of which differ from `gateNames()`) | see §2a and §8.2(j) |
| `gameplay/HistoricalSwordBlocking` | `DAMAGE_REDUCTION_PERCENT = 50` (`:23`) and `damageMultiplier()` (`:55-57`). | — | beta_1_8_1 … release_1_6_4 for the stance; beta_1_8_1 alone for the fall-damage half |
| `generation/BetaOreRules` | `BETA_1_7_3`, 8 `OreRule`s (`:77-86`): dirt 32/20/128, gravel 32/10/128, coal 16/20/128, iron 8/20/64, gold 8/2/32, redstone 7/8/16, diamond 7/1/16, lapis 6/1/(16+16). Caps `maxOriginY`/`maxReachableY` derived at `:60-73`. | rule list, selected by recipe alias through `forAlias` (`:102-109`) | revision 5 and later of Beta 1.7.3 only (revisions 6–10 reuse it) |
| `generation/BetaOreVein` | No data; the transcribed vein algorithm from `fl.a(World,Random,int,int,int)` of the b1.7.3 client (`:59-117`). | — | called by `BetaOrePlacement` |
| `generation/BetaOrePlacement` | No data; the per-chunk population loop, seeded by `setDecorationSeed(levelSeed, chunkX, chunkZ)` (`:57-59`) and replacing **stone and nothing else** (`:108-111`). | — | revisions that declare an ore population |
| `generation/HistoricalBlockPalette` | `BETA_1_7_3`: 83 legacy block **names** (`:38-49`). `MODERN_TO_LEGACY`: 150+ modern-id→legacy-name mappings built by `map(...)` with a duplicate guard that throws at class initialisation (`:54-67`, table at `:69-270`). | modern block id → legacy name; palette is per version | `paletteFor` answers **only** for beta_1_7_3 and returns `null` for everything else, deliberately (`:276-284`) |
| `generation/HistoricalBlockAvailability` | A generated introduction table (`:109-…`, 975 lines) mapping a modern block id to the earliest declared version that had it, with an evidence string and a `timeline`/`palette` trust flag. `CHRONOLOGY` (`:60-71`) is the ten versions oldest first. `VERSIONS_WITH_EXTRACTED_PALETTE` (`:85-93`) is the seven versions with a real palette. | modern block id → `Boundary` | all ten declared versions, but a `palette`-derived date does **not** condemn a version that has no palette of its own (`:78-84`, asserted at `ProfileGenerationRules1211Test.java:327-331`) |
| `generation/HistoricalBlockAnachronisms` | A curated id→release map of blocks later than *every* declared version (`:33-109`), covering 1.7.2 through 1.21.5 plus 1.8–1.12 for the pre-Release profiles. `introducedIn` strips block-state suffixes (`:119-127`). | modern block id → release string | all versions (it means "impossible everywhere") |
| `generation/HistoricalFeatureExclusions` | Eight named exclusion sets, each an append-only superset/derived subset: `BETA_1_7_3_RESTRICTIONS` (8 entries, `:87-104`), `BETA_1_7_3_WITH_REPLACED_ORES` (8 + 18, `:120-162`), `BETA_1_7_3_WITHOUT_AQUATIC_PLANTS` (+10, `:178-193`), `BETA_1_7_3_WITHOUT_MODERN_PLANTS` (+3, `:213-224`), `BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES` (+7, `:248-270`), `BEFORE_GRASS_EXISTED` (+1, `:289-297`), `BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL` (+1, `:305-310`), `INFDEV_WITHOUT_DUNGEONS` (+2, `:320-330`), `BETA_1_7_3_WITHOUT_DESERT_WELL` (+1, `:333-338`), `BETA_1_8_1_WITHOUT_MODERN_VANILLA_FEATURES` (the 1.7.3 set minus melons plus the well, `:360-366`), `BETA_1_9_PRE_3_WITHOUT_MODERN_VANILLA_FEATURES` (minus lily pads, `:374-378`), `RELEASE_1_2_5_WITHOUT_MODERN_VANILLA_FEATURES` (minus the well, `:386-390`). `adaptTo1211` (`:428-446`) drops the three 1.21.5 firefly features (absent from this runtime) and adds the `_upper` granite/diorite/andesite variants, because 1.21.1 has both upper and lower placements while 26.3 had only lower. | recipe alias (`forAlias`, `:412-420`) | selected per revision; see the revision table in §5 |
| `generation/HistoricalStructureRules` | `RULES`: 10 entries, one per declared version (`:143-188`), plus `VANILLA_STRUCTURE_IDS` (34, `:92-104`). | version id, resolved from an alias at `:225-227` | all ten versions |
| `worldgen/HistoricalVillageRules` | `PERMITTED_BIOMES` = plains/sunflower_plains/desert (`:96-99`); two chronology constants (`:76`, `:79`); the evidence string (`:82-86`); `vanillaVillageStructures()` = the five modern village ids (`:201-211`). | version id / alias | villages for beta_1_8_1 … release_1_6_4; modern jigsaw villages suppressed for the same six |
| `worldgen/HistoricalVillageBlocks` | `COMMON` = 28 slot→block ids (`:103-132`); `FORBIDDEN` = 33 banned ids with the release that introduced each or the reason it is not the right block (`:148-185`). Per-version slots derived in `forVersion` (`:214-250`). | version id (derived from the registry, not a second list) | the six village-bearing versions |
| `worldgen/HistoricalVillageStates` | No data beyond a resolved id→`Block` map built once (`:36-60`); fails loudly on an id this runtime does not register (`:50-56`). | block id | runtime-side counterpart to the table above |
| `worldgen/HistoricalVillageTypes` | The structure type and piece type registry keys (`:38-44`), registered idempotently (`:52-61`). | — | all worlds (registered data) |
| `server/ColumnStats` | No version data; the pure column arithmetic the runtime probe reports (`:1-195`). | — | probe only |
| `client/HistoricalClientRules` | `syncedVersionId` (`:28`); resolves the active version from the integrated server in single-player, else from the pushed id (`:52-67`). | — | all client-side rules |
| `client/hud/HudHelper` | The HUD element offsets (`:110-154`) and the two gates `hideHungerBar()` (`:73-76`) and `hideExperienceBar()` (`:79-82`). | active session | hunger bar hidden for the first 4; experience bar hidden for the same 4 |

---

## 5. World generation

### How a version maps to a recipe

`HistoricalVersion.latestRevision()` (`version/HistoricalVersion.java:85-87`) is the creation default for a
version. A recipe is a `GenerationRevision(revision, alias, foundation, experimental, description,
excludedPlacedFeatures, orePopulation)` (`:106-114`). The alias is project-owned and is what a world persists;
the foundation is the real upstream `moderner_beta:*` preset the alias inherits terrain from
(`profile/HistoricalProfiles.java:344-404`).

- `profile/HistoricalProfiles` derives both its tables from the registry (`:100-129`): one canonical
  `HistoricalProfile` per version (only revision 0 registers one, `:118-124`) and one `GenerationBinding` per
  revision, **keyed by the owned alias** (`:152-153`). Every binding's foundation must start with
  `moderner_beta:` (`:162-167`).
- `generation/HistoricalFeatureExclusions.forAlias` (`:412-420`) and `generation/BetaOreRules.forAlias`
  (`:102-109`) both resolve the recipe through `HistoricalVersions.byRevisionAlias`.
- `HistoricalMarker.HISTORICAL_PRESET_IDS` is **derived** from the registry
  (`HistoricalMarker.java:146-147`) so the marker list cannot drift.

Aliases and foundations, verbatim from the registry:

| version | alias(es) | foundation |
|---|---|---|
| beta_1_7_3 | `historical_experience:beta_1_7_3`, then `…:beta_1_7_3_content_preview_1` … `_10` | rev 0 → `historical_experience:beta_1_7_3`; rev 1–10 → `moderner_beta:beta` |
| beta_1_8_1 | `historical_experience:beta_1_8_1`, `…_cave_fix`, `…_no_modern_content` | `moderner_beta:beta_1_8_1` |
| beta_1_9_pre_3 | `historical_experience:beta_1_9_pre_3`, `…_cave_fix`, `…_no_modern_content` | `moderner_beta:beta_1_9_pre_3` |
| release_1_0_0 | `…:release_1_0_0`, `…_cave_fix`, `…_no_modern_content` | `moderner_beta:release_1_0_0` |
| release_1_1 | likewise | `moderner_beta:release_1_1` |
| release_1_2_5 | likewise | `moderner_beta:release_1_2_5` |
| release_1_6_4 | likewise | `moderner_beta:release_1_6_4` |
| infdev_420 | `…:infdev_420`, `…_no_grass`, `…_no_modern_content` | `moderner_beta:infdev_420` |
| infdev_611 | `…:infdev_611`, `…_no_grass`, `…_no_modern_content` | `moderner_beta:infdev_611` |
| alpha | `…:alpha`, `…_no_grass`, `…_no_modern_content` | `moderner_beta:alpha` |

Beta 1.7.3's eleven revisions are the append-only history of the fidelity work, each with a stated delta:
rev 1 deepslate off + no cave-biome injection, rev 2 `min_y 0` via the dimension type, rev 3 the reduced
noise envelope that restores the bedrock column, rev 4 the first eight content exclusions, rev 5 the Beta ore
population, rev 6 one era biome for all caves, rev 7 the 1.13 aquatic plants, rev 8 the 1.21.5 firefly bushes,
rev 9 the remaining modern vanilla features, rev 10 the Release 1.2.1 desert well
(`HistoricalVersions.java:85-166`). The two Infdev and the Alpha version share one helper, so all three have
the same three-revision shape.

**Which revision a new world gets: the newest.** Only the ten revision-0/`no_modern_content` aliases that a
version's *latest* revision resolves to are tagged into the vanilla menu, and the tag holds exactly ten
entries (`src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json`), asserted at
`GenerationBackport1211Test.java:159-166`. So the selectable presets are the ten `latestRevision()` aliases:

`beta_1_7_3_content_preview_10`, `beta_1_8_1_no_modern_content`, `beta_1_9_pre_3_no_modern_content`,
`release_1_0_0_no_modern_content`, `release_1_1_no_modern_content`, `release_1_2_5_no_modern_content`,
`release_1_6_4_no_modern_content`, `infdev_420_no_modern_content`, `infdev_611_no_modern_content`,
`alpha_no_modern_content`.

### Resources

- **38 settings presets** in `data/historical_experience/moderner_beta/settings_preset/`.
- **38 world presets** in `data/historical_experience/worldgen/world_preset/` — one per declared revision,
  named `historical_experience:<alias path>`.
- `HistoricalVersionsTest.everyRevisionHasItsResources` (`:110-126`) asserts both files exist for every
  declared revision.
- 35 of the 38 settings presets carry a reduced noise envelope; the assertion is
  `assertEquals(35, reduced)` at `GenerationBackport1211Test.java:134`, and the envelope's key set is
  asserted to be exactly `{min_y, height, size_horizontal, size_vertical}` = `{0, 128, 1, 2}`
  (`GenerationBackport1211Test.java:124-131`). The three that do not are Beta 1.7.3 revisions 0, 1 and 2.
- One dimension type: `data/historical_experience/dimension_type/beta_1_7_3_overworld.json` —
  `min_y = 0`, `height = 320` (`:9`, `:12`). Every tagged world preset uses it for the overworld
  (`GenerationBackport1211Test.java:40-41`); the Nether and End stay vanilla (`:62-65`).
- Each world preset names the recipe in **all three** places — `provider_settings`,
  `biome_source.provider_settings`, `biome_source.cave_provider_settings` — and the test that all three agree
  is what stops a world whose terrain and cave biomes disagree
  (`GenerationBackport1211Test.java:44-50`; example at
  `data/historical_experience/worldgen/world_preset/beta_1_7_3.json:9-18`).
- One structure (`data/historical_experience/worldgen/structure/historical_village.json`), one structure set
  (`…/structure_set/historical_villages.json`, spacing 60 / separation 20), one biome tag
  (`…/tags/worldgen/biome/has_structure/historical_village.json` = plains, sunflower_plains, desert) and two
  blacksmith loot tables (`…/loot_table/chests/historical_village_blacksmith{,_early}.json`).

### The exclusion and ore systems

- **Exclusion**: `mixin/FeaturePlacerHistoricalContentMixin` cancels `PlacedFeature.placeWithBiomeCheck` when
  `HistoricalFeatureExclusions.shouldSkip` matches (`:43-45`). Identity comparison is against the live
  `PLACED_FEATURE` registry's own instances, resolved per call (`:500-531`). An exclusion that does not
  resolve is **reported**, not swallowed (`:648-656`, surfaced through `unresolvedExclusions()`).
- **Block suppression**: `mixin/TreeDecoratorContextHistoricalSuppressionMixin` cancels
  `TreeDecorator.Context.setBlock` for an anachronistic block (`:54-64`). This exists because leaf litter and
  vines are written from *inside* tree features and excluding the feature would delete the tree
  (`:16-42`). `shouldSuppressVines(WorldGenLevel)` is defined (`HistoricalFeatureExclusions.java:578-580`) but
  has no caller — see §8.
- **Ores**: `mixin/ChunkGeneratorHistoricalOreMixin` runs `BetaOrePlacement.placeForChunk` after
  `applyBiomeDecoration` (`:31-39`). Only the generator's own marker selects a population, so an ordinary
  world, a plain Moderner Beta world and a revision that places vanilla ores are untouched
  (`BetaOrePlacement.java:45-53`). The enabling revision also excludes the 18 vanilla ore features, so the two
  cannot double up (`HistoricalFeatureExclusions.java:120-162`).
- **Deviation, stated in the source**: the chunk random is seeded the vanilla decoration way, not Beta's own
  population stream, so individual vein positions differ from a real Beta 1.7.3 world with the same seed while
  the counts, sizes, height ranges and vein shape are the verified ones (`BetaOrePlacement.java:29-34`).

---

## 6. Persistence and version changes

### How a world's version is stored

`world/metadata/MetadataSchema.java` fixes the on-disk name
(`historical_experience_metadata.json`, `:16`) and the accepted schema (`CURRENT = 1`, `:13`);
`world/metadata/HistoricalMetadata.java` is the record — `schema`, `profileId`, `generationPreset`,
`revision`, `history` (`:24-30`) — with the compact constructor enforcing the current schema, a strictly
increasing append-only history and `revision == history.last().revision` (`:31-69`). Encoding is a
hand-written Gson-streaming codec that rejects unknown and duplicate fields and any trailing content
(`world/metadata/MetadataJson.java:56-132`). Persistence is
`HistoricalMetadataStore.write(HistoricalMetadata)` — forced sibling temp file, then an atomic move
(`:156-179`) — and `writeIfAbsent(HistoricalMetadata)` — a hard link, so it cannot clobber even an unreadable
document (`:204-235`). Classification is `world/HistoricalWorldState.load(...)`, a closed set of three
outcomes: `Normal`, `Historical`, `Unreadable` (`:140-197`), with `loadAt(Path)` for callers that have no
level (`:118-121`). Crucially, `HistoricalWorldState.load` returns `Unreadable` unless the persisted profile
id **equals the profile the persisted recipe is bound to** (`:182-194`), which is what makes a cross pair fail
closed rather than silently re-point.

Key signatures:

```java
// world/metadata/HistoricalMetadataStore.java
public static HistoricalMetadataStore forWorldDirectory(Path worldDirectory)   // :36
public HistoricalMetadata read() throws MetadataException, IOException          // :80
public void write(HistoricalMetadata metadata) throws IOException               // :156
public boolean writeIfAbsent(HistoricalMetadata metadata) throws IOException    // :204

// world/HistoricalWorldState.java
public static HistoricalWorldState loadAt(java.nio.file.Path worldDirectory)     // :118
static HistoricalWorldState load(HistoricalMetadataStore store)                  // :140
```

### How a world's version is changed

`server/HistoricalUpgradeService.java` is the operator-facing half, and `server/HistoricalCommand.java`
(`/hex status|versions|recover|upgrade`, permission level 2, `:44-104`) is a thin text layer over it. The key
signatures:

```java
// server/HistoricalUpgradeService.java
public static Report describe(Path worldDirectory)                                  // :60
public static Report upgrade(Path worldDirectory, String targetId, String actor, long timestamp) // :163
public static WorldVersionState inspect(Path worldDirectory)                        // :238
public static Report upgradeUnloadedWorld(Path worldDirectory, String targetId, String actor, long timestamp) // :288
public static boolean appearsOpen(Path worldDirectory)                              // :316
public static Report recover(Path worldDirectory)                                   // :340
```

`upgrade` stages rather than applies: it plans the new document
(`world/upgrade/GenerationUpgrade.plan`, `:109-170`), writes a verified SHA-256 backup and a write-ahead
journal, and publishes the metadata (`GenerationUpgrade.stage`, `:200-230`). The generator half is deferred
to `GenerationUpgrade.applyPendingAdvance(Path)` (`:241-279`) which rewrites `level.dat`'s three preset
references through `WorldGenSettingsAdvance.advance(...)` and deletes the journal. `GenerationUpgrade.perform`
(`:336-429`) is the one-pass variant for offline tooling, with an injectable `Phase` so each crash window is
testable. The phase enum is at `:49-78`. Recovery is `GenerationUpgrade.recover(Path)` (`:436-441`) and
`world/upgrade/UpgradeRecovery`. The lifecycle applies the pending advance on `SERVER_STOPPED`
(`server/HistoricalServerLifecycle.java:352-372`) — deliberately after the server has saved, because a
measurement showed a running server's own save silently undid an advance performed at startup
(`:341-351`). `mixin/LevelStorageHistoricalUpgradeRecoveryMixin` runs recovery earlier still, under the world
lock and before `getDataTag` decodes the generator (`:23-41`).

Refusals before anything is written: generation in flight, an unfinished journal, an unreadable document, a
target that is not strictly newer, a missing backed-up file, and a stored generator that disagrees with the
metadata (`GenerationUpgrade.java:281-321`, `:345-375`). Downgrade is judged **within** a version by
generation-revision number only, never across versions (`:129-142`).

### What the GUI uses

`client/version/` is four classes plus a probe:

- `HistoricalVersionScreen` — one reusable selector (era row over a scrolling list with a details panel),
  driven entirely by `HistoricalVersionCatalogue` (`:7-15`, `:227-256`). Unimplemented versions are listed,
  dimmed and readable but cannot be confirmed (`:192-207`).
- `CreateWorldHistoricalVersion` — adds the selector to world creation and applies the choice by selecting
  the vanilla world preset the project already ships (`:125-175`); it composes no generation settings.
- `ManageWorldHistoricalVersion` — adds the selector to the world list and calls
  `HistoricalUpgradeService.upgradeUnloadedWorld` (`:121-122`), resolving the save folder from
  `LevelSummary.getLevelId()` through the `WorldListEntryAccessor` client mixin (`:144-147`).
- `HistoricalVersionResultScreen` — prints the service's own words (`:12-17`).
- `ScreenProbe` — property-gated screenshot evidence (`:46-66`).

`client/version/VersionGuiWiringTest` enforces by reading the source that no GUI class names
`HistoricalMetadataStore`, `MetadataJson`, `NbtIo`, `WorldGenSettingsAdvance`, `WorldBackup` or
`GenerationUpgrade` (`:44-65`) and that no GUI class names `ModernBetaSettingsPreset`, `ChunkGenerator`,
`WorldGenSettings` or `settings_preset` (`:68-84`).

---

## 7. Tests already covering historical behaviour

**37 test classes** under `src/test/java/net/historicalexperience/`, all JUnit 5 (`build.gradle:42-53`,
working directory = project root, `:51`). Below, **B** marks a test that verifies a historical *claim*
(behaviour, data or boundary) and **W** marks one that verifies *wiring/registration* or infrastructure only.

| test class | what historical claim it actually verifies | kind |
|---|---|---|
| `version/HistoricalVersionsTest` | Every declared revision is a recognised marker, binds back to its own version and profile, and has both resource files; Beta 1.7.3 revisions 0–3 carry no exclusions and no ore population while revisions 4 and 5 do; Beta 1.7.3 has no sprint/hunger/experience and Beta 1.8.1 has all three; a Beta 1.7.3 zombie drops a feather and a Beta 1.8.1 one rotten flesh; Beta 1.8.1's spawn set is a strict superset of Beta 1.7.3's (`:68-195`) | B (+ registry invariants) |
| `version/HistoricalVersionCatalogueTest` | The planned matrix is exactly 29 targets split 8/14/7 with 6 Beta 1.9 prereleases; the 3 extras stay reachable and uncounted; no future entry is selectable, resolvable or mappable onto a real version; every declared version is reachable from the menu (`:27-156`) | B (menu data) |
| `version/SpawnRestrictionSemanticsTest` | An empty restricted spawn set means *nothing spawns* while an empty unrestricted one means *unresearched*; the surviving Infdev builds carry their 7-mob set and a verified-absent food table; the two withdrawn Infdev builds are absent from the registry but their extracted mob data is retained (`:34-151`) | B |
| `gameplay/HistoricalGameplayTweaksTest` | All 16 gates, against all ten declared versions, checked against the feature-timeline boundary table; the proxy version itself keeps the behaviour; ordinary/unknown worlds are never restricted; an unknown gate name throws; the recorded corrections (panic, baby zombies, gear≡enchant) still hold (`:28-160`) | B |
| `gameplay/HistoricalPlayerRulesWiring1211Test` | Hunger-tick suppression tracks `hasHunger` exactly; sprinting is refused for exactly the four pre-Beta-1.8 versions; unknown worlds keep vanilla hunger and sprinting; the two mixins are registered (`:40-110`) | B + W |
| `gameplay/HistoricalFoodWiring1211Test` | Direct healing applies exactly to the hungerless versions that declare it; every heal value is positive and carries evidence; an unmodelled food is left to vanilla (`:30-89`) | B |
| `gameplay/HistoricalFoodHealTableTest` | The six researched direct-heal values (cookie 1, stew 10, etc.) and the three foods deliberately left on the fallback (`:16-40`) | B |
| `gameplay/HistoricalFoodRulesTest` | The applied Beta 1.7.3 table is exactly the nine verified values incl. the golden apple's out-of-range 42; cake is recorded but not applied and the omission is reported; no hunger-bearing version declares direct heals; the eat gate follows the absence of hunger (`:26-152`) | B |
| `gameplay/HistoricalLootRulesTest` | The Beta 1.7.3 drop table is exactly what the client's `getDropItemId()` returns, each rule cites the client field, and no modern-only drop is in it; experience suppression is a profile rule (`:25-120`) | B |
| `gameplay/HistoricalLootWiring1211Test` | Every mixin on disk is registered and every registered mixin exists; the drop tables apply to Beta 1.7.3 and Beta 1.8.1 and no other version; experience is withheld exactly where the version awarded none (`:46-136`) | W + B |
| `gameplay/HistoricalClientSideGateTest` | Source guard: `canBlock` asks the session and never the player's level, a version-id overload exists, and no mixin asks a version gate about a level (`:30-76`) | W (source guard) |
| `gameplay/BlockFallDamageBoundaryTest` | Only Beta 1.8.1 among the ten is inside the fall-damage-reduction window, while Beta 1.9 Prerelease 3 still has the stance (`:29-54`) | B |
| `gameplay/OreSelfDropTest` | Every declared version drops the ore block rather than a raw metal; ordinary/undeclared worlds keep vanilla drops (`:34-48`) | B |
| `generation/HistoricalFeatureExclusionsTest` | Revisions 0–3 keep selecting no restrictions; only restricted revisions select them; revision 5 keeps every revision-4 exclusion; every restriction names a placed feature with a reason; Beta's own ores are never excluded (`:35-162`) | B |
| `generation/HistoricalStructureRulesTest` | The four pre-Beta-1.8 versions allow no structure; every later version has a researched rule; the modern jigsaw villages are suppressed for all six village-bearing versions; `hasStructures` agrees with the rule table (`:25-127`) | B |
| `generation/HistoricalBlockPaletteTest` | The code palette equals the reviewed `research/beta173-block-palette.json` (83 names, SHA-1 recorded); cobblestone is ALLOWED while stone bricks are ANACHRONISTIC ("the rename trap"); a version with no extracted palette gets no answer (`:33-156`) | B |
| `generation/ProfileGenerationRules1211Test` | Every profile has a sourced structure rule; `blockAvailability`'s version order equals the registry; the generated table re-derives from the reviewed palettes; Alpha/Infdev see vines, grass, stone bricks, sandstone and lapis as anachronistic while Alpha keeps snow/ice/clay/cactus/sugar cane/redstone/spawner; terracotta's corrected date (`:78-354`) | B |
| `generation/GenerationBackport1211Test` | The menu tag resolves to ten presets whose three recipe references agree and name the newest declared revision; every post-Beta-1.7.3 version has a `_no_modern_content` revision excluding the aquatic plants/flowers while keeping melons; 35 settings presets carry the exact 0/128/1/2 envelope; Beta's cave provider is the single non-modern biome (`:19-166`) | B |
| `generation/BetaOreRulesTest` | The eight ore populations match the verified call sites number for number; no emerald; only revision 5+ places Beta ores and it also excludes the vanilla ore features; the derived height ceilings match the algorithm (`:22-128`) | B |
| `generation/BetaOreVeinTest` | The transcribed vein shape is deterministic, bounded, reaches the derived ceiling, re-offers positions, and matches a pinned seed's exact offered-position sequence (`:36-138`) | B |
| `generation/HistoricalVillageGeneration1211Test` | Villages exist exactly from Beta 1.8.1; the boundary cites `feature-timeline.json`'s own quote; the data files agree with the code (spacing 60/20, empty spawn overrides); the biome tag is exactly plains/sunflower/desert; no emitted block is anachronistic or unjudged for Beta 1.8.1; the era substitutions (gravel path, stone slab, fence not iron bars, no terracotta); carrots/potatoes/sandstone stairs only at Release 1.6.4; brewing stand from Beta 1.9 Pre3; every forbidden family is never emitted (`:143-533`) | B |
| `world/GenerationBindingReloadTest` | A reload reproduces exactly the persisted binding, never downgrades to the frozen declaration, never substitutes a different revision or the shared foundation, never rewrites the document, and fails closed on an unknown recipe, the foundation, or a cross pair (`:62-293`) | B |
| `world/HistoricalWorldStateTest` | An ordinary world is `Normal`; a valid document is `Historical`; corrupt metadata, an unknown profile, a preset/profile mismatch and a newer schema are all `Unreadable` with bytes preserved (`:36-171`) | B |
| `world/upgrade/World1211GenerationPersistenceTest` | An advance rewrites all three 1.21.1 references and touches no other NBT; lookalike provider tags on another generator are refused before writing; every crash window recovers with region files and player data byte-identical (`:44-108`) | B |
| `server/HistoricalUpgradeService1211Test` | Bare, namespaced and quoted target names all qualify; a stored-marker mismatch is refused with no journal and no backup; ordinary, unknown and no-op upgrades are refused without mutations (`:50-91`) | B |
| `server/WorldVersionManagementTest` | An ordinary world is reported ordinary and cannot be changed; a historical world reports its version and recipe; an unreadable document is not read as ordinary; a world open elsewhere is refused; a staged change is reported pending and blocks a second change (`:36-141`) | B |
| `server/HistoricalMarkerDecisionTest` | The classification policy: unmarked normal stays ordinary, a marked overworld establishes, any registry failure or unknown marker fails closed in every metadata state, corrupt metadata and marker/profile mismatches fail closed (`:72-223`) | B |
| `server/ColumnStatsTest` | The probe's column arithmetic, incl. signed chunk-coordinate conversion (`:33-235`) | infrastructure |
| `integration/modernerbeta/AppliedPresetEvidenceTest` | `allSettingsMatch` requires all three component blocks and is independent of provider readiness (`:9-22`) | infrastructure |
| `integration/modernerbeta/GeneratorFactsTest` | Envelope maxima are computed, and any unknown input keeps terrain diagnostics unknown (`:18-57`) | infrastructure |
| `world/metadata/MetadataJsonTest` | Strict codec: roundtrip, unknown schema, missing field, unknown field, malformed JSON, wrong types, invalid identifier, non-increasing history, revision/history mismatch (`:26-178`) | infrastructure |
| `world/metadata/HistoricalMetadataStoreTest` | Atomic replace with no temp files left, corrupt bytes preserved, `writeIfAbsent` never clobbers, and one winner among eight concurrent creators (`:45-237`) | infrastructure + preservation |
| `world/metadata/HistoricalIdentifierTest` | The identifier value type's accepted and rejected forms (`:17-68`) | infrastructure |
| `profile/GenerationBindingTest` | The three identifiers are never conflated; both Beta 1.7.3 revisions share a profile id but are distinct resolved values; unknown/cross-pair lookups resolve empty; markers are exactly the declared revision aliases (`:54-331`) | B + registry invariants |
| `profile/HistoricalProfilesTest` | Beta 1.7.3 is bound to the pinned preset and the display name does not overclaim (`:18-74`) | infrastructure |
| `client/version/VersionGuiWiringTest` | Source guard: the interface changes versions only through the shared service and builds no generation settings (`:32-124`) | W (source guard) |

No test covers: the client `HudHelper` offset arithmetic; the sword-blocking render poses; the armour tint;
the mixin-ordinal correctness that `docs/nt-port-audit.md:266-300` records as the most expensive past defect;
or the `_upper`/`_lower` granite-diorite-andesite adaptation (only asserted indirectly through
`GenerationBackport1211Test.java:146-157`).

---

## 8. Gaps and risks you can evidence

### 8.1 Confirmed code/comment disagreements

**(a) `HistoricalPlayerRules`' own class comment says direct food healing is not implemented. It is.**
`gameplay/HistoricalPlayerRules.java:28-31` states: *"Beta 1.7.3 healed directly when the player ate. That is
not implemented yet, so a no-hunger version currently has no hunger, no natural regeneration and no
starvation - but eating also does not yet heal."* Direct healing is implemented in
`mixin/PlayerHistoricalFoodMixin.java:46-69` (heal at the HEAD of `eat`), in
`mixin/FoodDataHistoricalHungerMixin.java:37-51` (heal from the remembered food-level modifier), and in
`mixin/LivingEntityHistoricalInstantEatMixin` for the eat timing. The comment is stale.

**(b) Two different, disagreeing direct-heal tables.**
`gameplay/HistoricalFoodRules.BETA_1_7_3_HEAL_POINTS` is the registry's declared table: apple 4, bread 5,
mushroom_stew 10, porkchop 3, cooked_porkchop 8, golden_apple 42, cod 2, cooked_cod 5, cookie 1
(`:84-110`). `gameplay/HistoricalFoodHelper.DIRECT_HEAL_BY_ID` is what the mixin actually calls
(`PlayerHistoricalFoodMixin.java:58` → `HistoricalFoodHelper.directHealAmount`, `:87-92`) and it holds
**only six entries**: apple 4, bread 5, mushroom_stew 10, porkchop 3, cooked_porkchop 8, cookie 1 — with
`cod`, `cooked_cod` and `golden_apple` absent, so those fall through to modern nutrition
(`HistoricalFoodHelper.java:31-43`; the omission is described at `:34-36`). Consequences visible in the code:
the registry declares golden_apple = 42 while `HistoricalFoodHealTableTest.java:36-37` asserts that
`researchedHeal("minecraft:golden_apple")` is **null** and that this is deliberate. The registry's own
`healPointsFor` path (`HistoricalFoodRules.java:178-184`) is called only from tests. Two independently
reviewed tables describe the same behaviour and only one is on the live path.

**(c) `swordBlockingEnabled(Level)` cannot work on a client, yet its own javadoc says it is needed there.**
`HistoricalGameplayTweaks.java:304-314`: *"Needed because the answer has to hold on the CLIENT too: a client's
player level is a ClientLevel, so a level-based question returns null there"* — and the body of the level
overload is `return swordBlockingEnabled(versionOf(level));` (`:300-302`), while `versionOf` returns `null`
for anything that is not a `ServerLevel` (`:102-107`). So the level overload is exactly the shape the comment
warns about. In practice the live path is correct because `HistoricalSwordBlocking.canBlock` calls the
`String` overload with `HistoricalActiveSession.current()` (`gameplay/HistoricalSwordBlocking.java:50`), which
is also what `HistoricalClientSideGateTest.java:49-52` asserts. The risk is a future caller reaching for the
`Level` overload.

**(d) Two dead-but-documented chest references.** `HistoricalGameplayTweaks.alwaysOpenChestEnabled(Level)`
(`:264-266`) is declared and its own comment says *"UNVERIFIED … so this stays on for every declared version
rather than pretending to a boundary"*. `docs/nt-port-audit.md:228-236` claims the opposite for the same
feature: *"`ALWAYS_OPEN_CHEST` is … named in the timeline under 'chests cannot be opened when a block is
above'. It has a source"* — and then `:239-250` retracts it again (low confidence, source runs the other
way). A repository-wide search finds **no caller** for `alwaysOpenChestEnabled`, so the feature is declared,
unwired and contradicted by its own documentation trail.

### 8.2 Gates that look broader or narrower than the version they name

**(e) `ItemBodyBlockHistoricalDropMixin`'s rule is a no-op for most of what it names.** The mixin modifies
`BlockBehaviour.getDrops` (`mixin/BlockBehaviourHistoricalDropMixin.java:39-51`) and recognises six ores:
iron, gold, copper and their deepslate variants (`:54-61`). In 1.21.1 the ore *drops* are datapack loot
tables, not `getDrops` — verified directly in the pinned jar: `data/minecraft/loot_table/blocks/iron_ore.json`
and `gold_ore.json` exist alongside the others. So the hook cannot change what a mined iron or gold ore
yields; `1.21.1` still drops `raw_iron`/`raw_gold`. `OreSelfDropTest.java:35-42` passes because it asserts the
*predicate* (`oreDropsAsBlocks()`), and `HistoricalGameplayTweaks.java:374-386` claims *"all ten drop the
block"*. The claim is not established by the mechanism. Copper and the deepslate variants are unreachable in
any case (`:29-31`), and the fourth "self block drop" the comment in `docs/nt-port-audit.md` alludes to has no
separate confirmation here.
I flag this as too broad a claim over too narrow a hook, evidenced by the jar's own loot-table files; I did not
run the game to observe a drop.

**(f) `betaExhaustionCosts` and `betaRegenerationModel` are documented as gated on hunger, but are gated on
the pre-Beta-1.8 list.** `HistoricalGameplayTweaks.java:395-405`: the javadoc says *"Gated on the hunger
boundary"* and the body is `isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId)`. The
distinction is invisible today because the two membership sets coincide exactly, but the predicate is written
as a version-list test rather than a registry-flag test (`HistoricalPlayerRules.hasHunger`), so a future
declared version between Beta 1.7.3 and Beta 1.8.1 — or one with hunger and a chronology below Beta 1.8.1 —
would silently receive Beta exhaustion costs. Same shape at `:430-432` for the regeneration model. The range
is currently correct; the *expression* is not the one the comment describes.

**(g) `avoidSun` and `customDeathLoot` proxy Release 1.2.1 with Release 1.2.5.** Both use
`olderThan(versionId, RELEASE_1_2_5)` (`:145`, `:149`), and the comments say so explicitly (`:202-205`,
`:146-148`). The consequence is that Release 1.2.5 and Release 1.6.4 keep the behaviour the real 1.2.1
introduced, and everything below 1.2.5 lacks it — correct for the ten declared versions, but there is no
declared version *between* 1.2.1 and 1.2.5, so the proxy is doing real work and would be wrong if one were
added.

**(h) `monsterGear`, `monsterEnchant`, `itemPickup` and `babyZombie` all proxy four real boundaries with
Release 1.6.4.** The evidence map dates item pickup and gear to Release 1.4.2 (`:56-62`), enchantments to
1.4.2 (`:72-75`) and baby zombies to Release 1.6.2 (`:67-71`), but the predicate is one shared
`olderThan(versionId, RELEASE_1_6_4)` (`:152-153`). Every version from Release 1.0.0 to Release 1.2.5 is
therefore treated as "before 1.4.2" while never being distinguishable from each other; the range is right for
the declared set and structurally coarse.

**(i) `cooldown`, `sweep` and `swim` are `isDeclaredVersion(versionId)` — true for all ten.** The comment
justifies it (`:154-156`) and the test pins it as `ABSENT_FROM_EVERY_DECLARED_VERSION`
(`HistoricalGameplayTweaksTest.java:53`). This is honest but means the gate cannot express a partial
boundary: a future declared version at or after 1.9 would keep all three behaviours removed. `mobArms` has
the same shape for a different reason (arm swinging was removed in Indev 20100202, `:157-160`).

**(j) One documented rule has no research row at all.** `tempting` (`:212-215`) is a real gate and a real
mixin (`TemptGoalHistoricalMixin`), but `EVIDENCE` has no `tempting` key; the nearest evidence is the
`breeding` entry, which names `DISABLE_ANIMAL_TEMPTING` only in passing (`:87-93`). Similarly `cooldown`,
`sweep` and `swim` are absent from `EVIDENCE`. `documentedGates()` returns 12 names, `gateNames()` returns 16,
and the two vocabularies differ further because `EVIDENCE` uses the prose keys `gearedMobs` and
`monsterPickup` where the gate table uses `monsterGear` and `itemPickup` — so a reader cannot join them
mechanically.

### 8.3 TODO / FIXME markers

A repository-wide search for `TODO|FIXME|XXX|HACK` in `src/main/java/net/historicalexperience` returns **no
markers**. There is one `"Known gap"` heading (`gameplay/HistoricalFoodRules.java:39`) and one `"Known gap"`
phrase (`gameplay/HistoricalPlayerRules.java:28`) — and the second is the stale claim in 8.1(a). The
"not implemented" family is: `PlayerHistoricalCriticalHitMixin.java:30` (the crit formula's +2 HP and its
"after enchantments" ordering), `HistoricalFoodRules.java:39-51` (cake, the wolf-feeding quirk, the hunger bar
still drawn), `HistoricalPlayerRules.java:28-31` (stale), `gameplay/HistoricalDamageValues.java:53-58` (the
pre-Beta-1.9-per-tier sword numbers), `gameplay/HistoricalMobLootHelper.java:55-57` (six unreachable entity
mappings), `generation/HistoricalStructureRules.java:184-188` (the modern block materials of the Release 1.6.4
desert pyramid), `generation/HistoricalFeatureExclusions.java:201-212` (leaf litter), and
`docs/nt-port-audit.md:117-137` (`OLD_MOB_MOVEMENT_SPEED`).

### 8.4 Behaviour implemented in more than one place (possible duplication)

**(k) `swordBlockingEnabled` is written twice with contradictory forms.** The canonical form is
`isDeclaredVersion(versionId) && !BEFORE_BETA_1_8.contains(versionId)` (`HistoricalGameplayTweaks.java:311-314`).
The comment block at `:316-329` describes a *second*, inline statement of the same boundary that "used to" be
wrong — but the live duplicate is the level overload at `:300-302`, which re-derives the answer from a
`Level`. Two entry points, one of which (the level form) is provably inert on a client (8.1c).

**(l) The 1.9-combat boundary is stated once but consumed in five files.** `appliesTo("cooldown", …)` in
`PlayerHistoricalCombatMixin` (2 sites) and `GuiHistoricalAttackIndicatorMixin`;
`appliesTo("sweep", …)` in `PlayerHistoricalCombatMixin`, `ParticleEngineHistoricalSweepMixin` and
`ClientLevelHistoricalAttackSoundMixin`. This is the intended shared-abstraction design rather than
duplication, but note that `ClientLevelHistoricalAttackSoundMixin.java:54` consumes the **`sweep`** gate for
the attack *sound* set, so a future split of the sweep boundary would silently change sound behaviour.

**(m) Direct food healing exists on three separate code paths.** `PlayerHistoricalFoodMixin` heals at the HEAD
of `Player.eat` using `HistoricalFoodHelper`; `FoodDataHistoricalHungerMixin` heals from the remembered
food-level modifier on the next `FoodData.tick`; and `DataComponentHolderHistoricalFoodMixin` rewrites the
`FOOD` component's nutrition. Only the first uses the researched table; the third uses a different table
(8.1b). A meal can therefore be described by up to three different values depending on which path a caller
asks.

**(n) The mob-loot behaviour is written twice.** `gameplay/HistoricalLootRules` declares four drops per
version as plain data, and `gameplay/HistoricalMobLootHelper` builds fourteen *different* tables
(zombie→feather max 2, pig→porkchop, skeleton→arrow+bone, sheep→dyed wool) and is what the mixin actually
consults (`LivingEntityHistoricalLootMixin.java:49` → `HistoricalMobLootHelper.getTable` →
`HistoricalLootRules.hasOldDrops` only as a *boolean gate*, `HistoricalMobLootHelper.java:154`). The declared
`DropRule.itemId` and `maxExclusiveCount` are never used to build a table: a Beta 1.7.3 zombie gets
`HistoricalMobLootHelper`'s feather table (max 2), not the declared `maxExclusiveCount = 3`. Both are tested,
by different tests (`HistoricalLootRulesTest` vs `HistoricalLootWiring1211Test`), so neither suite notices the
divergence.

**(o) The ore population and the ore-drop rule both restate "six ores".**
`HistoricalBlockPalette.BETA_1_7_3` (`:38-49`), `HistoricalBlockAvailability`'s ore entries,
`BetaOreRules.writtenBlockIds()` (`:122-125`), `HistoricalFeatureExclusions`' vanilla-ore exclusion list
(`:120-162`) and `BlockBehaviourHistoricalDropMixin.isOre` (`:54-61`) each enumerate ores independently.

### 8.5 Documentation that contradicts the code

**(p) `docs/GUI-REDESIGN.md` is stale in the direction that matters most.** Its "Stage A — what the repository
actually is" section states *"No GUI exists today"* and *"A repository-wide grep for
`Screen|setScreen|CreateWorldScreen|WorldPreset` in Java returns no matches. The screens are entirely new
work"* (`:10-15`), and it counts *"47 settings presets"* and *"47 matching world presets"* with 10 tagged
(`:19-24`). The code now has four GUI classes and five client-version sources
(`client/version/*.java`), and the resource counts are **38 and 38** (see §5). The same file later records
the landed work at `:104-130`, so the document contradicts itself.

**(q) `docs/GUI-REDESIGN.md:100` claims a test count of 257 tests in 37 suites.** 37 test classes is right;
the test-method count was not re-derived here.

**(r) `docs/nt-port-audit.md:9-28` names two files that no longer exist** — `mixin/PlayerHistoricalSprintMixin`
and `mixin/ItemStackHistoricalDamageMixin` — and omits roughly half of the mixins that do. The sections from
`:79` onward are accurate about carriers and are the best available evidence for why several current designs
look the way they do (`:266-300` on why a server boot cannot substitute for a client join is the single most
load-bearing note in the repository).

### 8.6 Other evidenced risks

**(s) Several declared predicates have no caller.** Repository-wide call-site search (excluding their own
declaration and tests):
`alwaysOpenChestEnabled`, `monsterItemPickupDisabled`, `monsterItemSpawnDisabled`, `monsterAvoidSunDisabled`,
`animalBreedingDisabled`, `animalTemptingDisabled`, `babyAnimalSpawningDisabled`, `animalPanicDisabled`,
`babyZombieSpawningDisabled`, `monsterEnchantmentSpawnDisabled`, `skeletonStrafingDisabled`,
`criticalHitDisabled`, `attackCooldownDisabled`, `sweepAttackDisabled`, `HistoricalFoodRules.healsFoodDirectly`,
`HistoricalFoodRules.allowsEatingWithoutHunger`, `HistoricalFoodRules.healPointsFor`,
`HistoricalPlayerRules.recordSprintSuppression`, `HistoricalPlayerRules.recordHungerTickSuppression`,
`HistoricalFoodRules.recordHeal`, `HistoricalFoodRules.recordEatGateOverride`,
`HistoricalCombatDiagnostics.recordCooldownAnswer`, `HistoricalCombatDiagnostics.recordDamageRewrite` and
`HistoricalFeatureExclusions.shouldSuppressVines` are all defined and never called by production code. That is
not a defect by itself (the level-shaped overloads are deliberate seams for testing, and the counters were
superseded by the `LongAdder`s in `HistoricalSpawnRules`/`HistoricalLootRules`), but it is the exact shape of
two past defects this project records: dead loot tables (`HistoricalLootWiring1211Test.java:21-32`) and a
panic gate that applied everywhere (`HistoricalGameplayTweaksTest.java:18-24`). The diagnostics counters that
*are* wired are `recordOrbRefused` (`ServerLevelHistoricalOrbMixin.java:47`), `recordSweepCancel`
(`PlayerHistoricalCombatMixin.java:80`), `recordFilterInvocation`/`recordRemoval`
(`HistoricalSpawnFilter.java:89`, `:91`), `recordReplacedDeath`/`recordSuppressedExperience`
(`LivingEntityHistoricalLootMixin.java:54`, `:68`), `recordVeinAttempt`/`recordBlockPlaced`
(`BetaOrePlacement.java:73`, `:113`) and the `SKIPPED`/`SUPPRESSED_BLOCKS` increment sites.

**(t) The dimension gap is real, was measured, and is only closed for structures.**
`HistoricalGenerationScope` exists because a historical world's Nether and End use vanilla generators, so
`historicalAlias` answers `null` there (`generation/HistoricalGenerationScope.java:11-20`); the recorded
consequence is that a Beta 1.7.3 world could generate a Nether fortress before it existed. The scope is set on
`ENFORCE_HISTORICAL`/`ESTABLISH_METADATA` and cleared otherwise (`server/HistoricalServerLifecycle.java:140`,
`:143`, `:153`, `:355`). It is consulted by `HistoricalFeatureExclusions.activeAlias` (`:613-616`, used by the
structure hook at `mixin/StructureHistoricalSuppressionMixin.java:77`) and by
`isAnachronisticForActiveVersion` (`:590`). It is **not** consulted by
`HistoricalProfileLookup.profileIdForGenerator` (`gameplay/HistoricalProfileLookup.java:19-26`), which every
gameplay gate uses. So in a historical world's Nether or End, the structure rule still applies but the mob
spawn allow-list, the drop substitution, the experience suppression, the sprint gate and every other
`HistoricalGameplayTweaks` gate correctly **do not**, because the profile resolves to `null`. That is the
documented fail-safe direction ("unknown ⇒ leave modern behaviour alone"), so it is a gap in coverage rather
than a wrong answer — but it is unimplemented, not merely undocumented.

**(u) In `HistoricalStructureRules` the allow-list short-circuits before the version lookup, and one result is
unreachable.** `suppresses` (`:221-247`) returns `false` early for a structure the rule *allows*
(`if (rule == null || rule.allows(structureId)) return false;`) — that is, when `allows` matches, the
`SUPPRESSED` counter is not incremented. The consequence is that the project's own
`historical_experience:historical_village` is allowed by every village-bearing version's rule, so its
suppression count can never be observed; only *suppressed* structures are counted (`:244-246`). I make no
claim about whether that is intended — it is simply what the code does, and it means
`HistoricalStructureRules.suppressedCount()` is a count of denials and not of decisions.

**(v) The only numeric disagreement found between a document and a resource is the resource count in
`docs/GUI-REDESIGN.md`** (8.5p). The ten-entry `normal.json` tag, the fixture revisions used by tests
(`release_1_6_4`, `beta_1_7_3_content_preview_4`/`_10`) and every alias/resource pair all agree with the code.

---

## Appendix: counted facts

| quantity | value | source of the count |
|---|---|---|
| declared versions | **10** | `HistoricalVersions.ALL` |
| declared generation revisions | **38** | sum over `HistoricalVersion.revisions()` |
| declared eras (registry) | 4 (`infdev`, `alpha`, `beta`, `release`) | `HistoricalVersions.eras()`, `:661-664` |
| eras declared but unused | 2 (`classic` 100 000, `indev` 200 000) | `:37-39` |
| catalogue rows | 32 = 29 planned + 3 implemented extras | `HistoricalVersionCatalogue.build()` |
| named behaviour gates | 16 | `HistoricalGameplayTweaks.gateNames()` |
| gates with a recorded reason | 11 of 16 (from 12 `EVIDENCE` entries; two keys use different names) | `HistoricalGameplayTweaks.EVIDENCE` |
| version-gated predicates outside the gate table | 12 | §2b |
| registry-flag gates | 12 | §2c |
| **distinct gated behaviours catalogued** | **~40** (16 gate names + 12 outside predicates + 12 registry-flag rules; the structure allow-list and the village `chronology >=` comparisons are reported as ranges inside those rows rather than counted separately) | §2a–2d |
| mixins | **47** = 35 shared + 12 client | `historical_experience.mixins.json` |
| mixin classes on disk | 47 (exactly the registered set) | `HistoricalLootWiring1211Test.java:48-82` |
| settings presets | **38** | `data/historical_experience/moderner_beta/settings_preset/` |
| world presets | **38** | `data/historical_experience/worldgen/world_preset/` |
| settings presets with the reduced envelope | 35 | `GenerationBackport1211Test.java:134` |
| selectable world presets (vanilla menu) | 10 | `data/minecraft/tags/worldgen/world_preset/normal.json` |
| structure rules | 10 (one per declared version) | `HistoricalStructureRules.RULES` |
| test classes | 37 | `src/test/java/net/historicalexperience/` |
| `TODO`/`FIXME` markers in main sources | 0 | repository search |

# Implementation coverage

The traceability matrix the 29-version task requires. It records what is implemented, what is corrected, what is missing,
and **why** anything missing is missing, so that "supported" cannot be inflated by counting registered versions.

Status key:

| Mark | Meaning |
|---|---|
| **V** | Verified existing implementation, preserved |
| **E** | Existing implementation extended (gate widened, narrowed or corrected) |
| **N** | Newly implemented |
| **C** | Corrected historical inaccuracy |
| **U** | Unresolved - with the reason, never silently chosen |
| **A** | Absent and intentionally not attempted yet |

Cross-references: `research/implementation-inventory.md` (what existed before this task),
`research/version-transitions.json` (215 build-anchored states across 29 versions),
`docs/ADDING-A-VERSION.md` (the 17-step registration checklist), `docs/GUI-REDESIGN.md` (the GUI).

---

## 1. Version registry

**Declared: 10 of 29.** A version is counted as supported only when its content set, not merely its id, exists.

| Version | Id | State | Notes |
|---|---|---|---|
| Infdev 20100420 | `infdev_420` | V | Pre-Alpha, not one of the 29 targets; kept reachable |
| Infdev 20100611 | `infdev_611` | V | as above |
| Alpha v1.1.2_01 | `alpha` | V | the only Alpha profile so far; **not** one of the 29 targets |
| Beta 1.7.3 | `beta_1_7_3` | V | 11 generation revisions; the reference profile |
| Beta 1.8.1 | `beta_1_8_1` | V | 3 revisions |
| Beta 1.9 Prerelease 3 | `beta_1_9_pre_3` | V | 3 revisions |
| Release 1.0 | `release_1_0_0` | V | 3 revisions |
| Release 1.1 | `release_1_1` | V | 3 revisions |
| Release 1.2.5 | `release_1_2_5` | V | 3 revisions |
| Release 1.6.4 | `release_1_6_4` | V | 3 revisions |

The other **19 targets are registered in the GUI catalogue as planned, not as playable versions**, and the create-world
screen refuses them. That distinction is enforced by `HistoricalVersionCatalogueTest`.

### Chronology integrity (C)

The Alpha profile's chronology was hand-written as `ERA_ALPHA + 112` = 400112, which sits **below** every Alpha 1.x
build the registry's own scheme produces (Alpha 1.0.1 is 401001). Declaring Alpha 1.0.1 would have sorted it *after*
Alpha v1.1.2_01 - eleven builds backwards, silently. Normalised to `chronology(ERA_ALPHA, 1, 1, 2)`, guarded by
`VersionChronologyTest` (era bands, uniqueness, and specifically that Alpha 1.0.11 sorts before Alpha 1.1.0 - the packed
three-digit form would have inverted those two as well).

---

## 2. Gameplay subsystems

| Subsystem | State | Where | Evidence / caveat |
|---|---|---|---|
| Hunger model, Beta 1.8 form | V | `HistoricalGameplayTweaks.betaRegenerationModel`, `FoodDataHistoricalRegenerationMixin` | threshold 18, interval 80, heal 1 - matches the research's "1 health every four seconds above 90%" |
| Regeneration exhaustion charge | N | `FoodDataHistoricalRegenerationMixin` | owner class `FoodData`, not `Player`; verified by the probe's mixin smoke test |
| Starvation thresholds (Easy 10 / Normal 1 / Hard kills) | V | vanilla | modern already matches; no work needed |
| Direct food healing, pre-Beta-1.8 | C | `HistoricalFoodHelper` | was a 6-entry copy against the registry's 9; golden apple 42, cod 2, cooked cod 5 were falling back to **modern nutrition**. Now one table | 
| Food healing is **per version** | C | `HistoricalFoodHelper.researchedHeal` | the path read one fixed table for every version, so each version's researched table was **dead data**: `ALPHA_1_1_2_HEAL_POINTS` already existed and was never consulted, so raw fish (Alpha 1.2.0), cookies (Beta 1.4) and cake (Beta 1.2) healed in Alpha worlds. It now reads the active version's own table, with the pre-1.8 table as the documented fallback for versions whose heals were never researched |
| Golden apple, pre-Release-1.1 | C | `HistoricalFoodHelper`, `DataComponentHolderHistoricalFoodMixin` | 10 hunger, always edible, **Regeneration 30 s at amplifier 0**. First implementation returned an EMPTY effect list. Boundary corrected from a 1.2.5 proxy to Release 1.1 itself |
| Cookie / stew hunger values | V | `HistoricalFoodHelper.BETA_HUNGER` | cookie 1, stew 8 |
| Exhaustion costs (attack 0.3, jump 0.2, sprint-jump 0.8) | N | `PlayerHistoricalExhaustionMixin` | per-action numeric costs are **absent** from the research beyond these |
| Critical-hit formula | V | `PlayerHistoricalCriticalHitMixin` | `[1.0, 2.0)` multiplier; the Beta "+2 HP" term is **U** - needs a verified local ordinal |
| Sword blocking | V | `LivingEntityHistoricalSwordBlockingMixin` | 50% reduction, no durability cost; blocking **stops reducing fall damage** in some Beta 1.9 prerelease - which one is **U** |
| Enchanting | N | `EnchantmentMenuHistoricalEnchantingMixin` | three states: none before Pre3, **UI and glint only** at Pre3, functional from Pre4 |
| Brewing | N | `BrewingStandHistoricalBrewingMixin` | Pre2's cauldron prototype has no block ID; **Pre3 is the first functional brewing** |
| End portal | N | `EnderEyeHistoricalPortalMixin` | inert at Pre3, activated at Pre4. Hooked the **item** - `EndPortalFrameBlock` has no use method in 1.21.1 |
| The dragon | A | - | Pre4 code only, Pre5 achievement and no spawn, **Pre6 first killable boss** |
| Armour model change at Beta 1.8 -> Pre1 | A | - | fire/explosion/projectile stop being reduced; higher durability; effectiveness stops decaying. Sourced, not yet implemented |
| Orb suppression before Beta 1.8 | V | `ServerLevelHistoricalOrbMixin` | collected XP exists from Beta 1.8 and is **spendable only from Pre4** |
| XP-per-jump bug (Pre1, 1-100 XP) | A | - | temporary build defect; sourced with a range |
| Sprint speed, hunger gate | V | movement mixins | 5.612 m/s against a 4.317 walk is exactly the modern 1.3 multiplier; no work needed |
| Mob spawn sets, per version | V / E | `HistoricalSpawnRules` | **per-build**, not shared: chickens arrive 1.0.14, slimes 1.0.11 (and their spawning is *disabled by a bug* in 1.0.14) |
| Mob drops | V | `HistoricalMobLootHelper` | pre-1.8 tables verified against the research for the whole pre-1.8 window |
| Ore self-drop (iron/gold drop the block) | V | `BlockBehaviourHistoricalDropMixin` | **runtime-verified** by breaking blocks, after an audit inferred it could not fire |
| Block availability dating | V | `HistoricalBlockAvailability` | 205 blocks dated; its `CHRONOLOGY` list is ordered and `indexOf` **is** the comparison |

---

## 3. World generation

Generator epochs are **seed-discontinuous** and a single settings preset cannot express per-build differences. The
pinned generator's inventory is the hard limit on what can be supported:

| Epoch | Available foundation | Covers | Caveat |
|---|---|---|---|
| Pre-biome Alpha | `moderner_beta:alpha` | the 7 Alpha 1.0.x-1.1.0 targets | one preset for seven builds |
| Biome-era Alpha | **none** | Alpha 1.2.0 | **U** - nearest is `beta_1_1_02`; an approximation that must be stated |
| Climate Beta | `moderner_beta:beta`, `beta_1_1_02` | Beta 1.0-1.7 | per-build defects (1.0 lava lakes, 1.2 lapis, 1.6 ore bias) need exclusions |
| Beta 1.8+ | `beta_1_8_1` | Beta 1.8 | sea level 64 -> 63 |
| Beta 1.9 prereleases | `beta_1_9_pre_3` **only** | all six prereleases | their distinctness must come from gameplay, not terrain |
| Release | `release_1_0_0`, `release_1_1`, `release_1_2_5`, `release_1_6_4` | 1.0, 1.1, 1.2, 1.6 | 1.3/1.4/1.5 have **no preset** - **U**, nearest is 1.6.4 with exclusions |
| Height 128 -> 256 | declared as 320 in one dimension type | Release 1.2+ | **U** - the dimension type and the presets disagree; observed, not yet explained |

Also **U**: dungeon attempts 4 -> 8 in Alpha 1.0.14 shifts downstream decorator RNG and cannot be expressed through the
generator; and winter-mode worlds (Alpha 1.0.4) would need a new world preset rather than a reuse.

---

## 4. Verification state

| Layer | What it covers | Honest limit |
|---|---|---|
| Unit + boundary | 285 tests / 45 suites | values and gates, not pixels or terrain |
| Source guards | no GUI class writes world state; no hardcoded version names; every implemented version is selectable and has a preset; settings presets inherit the recorded foundation; chronology bands and uniqueness; pre-Beta-1.8 derivation; catalogue ordering | guards a claim, not behaviour |
| Mixin smoke test | probe loads the classes the mixins target, so a wrong injection fails a **probe** | client-only targets cannot be loaded on a server |
| Server generation probe | per profile: structures, block sampling, spawns, ore drops, chunk hashes | a boot does **not** exercise player join |
| Client join | the only check that exercises player-join classes; verified once this session | visuals still need a human eye |
| Research cross-check | golden apple settled against the official client artifact | most of Part VI's numeric gaps have no artifact |

**Recorded lesson:** a failed injection is fatal when the class is *transformed*, and transformation happens at class
load - so a server boot cannot see an injection on `FoodData`, which is transformed when a player joins. That blind spot
broke every join twice before the probe was taught to load those classes.

---

## 5. Contradictions in the research, and how each is handled

| Contradiction | Handling |
|---|---|
| Golden apple: `historical-features.json` said 10 hunger with no effect; the version-history doc said 10 hunger with 30 s Regeneration | Settled against the **official Beta 1.8 client artifact**: 10 hunger, always edible, Regeneration 30 s at amplifier 0. Implementation corrected (**C**) |
| 256-block height: the matrix says Release 1.0, the prose says Release 1.2.1 | Prose followed; both recorded |
| Dragon: the timeline row says Release 1.0.0, every other section says Pre6 | Pre6 treated as the first killable boss |
| Cake's Beta 1.8 value: version page 3, Food history 2 | Left disputed; not implemented |
| Which prerelease dropped sword-blocking's fall-damage reduction | **U** - the research says only "a Beta 1.9 prerelease" |
| Nether fortress first build: Pre1 versus sequence-level only | Treated as sequence-level, not pinned |
| Enderman health 20 -> 40 at Pre3 | Recorded from Part IV only; the Pre3 section says mob stats were not audited |

---

## 5a. Alpha 1.0.1: what the first probe found

The generation probe for `historical_experience:alpha_1_0_1` passed (success, 1018 chunks, no palette failures, 205
dated blocks, all eleven mixin targets transformed, and **72 spawn-filter invocations that removed `minecraft:chicken`** -
the per-version mob set working at runtime rather than merely declared).

It also listed ten blocks in the generated chunks that the availability table cannot date, because only Beta 1.7.3 has
an extracted palette. Checking each against the research:

| Block | Verdict |
|---|---|
| `dandelion`, `poppy` | **Authentic.** Alpha 1.0.11's delta is "tree/flower patch generation changed", so flower patches predate 1.0.1. Their *placement* changed in 1.0.11 and that is **U** - patch counts and heights are not documented |
| `short_grass`, `fern` | **Absent, correctly.** Beta 1.6 introduces short grass and ferns, and neither appears in the probe - so the pre-1.8 grass suppression is working |
| **`clay`** | **FIXED AND VERIFIED.** Clay is introduced in **Alpha 1.0.11**, nine builds after 1.0.1, and it was generating in Alpha 1.0.1 worlds. Four placed features can place clay - Moderner Beta's own `moderner_beta:ore_clay` and vanilla's `minecraft:ore_clay`, `minecraft:disk_clay`, `lush_caves_clay` - and all four are now excluded through `HistoricalFeatureExclusions.ALPHA_1_0_1_WITHOUT_CLAY`. **Re-probed: the block is gone, and every other sampled block count is unchanged.** The exclusion could NOT go in the shared pre-Beta list, because Alpha v1.1.2_01 HAS clay; the registry helper now takes an exclusion override, since "this build predates clay" is a fact about the version rather than the era |
| `oak_log`, `red_mushroom`, `sand`, `dirt`, `stone`, `lava`, `water` | Pre-Alpha content, correctly present |

The clay finding is the first anachronism this task has found **in generated terrain** rather than in a rule table, and it
is the case the brief describes: "an otherwise suitable historical generator must not produce" content that did not
exist. Disabling modern content is not sufficient when the foundation preset itself places a later block.

**Where the clay comes from, established from the jars rather than guessed:** Moderner Beta ships its OWN clay feature -
`moderner_beta:ore_clay` (both configured and placed), backed by `BetaOreClayFeature` - alongside vanilla's three
(`minecraft:ore_clay`, `minecraft:disk_clay`, `lush_caves_clay`). All four are placed features, which is what the
exclusion system filters, so all four can be suppressed by id.

**The fix must be per-version.** Alpha v1.1.2_01 HAS clay, so the exclusion cannot go into the shared pre-Beta list that
`terrainOnlyWithRules` picks internally (`BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL`). Alpha 1.0.1 needs its own list, which
means the helper has to accept an exclusion override - a small change, and a justified one, because "this build predates
clay" is exactly the kind of fact that belongs on the version rather than on the era.

## 5b. Alpha 1.0.4, and a conflict the model exposed

Declared with three revisions like Alpha 1.0.1, sharing its mob set and food table - a fact from the deltas, since no mob
and no edible item is added between 1.0.1 and 1.0.4. **Snow and ice are now dated to Alpha v1.0.4**, which their own
generator evidence already said ("snow and ice introduced Alpha v1.0.4 (Winter Update)"); they had been pointed at `alpha`
only because 1.0.4 did not exist yet, so a 1.0.4 world would have been told it lacked blocks it introduced.

**WINTER MODE IS NOT DECLARED, and the reason is the model rather than the research.** Moderner Beta ships
`moderner_beta:alpha_winter`, and this project's own settings preset `alpha_1_0_4_winter` inherits it - so the terrain is
available and nothing needs reconstructing. What is missing is a place to put it:

- `GenerationRevision`s are **append-only improvements**, and `GenerationBackport1211Test` enforces that a version's
  public menu entry selects its **newest** revision.
- Winter mode is an **alternative**, not a successor. Declared as revision 3, the public entry would point at revision 2
  while the newest was revision 3 - which the invariant rejected, correctly.

So the fix is a design decision, not a data entry: either a **variant-version** concept (a distinct profile id for a
distinct world type, as the two Infdev builds already are), or a way to mark a revision **selectable but not newest**.
Recorded as **U** with that choice unmade. The settings preset and the knowledge stay on disk; the public world-type entry
was removed so that no menu offers a world whose recipe no version declares.

## 5c. Mob spawn sets: the audit the owner asked for

Asked "have you checked what mobs can spawn?", and the honest answer was no. The sets existed, one was derived, and a
spawn filter was seen removing chickens at runtime - none of which establishes that a set is RIGHT. Researching it
found a real defect:

| Mob | Arrives | Was |
|---|---|---|
| **cow** | **Alpha v1.0.8** (`beta18-cow-drops`, confidence high) | **wrongly allowed** in Alpha 1.0.1, 1.0.4 and 1.0.6 |
| chicken | Alpha v1.0.14 | correctly excluded from those three |
| slime | Alpha v1.0.11 | correctly excluded from those three |
| enderman, silverfish, cave spider | Beta 1.8 | correctly present from Beta 1.8.1 |
| **mooshroom** | Release 1.0.0, i.e. the Beta 1.9 prereleases | **U** - a Beta 1.8.1 world can still spawn them, because they are a biome-driven cow VARIANT rather than a spawn-list entry, so a spawn set cannot express their absence |

`AlphaMobSetTest` now pins all three directions for the earliest builds: cows absent before 1.0.8, slimes and chickens
absent before 1.0.11 and 1.0.14, and the pre-Alpha mobs (creeper, skeleton, spider, zombie, pig, sheep) still allowed -
because removing those would be the opposite mistake and a silent one.

**A first attempt at this audit was unsound and is recorded as such:** it matched mob names in the delta text, so it
"found" creepers arriving in Release 1.1 from a sentence about a *skeleton killing a creeper*, and it read
`BETA_1_8_1_MOBS` as three mobs because that constant is built by `Stream.concat` and it only captured the added
half. The structured `historical-features.json` answered it properly; the text-matching approach is in
`scripts/audit-mob-sets.py` and should not be trusted.

## 5d. Beta 1.9 Prerelease 1 and 2, and the "they have restrictions" report

The owner asked whether the prereleases were selectable on purpose, then: *"they definitely have restrictions so dont
let them spawn modern 1.21.1 mobs"*. Both parts were right.

**Only Pre3 was declared, and it was declared through `terrainOnly`, which sets NO spawn restriction.** So a Beta 1.9
world spawned modern mobs. Now:

| Version | Spawn set | Note |
|---|---|---|
| Pre1 | Beta 1.8.1's 17 mobs + blaze, magma cube, mooshroom, villager | snow golem deliberately absent: it is **built**, not a natural spawn |
| Pre2 | Pre1's set | adds **no** mob - breeding changes how mobs are made, not which exist |
| Pre3 | Pre2's set | was unrestricted; the research adds no mob here either |

Runtime evidence, Pre1 probe: `restrictedProfiles` now lists all three prereleases, `filterInvocations: 74`, and
`removedEntityTypes: [minecraft:llama]` - a 1.11 mob actually removed from a Beta 1.9 Prerelease 1 world.

**Still open, and the same defect:** every **Release** profile (1.0.0, 1.1, 1.2.5, 1.6.4) is also declared through
`terrainOnly` and therefore has **no spawn restriction** - the research's mob sets for those builds are not extracted,
and `SpawnRestrictionSemanticsTest` deliberately forbids inventing one. This is recorded as **U**: they spawn modern
mobs, and the honest fix is to research their sets, not to copy Pre3's.

### The palette lesson, which cost two wrong attempts

`HistoricalBlockAvailability.judge` answers UNKNOWN for a palette-derived date judged against a version with no
extracted palette, so Pre1's twenty village blocks were unjudgeable. The first fix mapped Pre1 to `b1_9_pre3` in
`PALETTE_FILES` - which **also drives dating**, so it dated `brewing_stand` to Pre1 and would have allowed Pre1
everything Pre2 and Pre3 added. **Dating and membership are different questions** and are now separate lists;
`SHARED_PALETTE_VERSIONS` exists only so the shared-terrain versions are judgeable at all.

Two blocks came out of it, both stated by the research rather than left to the palette:

- `lily_pad` → **Pre1** ("lily pads (visually unnamed/grayscale and non-solid)"). The palette put it at Pre3 - the only
  Beta 1.9 palette extracted, so the **latest** such date, not the earliest - and Pre1/Pre2 were refusing a block their
  build added, the same false-veto shape as redstone ore in Alpha 1.0.1.
- `brewing_stand` → **Pre3**, matching both sources. A village boundary moved to Pre2 last round on the reasoning that
  Pre2 potions imply a stand was **reverted**: the research records potions at Pre2 and the stand at Pre3, a tension
  recorded in the code rather than smoothed over.

## 6. State of the matrix, and what is actually left

**32 profiles declared and selectable**, and **every one of the 29 planned matrix targets is now implemented**:
Beta 1.4, 1.5 and 1.6 were the last three that were named but unplayable, and they are declared. Declaration is
not proof of complete historical fidelity: the remaining behaviour and terrain gaps are recorded below.
[Completion and envelope evidence](<matrix-completion-verification.md>).

The catalogue's remaining `planned` rows with no profile are **none**: all 29 targets resolve to declared
profiles, and `HistoricalVersionCatalogueTest.everyPlannedTargetIsImplementedRatherThanMerelyNamed` fails if
that stops being true.
Their content is real and distinct - beds and repeaters, wolves and cookies, achievements and weather, maps and
trapdoors - and they need the same question answered that Release 1.3-1.5 did, except the answer is likely to be
cleaner: the terrain generator did not change between Beta 1.0 and 1.7.3 (the Adventure Update overhauled it in 1.8),
so `moderner_beta:beta` or `beta_1_1_02` is probably the CORRECT foundation rather than an approximation. **That must
be verified from the research before declaring any of them**, exactly as it was for Release 1.3-1.5, where the answer
turned out to be "no approximation needed" rather than the decision I expected to have to make.

### Open defects, in the order I would take them

1. **Nether enforcement bypass (repaired in this change).** Release spawn sets already existed, but the live
   feature and spawn filters used `historicalAlias(generator)`, which returns null for vanilla Nether generators.
   They now resolve `activeAlias` with the server's historical scope, as structures and block-write guards do.
   Both placed-feature entry points are guarded. Dimension-specific Enderman spawning also needs a separate
   rule: Nether spawning arrived in [16w20a / Release 1.10](https://minewiki.pl/16w20a#1), later than every declared
   profile, even though Endermen themselves exist from Beta 1.8. Ordinary worlds remain unrestricted.
   Regression coverage: [scope tests](<../src/test/java/net/historicalexperience/generation/NetherEnforcementScopeTest.java>)
   and the opt-in `-PgenerationProbe=true -PnetherProbe=true` runtime probe, which scans 49 Nether chunks,
   calls the injected `NaturalSpawner.mobsAt` method and explicitly exercises excluded placed features.
   [Executed evidence](<nether-enforcement-verification.md>): 338 passing tests; Release 1.0 and 1.5
   plus an ordinary control each sampled 49 Nether chunks, with zero historical-block/spawn violations.
   Release 1.5 retained 6,063 quartz blocks; the ordinary control retained modern content with no suppressions.
   Dedicated-server verification and Prism deployment are distinct; no new Prism playthrough claimed.
2. **The bogus prerelease `_no_grass` choices have been removed.** Release and prerelease declarations now use
   `releaseTerrainOnlyWithRules`, with their own spawn sets and only the relevant content revision.
3. **The Beta settings presets stated no envelope (repaired).** Beta 1.0, 1.2 and 1.3 inherited
   `moderner_beta:beta` bare, so their worlds generated at the modern −64..320 height with deepslate: the probe
   measured 8,366 deepslate and 424 emerald ore in nine chunks of a Beta 1.3 world, plus dripstone and lush
   cave biomes. Beta 1.4–1.6 were corrected directly; 1.0, 1.2 and 1.3 gained append-only
   `_historical_envelope` revisions so existing worlds keep their terrain. Every public menu recipe is now
   asserted to declare the envelope. See the evidence table in
   [completion and envelope evidence](<matrix-completion-verification.md>).
4. **The pre-Beta-1.8 spawning ALGORITHM was implemented, then REMOVED at the owner's instruction. Only the
   filtering remains.** The owner asked for "only the spawning algorithm stuff not the filtering" to be taken out,
   so `HistoricalAnimalSpawner`, its `END_WORLD_TICK` registration, its probe section and its tests are gone, and
   `NaturalSpawnerHistoricalSpawnMixin` is back to narrowing the candidate list and nothing else.
   **What was removed, and why it had been built.** The owner reported that an emptied Alpha world never
   repopulated: every entity killed, tick speed 10000, hours of waiting, nothing returned. That was architectural
   — everything this project did about spawning was an allow-list filter over vanilla's candidates, and vanilla's
   modern friendly cycle is a different algorithm, so the candidates were correct and nothing ever asked for them.
   A spawner was written from the jars to fill that hole, and a second defect it exposed was fixed with it (a
   screenshot showed a landscape carpeted with animals where the era's cap is fifteen loaded creatures: the cap
   was compared once per call rather than per placement, and the modern per-chunk animal pack was still running
   beside the cycle). Both were measured and green.
   **What still holds, because it was never the algorithm.** The allow-list filtering is untouched and still
   verified at runtime: an Alpha v1.1.0 world reports `creatureOfferedByBiome = [chicken, cow, pig, sheep]`,
   `creatureAfterHistoricalFilter` the same, `passiveSpawnPossible = true` and 49 filter invocations. So no
   version can spawn a mob its own build lacked — which is what the filtering was always for.
   **The consequence, stated rather than glossed:** an emptied pre-Beta-1.8 world will again stay empty, because
   the era's cycle that repopulated it is the part that was removed. New chunks still fill with the modern
   per-chunk animal pack, which is what the screenshot showed in the first place and is now unfiltered-but-capped
   by nothing. Repopulation is therefore a known, deliberate gap rather than an unnoticed one.
   **The research is preserved, so this is reversible cheaply.** Every cap, radius, attempt count and rule read
   from `alpha-1.1.2_01-client.jar` (sha1 `daa4b9f1…`) and `beta173-client.jar` (sha1 `43db9b49…`), plus the
   Infdev caps read later (monsters 100, animals 50 from `inf-20100420.jar`, sha1 `27010a51…`), is recorded in
   [pre-Beta-1.8 spawning evidence](<pre-beta-1.8-spawning-evidence.json>). Reinstating it is a matter of adding
   the class and the hook back, not of redoing the research.
   `HistoricalAnimalSpawner` reproduces the pre-Beta-1.8 cycle from the jars, parameterised by era:
   Alpha (flat cap 15, radius 4, 3 × 2 attempts, `rand(128)` start height) below the Alpha v1.2.0 rework, and
   Beta (scaled cap `15 × eligibleChunks / 256`, radius 8, 3 × 4) from it onward, both with the `±(rand(6) −
   rand(6))` walk, the 24-block player exclusion, the 576-block spawn exclusion and the entity's own placement
   rule as the `getCanSpawnHere()` equivalent. Driven from `ServerTickEvents.END_WORLD_TICK`, so it costs nothing
   with no player online — which is also faithful, since both jars iterated players.
5. **The newly declared Beta builds' own content is recorded but NOT implemented**: Beta 1.4's locked chest,
   bed-spawn reset and achievement infrastructure; Beta 1.5's weather, powered/detector rails and cobweb
   obtainability; Beta 1.6's map, trapdoor, Nether bed explosion, fire-spread slowdown and ore-quadrant-bias
   fix. These are the next content work rather than claims.
6. **Food stacking before Beta 1.8 was enforced in only one of four places (repaired).** The owner reported that
   food "picks up in a stack" and that "if you double click in inventory it turns into a stack". The rule was on
   `Slot` alone, and disassembling 1.21.1 shows why that leaks: an item's maximum is read directly by three other
   paths. `Inventory.addResource` merges through **`Container.getMaxStackSize(ItemStack)`** — an interface
   *default* method that `Inventory` never overrides — which is what pickup uses;
   `AbstractContainerMenu` reads the item maximum in **six `doClick` sites and two quick-craft helpers**, which is
   what double-click and shift/drag use; and `Inventory.placeItemBackInInventory` reads it too, which is how a
   collected cursor stack is returned. All four are now capped, and `Container`, `Inventory` and
   `AbstractContainerMenu` were added to the probe's mixin smoke test so a wrong redirect target fails the probe
   instead of a player opening an inventory. The item's own component is still deliberately **not** lowered: that
   was tried first and broke recipe data validation (`dried_kelp`, count 9 > maximum 1), which is why the limit
   belongs on containers and slots rather than on the item.
   Verified: build green (340 tests), all four targets report `loaded and transformed` at runtime, probe
   `success=true`. The click behaviour itself still needs an in-game confirmation — a scripted double-click is not
   something this harness can drive.
7. **Sheep wool and mutton (repaired, both pinned from jars).** The owner reported that wool should be given when a
   sheep is *punched* before a certain version, and that sheep should not drop mutton until Release 1.8.
   * **Punch-shear, boundary Beta 1.7.** From `alpha-1.1.2_01-client.jar` (sha1 `daa4b9f1…`, the exact jar for the
     `alpha` profile): the sheep class `bo extends ag` overrides `attackEntityFrom(Entity, int)` — not any
     death-drop method — as `if (!shorn && attacker instanceof EntityLiving) { shorn = true; drop 1 +
     random.nextInt(3) of Item.cloth }`. So punching shorns it for **1–3 wool, once**, and the class has **no**
     death-drop override, so killing an Alpha sheep yielded nothing. Shears are **Beta 1.7**, confirmed by the
     string "shears" being absent from the Alpha jar and present in `beta173-client.jar`, and confirmed
     independently by the owner. The gate therefore covers every declared version older than Beta 1.7.3.
   * **Mutton, boundary Release 1.8.** The string "mutton" occurs **0** times in the language data of
     `alpha-1.1.2_01`, `b1.7.3`, `b1.8.1`, `1.0.0`, `1.1`, `1.2.5` and `1.6.4`, and **6** times in `1.8.9` — so no
     declared version may drop it. The defect was ordering, not a missing rule: the sheep case in
     `HistoricalMobLootHelper.getTable` sat *below* the `hasOldDrops` gate, which is true for only two of the 32
     profiles, so every other version fell through to the **modern** table that contains raw mutton. Hoisting the
     sheep branch above the gate fixes all of them, and both of its branches are mutton-free.
8. **Crafting checkpoints (new, with a stated limit).** The owner asked for these after finding beds craftable in
   an Alpha world. `HistoricalRecipeGate` refuses a crafting result whose block the active version could not have,
   answered by the **same dated availability table the generation veto uses** rather than by a recipe list — so it
   covers beds (Beta 1.3), stone bricks, quartz, hay, anvils, beacons and anything else in that table at once,
   and gains a boundary whenever the table does. Enforced at the two places a result is filled
   (`CraftingMenu.slotChangedCraftingGrid` and `InventoryMenu.slotsChanged`); the recipe registry is deliberately
   **not** filtered, which the project forbids.
   **Limit, recorded rather than hidden: the availability table is a BLOCK table**, so an anachronistic non-block
   *item* is not yet gated — mutton, for one. Closing that needs an item-availability table that does not exist.
   Verified: 4 new unit tests (344 total, green) plus runtime `loaded and transformed` for `CraftingMenu`,
   `InventoryMenu`, `Sheep` and `Container`.
3. **The Nether's terrain SHAPE is still the modern 1.16+ one.** Its biomes are fixed (one biome,
   `minecraft:nether_wastes`, via vanilla's `minecraft:fixed` biome source), but the shape comes from
   `minecraft:nether` noise settings and Moderner Beta ships no historical Nether to point at. Recorded as a gap.
4. **Alpha 1.2.0's own terrain is approximated** by `moderner_beta:alpha`, because Moderner Beta ships no Alpha 1.2.0
   preset. Its twelve-biome set and biome-coloured terrain are therefore not reproduced. Same shape as the Nether gap.
5. **Unimplemented behaviours, each recorded where it was found** rather than in a list nobody reads: leaf decay
   removal and the 10%->5% sapling cut (Alpha 1.0.6), the snow-block economy quirk (1.0.6), the 4->8 dungeon generator
   iterations (1.0.14), the armour model change at Beta 1.9 Pre1, the snowfall-destroys-redstone regression (Pre1,
   fixed by Pre5), slime spawn height (Pre5), Silk Touch changes (Pre5), and breeding and tool repair (Pre2).

### What the last few rounds established, because the pattern matters more than the fixes

Every significant defect found since the audit was found by **the owner playing or pasting a crash**, not by a test:
sugar cane, sneaking, the offhand, cow spawns, food stacking, the portal gap, the modern Nether, and finally a village
crash that proved villages had never generated at all. Two structural lessons came out of that:

- **A server probe cannot see what a client does, and neither can see data validation.** The `Network Protocol Error`
  and the silently dropped recipes were both invisible to 300+ passing tests and to a clean server boot.
- **A guard that only runs in a unit test cannot cover code that needs a bootstrapped registry.** The village style
  check had to become a runtime diagnostic (`villageStyles` in the probe) after the unit-test version died with
  `NoClassDefFoundError: Could not initialize class HistoricalVillageStates`.

The probe now reports, every round: mixin transform results, suppressed blocks and structures, spawn-filter removals,
ore drops, village biome declines, and village style resolution per version.

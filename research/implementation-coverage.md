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

## 6. Next tasks, in order

1. **Alpha 1.0.1** - the pattern version: registry entry via `terrainOnlyWithRules`, its own mob set (no chickens before
   1.0.14, no slimes before 1.0.11), its own food table, dungeon-only structures, ore deltas (gold and diamond twice as
   often), settings and world presets, `normal` tag entry, catalogue row, block-introduction chronology, regenerated
   artefacts, and a generation probe.
2. The remaining Alpha targets, **one at a time**, each from its own deltas in `research/version-transitions.json`.
3. Alpha 1.2.0, which needs the biome-era foundation decided and documented first.
4. The Beta 1.9 prereleases, whose distinctness lives in gameplay gates rather than terrain.
5. The armour model change at Beta 1.8 -> Pre1.
6. Items 3 and 8 from the checklist: twelve references to non-existent Gradle tasks, and the dimension-height mismatch.

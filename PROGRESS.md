# Current work — 2026-10-21 / Gap 26: brewing takes 30 seconds before Release 1.1

Release audit rows R10-10 / X-08 pointed at potions. The strengths half needs machinery the project does not have
(see below); the brew-time half is bounded and sourced:

- research/version-transitions.json, Beta 1.9 Prerelease 3: brewing is "listed at 30 seconds";
- minecraft.wiki, Java Edition 1.1: "Brewing - Reduced brewing time from 30 seconds to 20 seconds."

So Prerelease 3 through Release 1.0 brew in 600 ticks, and Release 1.1 onward in the modern 400.

`BREW_TIME_THIRTY_SECONDS` `[Pre3, Release 1.1)` plus `BrewingStandHistoricalBrewTimeMixin`, a `@ModifyConstant` on
the 400 in `BrewingStandBlockEntity.serverTick` (verified with javap: `sipush 400`). The constant is modified rather
than the field corrected a tick later, because the same pass compares against it. The existing pre-Pre3 brewing cancel
is untouched. Mixin application verified in a client:
`CLIENTMIXINCHECK loaded net.minecraft.world.level.block.entity.BrewingStandBlockEntity ... its mixins applied`.

**Deliberately not done - potion STRENGTHS (Instant Health 6 vs modern 4; Regeneration 1 HP per 25 vs 50 ticks):** the
effect classes (`RegenerationMobEffect`, `HealOrHarmMobEffect`) are stateless singletons with no level, and on a
dedicated server `ActiveVersion.current()` is empty, so there is nothing to resolve a version from. They need a
server-wide profile context first, and Instant Health and Instant Damage share one class whose 4 and 6 constants must
be separated before either is changed. Recorded with those two specific blockers rather than shipping a 50/50 guess.

**Evidence:** `./gradlew cleanTest test` - **818 tests, 0 failures**; `BrewTimeTest` pins the window on both sides and
the ordinary-world exemption; golden, matrix and facts regenerated.

---
# Current work — 2026-10-21 / Gap 25: the five-tick block-break delay before Release 1.3.1

Release audit row R10-26, with the wiki line: Java Edition 1.3.1 - "The 1/4 second (5 game tick) delay between block
breaking has been removed for players in Survival." Modern Minecraft has no such delay, so every declared build before
Release 1.3 (this project's 1.3.1 baseline) should refuse a second survival break within five ticks.

- Feature `BLOCK_BREAK_DELAY` `before(Release 1.3)`.
- `HistoricalBlockBreakDelay` holds the pure decision (`shouldRefuse(version, survival, now, lastBreak)`) and the
  per-player last-break bookkeeping; `ServerPlayerGameModeHistoricalBreakDelayMixin` refuses at HEAD and records only a
  break that actually happened (at RETURN), so a failed break does not start a cooldown. Creative players are exempt.
- **Scope, stated as an inference:** the wiki documents the REMOVAL and no introduction, so the delay is applied to
  every earlier declared build. That reasoning is written in the feature's evidence rather than being presented as a
  dated fact.
- Mixin application verified in a real client: `CLIENTMIXINCHECK loaded net.minecraft.server.level.ServerPlayerGameMode
  (ServerPlayerGameMode) - its mixins applied`.

**Also this round:** pressure plates were investigated first and left alone. The 1.3 row says plates "now require only
two planks/stone ... previous versions required three", and modern plates are two-blocks-to-ONE-plate; nothing states what
the pre-1.3 recipe OUTPUT was, so it stays recorded as UNKNOWN instead of being given a guessed count.

**Evidence:** `./gradlew cleanTest test` - **817 tests, 0 failures**; `BlockBreakDelayTest` pins the five-tick boundary,
the creative/first-break/ordinary exemptions and the bookkeeping; golden, matrix and facts regenerated.

---
# Current work — 2026-10-21 / Gap 24: slab and ladder output counts before Release 1.2.1

This was on the "count changes could not be sourced" list, but the Release audit's own row (X-09) carries the wiki
line: minecraft.wiki, Java Edition 1.2.1 - "Crafting recipe for slabs now yields 6 instead of 3"; "Ladders: ...
Crafting recipe now yields 3 ladders instead of 2". Since this project's Release 1.2.5 profile IS the 1.2.1 baseline,
every declared build before it crafted three slabs and two ladders.

- Feature `LEGACY_SLAB_LADDER_COUNTS` `before(Release 1.2.5)`.
- Four supplied recipes behind it: oak, cobblestone and sandstone slabs (three blocks in a row -> three slabs) and the
  ladder's seven-stick shape -> two ladders. The modern recipes are refused in the window, so the game has exactly one
  answer per version rather than both.
- One existing test updated deliberately: its "Alpha can still craft" sweep expected the vanilla ladder recipe, which is
  now refused in favour of the supplied shape - the same treatment the arrow got, with the reason stated in the test.

**Evidence:** `./gradlew cleanTest test` - **814 tests, 0 failures**; `SlabLadderCountsTest` pins the window, the counts,
the ingredients and the refusal on both sides of 1.2.5; golden, matrix and facts regenerated.

**Recorded instead of guessed:** the pressure-plate count row (1.3: "require only two planks/stone") does not state the
pre-change OUTPUT, so it stays open; the stone-slab item substitution remains open for the substituted-result path;
boats and maps need mechanic work (boat break-on-land and the single pre-1.4 map item), and block-break delay still needs
its rule read.

---
# Current work — 2026-10-21 / Gap 23: signs and buckets before Release 1.3.1

Two sourced rows from the Release audit, both with the wiki line quoted in the code:

- **Signs**: minecraft.wiki, Java Edition 1.3.1 - "Signs: Stack up to 16 now. Crafting now gives 3 signs instead of 1."
  Release 1.0.0, 1.1 and 1.2.5 crafted ONE sign from six planks and a stick. Added `SIGN_RECIPE_ONE` and a
  `RemovedRecipes` entry supplying the one-sign shape while refusing the modern three-sign recipe in that window.
- **Empty buckets**: the same wiki row - "Empty buckets: Stack up to 16 now." Before 1.3.1 a bucket stacked to one.
  The existing shared stack-limit hook (which already confines food pre-Beta-1.8) gained the bucket rule, so the six
  call sites that keep client and server agreeing get it automatically. Its javadoc now records the second rule rather
  than leaving the bucket case unexplained by the class name.

**Mistake caught by the test, and how:** the first version of the sign entry was defined but never added to
`RemovedRecipes.all()`, so the recipe was inert. The new test failed on exactly that assertion; the cause was a
string replacement whose prefix did not match, which I had not asserted. Fixed, and the accidental duplicated javadoc
line from the same edit removed.

**Evidence:** `./gradlew cleanTest test` - **812 tests, 0 failures**; `SignAndBucketEraTest` pins the window, the recipe
shape, the refusal and both stack sizes (bucket one before 1.3.1, modern after, bread still one pre-1.8); golden, matrix
and facts regenerated.

**Still open from the audit's inventory list:** the pre-1.4 map item (a single map item rather than the modern
empty/filled pair and zoom), which needs a dynamic item id the recipe mechanism cannot express; boats; and
block-break delay.

---
# Current work — 2026-10-21 / Gap 22: no achievements before Beta 1.5

research/version-transitions.json carries a per-build "achievements" mechanic table, which corrected the audit note I
was working from ("none until 1.4"): Alpha v1.0.1 "through Beta 1.3" has none (matrix row "A"), **Beta 1.4** has trigger
"infrastructure ... but the complete achievement set/UI arrives incrementally", and **Beta 1.5** introduces "16
achievements and statistics" with "an achievement/statistics UI". So modern advancement toasts and the modern screen in
a Beta 1.3/1.4 world are wrong content.

- Feature `NO_ACHIEVEMENTS` `before(Beta 1.5)`.
- `PlayerAdvancementsHistoricalMixin` cancels `award(...)` at HEAD, so nothing is recorded - filtering at the return
  would still fill the advancement screen behind the suppressed toast. The version is resolved from the awarding
  player's level, not from the empty server session.
- Mixin application verified in a real client: `CLIENTMIXINCHECK loaded net.minecraft.server.PlayerAdvancements
  (PlayerAdvancements) - its mixins applied`.

**Deliberately not done:** the era's own achievement lists above Beta 1.5. Mapping sixteen Beta 1.5 achievements (and
the later ten) onto modern advancement ids is a design table the corpus explicitly says "needs a per-page audit"; until
then those versions keep the modern advancement set, recorded as an open gap rather than replaced with a guess.

**Evidence:** `./gradlew cleanTest test` - **810 tests, 0 failures**; client probe line above; golden, matrix and facts
regenerated; docs status board updated.

---
# Current work — 2026-10-21 / Gap 21: the hardcore gate, runtime-proven in both directions

Hardcore mode arrives in Beta 1.9 Prerelease 2 (research/version-transitions.json: "Hardcore mode (locks difficulty to
Hard, permadeath)"), yet vanilla hardcore was selectable and effective in every profile. Implemented:

- feature `NO_HARDCORE` `before(Pre2)`;
- a `PrimaryLevelData.isHardcore` override that answers false for a gated build, and a small registry
  (`HistoricalHardcore`) noted once at server start. The stored level flag is never edited - only the answer changes -
  and `/hex status` now prints `hardcore: effective=...` so the decision is observable.

Getting there took three runtime corrections, all caught by probing rather than by tests:

1. The first version consulted `ActiveVersion.current()`, which is EMPTY on a dedicated server (the session is
   client/world-join state), so nothing was gated.
2. `PrimaryLevelData` has no server or level to resolve a version from, which is why the gate is noted by the lifecycle
   instead.
3. The lifecycle runs at `SERVER_STARTING`, where `server.overworld()` is **null** (levels are created afterwards) - the
   first probe crashed the server with exactly that NPE. The note now uses `server.getWorldData()` (cast through
   `LevelData`), and falls back to reading the metadata store because a NEW world establishes its metadata during the
   same hook, so the in-memory state can still be "Normal" on first boot.

**Runtime evidence** (`/hex status` over RCON, fresh hardcore-flagged worlds):

- Beta 1.7.3: `Historical hardcore gate: profile=historical_experience:beta_1_7_3 gated=true stored=true` then
  `hardcore: effective=false (this build predates hardcore; ...)`;
- Release 1.6.4: `... gated=false stored=true` then `hardcore: effective=true`.

**Evidence:** `./gradlew cleanTest test` - **809 tests, 0 failures**; golden, matrix and facts regenerated; docs status
board updated.

**Still open for this item:** the world-creation SCREEN still offers hardcore for a pre-Pre2 profile. Server authority
stops hardcore behaviour, but hiding the option is a client screen change and is recorded rather than claimed.

---
# Current work — 2026-10-21 / Gap 20: the creeper's conditional music-disc drop

The last loot shape that was missing was a CONDITIONAL drop: a disc only when a skeleton kills the creeper, from a set
that grows over time. The wiki's Creeper history gave three exact rows, and one of them corrected an assumption I would
have made: "v1.0.14 Added music discs '13' and 'cat', which are dropped by creepers shot by skeletons"; "1.1 (11w50a)
Creepers now drop all music discs, except for '11'"; "1.4.4 (1.4.3) Creepers now drop music discs '11' and 'wait'". So:

- v1.0.14 through Release 1.0: 13 and cat only;
- Release 1.1 through the 1.4 profile (1.4.2, before 1.4.3): ten discs, excluding "11";
- Release 1.5 and 1.6.4: twelve, adding "11" and "wait".

`HistoricalDiscDrops` is pure and unit-tested (window, sets, random pick); one shared mixin injects at the tail of
`LivingEntity.die` and adds the disc when the source's entity is a Skeleton and the world is a server level. The disc is
ADDITIVE to gunpowder - the rows do not say so explicitly, but the runtime probe confirms vanilla's extra-pool shape.

**Runtime evidence** in a Beta 1.7.3 world (RCON): a creeper killed with
`/damage ... minecraft:mob_attack by <skeleton>` dropped `minecraft:gunpowder` **and** `minecraft:music_disc_13`; a
creeper killed with `minecraft:player_attack` dropped `minecraft:gunpowder` only.

**Evidence:** `./gradlew cleanTest test` - **808 tests, 0 failures**; server booted with no mixin errors; docs status
board updated; golden, matrix and facts regenerated.

**Still open:** potion effect tables, achievements, the Pre3 item form, slab counts/substitution, boats/maps/signs,
pre-1.9 AI, hardcore gate, and the evidence-missing list.

---
# Current work — 2026-10-21 / Gap 19: "dropped nothing" is now expressible, and runtime-proven

The audit listed the ghast as dropping modern items in Alpha and Beta. Checking the project's own build records changed
the date: ghast tears are **Beta 1.9 Prerelease 1** content ("blaze rods, ghast tears and Nether wart are obtainable
but have no use yet"; the fortress row says they "arrive" there), not Beta 1.7 as I had earlier assumed.

- `HistoricalLootRules.dropsNothing(profile, entity)` now answers "this mob dropped nothing in this version", which is
  a different fact from "no rule, use the modern table". The helper checks it before the rule lookup and returns an
  empty table. The rule set is built lazily: computing it in the static initialiser would have asked the version
  registry that constructs this class - a real init cycle I hit and fixed.
- Ghast drops nothing for every declared build before Pre1.
- Squid ink sacs are Beta 1.2, but squid is Beta 1.2 content too, so there is no earlier declared build to suppress for.

**Runtime evidence** (a probe, not a unit test): a server at `historical_experience:beta_1_7_3` had a ghast summoned and
killed with RCON - `execute if entity @e[type=minecraft:item]` answered **Test failed** and `data get entity` found no
item. Positive control in the same world: a zombie killed instead produced `{count: 1, id: "minecraft:feather"}`. So the
suppression is real and drops still happen.

**Evidence:** `./gradlew cleanTest test` - **805 tests, 0 failures**; docs status board updated (the ghast row moved from
mechanism-missing to implemented); golden, matrix and facts regenerated.

**Still open:** conditional second drops with a probability (the music-disc rule), potion effect tables, achievements,
the Pre3 item form, slab counts/substitution, boats/maps/signs, pre-1.9 AI, hardcore gate, and the evidence-missing list.

---
# Current work — 2026-10-21 / Gap 18: the client mixin is verified in a real client

The debug-overlay mixin (audit A-06, no coordinates before Alpha v1.2.3) was unit-tested but unverified: a dedicated
server never loads `DebugScreenOverlay`, so the boot check that caught the invalid dragon injection could not see it.

The project already had the right tool: `ClientMixinCheck` (`-PclientMixinCheck=<class>`) forces a named client class to
load on the first tick with a screen present; an invalid injection makes the load itself throw, so loading IS the test.
Run:

`./gradlew runClientQuickPlay -PtitleStart -PnoFlashback=true -PclientMixinCheck=net.minecraft.client.gui.components.DebugScreenOverlay`

Result, from the client's own log:

`[Render thread/INFO] (historical_experience/client-mixin-check) CLIENTMIXINCHECK loaded net.minecraft.client.gui.components.DebugScreenOverlay (DebugScreenOverlay) - its mixins applied`

So the third and last verification route is now in use for client code: unit test for the logic, class-load probe for
mixin application, server boot for server-side behaviour. What remains unproven for this item is only the in-game
APPEARANCE (F3 pressed in a historical world), which the class-load probe does not exercise.

Operational note: the probe command killed the client at the end and the tool runner reported a job error while doing
so; the log was already complete and no game process was left running. Next time I will stop the game JVM from a
separate call so the harness does not lose the result.

**Evidence:** client log line above; `./gradlew cleanTest test` - **802 tests, 0 failures**; docs status board updated.

---
# Current work — 2026-10-21 / Gap 17: all five 256-height revisions are runtime-verified and accepted

The previous round accepted Release 1.2.5 only. This round ran the same probe for the other four builds:

| Build probed | Result |
| --- | --- |
| Release 1.3 | PASS 0..255 (y=0/1/255 changed, y=256 and y=-1 out of world) |
| Release 1.4 | PASS 0..255 |
| Release 1.5 | PASS 0..255 |
| Release 1.6.4 | PASS 0..255 |

Each ran `scripts/verify-release-height.ps1 -LevelType historical_experience:<build>`, which boots a dedicated server on
that build's world preset, force-loads a chunk and asks the server itself. `HistoricalVersion.ACCEPTED_REVISIONS` now
holds all five height aliases and the `_height_256` revisions are no longer marked experimental.

The script was also improved: it derives a per-build world and log name from the level type, so one build's evidence
cannot overwrite another's (`run/heightprobe-<slug>.log`). The 1.5 and 1.6.4 probes were run after that improvement and
have per-build logs; 1.2.5/1.3/1.4 used the single log path before it, and the script reproduces any of them on demand.

**Evidence:** `./gradlew cleanTest test` - **802 tests, 0 failures**; probes exit 0 with PASS transcripts; golden, matrix
and facts regenerated; `run/server.properties` restored by the script each time.

**Still open:** client-launch verification for the client mixins; and the status board's evidence-missing /
mechanism-missing lists (armour formulas, spawn weights, XP curve, potions, achievements, Pre3 item form, slab counts,
boats/maps/signs, AI).

---
# Current work — 2026-10-21 / Gap 16: the 256-height revision is VERIFIED at runtime, and acceptance becomes expressible

The audit item G8 was implemented a few rounds ago but only unit-tested. This round generated a real world on it and
asked the server where the world ends.

**Probe (repeatable: `scripts/verify-release-height.ps1`).** A dedicated server was started with
`level-type=historical_experience:release_1_2_5`, which points at the `_height_256` revision. Evidence:

- `/hex status` reported `generation preset: historical_experience:release_1_2_5_height_256`, the newest revision as
  `release_1_2_5_height_256`, and **all three** stored generator slots as that revision;
- RCON `setblock` results: **y=0, y=1, y=127, y=128 and y=255 all "Changed the block"**, while **y=256 and y=-1 both
  answered "That position is out of this world!"** - a genuine 0..255 envelope, not a 128 one;
- the control probe for an older build is the same script with `-LevelType historical_experience:release_1_1
  -ExpectedMaxY 127`.

**An architectural gap found on the way, and fixed.** Both `GenerationRevision` and `GenerationBinding` enforced
"a numbered revision must be experimental until it is accepted" - but nothing could express acceptance at all, so the
flag could never legally become false even for a revision that had been probed. Added
`HistoricalVersion.ACCEPTED_REVISIONS`, a small reviewed allow-list; the two invariants consult it, and
`HistoricalVersionsTest` now asserts the flag equals "not on the accepted list". Release 1.2.5's height revision is on
it after the probe; the other four stay experimental until each is probed. Two mistakes of mine surfaced while doing
this and were caught by the suite: the binding compares a `HistoricalIdentifier`, not a String, and I initially trusted
stale test XML instead of a clean run.

**Evidence:** `./gradlew cleanTest test` - **802 tests, 0 failures**; the probe script exits 0 with a PASS transcript;
golden, matrix and facts regenerated; `run/server.properties` is restored by the script's own finally block.

**Still open:** the same probe for 1.3/1.4/1.5/1.6.4; client-launch verification for the client mixins; and the rest of
the status board's evidence-missing and mechanism-missing lists.

---
# Current work — 2026-10-21 / Gap tracking: the audit document becomes a status board

After fifteen implementation slices, docs/VERSION_GAPS.md still presented every audit finding as open and carried a
stale backlog. Rewritten as a status board:

- an **Implemented** table (17 rows) naming what changed, what was wrong, and the test that pins each one;
- **Open, with the reason** split into three honest classes: evidence missing (armour formulas, spawn weights, the
  pre-1.3.1 XP curve, the 1.6.x crit formula, cake hunger, glistering melon/fire charge), mechanism missing (the Pre3
  frame item form, "no drop" and conditional second drops, potion tables, achievements, boats/maps/signs/block-break
  delay, slab counts and substitution, AI), and **verified only by unit test** (the 256-height revisions, client
  mixins, mixin application generally);
- a "checked and correct" section so the same ground is not re-audited;
- the full evidence remains in research/gaps/, which this document links rather than duplicates.

**Why the Pre3 frame item form was not implemented this round.** The tooltip half is easy, but the visible half is a
flat filled-frame ITEM model. The texture system routes assets from pinned dependency packs per version and has no way
to serve a mod-authored per-version model, and the frame item is creative-only in Pre3, so a tooltip-only change would
marry a correct tooltip to a wrong sprite. It is recorded under "mechanism missing" instead of half-done.

**Evidence:** `./gradlew test` - **802 tests, 0 failures**.

---
# Current work — 2026-10-21 / Gap 15: no coordinates on the debug overlay before Alpha v1.2.3 (A-06)

minecraft.wiki, Debug screen history (raw wikitext, fetched 2026-10-21): "v1.2.3: The coordinates have been added to the
debug screen." The same page dates the F3 hold to v1.1.1 and the lagometer to v1.2.0; those are input/feature
differences this gate does not model, and they are recorded rather than implied.

- Feature `NO_DEBUG_COORDINATES` `before(Alpha v1.2.3)`, with a new `ALPHA_1_2_3` milestone.
- The filtering is pure and unit-tested: `HistoricalDebugScreen.filter(versionId, lines)` drops the line whose
  untranslated label begins `XYZ:` and leaves every other line in order; `HistoricalDebugScreenTest` pins both the
  window and the filtering, and asserts that a normal version's list is returned untouched (same instance).
- One client mixin, `DebugScreenOverlayHistoricalCoordinatesMixin`, `@ModifyReturnValue` on
  `DebugScreenOverlay.getGameInformation`.

**Verification limit, stated plainly:** this is a CLIENT mixin, and a dedicated server never loads
`DebugScreenOverlay`, so the server boot that verified the last two mixins cannot verify this one. The hook's target and
return type were read from the mapped class with javap, and the logic is unit-tested, but mixin application itself
needs a client launch. That is the difference between "tested" and "verified", and it stays on the open list until a
client run happens.

**Evidence:** `./gradlew test` - **802 tests, 0 failures**; golden, matrix and facts regenerated.

**Still open:** potion strengths, XP curve/sources, achievements, block-break delay, boats, maps, signs, stone-slab
substitution, slab/ladder/plate output counts, the Pre3 frame item form, and a client launch to confirm the debug hook.

---
# Current work — 2026-10-21 / Gap 14: the redstone lamp is Release 1.2.1 content

minecraft.wiki, Redstone Lamp history (raw wikitext, fetched 2026-10-21): "1.2.1 (12w07a) Added redstone lamps". The
project's Release 1.2.5 profile is that line, so the lamp belongs there. It was in the Release 1.3+ bucket, so a
Release 1.2.5 world refused it while the creative catalogue already dated it correctly - the exact internal contradiction
the audit reported.

Moved to `from("release_1_2_5", "redstone_lamp")`, with the wiki row quoted in the code. Release 1.1 still refuses it,
1.2.5 and later allow it; pinned in `ReleaseGearedMobsAndToolsTest`.

**Checked while here, and deliberately not changed:** the audit's A-11/A-13 slab items. The facts sheet shows no slab
being refused in Beta 1.3 - the block-date gate does not currently reject `oak_slab` there - so the remaining slab
issues are the OUTPUT COUNT and the `smooth_stone_slab` substitution, exactly the two I could not source well enough
earlier. They stay recorded as open rather than guessed, and `oak_slab`'s 1.6.4 date in the block table is noted as
inconsistent with that but has no observed effect today.

**Evidence:** `./gradlew test` - **800 tests, 0 failures**; golden, matrix and facts regenerated.

**Still open:** potion strengths, XP curve/sources, achievements, block-break delay, boats, maps, signs, the F3 overlay,
stone-slab substitution, slab/ladder/plate output counts, and the Pre3 frame item form.

---
# Current work — 2026-10-21 / Gap 13: cake heals 3 per slice before hunger (Beta 1.2-1.7.3)

research/minecraft-version-history.md: "Beta 1.2 cake (3 health per slice, 18 for the whole cake)". Cake is the only
pre-hunger food whose heal happens through a BLOCK interaction, so the item-food heal table could not cover it and a
Beta 1.2-1.7.3 cake restored nothing.

- Feature `CAKE_SLICE_HEALS` `[Beta 1.2, Beta 1.8.1)` and `HistoricalFoodRules.cakeSliceHeal` (3.0F, pure and tested).
- One shared mixin, `CakeBlockHistoricalHealMixin`, with two redirects inside vanilla's own `CakeBlock.eat` so the bites
  count, eating sound, block-state update and statistic stay vanilla's: `Player.canEat` is bypassed (no hunger bar to be
  full) and `FoodData.eat` becomes `player.heal(3)`. Both redirect targets were read from the mapped class with javap
  before being written, and the mixin applies - verified by a dedicated server boot, `Done (2.663s)`, no mixin errors.

**Why the boot matters here specifically:** redirect injection points are only validated when the target class loads,
and `CakeBlock` loads through the block registry at server start. That is the same check that caught the invalid dragon
injection last round.

**Evidence:** `./gradlew test` - **799 tests, 0 failures**; golden, matrix and facts regenerated.

**Still open:** potion strengths, XP curve/sources, achievements, block-break delay, boats, maps, signs, the F3 overlay,
the stone-slab substitution, slab/ladder/plate counts, `redstone_lamp` in 1.2.5, and the Pre3 frame item form.

---
# Current work — 2026-10-21 / Gap 12: the Beta 1.5 damage bug, and a runtime-verified correction

**The damage bug is implemented.** research/feature-timeline.json, Java Edition history: Beta 1.5 "All attacks from
players now deal 1 more damage than intended unless the player is currently moving upwards in singleplayer"; Beta 1.8
Pre-release "Player attacks no longer deal 1 more damage than intended". The same builds raised unarmed damage 1 -> 2 and
lowered it back, so one extra point reproduces both statements without a second rule.

- Feature `BETA_DAMAGE_BUG` `[Beta 1.5, Beta 1.8.1)`.
- The decision is pure and unit-tested: `HistoricalDamageValues.betaDamageBugBonus(versionId, movingUpwards)`.
- One shared mixin applies it at `LivingEntity.hurt(DamageSource, float)`, only when the source's DIRECT entity is a
  player (so arrows are untouched, which is what "attacks" means) and the player is not moving upwards.

**A round-9 defect the runtime caught, which the tests could not.** Booting a dedicated server after adding the mixin
showed the earlier dragon work was broken: `EndDragonFight.findOrCreateDragon` is **void**, so the
`CallbackInfoReturnable<EnderDragon>` injection I wrote was an invalid descriptor and crashed the server during mixin
application:

`InvalidInjectionException Invalid descriptor ... Expected (CallbackInfo)V but found (CallbackInfoReturnable)V`

Every unit test passed, because none of them loads `EndDragonFight`. The injection is now a plain `CallbackInfo` with
`cancel()`, which is what suppressing a void creation method means, and the server boots to `Done (2.749s)` with no
mixin errors. This is recorded as a correction rather than edited out of history: the earlier entry's claim was based on
tests alone, and the boot is the evidence that matters for a mixin.

**Evidence:** `./gradlew test` - **798 tests, 0 failures**; dedicated server boots with 0 mixin failures; golden, matrix
and facts regenerated.

**Still open:** cake healing, potion strengths, XP curve/sources, achievements, block-break delay, boats, maps, signs,
the F3 overlay, the stone-slab substitution, slab/ladder/plate counts, `redstone_lamp` in 1.2.5, and the Pre3 frame item
form.

---
# Current work — 2026-10-21 / Gap 11: cookies stack to 8 (Beta 1.4-1.7.3)

Before Beta 1.8 no food stacks - but the cookie is the documented exception: a Beta 1.7.3 cookie is the same item in a
dedicated subclass whose only purpose is a stack size of 8 (`research/beta173-food-healing.json`: "Beta cookie saved in
the `qk` subclass purely to raise stack size to 8"; `research/infdev-registries.json` shows the constructor's last
argument as 8). The project's blanket "food does not stack" cap made a Beta 1.4-1.7.3 cookie unstackable.

Implemented as feature `COOKIE_STACKS_EIGHT` `[Beta 1.4, Beta 1.8.1)` plus `HistoricalFoodStackLimit.historicalStackLimit`,
which returns 8 for a cookie in that window and 1 for every other limited food; all six call sites (container, slot,
menu's four paths, inventory) now ask it, so the four hooks cannot disagree - the reason that helper exists in the first
place. New `BETA_1_4` milestone.

`CookieStackLimitTest` pins the window and the actual limits on a bootstrapped registry: cookie 8 at Beta 1.7.3, 1 at
Beta 1.3, 1 for bread, 1 in an ordinary world.

**Evidence:** `./gradlew test` - **796 tests, 0 failures**. Golden, matrix and facts regenerated. The first pass was
committed only after the compiler and the tests caught two of my own mistakes: the milestone name did not exist, and the
slot mixin's no-argument method has no `stack` variable.

**Still open:** the player damage +1 bug (Beta 1.5-1.7.3, wiki quote already recorded in the timeline), cake healing,
potion strengths, XP curve/sources, achievements, block-break delay, boats, maps, signs, the F3 overlay, the stone-slab
substitution, slab/ladder/plate counts, `redstone_lamp` in 1.2.5, and the Pre3 frame item form.

---
# Current work — 2026-10-21 / Gap 10: geared monsters from Release 1.4, compass/clock dates (G9, A-10)

**G9 - geared monsters.** Armour, weapons, item pickup and spawn enchantments for monsters all arrive in Release 1.4.2
(12w32a), and this project's Release 1.4 profile IS the 1.4.2 stable baseline. The three gates were proxied by Release
1.6.4, so Release 1.4 and 1.5 spawned ungeared monsters while 1.6.4 stood in for a boundary it is not. Windows moved to
`before(RELEASE_1_4)`, evidence strings rewritten to say so (they still described the old proxy), and the boundary test
corrected. Baby zombies are a different boundary - Release 1.6.2, whose only declared successor is 1.6.4 - so that prox
is correct and was left alone; the test now asserts the two cases separately.

**A-10 - compass and clock.** Both sat in the allow-list's `always()`, so they were craftable from Alpha v1.0.1, while
the project's own creative catalogue dates compass to Alpha v1.1.0 and clock to Alpha v1.2.0. Moved out of `always()`
to `from("alpha_1_1_0", "compass")` and `from("alpha_1_2_0", "clock")`, so the two tables now agree.

**Evidence:** `./gradlew test` - **794 tests, 0 failures**. `ReleaseGearedMobsAndToolsTest` pins both boundary
directions and the three crafting dates; golden, matrix and facts regenerated.

**Still open from the audits' Release-era list:** the player damage +1 bug in Beta 1.5-1.7.3, cookie stacking from Beta
1.4, cake healing, potion strengths, the XP curve and its sources, achievements as modern advancements, block-break
delay, boats, maps, signs, the F3 overlay, the stone-slab item substitution, slab/ladder/plate output counts,
`redstone_lamp` in Release 1.2.5, and the Pre3 item form of the End portal frame.

---
# Current work — 2026-10-21 / Gap 9: the End's boundaries, and the Pre3 frame's art

**Both End boundaries implemented (audit G10):**

- **No Ender Dragon before Prerelease 6.** New feature `NO_ENDER_DRAGON` (`before` Pre6), and
  `HistoricalDragonFight.noDragon` gates `EndDragonFight.findOrCreateDragon` to null before it. The mixin's target was
  found in the mapped jar, not guessed. Pre4 and Pre5 now have the End they actually had: the central island and the
  obsidian pillars, no boss and no boss bar. Pre6 keeps its milestone.
- **Prerelease 3 fills frames but opens nothing.** New feature `END_FRAME_EYE_REPAIR` (Pre3 only). The eye of ender is
  placed on the frame, consumed, sounded and the frame's `HAS_EYE` state set - and no portal logic runs, which is the
  wiki's "nothing further occurs". Before Pre3 the eye still does nothing (`INERT_END_PORTAL`), and from Pre4 vanilla
  completes the portal normally. New `BETA_1_9_PRE_6` milestone.

**Owner question: were the Pre3 frame's texture and models copied over?** No - and deliberately so, because nothing
had to be. The project already routes the pre-redraw frame art for every version **before Pre4**: the
`END_PORTAL_FRAME_AFTER_REDRAW` rule serves four textures AND two models from the pinned Golden Days pack, and
`Beta_1_9_pre_3` is inside that window. Checked rather than assumed, because "already routed" is not "correct for a
placed eye":

- vanilla 1.21.1's `blockstates/end_portal_frame.json` maps `eye=true` to `block/end_portal_frame_filled`, and that is
  exactly one of the two models the rule routes - which is why the eye shows the era's art when placed;
- the pinned pack's contents were verified by downloading it and matching the recorded SHA-512, so the models and the
  eye texture are known present rather than hoped for;
- the texture test now asserts **every** frame asset (textures and both models) resolves to Golden Days at Pre3, not
  just the side texture, and to vanilla from Pre4.

The packs are All Rights Reserved and are fetched on the player's own machine from their official distribution point;
copying their files into this repository is not something the project may do, which is another reason the existing
routing is the right mechanism.

**Not done:** the Pre3 **item form** - the wiki says the frame item was a flat texture of the filled frame with an empty
tooltip, which is an item-model/tooltip change and remains recorded as open. And the placed-eye appearance has not been
checked in a running client, only proven at the resource-selection level.

**Evidence:** `./gradlew test` — **792 tests, 0 failures**. Golden, matrix and facts regenerated.

---
# Current work — 2026-10-21 / Gap 8: the Pre2/Pre3 boundary shift (audit G11)

The Adventure era audit called this "one structural error behind many rows": several behaviours were gated at
Prerelease 3 although the project's own `research/version-transitions.json` says Prerelease 2, and several Pre1 items
were dated Pre3 because Pre3 is the first palette that contains their names.

**Animal breeding and tempting moved to Prerelease 2.** The transitions entry for Pre2 says "animal breeding using wheat
for cows, mooshrooms, sheep, pigs and chickens"; the window had always ended at Pre3, and the feature's own evidence
string admitted the disagreement ("the boundary is preserved as the code had it"). Per the architecture contract,
correcting a preserved disagreement is a deliberate change with its own golden diff - this is that change. Pre2 and
everything after now have breeding; Beta 1.8.1 and Pre1 do not. `NO_ANIMAL_TEMPTING` moved with it, as its evidence
always said it should.

**Pre1's own additions are craftable in Pre1**: nether bricks, nether brick stairs and fences, and gold nuggets. They
were dated Pre3 (palette first sighting). **Pre2's potion ingredients** - glass bottles, blaze powder, fermented spider
eye, magma cream - are craftable in Pre2; only the brewing stand itself stays Pre3, which is what the transitions rows
say. Spider eyes are deliberately NOT in the allow-list: they are a mob drop with no recipe, and the allow-list is a
list of recipe results (a test enforces that, and caught the first version of this change).

**Two block dates corrected** in the generator: nether wart is Pre1 content and end stone is Pre4 content, both of
which the table had dated Release 1.0 - the same two misdates the era audit reported, and both were being judged
anachronistic in the very builds that added them.

**Evidence:** `./gradlew test` — **788 tests, 0 failures**. `Pre2Pre3BoundaryTest` pins the windows, the craftability
direction on both sides of every boundary, and the two block dates. Golden, matrix and facts regenerated.

**Still open in this item:** the Pre2 cauldron-based brewing prototype (the modern game needs a stand, so no brewing
is possible in Pre2 even though the potion system and its ingredients are there - recorded rather than faked); the
first Pre2 music discs; glistering melon and fire charge, whose dates the audit could not move to Pre2 on its evidence;
splash potions; and the dragon's Pre4-vs-Pre6 boundary (audit G10), which is a separate item.

---
# Current work — 2026-10-21 / Gap 7: 256-height Release worlds (audit G8)

minecraft.wiki, Java Edition 1.2.1 (12w07a): the build limit was raised from 128 to 256. Every Release preset in this
project used `historical_experience:beta_1_7_3_overworld` (min_y 0, height 128), so a Release 1.2.5-1.6.4 world could
not be built above y=127 even though the build had 256. Release 1.0/1.1 are correctly 128.

Implemented as an **appended generation revision**, not an edit, because revisions are append-only and a new world
selects the newest one:

- new dimension type `historical_experience:release_1_2_overworld` (min_y 0, height 256, logical_height 256, the rest
  identical to the 128 type);
- `withHeight256` appends revision `<key>_height_256` for **release_1_2_5, 1_3, 1_4, 1_5 and 1_6_4**, copying the
  previous revision's foundation, exclusions and ore population - only the engine-enforced envelope changes, because
  the terrain generator did not change in 1.2.1, the space above it simply became buildable;
- each new revision has a settings-preset mirror and a world preset, and the world-creation preset for those five
  versions now names the height revision and the 256 dimension type, so the menu cannot hand a player the old envelope.
- The revision is marked experimental, per the record invariant: numbered revisions stay experimental until a world has
  actually been generated on them. **Runtime validation is therefore still pending** - the unit tests prove the data is
  consistent and the presets resolve, not that a world boots to y=255.

Tests updated deliberately, each with a comment: the era-ore tests now find the `_era_ores` revision **by alias**
rather than by position (a later revision is allowed), the envelope-count assertion moves 132 -> 137 (five more settings
presets carry the 128 terrain envelope, which is terrain and not build limit), and the world-creation test accepts the
256 envelope for the raised-limit profiles. `ReleaseHeightTest` pins the revision, the dimension type and every preset.

**Evidence:** `./gradlew test` — **783 tests, 0 failures**. Golden, matrix and facts regenerated.

**Open:** runtime generation on a `release_1_2_5` world at the height revision (needed before the revision can be
un-marked experimental); the five height revisions duplicate their predecessor's settings preset content, which is
intentional but worth noting for a future reviewer.

---
# Current work — 2026-10-21 / Gap 6: Alpha cloth armour (audit A-14)

Audit finding: in Alpha v1.0.1, v1.0.4 and v1.0.6 the only armour recipes are the modern **leather** ones, but leather
has no legitimate source there - cows arrive in Alpha v1.0.8, the same build that changes the armour recipe. Those
three builds therefore had no craftable armour at all, while the era's actual armour was **cloth** (wool) in the four
vanilla patterns.

Implemented as four `RemovedRecipes` entries behind a new `CLOTH_ARMOUR_RECIPE` window `[Alpha v1.0.1, Alpha v1.0.8)`,
citing minecraft.wiki, Alpha v1.0.8: "The crafting recipe of cloth/leather armor has been changed from cloth to
leather". Each recipe produces the same item tier (leather helmet/chestplate/leggings/boots) from white wool in the
standard armour shape, with `refuseModernRecipe = true` so the modern leather shape is refused in that window; from
v1.0.11 the leather recipe is the real one again. New milestone `ALPHA_1_0_8`.

This composes with the previous round: wool is now craftable in those builds from nine string (`WOOL_NINE_STRING_RECIPE`),
so the cloth-armour route is actually reachable in survival - string from spiders, wool from string, armour from wool.

**Evidence:** `./gradlew test` — **780 tests, 0 failures**. Golden, matrix and facts regenerated. `ClothArmourTest` pins
the window, the shapes and the gate.

**Counts deferred, with the evidence state recorded:** I fetched the wiki Slab, Ladder and Pressure Plate pages for the
output-count changes. The Pressure Plate page has a clean row for 1.3 ("now require only two wood planks/stone to
craft; previous versions required three"), but neither it nor the other two pages states the pre-change *output*
quantity unambiguously enough to encode, and the audit's own numbers ("slabs 6 not 3, ladders 3 not 2, plates 2 not 3")
are not backed by a row I could reproduce. Those stay recorded as open rather than guessed; the stone-slab item
substitution is likewise still open.

---
# Current work — 2026-10-21 / Gap 5: golden apple effects, Release eras

The recipes were fixed in an earlier round; the effects were still modern. Took the values from minecraft.wiki's
Golden Apple history rows directly (fetched raw, 2026-10-21), because the repo's own notes and the era audit disagreed
on the duration (4 s vs 5 s vs 10 s):

- **Release (1.0) - 1.5**: 4 hunger and **Regeneration for 5 seconds at amplifier 0**, with no Absorption. Wiki:
  "Golden apples have been changed, so that they now restore 4 hunger and give only 5 seconds of Regeneration. The
  Regeneration effect of golden apples has been slowed down." New feature `GOLDEN_APPLE_REGEN_ONE`.
- **Release 1.6.4**: **Regeneration II for 0:10 plus Absorption I for 2:00**. Wiki rows: 13w23b "Health Boost for 1:30
  and Regeneration II for 0:10", 13w24b replaced Health Boost with Absorption lasting 1:30, 13w25a "Absorption from
  golden apples now lasts 2:00"; those snapshots ship in 1.6.1. New feature `GOLDEN_APPLE_SIX_ONE_EFFECTS`.
- Beta 1.8.1's 10-hunger / 30-second form is untouched, and both new windows are inactive in ordinary worlds.

The mixin rebuilds the food component through the record's accessors, so saturation, eat time and the conversion stack
are carried across; only nutrition and effects differ. `GoldenAppleEffectsTest` pins the windows and the tick/amplifier
values. Two new features are described in `FeatureBoundaryTest` (the 1.6.1 window is written as FROM release_1_6_4
because 1.6.1 is not itself a declared version).

**Evidence:** `./gradlew test` — **777 tests, 0 failures**. Golden, matrix and facts regenerated.

**Open in this item:** the golden apple's *saturation* for Release 1.0-1.5 is the modern value (4 hunger matches, the
saturation modifier is unverified - recorded, not guessed); Health Boost's brief 13w23b-only existence is not modelled
because it shipped in no declared build.

---
# Current work — 2026-10-21 / Gap 4: arrow and wool recipes (audit A-12, A-13)

Two more recipes modern Minecraft no longer has, both with their own window and both refusing the modern shape so it
cannot remain a second way to craft the item:

- **Arrows, iron tip**: `[Alpha v1.0.1, Alpha v1.0.14)`, 1x3 iron ingot + stick + feather → 4 arrows. minecraft.wiki,
  Arrow history, Alpha v1.0.14: "The tip in crafting is now made from flint rather than iron ingots". Alpha v1.0.14
  and later keep the modern flint recipe. New milestone `ALPHA_1_0_14` because the boundary build was not previously
  a milestone.
- **Wool, nine string**: `[Alpha v1.0.1, Beta 1.7)`, 3x3 nine string → one white wool. minecraft.wiki, Wool history:
  "1.6.6 The crafting recipe of wool has been changed, reducing the amount of string required from 9 to 4 pieces";
  Beta 1.7.3 is the first declared version after that change, so it keeps the modern four-string recipe.

Both are `RemovedRecipes` entries with `refuseModernRecipe = true`, and both features are described in
`FeatureBoundaryTest`. `ArrowAndWoolRecipeTest` pins the windows, the shapes and the gate; one older allow-list test was
updated deliberately (arrows are no longer in the vanilla sweep because the supplied shape replaces the registry one).

**Evidence:** `./gradlew test` — **774 tests, 0 failures**. Golden (two new features), matrix and facts regenerated.

**Still open in this audit item:** slab/ladder/pressure-plate output counts; the stone slab substitution
(`smooth_stone_slab` 3 cobblestone → 3, refused as "Beta 1.3" while the 1.14 `stone_slab` is craftable); sheep shearing
should yield **2-4** wool for Beta 1.7+ against the modern 1-3 (needs a hook on the modern shear path, not the punch
path this project already intercepts); golden apple effects for Release 1.1-1.5 (Regeneration I, 5 s) and 1.6.4 (10 s);
`redstone_lamp` still refused in Release 1.2.5 by the item allow-list; `oak_slab` still dated 1.6.4.

---
# Current work — 2026-10-21 / Gap 3: golden apple recipes and a thorough Beta 1.8.1 drop pass

Owner asked for thoroughness after the 1.8 drop defect, on the fair grounds that one found-by-testing bug means
others may be unlooked-for. This entry records what was checked, not only what was changed.

**Golden apple recipes (audit G6/G4).** `RemovedRecipes` now carries a window per recipe instead of one for the class,
and two historical golden-apple shapes are supplied: 8 **gold blocks** + apple for `[Infdev 20100420, Release 1.1)`,
8 **gold nuggets** + apple for `[Release 1.1, Release 1.9)`. The item is allow-listed from Infdev (it had been dated
Beta 1.8, making it uncraftable in every Beta world); the modern 8-ingot recipe is refused wherever a supplied shape
replaces it, so the player cannot use anachronistic ingredients. Two new features
(`GOLDEN_APPLE_GOLD_BLOCK_RECIPE`, `GOLDEN_APPLE_GOLD_NUGGET_RECIPE`) are described in `FeatureBoundaryTest`; the facts
sheet now reports replaced recipes separately from refusals.

**Beta 1.8.1 drops, re-derived from `research/historical-features.json`** (all six mob entries are artifact-verified
against the b1.8 client, not guessed):
- zombie 0-2 rotten flesh; pigman 0-1 cooked porkchop + 0-1 rotten flesh; pig 0-2 porkchop (cooked on fire);
  chicken 0-2 feathers + raw chicken (cooked on fire); cow 0-2 leather + raw beef/steak; spider string + spider eye;
  skeleton arrows + bones; creeper gunpowder.
- The model was one item per mob, so the second items could not be expressed at all. Added `ExtraDrop` pools (a
  second loot pool with its own count and burn flag) and filled all five: chicken/feather, cow/leather,
  spider/spider_eye, skeleton/bone, pigman/rotten_flesh. Burn substitution is now version-aware (pork from Beta 1.5,
  beef and chicken from Beta 1.8).
- The 1.8.1 primary table gained pig, skeleton, creeper and pigman rows; previously they fell through to vanilla.

**Checked and found still open (not guessed):** the exact 1.8 quantities for chicken raw-chicken/feathers and cow
beef/leather are wiki-sourced or modern placeholders (the corpus says so itself); sheep shearing quantity is 2-4 and
this model still builds a 0-1 table for it (needs its own builder); ghast tears and squid ink sacs have no 1.8 rule yet.

**Evidence:** `./gradlew test` — **771 tests, 0 failures**. Golden, matrix and facts regenerated; licence headers
re-applied.

---
# Current work — 2026-10-21 / Gap implementation 2: block dates (G3) and the chicken/cow meat defect

**Owner-reported defect fixed.** "Chickens don't drop meat nor do cows in Beta 1.8.1" was real and the cause was a
cache, not the table: `HistoricalMobLootHelper` built ONE global table per entity type from Beta 1.7.3's items and
looked it up by entity type alone. So a 1.8.1 chicken resolved to the 1.7.3 feather table and a 1.8.1 cow to leather,
regardless of the version's own rule — raw chicken and beef never dropped. The helper now builds from the **version's
own rule** (`eraTable`, keyed by profile+entity), keeping the two special shapes (bones join arrows from Beta 1.2, pork
smelts on fire from Beta 1.5). `EraDropItemTest` pins Beta 1.8.1 → raw chicken / beef / rotten flesh while Beta 1.7.3
and Alpha keep feathers and leather, and Release profiles still invent no rule.

**Audit G3 fixed at its source.** The block-availability generator dated genuine Release 1.2.1/1.3.1/1.4.2/1.5 content
to Release 1.6.4 because 1.6.4 was the first *extracted palette* to register those names — the project has no extracted
1.3/1.4/1.5 pages, so palette evidence could not tell them apart. The one big 1.6.4 `TIMELINE` entry is now four
entries sourced from `research/version-transitions.json`: 1.3 (ender chest, tripwire, cocoa), 1.4 (anvil, beacon,
cobblestone wall, flower pot, command block, carrots, potatoes), 1.5 (hopper, dropper, comparator, daylight detector,
activator rail, quartz), 1.6 (hay bale, coal block, slab). Consequences: a Release 1.3 world can craft an ender chest,
1.4 an anvil or beacon, 1.5 a hopper or comparator — all of which the gate refused before. `ReleaseBlockDatesTest`
pins each date and its "not before" neighbour.

**Two generator defects exposed and fixed on the way.** Re-running the generator dropped `indev_latest` and
`alpha_1_2_2a` from the chronology (they have no extracted palette but are declared versions), which failed the
registry-agreement test; and adding Indev to the chronology revealed that pre-Classic/Classic blocks were dated to
Infdev, so the Indev profile was judged anachronistic for grass, bedrock, ores, leaves, saplings, glass, TNT, obsidian
and farmland. Those are now dated to `indev_latest` (diamond ore stays Infdev: it was restored 20100320, after Indev).

**Evidence:** `./gradlew test` — **135 suites, 769 tests, 0 failures**. Golden diff reviewed: block-boundary lines and
the drop-rule lines only. `VERSION_MATRIX.md` and `version-facts.md` regenerated. `scripts/apply-licence-headers.py`
run because the generator does not emit the SPDX header it used to carry.

**Still open from G3:** `redstone_lamp` (1.2.1) is refused in Release 1.2.5 by the item allow-list and needs its own
sourced date; `oak_slab` remains dated 1.6.4 although the slab block is Beta 1.3; the 1.6 bucket's evidence is the
palette, not the release list, for hay/coal.

---
# Current work — 2026-10-21 / Gap implementation 1: mob drop tables (audit G1), wired

Owner instruction: implement everything the four era audits found. Goal `goal-93bd997b`, backlog in
[docs/VERSION_GAPS.md](<docs/VERSION_GAPS.md>).

**Done in this slice.** The pre-Beta-1.8 drop table is now applied to every version that needs it:

- `HistoricalLootRules.PRE_BETA_1_8` — zombie→feather, skeleton→arrow, creeper→gunpowder, spider→string,
  pig→porkchop, cow→leather, chicken→feather, zombie pigman→cooked porkchop (0–1). Every row carries its
  `research/feature-timeline.json` citation; the model's one-item-per-mob limit is stated (bones Beta 1.2, spider eyes
  Beta 1.8, ghast tears Beta 1.7, chicken's early leather) rather than faked.
- Wired into the **10 Alpha profiles and Beta 1.0–1.6** (16 declarations) plus the Alpha v1.1.2_01 helper, which had
  been sharing the Indev table and so gave cows and chickens the modern drops.
- **Beta 1.7.3's jar-verified table extended** with the four mobs that previously fell through to vanilla (skeleton,
  creeper, pig, zombie pigman), cited to the timeline; the four jar rows keep their `gm.` field evidence and the test
  now distinguishes the two evidence classes instead of requiring every row to be from the client.
- **Sheep death wool corrected** to Beta 1.7 → Release 1.8 (previous slice); Alpha/Beta 1.0–1.6 sheep no longer drop
  wool on death.
- Fixed a wrong edit of my own before it landed: the first wiring script changed the Infdev helper instead of the Alpha
  helper, which the golden snapshot caught as Infdev gaining cow/chicken rules; both helpers are now correct and the
  facts sheet confirms Indev/Infdev keep the 5-row Indev table while Alpha/Beta use the 8-row one.

**Evidence:** `./gradlew test` — **133 suites, 761 tests, 0 failures**. Golden snapshot diff was 330 lines, reviewed and
entirely within `HistoricalLootRules`/`VersionRules.mobDrops`/`replacesLoot` (no other behaviour moved). `VERSION_MATRIX.md`
and `version-facts.md` regenerated. Three older tests that encoded the four-row table and the old sheep window were
updated deliberately, with comments saying why.

**Still open from this audit item:** ghast (should drop nothing before Beta 1.7 — the model cannot express "no drop" for
a mob inside a replaced table), squid ink sac before Beta 1.2, spider jockey/skeleton bones as secondary drops, and the
creeper music-disc condition. Recorded, not guessed.

---
# Current work — 2026-10-21 / Per-version fidelity audit: every declared version, every domain

Owner request: *"Run some deep research and figure out if there is ANYTHING missing per version, be as in depth as
possible."* Done as four era audits plus a generated fact sheet, merged into [docs/VERSION_GAPS.md](<docs/VERSION_GAPS.md>).

**Method.** The ACTUAL side is generated from the live APIs into [research/version-facts.md](<research/version-facts.md>)
by `VersionFactsTest` (HEX_DOCS_UPDATE=1), so nothing is compared against prose. The EXPECTED side is minecraft.wiki
per-version history (raw pages, quote + URL per row) plus the project's own research artefacts. Four parallel era
audits cover all 34 profiles: [Indev/Infdev/Alpha](<research/gaps/indev-infdev-alpha.md>) (20 cross-version rows + 13
versions), [Beta 1.0–1.7](<research/gaps/beta-1.0-1.7.md>) (135 rows), [Beta 1.8–1.9pre](<research/gaps/beta-1.8-1.9pre.md>)
(71 gaps + 91 checked-and-matched), [Release 1.0–1.6.4](<research/gaps/release.md>) (138 rows).

**The five findings that matter most** (full list and evidence in the audit):

1. **Mob drops are modern in 29 of 34 versions.** Only Indev, both Infdev builds, Beta 1.7.3 and Beta 1.8.1 declare
   drop tables; everywhere else a zombie drops rotten flesh (Beta 1.8 content) instead of feather. minecraft.wiki,
   Zombie: "1.8 (Pre-release) — Zombies now drop 0-2 rotten flesh instead of 0-2 feathers"; "20100219 — Zombies now drop
   0–2 feathers". Largest player-visible error in the project.
2. **Release 1.2.5–1.5 cannot craft their own era's blocks.** The block-availability table dates genuine
   1.2.1/1.3.1/1.4.2/1.5 content to Release 1.6.4, and `HistoricalRecipeGate` refuses it: a 1.3 world cannot craft an
   ender chest, 1.4 cannot craft an anvil or beacon, 1.5 cannot craft a hopper, comparator or quartz. **Direction
   corrected**: my first reading used `isRefused(version, item, null)`, which tests only the item allow-list and gave
   the opposite answer; the game passes the result block. `VersionFactsTest` now probes both gates.
3. **Every Release world from 1.2.5 is 128 blocks tall.** All Release presets use
   `historical_experience:beta_1_7_3_overworld` (height 128, min_y 0); 1.2 raised the limit to 256. Release 1.0/1.1 are
   correctly 128.
4. **Golden apples are uncraftable before Beta 1.8 and use the wrong recipe afterwards.** Allow-listed only from
   `beta_1_8_1` although added in Infdev 20100227; the recipe is 8 gold *ingots* (1.9) where it should be 8 gold blocks
   (to Release 1.1) then 8 gold nuggets. Effects are wrong for 1.1–1.6.4 too. Same class as the enchanted-apple fix.
5. **The Pre2/Pre3 boundary is shifted by one prerelease in both directions** (breeding/potions/discs/dragon late;
   nether bricks/gold nuggets/glistering melon/splash potions early), a structural error behind many rows.

Also: mob AI is modern beyond 13 gates; recipe ingredients/counts are modern wherever a removed recipe is not supplied;
geared mobs stop at 1.6.4 so 1.4/1.5 never have them; the dragon exists from Pre4 instead of Pre6; XP curve and enchanting
caps are modern; no hardcore gate.

**A retraction, recorded because the method matters.** I first reported "eating does nothing in Beta 1.0–1.6" from the
facts file. Wrong: the file showed the *declared* heal table, while `HistoricalFoodHelper.researchedHeal` deliberately
falls back to the pre-1.8 table. The facts sheet now prints declared and effective separately, and the audit records the
retraction rather than quietly deleting it.

**Verification.** `./gradlew test` — **132 suites, 758 tests, 0 failures**. Nothing in this entry changed gameplay; it is
research plus the facts generator. The audit separates [verified here] from era-audit findings, and two era-audit
coverage-matrix cells (anvil/beacon refused in 1.4/1.5) were checked and found wrong, which is why the status markers
exist.

**What I did not do:** none of the gaps are fixed yet. The audit's backlog ranks them; the first three are
crafting/drop data that the existing mechanisms can express.

---
# Current work — 2026-10-21 / Enchanted golden apples are craftable again in Release 1.3 to 1.8

Owner report: **"Why can't I craft egaps in 1.3.1 when they were added, crafting was removed in 1.9 no?"** The owner is
right and the project was wrong.

**The history** (minecraft.wiki, [Enchanted Golden Apple](https://minecraft.wiki/w/Enchanted_Golden_Apple), Java Edition
history): added in **1.3.1 (12w21a)** as a second golden-apple tier, *"crafted with 8 blocks of gold and one apple"*,
granting Resistance 5:00 + Fire Resistance 5:00 + Regeneration IV 0:30 and 4 hunger. **1.6.1 (13w24b)** added Absorption
I and raised Regeneration to V. **1.9 (15w44a)** made it *uncraftable* and moved it into chest loot; **1.13 (17w47a)**
renamed it and split the item ids.

**Two defects, both fixed:**

1. **No recipe existed.** Modern 1.21.1 has no gold-block recipe, and the project could only *filter* vanilla recipes or
   replay the extracted Indev/Infdev recipe tables - so there was nothing to allow. New `gameplay/RemovedRecipes` supplies
   recipes that modern Minecraft **removed**, assembled at lookup time exactly as `EarlyRecipeLookup` does (never patching
   the global, client-synchronized registry). Its first member is the 8-gold-blocks-around-an-apple enchanted golden apple,
   gated on `Feature.LEGACY_ENCHANTED_GOLDEN_APPLE` = `[Release 1.3, Release 1.9)`.
2. **The gate refused the item everywhere.** `enchanted_golden_apple` was absent from `HistoricalCraftingEra`'s allow-list,
   so `isRefused` returned true for every version. It is now allow-listed from `release_1_3`.

**And the note that caused it was wrong.** `HistoricalItemAvailability` said the enchanted golden apple *"is 1.9 content;
before that the golden apple itself was the enchanted form"*. It is not: the item is 1.3.1+, and what arrived in 1.9 is its
appearance in **chest loot**. The entry (loot only) stands; its reason now says so, and points at the crafting path.

**Effects, so the item is not merely craftable but correct.** `DataComponentHolderHistoricalFoodMixin` now gives the
second-tier apple its 1.3.1 effect set - Regeneration IV 0:30, Resistance 5:00, Fire Resistance 5:00 - and, from Release
1.6.1, Regeneration V plus Absorption I (`Feature.ENCHANTED_GOLDEN_APPLE_ABSORPTION`, 13w24b). Values are plain numbers in
`HistoricalFoodHelper` so they are unit-testable without registries, as the existing Beta golden-apple values are.

**Evidence:** `./gradlew test` - **131 suites, 757 tests, 0 failures**, including new
`EnchantedGoldenAppleCraftingTest` (window, recipe shape, gate agreement, effect values, and that 13w24b's boundary falls
between Release 1.5 and 1.6.4) and two new `FeatureBoundaryTest` windows. The golden snapshot changed by **one line**
(the corrected loot-note evidence text) and **seven additions** (the new allow-list entry); everything else in the diff
is reordering. Dedicated server boots to `Done (2.718s)` with 0 mixin failures
([log](<logs/egapple-fix-server-boot.log>)). `docs/VERSION_MATRIX.md` regenerated.

**Open item this creates: Absorption's duration is an assumption.** No source this project holds states how long the
Absorption I from 13w24b lasts; the modern two minutes is used and marked as such in `HistoricalFoodHelper`. If a 1.6.1
client artifact is ever extracted, replace it.

**Not verified at runtime:** the recipe was verified as data (shape, window, gate) and the server boots with the hook
applied, but no player crafted one in a live session - a crafting probe needs a player harness this project does not
have yet. That is stated rather than claimed.

---
# Current work — 2026-10-20 / Version-driven architecture refactor: audit, migration, verification

The codebase had grown 154 places where a version was compared by hand, 46 distinct boundaries written nine different
ways, string-keyed gates, and a 1,912-line registry that mixed declarations with the data they compose. This entry
records the refactor that replaced all of that with one version-driven system. Full design record:
[docs/audit/](<docs/audit/>) (three audits, two independent reviews, and the contract v2 they forced);
what the result is: [docs/ARCHITECTURE.md](<docs/ARCHITECTURE.md>), [docs/VERSION_SYSTEM.md](<docs/VERSION_SYSTEM.md>),
[docs/VERSION_MATRIX.md](<docs/VERSION_MATRIX.md>) (generated), [docs/ADDING_VERSIONS.md](<docs/ADDING_VERSIONS.md>),
[docs/ADDING_FEATURES.md](<docs/ADDING_FEATURES.md>).

**Verified end state:** `./gradlew cleanTest test` - **130 suites, 751 tests, 0 failures** (the count includes the new
golden snapshot, boundary, lint, matrix and mixin-reference tests). A dedicated server boots to `Done (2.689s)` on the final tree, applies all
145 mixins from the new layout with **0** `was not found`, **0** `PREPARE` failures and **0** client-class warnings, and
loads the existing Beta 1.7.3 test world at its recorded revision 10
([final boot log](<logs/refactor-final-server-boot.log>)).

## What was found (evidence in the audits)

- **Scattered version arithmetic**: 154 check sites, 46 boundaries, ~123 of them in `HistoricalGameplayTweaks`. The
  same boundary was written as an id, a literal, `chronology(...)`, `byId(proxy)`, a string gate, an era string or a
  `VersionWindow`; Beta 1.8 alone had five spellings that agreed only because no build sits between the values
  ([version-checks](<docs/audit/version-checks.md>)).
- **A reachable ordinary-world defect**, found by the audit and confirmed against the callers: `netherAvailable(null)`
  and `foodStacks(null)` answered `false`, and their mixins cancel on `false`, so an ordinary world refused nether
  portals and capped food at stack size 1. Five sibling predicates had already been fixed for this; these two were
  missed.
- **String-keyed gates** (a typo is a runtime throw or a silently-never-applies gate) and a `gateNames()` list that
  covered 21 of 33 gates, so 12 were never walked by the all-versions test.
- **The model depended on its consumers**: `HistoricalVersions` imported `gameplay`/`generation` for data, and the
  carrier records it needs lived there too.
- 22 probe/verification classes in the production tree; four client-dependent mixins in the common mixin list (three
  logged `target was not found` on every dedicated server).

## What was done

| Step | Change | Evidence |
| --- | --- | --- |
| S0 | Golden snapshot of **every** pure version-keyed answer (12,930 lines) generated from the old code and committed | `VersionBehaviourSnapshotTest` |
| S1 | The ordinary-world Nether/food fix, isolated; both food-stack paths share one rule | golden diff = exactly the 6 ordinary-input lines |
| S2 | `Exclusion`, `OreRule`, `DropRule` moved into `version`; `HistoricalVersion` no longer imports gameplay/generation | lint |
| S3 | `Era`, `Milestone`, `Milestones`, `Feature`, `FeatureWindow`, `FeatureSet`, `Features`, `ActiveVersion`; a differential test against the replaced rules; a 34-key chronology oracle; a fresh-JVM load-order test | `FeatureBoundaryTest`, `MilestonesTest`, `InitOrderTest` |
| S4 | 60 sources and the mixins ask `Features.active(...)`; the 1,193-line gate class became ~90 lines; its research table survives as `FeatureBoundaryTest` | golden file lost only the 362 lines of deleted methods |
| S5 | Client rendering, weather, left-click, creative, tooltips, label, colour maps, dragon fight and Nether endermen become windows; 27 more features declared once; literals become `Milestones` | golden unchanged |
| S6 | `HistoricalVersions` 1,912 → 195 lines, lookups only; declarations move to `legacy/LegacyCatalog`; catalogue `Era` → `Group` | golden changed by 3 name-only lines |
| S7 | Four client mixins reclassified; mixins split into `mixin/client` and `mixin/common` with json order preserved; the boot caught 8 package statements missed by the move (they sat on the licence comment's closing line) | dedicated-server boot above |
| S8 | 22 probe/verification classes move to `verification/`; a lint rule keeps them out of shipped behaviour | suite + boot |\n| S8b | Documentation: ARCHITECTURE rewritten (the old 26.3 contract preserved in `docs/audit/`), VERSION_SYSTEM, ADDING_VERSIONS, ADDING_FEATURES, generated VERSION_MATRIX, AGENTS.md contract | `VersionMatrixTest` fails on a stale matrix |

New enforcement, all in the normal test run: `ArchitectureLintTest` (no chronology comparisons or era arithmetic
outside the model; the model does not depend on its consumers), `MixinConfigConsistencyTest` (common mixins are
server-safe, json and files agree, package matches directory, apply order preserved), `VersionMatrixTest`,
`FeatureBoundaryTest`, `InitOrderTest`.

## What this refactor deliberately did NOT change

Every proxy boundary, every table disagreement and every inferred rule is preserved as the code had it, and the
disagreement is now written down in the feature's evidence string instead of being hidden in a class. Notably:

- breeding/tempting/baby animals still share the Prerelease 3 boundary although the evidence dates the first two to
  Prerelease 2 (`NO_ANIMAL_BREEDING`);
- mob pickup/gear/enchants still use Release 1.6.4 as the proxy for 1.4.2, so Release 1.4 and 1.5 still lack them;
- the four "introduced in" tables (blocks, items, creative catalogue, crafting) still disagree on ids such as
  `nether_wart`, `end_stone`, `ender_pearl`, `sandstone_stairs`, `carrots` and `short_grass`. That is research to
  settle, not refactoring to hide.

## Final consolidation\n\n- **Dead code removed** (37 unreferenced members the debt audit listed: unused client setters, diagnostic recorders,\n  redundant accessors) across 24 files. The golden snapshot is unchanged, so nothing live depended on them.\n- **Two hot-path orderings fixed**: the `MAX_DAMAGE` handler and the per-block generation veto now ask the cheap question\n  before building an item or block id, so ordinary worlds stop paying a registry lookup and a string per component read\n  and per block write.\n- **Probe/verification classes moved** out of `server/` and `client/` into `verification/` (22 classes), with a lint rule\n  that only the two entrypoints and the probe mixin may reference them.\n\n## Open items (recorded, not fixed)

1. **Four dating tables disagree** (above). Choose the sourced date per id, or state which table is authoritative and
   why. `HistoricalBlockAvailability`, `HistoricalItemAvailability`, `HistoricalCreativeCatalog`, `HistoricalCraftingEra`.
2. **`enchantingTableExists` was deleted as dead code** (no caller outside its tests). If a capability answer is ever
   wanted, it is `!Features.active(Feature.NO_ENCHANTING, id)` **and** a decision about what an unknown version means.
3. **No session before the first player joins.** The session is set on JOIN, so session-gated rules do not apply to
   spawns during chunk generation; worldgen rules correctly resolve from the generator instead. Recorded, not changed.
4. ~~Probe/verification classes still live in `server/` and `client/`~~ - **done**: 22 classes moved to
   `verification/` (client ones under `verification/client`), with a lint rule that only the two entrypoints and the probe
   mixin may reference them. **Still open**: the probe mixin (`ServerLevelHistoricalMobDropProbeMixin`) remains on the
   production `ServerLevel.addEntity` path; the review's recommendation was to gate it behind the same property that arms
   the probe, which needs a mixin config plugin and was not done.
5. **Licence header drift is pre-existing and unfixed**: `python scripts/apply-licence-headers.py --check` reports 72
   files whose header is the short one-line form rather than the full boilerplate (e.g. `server/EarlyMobDropCapture.java`,
   untouched since `b69f03f`). `LicenceComplianceTest` passes. Fixing it changes licence claims across 72 files, so it
   needs a deliberate pass, not a refactor side effect.
6. **`docs/ADDING-A-VERSION.md`** (73 KB, long-form checklist) predates this architecture; it is still the research
   reference, and `docs/ADDING_VERSIONS.md` now gives the code-level flow. The two should be merged eventually.
7. **Runtime coverage**: the boot proves mixin application and world loading. It does not prove rendering, and no probe
   suite was run in this session; client-side visuals remain unit-verified only.

---
# Current work — 2026-10-19 / Ore heights, part 2: the fix now covers EVERY revision, not just new worlds

The first pass fixed only each version's newest revision, so a world recorded on an older alias still generated modern ore heights (an audit found **64 of 138 revisions** unprotected: `beta_1_7_3` previews 1-4, every Beta 1.9 prerelease, every Release revision 0 and 1, `indev_latest` 0 and 3). Two code-level defaults close that without editing any stored revision data:

- **`BetaOrePlacement.populationForAlias`**: a revision's declared population still wins; an EMPTY one now falls back to the version's era population instead of placing nothing.
- **`HistoricalFeatureExclusions.shouldSkip`**: every historical world now skips the 17 modern `minecraft:ore_*` features (plus Moderner Beta's emerald before Release 1.3.1), in addition to the per-revision exclusions.

Measured on OLD revisions that were previously unprotected: `beta_1_7_3_content_preview_4` diamond y=1..15, `release_1_2_5_no_modern_content` diamond y=1..15. Release/Alpha/Indev/Beta current revisions unchanged and correct; ordinary world still runs vanilla features (diamond + deepslate diamond present, lapis to y=63). `TemporaryOreAuditTest` became the permanent `OrePopulationInvariantTest` (every revision's effective population is non-empty and contains diamond; emerald follows its boundary; removing the fallback makes it fail). Build green, **728 tests, 0 failures**.

**What this does NOT do:** chunks already generated keep the ore they were generated with. The project's rule is that upgrades affect future chunks only, so nothing is rewritten retroactively.
# Current work — 2026-10-19 / Ore heights: diamonds (and every other ore) were generating at modern heights

Owner report: "Diamonds shouldn't spawn above y level 15". **Confirmed, measured, and fixed.**

**Measured before** (`scripts/ore-height-census.py`, new, reads the saved region files): a FRESH `release_1_2_5` world, 1349 chunks — diamond **y=1..72, 1180 of 1795 above y=15**; redstone y=1..30; lapis y=1..63; gold y=1..30; iron y=1..71; coal y=1..80. A fresh `beta_1_7_3` world was already correct (diamond 1..15), which located the fault precisely: versions that place no ore population of their own were left running 1.21.1's vanilla ore features, whose `ore_diamond` is `trapezoid above_bottom -80..80` — up to y=80 in a 128-high historical world.

**Historical truth** (`research/ore-height-ranges.json`, new): every declared version predates 1.18, where diamond was placed at y 0..15 only — and the vein cannot exceed 15 either (centre ≤ origin+2, half-radius < 1). Two independent sources agree: the hash-verified Beta 1.7.3 client (`research/beta173-ore-parameters-parent-reviewed.json`: diamond size 7, 1 attempt, `nextInt(16)`) and Moderner Beta's own `reduced_height` override (size 8, `height_range uniform above_bottom 0..15`, count 1).

**Fix:** every version that placed no own population now ends at a new append-only `<version>_era_ores` revision carrying `BetaOreRules.eraPopulation(hasRedstone, hasLapis)` (the verified 1.7.3 rules *filtered* to the ores the era had — no new sizes or counts) and `HistoricalFeatureExclusions.eraOreReplacement(...)` (all 17 modern `minecraft:ore_*` plus Moderner Beta's emerald where it did not exist). 32 settings + 32 world presets added; each version's public preset retargeted, so **new** worlds are correct.

**Measured after:** release_1_2_5 diamond y=1..15 (0 above 15), redstone 1..15, gold 1..30, lapis 1..30, iron 1..63; alpha_1_2_0 diamond 0..15 with no lapis; indev diamond 2..15 with no redstone/lapis/emerald; beta_1_7_3 unchanged. Control with vanilla features intact: diamond 0..70, 1155 above y=15 — ordinary worlds unaffected. Build green, **724 tests, 0 failures**.

**Existing worlds are not retroactively changed** (append-only rule): a world records its revision, so an already-created world keeps its chunks. New worlds are correct; an upgrade onto `<version>_era_ores` fixes future chunks only.
# Current work — 2026-10-19 / Survival Test removed, codebase cleaned

- **Removed Classic Survival Test** (commit `Remove Classic Survival Test`): the `survival_test_030` profile, presets, gameplay rules, mixins, HUD/screens, probes, verification harnesses, docs and research data. The complete pre-removal tree is preserved on branch **`archive/survival-test`** (one snapshot commit, because that work had never been committed). Kept because they are not Survival Test specific: the region chunk census/contents/diff scripts, the opt-in `ChunkBlockContentCompareTest`, the Indev probe's `PROVIDER_HEIGHT` assertion, and two relative-link fixes in `docs/FLASHBACK_INTEGRATION.md` and `docs/LICENSING.md`.
- **Refactor** (next commit): deleted 67 one-shot patch/port/fix scripts that nothing references (their results are already in the source), one orphaned extractor, three unreferenced classes (`ReplayProbe`, `ModernerBetaGeneratorIdentity`, `ModernerBetaSettingsAccess`), 27 unused imports, and normalised three CRLF source files to LF.
- Verified: build green, **719 tests, 0 failures, 2 skipped**. Not done: the ~570 inline fully-qualified `net.historicalexperience.*` references (mostly in `GenerationVerification`) are left as they are; converting them to imports is mechanical but wide.
**First measurement, and it corrects an assumption.** The run reached the first world and printed
`[hex-indev] APRON hell floating deep small chunk 5,0 column 88,8 minY=0 maxY=128 = 0-127 minecraft:air`: for a
**Deep Small (64x64)** Hell/Floating world, a column well outside the footprint is **pure air**, and the level's own
envelope is still `maxY=128` - so the researched `height` does not raise the world height, and the shell outside a
custom footprint is *not* the default bedrock/dirt/water apron. Caveat before drawing conclusions: that chunk was
loaded on demand, whereas the release's apron is what the provider *pre-fills* during initial generation, so the next
measurement must read the **saved region chunks** (protochunks), not a freshly generated live chunk. The run died
~40s in (no crash report, no JVM log - killed, not an exception), so Inland's own signature is still unmeasured.

# Current work — 2026-10-17 / two lighting switches with no row of their own were still on

Owner: "I think the smooth shading being applied is the old smooth shading model even when it's off... i turned both options off in infdev". Right again, and the cause was a second pair of switches the config screen never shows: [HistoricalLightingTweaks](<src/main/java/net/historicalexperience/client/lighting/HistoricalLightingTweaks.java>) holds `oldLeavesLighting` and `oldWaterLighting`, both **defaulting to on**, both stored under their own keys, and both consulting **only the version window** (`beforeNewEngine`, i.e. everything before Beta 1.8). So in an Infdev world with both visible rows off, leaves were still lit by the old model (unshaded, `1.0F` shade) and old water lighting still applied - the mod's lighting still on after the player turned the mod's lighting off. Both now also require the visible old-lighting switch, so "off" means off including the parts with no row of their own. **718 tests, 0 failures, 1 skipped.**

**Third sighting of the same defect class.** The terrace bug was the advance forgetting stored fields; the forced flat lighting was `useAmbientOcclusion` consulting the version window and never its switch; this is two more features doing exactly that. The config screen exposes **four** lighting-adjacent switches' worth of behaviour while showing two rows, which is why the owner could not switch the rest off. **Next step, and it needs doing before more reports arrive:** sweep every version-window gate in the client lighting/visual path for a missing switch check, and either gate it on a visible switch or give it a row.

# Current work — 2026-10-17 / Inland generates land, not ocean

Owner: "Ideally avoid tonnes of water when using the Inland varient, water is fine when using regular bcus its an island". Cause: an Inland map's **land** border becomes the destination's infinite **ocean**, because the infinite terrain is generated at the destination's sea level. `moderner_beta:sea_level` is a stored Moderner Beta setting (confirmed in this project's own evidence files, not guessed), so [WorldGenSettingsAdvance.advance](<src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>) now writes `0` into the destination for an **Inland** source only - at 0 the destination generates no ocean, so the land continues. Island and Floating sources are deliberately untouched: for those, water is the point. If the source geometry cannot be read the advance carries on without the adjustment rather than refusing to move the world. Test: [anInlandSourceGetsALandSeaLevelAndOtherSourcesAreLeftAlone](<src/test/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentityTest.java>). **718 tests, 0 failures, 1 skipped.**

**Scope note:** this applies at the finite-to-infinite advance, which is where the ocean appears. The finite Indev map itself is left alone - Inland already generates land at its borders, and lowering the sea level there would also drain the lakes inside the island, which was not asked for.

# Current work — 2026-10-17 / upgrades are now a HARD CUT, and already-broken worlds get repaired on open

Owner: "you are allowed to just do what everything else does and just hard cut, these elongated chunks are horrid". Two changes, both landing on that.

**1. The collar is gone from the upgrade.** [FiniteApronRelease](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) no longer samples the island edge on the blanket (upgrade) path, so its edge table stays empty, `blend` returns upstream and no transition geometry is invented - release outside, let the infinite generator write fresh chunks, exactly like the converters the owner compared it to. The repair path still samples, because its job is the opposite (put back a rim without touching what is there), and [FiniteEdgeRepairTest](<src/test/java/net/historicalexperience/world/upgrade/FiniteEdgeRepairTest.java>) still pins that. A test that pinned the upgrade's 16 sampled columns now pins **0** and explains why.

**2. A world already broken by an older build is repaired when it is opened.** The strip added last round only helped worlds advanced from then on; the owner's world still stores the finite map's components, including the `moderner_beta:finite_2d` noise reference that generates the ridges. New [repairRetainedFiniteComponents](<src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>) reads the stored generator, and - only when it is NOT a finite preset - removes the finite-only keys and writes the file back; a clean world is left byte-identical and no write happens. It runs from [WorldOpenFlowsHistoricalBiomeMixin](<src/main/java/net/historicalexperience/mixin/WorldOpenFlowsHistoricalBiomeMixin.java>) **before** the level reads its generator and **before** the owed-conversion early return, so it also applies to worlds that owe no conversion, wrapped so a failure can never stop a world opening. **717 tests, 0 failures, 1 skipped.**

**What still will not be fixed in an existing world:** chunks already written keep what they were generated with. New chunks after this repair generate normally, so a damaged world shows a seam and then behaves - the owner should expect that, and a fresh world is still the clean comparison.

# Current work — 2026-10-17 / smooth lighting was forced off with no toggle check at all

Owner: "It still forces smooth shading off for versions that didn't come with smooth shading when i have old lighting turned off". Correct, and the cause was [MinecraftHistoricalSmoothLightingMixin](<src/main/java/net/historicalexperience/mixin/MinecraftHistoricalSmoothLightingMixin.java>): `useAmbientOcclusion` answered **false for every version before Beta 1.3 unconditionally** - it consulted the version and the player's option, never the mod's own switch. So the mod kept imposing the era's flat shading after the player switched its lighting off. It now answers false only while the historical lighting engine is on (`HistoricalOldLighting.toggle()`); with it off the player's own ambient-occlusion setting stands and the world is lit exactly as an unmodded one would be. **717 tests, 0 failures, 1 skipped.**

**Judgement call recorded:** the pre-Beta-1.3 flat answer is governed by the OLD LIGHTING switch rather than the smooth-shading one. The era's lighting is what old lighting emulates, and the smooth-shading row's own tooltip says it applies to Beta 1.3 to 1.7.3, so it does not speak for a version that predates the feature. The two differ only when exactly one switch is on; both off is vanilla either way, which is what the owner asked for. This is also the first sighting of the general defect behind their last two reports - a feature consulting the version window and forgetting its own switch.

# Current work — 2026-10-17 / config rows shortened; both lighting toggles off already gate to vanilla (code-verified)

Owner: "these names should be much shorter and when turning both of them off the world should use defacto lighting as in as if there was no mod installed at all just regular lighting".

**Short labels: done.** A row used to read `Old smooth shading (NT): OFF - not used by this version, kept for the versions that had it (click to turn on)` - wider than its own button, because the scope sentence was interpolated into the label. Rows are now name plus state: `Old smooth shading: ON`, `Old lighting: OFF`, `Version name in corner: ON`, `Developer mode: OFF`. The sentence and the click hint moved to the row's **tooltip** (`rowTooltip` in [HistoricalConfigScreen](<src/main/java/net/historicalexperience/client/config/HistoricalConfigScreen.java>)), and the screen's foot note still covers both lighting options, so nothing is lost. [OptionScopeTest](<src/test/java/net/historicalexperience/client/config/OptionScopeTest.java>) now pins the labels as short (no `%s`, under 40 characters) and the tooltip keys as present. **717 tests, 0 failures, 1 skipped.**

**Lighting with both toggles off: I could not find a non-vanilla path, and I did not change behaviour on a guess.** Every hook branches on the toggles: [LightTextureHistoricalMixin](<src/main/java/net/historicalexperience/mixin/LightTextureHistoricalMixin.java>) (all five injections), [LightEngineHistoricalOldLightingMixin](<src/main/java/net/historicalexperience/mixin/LightEngineHistoricalOldLightingMixin.java>), [LayerLightSectionStorageHistoricalMixin](<src/main/java/net/historicalexperience/mixin/LayerLightSectionStorageHistoricalMixin.java>), [BlockAndTintGetterHistoricalMixin](<src/main/java/net/historicalexperience/mixin/BlockAndTintGetterHistoricalMixin.java>), [BlockBehaviourHistoricalShadeBrightnessMixin](<src/main/java/net/historicalexperience/mixin/BlockBehaviourHistoricalShadeBrightnessMixin.java>) and [HistoricalDataLayer](<src/main/java/net/historicalexperience/client/lighting/HistoricalDataLayer.java>). `setToggle` persists, recomputes, and queues every visible section for rebuild on a change; `activeNow()` is `availableFor(version) && toggle()`. So the remaining suspects for what the owner sees are outside those two switches - the **historical texture pack** (its own lightmap/textures), stale meshes before the rebuild drains, or a row whose state they read as off while the world's version window keeps it on. Needs one specific observation to act on rather than another guess.

# Current work — 2026-10-17 / the terrace bug: finite-only components survived the advance

Owner screenshot: an Infdev world generated in flat, evenly spaced terraces, "wtf is going on here???". Cause found in code, not guessed: [WorldGenSettingsAdvance.advance](<src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>) rewrote the three `preset` references and **left every inline component behind**. A customized finite world therefore became an infinite generator still carrying the finite map's own settings - including `noise_generator_settings` still naming `moderner_beta:finite_2d`, the finite map's 2D noise field, which is not a terrain generator for an infinite world and produces exactly those terraces.

**Fix.** The advance now strips finite-only components from all three provider-settings objects: the `moderner_beta:finite_*` family, `moderner_beta:world_border`, `moderner_beta:biome_injection_rules`, and `noise_generator_settings` when it names the finite 2D field. A merely customized component (deepslate, for instance) is left alone - the rule removes what cannot mean anything any more, not what the player chose. Test: [advancingToInfiniteDropsTheFiniteMapsInlineSettings](<src/test/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentityTest.java>). **717 tests, 0 failures, 1 skipped.**

**Not fixed by this, stated plainly:** a world ALREADY advanced keeps its baked-in components, so the owner's current terraced world is not repaired by this change. The same strip applied at load time to any non-finite world that still carries finite-only components would be the self-healing repair - small, but not written yet, and it must not claim to regenerate chunks that were already written.

**This is the third owner-visible defect traced to the same root**: the finite-to-infinite advance was written for the project's own unchanged default preset, and every world whose settings differ - customized map settings, a shorter height, retained components - found a different way to break. Earlier records preserved below.

# Current work — 2026-10-17 / Indev height corrected: the provider's 128, not the table's 64

Owner screenshot: an **Infdev** world with a wall of water hanging off the island, "indev world height is clearly wrong and this is absurd". Cause found and it was mine: the Indev map chooser wrote the **published table's height (64)** for a Normal-square map, while the provider this project actually uses ships **128** - measured earlier (an untouched Indev world stores `finite_level_properties.height = 128` and loads with `maxY = 128`). So a world made through the chooser generated at half the provider's terrain, and after the upgrade its short terrain met the new infinite terrain as a cliff.

**Fix.** [IndevMapSettings](<src/main/java/net/historicalexperience/worldgen/IndevMapSettings.java>) now takes the **height from the provider** (`PROVIDER_HEIGHT = 128`) for every size and shape, and keeps the table's **width and length**, which that same preset corroborates (Normal square is 256x256, exactly the table). The table's Deep "four times the height" is no longer claimed: 256 is twice this provider's envelope. The test that pinned the old table is rewritten to pin the distinction - footprint from the table, height from the provider - **716 tests, 0 failures, 1 skipped**.

**What this does not fix, stated plainly.** It fixes worlds created from now on. A world already created at height 64 keeps its saved 64 - changing a stored setting to 128 would leave its existing terrain misaligned with its own generator, which is worse than the cliff. The owner's Inland world therefore needs recreating (or a fresh world) to test this properly.

**Still open from the previous round:** the blanket cut-everything rule is wired for the upgrade path only (repair keeps its audited rule), and the real in-game Inland upgrade run has not been executed. Earlier records preserved below.

# Current work — 2026-10-17 / apron migration: the owner's cut-everything rule is wired, but ONLY for the upgrade

Goal `goal-7b14fdcd-51e0-4431-bf14-1763059a4b8f` round6/6. Owner accepted the trade-off and said "do it", so the blanket rule is in - scoped so it cannot destroy the repair path's guarantees.

**Change.** [Plan](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) carries `blanketExterior`. `release(world)` - the explicit upgrade, the call with no proven source - sets it: an exterior chunk is releasable when it holds **no entity and no block entity**, and **shape is not consulted**, so an Inland map's generated flat land is cut. `release(world, provenSource, evidence)` - the repair, which proves its source from a backup - keeps the narrow audited rule (known shell or entirely empty, plus saved evidence). `apply` re-proves each slot under the rule **that plan** was prepared with, so the two paths cannot drift; a plan written before the field existed reads as the narrow rule, which is what it was.

**Test.** [theUpgradePathCutsOffPlainExteriorTerrainThatTheNarrowRuleWouldKeep](<src/test/java/net/historicalexperience/world/upgrade/FiniteApronReleaseTest.java>) builds a plain-land chunk (grass/dirt/stone - neither shell nor empty), asserts the upgrade records `blanketExterior`, cuts it, and leaves the island chunk. The five `FiniteEdgeRepairTest` guarantees that broke when the rule was global are green again. **716 tests, 0 failures, 1 skipped.**

**Also fixed this round:** the refusal in the owner's screenshot. The geometry reader demanded settings of *exactly* `{preset}` or `{preset + map settings}`; it now ignores any other inline component (that is not this migration's business) and reads only the map settings, while malformed map settings still refuse with the reason.

**Last item, not done:** the real in-game upgrade run on an Inland world. It needs the probe wired through the upgrade service plus the conversion driver, in the right order - the driver releases only while the stored settings still name a finite preset, so the sequence has to be read rather than guessed. Everything else is implemented and covered. Earlier records preserved below.

# Current work — 2026-10-17 / apron migration: scope narrowed by the owner to Inland + default

Owner's instruction, verbatim in effect: **only Inland and the default matter**; an Inland world's outside is "infinite grass", so the job is to cut off the chunks outside the world, optionally leave about a block of buffer at the boundary, and let new chunks generate. They also noted that an online Indev world converter already does this, so it should not be hard. That narrows this goal: **no Long/Deep/Huge footprints and no other themes are required.**

**Why it is right that this is not hard for Inland.** Inland's map is the default 256x256 footprint, so `captureEdge`/`blend` sample the correct columns and the existing collar applies; and the recorded-geometry read (already landed) accepts an Inland world instead of refusing it. The only thing missing is that the release does not RECOGNISE Inland's exterior: it accepts the bedrock/dirt/water shell and provably-empty chunks, and Inland's outside is generated flat grass, which matches neither and is therefore kept.

**The remaining change, exactly:** accept a third provably-untouched exterior shape - generated flat land. Proposed rule, to be confirmed against one measurement: no entities, no block entities, no POI; every column the same height (perfectly flat, which a generated plain is and an edited one is not); and only the generated materials (grass block, dirt, stone, bedrock, water level 0, sand, gravel). Then Inland's plain is released and the boundary behaves like the default's water shell. The optional "one block of buffer" belongs to the collar, which already exists for the default edge.

**The one missing fact:** the exact blocks of an Inland world's saved exterior (never measured - no Inland world has ever existed). One measurement run creates an Inland world and reads its saved regions with the existing opt-in tool ([SavedApronShapeTest](<src/test/java/net/historicalexperience/world/upgrade/SavedApronShapeTest.java>), `-PapronFixture=run/client/saves`). Then implement the flat-land acceptance with a unit fixture, and run the real upgrade once.

**Everything else already landed and green (715 tests, 0 failures, 1 skipped):** geometry read from the world's own settings with specific refusals, provably-empty release in both prepare and apply, footprint from the recorded width/length, plans without the geometry field defaulting to the shipped one, and the ring-chunk integration proof. Earlier records preserved below.

# Current work — 2026-10-17 / apron migration: recorded geometry drives the release (integration test green)

Goal `goal-7b14fdcd-51e0-4431-bf14-1763059a4b8f` round5/6. [FiniteApronReleaseTest.aRecordedCustomFootprintDecidesWhatIsIslandAndWhatIsExterior](<src/test/java/net/historicalexperience/world/upgrade/FiniteApronReleaseTest.java>) now runs the whole release path against a fixture world recording `inland/normal 64x64x64`, with three chunks placed to mean three different things: slot 1 is island under the recorded footprint, slot 2 is the **ring** (outside the recorded footprint but inside the shipped default's [-8,8)) and empty, slot 9 is beyond both and shell-shaped. Result: **both exterior chunks are released including the ring one**, the plan records the custom geometry, and the island chunk is recorded as terrain to preserve. That ring chunk is exactly what the hardcoded footprint left behind forever, so this is the end-to-end proof of the previous round's change. **715 tests, 0 failures, 1 skipped.**

Two of my own assertions were wrong first and are worth recording: I first placed the "shell" chunk at the origin (which is island), then expected a kept *island* chunk to appear in `preservedExteriorChunks` (it is protected terrain instead). Both were test mistakes, not product ones.

**What is left for the goal, and nothing else:** the real in-game upgrade run (advance a custom-settings Indev world to a non-finite version and let the owed conversion + apron release run), and creating + measuring an Inland world's own saved exterior, which still has never existed. Everything else - geometry read and refusals, provably-empty release in both phases, footprint from the recorded width/length, old plans defaulting to the shipped geometry, the coast-collar generalization being an explicit known gap - is implemented and covered. Earlier records preserved below.

# Current work — 2026-10-17 / apron migration: the world's own geometry is read, so custom maps are no longer refused

Goal `goal-7b14fdcd-51e0-4431-bf14-1763059a4b8f` round4/6. **The refusal that blocked the owner is gone**, replaced by a verified read of the world's own map settings.

**Change.** New [FiniteGeometry](<src/main/java/net/historicalexperience/world/upgrade/FiniteGeometry.java>) - the four stored values plus the type and theme, with the footprint derived from `width`/`length` (the map is centred on the origin, so the default 256 is exactly chunks [-8,8), the range that used to be hardcoded) and a `describe()` for messages. [WorldGenSettingsAdvance.readFiniteGeometry](<src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>) replaces `requireDefaultFiniteSource`: each of the three provider-settings objects may now be the preset reference alone (meaning the shipped default) or that reference plus exactly `moderner_beta:finite_level_properties`; malformed fields, range violations, a different customized component, or values that disagree between the generator and the two biome-source copies are each refused with the specific reason. [FiniteTransitionIdentity](<src/main/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentity.java>) records the geometry so recovery and re-verification re-prove the same footprint - and a plan written before the field existed reads as the shipped default, which is what the old rule only ever accepted. [FiniteApronRelease](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) now uses that geometry for "is this chunk the island" in BOTH the prepare loop and the apply re-verification.

**Tests (714 total, 0 failures, 1 skipped measurement).** A preset-only world reports the shipped default; a world recording `inland/normal 256x256x64` is **accepted** and reports that geometry as customized (this test fails on the old rule); settings that disagree between the three copies are refused naming "disagree"; and malformed settings are refused naming the missing field while keeping the stable first clause the upgrade screen shows.

**Known limits after this round.** `captureEdge`/`blend` still sample the default ±128 edge columns and use a fixed 64-block collar, so a custom footprint gets **no** edge heights and therefore no coast smoothing (`blend` returns upstream for unknown edges rather than inventing terrain); generalizing that is the remaining fidelity work. The block reader is still a 128-tall array matching the measured Indev envelope (a taller envelope is refused/kept, never guessed). And the real upgrade has not been run yet on a custom-settings world - **next round**: run it (and create + measure an Inland world, which still has never existed), then decide whether the collar needs generalizing for the owner's case.

# Current work — 2026-10-17 / apron migration: "provably empty" exterior is now releasable

Goal `goal-7b14fdcd-51e0-4431-bf14-1763059a4b8f` round3/6. **Implemented the half that delivers "cuts off everything"**, at both places that decide it, with a regression test that fails on the old rule.

**Change.** [FiniteRegionSnapshot.provablyEmpty](<src/main/java/net/historicalexperience/world/upgrade/FiniteRegionSnapshot.java>) proves a chunk holds nothing (every block air, no block entities, no entities). [FiniteApronRelease](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) now releases an exterior chunk when it is **either** the known synthetic shell **or** provably empty - in the prepare loop *and* in the re-verification used for prepared/old plans, because BOTH had their own copy of the shell-only rule (the second one surfaced as `Prepared finite slot lacks pristine default source evidence` on the first test run, which is why the test is worth having). Terrain, containers, entities and POI records still refuse; the log line and the repair message now say "provably nothing" instead of "pristine default".

**Test.** [FiniteApronReleaseTest.provablyEmptyExteriorIsReleasedWhileAnythingHoldingContentIsNot](<src/test/java/net/historicalexperience/world/upgrade/FiniteApronReleaseTest.java>) builds the same fixture as the shell test but makes the releasable exterior chunk *empty* rather than shell-shaped: only it is released, while an empty chunk holding a chest and a shell chunk carrying an entity record both stay. It fails against the previous rule (which released only bedrock/dirt/water). Suite: **711 tests, 0 failures, 1 skipped** (the measurement test).

**Consequence the owner should know, and it is a deliberate one.** This changes what a *default* upgrade releases too: measured, ~668 of ~705 exterior chunks in a default world are empty rather than shell-shaped, so those are now cut off where previously they were kept as void. That is what "cut off everything" means, and deciding it differently is a one-line gate if the old default set is preferred.

**Still to do for the goal:** accept a world whose stored geometry is customized (today `requireDefaultFiniteSource` still refuses any inline override, so an Inland or Floating world is refused before this rule is even reached - the user's case), derive the footprint from the recorded `width`/`length` and the coastline/edge table from it, record that geometry in the plan, and size the block reader by the recorded height. Inland's own exterior shape is still unmeasured. Earlier records preserved below.

# Current work — 2026-10-17 / apron migration: measured from saved worlds, and the design changed because of it

Goal `goal-7b14fdcd-51e0-4431-bf14-1763059a4b8f` round2/6. Measurement first, and it moved the target: the release's problem is not only that it refuses customized worlds, it is that **it does not "cut off everything" even for a default one**. Full numbers in [the research record](<research/finite-apron-shapes.json>).

**Measured from SAVED region chunks** (the shell is what the provider pre-fills, so a live chunk was never evidence) with an opt-in developer test, [SavedApronShapeTest](<src/test/java/net/historicalexperience/world/upgrade/SavedApronShapeTest.java>), armed by `-PapronFixture=<saves dir>` and skipped in the normal suite:

| World | exterior chunks | matching the bedrock/dirt/water shell | entirely air |
| --- | --- | --- | --- |
| default (preset-only, 256x256x128) | 705 | **37** | 668 |
| default option names confirmed | 696 | 33 | 663 |
| Floating / Hell / Deep Small (64x64, override) | 751 | **0** | **751** |

The sample column **inside** a default world's edge is real terrain (bedrock, lava, ores, the water table at 55-63); every sampled exterior column was air. Three findings follow, and the second is new:

1. Moderner Beta pre-fills exactly the **default 256x256 area** (256 chunks inside [-8,8)) whatever the map settings say; a 64x64 map puts its terrain in the middle and leaves the rest empty.
2. A custom map has **no shell at all** - 100% empty exterior. So the release, which recognises only the shell, would release nothing even if it were allowed to run.
3. Even the default world keeps ~95% of its exterior chunks because they are empty rather than shell-shaped. A kept chunk is one future worldgen will not write to, which is precisely the "it does not cut off everything" the owner reports.

**Design decided:** accept a **provably empty** exterior chunk (all air, no entities/block-entities/POI) as releasable alongside the shell; take the footprint, height and coastline from the world's own recorded `finite_level_properties` and record that geometry in the transition plan; keep terrain/container/entity chunks unreleasable whatever the geometry; verify the default path releases exactly what it releases today. The 128-tall block reader also has to be sized by the recorded height.

**Not measured yet:** Inland's own exterior shape (no Inland world has ever been created - the run that would have made one died), Long/Deep/Huge footprints, and Hell's boundary at default geometry. **Next round:** implement the two accepted exterior shapes plus recorded geometry, then measure Inland and re-run a real upgrade for a custom-settings world. 709 tests green, plus this one skipped measurement; nothing about shipped behaviour changed yet. Earlier records preserved below.

# Current work — 2026-10-17 / generalizing the apron migration: cause confirmed, geometry still to be measured

Owner request: "I should be able to upgrade a world that has the inland setting and just cuts off everything like it would if it were default." New goal `goal-7b14fdcd-51e0-4431-bf14-1763059a4b8f` (max6) carries it. Round1/6 is a reconnaissance round and is **not finished**.

**Cause confirmed by reading the code, not inferred.** Three things are hardcoded to the default Indev map:
[FiniteRegionSnapshot.pristineApron](<src/main/java/net/historicalexperience/world/upgrade/FiniteRegionSnapshot.java>) recognises the synthetic shell as `bedrock 0-53, dirt 54, water(level=0) 55-63, air 64-127`; [FiniteApronRelease.inside](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) fixes the footprint at chunk x/z in [-8,8) (the ±128 blocks of the default 256 map) and `captureEdge` samples exactly the columns x/z = ±127/-128 into a 1024-entry table; `blocks()` is a 128-tall array and throws on non-air above y=127. `requireDefaultFiniteSource` refuses any inline override outright - which is why an Inland world (or any world where the options screen was confirmed) is refused before anything is written.

**So the generalization needs, in order:** (1) the apron *signature* per level type and theme (Inland's boundary is flat land rather than water, and Hell's is lava - the wiki describes the terrain, not the shell, so this must be measured); (2) the footprint in chunks from `width`/`length` instead of ±8; (3) the edge-column table and its indexing for other footprints, plus the coast collar's constant 64-block collar; (4) the plan schema, which stores a fixed 1024-entry edge table and would need the geometry recorded with it; (5) `blocks()` sized by `height`, since a Deep map is 256 tall and today's array cannot even represent it. The default path must stay byte-identical, so the geometry belongs *in* the signature/plan rather than replacing it.

**Where it stopped.** [IndevMapProbe](<src/main/java/net/historicalexperience/client/indev/IndevMapProbe.java>) gained an `apronSignature(...)` measurement - it prints the real block column of an out-of-bounds chunk, as contiguous y-runs, from the running server - plus the `inland` mode and an Inland world in the run, so the shell can be *measured* instead of reimplemented from the generator. It builds (offline build exit0), but the measurement run **failed with exit 1 and produced no receipt and no new client log** (the newest `logs/latest.log` on disk is still the previous, successful run at 06:35). Cause not yet determined; nothing about the shipped behaviour changed this round, and no workaround was attempted blind.

**Next round:** diagnose that run (start by capturing the run's raw tail rather than a filtered pattern), obtain the apron signatures for default / Inland / Hell / Deep-Small, then implement (1)-(5) with the existing exact-pristine-only release rules intact and unit fixtures in the style of `FiniteApronReleaseTest`. The owner's existing world still cannot be migrated until then. Earlier records preserved below.

# Current work — 2026-10-17 / Indev settings vs the finite-to-infinite migration: cause found, trap closed

Owner report, with screenshot: **"Converting world biomes / Conversion failed: java.io.IOException: Cannot establish safe default finite source identity"**. Cause found in one pass: the Indev map chooser writes an inline `finite_level_properties` override, and the finite-to-infinite migration deliberately refuses **customized** finite settings - its apron release is verified only against the default map (`FiniteApronRelease` hardcodes the 256x256 footprint, the ±128 edges and a surface at y=127). So the refusal was protective and correct; what was wrong was that (a) merely opening the options screen and confirming the **defaults** also wrote an override, silently costing that world the Infdev upgrade, and (b) the screen showed only the wrapper text, so nothing said *why*.

**Fixed.** [IndevMapApplication](<src/main/java/net/historicalexperience/client/indev/IndevMapApplication.java>) now writes **nothing** when the choice equals the project default (Island / Normal / Square Normal): it re-applies the world type instead, which vanilla's `setWorldType` rebuilds from the preset, so the context returns to preset-only and any override an earlier choice wrote is dropped. The chooser also tells the player that a customized Indev world cannot use the bundled migration. [FiniteTransitionIdentity](<src/main/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentity.java>) now carries the cause's own message, so the screen reads "…: Customized finite provider settings cannot use the default apron migration; no region changes performed" instead of the bare wrapper.

**Verified.** [Four-world native receipt](<run/client/indev-probe-1792179312423.json>) `passed=true`,35 checks, 2026-10-17T06:35 local: (1) a chosen Floating/Hell/Deep Small world stores `FiniteLevelProperties[type=FLOATING, theme=HELL, width=64, length=64, height=256]`, generates the `indev_hell` biome and a border of 64; (2) **confirming the defaults writes no override** and keeps `indev_normal` - the new guard against the trap; (3) an Indev world that never opened the chooser still writes nothing, keeps `indev_normal` and the 256 border; (4) an ordinary world has no historical metadata and no Moderner Beta generator. Offline build exit0, **709 tests,0 failures/errors/skips** (the new test pins that the refusal names customized settings as the cause and covers the exact override shape the chooser writes), whitespace clean.

**What this does NOT do - and the decision it leaves.** An **existing** world that already has a custom override (like the owner's "New World") still cannot use the finite-to-infinite migration. Un-customizing it is not a fix: its terrain was generated with the customized footprint, so reverting the settings would misalign saved chunks with the generator. The only real path for such worlds is to generalize the apron release to the world's own stated geometry (footprint and height from `finite_level_properties` rather than hardcoded 256x256x128) with its own exact-block evidence and verification - which reopens the finite-transition machinery the owner previously asked me not to churn, so it is a decision to take deliberately rather than in passing. Everything else about such a world is unaffected: it plays, saves and upgrades its *version* normally; only the finite-edge apron migration refuses. Earlier records preserved below.

# Current work — 2026-10-17 / Indev map settings accepted: chosen options reach the world

Goal `goal-11ea56e7-13e9-420a-abab-ae263eef8da0` round3/6. **[Native receipt](<run/client/indev-probe-1792177034051.json>) `passed=true`,25 checks, 2026-10-17T05:57 local**, one client, three worlds, all through the real screens.

**What is proven.** With no version highlighted the picker offers nothing; selecting the Indev row makes "Indev settings..." live; it opens the chooser; Floating + Hell + Deep + Small records exactly `Floating, Hell, Deep Small 64x64x256`; the world is created in this project's own saves directory and loads with a Moderner Beta generator. On the **running server** the chosen values are in the world: `FiniteLevelProperties[type=FLOATING, theme=HELL, width=64, length=64, height=256]`, the generated biome at spawn is `moderner_beta:indev_hell`, the world border follows the chosen footprint (**64**, not upstream's 256), and the world is still the `indev_latest` profile. An Indev world created **without** opening the chooser gets **no override at all**, keeps `moderner_beta:indev_normal` and upstream's own 256 border. An ordinary world still has no historical metadata and no Moderner Beta generator.

**The three defects the native runs caught, all fixed:** (1) the theme lives in TWO upstream places - the chunk settings' `finite_level_properties.theme` and the biome settings' `single_biome` - and setting only the first generated Hell terrain under a Normal biome; the biome is now set for the chosen theme. (2) The settings object is a preset **delta** (`historical_experience:indev_latest_with_ores` plus nothing), on the client during creation as much as on a loaded server, so detecting an Indev world by the presence of `finite_level_properties` silently answered "not Indev" and the choice was never applied; detection now comes from the chosen version via `HistoricalVersions.isFiniteWorld`. (3) `ModernBetaSettings.Builder.replace` leaves an **absent** component absent, so the dimensions were silently discarded while the biome override (an `add`) took effect - the asymmetry the run showed as "theme arrived, dimensions did not"; it is an `add` now. Also fixed: returning from the chooser re-initialised the picker from its opening key and greyed out "Use this version".

**Limits, stated plainly.** Only the Hell theme was exercised natively; Paradise and Woods resolve through the same one-line biome lookup but were not run. Level type was exercised as Floating; Island (the default) is what the untouched world proves, and Inland was not run. Flat remains unavailable - upstream has no flat Indev type - and is shown with that reason. The researched height conflict (table 64 vs upstream's preset 128) is unchanged: a world that never opens the chooser keeps upstream's preset, and the chooser writes the researched table.

**Build state:** offline build exit0, **708 tests,0 failures**, whitespace clean. [Guide](<docs/INDEV-MAP-SETTINGS.md>) and [research](<research/indev-map-settings.json>) describe the option set with sources. Objective met; the goal is marked complete. Earlier records preserved below.

# Current work — 2026-10-17 / Indev settings: native run reached the world, two gaps found

Goal round2/6. The native probe now drives the whole real flow and the world it creates **is** an Indev Moderner Beta world — but two concrete defects were exposed, so the feature is **not** accepted yet.

**What the run proved** ([probe](<src/main/java/net/historicalexperience/client/indev/IndevMapProbe.java>), run 2026-10-17T05:49 local, `-PindevProbe`): with no version highlighted the picker's Indev button is inactive; the Indev row can be selected in the real picker and the button becomes live; it opens the chooser; pressing Floating/Hell/Deep/Small then "Use these settings" records exactly `Floating, Hell, Deep Small 64x64x256`; the world is created in the project's own saves directory, loads, and its overworld generator **is** a `ModernBetaChunkGenerator`. Earlier rounds' claims about the option set, gating and the picker path are now backed by a running game.

**Gap 1 - the theme is not fully applied.** Our presets express a theme in TWO places: `chunkSettings.finite_level_properties.theme` (terrain material, boundary liquid) and `biomeSettings.single_biome` (`moderner_beta:indev_hell` vs `indev_normal`). The adapter only replaces the chunk-settings component, so a "Hell" choice would generate Hell terrain under a **Normal biome**. Fix: build the biome settings for the chosen theme too (`SettingsComponentTypes` biome/single-biome component plus a biome registry lookup for `moderner_beta:indev_<theme>`), and assert the loaded biome at spawn as the proof.

**Gap 2 - the server-side settings are a preset delta, so the current detection is wrong.** On the running server, `getChunkSettings().get(FINITE_LEVEL_PROPERTIES)` is **null**: the stored settings carry `moderner_beta:preset` plus the project's overrides, and the component is resolved through the referenced preset. The client's creation context is resolved (which is why the button was offered), the server's is not. So: recognise a finite Indev world by its provider/preset reference rather than by the presence of that component, and prove the choice from generated state (loaded biome, world border, terrain shape) rather than only from the component. `ModernBetaSettings.getProvider()` and the `PROVIDER` component are the candidates to inspect first.

**Also fixed this round, found by the probe:** returning from the chooser re-initialised the picker from the *opening* key, which cleared the highlighted row and greyed out "Use this version" - the player would have had to re-select the version. [HistoricalVersionScreen](<src/main/java/net/historicalexperience/client/version/HistoricalVersionScreen.java>) now keeps the highlighted key across `init()`. This is a real bug the native run caught, and it is fixed and re-run green up to the assertions above.

**Build state:** offline build exit0, **708 tests,0 failures**, whitespace clean. Probe compile errors fixed on the way: `ServerLevel.getChunkGenerator()` does not exist (it is `getChunkSource().getGenerator()`), a probe in another package needed `SnapshotFeatureProbe.check`/`checks()` exposed, and a guard-test lambda could not throw `IOException`. **Next round:** the two gaps above, then re-run the probe to the end (chosen world, untouched Indev world, ordinary negative). No native proof exists yet for the chosen values reaching the world; do not claim it. Earlier records preserved below.

# Current work — 2026-10-17 / Indev map settings: chooser and write-path landed, native run next

Goal `goal-11ea56e7-13e9-420a-abab-ae263eef8da0` round1/6. **Implemented and compiling; the native create-world run is the acceptance still owed, so this is not finished.**

**The chooser.** [IndevMapSettingsScreen](<src/main/java/net/historicalexperience/client/version/IndevMapSettingsScreen.java>) shows the four original groups as rows of named buttons - Level type (Island, Floating, Flat, Inland), Theme (Normal, Hell, Paradise, Woods), Shape (Square, Long, Deep), Size (Small, Normal, Huge) - marks the current answer with "»", shows what will generate, what the world's generator holds now, and why Flat cannot be chosen. It is reached from the version picker, which now carries an optional third footer button ("Indev settings...") that is live **only while an Indev version is highlighted** ([HistoricalVersionScreen](<src/main/java/net/historicalexperience/client/version/HistoricalVersionScreen.java>), `Context.eraOptions`); world management passes null, because upgrading an existing world must not re-choose its map settings. The choice is remembered across screen rebuilds by [IndevMapSelection](<src/main/java/net/historicalexperience/client/version/IndevMapSelection.java>), reset on a fresh create-world screen with the rest of the choice.

**The write-path.** [IndevMapApplication](<src/main/java/net/historicalexperience/client/indev/IndevMapApplication.java>) recognises an Indev creation by its actual generator (overworld is a Moderner Beta generator whose settings carry `finite_level_properties`), then reads upstream's own settings - `ModernBetaChunkGenerator.getChunkSettings()`, `ModernBetaBiomeSource.getBiomeSettings()`/`getCaveBiomeSettings()` - replaces that one component with `SettingsComponentTypes.FINITE_LEVEL_PROPERTIES`, and hands the result to **upstream's own** `ModernBetaWorldScreenProvider.createModifier(...)` through vanilla's supported `WorldCreationUiState.updateDimensions(...)`. It constructs no generator, parses no settings JSON and writes no file; a world created without opening the chooser keeps upstream's preset untouched. It is re-applied after every world-type change, because setting a world type rebuilds the context from the preset.

**Known-and-not-yet-run.** The chooser has **no native evidence yet**. In particular these are unproven until the run: that `updateDimensions` accepts upstream's modifier in our flow at all, that the chosen type/theme reach the generated world, and that an untouched Indev world still generates exactly as before. Treat all three as open.

**Layering respected, and enforced.** The GUI must not name generators, so the adapter lives outside `client/version` in `client/indev`, and [VersionGuiWiringTest](<src/test/java/net/historicalexperience/client/version/VersionGuiWiringTest.java>) gained a check that the adapter names none of `new ModernBetaChunkGenerator`, `ModernBetaSettingsPreset`, `WorldGenSettings`, `NoiseGeneratorSettings`, `settings_preset`, `fromCompound`, `toCompound`, `ModernerBetaSettingsPresets`, `IRegistryHandler`, **and** that it hands its result to `ModernBetaWorldScreenProvider.createModifier` rather than applying settings by hand. The pre-existing guard that keeps generators out of the GUI is untouched and still passes.

**Build state:** offline build exit0, **708 tests,0 failures/errors/skips** (707 before; the new guard is the 708th), whitespace clean. Two self-inflicted compile errors were fixed on the way and are worth recording: I wrote `ModernerBetaWorldScreenProvider` for the class actually named `ModernBetaWorldScreenProvider` (the message was an unhelpful "cannot find symbol: variable", which a scratch class-literal probe disproved as a classpath problem), and used `method_15434()`/`isFiniteProfile` where the named APIs are `getSerializedName()`/`isFiniteWorld`.

**Next (the remaining acceptance):** a native run that creates Indev worlds with different choices and proves the values reached the world - stored generator settings plus the generated biome and border - with untouched-Indev and ordinary-world negatives. No native evidence exists yet for this feature; do not claim it before that run. [Guide](<docs/INDEV-MAP-SETTINGS.md>), [research](<research/indev-map-settings.json>). Earlier records preserved below.

# Current work — 2026-10-17 / original Indev map settings: researched and modelled, chooser next

Owner request: access to all the different Indev world generation types that were in the original. Active goal `goal-11ea56e7-13e9-420a-abab-ae263eef8da0` (max6 rounds) carries it; this round is the research and the data model, and the in-game chooser plus its write-path is the next step. Not finished - do not claim it is.

**Researched (sourced, not guessed).** The Indev level-generation screen had four groups, all present by the project's profile **Indev 20100223** (the last Indev build): **level type** Island/Floating/Flat/Inland ("Original" renamed to Inland on 20100109), **theme** Normal/Hell (20100107) then Paradise/Woods (20100214), **shape** Square/Long/Deep and **size** Small/Normal/Huge, whose nine combinations give the published dimensions (Small 128×128×64 / 256×64×64 / 64×64×256; Normal 256×256×64 / 512×128×64 / 128×128×256; Huge 512×512×64 / 1024×256×64 / 256×256×256). Sources are the [Indev map settings raw article](https://minecraft.wiki/w/Indev_map_settings?action=raw) and [Java Edition Indev](https://minecraft.wiki/w/Java_Edition_Indev?action=raw); dimensions are a wiki table, explicitly not read from an original jar. Recorded with confidence and open questions in [the research record](<research/indev-map-settings.json>).

**Upstream reality, checked in the pinned jar.** Moderner Beta carries the options in its own `moderner_beta:finite_level_properties` component - `FiniteLevelProperties(IndevType type, IndevTheme theme, int width, int length, int height)` - so no terrain work is involved. Its themes are exactly `normal/hell/paradise/woods` and it ships one preset per theme (`indev`, `indev_hell`, `indev_paradise`, `indev_woods`). Its level types are `island/floating/inland/classic`: **there is no `flat`**, and `classic` is Classic-era terrain rather than Indev's flat map, so original Indev's Flat is genuinely unavailable upstream and must not be presented as working. Shape and size have no enum; both collapse into width/length/height.

**Discrepancy recorded, not resolved.** The table gives height 64 for every Square and Long map, while the pinned upstream Indev preset ships **256×256×128**. Width and length agree with that preset (footprint corroborated); the height does not. The model reports the researched numbers, a world created without touching the chooser keeps upstream's preset untouched, and the conflict is written down rather than averaged away.

**Built this round.** [IndevMapSettings](<src/main/java/net/historicalexperience/worldgen/IndevMapSettings.java>): the four option enums in the original screen's order, the nine-cell dimension table, `available(type)` returning the reason Flat cannot be chosen, the project default (Island/Normal/Square Normal, i.e. what it generates today), and `finiteLevelProperties(choice)` producing exactly upstream's five keys. [Five tests](<src/test/java/net/historicalexperience/worldgen/IndevMapSettingsTest.java>) hold the option lists, the upstream ids, all nine dimension cells (and that a caller cannot mutate the table), the default, and the component - including that Flat throws instead of silently becoming a settings component. Offline build exit0, **707 tests,0 failures/errors/skips** (702 before).

**Next step, with the hook already identified.** Vanilla exposes `WorldCreationUiState.getSettings()/setSettings(WorldCreationContext)`, and Moderner Beta exposes `ModernBetaWorldScreenProvider.createModifier(settings, biomeSettings, caveBiomeSettings)` - the modifier its own settings editor uses. Next round: confirm the exact call by inspection, add the chooser to the create-world flow (only for the Indev-era profile), apply the choice to the new world's settings, then verify natively with one world per level type and per theme (stored settings, generated biome, world border), plus untouched-Indev and ordinary-world negatives. [Guide](<docs/INDEV-MAP-SETTINGS.md>) and [research](<research/indev-map-settings.json>) document the option set and the plan. No ordinary-world, Infdev or later-version behaviour is touched; no existing save is rewritten. Earlier records preserved below.

# Current work — 2026-10-17 / footer row aligned with vanilla's create/cancel row

Owner caught what the previous screenshot showed: the version row hung slightly past the Cancel button. Measured, fixed, and now asserted.

**The bug, measured rather than eyeballed.** Scanning the rendered PNG for button-face pixels: the version row spanned x=180..1097 while the create/cancel row spanned 180..1091 - a 6px (2 GUI unit) overhang past Cancel. Cause: my row was hardcoded 310 wide against [CreateWorldScreen](<src/main/java/net/historicalexperience/client/version/CreateWorldHistoricalVersion.java>)'s own action row, which the bytecode shows is two `Button.DEFAULT_WIDTH` buttons with `Button.DEFAULT_SPACING` between them = **308**, and the footer positions its rows on a shared left edge. A row of any other width cannot line up.

**The fix.** The constants are now derived, not chosen: `ROW_WIDTH = Button.DEFAULT_WIDTH * 2 + Button.DEFAULT_SPACING` (308), `ROW_GAP = Button.DEFAULT_SPACING` (8, the same gap vanilla puts between Create New World and Cancel), `TOGGLE_WIDTH = 40`, so the version button is 260. Both edges now agree to the pixel: re-measuring the new screenshot gives 183..1094 for the version row AND 183..1094 for the create/cancel row.

**Asserted so it cannot regress.** [The probe](<src/main/java/net/historicalexperience/client/version/SnapshotFeatureProbe.java>) now finds the real Create New World and Cancel buttons and checks `version.left == create.left` and `toggle.right == cancel.right` in GUI units. [Receipt](<run/client/snapshot-probe-1792174881517.json>) `passed=true`, "version button's left edge matches Create New World exactly (59 vs 59)" and "toggle's right edge matches Cancel exactly (367 vs 367)", alongside the unchanged behaviour checks (same height, plain ON/OFF label, 8px gap, off keeps the version remembered, greys the button, restores `minecraft:normal`, on re-applies the version). [Screenshot](<run/client/screenshots/create-world-toggle-1792174883675.png>). Offline build exit0, 702 tests,0 failures/errors/skips. [GUI notes](<docs/GUI-REDESIGN.md>) updated. Earlier records preserved below.

# Current work — 2026-10-17 / historical-version switch is now a plain ON/OFF button beside the version button

Owner follow-up to the switch added minutes earlier: **not coloured, beside the button with a visible disconnect, same height, a simple ON/OFF.** Rebuilt that way and verified natively.

**What changed.** The custom beveled/coloured widget is **deleted** ([HistoricalVersionToggle.java](<src/main/java/net/historicalexperience/client/version/HistoricalVersionToggle.java>) no longer exists); the switch is now an ordinary vanilla `Button` whose whole label is `ON` or `OFF`, so it uses the vanilla button texture at the vanilla button height and nothing is drawn by this project. [CreateWorldHistoricalVersion.createRow](<src/main/java/net/historicalexperience/client/version/CreateWorldHistoricalVersion.java>) returns a vanilla `LinearLayout.horizontal().spacing(4)` holding the version button (266 wide) and the switch (40 wide): **a 4px visible gap**, no overlap, and 266+4+40 keeps the row at exactly the 310px the button alone used to occupy, so it still lines up with vanilla's Create/Cancel row. The old hand-written composite `LayoutElement` is gone with the custom widget. Labels are refreshed from a per-screen entry in a `WeakHashMap`, so a rebuilt screen replaces its own pair instead of looking it up by message prefix; the version picker's confirm refreshes the same pair in place.

**Behaviour unchanged from the accepted version** (the owner only asked about appearance): off means an ordinary world and does not forget the chosen version; the version button greys out; on restores it with no second trip through the picker; only a preset of ours is taken back off, so a player's own Large Biomes/Amplified choice survives; snapshot staging is skipped while off. Creation only.

**Evidence.** [Native receipt](<run/client/snapshot-probe-1792174415349.json>) `passed=true`,197 checks, 2026-10-17T05:13 local, driven through real screen input: both widgets present; switch starts ON; **same height as the version button and level with it**; **its whole label is "ON"** (a plain button, nothing custom); **it sits beside the button with a 4px gap and the pair keeps the 310px row**; clicking turns the version off and the label becomes "OFF"; the chosen version stays remembered; the version button greys; `minecraft:normal` is restored; clicking again restores the choice and re-applies `historical_experience:release_1_1`; the button is live again. [Screenshot](<run/client/screenshots/create-world-toggle-1792174417367.png>) shows the plain grey ON button beside the version button. All earlier menu checks still pass in the same receipt. Offline build exit0, **702 tests,0 failures/errors/skips**. [GUI notes](<docs/GUI-REDESIGN.md>) updated to record the removal and the new geometry. Earlier records preserved below.

# Current work — 2026-10-17 / historical-version ON/OFF switch in the create-world footer

Owner changed the request from a "none" row in the version list to a **switch beside the version button** in the world-creation GUI: on/off, tucked into the button, properly spaced. Implemented and verified natively.

**What was built.** [HistoricalVersionToggle](<src/main/java/net/historicalexperience/client/version/HistoricalVersionToggle.java>) is the classic beveled ON/OFF switch, drawn from own code only; the old flat look of the era's own option switches is deliberate. [CreateWorldHistoricalVersion](<src/main/java/net/historicalexperience/client/version/CreateWorldHistoricalVersion.java>) now hands vanilla's footer one composite [LayoutElement](<src/main/java/net/historicalexperience/client/version/CreateWorldHistoricalVersion.java>) holding both widgets, because two separate children of that row would each be centred on their own and put the switch in the middle of the window. Measured and asserted: the version button is 288 wide, the switch 26x20, the switch starts 4px inside the button's right edge, the pair is exactly the 310px row the button alone used to occupy, and both share one y and height. The switch is registered through vanilla's own `visitWidgets`, so no widget list is manipulated by hand.

**Behaviour, and the owner's actual goal.** Off means an ordinary world and **does not forget the chosen version**; the button greys out and its label stays; on restores that same version with no second trip through the picker. While off, `applyStoredSelection` puts vanilla's `minecraft:normal` world type back only when a preset of OURS was applied, so a player who chose Large Biomes or Amplified keeps it, and snapshot staging is skipped so no patch is bound to the new directory. Picking a version in the list switches the feature back on rather than leaving the switch contradicting the screen. `Selection.enabled` is the single gate; it is reset by `openFresh`/`createFromExisting` with the rest of the choice. Creation only - world management still changes an existing historical world's version through the upgrade transaction.

**Evidence.** [Native receipt](<run/client/snapshot-probe-1792173734259.json>) `passed=true`,195 checks, Minecraft1.21.1/Java21, root-pinned developer world, 2026-10-17T05:02 local. The switch is driven through **real screen input at its own centre**, and the receipt records: footer has both widgets; switch starts on with a version chosen; aligned with the button to the pixel; cuts 4px in and the pair keeps the 310px row; clicking it turns the version off; the chosen version is still remembered; the button is greyed; `minecraft:normal` is restored; clicking again restores the remembered choice and re-applies `historical_experience:release_1_1`; the button is live again. [Screenshot](<run/client/screenshots/create-world-toggle-1792173736208.png>) confirms the arrangement by eye. All earlier menu checks still pass in the same receipt: B triple toggling, the 316-click hotbar band sweep, QUICK_MOVE deletion, catalog discard, refused future insertion/drop/CLONE, saved-player preservation. Offline build exit0, **702 tests,0 failures/errors/skips** (one new JVM test asserts off keeps the choice and on restores it).

**Probe harness fix, honestly reported.** The first run of this change [failed](<run/client/snapshot-probe-1792173295679.json>) with a 180s timeout and no world start. Cause was **vanilla's own experimental-features confirmation** (`ConfirmScreen` "Warning! These settings are using experimental features", buttons Yes/No), which the village/anvil probe already accepted but the 11w49a menu probe never did; the world is not created until it is answered. That is why the earlier 11w49a runs sometimes passed and sometimes hung - the dialog is timing dependent, not caused by the switch. The handling now lives once, in [SnapshotFeatureProbe.acceptVanillaWorldWarning](<src/main/java/net/historicalexperience/client/version/SnapshotFeatureProbe.java>), accepting only that one vanilla warning and logging every screen transition (`[hex-probe-screen]`); no legal/EULA dialog is ever accepted. The duplicate copy in the anvil probe and its unused toggle choreography were removed so one place owns it. Earlier PROGRESS text claiming the driver already handled this for every probe was too broad; it is true from this run.

Docs synchronized: [GUI redesign](<docs/GUI-REDESIGN.md>) lists the switch and the deliberate creation-only scope; [adding a version](<docs/ADDING-A-VERSION.md>)'s quoted `applyStoredSelection` snippet and line range were updated to the current code and now state the switch gate. No terrain, registry, gameplay-rule, frozen-recipe or ordinary-world change; no commits, deployments, personal saves or terms acceptance. Earlier records preserved below.

# Current work — 2026-10-17 / hotbar band click routing repaired

Owner follow-up: after the first gap fix the item was no longer thrown, but a click in the separator band "just doesn't go into the slot", needing repeated clicks. Cause: the band between the catalogue's last painted row and the hotbar was handled by extending each hotbar slot's native hover box only part-way, and clicks that matched no slot fell into the "inside the panel, preserve the cursor" branch — so the item stayed on the cursor while the click looked ignored. Row 178 (the catalogue's bottom painted edge) additionally still resolved to the bottom catalogue cell, which discards the held stack by design.

Fix in [LegacyCreativeScreen](<src/main/java/net/historicalexperience/client/LegacyCreativeScreen.java>): `mouseClicked` now routes a held stack clicked anywhere below the catalogue — separator rows, side margins and the strip under the hotbar — straight to the hotbar slot under the pointer, snapped to the nearest of the nine columns. Geometry is derived from the menu (`catalogBottom()`), not from hardcoded rows. `isHovering` agrees with that band and stops the bottom catalogue row at its painted interior, so the highlighted slot is always the slot the click fills. Non-slot padding elsewhere in the panel (title strip, catalogue side margin) still preserves the cursor instead of throwing the item out; shift-click deletion, catalogue click-to-clear, unsupported-stack freezing and outside-panel dropping are unchanged. No asset, terrain, registry or server-authority change.

**Evidence:** [native receipt](<run/client/snapshot-probe-1792170860656.json>) `passed=true`,2026-10-17T04:14 local, Minecraft1.21.1/Java21, root-pinned developer world. New sweep clicks **316** positions — rows178–207 across all nine columns, plus both side margins over rows179–201 — and every one places into the hotbar slot under the pointer (`[hex-gap-sweep] 176:DDDDDDDD- 178..207:PPPPPPPPP`). Rows170–177 remain catalogue cells that discard a held stack as the owner asked; column9 of row176 is the scrollbar strip. All prior menu checks stay green in the same receipt: B triple-press toggling, separator grants on all nine columns, internal-padding and side-margin cursor preservation, catalogue click-to-clear variants, native QUICK_MOVE deletion of all nine stacks with no item entities, refused future insertion/drop/CLONE, RAW packet preservation, and exact saved-player reopen preservation. Offline build (job1973) exit0, **701 tests,0 failures/errors/skips**; whitespace clean. Earlier records preserved below.

# Current work — 2026-10-17 / hotbar and village-resident fixes accepted

Goal round1 completes `goal-96fdc37e-a248-4a8e-9ea3-5239612fee20` within the bounded requested scope. **Hotbar separator/padding and Shift-delete** are implemented and [native-verified](<run/client/snapshot-probe-1792163853003.json>):9 real separator-to-hotbar server grants, no internal-padding drops,9 native QUICK_MOVE whole-stack deletions without item entities; old B toggle/catalog-discard/unsupported source+CLONE/drop/cursor/save-reopen controls remain passing. The native Shift test dispatches the actual QUICK_MOVE path; it is not a claim of synthetic physical modifier/hardware acceptance.

**Owner's 'now no villagers' feedback exposed a real omission:** first repair copied only the five upstream-populated types and incorrectly treated small houses/shacks as nonresidential because upstream omitted their calls. Layouts composed only of those homes had no eligible residents. [Resident data](<src/main/java/net/historicalexperience/worldgen/HistoricalVillageResidents.java>) now includes one resident in each small/fenced-small house at local(2,2,2), and each shack/raised-roof shack at(1,2,2). Positions were inspected against existing floors at y1, walls/headroom, ladder/fence/table/door. These four additions are an explicit own population repair, not claimed upstream call sites/original-artifact algorithms. The five prior upstream positions remain. All nine residential types have caps1/2 per piece; well/roads/farms have0. Saved per-piece claims and current-chunk clipping prevent multiplication. No existing NPCs deleted/moved; no terrain/layout/loot/frozen recipe/settings or mob-list changes, no giants.

**Final native proof:** [1970 receipt](<run/client/village-resident-probe-1792166227822.json>) passed=true. Actual Release1.1 generation at village chunk[-118,23] has14 planned/claimed and **14 live loaded villagers** (not just plan/NBT counts). The corresponding actual Beta1.8.1 village has0, correctly. [Native acceptance](<src/main/java/net/historicalexperience/server/VillageResidentAcceptance.java>) additionally exercises all nine residential types in four rotations:44 actual NPCs for1.1 and1.4,0 forBeta1.8.1 and ordinary; exact interior coordinate multiplicities; a two-resident north-facing house split across chunks; registered piece compressed disk NBT save/load; three reordered/repeated callback passes with unchanged complete UUID sets; exact existing sentinel villager UUID/name/age/health/equipment/brain data. Ordinary metadata remains absent and no historical villagers are added there. Marker fixtures and real natural generation are distinct; no whole-world/original-villager-AI certification is claimed.

**Retained failures, not fabricated passes:**1966 native world entry timed out on the experimental-worldgen confirmation before generation;1967 diagnostic import failed compilation and was fixed;1968 first five-type native plan passed with only3 actual1.1 residents and did not disprove the owner's unspecified save.1969 after home-coverage repair proved14 actual natural villagers, but its enlarged distant fixture saw39 immediately tracked NPCs despite44 successful claims.1970 compacts the fixture into already tracked player-area chunks (same44 actual-entity/position assertions), explicitly splits a house across chunk boundaries, and passes all four worlds. The driver now handles only the exact experimental-worldgen stability warning in root-pinned opt-in fresh-world fixtures, never generic/legal/EULA dialogs. The owner's exact version/save was requested but not supplied; it was not claimed reproduced.

**Final checks:**1970 offline full build18s, native run exit0 with passing receipt;1971 independently aggregates **701 tests,0 failures/errors/skips**, validates research JSON and both positive native receipts, hashes artifacts and passes whitespace. [Runtime mod](<build/libs/historical-experience-1211-0.1.0.jar>) SHA256 `740e3bce4c98e7c07a7e9aa07a38187035afde4953619c90c44f0937c4f3d5df`; [sources](<build/libs/historical-experience-1211-0.1.0-sources.jar>) SHA256 `1e3e79f9ac39685766914d3f5b02629809f3450dfb857bfecde6b08738530156`. Remap and licence artifacts pass. Relevant source/new files reviewed; no personal saves/settings/accounts, terms acceptance, deployment, commits, resets or upstream edits.1963 API job and1966–1971 jobs all collected; no jobs running, no subagents.

[Village guide](<docs/VILLAGE-RESIDENTS.md>), [research](<research/village-resident-placement.json>), [provenance](<docs/PROVENANCE.md>) and [menu guide](<docs/SNAPSHOT-PATCHES.md>) synchronized. **User action:** restart the updated build and test newly generated villages. Already-generated empty villages are not automatically repopulated, nor are old well crowds silently cleaned up. Beta1.8.1 remains intentionally empty; villagers startBeta1.9pre1. Existing unsupported stacks/villagers remain preserved. These are developer Fabric1.21.1/Java21 native tests, not a production-wide fidelity claim. Do not reopen older finite terrain repair. Earlier records preserved below.

# Current work — 2026-10-17 / hotbar gestures accepted; village residents native acceptance next

**Active new goal** `goal-96fdc37e-a248-4a8e-9ea3-5239612fee20`, revision1, max4 rounds: fix internal-gap throwing + hotbar Shift-delete and historical village well overpopulation, preserve ordinary worlds/existing villagers. Prior snapshot goal is complete; do not reopen finite terrain repair. No subagents (earlier direct prohibition), no commits/deployment/personal saves/EULA changes.

**Menu request implemented and native-verified:** [legacy screen](<src/main/java/net/historicalexperience/client/LegacyCreativeScreen.java>) extends hotbar hit targets through the separator and painted borders; internal null-slot padding no longer means outside-world drop. Hotbar QUICK_MOVE deletes the complete grantable stack through existing validated native creative slot packets; unsupported stored stacks remain frozen/preserved. [Native receipt1960](<run/client/snapshot-probe-1792163853003.json>) passed=true, including all9 separator columns with actual server stone grants, padding cursor preservation, all9 native QUICK_MOVE server deletions without item entities, and earlier B toggle/catalog discard/unsupported source+drop+CLONE/cursor/save-reopen checks. [Probe](<src/main/java/net/historicalexperience/client/version/SnapshotFeatureProbe.java>) invokes the exact QUICK_MOVE dispatch through [container accessor](<src/main/java/net/historicalexperience/mixin/ContainerScreenAccess.java>); it does NOT certify physical Shift hardware injection.1958/1959 retained false timeout receipts after the padding checks because posted Win32 modifier input was not reliable;1960 changed the diagnostic to native dispatch, not production behavior. All three full builds passed696 tests; normal process exit alone was not mistaken for acceptance. [User guide](<docs/SNAPSHOT-PATCHES.md>) updated.

**Village cause found and production fix implemented, native acceptance PENDING:** old [afterPlace](<src/main/java/net/historicalexperience/worldgen/HistoricalVillageStructure.java>) compared current chunk to the callback clip-box corner (not a durable whole-structure anchor), then spawned at every non-road piece centre, including the well, with a fresh cap8 on every callback. Replaced that loop with piece-owned upstream residential coordinates, current-chunk clipping and saved `HexResidentMask` claims after successful native entity insertion. [Piece implementation](<src/main/java/net/historicalexperience/worldgen/HistoricalVillagePieces.java>) keeps Beta1.8.1 empty, pre1.3 no-profession and old-profession/plain-skin policy. The outer structure alias/version gate remains, including ordinary-world isolation; the piece helper deliberately does NOT rely on metadata/cache that may not yet exist during initial spawn-region generation. Only entity population code/new piece NBT is changed, not terrain/layout/loot/frozen presets or recipes. No existing NPC removal/movement and no giants.

[Declarative resident coordinates](<src/main/java/net/historicalexperience/worldgen/HistoricalVillageResidents.java>) are directly inspected from the owner-designated pinned MIT-declared OldVillages source: large house type4=(2,2,3),(4,2,3); blacksmith7=(4,2,2); tavern8=(1,2,7),(4,2,7); church9=(2,2,7); library12=(2,2,3). Well/road/farms/small houses/shacks have no call sites in that source. [Research](<research/village-resident-placement.json>) records exact source URL/lines and confidence; [provenance](<docs/PROVENANCE.md>) updated, existing notices preserved. This is the pinned port's population, not a new original-Minecraft fidelity claim.

**Latest verification:**1961 offline full build20s and whitespace exit0;1962 aggregates **700 tests,0 failures/errors/skips**. [Four new bootstrapped JVM tests](<src/test/java/net/historicalexperience/worldgen/HistoricalVillageResidentsTest.java>) check exact five-piece counts/coordinates, no well/nonresidential spots, immutable lists, all four native rotations at negative coordinates, saved own-NBT resident claims/default/clamping. Main/sources remap/licence checks pass. They do not yet prove actual entity insertion or repeated afterPlace/save-reload behavior. All1958–1962 background jobs collected; none running. No Java edits during a build/native run.

**Next bounded step:** implement/run a developer-owned native population acceptance. Exercise actual structure afterPlace/piece entity insertion at residential coordinates, chunk-split/negative/all4 orientation ownership, callback order/repetition, persisted ledger reload, empty Beta1.8.1 and ordinary-generator negatives, existing villager preservation. Prefer a small owned fixture using real pieces/ServerLevel and callbacks (API-inspect first) or extend the existing [village verification](<src/main/java/net/historicalexperience/server/VillageVerification.java>); do not turn this into a broad terrain audit. Verify actual spawned UUID/count/positions, not just that the plan/helper says so. Keep fixtures under project run, no legal acceptance. Read relevant native APIs for structure serialization context/pieces creation and current village probe flags before choosing the smallest valid harness. After passing native evidence, synchronize research/docs/current milestone, review scope/whitespace and complete the NEW goal. Do not claim village runtime acceptance yet. Earlier records preserved below.

# Current work — 2026-10-17 / requested snapshot/menu slice accepted

Goal round3 completes this bounded snapshot/menu request, including the owner's two clarifications. **B toggles Survival ↔ Creative** once per press (private11w49a owner, cheats disabled); **clicking a catalog cell with an item held clears the cursor**, including same/different/right clicks, instead of replacing/increasing it. [Server toggle](<src/main/java/net/historicalexperience/server/SnapshotCreativeRequest.java>), [old menu](<src/main/java/net/historicalexperience/client/LegacyCreativeScreen.java>), player-facing selector text and its tests agree. [Latest11w49a native receipt](<run/client/snapshot-probe-1792162092679.json>) has passed=true: three real exact-window B presses, cursor-discard cases,72-slot layout/scroll, mouse grant, future insertion/drop/CLONE refusal, resync, actual save/reopen and exact count/name/damage/custom-data player sentinel preservation.

**12w49a and controls accepted:** [four-world receipt](<run/client/snapshot-anvil-probe-1792162354757.json>) has passed=true. One client process creates/selects12w49a, unpatched1.4, unpatched1.1, then ordinary worlds. Snapshot12 single dirt/glass/bowl/apple previews work in Survival; stacked targets refuse in Survival and Creative without consumption; Fortune/Silk Touch conflict and original input components remain. An actual AnvilScreen/native C2S result click yields one Unbreaking dirt, keeps its name/custom data, consumes book/target and charges exactly the displayed XP. The unpatched/ordinary Survival non-enchantable targets remain refused, while their existing normal-tool behavior is unchanged. Real B and forged B requests cannot activate the feature outside11w49a.

Native replacement tab/search selection is exercised on12w49a and1.4, checks every displayed stack against the dated component policy, and verifies shared search-list size is unchanged. The later ordinary world has no historical metadata, restores netherite search entries and normal Creative CLONE in the same process. Normal Creative chest pickup/shift-transfer preserves the unsupported rich sword; clone-style quick-craft cannot duplicate it in historical worlds. Disconnect clears client patch scope. [Probe](<src/main/java/net/historicalexperience/client/version/SnapshotAnvilProbe.java>) uses native menus/packets; its tab invoker is inert unless explicitly armed.

The first four-world run retained a false receipt because the diagnostic incorrectly assumed every pre1.4 constructed anvil menu was globally blocked. [Existing early menu policy](<src/main/java/net/historicalexperience/gameplay/HistoricalEarlyMenuRules.java>) deliberately audits only Indev/Infdev; the corrected control tests that existing baseline rather than inventing a new general restriction. **No production anvil-availability or finite-repair behavior was broadened.** Initial and rerun receipts remain intact.

**Final verification:**1953 offline full build21s + native11w49a45s,1954 full build20s + four-world native73s, all exit0.1956 independently aggregates [JUnit reports](<build/reports/tests/test/index.html>): **696 tests,0 failures/errors/skips**, validates both research JSON documents and passing native receipts, hashes artifacts and passes whitespace. [Runtime mod](<build/libs/historical-experience-1211-0.1.0.jar>) SHA256 `2c247aa442e674deab7de827740ba0962b632d1367b2080ad79d28cb648dce40`; [sources archive](<build/libs/historical-experience-1211-0.1.0-sources.jar>) SHA256 `ebe503c96cd4703051874274125f88214bca3d030991cc0270cde4504e5116cb`. Main/sources licence artifacts checked. Tracked/untracked scope reviewed; no generation recipe, palette, algorithm or mob-spawn-list changes. No giants, personal saves/accounts/settings, legal acceptance, deployments or commits.

[User guide](<docs/SNAPSHOT-PATCHES.md>), [patch research](<research/snapshot-patches.json>) and [catalog scope](<research/creative-menu-catalog.json>) synchronized. **Limits, not new audit objectives:** own geometry/runtime icons, curated non-exhaustive catalog, host replacement tabs (not exact historical tab reconstruction), parent terrain/coarse1.4 baseline, native costs (not original snapshot algorithms); linked pack403/license unverified/no copied assets. Same-parent/cross-parent Manage backup/journal/recovery has JVM transaction evidence, not a newly claimed native Manage confirmation. Dedicated/LAN/non-owner sessions and saved-toolbar UI were not separately automated; safety policy/component/new-grant checks have unit/common-handler evidence. These are developer Minecraft1.21.1/Java21 runs, not an original-artifact or production-wide certification. Known texture/blocksource/pre-provider diagnostic warnings remain outside this slice.

All1951–1956 jobs collected; none running. Overall requested implementation is complete within those documented fidelity limits. Do not reopen the older finite terrain repair. Earlier milestone records follow unchanged.

# Current work — 2026-10-17 / creative implementation and native11w49a acceptance

Goal round2 installed the bounded creative slice. **Overall goal remains active: native12w49a anvil, unpatched/ordinary and replacement-menu controls are next.** Do not reopen the completed finite terrain repair or expand into a general audit.

[Positive dated catalog](<src/main/java/net/historicalexperience/gameplay/HistoricalCreativeCatalog.java>) is independent of fail-open chest loot/coarse crafting tables. It handles old slab/bed/dye/fish representations, dated wood/color/egg variants, potions and positive enchantments; unknown/modded items are refused only for known historical selections. [Catalog notes](<research/creative-menu-catalog.json>) explicitly disclose curated/non-exhaustive membership, coarse families and unverified original order/artwork. Old rose/pumpkin/cloth armor use modern project representations. Later buckets and cow products do not leak into June11/earlier profiles. No giants or spawn-list changes.

[Own legacy screen](<src/main/java/net/historicalexperience/client/LegacyCreativeScreen.java>) provides8 columns ×9 rows,72 catalog slots,9 real hotbar slots, scrolling/mouse grants and runtime item icons, selected only for Beta1.8.1 through pre1.3. [Native working-list filter](<src/main/java/net/historicalexperience/mixin/CreativePickerHistoricalCatalogMixin.java>) applies to other historical creative pickers without changing global tab caches or saved inventory. [New-grant validator](<src/main/java/net/historicalexperience/gameplay/HistoricalCreativeGrants.java>) checks count, dated enchantments/potions and rich components using a comparison copy, never sanitizing the real stack.

[Authority](<src/main/java/net/historicalexperience/server/HistoricalCreativeAuthority.java>) and [main-thread packet hook](<src/main/java/net/historicalexperience/mixin/ServerCreativeHistoricalGrantMixin.java>) freeze both removal and replacement of unsupported owned stacks, deny future insertion/drop and unavailable offhand writes, and resync inventory/current menu/cursor. Identical rich no-ops are canceled before native coordinate-based block-entity copying. [Menu hook](<src/main/java/net/historicalexperience/mixin/MenuHistoricalCreativeCloneMixin.java>) refuses unsupported CLONE and clone-style QUICK_CRAFT; normal server-owned pickup/swap/chest/drop is not filtered. This intentionally means unsupported items must be moved using a chest/Survival interface rather than raw creative writes.

**Native acceptance passed:** [11w49a receipt](<run/client/snapshot-probe-1792160232039.json>) has passed=true. [Screenshot](<run/client/screenshots/sol-snapshot-native-1792160232039-legacy.png>) visually inspected. [Explicit developer probe](<src/main/java/net/historicalexperience/client/version/SnapshotFeatureProbe.java>) creates a uniquely named owned world through the actual Create→Historical Version→Snapshots row→confirmation flow; native directory metadata and runtime patch match. Exact-Minecraft-window Win32 key messages exercised GLFW B polling and the real C2S handler: private owner entered Creative from Survival with cheats disabled. Native old-menu opening,72 cell coordinates/last-page policy, real mouse-to-hotbar/server stone grant, future source/insertion/drop/CLONE rejection, cursor/source resync, normal stop, actual saved-world reopen and exact named/damaged/custom-data netherite player sentinel all passed. No personal installation, EULA changes or global keyboard injection. This is developer runtime evidence, not original-snapshot artifact execution or a production-client sweep.

First native driver reopened immediately during resource reconciliation and stalled in native doWorldLoad. [Thread dump](<logs/20261017-snapshot-native-hang-v1.txt>) retained; managed run stopped after its first normal save. Driver now waits for overlay completion/title frames; rerun completed normally in1m12s. No production loading/texture code was altered. Known empty dynamic blocksource, missing-resource/profile and pre-provider sea-level diagnostic warnings are not claimed fixed by this slice.

**Build/tests:** final full offline build/remapped main+sources/licence checks and whitespace check passed in20s: **696 tests**, zero failures/skips. [17 catalog/component regressions](<src/test/java/net/historicalexperience/gameplay/HistoricalCreativeCatalogTest.java>) include the early bucket/cow-product boundary correction. Existing native1.1 item membership is unchanged by those early-boundary corrections. Native test driver initially failed Java's final-class/interface cast check; corrected with the normal mixin `(Object)` bridge before successful runs.

**Next bounded work:** extend native acceptance to12w49a Survival/Creative single/stacked anvil targets with actual output/XP/book/component checks; unpatched1.4/1.1 and ordinary positive controls; native replacement-menu/search and conservation/chest/quick-craft controls. Actual Manage confirmation/restart remains JVM transaction evidence unless exercised. Exact old pixels/order/complete catalogs, original anvil cost algorithm and original multiplayer B behavior remain unverified, not new audit objectives.

Jobs1938/1939 native API inspection,1940/1941 builds,1944 failed probe compilation,1945 test totals,1947 thread dump,1948 passing native rerun and1949 startup wait inspection collected.1946 was canceled/collected after the retained dump. Final1950 build696/whitespace also collected; no running/uncollected jobs. No subagents, deployment or commits. Earlier records follow unchanged.

# Current work — 2026-10-17 / snapshot selection and transactions, creative menu still pending

Goal round1 implements the Snapshots tab, parent-preset creation mapping and existing-world transaction integration. **The overall requested feature is NOT complete.** Do not reopen the completed finite repair or expand into unrelated audits.

[Catalogue](<src/main/java/net/historicalexperience/version/HistoricalVersionCatalogue.java>) now has explicit11w49a/12w49a patch entries in a Snapshots group, not new terrain profiles. [Player text](<src/main/java/net/historicalexperience/client/version/VersionText.java>) discloses parent release, feature, baseline and incomplete snapshot fidelity by default; the scrollable overview explicitly describes the baseline. [Create helper](<src/main/java/net/historicalexperience/client/version/CreateWorldHistoricalVersion.java>) resolves the patch to its existing parent preset while preserving the selection key, and synchronizes native preset changes. [Creation mixin](<src/main/java/net/historicalexperience/mixin/CreateWorldHistoricalFooterMixin.java>) clears new/recreated UI choices and binds only after native **createNewWorldDirectory** succeeds (not the previously proposed WorldOpenFlows.createFreshLevel). [One-shot intent](<src/main/java/net/historicalexperience/world/HistoricalSnapshotCreation.java>) is normalized/exact-directory/parent-validated; [bootstrap](<src/main/java/net/historicalexperience/server/HistoricalServerLifecycle.java>) consumes it only for initial metadata and preserves existing metadata, discarding matching stale intent.

[Upgrade planner](<src/main/java/net/historicalexperience/world/upgrade/GenerationUpgrade.java>) stages patch/clear changes with the exact existing alias for the same parent; cross-parent choices use the normal destination recipe with one new revision/history entry. Existing backup, journal, locking and recovery are reused. [Service/inspection](<src/main/java/net/historicalexperience/server/HistoricalUpgradeService.java>) keeps profileId canonical and adds explicit selectionId/patch identity. [Manage helper](<src/main/java/net/historicalexperience/client/version/ManageWorldHistoricalVersion.java>) selects by selectionId; operator suggestions include patches. Running snapshot scope still changes only on restart.

**Tests/evidence:** [Creation tests](<src/test/java/net/historicalexperience/world/SnapshotCreationTest.java>), [transaction tests](<src/test/java/net/historicalexperience/server/SnapshotUpgradeTest.java>) and [selector tests](<src/test/java/net/historicalexperience/client/version/SnapshotSelectorTest.java>) cover exact-path/parent/one-shot/cancel/existing-world safety, patch/clear exact alias and byte-identical same-parent level data, cross-parent history and sentinel preservation, verified backups, simulated journal-before-metadata recovery, ordinary/locked/unknown/no-op refusal and truthful UI identity. Final offline full build/remapped main/sources/licence artifacts and whitespace check pass: [679 tests](<build/reports/tests/test/index.html>), zero failures/skips (667 foundation/adapters +12 new selection/transaction regressions). Focused diff reviewed. No process-crash campaign or actual Minecraft Create/Manage confirmation-to-save/reload claim.

Native Minecraft1.21.1/Java21 [v2 GUI receipt](<logs/20261017-snapshot-selectors-v2.json>) and [small-window screenshot](<run/client/screenshots/hex-snapshots-v2-selector-snapshot-12w49a-scale-3.png>) show both patch choices at scales1/2/3 and passing existing footer bounds checks. V1 found a clipped Snapshots caption; v2 uses content-sized compact buttons and a yellow selected label, visibly fixing it. [Probe](<src/main/java/net/historicalexperience/client/version/ScreenProbe.java>) now includes both patches and a safe unique screenshot prefix, preserving earlier photos. Runs exited normally, never opened/created a world, touched no personal installation and accepted no terms. Known title-load empty dynamic blocksource log/profile-lookup timeout did not prevent menu rendering; this is not world/gameplay acceptance.

**Next bounded slice:** implement the requested legacy creative menu and dated catalog/server grant safety, then real B/anvil and ordinary controls. [Creative notes](<research/creative-menu-implementation-notes.md>) retain the verified **8 columns ×9 rows (72)** old grid, replaced by9×5 in12w21b; exact original pixels/order remain unverified. They also record a potentially smaller native-packet conservation approach (freeze/re-sync both removal and insertion of unsupported stacks, preserve normal server-owned container movement) and a standalone client legacy screen, neither installed yet. Do not use fail-open chest availability or blindly reuse coarse crafting dates. No giants. [Research manifest](<research/snapshot-patches.json>) updated; pack/source licensing still unverified403 and no assets/source copied.

Jobs1932 API inspection,1933 build675 tests,1934 nativev1,1935 build678/nativev2,1936 final build679/diff review all collected; no running jobs. No subagents, deployment, commits or personal data changes. Earlier records are preserved below.

# Current work — 2026-10-16 / snapshot patch foundation, NOT feature acceptance

New direct request: 11w49a B-to-Creative, the single-page creative inventory for Beta1.8.1 through pre-1.3.1, version-filtered creative entries, 12w49a unrestricted single-target book anvils, and a Snapshots tab. This is a separate goal from the completed finite repair below; do not reopen that audit.

**Verified boundaries:** [11w49a](https://minecraft.wiki/w/Java_Edition_11w49a?action=raw) belongs to1.1; [12w49a](https://minecraft.wiki/w/Java_Edition_12w49a?action=raw) belongs to **1.4.6, not1.4.2**. [12w21b](https://minecraft.wiki/w/Java_Edition_12w21b?action=raw), in the1.3.1 cycle, introduces category tabs, search and the Survival inventory tab. [Research/status](<research/snapshot-patches.json>) records exact URLs, confidence, published artifact pins and limitations. The archive download failed HTTP403 on11w49a before12w49a was attempted: neither jar was cached/executed. CurseForge and PlanetMinecraft pack pages also returned403; the owner's public-domain statement remains independently unverified. No pack code/assets or proprietary Minecraft implementation/textures copied.

**Implemented foundation:** [Snapshot descriptors](<src/main/java/net/historicalexperience/version/HistoricalSnapshotPatches.java>) are explicit overlays on existing release-family profiles, NOT new terrain versions, made-up chronology slots or changed generation revisions. The coarse1.4 baseline is openly identified; this is not full1.4.6 emulation. [Metadata model](<src/main/java/net/historicalexperience/world/metadata/HistoricalMetadata.java>) adds an optional validated snapshotPatchId with existing-constructor compatibility; [strict codec](<src/main/java/net/historicalexperience/world/metadata/MetadataJson.java>) omits absent patches so previous canonical bytes and finite-origin evidence do not change. Within-profile recipe advances preserve a patch; ordinary version advances clear it; patch changes append a metadata revision. These are immutable model operations, not a substitute for transactional persistence.

[Runtime scope](<src/main/java/net/historicalexperience/server/HistoricalSnapshotRuntime.java>) caches the validated explicit patch per MinecraftServer after startup and clears it on stop. [Existing sync](<src/main/java/net/historicalexperience/server/HistoricalVersionSync.java>) still sends one string, now a selection key; [client rules](<src/main/java/net/historicalexperience/client/HistoricalClientRules.java>) retain the parent's gameplay profile and separate the patch identity. Join scope now uses the overworld, matching the integrated-client/world identity rather than accidentally losing it when a player logs in in the Nether.

[Raw B adapter](<src/main/java/net/historicalexperience/client/SnapshotCreativeKey.java>) tracks press edges even while screens/focus suppress actions; [empty request](<src/main/java/net/historicalexperience/server/SnapshotCreativeRequest.java>) cannot name another player/version/mode. The server rechecks its own scope and only lets the owner of a private, unpublished integrated world enter Creative without cheats. Dedicated/LAN/non-owner escalation is refused. This is an explicit modern multiplayer safety boundary, **not** a proven original multiplayer algorithm. The B action is one-way into Creative, not an unverified toggle-back. [Anvil adapter](<src/main/java/net/historicalexperience/mixin/AnvilMenuHistoricalEarlyMixin.java>) retains the existing early guards and wraps the pinned native Enchantment.canEnchant(ItemStack) call for12w49a enchanted books and one target. A pre-assembly guard also refuses stacked targets in Creative. Native XP costs, book consumption, combination checks and normal input handling are retained, not asserted as exact2012 algorithms. These adapters are wired/compiled but NOT yet accepted through Minecraft execution or a usable selector.

**Evidence:** offline full build passed three times (22s foundation,19s adapters,19s final registration/comment cleanup and focused diff review), remapped main/sources and licence checks pass, whitespace check passes. [Test report](<build/reports/tests/test/index.html>) has **667 passing tests**, zero failures/skips (previous653 +14). [Patch policy regressions](<src/test/java/net/historicalexperience/gameplay/HistoricalSnapshotPatchesTest.java>) cover dates/parents, immutable definitions, attachment refusal, old-menu boundaries across every registered profile, ordinary/unknown isolation, B ownership policy, single/stacked book targets. [Metadata regressions](<src/test/java/net/historicalexperience/world/metadata/SnapshotMetadataTest.java>) cover byte-identical old canonical JSON, persistence round-trip, preservation/clearing, immutable revisions, unknown/mismatched/null/whitespace/type/duplicate rejection. This is JVM/build evidence, NOT mixed-in anvil, key input, GUI or network acceptance. Existing accepted finite/gameplay hashes below do not certify this newly rebuilt work-in-progress artifact.

**Next concrete work (goal active):**
1. Add the Snapshots selector era/entries. Entries resolve to parent rules but retain their own selection key/display. For new worlds, select the existing parent world preset and bind the explicit snapshot choice to the exact new-world directory in createFreshLevel before metadata establishment; do not invent duplicate generation aliases or globally tag the next arbitrary load. Bootstrap via initialWithSnapshot; preserve choice while the vanilla preset still matches its parent.
2. Integrate Manage World/operator selection with the existing backup/journal/restart transaction. Same-parent patch changes must retain the exact current generation alias (not silently advance its recipe). Cross-parent selections use the normal destination recipe and attach the patch in the same new metadata revision. Selecting a normal release clears a patch. No live generator swaps or bare sidecar writes.
3. Implement the historical creative layout/catalog. Existing chest-loot item availability is intentionally permissive and is NOT a complete creative whitelist. Gate newly granted items/components/spawn eggs (no giant additions), preserve stored unsupported content and ordinary worlds. Do not blindly reject vanilla creative slot writes: an empty-then-fill move could otherwise delete an already owned future item. Use server-authoritative grants/owned-inventory movement, with a positive dated catalog.
4. Actual Minecraft positive/control tests for11w49a,12w49a, unpatched1.1/1.4 and ordinary; B owner/non-owner/private/LAN/other-world checks; real single/stacked anvils in Survival/Creative with costs/material/component preservation; client screenshots and Snapshots/create/manage persistence. Final full build/docs after menu integration. Do not claim the user-facing request complete until these pass.

**Integration anchors already read:** [Create World helper](<src/main/java/net/historicalexperience/client/version/CreateWorldHistoricalVersion.java>) holds a global Selection.key, not a weak per-screen map; matches() currently compares preset ID directly and must resolve a snapshot key to its parent, while preserving the selected patch key. Native createFreshLevel(String,LevelSettings,WorldOptions,Function<RegistryAccess,WorldDimensions>,Screen) was verified in the pinned mapped jar. [Catalogue](<src/main/java/net/historicalexperience/version/HistoricalVersionCatalogue.java>) derives era buttons automatically; Entry.version()/displayName()/summary() must explicitly distinguish patch identity from baseline. [GenerationUpgrade.plan](<src/main/java/net/historicalexperience/world/upgrade/GenerationUpgrade.java#L140-L200>) already allows deliberate cross-profile moves in either direction; no new chronology model is needed. Its current same-recipe no-op rejection needs a narrow patch-selection exception. Use the existing stage/journal/recovery path, not a second transaction implementation. [World inspection](<src/main/java/net/historicalexperience/server/HistoricalUpgradeService.java#L255-L284>) currently reports only the base profile; add a compatible explicit patch/selection identity rather than relabeling profileId. Client registration now calls the B adapter directly; F4 code is unchanged.

No personal Minecraft data, legal-term acceptance, deployment, commits, subagents or running/uncollected jobs. Collected jobs:1926 failed public archive download (403),1927 foundation build,1928 pinned runtime API inspection,1929 adapter build,1930 final milestone build/diff. Earlier records remain unchanged below.

# Small follow-up — 2026-10-16 / correct Indev corner label

[Corner label](<src/main/java/net/historicalexperience/client/hud/HistoricalVersionLabel.java>) now returns **Minecraft Indev** for the Indev era, rather than falling through to Minecraft Infdev. [Existing label regressions](<src/test/java/net/historicalexperience/client/hud/HistoricalVersionLabelTest.java>) now cover Indev visibility/text and retain both Infdev expectations. Offline build, full tests, licence artifacts and whitespace check pass; remapped JAR rebuilt. No terrain/gameplay changes or personal installation. Earlier runtime acceptance hashes below identify the pre-label-fix build; Minecraft runtime sweeps were not repeated for this text-only change.

# Current handoff — 2026-10-16 / bounded Indev→Infdev repair verified on all four shorelines

The owner interrupted the open-ended audit, then asked to resume. The resumed pass stayed on the original repair: **no new feature branches or side audits**. Offline build/licence artifacts pass; Gradle reused unchanged inputs for the existing **653 passing tests**, zero failures/errors/skips. Current main artifact is still **b986fbd5980d8d393c559031dc4b3c68f30ef6d58ede7b71bc1ded6b91114006**, matching all [eight previously executed early/ordinary gameplay controls](<logs/early-mob-paths-acceptance-v1.json>). [Practical handoff and operator steps](<docs/INDEV-INFDEV-REPAIR.md>) distinguish delivered core behavior from an exhaustive historical certification.

A NEW owned replica, sol-20261016-finite-final-v1, was made from verified before-images of the existing second-seed April fixture; [source fingerprint receipt](<logs/sol-20261016-finite-final-v1-old-replica.json>) confirms the source unchanged. The current remapped JAR passed [real build/container/player-sentinel preparation](<logs/sol-1211-sol-20261016-finite-final-v1-reload-prepare-build.json>), [actual /hex repair-finite-edge staging](<logs/sol-1211-sol-20261016-finite-final-v1-reload-repair.json>), locked normal stop and restart. **183** proven apron chunks released. [Independent full-shoreline acceptance](<logs/20261016-core-repair-acceptance.json>) proves all **256 island chunks**, two protected exterior block/container/biome digests, seven-diamond chest and one synthetic saved-player file unchanged. All four sides now covered: **1,008 unmodified boundary columns** (north256/south256/west256/east240), **zero high bedrock**, maximum surface difference **one block**. The deliberately protected build's16 columns retain their old content and are counted separately. All **242 generated exterior terrain chunks** outside the two explicit protected fixtures were also scanned: zero bedrock at Y8+. This improves the older160-pair evidence without weakening preservation or the wall assertion. [SE sweep](<logs/sol-20261016-finite-final-v1-perimeter-se-v2.json>) and [NW sweep](<logs/sol-20261016-finite-final-v1-perimeter-nw.json>) each sampled289 chunks with radius8, validated their own centers/tokens and loaded artifact hash. Evidence-only rework: a zero-center run labels its output -reload, not -reload-far; the first SE copy was stale and the independent validator rejected it. The corrected SE-v2 copy uses the actual successful radius8/center0 receipt; no game code or assertion was changed to conceal that error.

The previously interrupted round9 milestone is now recorded: [actual mob-death audit](<research/early-gameplay-audit.md>) reproduced charged-killer zombie/skull loot outside normal tables and an unconditional sheep port removing ordinary mutton. [Creeper capability guard](<src/main/java/net/historicalexperience/mixin/CreeperHistoricalEarlyDropsMixin.java>) now prevents the early skull award before item/budget creation; [registry-derived sheep scope](<src/main/java/net/historicalexperience/gameplay/HistoricalLootRules.java>) restores ordinary passthrough while retaining the [attributed NT helper](<src/main/java/net/historicalexperience/gameplay/HistoricalMobLootHelper.java>). **18 original numeric selector calls**, [factual dataset](<research/early-mob-drop-facts.json>), [five new regressions](<src/test/java/net/historicalexperience/gameplay/EarlyMobDropFactsTest.java>) and **768 method-level deaths** across eight runtime controls passed. A preexisting named/damaged/custom-data netherite sword on a zombie survived its charged-killer death exactly. Positive ordinary controls detect heads/discs/bows/mutton/cooked pork/XP; early counterparts remain gated. Full craft/smelt/food/leaf/four-workstation checks passed. No true player-login/client-combat/pickup or saved-mob replay claim follows from untracked diagnostic actors.

**Completion boundary:** the requested core transition repair and higher-detail gating slice for Indev AND April/June Infdev are delivered with these explicit limits. This is not blanket production readiness or a certification of every Minecraft feature. Conservative uncertain/modified chunks are retained, potentially leaving a local old wall; custom finite settings/external datapacks, genuine GUI-converted sources, process-level crash injection, real client/login and exhaustive fuel/crop/tool/physics remain unverified. Recovery/corruption tests exist, but are not labeled a process-crash campaign. Earlier history remains below. No personal saves/accounts/client installation, deployment, commits, subagents or running background jobs. Do not restart an expanding audit automatically after this bounded handoff.

# Current repair — 2026-10-15 / artifact-backed oak leaf drops in Indev AND both Infdev profiles

Goal round8 fixed a normal-survival acquisition leak. [April Infdev baseline](<logs/sol-1211-sol-infdev420-leaf-drops-v1-fresh.log>) rolled **53 apples /506 saplings /260 sticks per10,000 seeded oak-leaf rolls**. Actual hash-pinned Indev/April/June leaf callbacks instead each return only **one sapling with one-in-ten probability**, with no apple/stick/fortune/silk branch. Infdev apple registration does NOT establish a leaf source. [New reflection extractor](<scripts/EarlyLeafExtractor.java>) verifies SHA-1/size before class loading, isolates the original registry, probes leaf18 from x.c (Indev/April) or y.c (June), and exhausts ten quantity buckets ×16 metadata values ×two callbacks ×three builds = **960 factual calls**. It records each bounded RNG call and rejects untraced primitives. [Existing regeneration script](<scripts/extract-early-gameplay.py>) now supports offline --leaf-only; all previous artifact/recipe/smelt/food fields independently match the pre-edit snapshot, with only leafDrop/leafDropEvidence added to the three bundled tables. Raw strict facts: [Indev](<logs/early-leaf-in-20100223-100979c94dba4fd48085c311a79dbb97.txt>), [April](<logs/early-leaf-inf-20100420-e0eacc3a9e684d7c93a5dceb326c8b1a.txt>), [June](<logs/early-leaf-inf-20100611-c272bff4034a47239eae634f349d2677.txt>). [Corrupted-copy rejection](<logs/early-leaf-artifact-rejection-v1.json>) confirms a one-byte-changed duplicate fails identity before registry probing; original jars untouched.

[Data model](<src/main/java/net/historicalexperience/gameplay/EarlyGameplayRecipes.java>) validates immutable drop rows, unknown profile/block scope and invalid rolls. [New loot guard](<src/main/java/net/historicalexperience/mixin/LootTableHistoricalEarlyLeavesMixin.java>), registered in [mixin config](<src/main/resources/historical_experience.mixins.json>), replaces the raw consumer roll **before modern pools execute**, using the context's own ServerLevel profile. Only mapped oak leaves in the three early profiles are covered; other species/eras pass through without a fidelity claim. No modern roll then item deletion, no registry removal, no stored-block rewriting or generation revision; existing attributed block/ore hooks unchanged. No proprietary Minecraft implementation or textures copied.

**648 tests pass, zero failures/errors/skips**, including [four leaf data/negative-path tests](<src/test/java/net/historicalexperience/gameplay/EarlyLeafDropsTest.java>); build/licence artifacts pass. [Runtime verifier](<src/main/java/net/historicalexperience/server/EarlyLeafDropVerification.java>) is wired into [full gameplay verifier](<src/main/java/net/historicalexperience/server/EarlyGameplayVerification.java>). Fresh development AND Java21 remapped restart pass for [Indev](<logs/sol-1211-sol-indev-leaf-drops-v1-reload.json#L30-L39>), [April](<logs/sol-1211-sol-infdev420-leaf-drops-v2-reload.json#L30-L39>), [June](<logs/sol-1211-sol-infdev611-leaf-drops-v1-reload.json#L30-L39>) and [ordinary](<logs/sol-1211-sol-leaf-drops-ordinary-v1-reload.json>). Each early run gets **1,036 saplings only per10,000 seeded rolls**, exactly matching an independent nextInt(10) reference. Ordinary retains the original **53 apples /506 saplings /260 sticks**, no historical metadata and zero decoration suppression. Per early run: **80 exact bucket/tool/state cases** (empty hand/shears/Silk Touch/FortuneIII ×two persistent/distance states ×ten buckets), exact output and unchanged tool components, **256 public Block.getDrops calls** rejecting forbidden items, three raw-consumer explosion compatibility cases. Full previous craft/limited-craft/food/four-workstation tests remain passing. No fake player data files saved. All four production copied main artifacts match SHA-256 **14ae9960503c77ba1678af343aa72d6ba5bb13671f338ba58ae847db9282f6cd**. Fresh records: [Indev](<logs/sol-1211-sol-indev-leaf-drops-v1-fresh.json>), [April](<logs/sol-1211-sol-infdev420-leaf-drops-v2-fresh.json>), [June](<logs/sol-1211-sol-infdev611-leaf-drops-v1-fresh.json>), [ordinary](<logs/sol-1211-sol-leaf-drops-ordinary-v1-fresh.json>). Compiler-only rework: this target exposes Enchantments.FORTUNE, not obsolete BLOCK_FORTUNE; javap confirmed the API, no assertions removed/skipped.

**Important limits:** All three original base-block callers were inspected too: their quantity callback is followed by a separate float survival test, item callback and item-position RNG. This change establishes callback distribution/item identity, NOT full source-world RNG consumption, old blast rates, decay timing or pickup physics. Runtime retains modern loot random-source plumbing and public ExplosionCondition survival as an explicit compatibility shim; radius2 checks do NOT prove historical TNT/creeper behavior. Queries used no placed blocks and did not reseed world RNG; actual player breaking/entity pickup/client/network remains unproved. The exact oak source overrides oak-leaf datapack pools inside early profiles, not a general mod-compatibility claim. Other apple sources remain unknown; valid registered Infdev recipes/foods were not removed. [Artifact audit](<research/early-gameplay-audit.md>) has the detailed boundaries.

**Next:** actual early death/punch/rare equipment drops (inspect existing skeleton custom-loot suppression first), actual harvesting/pickup, exact fuel/tools/armour/crop/acquisition/spawn behavior, remaining loom/cartography/NT opening paths, client/network/audio/dimension checks. GUI-converted source, crash injection and broader finite perimeter/seed coverage remain. Terrain/transaction code unchanged, previous finite preservation evidence NOT newly retested here. Goal active/incomplete, no production readiness; no personal save/client/deployment/subagents. Pre-edit snapshot: ignored `.upstream/snapshots/20261015-early-leaf-drops/`. All background jobs collected.

# Current repair — 2026-10-15 / anvil and grindstone result paths guarded for all three early profiles

Goal round6 was read-only audit plus the owner's scope question: this is **Indev AND both April20/June11 Infdev profiles**, not Indev-only; no claim that the new audit has verified every later era. Goal round7 reproduced an independent bypass: [Indev baseline](<logs/sol-1211-sol-indev-independent-menus-v1-fresh.log>) exposed a modern anvil rename result without any RecipeManager lookup. New [narrow pure policy](<src/main/java/net/historicalexperience/gameplay/HistoricalEarlyMenuRules.java>) gates only anvil/grindstone results in those three profiles; it is not a general availability oracle or a global block ban. [Anvil guard](<src/main/java/net/historicalexperience/mixin/AnvilMenuHistoricalEarlyMixin.java>) and [grindstone guard](<src/main/java/net/historicalexperience/mixin/GrindstoneMenuHistoricalEarlyMixin.java>) bind the menu creator's level and cancel createResult **before assembly**, clearing only output/cost state, not inputs, then broadcasting changes. Separate single-target mixins keep their unrelated production method mappings explicit. [Mixin config](<src/main/resources/historical_experience.mixins.json>) registers both. Ordinary/later profiles pass through.

Availability evidence: [existing anvil timeline](<research/feature-timeline.json#L647-L658>) / [Anvil history](https://minecraft.wiki/w/Anvil#History) dates it to **1.4.2/12w41a**; [exact fetched raw Java grindstone history](https://minecraft.wiki/w/Grindstone?action=raw&section=18) dates its block to **1.14/18w44a**, functional repair/disenchant to **18w48a**. Confidence high for both being later than the offered2010 builds; not algorithm evidence for old tool/loot/fuel mechanics. New guards are independently implemented server-result enforcement, NOT a claimed direct NT port. Inspected NT InteractionListener.onRightClickBlock separately interrupts anvil opening using DISABLE_ANVIL/BlockTags.ANVIL. Its attributed opening-path port and real placed-block interaction remain pending; no existing attributed NT code was rewritten/removed. Existing enchantment/brewing mixins remain intact.

[New owned-actor verifier](<src/main/java/net/historicalexperience/server/EarlyIndependentMenuVerification.java>), wired into [player verifier](<src/main/java/net/historicalexperience/server/EarlyPlayerGameplayVerification.java>), exercises actual AnvilMenu and GrindstoneMenu. Early cases check no rename/repair preview, no pickup OR shift-click gain, zero anvil cost, unchanged XP and exact input identity/name/damage/count/component data. A named sword carries a custom-data sentinel. Ordinary control actually takes a new name while preserving damage100/custom data, consumes exactly one sword and one XP level; two damaged iron swords then become one fully repaired sword with both inputs consumed and no XP-level change. XP30 is only a field on the unsaved/unjoined diagnostic actor, not a real account grant. This does NOT positively test disenchantment/XP-orb drops, actual block-opening, packets or client UI.

**Build/licence artifacts and 644 tests pass, zero failures/errors/skips** (previous641 + [three policy tests](<src/test/java/net/historicalexperience/gameplay/HistoricalEarlyMenuRulesTest.java>)). All four cases pass fresh development AND Java21 remapped-distributable restart: [Indev](<logs/sol-1211-sol-indev-independent-menus-v2-reload.json#L45-L55>), [April](<logs/sol-1211-sol-infdev420-independent-menus-v1-reload.json#L45-L55>), [June](<logs/sol-1211-sol-infdev611-independent-menus-v1-reload.json#L45-L55>), [ordinary](<logs/sol-1211-sol-independent-menus-ordinary-v1-reload.json>). Full existing craft/limited-craft/food/stonecutter/smithing regressions still pass; ordinary metadata absent, zero decoration suppression, vanilla food unchanged. No fake player data files saved. All four production copied main artifacts match SHA-256 **7831fb65f31a7a3c20867fc7231138994ecc86e99c014dffb3563252eae8fd9e**. Fresh records: [Indev](<logs/sol-1211-sol-indev-independent-menus-v2-fresh.json>), [April](<logs/sol-1211-sol-infdev420-independent-menus-v1-fresh.json>), [June](<logs/sol-1211-sol-infdev611-independent-menus-v1-fresh.json>), [ordinary](<logs/sol-1211-sol-independent-menus-ordinary-v1-fresh.json>). [Active artifact audit](<research/early-gameplay-audit.md>) records scope/URLs/limitations.

**Next priority:** normal-survival acquisition/loot: inspect the exact legacy leaf/sapling/apple/stick drop methods and test modern block loot in all three profiles; registration of an Infdev apple is not proof it should drop from leaves. Initial own-code search found no explicit apple/leaf rule, but inspect generic block/drop resources too before calling it missing. Then actual death/punch/rare drops (existing skeleton custom-drop suppression must not be duplicated), fuel/tools/armour/crops/spawn details. Loom/cartography, NT opening-path port, positive disenchanting, client/network/audio/cross-dimension controls, GUI-converted source, crash and perimeter/seed coverage remain. Terrain/transaction code unchanged; earlier finite/player-file evidence is NOT newly retested here. Goal active/incomplete; no production readiness, no personal Minecraft save/client changes, no deployment/subagents. Pre-edit snapshot: ignored `.upstream/snapshots/20261015-early-independent-menus/`. All background jobs collected.

# Current repair — 2026-10-15 / actual workstation recipe-list bypass closed

Automatic goal round5 found a real additional gating hole: the [Indev stonecutter baseline](<logs/sol-1211-sol-indev-workstations-v1-fresh.log>) exposed **seven modern recipes** through its actual menu despite single-lookup denial. Minecraft1.21.1 bytecode confirmed StonecutterMenu and SmithingMenu result selection call **RecipeManager.getRecipesFor(type,input,level)**, not the getRecipeFor overload already gated. [Recipe-manager mixin](<src/main/java/net/historicalexperience/mixin/RecipeManagerHistoricalCraftingMixin.java>) now gates that level-aware list API: exact early crafts, six allowed smelt pairs with one output, no other machine recipe types. No-level byKey/getAllRecipesFor queries remain shared/global, and ordinary/later historical profiles are unchanged. No registry removal, generation revision or terrain/transaction change.

All three pinned legacy furnace classes **net.minecraft.a.a.b.a.b** were inspected with javap: creating/incrementing one output and consuming one input is factual, not assumed from a modern recipe. [Pure rule](<src/main/java/net/historicalexperience/gameplay/EarlyGameplayRecipes.java>) now also validates output count1; [regression](<src/test/java/net/historicalexperience/gameplay/EarlyGameplayRecipesTest.java>) rejects counts -1/0/2/64, unknown pairs and unsupported profiles. A controlled two-output previous SmeltingRecipe holder is rejected in real mixed-in runtime without ever registering it or altering global recipe data. Fuel/timers/XP and actual placed-furnace ticking remain separate work.

[Manager verifier](<src/main/java/net/historicalexperience/server/EarlyGameplayVerification.java>) now compares single/list lookups for all **64/65/68** early craft cases and all six smelts per profile; rejects the existing modern/dormant patterns and bad-smelt lists; checks four other machine list types; exercises known-ID and CachedCheck valid→invalid→valid paths; verifies raw shared registry sentinels still exist. Every positive list is cleared/re-queried to verify mutable, independent results. [Player verifier](<src/main/java/net/historicalexperience/server/EarlyPlayerGameplayVerification.java>) constructs/clicks actual stonecutter/smithing menus with imported future items. Early menus have **zero** stonecut recipes, no button selection/result change, no shift-click output, exact input identity/count preservation (all three smithing inputs) and no inventory gain. Negative flint arrows now preserve all three ingredient counts. The previous full table/inventory/limited-crafting/food cases still pass.

Development uncovered and retained two distinct issues, not hidden behind skipped assertions: [v2 button test](<logs/sol-1211-sol-indev-workstations-v2-fresh.log>) wrongly treated the button's boolean ACK as acceptance; vanilla returns true even for an invalid index, so selection/output/input state is authoritative. [v3 cleanup](<logs/sol-1211-sol-indev-workstations-v3-fresh.log#L123-L132>) exposed a real bug in my first list implementation: immutable empty lists violate StonecutterMenu's retained-list clear-on-input-change contract. **Every early list path now returns a fresh mutable ArrayList**, verified by actual cleanup and explicit positive-list clear/re-query checks, not by swallowing the exception. v1/v2/v3 failures are NOT acceptance fixtures.

**Build/licence artifacts and 641 tests pass, zero failures/errors/skips.** All four fixtures passed fresh development AND Java21 remapped-distributable restart: [Indev](<logs/sol-1211-sol-indev-workstations-v4-reload.json#L12-L48>), [April](<logs/sol-1211-sol-infdev420-workstations-v1-reload.json#L12-L48>), [June](<logs/sol-1211-sol-infdev611-workstations-v1-reload.json#L12-L48>), [ordinary](<logs/sol-1211-sol-workstations-ordinary-v1-reload.json>). Ordinary menu exposes seven stonecut choices and actually takes **two stone slabs** from one stone and **one netherite sword**, consuming exactly one of each smithing input. It retains vanilla delayed food, no historical metadata and zero decoration suppression. No fake player data files were saved in any fixture. Each production report's copied main artifact SHA-256 matches **820578b2056c2677f7f201314f7e02f661f8ebdf361255fa8fb33c38d76bc8cc**. Fresh evidence: [Indev](<logs/sol-1211-sol-indev-workstations-v4-fresh.json>), [April](<logs/sol-1211-sol-infdev420-workstations-v1-fresh.json>), [June](<logs/sol-1211-sol-infdev611-workstations-v1-fresh.json>), [ordinary](<logs/sol-1211-sol-workstations-ordinary-v1-fresh.json>). These remain **unjoined server-player method tests**, not placed-block interactions, packet/client UI, login or same-process world-switch acceptance. [Active artifact audit](<research/early-gameplay-audit.md>) records exact scope and failures.

**Next concrete work:** audit independent non-RecipeManager workstation behaviors (anvil/grindstone/loom/cartography etc.) and actual early death/punch/rare loot paths, then exact fuel/tools/armour/crops/leaves/acquisition/spawn details. Existing enchantment and brewing mixins exist: inspect their current gates instead of duplicating them. Existing skeleton custom-equipment drop suppression also exists. Networking/client/audio/cross-dimension checks, GUI-converted source, crash injection and broader perimeter/seed coverage still remain. Previous round3 finite/player-file preservation is distinct evidence, not newly retested here. Goal active/incomplete, no production-readiness claim. No personal Minecraft save/client touched; no deployment/subagents. Pre-edit snapshot: ignored `.upstream/snapshots/20261015-early-workstations/`. All background jobs collected.

# Current repair — 2026-10-15 / server-player crafting and food verified in remapped runtime

Automatic goal round 4 moved beyond recipe lookup to actual server-player methods and found/fixed two real defects. [EarlyRecipeLookup](<src/main/java/net/historicalexperience/gameplay/EarlyRecipeLookup.java>) returned unsynchronized synthetic IDs as ordinary recipes: taking a craft really added those IDs to the vanilla book ([baseline failure](<logs/sol-1211-sol-indev-player-paths-v1-fresh.log#L157>)). Per-level synthetic shaped values now report **isSpecial=true**, preventing unsynced recipe awards/book unlocks and modern limited-crafting unlock requirements without touching the global registry. Null/unsupported profiles bypass their cache. [Bootstrapped vanilla-registry tests](<src/test/java/net/historicalexperience/gameplay/EarlyRecipeLookupRuntimeTest.java>) verify every early value's special flag/result and unsupported-profile isolation; they are plain JVM/vanilla tests, not Fabric/mixin acceptance.

Historical right-click behavior was rechecked directly in the three pinned SHA-1-verified client jars: Indev/April **net.minecraft.a.b.p.a(stack,world,player)** and June **net.minecraft.a.b.r.a** decrement immediately, call player.b(int), and return; **no health gate, delay or effects call**. Stew subclass **net.minecraft.a.b.b.a** delegates and returns a new bowl. [Audit/reproduction](<research/early-gameplay-audit.md>) records exact classes/descriptors and metadata URLs, not copied implementation code. The old one-tick NT shortcut still left an artificial use state ([properly-bound baseline](<logs/sol-1211-sol-indev-player-paths-v2-fresh.log#L154>)). [Early food mixin](<src/main/java/net/historicalexperience/mixin/ItemHistoricalEarlyFoodMixin.java>) now targets **ItemStack.use before subclass dispatch**, immediately finishing allowed foods via existing vanilla/healing/effect hooks even at full health, and rejecting later foods without changing their stacks. This also reaches honey bottles' overridden use method that an Item.use-only injection misses. Only the three early profiles change; existing attributed NT hooks remain, not duplicated. Other historical profiles and ordinary worlds pass through.

[Player-path verifier](<src/main/java/net/historicalexperience/server/EarlyPlayerGameplayVerification.java>), wired through [early verification](<src/main/java/net/historicalexperience/server/EarlyGameplayVerification.java>), uses a real **unjoined ServerPlayer**, real InventoryMenu/CraftingMenu clicks and ServerPlayerGameMode.useItem. It temporarily binds the context a real login sets, restores it and the limited-crafting rule in finally, never adds/joins/saves the actor and binds no socket. An intermediate wrong-heal diagnostic was a HARNESS error, not a product finding: the unjoined actor initially lacked login's version binding ([retained error](<logs/sol-1211-sol-indev-player-paths-v1-reload.log#L140>)). That was fixed before evaluating food behavior. A compile-time attempt to use a nonexistent getKnown API was corrected after inspecting the actual RecipeBook.contains interface. No assertions were skipped to pass these failures.

**Development AND remapped-distributable restart pass for all three profiles plus ordinary control.** [Indev production](<logs/sol-1211-sol-indev-player-paths-v3-reload.json#L25-L36>): **64** table results taken; [April](<logs/sol-1211-sol-infdev420-player-paths-v1-reload.json#L25-L36>): **65**; [June](<logs/sol-1211-sol-infdev611-player-paths-v1-reload.json#L25-L36>): **68**. Every take checks exact output/count and depletion of every ingredient. Each profile also takes **8** fitting inventory-grid crafts, shift-clicks three iron-arrow batches into exactly **12**, refuses modern flint arrows in a real result slot, keeps the primary flint ingredient, records no synthetic book IDs, and crafts successfully with limited crafting enabled. Food uses at health **1 AND maximum**: **8/12/12** cases respectively; exact direct heals/clamp, one consumption, one stew bowl, immediate completion and no modern **status** effects. Five later/specialized foods (honey bottle, suspicious stew, enchanted golden apple, cookie, chorus fruit) fail with same held stack/count and unchanged health/use/effect state; Indev also refuses apple/golden apple. Development evidence: [Indev](<logs/sol-1211-sol-indev-player-paths-v3-fresh.json>), [April](<logs/sol-1211-sol-infdev420-player-paths-v1-fresh.json>), [June](<logs/sol-1211-sol-infdev611-player-paths-v1-fresh.json>).

[Ordinary production control](<logs/sol-1211-sol-early-player-ordinary-v1-reload.json>): vanilla menu crafting, delayed apple use with no instant heal, ladder output3, no historical metadata and zero suppression. No probe player data files were saved in ANY of the four fixtures. [Runner](<scripts/verify-generation-1211.py>) now records launch mode and SHA-256 of actual copied mod artifacts. All four Java21 production restarts used remapped main JAR SHA-256 **ce339af8bfdebdc183a1d6ccd5b09681e54cded091edeba48baf4be41f0ba17f**. Full build/licence artifacts pass: **640 automated tests, zero failures/errors/skips** (previous638 +2). These server methods are not socket/client UI, packet serialization, true login or same-process world-switch acceptance. Visual/audio behavior, other non-food/block use and creative-mode adaptation remain unproved. The existing PlayerHistoricalFoodMixin comment records a still-unported burp suppression path: do not call audio fidelity complete merely because status effects are suppressed.

**Next concrete priority:** inspect actual stonecutter/menu recipe-list paths and alternate RecipeManager APIs, rather than treating getRecipeFor-only workstation denials as full menu acceptance. Then actual early death/punch/rare custom drops (existing skeleton custom-drop wrap already exists; do not duplicate), exact fuel/tool/armour/crop/leaf/acquisition/spawn details, broader GUI-converted-source and crash/corner/seed tests, and real network/client controls. Recipe registration is not survival acquisition: the current audit labels 64/65/68 as mapped non-fire cases, not a claim that apples or every ingredient can be acquired. Finite preservation evidence remains the bounded round3 result below; terrain/transaction code was not changed in this round. Goal active/incomplete, no production-readiness claim. No personal saves/client touched; no deployment; no subagents. Pre-edit snapshot: ignored `.upstream/snapshots/20261015-early-player-gameplay/`. All background jobs collected.

# Current repair — 2026-10-15 / per-chunk evidence and saved exterior build verified

Automatic goal round 3 adds [saved chunk evidence](<src/main/java/net/historicalexperience/world/upgrade/FiniteChunkEvidence.java>) on top of exact apron blocks, world seed/origin/default settings and verified backup history. It decodes **all 512 used biome cells** (unused palette entries prove nothing). Pinned-provider source saves contain native Indev normal plus out-of-bounds void, including mixed boundary cells. Rewritten source biomes must match [the existing converter](<src/main/java/net/historicalexperience/world/upgrade/BiomeCarryForward.java>) AND have an actual LONG nonnegative chunk LastUpdate no newer than the verified original backup's actual LONG monotonic game time. [Time reader](<src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>) never substitutes zero for missing/wrong-type/negative time. A later conversion census is not origin proof. Unknown attachments/metadata, missing/malformed sections, actual structures/ticks/entities/POI and destination-native replicas are retained, even if that leaves part of the wall. This is conservative encoded-save evidence under a normal monotonic clock, not a cryptographic receipt against deliberately forged NBT/native-biome replicas or game-clock resets. Converted chunks saved after the staging checkpoint may conservatively be retained even if older than the actual switch.

[Region migration](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) revalidates every immutable prepared before-image before ANY writes, including older development plans: correct checksums alone cannot authorize deletion. Current entity/POI occupancy is rechecked too. [Region reader](<src/main/java/net/historicalexperience/world/upgrade/FiniteRegionSnapshot.java>) accepts an explicit original region identity for .before images. [Repair service](<src/main/java/net/historicalexperience/world/upgrade/FiniteEdgeRepair.java>) derives converted-biome evidence and the clock from the verified source backup at execution. Valid finite-prefilled protochunks must NOT require FULL status: actual saved source statuses include structure_starts/carvers/initialize_light despite complete default blocks/biomes. Empty carving masks and correctly packed vanilla postprocessing queues are supported, not treated as player attachments. [Evidence tests](<src/test/java/net/historicalexperience/world/upgrade/FiniteChunkEvidenceTest.java>), [repair tests](<src/test/java/net/historicalexperience/world/upgrade/FiniteEdgeRepairTest.java>) and [identity/time tests](<src/test/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentityTest.java>) pin native/used-vs-unused palette data, converted clock boundary, unknown metadata, malformed packed cells, proto caches, fresh destination-native replicas and unsafe old prepared plans. **638 tests pass; zero failures/errors/skips.** Build and licence artifacts pass.

**Real saved build, inventory and negative-case runtime:** new diagnostic `sol-indev-old-edge-build-v5` passes preparation → independent snapshot → actual repair command → normal locked stop → restart → independent saved comparison. [Probe-only fixture code](<src/main/java/net/historicalexperience/server/FiniteRepairFixtureVerification.java>) places nine cobblestone blocks and a real chest with **seven diamonds** in exterior chunk 8,0; changes all **512** stored biome cells in chunk 9,0 to Infdev while preserving its exact **32768-block** apron table; and writes an explicitly **synthetic saved-player NBT sentinel** containing eleven diamonds. That sentinel tests file preservation, NOT player login/inventory interaction. [Preparation evidence](<logs/sol-1211-sol-indev-old-edge-build-v5-reload-prepare-build.json>). [Actual repair log](<logs/sol-1211-sol-indev-old-edge-build-v5-reload-repair.log#L125-L127>) clears exactly **183** slots, excludes both modified chunks, withdraws **183** biome positions (**1521 → 1338**), preserves every other biome field and historical metadata bytes, and warns about the one exact-pattern chunk with unsupported origin evidence. Retained exterior entries **1105** are mostly placeholders, not 1105 player builds.

[Restart](<logs/sol-1211-sol-indev-old-edge-build-v5-reload-far.json#L346>) verifies platform/chest, replica blocks/biomes and player-file SHA-256. [Independent saved result](<logs/sol-indev-old-edge-build-v5-finite-check.json>) proves **256 island chunks unchanged**, both protected exterior block/container/biome digests unchanged, and one player data file byte-identical. All **1024** source boundary heights are present. **160 eligible shore pairs (112 east / 48 south)** have zero height difference and zero bedrock at Y8+. **16** first-exterior columns in the deliberately retained build chunk still have the wall: these are explicitly reported separately, not hidden as regenerated successes. [Offline verifier](<scripts/verify-finite-transition-1211.py>) now uses versioned canonical digests (v2 normalizes absent air; v1 evidence stays readable), independently decoded biome cells for protected chunks and player-file hashes. Only explicitly snapshotted, identity-verified protected chunks get this exemption. Full perimeter/corner acceptance remains open.

Fresh [ordinary control](<logs/sol-1211-sol-early-ordinary-control-v2-fresh.json>): no historical metadata/alias, vanilla NoiseBasedChunkGenerator height384/minY-64, modern ladder output3, zero suppressed decoration writes, no fixture effects. This is a fresh-process control, not same-process world-switch/client UI acceptance. The final diagnostic assertion initially indexed the alias key directly; Gson omits null fields. It was corrected to validate absent/null alias plus independent no-metadata and vanilla-generator assertions, and passes. No product fix was needed for that helper error.

Intermediate failures were investigated and retained, not called acceptance. Build-v3 fixture setup mistakenly wrote its new diagnostic marker to the SOURCE project fixture; the owned-target guard refused BEFORE placing blocks. That one new marker was read/content-checked, its absolute path verified, and removed; source game data was not modified. [Replica builder](<scripts/prepare-old-finite-fixture-1211.py>) now confines the marker to the NEW target, rejects symlinks and fingerprints every source world file except the held session lock; [v5 receipt](<logs/sol-indev-old-edge-build-v5-old-replica.json>) verifies source unchanged. Build-v4 preserved build/container/player data but failed the actual wall assertion because the first metadata gate rejected valid prefilled protochunks and their vanilla generation caches. Those exact saved fields were inspected, regression coverage added, and v5 rebuilt from the original verified source. No wall/island assertions were relaxed to hide it.

**Next priority is actual gameplay, not another broad seam claim.** Real inventory/table/shift-click/result consumption and recipe-book/award/sync paths; actual food use/health/count/effects/full-health behavior and specialized subclasses; actual early death drops. Read current code before assuming missing functionality: [MobHistoricalCustomLootMixin](<src/main/java/net/historicalexperience/mixin/MobHistoricalCustomLootMixin.java>) already wraps skeleton custom equipment drops (do NOT duplicate it), [PlayerHistoricalFoodMixin](<src/main/java/net/historicalexperience/mixin/PlayerHistoricalFoodMixin.java>) heals at eat HEAD but still needs real consumption/effect verification, and [CraftingScreenHistoricalRecipeBookMixin](<src/main/java/net/historicalexperience/mixin/CraftingScreenHistoricalRecipeBookMixin.java>) already delegates button hiding. Investigate real historical right-click code rather than inferring full-health/instant-use semantics from a modern NT port. Audit acquisition separately: registered item/recipe presence is not proof of survival ingredient availability (apple/golden-apple sources remain to be checked; do not change their valid registered recipes on speculation). Then alternate recipe-list/workstation APIs, tool/armour/fuel/crop/leaf/spawn weights, GUI-converted-source runtime, real process-crash paths, broader corner/seed tests and same-process/client controls. [ADR 0011](<docs/ADR-0011-finite-island-transition.md>) and [operator docs](<docs/BACKPORT-1211.md>) synchronized. Objective remains active/incomplete. Giants remain excluded everywhere. No personal saves/client touched; no deployment; no subagents. Pre-edit snapshot: ignored `.upstream/snapshots/20261015-finite-chunk-evidence/`. All background jobs collected.

# Current repair — 2026-10-15 / explicit repair for already-upgraded worlds verified

Automatic continuation round 2 implements **`/hex repair-finite-edge`**, operator permission 2, staging intent only. It is not an unsolicited cleanup and never edits live region data. Exactly one matching historical backup must verify every manifest hash/size, source metadata + level, default finite source settings, the same seed/origin, an append-only history prefix and the following explicit infinite-version action. Current stored generator references must agree with current metadata. Request pins metadata/backup-manifest hashes and revalidates at execution. Missing, corrupt, foreign, ambiguous or changed provenance refuses; pending repair and version upgrades cannot interleave. COMPLETE requests remain audit records but do not block future version upgrades.

The stop callback now reacquires the world session lock before applying repair or a pending standard upgrade; startup recovery runs under the already-acquired lock before world-data decode. Repair reuses [the exact-pristine region transaction](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>) with the source identity proven by backup, protects all other current terrain, and keeps intent pending until [old biome coordinates are pruned](<src/main/java/net/historicalexperience/world/upgrade/BiomeConversionRecord.java>). Pruning preserves every other bit and every other JSON field, forces/verifies a content-addressed original backup, replaces atomically, and retries idempotently. Corrupt records/backups fail closed; partial region/biome completion cannot be silently loaded.

[Repair service](<src/main/java/net/historicalexperience/world/upgrade/FiniteEdgeRepair.java>), [command](<src/main/java/net/historicalexperience/server/HistoricalCommand.java>), [stop lifecycle](<src/main/java/net/historicalexperience/server/HistoricalServerLifecycle.java>), [pre-decode hook](<src/main/java/net/historicalexperience/mixin/LevelStorageHistoricalUpgradeRecoveryMixin.java>), [upgrade interlock](<src/main/java/net/historicalexperience/world/upgrade/GenerationUpgrade.java>), [repair regressions](<src/test/java/net/historicalexperience/world/upgrade/FiniteEdgeRepairTest.java>) and [biome pruning regressions](<src/test/java/net/historicalexperience/world/upgrade/BiomeRecordRepairTest.java>). Tests include intent-only staging, edited exterior blocks, unchanged metadata/level, missing/corrupt/foreign/ambiguous provenance, stage-vs-execute revalidation, concurrent-upgrade refusal, malformed intent, crash retry between region and biome commits, negative coordinates, unknown JSON fields and corrupt original-record backup refusal. All **623 automated tests** pass; build/licence artifacts and CRLF-aware diff check pass. Final preflight also verifies that the current infinite profile owns the stored recipe; metadata cannot disguise a still-finite recipe as an infinite world.

**Bounded runtime proof:** [the diagnostic builder](<scripts/prepare-old-finite-fixture-1211.py>) reconstructed an old-upgrade layout ONLY in a new project fixture `sol-indev-old-edge-repair-v2`, from the v4 fixture's hash-verified immutable pre-migration region images. It never changed the source and is NOT a production repair tool. Dedicated-server source had no optional GUI biome record, so the replica explicitly adds a diagnostic old-census record; this is disclosed in [replica provenance](<logs/sol-indev-old-edge-repair-v2-old-replica.json>). The [real command probe](<logs/sol-1211-sol-indev-old-edge-repair-v2-reload-repair.json>) returns 1, leaves pending=true and plan-before=false/plan-after=false while live. [Normal shutdown](<logs/sol-1211-sol-indev-old-edge-repair-v2-reload-repair.log#L126>) completes under the lock: **185** pristine chunks released, **185** biome positions withdrawn (**1521 → 1336**); all other biome fields unchanged, historical metadata byte-identical to source. After [restart](<logs/sol-1211-sol-indev-old-edge-repair-v2-reload-far.json>), [saved comparison](<logs/sol-indev-old-edge-repair-v2-finite-check.json>) proves **256 island chunks unchanged**, **1024 edge heights**, **96 sampled shore pairs**, **zero height difference**, **zero bedrock at Y8+**. Exterior retained count 1080 is mostly protochunk placeholders, not 1080 player edits.

Diagnostic setup failures were investigated, not counted as acceptance: v1 builder wrongly assumed a biome record existed; v2 snapshot originally required an already-existing finite plan, which an old world does not have. Builder now explicitly labels its synthetic census. [Offline verification](<scripts/verify-finite-transition-1211.py>) now independently proves backup hashes, exact seed, default finite references and a matching history prefix without requiring a plan. The snapshot then ran before any server repair attempt. Island equality/wall/seam assertions were not relaxed. Earlier incomplete fixture v1 is not runtime acceptance.

**Next / safety review before deployment:** the already-upgraded path currently proves WORLD source identity, then classifies exact 32,768-block apron patterns. It does not independently prove each candidate chunk existed before the original version switch. A player-created exact apron replica in a newly generated Infdev chunk could be indistinguishable by blocks alone. Investigate native saved Indev-vs-Infdev biome palettes and any trustworthy original conversion census/time evidence; preserve/refuse uncertain candidates, rather than treating a world backup as per-chunk provenance. A contrived exact-pattern build in a destination-native biome is a useful negative regression. GUI conversions may have rewritten source biomes, and a later/overwritten census must not be blindly assumed to be the original finite census. This gap is NOT a reason to claim the objective complete or deploy. Then verify real edited-exterior build/container/player-data preservation through this path and restart; then actual player inventory/table/shift-click/food/death-drop tests and alternate recipe-list/workstation APIs. For the edited fixture, the offline coast verifier currently checks every loaded first-exterior column: explicitly distinguish released/newly generated chunks from intentionally preserved modified exterior chunks, assert the latter's complete decoded state/container identity separately, and report retained-wall exceptions rather than calling them an algorithm failure or silently omitting them. A real saved cobblestone/chest inventory in one apron chunk can exercise that boundary. Source snapshots/digests should remain independent and versioned if normalizing absent air sections. Recipe manager lookup tests are NOT UI acceptance. Inspect actual implementation/bytecode before assuming the remaining food/equipment mechanics work. Full seam/corner coverage, external datapack override limitations and process-crash injection remain open. See [ADR 0011](<docs/ADR-0011-finite-island-transition.md>) and [operator flow](<docs/BACKPORT-1211.md>). Goal remains active/incomplete. Giants still excluded across all profiles. No personal save/client modification, no deployment, no subagents. Pre-edit snapshot for this round is ignored `.upstream/snapshots/20261015-finite-edge-repair/`. No background jobs remain uncollected.

# Current repair — 2026-10-15 / finite plans bound to world identity; second seed/destination verified

Automatic continuation round 1 made the migration safer, not broader in reach. Current finite plan **schema 3** binds the exact signed 64-bit saved seed, a SHA-256 of the immutable initial historical revision, and the recognized source preset. All three source provider-settings objects must contain only the preset reference; inline overrides refuse before staging backups or touching regions. Read, recovery and generator attachment revalidate identity. A legitimate clone/rename or later version advance remains valid; a copied plan for a different seed or different same-seed world origin is rejected. Missing seed is NOT treated as seed zero. Schemas 1/2 were development fixtures only and now fail closed; no personal world received them. Arbitrary external datapack replacements of named presets are not authenticated by this offline check.

Implementation: [identity binding](<src/main/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentity.java>), [source-settings/seed readers](<src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>), [migration/read/recovery checks](<src/main/java/net/historicalexperience/world/upgrade/FiniteApronRelease.java>), [runtime seed check](<src/main/java/net/historicalexperience/generation/FiniteTransitionRuntime.java>), [identity regressions](<src/test/java/net/historicalexperience/world/upgrade/FiniteTransitionIdentityTest.java>) and [no-write refusal/64-bit round-trip tests](<src/test/java/net/historicalexperience/world/upgrade/FiniteApronReleaseTest.java>). Snapshot before this round's source edits: ignored `.upstream/snapshots/20261015-finite-identity/`.

**Verification:** offline build and licence artifacts pass, **609 tests / zero failures, errors or skips**; CRLF-aware diff check passes. Fresh fixture `sol-indev-edge-v4-identity` uses seed **-9223372036854775791**, upgrades through the operator command to April Infdev, stops normally and reloads with the bound plan. All **256 island chunks** retain decoded blocks/block entities; **185** exact apron chunks released; **1024** source boundary heights captured. **128** north/west saved shoreline pairs have **zero height difference** and **zero bedrock at Y8+**. This complements the previous southeast/June fixture, not complete corner/perimeter acceptance. Exact seed is retained in JSON and checked against the live level after restart. [Source snapshot](<logs/sol-indev-edge-v4-identity-finite-source.json>), [coast result](<logs/sol-indev-edge-v4-identity-finite-check.json>), [April runtime](<logs/sol-1211-sol-indev-edge-v4-identity-reload-far.json>), [ADR](<docs/ADR-0011-finite-island-transition.md>).

**Next concrete task, still not implemented:** explicit operator repair for worlds already upgraded by the old build. Proposed contract: `/hex repair-finite-edge` stages intent only; process after closed-world drain or before world decode under the storage lock, never while chunks are live. Establish source default geometry from an existing **verified** historical backup's metadata + level, prove seed/origin and append-only history-prefix match the current world, and refuse absent/ambiguous/corrupt provenance. Reuse the exact-pristine-only region transaction with the verified source identity; preserve every other current terrain chunk. Retain the request until the region transaction AND biome position bookkeeping finish, then complete. Released chunk coordinates may still be in the old biome conversion record and must be withdrawn safely/idempotently; do not assume chunk header deletion alone updates that record. No unsolicited repair on ordinary load and no personal-save edits. Exercise this path on copies of project fixtures only.

The active objective remains incomplete. Real-player inventory/shift-click/consumption/death drops, alternate recipe-list/workstation paths, broader feature/fuel/crop/leaf/spawn mechanics, edited exterior runtime preservation and full seam/corner coverage remain owed. Giants remain excluded in all profile spawn lists. No deployment to the owner's client.

# Current repair — 2026-10-15 / exact early-build gameplay, no giants, finite transition under acceptance

The owner reported the saved bedrock/dirt apron when upgrading Indev to Infdev, poor gating, and then explicitly said **do not add giants to the spawn list**. No subagents were used. No personal save or running client was modified.

**Giants:** removed from every declared natural-spawn allowance, not merely Indev/Infdev. Registry presence is not natural-spawn evidence. A new all-profile regression asserts their absence. Indev and the two offered Infdev builds now allow six species. Earlier entries describing seven and calling the prior gating complete are superseded, not silently rewritten.

**Artifact corrections:** independently re-fetched metadata from `meta.celestia.sh` verifies SHA-1 and byte length for `in-20100223`, `inf-20100420`, `inf-20100611`. The Indev hash gap is resolved. An isolated reflection tool now extracts exact crafting patterns, output counts, six smelts, and food instances/fields into committed data. Totals: 68/69/72 recipe registrations (four dormant fire-chainmail recipes in each), four Indev foods and six Infdev foods. The old zero-food audit was false: executable food registrations and the heal caller prove it. The earlier Applet substring false positive did not validate the independent zero-caller assertion.

Implementation: build-local shaped recipe lookup before vanilla lookup; server profile from the actual level; closed early smelting/workstation lookup; exact food values; ordinary imported modern foods rejected without deleting stacks; early single-item mob tables without modern bones/Looting/on-fire cooking; light-gray punch-shearing; no early sheep grazing/regrowth; separate creeper disc/head custom-drop gate. Painting ingredients, gold-block golden apple, iron arrow tip, nine-string cloth, cloth armour, three-output steps and June-only one-output ladders/signs/doors are no longer approximated by modern recipes. See [corrected audit](<research/early-gameplay-audit.md>), [extractor](<scripts/extract-early-gameplay.py>), [pure tests](<src/test/java/net/historicalexperience/gameplay/EarlyGameplayRecipesTest.java>) and [runtime assertions](<src/main/java/net/historicalexperience/server/EarlyGameplayVerification.java>).

**Finite edge:** pinned Moderner Beta generates exterior chunks with bedrock Y0–53, dirt54, water55–63, air above. Releasing the world border alone retained that apron. The new explicit upgrade migration releases only whole exterior chunks equal to that exact pattern, with block/entity/POI protections; immutable SHA-256 before/after backups and PREPARED/COMPLETE recovery precede header-only region changes. It never regenerates the island. Sampled boundary heights feed a 64-block collar through Moderner Beta's density modifier, before block placement, attached to the specific generator. Unknown edges are not fabricated. See [ADR 0011](<docs/ADR-0011-finite-island-transition.md>).

**Important runtime-discovered defects:** empty zero-byte vanilla entity-region handles initially refused recovery; fixed and tested. Biomes-only placeholders initially looked like height-zero edges; fixed by requiring terrain-stage status. Intermediate fixture v2 then caught **178 real ore/dirt/gravel writes spilling into island chunk (7,7)** after regeneration. This was not waved away: plan schema 2 now persists source-terrain chunks and rejects future generation writes through both `WorldGenRegion.setBlock` and `ensureCanWrite` (vanilla OreFeature checks the latter before writing sections directly). Fixtures v1/v2 are diagnostic failures, not seam acceptance. Fresh v3 now passes the bounded preservation/seam check: 256/256 island chunks unchanged in decoded block states and block entities; 185 pristine apron chunks released with verified backups; 1024/1024 source edge heights captured; 96 saved exterior shoreline pairs show zero height difference and zero bedrock blocks at Y8 or above. The 1080 retained exterior entries are mostly empty generation placeholders, not 1080 player edits. Broader seeds/corners remain owed.

**Verification at this milestone:** repeated offline `build` passed, including licence artifacts and **599 tests, zero failures/errors/skips**. `git -c core.whitespace=cr-at-eol diff --check` passes; plain Git whitespace checking interprets the repository's existing CRLF as trailing whitespace, so line endings were not normalized wholesale. Actual mixed-in recipe-manager checks passed April's 65 crafts and June's 68 crafts (plus six smelts, six denied modern smelts, cached previous-holder rechecks and four workstation-type denials); fresh Indev runtime also passed all 64 crafts. These are manager-level runtime checks, not live player inventory/table/shift-click or health/drop end-to-end acceptance. The source-island snapshot/check tool compares decoded block states and containers rather than relying on unchanged palettes or a launch. Retained bounded acceptance: [v3 saved-coast comparison](<logs/sol-indev-edge-v3-finite-check.json>), [v3 source inventory](<logs/sol-indev-edge-v3-finite-source.json>), [Indev runtime](<logs/sol-1211-sol-indev-edge-v3-fresh-upgrade.json>), [June runtime](<logs/sol-1211-sol-indev-edge-v3-reload-far.json>) and [ordinary control](<logs/sol-1211-sol-early-ordinary-control-v1-fresh.json>). The fresh ordinary control kept the vanilla generator, no historical alias, no suppressed placements, and modern three-output ladders. This is a fresh-server negative control, not a same-process world-switch test.

**Still owed, not declared complete:** explicit safe repair for already-upgraded worlds; plan seed/custom-finite-setting identity validation; multiple-seed/full-corner seam evidence and edited-exterior runtime fixtures; real player crafting/consumption/drops/limited-crafting; alternate recipe-list/workstation paths; broader block/item/tool/fuel/crop/leaf/spawn-mechanics audit and ordinary-world negative controls. The new source and jar have NOT been deployed to the owner's client. No personal save cleanup has been attempted. Do not mark the active repair goal complete on the strength of these unit tests.

# Current change — 2026-10-15 / the owner selected Indev and got a vanilla world: my tag edit broke the picker

Reported directly: "I created and launched a world with indev selected and it was just default." It was, and the cause
was mine.

**Diagnosis, from artifacts rather than guesses.** The world the owner created (`New World (4)`) records **vanilla
generation** in its `level.dat` - no `historical_experience`, no `moderner_beta:preset`, no project dimension type -
and the session log shows this project's own decision line for it: `Historical marker decision for ...New World (4)..:
action=LEAVE_ORDINARY`. Four worlds the owner made earlier the same day (`New World` through `(3)`) all carried project
presets (`beta_1_9_pre_1_no_modern_content`, `beta_1_8_1_no_modern_content`, `release_1_3_no_modern_content`,
`beta_1_0_no_sandstone`), so the picker itself works and only the Indev path failed.

**The cause.** The version picker (`CreateWorldHistoricalFooterMixin` -> `HistoricalVersionScreen`) lists versions from
the catalogue, whose `key` is the **profile id** (`historical_experience:indev_latest`), and
`CreateWorldHistoricalVersion.matches` requires a preset in the `minecraft:normal` tag whose location **equals that key
exactly**. When I added the ore revision I edited the tag by *replacing* the three pre-Beta profile ids with
`_with_ores` aliases. That kept the entry count at 34, so the count assertion stayed green and the alias still named a
revision of a declared version, so the menu test stayed green too - but the keys the picker stores no longer existed in
the tag, `matches` found nothing, and the world type silently stayed at Default. The tests could not see it because
neither of them asserts that a tag entry *is* the profile id.

**The fix, in two halves, following the convention the tests do state** ("the normal world-preset tag must list every
declared profile exactly once"):
1. The tag lists **profile ids only** again - `indev_latest`, `infdev_420`, `infdev_611` - back to 34 entries, and the
   `_with_ores` aliases are gone from it.
2. By the project's convention the **base** world preset references the **newest settings revision**, which is what the
   picker's own description line reports. `indev_latest.json`, `infdev_420.json` and `infdev_611.json` now reference
   `..._with_ores` in all three places each, so a world created from the menu gets the era ores as well.

Verified wiring afterwards: picker key `historical_experience:indev_latest` -> present in the menu tag -> settings
preset `historical_experience:indev_latest_with_ores` -> which wraps Moderner Beta's `moderner_beta:indev`. Same shape
for both Infdev profiles. 588 tests, 0 failures, including both menu tests.

**Owed**: a re-deploy - the owner's client was running (PID 30932) when this was written - and then a fresh world from
the picker to confirm end to end. The lesson worth keeping: an invariant that a test enforces as a COUNT can still be
broken by substituting one kind of entry for another, and "the count stayed 34" is not the same statement as "the
picker can still find its key".

# Current change — 2026-10-15 / Indev closed out: registry, gating, upgrade, ores all evidenced

Closing summary of the Indev work, so the next session starts from facts rather than narration.

**Registered.** `historical_experience:indev_latest` - "Indev 20100223", era `indev`, chronology `ERA_INDEV + 223`,
upstream preset `moderner_beta:indev` (finite 256x256x128, `finite_2d`, theme normal, border 256 centred on origin),
with this project's overrides: deepslate off, cave biomes off, and the preset's stronghold placement override removed.
Four revision aliases ship: base, `_no_grass`, `_no_modern_content`, and the new `_with_ores`. It is the only finite
profile: `HistoricalVersions.FINITE_PROFILES` names it alone.

**Gated.** Rules start from Infdev's and adjust to Indev's record: seven mob species (creeper, skeleton, spider,
giant, zombie, pig, sheep), Indev 20100219 drops (feathers, arrows, porkchops, string), the direct-heal food model
(apple 4, bread 5, stew 10, raw porkchop 3, cooked 8, brown mushroom 5, with the red mushroom's -3 recorded as
unmodelled rather than applied), no hunger, sprinting, experience or structures. Per-profile boundary tests pass:
every gate agrees with the boundary it was declared for.

**Upgraded.** An Indev world upgrades to Infdev through the biome upgrader with the border released: the log records
"world border size 256.0 (vanilla default 2.9999984E7)", the profile advances to `historical_experience:infdev_420`
revision 2, and `FINITEPROBE` reads 256.0 before and 2.9999984E7 after. The upgraded world's census is
byte-identical in block content to its source, which is the chunk-preservation property the architecture requires.

**Ores.** A census found the pre-Beta family generating **no ore at all** - Indev 19 distinct blocks over 673 chunks,
and an infinite Infdev world 10 distinct over 928,038 stone. Revision 3 (`_with_ores`) fixes it for Indev 20100223 and
both Infdev builds, and all three are now **verified by census**: coal/iron/diamond/gold present (3498/2772/254/230,
6519/3312/278/248, 3862/2905/254/223) and redstone, lapis, copper and emerald absent in every one.

**Block registry.** Ids 1-62 read from `in-20100223.jar` with two reconciled readers, cross-checked against the era
texture layout on thirteen id/texture pairs. The Beta 1.7.3 table names ids 21-34 with blocks that postdate the
build; the atlas settles it instead - ids 21-36 are the sixteen cloth colours, texture 64-79, and `terrain.png` read
tile by tile gives them as a hue ramp from red (221,49,49) to white (221,221,221), with **no black** in the set.

**Open, and only two things.** (1) The re-deploy: the live jar is `BFD480D688E0D88C` while the build is now
`F1CF19B01F225540`, so the owner must close Minecraft before `scripts/deploy-to-prism.ps1` can replace it - the client
was still running (PID 36180) when this was written. (2) The Indev jar's SHA-1 is self-computed because the Omniarchive
Vault publishes no checksums, so it does not yet meet the project rule of comparison against an independently published
metadata document; the owner was asked where such a document lives and that answer is still outstanding.

# Current change — 2026-10-15 / the cloth colours read straight from the jar, and one inherited assumption corrected

No wiki citation turned out to be necessary. `terrain.png` is in `in-20100223.jar`, PIL was available, so the atlas was
read tile by tile and averaged - the artifact is the primary source.

**Indev ids 21 to 36 are the sixteen cloth colours, and their order is now known:** red (221,49,49), orange
(221,135,49), yellow (221,221,49), yellow-green (135,221,49), green (49,221,49), spring-green (49,221,135), cyan
(49,221,221), light blue (104,162,221), blue (119,119,221), blue-violet (135,49,221), violet (174,73,221), magenta
(221,49,221), pink (221,49,135), dark grey (76,76,76), grey (142,142,142), white (221,221,221). RBG triples are
recorded in `research/indev-20100223-blocks.json` so the reading can be checked without repeating it.

**And it corrects something I was about to inherit.** Texture 64 is RED and there is **no black** anywhere in the
range - the darkest tile is (76,76,76). The note added last round called the project's recorded Beta 1.7.3 wool order
"probable" for Indev; that is now superseded by measurement. Both records can be right about their own version: the
Beta order is a metadata order for sixteen dye colours, and Indev's sixteen are a hue ramp with no black. They must not
be conflated, and the file now says so rather than leaving the earlier guess standing.

**State**: 588 tests, 0 failures; five snapshots. Open items are down to one code-side item and one operator item: the
re-deploy once the owner's client closes, and nothing else on the research side of Indev.

# Current change — 2026-10-15 / the Indev mid-range is resolved, and the extraction script is repaired

**Ids 21 to 36 in Indev 20100223 are the sixteen cloth colours.** The parse reads a texture index for every one of
them, and they run 64, 65, 66 … 79 - sixteen ids against sixteen consecutive textures, which is the cloth range of the
era terrain atlas. The Beta 1.7.3 table names lapis ore, a lapis block, a dispenser, sandstone, a music block, a bed,
two rails, two pistons, cobweb, tall grass and a dead bush across that range: every one of those arrives after this
build, so the table is evidence about Beta there and must not be read as Indev's registry. Where the two ranges can be
checked against each other they agree - the Beta table calls ids 37 and 38 flower and rose, and the same parse gives
them textures 13 and 12, the plant textures.

Confidence, stated in three parts, in `research/indev-20100223-blocks.json` under `midRangeResolved`: **definite** that
21-36 are the sixteen cloth colours by texture index; **probable** that they follow the legacy metadata order the
project already records for wool (black, red, green, brown, blue, purple, cyan, silver, gray, pink, lime, yellow, light
blue, magenta, orange, white); **unverified** the colour of each individual id, which needs one citation and is not
needed for gating - what matters is that these ids are cloth and therefore not lapis ore, a dispenser, sandstone, a
music block, a bed, rails, pistons, cobweb, tall grass or a dead bush. Nothing in this project gated on the old names,
but anything that later read the table for a pre-Beta profile would have judged lapis ore available in Indev and cloth
unavailable - wrong in both directions.

**And the extraction script had a real bug, now fixed.** It appended two literal characters - a backslash and an `n` -
after its JSON instead of a newline, because an earlier repair of mine over-escaped the write. That is exactly what
produced "Extra data: line 453 column 2" from every run, including the run that emptied the artifact two rounds ago.
`scripts/extract-indev-blocks.py` now writes valid JSON, and its output was verified by loading it back: 63 blocks.
That also means the earlier failures had two independent causes, not one - the semicolon bug in the descriptor
comparison, and this write.

**State**: 588 tests, 0 failures; snapshots at four points. The open list is now two items: one citation for the
individual cloth colours, and a re-deploy once the owner's client closes.

# Current change — 2026-10-15 / all three pre-Beta ore profiles verified, and a flag of mine retracted

**The second Infdev build is censused**, which closes the caveat left last round rather than leaving it implied:

| world | preset | chunks | coal | iron | diamond | gold | later ores |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `run/indevores` | `indev_latest_with_ores` | 49 | 3,498 | 2,772 | 254 | 230 | none |
| `run/infdevores` | `infdev_420_with_ores` | 49 | 6,519 | 3,312 | 278 | 248 | none |
| `run/infdev611ores` | `infdev_611_with_ores` | 49 | 3,862 | 2,905 | 254 | 223 | none |

The acceptance criterion - four era ores present, four later ones absent - is therefore met **by measurement for all
three pre-Beta profiles**, not by construction for two of them.

**And the Infdev apple/mushroom discrepancy is retracted; the project's claim stands.** Two rounds ago I flagged the
recorded statement "there are no constant-pool hits for any food item" because a scan found `apple` and `mushroom` in
all four Infdev jars. Locating the hits settled it in the opposite direction: `apple` occurs only inside
`java/applet/Applet` in `net/minecraft/client/MinecraftApplet.class`, and `mushroom` only inside a splash text - "Try
the mushroom stew!" - in `net/minecraft/client/q.class`. Neither is a food registration.

The flag was my own scanning error: a naive substring search over class bytes matches ordinary English inside
unrelated strings, and `Applet` contains `apple`. The lesson is recorded in
`research/infdev-jar-verification.json` beside the retraction - needles must be chosen so they cannot occur inside
another identifier, or a window must be printed around every hit, which is what found this.

**State**: 588 tests, 0 failures; a fourth snapshot taken. Open items are unchanged and now shorter: the era id table
for Indev's block ids 21-34, the owner's independent hash document for the Indev jar, and a re-deploy once their
client closes.

# Current change — 2026-10-15 / the pre-Beta ore gap is closed and verified by census

The fix worked on the first try, because the edit was made the careful way: anchor counts asserted before writing, a
span chosen that only the terrain helper contains, and a paren-tolerant tail - the exact guard whose absence broke two
files three rounds earlier.

**What changed.** `HistoricalVersions.terrainOnlyWithRules` now wraps its three shared revisions in
`withPreBetaOreRevision(id, upstreamPreset, chronology, List.of(…))`, which appends **revision 3** (`_with_ores`) only
when `chronology < ERA_ALPHA`. That condition lives in the new helper precisely because the first attempt added the
revision unconditionally and handed every Alpha profile an alias with no preset file - caught by
`Alpha12BiomeRuleTest`. The revision carries `HistoricalFeatureExclusions.preAlphaWithVanillaOres()`:
`keepingVanillaOres(INFDEV_WITHOUT_DUNGEONS)` plus explicit exclusions for redstone, lapis, copper, emerald and the
three modern diamond shapes. Six preset files and the three create-world menu entries follow the alias.

**Verified by census, on newly generated worlds.**

| world | preset | chunks | coal | iron | diamond | gold | later ores |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `run/indevores` | `indev_latest_with_ores` | 49 | 3,498 | 2,772 | 254 | 230 | **none** |
| `run/infdevores` | `infdev_420_with_ores` | 49 | 6,519 | 3,312 | 278 | 248 | **none** |

Before this, the same Indev world had **no ore of any kind** (19 distinct blocks across 673 chunks) and an infinite
Infdev world had none either (10 distinct blocks, 928,038 stone). The acceptance criterion set two rounds ago - four
era ores present, four later ones absent - is therefore **met for Indev and for Infdev 20100420**. The 20100611 build
shares the revision list and the chronology band, so it follows by construction; it has not been censused itself, and
that is stated rather than implied.

**One test count moved** and was updated with its reason: `GenerationBackport1211Test` counts presets carrying the
reduced envelope without overriding ordinary noise registries, 99 → **101** (it moved by two, not by the three files
added, which is what that filter sees).

**State**: 588 tests, 0 failures; snapshots at three points in `.upstream/snapshots/`, all gitignored; nothing gated on
research artifacts. Still open: the era id table for Indev's block ids 21-34 (the Beta table's names there postdate the
build), the Infdev apple/mushroom constant-pool discrepancy, the owner's independent hash document for the Indev jar,
and a re-deploy - the live jar predates this session's last deploy and the owner's client was running.

# Current change — 2026-10-15 / ore revision: anchors measured, script refused to write, tree still green

The safe procedure recorded last round was followed, and it did its job: the edit script **refused to write** rather
than splice, because its own precondition failed.

**What was measured, with newline-tolerant regexes** (the first counts came back zero for a different reason - this
file mixes CRLF from its history with LF from edits written through Python, so any multi-line anchor must accept both):

| anchor | occurrences | verdict |
| --- | --- | --- |
| `return new HistoricalVersion(` … `rules,` … `List.of(` | **2** | shared by the terrain and Release helpers, as feared |
| `private static HistoricalVersion releaseTerrainOnlyWithRules(` | 1 | unique |
| `// Revision 1 removes grass, which did not exist before Beta 1.6` | 1 | unique |
| `// Revision 2 adds the desert well, and the monster room only where the version predates it` | 1 | unique |
| `revisionTwoExclusions,` newline `List.of()));` | 0 | the tail text is not what was assumed - the regex must allow any run of `)` |

**The next edit, precisely.** Span from the *unique* revision-1 comment backwards to the `List.of(` that opens the
terrain helper's revision list, wrap that opening in
`withPreBetaOreRevision(id, upstreamPreset, chronology, List.of(` - the helper is already in the file and compiles -
and take the tail with `revisionTwoExclusions,\s*\r?\n\s*List\.of\(\)+\);`, adding one `)` for the new call. Assert
exactly one match for each span before writing, and compile immediately after; that is the check whose absence broke
two files three rounds ago.

**Where the ore work stands.** `HistoricalFeatureExclusions.preAlphaWithVanillaOres()` exists and compiles:
`keepingVanillaOres(INFDEV_WITHOUT_DUNGEONS)` plus explicit exclusions for redstone, lapis, copper, emerald and the
three modern diamond shapes, leaving coal, iron, gold, the plain diamond vein, dirt and gravel. It is not yet
referenced by a revision, and no preset or tag entry has been added, so **nothing about ore generation has changed in
game** - the censuses that found no ore at all in Indev and in an infinite Infdev world still describe the current
binary. The measured facts behind it are in `research/indev-20100223-blocks.json` under `oreIsolationExperiment`.

**State**: 588 tests, 0 failures; a fresh snapshot covers the helper insertion
(`.upstream/snapshots/` now holds two, both gitignored). The live jar still predates this session's last deploy.

# Current change — 2026-10-15 / snapshot taken, ore helper reinstated, and the exact next step pinned down

**A snapshot now exists**, because the previous round showed what its absence costs: `.upstream/snapshots/2026-10-15-1721/`
holds `src/main` and `src/test` (685 files, gitignored). HEAD is `dc625cd Fixup Experience levels` with 35 uncommitted
entries, which is exactly why a `git checkout` of two files silently discarded a session of work - the snapshot is the
guard against repeating that, and it is taken before any further multi-file change.

**The ore helper is back in and compiles**: `HistoricalFeatureExclusions.preAlphaWithVanillaOres()`, a method rather
than a field so it can reference `INFDEV_WITHOUT_DUNGEONS` without an illegal forward reference. It is
`keepingVanillaOres(INFDEV_WITHOUT_DUNGEONS)` plus explicit exclusions for redstone, lapis, copper, emerald and the
three modern diamond shapes, leaving coal, iron, gold, the plain diamond vein, dirt and gravel. It is not yet used by
any revision, which is the remaining step.

**The remaining step, and the trap that must not be repeated.** `terrainOnlyWithRules` ends its revision list with

```
                    revisionTwoExclusions,
                    List.of())));
```

and revision 3 has to be added *conditionally* (`chronology < ERA_ALPHA`) because that helper is shared with the Alpha
declare­ments - adding it unconditionally handed every Alpha profile an alias whose preset file does not exist, which
`Alpha12BiomeRuleTest` caught. The safe shape is a small helper that takes the three shared revisions and appends the
ore one when the chronology is below `ERA_ALPHA`, called by wrapping the list in the terrain helper:

```
            withPreBetaOreRevision(id, upstreamPreset, chronology, List.of(
                new GenerationRevision(0, ...),
                ... )));
```

**The trap**: `releaseTerrainOnlyWithRules` (the Release helper) opens its own list with byte-identical text -
`return new HistoricalVersion(` then `id, displayName, era, chronology, rules,` then `List.of(` - so an anchor on that
text matches BOTH helpers, and the earlier attempt to change "the return" damaged both. Before replacing anything, the
next round must count the occurrences of every anchor it plans to use and pick one that is unique to the terrain
helper - the revision-2 comment block ("// Revision 2 adds the desert well...") is the obvious candidate, since it
appears only there. Then: compile after that single edit, add the third preset pair and the tag entry, build, and
re-census an Indev world and an Infdev world for the four era ores present and the four later ones absent.

**State**: 588 tests, 0 failures; the Indev profile, its presets, the finite helpers, the biome carry-forward, the
border release and the upgrade evidence are all intact. Live jar `BFD480D688E0D88C` differs from the current build
`FBFA1E4AA0FF8E6F` only because the build was re-run; the source is equivalent, and a re-deploy is owed once the
owner closes Minecraft (their client was running: Prism PID 36180).

# Current change — 2026-10-15 / Indev and Infdev generate no ores at all - measured, and isolated

The census the previous entry called for, and the experiment that isolates it.

**Indev.** `scripts/region-block-census.py` over the world this project generated from the Indev preset: 673 chunks,
22,052,864 blocks, **19 distinct**, and no ore of any kind - stone, bedrock, water, dirt, lava, grass_block,
oak_leaves, sand, oak_log, gravel, dandelion, poppy, both mushrooms (the planks, torches, obsidian and portal blocks
are the enchanting probe's own output; its log reads PORTALPROBE obsidian=14 portal=6, matching those counts exactly).

**The hypothesis was wrong in an instructive direction.** I expected an anachronistic *redstone* ore, since redstone is
Alpha v1.0.1 content. The measurement says a **missing** population instead: not only redstone and lapis, which belong
to later builds, but also coal, iron, gold and diamond, which Indev 20100223 certainly had.

**Isolated to the pre-Beta family, not the finite provider.** An INFINITE Infdev 20100420 world, generated from the
project preset and censused the same way, contains **10 distinct blocks** - stone 928,038, air, bedrock, dirt, water,
sand, grass_block, oak_leaves, oak_log, gravel - and again **no ore at all**. In 928,038 stone blocks a Beta-era
population would place roughly nine thousand coal ore. So the finite level is not the cause: Indev and both Infdev
builds share the pre-Alpha revision list, that list excludes the vanilla ore features, and nothing replaces them for
`chronology < ERA_ALPHA`.

**The project already owns the right tool.** `HistoricalFeatureExclusions.keepingVanillaOres(...)` is documented as
"the same restrictions without the replacement bans, **for versions that place no ores of their own**" - and it is
applied to the later profiles that have no researched ore population, but not to the pre-Beta ones. That asymmetry is
the whole gap.

**The shape of the fix is a NEW revision, not an edit.** Revisions are append-only and worlds already exist on the
current pre-Beta revisions, so changing their exclusion lists in place would move terrain for chunks those worlds have
not generated yet. The new revision applies `keepingVanillaOres` and then excludes the ores no declared version could
have had there - redstone (Alpha v1.0.1), lapis (Alpha), copper (1.17), emerald (Release 1.3.1) - leaving coal, iron,
gold, diamond, dirt and gravel, which is exactly what the availability table already allows for these builds.
Acceptance is a re-census of both an Indev and an Infdev world: the four era ores present, the four later ones absent.

**Recorded in** `research/indev-20100223-blocks.json` (rule `oreIsolationExperiment`) with both censuses and the
acceptance criterion. Nothing else changed this round: 588 tests, 0 failures, live jar in sync.

# Current change — 2026-10-15 / Indev: the block ids are read, the names are wrong in the middle, and the ores need a census

Three results from the artifact work the owner asked for, none of them gating anything yet.

**1. The Indev registry is read from the jar.** `in-20100223.jar` (555,633 bytes, Omniarchive Vault BETA Indev client
index), registry class `net.minecraft.a.a.b.x`, ids **1 to 62 continuous**. Two readers had to be reconciled because
neither sees everything: one takes the first two integer pushes between consecutive stores (catches constructions that
nest another object), the other walks each store back to the construction of its own field class (catches the first
blocks, which the first reader misattributes to the step-sound stores before them). Ids >= 128 were discarded as the
lookup-table sentinels they are (255, 256). Three of my own bugs had to be fixed on the way: a descriptor comparison
that appended a semicolon the regex had already consumed, a class name sliced with `[1:-1]`, and an exact-class
requirement that left 40 of 71 stores unread. The parse now lives in `scripts/extract-indev-blocks.py`; the broken
PowerShell version is deleted rather than left as a trap.

**Why the ids are trustworthy and the names are not.** Thirteen id/texture pairs match the era's terrain.png layout
exactly - stone 1/1, dirt 3/2, cobblestone 4/16, planks 5/4, sapling 6/15, bedrock 7/17, water 8/3, sand 12/18, gravel
13/19, ores 14/15/16 against 32/33/34 - so the reader is not swapping ids and texture indices. But mapping them
through the project's own Beta 1.7.3 numeric table (`research/beta173-availability.json`) produces names for ids 21-34
that **cannot** be Indev's: lapis, dispenser, sandstone, music block, bed, rails, pistons, cobweb, tall grass and dead
bush all arrive after this build, some of them (pistons Beta 1.7, tall grass and dead bush Beta 1.6) long after. So the
Beta table is usable for 1-20 and 35-62 and is evidence about Beta, not Indev, in the middle. That warning, five
individually flagged ids (4, 26, 52, 53, 55) and the reasoning are recorded in `research/indev-20100223-blocks.json`.

**2. MB's own Indev data is the citable authority for what generates**, and it is now read. Moderner Beta resolves
Indev blocks through data files, not code (`javap` on its providers finds no block references at all), so the sources
are `data/moderner_beta/worldgen/biome/indev_*.json`, `.../configured_feature/trees_indev*.json`,
`.../placed_feature/trees_indev*.json` and `.../noise_settings/finite_2d.json`. What they reference and what this
project's Indev revisions already handle: `minecraft:monster_room` and `monster_room_deep` (excluded by
INFDEV_WITHOUT_DUNGEONS), `ore_lapis`, `ore_copper`, `amethyst_geode`, `underwater_magma` (excluded through
BEFORE_LAPIS_EXISTED and the Beta root), `moderner_beta:patch_grass_alpha_2` (excluded by BEFORE_GRASS_EXISTED), and
`minecraft:strongholds` (removed by the Indev preset's own structure modifier). The biome also lists modern spawners -
bat, chicken, cow, enderman, glow squid, slime, squid, witch, zombie villager - which this project filters at runtime
through the seven-species spawn set rather than in the biome.

**3. The open question for the next round: the ores.** The ore features are not handled by those exclusion lists at
all. They are handled by `REPLACED_BY_BETA_POPULATION` (which strips the vanilla ore placement so Beta's own population
can replace it) plus whatever population the project applies per version - and for the pre-Beta eras the recorded
position is that ore distribution is **NOT researched** ("NOT researched for this version and therefore still modern:
its mob set, mob drops, food heal values, ore distribution and decoration"). So nothing in the code, as far as this
round got, says an Indev world will not generate **redstone ore**, which is Alpha v1.0.1 content and therefore
anachronistic in a build from February 2010. The block availability table already judges it ANACHRONISTIC for Indev
(its boundary is alpha_1_0_1, well above Indev's chronology), which is exactly the kind of disagreement between a table
and the generator that has bitten this project before.

**The way to settle it is a census, not an argument**: generate an Indev world, count the ore blocks it actually
contains, and compare that against the era. The project already has the instruments (`scripts/region-block-census.py`,
ColumnStats, the generation probe). That is the next round's first action, and it decides whether an exclusion is
needed for the two Infdev builds as well, since they share the same pre-Alpha revision list and both predate Alpha
v1.0.1.

**State**: 588 tests, 0 failures; live jar in sync; nothing gated on any of the above.

## 6. The recipe-banner mixin crashed the client, and why the build was green anyway

The owner's next run reached their Beta 1.9 Prerelease 1 world and then died the moment a recipe was unlocked:

```
Mixin apply for mod hex1211 failed historical_experience.mixins.json:RecipeToastHistoricalMixin
Invalid descriptor on ...historicalExperience$noRecipeBanner(Lnet/minecraft/class_374;Lnet/minecraft/class_1799;...)
Expected (Lnet/minecraft/class_374;Lnet/minecraft/class_8786;...)V
... Client disconnected with reason: Network Protocol Error
```

`class_8786` is `RecipeHolder`, not `ItemStack` (which is what the toast *displays*). The mixin is corrected to
`RecipeHolder<?>` on both `addOrUpdate` and `addItem`.

**Why nothing caught it, and what now does.** A mixin's target signature is verified by the mixin processor when the
game loads the target class, not by the compiler, and `RecipeToast` loads only when a recipe is unlocked - so
`compileJava`, the 583 tests and my own client probes were all consistent with a mixin that could not apply. The
project already has the instrument for this: `ClientMixinCheck` (`-PclientMixinCheck=<class>`) forces the named client
class to load and reports the outcome. It now reports:

```
CLIENTMIXINCHECK loaded net.minecraft.client.gui.components.toasts.RecipeToast (RecipeToast) - its mixins applied
```

with no `InvalidInjectionException` and no disconnect, in a real client run (`run/toast-mixin-check.out`). The other
two mixins added in this round never needed it: `ItemStack` and `KeyboardInput` are both loaded during startup, so the
owner's run that crashed on the toast had already proved those two applied - a lazy-mixin failure of either would
have stopped the game before the world loaded.

Deployed: live jar sha256 `C065FC1075BF0255…`, 722 entries, hash-matched to the tested build.

# Current change — 2026-10-15 / Indev block research: the jar is in hand, the registry is not yet

Following the owner's two corrections - *"Make sure you actually research indev's block availablility"* and *"You could
also extract from a jar..."* - the artifact route is started, and this records exactly how far it got.

**The artifact.** `in-20100223.jar`, 555,633 bytes, from the Omniarchive Vault BETA's Indev client index
(<https://vault.omniarchive.uk/archive/java/client-indev/index.html>), stored at
`.upstream/legacy-jars/in-20100223.jar` (gitignored, not vendored into the build). SHA-1 as computed here:
`801ce486bb7fd1b43a56bc5d226dfb1370c08678`.

**A provenance problem to settle before this jar gates anything.** The Vault index publishes **no checksums**, so that
hash is mine, computed from the download - and the project's rule for these jars is the opposite: *"the SHA-1 checked
against an independently published per-version metadata document - not against the download itself"*, which is how the
four Infdev jars in `.upstream/legacy-decompile/` were obtained. The owner supplied that document for Infdev; the same
source is needed for Indev. Until then the jar is evidence of nothing by itself.

**What the jar is.** 309 classes, fully **obfuscated** (`net/minecraft/client/a.class`, `b.class`, …), no
default-package classes, so no readable block or item names survive. Two structural candidates stand out by the number
of objects their `<clinit>` builds: `net.minecraft.a.a.b.x` (72 constructions, 7 array fields - the block registry) and
`net.minecraft.a.b.i` (67, 1 array - the item registry).

**The extraction, twice, and why neither is trustworthy yet.** A first backward scan of the integer pushes before each
`putstatic` produced a list containing lookup-table bounds (0, 255, 256) and missing many registrations. A rewritten
parse (`scripts/extract-indev-blocks.ps1`) reads each constructor call's **own** arguments and yields 45 calls / 44
distinct ids (6, 7, 12-16, 18, 20-42, 45-53, 55-57, 59) - still not the registry: it misses stone (1) at one end and
everything above 59 at the other, because many blocks are built by subclass constructors whose descriptors are longer
than the `(II…)` shape that pass accepts. Indev stores **no block name strings**, so even a complete id list needs the
era's documented id table to become names.

**Nothing is gated on any of this.** The wiki sweep artifact (`research/indev-block-availability.json`) stays marked
INCOMPLETE and unusable; the jar findings are in `research/indev-jar-provenance.json` (status PROVISIONAL) and
`research/indev-20100223-blocks.json`. `HistoricalBlockAvailability`'s Indev verdicts still rest only on the sixteen
boundaries whose own evidence dates them to Classic or earlier, plus diamond ore's introduction-and-withdrawal dates -
which is what the entry below records.

**Build**: 588 tests, 0 failures. **Deployment** still pending: the owner's client held the live jar through this round.

# Earlier change — 2026-10-15 / Indev: the biome upgrader, and the finite -> infinite border, both measured

Rounding out the Indev objective. Owner: *"remember the biome upgrader also itll upgrade from a finite to infinite
world so account for that"*. 588 tests, 0 failures.

## The biome upgrader: PLACEHOLDERS was a list nothing consulted

`BiomeCarryForward` keeps a `PLACEHOLDERS` set that already named `moderner_beta:indev_normal`, `indev_hell`,
`indev_paradise`, `indev_woods` and the Infdev names - and **nothing used it** except a test accessor. So a stored
Indev or Infdev biome reached the renamed-place suffix lookup, was not recognised, and answered **null**, which the
driver reads as "no change needed": an Indev world upgraded to Beta 1.8.1 would have kept `moderner_beta:indev_normal`
inside it, and the same was true of every Infdev placeholder.

`inLateBeta` and `inEarlyRelease` now carry any placeholder to `LATE_BETA_PLAINS`, and the switch's Alpha-only and
Beta-plains-only branches already mapped them correctly. Pocket Edition provider names (`moderner_beta:pe_*`) are
**deliberately excluded** by `isCarriedPlaceholder`: they come from another edition and this project has no researched
hierarchy for them, so moving one would be inventing a mapping - the existing test that pins that behaviour still
passes, and a new test asserts every Java-line placeholder carries while every `pe_*` name does not.

## The finite -> infinite border: found in the wrong NBT shape first

Owners' requirement implemented in `WorldGenSettingsAdvance`, inside the **same** `level.dat` write as the generator
advance and therefore under the same backup and journal - a second, later write would be invisible to the crash
recovery path.

The first attempt was wrong in a way worth recording. It looked for a `WorldBorder` compound under `Data`, found
nothing, and logged *"world border was absent, so the default already applied"* - over a world that was still 256
blocks wide. **1.21.1 flattens the border into `Data` as `BorderSize`, `BorderSizeLerpTarget`, `BorderCenterX/Z` and
the warning keys.** A byte-level dump of the world's own `level.dat` showed the real keys, and the release now writes
`BorderSize` **and** `BorderSizeLerpTarget` - leaving the lerp target alone would let the border crawl back to 256 while
the world looked released.

## Evidence: a real Indev world upgraded to Infdev, twice measured

`run/indevlatest` (the Indev world generated earlier) copied to a second world, upgraded through the project's own
path (`runServer -PgenerationProbe -PprobeUpgradeTo=historical_experience:infdev_420`), then re-opened and read:

```
[run 1] Finite world released: historical_experience:indev_latest_no_modern_content ->
        historical_experience:infdev_420_no_modern_content, world border size 256.0 (vanilla default 2.9999984E7)
[run 2] Historical world detected: profileId=historical_experience:infdev_420
        rawRecipePreset=historical_experience:infdev_420_no_modern_content
[run 2] FINITEPROBE borderSize=2.9999984E7 borderCenter=0,0 surfaceWithin=6 solidBeyond128=4
```

Before the upgrade the same world read `FINITEPROBE borderSize=256.0`; after it, vanilla's default. The upgraded
world's metadata records revision 2, `profileId=historical_experience:infdev_420`, with a history entry
*"version upgrade to historical_experience:infdev_420 on generation recipe
historical_experience:infdev_420_no_modern_content"*. Logs: `run/indev-upgrade2-1.out`, `run/indev-upgrade2-2.out`;
worlds kept at `run/indevupgrade`/`run/indevupgrade2`.

One thing the evidence also settles for later: the upgraded world's spawn chunk still reported
`moderner_beta:indev_normal` at the first load, which is correct - the **biome conversion is a separate
chunk-rewriting pass**, and the carry-forward rule above is what decides where those stored biomes go.

## Still open

1. **Indev block availability from the 20100223 client jar** - the outstanding correction. The wiki sweep remains
   inadequate and its artifact stays marked INCOMPLETE; the jar route needs the exact artifact and its published
   SHA-1, and Indev predates MCP mappings so the registry has to be identified from decompiled code.
2. The remaining "gate everything" sweep for Indev (crafting/availability tables, HUD, textures) - several gates
   already answer correctly because they are boundary-based and Indev sits below every boundary, which
   `HistoricalGameplayTweaksTest.everyGateAgreesWithItsBoundary` checks for every profile.
3. **Deployment is still pending**: the owner's client held the live jar through this entire round (Prism PID 12560),
   so both this build and the hang watchdog are built, tested and undeployed.

# Earlier change — 2026-10-15 / Indev 20100223: registered, generating, finite — and the block research that is NOT done

Owner: *"Implement Indev the latest version as you did every other version ... it should start using infdev version
gating and then you can adjust to what it should be also use the indev latest generation in moderner beta for gen
remember the biome upgrader also itll upgrade from a finite to infinite world so account for that"*, scope confirmed as
one profile (20100223, finite, with the border it shipped with), then: *"Make sure you actually research indev's block
availability"* and *"You could also extract from a jar..."*.

## What is implemented and verified

- **Registry**: `historical_experience:indev_latest`, "Indev 20100223", era `indev`, chronology `ERA_INDEV + 223`
  (200_223, the era + month*100 + day convention the Infdev builds use). New `Era.INDDEV("Indev", 200_000)` in
  `HistoricalVersionCatalogue`, and an implemented-extra entry beside the two Infdev builds.
- **Rules**, starting from Infdev as instructed and adjusted to Indev's own record:
  `HistoricalSpawnRules.INDEV_20100223_MOBS` (the same seven species, adopted from the next *measured* era because no
  research row adds or removes an Indev species); `HistoricalLootRules.INDEV_20100223` (Indev 20100219's tables:
  zombie 0-2 feathers, skeleton 0-2 arrows, pig 0-2 raw porkchops, spider 0-2 string - bones are Beta 1.2, rotten
  flesh is Beta 1.8); `HistoricalFoodRules.INDEV_20100223` (apple 4, bread 5, stew 10, raw porkchop 3, cooked 8,
  brown mushroom 5). **Not** Infdev's food table: Infdev's emptiness is a measurement of Infdev, while Indev is where
  eating was invented. No hunger, no sprinting, no experience, no structures.
- **The red mushroom's harmful -3 is recorded but not applied** (`UNMODELLED_HARMFUL_FOOD`): this model is positive
  heals only and two validations reject a non-positive value deliberately, so expressing a damaging food needs the
  eat path to damage the player - a behaviour change that does not belong inside a version registration. Recorded for
  the owner rather than silently dropped.
- **Generation**: three project-owned settings presets (`indev_latest{,_no_grass,_no_modern_content}`) wrapping
  Moderner Beta's `moderner_beta:indev` - deepslate already off, cave biomes off, and the preset's **stronghold
  placement override removed**, because a stronghold at distance 0 would put Release 1.0's structure in the middle of
  a 256-block Indev island - plus the matching world presets. Deliberately **no** `overworld_128` noise override: the
  finite provider reads its own `finite_2d` settings and its height comes from `finite_level_properties`.
- **Create-world menu**: `minecraft:normal` world-preset tag now lists
  `historical_experience:indev_latest_no_modern_content` (the newest revision, which is what the menu test requires).
- **Structure rule**: `noStructures("Indev 20100223", ...)` in `HistoricalStructureRules`, with the Indev house noted
  as a placed *spawn* feature rather than a structure-set entry.
- **Block chronology**: `HistoricalBlockAvailability.CHRONOLOGY` now starts at Indev, and the **sixteen** boundaries
  that were clamped to the then-oldest profile (Infdev) moved to Indev - bedrock, the stone/ore families, leaves and
  saplings, glass and sponge, TNT, grass block, obsidian, wheat/farmland, and diamond ore (whose own history is
  "introduced Indev 0.31 20100128-2200, withdrawn in Infdev 20100227-1414, restored 20100320" - this build sits
  between the introduction and the withdrawal, so it HAD diamond ore). A comment at the static block records the
  clamping rule and which rows moved, and the palette-derived halves of those evidence strings still name Beta 1.7.3.

**Runtime evidence that the finite profile works** (`runServer -PenchantProbe`, log `run/indev-latest-probe.out`,
world kept at `run/indevlatest/`):

```
Historical marker decision for .\indevlatest\.: action=ESTABLISH_METADATA
  marker=historical_experience:indev_latest_no_modern_content
Established historical world metadata ... profileId=historical_experience:indev_latest
Preparing level "indevlatest"
Done (3.196s)! For help, type "help"
Biome at spawn 42, 65, -10 = moderner_beta:indev_normal (profile historical_experience:indev_latest)
Biome variety across 81 samples: 1 distinct -> [moderner_beta:indev_normal]
FINITEPROBE borderSize=256.0 borderCenter=0,0 surfaceWithin=6 solidBeyond128=4
```

So a project-owned Indev world **loads and generates**, single-biome, finite, with the 256-block border centred on
the origin. That also narrows the old blanket note in `HistoricalVersions` ("Classic and Indev do not generate"): the
hang was measured on **Classic** (`moderner_beta:classic_0_30`, even from Moderner Beta's own preset); Indev is a
different provider path and does not hang. **Classic stays withdrawn** - only Indev is offered.

**Build**: 584 tests, 0 failures, 34 declared profiles.

## What the block-availability research is NOT

The owner asked for Indev's block availability to be *researched*, and it is **not** yet good enough to gate anything.
`scripts/research-indev-blocks.ps1` sweeps minecraft.wiki's Indev build pages, and its first pass was bad in two
visible ways: it enumerated pages by title prefix (182 hits, subpages included) and it accepted any wiki link inside a
section whose heading contains "Blocks", so its output contains prose links ("Gear", "block", "infinite water
source") and misread a "no longer" line as a removal of lava. Its own artifact says so:
`research/indev-block-availability.json` is marked **INCOMPLETE / NOT AUTHORITATIVE** and must not be used.

The right source is the **jar**, as the owner says, and the project already has the route and the tools for it: the
Infdev jars came from Omniarchive with every SHA-1 checked against an independently published per-version metadata
document (recorded in `infdevTerrainOnly`'s provenance comment), and `scripts/extract-b173-palette.py` /
`extract-block-palette.py` are the extraction pattern used for Beta 1.7.3. Two things to settle before Indev's jar is
used: the exact 20100223 artifact and its published SHA-1, and how to read its block registry - Indev precedes MCP
mappings, so `.upstream/legacy-decompile/b19pre6`'s named packages are not available for it, and the registry has to
be identified from the decompiled code itself. That is the next step, and the table's Indev verdicts should not be
considered reviewed until then.

## Still to do for this objective

1. Indev block availability from the client jar (above), then the table's Indev verdicts reviewed against it.
2. The rest of "gate everything": crafting/availability tables, HUD and texture rules, and the swept gates for
   Indev (several already answer correctly because they are boundary-based and Indev is below every boundary).
3. **Biome upgrader**: `BiomeCarryForward.destinationFor` returns null for `moderner_beta:indev_normal`
   (and `indev_hell`/`indev_paradise`/`indev_woods`) in the Late Beta and Early Release generations, so an Indev
   biome would survive into a Beta 1.8.1+ world; that family must join the placeholder branch.
4. **Finite -> infinite upgrade**: a finite Indev level persists `size=256` in `level.dat`, and advancing the preset
   to an infinite Infdev/Alpha generator does not clear it, so the upgrade path must reset the border to the
   destination's default and record it in the upgrade record/journal, preserving existing chunks as always.
5. Tests for the above, docs (coverage/provenance), and deployment - the watchdog build is still undeployed because
   the live jar is held by the running game.

# Current change — 2026-10-15 / the freeze on leaving a world, and the Indev reconnaissance

## The freeze: localised, not reproduced, now diagnosable

Owner: *"there was a freeze"*, then, asked where: **"Froze leaving the world — screen stuck on 'Saving world'/
loading, never reached the title screen."**

**What the log proves.** `logs/2026-10-15-7.log.gz` (the 16:25 session) ends at
`[16:26:07] [Server thread/INFO]: Saving worlds` and then contains **nothing**: no "Stopping worker threads", no
`Reloading resources for historical textures: … -> …`, no `Historical colour maps installed: none`, no atlas creation,
no `Stopping!`. The session that left a world earlier the same afternoon (16:03, log 5) contains all of those before
its `Stopping!`. So the client did not crash - it stopped in the game's own teardown, **before this mod's restore
reload is even reached**. There is no crash report (newest is 10:54) and no watchdog dump.

**What I could not do.** Reproduce it. A temporary quit probe (`QuitProbe`, since deleted) joined the Beta 1.8.1
fixture, called `server.halt(false)` the way "save and quit" does, and dumped the stacks from its own daemon thread 30
seconds later. The client left the world cleanly: the restore reload ran, `Historical colour maps installed: none (was
grass+foliage_new)`, `level=null`, and the Render thread was `RUNNABLE` at the multiplayer screen
(`run/quit-probe-b18.out`). The dev harness therefore does not reproduce it, and the most likely reason is a
difference in the clients: the owner's runs Iris, Sodium, EntityCulling, ImmediatelyFast, Lithium, FerriteCore and
ModernFix beside this mod, while `runClientQuickPlay` runs none of them.

**What now exists instead of a guess.** `LoadingHangWatchdog` (permanent, project-only, client): if the client's
loading overlay stays up for **30 seconds** after a world has been joined, it logs one warning with the client's
level/screen/overlay state and the stacks of the render thread, server thread, main and the worker pools. It fires once
per stuck overlay, never blocks and never throws. It is deliberately not a fix: on a hang the log otherwise simply
stops, and the next occurrence will now name the frame. The threshold cannot be tripped by an ordinary start-up, and
an ordinary world is untouched (it reads the client's own overlay and writes one log entry).

**Next occurrence**: send the `HANGWATCHDOG` block from `logs/latest.log` and the frame that holds the lock will be
visible in it.

## Indev reconnaissance (implementation next)

Owner, on scope: *"Implement Indev the latest version as you did every other version … it should start using infdev
version gating and then you can adjust to what it should be also use the indev latest generation in moderner beta for
gen remember the biome upgrader also itll upgrade from a finite to infinite world so account for that."* Scope
confirmed: **one profile, Indev 20100223, finite with the border it shipped with** (not the four themes).

**What Moderner Beta already provides** (read from the shipped jar, not assumed):
`data/moderner_beta/moderner_beta/settings_preset/indev.json` and `indev_{hell,paradise,woods}.json`; biome
`moderner_beta:indev_normal`; provider `moderner_beta:finite_2d`; noise settings `moderner_beta:finite_2d`;
`finite_level_properties` = type `island`, height 128, **length 256, width 256**, theme `normal`;
`world_border` = enabled, **width 256**, centre origin, falloff `ocean`, ground level 55; an out-of-bounds biome
injection to `minecraft:the_void` (margin 4); deepslate already disabled; cave biomes already `none`; and a stronghold
placement override this project's presets will drop. Also `ChunkProviderFinite`, `ChunkProviderFinite2D`,
`SpawnLocatorIndev` and `IndevTheme`/`IndevType` classes.

**The earlier attempt's conclusion is narrower than its comment.** `HistoricalVersions` says Classic *and* Indev are
not offered because Moderner Beta's finite path does not generate. The measurement behind that is **Classic**
(`moderner_beta:classic_0_30` hangs on chunk (0,0) even from Moderner Beta's own preset), while
`run/indevprobe.out` shows an Indev world **generating**: spawn prepared in 1.25 s, biome at spawn
`moderner_beta:indev_normal`, `FINITEPROBE borderSize=256.0 borderCenter=0,0 surfaceWithin=6 solidBeyond128=4`. So
Indev is worth re-measuring as its own case before repeating the Classic verdict, and that is the first runtime step.

**The project shape to add** (copied from how Infdev is declared, per the owner's "start using infdev version gating"):
project-owned settings presets `indev_latest{,_no_grass,_no_modern_content}.json` wrapping `moderner_beta:indev` (the
finite preset must **not** take the `overworld_128` noise override Infdev's presets use - it has its own
`finite_2d` settings and a 128-high level), matching world presets, a `terrainOnlyWithRules` entry with
`ERA_INDEV + 223` (era + month*100 + day, the Infdev convention), and a catalogue `Era.INDDEV("Indev", 200_000)`
entry - `ERA_INDEV = 200_000` already exists in `HistoricalVersions`.

**The Indev facts the project's own research already establishes** (all VERIFIED, `research/feature-timeline.json`
unless noted): mobs burn in daylight from **Indev 20100213**; mob armour and arm-swinging **removed in Indev
20100202**, so 20100223 has neither; fire from **Indev 20100109-1939** with infinite burn until Beta 1.2_02;
TNT primed by an empty hand (through Beta 1.6.6); farming, seeds and **wheat/farmland from Indev 20100206-2034**;
diamond ore from Indev 0.31; natural obsidian from water on lava; **drops from Indev 20100219** - zombie 0-2
feathers, skeleton arrows (bones only from Beta 1.2), pig 0-2 raw porkchop, spider 0-2 string; **sheep drop light
grey cloth from Indev 20100223** (white from Infdev 20100624); drowning present (1 damage/second from Indev
20100125); food is the direct-heal model - `research/historical-features.json` records apple 4 HP (Indev 20100110),
bread 5 HP and mushroom stew 10 HP (Indev 20100206-2034), raw porkchop 3 HP / cooked 8 HP (Indev 20100219). Infdev's
*empty* food table is a measured property of Infdev, **not** a baseline to inherit for Indev, which is exactly the
"then you can adjust" half of the owner's instruction.

**Biome upgrader and finite -> infinite** (owner: *"remember the biome upgrader also itll upgrade from a finite to
infinite world so account for that"*): `BiomeCarryForward.destinationFor` maps a stored biome into the destination's
generation and already treats `moderner_beta:alpha` as the pre-biome placeholder, but an Indev source biome
(`moderner_beta:indev_normal`, and `indev_hell`/`indev_paradise`/`indev_woods`) falls through `inLateBeta` and
`inEarlyRelease`'s suffix lookup and returns **null = no change**, which would leave an Indev biome inside a Beta
1.8.1+ world. That family must join the placeholder branch. The finite -> infinite half is the **world border**: a
finite Indev level persists `size=256` in `level.dat`, and advancing the preset to an infinite Infdev/Alpha generator
does not clear it, so the upgrade path must reset the border to the destination's default and record that in the
upgrade record/journal, with existing chunks inside the old border preserved as always.

**Build**: 584 tests, 0 failures; `apply-licence-headers.py --check` → "all 383 Java files carry the correct licence
header (103 Nostalgic Tweaks-derived, 280 project)". The watchdog build is **not deployed** - the game was running and
holds the live jar (live jar still `3964BC84A89BD1A8…`, the experience-number build).

# Current change — 2026-10-15 / the experience level number, and a boundary the owner corrected

Owner: *"The experience point number was only added in a later version check when it was but you couldnt see how much
experience you had in beta 1.8 or pre 1 im not sure when it was added find out and gate it"*.

## The boundary, and the source that misled

The bar and the number are **two** boundaries. Beta 1.8's Adventure Update added the bar (its own history line records
it being shown as "Skill points" in the development trailer); the **number above it arrives in Beta 1.9 Prerelease 3** -
minecraft.wiki, *Java Edition Beta 1.9-pre3*, section **Experience levels**: *"Now shown above experience bar."*

This rule was first written against the wrong build, and the owner caught it inside a minute: *"Experience levels were
first shown ... during Beta 1.9 Pre-release 3. and yet you're launching pre 3 and it dont' show thats the version it
should show"*. The cause is worth recording: the *Experience* article's History table files "The experience level
number is now shown" under its **Prerelease 4** row - the same row that carries the levelling-curve change and the
ability to spend levels - so a reader of that table alone lands one build late. The specific version page for
Prerelease 3 lists the number as its own entry, and the owner, who was playing Prerelease 3 at the time, named the same
build. Version page and owner agree; the History table is recorded in the code as the source that misled rather than
as an alternative.

## The change

- `HistoricalGameplayTweaks.experienceLevelShown(versionId)` - true from Beta 1.9 Prerelease 3, false for Beta 1.8.1
  through Prerelease 2. Positive predicate, so a null or undeclared version keeps the modern answer, per the rule on
  `functionalEnchanting` that four features had wrong.
- `HudHelper.hideExperienceLevel()` - deliberately separate from `hideExperienceBar()`, because the bar and the number
  are different boundaries: a Beta 1.8.1 world draws the bar with no number.
- `GuiHistoricalExperienceLevelMixin` - cancels `Gui.renderExperienceLevel` at its head, so the bar, the bar's geometry
  and every element positioned around it are untouched. **Not** an NT port, and marked as such in the file: upstream's
  nearest feature is `CandyTweak.SHOW_EXP_LEVEL_TEXT`, an optional corner readout drawn by its own `GuiListener`
  (default off), and `HIDE_EXPERIENCE_BAR` hides the whole bar - NT has nothing for "bar without the number", so this
  file carries the project header alone.

## Evidence - pixels, in an A/B fixture pair

A temporary probe (`XpLevelProbe`, since deleted along with its registration and its Gradle property) joined a fixture
world, granted 30 levels through the integrated server, and screenshotted the HUD 80 ticks later:

| fixture | version | gates reported | screenshot |
| --- | --- | --- | --- |
| `xp-b18` (copy of `fixture-world`) | beta_1_8_1 | `barHidden=false levelHidden=true` | bar drawn, **no number** |
| `xp-pre3` (copy of `New World (2)`) | beta_1_9_pre_3 | `barHidden=false levelHidden=false` | bar drawn, **"67" above it** |

`run/client/screenshots/xp-probe-historical_experience_beta_1_8_1.png` and `...beta_1_9_pre_3.png`; logs
`run/xp-probe-b18.out`, `run/xp-probe-pre3.out`. The two fixture copies are kept in `run/client/saves` as the A/B pair.

## The descriptor, again - and the check that now catches it

`GuiHistoricalExperienceLevelMixin` first declared `(GuiGraphics, Font)`. The parameter is
`(GuiGraphics, DeltaTracker)`: `fgf` in the tiny mappings is `class_9779` = `DeltaTracker`, and this was the second
time in one session that a descriptor was assumed from the mapping letters instead of resolved. The temporary probe
run caught it immediately - the client crashed at startup with `InvalidInjectionException` naming both descriptors -
so nothing shipped this time, unlike the recipe toast. The letters are resolved (`fhz` = `class_332` = `GuiGraphics`,
`fgf` = `class_9779` = `DeltaTracker`) and the mixin is documented with both.

**Build**: 584 tests, 0 failures; `apply-licence-headers.py --check` → "all 382 Java files carry the correct licence
header (103 Nostalgic Tweaks-derived, 279 project)". **Deployed**: live jar sha256 `3964BC84A89BD1A8…`, 723 entries,
hash-matched. The owner restarted and confirmed it in game ("yep it worked").

# Current change — 2026-10-15 / five reports: sneak in ordinary worlds, recipe banners, beds, Programmer Art, jump XP

Owner: *"you need to totally disable the banners that appear when you unlock new recipes, also coloured beds didn't
exist until well idk when but they were red beds for a long time no matter the wool used and they were just called
beds. I can also not shift in a default world which means some stuff is still being disabled. When textures are
disabled this should include the programmer art pack. When jumping in pre 1.9 beta 1 the xp being added is incorrect
consult the wiki for how much should be added per jump and/or inspet the jar."*

## 1. Sneaking (and three siblings) were switched off in ordinary worlds — fixed

**The defect, and it was a class of them.** These predicates answer a POSITIVE question and their call sites *cancel*
modern behaviour when the answer is false. All four answered false for a version they did not know, which includes a
world with no historical session at all:

| predicate | call site | what an ordinary world lost |
| --- | --- | --- |
| `sneakingAvailable` | `EntityHistoricalSneakingMixin`, `LocalPlayerHistoricalSneakingMixin` | the shift key did nothing - the owner's report |
| `functionalBrewing` | `BrewingStandHistoricalBrewingMixin` | the brewing stand refused to brew |
| `endPortalFunctional` | `EnderEyeHistoricalPortalMixin` | the eye of ender returned PASS, so the portal never filled |
| `functionalEnchanting` | `EnchantmentMenuHistoricalEnchantingMixin` (via `enchantingDisabled`) | the enchanting table click did nothing |

All four now answer the modern way when the version is null **or undeclared**, which is "this build has nothing to say
here". Their version boundaries are unchanged, and the tests that asserted the old answer
(`SneakingBoundaryTest.unresolvedVersionsNeverGetSneaking`, `PrereleaseProgressionTest.unresolvedVersionsGetNothing`,
`EnchantingBoundaryTest.undeclaredAndNullVersionsAreNeverTreatedAsEnchanting`) were rewritten to assert the fixed
behaviour with the reason. The negative-sense predicates (`offhandUnavailable`, `usesOldTableRules`,
`appliesTo(gate, …)`) already meant "no restriction" for a null version and were left alone.

## 2. Recipe banners — disabled

`RecipeToastHistoricalMixin` cancels both `RecipeToast.addOrUpdate` (the banner appears) and `RecipeToast.addItem`
(an existing banner grows), so no "New Recipe Unlocked!" banner can show in a historical world. An ordinary world
keeps the vanilla banner. Context: the recipe book itself is absent from every declared version (the newest declared
profile is Release 1.6.4; the book is Release 1.12), so the banner was the visible half of a feature that is not there.

## 3. Beds — red, and called "Bed"

Owner: *"they were red beds for a long time no matter the wool used and they were just called beds"*. minecraft.wiki,
Bed: the sixteen coloured beds appear in the 1.12 history row (**17w15a**), and "The red bed was the original default
bed color".

- **Crafting**: every bed item is substituted to `minecraft:red_bed` in any declared version, through the same result
  rewrite the logs use (`HistoricalCraftingEra.substitutedResult` + `CraftingRecipeHistoricalWoodMixin`). Before Beta
  1.3 there is no bed at all, so nothing is substituted and the recipe stays refused - no bed is conjured out of wool.
- **Name**: `ItemStackHistoricalBedNameMixin` returns this project's own `historical_experience.item.bed` ("Bed") from
  `getHoverName` for any bed in a declared version, because the item a player holds is the modern `red_bed` whose own
  name is "Red Bed". Every bed colour is covered, since a world may already hold others.
- The rule is written as "every declared version" rather than as a comparison against a 1.12 boundary, because no 1.12
  profile exists; `noDeclaredVersionHasColouredBeds` fails if one is ever declared, so the boundary has to be written
  then instead of silently painting 1.12's beds red.

## 4. Programmer Art is turned off with the textures it belongs to

Owner: *"When textures are disabled this should include the programmer art pack."* Programmer Art is the one source
this feature uses that changes the game **by being selected**: the mod's layer opens it directly, but if the player
also has it ticked, turning historical textures off left it applying on its own. `HistoricalTextureService` now
deselects `programmer_art` while the texture setting is OFF, persists the change the way the pack screen does
(`Options.updateResourcePacks`) and reloads; a player with the setting ON keeps their own pack selection untouched,
and an ordinary world is never touched. Golden Days and Continuation are the player's own downloads and are only read
through the layer, so their selection is left alone.

## 5. Jump XP: the amount was right, the other half of the wiki entry was missing — now implemented

Owner: *"When jumping in pre 1.9 beta 1 the xp being added is incorrect consult the wiki for how much should be added
per jump."* The wiki's own page for that build says, under **Jumping**:

> * When the [[player]] jumps, their [[experience]] increases by a number between 1 and 100 experience.
> * Continuous jumping by holding down the jump key is no longer possible.

`HistoricalGameplayTweaks.jumpExperienceBug` already awarded `1 + random.nextInt(100)` - 1 to 100 inclusive - and only
in Beta 1.9 Prerelease 1, so the amount and the build were what the wiki states. Asked which half was wrong, the owner
confirmed it is the second: **holding the jump key still re-jumps**, so the payout repeated on every landing where
that build required a fresh press. This project's own note had claimed that half "needs no code, because it describes
this method being reached on every tick the key is held, which is what vanilla does" - which is exactly the error:
vanilla *does* keep jumping while the key is held.

- `HistoricalGameplayTweaks.jumpRequiresFreshPress` answers from the same `jumpExperienceBug` gate, so both halves are
  the one build (our research records Pre2 as the build that "removes the XP bug and restores held-jump auto-repeat").
- `HistoricalJumpInput` is a one-flag latch: "a jump has happened since the jump key was last released".
- `KeyboardInputHistoricalJumpMixin` reads and writes it at `Input.tick`'s `TAIL` - the point where the key state has
  just been read into `Input.jumping`. A held key has its jump input cleared; releasing the key arms the next press.
  Suppressing the input rather than the jump keeps the client and server agreeing: the client never asks for the
  second jump, instead of predicting one the server refuses.
- The latch is cleared on disconnect, so a jump made in one world cannot suppress the first jump in the next.
- **An ordinary world and any undeclared version keep vanilla's held-jump repeat** (`jumpRequiresFreshPress(null)` is
  false), which is the same null rule as the four predicates fixed in item 1 above, and it is asserted.

**Build**: 583 tests, 0 failures; `apply-licence-headers.py --check` → "all 381 Java files carry the correct licence
header (103 Nostalgic Tweaks-derived, 278 project)". **Deployed**: live jar sha256 `97ADEDD45D15113D…`, 722 entries,
live hash independently compared equal to the tested build. The owner must restart Minecraft completely.

Not verified in a client run: the held-jump change (it needs a held key, which no probe here presses) and the recipe
banner suppression. Both are gated on the session, and both are the kind of change a hand on the keyboard settles in
seconds.

# Earlier change — 2026-10-15 / the off-hand slot by NT's own method, and logs that make planks again

Two owner reports. *"Birch trees are available in beta 1.2 but I can't craft anything with the logs and they dont go
into planks. Until Release 1.2 they were just wooden planks but in 1.2 they start crafting into birch planks."* and
*"you've failed yet again to remove your fix to do the offhand slot in inventory and just only take and directly
apply NT's fix."*

## 1. The off-hand slot: NT's method, and what "my fix" actually was

**Why the slot was still visible.** The previous in-house port moved the slot off-screen, which hides the slot itself
but not the box the player sees: **the off-hand box is painted into the game's own inventory background**
(`assets/minecraft/textures/gui/container/inventory.png`, checked against the 1.21.1 client jar). Moving a slot cannot
remove part of a background texture, so the empty box stayed - and the screenshot from 12:29 today shows it.

**What NT does, now ported directly.**
- `SlotTracker` identifies the slot **by position** (77,62), not by index or container, and moves it with
  `slot.x = x; slot.y = y;` - so upstream needs those fields writable, and it gets that from *its access widener*:
  `mutable field net/minecraft/world/inventory/Slot x I` / `... y I` under an `# Inventory` heading. **That is the
  piece my version reinvented**: a duck interface (`SlotPositionAccess`) plus a `@Mutable @Shadow` mixin
  (`SlotPositionMixin`), built on the false belief that the fields could not be lifted. Both are **deleted**, and the
  two upstream lines are copied into `historical_experience.accesswidener` unchanged.
- `HistoricalInventoryScreen` is NT's `InventoryScreenHelper` trimmed to the `InventoryShield.INVISIBLE` branch:
  `SlotTracker.OFF_HAND.move(slot, -9999, -9999)` and upstream's own paint-over patch
  (`graphics.blit(INVENTORY, leftPos + 76, height / 2 - 22, 178, 74, 18, 18)`), with upstream's inventory texture
  copied in byte-for-byte.
- `InventoryScreenHistoricalOffhandMixin` is NT's `InventoryScreenMixin` for the two injections that matter here
  (constructor `TAIL` and `renderBg` `TAIL`, calling the same two helpers), with its shield-position setting replaced
  by this project's version gate. Upstream's crafting-grid/recipe-button/label/`hasClickedOutside` injections belong
  to its full old-inventory layout and are recorded as not ported.
- `InventoryShieldSlotMixin` is NT's own, including its target (`InventoryMenu$1#getNoItemIcon`), its `null` return,
  and its second condition - upstream's creative-mode check is `gameMode.hasInfiniteItems()`.

**Evidence.** The project's NT visual probe on the Beta 1.7.3 fixture (`runClientQuickPlay -PquickPlayWorld=hexflash
-PntVisualProbe`, log `run/offhand-visual.out`): `version=historical_experience:beta_1_7_3`,
`recipeBookDisabled=true`, and `run/client/screenshots/nt-probe-inventory.png` now shows the inventory **with no
off-hand box**, where the 12:29 screenshot of the same screen had one.

**Provenance.** `client/inventory/SlotTracker.java`, `client/inventory/HistoricalInventoryScreen.java` and
`mixin/InventoryShieldSlotMixin.java` carry the compound header and are in `NT_FILES`;
`client/SlotPositionAccess.java` and `mixin/SlotPositionMixin.java` are gone from it. The copied texture and the two
access-widener lines are recorded in `docs/PROVENANCE.md` and `THIRD_PARTY_NOTICES.md`, and the declared counts move
with them: **103 Java files, 53 asset files** (`scripts/apply-licence-headers.py --check`: "all 377 Java files carry
the correct licence header (103 Nostalgic Tweaks-derived, 274 project)").

## 2. A log makes planks again before Release 1.2

**The defect.** The crafting gate refuses a recipe whose result item did not exist yet, and `birch_planks`,
`spruce_planks` and `jungle_planks` are dated Release 1.2. Refusing them left the logs those trees drop with **no
recipe at all** - which is the owner's "I can't craft anything with the logs": the recipe was hidden and nothing
replaced it.

**The fix.** Those three items are now *substituted* rather than refused before Release 1.2:
`HistoricalCraftingEra.substitutedResult` maps them to `minecraft:oak_planks` - the single planks item those versions
had - and `CraftingRecipeHistoricalWoodMixin` rewrites the assembled result (count and components preserved) in the one
method a crafting recipe turns its inputs into a stack, `CraftingRecipe.assemble`, targeting `ShapedRecipe` and
`ShapelessRecipe`. Judged by the result, so every route to a species plank before Release 1.2 yields the one planks
item, whatever the recipe was called. Nothing changes from Release 1.2 onward, and an ordinary world has no session
and is untouched.

**Evidence - a server probe through the real `RecipeManager`** (temporary `CRAFTPROBE`, since removed; log
`run/craft-probe.out`):

| version | birch_log | spruce_log | jungle_log | oak_log |
| --- | --- | --- | --- | --- |
| beta_1_2 | 4x oak_planks | 4x oak_planks | 4x oak_planks | 4x oak_planks |
| beta_1_7_3 | 4x oak_planks | 4x oak_planks | 4x oak_planks | 4x oak_planks |
| release_1_1 | 4x oak_planks | 4x oak_planks | 4x oak_planks | 4x oak_planks |
| release_1_2_5 | **4x birch_planks** | **4x spruce_planks** | **4x jungle_planks** | 4x oak_planks |

Each row is `RecipeManager.getRecipeFor` followed by `assemble`, which is exactly the pair of calls the two mixins
judge. The boundary is the one the owner named.

**Tests.** **580, 0 failures.** `HistoricalCraftingEraTest` gains `logsArePlanksBeforeRelease12` (both sides of the
Release 1.2 boundary, oak not substituted, a later wood refused rather than substituted, ordinary world untouched) and
its Alpha sweep now reports the item a player *receives*, so the species planks can no longer appear craftable in a
version that had one planks item.

Deployed once the game was closed: live jar sha256 `61020224CCAC9477…`, 717 entries, live hash independently compared
equal to the tested build. The owner must restart Minecraft completely.

# Earlier change — 2026-10-15 / Beta's sky colour reverted to the flat value the owner prefers

Owner: *"Colour for beta 1.7.3 should be reverted to before I recently told you to change as in the sky colour."*

**What "before" was, read out of git rather than memory.** Commit `9f9a4db` ("Universal sky colour per era")
carried `HistoricalWorldTweaks.eraSkyColour` with SKY_BETA `0x97A3FF` for the window `[beta_1_0, beta_1_8_1)`, and
`ClientLevelHistoricalSkyColourMixin` replaced the **finished** `ClientLevel.getSkyColor(Vec3, float)` vector with
that flat colour. The 2026-10-14 change - the owner's own correction, recorded in the section below - deleted that
mixin and replaced the flat Beta value with the clients' temperature function for `[beta_1_0, beta_1_9)`.

**Reverted.** `skyEra`/`eraSkyColour` map Infdev, Alpha and Beta below Beta 1.8.1 to `FLAT` again, with
`HistoricalSkyColours.INFDEV` `0x99CCFF`, `.ALPHA` `0x8BBDFF` and the new `.BETA_FLAT` `0x97A3FF`; the window's end
returns to Beta 1.8.1 as well, since the 2026-10-14 change had extended it to Beta 1.9. The temperature function,
its Moderner Beta sampler hook and its tests are **retained and no longer reached by any declared version** - the
mapping methods are where the decision lives now, so reversing it again is those two bodies and nothing else. A new
test derives the window from the registry and asserts that exactly those versions are served and that every one of
them has a flat colour, so the retained path cannot become live by accident.

**One deliberate difference from the pre-change code, stated because it is visible.** The flat colour is applied as
the *biome's base* colour, which is where the 2026-10-14 change put it, so the level's own day/night and rain
processing still applies on top. The pre-change hook replaced the finished vector, so it drew `0x97A3FF` even at
midnight. Daytime is identical; midnight and rain are not. If the owner wants the flat colour at all times of day,
that is a one-line change to `BiomeHistoricalSkyColourMixin`.

**Evidence.** Build: **579 tests, 0 failures** (the sky suite now pins the reverted mapping both by name and over the
whole catalogue). Client run of the project's own NT visual probe on the Beta 1.7.3 fixture
(`runClientQuickPlay -PquickPlayWorld=hexflash -PntVisualProbe`, log kept at `run/sky-revert-probe.out`):

```
biomeBaseSky=0x97a3ff
skyEra=FLAT flat=0x97a3ff optionActive=true
renderedSky=(0.5921568870544434, 0.6392157077789307, 1.0)      <- 0x97A3FF, daylight factor exactly 1.0
sky-noon=(0.5921568870544434, 0.6392157077789307, 1.0)
sky-midnight=(0.002960784360766411, 0.0031960783526301384, 0.004999999888241291)
sky-rain=(0.4471921920766367, 0.45895689725875854, 0.5491529703140259)
modernerBetaSky sampler=BiomeProviderBeta temperatureAtPlayer=1.8795123 betaFormula=0x70B1FF   <- retained, not drawn
```

Deployed: live jar sha256 `1356D8CE46152AF9…`, 712 entries, live hash independently compared equal to the tested
build. The owner must restart Minecraft completely.

# Earlier change — 2026-10-15 / the lighting options did nothing, and never stored anything

Owner: *"enabled and disabling old smooth lighting does not update in the game and it does not update even when
opening and closing the world neither does old lighting"*.

**Two defects, both in the config path rather than in the engines.**

1. **The write was discarded.** `HistoricalSmoothLighting.setToggle` and `HistoricalOldLighting.setToggle` both
   returned early unless the world being played was inside the engine's window, and `HistoricalConfigScreen`
   additionally greyed the row out. Mod Menu's config screen is normally opened with no world open at all
   (`activeVersion()` is null), so both the write and the click were refused - which is why nothing changed in the
   game and why reopening the world did not help: no value had been stored. The guard's stated reason ("a value
   stored in one world cannot silently apply in another") was unnecessary, because whether an engine is *used* is
   already decided by `activeNow()` per world.
2. **Old smooth shading never asked for a re-mesh.** Its rule is read while a chunk is meshed
   (`BlockBehaviour.getShadeBrightness`), so even a stored change stays invisible until chunks are rebuilt - and
   nothing requested a rebuild. Old lighting already had a relight queue; the smooth toggle now nudges it through
   the new `HistoricalOldLighting.requestRelight()`.

**The fix.** Both `setToggle`s now always store and persist (and the smooth one requests a relight when the value
actually changes). Every row on the config screen is clickable at all times, and its text states the preference and
where it applies, via the new pure `OptionScope` (`APPLIES` / `NOT_IN_THIS_VERSION` / `NO_VERSION`):
`Old lighting: ON - used by every version before Beta 1.8.1 (click to turn off)`. The old greyed variants
(`not_applicable`, `no_world`) are gone from `en_us.json`, and the third row (version name in corner) is treated the
same way, since it had the same shape.

**Evidence - the real config screen, driven by a client run** (`runClientQuickPlay -PquickPlayWorld=hexflash`, a
Beta 1.7.3 world where both windows apply; temporary probe, since removed; log kept at `run/lighting-probe.out`):

```
in world: version=…:beta_1_7_3 smooth(available=true toggle=true activeNow=true) old(available=true toggle=true activeNow=true) relightDrained=141
config opened row[0] active=true text=Old smooth shading (NT): ON - used by Beta 1.3 to Beta 1.7.3 (click to turn off)
config opened row[1] active=true text=Old lighting: ON - used by every version before Beta 1.8.1 (click to turn off)
clicked old smooth shading at 213,70 handled=true
after clicking the smooth row row[0] active=true text=Old smooth shading (NT): OFF - used by Beta 1.3 to Beta 1.7.3 (click to turn on)
clicked old lighting at 213,94 handled=true
after clicking the old lighting row row[1] active=true text=Old lighting: OFF - used by every version before Beta 1.8.1 (click to turn on)
still in the config screen: smooth(available=true toggle=false activeNow=false) old(available=true toggle=false activeNow=false) relightDrained=3981
back in the world: … relightDrained=5505
```

So each row is active, the click is handled, the label flips, the value the mixins read (`activeNow`) flips with it,
the re-mesh is requested and drained (141 -> 5505 sections - the smooth toggle previously requested *nothing*), and
the file ends up holding `oldLighting=false` and `oldSmoothLighting=false`. No crash.

**A note for the owner's expectation.** Old lighting's most visible effects are the grey lightmap (which differs from
the modern tint mainly on *block* light) and the round-robin re-light at dusk and dawn; at bright midday in a
brightly lit scene the difference is subtle even though it is applied. The measured facts above say the engine is
switched, not that every scene looks obviously different.

**New tests** (578 total, 0 failures): `client/config/OptionScopeTest` - the scope mapping, the two engine windows
against the registry (including that an unknown version is never "applies"), that every config row has text for every
scope, that the composed rows really take the `%s` note argument, and that the obsolete greyed-out keys are gone.

Deployed once the game was closed: live jar sha256 `DB2AC130E92E7685…`, 712 entries, live hash independently compared
equal to the tested build. The owner must restart Minecraft completely.

# Earlier repair — 2026-10-15 / the biome-conversion screen crashed the client on hand-off

The owner's client crashed at 10:54:03 with
`java.lang.NullPointerException: Screen cannot be null` from
`net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterTick`, affected screen
`net.historicalexperience.client.biome.BiomeConversionScreen`, after the log line
`Recorded biome conversion to biome_hierarchy_v4:beta_plains_only`. The world data was saved first - the integrated
server logged `All dimensions are saved` and the crash was client-side - so nothing was lost, and because the
conversion had already been recorded the world no longer owes one.

**Cause.** `BiomeConversionScreen.tick` called its hand-off runnable directly once the driver reported finished. That
runnable does `setScreen(null)` and then re-issues the world open, so `Minecraft.screen` became null while
`Screen.tick()` was still on the stack; Fabric's screen API fires its `afterTick` event against `Minecraft.screen`
once that method returns and was handed null. The path only became reachable now because the previous repair stopped
conversions running on every load, so a conversion only happens for an explicit upgrade - which is exactly the state
the owner's world was in.

**Fix.** `Minecraft.tell(whenFinished)` - the task is queued and drained by `runAllTasks` from the client's own tick,
never from inside a `Screen.tick()`. The failure button also moved out of `render` into `tick`: `render` walks the
widget list, and adding to it mid-iteration is its own crash.

**`execute()` is NOT deferred, and the first attempt at this fix was wrong.** Read from the 1.21.1 bytecode:
`BlockableEventLoop.execute` runs its task **inline** when called from the game thread (`ay()` is "not on the game
thread"), and only queues from another thread. `tell` (`method_18858`) adds to the queue unconditionally.

**Evidence - a real client run, A/B on the same fixture world.** A copy of the `world` fixture was given a revision-2
history entry naming a version upgrade and no conversion record, so it owed one exactly as the owner's did, and
quick-played with the project's own harness (`runClientQuickPlay -PquickPlayWorld=crashfix-probe`, game dir
`run/client`):

| build | conversion | result |
|---|---|---|
| first attempt (`execute`) | `4 region file(s), 338 pre-existing chunk(s)`, recorded | **crashed**: `Screen cannot be null`, `run/client/crash-reports/crash-2026-10-15_10.59.15-client.txt` |
| this build (`tell`) | `4 region file(s), 338 pre-existing chunk(s)`, recorded | `Player971 joined the game` - no crash |

Logs kept: `run/crashfix-probe.out` (crash) and `run/crashfix-probe-fixed.out` (fixed). The fixture world copy was
deleted afterwards; the original `world` fixture was never modified, and `run/client/saves` is back to its ten worlds.

Two harness lessons, both of which cost a run: `-PquickPlayWorld` only works with the project's own
`runClientQuickPlay` configuration, because it is that configuration which sets `runDir 'run/client'` - the generic
`runClient` runs with game dir `run`, so quick play looked for the world in the wrong saves folder and reported
"Failed to quick play". And killing the Gradle *wrapper* does not stop the client it launched: the orphaned dev client
kept running in the fixture world and re-saved it after the directory was deleted, which is how a world reappeared.
Stop the `KnotClient`/`devlaunchinjector` process itself, and only then clean up.

Build: 572 tests / 0 failures. Deployed twice: live jar sha256 `4B3B7252F1F91DAB…` with the crash fix, then
`B51AA219C671877C…` after the world-list notice stopped parsing the operation's summary by prefix (see the developer
mode section above). Both times the live hash was independently compared equal to the tested build. The owner must
restart Minecraft completely; their affected world needs no repair.

# Earlier feature — 2026-10-15 / "Developer mode" toggle, off by default

Owner: *"this screen this text and this text is developer and should only be shown if a toggle in mod menu called
developer mode is enabled you should add this and disable it by default regular users should not see this."* The three
surfaces shown were the staged-upgrade report screen, the version selector's list row (`emulates Release 1.6.4`) and
the selector's details block (`Current version: … / … - Release / Implemented. Emulates … / Generates with
historical_experience:release_1_6_4_no_modern_content`).

**The switch.** `client/config/HistoricalDeveloperMode` — key `developerMode` in this project's own client properties
file (`historical_experience-client.properties`), read through the same cache as the other client options, **off by
default**. Exposed as the fourth button on the Mod Menu config screen (`HistoricalConfigScreen`), labelled
`Developer mode: OFF (click to enable)` / `ON (click to disable)` (`historical_experience.config.developer.*`). Unlike
the three options above it, it is never greyed: it does not depend on which version is active.

**What it gates.**
* `HistoricalVersionScreen`'s details block and row second line, through the new `client/version/VersionText` — with
  it off a player sees the version name, its era, and `Implemented.` (or `Planned. <what is missing>`), and no recipe
  alias, revision number or measured build anywhere.
* `ManageWorldHistoricalVersion`'s result screen. With it on, the operation's own account is shown exactly as before.
  With it off, a player gets one plain sentence through vanilla's `AlertScreen` - "Version changed: <display name> will
  generate from the next time this world is opened. Existing chunks, builds and inventories are not changed." - and
  failures get the service's own summary, which is already plain English, with the world path and revision detail
  dropped. A successful change is still announced rather than performed silently: it does not take effect until the
  world is next opened, so silence would read as a failed click.
* `BiomeConversionScreen`'s target line, which printed the internal record label
  (`biome_hierarchy_v4:beta_plains_only`); with it off the world's name stands there instead and the progress bar,
  counts and file name are unchanged. This fourth surface was not in the owner's list but is the same kind of output.

`HistoricalVersionResultScreen` itself is unchanged and still reachable with the switch on.

**Evidence.** `gradlew.bat --console=plain build` with `JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1` exited 0 —
**572 tests / 0 failures / 0 skipped** (564 before this change). New `client/version/VersionTextTest` (7 tests) asserts
the negative case for **every** implemented catalogue entry - no `Emulates`, no `Generates with`, no
`historical_experience:` id in player-facing text - and that developer mode restores the recipe word for word. It also
records that `HistoricalVersionCatalogue.future()` is currently empty (every declared version is playable), so the
"Planned." branch is covered by a synthetically built entry and the test fails if a real planned version reappears.

**Verified in a real client run, both ways.** The project's own screen probe
(`runClientQuickPlay -PscreenProbe`, game dir `run/client`) photographed the version selector at 11:03 with the switch
at its default (the client config file had no `developerMode` key at all) and at 11:10 with `developerMode=true`
written into it:

* **OFF** - `run/client/screenshots/hex-selector-release.png`: the rows read `Release 1.2.5 / implemented` with no
  second line, and the details read `Release 1.6.4 - Release` / `Implemented.` - no `emulates`, no
  `Generates with historical_experience:…`.
* **ON** - the same screenshot path, rewritten: `Release 1.2.5 / emulates Release 1.2.5`, and the details read
  `Implemented. Emulates Release 1.6.4.` / `Generates with historical_experience:release_1_6_4_no_modern_content
  (revision …)`.

The probe config was restored afterwards (`developerMode` removed), and `en_us.json` was checked to parse and to
contain exactly the two keys the button asks for.

Also in this round: the world-list notice no longer takes the operation's summary apart on its prefix. It is built
from the chosen version's key through the catalogue, so renaming that sentence cannot silently produce a sentence with
a hole in it; the failure sentence is the service's own, which is already plain English. The world-list path itself
(the button, the selector, the notice in both modes) still has **not** been driven in-game - it needs GUI clicks that
no probe here performs - so the toggle's rendering on Mod Menu's config screen is likewise unphotographed: the row is
built with the same call as the three rows above it and its two translation keys exist and match.

**Deployed.** Live jar sha256 `B51AA219C671877C…`, 711 entries, live hash independently compared equal to the tested
build.

# Earlier repair — 2026-10-15 / the "experimental settings" prompt on every historical world

Owner: *"everytime I load a world with the mod installed this appears. get rid of it. also, make sure in vanilla
worlds the mod has absolutely 0 effect."* The screenshot was the client's `BackupConfirmScreen`: "Worlds using
Experimental Settings are not supported" / "…Here be dragons!" with **Create Backup and Load**, **I know what I'm
doing!**, **Cancel**.

**Cause, read from the 1.21.1 client rather than guessed.** `WorldOpenFlows.askForBackup(access, customized,
onLoad, onCancel)` is reached only when `WorldData.worldGenSettingsLifecycle() != Lifecycle.stable()`.
`LevelStorageSource` computes that as
`settings.dimensions().bake(levelStems).lifecycle().add(registryAccess.allRegistriesLifecycle())`, and the verdict is
formed in `WorldDimensions.bake`, which registers every dimension with `checkStability`. That reports `experimental`
unless the stem is *vanilla-like*: the overworld must use exactly `minecraft:overworld`/`minecraft:overworld_caves`
(ours is `historical_experience:beta_1_7_3_overworld`, height 128 — the era's envelope, so it can never match); the
Nether must carry vanilla's multi-noise Nether preset (ours is deliberately a fixed-biome one); the End passes.
`MappedRegistry.register` folds each entry's lifecycle into the registry's with `Lifecycle.add`, so one failing
dimension makes the whole world experimental. Consumers of the value in 1.21.1 are exactly two: that prompt, and the
server's crash-report "World Generation" line. (The world-list "Experimental" label is a different flag —
`FeatureFlags.isExperimental` over level.dat's `enabled_features` — and was never involved.)

**Fix.** `WorldDimensionsHistoricalStabilityMixin` overrides `WorldDimensions$Complete.lifecycle()` — the single
producer of the value — and returns `Lifecycle.stable()` only when the overworld stem is this mod's: a dimension type
in the `historical_experience` namespace, or a Moderner Beta biome source. The decision is in
`worldgen/HistoricalWorldStability.java`; `worldgen/HistoricalWorldStabilityTest` (4 tests) pins the negative cases,
including that a namespace merely *starting* with ours does not match and that a plain vanilla overworld is never
credited.

**Runtime evidence, two worlds, one process each (`-PenchantProbe`, temporary STABILITYPROBE lines since removed):**

| probe world | overworld dimension type | Moderner Beta source | vanilla verdict | with the fix |
|---|---|---|---|---|
| `level-type=historical_experience:beta_1_7_3` | `historical_experience:beta_1_7_3_overworld` | true | `Experimental` | `Stable` |
| `level-type=minecraft:normal` | `minecraft:overworld` | false | `Stable` | `Stable` (recognised=false, untouched) |

So the injection lands (its `require = 1` would have aborted the run) and the historical world's stored
`worldGenSettingsLifecycle()` is now `Stable`, which is the value the prompt reads. The prompt *screen* itself was not
photographed this round: the client path resolves the same value, and the owner can confirm by opening a world.

**"Absolutely 0 effect in vanilla worlds" — audited, and the guarantee is by construction.** A vanilla world
(`level-type=minecraft:normal`) logged `action=LEAVE_ORDINARY … treating it as an ordinary world and leaving it
untouched`: no metadata is written, no upgrade journal exists, and `HistoricalGenerationScope` is cleared, so no
historical structure/block rule applies to its generation either. Client-side presentation is driven by
`HistoricalClientRules.activeVersion()`, which is the *server's* classification in single-player and the server's push
in multiplayer (cleared on disconnect); for an ordinary world `HistoricalProfileLookup.profileIdForLevel` returns
`null`, so the colour maps, texture stack, tooltips, HUD and the NT-derived visual tweaks (`HistoricalWorldTweaks`,
all of which return the original when the version is null) are inert — which the owner's own log shows as
`Historical colour maps installed: none`. The one deliberate vanilla-namespace write remains the datapack tag
`data/minecraft/tags/worldgen/world_preset/normal.json`, which lists this mod's presets in the world-creation list;
that changes the creation screen, not any world.

Evidence: `gradlew.bat --console=plain build` with `JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1` exited 0 —
**564 tests / 0 failures / 0 skipped**, licence checks pass. Deployed to Prism; live jar sha256
`BE782A277CF4CFF5…`, 709 entries, build and live hashes independently compared equal. Both probe worlds and their
logs were removed and `run/server.properties` was left at `level-name=world`,
`level-type=historical_experience\:beta_1_7_3`.

Also this round: the dev-server probe harness was fixed after it reported "not launching" — the run had actually
failed to compile (`ServerLevel.dimensionTypeId()` is Yarn naming; the official accessor is
`level.dimensionType()`/`dimensionTypeRegistration()`), and the harness then waited for a `Done` line that could
never arrive. It now stops on `BUILD FAILED` too.

# Earlier repair — 2026-10-15 / explicit chunk-upgrade triggers and durable completion

Owner paused Indev work to fix unintended chunk upgrades on reopening an Infdev world and upgrade status resetting
across app restarts. The old open-world hook inferred an upgrade from biome palettes alone, including a fresh Infdev
world whose native placeholder differs from the Alpha mapping. The revised driver requires the canonical explicit
version-change entry already persisted by GenerationUpgrade in append-only metadata history. Initial establishment,
recipe-only changes, palette differences and mapping-label changes are not independent triggers.

Completion now records the specific metadata upgrade revision in the separate biome-conversion record (schema 3),
without modifying upgrade-owned world metadata. A later explicit version change can run even within the same biome
family; revisiting a family updates its chunk set instead of returning early with stale bookkeeping. Legacy records
use their persisted completion timestamp to avoid replay on migration. Failed completion writes are now failures,
not silently reported success; the screen offers Back instead of repeatedly reopening/retrying. Conversion holds the
world's vanilla directory lock. Empty worlds also persist completion.

Evidence: `gradlew.bat --console=plain build` with JAVA_HOME `C:\Program Files\Java\jdk-25.0.4.1` exited 0;
560 tests / 0 failures / 0 skipped, including 8 new trigger/worker/disk-persistence regressions. These tests cover
re-reading completion after resetting progress state, not a manual client quit/relaunch. Full client UI and gameplay
verification remain outstanding. No personal saves were changed, and previously misconverted chunks were not restored.

Deployment: `scripts/deploy-to-prism.ps1` succeeded; live jar SHA-256 prefix D46E313DBD9A73C4, 707 archive entries,
readability and full build/live hash equality verified by the script. Restart Minecraft completely to load it.
Indev implementation remains paused; its earlier scratch probe resources were left alone during this repair.

# Earlier feature — 2026-10-15 / historical biome tint, tooltips, mossy cobblestone, and licensing re-read

Owner's four requests this round: re-read the reworked licensing policy; extract the real biome/grass/foliage colourings
from the Alpha 1.2, Beta 1.8.1 and Release 1.0 clients and stop relying on Moderner Beta's; make the Alpha → Alpha 1.2
upgrade change the stored biome to Beta plains; and port Nostalgic Tweaks' legacy tooltips with the boundaries "nothing
before Beta 1.0, legacy from Beta 1.0, modern from Beta 1.9 Pre-release 2". Mossy cobblestone was added to the historical
texture exception as an extra.

## 1. Licensing — re-read and complied with (done, verified)

`docs/LICENSING.md` is the project licence account: the project is **LGPL-3.0-or-later**, incorporated Nostalgic Tweaks
material stays **LGPL-3.0-only**, and a file mixing both carries the compound
`SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only`. Three new NT-derived files were added to `NT_FILES` in
`scripts/apply-licence-headers.py` and the declared counts were corrected from 98 to 101 in `THIRD_PARTY_NOTICES.md` and
`docs/PROVENANCE.md` (both files state the count, and the compliance test checks both):

* `client/tooltip/HistoricalTooltips.java`
* `mixin/AbstractContainerScreenHistoricalTooltipMixin.java`
* `mixin/TooltipRenderUtilHistoricalMixin.java`

Evidence: `python scripts/apply-licence-headers.py --check` → *all 317 Java files carry the correct licence header (101
Nostalgic Tweaks-derived, 216 project)*; `*LicenceComplianceTest*` passes in the full build. The extracted colour-map data
is recorded in `docs/PROVENANCE.md` as **Mojang's values, extracted not authored**, with its source jars and hashes.

## 2. Historical grass/foliage colouring (done and verified in the real client)

**What was measured, not assumed.** The original clients store the tint lookup as a 256×256 `misc/grasscolor.png` /
`misc/foliagecolor.png`, which the client reads into a 65536-entry array with
`getRGB(0, 0, 256, 256, pixels, 0, 256)` and indexes with
`((int)((1.0 - rain * temp) * 255.0) << 8) | (int)((1.0 - temp) * 255.0)`. Extracted across the available clients
(`tools/ExtractHistoricalColormaps.java`):

* the **grass** map is byte-identical in every version from Alpha v1.2.6 through Release 1.0;
* the **foliage** map changes exactly once in that window: Alpha v1.2.6 and Beta 1.0 share the older map, Beta 1.2_02
  through Release 1.0 share the newer one. (Measured with Beta 1.2_02, 1.3_01, 1.4_01, 1.5_01 added to the survey.)

**How the user's "Moderner Beta is controlling the colouring via the biome" was confirmed.** MB's `GrassBlockTintSource`
and `FoliageTintSource` are registered block tint sources that call vanilla `GrassColor.get`/`FoliageColor.get` with its
own climate samples, and vanilla's own answers come from the 65536-entry `pixels` array that
`GrassColorReloadListener`/`FoliageColorReloadListener` fill via `GrassColor.init`/`FoliageColor.init`. So the one place
that decides the era's colour for *every* consumer is that table.

**Implementation.** `client/colour/HistoricalColormaps.java` loads the extracted tables; two mixins
(`GrassColorHistoricalColormapMixin`, `FoliageColorHistoricalColormapMixin`) substitute the era's table in `init`. No
per-block rule is needed. `HistoricalTextureService.tick` compares `desiredEraSignature()` against `appliedEra()` and
requests one reload when the era changes — the tables load during a resource reload, which happens before a world is
known. Whole ranges share a signature, so two adjacent versions inside one range never reload.

**Data.** `assets/historical_experience/colormap/*.bin.gz` (3 files, 74 KB total), regenerated by
`tools/GenerateHistoricalColormaps.java`; `README.txt` beside them records the source jar SHA-256s and the SHA-256 of
each uncompressed table. No image is copied and no original file is redistributed.

**Verified.** Unit: 6 tests in `HistoricalColormapsTest`, including exact cell values read back from the extracted tables
(grass `80B497`/`8EB971`/`47CD33`; Release 1.0 foliage `60A17B`/`71A74D`/`1ABF00`; Beta 1.0 foliage `60A17C`/`19BF00`).
Real client: a three-world cycle (Alpha 1.0.1 → Beta 1.7.3 → Release 1.0) logged the tables being swapped per join —
`grass+foliage_new` on join, `none` on leave — with `options.txt` unchanged and one reload per join.

**Open.** Alpha 1.0.x and Infdev had no colour map at all (the tint was literal renderer constants; `tools/ScanLegacyColourConstants.java`
was written to recover them and did not find them). No declared profile sits in that range, so nothing was invented and
the game's own colouring is left alone there. This is recorded as an open unknown rather than guessed.

## 3. Mossy cobblestone (done and verified)

Measured with `tools/ScanLegacyTerrainTiles.java` and `tools/MatchPackTextureToJar.java`, which compares a pack's 16×16
texture against each jar's `terrain.png` tile by mean channel distance:

* Plain cobblestone changes at **Beta 1.7**; Golden Days Base carries the pre-1.7 art, Programmer Art the 1.7+ art.
* **Mossy cobblestone changes later, at Beta 1.8** — a genuinely different boundary, so it needs its own rule.
* And the packs are paired the *opposite* way for mossy: **Golden Days Base is identical (distance 0.00) to the tile
  Alpha v1.0.11 through Beta 1.7.3 shipped**, while **Programmer Art is identical to Beta 1.8's**.

`HistoricalTextureRules` therefore gained `MOSSY_COBBLESTONE_CHANGE` (Beta 1.8) and two rules: Golden Days Base through
Beta 1.7, Programmer Art from Beta 1.8, each excluding the other so the `.mcmeta` follows the image. Unit-tested at both
boundaries plus the metadata companion, and the test asserts the two cobblestone boundaries differ.

## 4. Legacy tooltips (implemented, unit-tested, confirmed in game by the owner)

`client/tooltip/HistoricalTooltips.java` + two mixins, ported from NT's `CandyTweak.OLD_NO_ITEM_TOOLTIPS`
(`AbstractContainerScreenHistoricalTooltipMixin`, answering `Slot.hasItem()`) and `CandyTweak.OLD_TOOLTIP_BOXES`
(`TooltipRenderUtilHistoricalMixin`, which cancels `renderTooltipBackground` and fills NT's own geometry and default
colour `#000000C0`):

* before **Beta 1.0** → `ABSENT`, no tooltip drawn;
* **Beta 1.0 → Beta 1.9 Pre-release 1** → `LEGACY`, the flat black box;
* **Beta 1.9 Pre-release 2 onward** → `MODERN`, nothing substituted and the game draws its own.

Upstream exposes both as player settings with a global default; here the condition is the active version's era, matching
this project's rule that behaviour is gated per version rather than by a config flag.

**Verified.** Unit: 5 tests over the real registry, both boundaries, and the ordinary-world case. Real client: the owner
confirmed the in-game result after running the tooltip probe (`tooltipShots` in `TexturesProbe`, which hands the player a
stone pickaxe, opens the survival inventory, moves the real cursor onto a slot holding an item and photographs the
result). The probe's own automatic run was interrupted before it recorded a screenshot, so the photographic evidence has
not been filed — the confirmation is the owner's. `mixin/ContainerScreenAccess` was added for the probe to read which slot
is hovered.

## 5. Alpha → Alpha 1.2 biome rule — configuration, not chunk rewriting (corrected, owner-reported defect fixed)

**The first attempt was wrong and the owner caught it.** A chunk rewrite was built: it discovered chunk positions from
the region headers and rewrote the stored biome of each resident chunk through `fillBiomesFromNoise`. It was made
incremental (work per tick, progress in chat) after an earlier synchronous version froze the client. It demonstrably
worked on disk — a real 2652-chunk world went from 1260 chunks of `moderner_beta:alpha` and 0 of `beta_plains` to 419 and
**841** of `beta_plains`, read back from the region files by `scripts/region-biome-census.py`, with the block census
proving no terrain changed.

**And it changed nothing the game reported.** The owner's F3 readout still said `moderner_beta:alpha`. The reason is in
Moderner Beta's source: `ModernBetaBiomeSource.getNoiseBiome(biomeX, biomeY, biomeZ)` returns
`this.biomeProvider.getBiome(...)` for every position and **never reads the biome stored in a chunk**. On top of that the
Alpha preset's provider is `BiomeProviderSingle`, which returns one held biome unconditionally. So the stored value is not
what the game reports, and for this world the *provider's settings* are the only thing that decides the biome.

**The fix is the supported configuration path**, which is also what this project's own rules already prefer — "use
generation-time settings; use narrowly scoped hooks only where configuration cannot express per-world restrictions":

* `alpha_1_2_0_beta_plains` — a NEW revision of Alpha v1.2.0 (never an edit to the published `_no_modern_content`
  alias, which would move the terrain of existing worlds' ungenerated chunks). Its settings preset inherits
  `moderner_beta:alpha` unchanged for terrain and adds `"moderner_beta:single_biome": "moderner_beta:beta_plains"`.
* A matching world preset and the menu tag entry, so the build can actually be created through the GUI.
* All the chunk-rewrite machinery was deleted: `HistoricalBiomeConversion`, `HistoricalBiomeConversionTask`,
  `ChunkMapAccess`, their tests and the tick hook.

**Why it matters for this build specifically:** Alpha v1.2.0 is the Halloween Update, which removed the selectable winter
world mode in favour of biome-based weather, and the owner's rule for the upgrade into it is that the current biome
becomes Beta plains. `GenerationUpgrade` targets "a declared version's newest revision", so upgrading a world to Alpha
v1.2.0 now selects the Beta plains revision, and the biome follows from the provider with no chunk work at all.

**The entry gate is therefore moot.** The owner asked that the player not be let in until chunks are converted. Because
the biome now comes from provider configuration there are no chunks to convert, so there is nothing to gate and no
progress to show. The player is let straight in, as before.

**Verified statically, deliberately.** `Alpha12BiomeRuleTest` asserts that Alpha v1.2.0's newest revision names
`moderner_beta:beta_plains` as its single biome, that the revision keeps the `moderner_beta:alpha` foundation so terrain
is unchanged, that Alpha 1.0.1 carries no single-biome override, and that every declared revision alias has a preset file
of the same name. Asserting the preset's content *is* asserting the behaviour here, because the provider is the only
input.

**Measured in the real client before that:** Alpha 1.0.1 reported
`Biome at spawn -110, 66, 155 = moderner_beta:alpha (profile historical_experience:alpha_1_0_1)` from the spawn-biome
line the server now logs at startup.

**Still to confirm in game:** that an Alpha v1.2.0 world reports `moderner_beta:beta_plains`. Two attempts to produce
that world by hand failed *closed* — first because the copied world's level stem disagreed with its metadata, then
because the metadata's revision did not match its history — and each left the client on a fail-closed screen, which the
owner saw as a freeze. The natural test is the real upgrade path (Manage World → Alpha v1.2.0) on a throwaway copy, and
that has not been run since the preset fix. `<world>/historical_experience_metadata.json` records the revision; the hand
edit that failed set `revision` to 3 while `history` still ended at 1, and the loader rejects that mismatch by design.

## 6. Open question left with the owner: are the colour maps still wanted?

The owner said "don't change the colourings if changing the biome fixes it", then in the same message described the
colourings as something to apply retroactively to pregenerated chunks. These point in different directions, so nothing was
removed, and the distinction that matters is:

* **The colour maps decide the tint** — the actual green a grass block is drawn in. Substituting them is inherently
  retroactive, because the tint is resolved per block *at render time*, so pregenerated chunks are recoloured with no
  chunk rewriting at all.
* **A biome decides identity** — temperature, rainfall, what spawns, and which entry of the colour map is consulted.

Moderner Beta already places biomes sensibly (the owner's "colours biomes fine (ish)"), and its tint sources then read
whatever colour map the game loaded. So the two mechanisms do not overlap: dropping the colour maps would leave MB's
placement deciding the tint, and every pre-1.2 version would go back to modern green. Recommendation is to keep them;
say the word and they come out.

## 7. Golden Days Alpha boundary corrected (done and verified in the real client)

**The owner's rule:** the Alpha texture pack applies only to Alpha versions **below** Alpha 1.2. The boundary had been
`chronology(ERA_ALPHA, 1, 3, 0)` with the note that "the owner's rule extends it through 1.2.x"; it is now
`chronology(ERA_ALPHA, 1, 2, 0)` and exclusive, so Alpha 1.2.0 is the first build without it.

That boundary is right for two independent reasons, and both were already measured: the pack's `grass_block_top` and
`oak_leaves` are the Alpha v1.1.2_01 `terrain.png` pixels exactly (tiles 0, 52 and 53, distance 0.0), and those tiles
first change in Alpha v1.2.x; and Alpha v1.2.0 is the Halloween Update, which introduced biome-based weather and the
`grasscolor`/`foliagecolor` lookups, so the pre-1.2 art and the pre-1.2 colouring stop in the same build.

**Verified in the real client** by upgrading a scratch world from Alpha 1.0.1 to Alpha 1.2.0 through the normal upgrade
path:

| | Alpha 1.0.1 | Alpha 1.2.0 |
| --- | --- | --- |
| stack | `vanilla+programmer_art+continuation+golden_days_base+golden_days_alpha` | `vanilla+programmer_art+continuation+golden_days_base` |
| `grass_block_top` | `GOLDEN_DAYS_ALPHA` | `GOLDEN_DAYS_BASE` |
| `oak_leaves` | `GOLDEN_DAYS_ALPHA` | `GOLDEN_DAYS_BASE` |

## 8. Bug found and fixed: the biome override was written into the wrong preset

While verifying, the biome still reported `moderner_beta:alpha` for the Alpha 1.2.0 world. The cause was mine: an early
edit put `"moderner_beta:single_biome": "moderner_beta:beta_plains"` into
`alpha_1_2_0_no_modern_content.json` — a **published alias**, whose worlds recorded the Alpha biome — and the new
revision's preset was then created by copying that file, so both asked for Beta plains and neither stood out.
`alpha_1_2_0_no_modern_content.json` has been restored to the shape every other Alpha preset has (no single-biome
override) and the override now lives only in `alpha_1_2_0_beta_plains.json`.

`Alpha12BiomeRuleTest` did **not** catch this, and that is the useful part: it only asserted the override existed. It now
compares the two presets as parsed JSON and requires them to differ by exactly the single-biome setting, so an override
in the wrong file fails.

## 9. RESOLVED — why the biome rule appeared not to work

After the wrong-preset fix the biome at spawn for an upgraded Alpha 1.2.0 world was still `moderner_beta:alpha`, and
every candidate was checked and found correct, so none of them was the cause: `level.dat` named the beta-plains preset in
all three places the overworld generator records one; the metadata recorded the same preset; the preset inside the built
jar contained `"moderner_beta:single_biome": "moderner_beta:beta_plains"`; and Moderner Beta's own `alpha.json` declares
`provider: single` with `single_biome: alpha`, which `ModernBetaSettings.mapPreset` merges *under* the overriding file's
own components, so the override should win.

**The answer, found in section 10 by instrumenting the biome source: those checks were reading the wrong API.**
`ModernBetaBiomeSource.getNoiseBiome` reads the *provider* and correctly reported the new biome. `Level.getBiome(pos)`,
which is backed by the *chunk's stored biome data*, reported the old one. Both readings are real; they answer different
questions, and an upgraded world has them out of step until its stored chunks are rewritten. The rule was never broken —
it was being checked against the chunk-backed API while only the provider had been changed.

## 10. Beta 1.8.1 onward to `late_beta_plains` — preset side DONE, chunk converter still required

Owner's request: apply the Alpha 1.2 treatment again from Beta 1.8.1 to the latest Release so the biome reads as
`late_beta_plains`; "upgrade the beta plains to late beta plains and also if there are any other beta/late beta it should
do that"; AND build an extremely performant chunk converter so the stored chunks change too, with the player held out
until it finishes, a progress bar, and multithreading.

### What the diagnostic settled (this is the important part)

A diagnostic added to `HistoricalServerLifecycle` (`reportBiomeProvider`, which prints the resolved single-biome
component, the concrete provider class and the provider's own answer at spawn) settled the whole question in one run:

```
Moderner Beta biome source: provider=BiomeProviderSingle single_biome=<none>
  biome_at_spawn=ResourceKey[minecraft:worldgen/biome / moderner_beta:late_beta_plains]
Biome at spawn 100, 64, -229 = moderner_beta:beta_rainforest
```

Three conclusions, all measured:

1. **The preset change works.** The provider is `BiomeProviderSingle` and its own answer for the spawn position is
   `late_beta_plains`.
2. **The two biome APIs disagree.** `ModernBetaBiomeSource.getNoiseBiome` — the provider, which is what F3 reports —
   answers `late_beta_plains`. `Level.getBiome(pos)`, backed by the **chunk's stored biome data**, still answers
   `beta_rainforest`, because those chunks were written before the preset changed and no preset rewrites them.
3. **Therefore the owner was right and the chunk converter IS required.** The two have to agree, so both halves are
   needed. (This also means the earlier chunk rewrite was not useless, only insufficient on its own.)

### Done: the preset side

`scripts/set-late-beta-biome.py` applied to the 15 presets that worlds in that range generate from (Beta 1.8.1's
`cave_fix` and `no_modern_content`, every Beta 1.9 prerelease `no_modern_content`, and Releases 1.0 through 1.6.4):

* `biomeSettings` gains `"moderner_beta:provider": "moderner_beta:single"` and
  `"moderner_beta:single_biome": "moderner_beta:late_beta_plains"`. The provider had to be set as well as the biome,
  because from Beta 1.8.1 the inherited providers are fractal (`moderner_beta:beta_fractal`, `moderner_beta:fractal`) and
  a fractal provider ignores a single-biome setting. **This is what makes those worlds a single biome rather than a
  distribution** — the owner confirmed that is wanted.
* `caveBiomeSettings.single_biome` moves `beta_taiga` → `late_beta_taiga`, since the whole `beta_*` family is the
  pre-1.8 naming and none of it should survive into this range.
* Chunk settings and the terrain foundation are untouched, so terrain shape is unchanged.

`scripts/rename-beta-biomes.py` was written for the name-rename reading of the request and is kept, but it showed that
the project's own presets override only the cave biome; the main biome names live in Moderner Beta's inherited presets, so
the single-biome setting is the mechanism that actually reaches them.

### Still to do: the chunk converter

Required, per conclusion 3, and now known to be cheap: a 1.21.1 chunk stores biomes as a **palette-only container with no
packed data** (`data` absent, `palette: ['minecraft:plains']`), so a rewrite replaces a string per section rather than
re-packing indices. `scripts/inspect-chunk-biomes.py` shows this for any world.

Design to implement: read region files in worker threads with one region per thread, rewrite each section's biome palette
to the target, re-compress, replace atomically, aggregate progress into a bar, and hold world entry until it reports
finished. It must not run while a world is open, since it writes the files underneath it.

`RevisionInventoryTest` was added to print each version's revisions and the preset each names, so this work is driven by
the compiled registry rather than by reading the Java source by eye.

### Done: the chunk converter, verified

`scripts/convert-chunk-biomes.py`. It rewrites the biomes stored in a world's chunks with:

* **one worker per region file**, so regions convert in parallel (`--threads`, default one per CPU);
* a **live progress bar** — `[####----] 2652/2652 100.0% 1020 chunks/s 0s left 36008 changed`;
* **atomic replacement** — each rewritten region is built in a `.tmp` file and moved into place with `os.replace`,
  so an interrupted run leaves the original intact;
* **no-op safety** — a chunk already holding only the target biome is left byte-identical, so re-running is nearly
  free (measured: a second run reports 0 changed);
* only the `biomes` palette is touched, so terrain, entities and containers are never read or written.

Because the target name is a different LENGTH from the names it replaces, each NBT string's length prefix has to be
rebuilt, which is why it parses NBT rather than patching bytes in place.

**Measured on a real 2652-chunk Release 1.0 world** (a throwaway copy of `New World (3)`):

| | before | after |
| --- | --- | --- |
| biomes referenced | `minecraft:plains` 1392, `moderner_beta:beta_taiga` 1122, `early_release_swampland` 290, `minecraft:ocean` 290, `late_beta_extreme_hills` 238, … | **`moderner_beta:late_beta_plains` in 2652 chunks, and nothing else** |
| palette entries rewritten | — | 36,008 |
| wall clock | — | **2.6 s** (≈1,020 chunks/s) |
| block census | air 86.08%, stone 10.99%, water 0.97%, bedrock 0.66% | **identical to the source world, unchanged** |

Parsing bug worth recording, because it failed silently three times: a LIST payload parses to
`(item_tag, [items])`, and this repository's reader builds that pair as a single element, so `sections[1]` arrives as
`[item_tag, [items]]` — one level deeper than assumed. Iterating the wrong level yields zero changes and looks like a
correct no-op. `unwrap`/`item_value` now accept every shape seen, with the reason in their docstrings.

### Still to do: the entry gate

The owner asked that the player not be let into the world until the conversion finishes. The converter is a
region-file tool, so it must run with the world CLOSED — it cannot run behind the "Loading terrain" screen, because
the running server holds those files and would overwrite the result on its next save. Two honest options, and the
owner's call:

1. **Run it as a pre-launch step** in the launcher/profile, before the game starts, showing its own progress bar.
2. **Run it from inside the mod at world selection**, before the server opens the region files, blocking entry with a
   progress screen until it reports finished. This is the owner's literal request and is the next piece of work.

Option 1 is what the tool supports today.

### Done and END-TO-END VERIFIED: both halves together

The decisive run, on one upgraded Release 1.6.4 world, before and after the converter:

```
before  Biome at spawn 100, 64, -229 = moderner_beta:beta_rainforest
        Moderner Beta biome source: provider=BiomeProviderSingle ... biome_at_spawn=late_beta_plains
        ^ the provider and the stored chunk DISAGREED

after   Biome at spawn 100, 64, -229 = moderner_beta:late_beta_plains
        Moderner Beta biome source: provider=BiomeProviderSingle ... biome_at_spawn=late_beta_plains
        ^ both agree
```

So the rule now holds on both sides: the provider reports `late_beta_plains` and the stored chunks say the same.
The conversion on that world rewrote 36,008 palette entries across 2652 chunks in 2.6 s, and a re-run reports 0
changed.

### NEXT: the converter must live in the jar, and this is the design

**Owner's correction, and it is correct:** "You should not use a script bruh that's ABYSMAL for the user this is
supposed to be an end to end product it needs to be built into the jar and it should run a conversion step like before
the world even attempts to load via minecraft it should run conversion and then load and show the screen then."

So `scripts/convert-chunk-biomes.py` is a **development tool that proved the transform**, not the feature. The feature
is a Java converter inside the jar. Nothing about the mechanism changes — the Python version is the specification, and
it is measured (36,008 palette entries, 2652 chunks, 2.6 s, terrain byte-identical, re-run is a no-op). What changes is
where it lives and how the player meets it.

**Why the game must not be allowed to open the storage first.** `LevelStorageSource.validateAndCreateAccess(String)`
is what acquires the session lock and opens the region files. Converting after that means the running server holds
those files and overwrites the result on its next save, so the conversion has to complete *before* that call is
allowed to proceed — which is exactly what the owner asked for.

**The entry point.** `createWorldOpenFlows().openWorld(...)` → `LevelStorageSource.validateAndCreateAccess`. A mixin
on `LevelStorageSource` that intercepts `validateAndCreateAccess` is the single choke point; it is called on the client
thread, so returning early and resuming later is safe.

**The flow to build.**

1. On the client thread, read the world directory named by the argument and decide whether conversion is needed: the
   world's metadata `generationPreset`/`profileId` says it is in the Beta 1.8.1 → Release 1.6.4 range, and at least one
   region file still holds a biome other than `moderner_beta:late_beta_plains`.
2. If not needed, call the original method and load as normal — no screen, no delay. Ordinary worlds and worlds already
   converted take this path.
3. If needed, **do not call the original**. Instead set a progress screen and start the conversion on a daemon thread
   pool (one task per region file, mirroring the Python tool's parallelism), then return without opening the storage.
4. The progress screen polls an `AtomicInteger`/`LongAdder` of chunks done and total and draws a bar, the way the
   "Loading terrain" screen does.
5. When the pool reports finished, the screen re-issues the world load, which now takes path 2 and opens normally.
   Show the result line briefly ("Converted 36,008 biomes across 2652 chunks") before handing off.

**What to port, in order.** The NBT read/write pair, the region rebuild with recomputed sector offsets and preserved
timestamps, the biome palette rewrite, per-region worker parallelism, the progress counters, and the "already
converted is a no-op" guard. Vanilla already ships NBT (`CompoundTag`, `NbtIo`, `NbtAccounter`) and the region format
constants, so the parse and re-encode should use those rather than a hand-written pair — only the biome rewrite itself
is ours.

**A trap already paid for, so it is not paid twice.** A parsed LIST is `(item_tag, [items])`, and nested one level
deeper again inside a compound that is itself a pair: the Python version reported "0 changed" three times before the
shapes were handled. Whatever port is written must assert a non-zero expected count on a fixture world, because this
failure is silent and looks like success.

### DONE: the conversion is now inside the jar, gated before the world opens

**Owner's correction, and it was right:** "You should not use a script bruh that's ABYSMAL for the user this is
supposed to be an end to end product it needs to be built into the jar and it should run a conversion step like before
the world even attempts to load via minecraft it should run conversion and then load and show the screen then."
`scripts/convert-chunk-biomes.py` remains a development tool that proved the transform; the feature is now Java in the
jar.

**Where it hooks.** `mixin/WorldOpenFlowsHistoricalBiomeMixin` injects at the HEAD of `WorldOpenFlows.openWorld`, which
is the client's single entry point for opening an existing world and runs on the client thread *before* any storage is
acquired. When a world needs converting the hook cancels the call and shows `BiomeConversionScreen`, so the region
files are never handed to a server that would overwrite the work on its next save. That is the ordering the owner asked
for: convert, then load.

**The rest of the implementation.**

* `world/upgrade/HistoricalRegionBiomes` — the region-file rewrite: read the location table and the chunk bodies, decode
  each chunk through vanilla's own `NbtIo` with the compression scheme its header names, replace every biome palette
  entry, re-encode, recompute the sector offsets, preserve the timestamps, and write atomically through a temporary file
  and `ATOMIC_MOVE`.
* `world/upgrade/HistoricalBiomeConversionDriver` — one task per region file across a pool sized to the available
  processors, atomic counters for the screen to read from the client thread, and a `needsConversion` scan that decides
  by inspecting the biome palettes rather than trusting a stored flag.
* `client/biome/BiomeConversionScreen` — the progress screen: world name, `done / total (n%)`, a bar smoothed so it
  cannot jump backwards, the region file in flight, and a one-line summary when finished. Escape does not close it,
  because closing would leave the conversion running against a half-open world.
* Three language keys under `historical_experience.biome_conversion.*`.

**Verified end to end, in the client, from the jar.** A scratch world was upgraded to Release 1.6.4 so it was on the new
preset while its chunks still held the old biomes, then opened through the ordinary path:

1. **The owner saw the conversion screen**, which is the gate working: the world was held back rather than loaded.
2. The stored biomes were rewritten — the census then reported `moderner_beta:late_beta_plains` in all 2652 chunks.
3. The world loaded afterwards, and both biome APIs agreed:

```
Biome at spawn 100, 64, -229 = moderner_beta:late_beta_plains
Moderner Beta biome source: provider=BiomeProviderSingle ... biome_at_spawn=late_beta_plains
```

`HistoricalRegionBiomesTest` (4 tests) covers the rewrite on hand-built chunk tags. It exists because **this failure
mode is silent**: the same transform in the development tool reported "0 changed" three times while skipping every
section, and a no-op looks exactly like success. The tests assert a non-zero count for that reason, including the
already-converted case that must report zero.

**Known limit.** The scanning pass runs on the client thread before the screen appears, so a very large world has a
short pause before the bar shows. The conversion itself is parallel and off-thread; only the size scan is not.

### Three corrections from the owner, all verified

**1. The conversion started at the wrong build.** It ran only from Beta 1.8.1. It now starts at **Alpha v1.2.0**, which
is where biome-based colouring and weather actually arrive, so a world upgraded to that build is converted there rather
than eleven builds later. Beta 1.8.1 is the second boundary, where the *target* changes from `beta_plains` to
`late_beta_plains`, not where conversion begins. `targetBiomeFor(version)` returns the right one per version, and the
screen names the biome it writes rather than assuming.

**2. Upgrades from Infdev (or anything earlier) convert.** They already did in principle, because the hook runs on the
*destination* version and a world cannot be upgraded *to* Infdev - so there is no source-side case to write. What was
missing was that Alpha v1.2.0 was not a converting build at all (see 1), which is exactly the path an Infdev world
takes on its way forward. Verified: an Alpha 1.0.1 world upgraded to Alpha 1.2.0 had all 3212 chunks converted to
`moderner_beta:beta_plains`, and `Level.getBiome` at spawn agreed.

**3. A conversion now runs exactly once per boundary, not once per load.** This was the dangerous one. Deciding by
scanning the chunks was destructive: once a converted world generates chunks of its own, those hold the biomes its
generator produces - a distribution, not one biome - so the next load would see them, call the world unconverted, and
flatten them. That destroys precisely the variety the rule exists to keep.

`BiomeConversionRecord` records each conversion in the world's own append-only metadata history, in the same form the
version upgrades use:

```
rev3 [biome conversion] converted stored chunk biomes to moderner_beta:late_beta_plains
```

A conversion is owed only when the target this world's version needs has **never** been recorded for it. Written as
"has this target ever been done" rather than "is it the most recent", because a world can move between profiles in
either direction and a last-target record would consider an earlier one owed again on the way back. The chunk scan
survives only as the fallback for a world with no record at all.

### Two more corrections about generation

**Generation must not be flattened.** The single-biome settings were removed from every Beta 1.8.1-to-Release-1.6.4
preset, so those builds keep the multi-biome fractal providers they ship with. Measured on an upgraded Beta 1.8.1 world:
**8 distinct biomes across 81 samples** - desert, forest, ocean, river, and the four `late_beta_*` names - while the
chunks that already existed were converted. That is the owner's rule exactly: only chunks generated prior are affected.

**Alpha v1.2.0 takes Beta 1.1's biome generator**, because the owner's rule is that their generators are the same.
Beta 1.1 uses `"moderner_beta:provider": "moderner_beta:beta"` - climate-based, with a mapping table from climate to
biome - so Alpha 1.2.0 now generates plains, taiga, desert, forest, rainforest and the rest rather than one biome. Only
the *biome* settings are taken from Beta 1.1; the chunk settings stay `moderner_beta:alpha`, so terrain and the height
envelope do not move.

### Verified end to end

| what | result |
| --- | --- |
| Alpha 1.0.1 → Alpha 1.2.0 | 3212 chunks converted to `beta_plains`; `getBiome` at spawn agrees |
| Upgrade to Beta 1.8.1 | pregenerated chunks converted; generation still yields 8 distinct biomes |
| Second open of that world | **no conversion** - the record prevented it; still 8 distinct biomes |
| Metadata after | `rev3 [biome conversion] ...`; history intact, profile and preset untouched |

`BiomeConversionRecordTest` (4 tests) and `BiomeConversionTargetsTest` (3 tests) cover the run-once rule, the
both-directions case and the two boundaries. `HistoricalRegionBiomesTest` (4 tests) covers the rewrite itself.

## Build state at this handoff

`.\gradlew.bat --console=plain build` (JDK 25) → **exit 0, 499 tests, 0 failures**. `python scripts/apply-licence-headers.py
--check` → all 329 Java files carry the correct licence header (101 Nostalgic Tweaks-derived, 228 project). The jar
carries the three colour-map resources, the mixin classes and `META-INF/{LICENSE,COPYING,COPYING.LESSER}`.

The Alpha v1.2.0 biome rule is now data, not code: `alpha_1_2_0_beta_plains` in the version registry, its settings preset,
its world preset, and the world-creation menu tag entry. `Alpha12BiomeRuleTest` holds those four in agreement — the same
way the sandbox history in this project has always guarded a preset, and for the same reason: the alias and the JSON file
of the same name must both exist, and a test catches it when they do not.
# Previous feature — 2026-10-14 / automatic historical textures (Programmer Art + Continuation + Golden Days)

Goal `goal-341dba9d…` is active. **Target note:** the brief says Minecraft 26.3; this repository's only target is 1.21.1
(AGENTS.md: the 26.3 tree was deleted at the owner's instruction), so all of this was built and verified on 1.21.1.
Research, mappings, boundary evidence and provenance: `research/historical-textures.md`.

**What exists (all under `net.historicalexperience.client.textures`, plus one mixin):**

* `HistoricalTextureRules` / `TexturePlan` / `AssetRule` / `VersionWindow` / `TextureSource`: the pure, tested policy. The
  stack is a function of the registry's chronology key (never of names); per-asset rules (XP bar pinned away from Golden Days,
  cobblestone Golden Days through Beta 1.6 then Programmer Art) express exclusion, preference, vanilla pin and mcmeta-follows-image.
* `PackRepositoryHistoricalTexturesMixin` + `HistoricalTexturePackResources`: the whole integration. One synthetic pack is
  appended to `PackRepository.openAllSelected()` for the client's own repository; the selected list and `options.txt` are
  never touched, and it outranks user packs only for the resources the plan defines.
* `PackScanner` / `PackClassifier` / `PackFiles`: detection by metadata + namespaces + credits, ZIPs and folders, any file
  name, duplicates, wrong variant, unsupported format. Read-only.
* `PackPin` / `PackDownloader`: pinned Modrinth CDN downloads, SHA-512 verified, size-bounded, atomic, off-thread, into the
  mod's own folder. Consent is explicit (setup screen button, or the stored opt-in).
* `HistoricalTextureService`: one reconcile that compares desired against applied signature and reloads only on a change.
* `TextureSetupScreen` (first run, once, versioned marker), `TexturesConfigScreen` (reached from the existing options screen,
  therefore from Mod Menu), `TextureFirstRun`, `TexturesProbe` (opt-in verification).

**Boundaries (evidence in the research note):** Golden Days Alpha through Alpha 1.2.x (its grass/leaves art is exactly the
pre-1.2.0 art; the 1.2.x change is at tiles 0/14/52/53/...); Golden Days Base through Release 1.2.5; cobblestone changes at
Beta 1.7 (tile identical Alpha 1.0.11 to Beta 1.6.6, Golden Days Base = old tile, Programmer Art = new tile).

**Verification, with what each level does and does not show:**

* Unit/integration: `gradlew.bat --console=plain build` exit 0, **460 tests, 0 failures** (49 new across rules, scanner,
  composition, downloader, no-bundled-assets guard). They test logic against synthetic in-memory/temp packs.
* Real client (1.21.1, `-PtexturesProbe`, evidence in `run/client/logs/tex-*.txt`): the probe asks the live
  `ResourceManager` which pack served each resource and hashes the bytes. Results:
  * Alpha 1.0.1: cobblestone = Golden Days Base (`5d142ff1…`), grass/leaves = Golden Days **Alpha**, XP bar = vanilla.
  * Beta 1.7.3 / 1.8.1 / 1.9 pre-3 / Release 1.0: cobblestone = Programmer Art (`d3b21a63…`), others Golden Days Base, XP bar vanilla.
  * One session joined Alpha, Beta 1.7.3, Beta 1.9 pre-3, Release 1.0 in turn: each JOINED state correct, every LEFT state back to
    vanilla, 1 reload per join and 1 per leave, final title state `none`, `options.txt` byte-identical throughout.
  * A synthetic user pack overriding cobblestone/stone/dirt/XP-bar: with enforcement ON the user pack served none of the
    enforced resources; with it OFF it served all of them; ON again restored enforcement. `options.txt` unchanged.
  * A genuinely ordinary world (fresh vanilla world): no layer, no reload, everything served by vanilla.
  * First run from the title screen: the setup screen appeared on its own, listed the 4 packs, and with an empty pack folder
    the real pinned download fetched all three; their SHA-512 equal Modrinth's and the owner's installed copies. Declining
    downloads nothing: the managed folder was never created.
* **Committed version upgrade (round 2):** on a scratch copy of the Beta 1.7.3 test world the probe called
  `HistoricalUpgradeService.upgradeUnloadedWorld` (the Manage World button's own service) to Release 1.3, then opened it.
  Before: Golden Days Base + Programmer Art cobblestone. After the upgrade: stack `programmer_art+continuation` only, grass
  and dirt back to vanilla/Programmer Art, `oak_leaves` and `stone` now Programmer Art, cobblestone still Programmer Art, XP
  bar still vanilla, one reload on the join. The original `hexflash` world was untouched (still `beta_1_7_3`) and the scratch
  copy was deleted afterwards.
* **Mod Menu through the real loader (round 2):** the probe discovered the `modmenu` entrypoints via `FabricLoader`, found
  `hex1211`, built its config screen (`HistoricalConfigScreen`, which contains the Historical Textures button) and opened
  Mod Menu's own list, where the mod appears. A second client launched with `-PwithoutModMenu` (Mod Menu absent from the
  mod list, 56 mods, `hex1211` present) started normally, joined Beta 1.7.3 and built the full layer: Mod Menu is optional.
* **Missing packs, downloads declined (round 2):** with setup completed, `historicalTexturesAutoDownload=false` and an empty
  pack folder, joining Beta 1.7.3 showed exactly one toast naming the missing packs, started 0 downloads, never created the
  managed folder, and still built a working layer from what exists (`opened=[VANILLA_PIN, PROGRAMMER_ART]`).
* Screenshots (`run/client/screenshots/tex-probe-*.png`) were viewed for the setup, both config screens and the Mod Menu list;
  precedence is proven by pack ids and hashes, not by them.

**Not verified / known limits (do not read the above as more than it is):**

* **Multiplayer is out of scope by the owner's decision** and was not exercised. The service reads
  `HistoricalClientRules.activeVersion()`, which already uses the existing server-push handshake; an unknown version means
  "no layer", which is the safe fallback.

* **Not visually compared against the original games.** Textures in the game are the packs' own; this proves which pack
  serves what, not that Golden Days Base matches a given Beta build pixel for pixel.
* **Golden Days Alpha is pre-1.2.0 art** extended through 1.2.x by the owner's rule; Alpha 1.2.x grass/leaves therefore show
  1.1.x art. Recorded in the research note, not hidden.
* **Mossy cobblestone** is not part of the cobblestone exception (it changed in Beta 1.8 and the brief names only cobblestone).
* Not deployed to the live Prism instance: the running owner game holds the jar (see the sky section).
* The repo's `Repeat texture-pack scan` diagnostic stays in `rescan()`; the original 13-scan burst came from an unsynchronised
  first call, now fixed (one scan per session in the later runs).

# Previous repair — 2026-10-14 / the Beta sky colour: NT's palette was wrong, the clients were right

- **The owner's correction is what closed this.** Nostalgic Tweaks' Old Universal Sky Colour is not the Beta sky:
  its Beta entry is a flat `0x97A3FF`, and the Beta clients never used a flat sky colour. Time previously spent
  comparing our render against NT was spent against the wrong reference.
- **Extracted from the original clients themselves**, decompiled into `.upstream/legacy-decompile/`:
  - Alpha v1.0.11: sky colour is a field on the world object, `8961023 == 0x8BBDFF` (`a1011/cj.java:20`, applied at
    `cj.java:868`). NT's Alpha value was the one entry that was right, and Moderner Beta's `ALPHA_SKY_COLOR` agrees.
  - Beta 1.0, Beta 1.7.3 and Beta 1.8.1: all three call `getSkyColorByTemp(temperature)`, i.e.
    `Color.getHSBColor(0.62222224F - t*0.05F, 0.5F + t*0.1F, 1.0F)` with `t = clamp(temperature/3, -1, 1)`
    (`b10/dd.java:952`+`gq.java:96`, `b173/fd.java:927`+`kd.java:126`, `b181/rv.java:997`+`vh.java:106`). Reachable
    range `0x9C99FF` cold to `0x66BDFF` warm, `0x80A1FF` neutral. The temperature is the world's climate noise
    (`b173/xv.java:30`), which is what Moderner Beta's own `SkyColorSampler` reproduces.
  - Beta 1.9 Pre-release 6 (`b19pre6/uy.java:1015`) and Release 1.0 (`10/ry.java:1015`) switch to the biome
    temperature at height 64 - the same HSV function modern Minecraft still calls from
    `OverworldBiomes.calculateSkyColor`. From Beta 1.9 the game's own colour is already right, so nothing is
    substituted there; this moves the owner's "Beta until 1.7.3, then modern" line to where the clients changed.
  - Infdev: NT's `0xC6DEFF` is uncited; Moderner Beta's extracted `INFDEV_*_SKY_COLOR` is `0x99CCFF` and is now
    used, recorded as the weaker provenance because no Infdev jar exists locally.
- **Beta's rounding, not modern's, is ported.** `java.awt.Color.HSBtoRGB` rounds (`+0.5F`); `Mth.hsvToRgb`
  truncates, so modern differs by one unit at most temperatures (`0x80A1FF` against `0x7FA1FF` neutral). Runtime
  evidence shows both: Moderner Beta's own sampler returns `0x70AFFF` where our port returns Beta's `0x71AFFF` for
  the same temperature. Reproducible table: `java research/tools/SkyTable.java`.
- **Implemented** in `HistoricalSkyColours` (extracted values + Beta's function), `HistoricalWorldTweaks.SkyEra`
  (Infdev/Alpha flat, Beta 1.0-1.8.1 temperature, nothing from Beta 1.9), and `BiomeHistoricalSkyColourMixin`, which
  now feeds Beta's function the climate temperature from Moderner Beta's sampler rather than inventing a second
  generator. NT's hook point and its low-render-distance fog branch are unchanged.
- **Verification.** `gradlew.bat --console=plain build` exit 0; **408 tests, 0 failures** (8 in the world-tweaks
  suite, including new pins on the extracted colours and on Beta's rounding). Dev probe
  (`logs/nt-visual-client7.log`, exit 0) on the Beta 1.7.3 test world: `skyEra=TEMPERATURE flat=none
  optionActive=true`, temperature at player `1.7115993`, our colour `0x71AFFF`, `biomeBaseSky=0x71AFFF`,
  `renderedSky=(0.44313726, 0.6862745, 1.0)` — noon, so the daylight factor is exactly 1.0. Midnight and rain
  samples still show the level's own modulation on top, which is what the original clients did too.
- **Known gap, stated rather than papered over:** Alpha v1.1.x/v1.2.x used the same temperature form
  (`a126/cy.java:948`), but Moderner Beta exposes no Alpha sky-temperature sampler, so those profiles keep the
  Alpha v1.0.x flat colour (the value Moderner Beta gives its Alpha biomes).
- Not addressed here and still open: the stars probe still stalls on its synchronous disconnect inside a tick, and
  the wider fog/void/blue-void/2D-item/crafting-screen/config work is unchanged by this repair.

# Previous repair — 2026-10-14 / NT visual defects (superseded in part by the sky finding above)

> Superseded detail: the bullets below describe the port as it stood while NT's palette was still trusted. Its Beta
> entry `0x97A3FF` is not the Beta sky (see the section above); the flat-colour probes recorded there measured a
> constant that has since been replaced by the era's own temperature function.


- Owner confirms chests now look correct. Leave appearance intact; ordinary-world block-model/particle sampling remains a documented isolation caveat.
- Sky acceptance is STILL OPEN. Owner identifies NT **Old Universal Sky Colour**, not blue void or fog. Source traced directly to `CandyTweak.UNIVERSAL_SKY_COLOR`, `SkyHelper.getOldColor`, and `world_sky/BiomeMixin`: Infdev `0xC6DEFF`, Alpha `0x8BBDFF`, Beta `0x97A3FF`, disabled preserves original. Replacement hook changes `Biome.getSkyColor`, not the final sky vector; added missing NT low-render-distance shader-fog branch. Releases/Beta1.8+ retain modern base per owner direction.
- NT old-night minimum port is now separate, preserving time/weather math. Old world-fog distance/shape and pre-Beta1.8 sunrise-fog tint suppression are a bounded additional slice; full NT colour/transition/void algorithms remain pending, not claimed implemented.
- Recipe book now hides/deactivates the actual ImageButton in inventory/crafting/furnace init, besides hiding the panel. Final dev probe confirms InventoryScreen/CraftingScreen/FurnaceScreen each have zero visible image buttons; screenshots retained. First inventory probe incorrectly opened creative UI; corrected probe temporarily selects local survival presentation only and restores it.
- Corrected clouds to 108 before Beta1.8,128 afterward for declared historical releases; no-cloud NaN retained. Corrected 2D-item gate to include published1.4.2 profile (renderer still pending). Left-click ends at release_1_4, not1.5; handler now resolves the authoritative server profile without importing client classes.
- Removed orphan Classic lightmap function, moved client shading mixin out of common array, and cached new client option reads (no per-frame file reads). Compared four allegedly corrupted lighting rewrites against `0f17f4c`: actual diffs are the intended mode removal plus comments/imports, not letter corruption.
- Build after direct sky/fog changes: `gradlew.bat --console=plain build`, exit0; independently counted404 tests,0 failures,0 errors. Earlier first build failed on two nonexistent1.8.9 test-profile references, corrected before acceptance.
- Final dev probe (`logs/nt-visual-client-final.log`) exit0: Beta base/noon RGB match `0x97A3FF`; midnight RGB `(0.00296,0.003196,0.005)`; rain RGB `(0.44719,0.45896,0.54915)`. This verifies downstream environment modulation, not owner's visual acceptance.
- Deployment SUCCESS: `pwsh -NoProfile -File scripts/deploy-to-prism.ps1`, exit0,707KB,593 readable archive entries,SHA256 prefix `5B17981A4556F033`. Only Prism1.21.1 HE instance modified. Existing running owner game must restart; no game processes killed. Sky remains awaiting owner comparison after restart.
- Dev probe2 (`logs/nt-visual-client-second.log`) exited0 and observed Beta1.7.3 base `0x97A3FF`, matching rendered daytime RGB; this is NOT visual fidelity acceptance. Screenshots inspected: crafting/furnace button absent, cube chest drawn. Latest live Prism jar at20:12 remains hash `F8B1E0DCCC55FE26...`, contains the old final-vector sky hook, not the replacement biome hook. Do not mistake dev verification for deployment or claim stale deployment is the sole underlying defect.
- Reproduce current opt-in probe: `gradlew.bat --console=plain runClientQuickPlay -PntVisualProbe -PquickPlayWorld=hexflash`. It operates only on project test save, writes `run/client/logs/nt-visual-probe.txt` and screenshots, and closes through Minecraft normally. No owner game process is stopped.
- Remaining requested NT list: stars geometry/brightness, sky/nether effect completion, full fog/dark/dynamic/nether/cave/void/creative algorithms, blue void, water lighting, 2D item rendering, old crafting screen and config controls. Gates alone are not ports. Chest texture provenance remains unresolved; earlier categorical legal claims corrected.

# Previous milestone — integrated historical-version UI

## UI repair — historical-version buttons no longer float over screen content

- Create World: replaced the fixed bottom-left overlay with a real vertical footer layout containing the historical-version row and vanilla's original horizontal Create/Cancel row. Footer height is reserved through vanilla's own HeaderAndFooterLayout, so the separator and tab content account for the extra row and resizing follows the native layout.
- Select World: historical version occupies a centred 308-pixel footer row matching vanilla's controls. The actual world-list viewport (including scrolling, separator and hit testing) is shortened by 24 pixels to reserve that row. The action is disabled until a world is selected.
- No generation, upgrade service or world-storage behaviour changed.
- Reproducible menu-only verification: `./gradlew.bat runClientQuickPlay -PscreenLayoutProbe --console=plain`. Opens no save; captures both host screens, all three creation tabs and return-to-screen at GUI scales 1/2/3 in a 960x720 window, logs visible-widget overlap/bounds/duplicate checks to `run/client/logs/screen-probe.json`, and exits gracefully. Screenshots are under `run/client/screenshots/hex-layout-*.png`.
- Initial build: `./gradlew.bat build --console=plain`, exit 0; **376 tests, 0 failures**. Initial 854x480 client probe: exit 0, all nine host/return layout checks passed (GUI 3 was clamped to 2 by vanilla at this window size). Final 960x720 client probe: exit 0; **15/15 checks passed**, exactly one historical button and zero overlaps/out-of-bounds at effective GUI sizes 960x720, 480x360 and 320x240. Visually reviewed the compact World tab and Select World screenshots. Final artifact: `build/libs/historical-experience-1211-0.1.0.jar`. No external launcher instance was modified.

# Previous milestone — 2026-10-13 (14) / the Nostalgic Tweaks worklist: nine of ten in, one held on evidence

## Round 79 — the owner's ten-item NT list, worked through

The owner named ten Nostalgic Tweaks behaviours to port, each "in the versions they apply to". All boundaries are now
established from primary sources — NT's own catalogue marks every one of them `unknown` and its `historicalClaim` field
says a name alone is not evidence — and the work is done except for one item held deliberately.

| Requested | Status | Boundary and source |
| --- | --- | --- |
| disable axe stripping | **ported** | 1.13 (17w47a), wiki Axe — later than all 32 declared builds |
| disable shovel pathing | **ported** | 1.9 (15w31a), wiki Shovel — later than all 32 |
| prevent swimming | **already implemented** | the `swim` gate in `LivingEntityHistoricalMovementMixin`, added with the earlier NT movement batch |
| instant air refill | **ported** | NT swimming group, pre-Beta-1.8 |
| instant eat | **already implemented — and it IS upstream's** | see below |
| instant bow / arrow speed / invincible bow | **ported** | pre-Beta-1.8; wiki Bow: *"1.8 Pre-release: Arrows can no longer be rapid-fired from bows; they must be charged first."* |
| old pig / cow / sheep drops | **already implemented** | `HistoricalMobLootHelper` is already the direct port of NT's `MobLootHelper` |
| old Nether lighting | **HELD** | NT's code contradicts its own name — see below |
| old smooth lighting | **ported (corner shading)** | Beta 1.3 → 1.8; the lightmap curve is still a recorded gap |
| disable smooth lighting | **ported** | before Beta 1.3 |

**The classic lighting engine was reinstated** at your request after round 16 withdrew it — restored **verbatim** from
`06a9bb9` rather than rewritten, so its verified reasoning survives. Its boundary was already the Beta 1.3 you named,
and the wiki corroborates it: *"1.3: Smooth lighting engine added with the help of MrMessiah and his 'BetterLight'
mod"*, *"1.8 Pre-release: Implemented a new lighting engine."*

### Two of your items were already done — said plainly rather than re-implemented

- **Instant eat.** You asked for this "overriding your own in-house instant eat". What is in the tree **is** NT's
  implementation: `LivingEntityHistoricalInstantEatMixin` and `MinecraftHistoricalInstantEatMixin` are the direct port
  of NT's `food_health` group, and their own file headers name the upstream file. There is nothing in-house to
  override.
- **Mob drops.** `HistoricalMobLootHelper` is already the full port — all fourteen entity mappings, the looting-aware
  count, the on-fire smelting, and the sheep case. **Your sheep point is already handled**: that branch is hoisted
  *above* the version gate precisely because sheep were still dropping raw mutton, a Release 1.8 item.
  Two narrow gaps are recorded rather than patched: no profile carries a **pig** rule, and no pre-1.8 profile states a
  chicken rule.

### What "old smooth lighting" actually is, since the name misleads

Not a different lighting model. The two engines differ in **two specific places**, and one of them is now implemented:
the modern engine darkens a block's **corner** towards `0.2`, and the 1.3–1.7.3 engine did not darken it at all. The
other is the **lightmap curve**, recorded as a known gap rather than claimed.

The two windows are **disjoint by construction** — the classic engine is true *before* Beta 1.3, the MrMessiah engine
from Beta 1.3 to *before* Beta 1.8 — so no version can be given both engines or neither. A single "old lighting"
boolean would have got exactly that wrong.

### The one held item, and why

**`OLD_NETHER_LIGHTING` is not implemented, because upstream contradicts both its own name and the wiki.** NT's
`LightTextureHelper` reads `isNether = dimension == NETHER && !OLD_NETHER_LIGHTING` and
`forceBrightness = isNether ? 7.0F : 15.0F` — so with the tweak **enabled** the Nether is treated as *not* Nether and
gets **15**; with it disabled it gets **7**. The tweak *brightens* the Nether. The wiki says the opposite: *"v1.2.0
preview: Added the Nether, where light decreases by 10% each level, rather than the normal 20%"* — and 10% per level is
**dimmer**, not brighter.

The two possible implementations look **opposite on screen**, so picking one would be a guess dressed as a port. It
also cannot be verified here: the effect is a client lightmap value with no server-visible consequence, so the runtime
probe cannot see it and it would ship on your eyes alone. That is not a reason never to do it — your play is a real
harness — but it is a reason not to do it on contradictory evidence without saying so first.

**Say which you want** and I will implement that one: the **dimmer** Nether the wiki describes, or the **brighter** one
NT's code produces.

**374 tests, 0 failures.** Deployed:
**0A0DC2E3FE278BFC23975923BA91D6932D4061B2F53C2825B4DA8B80283BD907**.

**To see it:** restart fully. In a **Beta 1.7.3** world an axe should not strip logs and a shovel should not make paths;
a bow should fire on click without charging and never lose durability; air should refill instantly on surfacing. In an
**Alpha** world smooth lighting should be off; in **Beta 1.7.3** corners should be undarkened.

## Round 78 — the owner's damage audit found swords and shovels wrong

The owner asked for the tool and weapon damage values to be verified against the wiki per version. That check found
**two live defects**, in data this project has repeatedly described as matching the research exactly.

### Every sword was three points too strong

The constant was `b+6`. 1.21.1's own raw sword modifier is **`b+3`** — read out of the jar, not inferred. The wrong
value happened to give the right total for exactly **one tier**: the diamond sword, because `6+3 = 9` matched the
pre-Pre3 figure. Every other sword dealt three too much — a wooden sword **7** where the wiki says **4**, a stone
sword **8** where it says **5**.

That also explains why the old probe never caught it: it **printed** seven sample items and asserted nothing, and the
one it displayed most prominently was the correct one.

### Every shovel was one point too weak

The constant was `b+0.5`. The era total is **1.5** — the only non-integer figure in the table — which is also what
1.21.1 gives them.

**Axe and pickaxe** were corrected in the same pass to `b+3` (total 4) and `b+2` (total 3). The rule that makes these
totals rather than bonuses: before 1.9 an item's damage **replaced** the bare hand's, and **13w21a** moved sword, axe
and pickaxe to **add** to it — documented per item as adding exactly 1.

### Swords before Beta 1.9 Prerelease 3, where the wiki gives exact figures

| Tier | Wiki | Total before → after |
| --- | --- | --- |
| Diamond | *"Reduced the damage from 10 to 7"* | 10 → 7 |
| Iron | *"Reduced the iron sword's damage from 8 to 6"* | 8 → 6 |
| Wood, gold | *"except for wooden and golden ones"* — **exempt** | 4 → 4 |
| Stone | **inferred** 5 from the shape of those | 5 → 5 |

The old code applied a flat **+3 to every sword** — right for diamond, wrong for all the rest, and wrong by three for
the two the wiki explicitly exempts. **Stone is the one inference** and is recorded as inferred, not sourced.

### Verified, and this time it asserts

**24 items — every tier of sword, axe, pickaxe and shovel — across three versions** bracketing both sword eras
(`beta_1_8_1` pre-nerf; `beta_1_9_pre_3` and `release_1_6_4` post-nerf) all report **"NONE — every tier matches the
wiki"**. Confirmed era totals: wooden sword 4, stone 5, iron 8, diamond 10 pre-nerf and 7 post; wooden axe 3 →
netherite 7; wooden pickaxe 2 → netherite 6; wooden shovel 1.5 → netherite 5.5.

**Hoe is correctly untouched**, now for a sourced reason: 15w34c *"Stone, iron and diamond hoes now do slightly more
damage than an unarmed attack"*, so before it every hoe was 1 damage in every tier — which is what 1.21.1 gives them,
so there is nothing to rewrite.

### The checker was wrong twice, and that is the transferable lesson

Three mistakes were made in the **verification** while checking the product, and two looked exactly like product
failures:
- name matching tested `contains("axe")` before `contains("pickaxe")` — and **"pickaxe" contains "axe"** — so every
  pickaxe was checked against the axe base and reported broken while the code was right;
- the tier bonus was derived from the item and produced **self-contradictory output** (a row claiming `tierBonus=1`
  beside an expectation that only holds for 0), which sent the diagnosis after the product for several rounds;
- an early attempt asserted on `ItemAttributeModifiers.compute()`, which **also folds in attack speed** — that is
  where the "modern wooden sword = 1.6" reading came from, which is an attack-speed figure, not a damage one.

A wrong checker is indistinguishable from a wrong product, and this project has now spent real rounds on each.

**A unit test now pins what can be pinned offline** — the Pre3 boundary across all 32 declared versions, and the
per-tier figures. The per-item table **cannot** be a unit test: `Items.<clinit>` initialises the registry, so
constructing an `ItemStack` dies with `ExceptionInInitializerError`. Third time this project has hit that wall, so
the table lives in the runtime probe.

**369 tests, 66 suites, 0 failures.** Deployed:
**65E776C623824F1842C28C64CD96344E64624CF6068BC87241D235BDC7FDCF3B**.

**To see it:** restart fully, then hit a mob with a wooden sword in a **Beta 1.8.1** world — 4 damage, not 7 — and
compare a diamond sword between a **Pre2** world (10) and a **Pre3** world (7).

## Round 77 — the full matrix is clean, and the owner closed the Nether item

**A clean full 32-version sweep against the current build — one sweep at a time, nothing edited during it:**

| Check | Result |
| --- | --- |
| versions | **32 / 32** `success=true`, `presetMatches=true` |
| anachronistic block kinds | **0 in every version** |
| mixin failures | **0** |
| timeouts / server startup failures | **0** |
| cost | 686 s total, **21.5 s mean** (min 18.2, max 30.2) |
| the two re-pointed revisions | `beta_1_0 -> beta_1_0_no_sandstone`, `beta_1_2 -> beta_1_2_no_sandstone`, both 0 anachronisms |

That is the strongest single piece of evidence this project has produced: every declared profile loads, resolves its own
identity, applies its inherited preset, and generates **nothing its era lacked**.

**The Nether item is CLOSED.** The owner reported *"nether is fine as of right now"*, which is the acceptance coverage
item 12 was waiting for. It is recorded as a **deliberate approximation the owner has accepted**, not as outstanding
work, so no future round should spend effort on it without the owner reopening it.

**A documentation-accuracy audit was run while the sweep ran**, because a matrix that passes while the docs overstate
it is still a false traceability matrix. Every gate name in `appliesTo` was checked for a real caller through its
no-arg wrapper: `endPortalFrameExplodes` (4 callers), `endPortalFrameLightFifteen` (2), `jumpExperienceBug` (1),
`snowballSurplus` (2), `silkySpawnerDrops` (2). **No gate is dead code**, which matters because this project has
shipped three dead rule sets before and "the gate exists" is not the same claim as "something consults it".

**366 tests, 65 suites, 0 failures.** Deployed build:
**8220D2717FD92889644E832C27E96EF5273F1390187E6C8C951E7E931A50F8B5**.

### The only item still genuinely open

The **Pre1–Pre4 snowfall-destroys-blocks** regression. It has **no reachable source** (both the Pre1 page and the Snow
page are silent; its only citation is this project's own evidence string), no established mechanism, and it is the one
catalogued bug whose faithful emulation **destroys player builds**. It is recorded as blocked-on-evidence rather than
merely undone: the unblocking route is to read the archived `b1.9-pre1.jar` … `b1.9-pre4.jar` clients, which the
wiki's own infoboxes link.

## Round 76 — the spawner drop never fired, and Pre3 was wrongly refused enchanting

**Both reports were real defects in code I had written, and both are now verified.** The owner's play is still the
most productive test harness this project has.

### "Mined a spawner in 1.9 Pre4 with Silk Touch and it dropped nothing"

The rule **could never fire**, and the cause is the same trap as the End-portal-frame bug two rounds ago: it
assumed how the engine reaches a rule instead of checking.

**A modern monster spawner drops NOTHING by design.** Its loot table is literally
`{"type":"minecraft:block","random_sequence":"minecraft:blocks/spawner"}` — no pools — and its only reward is
experience, which arrived in 12w22a. My rule looked for a spawner item in the **rolled list**, reasoning that only
Silk Touch produces one. But the roll is *always* empty, so the test never passed.

Two wrong shapes were tried before the right one:
1. detect Silk Touch from the rolled result — impossible, the roll is empty;
2. move it to the `ordinal-0` "no loot table" return where the frame rule lives — wrong, because the spawner **does**
   have a loot table file (empty), so it takes the `ordinal-1` return.

The fix reads the tool **where the tool actually is**: `Block.getDrops` sets `LootContextParams.TOOL` with
`withParameter` and `LootParams$Builder.getOptionalParameter` reads it back — both verified by disassembling the
mapped jar — so Silk Touch is now a real test via `EnchantmentHelper.getItemEnchantmentLevel`.

**Verified with a control that proves the tool was genuinely enchanted** (a plain stone block, which Silk Touch does
not change, drops stone in every row):

| Version | spawner, no tool | spawner + Silk Touch | stone + Silk Touch |
| --- | --- | --- | --- |
| Pre3 | nothing | nothing | `stone x1` |
| **Pre4** | nothing | **`minecraft:spawner x1`** | `stone x1` |
| Pre5 | nothing | nothing | `stone x1` |

### "Enchanting should work in Pre3, it should just show `Enchanted!!`"

**The owner is right and my gating was too aggressive.** Pre3 had a **working** enchanting table: it applied the
glint, made the name blue and spent levels. What did not exist yet was the *enchantment data*, so the tooltip was a
placeholder. Cancelling the click removed the half that worked in order to imitate the half that was missing.

The click is now refused only *before* Pre3 (`enchantingDisabled`), and the placeholder is implemented where the
difference actually is — the tooltip: `ItemStackHistoricalEnchantedTooltipMixin` renders `"Enchanted!!"` in place of
the enchantment lines for Pre3 alone, and only for an item that **has** enchantments. The data is untouched, so
upgrading a world to Pre4 shows the real names immediately.

### And the tooltip mixin crashed every server start at first

`InvalidInjectionException`: `getTooltipLines` on 1.21.1 takes **`Item.TooltipContext` as its first parameter**, and
my handler had only `Player, TooltipFlag`. Found because the sweep reported a 150-second "hang" whose log was
actually a bootstrap crash. **Fourth time** this project has learned that only starting the game validates a mixin.

### A tooling lesson: never run two sweeps at once

A full 32-version sweep was left running while targeted ones ran, and **two sweeps share `run/probe/server.properties`
and the probe directory**, so they contaminate each other. Its results were discarded rather than reported, and the
mid-sweep Java edits made Pre3 crash, which is what produced the false "hang". One sweep at a time from here.

**366 tests, 65 suites, 0 failures.** Deployed:
**8220D2717FD92889644E832C27E96EF5273F1390187E6C8C951E7E931A50F8B5**.

**To test:** restart fully, then in a new **Beta 1.9 Pre4** world mine a spawner with a Silk Touch pickaxe — it
should drop the spawner. In a new **Pre3** world the enchanting table should work and the tooltip should read
`Enchanted!!`.

## Round 75 — a research gap settled honestly, plus a full matrix re-run

**The Pre1–Pre4 "snowfall destroys blocks" regression has no source I can reach, and is therefore deliberately NOT
implemented.** `research/historical-bugs.json` records it, but its only citation is this project's *own*
`HistoricalVersions.java` evidence string — it is not sourced to any wiki row. Two primary sources were fetched to
settle it and **both are silent**:

- the **Beta 1.9 Prerelease 1** page (the correct title is `Java_Edition_Beta_1.9_Prerelease`; the `_1` suffix is a
  404) lists the jump-experience bug **verbatim** — a useful independent confirmation that the implemented jump bug
  is right — and says nothing about snowfall destroying anything;
- the **Snow** page's Java Edition History table has no such row either. Its nearest real entries are Alpha v1.0.5's
  snowball harvesting and Alpha v1.0.17's *"Snow can no longer fall on non solid blocks, including leaves."*

So the effect, the affected block set and the trigger are all recorded-but-unsourced, and the **mechanism** — what
snowfall does to a redstone torch — has never been established at all.

**Why this one matters more than the others.** It is the only catalogued bug whose faithful emulation is
**destructive**: implementing it means deleting a player's redstone, rails and saplings at random during weather, in
four world versions. Building that on an unsourced description, with no mechanism and no way to verify the affected
set, is the opposite of this project's rule about evidence direction — a wrong guess here does not under-refuse
safely, it destroys real content. Item 16 is the precedent: the owner withdrew the NT lighting engine after it was
implemented and verified, so wasted effort is recoverable but player builds are not.

**What would unblock it, recorded so it is not left vague:** the Pre1–Pre4 client jars are archived, and the wiki's
own infoboxes link them (`b1.9-pre1.jar` … `b1.9-pre4.jar`). The behaviour can be read out of the real code instead
of inferred. That is the route to take if this is wanted.

**A full 32-version matrix re-run is in flight** against the current build, because rounds 73 and 74 re-pointed the
public menu mapping for **two** versions (Beta 1.0 and Beta 1.2). Those were verified individually with controls,
but the sweep is what proves no *other* version moved. Its result belongs in the next checkpoint.

**366 tests, 65 suites, 0 failures.**

## Round 74 — Beta 1.2 was generating sandstone too, and the primary source proved it

Round 73 fixed Beta 1.0's sandstone and deliberately **kept** Beta 1.2's, reasoning that the block existing from
Beta 1.2 made its presence coherent. That reasoning was wrong, and one fetch settled it. The **Sandstone** History
table states the two dates as separate rows:

> `1.2` — **Added sandstone.**
> `1.3` — **Sandstone is now found below three blocks of naturally generated sand.**

So Beta 1.0 had neither the block nor the rule, **Beta 1.2 had the block but not the rule**, and Beta 1.3 has both.
Beta 1.2 was generating 2,993 sandstone its own build could not have placed — the "control" was a second instance of
the same defect.

Both are fixed, each as its own append-only revision: `beta_1_0_no_sandstone` and `beta_1_2_no_sandstone`. Beta 1.3
deliberately keeps the rule, because there it is genuine.

**Verified in three fresh worlds — and the middle row is now the control:**

| Version | alias | sandstone | sand | anachronisms |
| --- | --- | --- | --- | --- |
| Beta 1.0 | `beta_1_0_no_sandstone` | **none** | 8,183 | 0 |
| Beta 1.2 | `beta_1_2_no_sandstone` | **none** | 8,183 | 0 |
| **Beta 1.3** | `beta_1_3_historical_envelope` | **2,993 retained** | 8,198 | 0 |

Beta 1.3 keeping its sandstone is what proves the change is scoped to the two builds whose own history lacked the
rule, rather than to "sandstone everywhere".

**A test guard caught my first attempt at this.** `everyPublicMenuEntryNamesOneAgreeingRevision` failed with
*"beta_1_2 selects beta_1_2_historical_envelope but the newest recipe for that version is beta_1_2_no_sandstone"*,
which exposed a **duplicate dictionary key** in the preset generator where the second entry silently won — the
module would have shipped the fix while the menu still selected the old recipe. Found by a test, not by a player,
which is the outcome to want.

**The lesson, written into the coverage doc:** an inference that a neighbour is "coherent" is not evidence that it is
correct. The cheapest way to settle it was to fetch the page that lists both dates.

**366 tests, 65 suites, 0 failures.** Deployed:
**A1DF45506B062C6596FFA7E387DEC648CF3B9C81803111ED0ED20D78447D4791**.

**To see it:** a **new Beta 1.0** or **Beta 1.2** world has sand with no sandstone beneath it; a **new Beta 1.3**
world still has both.

## Round 73 — the last anachronism in the matrix is gone

**Beta 1.0 was the only version still generating content it never had, and it no longer does.** Measured before:
`minecraft:sandstone=2993` in nine chunks of a fresh Beta 1.0 world while every other declared version held none.
The research is unambiguous on two counts: `version-transitions.json` lists sandstone among **Beta 1.2's** additions
and dates *"sandstone generates under naturally generated sand"* to **Beta 1.3**. Beta 1.0 had neither.

**The cause was a supported setting, not a hook.** Moderner Beta serialises its surface behaviour into the settings
preset, and its own presets read `surface_properties.generateSandstone: true` for `beta` and **`false` for `alpha`** —
read out of the shipped preset JSON. Round 72's disassembly was right that `SurfaceSystem` writes through
`BlockColumn.setBlock` and never reaches the block veto; what it missed is that the rule can be **switched off
upstream instead of intercepted**. That is the better fix and it needed no new hook at all.

The fix is a new revision `historical_experience:beta_1_0_no_sandstone`, whose preset declares the full surface
object with that one flag flipped and everything else identical. **Appended, not edited** — `_historical_envelope` is
published and a world recorded against it must keep the terrain it was given. The menu mapping is re-pointed and the
105 world presets regenerated; the regenerated `beta_1_0.json` differs in exactly the three alias strings.

**Verified in two fresh worlds, with a control:**

| Version | alias | anachronisms | sandstone | sand |
| --- | --- | --- | --- | --- |
| **Beta 1.0** | `beta_1_0_no_sandstone` | **0** | **none** | 8,183 |
| Beta 1.2 | `beta_1_2_historical_envelope` | 0 | **2,993 retained** | 8,198 |

The 8,183 sand blocks are the point of the control: the surface rule that matters was **not** switched off along with
the sandstone, and Beta 1.2 proves the change did not leak to a neighbour.

One test guard did its job and was corrected rather than deleted: `everyPublicMenuEntryNamesOneAgreeingRevision`
rejected the new alias until it was registered, which is exactly the check that stops a menu entry from naming a
recipe the build does not declare.

**Recorded, not resolved.** If sandstone-under-sand is Beta 1.3 then Beta 1.2 has the same rule, and so might
Beta 1.3. Checked directly: `beta_1_2`, `beta_1_3` and `beta_1_7_3` all declare **no** surface properties, so all
three inherit the foundation's `true`. That is coherent — sandstone exists from Beta 1.2, so its presence there is
not anachronistic — but the rule's exact build is not pinned, and Beta 1.0 is now the only version that switches it
off. Changing a second version on that inference would repeat the lapis near-miss in the opposite direction, so
nothing else was touched.

**366 tests, 65 suites, 0 failures.** Deployed:
**178E62C34EC870BB5A16427C26B02646D541E19860906855A3CF8CD016CBCD31**.

**To see it:** restart fully and create a **new Beta 1.0 world** — there should be sand, and no sandstone under it.

## Round 72 — the full matrix measured, three tool defects fixed, the last content gap narrowed

**The whole matrix was re-run against the current build.** 32 declared versions, all `success=true`, all
`presetMatches=true`, **0 timeouts, 0 mixin failures, and 0 anachronisms in 31 of 32**. Total 727.1s, **22.7s mean
per version** (min 20.2s, max 30.3s) — so the entire matrix is about a twelve-minute job. That replaces the
six-minute figure from round 69, which came from an instrument that was mis-reporting.

**The sweep can now report its own cost**, which it could not before: round 69 had to reconstruct the real per-version
time by hand from log timestamps because the tool had called a 30-second run a 180-second hang. Each row carries a
measured `seconds` and the summary prints total and mean.

**Three defects had to be fixed in the tool first**, all the same shape — the instrument contradicting the evidence:

| Symptom | Cause | Fix |
| --- | --- | --- |
| `alpha` reported `exit=1 success=True` | world delete is best-effort; a leftover lock left the **previous run's probe JSON**, so the loop broke instantly and the row read success from a stale file | a verdict counts only if newer than **this** run's start |
| a version that loaded was labelled a startup failure | the failure scan ran on log text alone | gated on the absence of positive evidence it started (`Done (…)` / the verification line) |
| `runExit` showed `1` for runs that plainly succeeded | the log was read a moment before Gradle flushed its final line | left informational, excluded from every verdict, and said so |

**The matrix's only remaining content defect is confirmed and narrowed.** Beta 1.0 still generates
**`sandstone=2993`** in nine chunks; every other version is at zero. The research is now unambiguous on two counts:
`version-transitions.json` puts sandstone among **Beta 1.2's** additions and dates
*"sandstone generates under naturally generated sand"* to **Beta 1.3** — so Beta 1.0 should have had neither. The
mechanism was already established by disassembly and still holds: `SurfaceSystem` writes through
`BlockColumn.setBlock`, and the block veto covers the **decoration phase only** (`blockVetoInvocations` was 24,822
across 26 block ids with sandstone never among them — the veto is never asked, so the refusal logic is not at fault).

It is **not fixed this round, deliberately**: the honest fix is a Moderner Beta surface-rule or settings override for
Beta 1.0 alone, and that third party's preset internals have not been inspected — no `moderner_beta` jar is present
in any cache this environment searches. A blind change to the shared `moderner_beta:beta` preset is exactly how lapis
was nearly deleted from five versions that really had it. A global sandstone veto was considered and rejected: Beta
1.2 onwards legitimately generate sandstone.

**Also fixed:** the owner's log reported `hex1211 has a broken icon`; `fabric.mod.json` declared no icon. A 128×128
icon is now generated and shipped (verified present inside the built jar).

**366 tests, 65 suites, 0 failures.** Deployed:
**7139D1E7AEA4E6BA226FEC515DF26D32D5477B2251F14EBF86FECBE693179BF0**.

**Still open, unchanged and not implied:** Pre4's Silk Touch spawner drop is unverified in the positive direction (the
probe has no player or enchanted tool); the Pre1–Pre4 snowfall-destroys-blocks regression needs its mechanism sourced
before anything destructive is built on a guess; Pre3's `Enchanted!!` placeholder tooltip would remove working
enchantment display; Beta 1.0's sandstone; the Nether terrain shape.

## Round 71 — two more catalogued behaviours implemented, and the first inner-class mixin target

Both are exact-build bugs, verified at runtime rather than merely booted — which is the rule that round 70 paid for.

### Pre3's End portal frame emitted light level 15 (implemented, measured)

The same wiki history row that carries the explosion drop also carries *"Gives off a light level of 15"*, lowered in
Pre4 to *"light level of 1"*. Both halves of that prerelease's documented frame quirks are now implemented.

**Where it could not go, and why that mattered.** `getLightEmission` is declared on neither `Block` nor
`BlockBehaviour` — checked with `javap`, not assumed. It lives on `BlockBehaviour.BlockStateBase` and its whole body
is `getfield lightEmission; ireturn`, so a mixin targeting `Block` could not name the method at all. The new mixin
therefore targets the **nested class** — the first in this project to do so — and it applies cleanly.

Two deliberate choices in it:
- the modern value is **never written into the code**, so only Pre3 is overridden and a future change to the block
  cannot be frozen out;
- the cheap block-identity test comes **before** the gate, because `getLightEmission` is on the light engine's read
  path and the gate reads a volatile session field. Ordered the other way it would pay that read on every light
  query in the world to answer a question that is false for every block but one.

### Alpha 1.0.6's infinite snowballs (implemented, measured)

The wiki row: *"Now require only 4 snowballs to be crafted instead of 9. They still always drop 6 snowballs, allowing
for an infinite source of snowballs."* The recipe half turned out to **already be modern** — verified in the jar where
`recipe/snow_block.json` is a 2x2 of snowballs and `loot_table/blocks/snow_block.json` sets a count of four — so the
surplus is exactly one build returning **six**.

The gate is **equality on the documented build, not a range**, and that is the point: the behaviour is documented for
Alpha 1.0.6 but the fix boundary never was, and a range invented from a single data point is how one build's bug gets
handed to its neighbours.

### Runtime evidence — every row from a real dedicated server, with controls

| Version | Snow block drops | Frame light | Torch control |
| --- | --- | --- | --- |
| **Alpha 1.0.6** | **`snowball x6`** | 1 | — |
| Alpha 1.0.4 | `snowball x4` | 1 | — |
| **Beta 1.9 Pre3** | `snowball x4` | **15** | **14** |
| Beta 1.9 Pre4 | `snowball x4` | 1 | — |
| Release 1.6.4 | `snowball x4` | 1 | — |

The torch reads 14 on the Pre3 run, which is the control that proves the light override is scoped to the frame and has
not lifted an unrelated block's light value.

**366 tests, 65 suites, 0 failures.** Deployed:
**5FE7045252A8F22026E168B30FF9F5FD9BAA0AF02CC76217D451350AD91D2655**.

### What this round did NOT do, recorded so it is not implied

- **Pre4's Silk Touch spawner drop is still not verified** in the positive direction: the probe has no player and no
  enchanted tool. Only the safe direction is proven — no other declared build drops a spawner.
- **The Pre1–Pre4 snowfall-destroys-blocks regression is still unimplemented.** The research records it as a range
  and the effect, but not the *mechanism* (what snowfall does to a redstone torch), and implementing a destructive
  block-removal path on a guess would delete player builds in four worlds. It needs its own source check first.
- **Pre3's "Enchanted!!" placeholder tooltip is still unimplemented.** Making modern tools show a text placeholder
  instead of real enchantments would remove working content, so it needs a decision rather than a default.

## Round 70 — the End portal frame drop was broken twice over, and both causes are now measured

The owner played the round-69 build and reported: **"the frames blew up but didn't drop the item."** That single
sentence was worth more than any test in the suite, and it exposed two independent defects in work I had previously
described as "boot-verified".

### Defect 1 — the injection targeted the wrong RETURN, so it never ran

`BlockBehaviour.getDrops` has two returns: the first hands back `Collections.emptyList()` when the block has **no
loot table at all**, and the second returns a rolled table. The mixin injected at `ordinal = 1` — the post-roll
return, which is what an **ore** takes — so for the End portal frame it never executed.

`end_portal_frame` has **no loot table** in 1.21.1. Verified by listing the jar, not by reasoning: its blockstate,
models, item model and textures are all present, and
`data/minecraft/loot_table/blocks/end_portal_frame.json` does not exist. The upstream-shaped injection could not
reach the one block this feature is about.

The fix is a second handler at `ordinal = 0` for the no-loot-table path, with the frame rule claimed there and a
comment in the ordinal-1 handler saying explicitly why the rule is *not* there — a duplicate would be dead code
that reads like the live rule.

### Defect 2 — the drop path hands back an IMMUTABLE list, so `clear()` threw

Having reached the right return, `drops.clear()` threw `UnsupportedOperationException`: `Collections.emptyList()`
cannot be mutated, and the engine swallowed the exception inside the explosion's own drop call. Both mutation sites
now return a **new** list instead of mutating the one they were given.

This one is worth reading twice, because the runtime probe found it and nothing else did:
`endPortalFrame: probeFailed: java.lang.UnsupportedOperationException`, on Pre3 alone.

### Runtime evidence, four versions, and it is now the probe's job

A `blockDrops` probe was added; it calls the same `Block.getDrops` entry point the explosion uses and reports what
comes back rather than what was expected.

| Version | Frame drop | Spawner gate | Expected |
| --- | --- | --- | --- |
| **Beta 1.9 Pre3** | **`minecraft:end_portal_frame x1`** | false | drops exactly here |
| Beta 1.9 Pre4 | nothing | true | none |
| Beta 1.9 Pre5 | nothing | false | none |
| Release 1.6.4 | nothing | false | none |

**364 tests, 65 suites, 0 failures.** Deployed:
**E0D9B92619938A767BAD7F8E476042F542413CB48662F6F62AF984F2F7A2D25C**.

### Two more defects found in the tooling while fixing the above

- **Making a mixin's static helper `public` crashed every server launch.** `InvalidMixinException: Mixin ... contains
  non-private static method isSpawnerDrop(...)` — Mixin merges such methods onto the target class, so visibility is
  rejected. A green compile said nothing; only starting the game did. This is the third time this project has
  learned that lesson, so it is now written into the file.
- **`BUILD SUCCESSFUL` is printed for a server that died during bootstrap**, because the forked JVM still exits 0
  after reporting `Failed to start the minecraft server`. `runExit=0` was therefore *faithful* and Gradle is what
  misleads. The sweep now answers three questions separately — did the run end, did the build pass, did the server
  actually start — and records `serverStartupFailed`.

**Still not verified by any automated run:** Pre4's Silk Touch spawner drop, because the probe has no player and no
enchanted tool. The probe proves the *safe* direction only (no other version drops a spawner) and says so in its own
javadoc rather than implying more.

# Persistent project status — 2026-10-13 (4) / the "six-minute hang" was my sweep, and two named bugs are in

## Round 69 — the hang explained by measurement, two bugs implemented, all bugs catalogued

**The six-minute hang was a defect in my sweep tool, and the sweep's own output blamed the mod.** The owner
stopped a three-version sweep having watched two versions finish in ~21s and the third sit. The wait loop trusted
`$proc.HasExited`, which never fired: the redirected log beside it read `BUILD SUCCESSFUL in 28s` and the probe
verdict read `success=true` — measured twice, 30s and 32s of real work — while the loop ran to its 180s timeout and
printed *"no verdict within 180s - killing the run (a hang, not a failure)"*. A handle on the redirected pipe
outlives the wrapper by minutes and the script read that as a live server. Completion is now judged from the
artifacts a run writes, never from the process object.

**Measured cost after the fix, because the old number was quoted from a broken instrument:** two versions swept end
to end in **under a minute**, ~26s per version between consecutive `GENERATION_VERIFICATION` lines, both reporting
`exit=0 success=True`. The three-version batch the owner stopped is roughly a **90-second** job, not six minutes.

Two further defects fell out of that work, both recorded in coverage items 17-19:

- The timeout cleanup ran `Get-Process java | ... newStart -and > 200MB` and killed **every** match. This
  workspace's java processes include `C:\Users\jwalr\Flashback`'s own Gradle wrapper — `java -jar
  gradle-wrapper.jar build` — which matches exactly, so one timeout here could silently kill an unrelated project's
  build. Cleanup is now scoped to the pid tree the sweep started.
- The fix's *first* run reported `runExit=-1` for three versions that had all succeeded, because completion was read
  from a log an earlier run had left saying `BUILD SUCCESSFUL`. The log is deleted per version now, beside the world
  that was already deleted for the same reason. Stale evidence that looks fresh is this project's recurring failure.

**Both bugs the owner named are implemented, boot-verified, and honestly half-verified.**

| Bug | Build | Implementation | Verified |
| --- | --- | --- | --- |
| End portal frames blowable | Beta 1.9 Pre3 only | two mixins: `getExplosionResistance` is on `Block`, `getDrops` on `BlockBehaviour` | gate asserted across all 32 versions; Pre3 server boot clean — **no live TNT test** |
| Jump grants 1-100 XP | Beta 1.9 Pre1 only | server-side TAIL injection on `Player.jumpFromGround` | range asserted against the method the hook calls; **no live jump test** |

The jump gate existed but **nothing called it** — the third dead rule set this project has shipped. A test-quality
defect was fixed with it: the old range test re-implemented `1 + nextInt(100)` and compared it with itself, so it
passed whether or not the real gate was correct. The version-keyed overload exists so the test asserts the method
the hook actually calls.

**The full bug catalogue is researched and committed**: 12 records in
[historical-bugs.json](<research/historical-bugs.json>) plus [prose](<research/historical-bugs.md>), validated with
zero schema problems, every record carrying a source and a verbatim quote.

**The owner's spawner premise is wrong for this matrix, and is corrected rather than implemented.** The block was
**added in Infdev 20100625-1917** and pickup stopped in **Alpha v1.0.1**, so both declared Infdev profiles
(`infdev_420` = 20100420, `infdev_611` = 20100611) predate spawners entirely — "mineable before the first Alpha
version" cannot apply to a build that had none. The repo already excludes monster rooms for both with exactly that
reasoning, so this is consistent rather than a gap. The real in-range spawner bug is **Pre4's Silk Touch drop,
removed in Pre5**.

**Final build: 361 tests, 65 suites, 0 failures/errors/skips.** Deployed and hash-verified:
**0FE23A710FE38D5E799DE3E8826E9CE5F515DA58D24C032FE228301696EC6883**, `IN SYNC` with the built jar. (An
intermediate deploy carried `C601B0B8…`; that jar differed only in a corrected comment in the new mixin, but this is
the hash to quote — the earlier one is superseded.)

**For the owner to test — every fix here affects NEW CHUNKS ONLY, and the mod loads at launch:**

1. **Restart Minecraft completely** (a world reload will not pick up a new jar), then **create a new world** in
   **Beta 1.9 Prerelease 3**. Place an End portal frame and blow it up — it should drop as an item. Repeat in
   **Pre4**, where it must be indestructible. This is the half no automated run can see.
2. In a **Beta 1.9 Prerelease 1** world, jump and watch the experience bar: 1-100 XP per jump, and holding the jump
   key keeps paying. In **Pre2** it must pay nothing.

Still open, unchanged: Pre3's light level 15 and its empty-tooltip item form; `beta_1_7` in the bug catalogue is
**not a declared build** (the declared id is `beta_1_7_3`, which the wiki says can load the affected worlds again),
so that one record needs its boundary re-derived before anything is gated on it; the Beta Nether terrain shape;
spawn weights and group sizes. Full commands and limits:
[implementation coverage](<research/implementation-coverage.md>).

# Persistent project status — 2026-10-13 (3) / matrix complete, Beta envelope defect repaired

## Round 54 — the 29-version matrix is complete

Beta 1.4, 1.5 and 1.6 were the last three targets that were NAMED but not implemented. They are now declared
profiles with spawn sets, structure rules, settings/world presets and menu entries, so all 29 planned targets
create real worlds. `HistoricalVersionCatalogueTest.everyPlannedTargetIsImplementedRatherThanMerelyNamed` now
fails on any target that is named without being playable — the counts it replaces could not tell the two apart.

Adding them exposed a **pre-existing defect on Beta 1.0/1.2/1.3**: their settings presets stated no envelope,
so a world created from the menu generated at the modern −64..320 height with deepslate. Measured: 8,366
deepslate and 424 emerald ore in nine chunks of a Beta 1.3 world, plus dripstone and lush cave biomes. The
three new builds' presets were corrected directly; the three published aliases gained append-only
`_historical_envelope` revisions so existing worlds keep the terrain they recorded. Every public menu recipe is
now asserted to declare the envelope.

Also moved two proxy dates that had become true cases: `short_grass`/`dead_bush`/`fern` → Beta 1.6 and
`cobweb` → Beta 1.5. Left alone, a Beta 1.6 world would have refused the vegetation that build introduced.

Final build: **340 tests, 0 failures/errors/skips**. Runtime probes: Beta 1.6 went from depth 192/−64 with
deepslate+copper+emerald to depth 128/0 with **no anachronistic blocks**; Beta 1.3's new envelope revision
likewise clean; Beta 1.7.3's established preset sampled away from the ocean still yields grass_block 1,019 and
short_grass 46, proving the envelope preserves vegetation and the origin sample was simply water.

Remaining: the three new builds' own content (weather, rails, maps, trapdoors, ore-bias fix) is recorded but
unimplemented; historical Nether terrain shape; spawn weights and group sizes; the full per-version behaviour
list in [implementation coverage](<research/implementation-coverage.md>).
Full commands, tables and limitations: [matrix completion and envelope evidence](<research/matrix-completion-verification.md>).

# Persistent project status — 2026-10-13 (2) / 1.21.1 root, NT port + per-profile content correctness

## Nether live-enforcement repair — 2026-10-13 14:02 AEDT

Owner's repeated Nether reports were an enforcement defect, not stale worlds or missing exclusions:
both feature and spawn filters used the generator-only marker lookup, which returns null in the vanilla
Nether. Both now fall back to the historical server scope. Both placed-feature entry points are guarded;
natural spawning uses the level's dimension too, so pre-1.10 Nether Endermen are excluded without affecting
Overworld Endermen. Existing content is not deleted.

Reviewed DeepSeek spawn changes directly; corrected wrong chronology and test assumptions before acceptance.
Final build: **338 tests, 0 failures/errors/skips**. Three dedicated-server runtime controls each scanned
49 Nether chunks: Release 1.0 had zero reported modern ores/blocks or modern spawn candidates;
Release 1.5 retained 6,063 quartz blocks; ordinary world retained modern content with zero suppressions.
Prism deployment hash verified **7E27C27BF871AA71**. No new Prism playthrough claimed.

Commands, raw evidence, limitations and reproducible probe flags are in
[Nether enforcement verification](<research/nether-enforcement-verification.md>).
Remaining: historical Nether terrain shape/Legacy-Nether registry-valid port; spawn weights/pack sizes;
full matrix behaviours and the three undeclared Beta builds. The palette-null theory in the previous
checkpoint was not the cause of this bypass: live block content judging uses dated/late-content tables.


## Rounds 25-29: the combat rules, and three fixes that were structurally right and behaviourally inert

This stretch is worth reading before touching combat again, because the same failure repeated three times: a
gate that answered `true`, a hook on a method that exists, and no change in the game.

### Attack cooldown - works, confirmed by the owner

`Player.getAttackStrengthScale` pinned to `1.0F` when the session says the version predates 1.9. The owner
reported the cooldown gone.

### Sweep attack - cancelled the wrong method first

The first fix cancelled `Player.sweepAttack()`, which looks like the method that performs a sweep. It does not:
disassembling `Player` shows the **`getEntitiesOfClass` loop that actually damages nearby entities sits inside
`attack`**, while `sweepAttack()` plays the sound and particles. So cancelling it removed the presentation and
left the damage, which is exactly what the owner saw - "sweeping remains".
[PlayerHistoricalSweepMixin](<src/main/java/net/historicalexperience/mixin/PlayerHistoricalSweepMixin.java>)
now redirects the area-of-effect call itself, so the loop iterates nothing, and it avoids guessing the local
boolean that gates the sweep.

Building that mixin cost two more mistakes, both from *generating* code rather than reading what was generated:
a regex ate the terminating `;` of the descriptor (remapJar refused the jar outright), and the call is the
**two-argument** `getEntitiesOfClass(Class, AABB)`, so the handler had a parameter too many.

### Damage values - the table was right and my alarm was wrong

The owner reported swords and axes at modern values. The gate answered `true`, so the hook was suspect. It was
not:

1. `LivingEntity.collectEquipmentChanges()` **does** call `ItemStack.forEachModifier` with the `EquipmentSlot`
   overload - the one that is hooked. An earlier search claimed it did not, because the reference sits inside a
   lambda and the grep missed it.
2. The real ordering problem: the attribute map is built when equipment changes, and the session is only known
   when a player **joins**. So the map held modern values and was never rebuilt while the same item was held.
   [HistoricalEquipmentRefresh](<src/main/java/net/historicalexperience/gameplay/HistoricalEquipmentRefresh.java>)
   re-runs the equipment pass once the session is set, on both sides, using
   [an accessor](<src/main/java/net/historicalexperience/mixin/LivingEntityEquipmentInvoker.java>) because
   `collectEquipmentChanges` is private.

Then the probe printed both numbers for real items, which settled it:

| Item | Modern | Era | |
| --- | --- | --- | --- |
| wooden sword | 1.6 | 1.6 | same |
| diamond sword | 4.6 | 4.6 | same |
| **diamond axe** | 6.0 | **3.0** | halved |
| **iron axe** | 5.9 | **1.9** | halved |
| diamond pickaxe | 2.2 | 2.2 | same |
| **diamond shovel** | 2.5 | **1.0** | -60% |
| **diamond hoe** | 1.0 | **4.0** | x4 |

That matches the research exactly: 1.9's 15w34c reduced swords by one and raised axes by four then three, so a
sword is the **same number on both sides of the boundary**. "Swords did not change" is correct behaviour, and
the round-14 suspicion that the damage hook was dead was withdrawn once it could be measured.

### Critical hits - removed by redirecting the condition's own lever

`PlayerHistoricalCriticalHitMixin` redirects the `onClimbable()` call the crit condition makes inside `attack`:
answering "climbing" makes the condition false. It touches only that call site, so ladders still work, and it
avoids guessing a local variable index. Boot-verified - a wrong owner descriptor fails fatally, so a clean boot
is evidence here.

### A hole in my own verification, found and closed

The probe **never set the active session**, so every session-gated rule was inert while it measured - it was
reporting success on the modern game. It now sets the session as a joining player would. That is the same class
of defect as the missing bindings: a check that cannot fail is worse than no check.

### Where the gates stand

**All 13 gates in the tested `appliesTo` table are wired to a hook. 26 mixins.** One behaviour is deliberately
left unwired: `alwaysOpenChestEnabled`, because the timeline has no row for "a chest blocked by a solid block
above", and an unsourced behaviour is not ported just because upstream has a flag for it.

## Rounds 18-19: sword blocking both halves, and both damage overloads

**Every rule set in the project is now wired.** Nothing is dead code any more.

### Sword blocking, put in as one coherent change

| Half | Implementation |
| --- | --- |
| the stance | `HistoricalSwordBlockingEvents.register()`, now enabled in the entrypoint |
| the 50% reduction | [LivingEntityHistoricalSwordBlockingMixin](<src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalSwordBlockingMixin.java>), `@ModifyVariable` on `hurt` |

Only the first half was available from a Fabric event; the second needed a hook that did not exist. Enabling
one without the other would show a player a raised sword that does not protect them, which is a visible lie
about the era rather than a smaller version of the feature, so they went in together. The version gate is
free: `canBlock` already consults `swordBlockingEnabled`, so the stance exists from Beta 1.8.1 to Release
1.6.4 and nowhere earlier.

### The damage hook now covers both overloads, settled by bytecode rather than guesswork

`ItemStack.forEachModifier` is declared twice on 1.21.1 - per equipment slot and per slot group. Only the slot
form was hooked at first. Whether the other mattered was not guessed: `javap -c` shows **each overload reading
`DataComponents.ATTRIBUTE_MODIFIERS` directly and calling its own `ItemAttributeModifiers.forEach`, with
neither delegating**. So the group form was silently skipping the substitution, and hooking both cannot
double-apply - which would have doubled attack damage, because each hook rewrites independently.

**Every mixin descriptor is now written in full.** A bare ambiguous name is a fatal mixin error at startup, and
rounds 12-15 cost two crashes learning it.

### Runtime evidence: seven of ten profiles, then ten

Sampled in rounds 16-19 (all with an empty anachronism list, the reduced envelope, deepslate off and the
cave-biome fix applied):

| Profile | Structures suppressed | Allowed |
| --- | --- | --- |
| Infdev 20100420 | 16 | none |
| Infdev 20100611 | 12 | none |
| Alpha | 21 | none |
| Beta 1.7.3 | on `preview_10` | n/a |
| Beta 1.8.1 | 49 | mineshaft, stronghold, **our village** |
| Beta 1.9 pre3 | 42 | + fortress |
| Release 1.0.0 | 42 | + fortress |

The fortress appears only from Beta 1.9 pre3, which is the boundary the timeline records - the gate is doing
what the research says rather than what a list happens to contain.

## Rounds 15-17: two fatal mixin errors, and the lesson they bought

**Rounds 12 and 14 reported "BUILD SUCCESSFUL, all tests passing" while shipping mixins that made the server
refuse to start.** Unit tests never load mixins. Two failed, both fatally:

| Mixin | Error | Cause |
| --- | --- | --- |
| `PlayerHistoricalSprintMixin` | critical injection failure | targeted `Player`, but `setSprinting` is declared on **`LivingEntity`**; inheritance does not count for a class-level mixin |
| `ItemStackHistoricalDamageMixin` | critical injection failure | 1.21.1 declares `forEachModifier` **twice**; a bare method name is ambiguous |

Both fixed: the sprint mixin retargeted to `LivingEntity`, the damage mixin given the full descriptor
`forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V`.

**The rule that follows, and it is not negotiable from here: a mixin is only validated by starting the game.**
A green build says the code compiles; it says nothing about whether the injection matches. Every mixin added
from now on is confirmed by a server boot before anything is claimed about it, and the reason is written into
the file headers so it survives.

### Sprinting: server-side was not enough

The owner loaded a Beta 1.7.3 world and reported sprinting still worked. Two causes, one of them real:

- the jar they had predated the fix, because round 12's version could not boot at all;
- **the gate required a `ServerLevel`**, so the client took an early return and kept sprinting locally. The
  player saw sprint speed and the field-of-view change while only the server disagreed - the worst of both.

It now consults `HistoricalActiveSession`, which `HistoricalVersionSync` sets on both sides. Checked for a
cross-world leak, because a global static gating a player ability is exactly the shape that leaks: the server
clears it on `SERVER_STOPPED` and `HistoricalClientRules` clears it when the world goes away, so an ordinary
world after a historical one keeps sprinting.

### The Infdev dungeon defect, confirmed by observation and then fixed

The research recorded that monster rooms arrive in Infdev 20100625-1917. A real Infdev 20100420 world then
showed `minecraft:spawner` and chests, so a dungeon was generating two months early - in a build whose own
client had none. Four new revisions, append-only as required:

| Profile | What the new revision adds |
| --- | --- |
| Infdev 20100420, Infdev 20100611 | the Release 1.2.1 desert well **and** the monster room |
| Alpha | the desert well |
| Beta 1.7.3 | the desert well, as `beta_1_7_3_content_preview_10` |

The dungeon boundary is **derived** inside the shared helper from the registry's own chronology (20100625-1917
falls between the 20100611 build and Alpha) rather than passed in per caller, so the three profiles cannot
drift apart from the research. Script: [add-1211-pre-beta-content-revisions.py](<scripts/add-1211-pre-beta-content-revisions.py>).

Verified: **226 tests, 30 suites, 0 failures**, server boots with no mixin failures, and Beta 1.7.3 now runs as
`beta_1_7_3_content_preview_10` with `sampledPaletteFailures` empty.

### Still open

- `HistoricalSwordBlocking` - the last rule set deliberately not registered.
- The `EquipmentSlotGroup` overload of `forEachModifier` is **not** hooked, so a caller reaching that form
  directly skips the damage substitution.
- The damage substitution has never been watched in a live world (needs one sword hit in a Beta 1.7.3 world).
- The visual/client tranche: HUD hunger bar, experience bar, sprint field-of-view. The mechanics behind them
  are off; the pixels are not.

## NT port — batches landed, and what is NOT yet verified

Server-side mob behaviour, copied from Nostalgic Tweaks and gated per version from
[the feature timeline](<research/feature-timeline.json>). Eight mixins are registered in
[the mixin config](<src/main/resources/historical_experience.mixins.json>):

| Mixin | Upstream tweak | Boundary used |
| --- | --- | --- |
| [FleeSunGoalHistoricalMixin](<src/main/java/net/historicalexperience/mixin/FleeSunGoalHistoricalMixin.java>) | DISABLE_MONSTER_AVOID_SUN | Release 1.2.1, proxied by 1.2.5 |
| [PanicGoalHistoricalMixin](<src/main/java/net/historicalexperience/mixin/PanicGoalHistoricalMixin.java>) | DISABLE_ANIMAL_PANIC | **Beta 1.8** - and the old gate applied to *every* version, a real defect |
| [TemptGoalHistoricalMixin](<src/main/java/net/historicalexperience/mixin/TemptGoalHistoricalMixin.java>) | DISABLE_ANIMAL_TEMPTING | Beta 1.9 Prerelease 2 |
| [MobHistoricalItemPickupMixin](<src/main/java/net/historicalexperience/mixin/MobHistoricalItemPickupMixin.java>) | DISABLE_MONSTER_ITEM_PICKUP | Release 1.4.2, proxied by 1.6.4 |
| [AnimalHistoricalBreedingMixin](<src/main/java/net/historicalexperience/mixin/AnimalHistoricalBreedingMixin.java>) | DISABLE_ANIMAL_BREEDING | Beta 1.9 Prerelease 2 |
| [MobHistoricalEquipmentMixin](<src/main/java/net/historicalexperience/mixin/MobHistoricalEquipmentMixin.java>) | DISABLE_MONSTER_ITEM_SPAWN / ARMOR / ENCHANT | Release 1.4.2, proxied by 1.6.4 |
| [ZombieHistoricalSpawnMixin](<src/main/java/net/historicalexperience/mixin/ZombieHistoricalSpawnMixin.java>) | DISABLE_BABY_ZOMBIE_SPAWN / PIGLIN | **Release 1.6.2** |
| [AgeableMobHistoricalSpawnMixin](<src/main/java/net/historicalexperience/mixin/AgeableMobHistoricalSpawnMixin.java>) | DISABLE_BABY_ANIMAL_SPAWNING | Beta 1.9 Prerelease 3 |

Three API facts were settled by the compiler and the crash log rather than by memory, and are recorded in
the file headers: MixinExtras **is** present (Fabric API bundles 0.4.1) but the project uses plain Mixin;
`Mob.pickUpItem` is `protected void`, and the `@Redirect` + static `@Invoker` form crashed the server with
`InvalidAccessorException`, so the hook is a cancellable HEAD injection on the method itself;
`Zombie.getSpawnAsBabyOdds` is `public static` and therefore has no level, so the baby hook is on
`finalizeSpawn` instead.

**NOT YET VERIFIED, and this is the first task of the next round.** The eight gates compile, are registered
and are exercised by real worlds, but **no unit test asserts which versions each one applies to**. The gates
take a `Level`, so an offline test cannot reach them as written; they need a `(String versionId)` seam that
the `Level` methods delegate to, and then a per-version assertion for all ten profiles. Every other gate in
this project has that test and this batch is the exception - it is recorded here rather than left implied.

### That gap is now closed

Every gate computes its boundary in one version-keyed method, `HistoricalGameplayTweaks.appliesTo(gate,
versionId)`, and each `Level` gate is that method plus one call. An **unknown gate name throws** rather than
answering false, because a gate that silently never applies is indistinguishable from one that correctly
does not. [HistoricalGameplayTweaksTest](<src/test/java/net/historicalexperience/gameplay/HistoricalGameplayTweaksTest.java>)
walks all 11 gates against all 10 profiles and asserts the whole table, that the gate list and the tested set
agree in both directions, that a proxy version keeps the behaviour it proxies, that ordinary and undeclared
worlds are never restricted, and that the two corrections below stay corrected:

- animal panic is Beta 1.8, **not** every version;
- baby zombies are Release 1.6.2, so only Release 1.6.4 has them;
- the gear and enchantment gates must always agree, because they share one boundary.

**215 tests, 27 suites, 0 failures.**

### Found: the researched gameplay rules are dead code

A usage sweep shows three researched rule sets are referenced by nothing and applied by nothing, because the
mixins that consumed them were lost when the 26.3 tree was deleted and never re-ported:

| Rule set | Contains | Applied by |
| --- | --- | --- |
| `HistoricalLootRules` | per-version mob drop tables (`BETA_1_7_3`, `BETA_1_8_1`), experience suppression | **nothing** - only the version registry names it |
| `HistoricalDamageValues` | the old weapon and tool damage numbers | **nothing** |
| `HistoricalPlayerRules` | the old player rules the version registry reads | **nothing** |
| `HistoricalFoodRules` | per-version direct food healing | only the registry, no hook |
| `HistoricalSwordBlocking` | the Beta 1.8-1.9 blocking stance | `HistoricalSwordBlockingEvents`, which the entrypoint deliberately does not register yet |

These are the next task, and the loot one first: the data is already researched and reviewed, so wiring it
is a mixin plus tests rather than new research. `LivingEntity.dropAllDeathLoot` is the hook the drop tables
need, and `suppressesExperience` belongs on the experience award path.

### The loot tables are live again (Release 1.6.4's rows are still dead - see below)

[LivingEntityHistoricalLootMixin](<src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalLootMixin.java>)
registers both halves:

- `LivingEntity.dropAllDeathLoot` cancelled at HEAD for the mobs a version's table names, with the era's own
  item spawned instead. Cancelling before spawning is what stops a mob dropping both; only the four mobs the
  tables name are touched.
- `LivingEntity.dropExperience` cancelled where the version awarded no experience. That needs its own hook
  because on 1.21.1 experience is not awarded through the loot path - verified against the mapped jar, which
  is why the two injections are separate.

The probe now reports `loot` alongside `spawns`, so `replacedDeaths`, `dropsByEntity` and
`suppressedExperience` are visible from a real session rather than only from a unit test.

[HistoricalLootWiring1211Test](<src/test/java/net/historicalexperience/gameplay/HistoricalLootWiring1211Test.java>)
is a **wiring** test, and that shape is the point: the tables were dead because every test asked the table a
question instead of asking whether anything read it. It now asserts that the set of mixin classes on disk is
exactly the set the mixin configuration names - a behaviour file with no registration fails - and separately
that Beta 1.7.3's zombie drops a feather with bound 3, that only the versions with their own researched drops
replace loot, and that experience is withheld exactly where the version awarded none.

**218 tests, 28 suites, 0 failures.**

Still dead, and the next task: `HistoricalDamageValues` (old weapon and tool damage), `HistoricalPlayerRules`,
`HistoricalFoodRules`' heal values, and `HistoricalSwordBlocking` - which the entrypoint still deliberately
does not register. The loot work is the template: find the hook the mapped jar actually offers, wire it, and
add a registration test so it cannot go quiet again.

### Correction: the round-9 "Beta 1.8.1 loses experience" finding is NOT established (round 10)

The fix was implemented anyway, because it is right for a different reason, and then the same experiment was
run again - and it showed the round-9 conclusion does not hold.

What the Beta 1.8.1 run shows now:

| Observation | Result |
| --- | --- |
| `zombieDrops` | `minecraft:rotten_flesh x1` - the era's drop, replaced correctly |
| `replacedDeaths` / `droppedItems` | `1` / `1` - coherent with the observed item |
| `shouldDropExperience` | `true` |
| `experienceOrbs` | **0** |
| `suppressedExperience` | **0** - the suppression hook still never runs |

The hook still never runs, so the orbs are still not withheld by it. **The experiment cannot distinguish
"the version awards no experience for this kill" from "our hook removed it"**, and the reason is a vanilla
rule: mobs award experience only when the kill is attributed to a player, and the probe kills the zombie with
`damageSources().generic()`. Zero orbs is therefore the expected result for *any* version in this probe.

So the honest statement is: **the experience half of the loot mixin is unverified, in both directions.** It
may be correct; it has never been exercised. Round 9's claim that Beta 1.8.1 was losing experience was an
inference from ambiguous evidence and is withdrawn rather than left standing as a finding.

**The hook change is kept, on its own merits.** Cancelling `dropFromLootTable` instead of
`dropAllDeathLoot` is strictly more surgical: it replaces the loot *table* and leaves the rest of the death
path - custom death loot, equipment drops, the experience step - untouched, where the old hook removed all of
it. Verified on Beta 1.8.1 that the drop is still replaced and the counters still agree.

**What would settle the experience question:** a kill attributed to a player, which needs a fake or real
player in the probe, or a direct invocation of the experience path with a player attacker. Until then no run
may be cited as evidence for it in either direction.

### Settled in-game by the owner (round 12), and the hunger report was not just cosmetic

The owner loaded a real Beta 1.7.3 world and a Beta 1.8.1 world and reported: **experience is off in Beta
1.7.3 and food directly restores health there, and the opposite is true in Beta 1.8.1.** That is first-hand
observation from a live client, so it settles both questions the probe could not answer:

- the experience suppression **works** (rounds 9-10 could only say "unverified in both directions"; the
  zero-orb readings were inconclusive, exactly as recorded, and are now resolved in the implementation's
  favour);
- direct food healing **works** in both directions.

The owner also reported that **hunger still exists in Beta 1.7.3**, and assumed that was only the visual
tranche. That assumption is half right, and the important half was a real defect: the HUD is cosmetic, but
`FoodData.tick` genuinely drains saturation and then the food level, so a Beta 1.7.3 player really was getting
hungry and the bar was telling the truth. Hiding the bar alone would have left the mechanic running.

**Fixed this round** - the third dead rule set, `HistoricalPlayerRules`, is now applied:

| Mixin | Hook | Boundary |
| --- | --- | --- |
| [FoodDataHistoricalHungerMixin](<src/main/java/net/historicalexperience/mixin/FoodDataHistoricalHungerMixin.java>) | `FoodData.tick` cancelled at HEAD | no hunger system before Beta 1.8.1 |
| [PlayerHistoricalSprintMixin](<src/main/java/net/historicalexperience/mixin/PlayerHistoricalSprintMixin.java>) | `Player.setSprinting` cancelled at HEAD | sprinting is the Beta 1.8 Pre-release |

Both are server-side, because that is what decides whether the mechanic happens. The remaining Beta 1.7.3
complaints are now genuinely presentation only: the hunger bar, the experience bar and the sprint FOV are
drawn by the client and belong to the visual tranche, which has not been started.

**18 mixins registered, 221 tests, 29 suites, 0 failures.** The two new gates are version-keyed already
(`HistoricalPlayerRules.suppressesHungerTick/allowsSprinting`) and the registration test covers the wiring;
neither has a per-version assertion test yet, which is the same gap the gameplay gates had in round 4.

### The last dead rule set is wired: old damage values (round 14)

[ItemStackHistoricalDamageMixin](<src/main/java/net/historicalexperience/mixin/ItemStackHistoricalDamageMixin.java>)
hooks `ItemStack.forEachModifier`, which is how 1.21.1 publishes a stack's attribute modifiers to an
`AttributeMap`. It reads the modern `ATTRIBUTE_MODIFIERS` component, calls the already-researched
`HistoricalDamageValues.rewrite`, and forwards the rewritten modifiers to the caller's consumer before
cancelling. Only the attack-damage entry is substituted - `rewrite` preserves every other attribute - so
mining speed and the rest keep their modern values.

`HistoricalDamageValues` was the third and last rule set found as dead code in round 5. All three now have a
hook:

| Rule set | Hook | Runtime status |
| --- | --- | --- |
| `HistoricalLootRules` | `LivingEntity.dropFromLootTable` + `dropExperience` | drops observed live; XP confirmed in-game by the owner |
| `HistoricalFoodRules` | `Player.canEat` + `Player.eat` | confirmed in-game by the owner |
| `HistoricalPlayerRules` | `FoodData.tick` + `Player.setSprinting` | reported fixed to the owner this round; not yet re-checked by them |
| `HistoricalDamageValues` | `ItemStack.forEachModifier` | **compiles and is registered; the effect is NOT yet verified** |

**19 mixins, 226 tests, 30 suites, 0 failures.**

Two honest limits on the damage wiring. First, only the `EquipmentSlot` overload of `forEachModifier` is
hooked; the `EquipmentSlotGroup` overload is not, so if any caller reaches the group form directly the
substitution is skipped there. Second, nothing has observed a Beta 1.7.3 sword dealing the old number - the
arithmetic is unit-tested and the hook is registered, and those are not the same claim. A one-line in-game
check (hit something with a diamond sword in a Beta 1.7.3 world and in a Release 1.6.4 world) would settle
it, exactly as the owner's last check settled experience and food.
 **Gap closed (round 13).** [HistoricalPlayerRulesWiring1211Test](<src/test/java/net/historicalexperience/gameplay/HistoricalPlayerRulesWiring1211Test.java>)
now asserts, for all ten profiles, that hunger is suppressed exactly where the version had none (the two
answers are one rule read twice, so a disagreement fails by construction), that sprinting is refused exactly
before the Beta 1.8 Pre-release and allowed from it, that an ordinary or undeclared world keeps vanilla
hunger and sprinting, and that both carrying mixins are registered by name so a rename cannot quietly
un-wire the fix. **226 tests, 30 suites, 0 failures.**


### The player-free blocker, and it is concrete (round 11) - now partly bypassed by owner observation

The obvious route does not exist on this dependency set: **Fabric API 0.116.5+1.21.1 ships no
`FakePlayer`.** The aggregate `fabric-api` jar is only a bundle of nested modules, and the module that would
own it, `fabric-entity-events-v1-1.8.0+2b27e0a419.jar`, contains no FakePlayer class at all - checked by
listing the jar, not by memory.

That matters twice over, because a player is needed for two separate verifications:

| Wanted | Why a player is required |
| --- | --- |
| experience suppression | vanilla awards mob experience only for a kill attributed to a player, so a generic-damage kill proves nothing in either direction |
| direct food healing | eating is a player action - `Player.eat`, `Player.canEat` and `getFoodData` all need a player instance |

Vanilla commands do not get around it either: `/damage` applies damage with no attacker, and `/execute as`
needs a player that already exists.

**The options, recorded so the next attempt does not redo the search:** construct a synthetic `ServerPlayer`
with a dummy connection (real work, and the connection type differs from a client connection); or drive a
real dedicated server with a headless client/bot and use commands; or move these two rules to a test that
runs inside a world with a genuine player, which the project's owner can do in seconds and a probe cannot.

No code changed this round: the tree is left exactly as round 10 verified it, green, rather than adding a
probe path that cannot answer its question.


The probe asked the zombie `shouldDropExperience()` **before** killing it, so the orb count is explained
rather than guessed at. In a real Beta 1.7.3 world: `shouldDropExperience = true`, yet `experienceOrbs = 0`
**and** `loot.suppressedExperience = 0` - so the suppression hook never ran, and the zero orbs were not its
doing.

That is the signature of experience being awarded *inside* `LivingEntity.dropAllDeathLoot`, which the loot
mixin cancels at HEAD. Two consequences, and the second is a real bug:

- For a version with no experience (Beta 1.7.3, Alpha, both Infdev) the behaviour is accidentally right:
  cancelling the whole method removes the loot and the orbs together.
- For **Beta 1.8.1, which HAS experience**, replacing the drop therefore also deletes the experience the
  version should award. A Beta 1.8.1 zombie would give the era's rotten flesh and no orbs.

The same run also showed `zombieDrops = []` with `replacedDeaths = 1`, and that is **correct**, not a defect:
the Beta 1.7.3 count bound is 3, so `nextInt(3)` legitimately rolls zero. The earlier run's two feathers were
the other side of the same roll. Worth recording because "no drops" beside a replaced death looks like a bug
and is not.

**The fix, to be implemented next:** hook `LivingEntity.dropFromLootTable(DamageSource, boolean)` instead of
`dropAllDeathLoot`. Cancelling the loot *table* for a covered mob replaces the drop while letting the rest of
the death path run, so experience survives where the version had it and the `dropExperience` hook keeps
suppressing it only where it did not. Neither the drop tables nor the XP gate may be considered done until a
Beta 1.8.1 run shows the era's drop **and** non-zero orbs.


`GenerationVerification` now runs a **gameplay probe** in the same opt-in pass, because the block and
structure checks cannot see the simulation rules and a rule can be correct in a unit test while never being
applied in a world - which is exactly how the drop tables sat dead.

It spawns a zombie through the vanilla spawn path, reads its spawn result, kills it, and reports what the
world produced. A zombie is the subject because every one of these rules has an opinion about it.

Observed in a real Beta 1.7.3 world ([evidence](<logs/sol-1211-sol-gameplay-beta173b-fresh.json>)):

| Observation | Result | Meaning |
| --- | --- | --- |
| `zombieDrops` | `feather x1, feather x1` | the researched Beta 1.7.3 drop, **not** rotten flesh |
| `loot.replacedDeaths` | `1` | the vanilla table really was replaced, not merely skipped |
| `loot.droppedItems` / `dropsByEntity` | `2` / `{zombie=2}` | the counters and the observed items agree |
| `babyZombie` | `false` | the Release 1.6.2 gate holds |
| `gearedZombie` | `false` | the Release 1.4.2 gate holds |

A reporting defect was found by reading this output rather than trusting it: `loot` was captured **before**
the probe killed the zombie, so it reported `replacedDeaths=0` beside two dropped feathers. The counters are
now read after the probe, so the evidence cannot contradict itself.

**NOT proven by this observation, and recorded as such: experience suppression.** `experienceOrbs` is 0, but
so would it be if no experience were awarded at all - and `suppressedExperience` stayed 0, meaning the hook
was never reached. A zombie killed without a player is not the experiment that settles it, so the XP half of
the loot mixin remains unit-verified only.


[PlayerHistoricalFoodMixin](<src/main/java/net/historicalexperience/mixin/PlayerHistoricalFoodMixin.java>)
wires `HistoricalFoodRules`, the second rule set that was researched and unused. Two hooks, and the first is
not a convenience:

- **`Player.canEat` is un-gated.** A version with no hunger keeps its food level pinned at full, so vanilla's
  "you are not hungry enough" check refuses every meal - a hurt player would starve beside a full inventory.
- **`Player.eat` is replaced**: the era's food restored health directly and fed no bar, so the item is consumed
  and `heal` is called with the value the table records for that item. A food the table does not model keeps
  the modern path rather than being guessed at, which the test asserts.

[HistoricalFoodWiring1211Test](<src/test/java/net/historicalexperience/gameplay/HistoricalFoodWiring1211Test.java>)
asserts direct healing applies to exactly the hungerless versions (both directions, for all ten), that eating
is un-gated only where there is no hunger, that every healing value is positive and evidenced, and that
unmodelled or null inputs leave vanilla alone.

**221 tests, 29 suites, 0 failures.**

Remaining dead rule sets, in the order they should be tackled: `HistoricalDamageValues` (old weapon and tool
damage - needs the item attribute path), `HistoricalPlayerRules`, and `HistoricalSwordBlocking`, which the
entrypoint still deliberately does not register. None of the three can be verified at runtime by the
generation probe, so each will need either a command-driven runtime test or an explicit statement that it is
unit-verified only.

## The project is now the 1.21.1 mod at the repository root

Owner instruction: the port is finished, so migrate out of `mc1211/`, delete the 26.3 sources, and keep
1.21.1 as the only target. Done, in [the migration script](<scripts/migrate-to-root.ps1>):

- `mc1211/src` → `src`, and `mc1211/build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`,
  `gradlew`, `gradlew.bat` → the root (replacing the 26.3 Kotlin build scripts).
- The 26.3 `src/`, `build.gradle.kts`, `settings.gradle.kts`, `build/` and project `.gradle/` were deleted.
- The 21 shared unit tests that mc1211 used to include through `srcDir '../src/test/java'` were copied in
  first, so the suite is local now and [build.gradle](<build.gradle>) no longer reaches outside the project.
- `run/` (EULA, server.properties, verification fixtures) merged into the root `run/`, and `mc1211/` is gone.
- Every script and doc that named `mc1211/...` was rewritten for the flat layout.

**Root build and tests pass: 209 tests, 26 suites, 0 failures** ([build log](<logs/sol-1211-root-build2.log>)).
The count rose from 197 because the previously shared tests are now inside this project.

One test had to be rewritten rather than repaired: `publicPresetsSelectOneAgreeingRevisionOfTheSameHistoricalVersion`
compared each preset against the 26.3 file it came from, and that file no longer exists. It now asserts, against
this project's own data and version registry, that every menu entry resolves, that all three stored recipe
references agree, that the recipe is the NEWEST for its version, and that the Nether and End stay vanilla, so
it can still fail.

## Village generation ported from the owner's designated reference

[old_villages](https://github.com/MaxTechnik3796/old_villages) (MIT per its own `gradle.properties`; no LICENSE
file, recorded rather than glossed) is ported as `historical_experience:historical_village`:
[structure](<src/main/java/net/historicalexperience/worldgen/HistoricalVillageStructure.java>),
[pieces](<src/main/java/net/historicalexperience/worldgen/HistoricalVillagePieces.java>),
[per-version block rules](<src/main/java/net/historicalexperience/worldgen/HistoricalVillageBlocks.java>),
[data](<src/main/resources/data/historical_experience/worldgen/structure/historical_village.json>) and a
[verification probe](<src/main/java/net/historicalexperience/server/VillageVerification.java>).

The subagent that started this was stopped and the work taken over, integrated and finished by hand. Three
real defects were found and fixed in it, each by the research rather than by inspection:

- **Carrots and potatoes were gated at Beta 1.8.1.** They are Release 1.4.2 (12w34a), so Beta 1.8.1 through
  Release 1.2.5 grew wheat alone; only Release 1.6.4 grows root vegetables.
- **Sandstone stairs were in the every-version set.** They are absent from the Beta 1.8.1 extraction and
  first appear in the 1.6.4 one, so the older desert village gets the era's stone stair (cobblestone) instead.
- **Sandstone slabs, sandstone stairs, brewing stands and cobblestone walls were unjudgeable**, because the
  block mapping did not know their era names (`stoneSlab`, `stairsSandStone`, `brewingStand`, `cobbleWall`).

The village is still a **recorded fidelity limit**, not a claim: it is the Release 1.13 layout, so for
profiles before then the building shapes are later than the era's own. Its blocks are era-correct, which the
tests enforce, and the desert-material rebuild of Release 1.3.1 is the remaining gap.

## Current priority — every profile, not just Beta 1.7.3

### Latest: the first measured modern-content defect is fixed and verified

A Beta 1.8.1 world generated on the previous recipe contained **kelp 338, kelp plants 1,666, seagrass 1,301
and tall seagrass 1,212** — all 1.13 content — because every version after Beta 1.7.3 carried only the eight
far-future block exclusions and nothing else. New revision `_no_modern_content` added to all six
post-Beta-1.7.3 versions, each with its own evidence-filtered exclusion list:

| Version | Extra exclusions beyond the shared modern set |
| --- | --- |
| Beta 1.8.1 | + water lilies (Beta 1.9 content) + desert well (Release 1.2.1) |
| Beta 1.9 pre3, 1.0.0, 1.1 | + desert well |
| Release 1.2.5, 1.6.4 | none - lily pads and the desert well are content these versions had |

Melons are deliberately **kept** for all of them (Beta 1.8 introduced melons), which is why the list is
derived with an explicit filter rather than copied from the Beta 1.7.3 set.

Verified in a real world: alias `historical_experience:beta_1_8_1_no_modern_content`,
**`sampledPaletteFailures` empty**, and kelp/seagrass absent from the saved blocks. **197 tests pass.**
Evidence: [revision test run](<logs/sol-1211-revisions-test.log>),
[runtime sample](<logs/sol-1211-sol-rev2-beta181-fresh.json>).

New data: six `*_no_modern_content` settings presets, the public mapping in
[the world-preset generator](<scripts/generate-1211-world-presets.py>) and all 34 world presets regenerated.
Explicitly NOT visible to players: a world created earlier keeps the alias it recorded, so nothing already
generated moves.

### Next owner direction — port NT directly, per version

The owner's newest instruction is to **copy the Nostalgic Tweaks implementations directly and apply them per
historical version**, using [the feature timeline](<research/feature-timeline.json>) to decide which versions
each behaviour belongs to, and to take the village port over from the subagent. The licence direction is
recorded in [the notices](<THIRD_PARTY_NOTICES.md>): the ported portions carry LGPL-3.0 attribution, and every
ported file names its upstream file. Scope stated by the owner: gameplay, visual (client) and other NT
features, per version.

Owner request: *"copy over the stuff from NT and implement that … match this with the research … also you
need to go through all the profiles not just beta 1.7.3 and make sure they don't include structures that
don't belong or blocks that don't belong again cross reference with the research already done."*
The feature source is [the feature timeline](<research/feature-timeline.json>) plus the per-version
[block palettes](<research/block-palettes/>) and [version boundaries](<research/version-boundaries.json>).
Goal `goal-20e81d72-fef0-4950-857d-7b2fac48e43a` tracks it.

### Structures — a sourced allow-list for all ten profiles (DONE, tested)

[HistoricalStructureRules](<src/main/java/net/historicalexperience/generation/HistoricalStructureRules.java>)
now carries a rule for **every** declared profile, each with the wiki History line as evidence. What the
rules enforce, and the defects they close:

| Profile | Allowed structures | Was |
| --- | --- | --- |
| Infdev 20100420, Infdev 20100611, Alpha, Beta 1.7.3 | none | already empty |
| Beta 1.8.1 | mineshaft, stronghold, plains village, desert village | **nothing was suppressed at all** |
| Beta 1.9 pre3, 1.0.0, 1.1, 1.2.5 | + nether fortress | nothing suppressed |
| Release 1.6.4 | + desert pyramid, jungle pyramid, witch hut | nothing suppressed |

Deliberate exclusions: `mineshaft_mesa` is barred from every profile (the mesa biome is Release 1.7.2, so
the structure cannot legitimately exist), and the savanna/taiga/snowy village variants are barred from every
profile (Beta 1.8 villages "only generated in plains and desert biomes"). The rule is an **allow-list**, so
a structure from a later release or another mod is suppressed rather than silently appearing.

A second defect is fixed with it: the Nether and the End of a historical world use ordinary vanilla level
stems, so the structure hook previously saw no marker there and could not rule on them - a Beta 1.7.3 world
could generate a **Nether fortress**, which is Release 1.0.0 content.
[HistoricalGenerationScope](<src/main/java/net/historicalexperience/generation/HistoricalGenerationScope.java>)
records the running world's recipe for exactly those hooks, and is cleared on server stop so it cannot leak
into the next world loaded in the same process.

### Blocks — a per-version introduction table for all ten profiles (DONE, tested)

The block check used to be hard-coded to one Beta 1.7.3 revision, and `paletteFor` returned null for every
other version, so **no other profile could be judged at all**. That is the defect the research itself
recorded: a 289-chunk Alpha world contained ten vines and nothing objected.

[HistoricalBlockAvailability](<src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java>)
is **generated** by [the generator script](<scripts/generate-1211-block-availability.py>) from the reviewed
palettes and the timeline, so it cannot drift from them: **201 of 221 mapped blocks are dated**, 63 from the
timeline and 138 from the earliest extracted palette that registers the name.
[HistoricalBlockContent](<src/main/java/net/historicalexperience/generation/HistoricalBlockContent.java>)
combines it with the version-agnostic list for content later than every declared profile.

Three real transcription errors were found and fixed while building it, each of which would have dated a
block to a version that never had the name and then **deleted it from worlds that did**: `stoneBrick` should
be `stonebricksmooth`, `ironFence` → `fenceIron`, `brickStairs` → `stairsBrick`, `stoneStairs` →
`stairsStoneBrickSmooth`, `emeraldOre` → `oreEmerald`, `monsterEgg` → `monsterStoneEgg`, and
`hardenedClay`/`stainedClay`/`carpet` → `clayHardened`/`clayHardenedStained`/`woolCarpet`. The
version-agnostic list also wrongly filed **terracotta under Release 1.12** (it is 1.6.1, when it was called
hardened clay), which would have reported a correct Release 1.6.4 world as broken.

The safety rule that makes this trustworthy: a **palette-derived** date may not condemn a version that has
no extracted palette of its own. Alpha and both Infdev builds never attach a name to a block at
registration, so their absence from a palette proves nothing; only a sourced timeline boundary may rule
against them. Otherwise "stone first appears in the 1.7.3 set" would be read as "Alpha had no stone".

Evidence: 196 tests pass in [the run log](<logs/sol-1211-profiles-test5.log>); the new
[ProfileGenerationRules1211Test](<src/test/java/net/historicalexperience/generation/ProfileGenerationRules1211Test.java>)
re-parses the generated table, cross-checks every palette-derived date against the research JSON, asserts no
profile may generate a structure its era lacked, and asserts blocks each version certainly had are never
called anachronistic. Two shared tests that pinned the old, wrong state were corrected rather than deleted:
one expected the misspelled legacy name, and one asserted that later profiles deliberately had **no**
structure rule (that gap is now filled from evidence).

### Recorded, not yet implemented

These are evidence-backed findings that need **new generation revisions** (revisions are append-only, so an
existing recipe may not be edited) and therefore new settings/world presets:

- `minecraft:monster_room` and `monster_room_deep` are **placed features**, not structures, and the monster
  room only arrives in **Infdev 20100625-1917**. Infdev 20100420 and Infdev 20100611 therefore must not
  generate dungeons. They currently do. This needs a new revision on those two versions.
- `minecraft:desert_well` is a placed feature introduced in **Release 1.2.1 (12w04a)**, so it must be
  excluded from every profile up to and including Release 1.1. Same mechanism, eight versions.
- Allowed later structures are generated by the **modern** structure code, whose build materials are not the
  era's (the modern desert pyramid uses Release 1.13 sandstone variants). The structure-level boundary is
  correct; block-level fidelity inside those allowed structures is an open gap and is recorded as one rather
  than claimed.
- Birch/spruce/jungle logs, leaves, saplings and planks are currently mapped to the single Beta 1.7.3 `log`
  and `leaves` blocks. Jungle woods are Release 1.2.1 content and birch/spruce earlier still; no timeline
  row dates them, so they are deliberately left UNKNOWN rather than guessed.

### Nostalgic Tweaks port

The reference catalogue is [the candidate list](<research/nostalgic-tweaks-candidates.json>) (489 tweaks on
the audited branch, 105 non-client). The upstream reference is
[old_villages](https://github.com/MaxTechnik3796/old_villages) for village generation, pinned at
`7578013ec061303982539fac7606df67577ffb26` in `.upstream/old_villages`, licence MIT as declared in its own
`gradle.properties` (it ships no `LICENSE` file, which is recorded rather than glossed). A port of its
worldgen into this project is in progress; the parent integrates only after it compiles and generates.

---

# Persistent project status — 2026-10-13 / Minecraft 1.21.1

## Current priority — Sol's direct generation-backport repair

Latest human instruction: retarget the active runtime to **1.21.1**, backport ALL generation features
already implemented in the root 26.3 reference, and exclude non-generation gameplay/client and Flashback.
The old 26.3 milestones below are retained history, not current runtime instructions.

### Additional direct owner request — include world upgrading

Owner: “Make sure you port over the world upgrading too”. Generation scope now explicitly includes
upgrading. Real production tests exercise the `/hex upgrade` command for both same-era recipe revisions
and cross-era targets. Sol also identified and fixed the late startup-recovery timing hole: recovery now
runs under the acquired session lock BEFORE level.dat is decoded, not after the old generator is loaded.
The 1.21.1 persisted references are preflight-checked against metadata before staging, and unquoted
namespaced targets use the correct Brigadier argument parser. **World upgrading PASSED its 11 production controls**, including same-era recipe advance, cross-era
advance, destination future chunks, both interrupted recovery windows, corrupt-backup refusal without
writes, unknown/no-op targets and ordinary-world refusal. Existing full-chunk blocks, block entities and
player-data sentinel were preserved. Final safety-guard recheck is accepted: the last harness assertion
was fixed to treat Gson's omitted null `alias` as null, then the actual ordinary refusal was rerun.
The final remapped artifact also passed a fresh production JVM reload with all 98 original completed
chunk block hashes unchanged. [Final verification summary](<logs/sol-1211-final-verification-summary.json>)
records **187 tests / 24 suites / zero failed, errors or skipped**, 15 generation controls, 11 upgrade
controls, packaged hooks/notices and actual 1.21.1 saved recipe references.

Deliverable: [verified Minecraft 1.21.1 jar](<build/libs/historical-experience-1211-0.1.0.jar>),
SHA-256 `ece1d36b4429edc51d6b7b553174e27924453d207a1ff2b93179ff4231d36735`.
Replace the old hex1211 jar rather than loading both. `/hex upgrade VERSION` stages the operation;
save/quit and restart to load the destination recipe. Existing chunks are deliberately not scrubbed.
The generation-plus-upgrading repair is complete; the broader historical gameplay, full timeline,
Nether/End fidelity, Flashback and the user's full client modpack remain separately scoped work.

### Implementation repaired

- Restored all five generation mixins, enabled and verified one at a time: feature exclusions, historical
  ores, decorator writes, structure starts, and natural/chunk-generation spawn candidates. Their real
  1.21.1 descriptors were inspected, not guessed.
- Restored all ten public world-preset entries to their original latest recipe bindings. The previous
  regeneration script had silently selected unrestricted legacy Beta, pre-grass-fix Alpha/Infdev and
  pre-cave-fix later versions. Legacy recipe declarations remain separate and preserved.
- Added 1.21.1's upper alternate-stone feature placements to runtime exclusion translation; do not try to
  replace absent leaf litter/firefly content. Restrictions remain generation-time and per generator.
- Restored the owned dimension type in the actual 1.21.1 schema. Measured MB's ordinary overworld_128
  as **minY -64 / depth 192**, not 0..127. Applying the supported upstream reduced-envelope component
  produces **generator minY 0 / depth 128 / real bedrock**, without copying a terrain/router database.
- Corrected generator persistence to 1.21.1's actual level.dat layout and backup coverage. The new tests
  preserve player/world tags, refuse wrong aliases/layouts, and exercise journal recovery/preservation.
- The active entrypoint no longer registers partial sword-blocking gameplay. Menu descriptions state
  generation-only scope. Source notice inclusion and the latest migration report are synchronized.
- Shared plus 1.21.1-specific unit suite: **187 tests, all passing** after the upgrade safety additions,
  in the retained [final upgrade build](<logs/sol-1211-upgrade-final-build.log>) (previous generation-only
  milestone: 183 tests in the [earlier build log](<logs/sol-1211-final-build.log>)). The remapped jar, not just dev classes, has also booted and
  generated Beta terrain in a production Fabric server.

### Acceptance evidence

The full acceptance matrix **PASSED: 15 server runs**, covering all ten implemented public profiles,
two Beta seeds, a production remapped-jar launch, fresh-JVM reload, distant future generation and
vanilla/plain-Moderner-Beta negative controls. The original **98 completed chunk block hashes** and metadata
bytes were unchanged on reload and after generating the distant sample (147 completed chunks thereafter).
Every run sampled 49 full chunks; Beta samples had **zero anachronistic palette entries, zero unknown
non-air blocks and zero valid saved structure starts**. Fresh Beta explicitly suppressed 54 structure
starts, 4,183 feature placements and 154 spawn-list entries; the actual injected decorator funnel suppressed
bee nests and vines while permitting an oak-log write. Evidence:
[matrix summary](<logs/sol-1211-matrix-summary.json>),
[matrix output](<logs/sol-1211-matrix.log>),
[production Beta sample](<logs/sol-1211-sol-production-beta-fresh.json>).
Current source parity, exact reproduction commands and inherited limitations are in the
[backport report](<docs/BACKPORT-1211.md>). Test worlds are isolated under
[the project test directory](<run/verification/>), never personal Minecraft saves.


## Current continuation — 2026-10-12

Active goal: `goal-f0185cd7-d27c-44a0-af66-9292778a641a`; old session remains paused. Actual routes `deepseek-official/deepseek-flash` and `lmstudio/qwen3.6-35b-a3b-splash` were discovered and genuinely invoked. Sol coordinates shared builds and reviews actual source/runtime evidence; [review notes](<docs/REVIEW_NOTES.md>) record worker corrections and scope adjustments.

### Verified current milestone

- Parent latest `gradlew --console=plain test build probeVerdictNegativeControls`: exit0, **101 tests / 12 suites / 0 failed, errors or skipped**, independently parsed all XML headers ([latest build](<logs/continuation-08-direct-preset-test-build.log>)). Four new scoped-probe tests were genuinely implemented by Qwen and reviewed; two complete-map evidence predicate tests by parent. Earlier milestone: 95 tests / 10 suites. [Latest build log](<logs/continuation-06-diagnostic-test-build.log>). Earlier full `--rerun-tasks` build had 89 tests / 9 suites ([log](<logs/continuation-02-test-build.log>)); parent then added six pure diagnostic sentinel/envelope tests. Observed UNKNOWN marker never adopts any metadata state as ordinary.
- Hardened verdict gate: **15 synthetic negatives rejected + valid positive PASS accepted**, exit0. [Controls log](<logs/continuation-01-verdict-controls.log>). Fixture prints actual Loom task names.
- **Fresh establish passed**, exit0, runId `establish-20261012T220402613-3ec6afe3`, **77 checks / 0 failures / 9 FULL chunks / 270,210 non-air**; metadata created, gold at **(-8,16,-8)** flushed to correct dimension region path. [Establish log](<logs/continuation-03-establish.log>), [retained verdict](<logs/continuation-03-establish-verdict.txt>).
- **Separate fresh-JVM reload passed**, exit0, runId `reload-20261012T220517319-46a84f91`, **76 checks / 0 failures / 9 FULL chunks / 270,211 non-air**; metadata enforced, gold survived. [Reload log](<logs/continuation-04-reload.log>). These are current terrain/persistence evidence, NOT gameplay fidelity.
- Root cause now observed: [reload NPE evidence](<logs/continuation-04-reload.log#L75-L91>) shows MB sea-level getter before its provider was initialised by a later SERVER_STARTING listener. Guarded terrain diagnostics prevent this becoming registry failure; post-start terrainKnown=true required. Vanilla composite LEVEL_STEM API remains unchanged. No DIMENSIONS fallback or substitution of unavailable registry evidence.
- Fresh failed fixture preserved as [failed world](<run/probe/world-failed-20261012T2157>); original earlier baseline also untouched. **Three real-server ordinary/corrupt/missing-marker controls passed** via [lifecycle harness](<scripts/verify-lifecycle-controls.ps1>), harness exit0 and each expected server/probe command exit1. Parent inspected actions/reasons, ordinary successful startup followed by intentional historical-probe rejection, metadata SHA256 preservation and byte-for-byte properties restoration. [Results](<logs/lifecycle-20261012T220621012-626a6010/results.json>), [summary log](<logs/continuation-05-lifecycle-controls.log>).

### Latest milestone — the generation backport shipped two crashers; mixins are off until each is verified

**What happened, plainly.** The 1:1 backport of the generation-suppression mixins was registered as a batch of four and given to the project owner without being verified against 1.21.1. Two of them crash the game:

| mixin | failure | cause |
| --- | --- | --- |
| `StructureHistoricalSuppressionMixin` | `InvalidInjectionException: Invalid descriptor` | 26.3's `Structure.generate` takes a `Holder`, a `ResourceKey` and a `Climate.Sampler` that 1.21.1's does not; the ported handler kept 26.3's parameter list |
| `TreeDecoratorContextHistoricalSuppressionMixin` | `@Shadow method level() was not located` | 26.3's `TreeDecorator.Context` has `level()`; 1.21.1's does not |

Both are the same mistake - porting 26.3's **signatures** faithfully instead of its **behaviour** - and it is exactly the failure this project already made twice on 26.3, where the mixin config being `required` is what caught it. The difference here is that the catch happened in the project owner's game rather than in a verification run, because the mixins were registered in a batch and handed over without one.

**Corrective action, and a change of process.** The mixin list is now **empty**. Verified locally with a real server run: `Done (6.829s)!`, no mixin failure, and a world that generates 49 chunks with real blocks. A world can be created and played.

**The rule from here, and it is not negotiable for this backport:** one mixin at a time. Add it, run a server locally, confirm the mixin applies and the world still generates, and only then does it reach the owner. A mixin that has not been seen to apply is not finished work, however correct its logic.

**What is working and does not need mixins:** Moderner Beta's own settings carry most of the historical content already - `deepslate_generation: false` removed all 684,735 deepslate blocks and restored the Beta stone column (586,359 → 1,282,636), `noise_generator_settings: moderner_beta:overworld_128` gives the 128-block height, and `caveBiomeSettings.provider: moderner_beta:none` removed the dripstone. The 28 settings presets and their world presets are data and are selectable and valid on 1.21.1.

**What still needs the mixins, and is therefore not done:** `andesite`, `tuff`, `diorite`, `granite` and `copper_ore` still generate - about 4.4 percent of blocks - because Moderner Beta's own Beta preset sets nothing for them. That is what `HistoricalFeatureExclusions` is for, and it will be wired one hook at a time.

### Previous milestone — the 1.21.1 runtime loads and generates, and the datapack answer to "did we lose anything?"

**The crash the project owner hit is fixed, and the cause is understood exactly.** Taking the built jar into a 1.21.1 instance and opening Create World crashed on:

```
Errors in element historical_experience:beta_1_7_3_overworld:
  Failed to parse historical_experience:dimension_type/beta_1_7_3_overworld.json
  No key has_raids / piglin_safe / bed_works / natural / ultrawarm
```

Those files were **26.3-schema data carried into the port**: 26.3's dimension type uses the newer `attributes`/`timelines` form and omits fields 1.21.1 requires, and the noise settings use 26.3's `material_rule`/`noise_router` names. Two further stale copies made this look like it survived a fix - Gradle's `build/resources` and `run/config/moderner_beta/datapacks` - so the jar and the run directory kept serving deleted files. After clearing both, the server reaches **`Done (5.837s)!`** and generates real chunks on 1.21.1.

**The answer to "didn't that remove things that needed backporting?" is: the reconstruction, deliberately, and the capability, re-pointed.**

- The deleted `noise_settings/beta_1_7_3_overworld_128` is a hand-written noise router of ours. The contract says plainly not to reconstruct terrain/noise/climate configurations because **Moderner Beta owns them** - so this file should never have existed in a 1.21.1 build.
- **But 25 of our 28 settings presets referenced it**, to force the 128-block Beta world height. So the capability did need carrying over, and it now points at **`moderner_beta:overworld_128`**, which Moderner Beta itself ships. Same height, MB's own settings, no parallel terrain database. The substitution is recorded in `scripts/point-presets-at-mb-height-1211.py`.
- Our 28 MB **settings** presets are otherwise correct for 1.21.1 as written - `beta_1_7_3.json` is simply `moderner_beta:preset: moderner_beta:beta` for the chunk, biome and cave providers.

**Still to do on this path:** the world *selectors*. The deleted `world_preset` files were the 26.3-schema mechanism for choosing a profile at world creation; 1.21.1 needs the same idea in its own schema, referencing these settings presets, and Moderner Beta's own `world_preset/moderner_beta.json` shows the exact shape. Until that exists, a new world uses the default preset, which is why the census above still shows modern blocks.

### Previous milestone — 101 compile errors down to 10, with every generation file across

**The 1.21.1 port is nearly through its compile surface: 101 errors → 10, in five files.** Everything generation-related compiles: `HistoricalFeatureExclusions` (extra-block and feature suppression), `BetaOrePlacement` (ore placement) and `BetaOreVein` (Beta's ore vein geometry), plus all of `version/`, `profile/` and `world/` - the metadata journal, backups and the upgrade transaction among them.

**`RuntimeProbe` was deleted rather than ported.** It was 39 of the 101 errors on its own and its job - reporting what generation produced - is done better by `scripts/region-block-census.py`, which reads saved region files without launching the game and can hold them against a version's own block palette.

**Every difference was read out of the real 1.21.1 jar with `javap` rather than guessed**, and they are all recorded in [docs/BACKPORT-1211.md](docs/BACKPORT-1211.md) - `Identifier`→`ResourceLocation`, `ToolMaterial`→`Tier`, `WeightedList`→`WeightedRandomList` (with no builder: the element type carries its own weight), the permission-set API→`hasPermission(2)`, `compositeAccess()`→`registryAccess()`, `PayloadTypeRegistry.clientboundPlay()`→`playS2C()`, `ItemStack.isValidRepairItem`→`Item.isValidRepairItem`, and 26.3's `Optional`-returning NBT reads → 1.21.1's direct ones, where a helper keeps "absent" distinguishable from "present but empty".

**One difference is an improvement, not a workaround.** 1.21.1's `Mth.sin` takes a `float` where 26.3's takes a `double`, and Beta's original algorithm was float maths - so the `(double)` casts in the 26.3 ore-vein code were a 26.3-ism, and removing them makes that geometry **more** faithful than it was.

**And where guessing stopped being good enough, that is recorded too.** The last ten errors were attacked the same way, and three guesses about *surrounding* types were wrong: at the settings-preset site the holder is neither `isEmpty()` nor `isBound()`, and `TypedActionResult` is in `net.minecraft.util`, not `net.minecraft.world`. Those need the code around them read rather than another substitution, and pretending otherwise is how the wrong hook got shipped twice on 26.3.

### Previous milestone — 101 compile errors down to 25, and every generation file is across

**The 1.21.1 port is most of the way through its compile surface.** `` now builds all of `version/`, `profile/`, `world/` and - the part the project owner asked for specifically - the generation content rules: `HistoricalFeatureExclusions` (extra-block and feature suppression), `BetaOrePlacement` (ore placement) and `BetaOreVein` (Beta's ore vein geometry). **101 errors → 25**, across 7 files whose remaining work is understood.

**`RuntimeProbe` was deleted rather than ported.** It was the single largest block of errors (39) and its job - reporting what generation produced - is now done better by `scripts/region-block-census.py`, which reads the saved region files without launching the game and can hold them against a version's own block palette.

**Every API difference was read out of the real 1.21.1 jar rather than guessed**, and recorded in [docs/BACKPORT-1211.md](docs/BACKPORT-1211.md): `Identifier`→`ResourceLocation`, `ToolMaterial`→`Tier`, `WeightedList`→`WeightedRandomList` (and no builder - `create(list)` instead), `Holder.isEmpty()`→`isBound()`, the permission-set API → `hasPermission(2)`, `compositeAccess()`→`registryAccess()`, and `ItemStack.isValidRepairItem`→`Item.isValidRepairItem`.

**One difference turned out to be an improvement rather than a workaround.** 1.21.1's `Mth.sin` takes a `float` where 26.3's takes a `double`, and Beta's original algorithm was **float** maths - so the `(double)` casts in the 26.3 code were a 26.3-ism that the backport removes, making the ore vein geometry more faithful than it was.

### Previous milestone — the port is compiling, and one wrong assumption was costing 101 errors

**The whole source tree is now in `` and reducing to a known surface: 101 compile errors down to 95, with the important files already across.**

**The finding that mattered most: this project must use Mojang official mappings, not Yarn.** The first compile's leading error was `package net.minecraft.core.registries does not exist` - the wrong mapping set, not an API difference. Yarn renames `ResourceLocation`, `Level` and `BuiltInRegistries`, which this codebase *and* Nostalgic Tweaks are written against. `loom.officialMojangMappings()` collapsed that noise and left only real differences. Had it not been caught, every ported file would have been rewritten for nothing.

**The generation content the project owner asked to bring across is compiling on 1.21.1:**
- `HistoricalFeatureExclusions` - the extra-block and feature suppression
- `BetaOrePlacement` - ore generation
- plus `version/` and `profile/` (1,184 lines, **zero Minecraft imports**) and all 2,238 lines of `world/`, including the metadata journal, backups and the upgrade transaction, all unchanged.

Only two mechanical fixes were needed for those: 1.21.1 still calls the class `ResourceLocation` (renamed to `Identifier` in 1.21.2), and `Mth.sin`/`Mth.cos` return a `double` where 26.3 returned a `float`.

**Remaining surface, per file:** `RuntimeProbe` 39 (the 26.3 in-game census tool - now redundant beside `scripts/region-block-census.py`, so a candidate for deletion rather than porting), `HistoricalDamageValues` 14, `HistoricalSpawnFilter` 11, `WorldGenSettingsAdvance` 9, `ModernerBetaAdapter` 8, then five files with 1-4 each. Full detail in [docs/BACKPORT-1211.md](docs/BACKPORT-1211.md).

### Previous milestone — a block census that reads region files, and the honest news that preset selection does not work declaratively

**A new tool answers "what did generation actually produce?" without launching the game.** [scripts/region-block-census.py](scripts/region-block-census.py) reads Anvil region files and the 1.18+ chunk format - sections, palettes, packed indices - using only the standard library, and reports the real block distribution. It also takes a palette and lists every block that is **not** in the declared version, which is precisely the check the generation work needs. Verified against real 1.21.1 chunks: 49 chunks, Y −4..19, 4,816,896 blocks.

**Moderner Beta runs and generates on 1.21.1** - and the census can prove what it generated, not merely that it started.

**But selecting a historical preset headlessly does NOT work, and this is now measured rather than assumed.** Three attempts, each judged by the census:

| attempt | result |
| --- | --- |
| `level-type=moderner_beta:moderner_beta` | MB's own world preset runs, but its settings map is empty - MB logs `No key preset in MapLike[{}]` - so generation stays modern |
| our own world preset, `level-type=hex1211:beta_1_7_3` with `moderner_beta:preset` set on the generator, biome source and cave provider | **silently ignored**, no error |
| overriding MB's own `world_preset/moderner_beta.json` from our mod's data | **ignored as well** |

All three produced 49 chunks, 1,176 sections, 684,728 deepslate and 57,756 tuff - identical to the digit, which is not spawn variance. One run's *region file sizes* differed and looked like a change; it was chunk ordering, and content is the only honest measure. So the declarative route is out, and the next step is the **API route the 26.3 work already uses**: bind the generator at world creation through `ModernBetaSettingsPresets.BETA_1_7_3` and the `createChunkGenerator` path in [ModernerBetaAdapter.java](src/main/java/net/historicalexperience/integration/modernerbeta/ModernerBetaAdapter.java). That is the "port the identical generation functionality" work rather than a new design.

**Moderner Beta does have per-version presets, as the project owner said.** The Java constants include `BETA_1_1_02`, `BETA_1_7_3`, `BETA_1_8_1` and its amplified/large-biome variants, `BETA_1_9_PRE_3`, and the data files include `alpha`, `alpha_infdev`, `infdev_227/325/415/420/611`, `indev`, `classic_0_0_14a_08`, `classic_0_30`, `pe`, `early_release`, `release_1_0_0`, `release_1_1`, `release_1_2_5` and `release_1_6_4`. `BETA_1_7_3` resolving to the data file named `beta` is why the absence of a `beta_1_7_3.json` was never evidence of a missing preset - the first version of that claim was mine, and it was wrong.

**Also moved:** Nostalgic Tweaks is now checked out at `origin/1.21` (`51d6d3dc`), whose own build properties declare `minecraft_version=1.21`, `minecraft_max_version=1.21.1` - the exact target, replacing the 1.21.4 revision the 26.3 work cited.

### Previous milestone — the runtime target is moving to 1.21.1, and Moderner Beta generation is PROVEN there

**The project owner directed that the runtime move from Minecraft 26.3 to 1.21.1, and that Nostalgic Tweaks' 1.21.x branch - not the 1.21.4 branch - be the source for behaviour ports.** This supersedes the earlier 26.3 objective and is recorded here rather than silently applied. Full reasoning and the ordered plan are in [docs/BACKPORT-1211.md](docs/BACKPORT-1211.md).

The reason is arithmetic. Nostalgic Tweaks' `origin/1.21` branch declares `minecraft_version=1.21` and `minecraft_max_version=1.21.1` - the exact target. On 26.3 every tweak had to be re-derived against an API that had moved underneath it, and some could not be ported at all: `getSpawnAsBabyOdds` became static and lost the level it needed to ask which version it was in. On 1.21.1 the same files are copied in and compiled.

**Verified, not assumed:**

| claim | evidence |
| --- | --- |
| Moderner Beta publishes for 1.21.1 | `5.0.0-alpha.4+1.21.1-fabric` - the **same 5.0.0-alpha line** as the 26.3 build, depending only on Fabric API |
| **Moderner Beta generates terrain on 1.21.1** | a dedicated server reached "Preparing start region" and wrote real region files - **348.9 KB and 175.3 KB** |
| The installed toolchain covers it | JDK 21 is present; Loom 1.18.4 and Gradle 9.8.1 are reused unchanged and dependency resolution succeeds |
| Nostalgic Tweaks has an exact-target branch | `origin/1.21` at `51d6d3dc`, `minecraft_version=1.21`, `minecraft_max_version=1.21.1` |

**What is NOT claimed.** The generation run used Moderner Beta's **default** preset, not Beta 1.7.3. Setting a preset headlessly needs its per-world settings, which were not located: the config at `run/config/moderner_beta.json` holds only client-side colour and preview data, so the world settings live with the save. Until a block census is taken under the Beta preset, the only claim is that Moderner Beta **runs and generates** on 1.21.1.

**The 26.3 work is not wasted.** The version registry, profile and world metadata, the journal/backup/upgrade machinery, the generation content rules, the research files (including the 143-row sourced timeline and 7 of 10 block palettes) and the 240-test suite are Minecraft-version-agnostic Java and data. The 24 mixins are what get rewritten - by copying upstream, which is the point. The 26.3 project at the repository root stays intact and working until the port is proven, so verified behaviour is not lost while 1.21.1 is built beside it in ``.

### Previous milestone — the timeline research landed, corrected four of my assumptions, and sword blocking is in

**`research/feature-timeline.json`: 143 sourced boundary rows - 130 verified - each with a URL and the exact quote.** Mostly the Minecraft Wiki's own History tables, cross-checked against three hash-verified Mojang client jars. It is the artefact that turns "which versions had this?" from guesswork into a lookup, and it immediately **corrected four things this project assumed**:

| assumption | reality | effect |
| --- | --- | --- |
| sword blocking existed before 1.9, so gate it for every version | **added** in the Beta 1.8 Pre-release, removed in 1.9 | gate flipped: off for Infdev/Alpha/1.7.3, **on from Beta 1.8.1** |
| breeding/tempting absent only before Beta 1.8 | arrives in **Beta 1.9 Prerelease 2** | **Beta 1.8.1 had neither** - gate extended, my version was wrong |
| monsters avoiding sun applies to all versions | avoiding is **Release 1.2.1** (burning is Indev 20100213, in range everywhere) | no longer applied to 1.2.5 or 1.6.4 |
| monster item pickup + geared mobs apply to all versions | both **1.4.2** | no longer applied to 1.6.4 |

The two items the timeline *confirmed* are attack cooldown and sweep (**1.9**) and critical hits (**Beta 1.8 Pre-release** - which is why the critical-hit gate correctly stops at 1.7.3).

**Boundaries are now expressed as version arithmetic, not lists.** `olderThan(version, "release_1_2_5")` reads as the evidence reads, and adding a version between two others moves it to the correct side of every boundary automatically. Where a real boundary falls between declared versions - 1.4.2 is not declared - the nearest declared version on the right side proxies it, and the comment says so. That is a visible approximation; a hand-written id list is the invisible kind.

**Dungeons were the one finding that did not require a fix, and the reason is worth recording.** The timeline shows dungeons go back to **Infdev 20100625-1917**, so Alpha and Beta 1.7.3 DID have them - which contradicts this project's structure rule as written. But 26.3 generates dungeons through a **placed feature**, verified against the jar: `monster_room.json` exists under `worldgen/placed_feature` and not under `worldgen/structure`. So the structure rule never touched them, and the Alpha census that showed chests, spawners and mossy cobblestone was correct all along. The misleading "no structures of any kind" comments are corrected to say exactly what the rule does and does not govern.

**Sword blocking is implemented** - the gameplay half of the eight-file upstream cluster: the blocking test (`isBlocking`, `canBlock`, empty off hand, `#minecraft:swords`), the right-click that raises the sword (Fabric's use-item callback, upstream's `startUsingItem` plus interrupt), and the damage reduction on `hurtServer` with the sword wearing down one point per blocked hit and blocking stopping if it breaks, all at upstream's 50 percent. The remaining five files are the renderer side - arm and hand poses - which are cosmetic and await 26.3's renderer.

**240 tests / 28 suites / 0 failures**, probe passes with no mixin failure, `git diff --check` clean.

### Previous milestone — eleven NT behaviours ported; one of them needed no new boundary at all

**Two more ported this round, both verified applying:**

| our mixin | upstream tweak | behaviour | boundary |
| --- | --- | --- | --- |
| `TemptGoalHistoricalMixin` | `DISABLE_ANIMAL_TEMPTING` | animals stop following a player holding their food, restricted to `AgeableMob` as upstream does | pre-Beta-1.8, alongside breeding, because tempting exists to start breeding |
| `ServerLevelHistoricalOrbMixin` | `DISABLE_ORB_SPAWN` | experience orbs are refused at the entity manager, so they never exist | **reads the version's existing `awardsExperience` rule** |

**The orb gate is the shape the rest should follow, and it is worth naming.** It needed no new historical research at all: a version that awards no experience cannot legitimately produce an orb, and `awardsExperience` is already recorded per version from verified evidence. Two rules that mean the same thing now cannot drift apart, and the port cost nothing but the mixin. Wherever a new behaviour can be derived from an existing verified rule instead of a fresh boundary, that is strictly better.

**Eleven Nostalgic Tweaks behaviours are now ported:** skeleton strafing, monster item pickup, animal breeding, attack cooldown, sweep attack, critical hits, monsters fleeing the sun, animal panic, monster equipment at spawn, animal tempting and experience orbs.

**Two more 26.3 API differences were found by compiling rather than by assuming.** `EntityAccess` - the type handed to the entity manager - exposes no `level()`, so the orb gate takes the version from `this`, the `ServerLevel` being mixed into. And the entity-manager call is `PersistentEntitySectionManager.addNewEntity`, which is where upstream hooks it too, so that one carried across unchanged.

**240 tests / 28 suites / 0 failures**, 18 verdict negatives rejected, `git diff --check` clean. Worklist: **78 server gameplay tweaks, 10 touched, 68 to port.**

**Both research subagents are still running.** They are the ones that unlock the remaining 68 systematically: the feature timeline establishes the boundaries the UNVERIFIED gates are waiting on, and the per-version block palettes extend the block suppression beyond Beta 1.7.3, which only that version has today.

### Previous milestone — mob behaviour batch ported, two research subagents dispatched, and the verification loop caught a real bug

**Three more Nostalgic Tweaks behaviours are ported and gated by version, bringing the total to eight:**

| our mixin | upstream tweak | behaviour |
| --- | --- | --- |
| `FleeSunGoalHistoricalMixin` | `DISABLE_MONSTER_AVOID_SUN` | monsters stop running for shade - the burning damage is untouched, only the fleeing |
| `PanicGoalHistoricalMixin` | `DISABLE_ANIMAL_PANIC` | animals stop panicking when hurt, restricted to `Animal` exactly as upstream does |
| `ZombieHistoricalSpawnMixin` | `DISABLE_MONSTER_ITEM_SPAWN` | zombies stop spawning already equipped, by wrapping the `setItemSlot` calls |

**These are the first gates with UNVERIFIED boundaries, and they say so.** The project owner chose breadth over waiting for research, so each applies to every declared version and each evidence string carries "UNVERIFIED" explicitly. The value of putting them in one place is that tightening a boundary later is a one-line change to the gate, with no mixin touched.

**One upstream tweak was deliberately NOT carried across, and the reason matters.** `DISABLE_BABY_ZOMBIE_SPAWN` hooks the `RandomSource.nextFloat()` inside `getSpawnAsBabyOdds` - which is **static** in 26.3 and therefore has no mob and no level, so it cannot ask which version it is in. The easy workaround would be a global flag, which would apply to every world at once and break the per-world guarantee, so it is left unported and recorded as needing a different hook where the level is known. A gate that cannot see the world is not a gate.

**The verification loop earned its keep again.** The first run of this batch **failed loudly**: `@Shadow field mob was not located in the target class FleeSunGoal`. Both goals declare their own `mob`, but as a `PathfinderMob` - Mixin resolves a shadow by name **and descriptor**, so a `Mob`-typed shadow does not match. Corrected to `PathfinderMob`, after which the probe passes with no mixin failure. That is the second time this session that a wrong hook was caught by running rather than by reading; the required mixin config is why it cannot be missed.

**Two research jobs are now running as subagents**, because they are the bottleneck and they are independent of the porting:

1. **`research/feature-timeline.json`** - when every feature was added or removed, from the Nostalgic Tweaks catalogue's 489 entries plus blocks, mobs and structures, with a source and a quote per entry, primary-jar where possible and wiki where not, and explicit `UNVERIFIED` where nothing can be sourced.
2. **`research/block-palettes/`** - a block palette per version, extending the Beta 1.7.3 extractor that already works, with the hash-verified-jar rule and the "reproduce the reviewed 83-name set exactly" self-check. The hard problem handed to it is Alpha and Infdev, which do not carry plain block-name strings the way Beta 1.7.3 does.

**240 tests / 28 suites / 0 failures**, 18 verdict negatives rejected, `git diff --check` clean. Worklist: **78 server gameplay tweaks, 8 touched, 70 to port**.

### Previous milestone — the 1.9 combat model is gone from every declared version, ported from NT

**Six tweaks are now ported from Nostalgic Tweaks, three of them this round, all gated by the world's version so an upgrade changes them automatically.**

The combat batch is the strongest case yet, because the boundary is not a guess: the **attack cooldown, its damage scaling and the sweep attack are all the 1.9 combat update**, and every version this project declares is Release 1.6.4 or older - with the 1.8.9 target older still. So these apply to **all declared versions**, not only the earliest ones:

| gate | upstream tweak | applies to |
| --- | --- | --- |
| `attackCooldownDisabled` | `DISABLE_COOLDOWN` | **every declared version** (1.9 is beyond all of them) |
| `sweepAttackDisabled` | `DISABLE_SWEEP` | **every declared version** |
| `criticalHitDisabled` | `DISABLE_CRITICAL_HIT` | Infdev, Alpha, Beta 1.7.3 - the boundary above 1.7.3 is unverified, so it stops there |

**26.3 had refactored the code upstream hooks into, which is exactly why reading upstream matters.** In 1.21.4 Nostalgic Tweaks suppresses critical hits by making the climb check answer true inside `Player.attack`, and empties a `getEntitiesOfClass` list for sweep attacks. In 26.3 `Player.attack` was split apart: the critical check is now its own method `canCriticalAttack`, sweep eligibility is `isSweepAttack`, and the entity-list call no longer sits inside `attack` at all. Upstream's sweep target **could not have been carried across unchanged**. Both are therefore modified at their return value, which is narrower than upstream's expression rewrites and says what it means. The cooldown port keeps upstream's `getAttackStrengthScale` pinning exactly; upstream's extra attack-strength-ticker reset only affects the client's cooldown indicator, which this project does not draw.

**Verified rather than assumed:** the probe passes with **no mixin failure** for all three new injections, which is meaningful because the mixin config is `required` - a wrong target fails loudly at class load. **240 tests / 28 suites / 0 failures**, `git diff --check` clean.

**Worklist state, from `research/nt-port-worklist.json`:** of 78 server-side gameplay tweaks, **6 touched, 72 to port**; 384 client tweaks and 91 server tweaks remain untriaged in total, the lighting work among them.

### Previous milestone — NT porting starts for real, with a worklist instead of hand-picking

**My hand-rolled HUD layout adjustment is removed.** It shifted the hearts row by a guessed 10 pixels and pushed the hearts onto the hotbar, which a screenshot showed immediately. The project owner's instruction - stop hand-rolling, use Nostalgic Tweaks' implementation - was correct, and the file is deleted rather than tuned.

**`origin/26.1.x` was checked before assuming 1.21.4 was the best source.** It is Nostalgic Tweaks 2.0: a rewrite, 591 files, different package layout, and `HIDE_HUNGER_BAR` does not appear under that name. So the **1.21.4 branch is the substantive reference**, as the project owner directed, and its revision `010526de` is what everything below cites.

**Three tweaks are now ported faithfully, with the gate changed from a global config flag to the world's version.** Every one reads upstream's actual implementation rather than a paraphrase of it:

| our mixin | upstream | what was kept |
| --- | --- | --- |
| `RangedBowAttackGoalHistoricalMixin` | `fabric/.../mob_ai/RangedBowAttackGoalMixin.java` | the `WrapWithCondition` on `MoveControl.strafe` inside `RangedBowAttackGoal.tick`, the `AbstractSkeleton` check, **and the look-control re-aim** - without it a suppressed skeleton fires at a target it is no longer facing, which looks broken rather than old |
| `MobHistoricalItemPickupMixin` | `mixin/tweak/gameplay/monster_rule/MobMixin.java` | the `WrapWithCondition` on `Mob.pickUpItem` in `aiStep`, restricted to `Monster` so allays, foxes and players are untouched |
| `AnimalHistoricalBreedingMixin` | `mixin/tweak/gameplay/animal_spawn/AnimalMixin.java` | only the `isFood` expression modification; upstream's file also changes animal removal, which is a different tweak and is deliberately not carried across |

Because the gate reads the world's version at runtime, **an upgrade changes all three at once** with no separate migration.

**And a worklist now exists, because picking three at a time is not a plan.** `scripts/plan-nt-ports.py` turns the catalogue into an ordered list with each tweak's upstream carrier file and whether this project has touched it: **78 server-side gameplay tweaks, 75 unported**, plus 384 client tweaks of which 275 are eye candy. The unported list contains exactly the things that were asked for and are simply missing - `DISABLE_CRITICAL_HIT`, `DISABLE_SWEEP`, `DISABLE_COOLDOWN`, `DISABLE_MISS_TIMER`, `ATTACK_WHILE_SWORD_BLOCKING`, `DISABLE_ANIMAL_TEMPTING`, `DISABLE_ANIMAL_PANIC`, `DISABLE_MONSTER_AVOID_SUN`, `ANIMAL_SPAWN_CAP`, `DISABLE_MONSTER_ARMOR_SPAWN`, `DISABLE_SHEEP_EAT_GRASS`, `CART_BOOSTING`, `ALWAYS_OPEN_CHEST`.

**The real bottleneck is not the porting, and it is worth stating plainly.** Nostalgic Tweaks has **no version-to-tweak mapping** - only user-saved config presets - so "which tweak applies to which version" is this project's own research problem, per tweak. That is why the three ports above apply to Infdev, Alpha and Beta 1.7.3, where the systems provably did not exist, and are **not applied to Beta 1.8.1 and later, where the boundary is unverified**. Filling in those boundaries is the work that unlocks the remaining ~470 tweaks, and guessing them would recreate the exact class of defect this project keeps having to undo.

**Verified:** all three mixins apply against real 26.3 targets with no injection failure (the mixin config is `required`, so a wrong target fails loudly rather than silently doing nothing), the runtime probe passes, and **240 tests / 28 suites / 0 failures** with `git diff --check` clean.

### Previous milestone — we control structures ourselves now, and client-side rules finally exist

**Structures: the leak was real and is now closed for the early versions.** A Beta 1.7.3 world's eligible structure sets contained **30 modern structures** — ocean monuments, pillager outposts, trial chambers, ruined portals, woodland mansions, shipwrecks, buried treasure — because Moderner Beta declares the vanilla structure sets and leaves them to the world option. Until now this project handled that by *asking the player to untick Generate Structures*, and the probe **refused any world that had the option on**. That is advertising a guarantee, not enforcing one, and it is exactly the difference the project owner identified.

- **Per-version structure rules** now exist (`HistoricalStructureRules`), with an empty allowed set that *restricts* meaning "no structures at all" — the same shape the spawn rules use, and for the same reason: an empty set must be able to mean "nothing" rather than "unspecified".
- **Enforced at generation time** by a mixin on `Structure.generate` that returns `StructureStart.INVALID_START`, the game's own value for "nothing here". Nothing is generated and then removed, and no caller can tell a suppressed structure from one that did not fit.
- **Verified with structures ON, which is the case that used to be refused:** `structure.suppressedAttempts=45` across **21 distinct structures** (all eight ruined-portal variants, five village variants, shipwrecks, buried treasure, ocean ruins, mineshafts, ocean monument, jungle pyramid) and `structure.startsByChunk={}` — zero generated — with the probe now **passing**.
- **Only defended boundaries are enforced.** Infdev, Alpha and Beta 1.7.3 get "no structures"; Beta 1.8.1 and later are deliberately **left alone and recorded as unresearched**, because their boundaries are not yet verified in this project and inventing them from memory is the failure mode it keeps having to undo. A test asserts the *absence* of those rules so that adding one must be deliberate.

**Client-side rules exist at all now, which they did not before.** Every rule was server-side, which is why the rules were invisible: hunger never drained while the hunger bar still drew, and sprinting was refused for a `ServerPlayer` while the client's own player sprinted regardless. Now there is a client-side rules resolver fed by the integrated server in single-player and by a **server push on join** for multiplayer, the **hunger bar is removed** where the version had no hunger system, and **client-side sprint denial** applies to `LocalPlayer`.

**240 tests / 28 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — the upgrade crash is fixed, and gameplay enforcement was real but invisible

**The crash: found, fixed, and covered by a test that uses the loader's own rule.** Upgrading a Beta 1.7.3 world to Beta 1.8.1 stored profile `beta_1_7_3` with generation preset `beta_1_8_1_cave_fix` — a document claiming to be one version while generating another — and the next start refused to load it. **The refusal was correct**: the world would otherwise have kept Beta 1.7.3's gameplay rules on top of Beta 1.8.1 terrain, which is precisely the kind of half-applied state fail-closed exists to prevent. Writing that state was the defect. The upgrade now takes the target profile from the **binding for the target preset**, so the two halves cannot be written from different sources and then disagree, and a within-version move still changes only the recipe. My own harness hit this bug first and I misread it as a server-start timing problem instead of reading the log; that is a lesson about my diagnosis, not about the code.

**And the gameplay complaint is correct, for a reason visible in the code.** Enforcement existed only on the server, and it was not something a player could feel:

| feature | what was implemented | what a player saw |
| --- | --- | --- |
| sprinting | `setSprinting(true)` denied, but only when `self instanceof ServerPlayer` **and** the level is a `ServerLevel` | sprinting worked, because in single-player the client computes the movement |
| hunger | `FoodData.tick` cancelled, so hunger never drains | **the hunger bar still rendered** — no client-side presentation existed at all |
| experience | suppressed server-side | XP bar and orbs still shown |
| combat | **nothing at all** | identical to modern Minecraft |
| client | no client mixin existed | the game looked modern whatever the rules said |

**The first client-side enforcement is now in.** A client-only mixin denies sprinting on `LocalPlayer` in single-player, taking the rules from the integrated server so it uses the same authority as the server side. It is registered in the mixin configuration's `client` list and fires only for `LocalPlayer`, so it cannot affect a server or a fake player. **It compiles and is registered; I have not yet observed it in a running client**, and on a multiplayer server it does nothing at all, because the rules are not sent to the client yet.

**On Alpha, being exact:** `historical_experience:alpha` is declared, appears in the create-world list, and its mob set and food values are probe-verified. It has **never been demonstrated in a playable client**, and calling that "done" was overstatement. The same is true of the other declared versions: verified by measurement, not played.

**234 tests / 27 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — Beta 1.7.3 passes: leaf litter and vines are now suppressed where they are placed

**Your suggestion was right, with one refinement that matters: the block is never written, rather than written and then replaced with air.** That distinction is the difference between generation-time suppression, which this project permits, and generate-then-remove, which it forbids. The hook cancels the write, so the block is absent from the chunk as the chunk is built.

**Where the hook goes, from the sources rather than from guessing.** Leaf litter and vines are both placed from *inside* tree features, which is why excluding the feature was never an option — it would delete the forest's or swamp's trees. Reading the 26.3 sources gave the shape:

```java
// leaf litter is a tree decorator, selected by Moderner Beta on this Minecraft version
sparseLeafLitter = new PlaceOnGroundDecorator(96, 4, 2, ...leafLitterPatchBuilder(1, 3));
register(OAK_BEES_0002_LEAF_LITTER, createOak(...).decorators(List.of(beehive0002, sparseLeafLitter, thickLeafLitter)).build());
//? if >=1.21.5 { TreePlacements.OAK_BEES_0002_LEAF_LITTER; } else { TreePlacements.OAK_BEES_0002; }
```

and, decisively, that **every** tree decorator writes through one funnel:

```java
public void placeVine(BlockPos pos, BooleanProperty direction) {
    this.setBlock(pos, Blocks.VINE.defaultBlockState().setValue(direction, true));
}
public void setBlock(BlockPos pos, BlockState state) { this.decorationSetter.accept(pos, state); }
```

So a single hook on `TreeDecorator.Context.setBlock` covers leaf litter, vines and anything else of the same shape. My first attempt redirected the `placeVine` call sites and **failed to apply**, because those calls sit inside a lambda and therefore in a synthetic method — the hook is now on the one method that cannot be inlined away.

**Why a redirect on the write rather than a check on the state provider:** sampling the provider to decide would consume a random value and change every later placement in the chunk. Reading the state *after* it is computed adds no randomness, so historical worlds stay deterministic.

**And it works, verified by census rather than by assertion.** A fresh 289-chunk Beta 1.7.3 world now reports:

- `generation.versionAnachronisticBlocks={}` and `generation.anachronisticBlocks={}` — **the first time this version is clean on both checks**
- **leaf litter: 1,566 → 0.** Vines: gone. Bee nests: gone.
- Era-correct decoration survived untouched: dandelions 12, poppies 4, short grass 345, sugar cane 25, brown and red mushrooms, oak and birch logs and leaves. Suppression removed the anachronisms and nothing else.

**Two further defects were found on the way, both by the check rather than by looking.** The first run with the hook revealed **`bee_nest`**, which the suppression could not recognise because bee nests were not in the modern-to-legacy table — a mapped-but-wrong table is silent, an unmapped block is at least reported. Mapping them cleared it. And checking the palette's completeness showed it is **incomplete**: `cocoa` does not occur in the block class at all, although Beta 1.7.3 certainly had it. That is now recorded as a known limitation, and it is safe in the direction that matters — a block missing from the palette *and* from the mapping table is reported UNKNOWN and is never suppressed, so the worst case is an unreported defect rather than a wrongly removed historical block.

**232 tests / 27 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — the anachronisms were all coming from one place, and reading Moderner Beta's code found them all at once

**Instead of censusing worlds one defect at a time, I read Moderner Beta's own biome-feature code — and it lists every vanilla placed feature it adds to a historical biome.** That turns "discover an anachronism per run" into "enumerate them", and it explains every defect this project has found so far:

| vanilla feature MB adds | what it places | verdict |
| --- | --- | --- |
| `forest_flowers`, `flower_plains`, `flower_swamp` | rose bush, peony, lilac, lily of the valley, cornflower | **1.7.2 and 1.14 — excluded in revision 9** |
| `patch_melon_sparse` | melons | **Beta 1.8 — excluded in revision 9** |
| `sea_pickle`, `warm_ocean_vegetation` | sea pickles, coral, seagrass | **1.13 — excluded in revision 9** |
| `patch_waterlily` | lily pads | later than Beta 1.7.3 — excluded in revision 9 |
| `kelp_*`, `seagrass_*` | kelp and seagrass | 1.13 — excluded in revision 7 (2,712 blocks) |
| `glow_lichen`, `ore_tuff`, `ore_granite/diorite/andesite` | modern stone and lichen | excluded from the start |
| `trees_swamp` | swamp trees **carrying vines** | Beta 1.8 vines, inside the tree |
| MB's `OAK_BEES_0002` | Beta oak trees | **leaf litter, inside the tree** |

**Revision 9 excludes the seven that can be excluded, and the census confirms it.** A 289-chunk Beta 1.7.3 world now reports `unknown=0` and contains **no vine, rose bush, peony, lilac, lily of the valley, melon, sea pickle, kelp, seagrass or firefly bush** — with dandelions and poppies still present (12 and 3), which is the era-correct content Moderner Beta places itself. Removing the vanilla flower features therefore cost nothing historical, exactly as the code said it would.

**And the last one is now fully explained, from the source rather than by inference.** Leaf litter cannot be excluded because of a deliberate version conditional in Moderner Beta:

```java
//? if >=1.21.5 { TreePlacements.OAK_BEES_0002_LEAF_LITTER; } else { TreePlacements.OAK_BEES_0002; }
```

On 26.3 there is no non-litter oak placement for it to use, so **Moderner Beta's "Beta oak tree" IS the leaf-litter variant**. Excluding the feature deletes the forest's trees, and overriding Moderner Beta's data would change ordinary worlds too, which the architecture forbids. It therefore needs **generation-time suppression of the block** — a narrowly scoped hook, which the architecture permits precisely where configuration cannot express a per-world restriction. `leaf_litter` (1,566 blocks in this sample) and vines are the only two defects left in this class, and they are the same problem.

**232 tests / 27 suites / 0 failures, 18 verdict negatives rejected.** Beta 1.7.3 still reports FAIL, now on a single known block placed by a single known mechanism.

### Previous milestone — the check is version-aware now, and it immediately found two more defects

**The palette is wired in, and a generated world is judged against its own version.** `generation.versionPalette=historical_experience:beta_1_7_3 size=83 checked=31 unknown=0` — every block the world contains is now recognised, and the question asked is the right one: could *this* version have had it? That is the question the version-agnostic list structurally cannot ask, so it could never flag `vine`: vines are Release 1.6.4 content and therefore on no "later than everything" list, yet **Beta 1.8 introduced them**.

**The modern-to-legacy table is hand-written, and that is the point.** The obvious implementation — turn the modern id into camelCase and look it up — is wrong exactly where this project cares: modern `minecraft:cobblestone` is Beta 1.7.3's `stonebrick`, while modern `minecraft:stone_bricks` is Beta 1.8's `stoneBrick` and did not exist in 1.7.3 at all. A rename rule gets one of those wrong whichever way it errs. Seven tests pin the boundary, including that pair.

**Two real bugs were caught by a guard I added to the table itself.** A duplicate mapping for one modern id would silently replace the earlier entry, and could turn a correct block into a reported defect, so the table now throws during class initialisation on a contradiction. It caught two:

- `minecraft:white_terracotta` mapped to **both** `hardenedClay` and `stainedClay` — they are genuinely different blocks, uncoloured terracotta against the stained set.
- `minecraft:large_fern` and `tall_grass` mapped into `tallgrass`, which is Beta 1.7.3's single-height grass. The double-height forms are Beta 1.8's `doublePlant`, and folding them together would have made a Beta 1.8 plant look legitimate.

**And the coverage counter earned its place immediately.** The first run reported `minecraft:rose_bush` as **UNKNOWN** rather than acceptable — rose bushes are **1.7.2** content, so they are wrong in every declared version except the Release 1.7.10 target. An unknown block is an unverified block, and reporting it as its own line is what turned "we could not say" into a defect. The same reasoning mapped the rest of the 1.7.2 flowers (peony, sunflower, lilac, azure bluet, oxeye daisy, blue orchid, allium and the four tulips), after which the run reports **unknown=0**.

**Finding things by measuring rather than by remembering is now the pattern, and it keeps paying.** Every anachronism this project has actually fixed — 2,712 aquatic plants, the firefly bushes, the rose bushes, the terracotta contradiction — was found by a check or a guard, not by recalling a changelog. The hand-written "too new" list was wrong twice in exactly the places nobody thinks of: the newest decorative plants.

**Still failing, deliberately:** `leaf_litter` (1,758 blocks in this sample) is reported by both checks and is not fixed, because it is placed as a decorator on the tree features themselves and excluding it would delete the forest's trees. It needs a replacement feature. Beta 1.7.3 therefore reports FAIL, with a known and quantified cause.

**232 tests / 27 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — the Beta 1.7.3 block palette, re-extracted properly and independently checked

**The rejected extraction has been redone, and the controls are the point.** `research/beta173-availability.json` carries `REJECTED_DO_NOT_CONSUME` because it included Beta 1.8 content, omitted wool block 35, and conflated wool with dye metadata; its recorded required next step was re-extraction from a hash-verified jar with provenance-bound tooling and introduction-version negative controls. `scripts/extract-b173-palette.py` is that tool, and `research/beta173-block-palette.json` is its output.

- **The jar is verified, not assumed.** The run refuses unless the SHA-1 is `43db9b49…bb45` and the size is 1,465,375 bytes.
- **The parser is ours and complete.** 747 classes parsed, **747 to their exact last byte**, zero failures. Getting there required fixing a real bug in my own parser: it skipped `access_flags`, `this_class` and `super_class` between the constant pool and the interfaces, so the walk silently ran away — caught by diffing the class header against `javap`, not by guessing. (The earlier claim that these jars need a non-standard 4-byte attribute length was wrong: `attribute_length` is u4 in the specification all along.)
- **The controls decide acceptance**, and the script exits non-zero if they fail: Beta 1.8 names must be absent, and wool, the six ores and the rest of the era's blocks must be present.

**83 block names came out**, and seven independent checks all pass:

```
stone wood gravel grass cloth sand dirt stonebrick sapling bedrock water lava
oreGold oreIron oreCoal log leaves sponge glass oreLapis blockLapis dispenser
sandStone musicBlock bed goldenRail detectorRail pistonStickyBase web tallgrass
deadbush pistonBase flower rose mushroom blockGold blockIron stoneSlab brick tnt
bookshelf stoneMoss obsidian torch fire mobSpawner stairsWood chest redstoneDust
oreDiamond blockDiamond workbench crops farmland furnace sign doorWood ladder
rail stairsStone lever pressurePlate doorIron oreRedstone notGate button snow
ice cactus clay reeds jukebox fence pumpkin hellrock hellsand lightgem portal
litpumpkin cake diode lockedchest trapdoor
```

The checks that matter most are the ones that would have caught the previous failure: **exactly six ores and no emerald**; **`cloth` present**, the very block the rejection was about; **no melon, vine, fence gate, monster egg, glass pane, iron bars or Beta 1.8 stair families**; and the boundary drawn at the right block — **`stairsStone` (cobblestone stairs, id 67) is present while `stoneStairs` (stone-brick stairs, id 109) is absent**, which is exactly the Beta 1.7.3/Beta 1.8 line. Two further checks confirm the set is not merely plausible: **pistons are present**, because Beta 1.7.3 is the version that introduced them, and **`lockedchest` is present**, an unused block that exists in 1.7.3 and is gone by 1.8.

The artifact records `ACCEPTED_FOR_VERSION_CHECKS` — for asking whether a given block could have existed in Beta 1.7.3 — and explicitly **not** for gameplay enforcement, because the tool recovers names only and deliberately does not pair them with numeric ids. A positional id pairing is how the previous artifact went wrong, so it is not attempted.

**What this unlocks, and what it still needs.** The anachronism check can now become version-aware, which is the only way to catch the defect the census exposed: `vine` is Release 1.6.4 content and so passes the current version-agnostic list, yet vines were **added in Beta 1.8** and are wrong in a Beta 1.7.3 world. Doing that needs a reviewed modern-id-to-legacy-name table, and the interesting entries are the ones where the mapping is not a simple rename: modern `cobblestone` is legacy `stonebrick` while modern `stone_bricks` is legacy `stoneBrick`, which is precisely how the two eras are told apart. That table is the next step, and it must be written carefully rather than generated by a rename rule.

**225 tests / 26 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — here is literally every block in a Beta 1.7.3 world, and two of them are still wrong

**The complete census of a 289-chunk Beta 1.7.3 world** (this is the direct answer to "what is actually in the world"):

```
bedrock 222560, stone 3380705, dirt 372759, grass_block 6923, sand 89089, sandstone 37768,
gravel 114788, clay 344, water 288087, lava 634, cobblestone 1187, mossy_cobblestone 457,
coal_ore 42198, iron_ore 24317, gold_ore 2414, diamond_ore 854, lapis_ore 903, redstone_ore 6799,
oak_log 802, oak_leaves 11633, birch_log 97, birch_leaves 885, short_grass 278, dandelion 12,
poppy 4, brown_mushroom 27, red_mushroom 10, sugar_cane 54, chest 10, spawner 10, vine 4,
leaf_litter 1786
```

Most of that is a correct Beta 1.7.3 palette: six ores and no emerald, no copper, no deepslate, no granite/diorite/andesite, dungeons with mossy cobblestone, chests and spawners, oak and birch, dandelions and poppies, sugar cane, clay, and bedrock at the floor. **Two entries are wrong**, and both were found by this census rather than by reading a changelog:

- **`firefly_bush` × 7 — found and fixed.** It is 1.21.5 content and was not on the original "too new" list, because a hand-written list has holes exactly where the newest *decorative plants* are. Excluded as **revision 8** (three placed features), re-measured: gone.
- **`leaf_litter` × 1,786 — found and NOT fixed, deliberately.** Its blocks are placed as a decorator on the tree features themselves. Checking the biomes proves it: `forest` places `minecraft:trees_birch_and_oak_leaf_litter`, so excluding it would **delete the forest's trees**, while `dark_forest`'s `minecraft:patch_leaf_litter` is a separate feature that could safely go. Litter therefore needs a **replacement** feature, the way the ore population replaced vanilla placement. Until then the census reports it, and **the newest Beta 1.7.3 revision reports FAIL** — which is the correct verdict, not a reason to narrow the check.

**And the census exposed a limit in the check itself, which matters more than either fix.** The anachronism list is deliberately version-agnostic: "blocks no declared version can contain". So it can never flag **`vine` × 4** in a Beta 1.7.3 world — vines are Release 1.6.4 content and therefore on no such list, yet vines were **added in Beta 1.8**, so they are anachronistic here. This is the version-specific class the earlier milestone predicted, and it is the same gap the rejected `beta173-availability.json` was meant to fill: that extraction was rejected for mixing in Beta 1.8 content (melon, enderman, blocks 97-109), omitting wool block 35, and conflating wool with dye metadata, with a required next step of re-extraction from the hash-verified jar using provenance-bound tooling and introduction-version negative controls. That jar is present and hash-verified, and the class parser from the same work survives, so the re-extraction is bounded work — and it would let the check ask the right question per version: "could *this* version have had this block?"

**225 tests / 26 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — the world now judges itself, and it immediately found 2,712 blocks that should not exist

**The gap you identified was real and I had not measured it.** The probe counted every generated block but never *judged* them, so "are there blocks here that could not have existed?" had no answer. There is now a curated list of blocks whose introduction is unambiguously later than every declared version — copper, deepslate, amethyst, the lush-cave and dripstone families, the 1.13 aquatic and ocean families, the 1.14 village blocks, the 1.16 Nether family, and 1.19-1.21 content — and a generated world is checked against it, reported as `generation.anachronisticBlocks`. It is deliberately **one-directional**: an empty result means "none of these were found", never "the world is historically correct".

**First run on a 169-chunk Beta 1.7.3 world, and it failed immediately:**

```
generation.anachronisticBlocks = { kelp=405, kelp_plant=910, seagrass=833, tall_seagrass=564 }
```

**2,712 blocks that did not exist until 1.13**, in oceans that in Beta 1.7.3 held nothing that grew. This is exactly the class of defect that had gone unnoticed: the eight original exclusions covered ores and stone variants, and nothing looked at vegetation.

**Fixed at generation time, as revision 7** — ten placed features (`kelp_cold`, `kelp_warm`, and the eight `seagrass_*` variants) excluded through the same mechanism the ores use, so nothing is generated and then removed. The exclusion set is a **new** constant rather than an addition to the shared one, because revisions 4, 5 and 6 already reference that and worlds exist on them. Re-measured on a fresh 169-chunk world: **2,712 blocks → 1**.

**And that one is a different kind of problem, so I am not going to call it fixed.** The residue is a single `minecraft:lily_of_the_valley` (1.14). It is placed by `flower_flower_forest` and `forest_flowers`, which are the *same configured features* that place dandelions and poppies — the flowers Beta 1.7.3 actually had. Excluding the feature would delete the historical flowers along with the anachronistic one, so this needs a **replacement** feature, the way the ore population replaced vanilla's placement, not an exclusion. Until then **the newest Beta 1.7.3 revision reports FAIL**, and that verdict is correct: I would rather it fail on a known, quantified, one-block defect than pass by narrowing the check.

**Also fixed this round, found by driving the operator command instead of reading it:** `/hex upgrade` used a `word()` argument, which cannot contain a colon, so `historical_experience:release_1_2_5` was **untypable** — the command could not accept its own version ids. It now takes `string()`, accepts both `release_1_2_5` and the full id, and suggests both. That defect was invisible to the unit tests and to a code read; it took a live run over RCON to surface, which is an argument for the live demonstrations this project keeps being asked for.

**225 tests / 26 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — playback and seeking verified, and the cave biomes were leaking 1.18 content

**Flashback playback and seeking are now verified, not assumed.** The fork publishes no way to open a replay — `/flashback` offers start, finish, pause, unpause, config and mark, and `openReplayWorld` is reachable only from the menu UI or programmatically — so I added a gated client probe that drives the fork's own public entry points reflectively (`openReplayWorld`, `getReplayServer`, `goToReplayTick`, `getReplayTick`, `getTotalReplayTicks`), keeping the fork out of the compile classpath exactly as the runtime-only rule requires. Run with `./gradlew runClientReplayVerify`, it opens the newest replay in the game directory and needs no arguments.

**The first version of that test was vacuous, and I caught it in my own output.** It sought to two thirds of the recording, which happened to be *exactly* where the replay had already advanced to (`tickAtOpen=1708`, `seekRequested=1708`), so "seek landed" passed without proving anything. It also re-wrote its verdict on every client tick, producing 15,016 identical checks. Both are fixed: the seek target is now chosen to differ from the current tick, a **second seek backwards** rules out plain fast-forwarding, and the verdict is written once. The result is 14 meaningful checks:

`totalTicks=2563` (matching the recording's own metadata), `tickAtOpen=0 → seek 2553 → tick 2553`, then **back to tick 0**, `seekLanded=true`, `backSeekLanded=true`, `PROBE_RESULT PASS`.

**And you were right about the cave biomes — the leak was worse than one mistake.** Moderner Beta's Beta preset assigns cave biomes through a voronoi diagram whose points literally name `minecraft:lush_caves` and `minecraft:sulfur_*`, so a "Beta 1.7.3" world was being carved with **1.18-era cave biomes**. The previous mitigation was to set the provider to `moderner_beta:none`, which is not a fix either: `CaveBiomeProviderNone.getBiome` returns **null**. Cave biomes did not exist before 1.13, so the historical choice is one era-appropriate biome.

**Fixed as new revisions, never as edits** — a settings preset *is* a revision's definition, and worlds already exist on the old ones, so editing them would silently change what those worlds generate. Beta 1.7.3 gains revision 6; Beta 1.8.1, Beta 1.9-pre-3 and Release 1.0/1.1/1.2.5/1.6.4 each gain revision 1 with a `single` cave provider and `moderner_beta:beta_taiga`. **Alpha and the two Infdev builds are deliberately untouched**, as you specified — their own upstream presets already declare no cave injection.

**Reading my own evidence caught the fix being incomplete.** The revisions existed, but a *newly created* world still reported `generationRevision=0`: the create-world entry pointed at the legacy alias, so the correction was only reachable by upgrading. Repointing the world preset — and only the world preset — fixes new worlds while existing ones keep the recipe they stored. Re-verified: `rawRecipePreset=…_content_preview_6`, `generationRevision=6`, `caveComponents=3`, `caveMatches=true`, `providersInitialized=true`. That last flag is the real proof, because `CaveBiomeProviderSingle` reads its biome with `getOrThrow`: MB could only have built that provider if the era biome resolved.

**Four test fixtures went stale, and the reason is worth keeping.** Each used `beta_1_7_3_content_preview_6` as its example of "a plausible name that was never declared" — a sound choice that my change turned into a real revision. The invariants were fine; the *examples* were. They now use `_content_preview_99`, and the upgrade test derives its revision number from the registry instead of pinning it, because a guard that must be hand-edited whenever a revision is added is one that will eventually be edited carelessly. **225 tests / 26 suites / 0 failures, 18 verdict negatives rejected.**

### Previous milestone — Flashback: a real replay of a historical world exists, and my earlier "blocker" was wrong

**Correction first, because the previous two entries on this subject were wrong and the project owner had to tell me so.** I reported that recording was "blocked by the environment" and then that "Flashback blocks world load". Both were false:

- **The environment loads worlds perfectly well.** The baseline client (Flashback disabled) loaded the world and generated its spawn area: `Preparing spawn area: 16% → 32% → 63%`, `Time elapsed: 2120 ms`, `level.dat` rewritten.
- **Flashback does not block world load.** With Flashback and a **260-second** window the same run also loaded the world (`Time elapsed: 933 ms`, `level.dat` rewritten). My 100-second window had simply expired while the client — now carrying Flashback plus its bundled ImGui, Lattice and commons-math3 — was still starting up. The thread dump proved it: the Render thread was in `renderFrame → FramerateLimiter.limitDisplayFPS`, i.e. **normally rendering the main menu**, with **no Flashback frames and no network frames anywhere**.
- **The sessionserver timeouts were background noise**, not a blocker: they appear on a `Download` worker thread and never appear in any stack that mattered.
- **One real defect found along the way was mine:** the server's own default is `white-list=true`, and with an empty whitelist that rejects every connection. The harness now pins `white-list=false` and `enforce-whitelist=false` explicitly instead of inheriting a default.

**A real replay of a historical world now exists** — and it is important to be exact about how: **the project owner started the recording manually.** My harness did not automate the start, so auto-start remains unverified by me either way. What the session does establish is everything around it:

- The fork loads in a real 26.3 client alongside this mod (`flashback, historical_experience, lattice, moderner_beta`).
- The client joined the dedicated server running a **historical Beta 1.7.3** world (`Player769 logged in at (194.5, 64.0, -22.5)`).
- Flashback exported `run/client/flashback/replays/2026-10-13T03_08_12.zip`, **8,948,061 bytes**, and it is well-formed: `metadata.json`, `icon.png`, a level-chunk cache and the replay data, with `total_ticks=2563` (~128 seconds), `version_string=26.3`, `data_version=5023`, and **`historical_experience` recorded in its `modVersions`**.

**Two facts that decide what happens next.** First, the auto-start route: the fork arms it in `ClientPlayConnectionEvents.JOIN`, which is the *join a server* event — a genuine code fact — while the project owner confirms the config option works, so the supported route is Flashback's own config rather than a hand-written partial JSON file, which is what I had been supplying. Second, **playback has no command**: `/flashback` offers `start`, `finish`, `pause`, `unpause`, `config` and `mark`, and `openReplayWorld` is reachable only from the menu UI or programmatically. So the honest next step for playback and seeking is a **client gametest** — `fabric-client-gametest-api-v1` is already on the classpath — that calls `openReplayWorld` and then drives `ReplayServer.goToReplayTick` and reads the tick back. That is automation the client actually supports, rather than an attempt to type into an ImGui window.

### Previous milestone — Infdev's data arrives, "nothing spawns" breaks my model, and the sweep finds a broken preset

**The Infdev extraction is complete, under the archive-provenance standard you authorised.** All four builds came from Omniarchive Vault BETA with their SHA-1s checked against **independently published per-version metadata** — not against the downloads themselves. Every hash matched: 227 `78ba0037…`, 325 `99da3b55…`, 420 `27010a51…`, 611 `421318a5…`. The extraction method was validated first by reproducing Beta 1.7.3's 14 mobs and 10 food values exactly, and it independently recomputed `jc.class`'s SHA-1 to the same value a previous worker recorded.

**The findings are more interesting than "here are the mobs".**

- **No Infdev build has any food at all.** The health-restoration method exists in every jar and clamps at 20, and it has **zero call sites jar-wide**, with zero constant-pool hits for any food item and no hunger, saturation or exhaustion system. So an empty food table for these versions is a **measured fact**, not an unfinished extraction — and the entries now say exactly that.
- **Each Infdev build has a different mob situation, and 20100227 has none.** That jar has no entity-name registry and no registrar; a case-sensitive scan of every constant pool for every entity name returns zero hits; twelve entity classes and seven mob renderers ship, but **no mob is ever constructed by any class in the jar**. Its correct rule is therefore *"nothing spawns naturally"*.
- 20100325 has no name registry either, but constructs six mobs from a `nextInt(6)` switch (zombie, creeper, skeleton, spider, pig, sheep) — recorded as **inferred**, because that switch fits a natural spawner and would also fit an effect entity. 20100420 and 20100611 share a registry-backed set of seven (creeper, skeleton, spider, giant, zombie, pig, sheep) from byte-identical `<clinit>` code.

**"Nothing spawns" broke my model, and the model was wrong.** An empty allowed-spawn set meant *unrestricted* — "we have not researched this version". Infdev 20100227 needs the same empty set to mean *nothing may spawn*. Under the old meaning, the least mob-populated version of the game would have been given **modern** mobs and looked like success. `VersionRules` now carries an explicit `restrictsSpawns` flag separate from the set, the record refuses a rule set that claims not to restrict while naming allowed spawns, and six new tests pin the distinction in both directions.

**A useful method note from the extraction, for anyone parsing these jars again:** these are **not standard JVM class files** — the attribute-length field is **4 bytes**, not the spec's 2. With 2 bytes essentially every class fails to parse; with 4, all 747 of Beta 1.7.3's classes parse exactly to the last byte. That is the kind of thing that silently produces wrong answers rather than errors.

**And the verification sweep found that declared versions do not necessarily work — then found why.** I wrote `scripts/verify-all-declared-versions.ps1` to load the eight versions that had never actually been run, and its first case, **Infdev 20100227, hung the server during "Preparing level"** with no verdict at all. The sweep then hung *forever*, which taught a second lesson: when the hang happens **before** the probe starts, the probe's own watchdog never arms, so a naive sweep is unbounded. I replaced it with `verify-version-bounded.ps1`, which kills a run that produces no verdict inside a deadline.

**The provider is the discriminator, and it is measured.** Four Infdev builds, run one at a time under a hard timeout:

| build | upstream provider | result |
| --- | --- | --- |
| 20100227 | `moderner_beta:infdev_227` | **hangs**, no verdict |
| 20100325 | `moderner_beta:infdev_227` | **hangs**, killed at 170s |
| 20100420 | `moderner_beta:noise_3d` | **PASS**, 12/3/1 components all matching |
| 20100611 | `moderner_beta:noise_3d` | **PASS** |

The static comparison matches exactly: the two that hang declare `provider = moderner_beta:infdev_227` plus a unique `infdev_227_structures` component, while every version that loads — 20100420, 20100611, Alpha and all the Release ones — declares `provider = moderner_beta:noise_3d`. Same shape as the Classic/Indev finding: an upstream preset that does not generate here.

**So both were withdrawn**, with their resources and create-world listings, because a version that never loads must not be selectable. **Their extracted data is preserved** — 20100227 instantiates no mob of any kind, 20100325 constructs six from a `nextInt(6)` switch, and neither has any food — so the research survives and can be reinstated if the provider is fixed. Deleting it would only mean extracting it again.

**Ten versions are declared, six of them runtime-verified.** Beta 1.7.3, Beta 1.8.1, Alpha v1.1.2_01, Infdev 20100420, Infdev 20100611 and Release 1.2.5 have all loaded in real worlds and passed. Beta 1.9 Prerelease 3 and Release 1.0, 1.1 and 1.6.4 share the provider and envelope of versions that do load — which is a reason to expect them to work, and is **not** evidence that they do, so they are recorded as declared-but-unrun rather than counted as verified.

### Previous milestone — Flashback: the fork now loads in a real client, and the recording blocker is measured rather than assumed

Verified Flashback recording is a mandatory acceptance criterion, and the earlier audit concluded it could not be automated headlessly. That conclusion was about *automation*; this round went at the **runtime** question directly and got two concrete results.

**The fork now loads in a real Minecraft 26.3 client alongside this mod.** That is new and measured, not read: the client's own mod list for a real launch reads `flashback, historical_experience, lattice, moderner_beta`, on a real GPU (`OpenGL 3.3.0 NVIDIA 617.42`) in a real display session. Getting there required one non-obvious fact, found by failing: the plain `flashback-0.43.6.1.jar` loads and then dies with `NoClassDefFoundError: imgui/moulberry90/callback/ImStrSupplier` — it carries **none** of its dependencies (725 entries, zero ImGui classes), while `merged-0.43.6.1.jar` bundles them (1827 entries, 187 ImGui classes). The build now depends on the merged jar as a **dev-only local runtime dependency**: not compile-visible, never shipped, and its absence is not an error, so the build still works on a checkout without the clone.

**Recording still did not happen, and the reason is environmental and specific.** With the merged jar the client starts cleanly, reaches resource reload — and **never loads a world**. The copied world's region files are untouched and no replay directory is ever created. The launch stalls immediately after resource reload while `sessionserver.mojang.com` profile lookups time out; the dev account is set (`Setting user: Player807`) but the client cannot reach the session service from this environment. Recording needs a loaded client player — which the earlier audit established — so no world means no recording, and no harness work changes that. A second launch through a proper `clientQuickPlay` run configuration reproduced the identical state, which also removed one candidate cause: Gradle's `--args` replaces a `JavaExec` task's configured arguments, so the argument plumbing is not the problem.

**What would unblock it**, most likely first: outbound access to `sessionserver.mojang.com` from this machine, or a client dev account that needs no profile fetch. Both are environment changes rather than code changes, and neither is something this project can supply for itself.

**What this does not establish**: recording, playback or seeking against the fork. The build can now reach the point where they could be verified, and the remaining obstacle is outside the repository — but that acceptance criterion stays **unverified**, and this record is a blocker report rather than a result.

### Previous milestone — the finite-world question is answered, and Alpha gains verified gameplay

**The decisive experiment came back negative, and that is worth more than a workaround.** Last round I withdrew Classic and Indev because a Classic world hung on chunk (0,0) and I could only say the cause was "most likely" our dimension type and world preset. I ran the experiment that separates the possibilities: a world created **directly from Moderner Beta's own world preset** with `generator-settings={"preset":"moderner_beta:classic_0_30"}` — no project preset, marker, dimension type or wrapper involved at all. It applies its settings completely (14 chunk / 3 biome / 1 cave components, providers initialized) and then **hangs on chunk (0,0) until the watchdog kills the server**. So Moderner Beta's **finite path does not generate in this environment**. It is an upstream finding, not a defect here, and it means the withdrawal was not a limitation of this project's approach. It is recorded with the evidence so nobody re-attempts it expecting a different answer.

**Alpha v1.1.2_01 is no longer terrain-only: its mob set and food values are verified from its own client** (SHA-1 `daa4b9f1…`, 897,164 bytes, checked against Mojang's metadata first).

- **Ten mobs**, read from entity registry class `ew`: chicken, cow, creeper, giant, pig, sheep, skeleton, slime, spider, zombie. Ghasts, zombie pigmen, squid and wolves are **confirmed absent as constants anywhere in the jar**, not merely missing from a list.
- **Six food heals**, each proved from that jar's *own* contents rather than by analogy to Beta: apple 4 (it is the centre of the golden-apple recipe), bread 5 (its recipe), mushroom stew 10 (its recipe plus the bowl-returning subclass), raw porkchop 3 (the pig's drop method), cooked porkchop 8 (the smelting table), golden apple 42. Alpha has **no** cookie, fish or cake — its item registry holds ids 0–90 only.
- The extraction method was validated by **reproducing Beta 1.7.3's 14 mobs and 10 food values exactly** before it was trusted on an older jar, and it independently re-identified Beta 1.8.1's registry as the known class. That self-check is why these values can be used at all.

**Measured at runtime, and demonstrably different from Beta 1.7.3** — one shared implementation, two rule sets from two separate extractions ([Alpha](<logs/continuation-302-alpha-verify.log>)):

| spawn lists | Beta 1.7.3 | Alpha v1.1.2_01 |
| --- | --- | --- |
| creature | chicken, cow, pig, sheep, **wolf** | chicken, cow, pig, sheep |
| monster | creeper, skeleton, slime, spider, zombie | the same five |
| water | **squid** | — |
| removed modern types | bat, enderman, witch, zombie_villager, glow_squid, turtle | the same, **plus wolf and squid** |

Alpha's verdict is **PASS at 116 checks**, including bread at a **full** food level being accepted and healing exactly its verified 5 points.

**A provenance blocker, measured rather than assumed — and then resolved by an explicit decision.** An extraction attempt for the four Infdev versions found that **Mojang does not publish them at all** — absent from all three official manifests (918 versions each), with `inf-20100618` the only published Infdev id and a different build. Rather than let that sit as an unexplained gap, I put the decision to the project owner, who authorised **archive-hosted jars** under conditions: the SHA-1 must be verified against a value published by a source **independent of the download** (an archive index or hash list, not the file itself), the archive, URL and hash source are recorded per jar, provenance is labelled archive-provided rather than Mojang-distributed, and a jar with no independently published hash must be flagged rather than treated as verified. That extraction is running now. The standard is recorded in the version framework so a widening for builds Mojang no longer distributes cannot quietly become the standard for builds it does.

### Previous milestone — I tried to add Classic and Indev, and measurement says they cannot be offered yet

This is a negative result with a precise diagnosis, recorded because the next attempt should start from it rather than repeat it.

**Why they are different, and why the difference is real.** Classic and Indev are **finite** worlds in Moderner Beta: their presets use a different chunk provider (`moderner_beta:finite_2d`) and carry components the infinite versions do not have — `finite_level_properties`, `finite_noise`, `finite_pools`, `finite_beaches`, `finite_cave_generation`, `world_border`, `spawn_indev_house`. So they must **not** take the reduced-envelope substitution every other version uses: pointing a finite world at the shared 0..127 generator would replace a finite generator with an infinite one. I checked Moderner Beta's own shipped layout first and found it ships exactly **one** dimension type — a replacement of vanilla `minecraft:overworld`, which this project already copied verbatim — and one world preset. From that I concluded the finite extent must come from those components, implemented three versions accordingly, and gave them settings presets that inherit the upstream preset whole with only deepslate changed.

**What happened.** A Classic 0.30 world **loads, applies its preset completely** (14 chunk components, all matching, providers initialized) and **resolves its own marker** — everything except generation. Then a chunk request never returns. Measured twice: ring-sampling at radius 1, and at radius 0, where it hangs on **chunk (0,0), the very centre**. The server watchdog killed both runs after 60 seconds.

**So they are withdrawn, completely.** The registry entries, the five resource files, the create-world entries and the world-preset tag entries are all removed. A version that hangs the server must not be selectable, and half-offering one is worse than not offering it.

**What would settle it.** The most likely cause is the pairing: a *copied* dimension type under a project-owned id plus a project-owned world preset may not be what the finite components expect, even though the copy is byte-identical to Moderner Beta's. That is a hypothesis, not a finding. The decisive experiment is to generate a finite world **directly from Moderner Beta's own world preset** and see whether it generates at all — which separates "our wrapper is wrong" from "finite worlds do not work in this environment", and the fix differs completely between those two answers.

**Two harness defects this exposed, both fixed.** The probe could **hang the server** with no timeout and no indication of where: it now logs every chunk coordinate *before* requesting it, so a hang identifies itself in the log, and the radius is a run parameter (`-PprobeChunkRadius`) because a finite world has no chunk at every coordinate. A verification harness that can kill the server it is verifying is worse than one that reports a failure.

### Previous milestone — the version range now reaches back to Infdev, and the inference is labelled as one

**Twelve versions are declared**, spanning Infdev 20100227 to Release 1.6.4: Beta 1.7.3, Beta 1.8.1, Beta 1.9 Prerelease 3, Release 1.0, 1.1, 1.2.5, 1.6.4, and now **Infdev 20100227, 20100325, 20100420, 20100611 and Alpha v1.1.2_01**.

**Five more versions cost one registry helper and two resource files each** — the same pattern as before, because each was verified to declare Moderner Beta's `overworld_128` upstream, the reduced envelope this project has already validated. The registry test validated all five automatically, and the create-world label test *failed* until every new version had an honest label, which is that test doing its job.

**Their simulation rules are an inference, and every entry says so.** Hunger, sprinting and experience are **verified absent** at Beta 1.7.3 (that client has no food, saturation or exhaustion concept and no sprint binding) and **verified present** at Beta 1.8.1 (it has a FoodStats-shaped class and XP orbs). A feature absent at 1.7.3 and present at 1.8.1 is therefore absent at every version preceding 1.7.3. That is sound rather than a guess — but it is still an inference, so it does not borrow the authority of the Beta 1.7.3 measurements, and the evidence string says which is which.

**What is not researched is left modern and named, not invented.** For these eras the mob set, mob drops, food heal values, ore distribution and decoration are unmeasured, so the entries leave them unrestricted and state that in the evidence string, in the world-creation description, and in the generator label. This is the discipline the villager mistake taught: a plausible value borrowed from a neighbouring version looks like evidence and is worse than a stated gap. An extraction of the pre-Beta entity registries and food values is running now to fill those in.

**Verified in a real world.** Alpha v1.1.2_01 establishes **PASS at 118 checks**: complete preset application (12 chunk, 3 biome, 1 cave components all matching, providers initialized), `version.resolved=historical_experience:alpha`, `hasStructures=false`, a sprint request **denied** with the rule recording it, and an exhausting food tick leaving the food level at 10 — the inferred pre-Beta rules, applied and measured ([log](<logs/continuation-284-alpha.log>)).

**The probe gained a version too, by failing on the second one.** Its food assertion branched on "no hunger ⟹ has a researched heal table", which was true while Beta 1.7.3 was the only such version. Alpha has no hunger and no extracted heal values, so the assertion would have failed a *correct* Alpha world. It now branches on the absence of hunger and asserts the recorded gap explicitly, so eating-heals-nothing cannot silently become eating-heals-something-wrong.

### Previous milestone — the upgrade is now reachable by an operator, not only by the test harness

Three rounds running I recorded the same gap: "still missing: the operator-facing entry point". It exists now, and the split behind it is the point.

**The logic is separated from the command.** `HistoricalUpgradeService` holds every decision and is unit-tested against real world directories with no server; `HistoricalCommand` only parses arguments and renders text. That is what stops the rules drifting into three copies once a probe, a command and any future interface all need them — and it makes the operations testable at all, which a Brigadier tree is not.

**Four subcommands**, all gated on `COMMANDS_GAMEMASTER`:

- `/hex status` — the world's profile, stored generator, document revision (explicitly *not* a recipe number, since conflating the two is the mistake this codebase has already made once), the declared version's whole rule set, and whether an upgrade is pending.
- `/hex versions` — every version this build declares, so a mistyped target can be corrected.
- `/hex upgrade <version>` — stages the upgrade.
- `/hex recover` — runs recovery without waiting for a restart.

**The reports say what is *not* yet true.** A staged upgrade answers with "the stored generator is deliberately NOT changed yet… it is rewritten when the server stops", because an operator who expects an immediate change would otherwise reasonably conclude it had failed. Recovery says the same about the generator half, so a partial result can never be read as a complete one. A refusal states what exists instead, and asserts that nothing was changed.

**Registration is verified against the live dispatcher**, not the source. A command can compile, pass its unit tests, and never be registered, because registration goes through a Fabric event no test can fire — a real failure mode that is invisible in the code. The probe now reads the running server's command tree and requires the root plus every useful subcommand and the version argument:

```
CHECK command.registered=hex[recover, status, upgrade, versions]
```

That check passes and the verdict is **PASS** ([log](<logs/continuation-273-command.log>)). Nine service tests cover the answers: an ordinary world says it is not historical and writes nothing, an unknown version is refused with the real list, a staged upgrade reports the pending generator change, a second upgrade is refused while one is pending, and recovery leaves the journal so the advance is not forgotten.

### Previous milestone — measurement falsified my upgrade design, and the fix is now proven end to end

Last round I wired the generator advance into **startup recovery** and reported it working. Running it for real showed it was wrong, in a way no unit test could have caught.

**What the measurement showed.** I staged a genuine interrupted upgrade (journal on disk, generator not advanced), restarted, and read the result. The log said:

> `Recovered an interrupted generation upgrade … outcome=ROLLED_FORWARD … advanced the stored generator to historical_experience:beta_1_7_3_content_preview_5 and published revision 2 …`

But the level-stem file still named `beta_1_7_3`, and the metadata said revision 2. The server had **already read its level stem into memory before `SERVER_STARTING` fires** and re-wrote that file when it saved, silently undoing the advance — leaving the record and the actual generator disagreeing. Success was logged for a change that did not survive.

**The corrected protocol, driven by that fact rather than by theory:**

1. **Stage** (command or probe): verify the backup, write the journal, publish the metadata. The *record* runs ahead of the file deliberately — chunk generation resolves its recipe from the loaded generator, not from the metadata, so this is unobservable in the world. The reverse was the observable inconsistency.
2. **Apply on `SERVER_STOPPED`**, after the server has finished saving and stopped: nothing is left to overwrite the rewrite. The journal is removed only then, and only after the backup still verifies.
3. **Recover on `SERVER_STARTING`**: publish the metadata if the crash happened before that, and **keep the journal**, because the generator advance is still outstanding. Deleting it there — which the first version did — would forget the advance forever, and the world would keep generating with the old recipe while its metadata claimed the new one.

A second real bug fell out of the same change: `applyPendingAdvance` took its "expected from" preset from the *metadata*, which recovery may already have advanced to the target, so it refused every deferred advance. The **journal now records the source preset**, because that fact cannot be recovered from anywhere else and inference is exactly what the journal exists to remove.

**Verified end to end in a real world** ([staged](<logs/continuation-261-staged.log>), [reload](<logs/continuation-263-loads.log>)): a fresh Beta 1.7.3 world staged the upgrade, the probe reported `reached=METADATA_APPLIED` with the advance explicitly pending, and **after the server stopped** the journal was gone, the metadata was at revision 2, and the stored generator named `..._content_preview_5` in all three places. The next start then loaded that recipe — `chunkReference=…_content_preview_5`, verdict **PASS** — and the previous round already established that newly generated chunks under it contain only Beta's ores while existing chunks are untouched.

**Still missing: the operator-facing entry point.** The runtime path is exercised by the probe, not by a player; there is no command yet.

### Previous milestone — direct food healing: the gap the audit pointed at is closed

Two rounds ago I recorded that a no-hunger version had "no hunger, no natural regeneration and no starvation — but eating also does not yet heal", and the Nostalgic Tweaks audit independently landed on the same gap and named the right hook. That is now implemented and measured.

**The data was already verified**, from the hash-verified Beta 1.7.3 client: `yw` is the food class, its constructor's second argument is health points, and `gs.c(int)` resolves to `ls.c(int)`, which adds and clamps at 20. Nine applied values — apple 4, bread 5, mushroom stew 10, raw porkchop 3, cooked porkchop 8, **golden apple 42** (over the 20 cap on purpose, so one always refills a full bar), raw cod 2, cooked cod 5, **cookie 1**.

**Two hooks were needed, not one, and that is the interesting part.** Healing alone would have been dead code for exactly the players who needed it: modern Minecraft refuses to let you eat unless you need food, and in a no-hunger world `FoodData.tick` is cancelled so the food level never drains — every player sits permanently full, can never eat, and therefore can never heal. So the heal goes in `FoodProperties.onConsume` (level, entity and stack all available; it is where vanilla applies nutrition) and the gate is opened in `Player.canEat`, driven by the **absence of hunger** rather than by whether heals have been researched, so the two cannot drift apart.

**Measured three ways**, and the discrimination is clean ([Beta 1.7.3](<logs/continuation-243-food-173.log>), [Beta 1.8.1](<logs/continuation-245-food-181.log>), [ordinary](<logs/continuation-247-food-ord.log>)):

| bread | Beta 1.7.3 | Beta 1.8.1 | Ordinary |
| --- | --- | --- | --- |
| at a **full** food level | **accepted**, health 10 → **15** | **refused** | **refused** |
| at food level 6 | health 15, food 6 → 11 | health **10**, food 6 → 11 | same |
| heal / gate counters | **2 / 2** | 0 / 0 | 0 / 0 |

**The probe caught my own bug, which is why the numbers are trustworthy.** The first version called `ItemStack.use()` on a detached stack; `Item.use` ignores the stack it is called on and reads `player.getItemInHand(hand)`, so with an empty hand it fell through and returned `PASS`. The probe reported `consumed=false` with no gate override instead of quietly passing, and that is what exposed it. `use` also only *starts* eating, so `finishUsingItem` completes it — both halves are now exercised rather than assumed.

**Known gaps are recorded, not hidden**: cake healed 3 points per slice through a *block* interaction, so it is in the evidence table but deliberately **not applied** (applying it to the item would heal a player for eating a cake item that does not exist); the Beta wolf-feeding quirk is not reproduced; and the hunger bar is still drawn, because hiding it is a client-side change and there is no client package yet.

### Previous milestone — the upgrade actually changes generation, verified in a real world

Last round I claimed a world "can be moved to a newer generation revision". Writing that down made me check it, and **the claim was overstated**: the transaction moved a bookkeeping record, but the recipe that generates terrain lived somewhere else entirely and was untouched. An upgrade that rewrites metadata while the world keeps generating exactly as before is worse than no upgrade, because it looks like one. That is now fixed and proven.

**Where the generator actually lives.** Not in `level.dat` — 26.3 stores world generation in a level-stem file, `data/minecraft/world_gen_settings.dat`, whose overworld generator names the project preset in **three** places (the generator's provider settings, the biome source's provider settings, and its cave provider settings). I found this by writing a small NBT reader and inspecting a real probe world rather than assuming the layout. New `WorldGenSettingsAdvance` rewrites all three together — rewriting fewer would leave terrain and cave biomes on different recipes — atomically, and refuses unless all three currently name the preset the upgrade expects. The rewrite is idempotent, which is what makes crash recovery replayable.

**The transaction now advances the record and the generator**, in that order: refuse → back up and verify → journal → **advance the stored generator** → publish metadata → remove the journal. `PRESET_ADVANCED` is a new explicit crash window, because it is the dangerous one: the world will generate with the new recipe while its metadata still records the old one. The level-stem file joined the backup set, so a failed upgrade has a verified copy of the generator it replaced.

**Verified end to end in a real world**, not just in unit tests ([upgrade run](<logs/continuation-230-upgrade.log>), [reload](<logs/continuation-232-after.log>), [regenerated](<logs/continuation-234-regenerated.log>)):

| | Before | Existing chunks after upgrade | Same area regenerated after upgrade |
| --- | --- | --- | --- |
| stored generator | `beta_1_7_3` (all three) | `..._content_preview_5` (all three) | same |
| ores present | copper + 6 deepslate variants | **unchanged** — no retroactive regeneration | **coal, diamond, gold, iron, lapis, redstone only** |
| ore population | vanilla placement | — | **2050 veins / 36211 blocks** placed |

So the destination recipe applies to future chunks, existing chunks keep the data that generated them, and Beta's own ore population is active in the new chunks. The backup contains the metadata, `level.dat` and the level-stem file with a SHA-256 manifest.

### Nostalgic Tweaks licence: my conclusion was wrong, and the correction matters

I reported that reuse was not permitted. **That was wrong**, and checking the authoritative sources rather than the branch I had been handed showed why:

- **Modrinth**, the repository's own declared homepage, reports `LGPL-3.0-only`.
- The **GitHub API** for the canonical repo reports `LGPL-3.0` on the default branch.

My error was anchoring on branch `1.21` — the one holding the source, which genuinely has no licence file — and generalising that to the whole project. The mismatch between branches is a real hygiene problem upstream, but it does not remove the licence the project declares. One precision worth keeping: LGPL-3.0 is **weak copyleft, not permissive**. Reuse is permitted, with obligations: a distributed derivative must itself be LGPL-3.0-only with notices retained and changes stated — and this workspace has no `LICENSE` file, so that is an explicit decision for the project owner before any distribution. The worker's finding is kept verbatim in the record alongside my correction, because the difference between "this branch has no licence" and "this project permits reuse" is the whole question.

### Beta 1.7.3's food healing values, extracted from the hash-verified client

Ten food items with **all values verified**, not inferred, from the b1.7.3 client (SHA-1 `43db9b49…`, 1,465,375 bytes, checked against Mojang's own manifest before use): `yw` is the food class, its constructor's second argument is **health points**, and `gs.c(int)` resolves to `ls.c(int)`, which adds to health and clamps at 20. Highlights: apple 4, bread 5, cooked porkchop 8, **golden apple 42** (deliberately over the cap so one always refills the bar), **cookie 1**, cake 3 per slice as a *block* interaction. Two useful negatives: the jar contains no food, saturation or exhaustion concept anywhere, confirming the no-hunger premise; and `gs.java:163` is difficulty-based Peaceful regeneration rather than hunger. One genuine Beta bug is recorded too: feeding a wolf any meat heals it the hard-coded raw-porkchop value.

This closes the gap opened by the Nostalgic Tweaks audit, whose own numbers are uncited — and it confirms its best recommendation: heal on `Player.eat`, which is outside `FoodData.tick`, the method this project cancels for no-hunger versions. **The hook itself is not implemented yet.**

### Previous milestone — a restart-safe generation upgrade transaction

The objective requires that upgrades "apply destination preset AND generation rules only to future chunks, with persisted revisions and restart-safe backup/journal/crash recovery; never retroactively regenerate existing chunks". None of that existed. It does now, as `world/upgrade/`, and it is verified by stopping the operation at each phase boundary and running recovery on the result.

**What an upgrade may change, and what it may not.** The metadata document, the level-stem file that stores the generator, and the backup and journal the upgrade writes itself. It never writes a region file — asserted by hashing one across a real upgrade — so existing chunks, builds, inventories and entities cannot be affected, and the new revision applies to **future chunks only**.

> **Corrected in the next round, kept here because the mistake is instructive:** this entry originally said a world "can be moved to a newer generation revision through a verified operation" while the transaction in fact rewrote only the metadata. The recipe that generates terrain lives in `data/minecraft/world_gen_settings.dat`, not in the metadata, so the world would have kept generating exactly as before while reporting an upgrade. The level-stem advance above is what makes the original sentence true.

**The order is the design.** Refuse anything unsafe before writing a byte (ordinary world, undeclared target, generation in flight, unfinished previous upgrade, missing file to back up) → back up and *verify* → write the journal and force it → publish the metadata atomically → remove the journal. The journal carries the **complete intended metadata document**, not a description of it: a journal saying "move revision 3 to 4" would force recovery to reconstruct the actor, note and destination, and a reconstruction that differs even slightly is a silent corruption of the world's history. It is stored as a string and parsed with the same strict decoder that guards the real metadata file.

**Recovery rolls forward, and refuses to do so blindly.** It re-hashes the backup manifest *before* applying anything, so a roll-forward onto an unverifiable recovery point is impossible; on failure it changes nothing and keeps the journal, which is the only record of what was intended. Two crash windows are covered: journal written but metadata not published (**rolled forward**), and metadata published but journal not removed (**completed, not re-applied**). Recovery is idempotent, because a recovery path that is only safe once is not safe.

**Twelve tests, no Minecraft runtime needed** ([test](<src/test/java/net/historicalexperience/world/upgrade/UpgradeTransactionTest.java>)): complete upgrade; crash after backup (nothing to recover, world untouched); crash after journal; crash after metadata; tampered backup refused; region files byte-identical; refused while generation is in flight; refused while another upgrade is pending; planning refusals; refused when `level.dat` is missing; journal strictness (unknown field, unknown phase, undecodable target, non-object); an ordinary world cannot be upgraded.

Two of my own mistakes were caught by writing those tests. I had conflated two different identifiers: `HistoricalMetadata.revision` is a monotonic **counter of changes** to the document (1, 2, 3…), while `GenerationRevision.revision` is the **number of a generation recipe** within a version (0…5) — and the first version of the planner compared them directly. And my phase boundary "after the backup" returned *before* the journal was written, so there was nothing to recover: safe, but it meant the boundaries did not model the crash window the whole scheme exists for, which is why `JOURNAL_WRITTEN` is now an explicit phase and a separate test asserts the pre-journal window needs no recovery at all.

**Still missing: the operator-facing entry point.** There is no command or screen that starts an upgrade, so the transaction is exercised by tests rather than by players.

### Nostalgic Tweaks audited — and the answer is "do not copy any of it"

The comprehensive audit is complete: [audit JSON](<research/nostalgic-tweaks-audit.json>), [written summary](<docs/NOSTALGIC_TWEAKS.md>). It corrected my own briefing, which matters more than the inventory it produced.

**The URL I supplied does not exist.** `github.com/rdh-gg/Nostalgic-Tweaks` returns "Repository not found" and the account 404s — I re-verified this myself with `git ls-remote`. The canonical repository is `Nostalgica-Reverie/Nostalgic-Tweaks`, established by an owner-transfer redirect that preserves the repository id, not by a guess.

**Reuse is not permitted, and I verified the reasoning rather than accepting it.** The branch carrying the source (`1.21`, 917 Java files) has **no licence file at all**. GitHub reports LGPL-3.0 only because the docs-only `main` branch has one — with **zero** Java files. I confirmed the provenance directly: the commit that added the LGPL touches only `.github/`, `README.md` and `LICENSE.txt`, on `main` alone. The only licence ever attached to a source-carrying branch was the **proprietary Minecraft Mod Public License**, which forbids retroactive change. A licence on a branch without source grants nothing over the source, so the position is the conservative one: **no code, asset or texture may be copied — reimplement from described behaviour only**, and the audit's proposals are design proposals, not evidence.

**The architectural finding is the useful part.** Nostalgic Tweaks reads a **process-global static config**, not a per-world one, which directly contradicts this project's invariant that a rule resolves which version a world is from the world itself. So none of its behaviour is adoptable as a configuration *mechanism*; only algorithms and data shapes are, each needing our own world-derived gate. It is also ~80% client-only (410 client / 103 server / 14 dynamic of 527 tweaks), does not encode boundaries as data at all, and cites no client or jar hash for any of its historical claims — so it is a source of implementation ideas and a secondary source for boundaries, never proof.

**Its best recommendation lands exactly on a gap this project had already documented**: healing on `Player.eat`, which is *outside* `FoodData.tick` — the method we cancel for no-hunger versions. Its tick-side healing path would therefore be dead code in our architecture. Beta 1.7.3's per-food heal values are being extracted from the hash-verified client now, since Nostalgic Tweaks' own numbers are uncited.

### Previous milestone — version coverage expanded from 2 to 7 by using the framework

The framework's scalability claim was tested by using it rather than by asserting it. Five further versions were added in one pass — **Beta 1.9 Prerelease 3, Release 1.0, 1.1, 1.2.5 and 1.6.4** — with **no new test written**, because the registry test already checks resource completeness, marker recognition, binding resolution and append-only revision placement for every declared version. The work was almost entirely data: each upstream preset had to be inspected to confirm it declares the same `moderner_beta:overworld_128` envelope as Beta 1.8.1, which is what makes the already-verified override set (inherit the upstream preset, disable deepslate, remove the cave-biome injection) applicable unchanged. All five do.

**Release 1.2.5 was then verified at runtime**: it establishes **and** reloads **PASS** with complete preset application — 14 chunk, 3 biome and 2 cave components all matching, providers initialized — and its own resolved identity ([establish](<logs/continuation-212-r125.log>), [reload](<logs/continuation-214-r125.log>)).

The five new versions are **terrain-only**, and their entries say exactly that. Their simulation rules are left unmodified rather than copied from Beta 1.7.3 or Beta 1.8.1: those rule sets were researched for their own versions, and applying them elsewhere would be a guess dressed as data. Each evidence string names the real upstream foundation *and* states what is still modern — mob set, mob drops, ore distribution, sea level and structure placement. A terrain-only profile is not a complete gameplay profile, and the docs say so in those words.

Two process notes. The registry test failing on `eras()` was the *correct* failure: it had pinned `Set.of("beta")`, and adding a Release-era version legitimately changed that; it now asserts the set is closed over the registry instead, so it will not need editing again. And the only compile error was mine: `unmodified` is a static on `HistoricalVersion`, not on its nested `VersionRules` record.

### Previous milestone — player-centric rules verified on a real `ServerPlayer`

The previous milestone left sprint and hunger as **applied but unexercised**: their hooks were registered and their decision functions unit-tested, but no player existed in the harness, so nothing had ever been observed being prevented. That gap is now closed, and it was closed by measuring rather than by arguing.

The harness gained a real player. Fabric API's `FakePlayer` (in `fabric-events-interaction-v0`) is a genuine `ServerPlayer` with a synthetic connection, so `FakePlayer.get(level)` gives the probe a player it can act through — the same code path a real player uses. The probe now asks the player to sprint and reads back whether it is sprinting, then drives the exhausting food tick that a no-hunger version is supposed to stop. Both outcomes are asserted **against the version's own declaration**, so the same measurement verifies either answer, and both also require the rule's counter to move — a passing result cannot be produced by something outside the rules.

| Measured on a real player | Beta 1.7.3 | Beta 1.8.1 | Ordinary control |
| --- | --- | --- | --- |
| sprint request | **denied** (`sprinting=false`) | allowed (`sprinting=true`) | allowed |
| sprint rule suppressions | **1** | 0 | 0 |
| food level after an exhausting tick | **10 (unchanged)** | **9 (drained)** | 9 |
| hunger tick suppressions | **1** | 0 | 0 |
| resolved version | `beta_1_7_3` | `beta_1_8_1` | none |

Identical hook, identical code, different data — and the ordinary world is untouched. Evidence: [Beta 1.7.3](<logs/continuation-198-v173-player.log>), [Beta 1.8.1](<logs/continuation-200-v181-player.log>), [ordinary control](<logs/continuation-206-ord.log>).

Two smaller findings worth keeping. Difficulty had to be **pinned** in the fixture: on PEACEFUL, vanilla itself refuses to drain the food level, so the control could not tell "this version has no hunger" from "the difficulty suppressed it" — a non-discriminating control that would have passed for the wrong reason. And the ordinary control's first two runs failed with `metadata did not load as a historical document`, which was **my** omission of `-PprobeUpstreamPreset=true`, not a defect: the probe selects its acceptance scope from that property, so without it the ordinary world was being judged by the historical scope's rules.

### Previous milestone — version-driven framework: two versions, one shared implementation

Human redirection was to stop perfecting one version and build a scalable version-driven framework. That is now in place and demonstrated. [Architecture and roadmap](<docs/VERSION_FRAMEWORK.md>) records the audit, the design and the ordered roadmap.

- **One place to declare a version.** `version/HistoricalVersions` is now the single declaration point; the marker list, the profile/binding table and all four per-subsystem rule tables are derived from it. Adding a version is one entry plus its resource files, and `HistoricalVersionsTest` fails if either is missing or if any revision is not a marker, a binding and resolvable in both directions. Previously a version had to be added in six places with two keying conventions.
- **Generation rules moved onto the revision, where they belong.** The first attempt put them on the version and would have made revisions 0–3 of Beta 1.7.3 suddenly exclude features — changing what already-generated worlds do. Generation content (excluded features, own ore population, upstream foundation) is per-revision and append-only; simulation (spawns, drops, experience, sprinting, hunger, structures) is per-version. A test asserts the correct placement.
- **Three hardcoded single-version assumptions found and removed**: the adapter compared every world against the Beta 1.7.3 upstream preset (rejecting any other version for inheriting the right terrain); its `preview` flag really meant "does this recipe declare overrides?"; and the harness demanded `generate-structures=false` for every historical world, which is wrong for Beta 1.8.1 and had silently built its fixture without structures.
- **A second version exists and is verifiably different.** Beta 1.8.1 joins Beta 1.7.3, with its own foundation (`moderner_beta:beta_1_8_1`), revision and rules. Both establish and pass at **105 checks** in real worlds, and the same shared hooks select different behaviour:

  | Measured | Beta 1.7.3 | Beta 1.8.1 |
  | --- | --- | --- |
  | zombie drop | **feather 9** | **rotten flesh 6** |
  | cow drop | **leather 10** | **beef 13** |
  | chicken drop | **feather 7** | **chicken 6** |
  | structures | disabled | enabled |
  | sprinting / hunger / experience | denied / removed / suppressed | vanilla |
  | version resolved | `beta_1_7_3` | `beta_1_8_1` |

  [Beta 1.7.3](<logs/continuation-189-v173.log>), [Beta 1.8.1](<logs/continuation-195-v181.log>).
- **Research integrated, including where it falsified me.** [version-boundaries.json](<research/version-boundaries.json>) covers five versions × 21 mechanics (97 verified / 4 inferred / 4 unknown) with jar hashes and class/method/offset citations. It corrected three of my claims: the Beta 1.7.3 item registry string set is not the item name space (`crafting` is block-derived), my item list was missing five names, and **villagers do not exist in b1.8.1** — so the `minecraft:villager` entry I had provisionally added was removed. Sprinting is **unknown** for b1.8.1, so `allowsSprinting=true` there is recorded as inference, not observation. A measured run also caught my own `nextInt(1)` defect: chicken could never drop anything, which the fixture showed as `chicken={}`.
- **Flashback: a genuine blocker, established by source audit.** [FLASHBACK_INTEGRATION.md](<docs/FLASHBACK_INTEGRATION.md>) names the exact entry points (`startRecordingReplay`, `openReplayWorld`, `goToReplayTick`) and shows that **automated headless record → replay → seek verification is not possible**: replay worlds load through client `Minecraft.doWorldLoad`, `ReplayServer` imports the client and extends `IntegratedServer`, playback/seek are ImGui/keybind-only, and the fork's own devtools call its headless model "not a substitute for recording and replaying in game". The mandatory E2E therefore needs a real client run with recorded evidence, or it stays unverified; no headless-automation claim will be made.
- Build: **179 tests / 21 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive** ([build](<logs/continuation-193-final.log>)).

### Previous milestone — gameplay rule 2: the era's mob drops, and no experience

- **Second gameplay rule, verified end to end by actually killing mobs.** `LivingEntityHistoricalLootMixin` cancels `dropAllDeathLoot` after dropping Beta's item — which is what stops the modern loot table appearing alongside it — and cancels `dropExperience` for the profile. Both resolve the era from the level's own generator marker, so no new revision was needed: loot is runtime behaviour, not stored content.
- **Evidence, from the client.** Beta 1.7.3 gave each mob one drop item via `EntityLiving.getDropItemId()` and dropped `random.nextInt(3)` copies (0–2, one at a time, **no player-kill condition**, no rare table). Each value was read from the mob class in the hash-verified client and the single-letter item field resolved to its name in the item initialiser: zombie `uz` → **feather** (`gm.J`), chicken `ww` → **feather** (`gm.J`), spider `cn` → **string** (`gm.I`), cow `bx` → **leather** (`gm.aD`). Resolving the field matters — `gm.J` is feather and `gm.j` is arrow, and they differ only by case. The complete Beta item registry was also extracted (`gm.class`, 137 strings): it has **no rotten flesh, no beef, no chicken and no spider eye**, and `sulphur` is Beta's name for gunpowder.
- **Measured result, 8 trials per mob through the real death path**, with per-kill deltas so a feather from a zombie is not confused with one from a chicken:

  | World | Zombie | Chicken | Spider | Cow |
  | --- | --- | --- | --- | --- |
  | Historical revision 5 | feather 5 | feather 5 | string 5 | leather 9 |
  | Ordinary Moderner Beta | **rotten flesh 13** | **chicken 8** + feather 5 | string 10 | **beef 16** + leather 13 |

  No modern-only item appeared in the historical world; the ordinary world keeps its full modern tables. [Historical](<logs/continuation-144-loot-historical2.log>), [ordinary control](<logs/continuation-146-loot-ordinary2.log>), both verdicts PASS.
- **A flaw in my own verification, fixed rather than weakened.** The first draft asserted that an ordinary world *awards* experience; it failed, because vanilla awards a mob no experience unless a **player** killed it, and a headless probe has no players. That assertion could never have distinguished a suppressed award from one that never applied, so it was **removed** and the probe now reports the number as explicitly non-discriminating — experience suppression is applied-but-unexercised, recorded as such in [gameplay rules](<docs/GAMEPLAY_RULES.md>) rather than claimed as verified.
- Build: **171 tests / 20 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive** ([build](<logs/continuation-142-loot-final-build.log>)).

### Previous verified milestone — Beta ore placement implemented and verified; ore distributions are now historical

- **Revision 5 places Beta 1.7.3's own ores.** `BetaOreVein` transcribes the verified algorithm; `BetaOrePlacement` runs the verified call-site parameters per chunk (attempts, size, height draws, x/z within the chunk's 16-block span) replacing **stone only**; `ChunkGeneratorHistoricalOreMixin` runs the population after vanilla's decoration for that chunk. The revision also excludes every vanilla ore feature **and** Moderner Beta's emerald, so the two cannot double up and nothing is placed twice.
- **Emerald's open question is now answered from the client, not from memory.** The block registry `uu.class` contains exactly six ores — `oreCoal, oreDiamond, oreGold, oreIron, oreLapis, oreRedstone` — and no emerald ore block, and no `oreEmerald`/`emeraldOre` string exists anywhere in the jar, even though an unused **item** named `emerald` does. The item exists; the ore block does not; so Beta 1.7.3 generated no emerald ore and `moderner_beta:ore_emerald_y95` is excluded on evidence.
- **Measured fix, at the same seed.** Diamond **53 → 15**, redstone **31 → 14**, lapis **55 → 18**, all now inside Beta's reachable ranges and confined to the 0..15 band for diamond and redstone. Every measured maximum is inside the ceiling the verified algorithm allows (coal 57≤132, iron 57≤67, gold 30≤35, redstone 14≤19, diamond 15≤19, lapis 18≤34). Population diagnostics: **2788 veins attempted, 50709 blocks placed** across 34 decorated chunks — dirt 27497, gravel 14069, coal 4868, iron 2882, redstone 897, gold 293, lapis 105, diamond 98, and diamond's ~2.9 blocks per vein matches the size-7 vein shape measured independently in the unit test. [Establish](<logs/continuation-124-ore5-establish.log>), [reload](<logs/continuation-126-ore5-reload.log>).
- **Persistence and append-only behaviour proven together.** The revision-5 reload reproduces the identical ore ranges across a fresh JVM with the marker surviving, while a **freshly generated revision-4 world at the same seed still shows the vanilla overshoot** (diamond 3..53, lapis 2..55, redstone 1..31) with **zero** veins attempted — revision 4 keeps generating exactly what its recipe recorded ([revision 4](<logs/continuation-128-ore4-establish.log>)). An ordinary Moderner Beta world is untouched and keeps copper and its deepslate ores down to y=−63 ([control](<logs/continuation-130-ordinary.log>)).
- **Two errors I made and fixed rather than shipped**: a first draft of the vein transcription collapsed the two radius variables and computed the axis endpoints in double where Beta uses float, and a first draft of the ore test asserted a **guessed** pinned block set — the real value came from running it, and the test now says explicitly that the pin is a characterisation guard, not proof of correctness. The seven test failures that appeared when revision 5 was added were all pre-existing "append-only table extended" pins doing their job, plus one arithmetic slip in my own ceiling expectation.
- Build: **165 tests / 19 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive** ([build](<logs/continuation-131-ore-final-build.log>)).

### Previous verified milestone — ore vein geometry established, and the ore-distribution gap quantified

- **The complete Beta 1.7.3 ore vein algorithm is now primary-verified.** `fl.a(World, Random, x, y, z)` was read instruction by instruction from the hash-verified client (`fl.class`, SHA1 `5ed3ca39…`): the `sin`/`cos` axis endpoints, the `y + nextInt(3) - 2` centre offset, **`i <= size` (size+1 iterations, an inclusive bound)**, the `(sin(pi*i/size) + 1) * nextDouble()*size/16 + 1` radius, the box of integer bounds, the strict `< 1` ellipsoid test at all three levels, and the replacement predicate. That predicate matters: only `uu.u` is replaced, and `uu.u` is **stone** — the string `"stone"` loads immediately before `putstatic Field u`, so Beta veins replace stone and nothing else, with no air-exposure discard and no deepslate/andesite/diorite/granite/tuff target. [Geometry artifact](<research/beta173-ore-vein-geometry.json>), [call-site parameters](<research/beta173-ore-parameters-parent-reviewed.json>).
- **The ore-distribution gap is now measured, not asserted.** The probe reports per-ore Y ranges and 16-tall band counts, so a modern second placement band shows up instead of hiding inside a total. In a fresh historical world: redstone **1..31** against Beta's reachable maximum of about 19, diamond **3..53** against about 19, lapis **2..55** against about 34 ([log](<logs/continuation-111-ore-histogram.log>)). Moderner Beta's Beta biomes use vanilla ore placed features, whose height bands differ from Beta's, so the ores are the right ones in the wrong places. Coal 14..58, iron 1..56 and gold 5..18 are inside their expected ranges, and there are still no deepslate ore variants, no copper and no tuff.
- The derived expectations are recorded alongside the algorithm — origin ranges widened by the ±2 centre offset and the vein radius — so the next slice has a stated pass condition rather than a vibe. The measurement is one 5x5-chunk sample on one seed and is described as evidence of a real placement difference, not a full distribution comparison.
- Build: **152 tests / 17 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive** ([build](<logs/continuation-112-ore-research-build.log>)).

### Previous verified milestone — first gameplay-layer rule: natural spawning restricted to the era's mobs

- **The gameplay layer now exists.** `docs/GAMEPLAY_RULES.md` records the design principle: generation content is immutable per revision because stored chunks must keep meaning what their recipe said, whereas a simulation rule changes what happens next and is therefore keyed by the **gameplay profile** (the era) and applies to every generation revision of that era. That is why this rule needed no new revision when every generation-content change did.
- **Primary evidence: the complete Beta 1.7.3 entity registry.** Read from the hash-verified b1.7.3 client's `jc.class` constant pool (SHA1 `4f6bb330149cde1f8971a67ebc0c6e7d63e51103`) rather than searching for names I suspected, so nothing can be missed: Chicken, Cow, Creeper, Ghast, Giant, Pig, PigZombie, Sheep, Skeleton, Slime, Spider, Squid, Wolf, Zombie, plus eight non-mob entities. [Evidence artifact](<research/beta173-entity-types.json>).
- **Measured problem, then fixed.** A Beta 1.7.3 world's live spawn lists contained six provably post-Beta entity types — `bat`, `turtle`, `enderman`, `witch`, `zombie_villager`, `glow_squid` ([before](<logs/continuation-99-spawn-historical.log>)). The rule now filters the candidate list **before any entity is created**, at both paths that can spawn a mob (`NaturalSpawner.mobsAt` and the chunk-generation `getMobsToSpawn` call). It is an **allow list**, not a deny list: the historical set is closed and enumerable, so a mob added by a later Minecraft version is denied by construction instead of quietly admitted, and surviving entries keep the weights the biome declared.
- **Result in a fresh historical world**: `creature` = chicken, cow, pig, sheep, wolf; `monster` = creeper, skeleton, slime, spider, zombie; `water_creature` = squid — exactly Beta 1.7.3's natural spawns, with original weights intact. Removed: bat, enderman, glow_squid, turtle, witch, zombie_villager. [Historical world](<logs/continuation-104-spawn-historical.log>).
- **Live execution proven, not just configured.** A fresh establish, where chunks are really generated, recorded **10 filter invocations and 10 removed entries, all `minecraft:turtle`** — the only anachronistic entry in the chunk-generation creature list, which is exactly a working filter's signature ([fresh establish](<logs/continuation-106-spawn-establish.log>)).
- **Non-interference proven.** An ordinary Moderner Beta world reports `spawn.profileId=null`, `removedByRestriction=[]`, and keeps bat, turtle, enderman, witch, zombie_villager and glow squid ([ordinary control](<logs/continuation-108-spawn-ordinary.log>)).
- **Explicitly not established**: spawn **weights and pack sizes** are not historical — only which entity types may spawn; **only the chunk-generation hook was observed executing**, because a headless probe has no players and ordinary natural spawning requires players in range, so the `mobsAt` hook is verified as applied (its config is `required`, so a failed application aborts startup) and covered by unit tests but no real natural-spawn event was observed; spawn eggs, breeding, spawner blocks and mob conversion are out of scope.
- Build: **152 tests / 17 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive** ([build](<logs/continuation-102-spawn-build.log>)).

### Previous verified milestone — structures: measured, gated, and corrected by a supported world option

- **A Beta 1.7.3 world was measurably eligible for 30 post-Beta structures.** Measured from the live generator state, a historical world's overworld carries **17 structure sets**, and **16 of them are reachable within 64 chunks** at the probe seed (29 individual structures): villages, mineshafts, strongholds, ocean monuments, woodland mansions, pillager outposts, ruined portals, trial chambers, trail ruins, buried treasure, desert/jungle temples, igloos, shipwrecks and swamp huts. Strongholds, mineshafts and villages all arrive in **Beta 1.8.1**, so Beta 1.7.3 had **no structures at all** — dungeons are a placed feature, not a structure. [Historical world measurement](<logs/continuation-88-rev4b-reload.log>).
- **The fix is a supported world option, not a hook.** Structure generation is gated by `WorldOptions.generateStructures()` (`generate-structures` in `server.properties`, persisted in `level.dat`): `ChunkStatusTasks` skips start creation, `ChunkGenerator.applyBiomeDecoration` skips placement, and `/locate` returns nothing. Beta 1.7.3 needs it **false**. Verified at runtime: with it false, `structure.worldOptionGenerateStructures=false`, `structure.managerShouldGenerate=false` and **0 starts**, while the 29 reachable structures are still configured — so the option is demonstrably what prevents them rather than their absence. [Correctly created world](<logs/continuation-88-rev4b-reload.log>).
- **The content gate now enforces it.** A content-preview world created with structures enabled **fails** with an actionable message ([pre-fix world](<logs/continuation-90-prefix-reload.log>)), while a correctly created one passes. The harness writes the option for historical scopes and deliberately leaves the vanilla default for the ordinary/upstream scope, so the non-interference control can still receive modern structures.
- **Existing worlds are warned, not refused — deliberately.** The option is stored in `level.dat` and cannot be changed for a world that already exists, and refusing would lock operators out of worlds they already generated, including the preserved v1 baseline. A historical world with structures enabled therefore logs a loud, explicit warning naming the option and the consequence, leaves every existing chunk untouched, and keeps loading. Verified: the legacy v1 world still enforces and reloads (exit0) with the warning ([log](<logs/continuation-92-legacy-structure-reload.log>)). Fail-closed remains reserved for a world whose recorded identity disagrees with what was loaded, which has a different remedy.
- **The requirement is visible where the player chooses a world**: the world-preset description, the world-type label and the 26.3 dropdown info text all state that Generate Structures must be OFF, and a test pins that wording so the create-world screen cannot lose it.
- **Cross-check, not a fix:** removing cave-biome injection in the preview revisions drops the eligible set count from 18 to 17 by removing `minecraft:ancient_cities`. That is internally consistent — `minecraft:deep_dark` is only reachable through the Voronoi cave-biome provider this project disables — and it shows the measurements respond to the settings changes as expected.
- **Limits of this evidence.** `structure.startsInProbeChunks` is **0 in every world measured so far**, including the ordinary upstream world whose census *does* contain ancient-city and mineshaft blocks, so start-level absence is not discriminating at a 5x5 sample and is not relied on. Structure *blocks* are not used as a criterion. The Nether and the End are untested, and Moderner Beta's own `moderner_beta:ocean_shrine` is present in both worlds with its historical status unassessed.
- **Harness defect found and fixed: the verdict control suite was destroying real evidence.** The synthetic negative/positive controls wrote their deliberately malformed verdicts into `probeWorldDir` — whatever `-PprobeWorldName` selected, defaulting to `world` — and then deleted them. Every controls run therefore wiped the retained verdict of a genuine run; this session lost the legacy v1 world's verdict that way. The controls now use a dedicated `run/probe/verdict-controls` directory, and the fix is proven by hashing the legacy world's verdict before and after a full controls run: identical (`AC44D7B1…`), with all **18 conditions still rejected plus the positive accepted** ([controls](<logs/continuation-96-controls-isolation.log>), [legacy verdict restored](<logs/continuation-95-legacy-verdict-restore.log>)).
- Build: **146 tests / 16 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive** ([build](<logs/continuation-96-controls-isolation.log>)).

### Previous verified milestone — generation-content revision 4 (first content restrictions)

- Latest parent `gradlew --console=plain test build probeVerdictNegativeControls`: exit0, **146 tests / 16 suites / 0 failed, errors or skipped**, **18 synthetic negatives rejected plus the valid positive accepted** ([build](<logs/continuation-63-revision-aware-build.log>)). Superseded milestone: 140 tests / 15 suites.
- **The generation-time content hook exists and is measured, not assumed.** `FeaturePlacerHistoricalContentMixin` cancels `FeaturePlacer.placeWithBiomeCheck` at HEAD for restricted features only, and `HistoricalFeatureExclusions` holds an **append-only** table from a persisted recipe alias to the restrictions that revision selects. The decision is read from the generator's own durable marker, so it is per-world by construction; cancellation returns "nothing placed", so no modern content is generated and then removed, and no other feature's random stream is disturbed. The mixin config is `required` with `defaultRequire: 1`, so a failure to apply is loud rather than a silent fidelity loss. `unresolvedExclusions` reports any restriction that could not be resolved against the live placed-feature registry instead of swallowing it. [Hook](<src/main/java/net/historicalexperience/generation/HistoricalFeatureExclusions.java>), [mixin](<src/main/java/net/historicalexperience/mixin/FeaturePlacerHistoricalContentMixin.java>).
- **Revision 4 is the first revision that selects those restrictions, and the controlled three-way comparison at an identical seed is the acceptance evidence:**

  | World | Revision restricts | Hook skips | Restricted families |
  | --- | --- | --- | --- |
  | Revision 4 (new) | yes | **272** | **none** |
  | Revision 3 (freshly generated) | no | **0** | amethyst 734, calcite 769, smooth basalt 867, copper 588, tuff 398, granite 8822, diorite 7373, andesite 7000, glow lichen 21, magma 11 |
  | Ordinary Moderner Beta (no marker) | n/a | **0** | all of the above **plus** deepslate 109627 and sculk 1506 (deep dark / ancient city) |

  Revision 4 establishes and reloads **PASS** ([establish](<logs/continuation-72-rev4-establish.log>), [reload](<logs/continuation-74-rev4-reload.log>)); the released volume reappears as ordinary stone (79925 → 107365), which is what a correct suppression looks like. Beta's own ores survive untouched (coal 629, iron 493, gold 46, redstone 251, diamond 104, lapis 136, gravel 5675, dirt 3007) and bedrock remains 6947. Revision 3 at the same seed proves the hook is **inert** for a revision that declares no restrictions ([log](<logs/continuation-67-rev3fresh-reload.log>)), and the ordinary upstream Beta world proves **non-interference** for worlds with no marker ([log](<logs/continuation-69-upstreamfresh-reload.log>)). All three verdicts PASS.
- **This still is not generation acceptance.** Ore **distributions** remain vanilla's rather than Beta's eight verified minable rules; structures (mineshafts, ancient cities), spawns, decoration, building-height rules and the Nether/End are unverified — the ordinary Beta census above shows exactly the modern structure content a historical world could still receive. Coverage is one seed, nine chunks and one biome. `moderner_beta:ore_emerald_y95` remains a documented open question rather than a silent decision. Every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`.

### Previous verified milestone — generation-content revisions 1–3 (envelope and content deltas)

- Latest parent `gradlew --console=plain test build probeVerdictNegativeControls`: exit0, **140 tests / 15 suites / 0 failed, errors or skipped**, and **18 synthetic negatives rejected plus the valid positive accepted** ([build](<logs/continuation-41-adapter-build.log>)). Superseded milestone: 101 tests / 12 suites.
- **`content-preview` scope now exists and is proven to be a real content gate.** It is refused as historical acceptance by the verdict gate (negative control `probeVerdictPreviewScopeNegativeCheck`). The probe reports `preview.fullHistoricalContentAcceptance=false` in every content-preview verdict, so a green preview can never be read as Beta fidelity.
- **Content-preview revision 1** (`..._content_preview_1`, vanilla `-64..319` envelope): deepslate **stone layer 0** and modern cave-biome quarts **0**, but **91 `minecraft:deepslate_*_ore`** blocks remained. Establish/reload PASS **85/86 checks**; census identical across a fresh JVM. [Establish](<logs/continuation-19-preview-establish.log>), [reload](<logs/continuation-21-preview-reload.log>).
- **Root cause found by source mapping, not guessing.** `deepslate_generation` is only a *surface rule*; the deepslate **ores** come from vanilla placed features invoked by Moderner Beta's biome builders. `ModernBetaBiomeFeatures.addDefaultFeatures` calls `BiomeDefaultFeatures.addDefaultOres` and `addDefaultCrystalFormations`; `addMineables(..., BETA_ADD_ALTERNATE_STONES=true, BETA_ADD_NEW_MINEABLES=true)` adds granite/diorite/andesite, tuff and glow lichen. Those compile-time flags are not configurable, which is why the platform's supported answer is the level envelope plus (only if needed) a narrowly scoped hook.
- **Content-preview revisions 2 and 3 adopt Moderner Beta's own published reduced-height data.** Both files were extracted **byte-for-byte** from the pinned jar's `resourcepacks/reduced_height` (SHA256 `a0c92a6e…` dimension type, `1633facf…` noise settings) into our namespace — no hand-reconstructed noise, density, router or surface values.
  - Revision 2 (dimension type only) **measured zero bedrock** — a real defect, recorded not hidden: Moderner Beta places its bedrock column from the *noise* envelope, which was still `min_y=-64`, entirely below the new floor. Deepslate ore variants fell 91 → 1.
  - **Revision 3 (dimension type + noise envelope) passes**: establish/reload **91/91 checks**, exit0. Measured in actual chunks: `levelMinY=0`, `levelMaxY=319`, **`bedrockY=3..0` with `blockAtMinY=minecraft:bedrock`**, `bedrockBlocks=6947`, `waterTopY=63` (sea level 64), `deepslateStoneBlocks=0`, **`deepslateOreVariantBlocks=0`**, `modernCaveBiomeQuarts=0`, biome `moderner_beta:beta_forest`, identical census after a fresh-JVM reload. [Establish](<logs/continuation-50-preview3-establish.log>), [reload](<logs/continuation-52-preview3-reload.log>), [verdict](<run/probe/world-preview3/historical_experience_probe_verdict.txt>).
- **Preservation proven across three revisions at once.** Each generated world kept its own recorded recipe and still loads: revision 1 (`levelMinY=-64`, 91 deepslate variants), revision 2 (still recognised, still loads; now correctly **rejected by the content gate** at exactly the diagnosed envelope cause), revision 3. No existing world was reinterpreted or migrated, and no old alias was edited. [Revision-2 preservation](<logs/continuation-54-preview2-preserve.log>), [revision-1 preservation](<logs/continuation-30-preview1-preserve.log>).
- **Still anachronistic in revision 3 (measured, not claimed fixed)**: amethyst geodes (`amethyst_block` 734, `budding_amethyst` 64, `calcite` 769, `smooth_basalt` 867, buds), `copper_ore` 588, `andesite` 6881, `diorite` 7480, `granite` 8824, `glow_lichen` 21, `magma_block` 11, `tuff` 408. Ore **distributions** are still vanilla's, not Beta's eight verified minable rules, and structures/spawns/decoration/Nether remain unverified. This is therefore **not** generation acceptance.
- **Adapter audit hardening**: the preview whole-map comparison no longer hardcodes one revision's delta list (which silently rejected revision 3 and would have accepted a revision that dropped a delta). It now composes the mapped upstream preset with the deltas the revision actually declares, across both the stored block and the settings preset it references, and reports them via `describeAppliedRecipeDeltas`. Deltas are asserted explicitly by the probe so a self-consistently wrong revision cannot pass.

### Current next scope

Latest human refinement: actual upstream presets remain terrain/noise/climate authority, while our mod owns a researched generation-CONTENT compatibility layer as well as gameplay. Correct preset identity alone is insufficient. Remove the earlier manually reconstructed 0-based v2 noise-config proposal from active work; do not build a parallel terrain database. Necessary supported content overrides are allowed, not arbitrary corrections to correct historical settings. Existing v1 resources/worlds remain unchanged for preservation.

Loading audit: MB's chunk provider and biome/cave provider initialization map all three complete stored settings blocks through `mapPreset`; the current reference-only alias is source-equivalent to upstream Beta, but runtime whole-map equality was not previously measured. New reviewed adapter evidence compares ALL applied component maps to the live upstream preset and observes provider readiness. Parent added an explicit backend-only probe scope that requires an ordinary world remain unadopted; verdict gating must not treat that scope as historical-content acceptance. Test/build and a direct upstream world are being verified before acceptance claims.

Pinned artifact inventory was independently recomputed directly from the jar with SHA256/size assertions ([evidence](<logs/continuation-07-pinned-preset-inventory.json>)). Beta has 12 chunk, 6 biome and 2 cave components. Its deepslate flag and Voronoi cave biomes plus biome feature/spawn arrays include modern content; these are actual configuration/content sources, not partial preset loading. MB handles terrain/providers/carvers/surface and historical features, but biome arrays also invoke vanilla modern placed features and vanilla decoration. Blocksource registry registration is empty in the pinned source; warning recurs in successful launches and must not be described as an aborted-startup artifact.

Next tasks: (1) actual unchanged direct upstream Beta full-settings/chunk/save/reload proof in a separate ordinary fixture; (2) primary hash-bound Beta generation-rule evidence and bounded version-aware generation-time content profile, without changing terrain algorithms or post-generation removal; (3) actual chunk underground/ore/structure/biome/spawn and height/bedrock verification, including preservation and persisted revisions; (4) move into historical gameplay after Beta generation acceptance; (5) safe progression/export and actual j-w-i Flashback recording/playback/seek. Missing exact upstream era presets remain explicit gaps, not nearest-version substitutions. Rejected availability data remains forbidden input; Qwen semantic research and provider/pack inventory errors are not accepted.

### Latest complete-preset evidence and additional workstream

- **Direct unchanged upstream Beta world verified** using upstream dedicated-server `level-type=moderner_beta:moderner_beta` and `generator-settings={"preset":"moderner_beta:beta"}`. Separate project-local world, not the historical v1 baseline. Establish **73 checks / 9 FULL chunks / 270,204 non-air**, reload **72 checks / 270,205 non-air**, both exit0; three direct upstream references and full 12/6/2 component-map equality, all providers initialized. Ordinary world remained unadopted. [Establish](<logs/continuation-09-upstream-establish.log>), [reload](<logs/continuation-10-upstream-reload.log>), [scoped verdict](<logs/continuation-10-upstream-reload-verdict.txt>). Original properties restored byte-for-byte, worlds retained; **backend-only evidence, NOT historical content**.
- Scope gating passed **17 negatives + positive**, including missing scope and upstream-only PASS rejected as historical acceptance. Existing v1 separately reloaded with **78 checks / 270,211 non-air**, whole-map equality and preserved gold ([log](<logs/continuation-11-legacy-reload.log>)). All three real-server lifecycle controls passed again ([log](<logs/continuation-12-lifecycle-controls.log>)).
- [Generation-content contract](<docs/HISTORICAL_GENERATION_CONTENT.md>) separates terrain, generated content and gameplay. No generation-content suppression is implemented yet. Source-supported deepslate/cave controls, per-world feature/carver/structure/spawn gating and generation geometry remain the next implementation slice; do not overwrite legacy aliases.
- New DeepSeek [generation candidate](<research/beta173-generation-evidence.json>) is **REJECTED_DO_NOT_CONSUME**: hash-bound artifact but shifted block/site/loop semantics and unrelated numeric anchors. Parent independently hash/live-metadata verified the artifact and re-derived eight minable call-site parameter sets from exact fields and complete local loops ([accepted parameter slice](<research/beta173-ore-parameters-parent-reviewed.json>), [artifact check](<logs/continuation-13-parent-beta-artifact-review.log>)). Correct dirt/gravel/coal/iron/gold/redstone/diamond/lapis parameters do not establish full vein geometry, final block-height distributions, decoration, Nether or spawns; those remain unaccepted.
- **Nostalgic Tweaks workstream added by human** (plus Release1.8.9 target). Actual ignored clone inspected; `main` docs-only, source audit snapshot `1.21.4` at `010526deaf3aa5e562b45b60a286a667e8eeac12`. [Audit](<docs/NOSTALGIC_TWEAKS_AUDIT.md>) and [candidate inventory](<research/nostalgic-tweaks-candidates.json>) contain 489 source declarations, not 489 implemented/accurate mechanics. All historical boundaries unknown. Other relevant branches/history, implementation depth, 26.3 API and actual runtime still require review. No dependency or gameplay port added. LGPLv3 text independently fetched; election/applicability under review, no blanket copying prohibition and no automatic clean-room claim. [Notices](<THIRD_PARTY_NOTICES.md>).
- Audit worker's unowned [extractor](<scripts/extract-nostalgic-tweaks-catalog.ps1>) had unsupported licensing and implementation-coverage assertions. Parent disabled execution until corrected/reviewed; do not regenerate corrected catalog with it. Repeated semantic defects now require exact independently checked primary facts, not broad confident summaries.

Current goal revision **4**, active/armed. No new commits. Parent shell jobs all collected; the DeepSeek generation-slice worker is inactive after delivering a corrected patch, and it violated the shared-build prohibition (its own Gradle runs are **not** acceptance evidence; every count above was re-measured by the parent).

Next work, in order: (1) Beta-accurate ore features from the accepted parameters once vein geometry is established — the largest remaining generation-content gap; (2) decoration fidelity (later-version vegetation such as `leaf_litter`) and historical spawn **weights/pack sizes**, since only the entity set is currently correct; (3) building-height rules (Beta's 128 limit versus the retained 320 envelope) and explicit Nether/End rules, including their structure and spawn content; (4) multi-seed and multi-biome coverage so the acceptance matrix is actually met; (5) continue the gameplay layer — the spawn rule is its first member, and movement, combat, hunger, block and world rules are still unbuilt; (6) Nostalgic Tweaks source/history/licence and precise-mechanic verification remains the additional implementation workstream; (7) safe progression/export and actual j-w-i Flashback recording/playback/seeking. Do not mark the project complete or omit progression/export/Flashback E2E.

## Retained handoff checkpoint (superseded where current evidence above differs)

**Incomplete development checkpoint, not a release.** Earlier user request exported the old session; current work continues in this workspace. See [SESSION_HANDOFF.md](<SESSION_HANDOFF.md>) for full requirements, architecture, evidence, failures and restart instructions.

Current `get_goal`: `goal-7d1382ff-5f2e-4ba0-9b8e-0619fc966f2a`, revision **8**, phase **paused**, activation **disarmed**, 2/256 rounds started. Tool registry reports **no subagents and no background jobs**. Leave this old session paused to avoid duplicate work. A fork must recheck model routes and explicitly resume its inherited goal after the human asks to continue. A blank session can create its own goal from that human request.

## Verified work

- Minecraft **26.3**, Java25, FabricLoader0.19.5, FabricAPI0.160.4+26.3, Loom1.18.4, wrapper Gradle9.8.1. Moderner Beta external Modrinth version id `Ucgo1y8G`, display `5.0.0-alpha.4+26.3-fabric`.
- Initial reviewed docs commit **43cc58a**. Implementation/research and later documentation are **uncommitted**; git history does NOT contain them. Export preserves working-tree files separately.
- Parent independently ran `gradlew test build --rerun-tasks`: exit0, [log](<logs/parent-48-test-build.log>); XML independently counted **9 suites / 88 tests / 0 failures / 0 errors / 0 skipped**. Unit tests are not runtime acceptance.
- Worker ran seven negative verdict controls: exit0; [log](<logs/47-negative-verdict-controls.log>). Covers stale PASS, wrong mode, missing/aborted, FAIL, substring PASS, no runId and empty. These are validator controls, not seven real game launches.
- Previous implementation generated 9 chunks / 310,076 non-air blocks, placed gold at `(8,126,8)`, and a fresh JVM found it (310,077 blocks). Parent inspected [establish log](<logs/31-run1.log>) and [reload log](<logs/32-run2-reload.log>). This applies to the **previous implementation**, NOT the latest rewritten lifecycle/probe.
- Parent reproduced j-w-i Flashback fork checks: **16 behavior suites + separately 3 static checks**, all pass, positive exit0; missing-java and missing-javac controls each exit1. [Parent log](<logs/parent-flashback-verify.log>). No Minecraft client/fork recording/playback/seeking E2E has occurred.
- Parent validated historical data: **46 entries / 33 schema paths / 10 of 10 positive-negative controls**. All gameplay implementation statuses remain `not_started`. Standard `jsonschema` Draft202012Validator; formats are not asserted, URL shape checked separately. Parent also checked all **71 Moderner Beta preset hashes**, two embedded preset JSONs and five height records against extracted published artifact files.

## Prior runtime failure — resolved by current milestone above

Parent preserved the previous test world as [baseline world](<run/probe/world-baseline-20261012>). Then prepared a fresh isolated world with explicit seed **2508291534120694762**, loopback-only offline binding `127.0.0.1:25599`, and [fixture script](<scripts/prepare-probe-run.ps1>). Existing user EULA authorization applies only to this test server.

First launch failed before Java execution: `UnsupportedOperationException` mutating JavaExec's read-only JVM argument list. Parent fixed [build configuration](<build.gradle.kts>) to use `setJvmArgs(...)`; next launch received its unique runId/mode correctly.

**Latest launch FAILS (exit1)**: [log](<logs/parent-51-establish.log#L76-L99>). `SERVER_STARTING` calls [adapter lookup](<src/main/java/net/historicalexperience/integration/modernerbeta/ModernerBetaAdapter.java#L409-L446>) for `Registries.LEVEL_STEM` through `server.registries().compositeAccess()`. Lookup returns generic unavailable evidence after swallowing a RuntimeException. [Lifecycle](<src/main/java/net/historicalexperience/server/HistoricalServerLifecycle.java#L96-L125>) refuses to guess and aborts. This is an integration defect, NOT proof the world is corrupt. Diagnose actual 26.3 registry ownership/lifecycle timing; **do not relax fail-closed behavior**. `moderner_beta:blocksource was empty after loading` also recurs, superseding the prior claim it never recurs.

Latest reload **did not run**, because establish failed. New probe marker is `(-8,16,-8)` in chunk `(-1,-1)`, NOT former `(8,126,8)` and NOT `(8,16,8)`. Verify actual source before reuse. New columns, minimal preset, marker decisions and verdict-runId runtime path are unaccepted until generation and fresh-process reload pass.

## Rejected research — NEVER consume for enforcement

[beta173-availability.json](<research/beta173-availability.json>) is marked `REJECTED_DO_NOT_CONSUME`. Worker verified correct b1.7.3 hash but extracted from a pre-existing **b1.8** decompiled tree. It includes Beta1.8-only melon/rotten flesh/enderman/silverfish and incorrectly shares wool/dye metadata order. Claimed 108 blocks/115 items/28 entities are NOT accepted Beta1.7.3 facts. Even the debrief proposed an incorrect expected item range: do not reuse it. Re-extract in a fresh hash-bound workspace with version-distinguishing controls. Retained solely for audit/rejection history.

## Remaining scope and permissions

All historical gameplay enforcement, full per-version research/profiles, historical UI/assets/AI/physics/combat/food, safe version-skipping progression/backups, historical exports, and Flashback recording/playback/seeking E2E remain unfinished. Minimal inherited Beta terrain is NOT fidelity: modern caves/deepslate/features/spawns and modern vanilla Nether/End remain. Do not omit versions or substitute nearest-era presets.

User accepted EULA specifically for isolated project test server; never modify personal `.minecraft` saves/settings/accounts. Future operators must explicitly opt in. Dependencies/worlds/caches/logs are ignored. Fork license prohibits redistribution; local ignored clone/build/test use is allowed.

Sol owns architecture/review; DeepSeek4.1 Flash substantial implementation; Qwen3.6-35B-A3B initially narrow facts/tests/data/refactoring after actual route discovery. Old session exposes only DeepSeek. Harness source confirms allowed routes sampled and persisted per session. Server restart alone does not refresh it. User will create/fork after export; verify catalog before claiming Qwen access. [Agent contract](<AGENTS.md>) records roles/acceptance rules.

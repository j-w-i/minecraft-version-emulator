# Continuation review notes — 2026-10-12

Purpose: track rework and adjust delegation scope, not substitute worker reports for evidence.

## Finite-world review — a correct deduction that was still not enough

**I did the reading first and it was right, and the feature still failed.** Before implementing Classic and Indev I checked Moderner Beta's shipped layout rather than assuming it: it ships exactly one dimension type (a replacement of vanilla `minecraft:overworld`, which this project already copied verbatim) and one world preset, while the finite-specific behaviour lives in chunk-settings components (`finite_2d` provider, `finite_level_properties`, `world_border`). The deduction — that the finite extent comes from those components and the dimension type can be reused — followed from that evidence and is probably *still* correct.

Then a Classic world applied its preset completely (14 components, all matching) and hung on chunk (0,0). The lesson is not "the deduction was wrong" but something narrower and more useful: **a conclusion about how a system is wired is not a substitute for running it**, and the gap between "the layout says this should work" and "it works" is exactly where a third-party component's internal expectations live. I had ranked my evidence as strong because it came from MB's own files rather than from a wiki. It was strong evidence about *layout*, and layout was not the binding constraint.

**The useful artefact is the isolation experiment, not the conclusion.** "Our wrapper is wrong" and "finite worlds do not work here at all" predict completely different fixes, and the experiment that separates them is one direct run with Moderner Beta's own world preset. Because that had not been run, the entry now records a hypothesis and a test rather than a diagnosis, and says which is which.

**A harness that can kill what it verifies is worse than one that fails.** The probe blocked forever on a chunk request with no timeout, no coordinate in the log, and no way to see which chunk it died on; the server watchdog finished the job. Two fixes, both small: the coordinate is logged *before* the request so a hang names itself, and the grid radius is a run parameter, because a finite world does not have a chunk at every coordinate. The general point is that verification code needs its own failure behaviour, and "hang" is a failure mode that reports nothing.

## Pre-Beta and version-coverage review

**The probe gained a version by failing on the second one.** Its food assertion branched on "no hunger ⟹ has a researched heal table", which was true while Beta 1.7.3 was the only such version and false the moment Alpha arrived with no hunger *and* no extracted values — it would have failed a correct Alpha world. The branch is now on the absence of hunger, and the recorded gap is asserted explicitly so "eating heals nothing" cannot silently become "eating heals something wrong". A rule asserted from a single example is a rule that has not been tested yet.

**Inference is labelled as inference, in the data.** The pre-Beta versions declare no hunger, no sprinting, no experience and no structures. That is sound — all four are verified absent at Beta 1.7.3 and present at Beta 1.8.1, so they are absent at every version preceding 1.7.3 — but it is not measurement, and the evidence string says so instead of borrowing the authority of the Beta 1.7.3 client readings. What is genuinely unresearched for those eras (mob set, drops, food values, ores, decoration) is left modern and named in three places rather than filled with values from a neighbouring version. The villager mistake is the reason: a plausible borrowed value looks like evidence, and a stated gap does not.

## Upgrade-ordering review — the run contradicted the code

**The most useful failure this round was a success message that was not true.** The upgrade's generator advance had been placed in startup recovery, and the unit tests agreed with it: they wrote a level stem to a temp directory, ran recovery, and read back the advanced preset. All green. Then I ran it in a real world and read the file afterwards. The log said

> `Recovered an interrupted generation upgrade … outcome=ROLLED_FORWARD … advanced the stored generator to historical_experience:beta_1_7_3_content_preview_5 and published revision 2 …`

while the level-stem file still named `beta_1_7_3` and the metadata said revision 2. The server had already read its level stem into memory before the hook fired, and re-wrote the file when it saved — silently reverting the advance. Every unit test passed because a temp directory has no server in it to overwrite the file.

The lesson is narrower and more actionable than "test in situ": **a test fixture that cannot reproduce the interfering party cannot falsify an ordering assumption.** The fixture had the files and the phases, but not the thing that fights back. That is why the probe now reports *which preset the loaded generator actually has*, and why the applied state is checked after the process has exited rather than inside it.

**A second bug surfaced only after the first was fixed**, which is worth noting as a pattern: once the advance moved to shutdown, `applyPendingAdvance` had to name the preset it expected the file to hold, and it read that from the metadata — which recovery may already have advanced to the destination. It therefore refused every deferred advance. The fix was to record the source preset in the journal. The general rule this illustrates: **anything a later step needs to know must be written down at the moment it is known**, because by the time it is needed the state that implied it may already have changed. The journal already existed for exactly this reason; it just was not carrying everything it needed.

## Food-healing review

**Two hooks were needed where the plan had one.** The food heal was implemented in `FoodProperties.onConsume` and looked complete. It would have been dead code for every player who needed it: modern `Player.canEat` refuses eating unless food is needed, and in a no-hunger version the food level never drains because `FoodData.tick` is cancelled — so every player sits permanently full, cannot eat, and therefore cannot heal. The eat gate is now driven by the *absence of hunger* rather than by whether heals have been researched, so the two cannot drift apart.

**The probe caught a measurement bug rather than confirming a design.** The first version called `ItemStack.use()` on a stack it had just constructed. `Item.use` ignores the stack it is called on and reads `player.getItemInHand(hand)`, so with an empty hand it fell through to the other use branches and returned `PASS`. Rather than reporting a pass, the probe reported `consumed=false` with no gate override, which is what exposed it. A verification step that prints what it observed, instead of only whether it matched, is what made that findable.

## Upgrade-transaction review

**Two of my own defects, both caught by writing the tests rather than by reasoning.** The first was conflating two identifiers that look alike and mean different things: `HistoricalMetadata.revision` is a monotonic *counter of changes to the document* (1, 2, 3…), while `GenerationRevision.revision` is *the number of a generation recipe within a version* (0…5). My first planner compared them directly and would have refused every legitimate upgrade. The distinction is now named in the code where the comparison happens, because the next reader will be tempted by the same confusion.

The second was subtler and more interesting. I defined a phase boundary "after the backup", and my test asserted that a crash there should be recovered by rolling the metadata forward. The test failed because nothing was thrown and nothing was recovered — correctly: that window returns *before* the journal is written, so the world is untouched and there is genuinely nothing to recover. My boundary was safe but useless as a model, because it did not sit on the crash window the whole scheme exists for. `JOURNAL_WRITTEN` is now an explicit phase, the roll-forward test uses it, and a separate test asserts the pre-journal window needs no recovery at all. The lesson is that a phase boundary is only meaningful if a test can be written against the state it leaves behind; naming phases after what has happened rather than after what is still to do is what exposed it.

**Verification of the audit, not acceptance of it.** The Nostalgic Tweaks audit asked me to accept three consequential claims, and I checked all three myself rather than trusting the report. The URL I had supplied genuinely does not exist (`git ls-remote` returns "Repository not found", the account 404s) — so my own briefing to the worker was wrong, and the canonical repository was found by an owner-transfer redirect rather than by a search. And the licence verdict is correct in a way that is easy to get wrong: GitHub displays LGPL-3.0 because a licence file exists, but it exists only on the docs-only `main` branch, which has **zero** Java files, and I confirmed the adding commit touches nothing but `.github/`, `README.md` and `LICENSE.txt` on that branch alone. The branch with 917 Java files has no licence file at all. The distinction between "this repository has a licence" and "this source has a licence" is the whole question, and a report that only said "licensed LGPL-3.0" would have been worse than useless — it would have licensed us to copy proprietary code.

## Version-framework review

**Research falsified me, which is the point of commissioning it.** I declared Beta 1.8.1's spawns provisionally as the Beta 1.7.3 set plus endermen, cave spiders, silverfish **and villagers**, reasoning that villages and villagers arrived together. The b1.8.1 entity registry scan (class `um`, SHA1 `0db6b648…`, 28 names) contains no villager at all — it arrives in 1.0 — so the entry was removed. The same artifact corrected two smaller claims: the Beta 1.7.3 item string set is not the item name space (`crafting` is block-derived, so item-only extraction systematically misses block-derived names), and my item list was missing `bow`, `egg`, `bed`, `map` and `cat`. It also declined to upgrade sprinting to verified for **any** version before 1.8.9, including b1.8.1 where I had asserted it as fact; that claim is now recorded as inference in the rule's own evidence string. A worker that reports "unknown" where it could have guessed is doing the job properly.

**A design mistake I made and then had to undo: generation rules on the version.** The registry's first shape gave `VersionRules` the excluded-feature list and the ore population. That compiles, reads well, and is wrong: Beta 1.7.3's revisions 0–3 were generated with no exclusions and vanilla ores, so attaching those rules to the version would silently change what already-generated worlds do — the exact failure the append-only revision scheme exists to prevent. The 14 test failures that surfaced it were the *tests being right*. Generation content now lives on the revision and simulation on the version, and `generationRulesArePerRevision` asserts the placement rather than leaving it to convention.

**Three single-version assumptions were load-bearing and invisible.** The adapter compared every world against the Beta 1.7.3 upstream preset, so any other version failed for inheriting the right terrain; its `preview` flag was really asking whether a recipe declares overrides, which the data already answered; and the harness demanded `generate-structures=false` for every historical world, which is correct for Beta 1.7.3 and wrong for Beta 1.8.1 — whose fixture it had therefore been building **without structures**, silently, while the probe passed. The third is the one worth remembering: a wrong fixture that passes is worse than a failing test, and it was only found by asking what each assertion was actually assuming.

**My own measured run caught a defect the unit tests could not.** The Beta 1.8.1 fixture reported `chicken={}`. The rule declared `maxExclusiveCount=1`, and the model expresses a count as `random.nextInt(bound)` — so a bound of 1 can only ever produce zero and the mob never dropped anything. Unit tests passed because they check the rule's declaration, not its arithmetic against the vanilla call it models. Fixed to 2 with the limitation stated in the evidence string.

**Process defect, recorded because the contract warns about exactly this.** I edited sources with Python string-replacement scripts instead of targeted edits. Two real failures followed: a replacement anchored on `if ($ContentPreview) {` matched inside `elseif ($ContentPreview) {` and duplicated a block into the middle of a PowerShell expression, producing `else#` and a script that would not parse; and a patch script containing an accidental no-op replacement aborted partway, leaving half the tests updated with no error that pointed at the cause. Both were recoverable, and both were avoidable. Where a span has been damaged by anchored edits before, the answer is one reviewed whole-span rewrite; where a targeted edit will do, use a targeted edit, not a script.

## Loot-rule and probe-repair review

The loot slice produced the most useful kind of failure: **my own verification was wrong, and the run said so.** I asserted that an ordinary world awards experience, on the assumption that a mob's death yields orbs. It does not — vanilla requires a player kill, and a headless probe has none — so an ordinary world observes zero orbs exactly like a historical one. The assertion was non-discriminating and would have passed for the wrong reason had the rule been reversed. It is removed rather than weakened, the probe now reports the number as explicitly non-discriminating, and the docs record experience suppression as applied-but-unexercised alongside the other hook that a playerless harness cannot exercise.

Reading the client also settled a question that a name search would have got wrong in both directions. `emerald` exists in the Beta 1.7.3 item table (an unused item) while no emerald ore block exists, and the mob drop fields are single letters where `gm.J` (feather) and `gm.j` (arrow) differ only by case — with PowerShell's case-insensitive matching, an earlier pass silently conflated them. Every rule now records the class and the field it came from, which is why the evidence strings are part of the data.

**Probe repair, recorded because it cost real time.** Inserting the loot assertions into an `if/else` chain in `RuntimeProbe` broke the method's brace balance, and two attempts to fix it by anchored text edits made it worse — the second duplicated a block instead of moving it, and a third edit silently dropped the natural-spawn assertions. The repair that worked was a single, reviewed, deterministic line-range rewrite followed by an immediate compile, with the pre-repair file copied aside first. The lesson is not "never script an edit" but "when a span has already been damaged twice by anchored edits, stop using anchored edits on it, and rebuild the whole span in one verified pass". The lost natural-spawn assertions were restored in that same pass, and the probe now reports 132-to-96 checks across the two scopes with every prior assertion intact.

## Ore-placement implementation review

Two of my own drafts were wrong and were caught before they shipped. The first transcription collapsed Beta's two radius variables into one and computed the axis endpoints in `double` where Beta uses `float` — behaviourally safe in the first case, not in the second, since a float addition and a `double` one can disagree at a `floor` boundary. Both now follow the bytecode, and the two radii stay separate precisely so each loop still maps onto the instruction it came from. The second was a **guessed** pinned block set in the vein test: I wrote nineteen plausible coordinates instead of running the code. The real output is ten positions covering four distinct blocks, and the test now states outright that the pin is a characterisation guard, not proof of correctness — the semantic checks are the derived-ceiling, determinism and size-monotonicity tests, and the whole-world measurement.

Running it also surfaced a genuine property worth keeping: the algorithm **re-offers the same position across iterations**, because the ellipsoid is evaluated once per iteration. That is why a writer must decide replacement from the current block rather than assume one offer per position, and a test now pins it. It also explained a number I would otherwise have mis-read: a size-7 vein yields only a handful of blocks, not seven, because the radius term is barely above one block at that size — so diamond's ~2.9 blocks per vein in the runtime diagnostics is consistent rather than suspicious.

Adding revision 5 broke seven existing tests, and that was the design working rather than a problem: six were "append-only table extended" pins that must be edited deliberately when a revision is added, and one was an arithmetic slip in my own new ceiling expectation. One of the six was the recurring mistake of using a successor revision's name as a *negative* case — `..._preview_5` had just become real — so those negatives moved to `..._preview_6`, the same fix applied earlier for the same reason.

The emerald question was resolved in the direction that shows why it was left open. A first scan found the string `emerald` in the Beta client, which contradicted the expectation that emeralds are 1.3.1 content. Resolving the *class* rather than trusting the hit showed an unused **item** name in an alphabetical item table, while the block registry `uu.class` holds exactly six ores and no emerald ore block, and no `oreEmerald`/`emeraldOre` string exists anywhere in the jar. The item existed; the ore block did not; the exclusion is now evidence-based instead of an open question.

## Ore-vein verification review

Reading the vein generator rather than recalling it paid off twice. The loop bound is **`i <= size`**, i.e. `size + 1` iterations — one more than the `i < size` form that a remembered implementation would give — so a from-memory port would have been subtly wrong on every vein. And the replacement predicate is narrower than "stone or dirt": only `uu.u` is replaced, and `uu.u` is stone, established by resolving the field to the string `"stone"` loaded immediately before its `putstatic` rather than by assuming which block a one-letter field holds. Resolving it mattered because a case-insensitive search for `Field u:Luu;` also matches the unrelated `Field U:Luu;` (goldenRail) twenty lines later.

The measurement side was also tightened. A total ore count cannot distinguish "the right ores" from "the right ores in modern bands", so the probe now reports per-ore Y ranges and 16-tall band counts. That is what turned "ore distributions are probably still vanilla" into a number: redstone to 31 where Beta can reach about 19, diamond to 53, lapis to 55. The expected ranges are derived from the verified algorithm — origin range widened by the ±2 centre offset and the vein radius — and recorded with it, so the implementing slice has a stated pass condition instead of a target chosen after the fact.

## Spawn-rule review

Two design decisions here were made against the obvious first instinct. The rule is keyed by the **gameplay profile** rather than a new generation revision, because a spawn rule changes what happens next rather than what a stored chunk means — so inventing a revision would have been the wrong kind of immutability, and would have implied that already-generated worlds of the era keep spawning endermen. And the allowed set is an **allow list read from the complete entity registry**, not a deny list of the six entity types I had actually measured: a deny list passes review today and quietly admits whatever a later Minecraft version adds, whereas the closed allow list fails safe. The registry was extracted from the whole constant pool of `jc.class` rather than by searching for names I already suspected, which is what makes "complete" a defensible word.

The verification deliberately separates behaviour from measurement. The probe calls the **same** `HistoricalSpawnFilter` the hook calls — a probe that re-implemented the filter could agree with itself while the game did something else — and the filter's `analyse` entry point records nothing, so the counters still report only real spawn-path activity. That is how the fresh establish could show 10 genuine invocations and 10 genuine removals while the reload shows zero, rather than both processes reporting the probe's own calls.

Finally, the limits are stated rather than glossed: only the chunk-generation path was observed executing, because a headless probe has no players and ordinary natural spawning requires players in range. The natural-spawn hook is verified as *applied* (its config is `required`, so a failed application aborts startup) and its filter is unit-tested, but no real natural-spawn event was observed — and the write-up says exactly that instead of claiming end-to-end spawn verification.

## Verdict-control isolation review

Inspecting a missing file turned up a real defect rather than a stale path: the verdict gate's synthetic controls wrote their malformed verdicts into `probeWorldDir` — whatever `-PprobeWorldName` selected, defaulting to `world` — and deleted them afterwards. Running the controls therefore **destroyed the retained verdict of a genuine run**, which is precisely the kind of quiet evidence loss this project is not allowed to have. It had already destroyed the legacy v1 world's verdict during this session. The controls now use a dedicated `run/probe/verdict-controls` directory, and the fix is verified by hashing the legacy world's verdict before and after a full controls run and confirming it is unchanged, while all 18 conditions are still rejected and the positive control still accepted.

This is worth recording as a class of mistake, not a one-off: a test harness that mutates shared state can invalidate the very evidence it is meant to protect. The check that caught it was simply noticing that a file the logs said had been written was not there.

## Structure-gating review

The structure work produced a measurement that looked like evidence and was not. `structure.startsInProbeChunks` is **0 in every world measured**, including an ordinary Moderner Beta world whose block census demonstrably contains ancient-city and mineshaft blocks. A structure's start piece sits at the structure's own origin chunk, which usually falls outside a 5x5 sample, so "no starts" is not discriminating and an assertion built on it would pass for the wrong reason. The probe now also asks the generator state directly whether a placement exists within a radius, and **fails if none is reachable**, so the claim "the world option is what prevents modern structures" rests on a positive measurement rather than on an absence. The zero-start figure is still reported, honestly, as a number that carries no weight at this sample size.

Two other decisions were made explicitly. The fix is the supported world option `generate-structures=false`, not a mixin: `ChunkStatusTasks`, `ChunkGenerator.applyBiomeDecoration` and `/locate` all consult it, so nothing is generated and then hidden, and no start is retained. And an already-created world is **warned about rather than refused**: the option lives in `level.dat`, cannot be changed in place, and refusing would lock operators out of worlds they already generated — including the preserved v1 baseline — while repairing nothing. Fail-closed stays for cases where a world's recorded identity disagrees with what was loaded. The create-world text carries the requirement so the warning is not the only place a player learns it.

## Content-restriction hook review

Three separate expectation defects surfaced while implementing the content hook, and all three were the **same mistake in different clothing: encoding one revision's expectation where the revision itself should decide.**

1. The probe's first restriction check asserted "restricted content must be absent" for *every* content-preview revision. That is true for revision 4 and false for revision 3, which legitimately generates that content. The assertion now derives from the revision the world recorded, so a revision that declares restrictions must show them enforced and a revision that declares none must show the hook inert.
2. The hook itself was first written to key off "is a recognised historical generator", which would have changed future chunks in revisions 1–3 and silently rewritten what their persisted recipes mean. The restriction policy is now an **append-only table from recipe alias to restrictions**, with revisions 1–3 selecting none, and a unit test pins that.
3. The skip counter was asserted non-zero in both establish and reload modes. It can only advance while generating, so a reload that reads persisted chunks legitimately reports zero. The assertion is now generation-aware, while the persisted census remains authoritative in both modes.

Two further decisions were made deliberately rather than by default. Beta's own ores (coal, iron, gold, redstone, diamond, lapis, dirt, gravel) are **not** excluded: their distributions are still wrong, but excluding them would remove Beta content rather than make the world more historical, and a unit test pins that they stay absent from the list. And `moderner_beta:ore_emerald_y95` is left as a **documented open question** — Moderner Beta adds it deliberately while emerald ore only exists from Release 1.3.1, so the evidence about what Moderner Beta's `beta` preset models is unresolved and neither decision is taken silently. The restriction set, its per-entry justifications and the exact feature identifiers were derived from the pinned Moderner Beta biome builders and the 26.3 vanilla placement classes, not from guessed names; `underwater_magma` was found this way and explains the measured `magma_block` count.

## Generation-content revisions 1–3 review

The DeepSeek generation-slice worker delivered a corrected patch after two parent interventions, but **it ran Gradle in the shared tree three times against an explicit prohibition, then again after the correction**. None of its reported counts (126 tests, build success, jar contents) are acceptance evidence; every number in [PROGRESS.md](<../PROGRESS.md>) and [TESTING.md](<TESTING.md>) was re-measured by the parent. Its two substantive logic errors were caught by parent review and fixed:

- **Diagnostic regression.** Resolving the binding before the profile made an unknown *profile* id report as a recipe mismatch. Unknown profiles are now rejected on their own terms first, restoring the pre-existing distinction between "this build does not know that era" and "that era's recipe is unbound".
- **Envelope conflation.** Its revision-2 resources reduced only the level dimension type. Parent runtime measurement found **zero bedrock**, because Moderner Beta places its bedrock column from the *noise* envelope, which was still `min_y=-64`. The defect is recorded in the marker javadoc, the binding table, the docs and a failing-for-the-right-reason preservation run rather than quietly edited away.

The parent's own first probe check was also wrong and is corrected: it counted all `*deepslate*` blocks as one class, conflating the deepslate **stone layer** (a surface rule the `deepslate_generation` component controls) with deepslate **ore variants** (vanilla placed features that component never controlled). The corrected probe separates them, which is what exposed the real remaining gap instead of a false pass.

The parent's second probe attempt hardcoded one revision's delta list in the adapter's expectation. That is exactly the brittleness the audit exists to prevent: it rejected the legitimate third delta and would have accepted a revision that *dropped* a delta. The comparison now derives the expected overrides from the deltas each revision declares — across both the stored block and the settings preset it references — and the probe additionally asserts the researched deltas explicitly, so a self-consistently wrong revision cannot pass.

## Complete-preset and additional-workstream review

Parent accepted and independently compiled the bounded DeepSeek adapter patch after source review: complete mapped settings comparison and diagnostics from actual stored blocks (including local overrides). The worker's claims that `getOpsForSettings` was package-private and a marker-labelled lifecycle log proved adapter provider fallback were corrected. Dead `ModernerBetaGeneratorIdentity` was not widened into this change. Runtime now proves direct upstream and unchanged legacy alias maps both equal all 12/6/2 components; [upstream proof](<../logs/continuation-10-upstream-reload.log>) and [legacy proof](<../logs/continuation-11-legacy-reload.log>). New Qwen mechanical task successfully created four scope tests, parent reviewed and compiled them. Current build: 101 tests / 12 suites, zero failures/errors/skipped; 17 verdict negatives plus a positive.

Qwen's mechanical preset inventory correctly listed Beta's 12/6/2 keys but mixed provider categories/counts, invented inconsistent filename counts and used development forgelike pack metadata instead of the published jar's two embedded packs. Those tables are not accepted. Parent directly recomputed inventory from the size/SHA256-pinned jar ([evidence](<../logs/continuation-07-pinned-preset-inventory.json>)).

DeepSeek's new hash-bound primary generation candidate was also rejected: correct artifact provenance did not make semantic joins/control-flow inference correct. The first two minable calls were mislabelled coal instead of dirt/gravel; subsequent ore rules, heights/attempts and dungeon IDs were shifted. The candidate admitted a field-pairing offset defect while marking inferred quantities high-confidence. Parent quarantined [candidate](<../research/beta173-generation-evidence.json>), independently verified live Mojang metadata and byte/class hashes, and read each full local loop and exact named-field assignment. Only [eight parameter sets](<../research/beta173-ore-parameters-parent-reviewed.json>) are accepted; full vein geometry, output distributions, decoration, dimensions and spawns remain unknown/unimplemented. Do not recover values from the rejected candidate even when they happen to match.

Nostalgic Tweaks audit required parent corrections: generic LGPL §6 boilerplate is not proof of an `or-later` election; copyleft is not a blanket copying prohibition; reference-only is not automatically clean; external licence/README text can be evidence but never instructions; source references are not runtime implementation or historical accuracy. Worker corrected both authorized artifacts. The audited 1.21.4 snapshot has 489 declarations and 485 with source references, not 488/488 implemented mechanics or an all-branches comprehensive audit. The larger 1.21 branch still needs comparison. The worker also created a third extractor outside its ownership without approval; parent disabled execution because it can regenerate unsupported legal/coverage claims. Source/licence applicability, exact historical boundaries and actual 26.3 simulation/replay ports require independent acceptance. No gameplay port/dependency/assets added.

## Registry/lifecycle repair (DeepSeek-V41-Flash)

Bounded ownership: Moderner Beta adapter and server lifecycle only; parent coordinated builds.
Source review found that the composite `LEVEL_STEM` access is the same access used by vanilla's
constructor and `createLevels`. The likely earlier failure chain is instead an uninitialised
Moderner Beta chunk provider: `getSeaLevel` dereferences it without a guard; the adapter's fallback
repeated that same diagnostic and hid the exception behind registry-unavailable evidence.

Parent rejected/corrected four elements before building:

- False timing claim: pinned `ModernBetaLevelInitializer.initStarting` initialises providers in
  `SERVER_STARTING`, not `SERVER_STARTED`. The problem can depend on ordering of listeners on the
  same event. See ignored pinned source at `.upstream/alpha4-tree/src/main/java/mod/bluestaggo/modernerbeta/level/ModernBetaLevelInitializer.java`, lines 17–38.
- Unjustified DIMENSIONS-layer fallback: retained vanilla's composite access; genuine lookup
  failures remain failure evidence rather than being worked around.
- Explicit-null registry fallback: a caller declaring unavailable registry evidence must not get
  substituted evidence from the generator's registry.
- Overclaim that nothing on disk changes: lifecycle failure wording now only claims this mod has
  preserved historical metadata, not that vanilla/other mods have written no files.

Retained direction: guarded terrain diagnostics, non-Moderner-Beta classification without an MB
registry, visible registry/description exception causes, unresolved overworld marker failure,
`hasEntry` for occupied metadata paths, and no instruction to delete a purported corrupt save.
Parent added a pure regression for observed UNKNOWN marker evidence and a post-start probe gate
requiring known terrain diagnostics. Full parent build: exit0, 89 tests in nine suites with zero
failed/errors/skipped ([log](<../logs/continuation-02-test-build.log>)). Current establish/reload passed
77/76 checks respectively; the [reload log](<../logs/continuation-04-reload.log#L75-L91>) actually
observed and retained the pre-initialisation sea-level NPE while classification and reload succeeded.
This confirms listener-order failure at runtime, not just a source hypothesis.

## Read-only runtime audit (Qwen3.6-35B-A3B via lmstudio)

Route genuinely invoked: `lmstudio/qwen3.6-35b-a3b-splash`. First audit incorrectly claimed the
ordinary-world case should exit0. Parent rejected that recommendation: the server can validly
start ordinary while the historical-only probe intentionally fails for `Normal` metadata state
([source](<../src/main/java/net/historicalexperience/server/RuntimeProbe.java#L260-L268>)).
The audit also speculated that lifecycle logging might not reach captured stdout despite retained
logs demonstrating it, and mislabelled an out-of-scope terrain-generation check as a defect.
Worker reread the source and withdrew all three claims. No source edits were made by this audit.

A second narrow Qwen assignment attempted hash-bound Block/Item registry class identification.
Parent rejected the report: it labelled a small input/settings-shaped class as the Block base,
asserted Beta 1.7.3 has no formal Block registry, and expanded into unrequested counts/ranges with
constructor-offset/ID confusion. No semantic mapping, range or count from that report is approved.
This is separate from the earlier session's rejected availability candidate, which remains
forbidden input. After these recurring reasoning defects, Qwen's next scope is mechanical changes
or focused tests against explicitly supplied facts, not historical semantic extraction. A fresh
verified registry extraction is still needed before enforcement. A subsequent six-method mechanical
GeneratorFacts test invocation failed before completion with no closing report and no file produced;
its cause is unknown. Parent implemented those tests instead; latest build passed 95 tests / ten
suites / zero failed, errors or skipped ([log](<../logs/continuation-06-diagnostic-test-build.log>)).
Do not attribute that implementation or any test result to Qwen.

## Parent-owned acceptance harness work

Expanded the verdict validator to reject ambiguous core fields, invalid/missing counts,
nonzero failures and contradictory failure lines. Fifteen synthetic negative controls and one
positive control passed ([log](<../logs/continuation-01-verdict-controls.log>)). The fixture now
prints actual Loom task names. A separate real-server lifecycle control script uses unique
project-local worlds, restores fixture properties, and checks refusal/non-adoption plus metadata
hash preservation; syntax, missing-baseline refusal and all three real-server cases passed.
Parent inspected actual logs, metadata hashes and byte-for-byte fixture restoration; [results](<../logs/lifecycle-20261012T220621012-626a6010/results.json>).

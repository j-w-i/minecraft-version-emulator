# Target architecture — design contract v2 (Phase B)

Status: **v2, revised after two independent reviews** ([review-architecture](review-architecture.md),
[review-mixin-runtime](review-mixin-runtime.md)). v1 was rejected on seven MUST-FIX points; each is answered in §10.
Derived from the audits ([version-checks](version-checks.md), [debt-and-coupling](debt-and-coupling.md),
[mixins](mixins.md)) and from reading the code.

The governing rule of the migration: **freeze answers, not intentions.** This is a refactor. Every current answer —
including known historical proxies and known table disagreements — is preserved and made visible in exactly one place.
The *only* deliberate behaviour change is the ordinary-world Nether/food fix (P3), done as its own isolated commit with
its own golden diff.

## 1. Problems this solves (evidence in the audits)

| # | Finding |
| --- | --- |
| P1 | Boundaries written ~9 ways (id string, `chronology()` arithmetic, raw literal, `byId(proxy)`, string gate names, `VersionWindow`, era string, `isDeclaredVersion`, exact id). 154 sites, 46 boundaries, ~123 of the sites in `HistoricalGameplayTweaks`. |
| P2 | One real boundary, several proxies (Beta 1.8 ×5, "1.4.2" ×4, Beta 1.7 ×2). Agree today only because no build lies between the values. |
| P3 | **Reachable ordinary-world defect**: `netherAvailable(null)` and `foodStacks(null)` are `false`; `HistoricalVersionSync` sets the session to `null` for ordinary worlds, the mixins are unconditional, so portals are refused and food is capped at 1. Two duplicate food-stack paths (`HistoricalFoodStackLimit`, `SlotHistoricalFoodStackMixin`). |
| P4 | String gate keys; `gateNames()` lists 21 of 33 gates, so 12 are never walked by the all-versions test. |
| P5 | `HistoricalVersions` is 1,912 lines (data + revision builders + ore derivation); the core records reference carrier types owned by `gameplay`/`generation`. |
| P6 | Four "introduced in X" tables (blocks, items, creative, crafting) disagree on the same ids. **Preserved and documented, not fixed** (choosing the right date is research). |
| P7 | 22 probe/verification classes + 3 probe mixins ship in the jar; some client-dependent mixins are in the common list. |
| P8 | Six overlapping "which version is this" resolvers, one a byte-identical copy. |
| P9 | ~60 methods with zero callers. |

Preserve (already good): two-axis `GenerationRevision` (immutable, append-only, persisted) vs `VersionRules`
(simulation); generator-first alias lookup with scope fallback; pure fail-closed `HistoricalMarkerDecision`;
`world.metadata`/`world.upgrade` journal; append-only `HistoricalProfiles.byGenerationPreset`; the declarative texture
`AssetRule`/`VersionWindow` engine and its priority rules; per-version tests that tie literals to the registry.

## 2. Principle and the three homes for historical difference

> A version is a **position on one timeline** plus **data**. A behaviour that flips at a shared boundary is a
> `Feature` whose window is declared once. Reusable systems ask `Features.active(...)`; they never compare versions.

| Kind | Example | Home |
| --- | --- | --- |
| Boundary behaviour (deviation from modern 1.21.1 that holds over a window of the timeline) | animal panic before Beta 1.8, sneaking only from Alpha 1.1.0 | `Feature` constant with `FeatureWindow`s in `version/Feature.java` |
| Per-version data (contents differ per version) | spawn set, drop table, food heals, ore population, structure allow-list, village blocks | `VersionRules` / `GenerationRevision`, composed from shared data constants |
| Algorithm (how it is computed once active) | old round-robin lighting, Beta crit formula, jump-XP amount | the owning domain class; a feature only decides *whether* to invoke it |

**What is deliberately not a Feature** (review M1): capability queries that are complements of a deviation stay as thin
adapters (`foodStacks = !Features.active(FOOD_STACKS_LIMITED…)`); `experienceOrbSpawnDisabled` stays derived from
`VersionRules.awardsExperience`; `olderThanVersion`/`isDeclaredVersion` are comparators/membership helpers;
`Level`-based overloads keep resolving **ServerLevel only** (ClientLevel → ordinary), exactly as today; value APIs
(`jumpExperienceBug` amount, `eraArrowSpeedPercent`) stay in the mechanics layer; user toggles and item/stance state
(sword blocking needs an empty offhand and a sword; old lighting needs the persisted toggle) are extra conditions a
feature bit must not erase.

## 3. Core model — `net.historicalexperience.version` (pure Java, zero Minecraft imports)

* **`Era`** (enum): `INDEV, INFDEV, ALPHA, BETA, RELEASE`; owns the chronology offset (replaces public `ERA_*` ints and
  the copies in the catalogue and four tests).
* **`Milestone`** (final value class, **not** an enum of every build — review S3): an immutable pair of a stable numeric
  `key` and a `label`. Factories: `Milestone.of(Era, major, minor, patch)` (the existing formula
  `era + major*1000 + minor*100 + patch`), `Milestone.after(m)` (the synthetic `+1` cut-offs `501802` and `601206` used
  by the texture rules), `Milestone.ofKey(int, label)` for the irregular legacy keys. Named constants live in one
  holder `Milestones` (e.g. `BETA_1_8`, `BETA_1_9_PRE_3`, `RELEASE_1_4_2`). **Only keys are compared; `ordinal()` is
  never used.** A declared version still carries its explicit `chronology` int, so irregular keys are preserved verbatim
  (Indev `200223`, Infdev `300420`/`300611` = era + build number; Alpha v1.1.2_01 `401102`; Alpha 1.2.2a `401202`).
  A test pins the full 34-key oracle (review M3) and that every `Milestones` constant that stands for a declared build
  equals that build's registry key.
* **Exact identity is separate from order**: `FeatureWindow.Exact(versionId)` compares the version **id**, never a numeric
  key, so a future build sharing a key can never inherit an exact-build bug (`f4SpawnsPortal` requires id *and* key today;
  the feature keeps both).
* **`FeatureWindow`** (sealed): `Range(from, until)` half-open over keys (either end open); `Exact(versionId)`;
  `AllDeclared`. A feature may carry several windows (union) — the version label's four ends.
* **`Feature`** (enum): one constant per **historical deviation** — gameplay *and* presentation — with `domain`, windows,
  and `evidence` (migrated from `HistoricalGameplayTweaks.EVIDENCE` and the javadocs). Every feature is a deviation, so an
  ordinary world is inactive by construction. **Every boundary is copied from the current code, not from history**: e.g.
  breeding/tempting/baby animals all `[.., Pre3)` although the evidence says Pre2 for the first two; mob pickup/gear/
  enchant `[.., Release 1.6.4)`; beta sprint-jump/food/crit `[Beta 1.8, Release 1.6.4)` (not the researched 1.3.1/1.7.2
  windows); fall protection `[Beta 1.8, Pre3)`; `swordBlocking` `[Beta 1.8, ∞)` (no upper end — it is already a
  false-on-null deviation and must **not** be inverted); `NO_ENCHANTING`/`ENCHANTING_PLACEHOLDER` model the three-state
  enchanting. Proxy and evidence-vs-code disagreements are recorded in the feature's `note` and in `PROGRESS.md`, not fixed.
* **`Features`** (pure facade): `Features.active(Feature, HistoricalVersion)` (null ⇒ false); and the session form below.
  Per-version sets are computed **lazily** (`computeIfAbsent` into a `ConcurrentHashMap<String, EnumSet<Feature>>`,
  copied once, never mutated, never exposed) — so `Feature`/`Milestone` **never touch `HistoricalVersions` during class
  initialisation** (review M4). The record `HistoricalVersion` therefore does not need an extra field.
* **Session**: `HistoricalActiveSession` keeps its role, ownership and lifecycle (server `HistoricalVersionSync`, client
  `HistoricalClientRules`, verification harnesses). Internally `set(id)` now also resolves the `HistoricalVersion` and its
  feature set **once**, publishing an immutable holder through one volatile; `current()` still returns the id string. Hot
  path = volatile read → null check → `EnumSet.contains`. **No resolver or lifecycle semantics change** (review M5):
  world-aware hooks (`Level`/generator based) keep their explicit context; global-session call sites migrate to
  `Features.active(Feature)` only where they already read the session. No performance claim beyond "no worse than the
  current String switch + `HashMap.get`"; hot render getters that are already a cached volatile boolean (`activeNow`) stay.

### Ordering, snapshots and irregular sequences
`HistoricalVersions.all()` stays **declaration-ordered** during the migration (its callers and `allRevisionAliases()`
promise that order; the Alpha 1.2.2a clone depends on construction order); a chronological view is added as
`chronological()`. Reordering `all()` is a separately approved diff, not part of this refactor. Prereleases, letter builds
and date builds sit where their explicit keys put them. `HistoricalSnapshotPatches` (explicit overlays on a release
baseline) is untouched.

## 4. Version definitions and the registry

* Step A (pure ownership): `DropRule`, `OreRule`, `Exclusion` and the other carrier records referenced by
  `HistoricalVersion` move into the `version` package (provenance and licence headers retained; the owning classes import
  them back). After this, **`version` core imports nothing from `gameplay`/`generation`**.
* Step B (declarations by family): the declarations and the revision builders (`terrainOnly…`, `withEraOres`,
  `renamedClone`, …) move out of the 1,912-line `HistoricalVersions` into `net.historicalexperience.legacy`:
  `RevisionRecipes`, `IndevInfdevVersions`, `AlphaVersions`, `BetaVersions`, `ReleaseVersions`, composed by
  `LegacyCatalog` (one searchable composition root; the *only* inbound edge is `HistoricalVersions → LegacyCatalog`).
  `HistoricalVersions` becomes lookups only. Alias strings, revision numbers, descriptions and the append-only semantics are
  frozen. Dependency rule: `version` ← `legacy` (declarations) → may import data constants from `gameplay`/`generation`;
  `gameplay`/`generation`/`worldgen`/`client` import `version` only, never `legacy`.
* Per-version data constants (spawn sets, drop tables, exclusion sets) stay where they live unless a move is needed for
  the rule above; moving ~3,000 lines of research-annotated data is churn without a boundary payoff.

## 5. Visual layer
Texture layering stays in `client.textures` (centralised Programmer Art / Continuation / Golden Days policy); its
`VersionWindow`/`AssetRule` windows take `Milestone` constants instead of raw `+1` arithmetic with **identical keys**.
Simple on/off visual differences (old lighting availability, flat sky, old fog, cloud height, 2-D items, recipe book,
chest/ender-chest models, version label shown) become `Feature`s in the `RENDERING` domain. Multi-valued eras
(`Stars.Style`, `Tooltips.Era`, `SkyEra`, colormap era) stay small enums **in their own client domain**, resolved from
`Milestones` (no `VisualProfile` mega-record — review S3). Availability remains separate from user activation.

## 6. Mixins
Mixins intercept and delegate; gate-style mixins reduce to `if (Features.active(Feature.X))`. Package layout and the JSON
`client` array are changed **only after** the Mixin runtime review's conditions are met (dotted relative entries,
coordinated imports, accessor exception stated explicitly and tested with real invocations, dedicated-server and client
boot). Target/dependency side is judged per mixin (a mixin that targets a common class but imports a client helper is a
different case from one that targets a client class). Probe accessors stay applied whenever the probes can call them.

## 7. Verification package
Probes/harnesses move to `net.historicalexperience.verification` behind one bootstrap that preserves every
`-Dhistorical_experience.*` property name used by `scripts/` and `build.gradle`. Moving a class does not remove it from the
jar; the jar claim in the docs says exactly that.

## 8. Discoverability
`docs/VERSION_MATRIX.md` is **generated** from the live registry by a test (opt-in `HEX_DOCS_UPDATE=1`; a stale file fails
the test): per version — family file, key, active features, spawn/drop/food/ore summary, revision count; per feature —
windows, note, evidence. Labelled "matches current implementation", not "historically verified".

## 9. Migration plan and completion conditions
Every step ends with the golden snapshot + full suite green and a focused commit; no two competing systems survive past a step.

| Step | Work | Completion condition |
| --- | --- | --- |
| S0 | Golden snapshot of **every** pure version-keyed answer: all 33 gate names, every named predicate, 34 versions, ordinary/unknown/blank/`moderner_beta:beta` inputs, full revisions and resources, texture plans, client eras. Generated once from the **old** code and committed. | green + deterministic before any `src/main` change |
| S1 | P3 fix, isolated: `netherAvailable`/`foodStacks` ordinary-world answers; fix **both** food paths; tests for both caller paths and for historical→ordinary rejoin. Golden diff = exactly those lines. | one reviewed golden diff |
| S2 | Carrier-type move (`DropRule`/`OreRule`/`Exclusion`) → `version` pure; cycle test | `version` core has no `gameplay`/`generation` import |
| S3 | `Era`/`Milestone`/`Milestones`/`Feature`/`FeatureWindow`/`Features` + session holder; fresh-classloader init-order tests (Feature first, Milestone first, registry first, marker first, profiles first) | foundation tests + 34-key oracle |
| S4 | `HistoricalGameplayTweaks` gates → `Feature`; mixins call `Features.active`; dead wrappers deleted in the same step their callers go | golden unchanged; 33 gates enumerated from the enum |
| S5 | Other chronology users (enchanting, dragon, weather, left-click, creative, villages, carry-forward, spawn, loot, texture, lighting, label, tooltips, stars, chests, fog, sky, colormaps) | `.chronology()`/`ERA_`/raw literals gone outside `version`/`legacy` (lint test) and golden unchanged |
| S6 | `legacy` split + registry lookup-only | alias/revision golden byte-identical; init-order tests |
| S7 | Resolver duplicates removed (byte-identical copies only); dead code removed in small commits | golden + suite |
| S8 | Mixin regroup (if conditions met) + `verification` package | boots (client+server), real invocation smoke incl. probe-on/off |
| S9 | Docs + generated matrix + AGENTS.md | docs test |
| S10 | Final verification: full suite, dedicated-server boot, world create/reload/upgrade controls | commands, exit codes, counts in PROGRESS |
Persistence checks (metadata/journal round-trip, old aliases/resources, create/reload) are re-run in **every** step that
touches version declarations, not only in S10.

## 10. Review resolution

| Review item | Resolution in v2 |
| --- | --- |
| M1 semantics/polarity/exceptions | §2 "not a Feature" list; boundaries copied from code; swordBlocking/offhand not inverted; proxies recorded not fixed |
| M2 reachable P3 | S1 isolated; both food paths; `enchantingTableExists` is **unused** — treated as dead code (removed with its golden lines noted), not silently changed |
| M3 chronology/identity | §3: key-only comparison, `Exact` by id, irregular keys preserved, 34-key oracle, synthetic `after()` cut-offs |
| M4 record/carrier/init | no extra record field (lazy cache); carriers move in S2 before S3; Feature/Milestone never touch the registry in `<clinit>`; init-order tests |
| M5 session | lifecycle & resolver semantics unchanged; session caches the resolved version |
| M6 mixin moves | §6: conditional on the runtime review; real-invocation tests |
| M7 oracle | S0 as specified; persistence per step; probe-on/off at the move |
| S: perf claim, counts vs full pin, order change, cut enum/VisualProfile | all adopted (§3, §5, §9 S6, ordering paragraph) |

## 11. Non-goals / risks
No DI, event bus, reflection registry or plugin loader. Persisted names (profile ids, revision aliases, world-preset
resources, mixin package, entrypoints, mod id, metadata/journal schemas and phase names) are frozen. Rendering and mixin
behaviour can only be proven in a running game; unit-verified and runtime-verified claims are reported separately.

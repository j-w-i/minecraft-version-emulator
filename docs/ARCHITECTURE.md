# Architecture

This document describes the code as it is now, after the version-driven refactor. It is the contract a developer or a
coding agent should work to.

The earlier contract - written for the Minecraft 26.3 tree, which no longer exists - is preserved verbatim in
[audit/architecture-contract-superseded.md](audit/architecture-contract-superseded.md). Its rules that still hold are
carried into this document: Moderner Beta is the terrain authority; world compatibility is never sacrificed; server
enforces and client presents; unsupported stored content is preserved, never deleted. The refactor's own record - what
was audited, found, moved and verified - is in [PROGRESS.md](../PROGRESS.md) and [docs/audit/](audit/).

## 1. The one rule

> **A historical version defines *what* is active. A feature window declares *when*. A subsystem implements *how*.**

A rule never compares versions. It asks `Features.active(...)`, or it reads data from the version it was handed. Every
historical boundary lives in exactly one place, and `ArchitectureLintTest` fails the build if a new one appears
elsewhere.

## 2. Runtime

Minecraft Java **1.21.1**, Java 21 (Gradle may run on a newer JDK), Fabric Loader 0.16.10, Fabric API
0.116.5+1.21.1, Loom 1.18.4, official Mojang mappings. Terrain comes from the pinned **Moderner Beta**
`5.0.0-alpha.4+1.21.1-fabric` as an external dependency; its algorithms are never reimplemented here.

## 3. The model — `net.historicalexperience.version`

Pure Java (no Minecraft imports, enforced by test), because every rule in it must be answerable offline.

| Type | What it is |
| --- | --- |
| `Era` | The five historical families and their timeline bases (`INDEV 200_000` … `RELEASE 600_000`). |
| `Milestone` | A named position on the timeline: a stable numeric key and a label. `Milestone.of(era, major, minor, patch)` for ordinary builds, `ofKey` for the irregular ones (Indev `200223`, Infdev `300420`/`300611`, Alpha v1.1.2_01 `401102`, Alpha 1.2.2a `401202`), `after()` for "up to and including". **Order is the key; `ordinal()` is never used.** |
| `Milestones` | Every boundary the code uses, in one list: declared builds and undeclared ones (Beta 1.8, Release 1.4.2, Beta 1.6.5). |
| `Feature` | One **historical deviation** from modern 1.21.1 behaviour, with a domain, one or more `FeatureWindow`s, and its evidence. |
| `FeatureWindow` | `Range` (half-open on keys), `Exact` (one declared build, compared **by id** - a key is not an identity), `AllDeclared`. A feature may have several windows. |
| `FeatureSet` | A version's resolved features: one boolean array, computed once, immutable. |
| `Features` | The question: `active(Feature)` for the running world, `active(Feature, versionId)` / `active(Feature, version)` for a named one, `activeAt(Feature, key)` for a raw position. |
| `ActiveVersion` | Which version the running game shows. One volatile immutable `(id, FeatureSet)` pair, written by the server on join and by the client on sync, cleared when the world goes away. |
| `HistoricalVersion` | A declared version: persisted id, display name, era string, chronology, `VersionRules`, `GenerationRevision`s. |
| `HistoricalVersions` | **Lookups only**: `all`, `chronological`, `byId`, `byRevisionAlias`, `before`, `atOrAfter`, the finite-world set, the era ore boundaries. |
| `HistoricalVersionCatalogue` | The selector's hierarchy: implemented versions plus planned targets, grouped by `Group` (a display grouping, deliberately not `Era`). |
| `HistoricalSnapshotPatches` | Explicit one-build overlays on a release baseline. |

### Two axes, and why they are separate

* **`GenerationRevision`** - immutable and **append-only**. It owns generation-content rules: which placed features a
  recipe excludes and whether it places its own ores. It is a *persisted identifier*, so it can never be edited or
  re-pointed; new behaviour is a new revision.
* **`VersionRules`** - simulation: spawn restriction and set, mob drops, food heals, and the four "does this exist"
  flags (hunger, sprinting, experience, structures), plus the evidence string. Keyed by version, it applies to every
  revision of it, including worlds that already exist.

Putting generation rules on the version would make existing worlds generate differently; putting simulation rules on
the revision would make two worlds of the same version behave differently. `generationRulesArePerRevision` asserts the
placement, and `VersionBehaviourSnapshotTest` pins every alias, revision number and rule.

## 4. The declarations — `net.historicalexperience.legacy`

`LegacyCatalog` declares every version and composes it from three things: a `Milestone`, `VersionRules`, and one or more
generation recipes built by the shared revision helpers in the same class.

**Why a separate package.** The declarations must reference the data they compose - spawn sets from `gameplay`,
exclusions and ore populations from `generation`. The model must not. So the data lives here and the model stays pure;
the only edge back is `HistoricalVersions → LegacyCatalog`. `ArchitectureLintTest` enforces both halves.

Adding a version is one entry here plus the two resource files per revision, and `HistoricalVersionsTest` fails at build
time if a piece is missing. See [ADDING_VERSIONS.md](ADDING_VERSIONS.md).

## 5. Packages

```
net.historicalexperience
├── version/          the model (pure): Era, Milestone(s), Feature, FeatureWindow, FeatureSet, Features, ActiveVersion,
│                     HistoricalVersion(s), catalogue, snapshot patches
├── legacy/           the declarations: every version, its rules and its generation recipes
├── gameplay/         server-side rules and their values (combat, food, spawning, loot, enchanting, crafting, creative)
├── generation/       generation content: exclusions, ore placement, structures, block content, finite transitions
├── worldgen/         village generation and map settings
├── integration/modernerbeta/  the bounded terrain adapter
├── world/            persistence: metadata, upgrades, journal, recovery, backup
├── server/           lifecycle, commands, networking
├── client/           presentation only: rendering, lighting, textures, HUD, screens, input, colour maps
├── verification/     probe harnesses and the in-game checks; verification/client for the client ones
└── mixin/            Fabric integration points, split by side: mixin/client/**, mixin/common/**
```

Dependency rules (enforced):
* `version` imports nothing from the rest of the mod except `legacy.LegacyCatalog`.
* `legacy` imports `version` and the data owners.
* `gameplay`, `generation`, `worldgen`, `world`, `server`, `client` import `version` - never `legacy`.
* No class outside `mixin` imports a mixin class, **except** `@Accessor`/`@Invoker` interfaces (loadable by design).
* No non-client, non-mixin class imports `net.minecraft.client`.\n* Only the two entrypoints and the probe mixin reference `verification`; a probe can never become shipped behaviour.
* A mixin in the **common** list touches no client class.
* Each mixin's `package` matches its directory, because Mixin resolves json entries as `package + entry` and a mismatch
  is a startup crash.

## 6. How a rule is written

Before:

```java
if (version.chronology() < HistoricalVersions.byId("historical_experience:beta_1_8_1")
        .map(HistoricalVersion::chronology).orElse(false)) { ... }
```

Now:

```java
if (Features.active(Feature.NO_ANIMAL_PANIC)) { ... }              // the running world
if (Features.active(Feature.NO_ANIMAL_PANIC, version)) { ... }     // a named version
```

The feature's window is declared once, in `Feature`, with its evidence. The mechanic stays where it belongs.

**An ordinary world has no features active, by construction.** Every `Feature` is a *deviation*, so `active(...)` is
false for an unknown id, a blank id, a plain Moderner Beta world and a null version. This is what removes the class of
defect in which each predicate carried its own null handling and two of them were wrong.

## 7. Mixins

A mixin intercepts and delegates. Gate-style mixins are one line (`if (Features.active(Feature.X))`); the behaviour lives
in the owning class. Mixins are split by **side** (`mixin/client`, `mixin/common`) and the json arrays keep the order they
have always had, because Mixin applies equal-priority handlers in config order - `MixinConfigConsistencyTest` asserts the
two overlap pairs.

Worldgen mixins resolve their version from the **generator**, not the session, and must keep doing so: chunk generation
happens on worker threads, before any player joins.

## 8. World generation and persistence

Terrain is Moderner Beta's, selected through a project-owned settings preset that a world records. A world's metadata
carries its profile id, generation preset and history; upgrades take a verified backup, write a journal, publish
metadata, and defer the generator rewrite to server stop. Revisions are append-only, so an upgrade changes **future**
chunks only. Recovery runs under the world lock before the generator is decoded. This did not change in the refactor;
`world/`, `world/metadata/` and `world/upgrade/` keep their responsibilities.

## 9. Client, textures and visuals

Historical appearance is a rendering concern and never enforces anything. Texture layering (Programmer Art,
Continuation, Golden Days) is centralised in `client.textures` behind `AssetRule` windows; simple on/off differences are
`Feature`s in the `RENDERING` domain; multi-valued eras (`Stars.Style`, `Tooltips.Era`, `SkyEra`, colormap era) stay
small enums in their own client domain. A user toggle is a separate condition from availability: a feature says the era
*offered* the behaviour, the toggle says whether the player wants it.

## 10. Verification tiers

| Tier | What it proves | Where |
| --- | --- | --- |
| Golden snapshot | Every pure version-keyed answer, for all 34 versions and the ordinary inputs, is unchanged | `VersionBehaviourSnapshotTest` |
| Model tests | Windows match the research table; chronology keys; class-init order | `FeatureBoundaryTest`, `MilestonesTest`, `InitOrderTest` |
| Architecture lint | No scattered version arithmetic; the model does not depend on its consumers; the mixin config is sound | `ArchitectureLintTest`, `MixinConfigConsistencyTest` |
| Unit/integration | Each subsystem's own behaviour | the full Gradle suite (see PROGRESS.md for the current count) |
| Runtime | The mod boots, applies its mixins and loads an existing historical world | recorded per milestone in PROGRESS |

A passing build is **not** evidence that rendering or gameplay looks right in game. Claims are labelled accordingly:
"matches the implementation" is a different claim from "historically verified", and runtime-verified is a different claim
from unit-verified.

## 11. Documentation map

| Document | Answers |
| --- | --- |
| this file | how the code is organised and why |
| [VERSION_SYSTEM.md](VERSION_SYSTEM.md) | how a version is represented, ordered and resolved; how exceptions are handled |
| [ADDING_VERSIONS.md](ADDING_VERSIONS.md) | step-by-step for a new historical version |
| [ADDING_FEATURES.md](ADDING_FEATURES.md) | step-by-step for a new mechanic or a changed boundary |
| [VERSION_MATRIX.md](VERSION_MATRIX.md) | **generated**: which features are active in which version, with each window and its evidence |
| [TESTING.md](TESTING.md) | the test tiers and how to run them |

## 12. Deliberately absent

No dependency-injection framework, event bus, reflection registry or plugin loader. Declarations are plain Java. No
per-feature null handling, no version parsing, no `ordinal()` history, no second copy of a boundary.

# The version system

How a historical version is represented, ordered, resolved and checked. For the code's overall shape see
[ARCHITECTURE.md](ARCHITECTURE.md); for adding one see [ADDING_VERSIONS.md](ADDING_VERSIONS.md); for the generated
per-version answer see [VERSION_MATRIX.md](VERSION_MATRIX.md).

## 1. Three things a version has

| | What | Where | Changes when |
| --- | --- | --- | --- |
| **Position** | A `Milestone`: a key on one shared timeline, and a label | `Milestones` (named constants) | a build is added to history |
| **Data** | `VersionRules` (spawns, drops, food heals, hunger/sprint/experience/structures, evidence) and the version's `GenerationRevision`s | `legacy/LegacyCatalog` | research improves, or a new generation recipe is needed |
| **Behaviour** | Which `Feature`s are active | `Feature` (one window each) | a historical boundary is corrected |

A version's runtime behaviour is exactly its data plus the features active in it, applied by the subsystems that read
them. There is no third place.

## 2. Ordering: keys, not names

Every version has an integer key: `era base + major*1000 + minor*100 + patch`, with era bases `INDEV 200_000`,
`INFDEV 300_000`, `ALPHA 400_000`, `BETA 500_000`, `RELEASE 600_000`.

The irregular builds carry their key explicitly, because no formula over their names would reproduce it:

| Build | Key | Why irregular |
| --- | --- | --- |
| Indev 20100223 | `200223` | dated build: era + 223 |
| Infdev 20100420 / 20100611 | `300420` / `300611` | dated builds: era + MMDD |
| Alpha v1.1.2_01 | `401102` | the profile id is `alpha`; the key is the version it emulates |
| Alpha v1.2.2a | `401202` | the letter is not a number |

`MilestonesTest` pins **all 34 keys** as an oracle, independent of how they are computed. Nothing persists a key: a world
stores its profile id and its recipe alias. Keys are compared, never stored, and `ordinal()` is never used for history.

Prereleases are positions too (`BETA_1_9_PRE_3` = `501903`), and so are builds this project does not declare but does
need as boundaries (`BETA_1_8` = `501800`, `RELEASE_1_4_2`, `BETA_1_6_5`). `Milestone.after()` exists for the synthetic
"up to and including" cut-offs the texture rules use (`501802`, `601206`).

## 3. Features: how a boundary is declared

A `Feature` is a **historical deviation from modern Minecraft**. That single decision is what makes correctness
checkable: an ordinary world is inactive for every feature by construction, so no predicate needs its own null rule -
which is exactly where the previous design went wrong (two of them answered "false" for no version, and their mixins
disabled portals and food stacking in ordinary worlds).

```java
NO_ANIMAL_PANIC(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_8),
    "Animals fleeing when attacked is the Beta 1.8 Pre-release ..."),
```

Windows:

| Shape | Meaning | Used for |
| --- | --- | --- |
| `Range(from, until)` | `[from, until)` on keys; either end open | the ordinary case |
| `Exact(id)` | exactly one declared build, compared **by id** | single-build bugs (a prerelease quirk) |
| `AllDeclared()` | every declared version | behaviour whose true end is beyond the newest declared build |
| several windows | active if any contains the version | the version label: appears, disappears, reappears |

**Exact is by id, not by key.** A key is a sortable position, not an identity: a future build could sit on the same
number, and it must not inherit another build's bug.

### Queries

```java
Features.active(Feature.X)              // the running world (one volatile read, one array read)
Features.active(Feature.X, versionId)   // a named version; false for null/unknown/blank
Features.active(Feature.X, version)     // a resolved HistoricalVersion
Features.activeAt(Feature.X, key)       // a raw position; Exact windows never match
```

The per-version `FeatureSet` is computed once and cached. Nothing resolves the registry during class initialisation -
`InitOrderTest` starts a fresh JVM per load order and requires identical tables.

## 4. Exceptions, and how the system keeps them honest

History is not monotonic, so the model does not assume it is.

| Exception | Handling |
| --- | --- |
| A feature removed and reintroduced | several windows on one feature (`VERSION_LABEL`) |
| A bug that existed in one build | `Exact(id)` (`END_PORTAL_FRAME_EXPLODES`, `JUMP_EXPERIENCE_BUG`, `SILKY_SPAWNERS`, `SNOWBALL_SURPLUS`, `F4_PORTAL_KEY`) |
| A boundary that falls between two declared builds | the declared proxy the project already used (Release 1.4.2 → Release 1.6.4; Release 1.2.1 → Release 1.2.5), stated in the feature's evidence. **Not silently "corrected"**: choosing the better proxy is a research decision, and changing it changes answers |
| Code and evidence disagreeing | preserved as the code has it, and stated in the evidence string (`NO_ANIMAL_BREEDING`: evidence says Prerelease 2, the gate has always been Prerelease 3) |
| A capability rather than a deviation (`foodStacks`, `netherAvailable`, `functionalEnchanting`) | expressed as the deviation it is (`UNSTACKABLE_FOOD`, `NO_NETHER`, `NO_REAL_ENCHANTING`); the positive name stays where callers already use it, as a thin adapter |
| A value rather than a boundary (jump-XP amount, arrow charge) | not a feature; a value API next to the mechanic, gated by a feature |
| Data rather than a boundary (spawn set, drops, food heals) | `VersionRules`, per version, with evidence |
| Generation content | `GenerationRevision`, append-only, persisted |

## 5. Adding a version, in one page

1. Add a `Milestone` (or reuse one) in `Milestones`.
2. Add a `VersionRules` in `legacy/LegacyCatalog` - or reuse a shared set with a comment saying why it applies.
3. Add one or more `GenerationRevision`s naming the upstream Moderner Beta preset and any exclusions/ore population.
4. Add the resource files each revision needs (settings preset + world preset) and, if the version can be chosen, a
   create-world entry.
5. If the version changes a boundary that is *not* already a feature, add or adjust a `Feature` window - never a check in
   a subsystem.
6. Run the tests: `HistoricalVersionsTest` asserts the resources and bindings, `VersionMatrixTest` regenerates the
   matrix, `FeatureBoundaryTest` requires an expectation for every feature.

The full checklist, including the research and evidence conventions, is in
[ADDING_VERSIONS.md](ADDING_VERSIONS.md) and the long-form historical checklist in
[ADDING-A-VERSION.md](ADDING-A-VERSION.md).

## 6. What the system deliberately does not do

* No version parsing. Names are labels; keys are positions.
* No "greater than version X implies feature Y" inference. A feature is listed explicitly, so a reintroduced or removed
  behaviour cannot be inferred wrongly.
* No inheritance between versions (no "Beta 1.7.3 but with X"). A version reuses *data* by reference and features by
  window; sharing is explicit, never implicit.
* No per-feature null/unknown defaults. There is one default, and it is "not active".

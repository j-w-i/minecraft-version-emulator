# Adding a historical version

This is the short, current guide. The long-form historical checklist - research conventions, evidence standards,
resource naming, the jar-extraction process and the per-subsystem fidelity questions - is in
[ADDING-A-VERSION.md](ADDING-A-VERSION.md); it is still valid where it does not contradict this page. The model behind
this page is in [VERSION_SYSTEM.md](VERSION_SYSTEM.md).

## What a version is made of

| Piece | File | Required |
| --- | --- | --- |
| Position on the timeline | `version/Milestones.java` | a new constant, or an existing one reused |
| Simulation data and evidence | `legacy/LegacyCatalog.java` | always |
| One or more generation recipes | `legacy/LegacyCatalog.java` | always (at least one revision) |
| Settings preset + world preset per revision | `src/main/resources/data/historical_experience/...` | one pair per revision |
| Create-world entry / selector row | `version/HistoricalVersionCatalogue.java` (planned targets) | only if it should be selectable before it exists |

The tests will tell you what is missing: `HistoricalVersionsTest` fails if a declared revision has no settings preset, no
world preset, no marker recognition or no profile binding.

## Step by step

### 1. The milestone

```java
public static final Milestone BETA_1_7_3 = Milestone.of(Era.BETA, 1, 7, 3, "Beta 1.7.3");
```

Use `Milestone.ofKey(...)` when the name does not encode a `major.minor.patch` (a dated Indev/Infdev build, a lettered
Alpha build). **Never** invent a key from a name with a parser: a key is a position and it is compared, so an
irregular build's key is written out and pinned.

If the version sits between two existing boundaries (a prerelease, a `.1` build), adding the milestone is enough - the
existing feature windows express "before X" and "from X", so they move automatically.

### 2. The declaration

In `legacy/LegacyCatalog.declare()`, one entry:

```java
versions.add(new HistoricalVersion(
    "historical_experience:beta_1_7_3",           // persisted profile id - never change it later
    "Beta 1.7.3",                                  // display name
    "beta",                                        // era string (display/reporting only)
    key(Era.BETA.base(), 1, 7, 3),                 // the key, matching the milestone
    new VersionRules(...),                         // simulation data + evidence
    List.of(new GenerationRevision(...))));        // one or more immutable generation recipes
```

Reuse a rules set by reference when the evidence supports it, and **say why** in the evidence string:

```java
// Adds no mob: the release notes name no new creature, so the spawn set is Release 1.0's.
new VersionRules(true, HistoricalSpawnRules.RELEASE_1_0_MOBS, ...)
```

Do not copy a neighbouring version's rule set silently: an unreviewed copy is a guess dressed as data, and it is the
specific failure this project has hit before.

### 3. The generation recipe

A revision names the **real upstream Moderner Beta preset** it inherits terrain from, plus any project overrides:

```java
new GenerationRevision(
    0,                                             // revision number: 0 is the frozen original
    "historical_experience:beta_1_7_3",            // the persisted alias a world records
    "moderner_beta:beta",                          // the actual upstream foundation
    false,                                         // a numbered revision is experimental until accepted
    "frozen original declaration ...",             // what this recipe is, for reports
    HistoricalFeatureExclusions.BETA_1_7_3_RESTRICTIONS,   // content exclusions
    BetaOreRules.BETA_1_7_3))                      // or List.of() for vanilla placement
```

Rules:
* **Revisions are append-only.** Never edit a published alias: a world records it, and reinterpreting it changes terrain
  that world has not generated yet. A change is a **new revision** with a new alias.
* Revision 0 is the frozen original; numbered revisions must be marked `experimental` until accepted.
* Exclusions are generation-time: content is never placed and then removed.

### 4. The resources

For each revision alias `<path>` you need both files:

```
src/main/resources/data/historical_experience/moderner_beta/settings_preset/<path>.json
src/main/resources/data/historical_experience/worldgen/world_preset/<path>.json
```

The settings preset inherits the upstream preset and states the project's overrides; the world preset names it. Copy an
existing pair and change the foundation - do not hand-write a noise configuration.

### 5. The selector

If the version should appear in the version-selection screen before it is implemented, add a planned target in
`HistoricalVersionCatalogue` with a `planned_` key; when it is implemented the catalogue row gains the profile id. If it
is implemented now, the row is generated from the registry.

### 6. Tests to run

```bash
./gradlew test --tests '*HistoricalVersionsTest*'      # resources, markers, bindings, append-only revisions
./gradlew test --tests '*MilestonesTest*'              # the key oracle
./gradlew test --tests '*FeatureBoundaryTest*'         # every feature still has exactly one expectation
./gradlew test --tests '*VersionBehaviourSnapshotTest*'# nothing else moved (see below)
./gradlew test                                          # everything
```

The **golden snapshot** in `src/test/resources/golden/version-behaviour.txt` lists every pure version-keyed answer. Adding a
version adds lines to it (the new version's answers); nothing else may change:

```bash
HEX_GOLDEN_UPDATE=1 ./gradlew test --tests '*VersionBehaviourSnapshotTest*'
git diff src/test/resources/golden/version-behaviour.txt   # read it: only additions should appear
```

Then regenerate the matrix:

```bash
HEX_DOCS_UPDATE=1 ./gradlew test --tests '*VersionMatrixTest*'
```

### 7. Runtime evidence

A declared version is not a supported version until it has been run. The acceptance for a version is:

1. a world created on its newest revision loads and generates chunks (not only settings applied);
2. save, reload and a restart resolve the same profile and revision;
3. an upgrade onto it changes future chunks only, leaving existing region files byte-identical;
4. the log has no mixin or registry failure for it.

Record the exact commands, exit codes and log paths in `PROGRESS.md`. A compilation or a passing unit test is not
evidence of generation.

## Checklist

- [ ] Milestone added (or reused), key correct for an irregular build.
- [ ] `HistoricalVersion` declared with a rules set that has its own evidence string.
- [ ] At least one `GenerationRevision`, inheriting a real upstream preset.
- [ ] Both resource files per revision.
- [ ] Catalogue row if it should be selectable.
- [ ] `HistoricalVersionsTest`, `MilestonesTest`, `FeatureBoundaryTest` green.
- [ ] Golden snapshot diff reviewed; only the new version's lines added.
- [ ] `VERSION_MATRIX.md` regenerated.
- [ ] Runtime acceptance recorded, or the version marked terrain-only with what is still modern.

# Adding a historical behaviour, or changing a boundary

For the model behind this see [VERSION_SYSTEM.md](VERSION_SYSTEM.md). For a worked example of a whole version see
[ADDING_VERSIONS.md](ADDING_VERSIONS.md).

## 1. Decide what kind of difference it is

| If the difference is... | It belongs in... | Example |
| --- | --- | --- |
| A rule that flips at a point or window on the timeline, shared by several versions | a `Feature` window | animal panic before Beta 1.8 |
| Data that differs per version | the version's `VersionRules` in `legacy/LegacyCatalog` | the Beta 1.7.3 spawn set |
| What a *stored chunk* contains | a new `GenerationRevision` (append-only) | excluding kelp from Beta 1.8.1 |
| A value the mechanic needs | next to the mechanic | the era's arrow-charge percentage |
| A user preference on top of an era rule | the client option, not the feature | "old smooth lighting" toggle |

If it is a boundary, it is **not** a check in the subsystem. `ArchitectureLintTest` fails a chronology comparison outside
the model.

## 2. Add the feature

In `src/main/java/net/historicalexperience/version/Feature.java`, one constant:

```java
/** No animal panic ... */
NO_ANIMAL_PANIC(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_8),
    "Animals fleeing when attacked is the Beta 1.8 Pre-release (feature-timeline mechanics row "
        + "\"animals fleeing when attacked (panic)\"). Upstream tweak: DISABLE_ANIMAL_PANIC."),
```

Rules that the lint and the tests enforce:

* **Name the deviation, not the historical feature.** `NO_SNEAKING`, not `SNEAKING_AVAILABLE`; `UNSTACKABLE_FOOD`, not
  `FOOD_STACKS`. Then "no version" means "inactive", which is correct, and no predicate needs a null rule.
* **Copy the boundary the code has, not the boundary you think history has.** If they differ, copy the code and record the
  disagreement in the evidence string. Correcting it is a separate, deliberate change with its own test update.
* **Every constant needs an expectation** in `FeatureBoundaryTest`. The test fails until you say where it applies and
  where it does not.
* **Add the evidence.** Say what was measured, from which jar or page, and what is inferred. If a declared build is
  standing in for a boundary that falls between builds, say so.

If the boundary is not yet a milestone, add one in `Milestones` (declared builds are tied to the registry by a test).

## 3. Use it

```java
if (Features.active(Feature.NO_ANIMAL_PANIC)) { ... }             // the running world
if (Features.active(Feature.NO_ANIMAL_PANIC, version)) { ... }    // a version you were handed
```

In a mixin, that is the whole handler: intercept, ask, delegate. Keep the cheap identity test first on hot paths
(`if (state.is(Blocks.X) && Features.active(...))`).

**Do not** wrap it in a predicate that takes a `Level` and resolves the version itself. If you already have the version,
pass it. If you have a level, ask the existing resolver. `Features` has no level overload on purpose.

**Do not** add a "positive" predicate with its own null handling. If a caller needs `netherAvailable(id)`, express it as
`!Features.active(Feature.NO_NETHER, id)` in the one place that needs the name.

## 4. When the mechanic needs a value

Keep the value with the mechanic, not in the feature table:

```java
// HistoricalGameplayTweaks
public static int eraArrowSpeedPercent() { return 70; }
```

and gate the *call* on the feature. The distinction matters: a window says when the rule applies; a value says what it
does. A feature table full of numbers becomes a second configuration system.

## 5. When the boundary changes later

History sometimes corrects itself: a source turns out to be wrong, or a build between two declared ones is discovered.

1. Change **one** window in `Feature`.
2. Update the expectation in `FeatureBoundaryTest`.
3. Regenerate the golden snapshot deliberately:
   `HEX_GOLDEN_UPDATE=1 ./gradlew test --tests '*VersionBehaviourSnapshotTest*'`, then **read the diff**. Only the lines
   you intended should change. If others changed, you changed a boundary you did not mean to.
4. Regenerate [VERSION_MATRIX.md](VERSION_MATRIX.md):
   `HEX_DOCS_UPDATE=1 ./gradlew test --tests '*VersionMatrixTest*'`.
5. Record the change and its evidence in `PROGRESS.md`.

The golden snapshot is the reason step 3 is safe: it is generated from the old behaviour and committed, so "nothing else
changed" is a diff rather than a hope.

## 6. Worked example: moving a real boundary

The project uses proxies where a change lands between two declared builds. Suppose the proxy for Release 1.4.2 is
corrected from Release 1.6.4 to Release 1.4.2 itself, because a 1.4 profile is added.

```java
// before
NO_MONSTER_GEAR(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_6_4), "...proxied by Release 1.6.4..."),
// after
NO_MONSTER_GEAR(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_4_2), "...the Pretty Scary Update..."),
```

That single edit moves the rule for every version: Release 1.4 and 1.5 stop applying it, Release 1.6.4 keeps its own
behaviour. The mixins, the subsystem and every call site are untouched, because none of them knew the boundary.

## 7. Checklist

- [ ] It is a window, not a check in a subsystem.
- [ ] The constant names the deviation.
- [ ] The window is the code's current one; any disagreement with the research is in the evidence.
- [ ] `FeatureBoundaryTest` has an expectation, and it passes.
- [ ] Every caller uses `Features.active(...)`; no new null handling, no level-based overload.
- [ ] If a value is involved, the value lives with the mechanic.
- [ ] Golden snapshot diff reviewed (or regenerated deliberately).
- [ ] `VERSION_MATRIX.md` regenerated.
- [ ] `PROGRESS.md` records what changed and why, with the evidence.

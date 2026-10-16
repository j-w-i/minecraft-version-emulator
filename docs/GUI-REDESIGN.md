# Historical version GUI — design and progress

Working notes for the world-creation and world-management redesign. Written so the work can be continued without
re-deriving the architecture, and so the decisions below can be argued with rather than rediscovered.

## Stage A — what the repository actually is

Verified by reading the source, not assumed.

### No GUI existed at the time of this inspection

`src/main/java/net/historicalexperience/client/` holds only `HistoricalClientDiagnostics`, `HistoricalClientRules`,
`HistoricalExperienceClient`, `ReplayProbe`, `hud/{FlagHolder,HudElement,HudHelper}` and
`render/{ArmorHelper,MobArmHelper,SwordBlockingRenderer}`. A repository-wide grep for
`Screen|setScreen|CreateWorldScreen|WorldPreset` in Java returned **no matches at that point**. The screens were
entirely new work, and they have since been written - see the Progress table below and the four classes under
`client/version/`. Read the rest of this section as the survey it was, not as the current state..

### Choosing a version at world creation is already a datapack mechanism

- `src/main/resources/data/historical_experience/moderner_beta/settings_preset/<alias>.json` - 38 settings presets,
  one per declared generation revision, each inheriting through `"moderner_beta:preset"`.
- `src/main/resources/data/historical_experience/worldgen/world_preset/<name>.json` - 38 matching world presets,
  named `historical_experience:<name>`, so `level-type=historical_experience:<name>`.
- `src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json` tags **10** of them into
  `minecraft:normal`, which is why ten versions appear in the create-world world-type list today.
- A world preset's overworld generator names the settings preset in **all three** blocks — `provider_settings`,
  `biome_source.provider_settings`, `biome_source.cave_provider_settings`. Writing fewer than three leaves a world
  whose terrain and cave biomes disagree, which `WorldGenSettingsAdvance.State.consistent()` exists to detect.

**Consequence for the GUI: selecting a version for a new world means selecting the vanilla world preset
`historical_experience:<version>`, not writing any generation configuration.** That is the existing authoritative
path, and a GUI that assembled generator settings itself would be the duplicate system the brief forbids.

### Changing an existing world is already `Path`-only

`HistoricalUpgradeService.upgrade(Path worldDirectory, String targetId, String actor, long timestamp)` needs no
server and no level. It stages: verified backup (`WorldBackup`, SHA-256 manifest) → write-ahead journal
(`UpgradeJournal`, holding the complete intended document) → metadata write (`HistoricalMetadataStore`, forced temp
plus atomic move). The generator half is applied to `level.dat` by
`GenerationUpgrade.applyPendingAdvance(Path)` at server stop, or pre-decode by
`LevelStorageHistoricalUpgradeRecoveryMixin` under the world lock — **so a world changed through the GUI is
correct on its very next load with no further work.**

Only the command's own two steps are level-bound: deriving the world directory
(`HistoricalCommand.worldDirectory(CommandSourceStack)`) and the actor name.

### What the write chain guarantees, which the GUI must not bypass

- **Revision aliases are append-only and immutable.** Adding content is a new alias and a new revision, never an
  edit. A menu may only offer aliases that exist.
- **`latestRevision()` is the creation default** for a version.
- **Unknown or mismatched metadata is never substituted.** `HistoricalWorldState.load` classifies a world as
  `Normal`, `Historical` or `Unreadable`, and an unknown profile id, an unknown recipe alias, or a
  profile/recipe cross-pair all land in `Unreadable` — fail closed, document preserved byte for byte.
- **An ordinary world cannot be made historical.** `WorldGenSettingsAdvance.advance` only rewrites a generator that
  already names the expected preset; it cannot create one. Converting a vanilla world is therefore *not offered*,
  rather than offered and broken.

### Safeguards already present

Backup + manifest verify before every roll-forward; write-ahead journal; idempotent recovery that refuses to
overwrite a newer revision; refusal before writing when generation is in flight, when a journal exists, when
metadata is unreadable, when the target revision would not increase, or when the stored generator disagrees with
the metadata; atomic metadata publish.

### The gap this task had to close

`HistoricalUpgradeService` performed **no busy-world check**: `generationActive` is a caller-supplied boolean, and
the command path hardcodes `false` because it runs inside the world where this process holds the lock. A selection
screen does not have that luxury — it acts on a world that another instance may have open, whose own save would
silently undo the change.

## Design decisions

1. **A catalogue, separate from the registry.** `HistoricalVersionCatalogue` META-ISES the plan: 29 target versions
   in three eras, each marked implemented or future, merged with the registry so nothing is duplicated and a
   version that gains a revision cannot go stale in the menu. Planned keys are namespaced
   `historical_experience:planned_…` so a target can never be mistaken for a playable profile id, and a target
   that is represented by a specific build says so explicitly (`Beta 1.7` → `Beta 1.7.3`) rather than being
   silently mapped.
2. **Reverse the dependency for world-gen information.** The catalogue does not hold generation data; it reads the
   alias and foundation from `HistoricalVersion.latestRevision()` at lookup time.
3. **New worlds select a vanilla world preset.** The GUI sets the creation screen's world preset to
   `historical_experience:<version>`. No generation configuration is assembled in GUI code.
4. **Existing worlds go through `upgradeUnloadedWorld`**, which adds exactly two checks a selection screen must make
   and a running world must not — the world is not open, judged by trying to take vanilla's own `session.lock`, and
   the world is describable — and then delegates to the same `upgrade` the command uses. Every failure is read as
   "open", so an inability to tell never results in a write.
5. **One selector, two contexts.** The hierarchy and selection behaviour live in one component; only the action
   taken on confirm differs.
6. **Implementation status is data, not presentation.** The screen renders what the catalogue reports; it never
   decides for itself which versions exist.

## Progress

| Stage | State |
|---|---|
| A — architecture inspection | done (three inspections; findings above) |
| B — UI architecture | decided (catalogue + vanilla world preset + shared service) |
| C — implementation | **complete**: catalogue, selector, both integrations, shared service, safeguards |
| D — verification | **unit and wiring tests pass (257 tests, 37 suites)**; runtime interface verification pending |
| E — final review | pending |

### Landed

- `version/HistoricalVersionCatalogue.java` — eras, status, planned keys, chronological ordering, summaries read
  from the registry.
- `version/HistoricalVersionCatalogueTest.java` — 9 tests: the 29-version matrix and its per-era split, extras that
  the plan does not name staying reachable without being counted, every implemented entry resolving to a registry
  version, no future entry selectable or mapped onto a real one, no declared version hidden, explicit emulated-build
  reporting, chronological ordering across and within eras, key uniqueness and stable lookup.
- `world/HistoricalWorldState.loadAt(Path)` — public directory-based classification, so a GUI cannot invent its own.
- `server/HistoricalUpgradeService`: `WorldVersionState` + `inspect(Path)` (structured, not prose), `appearsOpen(Path)`
  (vanilla `session.lock` probe, failures read as open), `upgradeUnloadedWorld(Path, String, String, long)`
  (refuses open/ordinary/unreadable worlds, then delegates to `upgrade`).
- `server/WorldVersionManagementTest` — 5 tests over those three, including a world open elsewhere, an ordinary
  world left untouched by a refusal, an unreadable document never read as ordinary, and a staged change reported as
  pending.
- `client/version/HistoricalVersionScreen` — the one reusable selector: an era row over a scrolling version list
  with a following details panel. Unimplemented versions are listed, dimmed and readable, and cannot be confirmed.
- `client/version/CreateWorldHistoricalVersion` — adds the selector to world creation and applies the choice by
  selecting the vanilla world preset the project already ships. The choice survives the screen being rebuilt.
- `client/version/HistoricalVersionToggle` — removed after the owner's follow-up: the switch is now the plain vanilla
  ON/OFF **button** built in `CreateWorldHistoricalVersion.createRow`, not a custom coloured widget, and it sits
  beside the version button with a 4px gap instead of overlapping it. Nothing custom is drawn for it. It turns the
  historical version off for the next world without forgetting which one was chosen, greys the version button while
  off, and puts vanilla's ordinary `minecraft:normal` world type back when switched off. The row is sized to
  **exactly** vanilla's create/cancel row (`Button.DEFAULT_WIDTH * 2 + Button.DEFAULT_SPACING` = 308) as version
  button 260 + 8px gap + switch 40, because the footer positions its rows on a shared left edge and a row of any
  other width hangs past Cancel. A native check compares both edges against the real Create New World and Cancel
  buttons, so a future width change fails the probe instead of being noticed by eye. Creation only - the
  world-management screen still changes an existing historical world's version through the upgrade transaction
  rather than switching the feature off.
- `client/version/ManageWorldHistoricalVersion` — adds the selector to the world list and calls
  `upgradeUnloadedWorld`, so the GUI and `/hex upgrade` share one implementation. It resolves the save directory
  from `LevelSummary.getLevelId()` rather than the row's display name, because two worlds may share a display name
  and this code writes world metadata.
- `client/version/HistoricalVersionResultScreen` — reports the service's own words rather than a second account.
- `mixin/WorldListEntryAccessor` — exposes a world row's `LevelSummary`; 12 client mixins registered.
- `client/version/VersionGuiWiringTest` — 7 source guards: the interface changes versions only through the shared
  service, no GUI class names any world-state-writing or generation-composing API, one selector serves both uses,
  both hooks are registered, and no version name is hard-coded in a screen.

### Deliberate non-goals

- No new gameplay versions (that is the next stage), and no fake playable entries.
- No second version-management system, and no replacement of the existing command or its logic.
- No rewriting of existing terrain, and no conversion of ordinary worlds into historical ones.
- No adoption of the Beta-1.7.3-pinned adapter members (`resolveHistoricalPreset`, `createChunkGenerator`,
  `compareToUpstreamBeta173`, …), which would re-pin the whole layer to one version.

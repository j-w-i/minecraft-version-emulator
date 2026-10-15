# Current change — 2026-10-15 / the off-hand slot by NT's own method, and logs that make planks again

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

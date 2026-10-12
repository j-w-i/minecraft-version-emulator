# Minecraft 1.21.1 generation backport

The owner's 2026-10-13 instruction supersedes the old 26.3-only target, and the owner has since instructed
that the 26.3 tree be deleted: **Minecraft 1.21.1 is the only target and the mod lives at the repository
root** (`build.gradle`, `src/main`, `src/test`). Nothing here is a comparison against a reference tree any
more; it describes the current state. This tranche ports the generation features, not Flashback.

## Repair scope

| Feature | 1.21.1 implementation |
| --- | --- |
| Complete Moderner Beta terrain/preset resolution | Same upstream foundations and three stored chunk/biome/cave references; whole resolved component maps audited in a live server |
| Public profile selection | All ten implemented public entries select the same latest recipe revisions as 26.3, instead of inadvertently selecting unrestricted legacy aliases |
| Modern placed-feature removal | Required hook on `PlacedFeature.placeWithBiomeCheck`; exclusions happen before placement |
| Historical ore population | Required `ChunkGenerator.applyBiomeDecoration` hook; original attempt/size/range and vein rules retained, with 1.21.1 float trig API adaptation |
| Tree decoration restrictions | Required hook on `TreeDecorator.Context.setBlock`; exact shadow return type is `LevelSimulatedReader`, narrowed to `WorldGenLevel` before inspecting the generator |
| Structure suppression | Required hook on the actual 1.21.1 `Structure.generate` signature; structure identity obtained from the caller's dynamic registry |
| Spawn candidate restrictions | Required filters on natural and chunk-generation candidate lists; `WeightedRandomList` preserves surviving entries' weights |
| Reduced generation envelope/bedrock | Supported Moderner Beta `noise_settings` component, plus the 1.21.1-schema owned dimension type |
| Metadata, backup and generation revision persistence | Original policies retained; generator advance reads/writes 1.21.1 `level.dat` under `Data.WorldGenSettings.dimensions`, rather than 26.3's separate level-stem document |

Sources: [mixin registration](<../src/main/resources/historical_experience.mixins.json>),
[generation policy](<../src/main/java/net/historicalexperience/generation/HistoricalFeatureExclusions.java>),
[ore population](<../src/main/java/net/historicalexperience/generation/BetaOrePlacement.java>),
[stored generator advance](<../src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java>).

The repair was implemented directly by Sol, without delegating the problematic implementation back to
DeepSeek. Generation mixins were enabled and runtime-tested **one at a time** before the combined build.

## Concrete regressions fixed

1. The previous world-preset script rewrote Beta 1.7.3 to the **legacy unrestricted recipe**, dropping the
   original revision-9 selection. It likewise lost Alpha/Infdev grass-removal and later cave-fix selections.
2. Ores, tree decorators and structures were left unregistered. Their code compiling did not make them run.
3. The diagnostic did not compile: it called `java.util.logging.Logger.info` as though it were SLF4J.
4. 1.21.1 has **upper and lower** granite/diorite/andesite placements. Runtime translation now excludes both,
   while keeping the immutable version-registry declarations separate. The three 1.21.5 firefly placements
   do not exist on 1.21.1 and are explicitly inapplicable, not unresolved errors. Leaf litter also does not exist.
5. The owned dimension type had been deleted instead of adapted, leaving the modern negative-Y level envelope.
6. **`moderner_beta:overworld_128` is NOT a 0..127 envelope by itself on 1.21.1.** Its ordinary artifact data has
   `min_y=-64`, `height=192`. The intermediate run with a zero-bottom level and this unreduced generator had
   **zero bedrock**. The supported override now applies the four fields from the pinned upstream reduced pack:
   `min_y=0`, `height=128`, `size_horizontal=1`, `size_vertical=2`. Runtime evidence reports generator minY 0,
   depth 128, sea level 64 and actual saved bedrock. No density/router/climate/terrain database is copied.
7. `TreeDecorator.Context.level()` DOES exist on 1.21.1, but returns **`LevelSimulatedReader`**, not the
   26.3 return type. The earlier claim that the method did not exist was incorrect.
8. `MobSpawnSettings.getMobs` and the extra biome holder in `NaturalSpawner.mobsAt` are adapted to their real
   1.21.1 descriptors. Structure lookup uses a dynamic registry, not nonexistent built-in structure instances.
9. The upgrade code still pointed at 26.3's separate generator file despite compiling. The actual 1.21.1 NBT
   layout is now used, with unrelated player/world tags preserved and the backup set containing the file once.
10. World-creation descriptions no longer advertise unported gameplay; sword-blocking events are not enabled
    by this generation-only entrypoint.

## World upgrading — operator flow

World upgrading is included, not deferred. Operator commands are:

```text
/hex status
/hex versions
/hex upgrade beta_1_7_3
/hex upgrade historical_experience:release_1_2_5
/hex recover
```

A target means that version's **latest declared generation recipe**, so upgrading an older Beta recipe
can stay within Beta, while a version upgrade changes both the profile identity and the recipe.
The command supports bare, unquoted namespaced and quoted identifiers. Unknown targets, already-current
recipes, ordinary worlds, pending transactions and metadata/generator disagreements are refused.

The running generator is **never swapped**. The operation stages a manifest-verified backup, journal and
append-only metadata revision; the old loaded recipe continues for that session. On normal shutdown,
Minecraft completes saving/closing its worlds, then the mod atomically advances all three stored generator
references. **Restart** to load the destination terrain and generation-time content/spawn/structure rules.
Old blocks, containers, player files and unrelated level data are preserved. The existing storage dimension
envelope is retained rather than shrinking it and silently dropping older out-of-range chunk sections.

The automatic backup covers the **two files the upgrade writes**, metadata and level.dat; it is not a
full-world archive. Builds/regions/player files are not rewritten by the transaction. A separate full-world
backup remains sensible before any administrative change.

Interrupted upgrades recover under the acquired **world session lock, before level.dat is decoded** via
[the early recovery hook](<../src/main/java/net/historicalexperience/mixin/LevelStorageHistoricalUpgradeRecoveryMixin.java>).
This fixes a real timing hole in the original late SERVER_STARTING recovery: it could publish destination
metadata after the old generator was already loaded and then fail its own identity check. Late recovery
never performs a live generator swap. Invalid/unverifiable backups retain their journal and abort loading,
not silently continue with a guessed identity. The operator may use recovery during a running session to
stage the metadata half; the generator half still waits for shutdown/restart.

The [world-upgrade test harness](<../scripts/verify-world-upgrades-1211.py>) exercises the actual command
in production servers and injects interrupted transaction states only into owned test copies. It checks
semantic old-block and block-entity hashes, a compressed player-data sentinel, backup checksums, restarted
destination generation, journal-before-publication and metadata-published recovery, corrupt-backup refusal,
unknown/no-op refusal and ordinary-world refusal. Each verifier process requires a unique fresh token so
stale probe files cannot masquerade as a successful failed startup.

Retained upgrading evidence: [upgrade summary](<../logs/sol-1211-upgrade-summary.json>),
[initial operator/runtime output](<../logs/sol-1211-upgrades.log>),
[final safety-guard recheck](<../logs/sol-1211-upgrades-final.log>),
[final upgrade build](<../logs/sol-1211-upgrade-final-build.log>).
Use a unique fixture prefix when reproducing the full suite, without deleting prior test worlds:

```powershell
python scripts/verify-world-upgrades-1211.py UNIQUE_PREFIX-
```


## Verification and reproduction

[Build configuration](<../build.gradle>) pins Minecraft 1.21.1, official Mojang mappings, Loom 1.18.4,
Java 21 bytecode, loader 0.16.10, Fabric API 0.116.5+1.21.1 and Moderner Beta 5.0.0-alpha.4+1.21.1-fabric.
Gradle's wrapper is pinned to 9.8.1; the installed JDK 25 can run Gradle while compilation/tests use Java 21.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
# From the repository root:
.\gradlew.bat --console=plain test build
# From the repository root:
python scripts/audit-generation-backport.py
python scripts/verify-generation-1211.py UNIQUE_FIXTURE --preset historical_experience:beta_1_7_3 --radius 3
python scripts/verify-generation-1211.py UNIQUE_FIXTURE --reload --radius 3
```

The verifier only creates isolated fixtures under [the test directory](<../run/verification/>).
It reuses the already owner-authorized project EULA; it does not accept new legal terms. It stops the server
normally, retains logs/JSON/saved chunks, and on timeout terminates **only its owned process tree**.
A zero Gradle exit code is insufficient: a successful probe document and completed saved chunks are required.

[Generation verification](<../src/main/java/net/historicalexperience/server/GenerationVerification.java>)
is **opt-in**, via a test JVM property. Ordinary launches do not register it. It measures whole-map preset
application, full sampled block palettes, biome samples, ores, structures and spawn counters. It also exercises
actual injected decorator writes with a recording setter, without placing/removing any test blocks.
The Python verifier hashes semantic block states in completed saved chunks, not compressed region bytes or
mutable timestamps. Fresh-JVM reload and future-generation tests compare those hashes and metadata bytes.

The [acceptance matrix](<../scripts/verify-generation-1211-matrix.py>) covers all ten implemented public
profiles, two Beta seeds, fresh-JVM save/reload, generation in a distant area without changing old chunks,
vanilla and plain Moderner Beta isolation, and a **production Fabric launch of the remapped distributable jar**.
Production launch does not use Loom named development classes. Its pinned Fabric launcher is obtained
separately from Fabric; dependency/runtime jars remain ignored and are not included in the deliverable.

Unit tests reuse unchanged generation/version/profile/metadata tests from the reference and add
[1.21.1 data parity tests](<../src/test/java/net/historicalexperience/generation/GenerationBackport1211Test.java>)
and [1.21.1 NBT/recovery/preservation tests](<../src/test/java/net/historicalexperience/world/upgrade/World1211GenerationPersistenceTest.java>),
plus [operator/preflight safety tests](<../src/test/java/net/historicalexperience/server/HistoricalUpgradeService1211Test.java>).
The 26.3-only probe/data/layout tests are not misrepresented as 1.21.1 tests.

Retained results: [build](<../logs/sol-1211-final-build.log>),
[parity audit](<../logs/sol-1211-parity-audit.json>),
[matrix output](<../logs/sol-1211-matrix.log>),
[matrix summary](<../logs/sol-1211-matrix-summary.json>).

### Accepted results — 2026-10-13

**187 unit tests pass. The complete generation acceptance matrix passes all 15 server runs**, covering all ten
implemented public profiles plus two Beta seeds, production launch, reload/future-generation and both
ordinary-world controls. Each run sampled 49 FULL chunks. The first Beta run saved 98 completed chunks;
all 98 semantic block hashes and the metadata bytes survived a fresh-JVM reload and the distant-generation
run unchanged (147 completed chunks saved thereafter). Saved chunk counts from the broad census also
include unfinished neighboring chunks and must not be confused with these completed/sample counts.

The production Beta sample has **no anachronistic palette entries, no unknown non-air blocks, no modern
cave biomes and no valid saved structure starts**. Bedrock and all six historical ores are present.
It reports 4,183 suppressed feature placements, 54 suppressed structure starts, and 154 removed spawn
candidates across 98 candidate-list invocations. The actual decorator funnel suppresses synthetic bee-nest
and vine writes while allowing an oak-log write; those diagnostic writes do not modify the saved world.
The distant frozen-ocean/taiga/tundra sample likewise passes. Ordinary vanilla and upstream MB worlds
report zero historical feature/ore/structure mutations, retain all three decorator control writes and do
not acquire historical metadata.

Choose the **Historical Experience world type**, not the plain upstream Moderner Beta preset or an old
saved legacy recipe. Test a **fresh world** to see all corrected generation; existing chunks are deliberately
preserved. The older aliases remain resolvable for existing-world compatibility and are not repurposed.

### World-upgrade and final package acceptance

The **11 production world-upgrade controls pass**, including both interruption states and all refusal
controls. Old completed-chunk blocks, block entities/containers and the compressed player-data sentinel
are unchanged after advancing Beta revision 4 → revision 9 → Release 1.2.5 and generating distant chunks.
The final suite's last ordinary-world assertion was corrected for Gson's omission of null JSON fields;
the real refusal was rerun, not counted as success merely because startup exited.

[Final package verification](<../logs/sol-1211-final-verification-summary.json>) validates the exact artifact,
**187 tests / 24 suites / zero failed, errors or skipped**, five required generation hooks plus the early
upgrade-recovery hook, source notices, no vendored noise/router database and all ten public entries.
It reads actual saved 1.21.1 NBT (`DataVersion=3955`) and verifies all three revision-9 references. A fresh
production reload of this final jar preserves all 98 original full-chunk block hashes.

Install by replacing the previous hex1211 jar with the
[verified 1.21.1 artifact](<../build/libs/historical-experience-1211-0.1.0.jar>) (275,788 bytes).
SHA-256: `ece1d36b4429edc51d6b7b553174e27924453d207a1ff2b93179ff4231d36735`.
The user's existing Fabric API and Moderner Beta dependencies remain external. No personal saves or
Prism instance were modified by the verifier.

## Preserved limitations — do not confuse backport parity with complete historical fidelity

- This ports the **ten profiles that were implemented in 26.3**. It does not invent missing Pre-Classic,
  Classic, Indev or later Release coverage, and does not claim the entire originally requested timeline is done.
- The terrain generator remains Moderner Beta. Historical ore attempt counts, size/range and vein shape are
  retained, but individual vein positions do not equal old Minecraft at the same seed: the population RNG
  sequence remains the implementation's documented sequence.
- The generation envelope is 0..127; the inherited build/storage envelope remains **0..319**. A strict old
  player building-height rule is not implemented by this generation-only backport.
- Modern cave-biome fallback behavior is retained exactly per revision, including the latest single historical
  taiga fallback. It is not renamed to `none` where that would drop the original cave fix.
- Structure restrictions are researched/enforced for the declared pre-Beta-1.8 profiles. Later structure and
  spawn boundaries were incomplete in 26.3 and are not silently declared historically complete here.
- Nether/End generators remain the original unmodified vanilla declarations. **Historically accurate Nether/End
  generation is not claimed.** Feature suppression is generator-marker scoped, not a global registry rewrite.
- Server tests and simulated interruption recovery do not imply compatibility with every client modpack.
  The user's complete Prism modpack/client interaction remains a separate acceptance level.
- Gameplay/client presentation and Flashback remain deferred. Dormant compiled helpers do not constitute
  an enabled gameplay feature, and old chunks are never retroactively scrubbed or regenerated.

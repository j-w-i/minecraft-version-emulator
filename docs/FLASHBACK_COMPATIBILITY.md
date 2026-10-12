# Flashback compatibility — j-w-i fork vs Minecraft Historical Experience

Scope: assess the user's required Flashback fork as this project's recording/replay component.
Owner: Flashback compatibility worker. Owned: this document, `research/flashback-fork-evidence.json`,
and `scripts/verify-flashback-fork.ps1` (+ its Gradle init script). No fork source was edited; the
fork checkout is byte-clean.

**Machine-readable detail is in `research/flashback-fork-evidence.json`** — artifact hashes, per-file
source hashes, the full fix/mixin inventory, per-suite results, the nine-case E2E matrix, and the
verified/assumption/unknown lists. This document is the compact contract.

**No replay compatibility is claimed.** Nothing here was validated by recording, replaying or seeking
a real recording. Per `AGENTS.md`, "a recording is not replay compatibility".

---

## 1. Revision pin

| Fact | Value |
| --- | --- |
| Checkout (gitignored) | `.upstream/flashback-j-w-i` |
| Commit | `076b96b9fa400212d3d75fa2239d0f3836cf085b`, tag `v0.43.6.1`, branch `master` |
| Commit date / author | 2026-10-10T18:30:54+11:00, `JWI <jwi@j-w-i.org>` |
| HEAD tree | `34e756cf7eb22d390818be1ea628c4e5139c0ada` |
| Remote master tree | identical; `ls-remote` matches |
| Working tree | clean |
| Upstream base (comparison only) | `bf046b763ddf466bdc1b6d9c6cc7ba06e527f3e2`, "Bump version to 0.43.6" |
| Relationship (GitHub API) | fork of `Moulberry/Flashback`; compare `master` = `ahead_by 53`, `behind_by 0`, merge base = the base SHA |

The acceptance target is the fork, not upstream and not any published release.

**Clone caveat (corrected).** The clone is **shallow**: `.git/shallow` contains the commit itself and
the real parent object `de60274499cc997bd429ec5cb6dcf0e2e84a849e` is absent, so `git rev-list --count HEAD`
reports 1. That is **truncated local history, not a squashed commit** — the commit object does record
a parent, and GitHub lists 53 commits. An earlier revision of this document called it squashed; that
claim is withdrawn. Pin the **commit SHA**; use a full clone for per-commit provenance.

---

## 2. Build (verified, exit 0)

```
JAVA_HOME=<jdk25> ; cd .upstream/flashback-j-w-i ; gradlew.bat --no-daemon clean build
```

`BUILD SUCCESSFUL in 1m 7s`, 9 tasks (7 executed), exit 0. Log `logs/flashback-fork-build.log`.
Toolchain: JDK 25.0.4.1, wrapper 9.4.1, Fabric Loom resolved **1.15.5**.

**No JVM tests exist**: `:compileTestJava NO-SOURCE` and `:test NO-SOURCE`. A green `build` proves
compilation only.

**Reproducibility risk.** `build.gradle:14` declares `id 'net.fabricmc.fabric-loom' version '1.15-SNAPSHOT'`
— a moving SNAPSHOT, **not pinned**, unlike MC 26.3 / loader 0.19.5 / Fabric API 0.161.0+26.3 / shadow
9.3.0 / wrapper 9.4.1. It resolved to 1.15.5 here. A later rebuild can resolve a different Loom, so
this build is **not byte-reproducible** and the recorded hashes characterise *this* build, not the
revision. Record the resolved Loom version in every build log; never treat a hash mismatch alone as
evidence of a source change.

An earlier attempt failed at `:shadowJar` ("Unable to delete …-all.jar", a stale locked artifact from
an interrupted run); a clean rebuild fixed it with no source change.

---

## 3. Artifacts

| Artifact | Bytes | SHA-256 | Contents (verified by opening the zip) |
| --- | --- | --- | --- |
| `build/libs/merged-0.43.6.1.jar` | 201,277,088 | `F6DCC11FE132E55ACE33C67861A951BF2B77391F4E01B0300F74561FBDA6A51A` | **complete mod jar**: valid `fabric.mod.json` (id `flashback`, version `0.43.6.1`, client env, entrypoint `com.moulberry.flashback.Flashback`, depends minecraft `>26.2`, fabricloader `>=0.15.10`), `flashback.accesswidener`, `flashback.mixins.json`, icon, mod classes, `META-INF/jars/` (lattice, mixinconstraints, mixinsquared, commons-math3) and 799 `org/bytedeco/*` entries incl. `org/bytedeco/ffmpeg/avcodec/AVBSFContext.class` |
| `build/tmp/allJar/flashback-0.43.6.1-all.jar` | 199,104,753 | `A83FD7C2B80179FC6FCF9B7B7129F34010735CCD4273ABFA5C2E2E47A8C5AAD8` | shadowJar output |
| `build/libs/flashback-0.43.6.1.jar` | 22,696,306 | `76B95AB46A42F902378578DE3017E32FC55A08799D0E4F808F980D068968E090` | metadata present but **0 `org/bytedeco/*` entries** — incomplete |
| `build/libs/flashback-0.43.6.1-sources.jar` | 19,746,270 | `C30D…` (full value in JSON) | sources |

`build.gradle:172-184` registers `mergeShadowWithIncludes`, which merges the shadow jar into the plain
jar and then, in `doLast`, renames the result over `project.tasks.jar.archiveFile`. In the observed
run the task executed and its merged output was structurally complete, but the rename did not take
effect: the 22,696,306-byte jar persisted with the same hash across two independent clean builds.

**What is verified is the state, not the cause.** The merged jar's metadata, nested jars and bundled
natives were confirmed by reading the archive; whether the rename miss is a Windows file-handle issue
or a task-ordering interaction is not established, and is not needed for E2E. **Use
`build/libs/merged-0.43.6.1.jar`**, verifying its hash first. The plain jar was **never launched** and
neither was the merged one — "runnable candidate" means structurally complete, not verified to start.
A failure observed with the plain jar would be an artifact-selection artifact, not a fork defect.

---

## 4. License: non-redistribution vs. E2E setup

`LICENSE.md`: "Copyright 2024 Moulberry", **"Do not redistribute."**, "All rights reserved." Only
`src/main/resources/assets/flashback/lang` is separately CC BY 4.0.

The restriction is on **redistribution**, not on local use. These are different things:

* **Forbidden** — bundling, shading, hosting, re-uploading, committing or shipping fork jars, classes
  or assets in anything we distribute; copying Flashback implementation into our source. Owning the
  fork is not permission to redistribute the upstream-derived portions.
* **Permitted** — cloning, building and running the fork locally for validation, which is what this
  worker did. An E2E request authorises the fork as a **local test dependency**: the locally built
  artifact may be copied into a **project-isolated, gitignored test instance** (e.g. under `run/` or
  `.upstream/`) so the run is self-contained.
* **Practical rule** — nothing derived from the fork enters git or a distributable path, and the mod
  plus its tests must still work with Flashback **absent**.

An earlier draft invented a requirement that the user install the fork by hand into their own launcher
profile, and treated the licence as a blocker for local testing. That was wrong and is withdrawn.

---

## 5. Verification actually run (verified)

```
pwsh -NoProfile -File scripts/verify-flashback-fork.ps1     # exit 0, RESULT=OK
```

**16 behaviour tests passed, 0 failed, 0 invalid** (15 fork `devtools/checks` suites + the standalone
`ReplayInventorySlotsTest` seek model) — and **separately** 3 static source checks passed. The two are
counted apart deliberately: a source check is not a behaviour test and must never be added to that
count. 24 process launches recorded. Log `logs/flashback-fork-checks.log`.

The load-bearing result is `MixinTargetCheck`: *"All mixin targets resolve (60 targets, 198 methods,
54 shadows, 23 unchecked)"* against the real `minecraft-merged-f36920653e-26.3.jar`. That is static
proof the fork's injections resolve on MC 26.3 — not proof about a second mod at the same sites.
`PlaybackTimingTest` itself reports "in-game reproduction still required"; these are headless models.

### 5.1 Harness safety (rewritten after review)

The earlier `logs/run-fork-checks.ps1` was unsafe and is superseded. What the new harness enforces:

* Tool paths are checked with `Test-Path` before use; a missing tool throws and exits non-zero.
* **Real exit codes only.** `Start-Process` is used because `$LASTEXITCODE` is not reliably set for
  `.bat`/`.cmd` wrappers (`gradlew.bat`) and PowerShell reuses the previous value when a command fails
  to start. A null exit code means "never ran" → reported **INVALID**, never PASS, and any INVALID
  makes the run exit non-zero.
* **Argument quoting**: `Start-Process` does not quote for the caller; each argument containing
  whitespace is quoted, or the parser splits paths with spaces.
* **Classpath provenance**: the fork's own Gradle-resolved `runtimeClasspath` (162 entries) and
  `compileClasspath` (139; 172 unique combined) are dumped by an `--init-script` that lives outside
  the fork. No scanning of `~/.gradle` and no copy step, so unrelated cached versions cannot leak in
  and a copy failure cannot be swallowed. `runtimeClasspath` alone is insufficient: Starlight's
  `SWMRNibbleArray` is referenced by Flashback's compat code but only exists inside the compileOnly
  `mobrinth:scalablelux` jar, so source checks need `compileClasspath` too. Behaviour suites run
  against `runtimeClasspath`; compile and source checks use the combined set. Gson resolves to 2.14.0
  (MC 26.3 needs ≥ 2.11), which also removes the old first-match hazard.
* **Teardown safety**: the scratch path is resolved and asserted to be a descendant of the project
  root before `Remove-Item` may delete it.
* **Working directory**: the wrapper and suites run *in* the fork and this is asserted, because several
  suites open relative paths like `src/main/java/...`. Invoked from the project root, `gradlew.bat`
  picks up the root project's build and fails on an unrelated Loom variant — a real defect this
  harness hit and now guards.
* **Negative controls**: `-JavaPath`/`-JavacPath` point at a non-existent executable. Both cases exit
  1 having reported no PASS; this is the check that a launch failure cannot masquerade as success.

Ignored output (the Gradle classpath-dump log) is retained for diagnosis and is never allowed to
decide the result.

---

## 6. Replay contract (source-backed)

**6.1 Format unchanged.** Every file in `src/main/java/com/moulberry/flashback/action/` (all 11) is
**byte-identical** to upstream `bf046b7`. The fork adds no new action type and no serialized field.
`ReplayReader` builds index→`Action` from action **names** in the file and throws on an unknown action
whose path does not end in `optional`, so a new action would break stock readers. Fork additions to a
recording travel as ordinary game packets inside `flashback:action/game_packet`.

**6.2 Recording.** `Recorder.writePacketAsync`: `GuiRecording.observePacket` runs *before* filtering
(deliberately, to capture client prediction the server never echoes); player chat is rewritten to
system chat; `fabric-screen-handler-api` payloads are dropped; `IgnoredPacketSet.isIgnored` decides
the rest; and the recording player's **own** entity-data/equipment packets are dropped because the
replay server owns that player. `IgnoredPacketSet` became three sets: `IGNORED` (never written),
`IGNORED_IN_REPLAY` (written, not applied), `CONTAINER_PACKETS` (written only when
`internal.showGuisInReplays`), with the fork warning that membership in both the container and ignored
sets silently disables the feature.

**6.3 Real snapshots.** `Recorder.writeSnapshot(true)`, every `CHUNK_LENGTH_SECONDS = 5*60` (300 s) and
at finish, writes configuration/registry, loaded chunks, entities, RTC, the container snapshot payloads,
and — gated on `recording.recordHotbar` — XP, health/food/saturation, held slot and all 9 hotbar slots.

**6.4 Playback.** `ReplayServer.handleGamePacket` skips undecodable packets instead of aborting.
Custom payloads take a raw-bytes path avoiding reserialize round-trips ("some mods are poorly written
and the packet != encode(decode(packet))"), after filtering against `advanced.ignoredCustomPayloads`;
during a snapshot each raw payload is also appended to `customPacketsInSnapshot`. Unsupported types
throw `UnsupportedPacketException` from ~50 handlers, but those types are also in `IgnoredPacketSet`,
so the throw is a guard rather than the normal path. `lenient_registry` mixins tolerate registry
mismatches at playback.

**6.5 Seek.** Snapshot-anchored, **not** re-simulated from tick 0. `handleActions` jumps to the floor
snapshot: `playSnapshot` → `clearDataForPlayingSnapshot()` (boss bars, `playerInfoMap`),
`handleSnapshot` (rewind, clear `customPacketsInSnapshot`, dispatch all snapshot actions), flush,
`updateRtc`. Chunks come from `ReplayChunkCache`, never regeneration; a miss logs
`"Missing cached level chunk"`. Snapshot-time custom payloads are redelivered on join via
`PlayerList.sendLevelInfo` — how a container open *before* the seek target reappears.

**6.6 Configuration.** Config version 2 in both. Fork adds `advanced.interpolateToLatestPosition=true`,
`advanced.recordGuiEvents=false`, `internal.showGuisInReplays=true`, `exporting.proResProfile=STANDARD`.

**Trap:** `recording.recordHotbar` stays `false` (as upstream) while `showGuisInReplays` defaults
`true`. Defaults therefore record container packets but **no** hotbar/XP/health. State both flags
explicitly in every run.

---

## 7. Co-existence risks and the E2E matrix

Fixes and exact mixin targets are inventoried in the JSON. What matters for a terrain mod sharing the
runtime:

* **Worldgen tolerance — the key existing adaptation.** `mixin/replay_server/MixinServerLevel.java:25,66-90`
  `@WrapOperation`s `ServerLevel.<init>` → `ServerChunkCache.getGeneratorState()` and
  `ChunkGeneratorStructureState.ensureStructuresGenerated()`, returning null/no-op for a `ReplayServer`,
  with the source comment *"Fix for worldgen mods injecting on getGeneratorState to add custom worldgen
  properties. Lets just nuke the whole line"*. Evidence of **intent**; not proof Moderner Beta works.
* **Chunk generation is short-circuited** in replay (`MixinChunkStatusTasks`); `MixinChunkGenerators`
  registers a `flashback/empty` codec at bootstrap.
* **`MixinLevelExtractor.extractPlayerState` cancels vanilla first-person extraction** and re-extracts
  through the entity dispatcher — the likeliest hard conflict with entity-culling mods.
* **Weather fixes wrap three call sites inside `WeatherEffectRenderer.prepareInstances`** plus
  `VertexConsumer.setUv` and `ClientLevel.getGameTime` — render/shader mods are the conflict candidates.
* `injectors.defaultRequire = 1`: a moved injection target fails **hard**, so treat an MC or Moderner
  Beta update as a re-verification trigger (`MixinTargetCheck` is the fast gate).

`flashback.mixins.json` differs from upstream by exactly three client entries
(`gui.MixinAbstractContainerScreen`, `gui.MixinScreenGuiRecorder`, `gui.MixinMinecraftGuiRecorder`);
the weather mixin already existed upstream and was modified in place.

**Minimal E2E matrix** (full criteria in the JSON). All cases unverified.

| # | Test | Pass criterion (abbreviated) |
| --- | --- | --- |
| 0 | Install `merged-0.43.6.1.jar` in Fabric 26.3 / loader 0.19.5, Java 25, launch, open editor | Loads with no mixin apply failure. **Blocks all others.** |
| 1 | Record ≥60 s (movement, block break/place, container open+close, rain/snow), replay | Actions in order; terrain renders; container round-trips; hotbar/XP/health/food match |
| 2 | Seek back past a container, forward >300 s (real snapshot boundary), to tick 1, repeatedly | No partial container state, no stale crack overlay, profile still correct, no "Missing cached level chunk" |
| 3 | In rain, cross block boundaries slowly incl. vertically; seek mid-rain | No texture jump; weather continues across seek |
| 4 | **Moderner Beta world**: record, replay, seek; then repeat with the mod removed | Replay shows *recorded* terrain and does not generate; no worldgen/registry exception |
| 5 | Attempt historical gameplay rules during playback/pause/seek | No rule fires on the synthetic world; recorded world unchanged (hash compare) |
| 6 | Record under one registry set, replay under another | Tolerated or fails safely with a diagnosable log — never silently wrong |
| 7 | Our payloads through record→seek, with and without `advanced.ignoredCustomPayloads` | With the ignore entry: absent and nothing breaks; without: behaviour recorded precisely |
| 8 | Seek into the middle of a trade, a mount screen, the player inventory | Reconstructed or absent, never partial; no item in a slot that cannot hold it |

Per run capture: fork jar hash, jar filename, MC/loader/API/Java versions, full mod list, both config
flags, recording file size+hash, `logs/latest.log`, crash reports; for #4 the seed and preset ID.

---

## 8. Verified vs. assumed

**Verified:** revision/tree equality and fork relationship; clean build exit 0 and the absence of JVM
tests; artifact sizes/hashes and the merged jar's metadata, nested jars and bundled natives; the
license text; 16 behaviour tests and 3 static source checks passing with real exit codes and two
negative controls; `MixinTargetCheck` 60/198/54; all `action/` files byte-identical to upstream; the
recording, snapshot, playback, seek and config behaviours in §6; the three-entry mixin delta;
`MixinServerLevel`'s worldgen neutralisation.

**Assumed / unknown (no proof):** that either built jar launches (neither was started); the cause of
the merge/rename miss; that "worldgen mods" in `MixinServerLevel` includes Moderner Beta and suffices
for it; that a stock 0.43.6 client would ignore rather than reject the extra container payloads (only
the *action* stream is proven identical); that the accurate-position timeline never mutates live
interpolation during preview; that our own payloads survive record→replay→seek; whether our payload is
delivered at the same points as the container payloads on every seek; whether `showGuisInReplays=false`
changes that path; whether a stock 0.43.6 binary replay loads in the fork (project JSON is covered by
`LoadCompatTest`, the binary format is not); real-session seek fidelity for block destruction, weather,
entity positions and camera.

---

## 9. Assessment for the parent (no architecture change assumed)

**Your profile-payload plan looks feasible without changing the fork or its file format**, because the
mechanism already exists for containers and the profile family is already snapshot-anchored:

1. `GuiRecording.snapshotState()` → `Recorder.writeSnapshot` lines 1222-1228 writes idempotent
   full-state payloads into the **real snapshot** ("A seek starts here, not at the next tick");
   `GuiRecording.tick()` is the per-tick delta channel. Copy that shape rather than adding an action.
2. XP, health/food/saturation, held slot and all 9 hotbar slots are **already** written into every real
   snapshot (lines 1126-1146), and `handleSetExperience`/`handleSetHealth` apply them to the replay's
   player and re-broadcast to viewers. A seek already restores them — **provided `recordHotbar=true`**.
3. Your constraints hold by construction: profile values are read from the live `LocalPlayer`'s
   in-memory fields at record time, so no historical metadata on disk is needed to render a HUD; and
   "server enforcement must not run on the synthetic world" is a constraint on **our** code —
   Flashback's guards will not enforce it for us, so add an explicit `Flashback.isInReplay()` guard.
4. Design around two facts: the recorder deliberately drops the recording player's own entity-data and
   equipment packets, and `ClientboundPlayerAbilitiesPacket` is ignored (`handlePlayerAbilities`
   throws) so abilities are never recorded; game mode is read per-render from the spectated player.

Open **measurements**, not deductions: delivery timing of a second payload type relative to replay
ticks and seek; the effect of `showGuisInReplays=false`; and whether to gate the profile payload on
`recordHotbar` or its own flag. Case #7 covers these.

**Next actions.** (1) The fork is optional and never redistributed; a project-isolated gitignored local
test instance is permitted, and the mod plus its tests must still work with Flashback absent.
(2) If E2E is authorised, start at case 0 with `merged-0.43.6.1.jar` (hash `F6DCC11F…`) and pin the
commit SHA. (3) Decide the profile-payload shape using the container precedent. (4) Add and test the
in-replay invalidation guard on our server-authoritative rules. (5) `advanced.ignoredCustomPayloads` is
the only lever that keeps a mod's payloads out of replays without touching Flashback — document it.

**Reproduction.** Revision `git -C .upstream/flashback-j-w-i rev-parse HEAD`. Build
`gradlew.bat --no-daemon clean build` in the checkout. Checks
`pwsh -NoProfile -File scripts/verify-flashback-fork.ps1`; negative control
`… -JavaPath 'C:\definitely-not-a-jdk\bin\java.exe'` (must exit 1, no PASS).

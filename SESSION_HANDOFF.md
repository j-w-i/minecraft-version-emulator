# Minecraft Historical Experience — session handoff

> **2026-10-13 OVERRIDE: the old handoff below is historical, not current instructions.**
> The owner explicitly retargeted the active mod to **Minecraft 1.21.1**, in [the active project](<>).
> Sol is directly repairing ALL generation features actually implemented in the preserved 26.3 reference.
> World upgrading is explicitly included by the owner's follow-up: verify backup/journal, restart,
> destination future chunks and old blocks/containers/player preservation. Gameplay/client behavior
> and Flashback are excluded from this tranche. Do not revert the runtime to 26.3,
> do not use Nostalgic Tweaks' 1.21.4 branch, and do not re-enable unverified gameplay mixins.
> This repair is accepted: **187 tests / 24 suites, 15 generation controls and 11 real production upgrade
> controls passed**, plus a final remapped-jar production reload. All ten existing profile recipes and five
> generation hooks are restored; upgrade recovery runs under the world lock before generator decode.
> Artifact and checksum: [final verification](<logs/sol-1211-final-verification-summary.json>).
> Read [the current backport report](<docs/BACKPORT-1211.md>) and the latest section of
> [project progress](<PROGRESS.md>) before the old notes below. The correct branch for later behavior work
> is Nostalgic Tweaks `origin/1.21`, supporting 1.21–1.21.1.


**Export date:** 2026-10-12, Australia/Sydney (AEDT). **Status:** incomplete development checkpoint, NOT a finished mod or accepted release.

> **Latest continuation:** This is the preserved old-session handoff, not current runtime/priority status. Current build has **171 tests / 20 suites**, **18 verdict negatives + positive**, and repeated real lifecycle controls. **Five immutable generation-content revisions exist and were measured in real worlds**: revision 3 fixes the envelope (`minY=0`, **bedrock at y=0..3**, sea level 64, zero deepslate/cave-biome content), revision 4 adds the first generation-time content restrictions (272 placements suppressed), and **revision 5 places Beta 1.7.3's own ores** from a vein algorithm re-derived from the hash-verified client (diamond 53→15, redstone 31→14, lapis 55→18 at the same seed), while a freshly generated revision-4 world still shows the vanilla overshoot with zero veins attempted. **Structures are gated** by the supported world option, and **the gameplay layer has two verified rules**: era-restricted natural spawning (bat, turtle, enderman, witch, zombie_villager and glow_squid removed, weights preserved) and **the era's mob drops** (zombie/chicken → feather, spider → string, cow → leather, with modern tables not appearing), plus experience suppression. Backend-only or content-preview success is NOT historically valid content: **individual ore vein positions do not match a real Beta world at the same seed**, spawn **weights**, ore-band distributions and decoration are not historical, building-height rules and the Nether/End are unverified, coverage is one seed and one biome, **experience suppression is applied but unexercised** because a playerless harness cannot exercise it, and every content-preview verdict states `preview.fullHistoricalContentAcceptance=false`. Nostalgic Tweaks selective source/implementation audit remains an additional workstream and Release1.8.9 an added target. Read [current progress](<PROGRESS.md>), [gameplay rules](<docs/GAMEPLAY_RULES.md>), [generation contract](<docs/HISTORICAL_GENERATION_CONTENT.md>) and [review notes](<docs/REVIEW_NOTES.md>) before older instructions. Active goal is armed; no commits since the original docs commit.

This document preserves the important decisions, requirements, evidence, failures, rejected work and continuation instructions from the session. Read this before older worker documentation. [PROGRESS.md](<PROGRESS.md>) is the shorter authoritative status; [AGENTS.md](<AGENTS.md>) defines the engineering contract.

## 1. Paste this into the new session

```text
Continue the Minecraft Historical Experience project in C:\Users\jwalr\Minecraft Version Emulator.
Read SESSION_HANDOFF.md, PROGRESS.md and AGENTS.md before changing anything.
Keep Minecraft 26.3 as the runtime. Moderner Beta remains terrain authority;
Flashback MUST be my j-w-i fork. All original historical gameplay, progression,
export and recording/playback/seek requirements remain mandatory.
Act as GPT-6.1 Sol architect/reviewer. Discover actual subagent routes first;
use DeepSeek 4.1 Flash for substantial Fabric implementation and Qwen3.6-35B-A3B
for initially narrow, independently verified data/tests/features.
The old session is paused. If this fork inherited its goal, get_goal and explicitly
resume it. If this is a blank session, create the project goal from this request.
Continue autonomously with bounded ownership, real tests and accurate persistent docs.
First fix the CURRENT registry/lifecycle runtime failure, NOT the old already-fixed defects.
Do not consume research/beta173-availability.json: it is rejected provenance-contaminated data.
```

A fork may retain the old model allowlist. Check `list_subagent_models` before assigning Qwen. If it still exposes only DeepSeek, use a genuinely new blank session plus this document. Do not pretend that a rejected route was invoked.

## 2. Human requirements and permissions

- Build a production-quality **Minecraft Historical Experience** mod inside **Minecraft Java 26.3**. Historical eras run inside the modern engine; NEVER replace it with an old Minecraft executable.
- Cover **Pre-Classic through Release 1.7.10**, including intervening Classic/Indev/Infdev/Alpha/Beta eras. Do not omit versions or silently substitute the nearest terrain era.
- Historical fidelity includes availability/acquisition, blocks/items/entities/metadata variants, crafting/smelting/loot/drops, movement/water/physics, combat, food/health/hunger/survival, mob AI/spawning, world generation/structures/build bounds, lighting/visuals, historical HUD/UI/assets/audio and era-specific behavior. A terrain preset is NOT a complete gameplay profile.
- **Moderner Beta is the terrain foundation.** Sol has authority to choose dependency/source/hybrid integration with evidence and license review; current ADR chooses external dependency. Do not recreate its terrain algorithms.
- Safe version progression must support skipping versions, preserve player builds and unsupported stored content, and have backups, journal/crash recovery and tested boundary/concurrency behavior. No live generator-reference swap.
- Historical world export is mandatory. Storage-format/version bounds, unsupported content and conversion loss require explicit validation/preservation policies; no silent destructive conversion.
- Genuine compatibility with **[j-w-i/Flashback](https://github.com/j-w-i/Flashback)**: launch, record, play back, seek and snapshot restoration must actually be tested. User explicitly corrected the target away from upstream releases.
- Sol manages architecture, review, difficult reasoning and final acceptance; delegate substantial work rather than doing everything in the manager context. Optimize accepted output, not call count. Keep durable progress, verified claims and focused reviewed commits.
- The user explicitly accepted Minecraft EULA through question `minecraft_test_eula`, authorizing `eula=true` **only for isolated project test server**. This is not permission to alter personal `.minecraft` settings/saves/accounts or accept new unrelated legal terms.
- Future operators must opt in themselves; the fixture requires explicit `-AcceptEula`. Current test instance is project-local and ignored. Parent added loopback binding to avoid exposing an unauthenticated offline server.
- User requested the three-agent workflow, then asked to export this session to start/fork another with Qwen access. No current worker is meant to keep modifying files during transfer.

## 3. Goal and orchestration state

Latest actual `get_goal` result:

```text
id: goal-7d1382ff-5f2e-4ba0-9b8e-0619fc966f2a
revision: 8
phase: paused
activation: disarmed
roundsStarted: 2
maxGoalRounds: 256
objective: Deliver the Minecraft Historical Experience mod targeting Minecraft 26.3
with Moderner Beta terrain, historical gameplay profiles, safe progression,
historical export, and verified compatibility with the user's j-w-i Flashback fork;
autonomously implement and verify milestones with persistent documentation.
```

Latest registries report **no subagents and no background jobs**. Earlier session agents all finished; don't rely on their old ids being continuable after transfer. Old session remains paused to prevent duplicate work. Project is NOT complete and NOT marked blocked. A fork/resume disarms goals: after the human continuation request, read the goal and resume the inherited goal, or create the goal in a blank session.

### Model availability issue

Old session repeatedly exposed only `deepseek-official/deepseek-flash` (DeepSeek-V41-Flash; supported efforts off/low/high/max). Both uppercase and lowercase attempts to inspect a Qwen model under that provider were rejected as not allowed. **No Qwen task was actually run.** User says new sessions can invoke Qwen; exact provider/model route is unknown here and must be discovered.

Harness implementation inspection found the reason: allowed child routes are sampled when a fresh top-level session is composed and then persisted. Existing sessions use their persisted selection; children inherit it. Thus restarting the web server and reopening the same session would NOT refresh this allowlist. Relevant [Harness selection code](<C:/Users/jwalr/AppData/Roaming/npm/node_modules/@deepseek-ai/dsh/node_modules/@deepseek-ai/dsh-tool-subagent/lib/index.js#L588-L608>). No Harness process was killed, restarted or patched.

## 4. Environment and dependency pins

| Component | Verified value |
|---|---|
| Workspace | `C:\Users\jwalr\Minecraft Version Emulator` |
| JDK | Java25, `C:\Program Files\Java\jdk-25.0.4.1` |
| Node / Git | Node24, Git2.50.1 |
| Minecraft | 26.3, unobfuscated; no mappings |
| Wrapper | Gradle9.8.1; no system Gradle required |
| Loom | `net.fabricmc.fabric-loom`1.18.4 |
| Loader | Fabric0.19.5 |
| Main Fabric API | 0.160.4+26.3 |
| Moderner Beta | Modrinth version id `Ucgo1y8G`; display `5.0.0-alpha.4+26.3-fabric` |
| Mod id/package | `historical_experience` / `net.historicalexperience` |

Configuration: [build](<build.gradle.kts>), [properties](<gradle.properties>), [settings](<settings.gradle.kts>), [wrapper properties](<gradle/wrapper/gradle-wrapper.properties>).

### Moderner Beta provenance

- Published release tag `5.0.0-alpha.4` = **95f3da6c3f23e7e128b319e5c721201ed4c17243**.
- Initial shallow multiversion clone at **87c015b63e8618795dbeafe41166a3818eb81f8f** is DIFFERENT; do not use that branch as exact release evidence.
- Published jar SHA256 **2da0b0ded6053dbc18b630d73c3252e30f961ad920589edf75a6dbe243220727**; SHA1 **6950ab246f86e66c0a763baeb09fc429f16c5049**.
- Ignored local material: [.upstream](<.upstream/>) contains source checkouts, release tree, downloaded jar and extracted data. It is excluded from portable export.
- Research: [integration document](<docs/MODERNER_BETA_INTEGRATION.md>), [evidence JSON](<research/moderner-beta-evidence.json>), [ADR](<docs/ADR-001-terrain-dependency.md>).
- Parent verified 71 published preset hashes, two embedded preset JSONs, and five height records against extracted artifact files. This is source/artifact evidence, NOT historical runtime fidelity.
- MIT notice for research preset excerpts is preserved in [third-party notices](<THIRD_PARTY_NOTICES.md>).

### Required Flashback fork

- [j-w-i/Flashback](https://github.com/j-w-i/Flashback), pinned **076b96b9fa400212d3d75fa2239d0f3836cf085b**, tag/version **v0.43.6.1 / 0.43.6.1**.
- Minecraft26.3, loader0.19.5, FabricAPI0.161.0+26.3. Main API harmonization is pending, not silently performed.
- Ignored [fork checkout](<.upstream/flashback-j-w-i/>); [upstream comparison](<.upstream/flashback/>) is NOT the acceptance target.
- Fork source declares Loom **1.15-SNAPSHOT**, resolved1.15.5 in this build. Moving plugin means byte reproducibility is NOT established despite source/other pins.
- Locally built merged candidate SHA256 **F6DCC11FE132E55ACE33C67861A951BF2B77391F4E01B0300F74561FBDA6A51A**, 201,277,088 bytes. Thin artifact was ~22.7MB; merged contains bundled bytedeco and Fabric nested includes. Archive completeness does not prove it launches.
- Fork inspected license: **Do not redistribute / all rights reserved**. Local ignored clone/build/testing allowed; never ship/commit/bundle its jars, classes, assets or implementation. Portable export contains no fork artifact/source.
- Fork clone having one locally visible commit was shallow/truncated history, NOT proof of a squashed commit.

## 5. Architectural invariants and implemented slice

[Architecture](<docs/ARCHITECTURE.md>) and the ADR remain authoritative. Key decisions:

1. Keep global modern registries, modern networking and canonical modern world storage intact. Server owns gameplay restrictions; client is presentation/prediction. Never remove registry entries to hide historical content.
2. Separate gameplay profile ids from generation settings ids. Initial profile is Beta1.7.3 only, NOT a complete historical feature implementation.
3. Opt in via explicit project-owned `historical_experience:beta_1_7_3` marker retained in generator settings. Provider tuple resemblance or upstream Beta id never adopts an ordinary world.
4. Own settings preset now minimally inherits `moderner_beta:beta` in chunk/biome/cave settings. Earlier own-self references caused an infinite `mapPreset` loop and were removed. MB repeated inheritance maps local overrides over upstream; cycles remain unsafe.
5. Own world preset exists; vanilla `minecraft:normal` world-preset tag appends it with `replace:false`. An earlier private `historical_experience:normal` tag was unread by vanilla and corrected. Client label key is `generator.historical_experience.beta_1_7_3`; explicit **terrain prototype** wording avoids overclaiming. Client menu has NOT been exercised.
6. Metadata uses strict parsing, revision/schema/id/history checks, duplicate-key/trailing-document rejection and fail-closed states. Presence checks don't silently treat a directory/unreadable entry as an ordinary world. Unsupported/corrupt documents are preserved.
7. Initial publication uses forced unique temp plus `Files.createLink` no-clobber publication; unsupported hard links fail rather than degrade into a race. Updates use forced temp + atomic replacement; directory fsync is best effort. Parent corrected single FileChannel.write to loop until all bytes written. Creation concurrency tested with distinct and identical competing values.
8. Pure marker policy classifies only OVERWORLD, not nether/end. Existing historical metadata plus missing/mismatched marker fails closed. Registry failure is not ordinary-world evidence. Actual registry lookup integration currently fails and is the next repair.
9. No implemented gameplay engine, migrations/progression or exporter yet. Persisted unsupported content must remain, not be silently deleted or rewritten.

Main Java packages are [source tree](<src/main/java/net/historicalexperience/>): entrypoint/marker, profile, world/metadata, server lifecycle/probe/policy/column stats, and Moderner Beta adapter. Dependency types must stay inside [adapter package](<src/main/java/net/historicalexperience/integration/modernerbeta/>). Tests: [test tree](<src/test/java/net/historicalexperience/>). Resources: [resource tree](<src/main/resources/>).

### Terrain facts/unknowns

Pinned `overworld_128` noise envelope minY=-64, height192, inclusive maximum127 (NOT255). Modern overworld dimension envelope is minY=-64, height384, inclusive maximum319. Stock Beta settings have modern cave biomes/deepslate/features/spawns/structures; inheriting them is NOT fidelity. Source namespaces `minecraft:*` do NOT by themselves prove anachronism: valid old ores/springs/dungeons/reeds must not be stripped indiscriminately. Do not blindly enable `reduced_height` or claim unspecified packs are necessary. Need measured below-Y0/bedrock/waterline/ceiling probes and scoped historically appropriate biome/features/spawns. Current world preset retains vanilla modern Nether/End; those dimensions are NOT historically validated.

## 6. Verified tests versus latest failing runtime

### Main build/unit evidence

Parent independently executed:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat --console=plain test build --rerun-tasks
```

Exit0, [parent build log](<logs/parent-48-test-build.log>). Parent independently parsed XML: **88 tests / 9 suites / zero failed, errors or skipped**. Class counts: MetadataJson12; HistoricalMetadataStore11; HistoricalWorldState9; HistoricalIdentifier4; HistoricalProfiles5; HistoricalSettingsPresetData10; HistoricalMarkerDecision13; ColumnStats15; RuntimeProbePaths9.

Worker [release log](<logs/46-release-test-build.log>) also reports88. Counts progressed39→47→51(method-count-only interim)→88(executed). Don't repeat obsolete counts73 or63 as current total.

Seven verdict negative controls, exit0: [log](<logs/47-negative-verdict-controls.log>). They validate stale runId, wrong mode, absent/abort marker, FAIL, substring-not-PASS, missing runId, empty verdict. These are focused validator controls, not real game-abort launches. Run with `gradlew probeVerdictNegativeControls` after repairs. Gate must never accept stale PASS or EULA-aborted exit0.

### Previous implementation actually passed generation/reload

[Run A](<logs/31-run1.log>): nine real chunks,310076 non-air, marker `(8,126,8)`, real flush, PASS41 checks. [Run B](<logs/32-run2-reload.log>): separate fresh JVM, same world, marker survived,310077 non-air, PASS42 checks. Parent inspected these logs. This is a genuine earlier terrain/persistence prototype, NOT evidence the rewritten current code works.

Parent preserved that world unchanged as [baseline backup](<run/probe/world-baseline-20261012>) before preparing a fresh deterministic instance. It remains on the original host and is not included in the portable source archive.

### Latest source FAILS startup — exact continuation bug

- Parent fixture: [prepare script](<scripts/prepare-probe-run.ps1>), explicit seed **2508291534120694762**, existing authorized EULA opt-in, localhost offline server **127.0.0.1:25599**. Fresh [run instance](<run/probe/>) excludes personal saves/accounts.
- First parent launch: [log](<logs/parent-49-establish.log>), exit1 before game execution, `UnsupportedOperationException` from mutating JavaExec's read-only JVM-argument list. Parent corrected root build to use **setJvmArgs(...)**. Preserve this fix.
- Next launch: [latest log](<logs/parent-51-establish.log>), exit1. Correct unique id was passed: `establish-20261012T213304424-d5e78afc`.
- At SERVER_STARTING, [adapter](<src/main/java/net/historicalexperience/integration/modernerbeta/ModernerBetaAdapter.java#L409-L446>) obtains LEVEL_STEM via `server.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM)`. It catches RuntimeException and suppresses the cause, returning unavailable evidence. [Lifecycle](<src/main/java/net/historicalexperience/server/HistoricalServerLifecycle.java#L96-L125>) aborts fail-closed. There was no pre-existing fresh-world metadata; don't tell the operator their save is corrupt or simply remove files.
- FIRST repair: expose the exception and determine correct 26.3 level-stem ownership/access/lifecycle timing. Preserve explicit marker checks and ordinary-world protection; don't turn registry failure into unmarked/ordinary. Verify ordinary unmarked worlds as well as historical startup and corruption/mismatch cases.
- `moderner_beta:blocksource was empty after loading` recurs in the latest aborted run. Earlier claim that it was resolved/not recurring is superseded; classify only after a successful full run.
- Latest reload task **never ran** because establish failed. Do not invent a PASS or a reload log.

### Important probe semantics already established

- `MinecraftServer.halt(true)` on server thread joins itself: deadlock. Use `halt(false)`; verdict/run exit is a separate acceptance mechanism.
- `ServerLevel.save(null,true,true)` suppresses save entirely. Verified flush is **save(null,true,false)**.
- 26.3 region files are **world/dimensions/minecraft/overworld/region**, NOT old flat world/region. New probe derives dimension path and uses flat path only as failure diagnostic.
- New marker: chunk(-1,-1), local(8,8), y=minY+80 → absolute **(-8,16,-8)** for minY=-64. Earlier worker text `(8,16,8)` was wrong. Establish/reload must agree on current source.
- New column reader now uses absolute world coords with level.getBlockState and registry block ids, not local coords or `Block{...}` display strings. Bedrock/water bounds reported as absolute Y, not sample indexes. These changes have unit coverage, NOT successful runtime evidence yet.
- New watchdog default300000ms, hard floor180000ms, property `historical_experience.probe.timeoutMillis`; only enabled under probe flag. Avoid killing legitimate cold generation or treating timeout as proof of corrupt metadata.
- Verdict first line exactly `PROBE_RESULT PASS` or FAIL, then runId/mode/checks/failures. Harness removes stale artifacts and injects fresh run id, mode and absolute verdict path. Latest game failed before producing verdict; harness correctly rejected missing verdict, rather than accepting old PASS.
- Fabric GameTest hardcodes FLAT_ALL_DIMENSIONS/seed0 and resets its universe. It is NOT an MB terrain acceptance harness. Fabric GameTest property internally bypasses EULA; dedicated test now uses explicit user authorization instead.

## 7. Historical research: accepted method, limited coverage

[Feature JSON](<research/historical-features.json>), [closed schema](<research/historical-features.schema.json>), [sources](<docs/RESEARCH_SOURCES.md>), [tool README](<research/dev/README.md>), [pinned Python dependencies](<research/requirements.txt>).

-46 entries,33 expected paths,110 citations/25 unique URLs. Worker reported25/25 URLs reachable; parent independently reran schema/control checks, NOT every behavioral source claim.
- Coverage26 artifact_verified /19 documented_only /1 contradicted; confidence32 high /11 medium /1 low /2 unknown. All gameplay statuses `not_started`. This is NOT complete era/profile coverage.
- Standard jsonschema4.26.0 Draft202012Validator with schema self-check plus coverage consistency/unique ids/category/URL shape/path checks.10 controls (1 positive+9 negative). Format annotations are NOT enabled as assertions, explicitly documented.
- Artifact inspector now compares metadata identity and pinned identity, then byte hash/size against both. Worker tested tampered-cache rejection and clean-venv validation; don't substitute a string-absence scan for behavior proof.
- Corrected false `Beta1.7.10` scope: Beta1.0–1.8.1 and Release1.0.0–1.7.10. Food category start is unknown/food-specific; apple/bread chronology not confidently collapsed. Cow bucket interaction is **milking**, not mooshroom shearing. Mooshrooms aren't Beta1.8. An exhaustive Beta1.8.1 equivalence claim was withdrawn; that jar was not analyzed.
- Item constructor args are offset: bucket69→saved325, milk79→335. Don't confuse raw constructor arguments with saved ids.
- Official client hashes: b1.7.3 **43db9b498cb67058d2e12d394e6507722e71bb45**; b1.8 **3139e9c29b2c74f59ea04de760ac2af5bc21b410**; b1.8.1 metadata/client identity **6b562463ccc2c7ff12ff350a2b04a67b3adcd37b**, unanalysed.
- Source-backed b1.8 food findings include food20, saturation5 initially, regen at food≥18 every80ticks for1HP with no exhaustion charge, starvation floors10HP easy/1HP normal/none hard, saturation min(current+hunger*modifier*2, foodLevel), eating32ticks, sprint-jump0.8/non-sprint0.2/attack0.3 exhaustion. Don't transplant these into Beta1.7.3 or assume all Beta1.8.1 behavior matches without verification.

### Critical rejected availability candidate

[Candidate](<research/beta173-availability.json>) has parentReview=`REJECTED_DO_NOT_CONSUME`. **NO entry is approved for enforcement.**

Worker verified correct b1.7.3 hash, then actually extracted from pre-existing `%TEMP%\mce-hist\dec` belonging to **b1.8**. Confirmed by differing class shapes: b1.7.3 `lr` is NOT Block; b1.8 `lr`/`sv` are Block/Item and match that tree. b1.7.3 download postdates tree creation. Hash verification of one jar never established provenance of a different decompiled input.

Candidate wrongly contains Beta1.8-only blocks97–109 and items360–368, melon/stems, rotten flesh, enderman/silverfish; omits wool35; conflates dye black-first order with wool metadata. Claimed108blocks/115items/28entities are NOT Beta1.7.3 evidence. Worker debrief's suggested item upper bound368/no records is also WRONG; discard its proposed anchors. Need fresh executable, hash-bound extraction plus introduction-version negative controls and separate wool/dye verification. Don't reuse contaminated temp trees or scratch scripts. This is a narrow future audit suitable for Qwen only after actual route availability and close review; complex registry integration remains DeepSeek's job.

## 8. Flashback results and integration contract

[Compatibility doc](<docs/FLASHBACK_COMPATIBILITY.md>), [evidence](<research/flashback-fork-evidence.json>), [harness](<scripts/verify-flashback-fork.ps1>), [Gradle classpath dumper](<scripts/dsh-dump-classpath.init.gradle>).

Fork clean build succeeded; Gradle had no JUnit source tests. Separate harness actually exercises fork development checks. Parent reran with `-SkipClasspathDump` against previously resolved fork classpaths: **16 behavior suites +3 separately static checks**, all pass; positive exit0, missing Java/Javac each exit1. [Parent log](<logs/parent-flashback-verify.log>) records23 launches (worker earlier full classpath run24), not thousands of tests. Static mixin check:60targets/198methods/54shadows/23unchecked — unchecked is NOT verified. Native rendering isn't exercised by these checks.

Harness uses real Start-Process exit codes, rejects missing tools/null launch status, derives runtime/compile classpaths from fork Gradle resolution, verifies entries and safe project-local scratch before deletion. Old all-cache scan/Gson2.10.1 trap and swallowed native failures were withdrawn; resolvedGson2.14.0. RuntimeClasspath162+compileClasspath139, combined172. Source checks need compileOnly scalablelux classes; don't blindly conflate their classpath with shipping runtime.

**Neither jar has been launched in Minecraft. No real recording/playback/seeking case passed.** All9E2E cases remain unverified.

Contract findings (source evidence only):
- Snapshot/cached modern chunk replay, not historical resimulation. Replay action format matches upstream; no fabricated new action needed.
- Full profile state must be restored at snapshots and seeks. Custom snapshot payloads/container full-state precedent are relevant; packet retention must be actually tested.
- Fork already records health/food/XP/held slot/hotbar in real snapshots when **recordHotbar=true**; defaultfalse. **showGuisInReplays** defaulttrue. Explicitly record both flags in E2E evidence.
- Recorder drops local-player metadata/equipment and abilities; don't stash historical profile state only in those channels.
- Historical enforcement must not run as live survival simulation on synthetic ReplayServer. Avoid common-server hard imports of client-only Flashback types/global replay flags. Current registry-startup defect may also threaten replay startup; test isolation explicitly.
- ReplayServer public goToReplayTick/getReplayTick/getTotalReplayTicks and Flashback startRecordingReplay/finishRecordingReplay/openReplayWorld APIs are available in inspected fork. GUI/game launch automation still needs an actual bounded client harness.

## 9. Review lessons and ownership

Old workers: bootstrap c8d79fe7; terrain744733bc; historical80f819fd; fork a0290103; runtime reviewer4c6768cd. They finished; their reports are evidence candidates, not authority.

Broad bootstrap required multiple rejection cycles: preset self-cycle, ignored private tag namespace, incomplete failure handling/flush/reload, source type leaks, no-clobber claims, dropped Gradle resource/JUnit blocks, and immutable args mutation. Runtime reviewer initially reintroduced flat region path and local-coordinate/identifier mistakes; parent required bounded corrections. Availability worker used wrong artifact and was rejected. Adapt task decomposition; don't blindly retry broad work or trust success wording. Source files were edited during one shared build, producing transient compile failures; freeze owners before integration and have ONE main build coordinator.

Future allocations: Sol contract/architecture/actual review; DeepSeek substantial Fabric/mixins/API debugging; Qwen initially small source inspections, facts/data, isolated tests/refactors with clear independent controls. No workflow orchestration tool unless user explicitly requests large workflow orchestration.

## 10. Exact next sequence

1. Read contract/status/handoff; discover actual routes; resume/create goal after human continuation; check git/active jobs. Old goal stays paused.
2. Assign DeepSeek a narrow adapter/lifecycle registry repair, with actual26.3 API evidence and no unrelated rewrites. Expose swallowed exception; keep normal-world preservation and marker mismatch/corrupt fail-closed tests. Sol reviews actual diff.
3. Rebuild and independently count tests. Run real establish then separate reload with unique verdict ids and current marker; verify nineFULL chunks, non-air, correct columns, proper region storage and saved gold block. Keep failed logs, prove failure gate, don't treat older pass as current.
4. Fix prototype fidelity in scoped project presets/biomes/spawns/features, using MB terrain authority; measure bounds. No wholesale vanilla-namespace stripping or blind height pack.
5. Independently regenerate/validate Beta1.7.3 availability from correct jar before any acquisition/recipes/loot gating. Start bounded real gameplay slices, not only interfaces/data frameworks; server-authoritative rules preserve unsupported stored content.
6. In isolated ignored client instance, use required locally built fork for actual coexistence/record/playback/seek tests. Harmonize API only with a green baseline; preserve local license constraints.
7. Progression backup/journal/restart transactions, historical export writers, all eras and UI/assets remain mandatory. Keep profile fidelity/test coverage explicit and maintain persistent docs and reviewed focused commits.

## 11. Export and repository preservation

Only existing reviewed commit is **43cc58a** (initial docs). Implementation/research/current docs are mostly untracked/modified, so a git clone alone loses this work. No unreviewed implementation was falsely committed as accepted during handoff.

Export includes this handoff, updated status/contract/notices, complete project source/tests/resources/build/wrapper/scripts/research (including visibly rejected candidate), selected logs and test XML, plus a git history bundle and file/hash manifest. It excludes generated worlds, caches, upstream checkouts, Minecraft/Flashback jars/classes/assets, credentials and personal account/settings files. Those local test worlds/checkouts remain on the original host. The archive is a development-source checkpoint, not an installable release mod.

On the original machine use the existing workspace directly. On another machine unpack the archive, obtain Java25 and pinned dependencies, separately clone/build the permitted local Flashback fork, and explicitly prepare an isolated test server. Excluded test worlds are reproducible after fixing current startup; older original marker world remains only on the original host. Review [notices](<THIRD_PARTY_NOTICES.md>) before redistribution. Current status takes precedence over older worker claims in documentation.

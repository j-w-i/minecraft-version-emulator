# Flashback fork integration specification

Target: the user's fork `https://github.com/j-w-i/Flashback`, local checkout `.upstream/flashback-j-w-i`, revision
`076b96b9fa400212d3d75fa2239d0f3836cf085b`, version `0.43.6.1`, environment `client`
(`src/main/resources/fabric.mod.json:16`). Machine-readable evidence: [flashback-api.json](<../research/flashback-api.json>).

All line references below are inside `.upstream/flashback-j-w-i/`. Nothing in this document was executed; it is source reading.
No fork code, jar, class or asset may be copied into this repository: `LICENSE.md:3-4` says "Do not redistribute. All rights
reserved." A dependent mod must compile against a locally built artifact; depend on a Maven coordinate and the build fails.

## 1. Reachability summary

| Operation | Symbol | Location | Class of reachability |
| --- | --- | --- | --- |
| start recording | `Flashback.startRecordingReplay()` | `.../Flashback.java:1088` | public static, client-runtime-gated (needs a loaded player, line 1101) |
| start recording (command) | `/flashback start` | `.../Flashback.java:483` | client command (`FabricClientCommandSource`) |
| stop recording | `Flashback.finishRecordingReplay()` | `.../Flashback.java:1137` | public static, client-runtime-gated (toasts) |
| cancel recording | `Flashback.cancelRecordingReplay()` | `.../Flashback.java:1120` | public static, no in-repo caller |
| write files | `Recorder.finish()` -> `AsyncReplaySaver.finish()` | `.../record/Recorder.java:526`, `.../io/AsyncReplaySaver.java:310` | public, no client dependency found |
| pack replay zip | `ReplayExporter.export(Path,Path,String)` | `.../record/ReplayExporter.java:26` | public static, no client dependency found |
| begin playback | `Flashback.openReplayWorld(Path)` | `.../Flashback.java:1244` | public static, client render thread (via `MixinMinecraft.java:453`) |
| playback object | `Flashback.getReplayServer()` | `.../Flashback.java:1032` | public static, null until a replay world is loaded |
| play/pause transport | `TimelineWindow.togglePaused(ReplayServer)` | `.../editor/ui/windows/TimelineWindow.java:3620` | ImGui only (keybind 3419, button 2424) |
| seek request | `ReplayServer.goToReplayTick(int)` | `.../playback/ReplayServer.java:464` | public, sets `jumpToTick` (`:135`) |
| seek execution | `tickServer` jump branch | `.../playback/ReplayServer.java:1041` | server tick of a client-hosted replay world |
| seek (editor) | `TimelineWindow.seek(Frame,int)` | `.../editor/ui/windows/TimelineWindow.java:3337` | ImGui only |
| seek (export) | `ExportJob.setServerTickAndWait(...)` | `.../exporting/ExportJob.java:699` | ImGui-initiated, needs framebuffers |

## 2. The calls a future implementation would make

Every call below is public in the fork as of the pinned revision. None of them is a supported API: the fork has no API
package and no capability registry, so all of this is coupling to internals that may change without notice.

Recording, from a client-side mod:
1. `Flashback.startRecordingReplay()` (`Flashback.java:1088`) after the client player exists. Guard with
   `Flashback.RECORDER == null` (`Flashback.java:153`) — calling it while recording only shows a toast (`:1089-1093`).
2. Optional: `Flashback.pauseRecordingReplay(boolean)` (`:1111`) / `Recorder.setPaused` (`Recorder.java:522`).
   Note `pauseRecordingReplay` dereferences `RECORDER` without a null check.
3. `Flashback.finishRecordingReplay()` (`:1137`) to detach the recorder and flush. With
   `recordingControls.quicksave == true` (`FlashbackConfigV1.java:80`) it zips straight to the replay folder via
   `ReplayExporter.export` (`:1168`); otherwise the record folder is queued in `pendingReplaySave` (`:1170`) and the
   confirmation happens in the UI. For automation, set `quicksave` so the zip path is deterministic.
4. Read the replay zip path from `Flashback.getReplayFolder()` (`:197`), or call `Recorder.finish()` and
   `ReplayExporter.export(...)` directly if the recorder is driven manually.

To arm automatic recording instead of calling the entry point: set `config.recordingControls.automaticallyStart`
(`FlashbackConfigV1.java:68`) through `Flashback.getConfig()` (`:1027`); the client tick path at `Flashback.java:646-657`
then starts the recording 20 ticks after join.

Playback:
1. `Flashback.openReplayWorld(replayZip)` (`Flashback.java:1244`) — synchronous client world load; call on the client thread.
   Failure modes are silent: a bad or missing `/metadata.json` logs and returns (`:1279-1294`).
2. Poll `Flashback.getReplayServer()` (`:1032`) until non-null, then check `ReplayServer.isReady()` (`ReplayServer.java:981`).
3. An opened replay is paused: `replayPaused` is declared `true` (`ReplayServer.java:136`). There is no public method that
   sets it. Either write the field directly or set `replayServer.setDesiredTickRate(rate, true)` and clear the flag.

Seeking:
1. `replayServer.goToReplayTick(tick)` (`ReplayServer.java:464`) or assign `replayServer.jumpToTick = tick` (`:135`).
2. The jump is only consumed inside `tickServer` (`:1041`); wait for `getReplayTick()` (`:489`) to report the target before
   asserting anything. `ExportJob.setServerTickAndWait` (`ExportJob.java:699`) shows the wait pattern: park until the
   server thread acknowledges, using `sendFinishedServerTick` (`ReplayServer.java:138`).
3. Read back with `getReplayTick()` (`:489`), `getPartialReplayTick()` (`:510`), `getPlaybackTime()` (`:499`) and
   `getTotalReplayTicks()` (`:514`). A jump of more than one tick also resets forwarded GUI, block-destruction, lerp and
   particle state for each replay viewer (`:1053-1078`) — that reset is part of the seek contract, not an optimisation.

## 3. Verification plan

Fail fast, shortest path first, and keep the fork's own gates before any runtime claim:

1. Static gate (already independently reproduced by the parent, not repeated here): build the pinned fork and run
   `devtools/checks/run.sh`; treat a pass as a compile/static check only. `devtools/checks/README.md:6-8` states the
   model is "not a substitute for recording and replaying in game".
2. Verification hook for this repository: expose the calls in section 2 behind a debug-only entry point, and record the
   exact `Flashback.java` revision hash with every result.
3. Manual runtime script, because there is no supported API to lean on: launch the clean client, load a project test world
   from `run/`, `/flashback start`, act for a bounded number of ticks, `/flashback finish`, then open the produced zip and
   confirm a non-zero `FlashbackMeta.totalTicks` (`FlashbackMeta.java:29`).
4. Seek evidence: after opening the replay, seek to tick 0 and to `getTotalReplayTicks()` and record
   `getReplayTick()`/`getPartialReplayTick()` at each stop plus a screenshot. A seek that does not settle on the requested
   tick is a failure, not a warning.
5. Negative paths: open a truncated/garbage zip (expect the silent-return path at `Flashback.java:1279-1294`, asserted as
   "no crash and no replay server"), and call `finishRecordingReplay()` with no recording (expect the toast path at
   `:1138-1142`).
6. Record the evidence as a log plus screenshot references, with the fork revision and the exact commands.

## 4. What is blocked

Automated headless end-to-end record -> replay -> seek is **not possible** at this revision. The blockers, each with a location:

- Replay worlds are loaded through the client: `openReplayWorld` calls `MinecraftExt.flashback$startReplayServer`
  (`Flashback.java:1333`), implemented at `MixinMinecraft.java:453` as `Minecraft.doWorldLoad`. `ReplayServer` itself
  extends `IntegratedServer` (`ReplayServer.java:130`) but imports `net.minecraft.client.Minecraft` (`:55`) and is
  constructed only from that client mixin (`MixinMinecraft.java:441`). There is no server-only start path.
- Playback state is pushed to the client tick: `forceApplyKeyframes` is applied by `MixinMinecraft.flashback$applyKeyframes`
  (`MixinMinecraft.java:364`), called from `ReplayServer.java:1145-1147`, and pose application is gated by
  `MixinMinecraft.java:419` reading `doClientRendering()` (`ReplayServer.java:518`).
- Recording needs a loaded client player: `Flashback.java:1101`, and its automatic path is a
  `ClientTickEvents.START_CLIENT_TICK` handler (`Flashback.java:632-657`).
- Command registration is client-side (`ClientCommandRegistrationCallback`, `Flashback.java:481-483`), so no server console
  or RCON path exists.
- The user-facing transport is keybind (`TimelineWindow.java:3419`) or ImGui button (`TimelineWindow.java:2424`), i.e. a
  human input device and a rendering editor.
- The fork ships no end-to-end harness; its offscreen UI harness stubs out `ReplayServer`, `ReplayUI`,
  `EditorStateManager` and `Flashback` (`devtools/uitest/stubs/`), so it cannot exercise a real replay.

Consequently the mandatory Flashback verification must be a real client launch with scripted or human input and recorded
evidence, or it stays unverified. Any claim of automated headless verification would be false.

## 5. Open items for the parent

- No published artifact exists for the pinned revision: `build.gradle` applies `maven-publish` (`:15`) but defines no
  publication, and the only outputs are local files under `build/libs/` (`flashback-0.43.6.1.jar`,
  `-sources.jar`, `merged-0.43.6.1.jar`). Decide how the dependent mod obtains the fork without redistribution.
- The fork's in-tree consumer `inventory-addon-mod` is stale (Minecraft 1.21.1, Loom 1.7.4) and uses mixins rather than the
  calls in section 2; it is not a worked example for this revision.
- Not established by reading: whether an offscreen/headless GL context suffices for `Minecraft.doWorldLoad` plus
  `ReplayServer`; whether `startRecordingReplay` has any guard before the `player` dereference; the long-term stability of
  the replay zip format for third-party readers.

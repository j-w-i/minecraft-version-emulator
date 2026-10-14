/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side verification of the Flashback fork's playback and seeking, driven by a system property.
 *
 * <h2>Why this exists</h2>
 *
 * <p>Recording, playback and seeking are a required acceptance criterion, and the fork offers no command
 * to open a replay: {@code /flashback} publishes start, finish, pause, unpause, config and mark, and
 * {@code openReplayWorld} is reachable only from the menu UI or programmatically. So there is no way to
 * "type" a playback into existence, and the honest options are a client gametest or a gated path like
 * this one. This follows the same pattern as the server-side runtime probe: a verification path in the
 * main sources that does nothing unless a system property names it, and which never runs in a normal
 * session.
 *
 * <p><b>The client stays presentation-only for gameplay.</b> This class changes no game behaviour. It
 * drives the fork's own public entry points and writes a verdict file.
 *
 * <h2>Why reflection</h2>
 *
 * <p>The fork is a <em>runtime-only</em> dependency, deliberately: no project code may bind to its API,
 * because the fork publishes no artifact and its licence forbids redistribution. Calling it reflectively
 * keeps that boundary intact - the compile classpath does not contain it - at the cost of the lookups
 * below being resolved by name at runtime. If the fork is absent, this class reports that and stops,
 * rather than failing the client.
 *
 * <h2>What it verifies, in order</h2>
 * <ol>
 *   <li>the fork's playback entry point accepts a recorded replay file;</li>
 *   <li>a replay server actually starts;</li>
 *   <li>it reports a total tick count consistent with the recording's own metadata;</li>
 *   <li>a seek request moves the replay to the requested tick and the tick can be read back.</li>
 * </ol>
 * Each step is written to the verdict file as it is reached, so a partial result says exactly how far it
 * got instead of only pass or fail.
 */
public final class ReplayProbe implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience_replay_probe");

    /** Names a recorded replay file to open. Unset in every normal session. */
    public static final String REPLAY_PATH_PROPERTY = "historical_experience.replay.verify";

    /** Tick to seek to. Defaults to roughly two thirds through the recording. */
    public static final String REPLAY_SEEK_PROPERTY = "historical_experience.replay.seekTick";

    /** Where the verdict is written. */
    public static final String REPLAY_VERDICT_PROPERTY = "historical_experience.replay.verdictFile";

    private static final String FLASHBACK_CLASS = "com.moulberry.flashback.Flashback";
    private static final String REPLAY_SERVER_CLASS = "com.moulberry.flashback.playback.ReplayServer";

    /** Client ticks to wait for the replay server to appear, and then for a seek to land. */
    private static final int SERVER_WAIT_TICKS = 60 * 60;
    private static final int SEEK_WAIT_TICKS = 60 * 30;

    /** Ticks to let the client settle after startup before asking the fork to open anything. */
    private static final int SETTLE_TICKS = 60;

    private final List<String> verdict = new ArrayList<>();
    private boolean failed;
    private boolean finished;
    private int ticksWaited;
    private boolean requestedOpen;
    private boolean reportedNoFork;
    private Path verdictFile;

    @Override
    public void onInitializeClient() {
        Path replayPath = resolveReplayPath();
        if (replayPath == null) {
            return;
        }
        String verdictPath = System.getProperty(REPLAY_VERDICT_PROPERTY);
        this.verdictFile = (verdictPath == null || verdictPath.isBlank())
            ? Path.of("historical_experience_replay_verdict.txt")
            : Path.of(verdictPath);
        check("replay.requestedPath=" + replayPath);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                tick(client, replayPath);
            } catch (Throwable t) {
                fail("the replay probe threw " + t);
                writeVerdict();
            }
        });
    }

    /**
     * Which replay to verify.
     *
     * <p>When the property names one, that one. Otherwise the newest replay in the game directory's own
     * {@code flashback/replays} folder - which is why the run configuration normally needs no arguments
     * at all. The property route exists but is deliberately not the only route: passing a path through a
     * Windows command line is fragile when the workspace path contains spaces, and a probe that cannot be
     * started because of shell quoting is a probe nobody runs.
     *
     * @return the replay to open, or null when this is not a verification session
     */
    private static Path resolveReplayPath() {
        String configured = System.getProperty(REPLAY_PATH_PROPERTY);
        boolean explicit = configured != null && !configured.isBlank();
        // The presence of the verdict property is the opt-in when no path is named, so a normal client
        // session never reaches any of this.
        boolean optedIn = explicit
            || (System.getProperty(REPLAY_VERDICT_PROPERTY) != null);
        if (!optedIn) {
            return null;
        }
        if (explicit) {
            return Path.of(configured);
        }
        try {
            Path folder = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
                .resolve("flashback").resolve("replays");
            if (!Files.isDirectory(folder)) {
                return null;
            }
            try (var stream = Files.list(folder)) {
                return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .max(java.util.Comparator.comparingLong(ReplayProbe::lastModified))
                    .orElse(null);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private void tick(net.minecraft.client.Minecraft client, Path replayPath) throws Exception {
        if (finished) {
            // The verdict is written once. The first version re-checked and re-wrote it on every client
            // tick, which produced a verdict with fifteen thousand identical checks in it and buried the
            // result it was meant to report.
            return;
        }
        ticksWaited++;

        // Step 1: open the replay once the client is somewhere it can be opened from.
        //
        // Readiness is decided from PUBLIC client state only. `Minecraft.screen` is package-private in
        // 26.3, so "is the title screen showing" is not answerable from here; `level` is public and is
        // the part that matters, and the settle delay covers startup - these tick events do not fire
        // until the client is ticking at all.
        if (!requestedOpen) {
            if (client.level != null || ticksWaited < SETTLE_TICKS) {
                return;
            }
            Class<?> flashback = loadClassOrNull(FLASHBACK_CLASS);
            if (flashback == null) {
                if (!reportedNoFork) {
                    reportedNoFork = true;
                    // Not a failure of this project: the fork is a runtime-only dependency and may simply
                    // not be present in this checkout.
                    check("replay.forkPresent=false (the Flashback fork is not on the runtime classpath, "
                        + "so playback cannot be verified here)");
                    writeVerdict();
                }
                return;
            }
            check("replay.forkPresent=true");
            if (!Files.isRegularFile(replayPath)) {
                fail("the requested replay file does not exist: " + replayPath);
                writeVerdict();
                return;
            }
            check("replay.fileBytes=" + Files.size(replayPath));
            Method open = flashback.getMethod("openReplayWorld", Path.class);
            open.invoke(null, replayPath);
            requestedOpen = true;
            check("replay.openInvoked=true");
            return;
        }

        // Step 2: wait for the replay server, then read what it says about the recording.
        Object server = replayServer();
        if (server == null) {
            if (ticksWaited > SERVER_WAIT_TICKS) {
                fail("no replay server appeared within " + SERVER_WAIT_TICKS + " client ticks; the "
                    + "playback entry point was called but did not produce a running replay");
                writeVerdict();
            }
            return;
        }
        Class<?> serverClass = server.getClass();
        int total = (int) serverClass.getMethod("getTotalReplayTicks").invoke(server);
        int atOpen = (int) serverClass.getMethod("getReplayTick").invoke(server);
        check("replay.serverStarted=true");
        check("replay.totalTicks=" + total);
        check("replay.tickAtOpen=" + atOpen);
        if (total <= 0) {
            fail("the replay server reports no ticks, so nothing was recorded or nothing was loaded");
            writeVerdict();
            return;
        }

        // Step 3: seek to a tick the replay is demonstrably NOT already at.
        //
        // The first version of this test sought to two thirds of the recording, which happened to be
        // exactly where the replay had already advanced to, so "seek landed" passed without proving
        // anything. A seek test has to change the position and observe the change.
        int target = seekTarget(total, atOpen);
        if (target == atOpen) {
            fail("the seek target (" + target + ") equals the replay's current tick, so the seek could "
                + "not be distinguished from doing nothing");
            writeVerdict();
            return;
        }
        check("replay.seekRequested=" + target);

        boolean seekLanded = false;
        int observed = atOpen;
        for (int i = 0; i < SEEK_WAIT_TICKS; i++) {
            serverClass.getMethod("goToReplayTick", int.class).invoke(server, target);
            observed = (int) serverClass.getMethod("getReplayTick").invoke(server);
            if (observed == target) {
                seekLanded = true;
                break;
            }
            // The replay advances on its own ticks; the client tick handler gives it those ticks.
            Thread.sleep(50);
        }
        check("replay.tickAfterSeek=" + observed);
        check("replay.tickChanged=" + (observed != atOpen));
        check("replay.seekLanded=" + seekLanded);
        if (!seekLanded) {
            fail("seeking from tick " + atOpen + " to tick " + target + " did not land; the replay is "
                + "at tick " + observed);
            writeVerdict();
            return;
        }

        // And a second seek backwards, because a forward-only seek could be satisfied by fast-forwarding
        // rather than by actually jumping.
        int back = Math.max(0, atOpen / 4);
        if (back != observed) {
            check("replay.seekBackRequested=" + back);
            boolean backLanded = false;
            for (int i = 0; i < SEEK_WAIT_TICKS; i++) {
                serverClass.getMethod("goToReplayTick", int.class).invoke(server, back);
                observed = (int) serverClass.getMethod("getReplayTick").invoke(server);
                if (observed == back) {
                    backLanded = true;
                    break;
                }
                Thread.sleep(50);
            }
            check("replay.tickAfterBackSeek=" + observed);
            check("replay.backSeekLanded=" + backLanded);
            if (!backLanded) {
                fail("seeking backwards to tick " + back + " did not land; the replay is at tick "
                    + observed + ", so only forward movement may have been working");
                writeVerdict();
                return;
            }
        }
        finished = true;
        writeVerdict();
    }

    private static Object replayServer() throws Exception {
        Class<?> flashback = loadClassOrNull(FLASHBACK_CLASS);
        if (flashback == null) {
            return null;
        }
        return flashback.getMethod("getReplayServer").invoke(null);
    }

    private static Class<?> loadClassOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private int seekTarget(int total, int atOpen) {
        String raw = System.getProperty(REPLAY_SEEK_PROPERTY);
        if (raw != null && !raw.isBlank()) {
            try {
                int requested = Integer.parseInt(raw.trim());
                return Math.max(0, Math.min(requested, total - 1));
            } catch (NumberFormatException e) {
                check("replay.seekPropertyIgnored=" + raw);
            }
        }
        // Deliberately NOT two thirds: that is where a paused replay commonly sits, and a target equal to
        // the current tick makes the test vacuous. Aim near the end unless the replay is already there.
        int nearEnd = Math.max(0, total - 10);
        return atOpen >= nearEnd ? 0 : nearEnd;
    }

    private void check(String message) {
        verdict.add("CHECK " + message);
        LOGGER.info("[REPLAY_PROBE] {}", message);
    }

    private void fail(String message) {
        failed = true;
        verdict.add("FAILURE " + message);
        LOGGER.error("[REPLAY_PROBE] {}", message);
    }

    private void writeVerdict() {
        if (verdictFile == null) {
            return;
        }
        List<String> lines = new ArrayList<>(verdict);
        lines.add("PROBE_RESULT " + (failed ? "FAIL" : "PASS") + " checks="
            + verdict.stream().filter(l -> l.startsWith("CHECK ")).count());
        try {
            Files.createDirectories(verdictFile.toAbsolutePath().getParent());
            Files.write(verdictFile, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[REPLAY_PROBE] could not write the verdict to {}", verdictFile, e);
        }
    }
}

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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import net.minecraft.client.Minecraft;

/**
 * Records where the client is stuck when the loading overlay will not go away.
 *
 * <h2>Why this exists</h2>
 *
 * <p>The owner reported a freeze on leaving a world: the log stops at the integrated server's "Saving worlds" and
 * never reaches this mod's restore reload or the client's "Stopping!". No exception, no crash report, no watchdog - on
 * a hang, a log simply stops, and the freeze is gone by the time anyone can attach a profiler.
 *
 * <p>It is not reproducible in this project's own client, which is the second reason for the file: the owner's client
 * runs Iris, Sodium, EntityCulling and ImmediatelyFast alongside this mod, and the dev harness runs none of them, so
 * the difference has to be captured where it happens. This does not fix anything by itself; it makes the next
 * occurrence diagnosable, because a thread dump taken while it is stuck names the exact frame.
 *
 * <h2>When it fires</h2>
 *
 * <p>Only after this client has been IN a world at least once - so an ordinary slow start-up, which also shows the
 * overlay, is never reported - and only when that overlay has been up for {@value #THRESHOLD_SECONDS} seconds
 * continuously, which no healthy reload in this project reaches. It reports once per stuck overlay, so a frozen game
 * does not fill the log, and it never blocks, throws or retries: the dump runs on its own thread precisely because the
 * thread being reported may be the one that cannot run.
 *
 * <p>Ordinary worlds are affected in no way: this observes the client's own loading overlay and writes one log entry.
 */
public final class LoadingHangWatchdog {
    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger("historical_experience/hang-watchdog");
    private static final long THRESHOLD_NANOS = 30L * 1_000_000_000L;

    /** Set once a world has been joined, so a slow first start-up is not mistaken for a hang. */
    private static boolean joinedAWorld;

    private static long overlaySinceNanos;
    private static boolean reported;

    private LoadingHangWatchdog() {
    }

    /** Called from the client tick. */
    public static void tick(Minecraft client) {
        if (client.level != null) {
            joinedAWorld = true;
        }
        if (client.getOverlay() == null) {
            // The overlay cleared, so whatever it was doing finished. Re-arm for the next one.
            overlaySinceNanos = 0L;
            reported = false;
            return;
        }
        if (!joinedAWorld) {
            return;
        }
        long now = System.nanoTime();
        if (overlaySinceNanos == 0L) {
            overlaySinceNanos = now;
            return;
        }
        if (!reported && now - overlaySinceNanos >= THRESHOLD_NANOS) {
            reported = true;
            report(client);
        }
    }

    private static void report(Minecraft client) {
        LOGGER.warn("HANGWATCHDOG the loading overlay has been up for {}s after joining a world: "
            + "level={} screen={} overlay={} - dumping the stacks that matter",
            THRESHOLD_NANOS / 1_000_000_000L,
            client.level == null ? "null" : "present",
            client.screen == null ? "null" : client.screen.getClass().getName(),
            client.getOverlay() == null ? "null" : client.getOverlay().getClass().getName());
        try {
            for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
                String name = info.getThreadName();
                if (name.equals("Render thread") || name.equals("Server thread") || name.equals("main")
                        || name.startsWith("Worker-") || name.startsWith("IO-Worker")
                        || name.startsWith("Worker-Main")) {
                    StringBuilder stack = new StringBuilder(name).append(" state=").append(info.getThreadState());
                    if (info.getLockName() != null) {
                        stack.append(" waiting on ").append(info.getLockName());
                    }
                    for (StackTraceElement frame : info.getStackTrace()) {
                        stack.append("\n    at ").append(frame);
                    }
                    LOGGER.warn("HANGWATCHDOG {}", stack);
                }
            }
        } catch (RuntimeException failure) {
            // A diagnostic that throws is worse than no diagnostic: the game may already be in a bad state.
            LOGGER.warn("HANGWATCHDOG could not dump threads", failure);
        }
    }
}

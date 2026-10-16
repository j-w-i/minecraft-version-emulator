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
package net.historicalexperience.client.version;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * Opens the version interface and photographs it, for verification rather than for players.
 *
 * <p>Armed only by a system property, like the generation probe on the server side, and it writes a verdict file
 * so a run can be judged without a person watching. It exists because a screenshot cannot be argued with: a
 * clipped list, a button under another button, a details panel running off the bottom of a small window and an era
 * row that does not fit are all invisible in a log and obvious in an image.
 *
 * <p>It is deliberately not a test harness that asserts anything about layout - it produces the evidence, and the
 * judgement happens by looking at what it produced.
 *
 * <p>Usage:
 * <pre>
 *   gradlew runClientQuickPlay -DscreenProbe... (see ScreenProbe.enabled)
 * </pre>
 */
public final class ScreenProbe {
    private static final String PROPERTY = "historical_experience.screenProbe";
    private static final String OUTPUT_PROPERTY = "historical_experience.screenProbe.dir";

    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final List<String> LOG = new ArrayList<>();

    private ScreenProbe() {
    }

    /** Registers the probe. Does nothing at all unless the property is set. */
    public static void register() {
        if (!Boolean.getBoolean(PROPERTY)) {
            System.out.println("[hex] screen probe not armed (" + PROPERTY + " is not true)");
            return;
        }

        System.out.println("[hex] screen probe armed; screenshots will be written under run/client/screenshots");

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            // Any screen, not the title screen specifically: a quick-play run goes straight into a world and the
            // title screen never appears at all, which is exactly how the first version of this probe failed to do
            // anything. The driver then waits for a level rather than assuming where it started.
            if (!STARTED.compareAndSet(false, true)) {
                return;
            }

            Thread driver = new Thread(() -> drive(client, screen), "hex-screen-probe");
            driver.setDaemon(true);
            driver.start();
        });
    }

    private static void drive(Minecraft client, net.minecraft.client.gui.screens.Screen firstScreen) {
        try {
            if (Boolean.getBoolean(PROPERTY + ".layoutOnly")) {
                driveLayouts(client);
                return;
            }
            // Wait for the world, so the photographs are of screens drawn over a loaded game rather than of a
            // loading screen mid-fade. Bounded, so a run that never reaches a world still photographs the menus.
            for (int waited = 0; waited < 60 && client.level == null; waited++) {
                Thread.sleep(1000);
            }

            Thread.sleep(2000);
            note("probe started; in world = " + (client.level != null));
            net.minecraft.client.gui.screens.Screen titleScreen = firstScreen;
            // Beta, at three GUI scales: the era with the most rows, which is where a list is most likely to
            // overflow and where row text is most likely to be squeezed.
            for (int scale : new int[] {1, 2, 3}) {
                photograph(client, titleScreen, "historical_experience:beta_1_7_3", scale, "beta-scale-" + scale);
            }

            // The other eras, to check the hierarchy and that an implemented extra (Alpha v1.1.2_01, which is not
            // one of the planned targets) renders as a normal selectable row rather than as a planned one.
            photograph(client, titleScreen, "historical_experience:planned_alpha_1_0_1", 2, "alpha");
            photograph(client, titleScreen, "historical_experience:release_1_6_4", 2, "release");
            photograph(client, titleScreen, "historical_experience:infdev_420", 2, "infdev");

            // The two screens the selector is reached from, which is where an added button could overlap vanilla's.
            onClientThread(client, () -> client.setScreen(new SelectWorldScreen(titleScreen)));
            Thread.sleep(2500);
            capture(client, "select-world");

            onClientThread(client, () -> {
                try {
                    net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
                        .openFresh(client, titleScreen);
                } catch (Throwable failure) {
                    note("create-world failed to open: " + failure);
                }
            });
            Thread.sleep(4000);
            capture(client, "create-world");
        } catch (Throwable failure) {
            note("probe failed: " + failure);
        } finally {
            writeVerdict(client);
            if (Boolean.getBoolean(PROPERTY + ".layoutOnly")) {
                client.execute(client::stop);
            }
        }
    }

    /** Menu-only regression run: never opens or writes a save. */
    private static void driveLayouts(Minecraft client) throws InterruptedException {
        Thread.sleep(6000);
        int originalScale = client.options.guiScale().get();
        try {
            for (int scale : new int[] {1, 2, 3}) {
                onClientThread(client, () -> {
                    client.options.guiScale().set(scale);
                    client.resizeDisplay();
                    client.setScreen(new SelectWorldScreen(new TitleScreen()));
                });
                Thread.sleep(2200);
                checkLayout(client, "select-world-scale-" + scale);
                capture(client, "layout-select-world-scale-" + scale);

                onClientThread(client, () -> net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
                    .openFresh(client, new TitleScreen()));
                Thread.sleep(4000);
                checkLayout(client, "create-world-scale-" + scale);
                capture(client, "layout-create-world-scale-" + scale);
                for (int tab : new int[] {1, 2}) {
                    onClientThread(client, () -> client.screen.children().stream()
                        .filter(net.minecraft.client.gui.components.tabs.TabNavigationBar.class::isInstance)
                        .map(net.minecraft.client.gui.components.tabs.TabNavigationBar.class::cast)
                        .findFirst().orElseThrow().selectTab(tab, false));
                    Thread.sleep(500);
                    checkLayout(client, "create-world-tab-" + tab + "-scale-" + scale);
                    capture(client, "layout-create-world-tab-" + tab + "-scale-" + scale);
                }
                // Returning from the picker invokes init again; verify no duplicate or misplaced footer rows.
                onClientThread(client, () -> client.setScreen(client.screen));
                Thread.sleep(800);
                checkLayout(client, "create-world-return-scale-" + scale);
            }
            for (int scale : new int[]{1, 2, 3}) {
                photograph(client, new TitleScreen(), "historical_experience:snapshot_11w49a", scale, "snapshot-11w49a-scale-" + scale);
                photograph(client, new TitleScreen(), "historical_experience:snapshot_12w49a", scale, "snapshot-12w49a-scale-" + scale);
            }
        } finally {
            onClientThread(client, () -> client.options.guiScale().set(originalScale));
        }
    }

    private static void checkLayout(Minecraft client, String label) {
        onClientThread(client, () -> {
            var screen = client.screen;
            var widgets = screen.children().stream()
                .filter(net.minecraft.client.gui.components.AbstractWidget.class::isInstance)
                .map(net.minecraft.client.gui.components.AbstractWidget.class::cast)
                .filter(widget -> widget.visible).toList();
            var historical = widgets.stream().filter(widget ->
                widget.getMessage().getString().startsWith("Historical Version")).toList();
            int failures = historical.size() == 1 ? 0 : 1;
            for (var button : historical) {
                if (button.getX() < 0 || button.getY() < 0
                        || button.getRight() > screen.width || button.getBottom() > screen.height) {
                    failures++;
                }
                for (var other : widgets) {
                    if (other != button && button.getX() < other.getRight() && button.getRight() > other.getX()
                            && button.getY() < other.getBottom() && button.getBottom() > other.getY()) {
                        failures++;
                        note("OVERLAP " + label + ": " + other.getClass().getSimpleName());
                    }
                }
            }
            note((failures == 0 ? "PASS " : "FAIL ") + label + " " + screen.width + "x" + screen.height
                + " historical buttons=" + historical.size() + " failures=" + failures);
        });
    }

    private static void photograph(Minecraft client, net.minecraft.client.gui.screens.Screen lastScreen,
            String initialKey, int guiScale, String label) throws InterruptedException {
        onClientThread(client, () -> {
            client.options.guiScale().set(guiScale);
            client.resizeDisplay();

            HistoricalVersionScreen screen = new HistoricalVersionScreen(lastScreen,
                new HistoricalVersionScreen.Context(
                    Component.literal("Historical Version"),
                    Component.literal("Use this version"),
                    initialKey,
                    Entry::selectable,
                    key -> note("confirmed " + key),
                    Component.empty(),
                    null));

            client.setScreen(screen);
        });

        Thread.sleep(1800);
        capture(client, "selector-" + label);
    }

    private static void capture(Minecraft client, String label) throws InterruptedException {
        onClientThread(client, () -> {
            String prefix = System.getProperty(PROPERTY + ".prefix", "");
            if (!prefix.matches("[A-Za-z0-9_-]*")) throw new IllegalArgumentException("Unsafe screenshot prefix");
            Screenshot.grab(client.gameDirectory, "hex-" + prefix + (prefix.isEmpty() ? "" : "-") + label + ".png", client.getMainRenderTarget(),
                message -> note("captured " + label + ": " + message.getString()));
        });

        // The grab is asynchronous; give it time to reach disk before the next screen replaces this one.
        Thread.sleep(1200);
    }

    private static void onClientThread(Minecraft client, Runnable action) {
        client.execute(() -> {
            try {
                action.run();
            } catch (Throwable failure) {
                note("action failed: " + failure);
            }
        });
    }

    private static synchronized void note(String line) {
        LOG.add(line);
    }

    private static void writeVerdict(Minecraft client) {
        try {
            Path output = Path.of(System.getProperty(OUTPUT_PROPERTY,
                new File(client.gameDirectory, "screen-probe.json").getAbsolutePath()));
            Files.createDirectories(output.getParent());

            List<String> screenshots;
            try (var stream = Files.list(new File(client.gameDirectory, "screenshots").toPath())) {
                screenshots = stream.map(path -> path.getFileName().toString()).sorted().toList();
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n  \"versionsInCatalogue\": ")
                .append(HistoricalVersionCatalogue.entries().size())
                .append(",\n  \"plannedTargets\": ")
                .append(HistoricalVersionCatalogue.plannedTargets().size())
                .append(",\n  \"implemented\": ")
                .append(HistoricalVersionCatalogue.implemented().size())
                .append(",\n  \"screenshots\": [");
            for (int index = 0; index < screenshots.size(); index++) {
                json.append(index == 0 ? "" : ", ").append('"').append(screenshots.get(index)).append('"');
            }
            json.append("],\n  \"log\": [");
            for (int index = 0; index < LOG.size(); index++) {
                json.append(index == 0 ? "" : ", ").append('"')
                    .append(LOG.get(index).replace("\\", "/").replace("\"", "'")).append('"');
            }
            json.append("]\n}\n");

            Files.writeString(output, json.toString());
        } catch (Throwable failure) {
            System.err.println("[hex] screen probe could not write its verdict: " + failure);
        }
    }
}

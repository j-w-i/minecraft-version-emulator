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
        }
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
                    Component.empty()));

            client.setScreen(screen);
        });

        Thread.sleep(1800);
        capture(client, "selector-" + label);
    }

    private static void capture(Minecraft client, String label) throws InterruptedException {
        onClientThread(client, () -> {
            File directory = new File(client.gameDirectory, "screenshots");
            Screenshot.grab(directory, "hex-" + label + ".png", client.getMainRenderTarget(),
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

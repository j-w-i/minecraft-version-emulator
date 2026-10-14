package net.historicalexperience.client.version;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.historicalexperience.mixin.WorldListEntryAccessor;
import net.historicalexperience.server.HistoricalUpgradeService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;

/**
 * Changes an existing world's historical version from the world list, without opening it.
 *
 * <h2>Why the world list rather than a command</h2>
 *
 * <p>{@code /hex upgrade} already does this, but it can only be used from inside a world, which is the wrong place
 * to decide what a world is: the player is looking at the list when the question arises, and a command that needs
 * the world loaded cannot answer it for a world they have not opened.
 *
 * <h2>What it actually calls</h2>
 *
 * <p>{@link HistoricalUpgradeService#upgradeUnloadedWorld} - the same service the command calls, with the two
 * checks a caller outside the running world must make. There is no second implementation here: no metadata is
 * written, no backup is made and no generator setting is touched by this class. It resolves which world is
 * selected, asks the service what that world is, and reports what the service did.
 *
 * <h2>What the player is told</h2>
 *
 * <p>A change is staged: the record changes now and the generator is rewritten when the world is next loaded, so
 * the screen says so in the service's own words. Terrain is never regenerated, and a world that is open elsewhere
 * is refused rather than written under a running game that would overwrite it.
 */
public final class ManageWorldHistoricalVersion {
    private static final String MARKER = "Historical Version";
    private static final int BUTTON_WIDTH = 308;
    private static final int ROW_HEIGHT = 24;

    private ManageWorldHistoricalVersion() {
    }

    /** Registers the screen hook. Called from the client initialiser. */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof SelectWorldScreen selectWorldScreen)) {
                return;
            }

            // Reserve another footer row by shrinking the actual scrolling/hit-test area, not by drawing over it.
            // Vanilla rebuilds this list on init/resize, so this adjustment is applied once to each new list.
            WorldSelectionList list = screen.children().stream()
                .filter(WorldSelectionList.class::isInstance)
                .map(WorldSelectionList.class::cast).findFirst().orElseThrow();
            list.updateSizeAndPosition(list.getWidth(), list.getHeight() - ROW_HEIGHT, list.getY());

            Button button = existingButton(screen);
            if (button == null) {
                button = Button.builder(Component.literal(MARKER + "..."),
                    pressed -> open(selectWorldScreen, client)).width(BUTTON_WIDTH).build();
                Screens.getButtons(screen).add(button);
                Button managedButton = button;
                ScreenEvents.beforeRender(screen).register((current, graphics, mouseX, mouseY, delta) ->
                    managedButton.active = selectedEntry(selectWorldScreen).isPresent());
            }
            // Same width, centre and four-pixel row spacing as vanilla's two footer rows.
            button.setPosition((width - BUTTON_WIDTH) / 2, height - 76);
            button.active = selectedEntry(selectWorldScreen).isPresent();
        });
    }

    /** The button this class added, if the screen already has one. */
    private static Button existingButton(net.minecraft.client.gui.screens.Screen screen) {
        for (var widget : Screens.getButtons(screen)) {
            if (widget instanceof Button button && button.getMessage().getString().startsWith(MARKER)) {
                return button;
            }
        }
        return null;
    }

    private static void open(SelectWorldScreen screen, Minecraft client) {
        Optional<WorldSelectionList.WorldListEntry> selected = selectedEntry(screen);

        if (selected.isEmpty()) {
            client.setScreen(new HistoricalVersionResultScreen(screen, false,
                "Select a world first, then choose its historical version.", List.of(
                    "The version belongs to a world, so one has to be selected before it can be changed.")));
            return;
        }

        Path worldDirectory = worldDirectory(selected.get(), client);
        HistoricalUpgradeService.WorldVersionState state = HistoricalUpgradeService.inspect(worldDirectory);

        if (!state.changeable()) {
            client.setScreen(new HistoricalVersionResultScreen(screen, false,
                "This world's version cannot be changed from here.", List.of(
                    "world: " + worldDirectory.getFileName(),
                    state.problem())));
            return;
        }

        // The display name only. The full preset alias is long enough to wrap onto a second line, which pushed the
        // version's own details out of the panel - the note is context, not the subject of the screen.
        String note = "Current version: " + state.displayName();

        client.setScreen(new HistoricalVersionScreen(screen, new HistoricalVersionScreen.Context(
            Component.literal("Change Historical Version"),
            Component.literal("Change to this version"),
            state.profileId(),
            entry -> entry.selectable(),
            key -> change(screen, client, worldDirectory, state, key),
            Component.literal(note))));
    }

    private static void change(SelectWorldScreen screen, Minecraft client, Path worldDirectory,
            HistoricalUpgradeService.WorldVersionState before, String key) {
        HistoricalUpgradeService.Report report = HistoricalUpgradeService.upgradeUnloadedWorld(
            worldDirectory, key, "world list", System.currentTimeMillis());

        client.setScreen(new HistoricalVersionResultScreen(screen, report.ok(),
            report.summary(), report.details()));
    }

    /** The selected row, taken from the screen's own list rather than from a second source of selection state. */
    private static Optional<WorldSelectionList.WorldListEntry> selectedEntry(SelectWorldScreen screen) {
        return screen.children().stream()
            .filter(WorldSelectionList.class::isInstance)
            .map(WorldSelectionList.class::cast)
            .findFirst()
            .flatMap(WorldSelectionList::getSelectedOpt);
    }

    /**
     * The save directory of a world-list row.
     *
     * <p>From {@code LevelSummary.getLevelId()}, which is the folder name. The row's public name accessor returns
     * the display name instead, and two worlds may share one, so a directory resolved from it could name the wrong
     * world - and this code changes world metadata.
     */
    private static Path worldDirectory(WorldSelectionList.WorldListEntry entry, Minecraft client) {
        LevelSummary summary = ((WorldListEntryAccessor) (Object) entry).historicalExperience$summary();
        return client.getLevelSource().getBaseDir().resolve(summary.getLevelId());
    }
}

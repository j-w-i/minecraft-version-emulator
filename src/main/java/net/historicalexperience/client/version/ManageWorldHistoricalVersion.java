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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.historicalexperience.client.config.HistoricalDeveloperMode;
import net.historicalexperience.mixin.client.WorldListEntryAccessor;
import net.historicalexperience.server.HistoricalUpgradeService;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
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
 * <p>A change is staged: the record changes now and the generator is rewritten when the world is next loaded, so the
 * player is told that in one plain sentence. The operation's own account of itself - recipe alias, document revision,
 * backup directory and manifest hash - is shown instead when developer mode is on, and is not shown otherwise.
 * Terrain is never regenerated, and a world that is open elsewhere is refused rather than written under a running
 * game that would overwrite it.
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
            report(screen, client, false,
                "Select a world first, then choose its historical version.", List.of(), null);
            return;
        }

        Path worldDirectory = worldDirectory(selected.get(), client);
        HistoricalUpgradeService.WorldVersionState state = HistoricalUpgradeService.inspect(worldDirectory);

        if (!state.changeable()) {
            report(screen, client, false,
                "This world's version cannot be changed from here.", List.of(state.problem()), null);
            return;
        }

        // The display name only. The full preset alias is long enough to wrap onto a second line, which pushed the
        // version's own details out of the panel - the note is context, not the subject of the screen.
        String note = "Current version: " + state.displayName();

        client.setScreen(new HistoricalVersionScreen(screen, new HistoricalVersionScreen.Context(
            Component.literal("Change Historical Version"),
            Component.literal("Change to this version"),
            state.selectionId(),
            entry -> entry.selectable(),
            key -> change(screen, client, worldDirectory, state, key),
            Component.literal(note),
            // No Indev map settings here: changing an existing world's version is an upgrade of the world it already
            // is, and does not re-choose the map settings it was created with.
            null)));
    }

    private static void change(SelectWorldScreen screen, Minecraft client, Path worldDirectory,
            HistoricalUpgradeService.WorldVersionState before, String key) {
        HistoricalUpgradeService.Report report = HistoricalUpgradeService.upgradeUnloadedWorld(
            worldDirectory, key, "world list", System.currentTimeMillis());

        report(screen, client, report.ok(), report.summary(), report.details(), key);
    }

    /**
     * Shows the outcome of a version change, at the level of detail the player has asked for.
     *
     * <h2>Why this is not simply the report screen</h2>
     *
     * <p>{@link HistoricalVersionResultScreen} prints the operation's own account of itself, line for line, which is
     * the right thing while developing and the wrong thing for a player: it names generation recipe aliases,
     * document revisions, the backup directory and its SHA-256 manifest. Behind
     * {@link HistoricalDeveloperMode} that account is unchanged; without it a player gets one plain sentence and no
     * identifiers.
     *
     * <p>A successful change is still announced rather than performed silently. It does not take effect until the
     * world is next opened - that is inherent, because a running world holds its own copy of the generator and would
     * write it back over a change made underneath it - so a player who saw nothing at all would reasonably conclude
     * that the click had failed.
     *
     * @param chosenKey the version the world was moved to, used to name it to a player. The player-facing sentence is
     *                  built from the catalogue rather than by taking the operation's summary apart: the summary
     *                  names the internal version id, and parsing it would be a second implementation of that
     *                  wording that breaks silently the next time the wording changes
     */
    private static void report(Screen screen, Minecraft client, boolean ok, String summary,
            java.util.List<String> details, String chosenKey) {
        if (HistoricalDeveloperMode.enabled()) {
            client.setScreen(new HistoricalVersionResultScreen(screen, ok, summary, details));
            return;
        }

        String title;
        String message;
        if (!ok) {
            // The service already answers failures in plain English - "This world appears to be open, so its version
            // was not changed." - so there is nothing to reword. Its detail lines, which carry the world path and the
            // recipe alias, are what developer mode is for.
            title = "Version not changed";
            message = summary;
        } else {
            String named = chosenKey == null ? null : HistoricalVersionCatalogue.byKey(chosenKey)
                .map(HistoricalVersionCatalogue.Entry::displayName)
                .orElse(chosenKey);
            title = "Version changed";
            message = named == null
                ? "This world will generate with the new version from the next time it is opened. Existing chunks, "
                    + "builds and inventories are not changed."
                : named + " will take effect when this world is next opened. Existing chunks, builds and "
                    + "inventories are not changed.";
        }
        client.setScreen(new AlertScreen(() -> client.setScreen(screen),
            Component.literal(title), Component.literal(message)));
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

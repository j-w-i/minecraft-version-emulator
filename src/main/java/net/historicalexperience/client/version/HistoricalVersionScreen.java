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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.historicalexperience.version.HistoricalVersionCatalogue.Era;
import net.historicalexperience.version.HistoricalVersionCatalogue.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Browsing and choosing a historical Minecraft version, shared by world creation and world management.
 *
 * <h2>The layout, and why this one</h2>
 *
 * <p>An era row across the top, the versions of that era in a scrolling list, and a details panel that follows the
 * highlight. Three reasons, weighed against the alternatives rather than picked first:
 *
 * <ul>
 *   <li>A two-pane tree (eras left, versions right) spends a third of a small window on a list of four items, and
 *       at the lowest GUI scales it leaves the version names too narrow to read without truncation. The era row
 *       costs one line of height at every resolution.</li>
 *   <li>Expandable groups inside one list make the count of visible versions depend on what is expanded, so a
 *       player looking for Beta 1.8.1 may have to open the era they are already looking at. With one era shown at
 *       a time the list is never longer than Beta's fourteen rows, which fits at 3x GUI scale without scrolling
 *       and scrolls cleanly when it does not.</li>
 *   <li>It matches what this screen sits next to. Vanilla's create-world screen is itself a row of tabs over
 *       content, so an era row reads as the same kind of control rather than a new interaction to learn.</li>
 * </ul>
 *
 * <p>Everything the screen shows comes from {@link HistoricalVersionCatalogue}; the screen decides nothing about
 * which versions exist or what they generate with. What differs between the two uses is passed in as a
 * {@link Context}: the title, the confirm label, which entries may be chosen, and what happens on confirm.
 *
 * <h2>Unimplemented versions</h2>
 *
 * <p>They are listed, because a player should be able to see that Beta 1.3 is planned and not wonder whether it was
 * forgotten. They cannot be selected: the list renders them dimmed, the details panel says what is missing, and
 * the confirm button stays disabled while one is highlighted. Nothing silently substitutes a supported version for
 * an unsupported one.
 */
public class HistoricalVersionScreen extends Screen {
    private static final int ERA_ROW_HEIGHT = 20;
    private static final int DETAILS_HEIGHT = 52;
    private static final int ROW_HEIGHT = 22;

    /** The width of the version column. The list and the details panel share it so they line up. */
    private int columnWidth() {
        return Math.min(320, this.width - 40);
    }

    private int columnLeft() {
        return (this.width - columnWidth()) / 2;
    }

    private final Screen lastScreen;
    private final Context context;
    private final Era initialEra;
    private final String initialKey;

    private Era shownEra;
    private VersionList list;
    private MultiLineTextWidget details;
    private Button confirmButton;

    /**
     * What the two callers of this screen have in common, and the little they differ by.
     *
     * @param title        the screen's title
     * @param confirmLabel what the accept button says, e.g. "Create" or "Change version"
     * @param initialKey   the version to start on, or blank to start on nothing
     * @param permitted    which entries may be chosen. World creation allows any implemented version; world
     *                     management passes the same predicate, so the two cannot drift apart
     * @param onConfirm    what to do with the chosen key
     * @param note         an extra line shown above the version details, for context a caller needs to state -
     *                     the world's current version, or a warning about a staged change
     */
    public record Context(Component title, Component confirmLabel, String initialKey,
            Predicate<Entry> permitted, Consumer<String> onConfirm, Component note) {
        public Context {
            if (permitted == null) {
                throw new IllegalArgumentException("a selector must state which versions may be chosen");
            }
            if (onConfirm == null) {
                throw new IllegalArgumentException("a selector must say what confirmation does");
            }
            if (initialKey == null) {
                initialKey = "";
            }
            if (note == null) {
                note = Component.empty();
            }
        }
    }

    public HistoricalVersionScreen(Screen lastScreen, Context context) {
        super(context.title());
        this.lastScreen = lastScreen;
        this.context = context;
        this.initialKey = context.initialKey();
        this.initialEra = HistoricalVersionCatalogue.byKey(this.initialKey)
            .map(Entry::era)
            .orElse(Era.BETA);
        this.shownEra = this.initialEra;
    }

    @Override
    protected void init() {
        // Reached again on every resize, so all geometry is derived from width and height here and nothing is
        // cached from a previous size.
        int eraCount = HistoricalVersionCatalogue.eras().size();
        int eraWidth = Math.min(120, (this.width - 20) / eraCount - 4);
        int eraRowWidth = eraCount * (eraWidth + 4) - 4;
        int eraX = (this.width - eraRowWidth) / 2;

        int index = 0;
        for (Era era : HistoricalVersionCatalogue.eras()) {
            final Era target = era;
            boolean current = era == this.shownEra;

            // The current era is marked rather than disabled. Disabling it made the row read as four greyed-out
            // buttons, which is what "broken" looked like: a tab that is selected should look selected, not
            // unavailable. Every era stays clickable, and the marker says which one is showing.
            Button button = Button.builder(
                    Component.literal(current ? "» " + era.displayName() : era.displayName()),
                    pressed -> showEra(target))
                .pos(eraX + index * (eraWidth + 4), 30)
                .width(eraWidth)
                .build();

            addRenderableWidget(button);
            index++;
        }

        // A gap between the list and the details, because the list clips its last row at its own edge and a clipped
        // row sitting directly on top of the text reads as a rendering fault rather than as a scrolled list.
        int listTop = 30 + ERA_ROW_HEIGHT + 8;
        int detailsTop = this.height - DETAILS_HEIGHT - 30;
        this.list = new VersionList(this.minecraft, columnWidth(),
            Math.max(ROW_HEIGHT * 3, detailsTop - listTop - 14), listTop, ROW_HEIGHT);
        addRenderableWidget(this.list);
        this.list.populate(this.shownEra);
        this.list.selectKey(this.initialKey);

        // Aligned with the version column rather than to the window edge, so the panel reads as belonging to the
        // list instead of floating in the corner.
        this.details = new MultiLineTextWidget(columnLeft(), detailsTop, Component.empty(), this.font)
            .setMaxWidth(columnWidth())
            .setMaxRows(5);
        addRenderableOnly(this.details);

        this.confirmButton = addRenderableWidget(Button.builder(this.context.confirmLabel(), pressed -> confirm())
            .pos(this.width / 2 - 154, this.height - 26)
            .width(150)
            .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), pressed -> onClose())
            .pos(this.width / 2 + 4, this.height - 26)
            .width(150)
            .build());

        updateDetails();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen.render draws the background and the widgets but NOT the title: vanilla screens draw their own, so
        // without this the screen has no heading at all.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }

    private void showEra(Era era) {
        this.shownEra = era;
        // Rebuilding is the simplest correct answer to a changed era: the era buttons' labels and the list's
        // contents both change, and going through init() keeps one layout code path rather than two.
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void confirm() {
        VersionList.VersionRow row = this.list.getSelected();
        if (row == null || !row.entry.selectable() || !this.context.permitted().test(row.entry)) {
            return;
        }

        this.context.onConfirm().accept(row.entry.key());

        // Return to whatever opened this screen, but only if the action did not already replace it. World
        // management opens a result screen from its confirm action, and that must not be overwritten here; world
        // creation only records the choice, so without this the screen stayed open and the button appeared to do
        // nothing.
        if (this.minecraft.screen == this) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    private void updateDetails() {
        VersionList.VersionRow row = this.list == null ? null : this.list.getSelected();
        Entry selected = row == null ? null : row.entry;
        this.details.setMessage(describe(selected));
        if (this.confirmButton != null) {
            this.confirmButton.active = selected != null
                && selected.selectable()
                && this.context.permitted().test(selected);
        }
    }

    /**
     * The details line, kept to three short lines on purpose.
     *
     * <p>The first version printed the catalogue's full summary, which wrapped mid-phrase - "revision" on one line
     * and "10" on the next - and pushed the panel over the list. A screen has room for a sentence, not for a
     * report.
     */
    private Component describe(Entry selected) {
        StringBuilder text = new StringBuilder();

        if (!this.context.note().getString().isEmpty()) {
            text.append(this.context.note().getString()).append('\n');
        }

        if (selected == null) {
            text.append("Select a version. Use the era buttons to browse Alpha, Beta and Release.");
            return Component.literal(text.toString());
        }

        text.append(selected.displayName()).append(" - ").append(selected.era().displayName());

        if (selected.status() == Status.FUTURE) {
            text.append('\n').append("Planned. ").append(selected.summary());
            return Component.literal(text.toString());
        }

        text.append('\n').append("Implemented.");
        if (!selected.emulated().isEmpty()) {
            text.append(" Emulates ").append(selected.emulated()).append('.');
        }

        var revision = selected.version().latestRevision();
        text.append('\n').append("Generates with ").append(revision.alias())
            .append(" (revision ").append(revision.revision()).append(')');

        return Component.literal(text.toString());
    }

    /** The scrolling list of one era's versions. */
    private final class VersionList extends ObjectSelectionList<VersionList.VersionRow> {
        private VersionList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }

        private void populate(Era era) {
            clearEntries();
            for (HistoricalVersionCatalogue.Entry entry : HistoricalVersionCatalogue.entriesIn(era)) {
                addEntry(new VersionRow(entry));
            }
        }

        private void selectKey(String key) {
            for (VersionRow row : children()) {
                if (row.entry.key().equals(key)) {
                    setSelected(row);
                    return;
                }
            }
            // Nothing chosen yet: highlight nothing rather than silently choosing the first version, so a player
            // cannot create a world with a version they never picked.
        }

        @Override
        public int getRowWidth() {
            return Math.min(320, this.width - 40);
        }

        /** One version. */
        private final class VersionRow extends ObjectSelectionList.Entry<VersionRow> {
            // Qualified because the inherited nested type ObjectSelectionList.Entry shadows any import of a type
            // also called Entry, and it shadows it *inside this class only* - which is exactly where the two got
            // confused the first time.
            private final HistoricalVersionCatalogue.Entry entry;

            private VersionRow(HistoricalVersionCatalogue.Entry entry) {
                this.entry = entry;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                    int mouseX, int mouseY, boolean hovering, float partialTick) {
                boolean selectable = this.entry.selectable();
                int colour = selectable ? 0xFFFFFFFF : 0xFF808080;

                // GuiGraphics.drawString is the 1.21.1 text path; Font has no drawShadow(GuiGraphics, ...) overload
                // in this version, which is what the first attempt assumed.
                graphics.drawString(HistoricalVersionScreen.this.font, this.entry.displayName(), x + 4, y + 3,
                    colour, true);

                String state = selectable ? "implemented" : "planned";
                int stateWidth = HistoricalVersionScreen.this.font.width(state);
                graphics.drawString(HistoricalVersionScreen.this.font, state, x + entryWidth - stateWidth - 4,
                    y + 3, selectable ? 0xFF55FF55 : 0xFFAAAAAA, true);

                if (!this.entry.emulated().isEmpty()) {
                    graphics.drawString(HistoricalVersionScreen.this.font,
                        "emulates " + this.entry.emulated(), x + 4, y + 12, 0xFFAAAAAA, true);
                }
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.entry.displayName() + ", "
                    + (this.entry.selectable() ? "implemented" : "planned, not selectable"));
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Selecting an unimplemented version is allowed so its details can be read; confirming is what is
                // refused. Returning false here would make the row feel broken instead of informative.
                VersionList.this.setSelected(this);
                updateDetails();
                return true;
            }
        }
    }
}

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
import net.historicalexperience.version.VersionOverview;
import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.historicalexperience.version.HistoricalVersionCatalogue.Era;
import net.historicalexperience.version.HistoricalVersionCatalogue.Status;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

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

    /** Gap between the version list and the overview panel beside it. */
    private static final int PANEL_GAP = 8;

    /**
     * How the window's width is shared between the version list and the overview panel.
     *
     * <p>On a wide window the list keeps its full 320 and the panel takes the rest, up to 300. When the window is
     * narrow - a high GUI scale - the list gives up width first, down to 170, because a version name still reads at
     * that width while a paragraph of text beside it does not read at less than about 150.
     */
    private int usableWidth() {
        return this.width - 20;
    }

    /** The width of the version column. The list and the details line under it share it so they line up. */
    private int columnWidth() {
        int usable = usableWidth();
        if (usable >= 320 + PANEL_GAP + 220) {
            return 320;
        }
        return Math.max(170, (usable - PANEL_GAP) * 45 / 100);
    }

    private int panelWidth() {
        return Math.max(120, Math.min(300, usableWidth() - columnWidth() - PANEL_GAP));
    }

    /** Left edge of the whole list-plus-panel block, centred as one unit. */
    private int blockLeft() {
        return (this.width - (columnWidth() + PANEL_GAP + panelWidth())) / 2;
    }

    private int columnLeft() {
        return blockLeft();
    }

    private int panelLeft() {
        return blockLeft() + columnWidth() + PANEL_GAP;
    }

    private final Screen lastScreen;
    private final Context context;
    private final Era initialEra;
    private final String initialKey;

    private Era shownEra;
    private String highlightedKey = "";
    private VersionList list;
    private MultiLineTextWidget details;
    private OverviewPanel overview;
    private Button confirmButton;
    private Button eraOptionsButton;

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
     * @param eraOptions   an extra action offered only while a version of a particular era is highlighted, or null.
     *                     World creation passes the original Indev map settings here; world management passes null,
     *                     because changing an existing world's version does not re-choose its map settings
     */
    public record Context(Component title, Component confirmLabel, String initialKey,
            Predicate<Entry> permitted, Consumer<String> onConfirm, Component note, Runnable eraOptions) {
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
        var eras = HistoricalVersionCatalogue.eras();
        int eraCount = eras.size();
        boolean compact = this.width < 480;
        int[] widths = new int[eraCount];
        int minimum = eras.stream().mapToInt(era -> this.font.width(era.displayName()) + 10).sum() + (eraCount - 1) * 4;
        int extra = Math.max(0, (this.width - 20 - minimum) / eraCount);
        for (int i = 0; i < eraCount; i++) widths[i] = compact
            ? this.font.width(eras.get(i).displayName()) + 10 + extra
            : Math.min(120, (this.width - 20) / eraCount - 4);
        int eraRowWidth = java.util.Arrays.stream(widths).sum() + (eraCount - 1) * 4;
        int eraX = (this.width - eraRowWidth) / 2;

        int index = 0;
        for (Era era : eras) {
            final Era target = era;
            boolean current = era == this.shownEra;

            // The current era is marked rather than disabled. Disabling it made the row read as four greyed-out
            // buttons, which is what "broken" looked like: a tab that is selected should look selected, not
            // unavailable. Compact windows use a colour marker and content-sized buttons so "Snapshots" fits.
            var label = Component.literal(current && !compact ? "» " + era.displayName() : era.displayName());
            if (current && compact) label.withStyle(net.minecraft.ChatFormatting.YELLOW);
            Button button = Button.builder(label, pressed -> showEra(target))
                .pos(eraX, 30)
                .width(widths[index])
                .build();

            addRenderableWidget(button);
            eraX += widths[index++] + 4;
        }

        // A gap between the list and the details, because the list clips its last row at its own edge and a clipped
        // row sitting directly on top of the text reads as a rendering fault rather than as a scrolled list.
        int listTop = 30 + ERA_ROW_HEIGHT + 8;
        int detailsTop = this.height - DETAILS_HEIGHT - 30;
        int listHeight = Math.max(ROW_HEIGHT * 3, detailsTop - listTop - 14);
        this.list = new VersionList(this.minecraft, columnWidth(), listHeight, listTop, ROW_HEIGHT);
        this.list.setX(columnLeft());
        addRenderableWidget(this.list);

        // The overview spans from the top of the list to the bottom of the details line, so it is as tall as the two
        // together and long entries have room before they need to scroll.
        this.overview = new OverviewPanel(panelLeft(), listTop, panelWidth(),
            detailsTop + DETAILS_HEIGHT - listTop);
        addRenderableWidget(this.overview);
        this.list.populate(this.shownEra);
        // The row the player was on wins over the key this screen was opened with: init() runs again when a screen
        // opened from here (the Indev map settings) returns, and re-selecting from the opening key would silently
        // clear the highlight - which is what disables the confirm button.
        this.list.selectKey(this.highlightedKey.isEmpty() ? this.initialKey : this.highlightedKey);

        // Aligned with the version column rather than to the window edge, so the panel reads as belonging to the
        // list instead of floating in the corner.
        this.details = new MultiLineTextWidget(columnLeft(), detailsTop, Component.empty(), this.font)
            .setMaxWidth(columnWidth())
            .setMaxRows(5);
        addRenderableOnly(this.details);

        this.confirmButton = addRenderableWidget(Button.builder(this.context.confirmLabel(), pressed -> confirm())
            .pos(this.footerButtonX(0), this.height - 26)
            .width(this.footerButtonWidth(0))
            .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), pressed -> onClose())
            .pos(this.footerButtonX(1), this.height - 26)
            .width(this.footerButtonWidth(1))
            .build());
        if (this.context.eraOptions() != null) {
            this.eraOptionsButton = addRenderableWidget(Button.builder(this.eraOptionsLabel(), pressed -> {
                if (this.context.eraOptions() != null) {
                    this.context.eraOptions().run();
                }
            }).pos(this.footerButtonX(2), this.height - 26).width(this.footerButtonWidth(2)).build());
        }

        updateDetails();
    }

    /**
     * The footer's button geometry: two buttons as before, or three when the caller offered an era-specific action.
     *
     * <p>Index 0 is the confirm button, 1 is Cancel and 2 is the era action, laid out in that order left to right and
     * centred as a row. Widths shrink so three buttons still fit the narrowest supported window.
     */
    private int footerButtonWidth(int index) {
        int count = this.context.eraOptions() == null ? 2 : 3;
        int gap = 4;
        return Math.min(150, (this.width - 32 - (count - 1) * gap) / count);
    }

    private int footerButtonX(int index) {
        int count = this.context.eraOptions() == null ? 2 : 3;
        int gap = 4;
        int width = footerButtonWidth(0);
        int total = count * width + (count - 1) * gap;
        return (this.width - total) / 2 + index * (width + gap);
    }

    private Component eraOptionsLabel() {
        return Component.literal("Indev settings...");
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
        this.highlightedKey = selected == null ? "" : selected.key();
        this.details.setMessage(describe(selected));
        if (this.overview != null) {
            this.overview.show(selected);
        }
        if (this.confirmButton != null) {
            this.confirmButton.active = selected != null
                && selected.selectable()
                && this.context.permitted().test(selected);
        }
        if (this.eraOptionsButton != null) {
            // Offered only for the era whose extra options they are, and only for a version that can be chosen, so
            // the button cannot appear against Beta or an ordinary world type.
            this.eraOptionsButton.active = selected != null
                && selected.selectable()
                && net.historicalexperience.version.HistoricalVersions.isFiniteWorld(selected.version().id());
            this.eraOptionsButton.setMessage(this.eraOptionsLabel());
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
        // The words themselves live in VersionText, where a test can call them: whether the recipe alias and the
        // measured build are shown is a decision, and the screen is not testable. Developer mode is read here, once
        // per redraw, so switching it in Mod Menu takes effect on the next version highlighted.
        return Component.literal(VersionText.details(selected, this.context.note().getString(),
            net.historicalexperience.client.config.HistoricalDeveloperMode.enabled()));
    }

    /**
     * What the highlighted version added, changed, removed and did to world generation, beside the list.
     *
     * <p>A widget of its own rather than more text under the list: the entries run to a dozen lines for the larger
     * releases, so they have to scroll, and a scrolling region needs its own clip, wheel handling and scrollbar. The
     * content comes from {@link VersionOverview}; nothing here decides what a version contains.
     */
    private final class OverviewPanel extends AbstractWidget {
        private static final int PAD = 6;
        private static final int SCROLLBAR = 4;

        /** One rendered line; a null text is vertical space, and {@code dot} marks the first line of a bullet. */
        private record Line(FormattedCharSequence text, int indent, int colour, boolean dot) {
        }

        private static Line space() {
            return new Line(null, 0, 0, false);
        }

        private List<Line> lines = List.of();
        private String shownKey = "";
        private double scroll;
        private boolean draggingBar;

        private OverviewPanel(int x, int y, int width, int height) {
            super(x, y, width, height, Component.literal("Version overview"));
        }

        private int step() {
            return HistoricalVersionScreen.this.font.lineHeight + 2;
        }

        private int contentHeight() {
            return this.lines.size() * step() + 2 * PAD;
        }

        private int maxScroll() {
            return Math.max(0, contentHeight() - this.height);
        }

        private void show(Entry selected) {
            String key = selected == null ? "" : selected.key();
            if (!key.equals(this.shownKey)) {
                this.scroll = 0;
                this.shownKey = key;
            }
            this.lines = build(selected);
            this.scroll = Math.min(this.scroll, maxScroll());
        }

        private List<Line> build(Entry selected) {
            List<Line> out = new java.util.ArrayList<>();
            int wrap = this.width - 2 * PAD - SCROLLBAR - 2;

            if (selected == null) {
                paragraph(out, "Select a version to see what it added.", 0, 0xFFAAAAAA, wrap);
                return out;
            }
            paragraph(out, selected.displayName(), 0, 0xFFFFFFFF, wrap);
            if (!selected.selectable()) {
                out.add(space());
                paragraph(out, "Planned. " + selected.summary(), 0, 0xFFAAAAAA, wrap);
                return out;
            }
            if (selected.snapshotPatch().isPresent()) {
                paragraph(out, VersionText.details(selected, "", false), 0, 0xFFAAAAAA, wrap);
                out.add(space());
                paragraph(out, "The following overview describes the parent baseline:", 0, 0xFFAAAAAA, wrap);
            }
            var overview = VersionOverview.forVersion(selected.version().id());
            if (overview.isEmpty()) {
                out.add(space());
                paragraph(out, "No overview is recorded for this build.", 0, 0xFFAAAAAA, wrap);
                return out;
            }
            section(out, "Added", 0xFF55FF55, overview.get().introduced(), wrap);
            section(out, "Changed", 0xFFFFFF55, overview.get().changed(), wrap);
            section(out, "Removed", 0xFFFF5555, overview.get().removed(), wrap);
            section(out, "World generation", 0xFF55FFFF, overview.get().worldgen(), wrap);
            out.add(space());
            // Said on screen because the research behind these lists says so itself: a missing item is not evidence.
            paragraph(out, "Not exhaustive: an item missing here does not mean it was absent.", 0, 0xFF808080, wrap);
            return out;
        }

        private void section(List<Line> out, String title, int colour, List<String> items, int wrap) {
            if (items.isEmpty()) {
                return;
            }
            out.add(space());
            paragraph(out, title, 0, colour, wrap);
            for (String item : items) {
                bullet(out, item, wrap);
            }
        }

        private void paragraph(List<Line> out, String text, int indent, int colour, int wrap) {
            for (FormattedCharSequence part : HistoricalVersionScreen.this.font.split(FormattedText.of(text),
                    wrap - indent)) {
                out.add(new Line(part, indent, colour, false));
            }
        }

        /** A bullet whose wrapped lines hang under its first word rather than under the dot. */
        private void bullet(List<Line> out, String text, int wrap) {
            int hang = HistoricalVersionScreen.this.font.width("\u2022 ");
            boolean first = true;
            for (FormattedCharSequence part : HistoricalVersionScreen.this.font.split(FormattedText.of(text),
                    wrap - hang)) {
                out.add(new Line(part, hang, 0xFFE0E0E0, first));
                first = false;
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xB0000000);
            graphics.renderOutline(getX(), getY(), this.width, this.height, 0xFF555555);

            graphics.enableScissor(getX() + 1, getY() + 1, getX() + this.width - 1, getY() + this.height - 1);
            int y = getY() + PAD - (int) this.scroll;
            int step = step();
            for (Line line : this.lines) {
                if (line.text() != null) {
                    graphics.drawString(HistoricalVersionScreen.this.font, line.text(),
                        getX() + PAD + line.indent(), y, line.colour(), false);
                    if (line.dot()) {
                        graphics.drawString(HistoricalVersionScreen.this.font, "\u2022", getX() + PAD, y,
                            line.colour(), false);
                    }
                }
                y += step;
            }
            graphics.disableScissor();

            if (maxScroll() > 0) {
                int trackX = getX() + this.width - SCROLLBAR - 2;
                int trackTop = getY() + 2;
                int trackHeight = this.height - 4;
                graphics.fill(trackX, trackTop, trackX + SCROLLBAR, trackTop + trackHeight, 0xFF202020);
                int thumb = Math.max(16, trackHeight * this.height / contentHeight());
                int thumbY = trackTop + (int) ((trackHeight - thumb) * (this.scroll / maxScroll()));
                graphics.fill(trackX, thumbY, trackX + SCROLLBAR, thumbY + thumb, 0xFFA0A0A0);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!isMouseOver(mouseX, mouseY) || maxScroll() == 0) {
                return false;
            }
            this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - scrollY * step() * 2));
            return true;
        }

        private boolean overBar(double mouseX, double mouseY) {
            return maxScroll() > 0 && mouseX >= getX() + this.width - SCROLLBAR - 6 && mouseX <= getX() + this.width
                && mouseY >= getY() && mouseY <= getY() + this.height;
        }

        private void dragTo(double mouseY) {
            int trackTop = getY() + 2;
            int trackHeight = this.height - 4;
            double fraction = (mouseY - trackTop) / Math.max(1, trackHeight);
            this.scroll = Math.max(0, Math.min(maxScroll(), fraction * maxScroll()));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && overBar(mouseX, mouseY)) {
                this.draggingBar = true;
                dragTo(mouseY);
                return true;
            }
            // Clicking the text does nothing, and must not play the button click or take focus from the list.
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (this.draggingBar) {
                dragTo(mouseY);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            boolean was = this.draggingBar;
            this.draggingBar = false;
            return was;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal("Version overview"));
        }
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
            return columnWidth() - 12;
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

                String note = VersionText.rowNote(this.entry,
                    net.historicalexperience.client.config.HistoricalDeveloperMode.enabled());
                if (!note.isEmpty()) {
                    graphics.drawString(HistoricalVersionScreen.this.font, note, x + 4, y + 12, 0xFFAAAAAA, true);
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

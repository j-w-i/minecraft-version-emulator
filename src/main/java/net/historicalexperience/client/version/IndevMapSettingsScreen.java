/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.client.version;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.historicalexperience.client.indev.IndevMapApplication;
import net.historicalexperience.client.indev.IndevMapApplication;
import net.historicalexperience.worldgen.IndevMapSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * The original Indev map settings, as a screen: level type, theme, shape and size.
 *
 * <h2>Why four rows of buttons rather than a list of ready-made worlds</h2>
 *
 * <p>The original game asked these four questions on its level-generation screen, and each group has three or four
 * answers. Four rows of named buttons reproduce that directly, and - unlike 144 pre-built world entries - it keeps
 * one Indev version in the selector and does not pretend the combinations are different versions. The values, the
 * dimensions and the one type upstream cannot generate come from {@link IndevMapSettings}; this screen decides
 * nothing about what exists.
 *
 * <p>It is reached only from a world creation whose generator is actually Indev's, so an ordinary world, Infdev or
 * any later era never sees it.
 */
public final class IndevMapSettingsScreen extends Screen {
    private static final int LABEL_WIDTH = 96;
    private static final int ROW_HEIGHT = 24;
    private static final int FIRST_ROW = 44;

    private final Screen lastScreen;
    private final CreateWorldScreen createWorldScreen;

    private IndevMapSettings.LevelType type;
    private IndevMapSettings.Theme theme;
    private IndevMapSettings.Shape shape;
    private IndevMapSettings.Size size;

    /** Where each group's row was drawn, so the labels can be drawn beside the buttons laid out in init(). */
    private final List<String> rowLabels = new ArrayList<>();
    private final List<Integer> rowY = new ArrayList<>();
    private String status = "";

    public IndevMapSettingsScreen(Screen lastScreen, CreateWorldScreen createWorldScreen) {
        super(Component.literal("Indev map settings"));
        this.lastScreen = lastScreen;
        this.createWorldScreen = createWorldScreen;
        var current = IndevMapSelection.choice();
        this.type = current.type();
        this.theme = current.theme();
        this.shape = current.shape();
        this.size = current.size();
    }

    private IndevMapSettings.Choice choice() {
        return new IndevMapSettings.Choice(this.type, this.theme, this.shape, this.size);
    }

    @Override
    protected void init() {
        this.rowLabels.clear();
        this.rowY.clear();

        int left = 16;
        int right = this.width - 16;
        int y = FIRST_ROW;
        y = row(y, "Level type", List.of(IndevMapSettings.LevelType.values()), this.type,
            value -> this.type = value, left, right);
        y = row(y, "Theme", List.of(IndevMapSettings.Theme.values()), this.theme,
            value -> this.theme = value, left, right);
        y = row(y, "Shape", List.of(IndevMapSettings.Shape.values()), this.shape,
            value -> this.shape = value, left, right);
        row(y, "Size", List.of(IndevMapSettings.Size.values()), this.size, value -> this.size = value, left, right);

        // Apply and Cancel, adapting to a third button-free row: this screen's own buttons only, so it never has to
        // line up with anything in the window behind it.
        int buttonWidth = Math.min(150, (this.width - 32 - 4) / 2);
        int total = buttonWidth * 2 + 4;
        int x = (this.width - total) / 2;
        int buttonY = this.height - 26;
        addRenderableWidget(Button.builder(Component.literal("Use these settings"), pressed -> apply())
            .pos(x, buttonY).width(buttonWidth).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), pressed -> onClose())
            .pos(x + buttonWidth + 4, buttonY).width(buttonWidth).build());
    }

    /** Lays out one group's buttons and records its label position. Returns the next row's y. */
    private <T> int row(int y, String label, List<T> values, T selected, Consumer<T> choose, int left, int right) {
        this.rowLabels.add(label);
        this.rowY.add(y);
        int available = right - (left + LABEL_WIDTH);
        int gap = 4;
        int width = Math.max(48, Math.min(96, (available - (values.size() - 1) * gap) / values.size()));
        int x = left + LABEL_WIDTH;
        for (T value : values) {
            boolean blocked = value instanceof IndevMapSettings.LevelType levelType
                && !IndevMapSettings.available(levelType).isEmpty();
            boolean isSelected = value.equals(selected);
            String text = (isSelected ? "\u00bb " : "") + name(value);
            Button button = Button.builder(Component.literal(text), pressed -> {
                if (blocked) {
                    return;
                }
                choose.accept(value);
                this.status = "";
                rebuildWidgets();
            }).pos(x, y).width(width).build();
            button.active = !blocked;
            addRenderableWidget(button);
            x += width + gap;
        }
        return y + ROW_HEIGHT;
    }

    private static String name(Object value) {
        if (value instanceof IndevMapSettings.LevelType levelType) return levelType.displayName();
        if (value instanceof IndevMapSettings.Theme theme) return theme.displayName();
        if (value instanceof IndevMapSettings.Shape shape) return shape.displayName();
        return ((IndevMapSettings.Size) value).displayName();
    }

    private void apply() {
        IndevMapSettings.Choice wanted = choice();
        IndevMapSelection.choose(wanted);
        boolean indevWorld = IndevMapApplication.offered(this.createWorldScreen);
        if (IndevMapApplication.apply(this.createWorldScreen, wanted) || !indevWorld) {
            // Either it is applied now, or the world type is not Indev's yet - in which case the choice is stored and
            // is applied the moment an Indev version is confirmed. Both are success from the player's side.
            this.minecraft.setScreen(this.lastScreen);
            return;
        }
        // An Indev world that refused the settings: say so instead of closing as if it had worked.
        this.status = "That choice could not be applied to this world's generator.";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        for (int i = 0; i < this.rowLabels.size(); i++) {
            graphics.drawString(this.font, this.rowLabels.get(i), 16, this.rowY.get(i) + 6, 0xFFA0A0A0, false);
        }
        int y = this.height - 26 - 12 * (noteLines().size() + 1);
        for (String line : noteLines()) {
            graphics.drawString(this.font, line, 16, y, 0xFFAAAAAA, false);
            y += 12;
        }
        if (!this.status.isEmpty()) {
            graphics.drawString(this.font, this.status, 16, y, 0xFFFF5555, false);
        }
    }

    /** What will be generated, what the world holds now, and the one type that cannot be chosen. */
    private List<String> noteLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Will generate: " + choice().describe());
        String current = IndevMapApplication.currentDescription(this.createWorldScreen);
        if (!current.isEmpty()) {
            lines.add("This world's generator holds: " + current + " (the pinned upstream preset)");
        }
        lines.add(IndevMapSettings.FLAT_NOTE);
        lines.add("The bundled Indev-to-infinite migration is verified only for the default 256x256 map, so a "
            + "customized Indev world cannot use it; the world itself is unaffected.");
        return lines;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}

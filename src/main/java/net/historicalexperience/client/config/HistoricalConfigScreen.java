package net.historicalexperience.client.config;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.client.lighting.HistoricalLightingEngine;
import net.historicalexperience.client.lighting.HistoricalSmoothLighting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The mod's options screen, reachable from Mod Menu's config button.
 *
 * <h2>The two options, and why there are exactly two</h2>
 *
 * <p>Both are lighting engines, and they cover <b>disjoint</b> spans of history:
 *
 * <ul>
 *   <li><b>Classic lighting</b> - before Beta 1.3. There was no smooth lighting yet: block light was a constant 13
 *       and shade was binary.</li>
 *   <li><b>Old smooth lighting</b> - Beta 1.3 to Beta 1.7.3. The MrMessiah engine, which Beta 1.8 replaced. In this
 *       window the difference is the corner shading: the modern engine darkens a corner towards 0.2 and this one did
 *       not darken it at all.</li>
 * </ul>
 *
 * <p>So in any given world at most one is live, and the other says why it is not. Every other historical behaviour
 * this project implements is a fact about the build rather than a preference, which is why nothing else appears here.
 *
 * <h2>Why an unavailable option is greyed with a reason rather than hidden</h2>
 *
 * <p>A silently absent option is indistinguishable from a broken one, and this project has already been misled once
 * by exactly that shape - a mixin that failed silently and lit nothing. So both buttons are always present; the one
 * that does not apply is inactive and its own text says which window it belongs to.
 *
 * <h2>Why the widget references are kept</h2>
 *
 * <p>Each button is held in a field and its label and active state are set from that reference. The first version of
 * this screen re-derived which button was which by string-matching its own labels, which is fragile in exactly the
 * way that breaks when a translation changes - and it needed a sentinel character to do it. Holding the widgets is
 * simpler and cannot drift from what is on screen.
 */
public final class HistoricalConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_WIDTH = 300;

    private final Screen parent;

    private Button classicButton;
    private Button smoothButton;

    public HistoricalConfigScreen(Screen parent) {
        super(Component.translatable("historical_experience.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centre = this.width / 2;
        int y = this.height / 4;

        this.classicButton = this.addRenderableWidget(
            Button.builder(classicLabel(), button -> {
                HistoricalLightingEngine.setToggle(!HistoricalLightingEngine.toggle());
                refresh();
            })
                .bounds(centre - ROW_WIDTH / 2, y, ROW_WIDTH, ROW_HEIGHT)
                .build());

        this.smoothButton = this.addRenderableWidget(
            Button.builder(smoothLabel(), button -> {
                HistoricalSmoothLighting.setToggle(!HistoricalSmoothLighting.toggle());
                refresh();
            })
                .bounds(centre - ROW_WIDTH / 2, y + ROW_HEIGHT + 4, ROW_WIDTH, ROW_HEIGHT)
                .build());

        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centre - ROW_WIDTH / 2, this.height - 40, ROW_WIDTH, ROW_HEIGHT)
                .build());

        refresh();
    }

    /** Re-reads both engines and brings the two buttons in line with them. */
    private void refresh() {
        if (this.classicButton != null) {
            this.classicButton.setMessage(classicLabel());
            this.classicButton.active = HistoricalLightingEngine.availableFor(activeVersion());
        }
        if (this.smoothButton != null) {
            this.smoothButton.setMessage(smoothLabel());
            this.smoothButton.active = HistoricalSmoothLighting.availableFor(activeVersion());
        }
    }

    private static net.historicalexperience.version.HistoricalVersion activeVersion() {
        return HistoricalClientRules.activeVersion();
    }

    /** The classic engine's button text. */
    private static Component classicLabel() {
        if (activeVersion() == null) {
            return Component.translatable("historical_experience.config.lighting.no_world");
        }
        if (!HistoricalLightingEngine.availableFor(activeVersion())) {
            return Component.translatable("historical_experience.config.lighting.not_applicable");
        }
        return Component.translatable(HistoricalLightingEngine.toggle()
            ? "historical_experience.config.lighting.on"
            : "historical_experience.config.lighting.off");
    }

    /** The old smooth lighting engine's button text, which applies from Beta 1.3 to Beta 1.7.3. */
    private static Component smoothLabel() {
        if (activeVersion() == null) {
            return Component.translatable("historical_experience.config.smooth.no_world");
        }
        if (!HistoricalSmoothLighting.availableFor(activeVersion())) {
            return Component.translatable("historical_experience.config.smooth.not_applicable");
        }
        return Component.translatable(HistoricalSmoothLighting.toggle()
            ? "historical_experience.config.smooth.on"
            : "historical_experience.config.smooth.off");
    }

    /** Puts the reasoning on the screen, so a player does not have to consult a changelog to understand two toggles. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 34, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
            Component.translatable("historical_experience.config.note"),
            this.width / 2, this.height / 4 + (ROW_HEIGHT + 4) * 2 + 12, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        // Back to whoever opened this - Mod Menu's list, or another screen. Never a hard-coded parent, because
        // this screen is reachable from more than one place.
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}

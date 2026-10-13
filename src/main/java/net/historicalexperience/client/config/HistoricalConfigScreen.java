package net.historicalexperience.client.config;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.client.lighting.HistoricalLightingEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The mod's options screen, reachable from Mod Menu's config button.
 *
 * <h2>What it offers, and why only that</h2>
 *
 * <p>One option: the classic lighting engine, which applies to versions before Beta 1.3. It is the only setting this
 * project has that is a genuine player preference rather than a fact about the version - every other historical
 * behaviour is what the build did and is not offered as a choice. The screen says so, because a player who found one
 * toggle might reasonably expect more.
 *
 * <h2>Why the option states its own applicability rather than hiding itself</h2>
 *
 * <p>An option that silently disappears is indistinguishable from one that is broken, and this project has already
 * been misled once by exactly that shape - a mixin that failed silently and lit nothing. So the button is always
 * present and always says which of the three situations applies:
 *
 * <ul>
 *   <li>the current version predates smooth lighting, so the engine is available and can be switched;</li>
 *   <li>the current version is Beta 1.3 or later, so there is nothing to switch - the game's own engine IS the
 *       engine, and the button explains that instead of vanishing;</li>
 *   <li>no historical version is active at all, so the option does not apply.</li>
 * </ul>
 *
 * <p>When it is not available the button is <b>inactive</b> rather than absent, which keeps the screen's shape
 * stable and makes the reason readable.
 */
public final class HistoricalConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_WIDTH = 260;

    private final Screen parent;

    public HistoricalConfigScreen(Screen parent) {
        super(Component.translatable("historical_experience.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boolean available = HistoricalLightingEngine.availableFor(HistoricalClientRules.activeVersion());
        boolean on = HistoricalLightingEngine.toggle();

        int centre = this.width / 2;
        int y = this.height / 4;

        this.addRenderableWidget(Button.builder(lightingLabel(available, on), button -> {
            // The engine refuses a change it cannot honour, so the label is rebuilt from the engine's own state
            // rather than from a local flag that could disagree with it.
            HistoricalLightingEngine.setToggle(!HistoricalLightingEngine.toggle());
            button.setMessage(lightingLabel(
                HistoricalLightingEngine.availableFor(HistoricalClientRules.activeVersion()),
                HistoricalLightingEngine.toggle()));
        })
            .bounds(centre - ROW_WIDTH / 2, y, ROW_WIDTH, ROW_HEIGHT)
            .build());

        if (!available) {
            // Inactive, not hidden: see the class comment. A greyed option beside its reason is information; a
            // missing option is a puzzle.
            this.children().stream()
                .filter(child -> child instanceof Button)
                .map(child -> (Button) child)
                .forEach(button -> button.active = false);
        }

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(centre - ROW_WIDTH / 2, this.height - 40, ROW_WIDTH, ROW_HEIGHT)
            .build());
    }

    /** The button's text: what the option is, what state it is in, and - when it cannot apply - why. */
    private static Component lightingLabel(boolean available, boolean on) {
        if (HistoricalClientRules.activeVersion() == null) {
            return Component.translatable("historical_experience.config.lighting.no_world");
        }
        if (!available) {
            return Component.translatable("historical_experience.config.lighting.not_applicable");
        }
        return Component.translatable(on
            ? "historical_experience.config.lighting.on"
            : "historical_experience.config.lighting.off");
    }

    /** Puts the reasoning on the screen, so a player does not have to consult a changelog to understand one toggle. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 30, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
            Component.translatable("historical_experience.config.lighting.note"),
            this.width / 2, this.height / 4 + ROW_HEIGHT + 8, 0xA0A0A0);
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

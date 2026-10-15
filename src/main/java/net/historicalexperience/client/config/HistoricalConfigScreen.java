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
package net.historicalexperience.client.config;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.historicalexperience.client.lighting.HistoricalSmoothLighting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The mod's options screen, reachable from Mod Menu's config button.
 *
 * <h2>The options</h2>
 *
 * <ul>
 *   <li><b>Old smooth shading</b> - Beta 1.3 to Beta 1.7.3. In this window the difference is the corner shading: the
 *       modern engine darkens a corner towards 0.2 and this one did not darken it at all.</li>
 *   <li><b>Old lighting</b> - every declared build before the Beta 1.8 Pre-release. Grey light with no tint, sky
 *       light that follows the time of day (so the game re-lit chunks at dusk and dawn), and no flicker.</li>
 *   <li><b>Version name in corner</b> - the builds that drew their own version in the top-left.</li>
 *   <li><b>Developer mode</b> - off by default. Shows this project's own accounting: version ids, generation recipe
 *       aliases and revisions, upgrade report detail and the measured build beside each profile. See
 *       {@link HistoricalDeveloperMode} for what it gates and why the default is off.</li>
 * </ul>
 *
 * <p>The two windows overlap between Beta 1.3 and Beta 1.7.3, where both really did apply, so both buttons are live
 * there and each can be switched off on its own.
 *
 * <p>A third option used to sit here called "Classic lighting". It is gone: Classic is Minecraft before Infdev, and
 * the oldest version this project declares is Infdev 20100420, so that engine could never apply to any world here.
 * The owner removed it for that reason. Before Beta 1.3 there was no smooth lighting at all, which is a real fact and
 * is still enforced - but by the game's own setting being answered false in {@code MinecraftHistoricalSmoothLightingMixin},
 * not by an engine option, because there is nothing to choose between.
 *
 * <h2>Why no option is ever greyed out</h2>
 *
 * <p>Each row used to be inactive whenever the world being played did not offer the thing it controls, and the two
 * lighting rows refused to even store the change in that state. The owner's report was that turning old lighting and
 * old smooth lighting on or off did nothing - in the game, and still nothing after reopening the world - and the cause
 * was that refusal: Mod Menu is usually opened with no world open, where the write was discarded. Every row is now
 * clickable at all times, the setting is always stored, and the row's own text says which worlds it is used in. See
 * {@link OptionScope}.
 *
 * <h2>Why the widget references are kept</h2>
 *
 * <p>Each button is held in a field and its label and active state are set from that reference, rather than
 * re-deriving which button is which by string-matching its own labels - which is fragile in exactly the way that
 * breaks when a translation changes.
 */
public final class HistoricalConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_WIDTH = 300;

    private final Screen parent;

    private Button smoothButton;
    private Button oldLightButton;
    private Button versionLabelButton;
    private Button developerButton;

    public HistoricalConfigScreen(Screen parent) {
        super(Component.translatable("historical_experience.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centre = this.width / 2;
        int y = this.height / 4;

        this.smoothButton = this.addRenderableWidget(
            Button.builder(smoothLabel(), button -> {
                HistoricalSmoothLighting.setToggle(!HistoricalSmoothLighting.toggle());
                refresh();
            })
                .bounds(centre - ROW_WIDTH / 2, y, ROW_WIDTH, ROW_HEIGHT)
                .build());

        this.oldLightButton = this.addRenderableWidget(
            Button.builder(oldLightLabel(), button -> {
                HistoricalOldLighting.setToggle(!HistoricalOldLighting.toggle());
                refresh();
            })
                .bounds(centre - ROW_WIDTH / 2, y + ROW_HEIGHT + 4, ROW_WIDTH, ROW_HEIGHT)
                .build());

        this.versionLabelButton = this.addRenderableWidget(
            Button.builder(versionLabelLabel(), button -> {
                net.historicalexperience.client.hud.HistoricalVersionOverlay.setToggle(
                    !net.historicalexperience.client.hud.HistoricalVersionOverlay.toggle());
                refresh();
            })
                .bounds(centre - ROW_WIDTH / 2, y + (ROW_HEIGHT + 4) * 2, ROW_WIDTH, ROW_HEIGHT)
                .build());

        // The historical texture system has its own section; this button is how Mod Menu's config button reaches it.
        this.addRenderableWidget(
            Button.builder(Component.translatable("historical_experience.textures.config.open"),
                    button -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(
                                new net.historicalexperience.client.textures.TexturesConfigScreen(this));
                        }
                    })
                .bounds(centre - ROW_WIDTH / 2, y + (ROW_HEIGHT + 4) * 3, ROW_WIDTH, ROW_HEIGHT)
                .build());

        // Last, and never greyed: unlike the three above it, this one does not depend on which version is active. It
        // is off by default because what it reveals - version ids, generation recipe aliases, document revisions,
        // backup paths - is written for whoever maintains this mod.
        this.developerButton = this.addRenderableWidget(
            Button.builder(developerLabel(), button -> {
                HistoricalDeveloperMode.setEnabled(!HistoricalDeveloperMode.enabled());
                refresh();
            })
                .bounds(centre - ROW_WIDTH / 2, y + (ROW_HEIGHT + 4) * 4, ROW_WIDTH, ROW_HEIGHT)
                .build());

        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centre - ROW_WIDTH / 2, this.height - 40, ROW_WIDTH, ROW_HEIGHT)
                .build());

        refresh();
    }

    /**
     * Re-reads every engine and brings the buttons in line with it.
     *
     * <p>None of them is disabled: a preference can be set in any world, or none, and the label says where it will be
     * used. See the class comment for the report that established this.
     */
    private void refresh() {
        if (this.smoothButton != null) {
            this.smoothButton.setMessage(smoothLabel());
        }
        if (this.oldLightButton != null) {
            this.oldLightButton.setMessage(oldLightLabel());
        }
        if (this.versionLabelButton != null) {
            this.versionLabelButton.setMessage(versionLabelLabel());
        }
        if (this.developerButton != null) {
            this.developerButton.setMessage(developerLabel());
        }
    }

    private static net.historicalexperience.version.HistoricalVersion activeVersion() {
        return HistoricalClientRules.activeVersion();
    }

    /**
     * One row's text: the option's name, whether it is on, where that applies, and what clicking does.
     *
     * @param option  the language-file stem, e.g. {@code smooth}
     * @param on      the stored preference
     * @param applies whether the world being played uses it
     */
    private static Component row(String option, boolean on, boolean applies) {
        String scope = OptionScope.of(activeVersion(), applies).keySuffix();
        return Component.translatable("historical_experience.config." + option + (on ? ".on" : ".off"),
            Component.translatable("historical_experience.config." + option + ".note." + scope));
    }

    /** The old smooth shading engine's row, which applies from Beta 1.3 to Beta 1.7.3. */
    private static Component smoothLabel() {
        return row("smooth", HistoricalSmoothLighting.toggle(),
            HistoricalSmoothLighting.availableFor(activeVersion()));
    }

    /** The old lighting's row: grey light, sky light that follows the time of day, no flicker. */
    private static Component oldLightLabel() {
        return row("oldlight", HistoricalOldLighting.toggle(),
            HistoricalOldLighting.availableFor(activeVersion()));
    }

    /** The corner version name's row, drawn only by the builds that drew it. */
    private static Component versionLabelLabel() {
        return row("versionlabel", net.historicalexperience.client.hud.HistoricalVersionOverlay.toggle(),
            net.historicalexperience.client.hud.HistoricalVersionOverlay.availableFor(activeVersion()));
    }

    /** The developer-mode button's text. Available in every world and at the title screen, so it has no "no world" form. */
    private static Component developerLabel() {
        return Component.translatable(HistoricalDeveloperMode.enabled()
            ? "historical_experience.config.developer.on"
            : "historical_experience.config.developer.off");
    }

    /** Puts the reasoning on the screen, so a player does not have to consult a changelog to understand two toggles. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 34, 0xFFFFFF);
        // Wrapped to the row width: the note is long, and a single centred line ran off both edges of the screen.
        // Clamped so the last row of it cannot collide with the Done button on a short window.
        int noteY = Math.min(this.height / 4 + (ROW_HEIGHT + 4) * 5 + 12, this.height - 62);
        for (net.minecraft.util.FormattedCharSequence line : this.font.split(
                Component.translatable("historical_experience.config.note"), ROW_WIDTH)) {
            graphics.drawString(this.font, line, this.width / 2 - ROW_WIDTH / 2, noteY, 0xA0A0A0);
            noteY += 10;
        }
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

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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Reports the outcome of a version change, in the words the operation itself produced.
 *
 * <p>Deliberately plain: the authoritative layer already answers in sentences - what happened, what it means, and
 * what to expect next - and rewording that here would be a second account of the same event that could disagree
 * with the first. The screen prints the report it was given, line for line.
 */
public class HistoricalVersionResultScreen extends Screen {
    private final Screen lastScreen;
    private final boolean ok;
    private final String summary;
    private final List<String> details;

    public HistoricalVersionResultScreen(Screen lastScreen, boolean ok, String summary, List<String> details) {
        super(Component.literal("Historical Version"));
        this.lastScreen = lastScreen;
        this.ok = ok;
        this.summary = summary;
        this.details = List.copyOf(details);
    }

    @Override
    protected void init() {
        StringBuilder text = new StringBuilder(summary);
        for (String line : this.details) {
            text.append('\n').append(line);
        }

        MultiLineTextWidget body = new MultiLineTextWidget(12, 40, Component.literal(text.toString()), this.font)
            .setMaxWidth(this.width - 24)
            .setMaxRows(Math.max(4, (this.height - 90) / 10));
        addRenderableOnly(body);

        addRenderableWidget(Button.builder(Component.literal("Done"), pressed -> onClose())
            .pos(this.width / 2 - 75, this.height - 26)
            .width(150)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 16,
            this.ok ? 0xFF55FF55 : 0xFFFF5555);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}

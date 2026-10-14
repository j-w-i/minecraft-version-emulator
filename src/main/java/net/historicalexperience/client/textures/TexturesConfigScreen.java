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
package net.historicalexperience.client.textures;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The Historical Textures section of the mod's configuration, reached from the existing options screen (and therefore
 * from Mod Menu). Deliberately small: one switch, the facts a player needs to understand what is happening, and two
 * buttons.
 */
public final class TexturesConfigScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private Button enabledButton;
    private List<Component> dependencyLines = List.of();

    public TexturesConfigScreen(Screen parent) {
        super(Component.translatable("historical_experience.textures.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.dependencyLines = TextureStatusView.dependencyLines();
        int left = this.width / 2 - WIDTH / 2;
        int half = (WIDTH - 4) / 2;
        int bottom = this.height - 28;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(left, bottom, WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("historical_experience.textures.open_folder"),
                button -> {
                    if (this.minecraft != null) {
                        Util.getPlatform().openPath(HistoricalTextureService.packDirectory());
                    }
                })
            .bounds(left, bottom - (BUTTON_HEIGHT + 4), half, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("historical_experience.textures.rescan"),
                button -> {
                    HistoricalTextureService.rescan();
                    this.rebuildWidgets();
                })
            .bounds(left + half + 4, bottom - (BUTTON_HEIGHT + 4), half, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("historical_experience.textures.reopen_setup"),
                button -> {
                    // Reopening is an explicit request to look again; it does not un-complete setup, so closing it
                    // does not cause it to reappear by itself on the next launch.
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new TextureSetupScreen(this));
                    }
                })
            .bounds(left, bottom - 2 * (BUTTON_HEIGHT + 4), half, BUTTON_HEIGHT).build());
        Button download = this.addRenderableWidget(Button.builder(
                Component.translatable("historical_experience.textures.download_missing"),
                button -> {
                    HistoricalTextureConfig.setAutoDownload(true);
                    HistoricalTextureService.downloadMissing(this.minecraft);
                    button.active = false;
                })
            .bounds(left + half + 4, bottom - 2 * (BUTTON_HEIGHT + 4), half, BUTTON_HEIGHT).build());
        download.active = !HistoricalTextureService.missingDependencies().isEmpty()
            && !HistoricalTextureService.downloader().isRunning();

        this.enabledButton = this.addRenderableWidget(Button.builder(enabledLabel(), button -> {
                HistoricalTextureConfig.setEnabled(!HistoricalTextureConfig.enabled());
                button.setMessage(enabledLabel());
            })
            .bounds(left, 34, WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(autoDownloadLabel(), button -> {
                HistoricalTextureConfig.setAutoDownload(!HistoricalTextureConfig.autoDownload());
                button.setMessage(autoDownloadLabel());
            })
            .bounds(left, 34 + BUTTON_HEIGHT + 2, WIDTH, BUTTON_HEIGHT).build());
    }

    private static Component autoDownloadLabel() {
        return Component.translatable(HistoricalTextureConfig.autoDownload()
            ? "historical_experience.textures.config.autodownload.on"
            : "historical_experience.textures.config.autodownload.off");
    }

    private static Component enabledLabel() {
        return Component.translatable(HistoricalTextureConfig.enabled()
            ? "historical_experience.textures.config.enabled.on"
            : "historical_experience.textures.config.enabled.off");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - WIDTH / 2;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        int y = 34 + (BUTTON_HEIGHT + 2) * 2 + 4;
        graphics.drawString(this.font, TextureStatusView.versionLine(), left, y, 0xFFFFFF);
        y += 12;
        graphics.drawString(this.font, Component.translatable("historical_experience.textures.config.stack")
            .append(": ").append(TextureStatusView.stackLine()), left, y, 0xC0C0C0);
        y += 14;
        for (Component line : this.dependencyLines) {
            graphics.drawString(this.font, line, left + 4, y, 0xFFFFFF);
            y += 11;
        }
        y += 6;
        for (Component line : TextureStatusView.ruleLines()) {
            graphics.drawString(this.font, line, left + 4, y, 0xA0A0A0);
            y += 10;
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}

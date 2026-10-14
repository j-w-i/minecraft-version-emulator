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
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The first-run setup screen: explains the historical texture system, shows which of the four packs it found, and lets
 * the player fetch the missing ones from Modrinth at their pinned versions.
 *
 * <h2>Consent, not surprise</h2>
 *
 * <p>Nothing is downloaded until the player presses the download button on this screen, which names the source
 * (Modrinth), the exact versions and that the packs are third-party works. Choosing to continue without downloading
 * turns automatic download off, so later sessions never fetch anything the player declined. Players who already have the
 * packs installed are not asked to download anything: the scan finds them by content under any file name.
 *
 * <p>Finishing, continuing without downloading and turning the feature off all record that setup was seen, so it is not
 * shown again unless the player reopens it from the configuration screen. Escape records nothing.
 */
public final class TextureSetupScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private List<Component> dependencyLines = List.of();
    private List<FormattedCharSequence> intro = List.of();
    private Button primary;
    private boolean sawDownloadRunning;

    public TextureSetupScreen(Screen parent) {
        super(Component.translatable("historical_experience.textures.setup.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.dependencyLines = TextureStatusView.dependencyLines();
        this.intro = this.font.split(Component.translatable("historical_experience.textures.setup.intro"), WIDTH);
        int left = this.width / 2 - WIDTH / 2;
        int half = (WIDTH - 4) / 2;
        // Lay the buttons out from the height of the text above them, so a long consent paragraph or a small window can
        // never put text under a button. The text block is measured with the same splitting the renderer uses.
        int bottom = Math.max(contentBottom() + 6 + 2 * (BUTTON_HEIGHT + 4), this.height - 28);
        bottom = Math.min(bottom, this.height - 6 - BUTTON_HEIGHT);

        this.addRenderableWidget(Button.builder(Component.translatable("historical_experience.textures.setup.skip"),
                // Declines the downloads only. The feature stays on and works with whatever is installed; a player who
                // wants it off entirely uses the switch in the configuration screen.
                button -> finish(false, true))
            .bounds(left + half + 4, bottom, half, BUTTON_HEIGHT).build());

        boolean complete = HistoricalTextureService.missingDependencies().isEmpty();
        this.primary = this.addRenderableWidget(Button.builder(primaryLabel(complete), button -> onPrimary(complete))
            .bounds(left, bottom, half, BUTTON_HEIGHT).build());
        this.primary.active = !HistoricalTextureService.downloader().isRunning();

        int row = bottom - (BUTTON_HEIGHT + 4);
        this.addRenderableWidget(Button.builder(Component.translatable("historical_experience.textures.open_folder"),
                button -> openFolder())
            .bounds(left, row, half, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.translatable("historical_experience.textures.rescan"),
                button -> {
                    HistoricalTextureService.rescan();
                    this.rebuildWidgets();
                })
            .bounds(left + half + 4, row, half, BUTTON_HEIGHT).build());
    }

    /** The y just below the last line of text the screen draws, mirroring {@link #render}. */
    private int contentBottom() {
        int y = 14 + 16 + this.intro.size() * 10 + 6 + 12;
        y += this.dependencyLines.size() * 11 + 4;
        y += 4;
        Component notice = noticeText();
        y += this.font.split(notice, WIDTH).size() * 10;
        return y;
    }

    private static Component noticeText() {
        return HistoricalTextureService.missingDependencies().isEmpty()
            ? Component.translatable("historical_experience.textures.setup.all_present")
            : Component.translatable("historical_experience.textures.setup.consent", pinnedList());
    }

    private static Component primaryLabel(boolean complete) {
        return complete
            ? Component.translatable("historical_experience.textures.setup.done")
            : Component.translatable("historical_experience.textures.setup.download");
    }

    /** All packs present: finish. Otherwise this is the player's explicit consent to download the missing ones. */
    private void onPrimary(boolean complete) {
        if (complete) {
            finish(false, false);
            return;
        }
        HistoricalTextureConfig.setAutoDownload(true);
        HistoricalTextureService.downloadMissing(this.minecraft);
        this.sawDownloadRunning = true;
        this.primary.active = false;
    }

    private void openFolder() {
        if (this.minecraft != null) {
            Util.getPlatform().openPath(HistoricalTextureService.packDirectory());
        }
    }

    /**
     * @param disable          turn the whole feature off
     * @param declineDownloads remember that the player declined automatic downloads
     */
    private void finish(boolean disable, boolean declineDownloads) {
        if (disable) {
            HistoricalTextureConfig.setEnabled(false);
        }
        if (declineDownloads) {
            HistoricalTextureConfig.setAutoDownload(false);
        }
        HistoricalTextureConfig.markSetupCompleted();
        this.onClose();
    }

    @Override
    public void tick() {
        // When a download finishes the service rescans; redraw so the ticks and the button reflect it.
        PackDownloader loader = HistoricalTextureService.downloader();
        if (this.sawDownloadRunning && !loader.isRunning()) {
            this.sawDownloadRunning = false;
            this.rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - WIDTH / 2;
        int y = 14;
        graphics.drawCenteredString(this.font, this.title, this.width / 2, y, 0xFFFFFF);
        y += 16;
        for (FormattedCharSequence line : this.intro) {
            graphics.drawString(this.font, line, left, y, 0xC0C0C0);
            y += 10;
        }
        y += 6;
        graphics.drawString(this.font, Component.translatable("historical_experience.textures.setup.packs"),
            left, y, 0xFFFFFF);
        y += 12;
        for (Component line : this.dependencyLines) {
            graphics.drawString(this.font, line, left + 4, y, 0xFFFFFF);
            y += 11;
        }
        y += 4;
        y = renderProgress(graphics, left, y);
        y += 4;
        for (FormattedCharSequence line : this.font.split(noticeText(), WIDTH)) {
            graphics.drawString(this.font, line, left, y, 0x909090);
            y += 10;
        }
    }

    private int renderProgress(GuiGraphics graphics, int left, int y) {
        PackDownloader loader = HistoricalTextureService.downloader();
        for (TextureSource source : TextureSource.values()) {
            if (!source.isDependency() || source == TextureSource.PROGRAMMER_ART) {
                continue;
            }
            PackDownloader.State state = loader.state(source);
            if (state.phase() == PackDownloader.Phase.DOWNLOADING) {
                int percent = state.total() <= 0 ? 0 : (int) (state.bytes() * 100 / state.total());
                graphics.drawString(this.font, Component.translatable("historical_experience.textures.setup.downloading",
                    Component.translatable(source.translationKey()), percent).withStyle(ChatFormatting.YELLOW),
                    left + 4, y, 0xFFFFFF);
                y += 10;
            } else if (state.phase() == PackDownloader.Phase.FAILED) {
                graphics.drawString(this.font, Component.translatable("historical_experience.textures.setup.failed",
                    Component.translatable(source.translationKey()), state.message()).withStyle(ChatFormatting.RED),
                    left + 4, y, 0xFFFFFF);
                y += 10;
            }
        }
        return y;
    }

    private static String pinnedList() {
        StringBuilder out = new StringBuilder();
        for (TextureSource source : HistoricalTextureService.missingDependencies()) {
            PackPin pin = PackPin.of(source);
            if (pin != null) {
                if (!out.isEmpty()) {
                    out.append(", ");
                }
                out.append(Component.translatable(source.translationKey()).getString())
                    .append(' ').append(pin.versionNumber());
            }
        }
        return out.toString();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Escape is "later": it must not record completion, or a player who dismissed it by reflex would never be told
        // what is missing.
        return true;
    }
}

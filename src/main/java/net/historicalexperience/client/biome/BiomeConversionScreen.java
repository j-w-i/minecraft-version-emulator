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
package net.historicalexperience.client.biome;

import net.historicalexperience.world.upgrade.HistoricalBiomeConversionDriver;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The screen shown while a world's stored chunk biomes are converted, and the gate that holds the world back.
 *
 * <p>It is opened by the world-open hook <em>instead of</em> opening the world, so the region files are never handed
 * to a server that would write over them. When the conversion reports finished this screen re-issues the world load,
 * which then takes the ordinary path.
 *
 * <p>Drawn in the same shape as the game's own "Loading terrain" progress: the world name, a percentage, and a bar.
 * The bar is smoothed towards the measured value so it cannot jump backwards.
 */
public class BiomeConversionScreen extends Screen {
    private static final int BAR_WIDTH = 240;
    private static final int BAR_HEIGHT = 12;

    private final String worldName;
    private final Runnable whenFinished;
    private final Runnable onFailure;
    private boolean failureShown;
    private boolean continuationQueued;
    private float smoothed;
    private int waitedTicks;

    public BiomeConversionScreen(String worldName, Runnable whenFinished, Runnable onFailure) {
        super(Component.translatable("historical_experience.biome_conversion.title"));
        this.worldName = worldName;
        this.whenFinished = whenFinished;
        this.onFailure = onFailure;
    }

    @Override
    public void tick() {
        waitedTicks++;
        if (HistoricalBiomeConversionDriver.finished()) {
            if (HistoricalBiomeConversionDriver.failed()) {
                // Never re-open automatically after failure: doing so immediately retries the owed conversion forever.
                if (!failureShown) {
                    failureShown = true;
                    // From tick() rather than render(): render walks the widget list, and adding to a collection while
                    // it is being iterated is how a progress screen becomes a ConcurrentModificationException.
                    addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        Component.translatable("gui.back"), button -> onFailure.run())
                        .bounds(width / 2 - 100, height / 2 + 40, 200, 20).build());
                }
                return;
            }
            // Let the finished bar be seen for a moment rather than snapping into the world, then hand off OUTSIDE
            // this screen's tick.
            //
            // Deferred deliberately, and this is a crash fix, not tidiness. Going straight to the world from tick()
            // cleared Minecraft.screen while Screen.tick() was still on the stack; Fabric's screen API fires its
            // afterTick event against Minecraft.screen once the screen's own tick returns, so it was handed null and
            // the game died with "java.lang.NullPointerException: Screen cannot be null" - measured twice on
            // 2026-10-15, once in the owner's world and once in a fixture world built to owe a conversion.
            //
            // tell(), NOT execute(). Read from the 1.21.1 bytecode: BlockableEventLoop.execute runs the task
            // INLINE when it is called from the game thread (`ay()` is "not on the game thread"), so execute would
            // have deferred nothing and the crash survived the first attempt at this fix - which the same fixture
            // then proved by crashing again. tell() adds the task to the queue unconditionally, and that queue is
            // drained by runAllTasks from Minecraft's own tick, never from inside a Screen.tick().
            if (waitedTicks > 20 && !continuationQueued) {
                continuationQueued = true;
                net.minecraft.client.Minecraft.getInstance().tell(whenFinished);
            }
            return;
        }
        int total = HistoricalBiomeConversionDriver.chunksTotal();
        if (total > 0) {
            smoothed = Math.max(smoothed,
                (float) HistoricalBiomeConversionDriver.chunksDone() / (float) total);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int centreX = width / 2;
        int centreY = height / 2;

        graphics.drawCenteredString(font, title, centreX, centreY - 50, 0xFFFFFF);

        // Name the biome actually being converted to: from Alpha v1.2.0 it is Beta plains and from Beta 1.8.1 it is
        // Late Beta plains, so a fixed label would be wrong for one of the two. The value is the conversion's own
        // record label ("biome_hierarchy_v4:beta_plains_only"), which is this project's internal bookkeeping and
        // meaningless to a player, so it is only shown in developer mode; otherwise the world's name stands here.
        String target = net.historicalexperience.client.config.HistoricalDeveloperMode.enabled()
            ? HistoricalBiomeConversionDriver.runTarget()
            : "";
        graphics.drawCenteredString(font, Component.literal(target.isEmpty() ? worldName : target),
            centreX, centreY - 36, 0xA0A0A0);

        int total = HistoricalBiomeConversionDriver.chunksTotal();
        int done = HistoricalBiomeConversionDriver.chunksDone();
        String status;
        if (HistoricalBiomeConversionDriver.finished()) {
            status = HistoricalBiomeConversionDriver.detail();
        } else if (total <= 0) {
            status = net.minecraft.network.chat.Component
                .translatable("historical_experience.biome_conversion.scanning").getString();
        } else {
            status = done + " / " + total + "  (" + (int) (smoothed * 100.0F) + "%)";
        }
        graphics.drawCenteredString(font, Component.literal(status), centreX, centreY - 20, 0xFFFFFF);

        int left = centreX - BAR_WIDTH / 2;
        graphics.fill(left - 1, centreY - 3, left + BAR_WIDTH + 1, centreY + BAR_HEIGHT + 1, 0xFF000000);
        graphics.fill(left, centreY - 2, left + BAR_WIDTH, centreY + BAR_HEIGHT, 0xFF303030);
        int filled = (int) (BAR_WIDTH * Math.max(0.0F, Math.min(1.0F, smoothed)));
        if (filled > 0) {
            graphics.fill(left, centreY - 2, left + filled, centreY + BAR_HEIGHT, 0xFF3CB43C);
        }

        String file = HistoricalBiomeConversionDriver.currentFile();
        if (!file.isEmpty() && !HistoricalBiomeConversionDriver.finished()) {
            graphics.drawCenteredString(font, Component.literal(file), centreX, centreY + 24, 0x808080);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Closing this would leave the conversion running with the world half-open, so it is not offered.
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

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
package net.historicalexperience.mixin.client;

import java.util.Optional;
import java.util.function.Consumer;
import net.historicalexperience.client.textures.HistoricalTextureService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the Mojang loading screen while the historical texture packs are being applied.
 *
 * <p>Turning the texture stack on or off, and switching the colour maps for an era, each need a resource reload, and
 * the game covers every reload with {@link LoadingOverlay}: the red Mojang Studios logo and a progress bar. Entering a
 * historical world does it twice, so the player saw that screen flash up in the middle of joining.
 *
 * <p>The overlay cannot simply be removed, because its {@code render} is also what ENDS the reload: it notices the
 * reload is done, runs the finish callback (which is where a failed reload rolls the player's packs back) and clears
 * itself. So for the overlay of a reload this service started - and only that one - this replaces the drawing and keeps
 * the finishing, in the same order vanilla does it. The screen underneath is still drawn, as vanilla does while it
 * fades.
 *
 * <p>Reloads the player starts (F3+T, the resource-pack screen) and the game's own start-up load keep the ordinary
 * screen: the overlay is recognised by identity, not by a global switch.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayHistoricalQuietMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$renderQuietly(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        if (!HistoricalTextureService.isQuietOverlay(this)) {
            return;
        }
        if (this.minecraft.screen != null) {
            this.minecraft.screen.render(graphics, 0, 0, partialTick);
        }
        if (this.reload.isDone()) {
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable failure) {
                this.onFinish.accept(Optional.of(failure));
            }
            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, graphics.guiWidth(), graphics.guiHeight());
            }
            this.minecraft.setOverlay(null);
        }
        callback.cancel();
    }
}
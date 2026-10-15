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
package net.historicalexperience.mixin;

import net.historicalexperience.client.hud.HudHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the experience level number from versions that had the bar without it.
 *
 * <p>Owner: <i>"The experience point number was only added in a later version ... you couldnt see how much
 * experience you had in beta 1.8 or pre 1"</i>. minecraft.wiki, <i>Experience</i> § History, puts the number in
 * <b>Beta 1.9 Prerelease 4</b> ("The experience level number is now shown") and the bar itself in Beta 1.8's
 * Adventure Update, so the two are separate boundaries and this project had only the bar. See
 * {@code HistoricalGameplayTweaks.experienceLevelShown} and {@code HudHelper.hideExperienceLevel}.
 *
 * <h2>Why not port this from Nostalgic Tweaks</h2>
 *
 * <p>Upstream has no equivalent to copy. Its nearest feature, {@code CandyTweak.SHOW_EXP_LEVEL_TEXT}, is an optional
 * corner readout of the level (default off, drawn by its own {@code GuiListener.renderTextOverlay}) and not the
 * vanilla number above the bar, and its {@code HIDE_EXPERIENCE_BAR} hides the whole bar through
 * {@code HudHelper.apply(graphics, HudElement.EXPERIENCE_BAR)} - which this project already ports. Cancelling the
 * game's own level-number method is therefore this project's own work, not a port, and this file carries the project
 * licence header alone.
 *
 * <h2>Why cancelling the drawing is right here</h2>
 *
 * <p>{@code renderExperienceLevel} draws the number and nothing else, so cancelling it at its head leaves the bar,
 * the bar's layout and every other HUD element exactly as they were. The alternative - hiding the bar and drawing a
 * replacement without the number - would have to reproduce vanilla's bar geometry to avoid moving the elements
 * around it.
 *
 * <h2>The descriptor is (GuiGraphics, DeltaTracker)</h2>
 *
 * <p>Resolved from the mappings rather than guessed: {@code fhz} is {@code class_332} = {@code GuiGraphics} and
 * {@code fgf} is {@code class_9779} = {@code DeltaTracker}. This project has now shipped one wrong mixin descriptor
 * (the recipe toast) and nearly shipped a second here, so the letters are resolved and the class is loaded in a real
 * client before the change is called verified.
 */
@Mixin(Gui.class)
public abstract class GuiHistoricalExperienceLevelMixin {
    /** Vanilla: {@code renderExperienceLevel(GuiGraphics, DeltaTracker)}. */
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$hideExperienceLevel(GuiGraphics graphics, DeltaTracker deltaTracker,
            CallbackInfo callback) {
        if (HudHelper.hideExperienceLevel()) {
            callback.cancel();
        }
    }
}

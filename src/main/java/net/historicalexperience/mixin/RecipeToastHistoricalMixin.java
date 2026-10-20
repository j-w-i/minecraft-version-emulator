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

import net.historicalexperience.version.ActiveVersion;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the "New Recipe Unlocked!" banner in a historical world.
 *
 * <p>Owner: <i>"you need to totally disable the banners that appear when you unlock new recipes"</i>. The recipe book
 * itself is already absent from every declared version - the newest declared profile is Release 1.6.4 and the book
 * arrives in Release 1.12 - so the banner is the visible half of a feature that is not there: it pops up over a game
 * that has no recipe book to open.
 *
 * <h2>Both entry points, because there are two</h2>
 *
 * <p>{@code addOrUpdate} is the one that creates or refreshes the toast; {@code addItem} is what appends the next
 * unlocked item to a toast that is already showing. Cancelling only the first would let a banner keep growing where
 * one had already appeared before this hook existed, and cancelling only the second would show up to one banner per
 * play session. Both are cancelled, and nothing else about the toast system is touched.
 *
 * <p>Gated on the session: an ordinary world has no historical version and keeps the vanilla banner.
 *
 * <h2>The parameter is a RecipeHolder, and this file shipped the wrong type once</h2>
 *
 * <p>The first version of this mixin declared {@code ItemStack}, which is what the toast <em>displays</em> rather
 * than what it is handed. It compiled, and it passed every offline check, because a mixin's target signature is only
 * verified when the game loads the target class - so the build was green right up to the moment the owner unlocked a
 * recipe and the client threw {@code InvalidInjectionException} on {@code RecipeToast} and disconnected with a
 * Network Protocol Error. The type is asserted at runtime by the mixin processor, and there is no substitute for
 * exercising the code path: see the report for the run that reopened this class.
 */
@Mixin(RecipeToast.class)
public abstract class RecipeToastHistoricalMixin {
    @Inject(method = "addOrUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    private static void historicalExperience$noRecipeBanner(ToastComponent toasts, RecipeHolder<?> recipe,
            CallbackInfo callback) {
        if (ActiveVersion.current() != null) {
            callback.cancel();
        }
    }

    @Inject(method = "addItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$noRecipeBannerItem(RecipeHolder<?> recipe, CallbackInfo callback) {
        if (ActiveVersion.current() != null) {
            callback.cancel();
        }
    }
}

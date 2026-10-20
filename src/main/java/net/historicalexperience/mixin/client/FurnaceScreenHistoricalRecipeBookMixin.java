/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.mixin.client;

import net.historicalexperience.client.HistoricalRecipeBookButton;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Port of the AbstractFurnaceScreenMixin.init caller of InventoryScreenHelper.setRecipeButton. */
@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceScreenHistoricalRecipeBookMixin {
    @Inject(method = "init", at = @At("TAIL"), require = 1)
    private void historicalExperience$setRecipeButton(CallbackInfo callback) {
        HistoricalRecipeBookButton.setRecipeButton((AbstractFurnaceScreen<?>) (Object) this);
    }
}

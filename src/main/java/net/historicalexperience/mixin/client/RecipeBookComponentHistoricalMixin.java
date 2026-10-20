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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides the recipe-book panel in historical worlds; screen mixins separately hide its independent modern button.
 *
 * <p>The original hook alone was insufficient: {@code RecipeBookComponent.isVisible()} controls the panel but does
 * not govern the ImageButton hosted by each screen. {@code HistoricalRecipeBookButton}, called from the inventory,
 * crafting and furnace init tails, ports Nostalgic Tweaks' {@code InventoryScreenHelper.setRecipeButton} disabled
 * branch to suppress that button as well. Keeping this hook preserves the panel's own visibility and layout behavior.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentHistoricalMixin {
    @ModifyReturnValue(method = "isVisible", at = @At("RETURN"), require = 1)
    private boolean historicalExperience$hideRecipeBook(boolean visible) {
        return HistoricalWorldTweaks.recipeBookDisabled() ? false : visible;
    }
}

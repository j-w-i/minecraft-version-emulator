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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.client.tooltip.HistoricalTooltips;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws no item tooltip before Beta 1.0.
 *
 * <p>Direct port of Nostalgic Tweaks' {@code tweak/candy/item_tooltip/AbstractContainerScreenMixin}, which answers
 * {@code OLD_NO_ITEM_TOOLTIPS} by forcing {@code Slot.hasItem()} false inside {@code renderTooltip}. The condition this
 * module reads is the active version's era instead of a player setting.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenHistoricalTooltipMixin {
    @ModifyExpressionValue(
        method = "renderTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;hasItem()Z"
        ),
        require = 1
    )
    private boolean historicalExperience$hideTooltipsBeforeBeta1(boolean hasItem) {
        return HistoricalTooltips.hidden() ? false : hasItem;
    }
}

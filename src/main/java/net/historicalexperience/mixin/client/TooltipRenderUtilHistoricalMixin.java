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

import net.historicalexperience.client.tooltip.HistoricalTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the legacy flat tooltip box, and suppresses the modern one, for Beta 1.0 through Beta 1.9 Pre-release 1.
 *
 * <p>Direct port of Nostalgic Tweaks' {@code fabric/mixin/tweak/candy/tooltip_background/TooltipRenderUtilMixin} plus
 * the {@code TooltipListener.render} it calls: upstream injects at the head of {@code renderTooltipBackground} and fills
 * a single rectangle three pixels outside the text, with the modern border hidden by zeroing the colours. That is what
 * this does, with the same colour upstream defaults to ({@code #000000C0}), gated by the active version's era.
 *
 * <p>Cancelling here replaces the whole modern background in one place, so the modern border colours never need to be
 * rewritten one call at a time.
 */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipRenderUtilHistoricalMixin {
    @Inject(method = "renderTooltipBackground", at = @At("HEAD"), cancellable = true, require = 1)
    private static void historicalExperience$legacyTooltipBox(GuiGraphics graphics, int x, int y, int width, int height,
            int z, CallbackInfo callback) {
        if (!HistoricalTooltips.legacyBox()) {
            return;
        }
        // Upstream's own geometry: three pixels of padding on every side of the text, filled flat.
        graphics.fill(x - 3, y - 3, x + width + 3, y + height + 3, z, HistoricalTooltips.LEGACY_BACKGROUND);
        callback.cancel();
    }
}

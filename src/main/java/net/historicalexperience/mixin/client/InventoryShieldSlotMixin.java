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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes the empty off-hand slot's icon in versions that had no off-hand.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/inventory_screen/InventoryShieldSlotMixin.java}
 * (LGPL-3.0-only; see THIRD_PARTY_NOTICES.md), including its target and its two conditions. Upstream's body is:
 *
 * <pre>
 *   &#64;ModifyReturnValue(method = "getNoItemIcon", at = &#64;At("RETURN"))
 *   private Pair&lt;ResourceLocation, ResourceLocation&gt; nt_inventory_screen$setNoShieldItemIcon(Pair&lt;...&gt; original)
 *   {
 *       if (CandyTweak.DISABLE_EMPTY_SHIELD_TEXTURE.get() &amp;&amp; GameUtil.isNotCreativeMode())
 *           return null;
 *       return original;
 *   }
 * </pre>
 *
 * <p>The first condition becomes this project's version gate - the off-hand did not exist, so its empty-slot icon
 * must not be drawn - and the second is upstream's own: {@code GameUtil.isCreativeMode()} is
 * {@code Minecraft.getInstance().gameMode.hasInfiniteItems()}, which is reproduced here rather than re-derived. The
 * icon is the shield outline the game draws over the empty off-hand slot, and it is drawn by the anonymous
 * {@code Slot} the player inventory's menu builds for that slot, hence the anonymous-class target.
 */
@Mixin(targets = "net/minecraft/world/inventory/InventoryMenu$1")
public abstract class InventoryShieldSlotMixin {
    @ModifyReturnValue(method = "getNoItemIcon", at = @At("RETURN"), require = 1)
    private Pair<ResourceLocation, ResourceLocation> historicalExperience$noEmptyShieldIcon(
            Pair<ResourceLocation, ResourceLocation> original) {
        if (Features.active(Feature.NO_OFFHAND)
                && !historicalExperience$inCreativeMode()) {
            return null;
        }
        return original;
    }

    /** Upstream's {@code GameUtil.isCreativeMode}, unchanged. */
    private static boolean historicalExperience$inCreativeMode() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameMode != null && minecraft.gameMode.hasInfiniteItems();
    }
}

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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.historicalexperience.gameplay.HistoricalFoodHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Stops the held item doing its equip animation after being eaten instantly.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/MinecraftMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.INSTANT_EAT}. The slice, the ordinal
 * and the {@code itemUsed} target are upstream's.
 *
 * <h2>Why this is a separate file from the sword block's identical injection</h2>
 *
 * <p>Both refuse the same call, for the same underlying reason - the equip animation - but they are different
 * rules with different conditions, and the sword block's one already exists. {@code @WrapWithCondition} handlers
 * compose on one instruction, so this is additive rather than a conflict: the call happens only when neither rule
 * refuses it. The two conditions cannot both be about one item, since an item is not a blocked sword and a meal
 * at once, so no ordering question arises.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftHistoricalInstantEatMixin {
    @Shadow @Nullable public LocalPlayer player;

    /** Upstream: {@code nt_food_health$shouldSetItemAsUsed}. */
    @WrapWithCondition(
        method = "startUseItem",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled("
                    + "Lnet/minecraft/world/flag/FeatureFlagSet;)Z")),
        at = @At(
            ordinal = 1,
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;itemUsed("
                + "Lnet/minecraft/world/InteractionHand;)V"))
    private boolean historicalExperience$shouldSetItemAsUsed(ItemInHandRenderer itemInHandRenderer,
            InteractionHand hand) {
        if (this.player == null) {
            return true;
        }

        return !HistoricalFoodHelper.isInstantaneousEdible(this.player.getItemInHand(hand));
    }
}

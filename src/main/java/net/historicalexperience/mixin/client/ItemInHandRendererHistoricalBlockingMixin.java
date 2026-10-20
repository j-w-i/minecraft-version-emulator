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
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.historicalexperience.client.render.SwordBlockingRenderer;
import net.historicalexperience.gameplay.HistoricalSwordBlocking;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses the raised sword in first person.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/ItemInHandRendererMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The injection points, their ordinals and the shifts between them are
 * upstream's.
 *
 * <p>This is a CLIENT mixin, registered under {@code client} in the mixin configuration:
 * {@code ItemInHandRenderer} does not exist on a dedicated server.
 *
 * <h2>The three injections kept, and what each does</h2>
 *
 * <ul>
 *   <li>Before and after the {@code renderItem} call, push and pop a pose so the block's transforms apply to the
 *       held item and nothing after it. Upstream guards this pair on its master switch; this project has no such
 *       switch, so the pair is unconditional - and symmetric, which is what keeps the pose stack balanced.</li>
 *   <li>On the {@code isUsingItem} read, answer false while blocking, so the vanilla "using an item" pose does
 *       not fight the block. This is the one that makes the difference between a raised sword and a sword being
 *       held up like a shield.</li>
 * </ul>
 *
 * <h2>The injection deliberately NOT ported</h2>
 *
 * <p>Upstream's fourth injection hides the off-hand SHIELD while sword blocking, for its
 * {@code BLOCK_WITH_SWORD_ON_SHIELD} case. Shields are 1.9 and no profile this project declares has one, so that
 * branch could never be reached: porting it would add a rule no version can exercise. The helper method it calls
 * is not ported either, for the same reason.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererHistoricalBlockingMixin {
    /** Upstream: {@code nt_combat_player$applyPreSwordBlockingAnimation}. */
    @Inject(
        method = "renderArmWithItem",
        at = @At(
            ordinal = 1,
            shift = At.Shift.BEFORE,
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem("
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
                + "Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void historicalExperience$applyPreSwordBlockingAnimation(AbstractClientPlayer player,
            float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack itemStack,
            float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
            CallbackInfo callback) {
        poseStack.pushPose();

        if (HistoricalSwordBlocking.isBlocking(player)) {
            SwordBlockingRenderer.applyFirstPerson(poseStack, player, hand);
        }
    }

    /** Upstream: {@code nt_combat_player$applyPostSwordBlockingAnimation}. */
    @Inject(
        method = "renderArmWithItem",
        at = @At(
            ordinal = 1,
            shift = At.Shift.AFTER,
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem("
                + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
                + "Lnet/minecraft/world/item/ItemDisplayContext;Z"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void historicalExperience$applyPostSwordBlockingAnimation(AbstractClientPlayer player,
            float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack itemStack,
            float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
            CallbackInfo callback) {
        poseStack.popPose();
    }

    /** Upstream: {@code nt_combat_player$shouldIndicateItemUse}. */
    @ModifyExpressionValue(
        method = "renderArmWithItem",
        at = @At(
            ordinal = 1,
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z"))
    private boolean historicalExperience$shouldIndicateItemUse(boolean isUsingItem,
            @Local(argsOnly = true) AbstractClientPlayer player) {
        if (HistoricalSwordBlocking.isBlocking(player)) {
            return false;
        }
        return isUsingItem;
    }
}

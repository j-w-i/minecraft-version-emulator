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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.historicalexperience.client.render.ArmorHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes armour take the red hurt tint, in versions where it did.
 *
 * <p>PORTED from Nostalgic Tweaks
 * {@code fabric/src/main/java/mod/adrenix/nostalgic/fabric/mixin/tweak/candy/armor_damage/HumanoidArmorLayerMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_DAMAGE_ARMOR_TINT}. The consumer
 * swap and the overlay argument are upstream's.
 *
 * <p>Two deliberate differences, both in this file's own history rather than implied: the entity is captured in
 * {@code renderArmorPiece}, which receives it as a parameter, instead of upstream's HEAD/RETURN pair around
 * {@code render}; and upstream's {@code renderTrim} injections are not ported, because armour trims arrive in
 * Release 1.20.5 and no declared profile can render one.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerHistoricalTintMixin<T extends LivingEntity,
        M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Unique private LivingEntity historicalExperience$entity;

    /** Captures the wearer, which {@code renderModel} does not receive. */
    @Inject(method = "renderArmorPiece", at = @At("HEAD"))
    private void historicalExperience$captureEntity(PoseStack poseStack, MultiBufferSource buffer,
            T entity, EquipmentSlot slot, int packedLight, A model, CallbackInfo callback) {
        this.historicalExperience$entity = entity;
    }

    /** Upstream: {@code nt_fabric_armor_damage$setRenderTypeConsumerForModel}. */
    @ModifyExpressionValue(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer("
                + "Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer historicalExperience$damagedConsumer(VertexConsumer vertexConsumer, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, A model, int dyeColor, ResourceLocation location) {
        return ArmorHelper.getDamagedConsumer(this.historicalExperience$entity, vertexConsumer, bufferSource,
            location);
    }

    /** Upstream: {@code nt_fabric_armor_damage$setOverlayTextureForModel}. Index 3 is the packed overlay. */
    @ModifyArg(
        method = "renderModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;renderToBuffer("
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
        index = 3)
    private int historicalExperience$damagedOverlay(int packedOverlay) {
        return ArmorHelper.getDamagedPackedOverlay(this.historicalExperience$entity, packedOverlay);
    }
}

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

import net.historicalexperience.client.render.SwordBlockingRenderer;
import net.historicalexperience.gameplay.HistoricalSwordBlocking;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the raised arm in third person, so other players see the old block stance.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/HumanoidModelMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The injection point - inside {@code setupAnim}, at the call to
 * {@code setupAttackAnimation} - and the two shadowed arms are upstream's.
 *
 * <p>This is a CLIENT mixin and is registered in the mixin configuration's {@code client} list:
 * {@code HumanoidModel} does not exist on a dedicated server, so a shared registration would fail to apply there
 * and take the server down with it.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelHistoricalBlockingMixin {
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;

    /** Upstream: {@code nt_combat_player$onSetupAnim}. */
    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation("
                + "Lnet/minecraft/world/entity/LivingEntity;F)V"))
    private <T extends LivingEntity> void historicalExperience$onSetupAnim(T entity, float limbSwing,
            float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callback) {
        if (entity instanceof Player player && HistoricalSwordBlocking.isBlocking(player)) {
            SwordBlockingRenderer.applyThirdPerson(player, this.leftArm, this.rightArm);
        }
    }
}

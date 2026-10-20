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
package net.historicalexperience.mixin;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.client.render.MobArmHelper;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a zombie's outstretched arms bobbing while it is idle.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/animation/mob_arms/AnimationUtilsMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The injection point - immediately before the {@code bobArms} call inside
 * {@code animateZombieArms} - and the call into {@code MobArmHelper} are upstream's.
 *
 * <h2>The second half of one tweak, found by reading to the end of the module</h2>
 *
 * <p>Round 62 ported the arm pose from {@code AbstractZombieModelMixin} and this file makes the pose hold still.
 * Upstream splits them because they hook different classes, not because they are separate features: with only the
 * first, a zombie stands with its arms in the right place and still waves them, which reads as nearly right - the
 * kind of near-miss that is harder to notice than an obvious fault. Reading the whole {@code mob_arms} directory
 * rather than the one file named after the flag is what surfaced it.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(AnimationUtils.class)
public abstract class AnimationUtilsHistoricalArmBobMixin {
    /** Upstream: {@code nt_mob_arms$onBobZombieArms}. */
    @Inject(
        method = "animateZombieArms",
        at = @At(
            shift = At.Shift.BEFORE,
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/AnimationUtils;bobArms("
                + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;F)V"))
    private static void historicalExperience$onBobZombieArms(ModelPart leftArm, ModelPart rightArm,
            boolean isAggressive, float attackTime, float ageInTicks, CallbackInfo callback) {
        if (Features.active(Feature.OLD_ZOMBIE_ARMS)) {
            MobArmHelper.applyStaticArms(rightArm, leftArm);
        }
    }
}

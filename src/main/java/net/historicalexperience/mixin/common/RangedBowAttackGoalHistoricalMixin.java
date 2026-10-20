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
package net.historicalexperience.mixin.common;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stops a skeleton moving sideways while it shoots, in versions whose AI did not do that.
 *
 * <p>Ported from Nostalgic Tweaks {@code DISABLE_SKELETON_STRAFING} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md). Strafing belongs to the goal-based mob AI that arrives with the Beta 1.8
 * Pre-release, so Infdev, Alpha and Beta 1.7.3 had archers that stood still and shot.
 *
 * <p>The redirect is on the {@code MoveControl.strafe} call inside {@code tick}, which is the whole
 * behaviour: the goal still aims, still draws and still fires, it simply does not sidestep. Cancelling the
 * goal instead would stop skeletons shooting at all, which is a different and much larger change - the
 * distinction is why the hook is on the movement call rather than on the goal.
 *
 * <p>Gated on the active session rather than a level, so the client predicts the same thing the server
 * enforces.
 */
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalHistoricalMixin {
    @Redirect(method = "tick",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/control/MoveControl;strafe(FF)V"))
    private void historicalExperience$noStrafingBeforeBetaOneEight(MoveControl control, float forward,
            float sideways) {
        if (!Features.active(Feature.NO_SKELETON_STRAFING)) {
            control.strafe(forward, sideways);
        }
    }
}

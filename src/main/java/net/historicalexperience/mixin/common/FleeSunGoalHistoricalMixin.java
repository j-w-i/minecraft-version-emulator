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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops monsters fleeing the sun, in versions whose monsters did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_ai/FleeSunGoalMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_MONSTER_AVOID_SUN}. The
 * injection point and its shape are upstream's: {@code @ModifyReturnValue} on {@code canUse}, so the goal
 * simply never becomes usable rather than being cancelled mid-flight. Only the condition changed, from a global
 * flag to this world's version.
 */
@Mixin(FleeSunGoal.class)
public abstract class FleeSunGoalHistoricalMixin {
    /** Upstream: {@code nt_mob_ai$canFleeSun}. */
    @ModifyReturnValue(method = "canUse", at = @At("RETURN"))
    private boolean historicalExperience$canFleeSun(boolean canFleeSun) {
        if (Features.active(Feature.NO_MONSTER_SUN_AVOIDANCE)) {
            return false;
        }
        return canFleeSun;
    }
}

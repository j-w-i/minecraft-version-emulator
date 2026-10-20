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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops animals following a player holding their food, in versions whose animals did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/animal_spawn/TemptGoalMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_ANIMAL_TEMPTING}.
 *
 * <p>The {@code instanceof AgeableMob} test is upstream's and is the second time it has mattered: this goal is
 * also used by mobs that are not animals, so without the test the tweak would stop more than its name says.
 * The shadowed {@code mob} field and the {@code @ModifyReturnValue} shape are upstream's too.
 */
@Mixin(TemptGoal.class)
public abstract class TemptGoalHistoricalMixin {
    @Shadow @Final protected PathfinderMob mob;

    /** Upstream: {@code nt_animal_spawn$modifyShouldFollow}. */
    @ModifyReturnValue(method = "shouldFollow", at = @At("RETURN"))
    private boolean historicalExperience$modifyShouldFollow(boolean shouldFollow) {
        if (this.mob instanceof AgeableMob
            && HistoricalGameplayTweaks.appliesTo("tempting", ActiveVersion.current())) {
            return false;
        }
        return shouldFollow;
    }
}

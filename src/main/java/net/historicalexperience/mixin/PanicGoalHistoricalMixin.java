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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops animals panicking when hurt, in versions whose animals did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_ai/PanicGoalMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_ANIMAL_PANIC}.
 *
 * <p>The {@code instanceof Animal} test is upstream's and is the part worth keeping: the goal is shared by
 * every {@code PathfinderMob}, so without it the tweak would also stop non-animals panicking, which is a wider
 * change than the name asks for. The shadowed {@code mob} field is upstream's too.
 */
@Mixin(PanicGoal.class)
public abstract class PanicGoalHistoricalMixin {
    @Shadow @Final protected PathfinderMob mob;

    /** Upstream: {@code nt_mob_ai$modifyShouldPanic}. */
    @ModifyReturnValue(method = "shouldPanic", at = @At("RETURN"))
    private boolean historicalExperience$modifyShouldPanic(boolean shouldPanic) {
        if (HistoricalGameplayTweaks.appliesTo("panic", ActiveVersion.current())
            && this.mob instanceof Animal) {
            return false;
        }
        return shouldPanic;
    }
}

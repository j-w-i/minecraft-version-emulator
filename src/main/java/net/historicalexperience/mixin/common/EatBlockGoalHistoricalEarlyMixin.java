/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Indev/Infdev sheep do not graze or regenerate cloth; regrowth arrived in Release 1.1 (11w49a). */
@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalHistoricalEarlyMixin {
    @Shadow @Final private Mob mob;
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$noEarlyGrazing(CallbackInfoReturnable<Boolean> cir) {
        if (mob.level() instanceof ServerLevel server
                && EarlyGameplayRecipes.supports(HistoricalProfileLookup.profileIdForLevel(server))) cir.setReturnValue(false);
    }
}

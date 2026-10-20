/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin.common;

import net.historicalexperience.server.EarlyMobDropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observer only: does not change entity acceptance or loot. No capture exists outside the opt-in owned probe. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelHistoricalMobDropProbeMixin {
    @Inject(method = "addEntity", at = @At("RETURN"), require = 1)
    private void historicalExperience$observeAcceptedProbeDrop(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ()) EarlyMobDropCapture.recordAccepted((ServerLevel) (Object) this, entity);
    }
}

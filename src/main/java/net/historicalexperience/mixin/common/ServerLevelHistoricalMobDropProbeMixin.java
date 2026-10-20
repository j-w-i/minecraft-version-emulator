/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.verification.EarlyMobDropCapture;

import net.historicalexperience.verification.EarlyMobDropCapture;
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

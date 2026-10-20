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

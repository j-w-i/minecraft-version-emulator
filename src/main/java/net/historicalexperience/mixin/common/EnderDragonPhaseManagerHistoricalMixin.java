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

import net.historicalexperience.gameplay.HistoricalDragonFight;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes the perch out of the dragon's behaviour.
 *
 * <p>The dragon only lands on the exit portal, sits there breathing, and takes off again since Release 1.9. Those are
 * six of its phases, and the only way into them is {@code setPhase}, so refusing them here leaves the original
 * behaviour: it flies. The dragon's own state machine asks again on the next tick (the decision to perch is a dice roll
 * in its holding pattern), so refusing a perch costs a few ticks of delay and nothing else.
 *
 * <p>The dying phase is deliberately not on the list: the dragon still flies to the portal and dies there.
 */
@Mixin(EnderDragonPhaseManager.class)
public abstract class EnderDragonPhaseManagerHistoricalMixin {
    @Shadow @Final private EnderDragon dragon;

    @Inject(method = "setPhase", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$noPerching(EnderDragonPhase<?> phase, CallbackInfo callback) {
        if (HistoricalDragonFight.isPerchPhase(phase)
            && this.dragon.level() instanceof net.minecraft.server.level.ServerLevel level
            && HistoricalDragonFight.originalFight(level)) {
            callback.cancel();
        }
    }
}
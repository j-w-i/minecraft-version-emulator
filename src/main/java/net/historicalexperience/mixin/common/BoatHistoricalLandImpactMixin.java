/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalBoatImpact;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A boat hitting land at speed breaks, from Alpha v1.0.6.
 *
 * <p>minecraft.wiki, Boat history: "v1.0.6_01 Boats now only break on impact with land at high speeds." Modern boats
 * only break from fall damage; the decision lives in {@link HistoricalBoatImpact} (pure, unit-tested) and this hook only
 * supplies the state. The removal is KILLED so the historical plank-and-stick drop runs.
 */
@Mixin(Boat.class)
public abstract class BoatHistoricalLandImpactMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void historicalExperience$breakOnLandImpact(CallbackInfo callback) {
        net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordBoatImpactCall();
        if (!((Object) this instanceof Boat boat) || boat.level().isClientSide() || boat.isRemoved()) {
            return;
        }
        if (!(boat.level() instanceof ServerLevel level)) {
            return;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(level);
        net.minecraft.world.phys.Vec3 motion = boat.getDeltaMovement();
        double speed = motion.horizontalDistance();
        if (speed < HistoricalBoatImpact.HIGH_SPEED) {
            return;
        }
        net.minecraft.world.phys.Vec3 ahead = boat.position().add(motion.normalize().scale(0.6D));
        boolean landAhead = level.getFluidState(net.minecraft.core.BlockPos.containing(ahead)).isEmpty();
        if (HistoricalBoatImpact.shouldBreak(profile, landAhead, boat.horizontalCollision, speed)) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordBoatImpactBreak();
            boat.setRemoved(Entity.RemovalReason.KILLED);
        }
    }
}

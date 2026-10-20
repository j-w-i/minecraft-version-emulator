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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalDragonFight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives the dragon the original's vertical steering, so a charge actually comes down to the player.
 *
 * <p>Each tick the dragon turns the vertical distance to its target into a vertical speed. Mojang's Release 1.0 client
 * multiplied that by 0.1 and clamped the ratio to 0.6; the 1.21.1 dragon multiplies by 0.01, a tenth, and clamps to its
 * phase's fly speed. With the modern gain the dragon is simply too slow to change height inside a charge, so it passed
 * over the player however low the target was set.
 *
 * <p>Only while the dragon is CHARGING_PLAYER, and only in a world that gets the original fight. The holding pattern -
 * the circling - keeps the modern gain, because that is what keeps its flight smooth, and nothing in the original's
 * circling needed the larger one to look right.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonVerticalDriveHistoricalMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "CONSTANT", args = "doubleValue=0.01"))
    private double historicalExperience$originalVerticalGain(double modernGain) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (dragon.level() instanceof ServerLevel level
            && dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.CHARGING_PLAYER
            && HistoricalDragonFight.originalFight(level)) {
            return HistoricalDragonFight.ORIGINAL_VERTICAL_GAIN;
        }
        return modernGain;
    }
}
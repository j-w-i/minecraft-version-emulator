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

import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Brewing takes 30 seconds before Release 1.1, not 20.
 *
 * <p>research/version-transitions.json records brewing "listed at 30 seconds" for Beta 1.9 Prerelease 3, and
 * minecraft.wiki, Java Edition 1.1: "Brewing - Reduced brewing time from 30 seconds to 20 seconds." The 400-tick
 * constant in {@code BrewingStandBlockEntity.serverTick} is 20 seconds; 600 is 30. The existing
 * {@code BrewingStandHistoricalBrewingMixin} still cancels brewing entirely before Prerelease 3.
 *
 * <p>The constant is modified rather than the field written after the fact: the tick compares against it on the same
 * pass that assigns it, so a one-tick-later correction would already have ticked the timer down once.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandHistoricalBrewTimeMixin {
    @ModifyConstant(method = "serverTick", constant = @Constant(intValue = 400))
    private static int historicalExperience$brewTime(int ticks, Level level, BlockPos pos, BlockState state,
            BrewingStandBlockEntity stand) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ticks;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        return Features.active(Feature.BREW_TIME_THIRTY_SECONDS, profile) ? 600 : ticks;
    }
}

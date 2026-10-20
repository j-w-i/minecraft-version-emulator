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

import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.historicalexperience.gameplay.HistoricalServerProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * A cake has SIX slices in every declared build, not seven.
 *
 * <p>minecraft.wiki, Cake history: Beta 1.8 Pre-release "Cake now restores 12 hunger and its slice restores 2"; 14w27a
 * "Cake slices available when eating cakes have been increased from 6 to 7, restoring 14 in total." Every declared
 * version is older than 14w27a, so every one of them has six slices: 12 hunger in the hunger era (2 each) and 18 health
 * before it (3 each, which the direct-heal path already does).
 *
 * <p>Only the comparison in {@code eat} is changed. The blockstate property still ranges over 0..6 - shrinking that
 * would break the state definition and any saved cake in an existing world - so the sixth bite simply consumes the
 * block instead of moving the property to 6.
 */
@Mixin(CakeBlock.class)
public abstract class CakeBlockHistoricalSlicesMixin {
    @ModifyConstant(method = "eat", constant = @Constant(intValue = 6))
    private static int historicalExperience$sixSlices(int modernMaxBites) {
        return HistoricalFoodRules.cakeBitesMax(HistoricalServerProfile.active());
    }
}

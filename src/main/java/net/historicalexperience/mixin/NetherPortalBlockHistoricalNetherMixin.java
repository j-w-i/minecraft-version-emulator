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
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops travel through a nether portal in versions that had no Nether.
 *
 * <p>The companion to refusing to CREATE a portal. That hook covers every activation path, but it cannot cover a
 * portal block that already exists - one written into a save before the gate existed, placed by a command, or
 * carried over from an ordinary world - and a version with no Nether must not send a player there by any route.
 *
 * <p>{@code entityInside} is the single point where an entity entering a portal block is handed to the dimension
 * transition, so refusing there closes the last door without touching the transition code itself.
 *
 * <p>A shared mixin: travel is a server decision, and the client's copy of the block runs the same method.
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockHistoricalNetherMixin {
    /** Vanilla: {@code entityInside(BlockState, Level, BlockPos, Entity)}. */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refusePortalTravel(BlockState state, Level level, BlockPos pos, Entity entity,
            CallbackInfo callback) {
        if (!HistoricalGameplayTweaks.netherAvailable(HistoricalActiveSession.current())) {
            callback.cancel();
        }
    }
}

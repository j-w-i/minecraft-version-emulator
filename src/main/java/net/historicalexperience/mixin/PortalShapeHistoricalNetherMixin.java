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

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a nether portal being lit in versions that had no Nether.
 *
 * <p>The owner asked: "I assume you've already blocked nether portals until alpha 1.2". It was not blocked. The block
 * veto runs on the generation path, so it prevents netherrack, glowstone and portal blocks from being GENERATED, and
 * does nothing about a player building obsidian and lighting it - which worked in an Alpha 1.0.1 world.
 *
 * <h2>Why this is the right single hook</h2>
 *
 * <p>{@code PortalShape.findEmptyPortalShape} is where EVERY activation path asks "is there a valid portal frame
 * here": flint and steel through {@code FlintAndSteelItem.useOn}, and fire through {@code BaseFireBlock.onPlace}.
 * Returning empty means the frame is never completed by any of them, rather than patching each item in turn and
 * missing the next one - and fire spread, dispensers and any future path are covered by construction.
 *
 * <p>A shared mixin: the client predicts the same action, and a client that lit a portal the server refused would
 * show a frame the world does not contain.
 */
@Mixin(PortalShape.class)
public abstract class PortalShapeHistoricalNetherMixin {
    /** Vanilla: {@code findEmptyPortalShape(LevelAccessor, BlockPos, Direction.Axis)}. */
    @Inject(method = "findEmptyPortalShape", at = @At("HEAD"), cancellable = true)
    private static void historicalExperience$refusePortalFrame(LevelAccessor level, BlockPos pos, Direction.Axis axis,
            CallbackInfoReturnable<java.util.Optional<PortalShape>> callback) {
        if (!HistoricalGameplayTweaks.netherAvailable(ActiveVersion.current())) {
            callback.setReturnValue(java.util.Optional.empty());
        }
    }
}

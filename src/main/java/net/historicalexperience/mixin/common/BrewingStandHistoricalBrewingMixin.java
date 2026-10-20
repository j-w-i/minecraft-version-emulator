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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a brewing stand brewing in versions that had no functional brewing.
 *
 * <p>The historical boundary is Beta 1.9 Prerelease 3: before it, Prerelease 2's brewing existed only as a
 * cauldron prototype with no ordinary block ID, and Beta 1.8 had no brewing at all. So a Beta 1.8 world must not
 * turn water and nether wart into potions, however the player obtained a stand.
 *
 * <p>The hook is the brew itself rather than the stand's menu or its fuel, because the brew is the effect that
 * matters: refusing it leaves the ingredients untouched, which is what "brewing does not work" should look like.
 * Cancelling fuel consumption or the interface instead would leave a stand that appears to work and quietly does
 * nothing.
 *
 * <p>The version comes from the active session, not from the level. A brewing stand in a historical world's
 * Nether would otherwise resolve no version at all, because the marker lookup reads the overworld generator - the
 * documented reason session-gated behaviour exists in this project.
 *
 * <p>A shared mixin: brewing happens on the server.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandHistoricalBrewingMixin {
    /** Vanilla: the static brew step, called when the timer elapses. */
    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void historicalExperience$refuseBrewing(Level level, BlockPos pos, NonNullList<ItemStack> items,
            CallbackInfo callback) {
        if (Features.active(Feature.NO_BREWING)) {
            callback.cancel();
        }
    }
}

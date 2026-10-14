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

import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuses to write a block the active historical version could not have had, whatever is trying to write it.
 *
 * <h2>Why this exists</h2>
 *
 * <p>The owner played an Alpha 1.0.1 world and found sugar cane, which arrives in Alpha 1.0.11, and then asked the
 * right question: "you need to enforce each version has all content present in later stuff are not present in older
 * versions". The veto already existed and was already version-aware - it consults the dated availability table - but
 * it was installed in exactly ONE place, the tree-decorator context. So it caught what tree decorators place, such
 * as vines and grass, and missed everything placed by any other feature: sugar cane, clay, ore blobs, disks, lakes,
 * and structure blocks. Every miss needed its own hand-written exclusion, discovered by playing.
 *
 * <p>Every one of those paths writes through {@code WorldGenRegion.setBlock}, which is why this single hook replaces
 * the guessing. It is a veto BEFORE the write, not a generate-then-remove step - which this project forbids, and
 * which would also leave the block briefly present for anything observing the chunk.
 *
 * <p>Returning {@code false} is the honest answer: the block was not set. Vanilla's own callers treat that as "no
 * change", and a feature that ignores the result - most do - leaves the chunk without the block rather than with it.
 *
 * <p>The decision itself is unchanged and still comes from {@code HistoricalBlockContent.isAnachronistic}, so an
 * undated block is never vetoed: no date means no claim, and this hook only ever enforces claims the research has
 * already made.
 *
 * <p>A shared mixin: generation happens on the server.
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionHistoricalBlockVetoMixin {
    /** Vanilla: {@code setBlock(BlockPos, BlockState, int flags, int recursionLeft)}. */
    @Inject(
        method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void historicalExperience$vetoAnachronisticBlock(BlockPos pos, BlockState state, int flags,
            int recursionLeft, CallbackInfoReturnable<Boolean> callback) {
        if (HistoricalFeatureExclusions.shouldSuppressBlock((WorldGenLevel) (Object) this, state)) {
            callback.setReturnValue(false);
        }
    }
}

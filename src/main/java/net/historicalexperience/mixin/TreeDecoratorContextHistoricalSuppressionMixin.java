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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a tree decorator writing a block that the active historical version never had, at the moment of
 * placement, so the block is <b>never generated</b>.
 *
 * <h2>Why this target</h2>
 *
 * <p>Two anachronisms are placed from inside tree features and cannot be expressed as placed-feature
 * exclusions, because excluding the feature would delete the tree itself:
 *
 * <ul>
 *   <li><b>Leaf litter</b> is a {@code PlaceOnGroundDecorator} on the tree, and on this Minecraft version
 *       Moderner Beta deliberately substitutes {@code TreePlacements.OAK_BEES_0002_LEAF_LITTER} for the
 *       pre-1.21.5 placement:
 *       <pre>//? if &gt;=1.21.5 { TreePlacements.OAK_BEES_0002_LEAF_LITTER; } else { ...OAK_BEES_0002; }</pre>
 *   <li><b>Vines</b> are written by the vanilla swamp tree's trunk and leaf decorators, which is why a
 *       Beta 1.7.3 world contained vines: vines are Release 1.6.4 content, so the version-agnostic check
 *       passed them, while Beta 1.8 is what introduced them.</li>
 * </ul>
 *
 * <p>{@code TreeDecorator.Context.setBlock} is the single funnel every decorator writes through - checked
 * in the 26.3 sources rather than assumed: {@code placeVine} itself is just
 * {@code setBlock(pos, VINE.defaultBlockState()...)}. So one hook covers both defects and any future one
 * of the same shape.
 *
 * <h2>What this is not</h2>
 *
 * <p>It is not "generate it and then remove it": the write never happens, so the block is absent from the
 * chunk as it is built. It also consumes no randomness, which matters because sampling a state provider to
 * decide would advance the world's random sequence and change every later placement in the chunk.
 */
@Mixin(TreeDecorator.Context.class)
public abstract class TreeDecoratorContextHistoricalSuppressionMixin {
    /**
     * The generation level the decoration is being written into. Shadowed rather than inherited, because
     * a mixin does not extend its target; the method exists on {@code TreeDecorator.Context} itself.
     * On 1.21.1 its descriptor returns LevelSimulatedReader, not WorldGenLevel. Leaf litter does not
     * exist on this runtime; vines and bee nests still need the generation-time policy.
     */
    @Shadow
    public abstract net.minecraft.world.level.LevelSimulatedReader level();

    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$suppressAnachronisticDecoration(
        BlockPos pos,
        BlockState state,
        CallbackInfo callback
    ) {
        if (this.level() instanceof net.minecraft.world.level.WorldGenLevel generationLevel
                && HistoricalFeatureExclusions.shouldSuppressBlock(generationLevel, state)) {
            callback.cancel();
        }
    }
}

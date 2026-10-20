/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalChestTweaks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws a chest as the old CUBE block model rather than as a block entity.
 *
 * <p>minecraft.wiki, Chest history, Beta 1.8 Pre-release: <i>"The chest's model has been redone. Rather than a usual
 * cube, it uses an entity model, which allows for an opening and closing animation, and is also smaller."</i> So the
 * cube is what every declared build below Beta 1.8 had.
 *
 * <p>{@code RenderShape.MODEL} means "this block state has a JSON model", and the model shipped under this project's
 * namespace is that cube. It only takes effect together with
 * {@code BlockEntityRenderDispatcherHistoricalChestMixin}, which removes the animated renderer that would otherwise
 * draw on top.
 *
 * <p>Two classes rather than one shared mixin: {@code getRenderShape} is declared {@code protected} on each block
 * separately, so a rule per block keeps each handler's target unambiguous and lets each one answer only for its own
 * chest.
 */
@Mixin(ChestBlock.class)
public abstract class ChestBlockHistoricalRenderShapeMixin {
    @ModifyReturnValue(method = "getRenderShape", at = @At("RETURN"), require = 1)
    private RenderShape historicalExperience$oldChestRenderShape(RenderShape shape, BlockState state) {
        return HistoricalChestTweaks.oldChest() ? RenderShape.MODEL : shape;
    }
}

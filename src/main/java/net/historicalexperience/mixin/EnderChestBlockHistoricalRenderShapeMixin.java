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
package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalChestTweaks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws an ender chest as the old CUBE block model rather than as a block entity.
 *
 * <p>Ported from Nostalgic Tweaks {@code EnderChestBlockMixin} {@code nt_chest_block$modifyRenderShape}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_ENDER_CHEST}.
 *
 * <p>This implements Nostalgic Tweaks' optional cube aesthetic, not authentic vanilla ender-chest geometry: the
 * ender chest existed as a block entity from Release 1.3.1, and was not historically a cube in these profiles.
 * {@code HistoricalChestTweaks.availableForEnderChest} gates this opt-in visual to versions where the block existed;
 * beta profiles remain excluded.
 */
@Mixin(EnderChestBlock.class)
public abstract class EnderChestBlockHistoricalRenderShapeMixin {
    @ModifyReturnValue(method = "getRenderShape", at = @At("RETURN"), require = 1)
    private RenderShape historicalExperience$oldEnderChestRenderShape(RenderShape shape, BlockState state) {
        return HistoricalChestTweaks.oldEnderChest() ? RenderShape.MODEL : shape;
    }
}

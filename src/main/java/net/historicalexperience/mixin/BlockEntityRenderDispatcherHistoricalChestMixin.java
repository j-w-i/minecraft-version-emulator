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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops the chest block entities from rendering, so the old CUBE model is drawn instead of the animated one.
 *
 * <p>Ported from Nostalgic Tweaks {@code BlockEntityRenderDispatcherMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <h2>Why the renderer is removed rather than replaced</h2>
 *
 * <p>The modern chest is not a cube: minecraft.wiki, Chest history, Beta 1.8 Pre-release - <i>"The chest's model has
 * been redone. Rather than a usual cube, it uses an entity model, which allows for an opening and closing animation,
 * and is also smaller."</i> That entity renderer draws the modern shape, so the old appearance needs it OUT of the
 * way and the block model used in its place - which is what {@code ChestBlockHistoricalRenderShapeMixin} arranges.
 * Upstream does the same pair, in the same order.
 *
 * <p>{@code equals} on the class is deliberate and is upstream's choice: a subclass of {@code ChestBlockEntity}
 * someone else adds should keep its own renderer rather than silently becoming a cube here.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherHistoricalChestMixin {
    @ModifyReturnValue(method = "getRenderer", at = @At("RETURN"), require = 1)
    private <E extends BlockEntity> BlockEntityRenderer<E> historicalExperience$noChestRenderer(
            @Nullable BlockEntityRenderer<E> renderer, E entity) {
        if (HistoricalChestTweaks.oldChest() && entity.getClass().equals(ChestBlockEntity.class)) {
            return null;
        }
        if (HistoricalChestTweaks.oldEnderChest() && entity.getClass().equals(EnderChestBlockEntity.class)) {
            return null;
        }
        return renderer;
    }
}

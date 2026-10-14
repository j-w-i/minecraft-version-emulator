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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalChestTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Selects the cube model for chest items only while the client's current world uses the historical chest appearance.
 *
 * <p>The item model resources themselves stay vanilla and are baked without consulting a world's version. The cube
 * models are already baked as blockstate models (which are harmless in ordinary worlds because those blocks retain
 * {@code RenderShape.ENTITY}); reusing those baked models here also avoids global item-model replacement and keeps
 * normal, trapped, and ender chest items paired with their corresponding block textures.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererHistoricalChestMixin {
    @ModifyReturnValue(method = "getModel", at = @At("RETURN"), require = 1)
    private BakedModel historicalExperience$oldChestItemModel(
            BakedModel vanillaModel,
            ItemStack stack,
            @Nullable Level level,
            @Nullable LivingEntity entity,
            int seed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || (level != null && level != minecraft.level)) {
            return vanillaModel;
        }

        Block block;
        if (stack.is(Items.CHEST) && HistoricalChestTweaks.oldChest()) {
            block = Blocks.CHEST;
        } else if (stack.is(Items.TRAPPED_CHEST) && HistoricalChestTweaks.oldChest()) {
            block = Blocks.TRAPPED_CHEST;
        } else if (stack.is(Items.ENDER_CHEST) && HistoricalChestTweaks.oldEnderChest()) {
            block = Blocks.ENDER_CHEST;
        } else {
            return vanillaModel;
        }

        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        BlockState state = block.defaultBlockState();
        return dispatcher.getBlockModelShaper().getBlockModel(state);
    }
}

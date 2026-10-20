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

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops an eye of ender completing a portal in versions where the portal did nothing.
 *
 * <p>Beta 1.9 Prerelease 3 adds the stronghold portal room, the eye of ender and the frame and portal assets, and
 * the portal block has no function: the research records that frame repair has "no further result". Prerelease 4
 * activates it.
 *
 * <p>The hook is the ITEM, not the frame block: in 1.21.1 the frame has no use method at all - placing an eye is
 * handled by {@code EnderEyeItem.useOn}. Hooking the block would have produced a mixin that never fires.
 *
 * <p>{@code PASS} rather than {@code FAIL}: it is what vanilla returns when an item simply has nothing to do here,
 * so the eye is not consumed, no sound plays, and the frame stays empty. Returning FAIL would tell the client the
 * interaction was rejected, which is a different message to the player than "this does nothing yet".
 *
 * <p>The version comes from the active session rather than the level, so a portal in any dimension of a historical
 * world is covered.
 *
 * <p>A shared mixin.
 */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeHistoricalPortalMixin {
    /** Vanilla: {@code useOn(UseOnContext)} places the eye into a frame and completes the portal. */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refusePortalCompletion(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (Features.active(Feature.END_FRAME_EYE_REPAIR)) {
            // Prerelease 3: the eye goes in and nothing further happens. Vanilla's useOn would fill the frame AND,
            // if the ring completed, light the portal - so the fill is reproduced here without the activation.
            net.minecraft.world.level.Level level = context.getLevel();
            net.minecraft.core.BlockPos pos = context.getClickedPos();
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (state.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME)
                    && !state.getValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE)) {
                level.setBlock(pos, state.setValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE, true), 3);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.END_PORTAL_FRAME_FILL,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                net.minecraft.world.entity.player.Player player = context.getPlayer();
                if (player == null || !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                callback.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide()));
                return;
            }
        }

        if (Features.active(Feature.INERT_END_PORTAL)) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}

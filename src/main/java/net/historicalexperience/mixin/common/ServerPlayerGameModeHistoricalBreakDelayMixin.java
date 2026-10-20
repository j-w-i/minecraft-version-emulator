/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalBlockBreakDelay;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The five-tick inter-block delay before Release 1.3.1.
 *
 * <p>minecraft.wiki, Java Edition 1.3.1: "The 1/4 second (5 game tick) delay between block breaking has been removed
 * for players in Survival." The decision and the per-player tick bookkeeping are in
 * {@link HistoricalBlockBreakDelay}, which is pure and unit-tested; this hook only supplies the game time and calls it.
 *
 * <p>Refused at HEAD and recorded at RETURN only when the break actually happened, so a failed break does not start the
 * cooldown. Creative players are untouched, and so is every version from Release 1.3 on.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeHistoricalBreakDelayMixin {
    @Shadow protected ServerPlayer player;

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseTooSoon(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (player == null || !((ServerPlayerGameMode) (Object) this).isSurvival()) {
            return;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(player.serverLevel());
        long now = player.level().getGameTime();
        if (HistoricalBlockBreakDelay.shouldRefuse(profile, true, now,
                HistoricalBlockBreakDelay.lastBreak(player.getUUID()))) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private void historicalExperience$recordBreak(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValueZ() && player != null) {
            HistoricalBlockBreakDelay.recordBreak(player.getUUID(), player.level().getGameTime());
        }
    }
}

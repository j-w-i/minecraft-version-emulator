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
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * "Can this position see the sky" under the round-robin engine.
 *
 * <p>Ported from Nostalgic Tweaks {@code BlockAndTintGetterMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Vanilla
 * answers {@code canSeeSky} with {@code getBrightness(SKY, pos) >= maxLightLevel}. Under this engine the sky light at
 * an open position is the TIME-OF-DAY value - 4 at midnight - so that test would say "no" for every open position
 * every night, and grass, rain and snow rendering that depend on it would all turn off at dusk. Upstream compares
 * against what an open position reads right now instead.
 *
 * <p>A default method on an interface, so the handler is itself a {@code default} method.
 */
@Mixin(BlockAndTintGetter.class)
public interface BlockAndTintGetterHistoricalMixin {
    @ModifyReturnValue(method = "canSeeSky", at = @At("RETURN"), require = 1)
    default boolean historicalExperience$canSeeSkyUnderOldLighting(boolean canSeeSky, BlockPos pos) {
        if (!HistoricalOldLighting.activeNow()) {
            return canSeeSky;
        }
        // The integrated server's level is also a BlockAndTintGetter; its answer drives gameplay and must stay real.
        if (!(((Object) this) instanceof ClientLevel level) || !Minecraft.getInstance().isSameThread()) {
            return canSeeSky;
        }
        return level.getBrightness(LightLayer.SKY, pos)
            >= HistoricalOldLighting.combined(level, level.getMaxLightLevel(), 0);
    }
}
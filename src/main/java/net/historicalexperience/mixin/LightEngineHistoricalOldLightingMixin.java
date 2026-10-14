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
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Old lighting for the vanilla renderer: the value the OLD engine would have stored at a position.
 *
 * <p>Ported from Nostalgic Tweaks {@code LightEngineMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The rule itself -
 * sky light following the time of day, grey light and no flicker - lives in {@link HistoricalOldLighting}.
 *
 * <p><b>This class used to carry the Classic engine as well, and no longer does.</b> Classic is Minecraft before
 * Infdev, and the oldest version this project declares is Infdev 20100420, so a Classic engine could never apply to
 * any world here. The owner removed it for that reason. What remains is the pre-Beta-1.8 round-robin engine, which
 * every early declared build really did have.
 *
 * <p>Sodium does not call this method - see {@code HistoricalDataLayer} and
 * {@code LayerLightSectionStorageHistoricalMixin} for the path it does read.
 *
 * <p>A client-only mixin, listed in the {@code client} section of the config: naming a client class in the common
 * section crashes a dedicated server at boot.
 *
 * <p>{@code require = 1} on purpose: without it a wrong target is a silent no-op and lighting quietly stays modern.
 */
@Mixin(LightEngine.class)
public abstract class LightEngineHistoricalOldLightingMixin {
    @Shadow
    protected net.minecraft.world.level.chunk.LightChunkGetter chunkSource;

    @ModifyReturnValue(method = "getLightValue", at = @At("RETURN"), require = 1)
    private int historicalExperience$oldLightValue(int lightValue, BlockPos blockPos) {
        if (!HistoricalOldLighting.activeNow()) {
            return lightValue;
        }
        // The integrated SERVER also owns a light engine over a different chunk source, and its values drive mob
        // spawning. Only the client's own engine is old-lit.
        if (!(this.chunkSource instanceof ClientChunkCache)) {
            return lightValue;
        }
        if (!(this.chunkSource.getLevel() instanceof ClientLevel level)) {
            return lightValue;
        }
        LightLayer layer = ((Object) this) instanceof SkyLightEngine ? LightLayer.SKY : LightLayer.BLOCK;
        return HistoricalOldLighting.transform(level, layer, blockPos, lightValue);
    }
}
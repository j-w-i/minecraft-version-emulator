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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.NativeImage;
import net.historicalexperience.client.lighting.HistoricalLightmap;
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The old grey lightmap: no tint, sky light that follows the time of day, and no flicker.
 *
 * <p>Ported from Nostalgic Tweaks {@code LightTextureMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The Classic
 * two-level lightmap that used to be applied from this class is gone: Classic is Minecraft before Infdev and no
 * declared version is that old.
 *
 * <h2>Why the pixel loop is neutralised before the texture is replaced</h2>
 *
 * <p>Vanilla fills 256 lightmap pixels and then uploads. Replacing the image just before the upload works, but would
 * leave the game doing the full computation first. The {@code 16} constant inside the vanilla loop is turned into 0
 * while the old engine is active, which makes that loop run zero times - the same optimisation upstream uses, and the
 * reason for the slice: there is more than one {@code 16} in the method, and only the one between the flicker read
 * and the first {@code getBrightness} call is the loop bound.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureHistoricalMixin {
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private float blockLightRedFlicker;

    @Shadow
    protected abstract float getDarknessGamma(float partialTick);

    @Shadow
    protected abstract float calculateDarknessScale(LivingEntity entity, float gamma, float partialTick);

    /** Light did not flicker before the Beta 1.8 Pre-release ("Artificial light now gives a subtle flicker"). */
    @Inject(method = "tick", at = @At("RETURN"), require = 1)
    private void historicalExperience$noFlicker(CallbackInfo callback) {
        if (HistoricalOldLighting.activeNow()) {
            this.blockLightRedFlicker = 0.0F;
        }
    }

    @ModifyExpressionValue(
        method = "updateLightTexture",
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LightTexture;blockLightRedFlicker:F"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LightTexture;getBrightness("
                + "Lnet/minecraft/world/level/dimension/DimensionType;I)F")),
        at = @At(value = "CONSTANT", args = "intValue=16"),
        require = 1)
    private int historicalExperience$skipVanillaLightmapLoop(int bound) {
        return HistoricalOldLighting.activeNow() ? 0 : bound;
    }

    @Inject(
        method = "updateLightTexture",
        at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"),
        require = 1)
    private void historicalExperience$oldLightmap(float partialTick, CallbackInfo callback) {
        if (!HistoricalOldLighting.activeNow()) {
            return;
        }
        float scale = this.minecraft.options.darknessEffectScale().get().floatValue();
        float gamma = this.getDarknessGamma(partialTick) * scale;
        float darkness = this.calculateDarknessScale(this.minecraft.player, gamma, partialTick) * scale;
        HistoricalLightmap.applyGrayscale(this.lightPixels, darkness, partialTick);
    }

    /** Alpha-to-Beta-1.7 brightness falls by 20% a level; the modern curve differs, so the old one is restored. */
    @ModifyReturnValue(method = "getBrightness", at = @At("RETURN"), require = 1)
    private static float historicalExperience$oldBrightnessCurve(float brightness, DimensionType dimension, int level) {
        if (!HistoricalOldLighting.activeNow()) {
            return brightness;
        }
        return Mth.lerp(dimension.ambientLight(), (float) Math.pow(0.8D, 15.0D - level), 1.0F);
    }
}

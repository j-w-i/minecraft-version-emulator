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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.historicalexperience.client.lighting.HistoricalFogTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NT world_fog/FogRendererMixin sunrise-fog hook and OverworldFogRenderer distance selection.
 * LGPL-3.0; see THIRD_PARTY_NOTICES.md. World gating replaces upstream's global enum.
 * Fluid, blindness/darkness and dimension-specific fog retain the game's overrides.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererHistoricalWorldMixin {
    @ModifyExpressionValue(method = "setupColor", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"), require = 1)
    private static float[] historicalExperience$oldSunriseFog(float[] color) {
        return HistoricalFogTweaks.oldSunriseFog() ? null : color;
    }

    @Inject(method = "setupFog", at = @At("TAIL"), require = 1)
    private static void historicalExperience$oldWorldFog(Camera camera, FogRenderer.FogMode mode,
            float farPlaneDistance, boolean foggy, float partialTick, CallbackInfo callback) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.level.dimension() != Level.OVERWORLD || foggy
                || !HistoricalFogTweaks.worldFog() || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }
        if (camera.getEntity() instanceof LivingEntity entity
                && (entity.hasEffect(MobEffects.BLINDNESS) || entity.hasEffect(MobEffects.DARKNESS))) {
            return;
        }
        int distance = client.options.getEffectiveRenderDistance();
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(mode == FogRenderer.FogMode.FOG_TERRAIN
            ? HistoricalFogTweaks.terrainEnd(distance) : HistoricalFogTweaks.skyEnd(distance));
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
}

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
import com.mojang.blaze3d.systems.RenderSystem;
import net.historicalexperience.client.lighting.HistoricalFogTweaks;
import net.historicalexperience.client.lighting.HistoricalStars;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Direct port of NT mixin/tweak/candy/world_sky/LevelRendererMixin
 * setStarWidth/setStarHeight/onSetupStarColor (LGPL-3.0; THIRD_PARTY_NOTICES.md).
 * VoidFogRenderer's separate star-opacity adjustment is pending with that renderer.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererHistoricalStarsMixin {
    @Shadow private ClientLevel level;

    @ModifyExpressionValue(method = "drawStars", at = @At(value = "CONSTANT", args = "floatValue=0.15F"), require = 1)
    private float historicalExperience$oldStarWidth(float width) {
        return HistoricalStars.width(HistoricalStars.style(), width);
    }

    @ModifyExpressionValue(method = "drawStars", at = @At(value = "CONSTANT", args = "floatValue=0.1F"), require = 1)
    private float historicalExperience$oldStarHeight(float height) {
        return HistoricalStars.width(HistoricalStars.style(), height);
    }

    @Inject(method = "renderSky", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/FogRenderer;setupNoFog()V"), require = 1)
    private void historicalExperience$oldStarColour(Matrix4f view, Matrix4f projection, float partialTick,
            Camera camera, boolean foggy, Runnable setupFog, CallbackInfo callback) {
        if (this.level == null || this.level.dimension() != Level.OVERWORLD) {
            return;
        }
        HistoricalStars.Style style = HistoricalStars.style();
        if (style != HistoricalStars.Style.MODERN) {
            float rain = 1.0F - this.level.getRainLevel(partialTick);
            float transparency = this.level.getStarBrightness(partialTick) * rain;
            float colour = HistoricalStars.colour(style, transparency);
            RenderSystem.setShaderColor(colour, colour, colour, transparency);
        }
        if (HistoricalFogTweaks.worldFog() && Minecraft.getInstance().options.getEffectiveRenderDistance() <= 4) {
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }
}

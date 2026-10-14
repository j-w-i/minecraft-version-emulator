package net.historicalexperience.client.lighting;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * The two old lightmaps: Classic's two-level one and the grey one used from Alpha to the Beta 1.8 Pre-release.
 *
 * <p>Ported from Nostalgic Tweaks {@code LightTextureHelper.setClassicTexture} and {@code setGrayscaleTexture}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The grey one is the "no tint" half of the Beta 1.8 Pre-release entry: before
 * it a block's light was a single brightness, afterwards it carried a colour from the dominant source.
 *
 * <p>Upstream also interpolates every pixel over a second so a lightmap that jumps does not flash. That is left out
 * deliberately: it is switched off whenever round-robin relighting is on (upstream's own
 * {@code SMOOTH_LIGHT_TRANSITION && !ROUND_ROBIN_RELIGHT}), and this project's old lighting always IS the round-robin
 * kind, so the interpolator could never run.
 */
public final class HistoricalLightmap {
    private HistoricalLightmap() {
    }

    /** Classic: bright at sky or block light 14 and up, a flat 60% grey otherwise. */
    public static void applyClassic(NativeImage lightPixels) {
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                lightPixels.setPixelRGBA(block, sky, sky >= 14 || block >= 14 ? 0xFFFFFFFF : 0xFF999999);
            }
        }
    }

    /**
     * Grey lightmap for the round-robin engine.
     *
     * @param darknessEffect the Darkness effect's pulse, as the game's own lightmap computes it
     */
    public static void applyGrayscale(NativeImage lightPixels, float darknessEffect, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        double gamma = minecraft.options.gamma().get();
        float darkenAmount = minecraft.gameRenderer.getDarkenWorldAmount(partialTick);
        float waterVision = player.getWaterVision();
        float potionEffect = player.hasEffect(MobEffects.NIGHT_VISION)
            ? GameRenderer.getNightVisionScale(player, partialTick)
            : (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER) ? waterVision : 0.0F);

        boolean flash = level.getSkyFlashTime() > 0 && !minecraft.options.hideLightningFlash().get();

        // Round robin: the stored sky light already carries the time of day, so nothing is subtracted for it.
        int skyLightSubtracted = 0;
        if (flash) {
            skyLightSubtracted = 1;
        } else if (darkenAmount > 0) {
            skyLightSubtracted = Mth.clamp(skyLightSubtracted + (int) Math.ceil(3 * darkenAmount), 1, 15);
        }

        float ambientLight = level.dimensionType().ambientLight();
        float minSkyLight = HistoricalOldLightMath.lightmapBrightness(15, true, true, gamma, true) * ambientLight;
        float minBlockLight = HistoricalOldLightMath.lightmapBrightness(15, false, true, gamma, true) * ambientLight;

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                float fromBlockLight = Math.max(
                    HistoricalOldLightMath.lightmapBrightness(x, false, true, gamma, true), minBlockLight);
                float fromSkyLight = Math.max(HistoricalOldLightMath.lightmapBrightness(
                    Math.max(y - skyLightSubtracted, 0), true, true, gamma, true), minSkyLight);

                if (level.dimension() == Level.END) {
                    fromSkyLight = 0.22F + fromSkyLight * 0.75F;
                }
                if (level.dimension() == Level.NETHER) {
                    fromSkyLight = 0.05F + fromSkyLight * 0.75F;
                }

                if (potionEffect > 0.0F) {
                    float shift = potionEffect * 0.7F;
                    fromBlockLight += ((1.0F - fromBlockLight - 0.5F) * (shift * fromBlockLight)) + (0.5F * potionEffect);
                    fromSkyLight += ((1.0F - fromSkyLight - 0.5F) * (shift * fromSkyLight)) + (0.5F * potionEffect);
                }

                if (darknessEffect > 0.0F && x != 15) {
                    fromBlockLight = Mth.clamp(fromBlockLight - darknessEffect, 0.025F, 1.0F);
                    fromSkyLight = Mth.clamp(fromSkyLight - darknessEffect, 0.025F, 1.0F);
                }

                // Upstream's defaults: dynamic brightness on, so the gamma slider is not applied to the whole map.
                float skyLight = Mth.clamp(fromSkyLight * 255.0F, 6.375F, 255.0F);
                float blockLight = Mth.clamp(fromBlockLight * 255.0F, 6.375F, 255.0F);
                int light = Math.round(fromBlockLight > fromSkyLight ? blockLight : skyLight);

                lightPixels.setPixelRGBA(x, y, 255 << 24 | light << 16 | light << 8 | light);
            }
        }
    }
}

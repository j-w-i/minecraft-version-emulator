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
import com.mojang.blaze3d.systems.RenderSystem;
import mod.bluestaggo.modernerbeta.api.level.biome.climate.ClimateSamplerSky;
import mod.bluestaggo.modernerbeta.client.color.SkyColorSampler;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.client.lighting.HistoricalSkyColours;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.historicalexperience.version.HistoricalVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the biome's base sky colour for the active historical profile.
 *
 * <p>Targeting {@link Biome#getSkyColor()} rather than the finished {@code ClientLevel} sky vector is deliberate: the
 * level renderer keeps applying its normal day-brightness, rain, thunder and lightning processing on top, which is
 * exactly what the original clients did after choosing the colour (Beta 1.7.3 {@code fd.java:945} darkens the same
 * way). Replacing the finished vector would throw that away.
 *
 * <p>The colour itself no longer comes from Nostalgic Tweaks' era menu. Its Beta entry is a flat {@code 0x97A3FF}
 * which is not what any Beta client drew: Beta computed the sky from the world's temperature field. What is applied
 * here is measured from the clients - see {@link HistoricalWorldTweaks.SkyEra} and {@link HistoricalSkyColours}:
 *
 * <ul>
 *   <li><b>Infdev and Alpha v1.0.x</b>: the flat colour that era's client used.</li>
 *   <li><b>Beta 1.0 - Beta 1.8.1</b>: {@code getSkyColorByTemp} of the temperature at the player's position. The
 *       temperature comes from Moderner Beta's own port of the Beta climate noise
 *       ({@code SkyColorSampler}/{@code ClimateSamplerSky}), because recreating that noise here would mean a second
 *       generator, which this project does not do. Moderner Beta ships the same function for the sky as well, but on
 *       1.21.1 its injection writes the colour into the sampler's position argument, so the value never reaches the
 *       sky on its own; reading the sampler's temperature and applying Beta's function is both its data and the
 *       original arithmetic.</li>
 *   <li><b>Everything else, including ordinary worlds</b>: untouched.</li>
 * </ul>
 *
 * <p>The low-render-distance branch is Nostalgic Tweaks' ({@code SkyHelper.getOldColor}): under the old world fog at
 * four chunks or fewer the sky was fully fogged, so the fog colour is what should be seen.
 */
@Mixin(Biome.class)
public abstract class BiomeHistoricalSkyColourMixin {
    @ModifyReturnValue(method = "getSkyColor", at = @At("RETURN"), require = 1)
    private int historicalExperience$oldSkyColour(int original) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.level.dimension() != Level.OVERWORLD) {
            return original;
        }
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        if (!HistoricalWorldTweaks.skyTweakActive(version)) {
            return original;
        }
        if (RenderSystem.isOnRenderThread() && client.options.getEffectiveRenderDistance() <= 4) {
            float[] fog = RenderSystem.getShaderFogColor();
            return (int) (Mth.clamp(fog[0], 0.0F, 1.0F) * 255.0F) << 16
                | (int) (Mth.clamp(fog[1], 0.0F, 1.0F) * 255.0F) << 8
                | (int) (Mth.clamp(fog[2], 0.0F, 1.0F) * 255.0F);
        }
        Integer flat = HistoricalWorldTweaks.eraSkyColour(version);
        if (flat != null) {
            return flat;
        }
        if (HistoricalWorldTweaks.skyEra(version) == HistoricalWorldTweaks.SkyEra.TEMPERATURE) {
            Float temperature = historicalExperience$betaSkyTemperature(client);
            if (temperature != null) {
                return HistoricalSkyColours.temperatureSkyColour(temperature);
            }
        }
        return original;
    }

    /**
     * The era's sky temperature at the player's position, from Moderner Beta's Beta climate sampler, or {@code null}
     * when this world has none (an ordinary or non-Beta world), in which case the game's own colour is kept.
     *
     * <p>Beta took the temperature at the entity's floored x/z ({@code fd.java:938} in Beta 1.7.3); this does the
     * same for the client player.
     */
    private static Float historicalExperience$betaSkyTemperature(Minecraft client) {
        if (client.player == null) {
            return null;
        }
        SkyColorSampler sampler = SkyColorSampler.INSTANCE;
        if (sampler == null || !sampler.useSkyColor()) {
            return null;
        }
        ClimateSamplerSky climate = sampler.getClimateSampler();
        if (climate == null) {
            return null;
        }
        int x = Mth.floor(client.player.getX());
        int z = Mth.floor(client.player.getZ());
        return (float) climate.sampleSky(x, z);
    }
}

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
package net.historicalexperience.mixin.client;

import java.util.Set;
import mod.bluestaggo.modernerbeta.api.level.biome.climate.Clime;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.server.HistoricalServerContext;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.world.upgrade.BiomeCarryForward;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * For Beta 1.8.1 and later only: tints a converted chunk from the biome it stores.
 *
 * <h2>Why this is version-gated</h2>
 *
 * <p>Moderner Beta tints vegetation from its climate NOISE at each block, not from the biome. That is exactly right for
 * Alpha 1.2 through Beta 1.7.3, whose clients coloured grass per block from temperature and rainfall noise regardless
 * of any biome, and it must be left completely alone there or the authentic Alpha look is flattened. This mixin
 * therefore does nothing below Beta 1.8.1.
 *
 * <p>From Beta 1.8.1 the real game coloured grass from the BIOME's own temperature and rainfall. A converted chunk
 * stores one biome but sat under the provider's noise, so a uniform {@code late_beta_plains} rendered as a patchwork of
 * greens. For those versions the sample for a pre-existing chunk is the stored biome's own climate, which the era's
 * extracted colour map (installed by {@code GrassColorHistoricalColormapMixin}) turns into the colour that biome really
 * had. New chunks, and every position outside a recorded chunk, keep Moderner Beta's own colouring.
 *
 * <p>Client-side and colour-only: it runs in the tint code, never in generation.
 */
@Mixin(targets = "mod.bluestaggo.modernerbeta.client.color.block.BlockColorSampler", remap = false)
public abstract class ModernBetaClimateSamplerHistoricalMixin {
    @Inject(
        method = "sampleClime(II)Lmod/bluestaggo/modernerbeta/api/level/biome/climate/Clime;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0)
    private void historicalExperience$tintFromStoredBiome(int x, int z, CallbackInfoReturnable<Clime> callback) {
        Set<Long> preExisting = HistoricalServerContext.convertedChunks();
        if (preExisting.isEmpty() || !preExisting.contains(ChunkPos.asLong(x >> 4, z >> 4))) {
            return;
        }
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        BiomeCarryForward.Generation generation = BiomeCarryForward.generationFor(version);
        if (generation != BiomeCarryForward.Generation.LATE_BETA
            && generation != BiomeCarryForward.Generation.EARLY_RELEASE) {
            // Alpha through Beta 1.7.3: climate noise IS the authentic colouring. Leave it alone.
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        ChunkAccess chunk = client.level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        Biome biome = chunk.getNoiseBiome(x >> 2, 64 >> 2, z >> 2).value();
        Biome.ClimateSettings climate = ((BiomeClimateAccess) (Object) biome).historicalExperience$climateSettings();
        callback.setReturnValue(new Clime(
            Mth.clamp(climate.temperature(), 0.0F, 1.0F),
            Mth.clamp(climate.downfall(), 0.0F, 1.0F)));
    }
}
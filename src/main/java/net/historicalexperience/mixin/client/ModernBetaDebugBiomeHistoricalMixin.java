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
import net.historicalexperience.server.HistoricalServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Moderner Beta's own F3 line ("[Modern Beta] Extended biome: ...") agree with the chunk for a chunk that existed
 * before a biome conversion.
 *
 * <p>That line does not go through {@code getNoiseBiome}. {@code DebugEntryExtendedBiome} calls
 * {@code ChunkProviderForcedHeight.getExtendedBiomeId}, which asks the provider straight away, so after an upgrade the
 * Moderner Beta section named whatever the provider would generate there ("some random one") while the chunk section
 * named the converted biome.
 *
 * <p>This changes only what that debug line prints. It deliberately does NOT override {@code getBiomeForHeightGen}, which
 * the terrain generator also uses: a new chunk blends biome heights with its neighbours, so overriding the real lookup
 * would alter how fresh terrain meets converted chunks. Display only, so generation is untouched.
 */
@Mixin(targets = "mod.bluestaggo.modernerbeta.client.debug.entries.DebugEntryExtendedBiome", remap = false)
public abstract class ModernBetaDebugBiomeHistoricalMixin {
    @Inject(method = "getLine", at = @At("HEAD"), cancellable = true, require = 0)
    private static void historicalExperience$showStoredBiomeForPreExistingChunks(
            Level level, int x, int z, CallbackInfoReturnable<String> callback) {
        Set<Long> preExisting = HistoricalServerContext.convertedChunks();
        if (preExisting.isEmpty() || !preExisting.contains(ChunkPos.asLong(x >> 4, z >> 4))) {
            // Not converted, or not a chunk that existed beforehand: Moderner Beta prints its own answer.
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        // The client's copy of the chunk holds exactly what the chunk stores, which is what the chunk section of F3
        // reports. A y of 64 is the sea-level biome row Moderner Beta's own line samples.
        ChunkAccess chunk = client.level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, false);
        if (chunk == null) {
            return;
        }
        chunk.getNoiseBiome(x >> 2, 64 >> 2, z >> 2).unwrapKey().ifPresent(key ->
            callback.setReturnValue(String.format("[Modern Beta] Extended biome: %s", key.location())));
    }
}

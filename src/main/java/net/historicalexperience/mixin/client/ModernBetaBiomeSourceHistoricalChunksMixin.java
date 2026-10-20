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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Moderner Beta report the CONVERTED biome for chunks that existed before a conversion, and generate normally
 * for everything else.
 *
 * <h2>The owner's rule</h2>
 *
 * <blockquote>"Moderner beta should register ONLY THE CHUNKS GENERATED BEFOREHAND the way the convertor does and then
 * everything else follows as it usually would."</blockquote>
 *
 * <p>Moderner Beta answers every position from its provider, and its biome source holds only registries and settings -
 * no level and no chunk access - so it cannot ask whether a position belongs to a chunk that already existed. It has to
 * be told, and {@link net.historicalexperience.world.upgrade.BiomeConversionRecord} is what tells it: a conversion
 * records every chunk position it found in the region files, which is exactly "generated beforehand".
 *
 * <p>So a position inside a pre-existing chunk returns the biome the conversion wrote, and any other position falls
 * through to Moderner Beta's own provider - its usual behaviour for newly generated chunks.
 *
 * <h2>Why a mixin and not an edit to Moderner Beta</h2>
 *
 * <p>Moderner Beta is consumed as a published jar, and its checkout under {@code .upstream/} is a git-ignored reference,
 * so editing that checkout would change nothing at runtime. This achieves the same rule without forking a dependency.
 * The target is named by string and {@code remap = false} so our jar still loads if Moderner Beta is absent.
 */
@Mixin(targets = "mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource", remap = false)
public abstract class ModernBetaBiomeSourceHistoricalChunksMixin {
    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true, require = 0)
    private void historicalExperience$useConvertedBiomeForPreExistingChunks(
            int biomeX, int biomeY, int biomeZ, Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> callback) {
        Set<Long> preExisting = HistoricalServerContext.convertedChunks();
        if (preExisting.isEmpty()) {
            // No conversion recorded for this world, so Moderner Beta answers as it always would.
            return;
        }
        // Biome coordinates are quart coordinates: one chunk spans four of them on each horizontal axis.
        if (!preExisting.contains(ChunkPos.asLong(biomeX >> 2, biomeZ >> 2))) {
            // Not a chunk that existed beforehand, so it is being generated now and follows the normal path.
            return;
        }
        String target = HistoricalServerContext.convertedTarget();
        if (target == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(target);
        if (id == null) {
            return;
        }
        ((ModernBetaBiomeSourceAccess) this).historicalExperience$biomeRegistry()
            .get(ResourceKey.create(Registries.BIOME, id))
            .ifPresent(callback::setReturnValue);
    }
}

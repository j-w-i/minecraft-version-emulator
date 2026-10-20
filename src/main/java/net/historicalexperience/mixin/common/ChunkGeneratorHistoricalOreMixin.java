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
package net.historicalexperience.mixin.common;

import net.historicalexperience.generation.BetaOrePlacement;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places a historical revision's ore population after vanilla's biome decoration.
 *
 * <p>Why here and not through the biome: ore features belong to a biome's feature list, which is global
 * registry data, so they cannot be added for one world. Running the population for the chunk that is
 * being decorated is the narrow equivalent, and it is placed on the chunk that the vanilla method was
 * already given rather than on any ambient state.
 *
 * <p>Only the generator's own durable marker selects the population, so an ordinary world, a
 * non-historical Moderner Beta world and a world on a revision that places vanilla ores are all
 * untouched. The revision that enables this also excludes the vanilla ore features, so the two cannot
 * double up.
 *
 * <p>The config is {@code required}: if this hook failed to apply, a historical world would silently
 * fall back to vanilla ore placement, which is exactly the defect being fixed.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorHistoricalOreMixin {
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void historicalExperience$placeHistoricalOres(
        WorldGenLevel level,
        ChunkAccess chunk,
        StructureManager structureManager,
        CallbackInfo ci
    ) {
        BetaOrePlacement.placeForChunk((ChunkGenerator) (Object) this, level, chunk);
    }
}

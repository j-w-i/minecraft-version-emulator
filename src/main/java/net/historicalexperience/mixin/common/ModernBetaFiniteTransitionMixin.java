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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mod.bluestaggo.modernerbeta.api.level.chunk.ChunkProvider;
import mod.bluestaggo.modernerbeta.api.level.chunk.ChunkProviderNoise;
import net.historicalexperience.generation.FiniteTransitionRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Uses Moderner Beta's own density-modifier extension point, before aquifers/surface/features and block placement.
 * Verified against pinned 1.21.1 alpha.4 bytecode: modifier coordinates are noise-cell units, not blocks.
 * This is an upgrade collar, not a replacement terrain algorithm or a post-generation content scrubber.
 */
@Mixin(value = ChunkProviderNoise.class, remap = false)
public abstract class ModernBetaFiniteTransitionMixin {
    @Shadow protected int noiseResolutionHorizontal;
    @Shadow protected int noiseResolutionVertical;

    @ModifyReturnValue(method = "isDensityModified", at = @At("RETURN"), require = 1)
    private boolean historicalExperience$usesTransitionDensity(boolean original) {
        return original || FiniteTransitionRuntime.planFor(((ChunkProvider) (Object) this).getChunkGenerator()) != null;
    }

    @ModifyReturnValue(method = "modifyEdgeDensity", at = @At("RETURN"), require = 1)
    private double historicalExperience$blendFiniteCoast(double original, double density, double x, double y, double z) {
        var plan = FiniteTransitionRuntime.planFor(((ChunkProvider) (Object) this).getChunkGenerator());
        return plan == null ? original : plan.blend(original,
            (int) Math.floor(x * noiseResolutionHorizontal),
            (int) Math.floor(y * noiseResolutionVertical),
            (int) Math.floor(z * noiseResolutionHorizontal));
    }
}

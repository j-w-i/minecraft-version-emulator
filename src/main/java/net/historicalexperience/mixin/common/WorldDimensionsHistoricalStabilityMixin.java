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

import com.mojang.serialization.Lifecycle;
import mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource;
import net.historicalexperience.worldgen.HistoricalWorldStability;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a historical world being reported as an experimental one.
 *
 * <p>Vanilla's {@code WorldDimensions.bake} registers every dimension with {@code checkStability}, which reports
 * {@code experimental} for any stem that is not vanilla-like - and a historical world is never vanilla-like, because
 * its overworld dimension type is this mod's (the era's 128-block envelope) and its Nether is a single biome. The
 * client turns that into the "Worlds using Experimental Settings are not supported" confirmation on every single
 * load of every historical world, with an offer to back the world up first.
 *
 * <p>The value is overridden at its source rather than the screen being suppressed, because
 * {@code WorldData.worldGenSettingsLifecycle()} has exactly two consumers in 1.21.1 - that prompt and the server's
 * crash report - and both should describe our own worlds correctly. See
 * {@link HistoricalWorldStability} for the reasoning and the evidence it was read from.
 *
 * <p>An ordinary world is untouched: it has neither a dimension type in this mod's namespace nor a Moderner Beta
 * biome source, so the lifecycle vanilla computed is returned unchanged.
 */
@Mixin(WorldDimensions.Complete.class)
public abstract class WorldDimensionsHistoricalStabilityMixin {
    @Inject(method = "lifecycle", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$doNotCallHistoricalWorldsExperimental(
            CallbackInfoReturnable<Lifecycle> callback) {
        WorldDimensions.Complete complete = (WorldDimensions.Complete) (Object) this;
        LevelStem overworld = complete.dimensions().get(LevelStem.OVERWORLD);
        if (overworld == null) {
            // Without an overworld there is nothing historical to recognise, and the members a world does have are
            // judged by vanilla exactly as before.
            return;
        }
        ResourceKey<DimensionType> dimensionType = overworld.type().unwrapKey().orElse(null);
        boolean modernerBeta = overworld.generator().getBiomeSource() instanceof ModernBetaBiomeSource;
        if (HistoricalWorldStability.isHistoricalWorld(dimensionType, modernerBeta)) {
            callback.setReturnValue(Lifecycle.stable());
        }
    }
}

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

import net.historicalexperience.client.colour.HistoricalColormaps;
import net.minecraft.world.level.FoliageColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Installs the era's own foliage tint map, in the same way as {@link GrassColorHistoricalColormapMixin}.
 *
 * <p>The foliage map is the one table that actually changed inside the supported range: Alpha v1.2.0 through Beta 1.0
 * used one map and Beta 1.2 onwards another, which was measured from the clients rather than assumed.
 */
@Mixin(FoliageColor.class)
public abstract class FoliageColorHistoricalColormapMixin {
    @ModifyVariable(method = "init", at = @At("HEAD"), argsOnly = true, require = 1)
    private static int[] historicalExperience$useEraFoliageMap(int[] pixels) {
        // Report through the same signature the grass hook uses, so the two agree about the installed era.
        HistoricalColormaps.noteApplied(HistoricalColormaps.desiredEraSignature());
        int[] era = HistoricalColormaps.current("foliage");
        return era == null ? pixels : era;
    }
}

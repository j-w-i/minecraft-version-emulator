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
package net.historicalexperience.mixin;

import net.historicalexperience.client.colour.HistoricalColormaps;
import net.minecraft.world.level.GrassColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Installs the era's own grass tint map when the game loads its colour map.
 *
 * <p>{@code GrassColor} holds the 65536-entry table the whole client consults ({@code get(temp, rain)} and
 * {@code getDefaultColor()}); the reload listener hands the freshly loaded texture to {@link GrassColor#init}. Changing
 * the table here means every consumer follows the era - the block tint, the average-colour resolver and Moderner Beta's
 * own tint source alike - instead of each of them needing its own rule.
 *
 * <p>Nothing is changed when no historical world is active, or when the version predates colour maps, so ordinary
 * worlds and the release profiles keep exactly what they load.
 */
@Mixin(GrassColor.class)
public abstract class GrassColorHistoricalColormapMixin {
    @ModifyVariable(method = "init", at = @At("HEAD"), argsOnly = true, require = 1)
    private static int[] historicalExperience$useEraGrassMap(int[] pixels) {
        int[] era = HistoricalColormaps.current("grass");
        // Record what was installed either way: the reload reconciler compares against this, so a version whose era
        // needs no substitution must also say so, or it would ask for a reload on every check.
        HistoricalColormaps.noteApplied(HistoricalColormaps.eraFor(era));
        return era == null ? pixels : era;
    }
}

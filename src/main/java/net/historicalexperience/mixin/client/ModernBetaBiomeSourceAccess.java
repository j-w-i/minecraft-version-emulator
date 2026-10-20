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

import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Moderner Beta's biome registry, which its biome source keeps private.
 *
 * <p>{@link ModernBetaBiomeSourceHistoricalChunksMixin} needs it to turn a recorded biome name back into a
 * {@code Holder<Biome>}. Without it the mixin would have to go through the level's registry access, which the biome
 * source is never given.
 *
 * <p>Targeted by name rather than by class reference, and with {@code remap = false}, because Moderner Beta is consumed
 * as a published jar: naming its class in our own compiled code would make our jar refuse to load without it.
 */
@Mixin(targets = "mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource", remap = false)
public interface ModernBetaBiomeSourceAccess {
    @Accessor(value = "biomeRegistry", remap = false)
    HolderGetter<Biome> historicalExperience$biomeRegistry();
}

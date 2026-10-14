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

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.historicalexperience.client.textures.HistoricalTextureService;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Appends the historical texture layer to the list of packs the client reloads its resources from.
 *
 * <p>{@code PackRepository.openAllSelected()} is what {@code Minecraft.reloadResourcePacks} hands to the resource
 * manager, lowest priority first. Adding one entry at the end is the entire integration: the repository's selected
 * list, {@code options.txt} and the resource-pack screen are never touched, so the layer outranks the player's packs
 * for the resources it defines without any of them being reordered, deselected or rewritten, and disabling it leaves
 * nothing behind.
 *
 * <p>The same repository class is used for data packs on the integrated server, so the layer is added only when the
 * repository is the client's own. Dedicated servers do not load client classes and never reach this mixin.
 */
@Mixin(PackRepository.class)
public abstract class PackRepositoryHistoricalTexturesMixin {
    @ModifyReturnValue(method = "openAllSelected", at = @At("RETURN"), require = 1)
    private List<PackResources> historicalExperience$appendTextureLayer(List<PackResources> selected) {
        PackRepository self = (PackRepository) (Object) this;
        if (!HistoricalTextureService.isClientRepository(self)) {
            return selected;
        }
        PackResources layer = HistoricalTextureService.buildLayer(self);
        if (layer == null) {
            return selected;
        }
        return ImmutableList.<PackResources>builder().addAll(selected).add(layer).build();
    }
}

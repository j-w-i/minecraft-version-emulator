/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.mixin.client;

import net.historicalexperience.client.lighting.HistoricalChestTweaks;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Redirects vanilla chest-family blockstate resources to this mod's cube-model state definitions.
 *
 * <p>Ported from Nostalgic Tweaks {@code BlockStateModelLoaderMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). This
 * redirect is unconditional because resource reloads usually happen at the title screen, before any world's version
 * is known. Baking both models is safe: the runtime render-shape mixins request those models only in eligible
 * historical worlds, while ordinary worlds keep the vanilla entity renderer. The item renderer similarly selects a
 * cube model only for the active historical world. The hook is limited to the three vanilla resource ids.
 */
@Mixin(net.minecraft.client.resources.model.BlockStateModelLoader.class)
public abstract class BlockStateModelLoaderHistoricalChestMixin {
    @ModifyArg(
        method = "loadBlockStateDefinitions",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/resources/FileToIdConverter;idToFile("
                + "Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceLocation;"),
        require = 1)
    private ResourceLocation historicalExperience$oldChestBlockState(ResourceLocation location) {
        String path = location.getPath();
        // Resource reloads commonly happen at the title screen, when no world's version exists yet. Always bake
        // these models; ChestBlock/EnderChestBlock select MODEL only for an eligible active historical world.
        // Ordinary worlds retain ENTITY render shape and the vanilla item models.
        if (HistoricalChestTweaks.hasHistoricalBlockState(location.getNamespace(), path)) {
            return ResourceLocation.fromNamespaceAndPath("historical_experience", path);
        }
        return location;
    }
}

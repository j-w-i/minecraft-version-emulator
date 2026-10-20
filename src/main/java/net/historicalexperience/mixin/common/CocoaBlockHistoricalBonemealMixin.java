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
package net.historicalexperience.mixin.common;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * One bone meal fully grows cocoa, before Release 1.5. Ported from Nostalgic Tweaks {@code CocoaBlockMixin}
 * (LGPL-3.0); the boundary and its source are on {@link CropBlockHistoricalBonemealMixin}.
 *
 * <p>Cocoa was added in 1.3.1 (12w18a), so only the Release 1.3 and 1.4 profiles can ever reach this.
 */
@Mixin(CocoaBlock.class)
public abstract class CocoaBlockHistoricalBonemealMixin {
    @ModifyArg(
        method = "performBonemeal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
        index = 1)
    private BlockState historicalExperience$instantCocoaGrowth(BlockState state) {
        return Features.active(Feature.INSTANT_BONE_MEAL)
            ? state.setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE)
            : state;
    }
}

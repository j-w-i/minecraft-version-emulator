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
package net.historicalexperience.mixin;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * One bone meal fully grows a melon or pumpkin stem, before Release 1.5. Ported from Nostalgic Tweaks
 * {@code StemBlockMixin} (LGPL-3.0); the boundary and its source are on
 * {@link CropBlockHistoricalBonemealMixin}.
 *
 * <p>The target is the single {@code Math.min(II)I} in {@code performBonemeal}, verified against the 1.21.1 bytecode:
 * it caps the new age at {@code MAX_AGE}, so replacing its result with {@code MAX_AGE} skips the random 2-5 step.
 */
@Mixin(StemBlock.class)
public abstract class StemBlockHistoricalBonemealMixin {
    @ModifyExpressionValue(
        method = "performBonemeal",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int historicalExperience$instantStemGrowth(int age) {
        return Features.active(Feature.INSTANT_BONE_MEAL) ? StemBlock.MAX_AGE : age;
    }
}

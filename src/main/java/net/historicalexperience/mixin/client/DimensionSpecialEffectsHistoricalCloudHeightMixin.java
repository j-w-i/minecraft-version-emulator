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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Port of NT world_sky/DimensionSpecialEffectsMixin's cloud-height selection
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), with historical version selection.
 * Clouds were at 108 before Beta 1.8, then 128 throughout our remaining catalogue.
 * Dimensions without clouds retain their original non-finite height.
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsHistoricalCloudHeightMixin {
    @ModifyReturnValue(method = "getCloudHeight", at = @At("RETURN"), require = 1)
    private float historicalExperience$oldCloudHeight(float cloudHeight) {
        return HistoricalWorldTweaks.cloudHeightOverride(cloudHeight);
    }
}

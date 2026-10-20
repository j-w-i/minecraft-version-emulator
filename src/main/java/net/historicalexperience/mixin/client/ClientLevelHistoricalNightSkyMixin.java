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

import net.historicalexperience.client.lighting.HistoricalStars;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Direct port of NT world_sky/ClientLevelMixin.setMinNightSkyColor (LGPL-3.0).
 * Its ALPHA/BETA star styles use a 0.005 minimum instead of modern black.
 * Both predate Release 1.3.1's smaller/brighter stars. No final sky-vector replacement.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelHistoricalNightSkyMixin {
    @ModifyArg(method = "getSkyColor", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/Mth;clamp(FFF)F"), index = 1, require = 1)
    private float historicalExperience$oldNightMinimum(float minimum) {
        if (((ClientLevel) (Object) this).dimension() != Level.OVERWORLD) {
            return minimum;
        }
        return HistoricalStars.oldStars() ? 0.005F : minimum;
    }
}

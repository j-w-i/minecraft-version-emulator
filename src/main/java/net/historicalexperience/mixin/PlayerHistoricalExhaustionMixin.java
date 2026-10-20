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

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Restores Beta 1.8's exhaustion costs, which are several times the modern ones.
 *
 * <p>From research/historical-features.json, entry {@code beta18-exhaustion-action-costs} (confidence: high):
 * attacking costs 0.3 rather than 0.1, and a jump while airborne costs 0.2 rather than 0.05. Nostalgic Tweaks has
 * no equivalent, so this is from the research.
 *
 * <p>The sprint jump is era-dependent and handled separately: the research dates it at 0.8 in Beta 1.8, 0.2 from
 * Release 1.3.1 and back to 0.8 from Release 1.7.2, so Release 1.6.4 - the only declared profile inside the 0.2
 * window - keeps the modern value while the five earlier hunger profiles get 0.8.
 *
 * <p>Constants are matched by value, not by ordinal, wherever the values distinguish them: a value is a fact about
 * the method and an ordinal is a count that has already been wrong once in this project. An ambiguous value is
 * fatal at transformation, and {@code Player} is one of the classes the generation probe now loads, so such a
 * mistake is caught by an automated run.
 *
 * <p>Gated on the hunger boundary: exhaustion only exists where a hunger meter does.
 *
 * <p>A shared mixin: exhaustion is server-side player state.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalExhaustionMixin {
    /** Upstream has no equivalent. Attacking: 0.3 instead of 0.1. */
    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 0.1F))
    private float historicalExperience$attackExhaustion(float exhaustion) {
        return exhaustsLikeBeta() ? 0.3F : exhaustion;
    }

    /** Upstream has no equivalent. A non-sprint jump: 0.2 instead of 0.05. */
    @ModifyConstant(method = "jumpFromGround", constant = @Constant(floatValue = 0.05F))
    private float historicalExperience$jumpExhaustion(float exhaustion) {
        return exhaustsLikeBeta() ? 0.2F : exhaustion;
    }

    /** Upstream has no equivalent. A sprint jump: 0.8 before Release 1.6.4, modern from it. */
    @ModifyConstant(method = "jumpFromGround", constant = @Constant(floatValue = 0.2F))
    private float historicalExperience$sprintJumpExhaustion(float exhaustion) {
        return HistoricalGameplayTweaks.betaSprintJumpExhaustion(ActiveVersion.current())
            ? 0.8F
            : exhaustion;
    }

    private static boolean exhaustsLikeBeta() {
        return HistoricalGameplayTweaks.betaExhaustionCosts(ActiveVersion.current());
    }
}

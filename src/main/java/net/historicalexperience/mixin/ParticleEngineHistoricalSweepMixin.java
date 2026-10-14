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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes the sweep attack's particle, in versions that had no sweep.
 *
 * <p>DIRECT PORT of the sweep branch of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/particle_engine/ParticleEngineMixin.java},
 * which answers {@code CandyTweak.OLD_SWEEP_PARTICLES} - a CLIENT tweak, and one upstream defaults to
 * <b>true</b>. The target, the handler shape and the {@code return null} answer are upstream's.
 *
 * <h2>Why the server fix was not enough, again</h2>
 *
 * <p>The owner reported the cooldown indicator gone but "the sweep" still present. Removing the sweep on the
 * server stops it damaging anything, but the particle is emitted by the attack regardless of whether any entity
 * was in range, and a particle is a client render - so suppressing the effect leaves the thing that shows it.
 * This is the third time the same shape has appeared: an effect is removed, its reading or its display is not.
 *
 * <p>Upstream treats it as a separate tweak because it is one, and it is on by default precisely so the visual
 * goes away when the mechanic does. This project gates it on the same boundary as the sweep itself, so the two
 * cannot disagree: no version declares a sweep, and none draws its particle.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineHistoricalSweepMixin {
    /** Upstream: {@code nt_particle_engine$modifyMakeParticle}, sweep branch only. */
    @ModifyExpressionValue(
        method = "createParticle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/ParticleEngine;makeParticle("
                + "Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)"
                + "Lnet/minecraft/client/particle/Particle;"))
    private Particle historicalExperience$noSweepParticles(Particle particle, ParticleOptions options,
            double x, double y, double z) {
        if (particle == null) {
            return null;
        }

        if (!HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            return particle;
        }

        // The gate above is the 1.9-combat boundary, which no declared version is on the far side of. Range 1.9
        // additions only: the sweep particle, the damage indicator and the enchanted-hit sparkle. CRIT is
        // deliberately absent - critical hits and their star burst arrive together in the Beta 1.8
        // Pre-release, and six declared profiles had them - so removing it would delete part of the era rather
        // than emulate it. Upstream has a flag for it, and a flag is not evidence.
        if (options.getType() == ParticleTypes.SWEEP_ATTACK
            || options.getType() == ParticleTypes.DAMAGE_INDICATOR
            || options.getType() == ParticleTypes.ENCHANTED_HIT) {
            return null;
        }

        return particle;
    }
}

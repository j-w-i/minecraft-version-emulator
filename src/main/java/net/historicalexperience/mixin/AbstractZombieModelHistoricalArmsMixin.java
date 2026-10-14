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

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.model.AbstractZombieModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Stops a zombie raising and swinging its arms, which it did not do in these versions.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/animation/mob_arms/AbstractZombieModelMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code AnimationTweak.OLD_ZOMBIE_ARMS}. Both argument
 * modifications, their indices and the target call are upstream's.
 *
 * <h2>Why this one IS ported while the crit particle was not</h2>
 *
 * <p>Both are default-true upstream client flags, and the difference is evidence. The research records that
 * zombie arm swinging was REMOVED in <b>Indev 20100202</b> - "Zombies' armor has been removed, as well as their
 * arm swinging attack animations" - and every declared profile is later than that date, the oldest being Infdev
 * 20100420. So the swinging arms are modern behaviour in all ten profiles. The crit particle, by contrast, is
 * older than six of the profiles, which is why that flag stayed out. A flag being on by default says nothing
 * either way; the version the behaviour changed is what decides.
 *
 * <h2>What each argument does</h2>
 *
 * <p>{@code isAggressive} makes the arms rise when the zombie notices a target, and {@code attackTime} drives the
 * swing. Upstream sets the first false and the second to zero, which leaves the classic outstretched arms in
 * every state rather than only when idle.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(AbstractZombieModel.class)
public abstract class AbstractZombieModelHistoricalArmsMixin {
    /** Upstream: {@code nt_mob_arms$setIsZombieAggressive}. */
    @ModifyArg(
        index = 2,
        method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/AnimationUtils;animateZombieArms("
                + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;ZFF)V"))
    private boolean historicalExperience$setIsZombieAggressive(boolean isAggressive) {
        if (HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            return false;
        }
        return isAggressive;
    }

    /** Upstream: {@code nt_mob_arms$setZombieAttackTime}. */
    @ModifyArg(
        index = 3,
        method = "setupAnim(Lnet/minecraft/world/entity/monster/Monster;FFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/AnimationUtils;animateZombieArms("
                + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;ZFF)V"))
    private float historicalExperience$setZombieAttackTime(float attackTime) {
        if (HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            return 0.0F;
        }
        return attackTime;
    }
}

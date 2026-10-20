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
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops zombies spawning as babies, in versions whose zombies did not.
 *
 * <p>DIRECT PORT of the baby half of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/monster_spawn/ZombieMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_BABY_ZOMBIE_SPAWN}.
 *
 * <h2>Why the handler is static, and why that matters</h2>
 *
 * <p>{@code getSpawnAsBabyOdds} is a static method with no level, so there is nothing to ask which version this
 * is. My first version worked around that by hooking {@code finalizeSpawn}'s return and calling
 * {@code setBaby(false)} afterwards. Upstream's answer is better and needs no context: it modifies the
 * {@code nextFloat()} call INSIDE the odds method and returns {@code 1.0F}, which can never compare as a baby,
 * so the spawn decision is made correctly the first time.
 *
 * <p>Upstream's other injection in that file, refusing armour and weapons on a zombie through
 * {@code populateDefaultEquipmentSlots}, belongs to the monster equipment rule and is ported with that rule
 * rather than here, so the two cannot drift apart.
 */
@Mixin(Zombie.class)
public abstract class ZombieHistoricalSpawnMixin {
    /** Upstream: {@code nt_monster_spawn$modifyBabyZombieOdds}. */
    @ModifyExpressionValue(
        method = "getSpawnAsBabyOdds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextFloat()F"))
    private static float historicalExperience$modifyBabyZombieOdds(float nextFloat) {
        if (Features.active(Feature.NO_BABY_ZOMBIES)) {
            return 1.0F;
        }
        return nextFloat;
    }
}

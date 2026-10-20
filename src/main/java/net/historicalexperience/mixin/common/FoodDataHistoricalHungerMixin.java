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

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the hunger system's side effects, in versions that had no hunger.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/FoodDataMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_HUNGER}. Both injection
 * points, the remembered heal amount and the shadowed fields are upstream's.
 *
 * <h2>What my first version got wrong</h2>
 *
 * <p>It suppressed the hunger tick and left it there. Upstream does two more things that matter: it ZEROES the
 * exhaustion level and tick timer every tick, so no hunger-related side effect can accumulate unseen, and it
 * heals from the food level modifier that {@code eat} reported - the second path by which eating restores
 * health, alongside the direct heal in {@code food_health/PlayerMixin}.
 */
@Mixin(FoodData.class)
public abstract class FoodDataHistoricalHungerMixin {
    @Unique private int historicalExperience$restoreHealthAmount = 0;

    @Shadow private int tickTimer;
    @Shadow private float exhaustionLevel;

    /** Upstream: {@code nt_food_health$onEat}. */
    @Inject(method = "eat(IF)V", at = @At("HEAD"))
    private void historicalExperience$onEat(int foodLevelModifier, float saturationLevelModifier,
            CallbackInfo callback) {
        if (hungerDisabled()) {
            this.historicalExperience$restoreHealthAmount = foodLevelModifier;
        }
    }

    /** Upstream: {@code nt_food_health$onTick}. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void historicalExperience$onTick(Player player, CallbackInfo callback) {
        if (this.historicalExperience$restoreHealthAmount > 0) {
            player.heal(this.historicalExperience$restoreHealthAmount);
            this.historicalExperience$restoreHealthAmount = 0;
        }

        if (!hungerDisabled()) {
            return;
        }

        this.exhaustionLevel = 0.0F;
        this.tickTimer = 0;
    }

    /** Whether this world's version had no hunger. */
    private static boolean hungerDisabled() {
        String session = ActiveVersion.current();
        return session != null && HistoricalPlayerRules.suppressesHungerTick(session);
    }
}

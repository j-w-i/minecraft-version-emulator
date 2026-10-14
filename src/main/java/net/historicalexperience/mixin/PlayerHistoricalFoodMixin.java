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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalFoodHelper;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Food restores health directly, in versions that had no hunger to restore.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/PlayerMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_HUNGER} and
 * {@code GameplayTweak.CUSTOM_FOOD_HEALTH}. Both injection points are upstream's, including the argument shapes.
 *
 * <h2>What my first version got wrong</h2>
 *
 * <p>It cancelled {@code canEat} and hooked the eating path with its own arithmetic. Upstream instead MODIFIES
 * what {@code canEat} returns, with a rule worth reading: without hunger you may eat whenever your health is
 * below your maximum, which is what makes an apple behave like an apple instead of a hunger refill. And it
 * heals at the HEAD of {@code eat}, before the food is consumed, so the amount comes from the item itself.
 *
 * <p>The heal amount comes from HistoricalFoodHelper, which uses the values recorded in
 * research/historical-features.json rather than the modern nutrition field. That mattered for cookies (the research
 * says 1 HP, modern nutrition says 2) and mushroom stew (10 against 6). The golden apple is deliberately left on
 * the fallback: the research lists it as a pre-1.8 food but gives no direct-heal figure for it.
 *
 * <p>NOT ported, and visible here rather than buried: upstream's third injection suppresses the eating "burp"
 * for items it classifies as instantaneous edibles, which needs its {@code FoodHelper} classification. And
 * {@code CUSTOM_FOOD_HEALTH} is a per-item map in upstream - a data listing, not code - so this port uses
 * upstream's own fallback, the item's nutrition value, for every food.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalFoodMixin {
    /** Upstream: {@code nt_food_health$onPlayerEat}. */
    @Inject(method = "eat", at = @At("HEAD"))
    private void historicalExperience$onPlayerEat(Level level, ItemStack itemStack,
            FoodProperties foodProperties, CallbackInfoReturnable<ItemStack> callback) {
        if (!hungerDisabled()) {
            return;
        }

        FoodProperties food = itemStack.get(DataComponents.FOOD);
        if (food == null) {
            return;
        }

        ((Player) (Object) this).heal(HistoricalFoodHelper.directHealAmount(itemStack, food));
    }

    /** Upstream: {@code nt_food_health$canPlayerEat}. */
    @ModifyReturnValue(method = "canEat", at = @At("RETURN"))
    private boolean historicalExperience$canPlayerEat(boolean playerCanEat, boolean canAlwaysEat) {
        if (hungerDisabled() && !canAlwaysEat) {
            Player self = (Player) (Object) this;
            return Mth.ceil(self.getHealth()) < Mth.ceil(self.getMaxHealth());
        }
        return playerCanEat;
    }

    /** Whether this world's version had no hunger. */
    private static boolean hungerDisabled() {
        String session = HistoricalActiveSession.current();
        return session != null && HistoricalPlayerRules.suppressesHungerTick(session);
    }
}

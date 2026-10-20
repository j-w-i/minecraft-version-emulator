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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalFoodHelper;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Restores the hunger values cookies and mushroom stew had before the tweaks that changed them.
 *
 * <p>From research/historical-features.json, entry {@code beta18-food-values-table} (confidence: high), which lists
 * the pairs introduced in Beta 1.8 and the later changes: cookies went from 1 to 2 hunger in Release 1.3.1, and
 * mushroom stew came down from 8 to 6 in Release 1.4.2. Nostalgic Tweaks has no food-value tweak, so this is from
 * the research.
 *
 * <p>The hook is the same one the damage values use - {@code DataComponentHolder.getOrDefault} - because that is
 * where every reader of a food component gets it from. Rebuilding through the record's own accessors carries the
 * saturation modifier, {@code canAlwaysEat}, the eat seconds, the conversion stack and the effect list across
 * untouched; only nutrition differs.
 *
 * <p>A shared mixin: item components are read on both sides.
 */
@Mixin(DataComponentHolder.class)
public interface DataComponentHolderHistoricalFoodMixin {
    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "getOrDefault", at = @At("RETURN"))
    default <T> T historicalExperience$modifyFoodValues(T value, DataComponentType<? extends T> component) {
        if (DataComponents.FOOD != component) {
            return value;
        }

        if (!((Object) this instanceof ItemStack itemStack) || !(value instanceof FoodProperties food)) {
            return value;
        }

        String versionId = ActiveVersion.current();

        // The golden apple changes in two ways, so it is handled first: 10 hunger instead of 4, and its effect
        // list is the Beta 1.8 one rather than the modern one.
        if (itemStack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
            && Features.active(Feature.BETA_GOLDEN_APPLE, versionId)) {
            // Regeneration for 30 seconds at amplifier 0, taken from the Beta 1.8 client artifact's item registry
            // line for appleGold. The first version of this returned an empty effect list, which was wrong: the
            // artifact applies the effect with probability 1.0.
            net.minecraft.world.effect.MobEffectInstance regeneration =
                new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION,
                    HistoricalFoodHelper.goldenAppleBetaRegenerationTicks(),
                    HistoricalFoodHelper.goldenAppleBetaRegenerationAmplifier());

            return (T) new FoodProperties(HistoricalFoodHelper.goldenAppleBetaHunger(), food.saturation(),
                food.canAlwaysEat(), food.eatSeconds(), food.usingConvertsTo(),
                java.util.List.of(new FoodProperties.PossibleEffect(regeneration, 1.0F)));
        }

        // Release 1.0-1.5: 4 hunger (which the modern item already has) and the slowed Regeneration, with no
        // Absorption. Only the effect list differs from modern.
        if (itemStack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
            && Features.active(Feature.GOLDEN_APPLE_REGEN_ONE, versionId)) {
            var release = java.util.List.of(new FoodProperties.PossibleEffect(
                new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION,
                    HistoricalFoodHelper.goldenAppleReleaseRegenerationTicks(),
                    HistoricalFoodHelper.goldenAppleReleaseRegenerationAmplifier()), 1.0F));
            return (T) new FoodProperties(food.nutrition(), food.saturation(), food.canAlwaysEat(),
                food.eatSeconds(), food.usingConvertsTo(), release);
        }

        // Release 1.6.1+: Regeneration II for 10 s plus Absorption I for 2:00 (the 13w23b-13w25a snapshot set).
        if (itemStack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
            && Features.active(Feature.GOLDEN_APPLE_SIX_ONE_EFFECTS, versionId)) {
            var sixOne = java.util.List.of(
                new FoodProperties.PossibleEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION,
                    HistoricalFoodHelper.goldenAppleSixOneRegenerationTicks(),
                    HistoricalFoodHelper.goldenAppleSixOneRegenerationAmplifier()), 1.0F),
                new FoodProperties.PossibleEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.ABSORPTION,
                    HistoricalFoodHelper.goldenAppleSixOneAbsorptionTicks(), 0), 1.0F));
            return (T) new FoodProperties(food.nutrition(), food.saturation(), food.canAlwaysEat(),
                food.eatSeconds(), food.usingConvertsTo(), sixOne);
        }

        // The second tier of the golden apple - the item 1.13 renamed the enchanted golden apple. Its effects are the
        // 1.3.1 set, with 13w24b's Absorption and Regeneration V from Release 1.6.1. It is 4 hunger, which is what the
        // modern item already has, so only the effect list differs.
        if (itemStack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)
            && Features.active(Feature.LEGACY_ENCHANTED_GOLDEN_APPLE, versionId)) {
            boolean absorption = Features.active(Feature.ENCHANTED_GOLDEN_APPLE_ABSORPTION, versionId);
            var effects = new java.util.ArrayList<FoodProperties.PossibleEffect>();
            effects.add(new FoodProperties.PossibleEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION,
                HistoricalFoodHelper.enchantedGoldenAppleRegenerationTicks(),
                HistoricalFoodHelper.enchantedGoldenAppleRegenerationAmplifier(absorption)), 1.0F));
            effects.add(new FoodProperties.PossibleEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                HistoricalFoodHelper.enchantedGoldenAppleResistanceTicks(), 0), 1.0F));
            effects.add(new FoodProperties.PossibleEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
                HistoricalFoodHelper.enchantedGoldenAppleFireResistanceTicks(), 0), 1.0F));
            if (absorption) {
                effects.add(new FoodProperties.PossibleEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.ABSORPTION,
                    HistoricalFoodHelper.enchantedGoldenAppleAbsorptionTicks(), 0), 1.0F));
            }
            return (T) new FoodProperties(food.nutrition(), food.saturation(), food.canAlwaysEat(),
                food.eatSeconds(), food.usingConvertsTo(), java.util.List.copyOf(effects));
        }

        if (!Features.active(Feature.BETA_FOOD_HUNGER_VALUES, versionId)) {
            return value;
        }

        String itemId =
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        Integer betaNutrition = HistoricalFoodHelper.betaHunger(itemId);

        if (betaNutrition == null || betaNutrition == food.nutrition()) {
            return value;
        }

        return (T) new FoodProperties(betaNutrition, food.saturation(), food.canAlwaysEat(),
            food.eatSeconds(), food.usingConvertsTo(), food.effects());
    }
}

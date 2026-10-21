/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalCombatDiagnostics;
import net.historicalexperience.gameplay.HistoricalPotionMixes;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.gameplay.HistoricalServerProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Night Vision and Invisibility cannot be brewed before Release 1.4.2.
 *
 * <p>minecraft.wiki, Java Edition 1.4.2 additions: the Potion of Night Vision is brewed from an awkward potion and a
 * golden carrot, and Invisibility from Night Vision and a fermented spider eye. The gate reads the ingredient from the
 * stand's own item list (slot 3) and the potion id from the result of {@code mix}, and records both in the diagnostics so
 * a probe can see exactly what it computed.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandHistoricalPotionMixesMixin {
    @Inject(method = "doBrew", at = @At("HEAD"), cancellable = true)
    private static void historicalExperience$refuseLaterPotions(Level level, BlockPos pos, NonNullList<ItemStack> items,
            CallbackInfo callback) {
        HistoricalCombatDiagnostics.recordBrewGateCall();
        String profile = level instanceof ServerLevel serverLevel
            ? HistoricalProfileLookup.profileIdForLevel(serverLevel)
            : HistoricalServerProfile.active();
        if (profile == null) {
            HistoricalCombatDiagnostics.recordBrewGateNoProfile();
            return;
        }
        ItemStack ingredient = items.get(3);
        String ingredientId = ingredient.isEmpty()
            ? "empty"
            : String.valueOf(BuiltInRegistries.ITEM.getKey(ingredient.getItem()));
        PotionBrewing brewing = level.potionBrewing();
        for (int slot = 0; slot < 3; slot++) {
            ItemStack bottle = items.get(slot);
            if (bottle.isEmpty()) {
                continue;
            }
            // mix's FIRST parameter is the ingredient and the second is the potion (verified in bytecode:
            // the first is tested against Mix.ingredient, the second's potion against Mix.from).
            ItemStack result = brewing.mix(ingredient, bottle);
            var contents = result.get(DataComponents.POTION_CONTENTS);
            String potionId = contents == null || contents.potion().isEmpty()
                ? "none"
                : contents.potion().get().unwrapKey().map(key -> key.location().toString()).orElse("unkeyed");
            HistoricalCombatDiagnostics.recordBrewMix(ingredientId, result.isEmpty() ? "empty-stack" : potionId);
            if (HistoricalPotionMixes.refuseBrew(potionId, profile)) {
                HistoricalCombatDiagnostics.recordBrewGateRefusal();
                callback.cancel();
                return;
            }
        }
    }
}

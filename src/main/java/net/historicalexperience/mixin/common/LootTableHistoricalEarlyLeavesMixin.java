/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import java.util.function.Consumer;
import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the leaf roll BEFORE modern apple/stick/fortune/silk pools execute; no post-roll deletion.
 * Source: hash-pinned leaf quantity/item callbacks, scripts/EarlyLeafExtractor.java.
 * Explosion survival delegates to the modern engine compatibility predicate; NOT validated old blast fidelity.
 */
@Mixin(LootTable.class)
public abstract class LootTableHistoricalEarlyLeavesMixin {
    @Inject(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
        at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$earlyLeafDrop(LootContext context, Consumer<ItemStack> output, CallbackInfo ci) {
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(Blocks.OAK_LEAVES)) return;
        String profile = HistoricalProfileLookup.profileIdForLevel(context.getLevel());
        var rule = EarlyGameplayRecipes.leafDrop(profile, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (rule.isEmpty()) return;
        var drop = rule.get();
        int count = drop.countForRoll(context.getRandom().nextInt(drop.chanceDenominator()));
        if (count > 0 && ExplosionCondition.survivesExplosion().build().test(context)) {
            output.accept(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(drop.result())), count));
        }
        ci.cancel();
    }
}

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
package net.historicalexperience.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.historicalexperience.gameplay.HistoricalChestLoot;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops content that did not exist yet out of a chest's roll.
 *
 * <p>{@code LootTable.fill} - the call every randomisable container makes when it is first opened - goes through the
 * private {@code getRandomItems(LootContext)}, so filtering there covers every chest in the game whatever table it
 * holds. The alternative, rewriting vanilla's chest tables, would have to be repeated for every table and would
 * silently do nothing for a table a datapack or another mod had added.
 *
 * <p>The level is taken from the roll's own {@link LootContext}, which is how the world's version is resolved; an
 * ordinary world resolves to no profile and every item is allowed, so nothing changes there.
 */
@Mixin(LootTable.class)
public abstract class LootTableHistoricalChestLootMixin {
    /**
     * The descriptor is pinned rather than left to the method name: {@code getRandomItems} has six overloads, and
     * matching on the name alone picked one of the others, which failed the injection at load.
     */
    @Inject(
        method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)"
            + "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
        at = @At("RETURN"))
    private void historicalExperience$dropAnachronisticLoot(LootContext context,
            CallbackInfoReturnable<ObjectArrayList<ItemStack>> callback) {
        ServerLevel level = context.getLevel();
        if (level == null) {
            return;
        }
        String profileId = HistoricalProfileLookup.profileIdForLevel(level);
        if (profileId == null) {
            return;
        }
        ObjectArrayList<ItemStack> rolled = callback.getReturnValue();
        if (rolled == null || rolled.isEmpty()) {
            return;
        }
        rolled.removeIf(stack -> {
            if (HistoricalChestLoot.allows(profileId, stack)) {
                return false;
            }
            HistoricalChestLoot.recordRemoved(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            return true;
        });
    }
}
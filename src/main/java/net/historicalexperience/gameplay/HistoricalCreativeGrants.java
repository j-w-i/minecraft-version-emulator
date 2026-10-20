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
package net.historicalexperience.gameplay;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Validate NEW creative grants, never rewrite stored stacks. Rich unsupported stacks remain owned content. */
public final class HistoricalCreativeGrants {
    private HistoricalCreativeGrants() { }
    public static boolean allowed(String selection, ItemStack stack) {
        if (!HistoricalCreativeRules.gatesCatalog(selection) || stack.isEmpty()) return true;
        if (stack.getCount() < 1 || stack.getCount() > stack.getMaxStackSize()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!HistoricalCreativeCatalog.available(selection, id)) return false;
        String base = net.historicalexperience.version.HistoricalSnapshotPatches.baseProfileForSelection(selection).orElseThrow();
        if (!enchants(base, stack.get(DataComponents.ENCHANTMENTS)) || !enchants(base, stack.get(DataComponents.STORED_ENCHANTMENTS))) return false;
        var defaults = new ItemStack(stack.getItem());
        var comparable = stack.copyWithCount(1);
        // Names/lore and native wear/repair counters do not introduce a new item. Never strip the real stack.
        reset(comparable, defaults, DataComponents.CUSTOM_NAME);
        reset(comparable, defaults, DataComponents.LORE);
        reset(comparable, defaults, DataComponents.DAMAGE);
        reset(comparable, defaults, DataComponents.REPAIR_COST);
        reset(comparable, defaults, DataComponents.ENCHANTMENTS);
        if (stack.is(Items.ENCHANTED_BOOK)) reset(comparable, defaults, DataComponents.STORED_ENCHANTMENTS);
        if (stack.has(DataComponents.DYED_COLOR) && id.startsWith("minecraft:leather_")
                && HistoricalCreativeCatalog.available(selection, "minecraft:anvil")) reset(comparable, defaults, DataComponents.DYED_COLOR);
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)) {
            var contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null || contents.potion().isEmpty() || contents.customColor().isPresent() || !contents.customEffects().isEmpty()) return false;
            String potionId = contents.potion().get().unwrapKey().map(key -> key.location().toString()).orElse("");
            if (!HistoricalCreativeCatalog.potionAvailable(selection, potionId, stack.is(Items.SPLASH_POTION))) return false;
            reset(comparable, defaults, DataComponents.POTION_CONTENTS);
        }
        // Includes entity/block-entity/container/custom data, modern attributes, trim, food overrides, etc.
        return ItemStack.isSameItemSameComponents(comparable, defaults);
    }
    private static boolean enchants(String base, ItemEnchantments values) {
        if (values == null) return true;
        for (var entry : values.entrySet()) {
            var key = entry.getKey().unwrapKey();
            int level = entry.getIntValue();
            if (key.isEmpty() || !HistoricalEnchanting.available(base, key.get()) || level < 1 || level > entry.getKey().value().getMaxLevel()) return false;
        }
        return true;
    }
    private static <T> void reset(ItemStack stack, ItemStack defaults, DataComponentType<T> component) {
        T value = defaults.get(component);
        if (value == null) stack.remove(component); else stack.set(component, value);
    }
    /** Two-sided guard: rejecting insertion alone would delete an old item during empty-then-fill moves. */
    public static boolean rejectsWrite(String selection, ItemStack owned, ItemStack incoming) {
        if (!HistoricalCreativeRules.gatesCatalog(selection) || ItemStack.matches(owned, incoming)) return false;
        return !allowed(selection, owned) || !allowed(selection, incoming);
    }
}

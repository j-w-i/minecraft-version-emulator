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
package net.historicalexperience.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.historicalexperience.gameplay.HistoricalCreativeCatalog;
import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Per-opening presentation; never alters shared native creative tab/search caches. */
public final class HistoricalCreativeInventory {
    private HistoricalCreativeInventory() { }
    public static String selection() {
        var version = HistoricalClientRules.activeVersion();
        if (version == null) return null;
        String patch = HistoricalClientRules.activeSnapshotPatchId();
        return patch.isEmpty() ? version.id() : patch;
    }
    public static List<ItemStack> entries(String selection, HolderLookup.Provider registries) {
        var result = new ArrayList<ItemStack>();
        for (String id : HistoricalCreativeCatalog.all().keySet()) {
            if (!HistoricalCreativeCatalog.available(selection, id)) continue;
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            if (item == Items.POTION || item == Items.SPLASH_POTION) {
                BuiltInRegistries.POTION.holders().forEach(potion -> add(result, selection, PotionContents.createItemStack(item, potion)));
            } else if (item == Items.ENCHANTED_BOOK) {
                registries.lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(enchantment -> {
                    for (int level = 1; level <= enchantment.value().getMaxLevel(); level++) {
                        var book = new ItemStack(Items.ENCHANTED_BOOK);
                        var enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        enchants.set(enchantment, level); book.set(DataComponents.STORED_ENCHANTMENTS, enchants.toImmutable());
                        add(result, selection, book);
                    }
                });
            } else add(result, selection, new ItemStack(item));
        }
        // Blocks before items. Stable data order within each group; not a claim of original numeric-ID order.
        result.sort(Comparator.comparingInt(stack -> stack.getItem() instanceof BlockItem ? 0 : 1));
        return result;
    }
    private static void add(List<ItemStack> into, String selection, ItemStack stack) {
        if (!stack.isEmpty() && HistoricalCreativeGrants.allowed(selection, stack)) into.add(stack);
    }
}

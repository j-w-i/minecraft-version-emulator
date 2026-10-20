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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalEnchanting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Takes out of the offer every enchantment the version did not have.
 *
 * <p>Vanilla rolls from the whole {@code #minecraft:in_enchanting_table} tag, which on 1.21.1 contains every
 * enchantment in the game - Mending, Frost Walker, the curses, the trident enchantments and the rest. None of those
 * existed in any version this project declares, so the list is filtered before the roll picks from it. Filtering the
 * CANDIDATES rather than the chosen enchantment matters: removing a result afterwards would bias the roll towards
 * whatever was left, while removing a candidate leaves a uniform roll over the era's own set.
 *
 * <p>An empty result is a real outcome and is left alone: a version whose permitted set cannot apply to the item
 * (Feather Falling is boots-only, for instance) offers nothing rather than a modern fallback.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuHistoricalPoolMixin {
    @ModifyReturnValue(method = "getEnchantmentList", at = @At("RETURN"))
    private List<EnchantmentInstance> historicalExperience$filterByVersion(List<EnchantmentInstance> rolled,
            RegistryAccess registries, ItemStack stack, int option, int cost) {
        if (rolled == null || rolled.isEmpty()) {
            return rolled;
        }
        String versionId = ActiveVersion.current();
        if (versionId == null) {
            return rolled;
        }
        if (!HistoricalEnchanting.usesOldTableRules(versionId)) {
            // The modern table keeps the modern pool; only a pre-Release-1.8 world is narrowed.
            return rolled;
        }
        if (Features.active(Feature.ENCHANTING_PLACEHOLDER, versionId)) {
            // Beta 1.9 Prerelease 3 applied NO enchantment of its own, because none existed yet, but it DID apply a
            // placeholder - the item gained the glint and a blue name, and the tooltip said "Enchanted!!".
            //
            // Filtering strictly by this version's own set therefore returned an empty list, the click found nothing
            // to apply, and the item came back unenchanted with no glint: an absence of everything the build did have,
            // in order to imitate the one thing it did not.
            //
            // The placeholder is FEATHER FALLING I, fixed. The owner asked for exactly that: every item enchanted in
            // Prerelease 3 should come out as Feather Falling I once the world reaches Prerelease 4. The option's cost
            // is charged as usual - the era spent levels - but the item does not get a random enchantment, so the
            // reveal is the same on every item and in every world rather than depending on a roll.
            return List.of(new EnchantmentInstance(registries
                .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(HistoricalEnchanting.placeholderEnchantment()),
                HistoricalEnchanting.placeholderEnchantmentLevel()));
        }
        List<EnchantmentInstance> permitted = rolled.stream()
            .filter(instance -> HistoricalEnchanting.available(versionId, instance.enchantment.unwrapKey()
                .orElse(null)))
            .toList();
        return permitted;
    }

}

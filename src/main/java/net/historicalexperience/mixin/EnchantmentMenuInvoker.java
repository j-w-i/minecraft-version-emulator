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

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches the enchanting table's own offer calculation, which 1.21.1 keeps private.
 *
 * <p>The historical click needs the same list vanilla's own click would have used - it must not be a second
 * implementation of the roll, or the two would disagree about what an offer contains. An invoker is how a mixin calls
 * the target's private method without reimplementing it.
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuInvoker {
    @Invoker("getEnchantmentList")
    List<EnchantmentInstance> historicalExperience$enchantmentList(RegistryAccess registries, ItemStack stack,
        int option, int cost);
}
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

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stops {@code placeItemBackInInventory} assembling a food stack in versions before Beta 1.8.
 *
 * <p>The third and last 1.21.1 path that consults the <b>item's</b> own maximum instead of a container limit.
 * It is the one used when something has to be handed back to the player - items left on the cursor when a
 * container closes, and a crafting result that will not fit - so without this a single such event could still
 * create a stack of food in an inventory that otherwise refuses to hold one. The owner's double-click test
 * exercises it directly, because the collected cursor stack is returned through here when the inventory screen
 * closes.
 *
 * <p>Found by disassembling the class rather than by reading the inventory code: {@code addResource} does
 * <b>not</b> call {@code ItemStack.getMaxStackSize} at the merge it performs (it calls the container's own
 * limit, which {@link ContainerHistoricalFoodStackMixin} now caps), and this method is the only place on
 * {@code Inventory} that does.
 */
@Mixin(Inventory.class)
public abstract class InventoryHistoricalFoodStackMixin {
    @Redirect(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int historicalExperience$capReturnedFoodMerge(ItemStack stack) {
        return net.historicalexperience.gameplay.HistoricalFoodStackLimit.foodDoesNotStack(stack)
            ? net.historicalexperience.gameplay.HistoricalFoodStackLimit.historicalStackLimit(stack)
            : stack.getMaxStackSize();
    }
}

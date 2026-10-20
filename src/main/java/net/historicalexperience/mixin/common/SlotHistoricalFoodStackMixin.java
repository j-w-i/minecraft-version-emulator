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

import net.historicalexperience.gameplay.HistoricalFoodStackLimit;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Holds one food per slot in versions before Beta 1.8, where food did not stack.
 *
 * <h2>Why the slot and not the item</h2>
 *
 * <p>The first attempt limited {@code ItemStack.getMaxStackSize}, and the client log caught what that breaks:
 *
 * <pre>
 *   ERROR: Parsing error loading recipe minecraft:dried_kelp
 *     Item stack with stack size of 9 was larger than maximum: 1
 *   ERROR: Parsing error loading recipe minecraft:honey_bottle
 *     Item stack with stack size of 4 was larger than maximum: 1
 * </pre>
 *
 * <p>An item's maximum size is part of DATA VALIDATION, not only inventory behaviour, so lowering it for every food
 * made any recipe whose ingredient count exceeded one - nine kelp, four honey bottles - fail to parse and disappear.
 * That is an accidental consequence rather than a decision, and this project does not accept those.
 *
 * <p>The slot's limit means "how many fit here", which is exactly the historical question, and nothing validates
 * recipe data against it. So the limit is applied where a player actually experiences it.
 *
 * <p>The limit is applied where a player actually experiences it, and it is now one of three places rather than
 * the only one. This mixin was the first, and its own note used to record that automatic pickup merging was still
 * possible because {@code Inventory.add} consults the container's maximum and not the slot's. The owner then
 * reported two live symptoms - food "picks up in a stack", and "if you double click in inventory it turns into a
 * stack" - and disassembling 1.21.1 found three paths that read the <b>item's</b> own maximum instead:
 * {@code Container.getMaxStackSize(ItemStack)} (what {@code Inventory.addResource} merges through),
 * {@code AbstractContainerMenu}'s six {@code doClick} and two quick-craft calls, and
 * {@code Inventory.placeItemBackInInventory}. Each is now capped by its own mixin, so the slot rule is no longer
 * the weak link.
 *
 * <p>A shared mixin: the client's slots answer the same question when it predicts a click.
 */
@Mixin(Slot.class)
public abstract class SlotHistoricalFoodStackMixin {
    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$limitFoodStack(CallbackInfoReturnable<Integer> callback) {
        Slot self = (Slot) (Object) this;
        if (historicalExperience$foodDoesNotStack(self.getItem())) {
            callback.setReturnValue(1);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$limitFoodStackFor(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (historicalExperience$foodDoesNotStack(stack)) {
            callback.setReturnValue(1);
        }
    }

    /**
     * True when this stack is food and the active version predates Beta 1.8.
     *
     * <p>Only items that are actually FOOD are limited, so tools, blocks and anything without a food component
     * cannot be affected - which also means the cake and mushroom stew exception the research records needs no
     * code: modern vanilla already keeps both at one.
     */
    private boolean historicalExperience$foodDoesNotStack(ItemStack stack) {
        // One definition, shared with the container, menu and inventory hooks, so the four paths cannot disagree.
        return HistoricalFoodStackLimit.foodDoesNotStack(stack);
    }
}

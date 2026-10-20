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

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops food stacking inside <b>any</b> container in versions before Beta 1.8, which is where the owner's two
 * reported symptoms actually come from.
 *
 * <h2>Why the existing slot limit was not enough</h2>
 *
 * <p>{@link SlotHistoricalFoodStackMixin} caps {@code Slot}'s own maximum, so a slot refuses to hold two food
 * items. The owner reported that this is bypassed in two places, and disassembling 1.21.1 shows exactly why:
 * automatic pickup merging does not consult the slot at all.
 * {@code Inventory.addResource(int, ItemStack)} computes
 * {@code Math.min(this.getMaxStackSize(stack) - present, incoming)} and calls
 * <b>{@code Container.getMaxStackSize(ItemStack)}</b> - the <b>container's</b> limit, which
 * {@code Inventory} inherits from the {@link Container} interface and never overrides. So a picked-up pile of
 * food merged up to the item's own maximum of 64 regardless of what the slot said.
 *
 * <p>That interface default is the single choke point every container merge goes through, so this is the
 * correct place for the rule rather than a second special case beside the slot one. It is an <b>interface
 * mixin</b> because {@code getMaxStackSize(ItemStack)} is a default method: the handler has to be a
 * {@code default} method itself, and the injection then applies to every implementer.
 *
 * <h2>Why the limit is not applied to the item instead</h2>
 *
 * <p>Lowering {@code ItemStack.getMaxStackSize} was tried first and broke data validation - the client log
 * recorded {@code Parsing error loading recipe minecraft:dried_kelp} ("Item stack with stack size of 9 was
 * larger than maximum: 1") and the same for honey bottles, so recipes whose ingredient count exceeded one
 * disappeared. A container's limit is a behavioural question that nothing validates recipe data against, which
 * is why the rule lives here and on the slot, and deliberately <b>not</b> on the item's component.
 *
 * <p>Only items that actually have a food component are limited, so tools, blocks and everything else cannot
 * be affected. Cake and mushroom stew need no special case: modern vanilla already keeps both at one.
 *
 * <p>A shared mixin: the client answers the same question when it predicts a click, and a disagreement there
 * is a desync.
 */
@Mixin(Container.class)
public interface ContainerHistoricalFoodStackMixin {
    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"),
        cancellable = true)
    default void historicalExperience$limitFoodStack(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (net.historicalexperience.gameplay.HistoricalFoodStackLimit.foodDoesNotStack(stack)) {
            callback.setReturnValue(1);
        }
    }
}

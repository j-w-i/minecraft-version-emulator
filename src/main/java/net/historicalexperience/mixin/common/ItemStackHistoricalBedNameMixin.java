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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalCraftingEra;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Calls every bed "Bed" in a version that only had one.
 *
 * <p>Owner: <i>"they were red beds for a long time no matter the wool used and they were just called beds"</i>. The
 * item a player holds here is the modern {@code red_bed}, whose own name is "Red Bed", so the name cannot come from
 * the item: the version decides it. Every bed colour is covered, because a world may already hold beds of other
 * colours from before this rule existed, and to a player in Beta they are all the same block.
 *
 * <p>{@code getHoverName} is the name the game shows in a tooltip, in the inventory, in the hotbar label and in the
 * creative search - one hook instead of one per surface. The decision itself lives in
 * {@link HistoricalCraftingEra#showsPlainBedName}, where a test can reach it.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackHistoricalBedNameMixin {
    @ModifyReturnValue(method = "getHoverName", at = @At("RETURN"), require = 1)
    private Component historicalExperience$plainBedName(Component original) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) {
            return original;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(self.getItem()).toString();
        if (!HistoricalCraftingEra.showsPlainBedName(ActiveVersion.current(), itemId)) {
            return original;
        }
        // This project's own key rather than the game's: modern Minecraft has a name per bed colour and no generic
        // "Bed" to borrow.
        return Component.translatable("historical_experience.item.bed");
    }
}

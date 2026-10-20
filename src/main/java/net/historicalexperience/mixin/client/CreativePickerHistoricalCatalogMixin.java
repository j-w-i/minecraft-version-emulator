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
package net.historicalexperience.mixin.client;

import net.historicalexperience.client.HistoricalCreativeInventory;
import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filter this picker's working list, not global tabs/search caches or owned inventory slots. */
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class CreativePickerHistoricalCatalogMixin {
    @Shadow @Final public NonNullList<ItemStack> items;
    @Inject(method = "scrollTo", at = @At("HEAD"), require = 1)
    private void historicalExperience$filterPage(float scroll, CallbackInfo ci) {
        String selection = HistoricalCreativeInventory.selection();
        if (!HistoricalCreativeRules.gatesCatalog(selection)) return;
        items.removeIf(stack -> !HistoricalCreativeGrants.allowed(selection, stack));
    }
}

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
import net.historicalexperience.client.LegacyCreativeScreen;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftHistoricalCreativeScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 1)
    private Screen historicalExperience$oldCreative(Screen requested) {
        var client = (Minecraft) (Object) this;
        if (requested instanceof CreativeModeInventoryScreen && client.player != null && client.gameMode != null
                && client.gameMode.hasInfiniteItems() && HistoricalCreativeRules.usesOldMenu(HistoricalCreativeInventory.selection()))
            return new LegacyCreativeScreen(client.player, HistoricalCreativeInventory.selection());
        return requested;
    }
}

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

import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.historicalexperience.server.HistoricalCreativeAuthority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class MenuHistoricalCreativeCloneMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$noUnavailableClone(int slot, int button, ClickType type, Player actor, CallbackInfo ci) {
        if (!(actor instanceof ServerPlayer player) || !player.gameMode.isCreative()) return;
        var menu = (AbstractContainerMenu) (Object) this;
        ItemStack source;
        if (type == ClickType.CLONE && slot >= 0 && slot < menu.slots.size()) source = menu.getSlot(slot).getItem();
        else if (type == ClickType.QUICK_CRAFT && AbstractContainerMenu.getQuickcraftType(button) == AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE) source = menu.getCarried();
        else return; // Normal pickup/swap/transfer/drop of already owned content is conserved, not forbidden.
        if (!HistoricalCreativeGrants.allowed(HistoricalCreativeAuthority.selection(player), source)) {
            HistoricalCreativeAuthority.resync(player); ci.cancel();
        }
    }
}

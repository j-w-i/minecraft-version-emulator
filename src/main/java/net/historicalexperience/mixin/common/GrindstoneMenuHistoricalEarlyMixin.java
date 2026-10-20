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

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalEarlyMenuRules;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Independent pre-assembly guard for repair/disenchant. Stored components and input slots are never rewritten. */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuHistoricalEarlyMixin {
    @Unique private Level historicalExperience$menuLevel;
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("RETURN"), require = 1)
    private void historicalExperience$bindLevel(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        historicalExperience$menuLevel = inventory.player.level();
    }
    @Inject(method = "createResult()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$denyEarlyResult(CallbackInfo ci) {
        Level level = historicalExperience$menuLevel;
        if (level == null) return;
        String profile = level instanceof ServerLevel server ? HistoricalProfileLookup.profileIdForLevel(server) : ActiveVersion.current();
        if (!HistoricalEarlyMenuRules.blocksResult(profile, "grindstone")) return;
        var menu = (AbstractContainerMenu) (Object) this;
        menu.getSlot(GrindstoneMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        menu.broadcastChanges();
        ci.cancel();
    }
}

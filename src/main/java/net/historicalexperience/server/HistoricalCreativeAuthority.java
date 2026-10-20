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
package net.historicalexperience.server;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Main-thread creative grants only. Does not filter loading/saving or ordinary container movement. */
public final class HistoricalCreativeAuthority {
    private HistoricalCreativeAuthority() { }
    public static String selection(ServerPlayer player) {
        String base = HistoricalProfileLookup.profileIdForLevel(player.server.overworld());
        return HistoricalSnapshotRuntime.selectionId(player.server, base);
    }
    public static boolean refuseWrite(ServerPlayer player, int slot, ItemStack incoming) {
        if (!player.gameMode.isCreative()) return false;
        String selection = selection(player);
        if (!HistoricalCreativeRules.gatesCatalog(selection)) return false;
        boolean inventorySlot = slot >= 1 && slot <= 45;
        ItemStack owned = inventorySlot ? player.inventoryMenu.getSlot(slot).getItem() : ItemStack.EMPTY;
        // Do not pass rich identical no-ops into vanilla's block-entity coordinate-copy rewrite either.
        if (inventorySlot && ItemStack.matches(owned, incoming) && !HistoricalCreativeGrants.allowed(selection, owned)) {
            resync(player); return true;
        }
        boolean unavailableOffhand = slot == 45 && !ItemStack.matches(owned, incoming)
            && Features.active(Feature.NO_OFFHAND, net.historicalexperience.version.HistoricalSnapshotPatches.baseProfileForSelection(selection).orElse(null));
        if (!unavailableOffhand && !HistoricalCreativeGrants.rejectsWrite(selection, owned, incoming)) return false;
        resync(player);
        player.displayClientMessage(Component.literal("Unavailable creative grant. Existing stacks kept; use a chest or Survival inventory to move them."), true);
        return true;
    }
    public static void resync(ServerPlayer player) {
        player.inventoryMenu.broadcastFullState();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastFullState();
        player.connection.send(new ClientboundContainerSetSlotPacket(-1, player.containerMenu.getStateId(), -1, player.containerMenu.getCarried().copy()));
    }
}

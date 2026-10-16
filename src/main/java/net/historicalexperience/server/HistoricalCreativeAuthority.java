/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.server;

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
            && net.historicalexperience.gameplay.HistoricalGameplayTweaks.offhandUnavailable(
                net.historicalexperience.version.HistoricalSnapshotPatches.baseProfileForSelection(selection).orElse(null));
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

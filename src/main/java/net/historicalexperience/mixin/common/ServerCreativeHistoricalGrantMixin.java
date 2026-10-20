/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin.common;

import net.historicalexperience.server.HistoricalCreativeAuthority;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerCreativeHistoricalGrantMixin {
    @Shadow public ServerPlayer player;
    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true, require = 1)
    private void historicalExperience$datedGrant(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (HistoricalCreativeAuthority.refuseWrite(player, packet.slotNum(), packet.itemStack())) ci.cancel();
    }
}

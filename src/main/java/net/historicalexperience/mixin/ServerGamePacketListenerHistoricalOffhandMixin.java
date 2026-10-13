package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses the swap-with-offhand action in versions that had no offhand.
 *
 * <p>The owner reported the hotkey still working after the offhand was supposedly removed: "i can still use the
 * hotkey". {@code PlayerHistoricalOffhandMixin} refuses {@code setItemSlot(OFFHAND, ...)}, which assumes the swap
 * ends up there - and it need not: the server may write the inventory directly, in which case the refusal never
 * runs.
 *
 * <p>Refusing the ACTION is both narrower and safer than guessing the write: it is exactly the key press the owner
 * complained about, and it cannot interfere with loading a saved inventory, which a guard on
 * {@code Inventory.setItem} would have done - and silently dropping an item a player already had is worse than
 * leaving it in place, which is the rule this project follows for content it cannot create.
 *
 * <p>A shared mixin: the packet is handled on the server, and this is the server-side handler.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerHistoricalOffhandMixin {
    /** Vanilla: {@code handlePlayerAction(ServerboundPlayerActionPacket)}. */
    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseOffhandSwap(ServerboundPlayerActionPacket packet, CallbackInfo callback) {
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
                && HistoricalGameplayTweaks.offhandUnavailable(HistoricalActiveSession.current())) {
            callback.cancel();
        }
    }
}

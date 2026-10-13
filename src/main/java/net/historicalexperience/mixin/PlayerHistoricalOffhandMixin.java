package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses to put anything in the offhand in versions that had no offhand.
 *
 * <p>The companion to removing the offhand slot from the menu. That stops the GUI and anything that acts on the
 * menu, but the swap hotkey does not go through a menu: the client sends a player-action packet and the server
 * writes the slot directly. Everything that writes an equipment slot - the hotkey, a command, a pick-block, a
 * plugin - funnels through {@code setItemSlot}, so guarding here means no source can create an offhand item in a
 * version that lacked the slot.
 *
 * <p>Only the OFFHAND slot is refused. The other four equipment slots are ancient and are guarded elsewhere, and a
 * broad refusal here would break armour and held items.
 *
 * <p>An existing item already in an offhand - from a save made before this gate existed, or by another tool - is
 * deliberately left alone rather than deleted, which is this project's rule for content it cannot place: refuse to
 * create it, never destroy what is there.
 *
 * <p>A shared mixin: the write happens on the server, and the client's copy of the player runs the same code path.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalOffhandMixin {
    /** Vanilla: {@code setItemSlot(EquipmentSlot, ItemStack)}. */
    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseOffhandWrite(EquipmentSlot slot, ItemStack stack, CallbackInfo callback) {
        if (slot == EquipmentSlot.OFFHAND
                && HistoricalGameplayTweaks.offhandUnavailable(HistoricalActiveSession.current())) {
            callback.cancel();
        }
    }
}

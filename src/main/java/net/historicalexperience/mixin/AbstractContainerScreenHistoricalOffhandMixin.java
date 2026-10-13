package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws nothing in the offhand slot in versions that had no offhand.
 *
 * <p>The client half of making the offhand inert. {@code SlotHistoricalOffhandMixin} already makes the slot
 * inactive, which vanilla respects when drawing - this is the explicit belt to that braces, because the drawing
 * path is the one a player actually notices, and being wrong here is invisible in every test.
 *
 * <p>Deliberately CLIENT-ONLY, registered under {@code client}. Nothing about which slots exist changes, so no
 * packet is affected - which is the difference between this and the attempt that caused "Network Protocol Error",
 * where the server's menu and the client's menu disagreed about the slot list.
 *
 * <p>If the session is not set on the client at the moment the screen draws, the worst outcome is a slot drawn that
 * the server will refuse to fill - a cosmetic disagreement, not a disconnection.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenHistoricalOffhandMixin {
    /** The offhand slot's index in the player's inventory. */
    private static final int OFFHAND_INVENTORY_INDEX = 40;

    /** Vanilla: {@code renderSlot(GuiGraphics, Slot)} draws one slot's contents. */
    @Inject(method = "renderSlot", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$hideOffhandSlot(GuiGraphics graphics, Slot slot, CallbackInfo callback) {
        if (slot.getContainerSlot() == OFFHAND_INVENTORY_INDEX
                && slot.container instanceof Inventory
                && HistoricalGameplayTweaks.offhandUnavailable(HistoricalActiveSession.current())) {
            callback.cancel();
        }
    }
}

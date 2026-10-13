package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes the offhand slot from the player's inventory in versions that had none.
 *
 * <p>Reported by the owner: "the offhand is available in all versions currently and should not be available at all,
 * not in the gui or via a hotkey". Correct - the offhand arrives in Release 1.9 and the newest version here is
 * Release 1.6.4.
 *
 * <p>Removing the SLOT from the menu, rather than hiding it while it still exists, is what makes this complete: a
 * hidden slot can still be clicked by a drag, reached by a shift-click, filled by the swap hotkey's inventory write,
 * and seen by any other screen that renders this menu. Refusing the {@code addSlot} call removes all of those at
 * once, and the survival and creative inventories both use this menu, so one change covers both.
 *
 * <h2>Why this hooks the declaring class, not the call site</h2>
 *
 * <p>Written first as a {@code @WrapOperation} on the constructor's {@code addSlot} call, which failed twice at
 * transformation: once because the callback took the receiver as a parameter, and once because the target named
 * {@code AbstractContainerMenu} as the owner when the call site's owner is {@code InventoryMenu} (javap omits the
 * owner when it equals the current class, which this session had already been bitten by once). Targeting the method
 * that actually adds a slot needs no call-site owner at all, and it cannot miss a call site because there is only
 * one implementation.
 *
 * <p>A shared mixin: the menu is built on the server and mirrored on the client, and both must agree about which
 * slots exist or the two will desynchronise.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuHistoricalOffhandMixin {
    /** The offhand slot's index in the player's inventory. */
    private static final int OFFHAND_INVENTORY_INDEX = 40;

    /** Vanilla: {@code addSlot(Slot)} returns the slot it added. */
    @Inject(method = "addSlot", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$skipOffhandSlot(Slot slot, CallbackInfoReturnable<Slot> callback) {
        if (!((Object) this instanceof InventoryMenu)) {
            return;
        }

        if (!HistoricalGameplayTweaks.offhandUnavailable(HistoricalActiveSession.current())) {
            return;
        }

        // Identified by container index and container type rather than by counting calls, so an unrelated slot being
        // added or reordered in a future version cannot move the removal to the wrong slot.
        if (slot.getContainerSlot() == OFFHAND_INVENTORY_INDEX && slot.container instanceof Inventory) {
            callback.setReturnValue(slot);
        }
    }
}

package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the offhand slot inert in versions that had no offhand, without changing which slots exist.
 *
 * <h2>Why not remove the slot</h2>
 *
 * <p>Removing it from {@code InventoryMenu} was the first attempt, and it broke joining with "Network Protocol
 * Error". A menu's slot list is PROTOCOL: the server builds one menu and the client builds its own copy, and the
 * client's decision depended on a session value delivered over the network, which may not have arrived yet. The two
 * disagreed about the slots and the connection died.
 *
 * <p>So the slot stays in the menu and is made unusable instead. That changes no packet shape: a refused
 * interaction simply returns the authoritative inventory to the client, which is what already happens when any
 * container rejects a click.
 *
 * <h2>The three refusals</h2>
 *
 * <ul>
 *   <li>{@code isActive} false - vanilla skips inactive slots when drawing, when working out what the mouse is over,
 *       and when resolving a click, so one override removes the slot from every player-facing path at once. This is
 *       why it is better than hiding the item alone, which would leave an invisible slot that still highlights when
 *       hovered and still accepts a click.</li>
 *   <li>{@code mayPlace} and {@code mayPickup} false - belt and braces, and the two methods that decide whether an
 *       item can enter or leave the slot when some other code path reaches it.</li>
 * </ul>
 *
 * <p>An item already in an offhand is left where it is rather than destroyed, which is this project's rule for
 * content it cannot place: refuse to create it, never destroy what is there.
 *
 * <p>A shared mixin, and deliberately so: these methods exist on both sides, and a version that refused the slot on
 * one side only would desynchronise predictions. Unlike the slot LIST, refusing an interaction is not protocol.
 */
@Mixin(Slot.class)
public abstract class SlotHistoricalOffhandMixin {
    /** The offhand slot's index in the player's inventory. */
    private static final int OFFHAND_INVENTORY_INDEX = 40;

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$deactivateOffhand(CallbackInfoReturnable<Boolean> callback) {
        if (historicalExperience$isDisabledOffhand()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseOffhandPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (historicalExperience$isDisabledOffhand()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseOffhandPickup(Player player, CallbackInfoReturnable<Boolean> callback) {
        if (historicalExperience$isDisabledOffhand()) {
            callback.setReturnValue(false);
        }
    }

    /**
     * Whether this slot is the player's offhand in a version that had no offhand.
     *
     * <p>Identified by container index and container type rather than by menu position, so an unrelated slot being
     * added or reordered cannot move the refusal to the wrong slot.
     */
    private boolean historicalExperience$isDisabledOffhand() {
        Slot self = (Slot) (Object) this;
        return self.getContainerSlot() == OFFHAND_INVENTORY_INDEX
            && self.container instanceof Inventory
            && HistoricalGameplayTweaks.offhandUnavailable(HistoricalActiveSession.current());
    }
}

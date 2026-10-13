package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.historicalexperience.client.SlotPositionAccess;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the inventory's offhand slot for versions that had none.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>, file
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/candy/screen/inventory/InventoryScreenHelper.java} and its
 * mixin {@code .../mixin/tweak/candy/inventory_screen/InventoryScreenMixin.java}, pinned in {@code .upstream/}. The
 * upstream {@code InventoryShield.INVISIBLE} branch is a single line:
 *
 * <pre>
 *   case INVISIBLE -&gt; SlotTracker.OFF_HAND.move(slot, -9999, -9999);
 * </pre>
 *
 * <p>This project gates by historical version rather than by a config flag - the upstream behaviour is selected from
 * {@code CandyTweak.INVENTORY_SHIELD} - but the mechanism is copied rather than reinvented, per the owner's standing
 * instruction to port NT directly instead of writing an in-house equivalent.
 *
 * <h2>Why this works where two of my own attempts did not</h2>
 *
 * <p>Attempt one removed the slot from {@code InventoryMenu} and broke joining with "Network Protocol Error": a menu's
 * slot list is protocol, so the server's menu and the client's disagreed. Attempt two made the slot inactive and
 * skipped its render, and the owner could still see it.
 *
 * <p>Moving the slot is neither. A slot's x and y are client-side LAYOUT - no packet carries them, and the menu still
 * contains exactly the same slots on both sides - so the slot draws nowhere, cannot be hovered because the mouse can
 * never be there, and cannot be clicked for the same reason. The upstream mod has shipped this for years, which is
 * better evidence than my reasoning was.
 *
 * <p>Only the INVISIBLE branch is ported. Upstream's other modes reposition the slot to the screen edge and patch
 * {@code hasClickedOutside} so an off-screen slot stays reachable; none of that is wanted here, because a version
 * with no offhand should have no offhand slot to reach.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenHistoricalOffhandMixin extends AbstractContainerScreen<InventoryMenu> {
    /** The offhand slot's index in the player's inventory. */
    private static final int OFFHAND_INVENTORY_INDEX = 40;

    /** Upstream's own off-screen coordinate: far enough that no mouse position can ever be over the slot. */
    private static final int UNREACHABLE = -9999;

    /** Matches the shape upstream uses for the same injection point. */
    private InventoryScreenHistoricalOffhandMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** Upstream injects at TAIL of the constructor; the slot list is complete by then. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void historicalExperience$hideOffhandSlot(Player player, CallbackInfo callback) {
        if (!HistoricalGameplayTweaks.offhandUnavailable(HistoricalActiveSession.current())) {
            return;
        }

        for (Slot slot : this.menu.slots) {
            if (slot.getContainerSlot() == OFFHAND_INVENTORY_INDEX && slot.container instanceof Inventory) {
                // Written through the duck interface rather than directly: Slot.x and Slot.y are final in 1.21.1,
                // which upstream's inline `slot.x = x` does not have to deal with.
                ((SlotPositionAccess) slot).historicalExperience$setSlotPosition(UNREACHABLE, UNREACHABLE);
            }
        }
    }
}

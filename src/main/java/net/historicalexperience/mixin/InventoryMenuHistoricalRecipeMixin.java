package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalRecipeGate;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses the player's own 2x2 crafting grid result when the active version never had it.
 *
 * <p>The 2x2 grid does not go through {@link CraftingMenu}; it fills its own result container directly in
 * {@code slotsChanged}, so the crafting-table hook cannot reach it. Without this, a bed - or any other later
 * block - would still be craftable from the inventory grid, which is the first place a player would try.
 *
 * <p>The result container is shadowed rather than obtained from a parameter because this method takes only the
 * changed {@link Container}. {@link HistoricalRecipeGate} owns the decision.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuHistoricalRecipeMixin {
    @Shadow @Final private ResultContainer resultSlots;

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void historicalExperience$refuseAnachronisticGridCraft(Container container, CallbackInfo callback) {
        ItemStack result = this.resultSlots.getItem(0);
        if (HistoricalRecipeGate.refuses(HistoricalActiveSession.current(), result)) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        }
    }
}

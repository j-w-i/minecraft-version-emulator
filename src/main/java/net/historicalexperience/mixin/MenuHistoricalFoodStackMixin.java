package net.historicalexperience.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stops a menu click from merging food into a stack in versions before Beta 1.8.
 *
 * <h2>The symptom this fixes</h2>
 *
 * <p>The owner put it precisely: <i>"if you double click in inventory it turns into a stack"</i>. Double-clicking
 * a slot is {@code ClickType.PICKUP_ALL}, which walks every slot and collects matching items onto the cursor.
 * {@code AbstractContainerMenu.doClick} asks {@code ItemStack.getMaxStackSize} - the <b>item's</b> own maximum -
 * in eight places, so it happily gathered food up to 64 and the slot rule was never consulted.
 *
 * <p>Redirecting that call inside {@code doClick} caps every merge the menu performs, which covers double-click
 * and ordinary pick-up/place alike: with the cap at one, a click can only ever move a single food item, and a
 * stack of food can no longer be assembled by hand. Non-food is untouched - the handler returns the item's own
 * maximum for it - so nothing else about the inventory changes.
 *
 * <p>Redirecting the call rather than cancelling the method is deliberate. Cancelling {@code doClick} would stop
 * clicks being processed at all on any version before Beta 1.8, which would break the whole inventory to fix one
 * rule.
 *
 * <p>A shared mixin: the client runs the same click handling to predict the result locally.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class MenuHistoricalFoodStackMixin {
    /**
     * The six calls inside {@code doClick} - the click handling itself, which is what double-click runs.
     */
    @Redirect(method = "doClick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int historicalExperience$capFoodMerge(ItemStack stack) {
        return ContainerHistoricalFoodStackMixin.HistoricalFoodStackLimit.foodDoesNotStack(stack)
            ? 1
            : stack.getMaxStackSize();
    }

    /**
     * The two calls in the static quick-craft helpers, which the class-wide count found and {@code doClick}
     * alone would have missed.
     *
     * <p>{@code canItemQuickReplace} decides whether a slot will accept more of a stack, and
     * {@code getQuickCraftPlaceCount} decides how many a drag places per slot. Both read the item's own
     * maximum, so shifting or dragging food could still assemble a stack even with the click handling capped -
     * the same bypass as the container merge, one layer out.
     */
    @Redirect(method = "canItemQuickReplace", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int historicalExperience$capQuickReplace(ItemStack stack) {
        return ContainerHistoricalFoodStackMixin.HistoricalFoodStackLimit.foodDoesNotStack(stack)
            ? 1
            : stack.getMaxStackSize();
    }

    @Redirect(method = "getQuickCraftPlaceCount", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int historicalExperience$capQuickCraftPlaceCount(ItemStack stack) {
        return ContainerHistoricalFoodStackMixin.HistoricalFoodStackLimit.foodDoesNotStack(stack)
            ? 1
            : stack.getMaxStackSize();
    }
}

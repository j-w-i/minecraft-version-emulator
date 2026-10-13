package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes food stack to one in versions before Beta 1.8.
 *
 * <p>Reported by the owner: "food also didnt stack until beta 1.8". Food became stackable with the hunger meter in
 * that update, so every earlier build carries it one at a time.
 *
 * <h2>Why the max stack size and not the component</h2>
 *
 * <p>{@code getMaxStackSize} is the single question every caller asks - the inventory's merge logic, the container's
 * click validation, the creative screen, and the client's own copy. Overriding it here means the answer is the same
 * everywhere, including places that never read the component directly. Writing the component instead would have to
 * cover each construction site.
 *
 * <p>Only items that are actually FOOD are limited. Everything else keeps its own size, so tools, blocks and
 * everything else are untouched, and an item with no food component cannot be affected by this at all.
 *
 * <p>A shared mixin, and deliberately so: the client validates its own clicks against this, and a client that
 * believed food stacked while the server did not would have its clicks rejected rather than desynchronised. Both
 * sides compute it from the same pushed session.
 *
 * <p>Residual risk worth naming: between joining and receiving the version payload, the client may briefly believe
 * food stacks. The worst outcome is a rejected merge, not a disconnection - which is why this is safe where
 * removing a menu slot was not.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackHistoricalFoodStackMixin {
    /** Vanilla: {@code getMaxStackSize()}. */
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$limitFoodStack(CallbackInfoReturnable<Integer> callback) {
        if (HistoricalGameplayTweaks.foodStacks(HistoricalActiveSession.current())) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        if (!self.isEmpty() && self.has(DataComponents.FOOD)) {
            // One, which is what every pre-hunger build carried.
            callback.setReturnValue(1);
        }
    }
}

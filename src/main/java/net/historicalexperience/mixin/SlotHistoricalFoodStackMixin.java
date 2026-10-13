package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Holds one food per slot in versions before Beta 1.8, where food did not stack.
 *
 * <h2>Why the slot and not the item</h2>
 *
 * <p>The first attempt limited {@code ItemStack.getMaxStackSize}, and the client log caught what that breaks:
 *
 * <pre>
 *   ERROR: Parsing error loading recipe minecraft:dried_kelp
 *     Item stack with stack size of 9 was larger than maximum: 1
 *   ERROR: Parsing error loading recipe minecraft:honey_bottle
 *     Item stack with stack size of 4 was larger than maximum: 1
 * </pre>
 *
 * <p>An item's maximum size is part of DATA VALIDATION, not only inventory behaviour, so lowering it for every food
 * made any recipe whose ingredient count exceeded one - nine kelp, four honey bottles - fail to parse and disappear.
 * That is an accidental consequence rather than a decision, and this project does not accept those.
 *
 * <p>The slot's limit means "how many fit here", which is exactly the historical question, and nothing validates
 * recipe data against it. So the limit is applied where a player actually experiences it.
 *
 * <p>Known limitation, recorded rather than glossed: automatic pickup merging may still combine food beyond one,
 * because {@code Inventory.add} consults the item's own maximum and not the slot's. Whether that is visible in play
 * is worth checking directly - manual stacking, which this covers, is the case a player is most likely to notice.
 *
 * <p>A shared mixin: the client's slots answer the same question when it predicts a click.
 */
@Mixin(Slot.class)
public abstract class SlotHistoricalFoodStackMixin {
    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$limitFoodStack(CallbackInfoReturnable<Integer> callback) {
        Slot self = (Slot) (Object) this;
        if (historicalExperience$foodDoesNotStack(self.getItem())) {
            callback.setReturnValue(1);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$limitFoodStackFor(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (historicalExperience$foodDoesNotStack(stack)) {
            callback.setReturnValue(1);
        }
    }

    /**
     * True when this stack is food and the active version predates Beta 1.8.
     *
     * <p>Only items that are actually FOOD are limited, so tools, blocks and anything without a food component
     * cannot be affected - which also means the cake and mushroom stew exception the research records needs no
     * code: modern vanilla already keeps both at one.
     */
    private boolean historicalExperience$foodDoesNotStack(ItemStack stack) {
        return !HistoricalGameplayTweaks.foodStacks(HistoricalActiveSession.current())
            && stack != null
            && !stack.isEmpty()
            && stack.has(DataComponents.FOOD);
    }
}

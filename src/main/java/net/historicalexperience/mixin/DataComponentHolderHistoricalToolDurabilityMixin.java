package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalToolDurability;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives tools the durability their version had, so a Beta 1.1 pickaxe breaks after 65 blocks rather than 131.
 *
 * <h2>Why the component and not {@code getMaxDamage}</h2>
 *
 * <p>In 1.21.1 an item stack's durability is the {@code DataComponents.MAX_DAMAGE} component, and
 * {@code ItemStack.getMaxDamage()} is only one of its readers - the damage bar, the anvil's repair arithmetic and
 * the "is this tool about to break" checks all read the component itself. Overriding the getter would change what
 * the player is told while leaving the arithmetic that actually destroys the tool on the modern value, which is
 * the same class of mistake as the food gate that limited a slot while pickup merged through the container.
 * {@code getOrDefault} is where every reader obtains it, and it is the hook the food values and the damage values
 * already use.
 *
 * <p>This was recorded as unimplemented for several rounds because it "needs an item-attribute gate this project
 * does not have" - the hook was already here for other components, so the numbers could simply be applied.
 *
 * <p>A shared mixin: the client draws the durability bar from the same component, so a server-only override would
 * show a full bar on a nearly-broken tool.
 */
@Mixin(DataComponentHolder.class)
public interface DataComponentHolderHistoricalToolDurabilityMixin {
    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "getOrDefault", at = @At("RETURN"))
    default <T> T historicalExperience$modifyToolDurability(T value, DataComponentType<? extends T> component) {
        if (DataComponents.MAX_DAMAGE != component || !(value instanceof Integer modern) || modern <= 0) {
            return value;
        }

        if (!((Object) this instanceof ItemStack itemStack)) {
            return value;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        Integer era = HistoricalToolDurability.durabilityFor(HistoricalActiveSession.current(), itemId);

        return era == null ? value : (T) era;
    }
}

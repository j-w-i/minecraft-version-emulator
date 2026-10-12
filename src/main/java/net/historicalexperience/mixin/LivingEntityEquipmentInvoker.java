package net.historicalexperience.mixin;

import java.util.Map;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code LivingEntity.collectEquipmentChanges}, which is private, so the equipment pass can be re-run
 * on demand.
 *
 * <p>Why it is needed: that private method is what applies a held item's ATTACK_DAMAGE to the player's
 * attribute map, and it only runs when equipment changes. The historical version is not known until the
 * player joins, so the map is built first and then never rebuilt - the era's damage values were gate-true and
 * completely inert until the owner switched items.
 *
 * <p>Declared as an abstract instance method on an interface, which is the form Mixin generates a body for.
 * The static form is rejected, and the failure is fatal at startup rather than a warning.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityEquipmentInvoker {
    @Invoker("collectEquipmentChanges")
    Map<EquipmentSlot, ItemStack> invokeCollectEquipmentChanges();
}

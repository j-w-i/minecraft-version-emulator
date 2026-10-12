package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalFoodHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Eating is instant, in versions that had no eating delay.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.INSTANT_EAT}. The injection point
 * is upstream's: the use duration read inside {@code startUsingItem}, forced to one so the item is consumed on
 * the next tick instead of after a second and a half of holding right-click.
 *
 * <h2>Where this file came from, because it took two attempts</h2>
 *
 * <p>The research worklist names {@code ConsumableMixin.java} as the carrier for instant eat. That file does not
 * exist in the upstream checkout: the worklist is generated from a <b>1.21.4</b> branch, and {@code Consumable}
 * is a 1.21.2 addition - our target is 1.21.1, which has no such component. The real mechanics are in the file
 * above, which I had already read and then wrongly set aside as out of scope.
 *
 * <p>Whether eating is instant is decided by {@link HistoricalFoodHelper}, which keys on the same Beta 1.8
 * boundary as hunger.
 *
 * <p>Upstream's companion injections that suppress the item-use effects are here as well: without them an
 * instant meal still threw eating particles, which the owner reported. The eating SOUND suppression and the
 * client-side heart-blink remain unported and are their own carriers.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalInstantEatMixin {
    @Shadow
    public abstract net.minecraft.world.item.ItemStack getItemInHand(InteractionHand hand);

    /**
     * Upstream: {@code nt_food_health$shouldTriggerItemUseEffectsOnUpdate}. Suppresses the eating particles that
     * tick while the item is being used.
     */
    @com.llamalad7.mixinextras.injector.v2.WrapWithCondition(
        method = "updateUsingItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;triggerItemUseEffects("
                + "Lnet/minecraft/world/item/ItemStack;I)V"))
    private boolean historicalExperience$noUseEffectsWhileEating(LivingEntity entity, ItemStack usingItem,
            int amount) {
        return !HistoricalFoodHelper.isInstantaneousEdible(usingItem);
    }

    /**
     * Upstream: {@code nt_food_health$shouldTriggerItemUseEffectsOnComplete}. The same suppression at the end of
     * the use, where the eating burst would otherwise appear.
     */
    @com.llamalad7.mixinextras.injector.v2.WrapWithCondition(
        method = "completeUsingItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;triggerItemUseEffects("
                + "Lnet/minecraft/world/item/ItemStack;I)V"))
    private boolean historicalExperience$noUseEffectsOnComplete(LivingEntity entity, ItemStack usingItem,
            int amount) {
        return !HistoricalFoodHelper.isInstantaneousEdible(usingItem);
    }

    /** Upstream: {@code nt_food_health$modifyUseDurationOnStartUsingItem}. */
    @ModifyExpressionValue(
        method = "startUsingItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseDuration("
                + "Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int historicalExperience$instantEating(int useDuration, InteractionHand hand) {
        if (HistoricalFoodHelper.isInstantaneousEdible(this.getItemInHand(hand))) {
            return 1;
        }
        return useDuration;
    }
}

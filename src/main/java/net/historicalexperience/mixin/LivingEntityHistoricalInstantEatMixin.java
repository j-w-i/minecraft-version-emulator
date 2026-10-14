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
 * instant meal still threw eating particles, which the owner reported.
 *
 * <h2>What this file got wrong, and how it was found</h2>
 *
 * <p>It said the companion injections were "here as well" and had <b>two of upstream's six</b>. The sound
 * suppression, the food-effects suppression and the hunger-effect rule were absent, so an instant meal still played
 * the eating sound and still applied its effects - which is exactly the kind of difference the owner described as
 * "it acts differently". The owner's suspicion was checked by reading upstream rather than by trusting this file's
 * own claim about itself, and it was right.
 *
 * <p>The hunger-effect rule ({@code PREVENT_HUNGER_EFFECT}) is a <b>different tweak</b> that happens to share the
 * upstream file, and it is still not ported - recorded as a gap rather than quietly folded in, because it is about
 * rotten flesh poisoning rather than about eating being instant.
 *
 * <p>Whether eating is instant is decided by {@link HistoricalFoodHelper}, which keys on the same Beta 1.8
 * boundary as hunger.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalInstantEatMixin {
    @Shadow
    public abstract net.minecraft.world.item.ItemStack getItemInHand(InteractionHand hand);

    /**
     * The item currently being used, shadowed because upstream's own {@code completeUsingItem} handler reads THIS
     * field rather than the method argument. The difference is not cosmetic: at completion the method's argument can
     * already be the finished stack, where {@code useItem} is still the food that was being eaten - and
     * {@link HistoricalFoodHelper#isInstantaneousEdible} asks a question about the food.
     *
     * <p>Named exactly as upstream shadows it. A wrong field name here is a fatal mixin error at launch, not a
     * warning, so it was checked against the 1.21.1 jar rather than assumed.
     */
    @Shadow
    protected net.minecraft.world.item.ItemStack useItem;

    /**
     * Upstream: {@code nt_food_health$shouldPlayConsumedFoodSound}. Suppresses the eating SOUND.
     *
     * <p>This was missing for several rounds while the file's own header claimed the companion injections were
     * "here as well". The owner noticed the behaviour differed from Nostalgic Tweaks, which is what prompted looking
     * - and upstream has six injections in this group where this project had two.
     *
     * <p>The suppression is UNCONDITIONAL once the item is an instantaneous edible, and that is upstream's
     * behaviour rather than a simplification: there is no second flag on this handler, unlike the effects one below.
     * A pre-hunger version had no eating sound because there was no eating time, so gating it further would
     * reintroduce a sound the era did not have.
     */
    @com.llamalad7.mixinextras.injector.v2.WrapWithCondition(
        method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound("
                + "Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;"
                + "Lnet/minecraft/sounds/SoundSource;FF)V"))
    private boolean historicalExperience$noConsumedFoodSound(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, double x, double y, double z,
            net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource category,
            float volume, float pitch, net.minecraft.world.level.Level arg1,
            ItemStack foodItem) {
        return !HistoricalFoodHelper.isInstantaneousEdible(foodItem);
    }

    /**
     * Upstream: {@code nt_food_health$shouldAddFoodEffects}. Suppresses the food's own status effects.
     *
     * <p>Upstream pairs this with {@code GameplayTweak.PREVENT_INSTANT_EAT_EFFECTS} and this project does not carry
     * that tweak, so the version gate alone decides - the same reduction the food helper documents for
     * {@code IGNORED_EDIBLES}. The consequence is stated rather than hidden: an instant meal in a pre-hunger version
     * grants no effects at all, where upstream would grant them if the player turned that second flag off.
     *
     * <p>That is the faithful reading of the era anyway - effects applied when the food was eaten, and these versions
     * had no eat duration for them to attach to.
     */
    @com.llamalad7.mixinextras.injector.v2.WrapWithCondition(
        method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;"
                + "addEatEffect(Lnet/minecraft/world/food/FoodProperties;)V"))
    private boolean historicalExperience$noFoodEffects(LivingEntity instance,
            net.minecraft.world.food.FoodProperties foodProperties, net.minecraft.world.level.Level level,
            ItemStack food) {
        return !HistoricalFoodHelper.isInstantaneousEdible(food);
    }

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
        // Upstream reads the SHADOWED useItem here, not the argument - see the shadow's own comment. The argument
        // can already be the finished stack by the time this runs, and the question is about the food.
        return !HistoricalFoodHelper.isInstantaneousEdible(this.useItem);
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

"""Port INSTANT_EAT, and record that upstream is a 1.21.4 branch being applied to 1.21.1.

Scope correction: the worklist names ConsumableMixin.java as the carrier, but that file does not exist in the
checkout - and cannot, because Consumable arrives in 1.21.2 and the checkout is a 1.21.4 branch. The mechanics
are in food_health/LivingEntityMixin.java, which I had already read and wrongly set aside:

    @ModifyExpressionValue(method = "startUsingItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration(...)I"))
    -> 1 if FoodHelper.isInstantaneousEdible(item)

with FoodHelper.isInstantaneousEdible = edible && INSTANT_EAT && not in IGNORED_EDIBLES.

Boundary: the eating delay and hunger arrive together in the Beta 1.8 Pre-release, so instant eat is gated on
the same rule that suppresses hunger. One source of truth rather than two boundaries that can drift.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java'
helper.write_text('''package net.historicalexperience.gameplay;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * PORTED from Nostalgic Tweaks {@code helper/gameplay/FoodHelper.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <p>Upstream's test is {@code isEdible(itemStack) && GameplayTweak.INSTANT_EAT && !IGNORED_EDIBLES}.
 *
 * <p>The version condition replaces upstream's global flag, and deliberately reuses the hunger rule rather than
 * introducing a second boundary: the eating delay and hunger arrive together in the Beta 1.8 Pre-release, so
 * "this version has no hunger" and "this version eats instantly" are the same fact stated twice, and stating it
 * once is how they stay in agreement.
 *
 * <p>Upstream's {@code IGNORED_EDIBLES} list is not ported: it is a data listing of items to exempt, and no
 * exemption is needed until one is asked for. Nothing here silently drops it - every edible is instantaneous in
 * these versions.
 */
public final class HistoricalFoodHelper {
    private HistoricalFoodHelper() {
    }

    /** Whether this item is edible at all. */
    public static boolean isEdible(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    /** Whether this item is eaten instantly in the active version. */
    public static boolean isInstantaneousEdible(ItemStack stack) {
        String session = HistoricalActiveSession.current();

        if (session == null || !HistoricalPlayerRules.suppressesHungerTick(session)) {
            return false;
        }

        return isEdible(stack);
    }
}
''', encoding='utf-8')
print('wrote HistoricalFoodHelper')

mixin = root / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalInstantEatMixin.java'
mixin.write_text('''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalFoodHelper;
import net.minecraft.world.InteractionHand;
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
 * <p>NOT ported here: upstream's companion injections that suppress the item-use effects and the eating sounds
 * for instantaneous edibles, and its client-side heart-blink. Those are presentation around the same mechanic
 * and are separate carriers.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalInstantEatMixin {
    @Shadow
    public abstract net.minecraft.world.item.ItemStack getItemInHand(InteractionHand hand);

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
''', encoding='utf-8')
print('wrote LivingEntityHistoricalInstantEatMixin')

import json
config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'LivingEntityHistoricalInstantEatMixin' not in document['mixins']:
    document['mixins'].append('LivingEntityHistoricalInstantEatMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

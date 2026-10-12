"""Two fixes from the owner's report.

1. CRITS COULD DO NEARLY ZERO DAMAGE. The research value is "random +0%..+100% of weapon damage, plus 2 HP", which
   is a BONUS on top of the weapon's damage: total ranges from 1x to 2x. I implemented it as the multiplier itself
   returning [0, 1], which ranges from 0x to 1x - so a crit could reduce a hit to nothing, which the owner
   correctly says never happened. The multiplier is now [1, 2].

   The +2 HP is still not implemented and still documented; this fix is about the range, which was wrong in a way
   the owner could see immediately.

2. INSTANT EATING SHOWED EATING PARTICLES. Before hunger, food was eaten with no eating animation or effects, and
   Nostalgic Tweaks suppresses exactly that through two @WrapWithCondition injections on triggerItemUseEffects -
   one in updateUsingItem, one in completeUsingItem - for items its FoodHelper calls instantaneous edibles. I
   ported the use duration and listed these as "not ported"; the owner saw the gap. Both are now here.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

crit = root / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalCriticalHitMixin.java'
text = crit.read_text(encoding='utf-8')
old = '''            Player self = (Player) (Object) this;
            return Mth.nextFloat(self.getRandom(), 0.0F, 1.0F);'''
new = '''            Player self = (Player) (Object) this;
            // 1..2, NOT 0..1. The formula is a bonus on top of the weapon's damage - "+0% to +100% of weapon
            // damage" - so a critical hit can never land for less than an ordinary one. Returning 0..1 made a
            // crit multiply the hit toward nothing, which the owner reported as damage that never happened.
            return Mth.nextFloat(self.getRandom(), 1.0F, 2.0F);'''
assert old in text, 'crit anchor not found'
text = text.replace(old, new, 1)
text = text.replace(''' * <p>WHAT IS NOT, and why it is stated rather than implied''', ''' * <p>WHAT IS NOT, and why it is stated rather than implied''', 1)
crit.write_text(text, encoding='utf-8')
print('crit multiplier is now 1x..2x')

mixin = root / 'src/main/java/net/historicalexperience/mixin/LivingEntityHistoricalInstantEatMixin.java'
text = mixin.read_text(encoding='utf-8')

old = '''    /** Upstream: {@code nt_food_health$modifyUseDurationOnStartUsingItem}. */'''
new = '''    /**
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

    /** Upstream: {@code nt_food_health$modifyUseDurationOnStartUsingItem}. */'''
assert old in text, 'mixin anchor not found'
text = text.replace(old, new, 1)

text = text.replace(''' * <p>NOT ported here: upstream's companion injections that suppress the item-use effects and the eating sounds
 * for instantaneous edibles, and its client-side heart-blink. Those are presentation around the same mechanic
 * and are separate carriers.''',
''' * <p>Upstream's companion injections that suppress the item-use effects are here as well: without them an
 * instant meal still threw eating particles, which the owner reported. The eating SOUND suppression and the
 * client-side heart-blink remain unported and are their own carriers.''', 1)
mixin.write_text(text, encoding='utf-8')
print('instant eating no longer triggers use effects')

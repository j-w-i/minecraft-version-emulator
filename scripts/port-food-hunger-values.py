"""Give the hunger profiles Beta 1.8's hunger values for cookies and mushroom stew.

research/historical-features.json, entry beta18-food-values-table (confidence: high) gives the per-item
(hunger, saturationModifier) pairs introduced in Beta 1.8, and two later changes that moved them:

    Release 1.3.1 | Cookie hunger raised to 2          -> Beta value is 1
    Release 1.4.2 | Mushroom stew lowered to 6 hunger  -> Beta value is 8

So five declared profiles feed a cookie that fills 2 hunger where the era's filled 1, and stew that fills 6 where it
filled 8. Both are nutrition-only changes, which is why they are safe to do first: the saturation modifier and every
other field are carried across untouched by rebuilding the record with its own accessors.

NOT done here: the golden apple. Beta 1.8's was 10 hunger with NO regeneration effect, and Release 1.1 remade it as
4 hunger WITH one - so restoring it means replacing the effects list as well, which is a different kind of edit.
Left for its own change rather than folded in.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Restores the hunger values cookies and mushroom stew had before the tweaks that changed them.
 *
 * <p>From research/historical-features.json, entry {@code beta18-food-values-table} (confidence: high), which lists
 * the pairs introduced in Beta 1.8 and the later changes: cookies went from 1 to 2 hunger in Release 1.3.1, and
 * mushroom stew came down from 8 to 6 in Release 1.4.2. Nostalgic Tweaks has no food-value tweak, so this is from
 * the research.
 *
 * <p>The hook is the same one the damage values use - {@code DataComponentHolder.getOrDefault} - because that is
 * where every reader of a food component gets it from. Rebuilding through the record's own accessors carries the
 * saturation modifier, {@code canAlwaysEat}, the eat seconds, the conversion stack and the effect list across
 * untouched; only nutrition differs.
 *
 * <p>A shared mixin: item components are read on both sides.
 */
@Mixin(DataComponentHolder.class)
public abstract class DataComponentHolderHistoricalFoodMixin {
    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "getOrDefault", at = @At("RETURN"))
    default <T> T historicalExperience$modifyFoodValues(T value, DataComponentType<? extends T> component) {
        if (DataComponents.FOOD != component) {
            return value;
        }

        if (!((Object) this instanceof ItemStack itemStack)
            || !(value instanceof FoodProperties food)
            || !HistoricalGameplayTweaks.betaFoodHungerValues(HistoricalActiveSession.current())) {
            return value;
        }

        String itemId =
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
        Integer betaNutrition = HistoricalFoodHelper.betaHunger(itemId);

        if (betaNutrition == null || betaNutrition == food.nutrition()) {
            return value;
        }

        return (T) new FoodProperties(betaNutrition, food.saturation(), food.canAlwaysEat(),
            food.eatSeconds(), food.usingConvertsTo(), food.effects());
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/DataComponentHolderHistoricalFoodMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the food values mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
if 'DataComponentHolderHistoricalFoodMixin' not in document['mixins']:
    document['mixins'].append('DataComponentHolderHistoricalFoodMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java'
text = helper.read_text(encoding='utf-8')
old = '''    /**
     * The researched direct-heal value for an item id, or null when the research gives none.'''
new = '''    /**
     * The hunger a food filled before the change that moved it, or null when the research records no change.
     *
     * <p>From {@code beta18-food-values-table} in research/historical-features.json: cookies filled 1 hunger until
     * Release 1.3.1, and mushroom stew filled 8 until Release 1.4.2.
     */
    private static final java.util.Map<String, Integer> BETA_HUNGER = java.util.Map.of(
        "minecraft:cookie", 1,
        "minecraft:mushroom_stew", 8);

    /** The pre-change hunger value for an item id, or null when there is none. */
    public static Integer betaHunger(String itemId) {
        return BETA_HUNGER.get(itemId);
    }

    /**
     * The researched direct-heal value for an item id, or null when the research gives none.'''
assert old in text, 'helper anchor not found'
helper.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added the beta hunger table')

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean betaRegenerationModel(String versionId) {'''
new = '''    /**
     * Whether cookies and mushroom stew fill their pre-change hunger values.
     *
     * <p>Both changes - cookie 1 to 2 in Release 1.3.1, stew 8 to 6 in Release 1.4.2 - happened before Release
     * 1.6.4, the newest declared profile, so it serves as the proxy for both. Gated on the hunger boundary as well,
     * since a food bar is what these values fill.
     */
    public static boolean betaFoodHungerValues(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId)
            && olderThan(versionId, RELEASE_1_6_4);
    }

    public static boolean betaRegenerationModel(String versionId) {'''
assert old in text, 'tweaks anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added betaFoodHungerValues')

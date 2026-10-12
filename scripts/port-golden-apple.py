"""Restore the pre-Release-1.1 golden apple: 10 hunger and no Regeneration.

research/historical-features.json, entry beta18-food-values-table (confidence: high):
    food.appleGold = {'hunger': 10, 'saturationModifier': 1.2}
and its change list:
    Release 1.1 | Golden apples remade with gold nuggets, restore 4 hunger and 4 s Regeneration (later).

So before Release 1.1 a golden apple filled 10 hunger and did NOT grant Regeneration; from Release 1.1 it fills 4
and does. Three declared profiles are on the earlier side - Beta 1.8.1, Beta 1.9 Prerelease 3 and Release 1.0.0 -
with Release 1.2.5 as the nearest declared profile past the change, so it is the proxy.

This was left out when cookies and stew went in because it needs the effects list replaced as well, not just
nutrition. It is the same edit as before plus an empty effect list, which is why it is safe now: the record is
rebuilt through its own accessors, so eat seconds, canAlwaysEat and the conversion stack are carried across.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java'
text = helper.read_text(encoding='utf-8')
old = '''    /** The pre-change hunger value for an item id, or null when there is none. */'''
new = '''    /**
     * The golden apple's pre-Release-1.1 values: 10 hunger, and no Regeneration effect.
     *
     * <p>Its nutrition and its effect list both change, which is why it is handled apart from the cookie-and-stew
     * table - those are nutrition alone.
     */
    private static final int GOLDEN_APPLE_BETA_HUNGER = 10;

    /** Upstream has no equivalent. The pre-Release-1.1 golden apple hunger value. */
    public static int goldenAppleBetaHunger() {
        return GOLDEN_APPLE_BETA_HUNGER;
    }

    /** The pre-change hunger value for an item id, or null when there is none. */'''
assert old in text, 'helper anchor not found'
helper.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added the golden apple value')

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')
old = '''    public static boolean betaFoodHungerValues(String versionId) {'''
new = '''    /**
     * Whether the golden apple is the pre-Release-1.1 item: 10 hunger, no Regeneration.
     *
     * <p>Release 1.1 remade it as a gold-nugget recipe granting Regeneration and 4 hunger, so Beta 1.8.1 through
     * Release 1.0.0 are on the earlier item. Release 1.2.5 is the nearest declared profile past the change and
     * serves as the proxy.
     */
    public static boolean betaGoldenApple(String versionId) {
        return isDeclaredVersion(versionId)
            && !BEFORE_BETA_1_8.contains(versionId)
            && olderThan(versionId, RELEASE_1_2_5);
    }

    public static boolean betaFoodHungerValues(String versionId) {'''
assert old in text, 'tweaks anchor not found'
tweaks.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added betaGoldenApple')

mixin = root / 'src/main/java/net/historicalexperience/mixin/DataComponentHolderHistoricalFoodMixin.java'
text = mixin.read_text(encoding='utf-8')
old = '''        if (!((Object) this instanceof ItemStack itemStack)
            || !(value instanceof FoodProperties food)
            || !HistoricalGameplayTweaks.betaFoodHungerValues(HistoricalActiveSession.current())) {
            return value;
        }'''
new = '''        if (!((Object) this instanceof ItemStack itemStack) || !(value instanceof FoodProperties food)) {
            return value;
        }

        String versionId = HistoricalActiveSession.current();

        // The golden apple changes in two ways, so it is handled first: 10 hunger, and no effect list at all.
        if (itemStack.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
            && HistoricalGameplayTweaks.betaGoldenApple(versionId)) {
            return (T) new FoodProperties(HistoricalFoodHelper.goldenAppleBetaHunger(), food.saturation(),
                food.canAlwaysEat(), food.eatSeconds(), food.usingConvertsTo(), java.util.List.of());
        }

        if (!HistoricalGameplayTweaks.betaFoodHungerValues(versionId)) {
            return value;
        }'''
assert old in text, 'mixin anchor not found'
text = text.replace(old, new, 1)
mixin.write_text(text, encoding='utf-8')
print('the golden apple is handled ahead of the nutrition-only table')

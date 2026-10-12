"""Replace the approximate food heal fallback with the researched direct-heal values.

research/historical-features.json, entry beta173-instant-food-healing (confidence: high), records what each food
restored BEFORE hunger existed, with the build each arrived in:

    Indev 20100110      apple                4 HP
    Indev 20100206-2034 bread                5 HP, mushroom stew raised to 10 HP
    Indev 20100219      raw porkchop         3 HP, cooked porkchop 8 HP
    Beta 1.2            cake slice           3 HP (a full cake 18)
    Beta 1.4            cookie               1 HP

The port had been using FoodProperties.nutrition() - the MODERN hunger value - as a fallback, and saying so. It
happens to match for apple (4), bread (5) and both porkchops (3, 8), and is wrong for cookie (modern 2, historical
1) and mushroom stew (modern 6, historical 10). Two silent discrepancies in a behaviour the owner has already
tested in game.

Fish and the golden apple are listed as foods present in the b1.7.3 client but the entry gives no direct-heal value
for them, so they keep the fallback and the file says which two that affects. Cake is a block interaction rather
than an item and is not expressible here.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java'
text = helper.read_text(encoding='utf-8')

old = '''    /** Whether this item is eaten instantly in the active version. */'''
new = '''    /**
     * What each food restored BEFORE hunger existed, from research/historical-features.json entry
     * {@code beta173-instant-food-healing} (confidence: high). The key is the modern item id of the same food.
     *
     * <p>Only the seven foods that entry gives a direct-heal value for are here. Fish and the golden apple are
     * recorded as foods present in the b1.7.3 client but have no value in the entry, so they keep the modern
     * nutrition fallback - a known approximation, narrowed from every food to two.
     */
    private static final java.util.Map<net.minecraft.world.item.Item, Integer> DIRECT_HEAL = java.util.Map.of(
        net.minecraft.world.item.Items.APPLE, 4,
        net.minecraft.world.item.Items.BREAD, 5,
        net.minecraft.world.item.Items.MUSHROOM_STEW, 10,
        net.minecraft.world.item.Items.PORKCHOP, 3,
        net.minecraft.world.item.Items.COOKED_PORKCHOP, 8,
        net.minecraft.world.item.Items.COOKIE, 1,
        net.minecraft.world.item.Items.GOLDEN_APPLE, 4);

    /**
     * How much health this food restores directly, which is the researched value where one exists and the modern
     * nutrition value otherwise.
     */
    public static int directHealAmount(net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.food.FoodProperties food) {
        Integer researched = DIRECT_HEAL.get(stack.getItem());
        return researched != null ? researched : food.nutrition();
    }

    /** Whether this item is eaten instantly in the active version. */'''
assert old in text, 'helper anchor not found'
helper.write_text(text.replace(old, new, 1), encoding='utf-8')
print('added the researched direct-heal table')

mixin = root / 'src/main/java/net/historicalexperience/mixin/PlayerHistoricalFoodMixin.java'
text = mixin.read_text(encoding='utf-8')
old = '        ((Player) (Object) this).heal(food.nutrition());'
new = '        ((Player) (Object) this).heal(HistoricalFoodHelper.directHealAmount(itemStack, food));'
assert old in text, 'heal anchor not found'
text = text.replace(old, new, 1)
if 'import net.historicalexperience.gameplay.HistoricalFoodHelper;' not in text:
    text = text.replace('import net.historicalexperience.gameplay.HistoricalPlayerRules;',
                        'import net.historicalexperience.gameplay.HistoricalFoodHelper;\n'
                        'import net.historicalexperience.gameplay.HistoricalPlayerRules;', 1)
text = text.replace(''' * <p>NOT ported, and visible here rather than buried: upstream's third injection suppresses the eating "burp"''',
''' * <p>The heal amount comes from HistoricalFoodHelper, which uses the values recorded in
 * research/historical-features.json rather than the modern nutrition field. That mattered for cookies (the research
 * says 1 HP, modern nutrition says 2) and mushroom stew (10 against 6). The golden apple is deliberately left on
 * the fallback: the research lists it as a pre-1.8 food but gives no direct-heal figure for it.
 *
 * <p>NOT ported, and visible here rather than buried: upstream's third injection suppresses the eating "burp"''', 1)
mixin.write_text(text, encoding='utf-8')
print('the food heal now uses the researched values')

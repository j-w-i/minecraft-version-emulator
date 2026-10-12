"""Key the heal table by item id, so it can be tested without bootstrapping Minecraft's registries.

The first version keyed the table by Item and its test failed with
"Could not initialize class net.minecraft.world.item.ItemStack" - unit tests here cannot construct game objects.
Keying by registry id is not a workaround: this project's other data tables are id-keyed, and it lets the values
be asserted directly, which matters because the values ARE the deliverable.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java'
text = helper.read_text(encoding='utf-8')

old_start = text.index('    private static final java.util.Map<net.minecraft.world.item.Item, Integer> DIRECT_HEAL')
old_end = text.index('    /** Whether this item is eaten instantly in the active version. */')
replacement = '''    private static final java.util.Map<String, Integer> DIRECT_HEAL_BY_ID = java.util.Map.of(
        "minecraft:apple", 4,
        "minecraft:bread", 5,
        "minecraft:mushroom_stew", 10,
        "minecraft:porkchop", 3,
        "minecraft:cooked_porkchop", 8,
        "minecraft:cookie", 1);

    /**
     * The researched direct-heal value for an item id, or null when the research gives none.
     *
     * <p>Keyed by id rather than by {@code Item} so the values can be asserted in a unit test: this project's tests
     * cannot construct {@code ItemStack}, whose static initialiser needs Minecraft's registries bootstrapped.
     */
    public static Integer researchedHeal(String itemId) {
        return DIRECT_HEAL_BY_ID.get(itemId);
    }

    /**
     * How much health this food restores directly: the researched value where one exists, the modern nutrition
     * value otherwise.
     */
    public static int directHealAmount(net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.food.FoodProperties food) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Integer researched = researchedHeal(itemId);
        return researched != null ? researched : food.nutrition();
    }

'''
text = text[:old_start] + replacement + text[old_end:]
helper.write_text(text, encoding='utf-8')
print('table is now id-keyed with a public accessor')

TEST = '''package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Asserts the direct-heal values, which are data rather than logic.
 *
 * <p>Source: research/historical-features.json, entry {@code beta173-instant-food-healing} (confidence: high). Two
 * of them differ from modern nutrition, which is why this table exists at all: a cookie restored 1 HP where modern
 * nutrition says 2, and mushroom stew restored 10 where modern says 6.
 */
class HistoricalFoodHealTableTest {
    @Test
    void researchedFoodsUseTheirEraValues() {
        assertEquals(4, HistoricalFoodHelper.researchedHeal("minecraft:apple"),
            "apple restored 4 HP from Indev 20100110");
        assertEquals(5, HistoricalFoodHelper.researchedHeal("minecraft:bread"),
            "bread restored 5 HP from Indev 20100206");
        assertEquals(10, HistoricalFoodHelper.researchedHeal("minecraft:mushroom_stew"),
            "mushroom stew restored 10 HP, not its modern nutrition of 6");
        assertEquals(3, HistoricalFoodHelper.researchedHeal("minecraft:porkchop"),
            "raw porkchop restored 3 HP");
        assertEquals(8, HistoricalFoodHelper.researchedHeal("minecraft:cooked_porkchop"),
            "cooked porkchop restored 8 HP");
        assertEquals(1, HistoricalFoodHelper.researchedHeal("minecraft:cookie"),
            "a cookie restored 1 HP, not its modern nutrition of 2");
    }

    @Test
    void foodsWithoutAResearchedValueFallBackRatherThanGuess() {
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:cooked_cod"),
            "the research gives no direct-heal value for fish, so the port must fall back");
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:golden_apple"),
            "the golden apple is recorded as a pre-1.8 food but the entry gives it no figure");
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:rotten_flesh"),
            "rotten flesh did not exist before Beta 1.8");
    }
}
'''
(root / 'src/test/java/net/historicalexperience/gameplay/HistoricalFoodHealTableTest.java').write_text(
    TEST, encoding='utf-8')
print('test rewritten against the id-keyed table')

"""Remove a contradiction I just introduced, and test the researched values.

The map I wrote includes GOLDEN_APPLE at 4 while the file's own note says the golden apple keeps the fallback. The
value happens to equal modern nutrition, so the behaviour is identical either way - but a note that contradicts
the code is exactly the kind of thing that misleads the next reader, and this file has already been caught twice
for that.

The test is the point of the round: the seven values are the deliverable, so they get asserted rather than trusted.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java'
text = helper.read_text(encoding='utf-8')
old = '''        net.minecraft.world.item.Items.COOKIE, 1,
        net.minecraft.world.item.Items.GOLDEN_APPLE, 4);'''
new = '''        net.minecraft.world.item.Items.COOKIE, 1);'''
assert old in text, 'golden apple anchor not found'
helper.write_text(text.replace(old, new, 1), encoding='utf-8')
print('removed the golden apple, which the note already said was on the fallback')

TEST = '''package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

/**
 * Asserts the direct-heal values, which are data rather than logic.
 *
 * <p>Source: research/historical-features.json, entry {@code beta173-instant-food-healing} (confidence: high). The
 * two that differ from modern nutrition are the reason this table exists: a cookie restored 1 HP where modern
 * nutrition says 2, and mushroom stew restored 10 where modern says 6.
 */
class HistoricalFoodHealTableTest {
    private static int healOf(ItemStack stack) {
        return HistoricalFoodHelper.directHealAmount(stack, stack.get(DataComponents.FOOD));
    }

    @Test
    void researchedFoodsUseTheirEraValues() {
        assertEquals(4, healOf(new ItemStack(Items.APPLE)), "apple restored 4 HP from Indev 20100110");
        assertEquals(5, healOf(new ItemStack(Items.BREAD)), "bread restored 5 HP from Indev 20100206");
        assertEquals(10, healOf(new ItemStack(Items.MUSHROOM_STEW)),
            "mushroom stew restored 10 HP, not its modern nutrition of 6");
        assertEquals(3, healOf(new ItemStack(Items.PORKCHOP)), "raw porkchop restored 3 HP");
        assertEquals(8, healOf(new ItemStack(Items.COOKED_PORKCHOP)), "cooked porkchop restored 8 HP");
        assertEquals(1, healOf(new ItemStack(Items.COOKIE)),
            "a cookie restored 1 HP, not its modern nutrition of 2");
    }

    @Test
    void foodsWithoutAResearchedValueKeepTheNutritionFallback() {
        ItemStack fish = new ItemStack(Items.COOKED_COD);
        FoodProperties food = fish.get(DataComponents.FOOD);

        assertEquals(food.nutrition(), healOf(fish),
            "a food the research gives no direct-heal value for must fall back to nutrition rather than guess");
        assertEquals(food.nutrition(), healOf(new ItemStack(Items.GOLDEN_APPLE)),
            "the golden apple is recorded as a pre-1.8 food but the entry gives it no figure");
    }
}
'''
(root / 'src/test/java/net/historicalexperience/gameplay/HistoricalFoodHealTableTest.java').write_text(
    TEST, encoding='utf-8')
print('wrote the heal table test')

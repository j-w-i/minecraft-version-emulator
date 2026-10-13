package net.historicalexperience.gameplay;

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

    /**
     * The three figures this test used to assert were ABSENT are present, and were present all along.
     *
     * <p>It asserted that fish and the golden apple "fall back rather than guess", which described a live table
     * that held only six of the registry's nine researched values. The registry's table carries client-jar
     * evidence for each one - {@code gm.aS = new yw(93,2,false)}, {@code gm.aT = new yw(94,5,false)},
     * {@code gm.ar = new yw(66,42,false)} - so the fallback was not caution, it was three foods healing by modern
     * nutrition in a Beta 1.7.3 world. A test that ratifies a bug is worse than no test, because it makes the bug
     * look deliberate.
     */
    @Test
    void everyResearchedFoodHasItsFigure() {
        assertEquals(2, HistoricalFoodHelper.researchedHeal("minecraft:cod"),
            "raw fish restored 2 HP");
        assertEquals(5, HistoricalFoodHelper.researchedHeal("minecraft:cooked_cod"),
            "cooked fish restored 5 HP");
        assertEquals(42, HistoricalFoodHelper.researchedHeal("minecraft:golden_apple"),
            "the golden apple restored 42 HP before Beta 1.8, which is why it was the expensive healing item");
    }

    @Test
    void foodsTheClientDidNotHaveHaveNoFigure() {
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:rotten_flesh"),
            "the research records rotten flesh as absent from the Beta 1.7.3 client's food registry");
        assertNull(HistoricalFoodHelper.researchedHeal("minecraft:cooked_beef"),
            "beef is in the same absent list, so there is no historical figure to apply");
    }
}

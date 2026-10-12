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

package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit.
 *
 * <p>These tests pin the two things that make the food rule a rule rather than a table: the values came
 * from the hash-verified client with a citation each, and the rule applies only to versions that had no
 * hunger. The second matters as much as the first - a version that HAS hunger must declare no direct
 * heals, because otherwise both paths would apply to the same meal and the player would be healed twice.
 */
class HistoricalFoodRulesTest {
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";
    private static final String BETA_1_8_1 = "historical_experience:beta_1_8_1";

    @Test
    @DisplayName("the applied heal table is exactly the verified Beta 1.7.3 values")
    void tableMatchesTheClient() {
        Map<String, Integer> heals = HistoricalFoodRules.healPoints(BETA_1_7_3);

        // Spot-checked against the client's own literals rather than against the summary: if a
        // transcription slips, one of these numbers changes and this fails.
        assertEquals(4, heals.get("minecraft:apple"), "gm.h = new yw(4,4,false)");
        assertEquals(5, heals.get("minecraft:bread"), "gm.S = new yw(41,5,false)");
        assertEquals(10, heals.get("minecraft:mushroom_stew"), "gm.D = new bz(26,10)");
        assertEquals(3, heals.get("minecraft:porkchop"), "gm.ao = new yw(63,3,true)");
        assertEquals(8, heals.get("minecraft:cooked_porkchop"), "gm.ap = new yw(64,8,true)");
        assertEquals(1, heals.get("minecraft:cookie"), "gm.ba = new qk(101,1,false,8)");
        assertEquals(2, heals.get("minecraft:cod"), "gm.aS = new yw(93,2,false)");
        assertEquals(5, heals.get("minecraft:cooked_cod"), "gm.aT = new yw(94,5,false)");

        // The golden apple's 42 exceeds the 20-point health cap on purpose, so one always refills a full
        // bar. A "sensible" 20 here would be a silent behaviour change, so it is pinned explicitly.
        assertEquals(42, heals.get("minecraft:golden_apple"), "gm.ar = new yw(66,42,false)");

        assertEquals(9, heals.size(),
            "nine applied heals: the ten verified items minus cake, which healed through a block");
    }

    @Test
    @DisplayName("cake is recorded but deliberately not applied, and the omission is visible")
    void cakeIsRecordedButNotApplied() {
        assertTrue(HistoricalFoodRules.BETA_1_7_3.stream()
                .anyMatch(heal -> heal.itemId().equals(HistoricalFoodRules.UNMODELLED_BLOCK_FOOD)),
            "cake must stay in the record: dropping it would hide a known gap");
        assertFalse(HistoricalFoodRules.healPoints(BETA_1_7_3)
                .containsKey(HistoricalFoodRules.UNMODELLED_BLOCK_FOOD),
            "cake healed through right-clicking a placed block, a different path from eating, so "
                + "applying it here would heal a player for eating a cake item that does not exist");
        assertTrue(HistoricalFoodRules.diagnostics().get("notModelled")
                .contains(HistoricalFoodRules.UNMODELLED_BLOCK_FOOD),
            "the unmodelled case must be reported, not merely commented");
    }

    @Test
    @DisplayName("every heal value carries its client citation and is positive")
    void everyValueIsCited() {
        for (HistoricalFoodRules.FoodHeal heal : HistoricalFoodRules.BETA_1_7_3) {
            assertNotNull(heal.evidence(), "no evidence for " + heal.itemId());
            assertFalse(heal.evidence().isBlank(), "blank evidence for " + heal.itemId());
            // The citation has to name where it came from, or it is not usable as evidence.
            assertTrue(heal.evidence().contains("gm") || heal.evidence().contains("uu"),
                "evidence for " + heal.itemId() + " names no client location: " + heal.evidence());
            assertTrue(heal.healPoints() > 0, "non-positive heal for " + heal.itemId());
        }
    }

    @Test
    @DisplayName("only a version without hunger heals food directly, and it always may eat")
    void ruleAppliesOnlyWhereThereWasNoHunger() {
        assertTrue(HistoricalFoodRules.healsFoodDirectly(BETA_1_7_3));
        assertTrue(HistoricalFoodRules.allowsEatingWithoutHunger(BETA_1_7_3));

        // A version with hunger heals through the food level. Declaring direct heals here would apply
        // both paths to one meal.
        assertFalse(HistoricalFoodRules.healsFoodDirectly(BETA_1_8_1));
        assertFalse(HistoricalFoodRules.allowsEatingWithoutHunger(BETA_1_8_1));
        assertTrue(HistoricalFoodRules.healPoints(BETA_1_8_1).isEmpty());

        // An unknown world is left with modern behaviour, which is what keeps ordinary worlds untouched.
        assertFalse(HistoricalFoodRules.healsFoodDirectly(null));
        assertFalse(HistoricalFoodRules.healsFoodDirectly("historical_experience:release_9_9_9"));
        assertFalse(HistoricalFoodRules.allowsEatingWithoutHunger(null));
        assertFalse(HistoricalFoodRules.healsFoodDirectly("moderner_beta:beta"));
    }

    @Test
    @DisplayName("no version with hunger declares direct food heals")
    void hungerAndDirectHealingAreMutuallyExclusive() {
        for (var version : net.historicalexperience.version.HistoricalVersions.all()) {
            if (version.rules().hasHunger()) {
                assertTrue(version.rules().foodHealPoints().isEmpty(),
                    version.id() + " has hunger and must not declare direct heals; both paths would "
                        + "apply to the same meal");
            }
        }
        // Derived rather than pinned to one id: the set of versions that declare direct heals is exactly
        // the set this class reports, and both Alpha and Beta 1.7.3 are expected in it because both have
        // had their values extracted from their own verified clients.
        assertEquals(
            net.historicalexperience.version.HistoricalVersions.all().stream()
                .filter(version -> !version.rules().foodHealPoints().isEmpty())
                .map(net.historicalexperience.version.HistoricalVersion::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()),
            HistoricalFoodRules.healingProfiles());
        assertTrue(HistoricalFoodRules.healingProfiles().contains(BETA_1_7_3));
        assertTrue(HistoricalFoodRules.healingProfiles().contains("historical_experience:alpha"),
            "Alpha's six verified food values must make it a healing version");
    }

    @Test
    @DisplayName("the eat gate follows from the absence of hunger, not from having researched heals")
    void eatGateFollowsFromTheAbsenceOfHunger() {
        for (var version : net.historicalexperience.version.HistoricalVersions.all()) {
            assertEquals(!version.rules().hasHunger(),
                HistoricalFoodRules.allowsEatingWithoutHunger(version.id()),
                version.id() + ": eating must be ungated exactly when there is no food level to gate on");
        }
    }

    @Test
    @DisplayName("lookup by item and version is total: anything unknown resolves to no heal")
    void lookupIsTotal() {
        assertEquals(5, HistoricalFoodRules.healPointsFor(BETA_1_7_3, "minecraft:bread").orElseThrow());
        assertTrue(HistoricalFoodRules.healPointsFor(BETA_1_7_3, "minecraft:diamond").isEmpty());
        assertTrue(HistoricalFoodRules.healPointsFor(BETA_1_7_3, "minecraft:cooked_beef").isEmpty(),
            "cooked beef is a modern item; it must not inherit a Beta value");
        assertTrue(HistoricalFoodRules.healPointsFor(BETA_1_8_1, "minecraft:bread").isEmpty());
        assertTrue(HistoricalFoodRules.healPointsFor(null, "minecraft:bread").isEmpty());
        assertTrue(HistoricalFoodRules.healPointsFor(BETA_1_7_3, null).isEmpty());
    }

    @Test
    @DisplayName("a non-positive heal is refused at construction rather than becoming a silent no-op")
    void nonPositiveHealIsRefused() {
        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalFoodRules.FoodHeal("minecraft:apple", "apple", 0, "gm.h"));
        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalFoodRules.FoodHeal("minecraft:apple", "apple", -1, "gm.h"));
        assertThrows(IllegalArgumentException.class,
            () -> new HistoricalFoodRules.FoodHeal("minecraft:apple", "apple", 4, "  "));
    }
}

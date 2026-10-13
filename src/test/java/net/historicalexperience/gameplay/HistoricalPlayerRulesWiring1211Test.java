package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The hunger and sprint rules, against every declared version.
 *
 * <p>The owner reported that hunger still drained in a Beta 1.7.3 world and assumed it was a missing visual
 * fix. It was not: {@code FoodData.tick} really drains the food level, so the mechanic had to be disabled and
 * not merely hidden. This is the test for that decision, written against the feature timeline rather than
 * against the code, and it is the same shape as the gameplay gate table - a version-keyed rule asserted for
 * all ten profiles so a version-registry change cannot silently reintroduce the system.
 */
class HistoricalPlayerRulesWiring1211Test {
    /** Oldest first. */
    private static final List<String> PROFILES = HistoricalVersions.all().stream()
        .sorted(java.util.Comparator.comparingInt(HistoricalVersion::chronology))
        .map(HistoricalVersion::id)
        .toList();

    /** The versions before the Beta 1.8 Pre-release, which is where hunger and sprinting both arrive. */
    private static final List<String> BEFORE_BETA_1_8 = HistoricalVersions.all().stream()
        .filter(version -> version.chronology()
            < HistoricalGameplayTweaks.preBeta18BoundaryChronology())
        .map(net.historicalexperience.version.HistoricalVersion::id)
        .toList();

    private static boolean hasHunger(String versionId) {
        return HistoricalVersions.byId(versionId).orElseThrow().rules().hasHunger();
    }

    @Test
    @DisplayName("the profile list is the twelve declared versions, oldest first")
    void profilesAreTheDeclaredVersions() {
        assertEquals(15, PROFILES.size(), "the project declares fifteen profiles: " + PROFILES);
        assertEquals("historical_experience:infdev_420", PROFILES.get(0));
        assertEquals("historical_experience:release_1_6_4", PROFILES.get(14));
    }

    @Test
    @DisplayName("the hunger tick is suppressed exactly in the versions that had no hunger system")
    void hungerFollowsTheVersion() {
        for (String profile : PROFILES) {
            boolean hunger = hasHunger(profile);
            // The two answers are one rule read twice, so a disagreement is a defect by construction.
            assertEquals(!hunger, HistoricalPlayerRules.suppressesHungerTick(profile),
                profile + " hasHunger=" + hunger + ", so suppressesHungerTick must be " + !hunger);
        }
        for (String early : BEFORE_BETA_1_8) {
            assertTrue(HistoricalPlayerRules.suppressesHungerTick(early),
                early + " predates the hunger system, so FoodData must not tick at all");
            assertFalse(hasHunger(early), early + " must not claim to have hunger");
        }
        for (String later : List.of("historical_experience:beta_1_8_1", "historical_experience:beta_1_9_pre_3",
                "historical_experience:release_1_6_4")) {
            assertFalse(HistoricalPlayerRules.suppressesHungerTick(later),
                later + " has hunger, so the tick must run and food must feed the bar");
        }
    }

    @Test
    @DisplayName("sprinting is refused exactly before Beta 1.8, and allowed from it")
    void sprintingFollowsTheVersion() {
        for (String early : BEFORE_BETA_1_8) {
            assertFalse(HistoricalPlayerRules.allowsSprinting(early),
                early + " predates the sprint key, so setSprinting must be refused");
        }
        for (String profile : PROFILES) {
            if (BEFORE_BETA_1_8.contains(profile)) {
                continue;
            }
            assertTrue(HistoricalPlayerRules.allowsSprinting(profile),
                profile + " is at or after the Beta 1.8 Pre-release, which introduced sprinting");
        }
    }

    @Test
    @DisplayName("an ordinary or undeclared world keeps vanilla hunger and sprinting")
    void ordinaryWorldsAreUntouched() {
        // An unknown version must not have the hunger tick suppressed: a missing entry is not a reason to
        // change an ordinary world's behaviour.
        assertFalse(HistoricalPlayerRules.suppressesHungerTick("moderner_beta:beta"));
        assertFalse(HistoricalPlayerRules.suppressesHungerTick(null));
        assertTrue(HistoricalPlayerRules.allowsSprinting("moderner_beta:beta"),
            "sprinting is allowed by default, which is what keeps an ordinary world normal");
        assertTrue(HistoricalPlayerRules.allowsSprinting(null));
        assertTrue(HistoricalPlayerRules.hasHunger("moderner_beta:beta"),
            "an undeclared version is treated as having hunger rather than silently losing it");
    }

    @Test
    @DisplayName("both new mixins are registered, so the rules are actually applied")
    void theRulesAreWired() throws Exception {
        // The registration check is generic in HistoricalLootWiring1211Test; this names the two that carry
        // these rules so a rename cannot quietly un-wire the hunger or sprint fix.
        String config = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/resources/historical_experience.mixins.json"));
        assertTrue(config.contains("FoodDataHistoricalHungerMixin"),
            "the hunger tick is not suppressed unless this mixin is registered");
        assertTrue(config.contains("LivingEntityHistoricalMovementMixin"),
            "sprinting is not refused unless this mixin is registered");
    }
}

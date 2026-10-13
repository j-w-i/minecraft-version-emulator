package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every behaviour gate, against every declared version.
 *
 * <p>This test exists because the gates were, until it, only reachable through a {@link
 * net.minecraft.world.level.Level} and therefore untestable offline. The gap is not theoretical: the panic
 * gate answered "applies" for <b>every</b> declared version, which would have removed animal panic from a
 * Release 1.6.4 world, and nothing objected. The gates now compute their boundary in one version-keyed
 * method, and this walks all of them.
 *
 * <p>Each expectation is written against the feature timeline, not against the code: the table is what the
 * research says, and a change to the code that disagrees with it should fail here.
 */
class HistoricalGameplayTweaksTest {
    /** Oldest first, one row per declared version. */
    private static final List<String> PROFILES = HistoricalVersions.all().stream()
        .sorted(java.util.Comparator.comparingInt(HistoricalVersion::chronology))
        .map(HistoricalVersion::id)
        .toList();

    /** Gates that arrive with the goal-based mob AI, so they apply only before Beta 1.8.1. */
    private static final List<String> BEFORE_BETA_1_8 =
        List.of("skeletonStrafing", "criticalHit", "panic");

    /** Gates that arrive in the Beta 1.9 Pre-releases, so they apply only before Beta 1.9 Prerelease 3. */
    private static final List<String> BEFORE_BETA_1_9_PRE_3 = List.of("breeding", "tempting", "babyAnimal");

    /** Gates dated by Release 1.4.2, proxied by Release 1.6.4 as the only declared version after it. */
    private static final List<String> BEFORE_RELEASE_1_6_4 =
        List.of("itemPickup", "monsterGear", "monsterEnchant", "babyZombie");

    /** The one gate dated by Release 1.2.1, proxied by Release 1.2.5. */
    private static final List<String> BEFORE_RELEASE_1_2_5 = List.of("avoidSun", "customDeathLoot");

    /**
     * 1.9 combat, which no declared profile has: the newest is Release 1.6.4, so the boundary sits above
     * every version rather than between two of them.
     */
    private static final List<String> ABSENT_FROM_EVERY_DECLARED_VERSION = List.of("cooldown", "sweep", "swim", "mobArms");

    private static int chronology(String versionId) {
        return HistoricalVersions.byId(versionId).orElseThrow().chronology();
    }

    @Test
    @DisplayName("the profile list is the thirty-two declared versions, oldest first")
    void profilesAreTheDeclaredVersions() {
        assertEquals(HistoricalVersions.all().size(), PROFILES.size(),
            "the list must be every declared profile: " + PROFILES);
        assertEquals(32, PROFILES.size(), "the project declares thirty-two profiles: " + PROFILES);
        assertEquals("historical_experience:infdev_420", PROFILES.get(0));
        assertEquals("historical_experience:release_1_6_4", PROFILES.get(PROFILES.size() - 1));
    }

    @Test
    @DisplayName("every gate answers for every profile, and agrees with the boundary the research records")
    void everyGateAgreesWithItsBoundary() {
        int beta181 = chronology("historical_experience:beta_1_8_1");
        int beta19pre3 = chronology("historical_experience:beta_1_9_pre_3");
        int release125 = chronology("historical_experience:release_1_2_5");
        int release164 = chronology("historical_experience:release_1_6_4");

        Map<String, Integer> boundary = new LinkedHashMap<>();
        BEFORE_BETA_1_8.forEach(gate -> boundary.put(gate, beta181));
        BEFORE_BETA_1_9_PRE_3.forEach(gate -> boundary.put(gate, beta19pre3));
        BEFORE_RELEASE_1_6_4.forEach(gate -> boundary.put(gate, release164));
        BEFORE_RELEASE_1_2_5.forEach(gate -> boundary.put(gate, release125));
        ABSENT_FROM_EVERY_DECLARED_VERSION.forEach(gate -> boundary.put(gate, Integer.MAX_VALUE));

        // Every gate the code knows must be in the table, and vice versa: an unlisted gate would be
        // answered without being checked, which is what this whole test is for.
        assertEquals(new java.util.TreeSet<>(HistoricalGameplayTweaks.gateNames()),
            new java.util.TreeSet<>(boundary.keySet()),
            "the gate list and the tested set disagree");

        for (String gate : HistoricalGameplayTweaks.gateNames()) {
            for (String profile : PROFILES) {
                boolean expected = chronology(profile) < boundary.get(gate);
                assertEquals(expected, HistoricalGameplayTweaks.appliesTo(gate, profile),
                    gate + " on " + profile + " must be " + expected + " (boundary is the profile at "
                        + boundary.get(gate) + ")");
            }
        }
    }

    @Test
    @DisplayName("the boundary is a proxy, so the proxy version itself keeps the behaviour")
    void theProxyVersionKeepsTheBehaviour() {
        // The point of a proxy: Release 1.4.2 introduced geared mobs and 1.6.4 stands in for it, so 1.6.4
        // must KEEP the behaviour. A gate that applied to 1.6.4 would delete content the version had.
        for (String gate : BEFORE_RELEASE_1_6_4) {
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, "historical_experience:release_1_6_4"),
                gate + " must not apply to the proxy version itself");
        }
        for (String gate : BEFORE_RELEASE_1_2_5) {
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, "historical_experience:release_1_2_5"),
                gate + " must not apply to its own proxy version");
        }
        for (String gate : BEFORE_BETA_1_9_PRE_3) {
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, "historical_experience:beta_1_9_pre_3"),
                gate + " must not apply to Beta 1.9 Prerelease 3, which introduced it");
        }
        for (String gate : BEFORE_BETA_1_8) {
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, "historical_experience:beta_1_8_1"),
                gate + " must not apply to Beta 1.8.1, which introduced it");
        }
    }

    @Test
    @DisplayName("an ordinary or unknown world is never restricted")
    void ordinaryWorldsAreUntouched() {
        for (String gate : HistoricalGameplayTweaks.gateNames()) {
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, null),
                gate + " must not apply to a world with no historical version");
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, "moderner_beta:beta"),
                gate + " must not apply to a plain Moderner Beta world");
            assertFalse(HistoricalGameplayTweaks.appliesTo(gate, "example:not_a_version"),
                gate + " must not apply to an undeclared version id");
        }
    }

    @Test
    @DisplayName("an unknown gate name is refused rather than answered false")
    void unknownGatesThrow() {
        // A silent false is indistinguishable from a gate that correctly does not apply, so the seam refuses.
        assertThrows(IllegalArgumentException.class,
            () -> HistoricalGameplayTweaks.appliesTo("no_such_gate", "historical_experience:alpha"));
        assertThrows(IllegalArgumentException.class,
            () -> HistoricalGameplayTweaks.appliesTo("", "historical_experience:alpha"));
    }

    @Test
    @DisplayName("the corrections this test was written for stay corrected")
    void recordedCorrectionsHold() {
        // Animal panic is Beta 1.8, NOT every version. The old gate said true everywhere, which would have
        // taken the behaviour away from Release 1.6.4 and everything after Beta 1.8.1.
        assertTrue(HistoricalGameplayTweaks.appliesTo("panic", "historical_experience:beta_1_7_3"));
        assertFalse(HistoricalGameplayTweaks.appliesTo("panic", "historical_experience:release_1_6_4"));
        // Baby zombies are Release 1.6.2, so only the newest declared profile has them.
        assertTrue(HistoricalGameplayTweaks.appliesTo("babyZombie", "historical_experience:release_1_2_5"));
        assertFalse(HistoricalGameplayTweaks.appliesTo("babyZombie", "historical_experience:release_1_6_4"));
        // Geared and enchanted mobs share one boundary, so they cannot drift apart.
        for (String profile : PROFILES) {
            assertEquals(HistoricalGameplayTweaks.appliesTo("monsterGear", profile),
                HistoricalGameplayTweaks.appliesTo("monsterEnchant", profile),
                "the gear and enchantment gates must agree on " + profile);
        }
    }
}

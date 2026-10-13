package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import net.historicalexperience.gameplay.HistoricalSpawnRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit.
 *
 * <p>An "empty allowed spawn set" used to mean one thing - unrestricted, i.e. this version has not been
 * researched - and a measured extraction showed it has to mean two. Infdev 20100227 never instantiates a
 * mob of any kind, so its correct rule is "nothing spawns naturally": an empty set that still restricts.
 * Read as "unrestricted", it would have given the least mob-populated version of the game <b>modern</b>
 * mobs and looked like success.
 *
 * <p>That build has since been <b>withdrawn</b>, because its upstream provider hangs the server during
 * world creation and a version that never loads must not be offered. The distinction it forced is kept
 * anyway, and pinned against constructed rule sets rather than a live version - losing the example would
 * have been the wrong lesson to take from the withdrawal.
 */
class SpawnRestrictionSemanticsTest {
    private static final String INFDEV_611 = "historical_experience:infdev_611";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";

    @Test
    @DisplayName("an empty allowed set that restricts means NOTHING may spawn, not that anything may")
    void emptyButRestrictedMeansNothingSpawns() {
        // The rule set Infdev 20100227 needed, constructed directly now that the version is withdrawn.
        var nothingSpawns = new HistoricalVersion.VersionRules(
            true, Set.of(), Map.of(), Map.of(), false, false, false, false,
            "constructed: the shape Infdev 20100227 needed, where no mob is ever instantiated");

        assertTrue(nothingSpawns.restrictsSpawns());
        assertTrue(nothingSpawns.allowedNaturalSpawns().isEmpty());

        var rule = new HistoricalSpawnRules.SpawnRule("synthetic", nothingSpawns.allowedNaturalSpawns(),
            nothingSpawns.evidence());
        // The observable consequence: nothing is allowed through. This is the assertion that would have
        // failed had the empty set been read as "unrestricted".
        for (String modern : Set.of("minecraft:zombie", "minecraft:cow", "minecraft:bat",
                "minecraft:turtle", "minecraft:enderman")) {
            assertFalse(HistoricalSpawnRules.isAllowed(rule, modern),
                modern + " must not be allowed through an empty but restricting rule set");
        }
        // And a null rule means no filtering at all, which is the opposite outcome - so the distinction
        // is observable at the call site, not merely in the record.
        assertTrue(HistoricalSpawnRules.isAllowed(null, "minecraft:turtle"),
            "no rule means no filtering, which is what an unresearched version gets");
    }

    @Test
    @DisplayName("an empty allowed set that does NOT restrict means unresearched, and leaves spawning alone")
    void emptyAndUnrestrictedMeansUnresearched() {
        // DERIVED, not named. This test used RELEASE_1_6_4 until that profile gained a researched mob set, which
        // was the third time a pinned version here went stale as the matrix filled in. Asking the registry for a
        // version that still makes no claim cannot go stale, and it fails loudly when none is left rather than
        // quietly testing a restricted version.
        var version = HistoricalVersions.all().stream()
            .filter(candidate -> !candidate.rules().restrictsSpawns())
            .findFirst()
            .orElseThrow(() -> new AssertionError("every version now has a researched mob set, so this test has "
                + "nothing left to check - delete it deliberately rather than weakening it"));

        assertTrue(version.rules().allowedNaturalSpawns().isEmpty(),
            version.id() + " does not restrict, so its allowed set must be empty. An EMPTY set that DOES restrict "
                + "is a different and meaningful state - nothing may spawn - and the two must not be confused");
        assertTrue(HistoricalSpawnRules.forProfile(version.id()).isEmpty(),
            version.id() + " does not restrict, so it must expose no rule at all and no filter runs");
    }

    @Test
    @DisplayName("the surviving Infdev build carries its extracted seven-mob set")
    void survivingInfdevSet() {
        assertEquals(HistoricalSpawnRules.INFDEV_20100420_AND_20100611_MOBS,
            HistoricalVersions.byId(INFDEV_611).orElseThrow().rules().allowedNaturalSpawns());
        assertTrue(HistoricalVersions.byId(INFDEV_611).orElseThrow().rules()
            .allowedNaturalSpawns().contains("minecraft:sheep"));
        assertTrue(HistoricalVersions.byId(INFDEV_611).orElseThrow().rules()
            .allowedNaturalSpawns().contains("minecraft:giant"));
    }

    @Test
    @DisplayName("the surviving Infdev builds have a VERIFIED absence of food")
    void infdevFoodIsVerifiedAbsent() {
        for (String id : Set.of(INFDEV_611, "historical_experience:infdev_420")) {
            var version = HistoricalVersions.byId(id).orElseThrow();
            assertTrue(version.rules().foodHealPoints().isEmpty(),
                id + " has no food items at all, so its table must be empty");
            assertFalse(version.rules().hasHunger(), id + " predates hunger entirely");
            // The evidence must record WHY it is empty, or a reader cannot tell this from an unfinished
            // extraction - which is exactly the confusion the wording exists to prevent.
            assertTrue(version.rules().evidence().contains("FOOD: verified ABSENT"),
                id + " must state that the absence is measured rather than pending");
            assertTrue(version.rules().evidence().contains("Omniarchive"),
                id + " must record its archive provenance");
        }
    }

    @Test
    @DisplayName("a rule set that does not restrict may not name allowed spawns")
    void contradictoryRuleSetIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new HistoricalVersion.VersionRules(
                false,
                Set.of("minecraft:zombie"),
                Map.of(),
                Map.of(),
                true, true, true, true,
                "contradictory on purpose"),
            "naming allowed spawns while claiming not to restrict would leave two answers to whether "
                + "anything is filtered");
    }

    @Test
    @DisplayName("the versions with researched rules all restrict, and the unresearched ones do not")
    void restrictionFollowsTheResearch() {
        assertTrue(HistoricalVersions.byId(BETA_1_7_3).orElseThrow().rules().restrictsSpawns());
        assertTrue(HistoricalVersions.byId("historical_experience:alpha").orElseThrow().rules()
            .restrictsSpawns());
        // The four terrain-only Release-era versions must be left alone: an unresearched mob set is not
        // a licence to invent one. Beta 1.9 Prerelease 3 was in this list and is no longer, because its
        // set is now INHERITED rather than invented: the research records Pre1 adding blaze, magma cube,
        // mooshroom and villagers, and Pre2 and Pre3 adding no mob at all, which is a researched answer.
        // Release 1.0.0 has left it for the same reason - it is the RELEASED FORM of that prerelease, so
        // its mob set IS the prerelease set. Declared terrain-only it set no restriction at all, which is
        // how Release worlds came to spawn modern mobs including piglins.
        assertTrue(HistoricalVersions.byId("historical_experience:release_1_0_0").orElseThrow().rules()
            .restrictsSpawns(),
            "Release 1.0.0 has a researched mob set - the prerelease one - and must restrict");
        for (var candidate : HistoricalVersions.all()) {
            if (candidate.rules().restrictsSpawns()) {
                continue;
            }
            assertTrue(candidate.rules().allowedNaturalSpawns().isEmpty(),
                candidate.id() + " does not restrict, so it must expose no allowed set at all. "
                    + "This list used to name versions one at a time and went stale three times as profiles gained "
                    + "researched sets; the invariant is what matters, not the names");
        }
        // The Beta 1.9 prereleases DO restrict, and that is the owner's point: an unrestricted Beta 1.9
        // world spawns modern 1.21.1 mobs, which is exactly what they reported.
        for (String id : Set.of("historical_experience:beta_1_9_pre_1",
                "historical_experience:beta_1_9_pre_2", "historical_experience:beta_1_9_pre_3")) {
            assertTrue(HistoricalVersions.byId(id).orElseThrow().rules().restrictsSpawns(),
                id + " has a researched mob set and must restrict, or modern mobs spawn there");
        }
        // Derived rather than counted by hand: pinning the number here would go stale on the next
        // version, and my first attempt at it was arithmetically wrong anyway.
        assertEquals(
            HistoricalVersions.all().stream()
                .filter(version -> version.rules().restrictsSpawns())
                .map(HistoricalVersion::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()),
            HistoricalSpawnRules.restrictedProfiles());
    }

    @Test
    @DisplayName("the two hanging Infdev builds are not offered, and their data is not lost")
    void withdrawnInfdevBuildsAreAbsentButRecorded() {
        assertTrue(HistoricalVersions.byId("historical_experience:infdev_227").isEmpty(),
            "20100227 hangs the server during world creation and must not be offered");
        assertTrue(HistoricalVersions.byId("historical_experience:infdev_325").isEmpty(),
            "20100325 hangs for the same reason");
        // Their extracted mob sets remain in the code, because the data is sound and only the upstream
        // provider is broken. Deleting it would mean extracting it again if that is ever fixed.
        assertEquals(Set.of(), HistoricalSpawnRules.INFDEV_20100227_MOBS,
            "20100227 instantiates no mob of any kind");
        assertEquals(6, HistoricalSpawnRules.INFDEV_20100325_MOBS.size(),
            "20100325's six mobs are preserved even though the version is withdrawn");
    }
}

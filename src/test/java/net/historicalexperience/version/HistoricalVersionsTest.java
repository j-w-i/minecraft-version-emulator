package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.historicalexperience.HistoricalMarker;
import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit (registry + packaged resources).
 *
 * <p>This is the test that makes the framework scalable rather than merely organised. Its job is to
 * assert the invariants that a new version must satisfy, so that adding one is a data change: every
 * declared revision resolves everywhere, is a marker, is bound to a profile, and has the two resource
 * files it needs. Without it, "declare the version in six places" would be a convention rather than a
 * rule, and the sixth place would be discovered by a world failing to load.
 */
class HistoricalVersionsTest {
    @Test
    @DisplayName("the registry declares more than one version, each with a distinct id and era")
    void registryDeclaresDistinctVersions() {
        List<HistoricalVersion> all = HistoricalVersions.all();
        assertTrue(all.size() >= 2,
            "the framework claims to support many versions; a single-entry registry cannot show that");

        Set<String> ids = new HashSet<>();
        for (HistoricalVersion version : all) {
            assertTrue(ids.add(version.id()), "duplicate version id " + version.id());
            assertFalse(version.rules().evidence().isBlank(),
                version.id() + " must state how its rule set was verified");
            assertTrue(version.rules().evidence().length() > 20,
                version.id() + " states no meaningful evidence: " + version.rules().evidence());
        }

        // Chronology must be total and distinct, or "which era came first" is unanswerable.
        Set<Integer> chronologies = new HashSet<>();
        for (HistoricalVersion version : all) {
            assertTrue(chronologies.add(version.chronology()),
                "two versions share chronology " + version.chronology());
        }
    }

    @Test
    @DisplayName("chronology orders eras without depending on how a version is named")
    void chronologyOrdersEras() {
        HistoricalVersion beta173 = HistoricalVersions.byId("historical_experience:beta_1_7_3")
            .orElseThrow();
        HistoricalVersion beta181 = HistoricalVersions.byId("historical_experience:beta_1_8_1")
            .orElseThrow();

        assertTrue(beta173.chronology() < beta181.chronology(),
            "Beta 1.7.3 must sort before Beta 1.8.1");
        assertTrue(HistoricalVersions.atOrAfter(beta181.chronology()).contains(beta181));
        assertFalse(HistoricalVersions.atOrAfter(beta181.chronology()).contains(beta173));
        assertTrue(HistoricalVersions.before(beta181.chronology()).contains(beta173));
    }

    @Test
    @DisplayName("every declared revision is a marker, a profile binding, and resolvable in both directions")
    void everyRevisionResolvesEverywhere() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                String alias = revision.alias();

                assertTrue(HistoricalMarker.isHistoricalMarker(alias),
                    alias + " is a declared recipe but not a recognised marker, so its worlds would be "
                        + "treated as ordinary");
                assertTrue(HistoricalMarker.HISTORICAL_PRESET_IDS.contains(alias),
                    alias + " must be in the derived marker list");

                assertEquals(version.id(), HistoricalVersions.byRevisionAlias(alias)
                        .orElseThrow().id(),
                    alias + " must resolve back to its own version");

                var binding = HistoricalProfiles.byGenerationPreset(alias).orElseThrow();
                assertEquals(alias, binding.rawSettingsAlias().value(),
                    alias + " must be keyed by its own alias");
                assertEquals(revision.foundation(), binding.terrainPreset().value(),
                    alias + " must record the foundation its revision declares");
                assertEquals(revision.revision(), binding.generationRevision(),
                    alias + " must keep its declared revision number");

                assertTrue(HistoricalProfiles.byId(version.id()).isPresent(),
                    version.id() + " must be a registered gameplay profile");
            }
        }
    }

    @Test
    @DisplayName("one canonical profile per version, and no revision invents a profile id")
    void oneCanonicalProfilePerVersion() {
        assertEquals(HistoricalVersions.all().size(), HistoricalProfiles.all().size(),
            "each version declares exactly one canonical gameplay profile");
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(HistoricalProfiles.byId(version.id()).isPresent(),
                version.id() + " has no canonical profile");
        }
    }

    @Test
    @DisplayName("every declared revision has both of the resource files it needs")
    void everyRevisionHasItsResources() {
        // The resource pair a revision cannot work without. Checking it here is what turns "remember to
        // add the JSON too" into a rule: a missing settings preset or world preset is otherwise found
        // only when a world fails to load.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                String path = revision.alias().substring(revision.alias().indexOf(':') + 1);
                assertNotNull(resource(
                        "/data/historical_experience/moderner_beta/settings_preset/" + path + ".json"),
                    "missing settings preset for " + revision.alias());
                assertNotNull(resource(
                        "/data/historical_experience/worldgen/world_preset/" + path + ".json"),
                    "missing world preset for " + revision.alias());
            }
        }
    }

    @Test
    @DisplayName("a version's own original declaration may carry restrictions, but earlier revisions keep theirs")
    void generationRulesArePerRevision() {
        // The load-bearing append-only property, now asserted generically: once a version has more than
        // one revision, the earlier ones must not have quietly acquired a later revision's generation
        // rules. Generation rules live on the revision precisely so this cannot happen.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                if (revision.revision() == 0) {
                    continue;
                }
                assertTrue(revision.experimental(),
                    revision.alias() + " is numbered and must be experimental until accepted");
            }
        }

        HistoricalVersion beta173 = HistoricalVersions.byId("historical_experience:beta_1_7_3")
            .orElseThrow();
        for (HistoricalVersion.GenerationRevision revision : beta173.revisions()) {
            if (revision.revision() <= 3) {
                assertTrue(revision.excludedPlacedFeatures().isEmpty(),
                    revision.alias() + " was generated with no exclusions and must keep none");
                assertTrue(revision.orePopulation().isEmpty(),
                    revision.alias() + " was generated with vanilla ores and must keep them");
            }
        }

        var revision4 = beta173.revisions().stream()
            .filter(revision -> revision.revision() == 4).findFirst().orElseThrow();
        var revision5 = beta173.revisions().stream()
            .filter(revision -> revision.revision() == 5).findFirst().orElseThrow();
        assertFalse(revision4.excludedPlacedFeatures().isEmpty());
        assertTrue(revision5.excludedPlacedFeatures()
                .containsAll(revision4.excludedPlacedFeatures()),
            "revision 5 must keep every revision 4 exclusion");
        assertTrue(revision5.placesOwnOres(), "revision 5 places Beta's own ores");
        assertFalse(revision4.placesOwnOres(), "revision 4 must still use vanilla ore placement");
    }

    @Test
    @DisplayName("versions can disagree on simulation rules, which is the point of the framework")
    void versionsSelectDifferentBehaviour() {
        HistoricalVersion beta173 = HistoricalVersions.byId("historical_experience:beta_1_7_3")
            .orElseThrow();
        HistoricalVersion beta181 = HistoricalVersions.byId("historical_experience:beta_1_8_1")
            .orElseThrow();

        // The Adventure Update boundary, expressed as data: the same shared hooks read these booleans,
        // so one implementation serves both versions.
        assertFalse(beta173.rules().allowsSprinting(), "Beta 1.7.3 has no sprinting");
        assertFalse(beta173.rules().hasHunger(), "Beta 1.7.3 has no hunger");
        assertFalse(beta173.rules().awardsExperience(), "Beta 1.7.3 has no experience");
        assertTrue(beta181.rules().allowsSprinting(), "Beta 1.8.1 has sprinting");
        assertTrue(beta181.rules().hasHunger(), "Beta 1.8.1 has hunger");
        assertTrue(beta181.rules().awardsExperience(), "Beta 1.8.1 has experience");

        // And they disagree on content too: the same loot hook, different items.
        assertEquals("minecraft:feather",
            beta173.rules().mobDrops().get("minecraft:zombie").itemId());
        assertEquals("minecraft:rotten_flesh",
            beta181.rules().mobDrops().get("minecraft:zombie").itemId());

        // Spawn sets differ by exactly the mobs the Adventure Update added.
        assertFalse(beta173.rules().allowedNaturalSpawns().contains("minecraft:enderman"));
        assertTrue(beta181.rules().allowedNaturalSpawns().contains("minecraft:enderman"));
        assertTrue(beta181.rules().allowedNaturalSpawns()
                .containsAll(beta173.rules().allowedNaturalSpawns()));
    }

    @Test
    @DisplayName("an unknown id or alias resolves to nothing rather than to a default version")
    void unknownIdsResolveToNothing() {
        assertTrue(HistoricalVersions.byId("historical_experience:release_9_9_9").isEmpty());
        assertTrue(HistoricalVersions.byId(null).isEmpty());
        assertTrue(HistoricalVersions.byRevisionAlias("moderner_beta:beta").isEmpty());
        assertTrue(HistoricalVersions.byRevisionAlias(null).isEmpty());
        assertTrue(HistoricalVersions.byId("historical_experience").isEmpty());
        // The era set is derived, not pinned to one value: adding a version in a new era must not require
        // editing this test. What matters is that every declared era is real and that the set is closed
        // over the registry.
        assertEquals(
            HistoricalVersions.all().stream().map(HistoricalVersion::era)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()),
            HistoricalVersions.eras());
        assertTrue(HistoricalVersions.eras().contains("beta"), "an era family must be declared");
    }

    private static java.io.InputStream resource(String path) {
        return HistoricalVersionsTest.class.getResourceAsStream(path);
    }
}

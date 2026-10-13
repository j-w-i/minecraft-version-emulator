package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.generation.HistoricalGenerationScope;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The Nether spawn lists a historical world is handed must be filtered to its era.
 *
 * <p>The three mobs the owner reported - <b>strider, piglin, hoglin</b> - arrive with the 1.16 Nether Update.
 * The lists below are transcribed from 1.21.1's shipped biome data, which is what the live hook actually
 * receives:
 *
 * <pre>
 *   nether_wastes.json    creature: strider   monster: ghast, zombified_piglin, magma_cube, enderman, piglin
 *   crimson_forest.json   creature: strider   monster: zombified_piglin, hoglin, piglin
 * </pre>
 *
 * <p>A Nether fortress returns its own {@code FORTRESS_ENEMIES} list instead - blaze, zombified piglin, wither
 * skeleton, skeleton, magma cube - so no piglin or hoglin reaches a world by that path; those two arrive
 * through the biome lists above.
 *
 * <p>Level: unit, entity-id based. {@code HistoricalSpawnRules} is deliberately free of Minecraft types, so
 * each entry's verdict is provable without a bootstrapped registry. The runtime {@code mobsAt} probe covers
 * the part this cannot: that the injected path is the one asking.
 */
class NetherSpawnFilteringTest {
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";
    private static final String BETA_1_9_PRE_1 = "historical_experience:beta_1_9_pre_1";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";
    private static final String NETHER = "minecraft:the_nether";
    private static final String ENDERMAN = "minecraft:enderman";

    private static final List<String> MODERN_NETHER_MOBS =
        List.of("minecraft:strider", "minecraft:piglin", "minecraft:hoglin");

    /** 1.21.1 {@code nether_wastes} monsters, in file order. */
    private static final List<String> NETHER_WASTES_MONSTERS = List.of(
        "minecraft:ghast", "minecraft:zombified_piglin", "minecraft:magma_cube",
        "minecraft:enderman", "minecraft:piglin");

    /** 1.21.1 {@code crimson_forest} monsters, in file order. */
    private static final List<String> CRIMSON_FOREST_MONSTERS = List.of(
        "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:piglin");

    /** The first version whose Nether exists at all: Alpha 1.2.0, the Halloween Update. */
    private static final int NETHER_ARRIVES = HistoricalVersions.chronology(400_000, 1, 2, 0);

    @AfterEach
    void clearScope() {
        HistoricalGenerationScope.clear();
    }

    private static HistoricalSpawnRules.SpawnRule rule(String profileId) {
        return HistoricalSpawnRules.forProfile(profileId).orElseThrow(
            () -> new AssertionError(profileId + " declares no spawn rule, so nothing would be filtered"));
    }

    /** The verdicts the live hook reaches, in encounter order. */
    private static List<String> denied(String profileId, String dimensionId, List<String> candidates) {
        HistoricalSpawnRules.SpawnRule rule = rule(profileId);
        return candidates.stream()
            .filter(id -> !HistoricalSpawnFilter.isAllowedInDimension(rule, profileId, dimensionId, id))
            .toList();
    }

    private static List<String> netherWastelands(String profileId) {
        return denied(profileId, NETHER, NETHER_WASTES_MONSTERS);
    }

    @Test
    @DisplayName("Beta 1.7.3 strips the whole modern nether_wastes list down to ghast and pigman")
    void betaNetherWastesIsFilteredToTheEra() {
        // Beta 1.7.3's own Nether had exactly two of these: the ghast and the zombie pigman, both from Alpha
        // 1.2.0. Magma cubes arrive in Beta 1.9 Pre1, the enderman does not reach the Nether until 1.10
        // (16w20a), and the piglin is 1.16. A first version of this test expected only the piglin and was
        // wrong: it read the list as "modern entries" rather than as this version's era.
        assertEquals(List.of("minecraft:magma_cube", ENDERMAN, "minecraft:piglin"),
            netherWastelands(BETA_1_7_3));

        // The two the era did have, asserted by name so a set that dropped either fails here.
        for (String era : List.of("minecraft:ghast", "minecraft:zombified_piglin")) {
            assertTrue(HistoricalSpawnRules.isAllowed(rule(BETA_1_7_3), era),
                era + " is Alpha 1.2.0 content and must keep spawning in a Beta 1.7.3 Nether");
        }
    }

    @Test
    @DisplayName("later versions keep progressively more of the same list")
    void laterVersionsKeepTheirOwnNetherMobs() {
        // Beta 1.9 Pre1 adds the magma cube, so it goes back on the list; the enderman and piglin do not.
        assertEquals(List.of(ENDERMAN, "minecraft:piglin"), netherWastelands(BETA_1_9_PRE_1));
        // Release 1.6.4 keeps the magma cube as well, leaving only the 1.10 enderman and the 1.16 piglin.
        assertEquals(List.of(ENDERMAN, "minecraft:piglin"), netherWastelands(RELEASE_1_6_4));

        // Wither skeleton is Release 1.4 content: refused in Beta, allowed in a later Release.
        assertFalse(HistoricalSpawnRules.isAllowed(rule(BETA_1_9_PRE_1), "minecraft:wither_skeleton"));
        assertTrue(HistoricalSpawnRules.isAllowed(rule(RELEASE_1_6_4), "minecraft:wither_skeleton"));
    }

    @Test
    @DisplayName("a crimson_forest list loses both 1.16 mobs and keeps the pigman")
    void crimsonForestIsFiltered() {
        assertEquals(List.of("minecraft:hoglin", "minecraft:piglin"),
            denied(BETA_1_7_3, NETHER, CRIMSON_FOREST_MONSTERS));
    }

    @Test
    @DisplayName("strider, piglin and hoglin are allowed by no declared version")
    void modernNetherMobsAreAllowedNowhere() {
        for (String profileId : HistoricalSpawnRules.restrictedProfiles()) {
            for (String modern : MODERN_NETHER_MOBS) {
                assertFalse(HistoricalSpawnRules.isAllowed(rule(profileId), modern),
                    modern + " is 1.16 content and must not spawn in " + profileId);
                // The dimension rule must not be the only thing standing between a version and a 1.16 mob.
                assertFalse(HistoricalSpawnFilter.isAllowedInDimension(
                        rule(profileId), profileId, NETHER, modern),
                    modern + " must be refused in the Nether of " + profileId);
            }
        }
    }

    @Test
    @DisplayName("the two Nether mobs Alpha 1.2.0 introduced survive every version that has a Nether")
    void eraNetherMobsSurviveWhereTheNetherExists() {
        // The filter REMOVES anything absent from the allow set, so an allow set missing either mob deletes it
        // from every historical Nether rather than merely failing to add it. Only versions at or after the
        // Halloween Update have a Nether at all; the Infdev and early Alpha profiles predate it and are
        // deliberately not asserted here.
        int checked = 0;
        for (HistoricalVersion version : HistoricalVersions.all()) {
            if (version.chronology() < NETHER_ARRIVES) {
                continue;
            }
            checked++;
            for (String era : List.of("minecraft:ghast", "minecraft:zombified_piglin")) {
                assertTrue(HistoricalSpawnRules.isAllowed(rule(version.id()), era),
                    version.id() + " has a Nether and must allow the " + era + " Alpha 1.2.0 introduced");
            }
        }
        assertTrue(checked > 0, "no declared version has a Nether, so this asserted nothing");
        // And the versions below the boundary are genuinely excluded rather than silently passing.
        assertTrue(HistoricalVersions.byId("historical_experience:alpha").orElseThrow().chronology()
            < NETHER_ARRIVES, "Alpha v1.1.2_01 predates the Nether and must be skipped");
    }

    @Test
    @DisplayName("the Nether enderman boundary is Release 1.10.0 on the chronology scheme")
    void netherEndermanBoundaryIsDated() {
        // Cited, not guessed: 16w20a's change list records "Enderman can spawn in the Nether"
        // (https://minewiki.pl/16w20a#1), and 16w20a is a 1.10 snapshot.
        //
        // The NUMBER must be this project's own scheme or it means nothing. chronology is
        // era + major * 1000 + minor * 100 + patch, so 1.10.0 is 600000 + 1000 + 1000 + 0 = 602000. An earlier
        // version of the constant used 1010000, a scale this scheme never produces: it happened to leave every
        // declared version on the same side of the comparison while being meaningless. Both forms are pinned.
        assertEquals(602_000, HistoricalSpawnRules.NETHER_ENDERMAN_CHRONOLOGY);
        assertEquals(HistoricalVersions.chronology(600_000, 1, 10, 0),
            HistoricalSpawnRules.NETHER_ENDERMAN_CHRONOLOGY,
            "the boundary must be what the registry's own chronology function returns for 1.10.0");

        // Above every declared version, and answering false for anything undeclared.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(version.chronology() < HistoricalSpawnRules.NETHER_ENDERMAN_CHRONOLOGY,
                version.id() + " (chronology " + version.chronology() + ") is at or after 1.10; the Nether "
                    + "enderman rule must then be revisited rather than assumed");
        }
        assertFalse(HistoricalSpawnRules.netherEndermenAllowed(BETA_1_7_3));
        assertFalse(HistoricalSpawnRules.netherEndermenAllowed(RELEASE_1_6_4),
            "the runtime probe saw this defect in a Release 1.0.0 Nether, so the newest Release must refuse it");
        assertFalse(HistoricalSpawnRules.netherEndermenAllowed("moderner_beta:beta"),
            "an undeclared profile must answer false rather than falling through to allowed");
        assertFalse(HistoricalSpawnRules.netherEndermenAllowed(null));
    }

    @Test
    @DisplayName("endermen are refused in the Nether only, and only that mob is affected")
    void endermenAreDimensionDependent() {
        for (String profileId : HistoricalSpawnRules.restrictedProfiles()) {
            HistoricalSpawnRules.SpawnRule rule = rule(profileId);
            assertFalse(HistoricalSpawnFilter.isAllowedInDimension(rule, profileId, NETHER, ENDERMAN),
                profileId + " must not spawn endermen in the Nether: they arrive there in 16w20a");
            // Overworld and End answers stay exactly the version's own set.
            for (String dimension : List.of("minecraft:overworld", "minecraft:the_end")) {
                assertEquals(HistoricalSpawnRules.isAllowed(rule, ENDERMAN),
                    HistoricalSpawnFilter.isAllowedInDimension(rule, profileId, dimension, ENDERMAN),
                    profileId + ": " + dimension + " must be untouched by the Nether rule");
            }
            // And every other nether_wastes entry keeps its version verdict in the Nether.
            for (String id : NETHER_WASTES_MONSTERS) {
                if (id.equals(ENDERMAN)) {
                    continue;
                }
                assertEquals(HistoricalSpawnRules.isAllowed(rule, id),
                    HistoricalSpawnFilter.isAllowedInDimension(rule, profileId, NETHER, id),
                    id + " must keep its version verdict in the Nether of " + profileId);
            }
        }
        // The Overworld half is not vacuous: the enderman is Beta 1.8.1 content.
        assertTrue(HistoricalSpawnRules.isAllowed(rule(RELEASE_1_6_4), ENDERMAN));
        assertFalse(HistoricalSpawnRules.isAllowed(rule(BETA_1_7_3), ENDERMAN));
        // An unknown dimension adds nothing.
        assertTrue(HistoricalSpawnFilter.isAllowedInDimension(
            rule(RELEASE_1_6_4), RELEASE_1_6_4, null, ENDERMAN));
    }

    @Test
    @DisplayName("no rule means no filtering, in the Nether as anywhere else")
    void nullRuleAllowsEverythingEverywhere() {
        // The dimension rule is an ADDITION to a version's set, so with no version there is nothing to add it
        // to. Returning false here would make an ordinary world's Nether refuse endermen through a helper the
        // live path never calls with a null rule - an inconsistency that would spread the moment a caller did.
        for (String dimension : List.of(NETHER, "minecraft:overworld", "minecraft:the_end")) {
            for (String mob : List.of(ENDERMAN, "minecraft:strider", "minecraft:piglin",
                    "minecraft:zombified_piglin")) {
                assertTrue(HistoricalSpawnFilter.isAllowedInDimension(null, null, dimension, mob),
                    mob + " must be allowed with no rule, in " + dimension);
            }
        }
        // And the two helpers must agree, which is the property that actually matters.
        for (String mob : List.of(ENDERMAN, "minecraft:strider")) {
            assertEquals(HistoricalSpawnRules.isAllowed(null, mob),
                HistoricalSpawnFilter.isAllowedInDimension(null, null, NETHER, mob),
                mob + ": the dimension-aware verdict must match the plain one when there is no rule");
        }
    }

    @Test
    @DisplayName("a historical Nether resolves through the server scope, an ordinary one does not")
    void scopeDecidesWhetherAnythingIsFiltered() {
        // The vanilla Nether generator carries no marker, so the scope fallback is the only route by which a
        // historical Nether acquires a rule at all.
        HistoricalGenerationScope.set(BETA_1_7_3);
        String alias = HistoricalFeatureExclusions.activeAlias(null);
        assertNotNull(alias, "a historical server scope must be visible to a markerless generator");
        String profileId = HistoricalSpawnRules.profileIdForAlias(alias);
        assertEquals(BETA_1_7_3, profileId);
        assertFalse(HistoricalSpawnRules.forProfile(profileId).isEmpty(),
            "resolving to a profile with no rule is the shape of the original defect");

        // Cleared: an ordinary world gets no alias, no profile, and therefore no filtering at all.
        HistoricalGenerationScope.clear();
        assertNull(HistoricalFeatureExclusions.activeAlias(null));
        assertNull(HistoricalSpawnRules.profileIdForAlias(null));
        for (String modern : MODERN_NETHER_MOBS) {
            assertTrue(HistoricalSpawnRules.isAllowed(null, modern),
                modern + " must keep spawning in an ordinary world");
        }
        // An unrecognised scope is the same as none, which is what stops a leak between worlds.
        HistoricalGenerationScope.set("moderner_beta:beta");
        assertNull(HistoricalSpawnRules.profileIdForAlias(
            HistoricalFeatureExclusions.activeAlias(null)));
    }
}

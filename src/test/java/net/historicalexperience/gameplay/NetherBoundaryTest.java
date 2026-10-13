package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the Nether boundary, which the owner asked about directly.
 *
 * <p>"I assume you've already blocked nether portals until alpha 1.2" - and it was not blocked. The block veto runs
 * on the generation path, so it stops netherrack, glowstone and portal blocks from being generated, and does nothing
 * about a player placing obsidian and lighting it. In an Alpha 1.0.1 world that produced a working portal into a
 * dimension the version never had.
 *
 * <p>The Nether arrives with the Halloween Update, Alpha 1.2.0, so every declared Alpha and Infdev build must refuse
 * both creating a portal and travelling through one, and every declared Beta or Release build must allow both.
 */
class NetherBoundaryTest {
    private static final int ERA_ALPHA = 400_000;

    @Test
    void everyDeclaredBuildBeforeAlphaOnePointTwoHasNoNether() {
        for (String id : new String[] {
            "historical_experience:infdev_420",
            "historical_experience:infdev_611",
            "historical_experience:alpha_1_0_1",
            "historical_experience:alpha_1_0_4",
            "historical_experience:alpha_1_0_6"}) {
            assertFalse(HistoricalGameplayTweaks.netherAvailable(id),
                id + " predates Alpha 1.2.0, so no portal may be lit and no travel may happen there");
        }
    }

    @Test
    void theDeclaredAlphaProfileAlsoHasNoNether() {
        // Alpha v1.1.2_01 is BELOW the boundary, which is the case a reader is most likely to get wrong: the Alpha
        // profile is the newest declared Alpha build and still predates the Halloween Update by one build.
        assertFalse(HistoricalGameplayTweaks.netherAvailable("historical_experience:alpha"),
            "Alpha v1.1.2_01 predates Alpha 1.2.0 and must have no Nether");
    }

    @Test
    void everyDeclaredBetaAndReleaseBuildHasTheNether() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            if (version.chronology() < 500_000) {
                continue;
            }
            assertTrue(HistoricalGameplayTweaks.netherAvailable(version.id()),
                version.id() + " is after Alpha 1.2.0 and must have the Nether");
        }
    }

    @Test
    void theBoundaryLiteralFollowsTheRegistrarsOwnNumbering() {
        assertEquals(HistoricalVersions.chronology(ERA_ALPHA, 1, 2, 0),
            HistoricalGameplayTweaks.netherBoundaryChronology(),
            "the Nether arrives in Alpha 1.2.0, a target that is not declared yet, so the boundary is a literal tied "
                + "to the registry's scheme");

        assertTrue(HistoricalGameplayTweaks.netherBoundaryChronology()
                > HistoricalVersions.byId("historical_experience:alpha").orElseThrow().chronology(),
            "Alpha v1.1.2_01 must sit below the Nether boundary");

        assertFalse(HistoricalGameplayTweaks.netherAvailable(null),
            "an ordinary world is not a historical version and this gate must not touch it");
    }
}

package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards which mobs may spawn naturally in the earliest declared builds.
 *
 * <p>Written because the owner asked whether the spawn sets had been checked. They had not: the sets existed, one
 * was derived correctly, and a spawn filter was observed removing chickens at runtime - none of which establishes
 * that a set is right. Researching it found cows allowed in builds that predate them.
 *
 * <p>The failures are asymmetric and both matter. A mob allowed too early spawns in a world that never had it, which
 * a player notices. A mob missing that had arrived never spawns at all, which looks like a quiet world and is never
 * reported.
 */
class AlphaMobSetTest {
    private static final String ALPHA_1_0_1 = "historical_experience:alpha_1_0_1";
    private static final String ALPHA_1_0_4 = "historical_experience:alpha_1_0_4";
    private static final String ALPHA_1_0_6 = "historical_experience:alpha_1_0_6";
    private static final String ALPHA_1_1_2 = "historical_experience:alpha";

    private static Set<String> mobsOf(String versionId) {
        // Read from the version's own rules, which is the declaration the mixins consult - not from a helper that
        // might present a different set.
        return net.historicalexperience.version.HistoricalVersions.byId(versionId)
            .orElseThrow(() -> new AssertionError(versionId + " is not declared"))
            .rules()
            .allowedNaturalSpawns();
    }

    @Test
    void cowsAreAbsentFromEveryBuildBeforeAlphaOnePointZeroPointEight() {
        // research/historical-features.json, beta18-cow-drops, confidence high: "Alpha v1.0.8 | Cows added".
        for (String version : new String[] {ALPHA_1_0_1, ALPHA_1_0_4, ALPHA_1_0_6}) {
            assertFalse(mobsOf(version).contains("minecraft:cow"),
                version + " predates Alpha 1.0.8, so cows must not spawn there. They were allowed until the spawn "
                    + "sets were checked against the research rather than assumed");
        }

        assertTrue(mobsOf(ALPHA_1_1_2).contains("minecraft:cow"),
            "Alpha v1.1.2_01 is past 1.0.8 and must have cows, or the removal has gone one build too far");
    }

    @Test
    void slimesAndChickensArriveLaterStill() {
        for (String version : new String[] {ALPHA_1_0_1, ALPHA_1_0_4, ALPHA_1_0_6}) {
            assertFalse(mobsOf(version).contains("minecraft:slime"),
                version + " predates Alpha 1.0.11, where slimes arrive");
            assertFalse(mobsOf(version).contains("minecraft:chicken"),
                version + " predates Alpha 1.0.14, where chickens arrive");
        }

        assertTrue(mobsOf(ALPHA_1_1_2).contains("minecraft:slime"),
            "Alpha v1.1.2_01 is past 1.0.11");
        assertTrue(mobsOf(ALPHA_1_1_2).contains("minecraft:chicken"),
            "Alpha v1.1.2_01 is past 1.0.14");
    }

    @Test
    void theBuildThatIntroducesClayAlsoHasCowsAndSlimesButStillNoChickens() {
        // Alpha 1.0.11 sits between two boundaries, which makes it the most informative declared Alpha build:
        // cows arrived at 1.0.8 (so it HAS them), slimes are inferred to have arrived here, and chickens do not
        // arrive until 1.0.14 (so it still LACKS them).
        Set<String> mobs = mobsOf("historical_experience:alpha_1_0_11");

        assertTrue(mobs.contains("minecraft:cow"),
            "Alpha 1.0.11 is past 1.0.8, where cows arrive");
        assertTrue(mobs.contains("minecraft:slime"),
            "slime natural spawning is described in the 1.0.14 delta as reduced and then disabled, which means "
                + "slimes were spawning before it - inference, recorded as such in ALPHA_1_0_11_MOBS");
        assertFalse(mobs.contains("minecraft:chicken"),
            "chickens arrive in Alpha 1.0.14, the build after this one");

        // And the neighbour that must differ from it in exactly one way.
        assertEquals(mobs.size() + 1, mobsOf(ALPHA_1_1_2).size(),
            "the 1.1.2 set is the 1.0.11 set PLUS the chicken, and nothing else - if that stops holding, one of the "
                + "two builds has a mob it should not");
    }

    @Test
    void thePreAlphaMobsThatPredateEveryDeclaredBuildAreStillPresent() {
        // Creeper, skeleton, spider, zombie, pig and sheep all predate Alpha - creepers from the Survival Test, the
        // rest from Indev or earlier - so removing them would be the opposite mistake, and a silent one.
        for (String mob : new String[] {
            "minecraft:creeper", "minecraft:skeleton", "minecraft:spider", "minecraft:zombie",
            "minecraft:pig", "minecraft:sheep"}) {
            assertTrue(mobsOf(ALPHA_1_0_1).contains(mob),
                mob + " predates every declared build and must be allowed, or an Alpha world spawns almost nothing");
        }
    }
}

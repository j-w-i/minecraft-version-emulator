package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards the registry's chronologies, which are the one thing every gate compares.
 *
 * <p>Found while preparing to declare Alpha 1.0.1. The Alpha profile was the only version whose chronology was
 * hand-written - {@code ERA_ALPHA + 112} - and that value sits BELOW every Alpha 1.x build the registry's own
 * scheme produces (Alpha 1.0.1 is 401001). Declaring Alpha 1.0.1 would therefore have placed it AFTER Alpha
 * v1.1.2_01: eleven builds in the wrong order, silently, with every gate that compares them answering backwards.
 *
 * <p>The scheme is {@code chronology(eraOffset, major, minor, patch) = eraOffset + major*1000 + minor*100 + patch},
 * which handles two-digit patches - Alpha 1.0.11 is 401011, correctly before Alpha 1.1.0 at 401100 - whereas the
 * packed three-digit form the old value used would have put 1.0.11 at 111 against 1.1.0 at 110, inverting those
 * two as well.
 */
class VersionChronologyTest {
    private static final int ERA_INFDEV = 300_000;
    private static final int ERA_ALPHA = 400_000;
    private static final int ERA_BETA = 500_000;
    private static final int ERA_RELEASE = 600_000;

    @Test
    void everyDeclaredVersionSitsInsideItsOwnEra() {
        Map<String, Integer> eraBase = Map.of(
            "infdev", ERA_INFDEV,
            "alpha", ERA_ALPHA,
            "beta", ERA_BETA,
            "release", ERA_RELEASE);

        for (HistoricalVersion version : HistoricalVersions.all()) {
            Integer base = eraBase.get(version.era());

            assertTrue(base != null, version.id() + " declares era '" + version.era()
                + "', which this test does not know the band for. An era with no band cannot be checked, and a "
                + "chronology outside every band compares wrongly against all of them");
            assertTrue(version.chronology() >= base && version.chronology() < base + 100_000,
                version.id() + " has chronology " + version.chronology() + ", which is outside the "
                    + version.era() + " band starting at " + base + ". A version in the wrong band sorts against "
                    + "every other era incorrectly");
        }
    }

    @Test
    void noTwoDeclaredVersionsShareAChronology() {
        Map<Integer, String> seen = new HashMap<>();

        for (HistoricalVersion version : HistoricalVersions.all()) {
            String previous = seen.put(version.chronology(), version.id());

            assertTrue(previous == null,
                version.id() + " and " + previous + " share chronology " + version.chronology()
                    + ", so every comparison between them is decided by declaration order instead of history");
        }
    }

    @Test
    void theAlphaProfileFollowsTheSchemeSoEarlierAlphaBuildsSortBeforeIt() {
        HistoricalVersion alpha = HistoricalVersions.byId("historical_experience:alpha").orElseThrow();

        assertEquals(HistoricalVersions.chronology(ERA_ALPHA, 1, 1, 2), alpha.chronology(),
            "Alpha v1.1.2_01 must be on the registry's own scheme. Its chronology was hand-written as ERA_ALPHA "
                + "+ 112, which is below Alpha 1.0.1's 401001 - so the first Alpha build declared would have "
                + "sorted after it and the Alpha line would read backwards");

        // The builds the Alpha line must sort before it, in the order they happened.
        List<Integer> earlierAlphaBuilds = new ArrayList<>(List.of(
            HistoricalVersions.chronology(ERA_ALPHA, 1, 0, 1),
            HistoricalVersions.chronology(ERA_ALPHA, 1, 0, 11),
            HistoricalVersions.chronology(ERA_ALPHA, 1, 1, 0)));

        for (int chronology : earlierAlphaBuilds) {
            assertTrue(chronology < alpha.chronology(),
                "an Alpha build (" + chronology + ") does not sort before Alpha v1.1.2_01 (" + alpha.chronology()
                    + ")");
        }

        assertTrue(HistoricalVersions.chronology(ERA_ALPHA, 1, 0, 11)
                < HistoricalVersions.chronology(ERA_ALPHA, 1, 1, 0),
            "Alpha 1.0.11 must sort before Alpha 1.1.0. The packed three-digit form would have put them at 111 "
                + "and 110, the wrong way round");
    }
}

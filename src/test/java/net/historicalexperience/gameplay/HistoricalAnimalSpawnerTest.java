package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which spawner shape each version ran, and that the boundary between the two is where the research says.
 *
 * <p>Both shapes were read from hash-verified clients and are recorded in
 * {@code research/pre-beta-1.8-spawning-evidence.json}: the Alpha v1.1.2_01 jar gives the flat-cap, radius-4,
 * two-attempt shape, and the Beta 1.7.3 jar gives the scaled-cap, radius-8, four-attempt one. The wiki's Alpha
 * v1.2.0 history row - the rework that stopped a solid block aborting the whole cycle - is why that build is the
 * cut-over rather than some build of my choosing.
 *
 * <p>Level: unit. {@link HistoricalAnimalSpawner#shapeFor} reads the version registry only, so this runs without
 * a bootstrapped game registry - which is the reason the shape was split out of the tick loop in the first place.
 */
class HistoricalAnimalSpawnerTest {
    private static final String ALPHA_1_0_1 = "historical_experience:alpha_1_0_1";
    private static final String ALPHA_1_1_0 = "historical_experience:alpha_1_1_0";
    private static final String ALPHA_V1_1_2_01 = "historical_experience:alpha";
    private static final String ALPHA_1_2_0 = "historical_experience:alpha_1_2_0";
    private static final String BETA_1_0 = "historical_experience:beta_1_0";
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";
    private static final String BETA_1_8_1 = "historical_experience:beta_1_8_1";
    private static final String RELEASE_1_6_4 = "historical_experience:release_1_6_4";

    @Test
    @DisplayName("the pre-v1.2.0 Alpha builds use the Alpha jar's shape, including the last of them")
    void alphaShapeCoversTheAlphaEraUpToTheRework() {
        for (String id : new String[] {ALPHA_1_0_1, ALPHA_1_1_0, ALPHA_V1_1_2_01}) {
            var shape = HistoricalAnimalSpawner.shapeFor(id);
            assertNotNull(shape, id + " must have a spawner: it predates Beta 1.8 and had natural spawning");
            assertEquals(4, shape.chunkRadius(), id + ": the Alpha jar loops +-4 chunks around a player");
            assertEquals(3, shape.outerIterations(), id);
            assertEquals(2, shape.attemptsPerChunk(), id);
        }
        assertTrue(!HistoricalAnimalSpawner.shapeFor(ALPHA_V1_1_2_01).scaledCap(),
            "the Alpha cap is FLAT: world.b(category) < 15, with no chunk arithmetic");
    }

    @Test
    @DisplayName("Alpha v1.2.0 onward uses the Beta shape, which the Beta 1.7.3 jar shows directly")
    void betaShapeStartsAtTheAlphaRework() {
        for (String id : new String[] {ALPHA_1_2_0, BETA_1_0, BETA_1_7_3}) {
            var shape = HistoricalAnimalSpawner.shapeFor(id);
            assertNotNull(shape, id + " predates Beta 1.8");
            assertEquals(8, shape.chunkRadius(), id + ": the Beta jar loops +-8 chunks, the wiki's 17x17");
            assertEquals(4, shape.attemptsPerChunk(), id);
            assertTrue(shape.scaledCap(), id + ": Beta compares against cap * eligibleChunks / 256");
        }

        // The creature cap itself is 15 in BOTH jars, which is the fact that makes one constant correct here.
        assertEquals(15, HistoricalAnimalSpawner.CREATURE_CAP);
    }

    @Test
    @DisplayName("Beta 1.8 and later have no such spawner: that is the version that replaced it")
    void theBoundaryIsBetaOneEight() {
        assertNull(HistoricalAnimalSpawner.shapeFor(BETA_1_8_1),
            "Beta 1.8 replaced light-level spawning with chunk-property spawning, so the old spawner must stop");
        assertNull(HistoricalAnimalSpawner.shapeFor(RELEASE_1_6_4));
        assertNull(HistoricalAnimalSpawner.shapeFor(null), "no version means no claim");
        assertNull(HistoricalAnimalSpawner.shapeFor("moderner_beta:beta"),
            "an undeclared recipe must not acquire a historical spawner");
    }

    @Test
    @DisplayName("the modern per-chunk animal spawn is suppressed exactly where the cycle replaces it")
    void chunkGenerationSpawningIsSuppressedOnlyWhereACycleExists() {
        // Beta 1.8's own history row is the boundary: spawning was light-level-driven and "was no longer the case
        // in Beta 1.8", replaced by spawning determined by the current chunk properties. So the per-chunk biome
        // pack is the arrangement that arrived WITH the version that removed the cycle.
        for (String id : new String[] {ALPHA_1_0_1, ALPHA_V1_1_2_01, ALPHA_1_2_0, BETA_1_0, BETA_1_7_3}) {
            assertTrue(HistoricalAnimalSpawner.suppressesChunkGenerationSpawning(id),
                id + " has the capped cycle, so the unbounded per-chunk pack must not run beside it - leaving it "
                    + "on is what carpeted a world with animals when the era allowed fifteen");
        }

        for (String id : new String[] {BETA_1_8_1, RELEASE_1_6_4}) {
            assertFalse(HistoricalAnimalSpawner.suppressesChunkGenerationSpawning(id),
                id + " spawns animals into chunks as they generate, which is its own behaviour and must be kept");
        }

        // Infdev keeps the modern path deliberately: its own cycle is not implemented, so suppressing this would
        // leave it with no animals at all rather than with the right ones.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            if ("infdev".equals(version.era())) {
                assertFalse(HistoricalAnimalSpawner.suppressesChunkGenerationSpawning(version.id()),
                    version.id() + " must keep the modern path until its own cycle is read from its jar");
            }
        }
    }

    @Test
    @DisplayName("Infdev is deliberately excluded rather than borrowing Alpha's numbers")
    void infdevIsNotSweptIn() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            if ("infdev".equals(version.era())) {
                assertNull(HistoricalAnimalSpawner.shapeFor(version.id()),
                    version.id() + " had natural spawning, but its own jar was not read for this, so it must not "
                        + "silently run Alpha's numbers");
            }
        }
    }
}

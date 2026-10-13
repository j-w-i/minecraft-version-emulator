package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What actually gates BLOCKS in the Nether for the six Beta 1.9 prereleases - written because the owner asked
 * whether that had been tested, and the honest answer was no.
 *
 * <p>My probes generate Overworld terrain and sample around spawn. They never enter the Nether, so nothing they
 * report says anything about Nether content. Asking the question found a real hole, and this class records both
 * halves of it rather than only the flattering one.
 *
 * <p><b>What does hold.</b> The 1.16 Nether FEATURES are excluded for the prereleases. Their exclusion list is
 * reached through the same chain as everything else, so {@code MODERN_NETHER_CONTENT} - ancient debris, blackstone,
 * nether gold ore, soul fire and magma blocks - is carried by the list each prerelease actually uses.
 *
 * <p><b>What does not.</b> There is no per-BLOCK gating for any version except Beta 1.7.3.
 * {@link HistoricalBlockPalette#paletteFor(String)} returns null for everything else, deliberately and with a
 * comment saying only one palette has been extracted from a hash-verified jar. That comment is now stale - ten
 * palettes exist under {@code research/block-palettes/}, including {@code b1_9_pre3} which five of the six
 * prereleases share - but the code still answers null, so the block veto cannot judge a single Nether block in a
 * prerelease world.
 *
 * <p>The second test below PINS that gap rather than hiding it. When the palettes are wired up it will fail, and
 * that failure is the signal to delete it. This is the same shape as the spawn-restriction test that announced its
 * own obsolescence, which worked.
 */
class PreReleaseNetherGatingTest {
    private static final List<String> PRERELEASES = List.of(
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6");

    @Test
    @DisplayName("every prerelease carries the 1.16 Nether feature exclusions, so none of it is ever placed")
    void prereleaseExclusionsCarryTheModernNetherFeatures() {
        Set<String> expected = HistoricalFeatureExclusions.MODERN_NETHER_CONTENT.stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(expected.contains("minecraft:ore_ancient_debris_large"),
            "sanity: the list this test is about must contain the ancient debris placements");

        for (String id : PRERELEASES) {
            var version = HistoricalVersions.byId(id).orElseThrow(() -> new AssertionError(id + " is not declared"));

            // The exclusions live on the version's revisions, which is what generation actually reads.
            Set<String> carried = version.revisions().stream()
                .flatMap(revision -> revision.excludedPlacedFeatures().stream())
                .map(HistoricalFeatureExclusions.Exclusion::featureId)
                .collect(java.util.stream.Collectors.toSet());

            for (String feature : expected) {
                assertTrue(carried.contains(feature),
                    id + " does not exclude " + feature + ", so a 1.16 Nether feature would be placed in a Beta "
                        + "1.9 world");
            }
        }
    }

    @Test
    @DisplayName("KNOWN GAP: no version except Beta 1.7.3 has a block palette, so prerelease Nether blocks are not gated")
    void blockPaletteGatingIsMissingOutsideBeta173() {
        assertNotNull(HistoricalBlockPalette.paletteFor("historical_experience:beta_1_7_3"),
            "Beta 1.7.3 is the one palette extracted from a hash-verified jar");

        for (String id : PRERELEASES) {
            assertNull(HistoricalBlockPalette.paletteFor(id),
                id + " NOW HAS A PALETTE. That is the fix, not a regression: delete this test, wire the palette into "
                    + "the veto path, and assert instead that the modern Nether blocks are judged ANACHRONISTIC for "
                    + "every prerelease. The palettes already exist under research/block-palettes - b1_9_pre3 is "
                    + "shared by five of these six - so only the wiring is missing.");
        }
    }
}

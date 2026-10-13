package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Emerald ore's boundary is Release 1.3.1, so Release 1.4 is the earliest DECLARED version that had it.
 *
 * <h2>Why this needed doing at all</h2>
 *
 * <p>The availability table pinned emerald to <b>Release 1.6.4</b> — not because that build introduced it, but
 * because 1.6.4 is the earliest <em>extracted palette</em> containing it. That is a proxy, and it was wrong in the
 * expensive direction: Release 1.4 and 1.5 really had emerald ore, and the table would have refused it there.
 *
 * <p>It surfaced only after the thirteen ore-less profiles were fixed. While those profiles generated no ores at
 * all, a missing emerald was invisible among them; once the vanilla features ran again, emerald had to be let
 * through for the versions that had it — and the table boundary had to move in the same change, or the sweep would
 * have reported a legitimate emerald as an anachronism. The two moving together is the point of asserting both
 * here.
 *
 * <p>Hidden trap this test also guards, because it caught it during the change itself: the first attempt retargeted
 * the list by a blanket text replacement, which also moved <b>Release 1.2.5 and Release 1.3</b> onto the
 * emerald-keeping list. Both predate 1.3.1 and must have none. The mapping assertion below is what makes that fail
 * rather than ship.
 */
class EmeraldIntroductionBoundaryTest {
    private static final String EMERALD_FEATURE = "moderner_beta:ore_emerald_y95";

    private static boolean excludesEmerald(String alias) {
        return HistoricalFeatureExclusions.runtimeExclusionsForAlias(alias).stream()
            .anyMatch(exclusion -> exclusion.featureId().equals(EMERALD_FEATURE));
    }

    private static String latestAlias(String versionId) {
        HistoricalVersion version = HistoricalVersions.byId(versionId)
            .orElseThrow(() -> new AssertionError(versionId + " must be declared"));
        return version.revisions().get(version.revisions().size() - 1).alias();
    }

    @Test
    @DisplayName("emerald is refused through Release 1.2.5 and allowed from Release 1.3")
    void theBoundaryIsReleaseOneThreeOne() {
        // Release 1.3.1 is the introduction, and this project's Release 1.3 profile IS that build - the research
        // labels it "Release 1.3 (stable baseline 1.3.1)". So Release 1.3 has emerald and 1.2.5 does not. The
        // first version of this test asserted the boundary was 1.4, which was wrong for exactly that reason, and
        // the owner corrected it.
        assertTrue(excludesEmerald(latestAlias("historical_experience:release_1_2_5")),
            "Release 1.2.5 predates emerald ore (Release 1.3.1) and must refuse it");
        for (String id : new String[] {"historical_experience:release_1_0_0", "historical_experience:release_1_1"}) {
            assertTrue(excludesEmerald(latestAlias(id)), id + " predates emerald ore and must refuse it");
        }

        for (String id : new String[] {"historical_experience:release_1_3", "historical_experience:release_1_4",
                "historical_experience:release_1_5", "historical_experience:release_1_6_4"}) {
            assertFalse(excludesEmerald(latestAlias(id)),
                id + " is the 1.3.1 stable baseline or later and really had emerald, so " + latestAlias(id)
                    + " must not refuse it");
        }

        // Beta 1.7.3 and Beta 1.8.1 predate emerald by years, and their own-ore revisions must keep the ban.
        for (String alias : new String[] {"historical_experience:beta_1_7_3_content_preview_10",
                "historical_experience:beta_1_8_1"}) {
            assertTrue(excludesEmerald(alias), alias + " predates emerald ore and must still refuse it");
        }
    }

    @Test
    @DisplayName("the availability table agrees with the exclusion lists, so a legitimate emerald is not called anachronistic")
    void theTableBoundaryMatchesTheExclusionBoundary() {
        // The two are separate mechanisms that have to agree: the exclusion list stops the feature running, and
        // the table decides whether a block that DID appear is an anachronism. Moving one without the other gives
        // either emerald silently missing or emerald generating and then being reported as a defect.
        var boundary = HistoricalBlockAvailability.boundaryFor("minecraft:emerald_ore");
        assertEquals("historical_experience:release_1_3", boundary.versionId(),
            "emerald ore is Release 1.3.1 content and the Release 1.3 profile is the 1.3.1 stable baseline, so it "
                + "is the earliest declared version that had it - not Release 1.4, and not Release 1.6.4, which was "
                + "only the earliest extracted palette");

        assertTrue(HistoricalBlockContent.isAnachronistic("historical_experience:release_1_2_5", "minecraft:emerald_ore"),
            "Release 1.2.5 predates 1.3.1 and must judge emerald ore an anachronism");
        assertFalse(HistoricalBlockContent.isAnachronistic("historical_experience:release_1_3", "minecraft:emerald_ore"),
            "Release 1.3 IS the 1.3.1 baseline and must allow emerald, or the sweep would flag a real ore as a defect");
        assertFalse(HistoricalBlockContent.isAnachronistic("historical_experience:release_1_6_4", "minecraft:emerald_ore"));
    }
}

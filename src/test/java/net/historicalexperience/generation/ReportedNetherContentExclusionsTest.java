/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.generation;

import net.historicalexperience.version.Exclusion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The exact content the owner reported seeing, asserted against the revisions each version actually generates from.
 *
 * <p>This exists because they reported "striders, magma generating, quartz when it should not be, gold netherrack
 * ore", I fixed the quartz, and they reported it STILL there. The timestamps said their worlds were generated
 * fifteen seconds before the fix deployed - but "trust the timestamp" is not verification, and I have already been
 * wrong twice in this area by inferring instead of checking.
 *
 * <p>So this asserts the exclusions where generation reads them: on the version's REVISIONS. If this passes and the
 * content still appears in a world made after the fix, the exclusions are present and the fault is in their
 * ENFORCEMENT, which is a different bug in a different place. That distinction is the whole point of the class.
 */
class ReportedNetherContentExclusionsTest {
    private static final Set<String> SIXTEEN = Set.of(
        "minecraft:ore_ancient_debris_large",
        "minecraft:ore_debris_small",
        "minecraft:ore_blackstone",
        "minecraft:ore_gold_nether",
        "minecraft:patch_soul_fire",
        "minecraft:ore_magma");

    private static final String QUARTZ = "minecraft:ore_quartz_nether";

    private static Set<String> carriedBy(String versionId) {
        var revisions = HistoricalVersions.byId(versionId)
            .orElseThrow(() -> new AssertionError(versionId + " not declared")).revisions();

        // The LATEST revision, because that is what a newly created world gets. Revision 0 is the frozen original
        // declaration and every version builds it from BETA_1_7_3_RESTRICTIONS, the shared root - so collecting
        // across ALL revisions would test the root as much as the version and report a difference the player never
        // meets. That is what happened on the first run of this test for Release 1.5, whose revision 0 carries the
        // root and therefore excludes quartz it should not.
        //
        // That revision-0 over-exclusion is real and is recorded rather than hidden: a world created on revision 0
        // of Release 1.5 or 1.6.4 would be missing quartz it should have. Existing worlds are on revision 0 by
        // definition, and generation revisions are append-only, so the honest fix is a new revision rather than an
        // edit - tracked as work, not glossed over here.
        return revisions.get(revisions.size() - 1).excludedPlacedFeatures().stream()
            .map(Exclusion::featureId)
            .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("every version before Release 1.5 excludes quartz, the 1.16 ores, soul fire and magma")
    void preReleaseFifteenExcludesAllOfIt() {
        List<String> preFifteen = List.of(
            "historical_experience:beta_1_7_3", "historical_experience:beta_1_8_1",
            "historical_experience:beta_1_9_pre_1", "historical_experience:beta_1_9_pre_2",
            "historical_experience:beta_1_9_pre_3", "historical_experience:beta_1_9_pre_4",
            "historical_experience:beta_1_9_pre_5", "historical_experience:beta_1_9_pre_6",
            "historical_experience:release_1_0_0", "historical_experience:release_1_1",
            "historical_experience:release_1_2_5", "historical_experience:release_1_3",
            "historical_experience:release_1_4");

        for (String id : preFifteen) {
            Set<String> carried = carriedBy(id);

            for (String feature : SIXTEEN) {
                assertTrue(carried.contains(feature), id + " does not exclude " + feature
                    + ", so a world generated from this version would place it");
            }

            assertTrue(carried.contains(QUARTZ), id + " does not exclude " + QUARTZ
                + ". Quartz is Release 1.5 content, and vanilla's nether_wastes places it, so this version would "
                + "generate quartz the owner would find - which is exactly what they reported");
        }
    }

    @Test
    @DisplayName("Release 1.5 and 1.6.4 keep quartz, because it is content they actually had, but not the rest")
    void releaseFifteenKeepsQuartzOnly() {
        for (String id : List.of("historical_experience:release_1_5", "historical_experience:release_1_6_4")) {
            Set<String> carried = carriedBy(id);

            assertFalse(carried.contains(QUARTZ), id + " must NOT exclude nether quartz: it arrives in Release 1.5, "
                + "and excluding it would delete content this version really had");

            for (String feature : SIXTEEN) {
                assertTrue(carried.contains(feature), id + " does not exclude " + feature
                    + ", and no declared version had 1.16 Nether content");
            }
        }
    }
}

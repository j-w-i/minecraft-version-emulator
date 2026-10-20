/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.client.version;

import net.historicalexperience.version.*;
import net.historicalexperience.client.version.CreateWorldHistoricalVersion.Selection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotSelectorTest {
    @Test void snapshotTabHasExactlyTheExplicitPatchesAndNoFakeTerrainProfiles() {
        var entries = HistoricalVersionCatalogue.entriesIn(HistoricalVersionCatalogue.Group.SNAPSHOTS);
        assertEquals(HistoricalSnapshotPatches.all().stream().map(HistoricalSnapshotPatches.Patch::id).collect(java.util.stream.Collectors.toSet()),
            entries.stream().map(HistoricalVersionCatalogue.Entry::key).collect(java.util.stream.Collectors.toSet()));
        for (var entry : entries) {
            assertTrue(entry.selectable()); assertFalse(entry.target()); assertTrue(entry.displayName().contains("patch"));
            assertTrue(HistoricalVersions.byId(entry.key()).isEmpty());
            assertEquals(entry.snapshotPatch().orElseThrow().baseProfileId(), entry.version().id());
            assertEquals(entry.version().chronology(), entry.order());
        }
    }
    @Test void switchingTheHistoricalVersionOffKeepsTheChoiceForSwitchingItBackOn() {
        try {
            Selection.clear();
            assertTrue(Selection.enabled(), "a fresh world-creation screen starts switchable");
            assertEquals("", Selection.key(), "and with no version chosen");

            Selection.set("historical_experience:release_1_1");
            assertTrue(Selection.enabled());
            Selection.setEnabled(false);
            assertEquals("historical_experience:release_1_1", Selection.key(),
                "turning the version off must not forget it: the toggle is what turns it back on");
            assertFalse(Selection.enabled(), "off means an ordinary world");

            Selection.setEnabled(true);
            assertEquals("historical_experience:release_1_1", Selection.key(), "and on again restores the same version");

            Selection.clear();
            assertEquals("", Selection.key());
            assertTrue(Selection.enabled(), "a new world's screen does not inherit the last switch position");
        } finally {
            Selection.clear();
        }
    }
    @Test void defaultPlayerTextDisclosesPatchBaselineLimitsAndActualFeature() {
        var a = VersionText.details(HistoricalVersionCatalogue.byKey("historical_experience:snapshot_11w49a").orElseThrow(), "", false);
        assertTrue(a.contains("B toggles Survival/Creative")); assertTrue(a.contains("private single-player"));
        assertTrue(a.contains("Baseline: Release 1.1")); assertTrue(a.contains("not full snapshot emulation"));
        var b = VersionText.details(HistoricalVersionCatalogue.byKey("historical_experience:snapshot_12w49a").orElseThrow(), "", false);
        assertTrue(b.contains("1.4.6 development")); assertTrue(b.contains("any single item/block"));
        assertTrue(b.contains("Baseline: Release 1.4")); assertFalse(b.contains("1.4.2"));
    }
    @Test void plainReleasesDoNotAdvertiseSnapshotBehavior() {
        var a = VersionText.details(HistoricalVersionCatalogue.byKey("historical_experience:release_1_1").orElseThrow(), "", false);
        var b = VersionText.details(HistoricalVersionCatalogue.byKey("historical_experience:release_1_4").orElseThrow(), "", false);
        assertFalse(a.contains("B toggles")); assertFalse(a.contains("Snapshot patch"));
        assertFalse(b.contains("any single")); assertFalse(b.contains("Snapshot patch"));
    }
}

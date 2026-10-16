/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.metadata;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotMetadataTest {
    private static final HistoricalIdentifier BASE = HistoricalIdentifier.parse("historical_experience:release_1_1");
    private static final HistoricalIdentifier PRESET = HistoricalIdentifier.parse("historical_experience:release_1_1");
    private static final String PATCH = "historical_experience:snapshot_11w49a";
    private static HistoricalMetadata ordinary() { return HistoricalMetadata.initial(BASE, PRESET, "test", 42); }

    @Test void absentPatchPreservesOriginalCanonicalJsonBytes() throws Exception {
        String old = "{\"schema\":" + MetadataSchema.CURRENT + ",\"profileId\":\"historical_experience:release_1_1\",\"generationPreset\":\"historical_experience:release_1_1\",\"revision\":1,\"history\":[{\"revision\":1,\"timestamp\":42,\"actor\":\"test\",\"note\":\"initial historical world establishment\"}]}";
        assertEquals(old, MetadataJson.encode(ordinary()));
        assertEquals(old, MetadataJson.encode(MetadataJson.decode(old)));
        assertEquals("", MetadataJson.decode(old).snapshotPatchId());
        assertEquals(BASE.value(), ordinary().selectionId());
    }
    @Test void explicitPatchRoundTripsWithoutChangingItsBaseTerrainOrProfile() throws Exception {
        var snapshot = HistoricalMetadata.initialWithSnapshot(BASE, PRESET, PATCH, "test", 42);
        assertEquals(snapshot, MetadataJson.decode(MetadataJson.encode(snapshot)));
        assertEquals(BASE, snapshot.profileId()); assertEquals(PRESET, snapshot.generationPreset());
        assertEquals(PATCH, snapshot.selectionId()); assertEquals(1, snapshot.revision());
        assertTrue(MetadataJson.encode(snapshot).contains("\"snapshotPatchId\""));
    }
    @Test void withinVersionRecipeAdvancesPreserveThePatch() {
        var snapshot = ordinary().advanceSnapshotPatch(PATCH, "test", "select snapshot", 43);
        var advanced = snapshot.advance(PRESET, "test", "advance recipe", 44);
        assertEquals(PATCH, advanced.snapshotPatchId()); assertEquals(3, advanced.revision());
        assertEquals(1, ordinary().revision());
    }
    @Test void selectingAnotherReleaseClearsRatherThanLeaksSnapshotBehavior() {
        var snapshot = ordinary().advanceSnapshotPatch(PATCH, "test", "select snapshot", 43);
        var next = snapshot.advanceVersion(HistoricalIdentifier.parse("historical_experience:release_1_4"),
            HistoricalIdentifier.parse("historical_experience:release_1_4"), "test", "next release", 44);
        assertEquals("", next.snapshotPatchId());
        var released = snapshot.advanceSnapshotPatch("", "test", "return to base release", 44);
        assertEquals("", released.snapshotPatchId()); assertEquals(BASE.value(), released.selectionId());
        assertEquals(snapshot.generationPreset(), released.generationPreset());
        assertThrows(IllegalArgumentException.class, () -> snapshot.advanceSnapshotPatch(PATCH, "test", "no change", 44));
    }
    @Test void unknownMismatchedNullAndWhitespacePatchesFailClosed() {
        for (String patch : new String[]{null, " ", "unknown", "historical_experience:snapshot_12w49a"}) {
            assertThrows(IllegalArgumentException.class, () -> new HistoricalMetadata(MetadataSchema.CURRENT, BASE, PRESET,
                ordinary().revision(), ordinary().history(), patch));
        }
    }
    @Test void patchJsonRetainsStrictTypeIdentityAndDuplicateFieldChecks() {
        String old = MetadataJson.encode(ordinary()); String prefix = old.substring(0, old.length() - 1);
        for (String value : new String[]{"null", "4", "true", "[]", "{}", "\"unknown\"", "\"historical_experience:snapshot_12w49a\""}) {
            assertThrows(MetadataException.class, () -> MetadataJson.decode(prefix + ",\"snapshotPatchId\":" + value + "}"));
        }
        assertThrows(MetadataException.class, () -> MetadataJson.decode(prefix + ",\"snapshotPatchId\":\"\",\"snapshotPatchId\":\"\"}"));
    }
}

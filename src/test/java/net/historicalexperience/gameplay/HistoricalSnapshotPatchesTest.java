/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.gameplay;

import java.time.LocalDate;
import java.util.Set;
import net.historicalexperience.version.HistoricalSnapshotPatches;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistoricalSnapshotPatchesTest {
    private static final String A = "historical_experience:snapshot_11w49a";
    private static final String B = "historical_experience:snapshot_12w49a";

    @Test void snapshotParentsAndDatesAreExplicit() {
        var a = HistoricalSnapshotPatches.byId(A).orElseThrow();
        var b = HistoricalSnapshotPatches.byId(B).orElseThrow();
        assertEquals("1.1", a.parentRelease()); assertEquals(LocalDate.of(2011, 12, 8), a.releaseDate());
        assertEquals("1.4.6", b.parentRelease()); assertEquals(LocalDate.of(2012, 12, 7), b.releaseDate());
        assertNotEquals("1.4.2", b.parentRelease());
        for (var patch : HistoricalSnapshotPatches.all()) assertTrue(HistoricalVersions.byId(patch.baseProfileId()).isPresent());
    }
    @Test void snapshotDefinitionsAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> HistoricalSnapshotPatches.all().clear());
        assertThrows(UnsupportedOperationException.class, () -> HistoricalSnapshotPatches.byId(A).orElseThrow().features().clear());
    }
    @Test void attachmentCannotLeakBetweenReleaseFamilies() {
        assertDoesNotThrow(() -> HistoricalSnapshotPatches.validateAttachment("historical_experience:release_1_1", A));
        assertDoesNotThrow(() -> HistoricalSnapshotPatches.validateAttachment("historical_experience:release_1_4", B));
        assertThrows(IllegalArgumentException.class, () -> HistoricalSnapshotPatches.validateAttachment("historical_experience:release_1_1", B));
        assertThrows(IllegalArgumentException.class, () -> HistoricalSnapshotPatches.validateAttachment("historical_experience:release_1_4", A));
        assertThrows(IllegalArgumentException.class, () -> HistoricalSnapshotPatches.validateAttachment("historical_experience:release_1_1", "unknown"));
        assertDoesNotThrow(() -> HistoricalSnapshotPatches.validateAttachment("minecraft:normal", ""));
    }
    @Test void oldMenuHasBothReleaseBoundaries() {
        var expected = Set.of("beta_1_8_1", "beta_1_9_pre_1", "beta_1_9_pre_2", "beta_1_9_pre_3", "beta_1_9_pre_4",
            "beta_1_9_pre_5", "beta_1_9_pre_6", "release_1_0_0", "release_1_1", "release_1_2_5");
        for (var version : HistoricalVersions.all()) assertEquals(expected.contains(version.id().substring(version.id().indexOf(':') + 1)),
            HistoricalCreativeRules.usesOldMenu(version.id()), version.id());
        assertTrue(HistoricalCreativeRules.usesOldMenu(A)); assertFalse(HistoricalCreativeRules.usesOldMenu(B));
    }
    @Test void ordinaryUnknownAndPresetAliasesAreNotHistoricalSelections() {
        for (String id : new String[]{null, "", "minecraft:normal", "unknown", "historical_experience:indev_latest_with_ores"}) {
            assertFalse(HistoricalCreativeRules.gatesCatalog(id)); assertFalse(HistoricalCreativeRules.usesOldMenu(id));
            assertFalse(HistoricalCreativeRules.hasCreativeShortcut(id));
            assertFalse(HistoricalCreativeRules.allowsBookOnAnyItem(id, 1, true));
        }
    }
    @Test void bShortcutBelongsOnlyToItsSnapshotPatch() {
        assertTrue(HistoricalCreativeRules.hasCreativeShortcut(A)); assertFalse(HistoricalCreativeRules.hasCreativeShortcut(B));
        assertFalse(HistoricalCreativeRules.hasCreativeShortcut("historical_experience:release_1_1"));
        assertTrue(HistoricalCreativeRules.allowsCreativeShortcut(A, true));
        assertFalse(HistoricalCreativeRules.allowsCreativeShortcut(A, false));
        assertFalse(HistoricalCreativeRules.allowsCreativeShortcut(B, true));
        assertFalse(HistoricalCreativeRules.allowsCreativeShortcut("historical_experience:release_1_1", true));
    }
    @Test void bookPatchRequiresExactlyOneTargetAndAnEnchantedBook() {
        assertTrue(HistoricalCreativeRules.allowsBookOnAnyItem(B, 1, true));
        for (int count : new int[]{-1, 0, 2, 64}) assertFalse(HistoricalCreativeRules.allowsBookOnAnyItem(B, count, true));
        assertTrue(HistoricalCreativeRules.rejectsStackedBookTarget(B, 2, true));
        assertTrue(HistoricalCreativeRules.rejectsStackedBookTarget(B, 64, true));
        assertFalse(HistoricalCreativeRules.rejectsStackedBookTarget(B, 1, true));
        assertFalse(HistoricalCreativeRules.rejectsStackedBookTarget(B, 64, false));
        assertFalse(HistoricalCreativeRules.rejectsStackedBookTarget(A, 64, true));
        assertFalse(HistoricalCreativeRules.rejectsStackedBookTarget("historical_experience:release_1_4", 64, true));
        assertFalse(HistoricalCreativeRules.allowsBookOnAnyItem(B, 1, false));
        assertFalse(HistoricalCreativeRules.allowsBookOnAnyItem(A, 1, true));
        assertFalse(HistoricalCreativeRules.allowsBookOnAnyItem("historical_experience:release_1_4", 1, true));
    }
    @Test void snapshotsResolveToBaseRulesWithoutChangingTheVersionRegistry() {
        assertEquals("historical_experience:release_1_1", HistoricalSnapshotPatches.baseProfileForSelection(A).orElseThrow());
        assertEquals("historical_experience:release_1_4", HistoricalSnapshotPatches.baseProfileForSelection(B).orElseThrow());
        assertTrue(HistoricalVersions.byId(A).isEmpty()); assertTrue(HistoricalVersions.byId(B).isEmpty());
        assertTrue(HistoricalCreativeRules.gatesCatalog(A)); assertTrue(HistoricalCreativeRules.gatesCatalog(B));
    }
}

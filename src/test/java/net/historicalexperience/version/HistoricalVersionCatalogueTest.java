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
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.historicalexperience.version.HistoricalVersionCatalogue.Group;
import net.historicalexperience.version.HistoricalVersionCatalogue.Status;

/**
 * Guards the version hierarchy the selector is built on.
 *
 * <p>Two mistakes this file exists to make impossible: showing a version that cannot actually be played, and
 * quietly pointing an unsupported choice at a supported one. Both would look fine on screen, which is why neither
 * can be left to inspection.
 */
class HistoricalVersionCatalogueTest {
    @Test
    void coversTheWholePlannedMatrix() {
        List<Entry> planned = HistoricalVersionCatalogue.plannedTargets();

        assertEquals(29, planned.size(), "the expansion target is twenty-nine versions and the catalogue covers "
            + "every one, so the selector never grows with the plan");
        assertEquals(8, planned.stream().filter(entry -> entry.era() == Group.ALPHA).count(),
            "the plan names eight Alpha versions");
        assertEquals(14, planned.stream().filter(entry -> entry.era() == Group.BETA).count(),
            "the plan names fourteen Beta versions, prereleases included");
        assertEquals(7, planned.stream().filter(entry -> entry.era() == Group.RELEASE).count(),
            "the plan names seven Release versions");

        List<String> prereleases = HistoricalVersionCatalogue.entriesIn(Group.BETA).stream()
            .map(Entry::targetName)
            .filter(name -> name.contains("Prerelease"))
            .toList();
        assertEquals(6, prereleases.size(), "Beta 1.9 has six prereleases: " + prereleases);
    }

    /**
     * The matrix is COMPLETE, which is the acceptance condition for the expansion objective and cannot be
     * inferred from the counts above.
     *
     * <p>{@link #coversTheWholePlannedMatrix} asserts that twenty-nine targets are DESCRIBED. It said nothing
     * about whether any of them could actually be played, and for a long time three could not: Beta 1.4, 1.5 and
     * 1.6 were listed with a {@code planned_} key and the note "no gameplay profile or generation recipe yet".
     * A matrix that names a version and cannot create a world of it is a plan, not an implementation, and the two
     * look identical from a count.
     *
     * <p>So this walks the plan and requires every target to resolve to a declared profile, to have at least one
     * generation revision, and to be reachable through a world preset - which is what "the GUI can create it"
     * means in this project. If a future target is added it fails here until it is implemented, which is the
     * intended direction of failure.
     */
    @Test
    void everyPlannedTargetIsImplementedRatherThanMerelyNamed() {
        List<String> unimplemented = HistoricalVersionCatalogue.plannedTargets().stream()
            .filter(entry -> entry.status() != Status.IMPLEMENTED)
            .map(Entry::targetName)
            .sorted()
            .toList();
        assertTrue(unimplemented.isEmpty(),
            "these planned versions are still only NAMED, so the matrix is not implemented: " + unimplemented);

        for (Entry entry : HistoricalVersionCatalogue.plannedTargets()) {
            HistoricalVersion version = HistoricalVersions.byId(entry.key())
                .orElseThrow(() -> new AssertionError(entry.targetName() + " resolves to profile "
                    + entry.key() + ", which the registry does not declare"));
            assertFalse(version.revisions().isEmpty(),
                entry.targetName() + " declares no generation revision, so no world could be created from it");
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                assertTrue(java.nio.file.Files.exists(java.nio.file.Path.of(
                        "src/main/resources/data/historical_experience/worldgen/world_preset",
                        revision.alias().substring(revision.alias().indexOf(':') + 1) + ".json")),
                    entry.targetName() + " has no world preset for revision " + revision.alias()
                        + ", so it cannot be created through the GUI at all");
            }
        }

        // And the three that were last, asserted by name rather than only through the loop above, because these
        // are the specific versions the objective was about.
        for (String id : List.of("historical_experience:beta_1_4", "historical_experience:beta_1_5",
                "historical_experience:beta_1_6")) {
            assertTrue(HistoricalVersions.byId(id).isPresent(),
                id + " is a planned target of the matrix and must be a declared profile");
        }
    }

    @Test
    void profilesThePlanDoesNotNameStayReachableWithoutCounting() {
        List<Entry> extras = HistoricalVersionCatalogue.implementedExtras();

        assertEquals(7, extras.size(),
            "Two explicit snapshot patches, Indev 20100223, both Infdev builds, Alpha v1.1.2_01 and Alpha v1.2.2a are outside the planned "
                + "matrix, and each must still be reachable: " + extras.stream().map(Entry::key).toList());
        assertTrue(extras.stream().allMatch(Entry::selectable),
            "an extra is implemented, so it must be selectable even though the plan does not name it");
        assertTrue(extras.stream().noneMatch(Entry::target),
            "and it must not be counted as one of the twenty-nine");
    }

    @Test
    void everyImplementedEntryResolvesToARegistryVersion() {
        for (Entry entry : HistoricalVersionCatalogue.implemented()) {
            assertNotNull(entry.version(), entry.key() + " is marked implemented but the registry has no such "
                + "version, which would make a menu row that cannot be played");
            if (entry.snapshotPatch().isPresent()) {
                assertEquals(entry.snapshotPatch().get().baseProfileId(), entry.version().id());
                assertFalse(HistoricalVersions.byId(entry.key()).isPresent(), "patches must not manufacture terrain profiles");
                assertEquals(Group.SNAPSHOTS, entry.era());
                assertTrue(entry.displayName().contains("patch"));
            } else assertTrue(HistoricalVersions.byId(entry.key()).isPresent(),
                entry.key() + " must be a registry id unless it explicitly describes a snapshot overlay");
        }
    }

    @Test
    void noFutureEntryCanBePlayedOrMappedOntoSomethingElse() {
        for (Entry entry : HistoricalVersionCatalogue.future()) {
            assertFalse(entry.selectable(), entry.key() + " is still to come and must not be selectable");
            assertNull(entry.version(),
                entry.key() + " resolves to a real version, which would be a silent substitution");
            assertTrue(entry.key().contains(":planned_"),
                entry.key() + " must be namespaced as planned so it can never be mistaken for a profile id");
            assertFalse(entry.summary().isBlank(),
                entry.key() + " must say why it cannot be chosen yet");
        }
    }

    @Test
    void noImplementedVersionIsHiddenFromTheMenu() {
        Set<String> catalogued = HistoricalVersionCatalogue.implemented().stream()
            .map(entry -> entry.version().id())
            .collect(Collectors.toSet());
        Set<String> declared = HistoricalVersions.all().stream()
            .map(HistoricalVersion::id)
            .collect(Collectors.toSet());

        assertEquals(declared, catalogued,
            "every declared version must be reachable from the selector; a version that exists but cannot be "
                + "chosen is worse than one that does not exist");
    }

    @Test
    void implementedTargetsStateWhichBuildTheyEmulate() {
        Entry beta17 = HistoricalVersionCatalogue.byKey("historical_experience:beta_1_7_3").orElseThrow();

        assertEquals("Beta 1.7", beta17.targetName(), "the plan calls this target Beta 1.7");
        assertEquals("Beta 1.7.3", beta17.displayName(), "the name shown is the build actually emulated");
        assertEquals("Beta 1.7.3", beta17.emulated(),
            "and the emulated build is stated explicitly, so the screen can say Beta 1.7 is emulated as 1.7.3 "
                + "rather than implying 1.7 exactly");

        Entry release10 = HistoricalVersionCatalogue.byKey("historical_experience:release_1_0_0").orElseThrow();
        assertEquals("Release 1.0", release10.targetName());
        assertTrue(release10.emulated().isEmpty(),
            "Release 1.0.0 IS the target, so there is no build to distinguish");
    }

    @Test
    void implementedEntriesDescribeTheirGenerationRecipe() {
        Entry entry = HistoricalVersionCatalogue.byKey("historical_experience:release_1_6_4").orElseThrow();
        HistoricalVersion version = entry.version();

        assertNotNull(version);
        assertTrue(entry.summary().contains(version.latestRevision().alias()),
            "the summary must name the recipe the version actually generates with, read from the registry "
                + "rather than stored a second time in the menu: " + entry.summary());
        assertTrue(entry.summary().contains("revision " + version.latestRevision().revision()),
            "and say which revision, because revisions are append-only identifiers");
    }

    @Test
    void orderIsChronologicalAcrossAndWithinEras() {
        List<Group> expected = List.of(Group.INDEV, Group.INFDEV, Group.ALPHA, Group.BETA, Group.RELEASE, Group.SNAPSHOTS);

        assertEquals(expected, HistoricalVersionCatalogue.eras(), "eras are ordered oldest first");
        assertSame(Group.INDEV, HistoricalVersionCatalogue.entries().get(0).era(),
            "the oldest implemented version is Indev 20100223, the project's only finite profile");

        for (Group era : expected) {
            List<Entry> inEra = HistoricalVersionCatalogue.entriesIn(era);
            for (int index = 1; index < inEra.size(); index++) {
                assertTrue(inEra.get(index - 1).order() < inEra.get(index).order(),
                    inEra.get(index - 1).targetName() + " must sort before " + inEra.get(index).targetName());
            }
        }
    }

    @Test
    void keysAreUniqueAndLookupIsStable() {
        Set<String> keys = HistoricalVersionCatalogue.entries().stream()
            .map(Entry::key)
            .collect(Collectors.toSet());

        assertEquals(HistoricalVersionCatalogue.entries().size(), keys.size(),
            "a key identifies a version, and a world or menu selection may hold one");

        for (Entry entry : HistoricalVersionCatalogue.entries()) {
            assertSame(entry, HistoricalVersionCatalogue.byKey(entry.key()).orElseThrow(),
                entry.key() + " must be findable by its own key");
        }
    }

    private static long countIn(Group era) {
        return HistoricalVersionCatalogue.entriesIn(era).stream()
            .filter(entry -> entry.status() == Status.IMPLEMENTED || !entry.targetName().contains("Infdev"))
            .count();
    }
}

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
package net.historicalexperience.client.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersionCatalogue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a player sees in the version selector, and what only developer mode adds.
 *
 * <p>The owner's report was that the selector showed them this project's internals - a generation recipe alias with a
 * revision number, and the build each profile was measured against - on screens a player uses. The failure mode of a
 * fix like that is silent in both directions: too little and the selector stops answering "why can I not pick this?",
 * too much and the identifiers are back in every screenshot. So both halves are asserted here, and the negative
 * assertions name the exact strings that must not appear.
 */
class VersionTextTest {
    /** A version this build implements, with a measured build to name: chosen from the catalogue, not hard-coded. */
    private static HistoricalVersionCatalogue.Entry implemented() {
        return HistoricalVersionCatalogue.implemented().stream()
            .filter(entry -> !entry.emulated().isEmpty())
            .findFirst()
            .orElseThrow(() -> new AssertionError("the catalogue declares no implemented version with a measured build"));
    }

    /**
     * A planned version, built here rather than looked up.
     *
     * <p>The catalogue currently declares no unimplemented version - every entry is playable - so {@code future()}
     * is empty and the branch this covers would otherwise go untested until the next version is declared. The record
     * is public and its own accessors already define the future case (version() is null, displayName() is the target
     * name, summary() is the future note), so a synthetic entry exercises exactly the real path.
     */
    private static HistoricalVersionCatalogue.Entry planned() {
        return new HistoricalVersionCatalogue.Entry("test:planned", "Beta 1.3", HistoricalVersionCatalogue.Group.BETA, 0,
            HistoricalVersionCatalogue.Status.FUTURE, "", "the terrain for this build is not implemented yet", true);
    }

    @Test
    @DisplayName("a player is told the version and era, and nothing about recipes or measured builds")
    void playerSeesNameAndEraOnly() {
        HistoricalVersionCatalogue.Entry entry = implemented();
        String text = VersionText.details(entry, null, false);

        assertTrue(text.contains(entry.displayName()), text);
        assertTrue(text.contains(entry.era().displayName()), text);
        assertTrue(text.contains("Implemented."), text);

        // The three things that must not reach a player: the measured build, and the generation recipe with its
        // revision - which is where the raw alias like historical_experience:release_1_6_4_no_modern_content appeared.
        assertFalse(text.contains("Emulates"), text);
        assertFalse(text.contains("Generates with"), text);
        assertFalse(text.contains(entry.emulated()), text);
        assertFalse(text.contains(entry.version().latestRevision().alias()), text);
        assertFalse(text.contains("revision"), text);
        assertFalse(text.contains("historical_experience:"), text);
    }

    @Test
    @DisplayName("developer mode restores the measured build and the generation recipe, word for word")
    void developerSeesTheRecipe() {
        HistoricalVersionCatalogue.Entry entry = implemented();
        String text = VersionText.details(entry, null, true);

        assertTrue(text.contains("Emulates " + entry.emulated() + "."), text);
        assertTrue(text.contains("Generates with " + entry.version().latestRevision().alias()
            + " (revision " + entry.version().latestRevision().revision() + ")"), text);
        assertEquals(VersionText.details(entry, null, false).lines().count() + 1, text.lines().count(),
            "developer mode adds exactly one line - the recipe - and extends the measured build onto the status line");
    }

    @Test
    @DisplayName("a planned version still says why it cannot be chosen, in both modes")
    void plannedVersionExplainsItself() {
        HistoricalVersionCatalogue.Entry entry = planned();
        String forPlayer = VersionText.details(entry, null, false);
        String forDeveloper = VersionText.details(entry, null, true);

        assertTrue(forPlayer.startsWith(entry.displayName() + " - " + entry.era().displayName()), forPlayer);
        assertTrue(forPlayer.contains("Planned. " + entry.summary()), forPlayer);
        assertEquals(forPlayer, forDeveloper,
            "a planned version has no recipe to name, so developer mode must not change its text");
        assertFalse(forPlayer.contains("Generates with"), forPlayer);
    }

    @Test
    @DisplayName("the catalogue is all playable today, which is why the planned branch is covered synthetically")
    void catalogueHasNoPlannedVersionsYet() {
        // Recorded rather than assumed: if a version is ever declared unimplemented again, this fails and points at
        // the helper above, which should then read the real catalogue instead of building an entry.
        assertTrue(HistoricalVersionCatalogue.future().isEmpty(),
            "a planned version exists now: planned() should come from the catalogue again");
    }

    @Test
    @DisplayName("the caller's own context line is always shown, and an empty one leaves no blank line")
    void contextNoteIsKeptButNotBlank() {
        HistoricalVersionCatalogue.Entry entry = implemented();

        String withNote = VersionText.details(entry, "Current version: Beta 1.7.3", false);
        assertTrue(withNote.startsWith("Current version: Beta 1.7.3\n"), withNote);

        assertEquals(entry.displayName() + " - " + entry.era().displayName() + "\nImplemented.",
            VersionText.details(entry, "", false), "an empty note must not leave a leading blank line");
        assertEquals(VersionText.details(entry, "", false), VersionText.details(entry, null, false));
    }

    @Test
    @DisplayName("nothing highlighted shows the prompt rather than an empty panel")
    void nothingHighlightedPromptsThePlayer() {
        assertEquals(VersionText.SELECT_PROMPT, VersionText.details(null, null, false));
        assertEquals(VersionText.SELECT_PROMPT, VersionText.details(null, null, true),
            "the prompt is not developer output");
        assertEquals("Current version: Beta 1.7.3\n" + VersionText.SELECT_PROMPT,
            VersionText.details(null, "Current version: Beta 1.7.3", false));
    }

    @Test
    @DisplayName("the list row's second line is the measured build, and only in developer mode")
    void rowNoteIsDeveloperOnly() {
        HistoricalVersionCatalogue.Entry entry = implemented();
        assertEquals("emulates " + entry.emulated(), VersionText.rowNote(entry, true));
        assertEquals("", VersionText.rowNote(entry, false));
        assertEquals("", VersionText.rowNote(null, true));

        // Every entry without a measured build has no second line either way, so row height never depends on the
        // mode - a row that grew when the switch was flipped would misalign the whole list.
        HistoricalVersionCatalogue.entries().stream()
            .filter(candidate -> candidate.emulated().isEmpty())
            .forEach(candidate -> {
                assertEquals("", VersionText.rowNote(candidate, true), candidate.displayName());
                assertEquals("", VersionText.rowNote(candidate, false), candidate.displayName());
            });
    }

    @Test
    @DisplayName("no implemented version's player-facing text names any generation recipe")
    void everyEntryIsCleanForAPlayer() {
        // The whole catalogue, not a sample: this is the assertion that would have caught the reported screen.
        for (HistoricalVersionCatalogue.Entry entry : HistoricalVersionCatalogue.implemented()) {
            String text = VersionText.details(entry, null, false);
            assertFalse(text.contains("historical_experience:"), entry.displayName() + " -> " + text);
            assertFalse(text.contains("Generates with"), entry.displayName() + " -> " + text);
            assertFalse(text.contains("Emulates"), entry.displayName() + " -> " + text);
        }
    }
}

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

import net.historicalexperience.client.config.HistoricalDeveloperMode;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersionCatalogue;

/**
 * The words the version selector shows, and which of them a player has asked to see.
 *
 * <h2>Why the text is here and not in the screen</h2>
 *
 * <p>It was in the screen, and the screen is not a thing a test can call: the difference between a player's view and
 * a developer's is exactly the kind of decision that must be asserted rather than eyeballed, because getting it
 * wrong is invisible to whoever wrote it. So the strings are built here, from the catalogue's own values, and the
 * screen only draws them.
 *
 * <h2>What a player sees, and what developer mode adds</h2>
 *
 * <p>A player sees the version's name and era, whether it is implemented or still planned, and - for a planned
 * version - what the plan says about it. Developer mode adds the two things that name this project's internals: the
 * specific build a profile was measured against ({@code emulates ...}) and the generation recipe a world of that
 * version is created with, including its revision number ({@code Generates with <alias> (revision n)}). The recipe
 * line is the one that matters most: a raw alias like
 * {@code historical_experience:beta_1_7_3_content_preview_10} tells a player nothing and tells whoever is debugging a
 * world everything.
 *
 * <p>The owner's instruction was that these are developer output and must not be shown by default; see
 * {@link HistoricalDeveloperMode}.
 */
public final class VersionText {
    /** Shown in the details area when nothing is highlighted yet. */
    public static final String SELECT_PROMPT =
        "Select a version. Use the era buttons to browse Alpha, Beta and Release.";

    private VersionText() {
    }

    /**
     * The details text for the highlighted version.
     *
     * @param selected  the highlighted entry, or null when nothing is highlighted
     * @param note      the caller's own context line - the world's current version, or a staged-change warning -
     *                  or null/blank for none. Always shown: it is the caller's plain-English statement, not a
     *                  recipe identifier
     * @param developer whether developer-facing output should be included
     */
    public static String details(HistoricalVersionCatalogue.Entry selected, String note, boolean developer) {
        StringBuilder text = new StringBuilder();
        if (note != null && !note.isEmpty()) {
            text.append(note).append('\n');
        }
        if (selected == null) {
            text.append(SELECT_PROMPT);
            return text.toString();
        }

        text.append(selected.displayName()).append(" - ").append(selected.era().displayName());

        if (selected.status() == HistoricalVersionCatalogue.Status.FUTURE) {
            // Planned versions say what is missing. That is the answer to "why can I not pick this?", which a player
            // is entitled to; it names no recipe and no build.
            text.append('\n').append("Planned. ").append(selected.summary());
            return text.toString();
        }

        text.append('\n').append("Implemented.");
        if (!developer) {
            return text.toString();
        }
        if (!selected.emulated().isEmpty()) {
            text.append(" Emulates ").append(selected.emulated()).append('.');
        }
        HistoricalVersion.GenerationRevision revision = selected.version().latestRevision();
        text.append('\n').append("Generates with ").append(revision.alias())
            .append(" (revision ").append(revision.revision()).append(')');
        return text.toString();
    }

    /**
     * The second line of a version's row in the list, or an empty string when the row should have only one line.
     *
     * <p>Empty rather than null so the caller can test it directly, and empty for every entry without a measured
     * build even in developer mode, so the row height never depends on which mode is on.
     */
    public static String rowNote(HistoricalVersionCatalogue.Entry entry, boolean developer) {
        if (!developer || entry == null || entry.emulated().isEmpty()) {
            return "";
        }
        return "emulates " + entry.emulated();
    }
}

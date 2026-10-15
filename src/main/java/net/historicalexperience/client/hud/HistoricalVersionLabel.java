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
 */package net.historicalexperience.client.hud;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Whether a historical version drew its name in the top-left corner of the screen, and what the text said.
 *
 * <p>Before Beta 1.6.5 the game always wrote "Minecraft Alpha v1.0.4" (and so on) in the corner; F3 only extended it
 * with the frame counters. It was then taken off the plain screen and kept for F3 only, brought back for the Beta 1.8
 * pre-release, and removed again for the Release 1.0 build.
 *
 * <h2>Evidence</h2>
 *
 * <p>Read from the hash-verified Mojang client jars (SHA-1 matched against the launcher manifest) by disassembling the
 * class that draws the F3 text and looking at the branch around the version string. The corner label is the string
 * written when the debug option is OFF:
 *
 * <ul>
 *   <li>Always drawn: Infdev 20100618, Alpha v1.0.4, v1.0.11, v1.0.14, v1.0.17_04, v1.1.0, v1.1.2_01, v1.2.0, v1.2.6,
 *       Beta 1.0, 1.1_02, 1.2_02, 1.3, 1.4_01, 1.5_01, 1.6, 1.6.4, 1.8 and 1.8.1.</li>
 *   <li>Debug only: Beta 1.6.5, 1.6.6, 1.7 and 1.7.3, and Release 1.0.0. The plain-screen branch is simply absent.</li>
 * </ul>
 *
 * <p>The wiki's HUD history ({@code https://minecraft.wiki/w/Heads-up_display}) agrees, and supplies the three points
 * no downloadable jar covers: Beta 1.9 Prerelease 4 removed it again, Prerelease 5 restored it, and the release
 * removed it. Prerelease 6 is treated like Prerelease 5, because the wiki records no change at 6. Infdev 20100420 and
 * 20100611 are not on the launcher manifest; they are covered by the wiki's statement that the label was added in
 * Classic 0.0.2a and not removed until Beta 1.6.5.
 */
public final class HistoricalVersionLabel {
    /** Beta 1.6.5, the first build that stopped drawing it. No profile sits exactly here; the next one is Beta 1.7.3. */
    static final int REMOVED_IN_BETA_1_6_5 = HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 6, 5);

    /** Beta 1.8, where it came back. */
    static final int RESTORED_IN_BETA_1_8 = HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 8, 0);

    /** Beta 1.9 Prerelease 4 took it away again. */
    static final int REMOVED_IN_PRERELEASE_4 = HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 9, 4);

    /** Beta 1.9 Prerelease 5 restored it. */
    static final int RESTORED_IN_PRERELEASE_5 = HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 9, 5);

    private HistoricalVersionLabel() {
    }

    /** Whether this version drew its name in the corner of the plain game screen. */
    public static boolean shownIn(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        int chronology = version.chronology();
        if (chronology < REMOVED_IN_BETA_1_6_5) {
            return true;
        }
        if (chronology >= RESTORED_IN_BETA_1_8 && chronology < REMOVED_IN_PRERELEASE_4) {
            return true;
        }
        return chronology >= RESTORED_IN_PRERELEASE_5 && chronology < HistoricalVersions.ERA_RELEASE;
    }

    /**
     * The text the corner showed. Infdev wrote only "Minecraft Infdev", with no build, so that is what is returned for
     * it; every later build named itself, and this project's display names are those same names.
     */
    public static String textFor(HistoricalVersion version) {
        if (version.chronology() < HistoricalVersions.ERA_ALPHA) {
            return "Minecraft Infdev";
        }
        return "Minecraft " + version.displayName();
    }
}
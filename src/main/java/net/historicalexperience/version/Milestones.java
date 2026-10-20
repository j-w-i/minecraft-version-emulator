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

/**
 * Every boundary the feature table and the other version-keyed rules use, in one place and on one scale.
 *
 * <p>Each constant is a position in the timeline, not a promise that a version with that name is declared. The
 * ones that are declared builds are tied to the registry by {@code MilestonesTest}, so a boundary can never drift
 * away from the version it names.
 *
 * <p>The proxies are deliberately preserved: where a historical change landed between two declared builds, the
 * rule uses the declared build the project has always used for it (see the notes on each {@link Feature}). Changing
 * one is a historical decision, not a refactor.
 */
public final class Milestones {
    private Milestones() {
    }

    public static final Milestone ALPHA_1_1_0 = Milestone.of(Era.ALPHA, 1, 1, 0, "Alpha 1.1.0");
    public static final Milestone ALPHA_1_2_0 = Milestone.of(Era.ALPHA, 1, 2, 0, "Alpha 1.2.0");
    public static final Milestone BETA_1_2 = Milestone.of(Era.BETA, 1, 2, 0, "Beta 1.2");
    public static final Milestone BETA_1_6 = Milestone.of(Era.BETA, 1, 6, 0, "Beta 1.6");
    public static final Milestone BETA_1_7_3 = Milestone.of(Era.BETA, 1, 7, 3, "Beta 1.7.3");
    /** Beta 1.8, the Adventure Update. Not a declared build: Beta 1.8.1 is, and sorts just after it. */
    public static final Milestone BETA_1_8 = Milestone.of(Era.BETA, 1, 8, 0, "Beta 1.8");
    public static final Milestone BETA_1_9_PRE_3 = Milestone.of(Era.BETA, 1, 9, 3, "Beta 1.9 Prerelease 3");
    public static final Milestone BETA_1_9_PRE_4 = Milestone.of(Era.BETA, 1, 9, 4, "Beta 1.9 Prerelease 4");
    public static final Milestone RELEASE_1_1 = Milestone.of(Era.RELEASE, 1, 1, 0, "Release 1.1");
    public static final Milestone RELEASE_1_2_5 = Milestone.of(Era.RELEASE, 1, 2, 5, "Release 1.2.5");
    public static final Milestone RELEASE_1_4 = Milestone.of(Era.RELEASE, 1, 4, 0, "Release 1.4");
    public static final Milestone RELEASE_1_5 = Milestone.of(Era.RELEASE, 1, 5, 0, "Release 1.5");
    public static final Milestone RELEASE_1_6_4 = Milestone.of(Era.RELEASE, 1, 6, 4, "Release 1.6.4");
    /** Release 1.9, the combat update that added the offhand. Newer than every declared build. */
    public static final Milestone RELEASE_1_9 = Milestone.of(Era.RELEASE, 1, 9, 0, "Release 1.9");
}
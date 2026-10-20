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

    public static final Milestone ALPHA_1_0_1 = Milestone.of(Era.ALPHA, 1, 0, 1, "Alpha v1.0.1");
    /** Alpha v1.0.14: the arrow recipe switches from iron to flint, and chickens arrive. */
    public static final Milestone ALPHA_1_0_14 = Milestone.of(Era.ALPHA, 1, 0, 14, "Alpha v1.0.14");

    public static final Milestone ALPHA_1_1_0 = Milestone.of(Era.ALPHA, 1, 1, 0, "Alpha 1.1.0");
    public static final Milestone ALPHA_1_2_0 = Milestone.of(Era.ALPHA, 1, 2, 0, "Alpha 1.2.0");
    public static final Milestone BETA_1_2 = Milestone.of(Era.BETA, 1, 2, 0, "Beta 1.2");
    public static final Milestone BETA_1_6 = Milestone.of(Era.BETA, 1, 6, 0, "Beta 1.6");
    public static final Milestone BETA_1_7_3 = Milestone.of(Era.BETA, 1, 7, 3, "Beta 1.7.3");
    /** Beta 1.8, the Adventure Update. Not a declared build: Beta 1.8.1 is, and sorts just after it. */
    public static final Milestone BETA_1_8 = Milestone.of(Era.BETA, 1, 8, 0, "Beta 1.8");
    /** Beta 1.8.1, the first declared build of the Adventure Update. */
    public static final Milestone BETA_1_8_1 = Milestone.of(Era.BETA, 1, 8, 1, "Beta 1.8.1");
    /** Beta 1.7, where the cobblestone texture changed. Beta 1.7.3 is the declared build; this is the release itself. */
    public static final Milestone BETA_1_7 = Milestone.of(Era.BETA, 1, 7, 0, "Beta 1.7");
    public static final Milestone BETA_1_9_PRE_3 = Milestone.of(Era.BETA, 1, 9, 3, "Beta 1.9 Prerelease 3");
    public static final Milestone BETA_1_9_PRE_4 = Milestone.of(Era.BETA, 1, 9, 4, "Beta 1.9 Prerelease 4");
    public static final Milestone RELEASE_1_1 = Milestone.of(Era.RELEASE, 1, 1, 0, "Release 1.1");
    public static final Milestone RELEASE_1_2_5 = Milestone.of(Era.RELEASE, 1, 2, 5, "Release 1.2.5");
    public static final Milestone RELEASE_1_4 = Milestone.of(Era.RELEASE, 1, 4, 0, "Release 1.4");
    public static final Milestone RELEASE_1_5 = Milestone.of(Era.RELEASE, 1, 5, 0, "Release 1.5");
    public static final Milestone RELEASE_1_6_4 = Milestone.of(Era.RELEASE, 1, 6, 4, "Release 1.6.4");
    /** Release 1.9, the combat update that added the offhand. Newer than every declared build. */
    public static final Milestone RELEASE_1_9 = Milestone.of(Era.RELEASE, 1, 9, 0, "Release 1.9");

    /** The oldest declared Infdev build; the start of the era this project's flat-sky rule covers. */
    public static final Milestone INFDEV_20100420 = Milestone.ofKey(300_420, "Infdev 20100420");
    public static final Milestone BETA_1_0 = Milestone.of(Era.BETA, 1, 0, 0, "Beta 1.0");
    public static final Milestone BETA_1_3 = Milestone.of(Era.BETA, 1, 3, 0, "Beta 1.3");
    public static final Milestone BETA_1_5 = Milestone.of(Era.BETA, 1, 5, 0, "Beta 1.5");
    /** Beta 1.6.5 stopped drawing the version label. Not declared: the next declared build is Beta 1.7.3. */
    public static final Milestone BETA_1_6_5 = Milestone.of(Era.BETA, 1, 6, 5, "Beta 1.6.5");
    public static final Milestone BETA_1_9_PRE_1 = Milestone.of(Era.BETA, 1, 9, 1, "Beta 1.9 Prerelease 1");
    public static final Milestone BETA_1_9_PRE_2 = Milestone.of(Era.BETA, 1, 9, 2, "Beta 1.9 Prerelease 2");
    public static final Milestone BETA_1_9_PRE_5 = Milestone.of(Era.BETA, 1, 9, 5, "Beta 1.9 Prerelease 5");
    public static final Milestone RELEASE_1_0 = Milestone.of(Era.RELEASE, 1, 0, 0, "Release 1.0");
    public static final Milestone RELEASE_1_3 = Milestone.of(Era.RELEASE, 1, 3, 0, "Release 1.3");
    /** The Pretty Scary Update, released as 1.4.2. Not declared: Release 1.4 is the nearest declared build. */
    public static final Milestone RELEASE_1_4_2 = Milestone.of(Era.RELEASE, 1, 4, 2, "Release 1.4.2");
    /** The attribute system. Not declared: Release 1.6.4 is the nearest declared build. */
    public static final Milestone RELEASE_1_6_1 = Milestone.of(Era.RELEASE, 1, 6, 1, "Release 1.6.1");
    public static final Milestone RELEASE_1_7_2 = Milestone.of(Era.RELEASE, 1, 7, 2, "Release 1.7.2");
    public static final Milestone RELEASE_1_8 = Milestone.of(Era.RELEASE, 1, 8, 0, "Release 1.8");
    /** The 16w20a snapshot lands in 1.10; the first release in which endermen spawn in the Nether. */
    public static final Milestone RELEASE_1_10 = Milestone.of(Era.RELEASE, 1, 10, 0, "Release 1.10");
    /** Release 1.12, where coloured beds arrive. Above every declared build. */
    public static final Milestone RELEASE_1_12 = Milestone.of(Era.RELEASE, 1, 12, 0, "Release 1.12");
    /** The first key of the Release family: the upper bound of everything Alpha and Beta. */
    public static final Milestone START_OF_RELEASE = Milestone.ofKey(Era.RELEASE.base(), "start of the Release family");
}
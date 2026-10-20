/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.gameplay;

import java.util.List;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.version.Milestones;

/**
 * Which music disc a skeleton-killed creeper dropped, and whether it dropped one at all.
 *
 * <p>minecraft.wiki, Creeper history (Java Edition), the three rows that define every declared build:
 * <ul>
 *   <li>"v1.0.14 Added music discs '13' and 'cat', which are dropped by creepers shot by skeletons"</li>
 *   <li>"1.1 (11w50a) Creepers now drop all music discs, except for '11'"</li>
 *   <li>"1.4.4 (1.4.3) Creepers now drop music discs '11' and 'wait'"</li>
 * </ul>
 *
 * <p>The declared Release 1.4 profile is the 1.4.2 baseline, which predates 1.4.3, so "11" and "wait" belong to
 * Release 1.5 and 1.6.4 only.
 *
 * <p>The drop is IN ADDITION to gunpowder: the wiki rows describe a disc creepers "drop ... when shot", and modern
 * vanilla implements it as an extra pool. Recorded because the rows do not say whether it replaced gunpowder, and no
 * extractor here proves which.
 */
public final class HistoricalDiscDrops {
    private HistoricalDiscDrops() {
    }

    /** True when this version's creepers dropped a disc at all. */
    public static boolean applies(String versionId) {
        return Features.active(Feature.CREEPER_DISC_DROPS, versionId);
    }

    /** The discs a skeleton-killed creeper could choose from in this version; empty when there is no such drop. */
    public static List<String> eligible(String versionId) {
        if (!applies(versionId)) {
            return List.of();
        }
        var declared = HistoricalVersions.byId(versionId).orElse(null);
        if (declared == null) {
            return List.of();
        }
        if (declared.isBefore(Milestones.RELEASE_1_1)) {
            // Alpha v1.0.14 through Beta 1.9 Pre6, and Release 1.0.
            return List.of("minecraft:music_disc_13", "minecraft:music_disc_cat");
        }
        var discs = new java.util.ArrayList<>(List.of(
            "minecraft:music_disc_13", "minecraft:music_disc_cat", "minecraft:music_disc_blocks",
            "minecraft:music_disc_chirp", "minecraft:music_disc_far", "minecraft:music_disc_mall",
            "minecraft:music_disc_mellohi", "minecraft:music_disc_stal", "minecraft:music_disc_strad",
            "minecraft:music_disc_ward"));
        if (!declared.isBefore(Milestones.RELEASE_1_5)) {
            // 1.4.3 added "11" and "wait"; the declared Release 1.4 profile is 1.4.2, before it.
            discs.add("minecraft:music_disc_11");
            discs.add("minecraft:music_disc_wait");
        }
        return List.copyOf(discs);
    }

    /** One eligible disc, or null when this version has no such drop. */
    public static String pick(String versionId, net.minecraft.util.RandomSource random) {
        var discs = eligible(versionId);
        if (discs.isEmpty() || random == null) {
            return null;
        }
        return discs.get(random.nextInt(discs.size()));
    }
}

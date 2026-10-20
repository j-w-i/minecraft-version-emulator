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
package net.historicalexperience.gameplay;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.version.HistoricalSnapshotPatches;
import net.historicalexperience.version.HistoricalVersions;

/** Selection-level policy only. Server adapters must independently validate the player's world and authority. */
public final class HistoricalCreativeRules {
    private HistoricalCreativeRules() { }

    /** Unknown or ordinary selections do not acquire historical creative policy. */
    public static boolean gatesCatalog(String selectionId) {
        return HistoricalSnapshotPatches.baseProfileForSelection(selectionId).isPresent();
    }

    /** Beta 1.8.1 through the 1.2 line; 1.3's family uses the tabbed 1.3.1 layout (12w21b). */
    public static boolean usesOldMenu(String selectionId) {
        var version = HistoricalSnapshotPatches.baseProfileForSelection(selectionId)
            .flatMap(HistoricalVersions::byId).orElse(null);
        if (version == null) return false;
        return Features.active(Feature.OLD_CREATIVE_MENU, version);
    }

    public static boolean hasCreativeShortcut(String snapshotPatchId) {
        return HistoricalSnapshotPatches.byId(snapshotPatchId)
            .map(patch -> patch.enables(HistoricalSnapshotPatches.Feature.B_CREATIVE_SHORTCUT)).orElse(false);
    }

    /** Deliberate multiplayer safety boundary, not a claim about unverified original multiplayer behavior. */
    public static boolean allowsCreativeShortcut(String snapshotPatchId, boolean ownsPrivateSingleplayerWorld) {
        return ownsPrivateSingleplayerWorld && hasCreativeShortcut(snapshotPatchId);
    }

    /** The requested single-target constraint also applies to Creative's normal compatibility bypass. */
    public static boolean rejectsStackedBookTarget(String snapshotPatchId, int targetCount, boolean enchantedBook) {
        return targetCount > 1 && enchantedBook && HistoricalSnapshotPatches.byId(snapshotPatchId)
            .map(patch -> patch.enables(HistoricalSnapshotPatches.Feature.BOOK_ON_ANY_SINGLE_ITEM)).orElse(false);
    }

    /** Relax target eligibility only: never grant free XP, waive material consumption or rewrite existing inputs. */
    public static boolean allowsBookOnAnyItem(String snapshotPatchId, int targetCount, boolean enchantedBook) {
        return targetCount == 1 && enchantedBook && HistoricalSnapshotPatches.byId(snapshotPatchId)
            .map(patch -> patch.enables(HistoricalSnapshotPatches.Feature.BOOK_ON_ANY_SINGLE_ITEM)).orElse(false);
    }
}

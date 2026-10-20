/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.client.lighting;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;

/**
 * The old chest and ender chest: their versions, and the toggles for them.
 *
 * <h2>The boundaries, both stated by the wiki</h2>
 *
 * <ul>
 *   <li><b>Chest.</b> minecraft.wiki, Chest history, <b>Beta 1.8 Pre-release</b>: <i>"The chest's model has been
 *       redone. Rather than a usual cube, it uses an entity model, which allows for an opening and closing animation,
 *       and is also smaller."</i> So the old cube is every declared build BELOW Beta 1.8.</li>
 *   <li><b>Ender chest.</b> Added in <b>Release 1.3.1</b> (12w21a) - <i>"Added ender chests."</i> - and its vanilla
 *       block-entity appearance was not replaced by a cube in these historical releases. The cube here is the NT
 *       optional visual aesthetic, not a claim about authentic 1.3.1+ vanilla rendering; it is only offered when the
 *       block existed, from Release 1.3.1 through the newest declared profile.</li>
 * </ul>
 *
 * <p>A correction worth keeping: the common statement that the ender chest arrived in Beta 1.8 is wrong, and the beta
 * profiles must not be given one. The wiki's own history puts it in 1.3.1.
 */
public final class HistoricalChestTweaks {
    private static final String CHEST_KEY = "oldChest";
    private static final String ENDER_CHEST_KEY = "oldEnderChest";
    private HistoricalChestTweaks() {
    }

    /** The old cube chest belongs to every declared build before Beta 1.8. */
    public static boolean availableForChest(HistoricalVersion version) {
        return Features.active(Feature.OLD_CHEST_MODEL, version);
    }

    /** An old ender chest belongs to Release 1.3.1 onwards, since that is when the block existed. */
    public static boolean availableForEnderChest(HistoricalVersion version) {
        return Features.active(Feature.OLD_ENDER_CHEST_MODEL, version);
    }

    /** Whether the old chest block model is wanted in the active client world. */
    public static boolean oldChest() {
        return wantsOldChest(HistoricalClientRules.activeVersion());
    }

    /** Pure version/config decision, separately testable from the live client singleton. */
    public static boolean wantsOldChest(HistoricalVersion version) {
        return availableForChest(version) && HistoricalClientOptions.get(CHEST_KEY, true);
    }

    /** Whether the old ender chest block model is wanted in the active client world. */
    public static boolean oldEnderChest() {
        return wantsOldEnderChest(HistoricalClientRules.activeVersion());
    }

    /** Pure version/config decision, separately testable from the live client singleton. */
    public static boolean wantsOldEnderChest(HistoricalVersion version) {
        return availableForEnderChest(version) && HistoricalClientOptions.get(ENDER_CHEST_KEY, true);
    }

    /**
     * Resource hook predicate: historical block models are baked for every reload, before a world is known.
     * RenderShape and ItemRenderer make the actual runtime choice, so these models are only assets, not a global swap.
     */
    public static boolean hasHistoricalBlockState(String namespace, String path) {
        return "minecraft".equals(namespace)
            && ("chest".equals(path) || "trapped_chest".equals(path) || "ender_chest".equals(path));
    }

}

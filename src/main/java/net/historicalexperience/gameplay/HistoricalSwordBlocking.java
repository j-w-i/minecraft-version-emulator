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
package net.historicalexperience.gameplay;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;


import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;

/**
 * Sword blocking, as every version before the 1.9 combat update did it: hold a sword, right-click to raise
 * it, and take half the damage while it is up.
 *
 * <h2>Source</h2>
 *
 * <p>A faithful translation of Nostalgic Tweaks {@code helper/gameplay/combat/SwordBlockingHelper.java}
 * (LGPL-3.0, see THIRD_PARTY_NOTICES.md), revision
 * {@code 010526deaf3aa5e562b45b60a286a667e8eeac12}. The logic is upstream's; what differs is that the
 * config flags are replaced by the world's version, so the behaviour follows an upgrade by itself.
 *
 * <p>Upstream's third case, blocking with a shield in the off-hand, is deliberately not carried across:
 * shields arrive in Release 1.9, and this project declares no version that had one, so the code could only
 * ever be dead. It is left out rather than included and unreachable.
 */
public final class HistoricalSwordBlocking {
    /** Upstream's default, and the value both sides of the old game agreed on. */
    public static final int DAMAGE_REDUCTION_PERCENT = 50;

    private HistoricalSwordBlocking() {
    }

    /**
     * Whether the player has a sword raised.
     *
     * <p>Upstream's exact condition: the item is in use <em>and</em> it is a sword with an empty off hand.
     */
    public static boolean isBlocking(Player player) {
        return player.isUsingItem() && canBlock(player);
    }

    /**
     * Whether this player's held item can be raised to block at all.
     *
     * <p>Requires an empty off hand, because a sword in the main hand and anything in the off hand is not
     * the old stance.
     */
    public static boolean canBlock(Player player) {
        if (!player.getOffhandItem().isEmpty()) {
            return false;
        }
        // The ACTIVE SESSION, not the player's level: this has to answer the same way on the client, where the
        // level is a ClientLevel and a level-based question is always false. Asking the level here is what made
        // the stance invisible while the server still reduced its damage.
        return Features.active(Feature.OLD_SWORD_BLOCKING)
            && player.getMainHandItem().is(ItemTags.SWORDS);
    }

    /** The damage multiplier applied while blocking, from upstream's percentage. */
    public static float damageMultiplier() {
        return 1.0F - DAMAGE_REDUCTION_PERCENT / 100.0F;
    }
}

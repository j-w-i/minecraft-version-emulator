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

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
/**
 * Raises a sword to block when the player right-clicks with one, as every version from Beta 1.8 until 1.9 did.
 *
 * <h2>Source</h2>
 *
 * <p>A faithful port of Nostalgic Tweaks' right-click handler in
 * {@code listener/common/InteractionListener.java} (LGPL-3.0, see THIRD_PARTY_NOTICES.md), revision
 * {@code 010526deaf3aa5e562b45b60a286a667e8eeac12}:
 *
 * <pre>
 * if (SwordBlockingHelper.canBlock(player) &amp;&amp; !player.isUsingItem()) {
 *     player.startUsingItem(hand);
 *     return CompoundEventResult.interruptTrue(itemStackInHand);
 * }
 * </pre>
 *
 * <p>Upstream reaches this through its own event abstraction; here it is Fabric's use-item callback, which
 * is the same hook this loader provides. Upstream's {@code interruptTrue} means "this interaction is
 * handled, stop looking", which is {@link InteractionResult#CONSUME} in this API - the return type here is
 * Fabric's own, not a holder, which is a difference between the two Fabric API versions rather than a
 * difference in intent.
 *
 * <p>The boundary is the timeline's: blocking was added in the Beta 1.8 Pre-release and removed in 1.9
 * (15w33c), so {@link HistoricalSwordBlocking#canBlock} answers for Beta 1.8.1 and later and for none of
 * the earlier versions.
 */
public final class HistoricalSwordBlockingEvents {
    private HistoricalSwordBlockingEvents() {
    }

    /** Registers the right-click handler. Called once from the mod initialiser. */
    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.MAIN_HAND
                    || player.isUsingItem()
                    || !HistoricalSwordBlocking.canBlock(player)) {
                // 1.21.1's Fabric API predates InteractionResult, so the answer is a TypedActionResult
                // carrying the stack. PASS and CONSUME mean what they mean in the later API: let vanilla
                // handle the interaction, or treat it as already handled.
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        });
    }
}

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

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.historicalexperience.mixin.AxeAccess;
import net.historicalexperience.mixin.ShovelAccess;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Refuses axe stripping and shovel pathing, which are features every declared version predates.
 *
 * <h2>Source, and what is upstream's</h2>
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/gameplay/InteractionHelper.java} and the right-click
 * block branch of {@code listener/common/InteractionListener.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Upstream
 * answers {@code GameplayTweak.DISABLE_AXE_STRIPPING} and {@code DISABLE_SHOVEL_PATHING}; the owner asked for both
 * by name. The predicates are upstream's verbatim and so is the use of the game's own strip/flatten tables rather
 * than a copied list:
 *
 * <pre>
 * if (DISABLE_SHOVEL_PATHING &amp;&amp; item instanceof ShovelItem)
 *     if (ShovelItemAccess.NT$FLATTENABLES().containsKey(blockState.getBlock())) return true;
 * if (DISABLE_AXE_STRIPPING &amp;&amp; item instanceof AxeItem)
 *     if (AxeItemAccess.NT$STRIPPABLES().containsKey(blockState.getBlock())) return true;
 * </pre>
 *
 * <p>Only the yes/no source changed: upstream reads a config flag, this project reads the world's version, which is
 * the whole point of the port.
 *
 * <h2>Boundaries, which are the wiki's and not NT's</h2>
 *
 * <p>Axe stripping is <b>1.13 (17w47a)</b> — <i>"Axes can now be used to strip logs into stripped logs."</i> — and
 * shovel pathing is <b>1.9 (15w31a)</b> — <i>"Shovels can now be used to create paths."</i> The newest version this
 * project declares is Release 1.6.4, so both features are anachronistic in <b>all 32 declared builds</b> and one
 * gate covers both. That is why the boundary is written as "is a declared historical version": it is not a range
 * whose upper end matters, it is every build in the matrix.
 *
 * <p>Recorded because NT's own catalogue lists both as {@code unknown} and its {@code historicalClaim} field says
 * the name alone is not evidence — so the boundaries above come from the wiki, not from upstream.
 *
 * <h2>Why FAIL rather than SUCCESS, and why it must be FAIL on the CLIENT too</h2>
 *
 * <p>Fabric documents the three answers: SUCCESS cancels processing and tells the client to inform the server, PASS
 * falls through, and <b>FAIL cancels processing and sends nothing</b>. Refusing a strip must not send anything, so
 * FAIL is the exact fit.
 *
 * <p><b>This handler was server-only and that was the defect the owner found.</b> It returned PASS when
 * {@code level.isClientSide()}, reasoning that the server should be the authority. But Fabric injects this callback
 * into the <b>client's own interaction path</b> -
 * {@code ClientPlayerInteractionManager.interactBlock}, with the framework's own comment that it sits there
 * <i>"to avoid sending a packet in line with the event javadoc"</i>. Returning PASS there let vanilla run its local
 * {@code useOn}, so the client <b>predicted the strip and changed the log on screen</b>, sent the packet, and the
 * server - which ran the same handler and correctly answered FAIL - refused it. The log then snapped back.
 *
 * <p>That is the owner's "it strips and reverts", and Nostalgic Tweaks does not have it because upstream refuses on
 * <b>both</b> sides: its listener is a common one that returns {@code interruptFalse} wherever it runs. A refusal is
 * not a server-authoritative decision to be replicated; it is a decision the client must also make, or it predicts a
 * change that will not survive.
 *
 * <p>So there is no side test. The tables read are vanilla's own static strip and flatten maps, present on both
 * sides, and the version gate reads the session, which the server pushes to the client for exactly this reason.
 */
public final class HistoricalToolInteractionEvents {
    private HistoricalToolInteractionEvents() {
    }

    /** Registers the right-click-block handler. Called once from the mod initialiser. */
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!Features.active(Feature.NO_TOOL_SHAPING)) {
                return InteractionResult.PASS;
            }

            // NO isClientSide() TEST HERE, deliberately - see the class comment. Both sides must refuse, because
            // the client predicts the interaction locally before the server ever sees it.
            BlockState state = level.getBlockState(hitResult.getBlockPos());
            var item = player.getItemInHand(hand).getItem();

            if (item instanceof AxeItem && AxeAccess.strippables().containsKey(state.getBlock())) {
                return InteractionResult.FAIL;
            }
            if (item instanceof ShovelItem && ShovelAccess.flattenables().containsKey(state.getBlock())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    /**
     * Fires a bow the instant it is used, in versions that had no charge cycle.
     *
     * <h2>Source</h2>
     *
     * <p>DIRECT PORT of Nostalgic Tweaks
     * {@code listener/common/InteractionListener.java}, method {@code onRightClickItem} (LGPL-3.0; see
     * THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.INSTANT_BOW} and whose charge window is computed
     * from {@code ARROW_SPEED}:
     *
     * <pre>
     * if (INSTANT_BOW &amp;&amp; itemInHand.equals(Items.BOW)) {
     *     int timeCharged = 72000 - (int) ((ARROW_SPEED / 100.0F) * 20.0F);
     *     bow.releaseUsing(itemStackInHand, player.level(), player, timeCharged);
     *     return CompoundEventResult.interruptTrue(itemStackInHand);
     * }
     * </pre>
     *
     * <p>Both constants are upstream's and both are kept: the {@code 72000} is vanilla's "held forever" start time,
     * and the subtraction is what makes the bow read as <i>already charged</i> by the arrow-speed percentage rather
     * than fully drawn. The percentage itself lives in
     * {@link HistoricalGameplayTweaks#eraArrowSpeedPercent()} with the owner's figure and its reasoning.
     *
     * <h2>Why the charge is expressed this way rather than by shortening the draw</h2>
     *
     * <p>Shortening the use duration would change how long the bow can be held and how the draw animation reads.
     * Releasing as already-charged reproduces what the era actually did - one click, one arrow, no draw - and
     * leaves every other bow behaviour untouched, which is also upstream's choice.
     *
     * <p>Server side only, matching the block rule above and upstream's own {@code isServer()} guard on the
     * neighbouring branches: the server decides whether an arrow is spawned.
     */
    public static void registerInstantBow() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (hand != InteractionHand.MAIN_HAND
                    || !(stack.getItem() instanceof BowItem bow)
                    || !Features.active(Feature.INSTANT_BOW)) {
                return InteractionResultHolder.pass(stack);
            }
            if (level.isClientSide()) {
                return InteractionResultHolder.pass(stack);
            }

            int timeCharged = 72000
                - (int) ((HistoricalGameplayTweaks.eraArrowSpeedPercent() / 100.0F) * 20.0F);
            bow.releaseUsing(stack, level, player, timeCharged);
            return InteractionResultHolder.consume(stack);
        });
    }
}

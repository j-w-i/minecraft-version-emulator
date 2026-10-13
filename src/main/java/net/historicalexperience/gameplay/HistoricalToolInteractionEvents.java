package net.historicalexperience.gameplay;

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
 * <h2>Why FAIL rather than SUCCESS</h2>
 *
 * <p>Fabric documents the three answers: SUCCESS cancels processing and tells the client to inform the server, PASS
 * falls through, and <b>FAIL cancels processing and sends nothing</b>. Refusing a strip must not send anything -
 * the point is that the interaction does not happen - so FAIL is the exact fit, and it is what upstream's
 * {@code EventResult.interruptTrue} compiles to in this API version.
 */
public final class HistoricalToolInteractionEvents {
    private HistoricalToolInteractionEvents() {
    }

    /** Registers the right-click-block handler. Called once from the mod initialiser. */
    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!HistoricalGameplayTweaks.noToolShaping()) {
                return InteractionResult.PASS;
            }
            // Server side only. The server decides whether the block changes; a client that refused locally would
            // predict a strip that the server then performed, which is the desync this project keeps being careful
            // about. Returning PASS on the client lets the server's refusal arrive as the authoritative answer.
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

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
                    || !HistoricalGameplayTweaks.instantBow()) {
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

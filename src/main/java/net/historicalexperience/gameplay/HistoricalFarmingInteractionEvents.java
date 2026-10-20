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

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;

/**
 * Two interactions that older versions had and the modern game does not: a hoe throwing wheat seeds off grass, and a
 * punch priming TNT.
 *
 * <p>Both are ported from Nostalgic Tweaks {@code listener/common/InteractionListener.java} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md). Upstream answers {@code GameplayTweak.TILLED_GRASS_SEEDS} and
 * {@code PUNCH_TNT_IGNITION}; here each answers the world's version, and the boundaries are in
 * {@link HistoricalGameplayTweaks}, each with its wiki quotation:
 *
 * <ul>
 *   <li>seeds: gone in Beta 1.6 Test Build 3, so present in every declared version older than the Beta 1.6
 *       profile;</li>
 *   <li>punching TNT: gone in Beta 1.7, so present in every declared version older than the Beta 1.7.3 profile.</li>
 * </ul>
 *
 * <h2>Why neither is server-only, and neither is client-only</h2>
 *
 * <p>The lesson of the axe/shovel defect applies here in both directions. Fabric fires these callbacks on the
 * client's own interaction path as well as the server's, so a handler that answers only on the server lets the client
 * <em>predict</em> an outcome the server then contradicts. The seed drop is purely a server-side world change (an item
 * entity), so the client has nothing to predict and the handler is a no-op there. The TNT punch is different: the
 * client must return a non-PASS result or vanilla starts breaking the block locally, and the packet that reaches the
 * server must be sent for the server to act. So it answers SUCCESS on both sides and only the server mutates the
 * world.
 */
public final class HistoricalFarmingInteractionEvents {
    private HistoricalFarmingInteractionEvents() {
    }

    /** One seed drop in ten, the figure upstream uses for the old behaviour. */
    private static final int SEED_ODDS = 10;

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || !Features.active(Feature.GRASS_SEEDS)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            if (!(player.getItemInHand(hand).getItem() instanceof HoeItem)
                    || !level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                return InteractionResult.PASS;
            }
            dropSeed(level.getRandom(), (ServerLevel) level, pos);
            // PASS, not a result of ours: the hoe must still till the grass. The seed was a by-product of tilling,
            // not a replacement for it.
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!Features.active(Feature.PUNCH_TNT)
                    || player.isCrouching()
                    || !(level.getBlockState(pos).getBlock() instanceof TntBlock)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                TntBlock.explode(level, pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** Upstream's drop: a one-in-ten roll, only onto open air, thrown with a small upward kick. */
    static void dropSeed(RandomSource random, ServerLevel level, BlockPos grass) {
        if (!level.getBlockState(grass.above()).isAir() || random.nextInt(SEED_ODDS) != 0) {
            return;
        }
        double x = grass.getX() + 0.5D + Mth.nextDouble(random, -0.05D, 0.05D);
        double y = grass.getY() + 1.0D;
        double z = grass.getZ() + 0.5D + Mth.nextDouble(random, -0.05D, 0.05D);
        double dx = Mth.nextDouble(random, -0.1D, 0.1D);
        double dy = Mth.nextDouble(random, 0.18D, 0.2D);
        double dz = Mth.nextDouble(random, -0.1D, 0.1D);

        ItemEntity seed = new ItemEntity(level, x, y, z, new ItemStack(Items.WHEAT_SEEDS), dx, dy, dz);
        seed.setDefaultPickUpDelay();
        level.addFreshEntity(seed);
    }
}

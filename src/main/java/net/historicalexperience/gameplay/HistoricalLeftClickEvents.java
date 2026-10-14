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

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Nostalgic Tweaks listener/common/InteractionListener.onLeftClickBlock
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), limited to historical survival worlds.
 * The server resolves its own profile; common code must never load client classes.
 * Release 1.4 represents the published Pretty Scary Update (1.4.2), where the
 * owner-specified left-click behaviour ends, not an unreleased 1.4.0 build.
 */
public final class HistoricalLeftClickEvents {
    private static final String FIRST_WITHOUT_LEFT_CLICK = "historical_experience:release_1_4";

    private HistoricalLeftClickEvents() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!(level instanceof ServerLevel serverLevel)
                    || player.isCreative() || player.isSpectator() || player.swinging) {
                return InteractionResult.PASS;
            }
            String profileId = HistoricalProfileLookup.profileIdForLevel(serverLevel.getServer().overworld());
            HistoricalVersion version = profileId == null ? null : HistoricalVersions.byId(profileId).orElse(null);
            if (!availableFor(version)) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock
                    || state.getBlock() instanceof TrapDoorBlock
                    || state.getBlock() instanceof FenceGateBlock
                    || state.getBlock() instanceof LeverBlock
                    || state.getBlock() instanceof ButtonBlock) {
                use(serverLevel, player, pos, direction, state);
            }
            // Upstream passes through: activation does not cancel the normal attack.
            return InteractionResult.PASS;
        });
    }

    private static void use(ServerLevel level, Player player, BlockPos pos, Direction direction, BlockState state) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), direction, pos, false);
        state.useWithoutItem(level, player, hit);
    }

    public static boolean availableFor(HistoricalVersion version) {
        return version != null && HistoricalVersions.byId(FIRST_WITHOUT_LEFT_CLICK)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
    }
}

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
package net.historicalexperience.verification;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the End dragon fight's own methods and reports what they built, so the original-fight rules can be checked
 * against the real 1.21.1 classes. Inert unless {@code -Dhistorical_experience.dragonProbe=true}.
 *
 * <p>Needed because a headless server has no player, and the fight only runs its logic for a boss event with players
 * in it, so the rules cannot be triggered by commands alone. This calls the same private methods the fight calls
 * itself, which is exactly the code the mixins sit in front of.
 */
public final class DragonFightVerification {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/dragon-probe");

    private DragonFightVerification() {
    }

    public static void registerIfRequested() {
        if (Boolean.getBoolean("historical_experience.dragonProbe")) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> run(server)));
        }
    }

    private static void run(MinecraftServer server) {
        try {
            ServerLevel end = server.getLevel(Level.END);
            EndDragonFight fight = end.getDragonFight();
            LOGGER.info("DRAGONPROBE alias={} fightPresent={}",
                HistoricalFeatureExclusions.historicalAlias(server.overworld().getChunkSource().getGenerator()),
                fight != null);
            if (fight == null) {
                return;
            }

            // 1. Gateway: the fight's own death handler calls spawnNewGateway(); do the same and look for the frame.
            for (int k = 1; k <= 20; k++) {
                int x = (int) Math.floor(96.0D * Math.cos(Math.PI * k / 10.0D));
                int z = (int) Math.floor(96.0D * Math.sin(Math.PI * k / 10.0D));
                end.getChunk(new ChunkPos(new BlockPos(x, 75, z)).x, new ChunkPos(new BlockPos(x, 75, z)).z,
                    ChunkStatus.FULL, true);
            }
            Method spawnNewGateway = EndDragonFight.class.getDeclaredMethod("spawnNewGateway");
            spawnNewGateway.setAccessible(true);
            spawnNewGateway.invoke(fight);
            int gateways = 0;
            for (int k = 1; k <= 20; k++) {
                int x = (int) Math.floor(96.0D * Math.cos(Math.PI * k / 10.0D));
                int z = (int) Math.floor(96.0D * Math.sin(Math.PI * k / 10.0D));
                if (end.getBlockState(new BlockPos(x, 75, z)).is(Blocks.END_GATEWAY)) {
                    gateways++;
                }
            }
            LOGGER.info("DRAGONPROBE gatewayBlocksAfterDeath={}", gateways);

            // 2. Crystal re-summon: with the dragon marked dead and no portal known, tryRespawn builds the podium.
            end.getChunk(0, 0, ChunkStatus.FULL, true);
            Field dragonKilled = EndDragonFight.class.getDeclaredField("dragonKilled");
            dragonKilled.setAccessible(true);
            dragonKilled.setBoolean(fight, true);
            Field portalLocation = EndDragonFight.class.getDeclaredField("portalLocation");
            portalLocation.setAccessible(true);
            portalLocation.set(fight, null);
            boolean podiumBefore = end.getBlockState(new BlockPos(0, 60, 0)).is(Blocks.BEDROCK)
                || anyBedrockColumn(end);
            fight.tryRespawn();
            boolean podiumAfter = anyBedrockColumn(end);
            LOGGER.info("DRAGONPROBE tryRespawnBuiltPodium={} (before={})", podiumAfter && !podiumBefore, podiumBefore);

            // 3. Outer-island gateways: generate some End Highlands chunks and count what the hook refused.
            long before = HistoricalFeatureExclusions.skippedPlacements();
            int highlandChunks = 0;
            for (int cx = 120; cx < 144; cx += 2) {
                for (int cz = -6; cz < 6; cz += 2) {
                    end.getChunk(cx, cz, ChunkStatus.FULL, true);
                    highlandChunks++;
                }
            }
            LOGGER.info("DRAGONPROBE highlandChunks={} skippedPlacements={}", highlandChunks,
                HistoricalFeatureExclusions.skippedPlacements() - before);
            int returnGateways = 0;
            for (int cx = 120; cx < 144; cx += 2) {
                for (int cz = -6; cz < 6; cz += 2) {
                    var chunk = end.getChunk(cx, cz);
                    for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                        if (chunk.getBlockState(pos).is(Blocks.END_GATEWAY)) {
                            returnGateways++;
                        }
                    }
                }
            }
            LOGGER.info("DRAGONPROBE outerIslandGatewayBlocks={}", returnGateways);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            LOGGER.error("DRAGONPROBE failed", failure);
        }
    }

    private static boolean anyBedrockColumn(ServerLevel end) {
        for (int y = 0; y < 120; y++) {
            if (end.getBlockState(new BlockPos(0, y, 0)).is(Blocks.BEDROCK)) {
                return true;
            }
        }
        return false;
    }
}
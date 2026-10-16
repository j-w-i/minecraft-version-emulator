/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.generation;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.world.upgrade.FiniteApronRelease;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;

/** Generator-instance-local transition settings. No active-session global can affect an ordinary world's terrain. */
public final class FiniteTransitionRuntime {
    private static final Map<ChunkGenerator, FiniteApronRelease.Plan> PLANS =
        Collections.synchronizedMap(new WeakHashMap<>());
    private FiniteTransitionRuntime() { }

    public static FiniteApronRelease.Plan planFor(ChunkGenerator generator) { return PLANS.get(generator); }

    /** Deny future generation writes into ALL source terrain, including edited exterior chunks. Player writes stay legal. */
    public static boolean protectsWrite(net.minecraft.world.level.WorldGenLevel level, net.minecraft.core.BlockPos pos) {
        var plan = planFor(level.getLevel().getChunkSource().getGenerator());
        return plan != null && plan.protectedTerrainChunks().contains(
            net.minecraft.world.level.ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    public static void register() {
        ServerWorldEvents.LOAD.register((server, level) -> {
            if (!level.dimension().equals(Level.OVERWORLD)) return;
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            if (HistoricalProfileLookup.profileIdForGenerator(generator) == null) return;
            try {
                var plan = FiniteApronRelease.read(server.getWorldPath(LevelResource.ROOT));
                if (plan != null) {
                    if (!plan.complete()) throw new IOException("Finite terrain migration remains pending");
                    plan.identity().verifySeed(level.getSeed());
                    PLANS.put(generator, plan);
                    net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
                        "FINITE_BLEND attached: knownEdges={}/1024 collar=64 releasedChunks={}",
                        plan.knownEdgeColumns(), plan.releasedChunks());
                }
            } catch (IOException failure) {
                throw new IllegalStateException("Cannot safely load the finite terrain transition", failure);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PLANS.clear());
    }
}

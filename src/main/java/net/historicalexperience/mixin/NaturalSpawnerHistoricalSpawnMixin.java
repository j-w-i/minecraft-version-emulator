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
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalSpawnFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backports the 26.3 historical candidate-list filters, before any entity is created.
 *
 * <p>Two live paths, both resolving the world through the same {@link HistoricalSpawnFilter} code:
 *
 * <ul>
 *   <li>{@code mobsAt} is the runtime path and covers <b>biome and structure lists alike</b>:
 *       {@code ChunkGenerator.getMobsAt} returns a covering structure's own spawn override and only falls
 *       back to the biome's {@code MobSpawnSettings.getMobs(category)} otherwise. Injecting on the return
 *       therefore filters the Nether's structure lists - a 1.21.1 fortress returns blaze, zombified piglin,
 *       wither skeleton, skeleton and magma cube - with no second hook to disagree with this one. It routes
 *       through {@code applyForLevel} rather than {@code apply} because the <b>level</b> is authoritative
 *       for both halves of the resolution: it supplies the generator and the dimension, and the dimension
 *       carries its own rule (endermen do not spawn in the Nether before 16w20a).</li>
 *   <li>{@code spawnMobsForChunkGeneration} is the generation-time path, which asks the <b>biome's</b>
 *       settings directly and so never sees a structure override. It reaches the same resolution through
 *       {@code applyForLevel}, so a generation-time list cannot disagree with a runtime one about the
 *       dimension either.</li>
 * </ul>
 *
 * <p>{@link NaturalSpawnerProbeAccessor} invokes this same {@code mobsAt}, so a probe observation is an
 * observation of the deployed hook.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerHistoricalSpawnMixin {
    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void historicalExperience$filterNaturalSpawnList(
        ServerLevel level, StructureManager structures, ChunkGenerator generator,
        MobCategory category, BlockPos pos, Holder<Biome> biome,
        CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> callback
    ) {
        callback.setReturnValue(HistoricalSpawnFilter.applyForLevel(level, callback.getReturnValue()));
    }

    @Redirect(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/biome/MobSpawnSettings;getMobs("
            + "Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/util/random/WeightedRandomList;"))
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> historicalExperience$filterChunkSpawnList(
        MobSpawnSettings settings, MobCategory category,
        ServerLevelAccessor level, Holder<Biome> biome, ChunkPos chunkPos, RandomSource random
    ) {
        // FILTER ONLY. A historical spawner algorithm lived here as well for a while - it suppressed this
        // per-chunk creature pack for the pre-Beta-1.8 eras and replaced it with the era's own cycle. The owner
        // asked for the spawning ALGORITHM work to be removed while the filtering stays, so this is back to
        // narrowing the candidate list and nothing else. The algorithm and the jar evidence behind it are
        // preserved in research/pre-beta-1.8-spawning-evidence.json for whenever it is wanted again.
        return HistoricalSpawnFilter.applyForLevel(level.getLevel(), settings.getMobs(category));
    }
}

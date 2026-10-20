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
package net.historicalexperience.mixin.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code NaturalSpawner.mobsAt}, which is private, so a verification probe can call the
 * <b>injected</b> spawn path instead of re-implementing it.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@code HistoricalSpawnFilter.analyse} is pure and can be measured offline, but measuring it proves only
 * that the rule would remove a mob if something asked it to. The defect this accessor was added for was
 * precisely that nothing asked: the live hook resolved the world through the generator's own marker, which a
 * vanilla Nether generator does not carry, so the filter was skipped and no counter moved.
 *
 * <p>Calling this accessor runs the real chain - {@code NaturalSpawner.mobsAt} returns whatever the generator
 * produced for that position (a structure's spawn override when one covers it, the biome's category list
 * otherwise) and the {@code @Inject} at its return applies the historical rule. A probe that observes striders
 * or piglins absent from the returned list has therefore observed the deployed hook, not a copy of its logic.
 *
 * <p>Every argument is the live one: {@code structures}, {@code generator} and {@code biome} come from the
 * level being sampled, so the answer cannot come from ambient state the game itself would not use.
 *
 * <p>{@code @Invoker} on a <b>static</b> target requires a static interface method; the instance form is
 * rejected. Declared in the shared {@code mixins} list, not {@code client}, because natural spawning is a
 * server-side decision and the probe runs headless.
 */
@Mixin(net.minecraft.world.level.NaturalSpawner.class)
public interface NaturalSpawnerProbeAccessor {
    /**
     * The candidate list the game itself would choose from at this position, after the historical filter.
     *
     * <p>Exactly the parameters of {@code NaturalSpawnerHistoricalSpawnMixin}'s {@code @Inject}: the same
     * method, so the arguments the probe passes are the arguments the hook sees.
     */
    @Invoker("mobsAt")
    static WeightedRandomList<MobSpawnSettings.SpawnerData> invokeMobsAt(
        ServerLevel level,
        StructureManager structures,
        ChunkGenerator generator,
        MobCategory category,
        BlockPos pos,
        Holder<Biome> biome
    ) {
        throw new AssertionError("mixin-generated invoker was not applied");
    }
}

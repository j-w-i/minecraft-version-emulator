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
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates = settings.getMobs(category);

        // From Beta 1.8 the game fills every newly generated chunk with a biome-weighted animal pack, with no
        // global cap. That is the arrangement Beta 1.8 introduced, so for the eras that had the capped spawn
        // cycle instead it is anachronistic AND unbounded: the owner's screenshot showed a landscape carpeted
        // with animals, and this per-chunk source is the one that can produce hundreds beside a cycle capped at
        // fifteen. The eras with a cycle suppress it; every other version, Infdev included, is untouched.
        if (category == MobCategory.CREATURE
            && net.historicalexperience.gameplay.HistoricalAnimalSpawner
                .suppressesChunkGenerationSpawning(HistoricalSpawnFilter.profileIdForGenerator(
                    level.getLevel().getChunkSource().getGenerator()))) {
            return WeightedRandomList.create();
        }

        return HistoricalSpawnFilter.applyForLevel(level.getLevel(), candidates);
    }
}

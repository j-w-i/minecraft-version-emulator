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

/** Backports the 26.3 historical candidate-list filters, before any entity is created. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerHistoricalSpawnMixin {
    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void historicalExperience$filterNaturalSpawnList(
        ServerLevel level, StructureManager structures, ChunkGenerator generator,
        MobCategory category, BlockPos pos, Holder<Biome> biome,
        CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> callback
    ) {
        callback.setReturnValue(HistoricalSpawnFilter.apply(generator, callback.getReturnValue()));
    }

    @Redirect(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/biome/MobSpawnSettings;getMobs("
            + "Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/util/random/WeightedRandomList;"))
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> historicalExperience$filterChunkSpawnList(
        MobSpawnSettings settings, MobCategory category,
        ServerLevelAccessor level, Holder<Biome> biome, ChunkPos chunkPos, RandomSource random
    ) {
        return HistoricalSpawnFilter.applyForLevel(level.getLevel(), settings.getMobs(category));
    }
}

package net.historicalexperience.mixin;

import net.historicalexperience.generation.BetaOrePlacement;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places a historical revision's ore population after vanilla's biome decoration.
 *
 * <p>Why here and not through the biome: ore features belong to a biome's feature list, which is global
 * registry data, so they cannot be added for one world. Running the population for the chunk that is
 * being decorated is the narrow equivalent, and it is placed on the chunk that the vanilla method was
 * already given rather than on any ambient state.
 *
 * <p>Only the generator's own durable marker selects the population, so an ordinary world, a
 * non-historical Moderner Beta world and a world on a revision that places vanilla ores are all
 * untouched. The revision that enables this also excludes the vanilla ore features, so the two cannot
 * double up.
 *
 * <p>The config is {@code required}: if this hook failed to apply, a historical world would silently
 * fall back to vanilla ore placement, which is exactly the defect being fixed.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorHistoricalOreMixin {
    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void historicalExperience$placeHistoricalOres(
        WorldGenLevel level,
        ChunkAccess chunk,
        StructureManager structureManager,
        CallbackInfo ci
    ) {
        BetaOrePlacement.placeForChunk((ChunkGenerator) (Object) this, level, chunk);
    }
}

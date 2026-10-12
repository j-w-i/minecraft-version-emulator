package net.historicalexperience.mixin;

import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Refuses to place a feature that did not exist in the world's version, before it places anything.
 *
 * <h2>What this is</h2>
 *
 * <p>The single funnel for decoration: every placed feature - grass, flowers, ores, trees, everything -
 * passes through this method, so refusing here stops anachronistic content being generated at all rather
 * than being generated and then removed. That distinction is a requirement of this project, not a
 * preference: generating and removing would mean the chunk was briefly wrong and would leave traces in
 * anything that observed it.
 *
 * <h2>Why it differs from the 26.3 build it is backported from</h2>
 *
 * <p>26.3 split this method out of {@code PlacedFeature} into a new {@code FeaturePlacer} class that held
 * the level and generator as fields, so its mixin shadowed them. 1.21.1 predates that split: the method
 * still lives on {@code PlacedFeature} and takes both as parameters. The injection point, the cancellation
 * and the decision are unchanged - only where the level and generator come from differs, which is what makes
 * this a backport rather than a rewrite.
 */
@Mixin(PlacedFeature.class)
public abstract class FeaturePlacerHistoricalContentMixin {
    @Inject(method = "placeWithBiomeCheck", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$skipAnachronisticFeatures(
        WorldGenLevel level,
        ChunkGenerator generator,
        RandomSource random,
        BlockPos origin,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (HistoricalFeatureExclusions.shouldSkip(generator, level, (PlacedFeature) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}

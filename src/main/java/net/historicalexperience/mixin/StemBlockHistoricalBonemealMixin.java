package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.level.block.StemBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * One bone meal fully grows a melon or pumpkin stem, before Release 1.5. Ported from Nostalgic Tweaks
 * {@code StemBlockMixin} (LGPL-3.0); the boundary and its source are on
 * {@link CropBlockHistoricalBonemealMixin}.
 *
 * <p>The target is the single {@code Math.min(II)I} in {@code performBonemeal}, verified against the 1.21.1 bytecode:
 * it caps the new age at {@code MAX_AGE}, so replacing its result with {@code MAX_AGE} skips the random 2-5 step.
 */
@Mixin(StemBlock.class)
public abstract class StemBlockHistoricalBonemealMixin {
    @ModifyExpressionValue(
        method = "performBonemeal",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int historicalExperience$instantStemGrowth(int age) {
        return HistoricalGameplayTweaks.instantBonemeal() ? StemBlock.MAX_AGE : age;
    }
}

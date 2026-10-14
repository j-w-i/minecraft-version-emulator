package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * One bone meal fully grows cocoa, before Release 1.5. Ported from Nostalgic Tweaks {@code CocoaBlockMixin}
 * (LGPL-3.0); the boundary and its source are on {@link CropBlockHistoricalBonemealMixin}.
 *
 * <p>Cocoa was added in 1.3.1 (12w18a), so only the Release 1.3 and 1.4 profiles can ever reach this.
 */
@Mixin(CocoaBlock.class)
public abstract class CocoaBlockHistoricalBonemealMixin {
    @ModifyArg(
        method = "performBonemeal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
        index = 1)
    private BlockState historicalExperience$instantCocoaGrowth(BlockState state) {
        return HistoricalGameplayTweaks.instantBonemeal()
            ? state.setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE)
            : state;
    }
}

package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Restores Beta 1.8's regeneration: one slow branch, and no exhaustion cost.
 *
 * <p>From research/historical-features.json, entry {@code beta18-natural-regeneration-rule} (confidence: high):
 * regeneration needs food 18 or more, heals once per 80 ticks, and <b>costs no exhaustion</b>. Modern
 * {@code FoodData.tick} has an additional branch - food 20 or more, every 10 ticks, charging exhaustion - so a
 * modern player regenerates far faster than these versions did and pays saturation for it.
 *
 * <p>Nostalgic Tweaks has no equivalent, so this is implemented from the research. The two hooks are placed against
 * the bytecode rather than the source: the fast branch is gated on an {@code int 20} comparison, and the slow
 * branch charges exhaustion through its own call.
 *
 * <p>The gate is the hunger boundary, because a version with no hunger meter has no regeneration to model. The
 * entry does not date the arrival of the fast branch or the exhaustion cost, so every hunger-bearing profile here
 * is treated as Beta-era; that assumption is written down rather than implied.
 *
 * <p>A shared mixin: {@code FoodData} is server-side state.
 */
@Mixin(FoodData.class)
public abstract class FoodDataHistoricalRegenerationMixin {
    /** Upstream has no equivalent. Makes the fast branch unreachable; the maximum food level is 20. */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
    private int historicalExperience$skipFastRegeneration(int threshold) {
        return usesBetaRegeneration() ? 21 : threshold;
    }

    /**
     * Zeroes the regeneration exhaustion charge on the SLOW branch.
     *
     * <p>The owner class is {@code FoodData}, not {@code Player}: the calls inside {@code tick} resolve to
     * FoodData's own {@code addExhaustion}, and targeting {@code Player.addExhaustion} never matched - a fatal
     * error at transformation, which happens at player join. The ordinal is the second call in the method (offset
     * 227, the slow branch); the first (offset 169) belongs to the fast branch, which the constant hook above
     * already makes unreachable.
     */
    @ModifyArg(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V",
            ordinal = 1),
        index = 0)
    private float historicalExperience$noExhaustionFromRegeneration(float exhaustion) {
        return usesBetaRegeneration() ? 0.0F : exhaustion;
    }

    /** Whether the active version regenerates the Beta 1.8 way. */
    private static boolean usesBetaRegeneration() {
        return HistoricalGameplayTweaks.betaRegenerationModel(HistoricalActiveSession.current());
    }
}

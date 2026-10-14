package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalLightingTweaks;
import net.historicalexperience.client.lighting.HistoricalSmoothLighting;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Two shading rules the Beta 1.3-1.7.3 lighting engine had and Beta 1.8 replaced.
 *
 * <h2>Source</h2>
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/world_lighting/BlockBehaviourMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_SMOOTH_LIGHTING} for the corner and
 * {@code CandyTweak.OLD_LEAVES_LIGHTING} for leaves. Upstream's injection points and constants are kept exactly:
 *
 * <pre>
 * &#64;ModifyExpressionValue(method = "getShadeBrightness", at = &#64;At(value = "CONSTANT", args = "floatValue=0.2F"))
 * return OLD_SMOOTH_LIGHTING ? 0.0F : shadeBrightness;
 *
 * &#64;ModifyReturnValue(method = "getShadeBrightness", at = &#64;At("RETURN"))
 * if (blockState.getBlock() instanceof LeavesBlock) return OLD_LEAVES_LIGHTING ? 1.0F : shadeBrightness;
 * </pre>
 *
 * <p>So there are two separate differences from the modern engine: <b>the darkness of a corner</b>, where the modern
 * value darkens towards 0.2 and this engine did not darken at all, and <b>leaves</b>, which this engine drew fully
 * bright rather than shaded. Both were listed by the owner for implementation.
 *
 * <h2>Boundary</h2>
 *
 * <p>The wiki's Light history gives both ends of the window: <i>"1.3: Smooth lighting engine added with the help of
 * MrMessiah and his 'BetterLight' mod"</i> and <i>"1.8 Pre-release: Implemented a new lighting engine. The lighting on
 * a block is given a tint based on the most prominent source of light."</i> So this applies from Beta 1.3 up to but
 * excluding Beta 1.8 — the window where the MrMessiah engine was the game's engine.
 *
 * <p>The leaves rule is gated on that same window, which is how upstream pairs it, and that is stated rather than
 * assumed: the wiki does not date leaves shading separately, so the window of the engine it belonged to is the best
 * available evidence and is preferred to an invented boundary.
 *
 * <p>A shared mixin. {@code getShadeBrightness} is common code and only changes a number a renderer reads, and the
 * gate is inert on a server because no server sets a client's active version.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourHistoricalShadeBrightnessMixin {
    /**
     * Upstream: {@code nt_world_lighting$modifyShadeDarkness}.
     *
     * <p>{@code require = 1} is set because without it a failed constant match is <b>silent</b>: the mod loads,
     * corners stay dark, and nothing reports it. The constant {@code 0.2F} in {@code getShadeBrightness} is the only
     * one of its value in that method, so a failure to match means the target moved rather than that the search was
     * ambiguous - either way it is a real defect worth stopping the launch for.
     */
    @ModifyExpressionValue(
        method = "getShadeBrightness",
        at = @At(value = "CONSTANT", args = "floatValue=0.2F"),
        require = 1)
    private float historicalExperience$oldShadeDarkness(float shadeBrightness) {
        return HistoricalSmoothLighting.activeNow() ? 0.0F : shadeBrightness;
    }

    /** Upstream: {@code nt_world_lighting$modifyShadeBrightness}. Leaves were not shaded by this engine. */
    @ModifyReturnValue(method = "getShadeBrightness", at = @At("RETURN"), require = 1)
    private float historicalExperience$oldLeavesBrightness(float shadeBrightness, BlockState blockState) {
        if (blockState.getBlock() instanceof LeavesBlock && HistoricalLightingTweaks.oldLeavesLighting()) {
            return 1.0F;
        }
        return shadeBrightness;
    }
}

package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.client.lighting.HistoricalSmoothLighting;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Beta 1.3–1.7.3 smooth lighting engine's corner shading, which Beta 1.8 replaced.
 *
 * <h2>Source</h2>
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/world_lighting/BlockBehaviourMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_SMOOTH_LIGHTING}. Upstream's
 * injection point and its constant are kept exactly:
 *
 * <pre>
 * &#64;ModifyExpressionValue(method = "getShadeBrightness", at = &#64;At(value = "CONSTANT", args = "floatValue=0.2F"))
 * return OLD_SMOOTH_LIGHTING ? 0.0F : shadeBrightness;
 * </pre>
 *
 * <p>So the difference between the two engines is <b>the darkness of the shade in a corner</b>: the modern value
 * darkens a corner towards 0.2, and the 1.3–1.7.3 engine did not darken it at all. That is a small and specific
 * change, which is worth stating because "old smooth lighting" sounds like a whole lighting model and is not one -
 * the two engines differ here and in the lightmap, not in how light propagates.
 *
 * <h2>Boundary, and why it is a separate gate from the classic engine</h2>
 *
 * <p>The wiki's Light history gives both ends: <i>"1.3: Smooth lighting engine added with the help of MrMessiah and
 * his 'BetterLight' mod"</i> and <i>"1.8 Pre-release: Implemented a new lighting engine. The lighting on a block is
 * given a tint based on the most prominent source of light."</i> So this applies from Beta 1.3 up to but excluding
 * Beta 1.8 — the window where the MrMessiah engine was the game's engine.
 *
 * <p>That window is deliberately <b>not</b> the same as the classic engine's: {@link HistoricalSmoothLighting} is
 * true only inside it, and the classic engine is true only *before* it. The two are disjoint, so a version cannot
 * be given both engines, which is the property a single "old lighting" boolean would have got wrong.
 *
 * <p>A shared mixin. {@code getShadeBrightness} is common code and the classic engine's mixin is client-scoped; this
 * one only changes a number a renderer reads, so leaving it common would be harmless - but the gate makes it inert
 * on a server anyway, since no server sets a client's active version.
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

    // Upstream pairs this with a second rule for leaves (OLD_LEAVES_LIGHTING, forcing 1.0F). That one is
    // deliberately NOT ported: it is a different tweak with a different flag, the owner asked for "old smooth
    // lighting" and not for leaves lighting, and porting an unasked-for rule would change how leaves render in
    // versions where nothing was requested. Recorded here so the omission is visible rather than looking like an
    // oversight - and as a comment rather than an injection, because a handler that returns its argument unchanged
    // is dead code shaped like a safeguard.
}

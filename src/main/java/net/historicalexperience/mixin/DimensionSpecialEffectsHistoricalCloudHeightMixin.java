package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Old cloud height: clouds sat lower before Beta 1.8.
 *
 * <p>Ported from Nostalgic Tweaks {@code DimensionSpecialEffectsMixin} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md), which answers {@code CandyTweak.OLD_CLOUD_HEIGHT}.
 *
 * <h2>The height, and the boundary, both from the wiki rather than from upstream's label</h2>
 *
 * <p>Upstream's description says "The old heights are; alpha y-108, beta y-128, and modern y-192". The wiki dates
 * the change more precisely than that label does. minecraft.wiki, Cloud history:
 *
 * <ul>
 *   <li><i>"Added clouds, which float westward between layers 108 and 112"</i> — Classic 0.0.14a.</li>
 *   <li>Beta 1.8: <i>"Now clouds float between layers 128 and 132."</i></li>
 * </ul>
 *
 * <p>So the OLD height is 108, not "alpha" specifically: it runs from Classic 0.0.14a to Beta 1.7.3, which is every
 * declared version (the oldest is Infdev 20100420, well after Classic 0.0.14a). Beta 1.8.1 onwards is 128, so the
 * gate is simply "before Beta 1.8.1" and the label "alpha" in upstream's description is the same window minus the
 * Infdev and early-Beta builds that also had it.
 *
 * <p>The modern value is 192, which is what 1.21.1 already uses, so nothing needs overriding after the boundary.
 *
 * <h2>Why this hooks the renderer's cloud height</h2>
 *
 * <p>{@code DimensionSpecialEffects.getCloudHeight()} is the one place the renderer asks how high clouds are, so a
 * single return-value hook moves them for every dimension at once. The declared versions have only two dimensions
 * between them in any case - the Nether arrived in Alpha 1.2.0 - and both used the same cloud height.
 */
@Mixin(DimensionSpecialEffects.class)
public abstract class DimensionSpecialEffectsHistoricalCloudHeightMixin {
    /** Clouds floated between layers 108 and 112 before Beta 1.8. */
    private static final float OLD_CLOUD_HEIGHT = 108.0F;

    @ModifyReturnValue(method = "getCloudHeight", at = @At("RETURN"), require = 1)
    private float historicalExperience$oldCloudHeight(float cloudHeight) {
        return HistoricalWorldTweaks.oldCloudHeight() ? OLD_CLOUD_HEIGHT : cloudHeight;
    }
}

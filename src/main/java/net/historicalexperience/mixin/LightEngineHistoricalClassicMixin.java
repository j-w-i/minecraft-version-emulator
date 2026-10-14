package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Old lighting for the vanilla renderer: the value the OLD engine would have stored at a position.
 *
 * <p>Ported from Nostalgic Tweaks {@code LightEngineMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). This used to carry
 * only the Classic engine; it now delegates to {@link HistoricalOldLighting#transform}, which also handles the
 * round-robin engine, so the two cannot disagree about which applies.
 *
 * <p>Sodium does not call this method - see {@code HistoricalDataLayer} and {@code
 * LayerLightSectionStorageHistoricalMixin} for the path it does read.
 *
 * <p>A client-only mixin, listed in the {@code client} section of the config: naming a client class in the common
 * section crashes a dedicated server at boot.
 *
 * <p>{@code require = 1} on purpose: without it a wrong target is a silent no-op and lighting quietly stays modern.
 */
@Mixin(LightEngine.class)
public abstract class LightEngineHistoricalClassicMixin {
    @Shadow
    protected net.minecraft.world.level.chunk.LightChunkGetter chunkSource;

    @ModifyReturnValue(method = "getLightValue", at = @At("RETURN"), require = 1)
    private int historicalExperience$classicLightValue(int lightValue, BlockPos blockPos) {
        if (HistoricalOldLighting.mode() == HistoricalOldLighting.Mode.NONE) {
            return lightValue;
        }
        // The integrated SERVER also owns a light engine over a different chunk source, and its values drive mob
        // spawning. Only the client's own engine is old-lit.
        if (!(this.chunkSource instanceof ClientChunkCache)) {
            return lightValue;
        }
        if (!(this.chunkSource.getLevel() instanceof ClientLevel level)) {
            return lightValue;
        }
        LightLayer layer = ((Object) this) instanceof SkyLightEngine ? LightLayer.SKY : LightLayer.BLOCK;
        return HistoricalOldLighting.transform(level, layer, blockPos, lightValue);
    }
}

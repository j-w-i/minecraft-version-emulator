package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalDataLayer;
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Old lighting for Sodium: wraps the light array the renderer copies out of the engine.
 *
 * <p>Ported from Nostalgic Tweaks {@code LayerLightSectionStorageMixin} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 * Sodium reads whole {@link DataLayer}s rather than calling {@code getLightValue}, so the
 * {@code LightEngineHistoricalClassicMixin} hook alone is invisible to it. Hooking the storage rather than the engine
 * also covers Flywheel, which upstream notes.
 *
 * <p>The wrapper is returned ONLY on the client's own chunk source. The integrated server's light engine feeds mob
 * spawning and plant growth, and must keep its real values.
 */
@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageHistoricalMixin {
    @Shadow
    @Final
    protected LightChunkGetter chunkSource;

    @ModifyReturnValue(method = "getDataLayerData", at = @At("RETURN"), require = 1)
    private DataLayer historicalExperience$wrapForOldLighting(DataLayer original, long sectionPos) {
        if (original == null || HistoricalOldLighting.mode() == HistoricalOldLighting.Mode.NONE) {
            return original;
        }
        if (!(this.chunkSource instanceof ClientChunkCache) || !(this.chunkSource.getLevel() instanceof ClientLevel)) {
            return original;
        }
        LightLayer layer = ((Object) this) instanceof SkyLightSectionStorage ? LightLayer.SKY : LightLayer.BLOCK;
        return new HistoricalDataLayer(original, layer, sectionPos);
    }
}

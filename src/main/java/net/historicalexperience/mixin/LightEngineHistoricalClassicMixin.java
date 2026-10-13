package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalLightingEngine;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;


/**
 * Applies the old Classic lighting engine to the CLIENT light engine only.
 *
 * <p>Ported from Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/world_lighting/LightEngineMixin.java} and
 * {@code helper/candy/light/NostalgicDataLayer.java} ({@code getLightValue}). LGPL-3.0; attribution is in
 * THIRD_PARTY_NOTICES.md.
 *
 * <h2>The guards, and why each one is here</h2>
 *
 * <ul>
 *   <li><b>Client light engines only.</b> Upstream tests {@code ClassUtil.isNotInstanceOf(this.chunkSource,
 *       ClientChunkCache.class)} and returns the vanilla value, and separately returns vanilla when running on the
 *       integrated server thread. Requiring a {@link ClientChunkCache} achieves both: the integrated server's own
 *       engines are backed by a {@code ServerChunkCache}, so the world's authoritative light is never touched.
 *       This matters beyond tidiness — rewriting server light would change mob spawning, which is light-driven.</li>
 *   <li><b>Version gate plus toggle.</b> {@link HistoricalLightingEngine#activeNow()} answers false for an unknown
 *       version and for every build from Beta 1.3 onward, so nothing changes where the smooth engine is the real
 *       one.</li>
 *   <li><b>{@code getLightValue} takes a position.</b> The sky rule has to look upward from the queried position,
 *       which is only possible because the upstream target method carries it.</li>
 * </ul>
 *
 * <p>{@code require = 1} is set even though this is a client-render mixin. Without it a failed injection is
 * <b>silent</b>: the mod loads, lighting quietly stays modern, and nothing anywhere reports it. The target
 * {@code LightEngine.getLightValue} is common code and exists on both sides in 1.21.1, so a failure to apply is a
 * real defect and should stop the launch rather than be discovered by looking at the screen.
 */
@Mixin(LightEngine.class)
public abstract class LightEngineHistoricalClassicMixin {
    // Shadowed WITHOUT @Final: Mixin rejects a final shadow here with an opaque "Attach error" at transform
    // time, and nothing about this hook needs the field to be treated as final.
    @Shadow
    protected net.minecraft.world.level.chunk.LightChunkGetter chunkSource;

    @ModifyReturnValue(method = "getLightValue", at = @At("RETURN"), require = 1)
    private int historicalExperience$classicLightValue(int lightValue, BlockPos blockPos) {
        if (!(this.chunkSource instanceof ClientChunkCache)) {
            return lightValue;
        }
        if (!HistoricalLightingEngine.activeNow()) {
            return lightValue;
        }
        if (!(this.chunkSource.getLevel() instanceof ClientLevel level)) {
            return lightValue;
        }

        LightLayer layer = ((Object) this) instanceof SkyLightEngine ? LightLayer.SKY : LightLayer.BLOCK;
        if (layer == LightLayer.BLOCK) {
            return HistoricalLightingEngine.classicBlockLight();
        }

        return HistoricalLightingEngine.classicSkyLight(
            lightValue,
            level.getMaxLightLevel(),
            level.dimensionType().hasSkyLight(),
            obstructedAbove(level, blockPos, level.getMaxLightLevel()));
    }

    /**
     * Whether water or a fully light-blocking block stands between this position and the build limit.
     *
     * <p>This is the upward scan inside upstream's {@code getClassicLight}, kept in the same order and with the
     * same two conditions: {@code Blocks.WATER}, and a {@code getLightBlock} at or above the maximum light level.
     */
    private static boolean obstructedAbove(BlockGetter level, BlockPos blockPos, int maxLightLevel) {
        BlockPos.MutableBlockPos cursor = blockPos.above().mutable();
        while (cursor.getY() < level.getMaxBuildHeight()) {
            BlockState state = level.getBlockState(cursor);
            if (state.is(Blocks.WATER) || state.getLightBlock(level, cursor) >= maxLightLevel) {
                return true;
            }
            cursor.move(0, 1, 0);
        }
        return false;
    }
}

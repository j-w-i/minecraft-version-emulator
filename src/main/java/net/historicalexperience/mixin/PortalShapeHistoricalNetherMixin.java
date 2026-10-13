package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops a nether portal being lit in versions that had no Nether.
 *
 * <p>The owner asked: "I assume you've already blocked nether portals until alpha 1.2". It was not blocked. The block
 * veto runs on the generation path, so it prevents netherrack, glowstone and portal blocks from being GENERATED, and
 * does nothing about a player building obsidian and lighting it - which worked in an Alpha 1.0.1 world.
 *
 * <h2>Why this is the right single hook</h2>
 *
 * <p>{@code PortalShape.findEmptyPortalShape} is where EVERY activation path asks "is there a valid portal frame
 * here": flint and steel through {@code FlintAndSteelItem.useOn}, and fire through {@code BaseFireBlock.onPlace}.
 * Returning empty means the frame is never completed by any of them, rather than patching each item in turn and
 * missing the next one - and fire spread, dispensers and any future path are covered by construction.
 *
 * <p>A shared mixin: the client predicts the same action, and a client that lit a portal the server refused would
 * show a frame the world does not contain.
 */
@Mixin(PortalShape.class)
public abstract class PortalShapeHistoricalNetherMixin {
    /** Vanilla: {@code findEmptyPortalShape(LevelAccessor, BlockPos, Direction.Axis)}. */
    @Inject(method = "findEmptyPortalShape", at = @At("HEAD"), cancellable = true)
    private static void historicalExperience$refusePortalFrame(LevelAccessor level, BlockPos pos, Direction.Axis axis,
            CallbackInfoReturnable<java.util.Optional<PortalShape>> callback) {
        if (!HistoricalGameplayTweaks.netherAvailable(HistoricalActiveSession.current())) {
            callback.setReturnValue(java.util.Optional.empty());
        }
    }
}

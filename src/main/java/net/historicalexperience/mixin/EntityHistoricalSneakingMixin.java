package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses the sneak key in versions that had no sneaking.
 *
 * <p>Reported by the owner after playing an Alpha 1.0.1 world: "i can also still sneak way before sneaking was
 * added" - and sneaking arrives in <b>Alpha 1.1.0</b>. No gate existed for it anywhere, so this is a newly
 * implemented behaviour rather than a corrected one.
 *
 * <p>The hook is the shift-key flag itself rather than the key binding. Cancelling the flag covers every
 * consequence at once: the crouch pose, the reduced movement speed, the reduced height, and the client's own
 * prediction, because the client sets the same flag locally. A hook on the key binding would have left the server
 * free to accept a crouch from a client that sent one, and would have missed anything that sets the flag without a
 * key press.
 *
 * <p>A shared mixin: the flag exists on both sides, and both must refuse it or one will disagree with the other.
 */
@Mixin(Entity.class)
public abstract class EntityHistoricalSneakingMixin {
    /** Vanilla: {@code setShiftKeyDown(boolean)}. */
    @Inject(method = "setShiftKeyDown(Z)V", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseSneaking(boolean sneaking, CallbackInfo callback) {
        if (!HistoricalGameplayTweaks.sneakingAvailable(HistoricalActiveSession.current())) {
            // Not even a release of the key, so the flag can never be left set by a version that had no crouch.
            callback.cancel();
        }
    }
}

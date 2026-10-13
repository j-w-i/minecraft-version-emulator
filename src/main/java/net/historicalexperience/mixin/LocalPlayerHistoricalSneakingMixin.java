package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the CLIENT crouching in versions that had no sneaking.
 *
 * <h2>Why the first attempt did nothing</h2>
 *
 * <p>{@code EntityHistoricalSneakingMixin} cancels {@code Entity.setShiftKeyDown} and works for the server, but the
 * client never calls it for crouching: {@code LocalPlayer} has its own private {@code crouching} field, its own
 * {@code isShiftKeyDown()} and {@code isCrouching()} overrides, and that field is written in {@code aiStep}. So the
 * owner's report - "i can still sneak" - survived a hook that was doing its job on the other side of the connection.
 *
 * <p>The refusal is placed on the two READERS rather than on the field write, because everything downstream asks one
 * of them: the crouch pose, the reduced movement speed, the reduced height, and the eye height all consult
 * {@code isCrouching()} or {@code isShiftKeyDown()}. Refusing both covers every consequence in one place, and cannot
 * be bypassed by a path that writes the field directly, which the field-write hook could.
 *
 * <p>Client-only, and that is correct rather than a compromise: the server's copy is refused separately by the
 * Entity hook, so neither side believes the player is crouching.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerHistoricalSneakingMixin {
    @Inject(method = "isCrouching", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseCrouchState(CallbackInfoReturnable<Boolean> callback) {
        if (!HistoricalGameplayTweaks.sneakingAvailable(HistoricalActiveSession.current())) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseShiftState(CallbackInfoReturnable<Boolean> callback) {
        if (!HistoricalGameplayTweaks.sneakingAvailable(HistoricalActiveSession.current())) {
            callback.setReturnValue(false);
        }
    }
}

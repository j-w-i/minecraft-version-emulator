package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops an eye of ender completing a portal in versions where the portal did nothing.
 *
 * <p>Beta 1.9 Prerelease 3 adds the stronghold portal room, the eye of ender and the frame and portal assets, and
 * the portal block has no function: the research records that frame repair has "no further result". Prerelease 4
 * activates it.
 *
 * <p>The hook is the ITEM, not the frame block: in 1.21.1 the frame has no use method at all - placing an eye is
 * handled by {@code EnderEyeItem.useOn}. Hooking the block would have produced a mixin that never fires.
 *
 * <p>{@code PASS} rather than {@code FAIL}: it is what vanilla returns when an item simply has nothing to do here,
 * so the eye is not consumed, no sound plays, and the frame stays empty. Returning FAIL would tell the client the
 * interaction was rejected, which is a different message to the player than "this does nothing yet".
 *
 * <p>The version comes from the active session rather than the level, so a portal in any dimension of a historical
 * world is covered.
 *
 * <p>A shared mixin.
 */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeHistoricalPortalMixin {
    /** Vanilla: {@code useOn(UseOnContext)} places the eye into a frame and completes the portal. */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refusePortalCompletion(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (!HistoricalGameplayTweaks.endPortalFunctional(HistoricalActiveSession.current())) {
            callback.setReturnValue(InteractionResult.PASS);
        }
    }
}

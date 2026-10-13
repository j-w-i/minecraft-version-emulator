package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalWeather;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the sky clear in every world older than Beta 1.5.
 *
 * <p>Two injections, because weather can start in two ways and blocking one is not enough:
 * <ul>
 *   <li>{@code advanceWeatherCycle} is the natural cycle, which sets the rain and thunder timers itself without
 *       going through the setter. Cancelling only the setter would leave natural rain running.</li>
 *   <li>{@code setWeatherParameters} is every deliberate change, including {@code /weather} and the vanilla level
 *       initialisation that seeds the timers. Cancelling only the cycle would let the command start a storm in a
 *       world whose era had none.</li>
 * </ul>
 *
 * <p>When the cycle is cancelled the level is also forced clear, because a world that already held rain — saved
 * before this rule existed, or upgraded from a build that did have weather — would otherwise keep raining forever
 * with no cycle left to end it. The clearing uses the protected level setters rather than
 * {@code setWeatherParameters}, precisely because this class cancels that method: routing the clear through it
 * would cancel the clear too.
 *
 * <p>The mixin is <b>common</b>, not client, and that is deliberate: the server owns weather and sends it to
 * clients, so enforcing it here means a client cannot disagree. It also means the dedicated-server probe loads
 * this class and therefore genuinely verifies the injection applies — unlike a client-only mixin, which a server
 * run never touches. {@code require = 1} makes a future mapping change fail loudly instead of silently leaving
 * the rain on.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelHistoricalWeatherMixin {
    // No @Shadow here on purpose. setRainLevel/setThunderLevel are declared in Level, not ServerLevel, and Mixin's
    // shadow-METHOD resolution does not walk superclasses - declaring them produced "was not located in the target
    // class net.minecraft.server.level.ServerLevel" and failed the whole mixin. They are public on Level, so they
    // are simply called on the level reference, which needs no shadow at all.

    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$noNaturalWeatherBeforeBeta1_5(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!HistoricalWeather.mustStayClear(level)) {
            return;
        }
        clearAndHold(level);
        ci.cancel();
    }

    @Inject(method = "setWeatherParameters", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$refuseWeatherChangesBeforeBeta1_5(
            int clearTime, int rainTime, boolean raining, boolean thundering, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!HistoricalWeather.mustStayClear(level)) {
            return;
        }
        clearAndHold(level);
        ci.cancel();
    }

    private void clearAndHold(ServerLevel level) {
        // Only touch the level when it is not already clear, so a clear world costs two float comparisons per tick
        // rather than a write that would dirty the level and re-send weather packets every tick.
        if (level.isRaining()) {
            level.setRainLevel(0.0F);
        }
        if (level.isThundering()) {
            level.setThunderLevel(0.0F);
        }
    }
}
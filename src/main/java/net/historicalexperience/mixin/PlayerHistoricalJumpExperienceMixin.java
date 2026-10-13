package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Beta 1.9 Prerelease 1's debug jump experience: every jump paid out a random 1-100 experience.
 *
 * <h2>Source</h2>
 *
 * <p>{@code research/minecraft-version-history.md} states it three times for the same build - item 6, "An
 * erroneous jump event grants 1-100 XP; auto-repeat jump was removed"; item 13, "XP from jumping and disabled
 * auto-repeat; Pre2 removes the XP bug and restores held-jump auto-repeat"; and item 11 of Prerelease 2, "XP-on-jump
 * debug bug removed; held-jump auto-repeat restored". {@code research/historical-features.json} records it
 * independently as "Experience gained from jumping as a test". {@code research/historical-bugs.json} carries it as
 * {@code beta-1-9-pre-1-jump-experience}: boundary kind <b>exact-build</b>, confidence high. The owner named this bug
 * directly.
 *
 * <h2>Why {@code jumpFromGround} and not the input packet</h2>
 *
 * <p>It is the method that performs the jump - {@code PlayerHistoricalExhaustionMixin} already hooks it for the
 * era's exhaustion cost - and it is entered once per jump including a held-down jump, so it reproduces the bug
 * without modelling input at all. The other half of the same research, the removal of continuous jumping, needs no
 * code: it describes this method being reached on every tick the key is held, which is what vanilla does and what
 * the check-out-of-the-way half of the bug was.
 *
 * <h2>Server only, and why that is not a style choice</h2>
 *
 * <p>{@code HistoricalActiveSession} is set on the client as well as the server, so without the
 * {@link ServerPlayer} test this would also run locally and call {@code giveExperiencePoints} on a client-side
 * player, which fights the server's authoritative value instead of predicting it. The server owns the grant; the
 * client is told.
 *
 * <p>The source of the draw is worth stating precisely because the first version of this comment got it wrong:
 * {@code Entity.getRandom()} is not a per-entity generator, it returns {@code level().getRandom()} - the level's
 * one shared source. {@code jumpFromGround} runs on the server thread, so that is not a race here, but nothing in
 * this file should be read later as a claim that the level's random is safe to reach from anywhere.
 *
 * <p>A shared mixin, and it has to be: {@code Player} is a common class.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalJumpExperienceMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void historicalExperience$jumpExperience(CallbackInfo info) {
        if (!((Object) this instanceof ServerPlayer serverPlayer)) {
            return;
        }
        int experience = HistoricalGameplayTweaks.jumpExperienceBug(HistoricalActiveSession.current(),
            serverPlayer.getRandom());
        // 0 means the gate did not apply; the caller is the only place that can tell a legitimate zero from an
        // inapplicable gate, so it acts on the amount rather than re-reading the gate.

        if (experience > 0) {
            serverPlayer.giveExperiencePoints(experience);
        }
    }
}

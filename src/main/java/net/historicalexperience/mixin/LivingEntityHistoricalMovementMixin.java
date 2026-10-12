package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Stops players sprinting - and, before 1.13, swimming - in versions that had neither.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mechanics_player/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_SPRINT} and
 * {@code GameplayTweak.DISABLE_SWIM}. The injection point, the argument modification, the creative and
 * spectator exemptions and the underwater branch are upstream's; only the two conditions changed, from global
 * config flags to this world's version.
 *
 * <h2>What my first version got wrong, three ways</h2>
 *
 * <p>It cancelled {@code setSprinting} outright. Upstream modifies the ARGUMENT with
 * {@code @ModifyVariable(argsOnly = true)} instead, which lets the method run with sprinting false - so the
 * rest of what the method does still happens. Cancelling skips all of it.
 *
 * <p>It blocked everyone. Upstream explicitly lets CREATIVE and SPECTATOR players sprint regardless, which is a
 * deliberate exemption and not an oversight.
 *
 * <p>It ignored swimming entirely, even though upward of a third of upstream's method is about it, and the two
 * flags share one carrier file. {@code DISABLE_SWIM} is 1.13, later than every profile this project declares,
 * so it is off everywhere - as the new gate says rather than as an assumption.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalMovementMixin {
    @ModifyVariable(argsOnly = true, method = "setSprinting", at = @At("HEAD"))
    private boolean historicalExperience$modifySprintingFlag(boolean sprinting) {
        String session = HistoricalActiveSession.current();
        boolean canSprint = HistoricalPlayerRules.allowsSprinting(session);
        boolean canSwim = !HistoricalGameplayTweaks.appliesTo("swim", session);

        if (canSprint && canSwim) {
            return sprinting;
        }

        if ((Object) this instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return sprinting;
            }

            if (player.isUnderWater()) {
                return canSwim;
            }

            if (!canSprint) {
                return false;
            }
        }

        return sprinting;
    }
}

package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops monsters fleeing the sun, in versions whose monsters did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_ai/FleeSunGoalMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_MONSTER_AVOID_SUN}. The
 * injection point and its shape are upstream's: {@code @ModifyReturnValue} on {@code canUse}, so the goal
 * simply never becomes usable rather than being cancelled mid-flight. Only the condition changed, from a global
 * flag to this world's version.
 */
@Mixin(FleeSunGoal.class)
public abstract class FleeSunGoalHistoricalMixin {
    /** Upstream: {@code nt_mob_ai$canFleeSun}. */
    @ModifyReturnValue(method = "canUse", at = @At("RETURN"))
    private boolean historicalExperience$canFleeSun(boolean canFleeSun) {
        if (HistoricalGameplayTweaks.appliesTo("avoidSun", HistoricalActiveSession.current())) {
            return false;
        }
        return canFleeSun;
    }
}

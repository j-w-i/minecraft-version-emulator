package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalPlayerRules;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops the Hunger effect applying, in versions that had no hunger for it to drain.
 *
 * <p>DIRECT PORT of part of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.PREVENT_HUNGER_EFFECT}. The target
 * and the {@code @ModifyReturnValue} shape are upstream's.
 *
 * <h2>Why it was set aside, and why that was wrong</h2>
 *
 * <p>I read this file in the hunger round and concluded the remaining injections were out of scope, because a
 * search of the port worklist found none of the flags. The worklist is a 200-row subset of the research; the join
 * against the research itself shows this row is <b>confidence: high</b> and names the tweak under the hunger
 * feature. "Not in the worklist" has now produced four wrong conclusions.
 *
 * <p>The gate is the hunger rule itself rather than a new boundary: an effect that drains a meter cannot matter in
 * a version that has no meter, and stating that once is how the two stay in agreement.
 *
 * <p>This is a shared mixin - {@code LivingEntity} exists on both sides, and the effect is applied server-side.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalHungerEffectMixin {
    /** Upstream: {@code nt_food_health$shouldAddHungerEffect}. */
    @ModifyReturnValue(method = "canBeAffected", at = @At("RETURN"))
    private boolean historicalExperience$preventHungerEffect(boolean canBeAffected,
            MobEffectInstance effectInstance) {
        String session = HistoricalActiveSession.current();

        if (effectInstance.getEffect() == MobEffects.HUNGER
            && session != null
            && HistoricalPlayerRules.suppressesHungerTick(session)) {
            return false;
        }

        return canBeAffected;
    }
}

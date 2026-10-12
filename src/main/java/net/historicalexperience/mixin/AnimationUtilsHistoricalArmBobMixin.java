package net.historicalexperience.mixin;

import net.historicalexperience.client.render.MobArmHelper;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a zombie's outstretched arms bobbing while it is idle.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/animation/mob_arms/AnimationUtilsMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The injection point - immediately before the {@code bobArms} call inside
 * {@code animateZombieArms} - and the call into {@code MobArmHelper} are upstream's.
 *
 * <h2>The second half of one tweak, found by reading to the end of the module</h2>
 *
 * <p>Round 62 ported the arm pose from {@code AbstractZombieModelMixin} and this file makes the pose hold still.
 * Upstream splits them because they hook different classes, not because they are separate features: with only the
 * first, a zombie stands with its arms in the right place and still waves them, which reads as nearly right - the
 * kind of near-miss that is harder to notice than an obvious fault. Reading the whole {@code mob_arms} directory
 * rather than the one file named after the flag is what surfaced it.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(AnimationUtils.class)
public abstract class AnimationUtilsHistoricalArmBobMixin {
    /** Upstream: {@code nt_mob_arms$onBobZombieArms}. */
    @Inject(
        method = "animateZombieArms",
        at = @At(
            shift = At.Shift.BEFORE,
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/AnimationUtils;bobArms("
                + "Lnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/model/geom/ModelPart;F)V"))
    private static void historicalExperience$onBobZombieArms(ModelPart leftArm, ModelPart rightArm,
            boolean isAggressive, float attackTime, float ageInTicks, CallbackInfo callback) {
        if (HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            MobArmHelper.applyStaticArms(rightArm, leftArm);
        }
    }
}

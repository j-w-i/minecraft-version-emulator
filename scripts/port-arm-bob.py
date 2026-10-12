"""Complete OLD_ZOMBIE_ARMS: suppress the idle arm bob as well.

Upstream's zombie-arms tweak is two files, not one. AbstractZombieModelMixin (ported in round 62) stops the arms
rising and swinging; AnimationUtilsMixin stops them BOBBING while idle, by calling MobArmHelper.applyStaticArms
before bobArms inside animateZombieArms. Without that second piece the arms are in the right pose but still move,
which is not the stance anyone remembers.

MobArmHelper is 26 lines and is a direct copy: both arms get xRot = -1.57F (straight out, -90 degrees), and no yaw
or roll.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

(root / 'src/main/java/net/historicalexperience/client/render/MobArmHelper.java').write_text(
'''package net.historicalexperience.client.render;

import net.minecraft.client.model.geom.ModelPart;

/**
 * Holds a mob's arms straight out.
 *
 * <p>DIRECT COPY of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/animation/MobArmHelper.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The three values per arm are upstream's.
 *
 * <p>-1.57 radians is -90 degrees about the x axis, which is arms out in front. It is written as the raw float
 * rather than as a computed constant for the same reason the sword block's pose values were copied: it is the
 * number that produces the remembered stance, and re-deriving it would only produce a different one.
 */
public final class MobArmHelper {
    private MobArmHelper() {
    }

    /** Upstream: {@code applyStaticArms}. */
    public static void applyStaticArms(ModelPart rightArm, ModelPart leftArm) {
        leftArm.xRot = -1.57F;
        leftArm.yRot = 0.0F;
        leftArm.zRot = 0.0F;

        rightArm.xRot = -1.57F;
        rightArm.yRot = 0.0F;
        rightArm.zRot = 0.0F;
    }
}
''', encoding='utf-8')
print('wrote MobArmHelper')

(root / 'src/main/java/net/historicalexperience/mixin/AnimationUtilsHistoricalArmBobMixin.java').write_text(
'''package net.historicalexperience.mixin;

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
''', encoding='utf-8')
print('wrote the arm bob mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document.setdefault('client', [])
if 'AnimationUtilsHistoricalArmBobMixin' not in document['client']:
    document['client'].append('AnimationUtilsHistoricalArmBobMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; client mixins =', len(document['client']))

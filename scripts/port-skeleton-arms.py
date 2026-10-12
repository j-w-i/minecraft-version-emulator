"""Port OLD_SKELETON_ARMS from its carrier, which was read in full while doing the zombie half.

Carrier: animation/mob_arms/SkeletonModelMixin (64 lines), two injections on Mob.isAggressive():
  prepareMobModel(Mob, F, F, F)      -> false, so no bow-drawing pose is prepared
  setupAnim(Mob, F, F, F, F, F)      -> apply static arms, bob them with ageInTicks, then false

The same research boundary covers both halves of this module: zombie and skeleton arm swinging were removed in
Indev 20100202, and every declared profile is later, so the mobArms gate serves both.

Note the class extends HumanoidModel so the inherited rightArm/leftArm parts are reachable, exactly as upstream
does; the fake constructor exists to satisfy the superclass, not to be called.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

MIXIN = '''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.historicalexperience.client.render.MobArmHelper;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a skeleton raising its arms to draw a bow.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/animation/mob_arms/SkeletonModelMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code AnimationTweak.OLD_SKELETON_ARMS}. Both injections,
 * their targets and the call into {@code MobArmHelper} are upstream's.
 *
 * <h2>Why it extends HumanoidModel</h2>
 *
 * <p>The arms are inherited {@code ModelPart}s, so reaching them means being a {@code HumanoidModel} - upstream
 * does the same, and the private constructor exists only because the compiler requires a superclass call. It is
 * never invoked; the mixin is woven into {@code SkeletonModel}.
 *
 * <h2>Evidence</h2>
 *
 * <p>The research records that arm swinging attack animations were removed in Indev 20100202, earlier than every
 * declared profile, so this reuses the {@code mobArms} gate rather than inventing a second boundary for the same
 * fact. Upstream splits the zombie and skeleton halves across two files because they hook different classes; the
 * enabling version is the same one.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(SkeletonModel.class)
public abstract class SkeletonModelHistoricalArmsMixin<T extends Mob> extends HumanoidModel<T> {
    private SkeletonModelHistoricalArmsMixin(ModelPart root) {
        super(root);
    }

    /** Upstream: {@code nt_mob_arms$modifyArmModel}. */
    @ModifyExpressionValue(
        method = "prepareMobModel(Lnet/minecraft/world/entity/Mob;FFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;isAggressive()Z"))
    private boolean historicalExperience$modifyArmModel(boolean isAggressive) {
        if (HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            return false;
        }
        return isAggressive;
    }

    /** Upstream: {@code nt_mob_arms$modifyArmSetup}. */
    @ModifyExpressionValue(
        method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;isAggressive()Z"))
    private boolean historicalExperience$modifyArmSetup(boolean isAggressive, T entity, float limbSwing,
            float limbSwingAmount, float ageInTicks) {
        if (!HistoricalGameplayTweaks.appliesTo("mobArms", HistoricalActiveSession.current())) {
            return isAggressive;
        }

        MobArmHelper.applyStaticArms(this.rightArm, this.leftArm);
        AnimationUtils.bobArms(this.rightArm, this.leftArm, ageInTicks);

        return false;
    }
}
'''
(root / 'src/main/java/net/historicalexperience/mixin/SkeletonModelHistoricalArmsMixin.java').write_text(
    MIXIN, encoding='utf-8')
print('wrote the skeleton arms mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document.setdefault('client', [])
if 'SkeletonModelHistoricalArmsMixin' not in document['client']:
    document['client'].append('SkeletonModelHistoricalArmsMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; client mixins =', len(document['client']))

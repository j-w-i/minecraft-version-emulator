package net.historicalexperience.client.render;

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

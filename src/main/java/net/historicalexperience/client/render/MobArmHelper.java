/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
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

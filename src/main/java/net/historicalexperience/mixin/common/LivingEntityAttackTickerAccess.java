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
package net.historicalexperience.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code LivingEntity.attackStrengthTicker}.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks' {@code LivingEntityAccess} usage in
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/PlayerMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which calls {@code nt$setAttackStrengthTicker} from the attack
 * hook to disable the cooldown.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackTickerAccess {
    @Accessor("attackStrengthTicker")
    void setAttackStrengthTicker(int value);
}

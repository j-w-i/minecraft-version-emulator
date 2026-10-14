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
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Rolls Beta 1.8's random critical hit instead of the flat +50%.
 *
 * <p>From Nostalgic Tweaks' subject matter - it has no critical hit FORMULA tweak, only DISABLE_CRITICAL_HIT, which
 * is ported separately - so this comes from research/historical-features.json, entry
 * {@code beta18-combat-critical-hits} (confidence: medium), which records the Beta 1.8 formula as
 * <b>"random +0%..+100% of weapon damage, plus 2 HP"</b> against a modern {@code criticalHitMultiplierModern} of
 * 1.5 that it describes as "explicitly not the Beta 1.8 behaviour".
 *
 * <h2>Where the constant is, and why that settles half the question</h2>
 *
 * <p>In 1.21.1's {@code Player.attack} the multiplier is {@code ldc 1.5f; fmul} and the enchantment bonus is added
 * immediately after with {@code fadd}. So modern applies the crit BEFORE enchantments - which is the 1.6/1.7-era
 * formula, not the Beta 1.8 one. That means Release 1.6.4 already matches the research and needs nothing, and the
 * five profiles before it need this.
 *
 * <h2>What is not here</h2>
 *
 * <p>The formula's <b>+2 HP</b> and its "after enchantments" ordering are not implemented. The +2 belongs at a
 * second instruction, and reaching the critical hit flag there needs a local ordinal - a guess, and this project
 * has already paid for two of those. The ordering only diverges when the weapon carries Sharpness. Both are
 * recorded as gaps rather than approximated silently.
 *
 * <p>A shared mixin: {@code Player.attack} runs on both sides, and the multiplier is read where the damage is
 * computed.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalCriticalHitMixin {
    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F))
    private float historicalExperience$randomCriticalHit(float multiplier) {
        if (HistoricalGameplayTweaks.betaCriticalHitFormula(HistoricalActiveSession.current())) {
            Player self = (Player) (Object) this;
            // 1..2, NOT 0..1. The formula is a bonus on top of the weapon's damage - "+0% to +100% of weapon
            // damage" - so a critical hit can never land for less than an ordinary one. Returning 0..1 made a
            // crit multiply the hit toward nothing, which the owner reported as damage that never happened.
            return Mth.nextFloat(self.getRandom(), 1.0F, 2.0F);
        }
        return multiplier;
    }
}

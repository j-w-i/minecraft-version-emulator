/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalPotionStrengths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Instant Health heals 6 per level before Release 1.6.1, not 4.
 *
 * <p>minecraft.wiki, Java Edition 1.6.1: "Instant Health nerfed by 33% (heals only 4 health points/2 hearts at level
 * I)." In {@code HealOrHarmMobEffect} the health branch uses {@code iconst_4} shifted by the amplifier and the harm
 * branch uses {@code bipush 6}; THIS MIXIN TOUCHES ONLY THE 4, never the 6, so Instant Damage keeps its modern amount.
 * Both branches exist in {@code applyEffectTick} and {@code applyInstantenousEffect}, so both methods are targeted.
 *
 * <p>The profile is resolved through the server context because a mob effect is a stateless singleton with no level.
 */
@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMobEffectHistoricalStrengthMixin {
    @ModifyConstant(method = {"applyEffectTick", "applyInstantenousEffect"}, constant = @Constant(intValue = 4))
    private int historicalExperience$instantHealthBase(int modernBase) {
        return HistoricalPotionStrengths.instantHealthBase(modernBase);
    }
}

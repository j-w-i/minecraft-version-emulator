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
 * Regeneration restores one health point every 25 ticks before Release 1.6.1, not every 50.
 *
 * <p>minecraft.wiki, Java Edition 1.6.1: "Regeneration nerfed by 50% (takes 50 ticks/2.5 seconds to restore 1 health
 * point/.5 heart at level I)." The 50 in {@code shouldApplyEffectTickThisTick} is that interval base, shifted right by
 * the amplifier; 25 is the pre-1.6.1 base. The choice lives in {@link HistoricalPotionStrengths}, which resolves the
 * profile through the server context because a mob effect is a stateless singleton with no level.
 */
@Mixin(targets = "net.minecraft.world.effect.RegenerationMobEffect")
public abstract class RegenerationMobEffectHistoricalStrengthMixin {
    @ModifyConstant(method = "shouldApplyEffectTickThisTick", constant = @Constant(intValue = 50))
    private int historicalExperience$regenerationInterval(int modernBase) {
        return HistoricalPotionStrengths.regenerationTickBase(modernBase);
    }
}

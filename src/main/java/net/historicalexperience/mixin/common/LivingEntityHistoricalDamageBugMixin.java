/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalDamageValues;
import net.historicalexperience.version.ActiveVersion;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Beta 1.5's one-extra-damage bug, applied at the one point every melee hit passes through.
 *
 * <p>minecraft.wiki / research/feature-timeline.json: Beta 1.5 "All attacks from players now deal 1 more damage than
 * intended unless the player is currently moving upwards in singleplayer"; fixed in the Beta 1.8 Pre-release. The
 * decision itself is {@link HistoricalDamageValues#betaDamageBugBonus}, which is pure and unit-tested; this hook only
 * supplies the player's motion.
 *
 * <p><b>Melee only, by construction.</b> The source's direct entity must be the player: an arrow's direct entity is
 * the arrow, so a bow shot is untouched, which is what "attacks" means here. <b>Both sides</b>, because damage
 * prediction compares client and server results and a one-sided point would show as a health flicker.
 *
 * <p>Shared: melee damage is authoritative on the server but is predicted on the client too.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalDamageBugMixin {
    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float historicalExperience$betaDamageBug(float amount, DamageSource source) {
        if (!(source.getDirectEntity() instanceof Player player)) {
            return amount;
        }
        float bonus = HistoricalDamageValues.betaDamageBugBonus(ActiveVersion.current(),
            player.getDeltaMovement().y > 0.0D);
        return bonus == 0.0F ? amount : amount + bonus;
    }
}

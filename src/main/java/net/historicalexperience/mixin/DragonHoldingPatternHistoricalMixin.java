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
 */package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalDragonFight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the fireball run with a charge.
 *
 * <p>The Release 1.9 dragon answers a destroyed crystal, and sometimes a nearby player, by strafing: it flies at the
 * player and shoots a dragon fireball that leaves a cloud of dragon's breath. Fireballs and the breath are 1.9 content,
 * so no strafe happens here. The dragon instead charges at where the player stands, which is the attack it had before:
 * a pass through the player, with its wings and head doing the damage. The charge phase is vanilla's own - it already
 * ends by returning to the holding pattern.
 *
 * <p>The aim height is the original's, read from Mojang's Release 1.0 client (see
 * {@link HistoricalDragonFight#aimHeightAbove}): at 48 blocks it aims at the player's feet, closer than that below them,
 * so the pass sweeps the ground. The vertical drive that makes it actually descend is changed in
 * {@code EnderDragonVerticalDriveHistoricalMixin}.
 */
@Mixin(DragonHoldingPatternPhase.class)
public abstract class DragonHoldingPatternHistoricalMixin extends AbstractDragonPhaseInstance {
    // Extends the phase superclass only to reach its protected {@code dragon} field; Mixin never instantiates this.
    protected DragonHoldingPatternHistoricalMixin(EnderDragon dragon) {
        super(dragon);
    }

    @Inject(method = "strafePlayer", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$chargeInsteadOfStrafe(Player player, CallbackInfo callback) {
        if (this.dragon.level() instanceof ServerLevel level && HistoricalDragonFight.originalFight(level)) {
            this.dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
            double dx = player.getX() - this.dragon.getX();
            double dz = player.getZ() - this.dragon.getZ();
            double aim = player.getBoundingBox().minY
                + HistoricalDragonFight.aimHeightAbove(Math.sqrt(dx * dx + dz * dz));
            this.dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER)
                .setTarget(new net.minecraft.world.phys.Vec3(player.getX(), aim, player.getZ()));
            callback.cancel();
        }
    }
}
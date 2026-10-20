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

import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * From Release 1.3, a boat broken BY A PLAYER drops the boat item, not three planks and two sticks.
 *
 * <p>minecraft.wiki, Boat history, 12w27a (a Release 1.3 snapshot): "Boats now drop their item instead of 3 oak planks
 * and 2 sticks if broken by a player. Instead, it needs to fall certain heights to drop 3 oak wood planks and 2
 * sticks." So the FALL path keeps vanilla's planks-and-sticks, and only the player-break path changes. Vanilla removes
 * a boat with the KILLED reason and spawns the planks in {@code Boat.remove}; it does not say why the boat was removed,
 * which is why this hooks {@code VehicleEntity.destroy(DamageSource)} - the one place the cause is still known.
 *
 * <p>The vanilla body is cancelled and replaced with the item plus a DISCARDED removal, so the plank drops never run;
 * DISCARDED does not {@code shouldDestroy()}, so {@code Boat.remove} spawns nothing.
 */
@Mixin(VehicleEntity.class)
public abstract class VehicleEntityHistoricalBoatDropMixin {
    @Inject(method = "destroy(Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At("HEAD"), cancellable = true)
    private void historicalExperience$boatDropsItself(DamageSource source, CallbackInfo callback) {
        if (!((Object) this instanceof Boat boat)) {
            return;
        }
        if (!(boat.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(source.getEntity() instanceof Player)) {
            return;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(level);
        if (!Features.active(Feature.BOAT_DROP_ITEM_ON_PLAYER_BREAK, profile)) {
            return;
        }
        callback.cancel();
        boat.spawnAtLocation(new ItemStack(boat.getDropItem()));
        boat.setRemoved(RemovalReason.DISCARDED);
    }
}

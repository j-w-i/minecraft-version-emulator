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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses the sneak key in versions that had no sneaking.
 *
 * <p>Reported by the owner after playing an Alpha 1.0.1 world: "i can also still sneak way before sneaking was
 * added" - and sneaking arrives in <b>Alpha 1.1.0</b>. No gate existed for it anywhere, so this is a newly
 * implemented behaviour rather than a corrected one.
 *
 * <p>The hook is the shift-key flag itself rather than the key binding. Cancelling the flag covers every
 * consequence at once: the crouch pose, the reduced movement speed, the reduced height, and the client's own
 * prediction, because the client sets the same flag locally. A hook on the key binding would have left the server
 * free to accept a crouch from a client that sent one, and would have missed anything that sets the flag without a
 * key press.
 *
 * <p>A shared mixin: the flag exists on both sides, and both must refuse it or one will disagree with the other.
 */
@Mixin(Entity.class)
public abstract class EntityHistoricalSneakingMixin {
    /** Vanilla: {@code setShiftKeyDown(boolean)}. */
    @Inject(method = "setShiftKeyDown(Z)V", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$refuseSneaking(boolean sneaking, CallbackInfo callback) {
        if (Features.active(Feature.NO_SNEAKING)) {
            // Not even a release of the key, so the flag can never be left set by a version that had no crouch.
            callback.cancel();
        }
    }
}

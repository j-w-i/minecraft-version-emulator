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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disc/head drops are a separate custom-death path, not part of the replaceable gunpowder loot table. */
@Mixin(Creeper.class)
public abstract class CreeperHistoricalEarlyDropsMixin {
    /** Victims award skulls through several separate overrides. Refuse capability BEFORE their budget/item creation,
     * not by cancelling all victim custom loot (which would also destroy imported stored equipment).
     */
    @ModifyReturnValue(method = "canDropMobsSkull", at = @At("RETURN"), require = 1)
    private boolean historicalExperience$noEarlySkullAward(boolean original) {
        Creeper self = (Creeper) (Object) this;
        return original && !(self.level() instanceof ServerLevel level
            && EarlyGameplayRecipes.supports(HistoricalProfileLookup.profileIdForLevel(level)));
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$noEarlyDiscsOrHeads(ServerLevel level, DamageSource source,
            boolean recentlyHit, CallbackInfo ci) {
        if (EarlyGameplayRecipes.supports(HistoricalProfileLookup.profileIdForLevel(level))) ci.cancel();
    }
}

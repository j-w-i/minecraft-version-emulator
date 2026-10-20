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

import net.historicalexperience.gameplay.HistoricalHardcore;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A build that predates hardcore mode cannot be in hardcore mode.
 *
 * <p>research/version-transitions.json, Beta 1.9 Prerelease 2's additions: "Hardcore mode (locks difficulty to Hard,
 * permadeath)". So Beta 1.8.1 and Prerelease 1 had no hardcore even if a level's stored flag says otherwise - a world
 * imported, upgraded or hand-edited can carry the flag, and the game must read it the way the build would have.
 *
 * <p>The stored flag is NOT edited: this answers the question at read time, so the record keeps what it had (the
 * project's rule for unsupported stored content) and only the behaviour is version-correct.
 *
 * <p>What this does not do is hide the option in world creation; that is a client screen change and is recorded as
 * open. Server authority is what stops hardcore behaviour actually happening.
 */
@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataHistoricalHardcoreMixin {
    @ModifyReturnValue(method = "isHardcore", at = @At("RETURN"))
    private boolean historicalExperience$noHardcoreBeforePrerelease2(boolean hardcore) {
        if (!hardcore) {
            return false;
        }
        return !HistoricalHardcore.gated((LevelData) (Object) this);
    }
}

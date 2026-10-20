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
package net.historicalexperience.mixin.client;

import java.util.List;

import net.historicalexperience.client.hud.HistoricalDebugScreen;
import net.historicalexperience.version.ActiveVersion;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes the debug overlay's coordinate readout before Alpha v1.2.3.
 *
 * <p>minecraft.wiki, Debug screen history: "v1.2.3: The coordinates have been added to the debug screen." The
 * filtering itself is {@link HistoricalDebugScreen#filter}, which is pure and unit-tested; this mixin only supplies
 * the line list.
 *
 * <p>A client class, so this lives in the client mixin config: a dedicated server never loads
 * {@code DebugScreenOverlay}, which is also why a server boot cannot verify this hook - only a client launch can.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayHistoricalCoordinatesMixin {
    @ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
    private List<String> historicalExperience$hideEarlyCoordinates(List<String> lines) {
        return HistoricalDebugScreen.filter(ActiveVersion.current(), lines);
    }
}

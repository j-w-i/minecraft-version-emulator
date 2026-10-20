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
package net.historicalexperience.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.historicalexperience.server.SnapshotCreativeRequest;
import org.lwjgl.glfw.GLFW;

/** 11w49a's raw B shortcut. The server, not this input adapter, decides whether it is permitted. */
public final class SnapshotCreativeKey {
    private static boolean held;
    private SnapshotCreativeKey() { }
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            boolean down = GLFW.glfwGetKey(client.getWindow().getWindow(), GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS;
            boolean press = down && !held;
            held = down; // Also track while a screen is open: closing it with B held must not toggle modes.
            if (!press || client.player == null || client.screen != null || !client.isWindowActive()) return;
            if (!HistoricalCreativeRules.hasCreativeShortcut(HistoricalClientRules.activeSnapshotPatchId())) return;
            if (ClientPlayNetworking.canSend(SnapshotCreativeRequest.Payload.TYPE)) ClientPlayNetworking.send(new SnapshotCreativeRequest.Payload());
        });
    }
}

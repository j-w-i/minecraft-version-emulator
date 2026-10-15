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
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.historicalexperience.server.PortalSpawnRequest;
import net.historicalexperience.version.HistoricalVersion;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alpha v1.2.2a's F4 key, bound the way that client bound it: the raw key, not a rebindable control.
 *
 * <h2>How the original read the key</h2>
 *
 * <p>Its {@code Minecraft.java} polled {@code Keyboard.next()} and switched on {@code Keyboard.getEventKey()}: key
 * code 62 was F4, alongside 87 for F11 and 63 for F3. It is a development shortcut with no entry in the controls
 * menu, so this reads the same physical key through GLFW rather than registering a {@code KeyMapping} - a player who
 * rebinds "Drop Item" to F4 must not lose the portal, and a player who binds something else to F4 must not have it
 * fire a portal. Reading the raw key is also the only way to keep the era's behaviour honest: the original had no
 * concept of a key binding for it.
 *
 * <h2>Why the press is edge-triggered</h2>
 *
 * <p>GLFW reports the key as DOWN for every tick it is held, and each press builds a whole portal. Sending on the
 * held state would fill the world with portals for as long as a finger rested on the key, so only the transition from
 * up to down sends.
 *
 * <h2>What the client does NOT decide</h2>
 *
 * <p>It sends an empty request and nothing else. The server checks the version again, chooses the position and writes
 * the blocks, so a client that lies about its version gains nothing.
 */
public final class F4PortalKey {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/f4-portal");

    /** GLFW's key code for F4, which is the code the Alpha client compared against. */
    private static final int GLFW_KEY_F4 = GLFW.GLFW_KEY_F4;

    /** True between the tick the key goes down and the tick it comes back up. */
    private static boolean held;

    private F4PortalKey() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.screen != null) {
                // A screen open means the player is not in the world the key applies to. The key is also forgotten,
                // so closing a screen while holding F4 does not immediately build a portal.
                held = false;
                return;
            }
            boolean down = GLFW.glfwGetKey(client.getWindow().getWindow(), GLFW_KEY_F4) == GLFW.GLFW_PRESS;
            if (down && !held) {
                request(client);
            }
            held = down;
        });
    }

    private static void request(net.minecraft.client.Minecraft client) {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        String versionId = version == null ? null : version.id();
        if (!HistoricalGameplayTweaks.f4SpawnsPortal(versionId)) {
            // Every other version: the key does whatever it does today, which is nothing.
            return;
        }
        LOGGER.info("F4 pressed in {} - asking the server to build a portal", versionId);
        ClientPlayNetworking.send(new PortalSpawnRequest.Payload());
    }
}

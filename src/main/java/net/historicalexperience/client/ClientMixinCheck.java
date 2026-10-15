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
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a client-only class on request, so a CLIENT mixin can be proved to apply without a person opening a screen.
 *
 * <h2>Why this exists</h2>
 *
 * <p>A mixin is applied when its TARGET class loads, so a broken client mixin is invisible until the player reaches
 * the screen that loads it - which is how a {@code @ModifyExpressionValue} aimed at a void method crashed a live game
 * the moment an enchanting table was opened. A dedicated server cannot see this at all, because the class does not
 * exist in a server environment; the mixin is simply reported as a target that was not found.
 *
 * <p>Class loading is enough. If an injection in the class is invalid, the load itself fails with a
 * {@code BootstrapMethodError}, so forcing the load is the same test the player would perform by opening the screen.
 *
 * <p>Inert unless {@code -Dhistorical_experience.clientMixinCheck=<class name>} names exactly one class.
 */
public final class ClientMixinCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/client-mixin-check");
    private static final String PROPERTY = "historical_experience.clientMixinCheck";

    private static boolean done;

    private ClientMixinCheck() {
    }

    public static void registerIfRequested() {
        String target = System.getProperty(PROPERTY);
        if (target == null || target.isBlank()) {
            return;
        }
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (done || client.screen == null) {
                // Wait for a screen, so the client is past its own startup and a failure here is unambiguous.
                return;
            }
            done = true;
            try {
                Class<?> loaded = Class.forName(target);
                LOGGER.info("CLIENTMIXINCHECK loaded {} ({}) - its mixins applied", target,
                    loaded.getSimpleName());
            } catch (Throwable failure) {
                // Deliberately swallowed and reported rather than thrown: the point is the log line, and a thrown
                // error here would be attributed to whatever screen happened to be open.
                LOGGER.error("CLIENTMIXINCHECK FAILED to load {}: {}", target, failure);
            }
        });
    }
}
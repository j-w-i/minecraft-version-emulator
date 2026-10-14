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
package net.historicalexperience.integration.modernerbeta;

/**
 * Raised when the Moderner Beta integration cannot satisfy a request.
 *
 * <p>This is a hard failure by design: a historical world whose terrain preset cannot be resolved
 * must not silently fall back to vanilla generation or to a different Moderner Beta preset, since
 * either would produce terrain that does not match the world's persisted metadata.
 */
public class ModernerBetaIntegrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ModernerBetaIntegrationException(String message) {
        super(message);
    }

    public ModernerBetaIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

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
package net.historicalexperience.gameplay;

import net.historicalexperience.version.ActiveVersion;

/**
 * The running server's historical profile, for code that has no level, entity or player to ask.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@link ActiveVersion} is the SESSION version: it is set when a client joins a world, and on a dedicated server it
 * is empty (the `/hex status` output says so in as many words). Some rules live in classes that are stateless
 * singletons - {@code RegenerationMobEffect}, {@code HealOrHarmMobEffect} - where there is no level to resolve a
 * profile from and no session to read. Without this, a potion-strength rule has no way to know which build it is in.
 *
 * <p>The lifecycle notes the profile once the world's historical state is resolved; ordinary worlds clear it. The
 * session wins when present, so a client predicting against its integrated server and a dedicated server both answer
 * for the world they are actually in.
 *
 * <p>One value per JVM is deliberate and documented: a JVM with two servers (the integrated client's and a dedicated
 * one) is not a configuration this project supports, and the last server to start owning the value is the honest
 * description of a static singleton.
 */
public final class HistoricalServerProfile {
    private static volatile String profileId;

    private HistoricalServerProfile() {
    }

    /** Records the profile of the server that just resolved its world; null for an ordinary world. */
    public static void set(String id) {
        profileId = id;
    }

    /** Forgets the recorded profile (server stopping, or an ordinary world). */
    public static void clear() {
        profileId = null;
    }

    /** The profile stateless code should answer for: the session when one is active, else the server's. */
    public static String active() {
        String session = ActiveVersion.current();
        return session != null ? session : profileId;
    }

    /** The recorded server profile, without consulting the session. */
    public static String recorded() {
        return profileId;
    }
}

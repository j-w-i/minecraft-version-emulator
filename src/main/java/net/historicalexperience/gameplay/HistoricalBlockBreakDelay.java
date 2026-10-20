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
package net.historicalexperience.gameplay;

import java.util.Map;
import java.util.UUID;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;

/**
 * The five-tick delay between breaking blocks that Release 1.3.1 removed.
 *
 * <p>minecraft.wiki, Java Edition 1.3.1: "The 1/4 second (5 game tick) delay between block breaking has been removed
 * for players in Survival." So in every declared build before Release 1.3 (this project's 1.3.1 baseline) a survival
 * player cannot break another block within five ticks of the previous one.
 *
 * <p><b>How far back this is applied:</b> the wiki documents the <b>removal</b> and no introduction, so the delay is
 * applied to every earlier declared build. That is an inference from the absence of a later introduction row, and it is
 * recorded here rather than presented as a dated fact.
 *
 * <p>The decision is pure so it can be tested without a server; the mixin supplies the game time and the player.
 */
public final class HistoricalBlockBreakDelay {
    /** The documented delay: one quarter of a second, five ticks. */
    public static final int DELAY_TICKS = 5;

    private static final Map<UUID, Long> LAST_BREAK = new java.util.concurrent.ConcurrentHashMap<>();

    private HistoricalBlockBreakDelay() {
    }

    /** True when this break must be refused because the previous one was fewer than five ticks ago. */
    public static boolean shouldRefuse(String versionId, boolean survival, long now, long lastBreak) {
        if (!survival || lastBreak < 0) {
            return false;
        }
        return Features.active(Feature.BLOCK_BREAK_DELAY, versionId) && now - lastBreak < DELAY_TICKS;
    }

    /** The tick of this player's last break, or -1 when they have not broken a block in this session. */
    public static long lastBreak(UUID player) {
        Long tick = player == null ? null : LAST_BREAK.get(player);
        return tick == null ? -1L : tick;
    }

    /** Records a successful break. */
    public static void recordBreak(UUID player, long now) {
        if (player != null) {
            LAST_BREAK.put(player, now);
        }
    }

    /** Forgets a player's history (used when they leave, so the map cannot grow without bound). */
    public static void forget(UUID player) {
        if (player != null) {
            LAST_BREAK.remove(player);
        }
    }
}

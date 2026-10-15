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
package net.historicalexperience.server;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.historicalexperience.world.upgrade.BiomeConversionRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Holds the running server's world directory and converted-chunk bookkeeping.
 *
 * <h2>Why this exists</h2>
 *
 * <p>Moderner Beta's biome source is constructed from settings alone and is never handed a level, but it has to know
 * which chunks existed before a biome conversion so it can report the converted biome for those and generate normally
 * for everything else. Minecraft 1.21.1 provides no static server accessor, so the server is captured here on the
 * lifecycle events and read from there.
 *
 * <p>The record is read once per world rather than per query: a biome source asks for a position very often, and the
 * record is a file on disk.
 */
public final class HistoricalServerContext {
    private static volatile MinecraftServer server;
    private static volatile String cachedWorld;
    private static volatile Set<Long> cachedConvertedChunks = Set.of();
    private static volatile String cachedTarget;

    private HistoricalServerContext() {
    }

    /** Called from the lifecycle when a server starts. */
    public static void onServerStarted(MinecraftServer running) {
        server = running;
        // Forget the previous world's record so a second world in the same session reads its own.
        cachedWorld = null;
        cachedConvertedChunks = Set.of();
        cachedTarget = null;
    }

    /** Called from the lifecycle when a server stops. */
    public static void onServerStopped() {
        server = null;
        cachedWorld = null;
        cachedConvertedChunks = Set.of();
        cachedTarget = null;
    }

    /** The running server, or null when there is none. */
    public static MinecraftServer server() {
        return server;
    }

    /**
     * The chunks that existed before this world's biome conversion, or an empty set when there is no conversion.
     *
     * <p>Empty is the safe answer: Moderner Beta then behaves exactly as it always would.
     */
    public static Set<Long> convertedChunks() {
        refresh();
        return cachedConvertedChunks;
    }

    /**
     * The single biome a conversion wrote for this world, or null when there is no conversion or its generation carried
     * each biome to its own counterpart (in which case the chunk's stored biome is the answer).
     */
    public static String convertedTarget() {
        refresh();
        return net.historicalexperience.world.upgrade.BiomeCarryForward.singleBiomeForLabel(cachedTarget);
    }

    /** Reads the record when the world has changed since the last read. */
    private static void refresh() {
        MinecraftServer running = server;
        if (running == null) {
            cachedConvertedChunks = Set.of();
            cachedTarget = null;
            return;
        }
        String world;
        Path directory;
        try {
            directory = running.getWorldPath(LevelResource.ROOT);
            world = directory.toString();
        } catch (RuntimeException unavailable) {
            cachedConvertedChunks = Set.of();
            cachedTarget = null;
            return;
        }
        if (world.equals(cachedWorld)) {
            return;
        }
        cachedConvertedChunks = BiomeConversionRecord.convertedChunks(directory);
        List<String> targets = BiomeConversionRecord.convertedTargets(directory);
        cachedTarget = targets.isEmpty() ? null : targets.get(targets.size() - 1);
        cachedWorld = world;
    }
}

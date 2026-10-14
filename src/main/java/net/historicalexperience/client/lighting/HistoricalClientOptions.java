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
package net.historicalexperience.client.lighting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Cached booleans in the shared client options file. Rendering must not open
 * the properties file per block or frame. External file edits take effect on restart;
 * in-game changes update the cache immediately.
 */
public final class HistoricalClientOptions {
    private static final String FILE = "historical_experience-client.properties";
    private static final ConcurrentHashMap<String, Boolean> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> STRING_CACHE = new ConcurrentHashMap<>();

    private HistoricalClientOptions() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    /** The stored string, or {@code fallback}; cached after the first read like the booleans. */
    public static String getString(String key, String fallback) {
        String cached = STRING_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String value = fallback;
        try {
            Path path = path();
            if (Files.isRegularFile(path)) {
                Properties properties = new Properties();
                try (InputStream in = Files.newInputStream(path)) {
                    properties.load(in);
                }
                value = properties.getProperty(key, fallback);
            }
        } catch (IOException | RuntimeException failure) {
            value = fallback;
        }
        STRING_CACHE.put(key, value);
        return value;
    }

    public static synchronized void setString(String key, String value) {
        STRING_CACHE.put(key, value);
        store(key, value);
    }

    /** The stored value, or {@code fallback} when absent or unreadable - a bad config must not break lighting. */
    public static boolean get(String key, boolean fallback) {
        return CACHE.computeIfAbsent(key, ignored -> read(key, fallback));
    }

    private static boolean read(String key, boolean fallback) {
        try {
            Path path = path();
            if (!Files.isRegularFile(path)) {
                return fallback;
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            }
            String value = properties.getProperty(key);
            return value == null ? fallback : !"false".equalsIgnoreCase(value);
        } catch (IOException | RuntimeException failure) {
            return fallback;
        }
    }

    /** Persists the value best-effort; the in-memory value is already set by the caller. */
    public static synchronized void set(String key, boolean value) {
        CACHE.put(key, value);
        store(key, Boolean.toString(value));
    }

    private static void store(String key, String value) {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    properties.load(in);
                }
            }
            properties.setProperty(key, value);
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "Historical Experience client options");
            }
        } catch (IOException | RuntimeException failure) {
            // Best effort.
        }
    }
}

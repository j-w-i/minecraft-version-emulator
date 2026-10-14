package net.historicalexperience.client.lighting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Reads and writes one boolean in the client options file shared by the lighting toggles.
 *
 * <p>The classic engine and the old smooth lighting each carry their own copy of this logic, written before there
 * was a third toggle. This is the shared form for new options; the two older copies are left alone because they work
 * and their behaviour is pinned by tests.
 */
final class HistoricalClientOptions {
    private static final String FILE = "historical_experience-client.properties";

    private HistoricalClientOptions() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    /** The stored value, or {@code fallback} when absent or unreadable - a bad config must not break lighting. */
    static boolean get(String key, boolean fallback) {
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
    static void set(String key, boolean value) {
        try {
            Path path = path();
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    properties.load(in);
                }
            }
            properties.setProperty(key, Boolean.toString(value));
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "Historical Experience client options");
            }
        } catch (IOException | RuntimeException failure) {
            // Best effort.
        }
    }
}

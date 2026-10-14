package net.historicalexperience.client.lighting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * The remaining client lighting toggles from the owner's list, and their version windows.
 *
 * <p>These are the light-related entries that are NOT the round-robin engine ({@link HistoricalOldLighting}) and not
 * the smooth-shading window ({@link HistoricalSmoothLighting}): leaves shading, water light absorption, and the old
 * light colour. Each is a separate tweak upstream and is kept separate here, because they do not share one boundary
 * and folding them together would make one of them wrong.
 */
public final class HistoricalLightingTweaks {
    /** Shares the client options file; one key per tweak. */
    private static final String FILE = "historical_experience-client.properties";

    /** Beta 1.8 Pre-release replaced the engine, so these belong to everything before it. */
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";

    private static final String LEAVES_KEY = "oldLeavesLighting";
    private static final String WATER_KEY = "oldWaterLighting";

    /**
     * The light a block of water absorbs, as the old engine had it.
     *
     * <p>The modern game lets water dim light gradually; the old engine gave water a fixed light-blocking value of 3,
     * which is upstream's number for {@code OLD_WATER_LIGHTING} and the reason water was notably clearer to see
     * through. Kept here as a named constant so the figure has one home.
     */
    public static final int OLD_WATER_LIGHT_BLOCK = 3;

    private HistoricalLightingTweaks() {
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
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

    private static void write(String key, boolean value) {
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
            // Best effort: the in-memory value is already set for this run.
        }
    }

    /** Whether leaves are drawn without shading, which the pre-Beta-1.8 engine did. */
    public static boolean oldLeavesLighting() {
        return beforeNewEngine() && read(LEAVES_KEY, true);
    }

    public static boolean setOldLeavesLighting(boolean enabled) {
        if (enabled && !availableFor(HistoricalClientRules.activeVersion())) {
            return false;
        }
        write(LEAVES_KEY, enabled);
        return true;
    }

    /** Whether water absorbs the fixed old amount of light rather than the modern gradient. */
    public static boolean oldWaterLighting() {
        return beforeNewEngine() && read(WATER_KEY, true);
    }

    public static boolean setOldWaterLighting(boolean enabled) {
        if (enabled && !availableFor(HistoricalClientRules.activeVersion())) {
            return false;
        }
        write(WATER_KEY, enabled);
        return true;
    }

    /** Whether these lighting rules belong to this version at all: everything before Beta 1.8. */
    public static boolean availableFor(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        return HistoricalVersions.byId(FIRST_NEW_LIGHTING_BUILD)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
    }

    private static boolean beforeNewEngine() {
        return availableFor(HistoricalClientRules.activeVersion());
    }
}

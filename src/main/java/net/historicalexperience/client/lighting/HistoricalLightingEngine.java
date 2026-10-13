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
 * The old Classic lighting engine, ported from Nostalgic Tweaks, toggleable and available only before Beta 1.3.
 *
 * <h2>Upstream</h2>
 *
 * <p>{@code common/src/main/java/mod/adrenix/nostalgic/helper/candy/light/NostalgicDataLayer.java}, method
 * {@code getLightValue}, and {@code helper/candy/light/LightingHelper.java}, method {@code getClassicLight}.
 * Nostalgic Tweaks is LGPL-3.0; the attribution is in THIRD_PARTY_NOTICES.md. The rule is ported rather than
 * paraphrased, because paraphrasing a lighting rule is how you get lighting that is subtly wrong:
 *
 * <pre>
 * if (OLD_CLASSIC_ENGINE) {
 *     if (layer == BLOCK) return 13;
 *     return getClassicLight(skyLight, level, blockPos);
 * }
 * </pre>
 *
 * <p>So under the classic engine <b>block light is a constant 13</b> — torches and lamps make no difference, which
 * is what Classic actually did — and sky light becomes binary: full brightness if you can see the sky, nothing if
 * anything solid or water is above you.
 *
 * <h2>Why the boundary is Beta 1.3 and why the toggle disappears there</h2>
 *
 * <p>The owner's instruction is to have this "up until Beta 1.3, at which case remove the toggle". That is not an
 * arbitrary cutoff and it is corroborated by this project's own research rather than only accepted:
 * {@code research/minecraft-version-history.md} records Beta 1.3 as <i>"Rendering/audio: Smooth-lighting engine
 * and graphics option added"</i>, and lists it again under "Difference from Beta 1.2: ... smooth lighting". From
 * Beta 1.3 the new engine IS the game's engine, so at 1.3 and later there is nothing to toggle — which is why
 * {@link #availableFor} answers false there and the option is not offered at all, rather than being offered and
 * ignored.
 *
 * <h2>Why an unknown version means "do nothing"</h2>
 *
 * <p>{@link HistoricalClientRules#activeVersion()} returns null when the client cannot establish the version
 * (before a multiplayer server's push arrives, or on a server that never sends one). A client that guessed would
 * render one thing while the server believed another, so an unknown version leaves vanilla lighting alone.
 */
public final class HistoricalLightingEngine {
    /** The Block-layer value Nostalgic Tweaks returns under this engine. */
    public static final int CLASSIC_BLOCK_LIGHT = 13;

    /** The first build whose engine is the smooth one, so the last build that offers this toggle is Beta 1.2. */
    private static final String FIRST_SMOOTH_LIGHTING_BUILD = "historical_experience:beta_1_3";

    private static final String TOGGLE_FILE = "historical_experience-client.properties";
    private static final String TOGGLE_KEY = "oldClassicLightingEngine";

    private static volatile Boolean toggle;

    private HistoricalLightingEngine() {
    }

    /**
     * Whether the classic engine is offered for this version.
     *
     * <p>A null version is unknown, and unknown is not "yes".
     */
    public static boolean availableFor(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        return HistoricalVersions.byId(FIRST_SMOOTH_LIGHTING_BUILD)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
    }

    /** Whether the classic engine should actually change the light values: offered AND switched on. */
    public static boolean activeFor(HistoricalVersion version, boolean toggleOn) {
        return toggleOn && availableFor(version);
    }

    /** Whether the classic engine is active for the client's current version. */
    public static boolean activeNow() {
        return activeFor(HistoricalClientRules.activeVersion(), toggle());
    }

    /**
     * Block light under the classic engine, which is a constant.
     *
     * <p>Ported verbatim from the upstream branch: {@code if (layer == LightLayer.BLOCK) return 13;}.
     */
    public static int classicBlockLight() {
        return CLASSIC_BLOCK_LIGHT;
    }

    /**
     * Sky light under the classic engine, which is binary rather than graded.
     *
     * <p>Ported from {@code getClassicLight}, with the upward scan collapsed into {@code obstructedAbove} so the
     * decision itself can be tested without a running client. The upstream order matters and is preserved: a
     * dimension without sky light is 0 <em>before</em> the pass-through check, and a value already at either
     * extreme is returned untouched rather than being recomputed.
     *
     * @param vanillaSkyLight  the value the modern engine computed
     * @param maxLightLevel    {@code level.getMaxLightLevel()}, 15 on the Overworld
     * @param hasSkyLight      whether the dimension type has sky light at all
     * @param obstructedAbove  whether water or a fully light-blocking block stands between this position and the
     *                         build limit
     */
    public static int classicSkyLight(int vanillaSkyLight, int maxLightLevel, boolean hasSkyLight,
            boolean obstructedAbove) {
        if (!hasSkyLight) {
            return 0;
        }
        if (vanillaSkyLight >= maxLightLevel || vanillaSkyLight <= 0) {
            return vanillaSkyLight;
        }
        return obstructedAbove ? 0 : maxLightLevel;
    }

    /** The toggle, loaded from the client config once and defaulted to on, because that is what these builds did. */
    public static boolean toggle() {
        Boolean current = toggle;
        if (current == null) {
            current = load();
            toggle = current;
        }
        return current;
    }

    /**
     * Sets the toggle and persists it.
     *
     * <p>Refused when the current version does not offer it, so a value stored in one world cannot silently start
     * applying in another: the caller is told no rather than being obeyed into a wrong render.
     *
     * @return whether the change was applied
     */
    public static boolean setToggle(boolean enabled) {
        if (enabled && !availableFor(HistoricalClientRules.activeVersion())) {
            return false;
        }
        toggle = enabled;
        save(enabled);
        return true;
    }

    /** Forgets the cached value. Used when a world is left, so the next world re-reads the config. */
    public static void resetCache() {
        toggle = null;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(TOGGLE_FILE);
    }

    private static boolean load() {
        // Default ON. Before Beta 1.3 this is not a preference, it is what the game did, so the default has to be
        // the historical one; the toggle exists so a player can opt out, not so they must opt in.
        try {
            Path path = configPath();
            if (!Files.isRegularFile(path)) {
                return true;
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            }
            return !"false".equalsIgnoreCase(properties.getProperty(TOGGLE_KEY, "true"));
        } catch (IOException | RuntimeException failure) {
            // A config that cannot be read must not break lighting; fall back to the historical value.
            return true;
        }
    }

    private static void save(boolean enabled) {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            if (Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    properties.load(in);
                }
            }
            properties.setProperty(TOGGLE_KEY, Boolean.toString(enabled));
            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "Historical Experience client options");
            }
        } catch (IOException | RuntimeException failure) {
            // Persisting is best effort: the in-memory value is already set and lighting stays correct this run.
        }
    }
}

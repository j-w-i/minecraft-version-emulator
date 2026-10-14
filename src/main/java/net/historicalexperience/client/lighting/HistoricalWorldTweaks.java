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
 * The sky, cloud and item-rendering toggles from the owner's list, with the boundary each one actually has.
 *
 * <h2>Boundaries that are ESTABLISHED, with the quotation they rest on</h2>
 *
 * <ul>
 *   <li><b>Cloud height</b> — 108 from Classic 0.0.14a to Beta 1.7.3, 128 from Beta 1.8. minecraft.wiki, Cloud
 *       history: <i>"Added clouds, which float westward between layers 108 and 112"</i> and Beta 1.8 <i>"Now clouds
 *       float between layers 128 and 132."</i> The declared catalogue straddles this boundary — Infdev through Beta
 *       1.7.3 are below it and Beta 1.8.1 through Release 1.6.4 are above it — so the old height applies to the
 *       earlier group only. An earlier draft said "every declared version", and the boundary test caught it.</li>
 *   <li><b>2D dropped items</b> — throughout. Item (entity) history dates the 3D spinning model to 12w50a, which is
 *       Release 1.4.6, <b>after</b> the newest declared version (Release 1.6.4 includes it, so the rule stops at the
 *       Release 1.4 profile). <i>"When dropped, items now render in the fancy graphics setting as 3D spinning
 *       animations (non-block items no longer display as sprites)."</i></li>
 *   <li><b>Recipe book</b> — added in 1.12 (17w13a), <i>"Added the recipe book."</i> Every declared version predates
 *       it, so it is off throughout.</li>
 * </ul>
 *
 * <h2>Boundaries that are NOT established, and are therefore not invented</h2>
 *
 * <p>Three of the owner's items could not be dated from the wiki, and this class deliberately does not guess a
 * boundary for them. They are listed so the gap is visible rather than silently filled in:
 *
 * <ul>
 *   <li><b>Stars.</b> The wiki dates individual changes — Beta 1.5 <i>"Stars are now slightly dimmer"</i>, Release
 *       1.3.1 <i>"Stars are now slightly smaller and brighter"</i> — but never explains what upstream's "alpha",
 *       "beta" and "modern" star styles are, so their boundaries are unknown.</li>
 *   <li><b>Universal sky colour.</b> The per-era colours exist in upstream's code, but the wiki does not date them,
 *       and the one related change it does record (Alpha v1.2.0 making the sky colour depend on biome temperature)
 *       is a different feature.</li>
 *   <li><b>Nether fog.</b> The colour is known from Alpha v1.2.0 — <i>"The fog color in the Nether is #330808"</i> —
 *       but not when it changed or how dense it was.</li>
 *   <li><b>Left-click activation</b> of doors, levers and buttons: the wiki records no input transition at all, so
 *       there is nothing to gate on. What would settle it is the old block classes themselves from a client jar.</li>
 * </ul>
 *
 * <h2>A finding that CORRECTS the list it came from</h2>
 *
 * <p><b>The blue void does not belong to these versions.</b> The premise behind "old blue void (alpha, beta, modern)"
 * is that the void was blue in alpha and beta and changed later. minecraft.wiki, Void history, says the opposite
 * direction: Beta 1.8 <i>"The Void's color above Y=0 was changed from the skybox's default color to a deeper blue"</i>,
 * and 18w01a <i>"no longer blue and behaves similarly to what it was before Beta 1.8"</i>. So the blue void is a
 * Beta 1.8 to 1.13 feature, and <b>no declared version had it</b> - the same shape of error as the Classic lighting
 * engine, where a behaviour was about to be implemented for versions that never had it.
 */
public final class HistoricalWorldTweaks {
    private static final String FILE = "historical_experience-client.properties";
    private static final String CLOUD_KEY = "oldCloudHeight";
    private static final String ITEM_2D_KEY = "old2dItems";
    private static final String RECIPE_BOOK_KEY = "disableRecipeBook";

    /** Beta 1.8 Pre-release raised the clouds and replaced the item model. */
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";

    /** The declared profile that already has the 3D spinning item; the one before it is the last 2D one. */
    private static final String FIRST_THREE_D_ITEM_BUILD = "historical_experience:release_1_4";

    private HistoricalWorldTweaks() {
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
            // Best effort.
        }
    }

    private static boolean earlierThan(HistoricalVersion version, String build) {
        if (version == null) {
            return false;
        }
        return HistoricalVersions.byId(build)
            .map(boundary -> version.chronology() < boundary.chronology())
            .orElse(false);
    }

    // ---- Cloud height: every declared version is before Beta 1.8.1 ------------------------------------------

    public static boolean availableForCloudHeight(HistoricalVersion version) {
        return earlierThan(version, FIRST_NEW_LIGHTING_BUILD);
    }

    /** Whether clouds sit at the old height of 108 for the client's current version. */
    public static boolean oldCloudHeight() {
        return availableForCloudHeight(HistoricalClientRules.activeVersion()) && read(CLOUD_KEY, true);
    }

    public static boolean setOldCloudHeight(boolean enabled) {
        if (enabled && !availableForCloudHeight(HistoricalClientRules.activeVersion())) {
            return false;
        }
        write(CLOUD_KEY, enabled);
        return true;
    }

    // ---- 2D dropped items: every declared version before Release 1.4 --------------------------------------

    public static boolean availableFor2dItems(HistoricalVersion version) {
        return earlierThan(version, FIRST_THREE_D_ITEM_BUILD);
    }

    /** Whether dropped items are drawn as flat sprites rather than the 3D spinning model. */
    public static boolean old2dItems() {
        return availableFor2dItems(HistoricalClientRules.activeVersion()) && read(ITEM_2D_KEY, true);
    }

    public static boolean setOld2dItems(boolean enabled) {
        if (enabled && !availableFor2dItems(HistoricalClientRules.activeVersion())) {
            return false;
        }
        write(ITEM_2D_KEY, enabled);
        return true;
    }

    // ---- Recipe book: added in 1.12, so absent from every declared version ---------------------------------

    /** The recipe book is later than every declared version, so it is offered for none of them. */
    public static boolean availableForRecipeBook(HistoricalVersion version) {
        return false;
    }

    /** Whether the recipe book is hidden. Offered nowhere, so this is decided by the version alone. */
    public static boolean recipeBookDisabled() {
        return HistoricalClientRules.activeVersion() != null && read(RECIPE_BOOK_KEY, true);
    }

    public static boolean setRecipeBookDisabled(boolean enabled) {
        write(RECIPE_BOOK_KEY, enabled);
        return true;
    }
}

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
    private static final String SKY_KEY = "oldUniversalSky";

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

    // ---- Universal sky colour: which flat colour the era's sky used ---------------------------------------

    /**
     * The old uniform sky colours, ported from Nostalgic Tweaks {@code SkyHelper.getOldColor} and its
     * {@code SkyColor} enum (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Upstream exposes these as a menu of five eras;
     * here the WORLD'S VERSION picks the era, which is the owner's instruction: alpha sky on alpha versions, beta
     * sky on beta versions below 1.8, the modern (unmodified) sky from the releases.
     *
     * <p>The two earliest values are carried for completeness even though no declared version is that old - the
     * oldest is Infdev 20100420, which is after Classic and after most of Infdev:
     * <ul>
     *   <li>{@code 0xA6D1FE} Classic, from the enum's own note.</li>
     *   <li>{@code 0xC6DEFF} Infdev, from the enum's own note.</li>
     *   <li>{@code 0x8BBDFF} Alpha, which is the value for every declared alpha build.</li>
     *   <li>{@code 0x97A3FF} Beta. The enum notes that the beta sky actually <i>"changed depending on the
     *       temperature of the biome"</i>, and this flat colour is the value upstream substitutes for it; it applies
     *       to every declared beta build below Beta 1.8.</li>
     * </ul>
     *
     * <p>No release profile gets a substitute: the owner's rule is that Release 1.0.0 onwards uses the modern sky,
     * which is what the game already draws.
     */
    private static final int SKY_CLASSIC = 0xA6D1FE;
    private static final int SKY_INFDEV = 0xC6DEFF;
    private static final int SKY_ALPHA = 0x8BBDFF;
    private static final int SKY_BETA = 0x97A3FF;

    /** The Nether's old sky: from {@code SkyHelper.getOldColor}'s nether branch. Public so a test can pin it. */
    public static final int NETHER_SKY_COLOUR = 0x210505;

    /** True while a beta-era sky should be substituted, which is every beta build below Beta 1.8. */
    private static final String FIRST_BETA_BUILD = "historical_experience:beta_1_0";

    /** The first build whose sky is the modern one: the releases, and Beta 1.8.1 onwards. */
    private static final String FIRST_RELEASE_BUILD = "historical_experience:release_1_0_0";

    /**
     * The era's flat sky colour for this version, or {@code null} when the game's own sky colour should be kept.
     *
     * <p>Pure and version-keyed so the boundary can be tested without a client. The decision uses the declared
     * CHRONOLOGY rather than name matching, so a profile is placed by when it is and not by what it is called - the
     * registry's own ordering is already the thing every other gate trusts.
     *
     * <p>The owner's rule, applied in order: <b>alpha versions get the alpha sky, beta versions get the beta sky,
     * and the releases get the modern sky.</b> Beta 1.8 and the Beta 1.9 prereleases are beta by name but already
     * have the rewritten sky, so they are placed with the releases - which is the same boundary the cloud height,
     * the lighting engine and the shading all use.
     */
    public static Integer eraSkyColour(HistoricalVersion version) {
        if (version == null) {
            return null;
        }
        HistoricalVersion infdev = HistoricalVersions.byId("historical_experience:infdev_420").orElse(null);
        HistoricalVersion alpha = HistoricalVersions.byId("historical_experience:alpha_1_0_1").orElse(null);
        HistoricalVersion beta = HistoricalVersions.byId(FIRST_BETA_BUILD).orElse(null);
        HistoricalVersion newLighting = HistoricalVersions.byId(FIRST_NEW_LIGHTING_BUILD).orElse(null);
        if (infdev == null || alpha == null || beta == null || newLighting == null) {
            // A registry that cannot resolve its own boundary constants must not guess a colour.
            return null;
        }
        int when = version.chronology();
        if (when < infdev.chronology()) {
            return SKY_CLASSIC;
        }
        if (when < alpha.chronology()) {
            return SKY_INFDEV;
        }
        if (when < beta.chronology()) {
            return SKY_ALPHA;
        }
        if (when < newLighting.chronology()) {
            return SKY_BETA;
        }
        return null;
    }

    /** The colour the sky should be drawn in for the client's current version, or {@code null} to leave it alone. */
    public static Integer skyColourOverride() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        if (!availableForSkyColour(version) || !read(SKY_KEY, true)) {
            return null;
        }
        return eraSkyColour(version);
    }

    /** Whether this version has an era sky colour at all, which is every build before Beta 1.8.1. */
    public static boolean availableForSkyColour(HistoricalVersion version) {
        return eraSkyColour(version) != null;
    }

    public static boolean setOldSkyColour(boolean enabled) {
        if (enabled && !availableForSkyColour(HistoricalClientRules.activeVersion())) {
            return false;
        }
        write(SKY_KEY, enabled);
        return true;
    }

    /**
     * The Nether's old sky colour: {@code 0x210505}, from {@code SkyHelper.getOldColor}'s nether branch, or
     * {@code null} outside the window.
     *
     * <p>The Nether arrived in Alpha v1.2.0, so it has no sky in the declared Infdev builds and its colour belongs
     * to the alpha, beta and early-release ones.
     */
    public static Integer netherSkyColour() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        if (!availableForSkyColour(version) || !read(SKY_KEY, true)) {
            return null;
        }
        return NETHER_SKY_COLOUR;
    }
}

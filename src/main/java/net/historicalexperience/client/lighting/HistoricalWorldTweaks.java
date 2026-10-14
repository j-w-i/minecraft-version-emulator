/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.client.lighting;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * The sky, cloud and item-rendering toggles from the owner's list, with the boundary each one actually has.
 *
 * <h2>Boundaries that are ESTABLISHED, with the quotation they rest on</h2>
 *
 * <ul>
 *   <li><b>Cloud height</b> — historical profiles use 108 before Beta 1.8 and 128 from Beta 1.8 onward; ordinary
 *       worlds retain modern 192. minecraft.wiki, Cloud history: <i>"Added clouds, which float westward between layers
 *       108 and 112"</i> and Beta 1.8 <i>"Now clouds float between layers 128 and 132."</i> Minecraft's NaN
 *       no-cloud sentinel is preserved.</li>
 *   <li><b>2D dropped items</b> — through the Release 1.4 profile (the published 1.4.2 update).
 *       Item (entity) history dates the fancy 3D model to 12w50a, released as 1.4.6. This class
 *       currently provides the gate only, not the renderer implementation. <i>"When
 *       dropped, items now render in the fancy graphics setting as 3D spinning animations (non-block items no longer
 *       display as sprites)."</i></li>
 *   <li><b>Recipe book button</b> — the modern button is hidden for historical worlds by porting Nostalgic Tweaks'
 *       {@code InventoryScreenHelper.setRecipeButton}; the recipe panel alone did not hide its separate screen button.</li>
 * </ul>
 *
 * <h2>Related effects with separate implementations or remaining research gaps</h2>
 *
 * <ul>
 *   <li><b>Stars.</b> HistoricalStars owns NT's geometry/brightness styles, with Beta1.5 and Release1.3.1
 *       transitions; exact old-client pixel comparison remains pending.</li>
 *   <li><b>Nether fog.</b> The colour is known from Alpha v1.2.0 — <i>"The fog color in the Nether is #330808"</i> —
 *       but not when it changed or how dense it was.</li>
 *   <li>Left-click activation is owned by HistoricalLeftClickEvents, ending at the owner's Release 1.4.2 boundary.</li>
 * </ul>
 *
 * <h2>A finding that CORRECTS the list it came from</h2>
 *
 * <p>The blue void DOES apply to the declared Beta 1.8+ and release profiles. Void history dates the deeper
 * blue to Beta 1.8 and its removal to 18w01a (1.13). The earlier claim that no declared version had it was wrong.
 * Its NT renderer port remains pending.
 */
public final class HistoricalWorldTweaks {
    private static final String ITEM_2D_KEY = "old2dItems";
    private static final String RECIPE_BOOK_KEY = "disableRecipeBook";
    private static final String SKY_KEY = "oldUniversalSky";


    /** The Beta 1.8-era boundary at which the cloud height rose from 108 to 128. */
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";

    /** The declared profile that already has the 3D spinning item; the one before it is the last 2D one. */
    private static final String FIRST_THREE_D_ITEM_BUILD = "historical_experience:release_1_5";

    private HistoricalWorldTweaks() {
    }

    private static boolean read(String key, boolean fallback) {
        return HistoricalClientOptions.get(key, fallback);
    }

    private static void write(String key, boolean value) {
        HistoricalClientOptions.set(key, value);
    }

    private static boolean earlierThan(HistoricalVersion version, String build) {
        if (version == null) {
            return false;
        }
        return HistoricalVersions.byId(build)
            .map(boundary -> version.chronology() < boundary.chronology())
            .orElse(false);
    }

    // ---- Cloud height: 108 before Beta 1.8, then 128 throughout the historical catalogue ------------------

    public static boolean availableForCloudHeight(HistoricalVersion version) {
        return earlierThan(version, FIRST_NEW_LIGHTING_BUILD);
    }

    /**
     * Cloud height for a historical world: 108 before Beta 1.8, 128 from Beta 1.8 through the declared releases.
     * The modern 192 height is kept only for ordinary worlds. NaN is Minecraft's no-cloud sentinel and is preserved.
     */
    public static float cloudHeightOverride(float original) {
        return cloudHeightForVersion(original, HistoricalClientRules.activeVersion());
    }

    /** Pure version mapping used by the client hook and boundary tests. */
    public static float cloudHeightForVersion(float original, HistoricalVersion version) {
        if (Float.isNaN(original) || version == null) {
            return original;
        }
        return earlierThan(version, FIRST_NEW_LIGHTING_BUILD) ? 108.0F : 128.0F;
    }

    // ---- 2D dropped items: through Release 1.4; the 3D model arrived during 1.4.x -------------------------

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

    /** The recipe book UI postdates every declared version, so its button is hidden in all historical profiles. */
    public static boolean availableForRecipeBook(HistoricalVersion version) {
        return false;
    }

    /** Pure gate shared with tests: ordinary worlds (null version) are never changed. */
    public static boolean shouldHideRecipeBookButton(HistoricalVersion version, boolean enabled) {
        return version != null && enabled;
    }

    /** Whether the separate recipe-book screen button should be hidden for the active world. */
    public static boolean recipeBookDisabled() {
        return shouldHideRecipeBookButton(HistoricalClientRules.activeVersion(), read(RECIPE_BOOK_KEY, true));
    }

    public static boolean setRecipeBookDisabled(boolean enabled) {
        write(RECIPE_BOOK_KEY, enabled);
        return true;
    }

    // ---- Universal sky colour: which colour the era's sky actually used -----------------------------------

    /** How a declared profile's sky was produced by the client of its own era. */
    public enum SkyEra {
        /** The era already draws the right sky, so nothing is substituted: the releases, and Beta 1.9 onward. */
        NONE,
        /** A flat sky colour read out of that era's client: Infdev and Alpha v1.0.x. */
        FLAT,
        /**
         * The Beta sky was computed from the world's temperature field, not chosen from a palette: Beta 1.0 through
         * Beta 1.8.1 inclusive. See {@link HistoricalSkyColours#temperatureSkyColour(float)}.
         */
        TEMPERATURE
    }

    /** The last declared profile whose sky came from the temperature field, and the first that did not. */
    private static final String FIRST_BETA_BUILD = "historical_experience:beta_1_0";
    private static final String FIRST_BIOME_TEMPERATURE_SKY_BUILD = "historical_experience:beta_1_9_pre_1";

    /**
     * Which sky the era used.
     *
     * <p>This is measured from the clients themselves, not from Nostalgic Tweaks' era menu, because that menu's Beta
     * entry ({@code 0x97A3FF}) is not the Beta sky at all. The boundaries:
     *
     * <ul>
     *   <li><b>Infdev</b> and <b>Alpha v1.0.x</b>: a flat colour. Alpha v1.0.11's world object carries the sky colour
     *       as a field ({@code cj.java:20}, {@code 8961023 == 0x8BBDFF}); see {@link HistoricalSkyColours}.</li>
     *   <li><b>Beta 1.0 - Beta 1.8.1</b>: the sky getter takes the temperature at the player's position and calls
     *       {@code getSkyColorByTemp}. Beta 1.0 ({@code dd.java:952}), Beta 1.7.3 ({@code fd.java:927}) and Beta
     *       1.8.1 ({@code rv.java:997}) are all this shape, and the temperature is the world's own noise field
     *       ({@code xv.java:30} in Beta 1.7.3) rather than a per-biome constant.</li>
     *   <li><b>Beta 1.9 prereleases onward</b>: the same HSV function, but fed the biome's temperature at height 64
     *       ({@code uy.java:1015} in Beta 1.9 Pre-release 6, {@code ry.java:1015} in Release 1.0) - which is what
     *       modern Minecraft still does, so the game's own value is already right and nothing is substituted. This is
     *       the boundary the owner described as "Beta until beta 1.7.3 and then modern", moved to where the clients
     *       actually changed.</li>
     * </ul>
     *
     * <p>Alpha v1.1.x and v1.2.x are a known gap: those clients also fed a temperature field into the same function
     * ({@code cy.java:948} in Alpha v1.2.6), but the Alpha worlds here are generated by Moderner Beta, which exposes
     * no Alpha sky-temperature sampler, so there is nothing faithful to feed it with. Those profiles keep the flat
     * Alpha v1.0.x colour, which is the value Moderner Beta assigns its Alpha biomes.
     */
    public static SkyEra skyEra(HistoricalVersion version) {
        if (version == null) {
            return SkyEra.NONE;
        }
        HistoricalVersion infdev = HistoricalVersions.byId("historical_experience:infdev_420").orElse(null);
        HistoricalVersion beta = HistoricalVersions.byId(FIRST_BETA_BUILD).orElse(null);
        HistoricalVersion biomeTemperature = HistoricalVersions.byId(FIRST_BIOME_TEMPERATURE_SKY_BUILD).orElse(null);
        if (infdev == null || beta == null || biomeTemperature == null) {
            // A registry that cannot resolve its own boundary constants must not guess a colour.
            return SkyEra.NONE;
        }
        int when = version.chronology();
        if (when < infdev.chronology() || when >= biomeTemperature.chronology()) {
            // Nothing is declared before Infdev, and Beta 1.9 onward already draws its own correct sky.
            return SkyEra.NONE;
        }
        if (when < beta.chronology()) {
            // Only the two eras with an extracted flat colour are claimed; a Classic or Indev profile would need a
            // value this project has not extracted, so it is left to Moderner Beta's own biome colours.
            return switch (version.era()) {
                case "infdev", "alpha" -> SkyEra.FLAT;
                default -> SkyEra.NONE;
            };
        }
        return SkyEra.TEMPERATURE;
    }

    /**
     * The flat sky colour for an era that used one, or {@code null} when the era's sky was not a flat colour or
     * nothing should be substituted.
     */
    public static Integer eraSkyColour(HistoricalVersion version) {
        if (skyEra(version) != SkyEra.FLAT) {
            return null;
        }
        return "infdev".equals(version.era()) ? HistoricalSkyColours.INFDEV : HistoricalSkyColours.ALPHA;
    }

    /** The colour the sky should be drawn in for the client's current version, or {@code null} to leave it alone. */
    public static Integer skyColourOverride() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        if (!availableForSkyColour(version) || !read(SKY_KEY, true)) {
            return null;
        }
        return eraSkyColour(version);
    }

    /**
     * Whether the sky option is on and this version has an era sky at all, which is what the client hook checks before
     * deciding between a flat colour and the era's temperature function.
     */
    public static boolean skyTweakActive(HistoricalVersion version) {
        return availableForSkyColour(version) && read(SKY_KEY, true);
    }

    /** Whether the sky option does anything for this version, which is every declared build below Beta 1.9. */
    public static boolean availableForSkyColour(HistoricalVersion version) {
        return skyEra(version) != SkyEra.NONE;
    }

    public static boolean setOldSkyColour(boolean enabled) {
        if (enabled && !availableForSkyColour(HistoricalClientRules.activeVersion())) {
            return false;
        }
        write(SKY_KEY, enabled);
        return true;
    }

}

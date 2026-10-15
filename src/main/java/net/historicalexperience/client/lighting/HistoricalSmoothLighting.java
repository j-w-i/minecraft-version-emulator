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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * The Beta 1.3–1.7.3 smooth lighting engine, which Beta 1.8 replaced, and its toggle.
 *
 * <h2>What "old smooth lighting" actually is</h2>
 *
 * <p>Not a different lighting model - the two engines differ in two specific places:
 *
 * <ol>
 *   <li><b>Corner shading.</b> The modern engine darkens a corner towards 0.2 in {@code getShadeBrightness}; from
 *       Beta 1.3 to Beta 1.7.3 it did not darken the corner at all. That is the rule ported from Nostalgic Tweaks'
 *       {@code BlockBehaviourMixin}, and it is the half this class gates.</li>
 *   <li><b>The lightmap.</b> The brightness curve per light level is the other half. It is recorded as a known gap
 *       rather than claimed: the corner shading applies and the curve does not.</li>
 * </ol>
 *
 * <h2>Sources for the boundary</h2>
 *
 * <p>minecraft.wiki, Light, History / Java Edition:
 *
 * <ul>
 *   <li><i>"1.3 — Smooth lighting engine added with the help of MrMessiah and his 'BetterLight' mod."</i></li>
 *   <li><i>"1.8 Pre-release — Implemented a new lighting engine. The lighting on a block is given a tint based on
 *       the most prominent source of light."</i></li>
 * </ul>
 *
 * <p>So the window is <b>Beta 1.3 inclusive to Beta 1.8 exclusive</b>, and {@link #availableFor} is written as two
 * comparisons rather than one boundary for that reason - a single "older than Beta 1.8" test would also cover every
 * version before Beta 1.3, where there was no smooth lighting to shape at all. Before Beta 1.3 the game's own
 * smooth-lighting setting is answered false instead, by {@code MinecraftHistoricalSmoothLightingMixin}, because
 * there is no engine to choose between.
 *
 * <h2>Why this class has a toggle as well as a gate</h2>
 *
 * <p>Until the owner asked whether it could be switched, this was gated by <b>version alone</b>: a Beta 1.7.3 world
 * always rendered with the old corner shading and there was no way to turn it off, where upstream offers
 * {@code OLD_SMOOTH_LIGHTING} as a setting defaulting to true. The version still decides whether the engine is
 * <i>offered</i>; the toggle decides whether it is <i>used</i>.
 */
public final class HistoricalSmoothLighting {
    /** The build that introduced the smooth lighting engine, so the window opens here. */
    private static final String FIRST_SMOOTH_LIGHTING_BUILD = "historical_experience:beta_1_3";

    /** The build that replaced it, so the window closes before this one. */
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";

    /** Shares the client options file with the classic engine's toggle; a different key. */
    private static final String TOGGLE_FILE = "historical_experience-client.properties";
    private static final String TOGGLE_KEY = "oldSmoothLighting";

    private static volatile Boolean toggle;

    private HistoricalSmoothLighting() {
    }

    /**
     * Whether the 1.3-1.7.3 engine is on offer for this version.
     *
     * <p>A null version is unknown, and unknown is not "yes": a client that guessed would render one thing while the
     * server believed another.
     */
    public static boolean availableFor(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        var first = HistoricalVersions.byId(FIRST_SMOOTH_LIGHTING_BUILD).orElse(null);
        var replaced = HistoricalVersions.byId(FIRST_NEW_LIGHTING_BUILD).orElse(null);
        if (first == null || replaced == null) {
            // A registry that cannot resolve its own boundary constants must not guess a window.
            return false;
        }
        return version.chronology() >= first.chronology() && version.chronology() < replaced.chronology();
    }

    /**
     * Whether the old engine should actually render: offered for this version AND switched on.
     *
     * <p>This is what the mixin asks. The two questions are kept apart on purpose - folding them together would make
     * "this version never had this engine" and "the player turned it off" the same answer, and the config screen has
     * to tell those apart to explain itself.
     */
    public static boolean activeNow() {
        return availableFor(HistoricalClientRules.activeVersion()) && toggle();
    }

    /**
     * The toggle, loaded once and defaulted to ON.
     *
     * <p>Default on because inside this window the old engine is not a preference, it is what the build did - the
     * same reason upstream defaults its flag to true. The toggle exists so a player can opt OUT, not so they must opt
     * in.
     */
    public static boolean toggle() {
        Boolean current = toggle;
        if (current == null) {
            current = load();
            toggle = current;
        }
        return current;
    }

    /**
     * Sets the toggle and persists it, always.
     *
     * <h2>Why this is not refused when the current world does not offer the engine</h2>
     *
     * <p>It used to be: {@code setToggle(true)} returned false unless a world inside the Beta 1.3-1.7.3 window was
     * open, on the reasoning that "a value stored in one world cannot silently start applying in another". The owner
     * reported the consequence - turning the option on or off appeared to do nothing, and reopening the world did not
     * help either - because the place a player most often reaches this screen is Mod Menu <em>without</em> a world
     * open, where the write was discarded. The reasoning was also unnecessary: whether the engine may apply is
     * decided by {@link #activeNow()} for the world being played, so a stored preference can only ever take effect
     * inside the window it belongs to.
     *
     * <p>Corner shading is read while a chunk is <em>meshed</em>, so a change is invisible until chunks are rebuilt.
     * The relight queue is therefore nudged here: without it the option would look inert until the player happened to
     * cause a chunk update or reload the world.
     */
    public static void setToggle(boolean enabled) {
        boolean changed = enabled != toggle();
        toggle = enabled;
        save(enabled);
        if (changed) {
            // The same queue the old-lighting engine drains a few sections per tick. It is drained whenever a world
            // is open, so a change made at the title screen simply waits until one is.
            HistoricalOldLighting.requestRelight();
        }
    }

    /** Forgets the cached value, so the next world re-reads the config. */
    public static void resetCache() {
        toggle = null;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(TOGGLE_FILE);
    }

    private static boolean load() {
        try {
            Path path = configPath();
            if (!Files.isRegularFile(path)) {
                return true;
            }
            Properties properties = new Properties();
            try (var in = Files.newInputStream(path)) {
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
                try (var in = Files.newInputStream(path)) {
                    properties.load(in);
                }
            }
            properties.setProperty(TOGGLE_KEY, Boolean.toString(enabled));
            try (var out = Files.newOutputStream(path)) {
                properties.store(out, "Historical Experience client options");
            }
        } catch (IOException | RuntimeException failure) {
            // Best effort: the in-memory value is already set and this run renders correctly.
        }
    }
}

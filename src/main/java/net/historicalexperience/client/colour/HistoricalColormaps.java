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
package net.historicalexperience.client.colour;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The original clients' grass and foliage tint maps, extracted from their own {@code misc/*color.png} and applied by
 * the client instead of Moderner Beta's own colouring.
 *
 * <h2>Why this exists</h2>
 *
 * <p>Moderner Beta picks the biome tint for grass and leaves from its own colour tables (its
 * {@code GrassBlockTintSource}/{@code FoliageTintSource} call {@code GrassColor.get(...)}/{@code FoliageColor.get(...)}
 * with its climate samples). Those tables are its approximations. This class replaces the table itself, so every path
 * that uses it follows the era's own values without any per-block rule: the game loads its colourmap, and this swaps the
 * result for the era's.
 *
 * <h2>What was extracted, and from where</h2>
 *
 * <p>Each original client from Alpha v1.2.0 stores the lookup as a 256x256 {@code misc/grasscolor.png} /
 * {@code misc/foliagecolor.png} which it reads into a 65536-entry array and indexes with
 * {@code ((int) ((1.0 - rain * temp) * 255.0) << 8) | (int) ((1.0 - temp) * 255.0)}. The values were pulled out of the
 * jars and are stored, gzipped and unchanged, under {@code assets/historical_experience/colormap/}; that directory's
 * {@code README.txt} records the source jar hashes and the hash of every table. No original image is included.
 *
 * <p>Measured across the available clients:
 *
 * <ul>
 *   <li>the <b>grass</b> map is identical in every version from Alpha v1.2.6 through Release 1.0;</li>
 *   <li>the <b>foliage</b> map changes exactly once in that range: Alpha v1.2.6 and Beta 1.0 share the older map, and
 *       Beta 1.2_02 through Release 1.0 share the newer one.</li>
 * </ul>
 *
 * <h2>What is deliberately not done</h2>
 *
 * <p>Before Alpha v1.2.0 there was no colour map at all - the tint was literal colour values in the renderer - and no
 * profile this project declares sits in that range, so no value is invented for it and the game's own colouring is
 * left alone there. Recovering those literals is an open task, recorded in the research notes.
 */
public final class HistoricalColormaps {
    /** Fallback when the era's table cannot be read: the game's own is kept, which is the safe direction. */
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/colormaps");

    /** First declared profile after Alpha 1.2.x: the newer foliage map applies from here. */
    private static final int FOLIAGE_CHANGE_CHRONOLOGY =
        HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 2, 0);

    /** The first version whose client actually consulted a colour map. */
    private static final int COLOURMAP_START_CHRONOLOGY =
        HistoricalVersions.chronology(HistoricalVersions.ERA_ALPHA, 1, 2, 0);

    private static final String GRASS = "assets/historical_experience/colormap/grass_alpha_1_2_to_release_1_0.bin.gz";
    private static final String FOLIAGE_OLD =
        "assets/historical_experience/colormap/foliage_alpha_1_2_to_beta_1_0.bin.gz";
    private static final String FOLIAGE_NEW =
        "assets/historical_experience/colormap/foliage_beta_1_2_to_release_1_0.bin.gz";

    private static volatile int[] grass;
    private static volatile int[] foliageOld;
    private static volatile int[] foliageNew;
    private static volatile boolean loaded;
    private static volatile String applied = "the game's own colour maps";

    private HistoricalColormaps() {
    }

    /** Which colour map names are currently being substituted, for diagnostics and the probe. */
    public static String appliedDescription() {
        return applied;
    }

    /**
     * The grass table for the active version, or {@code null} when the game's own table should stand.
     *
     * @param chronology the active version's chronology key
     */
    public static int[] grassFor(int chronology) {
        if (chronology < COLOURMAP_START_CHRONOLOGY) {
            return null;
        }
        return ensureLoaded() ? grass : null;
    }

    /** The foliage table for the active version, or {@code null} when the game's own table should stand. */
    public static int[] foliageFor(int chronology) {
        if (chronology < COLOURMAP_START_CHRONOLOGY) {
            return null;
        }
        if (!ensureLoaded()) {
            return null;
        }
        return chronology < FOLIAGE_CHANGE_CHRONOLOGY ? foliageOld : foliageNew;
    }

    /** The table the game should use right now for {@code kind}, or {@code null} to keep what it loaded. */
    public static int[] current(String kind) {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        if (version == null) {
            return null;
        }
        return "foliage".equals(kind) ? foliageFor(version.chronology()) : grassFor(version.chronology());
    }

    private static boolean ensureLoaded() {
        if (loaded) {
            return grass != null;
        }
        synchronized (HistoricalColormaps.class) {
            if (loaded) {
                return grass != null;
            }
            try {
                grass = read(GRASS);
                foliageOld = read(FOLIAGE_OLD);
                foliageNew = read(FOLIAGE_NEW);
                LOGGER.info("Loaded the original clients' colour maps: grass={} foliage(old)={} foliage(new)={}",
                    grass != null, foliageOld != null, foliageNew != null);
            } catch (IOException | RuntimeException failure) {
                // Leaving the game's own tables in place is the safe failure: a missing asset must not break rendering.
                LOGGER.error("Could not read the extracted colour maps; the game's own will be used", failure);
                grass = null;
            }
            loaded = true;
            return grass != null;
        }
    }

    private static int[] read(String resource) throws IOException {
        try (InputStream raw = HistoricalColormaps.class.getClassLoader().getResourceAsStream(resource)) {
            if (raw == null) {
                return null;
            }
            try (GZIPInputStream gzip = new GZIPInputStream(raw)) {
                byte[] bytes = gzip.readAllBytes();
                if (bytes.length != 65536 * 3) {
                    throw new IOException(resource + " holds " + bytes.length + " bytes, expected " + (65536 * 3));
                }
                int[] pixels = new int[65536];
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = (bytes[i * 3] & 0xFF) << 16 | (bytes[i * 3 + 1] & 0xFF) << 8 | (bytes[i * 3 + 2] & 0xFF);
                }
                return pixels;
            }
        }
    }

    /** Records what the last reload installed, for the probe and the logs. */
    public static void noteApplied(String description) {
        String previous = applied;
        applied = description;
        if (!description.equals(previous)) {
            LOGGER.info("Historical colour maps installed: {} (was {})", description, previous);
        }
    }

    /** What the last install actually put in place, so a mismatch can be noticed without re-reading the tables. */
    public static String appliedEra() {
        return applied;
    }

    /**
     * A stable name for the tint tables this chronology needs: {@code none}, or the maps by name. Whole ranges of
     * versions share a name, which is what keeps a version change from forcing a reload when nothing visible changed.
     */
    public static String eraSignatureFor(int chronology) {
        if (chronology < COLOURMAP_START_CHRONOLOGY || !ensureLoaded() || grass == null) {
            return "none";
        }
        return chronology < FOLIAGE_CHANGE_CHRONOLOGY ? "grass+foliage_old" : "grass+foliage_new";
    }

    /** The signature for the client's current version, or {@code none} when no historical world is active. */
    public static String desiredEraSignature() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        return version == null ? "none" : eraSignatureFor(version.chronology());
    }

    /** What {@code eraFor} should record given the table a hook just resolved; null means the game's own is kept. */
    public static String eraFor(int[] resolved) {
        return resolved == null ? "none" : desiredEraSignature();
    }
}

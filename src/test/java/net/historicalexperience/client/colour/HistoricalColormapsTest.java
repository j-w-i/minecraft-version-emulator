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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The colour maps extracted from the original clients: that they load, that they are the era's, and that two versions
 * which shared a map in the real clients share one here (so no reload is asked for between them).
 */
class HistoricalColormapsTest {
    private static int at(String id) {
        return HistoricalVersions.byId("historical_experience:" + id).orElseThrow().chronology();
    }

    @Test
    @DisplayName("the extracted maps load, and hold a full 65536-entry table each")
    void mapsLoad() {
        int[] grass = HistoricalColormaps.grassFor(at("beta_1_7_3"));
        assertNotNull(grass, "the grass map must load from the bundled extracted data");
        assertEquals(65536, grass.length);
        int[] foliage = HistoricalColormaps.foliageFor(at("beta_1_7_3"));
        assertNotNull(foliage);
        assertEquals(65536, foliage.length);
    }

    @Test
    @DisplayName("the published cell values match what the original client's file holds at those indices")
    void valuesMatchTheOriginal() {
        // Values read from the extracted Release 1.0 misc/grasscolor.png. The client indexes the table with
        //   index = ((int) ((1.0 - rain * temp) * 255.0) << 8) | (int) ((1.0 - temp) * 255.0)
        // where temp and rain are already the 0..1 values it sampled, which is what index() below reproduces.
        int[] grass = HistoricalColormaps.grassFor(at("release_1_0_0"));
        assertNotNull(grass);
        assertEquals(0x80B497, grass[index(0.0, 0.0)] & 0xFFFFFF, "cold and dry");
        assertEquals(0x8EB971, grass[index(0.5, 0.5)] & 0xFFFFFF, "temperate");
        assertEquals(0x47CD33, grass[index(1.0, 1.0)] & 0xFFFFFF, "hot and wet");
        // And the foliage map is genuinely a different table, not the same one under another name. The two foliage
        // maps differ by a little at these cells (Release 1.0 holds 60A17B/1ABF00 where Beta 1.0 holds 60A17C/19BF00),
        // so these assertions would also catch the two generations being swapped.
        int[] foliage = HistoricalColormaps.foliageFor(at("release_1_0_0"));
        assertEquals(0x60A17B, foliage[index(0.0, 0.0)] & 0xFFFFFF, "cold and dry, foliage at Release 1.0");
        assertEquals(0x71A74D, foliage[index(0.5, 0.5)] & 0xFFFFFF, "temperate, foliage");
        assertEquals(0x1ABF00, foliage[index(1.0, 1.0)] & 0xFFFFFF, "hot and wet, foliage");
        int[] oldFoliage = HistoricalColormaps.foliageFor(at("beta_1_0"));
        assertEquals(0x60A17C, oldFoliage[index(0.0, 0.0)] & 0xFFFFFF, "the older map's cold and dry value");
        assertEquals(0x19BF00, oldFoliage[index(1.0, 1.0)] & 0xFFFFFF, "the older map's hot and wet value");
        assertNotSame(grass, foliage);
    }

    private static int index(double temp, double rain) {
        double temperature = Math.max(0.0, Math.min(1.0, temp));
        double rainfall = Math.max(0.0, Math.min(1.0, rain));
        return ((int) ((1.0 - rainfall * temperature) * 255.0) << 8)
            | (int) ((1.0 - temperature) * 255.0);
    }

    @Test
    @DisplayName("foliage uses one map through Beta 1.0 and the other from Beta 1.2, exactly where the clients changed")
    void foliageChangesWhereTheClientsDid() {
        int[] old = HistoricalColormaps.foliageFor(at("beta_1_0"));
        int[] alpha2 = HistoricalColormaps.foliageFor(at("alpha_1_2_0"));
        int[] newer = HistoricalColormaps.foliageFor(at("beta_1_2"));
        int[] release = HistoricalColormaps.foliageFor(at("release_1_0_0"));
        assertNotNull(old);
        assertNotNull(newer);
        assertSame(old, alpha2, "Alpha 1.2 and Beta 1.0 shipped the same foliage map");
        assertSame(newer, release, "Beta 1.2 through Release 1.0 shipped the same foliage map");
        assertNotSame(old, newer, "the foliage map did change between Beta 1.0 and Beta 1.2");
    }

    @Test
    @DisplayName("the grass map is the same table for every version, which is what the clients shipped")
    void grassNeverChanges() {
        int[] release = HistoricalColormaps.grassFor(at("release_1_0_0"));
        for (String id : new String[] {"alpha_1_2_0", "beta_1_0", "beta_1_7_3", "beta_1_8_1", "beta_1_9_pre_6"}) {
            assertSame(release, HistoricalColormaps.grassFor(at(id)), id);
        }
    }

    @Test
    @DisplayName("no map is substituted before colour maps existed, and the era signature is stable within a range")
    void erasAndBoundaries() {
        assertNull(HistoricalColormaps.foliageFor(at("alpha_1_0_1")),
            "Alpha 1.0 predates colour maps; nothing is invented for it");
        assertEquals("none", HistoricalColormaps.eraSignatureFor(0));
        assertEquals("grass+foliage_old", HistoricalColormaps.eraSignatureFor(at("alpha_1_2_0")));
        assertEquals("grass+foliage_old", HistoricalColormaps.eraSignatureFor(at("beta_1_0")));
        assertEquals("grass+foliage_new", HistoricalColormaps.eraSignatureFor(at("beta_1_2")));
        assertEquals("grass+foliage_new", HistoricalColormaps.eraSignatureFor(at("release_1_0_0")));
        assertEquals("grass+foliage_new", HistoricalColormaps.eraSignatureFor(at("release_1_6_4")));
        // Two adjacent versions inside one range share a signature, so a version change between them reloads nothing.
        assertEquals(HistoricalColormaps.eraSignatureFor(at("beta_1_7_3")),
            HistoricalColormaps.eraSignatureFor(at("beta_1_8_1")));
    }
}

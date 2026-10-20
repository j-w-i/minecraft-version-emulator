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
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Audit G8 — minecraft.wiki, Java Edition 1.2.1 (12w07a): the build limit was raised from 128 to 256.
 * The project's Release presets all used the 128-height dimension type, so a Release 1.2.5-1.6.4 world could not be
 * built above y=127 even though the build had 256.
 *
 * <p>The fix is an appended generation revision (revisions are append-only; a new world selects the newest), which
 * keeps the version's own terrain foundation and ore population and only changes the engine-enforced dimension type.
 */
class ReleaseHeightTest {
    private static final String P = "historical_experience:";

    @Test
    @DisplayName("the raised-limit Release profiles default to the 256-height revision")
    void raisedLimitProfiles() {
        for (String version : new String[] {"release_1_2_5", "release_1_3", "release_1_4", "release_1_5",
                "release_1_6_4"}) {
            HistoricalVersion declared = HistoricalVersions.byId(P + version).orElseThrow();
            assertTrue(declared.latestRevision().alias().endsWith("_height_256"),
                version + " should default to its height revision, was " + declared.latestRevision().alias());
            assertTrue(declared.latestRevision().foundation().startsWith("moderner_beta:"),
                "the height revision must keep the version's own foundation");
        }
    }

    @Test
    @DisplayName("Release 1.0 and 1.1 keep the 128 envelope, because they predate 12w07a")
    void earlierProfilesUnchanged() {
        for (String version : new String[] {"release_1_0_0", "release_1_1", "beta_1_8_1", "alpha_1_2_0"}) {
            HistoricalVersion declared = HistoricalVersions.byId(P + version).orElseThrow();
            assertFalse(declared.latestRevision().alias().endsWith("_height_256"),
                version + " predates the height raise");
        }
    }

    @Test
    @DisplayName("the new dimension type is 0..256 and every height preset points at it")
    void dimensionTypeAndPresets() throws Exception {
        JsonObject type = read("data/historical_experience/dimension_type/release_1_2_overworld.json");
        assertEquals(256, type.get("height").getAsInt());
        assertEquals(0, type.get("min_y").getAsInt());
        assertEquals(256, type.get("logical_height").getAsInt());

        for (String version : new String[] {"release_1_2_5", "release_1_3", "release_1_4", "release_1_5",
                "release_1_6_4"}) {
            JsonObject preset = read("data/historical_experience/worldgen/world_preset/" + version + "_height_256.json");
            String used = preset.getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld")
                .get("type").getAsString();
            assertEquals("historical_experience:release_1_2_overworld", used,
                version + " height preset must use the 256 dimension type");
        }
    }

    private static JsonObject read(String resource) throws Exception {
        try (InputStream in = ReleaseHeightTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new AssertionError("missing resource: " + resource);
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}

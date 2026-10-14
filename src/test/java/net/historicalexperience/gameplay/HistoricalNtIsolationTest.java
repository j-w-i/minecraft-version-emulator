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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HistoricalNtIsolationTest {
    @Test
    void commonLeftClickHandlerNeverReferencesClientClasses() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/net/historicalexperience/gameplay/HistoricalLeftClickEvents.java"));
        assertFalse(source.contains("import net.historicalexperience.client."));
        assertTrue(source.contains("HistoricalProfileLookup.profileIdForLevel"));
        assertTrue(source.contains("instanceof ServerLevel"));
    }

    @Test
    void clientShadingGateIsNotLoadedOnDedicatedServer() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of(
            "src/main/resources/historical_experience.mixins.json"))).getAsJsonObject();
        assertFalse(config.getAsJsonArray("mixins").toString().contains("BlockBehaviourHistoricalShadeBrightnessMixin"));
        assertTrue(config.getAsJsonArray("client").toString().contains("BlockBehaviourHistoricalShadeBrightnessMixin"));
    }

    @Test
    void classicLightmapIsActuallyRemoved() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/net/historicalexperience/client/lighting/HistoricalLightmap.java"));
        assertFalse(source.contains("applyClassic"));
    }
}

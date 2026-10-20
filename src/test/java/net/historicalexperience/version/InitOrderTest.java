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
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit, one fresh JVM per permutation.
 *
 * <p>The registry, the marker list and the profile table all derive static tables from {@code HistoricalVersions.all()}
 * the first time their class loads, and a table built from a half-initialised registry would be wrong for the whole
 * life of the process, not just fail once. Which of them loads first depends on what the game touches first, so this
 * starts a separate JVM for each order and requires the same digest from all of them. It would have caught a
 * {@code Feature} or {@code Milestone} that read the registry while initialising.
 */
class InitOrderTest {
    private static final String VERSION = "net.historicalexperience.version.";

    @Test
    @DisplayName("the same tables result whichever class is loaded first")
    void digestIsIndependentOfLoadOrder() throws Exception {
        Map<String, String> orders = new LinkedHashMap<>();
        orders.put("registry first", VERSION + "HistoricalVersions");
        orders.put("feature first", VERSION + "Feature");
        orders.put("milestones first", VERSION + "Milestones");
        orders.put("features first", VERSION + "Features");
        orders.put("session first", VERSION + "ActiveVersion");
        orders.put("marker first", "net.historicalexperience.HistoricalMarker");
        orders.put("profiles first", "net.historicalexperience.profile.HistoricalProfiles");
        orders.put("feature then registry", VERSION + "Feature," + VERSION + "HistoricalVersions");
        orders.put("marker then feature", "net.historicalexperience.HistoricalMarker," + VERSION + "Feature");

        Map<String, String> results = new LinkedHashMap<>();
        for (Map.Entry<String, String> order : orders.entrySet()) {
            results.put(order.getKey(), run(order.getValue()));
        }
        String reference = results.values().iterator().next();
        assertTrue(reference.startsWith("DIGEST "), "unexpected output: " + reference);
        for (Map.Entry<String, String> result : results.entrySet()) {
            assertEquals(reference, result.getValue(), "load order '" + result.getKey() + "' produced different tables");
        }
    }

    private static String run(String first) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(VERSION + "InitOrderProbe");
        command.add(first);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertEquals(0, process.waitFor(), "probe failed for '" + first + "':\n" + output);
        String[] lines = output.split("\\R");
        return lines[lines.length - 1];
    }
}
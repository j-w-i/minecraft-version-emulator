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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.Test;

/**
 * Prints each version's revisions and the settings preset each one names, so the biome-rename work is driven by the
 * compiled registry rather than by reading the Java source by eye. Not an assertion: its output is the deliverable.
 *
 * <p>Run with {@code ./gradlew test --tests '*RevisionInventory*' --info} and read the lines beginning {@code INV}.
 */
class RevisionInventoryTest {
    @Test
    void printRevisions() throws IOException {
        Path presets = Paths.get("src/main/resources/data/historical_experience/moderner_beta/settings_preset");
        for (HistoricalVersion version : HistoricalVersions.all()) {
            String id = version.id();
            if (id.contains("infdev") || id.contains("indev")) {
                continue;
            }
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                String alias = revision.alias();
                String file = alias.substring(alias.indexOf(':') + 1) + ".json";
                Path path = presets.resolve(file);
                String preset = "<no file>";
                if (Files.isRegularFile(path)) {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    int at = text.indexOf("\"moderner_beta:preset\"");
                    preset = at < 0 ? "<none>" : text.substring(at, Math.min(text.length(), at + 46))
                        .replaceAll("\\s+", " ");
                }
                System.out.println("INV|" + id + "|rev" + revision.revision() + "|" + alias + "|" + preset);
            }
        }
    }
}

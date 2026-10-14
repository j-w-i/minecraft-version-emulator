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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the packaged artefacts a declared version needs, which no other test compares to the registry.
 *
 * <p>Written from findings in {@code docs/ADDING-A-VERSION.md}. Two of them are silent failures - everything passes
 * and the game does the wrong thing - which is the category this project has been bitten by most:
 *
 * <ol>
 *   <li><b>The world-preset tag.</b> The create-world screen selects a version by looking for it in vanilla's own
 *       world-type list, which is populated from {@code data/minecraft/tags/worldgen/world_preset/normal.json}. A
 *       version whose preset exists but is untagged leaves every test green while the GUI keeps vanilla's world
 *       type and its button still names the historical version - a world that claims to be Beta 1.7.3 and
 *       generates modern terrain. This is the only required artefact that had no test behind it.</li>
 *   <li><b>Settings-preset content.</b> Only the FILE's existence was ever asserted, never its content. A settings
 *       preset that inherits a different upstream preset than the registry records as its foundation generates
 *       terrain that the world's own metadata misdescribes, with nothing to notice it.</li>
 * </ol>
 */
class VersionArtefactConsistencyTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path TAG = ROOT.resolve("data/minecraft/tags/worldgen/world_preset/normal.json");
    private static final Path SETTINGS =
        ROOT.resolve("data/historical_experience/moderner_beta/settings_preset");

    @Test
    void everyImplementedVersionIsSelectableAtWorldCreation() throws IOException {
        String tag = Files.readString(TAG);
        List<String> missing = new ArrayList<>();

        for (HistoricalVersion version : HistoricalVersions.all()) {
            String path = version.id().substring(version.id().indexOf(':') + 1);
            String worldPreset = "historical_experience:" + path;

            if (!tag.contains(worldPreset)) {
                missing.add(worldPreset);
            }
        }

        assertTrue(missing.isEmpty(),
            "these versions have no world preset in minecraft:normal, so the create-world screen cannot select "
                + "them and will silently keep vanilla's world type while labelling the world historical: "
                + missing);
    }

    @Test
    void everyDeclaredRevisionHasASettingsPresetInheritingItsRecordedFoundation() throws IOException {
        List<String> problems = new ArrayList<>();

        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                String path = revision.alias().substring(revision.alias().indexOf(':') + 1);
                Path file = SETTINGS.resolve(path + ".json");

                if (!Files.isRegularFile(file)) {
                    problems.add(revision.alias() + ": no settings preset at " + file);
                    continue;
                }

                String content = Files.readString(file);
                if (!content.contains(revision.foundation())) {
                    problems.add(revision.alias() + ": inherits something other than the foundation the registry "
                        + "records (" + revision.foundation() + "), so the world's metadata would describe "
                        + "terrain it is not generating");
                }
            }
        }

        assertTrue(problems.isEmpty(), "settings presets disagree with the registry: " + problems);
    }

    @Test
    void everyImplementedVersionHasAWorldPresetResource() throws IOException {
        List<String> missing = new ArrayList<>();

        for (HistoricalVersion version : HistoricalVersions.all()) {
            String path = version.id().substring(version.id().indexOf(':') + 1);
            Path file = ROOT.resolve("data/historical_experience/worldgen/world_preset/" + path + ".json");

            if (!Files.isRegularFile(file)) {
                missing.add(path);
            }
        }

        assertTrue(missing.isEmpty(),
            "a version with no world preset cannot be created through the GUI at all: " + missing);
    }
}

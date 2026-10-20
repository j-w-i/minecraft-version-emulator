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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit (source lint). The rule that keeps this architecture from decaying into the scattered checks it replaced.
 *
 * <p>Historical behaviour belongs in a {@link Feature} window or in a version's data, and a version's position belongs
 * in {@link Milestones}. A rule that compares {@code chronology()} itself, or writes the era offset out again, is how
 * this project ended up with 154 check sites, 46 boundaries and one boundary spelled five ways. This test fails when
 * that comes back.
 *
 * <p>The allowlist is deliberately small and each entry says why it is not a boundary decision:
 *
 * <ul>
 *   <li>{@code version/} - the model itself, which is where keys and windows are defined.</li>
 *   <li>the {@linkplain #DATED_TABLES dated tables} - "this item was introduced at key K" is DATA keyed by the
 *       registry, not a rule about which era a build is in; the tables are compared by id, and the comparison is
 *       against the key the table itself recorded.</li>
 *   <li>the {@linkplain #EXEMPT era displays} - {@code version.era()} rendered as text in a screen or a log line.</li>
 * </ul>
 */
class ArchitectureLintTest {
    private static final Path MAIN = Path.of("src/main/java/net/historicalexperience");

    /** A comparison of a chronology key against anything. */
    private static final Pattern KEY_COMPARISON =
        Pattern.compile("\\.chronology\\(\\)\\s*[<>]=?|[<>]=?\\s*\\w+\\.chronology\\(\\)");

    /** The era offsets written out again, or the old arithmetic helpers. */
    private static final Pattern ERA_ARITHMETIC =
        Pattern.compile("HistoricalVersions\\.(ERA_\\w+|chronology\\()|[^\\w.]([2-6]0[0-9]_[0-9]{3})\\b");

    /** Files whose job is a dated table: they compare a version's key with a key they recorded themselves. */
    private static final Map<String, String> DATED_TABLES = Map.of(
        "HistoricalCreativeCatalog.java", "the creative catalogue's own introduction windows",
        "HistoricalEnchanting.java", "the enchantment introduction table",
        "HistoricalItemAvailability.java", "the item introduction table");

    /**
     * The one edge from the model to data: the registry reads the declared versions from the composition root, because
     * a version is composed from gameplay and generation data that the model itself must not depend on.
     */
    private static final Map<String, String> ALLOWED_EDGES = Map.of(
        "HistoricalVersions.java", "net.historicalexperience.legacy.LegacyCatalog");

    /** Files that only render the era name. */
    private static final Set<String> ERA_DISPLAYS = Set.of(
        "VersionText.java", "HistoricalUpgradeService.java", "IndevMapProbe.java", "SnapshotAnvilProbe.java",
        "TexturesProbe.java");

    @Test
    @DisplayName("no rule outside the model compares raw chronology keys or writes an era offset out again")
    void noScatteredVersionArithmetic() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : sources()) {
            String relative = MAIN.relativize(file).toString().replace('\\', '/');
            String name = file.getFileName().toString();
            if (relative.startsWith("version/") || relative.startsWith("legacy/")) {
                continue;
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                String trimmed = line.trim();
                if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                    continue;
                }
                if (KEY_COMPARISON.matcher(line).find() && !DATED_TABLES.containsKey(name)) {
                    offenders.add(relative + ":" + (index + 1) + " compares a chronology key: " + trimmed)
                    ;
                }
                if (ERA_ARITHMETIC.matcher(line).find()) {
                    offenders.add(relative + ":" + (index + 1) + " writes an era offset or uses the old arithmetic: "
                        + trimmed);
                }
            }
        }
        assertTrue(offenders.isEmpty(), "historical boundaries must be a Feature window or a Milestone, in one place:"
            + "\n  " + String.join("\n  ", offenders));
    }

    @Test
    @DisplayName("version.era() is only used to print the era, never to decide anything")
    void eraStringsAreDisplayOnly() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : sources()) {
            String relative = MAIN.relativize(file).toString().replace('\\', '/');
            if (relative.startsWith("version/") || relative.startsWith("legacy/")
                    || ERA_DISPLAYS.contains(file.getFileName().toString())) {
                continue;
            }
            if (Files.readString(file, StandardCharsets.UTF_8).contains(".era()")) {
                offenders.add(relative);
            }
        }
        assertTrue(offenders.isEmpty(),
            "use HistoricalVersion.family() for a decision and Features.active(...) for a boundary; era() is text: "
                + offenders);
    }

    @Test
    @DisplayName("the version package does not depend on the packages that consume it")
    void theModelDoesNotDependOnItsConsumers() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN.resolve("version"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (line.startsWith("import net.historicalexperience.")
                            && !line.startsWith("import net.historicalexperience.version.")
                            && !line.equals("import " + ALLOWED_EDGES.getOrDefault(
                                file.getFileName().toString(), "") + ";")) {
                        offenders.add(MAIN.relativize(file) + " imports " + line.substring(7, line.length() - 1));
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(),
            "the version model must not import gameplay, generation, client or server: " + offenders);
    }

    private static List<Path> sources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
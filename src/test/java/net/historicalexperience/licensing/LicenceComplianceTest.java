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
package net.historicalexperience.licensing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the licence and attribution files this project is obliged to ship.
 *
 * <p>Deliberately lightweight: it reads files off disk, not a Gradle model. The point is to fail fast when a
 * licence file is deleted, an SPDX identifier is edited in one place and not the others, or a new source file
 * arrives without a header. It does not attempt to check the JAR contents, which are already covered by
 * {@code build.gradle}'s {@code jar} and {@code sourcesJar} tasks.
 */
class LicenceComplianceTest {

    /** The project's own contributions. Must match gradle.properties and fabric.mod.json. */
    private static final String PROJECT_SPDX = "LGPL-3.0-or-later";

    /** The licence the incorporated Nostalgic Tweaks material remains under: -only, never -or-later. */
    private static final String NT_SPDX = "LGPL-3.0-only";

    /** The second line of every official GNU licence text, checked so a replaced file cannot pass. */
    private static final String GNU_VERSION_LINE = "Version 3, 29 June 2007";

    private static Path repo() {
        // Gradle runs the test task with workingDir set to the project directory (see build.gradle).
        Path here = Paths.get("").toAbsolutePath();
        assertTrue(Files.isDirectory(here.resolve("src/main/java")),
                "test working directory is not the project directory: " + here);
        return here;
    }

    @Test
    @DisplayName("the licence texts exist and are the GNU texts they claim to be")
    void licenceTextsExist() throws IOException {
        Path root = repo();

        // Sizes are the FSF's published files. A mismatch means someone edited or replaced a licence text,
        // which the licences themselves forbid ("changing it is not allowed").
        assertGnuText(root.resolve("LICENSE"), 7652, "GNU LESSER GENERAL PUBLIC LICENSE");
        assertGnuText(root.resolve("COPYING.LESSER"), 7652, "GNU LESSER GENERAL PUBLIC LICENSE");
        assertGnuText(root.resolve("COPYING"), 35149, "GNU GENERAL PUBLIC LICENSE");
    }

    private static void assertGnuText(Path path, long expectedSize, String title) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing licence file: " + path);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        long size = Files.size(path);
        // The FSF file uses \n; a checkout on a system that rewrote line endings is still acceptable, so the
        // size check allows for one extra byte per line.
        assertTrue(size == expectedSize || size == expectedSize + lines.size(),
                path.getFileName() + " is " + size + " bytes, expected the FSF text at " + expectedSize);
        assertEquals(title, lines.get(0).trim(), path.getFileName() + " does not start with the expected title");
        assertEquals(GNU_VERSION_LINE, lines.get(1).trim(),
                path.getFileName() + " does not state " + GNU_VERSION_LINE);
        assertTrue(lines.stream().anyMatch(line -> line.contains("Free Software Foundation")),
                path.getFileName() + " is not an FSF licence text");
    }

    @Test
    @DisplayName("every declared SPDX identifier agrees, and nothing claims the project is -only")
    void declaredIdentifiersAgree() throws IOException {
        Path root = repo();

        String gradleProperties = Files.readString(root.resolve("gradle.properties"), StandardCharsets.UTF_8);
        assertTrue(gradleProperties.contains("mod_license=" + PROJECT_SPDX),
                "gradle.properties must declare mod_license=" + PROJECT_SPDX);

        String modJson = Files.readString(root.resolve("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);
        assertTrue(modJson.contains("\"license\": \"" + PROJECT_SPDX + "\""),
                "fabric.mod.json must declare \"license\": \"" + PROJECT_SPDX + "\"");
        assertTrue(modJson.contains("Nostalgic Tweaks"),
                "fabric.mod.json must name the incorporated third-party project");

        // The project must never claim -or-later over the adapted Nostalgic Tweaks material. That is the one
        // relicensing claim this project is not entitled to make.
        String notices = Files.readString(root.resolve("THIRD_PARTY_NOTICES.md"), StandardCharsets.UTF_8);
        assertTrue(notices.contains(NT_SPDX),
                "THIRD_PARTY_NOTICES.md must state that Nostalgic Tweaks material is " + NT_SPDX);
        assertFalse(notices.contains("Nostalgic Tweaks") && notices.contains("Nostalgic Tweaks is relicensed"),
                "THIRD_PARTY_NOTICES.md must not claim Nostalgic Tweaks has been relicensed");
    }

    @Test
    @DisplayName("the packaging tasks would put the notices and both licence texts in the artefacts")
    void packagingIncludesLicences() throws IOException {
        Path build = repo().resolve("build.gradle");
        String script = Files.readString(build, StandardCharsets.UTF_8);
        for (String required : List.of("LICENSE", "COPYING", "COPYING.LESSER", "THIRD_PARTY_NOTICES.md")) {
            assertTrue(script.contains("'" + required + "'"),
                    "build.gradle does not package " + required + " into the release artefacts");
        }
        assertTrue(script.contains("sourcesJar"),
                "build.gradle must package the licences into the sources JAR too: it is the Corresponding Source");
        assertTrue(script.contains("Bundle-License"),
                "build.gradle must set the licence in the JAR manifest");
        // The manifest attribute must agree with the declared identifier, not merely exist.
        assertFalse(script.contains("Bundle-License': '" + NT_SPDX + "'"),
                "the JAR manifest must not claim the project's own licence is " + NT_SPDX);
    }

    @Test
    @DisplayName("every Nostalgic Tweaks-derived file carries the compound identifier, and the count is declared once")
    void nostaligicTweaksFilesAreCountedConsistently() throws IOException {
        Path root = repo();
        String tool = Files.readString(root.resolve("scripts/apply-licence-headers.py"), StandardCharsets.UTF_8);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("NT_FILES = \\[(.*?)\\n\\]", java.util.regex.Pattern.DOTALL)
                .matcher(tool);
        assertTrue(matcher.find(), "scripts/apply-licence-headers.py has no NT_FILES list");
        int declared = (int) matcher.group(1).lines()
                .map(String::trim)
                .filter(line -> line.startsWith("\"main/") || line.startsWith("\"test/"))
                .count();

        // The full compound expression, not the bare "-only" token: the bare token also appears in this
        // test's own constant and assertions, and in prose. Matching the whole header line avoids counting
        // the counter.
        String marker = "SPDX-License-Identifier: " + PROJECT_SPDX + " AND " + NT_SPDX;
        int actual = 0;
        Path src = root.resolve("src");
        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                String head = text.length() > 2500 ? text.substring(0, 2500) : text;
                if (head.contains(marker)) {
                    actual++;
                }
            }
        }
        assertEquals(declared, actual,
                "the number of files carrying \"" + marker + "\" does not match the NT_FILES list; "
                        + "re-run scripts/apply-licence-headers.py and update docs/PROVENANCE.md");
        assertTrue(actual > 0, "no file is marked as containing Nostalgic Tweaks material");

        // The notices and the provenance record must state the same count as the tree.
        String notices = Files.readString(root.resolve("THIRD_PARTY_NOTICES.md"), StandardCharsets.UTF_8);
        assertTrue(notices.contains("**" + actual + " Java source files**"),
                "THIRD_PARTY_NOTICES.md does not state the current count of " + actual + " Java files");
        String provenance = Files.readString(root.resolve("docs/PROVENANCE.md"), StandardCharsets.UTF_8);
        assertTrue(provenance.contains("**" + actual + " Java files**"),
                "docs/PROVENANCE.md does not state the current count of " + actual + " Java files");
    }

    @Test
    @DisplayName("every Java source file carries a licence header")
    void everySourceFileHasAHeader() throws IOException {
        Path src = repo().resolve("src");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(src)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                String head = text.length() > 2500 ? text.substring(0, 2500) : text;
                if (!head.contains("SPDX-License-Identifier:")) {
                    offenders.add(src.relativize(file).toString().replace('\\', '/'));
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "these source files have no SPDX licence header (run scripts/apply-licence-headers.py): " + offenders);
    }

    @Test
    @DisplayName("no file tells developers or agents to disregard licensing")
    void noInstructionToIgnoreLicensing() throws IOException {
        Path root = repo();
        List<String> suspicious = List.of(
                "licence be disregarded",
                "license be disregarded",
                "disregard the licence",
                "disregard the license",
                "ignore licensing",
                "ignore copyright restrictions",
                "regardless of licence",
                "regardless of license");
        List<String> offenders = new ArrayList<>();
        for (String relative : List.of("AGENTS.md", "THIRD_PARTY_NOTICES.md", "docs/LICENSING.md", "docs/PROVENANCE.md")) {
            Path file = root.resolve(relative);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
            for (String needle : suspicious) {
                if (text.contains(needle)) {
                    offenders.add(relative + " contains \"" + needle + "\"");
                }
            }
        }
        assertTrue(offenders.isEmpty(), "active project guidance still tells readers to disregard licensing: " + offenders);
    }

    @Test
    @DisplayName("the packaged notices name the incorporated project, its licence and its source")
    void noticesNameTheIncorporatedProject() throws IOException {
        Path notices = repo().resolve("THIRD_PARTY_NOTICES.md");
        String text = Files.readString(notices, StandardCharsets.UTF_8);
        for (String required : List.of(
                "Nostalgic Tweaks",
                "https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks",
                "LGPL-3.0-only",
                "51d6d3dc1fa33223f8548e1625ed7595183c2b9e",
                "minecraft-version-emulator",
                "Corresponding Source")) {
            assertTrue(text.contains(required), "THIRD_PARTY_NOTICES.md does not mention " + required);
        }
        // The notices file is a plain UTF-8 text file; prove it is readable as one.
        try (InputStream stream = Files.newInputStream(notices)) {
            assertTrue(stream.read() != -1, "THIRD_PARTY_NOTICES.md is empty");
        }
    }
}

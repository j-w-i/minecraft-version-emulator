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
package net.historicalexperience.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit (source and config lint).
 *
 * <p>Two things about the mixin config can only be checked outside the game, and both are fatal or silent rather than
 * loud:
 *
 * <ul>
 *   <li>A mixin in the <b>common</b> list that touches a client class does not crash a dedicated server - it logs
 *       {@code @Mixin target ... was not found} and applies nothing - so the mistake survives every test until someone
 *       reads the log. It is also fatal under {@code mixin.debug.strict}, which is how a release bug would appear.</li>
 *   <li>Mixin applies handlers in <b>config order</b> when priorities are equal (all of this project's are), so two
 *       mixins on the same instruction depend on the order of the arrays. Re-sorting the config during an unrelated
 *       edit would change behaviour with nothing failing.</li>
 * </ul>
 *
 * <p>This test turns both into build failures. It reads the real config, not a copy.
 */
class MixinConfigConsistencyTest {
    private static final Path CONFIG = Path.of("src/main/resources/historical_experience.mixins.json");
    private static final Path MIXINS = Path.of("src/main/java/net/historicalexperience/mixin");

    /** A comment line or a block comment, removed before the client-reference scan. */
    private static final Pattern COMMENTS = Pattern.compile("(?s)/\\*.*?\\*/|(?m)//.*$");

    private static final Pattern CLIENT_REFERENCE =
        Pattern.compile("net\\.minecraft\\.client\\b|net\\.historicalexperience\\.client\\b");

    @Test
    @DisplayName("a mixin in the common list never touches a client class")
    void commonMixinsAreServerSafe() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String entry : arrays().get("mixins")) {
            Path file = source(entry);
            if (file == null) {
                continue;
            }
            String body = COMMENTS.matcher(Files.readString(file, StandardCharsets.UTF_8)).replaceAll("");
            Matcher matcher = CLIENT_REFERENCE.matcher(body);
            if (matcher.find()) {
                offenders.add(entry + " refers to " + matcher.group());
            }
        }
        assertTrue(offenders.isEmpty(), "a common mixin must not touch client classes (it is skipped on a dedicated "
            + "server, and fatal under mixin.debug.strict): " + offenders);
    }

    @Test
    @DisplayName("every entry has a file, every file has an entry, and each is in exactly one array")
    void configAndFilesAgree() throws IOException {
        Map<String, List<String>> arrays = arrays();
        Set<String> listed = new LinkedHashSet<>();
        for (List<String> entries : arrays.values()) {
            for (String entry : entries) {
                assertTrue(listed.add(entry), entry + " is listed twice");
                assertTrue(source(entry) != null, entry + " is listed but has no source file");
            }
        }

        List<String> unlisted = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MIXINS)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String entry = entryName(file);
                if (!listed.contains(entry)) {
                    unlisted.add(entry);
                }
            }
        }
        assertTrue(unlisted.isEmpty(), "a mixin file exists but is not registered, so it is never applied: " + unlisted);
        assertTrue(listed.size() > 100, "the config should list every mixin; found only " + listed.size());
    }

    @Test
    @DisplayName("each mixin file declares the package its directory implies, or Mixin cannot find it at all")
    void packageStatementsMatchTheDirectories() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MIXINS)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String relative = MIXINS.relativize(file).toString().replace('\\', '/');
                String dotted = relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
                int lastDot = dotted.lastIndexOf('.');
                String expected = "package net.historicalexperience.mixin." + dotted.substring(0, lastDot) + ";";
                String body = Files.readString(file, StandardCharsets.UTF_8);
                // The package statement may sit on the same line as the end of the licence comment; search, do not line-match.
                if (!body.contains(expected)) {
                    offenders.add(relative + " does not declare " + expected);
                }
            }
        }
        assertTrue(offenders.isEmpty(), "a mixin whose package does not match its directory is not found by the "
            + "transformer, which is a startup crash with required:true: " + offenders);
    }

    @Test
    @DisplayName("the order of mixins sharing an instruction is preserved, because that order is the apply order")
    void overlappingMixinsKeepTheirOrder() throws IOException {
        List<String> common = arrays().get("mixins");
        List<String> client = arrays().get("client");

        // Two @WrapWithCondition handlers on the same instruction in Minecraft.startUseItem, and two
        // @ModifyExpressionValue handlers on LivingEntity.startUsingItem. Equal priority means config order decides,
        // so a re-sort of either array changes the order the values pass through. Each pair sits in whichever array
        // its side requires, so the assertion is per array.
        assertOrderedBefore(common, "LivingEntityHistoricalSwordBlockingMixin", "LivingEntityHistoricalInstantEatMixin");
        assertOrderedBefore(client, "MinecraftHistoricalSwordBlockingMixin", "MinecraftHistoricalInstantEatMixin");

        assertFalse(client.isEmpty(), "the client array must not be empty");
    }

    /** Asserts that the second entry is applied after the first, which is what config order means at equal priority. */
    private static void assertOrderedBefore(List<String> entries, String first, String second) {
        int a = indexOf(entries, first);
        int b = indexOf(entries, second);
        assertTrue(a >= 0 && b >= 0, first + " and " + second + " must both be registered");
        assertTrue(a < b, first + " must stay before " + second + " in the config: equal priorities are applied in "
            + "config order, so re-sorting the array changes behaviour silently");
    }

    private static int indexOf(List<String> entries, String simpleName) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).equals(simpleName) || entries.get(i).endsWith("." + simpleName)) {
                return i;
            }
        }
        return -1;
    }

    /** Reads the two arrays. A tiny hand parser: the file is ours and its shape is fixed. */
    private static Map<String, List<String>> arrays() throws IOException {
        String text = COMMENTS.matcher(Files.readString(CONFIG, StandardCharsets.UTF_8)).replaceAll("");
        Map<String, List<String>> arrays = new LinkedHashMap<>();
        for (String name : List.of("mixins", "client", "server")) {
            Matcher array = Pattern.compile("\"" + name + "\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(text);
            if (!array.find()) {
                continue;
            }
            List<String> entries = new ArrayList<>();
            Matcher entry = Pattern.compile("\"([^\"]+)\"").matcher(array.group(1));
            while (entry.find()) {
                entries.add(entry.group(1));
            }
            arrays.put(name, entries);
        }
        return arrays;
    }

    private static Path source(String entry) {
        String relative = entry.replace('.', '/') + ".java";
        Path direct = MIXINS.resolve(relative);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        // Entries are relative to the json "package", which is this directory.
        Path nested = MIXINS.resolve(relative);
        return Files.isRegularFile(nested) ? nested : null;
    }

    private static String entryName(Path file) {
        String relative = MIXINS.relativize(file).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".java".length()).replace('/', '.');
    }
}

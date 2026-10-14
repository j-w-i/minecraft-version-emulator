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
package net.historicalexperience.client.textures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Dependency detection against synthetic packs built in a temp directory.
 *
 * <p>The fixtures are tiny files written by the test, shaped like the real packs' metadata and namespaces (verified
 * against the installed packs). No real pack, texture or archive is copied or committed.
 */
class PackScannerTest {
    private static final int FORMAT = 34;

    private static final String GD_BASE_MCMETA = """
        {"pack": {"description": "§6Golden Days §8- §2Base §7(Beta)\\n§8by PoeticRainbow",
         "pack_format": 15, "supported_formats": [15, 420]}}""";
    private static final String GD_ALPHA_MCMETA = """
        {"pack": {"description": "§6Golden Days §8- §aAlpha §7(Alpha)", "pack_format": 15,
         "supported_formats": [15, 420]}}""";
    private static final String PACP_MCMETA = """
        {"pack": {"pack_format": 97, "min_format": 15, "max_format": 97,
         "supported_formats": {"min_inclusive": 15, "max_inclusive": 97}, "description": "2.5.4 Release / By mzovjn"}}""";

    private static Map<String, String> goldenDaysBase() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", GD_BASE_MCMETA);
        files.put("assets/golden_days/lang/en_us.json", "{}");
        files.put("assets/minecraft/textures/block/stone.png", "x");
        return files;
    }

    private static Map<String, String> goldenDaysAlpha() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", GD_ALPHA_MCMETA);
        files.put("usage.txt", "This pack is meant to be put over Programmer Art and Golden Days Base.");
        files.put("assets/golden_days_alpha/lang/en_us.json", "{}");
        files.put("assets/minecraft/textures/block/grass_block_top.png", "x");
        return files;
    }

    private static Map<String, String> continuation(String changelogHeading) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("pack.mcmeta", PACP_MCMETA);
        files.put("credits.txt", "Programmer Art Continuation Project \nBase v2.5.4\nBeta-Fied v1.4.1_01\n");
        files.put("changelog.txt", changelogHeading);
        files.put("assets/minecraft/textures/block/dirt.png", "x");
        return files;
    }

    private static final String PACP_BASE_CHANGELOG =
        "# Programmer Art Continuation Project\n## PACP Base Pack 2.5.4\n\nTiny Takeover";

    private static void zip(Path file, Map<String, String> entries) throws IOException {
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    private static void extract(Path directory, Map<String, String> entries) throws IOException {
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            Path target = directory.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue());
        }
    }

    @Test
    @DisplayName("all three external packs are found by content when installed as ZIPs")
    void findsAllThree(@TempDir Path dir) throws IOException {
        zip(dir.resolve("golden-days-base-16.5.zip"), goldenDaysBase());
        zip(dir.resolve("golden-days-alpha-16.5.zip"), goldenDaysAlpha());
        zip(dir.resolve("PACP_Base_Release.zip"), continuation(PACP_BASE_CHANGELOG));
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertEquals(3, result.usable().size());
        assertTrue(result.of(TextureSource.GOLDEN_DAYS_BASE).usable());
        assertTrue(result.of(TextureSource.GOLDEN_DAYS_ALPHA).usable());
        assertTrue(result.of(TextureSource.CONTINUATION).usable());
        assertEquals("2.5.4", result.of(TextureSource.CONTINUATION).classification().version());
    }

    @Test
    @DisplayName("renamed ZIPs are still recognised, because detection looks at contents, not names")
    void renamedZips(@TempDir Path dir) throws IOException {
        zip(dir.resolve("my-favourite-pack.zip"), goldenDaysBase());
        zip(dir.resolve("a.zip"), goldenDaysAlpha());
        zip(dir.resolve("zzz.zip"), continuation(PACP_BASE_CHANGELOG));
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertEquals(3, result.usable().size());
        assertEquals("my-favourite-pack.zip", result.of(TextureSource.GOLDEN_DAYS_BASE).chosen().getFileName().toString());
    }

    @Test
    @DisplayName("extracted folders are recognised the same way as archives")
    void extractedFolders(@TempDir Path dir) throws IOException {
        extract(dir.resolve("gd"), goldenDaysBase());
        extract(dir.resolve("gda"), goldenDaysAlpha());
        extract(dir.resolve("pacp"), continuation(PACP_BASE_CHANGELOG));
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertEquals(3, result.usable().size());
        assertTrue(Files.isDirectory(result.of(TextureSource.GOLDEN_DAYS_BASE).chosen()));
    }

    @Test
    @DisplayName("a familiar file name is not evidence: an unrelated ZIP called golden-days.zip is not accepted")
    void familiarNameIsNotEnough(@TempDir Path dir) throws IOException {
        Map<String, String> impostor = new LinkedHashMap<>();
        impostor.put("pack.mcmeta", "{\"pack\": {\"description\": \"Totally different\", \"pack_format\": 34}}");
        impostor.put("assets/minecraft/textures/block/stone.png", "x");
        zip(dir.resolve("golden-days-base-16.5-[1.20-to-26.4].zip"), impostor);
        zip(dir.resolve("PACP_Base_Release.zip"), impostor);
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertTrue(result.usable().isEmpty());
        assertNull(result.of(TextureSource.GOLDEN_DAYS_BASE).chosen());
    }

    @Test
    @DisplayName("files that are not packs, corrupt archives and an empty or missing directory report missing, not an error")
    void invalidAndMissing(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("notes.txt"), "hello");
        Files.writeString(dir.resolve("broken.zip"), "this is not a zip archive");
        zip(dir.resolve("no-meta.zip"), Map.of("assets/minecraft/textures/block/stone.png", "x"));
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertTrue(result.usable().isEmpty());
        assertEquals(0, PackScanner.scan(dir.resolve("does-not-exist"), FORMAT).usable().size());
        assertEquals(0, PackScanner.scan((Path) null, FORMAT).usable().size());
    }

    @Test
    @DisplayName("a Beta-Fied or Alpha-Fied Programmer Art Continuation is reported as the wrong variant")
    void wrongContinuationVariant(@TempDir Path dir) throws IOException {
        zip(dir.resolve("beta-fied.zip"), continuation("# Programmer Art Continuation Project\n## Beta-Fied v1.4.1_01\n"));
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        PackScanner.Found found = result.of(TextureSource.CONTINUATION);
        assertFalse(found.usable());
        assertEquals(PackClassifier.Status.WRONG_VARIANT, found.classification().status());
        assertFalse(found.rejected().isEmpty());
    }

    @Test
    @DisplayName("a pack whose declared formats exclude the running game is reported as unsupported")
    void unsupportedFormat(@TempDir Path dir) throws IOException {
        Map<String, String> old = goldenDaysBase();
        old.put("pack.mcmeta", "{\"pack\": {\"description\": \"Golden Days Base\", \"pack_format\": 15, "
            + "\"supported_formats\": [15, 20]}}");
        zip(dir.resolve("old-golden-days.zip"), old);
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertEquals(PackClassifier.Status.UNSUPPORTED_FORMAT,
            result.of(TextureSource.GOLDEN_DAYS_BASE).classification().status());
        assertFalse(result.of(TextureSource.GOLDEN_DAYS_BASE).usable());
    }

    @Test
    @DisplayName("duplicate installs: the usable one wins deterministically and the duplicate is reported, not hidden")
    void duplicates(@TempDir Path dir) throws IOException {
        zip(dir.resolve("golden-days-base-a.zip"), goldenDaysBase());
        zip(dir.resolve("golden-days-base-copy.zip"), goldenDaysBase());
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        PackScanner.Found found = result.of(TextureSource.GOLDEN_DAYS_BASE);
        assertTrue(found.usable());
        assertEquals(1, found.duplicates().size());
        assertEquals("golden-days-base-a.zip", found.chosen().getFileName().toString(),
            "ties break on the lexicographically first path");
    }

    @Test
    @DisplayName("a newer Programmer Art Continuation outranks an older copy")
    void newerContinuationWins(@TempDir Path dir) throws IOException {
        zip(dir.resolve("a-old.zip"), continuation("# Programmer Art Continuation Project\n## PACP Base Pack 2.3.0\n"));
        zip(dir.resolve("b-new.zip"), continuation("# Programmer Art Continuation Project\n## PACP Base Pack 2.5.4\n"));
        PackScanner.Found found = PackScanner.scan(dir, FORMAT).of(TextureSource.CONTINUATION);
        assertEquals("b-new.zip", found.chosen().getFileName().toString());
        assertEquals(1, found.duplicates().size());
    }

    @Test
    @DisplayName("Golden Days Alpha is told apart from Base by its namespace even when both say Golden Days")
    void alphaIsNotBase(@TempDir Path dir) throws IOException {
        zip(dir.resolve("only-alpha.zip"), goldenDaysAlpha());
        PackScanner.Result result = PackScanner.scan(dir, FORMAT);
        assertTrue(result.of(TextureSource.GOLDEN_DAYS_ALPHA).usable());
        assertFalse(result.of(TextureSource.GOLDEN_DAYS_BASE).usable());
    }

    @Test
    @DisplayName("scanning only reads: the installed archives are byte-identical afterwards")
    void neverModifiesPacks(@TempDir Path dir) throws IOException {
        Path base = dir.resolve("gd.zip");
        zip(base, goldenDaysBase());
        byte[] before = Files.readAllBytes(base);
        java.nio.file.attribute.FileTime time = Files.getLastModifiedTime(base);
        PackScanner.scan(dir, FORMAT);
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(base)));
        assertEquals(time, Files.getLastModifiedTime(base));
    }

    @Test
    @DisplayName("PACP's min_format array form and the standard forms are all understood")
    void formatForms() {
        com.google.gson.JsonObject arrayForm = com.google.gson.JsonParser.parseString(
            "{\"min_format\": [20], \"max_format\": [97]}").getAsJsonObject();
        assertTrue(PackClassifier.supports(arrayForm, 34));
        assertFalse(PackClassifier.supports(arrayForm, 99));
        com.google.gson.JsonObject legacy = com.google.gson.JsonParser.parseString(
            "{\"pack_format\": 34}").getAsJsonObject();
        assertTrue(PackClassifier.supports(legacy, 34));
        assertFalse(PackClassifier.supports(legacy, 35));
        assertTrue(PackClassifier.supports(new com.google.gson.JsonObject(), 34), "no declaration is compatible");
    }

    @Test
    @DisplayName("dotted versions compare numerically, not as text")
    void versionOrdering() {
        assertTrue(PackScanner.compareVersions("2.10.0", "2.9.0") > 0);
        assertTrue(PackScanner.compareVersions("2.5.4", "2.5.4") == 0);
        assertTrue(PackScanner.compareVersions("", "1.0") < 0);
    }
}

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The downloader's safety properties, driven by an in-memory fetcher so no network access and no third-party file is
 * involved. The fixture "packs" are a few bytes written by the test, pinned by a pin built here.
 */
class PackDownloaderTest {
    private static PackPin pinFor(byte[] body, String name) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(body));
        return new PackPin(TextureSource.GOLDEN_DAYS_BASE, "golden-days", "BFzJ6aQL", "x", "1.0", name,
            "https://cdn.modrinth.com/data/BFzJ6aQL/versions/x/" + name, hash, body.length);
    }

    private static byte[] body(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static long files(Path dir) throws IOException {
        try (Stream<Path> children = Files.list(dir)) {
            return children.count();
        }
    }

    @Test
    @DisplayName("a body that matches the pinned size and SHA-512 is kept at the final name")
    void goodDownloadIsKept(@TempDir Path dir) throws Exception {
        byte[] data = body("pretend this is a resource pack archive");
        PackPin pin = pinFor(data, "pack.zip");
        PackDownloader loader = new PackDownloader(dir, p -> new ByteArrayInputStream(data));
        PackDownloader.State state = loader.fetch(pin);
        assertEquals(PackDownloader.Phase.DONE, state.phase());
        assertTrue(java.util.Arrays.equals(data, Files.readAllBytes(dir.resolve("pack.zip"))));
        assertEquals(1, files(dir), "no .part file is left behind");
    }

    @Test
    @DisplayName("a body with the right size but the wrong hash is rejected and leaves nothing at the final path")
    void wrongHashRejected(@TempDir Path dir) throws Exception {
        byte[] data = body("0123456789");
        PackPin pin = pinFor(data, "pack.zip");
        byte[] tampered = body("0123456780");
        PackDownloader loader = new PackDownloader(dir, p -> new ByteArrayInputStream(tampered));
        PackDownloader.State state = loader.fetch(pin);
        assertEquals(PackDownloader.Phase.FAILED, state.phase());
        assertTrue(state.message().contains("SHA-512"), state.message());
        assertFalse(Files.exists(dir.resolve("pack.zip")));
        assertEquals(0, files(dir), "the partial file is removed");
    }

    @Test
    @DisplayName("a truncated body is rejected")
    void shortReadRejected(@TempDir Path dir) throws Exception {
        byte[] data = body("a longer body than we will receive");
        PackPin pin = pinFor(data, "pack.zip");
        PackDownloader loader = new PackDownloader(dir, p -> new ByteArrayInputStream(data, 0, 5));
        assertEquals(PackDownloader.Phase.FAILED, loader.fetch(pin).phase());
        assertFalse(Files.exists(dir.resolve("pack.zip")));
        assertEquals(0, files(dir));
    }

    @Test
    @DisplayName("a body longer than the pinned size is abandoned instead of being written in full")
    void oversizedAbandoned(@TempDir Path dir) throws Exception {
        byte[] data = body("short");
        PackPin pin = pinFor(data, "pack.zip");
        long[] served = {0};
        InputStream endless = new InputStream() {
            @Override
            public int read() {
                served[0]++;
                return 'x';
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                served[0] += length;
                java.util.Arrays.fill(buffer, offset, offset + length, (byte) 'x');
                return length;
            }
        };
        PackDownloader loader = new PackDownloader(dir, p -> endless);
        PackDownloader.State state = loader.fetch(pin);
        assertEquals(PackDownloader.Phase.FAILED, state.phase());
        assertTrue(served[0] < 1_000_000, "stopped promptly; read " + served[0]);
        assertEquals(0, files(dir));
    }

    @Test
    @DisplayName("a network error is a failed state, not an exception, and leaves no file")
    void networkErrorContained(@TempDir Path dir) throws Exception {
        PackPin pin = pinFor(body("data"), "pack.zip");
        PackDownloader loader = new PackDownloader(dir, p -> {
            throw new IOException("connection reset");
        });
        PackDownloader.State state = loader.fetch(pin);
        assertEquals(PackDownloader.Phase.FAILED, state.phase());
        assertTrue(state.message().contains("connection reset"));
        assertEquals(0, files(dir));
    }

    @Test
    @DisplayName("a failed re-download never damages a pack that is already there")
    void failedRetryKeepsExistingFile(@TempDir Path dir) throws Exception {
        byte[] data = body("the good pack contents");
        PackPin pin = pinFor(data, "pack.zip");
        new PackDownloader(dir, p -> new ByteArrayInputStream(data)).fetch(pin);
        PackDownloader failing = new PackDownloader(dir, p -> new ByteArrayInputStream(body("corrupted!!!!!!!!!!!!!!")));
        assertEquals(PackDownloader.Phase.FAILED, failing.fetch(pin).phase());
        assertTrue(java.util.Arrays.equals(data, Files.readAllBytes(dir.resolve("pack.zip"))));
    }

    @Test
    @DisplayName("every production pin is a fixed Modrinth CDN HTTPS URL with a complete SHA-512 and a plausible size")
    void productionPinsAreWellFormed() {
        assertEquals(3, PackPin.ALL.size());
        for (PackPin pin : PackPin.ALL) {
            URI uri = URI.create(pin.url());
            assertEquals("https", uri.getScheme(), pin.fileName());
            assertEquals("cdn.modrinth.com", uri.getHost(), pin.fileName());
            assertTrue(uri.getPath().contains("/" + pin.projectId() + "/versions/" + pin.versionId() + "/"),
                "the URL names the pinned project and version: " + pin.fileName());
            assertEquals(128, pin.sha512().length(), "SHA-512 is 128 hex characters: " + pin.fileName());
            assertTrue(pin.sha512().matches("[0-9a-f]{128}"), pin.fileName());
            assertTrue(pin.size() > 100_000, pin.fileName());
            assertFalse(pin.versionNumber().isBlank());
        }
        assertEquals(List.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA, TextureSource.CONTINUATION),
            PackPin.ALL.stream().map(PackPin::source).toList());
    }

    @Test
    @DisplayName("the production fetcher refuses a pin that is not a Modrinth CDN HTTPS URL")
    void productionFetcherRefusesOtherHosts() throws Exception {
        PackPin evil = new PackPin(TextureSource.GOLDEN_DAYS_BASE, "x", "x", "x", "1", "x.zip",
            "https://example.com/x.zip", "0".repeat(128), 10);
        PackPin plain = new PackPin(TextureSource.GOLDEN_DAYS_BASE, "x", "x", "x", "1", "x.zip",
            "http://cdn.modrinth.com/x.zip", "0".repeat(128), 10);
        PackDownloader.Fetcher fetcher = PackDownloader.httpsFetcher();
        org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> fetcher.open(evil));
        org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> fetcher.open(plain));
    }

    @Test
    @DisplayName("a downloaded copy in the managed folder wins over the same version elsewhere, and both are found")
    void managedFolderPreferred(@TempDir Path root) throws Exception {
        Path managed = Files.createDirectories(root.resolve("managed"));
        Path player = Files.createDirectories(root.resolve("player"));
        byte[] pack = body("x");
        for (Path folder : new Path[] {managed, player}) {
            Path zip = folder.resolve("copy.zip");
            try (var out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) {
                out.putNextEntry(new java.util.zip.ZipEntry("pack.mcmeta"));
                out.write(body("{\"pack\": {\"description\": \"Golden Days Base\", \"pack_format\": 15, "
                    + "\"supported_formats\": [15, 420]}}"));
                out.closeEntry();
                out.putNextEntry(new java.util.zip.ZipEntry("assets/golden_days/lang/en_us.json"));
                out.write(pack);
                out.closeEntry();
            }
        }
        PackScanner.Result result = PackScanner.scan(List.of(managed, player), 34);
        PackScanner.Found found = result.of(TextureSource.GOLDEN_DAYS_BASE);
        assertTrue(found.usable());
        assertEquals(managed.resolve("copy.zip"), found.chosen(), "the verified download is listed first and wins a tie");
        assertEquals(1, found.duplicates().size());
    }
}

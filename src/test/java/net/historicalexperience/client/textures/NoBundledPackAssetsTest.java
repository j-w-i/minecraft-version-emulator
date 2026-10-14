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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Licensing guard. The three dependency packs are All Rights Reserved and Programmer Art is Mojang's, so none of their
 * files may ever be committed to this project. The system works only by reading them where the player has them.
 *
 * <p>This fails if a pack archive, or a texture under a Minecraft-namespaced path, appears in the mod's own resources
 * or test resources. (The mod's own {@code historical_experience} namespace is its own art and is not checked here.)
 */
class NoBundledPackAssetsTest {
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final Path TEST_RESOURCES = Path.of("src/test/resources");

    private static List<Path> all(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    @DisplayName("no pack archive is bundled in the mod or its tests")
    void noArchives() throws IOException {
        for (Path root : new Path[] {MAIN_RESOURCES, TEST_RESOURCES}) {
            for (Path file : all(root)) {
                String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                assertTrue(!name.endsWith(".zip") && !name.endsWith(".jar"), "bundled archive: " + file);
            }
        }
    }

    @Test
    @DisplayName("no texture is shipped under assets/minecraft, where a third-party pack's art would have to live")
    void noMinecraftNamespaceTextures() throws IOException {
        for (Path root : new Path[] {MAIN_RESOURCES, TEST_RESOURCES}) {
            for (Path file : all(root)) {
                String path = root.relativize(file).toString().replace('\\', '/');
                assertTrue(!(path.startsWith("assets/minecraft/textures/") && path.endsWith(".png")),
                    "a Minecraft-namespace texture is bundled: " + path);
                assertTrue(!path.startsWith("assets/golden_days"), "Golden Days content is bundled: " + path);
            }
        }
    }

    @Test
    @DisplayName("every download is pinned, so nothing is fetched that this project has not named")
    void everythingFetchedIsPinned() {
        for (TextureSource source : TextureSource.values()) {
            if (source.isDependency() && source != TextureSource.PROGRAMMER_ART) {
                assertTrue(PackPin.of(source) != null, "an unpinned dependency: " + source);
            }
        }
        assertTrue(PackPin.of(TextureSource.PROGRAMMER_ART) == null, "Programmer Art is built in and is never downloaded");
    }
}

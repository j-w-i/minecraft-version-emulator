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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only view of the contents of a resource pack, whether it is a ZIP or an extracted folder.
 *
 * <p>This exists so detection can look at what a pack <i>contains</i> rather than at its file name, and so the same
 * detection code runs unchanged against a real archive, an extracted directory and a small synthetic test fixture.
 * Nothing here ever writes: installed packs are never modified or rewritten.
 */
public interface PackFiles extends AutoCloseable {
    boolean exists(String path);

    /** The file's text, capped at {@code maxBytes}; empty when absent or unreadable. */
    Optional<String> readText(String path, int maxBytes);

    /** The names under {@code assets/}, i.e. the namespaces the pack provides. */
    Set<String> namespaces();

    @Override
    void close();

    /** Opens a ZIP archive or a directory; throws when it is neither. */
    static PackFiles open(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return new Directory(path);
        }
        return new Zip(path);
    }

    /** An archive. The entry names are read once from the central directory; file bodies only on request. */
    final class Zip implements PackFiles {
        private final ZipFile zip;
        private final Set<String> names = new HashSet<>();
        private final Set<String> namespaces = new HashSet<>();

        Zip(Path path) throws IOException {
            this.zip = new ZipFile(path.toFile());
            zip.stream().forEach(entry -> {
                String name = entry.getName();
                names.add(name);
                if (name.startsWith("assets/")) {
                    int end = name.indexOf('/', "assets/".length());
                    if (end > 0) {
                        namespaces.add(name.substring("assets/".length(), end));
                    }
                }
            });
        }

        @Override
        public boolean exists(String path) {
            return names.contains(path);
        }

        @Override
        public Optional<String> readText(String path, int maxBytes) {
            ZipEntry entry = zip.getEntry(path);
            if (entry == null || entry.isDirectory()) {
                return Optional.empty();
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return Optional.of(new String(in.readNBytes(maxBytes), StandardCharsets.UTF_8));
            } catch (IOException failure) {
                return Optional.empty();
            }
        }

        @Override
        public Set<String> namespaces() {
            return Collections.unmodifiableSet(namespaces);
        }

        @Override
        public void close() {
            try {
                zip.close();
            } catch (IOException ignored) {
                // Nothing to recover: the archive was only read.
            }
        }
    }

    /** An extracted pack directory. */
    final class Directory implements PackFiles {
        private final Path root;

        Directory(Path root) {
            this.root = root;
        }

        @Override
        public boolean exists(String path) {
            return Files.isRegularFile(root.resolve(path));
        }

        @Override
        public Optional<String> readText(String path, int maxBytes) {
            Path file = root.resolve(path);
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            try (InputStream in = Files.newInputStream(file)) {
                return Optional.of(new String(in.readNBytes(maxBytes), StandardCharsets.UTF_8));
            } catch (IOException failure) {
                return Optional.empty();
            }
        }

        @Override
        public Set<String> namespaces() {
            Path assets = root.resolve("assets");
            if (!Files.isDirectory(assets)) {
                return Set.of();
            }
            try (Stream<Path> children = Files.list(assets)) {
                Set<String> found = new HashSet<>();
                children.filter(Files::isDirectory).forEach(child -> found.add(child.getFileName().toString()));
                return found;
            } catch (IOException failure) {
                return Set.of();
            }
        }

        @Override
        public void close() {
            // Nothing is held open.
        }
    }
}

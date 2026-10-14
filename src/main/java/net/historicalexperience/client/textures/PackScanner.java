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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Finds the dependency packs in a resource-pack directory by inspecting their contents.
 *
 * <p>Both ZIP archives and extracted folders are considered, under any name. When two installed packs both qualify for
 * the same dependency the usable one wins, then the higher declared version, then the lexicographically first path, so
 * the choice is deterministic and a stray older copy cannot shadow the current one. The duplicates are reported rather
 * than hidden.
 *
 * <p>Programmer Art is built into the game and is not scanned for.
 */
public final class PackScanner {
    private PackScanner() {
    }

    /**
     * One dependency's result.
     *
     * @param source     which dependency
     * @param chosen     the pack that will be used, or null when none is usable
     * @param status     the chosen pack's status, or {@link PackClassifier.Status#NOT_A_DEPENDENCY} when none was found
     * @param duplicates other installed packs that qualified for the same dependency
     * @param rejected   packs that matched the dependency's project but cannot be used, with why
     */
    public record Found(TextureSource source, Path chosen, PackClassifier.Classification classification,
                        List<Path> duplicates, List<String> rejected) {
        public boolean usable() {
            return chosen != null && classification.status() == PackClassifier.Status.OK;
        }
    }

    public record Result(Map<TextureSource, Found> found, int scanned) {
        public Found of(TextureSource source) {
            return found.get(source);
        }

        /** The usable packs, in the order they will be layered. */
        public Map<TextureSource, Path> usable() {
            Map<TextureSource, Path> usable = new EnumMap<>(TextureSource.class);
            found.forEach((source, result) -> {
                if (result.usable()) {
                    usable.put(source, result.chosen());
                }
            });
            return usable;
        }
    }

    public static Result scan(Path packDirectory, int gamePackFormat) {
        return scan(java.util.Collections.singletonList(packDirectory), gamePackFormat);
    }

    /**
     * Scans several directories. When the same dependency is found in more than one, a copy in an earlier directory
     * beats a copy in a later one if they declare the same version, so the caller can list the mod's own verified
     * download folder first.
     */
    public static Result scan(List<Path> packDirectories, int gamePackFormat) {
        List<Path> candidates = new ArrayList<>();
        Map<Path, Integer> directoryIndex = new java.util.HashMap<>();
        for (int index = 0; index < packDirectories.size(); index++) {
            Path packDirectory = packDirectories.get(index);
            if (packDirectory != null && Files.isDirectory(packDirectory)) {
                try (Stream<Path> children = Files.list(packDirectory)) {
                    List<Path> found = new ArrayList<>();
                    children.filter(PackScanner::looksLikePack).sorted(Comparator.comparing(Path::toString))
                        .forEach(found::add);
                    for (Path path : found) {
                        directoryIndex.put(path, index);
                    }
                    candidates.addAll(found);
                } catch (IOException failure) {
                    // An unreadable directory is the same as an empty one: every dependency reports missing.
                }
            }
        }
        Map<TextureSource, List<Candidate>> byDependency = new EnumMap<>(TextureSource.class);
        for (Path candidate : candidates) {
            try (PackFiles files = PackFiles.open(candidate)) {
                PackClassifier.Classification result = PackClassifier.classify(files, gamePackFormat);
                if (result.source() != null) {
                    byDependency.computeIfAbsent(result.source(), ignored -> new ArrayList<>())
                        .add(new Candidate(candidate, result, directoryIndex.getOrDefault(candidate, 0)));
                }
            } catch (IOException | RuntimeException unreadable) {
                // A corrupt archive is simply not a dependency.
            }
        }
        Map<TextureSource, Found> found = new EnumMap<>(TextureSource.class);
        for (TextureSource source : TextureSource.values()) {
            if (!source.isDependency() || source == TextureSource.PROGRAMMER_ART) {
                continue;
            }
            List<Candidate> matches = byDependency.getOrDefault(source, List.of());
            Candidate best = matches.stream().filter(c -> c.classification.status() == PackClassifier.Status.OK)
                .max(Comparator.comparing((Candidate c) -> c.classification.version(), PackScanner::compareVersions)
                    .thenComparing(c -> c.directoryIndex, Comparator.reverseOrder())
                    .thenComparing(c -> c.path.toString(), Comparator.reverseOrder()))
                .orElse(null);
            List<String> rejected = new ArrayList<>();
            for (Candidate match : matches) {
                if (match.classification.status() != PackClassifier.Status.OK) {
                    rejected.add(match.path.getFileName() + ": " + match.classification.status() + " ("
                        + match.classification.evidence() + ")");
                }
            }
            if (best == null) {
                Candidate first = matches.isEmpty() ? null : matches.get(0);
                found.put(source, new Found(source, null,
                    first == null
                        ? new PackClassifier.Classification(source, PackClassifier.Status.NOT_A_DEPENDENCY, "",
                            "not installed")
                        : first.classification,
                    List.of(), rejected));
                continue;
            }
            List<Path> duplicates = new ArrayList<>();
            for (Candidate match : matches) {
                if (match != best && match.classification.status() == PackClassifier.Status.OK) {
                    duplicates.add(match.path);
                }
            }
            found.put(source, new Found(source, best.path, best.classification, duplicates, rejected));
        }
        return new Result(found, candidates.size());
    }

    private record Candidate(Path path, PackClassifier.Classification classification, int directoryIndex) {
    }

    private static boolean looksLikePack(Path path) {
        if (Files.isDirectory(path)) {
            return Files.isRegularFile(path.resolve("pack.mcmeta"));
        }
        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return Files.isRegularFile(path) && name.endsWith(".zip");
    }

    /** Numeric-aware comparison of dotted versions; an empty string sorts lowest. */
    static int compareVersions(String a, String b) {
        String[] left = a.split("[._-]");
        String[] right = b.split("[._-]");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            String x = i < left.length ? left[i] : "";
            String y = i < right.length ? right[i] : "";
            int cmp;
            if (!x.isEmpty() && !y.isEmpty() && x.chars().allMatch(Character::isDigit)
                && y.chars().allMatch(Character::isDigit)) {
                cmp = Long.compare(Long.parseLong(x), Long.parseLong(y));
            } else {
                cmp = x.compareTo(y);
            }
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }
}

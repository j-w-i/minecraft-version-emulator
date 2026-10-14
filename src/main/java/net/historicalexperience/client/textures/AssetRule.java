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

import java.util.Set;

/**
 * One per-asset exception to whole-pack layering.
 *
 * <p>The stack is built by layering whole packs. A rule overrides that for specific resources, and only while the
 * active version is inside its {@link #window()}. A rule can do three independent things to the resources it matches:
 *
 * <ul>
 *   <li>{@link #excluded()}: the listed sources do not serve the resource at all.</li>
 *   <li>{@link #preferred()}: that source outranks every other source for the resource, i.e. every layer ranked above
 *       it stops serving it. Layers below it still serve it, so a preferred source that lacks the file degrades to the
 *       next one down instead of leaving a hole.</li>
 *   <li>{@link #pinToVanilla()}: the vanilla resource is admitted into the historical group for it, so a user pack
 *       cannot override it.</li>
 * </ul>
 *
 * <p>Resources are matched by namespace and exact path. A {@code .mcmeta} companion is matched by its base path, so an
 * animation or scaling file always follows the texture it describes and is never taken from a different source.
 *
 * <p>Conflicts between rules that match the same resource are resolved deterministically: exclusions are unioned, a
 * {@code preferred} source that another rule excludes is ignored (exclusion wins), and among several preferences the
 * first declared rule wins. {@link HistoricalTextureRules} declares the rules in priority order.
 */
public record AssetRule(
        String id,
        String namespace,
        Set<String> paths,
        VersionWindow window,
        Set<TextureSource> excluded,
        TextureSource preferred,
        boolean pinToVanilla,
        String reason) {

    public AssetRule {
        paths = Set.copyOf(paths);
        excluded = Set.copyOf(excluded);
    }

    /** True when this rule describes the resource, treating {@code x.png.mcmeta} as part of {@code x.png}. */
    public boolean matches(String resourceNamespace, String resourcePath) {
        return namespace.equals(resourceNamespace) && paths.contains(basePath(resourcePath));
    }

    public boolean activeAt(int chronology) {
        return window.contains(chronology);
    }

    /** The path with a trailing {@code .mcmeta} removed, so metadata is resolved from the same source as its image. */
    public static String basePath(String path) {
        return path.endsWith(".mcmeta") ? path.substring(0, path.length() - ".mcmeta".length()) : path;
    }
}

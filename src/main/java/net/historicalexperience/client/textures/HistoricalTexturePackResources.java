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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * The historical texture layer: one {@link PackResources} that answers every resource request from the right source
 * under a {@link TexturePlan}.
 *
 * <h2>Why a single synthetic pack and not several selected packs</h2>
 *
 * <p>The historical stack is not part of the player's resource-pack selection, so it cannot be five entries in the
 * pack list. It is one pack, handed to the resource manager at the end of the list the game builds for a reload, and
 * it carries the five-way priority inside itself. That is what lets a per-asset exception exist at all: a pack list can
 * only order whole packs, while this answers each resource individually.
 *
 * <h2>How a request is answered</h2>
 *
 * <ol>
 *   <li>Sources are tried from the highest rank down: Golden Days Alpha, Golden Days Base, Programmer Art
 *       Continuation, Programmer Art, then the vanilla pin.</li>
 *   <li>A source is skipped when the plan says it does not serve that resource (a rule excluded it, or a rule
 *       preferred a lower source) or when it simply has no such file.</li>
 *   <li>When no source answers, the request returns null and the game falls through to the user's packs and vanilla,
 *       exactly as it would without this layer.</li>
 * </ol>
 *
 * <p><b>Metadata follows its image.</b> An animation or scaling file ({@code x.png.mcmeta}) is served only from the
 * source that won {@code x.png}. Without that, an animated texture from one pack could be paired with a still image
 * from another, which is a real failure mode of naive stacking.
 *
 * <p>The underlying packs are opened the same way the game opens them (through {@code Pack}), so {@code pack.mcmeta}
 * overlays and format ranges are honoured exactly as they would be for a selected pack.
 */
public final class HistoricalTexturePackResources implements PackResources {
    public static final String PACK_ID = "historical_experience_textures";

    private final TexturePlan plan;
    private final Map<TextureSource, PackResources> sources = new EnumMap<>(TextureSource.class);
    private final Set<TextureSource> owned = new HashSet<>();
    private final List<TextureSource> highestFirst;
    private final PackLocationInfo info = new PackLocationInfo(PACK_ID,
        Component.literal("Historical textures"), PackSource.BUILT_IN, Optional.<KnownPack>empty());

    /**
     * @param opened every source that could be opened, keyed by its role
     * @param owned  the sources this layer must close; the game's own vanilla resources are shared and are not owned
     */
    public HistoricalTexturePackResources(TexturePlan plan, Map<TextureSource, PackResources> opened,
                                          Set<TextureSource> owned) {
        this.plan = plan;
        this.sources.putAll(opened);
        this.owned.addAll(owned);
        this.highestFirst = new ArrayList<>(opened.keySet());
        this.highestFirst.sort(Comparator.comparingInt(TextureSource::rank).reversed());
    }

    public TexturePlan plan() {
        return plan;
    }

    /** Which sources were actually opened, for diagnostics. */
    public Set<TextureSource> openedSources() {
        return Set.copyOf(sources.keySet());
    }

    /**
     * The source that answers {@code location}, or null when none does. Public so tests and the in-game probe can ask
     * "who served this" instead of inferring it from a screenshot.
     */
    public TextureSource winner(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }
        String path = location.getPath();
        if (path.endsWith(".mcmeta")) {
            ResourceLocation image = location.withPath(path.substring(0, path.length() - ".mcmeta".length()));
            TextureSource imageWinner = winnerOfFile(type, image);
            if (imageWinner == null) {
                return null;
            }
            return sources.get(imageWinner).getResource(type, location) != null ? imageWinner : null;
        }
        return winnerOfFile(type, location);
    }

    private TextureSource winnerOfFile(PackType type, ResourceLocation location) {
        for (TextureSource source : highestFirst) {
            if (plan.serves(source, location.getNamespace(), location.getPath())
                && sources.get(source).getResource(type, location) != null) {
                return source;
            }
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        TextureSource source = winner(type, location);
        return source == null ? null : sources.get(source).getResource(type, location);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        // Collect every candidate once, then emit each location from its single winner so the listing and
        // getResource can never disagree about who serves a file.
        Map<ResourceLocation, Boolean> seen = new HashMap<>();
        for (TextureSource source : highestFirst) {
            sources.get(source).listResources(type, namespace, prefix, (location, supplier) -> {
                if (seen.putIfAbsent(location, Boolean.TRUE) == null) {
                    TextureSource winner = winner(type, location);
                    if (winner != null) {
                        IoSupplier<InputStream> served = sources.get(winner).getResource(type, location);
                        if (served != null) {
                            output.accept(location, served);
                        }
                    }
                }
            });
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.CLIENT_RESOURCES) {
            return Set.of();
        }
        Set<String> namespaces = new HashSet<>();
        for (TextureSource source : highestFirst) {
            namespaces.addAll(sources.get(source).getNamespaces(type));
        }
        return namespaces;
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        // No resource filtering or language metadata of its own: the underlying packs' filters would hide files from
        // lower packs, which is the user's concern and not this layer's.
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return info;
    }

    @Override
    public void close() {
        for (TextureSource source : owned) {
            PackResources resources = sources.get(source);
            if (resources != null) {
                resources.close();
            }
        }
    }
}

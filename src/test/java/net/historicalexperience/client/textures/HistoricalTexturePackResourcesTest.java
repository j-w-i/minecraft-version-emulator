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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The composition layer against in-memory packs: who answers a request, and that the listing and the lookup can never
 * disagree. The packs are maps of strings written here, so no real texture is involved.
 */
class HistoricalTexturePackResourcesTest {
    private static final ResourceLocation COBBLE = ResourceLocation.parse("minecraft:textures/block/cobblestone.png");
    private static final ResourceLocation STONE = ResourceLocation.parse("minecraft:textures/block/stone.png");
    private static final ResourceLocation XP = ResourceLocation.parse("minecraft:textures/gui/sprites/hud/experience_bar_background.png");
    private static final ResourceLocation XP_META = ResourceLocation.parse("minecraft:textures/gui/sprites/hud/experience_bar_background.png.mcmeta");
    private static final ResourceLocation ONLY_USER = ResourceLocation.parse("minecraft:textures/block/only_in_programmer_art.png");

    /** A pack that holds named strings. */
    private static final class MemoryPack implements PackResources {
        private final String name;
        private final Map<ResourceLocation, String> files = new HashMap<>();
        private boolean closed;

        MemoryPack(String name, Map<ResourceLocation, String> files) {
            this.name = name;
            this.files.putAll(files);
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... elements) {
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            String body = files.get(location);
            return body == null ? null : () -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
            files.forEach((location, body) -> {
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith(prefix)) {
                    output.accept(location, () -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
                }
            });
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            Set<String> namespaces = new java.util.HashSet<>();
            files.keySet().forEach(location -> namespaces.add(location.getNamespace()));
            return namespaces;
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
            return null;
        }

        @Override
        public PackLocationInfo location() {
            return new PackLocationInfo(name, net.minecraft.network.chat.Component.literal(name), PackSource.DEFAULT,
                Optional.<KnownPack>empty());
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static Map<ResourceLocation, String> files(Object... pairs) {
        Map<ResourceLocation, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ResourceLocation) pairs[i], (String) pairs[i + 1]);
        }
        return map;
    }

    private static HistoricalTexturePackResources layer(String versionId, Map<TextureSource, MemoryPack> packs,
                                                        Set<TextureSource> owned) {
        TexturePlan plan = HistoricalTextureRules.planFor(
            HistoricalVersions.byId("historical_experience:" + versionId).orElseThrow());
        return new HistoricalTexturePackResources(plan, new EnumMap<TextureSource, PackResources>(packs), owned);
    }

    private static Map<TextureSource, MemoryPack> allPacks() {
        Map<TextureSource, MemoryPack> packs = new EnumMap<>(TextureSource.class);
        packs.put(TextureSource.VANILLA_PIN, new MemoryPack("vanilla", files(COBBLE, "vanilla-cobble", XP, "vanilla-xp",
            XP_META, "vanilla-xp-meta", STONE, "vanilla-stone")));
        packs.put(TextureSource.PROGRAMMER_ART, new MemoryPack("pa", files(COBBLE, "pa-cobble", STONE, "pa-stone",
            ONLY_USER, "pa-only")));
        packs.put(TextureSource.CONTINUATION, new MemoryPack("pacp", files(COBBLE, "pacp-cobble", STONE, "pacp-stone")));
        packs.put(TextureSource.GOLDEN_DAYS_BASE, new MemoryPack("gdb", files(COBBLE, "gdb-cobble", STONE, "gdb-stone",
            XP, "gdb-xp", XP_META, "gdb-xp-meta")));
        packs.put(TextureSource.GOLDEN_DAYS_ALPHA, new MemoryPack("gda", files(STONE, "gda-stone")));
        return packs;
    }

    private static String read(PackResources resources, ResourceLocation location) throws IOException {
        IoSupplier<InputStream> supplier = resources.getResource(PackType.CLIENT_RESOURCES, location);
        if (supplier == null) {
            return null;
        }
        try (InputStream in = supplier.get()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Beta 1.6: cobblestone from Golden Days Base, stone from the highest layer that has it")
    void beta16() throws IOException {
        HistoricalTexturePackResources layer = layer("beta_1_6", allPacks(), Set.of());
        assertEquals("gdb-cobble", read(layer, COBBLE));
        assertEquals("gdb-stone", read(layer, STONE), "Golden Days Alpha is not in the Beta 1.6 stack");
    }

    @Test
    @DisplayName("Beta 1.7.3: cobblestone steps back to Programmer Art, past Continuation and Golden Days")
    void beta173() throws IOException {
        HistoricalTexturePackResources layer = layer("beta_1_7_3", allPacks(), Set.of());
        assertEquals("pa-cobble", read(layer, COBBLE));
        assertEquals("gdb-stone", read(layer, STONE), "everything else is still Golden Days Base");
    }

    @Test
    @DisplayName("Alpha: Golden Days Alpha outranks Base for the textures it supplies")
    void alphaLayer() throws IOException {
        HistoricalTexturePackResources layer = layer("alpha_1_0_1", allPacks(), Set.of());
        assertEquals("gda-stone", read(layer, STONE));
        assertEquals("gdb-cobble", read(layer, COBBLE), "cobblestone is pinned to Golden Days Base through Beta 1.6");
    }

    @Test
    @DisplayName("after Release 1.2.5 no Golden Days layer answers anything")
    void afterGoldenDays() throws IOException {
        HistoricalTexturePackResources layer = layer("release_1_6_4", allPacks(), Set.of());
        assertEquals("pa-cobble", read(layer, COBBLE));
        assertEquals("pacp-stone", read(layer, STONE), "Continuation outranks Programmer Art");
    }

    @Test
    @DisplayName("the XP bar is vanilla's in every era, and its .mcmeta follows it, never Golden Days")
    void xpBar() throws IOException {
        for (String id : new String[] {"alpha_1_0_1", "beta_1_7_3", "release_1_2_5", "release_1_6_4"}) {
            HistoricalTexturePackResources layer = layer(id, allPacks(), Set.of());
            assertEquals("vanilla-xp", read(layer, XP), id);
            assertEquals("vanilla-xp-meta", read(layer, XP_META), id);
        }
    }

    @Test
    @DisplayName("a resource no pack in the plan has falls through (null), so the user's packs and vanilla answer")
    void fallThrough() throws IOException {
        Map<TextureSource, MemoryPack> packs = new EnumMap<>(TextureSource.class);
        packs.put(TextureSource.PROGRAMMER_ART, new MemoryPack("pa", files(STONE, "pa-stone")));
        HistoricalTexturePackResources layer = layer("beta_1_7_3", packs, Set.of());
        assertNull(read(layer, ResourceLocation.parse("minecraft:textures/block/dirt.png")));
        assertEquals("pa-stone", read(layer, STONE));
    }

    @Test
    @DisplayName("a missing dependency pack degrades to the next layer down instead of failing")
    void missingPackDegrades() throws IOException {
        Map<TextureSource, MemoryPack> packs = allPacks();
        packs.remove(TextureSource.GOLDEN_DAYS_BASE);
        HistoricalTexturePackResources layer = layer("beta_1_6", packs, Set.of());
        assertEquals("pacp-cobble", read(layer, COBBLE), "preferred Golden Days is absent; Continuation answers");
        assertEquals("vanilla-xp", read(layer, XP));
    }

    @Test
    @DisplayName("listing and lookup agree: every listed resource is the one getResource would return")
    void listingMatchesLookup() throws IOException {
        HistoricalTexturePackResources layer = layer("beta_1_7_3", allPacks(), Set.of());
        Map<ResourceLocation, String> listed = new HashMap<>();
        layer.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures", (location, supplier) -> {
            try (InputStream in = supplier.get()) {
                listed.put(location, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        });
        assertEquals("pa-cobble", listed.get(COBBLE));
        assertEquals("vanilla-xp", listed.get(XP));
        assertEquals("pa-only", listed.get(ONLY_USER));
        for (Map.Entry<ResourceLocation, String> entry : listed.entrySet()) {
            assertEquals(read(layer, entry.getKey()), entry.getValue(), entry.getKey().toString());
        }
    }

    @Test
    @DisplayName("server data is never touched: only CLIENT_RESOURCES is answered")
    void serverDataUntouched() {
        HistoricalTexturePackResources layer = layer("beta_1_7_3", allPacks(), Set.of());
        assertNull(layer.getResource(PackType.SERVER_DATA, COBBLE));
        assertTrue(layer.getNamespaces(PackType.SERVER_DATA).isEmpty());
        assertNull(layer.getRootResource("pack.mcmeta"));
    }

    @Test
    @DisplayName("closing closes only the packs the layer owns; the game's shared vanilla resources stay open")
    void closeOwnership() {
        Map<TextureSource, MemoryPack> packs = allPacks();
        HistoricalTexturePackResources layer = layer("beta_1_7_3", packs, EnumSet.of(TextureSource.PROGRAMMER_ART,
            TextureSource.GOLDEN_DAYS_BASE, TextureSource.CONTINUATION));
        layer.close();
        assertTrue(packs.get(TextureSource.PROGRAMMER_ART).closed);
        assertTrue(packs.get(TextureSource.GOLDEN_DAYS_BASE).closed);
        assertTrue(packs.get(TextureSource.CONTINUATION).closed);
        assertTrue(!packs.get(TextureSource.VANILLA_PIN).closed, "vanilla is shared with the game and not owned");
    }

    @Test
    @DisplayName("the layer knows who served a resource, so tests and probes need not infer it from pixels")
    void winnerIdentity() {
        HistoricalTexturePackResources layer = layer("beta_1_7_3", allPacks(), Set.of());
        assertEquals(TextureSource.PROGRAMMER_ART, layer.winner(PackType.CLIENT_RESOURCES, COBBLE));
        assertEquals(TextureSource.VANILLA_PIN, layer.winner(PackType.CLIENT_RESOURCES, XP));
        assertEquals(TextureSource.VANILLA_PIN, layer.winner(PackType.CLIENT_RESOURCES, XP_META));
        assertNotNull(layer.winner(PackType.CLIENT_RESOURCES, STONE));
        List<TextureSource> opened = new ArrayList<>(layer.openedSources());
        assertEquals(5, opened.size());
    }
}

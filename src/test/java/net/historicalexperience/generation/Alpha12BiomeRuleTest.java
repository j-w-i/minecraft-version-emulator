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
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the Alpha v1.2.0 biome rule through configuration rather than through a running game.
 *
 * <p>Why the check is here and not in a client run: Moderner Beta's biome source is authoritative -
 * {@code ModernBetaBiomeSource.getNoiseBiome} asks its provider for the biome at every position and never reads the
 * biome stored in a chunk - so the only thing that decides what F3 reports is the settings preset the world's revision
 * names. Asserting the preset's own content is therefore a direct check of the behaviour, and it is one that cannot
 * leave a client sitting on a fail-closed screen.
 */
class Alpha12BiomeRuleTest {
    private static final Path PRESETS = Paths.get("src/main/resources/data/historical_experience/moderner_beta/settings_preset");

    private static String presetJson(String alias) throws IOException {
        String file = alias.substring(alias.indexOf(':') + 1) + ".json";
        Path path = PRESETS.resolve(file);
        assertTrue(Files.isRegularFile(path), "no preset JSON for alias " + alias + " at " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Alpha v1.2.0 takes Beta 1.1's biome generator, and Alpha 1.0.1 does not")
    void alpha12TakesBetaElevenBiomes() throws IOException {
        HistoricalVersion alpha12 = HistoricalVersions.byId("historical_experience:alpha_1_2_0").orElseThrow();
        HistoricalVersion.GenerationRevision newest = alpha12.revisions().get(alpha12.revisions().size() - 1);
        assertEquals("historical_experience:alpha_1_2_0_beta_plains", newest.alias(),
            "the newest Alpha 1.2.0 revision is the one a new world selects");

        var json = com.google.gson.JsonParser.parseString(presetJson(newest.alias())).getAsJsonObject();
        assertEquals("moderner_beta:beta_1_1_02",
            json.getAsJsonObject("biomeSettings").get("moderner_beta:preset").getAsString(),
            "the owner's rule is that Alpha v1.2.0 and Beta 1.1 share a biome generator, so the biome settings come "
                + "from Beta 1.1's preset");
        // It must NOT be a single-biome world: Beta 1.1's generator is climate-based and produces many biomes, and the
        // owner explicitly rejected flattening generation.
        assertFalse(json.getAsJsonObject("biomeSettings").has("moderner_beta:single_biome"),
            "Alpha 1.2.0 must generate a biome distribution, not one biome everywhere");
        // The terrain must be untouched, or this revision would move the world's shape as well as its biomes.
        assertEquals("moderner_beta:alpha",
            json.getAsJsonObject("chunkSettings").get("moderner_beta:preset").getAsString(),
            "the terrain foundation stays moderner_beta:alpha");

        // And Alpha 1.0.1 must NOT carry it, or the change would apply to the wrong build.
        HistoricalVersion alpha101 = HistoricalVersions.byId("historical_experience:alpha_1_0_1").orElseThrow();
        for (HistoricalVersion.GenerationRevision revision : alpha101.revisions()) {
            assertFalse(presetJson(revision.alias()).contains("moderner_beta:beta_1_1_02"),
                "Alpha 1.0.1 must keep the Alpha biome provider: " + revision.alias());
        }
    }

    @Test
    @DisplayName("the Beta 1.1 biome settings are confined to the new revision, not the alias it derives from")
    void betaElevenBiomesAreNotInThePublishedAlias() throws IOException {
        // This is the check that was missing when the override was first written: it asserted the override EXISTED,
        // which was true even though it had been written into `alpha_1_2_0_no_modern_content` - a published alias whose
        // worlds recorded the biome they were given.
        String published = presetJson("historical_experience:alpha_1_2_0_no_modern_content");
        var publishedJson = com.google.gson.JsonParser.parseString(published).getAsJsonObject();
        assertEquals("moderner_beta:alpha",
            publishedJson.getAsJsonObject("biomeSettings").get("moderner_beta:preset").getAsString(),
            "the published alias must keep the Alpha provider: editing it would change worlds already created against it");

        String revision = presetJson("historical_experience:alpha_1_2_0_beta_plains");
        var revisionJson = com.google.gson.JsonParser.parseString(revision).getAsJsonObject();
        assertEquals("moderner_beta:beta_1_1_02",
            revisionJson.getAsJsonObject("biomeSettings").get("moderner_beta:preset").getAsString());
        // The two presets must differ in the biome settings and nowhere else. Compared as parsed JSON rather than as
        // text, so formatting and line endings cannot mask or fake the difference.
        revisionJson.add("biomeSettings", publishedJson.getAsJsonObject("biomeSettings"));
        assertEquals(publishedJson, revisionJson,
            "the revision must differ from the published alias only by its biome settings");
    }

    @Test
    @DisplayName("every declared revision alias has a preset file of the same name")
    void everyAliasHasItsPreset() {
        List<String> missing = new ArrayList<>();
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (HistoricalVersion.GenerationRevision revision : version.revisions()) {
                String alias = revision.alias();
                if (!alias.startsWith("historical_experience:")) {
                    continue;
                }
                String file = alias.substring(alias.indexOf(':') + 1) + ".json";
                if (!Files.isRegularFile(PRESETS.resolve(file))) {
                    missing.add(alias);
                }
            }
        }
        assertTrue(missing.isEmpty(),
            "these revision aliases name a preset that does not exist, so a world on them could not generate: " + missing);
    }
}

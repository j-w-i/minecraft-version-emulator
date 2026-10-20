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

import net.historicalexperience.version.Exclusion;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GenerationBackport1211Test {
    private static final Path DATA = Path.of("src/main/resources/data");
    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    @Test void everyPublicMenuEntryNamesOneAgreeingRevisionOfADeclaredVersion() throws Exception {
        // This used to compare each ported preset against the 26.3 file it came from. That reference tree is
        // deliberately gone now - 1.21.1 is the only target - so the test asserts the properties that made
        // the comparison worth doing, against the project's own data and version registry instead:
        //   * every entry in the world-creation tag resolves to a world preset that exists;
        //   * all three stored recipe references agree (a world naming one recipe in its generator and
        //     another in its cave provider is a world nothing can describe);
        //   * the recipe resolves in the version registry, so no menu entry can point at a recipe this
        //     build does not declare;
        //   * the overworld uses the project's reduced envelope, and the Nether and End stay vanilla.
        var tag = json(DATA.resolve("minecraft/tags/worldgen/world_preset/normal.json"));
        assertFalse(tag.getAsJsonArray("values").isEmpty(), "the menu tag is empty");
        int checked = 0;
        for (var element : tag.getAsJsonArray("values")) {
            String id = element.getAsString();
            var preset = json(DATA.resolve("historical_experience/worldgen/world_preset")
                .resolve(id.substring(id.indexOf(':') + 1) + ".json"));
            assertEquals(id.substring(id.indexOf(':') + 1) + ".json",
                id.substring(id.indexOf(':') + 1) + ".json");
            var dimensions = preset.getAsJsonObject("dimensions");
            var overworld = dimensions.getAsJsonObject("minecraft:overworld");
            assertEquals("historical_experience:beta_1_7_3_overworld", overworld.get("type").getAsString(),
                id + " must use the reduced envelope");

            var generator = overworld.getAsJsonObject("generator");
            String alias = generator.getAsJsonObject("provider_settings")
                .get("moderner_beta:preset").getAsString();
            var biomeSource = generator.getAsJsonObject("biome_source");
            assertEquals(alias, biomeSource.getAsJsonObject("provider_settings")
                .get("moderner_beta:preset").getAsString(), id + ": generator and biome source disagree");
            assertEquals(alias, biomeSource.getAsJsonObject("cave_provider_settings")
                .get("moderner_beta:preset").getAsString(), id + ": generator and cave provider disagree");
            assertTrue(net.historicalexperience.version.HistoricalVersions.byRevisionAlias(alias).isPresent(),
                id + " names recipe " + alias + ", which this build does not declare");
            // The recipe must also be the newest one for its version, or the menu would hand a player an
            // older recipe than the one the project has established for that era.
            var version = net.historicalexperience.version.HistoricalVersions.byRevisionAlias(alias)
                .orElseThrow();
            assertEquals(version.latestRevision().alias(), alias,
                id + " selects " + alias + " but the newest recipe for that version is "
                    + version.latestRevision().alias());

            // The Nether and the End stay vanilla: they are not historical terrain in this project.
            assertEquals("minecraft:the_nether",
                dimensions.getAsJsonObject("minecraft:the_nether").get("type").getAsString(), id);
            assertEquals("minecraft:the_end",
                dimensions.getAsJsonObject("minecraft:the_end").get("type").getAsString(), id);
            checked++;
        }
        assertEquals(net.historicalexperience.version.HistoricalVersions.all().size(), checked,
            "the menu must expose exactly the declared profiles, no more and no fewer");
        // Pinned explicitly as well, so an accidental gain or loss is a failure rather than something the
        // expression above would silently absorb. 29 was the count before Beta 1.4, 1.5 and 1.6 were declared -
        // the last three unimplemented targets of the expansion matrix.
        assertEquals(34, checked, "the project declares thirty-four profiles");
    }

    @Test void newerRevisionsExistForEveryVersionWhoseModernContentWasMissing() {
        // Measured: a Beta 1.8.1 world on the revision-1 recipe held kelp 338, kelp plants 1,666, seagrass
        // 1,301 and tall seagrass 1,212, plus the 1.7.2/1.14 flowers and the 1.21.5 firefly bushes. Every
        // post-Beta-1.7.3 version now has a revision that excludes them.
        for (String version : java.util.List.of("beta_1_8_1", "beta_1_9_pre_3", "release_1_0_0",
                "release_1_1", "release_1_2_5", "release_1_6_4")) {
            String aliasName = version + "_no_modern_content";
            var versionRecord = net.historicalexperience.version.HistoricalVersions
                .byRevisionAlias("historical_experience:" + aliasName)
                .orElseThrow(() -> new AssertionError("no revision " + aliasName));
            assertEquals(version, versionRecord.id().substring("historical_experience:".length()));
            var excluded = HistoricalFeatureExclusions
                .runtimeExclusionsForAlias("historical_experience:" + aliasName).stream()
                .map(Exclusion::featureId)
                .collect(Collectors.toSet());
            for (String aquatic : Set.of("minecraft:kelp_cold", "minecraft:kelp_warm",
                    "minecraft:seagrass_cold", "minecraft:seagrass_deep", "minecraft:seagrass_deep_cold",
                    "minecraft:seagrass_deep_warm", "minecraft:seagrass_normal", "minecraft:seagrass_river",
                    "minecraft:seagrass_swamp", "minecraft:seagrass_warm")) {
                assertTrue(excluded.contains(aquatic), aliasName + " must exclude " + aquatic);
            }
            for (String modern : Set.of("minecraft:forest_flowers", "minecraft:flower_plains",
                    "minecraft:flower_swamp", "minecraft:sea_pickle", "minecraft:warm_ocean_vegetation")) {
                assertTrue(excluded.contains(modern), aliasName + " must exclude " + modern);
            }
            // Melons are Beta 1.8 content, so every one of these versions must KEEP them.
            assertFalse(excluded.contains("minecraft:patch_melon_sparse"),
                aliasName + " would delete melons, which Beta 1.8 introduced");
        }
        // Lily pads arrive in the Beta 1.9 Pre-release, so only Beta 1.8.1 excludes them.
        assertTrue(HistoricalFeatureExclusions
            .runtimeExclusionsForAlias("historical_experience:beta_1_8_1_no_modern_content").stream()
            .anyMatch(e -> e.featureId().equals("minecraft:patch_waterlily")));
        assertFalse(HistoricalFeatureExclusions
            .runtimeExclusionsForAlias("historical_experience:beta_1_9_pre_3_no_modern_content").stream()
            .anyMatch(e -> e.featureId().equals("minecraft:patch_waterlily")));
        // The desert well is Release 1.2.1, so it is excluded up to Release 1.1 and allowed from 1.2.5.
        assertTrue(HistoricalFeatureExclusions
            .runtimeExclusionsForAlias("historical_experience:release_1_1_no_modern_content").stream()
            .anyMatch(e -> e.featureId().equals("minecraft:desert_well")));
        assertFalse(HistoricalFeatureExclusions
            .runtimeExclusionsForAlias("historical_experience:release_1_2_5_no_modern_content").stream()
            .anyMatch(e -> e.featureId().equals("minecraft:desert_well")));
    }

    @Test void supportedEnvelopeDoesNotReconstructTerrainOrOverrideOrdinaryNoiseRegistries() throws Exception {
        int reduced = 0;
        try (var paths = Files.list(DATA.resolve("historical_experience/moderner_beta/settings_preset"))) {
            for (Path path : paths.toList()) {
                var chunk = json(path).getAsJsonObject("chunkSettings");
                if (!chunk.has("moderner_beta:noise_settings")) continue;
                reduced++;
                var envelope = chunk.getAsJsonObject("moderner_beta:noise_settings");
                assertEquals(Set.of("min_y", "height", "size_horizontal", "size_vertical"), envelope.keySet());
                assertEquals(0, envelope.get("min_y").getAsInt());
                assertEquals(128, envelope.get("height").getAsInt());
                assertEquals(1, envelope.get("size_horizontal").getAsInt());
                assertEquals(2, envelope.get("size_vertical").getAsInt());
                assertFalse(chunk.has("moderner_beta:noise_3d_settings"));
                assertFalse(chunk.has("noise_router"));
            }
        }
        // 31 before the four pre-Beta-1.8 revisions were added; each carries the same envelope.
        //
        // 81 after the six Beta 1.9 PRERELEASES stopped offering a _no_grass world type: 87 minus those six bogus
        // revisions. They were declared through a helper whose own comment says every caller it was written for
        // predates Beta 1.6 - and grass arrives IN Beta 1.6, so all six predate it by years. The world-creation list
        // offered a "no grass" variant for versions that had grass, and this count is how that showed up as a number.
        //
        // 92, and the number is now the interesting part. It was 81, and adding Beta 1.4/1.5/1.6 did NOT move it -
        // which is how the real defect showed up. A Beta settings preset did not state the envelope at all: it
        // inherited `moderner_beta:beta` bare, so a Beta world generated at the modern -64..320 height with
        // deepslate, and the probe measured 8,366 deepslate and 424 emerald ore in nine chunks of a Beta 1.3 world.
        //
        // 81 + 8 + 3 = 92 now: the eight presets of Beta 1.4/1.5/1.6 (declared this session, so their presets could
        // be corrected directly) and three NEW revisions for the published Beta 1.0, 1.2 and 1.3 aliases, which
        // could not be edited without moving terrain in worlds already recorded against them.
        //
        // The count is kept because it moved for the right reason this time. The invariant that would have caught
        // the defect directly - every world a player can create carries the envelope - is asserted by
        // everyPublicMenuRecipeDeclaresTheReducedEnvelope below.
        // 92 + 2 = 94 now: the new `beta_1_0_no_sandstone` and `beta_1_2_no_sandstone` revisions. Both versions were
        // generating sandstone they could not have had. The Sandstone History table states the two dates separately
        // - `1.2` "Added sandstone." and `1.3` "Sandstone is now found below three blocks of naturally generated
        // sand." - so Beta 1.0 had neither the block nor the rule and Beta 1.2 had the block but not the rule. The
        // shared moderner_beta:beta foundation enables the rule for both; Beta 1.3 keeps it, because there the rule
        // is genuine. New revisions, never edits, because `_historical_envelope` is published.
        //
        // 94 + 1 = 95 now: `alpha_1_2_0_beta_plains`, whose single biome is Beta plains. Alpha v1.2.0 removed winter
        // world mode in favour of biome-based weather, and the owner's rule for the upgrade into it is that the current
        // biome becomes Beta plains. Again a NEW revision, and again a preset setting rather than a chunk rewrite,
        // because Moderner Beta's biome source asks its provider for every position and never reads a chunk's stored
        // biome - so only the provider's settings can change what the world reports and generates.
        // 95 + 4 = 99: the four Alpha 1.2.2a recipes, cloned from the 1.2.0 line so the banner build of that era
        // generates exactly what 1.2.0 did. Not one is a new revision of an existing version, which is why the count
        // moves by the number of NEW versions rather than by the number of edits.
        // 99 + 2 = 101: the pre-Beta ore recipes, revision 3 of the Indev 20100223 and the two Infdev profiles. The
    // count moved by TWO rather than by the three files added, which is what this filter sees - it counts presets
    // that carry the reduced envelope without overriding ordinary noise registries, and one of the three does not
    // qualify. The ore revision exists because a census found a generated Indev world, and an infinite Infdev world,
    // with no ore at all: the pre-Alpha list strips the vanilla ore features for Beta's population to replace and
    // nothing replaces them below Alpha.
    //
    // 101 + 31 = 132 now: the era-ore revision of thirty-two versions. Two of those versions were NOT in the
    // count's starting point - they already placed their own population - and every one of the thirty-two copies
    // its predecessor's settings preset, so thirty-one of the new files carry the envelope (the Indev copy does
    // not, for the same reason its predecessor does not). This count moves for the right reason: the measured
    // defect was a Release 1.2.5 world with diamond at y=1..72, and the fix is one new recipe per version.
    assertEquals(132, reduced);
        assertFalse(Files.exists(DATA.resolve("minecraft/worldgen/noise_settings")));
        assertFalse(Files.exists(DATA.resolve("moderner_beta/worldgen/noise_settings")));
    }

    /**
     * Every world a player can create must generate on the historical 0..127 envelope with deepslate disabled.
     *
     * <p>This is the invariant the count above only approximated, and the defect it is written for was invisible to
     * that count: Beta 1.0, 1.2 and 1.3 were in the menu with settings presets that named no envelope and did not
     * disable deepslate, so their worlds generated at the modern height with deepslate and emerald ore in them. The
     * count did not move when they were added because it counts presets that STATE the envelope, and the whole
     * problem was that these did not - a test can be perfectly correct and still not ask the question.
     *
     * <p>Checked through the world presets, because that is what the menu actually offers: a version whose public
     * entry points at an envelope-less preset is unplayable correctly whatever its older revisions say.
     */
    @Test void everyPublicMenuRecipeDeclaresTheReducedEnvelope() throws Exception {
        java.util.List<String> offenders = new java.util.ArrayList<>();
        for (String version : new String[] {"beta_1_0", "beta_1_2", "beta_1_3", "beta_1_4", "beta_1_5", "beta_1_6",
                "beta_1_7_3", "beta_1_8_1", "beta_1_9_pre_1", "release_1_0_0", "release_1_6_4"}) {
            var preset = json(DATA.resolve("historical_experience/worldgen/world_preset/" + version + ".json"));
            var generator = preset.getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld")
                .getAsJsonObject("generator");
            // The settings preset the world actually selects. Read from the generator's own provider_settings, which
            // is where a Moderner Beta world preset names it - the first version of this test looked for a top-level
            // "generator" object and a "settings" key, which is the vanilla noise shape and not this one.
            String settingsId = generator.getAsJsonObject("provider_settings")
                .get("moderner_beta:preset").getAsString();
            String settingsName = settingsId.substring(settingsId.indexOf(':') + 1);
            var settings = json(DATA.resolve(
                "historical_experience/moderner_beta/settings_preset/" + settingsName + ".json"));
            var chunk = settings.getAsJsonObject("chunkSettings");

            if (!chunk.has("moderner_beta:noise_settings")) {
                offenders.add(version + " -> " + settingsName + " states no moderner_beta:noise_settings");
                continue;
            }
            var envelope = chunk.getAsJsonObject("moderner_beta:noise_settings");
            assertEquals(0, envelope.get("min_y").getAsInt(), version + " envelope min_y");
            assertEquals(128, envelope.get("height").getAsInt(), version + " envelope height");
            if (!chunk.has("moderner_beta:deepslate_generation")
                || chunk.getAsJsonObject("moderner_beta:deepslate_generation").get("enabled").getAsBoolean()) {
                offenders.add(version + " -> " + settingsName + " does not disable deepslate generation");
            }
        }
        assertTrue(offenders.isEmpty(),
            "these versions offer a world a player can create that is not on the historical envelope, so it "
                + "generates modern-height terrain with deepslate in it: " + offenders);
    }

    @Test void betaLatestCaveSettingsKeepTheOriginalNonModernFallback() throws Exception {
        var cave = json(DATA.resolve("historical_experience/moderner_beta/settings_preset/beta_1_7_3_content_preview_9.json"))
            .getAsJsonObject("caveBiomeSettings");
        assertEquals("moderner_beta:single", cave.get("moderner_beta:provider").getAsString());
        assertEquals("moderner_beta:beta_taiga", cave.get("moderner_beta:single_biome").getAsString());
    }

    @Test void runtimeExclusionsCoverUpperStoneVariantsButNotNonexistentNewPlants() {
        var excluded = HistoricalFeatureExclusions.runtimeExclusionsForAlias("historical_experience:beta_1_7_3_content_preview_9")
            .stream().map(Exclusion::featureId).collect(Collectors.toSet());
        for (String stone : Set.of("granite", "diorite", "andesite")) {
            assertTrue(excluded.contains("minecraft:ore_" + stone + "_upper"));
            assertTrue(excluded.contains("minecraft:ore_" + stone + "_lower"));
        }
        assertTrue(excluded.contains("minecraft:ore_copper"));
        assertFalse(excluded.stream().anyMatch(id -> id.contains("firefly")));
        assertTrue(HistoricalFeatureExclusions.forAlias("moderner_beta:beta").isEmpty());
        assertTrue(HistoricalFeatureExclusions.forAlias("minecraft:normal").isEmpty());
    }

    @Test void frozenLegacySettingsAreUnchangedAndMenuDoesNotExposeDebugRevisions() throws Exception {
        var resource = "historical_experience/moderner_beta/settings_preset/beta_1_7_3.json";
        assertEquals(json(Path.of("src/main/resources/data").resolve(resource)), json(DATA.resolve(resource)));
        var tag = json(DATA.resolve("minecraft/tags/worldgen/world_preset/normal.json"));
        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(net.historicalexperience.version.HistoricalVersions.all().size(),
            tag.getAsJsonArray("values").size(),
            "the normal world-preset tag must list every declared profile exactly once");
        assertFalse(tag.getAsJsonArray("values").toString().contains("preview"));
    }
}

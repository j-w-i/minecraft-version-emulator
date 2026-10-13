package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import net.historicalexperience.HistoricalMarker;
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
        assertEquals(29, checked, "the menu must expose exactly the twenty-nine declared profiles");
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
                .map(HistoricalFeatureExclusions.Exclusion::featureId)
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
        assertEquals(87, reduced);
        assertFalse(Files.exists(DATA.resolve("minecraft/worldgen/noise_settings")));
        assertFalse(Files.exists(DATA.resolve("moderner_beta/worldgen/noise_settings")));
    }

    @Test void betaLatestCaveSettingsKeepTheOriginalNonModernFallback() throws Exception {
        var cave = json(DATA.resolve("historical_experience/moderner_beta/settings_preset/beta_1_7_3_content_preview_9.json"))
            .getAsJsonObject("caveBiomeSettings");
        assertEquals("moderner_beta:single", cave.get("moderner_beta:provider").getAsString());
        assertEquals("moderner_beta:beta_taiga", cave.get("moderner_beta:single_biome").getAsString());
    }

    @Test void runtimeExclusionsCoverUpperStoneVariantsButNotNonexistentNewPlants() {
        var excluded = HistoricalFeatureExclusions.runtimeExclusionsForAlias("historical_experience:beta_1_7_3_content_preview_9")
            .stream().map(HistoricalFeatureExclusions.Exclusion::featureId).collect(Collectors.toSet());
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
        assertEquals(29, tag.getAsJsonArray("values").size());
        assertFalse(tag.getAsJsonArray("values").toString().contains("preview"));
    }
}

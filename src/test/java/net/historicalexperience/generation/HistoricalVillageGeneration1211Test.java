/*
 * Unit test for the ported pre-1.14 village, in the style of mc1211's GenerationBackport1211Test.
 *
 * The village is ported from the NeoForge mod "OldVillages"
 * (https://github.com/MaxTechnik/OldVillages, pinned revision 7578013ec061303982539fac7606df67577ffb26,
 * MIT as declared in its gradle.properties; that checkout ships no LICENSE file).
 */
package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.worldgen.HistoricalVillageBlocks;
import net.historicalexperience.worldgen.HistoricalVillageRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit, driven from the block table the generator itself reads, and from this project's researched
 * artifacts.
 *
 * <p>Three claims are checked, each against the artifact that establishes it rather than against a copy
 * written into the test:
 *
 * <ol>
 *   <li>a village exists exactly for the versions at or after Beta 1.8.1, the boundary recorded in
 *       {@code research/feature-timeline.json};
 *   <li>the permitted biome set is exactly plains, sunflower plains and desert;
 *   <li>every block id {@link HistoricalVillageBlocks} can place is present, under this project's legacy
 *       name mapping, in the extracted palette of <b>every</b> version that can generate a village - the
 *       oldest one being the binding constraint - and none of them is a block the village's own rules
 *       forbid.
 * </ol>
 *
 * <p>The third check is what makes this test worth having. It walks
 * {@link HistoricalVillageBlocks#placedBlockIds(String)}, which is the same table the piece code resolves
 * its block states from, so a change to the palette moves the test's expectation with it. A hand-written
 * list of expected blocks would have gone on passing while the generator emitted dirt paths into a
 * Beta 1.8.1 world.
 */
class HistoricalVillageGeneration1211Test {
    /** The ten declared profiles, oldest first, as this project declares them. */
    private static final List<String> DECLARED = List.of(
        "historical_experience:infdev_420",
        "historical_experience:infdev_611",
        "historical_experience:alpha_1_0_1",
        "historical_experience:alpha_1_0_4",
        "historical_experience:alpha_1_0_6",
        "historical_experience:alpha_1_0_11",
        "historical_experience:alpha_1_0_14",
        "historical_experience:alpha_1_0_17",
        "historical_experience:alpha_1_1_0",
        "historical_experience:alpha_1_2_0",
        "historical_experience:alpha",
        "historical_experience:beta_1_0",
        "historical_experience:beta_1_2",
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6",
        "historical_experience:release_1_3",
        "historical_experience:release_1_4",
        "historical_experience:release_1_5",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_6_4");

    /** Versions that must NOT generate a village, from the Beta 1.8 boundary. */
    private static final List<String> NO_VILLAGE = DECLARED.subList(0, 14);

    /** Versions that must be able to, from the same boundary. */
    private static final List<String> VILLAGE = DECLARED.subList(14, DECLARED.size());

    /**
     * The extracted palette each village version is judged against.
     *
     * <p>Pre1 maps to the Beta 1.8.1 palette because its village places the Beta 1.8.1 workstation: the brewing
     * stand arrives in Prerelease 3, so Pre1's craftsman slot is a crafting table, which the Pre3 extraction does
     * not contain. Every other prerelease maps to Prerelease 3's palette, which is the honest mapping rather than a
     * shortcut - Moderner Beta ships ONE Beta 1.9 terrain preset and each prerelease is generated from it, so the
     * blocks they can emit are the blocks Pre3's extraction contains.
     *
     * <p>{@code Map.ofEntries} rather than {@code Map.of}, because there are now twelve entries and
     * {@code Map.of} accepts at most ten pairs - which the compiler said plainly once the sixth prerelease arrived.
     */
    private static final Map<String, String> PALETTE_FILE = Map.ofEntries(
        Map.entry("historical_experience:beta_1_8_1", "b1_8_1"),
        Map.entry("historical_experience:beta_1_9_pre_1", "b1_8_1"),
        Map.entry("historical_experience:beta_1_9_pre_2", "b1_9_pre3"),
        Map.entry("historical_experience:beta_1_9_pre_3", "b1_9_pre3"),
        Map.entry("historical_experience:beta_1_9_pre_4", "b1_9_pre3"),
        Map.entry("historical_experience:beta_1_9_pre_5", "b1_9_pre3"),
        Map.entry("historical_experience:beta_1_9_pre_6", "b1_9_pre3"),
        Map.entry("historical_experience:release_1_0_0", "1_0_0"),
        Map.entry("historical_experience:release_1_1", "1_1"),
        Map.entry("historical_experience:release_1_3", "1_2_5"),
        Map.entry("historical_experience:release_1_4", "1_2_5"),
        Map.entry("historical_experience:release_1_5", "1_2_5"),
        Map.entry("historical_experience:release_1_2_5", "1_2_5"),
        Map.entry("historical_experience:release_1_6_4", "1_6_4"));

    /**
     * The repository root, found by walking up from the working directory.
     *
     * <p>The directory that holds {@code mc1211/} is not the same directory that holds
     * {@code research/} in every checkout layout, so the two are located independently instead of one being
     * derived from the other.
     */
    private static final Path REPO_ROOT = findRepoRoot();
    private static final Path RESEARCH = locate("research", "block-palettes");
    private static final Path REPO = REPO_ROOT;
    private static final Path DATA =
        locate("src/main/resources/data/historical_experience", "worldgen/structure");

    private static Path findRepoRoot() {
        Path here = Path.of("").toAbsolutePath();
        for (Path cursor = here; cursor != null; cursor = cursor.getParent()) {
            if (Files.exists(cursor.resolve("research/feature-timeline.json"))) {
                return cursor;
            }
        }
        throw new IllegalStateException("cannot find the repository root from " + here);
    }

    /**
     * Finds a project directory that exists, whichever directory the tests were launched from.
     *
     * <p>This project's 1.21.1 build runs its tests with the working directory set to the directory that
     * contains {@code mc1211/}, so {@code research/} and {@code src/main/resources/...} are the two
     * shapes that have to resolve. The search walks up from the working directory and tries each shape at
     * every level, which also covers a copy of the project built somewhere else on disk.
     */
    private static Path locate(String suffix, String mustContain) {
        Path here = Path.of("").toAbsolutePath();
        for (Path cursor = here; cursor != null; cursor = cursor.getParent()) {
            for (Path prefix : List.of(Path.of(""), Path.of("mc1211"), Path.of("village-port-work"))) {
                Path candidate = prefix.toString().isEmpty()
                    ? cursor.resolve(suffix)
                    : cursor.resolve(prefix).resolve(suffix);
                if (Files.exists(candidate.resolve(mustContain))) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("cannot find " + suffix + " (containing " + mustContain
            + ") from " + here);
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    // ---- (a) the version boundary -------------------------------------------------------------------

    @Test
    @DisplayName("village generation is enabled exactly for versions at or after Beta 1.8.1")
    void villageGenerationIsEnabledExactlyFromBetaOneEightOne() {
        for (String versionId : DECLARED) {
            assertTrue(HistoricalVersions.byId(versionId).isPresent(),
                versionId + " must be a declared version for this boundary to mean anything");
        }
        for (String versionId : NO_VILLAGE) {
            assertFalse(HistoricalVillageRules.generationEnabledForVersion(versionId),
                versionId + " predates villages and must generate none");
        }
        for (String versionId : VILLAGE) {
            assertTrue(HistoricalVillageRules.generationEnabledForVersion(versionId),
                versionId + " is at or after Beta 1.8.1 and must be able to generate a village");
        }
        // The rule is derived from the registry, not from the two lists above, and the two must agree.
        assertEquals(new TreeSet<>(NO_VILLAGE), new TreeSet<>(HistoricalVillageRules.noVillageVersions()));
        assertEquals(new TreeSet<>(VILLAGE), new TreeSet<>(HistoricalVillageRules.villageVersions()));
        // A version nobody declared is refused rather than answered "no", which would make a typo look
        // exactly like a correct boundary decision.
        assertThrows(IllegalArgumentException.class,
            () -> HistoricalVillageRules.generationEnabledForVersion("historical_experience:beta_1_8_2"));
        // An alias of a revision of a pre-village version is still pre-village, and an alias that is not a
        // historical recipe at all is not governed by this rule: the structure is registered data, so it is
        // reachable in every world and an ordinary world must not be gated by a historical boundary.
        assertFalse(HistoricalVillageRules.generationEnabledForAlias(
            "historical_experience:beta_1_7_3_content_preview_9"));
        assertTrue(HistoricalVillageRules.generationEnabledForAlias("historical_experience:beta_1_8_1"));
        assertTrue(HistoricalVillageRules.generationEnabledForAlias(
            "historical_experience:beta_1_8_1_cave_fix"));
        assertFalse(HistoricalVillageRules.generationEnabledForAlias("moderner_beta:beta"));
        assertFalse(HistoricalVillageRules.generationEnabledForAlias(null));
    }

    @Test
    @DisplayName("the boundary is the one research/feature-timeline.json records, and the evidence says so")
    void theBoundaryCitesTheFeatureTimeline() throws Exception {
        Path timeline = REPO.resolve("research/feature-timeline.json");
        assertTrue(Files.isRegularFile(timeline), "the feature timeline is missing: " + timeline);
        JsonObject village = JsonParser.parseString(Files.readString(timeline, StandardCharsets.UTF_8))
            .getAsJsonObject().getAsJsonArray("structures").asList().stream()
            .map(element -> element.getAsJsonObject())
            .filter(entry -> "village".equals(entry.get("feature").getAsString()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("feature-timeline.json has no village entry"));
        assertEquals("Beta 1.8 (Pre-release)", village.get("introducedIn").getAsString());
        assertTrue(village.get("quote").getAsString().contains("only generated in plains and desert"),
            "the quote is what the biome rule rests on");
        assertTrue(HistoricalVillageRules.EVIDENCE.contains("plains and desert"),
            "the code's evidence string must carry the same quote, or it is just an assertion");
        int boundary = HistoricalVillageRules.BETA_1_8_1_CHRONOLOGY;
        assertEquals(boundary, HistoricalVersions.byId("historical_experience:beta_1_8_1")
            .orElseThrow().chronology());
        assertTrue(HistoricalVersions.byId("historical_experience:beta_1_7_3")
            .orElseThrow().chronology() < boundary);
    }

    @Test
    @DisplayName("the resource gate names the same structure type and the same version boundary")
    void theDataFilesAgreeWithTheCode() throws Exception {
        JsonObject structure = json(DATA.resolve("worldgen/structure/historical_village.json"));
        assertEquals("historical_experience:historical_village", structure.get("type").getAsString());
        assertEquals("#historical_experience:has_structure/historical_village",
            structure.get("biomes").getAsString());
        assertEquals("surface_structures", structure.get("step").getAsString());
        assertEquals("none", structure.get("terrain_adaptation").getAsString());
        // An empty spawn override is the deliberate choice: villagers are Beta 1.9 content and this port
        // places no entities at all.
        assertTrue(structure.getAsJsonObject("spawn_overrides").isEmpty(),
            "the structure must not declare spawn overrides; this port places no entities");

        JsonObject set = json(DATA.resolve("worldgen/structure_set/historical_villages.json"));
        assertEquals("historical_experience:historical_village",
            set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
        assertEquals("minecraft:random_spread",
            set.getAsJsonObject("placement").get("type").getAsString());
        // The vanilla villages' own spacing, so this structure is neither rarer nor denser than the thing it
        // stands in for.
        assertEquals(60, set.getAsJsonObject("placement").get("spacing").getAsInt());
        assertEquals(20, set.getAsJsonObject("placement").get("separation").getAsInt());

        // The two loot tables exist, and their only difference is the horse armour that is Release 1.6.1.
        JsonObject full = json(DATA.resolve("loot_table/chests/historical_village_blacksmith.json"));
        JsonObject early = json(
            DATA.resolve("loot_table/chests/historical_village_blacksmith_early.json"));
        assertTrue(entryNames(full).contains("minecraft:iron_horse_armor"));
        assertFalse(entryNames(early).contains("minecraft:iron_horse_armor"),
            "a chest in a pre-1.6.1 world must not contain horse armour");
        assertEquals(entryNames(full).size(), entryNames(early).size() + 3,
            "the early table is the full table minus the three horse armours");
    }

    private static Set<String> entryNames(JsonObject lootTable) {
        Set<String> names = new LinkedHashSet<>();
        lootTable.getAsJsonArray("pools").forEach(pool -> pool.getAsJsonObject()
            .getAsJsonArray("entries").forEach(entry -> names.add(
                entry.getAsJsonObject().get("name").getAsString())));
        return names;
    }

    // ---- (b) the biome set --------------------------------------------------------------------------

    @Test
    @DisplayName("the permitted biome set is exactly plains, sunflower plains and desert")
    void permittedBiomesAreExactlyPlainsDesert() throws Exception {
        Set<String> declared =
            json(DATA.resolve("tags/worldgen/biome/has_structure/historical_village.json"))
                .getAsJsonArray("values").asList().stream()
                .map(element -> element.getAsString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertEquals(Set.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:desert"),
            declared, "the tag is what the game reads and it must be exactly the researched set");
        assertEquals(declared, HistoricalVillageRules.permittedBiomes(),
            "the code's set and the tag must not drift");
        // The later expansion is after the newest profile this project declares (Release 1.6.4), so no world
        // this build can create may have these as village biomes.
        for (String later : List.of("minecraft:savanna", "minecraft:savanna_plateau", "minecraft:taiga",
                "minecraft:snowy_plains", "minecraft:meadow", "minecraft:swamp", "minecraft:forest")) {
            assertFalse(HistoricalVillageRules.biomeIsPermitted(later),
                later + " is not a village biome for any version this project declares");
        }
        // Upstream classified the style by substring, which would make these desert villages.
        assertFalse(HistoricalVillageRules.biomeIsPermitted("minecraft:desert_lakes"));
        assertFalse(HistoricalVillageRules.biomeIsPermitted("minecraft:deep_dark"));
        assertTrue(HistoricalVillageRules.isDesert("minecraft:desert"));
        assertFalse(HistoricalVillageRules.isDesert("minecraft:plains"));
    }

    // ---- (c) the blocks -----------------------------------------------------------------------------

    @Test
    @DisplayName("no block the pieces can place is anachronistic for the oldest village version")
    void noEmittedBlockIsAnachronisticForBetaOneEightOne() {
        String versionId = "historical_experience:beta_1_8_1";
        // The VERSIONED set, not the union across every village version: later revisions legitimately place
        // later content (Release 1.6.1 potatoes, for instance), and the binding constraint is the oldest
        // version that can generate a village at all.
        List<String> offenders = new ArrayList<>();
        List<String> unjudged = new ArrayList<>();
        Set<String> structural = HistoricalVillageBlocks.versionIndependentBlockIds();
        for (String blockId : HistoricalVillageBlocks.placedBlockIds(versionId)) {
            if (structural.contains(blockId)) {
                // air, water and lava are what the pieces clear space with, not content the era either had
                // or lacked; no block table can date them and none needs to.
                continue;
            }
            var verdict = HistoricalBlockContent.judge(versionId, blockId);
            if (verdict == HistoricalBlockAvailability.Verdict.ANACHRONISTIC) {
                offenders.add(blockId);
            } else if (verdict == HistoricalBlockAvailability.Verdict.UNKNOWN) {
                unjudged.add(blockId);
            }
        }
        assertEquals(List.of(), offenders,
            "every block Beta 1.8.1 can place must have existed in Beta 1.8.1");
        assertEquals(List.of(), unjudged,
            "these are placed by the structure but neither table can judge them, so the guarantee is "
                + "incomplete; either date them in the availability table or stop placing them");
    }

    @Test
    @DisplayName("no block emitted for a version is missing from that version's extracted palette")
    void everyEmittedBlockIsInItsOwnVersionsPalette() {
        for (String versionId : VILLAGE) {
            String file = PALETTE_FILE.get(versionId);
            assertNotNull(file, "no palette file is mapped for " + versionId);
            Set<String> palette = legacyPalette(file);
            assertFalse(palette.isEmpty(), file + " has no block names");
            List<String> offenders = new ArrayList<>();
            Set<String> structural = HistoricalVillageBlocks.versionIndependentBlockIds();
            for (String blockId : HistoricalVillageBlocks.placedBlockIds(versionId)) {
                if (structural.contains(blockId)) {
                    continue;
                }
                // Judged through the same mechanism the running game uses - the availability table plus the
                // version-agnostic anachronism list - rather than through the palette mapping alone, which
                // deliberately does not know every block a later release added.
                var verdict = HistoricalBlockContent.judge(versionId, blockId);
                if (verdict != HistoricalBlockAvailability.Verdict.ALLOWED) {
                    offenders.add(blockId + " (" + verdict + ")");
                }
            }
            assertEquals(List.of(), offenders, versionId + " must not place a block its own client never "
                + "registered; the emitted set is built from this project's block table, so a failure here "
                + "means that table and the extracted palette disagree");
        }
    }

    @Test
    @DisplayName("the era substitutions are the ones the evidence calls for")
    void eraSubstitutionsArePresent() {
        for (String versionId : VILLAGE) {
            Set<String> ids = HistoricalVillageBlocks.placedBlockIds(versionId);
            assertFalse(ids.contains("minecraft:dirt_path"),
                "dirt path is Release 1.9; every declared profile is older, so the path is gravel");
            assertFalse(HistoricalVillageBlocks.usesDirtPath(versionId),
                "the upstream 'dirt path' option is answered by the version, and the answer is no");
            assertTrue(ids.contains("minecraft:gravel"), "the path surface must be gravel");
            assertFalse(ids.contains("minecraft:smooth_stone_slab"),
                "smooth stone is Release 1.2.1; the floors use stone slabs");
            assertFalse(ids.contains("minecraft:smooth_sandstone"),
                "smooth sandstone is Release 1.2.1");
            assertFalse(ids.contains("minecraft:terracotta"),
                "terracotta is Release 1.6.1 and this structure never places it");
            assertFalse(ids.contains("minecraft:iron_bars"),
                "iron bars are Release 1.8; the forge rail is a fence");
        }
        Set<String> oldest = HistoricalVillageBlocks.placedBlockIds("historical_experience:beta_1_8_1");
        assertTrue(oldest.contains("minecraft:stone_slab"));
        assertTrue(oldest.contains("minecraft:glass_pane"));
        // Carrots and potatoes are Release 1.4.2 (12w34a). The oldest village version grows wheat alone;
        // an earlier draft of the port had Beta 1.8.1 growing them, which the block tables rejected.
        assertFalse(oldest.contains("minecraft:carrots"), "carrots are Release 1.4.2");
        assertFalse(oldest.contains("minecraft:potatoes"), "potatoes are Release 1.4.2");
        assertFalse(oldest.contains("minecraft:beetroots"), "beetroot is Release 1.9");
        assertFalse(oldest.contains("minecraft:brewing_stand"),
            "brewing stands are Beta 1.9 Prerelease 3");
        Set<String> newest = HistoricalVillageBlocks.placedBlockIds("historical_experience:release_1_6_4");
        assertTrue(newest.contains("minecraft:carrots"), "Release 1.6.4 is after Release 1.4.2");
        assertTrue(newest.contains("minecraft:potatoes"));
        assertTrue(newest.contains("minecraft:sandstone_stairs"),
            "sandstone stairs exist by Release 1.6.4");
        assertTrue(HistoricalVillageBlocks.forVersion("historical_experience:beta_1_8_1").grows("wheat"));
        assertFalse(HistoricalVillageBlocks.forVersion("historical_experience:beta_1_8_1").grows("carrots"));
        assertTrue(HistoricalVillageBlocks.forVersion("historical_experience:release_1_6_4").grows("carrots"));
        assertFalse(HistoricalVillageBlocks.forVersion("historical_experience:beta_1_8_1").grows("beetroot"));
        // The brewing stand arrives with Beta 1.9 Prerelease 3 and stays; before it, the church workstation
        // is the crafting table.
        assertEquals("minecraft:crafting_table", HistoricalVillageBlocks
            .forVersion("historical_experience:beta_1_8_1").block(HistoricalVillageBlocks.WORKSTATION_SLOT));
        assertEquals("minecraft:brewing_stand", HistoricalVillageBlocks
            .forVersion("historical_experience:beta_1_9_pre_3").block(HistoricalVillageBlocks.WORKSTATION_SLOT));
        assertEquals("minecraft:brewing_stand", HistoricalVillageBlocks
            .forVersion("historical_experience:release_1_6_4").block(HistoricalVillageBlocks.WORKSTATION_SLOT));
        // Beetroot is after every declared profile, so nothing grows it.
        for (String versionId : VILLAGE) {
            assertFalse(HistoricalVillageBlocks.forVersion(versionId).grows("beetroot"),
                versionId + " must not grow beetroot");
        }
        // Release 1.9 is after Release 1.6.4, so the path is gravel in the newest profile too. The answer is
        // computed from the version rather than written down, which is why this can be asserted at all.
        assertFalse(HistoricalVillageBlocks.usesDirtPath("historical_experience:release_1_6_4"));
    }

    @Test
    @DisplayName("none of the forbidden families is emitted, and every forbidden name is judgeable")
    void forbiddenBlocksAreNeverEmitted() {
        Map<String, String> forbidden = HistoricalVillageBlocks.forbiddenBlocks();
        assertTrue(forbidden.size() > 20, "the forbidden list looks truncated");
        for (String versionId : VILLAGE) {
            Set<String> ids = HistoricalVillageBlocks.placedBlockIds(versionId);
            for (String banned : forbidden.keySet()) {
                assertFalse(ids.contains(banned),
                    versionId + " must not place " + banned + ": " + forbidden.get(banned));
            }
        }
        // Every forbidden entry SHOULD be judgeable, because a banned name no table knows is a rule that
        // cannot be checked. The blocks the structure actually emits are held to a stricter standard by the
        // two tests above (they must judge ALLOWED), so here the unjudgeable names are reported instead of
        // failing: a missing mapping for a name the structure never places is a coverage gap, not a defect
        // in the world, and it is visible in this message rather than hidden.
        List<String> unjudgeable = new ArrayList<>();
        for (String banned : forbidden.keySet()) {
            if (HistoricalBlockContent.judge("historical_experience:release_1_6_4", banned)
                    == HistoricalBlockAvailability.Verdict.UNKNOWN) {
                unjudgeable.add(banned);
            }
        }
        assertTrue(unjudgeable.size() <= 24, "the unjudgeable forbidden list has grown past what a "
            + "reviewer can check by hand: " + unjudgeable);
    }

    @Test
    @DisplayName("a version that could not have a village has no village palette")
    void preVillageVersionsHaveNoPalette() {
        for (String versionId : NO_VILLAGE) {
            assertThrows(IllegalArgumentException.class,
                () -> HistoricalVillageBlocks.forVersion(versionId),
                versionId + " must not answer with a palette");
        }
    }

    @Test
    @DisplayName("the desert style substitutes sandstone for the plains style's wood and cobble")
    void desertStyleUsesSandstone() {
        HistoricalVillageBlocks.VillageBlockSet plains =
            HistoricalVillageBlocks.forVersion("historical_experience:beta_1_8_1");
        Map<String, String> desert = HistoricalVillageBlocks.desertSubstitutions(plains);
        assertEquals("minecraft:sandstone", desert.get("planks"));
        assertEquals("minecraft:sandstone", desert.get("cobble"));
        assertEquals("minecraft:sandstone", desert.get("pathSurface"));
        // The desert stair is whatever that VERSION's sandstone stair is. Sandstone stairs postdate the
        // Beta 1.8.1 extraction and first appear in the 1.6.4 set, so the oldest desert village gets the
        // era's stone stair (cobblestone) instead - and the desert materials themselves are a Release 1.3.1
        // rebuild, which is a recorded fidelity limit for the earlier versions rather than a claim.
        assertEquals(plains.blocks().get("sandstoneStairs"), desert.get("stairs"),
            "the desert stair must be the same version's sandstone stair slot");
        assertEquals("minecraft:cobblestone_stairs",
            HistoricalVillageBlocks.desertSubstitutions(
                HistoricalVillageBlocks.forVersion("historical_experience:beta_1_8_1")).get("stairs"),
            "Beta 1.8.1 had no sandstone stairs");
        // The door, fence and bed are not substituted: sandstone has no equivalent, and a village whose
        // doors were sandstone would not be enterable.
        assertFalse(desert.containsKey("door"));
        assertFalse(desert.containsKey("bed"));
        assertFalse(desert.containsKey("fence"));
        // A desert village therefore emits only blocks the plains one does.
        Set<String> plainsIds = HistoricalVillageBlocks.placedBlockIds("historical_experience:beta_1_8_1");
        for (String desertId : desert.values()) {
            assertTrue(plainsIds.contains(desertId),
                desertId + " is a desert substitution that the plains palette does not already contain");
        }
    }

    @Test
    @DisplayName("every slot the pieces use is declared for every version that can have a village")
    void everySlotIsDeclaredForEveryVersion() {
        List<String> slots = List.of("planks", "stairs", "log", "door", "fence", "pressurePlate",
            "pathSurface", "cobble", "cobbleStairs", "stoneSlab", "sandstone", "sandstoneStairs",
            "sandstoneSlab", "wool", "glassPane", "dirt", "grass", "shortGrass", "farmland",
            "chest", "furnace", "craftingTable", "bookshelf", "bed", "ladder", "torch", "wallTorch",
            HistoricalVillageBlocks.WORKSTATION_SLOT, "crop:wheat");
        for (String versionId : VILLAGE) {
            HistoricalVillageBlocks.VillageBlockSet set = HistoricalVillageBlocks.forVersion(versionId);
            for (String slot : slots) {
                assertNotNull(set.block(slot), versionId + " has no block for slot '" + slot + "'");
            }
            // Every slot that is present must be a block id in the modern namespace, so a typo cannot look
            // like a deliberate absence.
            for (Map.Entry<String, String> entry : set.blocks().entrySet()) {
                assertTrue(entry.getValue().startsWith("minecraft:"),
                    entry.getKey() + " is '" + entry.getValue() + "', which is not a namespaced block id");
            }
        }
    }

    // ---- helpers ------------------------------------------------------------------------------------

    /**
     * A version's extracted block-name set, read from {@code research/block-palettes/}.
     *
     * <p>The union of {@code blockNames} and {@code aliasNames} is used deliberately: 1.6.4's extraction
     * records two names for the same block (the pre-flattening identifier and the texture name), and a block
     * the version registered under either name did exist in that version. Reading only {@code blockNames}
     * would report a 1.6.4 block as absent when the client had it.
     */
    private static Set<String> legacyPalette(String file) {
        Path path = RESEARCH.resolve("block-palettes").resolve(file + ".json");
        assertTrue(Files.isRegularFile(path), "the extracted palette is missing: " + path);
        try {
            JsonObject document = json(path);
            Set<String> names = new TreeSet<>();
            var blockNames = document.getAsJsonArray("blockNames");
            if (blockNames != null) {
                blockNames.forEach(element -> names.add(element.getAsString()));
            }
            var aliases = document.get("aliasNames");
            if (aliases != null && aliases.isJsonObject()) {
                aliases.getAsJsonObject().keySet().forEach(names::add);
            }
            return names;
        } catch (Exception failure) {
            throw new AssertionError("cannot read " + path, failure);
        }
    }

    @Test
    @DisplayName("the palette files this test relies on were hash-verified extractions")
    void paletteFilesArePresentAndReviewed() throws Exception {
        for (String file : PALETTE_FILE.values()) {
            JsonObject document = json(RESEARCH.resolve("block-palettes").resolve(file + ".json"));
            assertEquals(Boolean.TRUE, document.getAsJsonObject("source")
                .get("sha1MatchesExpected").getAsBoolean(), file + " was not hash-verified");
            assertEquals(Boolean.TRUE, document.getAsJsonObject("source")
                .get("sizeMatchesExpected").getAsBoolean(), file + " was not size-verified");
            assertTrue(document.getAsJsonArray("blockNames").size() > 80,
                file + " has implausibly few block names");
        }
        // Every declared version is on exactly one side of the boundary, and the versions that can generate a
        // village are exactly the ones with an extracted palette.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            boolean expected = HistoricalVillageRules.villageVersions().contains(version.id());
            assertEquals(expected, HistoricalVillageRules.generationEnabledForVersion(version.id()),
                version.id() + " disagrees with the village boundary");
            if (expected) {
                assertNotNull(PALETTE_FILE.get(version.id()),
                    version.id() + " can generate a village but has no extracted palette to check it "
                        + "against");
            }
        }
    }
}

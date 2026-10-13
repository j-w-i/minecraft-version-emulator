package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.Test;

/**
 * Every declared profile is judged for blocks AND structures, and both judgements are cross-checked
 * against the reviewed research artifacts rather than trusted.
 *
 * <p>This test exists because the previous state of the project could not fail in the way that mattered:
 * the block check was hard-coded to one Beta 1.7.3 revision, and the structure rules covered only the four
 * pre-Beta-1.8 profiles. A real Alpha world therefore contained ten vines and a Beta 1.7.3 world could
 * generate a Nether fortress, and nothing in the suite objected.
 */
class ProfileGenerationRules1211Test {
    /** The repository root, which is the test working directory (set in mc1211/build.gradle). */
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static final Map<String, String> PALETTE_FILES = Map.of(
        "historical_experience:beta_1_7_3", "b1_7_3",
        "historical_experience:beta_1_8_1", "b1_8_1",
        "historical_experience:beta_1_9_pre_3", "b1_9_pre3",
        "historical_experience:release_1_0_0", "1_0_0",
        "historical_experience:release_1_1", "1_1",
        "historical_experience:release_1_2_5", "1_2_5",
        "historical_experience:release_1_6_4", "1_6_4");

    private static Set<String> palette(String versionId) {
        String file = PALETTE_FILES.get(versionId);
        if (file == null) {
            return null;
        }
        Path path = ROOT.resolve("research/block-palettes/" + file + ".json");
        assertTrue(Files.isRegularFile(path), "missing reviewed palette " + path);
        try {
            JsonObject document = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            Set<String> names = new java.util.LinkedHashSet<>();
            // The artifacts disagree about the shape of the unused alias list: some carry [] and some {}.
            // Read only what is really an array rather than assuming, so a shape difference cannot be
            // mistaken for a missing palette.
            var primary = document.get("blockNames");
            assertTrue(primary != null && primary.isJsonArray(), path + " has no blockNames array");
            for (var entry : primary.getAsJsonArray()) {
                names.add(entry.getAsString());
            }
            var aliases = document.get("aliasNames");
            if (aliases != null && aliases.isJsonArray()) {
                for (var entry : aliases.getAsJsonArray()) {
                    names.add(entry.getAsString());
                }
            }
            return names;
        } catch (Exception failure) {
            throw new AssertionError("could not read " + path, failure);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Structures
    // ---------------------------------------------------------------------------------------------

    @Test void everyDeclaredVersionHasASourcedStructureRule() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(HistoricalStructureRules.ruleForVersion(version.id()).isPresent(),
                version.id() + " has no structure rule, so later structures could generate in it");
        }
        assertEquals(HistoricalVersions.all().size(), HistoricalStructureRules.declaredVersions().size(),
            "the rule table and the version registry disagree about how many profiles exist");
    }

    @Test void noProfileMayGenerateAStructureItsEraLacked() {
        // Mineshafts, villages and strongholds: Beta 1.8 Pre-release. Nothing earlier has any structure.
        for (String earlier : List.of("historical_experience:infdev_420",
                "historical_experience:infdev_611", "historical_experience:alpha",
                "historical_experience:beta_1_7_3")) {
            assertTrue(HistoricalStructureRules.allowedForVersion(earlier).isEmpty(),
                earlier + " must generate no structure-system structure");
            assertTrue(HistoricalStructureRules.suppresses(earlier, "minecraft:mineshaft"));
            assertTrue(HistoricalStructureRules.suppresses(earlier, "minecraft:stronghold"));
            assertTrue(HistoricalStructureRules.suppresses(earlier, "minecraft:village_plains"));
            assertTrue(HistoricalStructureRules.suppresses(earlier, "minecraft:fortress"));
        }
        var beta181 = HistoricalStructureRules.allowedForVersion("historical_experience:beta_1_8_1");
        // The version's villages are this project's own structure; the vanilla jigsaw village ids are
        // suppressed in its favour and are therefore NOT in any allowed set.
        assertEquals(Set.of("minecraft:mineshaft", "minecraft:stronghold",
            HistoricalStructureRules.HISTORICAL_VILLAGE), beta181);
        assertTrue(HistoricalStructureRules.suppresses("historical_experience:beta_1_8_1",
            "minecraft:fortress"), "Nether fortresses are Beta 1.9 Prerelease content");
        for (String withFortress : List.of("historical_experience:beta_1_9_pre_3",
                "historical_experience:release_1_0_0", "historical_experience:release_1_1",
                "historical_experience:release_1_2_5")) {
            assertTrue(HistoricalStructureRules.allowedForVersion(withFortress)
                .contains("minecraft:fortress"), withFortress + " has Nether fortresses");
            assertFalse(HistoricalStructureRules.allowedForVersion(withFortress)
                .contains("minecraft:desert_pyramid"), withFortress + " predates the desert pyramid");
        }
        var release164 = HistoricalStructureRules.allowedForVersion("historical_experience:release_1_6_4");
        assertTrue(release164.containsAll(Set.of("minecraft:desert_pyramid", "minecraft:jungle_pyramid",
            "minecraft:swamp_hut")), "Release 1.6.4 has the 1.3.1 and 1.4.2 structures");
        assertFalse(release164.contains("minecraft:monument"), "the ocean monument is Release 1.8");
    }

    @Test void laterStructuresAreSuppressedInEveryProfileAndEarlierOnesAreNot() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            String id = version.id();
            // Every structure introduced after Release 1.6.4 - the newest profile declared - is suppressed.
            for (String later : List.of("minecraft:mansion", "minecraft:monument", "minecraft:igloo",
                    "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm", "minecraft:shipwreck",
                    "minecraft:shipwreck_beached", "minecraft:buried_treasure", "minecraft:pillager_outpost",
                    "minecraft:ruined_portal", "minecraft:ruined_portal_desert",
                    "minecraft:ruined_portal_jungle", "minecraft:ruined_portal_mountain",
                    "minecraft:ruined_portal_nether", "minecraft:ruined_portal_ocean",
                    "minecraft:ruined_portal_swamp", "minecraft:ancient_city", "minecraft:bastion_remnant",
                    "minecraft:trail_ruins", "minecraft:trial_chambers", "minecraft:end_city",
                    "minecraft:nether_fossil")) {
                assertTrue(HistoricalStructureRules.suppresses(id, later),
                    id + " must not generate " + later);
            }
            // The badlands mineshaft is Release 1.7.2 content (the mesa biome itself), so no profile has it.
            assertTrue(HistoricalStructureRules.suppresses(id, "minecraft:mineshaft_mesa"),
                id + " must not generate a mesa mineshaft: the mesa biome is Release 1.7.2");
            // Villages only generated in plains and desert until long after the newest declared profile.
            for (String variant : List.of("minecraft:village_savanna", "minecraft:village_taiga",
                    "minecraft:village_snowy")) {
                assertTrue(HistoricalStructureRules.suppresses(id, variant),
                    id + " must not generate " + variant);
            }
        }
    }

    @Test void everyVanillaStructureIsClassifiedForEveryProfile() {
        assertFalse(HistoricalStructureRules.VANILLA_STRUCTURE_IDS.isEmpty());
        for (HistoricalVersion version : HistoricalVersions.all()) {
            Set<String> allowed = HistoricalStructureRules.allowedForVersion(version.id());
            // A typo in an allowed id would silently suppress a structure the profile should have.
            for (String allowedId : allowed) {
                assertTrue(HistoricalStructureRules.VANILLA_STRUCTURE_IDS.contains(allowedId)
                        || HistoricalStructureRules.HISTORICAL_VILLAGE.equals(allowedId),
                    allowedId + " is not a structure the pinned 1.21.1 jar registers, so the allowed set "
                        + "for " + version.id() + " names something that can never generate");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Blocks
    // ---------------------------------------------------------------------------------------------

    /**
     * The generated table must agree with the version registry and with the reviewed palettes.
     *
     * <p>The generated Java is parsed rather than imported so the check is on the artifact that ships. A
     * boundary that claims to be palette-derived must actually be in that version's extracted name set; a
     * boundary that came from the feature timeline is allowed to name a version whose palette does not
     * contain the name, because that is precisely the case the timeline exists to cover (a renamed or
     * metadata-variant block).
     */
    @Test void generatedBlockDatesAgreeWithTheRegistryAndTheReviewedPalettes() throws Exception {
        List<String> registryOrder = HistoricalVersions.all().stream()
            .sorted(java.util.Comparator.comparingInt(HistoricalVersion::chronology))
            .map(HistoricalVersion::id).toList();
        assertEquals(registryOrder, HistoricalBlockAvailability.CHRONOLOGY,
            "the availability table's version order has drifted from the version registry");
        assertEquals(registryOrder.size(), HistoricalBlockAvailability.CHRONOLOGY.stream().distinct().count());

        Path source = ROOT.resolve("src/main/java/net/historicalexperience/generation/"
            + "HistoricalBlockAvailability.java");
        assertTrue(Files.isRegularFile(source), "the generated availability table is missing");
        String text = Files.readString(source);
        Pattern call = Pattern.compile(
            "introduce\\(\"([^\"]+)\",\\s*(true|false),\\s*\"((?:[^\"\\\\]|\\\\.)*)\",\\s*"
                + "new String\\[\\] \\{(.*?)\\}\\);", Pattern.DOTALL);
        Matcher matcher = call.matcher(text);
        Map<String, List<String>> byVersion = new LinkedHashMap<>();
        int groups = 0;
        int paletteDerivedNames = 0;
        Set<String> seen = new java.util.HashSet<>();
        while (matcher.find()) {
            groups++;
            String versionId = matcher.group(1);
            boolean timeline = Boolean.parseBoolean(matcher.group(2));
            String evidence = matcher.group(3).replace("\\\"", "\"");
            List<String> ids = new ArrayList<>();
            Matcher idMatcher = Pattern.compile("\"([^\"]+)\"").matcher(matcher.group(4));
            while (idMatcher.find()) {
                ids.add(idMatcher.group(1));
            }
            assertFalse(ids.isEmpty(), "an introduce() call names no block");
            for (String modernId : ids) {
                assertTrue(seen.add(modernId), modernId + " is dated twice");
                var boundary = HistoricalBlockAvailability.boundaryFor(modernId);
                assertNotNull(boundary, "no boundary for " + modernId
                    + " even though the generated table names it; the first block of a group being consumed "
                    + "as the evidence argument is the historical cause of exactly this");
                assertEquals(versionId, boundary.versionId(), "boundary version disagrees for " + modernId);
                assertEquals(timeline, boundary.timeline(), "trust flag disagrees for " + modernId);
                assertEquals(evidence, boundary.evidence(), "evidence text disagrees for " + modernId);
                assertFalse(boundary.evidence().isBlank(), "no evidence recorded for " + modernId);
                if (!timeline) {
                    // The date must have come from a name that is really in that version's extracted set.
                    // The generated evidence names it, and checking the named spelling (rather than the
                    // mapping's own spelling) is what lets an era rename such as airPortalFrame -> 
                    // endPortalFrame be recorded honestly instead of being forced through one name.
                    Matcher named = Pattern.compile("as \"([^\"]+)\"").matcher(boundary.evidence());
                    assertTrue(named.find(), "a palette-derived date for " + modernId
                        + " does not name the palette spelling it came from: " + boundary.evidence());
                    String paletteSpelling = named.group(1);
                    Set<String> names = palette(versionId);
                    assertNotNull(names, "palette-derived date for " + modernId + " in a version with no "
                        + "extracted palette");
                    assertTrue(names.contains(paletteSpelling), modernId + " is dated to " + versionId
                        + " as palette-derived, but that palette has no name '" + paletteSpelling
                        + "'; this is the signature of a transcription error in the mapping table");
                    paletteDerivedNames++;
                }
            }
            byVersion.put(versionId, ids);
        }
        assertTrue(groups > 100, "the generated table looks truncated: only " + groups + " introduce calls");
        assertEquals(HistoricalBlockAvailability.size(), seen.size());
        // The threshold was > 100 while the mapping dated EVERY descendant of a legacy name, which inflated this
        // count with blocks that were never palette-derived at all - blue_ice, coarse_dirt, soul_soil and soul_fire
        // among them, which is exactly how those four came to be permitted in fifteen versions. Dating only the
        // first target dropped the count to 56, and 56 is the honest number: the blocks the extracted palettes
        // actually justify. Lowering a threshold is usually a smell, so the reason is recorded, not just the number.
        assertTrue(paletteDerivedNames > 40, "expected the extracted palettes to justify a substantial share of "
            + "the dates, got " + paletteDerivedNames);
    }

    @Test void knownAnachronismsAreNowCaughtInEveryProfileThatLacksThem() {
        // The measured defect: a 289-chunk Alpha world contained ten vine blocks, and the old check could
        // not say so because only Beta 1.7.3 had a palette at all.
        for (String early : List.of("historical_experience:infdev_420",
                "historical_experience:infdev_611", "historical_experience:alpha")) {
            assertTrue(HistoricalBlockContent.isAnachronistic(early, "minecraft:vine"),
                "vines are Beta 1.8 content and must be an anachronism in " + early);
            assertTrue(HistoricalBlockContent.isAnachronistic(early, "minecraft:short_grass"),
                "the grass plant is Beta 1.6 content and must be an anachronism in " + early);
            assertTrue(HistoricalBlockContent.isAnachronistic(early, "minecraft:stone_bricks"),
                "stone bricks are Beta 1.8 content and must be an anachronism in " + early);
            assertTrue(HistoricalBlockContent.isAnachronistic(early, "minecraft:sandstone"),
                "sandstone is Beta 1.2 content and must be an anachronism in " + early);
            assertTrue(HistoricalBlockContent.isAnachronistic(early, "minecraft:lapis_ore"),
                "lapis is Beta 1.2 content and must be an anachronism in " + early);
        }
        // The two Infdev profiles additionally predate snow, ice, clay, cactus, sugar cane, redstone ore and
        // the monster spawner, all of which the Alpha profile legitimately has.
        for (String infdev : List.of("historical_experience:infdev_420",
                "historical_experience:infdev_611")) {
            for (String later : List.of("minecraft:snow", "minecraft:ice", "minecraft:clay",
                    "minecraft:cactus", "minecraft:sugar_cane", "minecraft:redstone_ore",
                    "minecraft:spawner", "minecraft:netherrack")) {
                assertTrue(HistoricalBlockContent.isAnachronistic(infdev, later),
                    later + " arrived after " + infdev);
            }
        }
        for (String later : List.of("minecraft:snow", "minecraft:ice", "minecraft:clay",
                "minecraft:cactus", "minecraft:sugar_cane", "minecraft:redstone_ore",
                "minecraft:spawner")) {
            assertFalse(HistoricalBlockContent.isAnachronistic("historical_experience:alpha", later),
                later + " existed by Alpha v1.1.2_01 and must not be deleted from an Alpha world");
        }
    }

    @Test void blocksAVersionReallyHadAreNeverCalledAnachronistic() {
        // False positives delete content, which is worse than a missed report, so the blocks each profile
        // certainly had are asserted explicitly.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            String id = version.id();
            for (String present : List.of("minecraft:stone", "minecraft:dirt", "minecraft:grass_block",
                    "minecraft:bedrock", "minecraft:water", "minecraft:lava", "minecraft:coal_ore",
                    "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:gravel", "minecraft:oak_log",
                    "minecraft:oak_leaves", "minecraft:cobblestone")) {
                assertFalse(HistoricalBlockContent.isAnachronistic(id, present),
                    present + " existed in every declared profile, so " + id + " must not lose it");
            }
        }
        // Release 1.6.4's own additions, and the block whose entry an earlier draft got wrong.
        for (String present : List.of("minecraft:terracotta", "minecraft:white_terracotta",
                "minecraft:white_carpet", "minecraft:coal_block", "minecraft:anvil", "minecraft:beacon",
                "minecraft:hopper", "minecraft:emerald_ore", "minecraft:ender_chest",
                "minecraft:quartz_block", "minecraft:infested_stone")) {
            assertFalse(HistoricalBlockContent.isAnachronistic("historical_experience:release_1_6_4",
                present), present + " is Release 1.6.x content");
        }
        // ... and those same blocks must NOT be allowed in an earlier profile.
        for (String absent : List.of("minecraft:terracotta", "minecraft:coal_block", "minecraft:emerald_ore",
                "minecraft:anvil", "minecraft:hopper", "minecraft:infested_stone")) {
            assertTrue(HistoricalBlockContent.isAnachronistic("historical_experience:beta_1_7_3", absent),
                absent + " is later than Beta 1.7.3");
        }
        // Content later than every declared profile is caught through the version-agnostic list.
        for (String later : List.of("minecraft:deepslate", "minecraft:copper_ore", "minecraft:amethyst_block",
                "minecraft:calcite", "minecraft:tuff", "minecraft:glow_lichen", "minecraft:bee_nest",
                "minecraft:leaf_litter", "minecraft:firefly_bush", "minecraft:dirt_path",
                "minecraft:large_fern", "minecraft:tall_grass", "minecraft:white_banner",
                "minecraft:slime_block", "minecraft:sea_lantern", "minecraft:white_stained_glass")) {
            for (HistoricalVersion version : HistoricalVersions.all()) {
                assertTrue(HistoricalBlockContent.isAnachronistic(version.id(), later),
                    later + " postdates every declared profile, so it is wrong in " + version.id());
            }
        }
    }

    @Test void unknownStaysUnknownRatherThanBecomingAcceptable() {
        // A block neither table knows must report UNKNOWN, not ALLOWED: an unverified block is not a
        // verified one, and the probe counts these so they stay visible.
        assertEquals(HistoricalBlockAvailability.Verdict.UNKNOWN,
            HistoricalBlockContent.judge("historical_experience:release_1_6_4", "example:not_a_block"));
        assertEquals(HistoricalBlockAvailability.Verdict.UNKNOWN,
            HistoricalBlockAvailability.judge("example:unknown_version", "minecraft:stone"));
        // A palette-derived date must NOT condemn a version that has no palette of its own: stone is dated
        // from the Beta 1.7.3 set only because no earlier set exists, and answering ANACHRONISTIC there would
        // delete stone from every Alpha and Infdev world.
        assertEquals(HistoricalBlockAvailability.Verdict.UNKNOWN,
            HistoricalBlockAvailability.judge("historical_experience:alpha", "minecraft:stone"),
            "absence from a palette that does not exist proves nothing");
        assertEquals(HistoricalBlockAvailability.Verdict.UNKNOWN,
            HistoricalBlockAvailability.judge("historical_experience:infdev_420", "minecraft:dirt"));
        // ... while a version that DOES have a palette is judged by it.
        assertEquals(HistoricalBlockAvailability.Verdict.ANACHRONISTIC,
            HistoricalBlockAvailability.judge("historical_experience:beta_1_7_3", "minecraft:stone_bricks"));
        assertEquals(HistoricalBlockAvailability.Verdict.ALLOWED,
            HistoricalBlockAvailability.judge("historical_experience:beta_1_8_1", "minecraft:stone_bricks"));
    }

    @Test void versionAgnosticListKeepsItsCorrectedDates() {
        // Regression guard for the specific mistake the earlier version-agnostic table made: terracotta was
        // filed under Release 1.12, which would have reported a correct Release 1.6.4 world as broken. It is
        // Release 1.6.1, inside the declared range, so it must be dated by the per-version table and must NOT
        // appear in the list that means "later than every declared version".
        assertNull(HistoricalBlockAnachronisms.introducedIn("minecraft:terracotta"),
            "terracotta is Release 1.6.1, so the version-agnostic list must not claim it is impossible");
        var terracotta = HistoricalBlockAvailability.boundaryFor("minecraft:terracotta");
        assertNotNull(terracotta);
        assertEquals("historical_experience:release_1_6_4", terracotta.versionId());
        assertTrue(HistoricalBlockContent.isAnachronistic("historical_experience:beta_1_7_3",
            "minecraft:terracotta"), "Beta 1.7.3 predates hardened clay");
        assertNull(HistoricalBlockAnachronisms.introducedIn("minecraft:stone"));
        assertTrue(HistoricalBlockAnachronisms.size() > 150,
            "the version-agnostic list looks truncated: " + HistoricalBlockAnachronisms.size() + " entries");
    }
}

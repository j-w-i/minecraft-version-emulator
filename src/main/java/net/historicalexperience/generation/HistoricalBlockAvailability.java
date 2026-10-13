package net.historicalexperience.generation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The earliest declared version that could contain a given modern block, and the evidence for it.
 *
 * <h2>Why an introduction table rather than a per-version palette</h2>
 *
 * <p>Asking "could this version have had this block?" against a single version's own block-name set
 * only works for the versions whose palette has been extracted, and even then absence is weak
 * evidence: Beta 1.7.3 and Release 1.6.4 spell the single-height plants differently from Release
 * 1.0, and a renamed block would look absent from the later set. Alpha v1.1.2_01 and both Infdev
 * builds have no recoverable block names at all (their registries never attach one), which is why
 * the earlier implementation could not suppress the ten vines a real Alpha world contained.
 *
 * <p>So the question is turned around: each block is dated once, as the earliest declared version
 * that had it, and every version is judged against that date. The consequences are:
 *
 * <ul>
 *   <li>a block present in the earliest profile is allowed everywhere, which is what makes the answer
 *       monotonic and immune to renames;
 *   <li>a block with no recorded date answers {@link Verdict#UNKNOWN} and is counted, never treated
 *       as acceptable - the same rule the rest of this project uses for unverified content;
 *   <li>the table is generated from the reviewed artifacts by
 *       {@code scripts/generate-1211-block-availability.py}, so it cannot drift from them, and a unit
 *       test re-derives it and fails on any difference.
 * </ul>
 *
 * <p>Evidence classes: {@code timeline} entries are sourced introduction boundaries transcribed from
 * research/feature-timeline.json; {@code palette} entries are the first extracted block-name set that
 * registers the block. A palette entry is weaker - it dates a NAME, not a block - which is exactly
 * why {@link #ALIASES} exists and why the conflict list below is reported rather than hidden.
 */
public final class HistoricalBlockAvailability {
    /** What a check can conclude about one block in one version. */
    public enum Verdict {
        /** The version is at or after the block's recorded introduction. */
        ALLOWED,
        /** The block is dated, and the version predates that date. */
        ANACHRONISTIC,
        /** No date is recorded, so nothing is claimed. Counted, never treated as acceptable. */
        UNKNOWN
    }

    /**
     * A recorded introduction.
     *
     * @param versionId the earliest declared version that could contain the block
     * @param evidence  why that version, in words a reviewer can check
     * @param timeline  true when sourced from the feature timeline, false when dated from a palette
     */
    public record Boundary(String versionId, String evidence, boolean timeline) {
    }

    /** The declared versions oldest first; a unit test asserts this equals the version registry. */
    public static final List<String> CHRONOLOGY = List.of(
        "historical_experience:infdev_420",
        "historical_experience:infdev_611",
        "historical_experience:alpha_1_0_1",
        "historical_experience:alpha_1_0_4",
        "historical_experience:alpha_1_0_6",
        "historical_experience:alpha_1_0_11",
        "historical_experience:alpha_1_0_14",
        "historical_experience:alpha_1_0_17",
        "historical_experience:alpha_1_1_0",
        "historical_experience:alpha",
        "historical_experience:alpha_1_2_0",
        "historical_experience:beta_1_0",
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_3",
        "historical_experience:release_1_4",
        "historical_experience:release_1_5",
        "historical_experience:release_1_6_4"
    );

    private static final Map<String, Boundary> INTRODUCED_IN = new LinkedHashMap<>();

    /**
     * The versions whose own client registered a block-name set this project has extracted.
     *
     * <p>This is what stops the table from over-claiming for the three pre-Beta profiles. Alpha v1.1.2_01
     * and both Infdev builds never attach a name to a block at registration - their registries load nine
     * material names and no block names at all - so they have no palette and cannot show that a block was
     * ABSENT. A palette-derived date would then read "stone first appears in the Beta 1.7.3 set" as
     * "Alpha had no stone", which is false and would delete it. Only a sourced timeline boundary may
     * condemn a block in a version that has no palette of its own.
     */
    private static final Set<String> VERSIONS_WITH_EXTRACTED_PALETTE = Set.of(
        "historical_experience:beta_1_7_3",
        "historical_experience:beta_1_8_1",
        "historical_experience:beta_1_9_pre_3",
        "historical_experience:release_1_0_0",
        "historical_experience:release_1_1",
        "historical_experience:release_1_2_5",
        "historical_experience:release_1_3",
        "historical_experience:release_1_4",
        "historical_experience:release_1_5",
        "historical_experience:release_1_6_4",
        "historical_experience:beta_1_9_pre_1",
        "historical_experience:beta_1_9_pre_2",
        "historical_experience:beta_1_9_pre_4",
        "historical_experience:beta_1_9_pre_5",
        "historical_experience:beta_1_9_pre_6"
    );

    private static void introduce(String versionId, boolean timeline, String evidence,
            String... modernBlockIds) {
        if (!CHRONOLOGY.contains(versionId)) {
            throw new IllegalStateException("unknown version " + versionId);
        }
        for (String modernBlockId : modernBlockIds) {
            Boundary previous = INTRODUCED_IN.put(modernBlockId,
                new Boundary(versionId, evidence, timeline));
            if (previous != null) {
                throw new IllegalStateException(modernBlockId + " is dated twice");
            }
        }
    }

    static {
        // feature-timeline blocks: bedrock introduced Classic 0.0.12a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: bedrock introduced Classic 0.0.12a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:bedrock"});
        // feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:bookshelf"});
        // feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:coal_ore"});
        // feature-timeline blocks: diamond ore restored Infdev 20100320, before the earliest profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: diamond ore restored Infdev 20100320, before the earliest profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:diamond_ore"});
        // feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:farmland"});
        // feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:glass"});
        // feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:gold_ore"});
        // feature-timeline blocks: "grass block (the dirt-with-grass top block)" introduced Pre-Classic; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: \"grass block (the dirt-with-grass top block)\" introduced Pre-Classic; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:grass_block"});
        // feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: these introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:gravel",
                "minecraft:iron_ore"});
        // feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:mossy_cobblestone"});
        // feature-timeline blocks: leaves introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: leaves introduced Classic 0.0.14a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:oak_leaves"});
        // feature-timeline blocks: sapling introduced Pre-Classic rd-161348; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: sapling introduced Pre-Classic rd-161348; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:oak_sapling"});
        // feature-timeline blocks: obsidian exists from Classic 0.28, natural formation from Indev; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: obsidian exists from Classic 0.28, natural formation from Indev; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:obsidian"});
        // feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: sponge and glass introduced Classic 0.0.19a; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:sponge"});
        // TNT is Classic 0.26 SURVIVAL TEST (feature-timeline mechanics: "punching TNT with an empty hand"); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "TNT is Classic 0.26 SURVIVAL TEST (feature-timeline mechanics: \"punching TNT with an empty hand\"); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:tnt"});
        // feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:infdev_420", true,
            "feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:wheat"});
        // feature-timeline blocks: redstone ore introduced Alpha v1.0.1 (Seecret Friday Update 3); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_1", true,
            "feature-timeline blocks: redstone ore introduced Alpha v1.0.1 (Seecret Friday Update 3); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:redstone_ore"});
        // feature-timeline blocks: monster spawner introduced Infdev 20100625-1917, after both Infdev profiles; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_1", true,
            "feature-timeline blocks: monster spawner introduced Infdev 20100625-1917, after both Infdev profiles; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:spawner"});
        // feature-timeline blocks: snow and ice introduced Alpha v1.0.4 (Winter Update); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_4", true,
            "feature-timeline blocks: snow and ice introduced Alpha v1.0.4 (Winter Update); the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:ice",
                "minecraft:snow"});
        // feature-timeline blocks: cactus introduced Alpha v1.0.6; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_6", true,
            "feature-timeline blocks: cactus introduced Alpha v1.0.6; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:cactus"});
        // feature-timeline blocks: clay block and sugar cane introduced Alpha v1.0.11; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_0_11", true,
            "feature-timeline blocks: clay block and sugar cane introduced Alpha v1.0.11; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:clay",
                "minecraft:sugar_cane"});
        // feature-timeline blocks: netherrack, glowstone and pumpkin introduced Alpha v1.2.0, after the Alpha v1.1.2_01 profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:alpha_1_2_0", true,
            "feature-timeline blocks: netherrack, glowstone and pumpkin introduced Alpha v1.2.0, after the Alpha v1.1.2_01 profile; the palette-derived boundary was beta_1_7_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:glowstone",
                "minecraft:jack_o_lantern",
                "minecraft:nether_portal",
                "minecraft:netherrack",
                "minecraft:pumpkin"});
        // feature-timeline: feature-timeline blocks: cobweb introduced Beta 1.5
        introduce("historical_experience:beta_1_7_3", true,
            "feature-timeline: feature-timeline blocks: cobweb introduced Beta 1.5",
            new String[] {
                "minecraft:cobweb"});
        // feature-timeline: feature-timeline blocks: lapis lazuli ore introduced Beta 1.2
        introduce("historical_experience:beta_1_7_3", true,
            "feature-timeline: feature-timeline blocks: lapis lazuli ore introduced Beta 1.2",
            new String[] {
                "minecraft:lapis_block",
                "minecraft:lapis_ore"});
        // feature-timeline: feature-timeline blocks: sandstone introduced Beta 1.2, so Alpha and Infdev have none
        introduce("historical_experience:beta_1_7_3", true,
            "feature-timeline: feature-timeline blocks: sandstone introduced Beta 1.2, so Alpha and Infdev have none",
            new String[] {
                "minecraft:sandstone"});
        // feature-timeline: feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, after every profile older than Beta 1.7.3
        introduce("historical_experience:beta_1_7_3", true,
            "feature-timeline: feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, after every profile older than Beta 1.7.3",
            new String[] {
                "minecraft:short_grass"});
        // first appears in the beta_1_7_3 extracted palette as "leaves" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"leaves\" (the reviewed mapping name)",
            new String[] {
                "minecraft:birch_leaves"});
        // first appears in the beta_1_7_3 extracted palette as "log" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"log\" (the reviewed mapping name)",
            new String[] {
                "minecraft:birch_log"});
        // first appears in the beta_1_7_3 extracted palette as "wood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"wood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:birch_planks"});
        // first appears in the beta_1_7_3 extracted palette as "sapling" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sapling\" (the reviewed mapping name)",
            new String[] {
                "minecraft:birch_sapling"});
        // first appears in the beta_1_7_3 extracted palette as "bed" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"bed\" (the reviewed mapping name)",
            new String[] {
                "minecraft:black_bed"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:black_wool"});
        // first appears in the beta_1_7_3 extracted palette as "bed" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"bed\" (the reviewed mapping name)",
            new String[] {
                "minecraft:blue_bed"});
        // first appears in the beta_1_7_3 extracted palette as "ice" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"ice\" (the reviewed mapping name)",
            new String[] {
                "minecraft:blue_ice"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:blue_wool"});
        // first appears in the beta_1_7_3 extracted palette as "brick" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"brick\" (the reviewed mapping name)",
            new String[] {
                "minecraft:bricks"});
        // first appears in the beta_1_7_3 extracted palette as "mushroom" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"mushroom\" (the reviewed mapping name)",
            new String[] {
                "minecraft:brown_mushroom"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:brown_wool"});
        // first appears in the beta_1_7_3 extracted palette as "cake" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cake\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cake"});
        // first appears in the beta_1_7_3 extracted palette as "chest" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"chest\" (the reviewed mapping name)",
            new String[] {
                "minecraft:chest"});
        // first appears in the beta_1_7_3 extracted palette as "sandStone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sandStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:chiseled_sandstone"});
        // first appears in the beta_1_7_3 extracted palette as "dirt" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"dirt\" (the reviewed mapping name)",
            new String[] {
                "minecraft:coarse_dirt"});
        // first appears in the beta_1_7_3 extracted palette as "stonebrick" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stonebrick\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cobblestone"});
        // first appears in the beta_1_7_3 extracted palette as "stairsStone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stairsStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cobblestone_stairs"});
        // first appears in the beta_1_7_3 extracted palette as "workbench" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"workbench\" (the reviewed mapping name)",
            new String[] {
                "minecraft:crafting_table"});
        // first appears in the beta_1_7_3 extracted palette as "sandStone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sandStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cut_sandstone"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:cyan_wool"});
        // first appears in the beta_1_7_3 extracted palette as "flower" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"flower\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dandelion"});
        // first appears in the beta_1_7_3 extracted palette as "deadbush" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"deadbush\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dead_bush"});
        // first appears in the beta_1_7_3 extracted palette as "detectorRail" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"detectorRail\" (the reviewed mapping name)",
            new String[] {
                "minecraft:detector_rail"});
        // first appears in the beta_1_7_3 extracted palette as "blockDiamond" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"blockDiamond\" (the reviewed mapping name)",
            new String[] {
                "minecraft:diamond_block"});
        // first appears in the beta_1_7_3 extracted palette as "dirt" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"dirt\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dirt"});
        // first appears in the beta_1_7_3 extracted palette as "dispenser" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"dispenser\" (the reviewed mapping name)",
            new String[] {
                "minecraft:dispenser"});
        // first appears in the beta_1_7_3 extracted palette as "tallgrass" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"tallgrass\" (the reviewed mapping name)",
            new String[] {
                "minecraft:fern"});
        // first appears in the beta_1_7_3 extracted palette as "fire" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"fire\" (the reviewed mapping name)",
            new String[] {
                "minecraft:fire"});
        // first appears in the beta_1_7_3 extracted palette as "ice" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"ice\" (the reviewed mapping name)",
            new String[] {
                "minecraft:frosted_ice"});
        // first appears in the beta_1_7_3 extracted palette as "furnace" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"furnace\" (the reviewed mapping name)",
            new String[] {
                "minecraft:furnace"});
        // first appears in the beta_1_7_3 extracted palette as "blockGold" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"blockGold\" (the reviewed mapping name)",
            new String[] {
                "minecraft:gold_block"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:gray_wool",
                "minecraft:green_wool"});
        // first appears in the beta_1_7_3 extracted palette as "blockIron" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"blockIron\" (the reviewed mapping name)",
            new String[] {
                "minecraft:iron_block"});
        // first appears in the beta_1_7_3 extracted palette as "doorIron" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"doorIron\" (the reviewed mapping name)",
            new String[] {
                "minecraft:iron_door"});
        // first appears in the beta_1_7_3 extracted palette as "jukebox" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"jukebox\" (the reviewed mapping name)",
            new String[] {
                "minecraft:jukebox"});
        // first appears in the beta_1_7_3 extracted palette as "leaves" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"leaves\" (the reviewed mapping name)",
            new String[] {
                "minecraft:jungle_leaves"});
        // first appears in the beta_1_7_3 extracted palette as "log" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"log\" (the reviewed mapping name)",
            new String[] {
                "minecraft:jungle_log"});
        // first appears in the beta_1_7_3 extracted palette as "wood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"wood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:jungle_planks"});
        // first appears in the beta_1_7_3 extracted palette as "sapling" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sapling\" (the reviewed mapping name)",
            new String[] {
                "minecraft:jungle_sapling"});
        // first appears in the beta_1_7_3 extracted palette as "ladder" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"ladder\" (the reviewed mapping name)",
            new String[] {
                "minecraft:ladder"});
        // first appears in the beta_1_7_3 extracted palette as "lava" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"lava\" (the reviewed mapping name)",
            new String[] {
                "minecraft:lava"});
        // first appears in the beta_1_7_3 extracted palette as "lever" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"lever\" (the reviewed mapping name)",
            new String[] {
                "minecraft:lever"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:light_blue_wool",
                "minecraft:light_gray_wool",
                "minecraft:lime_wool",
                "minecraft:magenta_wool"});
        // first appears in the beta_1_7_3 extracted palette as "pistonBase" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"pistonBase\" (the reviewed mapping name)",
            new String[] {
                "minecraft:moving_piston"});
        // first appears in the beta_1_7_3 extracted palette as "musicBlock" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"musicBlock\" (the reviewed mapping name)",
            new String[] {
                "minecraft:note_block"});
        // first appears in the beta_1_7_3 extracted palette as "doorWood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"doorWood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_door"});
        // first appears in the beta_1_7_3 extracted palette as "fence" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"fence\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_fence"});
        // first appears in the beta_1_7_3 extracted palette as "log" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"log\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_log"});
        // first appears in the beta_1_7_3 extracted palette as "wood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"wood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_planks"});
        // first appears in the beta_1_7_3 extracted palette as "sign" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sign\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_sign"});
        // first appears in the beta_1_7_3 extracted palette as "stairsWood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stairsWood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_stairs"});
        // first appears in the beta_1_7_3 extracted palette as "trapdoor" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"trapdoor\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_trapdoor"});
        // first appears in the beta_1_7_3 extracted palette as "sign" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sign\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_wall_sign"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:orange_wool"});
        // first appears in the beta_1_7_3 extracted palette as "ice" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"ice\" (the reviewed mapping name)",
            new String[] {
                "minecraft:packed_ice"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:pink_wool"});
        // first appears in the beta_1_7_3 extracted palette as "pistonBase" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"pistonBase\" (the reviewed mapping name)",
            new String[] {
                "minecraft:piston"});
        // first appears in the beta_1_7_3 extracted palette as "rose" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"rose\" (the reviewed mapping name)",
            new String[] {
                "minecraft:poppy"});
        // first appears in the beta_1_7_3 extracted palette as "snow" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"snow\" (the reviewed mapping name)",
            new String[] {
                "minecraft:powder_snow"});
        // first appears in the beta_1_7_3 extracted palette as "goldenRail" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"goldenRail\" (the reviewed mapping name)",
            new String[] {
                "minecraft:powered_rail"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:purple_wool"});
        // first appears in the beta_1_7_3 extracted palette as "rail" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"rail\" (the reviewed mapping name)",
            new String[] {
                "minecraft:rail"});
        // first appears in the beta_1_7_3 extracted palette as "bed" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"bed\" (the reviewed mapping name)",
            new String[] {
                "minecraft:red_bed"});
        // first appears in the beta_1_7_3 extracted palette as "mushroom" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"mushroom\" (the reviewed mapping name)",
            new String[] {
                "minecraft:red_mushroom"});
        // first appears in the beta_1_7_3 extracted palette as "sand" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sand\" (the reviewed mapping name)",
            new String[] {
                "minecraft:red_sand"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:red_wool"});
        // first appears in the beta_1_7_3 extracted palette as "notGate" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"notGate\" (the reviewed mapping name)",
            new String[] {
                "minecraft:redstone_torch",
                "minecraft:redstone_wall_torch"});
        // first appears in the beta_1_7_3 extracted palette as "redstoneDust" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"redstoneDust\" (the reviewed mapping name)",
            new String[] {
                "minecraft:redstone_wire"});
        // first appears in the beta_1_7_3 extracted palette as "diode" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"diode\" (the reviewed mapping name)",
            new String[] {
                "minecraft:repeater"});
        // first appears in the beta_1_7_3 extracted palette as "dirt" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"dirt\" (the reviewed mapping name)",
            new String[] {
                "minecraft:rooted_dirt"});
        // first appears in the beta_1_7_3 extracted palette as "sand" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sand\" (the reviewed mapping name)",
            new String[] {
                "minecraft:sand"});
        // first appears in the beta_1_7_3 extracted palette as "stoneSlab" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stoneSlab\" (the reviewed mapping name)",
            new String[] {
                "minecraft:sandstone_slab"});
        // first appears in the beta_1_7_3 extracted palette as "sandStone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sandStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:smooth_sandstone"});
        // first appears in the beta_1_7_3 extracted palette as "stoneSlab" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stoneSlab\" (the reviewed mapping name)",
            new String[] {
                "minecraft:smooth_stone_slab"});
        // first appears in the beta_1_7_3 extracted palette as "snow" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"snow\" (the reviewed mapping name)",
            new String[] {
                "minecraft:snow_block"});
        // first appears in the beta_1_7_3 extracted palette as "fire" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"fire\" (the reviewed mapping name)",
            new String[] {
                "minecraft:soul_fire"});
        // first appears in the beta_1_7_3 extracted palette as "hellsand" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"hellsand\" (the reviewed mapping name)",
            new String[] {
                "minecraft:soul_sand",
                "minecraft:soul_soil"});
        // first appears in the beta_1_7_3 extracted palette as "leaves" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"leaves\" (the reviewed mapping name)",
            new String[] {
                "minecraft:spruce_leaves"});
        // first appears in the beta_1_7_3 extracted palette as "log" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"log\" (the reviewed mapping name)",
            new String[] {
                "minecraft:spruce_log"});
        // first appears in the beta_1_7_3 extracted palette as "wood" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"wood\" (the reviewed mapping name)",
            new String[] {
                "minecraft:spruce_planks"});
        // first appears in the beta_1_7_3 extracted palette as "sapling" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sapling\" (the reviewed mapping name)",
            new String[] {
                "minecraft:spruce_sapling"});
        // first appears in the beta_1_7_3 extracted palette as "pistonStickyBase" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"pistonStickyBase\" (the reviewed mapping name)",
            new String[] {
                "minecraft:sticky_piston"});
        // first appears in the beta_1_7_3 extracted palette as "stone" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone"});
        // first appears in the beta_1_7_3 extracted palette as "button" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"button\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_button"});
        // first appears in the beta_1_7_3 extracted palette as "pressurePlate" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"pressurePlate\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_pressure_plate"});
        // first appears in the beta_1_7_3 extracted palette as "stoneSlab" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"stoneSlab\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_slab"});
        // first appears in the beta_1_7_3 extracted palette as "glass" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"glass\" (the reviewed mapping name)",
            new String[] {
                "minecraft:tinted_glass"});
        // first appears in the beta_1_7_3 extracted palette as "torch" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"torch\" (the reviewed mapping name)",
            new String[] {
                "minecraft:torch"});
        // first appears in the beta_1_7_3 extracted palette as "chest" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"chest\" (the reviewed mapping name)",
            new String[] {
                "minecraft:trapped_chest"});
        // first appears in the beta_1_7_3 extracted palette as "torch" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"torch\" (the reviewed mapping name)",
            new String[] {
                "minecraft:wall_torch"});
        // first appears in the beta_1_7_3 extracted palette as "water" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"water\" (the reviewed mapping name)",
            new String[] {
                "minecraft:water"});
        // first appears in the beta_1_7_3 extracted palette as "sponge" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"sponge\" (the reviewed mapping name)",
            new String[] {
                "minecraft:wet_sponge"});
        // first appears in the beta_1_7_3 extracted palette as "bed" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"bed\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_bed"});
        // first appears in the beta_1_7_3 extracted palette as "cloth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_7_3", false,
            "first appears in the beta_1_7_3 extracted palette as \"cloth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_wool",
                "minecraft:yellow_wool"});
        // feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever",
            new String[] {
                "minecraft:brown_mushroom_block"});
        // feature-timeline mobs: silverfish introduced Beta 1.8 (Pre-release), which is when monster egg blocks arrived; the palette alone would date the block to 1.6.4 because it spells it monsterStoneEgg; the palette-derived boundary was release_1_6_4 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline mobs: silverfish introduced Beta 1.8 (Pre-release), which is when monster egg blocks arrived; the palette alone would date the block to 1.6.4 because it spells it monsterStoneEgg; the palette-derived boundary was release_1_6_4 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:infested_stone"});
        // feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks",
            new String[] {
                "minecraft:melon"});
        // feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever",
            new String[] {
                "minecraft:red_mushroom_block"});
        // feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks
        introduce("historical_experience:beta_1_8_1", true,
            "feature-timeline: feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks",
            new String[] {
                "minecraft:stone_bricks",
                "minecraft:vine"});
        // first appears in the beta_1_8_1 extracted palette as "melon" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"melon\" (the reviewed mapping name)",
            new String[] {
                "minecraft:attached_melon_stem"});
        // first appears in the beta_1_8_1 extracted palette as "pumpkinStem" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"pumpkinStem\" (the reviewed mapping name)",
            new String[] {
                "minecraft:attached_pumpkin_stem"});
        // first appears in the beta_1_8_1 extracted palette as "stairsBrick" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"stairsBrick\" (the reviewed mapping name)",
            new String[] {
                "minecraft:brick_stairs"});
        // first appears in the beta_1_8_1 extracted palette as "stonebricksmooth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"stonebricksmooth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:chiseled_stone_bricks",
                "minecraft:cracked_stone_bricks"});
        // first appears in the beta_1_8_1 extracted palette as "thinGlass" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"thinGlass\" (the reviewed mapping name)",
            new String[] {
                "minecraft:glass_pane"});
        // first appears in the beta_1_8_1 extracted palette as "fenceIron" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"fenceIron\" (the reviewed mapping name)",
            new String[] {
                "minecraft:iron_bars"});
        // first appears in the beta_1_8_1 extracted palette as "melon" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"melon\" (the reviewed mapping name)",
            new String[] {
                "minecraft:melon_stem"});
        // first appears in the beta_1_8_1 extracted palette as "stonebricksmooth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"stonebricksmooth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:mossy_stone_bricks"});
        // first appears in the beta_1_8_1 extracted palette as "fenceGate" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"fenceGate\" (the reviewed mapping name)",
            new String[] {
                "minecraft:oak_fence_gate"});
        // first appears in the beta_1_8_1 extracted palette as "pumpkinStem" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"pumpkinStem\" (the reviewed mapping name)",
            new String[] {
                "minecraft:pumpkin_stem"});
        // first appears in the beta_1_8_1 extracted palette as "stairsStoneBrickSmooth" (the reviewed mapping name)
        introduce("historical_experience:beta_1_8_1", false,
            "first appears in the beta_1_8_1 extracted palette as \"stairsStoneBrickSmooth\" (the reviewed mapping name)",
            new String[] {
                "minecraft:stone_brick_stairs"});
        // feature-timeline blocks: lily pads introduced Beta 1.9 Prerelease 1 (the palette-derived date was pre3, which is the only Beta 1.9 palette extracted and therefore the latest such date, not the earliest); the palette-derived boundary was beta_1_9_pre_3 and the earlier of the two is used so a block a version did have is not deleted
        introduce("historical_experience:beta_1_9_pre_1", true,
            "feature-timeline blocks: lily pads introduced Beta 1.9 Prerelease 1 (the palette-derived date was pre3, which is the only Beta 1.9 palette extracted and therefore the latest such date, not the earliest); the palette-derived boundary was beta_1_9_pre_3 and the earlier of the two is used so a block a version did have is not deleted",
            new String[] {
                "minecraft:lily_pad"});
        // feature-timeline: feature-timeline blocks: brewing stand introduced Beta 1.9 Prerelease 3 (brewing listed at 30 seconds; a reduced set of 25 potions and 31 combinations)
        introduce("historical_experience:beta_1_9_pre_3", true,
            "feature-timeline: feature-timeline blocks: brewing stand introduced Beta 1.9 Prerelease 3 (brewing listed at 30 seconds; a reduced set of 25 potions and 31 combinations)",
            new String[] {
                "minecraft:brewing_stand"});
        // feature-timeline: feature-timeline blocks: end stone introduced Beta 1.9 Prerelease (Release 1.0.0)
        introduce("historical_experience:release_1_0_0", true,
            "feature-timeline: feature-timeline blocks: end stone introduced Beta 1.9 Prerelease (Release 1.0.0)",
            new String[] {
                "minecraft:end_stone"});
        // feature-timeline: feature-timeline blocks: nether wart introduced Beta 1.9 Prerelease (Release 1.0.0)
        introduce("historical_experience:release_1_0_0", true,
            "feature-timeline: feature-timeline blocks: nether wart introduced Beta 1.9 Prerelease (Release 1.0.0)",
            new String[] {
                "minecraft:nether_wart"});
        // feature-timeline: the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2
        introduce("historical_experience:release_1_6_4", true,
            "feature-timeline: the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2",
            new String[] {
                "minecraft:activator_rail",
                "minecraft:anvil",
                "minecraft:beacon",
                "minecraft:carrots",
                "minecraft:coal_block",
                "minecraft:cobblestone_wall",
                "minecraft:cocoa",
                "minecraft:command_block",
                "minecraft:comparator",
                "minecraft:daylight_detector",
                "minecraft:dropper"});
        // feature-timeline: feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), so it is in Release 1.6.4 and in none of the earlier profiles
        introduce("historical_experience:release_1_6_4", true,
            "feature-timeline: feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), so it is in Release 1.6.4 and in none of the earlier profiles",
            new String[] {
                "minecraft:emerald_block",
                "minecraft:emerald_ore"});
        // feature-timeline: the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2
        introduce("historical_experience:release_1_6_4", true,
            "feature-timeline: the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2",
            new String[] {
                "minecraft:ender_chest",
                "minecraft:flower_pot",
                "minecraft:hay_block",
                "minecraft:hopper",
                "minecraft:oak_slab",
                "minecraft:potatoes",
                "minecraft:quartz_block",
                "minecraft:tripwire"});
        // first appears in the release_1_6_4 extracted palette as "clayHardenedStained" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardenedStained\" (the reviewed mapping name)",
            new String[] {
                "minecraft:black_terracotta",
                "minecraft:blue_terracotta",
                "minecraft:brown_terracotta",
                "minecraft:cyan_terracotta"});
        // first appears in the release_1_6_4 extracted palette as "oreEmerald" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"oreEmerald\" (the reviewed mapping name)",
            new String[] {
                "minecraft:deepslate_emerald_ore"});
        // first appears in the release_1_6_4 extracted palette as "clayHardenedStained" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardenedStained\" (the reviewed mapping name)",
            new String[] {
                "minecraft:gray_terracotta",
                "minecraft:green_terracotta"});
        // first appears in the release_1_6_4 extracted palette as "monsterStoneEgg" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"monsterStoneEgg\" (the reviewed mapping name)",
            new String[] {
                "minecraft:infested_chiseled_stone_bricks",
                "minecraft:infested_cobblestone",
                "minecraft:infested_cracked_stone_bricks",
                "minecraft:infested_deepslate",
                "minecraft:infested_mossy_stone_bricks",
                "minecraft:infested_stone_bricks"});
        // first appears in the release_1_6_4 extracted palette as "clayHardenedStained" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardenedStained\" (the reviewed mapping name)",
            new String[] {
                "minecraft:light_blue_terracotta",
                "minecraft:light_gray_terracotta",
                "minecraft:lime_terracotta",
                "minecraft:magenta_terracotta",
                "minecraft:orange_terracotta",
                "minecraft:pink_terracotta",
                "minecraft:purple_terracotta",
                "minecraft:red_terracotta"});
        // first appears in the release_1_6_4 extracted palette as "stairsSandStone" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"stairsSandStone\" (the reviewed mapping name)",
            new String[] {
                "minecraft:sandstone_stairs"});
        // first appears in the release_1_6_4 extracted palette as "clayHardened" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardened\" (the reviewed mapping name)",
            new String[] {
                "minecraft:terracotta"});
        // first appears in the release_1_6_4 extracted palette as "woolCarpet" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"woolCarpet\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_carpet"});
        // first appears in the release_1_6_4 extracted palette as "clayHardenedStained" (the reviewed mapping name)
        introduce("historical_experience:release_1_6_4", false,
            "first appears in the release_1_6_4 extracted palette as \"clayHardenedStained\" (the reviewed mapping name)",
            new String[] {
                "minecraft:white_terracotta",
                "minecraft:yellow_terracotta"});
    }

    private HistoricalBlockAvailability() {
    }

    /** The recorded introduction of a modern block, or null when none is recorded. */
    public static Boundary boundaryFor(String modernBlockId) {
        if (modernBlockId == null) {
            return null;
        }
        // Property suffixes are stripped so "minecraft:oak_log[axis=y]" is still recognised.
        int bracket = modernBlockId.indexOf('[');
        String bare = bracket >= 0 ? modernBlockId.substring(0, bracket) : modernBlockId;
        return INTRODUCED_IN.get(bare);
    }

    /**
     * Judges one block against one declared version.
     *
     * <p>An unrecognised version id answers UNKNOWN rather than guessing: a version this build does
     * not declare is a version whose block set this build has no basis to judge.
     */
    public static Verdict judge(String versionId, String modernBlockId) {
        Boundary boundary = boundaryFor(modernBlockId);
        if (boundary == null || versionId == null) {
            return Verdict.UNKNOWN;
        }
        int version = CHRONOLOGY.indexOf(versionId);
        int introduced = CHRONOLOGY.indexOf(boundary.versionId());
        if (version < 0 || introduced < 0) {
            return Verdict.UNKNOWN;
        }
        if (!boundary.timeline() && !VERSIONS_WITH_EXTRACTED_PALETTE.contains(versionId)) {
            // A palette-derived date, judged against a version that has no palette of its own. The
            // date describes when the NAME appears, and absence from a palette that does not exist
            // proves nothing, so this answers UNKNOWN rather than deleting a block the version had.
            return Verdict.UNKNOWN;
        }
        return version < introduced ? Verdict.ANACHRONISTIC : Verdict.ALLOWED;
    }

    /** Every modern block this table dates, for reporting and tests. */
    public static Map<String, Boundary> all() {
        return java.util.Collections.unmodifiableMap(INTRODUCED_IN);
    }

    public static int size() {
        return INTRODUCED_IN.size();
    }
}

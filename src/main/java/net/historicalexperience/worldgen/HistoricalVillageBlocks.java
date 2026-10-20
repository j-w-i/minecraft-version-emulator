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
/*
 * Ported from the NeoForge mod "OldVillages" (upstream file
 * src/main/java/cz/maxtechnik/old_villages/worldgen/OldVillagePieces.java).
 * Upstream repository: https://github.com/MaxTechnik3796/old_villages (pinned revision
 * 7578013ec061303982539fac7606df67577ffb26).
 * Upstream declares "mod_license=MIT" in its gradle.properties. The reference checkout
 * ships no LICENSE file, so that gradle.properties declaration is the whole licence
 * statement available; it is recorded here rather than a licence file being claimed.
 * Licence: MIT.
 */
package net.historicalexperience.worldgen;

import net.historicalexperience.version.Milestones;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Which block ids a historical village may place, per version, as plain data.
 *
 * <h2>Why this class holds block ids and no Minecraft types</h2>
 *
 * <p>The unit test for this structure has to judge every emitted block against the version's extracted
 * palette, and it has to do that without a Minecraft runtime - a plain JUnit test cannot initialise
 * {@code net.minecraft.world.level.block.Blocks}. If the block table lived in a class that referenced
 * {@code Blocks}, the test could only reach it through a Minecraft-initialised environment, and the check
 * would become something that runs on a launch rather than on every build.
 *
 * <p>So the decision is here, as ids, and {@link HistoricalVillageStates} turns the ids into block states.
 * There is exactly one table: the piece code reads its states through the resolver, the resolver reads this
 * table, and the test reads this table. A duplicated list in the test would go on passing while the
 * generator emitted something else.
 *
 * <h2>Which blocks exist per version</h2>
 *
 * <p>Every block below is in Beta 1.8.1's extracted palette under this project's legacy-name mapping
 * ({@code research/block-palettes/b1_8_1.json}, 92 names, hash-verified), and every version at or after it
 * is a superset for these blocks. The only additions across the declared range are:
 *
 * <ul>
 *   <li>{@code minecraft:brewing_stand} - Beta 1.9 Prerelease 3. The church keeps its brewing stand from
 *       that version and uses the crafting table before it.</li>
 *   <li>the crop set - wheat always; carrots and potatoes from Beta 1.8.1; beetroot never, because it is
 *       Release 1.9 and the newest profile this project declares is Release 1.6.4.</li>
 * </ul>
 *
 * <h2>What is deliberately absent, and why</h2>
 *
 * <p>The upstream structure is a 1.14 village, so it names blocks that did not exist when villages were
 * introduced. Each is <b>substituted</b> with the era-appropriate block rather than dropped, so a village
 * still reads as the building it is:
 *
 * <ul>
 *   <li><b>Path surface.</b> Upstream has a "Use a Dirt Path instead of Gravel?" option because
 *       {@code minecraft:dirt_path} is Release 1.9 (15w31a). Every version this port declares predates
 *       that, so the option is not a choice: the path is {@code minecraft:gravel} everywhere, and
 *       {@link #usesDirtPath} reports the answer without a config file.</li>
 *   <li><b>Terracotta.</b> {@code minecraft:terracotta} and the stained set are Release 1.6.1 (13w37a), so
 *       only Release 1.6.4 may see terracotta at all. Upstream never places it, so this changes nothing
 *       today; it is listed in {@link #FORBIDDEN} so a later fidelity pass adding the hardened-clay desert
 *       village cannot silently emit it into Beta 1.8.1.</li>
 *   <li><b>Smooth stone.</b> {@code minecraft:smooth_stone} and {@code minecraft:smooth_stone_slab} are
 *       Release 1.2.1 (12w07a). The blacksmith and tavern floors used them, so they use
 *       {@code minecraft:stone_slab} instead - Beta 1.8.1's own slab.</li>
 *   <li><b>Workstations.</b> Composter, grindstone, smoker, lectern, barrel, bell, campfire, blast furnace,
 *       cartography table, fletching table, loom, stonecutter and lantern are 1.13/1.14 and none exists in
 *       any declared version. Each is replaced by the block that carried the same role in this era -
 *       crafting table for the lectern, furnace for the smoker, cobblestone for the grindstone - so the
 *       piece still keeps its shape.</li>
 *   <li><b>Pressure plates.</b> Upstream uses oak pressure plates. Beta 1.8.1's palette has one
 *       pressure-plate name, and this project's mapping table already ties that name to
 *       {@code minecraft:stone_pressure_plate} (a decision with its own test in
 *       {@code HistoricalBlockPaletteTest}). The village uses the stone pressure plate rather than
 *       re-pointing another subsystem's reviewed mapping.</li>
 *   <li><b>Iron bars</b> beside the blacksmith's forge are Release 1.8; a fence is what this era used for
 *       the same guard rail.</li>
 *   <li><b>The well's bell</b> is Release 1.14 and nothing in this era filled that role, so it is the one
 *       upstream block that is dropped rather than substituted. A census would show its absence.</li>
 * </ul>
 *
 * <p>No stripped wood, andesite/diorite/granite, per-species planks/logs/stairs, sweet-berry bush, banner or
 * stained glass is emitted: none is in the oldest village palette and none is needed to build these pieces.
 */
public final class HistoricalVillageBlocks {
    /** The path surface every declared profile uses. Upstream's "Gravel vs Dirt Path" option, answered. */
    public static final String PATH_SURFACE = "minecraft:gravel";

    /**
     * The blocks that are the same in every version, so they are not part of the per-version table.
     *
     * <p>They are still placed, so the palette test judges them too.
     */
    private static final Set<String> VERSION_INDEPENDENT = Set.of(
        "minecraft:air", "minecraft:water", "minecraft:lava");

    /** The blocks every version that can have a village had. */
    private static final Map<String, String> COMMON = new LinkedHashMap<>();

    static {
        COMMON.put("planks", "minecraft:oak_planks");
        COMMON.put("stairs", "minecraft:oak_stairs");
        COMMON.put("log", "minecraft:oak_log");
        COMMON.put("door", "minecraft:oak_door");
        COMMON.put("fence", "minecraft:oak_fence");
        COMMON.put("pressurePlate", "minecraft:stone_pressure_plate");
        COMMON.put("pathSurface", PATH_SURFACE);
        COMMON.put("cobble", "minecraft:cobblestone");
        COMMON.put("cobbleStairs", "minecraft:cobblestone_stairs");
        COMMON.put("stoneSlab", "minecraft:stone_slab");
        COMMON.put("sandstone", "minecraft:sandstone");
        COMMON.put("sandstoneStairs", "minecraft:sandstone_stairs");
        COMMON.put("sandstoneSlab", "minecraft:sandstone_slab");
        COMMON.put("wool", "minecraft:white_wool");
        COMMON.put("glassPane", "minecraft:glass_pane");
        COMMON.put("dirt", "minecraft:dirt");
        COMMON.put("grass", "minecraft:grass_block");
        COMMON.put("shortGrass", "minecraft:short_grass");
        COMMON.put("farmland", "minecraft:farmland");
        COMMON.put("wheat", "minecraft:wheat");
        COMMON.put("chest", "minecraft:chest");
        COMMON.put("furnace", "minecraft:furnace");
        COMMON.put("craftingTable", "minecraft:crafting_table");
        COMMON.put("bookshelf", "minecraft:bookshelf");
        COMMON.put("bed", "minecraft:red_bed");
        COMMON.put("ladder", "minecraft:ladder");
        COMMON.put("torch", "minecraft:torch");
        COMMON.put("wallTorch", "minecraft:wall_torch");
    }

    /**
     * The key of the slot holding whatever the version used where the church now has a brewing stand.
     *
     * <p>A named slot rather than an absent one: the church always places something there, and which block
     * that is depends on the version, exactly as the other workstations do.
     */
    public static final String WORKSTATION_SLOT = "workstation";

    /**
     * Block families this structure must never place, with the version that introduced each - or, for the
     * last entries, the reason the block is not the one the piece means.
     */
    private static final Map<String, String> FORBIDDEN = new LinkedHashMap<>();

    static {
        FORBIDDEN.put("minecraft:dirt_path", "Release 1.9 (15w31a); the path is gravel in every profile");
        FORBIDDEN.put("minecraft:terracotta", "Release 1.6.1 (13w37a)");
        FORBIDDEN.put("minecraft:white_terracotta", "Release 1.6.1 (13w37a)");
        FORBIDDEN.put("minecraft:black_terracotta", "Release 1.6.1 (13w37a), and stained at that");
        FORBIDDEN.put("minecraft:smooth_stone", "Release 1.2.1 (12w07a)");
        FORBIDDEN.put("minecraft:smooth_stone_slab", "Release 1.2.1 (12w07a)");
        FORBIDDEN.put("minecraft:smooth_sandstone", "Release 1.2.1 (12w07a)");
        FORBIDDEN.put("minecraft:cut_sandstone", "Release 1.2.1 (12w07a)");
        FORBIDDEN.put("minecraft:chiseled_sandstone", "Release 1.2.1 (12w07a)");
        FORBIDDEN.put("minecraft:andesite", "Release 1.8 (14w02a)");
        FORBIDDEN.put("minecraft:diorite", "Release 1.8 (14w02a)");
        FORBIDDEN.put("minecraft:granite", "Release 1.8 (14w02a)");
        FORBIDDEN.put("minecraft:stripped_oak_log", "Release 1.13 (17w47a)");
        FORBIDDEN.put("minecraft:birch_planks", "Beta 1.8.1 has one planks block, not per-species ones");
        FORBIDDEN.put("minecraft:spruce_planks", "Beta 1.8.1 has one planks block, not per-species ones");
        FORBIDDEN.put("minecraft:birch_log", "Beta 1.8.1 has one log block, not per-species ones");
        FORBIDDEN.put("minecraft:spruce_log", "Beta 1.8.1 has one log block, not per-species ones");
        FORBIDDEN.put("minecraft:birch_stairs", "Beta 1.8.1 has one wooden stair, not per-species ones");
        FORBIDDEN.put("minecraft:spruce_stairs", "Beta 1.8.1 has one wooden stair, not per-species ones");
        FORBIDDEN.put("minecraft:jungle_stairs", "Beta 1.8.1 has one wooden stair, not per-species ones");
        FORBIDDEN.put("minecraft:lantern", "Release 1.14 (18w49a)");
        FORBIDDEN.put("minecraft:campfire", "Release 1.14 (19w02a)");
        FORBIDDEN.put("minecraft:composter", "Release 1.14 (19w03a)");
        FORBIDDEN.put("minecraft:bell", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:barrel", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:smoker", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:blast_furnace", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:cartography_table", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:fletching_table", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:grindstone", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:lectern", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:loom", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:stonecutter", "Release 1.14 (18w44a)");
        FORBIDDEN.put("minecraft:sweet_berry_bush", "Release 1.14 (18w49a)");
        FORBIDDEN.put("minecraft:beetroots", "Release 1.9 (15w31a)");
        FORBIDDEN.put("minecraft:iron_bars", "Release 1.8; the forge rail is a fence in this era");
    }

    /**
     * One version's village block ids.
     *
     * @param versionId the version these ids belong to
     * @param blocks    slot name to block id; the slot names are the fields of the palette record and the
     *                  crop slots, so a missing crop slot is how a version says it grew no such crop
     */
    public record VillageBlockSet(String versionId, Map<String, String> blocks) {
        public VillageBlockSet {
            blocks = Map.copyOf(blocks);
        }

        /** The id in a slot, or null when this version has no such slot. */
        public String block(String slot) {
            return blocks.get(slot);
        }

        /** Whether this version grows a crop. */
        public boolean grows(String crop) {
            return blocks.containsKey("crop:" + crop);
        }
    }

    private HistoricalVillageBlocks() {
    }

    /** The version's slots, derived from the version registry rather than from a second list of versions. */
    public static VillageBlockSet forVersion(String versionId) {
        if (!HistoricalVillageRules.generationEnabledForVersion(versionId)) {
            throw new IllegalArgumentException(versionId + " could not generate a village, so it has no "
                + "village block palette; the gate is HistoricalVillageRules");
        }
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElseThrow(
            () -> new IllegalArgumentException("unknown historical version " + versionId));
        // Carrots and potatoes are Release 1.4.2 (12w34a), NOT Beta 1.8 as an earlier draft of this file
        // said. Release 1.6.4 is the only declared profile at or after that boundary, so it is the only one
        // whose village grows them; Beta 1.8.1 through Release 1.2.5 grew wheat alone.
        boolean carrotsAndPotatoes = version.isAtOrAfter(Milestones.RELEASE_1_6_4);
        // Sandstone stairs are not in the Beta 1.8.1 extraction and first appear in the 1.6.4 set, so no
        // earlier village may place them. The era's stone stair is cobblestone stairs, which the Beta 1.8.1
        // palette does register, so that is the substitution: a desert building gets the same stair every
        // other building in that version used.
        boolean sandstoneStairs = version.isAtOrAfter(Milestones.RELEASE_1_6_4);
        // The brewing stand arrives in Beta 1.9 Prerelease 3, per the research: "Beta 1.9 Prerelease 3 |
        // brewing stand (brewing listed at 30 seconds; a reduced set of 25 potions/31 combinations)". Prerelease 2
        // has potions - glass bottles, spider eyes, blaze powder, fermented spider eye, magma cream - so this
        // boundary was briefly moved to Pre2 on the reasoning that potions imply a stand. The research does not
        // support that, and the difference is visible: a Pre2 village would place a block the build did not have.
        // Noted rather than hidden: the research records potions at Pre2 with the stand at Pre3, which is a tension
        // in the sources, and this follows what the research states.
        boolean brewingStand = version.isAtOrAfter(Milestones.BETA_1_9_PRE_3);

        Map<String, String> blocks = new LinkedHashMap<>(COMMON);
        if (!sandstoneStairs) {
            blocks.put("sandstoneStairs", "minecraft:cobblestone_stairs");
        }
        // The church always places something in the workstation slot; which block it is depends on the
        // version, so this is a substitution rather than an absence.
        blocks.put(WORKSTATION_SLOT,
            brewingStand ? "minecraft:brewing_stand" : "minecraft:crafting_table");
        blocks.put("crop:wheat", "minecraft:wheat");
        if (carrotsAndPotatoes) {
            blocks.put("crop:carrots", "minecraft:carrots");
            blocks.put("crop:potatoes", "minecraft:potatoes");
        }
        // Beetroot is Release 1.9, after the newest profile this project declares, so no branch adds it.
        return new VillageBlockSet(versionId, blocks);
    }

    /** The slots that are the same in every version, as ids. */
    public static Set<String> versionIndependentBlockIds() {
        return VERSION_INDEPENDENT;
    }

    /** Upstream's "Use a Dirt Path instead of Gravel?" answer for every declared profile. */
    public static boolean usesDirtPath(String versionId) {
        // Dirt path is Release 1.9. Answering from the version rather than from a config is the whole point
        // of the port's shape, so the answer is computed even though it is false everywhere today.
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElseThrow(
            () -> new IllegalArgumentException("unknown historical version " + versionId));
        return version.isAtOrAfter(Milestones.RELEASE_1_9);
    }

    /**
     * Every block id this structure can place for one version, both village styles.
     *
     * <p>Built from {@link #forVersion} through the same accessors the piece code uses, so the test's
     * expectation moves with the generator.
     */
    public static Set<String> placedBlockIds(String versionId) {
        Set<String> ids = new LinkedHashSet<>(VERSION_INDEPENDENT);
        VillageBlockSet set = forVersion(versionId);
        ids.addAll(set.blocks().values());
        // The desert style is a substitution table, not extra blocks: it swaps the wood and cobble slots for
        // sandstone, which is already in the plains set. Asserting that here keeps the claim honest.
        for (String desertId : desertSubstitutions(set).values()) {
            ids.add(desertId);
        }
        return Set.copyOf(ids);
    }

    /** Every block id this structure can place for any declared version, both village styles. */
    public static Set<String> placedBlockIds() {
        Set<String> ids = new LinkedHashSet<>(VERSION_INDEPENDENT);
        for (String versionId : HistoricalVillageRules.villageVersions()) {
            ids.addAll(placedBlockIds(versionId));
        }
        return Set.copyOf(ids);
    }

    /**
     * The desert style's substitutions.
     *
     * <p>Upstream rebuilt a desert village from sandstone the same way, and only the desert biome reaches
     * this. The door, fence, bed and workstations are deliberately NOT substituted: sandstone has no
     * equivalent, and a village whose doors were sandstone would not be enterable.
     */
    public static Map<String, String> desertSubstitutions(VillageBlockSet plains) {
        Map<String, String> desert = new LinkedHashMap<>();
        String sandstone = plains.block("sandstone");
        if (sandstone == null) {
            return Map.of();
        }
        desert.put("planks", sandstone);
        desert.put("log", sandstone);
        desert.put("cobble", sandstone);
        desert.put("pathSurface", sandstone);
        desert.put("stairs", plains.block("sandstoneStairs"));
        desert.put("cobbleStairs", plains.block("sandstoneStairs"));
        return Map.copyOf(desert);
    }

    /** The ids this structure must never place, with the version that introduced each. */
    public static Map<String, String> forbiddenBlocks() {
        return java.util.Collections.unmodifiableMap(FORBIDDEN);
    }

    /** The block ids that are not slots but are still placed, for a report. */
    public static List<String> alwaysPlacedIds() {
        return List.of("minecraft:air", "minecraft:water", "minecraft:lava", PATH_SURFACE);
    }
}

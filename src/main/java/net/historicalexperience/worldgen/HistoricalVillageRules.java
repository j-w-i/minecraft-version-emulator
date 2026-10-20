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
 * Ported from the NeoForge mod "OldVillages" (upstream files
 * src/main/java/cz/maxtechnik/old_villages/OldVillagesCommonConfig.java,
 * src/main/java/cz/maxtechnik/old_villages/worldgen/OldVillageStructure.java and
 * src/main/java/cz/maxtechnik/old_villages/mixin/DisableVanillaVillagesMixin.java).
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * When this historical village exists, where it may exist, and which upstream config options became
 * version rules.
 *
 * <h2>Why there is no config file here</h2>
 *
 * <p>Upstream has three options - {@code mc14torches}, {@code use_dirt_path} and
 * {@code disable_vanila_villages} - read once from a NeoForge common config. This project has no config
 * framework on purpose: the answer is a property of the version being emulated, so each option is
 * computed from the world's own historical version instead of being chosen by an operator. In two of the
 * three cases that removes the choice entirely, and in the third - disabling vanilla villages - it makes
 * the rule apply to exactly the versions that had villages rather than to every world.
 *
 * <h2>Which versions could have a village</h2>
 *
 * <p>Beta 1.8 Pre-release, recorded as {@code introducedIn} "Beta 1.8 (Pre-release)" in
 * {@code research/feature-timeline.json}, whose structures entry "village" carries the quote
 * "|{{HistoryLine||1.8|dev=Pre-release|Added villages, which only generated in plains and desert
 * biomes." The chronology key for Beta 1.8.1 is 501801 and every declared release version is above it, so
 * a single comparison answers the question for all ten profiles. The four versions below it - Infdev
 * 20100420, Infdev 20100611, Alpha v1.1.2_01 and Beta 1.7.3 - must generate no village at all, and
 * {@link #generationEnabledForVersion} says so rather than leaving them to the structure data.
 *
 * <h2>Which biomes</h2>
 *
 * <p>Plains and desert only, including the sunflower-plains variant of plains. Deliberately NOT savanna,
 * taiga, snowy or meadow: the same research row states that Beta 1.8 villages "only generated in plains
 * and desert biomes", and the later biome expansion - savanna and taiga in Release 1.4.2/1.5, snowy in
 * Release 1.10, meadow in 1.18 - is all after the newest profile this project declares (Release 1.6.4),
 * so none of them is a village biome for any world this build can create. Upstream's biome tag lists
 * exactly the eight biomes this project must not use beyond plains and desert, and the difference is
 * intentional: upstream targets a modern game, this port targets the versions before the expansion.
 *
 * <p>Release 1.4.2 is the savanna/taiga village addition; because Release 1.6.4 is at or after it, a
 * future fidelity pass could legitimately add those two biomes for that one profile. That is a
 * generation-content change with its own evidence requirement, not something this port should assume, and
 * {@link #permittedBiomes()} is the single place it would be expressed.
 *
 * <h2>Villagers</h2>
 *
 * <p>Beta 1.8.1 villages contained no villagers: villagers are Beta 1.9 Prerelease content, and the same
 * research row records that "Villagers themselves arrive in Beta 1.9 Prerelease, so a Beta 1.8.1 village
 * is EMPTY of villagers." The simplest correct implementation is therefore to place <b>no entities at
 * all</b>, in any version, rather than to place villagers from Beta 1.9 onward. That is a deliberate
 * reduction of upstream (which spawns them in the tavern, blacksmith, library, church and large house):
 * this port is a generation feature, entity placement is not generation, and the failure mode of getting
 * it wrong - a villager standing in a Beta 1.8.1 village - is exactly the kind of anachronism the rest of
 * this project exists to prevent.
 */
public final class HistoricalVillageRules {
    /** Beta 1.8.1's chronology key: villages exist at or after this, and nowhere before it. */
    public static final int BETA_1_8_1_CHRONOLOGY = Milestones.BETA_1_8_1.key();

    /**
     * Beta 1.9 Prerelease 1's chronology: when villages became INHABITED.
     *
     * <p>Villages arrive in Beta 1.8.1 and are empty; villagers arrive in Pre1 and make them inhabited. That
     * boundary is what allows the two builds to differ, and it is why the village generator must spawn its villagers
     * CONDITIONALLY rather than either never or always.
     */
    public static final int BETA_1_9_PRE_1_CHRONOLOGY = Milestones.BETA_1_9_PRE_1.key();

    /**
     * Release 1.3's chronology: when villages gained TRADING, and therefore professions.
     *
     * <p>Before this a villager is a plain villager with nothing to trade, which is what the owner's Release 1.0
     * world should contain. This existed as a gap rather than a gate until now - villagers were placed correctly and
     * simply had no profession, which was right by omission for 1.0-1.2 and wrong for 1.3 and later.
     */
    public static final int RELEASE_1_3_CHRONOLOGY = Milestones.RELEASE_1_3.key();

    /**
     * The professions Release 1.3 actually had: farmer, librarian, priest, blacksmith and butcher.
     *
     * <p>Held as IDS rather than as {@code VillagerProfession} constants so a test can assert this list without a
     * bootstrapped registry, and resolved against the registry at spawn time. Later professions - cartographer,
     * fisherman, fletcher, leatherworker, mason, shepherd and the three smiths that replaced the single blacksmith -
     * did not exist, and a Release 1.3 village must not offer them.
     *
     * <p>The smiths deserve a note: Release 1.3 had ONE blacksmith, which modern Minecraft split into armorer,
     * toolsmith and weaponsmith. Any of those three is the era-appropriate stand-in, and all three are listed.
     */
    public static final java.util.List<String> RELEASE_1_3_PROFESSIONS = java.util.List.of(
        "minecraft:farmer",
        "minecraft:librarian",
        "minecraft:cleric",
        "minecraft:armorer",
        "minecraft:toolsmith",
        "minecraft:weaponsmith",
        "minecraft:butcher");

    /** Beta 1.9 Prerelease 3's chronology key: the first version with villagers, and a brewing stand. */
    public static final int BETA_1_9_PRE_3_CHRONOLOGY = Milestones.BETA_1_9_PRE_3.key();

    /** The evidence clause every answer here is derived from, so a report can quote one string. */
    public static final String EVIDENCE =
        "research/feature-timeline.json structures entry 'village': introducedIn 'Beta 1.8 "
            + "(Pre-release)', quote '|{{HistoryLine||1.8|dev=Pre-release|Added villages, which only "
            + "generated in plains and desert biomes.' The same entry records that villagers arrive in "
            + "Beta 1.9 Prerelease, so a Beta 1.8.1 village has none.";

    /**
     * The biomes a village may generate in, exactly as the resource tag
     * {@code historical_experience:has_structure/historical_village} declares them.
     *
     * <p>Both lists exist on purpose: the tag is what the game reads, and this set is what the test and
     * the diagnostic assert, so a tag that drifts from the researched boundary fails a test instead of
     * silently generating a village in a swamp.
     */
    private static final Set<String> PERMITTED_BIOMES = Set.of(
        "minecraft:plains",
        "minecraft:sunflower_plains",
        // Moderner Beta's own plains. This is the plains every village-capable profile actually generates: Beta 1.8.1
        // through Release 1.6.4 take their plains from {@code moderner_beta:late_beta_plains} (there is no
        // early-release plains), and the vanilla ids above never appear there. Leaving it out was the bug that made
        // villages generate in deserts only - the one biome on the list that Moderner Beta really produces.
        "moderner_beta:late_beta_plains",
        "minecraft:desert");

    /** The biome whose villages are built from sandstone. */
    private static final String DESERT = "minecraft:desert";

    private HistoricalVillageRules() {
    }

    /** The three permitted biome ids, for the tag and for tests. */
    public static Set<String> permittedBiomes() {
        return PERMITTED_BIOMES;
    }

    /**
     * Whether a village may generate in a biome.
     *
     * <p>The old upstream structure decided its village style by substring-matching the biome path -
     * {@code path.contains("desert")} and so on - which would classify {@code minecraft:desert_lakes} or
     * any addon biome with "desert" in its name as a desert village. This port matches exactly, and a
     * biome outside the permitted set is refused rather than silently styled as plains.
     */
    public static boolean biomeIsPermitted(String biomeId) {
        return PERMITTED_BIOMES.contains(biomeId);
    }

    /** Whether the biome is the desert style; only meaningful for a permitted biome. */
    public static boolean isDesert(String biomeId) {
        return DESERT.equals(biomeId);
    }

    /**
     * Every declared version that can generate a village, oldest first.
     *
     * <p>Read from the version registry rather than listed, so a version added later is covered by the
     * rule and by the palette test without either being edited.
     */
    public static List<String> villageVersions() {
        return HistoricalVersions.atOrAfter(BETA_1_8_1_CHRONOLOGY).stream()
            .map(HistoricalVersion::id)
            .toList();
    }

    /** Every declared version that must not generate a village, oldest first. */
    public static List<String> noVillageVersions() {
        return HistoricalVersions.before(BETA_1_8_1_CHRONOLOGY).stream()
            .map(HistoricalVersion::id)
            .toList();
    }

    /**
     * Whether the version could have generated a village.
     *
     * @throws IllegalArgumentException for an id the registry does not declare, because answering
     *     {@code false} for a typo would look exactly like a correct boundary decision
     */
    public static boolean generationEnabledForVersion(String versionId) {
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElseThrow(
            () -> new IllegalArgumentException(versionId + " is not a declared historical version, so "
                + "the village boundary cannot be answered for it"));
        return version.isAtOrAfter(Milestones.BETA_1_8_1);
    }

    /**
     * Whether the version a generation recipe alias belongs to could have generated a village.
     *
     * <p>An alias that is not a historical recipe - a plain Moderner Beta preset, a vanilla world - gets
     * {@code false} rather than an exception: this structure is registered data, so it is reachable in
     * every world, and an ordinary world must simply not be gated by a historical rule. The distinction
     * matters for the "disable vanilla villages" rule below, which must apply to historical worlds only.
     */
    public static boolean generationEnabledForAlias(String alias) {
        if (alias == null) {
            return false;
        }
        return HistoricalVersions.byRevisionAlias(alias)
            .map(version -> version.isAtOrAfter(Milestones.BETA_1_8_1))
            .orElse(false);
    }

    /**
     * Whether the alias names a historical world whose vanilla villages must not generate.
     *
     * <p>This is upstream's {@code disable_vanila_villages} expressed as a version rule: a historical world
     * at or after Beta 1.8.1 has villages, and they are this structure, so the modern jigsaw
     * {@code minecraft:village_*} structures must be suppressed there. Worlds below the boundary already
     * generate no structure at all through {@code HistoricalStructureRules}, and an ordinary world is never
     * touched.
     */
    public static boolean suppressVanillaVillagesForAlias(String alias) {
        return generationEnabledForAlias(alias);
    }

    /**
     * The modern jigsaw village structures this project's structure gate must deny once a version is at or
     * after Beta 1.8.1.
     *
     * <p><b>Nothing in this port enforces this yet.</b> {@code HistoricalStructureRules} currently declares
     * rules only for versions with NO structures, which means a historical Beta 1.8.1 world would still be
     * able to receive the modern {@code minecraft:village_plains} and friends alongside this structure. The
     * ids are listed here with the evidence so the fix is a data change in that one file - whose rule record
     * already supports a per-version allow-list - rather than another search for what the ids are.
     */
    public static Map<String, String> vanillaVillageStructures() {
        Map<String, String> ids = new LinkedHashMap<>();
        String why = "A historical version at or after Beta 1.8.1 generates the historical village instead; "
            + "upstream's 'disable vanilla villages' option, expressed as a version rule. " + EVIDENCE;
        ids.put("minecraft:village_plains", why);
        ids.put("minecraft:village_desert", why);
        ids.put("minecraft:village_savanna", why);
        ids.put("minecraft:village_taiga", why);
        ids.put("minecraft:village_snowy", why);
        return Map.copyOf(ids);
    }

    /**
     * The historical alias of the generator in a generation context, or empty when the world is not a
     * historical one.
     *
     * <p>The version is read from the generator's own stored settings through
     * {@link HistoricalFeatureExclusions#historicalAlias}, the same source every other per-world rule in
     * this project uses, so the answer cannot leak between worlds or survive an exception.
     */
    public static Optional<String> aliasOf(net.minecraft.world.level.chunk.ChunkGenerator generator) {
        return Optional.ofNullable(HistoricalFeatureExclusions.historicalAlias(generator));
    }
}

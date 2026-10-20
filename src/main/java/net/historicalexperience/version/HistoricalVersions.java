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
package net.historicalexperience.version;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.historicalexperience.legacy.LegacyCatalog;
import net.historicalexperience.version.HistoricalVersion.GenerationRevision;

/**
 * Lookups over the historical versions this build declares.
 *
 * <h2>This class answers questions; it does not declare anything</h2>
 *
 * <p>The declarations themselves - which versions exist, what rules each has and what generation recipes it owns -
 * live in {@link LegacyCatalog}, because they are composed from the gameplay and generation data those versions use.
 * This class is the read side: {@code byId}, {@code byRevisionAlias}, ordering queries and the finite-world set. Every
 * other package depends on <b>this</b>, and this depends on nothing but the model plus that one composition edge, so
 * the model cannot acquire a cycle through the data.
 *
 * <p>The chronology scale and the era offsets live here because they are part of the model: a key is
 * {@code era base + major*1000 + minor*100 + patch}, and {@link Era} owns the bases. {@link Milestones} names the
 * boundaries the rules use.
 */
public final class HistoricalVersions {
    /**
     * Sortable chronology key: {@code eraOffset + major * 1000 + minor * 100 + patch}.
     *
     * <p>Beta 1.7.3 is 501703 and Beta 1.8.1 is 501801, so a feature introduced in Beta 1.8 is present in every version
     * whose key is at least 501800. The irregular builds (date-labelled Indev and Infdev, lettered Alpha builds) give
     * their key explicitly; see {@link Milestone#ofKey}.
     */
    public static int chronology(int eraOffset, int major, int minor, int patch) {
        return eraOffset + major * 1000 + minor * 100 + patch;
    }

    /** @deprecated use {@link Era#ALPHA}. */
    @Deprecated
    public static final int ERA_ALPHA = Era.ALPHA.base();

    /** @deprecated use {@link Era#BETA}. */
    @Deprecated
    public static final int ERA_BETA = Era.BETA.base();

    /** @deprecated use {@link Era#RELEASE}. */
    @Deprecated
    public static final int ERA_RELEASE = Era.RELEASE.base();

    private static final List<HistoricalVersion> ALL = LegacyCatalog.all();
    private static final Map<String, HistoricalVersion> BY_ID;
    private static final Map<String, HistoricalVersion> BY_ALIAS;

    static {
        Map<String, HistoricalVersion> byId = new LinkedHashMap<>();
        Map<String, HistoricalVersion> byAlias = new LinkedHashMap<>();
        for (HistoricalVersion version : ALL) {
            byId.put(version.id(), version);
            for (GenerationRevision revision : version.revisions()) {
                byAlias.put(revision.alias(), version);
            }
        }
        BY_ID = Map.copyOf(byId);
        BY_ALIAS = Map.copyOf(byAlias);
    }

    /**
     * The declared profiles whose worlds are FINITE: a fixed-size level, and the world border that comes with it.
     *
     * <p>Indev 20100223 is the only one this build offers. Classic would belong here too but is not offered at all,
     * because its Moderner Beta provider hangs during world creation while Indev's does not. Kept as data so an
     * upgrade can ask the question without loading a world: see WorldGenSettingsAdvance's border release.
     */
    private static final Set<String> FINITE_PROFILES = Set.of("historical_experience:indev_latest");

    /**
     * Whether this version's era had redstone ore. The boundary is Alpha v1.0.1 (Seecret Friday Update 3), used to
     * derive a version's era ore population rather than to type out a per-version table.
     */
    public static boolean eraHasRedstoneOre(HistoricalVersion version) {
        return version.isAtOrAfter(Milestones.ALPHA_1_0_1);
    }

    /** Whether this version's era had lapis ore (Beta 1.2 onward). */
    public static boolean eraHasLapisOre(HistoricalVersion version) {
        return version.isAtOrAfter(Milestones.BETA_1_2);
    }

    /**
     * Whether this version's era had emerald ore. The boundary is Release 1.3, the stable baseline the project's
     * Release 1.3 profile represents; every earlier declared version predates 12w21a.
     */
    public static boolean eraHasEmeraldOre(HistoricalVersion version) {
        return version.isAtOrAfter(Milestones.RELEASE_1_3);
    }

    /**
     * The era population for a version: the verified Beta 1.7.3 population filtered to the ores that era had.
     *
     * <p>The derivation lives with the declarations, because it needs the ore table and this class must stay free of
     * the generation package.
     */
    public static List<net.historicalexperience.version.OreRule> eraOrePopulation(HistoricalVersion version) {
        return LegacyCatalog.eraOrePopulation(version);
    }

    /** Whether a profile id names a finite world. False for null, for an ordinary world and for every other era. */
    public static boolean isFiniteWorld(String profileId) {
        return profileId != null && FINITE_PROFILES.contains(profileId);
    }

    /** The same question for a generation-preset alias, which is what a journal and a level stem actually store. */
    public static boolean isFinitePreset(String presetAlias) {
        return byRevisionAlias(presetAlias).map(version -> isFiniteWorld(version.id())).orElse(false);
    }

    /** Every finite profile, so a test can assert the set is exactly the eras that were finite. */
    public static Set<String> finiteProfiles() {
        return FINITE_PROFILES;
    }

    /**
     * Every declared version, in declaration order.
     *
     * <p>Deliberately <b>not</b> sorted: {@link #allRevisionAliases()} and the marker list derived from it promise this
     * order, and Alpha v1.2.2a is constructed as a clone of Alpha v1.2.0. Use {@link #chronological()} for history.
     */
    public static List<HistoricalVersion> all() {
        return ALL;
    }

    /** Every declared version, oldest first: the order history happened in. */
    public static List<HistoricalVersion> chronological() {
        return ALL.stream().sorted(java.util.Comparator.comparingInt(HistoricalVersion::chronology)).toList();
    }

    /** A version by its gameplay profile id. */
    public static Optional<HistoricalVersion> byId(String versionId) {
        return versionId == null
            ? Optional.empty()
            : Optional.ofNullable(BY_ID.get(versionId));
    }

    /** The version that owns a persisted generation-recipe alias. */
    public static Optional<HistoricalVersion> byRevisionAlias(String rawAlias) {
        return rawAlias == null
            ? Optional.empty()
            : Optional.ofNullable(BY_ALIAS.get(rawAlias));
    }

    /** Every owned recipe alias of every version, in declaration order. */
    public static List<String> allRevisionAliases() {
        return ALL.stream()
            .flatMap(version -> version.revisions().stream())
            .map(GenerationRevision::alias)
            .toList();
    }

    /** The versions at or after a chronology key, for "which eras have this" queries. */
    public static List<HistoricalVersion> atOrAfter(int chronology) {
        return ALL.stream().filter(version -> version.chronology() >= chronology).toList();
    }

    /** The versions before a chronology key. */
    public static List<HistoricalVersion> before(int chronology) {
        return ALL.stream().filter(version -> version.chronology() < chronology).toList();
    }

    /** Every era family the registry declares. */
    public static Set<String> eras() {
        return ALL.stream().map(HistoricalVersion::era)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private HistoricalVersions() {
    }
}
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
package net.historicalexperience.gameplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Applies {@link HistoricalSpawnRules} to a live candidate spawn list.
 *
 * <p>Kept separate from the mixin so that both the hook and the verification probe run the <b>same</b>
 * code: a probe that re-implemented the filter could agree with itself while the game did something
 * else.
 *
 * <p>Two entry points exist on purpose. {@link #analyse} is pure and records nothing, so verification
 * can measure the rule's effect on real data without polluting the counters that report whether the
 * live spawn path actually ran; {@link #apply} and {@link #applyForLevel} are the hooked paths and do
 * record.
 *
 * <p>Nothing here removes an entity that already exists: the filter rewrites the candidate list a spawn
 * attempt is about to choose from, before any {@code EntityType} is instantiated, and a world that resolves
 * to no profile gets the list it was handed.
 */
public final class HistoricalSpawnFilter {
    /**
     * The rule's effect on one candidate list.
     *
     * @param kept    the surviving list; the same instance when nothing was removed
     * @param removed the entity type identifiers that were removed, in encounter order
     */
    public record Outcome(WeightedRandomList<MobSpawnSettings.SpawnerData> kept, List<String> removed) {
        public boolean changedAnything() {
            return !removed.isEmpty();
        }
    }

    private HistoricalSpawnFilter() {
    }

    /**
     * The gameplay profile a world is generating with, or {@code null} when it is not historical.
     *
     * <p>Resolved through {@link net.historicalexperience.generation.HistoricalFeatureExclusions#activeAlias},
     * which reads the generator's own marker first and falls back to the running server's
     * {@link net.historicalexperience.generation.HistoricalGenerationScope} only when the generator carries
     * none. The generator's own answer therefore still wins per dimension.
     *
     * <p>The fallback is what makes this correct for the Nether and the End, whose level stems are ordinary
     * vanilla generators even in a historical world: reading the generator alone answered null there, so the
     * spawn rule was skipped and a Beta 1.7.3 Nether kept 1.21.1's {@code nether_wastes} list (strider among
     * others) plus the 1.16 entries of {@code crimson_forest} (piglin, hoglin). The scope is set only for a
     * world whose overworld carries one of this project's markers and is cleared when the server stops, so an
     * ordinary world is left untouched.
     */
    public static String profileIdForGenerator(ChunkGenerator generator) {
        String alias = net.historicalexperience.generation.HistoricalFeatureExclusions
            .activeAlias(generator);
        return alias == null ? null : HistoricalSpawnRules.profileIdForAlias(alias);
    }

    /**
     * Computes the rule's effect without recording anything, for a dimension that can add its own
     * restrictions.
     *
     * <p>{@link #analyse} is this with no dimension, so a caller with a biome or a level rather than a
     * dimension key is unaffected. An unrestricted profile returns the candidate list unchanged, so an
     * ordinary world and a world of another era are provably untouched rather than merely believed to be.
     *
     * @param dimensionId the dimension the list belongs to, or {@code null} when the caller has none
     */
    public static Outcome analyseForDimension(
        String profileId,
        String dimensionId,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        Optional<HistoricalSpawnRules.SpawnRule> rule = HistoricalSpawnRules.forProfile(profileId);
        if (rule.isEmpty() || candidates == null || candidates.isEmpty()) {
            return new Outcome(candidates, List.of());
        }

        List<String> removed = new ArrayList<>();
        // 1.21.1's weighted list is built from the entries themselves rather than through a builder:
        // its element type extends WeightedEntry, so a kept SpawnerData already carries its own weight and
        // can be handed straight back. 26.3 split the value from the weight, which is why this differed.
        List<MobSpawnSettings.SpawnerData> kept = new ArrayList<>();
        for (MobSpawnSettings.SpawnerData candidate : candidates.unwrap()) {
            String entityId = BuiltInRegistries.ENTITY_TYPE
                .getKey(candidate.type).toString();
            if (isAllowedInDimension(rule.get(), profileId, dimensionId, entityId)) {
                kept.add(candidate);
            } else {
                removed.add(entityId);
            }
        }
        return new Outcome(removed.isEmpty() ? candidates : WeightedRandomList.create(kept), removed);
    }

    /** {@link #analyseForDimension} with no dimension, so the version's own set is the only rule. */
    public static Outcome analyse(
        String profileId,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        return analyseForDimension(profileId, null, candidates);
    }

    /**
     * The complete verdict for one candidate entry: the version's own allow set, then the dimension's
     * additions.
     *
     * <p>Today the only dimension rule is the Nether's enderman boundary - endermen reach the Nether in the
     * 1.10 snapshot 16w20a, and every declared version is older, so a historical Nether must not spawn one
     * while both the Overworld and the End keep them. It is expressed as a boundary rather than a veto so a
     * later version inherits the right answer; see
     * {@link HistoricalSpawnRules#NETHER_ENDERMAN_CHRONOLOGY}.
     *
     * <p>An unknown dimension id adds nothing, so a caller that cannot identify its dimension gets the
     * version's own rule rather than an invented restriction.
     */
    public static boolean isAllowedInDimension(
        HistoricalSpawnRules.SpawnRule rule,
        String profileId,
        String dimensionId,
        String entityTypeId
    ) {
        if (rule == null) {
            // No rule means no filtering, exactly as HistoricalSpawnRules.isAllowed(null, ...) answers. The
            // dimension rule is an ADDITION to a version's set, so it cannot apply where there is no version
            // to add it to - an ordinary world must not acquire a Nether restriction by asking this helper.
            return true;
        }
        if (!HistoricalSpawnRules.isAllowed(rule, entityTypeId)) {
            return false;
        }
        if (NETHER_DIMENSION_ID.equals(dimensionId) && "minecraft:enderman".equals(entityTypeId)) {
            return HistoricalSpawnRules.netherEndermenAllowed(profileId);
        }
        return true;
    }

    /**
     * The Nether's level identifier, as 1.21.1 spells it.
     *
     * <p>The End is deliberately absent: endermen are its native population in every version, so no
     * dimension rule may touch them there.
     */
    private static final String NETHER_DIMENSION_ID =
        net.minecraft.world.level.Level.NETHER.location().toString();

    /**
     * The hooked natural-spawning path.
     *
     * <p>Records nothing and returns the list unchanged when the world resolving through
     * {@link #profileIdForGenerator} is not a declared historical profile - which is what an ordinary world,
     * an unrecognised preset and a stopped server all are.
     */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> apply(
        ChunkGenerator generator,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        return applyForDimension(profileIdForGenerator(generator), null, candidates);
    }

    /**
     * The hooked chunk-generation path, which has a level rather than a generator in hand.
     *
     * <p>The level is authoritative for both halves of the resolution: it supplies the generator, and it
     * supplies the dimension whose rules apply. Using the generator alone would lose the second.
     */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> applyForLevel(
        ServerLevel level,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        if (level == null) {
            return candidates;
        }
        String dimensionId;
        try {
            dimensionId = level.dimension().location().toString();
        } catch (RuntimeException e) {
            // A level that cannot name its dimension is not one we can add a dimension rule to; the
            // version's own set still applies, which is the safe half.
            dimensionId = null;
        }
        return applyForDimension(
            profileIdForGenerator(level.getChunkSource().getGenerator()),
            dimensionId,
            candidates);
    }

    /** The shared resolving body of {@link #apply} and {@link #applyForLevel}. */
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> applyForDimension(
        String profileId,
        String dimensionId,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        if (profileId == null || HistoricalSpawnRules.forProfile(profileId).isEmpty()) {
            return candidates;
        }
        HistoricalSpawnRules.recordFilterInvocation();
        Outcome outcome = analyseForDimension(profileId, dimensionId, candidates);
        outcome.removed().forEach(HistoricalSpawnRules::recordRemoval);
        return outcome.kept();
    }
}

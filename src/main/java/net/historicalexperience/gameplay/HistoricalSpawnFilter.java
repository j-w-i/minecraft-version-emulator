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

    /** The gameplay profile this generator belongs to, or {@code null} when it is not historical. */
    public static String profileIdForGenerator(ChunkGenerator generator) {
        String alias = net.historicalexperience.generation.HistoricalFeatureExclusions
            .historicalAlias(generator);
        return alias == null ? null : HistoricalSpawnRules.profileIdForAlias(alias);
    }

    /**
     * Computes the rule's effect without recording anything.
     *
     * <p>An unrestricted profile returns the candidate list unchanged, so an ordinary world and a
     * world of another era are provably untouched rather than merely believed to be.
     */
    public static Outcome analyse(
        String profileId,
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
            if (HistoricalSpawnRules.isAllowed(rule.get(), entityId)) {
                kept.add(candidate);
            } else {
                removed.add(entityId);
            }
        }
        return new Outcome(removed.isEmpty() ? candidates : WeightedRandomList.create(kept), removed);
    }

    /** The hooked natural-spawning path. */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> apply(
        ChunkGenerator generator,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        String profileId = profileIdForGenerator(generator);
        if (profileId == null || HistoricalSpawnRules.forProfile(profileId).isEmpty()) {
            return candidates;
        }
        HistoricalSpawnRules.recordFilterInvocation();
        Outcome outcome = analyse(profileId, candidates);
        outcome.removed().forEach(HistoricalSpawnRules::recordRemoval);
        return outcome.kept();
    }

    /** The hooked chunk-generation path, which has a level rather than a generator in hand. */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> applyForLevel(
        ServerLevel level,
        WeightedRandomList<MobSpawnSettings.SpawnerData> candidates
    ) {
        if (level == null) {
            return candidates;
        }
        return apply(level.getChunkSource().getGenerator(), candidates);
    }
}

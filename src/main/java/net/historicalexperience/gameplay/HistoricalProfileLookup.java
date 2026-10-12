package net.historicalexperience.gameplay;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Resolves a historical <b>gameplay profile</b> from the world that is actually running.
 *
 * <p>Shared by every gameplay rule so they cannot disagree about which era a world belongs to: the
 * answer always comes from the generator's own durable marker through the same binding table the
 * server lifecycle uses. An ordinary world, a plain Moderner Beta world and a world on an
 * unrecognised revision all resolve to {@code null}, which every caller treats as "no restrictions".
 */
public final class HistoricalProfileLookup {
    private HistoricalProfileLookup() {
    }

    /** The gameplay profile of a generator, or {@code null} when it is not a historical world. */
    public static String profileIdForGenerator(ChunkGenerator generator) {
        if (generator == null) {
            return null;
        }
        String alias = net.historicalexperience.generation.HistoricalFeatureExclusions
            .historicalAlias(generator);
        return alias == null ? null : profileIdForAlias(alias);
    }

    /** The gameplay profile of a running level, or {@code null} when it is not historical. */
    public static String profileIdForLevel(ServerLevel level) {
        return level == null ? null : profileIdForGenerator(level.getChunkSource().getGenerator());
    }

    /**
     * The gameplay profile a persisted generation-recipe alias belongs to, or {@code null} when the
     * alias is not one this build declares.
     */
    public static String profileIdForAlias(String rawAlias) {
        return net.historicalexperience.profile.HistoricalProfiles.byGenerationPreset(rawAlias)
            .map(binding -> binding.profile().id().value())
            .orElse(null);
    }
}

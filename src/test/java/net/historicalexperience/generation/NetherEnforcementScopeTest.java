package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.*;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Vanilla dimension generators have no historical marker of their own. */
class NetherEnforcementScopeTest {
    @AfterEach void clearScope() { HistoricalGenerationScope.clear(); }

    @Test void unmarkedDimensionUsesOwningWorldRecipe() {
        for (var version : HistoricalVersions.all()) {
            var revisions = version.revisions();
            String alias = revisions.get(revisions.size() - 1).alias();
            HistoricalGenerationScope.set(alias);
            assertEquals(alias, HistoricalFeatureExclusions.activeAlias(null));
            var exclusions = HistoricalFeatureExclusions.runtimeExclusionsForAlias(
                HistoricalFeatureExclusions.activeAlias(null));
            for (String id : java.util.List.of("minecraft:ore_gold_nether", "minecraft:ore_magma",
                    "minecraft:ore_ancient_debris_large", "minecraft:ore_debris_small",
                    "minecraft:ore_blackstone", "minecraft:patch_soul_fire")) {
                assertTrue(exclusions.stream().anyMatch(e -> e.featureId().equals(id)), version.id() + ": " + id);
            }
        }
    }

    @Test void ordinaryWorldAndStoppedServerHaveNoFallback() {
        HistoricalGenerationScope.clear();
        assertNull(HistoricalFeatureExclusions.activeAlias(null));
        assertTrue(HistoricalFeatureExclusions.runtimeExclusionsForAlias(null).isEmpty());
        HistoricalGenerationScope.set(HistoricalVersions.all().iterator().next().revisions().get(0).alias());
        HistoricalGenerationScope.clear();
        assertNull(HistoricalFeatureExclusions.activeAlias(null));
    }
}

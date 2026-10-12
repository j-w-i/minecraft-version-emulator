package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.historicalexperience.HistoricalMarker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit. Pins the verified Beta 1.7.3 ore parameters and the append-only revision binding, so a
 * change to a number or to which revision places Beta ores is a visible edit to this file.
 */
class BetaOreRulesTest {
    private static final String REVISION_4 = "historical_experience:beta_1_7_3_content_preview_4";
    private static final String REVISION_5 = "historical_experience:beta_1_7_3_content_preview_5";

    @Test
    @DisplayName("the eight populations match the verified Beta 1.7.3 call sites")
    void parametersMatchTheVerifiedCallSites() {
        List<BetaOreRules.OreRule> rules = BetaOreRules.BETA_1_7_3;
        assertEquals(8, rules.size());

        // name, size, attempts, height draws - from research/beta173-ore-parameters-parent-reviewed.json
        assertRule(rules.get(0), "dirt", "minecraft:dirt", 32, 20, 128, 0);
        assertRule(rules.get(1), "gravel", "minecraft:gravel", 32, 10, 128, 0);
        assertRule(rules.get(2), "coal", "minecraft:coal_ore", 16, 20, 128, 0);
        assertRule(rules.get(3), "iron", "minecraft:iron_ore", 8, 20, 64, 0);
        assertRule(rules.get(4), "gold", "minecraft:gold_ore", 8, 2, 32, 0);
        assertRule(rules.get(5), "redstone", "minecraft:redstone_ore", 7, 8, 16, 0);
        assertRule(rules.get(6), "diamond", "minecraft:diamond_ore", 7, 1, 16, 0);
        // Lapis is the only rule that draws its height twice: nextInt(16) + nextInt(16).
        assertRule(rules.get(7), "lapis", "minecraft:lapis_ore", 6, 1, 16, 16);
    }

    @Test
    @DisplayName("no emerald and no seventh ore is placed")
    void emeraldIsNotPlaced() {
        // The Beta 1.7.3 block registry contains exactly six ores and no emerald ore block, verified
        // from the client jar; an unused emerald ITEM exists, which is why this was worth checking.
        Set<String> blocks = BetaOreRules.writtenBlockIds();
        assertFalse(blocks.contains("minecraft:emerald_ore"));
        assertFalse(blocks.contains("moderner_beta:ore_emerald_y95"));
        assertEquals(Set.of(
                "minecraft:dirt", "minecraft:gravel", "minecraft:coal_ore", "minecraft:iron_ore",
                "minecraft:gold_ore", "minecraft:redstone_ore", "minecraft:diamond_ore",
                "minecraft:lapis_ore"),
            blocks);
    }

    @Test
    @DisplayName("only the newest revision places Beta ores, so older worlds keep their recipe")
    void earlierRevisionsPlaceVanillaOres() {
        assertEquals(List.of(), BetaOreRules.forAlias(REVISION_4));
        assertEquals(List.of(), BetaOreRules.forAlias(HistoricalMarker.HISTORICAL_PRESET_ID));
        assertFalse(BetaOreRules.forAlias(REVISION_5).isEmpty());
        // Derived: every revision that declares its own ore population, with revision 5 among them.
        assertTrue(BetaOreRules.oreRevisions().contains(REVISION_5));
        for (String alias : BetaOreRules.oreRevisions()) {
            assertFalse(BetaOreRules.forAlias(alias).isEmpty(),
                alias + " is listed as placing ores but declares none");
        }
        assertEquals(List.of(), BetaOreRules.forAlias("moderner_beta:beta"));
    }

    @Test
    @DisplayName("the derived height ceiling follows the verified algorithm")
    void derivedCeilingMatchesTheAlgorithm() {
        // origin max + the +-2 centre offset + ceil(2 * size / 16) + 1, which is the same bound the
        // ore-vein research artifact states independently.
        assertEquals(127 + 2 + 2 + 1, rule("coal").maxReachableY());
        assertEquals(63 + 2 + 1 + 1, rule("iron").maxReachableY());
        assertEquals(15 + 2 + 1 + 1, rule("diamond").maxReachableY());
        assertEquals(15 + 2 + 1 + 1, rule("redstone").maxReachableY());
        assertEquals(30 + 2 + 1 + 1, rule("lapis").maxReachableY());
    }

    @Test
    @DisplayName("revision 5 also excludes the vanilla ore features it replaces")
    void revisionFiveExcludesVanillaOres() {
        List<HistoricalFeatureExclusions.Exclusion> revision5 =
            HistoricalFeatureExclusions.forAlias(REVISION_5);
        Set<String> ids = revision5.stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(java.util.stream.Collectors.toSet());

        // Both halves are required: placing Beta ores without excluding vanilla's would roughly double
        // every count, and excluding without placing would leave a world with no ores.
        for (String vanilla : List.of(
            "minecraft:ore_coal_upper", "minecraft:ore_coal_lower", "minecraft:ore_iron_upper",
            "minecraft:ore_iron_middle", "minecraft:ore_iron_small", "minecraft:ore_gold",
            "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower",
            "minecraft:ore_diamond", "minecraft:ore_diamond_medium", "minecraft:ore_diamond_large",
            "minecraft:ore_diamond_buried", "minecraft:ore_lapis", "minecraft:ore_lapis_buried",
            "minecraft:ore_dirt", "minecraft:ore_gravel",
            "moderner_beta:ore_emerald_y95")) {
            assertTrue(ids.contains(vanilla), vanilla + " must be excluded in revision 5");
        }

        // Revision 4 keeps the smaller set: it never placed Beta ores, so it must not remove vanilla's.
        Set<String> revision4 = HistoricalFeatureExclusions.forAlias(REVISION_4).stream()
            .map(HistoricalFeatureExclusions.Exclusion::featureId)
            .collect(java.util.stream.Collectors.toSet());
        assertFalse(revision4.contains("minecraft:ore_coal_lower"));
        assertEquals(8, revision4.size());
    }

    @Test
    @DisplayName("diagnostics start clean and expose every field the verdict needs")
    void diagnosticsAreComplete() {
        // The revision binding is static, so it is populated before any generation happens; the
        // counters are not.
        assertTrue(BetaOreRules.oreRevisions().contains(REVISION_5));
        assertEquals(0L, BetaOreRules.veinsAttempted());
        assertEquals(0L, BetaOreRules.blocksPlaced());
        assertEquals(java.util.Map.of(), BetaOreRules.placedByOre());

        var diagnostics = BetaOreRules.diagnostics();
        assertEquals(
            Set.of("oreRevisions", "rules", "veinsAttempted", "blocksPlaced", "placedByOre"),
            diagnostics.keySet());
        assertEquals("8", diagnostics.get("rules"));
        assertEquals("0", diagnostics.get("veinsAttempted"));
    }

    private static BetaOreRules.OreRule rule(String name) {
        return BetaOreRules.BETA_1_7_3.stream()
            .filter(candidate -> candidate.name().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private static void assertRule(
        BetaOreRules.OreRule rule,
        String name,
        String blockId,
        int size,
        int attempts,
        int boundA,
        int boundB
    ) {
        assertEquals(name, rule.name());
        assertEquals(blockId, rule.blockId());
        assertEquals(size, rule.size(), name + " size");
        assertEquals(attempts, rule.attempts(), name + " attempts");
        assertEquals(boundA, rule.originBoundA(), name + " first height bound");
        assertEquals(boundB, rule.originBoundB(), name + " second height bound");
    }
}

package net.historicalexperience.gameplay;

import java.util.Map;

/**
 * The tool durability the versions before Beta 1.2 had, where tools broke far sooner than they do now.
 *
 * <h2>The numbers, and where they come from</h2>
 *
 * <p>{@code research/version-transitions.json} records Beta 1.2's changed list as
 * <i>"tool durability: wood 33 -&gt; 59, stone 65 -&gt; 131, iron 129 -&gt; 250, diamond 1025 -&gt; 1561"</i>, and
 * {@code research/minecraft-version-history.md} says the same. Both sides of each pair matter here: the right-hand
 * values are what 1.21.1 already uses, which is why a version at or after Beta 1.2 needs no override at all and
 * only the older builds do.
 *
 * <p><b>Gold is deliberately absent.</b> The same sentence reads "gold tools gained much faster mining in some
 * categories while retaining old durability/restrictions" - so gold did not change, and there is no old number to
 * apply. Writing one would be inventing a value, which is the thing this project keeps refusing to do: an item the
 * table does not name keeps its modern durability rather than getting a guess.
 *
 * <p>This was recorded as unimplemented work for several rounds - the Beta 1.2 entry's own evidence said
 * "NOT implemented: the tool durability table, which needs an item-attribute gate this project does not have".
 * The gate now exists, so the numbers can be applied rather than merely written down.
 */
public final class HistoricalToolDurability {
    /**
     * The pre-Beta-1.2 durability of every tool the research gives a number for.
     *
     * <p>Keyed by modern item id. Every entry is one of the four families in the source sentence, applied to all
     * five tool shapes, because the source states a figure per MATERIAL and not per tool - a wooden sword and a
     * wooden pickaxe were both 33.
     */
    private static final Map<String, Integer> BEFORE_BETA_1_2 = buildTable();

    private static Map<String, Integer> buildTable() {
        Map<String, Integer> table = new java.util.LinkedHashMap<>();
        put(table, 33, "wooden");
        put(table, 65, "stone");
        put(table, 129, "iron");
        put(table, 1025, "diamond");
        return Map.copyOf(table);
    }

    private static void put(Map<String, Integer> table, int durability, String material) {
        for (String shape : new String[] {"sword", "pickaxe", "axe", "shovel", "hoe"}) {
            table.put("minecraft:" + material + "_" + shape, durability);
        }
    }

    private HistoricalToolDurability() {
    }

    /**
     * The durability this item had in this version, or {@code null} when nothing is claimed.
     *
     * <p>Null is the answer for a version at or after Beta 1.2, for an item the source does not name, and for an
     * undeclared version - and every one of those means "leave the modern value alone" rather than zero.
     */
    public static Integer durabilityFor(String versionId, String itemId) {
        if (itemId == null || !HistoricalGameplayTweaks.appliesTo("toolDurability", versionId)) {
            return null;
        }
        return BEFORE_BETA_1_2.get(itemId);
    }

    /** Every item this table speaks for, so a test can walk it rather than repeat the list. */
    public static java.util.Set<String> namedItems() {
        return BEFORE_BETA_1_2.keySet();
    }
}

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Removes content that did not exist yet from generated chest loot.
 *
 * <h2>What this is for</h2>
 *
 * <p>Vanilla's chest tables are modern. A dungeon chest can hold a name tag, a horse armour or an enchanted golden
 * apple, and none of those existed in Beta 1.7.3. The owner asked for chest loot to stop containing items that would
 * not exist, which is a content rule rather than a difficulty one, so it is enforced wherever a chest is rolled
 * rather than by rewriting vanilla's tables.
 *
 * <h2>Where it hooks, and why there</h2>
 *
 * <p>On {@code LootTable.getRandomItems}, the call {@code LootTable.fill} makes for every chest it fills. Hooking
 * there means one place covers every randomisable container in the game - dungeons, mineshafts, strongholds,
 * temples, village chests and the rest - whatever table the generator gave it, and whatever mod or datapack added
 * that table. It also runs against {@link LootContextParams#THIS_ENTITY}, so a table's own conditions and functions
 * have already done their work and this only decides what is allowed to survive.
 *
 * <h2>Removing rather than replacing</h2>
 *
 * <p>The project's rule that content must not be placed at all applies to WORLD GENERATION, where generating a block
 * and deleting it would leave the chunk briefly wrong and would move the decorator's random sequence. A chest's loot
 * is rolled in one go when the chest is opened, and an entry dropped from that roll is not placed, not observed and
 * not saved - so dropping it here satisfies the rule rather than working around it.
 */
public final class HistoricalChestLoot {
    private static final LongAdder REMOVED = new LongAdder();
    private static final Map<String, Integer> REMOVED_BY_ITEM = new ConcurrentHashMap<>();
    private static final Set<String> REPORTED_PROFILES = ConcurrentHashMap.newKeySet();

    private HistoricalChestLoot() {
    }

    /**
     * Whether a rolled stack may stay in the chest for this version.
     *
     * <p>An item with no recorded introduction is allowed. That direction is deliberate: a table entry nobody has
     * dated must not silently vanish, because the failure would look like ordinary bad luck to the player and like
     * nothing at all in a log.
     */
    public static boolean allows(String versionId, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack single = stack.copyWithCount(1);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(single.getItem());
        return HistoricalItemAvailability.existsIn(versionId, id.toString());
    }

    /** Records one entry dropped from a chest roll, for the diagnostics. */
    public static void recordRemoved(String itemId) {
        REMOVED.increment();
        REMOVED_BY_ITEM.merge(itemId, 1, Integer::sum);
    }

    public static long removedCount() {
        return REMOVED.sum();
    }

    public static Map<String, Integer> removedByItem() {
        return Map.copyOf(REMOVED_BY_ITEM);
    }

    /** Diagnostics as an ordered map, for verdict reporting. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new java.util.LinkedHashMap<>();
        report.put("datedItems", Integer.toString(HistoricalItemAvailability.size()));
        report.put("removedFromChests", Long.toString(REMOVED.sum()));
        report.put("removedByItem", REMOVED_BY_ITEM.toString());
        return report;
    }
}
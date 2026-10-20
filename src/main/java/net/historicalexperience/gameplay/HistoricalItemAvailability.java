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

import java.util.LinkedHashMap;
import java.util.Map;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * When an ITEM first existed, so loot that contains it can be refused for the versions that predate it.
 *
 * <h2>Why this is separate from the block table</h2>
 *
 * <p>{@code HistoricalBlockAvailability} answers the same question for BLOCKS, from the version boundary research and
 * the extracted palettes. Loot contains items, and several of the offenders are items with no block form at all -
 * emeralds, golden carrots, potatoes, name tags - so the block table cannot answer for them. This table is written
 * from the same two sources: {@code research/feature-timeline.json} for a recorded introduction, and the version
 * history notes for the rest.
 *
 * <h2>What it is used for</h2>
 *
 * <p>Replacing generated chest loot. Vanilla's chest tables are modern: a dungeon chest in 1.21.1 can hold
 * {@code minecraft:name_tag}, {@code minecraft:enchanted_golden_apple} and {@code minecraft:glistering_melon_slice},
 * none of which existed in Beta 1.7.3. A world of that era must not contain them, and the project's rule is that the
 * content must not be placed at all rather than placed and removed - so the affected tables are rebuilt without those
 * entries before the chest is ever rolled. See {@code HistoricalChestLoot}.
 *
 * <h2>Only the items that appear in chest loot are listed</h2>
 *
 * <p>This is not an item registry and makes no claim to be one. An item absent from it is treated as acceptable
 * rather than as modern, so that a table entry nobody has dated cannot silently delete legitimate loot. Every item
 * named in a vanilla chest table that did not exist in the earliest declared profile IS listed, because those are the
 * ones that would otherwise appear in a Beta 1.7.3 world.
 */
public final class HistoricalItemAvailability {
    /** One item's first appearance, with the evidence for it. */
    public record IntroducedIn(String versionId, int chronology, String evidence) {
    }

    private static final Map<String, IntroducedIn> INTRODUCED = new LinkedHashMap<>();

    private static void at(String versionId, String evidence, String... itemIds) {
        HistoricalVersion version = HistoricalVersions.byId("historical_experience:" + versionId)
            .orElseThrow(() -> new IllegalArgumentException("no such version: " + versionId));
        IntroducedIn entry = new IntroducedIn(versionId, version.chronology(), evidence);
        for (String itemId : itemIds) {
            INTRODUCED.put(itemId, entry);
        }
    }

    /** An item that no declared version has, with the evidence. Modelled as a cut-off beyond every declared build. */
    private static void never(String evidence, String... itemIds) {
        IntroducedIn entry = new IntroducedIn("none", Integer.MAX_VALUE, evidence);
        for (String itemId : itemIds) {
            INTRODUCED.put(itemId, entry);
        }
    }

    static {
        // Recorded in research/feature-timeline.json where the timeline carries the item, and in
        // research/version-transitions.json for the rest. Grouped by the build that introduced them.
        at("release_1_3", "Villagers and trading arrive in Release 1.3, and emeralds with them",
            "minecraft:emerald", "minecraft:emerald_block");
        at("release_1_4", "Release 1.4 (12w34a) adds carrots, potatoes and the golden carrot; the carrot on a "
            + "stick and the potato are recorded in version-transitions for Release 1.4",
            "minecraft:carrot", "minecraft:potato", "minecraft:baked_potato", "minecraft:golden_carrot",
            "minecraft:poisonous_potato");
        at("release_1_4", "Name tags are added in Release 1.4 (12w34a) as a renameable item",
            "minecraft:name_tag");
        at("release_1_6_4", "Horse armour, leads and the horse family arrive in Release 1.6.1; the coal and "
            + "other horse-riding items come with them",
            "minecraft:lead", "minecraft:iron_horse_armor", "minecraft:golden_horse_armor",
            "minecraft:diamond_horse_armor");
        at("release_1_0_0", "The End arrives in Beta 1.9 Prerelease 4 with ender pearls; the first shape of the "
            + "endgame loot is Release 1.0 content",
            "minecraft:ender_pearl");
        at("release_1_0_0", "Music discs 13 and cat are dungeon and monster-room loot, and both exist by "
            + "Release 1.0",
            "minecraft:music_disc_13", "minecraft:music_disc_cat");
        at("alpha_1_2_0", "The Halloween Update adds the Nether, and nether wart with it",
            "minecraft:nether_wart");
        at("release_1_1", "Release 1.1 adds the first block and item variants that show up as loot, including "
            + "the melon family",
            "minecraft:melon_seeds");
        at("release_1_5", "Nether quartz arrives in Release 1.5 (13w01a)",
            "minecraft:quartz");
        at("release_1_2_5", "Jungle wood, cocoa and the jungle temple arrive in Release 1.2",
            "minecraft:jungle_log");
        // Modern additions that every declared version predates. Named individually rather than by a cutoff, because
        // the point is that a specific chest entry is impossible in these worlds and not that a whole era is barred.
        never("Bamboo is Release 1.14 content, and no declared version reaches it", "minecraft:bamboo");
        never("The glistering melon slice is a brewing ingredient from Release 1.0 at the earliest, and appears in no "
            + "declared version's loot", "minecraft:glistering_melon_slice");
        // CORRECTED 2026-10-21 after the owner reported that an enchanted golden apple could not be crafted in a
        // Release 1.3 world. The earlier note here said the item itself was 1.9 content, which is wrong: minecraft.wiki,
        // Enchanted Golden Apple, Java Edition history, has it added in 1.3.1 (12w21a) as a second golden-apple tier
        // crafted from eight gold blocks, and made UNCRAFTABLE in 1.9 (15w44a), when it also entered chest loot. What is
        // 1.9 is therefore its appearance in LOOT, which is the only thing this table decides - so the entry stands,
        // and the reason now says what it actually means. Crafting it is RemovedRecipes' job.
        never("The enchanted golden apple entered chest loot in 1.9 (15w44a), so no declared version's chests contain "
            + "one. The ITEM is older - 1.3.1 - and is craftable from eight gold blocks in the versions that had the "
            + "recipe; see RemovedRecipes and Feature.LEGACY_ENCHANTED_GOLDEN_APPLE.",
            "minecraft:enchanted_golden_apple");
        never("Spectral arrows, tipped arrows, crossbows, shields, tridents, elytra, shulker boxes, totems, prismarine "
            + "crystals, heart of the sea, lodestones, netherite, sculk, amethyst, honey, glow berries, sweet berries, "
            + "kelp, turtle items, banners, the axe/hoe variants of gold and iron tools where they postdate the era, "
            + "and the modern smithing templates are all later than every declared version",
            "minecraft:spectral_arrow", "minecraft:tipped_arrow", "minecraft:crossbow", "minecraft:shield",
            "minecraft:trident", "minecraft:elytra", "minecraft:shulker_box", "minecraft:totem_of_undying",
            "minecraft:prismarine_crystals", "minecraft:heart_of_the_sea", "minecraft:lodestone",
            "minecraft:netherite_ingot", "minecraft:netherite_scrap", "minecraft:netherite_upgrade_smithing_template",
            "minecraft:sculk", "minecraft:sculk_catalyst", "minecraft:sculk_sensor", "minecraft:echo_shard",
            "minecraft:amethyst_shard", "minecraft:honey_bottle", "minecraft:honeycomb", "minecraft:glow_berries",
            "minecraft:sweet_berries", "minecraft:ominous_bottle", "minecraft:trial_key",
            "minecraft:disc_fragment_5", "minecraft:goat_horn", "minecraft:bell", "minecraft:candle",
            "minecraft:chain", "minecraft:tuff", "minecraft:calcite", "minecraft:moss_block",
            "minecraft:blue_ice", "minecraft:packed_ice", "minecraft:suspicious_stew", "minecraft:cake",
            "minecraft:pumpkin_pie", "minecraft:beetroot_seeds", "minecraft:beetroot_soup",
            "minecraft:fire_charge", "minecraft:rabbit_hide", "minecraft:rabbit_foot", "minecraft:cooked_rabbit",
            "minecraft:rabbit", "minecraft:prismarine_shard", "minecraft:chorus_fruit", "minecraft:popped_chorus_fruit",
            "minecraft:dragon_breath", "minecraft:lingering_potion", "minecraft:splash_potion",
            "minecraft:tipped_arrow", "minecraft:cod", "minecraft:salmon", "minecraft:cooked_cod",
            "minecraft:cooked_salmon", "minecraft:tropical_fish", "minecraft:pufferfish", "minecraft:clownfish");
    }

    private HistoricalItemAvailability() {
    }

    /** The item's recorded first appearance, or null when nothing has dated it. */
    public static IntroducedIn introducedIn(String itemId) {
        return INTRODUCED.get(itemId);
    }

    /**
     * Whether an item may appear in a world of this version.
     *
     * <p>An item nobody has dated is ALLOWED: the alternative makes an unknown item vanish from a chest, and this
     * table exists to remove content that did not exist rather than to police content that might not have.
     */
    public static boolean existsIn(String versionId, String itemId) {
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElse(null);
        if (version == null) {
            return true;
        }
        IntroducedIn introduced = INTRODUCED.get(itemId);
        return introduced == null || version.chronology() >= introduced.chronology();
    }

    /** Every dated item, for diagnostics and tests. */
    public static Map<String, IntroducedIn> all() {
        return Map.copyOf(INTRODUCED);
    }

    /** How many items are dated. */
    public static int size() {
        return INTRODUCED.size();
    }
}
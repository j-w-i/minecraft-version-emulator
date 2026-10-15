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
import java.util.Set;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Which enchantments a version had, and the two rules the pre-Release-1.8 table followed.
 *
 * <h2>Which enchantments, and when</h2>
 *
 * <p>From minecraft.wiki, Enchantment &gt; History. Beta 1.9 Prerelease 4 is the build that adds the first seventeen:
 * Aqua Affinity, Bane of Arthropods, Blast Protection, Efficiency, Feather Falling, Fire Aspect, Fire Protection,
 * Fortune, Knockback, Looting, Projectile Protection, Protection, Respiration, Sharpness, Silk Touch, Smite and
 * Unbreaking. Release 1.1 adds Flame, Infinity, Power and Punch. Release 1.4.2 (12w50a) adds Thorns. Release 1.7.2
 * adds Luck of the Sea and Lure, which is past the newest profile this project declares.
 *
 * <p>Enchanting before Prerelease 3 did not exist at all, and Prerelease 3 applied no real enchantment - see
 * {@link HistoricalGameplayTweaks#functionalEnchanting} and {@code #placeholderEnchantmentTooltip}.
 *
 * <h2>The two rules of the old table</h2>
 *
 * <ol>
 *   <li><b>The cost is the option's own requirement.</b> Release 1.8 "reduced experience cost from a cost equal to
 *       the enchant level to a flat 1, 2, or 3". So before 1.8, choosing the third option of a level-30 offer costs
 *       30 levels, not 3 - and the modern GUI's level display, which is the option's requirement, is the number that
 *       is charged.</li>
 *   <li><b>No lapis.</b> Lapis arrived as a secondary cost in the same overhaul (14w02a). Before that the table took
 *       levels and nothing else, so the lapis slot is refused and its requirement is ignored.</li>
 * </ol>
 */
public final class HistoricalEnchanting {
    /** Where the pre-1.8 table's rules stop: Release 1.8 (14w02a) is the overhaul. */
    public static final int MODERN_ENCHANTING_FROM =
        HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 8, 0);

    /** Release 1.4.2 (12w50a), where Thorns arrives. */
    public static final int THORNS_FROM =
        HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 4, 2);

    private static final Map<ResourceKey<Enchantment>, Integer> INTRODUCED = new LinkedHashMap<>();

    private static void at(int chronology, ResourceKey<Enchantment>... keys) {
        for (ResourceKey<Enchantment> key : keys) {
            INTRODUCED.put(key, chronology);
        }
    }

    static {
        int prereleaseFour = HistoricalVersions
            .byId("historical_experience:beta_1_9_pre_3").orElseThrow().chronology() + 1;
        at(prereleaseFour,
            Enchantments.AQUA_AFFINITY, Enchantments.BANE_OF_ARTHROPODS, Enchantments.BLAST_PROTECTION,
            Enchantments.EFFICIENCY, Enchantments.FEATHER_FALLING, Enchantments.FIRE_ASPECT,
            Enchantments.FIRE_PROTECTION, Enchantments.FORTUNE, Enchantments.KNOCKBACK, Enchantments.LOOTING,
            Enchantments.PROJECTILE_PROTECTION, Enchantments.PROTECTION, Enchantments.RESPIRATION,
            Enchantments.SHARPNESS, Enchantments.SILK_TOUCH, Enchantments.SMITE, Enchantments.UNBREAKING);
        at(HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 1, 0),
            Enchantments.FLAME, Enchantments.INFINITY, Enchantments.POWER, Enchantments.PUNCH);
        at(THORNS_FROM, Enchantments.THORNS);
        // Release 1.7.2 adds Luck of the Sea and Lure, which no declared profile reaches; listed so the boundary is
        // stated rather than implied by their absence.
        at(HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 7, 2),
            Enchantments.LUCK_OF_THE_SEA, Enchantments.LURE);
    }

    private HistoricalEnchanting() {
    }

    /**
     * Whether a version had this enchantment at all.
     *
     * <p>An enchantment with no recorded date is REFUSED rather than allowed, which is the opposite direction to
     * {@link HistoricalItemAvailability}. The two look inconsistent and are not: an undated ITEM may stay in a chest
     * because its presence is ordinary content, while an undated ENCHANTMENT is one this table has never heard of -
     * a modern addition, or another mod's - and handing one out in a Beta world is exactly the anachronism being
     * prevented. Every enchantment in a vanilla registry is dated here, so nothing legitimate is caught by it.
     */
    public static boolean available(String versionId, ResourceKey<Enchantment> enchantment) {
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElse(null);
        if (version == null) {
            return true;
        }
        Integer introduced = INTRODUCED.get(enchantment);
        return introduced != null && version.chronology() >= introduced;
    }

    /**
     * Whether the table charges the offer's own level requirement and needs no lapis.
     *
     * <p>True for every declared profile, because the overhaul is Release 1.8 and no profile reaches it. Written as
     * a boundary rather than as {@code true} so that a profile declared later cannot silently inherit the old rules.
     */
    public static boolean usesOldTableRules(String versionId) {
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElse(null);
        return version != null && version.chronology() < MODERN_ENCHANTING_FROM;
    }

    /** Every enchantment this table knows, with the chronology it arrived at, for tests and diagnostics. */
    public static Map<ResourceKey<Enchantment>, Integer> all() {
        return Map.copyOf(INTRODUCED);
    }

    /** The enchantments that arrived in Beta 1.9 Prerelease 4, as a set, for tests. */
    public static Set<ResourceKey<Enchantment>> prereleaseFourEnchantments() {
        int prereleaseFour = HistoricalVersions
            .byId("historical_experience:beta_1_9_pre_3").orElseThrow().chronology() + 1;
        return INTRODUCED.entrySet().stream()
            .filter(entry -> entry.getValue() == prereleaseFour)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** How many enchantments are dated. */
    public static int size() {
        return INTRODUCED.size();
    }

    /**
     * What Beta 1.9 Prerelease 3 actually puts on an item, which is FEATHER FALLING I - fixed, not rolled.
     *
     * <h2>Why this build applies a real enchantment when it had none of its own</h2>
     *
     * <p>Prerelease 3 had the table, the interface, the level cost and the glint, and <b>no enchantment data at
     * all</b>: minecraft.wiki records that enchanted items simply showed "Enchanted!!" and a blue name. Filtering the
     * offer strictly by this version's own set therefore produced an EMPTY list, so the click had nothing to apply,
     * refunded the levels, and the item came back unenchanted with no glint - the build's real behaviour (a glint)
     * was lost in order to imitate the part it lacked (which enchantment it was).
     *
     * <p>{@link #prereleaseFourEnchantments()} is deliberately NOT used here. The owner's rule is that every item
     * enchanted in Prerelease 3 comes out as Feather Falling I once the world reaches Prerelease 4, so the same item
     * reveals the same enchantment in every world - no roll, no seed, nothing that can disagree between the two
     * builds. Note that Feather Falling IS one of the seventeen that arrived in Prerelease 4, so the reveal is also
     * an enchantment the destination version genuinely has.
     */
    public static ResourceKey<Enchantment> placeholderEnchantment() {
        return Enchantments.FEATHER_FALLING;
    }

    /** The level the placeholder is applied at. Always one. */
    public static int placeholderEnchantmentLevel() {
        return 1;
    }
}
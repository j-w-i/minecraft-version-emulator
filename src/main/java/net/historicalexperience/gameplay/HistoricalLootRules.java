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

import net.historicalexperience.version.DropRule;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;


/**
 * What a historical gameplay profile's mobs drop when they die.
 *
 * <p>A gameplay rule, not a generation revision: loot happens at runtime and changes no stored chunk,
 * so it is keyed by the gameplay profile and applies to every revision of the era.
 *
 * <h2>The rule, from the client</h2>
 *
 * <p>Beta 1.7.3 gave every mob a single drop item through {@code EntityLiving.getDropItemId()} and
 * dropped {@code random.nextInt(3)} copies of it on death — 0, 1 or 2, one at a time, with no
 * player-kill condition and no rare-drop table. Each value below was read from the mob class's own
 * {@code getDropItemId()} in the hash-verified client, and the item field it returns was resolved to
 * its name in the item class's static initialiser. That last step is not optional: the fields are
 * single letters, and {@code gm.J} (feather) differs from {@code gm.j} (arrow) only by case.
 *
 * <p>The modern loot tables differ for all four, which is why the whole table has to be replaced rather
 * than adjusted: a modern zombie also drops rotten flesh, iron ingots, carrots and potatoes; a modern
 * chicken drops chicken; a modern spider drops a spider eye; a modern cow drops beef. None of those
 * items exist in the Beta 1.7.3 item registry at all — it has no {@code flesh}, no {@code beef}, no
 * {@code chicken} and no {@code spiderEye} entry — so the replacement is a content difference, not a
 * tuning difference.
 *
 * <p>Experience is suppressed separately and for every mob: Beta 1.7.3's entity registry contains no
 * experience orb at all, so nothing may award experience in a world of that era.
 */
public final class HistoricalLootRules {
    /** The verified Beta 1.7.3 drops, in a fixed order so reports and tests are stable. */
    public static final List<DropRule> BETA_1_7_3 = List.of(
        new DropRule("minecraft:zombie", "minecraft:feather", 3,
            "uz.j() returns gm.J, assigned from the item named \"feather\""),
        new DropRule("minecraft:chicken", "minecraft:feather", 3,
            "ww.j() returns gm.J, the same item, which is why both dropped feathers"),
        new DropRule("minecraft:spider", "minecraft:string", 3,
            "cn.j() returns gm.I, assigned from the item named \"string\""),
        new DropRule("minecraft:cow", "minecraft:leather", 3,
            "bx.j() returns gm.aD, assigned from the item named \"leather\"")
    );

    /**
     * Beta 1.8.1's drops: the Adventure Update replaced every one of the four.
     *
     * <p><b>Provisional.</b> Rotten flesh arrived with hunger, raw chicken, raw beef and spider eyes
     * all arrive in Beta 1.8, and none of them are in the Beta 1.7.3 item registry. The exact Beta 1.8
     * count distributions are not verified, and this model carries one item per mob, so secondary and
     * probabilistic drops (a spider's eye alongside its string) are not yet expressed.
     */
    public static final List<DropRule> BETA_1_8_1 = List.of(
        new DropRule("minecraft:zombie", "minecraft:rotten_flesh", 3,
            "PROVISIONAL: rotten flesh arrives with hunger in Beta 1.8"),
        // 2, not 1. The model expresses a count as random.nextInt(bound), so a bound of 1 can only ever
        // produce zero and the mob would never drop anything - which is exactly what the first measured
        // run showed as chicken={}. Beta 1.8's chicken dropped one raw chicken, which this model cannot
        // express exactly; 2 means 0 or 1 and is the closest it gets until the count carries a minimum.
        new DropRule("minecraft:chicken", "minecraft:chicken", 2,
            "PROVISIONAL: raw chicken arrives in Beta 1.8, verified present in its item registry; the "
                + "count is 0 or 1 because the model expresses random.nextInt(bound) and cannot yet "
                + "express a guaranteed single item"),
        new DropRule("minecraft:spider", "minecraft:string", 3,
            "PROVISIONAL: primary drop only; Beta 1.8's spider eye is not expressed by this model"),
        new DropRule("minecraft:cow", "minecraft:beef", 4,
            "PROVISIONAL: raw beef arrives in Beta 1.8; exact count distribution unverified")
    );

    /** Beta 1.7.3's drops keyed by entity type id. */
    public static final Map<String, DropRule> BETA_1_7_3_BY_ENTITY = BETA_1_7_3.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(
            DropRule::entityTypeId, rule -> rule));

    /** Beta 1.8.1's drops keyed by entity type id. */
    public static final Map<String, DropRule> BETA_1_8_1_BY_ENTITY = BETA_1_8_1.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(
            DropRule::entityTypeId, rule -> rule));

    /**
     * Indev 20100223's death drops, which are the Indev 20100219 tables.
     *
     * <p>Every row is a VERIFIED wiki row already recorded in {@code research/feature-timeline.json}:
     * <i>"Zombies drop 0-2 feathers"</i>, <i>"Skeletons now drop arrows"</i>, <i>"Pigs now drop 0-2 raw
     * porkchops instead of mushrooms"</i> and <i>"Spiders now drop 0-2 string"</i>, all dated 20100219, which
     * precedes this build by four days. Bones are NOT here: they arrive in Beta 1.2. Nor is rotten flesh, which
     * arrives in the Beta 1.8 Pre-release.
     *
     * <p>The counts are the model's {@code random.nextInt(bound)}, so a "0-2" row is a bound of 3 - the same
     * convention {@link #BETA_1_7_3} uses for the same three-item range.
     *
     * <p>Sheep are deliberately absent. Their Indev behaviour is not a death drop: minecraft.wiki, Sheep,
     * <i>"20100223: Sheep now drop only light gray cloth when hit"</i> - a punch drop whose colour changes in
     * Infdev 20100624, which belongs with the punching hook rather than in this table.
     */
    public static final List<DropRule> INDEV_20100223 = List.of(
        new DropRule("minecraft:creeper", "minecraft:gunpowder", 3,
            "Indev/Infdev creeper drop selector returns gunpowder; shared death loop nextInt(3), no music discs"),
        new DropRule("minecraft:zombie", "minecraft:feather", 3,
            "wiki Zombie, 20100219: zombies drop 0-2 feathers (chosen because chickens did not exist yet)"),
        new DropRule("minecraft:skeleton", "minecraft:arrow", 3,
            "wiki Skeleton, 20100219: skeletons now drop arrows; bones arrive in Beta 1.2"),
        new DropRule("minecraft:pig", "minecraft:porkchop", 3,
            "wiki Pig, 20100219: pigs drop 0-2 raw porkchops instead of mushrooms"),
        new DropRule("minecraft:spider", "minecraft:string", 3,
            "wiki Spider, 20100219: spiders now drop 0-2 string")
    );

    /** Indev 20100223's drops keyed by entity type id. */
    public static final Map<String, DropRule> INDEV_20100223_BY_ENTITY = INDEV_20100223.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(
            DropRule::entityTypeId, rule -> rule));

    private static final LongAdder REPLACED_DEATHS = new LongAdder();
    private static final LongAdder DROPPED_ITEMS = new LongAdder();
    private static final LongAdder SUPPRESSED_EXPERIENCE = new LongAdder();
    private static final Map<String, Integer> DROPS_BY_ENTITY = new ConcurrentHashMap<>();
    private static final Set<String> REPLACED_ENTITIES = ConcurrentHashMap.newKeySet();

    private HistoricalLootRules() {
    }

    /** Every version that currently replaces mob loot, for diagnostics and tests. */
    public static Set<String> restrictedProfiles() {
        return net.historicalexperience.version.HistoricalVersions.all().stream()
            .filter(version -> version.rules().replacesLoot())
            .map(net.historicalexperience.version.HistoricalVersion::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** True when this version suppresses experience rewards entirely. */
    public static boolean suppressesExperience(String profileId) {
        return net.historicalexperience.version.HistoricalVersions.byId(profileId)
            .map(version -> !version.rules().awardsExperience())
            .orElse(false);
    }

    /** Sheep meat arrived in Release 1.8; ordinary/unknown worlds must retain their own table. */
    public static boolean usesLegacySheepDrops(String profileId) {
        return profileId != null && net.historicalexperience.version.HistoricalVersions.byId(profileId)
            .map(version -> version.chronology() < net.historicalexperience.version.HistoricalVersions.chronology(
                net.historicalexperience.version.HistoricalVersions.ERA_RELEASE, 1, 8, 0))
            .orElse(false);
    }

    /** The version's drop for an entity type, or empty when that mob is not affected. */
    public static Optional<DropRule> forEntity(String profileId, String entityTypeId) {
        return net.historicalexperience.version.HistoricalVersions.byId(profileId)
            .map(version -> version.rules().mobDrops().get(entityTypeId));
    }

    /** Records a death whose loot was replaced. */
    public static void recordReplacedDeath(String entityTypeId) {
        REPLACED_DEATHS.increment();
        REPLACED_ENTITIES.add(entityTypeId);
        DROPS_BY_ENTITY.merge(entityTypeId, 0, Integer::sum);
    }

    /** Records one item dropped by the replacement. */
    public static void recordDrop(String entityTypeId) {
        DROPPED_ITEMS.increment();
        DROPS_BY_ENTITY.merge(entityTypeId, 1, Integer::sum);
    }

    /** Records one suppressed experience award. */
    public static void recordSuppressedExperience() {
        SUPPRESSED_EXPERIENCE.increment();
    }

    public static long replacedDeaths() {
        return REPLACED_DEATHS.sum();
    }

    public static long droppedItems() {
        return DROPPED_ITEMS.sum();
    }

    public static long suppressedExperience() {
        return SUPPRESSED_EXPERIENCE.sum();
    }

    public static Set<String> replacedEntityTypes() {
        return Set.copyOf(REPLACED_ENTITIES);
    }

    public static Map<String, Integer> dropsByEntity() {
        return Map.copyOf(DROPS_BY_ENTITY);
    }

    /** Diagnostics as an ordered map, for verdict reporting. */
    public static Map<String, String> diagnostics() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("restrictedProfiles", restrictedProfiles().toString());
        report.put("rules", Integer.toString(BETA_1_7_3.size() + BETA_1_8_1.size()));
        report.put("replacedDeaths", Long.toString(REPLACED_DEATHS.sum()));
        report.put("droppedItems", Long.toString(DROPPED_ITEMS.sum()));
        report.put("dropsByEntity", DROPS_BY_ENTITY.toString());
        report.put("suppressedExperience", Long.toString(SUPPRESSED_EXPERIENCE.sum()));
        return report;
    }

    /**
     * Whether the active version has an old-drop rule for this entity.
     *
     * <p>This is the gate the loot helper asks. Upstream has fourteen separate per-entity toggles; this project
     * asks the version instead, and reuses the boundary that is already tested here rather than introducing a
     * second one that could disagree with it.
     */
    public static boolean hasOldDrops(Level level, String entityTypeId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        String profileId = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        return profileId != null && forEntity(profileId, entityTypeId).isPresent();
    }
}

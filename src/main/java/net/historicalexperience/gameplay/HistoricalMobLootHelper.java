/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.gameplay;

import net.historicalexperience.version.ActiveVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import net.historicalexperience.mixin.common.SheepAccess;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Builds the mob loot tables earlier versions dropped, for {@code Mob.dropFromLootTable} to use.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/gameplay/MobLootHelper.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The table builders, the four generated tables, all fourteen entity
 * mappings and the sheep special case are upstream's.
 *
 * <h2>Why tables and not items</h2>
 *
 * <p>Upstream does not hand a mob a list of items; it builds a real {@link LootTable} and lets the vanilla death
 * path roll it. That is what keeps a Looting sword increasing the count
 * ({@code EnchantedCountIncreaseFunction}) and a mob that dies on fire dropping the cooked form
 * ({@code SmeltItemFunction} behind an on-fire predicate). The version this replaced cancelled the drop and
 * spawned items directly, so both mechanics were silently skipped while the visible drop looked correct.
 *
 * <h2>Two deliberate differences from upstream, both stated rather than hidden</h2>
 *
 * <ol>
 *   <li><b>One boundary instead of fourteen flags.</b> Upstream has a separate user toggle per entity
 *       ({@code OLD_ZOMBIE_DROPS}, {@code OLD_SPIDER_DROPS} and so on). This project gates by version, so the
 *       question asked here is whether the ACTIVE VERSION has an old-drop rule for the entity, which is the
 *       already-tested {@link HistoricalLootRules} boundary rather than a second one that could disagree.</li>
 *   <li><b>Lazy initialisation.</b> Upstream fills the map from a server-start call. The tables need a
 *       {@link RegistryAccess}, which is only available from a level, so they are built on first use and cached
 *       - which removes a lifecycle hook that could otherwise run before the registries exist.</li>
 * </ol>
 *
 * <p>Six of the fourteen entities (drowned, husk, stray, bogged, mooshroom, rabbit) do not exist in any version
 * this project declares. Their mappings are ported anyway so this file matches upstream, and no claim is made
 * that they are reachable.
 */
public final class HistoricalMobLootHelper {
    private static final Map<EntityType<?>, EntityLoot> LOOT_MAP = new HashMap<>();

    private HistoricalMobLootHelper() {
    }

    /** An entity's old table. Upstream pairs each with its own toggle; here the version decides. */
    private record EntityLoot(EntityType<?> entityType, LootTable lootTable) {
        EntityLoot {
            LOOT_MAP.put(entityType, this);
        }
    }

    /** Upstream: {@code addToTable}. One pool, 0..max, looting-aware, optionally smelted when on fire. */
    private static LootTable.Builder addToTable(LootTable.Builder builder, RegistryAccess registries,
            ItemLike item, int max, boolean canSmelt) {
        LootPool.Builder pool = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .add(LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, max)))
                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries,
                    UniformGenerator.between(0.0F, 1.0F))));

        if (canSmelt) {
            pool = pool.apply(SmeltItemFunction.smelted()
                .when(LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS,
                    EntityPredicate.Builder.entity().flags(
                        EntityFlagsPredicate.Builder.flags().setOnFire(true)))));
        }

        return builder.withPool(pool);
    }

    /** Upstream: {@code makeTable}. */
    private static LootTable makeTable(RegistryAccess registries, ItemLike item, int max) {
        return addToTable(LootTable.lootTable(), registries, item, max, false).build();
    }

    /** Upstream: {@code makePorkTable}. Pork is the one drop that smelts. */
    private static LootTable makePorkTable(RegistryAccess registries) {
        return addToTable(LootTable.lootTable(), registries, Items.PORKCHOP, 2, true).build();
    }

    /** Upstream: {@code makeSkeletonTable}. Two pools, so arrows and bones both drop. */
    private static LootTable makeSkeletonTable(RegistryAccess registries) {
        LootTable.Builder builder = LootTable.lootTable();

        addToTable(builder, registries, Items.ARROW, 2, false);
        addToTable(builder, registries, Items.BONE, 2, false);

        return builder.build();
    }

    /** Upstream: {@code WOOL_TABLE}, memoised per item so each colour is built once. */
    private static final BiFunction<RegistryAccess, ItemLike, LootTable> WOOL_TABLE =
        Util.memoize((registries, item) -> makeTable(registries, item, 1));

    /** Upstream: {@code init}. Built once against the level's registries. */
    private static void init(RegistryAccess registries) {
        if (!LOOT_MAP.isEmpty()) {
            return;
        }

        LootTable cookedPorkchop = makeTable(registries, Items.COOKED_PORKCHOP, 2);
        LootTable feather = makeTable(registries, Items.FEATHER, 2);
        LootTable string = makeTable(registries, Items.STRING, 2);
        LootTable leather = makeTable(registries, Items.LEATHER, 2);
        LootTable rabbitHide = makeTable(registries, Items.RABBIT_HIDE, 1);
        LootTable porkchop = makePorkTable(registries);
        LootTable arrowBone = makeSkeletonTable(registries);

        new EntityLoot(EntityType.ZOMBIFIED_PIGLIN, cookedPorkchop);
        new EntityLoot(EntityType.ZOMBIE, feather);
        new EntityLoot(EntityType.ZOMBIE_VILLAGER, feather);
        new EntityLoot(EntityType.DROWNED, feather);
        new EntityLoot(EntityType.HUSK, feather);
        new EntityLoot(EntityType.SPIDER, string);
        new EntityLoot(EntityType.CAVE_SPIDER, string);
        new EntityLoot(EntityType.COW, leather);
        new EntityLoot(EntityType.MOOSHROOM, leather);
        new EntityLoot(EntityType.RABBIT, rabbitHide);
        new EntityLoot(EntityType.CHICKEN, feather);
        new EntityLoot(EntityType.PIG, porkchop);
        new EntityLoot(EntityType.STRAY, arrowBone);
        new EntityLoot(EntityType.BOGGED, arrowBone);
    }

    /** Era tables built from a version's own rule, keyed by profile and entity so a 1.7.3 answer never serves 1.8.1. */
    private static final Map<String, LootTable> ERA_TABLES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The item a version's rule names for an entity, as an id, without touching a registry.
     *
     * <p>Exists so the version-dependent choice can be tested directly: the defect this replaced was that the
     * table cache ignored the version, so a test has to be able to ask "which item for this version" without a
     * running level.
     */
    public static String eraItemId(String profile, String entityTypeId) {
        return HistoricalLootRules.forEntity(profile, entityTypeId)
            .map(net.historicalexperience.version.DropRule::itemId).orElse(null);
    }

    /**
     * Builds the table for one version's rule, keeping the two special shapes the old global map had: bones join
     * arrows once Beta 1.2 added them, and pork smelts on fire once Beta 1.5 added that.
     */
    private static LootTable eraTable(RegistryAccess registries, EntityType<?> entityType,
            net.historicalexperience.version.DropRule rule, String profile) {
        String key = profile + "|" + entityType.builtInRegistryHolder().key().location() + "|" + rule.itemId()
            + "|" + rule.maxExclusiveCount();
        return ERA_TABLES.computeIfAbsent(key, ignored -> {
            var declared = net.historicalexperience.version.HistoricalVersions.byId(profile).orElse(null);
            boolean bones = declared != null
                && !declared.isBefore(net.historicalexperience.version.Milestones.BETA_1_2)
                && rule.itemId().equals("minecraft:arrow");
            if (bones) {
                return makeSkeletonTable(registries);
            }
            boolean smelts = declared != null
                && !declared.isBefore(net.historicalexperience.version.Milestones.BETA_1_5)
                && rule.itemId().equals("minecraft:porkchop");
            if (smelts) {
                return makePorkTable(registries);
            }
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(rule.itemId()));
            return makeTable(registries, item, rule.maxExclusiveCount() - 1);
        });
    }

    /**
     * Upstream: {@code getTable}. The entity's era table when the active version has one for it, otherwise the
     * table it was going to use anyway.
     */
    public static LootTable getTable(Entity entity, LootTable vanilla) {
        String entityTypeId =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        EntityType<?> entityType = entity.getType();

        String profile = entity.level() instanceof net.minecraft.server.level.ServerLevel server
            ? HistoricalProfileLookup.profileIdForLevel(server) : ActiveVersion.current();
        if (EarlyGameplayRecipes.supports(profile)) {
            // The old clients roll nextInt(3) of their one selected item: no looting, on-fire smelting,
            // music disc condition, bones, rare equipment, or sheep death wool. Sheep yield cloth when punched.
            var rule = HistoricalLootRules.forEntity(profile, entityTypeId);
            if (rule.isEmpty()) return LootTable.EMPTY;
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(rule.get().itemId()));
            return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(
                    UniformGenerator.between(0, rule.get().maxExclusiveCount() - 1))))).build();
        }

        // Upstream's sheep branch is independently gated by OLD_SHEEP_DROPS. Keep it before the
        // generic per-mob map, but restore the missing historical-world boundary: the old unconditional
        // port also replaced ordinary sheep death loot and removed its mutton. Meat arrives in Release 1.8.
        // The exact early clients above remain separate: cloth is a punch drop, not a death-table reward.
        if (entityType == EntityType.SHEEP && HistoricalLootRules.usesLegacySheepDrops(profile)) {
            RegistryAccess sheepRegistries = entity.level().registryAccess();
            init(sheepRegistries);
            Sheep sheep = (Sheep) entityType.tryCast(entity);

            // Upstream: a sheep that has not been sheared drops the wool of its own colour.
            if (sheep != null && !sheep.isSheared()) {
                return WOOL_TABLE.apply(sheepRegistries, SheepAccess.itemByDye().get(sheep.getColor()));
            }
            return LootTable.EMPTY;
        }

        // The version's OWN rule decides the item. The global LOOT_MAP below is built once from Beta 1.7.3's
        // tables, so using it here made Beta 1.8.1's chicken and cow resolve to the 1.7.3 feather and leather
        // tables: the Adventure Update's raw chicken and beef never dropped. Keyed lookup is per version.
        var eraRule = HistoricalLootRules.forEntity(profile, entityTypeId);
        if (eraRule.isPresent()) {
            return eraTable(entity.level().registryAccess(), entityType, eraRule.get(), profile);
        }

        if (!HistoricalLootRules.hasOldDrops(entity.level(), entityTypeId)) {
            return vanilla;
        }

        RegistryAccess registries = entity.level().registryAccess();
        init(registries);

        EntityLoot known = LOOT_MAP.get(entityType);
        if (known != null) {
            return known.lootTable();
        }

        return vanilla;
    }
}

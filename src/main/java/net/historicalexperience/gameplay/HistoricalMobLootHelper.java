package net.historicalexperience.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import net.historicalexperience.mixin.SheepAccess;
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

    /**
     * Upstream: {@code getTable}. The entity's era table when the active version has one for it, otherwise the
     * table it was going to use anyway.
     */
    public static LootTable getTable(Entity entity, LootTable vanilla) {
        String entityTypeId =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        EntityType<?> entityType = entity.getType();

        // THE SHEEP IS CHECKED BEFORE THE VERSION GATE, and that ordering is the fix for a reported defect.
        //
        // The owner reported that sheep still drop MUTTON, which arrived in Release 1.8 and so must not exist in
        // any of the 32 declared versions. It was reaching them because this branch used to sit BELOW the
        // hasOldDrops gate: that gate is true only for the versions that carry the four-mob drop table, so for
        // Alpha, Beta 1.0-1.6 and every Release profile the call fell through to the MODERN table - the one that
        // contains raw mutton. Measured from the jars rather than assumed: the string "mutton" occurs 0 times in
        // alpha-1.1.2_01, b1.7.3, b1.8.1, 1.0.0, 1.1, 1.2.5 and 1.6.4 language data, and 6 times in 1.8.9.
        //
        // Both branches below are mutton-free, so hoisting the whole branch is the correct fix rather than adding
        // a mutton filter somewhere downstream: a sheep of this era yields its wool, or nothing if already shorn.
        if (entityType == EntityType.SHEEP) {
            RegistryAccess sheepRegistries = entity.level().registryAccess();
            init(sheepRegistries);
            Sheep sheep = (Sheep) entityType.tryCast(entity);

            // Upstream: a sheep that has not been sheared drops the wool of its own colour.
            if (sheep != null && !sheep.isSheared()) {
                return WOOL_TABLE.apply(sheepRegistries, SheepAccess.itemByDye().get(sheep.getColor()));
            }
            return LootTable.EMPTY;
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

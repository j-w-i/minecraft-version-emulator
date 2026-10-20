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
package net.historicalexperience.verification;

import net.historicalexperience.gameplay.HistoricalProfileLookup;

import java.util.*;
import net.historicalexperience.gameplay.HistoricalLootRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;

/** Real hurt/death/custom-loot methods on untracked mobs, real emitted item/orb entities, only owned probe cleanup.
 * Not natural spawning, entity ticking, client/network combat or pickup acceptance.
 */
final class EarlyMobDeathVerification {
    private EarlyMobDeathVerification() { }
    private record Result(Map<String, Integer> items, int orbs, List<ItemStack> stacks) { }
    static Map<String, Object> verify(ServerLevel level, ServerPlayer player, boolean early) {
        if (!Boolean.getBoolean("historical_experience.generationProbe")) throw new IllegalStateException("Probe only");
        boolean drops = level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        ItemStack held = player.getMainHandItem().copy();
        var marker = new ItemEntity(level, 8, 90, 8, new ItemStack(Items.DIAMOND, 7));
        check(level.addFreshEntity(marker), "Cannot install owned preexisting-item sentinel");
        int deaths = 0;
        try {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, level.getServer());
            // Force the rare charged-killer routes first: these do NOT live in normal loot tables.
            var charged = (Creeper) create(level, EntityType.CREEPER, 11);
            var chargedData = new CompoundTag(); charged.saveWithoutId(chargedData); chargedData.putBoolean("powered", true); charged.load(chargedData);
            check(charged.isPowered(), "Charged-killer probe setup failed");
            for (var type : List.of(EntityType.ZOMBIE, EntityType.SKELETON)) {
                var victim = create(level, type, 13);
                Result result = kill(level, victim, level.damageSources().mobAttack(charged)); deaths++;
                if (early) check(result.items.keySet().stream().noneMatch(id -> id.endsWith("_head") || id.endsWith("_skull")),
                    "Early charged-killer custom death leaked a modern skull: " + type + " " + result.items);
                else check(result.items.containsKey(type == EntityType.ZOMBIE ? "minecraft:zombie_head" : "minecraft:skeleton_skull"),
                    "Ordinary charged-killer lost its head drop: " + result.items);
                // Each modern charged creeper has a one-skull budget; reset only this untracked probe actor.
                charged = (Creeper) create(level, EntityType.CREEPER, 11); charged.saveWithoutId(chargedData);
                chargedData.putBoolean("powered", true); charged.load(chargedData);
            }
            charged.discard();
            var skeletonKiller = create(level, EntityType.SKELETON, 17);
            var disc = kill(level, create(level, EntityType.CREEPER, 19), level.damageSources().mobAttack(skeletonKiller)); deaths++;
            boolean hasDisc = disc.items.keySet().stream().anyMatch(id -> id.startsWith("minecraft:music_disc_"));
            check(hasDisc == !early, "Creeper skeleton-kill disc rule changed: " + disc.items); skeletonKiller.discard();
            var counts = new TreeMap<String, Integer>(); int observedOrbs = 0;
            for (int looting : new int[] {0, 3}) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                if (looting > 0) sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), looting);
                player.setItemSlot(EquipmentSlot.MAINHAND, sword);
                for (var type : List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.PIG, EntityType.SPIDER, EntityType.CREEPER)) {
                    String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
                    for (int seed = 1; seed <= 8; seed++) {
                        Result result = kill(level, create(level, type, seed), level.damageSources().playerAttack(player)); deaths++;
                        if (early) {
                            var rule = HistoricalLootRules.forEntity(net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(level), id).orElseThrow();
                            int expected = net.minecraft.util.RandomSource.create(seed).nextInt(3);
                            check(result.items.equals(expected == 0 ? Map.of() : Map.of(rule.itemId(), expected)),
                                "Real early death/count/looting differs: " + id + " seed=" + seed + " looting=" + looting + " " + result.items + " expected=" + expected);
                            check(result.orbs == 0, "Early player kill awarded XP orbs");
                        }
                        result.items.forEach((item, count) -> counts.merge(item, count, Integer::sum)); observedOrbs += result.orbs;
                    }
                }
            }
            check(early ? observedOrbs == 0 : observedOrbs > 0, "Death XP control is vacuous/changed: " + observedOrbs);
            for (int seed = 1; seed <= 8; seed++) {
                var pig = create(level, EntityType.PIG, seed); pig.setRemainingFireTicks(200);
                var burnt = kill(level, pig, level.damageSources().playerAttack(player)); deaths++;
                if (early) {
                    int expected = net.minecraft.util.RandomSource.create(seed).nextInt(3);
                    check(burnt.items.equals(expected == 0 ? Map.of() : Map.of("minecraft:porkchop", expected)), "Early fire cooked/boosted pork: " + burnt.items);
                } else check(burnt.items.containsKey("minecraft:cooked_porkchop") && !burnt.items.containsKey("minecraft:porkchop"), "Ordinary fire pork changed");
            }
            var armed = create(level, EntityType.SKELETON, 29);
            armed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW)); armed.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            var equipment = kill(level, armed, level.damageSources().playerAttack(player)); deaths++;
            check(equipment.items.containsKey("minecraft:bow") == !early, "Skeleton custom equipment gate failed: " + equipment.items);
            // An imported stored weapon is NOT newly generated skull loot. Do not solve the leak by dropping all custom death loot.
            var imported = create(level, EntityType.ZOMBIE, 37);
            ItemStack priorGear = new ItemStack(Items.NETHERITE_SWORD); priorGear.setDamageValue(17);
            priorGear.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("owned future-gear sentinel"));
            var payload = new CompoundTag(); payload.putString("preserve", "early-death-fixture");
            priorGear.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(payload));
            imported.setItemSlot(EquipmentSlot.MAINHAND, priorGear.copy()); imported.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            var priorKiller = (Creeper) create(level, EntityType.CREEPER, 39);
            var priorKillerData = new CompoundTag(); priorKiller.saveWithoutId(priorKillerData); priorKillerData.putBoolean("powered", true); priorKiller.load(priorKillerData);
            var priorLoot = kill(level, imported, level.damageSources().mobAttack(priorKiller)); deaths++; priorKiller.discard();
            check(priorLoot.stacks.stream().filter(s -> s.is(Items.NETHERITE_SWORD)).count() == 1
                && priorLoot.stacks.stream().anyMatch(s -> s.getCount() == 1 && ItemStack.isSameItemSameComponents(s, priorGear)),
                "Stored future equipment/components were lost or changed");
            var sheep = (Sheep) create(level, EntityType.SHEEP, 31, mob -> ((Sheep) mob).setColor(DyeColor.RED));
            var first = collect(level, () -> sheep.hurt(level.damageSources().playerAttack(player), 1));
            if (early) check(sheep.isSheared() && first.items.size() == 1 && first.items.getOrDefault("minecraft:light_gray_wool", 0) >= 1
                && first.items.get("minecraft:light_gray_wool") <= 3, "Early sheep punch/cloth rule failed: " + first.items);
            else check(!sheep.isSheared() && first.items.isEmpty(), "Ordinary sheep was punch-sheared");
            var second = collect(level, () -> sheep.hurt(level.damageSources().playerAttack(player), 1));
            check(second.items.isEmpty(), "Repeated sheep punch duplicated wool");
            var sheepDeath = kill(level, sheep, level.damageSources().playerAttack(player)); deaths++;
            if (early) check(sheepDeath.items.isEmpty(), "Sheep death added modern wool/mutton: " + sheepDeath.items);
            else check(sheepDeath.items.containsKey("minecraft:red_wool") && sheepDeath.items.containsKey("minecraft:mutton"), "Ordinary sheep death loot changed: " + sheepDeath.items);
            var environment = (Sheep) create(level, EntityType.SHEEP, 41);
            var environmentalHit = collect(level, () -> environment.hurt(level.damageSources().generic(), 1));
            check(!environment.isSheared() && environmentalHit.items.isEmpty(), "Non-living damage punch-sheared sheep");
            var environmentalDeath = kill(level, environment, level.damageSources().generic()); deaths++;
            if (early) check(environmentalDeath.items.isEmpty(), "Environmental sheep death awarded wool/mutton");
            var lethal = (Sheep) create(level, EntityType.SHEEP, 43);
            var lethalDrops = kill(level, lethal, level.damageSources().playerAttack(player)); deaths++;
            if (early) check(lethal.isSheared() && lethalDrops.items.size() == 1
                && lethalDrops.items.getOrDefault("minecraft:light_gray_wool", 0) >= 1 && lethalDrops.items.get("minecraft:light_gray_wool") <= 3,
                "Killing blow did not punch-shear before the empty death table: " + lethalDrops.items);
            check(!marker.isRemoved() && marker.getItem().is(Items.DIAMOND) && marker.getItem().getCount() == 7,
                "Probe cleanup consumed an existing item sentinel");
            return Map.ofEntries(Map.entry("realDeaths", deaths), Map.entry("early", early), Map.entry("lootItems", counts),
                Map.entry("observedTableDeathXpOrbs", observedOrbs), Map.entry("chargedSkullCases", 2),
                Map.entry("skeletonDiscCase", true), Map.entry("onFirePigCases", 8), Map.entry("heldBowCase", true),
                Map.entry("sheepPunchOnceAndDeath", true), Map.entry("sheepEnvironmentalAndLethalCases", true),
                Map.entry("storedFutureGearPreserved", true), Map.entry("preexistingItemPreserved", true));
        } finally {
            marker.discard(); player.setItemSlot(EquipmentSlot.MAINHAND, held);
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(drops, level.getServer());
        }
    }
    private static Mob create(ServerLevel level, EntityType<? extends Mob> type, long seed) {
        return create(level, type, seed, mob -> { });
    }
    private static Mob create(ServerLevel level, EntityType<? extends Mob> type, long seed, java.util.function.Consumer<Mob> configure) {
        Mob mob = Objects.requireNonNull(type.create(level)); mob.setPos(8, 90, 8); configure.accept(mob);
        // Resolve the native table AFTER phenotype setup: freezing white before setting red gives a false control failure.
        var data = new CompoundTag(); mob.saveWithoutId(data);
        data.putString("DeathLootTable", mob.getLootTable().location().toString()); data.putLong("DeathLootTableSeed", seed);
        mob.load(data); check(mob.getLootTableSeed() == seed, "Loot seed was not installed through real NBT loading");
        return mob;
    }
    private static Result kill(ServerLevel level, Mob mob, DamageSource source) {
        try { return collect(level, () -> {
            check(mob.hurt(source, 1000), "Real damage was refused"); check(!mob.isAlive(), "Real damage did not kill the specimen");
        }); } finally { mob.discard(); }
    }
    private static Result collect(ServerLevel level, Runnable action) {
        try (var capture = EarlyMobDropCapture.open(level)) {
            action.run(); var items = new TreeMap<String, Integer>(); int orbs = 0; var stacks = new ArrayList<ItemStack>();
            for (var entity : capture.entities()) {
                if (entity instanceof ItemEntity item) {
                    items.merge(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString(), item.getItem().getCount(), Integer::sum);
                    stacks.add(item.getItem().copy());
                }
                if (entity instanceof ExperienceOrb) orbs++;
            }
            return new Result(Map.copyOf(items), orbs, List.copyOf(stacks));
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}

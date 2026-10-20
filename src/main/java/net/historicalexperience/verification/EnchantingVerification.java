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

import net.historicalexperience.gameplay.HistoricalChestLoot;
import net.historicalexperience.gameplay.HistoricalItemAvailability;
import net.historicalexperience.gameplay.HistoricalPortalSpawn;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalEnchanting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the real enchanting menu and reports which enchantments it offers, so the version gate can be checked against
 * the running game rather than only against its own rule. Inert unless
 * {@code -Dhistorical_experience.enchantProbe=true}.
 *
 * <p>A headless server has no player to open a table, so this builds the menu directly against a real server level and
 * asks it for the three offers, which is the same call the interface makes. The session's active version is set for the
 * duration and cleared afterwards, which is exactly how the client learns it.
 */
public final class EnchantingVerification {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/enchant-probe");

    private EnchantingVerification() {
    }

    public static void registerIfRequested() {
        if (Boolean.getBoolean("historical_experience.enchantProbe")) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> run(server)));
        }
    }

    private static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();

        // The loot half needs no player, so it runs first and always.
        try {
            lootProbe(server, level);
        } catch (RuntimeException failure) {
            LOGGER.error("LOOTPROBE failed", failure);
        }

        try {
            finiteProbe(level);
        } catch (RuntimeException failure) {
            LOGGER.error("FINITEPROBE failed", failure);
        }

        // The F4 portal, too: it needs no player either, and reading the blocks back is the only way to prove the
        // runtime writing path - unit tests cannot touch block states.
        try {
            portalProbe(level);
        } catch (RuntimeException failure) {
            LOGGER.error("PORTALPROBE failed", failure);
        }

        ServerPlayer player = null;
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            player = candidate;
            break;
        }
        if (player == null) {
            // No player connected: the menu needs an Inventory, so this cannot be exercised without one.
            LOGGER.info("ENCHANTPROBE skipped: no player connected");
            return;
        }
        try {
            player.giveExperienceLevels(60);
            for (String version : new String[] {"historical_experience:beta_1_9_pre_3",
                "historical_experience:beta_1_9_pre_4", "historical_experience:release_1_0_0",
                "historical_experience:release_1_6_4"}) {
                ActiveVersion.set(version);
                probe(level, player, version);
            }
            ActiveVersion.set(null);
            probe(level, player, "ordinary world");
            ActiveVersion.clear();
        } catch (ReflectiveOperationException | RuntimeException failure) {
            LOGGER.error("ENCHANTPROBE failed", failure);
        }
    }

    /**
     * Builds a real portal through the same path the F4 key uses and reads the blocks back, reporting the shape and
     * whether the opening came out lit.
     */
    private static void portalProbe(ServerLevel level) {
        net.minecraft.core.BlockPos origin = level.getSharedSpawnPos().offset(0, 20, 0);
        int written = net.historicalexperience.gameplay.HistoricalPortalSpawn.buildAt(level, origin,
            net.minecraft.core.Direction.Axis.X);

        int obsidian = 0;
        int portal = 0;
        int other = 0;
        boolean axisSet = true;
        java.util.Map<String, Integer> others = new java.util.TreeMap<>();
        for (var slot : net.historicalexperience.gameplay.HistoricalPortalSpawn.layout(origin,
            net.minecraft.core.Direction.Axis.X)) {
            var state = level.getBlockState(slot.pos());
            if (state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)) {
                obsidian++;
            } else if (state.is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL)) {
                portal++;
                if (state.getValue(net.minecraft.world.level.block.NetherPortalBlock.AXIS)
                    != net.minecraft.core.Direction.Axis.X) {
                    axisSet = false;
                }
            } else {
                other++;
                others.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(state.getBlock()).toString(), 1, Integer::sum);
            }
        }
        LOGGER.info("PORTALPROBE written={} obsidian={} portal={} other={} axisCorrect={} versionHasKey={} others={}",
            written, obsidian, portal, other, axisSet,
            net.historicalexperience.version.Features.active(net.historicalexperience.version.Feature.F4_PORTAL_KEY,
                net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(level)),
            others);
    }

    /**
     * What a finite era actually generates: the ground level through the middle, and whether the world runs out.
     *
     * <p>Reads the surface at a series of distances from the origin so the 256-block envelope can be seen rather than
     * assumed, and reports the world border the preset asks for.
     */
    private static void finiteProbe(ServerLevel level) {
        StringBuilder profile = new StringBuilder();
        int solidWithin = 0;
        int solidBeyond = 0;
        for (int distance : new int[] {8, 64, 120, 124, 126, 128, 130, 140, 200, 512}) {
            level.getChunk(distance >> 4, 0);
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                distance, 0);
            var at = level.getBlockState(new net.minecraft.core.BlockPos(distance, Math.max(1, surface - 1), 0));
            boolean solid = !at.isAir();
            if (distance <= 128) {
                if (solid) {
                    solidWithin++;
                }
            } else if (solid) {
                solidBeyond++;
            }
            profile.append(distance).append(':').append(surface).append(solid ? "S" : "a").append(' ');
        }
        var border = level.getWorldBorder();
        LOGGER.info("FINITEPROBE borderSize={} borderCenter={},{} surfaceWithin={} solidBeyond128={} profile={}",
            border.getSize(), (int) border.getCenterX(), (int) border.getCenterZ(), solidWithin, solidBeyond,
            profile.toString().trim());
    }

    /** The menu''s own offer calculation, which the interface calls. Private, so reached reflectively. */
    private static java.lang.reflect.Method enchantmentList() throws NoSuchMethodException {
        java.lang.reflect.Method method = EnchantmentMenu.class.getDeclaredMethod("getEnchantmentList",
            net.minecraft.core.RegistryAccess.class, ItemStack.class, int.class, int.class);
        method.setAccessible(true);
        return method;
    }

    /**
     * Rolls vanilla''s chest tables and reports what came out, so the filter is checked against real loot rather than
     * only against its own rule. Twenty rolls per table, because the point is which ITEMS can appear at all: a modern
     * item appearing once in any roll is a failure, and one that never appears across fifty rolls of fifty items is
     * good evidence it cannot.
     */
    private static void lootProbe(MinecraftServer server, ServerLevel level) {
        java.util.Map<String, Integer> seen = new java.util.TreeMap<>();
        int rolls = 0;
        for (String table : new String[] {"minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/stronghold_corridor", "minecraft:chests/desert_pyramid",
            "minecraft:chests/village/village_plains_house"}) {
            var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,
                net.minecraft.resources.ResourceLocation.parse(table));
            var lootTable = server.reloadableRegistries().getLootTable(key);
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                    net.minecraft.world.phys.Vec3.atCenterOf(level.getSharedSpawnPos()))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            for (int attempt = 0; attempt < 20; attempt++) {
                rolls++;
                for (var stack : lootTable.getRandomItems(params)) {
                    seen.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        1, Integer::sum);
                }
            }
        }
        java.util.List<String> refused = new java.util.ArrayList<>();
        for (String item : seen.keySet()) {
            if (!net.historicalexperience.gameplay.HistoricalItemAvailability
                    .existsIn("historical_experience:beta_1_7_3", item)) {
                refused.add(item);
            }
        }
        LOGGER.info("LOOTPROBE rolls={} distinctItems={} itemsThatShouldNotBeHere={} removedByFilter={}",
            rolls, seen.size(), refused, HistoricalChestLootTotal());
    }

    /** The filter''s own count, read through its diagnostics so the probe cannot invent a number. */
    private static long HistoricalChestLootTotal() {
        return net.historicalexperience.gameplay.HistoricalChestLoot.removedCount();
    }

    private static void probe(ServerLevel level, ServerPlayer player, String label) throws ReflectiveOperationException {
        EnchantmentMenu menu = new EnchantmentMenu(0, player.getInventory(),
            ContainerLevelAccess.create(level, BlockPos.containing(player.position())));
        // Leather boots: an item every version this project declares can enchant, and the one the owner's
        // "feather falling 1 gear" report was about.
        menu.getSlot(0).set(new ItemStack(Items.LEATHER_BOOTS));
        menu.slotsChanged(null);
        java.lang.reflect.Method calc = enchantmentList();
        StringBuilder offers = new StringBuilder();
        for (int option = 0; option < 3; option++) {
            @SuppressWarnings("unchecked")
            java.util.List<EnchantmentInstance> rolled = (java.util.List<EnchantmentInstance>) calc.invoke(
                menu, level.registryAccess(), menu.getSlot(0).getItem(), option, menu.costs[option]);
            offers.append(" [").append(option).append("] cost=").append(menu.costs[option]).append(" -> ");
            if (rolled.isEmpty()) {
                offers.append("none");
            } else {
                for (EnchantmentInstance instance : rolled) {
                    offers.append(instance.enchantment.unwrapKey().map(key -> key.location().toString())
                        .orElse("?")).append('/').append(instance.level).append(' ');
                }
            }
        }
        LOGGER.info("ENCHANTPROBE {}: active={} oldRules={}{}", label, ActiveVersion.current(),
            HistoricalEnchanting.usesOldTableRules(ActiveVersion.current()), offers);
    }
}
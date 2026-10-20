/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.verification;

import net.historicalexperience.version.ActiveVersion;

import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

/** Real server player/container/item methods, with an unjoined probe player and NO socket/client/login claim. */
final class EarlyPlayerGameplayVerification {
    private EarlyPlayerGameplayVerification() { }

    static Map<String, Object> verify(ServerLevel level, String profile) {
        if (!Boolean.getBoolean("historical_experience.generationProbe")) throw new IllegalStateException("Probe only");
        String previous = net.historicalexperience.version.ActiveVersion.current();
        try {
            // A real join binds this context via HistoricalVersionSync. This player is deliberately not joined.
            net.historicalexperience.version.ActiveVersion.set(profile);
            return verifyBound(level, profile);
        } finally {
            net.historicalexperience.version.ActiveVersion.set(previous);
        }
    }

    private static Map<String, Object> verifyBound(ServerLevel level, String profile) {
        var player = new ServerPlayer(level.getServer(), level,
            new GameProfile(UUID.randomUUID(), "HexEarlyProbe"), ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player,
            new CommonListenerCookie(player.getGameProfile(), 0, ClientInformation.createDefault(), false));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var report = new LinkedHashMap<String, Object>();
        report.put("scope", "unjoined ServerPlayer; real menu clicks and gameMode item use; no socket or client/login");
        boolean limited = level.getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING);
        try {
            level.getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING).set(false, level.getServer());
            if (!EarlyGameplayRecipes.supports(profile)) {
                if (profile == null) {
                    var menu = player.inventoryMenu; player.containerMenu = menu;
                    clear(player, menu, 4); menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
                    take(player, menu, "minecraft:oak_planks", 4);
                    player.setHealth(1); player.getFoodData().setFoodLevel(10);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
                    player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
                    check(player.isUsingItem() && player.getUseItemRemainingTicks() > 1 && player.getHealth() == 1,
                        "Ordinary food timing/healing changed");
                    player.stopUsingItem(); report.put("ordinaryMenuAndFood", true);
                    report.put("workstations", verifyWorkstations(level, player, false));
                    report.put("independentWorkstations", EarlyIndependentMenuVerification.verify(level, player, false));
                    report.put("mobDeaths", EarlyMobDeathVerification.verify(level, player, false));
                }
                return report;
            }
            int table = 0, inventory = 0;
            var menu = new CraftingMenu(41, player.getInventory(), ContainerLevelAccess.create(level, player.blockPosition()));
            player.containerMenu = menu;
            for (var row : EarlyGameplayRecipes.recipes(profile)) {
                if (row.dormant()) continue;
                clear(player, menu, 9); fill(menu, 3, row, 1); take(player, menu, row.result(), row.count());
                emptyGrid(menu, 9); table++;
            }
            player.containerMenu = player.inventoryMenu;
            for (var row : EarlyGameplayRecipes.recipes(profile)) {
                if (row.dormant() || row.width() > 2 || row.height() > 2) continue;
                clear(player, player.inventoryMenu, 4); fill(player.inventoryMenu, 2, row, 1);
                take(player, player.inventoryMenu, row.result(), row.count()); emptyGrid(player.inventoryMenu, 4); inventory++;
            }
            player.containerMenu = menu; clear(player, menu, 9);
            menu.getSlot(1).set(new ItemStack(Items.IRON_INGOT, 3));
            menu.getSlot(4).set(new ItemStack(Items.STICK, 3));
            menu.getSlot(7).set(new ItemStack(Items.FEATHER, 3));
            menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            check(player.getInventory().countItem(Items.ARROW) == 12, "Shift-click did not produce exactly 12 arrows");
            emptyGrid(menu, 9);
            clear(player, menu, 9);
            menu.getSlot(1).set(new ItemStack(Items.FLINT)); menu.getSlot(4).set(new ItemStack(Items.STICK));
            menu.getSlot(7).set(new ItemStack(Items.FEATHER));
            check(menu.getSlot(0).getItem().isEmpty(), "Modern flint arrow appeared in a real result slot");
            menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
            check(player.getInventory().countItem(Items.ARROW) == 0 && menu.getSlot(1).getItem().is(Items.FLINT)
                && menu.getSlot(1).getItem().getCount() == 1 && menu.getSlot(4).getItem().getCount() == 1
                && menu.getSlot(7).getItem().getCount() == 1 && menu.getCarried().isEmpty(),
                "Denied craft consumed ingredients or produced arrows");
            check(java.util.stream.IntStream.range(0, EarlyGameplayRecipes.recipes(profile).size()).noneMatch(i ->
                player.getRecipeBook().contains(ResourceLocation.fromNamespaceAndPath("historical_experience",
                    "early/" + profile.substring(profile.indexOf(':') + 1) + "/" + i))),
                "Unsynced synthetic recipe IDs entered the vanilla player recipe book");
            level.getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING).set(true, level.getServer());
            clear(player, menu, 9); menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
            take(player, menu, "minecraft:oak_planks", 4);
            report.put("tableCraftsTaken", table); report.put("inventoryCraftsTaken", inventory);
            report.put("shiftClickArrows", 12); report.put("deniedResultAndNoConsumption", true);
            report.put("noUnsyncedRecipeBookEntries", true); report.put("limitedCraftingCompatible", true);
            report.put("workstations", verifyWorkstations(level, player, true));
            report.put("independentWorkstations", EarlyIndependentMenuVerification.verify(level, player, true));
            report.put("mobDeaths", EarlyMobDeathVerification.verify(level, player, true));

            int foods = 0;
            for (String id : HistoricalFoodRules.healPoints(profile).keySet()) {
                int heal = HistoricalFoodRules.healPointsFor(profile, id).orElseThrow();
                for (float health : new float[] {1, player.getMaxHealth()}) {
                    player.getInventory().clearContent(); player.removeAllEffects(); player.stopUsingItem();
                    player.setHealth(health); player.getFoodData().setFoodLevel(20);
                    player.setItemInHand(InteractionHand.MAIN_HAND, stack(id));
                    var used = player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
                    check(used.consumesAction(), "Historical food use rejected: " + id + " at health " + health);
                    check(!player.isUsingItem(), "Historical right-click was not immediate: " + id);
                    check(player.getHealth() == Math.min(player.getMaxHealth(), health + heal), "Wrong actual heal: " + id);
                    check(player.getActiveEffects().isEmpty(), "Modern food effects escaped: " + id);
                    check(id.equals("minecraft:mushroom_stew") ? player.getMainHandItem().is(Items.BOWL)
                        && player.getMainHandItem().getCount() == 1 : player.getMainHandItem().isEmpty(),
                        "Food not consumed exactly once / wrong bowl return at health " + health + ": " + id);
                    foods++;
                }
            }
            int denied = 0;
            for (String id : new String[] {"honey_bottle", "suspicious_stew", "enchanted_golden_apple", "cookie", "chorus_fruit"}) {
                player.getInventory().clearContent(); player.stopUsingItem(); player.removeAllEffects(); player.setHealth(1);
                ItemStack input = stack(id); player.setItemInHand(InteractionHand.MAIN_HAND, input);
                var result = player.gameMode.useItem(player, level, input, InteractionHand.MAIN_HAND);
                check(!result.consumesAction() && !player.isUsingItem() && player.getMainHandItem() == input
                    && input.getCount() == 1 && player.getHealth() == 1 && player.getActiveEffects().isEmpty(),
                    "Later/specialized food worked or changed stored stack: " + id);
                denied++;
            }
            if (profile.endsWith(":indev_latest")) {
                for (var item : new net.minecraft.world.item.Item[] {Items.APPLE, Items.GOLDEN_APPLE}) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                    check(!player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction()
                        && !player.isUsingItem() && player.getMainHandItem().getCount() == 1,
                        "Infdev-only fruit worked in Indev");
                }
            }
            report.put("actualFoodUsesIncludingFullHealth", foods); report.put("laterSpecializedFoodsDenied", denied);
            report.put("immediateDirectHealingNoModernEffects", true);
            return report;
        } finally {
            player.stopUsingItem(); player.getInventory().clearContent();
            level.getGameRules().getRule(GameRules.RULE_LIMITED_CRAFTING).set(limited, level.getServer());
            // Never join/add/save this player, never bind a socket. No personal account/player files are used.
        }
    }
    /** Opens no block: directly exercises the menu used for an imported/preserved workstation. */
    private static Map<String, Object> verifyWorkstations(ServerLevel level, ServerPlayer player, boolean early) {
        var menu = new net.minecraft.world.inventory.StonecutterMenu(42, player.getInventory(),
            ContainerLevelAccess.create(level, player.blockPosition()));
        player.getInventory().clearContent(); player.containerMenu = menu;
        ItemStack input = new ItemStack(Items.STONE, 3); menu.getSlot(0).set(input);
        int recipes = menu.getNumRecipes();
        if (early) {
            check(recipes == 0 && menu.getSlot(1).getItem().isEmpty(),
                "Early stonecutter menu exposed " + recipes + " modern recipe-list entries despite single-lookup denial");
            int selected = menu.getSelectedRecipeIndex();
            // Vanilla acknowledges even an invalid button with true; selection/output are authoritative.
            menu.clickMenuButton(player, 0);
            check(menu.getSelectedRecipeIndex() == selected && menu.getSlot(1).getItem().isEmpty()
                && menu.getNumRecipes() == 0, "Early stonecutter button changed selection or exposed output");
            menu.clicked(1, 0, ClickType.QUICK_MOVE, player);
            check(menu.getSlot(0).getItem() == input && input.getCount() == 3 && menu.getCarried().isEmpty()
                && player.getInventory().isEmpty(), "Denied stonecutter changed inputs or produced output");
        } else {
            int slab = -1;
            for (int i = 0; i < recipes; i++) if (menu.getRecipes().get(i).value().getResultItem(level.registryAccess()).is(Items.STONE_SLAB)) slab = i;
            check(slab >= 0 && menu.clickMenuButton(player, slab), "Ordinary stonecutter lost its slab recipe");
            menu.clicked(1, 0, ClickType.PICKUP, player);
            check(menu.getCarried().is(Items.STONE_SLAB) && menu.getCarried().getCount() == 2
                && menu.getSlot(0).getItem().getCount() == 2, "Ordinary stonecutter output/consumption changed");
        }
        menu.setCarried(ItemStack.EMPTY); menu.getSlot(0).set(ItemStack.EMPTY);
        var smith = new net.minecraft.world.inventory.SmithingMenu(43, player.getInventory(),
            ContainerLevelAccess.create(level, player.blockPosition()));
        player.getInventory().clearContent(); player.containerMenu = smith;
        ItemStack template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemStack base = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack addition = new ItemStack(Items.NETHERITE_INGOT);
        smith.getSlot(0).set(template); smith.getSlot(1).set(base); smith.getSlot(2).set(addition);
        if (early) {
            check(smith.getSlot(3).getItem().isEmpty(), "Early smithing menu exposed a modern result");
            smith.clicked(3, 0, ClickType.QUICK_MOVE, player);
            check(smith.getCarried().isEmpty() && player.getInventory().isEmpty()
                && smith.getSlot(0).getItem() == template && template.getCount() == 1
                && smith.getSlot(1).getItem() == base && base.getCount() == 1
                && smith.getSlot(2).getItem() == addition && addition.getCount() == 1,
                "Denied smithing changed imported stacks or produced output");
        } else {
            check(smith.getSlot(3).getItem().is(Items.NETHERITE_SWORD), "Ordinary smithing lost its result");
            smith.clicked(3, 0, ClickType.PICKUP, player);
            check(smith.getCarried().is(Items.NETHERITE_SWORD) && smith.getCarried().getCount() == 1,
                "Ordinary smithing could not take its result");
            for (int i = 0; i < 3; i++) check(smith.getSlot(i).getItem().isEmpty(), "Ordinary smithing did not consume one of each input");
        }
        smith.setCarried(ItemStack.EMPTY);
        for (int i = 0; i < 3; i++) smith.getSlot(i).set(ItemStack.EMPTY);
        player.containerMenu = player.inventoryMenu;
        return Map.of("menuRecipes", recipes, "earlyBlocked", early, "ordinarySlabsTaken", early ? 0 : 2,
            "smithingBlocked", early, "ordinarySmithingTaken", early ? 0 : 1);
    }

    private static void fill(AbstractContainerMenu menu, int width, EarlyGameplayRecipes.Recipe row, int count) {
        for (int y = 0; y < row.height(); y++) for (int x = 0; x < row.width(); x++) {
            String id = row.ingredients().get(x + y * row.width());
            if (!id.isEmpty()) { ItemStack input = stack(id); input.setCount(count); menu.getSlot(1 + x + y * width).set(input); }
        }
    }
    private static void clear(ServerPlayer player, AbstractContainerMenu menu, int cells) {
        player.getInventory().clearContent(); menu.setCarried(ItemStack.EMPTY);
        for (int i = 1; i <= cells; i++) menu.getSlot(i).set(ItemStack.EMPTY);
    }
    private static void take(ServerPlayer player, AbstractContainerMenu menu, String item, int count) {
        ItemStack output = menu.getSlot(0).getItem();
        check(output.is(stack(item).getItem()) && output.getCount() == count, "Wrong real result slot: " + item + " / " + output);
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(menu.getCarried().is(stack(item).getItem()) && menu.getCarried().getCount() == count, "Wrong taken result: " + item);
        menu.setCarried(ItemStack.EMPTY);
    }
    private static void emptyGrid(AbstractContainerMenu menu, int cells) {
        for (int i = 1; i <= cells; i++) check(menu.getSlot(i).getItem().isEmpty(), "Craft ingredient remained in slot " + i);
    }
    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.contains(":") ? id : "minecraft:" + id)));
    }
    private static void check(boolean success, String message) { if (!success) throw new IllegalStateException(message); }
}

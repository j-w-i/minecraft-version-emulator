/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.server;

import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Owned fixture actor only; actual menu methods, not block-opening/network or world-player acceptance. */
final class EarlyIndependentMenuVerification {
    private EarlyIndependentMenuVerification() { }
    static Map<String, Object> verify(ServerLevel level, ServerPlayer player, boolean early) {
        if (!Boolean.getBoolean("historical_experience.generationProbe")) throw new IllegalStateException("Probe only");
        int priorLevels = player.experienceLevel;
        try {
            player.getInventory().clearContent(); player.experienceLevel = 30;
            var anvil = new AnvilMenu(44, player.getInventory(), ContainerLevelAccess.create(level, player.blockPosition()));
            player.containerMenu = anvil;
            ItemStack stored = new ItemStack(Items.IRON_SWORD);
            stored.setDamageValue(100); stored.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved blade"));
            var tag = new net.minecraft.nbt.CompoundTag(); tag.putString("hex_probe", "preserve future stack components");
            stored.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            ItemStack before = stored.copy();
            anvil.getSlot(0).set(stored); anvil.setItemName("Hex anvil probe");
            if (early) {
                check(anvil.getSlot(2).getItem().isEmpty(), "Early anvil exposed a modern rename result without any recipe lookup");
                check(anvil.getCost() == 0, "Disabled anvil still reports an XP cost");
                deniedTake(player, anvil);
                check(anvil.getSlot(0).getItem() == stored && ItemStack.matches(stored, before)
                    && player.experienceLevel == 30, "Denied anvil changed stored name/damage/components/count or XP");
            } else {
                int cost = anvil.getCost();
                check(cost == 1 && anvil.getSlot(2).hasItem(), "Ordinary anvil lost its rename result or cost");
                anvil.clicked(2, 0, ClickType.PICKUP, player);
                ItemStack renamed = anvil.getCarried();
                check(renamed.is(Items.IRON_SWORD) && renamed.getCount() == 1 && renamed.getDamageValue() == 100
                    && renamed.get(DataComponents.CUSTOM_NAME).getString().equals("Hex anvil probe")
                    && renamed.get(DataComponents.CUSTOM_DATA).equals(before.get(DataComponents.CUSTOM_DATA))
                    && anvil.getSlot(0).getItem().isEmpty() && player.experienceLevel == 30 - cost,
                    "Ordinary anvil rename/consumption/components/XP changed");
            }
            anvil.setCarried(ItemStack.EMPTY); anvil.getSlot(0).set(ItemStack.EMPTY);
            var grind = new GrindstoneMenu(45, player.getInventory(), ContainerLevelAccess.create(level, player.blockPosition()));
            player.containerMenu = grind; player.getInventory().clearContent(); player.experienceLevel = 30;
            ItemStack first = new ItemStack(Items.IRON_SWORD), second = new ItemStack(Items.IRON_SWORD);
            first.setDamageValue(100); second.setDamageValue(125);
            ItemStack firstBefore = first.copy(), secondBefore = second.copy();
            grind.getSlot(0).set(first); grind.getSlot(1).set(second);
            if (early) {
                check(grind.getSlot(2).getItem().isEmpty(), "Early grindstone exposed a modern repair result without any recipe lookup");
                deniedTake(player, grind);
                check(grind.getSlot(0).getItem() == first && grind.getSlot(1).getItem() == second
                    && ItemStack.matches(first, firstBefore) && ItemStack.matches(second, secondBefore)
                    && player.experienceLevel == 30, "Denied grindstone changed stored stacks or XP");
            } else {
                check(grind.getSlot(2).hasItem(), "Ordinary grindstone lost its repair result");
                grind.clicked(2, 0, ClickType.PICKUP, player);
                check(grind.getCarried().is(Items.IRON_SWORD) && grind.getCarried().getCount() == 1
                    && grind.getCarried().getDamageValue() == 0 && grind.getSlot(0).getItem().isEmpty()
                    && grind.getSlot(1).getItem().isEmpty() && player.experienceLevel == 30,
                    "Ordinary grindstone repair/consumption changed");
            }
            grind.setCarried(ItemStack.EMPTY); grind.getSlot(0).set(ItemStack.EMPTY); grind.getSlot(1).set(ItemStack.EMPTY);
            return Map.of("anvilBlocked", early, "grindstoneBlocked", early,
                "ordinaryRenamesTaken", early ? 0 : 1, "ordinaryRepairsTaken", early ? 0 : 1,
                "storedComponentsAndLevelsChecked", true);
        } finally {
            player.experienceLevel = priorLevels; player.getInventory().clearContent(); player.containerMenu = player.inventoryMenu;
        }
    }
    private static void deniedTake(ServerPlayer player, AbstractContainerMenu menu) {
        menu.clicked(2, 0, ClickType.PICKUP, player); menu.clicked(2, 0, ClickType.QUICK_MOVE, player);
        check(menu.getSlot(2).getItem().isEmpty() && menu.getCarried().isEmpty() && player.getInventory().isEmpty(),
            "Denied independent workstation produced output");
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}

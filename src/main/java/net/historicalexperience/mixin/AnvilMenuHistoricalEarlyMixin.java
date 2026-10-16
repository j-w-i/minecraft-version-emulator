/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalEarlyMenuRules;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Independent pre-assembly guard. Preserved inputs stay stored; no recipe manager is involved in renaming/repair. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuHistoricalEarlyMixin {
    @Unique private Level historicalExperience$menuLevel;
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("RETURN"), require = 1)
    private void historicalExperience$bindLevel(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        historicalExperience$menuLevel = inventory.player.level();
    }
    @Inject(method = "createResult()V", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$denyEarlyResult(CallbackInfo ci) {
        Level level = historicalExperience$menuLevel;
        if (level == null) return; // Constructor's empty-input initialization, before our level binding.
        String profile = level instanceof ServerLevel server ? HistoricalProfileLookup.profileIdForLevel(server) : HistoricalActiveSession.current();
        if (!HistoricalEarlyMenuRules.blocksResult(profile, "anvil")) return;
        var menu = (AbstractContainerMenu) (Object) this;
        menu.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        cost.set(0); repairItemCountCost = 0;
        menu.broadcastChanges();
        ci.cancel();
    }
}

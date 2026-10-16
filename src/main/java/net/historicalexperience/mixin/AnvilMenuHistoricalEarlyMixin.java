/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.historicalexperience.server.HistoricalSnapshotRuntime;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
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

/** Pre-assembly guards and snapshot-scoped eligibility; our guards never rewrite stored input stacks. */
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
        var menu = (AbstractContainerMenu) (Object) this;
        String patch = level instanceof ServerLevel server ? HistoricalSnapshotRuntime.activePatchId(server) : HistoricalSnapshotRuntime.clientPatchId();
        boolean stackedBook = HistoricalCreativeRules.rejectsStackedBookTarget(patch,
            menu.getSlot(0).getItem().getCount(), menu.getSlot(1).getItem().is(Items.ENCHANTED_BOOK));
        if (!HistoricalEarlyMenuRules.blocksResult(profile, "anvil") && !stackedBook) return;
        menu.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        cost.set(0); repairItemCountCost = 0;
        menu.broadcastChanges();
        ci.cancel();
    }

    /** 12w49a changes target eligibility only; the native anvil still consumes the book and charges XP. */
    @WrapOperation(method = "createResult()V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"), require = 1)
    private boolean historicalExperience$snapshotBookTarget(Enchantment enchantment, ItemStack target, Operation<Boolean> original) {
        Level level = historicalExperience$menuLevel;
        if (level == null) return original.call(enchantment, target);
        String patch = level instanceof ServerLevel server ? HistoricalSnapshotRuntime.activePatchId(server) : HistoricalSnapshotRuntime.clientPatchId();
        var menu = (AbstractContainerMenu) (Object) this;
        if (HistoricalCreativeRules.allowsBookOnAnyItem(patch, menu.getSlot(0).getItem().getCount(), menu.getSlot(1).getItem().is(Items.ENCHANTED_BOOK))) return true;
        return original.call(enchantment, target);
    }
}

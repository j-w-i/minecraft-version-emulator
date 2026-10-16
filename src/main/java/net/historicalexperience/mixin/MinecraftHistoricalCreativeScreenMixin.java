/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin;

import net.historicalexperience.client.HistoricalCreativeInventory;
import net.historicalexperience.client.LegacyCreativeScreen;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftHistoricalCreativeScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 1)
    private Screen historicalExperience$oldCreative(Screen requested) {
        var client = (Minecraft) (Object) this;
        if (requested instanceof CreativeModeInventoryScreen && client.player != null && client.gameMode != null
                && client.gameMode.hasInfiniteItems() && HistoricalCreativeRules.usesOldMenu(HistoricalCreativeInventory.selection()))
            return new LegacyCreativeScreen(client.player, HistoricalCreativeInventory.selection());
        return requested;
    }
}

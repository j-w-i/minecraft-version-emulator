/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Inert accessor used only by explicitly armed native acceptance to exercise actual tab selection. */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeScreenProbeAccess {
    @Invoker("selectTab") void historicalExperience$selectTab(CreativeModeTab tab);
}

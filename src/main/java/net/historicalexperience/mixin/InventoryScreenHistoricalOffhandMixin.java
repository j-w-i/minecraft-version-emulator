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
package net.historicalexperience.mixin;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.historicalexperience.client.inventory.HistoricalInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the inventory's off-hand slot in versions that had none, by Nostalgic Tweaks' own method.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/candy/inventory_screen/InventoryScreenMixin.java}
 * (LGPL-3.0-only; see THIRD_PARTY_NOTICES.md). The two injections below are upstream's
 * {@code nt_inventory_screen$onConstruct} and {@code nt_inventory_screen$onFinishBackgroundRendering}, kept at the
 * same points (constructor {@code TAIL} and {@code renderBg} {@code TAIL}) and calling the same two upstream helpers
 * through {@link HistoricalInventoryScreen}:
 *
 * <pre>
 *   this.nt$offHand = InventoryScreenHelper.setPositionsForSlots(this.menu.slots, this.recipeBookComponent);
 *   InventoryScreenHelper.renderOffHandSlot(graphics, this.recipeBookComponent, this.nt$offHand, this.leftPos, this.height);
 * </pre>
 *
 * <p>What differs is only the gate and the helper's internals: upstream selects a shield POSITION from
 * {@code CandyTweak.INVENTORY_SHIELD} and this project asks whether the version had an off-hand at all, and the
 * helper carries the {@code INVISIBLE} branch alone. Upstream's other injections in that mixin (the crafting-grid
 * slot positions, the recipe button, the label position and the {@code hasClickedOutside} patch for its bottom-left
 * mode) belong to its full old-inventory layout and are not part of hiding the off-hand slot, so they are not ported
 * here; see the helper's own note.
 *
 * <h2>Why this replaces the earlier in-house attempt</h2>
 *
 * <p>The first attempt removed the slot from {@code InventoryMenu} and broke joining with a "Network Protocol Error":
 * a menu's slot list is protocol. The second moved the slot off-screen, which hides the slot but not the box the game
 * paints into the inventory background - which is what the owner could still see. Both lessons are recorded in
 * {@link HistoricalInventoryScreen}.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenHistoricalOffhandMixin extends AbstractContainerScreen<InventoryMenu> {
    /** Upstream keeps the slot so a later mode can move it again; kept for the same reason. */
    @Unique
    @Nullable
    private Slot historicalExperience$offHand;

    /** Upstream's shape for the same injection point, which needs the superclass constructor. */
    private InventoryScreenHistoricalOffhandMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** Upstream: {@code nt_inventory_screen$onConstruct}. The menu's slot list is complete by the constructor's end. */
    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void historicalExperience$onConstruct(Player player, CallbackInfo callback) {
        if (!historicalExperience$noOffHandInThisVersion()) {
            return;
        }
        this.historicalExperience$offHand = HistoricalInventoryScreen.hideOffHandSlot(this.menu.slots);
    }

    /** Upstream: {@code nt_inventory_screen$onFinishBackgroundRendering}. Paints over the box the background has. */
    @Inject(method = "renderBg", at = @At("TAIL"), require = 1)
    private void historicalExperience$paintOverTheEmptySlot(GuiGraphics graphics, float partialTick, int mouseX,
            int mouseY, CallbackInfo callback) {
        if (!historicalExperience$noOffHandInThisVersion()) {
            return;
        }
        HistoricalInventoryScreen.paintOverOffHandSlot(graphics, this.leftPos, this.height);
    }

    /** The version gate that upstream expresses as a shield-position setting. */
    private static boolean historicalExperience$noOffHandInThisVersion() {
        return Features.active(Feature.NO_OFFHAND);
    }
}

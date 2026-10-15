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
package net.historicalexperience.client.inventory;

import net.historicalexperience.HistoricalExperience;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Draws and positions the off-hand slot for a version that had none, the way Nostalgic Tweaks does it.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/candy/screen/inventory/InventoryScreenHelper.java}
 * (LGPL-3.0-only; see THIRD_PARTY_NOTICES.md), specifically its {@code setPositionsForSlots} and
 * {@code renderOffHandSlot} methods and the {@code InventoryShield.INVISIBLE} branch they serve.
 *
 * <h2>What the two halves are for, and why moving the slot alone is not enough</h2>
 *
 * <p>{@link #hideOffHandSlot} moves the slot to upstream's own off-screen coordinate ({@code -9999}), so no item is
 * drawn in it, no mouse position can be over it and it cannot be clicked. That part is upstream's; the earlier
 * in-house attempt here did the same thing and the owner still saw an off-hand slot, for a reason that is visible in
 * the game's own art: <b>the box is painted into the inventory background texture</b>
 * ({@code assets/minecraft/textures/gui/container/inventory.png}), which the screen blits before any slot is drawn.
 * Moving the slot leaves the empty box behind.
 *
 * <p>Upstream answers that by painting over it: {@link #paintOverOffHandSlot} blits an 18x18 patch from the mod's own
 * inventory texture - taken from a part of it that is plain background - exactly where the box is. The screen height
 * is used to place it because the inventory GUI is vertically centred, which is upstream's own arithmetic
 * ({@code height / 2 - 22} is the slot's row on screen); the x is {@code leftPos + 76} for the same reason.
 *
 * <p>Upstream's other shield positions (middle-right, bottom-left, with their own slot art and their
 * {@code hasClickedOutside} patch) are NOT ported: they exist to make the off-hand slot reachable somewhere else,
 * and a version with no off-hand has no slot to reach.
 */
public final class HistoricalInventoryScreen {
    /** Upstream's own off-screen coordinate: far enough that no mouse position can ever be over the slot. */
    private static final int UNREACHABLE = -9999;

    /**
     * Upstream's inventory texture, copied into this project byte for byte
     * ({@code assets/nostalgic_tweaks/textures/gui/inventory.png}); its provenance and licence are recorded in
     * THIRD_PARTY_NOTICES.md and docs/PROVENANCE.md.
     */
    private static final ResourceLocation INVENTORY =
        ResourceLocation.fromNamespaceAndPath(HistoricalExperience.MOD_ID, "textures/gui/inventory.png");

    private HistoricalInventoryScreen() {
    }

    /**
     * Moves the off-hand slot out of the screen and returns it, or null when the menu has no such slot.
     *
     * <p>Upstream returns the slot as well, because its other modes later move it again from
     * {@code renderOffHandSlot}; the reference is kept for that reason and for a future mode that needs it.
     */
    @Nullable
    public static Slot hideOffHandSlot(NonNullList<Slot> slots) {
        Slot offHand = null;
        for (Slot slot : slots) {
            if (SlotTracker.OFF_HAND.isEqualTo(slot)) {
                offHand = slot;
                // Upstream: `case INVISIBLE -> SlotTracker.OFF_HAND.move(slot, -9999, -9999);`
                SlotTracker.OFF_HAND.move(slot, UNREACHABLE, UNREACHABLE);
            }
        }
        return offHand;
    }

    /**
     * Paints the inventory's own background over the off-hand slot's box.
     *
     * <p>Upstream's line, with its own coordinates: the patch is at 178,74 in the texture and the target is where the
     * game drew the box. Only reached when the version has no off-hand, so on every later version the vanilla box is
     * left exactly as it is.
     */
    public static void paintOverOffHandSlot(GuiGraphics graphics, int leftPos, int height) {
        graphics.blit(INVENTORY, leftPos + 76, height / 2 - 22, 178, 74, 18, 18);
    }
}

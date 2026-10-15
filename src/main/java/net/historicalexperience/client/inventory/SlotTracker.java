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

import net.minecraft.world.inventory.Slot;

/**
 * Recognises an inventory screen's slot by <b>position</b>, the way Nostalgic Tweaks does.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/candy/screen/inventory/SlotTracker.java}
 * (LGPL-3.0-only; see THIRD_PARTY_NOTICES.md), trimmed to the one tracker this project needs. Upstream also tracks the
 * four crafting-grid slots and the result slot, because its "old inventory" layout moves those as well; this project
 * does not move them, so only {@link #OFF_HAND} is carried and the rest are recorded here as not ported rather than
 * silently dropped.
 *
 * <h2>Why it identifies a slot by where it is</h2>
 *
 * <p>This is upstream's own idea and the reason the port is worth having. An index into the menu's slot list, or a
 * guess from the slot's container, is an assumption about a layout that is built independently on the client and the
 * server; the off-hand slot's position is the one thing both sides agree on, and it is what the player sees. The
 * position is also what makes {@link #move} idempotent: the tracker accepts a slot that is either where the screen
 * put it or where this tracker last moved it, so moving it twice cannot lose it.
 */
public enum SlotTracker {
    /** The player inventory's off-hand ("shield") slot, at 77,62 in the inventory screen's own coordinates. */
    OFF_HAND(77, 62);

    private final int originalX;
    private final int originalY;
    private int x;
    private int y;

    SlotTracker(int x, int y) {
        this.originalX = x;
        this.originalY = y;
        this.x = x;
        this.y = y;
    }

    /**
     * Upstream's test: whether this tracker describes the given slot, either at its original position or at the
     * position this tracker last moved it to.
     */
    public boolean isEqualTo(Slot slot) {
        return (slot.x == this.x && slot.y == this.y) || (slot.x == this.originalX && slot.y == this.originalY);
    }

    /** Moves the slot back to where the screen put it. Upstream: {@code reset}. */
    public void reset(Slot slot) {
        this.move(slot, this.originalX, this.originalY);
    }

    /**
     * Moves the slot, recording the position so {@link #isEqualTo} keeps recognising it.
     *
     * <p>{@code slot.x} and {@code slot.y} are written directly, exactly as upstream writes them; they are plain
     * fields in 1.21.1 and no duck interface or accessor is needed for this.
     */
    public void move(Slot slot, int x, int y) {
        if (this.isEqualTo(slot)) {
            this.x = x;
            this.y = y;
            slot.x = x;
            slot.y = y;
        }
    }
}

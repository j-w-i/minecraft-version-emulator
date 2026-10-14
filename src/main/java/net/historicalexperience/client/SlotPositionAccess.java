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
package net.historicalexperience.client;

/**
 * Lets this project move a slot, whose coordinates are final in 1.21.1.
 *
 * <p><b>Ported from Nostalgic Tweaks</b>, package {@code mod.adrenix.nostalgic.mixin.duck} - upstream solves the same
 * problem with duck interfaces plus accessor mixins rather than writing fields directly. Its
 * {@code InventoryScreenHelper.move} writes {@code slot.x = x} inline, which compiles against the version upstream
 * targets and does not against 1.21.1, where {@code Slot.x} and {@code Slot.y} are final.
 *
 * <p>The upstream files that do this work are mirrored here: {@code mixin/duck/SlotTracker.java} for the interface
 * convention, {@code mixin/access/AbstractContainerScreenAccess.java} and its siblings for the accessor convention.
 * Implemented by {@link SlotPositionMixin}.
 */
public interface SlotPositionAccess {
    /** Move this slot. Used to place one off-screen so it is neither drawn nor reachable. */
    void historicalExperience$setSlotPosition(int x, int y);
}

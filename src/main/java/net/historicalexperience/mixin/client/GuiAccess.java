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
package net.historicalexperience.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PORTED from Nostalgic Tweaks {@code mixin/access/GuiAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 *
 * <p>Exposes the vanilla armour and air sprites, which the old heads-up display redraws in the positions the
 * hunger bar used to occupy, plus {@code displayHealth}, which the heart-row height calculation reads.
 *
 * <p>These being vanilla fields matters: the old HUD is Minecraft's own icons MOVED, not third-party artwork,
 * so no textures travel with this port.
 */
@Mixin(Gui.class)
public interface GuiAccess {
    @Accessor("ARMOR_FULL_SPRITE")
    static ResourceLocation armorFullSprite() {
        throw new AssertionError();
    }

    @Accessor("ARMOR_HALF_SPRITE")
    static ResourceLocation armorHalfSprite() {
        throw new AssertionError();
    }

    @Accessor("ARMOR_EMPTY_SPRITE")
    static ResourceLocation armorEmptySprite() {
        throw new AssertionError();
    }

    @Accessor("AIR_SPRITE")
    static ResourceLocation airSprite() {
        throw new AssertionError();
    }

    @Accessor("AIR_BURSTING_SPRITE")
    static ResourceLocation airBurstingSprite() {
        throw new AssertionError();
    }

    @Accessor("displayHealth")
    int displayHealth();
}

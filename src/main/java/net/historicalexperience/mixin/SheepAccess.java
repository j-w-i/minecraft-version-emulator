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

import java.util.Map;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code Sheep.ITEM_BY_DYE}, the dye-to-wool map.
 *
 * <p>PORTED from Nostalgic Tweaks {@code mixin/access/SheepAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 * Old sheep drops give the wool of the sheep's own colour rather than white wool, which is what this map is for.
 *
 * <p>Verified against the 1.21.1 jar before writing: the field is
 * {@code private static final Map<DyeColor, ItemLike> ITEM_BY_DYE}. A wrong field name here is a fatal mixin
 * error at start-up, not a warning.
 */
@Mixin(Sheep.class)
public interface SheepAccess {
    @Accessor("ITEM_BY_DYE")
    static Map<DyeColor, ItemLike> itemByDye() {
        throw new AssertionError();
    }
}

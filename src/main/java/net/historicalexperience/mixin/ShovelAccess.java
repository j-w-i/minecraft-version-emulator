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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ShovelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ShovelItem.FLATTENABLES}, so "can this shovel make a path here" is answered by the game's own table.
 *
 * <p>PORTED from Nostalgic Tweaks {@code mixin/access/ShovelItemAccess.java} (LGPL-3.0; see THIRD_PARTY_NOTICES.md).
 * The same reasoning as {@link AxeAccess}: the game's table is the authority, and a copied list would drift.
 *
 * <p>Verified against the 1.21.1 jar before writing: the field is
 * {@code protected static final Map<Block, BlockState> FLATTENABLES}.
 */
@Mixin(ShovelItem.class)
public interface ShovelAccess {
    @Accessor("FLATTENABLES")
    static Map<Block, BlockState> flattenables() {
        throw new AssertionError();
    }
}

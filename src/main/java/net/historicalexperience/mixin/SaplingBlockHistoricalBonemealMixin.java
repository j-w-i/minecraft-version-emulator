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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.level.block.SaplingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * One bone meal grows a sapling into a tree, before Release 1.5. Ported from Nostalgic Tweaks
 * {@code SaplingBlockMixin} (LGPL-3.0); the boundary and its source are on {@link CropBlockHistoricalBonemealMixin}.
 *
 * <p>Two injections, because modern saplings do two separate things upstream's port only half-covers:
 *
 * <ul>
 *   <li>{@code advanceTree} normally cycles a stage-0 sapling to stage 1 and only grows the tree from stage 1, so two
 *       meals are needed. Reporting stage 4 skips straight to the tree.</li>
 *   <li>{@code isBonemealSuccess} is a 45% roll. Before the nerf bone meal on a sapling simply worked, so the roll is
 *       removed too - otherwise "one use grows it" would still fail more often than not. Upstream leaves the roll in
 *       place; this is a deliberate difference, made because the wiki's description of the older behaviour is
 *       unconditional.</li>
 * </ul>
 */
@Mixin(SaplingBlock.class)
public abstract class SaplingBlockHistoricalBonemealMixin {
    @ModifyExpressionValue(
        method = "advanceTree",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getValue("
                + "Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
    private Comparable<Integer> historicalExperience$skipToTree(Comparable<Integer> stage) {
        return HistoricalGameplayTweaks.instantBonemeal() ? 4 : stage;
    }

    @ModifyReturnValue(method = "isBonemealSuccess", at = @At("RETURN"))
    private boolean historicalExperience$noSaplingRoll(boolean success) {
        return HistoricalGameplayTweaks.instantBonemeal() || success;
    }
}

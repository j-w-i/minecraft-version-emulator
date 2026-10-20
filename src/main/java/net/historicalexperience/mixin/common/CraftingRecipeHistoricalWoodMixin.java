/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalCraftingEra;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes a pre-Release-1.2 world's per-species planks recipe produce the one planks item that version had.
 *
 * <h2>The report this answers</h2>
 *
 * <blockquote>"Birch trees are available in beta 1.2 but I can't craft anything with the logs and they dont go into
 * planks. Until Release 1.2 they were just wooden planks but in 1.2 they start crafting into birch planks."</blockquote>
 *
 * <p>The crafting gate already refused {@code birch_planks}, {@code spruce_planks} and {@code jungle_planks} before
 * Release 1.2, because those items did not exist - and refusing them left the recipes for turning those logs into
 * planks with nothing in their place, so a birch log crafted into nothing at all. What the version actually had was
 * one planks item, so the recipe is now allowed through and its result is rewritten.
 *
 * <h2>Where the substitution happens</h2>
 *
 * <p>{@code CraftingRecipe.assemble} is the one place a recipe turns its inputs into the stack the player receives:
 * {@code CraftingMenu.slotChangedCraftingGrid} calls it and puts the result in the result slot, and the recipe book
 * asks the same method for its preview. {@code CraftingRecipe} is an interface, so the injection targets the two
 * implementations that can produce planks.
 *
 * <p>Only the item changes, and only for a result this version never had; the count and the component patch are the
 * recipe's own. An ordinary world has no active session and is untouched.
 */
@Mixin({ShapedRecipe.class, ShapelessRecipe.class})
public abstract class CraftingRecipeHistoricalWoodMixin {
    @ModifyReturnValue(method = "assemble", at = @At("RETURN"), require = 1)
    private ItemStack historicalExperience$substituteTheWood(ItemStack assembled) {
        String versionId = ActiveVersion.current();
        if (versionId == null || assembled.isEmpty()) {
            return assembled;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(assembled.getItem()).toString();
        String replacement = HistoricalCraftingEra.substitutedResult(versionId, itemId);
        if (replacement == null) {
            return assembled;
        }
        var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(replacement));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            // A build that cannot resolve the substitute must not lose the player's craft.
            return assembled;
        }
        ItemStack substituted = new ItemStack(item, assembled.getCount());
        substituted.applyComponents(assembled.getComponentsPatch());
        return substituted;
    }
}

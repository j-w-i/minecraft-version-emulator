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
package net.historicalexperience.verification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** Opt-in isolated-fixture assertions through the MIXED-IN vanilla recipe manager, not just the pure data matcher. */
final class EarlyGameplayVerification {
    private EarlyGameplayVerification() { }
    static Map<String, Object> verify(ServerLevel level) {
        String profile = HistoricalProfileLookup.profileIdForLevel(level);
        var report = new LinkedHashMap<String, Object>();
        report.put("profile", profile);
        var manager = level.getRecipeManager();
        if (!EarlyGameplayRecipes.supports(profile)) {
            if (profile == null) {
                var ladder = manager.getRecipeFor(RecipeType.CRAFTING, crafting(3, 3,
                    "stick", "", "stick", "stick", "stick", "stick", "stick", "", "stick"), level).orElseThrow();
                if (ladder.value().assemble(crafting(3, 3,
                        "stick", "", "stick", "stick", "stick", "stick", "stick", "", "stick"), level.registryAccess()).getCount() != 3) {
                    throw new IllegalStateException("Ordinary crafting was changed by historical rules");
                }
                report.put("leafDrops", EarlyLeafDropVerification.verify(level, profile));
                report.put("ordinaryLadderCount", 3);
                report.put("playerPaths", EarlyPlayerGameplayVerification.verify(level, profile));
            }
            return report;
        }
        int checked = 0;
        RecipeHolder<CraftingRecipe> previous = null;
        for (var row : EarlyGameplayRecipes.recipes(profile)) {
            if (row.dormant()) continue;
            List<ItemStack> stacks = row.ingredients().stream().map(EarlyGameplayVerification::stack).toList();
            var input = CraftingInput.of(row.width(), row.height(), stacks);
            var match = manager.getRecipeFor(RecipeType.CRAFTING, input, level, previous).orElseThrow(
                () -> new IllegalStateException("No live early recipe for " + row.result()));
            ItemStack output = match.value().assemble(input, level.registryAccess());
            if (!BuiltInRegistries.ITEM.getKey(output.getItem()).toString().equals(row.result()) || output.getCount() != row.count()) {
                throw new IllegalStateException("Wrong live recipe result: " + row.result() + " expected " + row.count() + " got " + output);
            }
            var listed = manager.getRecipesFor(RecipeType.CRAFTING, input, level);
            if (listed.size() != 1 || !listed.getFirst().id().equals(match.id())) {
                throw new IllegalStateException("List lookup differs from exact early craft: " + row.result());
            }
            listed.clear(); // Vanilla menus mutate returned lists; must not throw or alter a cached result list.
            if (manager.getRecipesFor(RecipeType.CRAFTING, input, level).size() != 1) throw new IllegalStateException("Craft list shared mutable state");
            previous = match;
            checked++;
        }
        int forbidden = 0;
        for (CraftingInput input : List.of(
                crafting(3, 1, "sugar_cane", "sugar_cane", "sugar_cane"),
                crafting(3, 2, "iron_ingot", "", "iron_ingot", "", "iron_ingot", ""),
                crafting(1, 3, "flint", "stick", "feather"),
                crafting(2, 2, "string", "string", "string", "string"),
                crafting(3, 3, "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot", "fire", "iron_ingot", "iron_ingot", "iron_ingot", "iron_ingot"))) {
            if (manager.getRecipeFor(RecipeType.CRAFTING, input, level, previous).isPresent()
                    || !manager.getRecipesFor(RecipeType.CRAFTING, input, level).isEmpty()) {
                throw new IllegalStateException("Modern/dormant recipe escaped early manager gate");
            }
            forbidden++;
        }
        for (var smelt : EarlyGameplayRecipes.smelting(profile)) {
            var recipe = manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack(smelt.input())), level).orElseThrow();
            if (!BuiltInRegistries.ITEM.getKey(recipe.value().getResultItem(level.registryAccess()).getItem())
                    .toString().equals(smelt.result())) throw new IllegalStateException("Wrong early smelt " + smelt.input());
            var listed = manager.getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(stack(smelt.input())), level);
            if (listed.size() != 1 || listed.getFirst().value().getResultItem(level.registryAccess()).getCount() != 1
                    || !listed.getFirst().id().equals(recipe.id())) throw new IllegalStateException("Wrong listed early smelt " + smelt.input());
            listed.clear();
            if (manager.getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(stack(smelt.input())), level).size() != 1) {
                throw new IllegalStateException("Smelt list shared mutable state");
            }
        }
        for (String input : List.of("oak_log", "raw_iron", "beef", "stone", "copper_ore", "kelp")) {
            if (manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack(input)), level).isPresent()
                    || !manager.getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(stack(input)), level).isEmpty()) {
                throw new IllegalStateException("Modern furnace recipe escaped early gate: " + input);
            }
        }
        if (manager.getRecipeFor(RecipeType.SMOKING, new SingleRecipeInput(stack("porkchop")), level).isPresent()
                || manager.getRecipeFor(RecipeType.BLASTING, new SingleRecipeInput(stack("iron_ore")), level).isPresent()
                || manager.getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack("porkchop")), level).isPresent()
                || manager.getRecipeFor(RecipeType.STONECUTTING, new SingleRecipeInput(stack("cobblestone")), level).isPresent()) {
            throw new IllegalStateException("Modern workstation recipe escaped early gate");
        }
        if (!manager.getRecipesFor(RecipeType.SMOKING, new SingleRecipeInput(stack("porkchop")), level).isEmpty()
                || !manager.getRecipesFor(RecipeType.BLASTING, new SingleRecipeInput(stack("iron_ore")), level).isEmpty()
                || !manager.getRecipesFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack("porkchop")), level).isEmpty()
                || !manager.getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(stack("cobblestone")), level).isEmpty()) {
            throw new IllegalStateException("Modern workstation escaped recipe LIST gate");
        }
        var arrowId = ResourceLocation.withDefaultNamespace("arrow");
        if (manager.byKey(arrowId).isEmpty() || manager.getAllRecipesFor(RecipeType.STONECUTTING).isEmpty()) {
            throw new IllegalStateException("Shared no-level registry was incorrectly filtered");
        }
        if (manager.getRecipeFor(RecipeType.CRAFTING, crafting(1, 3, "flint", "stick", "feather"), level, arrowId).isPresent()) {
            throw new IllegalStateException("Known-ID overload bypassed early crafting");
        }
        var oversized = new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("historical_experience", "probe/oversized_smelt"),
            new net.minecraft.world.item.crafting.SmeltingRecipe("", net.minecraft.world.item.crafting.CookingBookCategory.MISC,
                net.minecraft.world.item.crafting.Ingredient.of(stack("iron_ore")), new ItemStack(stack("iron_ingot").getItem(), 2), 0.0F, 200));
        if (manager.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack("iron_ore")), level, oversized).isPresent()) {
            throw new IllegalStateException("Cached smelt with an oversized output bypassed early gate");
        }
        var cache = net.minecraft.world.item.crafting.RecipeManager.createCheck(RecipeType.SMELTING);
        if (cache.getRecipeFor(new SingleRecipeInput(stack("iron_ore")), level).isEmpty()
                || cache.getRecipeFor(new SingleRecipeInput(stack("raw_iron")), level).isPresent()
                || cache.getRecipeFor(new SingleRecipeInput(stack("iron_ore")), level).isEmpty()) {
            throw new IllegalStateException("Cached furnace lookup bypassed early gate");
        }
        report.put("listMutabilityAndIsolationChecked", true);
        report.put("listLookupCrafts", checked);
        report.put("listLookupSmelts", 6);
        report.put("cachedSmeltAndKnownIdChecked", true);
        report.put("sharedRegistryUnchanged", true);
        report.put("liveCrafts", checked);
        report.put("deniedCraftPatterns", forbidden);
        report.put("smelts", 6);
        report.put("deniedSmelts", 6);
        report.put("deniedWorkstations", List.of("smoker", "blast_furnace", "campfire", "stonecutter"));
        report.put("previousHolderRechecked", true);
        report.put("leafDrops", EarlyLeafDropVerification.verify(level, profile));
        report.put("playerPaths", EarlyPlayerGameplayVerification.verify(level, profile));
        return report;
    }
    private static CraftingInput crafting(int width, int height, String... items) {
        var stacks = new ArrayList<ItemStack>();
        for (String item : items) stacks.add(stack(item));
        return CraftingInput.of(width, height, stacks);
    }
    private static ItemStack stack(String id) {
        if (id.isEmpty() || id.equals("fire")) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.contains(":") ? id : "minecraft:" + id)));
    }
}

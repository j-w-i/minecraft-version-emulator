/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.gameplay;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** Creates ordinary shaped recipe values at lookup time; the global recipe manager and registries stay intact. */
public final class EarlyRecipeLookup {
    private static final Map<String, List<RecipeHolder<CraftingRecipe>>> CACHE = new ConcurrentHashMap<>();
    private EarlyRecipeLookup() { }

    public static Optional<RecipeHolder<CraftingRecipe>> find(String profile, CraftingInput input,
            net.minecraft.world.level.Level level) {
        if (!EarlyGameplayRecipes.supports(profile)) return Optional.empty();
        return CACHE.computeIfAbsent(profile, EarlyRecipeLookup::build).stream()
            .filter(holder -> holder.value().matches(input, level)).findFirst();
    }

    private static List<RecipeHolder<CraftingRecipe>> build(String profile) {
        var source = EarlyGameplayRecipes.recipes(profile);
        var out = new java.util.ArrayList<RecipeHolder<CraftingRecipe>>();
        for (int i = 0; i < source.size(); i++) {
            var recipe = source.get(i);
            if (recipe.dormant()) continue;
            var ingredients = NonNullList.<Ingredient>create();
            for (String id : recipe.ingredients()) {
                ingredients.add(id.isEmpty() ? Ingredient.EMPTY : Ingredient.of(item(id)));
            }
            var pattern = new ShapedRecipePattern(recipe.width(), recipe.height(), ingredients, Optional.empty());
            var value = new ShapedRecipe("", CraftingBookCategory.MISC, pattern,
                new ItemStack(item(recipe.result()), recipe.count()), false) {
                    // These per-level values are NOT in the global, client-synchronized recipe registry.
                    // Vanilla must not award/send their IDs or require a book unlock for limited crafting.
                    @Override public boolean isSpecial() { return true; }
                };
            out.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("historical_experience",
                "early/" + profile.substring(profile.indexOf(':') + 1) + "/" + i), value));
        }
        return List.copyOf(out);
    }

    private static net.minecraft.world.item.Item item(String id) {
        var key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalStateException("Unknown early item " + id);
        return BuiltInRegistries.ITEM.get(key);
    }
}

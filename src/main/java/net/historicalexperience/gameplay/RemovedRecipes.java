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
package net.historicalexperience.gameplay;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

/**
 * Recipes a declared version had that modern Minecraft <b>removed</b>.
 *
 * <h2>Why this exists, and how it differs from the other two crafting paths</h2>
 *
 * <p>Vanilla's recipe registry can only be <em>filtered</em>: {@link HistoricalCraftingEra} refuses a recipe a version
 * never had, and {@link EarlyGameplayRecipes} reproduces the exact per-build recipes of the Indev and Infdev clients
 * from extracted artifacts. Neither can supply a recipe the modern game no longer contains at all, and that is a real
 * category: Minecraft removes recipes, and a version before the removal still had them.
 *
 * <p>The first member is the one the owner reported: <b>the enchanted golden apple</b>. minecraft.wiki, Enchanted
 * Golden Apple, Java Edition history: added in <b>1.3.1 (12w21a)</b> as a second golden-apple tier crafted from eight
 * blocks of gold and one apple; made <b>uncraftable in 1.9 (15w44a)</b>, when the recipe was removed and the item moved
 * into chest loot. So a Release 1.3 to 1.8 world should be able to craft it, and 1.21.1 has no recipe to allow. The
 * project deliberately does not patch the global recipe registry - a world's recipes are shared, synchronized state -
 * so the recipe is assembled at lookup time, exactly as {@link EarlyRecipeLookup} does for its own era.
 *
 * <h2>Scope</h2>
 *
 * <p>A recipe is supplied only while {@link Feature#LEGACY_ENCHANTED_GOLDEN_APPLE} is active for the world's version,
 * which is {@code [Release 1.3, Release 1.9)}. An ordinary world, an earlier version and an unknown id get nothing, so
 * vanilla behaviour is unchanged for them.
 */
public final class RemovedRecipes {
    /** Shapes assembled at lookup time, cached per version id. */
    private static final Map<String, List<RecipeHolder<CraftingRecipe>>> CACHE = new ConcurrentHashMap<>();

    private RemovedRecipes() {
    }

    /**
     * One removed recipe, as data, so a test can assert the shape without building a {@code CraftingInput}
     * (which needs Minecraft's registries bootstrapped).
     *
     * @param id          the recipe's own path, for reports
     * @param width       pattern width
     * @param height      pattern height
     * @param ingredients row-major; empty string is an empty slot
     * @param result      the item produced
     * @param count       how many
     * @param window      the feature whose window decides whether this version had the recipe; one entry per recipe
     *                    rather than one for the class, because the golden apple has two historical shapes and the
     *                    enchanted one a third
     */
    public record Removed(String id, int width, int height, List<String> ingredients, String result, int count,
            Feature window, boolean refuseModernRecipe) {
        public Removed {
            ingredients = List.copyOf(ingredients);
            if (ingredients.size() != width * height) {
                throw new IllegalArgumentException(id + " declares " + ingredients.size()
                    + " ingredients for a " + width + "x" + height + " pattern");
            }
        }
    }

    /**
     * The enchanted golden apple, crafted from eight gold blocks around one apple.
     *
     * <p>The pattern is the one the wiki's history row shows for 1.3.1:
     * <pre>
     *   G G G
     *   G A G
     *   G G G
     * </pre>
     * with {@code G} a block of gold and {@code A} an apple.
     */
    public static final Removed ENCHANTED_GOLDEN_APPLE = new Removed(
        "enchanted_golden_apple",
        3, 3,
        List.of(
            "minecraft:gold_block", "minecraft:gold_block", "minecraft:gold_block",
            "minecraft:gold_block", "minecraft:apple", "minecraft:gold_block",
            "minecraft:gold_block", "minecraft:gold_block", "minecraft:gold_block"),
        "minecraft:enchanted_golden_apple", 1, Feature.LEGACY_ENCHANTED_GOLDEN_APPLE, false);

    /** The golden apple's pre-Release-1.1 shape: eight blocks of gold around an apple. */
    public static final Removed GOLDEN_APPLE_GOLD_BLOCK = new Removed(
        "golden_apple_from_gold_block",
        3, 3,
        List.of(
            "minecraft:gold_block", "minecraft:gold_block", "minecraft:gold_block",
            "minecraft:gold_block", "minecraft:apple", "minecraft:gold_block",
            "minecraft:gold_block", "minecraft:gold_block", "minecraft:gold_block"),
        "minecraft:golden_apple", 1, Feature.GOLDEN_APPLE_GOLD_BLOCK_RECIPE, true);

    /** The golden apple's Release 1.1 through 1.8 shape: eight gold nuggets around an apple. */
    public static final Removed GOLDEN_APPLE_GOLD_NUGGET = new Removed(
        "golden_apple_from_gold_nugget",
        3, 3,
        List.of(
            "minecraft:gold_nugget", "minecraft:gold_nugget", "minecraft:gold_nugget",
            "minecraft:gold_nugget", "minecraft:apple", "minecraft:gold_nugget",
            "minecraft:gold_nugget", "minecraft:gold_nugget", "minecraft:gold_nugget"),
        "minecraft:golden_apple", 1, Feature.GOLDEN_APPLE_GOLD_NUGGET_RECIPE, true);

    /** Every removed recipe, for tests and reports. */
    public static List<Removed> all() {
        return List.of(ENCHANTED_GOLDEN_APPLE, GOLDEN_APPLE_GOLD_BLOCK, GOLDEN_APPLE_GOLD_NUGGET);
    }

    /** Whether this version had any removed recipe at all. */
    public static boolean appliesTo(String versionId) {
        for (Removed recipe : all()) {
            if (Features.active(recipe.window(), versionId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when this version's own recipe for {@code itemId} is one the modern registry no longer has, so vanilla's
     * recipe for the same result must be refused rather than left as a second, anachronistic way to make it.
     */
    public static boolean replacesModernRecipe(String versionId, String itemId) {
        if (itemId == null) {
            return false;
        }
        for (Removed recipe : all()) {
            if (recipe.refuseModernRecipe() && recipe.result().equals(itemId)
                    && Features.active(recipe.window(), versionId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The removed recipe matching this input in this version, if there is one.
     *
     * <p>Returns empty rather than guessing: a version without the feature, or an input that does not match, simply
     * falls through to the vanilla lookup.
     */
    public static Optional<RecipeHolder<CraftingRecipe>> find(String versionId, CraftingInput input, Level level) {
        if (versionId == null || !appliesTo(versionId) || input == null || level == null) {
            return Optional.empty();
        }
        return CACHE.computeIfAbsent(versionId, RemovedRecipes::build).stream()
            .filter(holder -> holder.value().matches(input, level))
            .findFirst();
    }

    private static List<RecipeHolder<CraftingRecipe>> build(String versionId) {
        String path = versionId.substring(versionId.indexOf(':') + 1);
        var out = new java.util.ArrayList<RecipeHolder<CraftingRecipe>>();
        for (Removed recipe : all()) {
            if (!Features.active(recipe.window(), versionId)) {
                continue;
            }
            var ingredients = NonNullList.<Ingredient>create();
            for (String id : recipe.ingredients()) {
                ingredients.add(id.isEmpty() ? Ingredient.EMPTY : Ingredient.of(item(id)));
            }
            var pattern = new ShapedRecipePattern(recipe.width(), recipe.height(), ingredients, Optional.empty());
            var value = new ShapedRecipe("", CraftingBookCategory.MISC, pattern,
                new ItemStack(item(recipe.result()), recipe.count()), false) {
                // Per-level values are NOT in the global, client-synchronized recipe registry: vanilla must not award
                // or send their ids, and they must not need a recipe-book unlock.
                @Override
                public boolean isSpecial() {
                    return true;
                }
            };
            out.add(new RecipeHolder<>(
                ResourceLocation.fromNamespaceAndPath("historical_experience",
                    "removed/" + path + "/" + recipe.id()),
                value));
        }
        return List.copyOf(out);
    }

    private static Item item(String id) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            throw new IllegalStateException("Unknown item in a removed recipe: " + id);
        }
        return BuiltInRegistries.ITEM.get(key);
    }
}

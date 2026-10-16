/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Minecraft Historical Experience — independent implementation from extracted factual tables.
 */
package net.historicalexperience.gameplay;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Exact, build-local crafting inputs AND output counts; never inferred from a later era's result list.
 * Evidence and regeneration command: scripts/extract-early-gameplay.py, scripts/EarlyGameplayExtractor.java.
 * Four fire-based chainmail recipes are recorded but dormant, as in the original clients.
 */
public final class EarlyGameplayRecipes {
    public record Recipe(int width, int height, List<String> ingredients, String result, int count, boolean dormant) {
        public Recipe {
            ingredients = List.copyOf(ingredients);
            if (width < 1 || width > 3 || height < 1 || height > 3 || ingredients.size() != width * height
                    || result == null || result.isBlank() || count < 1 || count > 64) {
                throw new IllegalArgumentException("Invalid early recipe");
            }
        }

        /** Includes empty slots outside the pattern and reflection, not rotation or shapeless substitution. */
        public boolean matches(int gridWidth, int gridHeight, List<String> grid) {
            if (dormant || grid.size() != gridWidth * gridHeight) return false;
            for (int dz = 0; dz <= gridHeight - height; dz++) {
                for (int dx = 0; dx <= gridWidth - width; dx++) {
                    for (boolean mirror : new boolean[] {false, true}) {
                        boolean matches = true;
                        for (int z = 0; z < gridHeight; z++) {
                            for (int x = 0; x < gridWidth; x++) {
                                int px = x - dx, pz = z - dz;
                                String expected = px < 0 || pz < 0 || px >= width || pz >= height ? ""
                                    : ingredients.get(pz * width + (mirror ? width - px - 1 : px));
                                if (!Objects.equals(expected, grid.get(z * gridWidth + x))) matches = false;
                            }
                        }
                        if (matches) return true;
                    }
                }
            }
            return false;
        }
    }

    public record Smelt(String input, String result) { }
    /** Factual quantity/item callbacks; explosion survival/decay timing are separate engine behavior. */
    public record LeafDrop(String block, String result, int count, int chanceDenominator, int successRoll) {
        public LeafDrop {
            if (block == null || result == null || !block.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")
                    || !result.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+") || count < 1 || count > 64
                    || chanceDenominator < 1 || chanceDenominator > 65536 || successRoll < 0 || successRoll >= chanceDenominator) {
                throw new IllegalArgumentException("Invalid early leaf drop");
            }
        }
        public int countForRoll(int roll) {
            if (roll < 0 || roll >= chanceDenominator) throw new IllegalArgumentException("Roll outside leaf drop domain");
            return roll == successRoll ? count : 0;
        }
    }
    private record Data(int schema, String profile, List<Recipe> recipes, List<Smelt> smelting, Map<String, Integer> food, LeafDrop leafDrop) { }
    private static final Map<String, Data> DATA = Map.of(
        "historical_experience:indev_latest", load("indev_latest"),
        "historical_experience:infdev_420", load("infdev_420"),
        "historical_experience:infdev_611", load("infdev_611"));

    private EarlyGameplayRecipes() { }

    private static Data load(String name) {
        String path = "/data/historical_experience/early_gameplay/" + name + ".json";
        try (var stream = EarlyGameplayRecipes.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            Data data = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Data.class);
            if (data.schema != 1 || !data.profile.equals("historical_experience:" + name)
                    || data.recipes.isEmpty() || data.smelting.size() != 6
                    || data.food == null || data.food.size() != (name.equals("indev_latest") ? 4 : 6) || data.leafDrop == null) {
                throw new IllegalStateException("Invalid " + path);
            }
            return new Data(data.schema, data.profile, List.copyOf(data.recipes), List.copyOf(data.smelting), Map.copyOf(data.food), data.leafDrop);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    /** Other leaf species/eras are intentionally not inferred from this oak-only artifact mapping. */
    public static Optional<LeafDrop> leafDrop(String profile, String block) {
        Data data = profile == null ? null : DATA.get(profile);
        return data != null && data.leafDrop.block.equals(block) ? Optional.of(data.leafDrop) : Optional.empty();
    }
    public static boolean supports(String profile) { return profile != null && DATA.containsKey(profile); }
    public static Set<String> profiles() { return DATA.keySet(); }
    public static List<Recipe> recipes(String profile) {
        Data data = profile == null ? null : DATA.get(profile);
        return data == null ? List.of() : data.recipes;
    }
    public static List<Smelt> smelting(String profile) {
        Data data = profile == null ? null : DATA.get(profile);
        return data == null ? List.of() : data.smelting;
    }
    public static Optional<Recipe> find(String profile, int width, int height, List<String> input) {
        return recipes(profile).stream().filter(recipe -> recipe.matches(width, height, input)).findFirst();
    }
    public static boolean canCraftResult(String profile, String result) {
        return recipes(profile).stream().anyMatch(recipe -> !recipe.dormant && recipe.result.equals(result));
    }
    public static Map<String, Integer> foodHealing(String profile) {
        Data data = profile == null ? null : DATA.get(profile);
        return data == null ? Map.of() : data.food;
    }
    public static boolean canSmelt(String profile, String input, String result) {
        return canSmelt(profile, input, result, 1);
    }
    /** All three verified furnace ticks create/increment one output and decrement one input. */
    public static boolean canSmelt(String profile, String input, String result, int outputCount) {
        return outputCount == 1 && smelting(profile).stream().anyMatch(recipe -> recipe.input.equals(input) && recipe.result.equals(result));
    }
}

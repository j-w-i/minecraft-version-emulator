/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.Test;

class EarlyGameplayRecipesTest {
    private static final String INDEV = "historical_experience:indev_latest";
    private static final String APRIL = "historical_experience:infdev_420";
    private static final String JUNE = "historical_experience:infdev_611";
    private static List<String> grid(String... ids) {
        return java.util.Arrays.stream(ids).map(id -> id.isEmpty() ? "" : "minecraft:" + id).toList();
    }

    @Test void countsAreBuildLocalNotAlphaAliases() {
        assertEquals(68, EarlyGameplayRecipes.recipes(INDEV).size());
        assertEquals(69, EarlyGameplayRecipes.recipes(APRIL).size());
        assertEquals(72, EarlyGameplayRecipes.recipes(JUNE).size());
        for (String profile : EarlyGameplayRecipes.profiles()) {
            assertEquals(4, EarlyGameplayRecipes.recipes(profile).stream().filter(r -> r.dormant()).count());
            for (String item : List.of("bucket", "oak_stairs", "cobblestone_stairs", "oak_fence", "compass",
                    "clock", "bookshelf", "redstone_torch", "white_wool", "shears", "paper", "book")) {
                assertTrue(HistoricalCraftingEra.isRefused(profile, "minecraft:" + item, null), profile + " " + item);
            }
        }
        assertFalse(EarlyGameplayRecipes.canCraftResult(INDEV, "minecraft:golden_apple"));
        assertTrue(EarlyGameplayRecipes.canCraftResult(APRIL, "minecraft:golden_apple"));
        assertFalse(EarlyGameplayRecipes.canCraftResult(APRIL, "minecraft:ladder"));
        assertTrue(EarlyGameplayRecipes.canCraftResult(JUNE, "minecraft:ladder"));
        assertFalse(EarlyGameplayRecipes.canCraftResult(APRIL, "minecraft:oak_door"));
        assertTrue(EarlyGameplayRecipes.canCraftResult(JUNE, "minecraft:oak_door"));
    }

    @Test void everyObtainablePatternMatchesAtEveryOffsetAndMirror() {
        for (String profile : EarlyGameplayRecipes.profiles()) {
            for (var recipe : EarlyGameplayRecipes.recipes(profile)) {
                if (recipe.dormant()) continue;
                for (int dz = 0; dz <= 3 - recipe.height(); dz++) {
                    for (int dx = 0; dx <= 3 - recipe.width(); dx++) {
                        for (boolean mirror : new boolean[] {false, true}) {
                            List<String> items = new ArrayList<>(Collections.nCopies(9, ""));
                            for (int z = 0; z < recipe.height(); z++) for (int x = 0; x < recipe.width(); x++) {
                                items.set((z + dz) * 3 + x + dx,
                                    recipe.ingredients().get(z * recipe.width() + (mirror ? recipe.width() - 1 - x : x)));
                            }
                            var match = EarlyGameplayRecipes.find(profile, 3, 3, items).orElseThrow();
                            assertEquals(recipe.result(), match.result());
                            assertEquals(recipe.count(), match.count());
                        }
                    }
                }
            }
        }
    }

    @Test void ingredientsAndCountsAreNotModernApproximations() {
        var arrows = EarlyGameplayRecipes.find(INDEV, 1, 3, grid("iron_ingot", "stick", "feather")).orElseThrow();
        assertEquals("minecraft:arrow", arrows.result());
        assertEquals(4, arrows.count());
        assertTrue(EarlyGameplayRecipes.find(INDEV, 1, 3, grid("flint", "stick", "feather")).isEmpty());
        var steps = EarlyGameplayRecipes.find(INDEV, 3, 1,
            grid("cobblestone", "cobblestone", "cobblestone")).orElseThrow();
        assertEquals("minecraft:smooth_stone_slab", steps.result());
        assertEquals(3, steps.count());
        assertTrue(EarlyGameplayRecipes.find(INDEV, 3, 1, grid("stone", "stone", "stone")).isEmpty());
        assertEquals("minecraft:light_gray_wool", EarlyGameplayRecipes.find(INDEV, 3, 3,
            Collections.nCopies(9, "minecraft:string")).orElseThrow().result());
        assertEquals(1, EarlyGameplayRecipes.recipes(JUNE).stream()
            .filter(r -> r.result().equals("minecraft:ladder")).findFirst().orElseThrow().count());
        var painting = grid("oak_planks", "oak_planks", "oak_planks", "oak_planks", "light_gray_wool",
            "oak_planks", "oak_planks", "oak_planks", "oak_planks");
        assertTrue(EarlyGameplayRecipes.find(INDEV, 3, 3, painting).isPresent());
        assertTrue(EarlyGameplayRecipes.find(APRIL, 3, 3, painting).isEmpty());
    }

    @Test void smeltingAndFoodAreVerifiedNotEmpty() {
        for (String profile : EarlyGameplayRecipes.profiles()) {
            assertEquals(6, EarlyGameplayRecipes.smelting(profile).size());
            assertTrue(EarlyGameplayRecipes.canSmelt(profile, "minecraft:iron_ore", "minecraft:iron_ingot"));
            assertTrue(EarlyGameplayRecipes.canSmelt(profile, "minecraft:diamond_ore", "minecraft:diamond"));
            assertFalse(EarlyGameplayRecipes.canSmelt(profile, "minecraft:oak_log", "minecraft:charcoal"));
            assertFalse(EarlyGameplayRecipes.canSmelt(profile, "minecraft:raw_iron", "minecraft:iron_ingot"));
            var rules = HistoricalVersions.byId(profile).orElseThrow().rules();
            assertEquals(EarlyGameplayRecipes.foodHealing(profile), rules.foodHealPoints());
            assertEquals(3, rules.foodHealPoints().get("minecraft:porkchop"));
            assertEquals(10, rules.foodHealPoints().get("minecraft:mushroom_stew"));
            assertEquals("minecraft:arrow", rules.mobDrops().get("minecraft:skeleton").itemId());
            assertEquals("minecraft:feather", rules.mobDrops().get("minecraft:zombie").itemId());
        }
        assertEquals(4, HistoricalVersions.byId(INDEV).orElseThrow().rules().foodHealPoints().size());
        assertEquals(6, HistoricalVersions.byId(JUNE).orElseThrow().rules().foodHealPoints().size());
        assertEquals(42, HistoricalVersions.byId(APRIL).orElseThrow().rules().foodHealPoints().get("minecraft:golden_apple"));
    }

    @Test void furnaceOutputsAreExactlyOneAndUnknownPairsNeverPass() {
        for (String profile : EarlyGameplayRecipes.profiles()) for (var row : EarlyGameplayRecipes.smelting(profile)) {
            assertTrue(EarlyGameplayRecipes.canSmelt(profile, row.input(), row.result(), 1));
            for (int count : new int[] {-1, 0, 2, 64}) assertFalse(EarlyGameplayRecipes.canSmelt(profile, row.input(), row.result(), count));
            assertFalse(EarlyGameplayRecipes.canSmelt(profile, row.input(), "minecraft:netherite_ingot", 1));
        }
        assertFalse(EarlyGameplayRecipes.canSmelt(null, "minecraft:iron_ore", "minecraft:iron_ingot", 1));
        assertFalse(EarlyGameplayRecipes.canSmelt("historical_experience:beta_1_7_3", "minecraft:iron_ore", "minecraft:iron_ingot", 1));
    }

    @Test void ordinaryAndLaterWorldsDoNotUseEarlyRecipes() {
        assertFalse(EarlyGameplayRecipes.supports(null));
        assertTrue(EarlyGameplayRecipes.recipes(null).isEmpty());
        assertTrue(EarlyGameplayRecipes.smelting(null).isEmpty());
        assertTrue(EarlyGameplayRecipes.foodHealing(null).isEmpty());
        assertFalse(EarlyGameplayRecipes.supports("historical_experience:alpha_1_0_1"));
        assertFalse(HistoricalCraftingEra.isRefused(null, "minecraft:bucket", null));
        assertFalse(HistoricalCraftingEra.isRefused("historical_experience:beta_1_7_3", "minecraft:bucket", null));
    }
}

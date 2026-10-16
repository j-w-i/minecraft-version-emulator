/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Vanilla registry bootstrapping, not a Fabric/mixin or network test; real recipe values and pattern matches. */
class EarlyRecipeLookupRuntimeTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test void everyEarlyValueIsSpecialSoUnsyncedIdsCannotBeAwardedOrRequiredByLimitedCrafting() {
        for (String profile : EarlyGameplayRecipes.profiles()) for (var row : EarlyGameplayRecipes.recipes(profile)) {
            if (row.dormant()) continue;
            var input = CraftingInput.of(row.width(), row.height(), row.ingredients().stream().map(EarlyRecipeLookupRuntimeTest::stack).toList());
            var holder = EarlyRecipeLookup.find(profile, input, null).orElseThrow();
            assertTrue(holder.value().isSpecial(), holder.id().toString());
            assertEquals(row.count(), holder.value().getResultItem(null).getCount());
            assertEquals(row.result(), BuiltInRegistries.ITEM.getKey(holder.value().getResultItem(null).getItem()).toString());
            assertSame(holder, EarlyRecipeLookup.find(profile, input, null).orElseThrow());
        }
    }
    @Test void unsupportedProfilesDoNotEnterThePerLevelCache() {
        var input = CraftingInput.of(1, 1, List.of(stack("minecraft:oak_log")));
        assertTrue(EarlyRecipeLookup.find(null, input, null).isEmpty());
        assertTrue(EarlyRecipeLookup.find("historical_experience:beta_1_7_3", input, null).isEmpty());
        assertTrue(EarlyRecipeLookup.find("unknown:profile", input, null).isEmpty());
    }
    private static ItemStack stack(String id) {
        return id.isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }
}

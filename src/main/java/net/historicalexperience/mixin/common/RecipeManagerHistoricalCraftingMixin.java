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
import java.util.Optional;
import java.util.List;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalCraftingEra;
import net.historicalexperience.gameplay.HistoricalRecipeGate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Refuses a crafting recipe the active version never had, BEFORE a result exists.
 *
 * <h2>Why this replaced clearing the result slot</h2>
 *
 * <p>The earlier gate injected at the tail of {@code CraftingMenu.slotChangedCraftingGrid} and emptied the result
 * slot afterwards. By then the method had already recorded the recipe as "used" on the result container (which
 * unlocks it in the recipe book and awards its advancement), sent the finished stack to the client in a
 * {@code ClientboundContainerSetSlotPacket}, and stored the remote-slot copy the server compares against. Emptying
 * the slot fixed none of that, so the client could be left holding a result the server no longer agreed existed.
 *
 * <p>The recipe is chosen by exactly one call, {@code RecipeManager.getRecipeFor(type, input, level, previous)}, and
 * that is the choke point: the 3x3 table calls it directly, and the 2x2 inventory grid reaches it because
 * {@code InventoryMenu.slotsChanged} delegates to the same static method (verified in the 1.21.1 bytecode). Returning
 * an empty {@code Optional} for a refused recipe makes the vanilla method take its own "no recipe" branch: nothing is
 * assembled, nothing is recorded, and the client is sent an empty slot.
 *
 * <p>That is only the CRAFTING choke point. Stonecutter and smithing menus use the level-aware
 * {@code getRecipesFor} list API; early profiles must gate it too, before they can assemble a result.
 * No-level registry queries intentionally remain global and unchanged.
 *
 * <h2>Scope</h2>
 *
 * <p>Indev and the two offered Infdev builds use artifact-extracted exact crafting recipes and six smelts;
 * their other machine types are unavailable. Later profiles retain the existing crafting-only result gate.
 *
 * <p>This runs on the server because the vanilla caller returns early on the client, so there is no client
 * prediction to disagree with.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerHistoricalCraftingMixin {
    /** Build-local recipes precede vanilla lookup, including crafts whose historical ingredients differ. */
    @org.spongepowered.asm.mixin.injection.Inject(
        method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;"
            + "Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
        at = @At("HEAD"), cancellable = true, require = 1)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            void historicalExperience$earlyRecipe(RecipeType<T> type, I input, Level level, RecipeHolder<T> previous,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        String version = level instanceof net.minecraft.server.level.ServerLevel server
            ? net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(server)
            : ActiveVersion.current();
        if (!net.historicalexperience.gameplay.EarlyGameplayRecipes.supports(version)) return;
        if (type == RecipeType.CRAFTING && input instanceof net.minecraft.world.item.crafting.CraftingInput crafting) {
            cir.setReturnValue((Optional) net.historicalexperience.gameplay.EarlyRecipeLookup.find(version, crafting, level));
        } else if (type != RecipeType.SMELTING) {
            // Blasting/smoking, stonecutting, smithing and campfire cooking did not exist.
            cir.setReturnValue(Optional.empty());
        }
    }

    /** Stonecutter and smithing menus use this LIST API, not the single-result lookup above. */
    @org.spongepowered.asm.mixin.injection.Inject(
        method = "getRecipesFor", at = @At("HEAD"), cancellable = true, require = 1)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            void historicalExperience$earlyRecipeList(RecipeType<T> type, I input, Level level,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<List<RecipeHolder<T>>> cir) {
        String version = level instanceof net.minecraft.server.level.ServerLevel server
            ? net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(server)
            : ActiveVersion.current();
        if (!net.historicalexperience.gameplay.EarlyGameplayRecipes.supports(version)) return;
        if (type == RecipeType.CRAFTING && input instanceof net.minecraft.world.item.crafting.CraftingInput crafting) {
            cir.setReturnValue((List) net.historicalexperience.gameplay.EarlyRecipeLookup.find(version, crafting, level).stream()
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
        } else if (type != RecipeType.SMELTING) {
            // Vanilla menus retain and clear these lists on the next input change.
            cir.setReturnValue(new java.util.ArrayList<>());
        }
        // Never filter getAllRecipesFor/byKey: they have NO level and belong to the shared registry.
    }

    @ModifyReturnValue(method = "getRecipesFor", at = @At("RETURN"), require = 1)
    private <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            List<RecipeHolder<T>> historicalExperience$earlySmeltingList(List<RecipeHolder<T>> found,
                RecipeType<T> type, I input, Level level) {
        String version = level instanceof net.minecraft.server.level.ServerLevel server
            ? net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(server)
            : ActiveVersion.current();
        if (!net.historicalexperience.gameplay.EarlyGameplayRecipes.supports(version)) return found;
        if (type == RecipeType.CRAFTING) return found;
        if (type != RecipeType.SMELTING) return new java.util.ArrayList<>();
        return found.stream().filter(holder -> historicalExperience$earlySmeltAllowed(version, input, holder, level))
            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean historicalExperience$earlySmeltAllowed(String version, RecipeInput input, RecipeHolder<?> holder, Level level) {
        if (input.size() != 1) return false;
        ItemStack result = holder.value().getResultItem(level.registryAccess());
        String ingredient = BuiltInRegistries.ITEM.getKey(input.getItem(0).getItem()).toString();
        String output = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        return net.historicalexperience.gameplay.EarlyGameplayRecipes.canSmelt(version, ingredient, output, result.getCount());
    }

    @ModifyReturnValue(
        method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;"
            + "Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
        at = @At("RETURN"),
        require = 1)
    private <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            Optional<RecipeHolder<T>> historicalExperience$refuseAnachronisticRecipe(
                Optional<RecipeHolder<T>> found, RecipeType<T> type, I input, Level level, RecipeHolder<T> previous) {
        String versionId = level instanceof net.minecraft.server.level.ServerLevel server
            ? net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(server)
            : ActiveVersion.current();
        if (net.historicalexperience.gameplay.EarlyGameplayRecipes.supports(versionId)) {
            if (type == RecipeType.CRAFTING) return found; // Already chosen from the exact early table.
            if (type != RecipeType.SMELTING || found.isEmpty() || input.size() != 1) return Optional.empty();
            return historicalExperience$earlySmeltAllowed(versionId, input, found.get(), level) ? found : Optional.empty();
        }
        if (versionId == null || found.isEmpty() || !(found.get().value() instanceof CraftingRecipe recipe)) {
            return found;
        }
        RecipeHolder<T> holder = found.get();
        String path = holder.id().getPath();

        boolean refused = HistoricalCraftingEra.isRecipeRefused(versionId, path);
        if (!refused) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (!result.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
                String blockId = result.getItem() instanceof BlockItem blockItem
                    ? BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString()
                    : null;
                refused = HistoricalCraftingEra.isRefused(versionId, itemId, blockId);
            }
        }
        if (!refused) {
            return found;
        }
        HistoricalRecipeGate.recordRefusal(versionId, holder.id().toString());
        return Optional.empty();
    }
}
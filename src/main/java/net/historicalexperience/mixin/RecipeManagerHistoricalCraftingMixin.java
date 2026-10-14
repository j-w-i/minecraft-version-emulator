package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Optional;
import net.historicalexperience.gameplay.HistoricalActiveSession;
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
 * <h2>Scope</h2>
 *
 * <p>Only crafting recipes are judged. Smelting, blasting, smoking, campfire cooking, stonecutting and smithing go
 * through the same manager but are different recipe types, and the 1.21.1 furnace-family and stonecutter results are
 * outside what the era list covers; judging them by it would refuse real content. Each is a separate decision.
 *
 * <p>This runs on the server because the vanilla caller returns early on the client, so there is no client
 * prediction to disagree with.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerHistoricalCraftingMixin {
    @ModifyReturnValue(
        method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;"
            + "Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
        at = @At("RETURN"),
        require = 1)
    private <I extends RecipeInput, T extends net.minecraft.world.item.crafting.Recipe<I>>
            Optional<RecipeHolder<T>> historicalExperience$refuseAnachronisticRecipe(
                Optional<RecipeHolder<T>> found, RecipeType<T> type, I input, Level level, RecipeHolder<T> previous) {
        String versionId = HistoricalActiveSession.current();
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
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalRecipeGate;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses the 3x3 crafting table's result when the active version never had it.
 *
 * <p>This is the crafting-table half of the owner's request for "crafting checkpoints": before it, a bed could be
 * crafted in an Alpha world even though beds are Beta 1.3, and the same was true of every other later block.
 *
 * <p>{@code slotChangedCraftingGrid} is where the result slot is filled, so clearing it at {@code TAIL} is the
 * narrowest place that makes the refusal final: the player never sees a takeable result, and nothing about the
 * recipe itself is changed. {@link HistoricalRecipeGate} holds the decision and the reasons it asks the existing
 * block-availability table rather than carrying a recipe list.
 *
 * <p>The client runs this too, which is deliberate - a client that showed a craftable result the server refuses
 * would desync on the first click.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuHistoricalRecipeMixin {
    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void historicalExperience$refuseAnachronisticCraft(
        AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftSlots,
        ResultContainer resultSlots, RecipeHolder<CraftingRecipe> recipe, CallbackInfo callback
    ) {
        ItemStack result = resultSlots.getItem(0);
        if (HistoricalRecipeGate.refuses(HistoricalActiveSession.current(), result)) {
            resultSlots.setItem(0, ItemStack.EMPTY);
        }
    }
}

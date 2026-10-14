package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides the recipe book, which no declared version had.
 *
 * <p>minecraft.wiki, Recipe book history: <i>"Added the recipe book."</i> at 17w13a, released in <b>1.12</b>. The
 * newest version this project declares is Release 1.6.4, so the book is later than every one of them and there is no
 * boundary to search for - the answer is "none of them", which is why
 * {@code HistoricalWorldTweaks.availableForRecipeBook} answers false for every version and the option is offered for
 * no world.
 *
 * <h2>Why the component rather than the screen</h2>
 *
 * <p>{@code RecipeBookComponent.isVisible()} is what both the crafting table and the inventory ask before drawing or
 * accepting input for the book, so one return-value hook hides it everywhere and leaves nothing that can still be
 * clicked. Emptying it rather than removing the widget keeps the surrounding screen's layout arithmetic intact - the
 * component still reports its own position - so the crafting grid does not shift when the book disappears.
 */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentHistoricalMixin {
    @ModifyReturnValue(method = "isVisible", at = @At("RETURN"), require = 1)
    private boolean historicalExperience$hideRecipeBook(boolean visible) {
        return HistoricalWorldTweaks.recipeBookDisabled() ? false : visible;
    }
}

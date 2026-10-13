package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the enchanting table enchanting in versions where it could not.
 *
 * <p>{@code research/minecraft-version-history.md} Part IV records three historical states, and the middle one is
 * the trap: <b>Beta 1.9 Prerelease 3 has the enchanting table, its interface and its glint, and applies no
 * enchantments while spending nothing.</b> Beta 1.8 has no table at all but does have an experience bar. From
 * Prerelease 4 enchanting is real - 17 enchantments, numeric levels, levels spent.
 *
 * <p>So "the player has an experience bar" is not evidence that enchanting should work, and "the table opened" is
 * not evidence either. This cancels the menu's click handler outright, which both refuses the enchantment and
 * prevents the level spend, rather than letting the vanilla path run and trying to undo it afterwards - an
 * enchantment applied and then removed would still have written to the item.
 *
 * <p>Server-side only: the click is handled by the server in vanilla, and the client's copy of the menu is
 * presentation.
 *
 * <p>A shared mixin.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuHistoricalEnchantingMixin {
    /** Vanilla: {@code clickMenuButton(Player, int)} returns whether the click did anything. */
    @Inject(
        method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void historicalExperience$refuseEnchanting(Player player, int id,
            CallbackInfoReturnable<Boolean> callback) {
        if (!HistoricalGameplayTweaks.functionalEnchanting(HistoricalActiveSession.current())) {
            // false is what vanilla returns when a click does nothing, so the client shows no enchantment and no
            // level is taken.
            callback.setReturnValue(false);
        }
    }
}

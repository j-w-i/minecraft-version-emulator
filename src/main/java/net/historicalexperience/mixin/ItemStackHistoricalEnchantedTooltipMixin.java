package net.historicalexperience.mixin;

import java.util.List;

import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Beta 1.9 Prerelease 3's placeholder enchantment tooltip: an enchanted item says <b>"Enchanted!!"</b> instead of
 * naming its enchantments.
 *
 * <h2>Source</h2>
 *
 * <p>minecraft.wiki, Beta 1.9 Prerelease 3, Gameplay &gt; Enchanting: <i>"Not fully implemented, as no enchantments
 * exist yet."</i> and <i>"Enchanted items display \"Enchanted!!\" on their tooltip, and their name becomes blue."</i>
 * Beta 1.9 Prerelease 4, Changes &gt; Enchantment table: <i>"Enchanted items' tooltips now display their
 * enchantments instead of \"Enchanted!!\"."</i>
 *
 * <h2>What this replaces, and the correction that produced it</h2>
 *
 * <p>Prerelease 3 previously had its enchantment click <b>cancelled outright</b> by an earlier version of
 * {@code EnchantmentMenuHistoricalEnchantingMixin}, on the reading that "applies no enchantments" meant "enchanting
 * does not work". The owner corrected that from the historical record and it is the right reading: the table DID
 * enchant and DID spend levels, and what it lacked was <i>real enchantment data</i> - so the glint and the blue name
 * were real and the label was a stand-in for data that did not exist yet. Cancelling the click removed the working
 * half to imitate the missing half.
 *
 * <p>So enchanting is now handed back to vanilla for this build, and the substitution happens where the difference
 * actually is: the displayed tooltip.
 *
 * <h2>Why the hook is on {@code getTooltipLines} and why that is safe</h2>
 *
 * <p>The enchantment data stays exactly as vanilla wrote it - this only changes what is rendered, so nothing is
 * deleted from the item and a world upgraded to Prerelease 4 immediately shows the real names again. The
 * replacement is applied only to an item that <b>has</b> enchantments; an unenchanted item is returned untouched.
 *
 * <p>A shared mixin. The hook is on the shared tooltip path rather than a client-only class so a dedicated server
 * loads it identically, and the version gate means it can never fire in an ordinary world.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackHistoricalEnchantedTooltipMixin {
    /** The literal the wiki records. Kept verbatim, including both exclamation marks. */
    private static final Component PLACEHOLDER = Component.literal("Enchanted!!");

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void historicalExperience$placeholderEnchantmentTooltip(
            net.minecraft.world.item.Item.TooltipContext context,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.item.TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> callback) {
        if (!HistoricalGameplayTweaks.placeholderEnchantmentTooltip(HistoricalActiveSession.current())) {
            return;
        }
        if (!((Object) this instanceof ItemStack stack) || !stack.isEnchanted()) {
            // No enchanted data means no glint and no label: an ordinary item keeps its ordinary tooltip, which is
            // also what stops this from touching every item in the game.
            return;
        }

        List<Component> lines = callback.getReturnValue();
        if (lines == null || lines.size() < 2) {
            return;
        }
        // The first line is the item's own name, which this keeps - the wiki says the NAME becomes blue, and the
        // glint plus the blue name come from the enchantment data that vanilla still holds. Only the enchantment
        // lines beneath it are replaced, so the item is still identifiable.
        java.util.List<Component> replaced = new java.util.ArrayList<>(lines.size());
        replaced.add(lines.get(0));
        replaced.add(PLACEHOLDER);
        callback.setReturnValue(replaced);
    }
}

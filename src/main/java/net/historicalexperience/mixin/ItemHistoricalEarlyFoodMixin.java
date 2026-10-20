/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.mixin;

import net.historicalexperience.gameplay.EarlyGameplayRecipes;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dispatch before Item subclass overrides: honey bottles must not bypass the early food gate.
 * Verified p/r.a(stack,world,player) in all three pinned clients consumes on right-click without a
 * health gate or use timer; stew b.a delegates to it and returns a bowl. Use the existing vanilla
 * finish path (and existing historical heal/effect hooks), not a parallel inventory implementation.
 * Preserved later food stacks remain untouched. Later historical profiles and ordinary worlds pass through.
 */
@Mixin(ItemStack.class)
public abstract class ItemHistoricalEarlyFoodMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$onlyRegisteredFood(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        String profile = level instanceof ServerLevel server ? HistoricalProfileLookup.profileIdForLevel(server)
            : ActiveVersion.current();
        ItemStack stack = (ItemStack) (Object) this;
        if (!EarlyGameplayRecipes.supports(profile) || !stack.has(DataComponents.FOOD)) return;
        if (HistoricalFoodRules.healPointsFor(profile, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).isEmpty()) {
            cir.setReturnValue(InteractionResultHolder.fail(stack));
        } else {
            // No Player.canEat/full-health check and no artificial one-tick eating state in these builds.
            cir.setReturnValue(InteractionResultHolder.consume(stack.finishUsingItem(level, player)));
        }
    }
}

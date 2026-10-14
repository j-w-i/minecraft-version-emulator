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
 * Stops the enchanting table enchanting in versions that had no working table at all.
 *
 * <p>{@code research/minecraft-version-history.md} Part IV records the historical states, and the middle one is the
 * trap that this class originally got wrong: <b>Beta 1.9 Prerelease 3 has the enchanting table, its interface and its
 * glint, and it DID enchant and DID spend levels - what it had no data for was the enchantment itself.</b> Beta 1.8
 * has no table at all but does have an experience bar. From Prerelease 4 enchanting is real - 17 enchantments,
 * numeric levels, levels spent.
 *
 * <h2>The correction the owner made, and why the old reading was wrong twice over</h2>
 *
 * <p>This used to cancel the click for everything before Prerelease 4, on the reading that "applies no
 * enchantments" meant "enchanting does not work". That conflated a missing <i>effect</i> with a missing
 * <i>mechanic</i>: Prerelease 3's table applied a glint, made the name blue and took the player's levels, so
 * cancelling the click removed the part that worked in order to imitate the part that did not. The placeholder
 * label is now what represents the missing data, and it lives in
 * {@code ItemStackHistoricalEnchantedTooltipMixin} where the difference actually is.
 *
 * <p>So the gate is {@code enchantingDisabled} - before Prerelease 3 only - and not
 * {@code !functionalEnchanting}.
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
        if (HistoricalGameplayTweaks.enchantingDisabled(HistoricalActiveSession.current())) {
            // false is what vanilla returns when a click does nothing, so the client shows no enchantment and no
            // level is taken.
            callback.setReturnValue(false);
        }
    }
}

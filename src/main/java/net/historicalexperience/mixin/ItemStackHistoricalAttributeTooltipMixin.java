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

import net.historicalexperience.client.tooltip.HistoricalTooltips;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the item tooltip's attribute block - Attack Damage, Attack Speed and the slot header - before Release 1.6.1.
 *
 * <h2>Where the numbers came from</h2>
 *
 * <p>The attribute system shipped in <b>Release 1.6.1</b>, and with it the tooltip started printing what an item in
 * hand was worth. That was checked against the clients rather than remembered: {@code generic.attackDamage},
 * {@code attribute.modifier.plus.0} and the {@code attribute.name.*} family appear in no class or language file of
 * Beta 1.7.3 or Release 1.5, and are present in Release 1.6.1 and 1.6.4.
 *
 * <p>The {@code When in Main Hand:} header is a later arrival still. {@code item.modifiers.*} is in no language file
 * of 1.6.4, 1.7.10 or 1.8.9, and first appears in 1.9 - lower-cased as {@code When in main hand:}. Since every version
 * this project emulates predates 1.6.1, gating this one boundary removes the header along with the rest.
 *
 * <h2>Why the method is suppressed rather than its output filtered</h2>
 *
 * <p>The blank line that separates the attribute block from the item's name is emitted BY
 * {@code addAttributeTooltips}, immediately before the slot header, and nowhere else. Refusing the method therefore
 * removes the numbers, the header and that separator together, and removes them at the source - nothing has to be
 * recognised in already-built text afterward, and no line belonging to another part of the tooltip can be caught by
 * mistake.
 *
 * <p>Shared, so a dedicated server loads it identically; the gate answers about a client's active version, and an
 * ordinary world has none, so the modern tooltip is untouched.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackHistoricalAttributeTooltipMixin {
    @Inject(
        method = "addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void historicalExperience$noAttributeBlockBeforeRelease161(
            java.util.function.Consumer<net.minecraft.network.chat.Component> builder,
            net.minecraft.world.entity.player.Player player, CallbackInfo callback) {
        if (!HistoricalTooltips.showsAttributeTooltip()) {
            callback.cancel();
        }
    }
}

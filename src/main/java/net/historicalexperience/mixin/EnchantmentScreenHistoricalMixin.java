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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import java.util.List;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalEnchanting;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The two things the pre-Release-1.8 enchanting interface did not show.
 *
 * <ol>
 *   <li><b>No preview of what an offer would give.</b> "When enchanting an item, one enchantment now appears in the
 *       tooltip when selecting an enchantment" is the same Release 1.8 overhaul (14w02a) that added lapis. Before it
 *       the three options were the Standard Galactic Alphabet glyphs and a level, and the player learned what they
 *       had bought when they bought it. Only the tooltip's CONTENT is changed - the call is left in place so the
 *       interface, the book and the three option buttons are drawn exactly as vanilla draws them.</li>
 *   <li><b>No lapis line, and no lapis slot.</b> Lapis arrived with that overhaul, so the requirement line is removed
 *       from the tooltip and the slot is moved off-screen where it is neither drawn nor hit-testable - the mechanism
 *       this project already uses to hide the offhand slot in an old inventory. The menu's side of the rule, which
 *       stops it ever requiring or consuming lapis, is in {@code EnchantmentMenuHistoricalEnchantingMixin}.</li>
 * </ol>
 *
 * <p>What the old interface did show - the level the option costs - is kept. That line is vanilla's own and already
 * names the option's requirement, which is also what the player is charged once the menu's rule is applied, so the
 * number on screen is the number taken.
 *
 * <p>Client-side only, and gated on the version: an ordinary world keeps the modern screen exactly.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenHistoricalMixin {
    /** Far enough outside any window that a slot there is never drawn and never receives a click. */
    private static final int UNREACHABLE = -9999;

    /** The lapis slot's index in the menu: the item slot is 0 and lapis is 1. */
    private static final int LAPIS_SLOT = 1;

    @Inject(method = "init", at = @At("TAIL"))
    private void historicalExperience$hideLapisSlot(CallbackInfo callback) {
        if (!HistoricalEnchanting.usesOldTableRules(HistoricalActiveSession.current())) {
            return;
        }
        EnchantmentScreen self = (EnchantmentScreen) (Object) this;
        if (self.getMenu().slots.size() > LAPIS_SLOT) {
            // Written straight into the slot's own coordinates. This used to go through a duck interface of this
            // project's making because Slot.x and Slot.y were assumed to be final; they are plain fields in 1.21.1,
            // and Nostalgic Tweaks writes them the same way (`slot.x = x; slot.y = y;` in its SlotTracker). The duck
            // interface and its mixin are gone with that correction.
            Slot lapis = self.getMenu().slots.get(LAPIS_SLOT);
            lapis.x = UNREACHABLE;
            lapis.y = UNREACHABLE;
        }
    }

    /**
     * Skips the tooltip that reveals which enchantment an offer would give, and with it the lapis requirement line.
     *
     * <h2>Why the whole call is wrapped rather than its contents edited</h2>
     *
     * <p>The first version of this used {@code @ModifyExpressionValue} on the tooltip call to drop two of its lines.
     * That crashed the game: that injector requires a call that RETURNS a value, and
     * {@code renderComponentTooltip} returns {@code void}, so the mixin failed to apply while the player was already
     * in the world. {@code @WrapWithCondition} is the right tool for a void call - it decides whether the call happens
     * at all - and this project already uses it for that.
     *
     * <p>Nothing else in the screen's {@code render} is affected: the interface, the book and the three option
     * buttons are drawn before this call, so only the hover text goes. That is what the era did - the options were
     * the Standard Galactic Alphabet glyphs and a level, and the player found out what they had bought when they
     * bought it.
     */
    @WrapWithCondition(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip("
                + "Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"))
    private boolean historicalExperience$noPreviewAndNoLapis(net.minecraft.client.gui.GuiGraphics graphics,
            net.minecraft.client.gui.Font font, List<Component> lines, int mouseX, int mouseY) {
        return !HistoricalEnchanting.usesOldTableRules(HistoricalActiveSession.current());
    }
}

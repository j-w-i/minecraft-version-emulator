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

import net.historicalexperience.client.input.HistoricalJumpInput;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes held-jump repeat in the one build that removed it: Beta 1.9 Prerelease 1.
 *
 * <h2>The report, and the wiki line it comes from</h2>
 *
 * <p>Owner: <i>"When jumping in pre 1.9 beta 1 the xp being added is incorrect"</i>, and on being asked which half
 * was wrong, that holding the jump key still re-jumps. minecraft.wiki, Java Edition Beta 1.9-pre1, under
 * <b>Jumping</b>, lists the experience payout and then: <i>"Continuous jumping by holding down the jump key is no
 * longer possible."</i> This project implemented the payout and recorded the second line as needing no code, which
 * was wrong - vanilla keeps jumping while the key is held, so the payout repeated on every landing where that build
 * required a fresh press.
 *
 * <h2>Why the input and not the jump</h2>
 *
 * <p>The input is where "the key is held" is still true. Suppressing the jump itself, at {@code jumpFromGround},
 * would mean the server refused a jump the client had already predicted and played, and the client is the authority
 * for its own movement input. Clearing the flag here means the client never asks for the second jump at all - which
 * is what the original build did, and what keeps the client and the server agreeing in multiplayer.
 *
 * <p>The latch is read and written through {@link HistoricalJumpInput} rather than a field on this mixin, because the
 * state has to outlive a single input object and be cleared on a disconnect.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputHistoricalJumpMixin {
    /** Vanilla: {@code Input.tick(boolean, float)} - where the jump key is read into {@code Input.jumping}. */
    @Inject(method = "tick", at = @At("TAIL"), require = 1)
    private void historicalExperience$noHeldJumpRepeat(boolean isMovingSlowly, float slowDownFactor,
            CallbackInfo callback) {
        Input input = (Input) (Object) this;

        if (!HistoricalGameplayTweaks.jumpRequiresFreshPress(ActiveVersion.current())) {
            // Every other version, and every ordinary world, keeps vanilla's held-jump repeat.
            HistoricalJumpInput.allowNextPress();
            return;
        }
        if (!input.jumping) {
            // The key is up: the next press is a fresh one.
            HistoricalJumpInput.allowNextPress();
            return;
        }
        if (HistoricalJumpInput.alreadyJumped()) {
            // Still holding from the jump that has already happened, so this tick asks for nothing. The flag is
            // cleared by vanilla's own input handling on the next tick the key is up.
            input.jumping = false;
            return;
        }
        // This tick's press is the one that will jump; anything after it needs a release first.
        HistoricalJumpInput.markJumped();
    }
}

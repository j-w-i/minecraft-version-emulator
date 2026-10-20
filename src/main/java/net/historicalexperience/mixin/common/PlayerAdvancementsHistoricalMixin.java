/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.historicalexperience.version.Feature;
import net.historicalexperience.version.Features;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No advancement is ever awarded in a build that had no achievements.
 *
 * <p>research/version-transitions.json, mechanic "achievements", is explicit per build: absent from Alpha v1.0.1
 * "through Beta 1.3" (its matrix row is "A"); Beta 1.4 has only trigger "infrastructure ... but the complete
 * achievement set/UI arrives incrementally"; Beta 1.5 introduces "16 achievements and statistics" with "an
 * achievement/statistics UI". So a Beta 1.3 or 1.4 world must not show a modern advancement toast or fill the modern
 * advancement screen.
 *
 * <p>Cancelled at HEAD rather than filtered at the return: returning false afterwards would still record the progress
 * and register the listeners, so the screen would fill even though no toast appeared. HEAD means nothing is recorded.
 *
 * <p>Beta 1.5 and later keep the modern advancement system for now - the era's own sixteen achievements need a designed
 * mapping to modern advancement ids, which the corpus says "needs a per-page audit". That is recorded as an open gap
 * rather than half-done.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsHistoricalMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z",
        at = @At("HEAD"), cancellable = true)
    private void historicalExperience$noAchievementsBeforeBeta15(AdvancementHolder holder, String criterion,
            CallbackInfoReturnable<Boolean> callback) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(level);
        if (Features.active(Feature.NO_ACHIEVEMENTS, profile)) {
            callback.setReturnValue(false);
        }
    }
}

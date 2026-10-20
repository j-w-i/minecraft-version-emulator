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

import net.historicalexperience.gameplay.HistoricalFoodRules;
import net.historicalexperience.version.ActiveVersion;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.block.CakeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A cake slice heals in the versions without hunger.
 *
 * <p>research/minecraft-version-history.md: "Beta 1.2 cake (3 health per slice, 18 for the whole cake)". Cake is the
 * only pre-hunger food whose heal happens through a BLOCK interaction, so the item-food heal table could not cover it
 * and a Beta 1.2-1.7.3 cake restored nothing at all.
 *
 * <p>Two redirects, both inside vanilla's own {@code CakeBlock.eat}, so the bites count, the eating sound, the block
 * state update and the statistic stay vanilla's:
 * <ul>
 *   <li>{@code Player.canEat} answers false at a full hunger bar; there is no hunger bar in these builds, so a damaged
 *       player must not be refused a slice.</li>
 *   <li>{@code FoodData.eat} becomes {@code player.heal(3)}: the hunger it would have filled did not exist, and the
 *       health it restored was 3 per slice.</li>
 * </ul>
 *
 * <p>Shared: block interaction is server-authoritative, but the client predicts it too.
 */
@Mixin(CakeBlock.class)
public abstract class CakeBlockHistoricalHealMixin {
    @Redirect(method = "eat(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)"
            + "Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canEat(Z)Z"))
    private static boolean historicalExperience$alwaysEdible(Player player, boolean ignoreHunger) {
        return HistoricalFoodRules.cakeSliceHeal(ActiveVersion.current()) > 0.0F || player.canEat(ignoreHunger);
    }

    @Redirect(method = "eat(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)"
            + "Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private static void historicalExperience$healInstead(FoodData foodData, int nutrition, float saturation,
            net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, Player player) {
        float heal = HistoricalFoodRules.cakeSliceHeal(ActiveVersion.current());
        if (heal > 0.0F) {
            player.heal(heal);
        } else {
            foodData.eat(nutrition, saturation);
        }
    }
}

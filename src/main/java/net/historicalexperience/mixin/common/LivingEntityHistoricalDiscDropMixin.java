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
package net.historicalexperience.mixin.common;

import net.historicalexperience.gameplay.HistoricalDiscDrops;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A creeper killed by a skeleton drops a music disc, in the versions that had that rule.
 *
 * <p>minecraft.wiki, Creeper history: v1.0.14 added discs "13" and "cat" "dropped by creepers shot by skeletons";
 * Release 1.1 made it every disc except "11"; Release 1.4.3 added "11" and "wait". The eligible set and the version
 * gate live in {@link HistoricalDiscDrops}, which is pure and unit-tested; this hook only supplies the killer and the
 * world.
 *
 * <p>Injected at the tail of {@code die}, so it runs after vanilla's own drops: the disc is an addition to the
 * gunpowder, not a replacement (see the class comment on why that is recorded rather than certain).
 *
 * <p>Server-side only: an item entity spawned on the client is a ghost that vanishes on the next sync. This is a
 * shared mixin, and a shared mixin cannot be client-only, so the hook checks the side instead.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalDiscDropMixin {
    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("TAIL"))
    private void historicalExperience$creeperDisc(DamageSource source, CallbackInfo callback) {
        if (!((Object) this instanceof Creeper creeper)) {
            return;
        }
        if (!(creeper.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(source.getEntity() instanceof Skeleton)) {
            return;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(level);
        String disc = HistoricalDiscDrops.pick(profile, creeper.getRandom());
        if (disc == null) {
            return;
        }
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(disc));
        var entity = new ItemEntity(level, creeper.getX(), creeper.getY(), creeper.getZ(), new ItemStack(item));
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}

/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.mixin;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops monsters picking items up off the ground, in versions whose monsters did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/monster_rule/MobMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_MONSTER_ITEM_PICKUP}.
 *
 * <h2>Where it hooks, and why that took finding twice</h2>
 *
 * <p>Upstream wraps the {@code pickUpItem} call INSIDE {@code aiStep}; my first version cancelled
 * {@code Mob.pickUpItem} itself. Both stop a monster picking something up, but the call-site wrap leaves the
 * method intact for every other caller and needs no cancellation - and it sidesteps the trap that cost me a
 * server crash when I started: {@code pickUpItem} is {@code protected}, so redirecting it with a static
 * invoker found no candidates and took the game down.
 *
 * <p>The carrier for this was listed as {@code mob_ai/MobMixin} in my own notes, which is a DIFFERENT file about
 * mob movement speed. Two files share the name {@code MobMixin} in different packages, so the bare filename in a
 * search result pointed at the wrong one. The {@code instanceof Monster} guard is upstream's, so a villager
 * still picks up what it is given.
 */
@Mixin(Mob.class)
public abstract class MobHistoricalItemPickupMixin {
    /** Upstream: {@code nt_monster_rule$shouldPickupItem}. */
    @WrapWithCondition(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;pickUpItem("
                + "Lnet/minecraft/world/entity/item/ItemEntity;)V"))
    private boolean historicalExperience$shouldPickupItem(Mob mob, ItemEntity itemEntity) {
        if (mob instanceof Monster) {
            return !Features.active(Feature.NO_MONSTER_ITEM_PICKUP);
        }
        return true;
    }
}

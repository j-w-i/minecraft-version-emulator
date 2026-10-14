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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A pre-Beta-1.8 bow takes no durability from being fired.
 *
 * <h2>Source</h2>
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_bow/ProjectileWeaponItemMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.INVINCIBLE_BOW}. Upstream has two
 * injections on the same instruction - one resetting the damage to zero immediately before the call, one wrapping
 * the call itself. <b>Only the wrap is ported</b>, and that is a deliberate reduction: the reset exists so that a
 * bow already at its last point of durability survives the shot it is about to take, which only matters if the
 * call is still reached. Refusing the call outright makes the reset redundant, so carrying both would be dead code
 * that reads like a safeguard.
 *
 * <h2>Boundary</h2>
 *
 * <p>Pre-Beta-1.8, the same boundary as the rest of the bow trio. The wiki's Bow history: <i>"1.8 Pre-release:
 * Arrows can no longer be rapid-fired from bows; they must be charged first."</i> A bow that is fired the instant it
 * is used has no charge cycle to wear it out - which is why the owner asked for invincible and instant bows
 * together, and why they share a gate.
 *
 * <p>Only a {@link BowItem} is spared. A crossbow, a trident or anything else that reaches this method keeps its
 * normal durability, because upstream checks the same class and because the era being emulated had one projectile
 * weapon.
 *
 * <p>Shared mixin: durability is server state.
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemHistoricalInvincibleBowMixin {
    /** Upstream: {@code nt_combat_bow$isBowBreakable}. */
    @WrapWithCondition(
        method = "shoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak("
                + "ILnet/minecraft/world/entity/LivingEntity;"
                + "Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private boolean historicalExperience$bowIsInvincible(ItemStack itemStack, int amount, LivingEntity entity,
            EquipmentSlot slot) {
        if (itemStack.getItem() instanceof BowItem) {
            return !HistoricalGameplayTweaks.invincibleBow();
        }
        return true;
    }
}

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
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops monsters spawning with armour or enchanted gear, in versions whose monsters did not.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/monster_spawn/MobMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_MONSTER_ARMOR_SPAWN} and
 * {@code GameplayTweak.DISABLE_MONSTER_ENCHANT_SPAWN}. All three injection points are upstream's.
 *
 * <h2>The ordering detail that makes the armour half work</h2>
 *
 * <p>Upstream clears armour at the HEAD of {@code populateDefaultEquipmentEnchantments}, not in
 * {@code populateDefaultEquipmentSlots}. That looks like the wrong method until you notice the call order:
 * finalisation populates slots FIRST and enchantments second, so clearing here runs after the armour exists.
 * Written against the other method it would clear empty slots and change nothing - a no-op that looks like a
 * port.
 *
 * <p>Every guard is upstream's: only a {@code Monster} is affected, so a villager or a wolf keeps its gear, and
 * the two enchantment wrappers each check that again rather than relying on the first check having run.
 */
@Mixin(Mob.class)
public abstract class MobHistoricalEquipmentMixin {
    @Shadow
    public abstract void setItemSlot(EquipmentSlot slot, ItemStack stack);

    /** Upstream: {@code nt_monster_spawn$removeEquipment}. */
    @Inject(method = "populateDefaultEquipmentEnchantments", at = @At("HEAD"))
    private void historicalExperience$removeEquipment(ServerLevelAccessor level, RandomSource random,
            DifficultyInstance difficulty, CallbackInfo callback) {
        if (!((Object) this instanceof Monster)) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()
                && HistoricalGameplayTweaks.appliesTo("monsterGear", HistoricalActiveSession.current())) {
                this.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    /** Upstream: {@code nt_monster_spawn$shouldEnchantWeapon}. */
    @WrapWithCondition(
        method = "populateDefaultEquipmentEnchantments",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;enchantSpawnedWeapon("
                + "Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/util/RandomSource;"
                + "Lnet/minecraft/world/DifficultyInstance;)V"))
    private boolean historicalExperience$shouldEnchantWeapon(Mob mob, ServerLevelAccessor level,
            RandomSource random, DifficultyInstance difficulty) {
        if (mob instanceof Monster) {
            return !HistoricalGameplayTweaks.appliesTo("monsterEnchant", HistoricalActiveSession.current());
        }
        return true;
    }

    /** Upstream: {@code nt_monster_spawn$shouldEnchantArmor}. */
    @WrapWithCondition(
        method = "populateDefaultEquipmentEnchantments",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;enchantSpawnedArmor("
                + "Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/util/RandomSource;"
                + "Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/DifficultyInstance;)V"))
    private boolean historicalExperience$shouldEnchantArmor(Mob mob, ServerLevelAccessor level,
            RandomSource random, EquipmentSlot slot, DifficultyInstance difficulty) {
        if (mob instanceof Monster) {
            return !HistoricalGameplayTweaks.appliesTo("monsterEnchant", HistoricalActiveSession.current());
        }
        return true;
    }
}

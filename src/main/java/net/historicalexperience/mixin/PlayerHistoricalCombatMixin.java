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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The three 1.9 combat changes: no attack cooldown, no critical hits, no sweep attack.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/PlayerMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_COOLDOWN},
 * {@code DISABLE_CRITICAL_HIT} and {@code DISABLE_SWEEP}. Upstream's targets, injectors and method shapes are
 * kept; only the condition changed, from a global flag to this world's version.
 *
 * <h2>What had to be corrected to match upstream</h2>
 *
 * <p>My first version had only the {@code getAttackStrengthScale} half of the cooldown. Upstream also
 * <b>resets the attack strength ticker</b> at the head of {@code attack}, through an accessor - without it the
 * scale reads full while the client's own ticker still advances, which is exactly the kind of half-port that
 * looks right and is not.
 *
 * <p>The sweep and critical-hit targets were reached by disassembling {@code Player} here, before this file was
 * read: {@code Level.getEntitiesOfClass(Class, AABB)} inside {@code attack}, and {@code Player.onClimbable()}
 * inside {@code attack}. Both are <b>byte-for-byte the targets upstream uses</b>, which is the first
 * independent confirmation that the earlier work landed where Nostalgic Tweaks lands. The injector forms are
 * now upstream's too.
 */
@Mixin(Player.class)
public abstract class PlayerHistoricalCombatMixin {
    /** Upstream: {@code nt_combat_player$onAttack}. */
    @Inject(method = "attack", at = @At("HEAD"))
    private void historicalExperience$resetAttackStrengthTicker(Entity target, CallbackInfo callback) {
        if (!HistoricalGameplayTweaks.appliesTo("cooldown", ActiveVersion.current())) {
            return;
        }
        Player self = (Player) (Object) this;
        ((LivingEntityAttackTickerAccess) self)
            .setAttackStrengthTicker((int) Math.ceil(self.getCurrentItemAttackStrengthDelay()));
    }

    /** Upstream: {@code nt_combat_player$modifyAttackStrengthScale}. */
    @ModifyReturnValue(method = "getAttackStrengthScale", at = @At("RETURN"))
    private float historicalExperience$modifyAttackStrengthScale(float attackStrengthScale) {
        if (HistoricalGameplayTweaks.appliesTo("cooldown", ActiveVersion.current())) {
            return 1.0F;
        }
        return attackStrengthScale;
    }

    /** Upstream: {@code nt_combat_player$modifyCriticalHitAttack}. */
    @ModifyExpressionValue(method = "attack",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;onClimbable()Z"))
    private boolean historicalExperience$modifyCriticalHitAttack(boolean isOnClimbable) {
        return HistoricalGameplayTweaks.appliesTo("criticalHit", ActiveVersion.current())
            || isOnClimbable;
    }

    /** Upstream: {@code nt_combat_player$modifyListOfSweepAttacks}. */
    @ModifyExpressionValue(method = "attack",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;"
                + "Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<LivingEntity> historicalExperience$modifyListOfSweepAttacks(
            List<LivingEntity> listOfSweepAttacks) {
        if (HistoricalGameplayTweaks.appliesTo("sweep", ActiveVersion.current())) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordSweepCancel();
            return List.of();
        }
        return listOfSweepAttacks;
    }
}

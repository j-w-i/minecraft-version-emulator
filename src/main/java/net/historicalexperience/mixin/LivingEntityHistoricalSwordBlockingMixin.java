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

import net.historicalexperience.version.ActiveVersion;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.historicalexperience.gameplay.HistoricalSwordBlocking;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Reduces incoming damage while a player holds a raised sword, in versions that had that stance.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.SWORD_BLOCK_DAMAGE_REDUCTION}
 * (default 50).
 *
 * <h2>What my first version left out, and it was the interesting part</h2>
 *
 * <p>Mine halved the damage and stopped there. Upstream also <b>wears the sword down</b>: a blocked hit above one
 * damage costs the raised sword a point of durability, and if that breaks it, the block ends with
 * {@code stopUsingItem}. That is the whole tension of the old stance - blocking was not free - and without it a
 * diamond sword blocks forever at no cost, which is not the mechanic any 1.8 player would recognise.
 *
 * <p>The injection is {@code @ModifyVariable(argsOnly = true)} on the hurt amount, the same shape upstream uses,
 * rather than the redirect I had written. Two things upstream checks first are kept: the entity must be a player,
 * and that player must be using an item at all.
 *
 * <p>The fall damage half is here too, and it is the other documented sub-behaviour of the old stance: blocking
 * used to reduce fall damage as well as melee. Magnitude from the wiki's Blocking history, under 1.0.0 / Beta 1.9
 * Prerelease: "Blocking previously allowed the player to fall 42 blocks without dying, instead of 22." Fall damage
 * being linear in distance, 22 becoming 42 is the damage being halved - which agrees with the 50% the sword
 * blocking row already cites, and the code reuses that same multiplier so the two cannot drift apart. The wiki
 * confirms the companion mechanic, rising faster underwater, but never gives a magnitude for it, so that one is
 * not here.
 *
 * <p>The paired use-duration injection is now here as well. Upstream forces the duration to 72000 ticks for a
 * player who can block, because a sword has no use duration of its own - so without it the stance begins and ends
 * in the same tick and there is no block for this hook to reduce.
 *
 * <p>NOT ported, and visible rather than implied: the client-side arm and item animations, which are a separate
 * carrier ({@code SwordBlockingRenderer} plus the two render mixins), and upstream's
 * {@code BLOCK_WITH_SWORD_ON_SHIELD} case, which the renderer asks about.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalSwordBlockingMixin {
    /**
     * Upstream: {@code nt_combat_player$modifyGetUseDuration}. 72000 ticks is one hour, which in practice means
     * the stance lasts until the player lets go.
     */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
        method = "startUsingItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getUseDuration("
                + "Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int historicalExperience$swordBlockUseDuration(int useDuration) {
        if ((Object) this instanceof Player player && HistoricalSwordBlocking.canBlock(player)) {
            return 72000;
        }
        return useDuration;
    }

    /**
     * Upstream has no equivalent: Nostalgic Tweaks does not implement the fall damage half. Implemented from the
     * research row and the wiki quote in this file's header, which is why the magnitude is cited rather than
     * chosen.
     *
     * <p>{@code ordinal = 1} selects the multiplier, not the distance, which by type alone would be ambiguous.
     */
    @ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float historicalExperience$reduceBlockedFallDamage(float multiplier) {
        // The NARROWER window, not isBlocking: the reduction was removed in Beta 1.9 Prerelease, before blocking
        // itself went in 1.9, so Beta 1.9 Prerelease 3 has the stance without the reduction.
        if ((Object) this instanceof Player player
            && HistoricalSwordBlocking.isBlocking(player)
            && HistoricalGameplayTweaks.blockFallDamageReductionEnabled(ActiveVersion.current())) {
            return multiplier * HistoricalSwordBlocking.damageMultiplier();
        }
        return multiplier;
    }

    @ModifyVariable(argsOnly = true, method = "hurt", at = @At("HEAD"))
    private float historicalExperience$onPlayerHurt(float hurtAmount) {
        if (!((Object) this instanceof Player player) || !player.isUsingItem()) {
            return hurtAmount;
        }

        if (!HistoricalSwordBlocking.isBlocking(player)) {
            return hurtAmount;
        }

        // NO durability cost. Nostalgic Tweaks spends a point per blocked hit and this port copied that, but
        // research/historical-features.json records swordBlockDurabilityLoss = False for the Beta-era stance: the
        // sword "does not lose durability while blocking". The research decides version facts, so upstream's
        // behaviour is not followed here.
        return hurtAmount * HistoricalSwordBlocking.damageMultiplier();
    }
}

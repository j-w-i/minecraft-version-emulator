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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mutes the 1.9 attack sound set, leaving the hit itself to be heard through the mob's hurt sound.
 *
 * <p>PORTED from Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/helper/sound/SoundHelper.java}, method
 * {@code isAttackHandled}, which answers {@code SoundTweak.OLD_ATTACK} - a flag upstream defaults to
 * <b>true</b>. The six sound events muted are upstream's list, taken unchanged.
 *
 * <h2>What the old behaviour actually was</h2>
 *
 * <p>Before 1.9 a hit made one sound: the target's. The sounds below - crit, knockback, no-damage, strong, weak
 * and sweep - were added alongside the 1.9 combat rework, so this is not "replace one sound with another"; it is
 * removing the variants that did not exist, which leaves the classic single hit.
 *
 * <h2>Why this is not a copy of upstream's mechanism</h2>
 *
 * <p>Upstream routes every positional sound through its own {@code PositionSoundHandler}, muting by rebuilding a
 * {@code SimpleSoundInstance}. That framework exists to serve dozens of sound tweaks at once, and this port needs
 * one of them: at the head of {@code playSound} the {@code SoundEvent} is already a parameter, so cancelling there
 * is the same outcome with nothing built around it. The list of sounds and the condition are upstream's; only the
 * plumbing is narrower, and that is stated rather than implied.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelHistoricalAttackSoundMixin {
    /** Upstream: {@code SoundHelper.isAttackHandled}. */
    @Inject(
        method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V",
        at = @At("HEAD"),
        cancellable = true)
    private void historicalExperience$muteModernAttackSounds(double x, double y, double z, SoundEvent sound,
            SoundSource source, float volume, float pitch, boolean distanceDelay, long seed,
            CallbackInfo callback) {
        if (!isModernAttackSound(sound)) {
            return;
        }

        // The 1.9-combat gate, shared with the cooldown and the sweep: no declared version is on the far side of
        // it, and the three cannot disagree about which versions had the modern attack sound set.
        if (HistoricalGameplayTweaks.appliesTo("sweep", ActiveVersion.current())) {
            callback.cancel();
        }
    }

    /** Upstream's six: the attack sound variants that arrived with the 1.9 combat rework. */
    private static boolean isModernAttackSound(SoundEvent sound) {
        return sound == SoundEvents.PLAYER_ATTACK_CRIT
            || sound == SoundEvents.PLAYER_ATTACK_KNOCKBACK
            || sound == SoundEvents.PLAYER_ATTACK_NODAMAGE
            || sound == SoundEvents.PLAYER_ATTACK_STRONG
            || sound == SoundEvents.PLAYER_ATTACK_SWEEP
            || sound == SoundEvents.PLAYER_ATTACK_WEAK;
    }
}

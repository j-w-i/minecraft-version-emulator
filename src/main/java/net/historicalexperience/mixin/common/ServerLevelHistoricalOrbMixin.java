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
package net.historicalexperience.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.historicalexperience.gameplay.HistoricalCombatDiagnostics;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops experience orbs entering the world, in versions that awarded no experience.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/experience_orb/ServerLevelMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.DISABLE_ORB_SPAWN}.
 *
 * <h2>What was wrong with my first attempt</h2>
 *
 * <p>I cancelled {@code ExperienceOrb.award} at HEAD. That covers the ordinary award paths and it demonstrably
 * worked - the probe counted a suppression - but it is not where upstream stops an orb. Upstream refuses the
 * entity at the point the level adds it to the entity section manager, which catches <b>every</b> way an orb can
 * enter a world: block drops, smelting, fishing, breeding, commands, and anything another mod or a datapack
 * creates. {@code award} is one caller of many, so my version was a subset that happened to look complete.
 *
 * <p>Upstream's second injection, which silences a log warning about an entity being removed before it was
 * added, is NOT ported: it is gated on {@code IMMEDIATE_EXPERIENCE_PICKUP}, a Nostalgic Tweaks feature this
 * project does not implement, so nothing here can trigger that warning. Adding it would suppress a message this
 * code cannot produce.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelHistoricalOrbMixin {
    @SuppressWarnings("MixinExtrasOperationParameters")
    @WrapOperation(
        method = "addEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity("
                + "Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private <T extends EntityAccess> boolean historicalExperience$shouldAddExperienceEntity(
            PersistentEntitySectionManager<T> manager, T entity, Operation<Boolean> operation) {
        if (entity instanceof ExperienceOrb
            && HistoricalGameplayTweaks.experienceOrbSpawnDisabled((ServerLevel) (Object) this)) {
            HistoricalCombatDiagnostics.recordOrbRefused();
            return false;
        }
        return operation.call(manager, entity);
    }
}

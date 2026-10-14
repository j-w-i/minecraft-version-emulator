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
import net.historicalexperience.gameplay.HistoricalLootRules;
import net.historicalexperience.gameplay.HistoricalMobLootHelper;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives a mob the drops its era had, and withholds experience from versions that awarded none.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mob_drops/LivingEntityMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers the {@code OLD_*_DROPS} family.
 *
 * <h2>Substitution, not replacement</h2>
 *
 * <p>Upstream modifies the {@code getLootTable} call inside {@code dropFromLootTable} and returns a generated
 * historical table, so the vanilla death path still rolls it. My first version cancelled
 * {@code dropFromLootTable} outright and spawned the items itself. The visible drop looked right - a Beta 1.7.3
 * zombie dropped feathers either way - while two vanilla mechanics were skipped underneath it: a Looting sword
 * changed nothing, and a mob that died on fire did not drop the cooked form. Calling downstream code is not the
 * same thing as calling the code that downstream calls.
 *
 * <h2>The experience half</h2>
 *
 * <p>The {@code dropExperience} hook is kept, and is a separate rule from upstream's orb suppression: it removes
 * experience in versions whose own registry had no orbs, which is a fact about the version rather than about how
 * an orb is created. The owner confirmed its effect in play.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHistoricalLootMixin {
    /** Upstream: {@code nt_mob_drops$modifyDropLootTable}. */
    @ModifyExpressionValue(
        method = "dropFromLootTable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable("
                + "Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;"))
    private LootTable historicalExperience$replaceLootTable(LootTable vanilla) {
        LivingEntity self = (LivingEntity) (Object) this;
        LootTable chosen = HistoricalMobLootHelper.getTable(self, vanilla);

        if (chosen != vanilla) {
            String entityTypeId =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).toString();
            HistoricalLootRules.recordReplacedDeath(entityTypeId);
        }

        return chosen;
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$suppressExperience(Entity killer, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String profileId = HistoricalProfileLookup.profileIdForLevel(serverLevel);
        if (profileId != null && HistoricalLootRules.suppressesExperience(profileId)) {
            HistoricalLootRules.recordSuppressedExperience();
            callback.cancel();
        }
    }
}

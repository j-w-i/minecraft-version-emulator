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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stops animals being bred with food, in versions that had no breeding.
 *
 * <p>Ported from Nostalgic Tweaks {@code mixin/tweak/gameplay/animal_spawn/AnimalMixin.java} (LGPL-3.0; see
 * THIRD_PARTY_NOTICES.md), which answers {@code DISABLE_ANIMAL_BREEDING}.
 *
 * <p>The redirect is on the {@code isFood} <b>call inside {@code mobInteract}</b> rather than on
 * {@code isFood} itself, and that distinction is deliberate: on 1.21.1 {@code Animal.isFood} is
 * {@code public abstract}, so there is no body to modify, and the same predicate is consulted by unrelated
 * interactions. Answering "not food" only at the breeding call site removes breeding without changing what
 * an animal is fed for any other purpose.
 *
 * <p>Boundary: breeding arrives in the <b>Beta 1.9 Prerelease 2</b>, so Beta 1.8.1 had none.
 */
@Mixin(Animal.class)
public abstract class AnimalHistoricalBreedingMixin {
    @Redirect(method = "mobInteract",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Animal;isFood(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean historicalExperience$isFoodForBreeding(Animal animal, ItemStack stack) {
        // The session, not the level: this rule is server-side AI today, but a level-based question is the exact
        // shape that silently switched sprinting and sword blocking off on the client, and "this one is
        // server-only" is a judgement that can stop being true without anyone noticing.
        if (Features.active(Feature.NO_ANIMAL_BREEDING)) {
            return false;
        }
        return animal.isFood(stack);
    }
}

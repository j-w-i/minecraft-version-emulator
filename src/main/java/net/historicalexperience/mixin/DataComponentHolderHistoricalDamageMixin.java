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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalDamageValues;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives old weapons and tools the damage numbers their version used.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_item/DataComponentHolderMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.OLD_DAMAGE_VALUES}.
 *
 * <h2>Why this replaced two of my own hooks</h2>
 *
 * <p>I had hooked {@code ItemStack.forEachModifier} - twice, once per overload - and then added an equipment
 * refresh because the attribute map is built before a player joins and therefore before the version is known.
 * Upstream needs neither. It modifies {@code getOrDefault} on {@code DataComponentHolder}, which is where every
 * reader of the attribute component gets it from: the attribute map, the tooltip, and
 * {@code forEachModifier} itself, which calls through to it. One hook covers all of them, and the two
 * overloads stop being a question at all.
 *
 * <p>The refresh in {@code HistoricalEquipmentRefresh} is still needed, for a reason upstream does not have:
 * upstream's flag is a global config known from the first tick, while this project's answer depends on a
 * session that is only established when a player joins. The component now returns the right value whenever it
 * is asked; the map that was built before the answer existed still has to be rebuilt.
 */
@Mixin(DataComponentHolder.class)
public interface DataComponentHolderHistoricalDamageMixin {
    /** Upstream: {@code nt_combat_item$modifyGetOrDefaultAttributes}. */
    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "getOrDefault", at = @At("RETURN"))
    default <T> T historicalExperience$modifyGetOrDefaultAttributes(T value,
            DataComponentType<? extends T> component) {
        if (DataComponents.ATTRIBUTE_MODIFIERS != component) {
            return value;
        }

        if (!((Object) this instanceof ItemStack itemStack) || !(value instanceof ItemAttributeModifiers modifiers)) {
            return value;
        }

        if (itemStack.getItem() instanceof TieredItem tieredItem
            && HistoricalDamageValues.isApplicable(itemStack)) {
            return (T) HistoricalDamageValues.rewrite(itemStack, modifiers);
        }

        return value;
    }
}

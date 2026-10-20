/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.gameplay;

import net.historicalexperience.version.ActiveVersion;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Whether a stack of food refuses to stack, because the active version predates Beta 1.8.
 *
 * <h2>Why this is its own file and not a nested class</h2>
 *
 * <p>It used to be a nested helper called {@code HistoricalFoodStackLimit} inside
 * {@code ContainerHistoricalFoodStackMixin}, which was <b>fatal at runtime</b> and crashed the owner's client the
 * first time they put planks into a crafting table in a Beta 1.2 world:
 *
 * <pre>
 * IllegalClassLoadError: net.historicalexperience.mixin.ContainerHistoricalFoodStackMixin$HistoricalFoodStackLimit
 * is in a defined mixin package net.historicalexperience.mixin.* owned by historical_experience.mixins.json and
 * cannot be referenced directly
 * </pre>
 *
 * <p>The reason is worth stating precisely, because it is not obvious and it is not "nested classes are bad": the
 * mixin config declares {@code "package": "net.historicalexperience.mixin"}, which makes the transformer treat
 * <b>every</b> class in that package as a mixin that only it may load. A nested helper is therefore unreachable from
 * ordinary code no matter how it is named, and it stayed invisible until a container click actually reached the
 * reference - a compile passes, a class-load of the mixin passes, and only the call site fails.
 *
 * <p>So the rule for this project is: <b>nothing under {@code net.historicalexperience.mixin} may be referenced by
 * name from outside it.</b> Shared logic lives here instead.
 *
 * <h2>Why one shared test rather than three copies</h2>
 *
 * <p>Three mixins need this answer - the container's merge limit, the slot's own limit, and the menu's - and a
 * disagreement between them is a desync: the client predicts a merge the server then refuses, or the reverse. One
 * method, asked by all three, is how they stay in agreement.
 */
public final class HistoricalFoodStackLimit {
    private HistoricalFoodStackLimit() {
    }

    /** True when this stack is food and the active version predates Beta 1.8. */
    public static boolean foodDoesNotStack(ItemStack stack) {
        return !HistoricalGameplayTweaks.foodStacks(ActiveVersion.current())
            && stack != null
            && !stack.isEmpty()
            && stack.has(DataComponents.FOOD);
    }
}

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
package net.historicalexperience.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.inventory.ClickType;

/**
 * Exposes the protected fields of a container screen that the tooltip probe needs: which slot is hovered, and where the
 * GUI is drawn, so it can place the cursor over a slot that actually holds an item.
 *
 * <p>Follows the same accessor pattern as {@link GuiAccess} and {@link AxeAccess}.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("hoveredSlot")
    Slot historicalExperience$hoveredSlot();

    @Accessor("leftPos")
    int historicalExperience$leftPos();

    @Accessor("topPos")
    int historicalExperience$topPos();

    /** Native click dispatch for explicitly armed developer gesture acceptance. */
    @Invoker("slotClicked")
    void historicalExperience$slotClicked(Slot slot, int slotId, int button, ClickType type);
}

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

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the {@link LevelSummary} behind a world-list row.
 *
 * <p>Needed because the row's own accessor is misleading for this purpose: {@code getLevelName()} returns
 * {@code summary.getLevelName()}, the name a player sees, not the folder a world lives in. Two worlds may share a
 * display name, so resolving a save directory from it would be able to pick the wrong world - and this code
 * changes world metadata, which makes picking the wrong one unacceptable.
 *
 * <p>{@code LevelSummary.getLevelId()} is the folder name, and it is public; only the field holding the summary is
 * not.
 *
 * <p>This is a CLIENT mixin, registered under {@code client}.
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {
    /** The summary of the world this row represents. */
    @Accessor("summary")
    LevelSummary historicalExperience$summary();
}

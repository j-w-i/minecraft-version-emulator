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
package net.historicalexperience.worldgen;

import net.historicalexperience.HistoricalExperience;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Decides whether a world is one of this mod's historical worlds, for the one question vanilla asks about it.
 *
 * <h2>The measured problem this answers</h2>
 *
 * <p>Every historical world showed the client's <em>"Worlds using Experimental Settings are not supported"</em>
 * confirmation before it would load, with the buttons "Create Backup and Load", "I know what I'm doing!" and
 * "Cancel" - and a "Create Backup and Load" that backed up a world nobody had asked to back up. Read from the
 * 1.21.1 client rather than guessed at, the screen comes from
 * {@code WorldOpenFlows.askForBackup(access, customized, onLoad, onCancel)}, which is reached only when
 * {@code WorldData.worldGenSettingsLifecycle()} is not {@code Lifecycle.stable()}. That lifecycle is built in
 * {@code LevelStorageSource.loadLevelData} as
 * {@code settings.dimensions().bake(levelStems).lifecycle().add(registryAccess.allRegistriesLifecycle())}, and
 * {@code bake} is where the verdict is formed: every dimension is registered with
 * {@code WorldDimensions.checkStability(key, stem)}, which reports {@code experimental} unless the stem is
 * <em>vanilla-like</em>:
 *
 * <ul>
 *   <li>the overworld must use exactly {@code minecraft:overworld} or {@code minecraft:overworld_caves} - a
 *       dimension type of our own fails the test on its name alone, whatever it contains;</li>
 *   <li>the Nether must use a multi-noise biome source carrying vanilla's Nether preset - our historical Nether
 *       is a fixed-biome one, so it fails;</li>
 *   <li>the End passes, because it is genuinely the vanilla End.</li>
 * </ul>
 *
 * <p>So the verdict is not evidence about these worlds; it is vanilla asking "is this exactly my overworld?".
 * A historical world is not, by definition: {@code historical_experience:beta_1_7_3_overworld} exists to hold the
 * era's 128-block envelope, so it can never satisfy a check that only accepts the modern dimension type, and the
 * era's Nether is deliberately a single biome. Reporting our own worlds as experimental would mean the player is
 * asked to confirm, on every single load, that they want to open the world this mod exists to open.
 *
 * <p>{@code Complete.lifecycle()} is overridden rather than the prompt itself, because the prompt is one consumer
 * of the value and the crash report is another: a world this mod can describe and load should say so in both.
 *
 * <h2>What this deliberately does not touch</h2>
 *
 * <p>Only worlds this mod recognises are affected, and they are recognised by their own content - a dimension type
 * in this mod's namespace, or a Moderner Beta biome source. An ordinary world uses neither, so the verdict for it
 * is whatever vanilla computed, unchanged. Nothing here reads or writes a world file.
 */
public final class HistoricalWorldStability {
    private HistoricalWorldStability() {
    }

    /**
     * Whether a world whose overworld is described by these two facts is one of ours.
     *
     * @param overworldDimensionType the key of the overworld stem's dimension type, or null when it is not a named
     *                               registry entry
     * @param modernerBetaBiomeSource whether the overworld stem's biome source is Moderner Beta's
     */
    public static boolean isHistoricalWorld(
        ResourceKey<DimensionType> overworldDimensionType,
        boolean modernerBetaBiomeSource
    ) {
        if (overworldDimensionType != null
                && HistoricalExperience.MOD_ID.equals(overworldDimensionType.location().getNamespace())) {
            return true;
        }
        // A Moderner Beta world is a historical world even where its dimension type is vanilla's: that happens for
        // a profile that does not need the era's height, and for a world created with Moderner Beta's own preset.
        return modernerBetaBiomeSource;
    }
}

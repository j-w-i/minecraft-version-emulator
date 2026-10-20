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
package net.historicalexperience.mixin.client;

import net.historicalexperience.client.biome.BiomeConversionScreen;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.HistoricalWorldState;
import net.historicalexperience.world.upgrade.HistoricalBiomeConversionDriver;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Converts a world's stored chunk biomes before the world is allowed to open.
 *
 * <h2>Why here and not later</h2>
 *
 * <p>{@code WorldOpenFlows.openWorld} is the client's single entry point for opening an existing world, and it runs on
 * the client thread before any storage is acquired. Cancelling here means the region files are never handed to a
 * server that would write over the conversion on its next save - which is the ordering the owner asked for: convert,
 * then load.
 *
 * <p>A world that needs nothing takes the ordinary path with no screen and no delay; that includes every ordinary
 * world and every world with no outstanding explicit version change. Completion is read from the world's on-disk
 * conversion record rather than inferred from its biome palettes or process-local counters.
 */
@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsHistoricalBiomeMixin {
    @Shadow
    private net.minecraft.client.Minecraft minecraft;

    @Shadow
    private net.minecraft.world.level.storage.LevelStorageSource levelSource;

    @Inject(method = "openWorld", at = @At("HEAD"), cancellable = true, require = 1)
    private void historicalExperience$convertBiomesBeforeOpening(String levelId, Runnable onFail, CallbackInfo callback) {
        if (HistoricalBiomeConversionDriver.running()) {
            // Already converting: hold this open request rather than queueing a second pass over the same files.
            callback.cancel();
            return;
        }
        java.nio.file.Path worldDirectory;
        try {
            worldDirectory = levelSource.getLevelPath(levelId);
        } catch (RuntimeException notResolvable) {
            return;
        }
        // Loading is not an upgrade action. The driver requires an explicit version change in persisted metadata and
        // checks its on-disk completion revision before showing a screen or touching any chunks.
        HistoricalWorldState state = HistoricalWorldState.loadAt(worldDirectory);
        HistoricalVersion version = state instanceof HistoricalWorldState.Historical historical
            ? HistoricalVersions.byId(historical.metadata().profileId().value()).orElse(null)
            : null;
        if (version == null) {
            // Not a historical world as far as this project is concerned, so its biomes are not ours to move.
            return;
        }
        // Repair before anything else, and on every open, not only when a conversion is owed: a world advanced by an
        // older build carries the finite map's components in its stored generator - the finite 2D noise field among
        // them - which generates the infinite landscape in long ridges. Doing this here means the repair happens
        // before the level reads its generator, and it is a no-op (not a write) for a world already clean.
        try {
            var repaired = net.historicalexperience.world.upgrade.WorldGenSettingsAdvance
                .repairRetainedFiniteComponents(worldDirectory);
            if (!repaired.isEmpty()) {
                net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
                    "Retained finite settings removed from an already-infinite world: {}", repaired);
            }
        } catch (Exception repairFailed) {
            // Never block a world from opening over this: the world still loads, it may just generate as it did.
            net.historicalexperience.HistoricalExperienceMod.LOGGER.warn(
                "Could not check this world for retained finite settings: {}", repairFailed.toString());
        }
        if (!HistoricalBiomeConversionDriver.needsConversion(worldDirectory, version)) {
            return;
        }

        HistoricalBiomeConversionDriver.resetReport();
        if (!HistoricalBiomeConversionDriver.start(worldDirectory, version)) {
            return;
        }
        callback.cancel();
        minecraft.setScreen(new BiomeConversionScreen(levelId, () -> {
            minecraft.setScreen(null);
            // Re-issue the load. The conversion is finished, so needsConversion is false and this call falls
            // straight through to the ordinary path.
            minecraft.createWorldOpenFlows().openWorld(levelId, onFail);
        }, onFail));
    }
}

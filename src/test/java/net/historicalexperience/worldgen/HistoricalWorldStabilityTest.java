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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which worlds may be told they are not experimental, and - more importantly - which may not.
 *
 * <p>The second half is the load-bearing one. Every historical world was showing the client's "Worlds using
 * Experimental Settings are not supported" prompt before it would open, and the fix credits a world with stable
 * generation settings. Crediting the wrong world would silently remove that confirmation from worlds this mod knows
 * nothing about, so these tests pin the negative cases.
 */
class HistoricalWorldStabilityTest {
    private static ResourceKey<DimensionType> dimensionType(String id) {
        return ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.parse(id));
    }

    @Test
    @DisplayName("our own overworld dimension type is recognised, and does not need a Moderner Beta source")
    void ourDimensionTypeIsRecognised() {
        // Read from the shipped resources: 111 of the 112 world presets use this dimension type, which exists to hold
        // the era's 128-block envelope and therefore can never be minecraft:overworld.
        assertTrue(HistoricalWorldStability.isHistoricalWorld(
            dimensionType("historical_experience:beta_1_7_3_overworld"), false));
    }

    @Test
    @DisplayName("a Moderner Beta world with a vanilla dimension type is recognised too")
    void modernerBetaWorldIsRecognised() {
        // This is the profile that does not need the era's height, and Moderner Beta's own preset: vanilla-shaped
        // dimension type over a Moderner Beta generator.
        assertTrue(HistoricalWorldStability.isHistoricalWorld(dimensionType("minecraft:overworld"), true));
    }

    @Test
    @DisplayName("an ordinary world is never credited with stability it was not computed to have")
    void ordinaryWorldIsNotCredited() {
        assertFalse(HistoricalWorldStability.isHistoricalWorld(dimensionType("minecraft:overworld"), false),
            "a plain vanilla overworld must keep vanilla's own verdict");
        assertFalse(HistoricalWorldStability.isHistoricalWorld(dimensionType("minecraft:overworld_caves"), false));
        assertFalse(HistoricalWorldStability.isHistoricalWorld(null, false),
            "an unnamed dimension type with an ordinary generator is not ours either");
        assertFalse(HistoricalWorldStability.isHistoricalWorld(dimensionType("terralith:overworld"), false),
            "another project's dimension type is not this mod's world");
    }

    @Test
    @DisplayName("another namespace is never mistaken for this mod's")
    void otherNamespacesAreNotOurs() {
        assertFalse(HistoricalWorldStability.isHistoricalWorld(
            dimensionType("historical_experience_extra:overworld"), false),
            "a namespace that merely starts with ours must not match: this compares the whole namespace");
        assertFalse(HistoricalWorldStability.isHistoricalWorld(dimensionType("moderner_beta:overworld"), false),
            "Moderner Beta's own dimension types are recognised through the generator, not by name");
    }
}

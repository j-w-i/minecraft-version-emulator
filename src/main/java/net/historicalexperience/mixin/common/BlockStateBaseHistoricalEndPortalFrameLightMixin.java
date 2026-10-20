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
package net.historicalexperience.mixin.common;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * Beta 1.9 Prerelease 3's End portal frame emitted light level 15; Prerelease 4 lowered it to 1.
 *
 * <h2>Source</h2>
 *
 * <p>minecraft.wiki, End Portal Frame history, Java Edition: of Beta 1.9 Prerelease 3, <i>"Gives off a light level
 * of 15."</i>; of Beta 1.9 Prerelease 4, <i>"Now emits light level of 1"</i>. The same pair of rows carries the
 * explosion-drop change already implemented, so both halves of that one prerelease now come from one source.
 *
 * <p>This is a one-build fact, not a boundary, which is why it is gated on equality like the drop rule and why
 * {@code 1} is deliberately NOT written anywhere in this file: the modern value stays whatever the registry declares
 * it to be, and only Pre3 is overridden. Writing the Pre4+ value in as a constant would silently freeze a future
 * change to the block.
 *
 * <h2>Why the target is an inner class, and why it is not {@code Block}</h2>
 *
 * <p>There is no {@code getLightEmission} on {@code Block} or on {@code BlockBehaviour} in 1.21.1 - checked with
 * {@code javap}, not assumed. The value lives in a final field on {@code BlockBehaviour.BlockStateBase} and is read
 * back by {@code BlockStateBase.getLightEmission()}, whose whole body is {@code getfield lightEmission; ireturn}.
 * So the only place a light value can be answered from is the block STATE, and a mixin targeting {@code Block} could
 * not name the method at all. This is the project's first mixin whose target is a nested class.
 *
 * <h2>Why the block is tested before the version</h2>
 *
 * <p>{@code getLightEmission} is on the light engine's read path and is asked for essentially every block state that
 * is queried, so the cheap test must come first. {@code ==} against the frame is a reference comparison that answers
 * false for the overwhelming majority of queries, while the gate behind it reads a volatile session field. Putting
 * the gate first would pay that read on every light query in the world to answer a question that is false for every
 * block but one.
 *
 * <h2>Scope</h2>
 *
 * <p>A shared mixin: light is computed by the server and sent to clients.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseHistoricalEndPortalFrameLightMixin {
    @ModifyReturnValue(method = "getLightEmission", at = @At("RETURN"))
    private int historicalExperience$endPortalFrameLight(int light) {
        if (!((Object) this instanceof BlockBehaviour.BlockStateBase state)) {
            return light;
        }
        if (state.getBlock() != Blocks.END_PORTAL_FRAME) {
            return light;
        }
        return Features.active(Feature.END_PORTAL_FRAME_LIGHT_15) ? 15 : light;
    }
}

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

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * Beta 1.9 Prerelease 3's End portal frames could be blown up, which Pre4 fixed.
 *
 * <h2>Source</h2>
 *
 * <p>minecraft.wiki, End Portal Frame, History / Java Edition. Of Beta 1.9 Prerelease 3: <i>"It can drop as an
 * item due to explosions."</i> Of Beta 1.9 Prerelease 4: <i>"The End portal can no longer be broken by
 * explosions."</i> The owner named this bug specifically and called it very important.
 *
 * <h2>The value, and why it is not a guess dressed up as one</h2>
 *
 * <p>The wiki records the BEHAVIOUR and not a number, so this uses the block's ordinary (unset) resistance. What
 * matters is the threshold: TNT destroys blocks whose blast resistance is at or below roughly twenty, which is why
 * end stone at 9 is destroyed and obsidian at 1200 is not, and the modern End portal frame sits at 3,600,000 —
 * astronomically clear of it. Any value at or below that threshold reproduces the documented behaviour exactly,
 * so the precise figure is not load-bearing. The modern value is deliberately NOT scaled down by some invented
 * factor.
 *
 * <p>The drop half lives in {@code BlockBehaviourHistoricalDropMixin}, next to the other historical drop rules,
 * because {@code getDrops} is declared on {@code BlockBehaviour} while this method is on {@code Block}.
 *
 * <p>Server-side, and a shared mixin: explosions are resolved by the server.
 */
@Mixin(Block.class)
public abstract class BlockHistoricalEndPortalFrameMixin {
    /** At or below this, TNT destroys the block; the modern frame is 3,600,000. */
    private static final float DESTROYABLE_BY_EXPLOSION = 0.0F;

    @ModifyReturnValue(method = "getExplosionResistance", at = @At("RETURN"))
    private float historicalExperience$endPortalFrameExplodes(float resistance) {
        if (!((Object) this instanceof Block block) || !block.equals(Blocks.END_PORTAL_FRAME)) {
            return resistance;
        }
        if (!Features.active(Feature.END_PORTAL_FRAME_EXPLODES)) {
            return resistance;
        }
        return DESTROYABLE_BY_EXPLOSION;
    }
}
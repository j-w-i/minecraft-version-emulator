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

import net.historicalexperience.gameplay.HistoricalDragonFight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The parts of the Release 1.9 fight that live in {@link EndDragonFight} itself.
 *
 * <ul>
 *   <li><b>No End gateway.</b> {@code setDragonKilled} calls {@code spawnNewGateway()} on every death, which builds a
 *       bedrock frame at y=75, 96 blocks out, and (on first use) a second one on the outer islands. Gateways arrived in
 *       Release 1.9 (15w31a) and are the way onto islands that did not exist yet. Skipping that one call is the whole
 *       change; the portal, the egg and the experience are untouched.</li>
 *   <li><b>No re-summoning.</b> Placing four End crystals on the portal to bring the dragon back is Release 1.9 as
 *       well (15w44a). {@code tryRespawn} is what that placement calls, so it does nothing here.</li>
 * </ul>
 *
 * <p>Both only act when {@link HistoricalDragonFight#originalFight(ServerLevel)} says so, so an ordinary world keeps
 * the modern fight exactly.
 */
@Mixin(EndDragonFight.class)
public abstract class EndDragonFightHistoricalMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "spawnNewGateway()V", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$noGateway(CallbackInfo callback) {
        if (HistoricalDragonFight.originalFight(this.level)) {
            callback.cancel();
        }
    }

    @Shadow @Final private net.minecraft.server.level.ServerBossEvent dragonEvent;

    /**
     * Captions the dragon's bar "Boss health" for the versions that did. The name is part of the packet the client is
     * sent, so setting it here reaches the screen; it is compared first so nothing is rebroadcast every tick.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void historicalExperience$bossHealthCaption(CallbackInfo callback) {
        if (this.level.getServer() == null) {
            return;
        }
        String caption = HistoricalDragonFight.bossBarCaption(
            net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(
                this.level.getServer().overworld()));
        if (caption == null) {
            return;
        }
        net.minecraft.network.chat.Component wanted = net.minecraft.network.chat.Component.literal(caption);
        if (!wanted.equals(this.dragonEvent.getName())) {
            this.dragonEvent.setName(wanted);
        }
    }

    @Inject(method = "tryRespawn", at = @At("HEAD"), cancellable = true)
    private void historicalExperience$noCrystalRespawn(CallbackInfo callback) {
        if (HistoricalDragonFight.originalFight(this.level)) {
            callback.cancel();
        }
    }
}
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
package net.historicalexperience.server;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.historicalexperience.gameplay.HistoricalPortalSpawn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Carries "the player pressed F4" to the server, which is where the portal is built.
 *
 * <h2>Why the client asks rather than builds</h2>
 *
 * <p>The server owns world state in this project, and a client that wrote blocks itself would be able to place them
 * anywhere with no check. The payload therefore carries NOTHING - not a position, not an axis, not the version - and
 * the server decides everything from the player it arrived from. A request that cannot name where it wants the portal
 * cannot be used to ask for one somewhere else.
 *
 * <p>The server also re-checks the version rather than trusting that the client only sends when the key applies, so a
 * modified client on a Beta 1.7.3 world gets nothing.
 */
public final class PortalSpawnRequest {
    private PortalSpawnRequest() {
    }

    /** The request: no fields at all, on purpose. */
    public record Payload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath("historical_experience", "f4_portal"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
            StreamCodec.unit(new Payload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Registers the payload and the handler. Called once from the server initialiser. */
    public static void register() {
        PayloadTypeRegistry.playC2S().register(Payload.TYPE, Payload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) ->
            context.player().server.execute(() -> {
                var player = context.player();
                // The version gate is re-run against the level the player is actually in, so the client's copy of
                // the rule is never the authority.
                HistoricalPortalSpawn.spawnNear(player.serverLevel(), player);
            }));
    }
}

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
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.historicalexperience.gameplay.HistoricalProfileLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.historicalexperience.version.ActiveVersion;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Tells a joining client which historical version it is in, so client-side presentation can follow.
 *
 * <h2>Why a packet, and not the client working it out</h2>
 *
 * <p>In single-player the client can ask the integrated server directly, and
 * {@code HistoricalClientRules} does exactly that. On a multiplayer server it cannot: the client has no
 * world metadata, no generator and no server files, so every client-side rule would be single-player only.
 * Without this push, sprinting would be denied on a single-player world and work normally on a server,
 * which is the kind of difference nobody would think to test.
 *
 * <h2>What is sent</h2>
 *
 * <p>Only the version identifier, and only to that player. It is not a secret, it is not authoritative -
 * the server still does all enforcement - and the client treats a missing push as "unknown" rather than
 * assuming anything.
 */
public final class HistoricalVersionSync {
    private HistoricalVersionSync() {
    }

    /** The payload: one identifier string. */
    public record ActiveVersionPayload(String versionId) implements CustomPacketPayload {
        // The explicit constructor, not createType(String): that helper takes a PATH and prefixes the
        // minecraft namespace, so "historical_experience:active_version" became
        // "minecraft:historical_experience:active_version" and the mod failed to initialise.
        public static final CustomPacketPayload.Type<ActiveVersionPayload> TYPE =
            new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath("historical_experience", "active_version"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ActiveVersionPayload> CODEC =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ActiveVersionPayload::versionId,
                ActiveVersionPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Registers the payload and the join listener. Called once from the server initialiser. */
    public static void register() {
        HistoricalSnapshotRuntime.register();
        // clientboundPlay(), not the older playS2C(): the registry methods were renamed.
        PayloadTypeRegistry.playS2C().register(ActiveVersionPayload.TYPE,
            ActiveVersionPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String versionId = HistoricalProfileLookup.profileIdForLevel(server.overworld());
            // The server owns enforcement, so the session holder is set here too: gates that cannot see a
            // level (an item's attack damage, for one) read it instead.
            ActiveVersion.set(versionId);
            // Recompute the attributes NOW that the version is known. The equipment pass that applies a held
            // item's attack damage already ran, before this, with the modern values; without this the era's
            // damage never takes effect until the player happens to switch items.
            net.historicalexperience.gameplay.HistoricalEquipmentRefresh.refresh(handler.player);
            if (versionId == null) {
                // Not a historical world, so nothing is claimed and the client keeps modern presentation.
                return;
            }
            ServerPlayNetworking.send(handler.player, new ActiveVersionPayload(HistoricalSnapshotRuntime.selectionId(server, versionId)));
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ActiveVersion.clear());
    }
}

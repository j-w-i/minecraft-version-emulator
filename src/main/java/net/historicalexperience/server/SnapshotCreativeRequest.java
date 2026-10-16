/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.server;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.historicalexperience.gameplay.HistoricalCreativeRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;

/** Empty B-key intent. The sender cannot name a player, version, game mode or permission level. */
public final class SnapshotCreativeRequest {
    private SnapshotCreativeRequest() { }
    public record Payload() implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("historical_experience", "snapshot_b_creative"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.unit(new Payload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void register() {
        PayloadTypeRegistry.playC2S().register(Payload.TYPE, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, context) -> context.server().execute(() -> {
            var player = context.player();
            if (!HistoricalCreativeRules.hasCreativeShortcut(HistoricalSnapshotRuntime.activePatchId(player.serverLevel()))) return;
            // Compatibility safety policy, not an assertion about unverified old multiplayer code:
            // no cheats required for the private integrated-world owner; never a remote privilege escalation.
            boolean privateOwner = context.server().isSingleplayerOwner(player.getGameProfile()) && !context.server().isPublished();
            if (!HistoricalCreativeRules.allowsCreativeShortcut(HistoricalSnapshotRuntime.activePatchId(player.serverLevel()), privateOwner)) {
                player.displayClientMessage(Component.literal("The 11w49a B shortcut is limited to the owner of a private single-player world."), true);
                return;
            }
            // Owner-requested toggle: each accepted key edge switches between Survival and Creative.
            player.setGameMode(player.gameMode.getGameModeForPlayer() == GameType.CREATIVE
                ? GameType.SURVIVAL : GameType.CREATIVE);
        }));
    }
}

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
import net.historicalexperience.gameplay.HistoricalActiveSession;
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
        // clientboundPlay(), not the older playS2C(): the registry methods were renamed.
        PayloadTypeRegistry.playS2C().register(ActiveVersionPayload.TYPE,
            ActiveVersionPayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String versionId = HistoricalProfileLookup.profileIdForLevel(handler.player.serverLevel());
            // The server owns enforcement, so the session holder is set here too: gates that cannot see a
            // level (an item's attack damage, for one) read it instead.
            HistoricalActiveSession.set(versionId);
            // Recompute the attributes NOW that the version is known. The equipment pass that applies a held
            // item's attack damage already ran, before this, with the modern values; without this the era's
            // damage never takes effect until the player happens to switch items.
            net.historicalexperience.gameplay.HistoricalEquipmentRefresh.refresh(handler.player);
            if (versionId == null) {
                // Not a historical world, so nothing is claimed and the client keeps modern presentation.
                return;
            }
            ServerPlayNetworking.send(handler.player, new ActiveVersionPayload(versionId));
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> HistoricalActiveSession.clear());
    }
}

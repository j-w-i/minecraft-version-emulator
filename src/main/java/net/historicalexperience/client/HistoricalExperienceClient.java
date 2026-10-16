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
package net.historicalexperience.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.historicalexperience.server.HistoricalVersionSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point: receives the version the server pushes on join.
 *
 * <p>Registered as its own entry point rather than folded into the replay probe, because the two do
 * unrelated jobs: this one makes client-side presentation follow the world's version, and the probe is a
 * verification tool that only runs when a system property names it.
 *
 * <p>The pushed version is forgotten when the client disconnects, so joining a server cannot leave rules
 * behind that then apply to the next world the player opens.
 */
public final class HistoricalExperienceClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience_client");

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            HistoricalVersionSync.ActiveVersionPayload.TYPE,
            (payload, context) -> {
                // Applied on the client thread: the payload handler runs on the network thread, and the
                // value is read from rendering, so it must not be mutated from there without care. It is
                // a volatile single reference, which is why a plain assignment is enough.
                HistoricalClientRules.applySyncedVersion(payload.versionId());
                reportClientState("the server pushed " + payload.versionId());
                // And on this side too: the client predicts its own attack damage and draws the attack
                // indicator from its own attribute map.
                net.historicalexperience.gameplay.HistoricalEquipmentRefresh.refresh(context.player());
                LOGGER.info("the server reports this client is in {}", payload.versionId());
            });

        HistoricalClientDiagnostics.register();
        // The version name in the top-left corner, for the versions that drew it.
        net.historicalexperience.client.hud.HistoricalVersionOverlay.register();

        // The historical version selector, reachable while creating a world and, separately, while managing one.
        net.historicalexperience.client.version.CreateWorldHistoricalVersion.register();
        net.historicalexperience.client.version.ManageWorldHistoricalVersion.register();
        // Gated on a system property; inert unless a verification run asks for it.
        net.historicalexperience.client.version.ScreenProbe.register();
        NtVisualProbe.register();
        ClientMixinCheck.registerIfRequested();
        // Alpha v1.2.2a's F4: the raw key, as that client read it, rather than a rebindable control.
        F4PortalKey.register();
        NtStarsProbe.register();

        // The old lighting engine follows the sky: it re-lights a few sections a tick as the time of day moves, so it
        // needs a heartbeat. Cheap when no old mode is active - the mode is cached and the method returns early.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(
            net.historicalexperience.client.lighting.HistoricalOldLighting::onTick);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(
            net.historicalexperience.client.lighting.HistoricalStars::onTick);

        // Historical textures: keep the resource layer in step with the world (one comparison covers joining, leaving,
        // a version push, a committed upgrade and the config toggle), and show the first-run setup once.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(
            net.historicalexperience.client.textures.HistoricalTextureService::tick);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(
            net.historicalexperience.client.textures.TextureFirstRun::tick);
        net.historicalexperience.client.textures.TexturesProbe.register();

        // A client that hangs on leaving a world writes nothing at all, which is how the owner's freeze left no
        // evidence. This reports the stuck stacks once, if that ever happens again.
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(
            net.historicalexperience.client.LoadingHangWatchdog::tick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            HistoricalClientRules.clearSyncedVersion();
            // The next world's sky light is unrelated to this one's.
            net.historicalexperience.client.lighting.HistoricalOldLighting.reset();
            // A held-jump latch from the world just left must not suppress the first jump in the next one.
            net.historicalexperience.client.input.HistoricalJumpInput.clear();
        });
    }

    /**
     * Logs what THIS client believes, so a client run leaves evidence rather than requiring a screen.
     *
     * <p>Written because the failure mode that cost the most was silent: a rule answering true on the server and
     * false on the client, because a client's player level is a ClientLevel and any level-based question returns
     * null there. The session line is the decisive one - if it reads NONE, every client-side rule is inert no
     * matter how correct its mixin is.
     */
    private static void reportClientState(String cause) {
        String session = net.historicalexperience.gameplay.HistoricalActiveSession.current();

        LOGGER.info("[hex] client state after {}: session={} hungerBarHidden={} experienceBarHidden={}",
            cause,
            session == null ? "<NONE - client-side rules are inert>" : session,
            net.historicalexperience.client.hud.HudHelper.hideHungerBar(),
            net.historicalexperience.client.hud.HudHelper.hideExperienceBar());
    }
}

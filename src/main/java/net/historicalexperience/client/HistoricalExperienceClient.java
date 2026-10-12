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

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            HistoricalClientRules.clearSyncedVersion());
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

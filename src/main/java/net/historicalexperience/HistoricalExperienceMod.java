package net.historicalexperience;

import net.fabricmc.api.ModInitializer;
import net.historicalexperience.server.HistoricalCommand;
import net.historicalexperience.gameplay.HistoricalSwordBlockingEvents;
import net.historicalexperience.server.HistoricalServerLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.historicalexperience.server.HistoricalVersionSync;

/**
 * Mod entrypoint.
 *
 * <p>This slice is server authoritative. What is registered here is the server lifecycle hook that
 * decides whether a world is historical and enforces the persisted metadata, plus the operator command
 * that reports on and changes a world's recorded generation recipe. No client behaviour is registered,
 * because the client must remain presentation and prediction only.
 */
public final class HistoricalExperienceMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("historical_experience");

    @Override
    public void onInitialize() {
        LOGGER.info("Historical Experience {} initialising (server-authoritative slice)",
            HistoricalExperience.MOD_ID);
        HistoricalServerLifecycle.register();
        net.historicalexperience.server.GenerationVerification.registerIfRequested();
        // The historical village's structure type and piece type. Registered here rather than through a
        // mixin or a loader-specific event bus: both are ordinary 1.21.1 registries, still open during a
        // Fabric mod initialiser. The piece type matters in particular - it is the id saved into every
        // chunk's structure data, so a world with a village resolves its pieces through this registration.
        // See HistoricalVillageTypes.
        net.historicalexperience.worldgen.HistoricalVillageTypes.register();
        net.historicalexperience.server.VillageVerification.registerIfRequested();
        HistoricalCommand.register();
        // Tells a joining client which version it is in. Without this, every client-side rule would work
        // in single-player and silently do nothing on a server, because the client has no world metadata
        // of its own to read.
        HistoricalVersionSync.register();
        // The Beta 1.8-1.9 sword-blocking stance. Both halves are in place now: this handler raises the
        // sword on right-click, and LivingEntityHistoricalSwordBlockingMixin applies the 50% reduction. The
        // version gate lives in HistoricalSwordBlocking.canBlock, so a pre-Beta-1.8 world has no stance and
        // cannot block even if a client asks.
        HistoricalSwordBlockingEvents.register();
    }
}

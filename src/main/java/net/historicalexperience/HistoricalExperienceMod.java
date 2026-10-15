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
        net.historicalexperience.server.DragonFightVerification.registerIfRequested();
        net.historicalexperience.server.EnchantingVerification.registerIfRequested();
        HistoricalCommand.register();
        // Tells a joining client which version it is in. Without this, every client-side rule would work
        // in single-player and silently do nothing on a server, because the client has no world metadata
        // of its own to read.
        HistoricalVersionSync.register();
        // Alpha v1.2.2a's F4 key. The client sends an empty request and the server builds the portal, so the
        // placement stays authoritative and a modified client cannot ask for one somewhere else.
        net.historicalexperience.server.PortalSpawnRequest.register();
        // The Beta 1.8-1.9 sword-blocking stance. Both halves are in place now: this handler raises the
        // sword on right-click, and LivingEntityHistoricalSwordBlockingMixin applies the 50% reduction. The
        // version gate lives in HistoricalSwordBlocking.canBlock, so a pre-Beta-1.8 world has no stance and
        // cannot block even if a client asks.
        HistoricalSwordBlockingEvents.register();
        // Axe stripping (1.13) and shovel pathing (1.9) are both later than every declared version, so no
        // historical world may perform either. Registered here rather than in a mixin because both are ordinary
        // right-click interactions and the loader's own callback is the hook for them.
        net.historicalexperience.gameplay.HistoricalToolInteractionEvents.register();
        // A pre-Beta-1.8 bow fires on use rather than after a charge (INSTANT_BOW / ARROW_SPEED). Separate from the
        // block rule above because it answers a different Fabric callback - using an ITEM, not using a block.
        net.historicalexperience.gameplay.HistoricalToolInteractionEvents.registerInstantBow();
        // Two things older versions did that the modern game does not: a hoe throwing seeds off grass (gone in
        // Beta 1.6 Test Build 3) and a punch priming TNT (gone in Beta 1.7).
        net.historicalexperience.gameplay.HistoricalFarmingInteractionEvents.register();
        // Doors, levers and buttons were operated with a LEFT click until Release 1.4.2.
        net.historicalexperience.gameplay.HistoricalLeftClickEvents.register();
    }
}

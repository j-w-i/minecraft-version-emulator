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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.historicalexperience.client.hud.HudHelper;
import net.historicalexperience.gameplay.HistoricalActiveSession;
import net.historicalexperience.gameplay.HistoricalGameplayTweaks;
import net.historicalexperience.gameplay.HistoricalSwordBlocking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Reports what the CLIENT believes about the active version, and what its client-visible rules answer.
 *
 * <p>Written after a client-verification harness stopped opening worlds: rather than infer, this lets the owner
 * ask the client directly. The distinction it exists to expose is the one that broke sprinting and sword blocking -
 * a rule that answers true on the server and false on the client, because a client's player level is a
 * {@code ClientLevel} and any level-based question returns null there.
 */
public final class HistoricalClientDiagnostics {
    private HistoricalClientDiagnostics() {
    }

    /** Registers the client command. Called from the client initialiser. */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) ->
            dispatcher.register(describe()));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> describe() {
        return net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("hexclient")
            .executes(context -> {
                FabricClientCommandSource source = context.getSource();
                Minecraft minecraft = Minecraft.getInstance();

                String session = HistoricalActiveSession.current();
                source.sendFeedback(Component.literal("[hex] client session: "
                    + (session == null ? "<NONE - client-side rules are inert in this process>" : session)));

                if (session != null) {
                    source.sendFeedback(Component.literal("[hex]   hunger bar hidden: "
                        + HudHelper.hideHungerBar() + ", experience bar hidden: "
                        + HudHelper.hideExperienceBar()));
                    source.sendFeedback(Component.literal("[hex]   ore drops as blocks (server rule, answered here "
                        + "from the same session): " + HistoricalGameplayTweaks.oreDropsAsBlocks()));
                    source.sendFeedback(Component.literal("[hex]   sword block active for this player: "
                        + (minecraft.player != null && HistoricalSwordBlocking.isBlocking(minecraft.player))));
                    // The gates that were reported as not working. Printing them turns "it still happens" into a
                    // definitive answer: if these read as refusing while the behaviour still occurs, the hook is in
                    // the wrong place; if they read as allowing, the SESSION is the problem, not the hook.
                    source.sendFeedback(Component.literal("[hex]   offhand refused (this version has none): "
                        + HistoricalGameplayTweaks.offhandUnavailable(session)));
                    source.sendFeedback(Component.literal("[hex]   sneaking refused (arrives Alpha 1.1.0): "
                        + !HistoricalGameplayTweaks.sneakingAvailable(session)));
                    source.sendFeedback(Component.literal("[hex]   crouching right now, per this client: "
                        + (minecraft.player != null && minecraft.player.isCrouching())));
                    // The offhand slot still DRAWS even though the gate above answers true. These lines report what
                    // the menu's slots actually are, so the difference between "the hook is not reached" and "the
                    // hook's condition is false at render time" is visible instead of guessed at.
                    if (minecraft.player != null) {
                        for (var slot : minecraft.player.inventoryMenu.slots) {
                            if (slot.getContainerSlot() == 40
                                    && slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                                source.sendFeedback(Component.literal("[hex]   offhand slot: containerSlot="
                                    + slot.getContainerSlot() + " container=" + slot.container.getClass().getSimpleName()
                                    + " slotClass=" + slot.getClass().getName()
                                    + " isActive=" + slot.isActive()
                                    + " x=" + slot.x + " y=" + slot.y
                                    + (slot.hasItem() ? " holds=" + slot.getItem() : " empty")));
                            }
                        }
                    }
                }

                source.sendFeedback(Component.literal("[hex]   armour damage tint: registered (a client mixin; "
                    + "no runtime flag - it tints whenever hurtTime or deathTime is above zero)"));

                return 1;
            });
    }
}

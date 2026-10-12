package net.historicalexperience.client;

import com.mojang.brigadier.CommandDispatcher;
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
                }

                source.sendFeedback(Component.literal("[hex]   armour damage tint: registered (a client mixin; "
                    + "no runtime flag - it tints whenever hurtTime or deathTime is above zero)"));

                return 1;
            });
    }
}

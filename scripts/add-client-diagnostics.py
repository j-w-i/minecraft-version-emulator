"""A client-side diagnostic command, so client rules can be checked without client automation.

The in-client verification harness stopped reaching a world in round 83 and seven hypotheses failed to explain
it. Rather than keep spending rounds on that, this puts the client's own state behind one command the owner can
run: the client half of the active session, and whether each client-visible rule answers true on THIS side.

It exists because the failure that cost the most this session was invisible: sprinting and sword blocking both
answered true on the server and false on the client, because a client's level is a ClientLevel. A command that
prints the client's own answers turns that class of bug into one line of output.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

(root / 'src/main/java/net/historicalexperience/client/HistoricalClientDiagnostics.java').write_text(
'''package net.historicalexperience.client;

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
        return net.minecraft.commands.Commands.literal("hexclient")
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
''', encoding='utf-8')
print('wrote the client diagnostics command')

client = root / 'src/main/java/net/historicalexperience/client/HistoricalExperienceClient.java'
text = client.read_text(encoding='utf-8')
old = '''        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->'''
new = '''        HistoricalClientDiagnostics.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->'''
assert old in text, 'client init anchor not found'
client.write_text(text.replace(old, new, 1), encoding='utf-8')
print('registered from the client initialiser')

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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.historicalexperience.HistoricalExperienceMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * The operator's entry point to the historical version machinery.
 *
 * <p>Deliberately thin. Decisions live in {@link HistoricalUpgradeService} and the explicit finite-edge repair
 * service, tested against real world directories without a server; this class parses arguments and renders text.
 * That split is what makes the upgrade reachable from a command, the runtime probe and any future
 * interface without three copies of the rules drifting apart.
 *
 * <p>Commands require permission level 2, because every one of them either reports on or changes a
 * world's recorded generation recipe. That is an operator action, not a player one.
 */
public final class HistoricalCommand {
    /** The literal a player types. Short because the alternative is tedious in a chat box. */
    public static final String ROOT = "hex";

    private HistoricalCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            register(dispatcher));
    }

    /** Registers the tree. Public so a test can assert its shape without a server. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Each subcommand is built as its own value rather than as one nested expression. The nested
        // form is where a bracket goes missing, and a command tree is not the place to be counting them.
        LiteralCommandNode<CommandSourceStack> status = Commands.literal("status")
            .executes(HistoricalCommand::status)
            .build();

        LiteralCommandNode<CommandSourceStack> versions = Commands.literal("versions")
            .executes(context -> {
                reply(context, HistoricalUpgradeService.declaredVersions());
                return 1;
            })
            .build();

        LiteralCommandNode<CommandSourceStack> finiteRepair = Commands.literal("repair-finite-edge")
            .executes(context -> {
                try {
                    reply(context, net.historicalexperience.world.upgrade.FiniteEdgeRepair.stage(
                        worldDirectory(context.getSource()), context.getSource().getTextName(), System.currentTimeMillis()));
                    return 1;
                } catch (java.io.IOException failure) {
                    reply(context, "REFUSED: " + failure.getMessage());
                    return 0;
                }
            }).build();

        LiteralCommandNode<CommandSourceStack> recover = Commands.literal("recover")
            .executes(context -> report(context, HistoricalUpgradeService.recover(
                worldDirectory(context.getSource()))))
            .build();

        RequiredArgumentBuilder<CommandSourceStack, String> versionArgument =
            // greedyString(), not word()/string(): an UNQUOTED version id contains a colon
            // (historical_experience:release_1_2_5), and word() cannot contain one, so the operator-facing
            // upgrade was unusable for its own ids. Found by driving the command over RCON rather than by
            // reading it: "Expected whitespace to end one argument, but found trailing data ... :release_1_2_5".
            // Both forms are accepted, since typing the namespace every time is tedious:
            //   /hex upgrade release_1_2_5
            //   /hex upgrade historical_experience:release_1_2_5
            Commands.argument("version", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    // Suggestions come from the registry rather than declaredVersions(), which returns a
                    // pre-formatted sentence for the operator rather than a list.
                    for (net.historicalexperience.version.HistoricalVersion version
                        : net.historicalexperience.version.HistoricalVersions.all()) {
                        builder.suggest(version.id());
                        builder.suggest(version.id().substring(version.id().indexOf(':') + 1));
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    String target = StringArgumentType.getString(context, "version");
                    reply(context, "Staging an upgrade to " + target
                        + "; the generation change takes effect after the server stops.");
                    return report(context, HistoricalUpgradeService.upgrade(
                        worldDirectory(context.getSource()),
                        target,
                        context.getSource().getTextName(),
                        System.currentTimeMillis()));
                });

        LiteralCommandNode<CommandSourceStack> upgrade = Commands.literal("upgrade")
            .then(versionArgument)
            .build();

        dispatcher.register(Commands.literal(ROOT)
            // 1.21.1 predates the permission-set API; "permission level 2" is what a game master has, and
            // the numeric level is the form this version understands. The 26.3 build used
            // Permissions.COMMANDS_GAMEMASTER, which is the same privilege under a name that did not exist
            // yet. Kept as a level 2 literal rather than a constant so that the intent is readable here.
            .requires(source -> source.hasPermission(2))
            .executes(HistoricalCommand::status)
            .then(status)
            .then(versions)
            .then(recover)
            .then(finiteRepair)
            .then(upgrade));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        return report(context, HistoricalUpgradeService.describe(worldDirectory(context.getSource())));
    }

    private static int report(
        CommandContext<CommandSourceStack> context,
        HistoricalUpgradeService.Report report
    ) {
        reply(context, (report.ok() ? "" : "REFUSED: ") + report.summary());
        for (String detail : report.details()) {
            reply(context, "  " + detail);
        }
        return report.ok() ? 1 : 0;
    }

    private static void reply(CommandContext<CommandSourceStack> context, String message) {
        HistoricalExperienceMod.LOGGER.info("[{}] {}", ROOT, message);
        context.getSource().sendSuccess(() -> Component.literal("[" + ROOT + "] " + message), false);
    }

    private static java.nio.file.Path worldDirectory(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        return server.getWorldPath(LevelResource.ROOT);
    }
}

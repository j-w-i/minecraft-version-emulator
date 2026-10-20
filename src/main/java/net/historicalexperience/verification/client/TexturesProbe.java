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
package net.historicalexperience.verification.client;

import net.historicalexperience.client.colour.HistoricalColormaps;
import net.historicalexperience.client.config.HistoricalConfigScreen;
import net.historicalexperience.client.textures.HistoricalTextureConfig;
import net.historicalexperience.client.textures.HistoricalTextureService;
import net.historicalexperience.client.textures.TextureSetupScreen;
import net.historicalexperience.client.textures.TextureSource;
import net.historicalexperience.client.textures.TexturesConfigScreen;
import net.historicalexperience.mixin.client.ContainerScreenAccess;
import net.historicalexperience.server.HistoricalUpgradeService;
import net.historicalexperience.version.Era;

import net.historicalexperience.verification.client.TexturesProbe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.historicalexperience.client.HistoricalClientRules;
import net.minecraft.client.Minecraft;
import net.historicalexperience.client.tooltip.HistoricalTooltips;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;

/**
 * Opt-in verification that reads what the game actually resolves, never what the configuration claims.
 *
 * <p>For each checked resource it asks the live {@code ResourceManager} which pack served it and records a SHA-256 of
 * the bytes. Screenshots are taken as corroboration only: precedence is proven by the pack id and the hash.
 *
 * <h2>Modes (system properties)</h2>
 *
 * <ul>
 *   <li>{@code texturesProbe}: in-world. Waits for the service to build its layer, observes ENFORCED, switches
 *       enforcement off (the same call the config toggle makes) and observes DISABLED, then switches it back on and
 *       observes RE-ENABLED. Also checks {@code options.txt} is byte-identical throughout.</li>
 *   <li>{@code texturesProbe.titleOnly}: first-run flow from the title screen, optionally with the real download.</li>
 * </ul>
 *
 * <p>It never edits a world, a pack or {@code options.txt}; the only state it touches is the mod's own enforcement
 * switch, which it restores.
 */
public final class TexturesProbe {
    private static final List<String> RESULTS = new ArrayList<>();
    private static final String[] CHECKED = {
        "minecraft:textures/block/cobblestone.png",
        "minecraft:textures/gui/sprites/hud/experience_bar_background.png",
        "minecraft:textures/gui/sprites/hud/experience_bar_progress.png",
        "minecraft:textures/block/grass_block_top.png",
        "minecraft:textures/block/oak_leaves.png",
        "minecraft:textures/block/stone.png",
        "minecraft:textures/block/dirt.png",
    };

    private static int ticks;
    private static int stage;
    private static boolean savedEnabled;
    private static String optionsBefore;

    private TexturesProbe() {
    }

    public static void register() {
        if (Boolean.getBoolean("historical_experience.texturesProbe")) {
            ClientTickEvents.END_CLIENT_TICK.register(TexturesProbe::tick);
        }
    }

    private static void tick(Minecraft client) {
        try {
            if (client.getOverlay() != null) {
                return;
            }
            ticks++;
            if (Boolean.getBoolean("historical_experience.texturesProbe.titleOnly")) {
                titleOnly(client);
                return;
            }
            if (System.getProperty("historical_experience.texturesProbe.tooltipWorlds") != null) {
                tooltipShots(client);
                return;
            }
            if (Boolean.getBoolean("historical_experience.texturesProbe.modMenu")) {
                modMenuCheck(client);
                return;
            }
            if (Boolean.getBoolean("historical_experience.texturesProbe.configShots")) {
                configShots(client);
                return;
            }
            String cycle = System.getProperty("historical_experience.texturesProbe.cycle");
            if (cycle != null) {
                cycleWorlds(client, cycle.split(","));
                return;
            }
            if (Boolean.getBoolean("historical_experience.texturesProbe.ordinaryWorld")) {
                createOrdinaryWorld(client);
            }
            if (client.level == null || client.player == null || (stage == 0 && ticks < 200)) {
                return;
            }
            if (stage == 0) {
                stage = 1;
                // Timeouts below count from the moment the world is joined, not from launch: a slow world load must
                // not use up the time the reconcile needs.
                ticks = 0;
                String fixture = System.getProperty("historical_experience.texturesProbe.userPack");
                if (fixture != null) {
                    // Select a synthetic "user pack" in memory only. addPack changes the repository's selection for
                    // this process; nothing here calls Options.updateResourcePacks, so options.txt is never written,
                    // which the probe verifies below.
                    var repository = client.getResourcePackRepository();
                    repository.reload();
                    boolean added = repository.addPack(fixture);
                    note("user pack '" + fixture + "' selected in memory=" + added
                        + " selected=" + repository.getSelectedIds());
                    client.reloadResourcePacks();
                    ticks = 0;
                    return;
                }
            }
            if (stage == 1 && Boolean.getBoolean("historical_experience.texturesProbe.ordinaryWorld")) {
                // An ordinary world must get no layer and no reload. Give the reconcile generous time to (wrongly) act,
                // then observe: every resource must still be served by vanilla (or the user's packs), never our layer.
                // The probe must measure the world it created, not whichever world happens to be open: a person
                // clicking into another world in the dev window mid-run would otherwise be recorded as a result.
                var server = client.getSingleplayerServer();
                String openWorld = server == null ? "?" : server.getWorldData().getLevelName();
                if (!openWorld.equals(ordinaryName)) {
                    note("FAIL measuring the wrong world: open='" + openWorld + "' expected='" + ordinaryName
                        + "' (was another world opened in this window during the run?)");
                    finish(client);
                    return;
                }
                if (ticks < 400) {
                    return;
                }
                note("measured world: " + openWorld);
                optionsBefore = readOptions(client);
                note("version=" + describeVersion());
                note("desired=" + HistoricalTextureService.desiredSignature()
                    + " applied=" + HistoricalTextureService.appliedSignature());
                note("reloads=" + HistoricalTextureService.reloadCount());
                note("colormaps desired=" + net.historicalexperience.client.colour.HistoricalColormaps
                    .desiredEraSignature() + " applied=" + net.historicalexperience.client.colour.HistoricalColormaps
                    .appliedEra());
                observe(client, "ORDINARY");
                shot(client, "tex-probe-ordinary.png");
                boolean untouched = "none".equals(HistoricalTextureService.appliedSignature())
                    && HistoricalTextureService.activeLayer() == null;
                note("ordinary world untouched (no layer built): " + untouched);
                note("DONE");
                finish(client);
                return;
            }
            if (stage == 1) {
                // Observe only once the service's own reconcile has built the layer: sampling the moment the world
                // opens would measure the state before the reload this feature exists to trigger.
                String desired = HistoricalTextureService.desiredSignature();
                if (!desired.equals(HistoricalTextureService.appliedSignature())
                    || "none".equals(HistoricalTextureService.appliedSignature())) {
                    if (ticks > 1200) {
                        note("FAIL layer never applied: desired=" + desired
                            + " applied=" + HistoricalTextureService.appliedSignature());
                        finish(client);
                    }
                    return;
                }
                savedEnabled = HistoricalTextureConfig.enabled();
                optionsBefore = readOptions(client);
                note("version=" + describeVersion());
                note("scan=" + HistoricalTextureService.scan().usable().keySet());
                note("selectedUserPacks=" + client.getResourcePackRepository().getSelectedIds());
                note("colormaps desired=" + net.historicalexperience.client.colour.HistoricalColormaps
                    .desiredEraSignature() + " applied=" + net.historicalexperience.client.colour.HistoricalColormaps
                    .appliedEra());
                note("reloads=" + HistoricalTextureService.reloadCount());
                observe(client, "ENFORCED");
                shot(client, "tex-probe-enforced.png");
                if (Boolean.getBoolean("historical_experience.texturesProbe.skipToggle")) {
                    note("DONE");
                    finish(client);
                    return;
                }
                // Turn enforcement off and let the service reload on its own, exactly as the config toggle would.
                HistoricalTextureConfig.setEnabled(false);
                stage = 2;
                ticks = 0;
                return;
            }
            if (stage == 2 && ticks > 80 && "none".equals(HistoricalTextureService.appliedSignature())) {
                observe(client, "DISABLED");
                note("options.txt unchanged by enforcement: " + optionsBefore.equals(readOptions(client)));
                note("reloads=" + HistoricalTextureService.reloadCount());
                shot(client, "tex-probe-disabled.png");
                HistoricalTextureConfig.setEnabled(savedEnabled);
                stage = 3;
                ticks = 0;
                return;
            }
            if (stage == 3 && ticks > 80 && !"none".equals(HistoricalTextureService.appliedSignature())) {
                observe(client, "RE-ENABLED");
                note("reloads=" + HistoricalTextureService.reloadCount());
                note("DONE");
                finish(client);
                return;
            }
            if (stage < 4 && ticks > 1200) {
                note("FAIL timed out in stage " + stage + " applied=" + HistoricalTextureService.appliedSignature());
                finish(client);
            }
        } catch (Throwable failure) {
            note("FAIL " + failure);
            finish(client);
        }
    }

   private static int modMenuStage;
    private static int modMenuTicks;

    private static int tooltipStage;
    private static int tooltipTicks;
    private static String tooltipWorld = "";
    private static final java.util.List<String> TOOLTIP_WORLDS = new java.util.ArrayList<>();

    /**
     * Opens a world, puts a real item under the cursor in the survival inventory, and photographs the tooltip, then
     * repeats for the next world. Screenshots are the only way to see this: whether a tooltip is drawn, and whether it is
     * the flat legacy box or the modern bordered one, is a rendering outcome rather than a value to read.
     *
     * <p>Worlds are taken from {@code -Dhistorical_experience.texturesProbe.tooltipWorlds=a,b,c}, in order. The expected
     * result per world comes from {@link HistoricalTooltips.Era}, so the probe records what it saw rather than deciding.
     */
    private static void tooltipShots(Minecraft client) {
        tooltipTicks++;
        if (TOOLTIP_WORLDS.isEmpty()) {
            String configured = System.getProperty("historical_experience.texturesProbe.tooltipWorlds", "");
            for (String entry : configured.split(";")) {
                if (!entry.isBlank()) {
                    TOOLTIP_WORLDS.add(entry.trim());
                }
            }
        }
        if (client.getOverlay() != null) {
            return;
        }
        switch (tooltipStage) {
            case 0 -> { // on the title screen: open the next world
                if (client.level == null && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                    && tooltipTicks > 60) {
                    if (TOOLTIP_WORLDS.isEmpty()) {
                        note("FAIL no tooltipWorlds configured");
                        finish(client);
                        return;
                    }
                    tooltipWorld = TOOLTIP_WORLDS.remove(0);
                    HistoricalTextureConfig.markSetupCompleted();
                    note("---- tooltip check in world '" + tooltipWorld + "'");
                    note("tooltip era expected: " + HistoricalTooltips.era());
                    client.createWorldOpenFlows().openWorld(tooltipWorld, () -> { });
                    tooltipStage = 1;
                    tooltipTicks = 0;
                }
            }
            case 1 -> { // in world: hand the player an item, then open the survival inventory
                if (client.player == null || client.level == null) {
                    if (tooltipTicks > 1200) {
                        note("FAIL world never loaded: " + tooltipWorld);
                        finish(client);
                    }
                    return;
                }
                if (tooltipTicks == 40) {
                    client.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.STONE_PICKAXE));
                    note("gave the player a stone pickaxe to hover");
                }
                if (tooltipTicks == 60) {
                    client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
                    tooltipStage = 2;
                    tooltipTicks = 0;
                }
            }
            case 2 -> { // hover the slot, let a frame draw, then photograph
                if (tooltipTicks == 20) {
                    hoverFirstSlot(client);
                }
                if (tooltipTicks == 60) {
                    note("screen=" + client.screen.getClass().getSimpleName()
                        + " hoveredSlot=" + hoveredSlotDescription(client));
                    shot(client, "tex-probe-tooltip-" + tooltipWorld.replaceAll("[^A-Za-z0-9]+", "_") + ".png");
                }
                if (tooltipTicks == 90) {
                    // Leave the world so the next world starts from a clean title screen.
                    client.level.disconnect();
                    client.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
                    tooltipStage = 0;
                    tooltipTicks = 0;
                }
            }
            default -> { }
        }
    }

    /** Ask the container screen which slot is hovered, so the probe can say whether the hover actually landed. */
    private static String hoveredSlotDescription(Minecraft client) {
        if (!(client.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) {
            return "not a container screen";
        }
        net.minecraft.world.inventory.Slot slot =
            ((net.historicalexperience.mixin.client.ContainerScreenAccess) screen).historicalExperience$hoveredSlot();
        return slot == null ? "none" : ("slot " + slot.getContainerSlot() + " item=" + slot.getItem().getItem());
    }

    /**
     * Move the real OS cursor over the first inventory slot. The game resolves the hovered slot from the cursor, so the
     * cursor has to actually move; {@code AbstractContainerScreen.getSlotUnderMouse} then reports it and the tooltip is
     * drawn on the next frame.
     */
    private static void hoverFirstSlot(Minecraft client) {
        if (!(client.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen)) {
            return;
        }
        net.minecraft.world.inventory.Slot slot = null;
        for (net.minecraft.world.inventory.Slot candidate : screen.getMenu().slots) {
            if (candidate.hasItem()) {
                slot = candidate;
                break;
            }
        }
        if (slot == null) {
            note("no slot in this menu holds an item to hover");
            return;
        }
        // Slot coordinates are in GUI space; the window wants real pixels, so scale by the GUI factor.
        double scale = client.getWindow().getGuiScale();
        double windowX = (((net.historicalexperience.mixin.client.ContainerScreenAccess) screen).historicalExperience$leftPos()
            + slot.x + 8) * scale;
        double windowY = (((net.historicalexperience.mixin.client.ContainerScreenAccess) screen).historicalExperience$topPos()
            + slot.y + 8) * scale;
        // The window's framebuffer is the screenshot's coordinate space only when nothing is offset, so convert from
        // GUI space to window space and let GLFW place the cursor there.
        double[] cursorX = new double[1];
        double[] cursorY = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(client.getWindow().getWindow(), cursorX, cursorY);
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(client.getWindow().getWindow(), windowX, windowY);
        note("moved cursor to " + (int) windowX + "," + (int) windowY + " for slot " + slot.getContainerSlot()
            + " (was " + (int) cursorX[0] + "," + (int) cursorY[0] + ", guiScale=" + scale + ")");
        // The screen recomputes its hovered slot in its mouse-moved handler, so tell it the mouse moved.
        screen.mouseMoved(windowX / scale, windowY / scale);
    }

    private static int modMenuStageUnused;
    private static int modMenuTicksUnused;

    /**
     * Mod Menu check through the real loader: discovers the {@code modmenu} entrypoints exactly as Mod Menu does, finds
     * this mod's, asks it for its config screen factory, builds the screen, and verifies it is the options screen that
     * contains the Historical Textures section. Also opens Mod Menu's own mod list and photographs it.
     *
     * <p>Mod Menu is a compile-only dependency of the mod proper (it must stay optional), so it is reached here by
     * reflection and this probe cannot create a hard link to it.
     */
    private static void modMenuCheck(Minecraft client) {
        modMenuTicks++;
        if (!(client.screen instanceof net.minecraft.client.gui.screens.TitleScreen) && modMenuStage == 0) {
            return;
        }
        try {
            switch (modMenuStage) {
                case 0 -> {
                    if (modMenuTicks < 40) {
                        return;
                    }
                    HistoricalTextureConfig.markSetupCompleted();
                    Class<?> api = Class.forName("com.terraformersmc.modmenu.api.ModMenuApi");
                    var containers = net.fabricmc.loader.api.FabricLoader.getInstance()
                        .getEntrypointContainers("modmenu", api);
                    note("modmenu entrypoints discovered: " + containers.size());
                    boolean found = false;
                    for (var container : containers) {
                        if (container.getProvider().getMetadata().getId().equals("hex1211")) {
                            found = true;
                            Object entrypoint = container.getEntrypoint();
                            Object factory = api.getMethod("getModConfigScreenFactory").invoke(entrypoint);
                            // Through the interface, not the factory's own class: that is a method-reference lambda whose
                            // synthetic class is not accessible by reflection.
                            Object screen = Class.forName("com.terraformersmc.modmenu.api.ConfigScreenFactory")
                                .getMethod("create", net.minecraft.client.gui.screens.Screen.class)
                                .invoke(factory, client.screen);
                            note("hex1211 modmenu entrypoint builds " + screen.getClass().getName());
                            note("is the Historical Experience options screen: "
                                + (screen instanceof net.historicalexperience.client.config.HistoricalConfigScreen));
                        }
                    }
                    note("hex1211 entrypoint found through the Fabric loader: " + found);
                    Class<?> mods = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                    client.setScreen((net.minecraft.client.gui.screens.Screen) mods
                        .getConstructor(net.minecraft.client.gui.screens.Screen.class).newInstance(client.screen));
                    modMenuStage = 1;
                    modMenuTicks = 0;
                }
                case 1 -> {
                    if (modMenuTicks == 60) {
                        note("Mod Menu list screen opened: " + client.screen.getClass().getName());
                        shot(client, "tex-probe-modmenu.png");
                    }
                    if (modMenuTicks == 90) {
                        note("DONE");
                        finish(client);
                    }
                }
                default -> { }
            }
        } catch (ReflectiveOperationException failure) {
            note("FAIL " + failure);
            finish(client);
        }
    }

    private static int shotStage;
    private static int shotTicks;

    /** Opens the mod's options screen, then its Historical Textures section, and photographs both. */
    private static void configShots(Minecraft client) {
        shotTicks++;
        if (!(client.screen instanceof net.minecraft.client.gui.screens.TitleScreen) && shotStage == 0) {
            if (shotTicks > 800 && !(client.screen instanceof net.historicalexperience.client.config.HistoricalConfigScreen)) {
                note("FAIL never reached the title screen; screen=" + client.screen);
                finish(client);
            }
            if (shotStage == 0 && client.screen != null) {
                return;
            }
        }
        switch (shotStage) {
            case 0 -> {
                if (client.screen instanceof net.minecraft.client.gui.screens.TitleScreen title && shotTicks > 40) {
                    HistoricalTextureConfig.markSetupCompleted();
                    client.setScreen(new net.historicalexperience.client.config.HistoricalConfigScreen(title));
                    shotStage = 1;
                    shotTicks = 0;
                }
            }
            case 1 -> {
                if (shotTicks == 30) {
                    shot(client, "tex-probe-config-main.png");
                }
                if (shotTicks == 50) {
                    client.setScreen(new TexturesConfigScreen(client.screen));
                    shotStage = 2;
                    shotTicks = 0;
                }
            }
            case 2 -> {
                if (shotTicks == 30) {
                    shot(client, "tex-probe-config-textures.png");
                    note("config screens photographed; version=" + describeVersion()
                        + " enabled=" + HistoricalTextureConfig.enabled()
                        + " autoDownload=" + HistoricalTextureConfig.autoDownload());
                }
                if (shotTicks == 60) {
                    note("DONE");
                    finish(client);
                }
            }
            default -> { }
        }
    }

    private static String upgradeTarget;

    /**
     * After leaving a world and before opening the next one, upgrades the NEXT world's version through the same
     * {@code HistoricalUpgradeService.upgradeUnloadedWorld} the Manage World button calls, when the cycle entry is written
     * {@code name=targetVersionId}. The world is opened only after the committed upgrade, which is the order a player
     * goes through, so the texture stack must follow the new authoritative version.
     */
    private static boolean upgradeBeforeOpening(Minecraft client, String entry) {
        int arrow = entry.indexOf('=');
        if (arrow < 0 || upgradeTarget != null && upgradeTarget.equals(entry)) {
            return true;
        }
        String world = entry.substring(0, arrow).trim();
        String target = entry.substring(arrow + 1).trim();
        Path worldDirectory = client.gameDirectory.toPath().resolve("saves").resolve(world);
        var report = net.historicalexperience.server.HistoricalUpgradeService.upgradeUnloadedWorld(
            worldDirectory, target, "textures-probe", System.currentTimeMillis());
        note("UPGRADE '" + world + "' -> " + target + " ok=" + report.ok() + " " + report.summary());
        upgradeTarget = entry;
        return report.ok();
    }

    private static int cycleIndex;
    private static int cycleStage;
    private static int cycleTicks;
    private static int reloadsAtJoin;

    /**
     * Version-switching scenario: joins each named saved world in turn from the title screen, waits for the service's
     * own reconcile, records which pack serves each checked resource, then leaves to the title screen and records the
     * state there (which must be ordinary again). One client session, so this exercises exactly the
     * join / switch / leave lifecycle a player goes through.
     *
     * <p>Leaving is requested from END_CLIENT_TICK and performed on the following tick, never inside the tick that
     * observed the world: {@code Minecraft.disconnect} runs the render loop itself, and calling it re-entrantly from a
     * tick callback is what stalled the earlier stars probe.
     */
    private static void cycleWorlds(Minecraft client, String[] worlds) {
        cycleTicks++;
        if (client.getOverlay() != null) {
            return;
        }
        if (cycleIndex >= worlds.length) {
            if (cycleStage == 0 && client.level == null) {
                note("final title state: desired=" + HistoricalTextureService.desiredSignature()
                    + " applied=" + HistoricalTextureService.appliedSignature()
                    + " reloads=" + HistoricalTextureService.reloadCount());
                note("options.txt unchanged by the whole cycle: " + optionsBefore.equals(readOptions(client)));
                note("DONE");
                finish(client);
            }
            return;
        }
        String entry = worlds[cycleIndex].trim();
        String world = entry.contains("=") ? entry.substring(0, entry.indexOf('=')).trim() : entry;
        switch (cycleStage) {
            case 0 -> { // on the title screen: open the next world
                if (client.level == null && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
                    if (cycleIndex == 0) {
                        optionsBefore = readOptions(client);
                        HistoricalTextureConfig.markSetupCompleted();
                    }
                    if (cycleTicks < 60) {
                        return;
                    }
                    if (!upgradeBeforeOpening(client, entry)) {
                        note("FAIL the upgrade of '" + world + "' did not commit");
                        finish(client);
                        return;
                    }
                    note("---- opening world '" + world + "' (reloads so far " + HistoricalTextureService.reloadCount() + ")");
                    reloadsAtJoin = HistoricalTextureService.reloadCount();
                    client.createWorldOpenFlows().openWorld(world, () -> { });
                    cycleStage = 1;
                    cycleTicks = 0;
                }
            }
            case 1 -> { // waiting for the world, then for the reconcile
                if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
                    if (cycleTicks > 3000) {
                        note("FAIL world '" + world + "' never opened");
                        finish(client);
                    }
                    return;
                }
                boolean applied = HistoricalTextureService.desiredSignature()
                    .equals(HistoricalTextureService.appliedSignature())
                    && !"none".equals(HistoricalTextureService.appliedSignature());
                if (!applied && cycleTicks < 1500) {
                    return;
                }
                note("joined '" + client.getSingleplayerServer().getWorldData().getLevelName() + "' version="
                    + describeVersion() + " reloadsDuringJoin=" + (HistoricalTextureService.reloadCount() - reloadsAtJoin));
                observe(client, "JOINED(" + world + ")");
                cycleStage = 2;
                cycleTicks = 0;
            }
            case 2 -> { // request the leave, perform it on a later tick
                if (cycleTicks == 20) {
                    cycleStage = 3;
                    cycleTicks = 0;
                }
            }
            case 3 -> {
                client.level.disconnect();
                client.disconnect(new net.minecraft.client.gui.screens.TitleScreen());
                cycleStage = 4;
                cycleTicks = 0;
            }
            case 4 -> { // back at the title: let the reconcile restore ordinary resources, then record it
                if (client.level == null && cycleTicks > 40
                    && "none".equals(HistoricalTextureService.appliedSignature())
                    && !HistoricalTextureService.reloadPendingNow()) {
                    observe(client, "LEFT(" + world + ")");
                    cycleIndex++;
                    cycleStage = 0;
                    cycleTicks = 0;
                } else if (cycleTicks > 3000) {
                    note("FAIL never restored ordinary textures after leaving '" + world + "'; applied="
                        + HistoricalTextureService.appliedSignature());
                    finish(client);
                }
            }
            default -> { }
        }
    }

    private static boolean ordinaryRequested;
    private static String ordinaryName = "";

    /**
     * Ordinary-world scenario: from the title screen, creates a brand-new vanilla world (vanilla dimensions, no
     * historical marker) in the project's run/ directory, so the "ordinary worlds are unaffected" claim is measured on a
     * world that is genuinely not historical.
     */
    private static void createOrdinaryWorld(Minecraft client) {
        if (ordinaryRequested || client.level != null || client.getOverlay() != null
            || !(client.screen instanceof net.minecraft.client.gui.screens.TitleScreen)) {
            return;
        }
        ordinaryRequested = true;
        HistoricalTextureConfig.markSetupCompleted();
        String name = "hex-probe-ordinary-" + System.currentTimeMillis();
        ordinaryName = name;
        note("creating an ordinary vanilla world: " + name);
        var settings = new net.minecraft.world.level.LevelSettings(name, net.minecraft.world.level.GameType.CREATIVE,
            false, net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(),
            net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
        client.createWorldOpenFlows().createFreshLevel(name, settings,
            net.minecraft.world.level.levelgen.WorldOptions.defaultWithRandomSeed(),
            net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions,
            client.screen);
    }

    /** Writes the evidence and stops; leaving the stage first because {@code stop()} is asynchronous. */
    private static void finish(Minecraft client) {
        stage = 4;
        write();
        client.stop();
    }

    private static int titleStage;
    private static int titleTicks;

    /**
     * First-run flow, driven from the title screen with a client that has never completed setup: records that the
     * setup screen appeared on its own, shows the detected packs, then (when asked) downloads the missing ones through
     * the real pinned downloader and records the outcome.
     */
    private static void titleOnly(Minecraft client) {
        titleTicks++;
        switch (titleStage) {
            case 0 -> {
                if (client.screen instanceof TextureSetupScreen) {
                    note("first-run setup screen appeared on its own: " + client.screen.getClass().getSimpleName());
                    note("missing=" + HistoricalTextureService.missingDependencies());
                    note("setupCompleted(before)=" + HistoricalTextureConfig.setupCompleted());
                    titleStage = 1;
                    titleTicks = 0;
                } else if (titleTicks > 600) {
                    note("FAIL the setup screen never appeared; screen=" + client.screen);
                    finish(client);
                }
            }
            case 1 -> {
                // Several frames after the screen opened, so the capture contains it rather than the frame before it.
                if (titleTicks == 60) {
                    shot(client, "tex-probe-setup.png");
                }
                if (titleTicks == 80) {
                    if (Boolean.getBoolean("historical_experience.texturesProbe.download")) {
                        note("starting download of: " + HistoricalTextureService.missingDependencies());
                        HistoricalTextureService.downloadMissing(client);
                    }
                    titleStage = 2;
                    titleTicks = 0;
                }
            }
            case 2 -> {
                var loader = HistoricalTextureService.downloader();
                if (titleTicks % 40 == 0) {
                    for (TextureSource source : TextureSource.values()) {
                        if (source.isDependency() && source != TextureSource.PROGRAMMER_ART) {
                            note("download " + source.id() + " = " + loader.state(source));
                        }
                    }
                }
                if (titleTicks > 20 && !loader.isRunning()) {
                    note("usable=" + HistoricalTextureService.scan().usable().keySet());
                    note("missing(after)=" + HistoricalTextureService.missingDependencies());
                    note("managed dir=" + HistoricalTextureService.managedDirectory());
                    note("DONE");
                    finish(client);
                } else if (titleTicks > 6000) {
                    note("FAIL download did not finish");
                    finish(client);
                }
            }
            default -> { }
        }
    }

    private static String describeVersion() {
        var version = HistoricalClientRules.activeVersion();
        return version == null ? "ordinary" : version.id() + " chronology=" + version.chronology();
    }

    private static void observe(Minecraft client, String label) {
        var layer = HistoricalTextureService.activeLayer();
        note(label + " layer=" + (layer == null ? "none" : layer.plan().signature() + " opened=" + layer.openedSources()));
        for (String name : CHECKED) {
            ResourceLocation location = ResourceLocation.parse(name);
            Optional<Resource> resource = client.getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                note(label + " " + name + " -> MISSING");
                continue;
            }
            String bytes = hash(resource.get());
            String ours = layer == null ? "-" : String.valueOf(layer.winner(PackType.CLIENT_RESOURCES, location));
            note(label + " " + name + " -> servedBy=" + resource.get().sourcePackId()
                + " layerWinner=" + ours + " sha256=" + bytes);
        }
    }

    private static String hash(Resource resource) {
        try (var in = resource.open()) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()))
                .substring(0, 16);
        } catch (Exception failure) {
            return "unreadable:" + failure.getMessage();
        }
    }

    private static String readOptions(Minecraft client) {
        try {
            return Files.readString(client.gameDirectory.toPath().resolve("options.txt"));
        } catch (Exception failure) {
            return "unreadable";
        }
    }

    private static void shot(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(),
            message -> System.out.println("[hex-tex-probe] " + message.getString()));
    }

    private static void note(String text) {
        RESULTS.add(text);
        System.out.println("[hex-tex-probe] " + text);
    }

    private static void write() {
        try {
            String name = System.getProperty("historical_experience.texturesProbe.out", "logs/textures-probe.txt");
            Files.createDirectories(Path.of(name).getParent());
            Files.write(Path.of(name), RESULTS);
        } catch (Exception failure) {
            System.err.println("[hex-tex-probe] could not save evidence: " + failure);
        }
    }
}

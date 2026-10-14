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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.historicalexperience.client.lighting.HistoricalWorldTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Opt-in, read-only world inspection plus container-screen photographs in the development client. */
public final class NtVisualProbe {
    private static final List<String> RESULTS = new ArrayList<>();
    private static int ticks;
    private static int stage;
    private static net.minecraft.world.level.GameType originalLocalMode;

    private NtVisualProbe() {
    }

    public static void register() {
        if (!Boolean.getBoolean("historical_experience.ntVisualProbe")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(NtVisualProbe::tick);
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null || ++ticks < 200 || ticks % 60 != 0) {
            return;
        }
        try {
            switch (stage++) {
                case 0 -> {
                    var version = HistoricalClientRules.activeVersion();
                    note("version=" + (version == null ? "ordinary" : version.id()));
                    note("recipeBookDisabled=" + HistoricalWorldTweaks.recipeBookDisabled());
                    var biome = client.level.getBiome(client.player.blockPosition()).value();
                    note("biome=" + client.level.getBiome(client.player.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("?")
                        + " temp=" + biome.getBaseTemperature());
                    note("biomeBaseSky=0x" + Integer.toHexString(biome.getSkyColor()));
                    note("renderedSky=" + client.level.getSkyColor(client.player.position(), 1.0F));
                    note("skyEra=" + HistoricalWorldTweaks.skyEra(version)
                        + " flat=" + (HistoricalWorldTweaks.eraSkyColour(version) == null ? "none"
                            : "0x" + Integer.toHexString(HistoricalWorldTweaks.eraSkyColour(version)))
                        + " optionActive=" + HistoricalWorldTweaks.skyTweakActive(version));
                    var mbSky = mod.bluestaggo.modernerbeta.client.color.SkyColorSampler.INSTANCE;
                    var mbClimate = mbSky.getClimateSampler();
                    if (mbClimate != null && mbSky.useSkyColor() && client.player != null) {
                        int sx = net.minecraft.util.Mth.floor(client.player.getX());
                        int sz = net.minecraft.util.Mth.floor(client.player.getZ());
                        float temperature = (float) mbClimate.sampleSky(sx, sz);
                        note("modernerBetaSky sampler=" + mbClimate.getClass().getSimpleName()
                            + " temperatureAtPlayer=" + temperature
                            + " betaFormula=0x"
                            + Integer.toHexString(
                                net.historicalexperience.client.lighting.HistoricalSkyColours
                                    .temperatureSkyColour(temperature)).toUpperCase()
                            + " samplerColour=" + mbSky.getSkyColor(client.player.position()));
                    } else {
                        note("modernerBetaSky sampler unavailable; the game's own colour is kept");
                    }
                    for (var block : List.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST)) {
                        var state = block.defaultBlockState();
                        var model = client.getBlockRenderer().getBlockModel(state);
                        note("block=" + block + " shape=" + state.getRenderShape()
                            + " particle=" + model.getParticleIcon().contents().name());
                    }
                    for (var item : List.of(Items.CHEST, Items.TRAPPED_CHEST, Items.ENDER_CHEST)) {
                        var model = client.getItemRenderer().getModel(new ItemStack(item), client.level, client.player, 0);
                        note("item=" + item + " custom=" + model.isCustomRenderer()
                            + " particle=" + model.getParticleIcon().contents().name());
                    }
                    sampleEnvironmentSky(client);
                    originalLocalMode = client.gameMode.getPlayerMode();
                    client.gameMode.setLocalMode(net.minecraft.world.level.GameType.SURVIVAL);
                    client.setScreen(new InventoryScreen(client.player));
                    noteButtons(client.screen, "inventory-init");
                }
                case 1 -> {
                    checkButtons(client, "inventory");
                    client.setScreen(new CraftingScreen(new CraftingMenu(0, client.player.getInventory()),
                        client.player.getInventory(), Component.literal("Crafting probe")));
                }
                case 2 -> {
                    checkButtons(client, "crafting");
                    client.setScreen(new FurnaceScreen(new FurnaceMenu(0, client.player.getInventory()),
                        client.player.getInventory(), Component.literal("Furnace probe")));
                }
                case 3 -> {
                    checkButtons(client, "furnace");
                    client.setScreen(null);
                }
                case 4 -> {
                    capture(client, "world");
                    Files.createDirectories(Path.of("logs"));
                    Files.write(Path.of("logs/nt-visual-probe.txt"), RESULTS);
                    if (originalLocalMode != null) {
                        client.gameMode.setLocalMode(originalLocalMode);
                    }
                    // Minecraft's normal shutdown flushes the test save; never terminate unrelated JVMs.
                    client.stop();
                }
                default -> { }
            }
        } catch (Throwable failure) {
            note("FAIL " + failure);
            try {
                Files.createDirectories(Path.of("logs"));
                Files.write(Path.of("logs/nt-visual-probe.txt"), RESULTS);
            } catch (Exception ignored) { }
            client.stop();
        }
    }

    private static void sampleEnvironmentSky(Minecraft client) {
        // Client-side only, restored within the same tick: never changes server save time/weather.
        var level = client.level;
        long time = level.getDayTime();
        float rain = level.getRainLevel(1.0F);
        float thunder = level.getThunderLevel(1.0F);
        try {
            level.setRainLevel(0.0F);
            level.setThunderLevel(0.0F);
            level.setDayTime(6000);
            note("sky-noon=" + level.getSkyColor(client.player.position(), 1.0F));
            level.setDayTime(18000);
            note("sky-midnight=" + level.getSkyColor(client.player.position(), 1.0F));
            level.setDayTime(6000);
            level.setRainLevel(1.0F);
            note("sky-rain=" + level.getSkyColor(client.player.position(), 1.0F));
        } finally {
            level.setDayTime(time);
            level.setRainLevel(rain);
            level.setThunderLevel(thunder);
        }
    }

    private static void checkButtons(Minecraft client, String label) {
        noteButtons(client.screen, label);
        capture(client, label);
    }

    private static void noteButtons(net.minecraft.client.gui.screens.Screen screen, String label) {
        if (screen == null) {
            note("FAIL " + label + " screen was closed before capture");
            return;
        }
        long visible = screen.children().stream().filter(ImageButton.class::isInstance)
            .map(ImageButton.class::cast).filter(button -> button.visible).count();
        note(label + " screen=" + screen.getClass().getSimpleName() + " visibleImageButtons=" + visible);
    }

    private static void capture(Minecraft client, String label) {
        Screenshot.grab(client.gameDirectory, "nt-probe-" + label + ".png", client.getMainRenderTarget(),
            message -> System.out.println("[hex-nt-probe] screenshot " + message.getString()));
    }

    private static void note(String text) {
        RESULTS.add(text);
        System.out.println("[hex-nt-probe] " + text);
    }
}

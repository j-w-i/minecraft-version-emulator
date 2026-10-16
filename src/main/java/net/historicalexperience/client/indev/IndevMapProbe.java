/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.client.indev;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import com.google.gson.GsonBuilder;

import mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator;
import mod.bluestaggo.modernerbeta.settings.SettingsComponentTypes;
import mod.bluestaggo.modernerbeta.settings.component.FiniteLevelProperties;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.historicalexperience.client.version.IndevMapSelection;
import net.historicalexperience.client.version.SnapshotFeatureProbe;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.historicalexperience.server.HistoricalServerLifecycle;
import net.historicalexperience.worldgen.IndevMapSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Native acceptance for the Indev map settings: does a chosen option actually reach the world that is created?
 *
 * <p>Explicitly armed only ({@code -Dhistorical_experience.indevProbe=true}) and pinned to this project's run
 * directory, like the other developer probes. It creates three worlds in one client: one with a chosen non-default
 * Indev configuration, one Indev world left untouched, and one ordinary world as the negative. The proof is read off
 * the RUNNING SERVER's own generator - the stored settings component, the loaded biome at spawn and the world border
 * - not off the screen that asked for it.
 */
public final class IndevMapProbe {
    private static final String PROP = "historical_experience.indevProbe";
    private static final String PROFILE = "historical_experience:indev_latest";

    private IndevMapProbe() {
    }

    /** This probe's own receipt list is the shared one; the helper keeps the call sites readable. */
    private static void check(boolean result, String message) {
        SnapshotFeatureProbe.check(result, message);
    }

    /** Registers the probe only when it was explicitly asked for. */
    public static void registerIfRequested() {
        if (Boolean.getBoolean(PROP)) {
            ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
                private boolean started;

                @Override
                public void onEndTick(Minecraft client) {
                    if (this.started || client.getOverlay() != null) {
                        return;
                    }
                    this.started = true;
                    Thread driver = new Thread(() -> drive(client), "hex-indev-probe");
                    driver.setDaemon(true);
                    driver.start();
                }
            });
        }
    }

    private static void drive(Minecraft client) {
        Path root = client.gameDirectory.toPath().toAbsolutePath().normalize();
        String approved = System.getProperty(PROP + ".root");
        if (approved == null || !root.equals(Path.of(approved).toAbsolutePath().normalize())) {
            System.err.println("[hex-indev] Refusing probe outside explicitly approved project run directory");
            client.execute(client::stop);
            return;
        }
        String stamp = Long.toString(System.currentTimeMillis());
        boolean passed = false;
        Throwable failure = null;
        try {
            chosenWorld(client, stamp);
            defaultChoiceWorld(client, stamp);
            inlandWorld(client, stamp);
            untouchedWorld(client, stamp);
            ordinaryWorld(client, stamp);
            passed = true;
        } catch (Throwable problem) {
            failure = problem;
            problem.printStackTrace();
        } finally {
            try {
                var result = new LinkedHashMap<String, Object>();
                result.put("passed", passed);
                result.put("run", stamp);
                result.put("checks", net.historicalexperience.client.version.SnapshotFeatureProbe.checks());
                result.put("failure", failure == null ? null : failure.toString());
                Files.writeString(root.resolve("indev-probe-" + stamp + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
            } catch (Exception problem) {
                problem.printStackTrace();
            }
            client.execute(client::stop);
        }
    }

    /** The whole point: a chosen configuration, then the running world's own generator and biome. */
    private static void chosenWorld(Minecraft client, String stamp) throws Exception {
        String name = "sol-indev-chosen-" + stamp;
        create(client, name, "chosen");
        server(client, () -> {
            ModernBetaChunkGenerator generator = generator(client.getSingleplayerServer());
            FiniteLevelProperties properties = generator.getChunkSettings()
                .get(SettingsComponentTypes.FINITE_LEVEL_PROPERTIES);
            // The component may be a preset DELTA on the loaded server (the reference plus our overrides), so if it is
            // not inline the proof has to come from what was generated - which is the stronger evidence anyway.
            System.out.println("[hex-indev] loaded chunk settings: preset="
                + generator.getChunkSettings().get(SettingsComponentTypes.PRESET)
                + " provider=" + generator.getChunkSettings().get(SettingsComponentTypes.PROVIDER)
                + " finiteLevelProperties=" + properties + " size="
                + generator.getChunkSettings().size());
            check(properties != null && "floating".equals(properties.type().getSerializedName())
                    && "hell".equals(properties.theme().getSerializedName())
                    && properties.width() == 64 && properties.length() == 64 && properties.height() == 256,
                "the chosen type, theme and dimensions are stored in the loaded world: " + properties);
            apronSignature(client.getSingleplayerServer(), "hell floating deep small", 5, 0);
            check(biomeAt(client.getSingleplayerServer(), 0, 0).equals("moderner_beta:indev_hell"),
                "the chosen theme reached the generated world: " + biomeAt(client.getSingleplayerServer(), 0, 0));
            check(client.getSingleplayerServer().overworld().getWorldBorder().getSize() == 64.0,
                "the finite world border followed the chosen Deep Small footprint: "
                    + client.getSingleplayerServer().overworld().getWorldBorder().getSize());
            check(HistoricalServerLifecycle.readMetadata(
                HistoricalServerLifecycle.metadataFileFor(client.getSingleplayerServer()))
                .orElseThrow().selectionId().equals(PROFILE), "the world is still the Indev profile");
            return null;
        });
        disconnect(client);
    }

    /**
     * Prints the real block column of an out-of-bounds chunk, as contiguous runs from the bottom of the world up.
     *
     * <p>Measured rather than assumed, because that is the whole question for a migration: the apron release recognises
     * the synthetic shell outside the finite island by its exact blocks. Whether an Inland map, a Hell map or a
     * shorter map has the same shell cannot be read off the code without reimplementing the generator, and guessing it
     * is how the wrong 32,768 blocks get released.
     */
    private static void apronSignature(MinecraftServer server, String label, int chunkX, int chunkZ) {
        ServerLevel level = server.overworld();
        var chunk = level.getChunk(chunkX, chunkZ);
        int wx = chunk.getPos().getMinBlockX() + 8, wz = chunk.getPos().getMinBlockZ() + 8;
        StringBuilder runs = new StringBuilder();
        String previous = null;
        int start = level.getMinBuildHeight();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(level.getBlockState(new BlockPos(wx, y, wz)).getBlock()).toString();
            if (!name.equals(previous)) {
                if (previous != null) runs.append(start).append('-').append(y - 1).append(' ').append(previous).append(" | ");
                previous = name;
                start = y;
            }
        }
        runs.append(start).append('-').append(level.getMaxBuildHeight() - 1).append(' ').append(previous);
        System.out.println("[hex-indev] APRON " + label + " chunk " + chunkX + "," + chunkZ + " column " + wx + ","
            + wz + " minY=" + level.getMinBuildHeight() + " maxY=" + level.getMaxBuildHeight() + " = " + runs);
    }

    /**
     * The trap guard: opening the options screen and confirming the DEFAULT choice must write no override.
     *
     * <p>An override is what makes a world "customized" to the finite-to-infinite migration, which then refuses it -
     * so if merely looking at the options could do that, the player would silently lose the Infdev upgrade.
     */
    private static void defaultChoiceWorld(Minecraft client, String stamp) throws Exception {
        String name = "sol-indev-defaults-" + stamp;
        create(client, name, "defaults");
        server(client, () -> {
            check(generator(client.getSingleplayerServer()).getChunkSettings()
                    .get(SettingsComponentTypes.FINITE_LEVEL_PROPERTIES) == null,
                "confirming the default option names wrote no override into the world");
            check(biomeAt(client.getSingleplayerServer(), 0, 0).equals("moderner_beta:indev_normal"),
                "and the world keeps upstream's normal biome: "
                    + biomeAt(client.getSingleplayerServer(), 0, 0));
            apronSignature(client.getSingleplayerServer(), "default option names", 9, 0);
            return null;
        });
        disconnect(client);
    }

    /** Measures the apron for an Inland map: the configuration the owner wants migrated. */
    private static void inlandWorld(Minecraft client, String stamp) throws Exception {
        String name = "sol-indev-inland-" + stamp;
        create(client, name, "inland");
        server(client, () -> {
            apronSignature(client.getSingleplayerServer(), "inland square normal", 9, 0);
            check(client.getSingleplayerServer().overworld().getChunkSource().getGenerator()
                    instanceof ModernBetaChunkGenerator, "an Inland world is still a finite Indev world");
            return null;
        });
        disconnect(client);
    }

    /** The default must be untouched: no choice, so upstream's own preset, height 128 included. */
    private static void untouchedWorld(Minecraft client, String stamp) throws Exception {        String name = "sol-indev-default-" + stamp;
        create(client, name, "untouched");
        server(client, () -> {
            check(generator(client.getSingleplayerServer()).getChunkSettings()
                    .get(SettingsComponentTypes.FINITE_LEVEL_PROPERTIES) == null,
                "no Indev override was written for a world that never opened the chooser");
            check(biomeAt(client.getSingleplayerServer(), 0, 0).equals("moderner_beta:indev_normal"),
                "an Indev world created without the chooser keeps upstream's normal biome: "
                    + biomeAt(client.getSingleplayerServer(), 0, 0));
            check(client.getSingleplayerServer().overworld().getWorldBorder().getSize() == 256.0,
                "and upstream's own 256-wide border: "
                    + client.getSingleplayerServer().overworld().getWorldBorder().getSize());
            apronSignature(client.getSingleplayerServer(), "untouched no chooser", 9, 0);
            return null;
        });
        disconnect(client);
    }

    /** The negative: an ordinary world must have no historical profile and no Moderner Beta generator. */
    private static void ordinaryWorld(Minecraft client, String stamp) throws Exception {
        String name = "sol-indev-ordinary-" + stamp;
        create(client, name, "ordinary");
        server(client, () -> {
            check(HistoricalServerLifecycle.readMetadata(
                HistoricalServerLifecycle.metadataFileFor(client.getSingleplayerServer()))
                .isEmpty(), "an ordinary world still has no historical metadata");
            ChunkGenerator generator = client.getSingleplayerServer().overworld().getChunkSource().getGenerator();
            check(!(generator instanceof ModernBetaChunkGenerator),
                "and no Moderner Beta generator to carry Indev settings: " + generator.getClass().getName());
            return null;
        });
        disconnect(client);
    }

    private static ModernBetaChunkGenerator generator(MinecraftServer server) {
        ChunkGenerator generator = server.overworld().getChunkSource().getGenerator();
        check(generator instanceof ModernBetaChunkGenerator, "the Indev world generates with Moderner Beta");
        return (ModernBetaChunkGenerator) generator;
    }

    private static String biomeAt(MinecraftServer server, int x, int z) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        // Sample at the level's own surface height for the column, not at the spawn's y: out-of-bounds columns have
        // no terrain, and the biome question is about the column.
        BlockPos pos = new BlockPos(spawn.getX() + x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, spawn.getX() + x, spawn.getZ() + z) - 1, spawn.getZ() + z);
        return level.getBiome(pos).unwrapKey().map(key -> key.location().toString()).orElse("<unknown>");
    }

    private static void server(Minecraft client, java.util.concurrent.Callable<Object> action) throws Exception {
        MinecraftServer server = client.getSingleplayerServer();
        SnapshotFeatureProbe.check(server != null, "the created world has a running server");
        SnapshotFeatureProbe.call(server, action);
    }

    private static void disconnect(Minecraft client) throws Exception {
        SnapshotFeatureProbe.call(client, () -> {
            client.level.disconnect();
            client.disconnect(new TitleScreen());
            return null;
        });
        try {
            SnapshotFeatureProbe.await(() -> SnapshotFeatureProbe.call(client,
                () -> client.getSingleplayerServer() == null && client.getOverlay() == null), 120);
        } catch (java.util.concurrent.TimeoutException slow) {
            // Reported rather than left as a bare timeout: what is still holding the client matters, and a silent
            // "condition exceeded" says nothing about whether a screen, the overlay or the server is the one lingering.
            System.out.println("[hex-indev] disconnect still waiting: screen="
                + SnapshotFeatureProbe.call(client, () -> client.screen == null ? "<none>"
                    : client.screen.getClass().getName())
                + " overlay=" + SnapshotFeatureProbe.call(client, () -> client.getOverlay() == null ? "<none>"
                    : client.getOverlay().getClass().getName())
                + " server=" + SnapshotFeatureProbe.call(client, () -> client.getSingleplayerServer() == null
                    ? "<none>" : "present"));
            throw slow;
        }
        Thread.sleep(2000);
    }

    /** Creates a world through the real screens. {@code chosen} null means "no historical version at all". */
    private static void create(Minecraft client, String name, String mode) throws Exception {
        SnapshotFeatureProbe.call(client, () -> {
            client.options.renderDistance().set(2);
            client.options.simulationDistance().set(5);
            CreateWorldScreen.openFresh(client, new TitleScreen());
            return null;
        });
        SnapshotFeatureProbe.await(() -> SnapshotFeatureProbe.call(client,
            () -> client.screen instanceof CreateWorldScreen), 90);
        SnapshotFeatureProbe.call(client, () -> {
            var create = (CreateWorldScreen) client.screen;
            create.getUiState().setName(name);
            create.getUiState().setSeed("20100223");
            create.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
            create.getUiState().setAllowCommands(false);
            if (!mode.equals("ordinary")) {
                SnapshotFeatureProbe.press(client, "Historical Version:");
            }
            return null;
        });
        if (!mode.equals("ordinary")) {
            var entry = HistoricalVersionCatalogue.byKey(PROFILE).orElseThrow();
            SnapshotFeatureProbe.call(client, () -> {
                check(!indevOptionsActive(client),
                    "with no version highlighted the picker does not offer Indev map settings");
                SnapshotFeatureProbe.press(client, entry.era().displayName());
                boolean selected = false;
                for (var child : client.screen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.ObjectSelectionList<?> list) {
                        for (Object value : list.children()) {
                            var row = (net.minecraft.client.gui.components.ObjectSelectionList.Entry<?>) value;
                            if (row.getNarration().getString().equals(entry.displayName() + ", implemented")) {
                                row.mouseClicked(0, 0, 0);
                                selected = true;
                            }
                        }
                    }
                }
                check(selected, "the Indev row was selected in the real picker");
                check(indevOptionsActive(client), "the picker offers Indev map settings for that row");
                return null;
            });
            if (!mode.equals("untouched")) {
                SnapshotFeatureProbe.call(client, () -> {
                    SnapshotFeatureProbe.press(client, "Indev settings...");
                    return null;
                });
                SnapshotFeatureProbe.await(() -> SnapshotFeatureProbe.call(client,
                    () -> client.screen instanceof net.historicalexperience.client.version.IndevMapSettingsScreen), 15);
                SnapshotFeatureProbe.call(client, () -> {
                    if (mode.equals("chosen")) {
                        SnapshotFeatureProbe.press(client, "Floating");
                        SnapshotFeatureProbe.press(client, "Hell");
                        SnapshotFeatureProbe.press(client, "Deep");
                        SnapshotFeatureProbe.press(client, "Small");
                    } else if (mode.equals("inland")) {
                        SnapshotFeatureProbe.press(client, "Inland");
                    }
                    // The screen keeps its own working state and writes the choice only when confirmed, so the
                    // combination is read back after that button - which is also the state the world is created from.
                    // In "defaults" mode nothing is changed first: opening the screen and confirming it unchanged must
                    // write no override at all, or merely looking at the options would cost the world its upgrade.
                    SnapshotFeatureProbe.press(client, "Use these settings");
                    var choice = IndevMapSelection.choice();
                    if (mode.equals("chosen")) {
                        check(choice.type() == IndevMapSettings.LevelType.FLOATING
                                && choice.theme() == IndevMapSettings.Theme.HELL
                                && choice.shape() == IndevMapSettings.Shape.DEEP
                                && choice.size() == IndevMapSettings.Size.SMALL,
                            "the screen's four groups recorded the chosen combination: " + choice.describe());
                    } else {
                        check(choice.equals(IndevMapSettings.DEFAULT),
                            "confirming the screen unchanged keeps the default option names: " + choice.describe());
                    }
                    check(IndevMapSelection.chosen(), "and the choice is remembered for this world creation");
                    return null;
                });
                SnapshotFeatureProbe.await(() -> SnapshotFeatureProbe.call(client,
                    () -> !(client.screen instanceof net.historicalexperience.client.version.IndevMapSettingsScreen)), 15);
            }
            SnapshotFeatureProbe.call(client, () -> {
                SnapshotFeatureProbe.press(client, "Use this version");
                return null;
            });
        }
        SnapshotFeatureProbe.call(client, () -> {
            SnapshotFeatureProbe.press(client, Component.translatable("selectWorld.create").getString());
            return null;
        });
        SnapshotFeatureProbe.await(() -> SnapshotFeatureProbe.call(client, () -> {
            if (SnapshotFeatureProbe.acceptVanillaWorldWarning(client)) {
                return false;
            }
            return client.level != null && client.player != null && client.screen == null
                && client.getSingleplayerServer() != null;
        }), 180);
        SnapshotFeatureProbe.call(client, () -> {
            check(client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
                    .startsWith(client.gameDirectory.toPath().toAbsolutePath().normalize().resolve("saves")),
                "the world was created inside this project's own saves directory");
            return null;
        });
    }

    /** Whether the picker's Indev-map-settings button is live for whatever is highlighted, without pressing it. */
    private static boolean indevOptionsActive(Minecraft client) {
        for (var widget : net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(client.screen)) {
            if (widget instanceof Button button && button.getMessage().getString().startsWith("Indev settings")) {
                return button.active;
            }
        }
        return false;
    }
}

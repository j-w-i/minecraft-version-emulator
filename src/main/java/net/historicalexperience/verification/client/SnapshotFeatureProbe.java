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

import net.historicalexperience.client.LegacyCreativeScreen;
import net.historicalexperience.client.version.CreateWorldHistoricalVersion;
import net.historicalexperience.client.version.HistoricalVersionScreen;
import net.historicalexperience.server.HistoricalServerLifecycle;
import net.historicalexperience.server.HistoricalSnapshotRuntime;
import net.historicalexperience.version.HistoricalSnapshotPatches;

import net.historicalexperience.verification.client.SnapshotFeatureProbe;

import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.historicalexperience.client.*;
import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.historicalexperience.mixin.client.ContainerScreenAccess;
import net.historicalexperience.server.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;

/** Explicitly armed developer acceptance. Creates only uniquely named worlds in the pinned project run directory.
 * No EULA/account changes, personal saves, global keyboard injection, world deletion or external server.
 */
public final class SnapshotFeatureProbe {
    private static final String PROP = "historical_experience.snapshotProbe";
    private static final String PATCH = "historical_experience:snapshot_11w49a";
    private static final List<String> CHECKS = new CopyOnWriteArrayList<>();
    private static boolean started;
    private SnapshotFeatureProbe() { }
    public static void register() {
        if (!Boolean.getBoolean(PROP)) return;
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (started || !(screen instanceof TitleScreen)) return;
            started = true;
            Thread driver = new Thread(() -> drive(client), "hex-snapshot-feature-probe");
            driver.setDaemon(true); driver.start();
        });
    }
    private static void drive(Minecraft client) {
        Path root = client.gameDirectory.toPath().toAbsolutePath().normalize();
        String approved = System.getProperty(PROP + ".root");
        if (approved == null || !root.equals(Path.of(approved).toAbsolutePath().normalize())) {
            System.err.println("[hex-snapshot] Refusing probe outside explicitly approved project run directory");
            client.execute(client::stop); return;
        }
        String stamp = Long.toString(System.currentTimeMillis());
        String name = "sol-snapshot-native-" + stamp;
        boolean passed = false;
        Throwable failure = null;
        try {
            check(root.equals(Path.of(System.getProperty(PROP + ".root")).toAbsolutePath().normalize()), "exact approved project run directory");
            call(client, () -> {
                client.options.renderDistance().set(2); client.options.simulationDistance().set(5);
                CreateWorldScreen.openFresh(client, new TitleScreen()); return null;
            });
            await(() -> call(client, () -> client.screen instanceof CreateWorldScreen), 90);
            call(client, () -> {
                var create = (CreateWorldScreen) client.screen;
                create.getUiState().setName(name); create.getUiState().setSeed("11491249");
                create.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                create.getUiState().setAllowCommands(false);
                press(client, "Historical Version:"); return null;
            });
            await(() -> call(client, () -> client.screen instanceof HistoricalVersionScreen), 10);
            call(client, () -> {
                press(client, "Snapshots");
                boolean chosen = false;
                for (var child : client.screen.children()) if (child instanceof ObjectSelectionList<?> list)
                    for (Object entry : list.children()) {
                        var row = (ObjectSelectionList.Entry<?>) entry;
                        if (row.getNarration().getString().contains("11w49a")) { row.mouseClicked(0, 0, 0); chosen = true; }
                    }
                check(chosen, "real Snapshots tab row selected"); press(client, "Use this version"); return null;
            });
            await(() -> call(client, () -> client.screen instanceof CreateWorldScreen), 10);
            call(client, () -> { check(PATCH.equals(CreateWorldHistoricalVersion.Selection.key()), "create confirmation retains patch key"); toggleDeselect(client, PATCH); press(client, Component.translatable("selectWorld.create").getString()); return null; });
            await(() -> call(client, () -> {
                if (acceptVanillaWorldWarning(client)) return false;
                return client.level != null && client.player != null && client.screen == null && client.getSingleplayerServer() != null;
            }), 180);
            MinecraftServer server = client.getSingleplayerServer();
            Path world = call(server, () -> server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize());
            check(world.startsWith(root.resolve("saves")) && world.getFileName().toString().startsWith(name), "new owned native storage directory");
            String storageName = world.getFileName().toString();
            call(server, () -> {
                check(HistoricalSnapshotRuntime.activePatchId(server).equals(PATCH), "native directory bootstrap activates11w49a");
                check(HistoricalServerLifecycle.readMetadata(HistoricalServerLifecycle.metadataFileFor(server)).orElseThrow().selectionId().equals(PATCH), "persisted metadata has explicit patch identity");
                check(!server.getWorldData().isAllowCommands() && player(server).gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "private owner starts Survival without cheats"); return null;
            });
            pressB(client);
            await(() -> call(server, () -> player(server).gameMode.getGameModeForPlayer() == GameType.CREATIVE), 10);
            check(true, "native exact-window B key enters Creative through client adapter and real request handler");
            await(() -> call(client, () -> client.gameMode.hasInfiniteItems()), 10);
            pressB(client);
            await(() -> call(server, () -> player(server).gameMode.getGameModeForPlayer() == GameType.SURVIVAL), 10);
            await(() -> call(client, () -> !client.gameMode.hasInfiniteItems()), 10);
            check(true, "second B press toggles Creative back to Survival");
            pressB(client);
            await(() -> call(server, () -> player(server).gameMode.getGameModeForPlayer() == GameType.CREATIVE), 10);
            await(() -> call(client, () -> client.gameMode.hasInfiniteItems()), 10);
            check(true, "third B press toggles Survival back to Creative without held-key repetition");
            call(client, () -> { client.setScreen(new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false)); return null; });
            call(client, () -> {
                check(client.screen instanceof LegacyCreativeScreen, "native creative opening selects legacy screen");
                var screen = (LegacyCreativeScreen) client.screen; var menu = screen.getMenu();
                check(menu.slots.size() == 81, "72 catalog plus9 real hotbar slots");
                for (int i = 0; i < 72; i++) check(menu.getSlot(i).x == 8 + 18 * (i % 8) && menu.getSlot(i).y == 18 + 18 * (i / 8), "grid cell" + i);
                check(menu.maxRow() > 0, "catalog scrolls beyond first72 entries");
                menu.scroll(menu.maxRow());
                for (int i = 0; i < 72; i++) check(HistoricalCreativeGrants.allowed(PATCH, menu.getSlot(i).getItem()), "last page dated cell" + i);
                menu.scroll(0);
                var bounds = (ContainerScreenAccess) (Object) screen;
                double x = bounds.historicalExperience$leftPos() + 16;
                double y = bounds.historicalExperience$topPos();
                screen.mouseClicked(x, y + 26, 0); screen.mouseReleased(x, y + 26, 0);
                check(menu.getCarried().is(Items.STONE), "empty cursor picks catalog item");
                screen.mouseClicked(x + 18, y + 26, 0); screen.mouseReleased(x + 18, y + 26, 0);
                check(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().is(Items.STONE) && menu.getSlot(1).getItem().is(Items.GRASS_BLOCK), "held item plus another catalog item clears cursor without replacement or catalog mutation");
                screen.mouseClicked(x, y + 26, 0); screen.mouseReleased(x, y + 26, 0);
                screen.mouseClicked(x, y + 26, 1); screen.mouseReleased(x, y + 26, 1);
                check(menu.getCarried().isEmpty(), "right catalog click clears held item instead of increasing count");
                screen.mouseClicked(x, y + 26, 0); screen.mouseReleased(x, y + 26, 0);
                screen.mouseClicked(x, y + 26, 0); screen.mouseReleased(x, y + 26, 0);
                check(menu.getCarried().isEmpty(), "same-item second catalog click also clears cursor");
                screen.mouseClicked(x, y + 26, 0); screen.mouseReleased(x, y + 26, 0);
                screen.mouseClicked(x, y + 193, 0); screen.mouseReleased(x, y + 193, 0);
                return null;
            });
            await(() -> call(server, () -> player(server).getInventory().getItem(0).is(Items.STONE)), 10);
            check(true, "catalog click and hotbar placement reaches server as legal grant");
            hotbarGestures(client, server);
            call(client, () -> { Screenshot.grab(client.gameDirectory, name + "-legacy.png", client.getMainRenderTarget(), message -> System.out.println("[hex-snapshot] " + message.getString())); return null; });
            ItemStack sentinel = new ItemStack(Items.NETHERITE_SWORD);
            var data = new CompoundTag(); data.putString("owner", name); sentinel.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            sentinel.set(DataComponents.CUSTOM_NAME, Component.literal("Preservation sentinel")); sentinel.setDamageValue(137);
            call(server, () -> { player(server).getInventory().setItem(0, sentinel.copy()); player(server).inventoryMenu.broadcastFullState(); return null; });
            await(() -> call(client, () -> ItemStack.matches(client.player.getInventory().getItem(0), sentinel)), 10);
            int dropsBefore = call(server, () -> futureDrops(server));
            call(client, () -> {
                client.player.getInventory().setItem(0, ItemStack.EMPTY); // simulate a creative empty-then-fill prediction
                client.player.inventoryMenu.setCarried(sentinel.copy());
                var connection = client.player.connection;
                connection.send(new ServerboundSetCreativeModeSlotPacket(36, ItemStack.EMPTY));
                connection.send(new ServerboundSetCreativeModeSlotPacket(37, sentinel.copy()));
                connection.send(new ServerboundSetCreativeModeSlotPacket(-1, sentinel.copy())); return null;
            });
            await(() -> call(client, () -> ItemStack.matches(client.player.getInventory().getItem(0), sentinel) && client.player.containerMenu.getCarried().isEmpty()), 10);
            call(server, () -> {
                check(ItemStack.matches(player(server).getInventory().getItem(0), sentinel) && player(server).getInventory().getItem(1).isEmpty(), "real raw packets preserve unavailable source and reject duplicate insertion");
                check(futureDrops(server) == dropsBefore, "forged unavailable drop creates no item entity"); return null;
            });
            call(client, () -> {
                client.player.connection.send(new ServerboundContainerClickPacket(0, client.player.inventoryMenu.getStateId(), 36, 2, ClickType.CLONE, sentinel.copy(), new Int2ObjectOpenHashMap<>())); return null;
            });
            // Queue a tick after the server has processed the packet, then inspect actual server-owned state.
            Thread.sleep(1000);
            call(server, () -> { check(player(server).inventoryMenu.getCarried().isEmpty() && ItemStack.matches(player(server).getInventory().getItem(0), sentinel), "native container CLONE cannot duplicate unsupported owned stack"); return null; });
            call(client, () -> { client.level.disconnect(); client.disconnect(new TitleScreen()); return null; });
            await(() -> call(client, () -> client.getSingleplayerServer() == null && client.screen instanceof TitleScreen), 60);
            check(HistoricalSnapshotRuntime.activePatchId(server).isEmpty(), "stopped server snapshot cache cleared");
            // Do not reopen from the same task batch as disconnect/resource reconciliation. Let title frames
            // clear the previous world's pause/loading flags before the native blocking startup wait.
            await(() -> call(client, () -> client.getOverlay() == null), 60);
            Thread.sleep(2000);
            call(client, () -> { client.createWorldOpenFlows().openWorld(storageName, () -> { }); return null; });
            await(() -> call(client, () -> client.level != null && client.player != null && client.screen == null && client.getSingleplayerServer() != null), 180);
            var reopened = client.getSingleplayerServer();
            call(reopened, () -> {
                check(HistoricalSnapshotRuntime.activePatchId(reopened).equals(PATCH), "actual saved world reopen restores patch");
                check(ItemStack.matches(player(reopened).getInventory().getItem(0), sentinel), "actual saved player inventory preserves count damage name and custom data"); return null;
            });
            call(client, () -> { client.level.disconnect(); client.disconnect(new TitleScreen()); return null; });
            await(() -> call(client, () -> client.getSingleplayerServer() == null), 60);
            passed = true;
        } catch (Throwable problem) { failure = problem; problem.printStackTrace(); }
        finally {
            try {
                var result = new LinkedHashMap<String, Object>(); result.put("passed", passed); result.put("world", name); result.put("checks", CHECKS); result.put("failure", failure == null ? null : failure.toString());
                Files.writeString(root.resolve("snapshot-probe-" + stamp + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
            } catch (Throwable problem) { problem.printStackTrace(); }
            client.execute(client::stop);
        }
    }
    private static ServerPlayer player(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
    private static int futureDrops(MinecraftServer server) {
        int count = 0; for (var entity : server.overworld().getAllEntities()) if (entity instanceof ItemEntity item && item.getItem().is(Items.NETHERITE_SWORD)) count++;
        return count;
    }
    private static int stoneDrops(MinecraftServer server) {
        int count = 0; for (var entity : server.overworld().getAllEntities()) if (entity instanceof ItemEntity item && item.getItem().is(Items.STONE)) count++;
        return count;
    }
    private static void hotbarGestures(Minecraft client, MinecraftServer server) throws Exception {
        int before = call(server, () -> stoneDrops(server));
        call(client, () -> {
            var screen = (LegacyCreativeScreen) client.screen; var bounds = (ContainerScreenAccess)(Object)screen;
            int left = bounds.historicalExperience$leftPos(), top = bounds.historicalExperience$topPos();
            for (int col = 0; col < 9; col++) {
                screen.getMenu().setCarried(new ItemStack(Items.STONE));
                double x = left + 16 + col * 18, y = top + 181;
                screen.mouseClicked(x, y, 0); screen.mouseReleased(x, y, 0);
                check(screen.getMenu().getCarried().isEmpty() && client.player.getInventory().getItem(col).is(Items.STONE), "separator click targets hotbar column" + col);
            }
            screen.getMenu().setCarried(new ItemStack(Items.STONE));
            screen.mouseClicked(left + 100, top + 10, 0); screen.mouseReleased(left + 100, top + 10, 0);
            check(screen.getMenu().getCarried().is(Items.STONE), "internal panel padding preserves cursor instead of dropping");
            screen.mouseClicked(left + 172, top + 100, 0); screen.mouseReleased(left + 172, top + 100, 0);
            check(screen.getMenu().getCarried().is(Items.STONE), "catalogue side margin also preserves the cursor");
            screen.mouseClicked(left + 16, top + 26, 0); screen.mouseReleased(left + 16, top + 26, 0);
            // Every row and column below the catalogue must place into the hotbar slot under the
            // pointer, including the separator rows, the side margins and the strip under the hotbar.
            int dead = 0;
            StringBuilder sweep = new StringBuilder();
            for (int row = 170; row <= 207; row++) {
                StringBuilder line = new StringBuilder();
                for (int column = 0; column < 9; column++) {
                    for (int i = 0; i < 9; i++) client.player.getInventory().setItem(i, ItemStack.EMPTY);
                    screen.getMenu().setCarried(new ItemStack(Items.STONE));
                    double cx = left + 16 + column * 18, cy = top + row;
                    screen.mouseClicked(cx, cy, 0); screen.mouseReleased(cx, cy, 0);
                    boolean placed = client.player.getInventory().getItem(column).is(Items.STONE);
                    line.append(placed ? 'P' : screen.getMenu().getCarried().isEmpty() ? 'D' : '-');
                    if (row >= 178 && !placed) dead++;
                }
                if (row % 8 == 0 || row >= 178) sweep.append(row).append(':').append(line).append(' ');
            }
            for (int margin : new int[] {4, 172}) {
                for (int row = 179; row <= 201; row++) {
                    for (int i = 0; i < 9; i++) client.player.getInventory().setItem(i, ItemStack.EMPTY);
                    screen.getMenu().setCarried(new ItemStack(Items.STONE));
                    screen.mouseClicked(left + margin, top + row, 0); screen.mouseReleased(left + margin, top + row, 0);
                    if (!client.player.getInventory().getItem(margin == 4 ? 0 : 8).is(Items.STONE)) dead++;
                }
            }
            System.out.println("[hex-gap-sweep] " + sweep);
            check(dead == 0, "all 316 catalogue-to-hotbar band clicks land in the hotbar slot under the pointer"); return null;
        });
        await(() -> call(server, () -> java.util.stream.IntStream.range(0, 9).allMatch(i -> player(server).getInventory().getItem(i).is(Items.STONE))), 10);
        // Posted Win32 modifier messages do not change physical Shift state. Exercise the exact
        // QUICK_MOVE dispatch produced by native Shift-click, not global keyboard injection.
        call(client, () -> {
            var screen = (LegacyCreativeScreen)client.screen; var access = (ContainerScreenAccess)(Object)screen;
            for (int col = 0; col < 9; col++)
                access.historicalExperience$slotClicked(screen.getMenu().getSlot(72 + col), 72 + col, 0, ClickType.QUICK_MOVE);
            return null;
        });
        await(() -> call(server, () -> java.util.stream.IntStream.range(0, 9).allMatch(i -> player(server).getInventory().getItem(i).isEmpty())), 10);
        check(call(server, () -> stoneDrops(server)) == before, "native Shift-click QUICK_MOVE dispatch deletes all hotbar stacks on the server without dropping items");
    }
    private static boolean toggleChecked;
    private static final java.util.concurrent.atomic.AtomicReference<String> LAST_SCREEN =
        new java.util.concurrent.atomic.AtomicReference<>("");

    /**
     * Vanilla's own warning that the chosen preset's data packs carry experimental flags, which the historical
     * presets do. The probe drives vanilla's real create-world flow, so it answers the same dialog a player would
     * by pressing its Yes button - and nothing else. No legal or EULA screen is accepted here, and any other screen
     * is reported instead of being dismissed.
     */
    public static boolean acceptVanillaWorldWarning(Minecraft client) {
        var screen = client.screen;
        String state = screen == null ? "<none>"
            : screen.getClass().getName() + ": " + screen.getTitle().getString();
        if (!state.equals(LAST_SCREEN.getAndSet(state))) {
            var buttons = screen == null ? java.util.List.of()
                : Screens.getButtons(screen).stream().map(w -> w.getMessage().getString() + " active=" + w.active).toList();
            System.out.println("[hex-probe-screen] " + state + " buttons=" + buttons);
        }
        if (screen instanceof ConfirmScreen
                && screen.getTitle().getString().equals("Warning! These settings are using experimental features")) {
            press(client, Component.translatable("gui.yes").getString());
            return true;
        }
        return false;
    }

    /**
     * Exercises the ON/OFF switch beside the version button through real screen input, once per process.
     *
     * <p>What it has to prove is the owner's request: the switch can deselect the historical version without losing
     * which one was chosen, and switching it back on restores that same version - so a player never has to walk the
     * picker again just to stop using a version for one world.
     */
    static void toggleDeselect(Minecraft client, String selection) {
        if (toggleChecked || selection.isEmpty()) return;
        toggleChecked = true;
        var toggle = findToggleButton(client);
        var version = findVersionButton(client);
        check(toggle != null && version != null, "footer has both the version button and its toggle");
        check(version.active && CreateWorldHistoricalVersion.Selection.enabled(), "toggle starts on with a version chosen");
        check(toggle.getY() == version.getY() && toggle.getHeight() == version.getHeight(),
            "toggle is the same height as the version button and level with it");
        check(toggle.getMessage().getString().equals("ON"), "toggle is a plain ON/OFF button, showing ON");
        check(toggle.getX() == version.getX() + version.getWidth() + CreateWorldHistoricalVersion.ROW_GAP
            && version.getWidth() + toggle.getWidth() + CreateWorldHistoricalVersion.ROW_GAP
                == CreateWorldHistoricalVersion.ROW_WIDTH,
            "toggle sits beside the button with a " + CreateWorldHistoricalVersion.ROW_GAP + "px gap and the row is "
                + CreateWorldHistoricalVersion.ROW_WIDTH + "px");
        // The owner's complaint, as a check: the row must line up with vanilla's own create/cancel row, not merely be
        // close to it. Both edges are compared against the real buttons, so any future divergence fails here.
        var create = findButtonNamed(client, Component.translatable("selectWorld.create").getString());
        var cancel = findButtonNamed(client, "Cancel");
        check(create != null && cancel != null, "vanilla create/cancel row is present to align against");
        check(version.getX() == create.getX(),
            "version button's left edge matches Create New World exactly (" + version.getX() + " vs " + create.getX() + ")");
        check(toggle.getX() + toggle.getWidth() == cancel.getX() + cancel.getWidth(),
            "toggle's right edge matches Cancel exactly (" + (toggle.getX() + toggle.getWidth()) + " vs "
                + (cancel.getX() + cancel.getWidth()) + ")");
        clickWidget(client, toggle);
        check(!CreateWorldHistoricalVersion.Selection.enabled(), "clicking the toggle turns the historical version off");
        check(toggle.getMessage().getString().equals("OFF"), "the toggle now reads OFF");
        check(CreateWorldHistoricalVersion.Selection.key().equals(selection), "turning it off keeps the chosen version remembered");
        check(!version.active, "version button is greyed out while the historical version is off");
        check(presetId(client).equals("minecraft:normal"), "off restores vanilla's ordinary world type, found " + presetId(client));
        clickWidget(client, toggle);
        check(CreateWorldHistoricalVersion.Selection.enabled() && CreateWorldHistoricalVersion.Selection.key().equals(selection),
            "turning it back on restores the remembered choice without another trip through the picker");
        check(presetId(client).equals(net.historicalexperience.version.HistoricalSnapshotPatches
            .baseProfileForSelection(selection).orElseThrow()), "on re-applies the historical preset, found " + presetId(client));
        check(version.active, "version button is live again once the toggle is on");
        Screenshot.grab(client.gameDirectory, "create-world-toggle-" + System.currentTimeMillis() + ".png",
            client.getMainRenderTarget(), message -> System.out.println("[hex-snapshot] " + message.getString()));
    }

    private static String presetId(Minecraft client) {
        var type = ((CreateWorldScreen) client.screen).getUiState().getWorldType();
        return type == null || type.preset() == null ? "" : type.preset().unwrapKey().map(k -> k.location().toString()).orElse("");
    }

    private static Button findVersionButton(Minecraft client) {
        for (var widget : Screens.getButtons(client.screen))
            if (widget instanceof Button button && button.getMessage().getString().startsWith("Historical Version:")) return button;
        return null;
    }

    /** The toggle is the only button on this screen whose whole label is ON or OFF. */
    private static Button findToggleButton(Minecraft client) {
        for (var widget : Screens.getButtons(client.screen))
            if (widget instanceof Button button) {
                String text = button.getMessage().getString();
                if (text.equals("ON") || text.equals("OFF")) return button;
            }
        return null;
    }

    private static Button findButtonNamed(Minecraft client, String label) {
        for (var widget : Screens.getButtons(client.screen))
            if (widget instanceof Button button && button.getMessage().getString().equals(label)) return button;
        return null;
    }

    /** Real screen dispatch at the widget's own centre, so its hit box and not only its callback is under test. */
    private static void clickWidget(Minecraft client, net.minecraft.client.gui.components.AbstractWidget widget) {
        double x = widget.getX() + widget.getWidth() / 2.0, y = widget.getY() + widget.getHeight() / 2.0;
        client.screen.mouseClicked(x, y, 0);
        client.screen.mouseReleased(x, y, 0);
    }

    static void pressB(Minecraft client) throws Exception {
        call(client, () -> { GLFW.glfwFocusWindow(client.getWindow().getWindow()); return null; });
        await(() -> call(client, client::isWindowActive), 10);
        try {
            call(client, () -> { check(User32.PostMessage(GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow()), User32.WM_KEYDOWN, 0x42, 0x00300001L), "B keydown posted only to Minecraft window"); return null; });
            Thread.sleep(600);
        } finally { call(client, () -> { User32.PostMessage(GLFWNativeWin32.glfwGetWin32Window(client.getWindow().getWindow()), User32.WM_KEYUP, 0x42, 0xc0300001L); return null; }); }
        Thread.sleep(150); // Let the real tick adapter observe release before another synthetic press.
    }
    public static void press(Minecraft client, String label) {
        for (var widget : Screens.getButtons(client.screen)) if (widget instanceof Button button && button.active && button.getMessage().getString().contains(label)) { button.onPress(); return; }
        throw new IllegalStateException("No active button containing: " + label);
    }
    public static void check(boolean result, String message) { if (!result) throw new IllegalStateException(message); CHECKS.add(message); System.out.println("[hex-snapshot] PASS " + message); }
    /** The checks recorded so far, for a probe that keeps its own receipt. */
    public static java.util.List<String> checks() { return java.util.List.copyOf(CHECKS); }
    public static <T> T call(Executor executor, Callable<T> action) throws Exception {
        var future = new CompletableFuture<T>(); executor.execute(() -> { try { future.complete(action.call()); } catch (Throwable failure) { future.completeExceptionally(failure); } });
        return future.get(45, TimeUnit.SECONDS);
    }
    public static void await(Callable<Boolean> condition, int seconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) { if (condition.call()) return; Thread.sleep(100); }
        throw new TimeoutException("Snapshot native acceptance condition exceeded " + seconds + " seconds");
    }
}

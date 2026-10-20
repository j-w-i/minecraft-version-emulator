/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.client.version;

import static net.historicalexperience.client.version.SnapshotFeatureProbe.*;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.gameplay.HistoricalCreativeGrants;
import net.historicalexperience.mixin.client.CreativeScreenProbeAccess;
import net.historicalexperience.server.*;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/** Four owned native worlds: patch12, unpatched1.4/1.1, ordinary. No legal/account/personal data changes. */
public final class SnapshotAnvilProbe {
    private static final String PROP = "historical_experience.snapshotAnvilProbe";
    private static final String P = "historical_experience:";
    private static final List<String> CHECKS = new CopyOnWriteArrayList<>();
    private static boolean started;
    private SnapshotAnvilProbe() { }
    public static void register() {
        if (!Boolean.getBoolean(PROP)) return;
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (started || !(screen instanceof TitleScreen)) return;
            started = true; var thread = new Thread(() -> drive(client), "hex-snapshot-anvil-probe"); thread.setDaemon(true); thread.start();
        });
    }
    private static void drive(Minecraft client) {
        Path root = client.gameDirectory.toPath().toAbsolutePath().normalize();
        String approved = System.getProperty("historical_experience.snapshotProbe.root");
        if (approved == null || !root.equals(Path.of(approved).toAbsolutePath().normalize())) { client.execute(client::stop); return; }
        String stamp = Long.toString(System.currentTimeMillis());
        boolean passed = false; Throwable failure = null;
        boolean villageOnly = Boolean.getBoolean("historical_experience.villageResidentsProbe");
        try {
            var selections = villageOnly ? List.of(P + "release_1_1", P + "beta_1_8_1", P + "release_1_4", "")
                : List.of(P + "snapshot_12w49a", P + "release_1_4", P + "release_1_1", "");
            for (String selection : selections) {
                String suffix = selection.isEmpty() ? "ordinary" : selection.substring(selection.indexOf(':') + 1);
                String name = "sol-snapshot-controls-" + stamp + "-" + suffix;
                create(client, selection, name);
                var server = client.getSingleplayerServer();
                call(server, () -> {
                    var metadata = HistoricalServerLifecycle.readMetadata(HistoricalServerLifecycle.metadataFileFor(server));
                    if (selection.isEmpty()) check(metadata.isEmpty(), suffix + " no historical metadata");
                    else check(metadata.orElseThrow().selectionId().equals(selection), suffix + " real UI selection/native bootstrap identity");
                    check(!server.getWorldData().isAllowCommands() && actor(server).gameMode.getGameModeForPlayer() == GameType.SURVIVAL, suffix + " starts Survival without cheats"); return null;
                });
                if (villageOnly) {
                    if (selection.endsWith("release_1_1") || selection.endsWith("beta_1_8_1")) {
                        var natural = call(server, () -> net.historicalexperience.server.VillageResidentAcceptance.generateNatural(server, selection));
                        CHECKS.add(natural.description());
                        Thread.sleep(2000);
                        await(() -> call(server, () -> natural.count() == natural.expected()), 30);
                        check(call(server, () -> natural.count()) == natural.expected(), natural.description() + " actual loaded villager count=" + natural.expected());
                    }
                    CHECKS.addAll(call(server, () -> net.historicalexperience.server.VillageResidentAcceptance.run(server, selection)));
                    disconnect(client, suffix);
                    continue;
                }
                pressB(client);
                call(client, () -> { ClientPlayNetworking.send(new SnapshotCreativeRequest.Payload()); return null; });
                Thread.sleep(800);
                call(server, () -> { check(actor(server).gameMode.getGameModeForPlayer() == GameType.SURVIVAL, suffix + " real B and forged B request cannot activate11w49a in another scope"); return null; });
                boolean patch = selection.endsWith("snapshot_12w49a");
                // Constructed-menu baseline control, not a claim that the era had obtainable anvils.
                // The existing menu guard intentionally audits only the frozen early-artifact families.
                boolean anvilExists = !net.historicalexperience.gameplay.HistoricalEarlyMenuRules.blocksResult(
                    net.historicalexperience.gameplay.HistoricalProfileLookup.profileIdForLevel(server.overworld()), "anvil");
                call(server, () -> { directAnvils(server, patch, anvilExists, suffix); return null; });
                if (patch) liveAnvil(client, server);
                call(server, () -> { actor(server).setGameMode(GameType.CREATIVE); return null; });
                await(() -> call(client, () -> client.gameMode.hasInfiniteItems()), 10);
                if (!selection.endsWith("release_1_1")) picker(client, selection, name);
                chestConservation(client, server, !selection.isEmpty(), suffix);
                disconnect(client, suffix);
            }
            passed = true;
        } catch (Throwable problem) { failure = problem; problem.printStackTrace(); }
        finally {
            try {
                var result = new LinkedHashMap<String, Object>(); result.put("passed", passed); result.put("run", stamp); result.put("checks", CHECKS); result.put("failure", failure == null ? null : failure.toString());
                Files.writeString(root.resolve((villageOnly ? "village-resident-probe-" : "snapshot-anvil-probe-") + stamp + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
            } catch (Exception problem) { problem.printStackTrace(); }
            client.execute(client::stop);
        }
    }
    private static void disconnect(Minecraft client, String suffix) throws Exception {
        call(client, () -> { client.level.disconnect(); client.disconnect(new TitleScreen()); return null; });
        await(() -> call(client, () -> client.getSingleplayerServer() == null && client.getOverlay() == null), 60);
        Thread.sleep(2000);
        check(HistoricalClientRules.activeSnapshotPatchId().isEmpty(), suffix + " disconnect clears client snapshot scope");
    }
    private static void create(Minecraft client, String selection, String name) throws Exception {        call(client, () -> { client.options.renderDistance().set(2); client.options.simulationDistance().set(5); CreateWorldScreen.openFresh(client, new TitleScreen()); return null; });
        await(() -> call(client, () -> client.screen instanceof CreateWorldScreen), 90);
        call(client, () -> {
            var create = (CreateWorldScreen) client.screen;
            create.getUiState().setName(name); create.getUiState().setSeed("12491414");
            create.getUiState().setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL); create.getUiState().setAllowCommands(false);
            if (!selection.isEmpty()) press(client, "Historical Version:"); return null;
        });
        if (!selection.isEmpty()) {
            var entry = HistoricalVersionCatalogue.byKey(selection).orElseThrow();
            call(client, () -> {
                press(client, entry.era().displayName()); boolean chosen = false;
                for (var child : client.screen.children()) if (child instanceof ObjectSelectionList<?> list) for (Object value : list.children()) {
                    var row = (ObjectSelectionList.Entry<?>) value;
                    if (row.getNarration().getString().equals(entry.displayName() + ", implemented")) { row.mouseClicked(0, 0, 0); chosen = true; }
                }
                check(chosen, "selected exact row " + selection); press(client, "Use this version"); return null;
            });
        }
        call(client, () -> { check(CreateWorldHistoricalVersion.Selection.key().equals(selection), "new-world UI has no stale patch: " + selection); press(client, Component.translatable("selectWorld.create").getString()); return null; });
        var lastScreen = new java.util.concurrent.atomic.AtomicReference<String>("");
        await(() -> call(client, () -> {
            String state = client.screen == null ? "<none>" : client.screen.getClass().getName() + ": " + client.screen.getTitle().getString();
            if (!state.equals(lastScreen.getAndSet(state))) {
                String buttons = client.screen == null ? "" : net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(client.screen).stream().map(w -> w.getMessage().getString() + " active=" + w.active).toList().toString();
                System.out.println("[hex-village-load] " + state + " buttons=" + buttons);
            }
            // Only this known worldgen-stability warning in the explicitly armed, root-pinned fresh-world probe.
            // Never a generic confirmation or legal/EULA acceptance.
            if (client.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen
                && client.screen.getTitle().getString().equals("Warning! These settings are using experimental features")) {
                press(client, Component.translatable("gui.yes").getString()); return false;
            }
            return client.level != null && client.player != null && client.screen == null && client.getSingleplayerServer() != null;
        }), 180);
        call(client.getSingleplayerServer(), () -> { check(client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().startsWith(client.gameDirectory.toPath().toAbsolutePath().normalize().resolve("saves")), "owned project save directory"); return null; });
    }
    private static ServerPlayer actor(MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
    private static ItemStack book(ServerPlayer player) {
        var book = new ItemStack(Items.ENCHANTED_BOOK); var values = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        values.set(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, values.toImmutable()); return book;
    }
    private static AnvilMenu assembled(ServerPlayer player, ItemStack target) {
        var menu = new AnvilMenu(91, player.getInventory()); menu.getSlot(0).set(target.copy()); menu.getSlot(1).set(book(player)); menu.createResult(); return menu;
    }
    private static void directAnvils(MinecraftServer server, boolean patch, boolean exists, String label) {
        var player = actor(server); player.setExperienceLevels(30);
        for (var item : List.of(Items.DIRT, Items.GLASS, Items.BOWL, Items.APPLE)) {
            var menu = assembled(player, new ItemStack(item));
            check(!menu.getSlot(2).getItem().isEmpty() == patch, label + " Survival single-target eligibility " + item);
            check(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(1).getItem().getCount() == 1 && player.experienceLevel == 30, label + " preview consumes nothing");
        }
        var tool = assembled(player, new ItemStack(Items.DIAMOND_PICKAXE));
        check(!tool.getSlot(2).getItem().isEmpty() == exists, label + " ordinary eligible tool follows anvil availability");
        for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
            player.setGameMode(mode); var menu = assembled(player, new ItemStack(Items.DIRT, 2));
            if (patch || !exists || mode == GameType.SURVIVAL) check(menu.getSlot(2).getItem().isEmpty(), label + " stacked target refusal " + mode);
            check(menu.getSlot(0).getItem().getCount() == 2 && menu.getSlot(1).getItem().getCount() == 1, label + " stacked inputs retained " + mode);
        }
        player.setGameMode(GameType.SURVIVAL);
        if (patch) {
            var target = new ItemStack(Items.DIAMOND_PICKAXE);
            var original = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            original.set(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 1);
            target.set(DataComponents.ENCHANTMENTS, original.toImmutable());
            var incompatible = book(player); var other = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            other.set(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            incompatible.set(DataComponents.STORED_ENCHANTMENTS, other.toImmutable());
            var menu = new AnvilMenu(92, player.getInventory());
            menu.getSlot(0).set(target.copy()); menu.getSlot(1).set(incompatible.copy()); menu.createResult();
            check(menu.getSlot(2).getItem().isEmpty() && ItemStack.matches(menu.getSlot(0).getItem(), target)
                && ItemStack.matches(menu.getSlot(1).getItem(), incompatible), "12w49a eligibility patch retains native incompatibility and original input components");
        }
    }
    private static void liveAnvil(Minecraft client, MinecraftServer server) throws Exception {
        ItemStack target = new ItemStack(Items.DIRT);
        var tag = new CompoundTag(); tag.putString("owner", "native12-book-output"); target.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); target.set(DataComponents.CUSTOM_NAME, Component.literal("Anvil sentinel"));
        call(server, () -> {
            var player = actor(server); player.setExperienceLevels(30);
            player.openMenu(new SimpleMenuProvider((id, inv, who) -> new AnvilMenu(id, inv), Component.literal("12w49a native anvil")));
            player.containerMenu.getSlot(0).set(target.copy()); player.containerMenu.getSlot(1).set(book(player)); player.containerMenu.broadcastChanges(); return null;
        });
        await(() -> call(client, () -> client.screen instanceof AnvilScreen screen && !screen.getMenu().getSlot(2).getItem().isEmpty() && screen.getMenu().getCost() > 0), 10);
        int cost = call(server, () -> ((AnvilMenu)actor(server).containerMenu).getCost());
        check(cost > 0 && cost <= 30, "12w49a native displayed XP cost is payable, not waived");
        call(client, () -> { var menu = client.player.containerMenu; client.gameMode.handleInventoryMouseClick(menu.containerId, 2, 0, ClickType.PICKUP, client.player); return null; });
        await(() -> call(server, () -> actor(server).containerMenu.getSlot(0).getItem().isEmpty() && actor(server).containerMenu.getSlot(1).getItem().isEmpty()), 10);
        call(server, () -> {
            var player = actor(server); var output = player.containerMenu.getCarried();
            check(output.is(Items.DIRT) && output.getCount() == 1, "real C2S result click returns exactly one dirt");
            check(Objects.equals(output.get(DataComponents.CUSTOM_DATA), target.get(DataComponents.CUSTOM_DATA)) && Objects.equals(output.get(DataComponents.CUSTOM_NAME), target.get(DataComponents.CUSTOM_NAME)), "native anvil preserves target custom data/name");
            check(output.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING)) == 1, "non-enchantable block actually gains book enchantment");
            check(player.experienceLevel == 30 - cost, "real result pickup charges exactly displayed XP and consumes book/target"); return null;
        });
        call(client, () -> { client.player.closeContainer(); return null; });
        await(() -> call(server, () -> actor(server).containerMenu == actor(server).inventoryMenu), 10);
    }
    private static void picker(Minecraft client, String selection, String name) throws Exception {
        call(client, () -> {
            client.setScreen(new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false));
            check(client.screen instanceof CreativeModeInventoryScreen, "1.3+ and ordinary use replacement native picker");
            var screen = (CreativeModeInventoryScreen) client.screen;
            int globalBefore = CreativeModeTabs.searchTab().getDisplayItems().size(); int examined = 0;
            for (var tab : CreativeModeTabs.tabs()) {
                if (tab.getType() == CreativeModeTab.Type.INVENTORY || tab.getType() == CreativeModeTab.Type.HOTBAR) continue;
                ((CreativeScreenProbeAccess)(Object)screen).historicalExperience$selectTab(tab);
                check(screen.getMenu().items.stream().allMatch(stack -> HistoricalCreativeGrants.allowed(selection, stack)), "native working tab filters dated items/variants " + tab.getDisplayName().getString()); examined++;
            }
            check(examined > 3 && CreativeModeTabs.searchTab().getDisplayItems().size() == globalBefore, "actual tab changes never mutate shared search catalog");
            ((CreativeScreenProbeAccess)(Object)screen).historicalExperience$selectTab(CreativeModeTabs.searchTab());
            check(screen.getMenu().items.stream().anyMatch(stack -> stack.is(Items.NETHERITE_SWORD)) == selection.isEmpty(), "historical search excludes future items; subsequent ordinary search restores them");
            Screenshot.grab(client.gameDirectory, name + "-picker.png", client.getMainRenderTarget(), message -> System.out.println("[hex-anvil] " + message.getString())); return null;
        });
        call(client, () -> { client.player.closeContainer(); return null; });
    }
    private static void chestConservation(Minecraft client, MinecraftServer server, boolean historical, String label) throws Exception {
        ItemStack sentinel = new ItemStack(Items.NETHERITE_SWORD); var tag = new CompoundTag(); tag.putString("owner", label); sentinel.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); sentinel.setDamageValue(137);
        var chest = new SimpleContainer(27);
        call(server, () -> {
            actor(server).getInventory().setItem(8, sentinel.copy()); actor(server).openMenu(new SimpleMenuProvider((id, inv, who) -> ChestMenu.threeRows(id, inv, chest), Component.literal("Conservation probe"))); return null;
        });
        await(() -> call(client, () -> client.player.containerMenu instanceof ChestMenu), 10);
        click(client, 62, 0, ClickType.PICKUP);
        await(() -> call(server, () -> ItemStack.matches(actor(server).containerMenu.getCarried(), sentinel) && actor(server).getInventory().getItem(8).isEmpty()), 10);
        check(true, label + " normal Creative container pickup moves unsupported owned content intact");
        if (historical) {
            for (int stage : List.of(0, 1, 2)) {
                int button = AbstractContainerMenu.getQuickcraftMask(stage, AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE);
                int slot = stage == 1 ? 1 : -999;
                call(client, () -> { var menu = client.player.containerMenu; client.player.connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slot, button, ClickType.QUICK_CRAFT, sentinel.copy(), new Int2ObjectOpenHashMap<>())); return null; });
            }
            Thread.sleep(500);
            call(server, () -> { check(chest.getItem(1).isEmpty() && ItemStack.matches(actor(server).containerMenu.getCarried(), sentinel), label + " clone-style quick-craft cannot duplicate unsupported carried content"); return null; });
        }
        click(client, 0, 0, ClickType.PICKUP);
        await(() -> call(server, () -> ItemStack.matches(chest.getItem(0), sentinel) && actor(server).containerMenu.getCarried().isEmpty()), 10);
        if (!historical) {
            click(client, 0, 2, ClickType.CLONE);
            await(() -> call(server, () -> ItemStack.matches(chest.getItem(0), sentinel) && ItemStack.matches(actor(server).containerMenu.getCarried(), sentinel)), 10);
            check(true, "ordinary Creative CLONE still works as positive control");
            call(server, () -> { actor(server).containerMenu.setCarried(ItemStack.EMPTY); actor(server).containerMenu.broadcastFullState(); return null; });
            await(() -> call(client, () -> client.player.containerMenu.getCarried().isEmpty()), 10);
        }
        click(client, 0, 0, ClickType.QUICK_MOVE);
        await(() -> call(server, () -> chest.getItem(0).isEmpty() && ItemStack.matches(actor(server).getInventory().getItem(8), sentinel)), 10);
        check(true, label + " normal chest shift-transfer returns exact count/damage/custom data");
        call(client, () -> { client.player.closeContainer(); return null; });
    }
    private static void click(Minecraft client, int slot, int button, ClickType type) throws Exception {
        call(client, () -> { client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, slot, button, type, client.player); return null; });
    }
    private static void check(boolean value, String description) { if (!value) throw new IllegalStateException(description); CHECKS.add(description); System.out.println("[hex-anvil] PASS " + description); }
}

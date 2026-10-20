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
package net.historicalexperience.client.textures;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.client.colour.HistoricalColormaps;
import net.historicalexperience.version.HistoricalVersion;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the historical texture layer: what it should contain for the current world, building it, and reloading the
 * client's resources when that changes.
 *
 * <h2>The integration point, and why it is the smallest one</h2>
 *
 * <p>Minecraft builds the list of packs for a reload from {@code PackRepository.openAllSelected()}, which returns one
 * {@code PackResources} per <i>selected</i> pack in priority order, lowest first. A mixin on that method appends one
 * more entry - {@link HistoricalTexturePackResources} - for the client's own repository. Nothing about the selection is
 * touched: the repository's selected list, {@code options.txt}, and the resource-pack screen are all exactly what the
 * player left them, which is why enforcement can be switched off and leave no trace.
 *
 * <p>Because the layer is last it outranks every user pack, but it only answers the resources the historical stack
 * defines; everything else falls through to the player's packs and vanilla unchanged.
 *
 * <h2>When resources reload</h2>
 *
 * <p>The desired effective stack is a pure function of (enforcement setting, active historical version, which packs are
 * installed). {@link #tick} compares that to what the last reload actually built and reloads only when they differ, so:
 * adjacent versions with the same stack never reload; leaving a world, joining one, a multiplayer version push, a
 * committed version upgrade and the Mod Menu toggle are all handled by the one comparison; and a failed reload is not
 * retried in a loop for the same desired stack.
 */
public final class HistoricalTextureService {
    private static final Logger LOGGER = LoggerFactory.getLogger("historical_experience/textures");
    private static final String PROGRAMMER_ART_PACK = "programmer_art";
    private static final int CHECK_INTERVAL_TICKS = 10;

    private static volatile PackScanner.Result scan;
    private static volatile HistoricalTexturePackResources active;
    private static volatile String appliedSignature = "none";
    private static volatile String failedSignature;
    private static volatile boolean reloadPending;
    private static int ticks;
    private static int reloads;
    private static boolean missingNotified;

    private HistoricalTextureService() {
    }

    // ---- scanning --------------------------------------------------------------------------------------

    public static int gamePackFormat() {
        return SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
    }

    /** The cached scan, taken on first use. Synchronised so concurrent first calls cannot each run their own scan. */
    public static PackScanner.Result scan() {
        PackScanner.Result current = scan;
        if (current != null) {
            return current;
        }
        synchronized (HistoricalTextureService.class) {
            return scan != null ? scan : rescan();
        }
    }

    /**
     * Where the dependency packs are looked for: the game's own resource-pack folder, or - for development and
     * verification runs only - the folder named by {@code -Dhistorical_experience.packDirectory}, which lets a test
     * client read the packs the player already has installed in place instead of duplicating them anywhere.
     */
    public static Path packDirectory() {
        String override = System.getProperty("historical_experience.packDirectory");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        Minecraft client = Minecraft.getInstance();
        return client == null ? null : client.getResourcePackDirectory();
    }

    /**
     * The mod's own folder for the packs it downloads. It is deliberately not the player's resource-pack folder: the
     * downloaded copies never appear in the resource-pack menu, are never selectable there, and cannot be confused with
     * (or duplicated beside) packs the player installed themselves.
     */
    public static Path managedDirectory() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
            .resolve("historical_experience").resolve("texture_packs");
    }

    /** Directories searched for the dependency packs, best first: the verified downloads, then the player's folder. */
    static List<Path> searchDirectories() {
        List<Path> directories = new ArrayList<>();
        directories.add(managedDirectory());
        directories.add(packDirectory());
        return directories;
    }

    private static int scanCount;

    public static PackScanner.Result rescan() {
        if (++scanCount > 1) {
            // Diagnostic: a scan is cached, so a repeat means a caller asked for one on purpose (Rescan, a finished
            // download) - or that the cache is being bypassed, which this trace would show.
            StackTraceElement[] stack = new Throwable().getStackTrace();
            StringBuilder caller = new StringBuilder();
            for (int i = 1; i < Math.min(stack.length, 5); i++) {
                caller.append("\n    at ").append(stack[i]);
            }
            LOGGER.info("Repeat texture-pack scan #{} requested by:{}", scanCount, caller);
        }
        List<Path> directories = searchDirectories();
        PackScanner.Result result = PackScanner.scan(directories, gamePackFormat());
        scan = result;
        missingNotified = false;
        LOGGER.info("Scanned {} for historical texture packs: {}", directories, summary(result));
        return result;
    }

    // ---- automatic download ----------------------------------------------------------------------------

    private static volatile PackDownloader downloader;
    private static boolean autoDownloadAttempted;

    /** The downloader, created on first use against the managed folder. */
    public static synchronized PackDownloader downloader() {
        if (downloader == null) {
            downloader = new PackDownloader(managedDirectory(), PackDownloader.httpsFetcher());
        }
        return downloader;
    }

    /** The dependency packs that are not currently usable, in layering order. */
    public static List<TextureSource> missingDependencies() {
        PackScanner.Result current = scan();
        List<TextureSource> missing = new ArrayList<>();
        for (TextureSource source : TextureSource.values()) {
            if (source.isDependency() && source != TextureSource.PROGRAMMER_ART) {
                PackScanner.Found found = current.of(source);
                if (found == null || !found.usable()) {
                    missing.add(source);
                }
            }
        }
        return missing;
    }

    /**
     * Downloads whichever pinned packs are missing, then rescans on the client thread so the next reconcile picks them
     * up and reloads. No-op when nothing is missing or a download is already running. Returns whether one was started.
     */
    public static boolean downloadMissing(Minecraft client) {
        PackDownloader loader = downloader();
        List<TextureSource> missing = missingDependencies();
        if (missing.isEmpty() || loader.isRunning()) {
            return false;
        }
        LOGGER.info("Downloading pinned texture packs from Modrinth: {}", missing);
        loader.start(missing, () -> client.execute(() -> {
            rescan();
            LOGGER.info("Texture pack download finished; usable now: {}", scan().usable().keySet());
        }));
        return true;
    }

    /**
     * Once per session, when automatic download is on and a required pack is missing, fetch it. Runs from the tick so
     * players who completed setup in an earlier session (or deleted a pack) are repaired without being asked again.
     */
    private static void maybeAutoDownload(Minecraft client) {
        if (autoDownloadAttempted || !HistoricalTextureConfig.enabled() || !HistoricalTextureConfig.autoDownload()
            || !HistoricalTextureConfig.setupCompleted()) {
            return;
        }
        autoDownloadAttempted = true;
        downloadMissing(client);
    }

    private static String summary(PackScanner.Result result) {
        StringBuilder out = new StringBuilder();
        result.found().forEach((source, found) -> out.append(source.id()).append('=')
            .append(found.usable() ? found.chosen().getFileName() : "MISSING").append(' '));
        return out.toString().trim();
    }

    // ---- the plan --------------------------------------------------------------------------------------

    /** What the stack should be right now. {@link TexturePlan#NONE} when disabled or the version is not known. */
    public static TexturePlan desiredPlan() {
        if (!HistoricalTextureConfig.enabled()) {
            return TexturePlan.NONE;
        }
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        return HistoricalTextureRules.planFor(version);
    }

    /** The sources that can actually be opened right now. */
    static Set<TextureSource> availableSources(PackRepository repository) {
        Set<TextureSource> available = EnumSet.of(TextureSource.VANILLA_PIN);
        if (repository != null && repository.getPack(PROGRAMMER_ART_PACK) != null) {
            available.add(TextureSource.PROGRAMMER_ART);
        }
        available.addAll(scan().usable().keySet());
        return available;
    }

    /** Identity of the effective stack: the plan, restricted to what is installed. */
    static String signature(TexturePlan plan, Set<TextureSource> available) {
        if (!plan.isActive()) {
            return "none";
        }
        List<String> have = new ArrayList<>();
        for (TextureSource source : plan.sources()) {
            have.add(source.id() + (available.contains(source) ? "" : "(missing)"));
        }
        return String.join("+", have) + "|" + plan.signature();
    }

    public static String desiredSignature() {
        Minecraft client = Minecraft.getInstance();
        PackRepository repository = client == null ? null : client.getResourcePackRepository();
        return signature(desiredPlan(), availableSources(repository));
    }

    public static String appliedSignature() {
        return appliedSignature;
    }

    /** The layer from the last reload, or null when the last reload built none. */
    public static HistoricalTexturePackResources activeLayer() {
        return active;
    }

    public static int reloadCount() {
        return reloads;
    }

    /** Whether a reload started by this service is still in flight. */
    public static boolean reloadPendingNow() {
        return reloadPending;
    }

    // ---- building the layer (called from the PackRepository mixin during a reload) -------------------------

    /** True only for the client's own resource-pack repository; datapack repositories must never see this layer. */
    public static boolean isClientRepository(PackRepository repository) {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.getResourcePackRepository() == repository;
    }

    /**
     * Builds the layer for the current desired plan, or returns null when none should be added. Never throws: a layer
     * that fails to build must not turn into a failed reload, because the game answers a failed reload by deselecting
     * every pack the player has chosen.
     */
    public static PackResources buildLayer(PackRepository repository) {
        try {
            TexturePlan plan = desiredPlan();
            Set<TextureSource> available = availableSources(repository);
            appliedSignature = signature(plan, available);
            active = null;
            if (!plan.isActive()) {
                return null;
            }
            Map<TextureSource, PackResources> opened = new EnumMap<>(TextureSource.class);
            Set<TextureSource> owned = EnumSet.noneOf(TextureSource.class);
            Map<TextureSource, Path> installed = scan().usable();
            for (TextureSource source : plan.sources()) {
                PackResources resources = null;
                boolean shared = false;
                try {
                    switch (source) {
                        case VANILLA_PIN -> {
                            resources = Minecraft.getInstance().getVanillaPackResources();
                            shared = true;
                        }
                        case PROGRAMMER_ART -> {
                            Pack pack = repository.getPack(PROGRAMMER_ART_PACK);
                            resources = pack == null ? null : pack.open();
                        }
                        default -> {
                            Path path = installed.get(source);
                            resources = path == null ? null : openInstalled(source, path);
                        }
                    }
                } catch (RuntimeException failure) {
                    LOGGER.warn("Could not open the {} layer; continuing without it", source.id(), failure);
                }
                if (resources != null) {
                    opened.put(source, resources);
                    if (!shared) {
                        owned.add(source);
                    }
                }
            }
            HistoricalTexturePackResources layer = new HistoricalTexturePackResources(plan, opened, owned);
            active = layer;
            LOGGER.info("Historical textures active: stack={} opened={}", plan.signature(), layer.openedSources());
            return layer;
        } catch (RuntimeException failure) {
            LOGGER.error("Historical texture layer failed to build; leaving ordinary resource behaviour", failure);
            appliedSignature = "none";
            active = null;
            return null;
        }
    }

    /**
     * Opens an installed dependency the way the game opens a selected pack, so {@code pack.mcmeta} overlays and format
     * ranges are honoured. The archive itself is only ever read.
     */
    private static PackResources openInstalled(TextureSource source, Path path) {
        PackLocationInfo info = new PackLocationInfo("historical_experience/" + source.id(),
            Component.literal(path.getFileName().toString()), PackSource.DEFAULT, Optional.empty());
        Pack.ResourcesSupplier supplier = java.nio.file.Files.isDirectory(path)
            ? new PathPackResources.PathResourcesSupplier(path)
            : new FilePackResources.FileResourcesSupplier(path);
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES,
            new PackSelectionConfig(false, Pack.Position.TOP, false));
        if (pack == null) {
            throw new IllegalStateException("unreadable pack metadata: " + path);
        }
        return pack.open();
    }

    // ---- the reload reconciler -------------------------------------------------------------------------

    /** Called every client tick; does real work every {@value #CHECK_INTERVAL_TICKS} ticks. */
    public static void tick(Minecraft client) {
        if (++ticks % CHECK_INTERVAL_TICKS != 0 || reloadPending || client.getOverlay() != null) {
            return;
        }
        if (releaseProgrammerArt(client)) {
            // The pack list changed, so the game needs its own reload before anything else is compared.
            return;
        }
        maybeAutoDownload(client);
        // The tint tables are installed while the resource manager reloads, which happens before a world is known, so a
        // change of era needs one more reload. Whole ranges of versions share a table, so only a real change reloads.
        String tint = HistoricalColormaps.desiredEraSignature();
        if (!tint.equals(HistoricalColormaps.appliedEra())) {
            reload(client, "colormaps:" + tint);
            return;
        }
        String desired = desiredSignature();
        if (desired.equals(appliedSignature)) {
            failedSignature = null;
            maybeReportMissing(client);
            return;
        }
        if (desired.equals(failedSignature)) {
            return;
        }
        // Entering a historical world: wait until the world is actually there. Leaving one, or turning enforcement
        // off, can reload straight away.
        if (!desiredPlan().isActive() || client.level != null) {
            reload(client, desired);
        }
    }

    /**
     * The loading overlay of the reload this service started, or null.
     *
     * <p>Kept by identity so only OUR reload is made quiet: a reload the player starts themselves (F3+T, the pack
     * screen) is not in this field and keeps the ordinary Mojang loading screen. The overlay is then drawn by
     * {@code LoadingOverlayHistoricalQuietMixin}, which skips vanilla's logo and progress bar for exactly this one.
     */
    private static volatile Object quietOverlay;

    /** Whether {@code overlay} belongs to a reload this service started and should not draw the Mojang screen. */
    public static boolean isQuietOverlay(Object overlay) {
        return overlay != null && overlay == quietOverlay;
    }

    /**
     * Turns the vanilla Programmer Art pack off when the player has turned historical textures off.
     *
     * <h2>Why this one pack, and why the mod touches the pack list at all</h2>
     *
     * <p>Owner: <i>"When textures are disabled this should include the programmer art pack."</i> Programmer Art is the
     * one source of this feature that changes the game <em>by being selected</em>: the mod's own layer opens it
     * directly (see {@link #buildLayer}), but if the player also has it ticked in the pack list then turning
     * historical textures off leaves it applying on its own - the world still looks like the old textures after the
     * player asked for no historical textures. The other sources (Golden Days, Continuation) are the player's own
     * downloads and are only ever read through the layer, so their selection is left alone.
     *
     * <p>Only reached while the setting is OFF, so a player who has historical textures on keeps whatever pack
     * selection they chose, and an ordinary world with the setting on is untouched - the selection is a client-wide
     * setting and this method refuses to touch it for any other reason.
     *
     * @return whether the pack list was changed, in which case the caller reloads and waits
     */
    private static boolean releaseProgrammerArt(Minecraft client) {
        if (HistoricalTextureConfig.enabled()) {
            return false;
        }
        PackRepository repository = client.getResourcePackRepository();
        if (repository == null) {
            return false;
        }
        java.util.List<String> selected = new java.util.ArrayList<>(repository.getSelectedIds());
        if (!selected.remove(PROGRAMMER_ART_PACK)) {
            return false;
        }
        LOGGER.info("Historical textures are off, so the {} pack is being deselected with them", PROGRAMMER_ART_PACK);
        repository.setSelected(selected);
        // Persisted the way the pack selection screen persists it, so the change survives a restart rather than
        // reappearing on the next launch.
        client.options.updateResourcePacks(repository);
        reload(client, "programmer_art off");
        return true;
    }

    private static void reload(Minecraft client, String desired) {
        reloadPending = true;
        reloads++;
        LOGGER.info("Reloading resources for historical textures: {} -> {}", appliedSignature, desired);
        net.minecraft.client.gui.screens.Overlay before = client.getOverlay();
        java.util.concurrent.CompletableFuture<Void> reloading = client.reloadResourcePacks();
        net.minecraft.client.gui.screens.Overlay after = client.getOverlay();
        // Only claim an overlay this call actually created. If a reload was already running, the overlay on screen
        // belongs to it, and silencing that one would hide a loading screen this service did not start.
        quietOverlay = after != null && after != before ? after : null;
        reloading.whenComplete((ignored, failure) -> {
            quietOverlay = null;
            reloadPending = false;
            if (failure != null) {
                failedSignature = desired;
                LOGGER.error("Resource reload for historical textures failed", failure);
            } else if (!desired.equals(appliedSignature)) {
                // The reload ran but the world moved on underneath it; the next tick compares again.
                failedSignature = null;
            }
        });
    }

    /** One non-blocking toast per session when an enforced stack is missing a pack, with a pointer to the config. */
    private static void maybeReportMissing(Minecraft client) {
        if (missingNotified || !desiredPlan().isActive() || client.level == null) {
            return;
        }
        List<String> missing = new ArrayList<>();
        Map<TextureSource, Path> installed = scan().usable();
        for (TextureSource source : desiredPlan().sources()) {
            if (source.isDependency() && source != TextureSource.PROGRAMMER_ART && !installed.containsKey(source)) {
                missing.add(Component.translatable(source.translationKey()).getString());
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        missingNotified = true;
        LOGGER.info("Historical textures: showing a one-time toast, missing packs: {}", missing);
        net.minecraft.client.gui.components.toasts.SystemToast.add(client.getToasts(),
            net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
            Component.translatable("historical_experience.textures.toast.title"),
            Component.translatable("historical_experience.textures.toast.body", String.join(", ", missing)));
    }
}

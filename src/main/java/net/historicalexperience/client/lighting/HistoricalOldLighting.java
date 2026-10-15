/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.client.lighting;

import java.util.concurrent.ConcurrentLinkedDeque;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Which old lighting engine the client is running, and the "round robin" relighting that the pre-Beta-1.8 one needed.
 *
 * <p>Ported from Nostalgic Tweaks {@code helper/candy/light/LightingHelper} and {@code NostalgicDataLayer}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md). Upstream decides with config flags; here the world's VERSION decides, and
 * a player toggle can only switch the engine OFF inside the window it belongs to.
 *
 * <h2>The one engine, and the two things that are NOT it</h2>
 *
 * <ul>
 *   <li>{@link #activeNow()} is true for every declared build before the Beta 1.8 Pre-release: the sky light itself
 *       changes with the time of day, so the game re-lit chunks a few at a time at dusk and dawn, and the light was
 *       grey. minecraft.wiki, Light, History, Beta 1.8 Pre-release: <i>"Implemented a new lighting engine. The
 *       lighting on a block is given a tint based on the most prominent source of light. Day/night cycles no longer
 *       require chunk updates and have a smooth transition. Artificial light now gives a subtle 'flicker'."</i> That
 *       one entry ends the grey lightmap, the chunk updates and the flicker-free light together, which is why one
 *       boundary carries all of them.</li>
 *   <li><b>Not a Classic engine.</b> One used to live here. Classic is Minecraft before Infdev, and the oldest
 *       version declared is Infdev 20100420, so it could never apply; the owner had it removed.</li>
 *   <li><b>Not the smooth-shading toggle.</b> Whether a version had corner shading at all is
 *       {@link HistoricalSmoothLighting}'s question, and it is answered for Beta 1.3 to Beta 1.7.3.</li>
 * </ul>
 *
 * <h2>Why the answer is cached rather than computed per light read</h2>
 *
 * <p>The light hooks run for every light sample taken while meshing a chunk - hundreds of thousands per rebuild. The
 * version lookup behind {@link HistoricalClientRules#activeVersion()} goes through the integrated server, so it is
 * resolved once per client tick and published through a volatile field instead.
 */
public final class HistoricalOldLighting {
    /** The first build with the new lighting engine; the old one ends before it. */
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";

    private static final String TOGGLE_KEY = "oldLighting";

    /** Sections handed back to the renderer per tick; the "round robin" itself. */
    private static final int RELIGHT_PER_TICK = 128;

    /** Upstream's {@code MAX_BLOCK_LIGHT} default. */
    private static final int MAX_BLOCK_LIGHT = 15;

    private static volatile boolean active;
    private static volatile Boolean toggle;
    private static volatile int timeSkyLight = -1;
    private static volatile int weatherSkyLight = -1;
    private static volatile boolean relightAll;

    private static final ConcurrentLinkedDeque<Long> RELIGHT_QUEUE = new ConcurrentLinkedDeque<>();

    private HistoricalOldLighting() {
    }

    /** Whether the round-robin engine belongs to this version: every declared build older than Beta 1.8.1. */
    public static boolean availableFor(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        return HistoricalVersions.byId(FIRST_NEW_LIGHTING_BUILD)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
    }

    /** Whether the old round-robin engine should be used for the client's current version. */
    public static boolean activeNow() {
        return active;
    }

    public static boolean toggle() {
        Boolean current = toggle;
        if (current == null) {
            current = HistoricalClientOptions.get(TOGGLE_KEY, true);
            toggle = current;
        }
        return current;
    }

    /**
     * Sets the toggle and persists it, always.
     *
     * <p>Not refused when the current world does not have the engine, for the measured reason recorded in
     * {@link HistoricalSmoothLighting#setToggle}: a player reaches the config screen from Mod Menu with no world
     * open, the write was discarded there, and the option appeared to do nothing at all. The window still decides
     * whether the engine is used, through {@link #activeNow()}.
     */
    public static void setToggle(boolean enabled) {
        toggle = enabled;
        HistoricalClientOptions.set(TOGGLE_KEY, enabled);
        refresh();
    }

    /**
     * Marks everything in view for a mesh rebuild.
     *
     * <p>Public because the smooth-shading rule is baked into a chunk's mesh too, so its toggle has to ask for the
     * same rebuild this engine already performs when its own state changes. The flag is drained by {@link #onTick}
     * whenever a world is open, so a request made with no world open simply waits for one.
     */
    public static void requestRelight() {
        relightAll = true;
    }

    /** Recomputes the cached answer. Called every tick, and immediately when a toggle changes. */
    public static void refresh() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        boolean next = availableFor(version) && toggle();
        if (next != active) {
            active = next;
            // Everything already meshed was lit by the previous answer.
            relightAll = true;
        }
    }

    /** Forget everything, for when a world is left: the next world's sky light is unrelated. */
    public static void reset() {
        active = false;
        timeSkyLight = -1;
        weatherSkyLight = -1;
        relightAll = false;
        RELIGHT_QUEUE.clear();
    }

    /** Once per client tick: refresh the answer, watch the sky light, and feed the relight queue. */
    public static void onTick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        refresh();
        if (!active) {
            if (relightAll) {
                enqueueAllSections(minecraft, level);
            }
            drain(minecraft);
            return;
        }

        int fromTime = skyLightFromTime(level);
        int fromWeather = HistoricalOldLightMath.skyLightFromWeather(level.getRainLevel(1.0F), level.getThunderLevel(1.0F));

        if (timeSkyLight == -1 || timeSkyLight != fromTime) {
            timeSkyLight = fromTime;
            relightAll = true;
        }
        if (weatherSkyLight == -1 || weatherSkyLight != fromWeather) {
            weatherSkyLight = fromWeather;
            // Upstream: weather only matters while it is not already as dark as it gets.
            if (timeSkyLight > 4) {
                relightAll = true;
            }
        }
        if (relightAll) {
            enqueueAllSections(minecraft, level);
        }
        drain(minecraft);
    }

    private static int skyLightFromTime(ClientLevel level) {
        if (level.dimensionType().hasCeiling() || !level.dimensionType().hasSkyLight()) {
            return 0;
        }
        float day = level.dimensionType().timeOfDay(level.dayTime());
        return HistoricalOldLightMath.skyLightFromTime(day * ((float) Math.PI * 2.0F));
    }

    /**
     * Queues every non-empty section in view. All-air sections are skipped: they have no mesh, so there is nothing
     * to rebuild, and they are most of a column.
     */
    private static void enqueueAllSections(Minecraft minecraft, ClientLevel level) {
        relightAll = false;
        if (minecraft.player == null) {
            return;
        }
        RELIGHT_QUEUE.clear();
        int range = minecraft.options.getEffectiveRenderDistance();
        int centreX = minecraft.player.chunkPosition().x;
        int centreZ = minecraft.player.chunkPosition().z;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(centreX + dx, centreZ + dz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    if (!sections[i].hasOnlyAir()) {
                        RELIGHT_QUEUE.add(SectionPos.asLong(centreX + dx, chunk.getMinSection() + i, centreZ + dz));
                    }
                }
            }
        }
    }

    /**
     * Hands a few queued sections back to the renderer. {@code setSectionDirty} is the one entry point both the
     * vanilla renderer and Sodium honour - Sodium's own mixin redirects it to {@code scheduleRebuildForChunk} - so
     * this needs no renderer-specific code.
     */
    private static void drain(Minecraft minecraft) {
        for (int i = 0; i < RELIGHT_PER_TICK; i++) {
            Long packed = RELIGHT_QUEUE.pollFirst();
            if (packed == null) {
                return;
            }
            minecraft.levelRenderer.setSectionDirty(SectionPos.x(packed), SectionPos.y(packed), SectionPos.z(packed));
        }
    }

    /**
     * The value the active old engine would have stored at {@code pos}.
     *
     * <p>Called from the light hooks, on meshing threads as well as the render thread, so it reads only volatile
     * state and the immutable level.
     */
    public static int transform(ClientLevel level, LightLayer layer, BlockPos pos, int value) {
        if (!active) {
            return value;
        }
        if (layer == LightLayer.BLOCK) {
            // Block light is untouched by this engine: a torch lit what it lit, at the level it still does.
            return value;
        }
        return combined(level, value, level.getBrightness(LightLayer.BLOCK, pos));
    }

    /** Sky and block light combined as the old engine stored them right now. */
    public static int combined(ClientLevel level, int skyLight, int blockLight) {
        return HistoricalOldLightMath.combinedLight(skyLight, blockLight, level.getMaxLightLevel(), timeSkyLight,
            weatherSkyLight < 0 ? 0 : weatherSkyLight, level.dimensionType().hasFixedTime(), MAX_BLOCK_LIGHT);
    }
}

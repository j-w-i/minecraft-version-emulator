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
 */package net.historicalexperience.gameplay;

import net.historicalexperience.version.Features;

import net.historicalexperience.version.Feature;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

/**
 * Which Ender Dragon fight a historical world gets: the original one, or the one from Release 1.9.
 *
 * <p>Release 1.9 (15w31a) revamped the fight: the dragon perches on the exit portal and breathes, shoots fireballs,
 * can be re-summoned with four End crystals, drops an End gateway on every death, and the portal is pre-generated.
 * None of that existed before it, and every profile this project declares ends at Release 1.6.4, so every historical
 * world gets the original fight.
 *
 * <p>The original dragon only flew: it circled the obsidian pillars healed by their crystals, and went for the player.
 * It never landed, never breathed, never shot, could not be respawned, and left no gateway. What a world without the
 * 1.9 revamp needs is therefore mostly REMOVAL, and that is what the mixins using this class do. What remains of the
 * dragon's movement is Mojang's own: the aim height and vertical drive below are read from the original 1.0 client.
 *
 * <p>Where the dragon first appears is a separate, earlier boundary (Beta 1.9 Prerelease 6) and is not decided here.
 */
public final class HistoricalDragonFight {
    /** What the original client wrote over the dragon's health bar. */
    public static final String BOSS_HEALTH_CAPTION = "Boss health";

    /**
     * How far above a player's feet the original dragon aimed, given its horizontal distance from them.
     *
     * <p>Read from Mojang's Release 1.0 client, {@code EntityDragon.onLivingUpdate}: {@code 0.4 + distance / 80 - 1},
     * capped at 10. So it aims at the player's feet from 48 blocks, BELOW them when closer, and only climbs above them
     * when far away - which is what makes it sweep the ground through the player instead of passing overhead. A flat
     * "go to the player" target, which is what the Release 1.9 charge phase gives, ignores the distance term.
     */
    public static double aimHeightAbove(double horizontalDistance) {
        return Math.min(10.0D, 0.4000000059604645D + horizontalDistance / 80.0D - 1.0D);
    }

    /**
     * True when this world's version predates the dragon: Beta 1.9 Prerelease 4 and 5 have the End but no dragon.
     *
     * <p>The class comment used to say this boundary was "not decided here"; the 2026-10-21 audit found that in fact
     * nothing decided it anywhere, so Pre4 and Pre5 spawned a live dragon. This is the decision.
     */
    public static boolean noDragon(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        String profile = HistoricalProfileLookup.profileIdForLevel(level.getServer().overworld());
        return Features.active(Feature.NO_ENDER_DRAGON, profile);
    }

    /** The original dragon's vertical steering gain: it added {@code verticalError * 0.1} to its vertical speed. */
    public static final double ORIGINAL_VERTICAL_GAIN = 0.10000000149011612D;

    /** The original clamp on that vertical error ratio. */
    public static final float ORIGINAL_VERTICAL_RATIO_LIMIT = 0.6F;

    /** The 1.21.1 dragon's gain, which is a tenth of the original's. */
    public static final double MODERN_VERTICAL_GAIN = 0.01D;

    private HistoricalDragonFight() {
    }

    /**
     * The caption the boss bar carried in this version, or null when it carries the dragon's own name (an ordinary
     * world, and every version from Release 1.4).
     *
     * <p>Read from the client jars: Beta 1.9 Prerelease 6 added the bar with a hard-coded "Boss health" string in the
     * HUD class, and the Release 1.0, 1.2.5 and 1.3.2 jars all still contain it, while the 1.4.2, 1.4.7 and 1.5 jars
     * contain no such string and show the entity name "Ender Dragon". The wiki records the same change at 12w34b.
     */
    public static String bossBarCaption(String versionId) {
        if (versionId == null) {
            return null;
        }
        return Features.active(Feature.BOSS_HEALTH_CAPTION, versionId) ? BOSS_HEALTH_CAPTION : null;
    }

    /** Whether a profile id gets the original fight. An ordinary world (null or unknown) keeps the modern one. */
    public static boolean originalFight(String versionId) {
        if (versionId == null) {
            return false;
        }
        return Features.active(Feature.ORIGINAL_DRAGON_FIGHT, versionId);
    }

    /**
     * Whether the fight in this level is the original one.
     *
     * <p>The End's generator is vanilla and carries no historical marker, so the answer comes from the OVERWORLD of the
     * same server, which is where the world's version is recorded.
     */
    public static boolean originalFight(ServerLevel level) {
        if (level == null || level.getServer() == null) {
            return false;
        }
        return originalFight(HistoricalProfileLookup.profileIdForLevel(level.getServer().overworld()));
    }

    /** The phases that only exist since the revamp: landing on the portal, sitting there, and taking off again. */
    public static boolean isPerchPhase(EnderDragonPhase<?> phase) {
        return phase == EnderDragonPhase.LANDING_APPROACH
            || phase == EnderDragonPhase.LANDING
            || phase == EnderDragonPhase.TAKEOFF
            || phase == EnderDragonPhase.SITTING_FLAMING
            || phase == EnderDragonPhase.SITTING_SCANNING
            || phase == EnderDragonPhase.SITTING_ATTACKING;
    }
}
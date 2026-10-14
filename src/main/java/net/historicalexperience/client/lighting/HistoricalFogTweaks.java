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

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Direct port of NT helper/candy/level/fog/OverworldFogRenderer's
 * ALPHA_R164 getFarPlaneDistance/setTerrain/setHorizon calculations.
 * LGPL-3.0; see THIRD_PARTY_NOTICES.md. Classic mode is intentionally absent.
 * This is the distance/shape slice; colour, dark fog and void fog are separate pending ports.
 */
public final class HistoricalFogTweaks {
    private HistoricalFogTweaks() {
    }

    public static boolean worldFog() {
        return HistoricalClientRules.activeVersion() != null && HistoricalClientOptions.get("oldWorldFog", true);
    }

    /** Sunrise/sunset tint on FOG arrived in Beta 1.8; this does not remove the sky's sunrise disc. */
    public static boolean oldSunriseFog() {
        return beforeBeta18(HistoricalClientRules.activeVersion())
            && HistoricalClientOptions.get("oldSunriseSunsetFog", true);
    }

    public static boolean beforeBeta18(HistoricalVersion version) {
        return version != null && HistoricalVersions.byId("historical_experience:beta_1_8_1")
            .map(boundary -> version.chronology() < boundary.chronology()).orElse(false);
    }

    public static int farPlane(int renderDistance) {
        return (renderDistance * 16) * (renderDistance > 28 ? 1 : 2);
    }

    public static float terrainEnd(int renderDistance) {
        float density = switch (renderDistance) {
            case 9, 10, 11 -> 0.7F;
            case 6, 7, 8 -> 0.65F;
            case 4, 5 -> 0.55F;
            case 2, 3 -> 0.48F;
            default -> 0.8F;
        };
        return farPlane(renderDistance) * density;
    }

    public static float skyEnd(int renderDistance) {
        float density = switch (renderDistance) {
            case 6, 7, 8, 9 -> 0.65F;
            case 4, 5 -> 0.5F;
            case 2, 3 -> 0.2F;
            default -> 0.8F;
        };
        return farPlane(renderDistance) * density;
    }
}

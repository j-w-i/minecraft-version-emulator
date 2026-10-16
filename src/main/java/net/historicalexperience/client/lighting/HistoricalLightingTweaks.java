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
import net.historicalexperience.client.lighting.HistoricalOldLighting;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/** Version gates for NT's separate leaves and water lighting effects. */
public final class HistoricalLightingTweaks {
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";
    private static final String LEAVES_KEY = "oldLeavesLighting";
    private static final String WATER_KEY = "oldWaterLighting";

    /** NT world_lighting/BlockStateBaseMixin's water absorption, not an implementation by itself. */
    public static final int OLD_WATER_LIGHT_BLOCK = 3;

    private HistoricalLightingTweaks() {
    }

    /**
     * NT's leaves ambient-occlusion brightness, not emissive or full-bright rendering.
     *
     * <p>These two detail switches are ALSO governed by the old-lighting switch the player can see. They default to on,
     * they are not on the config screen, and they used to consult only the version window - so in an Infdev world with
     * both visible options turned off, leaves were still lit by the old model and the owner reported exactly that:
     * "smooth shading being applied is the old smooth shading model even when it's off". Turning the mod's lighting
     * off has to mean the mod's lighting is off, including the parts with no row of their own.
     */
    public static boolean oldLeavesLighting() {
        return beforeNewEngine() && HistoricalOldLighting.toggle()
            && HistoricalClientOptions.get(LEAVES_KEY, true);
    }

    public static boolean setOldLeavesLighting(boolean enabled) {
        if (enabled && !availableFor(HistoricalClientRules.activeVersion())) {
            return false;
        }
        HistoricalClientOptions.set(LEAVES_KEY, enabled);
        return true;
    }

    /**
     * Gate only: the complete water-lighting renderer/relight port is still pending.
     *
     * <p>Governed by the visible old-lighting switch for the same reason as the leaves one above.
     */
    public static boolean oldWaterLighting() {
        return beforeNewEngine() && HistoricalOldLighting.toggle()
            && HistoricalClientOptions.get(WATER_KEY, true);
    }

    public static boolean setOldWaterLighting(boolean enabled) {
        if (enabled && !availableFor(HistoricalClientRules.activeVersion())) {
            return false;
        }
        HistoricalClientOptions.set(WATER_KEY, enabled);
        return true;
    }

    public static boolean availableFor(HistoricalVersion version) {
        return version != null && HistoricalVersions.byId(FIRST_NEW_LIGHTING_BUILD)
            .map(first -> version.chronology() < first.chronology())
            .orElse(false);
    }

    private static boolean beforeNewEngine() {
        return availableFor(HistoricalClientRules.activeVersion());
    }
}

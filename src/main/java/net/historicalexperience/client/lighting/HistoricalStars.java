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
import net.historicalexperience.mixin.LevelRendererHistoricalStarsAccess;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.client.Minecraft;

/**
 * NT OLD_STARS era selection and buffer lifecycle. Geometry and colour formulas
 * come from world_sky/LevelRendererMixin (LGPL-3.0; THIRD_PARTY_NOTICES.md).
 * Beta1.5 dimmed stars; Release1.3.1 made them smaller/brighter (Sky history).
 * The wiki establishes those transitions; NT supplies the ALPHA/BETA formulas.
 */
public final class HistoricalStars {
    public enum Style { ALPHA, BETA, MODERN }

    // LevelRenderer constructs its first buffer at title time. Never consult a
    // not-yet-joined world while baking: start modern and explicitly rebuild on change.
    private static volatile Style style = Style.MODERN;
    private static int rebuilds;

    private HistoricalStars() {
    }

    public static Style styleFor(HistoricalVersion version) {
        if (version == null) {
            return Style.MODERN;
        }
        var dimmer = HistoricalVersions.byId("historical_experience:beta_1_5").orElse(null);
        var smaller = HistoricalVersions.byId("historical_experience:release_1_3").orElse(null);
        if (dimmer == null || smaller == null) {
            return Style.MODERN;
        }
        if (version.chronology() < dimmer.chronology()) {
            return Style.ALPHA;
        }
        return version.chronology() < smaller.chronology() ? Style.BETA : Style.MODERN;
    }

    public static Style style() {
        return style;
    }

    public static boolean oldStars() {
        return style != Style.MODERN;
    }

    public static int rebuildCount() {
        return rebuilds;
    }

    /** Main-thread only; joins, disconnects and profile switches all use this path. */
    public static void onTick(Minecraft client) {
        Style next = client.level == null || !HistoricalClientOptions.get("oldStars", true)
            ? Style.MODERN : styleFor(HistoricalClientRules.activeVersion());
        if (next != style) {
            style = next;
            ((LevelRendererHistoricalStarsAccess) client.levelRenderer).historicalExperience$createStars();
            rebuilds++;
        }
    }

    /** Upstream's colour calculation; alpha is twice beta at the same transparency. */
    public static float colour(Style selected, float transparency) {
        return selected == Style.BETA ? transparency : transparency / 0.5F;
    }

    public static float width(Style selected, float original) {
        return selected == Style.MODERN ? original : 0.25F;
    }
}

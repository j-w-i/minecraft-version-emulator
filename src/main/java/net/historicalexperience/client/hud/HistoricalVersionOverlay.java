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
 */package net.historicalexperience.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.client.lighting.HistoricalClientOptions;
import net.historicalexperience.version.HistoricalVersion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Draws the version name in the top-left corner, the way the game did before Beta 1.6.5.
 *
 * <p>ADAPTED from Nostalgic Tweaks' {@code OLD_VERSION_OVERLAY} tweak: the branch in
 * {@code common/src/main/java/mod/adrenix/nostalgic/listener/client/GuiListener.java} ({@code renderTextOverlay}) and the
 * top-left path of {@code common/src/main/java/mod/adrenix/nostalgic/util/client/gui/CornerManager.java}
 * ({@code drawText}), which starts a corner at 2 and clamps the text onto the screen. Two changes. Upstream decides with
 * a global flag that defaults on and draws "Minecraft %v" with the modern version; here the version decides, through
 * {@link HistoricalVersionLabel}, and the text is that version's own name. Upstream's corner choice, offsets and the
 * stacking of several overlays are not ported - this project has one overlay, always top-left.
 *
 * <p>Not drawn while F3 is open (the original replaced the plain label with the extended one, and modern F3 already
 * starts with its own version line in the same corner) or while the HUD is hidden.
 */
public final class HistoricalVersionOverlay {
    private static final String TOGGLE_KEY = "oldVersionOverlay";

    /** NT's CornerManager starts the top-left column at 2, and the drawn string is white with a shadow. */
    private static final int ORIGIN = 2;

    private HistoricalVersionOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(HistoricalVersionOverlay::render);
    }

    /** Whether the player wants the label. On by default, like upstream's flag. */
    public static boolean toggle() {
        return HistoricalClientOptions.get(TOGGLE_KEY, true);
    }

    public static void setToggle(boolean enabled) {
        HistoricalClientOptions.set(TOGGLE_KEY, enabled);
    }

    /** Whether the toggle can do anything for this version: only where the original game drew the label. */
    public static boolean availableFor(HistoricalVersion version) {
        return HistoricalVersionLabel.shownIn(version);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen() || !toggle()) {
            return;
        }
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        if (!HistoricalVersionLabel.shownIn(version)) {
            return;
        }
        String text = HistoricalVersionLabel.textFor(version);
        int x = Mth.clamp(ORIGIN, 0, graphics.guiWidth() - minecraft.font.width(text));
        int y = Mth.clamp(ORIGIN, 0, graphics.guiHeight() - minecraft.font.lineHeight);
        graphics.drawString(minecraft.font, text, x, y, 0xFFFFFF, true);
    }
}
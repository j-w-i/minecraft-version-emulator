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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Shows the setup screen once, over the first title screen of an installation that has not completed setup.
 *
 * <p>It never interrupts a game in progress and never reappears on its own: the screen records completion when it is
 * finished or skipped, and this trigger also refuses to fire again within a session even if the player dismissed the
 * screen with Escape (which deliberately records nothing, so the next launch asks once more).
 *
 * <p>Players who turned automatic textures off have opted out of the feature, so they are not asked.
 */
public final class TextureFirstRun {
    private static boolean shownThisSession;

    private TextureFirstRun() {
    }

    public static void tick(Minecraft client) {
        if (shownThisSession || client.level != null || client.getOverlay() != null) {
            return;
        }
        if (!(client.screen instanceof TitleScreen title)) {
            return;
        }
        shownThisSession = true;
        if (HistoricalTextureConfig.setupCompleted() || !HistoricalTextureConfig.enabled()) {
            return;
        }
        client.setScreen(new TextureSetupScreen(title));
    }
}

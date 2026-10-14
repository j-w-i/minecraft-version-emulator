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
package net.historicalexperience.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.historicalexperience.client.lighting.HistoricalStars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/** Opt-in star rendering/leave-world regression probe for project test saves; never edits terrain. */
public final class NtStarsProbe {
    private static int ticks;
    private static int afterDisconnect;
    private static boolean captured;
    private static long savedTime;
    private static float savedRain;
    private static float savedThunder;
    private static float savedPitch;
    private static boolean savedHud;
    private static final List<String> RESULTS = new ArrayList<>();

    private NtStarsProbe() {
    }

    public static void register() {
        if (Boolean.getBoolean("historical_experience.ntStarsProbe")) {
            ClientTickEvents.END_CLIENT_TICK.register(NtStarsProbe::tick);
        }
    }

    private static void tick(Minecraft client) {
        try {
            if (captured) {
                if (++afterDisconnect == 20) {
                    note("after-disconnect style=" + HistoricalStars.style() + " rebuilds=" + HistoricalStars.rebuildCount());
                    note(HistoricalStars.style() == HistoricalStars.Style.MODERN ? "PASS modern restored" : "FAIL style leaked");
                    write();
                    client.stop();
                }
                return;
            }
            if (client.level == null || client.player == null || ++ticks < 200) {
                return;
            }
            if (ticks == 200) {
                savedTime = client.level.getDayTime();
                savedRain = client.level.getRainLevel(1.0F);
                savedThunder = client.level.getThunderLevel(1.0F);
                savedPitch = client.player.getXRot();
                savedHud = client.options.hideGui;
                client.setScreen(null);
            }
            // Time/weather changes are client-only. The test player's view is restored before disconnect.
            client.level.setDayTime(18000);
            client.level.setRainLevel(0.0F);
            client.level.setThunderLevel(0.0F);
            client.player.setXRot(-65.0F);
            client.options.hideGui = true;
            if (ticks == 260) {
                note("joined version=" + HistoricalClientRules.activeVersion().id());
                note("style=" + HistoricalStars.style() + " rebuilds=" + HistoricalStars.rebuildCount());
                float transparency = client.level.getStarBrightness(1.0F);
                note("transparency=" + transparency + " colour=" + HistoricalStars.colour(HistoricalStars.style(), transparency));
                note("geometry terms=" + HistoricalStars.width(HistoricalStars.style(), 0.15F)
                    + "," + HistoricalStars.width(HistoricalStars.style(), 0.1F));
                Screenshot.grab(client.gameDirectory, "nt-probe-stars.png", client.getMainRenderTarget(),
                    message -> System.out.println("[hex-stars-probe] " + message.getString()));
                client.level.setDayTime(savedTime);
                client.level.setRainLevel(savedRain);
                client.level.setThunderLevel(savedThunder);
                client.player.setXRot(savedPitch);
                client.options.hideGui = savedHud;
                captured = true;
                client.disconnect(new TitleScreen());
            }
        } catch (Throwable failure) {
            note("FAIL " + failure);
            write();
            client.stop();
        }
    }

    private static void note(String message) {
        RESULTS.add(message);
        System.out.println("[hex-stars-probe] " + message);
    }

    private static void write() {
        try {
            Files.createDirectories(Path.of("logs"));
            Files.write(Path.of("logs/nt-stars-probe.txt"), RESULTS);
        } catch (Exception failure) {
            System.err.println("[hex-stars-probe] could not save evidence: " + failure);
        }
    }
}

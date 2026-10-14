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
package net.historicalexperience.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration: the config button in the mod list opens this project's options screen.
 *
 * <h2>Why this is a separate entrypoint class rather than a factory handed over from the client initialiser</h2>
 *
 * <p>Mod Menu discovers this class by name through the {@code modmenu} entrypoint in {@code fabric.mod.json}, so it
 * is loaded <b>only when Mod Menu is present</b>. That is what keeps Mod Menu optional: the mod lists it under
 * {@code suggests} and not {@code depends}, so a player without Mod Menu is not refused entry to the game, and the
 * main client path never references a class from a mod that may be absent. A factory registered from
 * {@code HistoricalExperienceClient} would be a hard dependency in practice, because the class would be resolved on
 * every launch whether or not Mod Menu existed.
 */
public final class HistoricalModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HistoricalConfigScreen::new;
    }
}

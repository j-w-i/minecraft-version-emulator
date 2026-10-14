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
package net.historicalexperience.client.version;

import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

/**
 * Adds historical version selection to the create-world screen.
 *
 * <h2>Why a button and a world preset rather than a settings form</h2>
 *
 * <p>This mod already ships one world preset per version, named {@code historical_experience:<version>}, and
 * already tags them into {@code minecraft:normal} so vanilla lists them. Choosing a version here therefore selects
 * one of those presets through the screen's own world-type state. Nothing about terrain, height, biomes or
 * structures is assembled here, and nothing needs to be: a GUI that built generator settings would be a second,
 * competing world-generation system, which is exactly what this project forbids.
 *
 * <h2>Why the choice is remembered outside the screen</h2>
 *
 * <p>The create-world screen is destroyed and rebuilt when the player opens game rules, data packs or the world
 * customization screen. A selection that lived only in the screen would be lost on the way back, so the chosen
 * version is held in {@link Selection} and re-applied every time the screen initialises. The screen is the view;
 * the selection is the state.
 *
 * <p>The button is part of vanilla's footer layout (see CreateWorldHistoricalFooterMixin), not an independently
 * positioned overlay. The footer reserves space for both this row and the create/cancel row, including on resize.
 */
public final class CreateWorldHistoricalVersion {
    private static final String MARKER = "Historical Version:";
    private static final int BUTTON_WIDTH = 310;

    /** The version chosen on the create-world screen, surviving the screen being rebuilt. */
    public static final class Selection {
        private static String key = "";

        private Selection() {
        }

        /** The chosen version's key, or blank when none was chosen. */
        public static String key() {
            return key;
        }

        static void set(String newKey) {
            key = newKey == null ? "" : newKey;
        }

        /** Forgets the choice, so the next world creation does not inherit it. */
        public static void clear() {
            key = "";
        }
    }

    private CreateWorldHistoricalVersion() {
    }

    /** Registers the screen hook. Called from the client initialiser. */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof CreateWorldScreen createWorldScreen)) {
                return;
            }

            applyStoredSelection(createWorldScreen);
        });
    }

    /** Constructed once per native footer, and positioned exclusively by that layout. */
    public static Button createButton(CreateWorldScreen screen) {
        return Button.builder(label(), pressed -> Minecraft.getInstance().setScreen(
                new HistoricalVersionScreen(screen, context(screen, pressed))))
            .width(BUTTON_WIDTH)
            .build();
    }

    private static HistoricalVersionScreen.Context context(CreateWorldScreen createWorldScreen, Button button) {
        return new HistoricalVersionScreen.Context(
            Component.literal("Historical Version"),
            Component.literal("Use this version"),
            Selection.key(),
            // Any implemented version may be chosen for a new world: each one has a world preset, so each can
            // actually be created. A planned version is not selectable and the screen says why.
            Entry::selectable,
            key -> {
                Selection.set(key);
                applyStoredSelection(createWorldScreen);
                createWorldScreen.getUiState().onChanged();

                // Re-opening this screen would re-initialise it and add another button; the label is the only
                // thing that has to change here.
                button.setMessage(label());
            },
            Component.empty());
    }

    /**
     * Applies the remembered version to the screen's world type.
     *
     * <p>Uses vanilla's own preset list rather than the registry directly: the list is what the player would
     * otherwise cycle through, so selecting from it cannot produce a world type the screen itself would not offer.
     * If a remembered version is no longer in the list, nothing is applied and the screen keeps whatever vanilla
     * chose - the label then reads "Historical Version: none", so the state is visible rather than implied.
     */
    private static void applyStoredSelection(CreateWorldScreen createWorldScreen) {
        String key = Selection.key();
        if (key.isBlank()) {
            return;
        }

        WorldCreationUiState uiState = createWorldScreen.getUiState();
        List<WorldCreationUiState.WorldTypeEntry> presets = uiState.getNormalPresetList();

        for (WorldCreationUiState.WorldTypeEntry preset : presets) {
            if (matches(preset, key)) {
                uiState.setWorldType(preset);
                return;
            }
        }
    }

    private static boolean matches(WorldCreationUiState.WorldTypeEntry entry, String versionKey) {
        Optional<ResourceKey<WorldPreset>> presetKey = entry.preset().unwrapKey();
        return presetKey
            .map(key -> key.location().toString().equals(versionKey))
            .orElse(false);
    }

    /** The button label: which version will be used, which is the whole point of the control. */
    private static Component label() {
        String key = Selection.key();

        if (key.isBlank()) {
            return Component.literal(MARKER + " none");
        }

        String name = HistoricalVersionCatalogue.byKey(key)
            .map(Entry::displayName)
            .orElse(key);

        return Component.literal(MARKER + " " + name);
    }
}

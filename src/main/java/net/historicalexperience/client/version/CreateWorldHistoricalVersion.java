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

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.historicalexperience.client.indev.IndevMapApplication;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;

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

    /**
     * The version button and the toggle beside it are laid out to EXACTLY the width of vanilla's own create/cancel
     * row in this footer, which is two {@link Button#DEFAULT_WIDTH} buttons with {@link Button#DEFAULT_SPACING}
     * between them.
     *
     * <p>This is the whole reason the numbers are derived rather than chosen. The footer positions its rows on a
     * shared left edge, so a row of a different width cannot line up with the row below it: a first attempt used
     * 310px against vanilla's 308 and the owner could see the extra 2px sticking out past Cancel. Matching the
     * width is what makes both edges agree, and it is asserted against the real buttons.
     */
    public static final int ROW_WIDTH = Button.DEFAULT_WIDTH * 2 + Button.DEFAULT_SPACING;
    static final int ROW_HEIGHT = Button.DEFAULT_HEIGHT;

    /**
     * The toggle: a plain vanilla button wide enough for "OFF", beside the version button rather than cut into it.
     * The gap between the two is vanilla's own button spacing, so this row's internal gap is the same 8px as the
     * gap between Create New World and Cancel.
     */
    static final int TOGGLE_WIDTH = 40;
    /** The gap between the version button and its toggle; public so the screen probe can assert the layout. */
    public static final int ROW_GAP = Button.DEFAULT_SPACING;

    private static final int BUTTON_WIDTH = ROW_WIDTH - TOGGLE_WIDTH - ROW_GAP;

    /** The version chosen on the create-world screen, surviving the screen being rebuilt. */
    public static final class Selection {
        private static String key = "";
        private static boolean enabled = true;

        private Selection() {
        }

        /** The chosen version's key, or blank when none was chosen. */
        public static String key() {
            return key;
        }

        /**
         * Whether the chosen version is used at all.
         *
         * <p>Off means an ordinary world, and it does NOT forget the version: the whole point of a switch beside
         * the picker is to stop using a choice without having to make it again. The remembered key is what the
         * toggle turns back on.
         */
        public static boolean enabled() {
            return enabled;
        }

        static void set(String newKey) {
            key = newKey == null ? "" : newKey;
        }

        static void setEnabled(boolean newValue) {
            enabled = newValue;
        }

        /** Forgets the choice, so the next world creation does not inherit it. */
        public static void clear() {
            key = "";
            enabled = true;
        }
    }

    /** Every world-preset id this screen can apply, so a preset of ours is recognisable in either direction. */
    private static final java.util.Set<String> HISTORICAL_PRESET_IDS = buildHistoricalPresetIds();

    private static java.util.Set<String> buildHistoricalPresetIds() {
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (Entry entry : HistoricalVersionCatalogue.entries()) {
            if (entry.selectable()) {
                net.historicalexperience.version.HistoricalSnapshotPatches
                    .baseProfileForSelection(entry.key()).ifPresent(ids::add);
            }
        }
        return java.util.Set.copyOf(ids);
    }

    private CreateWorldHistoricalVersion() {
    }

    /** Each screen's own two buttons, so a rebuilt screen replaces its pair instead of accumulating another. */
    private static final java.util.Map<Screen, Runnable> REFRESHERS = new java.util.WeakHashMap<>();

    /** Registers the screen hook. Called from the client initialiser. */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof CreateWorldScreen createWorldScreen)) {
                return;
            }

            applyStoredSelection(createWorldScreen);
            IndevMapApplication.reapplyIfChosen(createWorldScreen);
            ScreenEvents.beforeRender(screen).register((current, graphics, mouseX, mouseY, delta) -> {
                synchronizeNativeChoice(createWorldScreen);
                Runnable refresh = REFRESHERS.get(screen);
                if (refresh != null) refresh.run();
            });
        });
    }

    /**
     * The version button and the toggle beside it, laid out and positioned by vanilla's own footer layout.
     *
     * <p>A horizontal linear layout rather than two children of the footer row, because the footer centres each child
     * on its own and that would put the toggle in the middle of the window instead of on the end of the button. The
     * two buttons are ordinary vanilla buttons: the toggle is the plain ON/OFF switch the owner asked for, the same
     * height and style as the button next to it, separated by a visible gap rather than cut into it.
     */
    public static net.minecraft.client.gui.layouts.LayoutElement createRow(CreateWorldScreen screen) {
        Button version = Button.builder(label(), pressed -> Minecraft.getInstance().setScreen(
                new HistoricalVersionScreen(screen, context(screen))))
            .width(BUTTON_WIDTH)
            .build();
        Button toggle = Button.builder(toggleLabel(), pressed -> {
            Selection.setEnabled(!Selection.enabled());
            applyStoredSelection(screen);
            refresh(version, pressed);
            screen.getUiState().onChanged();
        }).width(TOGGLE_WIDTH).build();
        REFRESHERS.put(screen, () -> refresh(version, toggle));
        var row = net.minecraft.client.gui.layouts.LinearLayout.horizontal().spacing(ROW_GAP);
        row.addChild(version);
        row.addChild(toggle);
        return row;
    }

    /** The toggle's own two words, and nothing else: "ON" or "OFF". */
    private static Component toggleLabel() {
        return Component.literal(Selection.enabled() ? "ON" : "OFF");
    }

    /** The label of the version button, and whether it may be pressed at all; the toggle's own label follows it. */
    static void refresh(Button version, Button toggle) {
        version.setMessage(label());
        // Off greys the button rather than leaving it live: a live button whose version is not in use would let a
        // player pick a version and see nothing change, which is the confusion the toggle exists to remove.
        version.active = Selection.enabled();
        toggle.setMessage(toggleLabel());
    }

    private static HistoricalVersionScreen.Context context(CreateWorldScreen createWorldScreen) {
        return new HistoricalVersionScreen.Context(
            Component.literal("Historical Version"),
            Component.literal("Use this version"),
            Selection.key(),
            // Any implemented version may be chosen for a new world: each one has a world preset, so each can
            // actually be created. A planned version is not selectable and the screen says why.
            Entry::selectable,
            key -> {
                Selection.set(key);
                // Picking a version is also the moment the feature is switched on: the player has just said which
                // version they want, and leaving the switch off would silently ignore them.
                Selection.setEnabled(true);
                applyStoredSelection(createWorldScreen);
                // Setting a world type rebuilds the context from the preset, so a chosen Indev map setting has to be
                // put back afterwards rather than once when it was picked.
                IndevMapApplication.reapplyIfChosen(createWorldScreen);
                createWorldScreen.getUiState().onChanged();

                // Re-opening this screen would re-initialise it and add another button, so the pair is refreshed in
                // place: the version button's new label and the toggle's own state both come from the selection.
                Runnable refresh = REFRESHERS.get(createWorldScreen);
                if (refresh != null) refresh.run();
            },
            Component.empty(),
            // The original Indev map settings are offered only from the version picker, and only while an Indev
            // version is highlighted: they belong to that era, not to the footer.
            () -> Minecraft.getInstance().setScreen(new IndevMapSettingsScreen(
                Minecraft.getInstance().screen, createWorldScreen)));
    }

    /**
     * Rebuilds the creation context from the chosen world type's own preset, dropping any per-world override.
     *
     * <p>Vanilla's {@code setWorldType} unconditionally re-applies the preset's own dimensions, so this is a real
     * reset, not a no-op when the world type is already the one selected. Used when the player picks the project's
     * default Indev option names: the default configuration IS the preset, so nothing has to be written into the
     * world - and an inline override is exactly what makes a world "customized" to the finite-to-infinite migration.
     */
    public static void resetToPresetSettings(CreateWorldScreen screen) {
        applyStoredSelection(screen);
    }

    /**
     * Applies the remembered version to the screen's world type, or takes it back off again.
     *
     * <p>Uses vanilla's own preset list rather than the registry directly: the list is what the player would
     * otherwise cycle through, so selecting from it cannot produce a world type the screen itself would not offer.
     * If a remembered version is no longer in the list, nothing is applied and the screen keeps whatever vanilla
     * chose - the label then reads "Historical Version: none", so the state is visible rather than implied.
     */
    private static void applyStoredSelection(CreateWorldScreen createWorldScreen) {
        WorldCreationUiState uiState = createWorldScreen.getUiState();
        List<WorldCreationUiState.WorldTypeEntry> presets = uiState.getNormalPresetList();
        String key = Selection.enabled() ? Selection.key() : "";

        if (!key.isBlank()) {
            for (WorldCreationUiState.WorldTypeEntry preset : presets) {
                if (matches(preset, key)) {
                    uiState.setWorldType(preset);
                    return;
                }
            }
            return;
        }

        // Switched off, or nothing chosen: an ordinary world. Only a preset of OURS is taken back off, so a player
        // who chose Large Biomes or Amplified themselves keeps it; turning the switch off is not a reason to
        // overwrite a world type this mod never set.
        if (!isHistoricalPreset(uiState.getWorldType())) {
            return;
        }
        for (WorldCreationUiState.WorldTypeEntry preset : presets) {
            if (isVanillaNormal(preset)) {
                uiState.setWorldType(preset);
                return;
            }
        }
    }

    private static boolean isVanillaNormal(WorldCreationUiState.WorldTypeEntry entry) {
        return entry != null && entry.preset() != null && entry.preset().unwrapKey()
            .map(key -> key.location().toString().equals("minecraft:normal"))
            .orElse(false);
    }

    /** Whether this world type is one of the presets this mod hands out. */
    private static boolean isHistoricalPreset(WorldCreationUiState.WorldTypeEntry entry) {
        if (entry == null || entry.preset() == null) {
            return false;
        }
        return entry.preset().unwrapKey()
            .map(key -> HISTORICAL_PRESET_IDS.contains(key.location().toString()))
            .orElse(false);
    }

    private static boolean matches(WorldCreationUiState.WorldTypeEntry entry, String versionKey) {
        if (entry == null || entry.preset() == null) return false;
        String base = net.historicalexperience.version.HistoricalSnapshotPatches.baseProfileForSelection(versionKey).orElse("");
        return entry.preset().unwrapKey().map(key -> key.location().toString().equals(base)).orElse(false);
    }

    private static void synchronizeNativeChoice(CreateWorldScreen screen) {
        var nativeType = screen.getUiState().getWorldType();
        if (matches(nativeType, Selection.enabled() ? Selection.key() : "")) return;
        String nativeKey = nativeType == null || nativeType.preset() == null ? "" : nativeType.preset().unwrapKey()
            .map(key -> key.location().toString()).orElse("");
        String mapped = HistoricalVersionCatalogue.byKey(nativeKey).filter(Entry::selectable).map(Entry::key).orElse("");
        if (!mapped.isEmpty()) {
            // A historical preset of ours reached the screen through vanilla's own world-type control, so it is in
            // use: adopting it also switches the feature back on rather than leaving the switch contradicting the
            // world type on screen.
            Selection.set(mapped);
            Selection.setEnabled(true);
            return;
        }
        if (isHistoricalPreset(nativeType)) return; // one of our snapshot baselines, which has no catalogue key
        // The screen now shows a world type the player picked themselves. While the switch is on, the remembered
        // version is no longer the screen's choice, so the label must not claim it is. While the switch is OFF the
        // screen is deliberately ordinary and the remembered version has to survive - that is what turning it back
        // on restores, so it is not cleared here.
        if (Selection.enabled()) Selection.set("");
    }

    /** Called only after vanilla successfully allocated this new world's own storage directory. */
    public static void bindSnapshotToNewWorld(CreateWorldScreen screen, java.nio.file.Path newWorldDirectory) {
        net.historicalexperience.world.HistoricalSnapshotCreation.clear();
        synchronizeNativeChoice(screen);
        if (!Selection.enabled()) {
            // The snapshot patch is not in use, so nothing may be staged for this world's directory.
            return;
        }
        net.historicalexperience.version.HistoricalSnapshotPatches.byId(Selection.key()).ifPresent(patch ->
            net.historicalexperience.world.HistoricalSnapshotCreation.bind(newWorldDirectory, patch.id()));
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

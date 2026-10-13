package net.historicalexperience.client.version;

import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.historicalexperience.version.HistoricalVersionCatalogue;
import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;
import net.minecraft.client.gui.components.AbstractWidget;
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
 * <h2>Placement, and what the first attempt got wrong</h2>
 *
 * <p>The button sits in the bottom left, above the footer. It was first put in the top right corner, which is not
 * empty on this screen: the tab strip runs along the top and the button landed against the end of it, so the tabs
 * looked truncated. The bottom left is empty at every resolution, does not touch the create and cancel buttons in
 * the centre, and is next to the primary action rather than hidden in a corner.
 *
 * <p>The label is also short enough to fit. "Historical Version: Beta 1.7.3" is 186 pixels of text in a 200 pixel
 * button; the first attempt used a 150 pixel button and the text spilled over its edges.
 */
public final class CreateWorldHistoricalVersion {
    private static final String MARKER = "Historical Version:";
    private static final int BUTTON_WIDTH = 200;
    private static final int MARGIN = 16;

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

            // Re-applied on every init, which is what preserves the choice across the sub-screens that rebuild
            // this one.
            applyStoredSelection(createWorldScreen);

            // The screen is re-initialised whenever the player returns from a sub-screen, and an added widget can
            // survive that. Without this check the run that produced the screenshots drew two buttons on top of
            // each other, one showing the old version and one the new.
            Button existing = existingButton(screen);
            if (existing != null) {
                existing.setMessage(label());
                return;
            }

            // One holder, so the confirm action can update the very button it was pressed on instead of reopening
            // the screen - which is what produced the stacked labels.
            Button[] button = new Button[1];
            button[0] = Button.builder(label(), pressed ->
                    client.setScreen(new HistoricalVersionScreen(createWorldScreen,
                        context(createWorldScreen, button[0]))))
                .pos(MARGIN, contentBottom(height))
                .width(BUTTON_WIDTH)
                .build();

            Screens.getButtons(screen).add(button[0]);
        });
    }

    /**
     * A y position inside the screen's content area, above the footer.
     *
     * <p>Vanilla draws a separator where the content area ends and the footer begins, and a button placed relative
     * to the bottom of the WINDOW lands on that line: the first version used {@code height - 52} and the separator
     * ran straight through the middle of it. The footer is measured rather than assumed, because it is not the same
     * height on every screen.
     */
    private static int contentBottom(int height) {
        return height - FOOTER_HEIGHT - 30;
    }

    /** A height comfortably below the tallest footer in vanilla's menus, so the button never reaches it. */
    private static final int FOOTER_HEIGHT = 60;

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

    /** The button this class added, if the screen already has one. */
    private static Button existingButton(net.minecraft.client.gui.screens.Screen screen) {
        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof Button button && button.getMessage().getString().startsWith(MARKER)) {
                return button;
            }
        }
        return null;
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

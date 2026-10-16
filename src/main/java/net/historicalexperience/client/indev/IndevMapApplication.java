/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.client.indev;

import java.util.Locale;

import mod.bluestaggo.modernerbeta.client.gui.screen.ModernBetaWorldScreenProvider;
import mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource;
import mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator;
import mod.bluestaggo.modernerbeta.level.chunk.provider.indev.IndevTheme;
import mod.bluestaggo.modernerbeta.level.chunk.provider.indev.IndevType;
import mod.bluestaggo.modernerbeta.settings.ModernBetaSettings;
import mod.bluestaggo.modernerbeta.settings.SettingsComponentTypes;
import mod.bluestaggo.modernerbeta.settings.component.FiniteLevelProperties;
import net.historicalexperience.client.version.IndevMapSelection;
import net.historicalexperience.worldgen.IndevMapSettings;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Hands the chosen original Indev map settings to the world being created.
 *
 * <h2>Why this is not in the screen package</h2>
 *
 * <p>Deliberately beside the interface rather than inside it. The version GUI is guarded against naming generators at
 * all (see {@code VersionGuiWiringTest}), and that guard is worth keeping: presentation should not know what a chunk
 * generator is. Everything that has to touch one lives here, and the same test forbids here the things that would
 * make this file a second generator layer - constructing a generator, resolving a settings preset, touching noise
 * settings, or reading settings JSON.
 *
 * <h2>Why it goes through Moderner Beta's own API</h2>
 *
 * <p>This class reads the settings the selected world type already resolved, changes the one component the original
 * options live in, and asks upstream to rebuild the creation context from it:
 *
 * <ul>
 *   <li>{@code ModernBetaChunkGenerator.getChunkSettings()} and {@code ModernBetaBiomeSource.getBiomeSettings()} /
 *       {@code getCaveBiomeSettings()} are upstream's own readers, so no settings JSON is parsed or assembled here
 *       and no generator is constructed by this project.</li>
 *   <li>{@code ModernBetaWorldScreenProvider.createModifier(...)} is the very modifier upstream's settings editor
 *       uses, and {@code WorldCreationUiState.updateDimensions(...)} is vanilla's supported way to apply one.</li>
 * </ul>
 *
 * <p>Nothing here writes a file, edits a save, or replaces a stored recipe: it changes the context vanilla is about
 * to write into a brand-new world's level data, which is exactly what choosing a world type at creation means. A
 * world created without choosing keeps upstream's own preset untouched.
 *
 * <h2>How a world is recognised as Indev's</h2>
 *
 * <p>By its actual generator, not by a name: the overworld must be a Moderner Beta generator whose settings carry
 * {@code finite_level_properties} at all. Ordinary worlds, Infdev and every later era resolve to nothing here, so
 * the options cannot appear where they do not belong.
 */
public final class IndevMapApplication {
    private IndevMapApplication() {
    }

    /**
     * Whether the version being created is one of the finite/Indev profiles.
     *
     * <p>Asked of the project's own registry rather than inferred from the generator's settings. The native run proved
     * that a settings object - on the client during creation as much as on a loaded server - is a DELTA holding a
     * preset reference ({@code historical_experience:indev_latest_with_ores}) and none of the components the preset
     * resolves to, so a component check silently answered "not Indev" and the options were never applied. The
     * selection is the honest source: it is what the player chose, and the registry knows which profiles are finite.
     */
    private static boolean selectionIsFinite() {
        String key = net.historicalexperience.client.version.CreateWorldHistoricalVersion.Selection.key();
        if (key.isBlank()) {
            return false;
        }
        String profile = net.historicalexperience.version.HistoricalSnapshotPatches
            .baseProfileForSelection(key).orElse(key);
        return net.historicalexperience.version.HistoricalVersions.isFiniteWorld(profile);
    }

    /** The Moderner Beta generator this creation is using, or null when it is not one. */
    private static ModernBetaChunkGenerator modernerBetaGenerator(CreateWorldScreen screen) {
        if (screen == null || screen.getUiState() == null || screen.getUiState().getSettings() == null) {
            return null;
        }
        ChunkGenerator generator = screen.getUiState().getSettings().selectedDimensions().overworld();
        return generator instanceof ModernBetaChunkGenerator modern ? modern : null;
    }

    /** Whether this world creation can be given original Indev map settings at all. */
    public static boolean offered(CreateWorldScreen screen) {
        return modernerBetaGenerator(screen) != null && selectionIsFinite();
    }

    /**
     * What the world's generator currently holds, in one line, or blank when this is not a finite Indev world.
     *
     * <p>Read rather than assumed: it is how the screen can say that the pinned upstream preset uses 256x256x128
     * while the researched table for Normal square says 64, instead of hiding that disagreement. A delta carries only
     * the preset reference, so that is reported when the component itself is not inline.
     */
    public static String currentDescription(CreateWorldScreen screen) {
        ModernBetaChunkGenerator generator = modernerBetaGenerator(screen);
        if (generator == null) {
            return "";
        }
        ModernBetaSettings settings = generator.getChunkSettings();
        FiniteLevelProperties properties = settings.get(SettingsComponentTypes.FINITE_LEVEL_PROPERTIES);
        if (properties == null) {
            ResourceLocation preset = settings.get(SettingsComponentTypes.PRESET);
            return preset == null ? "(settings held as a preset this build cannot name)" : preset.toString();
        }
        return properties.type().getSerializedName() + ", " + properties.theme().getSerializedName() + ", "
            + properties.width() + "x" + properties.length() + "x" + properties.height();
    }

    /**
     * Applies a choice to the creation context.
     *
     * @return true when the context accepted it. False means "not an Indev world right now" or "that type cannot be
     *     generated upstream", both of which the caller reports rather than treating as success.
     */
    public static boolean apply(CreateWorldScreen screen, IndevMapSettings.Choice choice) {
        ModernBetaChunkGenerator generator = modernerBetaGenerator(screen);
        if (generator == null || choice == null || !selectionIsFinite()
                || !IndevMapSettings.available(choice.type()).isEmpty()) {
            return false;
        }
        if (!(generator.getBiomeSource() instanceof ModernBetaBiomeSource biomes)) {
            return false;
        }
        if (choice.equals(IndevMapSettings.DEFAULT)) {
            // The default option names ARE what the project already ships, so nothing is written into the world: the
            // world type's own preset is left to speak for itself. This is not tidiness. An inline override makes the
            // world "customized" to the finite-to-infinite migration, which refuses customized finite settings because
            // its apron release is verified only against the default map - the owner hit exactly that as "Conversion
            // failed: Cannot establish safe default finite source identity". Selecting the defaults must not cost the
            // player the Infdev upgrade, so this re-applies the preset and drops any override an earlier choice wrote.
            net.historicalexperience.client.version.CreateWorldHistoricalVersion.resetToPresetSettings(screen);
            return true;
        }
        // ADD, not replace: these settings are a preset DELTA, so the component is absent from the object and only
        // reachable through the preset reference - and `replace` leaves an absent component absent, which silently
        // discarded the whole choice while the biome override below still took effect. The native run caught exactly
        // that asymmetry: the theme arrived, the dimensions did not.
        ModernBetaSettings changed = ModernBetaSettings.builder(generator.getChunkSettings())
            .add(SettingsComponentTypes.FINITE_LEVEL_PROPERTIES, properties(choice))
            .build();
        // A theme is expressed in TWO places upstream: the chunk settings' finite_level_properties.theme drives the
        // terrain and boundary liquid, and the biome settings' single biome decides which biome the whole map is.
        // Setting only the first generated Hell terrain under a Normal biome, which the native run caught.
        ModernBetaSettings themedBiomes = ModernBetaSettings.builder(biomes.getBiomeSettings())
            .add(SettingsComponentTypes.SINGLE_BIOME, themeBiome(screen, choice.theme()))
            .build();
        screen.getUiState().updateDimensions(ModernBetaWorldScreenProvider.createModifier(
            changed, themedBiomes, biomes.getCaveBiomeSettings()));
        return true;
    }

    /** Upstream's own biome for a theme: {@code moderner_beta:indev_normal/hell/paradise/woods}. */
    private static Holder<Biome> themeBiome(CreateWorldScreen screen, IndevMapSettings.Theme theme) {
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("moderner_beta", "indev_" + theme.id()));
        return screen.getUiState().getSettings().worldgenLoadContext().lookupOrThrow(Registries.BIOME).getOrThrow(key);
    }

    /**
     * Re-applies the stored choice after the creation screen or the chosen world type changed.
     *
     * <p>Setting a world type rebuilds the context from the preset, which drops any earlier modifier, so this has to
     * run after every world-type change rather than once. Silent for worlds that are not Indev's.
     */
    public static void reapplyIfChosen(CreateWorldScreen screen) {
        if (IndevMapSelection.chosen()) {
            apply(screen, IndevMapSelection.choice());
        }
    }

    /** The chosen type and theme in upstream's own enums, and the researched dimensions. */
    private static FiniteLevelProperties properties(IndevMapSettings.Choice choice) {
        return new FiniteLevelProperties(
            IndevType.valueOf(choice.type().id().toUpperCase(Locale.ROOT)),
            IndevTheme.valueOf(choice.theme().id().toUpperCase(Locale.ROOT)),
            choice.width(), choice.length(), choice.height());
    }
}

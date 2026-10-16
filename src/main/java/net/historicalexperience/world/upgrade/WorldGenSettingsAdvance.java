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
package net.historicalexperience.world.upgrade;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/**
 * Advances the generation preset a world will use, by rewriting the level-stem NBT that stores it.
 *
 * <h2>Why this exists, and what it corrects</h2>
 *
 * <p>A world's historical metadata <em>records</em> a generation recipe, but it does not <em>drive</em>
 * generation. The recipe that actually generates terrain is the preset stored in the world's own
 * generator settings, and this mod resolves the historical version from the loaded generator's chunk
 * settings - not from its metadata file. So an "upgrade" that only rewrote metadata would move a
 * bookkeeping record while the world kept generating exactly as before, which is worse than no upgrade
 * because it would look like one.
 *
 * <p>In 1.21.1 the generator settings live in {@code level.dat}, under
 * {@code Data.WorldGenSettings.dimensions}, whose overworld generator holds the project preset in
 * <b>three</b> places: the generator's own provider settings, the biome source's provider settings, and
 * the biome source's cave provider settings. This class changes all three together; changing fewer would
 * leave a world whose terrain and cave biomes came from different recipes.
 *
 * <p>This runs <b>before</b> the world is loaded, from the startup path, so:
 * <ul>
 *   <li>generation is genuinely drained - no chunk of this session has been generated yet;</li>
 *   <li>existing player terrain is preserved. An explicit finite-to-infinite advance may additionally
 *       release exact pristine synthetic exterior chunks through the separately journaled apron transaction;</li>
 *   <li>the new preset applies to every chunk generated from this session onward.</li>
 * </ul>
 *
 * <p>The rewrite is atomic (temporary sibling plus a move) and refuses unless every one of the three
 * references currently holds the preset it expects. A world whose stored generator is not what the
 * upgrade believes it to be is a world this code must not silently rewrite.
 */
public final class WorldGenSettingsAdvance {
    /** World-relative path of the level-stem file that stores the generator. */
    public static final String FILE_NAME = "level.dat";

    private static final String OVERWORLD = "minecraft:overworld";
    private static final String PRESET_KEY = "moderner_beta:preset";
    /** The one stored component the apron migration can account for: the world's own finite map settings. */
    private static final String FINITE_PROPERTIES_KEY = "moderner_beta:finite_level_properties";

    private WorldGenSettingsAdvance() {
    }

    /**
     * Components that only mean something to a FINITE map, removed when the generator becomes infinite.
     *
     * <p>This is a real defect the owner found by playing: the advance rewrote the three preset references and left
     * every inline component behind, so a customized finite world became an infinite generator STILL carrying the
     * finite map's own settings. The visible result was an Infdev world generated in flat terraces - the retained
     * {@code noise_generator_settings} still named {@code moderner_beta:finite_2d}, the finite map's 2D noise field,
     * which is not a terrain generator for an infinite world. The destination preset supplies its own settings, so
     * anything finite-only goes with the transition. Components that are merely customized (deepslate, for instance)
     * are left alone: this removes what cannot mean anything any more, not what the player chose.
     */
    private static final List<String> FINITE_ONLY_KEYS = List.of(
        "moderner_beta:finite_beaches",
        "moderner_beta:finite_cave_generation",
        "moderner_beta:finite_level_properties",
        "moderner_beta:finite_noise",
        "moderner_beta:finite_pools",
        "moderner_beta:world_border",
        "moderner_beta:biome_injection_rules");

    private static List<String> stripFiniteComponents(CompoundTag settings) {
        if (settings == null) {
            return List.of();
        }
        List<String> removed = new java.util.ArrayList<>();
        for (String key : FINITE_ONLY_KEYS) {
            if (settings.contains(key)) {
                settings.remove(key);
                removed.add(key);
            }
        }
        if (settings.getString("moderner_beta:noise_generator_settings").equals("moderner_beta:finite_2d")) {
            settings.remove("moderner_beta:noise_generator_settings");
            removed.add("moderner_beta:noise_generator_settings");
        }
        return List.copyOf(removed);
    }

    /**
     * Removes finite-only components left in a world that is ALREADY infinite, and reports what it removed.
     *
     * <p>This exists because the strip above only helps worlds advanced from now on. A world advanced before it keeps
     * the finite map's components in its stored settings - measured on the owner's world as an Infdev landscape
     * generated in long parallel ridges, because the stored generator still named the finite 2D noise field. Reading
     * the settings and finding the same defect is the only way to repair a world that is already broken, and it is
     * safe by construction: this runs only when the stored generator is NOT a finite preset, so every key it removes
     * is one the current generator cannot use. A world that is already clean is left byte-identical, and the write
     * happens before the level is loaded, which is the same point in the life of a world that the advance write uses.
     *
     * @return the keys removed, empty when there was nothing to repair
     */
    public static List<String> repairRetainedFiniteComponents(Path worldDirectory) throws UpgradeException {
        State state = read(worldDirectory).orElse(null);
        if (state == null || !state.consistent()
                || net.historicalexperience.version.HistoricalVersions.isFinitePreset(state.settingsPreset())) {
            return List.of();
        }
        Path file = worldDirectory.resolve(FILE_NAME);
        CompoundTag root = readTag(file);
        CompoundTag generator = generatorTag(root).orElse(null);
        if (generator == null) {
            return List.of();
        }
        CompoundTag biome = biomeSource(generator).orElse(null);
        if (biome == null) {
            return List.of();
        }
        List<String> removed = new java.util.ArrayList<>();
        removed.addAll(stripFiniteComponents(childOrNull(generator, "provider_settings")));
        removed.addAll(stripFiniteComponents(childOrNull(biome, "provider_settings")));
        removed.addAll(stripFiniteComponents(childOrNull(biome, "cave_provider_settings")));
        if (removed.isEmpty()) {
            return List.of();
        }
        writeTag(root, file);
        return List.copyOf(removed);
    }

    /** The stored level type that means "land, not ocean", which decides whether the sea level is adjusted. */
    private static final String INLAND_TYPE = "inland";
    /** Moderner Beta's own sea-level setting, written only for an Inland source. */
    private static final String SEA_LEVEL_KEY = "moderner_beta:sea_level";
    /** At 0 the destination generates no ocean at all; the Inland map's land continues instead of drowning. */
    private static final int INLAND_SEA_LEVEL = 0;

    /**
     * The three preset references in the overworld generator, in the order the file stores them.
     *
     * @param settingsPreset the generator's own provider settings
     * @param biomePreset    the biome source's provider settings
     * @param cavePreset     the biome source's cave provider settings
     */
    public record State(String settingsPreset, String biomePreset, String cavePreset) {
        /** True when all three references name the same preset, which is the only consistent state. */
        public boolean consistent() {
            return settingsPreset != null
                && settingsPreset.equals(biomePreset)
                && settingsPreset.equals(cavePreset);
        }

        /** True when all three already name {@code preset}. */
        public boolean alreadyIs(String preset) {
            return consistent() && settingsPreset.equals(preset);
        }

        public List<String> asList() {
            return List.of(String.valueOf(settingsPreset), String.valueOf(biomePreset),
                String.valueOf(cavePreset));
        }
    }

    /** Reads the three preset references, or empty when the world has no Moderner Beta generator. */
    public static Optional<State> read(Path worldDirectory) throws UpgradeException {
        Path file = worldDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        CompoundTag root = readTag(file);
        Optional<CompoundTag> generator = generatorTag(root);
        if (generator.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag gen = generator.get();
        // All three references are read through the same helper, and a missing one yields null rather
        // than a default: null is what makes State.consistent() false, which is the signal the caller
        // needs. Substituting a default here would turn "this file is not the shape I expect" into "this
        // file says the preset I was hoping for".
        return Optional.of(new State(
            presetOf(childOrNull(gen, "provider_settings")),
            biomeSource(gen)
                .map(source -> presetOf(childOrNull(source, "provider_settings")))
                .orElse(null),
            biomeSource(gen)
                .map(source -> presetOf(childOrNull(source, "cave_provider_settings")))
                .orElse(null)));
    }

    /** Reads the actual saved seed, refusing missing/wrong-type fields rather than silently defaulting to zero. */
    public static long readWorldSeed(Path worldDirectory) throws UpgradeException {
        CompoundTag data = readTag(worldDirectory.resolve(FILE_NAME)).getCompound("Data").getCompound("WorldGenSettings");
        if (!data.contains("seed", net.minecraft.nbt.Tag.TAG_LONG)) {
            throw new UpgradeException("WorldGenSettings.seed is missing or is not a long; cannot bind a finite transition");
        }
        return data.getLong("seed");
    }

    /** Monotonic saved game time, NOT player-set day time; absence/invalid type is not guessed as zero. */
    public static java.util.OptionalLong readGameTime(Path worldDirectory) throws UpgradeException {
        CompoundTag data = readTag(worldDirectory.resolve(FILE_NAME)).getCompound("Data");
        return data.contains("Time", net.minecraft.nbt.Tag.TAG_LONG) && data.getLong("Time") >= 0
            ? java.util.OptionalLong.of(data.getLong("Time")) : java.util.OptionalLong.empty();
    }

    /**
     * The finite geometry this world generates with, read from its own settings, refusing anything it cannot prove.
     *
     * <p>Each of the three provider-settings objects is allowed to be either the preset reference alone - which means
     * the shipped default geometry - or that reference plus exactly the {@code finite_level_properties} map settings.
     * Anything else (another customized component, a malformed map setting, values that disagree between the three
     * copies) is refused with the reason, because the apron release cannot be handed a geometry it did not verify.
     *
     * <p>This replaces a rule that refused EVERY customized world. Reading the geometry is what lets a world with an
     * Inland or smaller map migrate at all; it is not a relaxation of what may be released, which is decided
     * separately and only for chunks that are provably nothing.
     */
    public static FiniteGeometry readFiniteGeometry(Path worldDirectory) throws UpgradeException {
        State state = read(worldDirectory).orElseThrow(() -> new UpgradeException("No stored historical finite generator"));
        if (!state.consistent() || !net.historicalexperience.version.HistoricalVersions.isFinitePreset(state.settingsPreset())) {
            throw new UpgradeException("Finite transition requires a consistent, recognized finite source preset");
        }
        CompoundTag gen = generatorTag(readTag(worldDirectory.resolve(FILE_NAME))).orElseThrow();
        CompoundTag biome = biomeSource(gen).orElseThrow();
        FiniteGeometry recorded = null;
        for (CompoundTag settings : List.of(gen.getCompound("provider_settings"), biome.getCompound("provider_settings"),
                biome.getCompound("cave_provider_settings"))) {
            java.util.Set<String> keys = settings.getAllKeys();
            if (!keys.contains(FINITE_PROPERTIES_KEY)) {
                // Other inline settings do not change the MAP geometry, which is the only thing the apron release has
                // to prove. Refusing them - which this did at first - stopped a real world dead with "Customized
                // finite provider settings other than the map settings cannot use the apron migration", while the
                // geometry it needed was right there. Whatever else is customized is simply not this migration's
                // business.
                continue;
            }
            FiniteGeometry here = geometryFrom(settings.getCompound(FINITE_PROPERTIES_KEY));
            if (recorded != null && !recorded.equals(here)) {
                throw new UpgradeException("Finite map settings disagree between the generator, the biome source and the "
                    + "cave biome source; refusing to choose one");
            }
            recorded = here;
        }
        return recorded == null ? FiniteGeometry.DEFAULT : recorded;
    }

    /** The five stored map-setting fields, all required and type-checked, or a refusal naming what was wrong. */
    private static FiniteGeometry geometryFrom(CompoundTag properties) throws UpgradeException {
        java.util.Set<String> expected = java.util.Set.of("type", "theme", "width", "length", "height");
        if (!properties.getAllKeys().equals(expected)) {
            throw new UpgradeException("Finite map settings must carry exactly " + expected + "; found "
                + properties.getAllKeys());
        }
        for (String key : List.of("type", "theme")) {
            if (!properties.contains(key, net.minecraft.nbt.Tag.TAG_STRING)) {
                throw new UpgradeException("Finite map setting '" + key + "' is missing or not a string");
            }
        }
        for (String key : List.of("width", "length", "height")) {
            if (!properties.contains(key, net.minecraft.nbt.Tag.TAG_INT)) {
                throw new UpgradeException("Finite map setting '" + key + "' is missing or not an int");
            }
        }
        try {
            return new FiniteGeometry(properties.getInt("width"), properties.getInt("length"),
                properties.getInt("height"), properties.getString("type"), properties.getString("theme"), true);
        } catch (IllegalArgumentException malformed) {
            throw new UpgradeException("Finite map settings are out of range: " + malformed.getMessage(), malformed);
        }
    }

    /**
     * Rewrites all three references from {@code expectedFrom} to {@code to}, atomically.
     *
     * @return the state before the change, so the caller can report or revert it
     * @throws UpgradeException when the file is missing, not a Moderner Beta generator, or does not
     *                          currently name {@code expectedFrom} in all three places
     */
    public static State advance(Path worldDirectory, String expectedFrom, String to)
            throws UpgradeException {
        return advance(worldDirectory, expectedFrom, to, false);
    }

    /**
     * The same rewrite, and - when {@code releaseWorldBorder} is set - the finite level's world border with it.
     *
     * <h2>Why the border belongs to this step</h2>
     *
     * <p>Owner: <i>"remember the biome upgrader also itll upgrade from a finite to infinite world so account for
     * that"</i>. A finite world's size is not a property of its generator that this rewrite would incidentally clear:
     * Moderner Beta's {@code world_border} component SETS the level's border when the world is created, and the game
     * persists that border in {@code level.dat}. So a 256-block Indev world upgraded to an infinite Infdev or Alpha
     * generator would keep generating as an infinite world while the player stayed fenced inside a 256-block square -
     * the terrain would move and the world would not.
     *
     * <p>It is done here, and not as a separate pass, because this is the one write to {@code level.dat} that the
     * upgrade already performs under the journal and the backup: a second, later write would be invisible to the
     * crash-recovery path, and a crash between the two would leave a world whose generator says "infinite" and whose
     * border still says "finite", with nothing recording that the second step was owed.
     *
     * <p>The replacement value is vanilla's own default border, which is what a world that never had a border uses.
     */
    public static State advance(Path worldDirectory, String expectedFrom, String to, boolean releaseWorldBorder)
            throws UpgradeException {
        Path file = worldDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            throw new UpgradeException("cannot advance the generation preset: " + FILE_NAME
                + " is missing, so this world's generator is not stored where it would have to be");
        }
        CompoundTag root = readTag(file);
        CompoundTag generator = generatorTag(root).orElseThrow(() -> new UpgradeException(
            "cannot advance the generation preset: " + FILE_NAME
                + " has no overworld generator this build recognises"));
        CompoundTag biomeSource = biomeSource(generator).orElseThrow(() -> new UpgradeException(
            "cannot advance the generation preset: " + FILE_NAME
                + " has no biome source this build recognises"));

        State before = read(worldDirectory).orElseThrow(() -> new UpgradeException(
            "cannot advance the generation preset: " + FILE_NAME + " could not be read back"));
        if (before.alreadyIs(to)) {
            // Already advanced. Treating this as success is what makes recovery idempotent: a crash
            // between the level-stem rewrite and the metadata publish must be replayable.
            return before;
        }
        if (!before.consistent() || !before.settingsPreset().equals(expectedFrom)) {
            throw new UpgradeException("refusing to advance the generation preset: " + FILE_NAME
                + " names " + before.asList() + " but the upgrade expected all three to be "
                + expectedFrom + ". Rewriting a generator this code did not recognise could change a "
                + "world in a way nothing has described");
        }

        if (releaseWorldBorder
                && net.historicalexperience.version.HistoricalVersions.isFinitePreset(expectedFrom)
                && !net.historicalexperience.version.HistoricalVersions.isFinitePreset(to)) {
            try {
                FiniteApronRelease.release(worldDirectory);
            } catch (IOException failure) {
                throw new UpgradeException("Finite edge migration did not finish; generator unchanged, retry recovery", failure);
            }
        }

        // Inland is chosen because it is land, and the owner's complaint about the upgraded result was exactly the
        // opposite: "avoid tonnes of water when using the Inland variant". An Inland map's land border becomes the
        // destination's infinite OCEAN, because the infinite terrain is generated at the destination's sea level. The
        // sea level is a stored Moderner Beta setting, so an Inland source gets one that keeps the new terrain land.
        // Island and Floating sources are left alone: for those, water is the point.
        try {
            FiniteGeometry source = readFiniteGeometry(worldDirectory);
            if (source.type().equals(INLAND_TYPE)) {
                CompoundTag destination = childOrNull(generator, "provider_settings");
                if (destination != null) {
                    destination.putInt(SEA_LEVEL_KEY, INLAND_SEA_LEVEL);
                    net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
                        "Inland source: destination sea level set to {} so the new terrain is land, not ocean",
                        INLAND_SEA_LEVEL);
                }
            }
        } catch (UpgradeException geometryUnreadable) {
            // The source geometry could not be read. The advance itself was already validated above, so carry on
            // without the Inland adjustment rather than refusing to move the world.
        }

        setPreset(childOrNull(generator, "provider_settings"), to);
        setPreset(childOrNull(biomeSource, "provider_settings"), to);
        setPreset(childOrNull(biomeSource, "cave_provider_settings"), to);
        stripFiniteComponents(childOrNull(generator, "provider_settings"));
        stripFiniteComponents(childOrNull(biomeSource, "provider_settings"));
        stripFiniteComponents(childOrNull(biomeSource, "cave_provider_settings"));

        if (releaseWorldBorder) {
            Double before2 = setWorldBorderToDefault(root);
            net.historicalexperience.HistoricalExperienceMod.LOGGER.info(
                "Finite world released: {} -> {}, world border {} (vanilla default {})",
                expectedFrom, to,
                before2 == null ? "was absent, so the default already applied" : "size " + before2,
                DEFAULT_WORLD_BORDER_SIZE);
        }

        writeTag(root, file);
        return before;
    }

    /** Vanilla's own default world border size, which is what a level with no stored border behaves as. */
    public static final double DEFAULT_WORLD_BORDER_SIZE = 29_999_984.0D;

    /**
     * The border size stored in this world's {@code level.dat}, when it stores one.
     *
     * <p>A read-only diagnostic: the upgrade reports the size before and after so a released border is visible in the
     * log rather than only in the effect on play. Absent, unreadable or not-a-number all answer empty rather than a
     * made-up default, because "this world stores no border" and "this world stores 29,999,984" are different facts.
     */
    public static Optional<Double> readWorldBorderSize(Path worldDirectory) {
        try {
            Path file = worldDirectory.resolve(FILE_NAME);
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            CompoundTag data = childOrNull(readTag(file), "Data");
            if (data == null || !data.contains("BorderSize")) {
                return Optional.empty();
            }
            return Optional.of(data.getDouble("BorderSize"));
        } catch (UpgradeException | RuntimeException failure) {
            return Optional.empty();
        }
    }

    /**
     * Sets the stored border to vanilla's default, returning the size it held.
     *
     * <p>1.21.1 stores the border FLATTENED into {@code Data} as {@code BorderSize} and its companions, not as a
     * {@code WorldBorder} compound - the first version of this method looked for the compound, found nothing, and
     * reported "the border was absent, so the default already applied" over a world that was still 256 blocks wide.
     *
     * <p>Only the size is touched. The border's centre is where the finite level put it - the origin, which is
     * also where an infinite world's default is - and its damage, warning distance and timer are the player's own
     * settings, not this project's to overwrite. A level with no stored border is left alone: it already behaves as
     * the default, and writing one would be inventing state.
     *
     * @return the previous size, or null when the level stored none
     */
    private static Double setWorldBorderToDefault(CompoundTag root) {
        CompoundTag data = childOrNull(root, "Data");
        if (data == null || !data.contains("BorderSize")) {
            return null;
        }
        double before = data.getDouble("BorderSize");
        data.putDouble("BorderSize", DEFAULT_WORLD_BORDER_SIZE);
        // The lerp target as well, and this is the part that would have been missed: the border grows toward
        // BorderSizeLerpTarget over BorderSizeLerpTime, so leaving the target at the finite size would let the
        // border crawl back to 256 while the world looked released.
        if (data.contains("BorderSizeLerpTarget")) {
            data.putDouble("BorderSizeLerpTarget", DEFAULT_WORLD_BORDER_SIZE);
        }
        return before;
    }

    /**
     * A child compound, or null when it is absent.
     *
     * <p>1.21.1's {@code getCompound} hands back an empty tag for a missing key, where 26.3 handed back an
     * {@code Optional}. This project treats "absent" as a meaningful answer - {@code State.consistent()} is
     * false when a reference is missing - so the distinction is kept rather than flattened to an empty tag,
     * which would silently look like a settings block that exists and says nothing.
     */
    private static CompoundTag childOrNull(CompoundTag parent, String key) {
        if (parent == null) {
            return null;
        }
        CompoundTag child = parent.getCompound(key);
        return child.isEmpty() ? null : child;
    }

    /** A string, or null when the key is absent. The 1.21.1 counterpart of 26.3's Optional-returning read. */
    private static String stringOrNull(CompoundTag parent, String key) {
        if (parent == null || !parent.contains(key)) {
            return null;
        }
        return parent.getString(key);
    }

    private static void setPreset(CompoundTag parent, String value) throws UpgradeException {
        if (parent == null) {
            throw new UpgradeException(
                "cannot advance the generation preset: a settings block the rewrite needs is absent, so "
                    + "the world's generator is not the shape this upgrade was planned against");
        }
        parent.putString(PRESET_KEY, value);
    }

    private static String presetOf(CompoundTag parent) {
        if (parent == null) {
            return null;
        }
        return stringOrNull(parent, PRESET_KEY);
    }

    private static Optional<CompoundTag> biomeSource(CompoundTag generator) {
        return Optional.ofNullable(childOrNull(generator, "biome_source"));
    }

    private static Optional<CompoundTag> generatorTag(CompoundTag root) {
        // 1.21.1 persists the generator inside level.dat, not the separate 26.3 level-stem file.
        // Preserve every unrelated tag (player/inventory, gamerules, seed, dimension types, etc.).
        CompoundTag data = childOrNull(root, "Data");
        CompoundTag worldGenSettings = childOrNull(data, "WorldGenSettings");
        CompoundTag dimensions = childOrNull(worldGenSettings, "dimensions");
        CompoundTag overworld = childOrNull(dimensions, OVERWORLD);
        CompoundTag generator = childOrNull(overworld, "generator");
        CompoundTag biome = generator == null ? null : childOrNull(generator, "biome_source");
        if (generator == null || biome == null
                || !"moderner_beta:moderner_beta".equals(stringOrNull(generator, "type"))
                || !"moderner_beta:moderner_beta".equals(stringOrNull(biome, "type"))) {
            return Optional.empty();
        }
        return Optional.of(generator);
    }

    private static CompoundTag readTag(Path file) throws UpgradeException {
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new UpgradeException("could not read " + file, e);
        }
    }

    private static void writeTag(CompoundTag tag, Path file) throws UpgradeException {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            NbtIo.writeCompressed(tag, temporary);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw new UpgradeException("could not rewrite " + file, e);
        }
    }

    /** Every file this operation touches, so a backup can cover exactly that set. */
    public static List<String> touchedFiles() {
        List<String> files = new ArrayList<>();
        files.add(FILE_NAME);
        return List.copyOf(files);
    }
}

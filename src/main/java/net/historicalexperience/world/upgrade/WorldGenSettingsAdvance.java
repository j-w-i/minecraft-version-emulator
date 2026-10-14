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
 *   <li>existing chunks are untouched, because only this small NBT file is rewritten and no region file
 *       is opened for writing;</li>
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

    private WorldGenSettingsAdvance() {
    }

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

    /**
     * Rewrites all three references from {@code expectedFrom} to {@code to}, atomically.
     *
     * @return the state before the change, so the caller can report or revert it
     * @throws UpgradeException when the file is missing, not a Moderner Beta generator, or does not
     *                          currently name {@code expectedFrom} in all three places
     */
    public static State advance(Path worldDirectory, String expectedFrom, String to)
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

        setPreset(childOrNull(generator, "provider_settings"), to);
        setPreset(childOrNull(biomeSource, "provider_settings"), to);
        setPreset(childOrNull(biomeSource, "cave_provider_settings"), to);

        writeTag(root, file);
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

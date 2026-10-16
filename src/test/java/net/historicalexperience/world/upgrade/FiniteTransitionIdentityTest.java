/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FiniteTransitionIdentityTest {
    static final String INDEV = "historical_experience:indev_latest_with_ores";
    static final String APRIL = "historical_experience:infdev_420_with_ores";
    @TempDir Path temp;

    static void fixture(Path world, long seed, long originTimestamp) throws Exception {
        Files.createDirectories(world);
        var settings = new CompoundTag(); settings.putString("moderner_beta:preset", INDEV);
        var biome = new CompoundTag(); biome.putString("type", "moderner_beta:moderner_beta");
        biome.put("provider_settings", settings.copy()); biome.put("cave_provider_settings", settings.copy());
        var generator = new CompoundTag(); generator.putString("type", "moderner_beta:moderner_beta");
        generator.put("provider_settings", settings.copy()); generator.put("biome_source", biome);
        var dimension = new CompoundTag(); dimension.put("generator", generator);
        var dimensions = new CompoundTag(); dimensions.put("minecraft:overworld", dimension);
        var worldgen = new CompoundTag(); worldgen.put("dimensions", dimensions); worldgen.putLong("seed", seed);
        var data = new CompoundTag(); data.put("WorldGenSettings", worldgen); data.putLong("Time", 100);
        var root = new CompoundTag(); root.put("Data", data);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        HistoricalMetadataStore.forWorldDirectory(world).write(HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:indev_latest"), HistoricalIdentifier.parse(INDEV), "test", originTimestamp));
    }
    static CompoundTag level(Path world) throws IOException {
        return NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap());
    }
    static CompoundTag generator(CompoundTag root) {
        return root.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions")
            .getCompound("minecraft:overworld").getCompound("generator");
    }
    static FiniteTransitionIdentity dummy() {
        return new FiniteTransitionIdentity(0L, "0".repeat(64), INDEV, FiniteGeometry.DEFAULT);
    }

    @Test void missingOrWrongTypedGameTimeIsNotGuessedAsZero() throws Exception {
        Path world = temp.resolve("time"); fixture(world, 1L, 100);
        assertEquals(100L, WorldGenSettingsAdvance.readGameTime(world).orElseThrow());
        var root = level(world); root.getCompound("Data").remove("Time"); NbtIo.writeCompressed(root, world.resolve("level.dat"));
        assertTrue(WorldGenSettingsAdvance.readGameTime(world).isEmpty());
        root.getCompound("Data").putInt("Time", 100); NbtIo.writeCompressed(root, world.resolve("level.dat"));
        assertTrue(WorldGenSettingsAdvance.readGameTime(world).isEmpty());
        root.getCompound("Data").putLong("Time", -1); NbtIo.writeCompressed(root, world.resolve("level.dat"));
        assertTrue(WorldGenSettingsAdvance.readGameTime(world).isEmpty());
    }

    @Test void boundSeedAndOriginSurviveLegitimateVersionAdvance() throws Exception {
        Path world = temp.resolve("world"); fixture(world, -123456789012345L, 100);
        var identity = FiniteTransitionIdentity.fromSource(world);
        identity.verifyWorld(world);
        var store = HistoricalMetadataStore.forWorldDirectory(world);
        store.write(store.read().advanceVersion(HistoricalIdentifier.parse("historical_experience:infdev_420"),
            HistoricalIdentifier.parse(APRIL), "test", "version upgrade to historical_experience:infdev_420 on generation recipe " + APRIL, 200));
        WorldGenSettingsAdvance.advance(world, INDEV, APRIL);
        identity.verifyWorld(world);
        assertEquals(-123456789012345L, identity.worldSeed());
        assertEquals(INDEV, identity.sourcePreset());
    }

    @Test void sameSeedIsNotProofOfSameWorld() throws Exception {
        Path source = temp.resolve("source"), other = temp.resolve("other");
        fixture(source, 123, 100); fixture(other, 123, 101);
        var identity = FiniteTransitionIdentity.fromSource(source);
        assertThrows(IOException.class, () -> identity.verifyWorld(other));
        assertThrows(IOException.class, () -> identity.verifySeed(124));
        identity.verifySeed(123);
    }

    @Test void sourceCloneKeepsItsIdentityRegardlessOfDirectoryName() throws Exception {
        Path source = temp.resolve("source"), copy = temp.resolve("renamed-copy");
        fixture(source, 0, 123); fixture(copy, 0, 123);
        FiniteTransitionIdentity.fromSource(source).verifyWorld(copy);
    }

    @Test void anInlineSettingThatIsNotTheMapGeometryDoesNotChangeTheGeometry() throws Exception {
        // A foreign inline component is no longer a refusal - refusing it stopped a real world dead while the geometry
        // it needed was present - and it must not be mistaken for the map settings either.
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        var root = level(world);
        generator(root).getCompound("provider_settings").putInt("moderner_beta:finite_width", 512);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        var identity = FiniteTransitionIdentity.fromSource(world);
        assertEquals(FiniteGeometry.DEFAULT, identity.geometry(),
            "a component that is not the map settings leaves the geometry as the preset's own");
    }

    @Test void theRefusalNamesTheCauseForMapSettingsThatAreNotWellFormed() throws Exception {
        // The map-settings shape the Indev chooser writes, but with a field missing. Well-formed settings are ACCEPTED
        // now (see below); malformed ones must still refuse, and the message on screen must say what was wrong - the
        // bare wrapper once sent the owner looking for a broken world instead of at their customized map settings.
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        var root = level(world);
        var properties = new CompoundTag();
        properties.putString("type", "floating");
        properties.putString("theme", "hell");
        properties.putInt("width", 64);
        properties.putInt("height", 256); // "length" deliberately absent
        generator(root).getCompound("provider_settings").put("moderner_beta:finite_level_properties", properties);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));

        IOException refusal = assertThrows(IOException.class, () -> FiniteTransitionIdentity.fromSource(world));
        assertTrue(refusal.getMessage().contains("length"),
            "the refusal must name the missing map setting, found: " + refusal.getMessage());
        assertTrue(refusal.getMessage().contains("Cannot establish safe default finite source identity"),
            "and keep the stable clause the upgrade path reports, found: " + refusal.getMessage());
    }

    @Test void aWorldsOwnMapGeometryIsReadAndAcceptedInsteadOfRefused() throws Exception {
        // The whole point of recording the geometry: an Inland (or smaller) map can migrate. Before this, ANY inline
        // override was refused outright, so the owner's Inland world could never advance at all.
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        var root = level(world);
        var properties = new CompoundTag();
        properties.putString("type", "inland");
        properties.putString("theme", "normal");
        properties.putInt("width", 256);
        properties.putInt("length", 256);
        properties.putInt("height", 64);
        generator(root).getCompound("provider_settings").put("moderner_beta:finite_level_properties", properties);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));

        var identity = FiniteTransitionIdentity.fromSource(world);
        assertEquals(new FiniteGeometry(256, 256, 64, "inland", "normal", true), identity.geometry());
        assertTrue(identity.geometry().customized());
        assertEquals("inland/normal 256x256x64 (customized)", identity.geometry().describe());
    }

    @Test void aPresetOnlyWorldStillReportsTheShippedDefaultGeometry() throws Exception {
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        assertEquals(FiniteGeometry.DEFAULT, FiniteTransitionIdentity.fromSource(world).geometry());
        assertFalse(FiniteGeometry.DEFAULT.customized());
    }

    @Test void mapSettingsThatDisagreeBetweenTheThreeCopiesAreRefused() throws Exception {
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        var root = level(world);
        var properties = new CompoundTag();
        properties.putString("type", "floating");
        properties.putString("theme", "normal");
        properties.putInt("width", 64);
        properties.putInt("length", 64);
        properties.putInt("height", 128);
        generator(root).getCompound("provider_settings").put("moderner_beta:finite_level_properties", properties);
        var biome = generator(root).getCompound("biome_source");
        var other = properties.copy();
        other.putInt("width", 512);
        biome.getCompound("provider_settings").put("moderner_beta:finite_level_properties", other);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        IOException refusal = assertThrows(IOException.class, () -> FiniteTransitionIdentity.fromSource(world));
        assertTrue(refusal.getMessage().contains("disagree"), refusal.getMessage());
    }

    @Test void advancingToInfiniteDropsTheFiniteMapsInlineSettings() throws Exception {
        // The owner's report, as a test. A customized finite world keeps its map settings INLINE, and the advance used
        // to leave them: the generator became infinite while still holding finite_level_properties and, worse, a
        // noise_generator_settings reference to the finite map's 2D noise field - which generated an Infdev world in
        // flat terraces. Finite-only components go with the transition; a merely customized one stays.
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        var root = level(world);
        var generated = generator(root);
        var properties = new CompoundTag();
        properties.putString("type", "inland");
        properties.putString("theme", "normal");
        properties.putInt("width", 256);
        properties.putInt("length", 256);
        properties.putInt("height", 128);
        var settings = generated.getCompound("provider_settings");
        settings.put("moderner_beta:finite_level_properties", properties);
        settings.putString("moderner_beta:noise_generator_settings", "moderner_beta:finite_2d");
        settings.put("moderner_beta:world_border", new CompoundTag());
        settings.put("moderner_beta:deepslate_generation", new CompoundTag());
        NbtIo.writeCompressed(root, world.resolve("level.dat"));

        WorldGenSettingsAdvance.advance(world, INDEV, APRIL);

        var after = generator(level(world)).getCompound("provider_settings");
        assertEquals(APRIL, after.getString("moderner_beta:preset"), "the preset is the infinite one");
        assertFalse(after.contains("moderner_beta:finite_level_properties"), "the finite map settings are gone");
        assertFalse(after.contains("moderner_beta:noise_generator_settings"), "and the finite 2D noise reference with them");
        assertFalse(after.contains("moderner_beta:world_border"), "and the finite border component");
        assertTrue(after.contains("moderner_beta:deepslate_generation"),
            "a merely customized component the player chose is left alone");
    }

    @Test void anInlandSourceGetsALandSeaLevelAndOtherSourcesAreLeftAlone() throws Exception {
        // The owner: "avoid tonnes of water when using the Inland variant". An Inland map's land border otherwise
        // becomes the destination's infinite ocean, so the destination's own sea-level setting is lowered for Inland
        // only - an Island map is water by design and must keep it.
        Path inland = temp.resolve("inland"); fixture(inland, 42, 1);
        writeGeometry(inland, "inland");
        WorldGenSettingsAdvance.advance(inland, INDEV, APRIL);
        assertEquals(0, generator(level(inland)).getCompound("provider_settings")
            .getInt("moderner_beta:sea_level"), "an Inland source generates land, not ocean");

        Path island = temp.resolve("island"); fixture(island, 42, 2);
        writeGeometry(island, "island");
        WorldGenSettingsAdvance.advance(island, INDEV, APRIL);
        assertFalse(generator(level(island)).getCompound("provider_settings")
            .contains("moderner_beta:sea_level"), "an Island source keeps the destination's own sea level");
    }

    /** Records finite map settings of the given level type in the generator's provider settings. */
    private static void writeGeometry(Path world, String type) throws Exception {
        var root = level(world);
        var properties = new CompoundTag();
        properties.putString("type", type);
        properties.putString("theme", "normal");
        properties.putInt("width", 256);
        properties.putInt("length", 256);
        properties.putInt("height", 128);
        generator(root).getCompound("provider_settings")
            .put("moderner_beta:finite_level_properties", properties);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
    }

    @Test void zeroSeedIsValidButMissingSeedDoesNotDefaultToIt() throws Exception {
        Path world = temp.resolve("world"); fixture(world, 0, 1);
        assertEquals(0, FiniteTransitionIdentity.fromSource(world).worldSeed());
        var root = level(world); root.getCompound("Data").getCompound("WorldGenSettings").remove("seed");
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        assertThrows(IOException.class, () -> FiniteTransitionIdentity.fromSource(world));
    }

    @Test void metadataAbsenceAndMalformedIdentityFailClosed() throws Exception {
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        // A separate directory with only a level.dat is not silently adopted as historical.
        Path ordinary = temp.resolve("ordinary"); Files.createDirectories(ordinary);
        Files.copy(world.resolve("level.dat"), ordinary.resolve("level.dat"));
        assertThrows(IOException.class, () -> FiniteTransitionIdentity.fromSource(ordinary));
        assertThrows(IllegalArgumentException.class, () -> new FiniteTransitionIdentity(null, "0".repeat(64), INDEV, FiniteGeometry.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new FiniteTransitionIdentity(1L, "short", INDEV, FiniteGeometry.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> new FiniteTransitionIdentity(1L, "0".repeat(64), APRIL, FiniteGeometry.DEFAULT));
    }
}

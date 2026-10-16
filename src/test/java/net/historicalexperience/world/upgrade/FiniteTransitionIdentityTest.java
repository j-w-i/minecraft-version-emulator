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
        return new FiniteTransitionIdentity(0L, "0".repeat(64), INDEV);
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

    @Test void inlineCustomFiniteSettingsAreNotTreatedAsDefaultGeometry() throws Exception {
        Path world = temp.resolve("world"); fixture(world, 42, 1);
        var root = level(world);
        generator(root).getCompound("provider_settings").putInt("moderner_beta:finite_width", 512);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        byte[] before = Files.readAllBytes(world.resolve("level.dat"));
        assertThrows(IOException.class, () -> FiniteTransitionIdentity.fromSource(world));
        assertArrayEquals(before, Files.readAllBytes(world.resolve("level.dat")));
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
        assertThrows(IllegalArgumentException.class, () -> new FiniteTransitionIdentity(null, "0".repeat(64), INDEV));
        assertThrows(IllegalArgumentException.class, () -> new FiniteTransitionIdentity(1L, "short", INDEV));
        assertThrows(IllegalArgumentException.class, () -> new FiniteTransitionIdentity(1L, "0".repeat(64), APRIL));
    }
}

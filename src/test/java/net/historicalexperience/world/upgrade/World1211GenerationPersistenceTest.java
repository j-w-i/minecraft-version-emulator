package net.historicalexperience.world.upgrade;

import static org.junit.jupiter.api.Assertions.*;
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

class World1211GenerationPersistenceTest {
    private static final String FROM = "historical_experience:beta_1_7_3";
    private static final String TO = "historical_experience:beta_1_7_3_content_preview_10";

    private static CompoundTag settings(String alias) {
        var tag = new CompoundTag(); tag.putString("moderner_beta:preset", alias); return tag;
    }
    private static CompoundTag fixture(Path world) throws Exception {
        var source = new CompoundTag();
        source.putString("type", "moderner_beta:moderner_beta");
        source.put("provider_settings", settings(FROM));
        source.put("cave_provider_settings", settings(FROM));
        var generator = new CompoundTag();
        generator.putString("type", "moderner_beta:moderner_beta");
        generator.put("provider_settings", settings(FROM)); generator.put("biome_source", source);
        var stem = new CompoundTag(); stem.put("generator", generator); stem.putString("type", "minecraft:overworld");
        var dimensions = new CompoundTag(); dimensions.put("minecraft:overworld", stem);
        var generation = new CompoundTag(); generation.put("dimensions", dimensions); generation.putLong("seed", 42L);
        var player = new CompoundTag(); player.putInt("XpLevel", 25); player.putString("InventorySentinel", "preserve me");
        var data = new CompoundTag(); data.put("WorldGenSettings", generation); data.put("Player", player);
        data.putLong("Time", 12345L); data.putInt("DataVersion", 3955);
        var root = new CompoundTag(); root.put("Data", data);
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        return root;
    }
    private static CompoundTag read(Path world) throws Exception {
        return NbtIo.readCompressed(world.resolve("level.dat"), NbtAccounter.unlimitedHeap());
    }

    @Test void readsAndAdvancesAllThreeActual1211ReferencesWithoutTouchingOtherData(@TempDir Path world) throws Exception {
        var before = fixture(world);
        assertEquals(FROM, WorldGenSettingsAdvance.read(world).orElseThrow().settingsPreset());
        WorldGenSettingsAdvance.advance(world, FROM, TO);
        var after = read(world);
        assertEquals(before.getCompound("Data").getCompound("Player"), after.getCompound("Data").getCompound("Player"));
        assertEquals(12345L, after.getCompound("Data").getLong("Time"));
        assertEquals(42L, after.getCompound("Data").getCompound("WorldGenSettings").getLong("seed"));
        assertTrue(WorldGenSettingsAdvance.read(world).orElseThrow().alreadyIs(TO));
        var bytes = Files.readAllBytes(world.resolve("level.dat"));
        WorldGenSettingsAdvance.advance(world, FROM, TO);
        assertArrayEquals(bytes, Files.readAllBytes(world.resolve("level.dat")), "idempotent recovery must not rewrite again");
    }

    @Test void lookalikeProviderTagsOnAnotherGeneratorAreNotOwned(@TempDir Path world) throws Exception {
        var root = fixture(world);
        root.getCompound("Data").getCompound("WorldGenSettings").getCompound("dimensions")
            .getCompound("minecraft:overworld").getCompound("generator").putString("type", "minecraft:noise");
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        var bytes = Files.readAllBytes(world.resolve("level.dat"));
        assertTrue(WorldGenSettingsAdvance.read(world).isEmpty());
        assertThrows(UpgradeException.class, () -> WorldGenSettingsAdvance.advance(world, FROM, TO));
        assertArrayEquals(bytes, Files.readAllBytes(world.resolve("level.dat")));
    }

    @Test void unexpectedAliasRefusesBeforeWritingAnything(@TempDir Path world) throws Exception {
        fixture(world);
        var bytes = Files.readAllBytes(world.resolve("level.dat"));
        assertThrows(UpgradeException.class, () -> WorldGenSettingsAdvance.advance(world, "unknown:alias", TO));
        assertArrayEquals(bytes, Files.readAllBytes(world.resolve("level.dat")));
    }

    @Test void wrongNbtLayoutIsNotSilentlyAdopted(@TempDir Path world) throws Exception {
        var root = new CompoundTag(); root.put("data", new CompoundTag());
        NbtIo.writeCompressed(root, world.resolve("level.dat"));
        var bytes = Files.readAllBytes(world.resolve("level.dat"));
        assertTrue(WorldGenSettingsAdvance.read(world).isEmpty());
        assertThrows(UpgradeException.class, () -> WorldGenSettingsAdvance.advance(world, FROM, TO));
        assertArrayEquals(bytes, Files.readAllBytes(world.resolve("level.dat")));
    }

    @Test void crashRecoveryAtEveryCommitWindowPreservesRegionsAndPlayerData(@TempDir Path base) throws Exception {
        for (var phase : new GenerationUpgrade.Phase[] { GenerationUpgrade.Phase.JOURNAL_WRITTEN,
                GenerationUpgrade.Phase.PRESET_ADVANCED, GenerationUpgrade.Phase.METADATA_APPLIED }) {
            Path world = Files.createDirectory(base.resolve(phase.name()));
            var original = fixture(world);
            Files.createDirectory(world.resolve("region"));
            byte[] region = new byte[] {1, 2, 3, 4, 5};
            Files.write(world.resolve("region/r.0.0.mca"), region);
            var initial = HistoricalMetadata.initial(HistoricalIdentifier.parse(FROM),
                HistoricalIdentifier.parse(FROM), "test", 1L);
            HistoricalMetadataStore.forWorldDirectory(world).write(initial);
            var target = GenerationUpgrade.plan(initial, FROM, "test", 1000L);
            GenerationUpgrade.perform(world, target, false, 2000L, phase);
            assertTrue(GenerationUpgrade.recover(world).isPresent());
            // Recovery deliberately keeps the journal until the server has stopped and its
            // in-memory world data can no longer overwrite the stored generator.
            assertTrue(GenerationUpgrade.applyPendingAdvance(world).isPresent());
            assertEquals(target, HistoricalMetadataStore.forWorldDirectory(world).read());
            assertTrue(WorldGenSettingsAdvance.read(world).orElseThrow().alreadyIs(TO));
            assertArrayEquals(region, Files.readAllBytes(world.resolve("region/r.0.0.mca")));
            assertEquals(original.getCompound("Data").getCompound("Player"), read(world).getCompound("Data").getCompound("Player"));
            assertFalse(UpgradeJournal.existsIn(world));
        }
    }
}

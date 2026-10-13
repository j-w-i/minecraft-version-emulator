package net.historicalexperience.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.historicalexperience.server.HistoricalUpgradeService.WorldVersionState;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

/**
 * Guards the three things a world-selection interface needs from the upgrade service.
 *
 * <p>All three are safety properties rather than conveniences, and each has a failure mode that would be invisible
 * on screen: reporting a world as changeable when it is not, writing to a world another instance has open, or
 * turning an ordinary world into a half-historical one. None of them can be checked by looking at the interface.
 */
class WorldVersionManagementTest {
    private static final String VERSION = "historical_experience:release_1_6_4";

    @Test
    void anOrdinaryWorldIsReportedAsOrdinaryAndRefusesToChange(@TempDir Path root) throws IOException {
        Path world = Files.createDirectories(root.resolve("ordinary"));
        Files.writeString(world.resolve("level.dat"), "not a world, but present");

        WorldVersionState state = HistoricalUpgradeService.inspect(world);

        assertEquals(WorldVersionState.Kind.ORDINARY, state.kind(),
            "a world with no historical document is ordinary");
        assertFalse(state.changeable(),
            "and it must not be offered a version: this mod never rewrites the terrain settings of an existing "
                + "world, so the change could not actually be applied");
        assertFalse(state.problem().isBlank(), "the interface needs a reason to show, not a silent refusal");

        HistoricalUpgradeService.Report report =
            HistoricalUpgradeService.upgradeUnloadedWorld(world, VERSION, "test", 1L);

        assertFalse(report.ok(), "the change must be refused");
        assertFalse(HistoricalMetadataStore.forWorldDirectory(world).hasEntry(),
            "and refusing must not leave a metadata document behind: a world tagged historical without a "
                + "generator to match would fail to load");
    }

    @Test
    void aHistoricalWorldIsReportedWithItsVersionAndRecipe(@TempDir Path root) throws IOException {
        Path world = Files.createDirectories(root.resolve("historical"));
        fixture(world, VERSION);

        WorldVersionState state = HistoricalUpgradeService.inspect(world);

        assertEquals(WorldVersionState.Kind.HISTORICAL, state.kind());
        assertEquals(VERSION, state.profileId());
        assertEquals("Release 1.6.4", state.displayName(),
            "the remembered name is resolved through the registry, so a renamed profile cannot go stale here");
        assertEquals(VERSION, state.generationPreset(),
            "this version's first revision shares the version's own id as its alias");
        assertTrue(state.changeable(), "a historical world can be changed");
        assertEquals(1L, state.documentRevision(), "the document's change counter is reported as stored");
    }

    @Test
    void anUnreadableDocumentIsReportedAsUnreadableRatherThanOrdinary(@TempDir Path root) throws IOException {
        Path world = Files.createDirectories(root.resolve("broken"));
        // A document naming a version this build does not declare. The lifecycle refuses to start such a world
        // rather than guessing, and the interface must agree with it instead of offering to change it.
        Files.writeString(world.resolve("historical_experience_metadata.json"), """
            {"schema":1,"profileId":"historical_experience:not_a_version","generationPreset":\""""
            + VERSION + """
            ","revision":1,"history":[{"revision":1,"timestamp":1,"actor":"test","note":"n"}]}
            """);

        WorldVersionState state = HistoricalUpgradeService.inspect(world);

        assertEquals(WorldVersionState.Kind.UNREADABLE, state.kind(),
            "an unknown version must be reported as unreadable, not quietly treated as ordinary");
        assertFalse(state.changeable());
        assertTrue(state.problem().contains("not_a_version") || !state.problem().isBlank(),
            "and the reason must name the problem: " + state.problem());
    }

    @Test
    void aWorldOpenElsewhereIsRefused(@TempDir Path root) throws IOException {
        Path world = Files.createDirectories(root.resolve("open"));
        fixture(world, VERSION);

        assertFalse(HistoricalUpgradeService.appearsOpen(world),
            "a world with no lock file is not open by anyone");

        Path lockFile = world.resolve("session.lock");
        Files.writeString(lockFile, "locked");

        // Holding the lock here is what a running game does, and it is the same condition the service must detect.
        // On this JVM the attempt reports an overlapping lock rather than a null, which is why both are failures.
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.WRITE);
                FileLock held = channel.tryLock()) {
            assertNotNull(held, "the test must actually hold the lock for this to mean anything");
            assertTrue(HistoricalUpgradeService.appearsOpen(world),
                "a locked session.lock means a world is open, and a change made under a running game would be "
                    + "overwritten by that game's own save");

            HistoricalUpgradeService.Report report =
                HistoricalUpgradeService.upgradeUnloadedWorld(world, "historical_experience:release_1_0_0",
                    "test", 2L);

            assertFalse(report.ok(), "so the change is refused");
            assertTrue(report.summary().toLowerCase(java.util.Locale.ROOT).contains("open"),
                "and the refusal says why: " + report.summary());
        }
    }

    @Test
    void aStagedChangeIsReportedAsPendingOnTheNextInspection(@TempDir Path root) throws IOException {
        Path world = Files.createDirectories(root.resolve("staged"));
        fixture(world, VERSION);

        HistoricalUpgradeService.Report report =
            HistoricalUpgradeService.upgradeUnloadedWorld(world, "historical_experience:release_1_0_0", "test",
                5L);

        assertTrue(report.ok(), "a historical world with nothing else wrong accepts a change: " + report.summary());

        WorldVersionState state = HistoricalUpgradeService.inspect(world);
        assertTrue(state.pendingUpgrade(),
            "the generator half waits for the next load, and the interface must be able to say so");
        assertFalse(state.changeable(),
            "and a second change must not be offered while the first is waiting to be applied");
    }

    /**
     * Writes the two files a historical world carries: the metadata document, and a {@code level.dat} whose
     * overworld generator names the same preset in all three places.
     *
     * <p>The generator half is not decoration. An upgrade refuses to run unless the stored generator already names
     * the recipe the metadata declares - which is a safeguard, not a formality, because writing a new recipe over a
     * world whose generator says something else would silently change terrain for chunks not yet generated. A
     * fixture without it would only be testing the refusal, which is why the first version of this test failed.
     */
    private static void fixture(Path world, String versionId) throws IOException {
        Files.createDirectories(world);
        HistoricalMetadataStore.forWorldDirectory(world).write(HistoricalMetadata.initial(
            HistoricalIdentifier.parse(versionId),
            HistoricalIdentifier.parse(versionId),
            "test",
            1L));

        CompoundTag reference = new CompoundTag();
        reference.putString("moderner_beta:preset", versionId);

        CompoundTag biome = new CompoundTag();
        biome.putString("type", "moderner_beta:moderner_beta");
        biome.put("provider_settings", reference.copy());
        biome.put("cave_provider_settings", reference.copy());

        CompoundTag generator = new CompoundTag();
        generator.putString("type", "moderner_beta:moderner_beta");
        generator.put("provider_settings", reference.copy());
        generator.put("biome_source", biome);

        CompoundTag overworld = new CompoundTag();
        overworld.put("generator", generator);
        CompoundTag dimensions = new CompoundTag();
        dimensions.put("minecraft:overworld", overworld);
        CompoundTag settings = new CompoundTag();
        settings.put("dimensions", dimensions);
        CompoundTag data = new CompoundTag();
        data.put("WorldGenSettings", settings);
        CompoundTag root = new CompoundTag();
        root.put("Data", data);

        NbtIo.writeCompressed(root, world.resolve("level.dat"));
    }
}

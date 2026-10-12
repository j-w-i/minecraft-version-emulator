package net.historicalexperience.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.historicalexperience.world.metadata.HistoricalIdentifier;
import net.historicalexperience.world.metadata.HistoricalMetadata;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import net.historicalexperience.world.metadata.MetadataSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the world-level historical state machine.
 *
 * <p>Level: unit with a real temporary filesystem. No Minecraft runtime. These tests cover the
 * preservation and negative paths that matter most: an ordinary world must never be reported as
 * historical, and an unreadable document must fail closed without being modified.
 */
class HistoricalWorldStateTest {
    private static HistoricalMetadata valid() {
        return HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            "historical_experience",
            1_700_000_000_000L);
    }

    @Test
    @DisplayName("a world with no metadata document is Normal, never historical")
    void worldWithoutMetadataIsNormal(@TempDir Path worldDirectory) {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Normal,
            "ordinary worlds must not be tagged: " + state);
        assertFalse(state.isHistorical());
        assertFalse(state.isUnreadable());
        assertTrue(state.metadataOrEmpty().isEmpty());
    }

    @Test
    @DisplayName("a valid document yields Historical with the persisted metadata")
    void validDocumentYieldsHistorical(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(valid());

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Historical, state.toString());
        HistoricalWorldState.Historical historical = (HistoricalWorldState.Historical) state;
        assertEquals(valid(), historical.metadata());
        assertEquals("historical_experience:beta_1_7_3",
            historical.metadata().profileId().value());
        assertEquals("historical_experience:beta_1_7_3",
            historical.metadata().generationPreset().value());
        assertTrue(historical.generationPresetMatchesProfile());
    }

    @Test
    @DisplayName("corrupt metadata yields Unreadable and the bytes are preserved")
    void corruptMetadataYieldsUnreadableAndPreservesBytes(@TempDir Path worldDirectory)
        throws Exception {
        Path file = worldDirectory.resolve(MetadataSchema.FILE_NAME);
        Files.writeString(file, "{ not metadata", StandardCharsets.UTF_8);

        HistoricalMetadataStore store = new HistoricalMetadataStore(file);
        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertTrue(state.isUnreadable());
        assertFalse(state.isHistorical());
        assertEquals("{ not metadata", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("metadata naming an unknown profile yields Unreadable, not a fallback profile")
    void unknownProfileYieldsUnreadable(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:release_9_9_9"),
            HistoricalIdentifier.parse("historical_experience:release_9_9_9"),
            "historical_experience",
            1_700_000_000_000L));

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertTrue(((HistoricalWorldState.Unreadable) state).reason().contains("unknown"),
            "reason should explain the profile is unknown: " + state);
    }

    @Test
    @DisplayName("metadata whose preset does not match its profile binding yields Unreadable")
    void presetProfileMismatchYieldsUnreadable(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            HistoricalIdentifier.parse("historical_experience:skylands"),
            "historical_experience",
            1_700_000_000_000L));

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertTrue(((HistoricalWorldState.Unreadable) state).reason().contains("does not match"),
            "reason should explain the mismatch: " + state);
    }

    @Test
    @DisplayName("a newer schema yields Unreadable so a newer build's data is never rewritten")
    void newerSchemaYieldsUnreadable(@TempDir Path worldDirectory) throws Exception {
        Path file = worldDirectory.resolve(MetadataSchema.FILE_NAME);
        Files.writeString(file, "{\"schema\":99,\"profileId\":\"historical_experience:beta_1_7_3\","
            + "\"generationPreset\":\"moderner_beta:beta\",\"revision\":1,\"history\":["
            + "{\"revision\":1,\"timestamp\":1,\"actor\":\"a\",\"note\":\"n\"}]}");

        HistoricalWorldState state =
            HistoricalWorldState.load(new HistoricalMetadataStore(file));

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
        assertTrue(Files.exists(file), "the unsupported document must not be deleted");
    }

    @Test
    @DisplayName("metadata history advances across reloads and stays readable")
    void advancedMetadataReloads(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalMetadata first = valid();
        HistoricalMetadata second = first.advance(
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            "historical_experience",
            "no-op advance",
            1_700_000_100_000L);
        store.write(second);

        HistoricalWorldState state = HistoricalWorldState.load(store);

        assertTrue(state instanceof HistoricalWorldState.Historical, state.toString());
        assertEquals(2L, state.metadataOrEmpty().orElseThrow().revision());
        assertEquals(2, state.metadataOrEmpty().orElseThrow().history().size());
    }

    @Test
    @DisplayName("Unreadable for a directory in place of the document")
    void directoryInPlaceOfDocumentIsUnreadable(@TempDir Path worldDirectory) throws Exception {
        Path file = worldDirectory.resolve(MetadataSchema.FILE_NAME);
        Files.createDirectory(file);

        HistoricalWorldState state =
            HistoricalWorldState.load(new HistoricalMetadataStore(file));

        assertTrue(state instanceof HistoricalWorldState.Unreadable, state.toString());
    }

    @Test
    @DisplayName("Historical record rejects a null profile and a null metadata")
    void historicalRecordRequiresComponents(@TempDir Path worldDirectory) {
        assertThrows(NullPointerException.class, () -> new HistoricalWorldState.Historical(
            worldDirectory.resolve("x"), null, null));
        assertThrows(NullPointerException.class, () -> new HistoricalWorldState.Historical(
            worldDirectory.resolve("x"), valid(), null));
    }
}

/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world;

import java.nio.file.Path;
import net.historicalexperience.profile.HistoricalProfiles;
import net.historicalexperience.server.HistoricalServerLifecycle;
import net.historicalexperience.version.HistoricalVersions;
import net.historicalexperience.world.metadata.HistoricalMetadataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotCreationTest {
    private static final String PATCH = "historical_experience:snapshot_11w49a";
    private static final String BASE = "historical_experience:release_1_1";
    @AfterEach void clear() { HistoricalSnapshotCreation.clear(); }

    @Test void exactDirectoryOneShotNeverTagsAnotherLoad(@TempDir Path root) {
        var target = root.resolve("new");
        HistoricalSnapshotCreation.bind(target, PATCH);
        assertEquals("", HistoricalSnapshotCreation.consume(root.resolve("other"), BASE));
        assertEquals(PATCH, HistoricalSnapshotCreation.consume(target.resolve(".").normalize(), BASE));
        assertEquals("", HistoricalSnapshotCreation.consume(target, BASE));
        assertFalse(java.nio.file.Files.exists(target), "the intent itself performs no filesystem writes");
    }
    @Test void wrongParentFailsClosedAndCannotLeakIntoALaterWorld(@TempDir Path root) {
        HistoricalSnapshotCreation.bind(root, PATCH);
        assertThrows(IllegalArgumentException.class, () -> HistoricalSnapshotCreation.consume(root, "historical_experience:release_1_4"));
        assertEquals("", HistoricalSnapshotCreation.consume(root, BASE));
        assertThrows(IllegalArgumentException.class, () -> HistoricalSnapshotCreation.bind(root, "unknown"));
    }
    @Test void cancellationAndANewCreationDiscardPreviousIntent(@TempDir Path root) {
        HistoricalSnapshotCreation.bind(root, PATCH); HistoricalSnapshotCreation.clear();
        assertEquals("", HistoricalSnapshotCreation.consume(root, BASE));
        HistoricalSnapshotCreation.bind(root.resolve("old"), PATCH);
        HistoricalSnapshotCreation.bind(root.resolve("new"), PATCH);
        assertEquals("", HistoricalSnapshotCreation.consume(root.resolve("old"), BASE));
        assertEquals(PATCH, HistoricalSnapshotCreation.consume(root.resolve("new"), BASE));
    }
    @Test void existingUnpatchedMetadataWinsAndDiscardsEvenAMatchingIntent(@TempDir Path root) throws Exception {
        var version = HistoricalVersions.byId(BASE).orElseThrow();
        var binding = HistoricalProfiles.byGenerationPreset(version.latestRevision().alias()).orElseThrow();
        var store = HistoricalMetadataStore.forWorldDirectory(root);
        HistoricalServerLifecycle.establishHistoricalWorld(store, binding);
        var original = store.read();
        HistoricalSnapshotCreation.bind(root, PATCH);
        HistoricalServerLifecycle.establishHistoricalWorld(store, binding);
        assertEquals(original, store.read()); assertEquals("", store.read().snapshotPatchId());
        assertEquals("", HistoricalSnapshotCreation.consume(root, BASE), "a refused stale intent must not survive the existing-world guard");
    }
    @Test void normalBootstrapPersistsThePatchAndDoesNotOverwriteAnExistingWorld(@TempDir Path root) throws Exception {
        var version = HistoricalVersions.byId(BASE).orElseThrow();
        var binding = HistoricalProfiles.byGenerationPreset(version.latestRevision().alias()).orElseThrow();
        var store = HistoricalMetadataStore.forWorldDirectory(root);
        HistoricalSnapshotCreation.bind(root, PATCH);
        HistoricalServerLifecycle.establishHistoricalWorld(store, binding);
        var created = store.read();
        assertEquals(PATCH, created.snapshotPatchId()); assertEquals(BASE, created.profileId().value());
        assertEquals(version.latestRevision().alias(), created.generationPreset().value());
        assertEquals(1, created.revision());
        HistoricalSnapshotCreation.clear();
        HistoricalServerLifecycle.establishHistoricalWorld(store, binding);
        assertEquals(created, store.read());
        assertEquals("", HistoricalSnapshotCreation.consume(root, BASE));
    }
}

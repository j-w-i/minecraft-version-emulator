package net.historicalexperience.world.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for metadata file persistence.
 *
 * <p>Level: unit with a real temporary filesystem. No Minecraft runtime.
 */
class HistoricalMetadataStoreTest {
    private static HistoricalMetadata metadata(long revision) {
        HistoricalMetadata base = HistoricalMetadata.initial(
            HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            HistoricalIdentifier.parse("moderner_beta:beta"),
            "historical_experience",
            1_700_000_000_000L);
        HistoricalMetadata result = base;
        for (long i = 2; i <= revision; i++) {
            result = result.advance(null, "historical_experience", "revision " + i,
                1_700_000_000_000L + i);
        }
        return result;
    }

    private static List<Path> entries(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.sorted().toList();
        }
    }

    @Test
    @DisplayName("write then read roundtrips through the filesystem")
    void writeThenReadRoundtrips(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        assertFalse(store.exists());

        HistoricalMetadata written = metadata(3);
        store.write(written);

        assertTrue(store.exists());
        assertEquals(written, store.read());
    }

    @Test
    @DisplayName("write replaces an existing document atomically and leaves no temporary files")
    void writeReplacesAndLeavesNoTemporaries(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        store.write(metadata(1));
        store.write(metadata(4));

        assertEquals(metadata(4), store.read());
        assertEquals(List.of(worldDirectory.resolve(MetadataSchema.FILE_NAME)),
            entries(worldDirectory));
    }

    @Test
    @DisplayName("read on a missing document reports unreadable rather than normal")
    void readMissingDocumentFails(@TempDir Path worldDirectory) {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        assertFalse(store.exists());
        assertThrows(MetadataException.class, store::read);
    }

    @Test
    @DisplayName("corrupt metadata fails closed and the corrupt bytes are preserved")
    void corruptMetadataIsPreserved(@TempDir Path worldDirectory) throws Exception {
        Path file = worldDirectory.resolve(MetadataSchema.FILE_NAME);
        byte[] corrupt = "{ this is not valid metadata".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);

        HistoricalMetadataStore store = new HistoricalMetadataStore(file);
        assertTrue(store.exists());
        assertThrows(MetadataException.class, store::read);

        // Fail closed means untouched: the original bytes must still be there.
        assertTrue(Files.exists(file));
        assertEquals("{ this is not valid metadata",
            Files.readString(file, StandardCharsets.UTF_8));
        assertArrayEqualsBytes(corrupt, Files.readAllBytes(file));
    }

    private static void assertArrayEqualsBytes(byte[] expected, byte[] actual) {
        assertEquals(expected.length, actual.length, "byte length must be unchanged");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "byte " + i + " must be unchanged");
        }
    }

    @Test
    @DisplayName("a directory in place of the document is reported, not treated as absent")
    void directoryInPlaceOfDocumentFails(@TempDir Path worldDirectory) throws Exception {
        Path file = worldDirectory.resolve(MetadataSchema.FILE_NAME);
        Files.createDirectory(file);

        HistoricalMetadataStore store = new HistoricalMetadataStore(file);
        MetadataException thrown = assertThrows(MetadataException.class, store::read);
        assertTrue(thrown.getMessage().contains("directory"),
            "message should explain the path is a directory: " + thrown.getMessage());
    }

    @Test
    @DisplayName("writeIfAbsent creates once and never clobbers an existing document")
    void writeIfAbsentNeverClobbers(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        assertTrue(store.writeIfAbsent(metadata(1)));
        assertFalse(store.writeIfAbsent(metadata(9)));
        assertEquals(metadata(1), store.read());
    }

    @Test
    @DisplayName("writeIfAbsent refuses to replace an unreadable document")
    void writeIfAbsentPreservesUnreadableDocument(@TempDir Path worldDirectory) throws Exception {
        Path file = worldDirectory.resolve(MetadataSchema.FILE_NAME);
        Files.writeString(file, "garbage");

        HistoricalMetadataStore store = new HistoricalMetadataStore(file);
        assertFalse(store.writeIfAbsent(metadata(1)));
        assertEquals("garbage", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("writeIfAbsent creates the parent directory when missing")
    void writeIfAbsentCreatesParent(@TempDir Path root) throws Exception {
        Path nested = root.resolve("world").resolve("sub");
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(nested);

        assertTrue(store.writeIfAbsent(metadata(1)));
        assertTrue(Files.isRegularFile(nested.resolve(MetadataSchema.FILE_NAME)));
    }

    @Test
    @DisplayName("store file location matches the documented file name")
    void storeFileLocationIsStable(@TempDir Path worldDirectory) {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        assertEquals(worldDirectory.resolve(MetadataSchema.FILE_NAME).toAbsolutePath(),
            store.file());
    }

    @Test
    @DisplayName("concurrent creators with distinct values: exactly one wins, content is never torn")
    void concurrentCreatorsWithDistinctValues(@TempDir Path worldDirectory) throws Exception {
        int writers = 8;
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);

        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newFixedThreadPool(writers);
        java.util.List<java.util.concurrent.Future<Boolean>> futures = new java.util.ArrayList<>();
        java.util.List<Throwable> failures =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        // Each writer offers a DIFFERENT revision so a torn or clobbered document would show up
        // as a revision matching no writer's value.
        for (int i = 1; i <= writers; i++) {
            long revision = i;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    return store.writeIfAbsent(metadata(revision));
                } catch (Throwable t) {
                    failures.add(t);
                    return Boolean.FALSE;
                }
            }));
        }

        start.countDown();
        int created = 0;
        for (java.util.concurrent.Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get(30, java.util.concurrent.TimeUnit.SECONDS))) {
                created++;
            }
        }
        pool.shutdownNow();

        assertTrue(failures.isEmpty(), "no writer may fail: " + failures);
        assertEquals(1, created, "exactly one writer may report creating the document");

        // The surviving document must be exactly one writer's value: readable and self consistent.
        HistoricalMetadata winner = store.read();
        assertTrue(winner.revision() >= 1 && winner.revision() <= writers,
            "winner revision must be one of the offered values, was " + winner.revision());
        assertEquals(winner, metadata(winner.revision()));
        assertEquals(MetadataJson.encode(winner),
            Files.readString(store.file(), StandardCharsets.UTF_8));

        // And no temporary files may be left behind by the losers.
        assertEquals(List.of(worldDirectory.resolve(MetadataSchema.FILE_NAME)),
            entries(worldDirectory));
    }

    @Test
    @DisplayName("concurrent creators with the same value still produce one valid document")
    void concurrentCreatorsWithSameValue(@TempDir Path worldDirectory) throws Exception {
        HistoricalMetadataStore store = HistoricalMetadataStore.forWorldDirectory(worldDirectory);
        HistoricalMetadata shared = metadata(3);

        int writers = 6;
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newFixedThreadPool(writers);
        java.util.List<java.util.concurrent.Future<Boolean>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < writers; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return store.writeIfAbsent(shared);
            }));
        }
        start.countDown();
        int created = 0;
        for (java.util.concurrent.Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get(30, java.util.concurrent.TimeUnit.SECONDS))) {
                created++;
            }
        }
        pool.shutdownNow();

        assertEquals(1, created);
        assertEquals(shared, store.read());
        assertEquals(List.of(worldDirectory.resolve(MetadataSchema.FILE_NAME)),
            entries(worldDirectory));
    }
}
